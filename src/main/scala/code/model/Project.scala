package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import json._
import net.liftweb.common.{Box,Full}

import java.util.Date

class Project1 extends Audited[Project1] with KeyedMapper[Long, Project1] with BaseLongKeyedMapper
     with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with NameSearchble[Project1] 
     with ActiveInactivable[Project1] with PerUnit with CompanyIdable[Project1]
     with Siteble with net.liftweb.common.Logger {
    def getSingleton = Project1
    override def updateShortName = false

    object bp_sponsor extends MappedLongForeignKey(this, Customer)
    object bp_manager extends MappedLong(this)
    object startAt extends EbMappedDateTime(this) {
        override def defaultValue = new Date()
    }
    object endAt extends EbMappedDateTime(this)
    object projectClass extends  MappedLongForeignKey(this, ProjectClass)
    object projectStage extends  MappedLongForeignKey(this, ProjectStage)
	object obs extends MappedString(this, 4000)
    object about extends MappedString(this, 4000)
    object goal extends MappedString(this, 4000)
    object audience extends MappedString(this, 4000)
    object schedule extends MappedString(this, 4000)
    object numberofguests extends MappedInt(this)
    object costCenter extends MappedLongForeignKey(this.asInstanceOf[MapperType], CostCenter) {
        override def dbIndexed_? = true
    }

    object projectOpt extends MappedInt(this) {// projeto evento, grupo orçamento...
        override def defaultValue = 0
/*
        if (AuthUtil.company.appType.isEsmile) {
            5 // budget
        } else if (AuthUtil.company.appType.isEgrex) {
            3 // group
        } else {
            5 // budget
            //2 // event
        }
*/
    }

    def prjOpt (opt : String) : Int = {
        if (opt == "budget" || opt == "5") {
            Project1.OPT_BUDGET
        } else if (opt == "event" || opt == "2") {
            Project1.OPT_EVENT
        } else if (opt == "group" || opt == "3") {
            Project1.OPT_GROUP
        } else if (opt == "") {
            // retirar depois que resolver o parm no snippet
            this.projectOpt.is
        } else {
            Project1.OPT_PROJECT // project
        }
    }

    def prjOptDesc = {
        if (this.projectOpt == Project1.OPT_PROJECT) {
            "Projeto"
        } else if (this.projectOpt == Project1.OPT_EVENT) {
            "Evento"
        } else if (this.projectOpt == Project1.OPT_GROUP) {
            "Grupo"
        } else if (this.projectOpt == Project1.OPT_BUDGET) {
            "Orçamento"
        } else {
            "Projeto"
        }
    }

    def bp_managerName : String = {
        if (bp_manager != 0) {
          val ac = Customer.findByKey(bp_manager).get
          ac.name;
        } else {
          ""
        }
    }
    def bp_sponsorName : String = {
        if (bp_sponsor != 0) {
          val ac = Customer.findByKey(bp_sponsor).get
          ac.name;
        } else {
          ""
        }
    }

    def hasSponsor = bp_sponsor.obj  match {
        case Full(a) => true
        case _ => false
    }

    override def save = {
// alguns grupos da cczs não tem sponsor
//        if (!hasSponsor) {
//          throw new RuntimeException("Um cliente/sponsor é obrigatório!")
//        }
        if (this.name == "" && hasSponsor) {
            this.name (Customer.findByKey(bp_sponsor).get.name.is + " " + Project.dateToMonthAndYear(Project.today))
            if (this.short_name == "") {
                this.short_name (Customer.findByKey(bp_sponsor).get.short_name.is)
            }
        }
        val isNew = this.id.is match {
            case p:Long if(p > 0) => false
            case _ => true
        }
        val r = super.save
        //if(isNew){ agora faz sempre
        addStakeholder(bp_manager.is, StakeHolderType.MANAGER)
        addStakeholder(bp_sponsor.is, StakeHolderType.SPONSOR)
        //}
        if(ProjectSection.count(By(ProjectSection.project,this.id)) < 1){
            val ac = ProjectSection.createInCompany.
            project(this.id).
            title(this.name)
            ac.save
        }
        r
    }

    def updateFromJson(json: JsonAST.JObject) = {
        json.values.map((value) =>{
            this.fieldByName(value._1).get.set(value._2)
        })
        this.save
    }
    def imagePath = "Project"

    def projectClassName = projectClass.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def projectClassTerms = projectClass.obj match {
        case Full(t) => t.terms.is
        case _ => 0l
    }
    def projectClassPrintType = projectClass.obj match {
        case Full(t) => t.printType.is.toString
        case _ => "0"
    }
    def projectClassHeaderStyle = projectClass.obj match {
        case Full(t) => t.headerStyle.is
        case _ => ""
    }
    def projectClassBodyStyle = projectClass.obj match {
        case Full(t) => t.bodyStyle.is
        case _ => ""
    }
    def projectClassContentStyle = projectClass.obj match {
        case Full(t) => t.contentStyle.is
        case _ => ""
    }
    def projectClassColumns = projectClass.obj match {
        case Full(t) => t.columns.is
        case _ => ""
    }

    def removeStakeholder(idStaclHolder:Long) = {
        StakeHolder.findByKey(idStaclHolder).get.delete_!
    }
 
    def stakeholderByBP (project:Long, customer:Long, stakeholdertype:Long) = {
        StakeHolder.count(
                            By(StakeHolder.business_pattern, customer), 
                            By(StakeHolder.project, project),
                            By(StakeHolder.stakeHolderType, stakeholdertype)
                        )
    }

    def addStakeholder(bpId:Long, stakeholdertype:Long){
        val stakes = Project1.stakeholderByBP(this.id.is, bpId, stakeholdertype)
        if (stakes > 0) {
        } else {
            StakeHolder
                .createInCompany
                .project(this.id.is)
                .startAt(this.startAt)
                .endAt(this.endAt)
                .stakeHolderType(stakeholdertype)
                .business_pattern(bpId)
                .save
        }
    }

    override def delete_! = {
        if(ProjectTreatment.count(By(ProjectTreatment.project,this.id)) > 0){
            throw new RuntimeException("Existe item ligado a este " + this.prjOptDesc)
        }
        if(ProjectSection.count(By(ProjectSection.project,this.id)) > 0){
            throw new RuntimeException("Existe Seção ligada a este " + this.prjOptDesc)
        }
        super.delete_!;
    }

}

