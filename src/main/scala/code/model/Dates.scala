package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 

class Dates extends LongKeyedMapper[Dates] with IdPK {
    def getSingleton = Dates
    object date_c extends EbMappedDate(this)
    object day extends MappedLong(this)
    object dow_number extends MappedLong(this)
    object short_name_year extends MappedPoliteString(this, 20)
    object short_name_dow extends MappedPoliteString(this, 20)
} 

object Dates extends Dates with LongKeyedMetaMapper[Dates]{
/*
	def findAllToSiteWithUnit = {
		findAll(
			BySql(" id in (select distinct up.cityref from companyunit u inner join business_pattern up on(u.partner = up.id)  where u.allowshowonsite=true)",IHaveValidatedThisSQL("",""))
		)
	}
*/
}

