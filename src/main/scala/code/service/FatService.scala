package code
package service

import net.liftweb._
import mapper._ 
import code.util._
import code.actors._
import code.comet._
import model._
import net.liftweb.util._
import http.js._
import JE._
import net.liftweb.common._
//import InventoryMovement._
import java.util.Date
import java.util.Calendar

import scalendar._
object FatService extends net.liftweb.common.Logger {
	
	def fat(cashier:Cashier)={
		val paymentsList = Payment.findAllInCompany(By(Payment.cashier,cashier.id.is)).map((p) => { p.details.toList }).filter( !_.isEmpty)
		if(!paymentsList.isEmpty){
			val payments = paymentsList.reduceLeft(_:::_)
			val paymentsByType = payments.groupBy((pt)=>{
				pt.typePayment.is
			})
			paymentsByType.keys.map((paymentTypeId)=>{
				val paymentType = PaymentType.findByKey(paymentTypeId).get
				// println("Fating "+paymentType.name.is)
				val total = paymentsByType(paymentTypeId).map(_.value.is).reduceLeft(_+_)
				//(paymentType,total)
				if (paymentType.receiveAtSight_?.is || paymentType.receiveAtSight_?.is) {
					if (paymentType.defaltAccount.obj.isEmpty) {
				      throw new RuntimeException("Forma de pagamento " + 
				      	paymentType.name.is + 
				      	" precisa ter uma conta associada")
					} else {
						if (paymentType.defaltCategory.obj.isEmpty) {
						   throw new RuntimeException("Forma de pagamento " + 
						   	paymentType.name.is + 
						   	" tem conta, mas não tem categoria associada.\n\nQuando a forma de pagamento é faturada, é necessário que ela esteja parametrizada corretamente com uma conta e categoria para geração do financeiro.")
						}
					}	
				}
				factoryFatStrategy(paymentType).process(cashier, paymentType,
					total.toDouble, paymentsByType(paymentTypeId))
			})			
		}
		// rigel 03/08/2017
		aggregatePtDt (cashier);
	}

