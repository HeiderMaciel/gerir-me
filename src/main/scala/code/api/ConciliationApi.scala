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


object ConciliationApi extends RestHelper with ReportRest with net.liftweb.common.Logger  {

		serve {

			case "accountpayable" :: "consolidate" :: Nil Post _ => {
				val ids = S.param("ids").get
				ids.split(",").map(_.toLong).map((l:Long) => {
					val ap = AccountPayable.findByKey(l).get
					if (!ap.paid_?) {
						ap.paid_? (true);
					}
					ap.makeAsConsolidated
				})
				JInt(1)
			}

			case "accountpayable" :: "mark_as_conciliated" :: Nil Post _ => {
				try {
					val ids = S.param("ids").get
					ids.split(",").map(_.toLong).map((l:Long) => {
						val ap = AccountPayable.findByKey(l).get
						if (!ap.paid_?) {
							ap.paid_? (true);
						}
						ap.makeAsConciliated
					})
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "changeofx" :: idofx :: customer :: exercisedate :: obs :: categ :: Nil JsonGet _ => {
				try{
					val ap = AccountPayable.findByKey(idofx.toLong).get
					if (!ap.paid_?) {
						ap.paid_? (true);
					}
					ap.exerciseDate(Project.strToDateOrToday(exercisedate))
					ap.obs(obs)
					ap.user (customer.toLong)
					ap.category (categ.toLong)
					ap.toConciliation_? (false);
					ap.save // para dar erro se for o caso
					ap.makeAsConciliated // aqui o save é partiallysercure
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "conciliate" :: id :: Nil JsonGet _ => {
				try{
					val ap = AccountPayable.findByKey(id.toLong).get
					if (!ap.paid_?) {
						ap.paid_? (true);
					}
					ap.makeAsConciliated
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "conciliateofx" :: 
				id :: idofx :: aggreg :: partial :: Nil JsonGet _ => {
				try{
					AccountPayable.conCilSol (id,idofx,(aggreg == "true"), 1, (partial == "true"))
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "consolidate" :: id :: Nil JsonGet _ => {
				try{
					val ap = AccountPayable.findByKey(id.toLong).get
					if (!ap.paid_?) {
						ap.paid_? (true);
					}
					ap.makeAsConsolidated
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}
			case "accountpayable" :: "consolidateofx" :: 
				id :: idofx :: aggreg :: partial :: Nil JsonGet _ => {
				try{
					AccountPayable.conCilSol (id,idofx,(aggreg == "true"), 2, (partial == "true"));
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "consolidateTotal" :: paymentStart :: paymentEnd :: accountId :: value :: Nil JsonGet _ => {
				//try{
					val ap = AccountPayable.consolidate(
						accountId.toLong, value.replaceAll (",",".").toDouble, 
						Project.strOnlyDateToDate(paymentStart),
						Project.strOnlyDateToDate(paymentEnd));
					JInt(1)
				//} catch {
				//	case e:Exception => JString(e.getMessage)
				//}
			}
		}
}
