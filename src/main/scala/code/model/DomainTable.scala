package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 

class DomainTable extends LongKeyedMapper[DomainTable] with IdPK {
    def getSingleton = DomainTable
    object cod extends MappedPoliteString(this,255)
    object name extends MappedPoliteString(this,255)
    object short_name extends MappedPoliteString(this,20)
    object domain_name extends MappedPoliteString(this,255) 
    object status extends MappedInt(this) {
        override def defaultValue = 1;
    }
    object orderInDomain extends MappedInt(this){
	    override def defaultValue = 1000
	}
} 

object DomainTable extends DomainTable with LongKeyedMetaMapper[DomainTable]{
}

