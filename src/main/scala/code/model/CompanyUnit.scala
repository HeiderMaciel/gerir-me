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

class CompanyUnit
  extends Audited[CompanyUnit]
  with PerCompany
  with IdPK
  with CreatedUpdated
  with CreatedUpdatedBy
  with NameSearchble[CompanyUnit]
  with ActiveInactivable[CompanyUnit]
  with Siteble {
  object partner extends MappedLongForeignKey(this, UnitPartner)
  object showInCalendar_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "showInCalendar"
  }

  object bpEmailValidate_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "bpEmailValidate"
  }

  object defaultDDD extends MappedInt(this) {
    override def defaultValue = 31
  }
  object defaultSex extends MappedPoliteString(this,8){
    override def defaultValue = {
      if (AuthUtil.company.appType.isEbelle) {
        "F" // só o ebelle o default é feminino
      } else {
        "N" // os outros é não informado
      }
    }
  }

  object costCenter extends MappedLongForeignKey(this, CostCenter) {
    override def dbIndexed_? = true
  }

  object useSingleCashier_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "useSingleCashier"
  }
  object smtpServer extends MappedPoliteString(this, 100) {
    override def defaultValue = "smtp.gmail.com"
  }
  object userSmtp extends MappedPoliteString(this, 100) {
    override def defaultValue = "suporte@vilarika.com.br"
  }

  object passwordSmtp extends MappedPoliteString(this, 100) with LifecycleCallbacks {
    override def defaultValue = "rika04011938"
    override def beforeSave() {
        super.beforeSave;
        if(userSmtp.toLowerCase.trim == "suporte@vilarika.com.br"){
          // previne que password de unidade seja alterada qdo usuário
          // manda o brower salvar sua senha
          this.set ("rika04011938");
        }
    }       
  }
  object port extends MappedPoliteString(this, 10) {
    override def defaultValue = "465"
  }
  object smtp_ssl_? extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "smtp_ssl"
  }

  object appTypeU extends MappedInt(this) {
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
     def isEbelleU = appTypeU.is == Company.SYSTEM_EBELLE
     def isGerirmeU = appTypeU.is == Company.SYSTEM_GERIRME
     def isEsmileU = appTypeU.is == Company.SYSTEM_ESMILE
     def isEdoctusU = appTypeU.is == Company.SYSTEM_EDOCTUS
     def isEgrexU = appTypeU.is == Company.SYSTEM_EGREX
     def isEphysioU = appTypeU.is == Company.SYSTEM_EPHYSIO
     def isEbellepetU = appTypeU.is == Company.SYSTEM_EBELLEPET
  }
  object calendarUrl extends MappedPoliteString(this, 100)

  override def updateShortName = false
  def getSingleton = CompanyUnit
  def SMTPMap = Map(
    "mail.smtp.starttls.enable" -> this.smtp_ssl_?.is.toString,
    "mail.smtp.ssl.enable" -> this.smtp_ssl_?.is.toString,
    "mail.debug" -> "true",
    "mail.smtp.host" -> this.smtpServer.is,
    "mail.smtp.port" -> this.port.is,
    "mail.smtp.auth" -> "true", // Provide a means for authentication. Pass it a Can, which can either be Full or Empty    
    "mail.transport.protocol" -> "smtp",
    "mail.smtp.socketFactory.port" -> this.port.is,
    "mail.smtp.socketFactory.fallback" -> "false",
    "mail.smtp.starttls.enable" -> "true")
  lazy val mailer = MailerUtil(this)
  def imagePath = "companyunit"
  def findUserToSite = User.findAll(By(User.unit, this), By(User.allowShowOnSite_?, true))
  def findMediaToSite = Media.findAll(By(Media.unit, this), By(Media.allowShowOnSite_?, true))
  def findActivitiesToSite = Activity.findAll(By(Activity.company, this.company), By(Activity.allowShowOnSite_?, true))
  lazy val getPartner: UnitPartner = {
    partner.obj match {
      case Full(currentPartner) => currentPartner
      case _ => {
        val unitPartner = UnitPartner.createInCompany.is_person_?(false).is_unit_?(true).mapIcon(4)
        partner(unitPartner)
        unitPartner
      }
    }
  }
  override def logo_web = Props.get("photo.urlbase").get + imagePath + "/" + image.is
  override def save() = {
    //      getPartner.name(BusinessRulesUtil.clearString(this.name.is)).save
    getPartner.company(this.company).name(this.name + " unit pattern").unit(this.id).save
    if (appTypeU == Empty) {
      appTypeU (AuthUtil.company.appType)
    }
    super.save()
  }

  override def delete_! = {
      if(Customer.count(By(Customer.unit,this.id)) > 0){
          throw new RuntimeException("Existe cliente para essa unidade! ")
      }
      if(User.count(By(User.unit,this.id)) > 0){
          throw new RuntimeException("Existe profissional para essa unidade! ")
      }
      if(WorkHouer.count(By(WorkHouer.unit,this.id)) > 0){
          throw new RuntimeException("Existe profissional com horário de trabalho para essa unidade! ")
      }
      if(UserCompanyUnit.count(By(UserCompanyUnit.unit,this.id)) > 0){
          throw new RuntimeException("Existe profissional associado a essa unidade! ")
      }
      if(Treatment.count(By(Treatment.unit,this.id)) > 0){
          throw new RuntimeException("Existe atendimento para essa unidade! ")
      }
      if(AccountPayable.count(By(AccountPayable.unit,this.id)) > 0){
          throw new RuntimeException("Existe lançamento financeiro para essa unidade! ")
      }

      // deleta o parceiro associado a unidade
      val ac = this.getPartner
      super.delete_!
      ac.delete_!
  }

  def unitsToShowSql = if (AuthUtil.user.isAdmin) {
      " 1 = 1 "
  } else {
      " (id = %s or (id in (select uu.unit from usercompanyunit uu where uu.user_c = %s and uu.company = %s))) ".format(AuthUtil.user.unit, AuthUtil.user.id, AuthUtil.user.company)
  }

  def replaceMessage (ac:CompanyUnit, message:String) = {
      var message_aux = message;
      //val extenso = WrittenForm (123.999.467.89)
      //println ("vaiiii ===================== " + extenso.humanize());

      message_aux = ac.company.obj.get.replaceMessage (AuthUtil.company, message_aux);

      message_aux = message_aux.replaceAll ("##unid_logo##", "<img width='100px' src='" + ac.thumb_web + "'/>");

      message_aux = message_aux.replaceAll ("##unid_nome##", ac.name.is)
      message_aux = message_aux.replaceAll ("##unid_apelido##", ac.short_name.is)
      message_aux = message_aux.replaceAll ("##unid_razao_social##", ac.getPartner.company_name)
      message_aux = message_aux.replaceAll ("##unid_telefone##", ac.getPartner.phone)
      message_aux = message_aux.replaceAll ("##unid_celular##", ac.getPartner.mobilePhone)
      message_aux = message_aux.replaceAll ("##unid_telefone2##", ac.getPartner.email_alternative)
      message_aux = message_aux.replaceAll ("##unid_email##", ac.getPartner.email)
      message_aux = message_aux.replaceAll ("##unid_doc##", ac.getPartner.document + ac.getPartner.document_company)
      
      message_aux = message_aux.replaceAll ("##unid_end_rua##", ac.getPartner.street)
      message_aux = message_aux.replaceAll ("##unid_end_nro##", ac.getPartner.number)
      message_aux = message_aux.replaceAll ("##unid_end_compl##", ac.getPartner.complement)
      message_aux = message_aux.replaceAll ("##unid_end_cep##", ac.getPartner.postal_code)
      message_aux = message_aux.replaceAll ("##unid_end_cid##", ac.getPartner.cityName)
      message_aux = message_aux.replaceAll ("##unid_end_bairro##", ac.getPartner.district)
      message_aux = message_aux.replaceAll ("##unid_end_ref##", ac.getPartner.pointofreference)
      message_aux = message_aux.replaceAll ("##unid_end_uf##", ac.getPartner.stateShortName)
      message_aux
  }

}

object CompanyUnit extends CompanyUnit with LongKeyedMapperPerCompany[CompanyUnit] with OnlyActive[CompanyUnit] with SitebleMapper[CompanyUnit] {
  def findAllForShowInCalendar(company:Long, user:Long) = findAllInCompany(
        BySql (this.unitsToShowSql,IHaveValidatedThisSQL("","")),
        By(showInCalendar_?, true),
        OrderBy(name, Ascending)
    )

  def findAllOfUser(company:Long, user:Long) = findAll(
//        BySql (this.unitsToShowSql,IHaveValidatedThisSQL("","")),
        By(showInCalendar_?, true),
        By(CompanyUnit.company, company),
        OrderBy(name, Ascending)
    )

  def findAllBySiteName(name: String) = CompanyUnit.findAll(Like(CompanyUnit.name, name), By(allowShowOnSite_?, true))(0)
  
  def findAllToSiteByCityName(cityName:String) = 
    CompanyUnit.findAll(
        BySql(" partner in (select id from business_pattern bp where bp.id = companyunit.partner and cityref in (select id from city where urlname = ?) )",IHaveValidatedThisSQL("",""), cityName),
        By(allowShowOnSite_?, true)
    )
}
