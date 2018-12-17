package code
package model 

import code.actors._
import code.service._
import net.liftweb._ 
import mapper._ 
import code.util._
import json._
import net.liftweb.widgets.gravatar.Gravatar
import net.liftweb.common._
import net.liftweb.util._


import java.util.Date;
import java.util.Calendar

class User extends  BusinessPattern[User] with UserIdAsString{ 

    def getSingleton = User
    def userIdAsString = this.id.is.toString
    def childs = User.findAll(
        By(User.parent, this),
        By(User.userStatus, User.STATUS_OK))
    override def updateShortName = false
    override def updateIdForCompany = if(AuthUtil.?) {
        AuthUtil.company.bpIdForCompany == 0 || AuthUtil.company.bpIdForCompany == 1
    } else {
        false
    }
    //AuthUtil.company.bpIdForCompany == 0 || AuthUtil.company.bpIdForCompany == 1

    object group extends MappedLongForeignKey(this.asInstanceOf[MapperType],UserGroup){
        override def dbIndexed_? = true
    }

    object parent extends MappedLongForeignKey(this.asInstanceOf[MapperType],User) with LifecycleCallbacks {
        override def dbIndexed_? = true
        override def beforeSave() {
            super.beforeSave;
            if(this.get == id.is){
                this.set (0l);
                //this.set(dueDate.is)
                //throw new RuntimeException("Data de fim deve ser maior que data de início!")
            }
            /* nao pode pq o superior informado permite ver a comissao do subalterno
                mesmo sem repasse
            if (parent_percent == 0.0) {
                this.set (0l);
            }
            */
        }       
    }

    object parent_percent extends MappedCurrency(this){
        override def defaultValue = 0.00
    } 

    def  percentToParrent:Double = {
        parent_percent.is
    }

    // achei melhor tratar no commission como superior ao invés de parent
    def hasSuperior = parent.obj  match {
        case Full(a) => true
        case _ => false
    }

/*
    object userStatus extends MappedInt(this) with LifecycleCallbacks {
    	override def dbIndexed_? = true
        override def defaultValue = User.STATUS_OK
      	override def beforeSave() {
        	super.beforeSave;
          	if(userStatus.is == User.STATUS_OK){
	        	if(status.is != User.STATUS_OK){
	            	status(User.STATUS_OK)
	          	}
          	}
      	} 
    }
*/

    object hireDate extends EbMappedDate(this)

    object resignationDate extends EbMappedDate(this)

    override def isUserDefaultValue = true
    
    object showInCalendar_? extends MappedBoolean(this){
        override def dbColumnName = "showInCalendar"
        override def defaultValue = true
    }

    object orderInCalendar extends MappedInt(this){
        override def defaultValue = 0
    } 

    // qdo o cliente agenda ele proprio on line
    object showInCalendarPub_? extends MappedBoolean(this){
        override def dbColumnName = "showInCalendarPub"
        override def defaultValue = true
    }

    object showInCommand_? extends MappedBoolean(this){
        override def dbColumnName = "showInCommand"
        override def defaultValue = true
    }

    object showInCashier_? extends MappedBoolean(this){
        override def dbColumnName = "showInCashier"
        override def defaultValue = true
    }

    object calendarFixed_? extends MappedBoolean(this){
        override def dbColumnName = "calendarFixed"
        override def defaultValue = true
    }

    object calendarPrice_? extends MappedBoolean(this){
        override def dbColumnName = "calendarPrice"
        override def defaultValue = false
    }
    object discountToCommission_? extends MappedBoolean(this){
        override def dbColumnName = "discountToCommission"
        override def defaultValue = true
    }
    object commissionAtSight_? extends MappedBoolean(this){//Avista
        override def defaultValue = false // respeita a forma pagto
        override def dbColumnName = "commissionAtSight"
    }

    object deletePayment_? extends MappedBoolean(this){
        override def dbColumnName = "deletePayment"
        override def defaultValue = true;
    }

