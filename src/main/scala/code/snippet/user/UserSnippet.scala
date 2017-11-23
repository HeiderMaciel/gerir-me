
package code
package snippet

import net.liftweb._
import http._

import code.util._
import code.actors._
import code.service._
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

class  UserSnippet extends BootstrapPaginatorSnippet[User] {

	/**	
	Pagination Methods
	*/
	def pageObj = User
	
	def statusFilter: List[Int] = if(showAll){
		List(User.STATUS_OK, User.STATUS_BLOCKED, User.STATUS_INACTIVE)
	}else{
		List(User.STATUS_OK)
	}

	def findForListParamsWithoutOrder: List[QueryParam[User]] = List(Like(User.search_name,"%"+BusinessRulesUtil.clearString(name)+"%")
		, 
		Like(User.mobilePhone,"%"+phone+"%")
		)

	def findForListParams: List[QueryParam[User]] = List(OrderBy(User.search_name, Ascending), Like(User.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"), 
		//Like(ac.mobilePhone,"%"+phone+"%"), 
		BySql ("((phone like '%"+phone+"%') or (mobile_phone like '%"+phone+"%') or (email_alternative like '%"+phone+"%'))",IHaveValidatedThisSQL("","")),
		BySql (userGroupList,IHaveValidatedThisSQL("","")),
		StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage), ByList(User.userStatus,statusFilter))

	val days = Seq(
	  				WeekDay.Mon.toString -> "Segunda" ,
	  				WeekDay.Tue.toString -> "Terça" ,
	  				WeekDay.Wed.toString -> "Quarta" ,	
	  				WeekDay.Thu.toString -> "Quinta",
	  				WeekDay.Fri.toString -> "Sexta",
	  				WeekDay.Sat.toString -> "Sábado",
	  				WeekDay.Sun.toString -> "Domingo",
	  				WeekDay.WorkDays.toString -> "Dias de Semana",
	  				WeekDay.All.toString -> "Todos"
  				)

