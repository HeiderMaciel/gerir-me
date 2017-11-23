package code
package model

import net.liftweb._
import mapper._
import code.actors._
import http._
import SHtml._
import util._
import code.util._
import net.liftweb.mapper.{ StartAt, MaxRows, NotBy }
import java.util.regex._
import java.util.Date
import scala.xml.Text
import net.liftweb.proto._
import net.liftweb.common._
import net.liftweb.json._

import _root_.java.math.MathContext

class Company extends Audited[Company] with PerCompany with IdPK with CreatedUpdated with WithCustomer with NameSearchble[Company] with ActiveInactivable[Company] with Siteble {
  def getSingleton = Company
  override def updateShortName = false
  object bpIdForCompany extends MappedInt(this) {
    override def defaultValue = 0 
    /*
      0 não edita
      1 gera e edita
      2 não gera e edita
    */
  }
  object doc extends MappedPoliteString(this, 100)
  object offsaleUsers extends MappedLongForeignKey(this, OffSale)
  object dateExpiration extends EbMappedDate(this)
  object monthlyValue extends MappedDecimal(this, MathContext.DECIMAL64, 2)
  object phone extends MappedString(this, 20)
  object website extends MappedPoliteString(this,150)
  object userActivityAssociate_? extends MappedBoolean(this) {
    override def dbColumnName = "userActivityAssociate"
  }
  object useTreatmentAsAClass_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "usetreatmentasaclass"

  }  
  object contact extends MappedPoliteString(this, 100) with LifecycleCallbacks {
    override def beforeSave() {
      super.beforeSave;
      this.set(BusinessRulesUtil.toCamelCase(this.is))
    }
  }
  object appType extends MappedInt(this) {
      //override def defaultValue = Company.SYSTEM_EBELLE
      /*
      1 - ebelle
      2 - gerirme
      3 - esmile
      4 - edoctus
      5 - egrex
      6 - ephysio
      7 - ebellepet
      */
     def isEbelle = appType.is == Company.SYSTEM_EBELLE
     def isGerirme = appType.is == Company.SYSTEM_GERIRME
     def isEsmile = appType.is == Company.SYSTEM_ESMILE
     def isEdoctus = appType.is == Company.SYSTEM_EDOCTUS
     def isEgrex = appType.is == Company.SYSTEM_EGREX
     def isEphysio = appType.is == Company.SYSTEM_EPHYSIO
     def isEbellepet = appType.is == Company.SYSTEM_EBELLEPET
  }

  def isMedical =
        if (AuthUtil.company.appType.isEdoctus || AuthUtil.company.appType.isEphysio
          || AuthUtil.company.appType.isEsmile) {
          true
        } else {
          false
        }

  def appShortName = if (appType == Company.SYSTEM_EBELLE) {
        "ebelle"
      } else if (appType == Company.SYSTEM_GERIRME) {
        "gerirme"
      } else if (appType == Company.SYSTEM_ESMILE) {
        "esmile"
      } else if (appType == Company.SYSTEM_EDOCTUS) {
        "edoctus"
      } else if (appType == Company.SYSTEM_EGREX) {
        "egrex"
      } else if (appType == Company.SYSTEM_EPHYSIO) {
        "ephysio"
      } else if (appType == Company.SYSTEM_EBELLEPET) {
        "ebellepet"
      } else {
        "vilarika"
      }

  def appCustName (name:String) = {
      if (name.toLowerCase == "cliente") { 
        if (appType == Company.SYSTEM_EBELLE) {
          "Cliente"
        } else if (appType == Company.SYSTEM_GERIRME) {
          "Cliente"
        } else if (appType == Company.SYSTEM_ESMILE) {
          "Paciente"
        } else if (appType == Company.SYSTEM_EDOCTUS) {
          "Paciente"
        } else if (appType == Company.SYSTEM_EGREX) {
          "Membro"
        } else if (appType == Company.SYSTEM_EPHYSIO) {
          "Paciente"
        } else if (appType == Company.SYSTEM_EBELLEPET) {
          "Cliente"
        } else {
          "Cliente"
        }
      } else if (name.toLowerCase == "clientes") {
        if (appType == Company.SYSTEM_EBELLE) {
          "Clientes"
        } else if (appType == Company.SYSTEM_GERIRME) {
          "Clientes"
        } else if (appType == Company.SYSTEM_ESMILE) {
          "Pacientes"
        } else if (appType == Company.SYSTEM_EDOCTUS) {
          "Pacientes"
        } else if (appType == Company.SYSTEM_EGREX) {
          "Membros"
        } else if (appType == Company.SYSTEM_EPHYSIO) {
          "Pacientes"
        } else if (appType == Company.SYSTEM_EBELLEPET) {
          "Clientes"
        } else {
          "Clientes"
        }
      } else {
        println ("Não encontrado appCustName ========= " + name)
        name
      }
  }    
  object appPlan extends MappedInt(this) {
      override def defaultValue = Company.PLAN_CLASSIC
      /*
      1 - free
      2 - solo
      3 - basic
      4 - classic
      5 - elite
      */
     def isFree = appPlan.is == Company.PLAN_FREE
     def isSolo = appPlan.is == Company.PLAN_SOLO
     def isBasic = appPlan.is == Company.PLAN_BASIC
     def isClassic = appPlan.is == Company.PLAN_CLASSIC
     def isElite = appPlan.is == Company.PLAN_ELITE
  }
  object appPlanPaid extends MappedInt(this) {
      override def defaultValue = Company.PLAN_BASIC
  }

  object calendarInterval extends MappedInt(this) {
    override def defaultValue = 30
  }
  object calendarIntervalAlt extends MappedInt(this) {
    override def defaultValue = 15
  }

  object calendarStart extends MappedInt(this) {
    override def defaultValue = 8
  }

  object calendarEnd extends MappedInt(this) {
    override def defaultValue = 18
  }

  object calendarShowId_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "calendarShowId"
  }
  object calendarShowPhone_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "calendarShowPhone"
  }
  object calendarShowLight_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "calendarShowLight"
  }
  object calendarShowInterval_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "calendarShowInterval"
  }
  object calendarShowActivity_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "calendarShowActivity"
  }
  object calendarShowDifUnit_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "calendarShowDifUnit"
  }