    def login (userName:String, passWord:String, company:Company):LoginStatus = {
        if(userName != "" && passWord != ""){
            val list = User.findAll(BySql[code.model.User]("lower(username) = lower(?)",IHaveValidatedThisSQL("",""), userName),
                BySql[code.model.User]("password = ?",IHaveValidatedThisSQL("",""), Project.md5(passWord)),
                BySql[code.model.User]("(company=? or id in (select business_pattern from bpcompany where targetcompany = ? and allowed=true))",IHaveValidatedThisSQL("",""), company.id.is, company.id.is),
                BySql[code.model.User]("(company in (select co.id from company co where co.status = 1 and co.id = company))",IHaveValidatedThisSQL("",""))
                )
            if(list.size >0){
                val user = list(0)
                def unitToSet = if(user.company.is != company.id.is){
                    company.mainUnit
                } else{
                    user.unit.obj.get
                }
                LogActor ! "Login username company " + user.company.is.toString + " " + user.name.is + " " +new Date().toString
                user.lastLogin(new Date()).insecureSave
              LoginStatus(true, "", user.unit(unitToSet).company(company))
            }else{
               LoginStatus(false, "", User.create)
            }
        }else{
            LoginStatus(false, "", User.create)
        }
    }

    def unitsToShowSql = if (AuthUtil.user.isAdmin) {
        " 1 = 1 "
    } else {
        " (unit = %s or (unit in (select uu.unit from usercompanyunit uu where uu.user_c = %s and uu.company = %s))) ".format(this.unit, this.id, this.company)
    }

    def loginEmail(email:String, passWord:String, companyP:Long =(-1l)):LoginStatus = {
        def emailLiked = "%"+email.trim.toLowerCase+"%"
        def strCompany = if(companyP != -1){
            "(company = " + companyP + ")"
        } else {
            " 1 = 1 "
        }
        if(emailLiked.length > 12 && passWord.trim != ""){
            val list = User.findAll(
                By (User.userStatus,1),
                    BySql[code.model.User]("lower(trim(email)) like lower(?)",IHaveValidatedThisSQL("",""), emailLiked),
                    BySql[code.model.User]("password = ?",IHaveValidatedThisSQL("",""), Project.md5(passWord.trim)),
                    BySql[code.model.User]("(company in (select co.id from company co where co.status = 1))",IHaveValidatedThisSQL("","")),
                    BySql[code.model.User](strCompany,IHaveValidatedThisSQL("","")),
                    OrderBy (User.company, Ascending)
                                )
            val listCompany = if(companyP != -1){
                    list.filter(_.company.is == companyP)(0)::Nil
                }else{
                    list
                }
            if(listCompany.size == 1){
                val user = listCompany(0)
                val today : String = Project.dateToStr(new Date());
                if (today == "06/05/2017" && !user.isSuperAdmin) {
                    LoginStatus(false, "Estamos em manutenção para melhoria de nossa infraestrutura.\n\n O acesso será normalizado a partir de 0h de 07/05/2017")
                } else {
                    if (user.groupPermission  == ""){
                        LoginStatus(false, "Este usuário não possui nenhuma permissão atribuida")
                    }else{
                        LogObj.wLogObj (user.company.is, "Login email user company " + user.company.is.toString + "       id " + user.id.is.toString +
                         "      user " + user.name.is + "       date " +new Date().toString, "Info");
//                        LogActor ! "Login email user company " + user.company.is.toString + "       id " + user.id.is.toString +
//                         "      user " + user.name.is + "       date " +new Date().toString
                        user.lastLogin(new Date()).insecureSave
                        LoginStatus(true, "", user, listCompany)
                    }
                }
            } else if(listCompany.size > 1){
                val user = listCompany(0)
                LoginStatus(true, "", user, listCompany)
            }else{
                val list1 = User.findAll(
                        BySql[code.model.User]("lower(trim(email)) like lower(?)",IHaveValidatedThisSQL("",""), emailLiked),
                        BySql[code.model.User]("password = ?",IHaveValidatedThisSQL("",""), Project.md5(passWord.trim)))
                if (list1.size > 0) {
                    // email e senha são válidos
                    val user = list1(0)
                    if (user.userStatus != 1) {
                        LoginStatus(false, "Usuário inativo, contate seu administrador!")
                    } else {
                        LoginStatus(false, "Empresa inativa, contate suporte@vilarika.com.br!")
                    }
                } else {
                    // email e senha não batem mesmo
                    LoginStatus(false, "")
                }
            }
        }else{
            LoginStatus(false, "Email inválido!")
        }
    }    

