package code
package service

import net.liftweb._
import mapper._
import scala.collection.mutable._
import code.util._
import code.comet._
import code.actors._
import model._
import net.liftweb.util._
import http.js._
import JE._
import net.liftweb.common._
//import InventoryMovement._
import java.util.Date
import java.util.Calendar

object CommissionService extends net.liftweb.common.Logger  {
  val sql_payments_to_reprocess = """
    update payment set commission_processed=false where  datepayment between  date(?) and date(?) and company=?;
  """
  val sql_paymentdetails_to_reprocess = """
    update paymentdetail 
    set  processed = false, 
    commisionnotprocessed=value 
    where payment
    in(select id from payment where commission_processed=false and  datepayment between  date(?) and date(?) and company=?)
  """
  val sql_clear_commision = """
    delete from commision
    where payment
    in(select id from payment where commission_processed=false and   datepayment between  date(?) and date(?) and company=?)
  """

  def prepareCommisionToReprocess(start:Date, end:Date, company:Company){
    val sqls = sql_payments_to_reprocess :: sql_paymentdetails_to_reprocess :: sql_clear_commision::Nil
    sqls.foreach((sql:String)=>{
      DB.runUpdate(sql,start::end::company.id.is::Nil)
    });
    reprocessAllNotProcessed(company);
  }

  def reprocessAllNotProcessed(company:Company){
    Payment.findAll(By(Payment.company, company), By(Payment.commision_processed_?, false)).foreach((p) => CommisionQueeue.processPaymentEnqueeue(p));
  }
  def processPayment(payment: PaymentProcessDTO) {
    DB.use(DefaultConnectionIdentifier) {
      conn =>
        val paymentObj = payment.payment
        clearPayment(paymentObj)
        val details = paymentObj.details.filter((pd) => !pd.processed_?.is)
        try{
            details.map((pd) => PaymentProcessor(pd, $(pd)))
            .map(_.commissions)
            .reduceLeft(_ ::: _)
            .foreach(_.save)
          }catch{
            case e:Exception => {
              LogActor ! "Erro ao processar comissão do payment [ "+payment.payment.id.is+" ] -> "+e.getMessage
            }
            case _ =>
        }
        try{
          val customerDebis = PaymentType.PaymentDebits(payment.payment.company.is)(0)
          if(customerDebis.generateCommision_?.is && !customerDebis.comissionAtSight_?.is){
            //LogActor ! "Processando Quitação de conta cliente com regra de balanceamento do pagamento [ "+payment.payment.id.is+" ]" 
            processAccountCommission(payment.payment)
          }
          details.foreach(_.processed_?(true).save)
          paymentObj.commision_processed_?(true).save
        }catch{
            case e:Exception => {
              LogActor ! "Erro ao processar Conta cliente do payment [ "+payment.payment.id.is+" ] -> "+e.getMessage
            }
            case _ =>
        }
    }
  }

  def processAccountCommission(payment: Payment) = {
    val debts = payment.treatments.filter(_.hasAccount)
    val dateToPayment = payment.datePayment
    val hasAccount = debts.size > 0
    if (hasAccount) {
      val customer = payment.customer.obj.get
      debts(0).accounts.map((account) => {
        LogActor ! "[processAccountCommission]Cliente " +customer.id.is
        val totalValue = account.price.is
        //LogActor ! "[processAccountCommission]Valor Pago " +totalValue
        val customerAccountValue = payment.valueInAccountAtPayment*(-1)
        //LogActor ! "[processAccountCommission]customerAccountValue" +customerAccountValue
        val percent:Double = (totalValue.toDouble / customerAccountValue)
        val abspercent:Double = if(percent < 0){ 
              percent*(-1.00)
          }else{
                percent
          }
        val paidPercent:Double = if (abspercent < 1.00) {
          LogActor ! "[processAccountCommission] Percentual de quitação"+(totalValue / customerAccountValue).toDouble
          (totalValue / customerAccountValue).toDouble
        } else {
          // LogActor ! "[processAccountCommission] 100%"
          1.00
        }
        val allDebits = customer.allDebits(payment)
        //LogActor ! "[processAccountCommission] allDebits : "+allDebits.size
        val commissions = allDebits.map((pd) => {
            val valueToProcess = (pd.commisionNotProcessed.is * paidPercent)
            //LogActor ! "[processAccountCommission] valueToProcess : "+valueToProcess
            LogActor ! "[processAccountCommission] Final  commissionNotProcessed: "+(pd.commisionNotProcessed.is - valueToProcess)
            (pd, valueToProcess)
          }).map((cpd) => {
            PaymentProcessor(cpd._1, CommissionGenerationStrategy.CustomerAccountCommissionCalculator(cpd._2.toDouble, dateToPayment))
          }).map(_.commissions)
          if(!commissions.isEmpty){
            commissions.reduceLeft(_ ::: _).foreach(_.save)
          }
      })
    }
  }

