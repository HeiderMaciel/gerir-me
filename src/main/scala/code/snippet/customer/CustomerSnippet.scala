
package code
package snippet

import net.liftweb._
import http._
import code.util._
import code.actors._
import net.liftweb.http.PaginatorSnippet
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

import net.liftweb.json._

import java.util.Date

class  CustomerSnippet extends BootstrapPaginatorSnippet[Customer] with net.liftweb.common.Loggable 
{

	def pageObj = Customer

	def operators = ("0" -> "Selecione uma operadora")::DomainTable.findAll(
		By(DomainTable.domain_name,"operadora"),
		OrderBy(DomainTable.name, Ascending)).map(t => (t.cod.is,t.name.is))

	def offsales = ("0" -> "Selecione um convênio")::OffSale.findAllInCompany(OrderBy(OffSale.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def mapicons = ("0" -> "Selecione um ícone")::MapIcon.findAllInCompanyOrDefaultCompanyMapicon.map(t => (t.id.is.toString,t.name.is))

	def degrees = ("0" -> "Selecione um grau de instrução")::InstructionDegree.findAll(OrderBy(InstructionDegree.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def occupations = ("0" -> "Selecione uma profissão")::Occupation.findAll(OrderBy(Occupation.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def civilstatuses = CivilStatus.findAll(OrderBy(CivilStatus.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def breeds = Breed.findAll(OrderBy(Breed.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def speciess = Species.findAll(OrderBy(Species.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	val sexanimals = Seq(
				Customer.Sexs.Female.toString -> "Fêmea",
				Customer.Sexs.Male.toString -> "Macho",
				Customer.Sexs.Undefined.toString -> "Não informado"
	)


	implicit val formats = DefaultFormats // Brings in default date formats etc.

	def units = ("0", "Nenhuma Unidade") :: CompanyUnit.findAllInCompany(OrderBy(CompanyUnit.name, Ascending)).map(t => (t.id.is.toString, t.name.is))

	//var itens = 200;
	def itens = S.param("itenspp_customer") match {
		case Full(s) => s.toInt
		case _ => 20
	}
	
	override def itemsPerPage = itens;

	def document = S.param("document") match {
		case Full(s) => s
		case _ => ""
	}

	def email = S.param("email") match {
		case Full(s) => s
		case _ => ""
	}
	def mapIcon = S.param("mapIcon") match {
		case Full(s) => s
		case _ => ""
	}	
	

	def showProspect = {
		val v = checkBooleanParamenter("prospect")
		logger.info(v.toString)
		v
	}
	def showMember = checkBooleanParamenter("member")
	def showPerson = checkBooleanParamenter("person")
	def showSuplier = checkBooleanParamenter("suplier")
	def showProfessional = checkBooleanParamenter("employee")
	def showUser = checkBooleanParamenter("user")
	def showCustomer = checkBooleanParamenter("customer")

	def document_company = S.param("document_company") match {
		case Full(s) => s
		case _ => ""
	}

	override def page = if(showAll){
		Customer.findAllInCompanyWithInactive(findForListParams :_*)
	}else{
		super.page
	}
	def findForListParamsWithoutOrder: List[QueryParam[Customer]] = Customer.searchCriteriaLowerCase(name,document, email, showProspect, showSuplier, showProfessional, showUser, showCustomer, showPerson, showMember, mapIcon)
	//def findForListParamsWithoutOrder: List[QueryParam[Customer]] = Customer.searchCriteriaLowerCase(name,document, showProspect, showSuplier, showProfessional, showUser, showCustomer)

	def findForListParams: List[QueryParam[Customer]] = 
	if (AuthUtil.company.appType.isGerirme) {
		findForListParamsWithoutOrder ::: (List(OrderBy(Customer.createdAt, Descending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage) )).asInstanceOf[List[QueryParam[Customer]]]
	} else {
		findForListParamsWithoutOrder ::: (List(OrderBy(Customer.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage) )).asInstanceOf[List[QueryParam[Customer]]]
	}

	
	def customers(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = Customer.findByKey(id.toLong).get	
		  				if (ac.is_animal_?) {
			  				ac.delete_!
			  				S.notice("Animal excluído com sucesso!")
	  					} else {
			  				ac.delete_!
			  				S.notice(AuthUtil.company.appCustName("Cliente") + " excluído com sucesso!")
	  					}
		  			}catch{
		  				case e: NoSuchElementException => S.error(AuthUtil.company.appCustName("Cliente") + " não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error(AuthUtil.company.appCustName("Cliente") + " não pode ser excluído!")
		  			}
			}
		
		page.flatMap(ac => 
			bind("f", xhtml,
							"name" -> Text(ac.name.is),

							"idforcompany" -> Text(ac.idForCompany.is.toString),
							"short_name" -> Text(ac.short_name.is),
						    "civilstatus" -> (SHtml.select(civilstatuses,Full(ac.civilstatus.is.toString),(v:String) => ac.civilstatus(v.toInt))),
						    "mapicon" -> (SHtml.select(mapicons,Full(ac.mapIcon.is.toString),(v:String) => ac.mapIcon(v.toInt))),
						    "sex" -> (SHtml.select(sexs,Full(ac.sex.is.toString),(v:String) => ac.sex(v.toString))),
							"birthday" -> Text(Project.dateToStrOrEmpty(ac.birthday.is)),
						    "instructiondegree" -> (SHtml.select(degrees,Full(ac.instructiondegree.is.toString),(v:String) => ac.instructiondegree(v.toInt))),
						    "occupation" -> (SHtml.select(occupations,Full(ac.occupation.is.toString),(v:String) => ac.occupation(v.toInt))),
			//			    "obscomplement" -> Text(ac.obsComplement.is),
							"obscomplement" -> (SHtml.textarea(ac.obsComplement.is, ac.obsComplement(_))),				
							"street" -> Text(ac.street.is),
							"number" -> Text(ac.number.is),
							"complement" -> Text(ac.complement.is),
							"district" -> Text(ac.district.is),
							"cityname" -> Text(ac.cityName),
							"statename" -> Text(ac.stateShortName),
							"postal_code" -> Text(ac.postal_code.is),

							"email" -> Text(ac.email.is), 
							"phone" -> Text(ac.mobilePhone.is + " " + ac.phone.is + " " + ac.email_alternative.is),
							"document" -> Text(ac.document.is + " " + ac.document_identity.is + " " + ac.document_company.is),
							"thumb" -> ac.thumb ("36"),
							"iconthumb" -> {if (!ac.mapIcon.isEmpty) {
								ac.mapIcon.obj.get.thumb ("32")
								}else{
									NodeSeq.Empty
								}
							},
							"iconpath" -> <img src={ac.iconPath}/>,
							"obs" -> Text(ac.obs.is),
							"patient" -> <a alt="Prontuário" href={"/records/edit_patient?id="+ac.id.is}> <img width='24' src='/images/records.png'/></a>,
							"actions" -> <a alt="Cadastro" href={"/customer/edit?id="+ac.id.is}> <img src='/images/edit.png'/></a>,
							"delete" -> SHtml.submit("",delete,"class" -> "delete-button danger","data-confirm-message"->{" excluir o registro "+ac.name}),							
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getCustomer:Customer = {
		//var id =  openOr "0"
		S.param("id") match {
			case Full(s) if (s == "0") => Customer.create
			case Full(id) => Customer.findByKey(id.toLong).get
			case _ => Customer.create
		}
	}

	def imageSave = {
		for {
			r <- S.request if r.post_?
				data <- S.param("transloadit")
		}{
			//S.notice(data);
			val transloadit2str = "["+data+"]"
			val json = parse(transloadit2str)
			val a = json.extract[List[TrasloaditResponse]]
			val thumb = a(0).results.thumb.url
			val image = a(0).uploads(0).url
			val customer = getCustomer
			ImageCustomer.createInCompany.customer(customer).thumb(thumb).image(image).save

		}
	}
	def maintain() = {
		try{

			imageSave
			var ac:Customer = getCustomer
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
			   		ac.save	
			   		if (ac.is_animal_?.is) {
				   		S.notice("Animal salvo com sucesso!")
				   		S.redirectTo("/animal/edit_animal?id="+ac.id.is)
			   		} else {
				   		S.notice(AuthUtil.company.appCustName("Cliente") + " salvo com sucesso!")
				   		S.redirectTo("/customer/edit?id="+ac.id.is)
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
			"name=createdAt" #> (SHtml.text(getDateAsString(ac.createdAt.is),
						(date:String) => {ac.createdAt(Project.strOnlyDateToDate(date))}))&
			"name=updatedAt" #> (SHtml.text(getDateAsString(ac.updatedAt.is),
						(date:String) => {ac.updatedAt(Project.strOnlyDateToDate(date))}))&
			"name=createdby" #> (SHtml.text(ac.createdByName, (p)=> {} ))&
			"name=updatedby" #> (SHtml.text(ac.updatedByName, (p)=> {} ))&
			"name=is_person" #> (SHtml.checkbox(ac.is_person_?, ac.is_person_?(_)))&
			"name=is_member" #> (SHtml.checkbox(ac.is_member_?, ac.is_member_?(_)))&
			"name=is_prospect" #> (SHtml.checkbox(ac.is_prospect_?, ac.is_prospect_?(_)))&
			"#img_customer" #> ac.thumb("192")&
			"#img_customer_short" #> ac.thumb("80")&
			"name=custInfo" #> (SHtml.textarea(ac.custInfo, (a:String) => {}))&
			"#img_mapicon" #> ac.mapIcon.obj.get.thumb ("36")&
			"name=is_suplier" #> (SHtml.checkbox(ac.is_suplier_?, ac.is_suplier_?(_)))&
			"name=is_brand" #> (SHtml.checkbox(ac.is_brand_?, ac.is_brand_?(_)))&
			"name=is_employee" #> (SHtml.checkbox(ac.is_employee_?, ac.is_employee_?(_)))&
		    "name=userstatus" #> (SHtml.select(status,Full(ac.userStatus.is.toString),(v:String) => ac.userStatus(v.toInt)))&
			"name=is_user" #> (SHtml.checkbox(ac.is_user_?, ac.is_user_?(_)))&
			"name=is_customer" #> (SHtml.checkbox(ac.is_customer_?, ac.is_customer_?(_)))&
			"name=is_auxiliar" #> (SHtml.checkbox(ac.is_auxiliar_?, ac.is_auxiliar_?(_)))&
			"name=is_animal" #> (SHtml.checkbox(ac.is_animal_?, ac.is_animal_?(_)))&
			"name=idForCompany" #> (SHtml.text(ac.idForCompany.is.toString, (f:String) => { 
					if(f != "")
						ac.idForCompany(f.toInt)
					else
						ac.idForCompany(0)

			}))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=external_id" #> (SHtml.text(ac.external_id.is, ac.external_id(_)))&
		    "name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=obscomplement" #> (SHtml.textarea(ac.obsComplement.is, ac.obsComplement(_)))&
		    "name=email" #> (SHtml.text(ac.email.is, ac.email(_))) &
			"name=senNotifications" #> (SHtml.text(ac.senNotifications.is, ac.senNotifications(_)))&
		    "name=website" #> (SHtml.text(ac.website.is, ac.website(_))) &
		    "name=barcode" #> (SHtml.text(ac.barcode.is, ac.barcode(_)))&
		    "name=phone" #> (SHtml.text(ac.phone.is, ac.phone(_)))&
		    "name=email_alternative" #> (SHtml.text(ac.email_alternative.is, ac.email_alternative(_)))&
			"name=mobilePhone" #> (SHtml.text(ac.mobilePhone.is, ac.mobilePhone(_)))&
			"name=document_offsale" #> (SHtml.text(ac.document_offsale.is, ac.document_offsale(_)))&
			"name=date_offsale" #> (SHtml.text(getDateAsString(ac.date_offsale),
						(date:String) => {
							ac.date_offsale(Project.strOnlyDateToDate(date))
						}))&			
			"name=begindate" #> (SHtml.text(getDateAsString(ac.beginDate),
						(date:String) => {
							ac.beginDate(Project.strOnlyDateToDate(date))
						}))&			
			"name=document" #> (SHtml.text(ac.document.is, ac.document(_)))&
			"name=document_identity" #> (SHtml.text(ac.document_identity.is, ac.document_identity(_)))&
			"name=document_company" #> (SHtml.text(ac.document_company.is, ac.document_company(_)))&
			"name=company_name" #> (SHtml.text(ac.company_name.is, ac.company_name(_)))&
			"name=document_city" #> (SHtml.text(ac.document_city.is, ac.document_city(_)))&
			"name=document_state" #> (SHtml.text(ac.document_state.is, ac.document_state(_)))&
			"name=sex" #> (SHtml.select(sexs,Full(ac.sex.is), ac.sex(_)))&
			"name=sexAnimal" #> (SHtml.select(sexanimals,Full(ac.sexAnimal.is), ac.sexAnimal(_)))&
		    "name=civilstatus" #> (SHtml.select(civilstatuses,Full(ac.civilstatus.is.toString),(v:String) => ac.civilstatus(v.toInt)))&
		    "name=weight" #> (SHtml.text(ac.weight.is.toString, (s:String) => ac.weight(BusinessRulesUtil.snippetToDouble(s))))&
		    "name=height" #> (SHtml.text(ac.height.is.toString, (s:String) => ac.height(BusinessRulesUtil.snippetToDouble(s))))&
			"name=bmi" #> (SHtml.text(ac.BMI, (a:String) => {}))&
			"name=ageBirth" #> (SHtml.text(ac.ageBirth, (a:String) => {}))&
			"name=birthday" #> (SHtml.text(getDateAsString(ac.birthday),
						(date:String) => {
							ac.birthday(Project.strOnlyDateToDate(date))
						}))&			
			"name=deathDate" #> (SHtml.text(getDateAsString(ac.deathDate),
						(date:String) => {
							ac.deathDate(Project.strOnlyDateToDate(date))
						}))&			
		    "name=state_ref" #> (SHtml.text(ac.stateRef.is.toString, (s:String) => ac.stateRef(s.toLong)))&
			"name=city_ref" #> (SHtml.text(ac.cityRef.is.toString, (s:String) => ac.cityRef(s.toLong)))&
		    "name=city" #> (SHtml.text(ac.city.is, ac.city(_)))&
		    "name=state" #> (SHtml.text(ac.state.is, ac.state(_)))&
		    "name=unit" #> (SHtml.select(units,Full(ac.unit.is.toString),(v:String) => ac.unit(v.toLong)))&
		    "name=offsale" #> (SHtml.select(offsales,Full(ac.offsale.is.toString),(s:String) => ac.offsale( s.toLong)))&
		    "name=mapicon" #> (SHtml.select(mapicons,Full(ac.mapIcon.is.toString),(s:String) => ac.mapIcon( s.toLong)))&
		    "name=mobilePhoneOp" #> (SHtml.select(operators,Full(ac.mobilePhoneOp.is.toString),(s:String) => ac.mobilePhoneOp( s.toLong)))&
		    "name=instructiondegree" #> (SHtml.select(degrees,Full(ac.instructiondegree.is.toString),(s:String) => ac.instructiondegree( s.toLong)))&
		    "name=occupation" #> (SHtml.select(occupations,Full(ac.occupation.is.toString),(s:String) => ac.occupation( s.toLong)))&
		    "name=breed" #> (SHtml.select(breeds,Full(ac.breed.is.toString),(s:String) => ac.breed( s.toInt)))&
		    "name=species" #> (SHtml.select(speciess,Full(ac.species.is.toString),(s:String) => ac.species( s.toInt)))&
		    "name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&
		    "name=street" #> (SHtml.text(ac.street.is, ac.street(_))) &
		    "name=pointofreference" #> (SHtml.textarea(ac.pointofreference.is, ac.pointofreference(_))) &
		    "name=valueinpoints" #> (SHtml.text(ac.valueInPoints.is.toString, (s:String) => ac.valueInPoints(BusinessRulesUtil.snippetToDouble(s))))&
		    "name=district" #> (SHtml.text(ac.district.is, ac.district(_)))&
		    "name=postal_code" #> (SHtml.text(ac.postal_code.is, ac.postal_code(_)))&
			"name=lng" #> (SHtml.text(ac.lng.is, ac.lng(_)))&
			"name=lat" #> (SHtml.text(ac.lat.is, ac.lat(_)))&
			"name=number" #> (SHtml.text(ac.number.is, ac.number(_)))&
			"name=complement" #> (SHtml.text(ac.complement.is, ac.complement(_)))&
			"#img_thumb" #> ac.thumb&
			"name=manager" #> (SHtml.text(ac.bp_managerName, (a:String) => {}))&
			"name=indicatedby" #> (SHtml.text(ac.bp_indicatedbyName, (a:String) => {}))&
			"name=bp_indicatedby" #> (SHtml.text(ac.bp_indicatedby.is.toString, (p:String) => ac.bp_indicatedby(BusinessRulesUtil.snippetToLong(p))))&
			"name=bp_manager" #> (SHtml.text(ac.bp_manager.is.toString, (p:String) => ac.bp_manager(BusinessRulesUtil.snippetToLong(p))))&
			"name=name" #> (SHtml.text(ac.name.is, ac.name(_))++SHtml.hidden(process))

		}catch {
		    case (e:Exception) => {
		    	S.error(AuthUtil.company.appCustName("Cliente") + " não existe! " + e.getMessage) 
		    	"#costumer_form *" #> NodeSeq.Empty
			}
  		}
  	}

