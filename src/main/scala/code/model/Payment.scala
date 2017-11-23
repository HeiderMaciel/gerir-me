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


class Payment extends LongKeyedMapper[Payment] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with OneToMany[Long, Payment] with LogicalDelete[Payment] with WithCustomer{
    def getSingleton = Payment
    object details extends MappedOneToMany(PaymentDetail, PaymentDetail.payment, OrderBy(PaymentDetail.id, Ascending))
    object treatments extends MappedOneToMany(Treatment, Treatment.payment, By(Treatment.hasDetail, true), OrderBy(Treatment.id, Ascending))
    object value extends MappedCurrency(this)
    object cashier extends MappedLongForeignKey(this,Cashier)
    object command extends MappedPoliteString(this,50)    
    object datePayment extends EbMappedDate(this)
    object detailPaymentAsText extends MappedPoliteString(this,400) with LifecycleCallbacks {
            override def beforeSave() {
                if (createdBy != 1) {
                  // se foi migrado user = 1 - mantem o detailastext original
                  super.beforeSave;
                  this.set(fieldOwner.asInstanceOf[Payment].descritionByDetails)
                }
            }         
        override def  defaultValue = descritionByDetails
    }

    def descritionDetails = {
        descritionByDetails
    }

    def descritionByDetails = {
        details match { 
                case (dl) if(dl.size >0 ) => { 
                  (details map(_.typePaymentName) reduceLeft(_+", "+_))
                }
                case _ => {
                    ""
                }
            }
    }    

    def commissions = Commision.findAll(By(Commision.payment,this))
    def deliveries = DeliveryControl.findAll(By(DeliveryControl.payment,this))
    def treatmentDetailsAsText = {
      treatments.map(_.detailTreatmentAsText.is).
      reduceLeft( _+" , "+_)
    }

    def unitId = {
      treatments(0).unit.obj.get.id.is
    }

    def categoryByType (category:AccountCategory) = {
      treatments(0).details(0).categoryByType (category)
    }

    def discountCategoryByType (category:AccountCategory) = {
      treatments(0).details(0).discountCategoryByType (category)
    }
    
    // usado para pgto vale profissional feito para cliente
    // ou seja o cliente neste caso não é profissional
    // no financeiro não gera vale para o cliente mas sim para o profissional mesmo
    def user = treatments(0).user;

    def treatmentUserAsText = {
      treatments.map(_.userName).
      reduceLeft( _+" , "+_)
    }
    def customerName = {
      customer.obj match {
        case Full(c) => c.name.is.toString
        case _ => ""
      }
    }
    object commision_processed_? extends MappedBoolean(this){
      override def dbColumnName = "commission_processed"
    }
    def valueOfDetail:Double = {
      details match {
                case (dl) if(dl.size >0 ) => {
                    details map( _.value.is) reduceLeft(_+_)
                }
                case _ => {
                    0.0
                }
            }
    }

    def totalOfTreatments:Double = {
      treatments match {
                case (ts) if(ts.size >0 ) => {
                    BusinessRulesUtil.roundHalfUp(ts map( _.totalValue(0).toDouble) reduceLeft(_+_))
                }
                case _ => {
                    0.0
                }
            }      
    }

    def totalPaidByCashier(idCashier:Long):Double = {
       val r = DB.performQuery("select sum(value) from paymentdetail where payment in (select id from payment where cashier = ?) and typePayment in(select id from paymenttype where sumInCachier = true)", List(idCashier))
       r._2(0)(0) match {
           case a:Any => a.toString.toDouble
           case _ => 0.toDouble
       }
    }

    def totalPaidByCashier(idCashier:Long,typePaymentoIds:List[Long]):Double = {
       val r = DB.performQuery("select sum(value) from paymentdetail where payment in (select id from payment where cashier = ?) and typePayment in(%s)".format(typePaymentoIds.map(_.toString).reduceLeft(_+","+_)), List(idCashier))
       r._2(0)(0) match {
           case a:Any => a.toString.toDouble
           case _ => 0.toDouble
       }
    }  

