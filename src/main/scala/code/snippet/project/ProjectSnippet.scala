
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

class  ProjectSnippet extends BootstrapPaginatorSnippet[Project1] {

	def pageObj = Project1

	def units = ("0", "Selecione uma Unidade") :: CompanyUnit.findAllInCompany().map(t => (t.id.is.toString, t.name.is))
	def types = ("0", "Selecione um Tipo de Projeto") :: ProjectType.findAllInCompany.map(t => (t.id.is.toString,t.name.is))
	def classes = if (ProjectClass.findAllInCompany.length != 1) {
			("0", "Selecione uma Classe de Projeto") :: ProjectClass.findAllInCompany.map(t => (t.id.is.toString,t.name.is))
		} else {
			ProjectClass.findAllInCompany.map(t => (t.id.is.toString,t.name.is))
		}

	def termss = ("0", "Selecione um Termo") :: Terms.findAllInCompany.map(t => (t.id.is.toString,t.name.is))
	def stages = ("0", "Selecione um Estágio") :: ProjectStage.findAllInCompany.map(t => (t.id.is.toString,t.name.is))
	def costcenters = ("0" -> "Selecione um Centro de Custo")::CostCenter.findAllInCompany.map(cc => (cc.id.is.toString,cc.name.is))
	def printTypes = (
		("0", "Tabela")::
		("1", "Lista")::
		Nil).map(t => (t._1,t._2))