	def aggregatePtDt (cashier:Cashier) = {
		// rigel 03/08/2017
		// faz agregação dos lancameentos gerados pelo fechamento do caixa
		// por forma de pagamento e data (duedate)
		val aclist = AccountPayable.findAllInCompany(
				By(AccountPayable.cashier, cashier.id.is),
				By(AccountPayable.auto_?,true),
				OrderBy (AccountPayable.paymentType, Ascending),
				OrderBy (AccountPayable.dueDate, Ascending),
				OrderBy (AccountPayable.id, Ascending) // para agregar sempre na 
				//receita e conciliar o typemovement do ofx
				);
		if (aclist.length > 0) {
			var dtAnt = new Date();
			var pt = 0l;
			var aggregId = 0l;
			var count = 0;
			var sum = 0.0;
			var iteration = 0;
			aclist.foreach((ac)=>{
				iteration += 1;
				if (ac.paymentType != pt || 
					Project.dateToStr(dtAnt) != Project.dateToStr(ac.dueDate)) {
					if (count == 1 && aggregId != 0l) {
						// neste caso aggregou um só - então limpa
						// o aggregateid
						AccountPayable.findByKey (aggregId).get.
						aggregateId(0).aggregateValue(0.0).save
					} else if (aggregId != 0l) {
						val paymentType = PaymentType.findByKey(pt).get
						// neste caso aggregou mais de um
						// salva o valoragregado no primeiro
						AccountPayable.findByKey (aggregId).get.
						aggregateValue(sum).
						aggregateLabel ("Cx %s fp (%s) === Agregado  ".
							format(cashier.idForCompany.is.toString, 
							paymentType.name.is)).
						save
					}
					dtAnt = ac.dueDate;
					pt = ac.paymentType
					aggregId = ac.id
					count = 0;
					sum = 0;
				}
				ac.aggregateId (aggregId)
				ac.save
				count += 1;
				if (ac.typeMovement == AccountPayable.OUT) {
					sum -= ac.value
				} else {
					sum += ac.value
				}
				// se for a ultima iteracao
				// salva o valor agregado
				if (iteration == aclist.length && aggregId != 0l) {
					if (count == 1 && aggregId != 0l) {
						// neste caso aggregou um só - então limpa
						// o aggregateid
						AccountPayable.findByKey (aggregId).get.
						aggregateId(0).aggregateValue(0.0).save
					} else {
						val paymentType = PaymentType.findByKey(pt).get
						// neste caso aggregou mais de um
						// salva o valoragregado no primeiro
						AccountPayable.findByKey (aggregId).get.
						aggregateValue(sum).
						aggregateLabel ("Cx %s fp (%s) === Agregado  ".
							format(cashier.idForCompany.is.toString, 
							paymentType.name.is)).
						save
					}
                }
			});	
		}
	}
	def factoryFatStrategy(paymentType:PaymentType)={
		// println ("vaiiii ===================== " + paymentType.name)
		if(paymentType.receiveAtSight_?.is)
			ReceiveAtSight
		else if(paymentType.addUserAccountToDiscount_?.is)
			ReceiveAddValueToUser
		else if(paymentType.nextMonth_?.is)
			ReceiveNextMonth
		else if(!paymentType.receive_?.is)
			NotReceive
		else if(paymentType.cheque_?.is && paymentType.receive_?.is)
			ReceiveCheque
		else if(paymentType.acceptInstallment_?.is && paymentType.receive_?.is){
			ReceiveParceled
		}else{
			//info("ReceiveFixDays : "+paymentType.name.is)
			ReceiveFixDays
			
		}
	}
	def canDesFat(cashier:Cashier)={
		// se tiver algun conciliado ou consolidado não pode 
		// reabrir caixa
		(AccountPayable.count(
				By(AccountPayable.cashier, cashier.id.is),
				By(AccountPayable.auto_?,true),
				NotBy(AccountPayable.conciliate,0)
			) == 0)
	}
	def desFat(cashier:Cashier)={
		AccountPayable.findAllInCompany(
				By(AccountPayable.cashier, cashier.id.is),
				By(AccountPayable.auto_?,true)
			).foreach(_.delete_!)
	}
}
//Fat
trait FatChain{
	private def discountMovementOrEmpty(paymentDetail:PaymentDetail, discountValue:Double, cashier:Cashier,
		paymentType:PaymentType,value:Double, categoryOnProduct:Boolean):Box[AccountPayable] = {
		if(discountValue != 0){
			if (categoryOnProduct) {
				val discountCat = paymentDetail.discountCategoryByType (paymentType.defaltDicountCategory.obj.get)
				val discountMovement = createAccount(cashier,paymentType,discountValue, discountCat,
					"Cx %s fp (%s)-desconto", AccountPayable.OUT)
				Full(discountMovement)
			} else {
				paymentType.defaltDicountCategory.obj match {
					case Full(accDiscount) => {
						val discountMovement = createAccount(cashier,paymentType,discountValue, accDiscount,
							"Cx %s fp (%s)-desconto", AccountPayable.OUT)
						Full(discountMovement)
					}
					case _ =>{
						Empty
					}
				}
			}
		}else{
			Empty
		}
	}
	def buildDefaltAccount(cashier:Cashier,paymentDetail:PaymentDetail,paymentType:PaymentType,value:Double,
		obs:String = "Cx %s fp (%s)-fechamento") = {
			val realValue = if(paymentType.defaltDicountCategory.obj.isEmpty){
							value * ((100-paymentType.percentDiscountToReceive.is)/100)
						}else{
							value
						}
		val discountValue = (value -(value * ((100-paymentType.percentDiscountToReceive.is)/100)))
		if (AuthUtil.company.categoryOnProduct_?) {
			val cat = paymentDetail.categoryByType (paymentType.defaltCategory.obj.get)
			//val discountCat = paymentDetail.discountCategoryByType
			Full(createAccount(cashier,paymentType, realValue, cat,	obs)) :: 
			(discountMovementOrEmpty(paymentDetail, discountValue,cashier,paymentType,value, AuthUtil.company.categoryOnProduct_?)) :: Nil
		} else {
			Full(createAccount(cashier,paymentType, realValue, paymentType.defaltCategory.obj.get, obs)) :: 
			(discountMovementOrEmpty(paymentDetail, discountValue,cashier,paymentType,value, AuthUtil.company.categoryOnProduct_?)) :: Nil
		}
	}
	def createDiscountAccount()={}
	def createAccount(cashier:Cashier,paymentType:PaymentType,value:Double, 
		category:AccountCategory,obs:String = "Faturamento fechamento do caixa (%s) valores em (%s)", accountType:Int=AccountPayable.IN)  = {
		//info(obs)
		val account = AccountPayable
			.createInCompany
			.unit(cashier.unit.is)
			.typeMovement(accountType)
			.category(category)
			.exerciseDate(cashier.openerDate.is)
			.obs(obs.format(cashier.idForCompany.is.toString, paymentType.name.is))
			.account(paymentType.defaltAccount.is)
			.auto_?(true)
			.cashier(cashier.id.is)
			.paymentType(paymentType.id.is)
			.costCenter(AuthUtil.unit.costCenter.is)
			.value(value)
		account
	}
	def process(cashier:Cashier, paymentType:PaymentType,value:Double,
		paymentDetail:List[PaymentDetail]=Nil):Unit
}

