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
import net.liftweb.json.JsonAST.JObject
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object CompanyApi extends RestHelper with ReportRest with net.liftweb.common.Logger {

		serve {

			case "api" :: "company" :: "registry" :: Nil JsonPost json -> _ =>{
				try{
					val company = Company.createFromJson(json.asInstanceOf[JObject])
					company.save
					UserCreateActors ! company
					JInt(company.id.is.toInt)
				}catch{
					case e:RuntimeException => JString(e.getMessage)
					case _ => JString("Erro desconhecido registrando empresa, tente novamente!")
				}
			}	

			case "api" :: "company" :: "registryagain" :: Nil Post _ =>{
				try{
					def id:String = S.param("id") openOr "0"
					val company = Company.findByKey (id.toLong).get
					UserCreateActors ! company
					JInt(company.id.is.toInt)
				}catch{
					case e:RuntimeException => JString(e.getMessage)
					case _ => JString("Erro desconhecido atualizando empresa, tente novamente!")
				}
			}	
		}

}
