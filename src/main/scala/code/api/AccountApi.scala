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


object AccountApi extends RestHelper with ReportRest with net.liftweb.common.Logger  {
		val FOREVER = 1;

		val PARCELED = 2;

		var apAuxId = 0l;

		serve {
			case "account" :: "report" :: "out_of_cacashier" :: Nil Post _ =>{
				for{
					cashier <- S.param("cashier") ?~ "cashier parameter missing" ~> 400
				}yield{
					S.param("isIdForCompany") match {
						case Full(p) => {
							toResponse(AccountPayable.SQL_REPORT_CASHIER_OUTS,List(AuthUtil.company.id.is, Cashier.findOpenCashierByIdAndCompany(cashier.toInt).id.is))
						}
						case _ => {
							toResponse(AccountPayable.SQL_REPORT_CASHIER_OUTS,List(AuthUtil.company.id.is, cashier.toLong))
						}
					}
				}
			}
			case "account" :: "category" :: "add" :: Nil Post _ => {
				for {
					name <- S.param("name") ?~ "name parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					color <- S.param("color") ?~ "color parameter missing" ~> 400
					typeMovement <- S.param("typeMovement") ?~ "typeMovement parameter missing" ~> 400
					userAssociated <- S.param("userAssociated") ?~ "typeMovement parameter missing" ~> 400
					//allowCashierOut <- S.param("allowCashierOut") ?~ "allowCashierOut parameter missing" ~> 400
					//("allowCashierOut",c.allowCashierOut.is)
				} yield {
					JBool(AccountCategory.createInCompany.userAssociated(userAssociated.toBoolean).typeMovement(typeMovement.toInt).name(name).obs(obs).color(color).save)
				}
			}

			case "account" :: "category" :: "remove" :: id :: Nil JsonGet _ => {
				try{
					val c = AccountCategory.findByKey(id.toLong).get
					c.delete_!
					JInt(1)
				} catch {
					case _ => JInt(0)
				}
			}

			case "account" :: "category" :: "edit" :: id :: Nil Post _ => {
				val c = AccountCategory.findByKey(id.toLong).get
				for {
					name <- S.param("name") ?~ "name parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					color <- S.param("color") ?~ "color parameter missing" ~> 400
				} yield {
					JBool(c.name(name).obs(obs).color(color).save)
				}
			}

			case "account" :: "category" :: "list" :: Nil JsonGet _ => {
				JsArray(AccountCategory
					.findAllInCompany
					(/*By(AccountCategory.parent_?,false),*/
						OrderBy(AccountCategory.maxTreeNode, Descending),
						OrderBy(AccountCategory.orderInReport, Ascending))
						.map((c) => {
							var tpm = if (c.typeMovement.is == 0) {
								" +"
							} else if (c.typeMovement.is == 1) {
								" -"
							} else {
								" t"
							}
							if (c.parent_?) {
								tpm = tpm + " (t)"
							}
							JsObj(
							("name",c._treeLevelstr + c.name.is + tpm),
							("obs",c.obs.is),
							("color",c.color.is),
							("id",c.id.is),
							("isparent",c.parent_?.is),
							("typeMovement",c.typeMovement.is),
							("userAssociated", c.userAssociated.is))
						}
					)
				)
			}

			case "account" :: "category" :: "list" :: "all" :: Nil JsonGet _ => {
				JsArray(AccountCategory.findAllInCompany(OrderBy(AccountCategory.name, Ascending)).map((c) => {
							var tpm = if (c.typeMovement.is == 0) {
								" +"
							} else if (c.typeMovement.is == 1) {
								" -"
							} else {
								" t"
							}
							if (c.parent_?) {
								tpm = tpm + " (t)"
							}
						JsObj(
							("name",c.name.is + tpm),
							("obs",c.obs.is), 
							("color",c.color.is),
							("id",c.id.is), 
							("typeMovement",c.typeMovement.is), 
							("userAssociated", c.userAssociated.is)
						)
					}
					))
			}

			case "account"  :: "list" :: Nil JsonGet _ => {
				JsArray(Account.findAllInCompany(OrderBy(Account.name, Ascending)).map((c) => JsObj(("id",c.id.is),("name",c.name.is),("value",c.value.is),("allowCashierOut",c.allowCashierOut_?.is))))
			}

		}
}