/*
  object senNotifications_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "senNotifications"
  }
*/
  object financialNotification extends MappedInt(this) {
    override def defaultValue = 0
  }
  object financialNotification2 extends MappedInt(this) {
    override def defaultValue = 0
  }
  // uma vez só mesmo
  object userNotification extends MappedInt(this){
      override def defaultValue = 0
  }    
  object customerNotification extends MappedInt(this){
      override def defaultValue = 0
  }    
  object customerNotification2 extends MappedInt(this){
      override def defaultValue = 0
  }    
  object customerBirthdayNotification extends MappedInt(this){
      override def defaultValue = 0
  }
  // id do email enviado auto    
  object customerBirthdayNotificationId extends MappedLong(this)

  object animalBirthdayNotification extends MappedInt(this){
      override def defaultValue = 0
  }    
  // id do email enviado auto    
  object animalBirthdayNotificationId extends MappedLong(this)

  object calendarPub extends MappedInt(this){
      override def defaultValue = 0
  }    
  object calendarUrl extends MappedPoliteString(this, 100)

// tá lá em baixo
//  val BEFORE1DAY8PM = 1
//  val TODAY1AM = 2
//  val TODAY7AM = 3
//  val BEFORE2DAY8PM = "1"
//  val TODAY7AM = "1"
// criar outrs campos para notificar no dia ou criar comobinacoes

