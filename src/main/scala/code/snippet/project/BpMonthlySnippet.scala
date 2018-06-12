
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

class  BpMonthlySnippet extends BootstrapPaginatorSnippet[BpMonthly] {

	def pageObj = BpMonthly

	def products = ("0", "Selecione um Serviço") :: Activity.findAllInCompany.map(t => (t.id.is.toString,t.name.is))
	def units = ("0", "Selecione uma Unidade") :: CompanyUnit.findAllInCompany(OrderBy(CompanyUnit.name, Ascending)).map(t => (t.id.is.toString, t.name.is))

//	def findForListParamsWithoutOrder: List[QueryParam[BpMonthly]] = List(Like(BpMonthly.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	def findForListParamsWithoutOrder: List[QueryParam[BpMonthly]] = List(Like(BpMonthly.obs,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			BpMonthly.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

//	def findForListParams: List[QueryParam[BpMonthly]] = List(Like(BpMonthly.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(BpMonthly.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))
	def findForListParams: List[QueryParam[BpMonthly]] = List(Like(BpMonthly.obs,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(BpMonthly.obs, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = BpMonthly.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Mensalidade excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Mensalidade não existe!")
		  				case _ => S.error("Mensalidade não pode ser excluída!")
		  			}
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.bpName),
							"product" -> Text(ac.productName),
							"startat" -> Text(Project.dateToStrOrEmpty(ac.startAt.is)),
							"valuediscount" -> Text(ac.valueDiscount.is.toString),
							"valuesession" -> Text(ac.valueSession.is.toString),
							"numsession" -> Text(ac.numSession.is.toString),
							"actions" -> <a class="btn" href={"/bpmonthly/bpmonthly?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir a mensalidade "+ac.obs}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getBpMonthly:BpMonthly = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => BpMonthly.create
			case _ => BpMonthly.findByKey(id.toLong).get
		}
	}


	def maintain = {
		try{
			var ac:BpMonthly = getBpMonthly
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
					ac.weekDays(S.params("weekDays").foldLeft("")(_+","+_))
					ac.save
				   	S.notice("Mensalidade salva com sucesso!")
				   	S.redirectTo("/bpmonthly/bpmonthly?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
			"name=auditstr" #> (SHtml.textarea(ac.auditStr, (a:String) => {}))&
			"name=business_pattern" #> (SHtml.text(ac.business_pattern.is.toString, (p:String) => ac.business_pattern(p.toLong)))&
			"name=startat" #> (SHtml.text(getDateAsString(ac.startAt.is),
						(date:String) => {
							ac.startAt(Project.strOnlyDateToDate(date))
						}))&
			"name=endat" #> (SHtml.text(getDateAsString(ac.endAt.is),
						(date:String) => {
							ac.endAt(Project.strOnlyDateToDate(date))
						}))&
		    "name=product" #> (SHtml.select(products,Full(ac.product.is.toString),(s:String) => ac.product( s.toLong)))&
			"name=weekDays_text" #> (SHtml.text(ac.weekDays, (a:String) => {}))&
			"name=value" #> (SHtml.text(ac.value.is.toString, (v:String) => { if(v !="")ac.value(v.toDouble)} ))&
			"name=valueDiscount" #> (SHtml.text(ac.valueDiscount.is.toString, (v:String) => { if(v !="")ac.valueDiscount(v.toDouble)} ))&
			"name=valueSession" #> (SHtml.text(ac.valueSession.is.toString, (v:String) => { if(v !="")ac.valueSession(v.toDouble)} ))&
			"name=numSession" #> (SHtml.text(ac.numSession.is.toString, (v:String) => { if(v !="")ac.numSession(v.toInt)} ))&
		    "name=canceled" #> (SHtml.checkbox(ac.canceled_?.is, ac.canceled_?(_)))&
		    "name=fixNumSession" #> (SHtml.checkbox(ac.fixNumSession_?.is, ac.fixNumSession_?(_)))&
		    "name=fixValueSession" #> (SHtml.checkbox(ac.fixValueSession_?.is, ac.fixValueSession_?(_)))&
			"name=bpmCount" #> (SHtml.text(ac.bpmCount.is.toString, (v:String) => { if(v !="")ac.bpmCount(v.toInt)} ))&
		    "name=unit" #> (SHtml.select(units,Full(ac.unit.is.toString),(v:String) => ac.unit(v.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error("Mensalidade não existe!")
		    "#BpMonthly_form *" #> NodeSeq.Empty
  		}
  	}
  	
}

