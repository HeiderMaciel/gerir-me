
package code
package snippet

import net.liftweb._
import http._
import code.util._
import model._
import http.js._
import json._
import JE._
import JsCmds._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text }
import net.liftweb.mapper._
import net.liftweb.mapper.{StartAt, MaxRows}


class  AccountCategorySnippet  extends BootstrapPaginatorSnippet[AccountCategory] {
	val typesAccount = (AccountPayable.IN.toString,"Entrada") :: (AccountPayable.OUT.toString,"Saída") :: (AccountPayable.TRANS.toString,"Transferência") :: Nil
	val parentAccounts = ("0" ,"Nenhum") :: AccountCategory.findAllInCompany(By(AccountCategory.parent_?,true),
		OrderBy(AccountCategory.maxTreeNode, Descending),
		OrderBy(AccountCategory.orderInReport, Ascending),
		OrderBy(AccountCategory.name, Ascending)
		).map(a =>(a.id.is.toString,a.name.is))
	/**
	* Pagination Methods
	*/
	def pageObj = AccountCategory

//	override def itemsPerPage = 10
	def findForListParamsWithoutOrder: List[QueryParam[AccountCategory]] = List(Like(AccountCategory.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			AccountCategory.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[AccountCategory]] = List(Like(AccountCategory.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(AccountCategory.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = AccountCategory.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Categoria excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Categoria não existe!")
						case e:Exception => S.error (e.getMessage)
		  				case _ => S.error("Categoria não pode ser excluída!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"order" -> Text(ac.orderInReport.is.toString),
							"colorid" -> ac.colorId,
							"typemovement" -> Text(
								    if(ac.typeMovement.is == 0){ 
										"Entrada" 
									} else if (ac.typeMovement.is == 1 ){ 
										"Saída" 
									} else {
										"Transferência" 
									}
									),
							"parentaccount" -> Text(
														ac.parentAccount.obj match {
															case Full(p) => p.name.is
															case _ => ""
														}
														
												),
							"userassociated" -> Text(if(ac.userAssociated.is){ "Sim" }else{ "Não" }),
							"isparent" -> Text(if(ac.parent_?.is){ "Sim" }else{ "Não" }),											
							"actions" -> <a class="btn" href={"/financial_admin/account_category?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir a conta "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getCategory:AccountCategory = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => AccountCategory.create
			case _ => AccountCategory.findByKey(id.toLong).get
		}
	}

	def maintain = {
		try{
			var ac:AccountCategory = getCategory
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
				
			   		ac.save	
			   		S.notice("Categoria salva com sucesso!")
			   		S.redirectTo("/financial_admin/account_category?id="+ac.id.is)
			   	}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}			   		
			   		case e: RuntimeException => S.error(e.getMessage)
			   	}
			}
			"name=auditstr" #> (SHtml.textarea(ac.auditStr, (a:String) => {}))&
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
			"name=external_id" #> (SHtml.text(ac.external_id, (v:String) => { ac.external_id(v)}))&
		    "name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=order" #> (SHtml.text(ac.orderInReport.is.toString, (s:String) => ac.orderInReport(s.toInt)))&
		    "name=managerLevel" #> (SHtml.text(ac.managerLevel.is.toString, (s:String) => ac.managerLevel(s.toInt)))&
		    "name=color" #> (SHtml.text(ac.color.is, ac.color(_)))&
		    "name=typeMovement" #> (SHtml.select(typesAccount,Full(ac.typeMovement.is.toString),(s:String) => ac.typeMovement(s.toInt)))&
		    "name=parentAccount" #> (SHtml.select(parentAccounts,Full(ac.parentAccount.is.toString),(s:String) => ac.parentAccount(s.toLong)))&
		    "name=isParent" #> (SHtml.checkbox(ac.parent_?.is,ac.parent_?(_)))&
		    "name=balanceControl" #> (SHtml.checkbox(ac.balanceControl_?.is,ac.balanceControl_?(_)))&
		    "name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&
		    "name=userAssociated" #> (SHtml.checkbox(ac.userAssociated.is,ac.userAssociated(_))++SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Categoria não existe!")
		    "#product_form *" #> NodeSeq.Empty
  		}
  	}
}

