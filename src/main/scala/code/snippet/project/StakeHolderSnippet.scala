
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

class  StakeHolderSnippet extends BootstrapPaginatorSnippet[StakeHolder] {

	def pageObj = StakeHolder

	def projects = ("0" -> "Selecione um Projeto/Evento")::Project1.findAllInCompany.map(cc => (cc.id.is.toString,cc.name.is))
	def staketypes = ("0" -> "Selecione um Tipo de Participante") :: StakeHolderType.findAllInCompanyOrDefaultCompany.map(t => (t.id.is.toString,t.name.is))

	val approveds = Seq(
				StakeHolder.Approveds.Approved.toString -> "Aprovado",
				StakeHolder.Approveds.NotApproved.toString -> "Reprovado",
				StakeHolder.Approveds.Undefined.toString -> "Não se aplica"
	)

	def findForListParamsWithoutOrder: List[QueryParam[StakeHolder]] = List(Like(StakeHolder.obs,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			StakeHolder.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[StakeHolder]] = List(Like(StakeHolder.obs,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(StakeHolder.obs, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))

	def list (xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = StakeHolder.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Participante excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Participante não existe!")
		  				case _ => S.error("Participante não pode ser excluído!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.bpName),
							"stakeholdertype" -> Text(ac.stakeHolderTypeName),
							"startat" -> Text(Project.dateToStrOrEmpty(ac.startAt.is)),
							"actions" -> <a class="btn" href={"/project/edit_stakeholder?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o participante "+ac.stakeHolderTypeName}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def staketypes(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = StakeHolderType.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Tipo de participante excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Tipo de participante não existe!")
		  				case _ => S.error("Tipo de participante não pode ser excluído!")
		  			}
			
			}

			StakeHolderType.findAllInCompany.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/project/edit_stakeholdertype?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o tipo de Participante "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getStakeHolderType:StakeHolderType = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => StakeHolderType.create
			case _ => StakeHolderType.findByKey(id.toLong).get
		}
	}	
	

	def getStakeHolder:StakeHolder = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => StakeHolder.create
			case _ => StakeHolder.findByKey(id.toLong).get
		}
	}	

	def maintainStakeHolderType = {
		try{
			var ac:StakeHolderType = getStakeHolderType
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Tipo de participante salvo com sucesso!")
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
			"name=orderinreport" #> (SHtml.text(ac.orderInReport.is.toString, (v:String) => ac.orderInReport(v.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Tipo de Participante não existe!")
		    "#stakeholdertype_form *" #> NodeSeq.Empty
  		}
  	}


	def maintain = {
		try{
			var ac:StakeHolder = getStakeHolder
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
				ac.save
			   	S.notice("Participante salvo com sucesso!")
			   	S.redirectTo("/project/edit_stakeholder?id="+ac.id.is)
			}
			"name=createdAt" #> (SHtml.text(getDateAsString(ac.createdAt.is),
						(date:String) => {ac.createdAt(Project.strOnlyDateToDate(date))}))&
			"name=updatedAt" #> (SHtml.text(getDateAsString(ac.updatedAt.is),
						(date:String) => {ac.updatedAt(Project.strOnlyDateToDate(date))}))&
			"name=createdby" #> (SHtml.text(ac.createdByName, (p)=> {} ))&
			"name=updatedby" #> (SHtml.text(ac.updatedByName, (p)=> {} ))&
			"name=startat" #> (SHtml.text(getDateAsString(ac.startAt.is),
						(date:String) => {
							ac.startAt(Project.strOnlyDateToDate(date))
						}))&
			"name=endat" #> (SHtml.text(getDateAsString(ac.endAt.is),
						(date:String) => {
							ac.endAt(Project.strOnlyDateToDate(date))
						}))&
		    "name=stakeholdertype" #> (SHtml.select(staketypes,Full(ac.stakeHolderType.is.toString),(s:String) => ac.stakeHolderType( s.toLong)))&
		    "name=project" #> (SHtml.select(projects,Full(ac.project.is.toString),(s:String) => ac.project( s.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
			"name=business_pattern" #> (SHtml.text(ac.business_pattern.is.toString, (p:String) => ac.business_pattern(p.toLong)))&
		    "name=approved" #> (SHtml.select(approveds,Full(ac.approved.is),ac.approved(_)))&					    
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error("Participante não existe!")
		    "#StakeHolder_form *" #> NodeSeq.Empty
  		}
  	}

}

