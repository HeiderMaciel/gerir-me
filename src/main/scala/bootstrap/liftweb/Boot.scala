package bootstrap.liftweb

import net.liftweb._
import util.{Helpers, Props}
import Helpers._

import common._
import http._
import scalate._
import sitemap._
import Loc._
import mapper._
import net.liftweb.http.provider._
import code.model._
import code.actors._
import code.util._
import code.api._
import code.service._
import javax.mail._
import javax.mail.internet._
import net.liftweb.util.Mailer
import Mailer._
import net.liftweb.widgets.autocomplete.AutoComplete

//import code.util.QuartzUtil;
/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
   val logger = Logger(classOf[Boot])
  def boot {
    LiftRules.resourceNames = "i18n/ebelle" :: LiftRules.resourceNames
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
	new MyStandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
			     Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }
    // lets add Scalate
    /*val scalateView = new ScalateView
    scalateView.register*/
    configMailer(Props.get("mail.smtp") openOr "",
                 Props.get("mail.user") openOr "", 
                 Props.get("mail.password") openOr "",
                 Props.get("mail.port") openOr ""
                 )
    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you wan
    Schemifier.schemify(true, Schemifier.infoF _, Company, WorkHouer, Treatment,TreatmentDetail,Payment,UserActivity,
      ProductType,Product,InventoryMovement,Cashier,PaymentDetail, PaymentType,Cheque,AccountPayable,AccountCategory,
      PermissionModule,LogMailSend,LocationHistory,ImageCustomer, CompanyUnit, InventoryCurrent,BusyEvent, UserGroup, 
      UserMessage, UserMessageLogRead,Commision, Recurrence, ProductBOM, DeliveryControl, DeliveryDetail, ProductLine, 
      ProductLineTag, Account,BusinessPatternConsideration, BusinessPatternAccount, Bank, InventoryCause, UserAcessCompany, 
      Monthly, OffSale, OffSaleProduct, CommisionDetails, PayrollEvent, BusinessPatternPayroll, CustomerAccountHistory, 
      AccountHistory, CostCenter, SqlCommand, UnificationCustomerHistory, NotificationMessage, User, Terms, Media, 
      ExtSession, UnitPartner, Activity, Customer, City, State, MapIcon, BpMonthly, CivilStatus, PaymentCondition)

    Schemifier.schemify(true, Schemifier.infoF _, AgeRange, AgeRangeInterval, UnitofMeasure, UserUserGroup, InvoiceGroup, Icd, 
        TreatEdoctus, TdEdoctus, TdEpet, DomainTable)

    Schemifier.schemify(true, Schemifier.infoF _, JobRequisition, Quiz, QuizSection, QuizQuestion, QuizDomain, QuizDomainItem,
      QuizApplying, QuizAnswer, UserCompanyUnit, AccountCompanyUnit, Breed, Species)

    Schemifier.schemify(true, Schemifier.infoF _, ProjectStage, ProjectType, ProjectClass, Project1, StakeHolderType, StakeHolder, 
      ProjectTreatment, ProjectSection, Contact)
  
    Schemifier.schemify(true, Schemifier.infoF _, RelationshipType, Relationship, BpRelationship, InstructionDegree, Occupation,
      Invoice, InvoiceTreatment)
    
    // Add a query logger
    /*DB.addLogFunc {
      case (log, duration) => {
        logger.debug("Total query time : %d ms".format(duration))
        log.allEntries.foreach {
          case DBLogEntry(stmt,duration) =>{
            if(!stmt.contains("INSERT") && stmt.toUpperCase.contains("SELECT")){
              LogActor ! "  %s in %d ms".format(stmt, duration)
              logger.debug("  %s in %d ms".format(stmt, duration))
            }
          }
        }
      }
    }*/
    // where to search snippet
    LiftRules.addToPackages("code")

    def toAccessDenied = {
      if(AuthUtil.?)
        RedirectResponse("/docs/access_denied.html")
      else
        if (S.hostName.contains ("local")) {
          RedirectResponse("/v2/login")
        } else {
          RedirectResponse("/v2/login")
          //RedirectResponse("http://45.33.99.152:7171/v2/login")
        }
    }
    
    val loggedIn = If( ()=> AuthUtil ?,
                       ()=> toAccessDenied
                      )

    val superAdmin = If( ()=> ( AuthUtil.? && AuthUtil.user.isSuperAdmin),
                       ()=> toAccessDenied
                      )

    val unLoggedIn = If( ()=> AuthUtil !, 
                         ()=> toAccessDenied
                      )

    val financialAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.financial_?),
                            ()=> toAccessDenied
                          )


    val financialManagerAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.financialManager_?),
                            ()=> toAccessDenied
                          )

    val peopleManagerAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.peopleManager_?),
                            ()=> toAccessDenied
                          )

    val customerAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.customer_?),
                            ()=> toAccessDenied
                          )

    val recordsAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.records_?),
                            ()=> toAccessDenied
                          )

    val inventoryAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.inventory_?),
                            ()=> toAccessDenied
                          )
    val inventoryManagerAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.inventoryManager_?),
                            ()=> toAccessDenied
                          )
    val crudeAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.crude_?),
                            ()=> toAccessDenied
                          )

    val reportAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.report_?),
                            ()=> toAccessDenied
                          )

    val serviceAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.service_?),
                            ()=> toAccessDenied
                          )
    val serviceManagerAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.serviceManager_?),
                            ()=> toAccessDenied
                          )

    val treatmentAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.treatment_?),
                            ()=> toAccessDenied
                          )

    val reportNotEgrexAccess = If(
                            ()=> (AuthUtil.? && !AuthUtil.company.appType.isEgrex && PermissionModule.report_?),
                            ()=> toAccessDenied
                          )    

    val calendarAccess = If(
                            ()=> (AuthUtil.? && ( PermissionModule.calendar_? || 
                              AuthUtil.user.isSimpleUserCalendar ||
                              AuthUtil.user.isSimpleUserCalendarView) ),
                            ()=> toAccessDenied
                          )

    val commandAccess = If(
                            ()=> (AuthUtil.? && ( PermissionModule.command_? || AuthUtil.user.isSimpleUserCommand ) ),
                            ()=> toAccessDenied
                          )

    val commandTermAccess = If(
                            ()=> (AuthUtil.? && ( PermissionModule.commandTerm_? ) ),
                            ()=> toAccessDenied
                          )

    val  adminAccess = If(
                            ()=> (AuthUtil.? && AuthUtil.user.isAdmin),
                            ()=> toAccessDenied
                        )                            

    val mapAccess = If(
                            ()=> (AuthUtil.? && PermissionModule.map_?),
                            ()=> toAccessDenied
                      )
