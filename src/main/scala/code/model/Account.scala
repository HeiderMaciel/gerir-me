package code
package model

import code.util._

import net.liftweb._
import mapper._
import http._
import SHtml._
//import util._
import _root_.java.math.MathContext;
import scalendar._
import Month._
import Day._
import net.liftweb.common.{ Box, Full, Empty }

import java.util.Date
import java.util.Calendar

class Account extends Audited[Account] 
  with PerCompany 
  with IdPK 
  with CreatedUpdated 
  with CreatedUpdatedBy 
  with NameSearchble[Account] 
  with ActiveInactivable[Account] {

  def getSingleton = Account
  override def updateShortName = false
/* agora só na accountcompanyunit
  object value extends MappedCurrency(this) with LifecycleCallbacks {
    override def defaultValue = 0.0;
    override def beforeSave() {
      super.beforeSave;
      if(!balanceControl_?.is){
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
*/

  object bank extends MappedLong(this)

  object allowCashierOut_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "allowCashierOut"
  }

  object balanceControl_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "balancecontrol"
  }

  object creditCard_? extends MappedBoolean(this){
    override def defaultValue = false
    override def dbColumnName = "creditCard"
  }

  object receive_? extends MappedBoolean(this){
    // quando false não entra na DRE 
    // para vale profissional por exemplo qdo a máquina é do 
    // profissional e ele vai devolver a parte da empresa
    // gera vale do total, mas não pode aparecer esta saída na DRE
    override def defaultValue = true
    override def dbColumnName = "receive"
  }

  def bankNum (account:Long) = if (account > 0) {
    val ac = Account.findByKey (account).get
    if (ac.bank > 0) {
      val bk = Bank.findByKey (ac.bank).get
      bk.banknumber
    } else {
      "000"
    }
  } else {
    "000"
  }


  def  getAccountUnit (unit : CompanyUnit): AccountCompanyUnit = {
    if (AccountCompanyUnit.count (
        By(AccountCompanyUnit.unit, unit),
        By(AccountCompanyUnit.account, this)) > 0) {
      AccountCompanyUnit.findAll (
        By(AccountCompanyUnit.unit, unit),
        By(AccountCompanyUnit.account, this))(0)
    } else if (AccountCompanyUnit.count (
        By(AccountCompanyUnit.account, this)) > 0) {
        // se já existe esta conta para alguma unit (qq unit)
        // o saldo da conta já foi pra uma delas
        val aau = AccountCompanyUnit.createInCompany.
        unit (unit).
        value (0.0). // cria com saldo zero
        lastValue (0.0).
        bank (this.bank).
        account (this).obs ("criado auto")
        aau
    } else {
        // se nao existe esta conta para nenhuma unit 
        // cria com o saldo que tá na conta
        // isso só tem sentido para clientes que tinham saldo antes
        // de separa por unit
        // 01/08/2017
        val aau = AccountCompanyUnit.createInCompany.
        unit (unit).
//        value (this.value).
//        lastValue (this.lastValue).
        value (0.0).
        lastValue (0.0).
        bank (this.bank).
        account (this).obs ("criado auto")
        aau
    }
  }

  lazy val balanceUnits = if (AuthUtil.user.isAdmin) {
      AccountCompanyUnit.findAll(By(AccountCompanyUnit.account,this))
  } else {
      AccountCompanyUnit.findAll(By(AccountCompanyUnit.account,this),
      BySql(" (unit = ? or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
              IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.user.id, AuthUtil.company.id))
  }

/*
  def removeRegister(accountP: AccountPayable, obs: String = "") = {
    Account.getAccountUnit._register(accountP, accountP.lastValue.is*(-1), obs, true)
  }
*/

/*
  private def saveWithoutHistory() = {
    super.save
  }
  private def createDefaultHistory(movementValue:Double, obs:String) = {
  AccountHistory.create
        .company(this.company)
        .account(this)
        .currentValue(this.value_ac.is)
        .description(obs)
        .value(movementValue)        
  }
  def valueChange:Boolean = this.value_ac.is != this.lastValue_ac.is
  override def save() = {
    if (valueChange) {
      createDefaultHistory(this.value_ac.is-this.lastValue_ac.is, "Alt Saldo Conta").save
    }
    val result = super.save    
    result    
  }

  private def _register(accountP: AccountPayable, registerValue: Double, obs: String = "", isRealValue:Boolean = false) = {
    DB.use(DefaultConnectionIdentifier) {
      conn =>
        try {
            val realValue  = if (accountP.typeMovement.is == AccountPayable.IN || isRealValue ) {
              registerValue
            } else {
              registerValue*(-1.00)
            }
            this.value_ac(this.value_ac.is + realValue)
            this.saveWithoutHistory
            accountP.debted_?(true)
            createDefaultHistory(realValue, obs).accountPayable(accountP).save
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
      _register(accountP, accountP.realValue - accountP.lastValue, "Alt Valor Lanç", true)
    }
  }
  def register(accountP: AccountPayable) = {
    if (accountP.typeMovement.is == AccountPayable.IN) {
        _register(accountP, accountP.value, "Crédito")
    } else {
        _register(accountP, accountP.value, "Débito")
    }
//    _register(accountP, accountP.value, "Lançamento")
  }
*/

  override def save() = {
    if(AuthUtil.?) {
      val ac = getAccountUnit (AuthUtil.unit)
      ac.save;
    }
    super.save
  }

  override def delete_! = {
      if(AccountPayable.count(By(AccountPayable.account,this.id)) > 0){
          throw new RuntimeException("Existe lançamento financeiro para esta conta! ")
      }
      if(PaymentType.count(By(PaymentType.defaltAccount,this.id)) > 0){
          throw new RuntimeException("Existe forma de pagamento faturada nesta conta! ")
      }
      super.delete_!
  }

}

object Account extends Account with LongKeyedMapperPerCompany[Account] with OnlyActive[Account]
