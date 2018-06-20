package code
package model 

import code.util._
import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import net.liftweb.common.{Box,Full,Empty}

import _root_.java.math.MathContext; 

class Cheque extends Audited[Cheque] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with WithCustomer{ 
    def getSingleton = Cheque 
    object bank extends MappedLongForeignKey(this,Bank)
    object account extends MappedPoliteString(this,255)
	object agency extends MappedPoliteString(this,255)
	object number extends MappedPoliteString(this,255)
	object value extends MappedDecimal(this,MathContext.DECIMAL64,2)
	object paymentDate extends EbMappedDateTime(this)
	object dueDate extends EbMappedDateTime(this) // bom para
	object receivedDate extends EbMappedDate(this) // data do cheque para controlar validade
//	object efectiveDate extends EbMappedDateTime(this)
	object received extends MappedBoolean(this)
	object paymentDetail extends MappedLongForeignKey(this,PaymentDetail)
	object efetivePaymentDate extends EbMappedDate(this) // data em que ele foi descontato
	object movementType extends MappedInt(this) {
		override def dbIndexed_? = true
		override def defaultValue = AccountPayable.IN
	}

    def customerName = {
        customer obj match {
            case Full(u) => u.short_name.is
            case _ => ""
        }        
    }

    def bankName = {
        bank obj match {
            case Full(u) => u.short_name.is
            case _ => ""
        }        
    }

    def chequeDesc : String = {
        val chequePre : String = 
        if (dueDate.after(Project.today)) "*** " else "";

        chequePre + 
        this.customerName + " " +
        number.is + " " +
        this.bankName + " " +
        value.toString + " " +
        Project.dateToStrOrEmpty(dueDate)
    }

//  rigfel 19/06/2018 - não atualizava nem o lançamento nem o saldo
//	def makeAsReceived  = this.received(true).save
} 

object Cheque extends Cheque with LongKeyedMapperPerCompany[Cheque]  with  OnlyCurrentCompany[Cheque]{
}
