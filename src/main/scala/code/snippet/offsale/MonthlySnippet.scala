
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

import java.util.Date

class  MonthlySnippet extends BootstrapPaginatorSnippet[Monthly] {

	def pageObj = Monthly

	//var itens = 200;
	def itens = S.param("itenspp_monthly") match {
		case Full(s) => s.toInt
		case _ => 150
	}

	def showDebtors:Boolean = checkBooleanParamenter("debtors", true);		
	
	override def itemsPerPage = itens;

	def findForListParamsWithoutOrder: List[QueryParam[Monthly]] = 
	List(Like(Monthly.description,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll) {
			super.page
		}else{
		   	Monthly.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	val sqlname = " business_pattern in (select id from business_pattern where search_name like '%"+
		BusinessRulesUtil.clearString(name)+"%' and company = " + AuthUtil.company.id.is + " )"

	val sqldebtors = if (!showDebtors) {
		" paid = false and dateExpiration < date(now())"
	} else {
		" 1 = 1 "
	}

	def findForListParams: List[QueryParam[Monthly]] = 
	List(//Like(Monthly.description,"%"+BusinessRulesUtil.clearString(name)+"%"),
		BySql (sqlname,IHaveValidatedThisSQL("","")),
		BySql (sqldebtors,IHaveValidatedThisSQL("","")),
		BySql (bpList,IHaveValidatedThisSQL("","")),
		OrderBy(Monthly.id, Descending), StartAt(curPage*itemsPerPage), 
		MaxRows(itemsPerPage))

	def bp = S.param("bp") match {
		case Full(s) => s
		case _ => ""
	}	
    val bpList = if(bp != ""){
        "business_pattern = %s ".format (bp)
    }else{
        " 1 = 1 "
    }

	def accounts = ("0" -> "Selecione uma conta")::Account.findAllInCompany(OrderBy(Account.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def list(xhtml: NodeSeq): NodeSeq = {
		    val today = Project.date_format_db.parse(Project.date_format_db.format(new Date()));
			def thumbSN(status:Integer, field:Boolean, expDate: Date) = if (status == 1) {
				if (field) {
					<img title="Pago" style= "width:16px" src="/images/good.png"/>
				} else if (expDate.after (today) || Project.date_format_db.parse(Project.date_format_db.format(expDate)).equals (today)) {
					<img title="Ainda no prazo" style= "width:16px" src="/images/account_customer.png"/>
				} else {
					<img title="Atrasado" style= "width:16px" src="/images/bad.png"/>
				}  
			} else {
				<img title="Inativo" style= "width:16px" src="/images/cancel.png"/>
			}
			def late (originalDate:Date, paymentDate: Date, paid : Boolean) = 
			if (paymentDate.after (originalDate)) {
				( (paymentDate.getTime() - originalDate.getTime()) 
                 / (1000 * 60 * 60 * 24) ).toString
			} else if ( !paid && today.after (originalDate)) {
				( (today.getTime() - originalDate.getTime()) 
                 / (1000 * 60 * 60 * 24) ).toString
			} else {
				""
			}  
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Monthly.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Mensalidade excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Mensalidade não existe!")
		  				case _ => S.error("Mensalidade não pode ser excluída!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"description" -> Text(ac.description.is),
							"company_customer" -> Text(ac.company_customer.toString),
							"companyname" -> Text(ac.company_customerName),
							"bpname" -> Text(ac.bpName),
							"accountshortname" -> Text(ac.accountShortName),
							"value" -> Text(ac.value.is.toString),
							"liquidvalue" -> Text(ac.liquidValue.is.toString),
							"paidvalue" -> Text(ac.paidValue.is.toString),
							"obs" -> Text(ac.obs.is),
							"idforcompany" -> Text(ac.idForCompany.toString),
							"dateexpiration" -> Text(Project.dateToStrOrEmpty(ac.dateExpiration.is)),
							"senddate" -> Text(Project.dateToStrOrEmpty(ac.sendDate.is)),
							"createdat" -> Text(Project.dateTimeToStr(ac.createdAt.is)),
							"paymentdate" -> Text(Project.dateToStrOrEmpty(ac.paymentDate.is)),
							//"paid" -> Text(if(ac.paid.is){ "Sim" }else{ "Não" }),
							"late" -> Text(late(ac.originalDate, ac.dateExpiration, ac.paid)),
							"paid" -> thumbSN(ac.status.is, ac.paid.is, ac.dateExpiration),
							"actions" -> <a class="btn" href={"/monthly/edit_monthly?id="+ac.id.is + "&company="+ac.company_customer.is}>Editar</a>,
							"gocompany" -> <a class="btn success" href={"/security/useCompany?id="+ac.company_customer.is}>Ir</a>,
							"delete" -> SHtml.submit("Excluir - Use Status",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir a mensalidade "+ac.description}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getMonthly:Monthly = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Monthly.create
			case _ => Monthly.findByKey(id.toLong).get
		}
	}
	

	def maintain = {
		try{
			var ac:Monthly = getMonthly
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
					ac.save
				   	S.notice("Mensalidade salva com sucesso!")
					S.redirectTo("/monthly/edit_monthly?id="+ac.id.is)
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
			"name=id" #> (SHtml.text(ac.id.is.toString, (f:String) => { 
					if(f != "")
						ac.id(f.toInt)
					else
						ac.id(0)

			}))&
			"name=idForCompany" #> (SHtml.text(ac.idForCompany.is.toString, (f:String) => { 
					if(f != "")
						ac.idForCompany(f.toInt)
					else
						ac.idForCompany(0)

			}))&
			"name=bpname" #> (SHtml.text(ac.bpName, (p)=> {} ))&
			"name=company_customer" #> (SHtml.text(ac.company_customer.is.toString, 
				(f:String) => ac.company_customer(BusinessRulesUtil.snippetToInt(f))))&
		    "name=description" #> (SHtml.text(ac.description.is, ac.description(_)))&
		    "name=paymentDate" #> (SHtml.text(getDateAsString(ac.paymentDate.is),
						(date:String) => {
							ac.paymentDate(Project.strOnlyDateToDate(date))
						}))&
		    "name=sendDate" #> (SHtml.text(getDateAsString(ac.sendDate.is),
						(date:String) => {
							ac.sendDate(Project.strOnlyDateToDate(date))
						}))&
			"name=dateExpiration" #> (SHtml.text(getDateAsString(ac.dateExpiration.is),
						(date:String) => {
							ac.dateExpiration(Project.strOnlyDateToDate(date))
						}))&
		    "name=originalDate" #> (SHtml.text(getDateAsString(ac.originalDate.is),
						(date:String) => {
							ac.originalDate(Project.strOnlyDateToDate(date))
						}))&
		    "name=efetiveDate" #> (SHtml.text(getDateAsString(ac.efetiveDate.is),
						(date:String) => {
							ac.efetiveDate(Project.strOnlyDateToDate(date))
						}))&
		    "name=account" #> (SHtml.select(accounts,Full(ac.account.is.toString),(s:String) => ac.account( s.toLong)))&
		    "name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=numberVd" #> (SHtml.text(ac.numberVd.is, ac.numberVd(_)))&
		    "name=barCode" #> (SHtml.text(ac.barCode.is, ac.barCode(_)))&
		    "name=editableLine" #> (SHtml.text(ac.editableLine.is, ac.editableLine(_)))&
			"name=value" #> (SHtml.text(ac.value.is.toString, (v:String) => { if(v !="") ac.value(v.toDouble)} ))&
			"name=notifications" #> (SHtml.text(ac.notifications.is.toString, 
				(f:String) => ac.notifications(BusinessRulesUtil.snippetToInt(f))))&
			"name=liquidValue" #> (SHtml.text(ac.liquidValue.is.toString, (v:String) => { if(v !="") ac.liquidValue(v.toDouble)} ))&
		    "name=paid" #> (SHtml.checkbox(ac.paid.is, ac.paid(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			//notification
			//"name=offType" #> (SHtml.select(offTypes,Full(ac.offType.is.toString),(v:String) => ac.offType(v.toInt)))&
		}catch {
		    case e: NoSuchElementException => S.error("Mensalidade não existe!")
		    "#Monthly_form *" #> NodeSeq.Empty
  		}
  	}
  	
}

