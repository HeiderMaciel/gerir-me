package code
package model 

import net.liftweb._ 
import scala.xml._
import mapper._ 
import http._ 
import SHtml._ 
import util._
import code.util._
import _root_.java.math.MathContext
import net.liftweb.mapper.DB
import net.liftweb.common._
import java.util.Date

class PaymentType extends Audited[PaymentType] with PerCompany with IdPK with CreatedUpdated 
with CreatedUpdatedBy with NameSearchble[PaymentType] with ActiveInactivable[PaymentType] {
  def getSingleton = PaymentType
  override def updateShortName = false
  
  object sumInCachier_? extends MappedBoolean(this){
    override def dbColumnName = "sumInCachier"
  }


  object sumToConference_? extends MappedBoolean(this){
    override def dbColumnName = "sumtoconference"
    override def defaultValue = false
  }  
  object numDays extends MappedInt(this) { // for commission
    override def defaultValue = 1;
  }  

  object order extends MappedInt(this){
    override def defaultValue = 1000
  }

  object key extends MappedPoliteString(this,50){

  }
  
  object generateCommision_? extends MappedBoolean(this){
    override def defaultValue = true
    override def dbColumnName = "generateCommision"
  }
  object comissionAtSight_? extends MappedBoolean(this){//Avista
    override def defaultValue = true
    override def dbColumnName = "comissionAtSight"
  }
  
  object creditCard_? extends MappedBoolean(this){
    override def dbColumnName = "creditCard"
  }

  object needCardInfo_? extends MappedBoolean(this){
    override def dbColumnName = "needCardInfo"
    override def defaultValue = false;
  }
  
  object cheque_? extends MappedBoolean(this){
    override def dbColumnName = "cheque"
  }

  object needChequeInfo_? extends MappedBoolean(this){
    override def dbColumnName = "needChequeInfo"
    override def defaultValue = true;
  }

  object deliveryContol_? extends MappedBoolean(this){
    override def dbColumnName = "deliveryContol"
  }

  object fidelity_? extends MappedBoolean(this){
    override def dbColumnName = "fidelity"
  }

  object customerRegisterDebit_? extends MappedBoolean(this){
    override def dbColumnName = "customerRegisterDebit"
  }

  object customerUseCredit_? extends MappedBoolean(this){
    override def dbColumnName = "customerusecredit"
  }

  object addUserAccountToDiscount_? extends MappedBoolean(this){
    override def dbColumnName = "addUserAccountToDiscount"
  }  
  object allowCustomeraddUserToDiscount_? extends MappedBoolean(this){
    override def dbColumnName = "allowCustomeraddUserToDiscount"
    override def defaultValue = false
  }  
  
  object bpmonthly_? extends MappedBoolean(this){
      override def defaultValue = false
      override def dbColumnName = "bpmonthly"
  }    

  object offSale_? extends MappedBoolean(this){
      override def defaultValue = false
      override def dbColumnName = "offsale"
  }    

  object showAsOptions_? extends MappedBoolean(this){
    override def defaultValue = true
    override def dbColumnName = "showAsOptions"
  }

  object showAsFinOptions_? extends MappedBoolean(this){
    override def defaultValue = true
    override def dbColumnName = "showAsFinOptions"
  }

  object acceptInstallment_? extends MappedBoolean(this){// parcelamento
    override def defaultValue = true
    override def dbColumnName = "acceptInstallment"
  }
  //Receive Options
  object receiveAtSight_? extends MappedBoolean(this){//Fatura a vista?
    override def defaultValue = true
    override def dbColumnName = "receiveatsight"
  }
  object receive_? extends MappedBoolean(this){//Nao recebe ex Cortesia....
    // quando true vai gerar financeiro
    override def defaultValue = false
    override def dbColumnName = "receive"
  }
  object individualReceive_? extends MappedBoolean(this){
    // parcelado e cheque sempre fatura individual - as outras antes só faturava consolidado
    // marcado true aqui - dinheiro, debito que normalmente não era parcelado podem ser gerados
    // individualmente no financeiro
    override def defaultValue = false
    override def dbColumnName = "individualreceive"
  }
  object autoChangeToPaid_? extends MappedBoolean(this){
      override def defaultValue = false
      override def dbColumnName = "autochangetopaid"
  }    
  object usernotification_? extends MappedBoolean(this){
      override def defaultValue = true
      override def dbColumnName = "usernotification"
  }    
  object nextMonth_? extends MappedBoolean(this){//Fatura no proximo mes
    override def defaultValue = false
    override def dbColumnName = "nextmonth"
  }
  object day extends MappedInt(this){// Dia para o caso de dia fixo
    override def defaultValue = 5
    override def dbColumnName = "day"
  }
  object limitDay extends MappedInt(this){// Dia limit para mes seguinte
    override def defaultValue = 15
    override def dbColumnName = "limitDay"
  }  
  object numDaysForReceive extends MappedInt(this)//Numedo de dias para recebimento ou faturamento
  object percentDiscountToReceive extends MappedDouble(this)//Percentual de desconto to receive
  object percentDiscountToCommision extends MappedDouble(this){
    override def defaultValue = 0.00
  }//Percentual de desconto para comissao
  object defaltAccount extends MappedLongForeignKey(this,Account)//Conta padrao...
  object defaltCategory extends MappedLongForeignKey(this, AccountCategory)//Categoria padrao...
  object defaltDicountCategory extends MappedLongForeignKey(this, AccountCategory)//Categoria padrao para desconto...
  
  override def delete_! = {
      if(PaymentDetail.count(By(PaymentDetail.typePayment,this.id)) > 0){
          throw new RuntimeException("Existe detalhe de pagamento para esta forma de pagamento! ")
      }
      if(AccountPayable.count(By(AccountPayable.paymentType,this.id)) > 0){
          throw new RuntimeException("Existe lançamento financeiro para esta forma de pagamento! ")
      }
      super.delete_!
  }
  override def save() = {
    if ((addUserAccountToDiscount_?) && (receiveAtSight_?)) {
      // println ("************************* Comportamento especial <vale profissional> não pode ser faturado a vista")
      // para permitir parcelamento e alteracao da data de desconto
      throw new RuntimeException("Comportamento especial <vale profissional> não pode ser faturado a vista")
    }
    super.save
  }
}
object PaymentType extends PaymentType with LongKeyedMapperPerCompany[PaymentType]  with  OnlyActive[PaymentType] with NameSearchble[PaymentType]{
    def findAllForOption = findAllInCompany(By(PaymentType.showAsOptions_?,true),OrderBy(PaymentType.order, Ascending))
    def PaymentCheckIds = 0l :: findAllInCompany(By(PaymentType.cheque_?,true)).map(_.id.is)
    def PaymentCardIds = 0l :: findAllInCompany(By(PaymentType.creditCard_?,true)).map(_.id.is)
    def PaymentToConferenceIds = 0l :: findAllInCompany(By(PaymentType.sumToConference_?,true)).map(_.id.is)
    def PaymentMoneyIds = 0l :: findAllInCompany(By(PaymentType.creditCard_?,false),
      By(PaymentType.cheque_?,false),
      By(PaymentType.sumInCachier_?,true)).map(_.id.is)
    
    def PaymentDebitsIds = 0l :: findAllInCompany(By(PaymentType.customerRegisterDebit_?,true)).map(_.id.is)
    def PaymentDebitsIds(companyId:Long) = 0l :: findAll(By(PaymentType.customerRegisterDebit_?,true),By(PaymentType.company,companyId)).map(_.id.is)    

    def PaymentDebits(companyId:Long) = findAll(By(PaymentType.customerRegisterDebit_?,true),By(PaymentType.company,companyId))

    def totalValueByCustomerInDebit(customer:Long) = DB.performQuery("select sum(value) from paymentdetail where payment in (select payment from treatment where customer = ?) and typePayment in(select id from paymenttype where customerRegisterDebit = true)", List(customer))._2(0)(0) match {
           case a:Any => a.toString.toDouble
           case _ => 0.toDouble
    }
 
    def resetPt (company:Company, nameLike:String) = {
      val pm = PaymentType.findAll (By(PaymentType.company, company),
        BySql(" lower (name) like ? ",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),nameLike))
      pm(0).status (0).save
    }

 }