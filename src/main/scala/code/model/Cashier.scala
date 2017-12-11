package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._ 
import code.service._ 
import code.service.events._
import net.liftweb.mapper.{StartAt, MaxRows,NotBy}
import java.util.regex._
import scala.xml.Text
import net.liftweb.util.{FieldError}
import net.liftweb.proto._
import _root_.java.math.MathContext;
import java.util.Date;
import net.liftweb.common.{Box,Full,Empty}


class Cashier extends LongKeyedMapper[Cashier]  
    with PerCompany 
    with PerUnit 
    with IdPK 
    with CreatedUpdated 
    with Audited[Cashier] 
    with CreatedUpdatedBy 
    with CompanyIdable[Cashier] 
    with net.liftweb.common.Logger { 
    def getSingleton = Cashier
    object openerDate extends EbMappedDateTime(this)
    object firstCloseDate extends EbMappedDateTime(this)
    object closerDate extends EbMappedDateTime(this)
    object startValue extends MappedCurrency(this)
    object firstStartValue extends MappedCurrency(this)
    object endValue extends MappedCurrency(this)//Money
    object autoOpened_? extends MappedBoolean(this){
      override def defaultValue = false
      override def dbColumnName = "autoOpened"
    }    
    object valueToConference extends MappedCurrency(this){
        override def defaultValue = 0.00
    }
    def isClosed = !isOpen
    def isOpen = status.is == Cashier.CashierStatus.Open
    object status extends MappedEnum(this,Cashier.CashierStatus){
      override def defaultValue = Cashier.CashierStatus.Open
    }
    object obs extends MappedPoliteString(this,555)

    def companyId= company.obj match {
        case Full(c) => c.id.is
        case _ => 0.toLong
    }
    
    def unSecureFrom(c:CompanyUnit) = {
        unit(c)
        save
        this        
    }

    def from(c:CompanyUnit) = {
        if(c.useSingleCashier_?.is && !Cashier.unSecurefindOpenCashiers.isEmpty){
            throw new AlreadyOpenedCashier
        }
        unSecureFrom(c)        
    }

    def paidValue = {
       Payment.totalPaidByCashier(this.id.is)
    }

    def paidValueInCheque = {
       Payment.totalPaidByCashier(this.id.is,PaymentType.PaymentCheckIds)
    } 
    def paidValueToFonference = {
        if(valueToConference.is == 0.00 ){
            valueToConference(Payment.totalPaidByCashier(this.id.is,PaymentType.PaymentToConferenceIds))
            if(!this.isOpen)
            this.save
        }
        valueToConference.is
    }      
    
    def paidValueInCard = {
       Payment.totalPaidByCashier(this.id.is,PaymentType.PaymentCardIds)
    }      
    def paidValueInMoney = {
       Payment.totalPaidByCashier(this.id.is,PaymentType.PaymentMoneyIds)
    }       
    // dinheiro
    def outputValue = {
        AccountPayable.findAll(By(AccountPayable.cashier,this),
            By(AccountPayable.toConciliation_?,false),
            By(AccountPayable.auto_?,false),
            BySql(" cheque is null ",IHaveValidatedThisSQL("",""))
            ).map((a) =>
            if(a.typeMovement.is == AccountPayable.OUT)
                a.value.is
            else
                0.0
        ).foldLeft(BigDecimal(0.0))(_+_)
    }
    // cheque
    def outputCheque = {
        AccountPayable.findAll(By(AccountPayable.cashier,this),
            By(AccountPayable.toConciliation_?,false),
            By(AccountPayable.auto_?,false),
            BySql(" cheque is not null ",IHaveValidatedThisSQL("",""))
            ).map((a) =>
            if(a.typeMovement.is == AccountPayable.OUT)
                a.value.is
            else
                0.0
        ).foldLeft(BigDecimal(0.0))(_+_)
    }
    def inputValue = {
        AccountPayable.findAll(By(AccountPayable.cashier,this),
            By(AccountPayable.toConciliation_?,false),
            By(AccountPayable.auto_?,false)).map((a) =>
            if(a.typeMovement.is == AccountPayable.IN)
                a.value.is
            else
                0.0
        ).foldLeft(BigDecimal(0.0))(_+_)
    }

    def buildEndValue ={
        endValue(paidValue+startValue.is-outputValue.toDouble+inputValue.toDouble)
        this
    }
    def reopen (valueStartValue:Double) = {
        def newValue : Double = if (valueStartValue != 0.0) {
                valueStartValue
            } else {
                startValue
            }
        if(this.status.is  == Cashier.CashierStatus.Open) {
            throw new RuntimeException("Caixa já está aberto!")
        }
        if (!FatService.canDesFat(this)) {
            throw new RuntimeException("Caixa com lançamentos conciliados/consolidados não pode ser reaberto!")
        }
        this.status(Cashier.CashierStatus.Open)
        .closerDate(null)
        .valueToConference(0)
        .autoOpened_?(false)
        .startValue (newValue)
        .save
        FatService.desFat(this)
        this        
    }
    def close = {
        def newValue : Date = if (firstCloseDate == Empty || firstCloseDate == null) {
                new Date()
            } else {
                firstCloseDate
            }
        if(this.status.is ==  Cashier.CashierStatus.Closed){
           throw new AlreadyClosedCashier("Caixa já está fechado!") 
        }
        try {
            FatService.fat(this);
        } catch  {
          case e: Exception => {
            throw e
          }
        }

        this.status(Cashier.CashierStatus.Closed)
        .buildEndValue
        .closerDate(new Date())
        .firstCloseDate(newValue)
        .save
        this
    }

    def unitsToShowSql = if (AuthUtil.user.isAdmin) {
      " 1 = 1 "
    } else {
      " (ca.unit = %s or (ca.unit in (select uu.unit from usercompanyunit uu where uu.user_c = %s and uu.company = %s))) ".format(AuthUtil.user.unit, AuthUtil.user.id, AuthUtil.user.company)
    }

    def openerAt(oDate:Date) = openerDate(oDate)

    def unitName ={
        unit obj match {
            case Full(u) => u.short_name.is
            case _ => ""
        }
        
    }
    def userName = {
        createdBy obj match {
            case Full(u) => u.short_name.is
            case _ => ""
        }        
    }

} 