/*
    val axiliarAcess = If(
                            ()=> (AuthUtil.? && AuthUtil.user.isOficceUser),
                            ()=> toAccessDenied
                      )
*/
    val simpleUserCommission = If(
                            ()=> (AuthUtil.? && AuthUtil.user.isSimpleUserCommission),
                            ()=> toAccessDenied
                          )  
    val simpleUserCommand = If(
                            ()=> (AuthUtil.? && (AuthUtil.user.isSimpleUserCommand)),
                            ()=> toAccessDenied
                          )  
    val cashierAccess = If(
                            ()=> (AuthUtil.? && (AuthUtil.user.isCashierGeneral || AuthUtil.user.isCashier)),
                            ()=> toAccessDenied
                          )

    val isEgrex = If(
                            ()=> (AuthUtil.? && AuthUtil.company.appType.isEgrex),
                            ()=> toAccessDenied
                          )
    //RestApis
    LiftRules.dispatch.append(CashApi)
    LiftRules.dispatch.append(InvoiceApi)
    LiftRules.dispatch.append(CalendarApi)
    LiftRules.dispatch.append(AnimalApi)
    LiftRules.dispatch.append(CommandApi)
    LiftRules.dispatch.append(PaymentApi)
    LiftRules.dispatch.append(SystemApi)
    LiftRules.dispatch.append(AccountApi)
    LiftRules.dispatch.append(AccountPayableApi)
    LiftRules.dispatch.append(LocationApi)
    LiftRules.dispatch.append(Reports)
    LiftRules.dispatch.append(Reports2)
    LiftRules.dispatch.append(Reports3)
    LiftRules.dispatch.append(TreatmentReportApi)
    LiftRules.dispatch.append(InventoryApi)
    LiftRules.dispatch.append(TreatmentApi)
    LiftRules.dispatch.append(TreatmentDetailsApi)
    LiftRules.dispatch.append(ProductApi)
    LiftRules.dispatch.append(IcdApi)
    LiftRules.dispatch.append(ProductBomApi)
    LiftRules.dispatch.append(MessageApi)
    LiftRules.dispatch.append(SocialApi)
    LiftRules.dispatch.append(SocialNotificationApi)
    LiftRules.dispatch.append(CustomerReportApi)
    LiftRules.dispatch.append(CustomerApi)
    LiftRules.dispatch.append(CustomerAddonsApi)
    LiftRules.dispatch.append(UtilApi)
    LiftRules.dispatch.append(NotificationApi)
    LiftRules.dispatch.append(SecurityApi)
    LiftRules.dispatch.append(MediaApi)
    LiftRules.dispatch.append(OffSaleApi)
    LiftRules.dispatch.append(OffSaleProductApi)
    LiftRules.dispatch.append(PayrollEventApi)  
    LiftRules.dispatch.append(MobileApi)
    LiftRules.dispatch.append(MigrationApi)
    LiftRules.dispatch.append(CompanyApi)
    LiftRules.dispatch.append(WorkHourApi)
    LiftRules.dispatch.append(UserActivityApi)
    LiftRules.dispatch.append(UserApi)
    LiftRules.dispatch.append(UserAddonsApi)
    LiftRules.dispatch.append(TermsApi)
    LiftRules.dispatch.append(CostCenterApi)
    LiftRules.dispatch.append(SiteApi)
    
    LiftRules.dispatch.append(JobRequisitionApi)

    LiftRules.dispatch.append(ProjectApi)
    LiftRules.dispatch.append(RelationshipApi)
    LiftRules.dispatch.append(StakeholderTypeApi)    
    LiftRules.dispatch.append(QuizApi)
    LiftRules.dispatch.append(QuizApplyingApi)
    LiftRules.dispatch.append(BpMonthlyApi)    
    LiftRules.dispatch.append(AgeRangeApi)    
    LiftRules.dispatch.append(AgeRangeIntervalApi)    
    LiftRules.dispatch.append(MapIconApi)
    LiftRules.dispatch.append(MonthlyApi)
    LiftRules.dispatch.append(ContactApi)

    def pricingUrl = {
      if(AuthUtil.? && AuthUtil.company.appType.isEgrex){
          "/pricing"
        }else{
          if(AuthUtil.? && (
              AuthUtil.company.appType.isEsmile||
              AuthUtil.company.appType.isEdoctus||
              AuthUtil.company.appType.isEphysio||
              AuthUtil.company.appType.isEbellepet
              )){
              "/pricing_ephysio"
            }else{
              "/pricing"
            }
        }
    }

    def customerMenuLabel:String = {
      if(AuthUtil.?) {
        AuthUtil.company.appCustName("clientes").toString
      }else{
        "Clientes"
      }
    }
    
    def userMenuLabel = {
      if(AuthUtil.? && AuthUtil.company.appType.isEgrex){
          "Líderes"
        }else{
          if(AuthUtil.? && AuthUtil.company.appType.isEsmile){
              "Profissionais"
            }else{
              "Profissionais"
            }
        }
    }
    def userGroupMenuLabel = {
      if(AuthUtil.? && AuthUtil.company.appType.isEgrex){
          "Grupo de Líderes"
        }else{
          if(AuthUtil.? && (AuthUtil.company.appType.isEdoctus
            || AuthUtil.company.appType.isEsmile || AuthUtil.company.appType.isEphysio)){
              "Especialidade de Profissionais"
            }else{
              "Grupo de Profissionais"
            }
        }
    }
    def activityMenuLabel = {
      if(AuthUtil.? && AuthUtil.company.appType.isEgrex){
          "Atendimentos"
        }else{
          if(AuthUtil.? && (AuthUtil.company.appType.isEsmile||AuthUtil.company.appType.isEdoctus||
              AuthUtil.company.appType.isEphysio||AuthUtil.company.appType.isEbellepet)){
              "Procedimentos"
            }else{
              "Serviços"
            }
        }
    }
    def quizMenuLabel = {
      if(AuthUtil.? && AuthUtil.company.appType.isEgrex){
          "Questionários"
        }else{
          if(AuthUtil.? && (AuthUtil.company.appType.isEsmile||AuthUtil.company.appType.isEdoctus||
              AuthUtil.company.appType.isEphysio || AuthUtil.company.appType.isEbellepet)){
              "Prontuários"
            }else{
              "Questionários"
            }
        }
    }
    def projectMenuLabel = {
      if(AuthUtil.? && AuthUtil.company.appType.isEgrex){
          "Grupos"
        }else{
          if(AuthUtil.? && (AuthUtil.company.appType.isEsmile||AuthUtil.company.appType.isEdoctus||
              AuthUtil.company.appType.isEphysio)){
              "Eventos"
            }else{
              "Eventos"
            }
        }
    }
    def budgetMenuLabel = {
      if(AuthUtil.? && (AuthUtil.company.appType.isEsmile||AuthUtil.company.appType.isEdoctus||
          AuthUtil.company.appType.isEphysio)){
          "Planejamentos"
        }else{
          "Orçamentos"
        }
    }
    // Build SiteMap
    def sitemap = SiteMap(
        //Menu.i("Home") / "index",
        Menu(Loc("Home", Link(List("index"), true, "/index"),"index",Hidden)),
        //Menu.i("a") / "angular_resource",
//        Menu(Loc("Login", Link(List("login"), true, "/login"),"Login",Hidden)),
        Menu(Loc("Planos", Link(List("pricing"), true, pricingUrl), "Planos",Hidden)),
        Menu(Loc("Planos Ephysio", Link(List("pricing_ephysio"), true, "/pricing_ephysio"), "Planos",Hidden)),
        Menu(Loc("docs", Link(List("docs"), true, "/docs/empty"), "Planos",Hidden)),
        Menu(Loc("LoginEmail", Link(List("login_email"), true, "/login_email"),"Login Email",Hidden)),
        Menu(Loc("Unidade", Link(List("unit"), true, "/unit"),"Unidades",crudeAccess,Hidden)),
        Menu(Loc("v2", Link(List("v2"), true, "/v2"),"v2",Hidden)),
        //Open
       // Menu(Loc("Sobre", Link(List("static"), true, "/new_company/add"),"Sobre",unLoggedIn)),
        Menu(Loc("Monitorar", Link(List("static/monitor"), true, "/static/monitor"),"Monitor",unLoggedIn,Hidden)),
        Menu(Loc("bug", Link(List("system"), true, "/system/bug_report"),"Reportar erro",loggedIn,Hidden)),
        //Defalt
        Menu(Loc("Cadastro", Link(List("new_company"), true, "/new_company/add"), "Começar",unLoggedIn)),
        
        Menu(Loc("Alterar", Link(List("company_log"), true, "/company_log/edit"),"Empresa",adminAccess,Hidden)),
        // assim só o reabrir caixa aparece como financeiro - antes tudo de financial_admin aparecia como caixa
        //Menu(Loc("financial_admin", Link(List("financial_admin"), true, "/financial_admin/checkout_reopen"),"Caixa",financialManagerAccess,Hidden)),
        Menu(Loc("financial_admin", Link(List("financial_admin"), true, "/financial_admin/checkout_reopen"),"Financeiro",financialManagerAccess,Hidden)),

        Menu(Loc("repost_sales", Link(List("reports"), true, "/reports/list"),"Relatórios",reportAccess,Hidden)),
        Menu(Loc("repost_chart", Link(List("reports_chart"), true, "/reports/chart"),"Relatório Grafico",reportAccess,Hidden)),

        //Menu(Loc("Sair", Link(List("security"), true, "/security/logout"),"Sair",loggedIn,Hidden)),
        Menu(Loc("AlterarSenha", Link(List("security"), true, "/security/change_password"),"Alterar Senha",loggedIn,Hidden)),
        //Calendar
        Menu(Loc("Agenda", Link(List("calendar"), true, "/calendar"),"Agenda",calendarAccess)),
        Menu(Loc("Atendimento", Link(List("treatment"), true, "/treatment/treatment"),"Atendimento",calendarAccess,Hidden)),
        //Crude
        Menu(Loc("Profissionais", Link(List("user"), true, "/user/list"),userMenuLabel,peopleManagerAccess)),
//        Menu(Loc("Profissionais1", Link(List("usernew"), true, "v2/pages/user/edit"),"editnew",peopleManagerAccess,Hidden)),
        Menu(Loc("Grupos de Profissionais", Link(List("group"), true, "/group/list"),userGroupMenuLabel,peopleManagerAccess,Hidden)),

        Menu(Loc("Animal", Link(List("animal"), true, "/animal/edit_animal"),"Pets",customerAccess,Hidden)),


        Menu(Loc("Prontuário", Link(List("records"), true, "/records/edit_patient"),"Prontuário",recordsAccess,Hidden)),

        Menu(Loc("Cliente", Link(List("customer"), true, "/customer/list"),customerMenuLabel,customerAccess)),
        Menu(Loc("ClienteForlist", Link(List("customer_form_list"), true, "/customer/list_form"),"Convênio",customerAccess,Hidden)),

        Menu(Loc("Convênio", Link(List("offsale"), true, "/offsale/list"),"Convênio",financialAccess,Hidden)),
        Menu(Loc("Mensalidade", Link(List("monthly"), true, "/monthly/list_monthly"),"Mensalidade",superAdmin,Hidden)),

        Menu(Loc("Faixa Etária", Link(List("agerange"), true, "/agerange/list_agerange"),"Faixa Etária",customerAccess,Hidden)),
        Menu(Loc("Ícone", Link(List("mapicon"), true, "/mapicon/list_mapicon"),"Ícone",customerAccess,Hidden)),

        Menu(Loc("Orçamentos", Link(List("budget"), true, "/budget/list_budget"),budgetMenuLabel,financialAccess,Hidden)),
        // peopleManagerAccess - lider na cczs
        Menu(Loc("Projetos", Link(List("project"), true, "/project/list_event"),projectMenuLabel,peopleManagerAccess,Hidden)),

        Menu(Loc("Grupos", Link(List("project_group"), true, "/project_group/list_group"),projectMenuLabel,peopleManagerAccess,Hidden)),

        Menu(Loc("Relacionamentos", Link(List("bprelationship"), true, "/bprelationship/list_bprelationship"),"Relacionamentos",customerAccess,Hidden)),
        Menu(Loc("Questionários", Link(List("quiz"), true, "/quiz/list"),quizMenuLabel,customerAccess,Hidden)),
        Menu(Loc("Questionários_admin", Link(List("quiz_admin"), true, "/quiz_admin/quiz"),quizMenuLabel,serviceManagerAccess,Hidden)),
        Menu(Loc("Mensalidades", Link(List("bpmonthly"), true, "/bpmonthly/list_bpmonthly"),"Mensalidade",customerAccess,Hidden)),

        //Treatmemnts
        Menu(Loc("Serviços", Link(List("activity"), true, "/activity/list"),activityMenuLabel,serviceAccess)),
        Menu(Loc("Dashboard", Link(List("dashboard"), true, "/dashboard"),"Dashboard",treatmentAccess,Hidden)),
        Menu(Loc("test", Link(List("test"), true, "/test"),"test",unLoggedIn,Hidden)),
        

        //Inventory
        Menu(Loc("Estoque", Link(List("product"), true, "/product/control_panel"),"Estoque",inventoryAccess)),
        //Menu(Loc("Estoque_movements", Link(List("product_"), true, "/product/inventory_movements"),"inventory_movements",inventoryAccess,Hidden)),
        Menu(Loc("Estouqe_admin", Link(List("product_admin"), true, "/product_admin/inventory"),"Estoque",inventoryManagerAccess,Hidden)),
 
        //Financial
        
        Menu(Loc("Caixa", Link(List("financial_cashier"), true, "/financial_cashier/register_payment"),"Caixa", cashierAccess, Hidden)),
        Menu(Loc("ComandaConference", Link(List("financial_cashier_ComandaConference"), true, "/financial_cashier/commands_missing"),"Caixa", cashierAccess, Hidden)),
        
        
        //Menu(Loc("CiaxaAuxiliar", Link(List("financial_caixa"), true, "/financial/register_payment"),"Caixa",axiliarAcess, Hidden)),
        Menu(Loc("Financeiro", Link(List("financial"), true, "/financial/account_register"),"Financeiro",financialAccess)),
        Menu(Loc("cheques", Link(List("financial_cheques"), true, "/financial/control_panel"),"Cheques",financialAccess,Hidden)),
        Menu(Loc("commission_conference_user", Link(List("commission_conference_user"), true, "/commission_conference_user"),"Comissão",simpleUserCommission)),
//        Menu(Loc("/commission/commission_report_redirect", Link(List("commission"), true, "/commission/commission_report_redirect"),"Comissão_new",simpleUserCommission)),
        Menu(Loc("/command/user_command", Link(List("command"), true, "/command/user_command_redirect"),"Comanda",simpleUserCommand)),
        Menu(Loc("/command_full/user_command_full", Link(List("command_full"), true, "/command_full/user_command_full"),"Co1",commandAccess,Hidden)),
        Menu(Loc("/command_full/user_command_term", Link(List("command_full_term"), true, "/command_full/user_command_term"),"Comanda",commandTermAccess)),
        // estabilizado o esquema de comanda pode tirar as opções abaixo
//        Menu(Loc("/command/user_command", Link(List("command"), true, "/command/user_command"),"Comanda",simpleUserCommand)),
//        Menu(Loc("/command_full/user_command_full", Link(List("command_full"), true, "/command_full/user_command_full"),"Co1",commandAccess,Hidden)),
        Menu(Loc("Mapa", Link(List("map"), true, "/map"),"Mapa",mapAccess, Hidden)),
        Menu(Loc("Formas de Pagamento", Link(List("company"), true, "/financial_admin/payment_forms"),"",financialManagerAccess,Hidden)),
        Menu(Loc("busy_manager", Link(List("busy_manager"), true, "/activity/busy_manager"),"busy_manager",crudeAccess,Hidden)),
        // antes T estava loggedin - os prof acessavam
        Menu(Loc("treatments_conferenc", Link(List("treatments_conferenc"), true, "/treatments_conferenc"),"treatments_conferenc",customerAccess,Hidden)),
        Menu(Loc("rank", Link(List("rank"), true, "/rank"),"rank",crudeAccess,Hidden)),
        Menu(Loc("Relatórios", Link(List("/reports/center"), true, "/reports/center"),"Relatórios",reportNotEgrexAccess)),
        Menu(Loc("Relatórios egrex", Link(List("/reports/center_egrex"), true, "/reports/center_egrex"),"Relatórios",isEgrex)),
        Menu(Loc("Ad", Link(List("manager"), true, "/manager/index"), "Ad",superAdmin))
        
        
        
    )
