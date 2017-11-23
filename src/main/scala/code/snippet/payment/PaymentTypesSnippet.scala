
package code
package snippet

import net.liftweb._
import http._
import code.util._
import code.model._
import http.js._
import JE._
import JsCmds._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text, Unparsed }
import net.liftweb.mapper._

class PaymentTypesSnippet extends BootstrapPaginatorSnippet[PaymentType]{
	def pageObj = PaymentType

	def findForListParamsWithoutOrder: List[QueryParam[PaymentType]] = 
	List(Like(PaymentType.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			PaymentType.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[PaymentType]] = List(Like(PaymentType.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
		OrderBy(PaymentType.name, Ascending), 
		StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))
//	def findForListParams: List[QueryParam[PaymentType]] = Nil

	def categoriesForSelect = ("0" ,"Nenhum") :: AccountCategory.findAllInCompany(OrderBy(AccountCategory.name, Ascending)).map(a =>(a.id.is.toString,a.name.is))
	def accountsForSelect = ("0" ,"Nenhum") :: Account.findAllInCompany(OrderBy(Account.name, Ascending)).map(a =>(a.id.is.toString,a.name.is))

	def paymenttypes(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = PaymentType.findByKey(id.toLong).get
		  				ac.delete_!
		  				S.notice("Forma de pagamento excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Forma de pagamento não existe!")
						case e:Exception => S.error (e.getMessage)
		  				case _ => S.error("Forma de pagamento não pode ser excluída!")
		  			}
			}