object Project1 extends Project1 with LongKeyedMapperPerCompany[Project1] with OnlyActive[Project1]{
  val OPT_PROJECT = 1
  val OPT_EVENT = 2
  val OPT_GROUP = 3
  //val opportunity = 4 deal
  val OPT_BUDGET = 5

    
    def createFromJson(json: JsonAST.JObject) = decodeFromJSON_!(json, true)
    override def dbTableName = "project"
}

class ProjectType extends Audited[ProjectType] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[ProjectType] with ActiveInactivable[ProjectType]{ 
    def getSingleton = ProjectType
    override def updateShortName = false
    object obs extends MappedPoliteString(this,255)
    object class_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "class"
    }
}

object ProjectType extends ProjectType with LongKeyedMapperPerCompany[ProjectType]  with  OnlyActive[ProjectType]

class ProjectClass extends Audited[ProjectClass] with PerCompany with IdPK 
    with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[ProjectClass] with ActiveInactivable[ProjectClass]
    with Siteble 
    { 
    def getSingleton = ProjectClass
    override def updateShortName = false
    object obs extends MappedPoliteString(this,255)
    object projectType extends  MappedLongForeignKey(this, ProjectType)
    object terms extends  MappedLongForeignKey(this, Terms)
    object printType extends MappedInt(this)with LifecycleCallbacks { 
        override def defaultValue = 0
        // table 0
        // list 1
    }
    object headerStyle extends MappedPoliteString(this,255) {
        override def defaultValue = " class='withnoborder' style='border:0'";
    }
    object bodyStyle extends MappedPoliteString(this,255) {
        override def defaultValue = "style='border:0'";
    }
    object contentStyle extends MappedPoliteString(this,255) {
        override def defaultValue = "style='border:0'";
    }
    object columns extends MappedPoliteString(this,255)

    def projectTypeName = projectType.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def imagePath = "Projectclass"

}

