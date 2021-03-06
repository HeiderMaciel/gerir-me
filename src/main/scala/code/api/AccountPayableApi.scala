package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

import net.liftweb._
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers
import net.liftweb.mapper._
import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object AccountPayableApi extends RestHelper with ReportRest with net.liftweb.common.Logger  {
		val FOREVER = 1;

		val PARCELED = 2;

		var apAuxId = 0l;

		serve {

			case "accountpayable" :: "aggregate" :: Nil Post _ => {
				try {
					val ids = S.param("ids").get
					var iteration = 0;
					var aggregId = 0l;
					//
					// Loop Verifica se pode agregar
					//
					ids.split(",").map(_.toLong).map((l:Long) => {
						if (aggregId == 0) {
							if (AccountPayable.findByKey(l).get.aggregateId.is != 0) {
								aggregId = AccountPayable.findByKey(l).get.aggregateId.is
							}
						}
						var idAux = 0l;
						idAux = AccountPayable.findByKey(l).get.aggregateId;
						if (idAux != 0 && aggregId != 0 && idAux != aggregId) {
					      throw new RuntimeException("Um lançamento não pode fazer parte de duas agregações!(api)")
						}
					})
					//
					// Loop agrega efetivamente
					//
					if (aggregId != 0) {
						// já tinha agregado então zera
						AccountPayable.findByKey(aggregId).get.aggregateValue(0.0).save
					}
					iteration = 0;
					ids.split(",").map(_.toLong).map((l:Long) => {
						if (iteration == 0 && aggregId == 0) {
							aggregId = AccountPayable.findByKey(l).get.id.is
						}
						iteration += 1;
						AccountPayable.findByKey(l).get.aggregate(aggregId);
					})
					JBool(true)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "mark_as_paid" :: Nil Post _ => {
				val ids = S.param("ids").get
				ids.split(",").map(_.toLong).map((l:Long) => {
					AccountPayable.findByKey(l).get.makeAsPaid
				})
				JInt(1)
			}

			case "accountpayable" :: "mark_as_unpaid" :: Nil Post _ => {
				val ids = S.param("ids").get
				ids.split(",").map(_.toLong).map((l:Long) => {
					AccountPayable.findByKey(l).get.makeAsUnPaid
				})
				JInt(1)
			}
			case "accountpayable"::"remove_checked" :: Nil Post _ => {
				try {
					val ids = S.param("ids").get
					ids.split(",").map(_.toLong).map((l:Long) => {
						AccountPayable.findByKey(l).get.delete_!
					})
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}
			case "accountpayable" :: "add" :: Nil Post _ => {
				for {
					category <- S.param("category") ?~ "category parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					complement <- S.param("complement") ?~ "complement parameter missing" ~> 400
					value <- S.param("value") ?~ "value parameter missing" ~> 400
					dueDateStr <- S.param("dueDate") ?~ "dueDate parameter missing" ~> 400
					paymentDateStr <- S.param("paymentDate") ?~ "paymentDate parameter missing" ~> 400
					exerciseDateStr <- S.param("exerciseDate") ?~ "exerciseDate parameter missing" ~> 400
					paid = S.param("paid") openOr "False"
					recurrence = S.param("recurrence") openOr "False"
					movementType = S.param("type") ?~ "movementType parameter missing" ~> 400
					recurrence_type <- S.param("recurrence_type") ?~ "recurrence_type parameter missing" ~> 400
					recurrence_term_type <- S.param("recurrence_term_type") ?~ "date_of_end parameter missing" ~> 400
					recurrence_term <- S.param("recurrence_term") ?~ "recurrence_term parameter missing" ~> 400
					user = S.param("user") openOr "0"

					out_of_cacashier <- S.param("out_of_cacashier") ?~ "out_of_cacashier parameter missing" ~> 400
					cashier <- S.param("cashier") ?~ "cashier parameter missing" ~> 400
					cashier_number <- S.param("cashier_number") ?~ "cashier_number parameter missing" ~> 400
					accountStr <- S.param("account") ?~ "account parameter missing" ~> 400

					user_parcels <- S.param("user_parcels") ?~ "user_parcels parameter missing" ~> 400
					user_parceled <- S.param("user_parceled") ?~ "user_parceled parameter missing" ~> 400
					transfer = S.param("transfer") openOr "False"

					out_of_cacashierTo <- S.param("out_of_cacashier_to") ?~ "out_of_cacashier_to parameter missing" ~> 400
					cashierTo <- S.param("cashier_to") ?~ "cashier_to parameter missing" ~> 400
					cashier_numberTo <- S.param("cashier_number_to") ?~ "cashier_number_to parameter missing" ~> 400
					accountTo <- S.param("account_to") ?~ "account_to parameter missing" ~> 400

					amount <- S.param("amount") ?~ "amount parameter missing" ~> 400
					unit <- S.param("unit") ?~ "unit parameter missing" ~> 400
					costcenter <- S.param("costcenter") ?~ "costcenter parameter missing" ~> 400
					unitvalue <- S.param("unitvalue") ?~ "unitvalue parameter missing" ~> 400
					parcelnum <- S.param("parcelnum") ?~ "parcelnum parameter missing" ~> 400
					parceltot <- S.param("parceltot") ?~ "parceltot parameter missing" ~> 400
					paymenttype <- S.param("paymenttype") ?~ "paymenttype parameter missing" ~> 400

					cheque <- S.param("cheque") ?~ "cheque parameter missing" ~> 400
					ch_bank = S.param("ch_bank") openOr "0" 
					ch_agency = S.param("ch_agency") openOr "" 
					ch_account = S.param("ch_account") openOr "" 
					ch_number = S.param("ch_number") openOr "" 

				} yield {
					try {
						var lcheque = cheque.split(",")
						if (lcheque.length < 1) {
							lcheque (0) = "0"
						}
						def cashierObj = Cashier.findByKey(cashier.toLong)
						def dueDate =  Project.strToDateOrToday(dueDateStr)
						def paymentDate =  Project.strOnlyDateToDate(paymentDateStr)
						def exerciseDate = Project.strToDateOrToday(exerciseDateStr)
						def userId = if(user == "") {
							0l
						}else{
							user.toLong
						}
						def unitId = if(unit == "" || unit == "null") {
							AuthUtil.unit.id.is
						}else{
							unit.toLong
						}
						def costCenterId = if(costcenter == "" || costcenter == "null") {
							0l
						}else{
							costcenter.toLong
						}
						def paymentTypeId = if(paymenttype == "" || paymenttype == "null") {
							0l
						}else{
							paymenttype.toLong
						}
						/* nao usa mais agora é lcheque
						def chequeId = if(cheque == "" || cheque == "null") {
							0l
						}else{
							cheque.toLong
						}
						*/
						def cashierBox:Box[Cashier] = cashierObj match {
							case Full(c:Cashier) => Full(c)
							case _ => if(cashier_number != "") {
								Full(Cashier.findByCompanyId(cashier_number.toInt))
							}else{
								Empty
							}
						}
						def register(i:Int, valueReal:Double, dueDate:Date, exerciseDate:Date,
							paymentDate:Date) {
							val account =
							AccountPayable
							.createInCompany
							.unit(unitId)
							.typeMovement(movementType.get.toInt)
							.category(category.toLong)
							.costCenter(costCenterId)
							.obs(obs).complement(complement)
							.value(valueReal.toDouble)
							.dueDate(dueDate)
							.exerciseDate(exerciseDate)
							.paymentDate(paymentDate)
							.paid_?(paid.toBoolean)
							.user(userId)
							.account(accountStr.toLong)
							.amount(amount.toDouble)
							.parcelNum(parcelnum.toInt)
							.parcelTot(parceltot.toInt)
							.paymentType(paymentTypeId)
							.cheque(lcheque(i).toLong)

							if(out_of_cacashier.toBoolean) {
								account.cashier(cashierBox)
							}
							if(recurrence.toBoolean){
								account
								.parcelTot(recurrence_term.toInt)
								.parcelNum(1)
							}
							account.save
							apAuxId = account.id.is;
							if(transfer.toBoolean){
								account.transferTo(accountTo.toLong, out_of_cacashierTo,
									cashierTo, cashier_numberTo)
							}
						}

						if(recurrence.toBoolean && (recurrence_term_type.toInt == FOREVER || recurrence_term.toInt > 1) ){
							val nextParcelDate = BusinessRulesUtil.sunDate(dueDate,recurrence_type.toInt,1)
							def endDate:Date =  if(recurrence_term_type.toInt == FOREVER){
								BusinessRulesUtil.END_OF_THE_WORLD
							}else{
								BusinessRulesUtil.sunDate(dueDate,recurrence_type.toInt,recurrence_term.toInt)
							}
							register(0,value.toDouble,dueDate, 
								exerciseDate, paymentDate)
							//JBool(
							val rec =
								Recurrence
								.createInCompany
								.typeMovement(movementType.get.toInt)
								.category(category.toLong)
								.costCenter(costCenterId)
								.value(value.toDouble)
								.typeRecurrence(recurrence_type.toInt)
								.startDate(nextParcelDate)
								.endDate(endDate)
								.parcelTot(recurrence_term.toInt)
								.lastExecuted(nextParcelDate)
								.user(userId)
								.account(accountStr.toLong)
								.unit(unitId)
								.obs(obs)
								.amount(amount.toDouble)
								.paymentType(paymentTypeId);
							//	)
							rec.save;
							if(recurrence.toBoolean){
								val ap = AccountPayable.findByKey(apAuxId).get
								ap.recurrence(rec.id.is)
								ap.save
							}

						}else{
							if(user_parceled.toBoolean){
								for( i <- 0 to user_parcels.toInt-1) {
									def date = {
									BusinessRulesUtil.sunDate(dueDate,Recurrence.MONTHLY,i)
									}
									register(0,value.toDouble/user_parcels.toDouble,
										date, date, null)
								}
							}else{
								var i = 0;
								for( i <- 0 until lcheque.length){
									register(i,value.toDouble,
										dueDate, exerciseDate, paymentDate)
								}
							}
						}

						if(ch_bank != "0" && ch_agency != "" && 
							ch_account != "" && ch_number != "") {
							// duedate da tela ou do ap 
							// paymentdate
							val pd = if (paymentDateStr != "") {
								paymentDate
							} else {
								dueDate
							}
							val received = if (movementType.get.toInt == AccountPayable.OUT) {
								true
							} else {
								false
							}
							val ch =
								Cheque
								.createInCompany
								.autoCreated_?(false)
								.dueDate(dueDate) 
								.paymentDate(pd)
								.movementType(movementType.get.toInt)
								.received(received)
								.customer(userId) // sacado ou favorecido
								.value(value.toDouble)
								.bank(ch_bank.toLong)
								.agency(ch_agency.toString)
								.account(ch_account.toString)
								.number(ch_number.toString)
								.unit(unitId)
								.obs(obs);
							//	)
							ch.save;
							val ap = AccountPayable.findByKey(apAuxId).get
							ap.cheque(ch.id.is)
							ap.save
						}

						JInt (lcheque.length)
					} catch {
						case e:Exception => JString(e.getMessage)
					}
				}
			}

			case "accountpayable" :: "remove" :: id :: recid :: Nil JsonGet _ => {
				try{
					if (recid == "0" || recid == "") {
						val c = AccountPayable.findByKey(id.toLong).get
						c.delete_!
						JInt(1)
					} else {
						val c = AccountPayable.findByKey(id.toLong).get
						c.deleteRecurencByThis
						//c.delete_!
						JInt(1)
					}
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "accountpayable" :: "edit" :: id :: partiallySecure :: Nil Post _ => {
				val c = AccountPayable.findByKey(id.toLong).get
				for {
					category <- S.param("category") ?~ "category parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					complement <- S.param("complement") ?~ "complement parameter missing" ~> 400
					value <- S.param("value") ?~ "value parameter missing" ~> 400
					dueDateStr <- S.param("dueDate") ?~ "dueDate parameter missing" ~> 400
					paymentDateStr <- S.param("paymentDate") ?~ "paymentDate parameter missing" ~> 400
					exerciseDateStr <- S.param("exerciseDate") ?~ "exerciseDate parameter missing" ~> 400
					paid = S.param("paid") openOr "False"
					movementType = S.param("type") ?~ "movementType parameter missing" ~> 400
					user = S.param("user") openOr "0"
					out_of_cacashier <- S.param("out_of_cacashier") ?~ "out_of_cacashier parameter missing" ~> 400
					cashier <- S.param("cashier") ?~ "cashier parameter missing" ~> 400
					cashier_number <- S.param("cashier_number") ?~ "cashier_number parameter missing" ~> 400
					accountStr <- S.param("account") ?~ "account parameter missing" ~> 400
					amount <- S.param("amount") ?~ "amount parameter missing" ~> 400
					unit <- S.param("unit") ?~ "unit parameter missing" ~> 400
					costcenter <- S.param("costcenter") ?~ "costcenter parameter missing" ~> 400
					paymenttype <- S.param("paymenttype") ?~ "paymenttype parameter missing" ~> 400
					cheque <- S.param("cheque") ?~ "cheque parameter missing" ~> 400
					unitvalue <- S.param("unitvalue") ?~ "unitvalue parameter missing" ~> 400
					parcelnum <- S.param("parcelnum") ?~ "parcelnum parameter missing" ~> 400
					parceltot <- S.param("parceltot") ?~ "parceltot parameter missing" ~> 400
					recurrence_all <- S.param("recurrence_all") ?~ "recurrence_all parameter missing" ~> 400
					recurrence_just_this <- S.param("recurrence_just_this") ?~ "recurrence_just_this parameter missing" ~> 400
				}yield {
					def unitId = if(unit == ""  || unit == "null") {
						AuthUtil.unit.id.is
					}else{
						unit.toLong
					}
					def costCenterId = if(costcenter == ""  || costcenter == "null") {
						0l
					}else{
						costcenter.toLong
					}
					def paymentTypeId = if(paymenttype == ""  || paymenttype == "null") {
						0l
					}else{
						paymenttype.toLong

					}
					def chequeId = if(cheque == ""  || cheque == "null") {
						0l
					}else{
						cheque.toLong

					}
					def userId = if(user == ""  || user == "null") {
						0l
					}else{
						user.toLong

					}
					def cashierObj  = Cashier.findByKey(cashier.toLong)
					def dueDate =  Project.strToDateOrToday(dueDateStr)
					def paymentDate =  Project.strOnlyDateToDate(paymentDateStr)
					def exerciseDate = Project.strToDateOrToday(exerciseDateStr)
					def cashierBox:Box[Cashier] = cashierObj match {
						case Full(c:Cashier) => Full(c)
						case _ => if(cashier_number != "") {
								Full(Cashier.findByCompanyId(cashier_number.toInt))
							}else{
								Empty
							}
					}
					c.typeMovement(movementType.get.toInt)
					.category(category.toLong)
					.obs(obs)
					.complement(complement)
					.value(BusinessRulesUtil.snippetToDouble(value))
					.dueDate(dueDate)
					.paymentDate(paymentDate)
					.exerciseDate(exerciseDate)
					.paid_?(paid.toBoolean)
					.user(userId)
					.account(accountStr.toLong)
					.cashier(cashierBox)
					.amount(BusinessRulesUtil.snippetToDouble(amount))
					.unit(unitId)
					.costCenter(costCenterId)
					.paymentType(paymentTypeId)
					.cheque(chequeId)
					.parcelNum(parcelnum.toInt)
					.parcelTot(parceltot.toInt)
					if(recurrence_all.toBoolean){
						c.reprocessRecurencByThis
					}
					try {
						if (partiallySecure == "true") {
							c.complement((complement + " Alteração de lançamento de caixa fechado e/ou conciliado " + AuthUtil.user.short_name.is + " " + Project.dateTimeToStr(new Date())).trim)
							c.partiallySecureSave
						} else {
							c.save;
						}
						JBool (true);
					} catch {
						case e:Exception => JString(e.getMessage)
					}
				}

			}

			case "accountpayable" :: "list" :: Nil Post _ => {
				for {
						dttype <- S.param("dttype") ?~ "dttype parameter missing" ~> 400
						start <- S.param("start") ?~ "start parameter missing" ~> 400
						startValue <- S.param("startValue") ?~ "startValue parameter missing" ~> 400
						endValue <- S.param("endValue") ?~ "endValue parameter missing" ~> 400
						end <- S.param("end") ?~ "end parameter missing" ~> 400
						startCreate <- S.param("startCreate") ?~ "startCreate parameter missing" ~> 400
						endCreate <- S.param("endCreate") ?~ "endCreate parameter missing" ~> 400
						status <- S.param("status") ?~ "status parameter missing" ~> 400
						types <- S.param("types") ?~ "types parameter missing" ~> 400
						categories <- S.param("categories") ?~ "categories parameter missing" ~> 400
						showtransfer <- S.param("showtransfer") ?~ "showtransfer parameter missing" ~> 400
						accounts <- S.param("accounts") ?~ "accounts parameter missing" ~> 400
						cashiers <- S.param("cashier") ?~ "cashier parameter missing" ~> 400
						units <- S.param("units") ?~ "units parameter missing" ~> 400
						users <- S.param("users") ?~ "users parameter missing" ~> 400
						obs <- S.param("obsSearch") ?~ "obsSearch parameter missing" ~> 400
						costcenters <- S.param("costcenters") ?~ "costcenters parameter missing" ~> 400
						paymenttypes <- S.param("paymenttypes") ?~ "paymenttypes parameter missing" ~> 400
				}yield{
					def dttypes:String = S.param("dttype") match {
						case Full(p) => p
						case _ => "0"; // vencimento
					}
					def startDate = Project.strToDateBox(start)
					def endDate = Project.strToDateBox(end)
					def startValueDouble:Double = if(startValue !=""){
						startValue.toDouble
					}else{
						Float.MinValue.toDouble
					}
					def endValueDouble = if(endValue !=""){
						endValue.toDouble
					}else{
						Float.MaxValue.toDouble
					}

					def startCreateDate = Project.strToDateBox(startCreate)

					def endCreateDate = Project.strToDateBox(endCreate)

					def statusList:List[Boolean] = status.split(",").map(_.toBoolean).toList

					def typesInt:List[Int] = types.split(",").map(_.toInt).toList

					def categoriesLong:List[Long] = if(categories != "0") {
						categories.split(",").map(_.toLong).toList
					}else{
						Nil
					}
					def categoryTx:String = showtransfer

/*						" 1 = 1 "
						} else {
						" category in ( select id from accountcategory where typeMovement <> 2) "	
						}
*/
					def costcentersLong:List[Long] = if(costcenters != "0" && costcenters != "") {
						costcenters.split(",").map(_.toLong).toList
					}else{
						Nil
					}
					def paymenttypesLong:List[Long] = if(paymenttypes != "0" && paymenttypes != "") {
						paymenttypes.split(",").map(_.toLong).toList
					}else{
						Nil
					}
					def accountsLong:List[Long] = if(accounts != "0") {
						accounts.split(",").map(_.toLong).toList
					}else{
						Nil
					}

					def cashierLong:List[Long] = if(cashiers != "0"){
						cashiers.split(",").map(_.toLong).toList
					}else{
						Nil
					}
					def unitLong:List[Long] = if(units != "0"){
						units.split(",").map(_.toLong).toList
					}else{
						Nil
					}
					def usersLong:List[Long] = if(users != "0"){
						users
							.split(",")
							.map(_.trim)
							.filter( _!= "" )
							.map(_.toLong)
							.toList
					}else{
						Nil
					}
					Recurrence.execureRecorenc(endDate match {
						case Full(e) => e
						case _ => new Date()
					})
					JsArray(
							AccountPayable
							.findAllByStartEnd(dttypes, startDate, endDate, categoriesLong, categoryTx, accountsLong, cashierLong,unitLong,usersLong,startCreateDate,endCreateDate, typesInt, statusList, startValueDouble, endValueDouble, obs, costcentersLong, paymenttypesLong).map(
								(c) =>
									JsObj(
											("category",c.category.obj.get.nameStatus),
											("obs",c.obs.is),
											("complement",c.complement.is),
											("obs_trunc",c.obs.is.substring(0,math.min (40, c.obs.is.length))),
											("value",c.value.is.toFloat),
											("aggregateValue",c.aggregateValue.is.toFloat),
											("aggregateId",c.aggregateId.is),
											("aggregateLabel",c.aggregateLabel.is),
											("conciliate",c.conciliate.is),
											("unitvalue",c.unitvalue.is.toFloat),
											("amount",c.amount.is.toFloat),
											("parcelnum",c.parcelNum.is.toInt),
											("parceltot",c.parcelTot.is.toInt),
											("id",c.id.is),
											("dueDate",c.dueDate.is.getTime),
											("paymentDate", dateOrEmpty(c.paymentDate)),
											("exerciseDate",c.exerciseDate.is.getTime),
											("paid",c.paid_?.is),
											("color",c.category.obj.get.color.is),
											("category_id",c.category.is),
											("unit_id",c.unit.is),
											("unit_name",c.unit.obj match {
												case Full(u) => u.short_name.is
												case _ => ""
											}),
											("costcenter_id",c.costCenter.is),
											("costcenter_name",c.costCenter.obj match {
												case Full(u) => u.short_name.is
												case _ => ""
											}),
											("paymenttype_id",c.paymentType.is),
											("cheque_id",c.cheque.is),
											("cheque_desc",c.chequeDesc),
											("paymenttype_name",c.paymentType.obj match {
												case Full(u) => u.short_name.is
												case _ => ""
											}),
											("type",c.typeMovement.is),
											("user_id", c.user.obj match {
												case Full(u) => u.id.is.toString
												case _ => ""
											}),
											("recurrence_id",c.recurrence.is),
											("user_name",c.user.obj match {
												case Full(u) => u.short_name.is
												case _ => ""
											}),
											("cashier",c.cashier.obj match {
												case Full(u) => u.idForCompany.is.toString
												case _ => ""
											}),
											("cashier_id",c.cashier.is),
											("account", c.account.is),
											("account_name", c.account.obj match {
												case Full(u) => u.short_name.is
												case _ => ""
											}),
											("auditstr", c.auditStr)
										)
								)
						)
				}
			}
			case "accountpayable" :: "list_user" :: start :: end :: unitparm :: userIdStr :: dttype :: Nil JsonGet _ => {
				def startDate = Project.strToDateOrToday(start)
				def endDate = Project.strToDateOrToday(end)
				def units:String = if (unitparm == "0") {
					AuthUtil.user.unitsToShowSql
				} else {
					" unit = %s ".format (unitparm)
				}
//				println ("vai ===== units " + units);
				def userId = userIdStr match {
					case s:String if(s !="") => s
					case _ => "0l"
				}

				def dttypes:String = dttype match {
					case s:String if(s !="") => s
					case _ => "0"; // vencimento
				}

				Recurrence.execureRecorenc(endDate)
				JsArray(AccountPayable.findAllByStartEndOnlyPaidByUser(startDate,endDate,
					AuthUtil.company, units, userId, dttypes).map((c) => JsObj( ("account",c.account.obj.get.name.is),
					("category",c.category.obj.get.name.is),
					("obs",c.obs.is), 
					("value",c.value.is.toFloat),
					("aggregateValue",c.aggregateValue.is.toFloat),
					("unitvalue",c.unitvalue.is.toFloat), 
					("id",c.id.is),
					("dueDate",c.dueDate.is.getTime),
					("paid",c.paid_?.is),
					("color",c.category.obj.get.color.is), 
					("category_id",c.category.is),
					("type",c.typeMovement.is), 
					("unit_name",c.unit.obj.get.short_name.is),
					("auditstr", c.auditStr),
					("cashier",c.cashier.obj match {
												case Full(u) => u.idForCompany.is.toString
												case _ => ""
											}) )))


			}
		}
}
