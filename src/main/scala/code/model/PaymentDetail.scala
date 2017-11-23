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


class PaymentDetail extends LongKeyedMapper[PaymentDetail]  with IdPK with CreatedUpdated 
    with CreatedUpdatedBy with PerCompany with WithCustomer with Audited[PaymentDetail]{
    def getSingleton = PaymentDetail
    object value extends MappedCurrency(this)
    object commisionNotProcessed extends MappedCurrency(this){
      override def defaultValue = fieldOwner.value.is
    }
    object typePayment extends MappedLong(this)
    object payment extends MappedLongForeignKey(this,Payment)
    object dueDate extends EbMappedDate(this)
    object processed_? extends MappedBoolean(this){
      override def dbColumnName = "processed"
    }
    def cheque = Cheque.findAll(By(Cheque.paymentDetail,this))(0)
    def percentInTotal = {
      if (value != 0.0) {
          ((100.00 * value.is) / payment.obj.get.value.is)
      } else if (payment.obj.get.value.is != 0.0) {
        0.0
      } else { // rigel 20/09/2016 para gerar comissão absoluta compagamento zero
        100.0
      }
    }

    def percentInTotal(value:Double) = {
      if (value != 0.0) {
        ((100.00 * value) / payment.obj.get.value.is)
      } else {
        0.0
      }
    }
    
    def typePaymentObj = PaymentType.findByKey(typePayment.is)

    def bpmonthly_? = typePaymentObj match {
      case Full(o:PaymentType) => o.bpmonthly_?.is
      case _ => false
    }

    def typePaymentTranslated = typePaymentObj match {
      case Full(o:PaymentType) => o.name.is
      case _ => "Def"
    }

    def typePaymentName = typePaymentObj match {
      case Full(o:PaymentType) => o.short_name.is
      case _ => "Def"
    }

    def treatmentDetailsAsText = payment.obj.get.treatmentDetailsAsText

    // usado para pgto vale profissional feito para cliente
    // ou seja o cliente neste caso não é profissional
    // no financeiro não gera vale para o cliente mas sim para o profissional mesmo
    def user = payment.obj.get.user
    
    def categoryByType (category:AccountCategory) = payment.obj.get.categoryByType (category)
    def discountCategoryByType (category:AccountCategory) = payment.obj.get.discountCategoryByType (category)

}

object PaymentDetail extends PaymentDetail with LongKeyedMapperPerCompany[PaymentDetail] with OnlyCurrentCompany[PaymentDetail] {

}