  def clearPayment(payment: Payment) {
    payment.commissions.foreach(_.delete_!)
  }

  def removePayment(payment: PaymentProcessDTO) {
    val paymentObj = payment.payment
    clearPayment(paymentObj)
    paymentObj.delete_!
  }

  /**
   * builf Commition stategy
   */
  def $(paymentdetail: PaymentDetail) = {
    import CommissionGenerationStrategy._
    val paymentType = paymentdetail.typePaymentObj.get
    if(paymentType.comissionAtSight_?){
      DefaultCommissionCalculator
    }else if (!paymentType.generateCommision_?.is) {
      NotCommissionCalculator
    } else if (paymentType.comissionAtSight_?.is) {
      println ("vaiiii ===================== ACHO QUE NUNCA CHEGA AQUI - comissionAtSight_")
      DefaultCommissionCalculator
    } else if (paymentType.cheque_?.is) {
      ChequeCommissionCalculator
    } else if (paymentType.numDays.is > 0) {
      FixDaysCommissionCalculator(paymentType.numDays.is)
    } else if (paymentType.customerRegisterDebit_?.is) {
        val payment = paymentdetail.payment.obj.get
        def valueToProcess:Double =  try{
                                        if(payment.valueInAccountAtPayment <= 0){
                                          0.00
                                        }else{
                                          paymentdetail.value.is.toDouble
                                       }
                                    }catch{
                                      case _ => {
                                        0.00
                                      }
                                    }
      //LogActor ! "[factory$] customerRegisterDebit payment(%s) valueToProcess(%s)".format(payment.id.is.toString(),valueToProcess.toString())
      CustomerAccountCommissionCalculator(valueToProcess, payment.datePayment.is)
    }else if(paymentType.bpmonthly_?.is){
      MonthlyCommissionCalculator
    }else{
      DefaultCommissionCalculator
    }
  }
}
trait CommissionCalculator {
  protected def comissionProcessedProcess(pd:PaymentDetail){
    pd.commisionNotProcessed(0).save
  }
  protected def buildCommission(company: Long, payment: Payment, 
    finalValue: BigDecimal, due_date: Date, 
    payment_date: Date, payment_detail: PaymentDetail, 
    treatment_detail: TreatmentDetail, user: User, 
    cheque: Box[Cheque], 
    commision: Box[Commision] = Empty): Commision = {
    val commisionObj = commision match {
      case Full(c) => c
      case _ => Commision.create
    }
    comissionProcessedProcess(payment_detail)
    commisionObj.company(company).payment(payment)
    .value(finalValue.toDouble)
    .due_date(due_date)
    .payment_date(payment_date)
    .payment_detail(payment_detail)
    .treatment_detail(treatment_detail)
    .check(cheque)
    .user(user)
    .product(treatment_detail.activity_id)
/*    primeira tentativa de gerar a comissão do supeior - 
      tb funcionaria, mas a oura solução ficou melhor - rigel - 02/2017
      faltava ainda aceertar o valor
    if (!user.parent.isEmpty) {
      val commisionObj1 = Commision.create
      val parentValue = (finalValue.toDouble/100) * user.parent_percent
        commisionObj1.company(company).payment(payment).value(parentValue).due_date(due_date).payment_date(payment_date).payment_detail(payment_detail).treatment_detail(treatment_detail).check(cheque).user(user.parent).product(treatment_detail.activity_id).save
      commisionObj.company(company).payment(payment).value(finalValue.toDouble).due_date(due_date).payment_date(payment_date).payment_detail(payment_detail).treatment_detail(treatment_detail).check(cheque).user(user).product(treatment_detail.activity_id)
    } else {
      commisionObj.company(company).payment(payment).value(finalValue.toDouble).due_date(due_date).payment_date(payment_date).payment_detail(payment_detail).treatment_detail(treatment_detail).check(cheque).user(user).product(treatment_detail.activity_id)
    }
*/
  }

