package code
package service

import net.liftweb._
import mapper._ 
import code.util._
import code.daily._
import code.actors._
import code.comet._
import model._
import net.liftweb.util._
import http.js._
import JE._
import net.liftweb.common._
import java.util.Date
object  TreatmentService extends net.liftweb.common.Logger {

	def updateCalendar(message:String, t:Treatment) {
		TratmentServer ! TreatmentMessage(message, t.start)
	}
	def sendTreatmentsEmailCustomer(id:Long) = {
		val treatment = Treatment.findByKey(id).get
		val customer = treatment.customer.obj.get
		val company = treatment.company.obj.get
		val companyEmail = company.email.is
        val mail = code.daily.DailyReport.treatmentsTodayCustomer(customer,company, treatment.dateEvent)
        val title = if (AuthUtil.company.appType.isEgrex) {
        	"Atendimento Membro "+company.name.is
        	}else if (AuthUtil.company.appType.isEphysio) {
        	"Atendimento Paciente "+company.name.is
        	}else if (AuthUtil.company.appType.isEdoctus) {
        	"Atendimento Paciente "+company.name.is
        	}else{
        	"Atendimento Cliente "+company.name.is
        	}
        val location = company.name.is+" - Unidade"+treatment.unit.obj.get.name;
/*
		EmailUtil.sendMailTo(
			customer.email.is.toString, mail, title, company,
			FullAttachment(
							"atendimento.ics",
							"text/calendar", 
							treatment.toIcs(title)
			)
		)
*/
        var cunit = if (!customer.unit.isEmpty) {
                CompanyUnit.findByKey(customer.unit).get
            } else {
                // unidade vilarika
                CompanyUnit.findByKey(7).get
            }
        if (customer.email.is.toString == "") {
		    throw new RuntimeException(customer.name.is + " não possui email cadastrado.")
        }

		EmailUtil.sendMailCustomer(cunit,
			Company.findByKey (customer.company).get, 
			customer.email.is.toString,
			mail, 
			title, 
			customer.id.is,
			FullAttachment(
							"atendimento.ics",
							"text/calendar", 
							treatment.toIcs(title)
			)
		)
	}
	def sendTreatmentsEmailUser(id:Long) = {
		val treatment = Treatment.findByKey(id).get
		val user = treatment.user.obj.get
		val company = treatment.company.obj.get
		val companyEmail = company.email.is
		val title = if (!AuthUtil.company.appType.isEgrex) {
			"Agenda Profissional "+company.name.is
			}else{
			"Agenda Líder "+company.name.is
			}
		val location = company.name.is+" - Unidade"+treatment.unit.obj.get.name;
        val mail = code.daily.DailyReport.treatmentsTodayUser(user,company, treatment.dateEvent)
/*
		EmailUtil.sendMailTo(
			user.email.is.toString, mail, title, company,
			FullAttachment(
							"atendimento.ics",
							"text/calendar", 
							treatment.toIcs(title)
			)		
		)
*/
        var uunit = if (!user.unit.isEmpty) {
                CompanyUnit.findByKey(user.unit).get
            } else {
                // unidade vilarika
                CompanyUnit.findByKey(7).get
            }
        if (user.email.is.toString == "") {
		    throw new RuntimeException(user.name.is + " não possui email cadastrado.")
        }
		EmailUtil.sendMailCustomer(uunit,
			Company.findByKey (user.company).get, 
			user.email.is.toString,
			mail,
			title,
			user.id.is,
			FullAttachment(
							"atendimento.ics",
							"text/calendar", 
							treatment.toIcs(title)
			)		
		)
	}
	def revertPrices(id:Long) = {
		Treatment.findByKey(id).get.revertPrices
	}
/* - acho que tinha duplicado sem necessidade
	def markAsPreOpen(id:Long) = {
		Treatment.findByKey(id).get.markAsPreOpen
	}
*/
	def markAsArrived(id:Long) = {
		Treatment.findByKey(id).get.markAsArrived
	}
	def markAsConfirmed(id:Long) = {
		Treatment.findByKey(id).get.markAsConfirmed
	}
	def loadTreatmentByUser(user:User,date:Date,company:Company):List[Treatment] = {
		Treatment.findAll(By(Treatment.user,user.id.is),
						  By(Treatment.company,company),
						  By(Treatment.hasDetail,true),
						  // -- deletado desmarcou faltou
						  BySql(" status not in (5,8,1) ",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00")),
						  BySql("dateevent = date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),date),
						  OrderBy(Treatment.start,Ascending)
						 )
	}	
	def loadTreatmentByCustomer(customer:Customer,date:Date,company:Company):List[Treatment] = {
		Treatment.findAll(By(Treatment.customer,customer.id.is),
						  By(Treatment.company,company),
						  By(Treatment.hasDetail,true),
						  // -- deletado desmarcou faltou
						  BySql(" status not in (5,8,1) ",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00")),
						  BySql("dateevent = date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),date),
						  OrderBy(Treatment.start,Ascending)
						 )
	}	
	def loadTreatmentByCustomerNotPaid(customer:Customer,date:Date) = {
		loadTreatmentByCustomer(customer,date).filter(_.status != Treatment.Paid)
	}

	def loadTreatmentByCustomer(customer:Customer,date:Date):List[Treatment] = {
		Treatment.findAllInCompany(By(Treatment.customer,customer.id.is),
						  By(Treatment.hasDetail,true),
						  BySql("dateevent = date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),date)
						 )
	}

/* ninguem chamava
	def loadTreatmentsByCustomer(customer:Customer,start:Date, end:Date):List[Treatment] = {
		Treatment.findAll(By(Treatment.customer,customer.id.is),
						  By(Treatment.hasDetail,true),
						  BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), start, end)
						 )
	}	
*/

	def loadTreatmentByCommandOrCustomer(command:String,customer:Long,dateIni:Date,date:Date,unit:Long):List[Treatment] = {
		val statusSql = if (date != dateIni) {
			" status not in (5,8,1) "
		} else {
			" status not in (5) "
		}
		if (command == "0") {
			// vai pelo customer
			Treatment.findAllInCompany(By(Treatment.customer,customer),
			  By(Treatment.unit,unit),
			  By(Treatment.hasDetail,true),
			  BySql(statusSql,IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00")),
			  BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),dateIni, date),
			  OrderBy(Treatment.id, Ascending)
			 )
		} else if (customer != 0) {
			// novo - 04/05/2017 vai pelos 2 comanda e customer
			Treatment.findAllInCompany(By(Treatment.customer,customer),
			  By(Treatment.command,command), 
			  By(Treatment.unit,unit),
			  By(Treatment.hasDetail,true),
			  BySql(statusSql,IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00")),
			  BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),dateIni, date),
			  OrderBy(Treatment.id, Ascending)
			 )
		} else {
			// vai pela comanda
			Treatment.findAllInCompany(By(Treatment.command,command),
			  By(Treatment.unit,unit),
			  By(Treatment.hasDetail,true),
			  BySql(statusSql,IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00")),
			  BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),dateIni, date),
			  OrderBy(Treatment.id, Ascending)
			 )
		}
	}

	def factoryTreatment(id:String,customerCode:String,userCode:String,date:String,hour_start:String, hour_end:String,command:String, obs:String="", conflit:String="", force:Boolean = false):Box[Treatment] = {
		lazy val startDate = Project.strToDate(date+" "+hour_start)
		lazy val endDate = Project.strToDate(date+" "+hour_end)
		lazy val customer = loadCustomer(customerCode)
		lazy val conflitLong = if(conflit != "") conflit.toLong else 0

		if ((AuthUtil.user.id.is != userCode.toLong) && 
			AuthUtil.user.isSimpleUserCalendarView) {
  	        throw new RuntimeException("Você não tem permissão para inserir atendimentos para outros profissionais!")
		}

		def commandInt:String = if(command.isEmpty() || command == "0"){
				val treatments = loadTreatmentByCustomerNotPaid(customer,startDate)
				if(treatments.size >0){
					treatments(0).command.is
				}else{
					//if(AuthUtil.company.autoIncrementCommand_?.is){
					if(AuthUtil.company.commandControl.is == Company.CmdDaily ||
						AuthUtil.company.commandControl.is == Company.CmdDailyCompany){
						Treatment.nextCommandNumber(startDate,AuthUtil.company, AuthUtil.unit).toString
					}else{
						// never - user vai por a comanda
						// ever - o sistema poe q comamnda ao clicar em pagar na agenda
						"0"
					}
				}
			}else{
				command
			}
		id match {
			case s:String  if s != "0" && s != "" => {
				val ret = Treatment.findByKey(id.toLong)
				ret match {
					case Full(t) => {
						t.customer(customerCode.toLong).start(startDate).obs(obs).end(endDate)
						if(force) {
							t.saveWithoutValidate
						}else {
							t.save
						}
						TratmentServer ! TreatmentMessage("updateTreatment", endDate)
					}
					case _ =>
				}
				ret
			}
			case _ => {
				var treatment = Treatment.createOpenTreatment(loadUser(userCode), customer,startDate)
				treatment
						.end(endDate)
						.company(AuthUtil.company)
						.unit(AuthUtil.unit)
						.command(commandInt)
						.treatmentConflit(conflitLong)
						.obs(obs)
				if(force) {
					treatment.saveWithoutValidate
				}else {
					treatment.save
				}
				TratmentServer !  TreatmentMessage("SaveNewTreatment",endDate)
				Full(treatment)
				
			}
		}
	}

	def updateTreatmentHours(id:String,user:Long,start:Date,end:Date, 
		status:Int = 0, validate:Boolean = true) = {
		var treatment = Treatment.findByKey(id.toLong).get
		if (treatment.status.is == Treatment.Paid) {
			// evita que atendimento pago seja alterado gerava erro de comissao se o atend
			// fosse moviedo para outro usuario
			TratmentServer ! TreatmentMessage("SaveUpdateTratment",end)		
  	        throw new RuntimeException("Atendimento já foi pago, não pode ser alterado!")
		} 
		if ((AuthUtil.user.id.is != user) && 
			AuthUtil.user.isSimpleUserCalendarView) {
			TratmentServer ! TreatmentMessage("SaveUpdateTratment",end)		
  	        throw new RuntimeException("Você não tem permissão para alterar atendimentos de outros profissionais!")
		}

		treatment.user(user)
		treatment.start(start)
		// Rigel 26/07/2017
		// este techo marreta um bug go jweekcalendar provocado pela 
		// migração do bootstrap que alterava o fim para 1 minuto a menos
		// qdo o evento era arrastado ou redimensionado
		// assumimos que as divisões da agenda são em 0 ou 5 minutos
		// e arredondamos a hora
		// por enquanto na 1 e na 8 só para teste da agenda pq esta alteracao
		// vai ro master em produção
		// 07/08/2017 cometado - rigel
		// if (AuthUtil.company.id == 1 || AuthUtil.company.id == 8 ) {
		if (true) {
			//println ("vaiiii ============== " + Project.dateToHours(treatment.end) + " ==== " + Project.dateToHours(end)
			//	+ " ==== " + Project.dateToHours(end).slice(4,5) + " <<<<< " + (10-Project.dateToHours(end).slice(4,5).toInt));
			var minute = Project.dateToHours(end).slice(4,5);
			var minutes = Project.dateToHours(end).slice(3,5).toInt;
			var hours = Project.dateToHours(end).slice(0,2).toInt;
			if (minute != "0" &&
				minute != "5" &&
				minutes != Project.dateToHours(treatment.end).slice(3,5).toInt) {
				// deixa como está
				var dif = if ((10-Project.dateToHours(end).slice(4,5).toInt) > 5) {
					(10-Project.dateToHours(end).slice(4,5).toInt) - 5;
				} else {
					(10-Project.dateToHours(end).slice(4,5).toInt)
				}
				var newEnd = "";
				println ("vaiiii dentro ============== " + Project.dateToStr (end) + " " + Project.dateToHours(end));
				if (minutes + dif == 60) {
					if ((hours + 1) < 10) {
			    		println ("vaiiii era 60 dentro ============== " + Project.dateToStr (end) + " " + "0" + (hours + 1).toString + ":00");
			    		newEnd = Project.dateToStr (end) + " " + "0" + (hours + 1).toString + ":00";
					} else {
			    		println ("vaiiii era 60 dentro ============== " + Project.dateToStr (end) + " " + (hours + 1).toString + ":00");
			    		newEnd = Project.dateToStr (end) + " " + (hours + 1).toString + ":00";
					}
				} else {
					println ("vaiiii dentro ============== " + Project.dateToStr (end) + " " + Project.dateToHours(end).slice(0,3)+	(minutes + dif).toString);
					newEnd = Project.dateToStr (end) + " " + Project.dateToHours(end).slice(0,3)+	(minutes + dif).toString;
				}
	//			var end1 = Project.
				treatment.end (Project.strToDate(newEnd));
			} else {
				treatment.end(end);
			}		
		} else {
			// isso já era feito sempre
			treatment.end(end)
		}
		if(status == Treatment.Arrived){
			treatment.markAsArrived
		}else if(status == Treatment.Missed){
			treatment.markAsMissed
		}else if(status == Treatment.ReSchedule){
			treatment.markAsReSchedule
		}else if(status == Treatment.Confirmed){
			treatment.markAsConfirmed
		}else if(status == Treatment.Ready){
			treatment.markAsReady
		}else if(status == Treatment.PreOpen){
			treatment.markAsPreOpen
		}else if(status == Treatment.Open){
			treatment.markAsOpen
		}else if(status == Treatment.Budget){
			treatment.markAsBudget
		}
		if(validate){
			treatment.save
		}else{
			treatment.saveWithoutValidate
		}
		
		TratmentServer ! TreatmentMessage("SaveUpdateTratment",end)		
	}

	def updateEventHours(id:String,user:Long,start:Date,end:Date) = {
		var busyEvent = BusyEvent.findByKey(id.toLong).get
		busyEvent.user(user)
		busyEvent.start(start)
		busyEvent.end(end)
		busyEvent.save
		TratmentServer ! TreatmentMessage("SaveUpdateTratment",end)		
	}	

	def addDetailTreatment(id:Long,activityCode:Long, auxiliar:Long, animal:Long, tooth: String, offsale:Long):Box[TreatmentDetail] = {
		val activity = Activity.findByKey(activityCode).get
		var tempd = addDetailTreatment(id, activity, auxiliar, animal, tooth, offsale, true)
		tempd
	}
	def addDetailTreatmentWithoutValidate(id:Long,activityCode:Long, auxiliar:Long, animal:Long, tooth: String, offsale:Long):Box[TreatmentDetail] = {
		val activity = Activity.findByKey(activityCode).get
		var tempd = addDetailTreatment(id,activity, auxiliar, animal, tooth, offsale, false)
		tempd
	}	

	def addDetailTreatment(id:Long,activity:Activity, auxiliar:Long, animal:Long, tooth: String, offsale:Long, validate:Boolean =true):Box[TreatmentDetail] = {
		DB.use(DefaultConnectionIdentifier) {
	 		conn =>
	 			try{
					val treatment = Treatment.findByKey(id).get
					// rigel 10/10/2017 - para alertar e não duplicar
					// sem querer na agenda
					if (validate && treatment.details.filter(_.activity == activity).size > 0) {
						val strAux = Activity.findByKey(activity.id.is).get.name							
						// nao alterar esta mensagem "Já existe este serviço
						// sem verificar o trecho no treatmentManger.js
						// que testa este string para inverte ok e cancelar
						throw new RuntimeException("Já existe este serviço/procedimento <" + strAux + "> neste agendamento!");
					}
					if(treatment.isPaid){
						throw new RuntimeException("Não é possível adicionar serviço/procedimento a um atendimento já pago!");
					}
					var detail = TreatmentDetail.createInCompany
					detail.treatment(treatment)
						  .activity(activity)
						  .auxiliar(auxiliar)
						  .offsale(offsale)
						  .price(detail.priceActivity)
						  .save

			        if (AuthUtil.company.appType.isEbellepet) {
						detail.getTdEpet.animal(animal).save;
					}
			        if (AuthUtil.company.appType.isEsmile) {
						detail.getTdEdoctus.tooth(tooth).save;
					}

					//treatment.details += detail
					if (detail.alertObs != "") {
						if (treatment.obs.indexOf (detail.alertObs.trim) == -1) {
							treatment.obs ((treatment.obs + " " + detail.alertObs).trim)
						}
					}
					treatment.resetEndDate
					if(validate) {
						treatment.save
					} else {
						treatment.saveWithoutValidate
					}
					updateCalendar("updateDetail", treatment)
					Full (detail)
				}catch{
					case e:Exception => { conn.rollback
			 			throw e
					}
	 			}
	 	}			
	}

	def addDetailTreatment(id:Long,prod:Product, animal:Long, offsale:Long):Box[TreatmentDetail] = {
		var treatment = Treatment.findByKey(id).get
		var detail = TreatmentDetail.createInCompany

		detail.treatment(treatment)
			  .product(prod)
			  .price(prod.salePrice)
			  .save

        if (AuthUtil.company.appType.isEbellepet) {
			detail.getTdEpet.animal(animal).save;
		}
			  
		//treatment.details += detail
		treatment.save
		updateCalendar("updateDetail", treatment)
		Full (detail)
	}	

	def price(activity:Activity,user:User):BigDecimal ={
		AuthUtil.company.userActivityAssociate_?.is match {
			case true => user.activityPrice(activity)
			case _ => activity.salePrice
		}	
	}
	def duration(activity:Activity,user:User):String ={
		AuthUtil.company.userActivityAssociate_?.is match {
			case true => user.activityDuration(activity)
			case _ => activity.duration.is
		}	
	}	

	def price(activity:Activity,treatment:Treatment):BigDecimal ={
		price(activity,treatment.user.obj.get)
	}

	/**
	* throw new RuntimeException if has payment for this treatment
	*/
	def delete(id:String){
		var treatment = loadTreatment(id)
		if (!treatment.user.isEmpty) {
			var user = treatment.user.obj.get;
			if ((AuthUtil.user.id.is != user.id.is) && 
				AuthUtil.user.isSimpleUserCalendarView) {
	  	        throw new RuntimeException("Você não tem permissão para excluir atendimentos de outros profissionais!")
			}
		}
		//treatment.details.foreach(_.delete_!);
		//treatment.details.clear
		treatment.delete_!;
		updateCalendar("removeTreatment", treatment)
	}

	def getDetail(idDetail:Long):TreatmentDetail = TreatmentDetail.findByKey(idDetail).get

	def deleteDetail(idDetail:Long):TreatmentDetail = {
		var td = TreatmentDetail.findByKey(idDetail).get
		td.delete_!
		td.treatment.obj match {
			case Full(t) => {
				t.resetEndDate.save
				updateCalendar("updateDetail", t)
			}
			case _ =>
		}
		td
	}

	def loadUser(id:String):User  ={
		User.findByKey(id.toLong).get
	}
		
	def loadCustomer(id:String):Customer ={
		Customer.findByKey(id.toLong).get
	}	
	
	def loadTreatment(id:String):Treatment = {
		Treatment.findByKey(id.toLong).get
	}

	def changeCommandId(id:String, command:String){
		val treatment = loadTreatment(id)
		val treatments = loadTreatmentByCustomerNotPaid(treatment.customer.obj.get,treatment.start.is)
		(treatment :: treatments).foreach((t) =>{
			t.command(command).validatePaid.saveWithoutValidate
		})
		updateCalendar("updateDetail", treatment)
	}
	def treatmentsPayadBetween(start:Date, end:Date) = {
        Treatment.findAll(
        				  OrderBy(Treatment.start,Ascending),
                          By(Treatment.company,AuthUtil.company),
                          By(Treatment.status,Treatment.Paid),
                          BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
                          )
    }

	def treatmentsPayadBetweenPerUser(start:Date, end:Date,u:User) = {
        Treatment.findAll(
                          By(Treatment.company,AuthUtil.company),
                          By(Treatment.user,u),
                          By(Treatment.status,Treatment.Paid),
                          BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
                          )
	}    

	def countTreatmentsPayadBetweenPerUser(start:Date, end:Date,u:User) = {
        Treatment.count(
                          By(Treatment.company,AuthUtil.company),
                          By(Treatment.user,u),
                          By(Treatment.status,Treatment.Paid),
                          BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
                          )
	}

	def countTreatmentsBetweenPerDate(start:Date)={
 		Treatment.count(
        	By(Treatment.company,AuthUtil.company),
            BySql("dateevent = date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start)//fica preso ao postgress
           	)
	}

	def countTreatmentsBetweenPerUser(start:Date, end:Date,u:User) = {
        Treatment.count(
                          By(Treatment.company,AuthUtil.company),
                          By(Treatment.user,u),
                          BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
                          )
	}	

	def activitiesMapByUser(user:User, calendarPub:Boolean):List[Activity] = {
		val company = user.company.obj.get
		company.userActivityAssociate_?.is match {
			case true => {
				val array  = user.activitys(calendarPub).toArray
				scala.util.Sorting.quickSort(array)
				array.toList
			}
			case _ => company.activities (calendarPub);
		}
	}
	def animalsMapByCustomer(customer:Customer):List[Customer] = {
		val company = customer.company.obj.get
		val array  = customer.animals.toArray
		//scala.util.Sorting.quickSort(array)
		array.toList
	}

	def activitiesMap:List[Activity] = {
		AuthUtil.company.activities(false)
	}	

	def activitiesMapForTreatment(treatment_id:String) = {
		AuthUtil.company.userActivityAssociate_?.is match {
			case true => Treatment.findByKey(treatment_id.toLong).get.
				user.obj.get.activitys (false).map(a => (a.id.is.toString,a.name.is))
			case _ => AuthUtil.company.activities(false).map(a => (a.id.is.toString,a.name.is))
		}
	}

	def treatmentsInDate(start:Date) = {
        Treatment.findAllInCompany(
        	By(Treatment.hasDetail,true),
        	BySql("dateevent = date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start))
	}

	def treatmentsInDate(start:Date, end:Date, user:Long, unit:Long, cashier:Long) = {
		def cashierBy = if(cashier != 0){
			BySql[Treatment]("payment in( select id from payment where cashier =?)",IHaveValidatedThisSQL("",""),cashier)
		}else{
			BySql[Treatment]("1=1",IHaveValidatedThisSQL("",""))
		}
		def userBy = if(user != 0){
			BySql[Treatment](" user_c = ? ",IHaveValidatedThisSQL("",""), user)
		}else{
			BySql[Treatment]("1=1",IHaveValidatedThisSQL("",""))
		}
        Treatment.findAllInCompany(
        	By(Treatment.unit,unit),
        	userBy, // By(Treatment.user,user),
        	By(Treatment.hasDetail,true),
        	cashierBy,
        	BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),start,end))
	}


	def treatmentsBetweenDates(start:Date, end:Date):List[UserEvent] = {
        Treatment.findAllInCompany(By(Treatment.hasDetail,true),
        						   BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateEvent","01-01-2012 00:00:00"),start,end)
        						  ) ::: BusyEvent.findByDate(start,end);
	}

	def tratmentToJson(t:UserEvent) = {
		t.toJson
	}

	def treatmentsBetweenDatesAsJson(start: Date,end:Date) = {
		JsArray(treatmentsBetweenDates(start,end).map(t => tratmentToJson(t)))
	}	

	def customersWithTreatments(company:Company, date:Date)= Customer.findAll(
			By(Customer.company, company),
			// -- deletado desmarcou faltou
			BySql("id in (select customer from treatment where hasdetail=true and status not in (5,1,8)  and dateevent = date(?))",IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"),date)
		)

	def usersWithTreatments(company:Company, date:Date)= User.findAll(
			By(User.company, company),
			// -- deletado desmarcou faltou
			BySql("id in (select user_c from treatment where hasdetail=true and status not in (5,1,8)  and dateevent = date(?))",IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"),date)
		)

	def tratmentsTodayJson() = {
		JsArray(
				AuthUtil
				.company
				.treatmentsToDay
				.map(
					t => tratmentToJson(t)
					)
			)
	}


	def generatePaymentFromTreatment(treatment:Treatment, specialType:Int):PaymentRequst = {
		//case class ActivityDTO(activityId:Int, activityType:String, id:Int, price:Double, removed:Boolean, amount:Float, for_delivery:Boolean, parentBom:Int, offsale:Int=0);
		//case class PaymentDTO(typePayment:Int,value:Double,removed:Boolean,chequeInfo:ChequeRequest, dateDetailStr:String)
		//case class TreatmentDTO(customerId:Int,userId:Int,treatmentStatus:String,activitys:List[ActivityDTO],removed:Boolean,id:Int, ignored:Boolean=false);
		//treatments:List[],payments:List[PaymentDTO],command:String,dataTreatments:String,cashier:String
		def typePayment = if (specialType == 0 /*bpmonthly*/) {
			AuthUtil.company.bmMonthlyPaymentType.id.is
		} else if (specialType == 1 /*offsale*/) {
			AuthUtil.company.offSalePaymentType.id.is
		} else if (specialType == 2 /*package*/) {
			AuthUtil.company.packagePaymentType.id.is
		} else {
			0
		}
		def cashier:String = {
			try {
				Cashier.findOpenCashiersUnit(0).id.is.toString
  			}catch{
  				//
  				// tratar o retorno está dando a minha msg, mas com um "input string: antes
  				//
  				case e:Exception => """
  				Não foi encontrado caixa aberto.

  				No caso de mensalidades, pacotes ou convênio a troca de status é como um pagamento podendo determinar a comissão para um profissional.

  				Caso existam caixas abertos, verifique se são da mesma unidade do atendimento."""
  			}
		}
		PaymentRequst(
						TreatmentDTO(
									treatment.customer.is.toInt, 
									treatment.user.is.toInt,  
									treatment.status.is.toString, 
									treatment.details.map((td) => {
										val priceAux:Double = if (specialType == 2 /*package*/) {
											val customer = Customer.findByKey(treatment.customer.is.toInt).get
											val product = Product.findByKey(td.activity_id.toInt).get
											DeliveryDetail.findPriceByCustomerProduct(customer,product)(0).price.toDouble;
										} else if (specialType == 0 /*bpmonthly*/) {
											//td.price.is.toDouble
											//25.0
											val customer = Customer.findByKey(treatment.customer.is.toInt).get
											val product = Activity.findByKey(td.activity_id.toInt).get
											val bpMonthlys = BpMonthly.findPriceByCustomerProduct(customer,product, treatment.dateEvent);
											if (!bpMonthlys.isEmpty) {
												val session = bpMonthlys(0).valueSession;
												if (session > td.price.is.toDouble) {
													td.price (BigDecimal (session)).save()
													session
												} else if (session == 0.0) {
													val str = if (bpMonthlys(0).weekDays != "") {
															""
														} else {
															"\n\nÉ preciso informar os dias da semana!\n\n"
														}
	            									throw new RuntimeException("Mensalidade com valor de sessão zero" + str);
												} else {
													session
												}
											} else {
												td.price.is.toDouble
											}
										} else {
											td.price.is.toDouble
										}
										ActivityDTO(
													td.activity_id.toInt, 
													"activity", 
													td.id.is.toInt, 
													priceAux, 
													false,
													1l,
													false, 
													0, 
													td.auxiliar.is.toInt,
													td.animal.toInt,
													td.tooth,
													td.offsale.is.toInt
												);
									}),
									false,
									treatment.id.is.toInt,
									false
						)::Nil,
						PaymentDTO(
									typePayment.toInt,
									treatment.totalValue(0).toDouble,  
									false,
									ChequeRequest("", "", 0, "",""),
									Project.dateToStr(treatment.start.is)
						)::Nil,
						treatment.command.is,
						Project.dateToStr(treatment.start.is),
						cashier,
						"" //status2 vazio
			)

	}
}