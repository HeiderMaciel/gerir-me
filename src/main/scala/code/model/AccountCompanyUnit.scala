package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.service._
import java.util.Date
//import java.util.Calendar
import net.liftweb.common.{Box,Full}

class AccountCompanyUnit extends Audited[AccountCompanyUnit] with PerCompany with IdPK 
    with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[AccountCompanyUnit] 
    with ActiveInactivable[AccountCompanyUnit] {
    def getSingleton = AccountCompanyUnit
    object account extends MappedLongForeignKey(this,Account)
    object unit extends MappedLongForeignKey(this,CompanyUnit) 
    object obs extends MappedPoliteString(this,255)
    object value extends MappedCurrency(this) with LifecycleCallbacks {
        override def beforeSave() {
          super.beforeSave;
          if(!balanceControl_?){
            this.set(0.00);
          }
        }
    }

    object lastValue extends MappedCurrency(this) with LifecycleCallbacks {
        override def defaultValue = fieldOwner.value
        override def beforeSave() {
          super.beforeSave;
          this.set(this.defaultValue);
        }
    }
    object bank extends MappedLong(this);
    def balanceControl_? : Boolean = {
        account.obj match {
            case Full(t) => t.balanceControl_?
            case _ => false
        }
    }

    lazy val accountName:String = {
        account.obj match {
            case Full(c)=> c.short_name.is
            case _ => ""
        }
    }
    lazy val unitName:String = {
        unit.obj match {
            case Full(c)=> c.short_name.is
            case _ => ""
        }
    }

  private def saveWithoutHistory() = {
    if (this.name == "" || this.name == null) {
       name.set (accountName + " " + unitName)
    }
    super.save
  }

  private def createDefaultHistory(movementValue:Double, 
    obs:String, paymentDate:Date) = {
    AccountHistory.create
        .company(this.company)
        .account(this.account)
        .currentValue(this.value.is)
        .paymentDate(paymentDate)
        .description(obs)
        .value(movementValue)        
  }

  def valueChange:Boolean = this.value.is != this.lastValue.is

  override def save() = {
    if (this.name == "" || this.name == null) {
       name.set (accountName + " " + unitName)
    }
    if (valueChange) {
      createDefaultHistory(this.value.is-this.lastValue.is, 
        "Alt Saldo Conta", new Date()).save
    }
    val result = super.save    
    result    
  }

  private def _register(accountP: AccountPayable, 
    registerValue: Double, obs: String = "", 
    isRealValue:Boolean = false) = {
    DB.use(DefaultConnectionIdentifier) {
      conn =>
        try {
            val realValue  = if (accountP.typeMovement.is == AccountPayable.IN 
              || isRealValue ) {
              registerValue
            } else {
              registerValue*(-1.00)
            }
            this.value(this.value.is + realValue)
            this.saveWithoutHistory
            accountP.debted_?(true)
            createDefaultHistory(realValue, 
              obs, accountP.paymentDate).accountPayable(accountP).save
        } catch {
          case e: Exception => {
            conn.rollback
            throw e
          }
        }
    }
  }

  def removeRegister(accountP: AccountPayable, obs: String = "") = {
    _register(accountP, accountP.lastValue.is*(-1), obs, true)
  }

  def registerDiference(accountP: AccountPayable) = {
    if (accountP.realValue != accountP.lastValue.is ) {
      _register(accountP, accountP.realValue - accountP.lastValue, "Alt Valor Lanç conta " + accountP.accountShortName + " cat " + accountP.categoryShortName, true)
    }
  }
  def register(accountP: AccountPayable) = {
    if (accountP.typeMovement.is == AccountPayable.IN) {
        _register(accountP, accountP.value, "Crédito conta " + accountP.accountShortName + " cat " + accountP.categoryShortName)
    } else {
        _register(accountP, accountP.value, "Débito conta  " + accountP.accountShortName + " cat " + accountP.categoryShortName)
    }
  }

}

object AccountCompanyUnit extends AccountCompanyUnit with LongKeyedMapperPerCompany[AccountCompanyUnit] with OnlyCurrentCompany[AccountCompanyUnit]
with OnlyActive[AccountCompanyUnit] {
    //override def dbTableName = "bpaccount"
}