/*
  object autoIncrementCommand_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "autoincrementcommand"
  }
*/

  object commandControl extends MappedInt(this){
    override def defaultValue = 1;
    /*
    0 não incrementa (never)
    1 incrementa no dia (daily)
    2 incrementa sequencial sempre (ever)
    */
  }

  object allowRepeatCommand_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "allowrepeatcommand"
  }

  object autoOpenCalendar_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "autoopencalendar"
  }
  object showSalesToUser_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "showsalestouser"
  }

  object email extends MappedString(this, 150) with LifecycleCallbacks {
    override def beforeSave() {
      super.beforeSave;
      if (hasCompanyWhithEMail()) {
        throw new RuntimeException("E-mail já cadastrado em outra empresa!")
      }
      this.get.split(",|;").foreach((email1) => {
        if (!emailPattern.matcher(email1).matches && !isNew) {
          throw new RuntimeException("E-mail inválido! " + email1)
        }
      })
      this.set(this.get.toLowerCase.trim)
    }
    def emailPattern = ProtoRules.emailRegexPattern.vend
    def unicValidation: List[FieldError] = {
      if (!hasCompanyWhithEMail()) {
        List[FieldError]()
      } else {
        List(FieldError(this, Text("E-mail já cadastado em outra empresa! Favor entrar em contato através de suporte@vilarika.com.br ou 31-99169-3247 (whatsapp)")))
      }
    }
    def emailValidation: List[FieldError] = {
      var strAux = "";
      //strAux = "";
      email.split(",|;").foreach((email1) => {
        if (!emailPattern.matcher(email1).matches && !isNew) {
          strAux += "E-mail inválido! " + email1
        } else {
          strAux += ""
        }
      })
      if (strAux == "") {
          List[FieldError]()
        } else {
          List(FieldError(this, strAux))
        }
    }
    override def validate =
      emailValidation ::: unicValidation ::: Nil
  }
  //User company
  object user extends MappedLongForeignKey(this, User)
  object partner extends MappedLongForeignKey(this, Customer)

  object bpmStartDay extends MappedInt(this) {
    override def defaultValue = 0
    // 0 dia da venda
    // outro determina dia de início
  }
  object bpmDaysToAlert extends MappedInt(this) {
    override def defaultValue = 7
  }
  object bpmDaysToEmail extends MappedInt(this) {
    override def defaultValue = 7
  }
  object bpmCommissionOnSale_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "bpmCommissionOnSale"
  }
  object bpmCommissionOnReady_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "bpmCommissionOnReady"
  }
  object bpmCommissionOnMissed_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "bpmCommissionOnMissed"
  }

  object offCommissionOnReady_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "offCommissionOnReady"
  }
  object offCommissionOnMissed_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "offCommissionOnMissed"
  }

  object packCommissionOnReady_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "packCommissionOnReady"
  }

  object packCommissionOnMissed_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "packCommissionOnMissed"
  }

  object categoryOnProduct_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "categoryOnProduct"
  }

  object toCancelAnAppointment extends MappedPoliteString(this,255)
  object expenseReceiptObs extends MappedPoliteString(this,2000)
  object recordsCustInfo extends MappedPoliteString(this,2000)

  def users = User.findAllInCompanyOrdened

  def usersForCalendar(onlyCurrenunit: Boolean = true): List[User] = 
  if ((AuthUtil.user.isSimpleUserCalendar) && (!AuthUtil.user.isCalendarUser)) {
    List(AuthUtil.user)
  } else {
//    if (onlyCurrenunit)
//      User.fildAllInUnit(By(User.showInCalendar_?, true), (By(User.userStatus, User.STATUS_OK)), OrderBy(User.orderInCalendar, Ascending), OrderBy(User.short_name, Ascending))
      User.findAllInCompany(
        BySql(" (unit = ? or (id in (select uu.user_c from usercompanyunit uu where uu.unit = ? and uu.company = ?))) ",IHaveValidatedThisSQL("",""), AuthUtil.unit.id, AuthUtil.unit.id, AuthUtil.company.id),
        By(User.showInCalendar_?, true), (By(User.userStatus, User.STATUS_OK)), 
        OrderBy(User.orderInCalendar, Ascending), OrderBy(User.short_name, Ascending))
 //   else
 //     User.findAllInCompany(By(User.showInCalendar_?, true), (By(User.userStatus, User.STATUS_OK)), OrderBy(User.orderInCalendar, Ascending), OrderBy(User.short_name, Ascending))
  }

  object accountCategoryTrasfer extends MappedLong(this) {
  }
  /**
   * for payment of professionals
   */
  object accountCategoryPayroll extends MappedLong(this)
  object inventoryCauseSale extends MappedLongForeignKey(this, InventoryCause){
    override def defaultValue = 4;
  }
  object inventoryCausePurchase extends MappedLong(this) {
    override def defaultValue = 5;
  }
  object inventoryCauseTrasfer extends MappedLong(this) {
    override def defaultValue = 6;
  }
  // produto pra geracao de taxa de serviço tipo 10% do garçom
  object serviceChargeProduct extends MappedLong(this)
  // nao usa dropar coluna
  //object productPreviusDebt extends MappedLong(this)
  object inventoryCauseUse extends MappedLongForeignKey(this, InventoryCause) {
    override def defaultValue = 7;
  }

  // to explain status
  object obs extends MappedPoliteString(this,255)

  def activities (calendarPub:Boolean) = {
    if (calendarPub) {
      Activity.findAllActive(
        By(Activity.company, this.id), 
        By(Activity.showInCalendarPub_?, true), 
        OrderBy(Activity.name, Ascending))
    } else {
      Activity.findAllActive(
        By(Activity.company, this.id), 
        OrderBy(Activity.name, Ascending))
    }
  }

  def customers = Customer.findAllInCompany()

  def customerCount(params: List[QueryParam[Customer]]) =
    Customer.count(By(Customer.company, this.id) :: params: _*)

  def hasCompanyWhithEMail() = {
    Company.count(NotBy(Company.id, this.id), By(Company.email, email)) > 0
  }

  def userPagination(params: List[QueryParam[User]]) = User.findAllInCompanyOrdened
  def count[T <: net.liftweb.mapper.LongKeyedMapper[T]](obj: OnlyCurrentCompany[T], params: List[QueryParam[T]]) =
    obj.countInCompany(params: _*)

  def pagination[T <: net.liftweb.mapper.LongKeyedMapper[T]](obj: OnlyCurrentCompany[T], params: List[QueryParam[T]]) =

    obj.findAllInCompany(params: _*)

  def paginationWithInactive[T <: net.liftweb.mapper.LongKeyedMapper[T]](obj: OnlyActive[T], params: List[QueryParam[T]]) =
    obj.findAllInCompanyWithInactive(params: _*)

  def customersPagination(params: List[QueryParam[Customer]]) = {
    val ret = Customer.findAll(By(Customer.company, this.id) :: params: _*)
    ret
  }

  def products = Product.findAllInCompany(OrderBy(Product.name, Ascending))

  def productTypes = ProductType.findAllInCompany(OrderBy(ProductType.name, Ascending))

  def bmMonthlyPaymentType = {
    if (PaymentType.count(By(PaymentType.bpmonthly_?, true)) < 1) {
       throw new RuntimeException("É preciso que haja uma forma de pagamento marcada com comportamento especial de <baixa de sessão de mensalidade>" + "\n\n" +
        "E ela precisa estar ativa, para que seja gerada a comissão para o profissional");
    }
    PaymentType.findAllInCompany(By(PaymentType.bpmonthly_?, true))(0)
  }  
  def offSalePaymentType = {
    if (PaymentType.count(By(PaymentType.offSale_?, true)) < 1) {
       throw new RuntimeException("É preciso que haja uma forma de pagamento marcada com comportamento especial de <pagamento por convênio>" + "\n\n" +
        "E ela precisa estar ativa, para que seja gerada a comissão para o profissional");
    }
    PaymentType.findAllInCompany(By(PaymentType.offSale_?, true))(0)
  }

  def packagePaymentType = {
    if (PaymentType.count(By(PaymentType.deliveryContol_?, true)) < 1) {
       throw new RuntimeException("É preciso que haja uma forma de pagamento marcada com comportamento especial de <baixa de sessão de pacote>" + "\n\n" +
        "E ela precisa estar ativa, para que seja gerada a comissão para o profissional");
    }
    PaymentType.findAllInCompany(By(PaymentType.deliveryContol_?, true))(0)
  }

  def treatmentsToDay = Treatment.findAll(By(Treatment.company, this.id), By(Treatment.hasDetail, true), BySql("date(start_c) = date(?)", IHaveValidatedThisSQL("start_c", "01-01-2012 00:00:00"), new Date()))

  def findCustomerByKey(id: Long) = {
    Customer.find(By(Customer.company, this.id), By(Customer.id, id)) match {
      case Full(customer) => Full(customer)
      case _ => Customer.find(By(Customer.company, this.id), By(Customer.idForCompany, id.toInt))
    }
  }

  def findChequeByKey(id: Long) = {
    Cheque.find(By(Cheque.company, this.id), By(Cheque.id, id))
  }

  // este métodos estão mal colocados aqui - RESOLVER
  def paymentTypes = PaymentType.findAllInCompany(OrderBy(PaymentType.name, Ascending))

  def chequesNotReceived = Cheque.findAllInCompany(By(Cheque.received, false), 
    By(Cheque.movementType, AccountPayable.IN), OrderBy(Cheque.dueDate, Descending))

  def accountCategorys = AccountCategory.findAllInCompany(By(AccountCategory.company, this.id))
  // acho que não usa
  def accountPayablesxxxxx = AccountPayable.findAllInCompany(By(AccountPayable.company, this.id))
  def imagePath = "company"
  override def logo_web = Props.get("photo.urlbase").get + imagePath + "/" + image.is
  override def thumb_web = Props.get("photo.urlbase").get + imagePath + "/" + imagethumb
  def mainUnit = {
    CompanyUnit.findAll(OrderBy(CompanyUnit.id, Ascending),By(CompanyUnit.company, this), By(CompanyUnit.showInCalendar_?, true))(0)
  }

  def calPubCompany (company:String) = {
    if (company == "") {
      1l
    } else {
      // testar aqui número e buscar na company pela url
      if (BusinessRulesUtil.isNumeric(company)) {
        company.toLong
      } else {
        val clist = Company.findAll(
          By (Company.status, 1),
          By (Company.calendarUrl,company))   
        if (clist.length > 0) {
          clist (0).id.is
        } else {
          1l;
        }
      }
    }
  }
}

