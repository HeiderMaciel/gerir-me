package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.service._
import java.util.Calendar
import net.liftweb.common.{Box,Full}

class UserCompanyUnit extends Audited[UserCompanyUnit] with PerCompany with IdPK 
    with CreatedUpdated with CreatedUpdatedBy 
    with ActiveInactivable[UserCompanyUnit] {
    def getSingleton = UserCompanyUnit
    object user extends MappedLong(this)
    object unit extends MappedLongForeignKey(this,CompanyUnit) 
    object groupPermission extends MappedPoliteString(this,200){
        override def defaultValue = ""
    }
    object obs extends MappedPoliteString(this,255)
    object showInCalendar_? extends MappedBoolean(this){
        override def dbColumnName = "showInCalendar"
        override def defaultValue = true
    }
}

object UserCompanyUnit extends UserCompanyUnit with LongKeyedMapperPerCompany[UserCompanyUnit] with OnlyCurrentCompany[UserCompanyUnit]{
    //override def dbTableName = "bpaccount"
}