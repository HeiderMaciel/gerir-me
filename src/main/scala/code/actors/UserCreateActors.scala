package code
package actors

import net.liftweb._
import mapper._
import http._
import actor._
import model.{Company, CompanyUnit, Customer, ProductType, Account, User, Product, Activity, PermissionModule, PaymentType}
import code.util._
import java.util.Random
import java.util.Date

object UserCreateActors extends LiftActor {

  val rand = new Random(System.currentTimeMillis())
  def treat(company:Company) {
    val unit = createUnit(company)
    val user = createUser(company)
    user.unit(unit).save
    EmailUtil sendWelcomeEMail user
    updatePermissionModuleInfos(company)

    createAccount(company)
    createAccountCategoriesForStart(company)
    createCustomerAtCompanyAdmin(company)
    createActivities(company)
    createProducts(company)
    NotficationMessageSqlMigrate.createNotificationMessage(company);
    updateAccessMasterCompany(company)
//    updateInventoryInfos(company)
    LogActor ! "Criando Usuario["+company.name+"] para empresa ["+company.id.is+"] "
    //company.calendarInterval(30)
    company.save
  }

  def updatePermissionModuleInfos(company:Company) = {
    PermissionModuleSqlMigrate.createPermissionModule;
    if (company.appType == Company.SYSTEM_EBELLE) {
      //"ebelle"
    } else if (company.appType == Company.SYSTEM_GERIRME) {
      PermissionModule.setModule (company, "BUDGET");
      PermissionModule.setModule (company, "CRM");
      //"gerirme"
    } else if (company.appType == Company.SYSTEM_ESMILE) {
      PermissionModule.setModule (company, "QUIZ");
      PermissionModule.setModule (company, "BUDGET");
      PermissionModule.setModule (company, "ANVISA");
      PermissionModule.setModule (company, "CRM");

      PermissionModule.resetModule (company, "FIDELITY");
      //"esmile"
    } else if (company.appType == Company.SYSTEM_EDOCTUS) {
      PermissionModule.setModule (company, "QUIZ");
      PermissionModule.setModule (company, "OFFSALE");
      PermissionModule.setModule (company, "ANVISA");
      PermissionModule.setModule (company, "BMINDEX");

      PermissionModule.resetModule (company, "FIDELITY");
      //"edoctus"
    } else if (company.appType == Company.SYSTEM_EGREX) {
      //"egrex"
      PermissionModule.setModule (company, "RELATION");
      PermissionModule.setModule (company, "EVENT");

      PermissionModule.resetModule (company, "FIDELITY");
      PermissionModule.resetModule (company, "PACKAGE");
    } else if (company.appType == Company.SYSTEM_EPHYSIO) {
      PermissionModule.setModule (company, "BPMONTHLY");
      PermissionModule.setModule (company, "QUIZ");
      PermissionModule.setModule (company, "BMINDEX");
      //"ephysio"
    } else if (company.appType == Company.SYSTEM_EBELLEPET) {
      PermissionModule.setModule (company, "RELATION");
      PermissionModule.setModule (company, "QUIZ");
    } else {
      //"vilarika"
    }

  }

/*
  def updateInventoryInfos(company:Company) = {
    InventoryCauseSqlMigrate.createInventoryCause
    InventoryCauseSqlMigrate.updateInventoryInfos
  }
*/
  def updateAccessMasterCompany(company:Company) {
    UtilSqlMigrate.updateAccessForUserCompanyMaster(company)
  }
  
  def createCustomer(company:Company){
    _createCustomer(company).company(company).save()
  }

  def _createCustomer(company:Company) = {
    var name = "";
    if (Customer.testIfDuplicatedName (1, -1, 
      BusinessRulesUtil.clearString(company.name))) {
      name = BusinessRulesUtil.clearString(company.name) + " " + new Date()
    } else {
      name = BusinessRulesUtil.clearString(company.name)
    }
    Customer.create.name(name).phone(company.phone).email(company.email).is_person_?(false)
  }  

  def createCustomerAtCompanyAdmin(company:Company){
    val customer = _createCustomer(company)
    .company(1)
    .idForCompany(company.id.is.toInt)
    .obs("cadastro pelo site")
    .mapIcon(7) // icone exclusivo da 1
    customer.save();
    company.partner(customer)
  }  