  def canBeBuyingBpMonthly = true;

  def discountPerPaymentType(value: Double, payment_detail: PaymentDetail, user:User): Double = {
    // rigel 18/10/2017
    if (user.discountToCommission_?) {
      value * (payment_detail.typePaymentObj.get.percentDiscountToCommision.is / 100).toDouble
    } else {
      0.0;
    }
  }
  def paymentDate(payment_detail: PaymentDetail): Date = payment_detail.payment.obj.get.datePayment.is
  def cheque(payment_detail: PaymentDetail): Box[Cheque] = Empty
  def priceToCommission(treatmentDetail:TreatmentDetail) = {
    treatmentDetail.priceToCommission
  }
  def percentInTotal(payment_detail:PaymentDetail) = payment_detail.percentInTotal / 100
  def calculate(payment_detail: PaymentDetail): List[Commision] = {
    val percent_in_total = percentInTotal(payment_detail)
    val payment = payment_detail.payment.obj.get
    val treatments = payment.treatments
    val treatmentsToCommission = treatments.filter((t) => {
      t.user.obj match {
        case Full(u) => true
        case _ => false
      }
    }).filter((t)=> t.hasDetail)
    if (!treatmentsToCommission.isEmpty) {
      treatmentsToCommission.map((t) => {
        val user = t.userObj
        val treatmentDetails = t.details
        treatmentDetails.map((treatment_detail) => {
          val commision = Commision.create.company(payment_detail.company)
          commision.save
          val percentCommission : BigDecimal = if (canBeBuyingBpMonthly && treatment_detail.isAMonthlyService) {
              0.0
            }else{
              treatment_detail.commissionActivity / 100.0;
            }
          val absCommission : BigDecimal = if (canBeBuyingBpMonthly && treatment_detail.isAMonthlyService) {
              0.0
            }else{
              treatment_detail.commissionAbsActivity;
            }
//          val percentCommission = treatment_detail.commissionActivity / 100.00;
//          val absCommission = treatment_detail.commissionAbsActivity;
          commision.addDetail("Val fpagto 1 " + payment_detail.value.is + 
            " Perc fpagto " + percent_in_total + 
            " Val serviço " + treatment_detail.price.is +
            " Tot descontos " + treatment_detail.discountsTotal.toDouble * (-1) +
            " Val calc comissão " + priceToCommission(treatment_detail) +
            " Perc comissão prof " + percentCommission + " % ")
          //commision.addDetail("Percentual da forma de pagamento no total : " + percent_in_total)

          //commision.addDetail("Valor do serviço :" + treatment_detail.price.is)
          //commision.addDetail("Total de descontos do serviço", treatment_detail.discountsTotal.toDouble * (-1))
          //commision.addDetail("Valor para calculo de comissão : " + priceToCommission(treatment_detail))
          //commision.addDetail("Percentual de comissão do profissional : " + percentCommission + " % ")

          val valueToUser = (priceToCommission(treatment_detail) * percentCommission) + 
            absCommission - 
            (treatment_detail.auxiliarCommissionValue) - 
            (treatment_detail.superiorCommissionValue (user.parent_percent));

          if ((treatment_detail.auxiliarCommissionValue + treatment_detail.auxiliarHouseCommissionValue)*percent_in_total > 0.0){
            commision.addDetail("Valor assistente 1: " + 
              (treatment_detail.auxiliarCommissionValue + treatment_detail.auxiliarHouseCommissionValue)*percent_in_total )
          }

          val finalValueWithoudDicount = (valueToUser * percent_in_total).toDouble //dicountPerPaymentType(,payment_detail)

          val discountPerPaymentTypeValue = discountPerPaymentType(finalValueWithoudDicount, payment_detail, user)

          if (discountPerPaymentTypeValue > 0.0) {
            commision.addDetail("Val prof antes desc fpagto " + finalValueWithoudDicount, finalValueWithoudDicount.toDouble)
          }

          //commision.addDetail("Valor dos descontos por forma de pagamento", dicountPerPaymentTypeValue.toDouble * (-1))

          val finalValue = finalValueWithoudDicount - discountPerPaymentTypeValue

          //commision.addDetail("Valor final para o profissional 1: " + finalValue)

          val dueDate = payment.datePayment
          val payment_date = paymentDate(payment_detail)
          buildCommission(t.company.is, payment, finalValue, dueDate, payment_date, payment_detail, treatment_detail, user, cheque(payment_detail), Full(commision)) :: Nil
        }).reduceLeft(_ ::: _)
      }).reduceLeft(_ ::: _)
    } else {
      Nil
    }
  }
}