object ReceiveAtSight extends FatChain{
	def process(cashier:Cashier, paymentType:PaymentType,value:Double,paymentDetail:List[PaymentDetail]=Nil):Unit = {
		if (!paymentType.individualReceive_?) {
			buildDefaltAccount(cashier, paymentDetail(0), paymentType, value).foreach((am)=>{
					am match {
						case Full(movement) => {
							movement.dueDate(cashier.openerDate.is)
							.paid_?(true)
							.save						
						}
						case _ => 
					}
					
			})
		} else {
			paymentDetail.foreach((pd)=>{
				try{
					buildDefaltAccount(cashier, pd, paymentType, pd.value.is.toDouble,
						"Cx %s fp (%s)" + " (%s)".format(pd.payment.obj.get.customer.obj.get.name.is)+"-a vista").foreach((am)=>{
							am match {
								case Full(movement) => {
									movement.dueDate(cashier.openerDate.is)
									.user (pd.payment.obj.get.customer.obj.get.id.is)
									.paid_?(true)
									.save						
								}
								case _ => 
							}
							
					})
				}catch{
					case e:Exception =>{
						e.printStackTrace
						LogActor ! "Erro ao faturar a vista %s %s %s".format(paymentType.id.is.toString,paymentType.name.is,e.getMessage)
					}
					case _ => 
				}
			})
		}
	}
}
object ReceiveFixDays extends FatChain{
	def process(cashier:Cashier, paymentType:PaymentType,value:Double,paymentDetail:List[PaymentDetail]=Nil):Unit = {
		var calendar = Calendar.getInstance()
		calendar.setTime(cashier.openerDate.is)
		//val toDay = calendar.get(Calendar.DAY_OF_MONTH);
		calendar.add(Calendar.DAY_OF_MONTH, paymentType.numDaysForReceive.is)
		//var weekDay = calendar.get(Calendar.DAY_OF_WEEK);
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) { // domingo
			calendar.add(Calendar.DAY_OF_MONTH, 1)
		} else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) { // sábado
			calendar.add(Calendar.DAY_OF_MONTH, 2)
		}
		if (!paymentType.individualReceive_?) {
			buildDefaltAccount(cashier, paymentDetail(0), paymentType, value).foreach((am)=>{
					am match {
						case Full(movement) => {
							movement.dueDate(calendar.getTime)
							.paid_?(false)
							.save
						}
						case _ => 
					}
					
			})
		} else {
			paymentDetail.foreach((pd)=>{
				try{
					buildDefaltAccount(cashier, pd, paymentType, pd.value.is.toDouble,
						"Cx %s fp (%s)" + " (%s)".format(pd.payment.obj.get.customer.obj.get.name.is)+"-proj dias").foreach((am)=>{
							am match {
								case Full(movement) => {
									movement.dueDate(calendar.getTime)
											.user (pd.payment.obj.get.customer.obj.get.id.is)
											.paid_?(false)
											.save
								}
								case _ => 
							}
							
					})
				}
			})
		}
	}
}
object ReceiveAddValueToUser extends FatChain{
	def process(cashier:Cashier, paymentType:PaymentType,value:Double, paymentDetail:List[PaymentDetail]=Nil):Unit = {
		paymentDetail.foreach((pd)=>{
			try{

				buildDefaltAccount(cashier, pd, paymentType, pd.value.is,
					"Cx %s fp (%s)" + " serviço "+"(%s)".format(pd.treatmentDetailsAsText)).foreach(
					(am)=>{
							am match {
								case Full(movement) => {
									// nesta forma de pagamento se o customer não for profissional
									// tem que gerar o vale no user do treatment mesmo
									// 
									val bp : Long = if (pd.customer.obj.get.is_user_?.is) {
										pd.customer
									} else {
										pd.user.toLong
									}
									movement.dueDate(pd.dueDate.is)
											.user(bp)
											.typeMovement(AccountPayable.OUT)
											.paid_?(true) // senão nao abate da comissão
											.save
								}
								case _ => 
							}
							
					})
			}catch{
				case e:Exception =>{
					e.printStackTrace
					LogActor ! "Erro ao faturar para profissional %s %s".format(paymentType.id.is.toString,e.getMessage)
				}
				case _ => 
			}
		})
	}
}
object ReceiveCheque extends FatChain{
	def process(cashier:Cashier, paymentType:PaymentType,value:Double, paymentDetail:List[PaymentDetail]=Nil):Unit = {
		paymentDetail.foreach((pd)=>{
			try{
				val cheque = pd.cheque
				buildDefaltAccount(cashier, pd, paymentType, cheque.value.toDouble,
					"Cx %s fp (%s)" + " (%s)".format(cheque.customer.obj.get.name.is)+"-cheque").foreach((am)=>{
							am match {
								case Full(movement) => {
									movement.dueDate(cheque.dueDate)
											.cheque(cheque)
											.paid_?(false)
											.user (cheque.customer.obj.get.id.is)
											.save
								}
								case _ => 
							}
							
					})
			}catch{
				case e:Exception =>{
					e.printStackTrace
					LogActor ! "Erro ao faturar cheque%s %s".format(paymentType.id.is.toString,e.getMessage)
				}
				case _ => 
			}
		})
	}
}

