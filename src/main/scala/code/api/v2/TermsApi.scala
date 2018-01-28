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


object TermsApi extends  RestHelper with ReportRest with net.liftweb.common.Logger {
		serve {

			case "api" :: "v2" :: "terms" :: Nil JsonGet _ =>{
			 	JsArray(Terms.findAllInCompany.map((obj:Terms) =>{
			 		obj.asJsToSelect
			 	}))
			}		
			case "api" :: "v2" :: "terms" :: "user" :: Nil Post _ =>{
				try{
					def id:String = S.param("id") openOr "0"
					def msg:String = S.param("msg") openOr "0"
					var msg_aux = msg
					val ac = User.findByKey(id.toLong).get
					msg_aux = ac.replaceMessage (ac, msg_aux);
		 			JString (msg_aux)
				}catch{
					case e:RuntimeException => (ParamFailure(e.getMessage, 500)) : Box[JValue]
					case _ => JString("Error")
				}

			}
		}

}