case class PaymentProcessDTO(paymentId: Long, remove: Boolean, treatments: List[Long] = Nil)  extends net.liftweb.common.Logger {
  def this(payment: Payment, remove: Boolean) {
    this(payment.id.is, remove)
  }
  def payment = {
    //info( "vaiii PaymentId "+paymentId.toString )
    Payment.findByKey(paymentId).get
  }
}

object CommissionGenerationStrategy {

  object DefaultCommissionCalculator extends CommissionCalculator {
  }
  case class CustomerAccountCommissionCalculator(valueToProcess: Double, dataToPayment:Date) extends CommissionCalculator {
    override protected def comissionProcessedProcess(pd:PaymentDetail){
      pd.commisionNotProcessed(pd.commisionNotProcessed.is-valueToProcess).save
    }
    override def percentInTotal(payment_detail:PaymentDetail) = payment_detail.percentInTotal(valueToProcess) / 100;
    override def calculate(payment_detail: PaymentDetail): List[Commision] = {
      val percent_in_total = percentInTotal(payment_detail)
      val payment = payment_detail.payment.obj.get
      val treatments = payment.treatments
      val treatmentsToCommission = treatments.filter((t) => {
        t.user.obj match {
          case Full(u) => true
          case _ => false
        }
      }).filter((t)=> t.hasDetail)
      if (!treatmentsToCommission.isEmpty && percent_in_total > 0.00) {
        treatmentsToCommission.map((t) => {
          val user = t.userObj
          val treatmentDetails = t.details
          treatmentDetails.map((treatment_detail) => {
            val commision = Commision.create.company(payment_detail.company)
            commision.save
            val percentCommission : BigDecimal = if (canBeBuyingBpMonthly && treatment_detail.isAMonthlyService) {
                0.0 
              }else{
                treatment_detail.commissionActivity / 100.0;
              }
            val absCommission : BigDecimal = if (canBeBuyingBpMonthly && treatment_detail.isAMonthlyService) {
                0.0             
              }else{
                treatment_detail.commissionAbsActivity;
              }

            commision.addDetail("Percentual de quitação de conta cliente 2: " + percent_in_total)
            commision.addDetail("Percentual de comissão : " + percentCommission)
  //          val valueToUser = (priceToCommission(treatment_detail) * percentCommission)
            val valueToUser = (priceToCommission(treatment_detail) * percentCommission) + 
              absCommission - 
              (commision.auxiliarCommissionValue) - 
              (treatment_detail.superiorCommissionValue (user.parent_percent))
            commision.addDetail("Valor assistente 2: " + 
              (commision.auxiliarCommissionValue + commision.auxiliarHouseCommissionValue) *percent_in_total)
            commision.addDetail("Valor do serviço: " + treatment_detail.price.is)
            commision.addDetail("Valor para o profissional : " + valueToUser)
            val finalValueWithoudDicount = (valueToUser * percent_in_total).toDouble //dicountPerPaymentType(,payment_detail)
            commision.addDetail("Valor final antes dos descontos : " + finalValueWithoudDicount)
            val finalValue = finalValueWithoudDicount - discountPerPaymentType(finalValueWithoudDicount, payment_detail, user)
            commision.addDetail("Valor depois dos descontos 2: " + finalValue)
            val dueDate = payment.datePayment
            val payment_date = dataToPayment
            buildCommission(t.company.is, payment, finalValue, dueDate, payment_date, payment_detail, treatment_detail, user, cheque(payment_detail),Full(commision)) :: Nil
          }).foldLeft(List[Commision]())(_ ::: _)
        }).foldLeft(List[Commision]())(_ ::: _)
      } else {
        Nil
      }
    }
  }

