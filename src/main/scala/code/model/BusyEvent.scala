
package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._
import _root_.java.math.MathContext
import net.liftweb.mapper.DB
import java.util.Date;
import net.liftweb.json._
import http.js._
import JE._
import net.liftweb.common._
import code.util._


class BusyEvent extends  UserEvent 
with Audited[BusyEvent] 
//with LogicalDelete[BusyEvent] 
with IdPK 
with CreatedUpdated with CreatedUpdatedBy 
with PerCompany with PerUnit {
    def getSingleton = BusyEvent
    object deleted_? extends MappedBoolean(this){
      	override def defaultValue = false
      	override def dbColumnName = "deleted"
    }
    object description extends MappedPoliteString(this,255)
    object is_employee_lanche_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbIndexed_? = true
        override def dbColumnName = "is_employee_lanche"
    }    
    object status extends MappedEnum(this,BusyEvent.StatusEnum){
    	override def dbIndexed_? = true
    	override def defaultValue = BusyEvent.StatusEnum.Single
    }
    def strDescription = description.is
    def toJson ={
    	// empurra hora de inicio para o inicio da agenda caso seja menor
    	// acontecia quando já tinha bloqueado e depois alterava o inicio da
    	// agenda para mais tarde. a opção de deletar (X) não aparecia
    	val start_aux = if (Project.dateToHours(this.start.is).slice (0,2).toLong < AuthUtil.company.calendarStart) {
	    	Project.dateToStrJs(this.start.is).slice (0,11) + 
	    	BusinessRulesUtil.zerosLimit(AuthUtil.company.calendarStart.toString,2) + 
	    	":00:00"
    	} else {
    		Project.dateToStrJs(this.start.is)
    	}

    	JsObj(
				("title", this.strDescription),
				("start", start_aux), // Project.dateToStrJs(this.start.is)),
				("end", Project.dateToStrJs(this.end.is)),
				("userId",this.user.is),
				("userName",this.user.obj match {
					case Full(u) => u.short_name.is
					case _ => ""
				}),
				("command",false),
				("customerId",false),
				("id", this.id.is),
				("color", ""), //"#E8AE9C"), // trazer aqui cor do bloqueio/serviço
                ("icon", ""), // colocar aqu o relogio se for workhour
				("is_employee_hour", this.is_employee_lanche_?.is),
                ("unitId", this.unit.is),
				("status", "not_work" )
		)
    }
    override def save() ={
        if(this.start.is == this.end.is){
            throw new RuntimeException("Bloqueio de agenda não pode ter início igual ao fim");
        }
        super.save

    }

}