	def honorifictitles = ("0" -> "Selecione uma forma de tratamento")::HonorificTitle.findAll(OrderBy(HonorificTitle.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def councils = ("0" -> "Selecione um conselho")::DomainTable.findAll(
		By(DomainTable.domain_name,"conselhoprofissional"),
		OrderBy(DomainTable.name, Ascending)).map(t => (t.cod.is,t.name.is))

	def degrees = ("0" -> "Selecione um grau de instrução")::InstructionDegree.findAll(OrderBy(InstructionDegree.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def units = ("0", "Selecione uma Unidade") :: CompanyUnit.findAllInCompany(OrderBy(CompanyUnit.name, Ascending)).map(t => (t.id.is.toString, t.name.is))
    val group_label = if (AuthUtil.company.appType.isEdoctus || AuthUtil.company.appType.isEphysio
      || AuthUtil.company.appType.isEsmile || AuthUtil.company.appType.isEbellepet) {
    	"uma Especialidade"
    } else {
    	"um Grupo"
    }
	def groups = ("0", "Selecione " + group_label) :: UserGroup.findAllInCompany(OrderBy(UserGroup.name, Ascending)).map(t => (t.id.is.toString, t.name.is))

	def civilstatuses = ("0" -> "Não Informado")::CivilStatus.findAll(OrderBy(CivilStatus.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def phone = S.param("phone") match {
		case Full(s) => s
		case _ => ""
	}

	def userGroup = S.param("userGroup") match {
		case Full(s) => s
		case _ => ""
	}	
//	LogActor ! "PARAMETRo " + userGroup
//	LogActor ! "PARAMETRo LISTA " + userGroupList
    val userGroupList = if(userGroup != ""){
        "group_c = %s ".format (userGroup)
    }else{
        " 1 = 1 "
    }


	def users(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = User.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Profissional excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Profissional não existe!")
		  				case _ => S.error("Profissional não pode ser excluído! Você pode alterar o status para inativo.")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"short_name" -> Text(ac.short_name.is),
							"password" -> Text(ac.password.is),
							"email" -> Text(ac.email.is),
							"phone" -> Text(ac.mobilePhone.is + " " + ac.phone.is + " " + ac.email_alternative.is),
							"usergroup" -> Text(ac.userGroupName),
							"unit" -> Text(ac.unitName),
//							"username" -> Text(ac.userName.is),
						    //"name=userName" #> (SHtml.text(ac.userName.is, ac.userName(_))) &
							//"groupPermission" -> SHtml.text(ac.groupPermission, (a:String) => {}),
							"gp" -> Text(ac.groupPermission.is),
							"actions" -> <a class="btn btn-default" href={"/user/edit?id="+ac.id.is}>Editar</a>,
//							"actions" -> <a class="btn" href={"/user/edit_new?id="+user.id.is}>Editar</a>,
							"thumb" -> ac.thumb ("36"),
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger btn-danger","data-confirm-message" -> {" excluir o profissional "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}
	def workHouers(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val day = WorkHouer.findByKey(id.toLong).get	
		  				day.delete_!
		  				S.notice("Profissional excluído com sucesso!")
		  			}catch{

		  				case e: NoSuchElementException =>{LogActor ! "Dia {"+id+"} não existe!";  S.error("Dia não existe!");}
		  			}
			
			}

			getUser.workHouers.flatMap(workHouer => 
			bind("f", xhtml,"start" -> Text(workHouer.start.is),
							"end" -> Text(workHouer.end.is),
							"start_lanch" -> Text(workHouer.startLanch.is),
							"end_lanch" -> Text(workHouer.endLanch.is),
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger"),
							"day" -> Text(workHouer.day.translate),
							"_id" -> SHtml.text(workHouer.id.is.toString, id = _)
				)
			)
	}	

	def maintain() = {
		try{
			var ac:User = getUser
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
					ac.groupPermission(S.params("groupPermission").foldLeft("")(_+","+_))
				   	ac.save
				   	S.notice("Profissional salvo com sucesso!")
				   	S.redirectTo("/user/edit?id="+ac.id.is)
				}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
			"name=allowshowonsite" #> (SHtml.checkbox(ac.allowShowOnSite_?, ac.allowShowOnSite_?(_)))&
			"name=allowshowonportal" #> (SHtml.checkbox(ac.allowShowOnPortal_?, ac.allowShowOnPortal_?(_)))&
			"name=moderatedportal" #> (SHtml.checkbox(ac.moderatedPortal_?, ac.moderatedPortal_?(_)))&
		    "name=sitetitle" #> (SHtml.text(ac.siteTitle.is, ac.siteTitle(_)))&
		    "name=sitedescription" #> (SHtml.textarea(ac.siteDescription.is, ac.siteDescription(_)))&
		    "name=external_id" #> (SHtml.text(ac.external_id.is, ac.external_id(_)))&
			"#img_user" #> ac.thumb("192")&
//		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
//		    "name=userName" #> (SHtml.text(ac.userName.is, ac.userName(_))) &
		    "name=password" #> (SHtml.password(ac.password.is, ac.password(_))) &
		    "name=phone" #> (SHtml.text(ac.phone.is, ac.phone(_)))&
			"name=mobilePhone" #> (SHtml.text(ac.mobilePhone.is, ac.mobilePhone(_)))&
			"name=email_alternative" #> (SHtml.text(ac.email_alternative.is, ac.email_alternative(_)))&
			"name=birthday" #> (SHtml.text(getDateAsString(ac.birthday),
						(date:String) => {
							ac.birthday(Project.strOnlyDateToDate(date))
						}))&			
			"name=hireDate" #> (SHtml.text(getDateAsString(ac.hireDate),
						(date:String) => {
							ac.hireDate(Project.strOnlyDateToDate(date))
						}))&			
			"name=resignationDate" #> (SHtml.text(getDateAsString(ac.resignationDate),
						(date:String) => {
							ac.resignationDate(Project.strOnlyDateToDate(date))
						}))&			
		    "name=sex" #> (SHtml.select(sexs,Full(ac.sex.is),ac.sex(_)))&					    
		    "name=civilstatus" #> (SHtml.select(civilstatuses,Full(ac.civilstatus.is.toString),(v:String) => ac.civilstatus(v.toInt)))&
		    "name=unit" #> (SHtml.select(units,Full(ac.unit.is.toString),(v:String) => ac.unit(v.toLong)))&
			"name=document" #> (SHtml.text(ac.document.is, ac.document(_)))&
			"name=document_identity" #> (SHtml.text(ac.document_identity.is, ac.document_identity(_)))&
			"name=document_company" #> (SHtml.text(ac.document_company.is, ac.document_company(_)))&
			"name=document_council" #> (SHtml.text(ac.document_council.is, ac.document_council(_)))&
		    "name=council" #> (SHtml.select(councils,Full(ac.council.is.toString),(s:String) => ac.council( s.toLong)))&
		    "name=instructiondegree" #> (SHtml.select(degrees,Full(ac.instructiondegree.is.toString),(s:String) => ac.instructiondegree( s.toLong)))&
		    "name=state_ref" #> (SHtml.text(ac.stateRef.is.toString, (s:String) => ac.stateRef(s.toLong)))&
			"name=city_ref" #> (SHtml.text(ac.cityRef.is.toString, (s:String) => ac.cityRef(s.toLong)))&
			"name=city" #> (SHtml.text(ac.city.is, ac.city(_)))&
			"name=state" #> (SHtml.text(ac.state.is, ac.state(_)))&
			"name=street" #> (SHtml.text(ac.street.is, ac.street(_))) &
			"name=district" #> (SHtml.text(ac.district.is, ac.district(_)))&
			"name=postal_code" #> (SHtml.text(ac.postal_code.is, ac.postal_code(_)))&		    
		    "name=pointofreference" #> (SHtml.textarea(ac.pointofreference.is, ac.pointofreference(_))) &
			"name=lng" #> (SHtml.text(ac.lng.is, ac.lng(_)))&
			"name=lat" #> (SHtml.text(ac.lat.is, ac.lat(_)))&
		    "name=group" #> (SHtml.select(groups,Full(ac.group.is.toString),(v:String) => ac.group(v.toLong)))&
		    "name=status" #> (SHtml.select(status,Full(ac.userStatus.is.toString),(v:String) => ac.userStatus(v.toInt)))&
			"name=number" #> (SHtml.text(ac.number.is, ac.number(_)))&		    
			"name=complement" #> (SHtml.text(ac.complement.is, ac.complement(_)))&
			"name=showInCalendar" #> (SHtml.checkbox(ac.showInCalendar_?, ac.showInCalendar_?(_)))&
			"name=calendarFixed" #> (SHtml.checkbox(ac.calendarFixed_?, ac.calendarFixed_?(_)))&
			"name=showInCalendarPub" #> (SHtml.checkbox(ac.showInCalendarPub_?, ac.showInCalendarPub_?(_)))&
			"name=showInCommand" #> (SHtml.checkbox(ac.showInCommand_?, ac.showInCommand_?(_)))&
			"name=showInCashier" #> (SHtml.checkbox(ac.showInCashier_?, ac.showInCashier_?(_)))&
			"name=discountToCommission" #> (SHtml.checkbox(ac.discountToCommission_?, ac.discountToCommission_?(_)))&
			"name=deletePayment" #> (SHtml.checkbox(ac.deletePayment_?, ac.deletePayment_?(_)))&
			"name=is_auxiliar" #> (SHtml.checkbox(ac.is_auxiliar_?, ac.is_auxiliar_?(_)))&
			"name=groupPermission_text" #> (SHtml.text(ac.groupPermission, (a:String) => {}))&
			"#img_thumb" #> ac.thumb&
			"name=orderInCalendar" #> (SHtml.text(ac.orderInCalendar.is.toString, (s:String) => ac.orderInCalendar(s.toInt)))&
			"name=parent" #> (SHtml.text(ac.parent.is.toString, 
				(f:String) => ac.parent(BusinessRulesUtil.snippetToLong(f))))&