  def createUnit(company:Company):Long = {
    val unit = CompanyUnit.create.company(company).name(company.name).showInCalendar_?(true)
    if (company.appType.isEbelle) {
      unit.defaultSex("F") // só o ebelle o default é feminino
    } else {
      unit.defaultSex("N") // os outros é não informado
    }
    unit.save
    unit.id.is
  }
  def createProducts(company:Company){
    val exampletype = ProductType.create.name("Tipo produto exemplo").company(company).createdBy(1).updatedBy(1)
    exampletype.save();
    Product.create.typeProduct(exampletype).name("Produto exemplo").purchasePrice(139.20).salePrice(141.50).minStock(0).commission(10.0).company(company).createdBy(1).updatedBy(1).save()
    Product.create.name("Conta Cliente").purchasePrice(0.0).salePrice(0.0).minStock(0).commission(0.0).company(company).createdBy(1).updatedBy(1).productClass(ProductType.Types.PreviousDebts).save()
    Product.create.name("Crédito Cliente").purchasePrice(0.0).salePrice(0.0).minStock(0).commission(0.0).company(company).createdBy(1).updatedBy(1).productClass(ProductType.Types.CustomerCredits).save()
  }

  def createAccount(company:Company) = {
    Account.create.company(company).name("Padrão: Seu Banco").allowCashierOut_?(false).save//.value(0).save
    Account.create.company(company).name("Caixa").allowCashierOut_?(true).save//.value(0).save    
  }

  def createActivities(company:Company){
    val extype = ProductType.create.typeClass(ProductType.Types.Service).name("Tipo serviço exemplo").company(company).createdBy(1).updatedBy(1)
    extype.save();
    Activity.create.company(company).name("Serviço exemplo").duration("00:30").typeProduct(extype).salePrice(50.0).commission(50).createdBy(1).updatedBy(1).save()
  }

  def createAccountCategoriesForStart (company:Company){
    FinancialSqlMigrate.clearData(company)
    FinancialSqlMigrate.createAccountCategories(company)
    FinancialSqlMigrate.createPaymentType(company)
  }

  def createUser(company:Company) =  User.create.name(BusinessRulesUtil.clearString(company.contact))
      .phone(company.phone)
      .userName(company.contact.split(" ")(0))
      .password(randomNumber+""+randomNumber+""+randomNumber+""+randomNumber+""+randomNumber+""+randomNumber)
      .company(company)
      .showInCalendar_?(true)
      .email(company.email)
      .groupPermission("1") // administrador
  
  def randomNumber = rand.nextInt(10)
  protected def messageHandler = {
    case a:Company => treat(a)
    case _ =>
  }
}


object UtilSqlMigrate {
  val SQL_UPDATE_ACCESS_MASTER_COMPANY = """
      insert into bpcompany 
    ( id,
      business_pattern ,
      company ,
      updatedby,
      createdby ,
      updatedat ,
      createdat ,
      targetcompany ,
      allowed )
     select 
      nextval ('bpcompany_id_seq') as id ,
      nos.id as business_pattern, 
      1 as company ,
      1 updatedby ,
      1 createdby ,
      now () updatedat ,
      now () createdat ,
      company.id as targetcompany ,
      true as allowed 
    from company 
    inner join business_pattern nos on nos.id in (2,3) and nos.company = 1
    where company.id <> 1
    and nos.id not in (select business_pattern from bpcompany bp where bp.targetcompany = company.id);  
  """
  val SQL_UPDATE_COMPANY_IN_COMPANY = """
      update company set company = id where company is null;
  """
  def updateAccessForUserCompanyMaster(company:Company) {
    DB.runUpdate(SQL_UPDATE_ACCESS_MASTER_COMPANY, Nil)
    DB.runUpdate(SQL_UPDATE_COMPANY_IN_COMPANY, Nil)
  }

}

object NotficationMessageSqlMigrate {
  val SQL_CREATE_NOTIFICATIONMESSAGE = """
    insert into notificationmessage 
    (select ic.message, 
    nextval ('notificationmessage_id_seq'), 
    subject,
    now(), now (),co.id,1,1
    from notificationmessage ic
    inner join company co on co.id = ?
    where ic.company = 26 and ic.subject not in (select ic1.subject from notificationmessage ic1 where ic1.company = co.id ));
  """
  def createNotificationMessage (company:Company) {
    val id = company.id.is
    DB.runUpdate(SQL_CREATE_NOTIFICATIONMESSAGE, id :: Nil)
  }
}

object PermissionModuleSqlMigrate {

  val SQL_CREATE_PERMISSIONMODULE = """
    insert into permissionmodule 
    (select ic.name, 
    nextval ('permissionmodule_id_seq'), 
    now(), now (),co.id,1,1, ic.status
    from permissionmodule ic
    inner join company co on co.id <> 26
    where ic.company = 26 and ic.name not in (select ic1.name from permissionmodule ic1 where ic1.company = co.id ));
  """
  def createPermissionModule {
    DB.runUpdate(SQL_CREATE_PERMISSIONMODULE, Nil)
  }
}