    def beginning = {
        if (AuthUtil.user.isFinancialManager &&
            (Project.diffYYMM (Project.today,AuthUtil.company.createdAt,"DAYSINT")).toInt > 25) {
          val of = User.findByKey(AuthUtil.user.id.is).get        
          val users = AuthUtil.user.id.is:: Nil
          val calendar = Calendar.getInstance()
          calendar.set(Calendar.YEAR,2029);
          val expirationdate = calendar.getTime()
          if (AuthUtil.unit.getPartner.street == "" ||
             AuthUtil.unit.getPartner.district == "" ||
             AuthUtil.unit.getPartner.postal_code == "" ||
             (AuthUtil.unit.getPartner.document == "" && 
              AuthUtil.unit.getPartner.document_company == "")) {
            val message = """Olá %s, <br/>

            Por favor, atualize os dados de sua empresa <br>É preciso informar um CPF ou CNPJ válido.<br/>
            É preciso também informar o endereço completo<br/>
            <br/>
            Acesse o <b>cadastro de unidades</b>, no menu superior direito, seu usuário, unidades, ou use o atalho ( ALT U ) <a href="/unit/edit?id=%s&msg=false">Ou Clique aqui</a>
            <br/>
            <br/>
            <img src="http://ebelle.vilarika.com.br/images/mensal_menu1.png" style="width: 400px;"/>
            <br/>
            <br/>
            """.format (AuthUtil.user.name, AuthUtil.unit.id.is)
            UserMessage.build("Atualize os dados de sua empresa", message, of, 0, users, AuthUtil.company, UserMessage.SYSTEM, expirationdate)
          }
        }
    }

    def goTo = if (AuthUtil.company.appType.isEgrex) {
      if (AuthUtil.user.isCustomer) {
        "/customer/list"
      } else {
        "/financial/account_register"
      }
    } else if (PermissionModule.treatment_? && 
      (AuthUtil.user.isSimpleUserCalendar || AuthUtil.user.isCalendarUser ||
       AuthUtil.user.isSimpleUserCalendarView)) {
        // if (S.hostName.contains ("local")) {
        if (Project.isLocalHost) {
          "/calendar"
        } else {
          "/calendar"
          //"http://45.33.99.152:7171/calendar"
        }
    } else if (PermissionModule.command_? && (AuthUtil.user.isCommandUser || AuthUtil.user.isCommandPwd)) {
      "/command_full/user_command_full"
    } else if (PermissionModule.command_? && (AuthUtil.user.isSimpleUserCommand)) {
      "/command/user_command"
    } else if (AuthUtil.user.isSimpleUserCommission) {
      //"/commission/commission_report_redirect"
      "/commission_conference_user"
    } else if (PermissionModule.inventory_?) {
      "/product/control_panel"
    } else if (PermissionModule.financial_?) {
      "/financial/account_register"
    } else if (PermissionModule.peopleManager_?) {
      "/user/list"
    } else {
      "/customer/list"
    }

    def loginCommand(userId:Long, passWord:String):Boolean = {
        
        if(passWord != ""){
            // testa a password com e sem md5, pq no caso de edoctus entra
            // com o usuario logado e pega a password já em md5
            val list = User.findAllInCompany(
                                    BySql[code.model.User]("id = ?",IHaveValidatedThisSQL("",""), userId),
                                    BySql[code.model.User]("password = ? or password = ?",IHaveValidatedThisSQL("",""), Project.md5(passWord), passWord)
                                )
            if(list.size >0){
                val user = list(0)
                if (user.groupPermission == ""){
                    //LoginStatus(false)
                    false
                }else{
                    LogActor ! "Login command user company " + user.company.is.toString + " " + user.name.is + " " +new Date().toString
                    user.lastLogin(new Date()).insecureSave
                    //LoginStatus(true, user, list)
                    true
                }
            }else{
               //LoginStatus(false)
               false
            }
        }else{
            //LoginStatus(false)
            false
        }
    }    

    lazy val workHouers = WorkHouer.findAll(By(WorkHouer.user,this.id))
    
    def userActivityByActivity[T <: code.model.ProductMapper[T] ](activity:ProductMapper[T]):Box[UserActivity] = {
        UserActivity.findAll(
            By(UserActivity.user,this.id),
            By(UserActivity.activity,activity.id.is)
            ) match {
            case a :: tail => Full(a)
            case _ => Empty
        }
    }

