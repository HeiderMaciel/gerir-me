package code
package model 

/*
import net.liftweb._ 
import net.liftweb.common.{Box,Full,Empty}
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 
import java.util.Date;
*/
import net.liftweb.common.{Box,Full,Empty}
import net.liftweb._ 
import mapper._
//import code.model._
import http._ 
import SHtml._ 
import util._ 
import code.util._
import _root_.java.math.MathContext; 
//import code.actors._
import java.util.Date
//import java.util.Calendar
//import net.liftweb.json._
//import net.liftweb.common._
//import net.liftweb.util._
import http.js._
//import JE._

class TreatmentDetail extends Audited[TreatmentDetail] with IdPK with CreatedUpdated with CreatedUpdatedBy with PerCompany with net.liftweb.common.Logger{
    def getSingleton = TreatmentDetail
    def maxConflictsAllowed = {
         activity.obj match {
            case Full(a) => a.maxConflictsAllowed
            case _ => 1
        }
    }
/*  tava duplicado com isamonthly e sem uso
    def bpmonthly_? = activity.obj match {
        case Full(a) => a.bpmonthly_?.is
        case _ => false
    }
*/
    object treatment extends MappedLongForeignKey(this,Treatment)
    object offsale extends MappedLongForeignKey(this,OffSale)
    object parentBom extends MappedLong(this)
    object price extends MappedDecimal(this,MathContext.DECIMAL64,2)
    object amount extends MappedDecimal(this,MathContext.DECIMAL64,2){
        override def defaultValue = 1l
    }
    object obs extends MappedString(this, 4000)
    
