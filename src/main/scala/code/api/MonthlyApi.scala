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


object MonthlyApi extends RestHelper  with net.liftweb.common.Logger  {
	serve {

		case "monthly" :: "transfer" :: Nil Post _ =>{
			for {
				dateStart <- S.param("start") ?~ "start parameter missing" ~> 400
				dateEnd <- S.param("end") ?~ "end parameter missing" ~> 400
				account <- S.param("account") ?~ "account parameter missing" ~> 400
				limit <- S.param("limit") ?~ "limit parameter missing" ~> 400
			} yield {				
				var start = Project.strToDate(dateStart+" 00:00")
				var end = Project.strToDateOrToday(dateEnd)
				var ac = Account.findByKey (account.toLong).get
		        //val  bank = "001";
		        val bank = BusinessRulesUtil.zerosLimit (ac.bank.toString, 3)
		        val now  = new Date()
				Monthly.toRemessa240(start, end, ac, limit.toLong)
				val filePath = if(Project.isLinuxServer){
		          if (Project.isLocalHost) {
					"file:///var/www/html/remessa/"
		          } else {
		          	(Props.get("remessa.urlbase") openOr "/tmp/")
		          }
		        }else{
		          "c:\\vilarika\\"
		        }
				JsObj(("status","success"),
					  ("url",filePath + "remessa_" + AuthUtil.company.id.toString + "_" 
		        + bank + "_" + Project.dtformat(now, "yyyyMMdd_HHmm") + ".txt"))
				//JInt(1)
			}			
		}
		case "monthly" :: "salesIntegration" :: Nil Post _ =>{
			for {
				dateStart <- S.param("start") ?~ "start parameter missing" ~> 400
				dateEnd <- S.param("end") ?~ "end parameter missing" ~> 400
			} yield {				
				var start = Project.strToDate(dateStart+" 00:00")
				var end = Project.strToDateOrToday(dateEnd)
				var ac = Account.findByKey (account.toLong).get
		        //val  bank = "001";
		        val bank = BusinessRulesUtil.zerosLimit (ac.bank.toString, 3)
		        val now  = new Date()
				Monthly.toRemessa240(start, end, ac, limit.toLong)
				val filePath = if(Project.isLinuxServer){
		          if (Project.isLocalHost) {
					"file:///var/www/html/remessa/"
		          } else {
		          	(Props.get("remessa.urlbase") openOr "/tmp/")
		          }
		        }else{
		          "c:\\vilarika\\"
		        }
				JsObj(("status","success"),
					  ("url",filePath + "remessa_" + AuthUtil.company.id.toString + "_" 
		        + bank + "_" + Project.dtformat(now, "yyyyMMdd_HHmm") + ".txt"))
				//JInt(1)
			}			
		}
	}
}
