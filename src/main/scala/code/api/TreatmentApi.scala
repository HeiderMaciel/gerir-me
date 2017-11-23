package code
package api

import code.model._
import code.util._
import code.service._

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
import java.util._

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object TreatmentApi extends RestHelper with net.liftweb.common.Logger {

	serve {
		case "treatment" :: Nil Put _ => {
			try{
				def id = S.param("id") openOr "0"
				def customerCode = S.param("customer") openOr "0"
				def userCode  = S.param("user") openOr "0"
				def date  = S.param("date") openOr "0"
				def hour_start  = S.param("hour_start") openOr "0"
				def hour_end  = S.param("hour_end") openOr "0"
				def command = S.param("command") openOr "0"
				def obs  = S.param("obs") openOr ""
				def conflit = S.param("treatmentConflit") openOr ""
				def force = S.param("force") openOr ""

				TreatmentService.factoryTreatment(id,customerCode,userCode,date,hour_start,hour_end,command, obs, conflit, force.toBoolean) match {
					case Full(t) => JsObj(("status", "success"), ("start",t.start.is.getTime), ("customerId", t.customer.is), ("id",t.id.is), ("command",t.command.is),("end",t.end.is.getTime))
					case _ => JInt(0)
				}
			}catch{
				case e:RuntimeException => {
					error(e) 
					JsObj(("status","error"), ("message", e.getMessage))
				}
			}
		}
		case "treatment" :: id :: Nil Delete _ =>{
/*
			TreatmentService.delete(id)
			JInt(1)
*/
			try {
				TreatmentService.delete(id)
				JInt(1)
			}catch{
				case e:RuntimeException  => {
					JsObj(("status","error"),("message",e.getMessage))
				}
				case e:Exception => {
					e.printStackTrace
					JString(e.getMessage())
				}
			}
		}	
		case "treatment" :: id :: Nil Post _ =>{
			try{
				def user:String = S.param("user") openOr "0"
				def start:String = S.param("start") openOr "0"
				def end:String = S.param("end") openOr "0"
				def status: String = S.param("status") openOr ""
				def validate = (S.param("validate") openOr "true").toBoolean
				def statstr:String = if (status.toLowerCase == "open") {
					"0"
				} else if (status.toLowerCase == "missed") {
					"1"
				} else if (status.toLowerCase == "arrived") {
					"2"
				} else if (status.toLowerCase == "ready") {
					"3"
				} else if (status.toLowerCase == "paid") {
					"4"
				} else if (status.toLowerCase == "delete") {
					"5"
				} else if (status.toLowerCase == "confirmed") {
					"6"
				} else if (status.toLowerCase == "preopen") {
					"7"
				} else if (status.toLowerCase == "rescheduled") {
					"8"
				} else if (status.toLowerCase == "budget") {
					"9"
				} else {
					status
				}
				TreatmentService.updateTreatmentHours(id,user.toLong,Project.strToDate(start),
					Project.strToDate(end), statstr.toInt, validate)
				JInt(1)
			}catch{
				case e:Exception => {
					e.printStackTrace
					JString(e.getMessage())
				}
			}
		}
//-------------------------------events
		case "userEvent" :: id :: Nil Delete _ =>{
			try{
				BusyEvent.findByKey(id.toLong).get.delete_!
			}catch{
				case e:Exception => {
					try {
						// se já tem um excluído logicamente não deixa excluir outro
						// pq duplica a chave
						//
						// Aqui faz deleção física mesmo
						//
						BusyEvent.findByKey(id.toLong).get.insecureDelete_!
						JString(e.getMessage())
					}catch{
						case e:Exception => {
							JString(e.getMessage())
						}
					}
				}
			}
			JInt(1)
		}	
		case "userEvent" :: id :: Nil Post _ =>{
			try{
				def user:String = S.param("user") openOr "0"
				def start:String = S.param("start") openOr "0"
				def end:String = S.param("end") openOr "0"
				TreatmentService.updateEventHours(id,user.toLong,Project.strToDate(start),Project.strToDate(end))
				JInt(1)
			}catch{
				case e:Exception => JString(e.getMessage())
			}
		}
		case "treatment" :: "util" :: "revert_prices" :: Nil Post _ =>{
			for {
				data <- S.param("treatments") ?~ "treatments parameter missing" ~> 400
			} yield {
				data.split(",").foreach((t) => {
					TreatmentService.revertPrices(t.toLong)
				})
				JInt(1)
			}
		}
		case "treatment" :: "util" :: "scheduling" :: Nil Post _ =>{
			for {
				data <- S.param("data") ?~ "data parameter missing" ~> 400
			} yield {
				val json = parse(data)
				val a = json.extract[scala.List[TreatmentScheduleDto]]
				JsArray(a.map((t)=>{
					try{
						val tempt = TreatmentService.factoryTreatment("", t.customer, t.user,t.start.split(" ")(0), t.start.split(" ")(1), t.start.split(" ")(1),"0")
						var tempd = TreatmentService.addDetailTreatmentWithoutValidate(tempt.get.id, t.service.toLong, 0l /* auxiliar */,
						0l /* animal */, "" /* tooth */, 0l /*offsale*/)
						tempt.get.end(Project.strToDate(t.end)).obs(t.obs).insecureSave
						if (t.amount != "" && t.amount != "1") {
							tempd.get.amount(t.amount.toDouble).price(tempd.get.price*t.amount.toDouble).save
						}
						if (t.price != "" && t.price != "0") {
							tempd.get.price(t.price.toDouble*t.amount.toDouble).save
						}
						JsObj(("id",t.id),("valid",true))
					}catch{
						case e:Exception =>{
							JsObj(("id",t.id),("valid",false),("message",e.getMessage))
						}
					}
				}))
			}
		}
	}
}
case class TreatmentScheduleDto(id:Long,start:String, end:String, service:String, customer:String, user:String, obs:String, price:String, amount:String)
object TreatmentDetailsApi extends RestHelper {
	serve{
	
		/*
		case "treatment" :: "confirm" :: id :: Nil Get _ =>{
			TreatmentService.markAsArrived(id.toLong)
			/*<script type="text/javascript">
				alert("Atendimento confirmado com sucesso!")
				window.close();
			</script>*/
			JInt(1)
		}*/		
		case "treatment" :: "detail" :: id :: Nil Delete _ =>{
			TreatmentService.deleteDetail(id.toLong)
			JInt(1)
		}
		case "treatment_detail" :: Nil Post _ =>{
			try{
				val activityCode = S.param("activity") openOr "0"
				val auxiliar = S.param("auxiliar") openOr "0"
				val animal = S.param("animal") openOr "0"
				val tooth = S.param("tooth") openOr ""
				val offsale = S.param("offsale") openOr "0"
				val id = S.param("id") openOr "0"
				val validate = S.param("validate") openOr "false"
				if(validate.toBoolean) {
					TreatmentService.addDetailTreatment(id.toLong,activityCode.toLong, 
						auxiliar.toLong, animal.toLong, tooth, offsale.toLong)
				} else {
					TreatmentService.addDetailTreatmentWithoutValidate(id.toLong,
						activityCode.toLong, auxiliar.toLong, animal.toLong, tooth, offsale.toLong)
				}
				JInt(1)
			}catch{
				case e:Exception => {
					e.printStackTrace
					JString(e.getMessage())	
				}
				
			}
		}
		/*
		case "treatment" :: id :: "details" :: Nil JsonGet _ =>{
			val treatment = TreatmentService.loadTreatment(id)
			JsArray(treatment.details.map( (td) =>{ detailJson(td) }))
		}*/

		case "treatment" :: "by_customer" :: customerStr :: dateStr :: "details" :: Nil JsonGet _ =>{
			val customer = Customer.findByKey(customerStr.toLong).get
			val date = Project.strToDateOrToday(dateStr)
			val treatments = TreatmentService.loadTreatmentByCustomer(customer, date)
			val details = treatments.map(_.details).foldLeft(scala.List[code.model.TreatmentDetail]())(_:::_)
			JsArray(details.map( (td) =>{  detailJson(td) }))
		}
	}

	def detailJson (td:TreatmentDetail) = {
		val treatment = td.treatment.obj.get
					JsObj(
						("user",td.userName),
						("auxiliar",td.auxiliarShortName),
						("auxiliarId",td.auxiliar.is),
						("animal",td.animalShortName),
						("animalId",td.animal),
						("tooth",td.tooth),
						("offsale",td.offsaleShortName),
						("unit",td.unitShortName),
						("activity",td.nameActivity),
						("treatment",td.treatment.is),
						("start",Project.dateToStrJs(treatment.start.is)),
						("end",Project.dateToStrJs(treatment.end.is)),
						("status",treatment.status.is.toString),
						("id",td.id.is),
						("auditstr",td.auditStr)
					)
	}	
}