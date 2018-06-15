package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

import net.liftweb._
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


object PaymentApi extends RestHelper {

		serve {
			case "payment" :: "getTotal" :: start :: end :: userStr :: Nil JsonGet _ => {
				def startDate = start match {
					case (s:String) if(s != "") => Project.strOnlyDateToDate(s)
					case _ => new Date();
				}
				
				def endDate =  end match {
					case (s:String) if(s != "") => Project.strOnlyDateToDate(s)
					case _ => new Date();
				}
				
				def user = 	userStr match {
					case (s:String) if(s != "") => User.findByKey(s.toLong).get
					case _ => User.create;
				}

				def paymentsFiltered = {
					PaymentService.commisionBetweenByUser(startDate,endDate,user)
				}

				JsObj(
					("status","success"),
					("total",paymentsFiltered.map(_.value).foldLeft(BigDecimal(0.0))(_.toFloat+_.toFloat).toFloat)
				)
		}

		case "payment" :: "cheque" :: "makeAsReceived" :: id :: Nil JsonGet _ => {
			try{
				AuthUtil.company.findChequeByKey(id.toLong) match {
					case Full(c:Cheque) => {
						c.makeAsReceived
						JsObj(("status","sucesso"))
					}
					case _ => JsObj(("status","error"),("message","Não encontrado"))

				}
			}catch{
				case e:Exception => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",""))
			}

		}

		case "payment" :: "getCheques" :: Nil JsonGet _ => {
			JsArray(
			AuthUtil.company.chequesNotReceived.map((c:Cheque) => {
				JsObj(
					("number",c.number.is),
					("date",c.paymentDate.is.getTime),
					("gooddate",c.dueDate.is.getTime),
					("agency",c.agency.is),
					("customer",c.customer.obj match {
						case Full(c) => c.name.is
						case _ => ""
					}
					),
					("customerId",c.customer.is),
					("bank",c.bank.is),
					("account",c.account.is),
					("value",c.value.is.toFloat),
					("auditstr", c.auditStr),
					("id",c.id.is)
				)
			})
			)
		}
		
		case "payment" :: "monthly" :: "transation" :: Nil JsonGet _ => {
			JInt(2)
		}

		case "payment" :: "monthly" :: "returnData" :: Nil JsonGet _ => {
			JInt(1)
		}	
	}
}