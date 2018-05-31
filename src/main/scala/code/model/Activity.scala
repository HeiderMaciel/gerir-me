package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.service._
import _root_.java.math.MathContext; 
import net.liftweb.common.{Box,Full,Empty}
class Activity extends ProductMapper[Activity] with Ordered[Activity] {
    def getSingleton = Activity 
    override def updateShortName = false
    
    object duration extends MappedPoliteString(this,10){
        override def defaultValue = if (AuthUtil.company.appType.isEphysio) {
            "00:60"
        } else {
            "00:30"
        }
    }

    object allowSimultaneos_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "allowSimultaneos"
    }    
    object productClass extends MappedEnum(this,ProductType.Types){
        override def defaultValue = ProductType.Types.Service;
    }

    object usernotification_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "usernotification"
    }    

    object customernotification_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "customernotification"
    }    
    def maxConflictsAllowed = conflictsallowed.is
    object conflictsallowed extends MappedInt (this){
        override def defaultValue = if (AuthUtil.company.appType.isEphysio) {
            2
        } else {
            1 // antes era zero melhor abaixar se cliente quiser que ficar dando msg
        }
    }
    object bpmonthly_? extends MappedBoolean(this){
        override def defaultValue = if (AuthUtil.company.appType.isEphysio) {
            true
        } else {
            false
        }
        override def dbColumnName = "bpmonthly"
    }    
    object outsideService_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "outsideservice"
    }
    object crmService_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "crmservice"
    }
    object showInRecords_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "showinrecords"
    }
    // expande obs no treatment - como alerta
    object showObs_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "showobs"
    }
/*
    object auxPrice extends MappedCurrency(this) // valor pago ao assistente
    object auxHousePrice extends MappedCurrency(this) // valor pago ao assistente pela casa

    object auxPercent extends MappedDecimal(this,MathContext.DECIMAL64,4) // percentual pago ao assistente
    object auxHousePercent extends MappedDecimal(this,MathContext.DECIMAL64,4) // percentual pago ao assistente pela casa
*/
    object discountPrice extends MappedCurrency(this) // valor absoluto de desconto no preço para calcular comissão
    object discountPercent extends MappedDecimal(this,MathContext.DECIMAL64,4) // desconto percentul mo preço pra calcular comissão
    object discountPercLimit extends MappedDecimal(this,MathContext.DECIMAL64,4) // percentual de desconto no caixa

    // usado para agenda publica na verificação de outros agendamentos 
    // em horarios pra fernte do desejado
    def durationMin:Long = {
        val h = duration.is.slice(0,2).toLong * 60
        val m = duration.is.slice(3,5).toLong
        return (h+m)
    }

/*
    def auxiliarCommissionValue = if(auxPrice.is.toDouble != 0) {
            auxPrice.is.toDouble
        }else{
            (auxPercent.is.toDouble / 100) * salePrice.is.toDouble
        }

    def auxiliarHouseCommissionValue = if(auxHousePrice.is.toDouble != 0) {
            auxHousePrice.is.toDouble
        }else{
            (auxHousePercent.is.toDouble / 100) * salePrice.is.toDouble
        }
*/
  
    class MyServiceProductClass extends MappedEnum(this,ProductType.Types){
        override def defaultValue = ProductType.Types.Service;
        override def dbIndexed_? = true
    }

    override def delete_! = {
/*
        if(UserActivity.count(By(UserActivity.activity,this.id)) > 0){
            throw new RuntimeException("Existe associação com profissional para esse serviço! ")
        }

        if(TreatmentDetail.count(By(TreatmentDetail.activity,this.id)) > 0){
            throw new RuntimeException("Existe atendimento para esse serviço! ")
        }
*/
        super.delete_!
    }
    override def save() = {
        if(this.duration.is == "" ){
            throw new RuntimeException("Não é possível salvar serviço sem duração")
        }
        super.save()
    }
    def typeActivityName = typeProduct.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }
    def compare(that: Activity) = name.is.compare(that.name.is)

    def discounts_text:String = products_bom.map((p)=>p.product_bom.is.toString).foldLeft("")(_+","+_)

    def clearBons = products_bom.foreach(_.delete_!)

    def colorId = <a style={"background-color:"+color.is+";"} 
    href={"/activity/edit?id="+id.is.toString} target='_activity_maste'>{id.is.toString}</a>
} 

object Activity extends Activity with ProductMapperMeta[Activity] {
    
    def findAllActive(params: QueryParam[Activity]*): List[Activity] = {
        //println (" vaii só ativos 1111 - - - - - - - - - - - - -- - - - - - -" + params)
        super.findAll(By(productClass,ProductType.Types.Service) :: By(Activity.status, Activity.STATUS_OK) :: 
            params.toList :_*)
    }

    override def findAll(params: QueryParam[Activity]*): List[Activity] = {
        //println (" vaii só ativos 1111 - - - - - - - - - - - - -- - - - - - -" + params)
        super.findAll(By(productClass,ProductType.Types.Service) :: //By(Activity.status, Activity.STATUS_OK) :: 
            params.toList :_*)
    }

    override def findAll(): List[Activity] = {
        //println (" vaii só ativos 2222 - - - - - - - - - - - - -- - - - - - -")
        super.findAll(By(productClass,ProductType.Types.Service), By(Activity.status, Activity.STATUS_OK))
    }    
    override def count(params: QueryParam[Activity]*): Long = {
        super.count(By(productClass,ProductType.Types.Service) :: params.toList :_*)
    }    

    object StockStatus extends Enumeration {
            type StockStatus = Value
            val Good, Warning, Bad = Value
    }      
}