object Company extends Company with LongKeyedMapperPerCompany[Company] with SitebleMapper[Company] {
  val SYSTEM_EBELLE = 1
  val SYSTEM_GERIRME = 2
  val SYSTEM_ESMILE = 3
  val SYSTEM_EDOCTUS = 4
  val SYSTEM_EGREX = 5
  val SYSTEM_EPHYSIO = 6
  val SYSTEM_EBELLEPET = 7

  val PLAN_FREE = 1
  val PLAN_SOLO = 2
  val PLAN_BASIC = 3
  val PLAN_CLASSIC = 4
  val PLAN_ELITE = 5

  val CmdNever = 0
  val CmdDaily = 1
  val CmdEver = 2;
  val CmdDailyCompany = 3
  val CmdEverCompany = 4;

  // public calendar
  val CPubNoone = 0
  val CPubCustomer = 1
  val CPubEveryone = 2;

  override def findByKey(id: Long) = {
    val company = super.findByKey(id)
    company match {
      case Full(c) => {
        if (c.isInactive) {
          throw new RuntimeException("Empresa inativa!");
        }
      }
      case _ =>
    }
    company
  }
  def createFromJson(json: JsonAST.JObject) = decodeFromJSON_!(json, true)
  def updateFromJson(json: JsonAST.JObject) = {
    json.values.map((value) => {
      this.fieldByName(value._1).get.set(value._2)
    })
    this
  }
 
