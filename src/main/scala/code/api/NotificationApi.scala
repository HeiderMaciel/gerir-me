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

object NotificationApi extends RestHelper with ReportRest {

  serve {
    case "notification" :: "message" :: Nil Put _ => {
      for {
        title <- S.param("title") ?~ "title parameter missing" ~> 400
        body <- S.param("body") ?~ "title parameter missing" ~> 400
        id <- S.param("id")
      } yield {
        val notification = {
          if (id != "") {
            NotificationMessage.findByKey(id.toLong).get
          } else {
            NotificationMessage.createInCompany
          }
        }
        notification.subject(title).message(body).save;
        JInt(1)
      }
    }
    case "notification" :: "message" :: id :: Nil Delete _ => {
      NotificationMessage.findByKey(id.toLong).get.delete_!
      JInt(1)
    }
    case "notification" :: "message" :: Nil JsonGet _ => {
      JsArray(NotificationMessage.findAllInCompany(OrderBy(NotificationMessage.subject, Ascending)).map((nm) => JsObj(("title", nm.subject.is), ("body", nm.message.is), ("id", nm.id.is))))
    }
    case "notification" :: "message" :: id :: Nil JsonGet _ => {
      val nm = NotificationMessage.findByKey(id.toLong).get
      JsObj(("title", nm.subject.is), ("body", nm.message.is), ("id", nm.id.is))
    }
    case "notification" :: "messagesend" :: customers_str :: message :: Nil JsonGet _ => {
      try{
          val customers: List[Customer] = if (customers_str == "all") {
            Customer.findAllInCompany
          } else if (customers_str == "company") {
            Customer.findAllInCompany(By (Customer.id, AuthUtil.user.id.is))
            //List(Customer.createInCompany.email(AuthUtil.user.email.is).name(AuthUtil.user.name.is))
          } else {
            customers_str.split(",").map((id) => {
              Customer.findByKey(id.toLong).get
            }).toList
          }
          val message_obj = NotificationMessage.findByKey(message.toLong).get
          customers.foreach((customer) => {
            if (customer.email.is != "") {
              var message_aux = Customer.replaceMessage (customer, message_obj.message.is)
/* - validando pode excluir maio/2016 rigel
              var message_aux = message_obj.message.is.replaceAll("##nome##", customer.name.is)
                  // tentei short_name search_name não consegui              
                  message_aux = message_aux.replaceAll ("##apelido##", customer.district.is)
                  message_aux = message_aux.replaceAll ("##prinome##", customer.firstName)
                  //println ("========== >>>  >>> " + message_aux)
*/
              val final_message = <span>{ Unparsed(message_aux) }</span>
              EmailUtil.sendMailToCustomer( AuthUtil.unit,AuthUtil.company, customer.email.is, final_message, message_obj.subject.is, customer.id.is)
            }
          })
          JInt(1)        
      }catch{
        case e:Exception =>{
          JString(e.getMessage)
        }
      }
    }
  }
}