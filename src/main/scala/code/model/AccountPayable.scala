package code
package model

import code.util._

import net.liftweb._
import mapper._
import http._
import SHtml._
import util._
import _root_.java.math.MathContext;
import scalendar._
import Month._
import Day._
import net.liftweb.common.{ Box, Full, Empty }

import java.util.Date

class AccountPayable 
extends Audited[AccountPayable] 
with PerCompany 
with IdPK 
with CreatedUpdated
with CreatedUpdatedBy 
with FinancialMovement 
with net.liftweb.common.Logger
with CanCloneThis[AccountPayable] {
  def getSingleton = AccountPayable
  object cashier extends MappedLongForeignKey(this, Cashier)
  object paymentDate extends EbMappedDate(this) with LifecycleCallbacks {
    override def beforeSave() {
        super.beforeSave;
        if( (this.get == Empty || this.get == null)  && paid_?.is){
          this.set(dueDate.is)
        }
    } 
  }
  object dueDate extends EbMappedDate(this)
  object exerciseDate extends EbMappedDate(this) with LifecycleCallbacks {
    override def defaultValue = fieldOwner.dueDate;
    override def beforeSave() {
        super.beforeSave;
        if(this.get > dueDate.is){
         this.set(dueDate.is)
        }
    } 
  }
  object createdAtDate extends EbMappedDate(this) {
    override def defaultValue = new Date()
  }
  
  object complement extends MappedPoliteString(this, 255)
  object lastAccount extends MappedLongForeignKey(this, Account) with LifecycleCallbacks {
    override def defaultValue = fieldOwner.account
    override def beforeSave() {
      super.beforeSave;
      this.set(this.defaultValue);
    }
  }

  object lastCashier extends MappedLong(this) with LifecycleCallbacks {
    override def defaultValue = fieldOwner.cashier
    override def beforeSave() {
      super.beforeSave;
      this.set(this.defaultValue);
    }
  }  

  object lastValue extends MappedDouble(this) with LifecycleCallbacks {
    override def defaultValue = fieldOwner.realValue
    override def beforeSave() {
      super.beforeSave;
      this.set(this.defaultValue);
    }
  }
  object lastPaidStatus_? extends MappedBoolean(this) with LifecycleCallbacks {
    override def defaultValue= fieldOwner.paid_?.is
    override def dbColumnName = "last_paid"
    override def beforeSave() {
      super.beforeSave;
      this.set(this.defaultValue);
      cheque.obj match {
        case Full(c:Cheque) => {
          def efetivePaymentDate:Box[Date]  = if (fieldOwner.paid_?.is) Full(fieldOwner.dueDate) else Empty
          c.received(fieldOwner.paid_?.is).efetivePaymentDate.setFromAny(efetivePaymentDate)
          c.save
        }
        case _ => 
      }
    }    
  }
  object paid_? extends MappedBoolean(this) {
    override def dbIndexed_? = true
    override def dbColumnName = "paid"
  }
  /**
   * mostra se ja saiu da conta que deveria sair o valor
   */
  object debted_? extends MappedBoolean(this) {
    override def dbIndexed_? = false
    override def dbColumnName = "debted"
  }

  object toConciliation_? extends MappedBoolean(this) {
    override def defaultValue= false
    override def dbIndexed_? = false
    override def dbColumnName = "toconciliation"
  }

  object conciliate extends MappedInt(this) {
    override def defaultValue=0
  }

  object auto_? extends MappedBoolean(this) {
    override def dbIndexed_? = false
    override def dbColumnName = "autocreated"
  }

  object recurrence extends MappedLongForeignKey(this, Recurrence) {
    override def dbIndexed_? = true
  }

  object cheque extends MappedLongForeignKey(this, Cheque)
  object unitvalue extends MappedCurrency(this.asInstanceOf[MapperType]) with LifecycleCallbacks {
      override def defaultValue = 0
      override def beforeSave() {
          super.beforeSave;
          if(amount.is != 0.0){
            unitvalue (value.is/amount.is)
          }
      } 
  }
  object parcelNum extends MappedInt(this)

  def makeAsPaid = this.paid_?(true).partialySecureSave

  def makeAsConciliated = {
    if (this.conciliate.is == 0) {
      this.conciliate(1).partialySecureSave
    } else {
      throw new RuntimeException("Só lançamentos em aberto podem ser conciliados!")
    }
  }

  def makeAsConsolidated = this.conciliate(2).partialySecureSave

  lazy val accountBox = this.account.obj

  lazy val accountShortName:String = {
      accountBox match {
          case Full(c)=> c.short_name.is
          case _ => ""
      }
  }

  //override
  def changeAccount_? = lastAccount.is != account.is
  def changeAccountProcess {
    if(debted_?.is && changeAccount_?){
      this.lastAccount.obj match {
        case Full(a: Account) => {
          val au = a.getAccountUnit
          au.removeRegister(this, "Alt Conta")
        }
        case _ => 
      }
      debted_?(false)
    }
  }

  override def delete_! = {
    if(AuthUtil.? && (!AuthUtil.user.isAdmin && !AuthUtil.user.isFinancialManager)){
      //info ("************************* não é adm nem gerente ")
      throw new RuntimeException("Somente Administradores e Gerentes Financeiros podem excluir lançamentos")
    }
    if ((!cashier.isEmpty) && (Cashier.checkifclosed(cashier))) {
      //info ("************************* caixa fehcado")
      throw new RuntimeException("Não é permitido excluir lançamento de caixa fechado")
    }
    if (conciliate > 0) {
      throw new RuntimeException("Não é permitido excluir lançamento conciliado")
    }
    if(debted_?.is){
      accountBox match {
        case Full(a: Account) => {
          val au = a.getAccountUnit
          au.removeRegister(this, "Excluindo lançamento")
        }
        case _ => 
      }
    }
    val result = super.delete_!
    result
  }

  def debitProcess {
    val registerDiference = debted_?.is
    accountBox match {
      case Full(a: Account) => {
        val au = a.getAccountUnit
        if (registerDiference) {
          au.registerDiference(this)
        } else {
          au.register(this)
        }
      }
      case _ => this.debted_?(true)
    }
  }

  def rollbackAccountValue() = {
    accountBox match {
      case Full(a: Account) => {
        val au = a.getAccountUnit
        au.removeRegister(this, "Alt Status Lanç")
      }
      case _ => 
    }
    debted_?(false)    
  }

  def partialySecureSave = {
    if (!isNew) {
      changeAccountProcess
      if (paid_?.is) {
        debitProcess
      }else if(lastPaidStatus_?.is){
        rollbackAccountValue()
      }
      super.save
    } else {
      println ("vaii nao deve cair aqui nunca ============= pq este método não é para inserir novo")
      throw new RuntimeException("vaii nao deve cair aqui nunca ============= pq este método não é para inserir novo")
      // 22/08/2016 feito o save primeiro para ter o id e carimbar no hist alteracao 
      if (this.paid_?.is) {
         this.debted_?(true)
      }
      super.save
      val a = Account.findByKey(this.account.is).get
      val au = a.getAccountUnit
      au.register(this)
    }

  }

  override def save() = {
    def action = if (isNew) {
      "cadastrar"
    } else {
      "alterar"
    }
    if ((!cashier.isEmpty) && (Cashier.checkifclosed(cashier))) {
      //info ("************************* caixa fechado alteracao")
      throw new RuntimeException("Não é permitido " + action + " lançamento de caixa fechado")
    }
    if (conciliate > 0) {
      throw new RuntimeException("Não é permitido alterar lançamento conciliado")
    }
    if ((account.isEmpty)) {
      // info ("************************* falta conta")
      throw new RuntimeException("Não é permitido lançamento sem conta")
    }
    if ((category.isEmpty)) {
      // info ("************************* falta categoria")
      throw new RuntimeException("Não é permitido lançamento sem categoria")
    }

    if (!isNew) {
//    if (this.id.is > 0) {
      changeAccountProcess
      if (paid_?.is) {
        debitProcess
      }else if(lastPaidStatus_?.is){
        rollbackAccountValue()
      }
      super.save
    } else {
      // 22/08/2016 feito o save primeiro para ter o id e carimbar no hist alteracao 
      if (this.paid_?.is) {
         this.debted_?(true)
      }
      if (this.paid_?.is) {
        super.save
        val a = Account.findByKey(this.account.is).get
        val au = a.getAccountUnit
        au.register(this)
      } else {
        super.save
      }
    }
  }

  def transferTo(toAccount:Long, toout_of_cacashier:String, cashierTo:String, cashier_numberTo:String){
    def cashierToObj = Cashier.findByKey(cashierTo.toLong)
    def cashierToBox:Box[Cashier] = cashierToObj match {
      case Full(c:Cashier) => Full(c)
      case _ => if(cashier_numberTo != "") {
        Full(Cashier.findByCompanyId(cashier_numberTo.toInt))
      }else{
        Empty
      }
    }

    val accountPayable = this.clone()
    if(accountPayable.typeMovement == AccountPayable.IN){
      accountPayable.typeMovement(AccountPayable.OUT)
    }else{
      accountPayable.typeMovement(AccountPayable.IN)
    }
    if(toout_of_cacashier.toBoolean) {
      accountPayable.cashier(cashierToBox)
    } else {
      // para colocar Empty - garantir 
      accountPayable.cashier(Empty)
    }
    accountPayable.debted_?(false).account(toAccount).save
  }

    def unitsToShowSql = if (AuthUtil.user.isAdmin) {
      " 1 = 1 "
    } else {
      " (ap.unit = %s or (ap.unit in (select uu.unit from usercompanyunit uu where uu.user_c = %s and uu.company = %s))) ".format(AuthUtil.user.unit, AuthUtil.user.id, AuthUtil.user.company)
    }

  def reprocessRecurencByThis {
    recurrence
      .obj
      .get
      .updateByAccount(this)
  }

  def deleteRecurencByThis {
    recurrence
      .obj
      .get
      .deleteByAccount(this)
  }

  def toIcs(title :String = this.category.obj.get.name.is) = IcsFileUtil.buildIcsStr(
            this.id.is.toString,
            this.dueDate.is,
            this.dueDate.is,
            this.company.obj.get.email.is,
            title,
            "", 
            this.obs.is
        ).getBytes("UTF-8")  

  def consolidate(accountId:Long, value:Double, dueDate: Date) = {
    val categoryBC = AccountCategory.balanceControlCategory;
    val val1 = if (value < 0.0) {
        value * -1
      } else {
        value
      }

    val accountType = if (value < 0.0) {
        AccountPayable.OUT
      } else {
        AccountPayable.IN
      }
    val account1 = AccountPayable
      .createInCompany
      .unit(AuthUtil.unit.id)
      .typeMovement(accountType)
      .category(categoryBC.id)
      .exerciseDate(dueDate)
      .dueDate(dueDate)
      .paymentDate(dueDate)
      .obs("Gerado pelo processo de consolidação")
      .account(accountId)
      .auto_?(true)
      //.cashier(cashier.id.is)
      //.paymentType(paymentType.id.is)
      .costCenter(AuthUtil.unit.costCenter.is)
      .value(val1)
//try{
    account1.save
    account1.makeAsConsolidated
    println ("vaiii  ===================== acabou consol mesmo " + account1.id.is)
        //} catch {
        //case e:Exception => "" //JString(e.getMessage)
        //}
   ""
  }

}

