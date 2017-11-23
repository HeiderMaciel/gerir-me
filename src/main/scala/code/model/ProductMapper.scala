package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import _root_.java.math.MathContext; 
import net.liftweb.common.{Box,Full}

trait ProductMapper[OwnerType <: ProductMapper[OwnerType]] extends Audited[OwnerType] with KeyedMapper[Long, OwnerType] with BaseLongKeyedMapper with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with NameSearchble[OwnerType] with StatusConstants with ActiveInactivable[OwnerType] with Siteble{
  self: OwnerType =>
    val MALE = "M"
    val FEMALE = "F"
    val BOTH = "A"
    object gender extends MappedPoliteString(this,2){
        override def defaultValue = BOTH
    }    
    object external_id extends MappedPoliteString(this,200)
    object barcode extends MappedPoliteString(this,200)
    object salePrice extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0.00
    }
    object averagePrice extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0.00
    }
    object pointsPrice extends MappedCurrency(this) {
        override def defaultValue = 0.00
    }
    object pointsOnBuy extends MappedCurrency(this){//point To sum on by this item
        override def defaultValue = 0.00
    }
    object commisionPrice extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0.00
    }
    object typeProduct extends MappedLongForeignKey(this,ProductType)
    object accountCategory extends MappedLongForeignKey(this,AccountCategory)
    object discountAccountCategory extends MappedLongForeignKey(this,AccountCategory)
    object brand extends MappedLongForeignKey(this,Brand)
    object obs extends MappedPoliteString(this,555)
    object commission extends MappedDecimal(this,MathContext.DECIMAL64,4) {
        override def defaultValue = 0.00
    }
    object commissionAbs extends MappedCurrency(this) {
        override def defaultValue = 0.00
    }

    object auxPrice extends MappedCurrency(this) {// valor pago ao assistente
        override def defaultValue = 0.00
    }   
    object auxHousePrice extends MappedCurrency(this) {// valor pago ao assistente pela casa
        override def defaultValue = 0.00
    }   

    object auxPercent extends MappedDecimal(this,MathContext.DECIMAL64,4) {// percentual pago ao assistente
        override def defaultValue = 0.00
    }   
    object auxHousePercent extends MappedDecimal(this,MathContext.DECIMAL64,4) {// percentual pago ao assistente pela casa
        override def defaultValue = 0.00
    }   

    object showInCommand_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "showincommad"
    }

    object orderInCommand extends MappedInt(this)
    object is_discount_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "is_discount"
    }

    // qdo o cliente agenda ele proprio on line
    object showInCalendarPub_? extends MappedBoolean(this){
        override def dbColumnName = "showInCalendarPub"
        override def defaultValue = true
    }

    object allowSaleByUser_? extends MappedBoolean(this){
        override def defaultValue = true
        override def dbColumnName = "allowsalebyuser"
    }    
    object is_bom_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "is_bom"
    }
    object costCenter extends MappedLongForeignKey(this.asInstanceOf[MapperType], CostCenter) {
        override def dbIndexed_? = true
    }

    object color extends MappedPoliteString(this, 55)
    object unit extends MappedLongForeignKey(this,CompanyUnit) 
    object bpmCount extends MappedInt (this) {
        // mensal bimestral tri -etc para mensalidades
        override def defaultValue = 1;
    }

    def products_bom = if(this.is_bom_?.is) {
        ProductBOM.findAll(By(ProductBOM.product,this.id.is)).filter(_.product_bom.is != this.id.is)
    }else{
        Nil
    }

    def discounts = if(this.is_bom_?.is) {
        ProductBOM.findAll(By(ProductBOM.product,this.id.is), By(ProductBOM.discount_of_commision_?,true))
    }else{
        Nil
    }
    def discountsTotal = {
        discounts.map((p)=>p.product_bom.obj.get.salePrice.toDouble).foldLeft(0.0)(_+_)
    }
    def lines = ProductLineTag.findAll(By(ProductLineTag.product,this.id.is))

    def clearLines = lines.foreach(_.delete_!)

    def lineIds = lines.map(_.line.is)

    def lines_text = lineIds.map(_.toString).foldLeft("0,")(_+","+_)
 
    /*
    Se for fazer outro levar para o nome searchable trai
    tem accountcategory e productmapper já
    */
    def reorgMigration = {
        val product = Product.findAllInCompany 
        var i = 1;
        product.foreach((c)=>{
            //val n1 = this.name.is
            c.name.set (BusinessRulesUtil.convertChars(c.name));
            println ("vaiii ================ product name === " + c.name)
            if (c.testIfDuplicatedName (c.company, c.id, c.name)) {
                c.name (c.name + " " + i.toString)
                i = i + 1;
            }
            c.obs (c.obs+" ")
            c.save        
        })
        val activity = Activity.findAllInCompany 
        i = 1;
        activity.foreach((c)=>{
            c.name.set (BusinessRulesUtil.convertChars(c.name));
            println ("vaiii ================ activity name === " + c.name)
            if (c.testIfDuplicatedName (c.company, c.id, c.name)) {
                c.name (c.name + " " + i.toString)
                i = i + 1;
            }
            c.obs (c.obs+" ")
            c.save        
        })
    }


    override def save() = {
        if(this.external_id.is != "" && getSingleton.asInstanceOf[OnlyCurrentCompany[OwnerType]].countInCompany(NotBy(self.id,this.id),By(self.external_id,this.external_id))>0){
            throw new RuntimeException("Já existe um produto com o código externo %s".format(this.external_id.is))
        }
/*
        val n1 = this.name.is
        this.name.set (BusinessRulesUtil.convertChars(n1));
*/
        super.save()
    }
    override def delete_! = {
        if(TreatmentDetail.count(By(TreatmentDetail.product, this.id)) > 0){
            throw new RuntimeException("Existe atendimento com este serviço/produto!")
        }

        if(ProductBOM.count(By(ProductBOM.product, this.id)) > 0){
            throw new RuntimeException("Existe item de pacote para este serviço/produto!")
        }

        if(ProductBOM.count(By(ProductBOM.product_bom, this.id)) > 0){
            throw new RuntimeException("Este serviço/produto é item de pacote!")
        }

        if(TreatmentDetail.count(By(TreatmentDetail.activity, this.id)) > 0){
            throw new RuntimeException("Existe atendimento com este serviço/produto!")
        }      
        if(UserActivity.count(By(UserActivity.activity, this.id)) > 0){
            throw new RuntimeException("Existe associação com profissional para este serviço/produto!")
        }      
        if(InventoryCurrent.count(By(InventoryCurrent.product, this.id)) > 0){
            throw new RuntimeException("Existe controle de estoque para este produto!")
        }
        if(InventoryMovement.count(By(InventoryMovement.product, this.id)) > 0){
            throw new RuntimeException("Exite movimentação de estoque para este produto!")
        }
        super.delete_!
    } 
    def imagePath = "product"
}

trait ProductMapperMeta[A <: LongKeyedMapper[A]] extends LongKeyedMapperPerCompany[A] with OnlyActive[A]{ 
	self: A with OnlyActive[A] with ActiveInactivable[A] with PerCompany with MetaMapper[A] =>
    override def dbTableName = "product"
}