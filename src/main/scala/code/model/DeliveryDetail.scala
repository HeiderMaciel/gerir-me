package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 
import net.liftweb.common.{Box,Full,Empty}
import code.util._

class DeliveryDetail extends LongKeyedMapper[DeliveryDetail] with IdPK with CreatedUpdated  with CreatedUpdatedBy with PerCompany with WithCustomer{
    def getSingleton = DeliveryDetail
    object delivery extends MappedLongForeignKey(this,DeliveryControl)
    object product extends MappedLongForeignKey(this,Product)
    object price extends MappedDecimal(this,MathContext.DECIMAL64,2)
    object treatmentDetail extends MappedLongForeignKey(this,TreatmentDetail)
    object paymentDetail extends MappedLongForeignKey(this,PaymentDetail)
    object used_? extends MappedBoolean(this){
        override def dbColumnName = "used"
        override def dbIndexed_? = true
    }
    object efetivedate extends EbMappedDate(this)

    def command = treatmentDetail.obj match {
        case Full(td:TreatmentDetail) => td.command
        case _ => ""
    }

    def findPriceByCustomerProduct(customer:Customer, product:Product) = {
        // aqui nao pode ser findAllInCompany pq este método
        // é usado na fila de proc comissão e lá a empresa
        // não é setada
        val dd = DeliveryDetail.findAll(
            By(DeliveryDetail.customer, customer), 
            By(DeliveryDetail.product, product), 
            By(DeliveryDetail.used_?, false),
            OrderBy(DeliveryDetail.createdAt, Ascending) // pra o antigo vir primeiro
            )
        dd
    }

}

object DeliveryDetail extends DeliveryDetail with LongKeyedMapperPerCompany[DeliveryDetail] with  OnlyCurrentCompany[DeliveryDetail]{
    def findByCustomer(customer:Customer) = {
        DeliveryDetail.findAllInCompany(
            By(DeliveryDetail.customer, customer), 
            By(DeliveryDetail.used_?, false),
            OrderBy(DeliveryDetail.createdAt, Ascending) // pra o antigo vir primeiro
            )
    }
}
