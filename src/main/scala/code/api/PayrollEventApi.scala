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
import net.liftweb.mapper._ 
import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object PayrollEventApi extends RestHelper with ReportRest with net.liftweb.common.Logger{
		serve {
			case "payroll" :: "bppayroll" :: Nil Post _ =>{
				for{
					id <- S.param("id") ?~ "id parameter missing" ~> 400
					business_pattern <- S.param("business_pattern") ?~ "business_pattern parameter missing" ~> 400
					event <- S.param("event") ?~ "event parameter missing" ~> 400
					qtd <- S.param("qtd") ?~ "qtd parameter missing" ~> 400
					value <- S.param("value") ?~ "value parameter missing" ~> 400
					dateStr <- S.param("date") ?~ "date parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
				}yield{
					def obj = if(id == "0"){
							BusinessPatternPayroll.createInCompany
						}else{
							BusinessPatternPayroll.findByKey(id.toLong).get
						}
					JBool(obj.date(Project.strToDateOrToday(dateStr)).business_pattern(business_pattern.toLong).qtd(qtd.toInt).event(event.toLong).obs(obs).value(value.toFloat).save)
				}
			}
			case "payroll" :: "bppayroll" :: id :: Nil Delete _ =>{
				BusinessPatternPayroll.findByKey(id.toLong).get.delete_!
				JBool(true)
			}			
			case "payroll" :: "bppayroll" :: Nil JsonGet _ =>{
				def start = Project.strToDateOrToday(S.param("start") openOr "")
				def end = Project.strToDateOrToday(S.param("end") openOr "")
				def users = S.param("user[]") match {
					case Full(p)=>{
						S.params("user[]").filter(_ != "").map(_.toLong)
					}
					case _ => S.param("user") match{
						case Full( r:String ) => {
							r.toLong::Nil
						}
						case _ => {
							0l::Nil	
						}
					}
				}
				def events = S.param("events_filter[]") match {
					case Full(p)=>{
						S.params("events_filter[]").filter(_ != "").map(_.toLong)
					}
					case _ => S.param("events_filter") match{
						case Full( r:String ) => {
							r.toLong::Nil
						}
						case _ => {
							0l::Nil	
						}
					}
				}
				JsArray(BusinessPatternPayroll.findAllInCompany(
					 OrderBy(BusinessPatternPayroll.business_pattern, Ascending),
					 OrderBy(BusinessPatternPayroll.date, Ascending),
					 OrderBy(BusinessPatternPayroll.value, Descending),
					 BySql("date_c between date(?) and date(?)",IHaveValidatedThisSQL("",""), start, end), 
					 ByList(BusinessPatternPayroll.business_pattern, users), 
					 ByList(BusinessPatternPayroll.event, events)).map(toJson))
			}
			case "payroll" :: "events" :: Nil JsonGet _ =>{
				JsArray(PayrollEvent.findAllInCompany(OrderBy(PayrollEvent.name, Ascending)).map(eventToJson))
			}
			case "payroll" :: "process_start" :: Nil Post _ =>{
				for{
					start <- S.param("start") ?~ "end parameter missing" ~> 400
					end <- S.param("end") ?~ "end parameter missing" ~> 400
					dttypes <- S.param("dttype") ?~ "end parameter missing" ~> 400
				}yield{
					PayrollService.processEvents(Project.strToDateOrToday(start), Project.strToDateOrToday(end), dttypes);
				}
				JInt(1)
			}
					
		}

	def eventToJson(event:PayrollEvent) = {
		JsObj(
				("id",event.id.is),
				("name",event.name.is),
				("type",event.eventType.is)
			)
	}
	def toJson(bppr:BusinessPatternPayroll) = {
		JsObj(
				("id",bppr.id.is),
				("business_pattern",bppr.business_pattern.is),
				("name",bppr.business_pattern.obj.get.name.is),
				("qtd",bppr.qtd.is),
				("obs",bppr.obs.is),
				("value",bppr.value.is),
				("date",bppr.date.is.getTime),
				("event",bppr.event.is),
				("eventName",bppr.event.obj.get.name.is)

			)
	}
}