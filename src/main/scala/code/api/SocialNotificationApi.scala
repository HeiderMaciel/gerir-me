package code
package api

import code.model._
import code.util._
import code.service._
import code.daily._

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
//implicit val formats = DefaultFormats // Brings in default date formats etc.

object SocialNotificationApi extends RestHelper with net.liftweb.common.Logger  {
	val DAY_IN_MILESECOUNDS = 86400000;
	serve {				
		case "social" :: "treatments" ::  "notify_customer" :: id :: Nil JsonGet _ =>{
			try {
				TreatmentService.sendTreatmentsEmailCustomer(id.toLong)
				JInt(1)
			} catch {
				case e:RuntimeException => {
					error(e) 
					JsObj(("status","error"), ("message", e.getMessage))
				}
			}
		}

		case "social" :: "treatments" ::  "notify_user" :: id :: Nil JsonGet _ =>{
			try {
				TreatmentService.sendTreatmentsEmailUser(id.toLong)
				JInt(1)
			} catch {
				case e:RuntimeException => {
					error(e) 
					JsObj(("status","error"), ("message", e.getMessage))
				}
			}
		}

		case "social" :: "treatments" ::  "rank_customer" :: id :: Nil JsonGet _ =>{
			try {
				TreatmentService.sendTreatmentsRankEmailCustomer(id.toLong)
				JInt(1)
			} catch {
				case e:RuntimeException => {
					error(e) 
					JsObj(("status","error"), ("message", e.getMessage))
				}
			}
		}

		case "social" :: "treatments" ::  "email_customer" :: Nil Post _ =>{
	      def id = S.param("id") openOr ""
	      def mail = S.param("body") openOr ""
	      def subject = S.param("subject") openOr ""
			DailyReport.sendMailBp(id.toLong, subject, mail)
			JInt(1)
		}
	}
}