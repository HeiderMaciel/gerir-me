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

class Product extends ProductMapper[Product]{
    def getSingleton = Product
    override def updateShortName = false
    def alertStock  = 5
    object purchasePrice extends MappedDecimal(this,MathContext.DECIMAL64,2)
    object productClass extends MappedEnum(this,ProductType.Types){
        override def defaultValue = ProductType.Types.Product;
    }
    def isProduct = this.productClass.is == ProductType.Types.Product
    object minStock extends MappedInt(this)

    def currentStock(unit:CompanyUnit):Float = {
        InventoryCurrent.currentStock(this,unit);
    }

    // todas as unidades
    lazy val inventoryTotal = InventoryCurrent.findAll(By(InventoryCurrent.product,this))
    // só as unidades que o usuário tem acesso
    lazy val inventoryUnits = if (AuthUtil.user.isAdmin) {
        InventoryCurrent.findAll(By(InventoryCurrent.product,this))
    } else {
        InventoryCurrent.findAll(By(InventoryCurrent.product,this),
        BySql(" (unit = ? or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
                IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.user.id, AuthUtil.company.id))
    }

    object is_inentory_control_? extends MappedBoolean(this){
        override def defaultValue = true
        override def dbColumnName = "is_inentory_control"
    }

    object allow_negative_inventory_? extends MappedBoolean(this){
        override def defaultValue = true
        override def dbColumnName = "allow_negative_inventory"
    }    

    object is_for_sale_? extends MappedBoolean(this){
        override def defaultValue = true
        override def dbColumnName = "is_for_sale"
    }    
    // gera taxa de serviço sobre este produto tipo 10% do garçom
    object serviceCharge_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "serviceCharge"
    }    

    object unitofMeasure extends MappedLongForeignKey(this, UnitofMeasure){
        var uom = if (AuthUtil.company.appType.isGerirme) {
            2 // kilo para capitao lopes
        } else {
            2 //5 // unidade
        }
        override def defaultValue = uom;
    }
    object size extends MappedPoliteString(this,20)
    object markup extends MappedDecimal(this,MathContext.DECIMAL64,4) {
        override def defaultValue = 0.00
    }

    object measureinUnit extends MappedDecimal(this,MathContext.DECIMAL64,2)

    def isInventoryControl = isProduct  && is_inentory_control_?.is

    def totalCurrentStock = inventoryTotal.map(_.currentStock.is).foldLeft(0.0)(_.toFloat+_.toFloat)

    def currentStock:Float = {
        InventoryCurrent.currentStock(this,AuthUtil.unit);
    }

    def statusStock = {
        currentStock match {
            case (current:Float) if(current < minStock.is) => Product.StockStatus.Bad
            case (current:Float) if(current < minStock.is + alertStock) => Product.StockStatus.Warning
            case _ => Product.StockStatus.Good
        }
    }

    def typeName = typeProduct.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    override def save() = {
        var r = super.save()
        if(this.is_inentory_control_? && 
            InventoryCurrent.countInCompany(
            By(InventoryCurrent.product,this.id)
            )<=0){
            var ac = InventoryCurrent.createInCompany
            ac.product (this.id).
            currentStock (0).
            unit (AuthUtil.unit.id).
            save
        }
        r 
    }
}


