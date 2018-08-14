package code
package model 

import code.actors._
import code.service._
import net.liftweb._ 
import mapper._ 
import net.liftweb.common._
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 
import net.liftweb.widgets.gravatar.Gravatar
import code.util._
import _root_.java.util.Calendar
import _root_.java.util.Date
//Class de cliente

//class ImageCustomer extends LongKeyedMapper[ImageCustomer] with IdPK with CreatedUpdated with CreatedUpdatedBy  with PerCompany with WithCustomer {
class ImageCustomer extends Audited[ImageCustomer] 
with IdPK 
with CreatedUpdated 
with CreatedUpdatedBy  
with LogicalDelete[ImageCustomer]
with PerCompany 
with Imageble 
with WithCustomer {
    def getSingleton = ImageCustomer 
 
 //   object image extends MappedPoliteString(this,455)
 //   object thumb extends MappedPoliteString(this,455)

    def imagePath = "imagecustomer"

}

object ImageCustomer  extends ImageCustomer with LongKeyedMapperPerCompany[ImageCustomer] with OnlyCurrentCompany[ImageCustomer]
