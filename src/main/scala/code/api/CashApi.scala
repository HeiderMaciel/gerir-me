package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

import net.liftweb._
import mapper._
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers

import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object CashApi extends RestHelper with net.liftweb.common.Logger  {

	serve {
		//case "cash" :: "test" :: Nil JsonGet _ =>JInt(1)		
		case "cash" :: "getProductPreviousDebts" :: customerId :: Nil JsonGet _ =>{
			lazy val product:ProductPreviousDebts = ProductPreviousDebts.productPreviousDebts
			lazy val productCredit:ProductPreviousDebts = ProductPreviousDebts.productCredits
			
			if (customerId != "0") {
				val customer = Customer.findByKey(customerId.toLong).get
				JsObj(("name",product.name.is), 
					  ("id",product.id.is), 
					  ("price",customer.totalDebit.toDouble),
					  ("credit", productCredit.id.is)
					)
			} else {
				JsObj(("name",product.name.is), 
					  ("id",product.id.is), 
					  ("price", 0.0),
					  ("credit", productCredit.id.is)
					)
			}
		}
		case "cash" :: "getPaymentTypes" :: Nil JsonGet _ => {
			JsArray(
				PaymentType.findAllForOption.map((pt:PaymentType)=>{
						JsObj(
							  ("status","success"),
							  ("id",pt.id.is),("name",pt.name.is),
							  ("cheque",pt.cheque_?.is),
							  ("needChequeInfo",pt.needChequeInfo_?.is),
							  ("creditCard",pt.creditCard_?.is),
							  ("needCardInfo",pt.needCardInfo_?.is),
							  ("customerRegisterDebit",pt.customerRegisterDebit_?.is), // 21/11/2017 rigel
							  ("customerUseCredit",pt.customerUseCredit_?.is), // 21/11/2017 rigel
							  ("numDaysForReceive",pt.numDaysForReceive.is),
							  ("accept_installment",pt.acceptInstallment_?.is)
							 )
					})
				)
		}

		case "cash" :: "getCheques" :: Nil JsonGet _ => {
			JsArray(
				Cheque.findAllInCompany(
					OrderBy (Cheque.id, Ascending)).map((ch:Cheque)=>{
						JsObj(
							  ("status","success"),
							  ("id",ch.id.is),("chequeDesc",ch.chequeDesc)
							 )
					})
				)
		}
		
		case "cash" :: "checkoutOpen" :: dateStart :: valueStart :: unitStart :: obs :: Nil JsonGet _ => {
			try{			
				def dateStartValue = Project.strOnlyDateToDate(dateStart)
				def valueStartValue = valueStart.toDouble
				def obsAux = if (obs == "index") {
					""
					} else {
						obs
					}
				def cashierOpen = {
					if (unitStart.isEmpty || unitStart.toLong == 0) {
						Cashier.open openerAt(dateStartValue) firstStartValue (valueStartValue) startValue(valueStartValue) obs(obsAux) from(AuthUtil.unit)
					} else {
						// rigel - permite abrir caixa em unidade diferente da do profissional
						Cashier.open openerAt(dateStartValue) firstStartValue (valueStartValue) startValue(valueStartValue) obs(obsAux) from(CompanyUnit.findByKey(unitStart.toLong).get)
					}
				}
				JsObj(("status","success"),("id",cashierOpen.idForCompany.is))
			}catch{
				case e:NumberFormatException  => JsObj(("status","error"),("message","Valor de abertura inválido!"+e.getMessage))
				case e:ParseException => JsObj(("status","error"),("message","Data inválida!"))
				case e:AlreadyOpenedCashier => JsObj(("status","error"),("message","Já existe um caixa aberto!"))
				//case e:Exception => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",false))
			}
		}

		// acho que não usa pra nada - em lugar nenhum
		// rigel - 30/06/2017
		case "cash" :: "operators" :: Nil JsonGet _ => {
			JsArray(
					DomainTable.findAll(
						By(DomainTable.domain_name,"operadora"),
						OrderBy (DomainTable.name, Ascending)).map(
						(u) => JsObj(("status","success"),
									 ("name",u.name.is),
									 ("id",u.cod.is)
									 )
							)
					)
		}

		case "cash" :: "brands" :: Nil JsonGet _ => {
			JsArray(
					Brand.findAllInCompany(OrderBy (Brand.name, Ascending)).map(
						(u) => JsObj(("status","success"),
									 ("name",u.short_name.is),
									 ("id",u.id.is)
									 )
							)
					)
		}

		case "cash" :: "suppliers" :: Nil JsonGet _ => {
			JsArray(
					Customer.findAllInCompany(OrderBy (Customer.name, Ascending), 
						By(Customer.is_suplier_?, true)).map(
						(u) => JsObj(("status","success"),
									 ("name",u.name.is),
									 ("id",u.id.is)
									 )
							)
					)
		}

		case "cash" :: "openCheckouts" :: Nil JsonGet _ => {
			JsArray(Cashier.findOpenCashiers.map(toJson))
		}

		case "cash" :: "allCheckouts" :: Nil JsonGet _ => {
			JsArray(Cashier.findAllCashiers.map(toJson))
		}		

		case "cash" :: "closedCheckouts" :: Nil JsonGet _ => {
			JsArray(Cashier.findClosedCashiers.map(toJson))
		}

		case "cash" :: "checkoutReopen" :: checkoutId :: isIdForCompany :: valueStart :: Nil JsonGet _ => {
			try{
				def valueStartValue = valueStart.toDouble
				val cashier = 	if(isIdForCompany.toBoolean) {
									Cashier.findByKey(checkoutId.toLong).get
								}else{
									Cashier.findOpenCashierByIdAndCompany(checkoutId.toInt)
								}
				cashier.reopen (valueStartValue)
				JsObj(("status","success"),
					  ("id",cashier.idForCompany.is),
					  ("dbId",cashier.id.is)
					 )
			}catch{
				case e:NumberFormatException  => JsObj(("status","error"),("message","Caixa inválido!"))
				case e:RuntimeException  => JsObj(("status","error"),("message","Caixa não existe! ou "+e.getMessage))
				case e:Exception  => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",false))
			}			
		}
		case "cash" :: "checkoutClose" :: checkoutId :: Nil JsonGet _ => {
			try{
				val cashier = Cashier.findOpenCashierByIdAndCompany(checkoutId.toInt)
				cashier.close
				JsObj(("status","success"),
					  ("id",cashier.idForCompany.is),
					  ("dbId",cashier.id.is),
					  ("endValue",cashier.endValue.is.toDouble)
					 )
			}catch{
				case e:NumberFormatException  => JsObj(("status","error"),("message","Caixa inválido!"))
				case e:AlreadyClosedCashier  => JsObj(("status","error"),("message",e.getMessage))
				//case e:Exception => S.error (e.getMessage)
				case e:RuntimeException  => {
					JsObj(("status","error"),("message","Caixa não existe! ou " + e.getMessage))
				}				
				case e:Exception  => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",false))
			}
		}
		case "cash" :: "checkoutValues" :: checkoutId :: Nil JsonGet _ => {
			try{			
				val cashier = Cashier.findOpenCashierByIdAndCompany(checkoutId.toInt)
				JsObj(("status","success"),
					  ("id",cashier.idForCompany.is),
					  ("startValue",cashier.startValue.is.toDouble),
					  ("paidValueInCheque",cashier.paidValueInCheque.toDouble),
					  ("paidValueInCard",cashier.paidValueInCard.toDouble),
					  ("paidValueInMoney",cashier.paidValueInMoney.toDouble),
					  ("outsValue",cashier.outputValue.toDouble),
					  ("outsCheque",cashier.outputCheque.toDouble),
					  ("insValue",cashier.inputValue.toDouble),
					  ("openerDate",cashier.openerDate.is.getTime),
					  ("cashierStatus",cashier.status.is.toString),
					  ("totalValueToConference",cashier.paidValueToFonference.toDouble)
					 )
			}catch{
				case e:NumberFormatException  => JsObj(("status","error"),("message","Caixa inválido!"))
				case e:RuntimeException  => JsObj(("status","error"),("message","Caixa não existe!"))
				case e:Exception  => JsObj(("status","error"),("message",e.getMessage))
				case _ => JsObj(("status","error"),("message",false))
			}
		}

		case "cash" :: "getUsers" :: "commission" :: Nil JsonGet _ =>{
			if (AuthUtil.user.isSimpleUserCommission) {
				JsArray(
					JsObj(
						("status","success"),
						("name",AuthUtil.user.short_name.is),("id",AuthUtil.user.id.is),
						("idForCompany",BusinessRulesUtil.zerosNoLimit(AuthUtil.user.idForCompany.is.toString,3))
					)		
				)
			} else {
				JsArray(User.findAllInCompanyOrdened.map( (u) => {
					JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
						,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
				}))
			}
		}

		case "cash" :: "getUsers" :: Nil JsonGet _ =>{
			JsArray(User.findAllInCompanyOrdened.map( (u) => {
				JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
					,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}

		case "cash" :: "getAuxiliars" :: Nil JsonGet _ =>{
			JsArray(Customer.findAllInCompany(By (Customer.is_auxiliar_?, true),
				OrderBy(Customer.short_name, Ascending)).map( (u) => {
				JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
					,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}

		case "cash" :: "getUsersCurrentUnitCashier" :: Nil JsonGet _ => {
				JsArray(User.findAllInCompany(
					By (User.showInCashier_?,true),
			        BySql(" (unit = ? or (id in (select uu.user_c from usercompanyunit uu where uu.unit = ? and uu.company = ?))) ",IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.unit.id, AuthUtil.company.id),
			        By(User.userStatus, User.STATUS_OK), 
			        OrderBy(User.short_name, Ascending)).map( (u) => {
					JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
						,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}

		case "cash" :: "getUsersCurrentUnitCommand" :: Nil JsonGet _ => 
			if (AuthUtil.user.isSimpleUserCommand) {
				JsArray(User.findAllInCompanyOrdened(By (User.showInCommand_?,true)).map( (u) => {
					JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
						,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
				}))
			} else {
				JsArray(User.findAllInCompany(
					By (User.showInCommand_?,true),
			        BySql(" (unit = ? or (id in (select uu.user_c from usercompanyunit uu where uu.unit = ? and uu.company = ?))) ",IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.unit.id, AuthUtil.company.id),
			        By(User.userStatus, User.STATUS_OK), 
			        OrderBy(User.short_name, Ascending)).map( (u) => {
					JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
						,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}

		case "cash" :: "getAuxiliarsCurrentUnitCommand" :: Nil JsonGet _ => 
			if (AuthUtil.user.isSimpleUserCommand) {
				JsArray(Customer.findAllInCompany(
					By (Customer.is_auxiliar_?, true),
					OrderBy(Customer.short_name, Ascending)).map( (u) => {
					JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
						,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
				}))
			} else {
				JsArray(Customer.findAllInCompany(
			        BySql(" (unit = ? or (id in (select uu.user_c from usercompanyunit uu where uu.unit = ? and uu.company = ?))) ",IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.unit.id, AuthUtil.company.id),
			        By(Customer.is_auxiliar_?, true), 
			        OrderBy(Customer.short_name, Ascending)).map( (u) => {
								JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
							,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
				}))
		}

		case "cash" :: "getUsersCurrentUnit" :: Nil JsonGet _ =>{
			JsArray(User.findAllInCompany(
		        BySql(" (unit = ? or (id in (select uu.user_c from usercompanyunit uu where uu.unit = ? and uu.company = ?))) ",IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.unit.id, AuthUtil.company.id),
		        By(User.showInCalendar_?, true), (By(User.userStatus, User.STATUS_OK)), 
		        OrderBy(User.short_name, Ascending)).map( (u) => {
				JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
					,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}
/*
		case "cash" :: "getAuxiliarsCurrentUnit" :: Nil JsonGet _ =>{
			JsArray(Customer.findAllInCompany(
        BySql(" (unit = ? or (id in (select uu.user_c from usercompanyunit uu where uu.unit = ? and uu.company = ?))) ",IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.unit.id, AuthUtil.company.id),
        By(User.showInCalendar_?, true), (By(User.userStatus, User.STATUS_OK)), 
        OrderBy(User.short_name, Ascending)).map( (u) => {
				JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
					,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}
*/
		case "cash" :: "getAllUsers" :: Nil JsonGet _ =>{ //So use em casos de coisas que todo mundo pode ver mesmo....
			JsArray(User.findAllInCompanyOrdenedInsecurity.map( (u) => {
				JsObj(("status","success"),("name",u.short_name.is),("id",u.id.is)
					,("idForCompany",BusinessRulesUtil.zerosNoLimit(u.idForCompany.is.toString,3)))
			}))
		}	
		case "cash" :: "getActivities" :: Nil JsonGet _ =>{
			JsArray(TreatmentService
			.activitiesMap.map((a) => {
				JsObj(
						("status","success"),
						("name",a.name.is),
						("price",a.salePrice.toDouble),
						("duration",a.duration.is),
						("bpmonthly",a.bpmonthly_?.is),
						//("discountsTotal",a.discountsTotal.toDouble),
						("id",a.id.is)
					)
				}))
		}
		case "cash" :: "getCRMActivities" :: Nil JsonGet _ =>{
			JsArray(Activity.findAllInCompany(
				//By(Activity.status, 1),
				By(Activity.crmService_?, true),
				OrderBy(Activity.name, Ascending)).map((a) => {
				JsObj(
						("name",a.name.is),
						("id",a.id.is)
					)
				}))
		}
		
		case "cash" :: "getActivityTypes" :: Nil JsonGet _ =>{
			JsArray(ProductType.findAllService.map((a) => {
				JsObj(
						("name",a.name.is),
						("id",a.id.is)
					)
				}))
		}
		case "cash" :: "getProductTypes" :: Nil JsonGet _ =>{
			JsArray(ProductType.findAllProduct.map((a) => {
				JsObj(
						("name",a.name.is),
						("id",a.id.is)
					)
				}))
		}		
		case "cash" :: "getPackages" :: Nil JsonGet _ =>{
			JsArray(Product.findAllPackages.map((a) => {
				JsObj(
						("name",a.name.is),
						("id",a.id.is)
					)
				}))
		}
		case "cash" :: "getActivities" :: userId :: Nil JsonGet _ =>{
			val userParm = if (userId != "" && userId != "0") {
				userId.toLong
			} else {
				1l // migracao para evitar none.get exception
			}
			if (userParm != 1l) {
				val user = User.findByKey(userParm).get
				JsArray(TreatmentService
				.activitiesMapByUser(user, false).map((a) => {
					JsObj(
							("status","success"),
							("name",a.name.is),
							("price",TreatmentService.price(a,user).toDouble),
							("duration",TreatmentService.duration(a,user)),
							("bpmonthly",a.bpmonthly_?.is),
							("id",a.id.is)
						)
					}))
		    } else {
		    	// busca todas as atividades da empresa corrente
				JsArray(AuthUtil.company.activities (false).map((a) => {
					JsObj(
							("status","success"),
							("name",a.name.is),
							("price",a.salePrice.toDouble),
							("duration",a.duration.is),
							("bpmonthly",a.bpmonthly_?.is),
							("id",a.id.is)
						)
					}))
		    }

		}

		case "cash" :: "getAnimals" :: customerId :: Nil JsonGet _ =>{
			val customerParm = if (customerId != "" && customerId != "0") {
				customerId.toLong
			} else {
				1l // migracao para evitar none.get exception
			}
			val customer = Customer.findByKey(customerParm).get
			JsArray(TreatmentService
			.animalsMapByCustomer(customer)
			.map((a) => {
				JsObj(
						("status","success"),
						("name",a.name.is),
						("obs",a.obs.is),
						("bp_manager",a.bp_manager.is), // é o dono - ao exibir 
						("death",Project.dateToStrOrEmpty(a.deathDate)),
						("id",a.id.is)
					)
				}))
		}
		
		case "cash" :: "removePayment" :: command :: customer :: date :: Nil JsonGet _ =>{
			try{
				def dateValue = date match {
					case (s:String) if(s != "" && s != "0") => Project.strOnlyDateToDate(date)
					case _ => new Date()
				}
				PaymentService.removePaymentByCommand(command, customer.toLong, dateValue)
				JInt(1)
			}catch{
				case e:CashierIsClosed =>{
					JString("Não é possível excluir pagamento de caixa fechado!")
				}
				case e:PaymentNotFound => {
					JString("Pagamento não encontrado, verifique a data!")
				}		
				case e:HaveDeliveriesUsed =>{
					JString("Essa comanda não pode ser excluída porque é de um pacote que já foi utilizado! Exclua a(s) comanda(s) (%s)".format(e.deliveriesUsed.map(_.command).reduceLeft(_+", "+_)))
				}
				case e:Exception => {
					e.printStackTrace
					JString(e.getMessage)
				}
			}
		}
		case "cash" :: "getCommand" :: customer :: date :: Nil JsonGet _ =>{
			def dateValue = date match {
				case (s:String) if(s != "" && s != "0") => Project.strToDate(date+" "+AuthUtil.company.calendarStart.is+":00:00")
				case _ => new Date()
			}
			def customerObj = Customer.findByKey(customer.toLong).get
			try{
				val cmdnr = TreatmentService.loadTreatmentByCustomerNotPaid(customerObj,dateValue)(0).command.is;
				if (cmdnr == "") {
					// println ("vaiiii ========================= comanda vazia")
					// 14/02/2017
					// ainda nao sei o que causa, mas sim contorna antes só tinha o else
					JsObj(("command",Treatment.nextCommandNumber(dateValue,AuthUtil.company, AuthUtil.unit)),("isNew", true))
				} else {
					JsObj(("command",TreatmentService.loadTreatmentByCustomerNotPaid(customerObj,dateValue)(0).command.is),("isNew", false))
				}
			}catch{
				case _ => JsObj(("command",Treatment.nextCommandNumber(dateValue,AuthUtil.company, AuthUtil.unit)),("isNew", true))
			}

		}
		case "cash" :: "getTreatment" :: command :: customer :: dateIni :: date :: Nil JsonGet _ =>{
			 
			def dateIniValue = date match {
				case (s:String) if(s != "" && s != "0") => Project.strToDate(dateIni+" "+AuthUtil.company.calendarStart.is+":00:00")
				case _ => new Date()
			}
			def dateValue = date match {
				case (s:String) if(s != "" && s != "0") => Project.strToDate(date+" "+AuthUtil.company.calendarStart.is+":00:00")
				case _ => new Date()
			}
		TreatmentService.loadTreatmentByCommandOrCustomer(
			command, customer.toLong, dateIniValue,
			dateValue, AuthUtil.unit.id.is) match {
				case tl:List[Treatment] if(tl.size >0) => {
					JsObj(("status","success"),("data",JsArray(
								tl.map((t) => {
									JsObj(
										("treatmentStatus",t.status.is.toString),
										("customerName",t.customerName),
										("customerId",t.customer.is),
										("userId",t.user.is),
										("unit",t.unit.is),
										("id",t.id.is),
										("userShortName",t.userShortName),
										("unitShortName",t.unitShortName),
										("removed",false),
										("ignored",false),
										("dateTreatment",t.start.is.getTime),
										("activitys",JsArray(t.details.toList.map((d) => JsObj( 
											("auxiliarShortName", d.auxiliarShortName), 
											("auxiliar", d.auxiliar.is),
											("offsale", d.offsale.is), 
											("offsaleShortName", d.offsaleShortName), 
											("tooth", d.tooth), // .is
											("animalShortName", d.animalShortName), 
											("animal", d.animal), // .is
											("parentBom", d.parentBom.is),
											("for_delivery",d.for_delivery_?.is),
											("activity",d.nameActivity),
											("price",d.unit_price),
											("id",d.id.is),
											("activityType",d.activityType),
											("activityId",d.activity_id), 
											("amount",d.amount.is.toFloat),
											("removed",false)))))
										)
									})
								)
							)
						)
				}
				case e:Exception => JsObj(("status","error"),("message",e.getMessage()))
				case _ => JsObj(("status","error"),("message","Comanda não existe.\n\nVerifique comanda, caixa, data e unidade selecionada, caso vocẽ tenha mais de uma."))
			}
		}
		case "cash" :: "saveTreatments" :: Nil Post _ =>{
			for {
				data <- S.param("data") ?~ "data parameter missing" ~> 400
				command <- S.param("command") ?~ "command parameter missing" ~> 400
				date <- S.param("date") ?~ "command parameter missing" ~> 400
			} yield {
				val json = parse(data)
				val treatments = json.extract[List[TreatmentDTO]]
				try{
					PaymentService.processTreatments(treatments,command, Project.strOnlyDateToDate(date),Treatment.Open,"")
					JsObj(("status","success"))
				}catch{
					case e:Exception => {
						e.printStackTrace
						JsObj(("status","error"),("message",e.getMessage))
					}
				}
			}
		}
		case "cash" :: "processPayment" :: Nil Post _ =>{
			for {
				data <- S.param("data") ?~ "data parameter missing" ~> 400
			} yield {
				val json = parse(data)
				val a = json.extract[PaymentRequst]
				try{
					PaymentService.processPaymentRequst(a)
					JsObj(("status","success"),("message","Pagamento efetuado com sucesso!"))
				}catch{
					case (e:PaymentTypeNotAvailableToReturn) => JsObj(("status","error"),("message","Não é possível processar este valor de troco, utilize forma de pagamento Dinheiro!"))
					case (e:InsufficientInventoryException) => JsObj(("status","error"),("message","Não há quantidade em estoque do produto %s na unidade %s para essa movimentação!".format(e.productName, e.unitName)))
					case (e:PaymentIsNotEnough) => JsObj(("status","error"),("message","Pagamento não é suficiente para o valor total do atendimento!"))
					case (e:PaymentCustomerIsNotAUser) => JsObj(
							("status","error"),
							("message","Não e possível usar a forma de pagamento %s para o cliente %s ela é exclusiva para profissionais!".format(e.paymentTypeName, e.customerName))
						)
					case (e:CommandIsNotValid) => JsObj(("status","error"),("message","Número da comanda não é válido para pagamento, por favor altere o número da comanda!"))
					case (e:InvalidPaymentValue) => JsObj(("status","error"),("message","Valor do pagamento não pode ser 0.00, utilize cortesia caso seja necessário."))
					case (e:NotAllowCommandRepeat) => JsObj(("status","error"),("message","Já existe uma comanda paga com esse número!"))
					case (e:CustomerNotHasValueInCredit) => JsObj(("status","error"),("message","Cliente possui apenas %s em crédito, utilize outra forma de pagamento, para o restante do valor".format(e.value.toString())))
					case (e:ProductNotAllowSaleByUser) => JsObj(("status","error"),("message","O produto "+e.getMessage+" não aceita venda com profissional! Remova e tente novamente."))
					case (e:PaymentDeliveryNotEnough) => JsObj(("status","error"),("message",e.getMessage))
					case (e:SessionValueWrong) => JsObj(("status","error"),("message",e.getMessage))
					case (e:PaymentMonthlyNotEnough) => JsObj(("status","error"),("message",e.getMessage))
					case (e:Exception) => {
						error(e)
						JsObj(("status","error"),("message","Ocorreu um erro desconhecido no processamento do pagamento, verifique os dados! "+e.getMessage))

					}
					case _ => JsObj(("status","error"),("message","Ocorreu um erro desconhecido no processamento do pagamento, verifique os dados!"))
				}
			}
		}
	}
	def toJson(cashier:Cashier) = {
			JsObj(("id",cashier.id.is),
			   ("idForCompany",cashier.idForCompany.is),
			  ("startValue",cashier.startValue.is.toDouble),
			  ("openerDate",cashier.openerDate.is.getTime),
			  ("cashierStatus",cashier.status.is.toString),
			  ("unit",cashier.unitName),
			  ("obs",cashier.obs.is),
			  ("unit_id",cashier.unit.is),
			  ("userName",cashier.userName)
			)
	}
}