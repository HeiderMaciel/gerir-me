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


class ProductType extends Audited[ProductType] 
    with PerCompany 
    with IdPK 
    with CreatedUpdated 
    with CreatedUpdatedBy 
    with ActiveInactivable[ProductType]
    with NameSearchble[ProductType]{ 
    def getSingleton = ProductType
    override def updateShortName = false
    object obs extends MappedPoliteString(this,255)
    // expande obs no treatment - como alerta
    object showObs_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "showobs"
    }
    object external_id extends MappedPoliteString(this,200)    
    object typeClass extends MappedEnum(this,ProductType.Types){
        override def defaultValue = ProductType.Types.Product;
    }
    
    object color extends MappedPoliteString(this, 55) with LifecycleCallbacks {
      override def defaultValue = "";
      override def beforeSave() {
          super.beforeSave;
          // não deixa setar branco pq é o default do color picker
          if(this.get == "#FFFFFF"){ 
            this.set("")
          }
      } 
    }

    object showInCommand_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "showincommad"
    }

    object invoiceGroup extends MappedLongForeignKey(this, InvoiceGroup)

    def invoiceGroupName = invoiceGroup.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def activitys = Activity.findAllInCompany(By(Activity.typeProduct,this))
    
    override def delete_! = {
        if(Product.unsecureCount(By(Product.typeProduct,this.id)) > 0){
            throw new RuntimeException("Existe produto/serviço com este tipo! ")
        }
        super.delete_!
    }
}

object ProductType extends ProductType with LongKeyedMapperPerCompany[ProductType]  with  OnlyCurrentCompany[ProductType]{
    override def dbTableName = "producttype"
    object Types extends Enumeration {
        type Types = Value
        val Service = Value("Serviço")
        val Product = Value("Produto")
        val PreviousDebts = Value("Debitos Anteriores")
        val CustomerCredits = Value("Compra de Credito")

    } 

//    def findAllService = findAllInCompany(By(ProductType.typeClass,ProductType.Types.Service),OrderBy(ProductType.name, Ascending))
//    def findAllProducts = findAllInCompany(By(ProductType.typeClass,ProductType.Types.Product),OrderBy(ProductType.name, Ascending))

    def createTypeService  = create.typeClass(ProductType.Types.Service)

    def createTypeProduct  = create.typeClass(ProductType.Types.Product)

//    def findAllProduct = findAllInCompany(By(ProductType.typeClass,ProductType.Types.Product),OrderBy(ProductType.name, Ascending))
}