  object ChequeCommissionCalculator extends CommissionCalculator {
    override def cheque(payment_detail: PaymentDetail) = Full(payment_detail.cheque)
    override def paymentDate(payment_detail: PaymentDetail) = payment_detail.cheque.paymentDate.is
  }

  object MonthlyCommissionCalculator extends CommissionCalculator with net.liftweb.common.Logger  {

    override def canBeBuyingBpMonthly = false;

    override def priceToCommission(treatmentDetail:TreatmentDetail) = {
      //info("val customer = treatmentDetail.customer")
      val customer = treatmentDetail.customer
      //info("val monthly ")
      val monthly = BpMonthly.monthlyByProduct(treatmentDetail.productBase, customer, treatmentDetail.start)
      //info("val monthlyValue = monthly.value.is")
      val monthlyValue = monthly.valueDiscount.is
      //info("val sessionValue = monthly.sessionValue")
      val sessionValue = monthly.sessionValue
      //info("sessionValue")
      // treatmentDetail.price(sessionValue)
      // treatmentDetail.save
      // LogActor ! "Valor da sessão: " + sessionValue
      sessionValue
      //treatmentDetail.priceToCommission
    }
  }

  case class FixDaysCommissionCalculator(numDays: Int) extends CommissionCalculator {
    override def paymentDate(payment_detail: PaymentDetail) = {
      val c = Calendar.getInstance
      c.setTime(payment_detail.dueDate.is)
      c.add(Calendar.DATE, (payment_detail.typePaymentObj.get.numDays.is-1))
      c.getTime
    }
  }

  object ParceledCommissionCalculator extends CommissionCalculator {
    override def calculate(payment_detail: PaymentDetail): List[Commision] = {
      Nil
    }
  }

  object NotCommissionCalculator extends CommissionCalculator {
    override def calculate(payment_detail: PaymentDetail): List[Commision] = {
      payment_detail.save;
      Nil
    }
  }
}
case class PaymentProcessor(paymentdetail: PaymentDetail, calc: CommissionCalculator) {

  def commissions: List[Commision] = {
    val commissions =  calc.calculate(paymentdetail)
    val percent:Double = calc.percentInTotal(paymentdetail)
    commissions ::: checkAuxiliar(commissions, percent) ::: checkSuperior(commissions, percent)
  }

  def checkAuxiliar(commissions:List[Commision], percent:Double): List[Commision] = {
    Nil
    val auxiliarCommissions = commissions.filter( (c) => {
          c.hasAuxiliar
      })
      .map( (c) => c.clone.value((c.auxiliarCommissionValue + c.auxiliarHouseCommissionValue) * percent ).user(c.auxiliar) )
    auxiliarCommissions
  }
  def checkSuperior(commissions:List[Commision], percent:Double): List[Commision] = {
    Nil
    val superiorCommissions = commissions.filter( (c) => {
          c.hasSuperior
      })
      .map( (c) => c.clone.value((c.superiorCommissionValue) * percent ).user(c.superior) )
    superiorCommissions
  }
}