object AccountPayable extends AccountPayable with LongKeyedMapperPerCompany[AccountPayable] with OnlyCurrentCompany[AccountPayable] with OnlyCurrentUnit[AccountPayable] with CanClone[AccountPayable] {
  lazy val IN = 0
  lazy val OUT = 1
  lazy val TRANS = 2

  def findAllByStartEndOnlyPaidByUser(start: Date, end: Date, company: Company, 
    unitsql: String, userId: String, dttypes: String) = {
    val sqldt : String = if (dttypes == "1") { // competencia
                " date(exerciseDate) between date(?) and date(?) "
              } else if (dttypes == "2") { // pagamento
                " date(paymentDate) between date(?) and date(?) "
              } else { // 0 vencimento
                " date(duedate) between date(?) and date(?) "
              }

    AccountPayable.findAllInCompany(
//      By(AccountPayable.user, userId),
      By(AccountPayable.company, company),
      By(AccountPayable.toConciliation_?,false),
      By(AccountPayable.paid_?, true),
      BySql(" user_c in (%s) ".format (userId), IHaveValidatedThisSQL("", "")),
      BySql(unitsql, IHaveValidatedThisSQL("", "")),
      BySql(sqldt, IHaveValidatedThisSQL("", ""), start, end)
      //,
      //OrderBy(AccountPayable.dueDate, Ascending),OrderBy(AccountPayable.id, Ascending)
      )
  }
  def findAllByStartEnd(dttypes: String, start: Box[Date], end: Box[Date], 
    categories: List[Long] = Nil, categoryTx : String, accountsLong: List[Long] = Nil, cashiers: List[Long] = Nil, units: List[Long] = Nil, users: List[Long] = Nil, startCreate: Box[Date], endCreate: Box[Date], types: List[Int] = Nil, statusList: List[Boolean] = Nil, startValue: Double = 0.00, endValue: Double = 0.00, obs:String, costcenters:List[Long], paymenttypes:List[Long]): List[code.model.AccountPayable] = {

    lazy val onlyPaidFilter = statusList match {
      case a :: Nil => By(AccountPayable.paid_?, a)
      case _ => BySql[AccountPayable]("1 = 1", IHaveValidatedThisSQL("", ""))
    }

    def typesFilter = types match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Int] => ByList(AccountPayable.typeMovement, list)
    }

    def startValueFilter = By_>=(AccountPayable.value, startValue)
    def endValueFilter = By_<=(AccountPayable.value, endValue)
    def costcentersFilter = costcenters match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Long] =>(
          BySql[code.model.AccountPayable]("""
            costcenter in(
              select ab.id
              from costcenter a,
              costcenter ab
              where a.id in ( %s )
              and ab.mintreenode between a.mintreenode and a.maxtreenode and ab.company=?
            )""".format(list.map(_.toString).reduceLeft(_+","+_)),IHaveValidatedThisSQL("", ""), AuthUtil.company.id.is)
        )
    }
    def paymenttypesFilter = paymenttypes match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Long] => ByList(AccountPayable.paymentType, list)
    }

    def categoryFilter = categories match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Long] =>(
          BySql[code.model.AccountPayable]("""
            category in(
              select ab.id
              from accountcategory a,
              accountcategory ab
              where a.id in ( %s )
              and ab.mintreenode between a.mintreenode and a.maxtreenode and ab.company=?
            )""".format(list.map(_.toString).reduceLeft(_+","+_)),IHaveValidatedThisSQL("", ""), AuthUtil.company.id.is)
        )
      
    }

    def categoryFilter2 = if (categoryTx == "1") {
      BySql[AccountPayable]("1 = 1", IHaveValidatedThisSQL("", ""))
    } else {
      BySql[code.model.AccountPayable]("""
        category in ( select id from accountcategory where typeMovement <> 2 and company = ?) """,IHaveValidatedThisSQL("", ""), AuthUtil.company.id.is)
    }

    def accountFilter = accountsLong match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Long] => ByList(AccountPayable.account, list)
    }

    def cashiersFilter = cashiers match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Long] => ByList(AccountPayable.cashier, list)
    }

    def unitFilter = units match {
      case Nil => By(AccountPayable.unit, AuthUtil.unit.id.is)
      case list: List[Long] => ByList(AccountPayable.unit, list)
    }
    def userFilter = users match {
      case Nil => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
      case list: List[Long] => ByList(AccountPayable.user, list)
    }

    def startEndFilter = start match {
      case Full(startDate) => {
        def endDate = end match {
          case Full(e) => e
          case _ => new Date()
        }

        val sqldt = if (dttypes == "1") { // compatencia
            "date(exerciseDate) between date(?) and date(?)"
          } else if (dttypes == "2") { // pagamento
            "date(paymentDate) between date(?) and date(?)"
          } else { // 0 vencimento
            "date(dueDate) between date(?) and date(?)"
          }
        BySql[code.model.AccountPayable](sqldt,IHaveValidatedThisSQL("", ""), startDate, endDate)
      }
      case _ => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
    }

    def startEndCreateFilter = startCreate match {
      case Full(startDate) => {
        def endDate = endCreate match {
          case Full(e) => e
          case _ => new Date()
        }

        BySql[code.model.AccountPayable]("date(createdatdate) between date(?) and date(?)",
          IHaveValidatedThisSQL("createdatdate", "01-01-2012 00:00:00"), startDate, endDate)
      }
      case _ => BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
    }
    def permissionFilter = if(AuthUtil.user.isCashier && 
        !AuthUtil.user.isCashierGeneral && 
        !AuthUtil.user.isFinancialManager && 
        !AuthUtil.user.isFinancialUser) {
        BySql[code.model.AccountPayable](" cashier in( select id from cashier where cashier.company = accountpayable.company and createdby = ? ) ",IHaveValidatedThisSQL("", ""), AuthUtil.user.id.is)
    }else if (AuthUtil.user.isCashierGeneral && 
        !AuthUtil.user.isFinancialManager && 
        !AuthUtil.user.isFinancialUser) {  
        BySql[code.model.AccountPayable](" cashier is not null ",IHaveValidatedThisSQL("", ""))
    }else if (AuthUtil.user.isFinancialUser && 
        !AuthUtil.user.isFinancialManager) {
        BySql[AccountPayable](" category in (select ac.id from accountcategory ac where (ac.managerlevel = 0 or ac.managerlevel is null) and ac.company = accountpayable.company) ", IHaveValidatedThisSQL("", ""))
    } else {
        BySql[AccountPayable]("1 =1", IHaveValidatedThisSQL("", ""))
    }

    def obsFilter = 
    BySql[code.model.AccountPayable]("lower(obs) like ?",
          IHaveValidatedThisSQL("", ""), "%"+obs.toLowerCase+"%")

    val filter: Seq[net.liftweb.mapper.QueryParam[code.model.AccountPayable]] = startEndFilter :: 
    startValueFilter :: endValueFilter :: onlyPaidFilter :: categoryFilter :: categoryFilter2 :: cashiersFilter :: 
    unitFilter :: userFilter :: startEndCreateFilter :: typesFilter :: accountFilter :: 
    By(AccountPayable.toConciliation_?,false) ::
    OrderBy(AccountPayable.dueDate, Descending) :: OrderBy(AccountPayable.id, Descending) :: 
    permissionFilter :: obsFilter :: costcentersFilter :: paymenttypesFilter ::Nil
    AccountPayable.findAllInCompany(filter: _*)
  }

  def countApByRecurrence (recurrence:Recurrence, date:Date) = {
      AccountPayable.count(
                          By(AccountPayable.recurrence, recurrence.id.is), 
                          By(AccountPayable.dueDate, date)
                      )
  }

  //acho que nao usa  
  def findAllByStartEndOnlyPaidxxxx(start: Date, end: Date, company: Company) = {
    AccountPayable.fildAllInUnit(
      By(AccountPayable.company, company),
      By(AccountPayable.paid_?, true), BySql("date(dueDate) between date(?) and date(?)", IHaveValidatedThisSQL("dueDate", "01-01-2012 00:00:00"), start, end))
  }

  def totalPaidXXXX(start: Date, end: Date, company: Company) = {
    val r = DB.performQuery("select sum(value) from accountpayable where company = ? date(paymentDate) between date(?) and date(?)", List(company.id.is, start, end))
    r._2(0)(0) match {
      case a: Any => a.toString.toFloat
      case _ => 0.0f
    }
  }
  val SQL_REPORT = """select %s,sum(value) as value,  min(dt.duedate) as duedate from (
          select date_part('year', ap.duedate)||'/'||date_part ('month', ap.duedate) as period,
                 ac.name,
                 (ap.value*(CASE WHEN ap.typemovement=1 THEN -1 ELSE 1 END)) as value, duedate
          from  accountpayable ap
          inner join accountcategory ac on(ac.id = ap.category)
          where 
            ap.toConciliation = false and
            ap.company = ? and
            ap.unit = ? and
            duedate between ? and ? and
            ap.paid in(? , ?)
          ) as dt
          group by name, period
          order by duedate"""

  val SQL_REPORT_ACCOUNT_MONTH = SQL_REPORT.format("name, period");
  val SQL_REPORT_MONTH_ACCOUNT = SQL_REPORT.format("period, name");
  val SQL_REPORT_CASHIER_OUTS = """select ap.duedate, ac.short_name as category, substr(ap.obs,1,40), 
      ap.value, ap.typemovement, bp.short_name, cs.idForcompany, acc.short_name as account, 
      bc.short_name, bk.short_name
        from     
        accountpayable ap
        left join accountcategory ac on(ac.id=ap.category)
        left join business_pattern as bp on(bp.id=ap.user_c)
        left join cashier cs on(cs.id=ap.cashier)
        left join account acc on(acc.id = ap.account)
        left join cheque ch on (ch.id = cheque)
        left join business_pattern bc on bc.id = ch.customer
        left join bank bk on bk.id = ch.bank
      WHERE 
      ap.toConciliation = false and
      ap.company=? and ap.cashier=? and ap.autocreated = false
    """

  val SQL_DRE = """
    select * from (
    select name,
(
(
  select COALESCE(sum(value),0) as total    
  from accountpayable ap where 
  ap.toConciliation = false and
  ap.category in (
      select id from 
      accountcategory acc 
      where acc.mintreenode between ac.mintreenode and ac.maxtreenode
      and ap.paid=true and ap.typemovement=0 and date(ap.duedate) between date(?) and date(?)
      )
) 
-
(
  select COALESCE(sum(value),0) as total    
  from accountpayable ap where 
  ap.toConciliation = false and
  ap.category in (
      select id from 
      accountcategory acc 
      where acc.mintreenode between ac.mintreenode and ac.maxtreenode
      and ap.paid=true and ap.typemovement=1 and date(ap.duedate) between date(?) and date(?)
      )
)
)
as total
from 
accountcategory ac
where company=?
order by orderinreport
) as data where total <>0
"""
val SQL_DRE_TREE_WITHID = """
select * from (
    select id,name, mintreenode, maxtreenode, parentaccount,isparent,
(
(
  select COALESCE(sum(value),0) as total    
  from accountpayable ap where 
  ap.toConciliation = false and
  ap.company=? and 
  ap.category in (
      select id from 
      accountcategory acc 
      where acc.company=? and acc.mintreenode between ac.mintreenode and ac.maxtreenode
      )
      and ap.typemovement=0 
      %s
      and ap.id in(%s)
) 
-
(
  select COALESCE(sum(value),0) as total    
  from accountpayable ap where 
  ap.toConciliation = false and
  ap.company=? and 
  ap.category in (
      select id from 
      accountcategory acc 
      where acc.company=? and acc.mintreenode between ac.mintreenode and ac.maxtreenode
      )
      and ap.typemovement=1 
      %s
      and ap.id in(%s)
)
)
as total
from 
accountcategory ac
where company=?
order by maxtreenode desc,orderinreport
) as data where total <>0
"""

  val SQL_TREE = """
  select id,name, mintreenode, maxtreenode, parentaccount,isparent
  from 
  accountcategory ac
  where company=?
  and status = 1
  order by orderinreport
  """

}