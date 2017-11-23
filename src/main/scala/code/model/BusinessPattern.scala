package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.service._
//import java.util.Calendar
import net.liftweb.common.{Box,Full}
import net.liftweb.proto._

import _root_.java.math.MathContext; 
import java.util.Date
import java.util.Calendar

trait BusinessPattern[OwnerType <: BusinessPattern[OwnerType]] extends 
Audited[OwnerType] 
with PerUnit 
with KeyedMapper[Long, OwnerType] 
with BaseLongKeyedMapper 
with PerCompany 
with IdPK 
with CreatedUpdated 
with CreatedUpdatedBy 
with NameSearchble[OwnerType] 
with ActiveInactivable[OwnerType] 
with CompanyIdable[OwnerType] 
with Restable[OwnerType]  
with Localizable 
with Siteble 
with PerCity{
  self: OwnerType =>

    override def updateIdForCompany = if(AuthUtil.?) {
        AuthUtil.company.bpIdForCompany == 0 || AuthUtil.company.bpIdForCompany == 1
    } else {
        false
    }
    //AuthUtil.company.bpIdForCompany == 0 || AuthUtil.company.bpIdForCompany == 1

    object email extends MappedPoliteString(this,150) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get.trim != ""){
            this.get.toLowerCase.trim.split(",|;").foreach((email1) => {
              if (!emailPattern.matcher(email1).matches /*&& !isNew*/) {
                throw new RuntimeException("E-mail inválido! " + email1)
              }
            })
            this.set(this.get.toLowerCase.trim)
          }
      } 
      def emailPattern = ProtoRules.emailRegexPattern.vend
    }
    object email_alternative extends MappedPoliteString(this,150) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
//          if(this.get == "(31) "){
          if(this.get.length < 8){
            this.set("")
          }
      } 
    }

    // veio do user pra ca para setar '' para todo mundo
    object groupPermission extends MappedPoliteString(this,200){
        override def defaultValue = ""
    }
    object userStatus extends MappedInt(this) with LifecycleCallbacks {
      override def dbIndexed_? = true
        override def defaultValue = StatusConstants.STATUS_OK
        override def beforeSave() {
          super.beforeSave;
            if(userStatus.is == StatusConstants.STATUS_OK && is_employee_?){
              if(status.is != StatusConstants.STATUS_OK){
                status(StatusConstants.STATUS_OK)
              }
            }
        } 
    }

    object phone extends MappedPoliteString(this,20)  with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
