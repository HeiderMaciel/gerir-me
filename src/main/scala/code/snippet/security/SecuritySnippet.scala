package code
package snippet

import net.liftweb._
import http._
import code.actors._
import code.util._
import model._
import http.js._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text }
import java.util.Random

object SecuritySnippet {

  def unLogged(in: NodeSeq): NodeSeq = {

    for {
      r <- S.request if r.post_?
      name <- S.param("user")
      password <- S.param("password")
      company <- S.param("company")
      } {
          try {
                println ("vaiiiiii TRY SECURITY SNIPPET ==================================")
            val loginStatus: LoginStatus = User.login(name, password, Company.findByKey(company.toLong).get)
            loginStatus.status match{
              case true =>  {
                println ("vaiiiiii SECURITY SNIPPET ==================================")
                LogActor ! "Login succeed PROCURAR AQUI ACHO QUE NAO USA MAIS "+name+" Company : "+company+ "userId"+loginStatus.user.id.is;
                LogActor ! "Login succeed "+name+" Company : "+company+ "userId"+loginStatus.user.id.is;
                AuthUtil << loginStatus.user
                if (AuthUtil.company.appType.isEgrex) {
                  S.redirectTo("/customer/list")
                } else if(PermissionModule.treatment_? && 
                  (AuthUtil.user.isSimpleUserCalendar || AuthUtil.user.isCalendarUser ||
                   AuthUtil.user.isSimpleUserCalendarView)) {
                  S.redirectTo("/calendar")
                } else if(PermissionModule.treatment_? && (AuthUtil.user.isSimpleUserCommand || AuthUtil.user.isCommandUser  || AuthUtil.user.isCommandPwd)){
                  S.redirectTo("/command/user_command")
                } else if (AuthUtil.user.isSimpleUserCommission) {
                  S.redirectTo("/commission_conference_user")
                }else if(PermissionModule.inventory_?){
                  S.redirectTo("/product/control_panel")
                }else if(PermissionModule.financial_?){
                  S.redirectTo("/financial/account_register")
                }else{
                  S.redirectTo("/customer/list")
                }
              }
              case _ =>{
                      LogActor ! "Login failed "+name+" Company"+company;    
                      S.error("Usuário ou senha inválida!")
                      S.redirectTo("/")
              }
                  }
                  } catch {
                    case e: NoSuchElementException => { LogActor ! "Empresa {"+company+"} não existe!"; S.error("Empresa não existe!");   }
                    case e: NumberFormatException => { LogActor ! "O campo empresa deve ser numérico! {"+company+"} "; S.error("O campo empresa deve ser numérico!"); }
                  }
                }

                if (!AuthUtil.?)
                in
                else
                NodeSeq.Empty
      }  
      //User permissions
      def isSuperAdmin(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isSuperAdmin)
        in
        else
        NodeSeq.Empty
      }

      def isNotAdminRead(in:NodeSeq):NodeSeq ={
        if (!AuthUtil.user.isAdminRead)
        in
        else
        NodeSeq.Empty
      }

      def isPeopleManager(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isPeopleManager)
        in
        else
        NodeSeq.Empty
      }

      def isInventoryManager(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isInventoryManager)
        in
        else
        NodeSeq.Empty
      }

      def isCustomer(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isCustomer)
        in
        else
        NodeSeq.Empty
      }

      def isServiceUser(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isServiceUser)
        in
        else
        NodeSeq.Empty
      }

      def isFinancialUser(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isFinancialUser)
        in
        else
        NodeSeq.Empty
      }

      def isCalendarUser(in:NodeSeq):NodeSeq ={
        if ((AuthUtil.user.isSimpleUserCalendar || AuthUtil.user.isCalendarUser))
        in
        else
        NodeSeq.Empty
      }

      def isCommandUser(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isCommandUser)
        in
        else
        NodeSeq.Empty
      }

      def isCommandPwd(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.isCommandPwd)
        in
        else
        NodeSeq.Empty
      }

      //System
      def isEbelleSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEbelle)
        in
        else
        NodeSeq.Empty
      }

      def isGerirmeSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isGerirme)
        in
        else
        NodeSeq.Empty
      }
      def isEsmileSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEsmile )
        in
        else
        NodeSeq.Empty
      }
      def isMedicalSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEdoctus || AuthUtil.company.appType.isEphysio
          || AuthUtil.company.appType.isEsmile || AuthUtil.company.appType.isEbellepet)
        in
        else
        NodeSeq.Empty
      }
      def isMedNotPetSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEdoctus || AuthUtil.company.appType.isEphysio
          || AuthUtil.company.appType.isEsmile)
        in
        else
        NodeSeq.Empty
      }
      def isNotMedicalSystem(in:NodeSeq):NodeSeq ={
        if (!(AuthUtil.company.appType.isEdoctus || AuthUtil.company.appType.isEphysio
          || AuthUtil.company.appType.isEsmile || AuthUtil.company.appType.isEbellepet))
        in
        else
        NodeSeq.Empty
      }
      def isEdoctusSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEdoctus)
        in
        else
        NodeSeq.Empty
      }
      def isEgrexSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEgrex)
        in
        else
        NodeSeq.Empty
      }
      def isNotEgrexSystem(in:NodeSeq):NodeSeq ={
        if (!AuthUtil.company.appType.isEgrex)
        in
        else
        NodeSeq.Empty
      }
      def isEphysioSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEphysio)
        in
        else
        NodeSeq.Empty
      }
      def isEbellepetSystem(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.appType.isEbellepet)
        in
        else
        NodeSeq.Empty
      }

      // Específicos de controle
      def isDeleteCalendar(in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.canDeleteCalendarEvents_?)
        in
        else
        NodeSeq.Empty
      }

      def isShowSalesToUser(in:NodeSeq):NodeSeq ={
        if (AuthUtil.company.showSalesToUser_?)
        in
        else
        NodeSeq.Empty
      }
      
      def isShowQuizToUser(in:NodeSeq):NodeSeq ={
        if (PermissionModule.quiz_? && AuthUtil.company.isMedical
          && AuthUtil.user.isRecords) {
          in
        } else if (PermissionModule.quiz_? && !AuthUtil.company.isMedical) {
          in
        } else {
          NodeSeq.Empty
        }
      }


      //PermissionModule
      def hasFinancialModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.financial_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasFinancialadModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.financialad_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasInventoryModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.inventory_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasServiceModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.service_?)
          in
        else
          NodeSeq.Empty
      }      
      // tá os 2 empty mesmo por enquanto
      def hasProjectModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.project_?)
          NodeSeq.Empty
        else
          NodeSeq.Empty
      }
      def hasEventModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.event_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasClassModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.class_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasPackageModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.package_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasFidelityModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.fidelity_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasOffSaleModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.offsale_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasReferralModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.referral_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasRelationModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.relation_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasAuditModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.audit_? || AuthUtil.user.isSuperAdmin)
          in
        else
          NodeSeq.Empty
      }      
      def hasPortalModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.portal_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasGalleryModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.gallery_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasUnitModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.unit_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasTissModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.tiss_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasAnvisaModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.anvisa_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasNotAnvisaModule(in:NodeSeq):NodeSeq ={
        if (!PermissionModule.anvisa_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasAuxiliarModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.auxiliar_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasNotAuxiliarModule(in:NodeSeq):NodeSeq ={
        if (!PermissionModule.auxiliar_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasCrmModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.crm_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasBpmonthlyModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.bpmonthly_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasQuizModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.quiz_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasBudgetModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.budget_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasBMIndexModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.bmindex_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasContactModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.contact_?)
          in
        else
          NodeSeq.Empty
      }      
      def hasCostcenterModule(in:NodeSeq):NodeSeq ={
        if (PermissionModule.costcenter_?)
          in
        else
          NodeSeq.Empty
      }      

//
      def calendarFixed (in:NodeSeq):NodeSeq ={
        if (AuthUtil.user.calendarFixed_?)
        in
        else
        NodeSeq.Empty
      }

      def editBpIdfC(in:NodeSeq):NodeSeq ={
        if ((AuthUtil.company.bpIdForCompany != 0) || AuthUtil.user.isAdmin)
        in
        else
        NodeSeq.Empty
      }
      def DoNoteditBpIdfC(in:NodeSeq):NodeSeq ={
        if ((AuthUtil.company.bpIdForCompany == 0) && !AuthUtil.user.isAdmin)
        in
        else
        NodeSeq.Empty
      }
      def logged(in: NodeSeq): NodeSeq = {
        if (AuthUtil ?)
        in
        else
        NodeSeq.Empty
      }

}