    def activityPrice[T <: code.model.ProductMapper[T] ](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_price_?){
                    activity.salePrice.is
                }else{
                    a.price.is
                }
            case _ => activity.salePrice.is
        }
    }

    def activityDuration(activity:Activity):String = {
        userActivityByActivity(activity) match {
            case Full(a) if(a.duration.is != "") => a.duration.is
            case _ => activity.duration.is
        }
    }

    def activityId[T  <: code.model.ProductMapper[T]](activity:ProductMapper[T]):Long = {
        userActivityByActivity(activity) match {
            case Full(a) => a.id
            case _ => 0l
        }
    }    

    def activityCommission[T <: code.model.ProductMapper[T]](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_commission_?){
                    activity.commission.is
                }else{
                    a.commission.is
                }
            case _ => activity.commission.is
        }
    }    

    def activityCommissionAbs[T <: code.model.ProductMapper[T]](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_commission_?){
                    activity.commissionAbs.is
                }else{
                    a.commissionAbs.is
                }
            case _ => activity.commissionAbs.is
        }
    }    

    def activityAuxPrice[T <: code.model.ProductMapper[T]](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_commission_?){
                    activity.auxPrice.is
                }else{
                    a.auxPrice.is
                }
            case _ => activity.auxPrice.is
        }
    }    
    def activityAuxPercent[T <: code.model.ProductMapper[T]](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_commission_?){
                    activity.auxPercent.is
                }else{
                    a.auxPercent.is
                }
            case _ => activity.auxPercent.is
        }
    }    

    def activityAuxHousePrice[T <: code.model.ProductMapper[T]](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_commission_?){
                    activity.auxHousePrice.is
                }else{
                    a.auxHousePrice.is
                }
            case _ => activity.auxHousePrice.is
        }
    }    
    def activityAuxHousePercent[T <: code.model.ProductMapper[T]](activity:ProductMapper[T]):BigDecimal = {
        userActivityByActivity(activity) match {
            case Full(a) => if(a.use_product_commission_?){
                    activity.auxHousePercent.is
                }else{
                    a.auxHousePercent.is
                }
            case _ => activity.auxHousePercent.is
        }
    }    

    def userActivities = UserActivity.findAll(By(UserActivity.user,this.id))

    def activitys (calendarPub:Boolean) = {
        val activitys = UserActivity.findAll(By(UserActivity.user,this.id))
        if (calendarPub) {
            activitys.filter((a) => a.activity.is !=0 && 
                a.activity.obj.get.status.is == StatusConstants.STATUS_OK && 
                a.activity.obj.get.showInCalendarPub_? && 
                a.activity.obj.get.productClass.is == ProductType.Types.Service ).
                map(_.activity.obj.get) ::: activitys.filter((a) => a.activity.is ==0).map(_.producttype.obj.get.activitys).foldLeft(scala.List[Activity]())(_:::_)
        } else {
            activitys.filter((a) => a.activity.is !=0 && 
                a.activity.obj.get.status.is == StatusConstants.STATUS_OK && 
                a.activity.obj.get.productClass.is == ProductType.Types.Service ).
                map(_.activity.obj.get) ::: activitys.filter((a) => a.activity.is ==0).map(_.producttype.obj.get.activitys).foldLeft(scala.List[Activity]())(_:::_)
        }
    }

    def updateLocation(lat:String,lng:String,distance:Double){
        LocationHistory.create.user(this).lat(lat).lng(lng).distance(distance).save
        this.lat(lat).lng(lng).save
     }

    def locations = LocationHistory.findAll(By(LocationHistory.user,this))

    def locationsByDate (start:Date,end:Date) =  LocationHistory.findAll(By(LocationHistory.user,this),OrderBy(LocationHistory.createdAt, Descending),BySql(" date(createdAt) between ? and ?",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end))

    def groupPermissionList:List[Int] = groupPermission.is.split(",").toList match {
                case Nil => List[Int]()
                case a:List[String] => a.map((s) => if(s!="") s.toInt else 0)
        }

    def replaceMessage (ac:User, message:String) = {
        var message_aux = message;
        //val extenso = WrittenForm (123.999.467.89)
        //println ("vaiiii ===================== " + extenso.humanize());

        message_aux = ac.unit.obj.get.replaceMessage (ac.unit.obj.get, message_aux);

        message_aux = message_aux.replaceAll ("##prof_nome##", ac.name.is)
        message_aux = message_aux.replaceAll ("##prof_apelido##", ac.short_name.is)
        message_aux = message_aux.replaceAll ("##prof_prinome##", ac.firstName)
        message_aux = message_aux.replaceAll ("##prof_telefone##", ac.phone)
        message_aux = message_aux.replaceAll ("##prof_celular##", ac.mobilePhone)
        message_aux = message_aux.replaceAll ("##prof_telefone2##", ac.email_alternative)

        message_aux = message_aux.replaceAll ("##prof_telefones##", ac.allPhones)

        message_aux = message_aux.replaceAll ("##prof_email##", ac.email)
        message_aux = message_aux.replaceAll ("##prof_doc##", ac.document + ac.document_company)
        message_aux = message_aux.replaceAll ("##prof_doc_rg##", ac.document_identity)

        message_aux = message_aux.replaceAll ("##prof_doc_prof##", ac.document_council)
        message_aux = message_aux.replaceAll ("##prof_conselho##", ac.councilShortName)

        message_aux = message_aux.replaceAll ("##prof_nasc_data##", Project.dateToStr(ac.birthday))
        message_aux = message_aux.replaceAll ("##prof_nasc_idade##", Project.dateToAge(ac.birthday))
        message_aux = message_aux.replaceAll ("##prof_nasc_anos##", Project.dateToYears(ac.birthday))
        message_aux = message_aux.replaceAll ("##prof_nasc_ext##", Project.dateToExt(ac.birthday))

        message_aux = message_aux.replaceAll ("##prof_adm_data##", Project.dateToStr(ac.hireDate))
        message_aux = message_aux.replaceAll ("##prof_adm_idade##", Project.dateToAge(ac.hireDate))

        message_aux = message_aux.replaceAll ("##prof_detalhes##", ac.obsComplement)
        message_aux = message_aux.replaceAll ("##prof_obs##", ac.obs)

        message_aux = message_aux.replaceAll ("##prof_end_rua##", ac.street)
        message_aux = message_aux.replaceAll ("##prof_end_nro##", ac.number)
        message_aux = message_aux.replaceAll ("##prof_end_compl##", ac.complement)
        message_aux = message_aux.replaceAll ("##prof_end_cep##", ac.postal_code)
        message_aux = message_aux.replaceAll ("##prof_end_cid##", ac.cityName)
        message_aux = message_aux.replaceAll ("##prof_end_bairro##", ac.district)
        message_aux = message_aux.replaceAll ("##prof_end_ref##", ac.pointofreference)
        message_aux = message_aux.replaceAll ("##prof_end_uf##", ac.stateShortName)
        message_aux = message_aux.replaceAll ("##prof_end_completo##", ac.full_address)
        message_aux
    }

    def isSuperAdmin = groupPermissionList.filter(_==UserGroupPermission.SUPER_ADMIN).size > 0

    def isSupportAdmin = groupPermissionList.filter(_==UserGroupPermission.SUPPORT_ADMIN).size > 0 || isSuperAdmin

    def isAdmin = groupPermissionList.filter(_==UserGroupPermission.ADMIN_USER).size > 0 || isSuperAdmin || isSupportAdmin

    def isAdminRead = groupPermissionList.filter(_==UserGroupPermission.ADMIN_READ).size > 0

