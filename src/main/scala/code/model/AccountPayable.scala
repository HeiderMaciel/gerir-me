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
import java.util.Calendar

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
        var cal = Calendar.getInstance();
        cal.setTime(dueDate.is);
        cal.add(Calendar.DATE, 1);
        val toDate = cal.getTime();  
        // data de competencia normalmente é igual ou inferior ao vencimento      
        // o sistema permite um dia maior para o caso de salão que paga 
        // comissão no final do dia e arredonda, e lança com venc hj o 
        // arredondado com competencia amanhã para descontar amanhã
        //
        // out/2018 rigel
        // feita outra alteracao liberando data de competencia para frente
        // para o que for associado ao profissional
        if(this.get > toDate && !categoryUserAssociated){
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
          if (isNew && !paid_?) {
            // rigel 13/09/2017 - novo e não pago e é cheque
            // é o caixa gerando cheque no financeiro, se o cheque foi 
            // pago e o caixa reaberto ao fechar novamente gera o 
            // financeiro com o status do cheque
            // ou é um lancamento passando um cheque para alguém
            // se o lançamento tava não pago que é pouco provavel,
            // seta com o status do cheque
            paid_?.set(c.received)
          } else {
            if (!paid_?.is) {
              println ("vaiii +++++++++++++++++++++++++++ TRUE ")
              paymentDate.set (Project.strToDateOrNull(""))
            } else {
              println ("vaiii +++++++++++++++++++++++++++ false ")
            }
            // se não é novo ou se é pago o status do lançamento 
            // determina o status do cheque sempre, se for saída ou se for lancamento de caixa autocreated
            if (typeMovement == AccountPayable.OUT || autoCreated_?) {
              def efetivePaymentDate:Box[Date]  = {
                if (fieldOwner.paid_?.is) {
                  Full(fieldOwner.dueDate) 
                } else {
                  Empty
                }
              }
              if (c.received && !fieldOwner.paid_?.is) {
                var obsAux = c.obs;
                c.obs ((obsAux + " Revertido " + AuthUtil.user.short_name.is + " " + Project.dateTimeToStr(new Date())).trim)
              }
              c.received(fieldOwner.paid_?.is).efetivePaymentDate.setFromAny(efetivePaymentDate)
              c.save
              // procura outro lançamento com este cheque
              // se for o caso só deve ter um mesmo e autoCreated
              // gerado pelo fechamento de caixa
              // importante que se a empresa paga um fornecedor com
              // com cheque de cliente e ele volta o lançamento que 
              // deve ser alterado é o feito ao fornecedor e o do caixa 
              // vai ser tratado aqui e não o contrário
              val aclist = AccountPayable.findAllInCompany (
                NotBy(AccountPayable.id, id),
                By(AccountPayable.cheque, cheque),
                By(AccountPayable.autoCreated_?, true))
              aclist.foreach((ap) => {  
                if (paid_?) {
                  ap.makeAsPaid 
                } else {
                  ap.makeAsUnPaid
                }
              });
            } else {
              // quando é entrada manual o cheque está sendo recebido e 
              // está na casa
            }
          }
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

  object autoCreated_? extends MappedBoolean(this) {
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

  // rigel 03/08/2017 
  object aggregateId extends MappedLong(this)
  object aggregateValue extends MappedCurrency(this.asInstanceOf[MapperType])

  // rigel 27/10/2017
  object aggregateLabel extends MappedPoliteString(this, 255)
  object ofxId extends MappedPoliteString(this, 255) {
    override def dbIndexed_? = true
  } 

  def aggregate (aggregId : Long)= {
    if (this.aggregateId != 0 && this.aggregateId != aggregId) {
      throw new RuntimeException("Um lançamento não pode fazer parte de duas agregações!")
    }
    this.aggregateId(aggregId)
    if (this.id == aggregId) {
        this.aggregateValue (this.aggregateValue.is + this.value.is)
        this.aggregateLabel ((bpShortName + " " + categoryShortName + "=== Agregado  ").trim)
    } else {
       var vaux = AccountPayable.findByKey(aggregId).get.aggregateValue.is
       AccountPayable.findByKey(aggregId).get.
         aggregateValue(vaux + this.value).partiallySecureSave;
    }
    this.partiallySecureSave
  }
    
  def thisUnit : CompanyUnit = {
    CompanyUnit.findByKey (this.unit.obj.get.id.toLong).get;
  }

  def makeAsPaid = this.paid_?(true).partiallySecureSave

  def makeAsUnPaid = {
    this.paid_?(false).
    paymentDate (Project.strToDateOrNull("")).
    partiallySecureSave
  }
    
  def makeAsConciliated = {
    if (this.conciliate.is == 0) {
      this.conciliate(1).partiallySecureSave
    } else {
      throw new RuntimeException("Só lançamentos em aberto podem ser conciliados!")
    }
  }

  def makeAsConsolidated = this.conciliate(2).partiallySecureSave

  lazy val accountBox = this.account.obj
  lazy val accountShortName:String = {
      accountBox match {
          case Full(c)=> c.short_name.is
          case _ => ""
      }
  }

  lazy val chequeDesc:String = {
      cheque.obj match {
        case Full(c:Cheque) => {
          c.chequeDesc
        }
        case _ => ""
      }
  }

  lazy val categoryBox = this.category.obj

  lazy val categoryUserAssociated:Boolean = {
      categoryBox match {
          case Full(c)=> c.userAssociated.is
          case _ => false
      }
  }

  lazy val categoryShortName:String = {
      categoryBox match {
          case Full(c)=> c.short_name.is
          case _ => ""
      }
  }

  lazy val bpBox = this.user.obj
  lazy val bpShortName:String = {
      bpBox match {
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
          val au = a.getAccountUnit (thisUnit)
          au.removeRegister(this, "Alt Conta  " + accountShortName + " cat " + categoryShortName)
        }
        case _ => 
      }
      debted_?(false)
    }
  }

  override def delete_! = {
    if(AuthUtil.? && (!AuthUtil.user.isAdmin && !AuthUtil.user.isFinancialManager)){
      throw new RuntimeException("Somente Administradores e Gerentes Financeiros podem excluir lançamentos")
    }
    if ((!cashier.isEmpty) && (Cashier.checkifclosed(cashier))) {
      throw new RuntimeException("Não é permitido excluir lançamento de caixa fechado")
    }
    if (conciliate > 0) {
      if(AuthUtil.? && (!AuthUtil.user.isAdmin)){
        throw new RuntimeException("Somente Administradores podem excluir lançamentos conciliados")
      }
    }
    if(debted_?.is){
      accountBox match {
        case Full(a: Account) => {
          val au = a.getAccountUnit (thisUnit)
          au.removeRegister(this, "Excluindo lançamento conta " + accountShortName + " cat " + categoryShortName)
        }
        case _ => 
      }
    }
    // reseta recebimento de cheque se o lançamento for excluido
    // desde que o lancto não seja auto - gerado pelo caixa
    if (!autoCreated_?) {
      cheque.obj match {
        case Full(c:Cheque) => {
          if (c.received) {
            var obsAux = c.obs;
            c.obs ((obsAux + " Revertido " + AuthUtil.user.short_name.is + " " + Project.dateTimeToStr(new Date())).trim)
          }
          c.received (false)
          c.efetivePaymentDate (null)
          c.save
          // procurar outros lancamento com este cheque e reverter tb
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
        val au = a.getAccountUnit (thisUnit)
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
        val au = a.getAccountUnit (thisUnit)
        au.removeRegister(this, "Alt Status Lanç conta " + accountShortName + " cat " + categoryShortName)
      }
      case _ => 
    }
    debted_?(false)    
  }

  def partiallySecureSave = {
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
      val au = a.getAccountUnit (thisUnit)
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

    // quando o laçamento tem um cheque vinculado, se o valor tá zero
    // seta o valor do cheque automaticamente
    if (value == 0.0) {
      cheque.obj match {
        case Full(c:Cheque) => {
          value.set (c.value.is.toDouble)
        }
        case _ => 
      }
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
      } else {

      }
      if (this.paid_?.is) {
        super.save
        val a = Account.findByKey(this.account.is).get
        val au = a.getAccountUnit (thisUnit)
        au.register(this)
      } else {
        super.save
      }
    }
  }

  def transferTo(toAccount:Long, toout_of_cashier:String, cashierTo:String, cashier_numberTo:String){
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
    if(toout_of_cashier.toBoolean) {
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

/*
  def prevBalance (account: Account, paymentDate : Date) = {
    val categoryBC = AccountCategory.balanceControlCategory;
    AccountPayable.findAllInCompany (By (AccountPayable.account, account)
      BySql (""" and paymentdate in
        (select max(ah1.paymentdate) from accounthistory ah1 where ah1.company = ah.company and ah1.account = ah.account
        and ah1.paymentdate < ? and accountcategory = ?)
        """, IHaveValidatedThisSQL("", ""), account, paymentDate));
  }
*/

  def conCilSol (id : String, idofx : String, 
    aggreg : Boolean, conciliation : Int, partial : Boolean) = {
    val apofx = AccountPayable.findByKey(idofx.toLong).get
    var aplist = if (!aggreg) {
      AccountPayable.findAllInCompany(
        By(AccountPayable.id, id.toLong))
    } else {
      AccountPayable.findAllInCompany(
        By(AccountPayable.aggregateId, id.toLong))
    }
    val ap = AccountPayable.findByKey(id.toLong).get
    var partialValue = ap.aggregateValue;
    var dif = apofx.value - ap.aggregateValue
/*
    if (dif < 0.0 && partial) {
      throw new RuntimeException("Conciliação/Consolidação Parcial só é possível se valor do ofx for maior que o valor agregado")
    }
*/
    aplist.map((apl) => {
      if (!apl.paid_?) {
        apl.paid_? (true);
      }
      // no ofx duedate sempre = data pagamento
      // o arq é importado sem pagto para não alterar saldo 
      // por isso usa duedate
      apl.paymentDate (apofx.dueDate)
      apl.account (apofx.account)
      apl.ofxId (apofx.ofxId)
      var compl = apl.complement
      apl.complement (compl + " " + apofx.obs)
      if (conciliation == 1) {
        apl.makeAsConciliated
      } else {
        apl.makeAsConsolidated
      }
    });
    val auxTm:Int = if (apofx.value > ap.aggregateValue) {
        apofx.typeMovement
      } else if (apofx.typeMovement == AccountPayable.IN) {
        AccountPayable.OUT
      } else {
        AccountPayable.IN
      }

    if (aggreg && dif != 0.0 && !partial) {
      if (dif < 0.0) {
        dif = dif * -1
      }
      val ap1 = AccountPayable.createInCompany
      .account (apofx.account) // ofx mesmo
      .paymentDate (apofx.dueDate)
      .typeMovement(auxTm) 
      .category (ap.category)
      .dueDate (ap.dueDate)
      .paymentType (ap.paymentType)
      .value (dif)
      .paid_? (true)
      .complement ((ap.complement + " " + apofx.obs).trim)
      .obs ("====complemento agregado " + ap.obs)
      ap1.save
      if (conciliation == 1) {
        ap1.makeAsConciliated
      } else {
        ap1.makeAsConsolidated
      }
    }
    if (partial) {
      if (aggreg && dif != 0.0) {
        val auxOfxTm:Int = if (apofx.value > partialValue) {
            apofx.typeMovement
          } else if (apofx.typeMovement == AccountPayable.IN) {
            AccountPayable.OUT
          } else {
            AccountPayable.IN
          }
        val auxOfxValue:Double = if (apofx.value > partialValue) {
            apofx.value - partialValue
          } else {
            partialValue - apofx.value
          }

        apofx.value(auxOfxValue).
        typeMovement(auxOfxTm).save
      } else {
        apofx.delete_!
      }
    } else {
      apofx.delete_!
    }
  }

  def consolidate(accountId:Long, value:Double, paymentStart: Date,
    paymentEnd: Date) = {
    val account = Account.findByKey (accountId).get
    val categoryBC = AccountCategory.balanceControlCategory;
    val apList = AccountPayable.findAllInCompany (
      By(AccountPayable.account, account),
      By(AccountPayable.toConciliation_?,false),
      By(AccountPayable.paid_?, true),
      By(AccountPayable.unit, AuthUtil.unit),
      BySql(" date(paymentDate) between date(?) and date(?) ", 
        IHaveValidatedThisSQL("", ""), paymentStart, paymentEnd)
      )
    apList.foreach((ap) => {  
      ap.makeAsConsolidated
      ap.partiallySecureSave
    });

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
      .exerciseDate(paymentEnd)
      .dueDate(paymentEnd)
      .paymentDate(paymentEnd)
      .obs("Gerado pelo processo de consolidação")
      .account(accountId)
      .paid_?(true)
      .autoCreated_?(true)
      //.cashier(cashier.id.is)
      //.paymentType(paymentType.id.is)
      .costCenter(AuthUtil.unit.costCenter.is)
      .value(val1)
      account1.save
      account1.makeAsConsolidated
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
      BySql(sqldt, IHaveValidatedThisSQL("", ""), start, end),
      OrderBy(AccountPayable.dueDate, Ascending),
      OrderBy(AccountPayable.aggregateId, Descending),
      OrderBy(AccountPayable.aggregateValue, Descending),
      OrderBy(AccountPayable.id, Descending)
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
                          // rigel 12/09/2018 - coloquei a company aqui
                          By(AccountPayable.company, AuthUtil.company.id.is),
                          By(AccountPayable.recurrence, recurrence.id.is), 
                          By(AccountPayable.dueDate, date)
                      )
  }

  def findAllToChangeToPaid(start: Date, company: Company) = {
    var aplist = AccountPayable.findAll(
      By(AccountPayable.company, company),
      By(AccountPayable.dueDate, start), 
      By(AccountPayable.paid_?, false), 
      BySql("""paymenttype in (select pt.id from paymenttype pt where
        pt.receive = true and pt.receiveatsight = false and pt.autoChangeToPaid = true
        and pt.company = ?)""", IHaveValidatedThisSQL("dueDate", "01-01-2012 00:00:00"),
        company.id.is.toLong)
      )
    aplist.map((ap) => {
      if (!ap.paid_?) {
        //ap.paid_? (true).save;
        ap.makeAsPaid;
      }
    });
  }
  
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

val SQL_DRE_TREE_WITHID = """
select * from (
    select id,name, mintreenode, maxtreenode, parentaccount,isparent,
(
(
  select COALESCE(sum(case when ap.typemovement = 0 then ap.value when ap.typemovement = 1 then ap.value * (-1) end),0) as total    
  from accountpayable ap where 
  ap.toConciliation = false and
  ap.company=? and 
  ap.category in (
      select id from 
      accountcategory acc 
      where acc.company=? and acc.mintreenode between ac.mintreenode and ac.maxtreenode
      )
      %s
      and ap.id in(%s)
) 
)
as total
from 
accountcategory ac
where company=?
    and (ac.status = 1 or ac.updatedat >=  date(?))
order by maxtreenode desc,orderinreport
) as data where total <>0
"""

}