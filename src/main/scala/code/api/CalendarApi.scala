package code
package api

import code.model._
import code.util._
import code.actors._
import code.comet._
import code.service._

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
import java.util.Calendar

import net.liftweb.json._
import scalendar._
import Month._
import Day._
//implicit val formats = DefaultFormats // Brings in default date formats etc.

object CalendarApi extends RestHelper with net.liftweb.common.Logger  {
	
	serve {
		
		case "calendar" :: "changeCommandId" :: Nil Post _ =>{
			try{
				def treatmentId = S.param("treatmentId") openOr ""
				def command = S.param("command") openOr ""
				TreatmentService.changeCommandId(treatmentId, command)
				JInt(1)
			}catch{
				case e:Exception => JString(e.getMessage)
				case _ => JString("Erro desconhecido!")
			}
		}
		case  "calendar" :: "customer" :: "add" :: Nil Post _ => {
			try{
				def name = S.param("name") openOr ""
				def phone = S.param("phone") openOr ""
				def mobile_phone = S.param("mobile_phone") openOr ""
				def mobile_phone_op = S.param("mobile_phone_op") openOr "0"
				def email = S.param("email") openOr ""
				def obs = S.param("obs") openOr ""
				def bp_indicatedby_str = S.param("bp_indicatedby") openOr "0"
				def offsale = S.param("offsale") openOr "0"
				def birthday = S.param("birthday") openOr "0"

				def bp_indicatedby = if(bp_indicatedby_str == ""){
					"0"
				}else{
					bp_indicatedby_str
				}
				val customer = Customer.create.name(name).obs(obs).
					phone(phone).mobilePhone(mobile_phone).
					mobilePhoneOp(mobile_phone_op.toLong).
					email(email).
					bp_indicatedby(bp_indicatedby.toLong).
					offsale(offsale.toLong).
					birthday(Project.strToDateOrNull(birthday)).
					company(AuthUtil.company)
				customer.save
				JInt(customer.id.is)
			}catch{
				case e:RuntimeException => {
					JString(e.getMessage)
				}
				case _ =>{
					JString("Erro desconhecido, ao cadastrar " + AuthUtil.company.appCustName("Cliente") + "!")
				}
			}
		}
		case "calendar" :: date :: "getNextCommandId" ::  Nil JsonGet _ => {
			def startDate = Project.strToDateOrToday(date)
			JInt(Treatment.nextCommandNumber(startDate,AuthUtil.company, AuthUtil.unit))
		}

		case "calendar" :: "treatments"::"today"::"total"::  Nil JsonGet _ => {
			JsArray(AuthUtil.company.usersForCalendar().map(u => JsObj(("name",u.userName.is),("total",TreatmentService.countTreatmentsBetweenPerUser(new Date(),new Date(),u)))))
		}

		case "calendar" :: "treatments"::"frequency"::"total"::  Nil JsonGet _ => {
			JsArray(week.reverse.map(date => JsObj(("date",date.getTime),("total",TreatmentService.countTreatmentsBetweenPerDate(date)))))
		}
		
		case "calendar" :: "customer_treatments" :: Nil Post _ => {
			def startDate = Project.strToDateOrToday(S.param("start") openOr "")
			def endDate = Project.strToDateOrToday(S.param("end")  openOr "")
			TreatmentService.treatmentsBetweenDatesAsJson(startDate,endDate)
		}

		case "calendar" :: "treatments" :: start :: end :: Nil JsonGet _ => {
			def startDate = Project.strToDateOrToday(start)
			def endDate = Project.strToDateOrToday(end)
			JsArray(code.service.TreatmentCalendarService.
				treatmentsForCalendarAsJson(AuthUtil.company, AuthUtil.unit, 
				startDate, endDate) ::: BusyEvent.findByDate(startDate, endDate, AuthUtil.unit).map(_.toJson))
		}
		case "calendar" :: "treatments" :: Nil Post _ => {
			def assetNumber(n:String) = if(n=="") 0l else n.toLong
			def startDate = Project.strToDateOrToday(S.param("start") openOr "")
			def endDate = Project.strToDate((S.param("end") openOr "")+" 23:00")
			def group = assetNumber(S.param("group") openOr "0")
			def user = assetNumber(S.param("user") openOr "0")
			lazy val currentUnit_? = (S.param("currentUnit") openOr "true").toBoolean
			val treatments = {
				if(user > 0){
					code.service.TreatmentCalendarService.
					treatmentsForCalendarAsJson(AuthUtil.company, AuthUtil.unit,
						User.findByKey(user).get, startDate, endDate)
				}else if(group>0 && currentUnit_?){
					code.service.TreatmentCalendarService.
					treatmentsForCalendarAsJson(AuthUtil.company, AuthUtil.unit,
						UserGroup.findByKey(group).get, startDate, endDate)
				}else{
					code.service.TreatmentCalendarService.
					treatmentsForCalendarAsJson(AuthUtil.company, AuthUtil.unit, 
						startDate, endDate)
				}
			}
			val busys = {
				if(user > 0){
					BusyEvent.findByUserDate(User.findByKey(user).get, startDate, endDate, AuthUtil.unit).map(_.toJson)
				}else if(group>0 && currentUnit_?){
					BusyEvent.findByDate(startDate, endDate, AuthUtil.unit).map(_.toJson)
				}else{
					BusyEvent.findByDate(startDate, endDate, AuthUtil.unit).map(_.toJson)
				}
			}
			JsArray( treatments ::: busys )
		}			

		case "calendar" :: "remove_freebusy"::idstr:: Nil JsonGet _ => {
			def id:Long = idstr.toLong
			val be = BusyEvent.findByKey(id.toLong).get	
			be.delete_!
			JInt(1)
		}

		case "calendar" :: "currentUnit" :: unitId :: Nil JsonGet _ => {
			AuthUtil << CompanyUnit.findByKey(unitId.toLong).get
			JInt(1)
		}

		case "calendar" :: "units" :: Nil JsonGet _ => {
			val currentUnit = AuthUtil.unit
			val currentCompany = AuthUtil.company.id
			val currentUser = AuthUtil.user.id
			JsArray(CompanyUnit.findAllForShowInCalendar(currentCompany,currentUser).map(u => JsObj(("id",u.id.is),("name", u.name.is),("isCurrent",u.id.is == currentUnit.id.is))))
		}

		case "calendar" :: "groups" :: "for_calendar" :: Nil JsonGet _ => {
			JsArray(UserGroup.findAllIncompayForCalendar.map(u => JsObj(("id",u.id.is),("name", u.name.is))))
		}		
		case "calendar" :: "groups" :: Nil JsonGet _ => {
			JsArray(UserGroup.findAllInCompany().map(u => JsObj(("id",u.id.is),("name", u.name.is))))
		}
		
		case "calendar" :: "freebusy" :: Nil Post _ => {
			try { 
			  
				def start = Project.strToDate( S.param("start") openOr "" )
				def end = Project.strToDate( S.param("end") openOr "" )
				def obs = S.param("obs") openOr ""
				def status = (S.param("user[]") openOr "") match {
					case (user:String) if(user == "SELECT_ALL") => BusyEvent.StatusEnum.All
					case _ => BusyEvent.StatusEnum.Single
				}

				def users:List[Long] = S.params("user[]").filter(_ != "all").map(_.toLong)

				if(status == BusyEvent.StatusEnum.Single){
					users.foreach((u)=>{
						BusyEvent.createInCompany.user(u).start(start).end(end).description(obs).status(status).save
					})
				}else{
					BusyEvent.createInCompany.start(start).end(end).description(obs).status(status).save
				}
				TratmentServer ! TreatmentMessage("BusyEvent", new Date())
				JInt(1)
			} catch {
			  case e: Exception => JString(e.getMessage)
			}			
		}

		case "calendar" :: "allfreebusy" :: start :: end :: Nil JsonGet _ => {
			lazy val startDate = Project.strToDateOrToday(start)
			lazy val endDate = Project.strToDateOrToday(end)
			lazy val showWorkHours = (S.param("showWorkHours")).get.toBoolean
			lazy val userIds = S.param("users").get.split(",").map(_.toLong).toList

			JsArray(BusyEvent.findByDate(startDate, endDate, userIds, AuthUtil.unit, showWorkHours).map( (be) => {
				JsObj(("id",be.id.is), ("user",be.user.obj.get.name.is), ("obs",be.description.is), ("start",be.start.is.getTime), ("end",be.end.is.getTime))
			}))
		}
		case "calendar" :: "clearBusyEventByUser" :: Nil Post _ => {
			// Nao está chegando aqui tem erro na chamada no userController.js
			// quem tá deletando mesmo é o save do workhour
			S.param("user") match {
				case Full(u) =>{
					BusyEvent.clearBusyEventByUser(User.findByKey(u.toLong).get, 
						AuthUtil.company, new Date(),
						AuthUtil.unit)
				}
				case _ => 
			}
			JInt(1)
		}
		case "calendar" :: "freebusys" :: start :: end :: Nil JsonGet _ => {
			def unit =  S.param("unit") match {
				case Full(u) => u.toLong
				case _ => AuthUtil.unit.id.is
			}
			val duration = Recurrence.#:#(Project.strOnlyDateToDate(start), Project.strOnlyDateToDate(end))
			val days = duration.by(1.day)
			days.foreach((day) => {
				FreeBusyActor ! FreeBusyRequest(Project.dateToStr(day.start), Project.dateToStr(day.end), unit, AuthUtil.company.id.is)
				
			});
			JInt(1)
		}
	}

	def startWeek = {
		val cal = Calendar.getInstance()
		cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - (cal.get(Calendar.DAY_OF_WEEK)-1));
		cal.getTime
	}

	def endWeek = {
		val cal = Calendar.getInstance()
		cal.setTime(startWeek)
		cal.add(Calendar.DAY_OF_YEAR,6)
		cal.getTime
	}

	def week = {
		def ++(d:Date) = {
			val cal = Calendar.getInstance()
			cal.setTime(d)
			cal.add(Calendar.DAY_OF_YEAR,1);
			cal.getTime
		}
		var actual = startWeek
		var listDate = startWeek :: Nil
		do{
			actual = ++(actual)
			listDate =  actual :: listDate
		}while(actual != new Date());
		listDate	
	}
}
