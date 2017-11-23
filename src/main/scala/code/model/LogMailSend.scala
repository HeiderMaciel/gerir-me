package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._
import _root_.java.math.MathContext
import net.liftweb.mapper.DB

class LogMailSend extends LongKeyedMapper[LogMailSend] with IdPK with CreatedUpdated with PerCompany {
    def getSingleton = LogMailSend
    object message extends MappedPoliteString(this,40000)
    object subject extends MappedPoliteString(this,255)
    object from extends MappedPoliteString(this,255)
    object to extends MappedPoliteString(this,255)
    object read_? extends MappedBoolean(this){
    	override def dbColumnName = "read"
    	override def defaultValue = false
  	}
    object times extends MappedInt(this) { // quantas vezes foi lido/aberto
        override def defaultValue = 0;
    } 
    object business_pattern extends MappedLongForeignKey(this,Customer) // mateus ver se nao é caro
}

object LogMailSend extends LogMailSend with LongKeyedMapperPerCompany[LogMailSend] with OnlyCurrentCompany[LogMailSend]{
    val SQL_TO_REPORT = """
        select lm.subject,bp.name, lm.to_c,lm.createdat, 
        lm.updatedat, lm.read, lm.times, bp.id from logmailsend lm
        left join business_pattern bp on bp.id = business_pattern
        where lm.company = ? and date (lm.createdat) between ? and ? 
        and lower (subject) like '%s' and %s
        and %s
        order by lm.updatedat desc, bp.name
        """
}
