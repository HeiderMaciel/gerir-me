package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 
import _root_.java.util.Date;
import net.liftweb.common.{Box,Full,Empty}
import code.util._

class ProductBOM extends Audited[ProductBOM] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy{
    def getSingleton = ProductBOM
    object product extends MappedLongForeignKey(this,Product)
    object obs extends MappedPoliteString(this,555)
    object qtd extends MappedDecimal(this,MathContext.DECIMAL64,2)
    object product_bom extends MappedLongForeignKey(this,Product)

    object salePrice extends MappedDecimal(this,MathContext.DECIMAL64,2)

    object praceled_? extends MappedBoolean(this){
        // estava false alterei para tru pq no ebelle o normal é pacote
        override def defaultValue = true
        override def dbColumnName = "praceled"
    }

    object discount_of_commision_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "discount_of_commision"
    }    

    object priceZero_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "priceZero"
        // 19/12/2017 - rigel - campo criado para indicar que
        // o preço é zero mesmo - caso de pacote que ganha um
        // item - antes mesmo que salvasse com zero o metodo 
        // price buscava o preço de lista no produto ou serviço
        // isso  era usado tanto no BOM como desconto para não 
        // fixar o preço no serviço e poder altera-lo no produto
        // quanto no pacote com itens zerados ao levar pro caixa 
        // buscava o preço de lista
    }    

    object orderInReport extends MappedInt(this) {
        override def defaultValue = 1
    }

    def listPrice = {
        product_bom.obj.get.salePrice.is
    }
    
    def price = if(salePrice.is > 0){
        salePrice.is
    } else if (priceZero_?) {
        salePrice.is // que neste caso é zero
    } else {
        // retorna o preço do produto ou do serviço
        product_bom.obj.get.salePrice.is
    }
}

object ProductBOM extends ProductBOM with LongKeyedMapperPerCompany[ProductBOM]  with  OnlyCurrentCompany[ProductBOM]{
    def registerProductBOM(product:Product, activity:Activity){
        ProductBOM
            .create
            .company(product.company)
            .product(product.id.is)
            .product_bom(Activity.id.is)
            .praceled_?(true)
            .qtd(10)
            .save
    }     
}