object ProjectClass extends ProjectClass with LongKeyedMapperPerCompany[ProjectClass]  with  OnlyActive[ProjectClass]

class StakeHolder extends Audited[StakeHolder] with KeyedMapper[Long, StakeHolder] with BaseLongKeyedMapper
     with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy
     with ActiveInactivable[StakeHolder] with PerUnit with CompanyIdable[StakeHolder] {
    def getSingleton = StakeHolder

    object project extends MappedLong(this)
    object business_pattern extends MappedLongForeignKey(this, Customer)
    object stakeHolderType extends  MappedLongForeignKey(this, StakeHolderType)
    object startAt extends EbMappedDateTime(this) {
        override def defaultValue = new Date()
    }
    object endAt extends EbMappedDateTime(this)
    object obs extends MappedString(this, 4000)
    
    object approved extends MappedString(this, 1) {
        override def defaultValue = "N"; //StakeHolder.Approveds.Undefined;
    }

    def bpName = business_pattern.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def stakeHolderTypeName = stakeHolderType.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }
}

object StakeHolder extends StakeHolder with LongKeyedMapperPerCompany[StakeHolder] with OnlyCurrentCompany[StakeHolder] with OnlyActive[StakeHolder]{
    object Approveds extends Enumeration {
     type Approveds = Value
     val Approved = Value("A")
     val NotApproved = Value("R")
     val Undefined = Value("N")
    }

}

class StakeHolderType extends Audited[StakeHolderType] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[StakeHolderType] with ActiveInactivable[StakeHolderType]{ 
    def getSingleton = StakeHolderType
    override def updateShortName = false
    object obs extends MappedPoliteString(this,255)
    object orderInReport extends MappedLong(this){
        override def defaultValue = 10
    }
}

object StakeHolderType extends StakeHolderType with LongKeyedMapperPerCompany[StakeHolderType]  with  OnlyActive[StakeHolderType]{
    val MANAGER = 2
    val SPONSOR = 1
}

class ProjectStage extends Audited[ProjectStage] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[ProjectStage] with ActiveInactivable[ProjectStage]{ 
    def getSingleton = ProjectStage
    override def updateShortName = false
    object obs extends MappedPoliteString(this,255)
}

object ProjectStage extends ProjectStage with LongKeyedMapperPerCompany[ProjectStage]  with  OnlyActive[ProjectStage]

class ProjectTreatment extends Audited[ProjectTreatment] 
    with PerCompany with IdPK 
    with CreatedUpdated with CreatedUpdatedBy 
    with ActiveInactivable[ProjectTreatment] 
    with net.liftweb.common.Logger {
    def getSingleton = ProjectTreatment
    object project extends MappedLongForeignKey(this, Project1)
    object treatment extends MappedLongForeignKey(this, Treatment)
    object treatmentDetail extends MappedLongForeignKey(this, TreatmentDetail)
    object title extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is))
      }      
    }  
    object obs extends MappedPoliteString(this,2000)
    object projectSection extends MappedLongForeignKey(this, ProjectSection)
    object treatmentDetailOk extends MappedLongForeignKey(this, TreatmentDetail)

    def countProjectTreatment (treatment:Long, treatmendDetail:Long) = {
        ProjectTreatment.count(By(ProjectTreatment.treatment, treatment),
            By(ProjectTreatment.treatmentDetail, treatmentDetail))
    }
     
    def createProjectTreatment(project:Long, projectSection:Long, treatment:Long, treatmentDetail:Long):Long = {
        var psl = projectSection
        if (psl == 0) {
            val ac = ProjectSection.findAll (
                By(ProjectSection.project, project)) (0)
            psl = ac.id.is
        }
        val itcount = ProjectTreatment.countProjectTreatment (treatment, treatmentDetail);
        if (itcount < 1) {
            val projectTreatment = ProjectTreatment.createInCompany.project(project)
            .treatment(treatment)
            .treatmentDetail(treatmentDetail)
            .projectSection (psl)
            .obs("teste")
            projectTreatment.save
            projectTreatment.id.is
        } else {
            // este treatment/td já está em algum project, nesse ou outro
            0
        }
    }

  override def delete_! = {
    if ((!treatmentDetailOk.isEmpty)) {
      throw new RuntimeException("Não é permitido excluir item de orçamento quitado")
    }
    val result = super.delete_!
    result
  }

}

