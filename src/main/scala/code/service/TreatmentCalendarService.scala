package code
package service

import net.liftweb.common.{Box,Full,Empty}
import net.liftweb._ 
import mapper._
import code.model._
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.actors._
import java.util.Date
import java.util.Calendar
import net.liftweb.json._
import net.liftweb.common._
import net.liftweb.util._
import http.js._
import JE._



object TreatmentCalendarService {
    def  treatmentsForCalendarAsJson(company:Company, cUnit:CompanyUnit,start:Date, end:Date) = {
//        val where = " tr.user_c in ( select id from business_pattern u where u.unit = ? ) "
        val where = " (tr.user_c in ( select id from business_pattern u where u.unit = ? ) " +
                   "or tr.user_c in (select user_c from usercompanyunit where unit = ?)) "
        treatmentsForCalendarQueryToJson(where,company.id.is::cUnit.id.is::cUnit.id.is::cUnit.id.is::start::end::Nil)
    }

    def treatmentsForCalendarAsJson(company:Company, cUnit:CompanyUnit, customer:Customer,start:Date, end:Date) = {
        val where = " tr.customer=? "
        treatmentsForCalendarQueryToJson(where,company.id.is::cUnit.id.is::customer.id.is::start::end::Nil)
    }    

    def treatmentsForCalendarAsJson(company:Company, cUnit:CompanyUnit, user:User,start:Date, end:Date) = {
        val where = " tr.user_c=? "
        treatmentsForCalendarQueryToJson(where,company.id.is::cUnit.id.is::user.id.is::start::end::Nil)
    }

    def treatmentsForCalendarAsJson(company:Company, cUnit:CompanyUnit, group:UserGroup,start:Date, end:Date) = {
//        val where = " tr.user_c in (select id from business_pattern u where u.group_c = ?) "
        val where = " (tr.user_c in ( select id from business_pattern u where u.group_c = ? ) " +
                   "or tr.user_c in (select user_c from userusergroup where group_c = ?)) "
        treatmentsForCalendarQueryToJson(where,company.id.is::cUnit.id.is::group.id.is::group.id.is::start::end::Nil)
    }

/*
    def treatmentsForCalendarAsJson(company:Company, cUnit:CompanyUnit, group:UserGroup,start:Date, end:Date) = {
        val where = " tr.user_c in (select id from business_pattern u where u.group_c = ?) and tr.unit=? "
        treatmentsForCalendarQueryToJson(where,company.id.is::cUnit.id.is::group.id.is::cUnit.id.is::start::end::Nil)
    }     
*/
    private def treatmentsForCalendarQueryToJson(where:String, params:List[Any]) = {
        //info(SQL_TREATMENT_TO_CALENDAR_DATA+where+Treatment.SQL_VALID_TREATMENT)
        val query = SQL_TREATMENT_TO_CALENDAR_DATA+where+Treatment.SQL_VALID_TREATMENT

        var icon = "";
        val r = DB.performQuery(query, params)
        r._2.map(
            (p:List[Any])=> {
            if (p(16).asInstanceOf[Double]>0) {
                icon = "warning.png"
              } else {
                icon = ""
              }
            JsObj(
                ("title", p(7).toString+"<br/>"+p(0).toString),
 //               ("title", p(7).toString+"<br/>"+p(0).toString),
                ("customerThumb",  Props.get("photo.urlbase").get+"business_pattern/"+p(13).toString),
                ("obs", p(1).toString),
                ("start", Project.dateToStrJs(p(2).asInstanceOf[Date])),
                ("end", Project.dateToStrJs(p(3).asInstanceOf[Date])),
                ("userId",p(4).asInstanceOf[Long]),
                ("userName",p(12).asInstanceOf[String]),
                ("command", p(5).toString),
                ("customerId", p(6).asInstanceOf[Long]),
                ("customerName", p(7).toString),
                ("id", p(8).asInstanceOf[Long]),
                ("treatmentConflit", p(9).asInstanceOf[Long]),
                ("status", p(10).asInstanceOf[Long]),
                ("status2", p(14).asInstanceOf[Long]),
                ("color", p(15).toString), //"#C7172D"), // trazer aqui cor do bloqueio/serviço
                ("icon", icon), 
                ("noConflits", p(11).asInstanceOf[Long])

                )
            }
            )        
    }
    def str_detail:String =  if (!AuthUtil.company.calendarShowActivity_?) {
      "''" 
    } else {
      " detailTreatmentAsText "
    }
    def str_cod:String =  if (!AuthUtil.company.calendarShowId_?) {
      "''" 
    } else {
        if (PermissionModule.anvisa_?) {
          "trim (c.barcode) "
        } else {
          "trim (to_char (customer,'9999999')) "
        }   
    }
    def str_phone:String = if (!AuthUtil.company.calendarShowPhone_?) {
        "''"
      } else {
        if (AuthUtil.?) {
          if (!AuthUtil.user.isCustomer) {
            "''"
          } else {
            " trim (c.mobile_phone || ' ' || c.phone || ' ' || c.email_alternative) "
          }
        } else {
            // 13/06/2017 - rigel 
            // login pelo customer não tem user
            "''"
        }
      }

    def str_unit:String = if (!AuthUtil.company.calendarShowDifUnit_?) {
      " AND tr.unit = ? "
      } else {
      " AND (tr.unit = ? or 1 = 1) "
    }
    // trim (substr (c.short_name,1,15)) diminuir o nome na agenda pode ser uma opção
    // no short tratar qd quebrar nome no meio e cortar na pos branca anterior
    def SQL_TREATMENT_TO_CALENDAR_DATA = 
      """ SELECT """ + str_detail + """, tr.obs, start_c, end_c, user_c, 
          command, customer, trim (""" + str_cod + 
          """ || ' ' || c.short_name || ' ' || """ + str_phone + """), tr.id, tr.treatmentConflit, tr.status, 
          --(SELECT count(1) FROM treatment tt
          --WHERE tr.id = tt.treatmentConflit) 
          0 AS conflits,
          u.name, c.imagethumb, tr.status2, coalesce (pr.color, ''), c.valueinaccount 
          FROM treatment tr
          INNER JOIN business_pattern c on(tr.customer=c.id)
          INNER JOIN business_pattern u on(tr.user_c=u.id)
          left JOIN treatmentdetail td on(td.treatment=tr.id and 
            td.id = (select min (td1.id) from treatmentdetail td1 where td1.treatment = tr.id))
          left join product pr on pr.id = td.activity
          WHERE tr.showincalendar = TRUE
            AND tr.company = ?
            """ + str_unit + """
            AND
      """
}

