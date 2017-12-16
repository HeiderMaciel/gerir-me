
package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import json._
import _root_.java.math.MathContext; 

import java.util.Date

//class OffSaleProduct extends LongKeyedMapper[OffSaleProduct] with KeyedMapper[Long, OffSaleProduct] with BaseLongKeyedMapper with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with ActiveInactivable[OffSaleProduct] with CompanyIdable[OffSaleProduct] {
class OffSaleProduct extends Audited[OffSaleProduct] with KeyedMapper[Long, OffSaleProduct] with BaseLongKeyedMapper with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with ActiveInactivable[OffSaleProduct] with CompanyIdable[OffSaleProduct] {
	def getSingleton = OffSaleProduct
	object offsale extends MappedLongForeignKey(this, OffSale)
	object product extends MappedLongForeignKey(this, Product)
	object productType extends MappedLongForeignKey(this, ProductType)
	object productLine extends MappedLongForeignKey(this, ProductLine)
    object indic1 extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0;
    }

    object indic2 extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0;
    }

    object indic3 extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0;
    }

    object indic4 extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0;
    }

    object indic5 extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0;
    }

	object suggestedPrice extends MappedDouble(this)
	object offPrice extends MappedDouble(this)
	object percentOff extends MappedDouble(this)
	object minimum extends MappedInt(this)
	object limitAmount extends MappedInt(this)//Ex:(promoção limitada não de uma venda, mas para por exemplo 100 clientes)
    object delivery_? extends MappedBoolean(this){
    	override def dbColumnName = "delivery"
    }	
    object obs extends MappedPoliteString(this,255)
    object external_id extends MappedPoliteString(this,200)

    def productName : String = {
        if (product != 0) {
          val ac = Product.findByKey(product).get
          ac.name;
        } else {
          ""
        }
    }
    def productTypeName : String = {
        if (productType != 0) {
          val ac = ProductType.findByKey(productType).get
          ac.name;
        } else {
          ""
        }
    }
}

object OffSaleProduct extends OffSaleProduct with LongKeyedMapperPerCompany[OffSaleProduct] with  OnlyCurrentCompany[OffSaleProduct]{
    def offSaleProductPrice (offsale : Long, product : Long, price : BigDecimal) : BigDecimal = {
        val ac = OffSaleProduct.findAllInCompany (
            By (OffSaleProduct.status, 1),
            By (OffSaleProduct.offsale, offsale),
            By (OffSaleProduct.product, product));
        if (ac.length > 0) {
            ac(0).offPrice.is
        } else {
            // retorna o preço do produto que já veio como parm
            price 
        }
    }
}