//			"name=parent_percent" #> (SHtml.text(ac.parent_percent.is.toString, (v:String) =>{ if(v !=""){user.parent_percent(BusinessRulesUtil.snippetToDouble(v))};}))&
			"name=parent_percent" #> (SHtml.text(ac.parent_percent.is.toString, 
				(f:String) => ac.parent_percent(BusinessRulesUtil.snippetToDouble(f))))&
			"name=cancreatecalendarevents" #> (SHtml.checkbox(ac.canCreateCalendarEvents_?, ac.canCreateCalendarEvents_?(_)))&
			"name=candeletecalendarevents" #> (SHtml.checkbox(ac.canDeleteCalendarEvents_?, ac.canDeleteCalendarEvents_?(_)))&
			"name=canmovecalendarevents" #> (SHtml.checkbox(ac.canMoveCalendarEvents_?, ac.canMoveCalendarEvents_?(_)))&
			"name=caneditcalendarevents" #> (SHtml.checkbox(ac.canEditCalendarEvents_?, ac.canEditCalendarEvents_?(_)))&
		    "name=email" #> (SHtml.text(ac.email.is, ac.email(_)))&
			"name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
			"name=process" #> (SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Profissional não existe!")
		    "#user_form *" #> NodeSeq.Empty
  		}
  	}
  	def getUser:User = {
		var id = S.param("id") openOr "0"
		var user:User = {
					id match {
						case "0" => User.create
						case _ => User.findByKey(id.toLong).get
					}
		}
		user
  	}
  	def workHouersMaintain() = {
  		try{
		  	var day:WorkHouer = WorkHouer.createInCompany
		  	day.user(getUser)
		  	def saveDay(): JsCmd= {
		  		day.save
		  		S.redirectTo("/user/edit?id="+day.user)
		  		S.notice("Adicionado com sucesso!")
			}
			"name=start" #> (SHtml.text(day.start.is, day.start(_)))&
			"name=end" #> (SHtml.text(day.end.is, day.end(_)))&
			"name=start_lanch" #> (SHtml.text(day.startLanch.is, day.startLanch(_)))&
			"name=end_lanch" #> (SHtml.text(day.endLanch.is, day.endLanch(_)))&
			"name=days" #> (SHtml.select(days, Empty,day.day(_))++SHtml.hidden(saveDay))
		}catch {
		    case e: NoSuchElementException => S.error("Profissional não existe!")
		    "#user_form *" #> NodeSeq.Empty
  		}

  	}


  	def usersJsonFromCalendar(html:NodeSeq):NodeSeq = {
  			def unit = S.param("unit") openOr ""
  			def userId = S.param("user") openOr ""
  			def onlyCurrenunit = true//unit == ""
  			/*if(userId != ""){
  				val user = User.findByKey(userId.toLong).get
  				AuthUtil << user.unit.obj.get
  			}*/
  			if(unit != ""){
  				AuthUtil << CompanyUnit.findByKey(unit.toLong).get
  			}
	  		Script(
		  			OnLoad(
			  				Call("buildCalendar",
									JsArray(AuthUtil.company.usersForCalendar( onlyCurrenunit  ).map(
												u 	=> JsObj(
													("name",u.thumbAndName),
													("id",u.id.is),
													("group",u.group.is)
													//,("hours", JsArray(u.workHouers.map(h => JsObj(("day", h.day.is),("start", h.start.is),("end", h.end.is),("startLanch", h.startLanch.is),("endLanch", h.endLanch.is))) ))
												)
											)
									),
									JsArray(),
									AuthUtil.company.calendarInterval.is,
									AuthUtil.company.calendarIntervalAlt.is,
									AuthUtil.company.calendarStart.is,
									AuthUtil.company.calendarEnd.is,
									//AuthUtil.company.autoIncrementCommand_?.is,
									AuthUtil.company.commandControl.is,
									AuthUtil.company.autoOpenCalendar_?.is,
									JsObj(
										("newEvent", AuthUtil.user.canCreateCalendarEvents_?.is),
										("deleteEvent", AuthUtil.user.canDeleteCalendarEvents_?.is),
										("moveEvent", AuthUtil.user.canMoveCalendarEvents_?.is),
										("editEvent", AuthUtil.user.canEditCalendarEvents_?.is)
									),
									JsObj(
										("useTreatmentClass", AuthUtil.company.useTreatmentAsAClass_?.is)
									),
									AuthUtil.unit.id.is,
									AuthUtil.company.calendarShowLight_?.is, // calendarShowLight
									AuthUtil.company.calendarShowInterval_?.is // calendarShowInterval
			  					)
			  			)
		  		)
  	}
}