//println ("vaiiii ====================== " + sitemap)

    /*
    LiftRules.earlyInStateful.append(ExtSession.testCookieEarlyInStateful)
    
    LiftRules.liftRequest.append { 
      case Req("favicon" :: Nil, "ico", GetRequest) => false
      case Req(_, "css", GetRequest) => false 
      case Req(_, "js", GetRequest) => false 
    }
    
    LiftRules.liftRequest.append { 
      case Req("classpath" :: _, _, _) => true
      case Req("ajax_request" :: _, _, _) => true
      case Req("comet_request" :: _, _, _) => true
      case Req("favicon" :: Nil, "ico", GetRequest) => false
      case Req(_, "css", GetRequest) => false 
      case Req(_, "js", GetRequest) => false 
    }
  */
  LiftRules.supplimentalHeaders = s => s.addHeaders(
      List(HTTPParam("X-Lift-Version", LiftRules.liftVersion),
        HTTPParam("Access-Control-Allow-Origin", "*"),
        HTTPParam("Access-Control-Allow-Credentials", "true"),
        HTTPParam("Access-Control-Allow-Methods", "GET, POST, PUT,OPTIONS"),
        HTTPParam("Access-Control-Allow-Headers", "Keep-Alive,User-Agent,X-Requested-With, Content-Type")
      ))
    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemap)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    //LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Make a transaction span the whole HTTP request
    //S.addAround(DB.buildLoanWrapper)

    startServices
    LiftRules.unloadHooks.append(() => {
    //  QuartzUtil.stop
    })
  }
  def startServices = {
    //QuartzUtil.start
    CommisionQueeue.start
    DeliveryQueeue.start
    BusinessPatternLocationQueeue.start
  }

