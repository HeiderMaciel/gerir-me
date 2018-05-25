
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

class  QuizSnippet extends BootstrapPaginatorSnippet[Quiz] {

	def pageObj = Quiz

	def quizs = ("0", "Selecione um " + Quiz.quizLabel) :: Quiz.
		findAllInCompany(OrderBy(Quiz.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def sections = ("0", "Selecione uma Seção") :: QuizSection.
		findAllInCompany(OrderBy(QuizSection.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def questions = ("0", "Selecione uma Questão") :: QuizQuestion.
		findAllInCompany(OrderBy(QuizQuestion.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def domains = ("0", "Selecione um Domínio") :: QuizDomain.
		findAllInCompanyOrDefaultCompany(OrderBy(QuizDomain.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def domainitems = ("0", "Selecione um Item de Domínio") :: QuizDomainItem.
		findAllInCompanyOrDefaultCompany(OrderBy(QuizDomainItem.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def groups = ("0", "Selecione um Grupo") :: UserGroup.
		findAllInCompany(OrderBy(UserGroup.name, Ascending)).map(t => (t.id.is.toString, t.name.is))

	def questionTypes = (
		("0", "Texto")::
		("1", "Parágrafo")::
		("2", "Lista")::
		("3", "Multipla escolha")::
		("4", "Pode vários")::
		("5", "Valor")::
		("6", "Data")::
		Nil).map(t => (t._1,t._2))

	def questionSizes = (
		("0", "Mini")::
		("1", "Curto")::
		("2", "Médio")::
		("3", "Grande")::
		("4", "X Grande")::
		("5", "XX Grande")::Nil).map(t => (t._1,t._2))

	def questionPositions = (
		("0", "Só")::
		("1", "Início")::
		("2", "Meio")::
		("3", "Fim")::Nil).map(t => (t._1,t._2))

	def questionFormats = (
		("0", "Reduzido - Esquerda")::
		("2", "Reduzido - Topo")::
		("3", "Longo - Esquerda")::
		("1", "Longo - Topo")::
		Nil).map(t => (t._1,t._2))

	def quizSection = S.param("quizSection") match {
		case Full(s) => s
		case _ => ""
	}	
    val quizSectionList = if(quizSection != ""){
        "quizSection = %s ".format (quizSection)
    }else{
        " 1 = 1 "
    }

	def findForListParamsWithoutOrder: List[QueryParam[Quiz]] = List(Like(Quiz.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			Quiz.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def findForListParams: List[QueryParam[Quiz]] = List(Like(Quiz.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(Quiz.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Quiz.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice(Quiz.quizLabel + " excluído(a) com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error(Quiz.quizLabel + " não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error(Quiz.quizLabel + " não pode ser excluído(a)!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"usergroup" -> Text(ac.userGroupName),
							"share" -> Text(if(ac.share_?.is){ "Sim" }else{ "Não" }),
							"showinrecords" -> Text(if(ac.showInRecords_?.is){ "Sim" }else{ "Não" }),
							"actions" -> <a class="btn" href={"/quiz_admin/quiz?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o(a) " + Quiz.quizLabel + " " + ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def sections(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = QuizSection.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Seção de " + Quiz.quizLabel + " excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Seção de avaliação não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Seção de " + Quiz.quizLabel + " não pode ser excluída!")
		  			}
			
			}

			QuizSection.findAllInCompany(
				Like(QuizSection.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
				OrderBy(QuizSection.quiz,Ascending),
				OrderBy(QuizSection.orderInQuiz,Ascending),
				OrderBy(QuizSection.name,Ascending)
				).flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"quizname" -> Text(ac.quizName),
							"obs" -> Text(ac.obs.is),
							"orderinquiz" -> Text(ac.orderInQuiz.toString),
							"actions" -> <a class="btn" href={"/quiz_admin/edit_section?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir a seção de " + Quiz.quizLabel + " " + ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def questions(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = QuizQuestion.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Questão excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Questão não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Questão não pode ser excluída!")
		  			}
			
			}

			QuizQuestion.findAllInCompany(
				Like(QuizQuestion.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
				BySql (quizSectionList,IHaveValidatedThisSQL("","")),
				OrderBy(QuizQuestion.quizSection,Ascending),
				OrderBy(QuizQuestion.orderInSection,Ascending),
				OrderBy(QuizQuestion.name,Ascending)
				).flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"short_name" -> Text(ac.short_name.is),
							"quizsectionname" -> Text(ac.quizSectionName),
							"quizdomainname" -> Text(ac.quizDomainName),
							"obs" -> Text(ac.obs.is),
							"orderinsection" -> Text(ac.orderInSection.toString),
							"actions" -> <a class="btn" href={"/quiz_admin/edit_question?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir a questão "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def domains(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = QuizDomain.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Domínio excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Domínio não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Domínio não pode ser excluído!")
		  			}
			
			}

			QuizDomain.findAllInCompany(OrderBy(QuizDomain.name,Ascending)).flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/quiz_admin/edit_domain?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o domínio "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def domainitems(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = QuizDomainItem.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Item de Domínio excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Domínio não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Item de Domínio não pode ser excluído!")
		  			}
			
			}

			def quizDomain = S.param("quizDomain") match {
				case Full(s) => s
				case _ => ""
			}	
		    val quizDomainList = if(quizDomain != ""){
		        "quizDomain = %s ".format (quizDomain)
		    }else{
		        " 1 = 1 "
		    }
		
		    //BySql (quizDomainList,IHaveValidatedThisSQL("",""))
			QuizDomainItem.findAllInCompany(
				OrderBy(QuizDomainItem.quizDomain,Ascending),
				OrderBy(QuizDomainItem.orderInDomain,Ascending),
				OrderBy(QuizDomainItem.name,Ascending),
				BySql (quizDomainList,IHaveValidatedThisSQL("",""))).flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"quizdomainname" -> Text(ac.quizDomainName),
							"orderindomain" -> Text(ac.orderInDomain.is.toString),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/quiz_admin/edit_domainitem?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o item de domínio "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getQuiz:Quiz = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Quiz.create
			case _ => Quiz.findByKey(id.toLong).get
		}
	}

	def getQuizSection:QuizSection = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => QuizSection.create
			case _ => QuizSection.findByKey(id.toLong).get
		}
	}	
	
	def getQuizQuestion:QuizQuestion = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => QuizQuestion.create
			case _ => QuizQuestion.findByKey(id.toLong).get
		}
	}	

	def getQuizDomain:QuizDomain = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => QuizDomain.create
			case _ => QuizDomain.findByKey(id.toLong).get
		}
	}	

	def getQuizDomainItem:QuizDomainItem = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => QuizDomainItem.create
			case _ => QuizDomainItem.findByKey(id.toLong).get
		}
	}	

	def getQuizApplying:QuizApplying = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => QuizApplying.create
			case _ => QuizApplying.findByKey(id.toLong).get
		}
	}	

	def maintainSection = {
		try{
			var ac:QuizSection = getQuizSection
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Seção de " + Quiz.quizLabel + " salva com sucesso!")
    		   		S.redirectTo("/quiz_admin/edit_section?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=quiz" #> (SHtml.select(quizs,Full(ac.quiz.is.toString),(s:String) => ac.quiz( s.toLong)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&			
			"name=weight" #> (SHtml.text(ac.weight.is.toString, (v:String) =>{ if(v !=""){ac.weight(v.toDouble)};}))&
			"name=rank" #> (SHtml.text(ac.rank.is.toString, (v:String) =>{ if(v !=""){ac.rank(v.toDouble)};}))&
			"name=rankPercent" #> (SHtml.text(ac.rankPercent.is.toString, (v:String) =>{ if(v !=""){ac.rankPercent(v.toDouble)};}))&
			"name=orderinquiz" #> (SHtml.text(ac.orderInQuiz.is.toString, (v:String) => ac.orderInQuiz(v.toInt)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Seção de " + Quiz.quizLabel + " não existe!")
		    "#QuizSection_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainQuestion = {
		try{
			var ac:QuizQuestion = getQuizQuestion
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Questão salva com sucesso!")
    		   		S.redirectTo("/quiz_admin/edit_question?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=quizSection" #> (SHtml.select(sections,Full(ac.quizSection.is.toString),(s:String) => ac.quizSection( s.toLong)))&
		    "name=quizDomain" #> (SHtml.select(domains,Full(ac.quizDomain.is.toString),(s:String) => ac.quizDomain( s.toLong)))&
		    //"name=gender" #>         (SHtml.select(genders,      Full(ac.gender.is.toString),(v:String) => ac.gender(v)))&
		    //"name=quizQuestionTypeStr" #> (SHtml.select(questionTypes,Full(ac.quizQuestionTypeStr.is.toString),(v:String) => ac.quizQuestionTypeStr(v)))&
		    "name=quizQuestionType" #> (SHtml.select(questionTypes,Full(ac.quizQuestionType.is.toString),(v:String) => ac.quizQuestionType(v.toInt)))&
		    "name=quizQuestionFormat" #> (SHtml.select(questionFormats,Full(ac.quizQuestionFormat.is.toString),(v:String) => ac.quizQuestionFormat(v.toInt)))&
		    "name=quizQuestionSize" #> (SHtml.select(questionSizes,Full(ac.quizQuestionSize.is.toString),(v:String) => ac.quizQuestionSize(v.toInt)))&
		    "name=quizQuestionPosition" #> (SHtml.select(questionPositions,Full(ac.quizQuestionPosition.is.toString),(v:String) => ac.quizQuestionPosition(v.toInt)))&
			"name=history" #> (SHtml.checkbox(ac.history_?, ac.history_?(_)))&
		    "name=addon" #> (SHtml.text(ac.addon.is, ac.addon(_)))&
			"name=rank" #> (SHtml.text(ac.rank.is.toString, (v:String) =>{ if(v !=""){ac.rank(v.toDouble)};}))&
			"name=orderinsection" #> (SHtml.text(ac.orderInSection.is.toString, (v:String) => ac.orderInSection(v.toInt)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&			
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Questão não existe!")
		    "#QuizQuestion_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainDomain = {
		try{
			var ac:QuizDomain = getQuizDomain
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Domínio salvo com sucesso!")
    		   		S.redirectTo("/quiz_admin/edit_domain?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=min" #> (SHtml.text(ac.min.is, ac.min(_)))&
		    "name=max" #> (SHtml.text(ac.max.is, ac.max(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&			
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Domínio não existe!")
		    "#Domain_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainDomainItem = {
		try{
			var ac:QuizDomainItem = getQuizDomainItem
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Item de Domínio salvo com sucesso!")
    		   		S.redirectTo("/quiz_admin/edit_domainitem?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=quizDomain" #> (SHtml.select(domains,Full(ac.quizDomain.is.toString),(s:String) => ac.quizDomain( s.toLong)))&
			"name=orderindomain" #> (SHtml.text(ac.orderInDomain.is.toString, (v:String) => ac.orderInDomain(v.toInt)))&
		    "name=color" #> (SHtml.text(ac.color.is, ac.color(_)))&
			"name=rank" #> (SHtml.text(ac.rank.is.toString, (v:String) =>{ if(v !=""){ac.rank(v.toDouble)};}))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&			
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Item de Domínio não existe!")
		    "#DomainItem_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainQuizApplying = {
		try{
			var ac:QuizApplying = getQuizApplying
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
					ac.save
				   	S.notice("Aplicação " + Quiz.quizLabel + " salva com sucesso! " + ac.message.length + " de um máximo de 40.000 caracteres")
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
			"name=bpName" #> (SHtml.text(ac.bpName, (p)=> {} ))&
			"name=quizName" #> (SHtml.text(ac.quizName, (p)=> {} ))&
			"name=applyDate" #> (SHtml.text(getDateAsString(ac.applyDate.is),
						(date:String) => {
							ac.applyDate(Project.strOnlyDateToDate(date))
						}))&
		    "name=quiz" #> (SHtml.select(quizs,Full(ac.quiz.is.toString),(s:String) => ac.quiz( s.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=message" #> (SHtml.textarea(ac.message.is, ac.message(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error("Aplicação " + Quiz.quizLabel + " não existe!")
		    "#quizzappy *" #> NodeSeq.Empty
  		}
  	}

	def maintain = {
		try{
			var ac:Quiz = getQuiz
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
					ac.save
				   	S.notice(Quiz.quizLabel + " salvo(a) com sucesso! " + ac.message.length + " de um máximo de 40.000 caracteres")
			   		S.redirectTo("/quiz_admin/quiz?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=userGroup" #> (SHtml.select(groups,Full(ac.userGroup.is.toString),(s:String) => ac.userGroup( s.toLong)))&
		    "name=share" #> (SHtml.checkbox(ac.share_?.is, ac.share_?(_)))&
		    "name=showInRecords" #> (SHtml.checkbox(ac.showInRecords_?.is, ac.showInRecords_?(_)))&
			"name=rank" #> (SHtml.text(ac.rank.is.toString, (v:String) =>{ if(v !=""){ac.rank(v.toDouble)};}))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=message" #> (SHtml.textarea(ac.message.is, ac.message(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error(Quiz.quizLabel + " não existe!")
		    "#quiz_form *" #> NodeSeq.Empty
  		}
  	}
  	
}