    def valueReturn = {
      valueOfDetail - totalOfTreatments
    }
    def prepareValueOfDetails = {
      if((totalOfTreatments - valueOfDetail) > 0.009 ){
        details filter((pt:PaymentDetail) => PaymentType.PaymentMoneyIds.contains(pt.typePayment.is)) match {
          case (ds) if(ds.size > 0) => {
            ds foreach((d) => {
              if(d.value.is >= valueReturn){
                d.value(d.value.is - valueReturn)
              }
            })
          }
          case _ =>{
             throw new PaymentTypeNotAvailableToReturn
          } 
        }

        if(valueReturn > 0){
          throw new PaymentTypeNotAvailableToReturn
        }
      }
      value(valueOfDetail)
    }
    def validateCommandRepeat:Boolean = {
      val allowrepeatcommand = this.company.obj.get.allowRepeatCommand_?.is
      if(allowrepeatcommand){
        true
      }else{
        Payment.countInCompany(By(Payment.command, this.command), By(Payment.datePayment, this.datePayment), NotBy(Payment.id, this.id)) == 0
      }
    }
    def detailTreaments = this.treatments.map(_.details.toList).reduceLeft(_:::_)
    def deliveryDetails = detailTreaments.filter(_.product.obj match {
        case Full(p) => p.is_bom_?.is
        case _ => false
      })

    def validadeAddUserAccountToDiscountIfCustomerIsAUser {
      // valida se na forma de pagto vale profissional aceita pagto pra cliente 
      // ou se exige que o cliente seja profissional
      val paymentDetailtoAddUserAccountToDiscount = this.details.filter(
        _.typePaymentObj.get.addUserAccountToDiscount_?.is).filter(!
        _.typePaymentObj.get.allowCustomeraddUserToDiscount_?.is)

      if(!paymentDetailtoAddUserAccountToDiscount.isEmpty){
        if(!this.customer.obj.get.is_user_?.is){
          throw new PaymentCustomerIsNotAUser(paymentDetailtoAddUserAccountToDiscount(0).typePaymentObj.get.name.is, this.customer.obj.get.name.is)
        }
      }
    }

    def validadeProductsAllowSaleByUser {
      detailTreaments.foreach( (td) => {
          if(!td.productBase.allowSaleByUser_? && td.hasUser){
            throw new ProductNotAllowSaleByUser(td.productBase.name.is);
          }
      })
    }
    def validateRules ={
//      dava erros de arredondamento por exemplo 940,00 em 6 vezes
//      if(this.totalOfTreatments > this.value.is){
      if((this.totalOfTreatments - this.value.is) > 0.001){
        throw new PaymentIsNotEnough
      }
      if(this.command.is.trim == "" || this.command.is.trim == "0"){
        throw new CommandIsNotValid
      }

      if(!validateCommandRepeat){
        throw new NotAllowCommandRepeat
      }
      validadeProductsAllowSaleByUser
      validadeAddUserAccountToDiscountIfCustomerIsAUser
      this
    }
    def usedDeliveryDetails = deliveries.map(_.usedDetails).reduceLeft(_:::_)
    def hasDeliveryUsed = {
      !deliveries.filter((d) => !d.usedDetails.isEmpty).isEmpty
    }
    def validateToDelete = {
      if(this.cashier.obj.get.isClosed){
        throw new CashierIsClosed
      }
      if(hasDeliveryUsed){
        throw HaveDeliveriesUsed(usedDeliveryDetails)
      }
    }

    override def delete_! = {
        validateToDelete
        details.foreach(_.delete_!)
        super.delete_!
    }

    def valueInAccountAtPayment:Double = {
      customer.obj.get.valueInAccountAtPayment(this)
    }
}

object Payment extends Payment with LongKeyedMapperPerCompany[Payment]  with  OnlyCurrentCompany[Payment]{
	object PaymentTypes extends Enumeration {
    	type Types = Value
     	val Money = Value
     	val Card = Value
     	val Check = Value
	}

  override def count(params: QueryParam[Payment]*): Long = {
    val deleteds = By(Payment.deleted_?, false);
    super.count(deleteds :: params.toList :_*)
  }

  override def findAll(params: QueryParam[Payment]*): List[Payment] = {
      val deleteds = By(Payment.deleted_?, false);
      super.findAll( deleteds :: params.toList :_*)
  }      
  def findAllWithDeleteds(params: QueryParam[Payment]*) = {
      super.findAll(params.toList :_*)
  }  
}