//    nao existe mais - por isso dobrei o U
//    def isSimpleUUserNotEdit = groupPermissionList.filter(_==UserGroupPermission.SIMPLE_UUSER_NOT_EDIT).size > 0

    def isSimpleUserCalendar = groupPermissionList.filter(_==UserGroupPermission.SIMPLE_USER_CALENDAR).size > 0

    def isSimpleUserCalendarView = groupPermissionList.filter(_==UserGroupPermission.SIMPLE_USER_CALENDAR_VIEW).size > 0

    def isSimpleUserCommission = groupPermissionList.filter(_==UserGroupPermission.SIMPLE_USER_COMMISSION).size > 0

    def isSimpleUserCommand = groupPermissionList.filter(_==UserGroupPermission.SIMPLE_USER_COMMAND).size > 0

    def isInventoryManager = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.INVENTORY_MANAGER).size > 0

    def isInventoryUser = isAdmin || isAdminRead || isInventoryManager || groupPermissionList.filter(_==UserGroupPermission.INVENTORY_USER).size > 0
    
    def isFinancialManager = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.FINANCIAL_MANAGER).size > 0

    def isPeopleManager = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.PEOPLE_MANAGER).size > 0

    def isCustomer = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.CUSTOMER).size > 0

    def isRecords = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.RECORDS).size > 0

    def isBudget = isAdmin || isAdminRead || isFinancialManager || groupPermissionList.filter(_==UserGroupPermission.BUDGET).size > 0

    def isFinancialUser = isAdmin  || isFinancialManager || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.FINANCIAL_USER).size > 0

    //def isSaleManagerUser = isAdmin || groupPermissionList.filter(_==UserGroupPermission.SALE_MANAGER).size > 0

    def isCashier = isAdmin || isFinancialManager || groupPermissionList.filter(_==UserGroupPermission.CASHIER).size > 0

    def isCashierGeneral = isAdmin || isFinancialManager || groupPermissionList.filter(_==UserGroupPermission.CASHIER_GENERAL).size > 0

    def isCalendarUser = isAdmin || groupPermissionList.filter(_==UserGroupPermission.CALENDAR_USER).size > 0

    def isCommandUser = isAdmin || groupPermissionList.filter(_==UserGroupPermission.COMMAND_USER).size > 0

    def isCommandPwd = /*isAdmin || */groupPermissionList.filter(_==UserGroupPermission.COMMAND_PWD).size > 0

    def isCommandTerm = groupPermissionList.filter(_==UserGroupPermission.COMMAND_USER).size > 0 || groupPermissionList.filter(_==UserGroupPermission.COMMAND_PWD).size > 0

    def isServiceManager = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.SERVICE_MANAGER).size > 0

    def isServiceUser = isAdmin || isAdminRead || isServiceManager || groupPermissionList.filter(_==UserGroupPermission.SERVICE_USER).size > 0

    def isReportUser = isAdmin || isAdminRead || groupPermissionList.filter(_==UserGroupPermission.REPORT_USER).size > 0

