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


class  AccountSnippet  extends BootstrapPaginatorSnippet[Account] {
	val banksSelect = ("0" ,"Nenhum") :: Bank.findAll.map(a =>(
		a.id.is.toString,a.name.is + " " + a.banknumber.is))
	/**
	* Pagination Methods
	*/
	def pageObj = Account

	def units:List[Long] = S.param("units") match {
		case Full(s) if(s != "") => {
			S.params("units").map(_.toLong)
		}
		case _ => {
			Nil
		}
	}

	def findForListParamsWithoutOrder: List[QueryParam[Account]] = List(Like(Account.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			Account.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}
	def statusFilter: List[Int] = if(showAll){
		List(Account.STATUS_OK, Account.STATUS_INACTIVE)
	}else{
		List(Account.STATUS_OK)
	}

	def findForListParams: List[QueryParam[Account]] = List(
		Like(Account.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
		OrderBy(Account.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage), ByList(Account.status,statusFilter))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Account.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Conta excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Conta não existe!")
						case e:Exception => S.error (e.getMessage)
		  				case _ => S.error("Conta não pode ser excluída!")
		  			}
			
			}

			//Account.findAllInCompany
			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"allowcashierout" -> Text(if(ac.allowCashierOut_?.is){ "Sim" }else{ "Não" }),
							// "value" -> Text(ac.value.is.toString),
							"balanceunits" -> <table>{ac.balanceUnits.filter((au) => { units.size ==0 || units.contains(au.unit.is) }).map((au) => <tr>
								<td>{au.unit.obj.get.short_name} </td> 
								<td>{au.short_name} </td> 
								<td>{au.value.is.toString}</td> 
								<td>{au.id.is.toString}</td>
								<td><a class="btn" href={"/financial_admin/accountcompanyunit?id="+au.id.is}>Editar</a></td>
							</tr>)}
							</table>,
							"actions" -> <a class="btn" href={"/financial_admin/account?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir a conta "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getAccount:Account = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Account.create
			case _ => Account.findByKey(id.toLong).get
		}
	}

	def maintain = {
		try{
			var ac:Account = getAccount
			def process(): JsCmd= {
			  try {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Conta salva com sucesso!")
			   	S.redirectTo("/financial_admin/account?id="+ac.id.is)
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
		    "name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&
		    "name=bank" #> (SHtml.select(banksSelect,Full(ac.bank.is.toString),(s:String) => ac.bank(s.toLong)))&
		    "name=allowCashierOut" #> (SHtml.checkbox(ac.allowCashierOut_?.is,ac.allowCashierOut_?(_)))&
		    "name=balanceControl" #> (SHtml.checkbox(ac.balanceControl_?.is,ac.balanceControl_?(_)))&
			"name=creditCard" #> (SHtml.checkbox(ac.creditCard_?, ac.creditCard_?(_)))&
//			"name=value" #> (SHtml.text(ac.value.is.toString, (v:String) => { if(v !="")ac.value(v.toDouble)} ))&
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_))++SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Conta não existe!")
		    "#product_form *" #> NodeSeq.Empty
  		}
  	}
}