  	def imageList = {
  		try{
	  		<div>
	  			{
	  				getCustomer.images.map((image:ImageCustomer)=>{
	  					<a href={image.image.is} target="imagepage" >
	  						<img src={image.image.is} width="200"/>	
	  					</a>
	  				})
	  			}
	  		</div>
  		}catch{
  			case _ => <div></div>
  		}

  	}


  	def formSeachCustomer() = {
  		var name = ""
  		var document  = ""
  		var email  = ""
  		def process():JsCmd = {
	  			var id = S.param("id_customer_search_form")
	  			id match {
	  				case Full(ids) if(ids != "") => {
	  					val c =  AuthUtil.company.findCustomerByKey(ids.toLong)
	  					c match {
	  						case Full(customer) => JsRaw("selectCustomer('"+customer.id+"','"+customer.name+"', ["+customer.alerts_messages.map("'"+_+"'").join(",")+"])")
	  						case _ => Alert(AuthUtil.company.appCustName("Cliente") + " "+ids+" não existe!")&JsRaw("$('.id_customer_search').val('')")
	  					}

	  				}
	  				case _ => getTableCustomers(name,document,email)	
	  			}
  			
  			
  		}
		"name=name" #> (SHtml.text(name, name = _))&
		"name=email" #> (SHtml.text(email, email = _))&
		"name=document" #> ((SHtml.text(document, document = _))++SHtml.hidden(process))
  	}

	def getTableCustomers(name:String,document:String,email:String) = {
		val customers = Customer.searchCustomer(AuthUtil.company.id.is,name,document,email,150)
		val htmlReturn =
			<thead>
				<tr>
					<th>Id</th>
					<th>Nome</th>
					<th>Telefone</th>
					<th>Email</th>
					<th></th>
				</tr>
			</thead>			
			<tbody>
			{
			customers.map(d => (
					<tr>
						<td>{d.id}</td>
						<td>{d.name}</td>
						<td>{d.mobilePhone + " " + d.phone + " " + d.email_alternative}</td>
						<td>{d.email}</td>
						<td>
							<img src="/images/select.png" width="20" onclick={"selectCustomer('"+d.id+"','"+d.name+"')"}></img>
						</td>
					</tr>))
				}
			</tbody>;
			SetHtml("table_customer",htmlReturn)	
	}   
  	def thumbToShow:NodeSeq = getCustomer.thumb("192")
}


case class TrasloaditResponse(assembly_id:String,assembly_url:String, uploads:List[Upload],results:Results)

case class Upload(url:String)
case class Results(thumb:Thumb)
case class Thumb(url:String)


