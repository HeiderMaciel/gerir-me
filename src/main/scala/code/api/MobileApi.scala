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
        company <- S.param("company") ?~ "company parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password, company)
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
        company <- S.param("company") ?~ "company parameter missing" ~> 400
      } yield {
        Customer.login(email, password, company).asJs
      }
    }
    case "mobile" :: "api" :: "history" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        company <- S.param("company") ?~ "company parameter missing" ~> 400
        startDate <- S.param("startDate") ?~ "startDate parameter missing" ~> 400
        endDate <- S.param("endDate") ?~ "endDate parameter missing" ~> 400
      } yield {
        val start = Project.strOnlyDateToDate(startDate)
        val end = Project.strOnlyDateToDate(endDate)
        val customer = Customer.login(email, password, company)
        JsArray(customer.history(start, end))
      }
    }
    case "mobile" :: "api" :: "users" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        company <- S.param("company") ?~ "company parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password, company)
        val users = User.findAll(
          By(User.company, customer.company),
          By(User.userStatus, 1),
          By(User.showInCalendarPub_?, true),
          OrderBy (User.name, Ascending)).map( (u) =>
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
        company <- S.param("company") ?~ "company parameter missing" ~> 400
        user <- S.param("user") ?~ "user parameter missing" ~> 400
        date <- S.param("date") ?~ "date parameter missing" ~> 400
        hour_start <- S.param("hour_start") ?~ "customer parameter missing" ~> 400
        activity <- S.param("activity") ?~ "activity parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password, company)
        val customerAsUser = User.findByKey(customer.id.is).get
        val userObj = User.findByKey(user.toLong).get
        AuthUtil << customerAsUser
        AuthUtil << userObj.unit.obj.get

        var treatment = TreatmentService.factoryTreatment("", customer.id.is.toString, user, date, hour_start, hour_start, "", "Agendamento Online","", true).get
        TreatmentService.addDetailTreatmentWithoutValidate(treatment.id.is, 
          activity.toLong, 0l /* auxiliar */ , 0l /* animal */, "" /* tooth*/, 0l /* offsale */)
        treatment.markAsPreOpen
        treatment.save
        JInt(1)
      }
    }    
    case "mobile" :: "api" :: "activities" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        company <- S.param("company") ?~ "company parameter missing" ~> 400
        user <- S.param("user") ?~ "user parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password, company)
        val userObj = User.findAll(
          By(User.company, customer.company),
          By(User.id, user.toLong))(0)
        val activities = TreatmentService.activitiesMapByUser(userObj, true).map( (u) =>
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
    case "mobile" :: "api" :: "companyInfo" :: Nil Get _ => {

      def asJson (ac:Company):JsObj = JsObj(
              ("status","success"),
              ("name",ac.name.is),
              ("id",ac.id.is),
              ("thumb_web",ac.thumb_web ))

      for {
        company <- S.param("id") ?~ "id parameter missing" ~> 400
      } yield {
        val companyLong = Company.calPubCompany (company)
        val ac = Company.findByKey(companyLong).get
        asJson (ac)
      }
    }

    case "mobile" :: "api" :: "joinus" :: Nil Post _ => {
      for {
        company <- S.param("company") ?~ "company parameter missing" ~> 400
        name <- S.param("name") ?~ "name parameter missing" ~> 400
        mobilephone <- S.param("mobilephone") ?~ "mobilephone parameter missing" ~> 400
        phone <- S.param("phone") ?~ "phone parameter missing" ~> 400
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
      } yield {
        val companyLong = Company.calPubCompany (company)
        var ac = Customer.findAll (By(Customer.company, companyLong),
          Like (Customer.email, "%"+email+"%"));
        if (ac.length > 0) {
          println ("vaiiiii ======================= JA existe ")
          JString("Email já cadastrado use a opção Esqueci minha Senha")
        } else {
          var customer = Customer.create
          customer.company(companyLong).
          name (name).
          email (email).
          phone (phone).
          mobilePhone (mobilephone).
          password (password).
          obs ("agenda online").
          save
          JString("1")
        }
      }
    }    
    case req if(req.requestType.method == "OPTIONS") => {
      JInt(1);
    }    
  }
}