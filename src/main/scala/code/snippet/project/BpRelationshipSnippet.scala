
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

class  BpRelationshipSnippet extends BootstrapPaginatorSnippet[BpRelationship] {

	def pageObj = BpRelationship

	def types = ("0", "Selecione um Tipo") :: RelationshipType.
	findAllInCompanyOrDefaultCompany(OrderBy(RelationshipType.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def relationships = ("0", "Selecione uma Classe de Relacionamento") :: Relationship.
	findAllInCompanyOrDefaultCompany(OrderBy(Relationship.name, Ascending)).map(t => (t.id.is.toString,t.generic_name.is))

//	def findForListParamsWithoutOrder: List[QueryParam[BpRelationship]] = List(Like(BpRelationship.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	def findForListParamsWithoutOrder: List[QueryParam[BpRelationship]] = List(Like(BpRelationship.obs,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			BpRelationship.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

//	def findForListParams: List[QueryParam[BpRelationship]] = List(Like(BpRelationship.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(BpRelationship.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))
	def findForListParams: List[QueryParam[BpRelationship]] = List(Like(BpRelationship.obs,"%"+BusinessRulesUtil.clearString(name)+"%"),OrderBy(BpRelationship.obs, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage))


	def list(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = BpRelationship.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Relacionamento excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Relacionamento não existe!")
		  				case _ => S.error("Relacionamento não pode ser excluído!")
		  			}
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.bpName),
							"class" -> Text(ac.relationshipName),
							"relatedname" -> Text(ac.relatedName),
							"startat" -> Text(Project.dateToStrOrEmpty(ac.startAt.is)),
							"actions" -> <a class="btn" href={"/bprelationship/bprelationship?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o relacionamento "+ac.obs}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def types(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = RelationshipType.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Tipo de relacionamento excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Tipo de relacionamento não existe!")
		  				case _ => S.error("Tipo de relacionamento não pode ser excluído!")
		  			}
			
			}

			RelationshipType.findAllInCompany(OrderBy(RelationshipType.name, Ascending)).flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/bprelationship/edit_type?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o tipo de Relacionamento "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def relationships(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Relationship.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Classe de relacionamento excluída com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Classe de relacionamento não existe!")
		  				case _ => S.error("Classe de relacionamento não pode ser excluída!")
		  			}
			}

			Relationship.findAllInCompany(OrderBy(Relationship.name, Ascending)).flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"female_name" -> Text(ac.female_name.is),
							"type" -> Text(ac.relationshipTypeName),
							"obs" -> Text(ac.obs.is),
							"actions" -> <a class="btn" href={"/bprelationship/edit_relationship?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o classe de Relacionamento "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getBpRelationship:BpRelationship = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => BpRelationship.create
			case _ => BpRelationship.findByKey(id.toLong).get
		}
	}

	def getRelationshipType:RelationshipType = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => RelationshipType.create
			case _ => RelationshipType.findByKey(id.toLong).get
		}
	}	
	
	def getRelationship:Relationship = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Relationship.create
			case _ => Relationship.findByKey(id.toLong).get
		}
	}	

	def maintainType = {
		try{
			var ac:RelationshipType = getRelationshipType
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Tipo de relacionamento salvo com sucesso!")
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
			"name=orderinreport" #> (SHtml.text(ac.orderInreport.is.toString, (v:String) => ac.orderInreport(v.toInt)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Tipo de relacionamento não existe!")
		    "#Relationshiptype_form *" #> NodeSeq.Empty
  		}
  	}

	def maintainRelationship = {
		try{
			var ac:Relationship = getRelationship
			def process(): JsCmd= {
				ac.company(AuthUtil.company)
			   	ac.save	
			   	S.notice("Classe de relacionamento salvo com sucesso!")
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=female_name" #> (SHtml.text(ac.female_name.is, ac.female_name(_)))&
		    "name=female_short_name" #> (SHtml.text(ac.female_short_name.is, ac.female_short_name(_)))&
		    "name=generic_name" #> (SHtml.text(ac.generic_name.is, ac.generic_name(_)))&
		    "name=generic_short_name" #> (SHtml.text(ac.generic_short_name.is, ac.generic_short_name(_)))&
		    "name=relationshiptype" #> (SHtml.select(types,Full(ac.relationshipType.is.toString),(s:String) => ac.relationshipType( s.toLong)))&
		    "name=reverse" #> (SHtml.select(relationships,Full(ac.relationshipReverse.is.toString),(s:String) => ac.relationshipReverse( s.toLong)))&
			"name=orderinreport" #> (SHtml.text(ac.orderInreport.is.toString, (v:String) => ac.orderInreport(v.toInt)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Classe de relacionamento não existe!")
		    "#relationship_form *" #> NodeSeq.Empty
  		}
  	}

	def maintain = {
		try{
			var ac:BpRelationship = getBpRelationship
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
					ac.save
				   	S.notice("Relacionamento salvo com sucesso!")
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
			"name=bp_related" #> (SHtml.text(ac.bp_related.is.toString, (p:String) => ac.bp_related(p.toLong)))&
			"name=startat" #> (SHtml.text(getDateAsString(ac.startAt.is),
						(date:String) => {
							ac.startAt(Project.strOnlyDateToDate(date))
						}))&
			"name=endat" #> (SHtml.text(getDateAsString(ac.endAt.is),
						(date:String) => {
							ac.endAt(Project.strOnlyDateToDate(date))
						}))&
		    "name=relationship" #> (SHtml.select(relationships,Full(ac.relationship.is.toString),(s:String) => ac.relationship( s.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt))++SHtml.hidden(process))			
		}catch {
		    case e: NoSuchElementException => S.error("Relacionamento não existe!")
		    "#bprelationship_form *" #> NodeSeq.Empty
  		}
  	}
  	
}