object ReceiveParceled extends FatChain{
	def process(cashier:Cashier, paymentType:PaymentType,value:Double, paymentDetail:List[PaymentDetail]=Nil):Unit = {
		paymentDetail.foreach((pd)=>{
			try{
				// a primeira tentativa de garegar foi com cada pagamento de
				// cartão e seu desconto de taxa
				// depois resolvi agregar tudo de qq forma de pagto numa mesma data
				// isso talve pudesse ser um parm neste caso descomentar a atribuição 
				// do aggregatedId e o if na sequencia
				// rigel ago/2017
				var aggregId = 0l;
				buildDefaltAccount(cashier, pd, paymentType, pd.value.is.toDouble,"Cx %s fp (%s)" + " (%s)".format(pd.payment.obj.get.customer.obj.get.name.is)+"-parcelado").foreach((am)=>{
							am match {
								case Full(movement) => {
									movement.dueDate(pd.dueDate.is)
											.paid_?(false)
											.user (pd.payment.obj.get.customer.obj.get.id.is)
											//.aggregateId (aggregId)
											.save
									//if (aggregId == 0) {
									//	aggregId = movement.id
									//	movement.aggregateId (movement.id)
									//	movement.save;
									//}
								}
								case _ => 
							}
							
					})
			}catch{
				case e:Exception =>{
					//e.printStackTrace
					LogActor ! "Erro ao faturar parcelado %s %s".format(paymentType.id.is.toString,e.getMessage)
				}
				case _ => 
			}	
		})
	}
}

object ReceiveNextMonth extends FatChain{
	def process(cashier:Cashier,paymentType:PaymentType,value:Double,paymentDetail:List[PaymentDetail]=Nil):Unit = {
		val calendar = Calendar.getInstance()
		calendar.setTime(cashier.openerDate.is)
		val toDay = calendar.get(Calendar.DAY_OF_MONTH);

		if(toDay < paymentType.limitDay.is){
			calendar.add(Calendar.MONTH, 1)
		}else{
			calendar.add(Calendar.MONTH, 2)
		}
		calendar.set(Calendar.DAY_OF_MONTH, paymentType.day.is)
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) { // domingo
			calendar.add(Calendar.DAY_OF_MONTH, 1)
		} else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) { // sábado
			calendar.add(Calendar.DAY_OF_MONTH, 2)
		}
		if (!paymentType.individualReceive_?) {
			buildDefaltAccount(cashier, paymentDetail(0), paymentType, value).foreach((am)=>{
				am match {
					case Full(movement) => {
						movement.dueDate(calendar.getTime)
								.paid_?(false)
								.save
						}
					case _ => 
				}	
			})
		} else {
			paymentDetail.foreach((pd)=>{
				try{
					buildDefaltAccount(cashier, pd, paymentType, pd.value.is.toDouble,
						"Cx %s fp (%s)" + " (%s)".format(pd.payment.obj.get.customer.obj.get.name.is)+"-prox mês").foreach((am)=>{
						am match {
							case Full(movement) => {
								movement.dueDate(calendar.getTime)
								.user (pd.payment.obj.get.customer.obj.get.id.is)
								.paid_?(false)
										.save
							}
							case _ => 
						}	
					})
				}
			})
		}
	}
}

object NotReceive extends FatChain{
	def process(cashier:Cashier,paymentType:PaymentType,value:Double,paymentDetail:List[PaymentDetail]=Nil):Unit = {
	}
}

