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
import scalendar._
import Month._
import Day._
import java.util.Date
import java.util.Calendar

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.

object MobileApi extends RestHelper with net.liftweb.common.Logger {
  serve {
    case "mobile" :: "api" :: "messagesToSend" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password)
        val customers = TreatmentService.customersWithTreatments(customer.company.obj.get, new Date())
        JsArray(
          customers.filter(_.mobilePhone.is != "").map((c: Customer) => {
            val company = c.company.obj.get
            val mail = code.daily.DailyReport.treatmentsTodaySms(c, company)
            JsObj(("phone", c.mobilePhone.is.toString), ("name",  c.name.is), ("message", mail.toString), ("title", "Atendimento " + company.name.is))
          }))
      }
    }
    case "mobile" :: "api" :: "login" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
      } yield {
        Customer.login(email, password).asJs
      }
    }
    case "mobile" :: "api" :: "history" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        startDate <- S.param("startDate") ?~ "startDate parameter missing" ~> 400
        endDate <- S.param("endDate") ?~ "endDate parameter missing" ~> 400
      } yield {
        val start = Project.strOnlyDateToDate(startDate)
        val end = Project.strOnlyDateToDate(endDate)
        val customer = Customer.login(email, password)
        JsArray(customer.history(start, end))
      }
    }
    case "mobile" :: "api" :: "users" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password)
        val users = User.findAll(By(User.company, customer.company)).map( (u) =>
          JsObj(
                ("name",u.name.is),
                ("id",u.id.is),
                ("group",u.group.is)
            )
        )
        JsArray(users)
      }
    }
    case "mobile" :: "api" :: "schedule" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        user <- S.param("user") ?~ "user parameter missing" ~> 400
        date <- S.param("date") ?~ "date parameter missing" ~> 400
        hour_start <- S.param("hour_start") ?~ "customer parameter missing" ~> 400
        activity <- S.param("activity") ?~ "activity parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password)
        val customerAsUser = User.findByKey(customer.id.is).get
        val userObj = User.findByKey(user.toLong).get
        AuthUtil << customerAsUser
        AuthUtil << userObj.unit.obj.get

        var treatment = TreatmentService.factoryTreatment("", customer.id.is.toString, user, date, hour_start, hour_start, "", "Agendamento Online","", true).get
        TreatmentService.addDetailTreatmentWithoutValidate(treatment.id.is, activity.toLong, 0l, 0l, 0l)
        treatment.markAsPreOpen
        treatment.save
        JInt(1)
      }
    }    
    case "mobile" :: "api" :: "activities" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        user <- S.param("user") ?~ "user parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password)
        val userObj = User.findAll(By(User.company, customer.company),By(User.id, user.toLong))(0)
        val activities = TreatmentService.activitiesMapByUser(userObj).map( (u) =>
          JsObj(
                ("name",u.name.is),
                ("id",u.id.is)
            )
        )
        /*
        val calendar = java.util.Calendar.getInstance
        val today = calendar.getTime
        calendar.add(java.util.Calendar.MONTH, 1)
        val nextMonth = calendar.getTime
        val dates = Scalendar(today.getTime) to Scalendar(nextMonth.getTime) by(1.day)
        dates.foreach( (d) => {

        })
        */
        JsObj(
          ("interval", userObj.company.obj.get.calendarInterval.is),
          ("start", userObj.company.obj.get.calendarStart.is),
          ("end", userObj.company.obj.get.calendarEnd.is),
          ("activities", JsArray(activities))
        )
      }
    }    
    case req if(req.requestType.method == "OPTIONS") => {
      JInt(1);
    }    
  }
}