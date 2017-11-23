package code
package service

import net.liftweb._
import mapper._ 
import code.util._
import code.comet._
import model._
//import net.liftweb.util._
import http.js._
import JE._
import net.liftweb.common._
import InventoryMovement._
import java.util.Date
import service.events._

object  PaymentService extends  net.liftweb.common.Logger  {
	def saveTreatment(treatment:Treatment, validate:Boolean) = {
		if(validate){
			treatment.save
		}else{
			treatment.validateCustomer
			treatment.saveWithoutValidate
		}
	}
	def processTreatments(treatments:List[TreatmentDTO],command:String,dateTreatment:Date,
		status:Int,
		status2:String,
		processInventory:Boolean=false, 
		payment:Box[Payment]=Empty, 
		validate:Boolean=false):List[Treatment] = {
		val validTreatments = treatRemovedTreatments(treatments)
		validTreatments.map((t:TreatmentDTO) => {
			val treatment:Treatment = factoryTreatment(t,command,dateTreatment)
			if (treatment.hasDetail || status != 4) {
				// rigel 08/11/2017
				// fiz o if pq antes setava paid em treatment vazio
				saveTreatment(treatment, validate)
			}
			
			val validActivities = treatRemovedActivity(t.activitys)
			validActivities foreach((a:ActivityDTO) => {
				val detail = factoryDetail(a)
				processActivityType(treatment.customer, detail, a.activityType, command, a.activityId.toLong, processInventory, (a.price*a.amount), a.amount, payment)
				detail
				.price(a.price*a.amount)
				.amount(BigDecimal(a.amount))
				.for_delivery_?(a.for_delivery)
				.treatment(treatment)
				.parentBom(a.parentBom)
				.offsale(a.offsale)
				.auxiliar(a.auxiliar)
				.save

		        if (AuthUtil.company.appType.isEbellepet) {
					detail.getTdEpet.animal(a.animal).save;
				}
		        if (AuthUtil.company.appType.isEsmile) {
					detail.getTdEdoctus.tooth(a.tooth).save;
				}
			})
			if (treatment.hasDetail || status != 4) {
				// rigel 08/11/2017
				// fiz o if pq antes setava paid em treatment vazio
				treatment.status(status)
				if (status2 =="4") {
					treatment.status2(status)
				}
				saveTreatment(treatment, validate)
			}
			treatment
		})
	}
	def processPaymentRequst(request:PaymentRequst):Payment = {
		val payment = Payment.create
		DB.use(DefaultConnectionIdentifier) {
			 conn =>
			 val jConn: java.sql.Connection = conn.connection
			 try{
					payment.company(AuthUtil.company)
					payment.command(request.command)
					payment.datePayment(request.dataTreatmentsAsDate)
					payment.cashier(request.cashier.toLong)
					payment.save
					/*					if (request.status2 == "4" /* pago e pago no caixa nao no atendido*/) {

					} else {

					} */
					val processedTreatments = processTreatments(request.treatments, request.command, 
						request.dataTreatmentsAsDate, 
						Treatment.Paid, request.status2,
						true, Full(payment), false)
					payment.customer(processedTreatments.head.customer)
					val ac = Customer.findByKey (processedTreatments.head.customer).get
					ac.lastSaleDate(request.dataTreatmentsAsDate).
					is_customer_?(true).insecureSave;
					processedTreatments.foreach((t) => payment.treatments += t)
					request.payments filter(!_.removed) foreach((p:PaymentDTO) => {
						createPaymentDetail(p,payment)
					});
				payment.prepareValueOfDetails.validateRules.save
				jConn.commit()
			}catch{
				case e:Exception => { conn.rollback
					 throw e
				}
			}
		}//DB.use(DefaultConnectionIdentifier)
		EventAggregator.onPaymentSuccess(PaymentSuccessMessage(payment))
		payment
	}



	private def treatRemovedActivity(activitys:List[ActivityDTO]):List[ActivityDTO] = {
		activitys.filter((a)=>{
			if(a.removed){
				TreatmentDetail.findByKey(a.id) match {
					case Full(td) => td.delete_!
					case _ => 
				}
			}
			!a.removed
		})
	}
	private def treatRemovedTreatments(treatments:List[TreatmentDTO]):List[TreatmentDTO] ={
		treatments.filter((t)=>{
			if(t.removed){
				if(t.id > 0){
					Treatment.findByKey(t.id).get.delete_!
				}				
			}
			!t.removed && !t.ignored;
		})
	}
	private def factoryDetail(a:ActivityDTO) = {
		if(a.id==0){
			TreatmentDetail.createInCompany
		}else{
			TreatmentDetail.findByKey(a.id).get
		}
	}
	private def factoryTreatment(t:TreatmentDTO,command:String,dateTreatment:Date, obs:String = ""):Treatment = {
		t.id match {
			case id:Int if(id > 0) =>{
					val treatment = Treatment.findByKey(id.toLong).get
					if(command != "" && command != "0")
						treatment.command(command)
					treatment
				}
			case _ => Treatment.create
					.customer(t.customerId.toLong)
					.user(t.userId.toLong)
					.command(command)
					.start(dateTreatment)
					.end(dateTreatment)
					.hasDetail(true)
					.obs(obs)
					.showInCalendar(false)
					.company(AuthUtil.company)					
		}		
	}
	def removeInventory(bp:Long, purchaseprice:Double, treatmentdetail:Long, price:Double, amount:Float, product:Product, obs:String, invoice:String, unit:CompanyUnit, inventorycause:InventoryCause, efetivedate:Date) {
		//AuthUtil.company.inventoryCauseSale.obj.get
		// remove(amount) item product company(AuthUtil.company) obs(obs) invoice(invoice) cause(AuthUtil.company.inventoryCauseSale.obj.get) efetiveDate(new Date()) from unit
		remove(amount) item product company(AuthUtil.company) purchasePrice (purchaseprice) treatment_detail (treatmentdetail) totalSalePrice (price) obs(obs) business_pattern (bp) invoice(invoice) cause(inventorycause) efetiveDate(efetivedate) from unit
	}
	def addInventory(bp:Long, purchaseprice:Double, treatmentdetail:Long, price:Double, amount:Float, product:Product, obs:String, invoice:String, unit:CompanyUnit, inventorycause:InventoryCause, efetivedate:Date) {
		add(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, amount) item product company(AuthUtil.company) purchasePrice (purchaseprice) treatment_detail (treatmentdetail) totalSalePrice (price) obs(obs) business_pattern (bp) invoice(invoice) cause(inventorycause) efetiveDate(efetivedate) from unit
	}	
	private def processActivityType(customer_id:Long,td:TreatmentDetail,activityType:String, command:String, product_id:Long,processInventory:Boolean=false, price:Double=0.0, amount:Float=1, payment:Box[Payment]=Empty){
		val paymentObj = payment match {
		      case Full(c) => c
		      case _ => Payment.create
		}
  		val unit = payment match {
			case Full(p) => p.cashier.obj.get.unit.obj.get
			case _ => AuthUtil.unit
		}		
		activityType match {
			case "activity" => {
				val activity = Activity.findByKey(product_id).get
				val discounts = activity.discounts
				discounts.foreach((d)=>{
					val product = d.product_bom.obj.get
					if(processInventory && product.isInventoryControl){
						removeInventory(customer_id, 0.0, td.id, 0.0, d.qtd.is.toFloat, product,  "Venda comanda :"+command, command, unit,AuthUtil.company.inventoryCauseUse.obj.get, paymentObj.datePayment)
					}
				})
				td.activity(product_id)
			}
			case "product" => {
				// info("product.findByKey(id.toLong).get")
				val prod = Product.findByKey(product_id).get
				
				td.product(prod)
				if(processInventory && prod.isInventoryControl){
					removeInventory(customer_id, prod.purchasePrice.is.toDouble, td.id, price, amount, prod,  "Venda comanda :"+command, command, unit,AuthUtil.company.inventoryCauseSale.obj.get, paymentObj.datePayment)
				}
			}
			case _ => {
			}
		}		
	}
	
	def createPaymentDetail(p:PaymentDTO,payment:Payment) = {
		val paymentType = PaymentType.findByKey(p.typePayment).get
		val paymentDetail = PaymentDetail.createInCompany
		paymentDetail.typePayment(p.typePayment)
					 .value(p.value)
					 .customer(payment.customer)
					 .payment(payment)
					 .dueDate(p.dateDetailAsDate)
					 .save
		payment.details += paymentDetail
		if(p.cheque_?){
			createCheque(p,paymentDetail, paymentDetail.dueDate, payment.datePayment)
		}
		if(paymentType.customerRegisterDebit_?.is){
			val customer = payment.customer.obj.get
			customer.registerDebit(p.value*(-1), payment, paymentDetail, "Adicionando valor a conta cliente")
		}
		if(paymentType.customerUseCredit_?.is){
			val customer = payment.customer.obj.get
			if(customer.valueInAccount.is < p.value){
				throw CustomerNotHasValueInCredit(customer.valueInAccount.is)
			}
			customer.registerDebit(p.value*(-1), payment, paymentDetail, "Usando credito cliente")
		}		
		if(paymentType.deliveryContol_?.is){
			validateDeliveryRules(paymentDetail)
		}
		// rigel 07/08/2017
		// aqui na verdade tem "converter" reais em pontos
		// talvez não deixar misturar ou fazer um proporcional
		// mas neste caso todos os itens deveriam term preço em 
		// pontos
		// total total $           = xpontos total
		//       val $ pt fidelity = y pontos
		//
		// mostrar os pontos que o cliente tem e o valor da compra 
		// em pontos alertar se tem itens sem preço em pontos
		// parametrizar se comprando com fidelidade gera pontos 
		// fidelidade
		// talvez salvar o valor em pontos no paymentdetail
		//
		if(paymentType.fidelity_?.is){
			val customer = payment.customer.obj.get
			customer.unRegisterPoints(p.value, payment, paymentDetail, "Adicionando valor a pontos fidelidade")
		}
	}

	def removePaymentDetails(detail:PaymentDetail) = {
		val paymentType = detail.typePaymentObj.get
		if(paymentType.cheque_?.is || paymentType.needCardInfo_?.is){
			detail.cheque.delete_!
		}
		if(paymentType.customerRegisterDebit_?.is){
			val customer = detail.payment.obj.get.customer.obj.get
			customer.registerDebit(detail.value.is.toDouble, 
			detail.payment.obj.get,detail, 
			"Removendo atendimento de conta cliente")//incremente customer account
		}
		if(paymentType.deliveryContol_?.is){
			validateDeliveryRulesToRemove(detail)
		}
		if(paymentType.fidelity_?.is){
			val customer = detail.payment.obj.get.customer.obj.get
			customer.registerPointsPt(detail.value.is.toDouble, 
			detail.payment.obj.get,detail, 
			"Removendo atendimento de conta cliente")//incremente customer accountRegisterPoints(p.value*(-1), payment, paymentDetail, "Adicionando valor a pontos fidelidade")
		}
	}
	private def validateDeliveryRulesToRemove(paymentDetail:PaymentDetail) = {
		
		val payment = paymentDetail.payment.obj.get
		val treatments = payment.treatments
		if(!treatments.isEmpty){
			val customer = treatments(0).customer.obj.get
			treatments.foreach((t) => {
				t.details.foreach((td)=>{
					td.activity.obj match {
						case Full(a) => customer.unRegisterDelivery(a.id.is,td.id.is,paymentDetail.id.is, td.amount.is.toInt)
						case _ => customer.unRegisterDelivery(td.product.is,td.id.is,paymentDetail.id.is, td.amount.is.toInt)
					}
				})
			});
		}
	}
	private def validateDeliveryRules(paymentDetail:PaymentDetail) = {
		
		val payment = paymentDetail.payment.obj.get
		val treatments = payment.treatments
		val customer = treatments(0).customer.obj.get
		treatments.foreach((t) => {
			t.details.foreach((td)=>{
				td.activity.obj match {
					case Full(a) => customer.registerDelivery(a.id.is,td.id.is,paymentDetail.id.is, td.amount.is.toInt, td.unit_price.toFloat, payment.datePayment)
					case _ => customer.registerDelivery(td.product.is,td.id.is,paymentDetail.id.is, td.amount.is.toInt, td.unit_price.toFloat, payment.datePayment)
				}
			})
		});
	}

	private def createCheque(p:PaymentDTO,paymentDetail:PaymentDetail, goodTo:Date, receivedDate:Date) = {
		val tp_cheque = if (p.chequeInfo.bank.toInt == 0) {
				3
			} else {
				AccountPayable.IN
			}
		Cheque.create
		.customer(paymentDetail.customer)
		.bank(p.chequeInfo.bank.toInt)
		.account(p.chequeInfo.account)
		.agency(p.chequeInfo.agency)
		.number(p.chequeInfo.cheque_number)
		.value(paymentDetail.value.is)
		.paymentDate(p.chequeInfo.datePayment)
		.receivedDate(receivedDate)
		.dueDate(goodTo)
		.received(false)
		.company(AuthUtil.company)
		.paymentDetail(paymentDetail)
		.movementType(tp_cheque)
		.save
	}

	def removePaymentByCommand(command:String,customer:Long,date:Date) ={
		DB.use(DefaultConnectionIdentifier) {
			 conn =>
			 try{		
					val payments = if (customer != 0) {
						Payment.findAllInCompany(
							By(Payment.customer,customer),
							By(Payment.command,command),
							BySql("date(datepayment) = date(?)",IHaveValidatedThisSQL("payment_date","01-01-2012 00:00:00"),date),
							OrderBy(Payment.createdAt, Descending)
							)
					} else {
						Payment.findAllInCompany(
							By(Payment.command,command),
							BySql("date(datepayment) = date(?)",IHaveValidatedThisSQL("payment_date","01-01-2012 00:00:00"),date),
							OrderBy(Payment.createdAt, Descending)
							)
					}
					if(payments.isEmpty){
						throw new PaymentNotFound
					}
					val payment = payments(0)
					restoreInventoryMovements(payment);
					payment.details.foreach( (d) => { removePaymentDetails(d) } )
					payment.delete_!;
					EventAggregator.onPaymentRemoved( new PaymentRemovedMessage(payment, payment.treatments.map(_.id.is).toList))
				}catch{
					case e:Exception => { conn.rollback
						 throw e
					}
				}
		}
	}

	def restoreInventoryMovements(p:Payment){
		lazy val unit = p.cashier.obj.get.unit.obj.get
		p.treatments.foreach((t:Treatment) => {
			t.details.foreach((td:TreatmentDetail) => {
				td.product.obj match {
					case Full(prod) =>{ 
						if(prod.isInventoryControl)
							addInventory(t.customer, prod.purchasePrice.is.toDouble, td.id, td.price.is.toDouble, td.amount.is.toFloat, prod, "Exclusão da comanda :"+t.command.is, t.command.is, unit, AuthUtil.company.inventoryCauseSale.obj.get, p.datePayment)
					}
					case _ => {
						val discounts = td.activity.obj.get.discounts
						discounts.foreach((d)=>{
							val product = d.product_bom.obj.get
							if(product.isInventoryControl){
								addInventory(t.customer, 0.0, td.id, 0.0, d.qtd.is.toFloat, product,  "Exclusão da comanda :"+t.command, t.command.is, unit, AuthUtil.company.inventoryCauseUse.obj.get, p.datePayment)
							}
						})						
					}
				}
			})
		})		
	}	
	def paymentsBetween(start:Date, end:Date,commans:List[String]) = {
		commans match {
			case a:List[Int] if(a.size > 0) => Payment.findAll(
						  OrderBy(Payment.command,Ascending),
        				  OrderBy(Payment.datePayment,Ascending),
                          By(Payment.company,AuthUtil.company),
                          ByList(Payment.command,commans),
                          BySql("date(datepayment) between date(?) and date(?)",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
     					)
			case Nil => Payment.findAll(
        				  OrderBy(Payment.datePayment,Ascending),
                          By(Payment.company,AuthUtil.company),
                          BySql("date(datepayment) between date(?) and date(?)",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
     					)
		}
    }

	def commisionBetweenByUser(start:Date, end:Date,user:User) = {
		Commision.findAllInCompany(
			  OrderBy(Commision.payment_date,Ascending),
			  By(Commision.user,user),
              BySql("date(payment_date) between date(?) and date(?)",IHaveValidatedThisSQL("payment_date","01-01-2012 00:00:00"),start,end),//fica preso ao postgress
              // fazer o in com produto
              BySql("1=1",IHaveValidatedThisSQL("",""))
		)
	}

	def paymentsBetweenByUser(start:Date, end:Date,user:User) = {
        Payment.findAll(
        				  OrderBy(Payment.datePayment,Ascending),
                          By(Payment.company,AuthUtil.company),
                          BySql("date(datepayment) between date(?) and date(?)",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
     					).map(p => {
							PaymentForReport(p.treatments.toList.filter(_.user == user.id.is),
											 p.details.toList,
											 p.command.is.toString,
											 p.customer.obj.get,
											 p.datePayment.is)
						})
    }    
	def paymentsBetween(start:Date, end:Date, cashier:Int, commans:List[String]) = {
		commans match {
			case a:List[Int] if(a.size > 0) => Payment.findAll(
        				  OrderBy(Payment.datePayment,Ascending),
                          By(Payment.company,AuthUtil.company),
                          By(Payment.cashier,cashier),
                          ByList(Payment.command,commans),
                          BySql("date(datepayment) between date(?) and date(?)",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
			)
			case Nil => Payment.findAll(
        				  OrderBy(Payment.datePayment,Ascending),
                          By(Payment.company,AuthUtil.company),
                          By(Payment.cashier,cashier),
                          BySql("date(datepayment) between date(?) and date(?)",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
     				)
		}
		
    }    
}
case class ActivityDTO(activityId:Int, activityType:String, id:Int, price:Double, 
	removed:Boolean, amount:Float, for_delivery:Boolean, parentBom:Int, 
	auxiliar:Int=0, animal:Int=0, tooth:String="", offsale:Int=0);

case class PaymentDTO(typePayment:Int,value:Double,removed:Boolean,chequeInfo:ChequeRequest, dateDetailStr:String){
	def this(typePayment:Int,value:Double,removed:Boolean, dateDetailStr:String) = this( typePayment, value, removed, null, dateDetailStr);
	def cheque_? = chequeInfo != null
	def dateDetailAsDate = Project.strOnlyDateToDate(this.dateDetailStr)
}
case class TreatmentDTO(customerId:Int,userId:Int,treatmentStatus:String,activitys:List[ActivityDTO],removed:Boolean,id:Int, ignored:Boolean=false);
case class PaymentRequst(treatments:List[TreatmentDTO],payments:List[PaymentDTO],command:String,dataTreatments:String,cashier:String, status2:String){
	def dataTreatmentsAsDate  ={
		dataTreatments match {
			case (s:String) if(s.length >= 9) => Project.strOnlyDateToDate(s)
			case _ => new Date()
		}
		
	}
};
case class ChequeRequest(account:String,agency:String,bank:Long,cheque_number:String,date_for_payment:String){
	def datePayment = Project.strOnlyDateToDate(date_for_payment)
}

case class PaymentForReport(treatments:List[Treatment],payments:List[PaymentDetail],command:String, customer:Customer,date:Date){
	def totalForPayToUser = treatments.map((t) => t.details.toList).foldLeft(List[TreatmentDetail]())(_:::_).map(_.valueToUser).foldLeft(BigDecimal(0.0))(_.toFloat+_.toFloat)
}