object Cashier extends Cashier with LongKeyedMapperPerCompany[Cashier]  with  OnlyCurrentCompany[Cashier] with net.liftweb.common.Logger{
    lazy val SQL_REPORT_PAYMENT_TYPES = """
                                        select * from (select t.name paymenttype, 
                                        sum(pd.value) as value
                                        from paymentdetail pd
                                        inner join payment p on(pd.payment = p.id and p.company = pd.company)
                                        inner join paymenttype t on(pd.typepayment=t.id)
                                        WHERE pd.company = ? and p.cashier= ? and %s
                                        group by t.name
                                        order by t.name) as data;"""

    lazy val SQL_REPORT_PAYMENT_TYPES_UNIT =  """
                                        select t.name paymenttype, sum(pd.value) as value
                                        from paymentdetail pd
                                        inner join payment p on(pd.payment = p.id)
                                        inner join paymenttype t on(pd.typepayment=t.id)
                                        inner join cashier c on(c.id = p.cashier)
                                        WHERE c.unit = ?
                                        and p.datepayment between ? and ?  
                                        and %s
                                        group by t.name
                                        order by t.name;"""
    lazy val SQL_REPORT_PAYMENT_TYPES_COMPANY =  """
                                        select t.name paymenttype, sum(pd.value) as value
                                        from paymentdetail pd
                                        inner join payment p on(pd.payment = p.id)
                                        inner join paymenttype t on(pd.typepayment=t.id)
                                        inner join cashier c on(c.id = p.cashier)
                                        WHERE pd.company = ?
                                        and p.datepayment between ? and ?
                                        and %s
                                        group by t.name
                                        order by t.name;"""

//    def showAllCashiers = AuthUtil.user.isAdmin  || AuthUtil.user.isFinancialManager || 
//     AuthUtil.user.isCashierGeneral || AuthUtil.unit.useSingleCashier_?.is
    def showAllCashiers = AuthUtil.user.isAdmin || 
     AuthUtil.unit.useSingleCashier_?.is

    def showUnitCashiers = (AuthUtil.user.isCashierGeneral || AuthUtil.user.isFinancialManager) &&
        (!AuthUtil.user.isAdmin)

    def findAllCashiers = 
        if (showUnitCashiers) {
            findAllInCompany(BySql(" (unit = ? or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
                IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.user.id, AuthUtil.company.id),OrderBy(Cashier.id, Descending), MaxRows(200))
        } else if(!showAllCashiers){
            findAllInCompany(By(Cashier.createdBy, AuthUtil.user),OrderBy(Cashier.id, Descending), MaxRows(200))
        } else {
            findAllInCompany(OrderBy(Cashier.id, Descending), MaxRows(200))
        }

