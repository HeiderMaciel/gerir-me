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


class  AccountCompanyUnitSnippet  extends BootstrapPaginatorSnippet[AccountCompanyUnit] {
	val banksSelect = ("0" ,"Nenhum") :: Bank.findAll.map(a =>(
		a.id.is.toString,a.name.is + " " + a.banknumber.is))
	/**
	* Pagination Methods
	*/
	def pageObj = AccountCompanyUnit

	def units = ("0", "Selecione uma Unidade") :: CompanyUnit.findAllInCompany(OrderBy(CompanyUnit.name, Ascending)).map(t => (t.id.is.toString, t.name.is))

	def findForListParamsWithoutOrder: List[QueryParam[AccountCompanyUnit]] = List(Like(AccountCompanyUnit.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))

	def findForListParams: List[QueryParam[AccountCompanyUnit]] = List(Like(AccountCompanyUnit.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(AccountCompanyUnit.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = AccountCompanyUnit.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Conta Unidade excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Conta Unidade não existe!")
						case e:Exception => S.error (e.getMessage)
		  				case _ => S.error("Conta Unidade não pode ser excluída!")
		  			}
			
			}

			AccountCompanyUnit.findAllInCompany.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"actions" -> <a class="btn" href={"/financial_admin/account?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir a conta "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getAccountCompanyUnit:AccountCompanyUnit = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => AccountCompanyUnit.create
			case _ => AccountCompanyUnit.findByKey(id.toLong).get
		}
	}

	def maintain = {
		try{
			var ac:AccountCompanyUnit = getAccountCompanyUnit
			def process(): JsCmd= {
			  try {	
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Conta Unidade salva com sucesso!")
			   	S.redirectTo("/financial_admin/accountcompanyunit?id="+ac.id.is)
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
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    //"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&
		    "name=bank" #> (SHtml.select(banksSelect,Full(ac.bank.is.toString),(s:String) => ac.bank(s.toLong)))&
		    "name=accountstr" #> (SHtml.text(ac.accountStr.is, ac.accountStr(_)))&
		    "name=accountVd" #> (SHtml.text(ac.accountVd.is, ac.accountVd(_)))&
		    "name=agency" #> (SHtml.text(ac.agency.is, ac.agency(_)))&
		    "name=agencyVd" #> (SHtml.text(ac.agencyVd.is, ac.agencyVd(_)))&
			"name=document" #> (SHtml.text(ac.document.is, ac.document(_)))&
			"name=document_company" #> (SHtml.text(ac.document_company.is, ac.document_company(_)))&
		    "name=agreement" #> (SHtml.text(ac.agreement.is, ac.agreement(_)))&
		    "name=unit" #> (SHtml.select(units,Full(ac.unit.is.toString),(v:String) => ac.unit(v.toLong)))&
//			"name=value" #> (SHtml.text(ac.value.is.toString, (v:String) => { if(v !="")ac.value(v.toDouble)} ))&
			"name=value" #> (SHtml.text(ac.value.is.toString, 
				(f:String) => ac.value(BusinessRulesUtil.snippetToDouble(f))))&
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_))++SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Conta não existe!")
		    "#product_form *" #> NodeSeq.Empty
  		}
  	}
}