    object for_delivery_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "for_delivery"
    }

    object activity extends MappedLongForeignKey(this,Activity){
        override def dbIndexed_? = true
    }

    override def delete_! = {
        treatment.obj match {
            case Full(t)  if(t.isPaid)=> { throw new RuntimeException(" Não é permitido excluir atendimento pago!") }
            case _ => 
        } 
        // deletar TdEpet tdEdoctus
        super.delete_!
    }

    object product extends MappedLongForeignKey(this,Product){
        override def dbIndexed_? = true
    }

    object auxiliar extends MappedLongForeignKey(this,User){
        override def dbIndexed_? = true
    }
    object auxPrice extends MappedCurrency(this)
    object external_id extends MappedPoliteString(this,200)

    def hasAuxiliar = auxiliar.obj  match {
        case Full(a) => true
        case _ => false
    }

    def hasOffSale = offsale.obj  match {
        case Full(a) => true
        case _ => false
    }
    
    def customer = treatment.obj.get.customer.obj.get
    def command = treatment.obj.get.command.is

    def userName = {
        treatment.obj match {
            case Full(t) => t.userName
            case _ => ""
        }
        
    }
    def unitShortName = {
        treatment.obj match {
            case Full(t) => t.unitShortName
            case _ => ""
        }
        
    }
    // rigel 28/07/2014
    def auxiliarShortName:String = {
        auxiliar.obj match {
            case Full(u)=> u.short_name.is
            case _ => ""
        }
    }

    def offsaleShortName:String = {
        offsale.obj match {
            case Full(u)=> u.short_name.is
            case _ => ""
        }
    }

    def animalShortName:String = {
        if (AuthUtil.company.appType.isEbellepet) {
            getTdEpet.animal.obj match {
                case Full(u)=> u.short_name.is
                case _ => ""
            }
        } else {
            ""
        }
    }

    def animal:Long = {
        if (AuthUtil.company.appType.isEbellepet) {
            getTdEpet.animal.obj match {
                case Full(u)=> u.id.is
                case _ => 0
            }
        } else { 
            0
        }
    }

    def tooth:String = {
        if (AuthUtil.company.appType.isEsmile) {
            getTdEdoctus.tooth
        } else { 
            ""
        }
    }

    def status = {
        treatment.obj match {
            case Full(t) => t.status.is
            case _ => ""
        }
        
    }    

    def start = treatment.obj  match {
            case Full(t) => t.start.is
            case _ => new Date
    }

    def dateEvent = treatment.obj  match {
            case Full(t) => t.dateEvent.is
            case _ => new Date
    }
    
    def end = treatment.obj  match {
            case Full(t) => t.end.is
            case _ => new Date
    }

    def alertObs : String = {
         activity.obj match {
            case Full(a) => if (a.showObs_?) {
                    a.obs
                } else {
                    ""
                }
            case _ => product.obj match {
                case Full(p) => ""
                case _ => ""
            }
        }        
    }

    def discountsTotal:Double  = {
         activity.obj match {
            case Full(a) => a.discountsTotal
            case _ => product.obj match {
                case Full(p) => p.discountsTotal
                case _ => 0.0
            }
        }        
    }

    def categoryByType (category:AccountCategory) = {
         activity.obj match {
            case Full(a) => if (a.accountCategory.obj.isEmpty) {
                    println ("vaiiiii =============== Serviço " + a.name.is + " não tem categoria associada")
                    //throw new RuntimeException("Serviço " + a.name.is + " não tem categoria associada.\n\nSe o parâmetro <Fatura por Categoria> em configurações estiver marcado, todo produto/serviço vendido deve ter uma categoria parametrizada para geração no financeiro" )
                    category
                } else {
                    a.accountCategory.obj.get
                }
            case _ => product.obj match {
                case Full(p) => if (p.accountCategory.obj.isEmpty) {
                    category
                } else {
                    p.accountCategory.obj.get
                }
                case _ => null
            }
        }        
    }

    def discountCategoryByType (category:AccountCategory) = {
         activity.obj match {
            case Full(a) => if (a.discountAccountCategory.obj.isEmpty) {
                    category
                } else {
                    a.discountAccountCategory.obj.get 
                }
            case _ => product.obj match {
                case Full(p) => if (p.discountAccountCategory.obj.isEmpty) {
                    category
                } else {
                    p.discountAccountCategory.obj.get
                }

                case _ => null
            }
        }        
    }

    def activityType:String = {
         activity.obj match {
            case Full(a) => "activity"
            case _ => product.obj match {
                case Full(p) => "product"
                case _ => ""
            }
            
        }        
    }

    def activity_id:Long = {
         activity.obj match {
            case Full(a) => a.id.is
            case _ => product.obj match {
                case Full(p) => p.id.is
                case _ => 0
            }  
        }
    }
    def productBase = Product.findByKey(activity_id).get
    def isPreviousDebts = {
        try{
            productBase.productClass.is == ProductType.Types.PreviousDebts
        }catch{
            case _ => false
        }
    }

    def isInvoiceGroup (ig:Long)= {
        activity.obj match {
            case Full(a) => 
                // println ("=== serv " + a.name)
                a.typeProduct.obj match {
                    case Full(b) => 
                        if (b.invoiceGroup == ig) {
                            true
                        } else {
                            false
                        }
                    case _ => false
                }
            case _ => 
                product.obj match {
                    case Full(a) => 
                        //println ("=== prod " + a.name)
                        a.typeProduct.obj match {
                            case Full(b) =>
                                if (b.invoiceGroup == ig) {
                                    true
                                } else {
                                    false
                                }
                            case _ => false
                        }
                    case _ => false
                }
        }
    }

    def isService = {
        activity.obj match {
            case Full(a) => true
            case _ => false
        }
    }
    def isProduct = {
//        !isService
        product.obj match {
            case Full(a) => true
            case _ => false
        }
    }
    def nameActivity:String = {
    	 activity.obj match {
            case Full(a) => a.name.is
            case _ => product.obj match {
                case Full(p) => p.name.is
                case _ => ""
            }  
        }
    }

    def whereIs:String = {
         activity.obj match {
            case Full(a) => if (a.outsideService_?) {
                customer.full_address
                } else {
                ""
                }
            case _ => "Sem atividade"  
        }
    }

    def superiorCommissionValue (percent: Double) = {
        (percent * priceToCommission / 100).toDouble              
/*        println ("vaiiiii ========================== " + percent + 
            " ==== " + commissionActivity + " ====== " + commissionAbsActivity)
        (percent * (commissionActivity + commissionAbsActivity) / 100).toDouble              
*/
    }

    def auxiliarCommissionValue = activity.obj match {
            case Full(t) => if(hasAuxiliar) {
                    //t.auxiliarCommissionValue
                    (commissionAbsActivityAuxiliar +
                    (commissionActivityAuxiliar * priceToCommission / 100)).toDouble              
                }else{
                    0.0
                }
            case _ => 0.00
    }

    def auxiliarHouseCommissionValue = activity.obj match {
            case Full(t) => if(hasAuxiliar) {
                    //t.auxiliarHouseCommissionValue
                    (commissionAbsActivityAuxiliarHouse +
                    (commissionActivityAuxiliarHouse * priceToCommission / 100)).toDouble              
                }else{
                    0.0
                }
            case _ => 0.00
    }

    def priceToCommission = activity.obj match {
            case Full(t) => price.is - t.discountsTotal - (price.is / 100.0 * t.discountPercent) - t.discountPrice.is.toDouble
            case _ => price.is
    }

    lazy val hasPointsOnBuy = pointsOnBuy > 0

    lazy val isAMonthlyService:Boolean = activity.obj match {
        case Full(t) => t.bpmonthly_?.is
        case _ => false
    }

    lazy val isAOffSaleService:Boolean = !(offsale == Empty || offsale == null)

    lazy val pointsOnBuy:Double = {
        activity.obj match {
            case Full(t) => t.pointsOnBuy.is
            case _ => product.obj match {
                case Full(p) => p.pointsOnBuy.is
                case _ => 0.0
            }
        }
    }

    def priceActivity:BigDecimal = {
        activity.obj match {
            case Full(t) => {
                            if(isAMonthlyService && BpMonthly.countBpMonthlyByProduct(t, customer, start) > 0){
                                BigDecimal(BpMonthly.monthlyByProduct(t, 
                                    customer, start).valueSession.is)
                            } else if(hasOffSale) {
                                OffSaleProduct.offSaleProductPrice (offsale.obj.get.id, 
                                    activity.obj.get.id.is, t.salePrice.is)
                            }else  if(hasUser) {
                                user.activityPrice(t)
                            } else {
                                t.salePrice.is
                            }
                }
            case _ => product.obj match {
                case Full(p) => {
                            if(hasOffSale) {
                                OffSaleProduct.offSaleProductPrice (offsale.obj.get.id, 
                                    product.obj.get.id.is, p.salePrice.is)
                            } else if(hasUser) {
                                user.activityPrice(p)
                            } else {
                                p.salePrice.is
                            }
                    }
                case _ => 0.0
            }
        }
    }

    def unit_price = (this.price.is / this.amount.is).toDouble
    def user = treatment.obj.get.user.obj.get
    def hasUser = treatment.obj.get.user.obj match {
        case Full(u) => true
        case _ => false
    }

    def commissionActivity:BigDecimal = {
        activity.obj match {
            case Full(t) => user.activityCommission(t)
            case _ => product.obj match {
                case Full(p) => user.activityCommission(p)//p.commission.is
                case _ => 0.0
            }
        }
    }    

    def commissionAbsActivity:BigDecimal = {
        activity.obj match {
            case Full(t) => user.activityCommissionAbs(t)
            case _ => product.obj match {
                case Full(p) => user.activityCommissionAbs(p)
                case _ => 0.0
            }
        }
    }    

    def commissionActivityAuxiliar:BigDecimal = {
        activity.obj match {
            //case Full(t) => auxiliar.obj.get.activityAuxPercent(t)
            case Full(t) => user.activityAuxPercent(t)
            case _ => 0.00
        }
    }    
    def commissionAbsActivityAuxiliar:BigDecimal = {
        activity.obj match {
            case Full(t) => user.activityAuxPrice(t)
            case _ => 0.00
        }
    }    
    def commissionActivityAuxiliarHouse:BigDecimal = {
        activity.obj match {
            case Full(t) => user.activityAuxHousePercent(t)
            case _ => 0.00
        }
    }    
    def commissionAbsActivityAuxiliarHouse:BigDecimal = {
        activity.obj match {
            case Full(t) => user.activityAuxHousePrice(t)
            case _ => 0.00
        }
    }    

    def duration:String = {
        activity.obj match {
            case Full(t) => user.activityDuration(t)
            case _ => "00:00"
        }
    }
    def valueToUser:BigDecimal = {
        DB.performQuery("select sum(value) from commision where treatment_detail=?", List(this.id.is))._2(0)(0) match {
                   case a:Any => BigDecimal(a.toString)
                   case _ => BigDecimal(0)
            }        
    }

    def percentInTotal = {
      ((BigDecimal(100.00) * price.is) / treatment.obj.get.totalValue(0))
    }

    def revertPrice = {
        this.price(priceActivity * this.amount.is).save
    }
    def validateTreatment {
        if(treatment.obj.get.isPaid){
            throw new RuntimeException("Este atendimento já foi pago, para alterá-lo exclua o pagamento e tente novamente!");
        }
        if(treatment.obj.get.company != company){
            throw new RuntimeException("Este atendimento é de uma empresa diferente do detalhe");
        }
    }
    override def save() = {
        validateTreatment
        if (AuthUtil.company.appType.isEbellepet) {
            if (this.id > 0) {
                getTdEpet.save
            }
        }
        if (AuthUtil.company.appType.isEsmile || AuthUtil.company.appType.isEdoctus) {
            // edoctus tb seria necessário para o tiss
            if (this.id > 0) {
                getTdEdoctus.save
            }
        }
        super.save()
    }    

    lazy val getTdEpet: TdEpet = {
        if (TdEpet.count (By (TdEpet.treatmentDetail,this.id)) > 0) {
            TdEpet.findAllInCompany (By (TdEpet.treatmentDetail,this.id))(0)
        } else {
            val tdEpet = TdEpet.createInCompany.treatmentDetail(this.id)
            tdEpet
        }
    }

    lazy val getTdEdoctus: TdEdoctus = {
        if (TdEdoctus.count (By (TdEdoctus.treatmentDetail,this.id)) > 0) {
            TdEdoctus.findAllInCompany (By (TdEdoctus.treatmentDetail,this.id))(0)
        } else {
            val tdEdoctus = TdEdoctus.createInCompany.treatmentDetail(this.id)
            tdEdoctus
        }
    }


    def toXmlTiss = {
       def descricaoProcedimento = if (isService) {
            this.activity.obj.get.search_name.toUpperCase
        } else if (isProduct) {
            this.product.obj.get.search_name.toUpperCase
        } else {
            " sem produto e servico "
        }
        if(this.external_id == ""){
          println ("************************* código vazio " + descricaoProcedimento + " " + this.treatment.obj.get.customer.obj.get.name.is)
          throw new RuntimeException(descricaoProcedimento + " sem código informado " + this.treatment.obj.get.customer.obj.get.name.is)
        }
       var strXml:String ="""
        <ans:procedimentoExecutado>
            <ans:dataExecucao>""" + Project.dateToDb(this.dateEvent) + """</ans:dataExecucao>
            <ans:horaInicial>""" + Project.dateToHourss(this.start) + """</ans:horaInicial>
            <ans:horaFinal>"""+ Project.dateToHourss(this.end) +"""</ans:horaFinal>
            <ans:procedimento>
                <ans:codigoTabela>98</ans:codigoTabela>
                <ans:codigoProcedimento>""" + this.external_id + """</ans:codigoProcedimento>
                <ans:descricaoProcedimento>""" + descricaoProcedimento +
                """</ans:descricaoProcedimento>
            </ans:procedimento>
            <ans:quantidadeExecutada> """ + this.amount.toInt + """</ans:quantidadeExecutada>
            <ans:viaAcesso>1</ans:viaAcesso>
            <ans:tecnicaUtilizada>2</ans:tecnicaUtilizada>
            <ans:reducaoAcrescimo>0</ans:reducaoAcrescimo>
            <ans:valorUnitario>""" + ("%.2f".format (this.price/this.amount)).replace(",",".") + """</ans:valorUnitario>
            <ans:valorTotal>""" + this.price + """</ans:valorTotal> """ +
            this.treatment.obj.get.user.obj.get.toXmlTissEquipe (1)
            if (hasAuxiliar) {
                strXml = strXml + this.auxiliar.obj.get.toXmlTissEquipe (1)
            }
            strXml = strXml +  """       
        </ans:procedimentoExecutado>        """
        strXml
    }

}

object TreatmentDetail extends TreatmentDetail with LongKeyedMapperPerCompany[TreatmentDetail] with OnlyCurrentCompany[TreatmentDetail]{
    
}