    def findClosedCashiers = {
        if (showUnitCashiers) {
            findAllInCompany(BySql(" (unit = ? or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
                IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.user.id, AuthUtil.company.id),
                By(Cashier.status,Cashier.CashierStatus.Closed), 
                OrderBy(Cashier.id, Descending), MaxRows(200))
		} else if(!showAllCashiers){
    	    findAllInCompany(By(Cashier.createdBy, AuthUtil.user),By(Cashier.status,Cashier.CashierStatus.Closed), OrderBy(Cashier.id, Descending), MaxRows(200))
		}else{
		    findAllInCompany(By(Cashier.status,Cashier.CashierStatus.Closed), OrderBy(Cashier.id, Descending), MaxRows(200))
		}
	}

	def findAllOpenCashiers(params: QueryParam[Cashier]*): List[Cashier] = { 
		findAllInCompany(
			   By(Cashier.status,Cashier.CashierStatus.Open)
            :: MaxRows[Cashier](500)
			:: OrderBy(Cashier.id, Descending) 
			:: params.toList :_*
		)
	}

    def findOpenCashiers = {
    	EventAggregator.onFindCashierOpen
        if (showUnitCashiers) {
            findAllOpenCashiers(BySql(" (unit = ? or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
                IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.user.id, AuthUtil.company.id))
        } else if(!showAllCashiers){
			findAllOpenCashiers(By(Cashier.createdBy, AuthUtil.user))
		}else{
		    unSecurefindOpenCashiers
		}
	}

    // feito especifco para encontrar caixa na troca de status de serviço de 
    // mensalidade
    def findOpenCashiersUnit = {
        EventAggregator.onFindCashierOpen
        if (showUnitCashiers) {
            // ver se aqui precisa olhar para outras unidades do usuario, 
            // mas a cho que é a corrente mesmo
            findAllOpenCashiers(By(Cashier.unit,AuthUtil.unit))
        } else if(!showAllCashiers){
            findAllOpenCashiers(By(Cashier.createdBy, AuthUtil.user),
                By(Cashier.unit,AuthUtil.unit))
        }else{
            findAllInCompany(
             By(Cashier.status,Cashier.CashierStatus.Open),
             By(Cashier.unit,AuthUtil.unit),
             MaxRows[Cashier](1),
             OrderBy(Cashier.id, Descending))             
        }
    }

    def checkifclosed (cashId:Long) = {
        //val ac = Cashier.findByKey(cashId)
        if (Cashier.count(By(id,cashId),By(status,Cashier.CashierStatus.Closed)) > 0) {
        //if(ac[0].status.is ==  Cashier.CashierStatus.Closed){
            //if (ac.isClosed) {
            true
        } else {
            false
        }
    }

    def unSecurefindOpenCashiers = findAllOpenCashiers()

    def unSecurefindCashiersToday = findAllInCompany(
        BySql[Cashier]("date(openerdate) = date(?)",IHaveValidatedThisSQL("",""), new Date())
    )

    def unSecurefindAutoOpenCashiersBeforeToday = findAllOpenCashiers(
        By(autoOpened_?, true),
    	BySql("date(openerdate) < date(?)",IHaveValidatedThisSQL("",""), new Date())
    )

	def open = {
		createInCompany
	}
    def findByCompanyId(idForCompany:Int) = {
        val results = findAllInCompany(By(this.idForCompany, idForCompany))
        results(0)
    }
    def findOpenCashierByIdAndCompany(id:Int):Cashier = {
        val cashers = 
        if (showUnitCashiers) {
                findAllInCompany(By(Cashier.idForCompany,id), 
                BySql(" (unit = ? or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
                IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.user.id, AuthUtil.company.id))
            } else if(!showAllCashiers){
                findAllInCompany(By(Cashier.idForCompany,id), By(Cashier.createdBy, AuthUtil.user))
            }else{
                findAllInCompany(By(Cashier.idForCompany,id))
            }
        cashers(0)
    }
    object CashierStatus extends Enumeration {
        type CashierStatus = Value
        val Open,Closed = Value
    }

    def singleCashierUnitProcess {
    	// info("singleCashierUnitProcess")
    	if(AuthUtil.unit.useSingleCashier_?.is){
    		unSecurefindAutoOpenCashiersBeforeToday.foreach(_.close)
    		if(unSecurefindCashiersToday.isEmpty)//manualy close needs manualy open
    			Cashier.open autoOpened_?(true)  openerAt(new Date()) startValue(0.00) unSecureFrom(AuthUtil.unit)
    	}
    }
}
class AlreadyOpenedCashier extends Exception
class AlreadyClosedCashier(m:String) extends RuntimeException(m)
