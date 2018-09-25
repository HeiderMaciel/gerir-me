package code
package snippet

import net.liftweb._
import http._

import code.util._
import code.actors._
import code.service._
import model._
import http.js._
import JE._
import JsCmds._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text }
import net.liftweb.mapper._
import net.liftweb.mapper.{StartAt, MaxRows}

object  UnitSnippet extends BootstrapPaginatorSnippet[CompanyUnit] with SnippetUploadImage {
	/**
	Pagination Methods
	*/
	def pageObj = CompanyUnit

	def findForListParamsWithoutOrder: List[QueryParam[CompanyUnit]] = List(Like(CompanyUnit.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			CompanyUnit.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}
	def findForListParams: List[QueryParam[CompanyUnit]] = List(Like(CompanyUnit.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(CompanyUnit.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))
//	def findForListParams: List[QueryParam[CompanyUnit]] = List(OrderBy(CompanyUnit.name, Ascending), Like(CompanyUnit.name,name+"%"), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))

	def apptypes = {
		Seq(
      "1" -> "ebelle",
      "2" -> "gerirme",
      "3" -> "esmile",
      "4" -> "edoctus",
      "5" -> "egrex",
      "6" -> "ephysio",
      "7" -> "ebellepet"
		)
	}

	def costcenters = ("0" -> "Selecione um Centro de Custo")::CostCenter.findAllInCompany.map(cc => (cc.id.is.toString,cc.name.is))

	def units(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = CompanyUnit.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Unidade excluída com sucesso!");
		  			}catch{
		  				case e: NoSuchElementException => S.error("Unidade não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Unidade não pode ser excluída!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"shortname" -> Text(ac.short_name.is),
							"showincalendar" -> Text(if(ac.showInCalendar_?.is){ "Sim" }else{ "Não" }),
							"actions" -> {<span><a class="btn success user_this_unit" data-id={ac.id.is.toString}>Usar Essa unidade</a> <a class="btn" href={"/unit/edit?id="+ac.id.is}>Editar</a></span>},
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir a unidade "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def maintain() = {
		try{
			var ac:CompanyUnit = getUnit
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Unidade salva com sucesso!")
				   	S.redirectTo("/unit/edit?id="+ac.id.is)
				}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
			"name=allowshowonsite" #> (SHtml.checkbox(ac.allowShowOnSite_?, ac.allowShowOnSite_?(_)))&
			"name=allowshowonportal" #> (SHtml.checkbox(ac.allowShowOnPortal_?, ac.allowShowOnPortal_?(_)))&
			"name=moderatedportal" #> (SHtml.checkbox(ac.moderatedPortal_?, ac.moderatedPortal_?(_)))&
		    "name=sitetitle" #> (SHtml.text(ac.siteTitle.is, ac.siteTitle(_)))&
		    "name=sitedescription" #> (SHtml.textarea(ac.siteDescription.is, ac.siteDescription(_)))&
			"name=useSingleCashier" #> (SHtml.checkbox(ac.useSingleCashier_?, ac.useSingleCashier_?(_)))&
		    "name=defaultDDD" #> (SHtml.text(ac.defaultDDD.is.toString,(s:String) => ac.defaultDDD(s.toInt)))&
			"name=defaultSex" #> (SHtml.select(sexs,Full(ac.defaultSex.is),ac.defaultSex(_)))&
		    "name=costcenter" #> (SHtml.select(costcenters,Full(ac.costCenter.is.toString),(s:String) => ac.costCenter( s.toLong)))&
		    "name=useSsl" #> (SHtml.checkbox(ac.smtp_ssl_?.is, ac.smtp_ssl_?(_)))&
		    "name=smtpServer" #> (SHtml.text(ac.smtpServer.is, ac.smtpServer(_)))&
			"name=userSmtp" #> (SHtml.text(ac.userSmtp.is, ac.userSmtp(_)))&
			"name=passwordSmtp" #> (SHtml.password(ac.passwordSmtp.is, ac.passwordSmtp(_)))&
			"name=port" #> (SHtml.text(ac.port.is, ac.port(_)))&
			"name=showInCalendar" #> (SHtml.checkbox(ac.showInCalendar_?, ac.showInCalendar_?(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&	
		    "name=apptypeu" #> (SHtml.select(apptypes,Full(ac.appTypeU.is.toString),(v:String) => ac.appTypeU(v.toInt)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=statusPartner" #> (SHtml.select(status,Full(ac.getPartner.status.is.toString),(v:String) => ac.getPartner.status(v.toInt)))&
		    "name=document_company" #> (SHtml.text(ac.getPartner.document_company.is, ac.getPartner.document_company(_))) &
		    "name=company_name" #> (SHtml.text(ac.getPartner.company_name.is, ac.getPartner.company_name(_))) &
		    "name=calendarUrl" #> (SHtml.text(ac.calendarUrl.is,ac.calendarUrl(_)))&
		    "name=phone" #> (SHtml.text(ac.getPartner.phone.is, ac.getPartner.phone(_))) &
		    "name=email_alternative" #> (SHtml.text(ac.getPartner.email_alternative.is, ac.getPartner.email_alternative(_))) &
		    "name=mobilePhone" #> (SHtml.text(ac.getPartner.mobilePhone.is, ac.getPartner.mobilePhone(_))) &
		    "name=street" #> (SHtml.text(ac.getPartner.street.is, ac.getPartner.street(_))) &
		    "name=state_ref" #> (SHtml.text(ac.getPartner.stateRef.is.toString, (s:String) => ac.getPartner.stateRef(s.toLong)))&
			"name=city_ref" #> (SHtml.text(ac.getPartner.cityRef.is.toString, (s:String) => ac.getPartner.cityRef(s.toLong)))&
		    "name=district" #> (SHtml.text(ac.getPartner.district.is, ac.getPartner.district(_)))&
		    "name=postal_code" #> (SHtml.text(ac.getPartner.postal_code.is, ac.getPartner.postal_code(_)))&
			"name=lng" #> (SHtml.text(ac.getPartner.lng.is, ac.getPartner.lng(_)))&
			"name=lat" #> (SHtml.text(ac.getPartner.lat.is, ac.getPartner.lat(_)))&
			"name=number" #> (SHtml.text(ac.getPartner.number.is, ac.getPartner.number(_)))&
			"name=complement" #> (SHtml.text(ac.getPartner.complement.is, ac.getPartner.complement(_)))&		    
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_))++SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Unidade não existe!")
		    "#user_form *" #> NodeSeq.Empty
  		}
  	}
  	def getUnit:CompanyUnit = {
		var id = S.param("id") openOr "0"
		var unit:CompanyUnit = {
					id match {
						case "0" => CompanyUnit.create
						case _ => CompanyUnit.findByKey(id.toLong).get
					}
		}
		unit
  	}

	def setImageToEntity(homeName:String, thumbName:String){
		val unit = getUnit
		unit.image(homeName).imagethumb(thumbName).save
	}
  	def imageFolder:String = CompanyUnit.imagePath
  	def thumbToShow = getUnit.thumb("128")

	def unit = AuthUtil unit

	def UnitName = {	
		<b>{unit.name}</b>
	}


}