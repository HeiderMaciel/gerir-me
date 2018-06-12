
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

class  PaymentConditionSnippet extends BootstrapPaginatorSnippet[PaymentCondition] {

	def pageObj = PaymentCondition 

	def projects = ("0" -> "Selecione um Projeto/Evento")::Project1.findAllInCompany.map(cc => (cc.id.is.toString,cc.name.is))

	def findForListParamsWithoutOrder: List[QueryParam[PaymentCondition]] = List(Like(PaymentCondition.obs,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			PaymentCondition.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[PaymentCondition]] = List(Like(PaymentCondition.obs,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(PaymentCondition.obs, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))

	def list (xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = PaymentCondition.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Condição de pagamento excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Condição de pagamento não existe!")
		  				case _ => S.error("Condição de pagamento não pode ser excluída!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,
							"paymentDate" -> Text(Project.dateToStrOrEmpty(ac.paymentDate.is)),
							"actions" -> <a class="btn" href={"/project/edit_paymentcondition?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir a condição de pagamento"}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}



	def getPaymentCondition:PaymentCondition = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => PaymentCondition.create
			case _ => PaymentCondition.findByKey(id.toLong).get
		}
	}	


	def maintain = {
		try{
			var ac:PaymentCondition = getPaymentCondition
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
				ac.save
			   	S.notice("Participante salvo com sucesso!")
			   	S.redirectTo("/project/edit_Paymentcondition?id="+ac.id.is)
			}
			"name=auditstr" #> (SHtml.textarea(ac.auditStr, (a:String) => {}))&
			"name=paymentDate" #> (SHtml.text(getDateAsString(ac.paymentDate.is),
						(date:String) => {
							ac.paymentDate(Project.strOnlyDateToDate(date))
						}))&
		    "name=project" #> (SHtml.select(projects,Full(ac.project.is.toString),(s:String) => ac.project( s.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error("Participante não existe!")
		    "#PaymentCondition_form *" #> NodeSeq.Empty
  		}
  	}

}