//          if(this.get == "(31) "){
          if(this.get.length < 8){
            this.set("")
          }
      } 
    }
    object mobilePhone extends MappedPoliteString(this,20) with LifecycleCallbacks {
    	override def dbColumnName = "mobile_phone"
        override def beforeSave() {
          super.beforeSave;
//          if(this.get == "(31) "){
          if(this.get.length < 8){
            this.set("")
          }
        } 
    }
    object mobilePhoneOp extends MappedLong(this)

    object birthday extends EbMappedDate(this){
          override def toLong: Long = is match {
            case null => 0l//(2206303200000l)*(-1)
            case d: java.util.Date => d.getTime
          }
    }
    object weight extends MappedDecimal(this,MathContext.DECIMAL64,3) {
        override def defaultValue = 0.00
    }
    object height extends MappedDecimal(this,MathContext.DECIMAL64,2) {
        override def defaultValue = 0.00
    }
    object document extends MappedPoliteString(this,20)
    object document_company extends MappedPoliteString(this,20)
    object document_identity extends MappedPoliteString(this,20)
    object document_professional extends MappedPoliteString(this,20) // carteira de trabalho
    object document_city extends MappedPoliteString(this,20) // inscrição municipal
    object document_state extends MappedPoliteString(this,20) // inscrição estadual

    object official_occupation extends MappedLong(this) // CBO criar tabela e FK
    object council extends MappedLong(this){
      // conselho do profissional crm crefito
      override def defaultValue = {
        if (AuthUtil.company.appType.isEdoctus) {
          6
        } else if (AuthUtil.company.appType.isEsmile) {
          9
        } else if (AuthUtil.company.appType.isEbellepet) {
          12
        } else if (AuthUtil.company.appType.isEphysio) {
          5
        } else {
          0
        }
      }

    }
    object document_council extends MappedPoliteString(this,20) // inscrição no conselho
    object document_offsale extends MappedPoliteString(this,20) // numero no conveio cliente
    object date_offsale extends EbMappedDate(this){ // validade da carteira do convenio cliente
          override def toLong: Long = is match {
            case null => 0l//(2206303200000l)*(-1)
            case d: java.util.Date => d.getTime
          }
    }
    object obs_offsale extends MappedPoliteString(this,255) 

    object company_name extends MappedPoliteString(this,255) with LifecycleCallbacks {
      // razão social - o name serve como fantasia
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is))
      }      
    }  
    object website extends MappedPoliteString(this,150)
    object obsComplement extends MappedPoliteString(this, 4000)

    object street extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get == ""){
              lat ("")
              lng ("")
          } else {
            this.set(BusinessRulesUtil.toCamelCase(this.is).trim)
            if (this.indexOf ("Avenida") != -1) {
              // padrão google
              this.set(this.replaceAll ("Avenida", "Av."));
            }
          }
      }      
    }  
    object district extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is).trim)
      }      
    }  
    object postal_code extends MappedPoliteString(this,255)
    object pointofreference extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is).trim)
      }      
    }  

    def cityDefaultValue:String = if(AuthUtil.? ){
          AuthUtil.unit.getPartner.city.is
      }else{
          ""
      }
    def stateDefaultValue:String = if(AuthUtil.? ){
          AuthUtil.unit.getPartner.state.is
      }else{
           ""
      }          

    def sexDefaultValue:String = if(AuthUtil.? ){
          AuthUtil.unit.defaultSex.is
      }else{
           "N"
      }          

    object city extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def defaultValue = cityDefaultValue
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is))
      }      
    }  
    object state extends MappedPoliteString(this,255){
        override def defaultValue = stateDefaultValue
    }
    object number extends MappedPoliteString(this,255)
    object complement extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is))
      }      
    }  

    object sex extends MappedPoliteString(this,8) with LifecycleCallbacks {
      override def defaultValue = sexDefaultValue 
      override def beforeSave() {
        super.beforeSave;
          if(this.get == "F"){
            if(mapIcon.isEmpty || mapIcon.toLong == 1){
                mapIcon(2);
            }
          } else if(this.get == "M"){
            if(mapIcon.isEmpty || mapIcon.toLong == 2){
                mapIcon(1);
            }
          }
      } 
    }

    object sexAnimal extends MappedPoliteString(this,8) with LifecycleCallbacks {
      override def defaultValue = sexDefaultValue 
    }

    object barcode extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get.length < 1){
            if (idForCompany != 0) {
              if (PermissionModule.anvisa_?) {
                val cal = Calendar.getInstance();
                val now  = new Date()
                cal.setTime(now);
                val year = cal.get(Calendar.YEAR);
                this.set(idForCompany.toString+"-"+((year.toString takeRight 2)))
              }
            }
          }
      } 
    }
    object indicatedby extends MappedPoliteString(this,255)
    object bp_indicatedby extends MappedLong(this)
    object bp_manager extends MappedLong(this)
    object obs extends MappedPoliteString(this,255)
    object userName extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if (this.get != "") {
	          this.set(BusinessRulesUtil.toCamelCase(this.is))
          }
      }      
    }  

    object password extends MappedPoliteString(this,50) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(self.password.is != self.lastPassword.is){
	          if(self.password.is != ""){
	            self.password(BusinessRulesUtil.md5(self.password.is))
	            self.lastPassword(self.password)
	          }
          }
      } 
    }
    object lastPassword extends MappedPoliteString(this, 50) with LifecycleCallbacks {
        override def defaultValue = ""
        override def beforeSave() {
          super.beforeSave;
          if(this.get == "")
            this.set(this.defaultValue);
        }
    }    
    object external_id extends MappedPoliteString(this,200)
    object offsale extends MappedLongForeignKey(this,OffSale)
    object senNotifications extends MappedPoliteString(this,2){
      override def defaultValue = "1"
      override def dbColumnName = "senNotifications"
    }    
    object is_prospect_? extends MappedBoolean(this){
        override def dbColumnName = "is_prospect"
        override def defaultValue = {
          if (AuthUtil.company.appType.isEsmile) {
            true
          } else {
            false
          }
        }
    }
	
	  object is_suplier_? extends MappedBoolean(this){
        override def dbColumnName = "is_suplier"
    }

    object is_candidate_? extends MappedBoolean(this){
        override def dbColumnName = "is_candidate"
    }

    def isUserDefaultValue = false

    object is_employee_? extends MappedBoolean(this){
        override def defaultValue = isUserDefaultValue
        override def dbIndexed_? = true
        override def dbColumnName = "is_employee"
    }

    object is_auxiliar_? extends MappedBoolean(this){
        override def defaultValue = isUserDefaultValue
        override def dbIndexed_? = false
        override def dbColumnName = "is_auxiliar"
    }

    object is_user_? extends MappedBoolean(this){
        override def defaultValue = isUserDefaultValue
        override def dbIndexed_? = true
        override def dbColumnName = "is_user"
    }
    def insecureSave = {
      super.save
    }
    
    override def save() ={
    	// nem mesmo adm com a permissao de profissional sem cliente estava conseguindo alterar
    	if (AuthUtil.? && !AuthUtil.user.isAdmin){
/* esta permissao nao existe mais
	        if(AuthUtil.? && AuthUtil.user.isSimpleUUserNotEdit){
	            throw new RuntimeException("Este usuário não pode editar clientes!");
	        }
*/
    	}

      if (this.city.length > 7) {
        // rigel 26/08/2017
        // enquanto não melhoramos a pesquisa de cep o codigo ibge
        // da cidade está sendo salvo no antigo campo city
        // aqui cityRef e stateRef são atualizados e o codigo
        // ibge retirado do campo city para que isso não seja feito
        // na proxima vez que o registro for salvo
        val ibge = this.city.is.slice (0,7)
        if (BusinessRulesUtil.isNumeric (ibge)) {
          val acList = City.findAll (By(City.official_code, ibge));
          if (acList.length > 0) {
            val ac = acList(0)
            this.cityRef(ac.id.is)
            this.stateRef(ac.state.is)
            val cityAux = this.city.is.slice (8,this.city.length);
            this.city (cityAux.trim);
          }
        }
      }
      if (this.species != 0) {
        this.is_animal_?.set(true)
        this.is_person_?.set(false)
        this.is_customer_?.set(false)
      }
      if (document != "") {
        if (!BusinessRulesUtil.dv_cpf (document)) {
          throw new RuntimeException("CPF Inválido! " + document)
        }
      }

      if (bp_manager == 0 && bp_indicatedby == 0 && is_animal_?) {
        // pelo menos um tem que ser informado
        throw new RuntimeException("Não é permitido cadastrar Pet sem tutor e quem indicou")
      }

      if (AuthUtil.? && this.company.is != AuthUtil.company.id.is) {
         throw new RuntimeException("Este cliente não pode ser alterado!");
      }
      val result = super.save

      // relacionamento de indicação
      if (bp_indicatedby.is != 0) {
        //println ("vaiii ======= " + bp_indicatedby.is)
        BpRelationship.addBpRelationship(id.is, bp_indicatedby.is, 24)
      }

      if ((bp_manager.is != 0) && (AuthUtil.company.appType.isEbellepet)) {
        // 27 é pet de - reverso 26 é dono de
        BpRelationship.addBpRelationship(id.is, bp_manager.is, 27)
      }

      if(!this.street.is.isEmpty || this.street.is != "") {
        BusinessPatternLocationQueeue.enqueeue(BusinessPatternQueeueDto(this.id.is))
      } else {
        this.lat.set ("");
        this.lng.set ("");
      }

      result
    }
    def defaultValueIsCustomer = true
    object is_customer_? extends MappedBoolean(this){
        //override def defaultValue = defaultValueIsCustomer
        override def dbColumnName = "is_customer"
        override def defaultValue = {
          // agora a primeira venda transforma em cliente
          if (AuthUtil.company.appType.isEsmile) {
            false
          } else {
            defaultValueIsCustomer; //true
          }
        }
    }
    object is_brand_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "is_brand"
    }    
    
    //PF ou PJ
    object is_person_? extends MappedBoolean(this) with LifecycleCallbacks {
      override def dbColumnName = "is_person"
      override def defaultValue = true
      override def beforeSave() {
        super.beforeSave;
          if(!is_person_? && !is_animal_?){
            if(mapIcon.isEmpty || mapIcon.toLong == 1 || mapIcon.toLong == 2){
                mapIcon(6); // empresa
            }
          } else if (is_animal_?) {
              mapIcon(39) // animal
          }
      } 
    }

    object is_vendor_? extends MappedBoolean(this){
        override def dbColumnName = "is_vendor"
    }
    object is_member_? extends MappedBoolean(this){
        override def dbColumnName = "is_member"
        override def defaultValue = true
    }

    object is_unit_? extends MappedBoolean(this){
      override def defaultValue = isUnitDefaultValue_?
      override def dbColumnName = "is_unit"
    }
    def isUnitDefaultValue_? = false

    object is_offsale_? extends MappedBoolean(this){
      override def defaultValue = isOffsaleDefaultValue_?
      override def dbColumnName = "is_offsale"
    }
    def isOffsaleDefaultValue_? = false

    object is_animal_? extends MappedBoolean(this){
      override def defaultValue = isAnimalDefaultValue_?
      override def dbColumnName = "is_animal"
    }
    def isAnimalDefaultValue_? = false

    object species extends  MappedInt(this); // espécie - cao gato etc
    object breed extends  MappedInt(this); // raça

    override def isPersonalName_? = is_person_?.is && !is_animal_?.is

    override def isAnimalName_? = !is_person_?.is && is_animal_?.is
    
    lazy val friendlyName = short_name.is

    object facebookId extends MappedPoliteString(this,255)
    object facebookAccessToken extends MappedPoliteString(this,255)
    object facebookUsername extends MappedPoliteString(this,255)
    object lastLogin extends EbMappedDateTime(this)
    object lastBuyDate extends EbMappedDate(this)
    object lastSaleDate extends EbMappedDate(this)
    object beginDate extends EbMappedDate(this)
    object deathDate extends EbMappedDate(this)
    object honorifictitle extends  MappedInt(this) {
      override def defaultValue = 1
    }
  

    object civilstatus extends MappedInt(this) {
        override def defaultValue = 0
        /*
        0 - indefinido
        1 - casado .....
        2 - solteiro
        3 - separado
        4 - divorciado
        5 - viuvo
        6 - noivo
        7 - outros
        */
    }    

    object placeofbirth extends MappedLong(this) // naturalidade
    object instructiondegree extends MappedLong(this) //
    object occupation extends MappedLongForeignKey(this,Occupation) // profissao
    object typeOfIndustry extends MappedLong(this) // ramo de atividade
    object mapIcon extends MappedLongForeignKey(this,MapIcon) {
      override def defaultValue = 1
    }

    def iconPath = mapIcon.obj match {
        case Full(t) => t.iconPath.is
        case _ => ""
    }

    def totalDebit:Double = 0.00