object ProjectTreatment extends ProjectTreatment with LongKeyedMapperPerCompany[ProjectTreatment] 
    with OnlyCurrentCompany[ProjectTreatment]  with OnlyActive[ProjectTreatment] {

   def makeAsReady (customer:Customer, tdId : Long) : String = {
    val td = TreatmentDetail.findByKey (tdId).get
    val aclist = ProjectTreatment.findAll (
        BySql(""" project in (select id from project where bp_sponsor = ? and status = 1)""", 
            IHaveValidatedThisSQL("",""), customer.id.is),
        BySql(""" (treatmentDetailOk is null or treatmentDetailOk = 0) and treatmentDetail in (select id from treatmentdetail where (product = ? or activity = ?))""", 
            IHaveValidatedThisSQL("",""), td.activity.is, td.activity.is)
        );
    if (aclist.length > 0) {
        if (AuthUtil.company.appType.isEsmile) {
            aclist.foreach((pt)=>{
                val td1 = pt.treatmentDetail.obj.get
                // qdo tem dente ou região da boca no orçamento
                // tem que ter tb no serviço feito
                // qdo não tem no orçamento, não importa se tem ou não no serviço
                if (td1.tooth != "") {
                    if (td1.tooth == td.tooth) {
                        pt.treatmentDetailOk(tdId).save
                    }
                } else {
                    pt.treatmentDetailOk(tdId).save
                }
            });
        } else {
            val ac = aclist (0)
            ac.treatmentDetailOk(tdId).save
        }
        ""
    } else {
        "Não foi possível marcar o item de orçamento como atendido"
    }
   }    
   def makeAsOpen (customer:Customer, tdId : Long) : String = {
    val td = TreatmentDetail.findByKey (tdId).get
    val aclist = ProjectTreatment.findAll (
        BySql(""" project in (select id from project where bp_sponsor = ? and status = 1)""", 
            IHaveValidatedThisSQL("",""), customer.id.is),
        BySql(""" ( treatmentDetailOk is not null and treatmentDetailOk = ? ) """, 
            IHaveValidatedThisSQL("",""), tdId)
        );
    if (aclist.length > 0) {
        val ac = aclist (0)
        ac.treatmentDetailOk(0).save
        ""
    } else {
        "Não foi possível marcar o item de orçamento como atendido"
    }
   }    
}

class ProjectSection extends Audited[ProjectSection] with KeyedMapper[Long, ProjectSection] with BaseLongKeyedMapper
     with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy
     with ActiveInactivable[ProjectSection] {
    def getSingleton = ProjectSection

    object project extends MappedLongForeignKey(this, Project1)
    object orderInReport extends MappedLong(this) {
        override def defaultValue = 10
    }
/* depois para cronograma
    object startAt extends EbMappedDateTime(this) {
        override def defaultValue = new Date()
    }
    object endAt extends EbMappedDateTime(this)
*/
    object title extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(this.is))
      }      
    }  
    object obs extends MappedString(this, 2000)

    def addSection(projectId:Long, orderInReport:Long, title:String, obs:String){
        ProjectSection
                .createInCompany
                .project(projectId)
                .orderInReport(orderInReport)
                .title(title)
                .obs(obs)
                .save

    }    
    override def delete_! = {
        if(ProjectTreatment.count(By(ProjectTreatment.projectSection,this.id)) > 0){
            throw new RuntimeException("Existe item ligado a esta seção " + this.title)
        }
        super.delete_!;
    }
}

object ProjectSection extends ProjectSection 
    with LongKeyedMapperPerCompany[ProjectSection] 
    with OnlyCurrentCompany[ProjectSection] 
    with OnlyActive[ProjectSection]{
}