	def findForListParamsWithoutOrder: List[QueryParam[Project1]] = List(Like(Project1.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			Project1.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[Project1]] = List(Like(Project1.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(Project1.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			def opt : String = S.param ("opt") match {
					case Full(p) => {
						p
					}
					case _ => {
						if (S.uri.contains ("budget")) {
							"budget"
						} else if (S.uri.contains ("group")) {
							"group"
						} else if (S.uri.contains ("event")) {
							"event"
						} else {
							"project"
						}
					}
				}			
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Project1.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Projeto excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Projeto não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Projeto não pode ser excluído!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"class" -> Text(ac.projectClassName),
							"startat" -> Text(Project.dateToStrOrEmpty(ac.startAt.is)),
							"actions" -> <a class="btn" href={
								if (opt == "budget") {
								  "/budget/budget?id="+ac.id.is+"&opt=budget"
								  } else if (opt == "event") {
								  "/project/event?id="+ac.id.is+"&opt=event"
								  } else if (opt == "group") {
								  "/project_group/group?id="+ac.id.is+"&opt=group"
								  } else {
								  "/project/event?id="+ac.id.is+"&opt=project"
								  }
								}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o projeto "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def types(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = ProjectType.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Tipo de projeto/evento excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Tipo de projeto/evento não existe!")
		  				case _ => S.error("Tipo de projeto/evento não pode ser excluído!")
		  			}
			
			}

			ProjectType.findAllInCompany.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/project/edit_type?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o tipo de Projeto "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def stages(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = ProjectStage.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Estágio de projeto/evento excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Estágio de projeto/evento não existe!")
		  				case _ => S.error("Estágio de projeto/evento não pode ser excluído!")
		  			}
			
			}

			def aclist = if (!showAll) {
				ProjectStage.findAllInCompany(
					By(ProjectStage.status,ProjectStage.STATUS_OK),
					Like(ProjectStage.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
					OrderBy(ProjectStage.name, Ascending))
			} else {
				ProjectStage.findAllInCompanyWithInactive(
					Like(ProjectStage.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
					OrderBy(ProjectStage.name, Ascending))
			}

			aclist.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/project/edit_stage?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o estágio de Projeto "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def classes(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = ProjectClass.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Classe de projeto/evento excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Classe de projeto/evento não existe!")
		  				case _ => S.error("Classe de projeto/evento não pode ser excluída!")
		  			}
			
			}

			ProjectClass.findAllInCompany.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"type" -> Text(ac.projectTypeName),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/project/edit_class?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir a classe de projeto "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getProject:Project1 = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Project1.create
			case _ => Project1.findByKey(id.toLong).get
		}
	}

	def getProjectType:ProjectType = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => ProjectType.create
			case _ => ProjectType.findByKey(id.toLong).get
		}
	}	

	def getProjectStage:ProjectStage = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => ProjectStage.create
			case _ => ProjectStage.findByKey(id.toLong).get
		}
	}	
	
	def getProjectClass:ProjectClass = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => ProjectClass.create
			case _ => ProjectClass.findByKey(id.toLong).get
		}
	}	

	def maintainType = {
		try{
			var ac:ProjectType = getProjectType
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Tipo de projeto/evento salvo com sucesso!")
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=class" #> (SHtml.checkbox(ac.class_?.is, ac.class_?(_)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Tipo de projeto/evento não existe!")
		    "#Projecttype_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainStage = {
		try{
			var ac:ProjectStage = getProjectStage
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Estágio de projeto/evento salvo com sucesso!")
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),
				(v:String) => ac.status(v.toInt)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Estágio de projeto/evento não existe!")
		    "#Projectstage_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainClass = {
		try{
			var ac:ProjectClass = getProjectClass
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Classe de projeto/evento salva com sucesso!")
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=projecttype" #> (SHtml.select(types,Full(ac.projectType.is.toString),(s:String) => ac.projectType( s.toLong)))&
		    "name=terms" #> (SHtml.select(termss,Full(ac.terms.is.toString),(s:String) => ac.terms( s.toLong)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=printType" #> (SHtml.select(printTypes,Full(ac.printType.is.toString),(v:String) => ac.printType(v.toInt)))&
		    "name=headerStyle" #> (SHtml.textarea(ac.headerStyle.is, ac.headerStyle(_)))&
		    "name=bodyStyle" #> (SHtml.textarea(ac.bodyStyle.is, ac.bodyStyle(_)))&
		    "name=contentStyle" #> (SHtml.textarea(ac.contentStyle.is, ac.contentStyle(_)))&
		    "name=columns" #> (SHtml.textarea(ac.columns.is, ac.columns(_)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Classe de projeto/evento não existe!")
		    "#Projectclass_form *" #> NodeSeq.Empty
  		}
  	}

	def maintain () = {
		def opt : String = S.param ("opt") match {
				case Full(p) => p
				case _ => {
					if (S.uri.contains ("budget")) {
						"budget"
					} else if (S.uri.contains ("group")) {
						"group"
					} else if (S.uri.contains ("event")) {
						"event"
					} else {
						"project"
					}
				}
			}			

		try{
			var ac:Project1 = getProject
			def process(): JsCmd = {
				try {
					ac.company(AuthUtil.company)
					if (ac.projectOpt == 0) {
						ac.projectOpt (ac.prjOpt (opt))
					}
					ac.save
				   	if (opt == "budget") {
					   	S.notice("Orçamento salvo com sucesso!")
				   		S.redirectTo("/budget/budget?id="+ac.id.is+"&opt=budget")
				   	} else if (opt == "event") {
					   	S.notice("Evento salvo com sucesso!")
			   			S.redirectTo("/project/event?id="+ac.id.is+"&opt=event")
				   	} else if (opt == "group" || AuthUtil.company.appType.isEgrex) {
					   	S.notice("Grupo salvo com sucesso!")
			   			S.redirectTo("/project_group/group?id="+ac.id.is+"&opt=group")
			   		} else {
					   	S.notice("Orçamento salvo com sucesso!")
				   		S.redirectTo("/project/project?id="+ac.id.is+"&opt=project")
//					   	S.notice("Projeto salvo com sucesso!")
//			   			S.redirectTo("/project/event?id="+ac.id.is+"&opt=project")
			   		}
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
			"name=allowshowonsite" #> (SHtml.checkbox(ac.allowShowOnSite_?, ac.allowShowOnSite_?(_)))&
			"name=allowshowonportal" #> (SHtml.checkbox(ac.allowShowOnPortal_?, ac.allowShowOnPortal_?(_)))&
			"name=moderatedportal" #> (SHtml.checkbox(ac.moderatedPortal_?, ac.moderatedPortal_?(_)))&
		    "name=sitetitle" #> (SHtml.text(ac.siteTitle.is, ac.siteTitle(_)))&
		    "name=sitedescription" #> (SHtml.textarea(ac.siteDescription.is, ac.siteDescription(_)))&
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
			"name=startat" #> (SHtml.text(getDateAsString(ac.startAt.is),
						(date:String) => {
							ac.startAt(Project.strOnlyDateToDate(date))
						}))&
			"name=endat" #> (SHtml.text(getDateAsString(ac.endAt.is),
						(date:String) => {
							ac.endAt(Project.strOnlyDateToDate(date))
						}))&
		    "name=projectclass" #> (SHtml.select(classes,Full(ac.projectClass.is.toString),(s:String) => ac.projectClass( s.toLong)))&
		    "name=projectstage" #> (SHtml.select(stages,Full(ac.projectStage.is.toString),(s:String) => ac.projectStage( s.toLong)))&
		    "name=numberofguests" #> (SHtml.text(ac.numberofguests.is.toString, (s:String) => ac.numberofguests(s.toInt))) &
			"name=projectOpt" #> (SHtml.text(ac.projectOpt.is.toString, (s:String) => {}))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
			"name=about" #> (SHtml.textarea(ac.about.is, ac.about(_)))&
			"name=schedule" #> (SHtml.textarea(ac.schedule.is, ac.schedule(_)))&
		    "name=unit" #> (SHtml.select(units,Full(ac.unit.is.toString),(v:String) => ac.unit(v.toLong)))&
		    "name=costcenter" #> (SHtml.select(costcenters,Full(ac.costCenter.is.toString),(s:String) => ac.costCenter( s.toLong)))&
			"name=bp_sponsor" #> (SHtml.text(ac.bp_sponsor.is.toString, (p:String) => ac.bp_sponsor(BusinessRulesUtil.snippetToLong(p))))&
			"name=bp_manager" #> (SHtml.text(ac.bp_manager.is.toString, (p:String) => ac.bp_manager(BusinessRulesUtil.snippetToLong(p))))&
			"name=manager" #> (SHtml.text(ac.bp_managerName, (a:String) => {}))&
//			"name=bp_sponsorname" #> (SHtml.text(ac.bp_sponsorName, (a:String) => {}))&
		    "name=terms1" #> (SHtml.select(termss,Full(ac.projectClassTerms.toString), (a:String) => {}))&
			"name=projectClassPrintType" #> (SHtml.text(ac.projectClassPrintType, (a:String) => {}))&
			"name=projectClassHeaderStyle" #> (SHtml.text(ac.projectClassHeaderStyle, (a:String) => {}))&
			"name=projectClassBodyStyle" #> (SHtml.text(ac.projectClassBodyStyle, (a:String) => {}))&
			"name=projectClassContentStyle" #> (SHtml.text(ac.projectClassContentStyle, (a:String) => {}))&
			"name=projectClassColumns" #> (SHtml.text(ac.projectClassColumns, (a:String) => {}))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error("Projeto não existe!")
		    "#Project_form *" #> NodeSeq.Empty
  		}
  	}
  	
}