//    def resetPasswordKey = BusinessRulesUtil.md5(this.id.is.toString)+BusinessRulesUtil.md5(this.email.is.toString)+BusinessRulesUtil.md5(this.password.is.toString)
    
    object canCreateCalendarEvents_? extends MappedBoolean(this){
        override def dbColumnName = "cancreatecalendarevents"
        override def defaultValue = true
    }
    object canDeleteCalendarEvents_? extends MappedBoolean(this){
        override def dbColumnName = "candeletecalendarevents"
        override def defaultValue = true
    }
    object canMoveCalendarEvents_? extends MappedBoolean(this){
        override def dbColumnName = "canmovecalendarevents"
        override def defaultValue = true
    }
    object canEditCalendarEvents_? extends MappedBoolean(this){
        override def dbColumnName = "caneditcalendarevents"
        override def defaultValue = true
    }
    object canBusyEvents_? extends MappedBoolean(this){
        override def dbColumnName = "canbusyevents"
        override def defaultValue = true
    }

    lazy val userGroupName:String = {
        group.obj match {
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

    def validateDuplicatedEmail = {
        if (this.email != "") {
            if (User.count(By(company, AuthUtil.company.id.is), By(email, this.email), NotBy(id, this.id)) > 0) {
                throw new RuntimeException("Já existe um registro com esse email : %s".format(this.email.is))
            }  
        }
    }

    val SQL_UPDATE_ORDER_10_10 = """
       update business_pattern set orderincalendar = 
       ((select count (*) from business_pattern t1 where t1.company = business_pattern.company 
           and is_employee = true and orderincalendar <> 0
           and t1.unit = business_pattern.unit 
           and (t1.orderincalendar < business_pattern.orderincalendar or (t1.orderincalendar = business_pattern.orderincalendar and t1.id < business_pattern.id)))+1) * 10
       where company = ? and unit = ? and orderincalendar <> 0
       and is_employee = true and userstatus = 1 and showincalendar = true;
    """

    override def save() = {
        if (this.password != "" && this.password.length < 6) {
            throw new RuntimeException ("Senha não pode ter menos de 6 caracteres");
        } else if (this.password != "" && this.password.length >= 6 
            && ("01234567890").indexOf (this.password) != -1) {
            throw new RuntimeException ("Senha não pode ser uma sequencia simples de números");
        }
        if (this.parent_percent != 0.0 && this.parent.isEmpty) {
            throw new RuntimeException ("Um profissional superior precisa ser informado, caso o percentual de comissão para o superior seja diferente de zero")
        }
        // rigel março 2018 - nao sei pq o length vem com um a mais
        if (isAdmin && groupPermissionList.length > 2) {
            throw new RuntimeException ("Se a permissão Administrador foi especificada, não é necessário especificar nenhuma outra")
        } else if (!isAdmin) {

            if (!isFinancialManager) {
                if (isCashierGeneral && isCashier) {
                    throw new RuntimeException ("Se a permissão Caixa Geral foi especificada, não é necessário especificar a permissão de Caixa")
                }
            }

            if ((isSimpleUserCalendar || isSimpleUserCommand || isSimpleUserCalendarView) 
                && (isCalendarUser || isCommandUser)) {
                throw new RuntimeException ("Se as permissões de Agenda Geral ou Comanda Geral forem especificadas, não faz sentido especificar permissões restritivas como Prof Agenda, Prof Agenda Geral e Prof Comanda")
            }
            if (isRecords && !isCustomer) {
                throw new RuntimeException ("Ao atribuir a permissão Prontuário você deve também atribuir Clientes/Pacientes")
            }
        }

        if (this.is_person_?) {
            if (this.image.is == "" || this.image.is == "empresa.png") {
                this.image.set ("cliente.png")
            }
            if (this.imagethumb.is == "" || this.imagethumb == "empresa.png") {
                this.imagethumb.set ("cliente.png")
            }
        }
        validateDuplicatedEmail
/*        // tirei no businesspattern - pq tava gerando prontuario
        voltei pra lá - provavelmente não usava o insecureSave
        e tirando de lá não gerava o mapa para unidade e convenio - 02/04/2017
        if(!this.street.is.isEmpty || this.street.is != "") {
            BusinessPatternLocationQueeue.enqueeue(BusinessPatternQueeueDto(this.id.is))
        } else {
            this.lat.set ("");
            this.lng.set ("");
        }
*/
        val r = super.save;

        DB.runUpdate(SQL_UPDATE_ORDER_10_10, this.company.obj.get.id.is :: this.unit.is :: Nil)

        r;

    }

    def toXmlTissSolicitante (offsale:Long) = {
        var os = OffSale.findByKey (offsale).get;
        var  bs = Customer.findByKey (os.partner).get
        var  bp = Customer.findByKey (AuthUtil.unit.partner).get
        val razaoSocial = if (bp.company_name != "") {
            bp.company_name.is.toUpperCase
        } else {
            AuthUtil.unit.search_name.is.toUpperCase
        }
       var strXml:String ="""
        <ans:dadosSolicitante>
            <ans:contratadoSolicitante>
                <ans:codigoPrestadorNaOperadora>""" + bs.document_offsale.is + """</ans:codigoPrestadorNaOperadora>
                <ans:nomeContratado>""" + razaoSocial +
            """</ans:nomeContratado>
            </ans:contratadoSolicitante>
            <ans:profissionalSolicitante>
                <ans:nomeProfissional>""" + this.search_name.is.toUpperCase +
            """</ans:nomeProfissional>
                <ans:conselhoProfissional>6</ans:conselhoProfissional>
                <ans:numeroConselhoProfissional>""" + this.document_council.is + """</ans:numeroConselhoProfissional>
                <ans:UF>31</ans:UF>
                <ans:CBOS>225148</ans:CBOS>
            </ans:profissionalSolicitante>
        </ans:dadosSolicitante>
        """
        strXml
    }

    def toXmlTissEquipe (business:Long) = {
       var strXml:String ="""
            <ans:equipeSadt>
                <ans:grauPart>01</ans:grauPart>
                <ans:codProfissional>
                <ans:codigoPrestadorNaOperadora>250</ans:codigoPrestadorNaOperadora>
                </ans:codProfissional>
                <ans:nomeProf>""" + this.search_name.is.toUpperCase +
             """</ans:nomeProf>
                <ans:conselho>2</ans:conselho>
                <ans:numeroConselhoProfissional>""" + this.document_council.is + """</ans:numeroConselhoProfissional>
                <ans:UF>31</ans:UF>
                <ans:CBOS>225148</ans:CBOS>
            </ans:equipeSadt>        
        """
        strXml
    }
    
    def toXmlTissExecutante (business:Long) = {
       var strXml:String ="""
        <ans:dadosExecutante>
            <ans:contratadoExecutante>
                <ans:codigoPrestadorNaOperadora>06636994000113</ans:codigoPrestadorNaOperadora>
                <ans:nomeContratado>""" + this.search_name.is.toUpperCase +
            """</ans:nomeContratado>
            </ans:contratadoExecutante>
            <ans:CNES>1123323</ans:CNES>
            </ans:dadosExecutante>
        """
        strXml
    }

    def thumbUser = imagethumb.is match {
                case img:String if(img!="") => <a href={"/user/edit?id="+id.is.toString} target='_user_maste'> 
                    <img class="img-circle" href={"/user/edit?id="+id.is.toString} target='_user_maste' style="width:36px" src={thumbPath}/>
                    </a>
        case _ => <span/>
    }

    override def thumbAndName = "<div class='image_users'>"+thumbUser+"</div><a href=/user/edit?id="+id.is.toString+" target='_user_maste'>"+friendlyName+"</a>"

    
} 

object User extends User with BusinessPatternMeta[User] with OnlyCurrentUnit[User]{
    def findByFacebook(facebookId:String, facebookAccessToken:String) = {
        val uList = findAll(
            By(User.facebookId,facebookId)
            //, By(User.facebookAccessToken,facebookAccessToken)
            )
        if (uList.length > 0) {
            val user = uList(0)
            LogActor ! "Login faceb user company " + user.company.is.toString + "       id " + user.id.is.toString +
             "      user " + user.name.is + "       date " +new Date().toString
            user.lastLogin(new Date()).insecureSave
        }
        uList
    }

    // rigel 18/09/2017
    // duplicados no customer para login em agenda onine
    //    
    // like para emails separados por ,
    def countByEmail (email:String) = count(//Like(User.email,"%"+email+"%"))
        By(User.userStatus,1),
        BySql[code.model.User]("(email like ? or email like ? or email like ?) ",
            IHaveValidatedThisSQL("",""), email+"%", "%,"+email+"%", "%;"+email+"%"))
    // like para emails separados por ,
    def findByEmail(email:String) = findAll(
        By(User.userStatus,1),
        Like(User.email,"%"+email+"%"))
        override def findAll(params: QueryParam[User]*): List[User] = {
            super.findAll(By(is_employee_?,true) :: params.toList :_*)
    }

//    def findAllInCompanyOrdened = if(AuthUtil.user.isSimpleUserCommission || AuthUtil.user.isSimpleUserCommand) { 
    def findAllInCompanyOrdened = if(AuthUtil.user.isSimpleUserCommand) { 
        List(AuthUtil.user) ::: AuthUtil.user.childs
    }else{
        findAllInCompany(OrderBy(User.search_name, Ascending), By(User.userStatus, User.STATUS_OK))
    }

    def findAllInCompanyOrdenedInsecurity() ={
        
        def listParamns = OrderBy(User.search_name, Ascending) :: By(User.userStatus, User.STATUS_OK) :: Nil
        findAllInCompany( listParamns :_*)
    }

//    def findAllInCompanyOrdened(params: QueryParam[User]*) = if(AuthUtil.user.isSimpleUserCommission || AuthUtil.user.isSimpleUserCommand) {
    def findAllInCompanyOrdened(params: QueryParam[User]*) = if(AuthUtil.user.isSimpleUserCommand) {
            List(AuthUtil.user) ::: AuthUtil.user.childs
    }else{
        def listParamns = OrderBy(User.search_name, Ascending) :: By(User.userStatus, User.STATUS_OK) :: params.toList
        findAllInCompany( listParamns :_*)
    }

    override def count(params: QueryParam[User]*): Long = {
        super.count(By(is_employee_?,true) :: params.toList :_*)
    }
    

    override def findAll(): List[User] = {
        super.findAll(By(is_employee_?,true))
    }    
}

case class LoginStatus(status:Boolean, msg:String, user:User = User.create, users:List[User] = Nil){
    
}