			page.flatMap(ac =>
			bind("f", xhtml,"name" -> <span>{Unparsed(ac.name.is)}</span>,
							"sumincachier" -> Text(if(ac.sumInCachier_?.is){ "Sim" }else{ "Não" }),
							"sumtoconference" -> Text(if(ac.sumToConference_?.is){ "Sim" }else{ "Não" }),
							"generatecommision" -> Text(if(ac.generateCommision_?.is){ "Sim" }else{ "Não" }),
							"creditcard" -> Text(if(ac.creditCard_?.is){ "Sim" }else{ "Não" }),
							"cheque" -> Text(if(ac.cheque_?.is){ "Sim" }else{ "Não" }),
							"deliverycontol" -> Text(if(ac.deliveryContol_?.is){ "Sim" }else{ "Não" }),
							"customerregisterdebit" -> Text(if(ac.customerRegisterDebit_?.is){ "Sim" }else{ "Não" }),
							"comissionatsight" -> Text(if(ac.comissionAtSight_?.is){ "Sim" }else{ "Não" }),
							"showasoptions" -> Text(if(ac.showAsOptions_?.is){ "Sim" }else{ "Não" }),
							"showasfinoptions" -> Text(if(ac.showAsFinOptions_?.is){ "Sim" }else{ "Não" }),
							"acceptinstallment" -> Text(if(ac.acceptInstallment_?.is){ "Sim" }else{ "Não" }),
							"receiveatsight" -> Text(if(ac.receiveAtSight_?.is){ "Sim" }else{ "Não" }),
							"receive" -> Text(if(ac.receive_?.is){ "Sim" }else{ "Não" }),
							"nextmonth" -> Text(if(ac.nextMonth_?.is){ "Sim" }else{ "Não" }),
							"defaltaccount" -> Text(
														ac.defaltAccount.obj match {
															case Full(ac) => ac.name.is
															case _ => ""
														}
														
												),
							"defaltcategory" -> Text(
														ac.defaltCategory.obj match {
															case Full(ac) => ac.name.is
															case _ => ""
														}
														
												),
							"defaltdicountcategory" -> Text(
														ac.defaltDicountCategory.obj match {
															case Full(ac) => ac.name.is
															case _ => ""
														}
														
												),
							"actions" -> <a class="btn" href={"/financial_admin/payment_forms?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" Excluir a forma "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getPaymentType = S.param("id") match {
		case Full(s:String) => PaymentType.findByKey(s.toInt).get
		case _ => PaymentType.create
	}
	
	def maintain = {
		try{
			var ac:PaymentType = getPaymentType
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
				   	ac.save
				   	S.notice("Forma de pagamento salva com sucesso!")
				   	S.redirectTo("/financial_admin/payment_forms?id="+ac.id.is)
	   			}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}				  
			}	
			"name=createdAt" #> (SHtml.text(getDateAsString(ac.createdAt.is),
						(date:String) => {ac.createdAt(Project.strOnlyDateToDate(date))}))&
			"name=updatedAt" #> (SHtml.text(getDateAsString(ac.updatedAt.is),
						(date:String) => {ac.updatedAt(Project.strOnlyDateToDate(date))}))&
			"name=createdby" #> (SHtml.text(ac.createdByName, (p)=> {} ))&
			"name=updatedby" #> (SHtml.text(ac.updatedByName, (p)=> {} ))&
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
//		    "name=key" #> (SHtml.text(ac.key.is, ac.key(_)))&
		    "name=sumInCachier" #> (SHtml.checkbox(ac.sumInCachier_?, ac.sumInCachier_?(_)))&
		    "name=sumToConference" #> (SHtml.checkbox(ac.sumToConference_?, ac.sumToConference_?(_)))&
			"name=generateCommision" #> (SHtml.checkbox(ac.generateCommision_?, ac.generateCommision_?(_)))&
			"name=creditCard" #> (SHtml.checkbox(ac.creditCard_?, ac.creditCard_?(_)))&
			"name=needCardInfo" #> (SHtml.checkbox(ac.needCardInfo_?, ac.needCardInfo_?(_)))&
			"name=cheque" #> (SHtml.checkbox(ac.cheque_?, ac.cheque_?(_)))&
			"name=needChequeInfo" #> (SHtml.checkbox(ac.needChequeInfo_?, ac.needChequeInfo_?(_)))&
			"name=deliveryContol" #> (SHtml.checkbox(ac.deliveryContol_?, ac.deliveryContol_?(_)))&
			"name=customerRegisterDebit" #> (SHtml.checkbox(ac.customerRegisterDebit_?, ac.customerRegisterDebit_?(_)))&
			"name=addUserAccountToDiscount" #> (SHtml.checkbox(ac.addUserAccountToDiscount_?, ac.addUserAccountToDiscount_?(_)))&
			"name=allowCustomeraddUserToDiscount" #> (SHtml.checkbox(ac.allowCustomeraddUserToDiscount_?, ac.allowCustomeraddUserToDiscount_?(_)))&
			"name=bpmonthly" #> (SHtml.checkbox(ac.bpmonthly_?, ac.bpmonthly_?(_)))&
			"name=offSale" #> (SHtml.checkbox(ac.offSale_?, ac.offSale_?(_)))&
			"name=fidelity" #> (SHtml.checkbox(ac.fidelity_?, ac.fidelity_?(_)))&
			"name=customerUseCredit" #> (SHtml.checkbox(ac.customerUseCredit_?, ac.customerUseCredit_?(_)))&
			"name=comissionAtSight" #> (SHtml.checkbox(ac.comissionAtSight_?, ac.comissionAtSight_?(_)))&
			"name=showAsOptions" #> (SHtml.checkbox(ac.showAsOptions_?, ac.showAsOptions_?(_)))&
			"name=showAsFinOptions" #> (SHtml.checkbox(ac.showAsFinOptions_?, ac.showAsFinOptions_?(_)))&
			"name=acceptInstallment" #> (SHtml.checkbox(ac.acceptInstallment_?, ac.acceptInstallment_?(_)))&
			"name=receiveAtSight" #> (SHtml.checkbox(ac.receiveAtSight_?, ac.receiveAtSight_?(_)))&
			"name=receive" #> (SHtml.checkbox(ac.receive_?, ac.receive_?(_)))&
			"name=individualReceive" #> (SHtml.checkbox(ac.individualReceive_?, ac.individualReceive_?(_)))&
			"name=usernotification" #> (SHtml.checkbox(ac.usernotification_?, ac.usernotification_?(_)))&
			"name=autochangetopaid" #> (SHtml.checkbox(ac.autoChangeToPaid_?, ac.autoChangeToPaid_?(_)))&
			"name=percentDiscountToReceive" #> (SHtml.text(ac.percentDiscountToReceive.is.toString, 
				(f:String) => ac.percentDiscountToReceive(BusinessRulesUtil.snippetToDouble(f))))&
			"name=order" #> (SHtml.text(ac.order.is.toString, 
				(f:String) => ac.order(BusinessRulesUtil.snippetToInt(f))))&
			"name=defaltAccount" #> (SHtml.select(accountsForSelect,Full(ac.defaltAccount.is.toString),(s:String) => ac.defaltAccount(s.toLong)))&
			"name=defaltCategory" #> (SHtml.select(categoriesForSelect,Full(ac.defaltCategory.is.toString), (s:String) => ac.defaltCategory(s.toLong )))&
			"name=defaltDicountCategory" #> (SHtml.select(categoriesForSelect,Full(ac.defaltDicountCategory.is.toString), (s:String) => ac.defaltDicountCategory(s.toLong )))&
			"name=numDays" #> (SHtml.text(ac.numDays.is.toString, 
				(f:String) => ac.numDays(BusinessRulesUtil.snippetToInt(f))))&
			"name=day" #> (SHtml.text(ac.day.is.toString, 
				(f:String) => ac.limitDay(BusinessRulesUtil.snippetToInt(f))))&			
			"name=limitDay" #> (SHtml.text(ac.limitDay.is.toString, 
				(f:String) => ac.day(BusinessRulesUtil.snippetToInt(f))))&			
			"name=nextMonth" #> (SHtml.checkbox(ac.nextMonth_?, ac.nextMonth_?(_)))&
			"name=percentDiscountToCommision" #> (SHtml.text(ac.percentDiscountToCommision.is.toString, 
				(f:String) => ac.percentDiscountToCommision(BusinessRulesUtil.snippetToDouble(f))))&
			"name=numDaysForReceive" #> (SHtml.text(ac.numDaysForReceive.is.toString, 
				(f:String) => ac.numDaysForReceive(BusinessRulesUtil.snippetToInt(f))))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			//notification
		}catch {
		    case e: NoSuchElementException => S.error("Forma de pagamento não existe!")
		    "#activity_form *" #> NodeSeq.Empty
  		}
  	}  	
}