/*
    lazy val incomplete_email = {
        if (email.is.isEmpty && AuthUtil.unit.bpEmailValidate_?) {
            true
          } else {
            false
          }
    }
*/

    lazy val hasDebits = totalDebit < 0
    lazy val hasCredits = totalDebit > 0
    lazy val birthdayThisMonth = {
        if(birthday.is != null){
//          println (" ============================ vai idade extenso " + ageBirth);
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH)
            cal.setTime(birthday.is)
            month == cal.get(Calendar.MONTH)
        }else{
            false
        }
    }


    def bodyMassIndex:BigDecimal = if (height > 0.0) {
        weight / (height * height)
      } else {
        0.0
      }

    // Body mass index - IMC
    def BMI:String = {

      var bmistr = bodyMassIndex.toString;

      var straux = if (bodyMassIndex == 0.0) {
        " "
      } else if (bodyMassIndex < 17) {  
        " Muito abaixo do peso "
      } else if (bodyMassIndex < 18.49) {
        " Abaixo do peso "
      } else if (bodyMassIndex < 24.99) {
        " Peso normal "
      } else if (bodyMassIndex < 29.99) {
        " Acima do peso "
      } else if (bodyMassIndex < 34.99) {
        " Obesidade I "
      } else if (bodyMassIndex < 39.99) {
        " Obesidade II (severa) "
      } else {
        " Obesidade III (mórbida) "
      }
      if (bodyMassIndex == 0.0) {
        ""
      } else {
        if (bmistr.indexOf(".") != -1) {
          // concat "00000" pra garantir 2 casas após o .
          bmistr.substring(0, (bmistr + "00000").indexOf (".") + 3) + straux
        } else {
          bmistr + straux
        }
      }
    }

    def ageBirth = Project.dateToAge (birthday)
    def ageBegin = Project.dateToAge (beginDate)

    def thumbAndName = "<div class='image_users'>"+thumb("32")+"</div>"+friendlyName
    
    def alerts_messages:List[String] = {
        def message_birthday = if (birthdayThisMonth && !AuthUtil.company.appType.isEgrex) this.name.is + " faz aniversário este mês dia %s!".format(Project.dateToMonthAndDay(this.birthday.is)) else ""
        def message_debit  = if (hasDebits) this.name.is + " possui débitos anteriores no valor de %s!".format(totalDebit.toString) else "" 
        def message_credit  = if (hasCredits) this.name.is + " possui créditos anteriores no valor de %s!".format(totalDebit.toString) else ""
//        def message_email = if (incomplete_email) "Email não informado!" else ""
        def validateMessage = {
            try{
                this.name.beforeSave
                ""
            }catch{
                case e:RuntimeException => e.getMessage+"\n\nVocê não será capaz de efetuar transações com este cliente até que o nome no cadastro seja corrigido!"
                case _ => ""
            }

        }
        val l = message_credit :: message_debit :: message_birthday :: validateMessage :: 
        BusinessPatternConsideration.noficationsNow(this.id.is) 
        if (PermissionModule.bpmonthly_?) {
            l ++ BpMonthly.noficationsNow(this.id.is)
        } else {
          l
        }

    }

    override def validateDuplicatedName = {
      if(!allowDuplicated_? && self.getSingleton.count(
        By(self.company, self.company.is), 
        Like(self.name, BusinessRulesUtil.toCamelCase(self.name.is.trim)), 
        By(self.document, self.document.is),
        NotBy(self.id, self.id.is)) > 0){
        val doc = if (isPersonalName_?) {
            " e documento: %s".format(self.document.is)
          } else {
            ""
          }
        throw new RuntimeException("Já existe um registro com esse nome : %s".format(self.name.is) + doc)
      }
    }

    override def delete_! = {
        if(Treatment.count(By(Treatment.customer,this.id)) > 0){
            throw new RuntimeException("Existe histórico para este cliente!")
        }
        if(Treatment.count(By(Treatment.user,this.id)) > 0){
            throw new RuntimeException("Existe histórico para este profissional!")
        }        
        if(TreatmentDetail.count(By(TreatmentDetail.auxiliar,this.id)) > 0){
            throw new RuntimeException("Existe histórico para este auxiliar!")
        }        
        if(BusyEvent.count(By(BusyEvent.user,this.id)) > 0){
            throw new RuntimeException("Existe bloqueio de agenda para este profissional!")
        }        
        if(WorkHouer.count(By(WorkHouer.user,this.id)) > 0){
            throw new RuntimeException("Existe horário de trabalho para este profissional!")
        }        
        if(UserActivity.count(By(UserActivity.user,this.id)) > 0){
            throw new RuntimeException("Existe produto/Serviço para este profissional!")
        }        
        if(AccountPayable.count(By(AccountPayable.user,this.id)) > 0){
            throw new RuntimeException("Existe lançamento financeiro para este parceiro!")
        }        
        if(Company.count(By(Company.partner,this.id)) > 0){
            throw new RuntimeException("Existe empresa onde este parceiro é contato!")
        }        
        if(CompanyUnit.count(By(CompanyUnit.partner,this.id)) > 0){
            throw new RuntimeException("Existe unidade onde este parceiro é ligado!")
        }        
        if(BpRelationship.count(By(BpRelationship.business_pattern,this.id)) > 0){
            throw new RuntimeException("Existe relacionamento para este parceiro!")
        }
        if(BpRelationship.count(By(BpRelationship.bp_related,this.id)) > 0){
            throw new RuntimeException("Existe relacionamento para este parceiro!")
        }
        if(QuizApplying.count(By(QuizApplying.business_pattern,this.id)) > 0){
            throw new RuntimeException("Existe questionário/prontuário para este cliente/paciente!")
        }
        super.delete_!
    }

    def imagePath = "business_pattern"

    lazy val firstName:String = {
      if (name.is.indexOf(" ") > 0) {
        name.is.substring (0,name.is.indexOf(" "))
      } else {
        name.is
      }
    }

    def cityName = cityRef.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def stateShortName = stateRef.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    def offSaleShortName = offsale.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    def occupationShortName = occupation.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def full_address = street.is + ", "+number.is+"- "+district.is+", "+cityName+" - "+stateShortName

    def resetPasswordKey = BusinessRulesUtil.md5(this.id.is.toString)+BusinessRulesUtil.md5(this.email.is.toString)+BusinessRulesUtil.md5(this.password.is.toString)

    object Sexs extends Enumeration {
     type Sexs = Value
     val Male = Value("M")
     val Female = Value("F")
     val Undefined = Value("N")
    }

}

trait BusinessPatternMeta[A <: LongKeyedMapper[A]] extends LongKeyedMapperPerCompany[A] with  OnlyActive[A] with StatusConstants with MetaRestable[A]{
	self: A with OnlyActive[A] with ActiveInactivable[A] with PerCompany with MetaMapper[A] =>
	override def dbTableName = "business_pattern"

    def findAllInCompanies = {
        super.findAll
    }
}