/*
object InventoryCauseSqlMigrate {

  val SQL_CREATE_INVENTORY_CAUSE = """
    insert into inventorycause 
    (select ic.name, 
    nextval ('inventorycause_id_seq'), 
    co.id, ic.obs, now(), now (),1,1 
    from inventorycause ic
    inner join company co on co.id <> 26
    where ic.company = 26 and ic.name not in (select ic1.name from inventorycause ic1 where ic1.company = co.id ));
  """
  val UPDATE_INVENTORY_SALE = """
      update company set inventorycausesale = (select id from inventorycause where company = company.id and lower (name) like 'venda%') 
      where (inventorycausesale is null or inventorycausesale = 0);
    """
  val UPDATE_INVENTORY_TRANSFER = """
      update company set inventorycausetrasfer = (select id from inventorycause where company = company.id and lower (name) like 'transf%') 
      where (inventorycausetrasfer is null or inventorycausetrasfer = 0);
    """
  val UPDATE_INVENTORY_PURCHASE = """
      update company set inventorycausepurchase = (select id from inventorycause where company = company.id and lower (name) like 'compra%') 
    where (inventorycausepurchase is null or inventorycausepurchase = 0);
  """
  val UPDATE_INVENTORY_USE = """
      update company set inventorycauseuse = (select id from inventorycause where company = company.id and lower (name) like 'uso%') 
    where (inventorycauseuse is null or inventorycauseuse = 0);
  """
  def updateInventoryInfos {
    DB.runUpdate(UPDATE_INVENTORY_SALE, Nil)
    DB.runUpdate(UPDATE_INVENTORY_TRANSFER, Nil)
    DB.runUpdate(UPDATE_INVENTORY_PURCHASE, Nil)
    DB.runUpdate(UPDATE_INVENTORY_USE, Nil)
  }

  def createInventoryCause {
    DB.runUpdate(SQL_CREATE_INVENTORY_CAUSE, Nil)
  }
}
*/

object FinancialSqlMigrate{

  def clearData(company:Company)={
    DB.runUpdate(CLEAR_ACCOUNTS,company.id.is::Nil)
    DB.runUpdate(CLEAR_PAYMENT_TYPE,company.id.is::Nil)
  }

  def createAccountCategories(company:Company) = {
    val id = company.id.is
    DB.runUpdate(CREATE_ACCOUNT_CATEGORIES,id::id::id::Nil)
  }

  def createPaymentType(company:Company) = {
    val id = company.id.is
    DB.runUpdate(CREATE_PAYMENT_TYPES,id::Nil)
    DB.runUpdate(UPDATE_FACT_INFORMATIONS,id::id::id::id::Nil)
    if (company.appType == Company.SYSTEM_EBELLE) {
      //"ebelle"
      PaymentType.resetPt (company, "%mensalidade%");
    } else if (company.appType == Company.SYSTEM_GERIRME) {
      //
    } 
  }  
  
  val CLEAR_ACCOUNTS = "delete from accountcategory where company =?";

  val CLEAR_PAYMENT_TYPE = "delete from paymenttype where company=?;"

  val CREATE_ACCOUNT_CATEGORIES = """insert into accountcategory
        SELECT name, nextval('accountcategory_id_seq'), color, ?, now(), now(), obs, 1, 
        1, typemovement, userassociated, external_id, search_name,  
        short_name, mintreenode, maxtreenode, "parent_$qmark", orderinreport, 
        parentaccount, isparent, id,fullname, status, parentreg, managerlevel,
        treelevel, treelevelstr
        FROM accountcategory a where a.company=26;
        update accountcategory 
        set parentaccount=(select a.id from accountcategory a where a.aux_id=accountcategory.parentaccount and a.company=?) 
        where company=?;"""

  val CREATE_PAYMENT_TYPES = """insert into paymenttype
                                SELECT name, nextval('paymenttype_id_seq'), ?, now(), now(), 1, 1, 
                                numdays, needchequeinfo, needcardinfo, sumincachier, generatecommision, 
                                deliverycontol, showasoptions, customerregisterdebit, customerdebitsettled, 
                                key_c, order_c, acceptinstallment, comissionatsight, receiveatsight, 
                                receive, numdaysforreceive, percentdiscounttoreceive, defaltaccount, 
                                defaltcategory, day, nextmonth, limitday, defaltdicountcategory,
                                percentdiscounttocommision, sumtoconference, customerusecredit,
                                search_name, short_name, adduseraccounttodiscount, status,
                                showasfinoptions, bpmonthly, offsale, individualReceive, creditcard,
                                cheque, fidelity, allowcustomeraddusertodiscount, autochangetopaid,
                                usernotification
                                FROM paymenttype where paymenttype.company=26;"""

  val UPDATE_FACT_INFORMATIONS = """update paymenttype 
                                    set 
                                    defaltdicountcategory=(select a.id from accountcategory a where a.aux_id=paymenttype.defaltdicountcategory and a.company=?),
                                    defaltcategory=(select a.id from accountcategory a where a.aux_id=paymenttype.defaltcategory and a.company=?),
                                    defaltaccount=(select account.id from account where account.company=? limit 1),
                                    status = 1  
                                    where paymenttype.company=?;"""
}