object Product extends Product with  ProductMapperMeta[Product]{
    val SQL_SEARCH = """
                select 
                p.name,
                p.id,
                p.external_id,
                p.is_bom,
                COALESCE(uac.price, p.saleprice, 0.00) as price,
                COALESCE(bb.short_name,'') as brandname
                from 
                product p
                left join business_pattern bb on bb.id = p.brand
                left join useractivity uac on(uac.activity = p.id and uac.user_c = ?)
                where
                p.company=?
                and p.productclass in (1,2,3)
                and p.status=1 
                and (p.search_name like ? or p.external_id like ? or p.barcode like ?)
                and (%s)
                order by p.name
                limit 30
                OFFSET ?;
    """
    def findAllForSearch(company:Long, name:String, code:String, barcode:String, startPage:Int, user:Long, where:String = "1=1") = {
        val r = DB.performQuery(SQL_SEARCH.format(where),user::company::name::code::barcode::(startPage*30)::Nil)
        r._2.map(
                (p:List[Any]) => ProductSearch(p(0).toString, p(1).asInstanceOf[Long], 
                    p(2).toString, p(3).asInstanceOf[Boolean],p(4).asInstanceOf[Double], 
                    p(5).toString)
            );
    }
    def findAllPackages = findAllInCompany(By(is_bom_?, true))
    def findAllForSearch(params: QueryParam[Product]*) = {
        //val PreviousDebts = Value("Debitos Anteriores")
        //val CustomerCredits = Value("Compra de Credito")
        super.findAll(By(Product.company, AuthUtil.company) :: ByList(productClass, ProductType.Types.Product::ProductType.Types.PreviousDebts::ProductType.Types.CustomerCredits ::Nil) :: By(Product.status, Product.STATUS_OK) :: 
            params.toList :_*)
    }
/*
    def findAllWithInactive(params: QueryParam[Product]*) :List[Product] = {
        println (" vaiii com inativos =- =- =- =- =- =- =- =- =- =- =- =- " + params)
        findAllInCompany(By(productClass,ProductType.Types.Product)  :: params.toList :_*)
    }
*/

    def findAllActive(params: QueryParam[Product]*): List[Product] = {
        //println (" vaii só ativos - - - - - - - - - - - - -- - - - - - -" + params)
        super.findAll(By(productClass,ProductType.Types.Product) :: By(Product.status, Product.STATUS_OK) :: params.toList :_*)
    }

    override def findAll(params: QueryParam[Product]*): List[Product] = {
        //println (" vaii com lista ativos - - - - - - - - - - - - -- - - - - - -" + params)
        super.findAll(By(productClass,ProductType.Types.Product) :: //By(Product.status, Product.STATUS_OK) :: 
            params.toList :_*)
    }

    override def findAll(): List[Product] = {
        super.findAll(By(productClass,ProductType.Types.Product)//, By(Product.status, Product.STATUS_OK)
            )
    }

    override def count(params: QueryParam[Product]*): Long = {
        super.count(By(productClass,ProductType.Types.Product) :: By(Product.status, Product.STATUS_OK) :: params.toList :_*)
    }

    def unsecureCount(params: QueryParam[Product]*): Long = {
        super.count( params.toList :_*)
    }


    object StockStatus extends Enumeration {
            type StockStatus = Value
            val Good, Warning, Bad = Value
    }

    def findAllForSale = findAllInCompany(By(is_for_sale_?,true),OrderBy(name,Ascending))

    def findAllDiscount = findAllInCompany(By(is_discount_?,true))

    // método usado pela importacao de venda para criar produto
    // permite estoque negativo - pq vai fazer a movimentacao de saida
    def findByName(name:String):Product = {
        Product.findAllInCompany(
        By(Product.name,BusinessRulesUtil.toCamelCase(name.trim))
        ) match {
            case i::Nil => i match {
                case  ip:Product => {
                    ip
                }
            }
            case _ => {
                var p = Product.createInCompany
                p.name(name)
                .allow_negative_inventory_?(true)
                .save
                p
            }
        }    
    }

}

case class ProductSearch(name:String, id:Long, external_id:String, 
    is_bom:Boolean, price:Double, brandname:String){
    def products_bom = Product.findByKey(id).get.products_bom
}

class InsufficientInventoryException(product:Product, unit:CompanyUnit) extends Exception{
    def productName = product.name.is
    def unitName = unit.name.is
}
class InvalidAmountInventoryException extends RuntimeException
class InvalidProductInventoryException extends RuntimeException
class InvalidUnitInventoryException extends RuntimeException