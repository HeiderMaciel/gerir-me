package code
package api

import code.model._
import code.util._
import code.service._

import net.liftweb._
import mapper._ 
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers

import scala.xml._

import java.text.ParseException
import java.util._

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.

//Com a lisença poetica do dao
//				pay.datepayment - rigel
object TreatmentReportApi extends RestHelper with ReportRest with net.liftweb.common.Logger  {
	val DAY_IN_MILESECOUNDS = 86400000;
	serve {
			case "treatments" :: "customer_delivery" :: customerId :: Nil JsonGet _ => {
				val sql = """						
				select
						p.name as pname,
						pbom.name as pbomname,
						(select count(1) from deliverydetail dd where dd.delivery=dc.id and used=true and dd.product=pbom.id ) as used,
						(select count(1) from deliverydetail dd where dd.delivery=dc.id and used=false and dd.product=pbom.id ) as un_used,
						pay.command as command,
						(select avg(price) from deliverydetail dd where dd.delivery=dc.id and dd.product=pbom.id ) as price,
						c.idforcompany,
						dc.efetivedate
						from
						deliverycontrol dc
						inner join product p on(p.id = dc.product)
						left join deliverydetail ddd on (dc.id = ddd.delivery)
						inner join product pbom on (ddd.product=pbom.id)
						left join payment pay on( pay.id = dc.payment)
						left join cashier c on( c.id = pay.cashier)
						where dc.customer = ?
						group by ddd.product, pbom.name, p.name, pay.command,c.idforcompany, dc.efetivedate, dc.id, pbom.id
						order by dc.efetivedate desc
				;""";
				//				--and ((select count(1) from deliverydetail dd where dd.delivery=dc.id and used=false and dd.product=pbom.id ) >0 )
				val r = DB.performQuery(sql, scala.List(customerId.toLong))
				JsArray(r._2.map((t)=>
				{
					JsObj(("delivery_name",t(0) match {
				           case a:Any => a.toString
				           case _ => ""
				        }),
						("product_name",t(1) match {
				           case a:Any => a.toString
				           case _ => ""
				        }),
						("used",t(2) match {
				           case a:Any => a.toString.toInt
				           case _ => 0
				        }),
						("un_used",t(3) match {
				           case a:Any => a.toString.toInt
				           case _ => 0
				        }),
						("command",t(4) match {
				           case a:Any => a.toString
				           case _ => 0
				        }),
						("saleprice",t(5) match {
				           case a:Any => a.toString.toDouble
				           case _ => 0.0
				        }),
						("cashier",t(6) match {
				           case a:Any => a.toString.toInt
				           case _ => 0
				        }),
				        ("date",t(7) match {
				           case a:Any => a.toString
				           case _ => "" 
				        })
						)
				}))				

			}
			case "treatments" :: "customer_account" :: customerId :: Nil JsonGet _ => {
				val customer = Customer.findByKey(customerId.toLong).get
				val allDebits = customer.allDebits
				JsObj (("total", customer.valueInAccount.is),
					   ("details",
						JsArray(
							allDebits.map((pd) => 
								JsObj(
									("due_date",pd.payment.obj match {
										case Full(p) => p.datePayment.is.getTime
										case _ => 0l
									}),
									("value",pd.value.is.toDouble),
									("command",pd.payment.obj match {
										case Full(p) => p.command.is
										case _ => ""
									}),
									("cashier", pd.payment.obj match {
										case Full(p) => p.cashier.is
										case _ => ""
									})
								)
							)
						)
				))
			}
			case "treatments" :: "command_missing" :: Nil Post _ => {
				lazy val user = (S.param("user") openOr "0").toLong
				lazy val unit = (S.param("unit") openOr "0").toLong
				lazy val cashier = (S.param("cashier") openOr "0").toLong
				lazy val startDate = Project.strToDateOrToday(S.param("startDate") openOr "")
				lazy val endDate = Project.strToDateOrToday(S.param("endDate") openOr "")
				lazy val commandStart = (S.param("commandStart") openOr "")
				lazy val commandEnd = (S.param("commandEnd") openOr "")
				
				val commands = TreatmentService.treatmentsInDate(startDate, endDate, user, unit, cashier).map((t) => { 
					try { t.command.is.toInt }
					catch { 
						case _ => if (t.command.is.length() > 0) {
							// retira a possível letra que a ieda coloca no fim da comanda
							t.command.is.substring(0,t.command.is.length()-1).toInt
						} else {
							0
						}
					}  })
				lazy val commandStartInt = if(commandStart == ""){
												commands.min
											}else{
												commandStart.toInt
											}
				lazy val commandEndInt = if(commandEnd == ""){
												commands.max
										}else{
												commandEnd.toInt
										}

				if(!commands.isEmpty)
					JsArray((commandStartInt to commandEndInt).toList.filter((command) => !commands.exists(_ == command)).map((c) =>JsObj(("command", c))))
				else
					JsArray()
			}
			case "treatments"::"dashboar" :: Nil Post _ => {
				lazy val unis:String = filterSqlIn("units", "t.unit in (%s)")
				lazy val users = filterSqlIn("users", "t.user_c in (%s)")
				lazy val userGroups = filterSqlIn("user_groups", "t.user_c in (select id from business_pattern where group_c in(%s))")
				lazy val services = filterSqlIn("activitys", "t.id in (select treatment from treatmentdetail where treatment=t.id and activity in (%s))")
				lazy val servicesType = filterSqlIn("activity_types", "t.id in (select treatment from treatmentdetail where treatment=t.id and activity in (select id from product where typeproduct in (%s)))")
				lazy val where = (unis::users::services::userGroups::servicesType::Nil).reduceLeft(_+" and "+_)

				val data = toJArray(TreatmentsDashBoard.sql.format(where.replaceAll("t\\.","ti\\."), where),scala.List(AuthUtil.company.id.is, startParam, endParam))
				val dataCustomer = toJArray(TreatmentsDashBoard.sql_customers.format(where),scala.List(AuthUtil.company.id.is, startParam, endParam))
				val dataDuration = toJArray(TreatmentsDashBoard.sql_avg_duraton.format(where),scala.List(AuthUtil.company.id.is, startParam, endParam))
				JsObj(("data",data), ("customer", dataCustomer),("duration", dataDuration))
			}
			case "treatments"::"ranking" :: Nil Post _ => {
				lazy val startDate = Project.strToDateOrToday(S.param("startDate") openOr "")
				lazy val endDate = Project.strToDateOrToday(S.param("endDate") openOr "")
				lazy val user = (S.param("user") openOr "0").toLong
				val r = DB.performQuery("""
					select sum(value), Extract(Year from datepayment) || '/' || to_char (Extract(Month from datepayment), '09') as m,
					u.name from treatment t
					inner join treatmentdetail td on(td.treatment = t.id and td.company = t.company)
					inner join payment p on(p.id = t.payment and t.company = p.company)
					inner join business_pattern u on(u.id = t.user_c)
					where t.company  = ? and t.user_c =?
					and t.dateevent between date(?) and date(?)
					group by t.user_c, m,u.name
					order by m
					""", scala.List(AuthUtil.company.id.is,user.toLong,startDate,endDate))
				JsArray(r._2.map((t)=>
				{
					JsObj(("total",t(0) match {
				           case a:Any => a.toString
				           case _ => ""
				        }),
						("month",t(1) match {
				           case a:Any => a.toString
				           case _ => 0
				        }),
						("name",t(2) match {
				           case a:Any => a.toString
				           case _ => ""
				        })			        

						)
				}))
			}
		case "treatments"::"print_command" :: Nil Post _ => {
			for {
					command  <- S.param("command") ?~ "command parameter missing" ~> 400
					date_str <- S.param("date") ?~ "date parameter missing" ~> 400
					unit_str <- S.param("unit") ?~ "unit parameter missing" ~> 400
			}yield{
				val unit = unit_str.toLong;
				val sql = """
						select user_id, user_name, product,amount, price, status, name, tdid from (
								
								select u.id user_id,u.short_name as user_name,p.name product,td.amount,td.price, c.name as name,-1 as orderincommand,'X' as status
								,td.id as tdid 
								from   treatment t
								inner join treatmentdetail td on(td.treatment = t.id and td.company = t.company)
								inner join product p on(p.id=td.product or p.id=td.activity and p.company = td.company)
								left join business_pattern u on( u.id = t.user_c and u.company = t.company)
								left join business_pattern c on( c.id = t.customer and c.company = t.company)
								where t.company =? and command=? and t.dateevent=date(?) and t.unit =? and t.status <> 5
							union
								select null,null,p.name,null as amount, p.saleprice,' ' as status ,orderInCommand, '' as name
								,orderInCommand as tdid from 
								product p where p.company =? and showincommad =true and status = 1
								and p.gender in('A',
									(	select c.sex
											from
											treatment t
											inner join business_pattern c on(c.id = t.customer)
											where t.company =? and command=? and t.dateevent=date(?) and t.unit =? and t.status <> 5
											limit 1
									)
								)
								and p.id not in (
									select td.activity from   treatment t
									inner join treatmentdetail td on(td.treatment = t.id and td.company = t.company)
									where t.company =? and command=? and t.dateevent=date(?) and t.unit =? and t.status <> 5
								)
							union
								select null, 'Total' as user_name,'',sum(td.amount),sum (td.price), '' as name,9999999 as orderincommand,'' as status
								,99999999 as tdid from   treatment t
								inner join treatmentdetail td on(td.treatment = t.id and td.company = t.company)
								where t.company =? and command=? and t.dateevent=date(?) and t.unit =? and t.status <> 5
						) as data order by orderInCommand, tdid asc
				""";
				toResponse(sql,scala.List(
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str), unit, 
					AuthUtil.company.id.is, 
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str), unit, 
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str), unit, 
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str), unit))
			}	
		}
		case "treatments"::"expense_ticket" :: Nil Post _ => {
			for {
					command  <- S.param("command") ?~ "command parameter missing" ~> 400
					date_str <- S.param("date") ?~ "customer parameter missing" ~> 400
			}yield{
				val sql = """
						select user_id, user_name, product,amount, price, name, phone, document, cstreet,
							cnumber_c, ccomplement, cdistrict, cpostal_code,
							cnpj, tdid from (
								
								select u.id user_id,u.short_name as user_name,p.name product,td.amount,td.price, 
								c.name as name, c.mobile_phone as phone, c.document as document, c.street as cstreet, 
								c.number_c as cnumber_c, c.complement as ccomplement, c.district as cdistrict, c.postal_code as cpostal_code, 
								bpu.document_company as cnpj, 
								td.id as tdid from   treatment t
								inner join treatmentdetail td on(td.treatment = t.id and td.company = t.company)
								inner join product p on(p.id=td.product or p.id=td.activity and p.company = td.company)
								left join companyunit cu on (cu.id = t.unit)
								left join business_pattern bpu on bpu.id = cu.partner
								left join business_pattern u on( u.id = t.user_c and u.company = t.company)
								left join business_pattern c on( c.id = t.customer and c.company = t.company)
								where t.company =? and command=? and t.dateevent=date(?) and t.status <> 5
							union
								select null, 'Total' as user_name,'', sum(td.amount),sum (td.price), 
								'' as name, '' as phone, '' as document, '' as cstreet, 
								'' as cnumber_c, '' as ccomplement, '' as cdistrict, '' as cpostal_code, 
								'' as cnpj, 
								99999999 as tdid from   treatment t
								inner join treatmentdetail td on(td.treatment = t.id and td.company = t.company)
								where t.company =? and command=? and t.dateevent=date(?) and t.status <> 5
						) as data order by tdid asc
				""";
				toResponse(sql,scala.List(
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str), 
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str)))
			}	
		}
		case "treatments"::"receipt_customer_unit" :: Nil Post _ => {
			for {
					command  <- S.param("command") ?~ "command parameter missing" ~> 400
					date_str <- S.param("date") ?~ "customer parameter missing" ~> 400
			}yield{
				val sql = """
					select bc.name, trim (bc.phone || ' ' || bc.mobile_phone), 
					bc.document || ' ' || bc.document_company as doc_customer, 
					bc.street, bc.number_c, bc.complement, bc.district, bc.postal_code,
					fu_extenso_real(to_number (to_char (pa.value,'999999999.99'),'999999999.99')), 
					cu.name, bu.document || ' ' || bu.document_company as doc_company,
					bu.street, bu.number_c, bu.complement, bu.district, bu.postal_code,
					ci.name, st.name, to_char (pa.datepayment,'dd/mm/yyyy'),
					pr.name, ban.name, to_number (to_char (td.price/td.amount,'9999999.99'),'9999999.99'), 
					td.amount, td.price, to_number (to_char (pa.value,'999999999.99'),'999999999.99'),
					cic.name, stc.name, bc.id, 
					trim (bu.phone || ' ' || bu.mobile_phone || ' ' || bu.email_alternative),
					co.expenseReceiptObs
					--, pa.* 
					from payment pa
					inner join company co on co.id = pa.company
					inner join business_pattern bc on bc.id = pa.customer
					inner join cashier ca on ca.id = pa.cashier
					inner join companyunit cu on cu.id = ca.unit
					inner join treatment tr on tr.payment = pa.id
					inner join treatmentdetail td on td.treatment = tr.id
					inner join product pr on pr.id = td.product or pr.id = td.activity
					left join tdepet tdp on tdp.treatmentDetail = td.id
					left join business_pattern ban on ban.id = tdp.animal
					left join business_pattern bu on bu.id = cu.partner
					left join city ci on ci.id = bu.cityref
					left join state st on st.id = ci.state
					left join city cic on cic.id = bc.cityref
					left join state stc on stc.id = cic.state
					where pa.company = ? and pa.command = ? and pa.datepayment = ?
				""";
				toResponse(sql,scala.List(
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str)))
			}	
		}
		case "treatments"::"receipt_payments" :: Nil Post _ => {
			for {
					command  <- S.param("command") ?~ "command parameter missing" ~> 400
					date_str <- S.param("date") ?~ "customer parameter missing" ~> 400
			}yield{
				val sql = """
					select pt.name || ' ' || coalesce (ba.short_name,'') || ' ' || coalesce (ch.agency,'') 
					               || ' ' || coalesce (ch.account,'') || ' ' || coalesce (ch.number_c,''), 
					pd.value, pd.duedate
					--, pd.* 
					from payment pa
					inner join paymentdetail pd on pd.payment = pa.id
					inner join paymenttype pt on pt.id = pd.typepayment
					left join cheque ch on ch.paymentdetail = pd.id
					left join bank ba on ba.id = ch.bank
					where pa.company = ? and pa.command = ? and pa.datepayment = ?
					order by pd.id
				""";
				toResponse(sql,scala.List(
					AuthUtil.company.id.is, command, Project.strOnlyDateToDate(date_str)))
			}	
		}
		case "treatments"::"getTreatmentsByFilter" :: Nil Post _ => {
			lazy val startDate = Project.strToDateOrToday(S.param("startDate") openOr "")
			lazy val endDate = Project.strToDateOrToday(S.param("endDate") openOr "")
			lazy val customer = S.param("customer") match {
				case Full(s) if(s != "") => By(Treatment.customer,s.toLong)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}

			lazy val user = S.param("user") match {
				case Full(s) if(s != "") => By(Treatment.user,s.toLong)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}

			lazy val activity = S.param("activity") match {
				case Full(s) if(s != "") => BySql[code.model.Treatment]("id in (select distinct t.id from treatment t inner join treatmentdetail td on( td.treatment = t.id) where t.company=? and dateevent between date(?) and date(?) and td.activity =?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), AuthUtil.company.id.is, startDate,endDate, s.toLong)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}
			lazy val product = S.param("product") match {
				case Full(s) if(s != "") => BySql[code.model.Treatment]("id in (select distinct t.id from treatment t inner join treatmentdetail td on( td.treatment = t.id) where t.company=? and dateevent between date(?) and date(?) and td.product =?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), AuthUtil.company.id.is, startDate,endDate, s.toLong)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}			

			lazy val cashiers = S.param("cashier") match {
				case Full(s) if(s != "") => BySql[code.model.Treatment](" payment in (select distinct p.id from payment p where p.company=? and date(p.datePayment) between date(?) and date(?) and p.cashier=?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), AuthUtil.company.id.is, startDate,endDate, s.toLong)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}

			lazy val units = S.param("unit") match {
				case Full(s) if(s != "") => By(Treatment.unit,s.toLong)
				case _ => BySql[code.model.Treatment](AuthUtil.user.unitsToShowSql,IHaveValidatedThisSQL("",""))
			}

			lazy val offsales = S.param("offsale") match {
				case Full(s) if(s != "") => BySql[code.model.Treatment](" customer in (select distinct bp.id from business_pattern bp where bp.company=? and bp.offsale=?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), AuthUtil.company.id.is, s.toLong)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}

			lazy val showDeleteds = S.param("status") match {
				case Full(s) if(s == "5") => true
				case _ => false
			}
			lazy val status = S.param("status") match {
				case Full(s) if(s != "All") => {
					val statusFilter = if(s == "7") {
						Treatment.PreOpen
					}else if(s == "0"){
						Treatment.Open
					}else if(s == "1"){
						Treatment.Missed
					}else if(s == "8"){
						Treatment.ReSchedule
					}else if(s == "2"){
						Treatment.Arrived
					}else if(s == "3"){
						Treatment.Ready
					}else if(s == "4"){
						Treatment.Paid
					}else if(s == "5"){
						Treatment.Deleted
					}else if(s == "6"){
						Treatment.Confirmed
					}else if(s == "9"){
						Treatment.Budget
					}else{
						Treatment.Deleted
					}
					By(Treatment.status2, statusFilter)
				}
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}

			lazy val payment_type = S.param("payment_type") match {
				case Full(s) if(s != "") => BySql[code.model.Treatment]("id in (select distinct t.id from treatment t inner join payment p on(t.payment = p.id) inner join paymentdetail pd on( pd.payment = p.id) where t.company=? and date(start_c) between date(?) and date(?) and pd.typepayment in(%s))".format(S.params("payment_type").foldLeft("0")(_+","+_)),IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"), AuthUtil.company.id.is, startDate,endDate)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}
			lazy val commands = S.param("commands") match {
				case Full(s) if(s != "") => ByList(Treatment.command,s.split(",").map(_.trim).toList)
				case _ => BySql[code.model.Treatment]("1 =1",IHaveValidatedThisSQL("",""))
			}			
			lazy val filterStartEnd = BySql[code.model.Treatment]("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),startDate,endDate)
			lazy val hasDetail = By(Treatment.hasDetail,true)
			lazy val obsFilter = Like(Treatment.obs,"%"+(S.param("obs_search") openOr "")+"%" )
			val params:Seq[net.liftweb.mapper.QueryParam[code.model.Treatment]] = filterStartEnd :: activity :: customer :: user :: status :: payment_type :: hasDetail :: commands :: cashiers :: product :: units :: offsales :: OrderBy(Treatment.start, Descending) :: obsFilter :: Nil
			val treatments = if(showDeleteds){
				Treatment.findAllInCompanyWithDeleteds(params.toList :_*)
			}else{
				Treatment.findAllInCompany(params.toList :_*)
			}
			JsArray(treatments.map((t) => {
				JsObj(
					("date", Project.dateToStrJs(t.start.is)),
					("customerid",t.customer.is),
					("customername",t.customerShortName),
					("obs",t.obs.is),
					("username",t.userShortName),
					("unitname",t.unitShortName),
					("status",t.status.toString),
					("total",t.totalValue(0).toDouble),
					("details",t.descritionDetails),
					("payments",t.paymentDescription),
					("command",t.command.is),
					("cashier",t.cashier),
					("phone",t.customerPhones), //t.customer.obj.get.mobilePhone.is + ' ' + t.customer.obj.get.phone.is + ' ' + t.customer.obj.get.email_alternative.is + ' ' + t.customer.obj.get.email.is),
					("id",t.id.is),
					("end", Project.dateToStrJs(t.end.is)),
					("status2",t.status2.toString)
				)
			})
			)
		}
	}
}