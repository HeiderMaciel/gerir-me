package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._ 
import _root_.java.math.MathContext; 

class PermissionModule extends LongKeyedMapper[PermissionModule] with PerCompany with IdPK 
	with CreatedUpdated with CreatedUpdatedBy with ActiveInactivable[PermissionModule] {
    def getSingleton = PermissionModule 
	object name extends MappedPoliteString(this,255)
}

object PermissionModule extends PermissionModule with LongKeyedMapperPerCompany[PermissionModule]  with  OnlyActive[PermissionModule]{
	lazy val FINANCIALAD_MODULE_NAME = "FINANCIALAD";
	lazy val FINANCIAL_MODULE_NAME = "FINANCIAL";
	lazy val INVENTORY_MODULE_NAME = "INVENTORY";
	lazy val SERVICE_MODULE_NAME = "SERVICE";
	lazy val CRUDE_MODULE_NAME = "CRUDE";
	lazy val TREATMENT_MODULE_NAME = "TREATMENT";
	lazy val MAP_MODULE_NAME = "MAP";
	lazy val CRM_MODULE_NAME = "CRM";
	lazy val PROJECT_MODULE_NAME = "PROJECT";
	lazy val EVENT_MODULE_NAME = "EVENT"; // eventos
	lazy val CLASS_MODULE_NAME = "CLASS"; //treinamentos
	lazy val PACKAGE_MODULE_NAME = "PACKAGE"; //pacotes
	lazy val FIDELITY_MODULE_NAME = "FIDELITY"; //pontos fidelidade
	lazy val OFFSALE_MODULE_NAME = "OFFSALE"; //convênio
	lazy val RELATION_MODULE_NAME = "RELATION"; //relacionamento
	lazy val AGENDA_MODULE_NAME = "AGENDA"; // ainda nao esta sendo usado
	lazy val AUDIT_MODULE_NAME = "AUDIT"; //
	lazy val PORTAL_MODULE_NAME = "PORTAL"; //
	lazy val GALLERY_MODULE_NAME = "GALLERY"; //
	lazy val REFERRAL_MODULE_NAME = "REFERRAL"; // indicações
	lazy val UNIT_MODULE_NAME = "UNIT"; // para mais de uma unidade
	lazy val TISS_MODULE_NAME = "TISS"; // 
	lazy val ANVISA_MODULE_NAME = "ANVISA"; // para prontuario idforcompany/AA
	lazy val AUXILIAR_MODULE_NAME = "AUXILIAR"; // para assistente na agenda / comissionamanto
	lazy val QUIZ_MODULE_NAME = "QUIZ"; // para prontuário anamnese e avaliações
	lazy val BPMONTHLY_MODULE_NAME = "BPMONTHLY"; // para mensalidades
	lazy val BUDGET_MODULE_NAME = "BUDGET"; // para orçamentos e lancto fora da agenda e do caixa
	lazy val BMINDEX_MODULE_NAME = "BMINDEX"; // peso e altura IMC = BMI body mass index
	lazy val CONTACT_MODULE_NAME = "CONTACT"; // importacao e contatos/mailing
	lazy val COSTCENTER_MODULE_NAME = "COSTCENTER"; // centros de custos
	

	def financial_? = PermissionModule.countInCompany(
											 By(PermissionModule.name,PermissionModule.FINANCIAL_MODULE_NAME)
											) > 0  &&  (AuthUtil.user.isFinancialUser || AuthUtil.user.isCashierGeneral || AuthUtil.user.isCashier)

