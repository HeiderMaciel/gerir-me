package code
package api

import code.model._
import code.service._
import code.util._
import code.comet._
import net.liftweb._
import dispatch._
import common._
import http._
import rest._
import json._
import mapper._ 
import http.js._
import JE._
import scala.xml._
import java.text.ParseException
import java.util.Date
 
object LocationApi extends RestHelper with net.liftweb.common.Logger {
	override implicit val formats = DefaultFormats // Brings in default date formats etc.

	serve {
		case "mobile" :: "sendSpeed" :: Nil  Post _ => {
			for {
					data <- S.param("data") ?~ "data parameter missing" ~> 400
				} yield {
					info(data)
					println(data)
					val json = parse(data)
					val a = json.extract[SpeedRequest]
					UserSpeedService.save(a)
					
				}
				JsObj(("status","success"),("message","ok"))
		}
		
		case "location" :: "customerLocation" :: Nil JsonGet _  => {
			JsArray(Customer.findAllInCompany(NotBy(Customer.lat,""))
				.map((customer:Customer)=>{
					JsObj(("id",customer.id.is),
					("title",customer.name.is),
					("lat",customer.lat.is),
					("lng",customer.lng.is),
					("icon","http://images.vilarika.com.br/mapicon/"+
					customer.mapIcon.obj.get.imagethumb.is/*iconPath*/))
				}))
			
		}		
		case "location" :: "userLocation" :: Nil JsonGet _  => {
			JsArray(User.findAllInCompanyOrdened.map((user:User)=>{JsObj(("user",user.name.is+" [ "+user.updatedAt.is.toString+" ]"),("lat",user.lat.is),("lng",user.lng.is),("icon","/images/car.png"))}))
			
		}

		case "location" :: "userLocation" :: user :: start :: end :: Nil JsonGet _  => {
				def startDate = start match {
					case (s:String) if(s != "") => Project.strOnlyDateToDate(s)
					case _ => new Date();
				}
				
				def endDate =  end match {
					case (s:String) if(s != "") => Project.strToDate(s+" 23:59:00")
					case _ => new Date();
				}

			JsArray(User.findByKey(user.toLong).get.locationsByDate(startDate,endDate).map((loc:LocationHistory)=>{JsObj(("title",loc.createdAt.toString),("lat",loc.lat.is),("lng",loc.lng.is),("icon","/images/car.png"))}))
		}	

		case "service" :: "startQueeue" :: Nil JsonGet _  => {
			UserSpeedServiceConsumer(UserSpeedService.brokerUrl,UserSpeedService.queueName)
			JString("Novo Cliente adicionado")  
		}
		case "location" :: "geocoder" :: Nil JsonGet _ =>{
			GoogleGeocoderUtil.geocoder(Customer.findByKey(2).get)
			JString("Ok")
		}
		case "location" :: "ajustsCompany" :: company  :: Nil JsonGet _ => {
//			Customer.findAll(By(Customer.company, company.toLong), By(Customer.lat, ""), NotBy(Customer.street, ""))
			Customer.findAll(By(Customer.company, company.toLong),
				          BySql("((lat = '' and street <> '') or (street <> '' and postal_code = ''))",IHaveValidatedThisSQL("","")))
					.foreach((bp) => {  
						BusinessPatternLocationQueeue.enqueeue(BusinessPatternQueeueDto(bp.id.is))
					})
			JInt(1)
		}//		
	}
}