/*... */

// inside the Boot class add a method to configure the mailer:
// you will pass this the name of the
// smtp server and the login credentials
def configMailer(host: String, user: String, password: String, port:String) {
  // Enable TLS support
  System.setProperty("mail.smtp.starttls.enable","true");
  // Set the host name
  System.setProperty("mail.smtp.ssl.enable", "true")
  System.setProperty("mail.debug", "true")//Em produção o jetty ta dando pal deixar false por hora
  System.setProperty("mail.smtp.host", host) // Enable authentication
  System.setProperty("mail.smtp.port", port) // SMTP port
  System.setProperty("mail.smtp.auth", "true") // Provide a means for authentication. Pass it a Can, which can either be Full or Empty
  Mailer.authenticator = Full(new Authenticator {
    override def getPasswordAuthentication = new PasswordAuthentication(user, password)
  })
}  
}

class MyStandardDBVendor(driverName: String, dbUrl: String, dbUser: Box[String],
          dbPassword: Box[String]) extends StandardDBVendor(driverName,dbUrl,dbUser,dbPassword){
    /**
     *  Override this method if you want something other than
     * 4 connections in the pool
     */
    override protected def maxPoolSize = 50

    /**
     * The absolute maximum that this pool can extend to
     * The default is 60.  Override this method to change.
     */
    override protected def doNotExpandBeyond = 500

}
