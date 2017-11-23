
package code
package snippet

import net.liftweb._
import http._
import code.util._
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

class  OffSaleSnippet extends BootstrapPaginatorSnippet[OffSale] with SnippetUploadImage {

	def pageObj = OffSale

	def findForListParamsWithoutOrder: List[QueryParam[OffSale]] = List(Like(OffSale.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			OffSale.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[OffSale]] = List(Like(OffSale.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(OffSale.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = OffSale.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Convênio excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Convênio não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Convênio não pode ser excluído!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"actions" -> <a class="btn" href={"/offsale/edit?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o serviço "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getOffSale:OffSale = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => OffSale.create
			case _ => OffSale.findByKey(id.toLong).get
		}
	}

	def setImageToEntity(homeName:String, thumbName:String){
		val offsale = getOffSale
		offsale.image(homeName).imagethumb(thumbName).save
	}
  	def imageFolder:String = CompanyUnit.imagePath
  	def thumbToShow = getOffSale.thumb("128")
	

	def maintain = {
		try{
			var ac:OffSale = getOffSale
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
					ac.validDays(S.params("validDays_select").foldLeft("")(_+","+_))
					ac.save
				   	S.notice("Convênio salvo com sucesso!")
					S.redirectTo("/offsale/edit?id="+ac.id.is)
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
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=xmlname" #> (SHtml.text(ac.xmlname.is, ac.xmlname(_)))&
			"name=startat" #> (SHtml.text(getDateAsString(ac.startAt.is),
						(date:String) => {
							ac.startAt(Project.strOnlyDateToDate(date))
						}))&
			"name=endat" #> (SHtml.text(getDateAsString(ac.endAt.is),
						(date:String) => {
							ac.endAt(Project.strOnlyDateToDate(date))
						}))&
			"name=indic1" #> (SHtml.text(ac.indic1.is.toString, (f:String) => { 
					if(f != "")
						ac.indic1(f.toDouble)
					else
						ac.indic1(0.0)

			}))&
			"name=indic2" #> (SHtml.text(ac.indic2.is.toString, (f:String) => { 
					if(f != "")
						ac.indic2(f.toDouble)
					else
						ac.indic2(0.0)

			}))&
			"name=indic3" #> (SHtml.text(ac.indic3.is.toString, (f:String) => { 
					if(f != "")
						ac.indic3(f.toDouble)
					else
						ac.indic3(0.0)

			}))&
			"name=indic4" #> (SHtml.text(ac.indic4.is.toString, (f:String) => { 
					if(f != "")
						ac.indic4(f.toDouble)
					else
						ac.indic4(0.0)

			}))&
			"name=indic5" #> (SHtml.text(ac.indic5.is.toString, (f:String) => { 
					if(f != "")
						ac.indic5(f.toDouble)
					else
						ac.indic5(0.0)

			}))&
			"name=percentOff" #> (SHtml.text(ac.percentOff.is.toString, (v:String) => { if(v !="") ac.percentOff(v.toDouble)} ))&
			"name=value" #> (SHtml.text(ac.value.is.toString, (v:String) => { if(v !="") ac.value(v.toDouble)} ))&
		    "name=limitedValue" #> (SHtml.checkbox(ac.limitedValue_?.is, ac.limitedValue_?(_)))&
		    "name=changePrice" #> (SHtml.checkbox(ac.changePrice_?.is, ac.changePrice_?(_)))&
			"name=preservCommission" #> (SHtml.checkbox(ac.preservCommission_?.is, ac.preservCommission_?(_)))&
			"name=validDays" #> (SHtml.text(ac.validDays.is, ac.validDays(_)))&
			"name=IniHour" #> (SHtml.text(ac.iniHour.is, ac.iniHour(_)))&
			"name=endHour" #> (SHtml.text(ac.endHour.is, ac.endHour(_)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=document_ans" #> (SHtml.text(ac.document_ans.is, ac.document_ans(_))) &
		    "name=statusPartner" #> (SHtml.select(status,Full(ac.getPartner.status.is.toString),(v:String) => ac.getPartner.status(v.toInt)))&
		    "name=street" #> (SHtml.text(ac.getPartner.street.is, ac.getPartner.street(_))) &
		    "name=state_ref" #> (SHtml.text(ac.getPartner.stateRef.is.toString, (s:String) => ac.getPartner.stateRef(s.toLong)))&
			"name=city_ref" #> (SHtml.text(ac.getPartner.cityRef.is.toString, (s:String) => ac.getPartner.cityRef(s.toLong)))&
		    "name=district" #> (SHtml.text(ac.getPartner.district.is, ac.getPartner.district(_)))&
		    "name=postal_code" #> (SHtml.text(ac.getPartner.postal_code.is, ac.getPartner.postal_code(_)))&
			"name=lng" #> (SHtml.text(ac.getPartner.lng.is, ac.getPartner.lng(_)))&
			"name=lat" #> (SHtml.text(ac.getPartner.lat.is, ac.getPartner.lat(_)))&
			"name=number" #> (SHtml.text(ac.getPartner.number.is, ac.getPartner.number(_)))&
			"name=complement" #> (SHtml.text(ac.getPartner.complement.is, ac.getPartner.complement(_)))&		    
		    "name=document_offsale" #> (SHtml.text(ac.getPartner.document_offsale.is, ac.getPartner.document_offsale(_))) &
		    "name=phone" #> (SHtml.text(ac.getPartner.phone.is, ac.getPartner.phone(_)))&
		    "name=email_alternative" #> (SHtml.text(ac.getPartner.email_alternative.is, ac.getPartner.email_alternative(_)))&
			"name=mobilePhone" #> (SHtml.text(ac.getPartner.mobilePhone.is, ac.getPartner.mobilePhone(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			//notification
			//"name=offType" #> (SHtml.select(offTypes,Full(ac.offType.is.toString),(v:String) => ac.offType(v.toInt)))&
		}catch {
		    case e: NoSuchElementException => S.error("Convênio não existe!")
		    "#offsale_form *" #> NodeSeq.Empty
  		}
  	}
  	
}