	def financialManager_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.FINANCIAL_MODULE_NAME)) > 0 && AuthUtil.user.isFinancialManager

	def inventory_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.INVENTORY_MODULE_NAME)) > 0 && AuthUtil.user.isInventoryUser

	def inventoryManager_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.INVENTORY_MODULE_NAME)) > 0 && AuthUtil.user.isInventoryManager

	def peopleManager_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CRUDE_MODULE_NAME)) > 0 && AuthUtil.user.isPeopleManager

	def customer_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CRUDE_MODULE_NAME)) > 0 && AuthUtil.user.isCustomer

	def records_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CRUDE_MODULE_NAME)) > 0 && AuthUtil.user.isRecords

	def crude_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CRUDE_MODULE_NAME)) > 0 && AuthUtil.user.isServiceUser

	def report_? = AuthUtil.user.isReportUser

	def service_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.SERVICE_MODULE_NAME)) > 0 && AuthUtil.user.isServiceUser

	def serviceManager_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CRUDE_MODULE_NAME)) > 0 && AuthUtil.user.isServiceManager

	def treatment_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.TREATMENT_MODULE_NAME)) > 0  && 
	(AuthUtil.user.isServiceUser || AuthUtil.user.isCalendarUser || 
	 AuthUtil.user.isSimpleUserCalendar || AuthUtil.user.isSimpleUserCalendarView || 
	 AuthUtil.user.isCommandUser || AuthUtil.user.isCommandPwd)

	def calendar_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.TREATMENT_MODULE_NAME)) > 0  && AuthUtil.user.isCalendarUser

	def command_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.TREATMENT_MODULE_NAME)) > 0  && (AuthUtil.user.isCommandUser || AuthUtil.user.isCommandPwd)

	def commandTerm_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.TREATMENT_MODULE_NAME)) > 0  && (AuthUtil.user.isCommandTerm)

	def map_? = true//PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.MAP_MODULE_NAME)) > 0	

//  ============= MODULOS 

	def financialad_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.FINANCIALAD_MODULE_NAME)) > 0
	def project_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.PROJECT_MODULE_NAME)) > 0
	def event_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.EVENT_MODULE_NAME)) > 0
	def class_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CLASS_MODULE_NAME)) > 0
	def package_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.PACKAGE_MODULE_NAME)) > 0
	def fidelity_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.FIDELITY_MODULE_NAME)) > 0
	def offsale_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.OFFSALE_MODULE_NAME)) > 0
	def relation_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.RELATION_MODULE_NAME)) > 0
	def referral_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.REFERRAL_MODULE_NAME)) > 0

	def agenda_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.AGENDA_MODULE_NAME)) > 0
	def audit_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.AUDIT_MODULE_NAME)) > 0
	def portal_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.PORTAL_MODULE_NAME)) > 0
	def gallery_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.GALLERY_MODULE_NAME)) > 0
	def unit_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.UNIT_MODULE_NAME)) > 0
	def tiss_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.TISS_MODULE_NAME)) > 0
	def anvisa_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.ANVISA_MODULE_NAME)) > 0
	def auxiliar_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.AUXILIAR_MODULE_NAME)) > 0
	def quiz_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.QUIZ_MODULE_NAME)) > 0
	def bpmonthly_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.BPMONTHLY_MODULE_NAME)) > 0
	def crm_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CRM_MODULE_NAME)) > 0
	def budget_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.BUDGET_MODULE_NAME)) > 0
	def bmindex_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.BMINDEX_MODULE_NAME)) > 0
	def contact_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.CONTACT_MODULE_NAME)) > 0
	def costcenter_? = PermissionModule.countInCompany(By(PermissionModule.name,PermissionModule.COSTCENTER_MODULE_NAME)) > 0

	def setModule (company:Company, module:String) = {
		val pm = PermissionModule.findAll (By(PermissionModule.company, company),
			By(PermissionModule.name, module))
		pm(0).status (1).save
	}
	def resetModule (company:Company, module:String) = {
		val pm = PermissionModule.findAll (By(PermissionModule.company, company),
			By(PermissionModule.name, module))
		pm(0).status (0).save
	}
}

object UserGroupPermission{
	lazy val ADMIN_USER = 1;
	lazy val INVENTORY_USER = 2;
	lazy val FINANCIAL_USER = 3;
	lazy val CALENDAR_USER = 4;
	lazy val SERVICE_USER = 5;
	lazy val SIMPLE_USER_CALENDAR = 6;
	lazy val INVENTORY_MANAGER = 7;
	lazy val FINANCIAL_MANAGER = 8;
	lazy val SERVICE_MANAGER = 9;
	lazy val REPORT_USER = 10; 
	lazy val PEOPLE_MANAGER = 11;
	lazy val CASHIER = 12;
	lazy val CUSTOMER = 13;
	lazy val ADMIN_READ = 14;
	lazy val CASHIER_GENERAL = 15;
	lazy val SIMPLE_USER_COMMISSION = 16;
	lazy val SIMPLE_USER_COMMAND = 17;
	lazy val COMMAND_PWD = 18;
	lazy val RECORDS = 19; // prontuário
	lazy val COMMAND_USER = 20;
	lazy val SIMPLE_USER_CALENDAR_VIEW = 21;
	lazy val SUPER_ADMIN = 1000;
}

