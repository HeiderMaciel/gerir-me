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
import net.liftweb.mapper._ 

import java.util.Calendar
import java.util.HashMap
import java.sql.Connection
import java.sql.DriverManager

object CustomerAddonsApi extends RestHelper {
		
		serve {
			case "customer_api" :: "addons":: customerId :: Nil JsonGet _ => {

			    val today = Project.date_format_db.parse(Project.date_format_db.format(new Date()));

				val customer = Customer.findByKey(customerId.toLong).get
				val messages = JsArray(customer.alerts_messages.map((ms) => JsObj(("message", ms))))
				val deliverys =  JsArray(DeliveryDetail.findByCustomer(customer).map(
					(dd) => { JsObj(("id",dd.id.is), ("product", dd.product.is), ("price", dd.price.is.toDouble) ) }));

				val budgets =  JsArray(TreatmentDetail.findBudgetByCustomer(customerId.toLong).map(
					(bud) => { JsObj(("id",bud.id.is), ("product", bud.activity.is), ("price", bud.price.is.toDouble) ) }));

				val bpmonthlys =  JsArray(BpMonthly.findByCustomer(customer,today).map(
					(bpm) => { JsObj(("id",bpm.id.is), ("product", bpm.product.is), ("valueSession", bpm.valueSession.is.toDouble) ) }));

				JsObj(("messages", messages), ("deliverys", deliverys), ("offsale", customer.offsale.is), ("budgets", budgets), ("bpmonthlys", bpmonthlys))
			}	

			case "customer_api" :: "consideration" :: Nil Post _ => {
				for {
					customer <- S.param("customer") ?~ "customer parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					typeNotification <- S.param("type") ?~ "type parameter missing" ~> 400

				} yield {
					JBool(BusinessPatternConsideration.createInCompany.business_pattern(customer.toLong).message(obs).notify_type(typeNotification.toInt).date(new Date()).save)
				}
			}
			case  "customer_api" :: "consideration" :: id :: Nil Delete _ => {
				JBool(BusinessPatternConsideration.findByKey(id.toLong).get.delete_!)
			}
			case "customer_api" :: "bankaccount" :: Nil Post _ => {
				for {
					customer <- S.param("customer") ?~ "customer parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					bank <- S.param("bank") ?~ "bank parameter missing" ~> 400
					agency <- S.param("agency") ?~ "agency parameter missing" ~> 400
					account <- S.param("account") ?~ "account parameter missing" ~> 400

				} yield {
					JBool(BusinessPatternAccount.createInCompany.business_pattern(customer.toLong).obs(obs).bank(bank.toLong).agency(agency).account(account).save)
				}
			}
			case "customer_api" :: "bankaccount" :: id :: Nil Delete _ => {
				JBool(BusinessPatternAccount.findByKey(id.toLong).get.delete_!)
			}
		}

}