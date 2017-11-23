
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

class  ActivitySnippet extends BootstrapPaginatorSnippet[Activity] with SnippetUploadImage  {

	def pageObj = Activity

	def findForListParamsWithoutOrder: List[QueryParam[Activity]] = List(Like(Activity.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))
	override def page = {
		if(!showAll){
			super.page
		}else{
			Activity.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}

	def statusFilter: List[Int] = if(showAll){
		List(Activity.STATUS_OK, Activity.STATUS_INACTIVE)
	}else{
		List(Activity.STATUS_OK)
	}

	def findForListParams: List[QueryParam[Activity]] = List(
		Like(Activity.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
		BySql (productTypeList,IHaveValidatedThisSQL("","")),
		OrderBy(Activity.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage), ByList(Activity.status,statusFilter))
	def types_allow_null = ("0" ,"Nenhum") :: types
	def types = ProductType.findAllService.map(t => (t.id.is.toString,t.name.is))
	def igroups = InvoiceGroup.findAllInCompanyOrDefaultCompany(OrderBy(InvoiceGroup.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def productType = S.param("productType") match {
		case Full(s) => s
		case _ => ""
	}	
    val productTypeList = if(productType != ""){
        "typeproduct = %s ".format (productType)
    }else{
        " 1 = 1 "
    }

	def costcenters = ("0" -> "Selecione um Centro de Custo")::
	CostCenter.findAllInCompany(OrderBy(CostCenter.name, Ascending)).map(cc => (cc.id.is.toString,cc.name.is))
	def accountcategories = ("0" -> "Selecione uma Categoria")::
	AccountCategory.findAllInCompany(OrderBy(AccountCategory.name, Ascending)).map(cc => (cc.id.is.toString,cc.name.is))

	def genders = ((Activity.MALE, "Masculino")::(Activity.FEMALE, "Feminino")::(Activity.BOTH, "Ambos")::Nil).map(t => (t._1,t._2))

	def activitiesForSelect = ("0" ,"Nenhum") :: AuthUtil.company.activities(false).map(a =>(a.id.is.toString,a.name.is))

	def activitys(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Activity.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Serviço excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Serviço não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Serviço não pode ser excluído!")
		  			}
			
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"shortname" -> Text(ac.short_name.is),
							"duration" -> Text(ac.duration.is),
							"obs" -> Text(ac.obs.is),
							"type" -> Text(ac.typeActivityName),
							"price" -> Text(ac.salePrice.is.toString),
							"commission" -> Text(ac.commission.is.toString),
							"bpmonthly" -> Text(if(ac.bpmonthly_?.is){ "Sim" }else{ "Não" }),
							"bpmcount" -> Text(ac.bpmCount.is.toString),
							"actions" -> <a class="btn" href={"/activity/edit?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" excluir o serviço "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def types(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = ProductType.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Tipo de produto/serviço excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Tipo de produto/serviço não existe!")
		  				case _ => S.error("Tipo de produto/serviço não pode ser excluído!")
		  			}
			
			}

			ProductType.findAllService.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"igname" -> Text(ac.invoiceGroupName),
							"actions" -> <a class="btn" href={"/activity/edit_type?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o tipo de produto/serviço "+ac.name}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}
	def getActivity:Activity = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Activity.create
			case _ => Activity.findByKey(id.toLong).get
		}
	}
	
	def getActivityType:ProductType = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => ProductType.createTypeService
			case _ => ProductType.findByKey(id.toLong).get
		}
	}


	def maintainType = {
		try{
			var ac:ProductType = getActivityType
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Tipo de serviço salvo com sucesso!")
					S.redirectTo("/activity/edit_type?id="+ac.id.is)
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
		    "name=invoiceGroup" #> (SHtml.select(igroups,Full(ac.invoiceGroup.is.toString),(s:String) => ac.invoiceGroup( s.toLong)))&
		    "name=showObs" #> (SHtml.checkbox(ac.showObs_?.is, ac.showObs_?(_)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Tipo de serviço não existe!")
		    "#activity_form *" #> NodeSeq.Empty
  		}
  	}

	def maintain = {
		try{
			var ac:Activity = getActivity
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
					ac.clearBons
					ac.is_bom_?(S.params("discounts").size >0)
					ac.save
					S.params("discounts").map(_.toLong).foreach((id) =>{
						ProductBOM.createInCompany.product(ac.id).qtd(1).product_bom(id).discount_of_commision_?(true).save
					})
				   	//ac.save	
				   	S.notice("Serviço salvo com sucesso!")
			   		S.redirectTo("/activity/edit?id="+ac.id.is)
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
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=showObs" #> (SHtml.checkbox(ac.showObs_?.is, ac.showObs_?(_)))&
		    "name=showInCommand" #> (SHtml.checkbox(ac.showInCommand_?.is, ac.showInCommand_?(_)))&
		    "name=showInCalendarPub" #> (SHtml.checkbox(ac.showInCalendarPub_?.is, ac.showInCalendarPub_?(_)))&
		    "name=orderInCommand" #> (SHtml.text(ac.orderInCommand.is.toString, 
		    	(s:String) => ac.orderInCommand(BusinessRulesUtil.snippetToInt(s)))) &
		    "name=allowSimultaneos" #> (SHtml.checkbox(ac.allowSimultaneos_?.is, ac.allowSimultaneos_?(_)))&
		    "name=customernotification" #> (SHtml.checkbox(ac.customernotification_?.is, ac.customernotification_?(_)))&
		    "name=usernotification" #> (SHtml.checkbox(ac.usernotification_?.is, ac.usernotification_?(_)))&
		    "name=bpmonthly" #> (SHtml.checkbox(ac.bpmonthly_?.is, ac.bpmonthly_?(_)))&
		    "name=bpmCount" #> (SHtml.text(ac.bpmCount.is.toString, 
		    	(s:String) => ac.bpmCount(BusinessRulesUtil.snippetToInt(s)))) &
		    "name=outsideService" #> (SHtml.checkbox(ac.outsideService_?.is, ac.outsideService_?(_)))&
		    "name=crmService" #> (SHtml.checkbox(ac.crmService_?.is, ac.crmService_?(_)))&
		    "name=showInRecords" #> (SHtml.checkbox(ac.showInRecords_?.is, ac.showInRecords_?(_)))&
		    "name=color" #> (SHtml.text(ac.color.is, ac.color(_)))&
		    "name=duration" #> (SHtml.text(ac.duration.is, ac.duration(_))) &
		    "name=type" #> (SHtml.select(types,Full(ac.typeProduct.is.toString),(v:String) => ac.typeProduct(v.toLong)))&
		    "name=costcenter" #> (SHtml.select(costcenters,Full(ac.costCenter.is.toString),(s:String) => ac.costCenter( s.toLong)))&
		    "name=accountCategory" #> (SHtml.select(accountcategories,Full(ac.accountCategory.is.toString),(s:String) => ac.accountCategory( s.toLong)))&
		    "name=discountAccountCategory" #> (SHtml.select(accountcategories,Full(ac.discountAccountCategory.is.toString),(s:String) => ac.discountAccountCategory( s.toLong)))&
		    "name=gender" #> (SHtml.select(genders,Full(ac.gender.is.toString),(v:String) => ac.gender(v)))&
			"name=price" #> (SHtml.text(ac.salePrice.is.toString, 
				(f:String) => ac.salePrice(BusinessRulesUtil.snippetToDouble(f))))&
			"name=pointsonbuy" #> (SHtml.text(ac.pointsOnBuy.is.toString, 
				(f:String) => ac.pointsOnBuy(BusinessRulesUtil.snippetToDouble(f))))&
			"name=pointsprice" #> (SHtml.text(ac.pointsPrice.is.toString, 
				(f:String) => ac.pointsPrice(BusinessRulesUtil.snippetToDouble(f))))&
		    "name=conflictsallowed" #> (SHtml.text(ac.conflictsallowed.is.toString, 
		    	(s:String) => ac.conflictsallowed(BusinessRulesUtil.snippetToInt(s)))) &
			"name=discount_text" #> (SHtml.text(ac.discounts_text, (v:String) => {}))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),
				(v:String) => ac.status(v.toInt)))&
			"name=commission" #> (SHtml.text(ac.commission.is.toString, 
				(f:String) => ac.commission(BusinessRulesUtil.snippetToDouble(f))))&
			"name=commissionAbs" #> (SHtml.text(ac.commissionAbs.is.toString, 
				(f:String) => ac.commissionAbs(BusinessRulesUtil.snippetToDouble(f))))&
			"name=auxPrice" #> (SHtml.text(ac.auxPrice.is.toString, 
				(f:String) => ac.auxPrice(BusinessRulesUtil.snippetToDouble(f))))&
			"name=auxPercent" #> (SHtml.text(ac.auxPercent.is.toString, 
				(f:String) => ac.auxPercent(BusinessRulesUtil.snippetToDouble(f))))&
			"name=auxHousePrice" #> (SHtml.text(ac.auxHousePrice.is.toString, 
				(f:String) => ac.auxHousePrice(BusinessRulesUtil.snippetToDouble(f))))&
			"name=auxHousePercent" #> (SHtml.text(ac.auxHousePercent.is.toString, 
				(f:String) => ac.auxHousePercent(BusinessRulesUtil.snippetToDouble(f))))&
			"name=discountPrice" #> (SHtml.text(ac.discountPrice.is.toString, 
				(f:String) => ac.discountPrice(BusinessRulesUtil.snippetToDouble(f))))&
			"name=discountPercent" #> (SHtml.text(ac.discountPercent.is.toString, 
				(f:String) => ac.discountPercent(BusinessRulesUtil.snippetToDouble(f))))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Serviço não existe!")
		    "#activity_form *" #> NodeSeq.Empty
  		}
  	}
  	
  	def maintainUserActivity = {
		prepareFormUserActivity  		
  	}

  	def getUserActivity = {
		def id = S.param("id_") openOr "0"
		id match {
			case "0" => UserActivity.createInCompany
			case _ => UserActivity.findByKey(id.toLong).get
		}
  	}
	
	def user:User =  S.param("id") match{
  		case Full(s:String) => {
  			User.findByKey(s.toLong).get
  		}
  		case _ => User.create
  	}

  	def prepareFormUserActivity = {

		try{
			var uac:UserActivity = getUserActivity
			uac.user(user)
			def process(): JsCmd= {
				try {
				   	uac.save
				   	S.notice("Serviço salvo com sucesso!")
				   	S.redirectTo("/user/edit?id="+uac.user.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=duration" #> (SHtml.text(uac.duration.is, uac.duration(_))) &
		    "name=activitys" #> (SHtml.select(activitiesForSelect,Full(uac.activity.is.toString),(v:String) => uac.activity(v.toLong)))&
		    "name=type" #> (SHtml.select(types_allow_null,Full(uac.producttype.is.toString),(v:String) => uac.producttype(v.toLong)))&
			"name=price" #> (SHtml.text(uac.price.is.toString, (v:String) => uac.price(v.toDouble)))&
			"name=commission" #> (SHtml.text(uac.commission.is.toString, (v:String) => uac.commission(v.toDouble)))&
			"name=obs" #> (SHtml.textarea(uac.obs.is, uac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Serviço não existe!")
		    "#activity_form *" #> NodeSeq.Empty
  		}  		
  	}

	def userActivities(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = UserActivity.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Serviço excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Serviço não existe!")
		  				case _ => S.error("Serviço não pode ser excluído!")
		  			}
			
			}
			user.userActivities.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name),
							"type" -> Text(ac.typeName),
							"duration" -> Text(ac.duration.is),
							"obs" -> Text(ac.obs.is),
							"commission" -> Text(ac.commission.is.toString),
							"price" -> Text(ac.price.is.toString),
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger", "data-confirm-message" -> ""),
							"edit" -> <a class="btn" href={"/user/edit?id="+ac.user.is+"&id_="+ac.id.is}>Editar</a>,
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}  	
	def setImageToEntity(homeName:String, thumbName:String){
		val activity = getActivity
		activity.image(homeName).imagethumb(thumbName).save
	}
  	def imageFolder:String = Activity.imagePath
  	def thumbToShow:NodeSeq = getActivity.thumb("128")
}

