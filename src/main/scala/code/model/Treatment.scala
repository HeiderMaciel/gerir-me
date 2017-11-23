package code
package model

import net.liftweb.common.{Box,Full,Empty}
import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.service._
import code.actors._
import java.util.Date
import java.util.Calendar
import net.liftweb.json._
import net.liftweb.common._
import net.liftweb.util._
import http.js._
import JE._
//import org.joda.time.Days
//import org.joda.time.Hours
import org.joda.time.Minutes
import org.joda.time.DateTime

class Treatment extends UserEvent 
with LogicalDelete[Treatment] with PerCompany 
with PerUnit with IdPK with CreatedUpdated 
with OneToMany[Long, Treatment] with CreatedUpdatedBy 
with TreatmentStatus
with WithCustomer with net.liftweb.common.Logger{
    def getSingleton = Treatment
    override def maxConflictsAllowed = {
        if(!details.isEmpty)
            details.map(_.maxConflictsAllowed).sortWith(_ > _).head
        else
            1000
    }
    object resource extends MappedLongForeignKey(this.asInstanceOf[MapperType],User){
        //override def dbIndexed_? = true
    }
    object obs extends MappedPoliteString(this,555)//dont work 
    object payment extends MappedLongForeignKey(this,Payment)
    object treatmentConflit  extends MappedLongForeignKey(this,Treatment){
        override def defaultValue = 0
    }
    object detailTreatmentAsText extends MappedPoliteString(this,400) with LifecycleCallbacks {
            override def beforeSave() {
                if (createdBy != 1) {
                  // se foi migrado user = 1 - mantem o detailastext original
                  super.beforeSave;
                  this.set(fieldOwner.asInstanceOf[Treatment].descritionByDetails)
                }
            }         
        override def  defaultValue = descritionByDetails
    }
    object command extends MappedPoliteString(this,50){
        override def dbIndexed_? = true
    }
    object hasDetail extends MappedBoolean(this)
    object showInCalendar extends MappedBoolean(this){
        override def defaultValue = true
    }
    object status2 extends MappedInt(this)//,Treatment.TreatmentStatus)

//    object status extends MappedEnum(this,Treatment.TreatmentStatus) with LifecycleCallbacks {
    object status extends MappedInt(this) with LifecycleCallbacks {
        override def defaultValue = Treatment.Budget;
        override def afterSave() {
            super.afterSave;
            // criar parm na empresa para tirar o false
            if (this.get == Treatment.Ready && AuthUtil.company.bpmCommissionOnReady_?) {
                //case class PaymentRequst(treatments:List[TreatmentDTO],payments:List[PaymentDTO],command:String,dataTreatments:String,cashier:String){
                if (hasBpMonthly) {
                    PaymentService.processPaymentRequst(TreatmentService.generatePaymentFromTreatment(fieldOwner.asInstanceOf[Treatment], 0 /*bpmonthly*/));
                }
            } 
            if (this.get == Treatment.Ready && AuthUtil.company.offCommissionOnReady_?) {
                //case class PaymentRequst(treatments:List[TreatmentDTO],payments:List[PaymentDTO],command:String,dataTreatments:String,cashier:String){
                if (hasOffSale) {
                    PaymentService.processPaymentRequst(TreatmentService.generatePaymentFromTreatment(fieldOwner.asInstanceOf[Treatment], 1 /* offsale */));
                }
            }
            if (this.get == Treatment.Ready && AuthUtil.company.packCommissionOnReady_?) {
                //case class PaymentRequst(treatments:List[TreatmentDTO],payments:List[PaymentDTO],command:String,dataTreatments:String,cashier:String){
                if (hasPackage(details(0).activity.obj.get.id.is)) {
                    PaymentService.processPaymentRequst(TreatmentService.generatePaymentFromTreatment(fieldOwner.asInstanceOf[Treatment], 2 /*package*/));
                }
            }

            if (this.get == Treatment.Missed && AuthUtil.company.bpmCommissionOnMissed_?) {
                if (hasBpMonthly) {
                    PaymentService.processPaymentRequst(TreatmentService.generatePaymentFromTreatment(fieldOwner.asInstanceOf[Treatment], 0 /* bpmonthly*/ ));
                }
            }
            if (this.get == Treatment.Missed && AuthUtil.company.offCommissionOnMissed_?) {
                if (hasOffSale) {
                    PaymentService.processPaymentRequst(TreatmentService.generatePaymentFromTreatment(fieldOwner.asInstanceOf[Treatment], 1 /* offsale */ ));
                }
            }
            if (this.get == Treatment.Missed && AuthUtil.company.packCommissionOnMissed_?) {
                if (hasPackage(details(0).activity.obj.get.id.is)) {
                    PaymentService.processPaymentRequst(TreatmentService.generatePaymentFromTreatment(fieldOwner.asInstanceOf[Treatment], 2 /* package */ ));
                }
            }
        }         
    }
    object originDate extends EbMappedDate(this) with LifecycleCallbacks{
        override def beforeSave() {
          super.beforeSave;
          if (this.get == Empty || this.get == null) {
              this.set(start.is)
          }
        } 
    }

    def hasPackage (productId:Long)= DeliveryDetail.findByCustomer(this.customer.obj.get).filter(_.product == productId).size > 0

    def hasBpMonthly = details.filter(_.isAMonthlyService).size > 0

    def hasOffSale = details.filter(_.isAOffSaleService).size > 0

    def hasAccount = details.filter(_.isPreviousDebts).size > 0
    def accounts = details.filter(_.isPreviousDebts)

    def  details = TreatmentDetail.findAll(By(TreatmentDetail.treatment,this), OrderBy(TreatmentDetail.id, Ascending))
    object facebookEventId extends MappedPoliteString(this,255){
        override def defaultValue = ""
    }
    def customerId = customer.is
    //extends MappedOneToMany(TreatmentDetail, TreatmentDetail.treatment, OrderBy(TreatmentDetail.id, Ascending))
    //override def strDescription = this.customer.is+" - "+this.customerName+"<br/>"+this.descritionDetails
    override def strDescription = if (!AuthUtil.user.isCustomer) {
            this.customer.is+" - "+this.customerName+" "+this.descritionDetails
        }else{
            this.customer.is+" - "+this.customerName + " " + this.customerInfo +" "+this.descritionDetails
        }

    lazy val customerInfo:String = {
        customer.obj match {
            case Full(c)=> c.mobilePhone.is + " " + c.phone.is
            case _ => ""
        }
    }
    lazy val customerName:String = {
        customer.obj match {
            case Full(c)=> c.name.is
            case _ => ""
        }
    }
    lazy val customerPhones:String = {
        // pq vez por outra dava erro no rel T nas migraçãoes qd cliente era nulo ou invalido
        // rigel 02/07/2017
        customer.obj match {
            case Full(c)=> c.mobilePhone.is + ' ' + c.phone.is + ' ' + c.email_alternative.is + ' ' + c.email.is
            case _ => "Cliente/Paciente ===== inválido!"
        }
    }
    // rigel 28/07/2014
    lazy val customerShortName:String = {
        customer.obj match {
            case Full(c)=> c.short_name.is
            case _ => ""
        }
    }

    // rigel 30/09/2014
    def unitShortName:String = {
        unit.obj match {
            case Full(c)=> c.short_name.is
            case _ => ""
        }
    }

    def descritionDetails = {
        descritionByDetails//detailTreatmentAsText.is
    }

    def descritionByDetails = {
        details match { 
                case (dl) if(dl.size >0 ) => { 
                    val petName = if (AuthUtil.company.appType.isEbellepet) {
                            val pet = details(0).getTdEpet.animal.obj
                            if (!pet.isEmpty) {
                                pet.get.short_name + " - "
                            } else {
                                ""
                            }
                        } else {
                            ""
                        }
                    petName + (details map(_.nameActivity) reduceLeft(_+", "+_))
                }
                case _ => {
                    ""
                }
            }
    }    

    def whereIs = {
        tdWhereIS//detailTreatmentAsText.is
    }
    def tdWhereIS = {
        details match { 
                case (dl) if(dl.size >0 ) => { 
                    details map(_.whereIs) reduceLeft(_+" "+_)
                }
                case _ => {
                    ""
                }
            }
    }    

    lazy val paymentDescription = {
        payment.obj match {
                case Full(p) if (p.details.size > 0) => {
                    p.details.map((p:PaymentDetail)=> { p.typePaymentTranslated }).groupBy(_.toString()).toList.map(_._1).reduceLeft((current:String, all:String) => current+", "+all)
                }
                case _ => {
                    ""
                }
            }
    }    
    lazy val isConflitTreatment = this.treatmentConflit.is > 0
    def cashier =  payment.obj match {
                case Full(p) => {
                    p.cashier.obj match {
                        case Full(csh) => csh.idForCompany.is.toString
                        case _ => ""
                    }
                    
                }
                case _ => {
                    ""
                }
            }


    def hasProduct:Boolean = details match { 
                case (dl) if(dl.size >0 ) => { 
                    details.filter(_.isProduct).size >0
                }
            case _ => {
                false
            }
        }
    def hasService:Boolean = details match { 
                case (dl) if(dl.size >0 ) => { 
                    details.filter(_.isService).size >0
                }
            case _ => {
                false
            }
        }    

    def totalValue(invoicegroup:Long):BigDecimal = {
         details match { 
                case (dl) if(dl.size >0 ) => { 
                    if (invoicegroup != 0) {
                        var tot_aux = details.filter(_.isInvoiceGroup(invoicegroup))
                        if(!tot_aux.isEmpty){
                            tot_aux map( _.price.is) reduceLeft(_+_);
                        } else {
                            0.0
                        }
                    } else {
                        details map( _.price.is) reduceLeft(_+_)
                    }
                }
                case _ => {
                    0.0
                }
            }
        
    }

    def userName:String = {
        user.obj match {
//            case Full(u)=> u.id.is + ' ' + u.name.is
            case Full(u)=> u.name.is
            case _ => ""
        }
    }
    // rigel 28/07/2014
    def userShortName:String = {
        user.obj match {
            case Full(u)=> u.short_name.is
            case _ => ""
        }
    }

    def setStatusOpenTreatment(user:User,customer:Customer, 
        start:Date, status:Int) = {
        val treatments = Treatment.findAllInCompany(By(Treatment.dateEvent,start), 
            By(Treatment.customer, customer),
            NotBy (Treatment.status,Treatment.Deleted))
        if(treatments.size > 0){
            if (treatments(0).status == Treatment.Paid) {
              throw new RuntimeException("Não é permitido alterar atendimento pago!")
            } else {
               if (status == 3) {
                  if (treatments(0).status == Treatment.Ready) {
                      throw new RuntimeException("Atendimento já havia sido encerrado!")
                  } else {  
                     treatments(0).markAsReady
                     treatments(0).save
                  }
               } else {
                  throw new RuntimeException("Falta implementar set status " + status)
               }
            }
        }
    }

    def createOpenTreatment(user:User,customer:Customer, start:Date) : Treatment = {
        val treatments = Treatment.findAllInCompany(By(Treatment.start,start), By(Treatment.customer, customer), By(Treatment.user, user) )
        if(treatments.size > 0){
            treatments(0)
        } else {
            val treatment = Treatment.create
            treatment.customer(customer)
            treatment.user(user)
            treatment.status(Treatment.Open)
            treatment.start(start)
            treatment.end(getEndDate(start))
        }
    }

    def resetEndDate() = {
        start.is match {
            case st:Date if(st != null) =>{
                var miutesToAdd = details match { 
                    case (dl) if(dl.size >0 ) => { 
                        dl map(_.duration) map (_ split(":") map(_.toInt) reduceLeft(_*60+_)) reduceLeft(_+_)
                    }
                    case _ => 0
                }
                var cal = Calendar.getInstance
                cal.setTime(start)
                cal.add(Calendar.MINUTE, miutesToAdd)
                end(cal.getTime)                
            }
            case _ => {

            }
        }
        this
    }

    def getEndDate(start:Date):Date = {
        var cal = Calendar.getInstance
        cal.setTime(start)
        cal.add(Calendar.HOUR, 1)
        cal.getTime
    }

    //def paid_? = status.is == Treatment.Paid 

    def validatePaid = {
        if(this.isPaid){
            throw new RuntimeException("Não é permitido alterar atendimento pago!")
        }
        this
    }
    
    override def delete_! = {
        if(this.isPaid){
            throw new RuntimeException("Não é permitido excluir atendimento pago!")
        }
        this.status(Treatment.Deleted).
        status2(Treatment.Deleted).
        deleted_?(true).saveWithoutValidate
    }

    def treatmentstoInvoice (start:Date, end:Date) = 
    Treatment.findAllInCompany(NotBy(Treatment.id, 0),NotBy(Treatment.customer, 0), 
        By(Treatment.hasDetail, true), 
        BySql("dateevent between ? and ?",IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"),start,end)
        )

    def userObj:User = this.user.obj.get
    def treatmentsOfSameUser = Treatment.findAllInCompany(NotBy(Treatment.id, this.id),NotBy(Treatment.customer, this.customer), By(Treatment.hasDetail, true), By(Treatment.user,this.user), BySql("date(start_c) = date(?)",IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"),this.start))
    def treatmentsToday(idactivity:Long) = Treatment.findAllInCompany(By(Treatment.hasDetail, true), BySql("id in (select distinct t.id from treatment t inner join treatmentdetail td on( td.treatment = t.id) where activity =? and date(start_c) = date(?))",IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"), idactivity, this.start.is))
    def validateHours  {
        start.is match {
            case st:Date if(st != null) =>{
                validate(treatmentsOfSameUser,st)
            }
            case _ => {

            }
        }
        TreatmentValidations.validateSimultaneosService(this)
    }

    def validate(treatments:List[Treatment],st:Date) :Boolean = {
            val validationsList =
                (
                    // acrescentei o status <> pago pq nao justifica alertar
                    // conflito se o treatment já ta pago só se está em aberto
                    // tb atendido faltou ou remarcou
                    treatments
                    .filter((t:Treatment) =>
                        TreatmentValidations.validateNotEquals(t,this) && 
                        !t.isConflitTreatment &&
                        t.status.is != Treatment.Paid &&
                        t.status.is != Treatment.Ready &&
                        t.status.is != Treatment.Missed &&
                        t.status.is != Treatment.ReSchedule &&
                        t.status.is != Treatment.Budget
                    )
                    ::: BusyEvent.constraintDate(userObj,st, AuthUtil.unit)
                )
                .map((a:UserEvent) => {
                    TreatmentValidations.validateBetween(a,this) :: 
                    TreatmentValidations.validateContains(a,this) ::
                    TreatmentValidations.validateEndBetween(a,this) :: Nil
                });
            if(!validationsList.isEmpty){
                val validations:List[Validation] =  validationsList
                    .reduceLeft(_:::_)
                    .filter(_.isFull)
                    .groupBy(_.event)
                    .map(_._2.head)
                    .toList
                    ;
                    val count = validations.size
                    if(count > 0){
                        val maxAllowed = math.max(validations.map((v)=> { v.event.maxConflictsAllowed }).sortWith(_ > _).head, this.maxConflictsAllowed)
                        if(maxAllowed < count){
                            //info(maxAllowed+" < "+count)
                            throw new RuntimeException(validations(0).exceptionMessage)
                        }
                    }
            }
        TreatmentValidations.validateCommand(this)
        false
    }
    def deliveries = DeliveryControl.findAll(By(DeliveryControl.treatment, this))
    def removeDeliveries = {
        val deliveriesLocal = deliveries
        deliveriesLocal.foreach( _.details.foreach(_.delete_!) )
        deliveriesLocal.foreach( _.delete_! )
    }

    def saveWithoutValidate()={
        this.hasDetail(this.details.size > 0)
        if (AuthUtil.company.appType.isEdoctus) {
            if (this.id > 0) {
                getTreatEdoctus.save
            }
        }
        // o status era feito no save, mas tava dando erro qdo tinha 
        // conflito na agenda - por isso trouxe pra cá
        if (this.status == Treatment.Open ||
            this.status == Treatment.Missed ||
            this.status == Treatment.Arrived ||
            this.status == Treatment.Ready ||
            this.status == Treatment.Deleted ||
            this.status == Treatment.Confirmed ||
            this.status == Treatment.PreOpen ||
            this.status == Treatment.ReSchedule ||
            this.status == Treatment.Budget) {
            this.status2.set (this.status.is)
        } else if (
            this.status == Treatment.Paid &&
            this.status2 != Treatment.Missed &&
            this.status2 != Treatment.Ready) {
            this.status2.set (this.status.is)
        }
        if (this.status == Treatment.Paid) {
            // as vezes chega aqui sem empresa, deve ser feito na fila
            // por isso carrego e testo o co. pela company do treatment
            if (AuthUtil.company.id == this.company.is) {
                if (AuthUtil.company.packCommissionOnReady_? ||
                    AuthUtil.company.packCommissionOnMissed_? ||
                    AuthUtil.company.bpmCommissionOnReady_? ||
                    AuthUtil.company.bpmCommissionOnMissed_?.is ||
                    AuthUtil.company.offCommissionOnReady_? ||
                    AuthUtil.company.offCommissionOnMissed_?) {
                } else {
                    this.status2.set (this.status.is)   
                }
            } else {
                var co = Company.findByKey(this.company.is).get
                if (co.packCommissionOnReady_? ||
                    co.packCommissionOnMissed_? ||
                    co.bpmCommissionOnReady_? ||
                    co.bpmCommissionOnMissed_?.is ||
                    co.offCommissionOnReady_? ||
                    co.offCommissionOnMissed_?) {
                } else {
                    this.status2.set (this.status.is)   
                }
            }
        }
        super.save()
    }  
    def isOpen = {
        this.status.is == Treatment.Open;
    }
    def isPaid = {
        this.status.is == Treatment.Paid;
    }
    def isReady = {  
        this.status.is == Treatment.Ready;
    }
    def isMissed = {  
        this.status.is == Treatment.Missed;
    }
    def isReScheduled = {  
        this.status.is == Treatment.ReSchedule;
    }
    def isBudget = {  
        this.status.is == Treatment.Budget;
    }

    def validateCustomer {
        customer.obj match {
            case Full(c)=> {
                if (this.customerName != "") {

                    } else {
                        throw new RuntimeException(AuthUtil.company.appCustName("Cliente") + " inválido customerName!");
                    }
            }
            case _ => {
                LogActor ! "Treatment com customer ( %s ) inválido, company (%s)!".format(customer.is, AuthUtil.company.id.is);
                throw new RuntimeException(AuthUtil.company.appCustName("Cliente") + " inválido!");
            }
        }        
    }

    def insecureSave = {
      super.save
    }

    override def save()={
        if(showInCalendar.is && !isConflitTreatment 
            && !this.isPaid && !this.isReady && !this.isMissed 
            && !this.isReScheduled && !this.isBudget) {
            validateHours
        }
        validateCustomer

        if (AuthUtil.company.appType.isEdoctus) {
            if (this.id > 0) {
                getTreatEdoctus.save
            }
        }
/*      o tratamento do status teve que ir para o savewithoutvalidate

        if (this.status == Treatment.TreatmentStatus.Open ||
            this.status == Treatment.TreatmentStatus.Missed ||
            this.status == Treatment.TreatmentStatus.Arrived ||
            this.status == Treatment.TreatmentStatus.Ready ||
            this.status == Treatment.TreatmentStatus.Deleted ||
            this.status == Treatment.TreatmentStatus.Confirmed ||
            this.status == Treatment.TreatmentStatus.PreOpen ||
            this.status == Treatment.TreatmentStatus.ReSchedule) {
            this.status2.set (this.status.is)
        } else if (
            this.status == Treatment.TreatmentStatus.Paid &&
            this.status2 != Treatment.TreatmentStatus.Missed &&
            this.status2 != Treatment.TreatmentStatus.Ready) {
            this.status2.set (this.status.is)
        }
        if (this.status == Treatment.TreatmentStatus.Paid) {
            // as vezes chega aqui sem empresa, deve ser feito na fila
            // por isso carrego e testo o co. pela company do treatment
            if (AuthUtil.company.id == this.company.is) {
                if (AuthUtil.company.packCommissionOnReady_? ||
                    AuthUtil.company.packCommissionOnMissed_? ||
                    AuthUtil.company.bpmCommissionOnReady_? ||
                    AuthUtil.company.bpmCommissionOnMissed_?.is ||
                    AuthUtil.company.offCommissionOnReady_? ||
                    AuthUtil.company.offCommissionOnMissed_?) {
                } else {
                    this.status2.set (this.status.is)   
                }
            } else {
                var co = Company.findByKey(this.company.is).get
                if (co.packCommissionOnReady_? ||
                    co.packCommissionOnMissed_? ||
                    co.bpmCommissionOnReady_? ||
                    co.bpmCommissionOnMissed_?.is ||
                    co.offCommissionOnReady_? ||
                    co.offCommissionOnMissed_?) {
                } else {
                    this.status2.set (this.status.is)   
                }
            }
        }
*/
        saveWithoutValidate
    }

    def findAllSameTreatments = {
        Treatment.findAllInCompany(By(Treatment.hasDetail,true), By(Treatment.customer,this.customer),BySql("dateevent = date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),this.start));
    }

    def markAsArrived {
        var minutes = 0;
        this.status(Treatment.Arrived)
        if (AuthUtil.company.appType.isEdoctus) {
            if (this.id > 0) {
                val today = Project.date_format_js.parse(Project.date_format_js.format(new Date()));
                val ted = getTreatEdoctus
        val   firstdate  = new DateTime(today)
        // o formato date time precisa de um T antes de HH:mi
        val   seconddate = new DateTime(Project.dateToStrJs(this.start).replaceAll (" ","T"))
        val   factor = Minutes.minutesBetween(firstdate, seconddate)
        minutes = factor.toString.replaceAll ("PT","").replaceAll("M","").toInt
        val obsAux = if (minutes > 15) {
            "Antecipado " + minutes + " minutos"
        } else if (minutes < 0) {
            "Atrasado " + (minutes * -1) + " minutos"
        } else {
            ""
        }
                ted.obsLate (obsAux)
                ted.arrivedAt (today)
                ted.save
            }
        }
    }

    def markAsMissed {
        this.status(Treatment.Missed)
        // falta criar o parm
        // zeroPrices
    }

    def markAsReady {
        this.status(Treatment.Ready)
    }

    def markAsOpen {
        this.status(Treatment.Open)
    }    
     
    def markAsPreOpen {
        this.status(Treatment.PreOpen)
    }    

    def markAsReSchedule {
        this.status(Treatment.ReSchedule)
    }    

    def markAsConfirmed {
        this.status(Treatment.Confirmed)
    }

    def markAsBudget {
        this.status(Treatment.Budget)
    }    


    def sendToFacebook {
        var customer = this.customer.obj.get
        if(customer.facebookId.is != "" && customer.facebookId.is != null && this.facebookEventId.is == ""){
            val eventId = FacebookUtil.createEvent(customer.facebookAccessToken,customer.facebookId," Agendamento no %s".format(this.company.obj.get.name.is),this.start,this.end,this.descritionDetails).id
            this.facebookEventId(eventId).save
        }
    }

    // para o caso de parm que manda zerar o preço na falta na remarcacao
    // falta criar o parm 
    def zeroPrices = {
        this.details.foreach((td)=>{
            td.price(0).save
        })
    }

    def revertPrices = {
        this.details.foreach((td)=>{
            td.revertPrice
        })
    }

    def unitsToShowSql = if (AuthUtil.user.isAdmin) {
      " 1 = 1 "
    } else {
      " (tr.unit = %s or (tr.unit in (select uu.unit from usercompanyunit uu where uu.user_c = %s and uu.company = %s))) ".format(AuthUtil.user.unit, AuthUtil.user.id, AuthUtil.user.company)
    }

    val SQL_VALID_TREATMENT = " and hasDetail=true and t.status<>5 and dateevent between date(?) and date(?) ORDER BY u.name, start_c"

    def location = company.obj.get.name.is+" - Unidade : "+unit.obj.get.name
    def toIcs(title:String = "Atendimento ") = IcsFileUtil.buildIcsStr(
            this.id.is.toString,
            this.start.is, 
            this.end.is, 
            this.company.obj.get.email.is,
            title,
            location, 
            this.detailTreatmentAsText.is
        ).getBytes("UTF-8")
    def toJson =JsObj()

    lazy val getTreatEdoctus: TreatEdoctus = {
        if (TreatEdoctus.count (By (TreatEdoctus.treatment,this.id)) > 0) {
            TreatEdoctus.findAllInCompany (By (TreatEdoctus.treatment,this.id))(0)
        } else {
            val treatEdoctus = TreatEdoctus.createInCompany.treatment(this.id)
            treatEdoctus
        }
    }

    def toXmlTiss (treatment:Long) = {
       val ted = getTreatEdoctus
       var os = OffSale.findByKey (ted.offsale).get;
       var typeHosp = ted.hospitalizationType;
       val tag = if (typeHosp == "") {
            "<ans:guiaSP-SADT>"
       } else {
            "<ans:guiaResumoInternacao>"
       }
       val tagClose = if (typeHosp == "") {
            "</ans:guiaSP-SADT>"
       } else {
            "</ans:guiaResumoInternacao>"
       }
       val tagNumber = if (typeHosp == "") {
            ""
       } else {
            "<ans:numeroGuiaSolicitacaoInternacao>" + this.command + "</ans:numeroGuiaSolicitacaoInternacao>"
       }
       val tagInternacao = if (typeHosp == "") {
            """<ans:dadosSolicitacao>
                <ans:dataSolicitacao>""" + Project.dateToDb(this.dateEvent) + """</ans:dataSolicitacao>
                <ans:caraterAtendimento>""" + ted.attendanceType + """</ans:caraterAtendimento>
            </ans:dadosSolicitacao>
            """ +
            """<ans:dadosAtendimento>
                                    <ans:tipoAtendimento>01</ans:tipoAtendimento>
                                    <ans:indicacaoAcidente>""" + ted.accidentIndicator + """</ans:indicacaoAcidente>
                                    <ans:tipoConsulta>1</ans:tipoConsulta>
                                    <ans:motivoEncerramento>""" + ted.closingCause + """</ans:motivoEncerramento>
                                </ans:dadosAtendimento>"""
       } else {
            """
            <ans:dadosInternacao>
                <ans:caraterAtendimento>""" + ted.attendanceType + """</ans:caraterAtendimento>
                <ans:tipoFaturamento>4</ans:tipoFaturamento>
                <ans:dataInicioFaturamento>""" + Project.dateToDb(this.dateEvent) + """</ans:dataInicioFaturamento>
                <ans:horaInicioFaturamento>""" + Project.dateToHourss(this.start) + """</ans:horaInicioFaturamento>
                <ans:dataFinalFaturamento>""" + Project.dateToDb(this.dateEvent) + """</ans:dataFinalFaturamento>
                <ans:horaFinalFaturamento>""" + Project.dateToHourss(this.end) + """</ans:horaFinalFaturamento>
                <ans:tipoInternacao>""" + ted.hospitalizationType + """</ans:tipoInternacao>
                <ans:regimeInternacao>""" + ted.hospitalizationRegime + """</ans:regimeInternacao>
            </ans:dadosInternacao>
            <ans:dadosSaidaInternacao>
               <ans:diagnostico>c50</ans:diagnostico>
               <ans:indicadorAcidente>""" + ted.accidentIndicator + """</ans:indicadorAcidente>
               <ans:motivoEncerramento>""" + ted.closingCause + """</ans:motivoEncerramento>
            </ans:dadosSaidaInternacao>       
            """
       }


       var strXml:String = tag + """
                            <ans:cabecalhoGuia>
                                <ans:registroANS>"""+ os.document_ans.is+"""</ans:registroANS>
                                <ans:numeroGuiaPrestador>""" + this.command + """</ans:numeroGuiaPrestador>
                                <ans:guiaPrincipal>""" + this.command + """</ans:guiaPrincipal>
                            </ans:cabecalhoGuia> """ + tagNumber + """
                            <ans:dadosAutorizacao>
                                <ans:numeroGuiaOperadora>""" + this.command + """</ans:numeroGuiaOperadora>
                                <ans:dataAutorizacao>""" + Project.dateToDb(this.dateEvent) + """</ans:dataAutorizacao>
                                <ans:senha>""" + this.command + """</ans:senha>
                                <ans:dataValidadeSenha>""" + Project.dateToDb(this.dateEvent) + """</ans:dataValidadeSenha>
                            </ans:dadosAutorizacao>
                            """ + this.customer.obj.get.toXmlTiss (1) + """   """ +
                            this.user.obj.get.toXmlTissSolicitante (getTreatEdoctus.offsale) + """   """ +
                            // antes aqui vinha o bloco da solicitacao
                            this.user.obj.get.toXmlTissExecutante (1) + """    
                            """ + tagInternacao  + 
                            """
                              <ans:procedimentosExecutados>
                            """
             
       TreatmentDetail.findAllInCompany(By(TreatmentDetail.treatment,this.id))
       .foreach ((td) => {
       println ("========= treatmentdetail " + td.id + " " + td.price + " " + td.nameActivity)
            //var td = Treatment.findByKey(it.treatment).get
            //info (tr.toXmlTiss(it.treatment).toString)
            strXml += td.toXmlTiss
       })
       strXml += """
</ans:procedimentosExecutados>""" + 
/*
    <ans:outrasDespesas>
        <ans:despesa>
            <ans:codigoDespesa>01</ans:codigoDespesa>
            <ans:servicosExecutados>
                <ans:dataExecucao>""" + Project.dateToDb(this.dateEvent) + """</ans:dataExecucao>
                <ans:horaInicial>08:00:00</ans:horaInicial>
                <ans:horaFinal>18:00:00</ans:horaFinal>
                <ans:codigoTabela>18</ans:codigoTabela>
                <ans:codigoProcedimento>99999999</ans:codigoProcedimento>
                <ans:quantidadeExecutada>1</ans:quantidadeExecutada>
                <ans:unidadeMedida>001</ans:unidadeMedida>
                <ans:reducaoAcrescimo>0</ans:reducaoAcrescimo>
                <ans:valorUnitario>0.00</ans:valorUnitario>
                <ans:valorTotal>0.00</ans:valorTotal>
                <ans:descricaoProcedimento>XXXXXXXXXXXXX XXXXXXXXXXXXXXX</ans:descricaoProcedimento>
                <ans:registroANVISA>250</ans:registroANVISA>
                <ans:codigoRefFabricante>1133</ans:codigoRefFabricante>
                <ans:autorizacaoFuncionamento>X</ans:autorizacaoFuncionamento>
            </ans:servicosExecutados>
        </ans:despesa>
    </ans:outrasDespesas>
*/
    """ <ans:observacao>X</ans:observacao>
    <ans:valorTotal> """ + InvoiceService.totalValues (this) + """
        <ans:valorTotalGeral>""" + this.totalValue(0) + """</ans:valorTotalGeral>
    </ans:valorTotal>
    """ + tagClose

        if (this.totalValue(0) != InvoiceService.totalValuesVal (this)) {
            throw new RuntimeException("Valor total " + this.totalValue(0) + " diferente da soma de subtotais " +
                InvoiceService.totalValuesVal (this))
        }
        strXml
    }

}
/*
        <ans:valorProcedimentos>5</ans:valorProcedimentos>
        <ans:valorDiarias>0.00</ans:valorDiarias>
        <ans:valorTaxasAlugueis>0.00</ans:valorTaxasAlugueis>
        <ans:valorMateriais>0.00</ans:valorMateriais>
        <ans:valorMedicamentos>0.00</ans:valorMedicamentos>
        <ans:valorOPME>0.00</ans:valorOPME>
        <ans:valorGasesMedicinais>0.00</ans:valorGasesMedicinais>
*/

object Treatment extends Treatment with LongKeyedMapperPerCompany[Treatment] with OnlyCurrentCompany[Treatment]{
    def nextCommandNumber(date:Date,company:Company, unit:CompanyUnit):Int = {
        try{
            //Status 5 = deleted...
            val r = if (company.commandControl == Company.CmdDaily) {
                DB.performQuery("""select max(CAST(nullif(command,'') AS integer)) 
                from treatment where company = ? and unit = ? 
                and dateevent = date(?) and hasDetail=true and status<>5""", 
                List(company.id.is, unit.id.is, date))
            } else if (company.commandControl == Company.CmdEver) {
                // sem o dia - otimizar depois
                DB.performQuery("""select max(CAST(nullif(command,'') AS integer)) 
                from treatment where company = ? and unit = ? 
                and hasDetail=true and status<>5""", 
                List(company.id.is, unit.id.is))
            } else if (company.commandControl == Company.CmdDailyCompany) {
                DB.performQuery("""select max(CAST(nullif(command,'') AS integer)) 
                from treatment where company = ? 
                and dateevent = date(?) and hasDetail=true and status<>5""", 
                List(company.id.is, date))
            } else if (company.commandControl == Company.CmdEverCompany) {
                // sem o dia - otimizar depois
                DB.performQuery("""select max(CAST(nullif(command,'') AS integer)) 
                from treatment where company = ? 
                and hasDetail=true and status<>5""", 
                List(company.id.is))
            } else {
                throw new RuntimeException("Gerando comanda - Ctrl Inválido " + company.commandControl)
            }
            r._2(0)(0) match {
               case a:Any => a.toString.toInt+1
               case _ => 1
            }
        }catch{
            case e:Exception =>{
                try{
                    //Status 5 = deleted...
                    // se der erro faz sem o cast
                    val r = if (company.commandControl == Company.CmdDaily) {
                        DB.performQuery("""select max(command) 
                            from treatment where company = ? and unit = ?
                            and dateevent = date(?) 
                            and hasDetail=true and status<>5 
                            and command is not null and command <> ''""", 
                            List(company.id.is, unit.id.is, date))
                    } else if (company.commandControl == Company.CmdEver) {
                        // sem o dia - otimizar depois
                        DB.performQuery("""select max(command) 
                            from treatment where company = ? and unit = ?
                            and hasDetail=true and status<>5 
                            and command is not null and command <> ''""", 
                            List(company.id.is, unit.id.is))
                    } else if (company.commandControl == Company.CmdDailyCompany) {
                        DB.performQuery("""select max(command) 
                            from treatment where company = ? 
                            and dateevent = date(?) 
                            and hasDetail=true and status<>5 
                            and command is not null and command <> ''""", 
                            List(company.id.is, date))
                    } else if (company.commandControl == Company.CmdEverCompany) {
                        // sem o dia - otimizar depois
                        DB.performQuery("""select max(command) 
                            from treatment where company = ? 
                            and hasDetail=true and status<>5 
                            and command is not null and command <> ''""", 
                            List(company.id.is))
                    } else {
                        throw new RuntimeException("Gerando comanda sem o cast - Ctrl Inválido " + company.commandControl)
                    }
                    r._2(0)(0) match {
                       case a:Any => {
                            println ("============== comanda resolvida sem o cast integer company " + company.id.is + " comanda " + (a.toString.toInt+1))
                            a.toString.toInt+1
                        }
                       case _ => 1
                    }
                }catch{
                    case e:Exception =>{
                        //error(e)
                        return 1
                    }
                }
            }
        }
    }

    def findAllInCompanyWithDeleteds(params: QueryParam[Treatment]*) = {
        super.findAllInCompany(params.toList :_*)
    }
    override def findAllInCompany(params: QueryParam[Treatment]*): List[Treatment] = {
        val status = ByList(Treatment.status, List(TreatmentStatus.Open, TreatmentStatus.PreOpen, 
            TreatmentStatus.Missed, TreatmentStatus.ReSchedule, 
            TreatmentStatus.Arrived, TreatmentStatus.Ready, 
            TreatmentStatus.Paid, TreatmentStatus.Confirmed)) // sem o budget por enquanto
        super.findAllInCompany( status :: params.toList :_*)
    }    
}


object TreatmentValidations extends net.liftweb.common.Logger {
    def validateCommand(treatment:Treatment) = {
        if(!treatment.company.obj.get.allowRepeatCommand_?.is && treatment.command != "" &&  treatment.command != "0"){
            val treatmentsWithThisCommand = Treatment.findAll(
                            By(Treatment.company,treatment.company),
                            By(Treatment.command,treatment.command),
                            NotBy(Treatment.customer,treatment.customer),
                            By(Treatment.dateEvent, treatment.dateEvent)
                        )
            if(treatmentsWithThisCommand.size > 0){
                throw new RuntimeException("O atendimento %s já contém essa comanda, por favor utilize outra!".format(treatmentsWithThisCommand(0).strDescription, treatment.strDescription))
            }
        }
        EmptyValidation
    }

    def validateNotEquals(a:Treatment, b:Treatment) = b.id.is != a.id.is

    def validateBetween(a:UserEvent, b:UserEvent):Validation = 
    if(a.start.is.getTime <= b.start.is.getTime && 
        a.end.is.getTime > b.start.is.getTime 
        //&& a.status != Treatment.paid
        ){
        // (c) centro só pra diferenciar uma da outra
        ValidationError("Atendimentos conflitantes (c)\n%s\n\n%s".format(b.strDescription, a.strDescription), a)
    }else{
        EmptyValidation
    }

    def validateContains(a:UserEvent,b:Treatment):Validation = validateBetween(b,a) match {
        case e:ValidationError => ValidationError(e.message, a)
        case EmptyValidation => EmptyValidation
    }
    

    def validateEndBetween(a:UserEvent,b:Treatment):Validation = if(a.start.is.getTime > b.end.is.getTime && a.end.is.getTime < b.end.is.getTime){
        // (f) fim só pra diferenciar uma da outra
        ValidationError("Atendimentos conflitantes (f)\n%s\n\n%s".format(b.strDescription, a.strDescription), a)
    }else{
        EmptyValidation
    }

    def validateSimultaneosService(b:Treatment):Validation = {
        b.details.filter(_.activity.obj match {
            case Full(a) => a.allowSimultaneos_?.is
            case _ => false
        }).foreach((s)=>{
            try{
                val treatments = b.treatmentsToday(s.id.is)
                b.validate(b.treatmentsToday(s.activity.is),b.start.is)
            }catch{
                case e:Exception => throw new RuntimeException("%s não aceita agendamento simultâneo!".format(s.activity.obj.get.name.is)) 
            }    
        })
        EmptyValidation
    }
}
abstract class Validation{
    def isEmpty:Boolean
    def isFull = ! isEmpty
    def event:UserEvent
    def exceptionMessage:String
}
object EmptyValidation extends Validation{
    def isEmpty = true
    def event = Treatment.create
    def exceptionMessage = ""
}
case class ValidationError(message:String, userEvent:UserEvent) extends Validation{
    def isEmpty = false
    def event = userEvent
    def exceptionMessage = message
}