  val SQL_REPORT_DATA = """
            select 
            (
            select count(1) from business_pattern  where is_customer =true and company=c.id
            ) as customers,
            (
            select count(1) from business_pattern  
            where is_customer =true and company=c.id
            and id in(select distinct customer from treatment  where company=c.id and date_part ('month', start_c) = date_part ('month', date (date_trunc ('month', now())) + interval '0 month'))
            ) as cutomer_this_month,
            (
            select count(1) from business_pattern  where is_user = true and company=c.id and userStatus=1
            ) as users,
            (
            select count(1) from product where productclass  = 1 and company=c.id
            ) as product,
            (
            select count(1) from product where productclass  = 0 and company=c.id
            ) as services,
            (
            select count(1) from treatment where status=4 and company=c.id
            ) as treatments,
            (
            select count(1) from treatment where status=4 and company=c.id and date_part ('month', start_c) = date_part ('month', date (date_trunc ('month', now())) + interval '0 month')
            ) as treatments_this_month
            from company c where c.id=?
    """
  val notifyNever = 0
  val notifyForTomorrow = 1
  val notifyForToday1Am = 2
  val notifyForToday7Am = 3
  val notifyFor2day = 4
  val notifyFor4day = 5

  def findAllActiveToSendNotification (notify : Int) = findAll(
    BySql ("(financialNotification = ? or customernotification = ? or usernotification = ?)",IHaveValidatedThisSQL("",""), notify, notify, notify), 
    By(Company.status, Company.STATUS_OK),
    OrderBy (id,Ascending))

  def findAllActiveToChangeFinancialToPaid = findAll(
    BySql ("id in (select pt.company from paymenttype pt where pt.autoChangeToPaid = true and pt.company = company.id)",IHaveValidatedThisSQL("","")), 
    By(Company.status, Company.STATUS_OK),
    OrderBy (id,Ascending))

}