object BusyEvent extends BusyEvent 
with LongKeyedMapperPerCompany[BusyEvent] 
//with LogicalDeleteMapper[BusyEvent]
{
	val SQL_CLEAR_BUSY_EVENT = """
		delete from busyevent  where 
		company =? and user_c=? 
		and is_employee_lanche=true 
		and  dateevent >= date(?) 
		and unit = ?;
	"""
	object StatusEnum extends Enumeration {
     	type StatusEnum = Value
     	val All, Single = Value
	}

	def clearBusyEventByUser(user:User, company:Company=AuthUtil.company, startDate:Date = new Date(), unit:CompanyUnit){
		// vaiii - nao está chegando aqui - tem erro no usercontroller na chamada da api
		// vaiii - o save do workhour tá deletando e deveria chamar este cara aqui
		//println ("vaiii ===================== excluir " + unit.id.toLong)
		DB.runUpdate(SQL_CLEAR_BUSY_EVENT, company.id.is::user.id.is::startDate::unit.id.toLong::Nil)
	}

	def hasLanchEventoToUser(user:Long, unit:Long, date:Date) = BusyEvent.
		// rigel 26/04/2018 
//	countWithDeleteds( 
		count (
		// tirei assim se tiver qq coisa no dia não gera mais
//		By(BusyEvent.is_employee_lanche_?, true),
		By(BusyEvent.unit, unit), // rigel 12/11/2016 
		By(BusyEvent.user, user), BySql("dateevent=date(?)",IHaveValidatedThisSQL("",""), date)) > 0;

	def constraintDate(user:User, start:Date, unit:CompanyUnit) = 
		BusyEvent.findAllInCompany(
			By(BusyEvent.user,user), 
			By(BusyEvent.unit,unit), 
			BySql(" date(?) between date(start_c) and date(end_c)",IHaveValidatedThisSQL("",""),start)) ::: BusyEvent.findAllInCompany( 
			BySql(" ? between start_c and end_c",IHaveValidatedThisSQL("",""),start),
			By(BusyEvent.status,BusyEvent.StatusEnum.All))

	def findByDate(start:Date, end:Date, cUnit:CompanyUnit) = BusyEvent.findAllInCompany(
			OrderBy(BusyEvent.start, Ascending),
			BySql[BusyEvent]("""
					((start_c between ? and ?)
				or
					(? between start_c  and end_c))
			 	and unit = ?
			 """,IHaveValidatedThisSQL("",""), start, end, start, cUnit.id.is))

// tirei esta condicao ao colocar a unit no busy event 
//			 	and user_c in (select id from business_pattern u where u.unit = ?)

	def findByDate(start:Date, end:Date) = BusyEvent.findAllInCompany(
			OrderBy(BusyEvent.start, Ascending),
			BySql[BusyEvent]("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("",""),start,end)
		)

	def findByDate(start:Date, end:Date, users:List[Long], cUnit:CompanyUnit, showWorkHours:Boolean = true) = {
		val basicWhere:List[QueryParam[BusyEvent]] = OrderBy(BusyEvent.start, Ascending) :: ByList(BusyEvent.user, users) :: BySql[BusyEvent]("""	
			    ((start_c between ? and date(?)+1)
				or
					(? between start_c  and end_c))
			 	and unit = ?
			 	""",IHaveValidatedThisSQL("",""), start, end, start, cUnit.id.is
			 ) :: Nil
		val workFilter:List[QueryParam[BusyEvent]] = if(showWorkHours){
				basicWhere
			}else{
				By(BusyEvent.is_employee_lanche_?, false) :: basicWhere
			}	
		BusyEvent.findAllInCompany((basicWhere ::: workFilter) :_* )
	}

	def findByDateWithoutLanch(start:Date, end:Date) = BusyEvent.findAllInCompany(
		OrderBy(BusyEvent.start, Ascending),
		By(BusyEvent.is_employee_lanche_?, false),BySql("""	(start_c between ? and ?)
				or
					(? between start_c  and end_c)
			 	""",IHaveValidatedThisSQL("",""),start,end, start)
		)

	def findByUserDate(user:User, start:Date, end:Date, cUnit:CompanyUnit) = BusyEvent.findAllInCompany(
			OrderBy(BusyEvent.start, Ascending),
			By(BusyEvent.user,user),
			BySql("""	((start_c between ? and ?)
				or
					(? between start_c  and end_c))
			 	and unit = ?
			 	""",IHaveValidatedThisSQL("",""),start,end, start, cUnit.id.is)
		) ::: BusyEvent.findAllInCompany(
			OrderBy(BusyEvent.start, Ascending),
			By(BusyEvent.status,BusyEvent.StatusEnum.All),
			BySql("dateevent between date(?) and date(?) and unit = ? ",IHaveValidatedThisSQL("",""),start,end, cUnit.id.is)
		) 
/*
                      .is_employee_lanche_?(true)
                      .company(company)
                      .start(new Date(assertedTime(freeBusy.start,"0"+realStartCompany+":00",0)) )
                      .end( new Date(assertedTime(freeBusy.start,"0"+realEndCompany+":00",0)))
                      .user(u.id.is)
                      .unit(cu.id.is) // vaiii teria que criar para as outras unidades do cara
                      .description("Não trabalha folga".format(u.name.is))
*/
	def countBE (company:Company, start:Date, end:Date, 
		user:Long, unit:Long) = {
		BusyEvent.count (
			By (BusyEvent.company, company),
			By (BusyEvent.start, start),
			By (BusyEvent.end, end),
			By (BusyEvent.user, user),
			By (BusyEvent.unit, unit))
	}
}
