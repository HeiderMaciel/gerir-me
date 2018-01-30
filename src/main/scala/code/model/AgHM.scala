package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 

class AgHM extends LongKeyedMapper[AgHM] with IdPK {
    def getSingleton = AgHM 
    object agh extends MappedInt (this);
    object agm extends MappedInt (this);
} 

object AgHM extends AgHM  with LongKeyedMetaMapper[AgHM]{

}

