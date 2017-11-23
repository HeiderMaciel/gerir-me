package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

import net.liftweb._
import mapper._
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
//implicit val formats = DefaultFormats // Brings in default date formats etc.

object UtilApi extends RestHelper  with net.liftweb.common.Logger  {

	serve {
		case "crud" :: "banks" :: Nil Get _ =>{
			JsArray(Bank.findAll(OrderBy(Bank.short_name, Ascending)).map((bank)=>{
				JsObj(("name",bank.short_name.is), ("id",bank.id.is), ("long_name",bank.name.is))
			}
			))
		}
		case "crud" :: "civilstatuses" :: Nil Get _ =>{
			JsArray(CivilStatus.findAll(OrderBy(CivilStatus.name, Ascending)).map((civilstatus)=>{
				JsObj(("name",civilstatus.name.is), ("id",civilstatus.id.is))
			}
			))
		}
		case "crud" :: "states" :: Nil Get _ =>{
			JsArray(State.findAll(OrderBy(State.short_name, Ascending)).map((state)=>{
				JsObj(("name",state.short_name.is), ("id",state.id.is))
			}
			))
		}
		case "crud" :: "cities" :: Nil Get _ =>{
			val state = S.param("state") openOr "false"
			if(state != "false"){
				JsArray(City.findAll(
					By(City.state, state.toLong),
					OrderBy(City.name, Ascending)
				).map((city)=>{
					JsObj(("name",city.name.is), ("id",city.id.is))
				}))
			}else{
				JsArray(Nil)
			}
		}		
		case "crud" :: "teeths" :: Nil Get _ =>{
			JsArray(DomainTable.findAll(By(DomainTable.domain_name,"dente"),
				OrderBy(DomainTable.name, Ascending)).map((domaintable)=>{
				JsObj(("name",domaintable.name.is), ("id",domaintable.cod.is.toLong))
			}
			))
		}
	}
}

