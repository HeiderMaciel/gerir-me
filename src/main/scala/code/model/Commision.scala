
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

class Commision  
  extends LongKeyedMapper[Commision] 
  with PerCompany 
  with IdPK 
  with CreatedUpdated 
  with CreatedUpdatedBy
  with Audited[Commision] 
  with CanCloneThis[Commision] {
  def getSingleton = Commision

  def addDetail(message:String, value:Double=0) {
    CommisionDetails.create.description(message).value(value).commision(this).company(this.company).save
  }
  object payment extends MappedLongForeignKey(this,Payment)
  object product extends MappedLongForeignKey(this,Product)
  object value extends MappedCurrency(this)
  object due_date extends EbMappedDate(this)
  object payment_date extends EbMappedDate(this)

  object payment_detail extends MappedLongForeignKey(this,PaymentDetail){
    override def dbIndexed_? = true
  }

  def auxiliar = treatment_detail.obj match {
    case Full(td) => td.auxiliar.is
    case _ => 0l
  }
  def hasAuxiliar = treatment_detail.obj match {
    case Full(td) => td.hasAuxiliar
    case _ => false
  }

  def superior = user.obj match {
    case Full(u) => u.parent.is
    case _ => 0l
  }

  def hasSuperior = user.obj match {
    case Full(td) => td.hasSuperior
    case _ => false
  }

  def superiorCommissionValue:Double =  {
    if(hasSuperior){
        val percent = user.obj.get.percentToParrent
        treatment_detail.obj.get.superiorCommissionValue (percent)
      }else{
        0.00
      }
  }

  def auxiliarCommissionValue:Double =  {
    if(hasAuxiliar){
        treatment_detail.obj.get.auxiliarCommissionValue
      }else{
        0.00
      }
  }
  def auxiliarHouseCommissionValue:Double =  {
    if(hasAuxiliar){
        treatment_detail.obj.get.auxiliarHouseCommissionValue
      }else{
        0.00
      }
  }
   

  object treatment_detail extends MappedLongForeignKey(this,TreatmentDetail){
    override def dbIndexed_? = true
  }

  object user extends MappedLongForeignKey(this,User){
    override def dbIndexed_? = true
  }

  object check extends MappedLongForeignKey(this,Cheque){
    override def dbIndexed_? = true 
  }
  override def save = {
    if(this.company.is < 1) {
      if(AuthUtil.?) {
        this.company.set (AuthUtil.company.id.is) 
        LogObj.wLogObj (AuthUtil.company.id.is,"commission sem company resolvido - produto " + this.product, "erro");
      } else {
        LogObj.wLogObj (1,"commission sem company não resolvido - produto " + this.product, "erro");
        throw new RuntimeException("erro ao salvar pagamento");
      }
    }
    val result = super.save
    result
  }
}

object Commision extends Commision with LongKeyedMapperPerCompany[Commision]  with  OnlyCurrentCompany[Commision] with  CanClone[Commision]{
  def totalCommsionWithoudDiscount(user:User, start:Date, end:Date):Double = {
      val r = DB.performQuery("select sum(value) from commision  where user_c=? and payment_date  between date(?) and date(?)", List(user.id.is,start, end))
      val childComisionTotal = user.childs.map( (u)=>{
        Commision.totalCommsionWithoudDiscount(u, start, end) * (u.percentToParrent/100)
      }).foldLeft(0.00)(_+_)

      r._2(0)(0) match {
         case a:Any => (a.toString.toDouble+childComisionTotal);
         case _ => 0.00
      }    
  }
  def totalCommsion(user:User, start:Date, end:Date):Double = {
    def valueToComision = 100.00 - user.parent_percent.is

    (totalCommsionWithoudDiscount(user, start, end) * (valueToComision/100))
  }

}