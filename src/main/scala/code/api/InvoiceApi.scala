package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

import net.liftweb._
import mapper._
import util._
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers

import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object InvoiceApi extends RestHelper with ReportRest with net.liftweb.common.Logger  {

	serve {
		case "invoice" :: "invoicing" :: offsaleId :: unitId :: typeHosp :: start :: end :: Nil JsonGet _ => {
			try{
				val invoice = Invoice.invoicing (Project.strToDateOrToday(start), 
					Project.strToDateOrToday(end), offsaleId.toInt, unitId.toInt, typeHosp)
				JsObj(("status","success"),
					  ("id",invoice.idForCompany.is),
					  ("value", invoice.value.toString)
					 )
			}catch{
				case e:NumberFormatException  => JsObj(("status","error"),("message","invoice inválido!"))
				case e:RuntimeException  => {
					JsObj(("status","error"),("message","Erro no faturamento " + e.getMessage))
				}				
				case e:Exception  => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",false))
			}
		}
		case "invoice" :: "to_invoicing" :: Nil Post _ => {
			def users:String = S.param("user") match {
				case Full(s) => if(s == "SELECT_ALL" || s == "") {
					" and 1 = 1 " 
					} else {
					" and tr.user_c = %s".format(s)
					}
				case _ => " and 1 = 1 "
			}

			def typess:String = S.param("hospitalizationType") match {
				case Full(p) => p
				case _ => "0,1";
			}
//			println ("vaiiiii ANTES =====" + typess + "====")
			def types = if (typess == "0") {
				" and ted.hospitalizationType = '' "
			} else {
				" and ted.hospitalizationType <> '' "
			}
//			println ("vaiiiii ===== " + types)
			def units:String = S.param("unit") match {
				case Full(s) if(s != "") => " and tr.unit = %s".format(s)
				case _ => " and " + Treatment.unitsToShowSql
			}

			def offsales:String = S.param("offsale") match {
				case Full(s) if(s != "") => " and (ted.offsale = %s or td.offsale = %s) ".format(s,s)
				case _ => " and 1 = 1 "
			}

			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			lazy val SQL_REPORT = """
				select tr.dateevent, bc.name, bp.short_name, cu.short_name, of.short_name, tr.status,
				(select sum (price) from treatmentdetail td where td.treatment = tr.id) as valor 
				from treatment tr 
				inner join business_pattern bc on bc.id = tr.customer
				inner join treatmentdetail td on td.treatment = tr.id
				left join business_pattern bp on bp.id = tr.user_c
				left join treatedoctus ted on ted.treatment = tr.id %s
				left join offsale of on (of.id = ted.offsale or of.id = td.offsale)
				left join companyunit cu on cu.id = tr.unit 
				where tr.company = ? and tr.dateevent between ? and ? and tr.hasdetail = true
				and tr.id not in 
				(select it.treatment from invoicetreatment it where it.company = tr.company and it.treatment = tr.id)
				%s %s %s 
				order by tr.dateevent desc
				"""
//				println ("vai ======= user " + users + " datas " + start + "  " + end);
//println ("vaiii SQL " + SQL_REPORT.format(users, units, offsales, types));
			toResponse(SQL_REPORT.format(types, users, units, offsales),List(AuthUtil.company.id.is, start, end))
		}

		case "invoice" :: "xmltiss" :: invoice :: Nil JsonGet _ => {
			try{
				val xinvoice = Invoice.toXmlTiss (invoice.toLong)
		       	var  iv = Invoice.findAllInCompany(By(Invoice.idForCompany,invoice.toInt))(0)
		       	var  os = OffSale.findByKey (iv.offsale).get
			    val filePath = if(Project.isLinuxServer){
		          (Props.get("tissxml.urlbase") openOr "/tmp/")
		        }else{
		          "c:\\vilarika\\"
		        }
				JsObj(("status","success"),
					  ("url",filePath + "tiss_" + AuthUtil.company.id.toString + "_" 
        				+ os.xmlname.is + "_" + invoice.toString +".xml")
					 )

				//S.redirectTo(filePath + "tissfile"+ invoice.toString +".xml")
			}catch{
				case e:NumberFormatException  => JsObj(("status","error"),("message","xml inálido!"))
				case e:RuntimeException  => {
					JsObj(("status","error"),("message","Erro no xml: " + e.getMessage))
				}				
				case e:Exception  => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",false))
			}
		}
	}
}

