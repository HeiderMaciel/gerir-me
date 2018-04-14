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


class InvoiceGroup extends Audited[InvoiceGroup] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with NameSearchble[InvoiceGroup]{ 
    def getSingleton = InvoiceGroup
    override def updateShortName = false
    object obs extends MappedPoliteString(this,255)
    object external_id extends MappedPoliteString(this,200)    
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
    object xmlTissTag extends MappedPoliteString(this,200)    

//    def activitys = Activity.findAllInCompany(By(Activity.typeProduct,this))
    
    override def delete_! = {
        if(ProductType.count(By(ProductType.invoiceGroup,this.id)) > 0){
            throw new RuntimeException("Existe Tipo de produto/serviço com este grupo de faturamento! ")
        }
        super.delete_!
    }
}

object InvoiceGroup extends InvoiceGroup with LongKeyedMapperPerCompany[InvoiceGroup]  with  OnlyCurrentCompany[InvoiceGroup]{
    override def dbTableName = "InvoiceGroup"
  
/*
    def findAllService = findAllInCompany(By(InvoiceGroup.typeClass,InvoiceGroup.Types.Service),OrderBy(InvoiceGroup.name, Ascending))
//    def findAllProducts = findAllInCompany(By(InvoiceGroup.typeClass,InvoiceGroup.Types.Product),OrderBy(InvoiceGroup.name, Ascending))

    def createTypeService  = create.typeClass(InvoiceGroup.Types.Service)

    def createTypeProduct  = create.typeClass(InvoiceGroup.Types.Product)

    def findAllProduct = findAllInCompany(By(InvoiceGroup.typeClass,InvoiceGroup.Types.Product),OrderBy(InvoiceGroup.name, Ascending))
*/
}

