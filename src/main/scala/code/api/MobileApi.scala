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
          OrderBy (User.name, Ascending)).map( (u) => {
          var strGroup = if (u.userGroupName != "") {
            " - " + u.userGroupName
          } else {
            ""
          }
          JsObj(
                ("name",u.name.is + strGroup),
                ("id",u.id.is),
                ("group",u.group.is)
            )}
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

    case "mobile" :: "api" :: "reschedule" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        company <- S.param("company") ?~ "company parameter missing" ~> 400
        trid <- S.param("trid") ?~ "trid parameter missing" ~> 400
        status <- S.param("status") ?~ "status parameter missing" ~> 400
      } yield {
        val customer = Customer.login(email, password, company)
        val customerAsUser = User.findByKey(customer.id.is).get
        AuthUtil << customerAsUser

        if (status == "7") { // preopen
          TreatmentService.delete(trid)
        } else if (status == "0") { // open
          var treatment = TreatmentService.markAsReSchedule(trid.toLong)
        } else {
          println ("vaiiiii ========= Status não previsto mobile api delete ")
        }
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

        JsObj(
          ("activities", JsArray(activities))
        )
      }
    }

    case "mobile" :: "api" :: "hoptions" :: Nil Post _ => {
      for {
        email <- S.param("email") ?~ "email parameter missing" ~> 400
        password <- S.param("password") ?~ "password parameter missing" ~> 400
        company <- S.param("company") ?~ "company parameter missing" ~> 400
        user <- S.param("user") ?~ "user parameter missing" ~> 400
        date <- S.param("date") ?~ "date parameter missing" ~> 400
        activity <- S.param("activity") ?~ "activity parameter missing" ~> 400
      } yield {

        val date1 = Project.strToDateOrToday(date)
        val activityObj = Activity.findByKey(activity.toLong).get
        val customer = Customer.login(email, password, company)
        val userObj = User.findAll(
          By(User.company, customer.company),
          By(User.id, user.toLong))(0)

        val hoptions = AgHM.findAll(
        BySql(""" 
          agh between ? and ?
          and (agh > to_number (to_char (now(),'hh24'),'99') or date(?) > now())
          and agm % ? = 0
          and 1 > (select count (1) from treatment tr where tr.user_c = ? and tr.company = ?
          and tr.status not in (5,4,8,1) 
          and (to_char (date(?), 'YYYY-MM-DD') || ' ' || trim (to_char (agh, '09'))||':'|| trim (to_char (agm,'09')))::timestamp 
          between tr.start_c and tr.end_c- (1 * interval '1 minute'))

          and 1 > (select count (1) from treatment tr where tr.user_c = ? and tr.company = ?
          and tr.status not in (5,4,8,1) 
          and tr.start_c 
          between (to_char (date(?), 'YYYY-MM-DD') || ' ' || trim (to_char (agh, '09'))||':'|| trim (to_char (agm,'09')))::timestamp and 
          (to_char (date(?), 'YYYY-MM-DD') || ' ' || trim (to_char (agh, '09'))||':'|| trim (to_char (agm,'09')))::timestamp + ((? - 1) * interval '1 minute'))

          and 1 > (select count (1) from busyevent tr where tr.user_c = ? and tr.company = ?
          and tr.deleted = false
          and (to_char (date (?), 'YYYY-MM-DD') || ' ' || trim (to_char (agh, '09'))||':'|| trim (to_char (agm,'09')))::timestamp 
          between tr.start_c and tr.end_c- (1 * interval '1 minute'))

          and 1 > (select count (1) from busyevent tr where tr.user_c = ? and tr.company = ?
          and tr.deleted = false
          and tr.start_c 
          between (to_char (date(?), 'YYYY-MM-DD') || ' ' || trim (to_char (agh, '09'))||':'|| trim (to_char (agm,'09')))::timestamp and 
          (to_char (date(?), 'YYYY-MM-DD') || ' ' || trim (to_char (agh, '09'))||':'|| trim (to_char (agm,'09')))::timestamp + ((? - 1) * interval '1 minute'))

           """, 
        IHaveValidatedThisSQL("1=1","01-01-2012 00:00:00"),
        userObj.company.obj.get.calendarStart.is,
        userObj.company.obj.get.calendarEnd.is-1,
        date1,
        userObj.company.obj.get.calendarInterval.is,
        user.toLong,
        customer.company,
        date1,

        user.toLong,
        customer.company,
        date1,
        date1,
        activityObj.durationMin,

        user.toLong,
        customer.company,
        date1,

        user.toLong,
        customer.company,
        date1,
        date1,
        activityObj.durationMin
        )
          ).map( (u) => {
            JsObj(
                ("hour",u.agh.is),
                ("min",u.agm.is)
            )
          } 
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
//          ("activities", JsArray(activities)),
          ("hoptions", JsArray(hoptions))
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
        doc <- S.param("doc") ?~ "doc parameter missing" ~> 400
      } yield {
        val companyLong = Company.calPubCompany (company)
        var ac = Customer.findAll (By(Customer.company, companyLong),
          Like (Customer.email, "%"+email+"%"));
        if (ac.length > 0) {
          JString("Email já cadastrado, use a opção Esqueci minha Senha\n\n ou entre em contato com o estabelecimento")
        } else {
          //println ("vaiii ================== name clear " + BusinessRulesUtil.clearString(name))
          //println ("vaiii ================== mobile clear " + BusinessRulesUtil.clearString(mobilephone))
          var ac1 = Customer.findAll (By(Customer.company, companyLong),
            By (Customer.search_name, BusinessRulesUtil.clearString(name)),
            By (Customer.document, doc));
          if (ac1.length > 0) {
            if (ac1 (0).email == "" &&
                phone != "" && 
                ((ac1 (0).phone == phone 
                || ac1 (0).mobilePhone == phone)) ||
                (mobilephone != "" && (ac1 (0).phone == mobilephone 
                || ac1 (0).mobilePhone == mobilephone))) {
              // nome igual / phone ou mobile igual e // email ""
              // atualiza o email em cliente que já existia e fala que cadastrou
              ac1 (0).email (email).save
              JString("1")
            } else {
              if (doc != "") {
                JString("Seu nome e cpf já estão cadastrados, verifique seus telefones e tente novamente\n\nou entre em contato com o estabelecimento")
              } else {
                JString("Seu nome já está cadastrado, verifique seus telefones ou informe um cpf e tente novamente\n\nou entre em contato com o estabelecimento")
              }
            }
          } else {
            try {
              var customer = Customer.create
              customer.company(companyLong).
              name (name).
              email (email).
              phone (phone).
              mobilePhone (mobilephone).
              document (doc).
              obs ("agenda online").
              createdBy(5). // usuario da 1 Agenda Online
              updatedBy(5).
              save
              JString("1")
            } catch {
              case e:Exception => JString(e.getMessage)
            }
          }
        }
      }
    }    
    case req if(req.requestType.method == "OPTIONS") => {
      JInt(1);
    }    
  }
}