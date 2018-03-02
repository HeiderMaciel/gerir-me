
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
import java.util.{Date, Calendar};

class  TreatmentDetailSnippet extends PaginatorSnippet [TreatmentDetail] {

	def getDateAsString(date:Date) = {		
		date match {
			case d:Date => {
				if (d.getHours == 0) {
					getDatePlus3(d)
				} else {
					d.getTime.toString
				}
			}
			case _ => ""
		}				
	}	

	def getDatePlus3(date:Date) = {
		val cal = Calendar.getInstance
		cal.setTime(date)
		cal.add(Calendar.HOUR_OF_DAY, 3) 
		cal.getTime.getTime.toString
	}

	def pageObj = TreatmentDetail
	def obs = S.param("obs") match {
		case Full(s) => s
		case _ => ""
	}	

	def teeth = DomainTable.findAll(OrderBy(DomainTable.cod, Ascending),By(DomainTable.domain_name, "dente")).map(t => (t.cod.is.toString,t.name.is))
	def activities = ("0", "Selecione um Serviço") :: Activity.findAllInCompany(OrderBy(Activity.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def auxiliars = ("0", "Selecione um Assistente") :: User.findAllInCompany(OrderBy(User.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def offsales = ("0", "Selecione um Convênio") :: OffSale.findAllInCompany(OrderBy(OffSale.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def units = ("0", "Selecione uma Unidade") :: CompanyUnit.findAllInCompany(OrderBy(CompanyUnit.name, Ascending)).map(t => (t.id.is.toString, t.name.is))
	def icds = ("0" -> "Selecione um cid")::Icd.findAll(By(Icd.section,"S"),OrderBy(Icd.namecomp, Ascending)).map(t => (t.id.is.toString,t.namecomp.is))
//,By(Icd.section,"S")

//	def findForListParamsWithoutOrder: List[QueryParam[TreatmentDetail]] = List(Like(TreatmentDetail.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	def findForListParamsWithoutOrder: List[QueryParam[TreatmentDetail]] = List(Like(TreatmentDetail.obs,"%"+BusinessRulesUtil.clearString(obs)+"%"))
/*	override def page = {
		if(!showAll){
			super.page
		}else{
			//TreatmentDetail.findAllInCompanyWithInactive(findForListParams :_*)
			TreatmentDetail.findAllInCompany(findForListParams :_*)
		}
	}
*/

/*
	override def page = {
		TreatmentDetail.findAllInCompany(findForListParams :_*)
	}
*/
	override lazy val count = {
		AuthUtil.company.count(pageObj,findForListParamsWithoutOrder)
	}
	override def page = AuthUtil.company.pagination(pageObj,findForListParams)


//	def findForListParams: List[QueryParam[TreatmentDetail]] = List(Like(TreatmentDetail.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(TreatmentDetail.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))
	def findForListParams: List[QueryParam[TreatmentDetail]] = List(Like(TreatmentDetail.obs,"%"+BusinessRulesUtil.clearString(obs)+"%"),OrderBy(TreatmentDetail.obs, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = TreatmentDetail.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Detalhe de Atendimento excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Detalhe de Atendimento não existe! " + id)
		  				case _ => S.error("Detalhe de Atendimento não pode ser excluído!")
		  			}
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.auxiliarShortName),
							"product" -> Text(ac.nameActivity),
							//"valuediscount" -> Text(ac.valueDiscount.is.toString),
							//"valuesession" -> Text(ac.valueSession.is.toString),
							//"numsession" -> Text(ac.numSession.is.toString),
							"actions" -> <a class="btn" href={"/treatment/treatmentdetail?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o Detalhe de Atendimento "+ac.obs}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getTreatmentDetail:TreatmentDetail = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => TreatmentDetail.create
			case _ => TreatmentDetail.findByKey(id.toLong).get
		}
	}


	def maintain = {
		try{
			var ac:TreatmentDetail = getTreatmentDetail
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
//					ac.weekDays(S.params("weekDays").foldLeft("")(_+","+_))
					if (ac.price == 0.0) {
						ac.price (ac.priceActivity * ac.amount) 
					}
					ac.save
					val ac1 = Treatment.findByKey(ac.treatment.toLong).get
					ac1.save
				   	S.notice("Detalhe de Atendimento salvo com sucesso!")
				   	S.redirectTo("/treatment/treatmentdetail?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
			"name=treatment" #> (SHtml.text(ac.treatment.is.toString, (v:String) => { if(v !="")ac.treatment(v.toLong)} ))&
			"name=external_id" #> (SHtml.text(ac.external_id.is, ac.external_id(_)))&
		    "name=activity" #> (SHtml.select(activities,Full(ac.activity.is.toString),(s:String) => ac.activity( s.toLong)))&
		    "name=auxiliar" #> (SHtml.select(auxiliars,Full(ac.auxiliar.is.toString),(s:String) => ac.auxiliar( s.toLong)))&
		    "name=offsale" #> (SHtml.select(offsales,Full(ac.offsale.is.toString),(s:String) => ac.offsale( s.toLong)))&
			"name=product" #> (SHtml.text(ac.product.is.toString, (p:String) => ac.product(p.toLong)))&
			"name=wayofaccess" #> (SHtml.text(ac.getTdEdoctus.wayOfAccess.is, ac.getTdEdoctus.wayOfAccess(_)))&
			"name=tooth" #> (SHtml.select(teeth,Full(ac.getTdEdoctus.tooth.is), ac.getTdEdoctus.tooth(_)))&
			"name=price" #> (SHtml.text(ac.price.is.toString, 
				(f:String) => ac.price(BusinessRulesUtil.snippetToDouble(f))))&
			"name=amount" #> (SHtml.text(ac.amount.is.toString, (v:String) => { if(v !="")ac.amount(v.toDouble)} ))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Detalhe de Atendimento não existe!")
		    "#TreatmentDetail_form *" #> NodeSeq.Empty
  		}
  	}
  	
}

