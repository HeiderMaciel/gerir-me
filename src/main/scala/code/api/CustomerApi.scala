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

object CustomerApi extends RestHelper {

		serve {
			// parece que não usa - rigel 01/2017
			case "customer" :: "search_full" :: Nil JsonGet _ => {
				for {
					name <- S.param("name") ?~ "name parameter missing" ~> 400
					phone <- S.param("phone") ?~ "phone parameter missing" ~> 400
					email <- S.param("email") ?~ "email parameter missing" ~> 400
					createdAfter <- S.param("created_after")
					purchasedAfter <- S.param("purchased_after")
					page <- S.param("page")
				} yield {				
					JsArray(Customer.searchCustomer(AuthUtil.company.id.is, name, phone, email, 30, page.toInt).map((c)=>{
						JsObj(("name", c.name.is), ("id", c.id.is), ("email", c.email.is))
					}))
				}
			}			
			case "customer" :: "search" :: id :: Nil Post _ =>{
				val ac = Customer.findByKey (id.toLong).get
				JString(ac.name)
			}
			
			case "customer" :: "search" :: Nil JsonGet _ => {
				for {
					name <- S.param("name") ?~ "name parameter missing" ~> 400
					phone <- S.param("phone") ?~ "phone parameter missing" ~> 400
//					email <- S.param("email") ?~ "email parameter missing" ~> 400
					page <- S.param("page")
					user <- S.param("user")
				} yield {				
						val email = S.param("email").openOr("")
						JsArray(Customer.searchCustomerAsDto(AuthUtil.company.id.is, name, phone, email, 30, page.toInt, user.toBoolean).map((c)=>{
						JsObj(("name", c.name), ("id", c.id), ("email", c.email), ("obs", c.obs), ("phone", c.phone), ("isemployee", c.isemployee))
					}))
				}
			}
			case "customer" :: "messages":: customerId :: Nil JsonGet _ => {
				JsArray(Customer.findByKey(customerId.toLong).get.alerts_messages.map((ms) => JsObj(("message", ms))))
			}
			case "customer" :: "unification":: customerSrc :: customerDest :: bptype :: Nil JsonGet _ => {
				val customerSource = Customer.findAllInCompany(By(Customer.id, customerSrc.toLong))(0)
				val customerDestination = Customer.findAllInCompany(By(Customer.id, customerDest.toLong))(0)
				try {
					if (customerSource.valueInAccount != 0.0 ||
						customerSource.valueInPoints != 0.0 ||
						customerDestination.valueInAccount != 0.0 ||
						customerDestination.valueInPoints != 0.0) {
						if (!AuthUtil.user.isFinancialManager) {
					      throw new Exception("Para unificar clientes com saldo diferente de zero, seja em moeda corrente ou pontos fidelidade, você precisa ter permissão de Administrador ou Gerente Financeiro")
						}
					}
					Customer.unificCustomer(customerSource, customerDestination, bptype)
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "customer" :: "next_id" :: Nil JsonGet _ => {
				val customer = Customer
				customer.company.set(AuthUtil.company.id.is)
				JsObj(("id", customer.nextIdForCompany))
			}

			case "customer" :: "del_image" :: Nil Post _ => {
				try { 
					def tdId:String = S.param("tdid") openOr "0"
					val ac = ImageCustomer.findByKey (tdId.toLong).get
					ac.delete_!
					JInt(1)
				} catch {
				  case e: Exception => JString(e.getMessage)
				}			
			}
		}
}
