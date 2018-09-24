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

import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
import net.liftweb.mapper._ 

import java.util.Calendar
import java.util.HashMap
import java.sql.Connection
import java.sql.DriverManager
import net.sf.jasperreports.engine.JasperRunManager
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.design.JasperDesign
import net.sf.jasperreports.engine.xml.JRXmlLoader
import net.sf.jasperreports.view.JasperViewer
import net.sf.jasperreports.engine.JasperExportManager


object Reports2 extends RestHelper with ReportRest with net.liftweb.common.Logger {
	lazy val regExQueryParameter = "\\:([^=<>\\s\\']+)".r
	def listUserProf = S.params("user")
	def userProf = {
			if(listUserProf.size > 0) {
				val listParm = listUserProf.foldLeft("")(_+_)
				" id in(select distinct customer from treatment t where t.company=%s and t.user_c in(%s))".format(AuthUtil.company.id.is.toString, listParm)
			} else {
				"1=1"
			}
		}
		//inventory_movements.jrxml
		//val reportFile = "/reports/inventory_movements.jasper"
	 	/*
	 	lazy val cn:Connection = {
			try {
			  Class.forName("org.postgresql.Driver")
			  DriverManager.getConnection("jdbc:postgresql://localhost:5432/e_belle_ligth_c","mateus","amanda1108")
			} catch {
			   case x: Exception =>{
			    x.printStackTrace()
			    null
			   }
			}
		}
		*/
		serve {
			case "report" :: "session_status" :: Nil Post _ =>{
				def user = S.param("user") match {
					case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) if(p != "") => " and bp.id in(%s)".format(S.params("user[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			
/*
				def producttype:String = S.param("category_select") match {
					case Full(s) => s
					case _ => S.params("category_select[]").filter( _!= "").reduceLeft(_+" , "+_)		
				}
*/
				def producttype = S.param("category_select") match {
					case Full(p) if(p != "")=> " and pt.id in(%s)".format(p)
					case _ => S.param("category_select[]") match {
						case Full(p) if(p != "") => " and pt.id in(%s)".format(S.params("category_select[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}			
				def prod = 	 S.param("product") match {
					case Full(p) if(p != "")=> " and pr.id in(%s)".format(p)
					case _ => S.param("product[]") match {
						case Full(p) if(p != "") => " and pr.id in(%s)".format(S.params("product[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				val valueOrQuantity:String = S.param("quantity") match {
					case Full(p) if(p != "")=> "count (td.amount)"
					case _ => "sum (td.price)"
				}			
				val userbreak:String = S.param("userbreak") match {
					case Full(p) if(p != "")=> "bp.short_name || ' - ' ||"
					case _ => ""
				}			
				val turnbreak:String = S.param("turnbreak") match {
					case Full(p) if(p != "")=> """|| 
					case when to_char (tr.start_c,'HH') < '12' then ' M'
					when to_char (tr.start_c,'HH') < '18' then ' T'
					else ' N' end"""
					case _ => ""
				}			
				def classes:String = S.param("type") match {
					case Full(p) => p
					case _ => "0,1";
				} 
//					and pr.typeproduct in (%s)

/*	SEMANA
			val SQL = """
					select short_name_week, tr.status2, count(tr.status2) from dates
					left join treatment tr on tr.dateevent between start_of_week and end_of_week
					and tr.status <> 5
					where tr.company = ? and date_c between ? and ?
					and date_c = start_of_week
					group by date_c, short_name_week, tr.status2
					order by date_c, short_name_week, tr.status2
				"""
*/
				val SQL = """
					select day  || ' ' || short_name_dow %s, 
					ed.name, count(tr.status2) from dates
					left join treatment tr on tr.dateevent = date_c 
					left join enumdesc ed on ed.table_c = 'treatment' and to_number (ed.value,'9') = tr.status2
					where tr.company = ? and date_c between ? and ?
					and tr.status <> 5
					group by date_c, short_name_dow, day, start_c, ed.name
					order by date_c, short_name_dow, day, start_c, ed.name
				"""
				toResponse(SQL.format (turnbreak),
					//.format(userbreak, valueOrQuantity,user,prod,unit,producttype,classes, userbreak, userbreak),
					List(AuthUtil.company.id.is, start, end)) 
			} 

			case "report" :: "referral_ranking" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}			
				def maxcli:Int = S.param("maxcli") match {
					case Full(p) => {
						if (p != "") {
							p.toInt
						} else {
							20
						}
					}
					case _ => 20
				}
				def rankingtype:String = S.param("rankingtype") match {
					case Full(p) => p
					case _ => "25,33";
				}
				// precisa criar outro data no js só com duas colunas e passar
				// para as lib do google
				// ai pode ter o grid com mais informações
			    lazy val ranking_indications_query = """
			            select bp.name, count (bp.id)--, bp.id 
			            from bprelationship br
			            inner join business_pattern bp on bp.id = br.business_pattern
			            where br.relationship in (%s) and br.company = ?
			            and br.startat between ? and ? %s
			            group by bp.name--, bp.id
			            having count(bp.id)>0
			            order by count(bp.id) desc
			            limit ?
			        """
				toResponse(ranking_indications_query.format(rankingtype, unit),
					List(AuthUtil.company.id.is, start, end, maxcli))
			}

			case "report" :: "customer_ranking" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}						
				def productclass:String = S.param("productclass") match {
					case Full(p) => p
					case _ => "0,1";
				}
				val payment_type_param_name = S.param("payment_type[]") match {
					case Full(p) => "payment_type[]"
					case _ => "payment_type"
				}
				def payment_type:String = S.param(payment_type_param_name) match {
					case Full(s) if(s != "") => " and tr.id in (select tr1.id from treatment tr1 " +
					"inner join payment pa on pa.id = tr.payment " +
					"inner join paymentdetail pd on pd.payment = pa.id " +
					"inner join paymenttype pt on pt.id = pd.typepayment and pt.id in " +
					"(%s) where tr1.id = tr.id and tr.company = tr1.company)".format(S.params(payment_type_param_name).foldLeft("0")(_+","+_))
					case _ => " and 1=1 "
				}
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			
				def maxcli:Int = S.param("maxcli") match {
					case Full(p) => {
						if (p != "") {
							p.toInt
						} else {
							20
						}
					}
					case _ => 20
				}

				val sql = """select row_number () OVER (ORDER BY valor_total desc) AS pos, * from (
					select cliente, quantidade, valor_total, 
					round (valor_total/quantidade,2) as ticket, telefone, email, id from 
				(select sum (td.price) valor_total, sum (td.amount) quantidade, bc.name cliente, 
				trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative) as telefone , bc.email as email, bc.id as id
				from treatment tr
					inner join treatmentdetail td on (td.treatment = tr.id and td.company = tr.company)
					left join product pr on (pr.id = td.product or pr.id = td.activity)
					inner join business_pattern bc on (bc.id = tr.customer and bc.company = tr.company)
					where bc.company = ? and tr.status = 4
		            and pr.productclass in(%s)
					and tr.dateevent  between ? and ? %s %s
					group by cliente, bc.id, telefone, email
					order by cliente, bc.id, telefone, email) 
					as data order by valor_total desc limit ?) as data1 ORDER BY valor_total desc
				""";
//				info (payment_type)
//				info (sql.toString)
//				payment_type
				toResponse(sql.format(productclass, unit, payment_type),List(AuthUtil.company.id.is,start,end, maxcli))
			}

			case "report" :: "message_send_log" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}				
				def subFilter = "%"+(S.param("subject_search") openOr "")+"%" 
				def qtd_query = S.param("qtd_start") match {
					case Full(s) if(s != "") => {
						def end =  S.param("qtd_end") match {
							case Full(se) if(se != "") => se.toInt
							case _ => 10000
						}
						
						" times between %s and %s".format (s.toInt, end)
					}
					case _ => {
						" 1 =1 "
					}

				}

			    val compl = if (AuthUtil.user.isSuperAdmin) {
		            " 1 = 1 "
		        } else {
		            " subject not like '%ERRO%' "
		        }
				toResponse(LogMailSend.SQL_TO_REPORT.format(subFilter.toLowerCase, qtd_query, compl),List(AuthUtil.company.id.is,start,end))
			}

			case "report" :: "animal_report" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}				
				def animal = "%"+(S.param("animal") openOr "")+"%" 
				// como os parceiros tutor e indicador são trazidos com left join
				// nao pode fazer o like '%s'
				def tutor = if (S.param("tutor") != "") {
					" and lower (bc.name) like '%"+(S.param("tutor") openOr "")+"%' " 
				} else {
					" and 1 = 1 "
				}
				def indic = if (S.param("indic") != "") {
					" and lower (bi.name) like '%"+(S.param("indic") openOr "")+"%' " 
				} else {
					" and 1 = 1 "
				}

/*
				val SQL = """
				select ban.name, bc.name as tutor, bi.name as indicou, date(ban.createdat), 
				ban.id, bc.id, bi.id from business_pattern ban 
				left join business_pattern bc on bc.id in (select bp_related from bprelationship bpr
				where business_pattern = ban.id and relationship = 27 /* é pet de */ and bpr.company = ban.company) and bc.company = ban.company
				left join business_pattern bi on bi.id in (select bp_related from bprelationship bpr
				where business_pattern = ban.id and relationship = 24 /* indicado por */ and bpr.company = ban.company) and bi.company = ban.company
				where ban.company = ?
				and ban.is_animal = true
				and date (ban.createdat) between ? and ?
				and lower (ban.name) like '%s'
				%s
				%s
				order by ban.name, ban.id, ban.createdat desc
				"""
*/
				val SQL = """
				select ban.name, bc.name as tutor, bi.name as indicou, date(ban.createdat), 
				ban.id, bc.id, bi.id 
				from business_pattern ban 
				left join bprelationship bpr on bpr.business_pattern = ban.id and bpr.relationship = 27
				left join business_pattern bc on bc.id = bpr.bp_related
				left join bprelationship bpr1 on bpr1.business_pattern = ban.id and bpr1.relationship = 24
				left join business_pattern bi on bi.id = bpr1.bp_related
				where ban.company = ?
				and ban.is_animal = true
				and date (ban.createdat) between ? and ?
				and lower (ban.name) like '%s'
				%s
				%s
				order by ban.name, ban.id, ban.createdat desc
				"""
				toResponse(SQL.format(animal.toLowerCase,
					tutor.toLowerCase,indic.toLowerCase),
				List(AuthUtil.company.id.is,start,end))
			}

			case "report" :: "contacts_conciliation" :: Nil Post _ => {
				def origin:String = S.param("origin") match {
					case Full(p) if(p != "") => p
					case _ => " 1 = 2 "  // origem precisa ser informada
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " co.unit =%S ".format(p) 
					case _ => " 1 = 1" 
				}			

				val SQL_REPORT = """
					select co.id, co.name, co.email, co.phone, 
					co.birthday, co.date1, co.obs, bc.name 
					from contact co
					left join business_pattern bc on bc.id = co.business_pattern
					where co.company = ? and co.origin = ?
					and %s
					order by co.name
				"""

//					and co.email in (select to_c from logmailsend lm where lm.company = co.company and subject like 'Dia da Noiva Fidelis Studio ERRO ======= ' and lm.id > 340884)

				toResponse(SQL_REPORT.format(unit),List(AuthUtil.company.id.is, origin))
			}

			case "report" :: "customers" :: Nil Post _ => {
				// customers_report.html
				val sql = """
					select id, short_name, 
					trim (mobile_phone || ' ' || phone || ' ' || email_alternative), 
					email, birthday, createdat,id,
					fu_auditstr (createdby, createdat, updatedby, updatedat) 
					from 
					business_pattern
					where company=? and (%s) and (%s) and (%s) and (%s) and (%s) and (%s) and (%s) and (%s)
				"""
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " unit =%S ".format(p) 
					case _ => " 1 = 1" 
				}			

				def offsale:String = S.param("offsale") match {
					case Full(s) if(s != "") => " offsale = %s".format(s)
					case _ => " 1 = 1 "
				}

				val createRange:String = S.param("start") match {
					case Full(p) if(p != "") => {
						val start = Project.strToDateOrToday(p)
						val end = S.param("end") match{
							case Full(e) => Project.strToDateOrToday(e)
							case _ => new Date()
						}
						" date(createdat) between date('%s') and date('%s') ".format(start, end)
					}
					case _ => "1=1"
				}
				val treatmentRange:String = S.param("start_treatment") match {
					case Full(p)  if(p != "") => {
						val start = Project.strToDateOrToday(p)
						val end = S.param("end_treatment") match{
							case Full(e) => Project.strToDateOrToday(e)
							case _ => new Date()
						}
						val where = """
							id in(
									select customer
										from treatment 
									where customer = business_pattern.id 
									and company=%s
									and dateevent between date('%s') and date('%s') 
									and status = 4
								)
						""".format(AuthUtil.company.id.is, start, end)
						where
					}
					case _ => "1=1"
				}
				val treatmentUser:String = S.param("start_treatment") match {
					case Full(p)  if(p != "") => {
						val start = Project.strToDateOrToday(p)
						val end = S.param("end_treatment") match{
							case Full(e) => Project.strToDateOrToday(e)
							case _ => new Date()
						}
						val where = """
							id in(
								select customer 
								from treatment 
								where customer = business_pattern.id 
								and company=%s
								and dateevent between date('%s') and date('%s') 
								and status = 4
							)  
						""".format(AuthUtil.company.id.is, start, end)
						where
					}
					case _ => "1=1"
				}

				val absenceRange:String = S.param("start_absence") match {
					case Full(p)  if(p != "") => {
						val start = Project.strToDateOrToday(p)
						val end = S.param("end_absence") match{
							case Full(e) => Project.strToDateOrToday(e)
							case _ => new Date()
						}
					val where= """ 
							id not in (
								select customer 
								from treatment 
								where company=%s
								and dateevent between date('%s') and date('%s') 
								and status = 4
							) 
						""".format(AuthUtil.company.id.is, start, end )
						where
					}
					case _ => "1=1"
				}				
				val userService = filterSqlIn("activity", "id in(select distinct customer from treatment t inner join treatmentdetail td on(td.treatment = t.id) where t.customer = business_pattern.id and t.company=business_pattern.company and activity in(%s)) and "+absenceRange+" and "+treatmentUser)
				val activityType  = filterSqlIn("activity_type", "id in(select distinct customer from treatment t inner join treatmentdetail td on(td.treatment = t.id) where t.customer = business_pattern.id and t.company=business_pattern.company and activity in(select id from product where product.company = business_pattern.company and typeproduct in (%s))) and "+absenceRange+" and "+treatmentUser)
				val userProf  = filterSqlIn("user", " id in(select distinct customer from treatment t where t.customer = business_pattern.id and t.company=business_pattern.company and t.user_c in( %s ) ) and "+absenceRange+" and "+treatmentUser)
				val sqlf = sql.format(unit, treatmentRange, absenceRange, createRange, userService, activityType, userProf, offsale)
				//info (sqlf.toString)
				toResponse(sqlf,AuthUtil.company.id.is :: Nil)
			}

			case "report" :: "customer_missed" :: Nil Post _=> {
				val user_param_name = S.param("user[]") match {
					case Full(p) => "user[]"
					case _ => 
				}

				def user = S.param("user") match {
					case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) => " and bp.id in(%s)".format(S.params("user[]").filter(_ != "").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}

				def producttype = S.param("category_select") match {
					case Full(p) if(p != "")=> " and pr.typeproduct in(%s)".format(p)
					case _ => S.param("category_select[]") match {
						case Full(p) if(p != "") => " and pr.typeproduct in(%s)".format(S.params("category_select[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}	
				def prod = 	 S.param("product") match {
					case Full(p) if(p != "")=> " and pr.id in(%s)".format(p)
					case _ => S.param("product[]") match {
						case Full(p) if(p != "") => " and pr.id in(%s)".format(S.params("product[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}
				def classes:String = S.param("productclass") match {
					case Full(p) => p
					case _ => "0,1";
				} 

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def start2:Date = S.param("start2") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end2:Date = S.param("end2") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			

				def offsale:String = S.param("offsale") match {
					case Full(s) if(s != "") => " and bc.offsale = %s".format(s)
					case _ => " and 1 = 1 "
				}

				val strAux = if (PermissionModule.anvisa_?) {
						" bc.barcode  || ' ' || "
					} else {
						""
					}
				val SQL = """
					select cu.short_name, bp.name as profissional, tr.dateevent as data, 
					pr.name as servico, td.amount, td.price, trim (""" + 
					strAux + """ bc.name) as cliente, bc.email,
					trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative) as telefone,
					bc.id, bp.id, td.activity, td.product from treatment tr 
					inner join business_pattern bc on bc.id = tr.customer
					inner join treatmentdetail td on td.treatment = tr.id
					left join business_pattern bp on bp.id = tr.user_c
					left join companyunit cu on cu.id = bp.unit
					inner join product pr on (pr.id = td.activity or pr.id = td.product)
					where tr.company = ? 
					and tr.status = 4
					and tr.dateevent between ? and ? 
					and bc.id not in (select tr1.customer from treatment tr1 where tr1.company = tr.company and tr1.customer = bc.id 
					and tr1.dateevent between ? and ?)
					%s %s %s
					%s
					%s
					and pr.productclass in (%s)
					order by cu.short_name, bp.name, tr.dateevent					
				"""
					//LogActor ! SQL
				toResponse(SQL.format(unit, offsale, user, prod, producttype, classes),
					List(AuthUtil.company.id.is, start, end, start2, end2))
			}

			case "report" :: "customer_missed_frequency" :: Nil Post _=> {
				val user_param_name = S.param("user[]") match {
					case Full(p) => "user[]"
					case _ => 
				}

				def user = S.param("user") match {
					case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) => " and bp.id in(%s)".format(S.params("user[]").filter(_ != "").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}

				def producttype = S.param("category_select") match {
					case Full(p) if(p != "")=> " and pr.typeproduct in(%s)".format(p)
					case _ => S.param("category_select[]") match {
						case Full(p) if(p != "") => " and pr.typeproduct in(%s)".format(S.params("category_select[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}	
				def prod = 	 S.param("product") match {
					case Full(p) if(p != "")=> " and pr.id in(%s)".format(p)
					case _ => S.param("product[]") match {
						case Full(p) if(p != "") => " and pr.id in(%s)".format(S.params("product[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}
				def classes:String = S.param("productclass") match {
					case Full(p) => p
					case _ => "0,1";
				} 

				def period:String = S.param("period") match {
					case Full(p) => p
					case _ => "7";
				} 

				def base:String = S.param("start") match {
					case Full(p) => Project.dateToDb (Project.strToDateOrToday(p))
					case _ => Project.dateToDb (new Date())
				}

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			

				def offsale:String = S.param("offsale") match {
					case Full(s) if(s != "") => " and bc.offsale = %s".format(s)
					case _ => " and 1 = 1 "
				}

				val strAux = if (PermissionModule.anvisa_?) {
						" bc.barcode  || ' ' || "
					} else {
						""
					}
				var SQL = """
				select cu.short_name, trim (""" + strAux + """ bc.name), 
				trim (bc.phone || ' ' || bc.mobile_phone  || ' ' || email_alternative || ' ' || email) as phone, bc.obs, 
				(select count (1) from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*(6+1))) and (date(now()) - ((30*6)+1))) as menos_6, 
				(select count (1) from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*(5+1))) and (date(now()) - ((30*5)+1))) as menos_5, 
				(select count (1) from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*(4+1))) and (date(now()) - ((30*4)+1))) as menos_4, 
				(select count (1) from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*(3+1))) and (date(now()) - ((30*3)+1))) as menos_3, 
				(select count (1) from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*(2+1))) and (date(now()) - ((30*2)+1))) as menos_2, 
				(select count (1) from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*(1+1))) and (date(now()) - ((30*1)+1))) as menos_1, 
				bc.id from business_pattern bc 
				left join companyunit cu on cu.id = bc.unit
				where bc.company = ?
				and bc.id not in (select customer tr from treatment tr where tr.company = bc.company and tr.customer = bc.id and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - 30) and date (now())+30)
				and bc.id in (select customer tr from treatment tr where tr.company = bc.company and tr.customer = bc.id  and tr.status in (3,4)
				    and tr.dateevent between (date(now()) - (30*6)) and (date(now()) - (30+1)))
				    order by bc.name
				"""
					//LogActor ! SQL
//				toResponse(SQL.format(unit, offsale, user, prod, producttype, classes),
//					List(AuthUtil.company.id.is, start, end, start2, end2))
				SQL = SQL.replaceAll ("30", period)	
				SQL = SQL.replaceAll ("now\\(\\)", "'" + base + "'")	
//				println ("vaiiiii ================ " + SQL)
				toResponse(SQL,
					List(AuthUtil.company.id.is))
			}

			case "report" :: "todo_list" :: Nil Post _=> {
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bc.id =%S".format(p) 
					case _ => ""
				}			
				val user_param_name = S.param("user[]") match {
					case Full(p) => "user[]"
					case _ => 
				}

				def user = S.param("user") match {
					case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) => " and bp.id in(%s)".format(S.params("user[]").filter(_ != "").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}

				def status = S.param("status_todo") match {
					case Full(p) if(p != "")=> " and tr.status in(%s)".format(p)
					case _ => S.param("statys_todo[]") match {
						case Full(p) => " and tr.status in(%s)".format(S.params("status_todo[]").filter(_ != "").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}

				def prod = 	 S.param("product") match {
					case Full(p) if(p != "")=> " and pr.id in(%s)".format(p)
					case _ => S.param("product[]") match {
						case Full(p) if(p != "") => " and pr.id in(%s)".format(S.params("product[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}

				def start:Date = S.param("start_date") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end_date") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def daysahead:Long = S.param("daysahead") match {
					case Full(p) if(p != "") => p.toLong
					case _ => 0;
				}			

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			

				def offsale:String = S.param("offsale") match {
					case Full(s) if(s != "") => " and bc.offsale = %s".format(s)
					case _ => " and 1 = 1 "
				}

				val strAux = if (PermissionModule.anvisa_?) {
						" bc.barcode  || ' ' || "
					} else {
						""
					}
				val SQL = """
					select bc.id, tr.dateevent, 
					fu_dt_humanize (tr.dateevent),
					to_char (tr.start_c, 'hh24:mi'), 
					tr.status, 
					pr.name, trim (""" +
					strAux + """ bc.name) as cliente, 
					(select case 
					  when tr1.status = 1 then 'faltou' 
					  when tr1.status = 8 then 'desmarcou' 
					  end || ' ' || tr1.detailtreatmentastext || ' ' || 
					  to_char (tr1.dateevent,'DD/MM/YYYY' || ' ' || bp1.short_name) 
					  from treatment tr1 
					  inner join business_pattern bp1 on bp1.id = tr1.user_c
					  where tr1.customer = bc.id 
					and tr1.id in 
					(select max (tr2.id) from treatment tr2 where tr2.customer = tr.customer and tr2.status in (1,8) and tr2.hasdetail = true and tr2.dateevent < date(now()))),
					trim (tr.obs || ' ' || td.obs), 
					trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative || ' ' || bc.email) as telefone ,
					cu.short_name,
					bp.name as profissional, 
					pj.name,
					bm.short_name,
					tr.id, 
					/* action troca status atendido */ 
					/* action edita obs */
					/* action desmarca e cria outro */
					bp.id,
					td.id,
					pr.id,
					bm.id,
					pj.id
					from treatment tr 
					inner join business_pattern bc on bc.id = tr.customer
					inner join business_pattern bp on bp.id = tr.user_c
					inner join treatmentdetail td on td.treatment = tr.id
					inner join companyunit cu on cu.id = tr.unit
					inner join product pr on pr.id = td.activity and pr.crmservice = true
					left join projecttreatment pt on pt.treatmentdetail = td.id
					left join project pj on pj.id = pt.project
					left join business_pattern bm on bm.id = pj.bp_manager
					where tr.status in (0,3) and tr.company = ? 
					and tr.dateevent between (?) and (date(?) + ?::integer)
					%s
					%s
					%s
					%s
					%s
					%s
					/* status */
					/* project */
					/* project class */
					order by tr.dateevent desc, bp.name asc
				"""
				toResponse(SQL.format(customer, status, unit, offsale, user, prod),
					List(AuthUtil.company.id.is, start, end, daysahead))
			}

			case "report" :: "birthdays" :: Nil Post _ => {
				def mapicon = S.param("mapIcon") match {
					case Full(p) if(p != "")=> " and bp.mapicon in(%s)".format(p)
					case _ => S.param("mapIcon[]") match {
						case Full(p) if(p != "") => " and bp.mapicon in(%s)".format(S.params("mapIcon[]").foldLeft("0")(_+","+_))
						case _ => "" 
					}
				}
				val status_param_name = S.param("status[]") match {
					case Full(p) => "status[]"
					case _ => "status"
				}

				def status:String = S.param(status_param_name) match {
					case Full(s) => if(s == "All") {
						" and bp.status in (1,4,0) "
						} else {
						" and bp.status in ("+S.params(status_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
						}
					case _ => " and 1=1 "
				}

				def start:Int = S.param("start") match {
					case Full(p) => p.toInt
					case _ => 1 
				}
				def end:Int = S.param("end") match {
					case Full(p) => p.toInt
					case _ => 31
				}

				def month:Int = S.param("month") match {
					case Full(p) => p.toInt+1
					case _ => Calendar.getInstance().get(Calendar.MONTH)+1
				}

				def customer:Boolean = S.param("customer") match {
					case Full(p) if(p=="on") => true
					case _ => false
				}

				def employee:Boolean = S.param("employee") match {
					case Full(p) if(p=="on") => true
					case _ => false
				}				
				def userstatus:String = S.param("userstatus") match {
					case Full(p) if(p=="on") => " and bp.is_employee=true and userstatus = 1 "
					case _ => ""
				}				
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}			

				val sql = """
					select * from 
					(
					select date_part ('day', bp.birthday) as day, bp.birthday, bp.id, bp.name, 
					trim (bp.mobile_phone || ' ' || bp.phone || ' ' || bp.email_alternative), bp.email, cu.short_name, bp.is_customer, bp.is_employee, bp.id
					from business_pattern bp
					inner join companyunit cu on cu.id = bp.unit
					where 
					bp.company =? and
					bp.birthday is not null
					and date_part ('month', bp.birthday) = ?
					and (bp.is_customer=? or bp.is_employee=?)
					%s
					%s
					%s
					%s
					)
					 as data
					where day between ? and ?
					order by 1
				"""
				toResponse(sql.format(userstatus, unit, mapicon, status),List(AuthUtil.company.id.is,month, customer, employee, start, end))
			}
			case "report" :: "birthdays_by_professional" :: Nil Post _ => {
				def start:Int = S.param("start") match {
					case Full(p) => p.toInt
					case _ => 1 
				}
				def end:Int = S.param("end") match {
					case Full(p) => p.toInt
					case _ => 31
				}

				def month:Int = S.param("month") match {
					case Full(p) => p.toInt+1
					case _ => Calendar.getInstance().get(Calendar.MONTH)+1
				}

				def customer:Boolean = S.param("customer") match {
					case Full(p) if(p=="on") => true
					case _ => false
				}

				def employee:Boolean = S.param("employee") match {
					case Full(p) if(p=="on") => true
					case _ => false
				}				
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}			
				def user = S.param("user") match {
					case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) if(p != "") => " and bp.id in(%s)".format(S.params("user[]").foldLeft("0")(_+","+_))
						case _ => "" 
					}
				}			

				val sql = """
					select bp.short_name as profissional, date_part ('day', bc.birthday) as day, to_char (bc.birthday, 'MM')||'/'||to_char (bc.birthday, 'DD') as aniversario, bc.id as codigo, bc.name as Nome,  
					trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative) as telefone, bc.email, cu.short_name, 
					tr1.dateevent as data_atendimento, bc.createdat as data_criacao, bo.name as usuario, bc.id
					from (select distinct tr.customer, tr.user_c, tr.company from treatment tr where tr.company = ? and tr.status = 4 and tr.dateevent > date (now()) - integer '182') as data1
					inner join business_pattern bp on bp.id = data1.user_c and bp.company = data1.company
					inner join business_pattern bc on bc.id = data1.customer and bc.company = data1.company
					and bc.status = 1 and bc.birthday is not null 
					left join treatment tr1 on tr1.customer = bc.id and tr1.user_c = bp.id and tr1.id = (select max (tr2.id) from treatment tr2 where tr2.customer = bc.id and tr2.user_c = bp.id)
					inner join business_pattern bo on bo.id = bc.createdby
					inner join companyunit cu on cu.id = tr1.unit
					where date_part ('month', bc.birthday) = ?
					and (bc.is_customer=true or bc.is_employee=true)
					and date_part ('day', bc.birthday) between ? and ?
					%s %s
					order by bp.userstatus, bp.name, aniversario
					"""
				toResponse(sql.format(unit, user),List(AuthUtil.company.id.is,month, start, end))
			}

			case "report" :: "animalbirthdays" :: Nil Post _ => {
				def start:Int = S.param("start") match {
					case Full(p) => p.toInt
					case _ => 1 
				}
				def end:Int = S.param("end") match {
					case Full(p) => p.toInt
					case _ => 31
				}

				def month:Int = S.param("month") match {
					case Full(p) => p.toInt+1
					case _ => Calendar.getInstance().get(Calendar.MONTH)+1
				}

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and ba.unit =%S".format(p) 
					case _ => ""
				}			

				val sql = """
				select * from ( 
				select date_part ('day', ba.birthday) as day, ba.birthday, 
				ba.id, ba.name, bc.name,  
				trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative),
				bc.email, cu.short_name, bi.name, bc.id, bi.id 
				from business_pattern ba 
				left join business_pattern bc on bc.id in (select business_pattern from bprelationship br 
				   where br.company = ba.company and br.status = 1 and relationship = 26 /* dono de */
				   and bp_related = ba.id) and bc.company = ba.company and bc.is_animal = false
				left join business_pattern bi on bi.id = ba.bp_indicatedby
				left join companyunit cu on cu.id = ba.unit
				where ba.company = ? and ba.is_animal = true
				and ba.birthday is not null
				and ba.deathDate is null
				and date_part ('month', ba.birthday) = ?
				%s
				) as data
				where day between ? and ?
				order by 1, 3
				"""
				toResponse(sql.format(unit),List(AuthUtil.company.id.is, month, start, end))
			}

			case "report" :: "customer_account" :: Nil Post _=> {
				val strAux = if (PermissionModule.anvisa_?) {
						" bp.barcode  || ' ' || "
					} else {
						""
					}
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bp.id =%S".format(p) 
					case _ => ""
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}			
				def start_value:Double = S.param("start_value") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => -9999999;
				}
				def end_value:Double = S.param("end_value") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => 9999999;
				}
				val SQL = """select bp.id, """ + strAux + """ bp.name, 
				trim (mobile_phone || ' ' || phone || ' ' || email_alternative), email, valueinaccount ,cu.name
				from business_pattern bp  
				left join companyunit cu on cu.id = bp.unit
				where valueinaccount <>0 and valueinaccount between ? and ? and bp.company=? %s %s order by valueinaccount """
				toResponse(SQL.format(customer, unit),List(start_value, end_value, AuthUtil.company.id.is))
			}

			case "report" :: "customer_fidelity" :: Nil Post _=> {
				val strAux = if (PermissionModule.anvisa_?) {
						" bp.barcode  || ' ' || "
					} else {
						""
					}
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bp.id =%S".format(p) 
					case _ => ""
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}			
				def start_value:Double = S.param("start_value") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => -9999999;
				}
				def end_value:Double = S.param("end_value") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => 9999999;
				}
				val SQL = """select bp.id, """ + strAux + """ bp.name, 
				trim (mobile_phone || ' ' || phone || ' ' || email_alternative), email, valueinpoints ,cu.name
				from business_pattern bp  
				left join companyunit cu on cu.id = bp.unit
				where valueinpoints <>0 and valueinpoints between ? and ? and bp.company=? %s %s order by valueinpoints """
				toResponse(SQL.format(customer, unit),List(start_value, end_value, AuthUtil.company.id.is))
			}

			case "report" :: "customer_accountdetail" :: Nil Post _=> {
				def customer:Long = S.param("customer") match {
					case Full(p) if(p != "") => p.toLong
					case _ => 0
				}			
				val SQL = """
				select tr.dateevent, tr.command, ca.idforcompany, cu.short_name, bp.short_name, pr.name, 
--				(td.price / pa.value * pd.value),
				case when pr.productclass = 3 then (td.price / pa.value * pd.value)
				     when pr.productclass = 2 then (td.price / pa.value * pd.value)
				     when pr.productclass in (0,1) then (td.price / pa.value * pd.value) * -1
				end,
				null,
				pt.short_name, bu.short_name, pa.createdat, 
				case when tr.customer <> tr.customerorigin then tr.customerorigin
				     when tr.customer = tr.customerorigin then null
				end,
				pr.productclass, 
				pd.id, bp.id, bu.id, td.id
				from treatment tr 
				inner join treatmentdetail td on td.treatment = tr.id
				inner join product pr on pr.id = td.product or pr.id = td.activity
				inner join payment pa on pa.id = tr.payment
				inner join paymentdetail pd on pd.payment = pa.id
				inner join paymenttype pt on pt.id = pd.typepayment
				inner join business_pattern bu on bu.id = pa.createdby
				inner join cashier ca on ca.id = pa.cashier
				inner join companyunit cu on cu.id = ca.unit
				left join business_pattern bp on bp.id = tr.user_c
				where tr.company = ? and tr.customer = ?
				and (pr.productclass not in (0,1) or pt.customerregisterdebit = true or customerusecredit = true)
				order by pa.datepayment, pa.id, pd.id, tr.dateevent, tr.id 
				"""
				toResponse(SQL,List(AuthUtil.company.id.is, customer))
			}

			case "report" :: "budget_plain" :: Nil Post _=> {
				for {
						trid  <- S.param("trid") ?~ "trid parameter missing" ~> 400
				}yield{
					val SQL = """select 'ig.name', pt.name || ' ' || td.external_id, 
						pr.name, tded.tooth, td.amount, um.short_name, 
						to_char (td.price/td.amount,'999999.99'), td.price, bc.name
						from treatment tr 
						inner join treatmentdetail td on td.treatment = tr.id
						inner join product pr on pr.id = td.product or pr.id = td.activity
						left join business_pattern bc on bc.id = tr.customer
						left join unitofmeasure um on um.id = pr.unitofmeasure
						left join producttype pt on pt.id = pr.typeproduct
						--left join invoicegroup ig on ig.id = pt.invoicegroup
						left join tdedoctus tded on tded.treatmentDetail = td.id
						where tr.company = ? and tr.id = ?
						order by tr.id, tr.customer asc"""
					toResponse(SQL,List(AuthUtil.company.id.is, trid.toLong))
				}
			}

			case "report" :: "customer_invoice" :: Nil Post _=> {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bc.id =%S".format(p) 
					case _ => ""
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and iv.unit =%S".format(p) 
					case _ => ""
				}			
				val SQL = """select bc.id, bc.name as cliente, tr.start_c as data_atendimento, cu.name, bp.name as profissional, iv.idforcompany, 
					iv.efectivedate as data_fatura, (select sum (td.price) 
						from treatmentdetail td where td.treatment = tr.id) as total_paciente, 
					iv.value as total_fatura, it.id 
					from business_pattern bc 
					inner join treatment tr on tr.customer = bc.id
					left join business_pattern bp on bp.id = tr.user_c
					inner join invoicetreatment it on tr.id = it.treatment
					inner join invoice iv on iv.id = it.invoice
					left join companyunit cu on cu.id = iv.unit
					where bc.company = ? and iv.efectivedate between ? and ?
					%s %s
					order by bc.name, iv.idforcompany """
				toResponse(SQL.format(customer, unit),List(AuthUtil.company.id.is, start, end))
			}

			case "report" :: "invoice_plain" :: Nil Post _=> {
				for {
						invoiceit  <- S.param("invoiceit") ?~ "invoiceit parameter missing" ~> 400
				}yield{
					val SQL = """select ig.name, pt.name || ' ' || td.external_id, pr.name, bp.name, td.amount, um.short_name, to_char (td.price/td.amount,'999999.99'), td.price
						from invoice iv 
						inner join invoicetreatment it on it.invoice = iv.id
						inner join treatment tr on tr.id = it.treatment
						inner join treatmentdetail td on td.treatment = tr.id
						inner join product pr on pr.id = td.product or pr.id = td.activity
						left join business_pattern bp on bp.id = td.auxiliar
						left join unitofmeasure um on um.id = pr.unitofmeasure
						left join producttype pt on pt.id = pr.typeproduct
						left join invoicegroup ig on ig.id = pt.invoicegroup
						where iv.company = ? and it.id = ?
						order by iv.idforcompany desc, tr.id, tr.customer, ig.id asc
						"""
					toResponse(SQL,List(AuthUtil.company.id.is, invoiceit.toLong))
				}
			}

			case "report" :: "invoice_summary" :: Nil Post _=> {
				for {
						invoiceid  <- S.param("invoiceid") ?~ "invoiceid parameter missing" ~> 400
				}yield{
					val SQL = """select iv.idforcompany, 
						os.name, bo.document_offsale, 
						cu.name, bu.document_company, bu.street, ci.name || ' / ' || st.short_name, bu.district, bu.postal_code,
						tr.command, bc.document_offsale, tr.dateevent, bc.name, 
						it.value, 
							(select count (distinct (customer)) from treatment tr1
							inner join invoicetreatment it1 on it1.treatment = tr1.id
							inner join invoice iv1 on iv1.id = it1.invoice
							where iv1.company = iv.company and iv1.idforcompany = iv.idforcompany and iv1.id = iv.id ) as nro_pacientes,
							(select count (it1.id) from invoicetreatment it1 where it1.invoice = iv.id) as nro_atendimentos,
						iv.value, trim (bu.mobile_phone || ' ' || bu.phone)
						from invoice iv 
						inner join invoicetreatment it on it.invoice = iv.id
						inner join offsale os on os.id = iv.offsale
						inner join treatment tr on tr.id = it.treatment
						inner join business_pattern bc on bc.id = tr.customer
						inner join companyunit cu on cu.id = iv.unit
						left join treatedoctus ted on ted.treatment = tr.id 
						left join business_pattern bo on bo.id = os.partner
						left join business_pattern bu on bu.id = cu.partner
						left join city ci on ci.id = bu.cityref
						left join state st on st.id = bu.stateref
						where iv.company = ? and iv.id = ? order by tr.dateevent, tr.start_c, tr.id
						"""
					toResponse(SQL,List(AuthUtil.company.id.is, invoiceid.toLong))
				}
			}

			case "report" :: "customer_treatment" :: Nil Post _=> {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bc.id =%S".format(p) 
					case _ => ""
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and iv.unit =%S".format(p) 
					case _ => ""
				}			
				val SQL = """select bc.id, bc.name, trim (bc.mobile_phone || bc.phone || bc.email_alternative), 
					bp.name, cu.name, tr.dateevent, tr.command, os.short_name, tr.obs, tr.id 
					from treatment tr 
					inner join business_pattern bc on bc.id = tr.customer
					left join business_pattern bp on bp.id = tr.user_c
					left join companyunit cu on cu.id = tr.unit
					left join treatedoctus ted on ted.treatment = tr.id
					left join offsale os on os.id = ted.offsale
					where tr.company = ? and tr.dateevent between ? and ?
					and tr.status <> 5 -- and tr.hasdetail = true
					%s %s order by tr.dateevent desc
					 """
				toResponse(SQL.format(customer, unit),List(AuthUtil.company.id.is, start, end))
			}

			// 
			// Vai ser descontinuado em 30/03/2017
			//
			case "report" :: "customer_bpmonthly" :: Nil Post _=> {
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bp.id =%S".format(p) 
					case _ => ""
				}			
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}			
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				val SQL = """
					select bp.id, bp.name, 
					trim (bp.mobile_phone || ' ' || bp.phone || ' ' || bp.email_alternative), bp.email, 
					cu.short_name, bm.endat, pr.name, to_number (substr (pr.duration,1,2),'99')*60 + to_number (substr (pr.duration,4,2),'99') as minutos, 
					pr.saleprice, bm.value, bm.valuediscount, 
					round (to_number (to_char(valuesession,'999999.99999'),'999999.99999'),2), 
					bm.numsession, bm.canceled, bm.obs, bp.id
					from bpmonthly bm 
					inner join business_pattern bp on bp.id = bm.business_pattern
					inner join product pr on pr.id = bm.product
					left join companyunit cu on cu.id = bp.unit
					where bm.endat between ? and ?
					and bm.company=? %s %s  order by bm.endat, bp.name, pr.name """

				toResponse(SQL.format(customer, unit),List(start, end, AuthUtil.company.id.is))
			}

			case "report" :: "monthly_cross" :: Nil Post _ =>{

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				val rel_paid:String = S.param("rel_paid") match {
					case Full(p) if(p != "")=> " and mo.paid = true "
					case _ => " and mo.paid = false "
				}			

				val SQL = """
				select * from (
				select * from (
				select to_char (date('2001-01-01'),'MON/YY'), bc.name, 
				coalesce (sum (mo.value),0), date('2001-01-01') as date_c, true from 
				monthly mo
				left join business_pattern bc on bc.id = mo.business_pattern
				where mo.company = ? and mo.status = 1 --and mo.paid = true 
				and mo.value > 0.01
				and date (mo.dateexpiration) between date (?) and date (?)
				%s
				group by bc.name
				order by bc.name) as data1
				union  
				select * from (
				select substr (short_name_year || mo.paid,1,7), bc.name, 
				coalesce (sum (mo.value),0), date_c, mo.paid from dates 
				left join monthly mo on mo.company = ? and mo.status = 1
				and date(mo.dateexpiration) between start_of_month and end_of_month 
				left join business_pattern bc on bc.id = mo.business_pattern
				where date_c between date (?) and date (?) and day = 1 and mo.value > 0.02
				%s
				group by date_c, short_name_year, bc.name, mo.paid
				order by date_c, short_name_year, bc.name, mo.paid) as data1
				union  
				select * from (select substr (short_name_year || mo.paid,1,7), 'V ' || (select name from company where id = mo.company) , 
				coalesce (sum (mo.value),0), date_c, mo.paid from dates 
				left join monthly mo on mo.company = ? and mo.status = 1
				and date(mo.dateexpiration) between start_of_month and end_of_month 
				where date_c between date (?) and date (?) and day = 1 and mo.value > 0.02
				%s
				group by date_c, short_name_year, mo.paid, mo.company
				order by date_c, short_name_year, mo.paid, mo.company) as data2
				union
				select * from (select substr (short_name_year || mo.paid,1,7), 'Q ' || (select name from company where id = mo.company) , 
				count (distinct mo.business_pattern), date_c, mo.paid from dates 
				left join monthly mo on mo.company = ? and mo.status = 1
				and date(mo.dateexpiration) between start_of_month and end_of_month 
				where date_c between date (?) and date (?) and day = 1 and mo.value > 0.02
				%s
				group by date_c, short_name_year, mo.paid, mo.company
				order by date_c, short_name_year, mo.paid, mo.company) as data2
				) 
				as data3
				order by date_c, 2
								"""
				toResponse(SQL.format (rel_paid, rel_paid, rel_paid, rel_paid),
					List(AuthUtil.company.id.is, start, end ,
						AuthUtil.company.id.is, start, end ,
						AuthUtil.company.id.is, start, end ,
						AuthUtil.company.id.is, start, end )) //, start, end)) 
			} 

			case "report" :: "sql_command" :: Nil Post _ => {
				val id:Long =  S.param("command") match {
					case Full(p) if(p != "") => p.toLong 
					case _ => 0l
				}
				try{
					val sql = SqlCommand.findByKey(id).get.sqlcmd.is
					val parameters = regExQueryParameter.findAllIn(sql)
					val preparedQuery = parameters.foldLeft(sql)((query, param) => {
						val requestParameter = S.param(param.replace(":","")).get
						val resultQuery = query.replace(param, requestParameter)
						resultQuery
					});
					toResponseWithHeader(preparedQuery,List(AuthUtil.company.id.is))
				}catch{
					case e:Exception => {
						e.printStackTrace
						JInt(1)
					}
					case _ => JInt(1)
				}
				
			}

			case "report" :: "project" :: Nil Post _=> {
				def customer:String = S.param("customer") match {
					case Full(p) if(p != "") => " and bs.id =%s".format(p) 
					case _ => ""
				}			
				def email:String = S.param("email") match {
					case Full(p) if(p != "") => " and bs.email like '%%%s%%'".format(p) 
					case _ => ""
				}			
				def projectclass:String = S.param("projectclass") match {
					case Full(s) => " and 1=1 "
					case _ => " and pc.id in(%s)".format(S.params("projectclass[]").filter( _!= "").reduceLeft(_+" , "+_))		
				}
				val status_param_name = S.param("status[]") match {
					case Full(p) => "status[]"
					case _ => "status"
				}

				def status:String = S.param(status_param_name) match {
					case Full(s) => if(s == "All") {
						" and pr.status in (1,4,0) "
						} else {
						" and pr.status in ("+S.params(status_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
						}
					case _ => " and 1=1 "
				}
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and pr.unit =%s".format(p) 
					case _ => ""
				}			
				val SQL = """select pr.id, pr.name, bs.id, bs.name, bs.email, 
					trim (bs.mobile_phone || ' ' || bs.phone || ' ' || bs.email_alternative), 
					bm.name, bm.email, 
					trim (bm.mobile_phone || ' ' || bm.phone || ' ' || bm.email_alternative), 
					pr.startat, pr.obs, pt.name, pc.name, cc.name, pr.numberofguests from project pr
					left join business_pattern bs on bs.id = pr.bp_sponsor
					left join business_pattern bm on bm.id = pr.bp_manager
					left join costcenter cc on cc.id = pr.costcenter
					left join projectclass pc on pc.id = pr.projectclass
					left join projecttype pt on pt.id = pc.projecttype 
					where pr.company = ? %s %s %s %s %s
					 """
				toResponse(SQL.format(email,customer, unit, projectclass, status),List(AuthUtil.company.id.is))
			}
			case "report" :: "relationship" :: Nil Post _ =>{
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def relationshiptype:String = S.param("relationshiptype") match {
					case Full(s) => " 1=1 "
					case _ => "rt.id in(%s)".format(S.params("relationshiptype[]").filter( _!= "").reduceLeft(_+" , "+_))		
				}
				val SQL = """
					select bp.name, bp.sex, 
					case when (bp.sex = 'M' or bp.sex is null or bp.sex = 'N') then re.name when bp.sex = 'F' then re.female_name end, 
					br.name
					from bprelationship rr
					inner join business_pattern bp on bp.id = business_pattern
					inner join business_pattern br on br.id = bp_related and br.company = bp.company
					inner join relationship re on re.id = rr.relationship
					inner join relationshiptype rt on rt.id = re.relationshiptype
					where rr.company = ? and 
					re.company in (26, ?) and %s
					order by bp.name, rt.orderinreport, re.orderinreport, bp_related
	        	""".format(relationshiptype);

				toResponse(SQL,List(AuthUtil.company.id.is, AuthUtil.company.id.is)) //, start, end))
			}
			case "report" :: "stakeholder" :: Nil Post _ =>{
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def projectclass:String = S.param("projectclass") match {
					case Full(s) => " 1=1 "
					case _ => "pc.id in(%s)".format(S.params("projectclass[]").filter( _!= "").reduceLeft(_+" , "+_))		
				}
				val status_param_name = S.param("status[]") match {
					case Full(p) => "status[]"
					case _ => "status"
				}

				def status:String = S.param(status_param_name) match {
					case Full(s) => if(s == "All") {
						" and pr.status in (1,4,0) "
						} else {
						" and pr.status in ("+S.params(status_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
						}
					case _ => " and 1=1 "
				}
				val SQL = """
					select pc.name, pr.name, bsh.name from stakeholder sh 
					inner join project pr on pr.id = sh.project
					inner join business_pattern bsh on bsh.id = sh.business_pattern
					left join stakeholdertype sht on sht.id = sh.stakeholdertype
					left join projectclass pc on pc.id = pr.projectclass
					where sh.company = ? and sh.status = 1 and (%s) %s
					order by pc.name, pr.name, bsh.name
	        	""".format(projectclass, status);

//			LogActor ! SQL

				toResponse(SQL,List(AuthUtil.company.id.is)) //, start, end))
			}
			case "report" :: "stakeholder_by_project" :: Nil Post _ =>{
				def project = S.param("project") match {
					case Full(p) => p.toLong
					case _ => 0l
				}
				val SQL = """
					select 
						st.name,
						bp.name,
						trim (bp.mobile_phone || ' ' || bp.phone || ' ' || bp.email_alternative),
						bp.id,
						s.id
					from 
					stakeholder s
					inner join business_pattern bp on( bp.id = s.business_pattern )
					left join stakeholdertype st on st.id = s.stakeholdertype
					where 
							bp.company = ? and s.project = ? and s.status = 1
							order by st.orderinreport, bp.name
	        	"""

				toResponse(SQL,List(AuthUtil.company.id.is, project)) //, start, end))
			}

			case "report" :: "paymentcondition_by_project" :: Nil Post _ =>{
				def project = S.param("project") match {
					case Full(p) => p.toLong
					case _ => 0l
				}
				val SQL = """
					select days, paymentdate, percent, value, obs, id from paymentcondition 
					where 
					company = ? and project = ? and status = 1
					order by days, paymentdate
	        	"""

				toResponse(SQL,List(AuthUtil.company.id.is, project)) //, start, end))
			}

			case "report" :: "section_by_project" :: Nil Post _ =>{
				def project = S.param("project") match {
					case Full(p) => p.toLong
					case _ => 0l
				}
				val SQL = """
					select orderinreport, title, obs, id from projectsection 
					where 
					company = ? and project = ? and status = 1
					order by orderinreport, title
	        	"""

				toResponse(SQL,List(AuthUtil.company.id.is, project)) //, start, end))
			}


			case "report" :: "td_activities" :: Nil Post _ =>{
				def treatment = S.param("treatment") match {
					case Full(p) => p.toLong
					case _ => 0l
				}

				def productclass:String = S.param("productclass") match {
					case Full(p) => p
					case _ => "0"
				}

				val SQL = """
					select pr.name, tded.tooth, ba.name, price/amount, amount, 
					price, td.obs, os.short_name,
					td.external_id, td.id from treatmentdetail td
					inner join product pr on (pr.id = td.activity or pr.id = td.product) and pr.productclass = ?
					left join business_pattern ba on ba.id = td.auxiliar
					left join offsale os on os.id = td.offsale
					left join tdedoctus tded on tded.treatmentdetail = td.id
					where td.company = ? and td.treatment = ? order by td.id
		        	"""
				toResponse(SQL,List(productclass.toLong, AuthUtil.company.id.is, treatment))
			}


			case "customer" :: "gender_chart" :: Nil Post _ => {
				def mapicon = S.param("mapIcon") match {
					case Full(p) if(p != "")=> " and bp.mapicon in(%s)".format(p)
					case _ => S.param("mapIcon[]") match {
						case Full(p) if(p != "") => " and bp.mapicon in(%s)".format(S.params("mapIcon[]").foldLeft("0")(_+","+_))
						case _ => "" 
					}
				}
				val sql_class = """and bp.id in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1
				inner join projectclass pc on pc.id in (%s) and pc.id = pr.projectclass
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def projectclass:String = S.param("projectclass") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("projectclass") != Empty || 
						S.param("projectclass[]") != Empty) {
							sql_class.format(S.params("projectclass[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}
				val sql_noclass = """and bp.id not in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1
				inner join projectclass pc on pc.id in (%s) and pc.id = pr.projectclass
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def noprojectclass:String = S.param("noprojectclass") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("noprojectclass") != Empty || 
						S.param("noprojectclass[]") != Empty) {
							sql_noclass.format(S.params("noprojectclass[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}
				val sql_project = """and bp.id in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1 and pr.id in (%s)
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def project:String = S.param("project") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("project") != Empty || 
						S.param("project[]") != Empty) {
							sql_project.format(S.params("project[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}
				val sql_noproject = """and bp.id not in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1 and pr.id in (%s)
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def noproject:String = S.param("noproject") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("noproject") != Empty || 
						S.param("noproject[]") != Empty) {
							sql_noproject.format(S.params("noproject[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}		

				def offsale:String = S.param("offsale") match {
					case Full(s) if(s != "") => " and bp.offsale = %s".format(s)
					case _ => " and 1 = 1 "
				}

				val sex_param_name = S.param("sex[]") match {
					case Full(p) => "sex[]"
					case _ => "sex"
				}

				def sex:String = S.param(sex_param_name) match {
					case Full(s) => if(s == "All") {
						" and bp.sex in ('F', 'M', 'N') "
						} else {
						" and bp.sex in ('"+S.params(sex_param_name).filter(_ != "").map(_.toString).reduceLeft(_+"','"+_)+"')"
						}
					case _ => " and 1=1 "
				}

				val status_param_name = S.param("status[]") match {
					case Full(p) => "status[]"
					case _ => "status"
				}

				def status:String = S.param(status_param_name) match {
					case Full(s) => if(s == "All") {
						" and bp.status in (1,4,0) "
						} else {
						" and bp.status in ("+S.params(status_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
						}
					case _ => " and 1=1 "
				}

				def civilstatus = S.param("civilstatus") match {
					case Full(p) if(p != "")=> " and bp.civilstatus in(%s)".format(p)
					case _ => S.param("civilstatus[]") match {
						case Full(p) if(p != "") => " and bp.civilstatus in(%s)".format(S.params("civilstatus[]").foldLeft("0")(_+","+_))
						case _ => "" 
					}
				}

				val sql = """select * from (select (case when sex = 'F' then 'Feminino' when sex = 'M' then 'Masculino' when sex not in ('F', 'M') then 'Não Informado' end) as sex, count (sex) as cont
							from business_pattern bp where bp.company = ? and bp.is_person = true 
							%s %s %s %s %s %s %s %s %s %s 
							group by bp.sex order by bp.sex) as data1 where cont <> 0;
							""";
				toResponse(sql.format (unit, sex, status, civilstatus, mapicon, projectclass, noprojectclass, project, noproject, offsale),
					List(AuthUtil.company.id.is/*,start,end*/))
			}
			case "customer" :: "age_range_chart" :: Nil Post _ => {
				def mapicon = S.param("mapIcon") match {
					case Full(p) if(p != "")=> " and bp.mapicon in(%s)".format(p)
					case _ => S.param("mapIcon[]") match {
						case Full(p) if(p != "") => " and bp.mapicon in(%s)".format(S.params("mapIcon[]").foldLeft("0")(_+","+_))
						case _ => "" 
					}
				}
				val sql_class = """and bp.id in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1
				inner join projectclass pc on pc.id in (%s) and pc.id = pr.projectclass
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def projectclass:String = S.param("projectclass") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("projectclass") != Empty || 
						S.param("projectclass[]") != Empty) {
							sql_class.format(S.params("projectclass[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}
				val sql_noclass = """and bp.id not in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1
				inner join projectclass pc on pc.id in (%s) and pc.id = pr.projectclass
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def noprojectclass:String = S.param("noprojectclass") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("noprojectclass") != Empty || 
						S.param("noprojectclass[]") != Empty) {
							sql_noclass.format(S.params("noprojectclass[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}
				val sql_project = """and bp.id in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1 and pr.id in (%s)
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def project:String = S.param("project") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("project") != Empty || 
						S.param("project[]") != Empty) {
							sql_project.format(S.params("project[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}
				val sql_noproject = """and bp.id not in (select st.business_pattern from stakeholder st 
				inner join project pr on pr.id = st.project and pr.status = 1 and pr.id in (%s)
				where st.company = bp.company and bp.id = st.business_pattern and st.status = 1)"""
				def noproject:String = S.param("noproject") match {
					case Full(s) => " and 1=1 "
					// o tratamento é pq as company que nao tem projeto tava quebrando
					case _ => if (S.param("noproject") != Empty || 
						S.param("noproject[]") != Empty) {
							sql_noproject.format(S.params("noproject[]").filter( _!= "").reduceLeft(_+" , "+_))		
						} else {
							" and 1=1 "
						}
				}

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bp.unit =%S".format(p) 
					case _ => ""
				}		

				def offsale:String = S.param("offsale") match {
					case Full(s) if(s != "") => " and bp.offsale = %s".format(s)
					case _ => " and 1 = 1 "
				}

				val sex_param_name = S.param("sex[]") match {
					case Full(p) => "sex[]"
					case _ => "sex"
				}

				def sex:String = S.param(sex_param_name) match {
					case Full(s) => if(s == "All") {
						" and bp.sex in ('F', 'M', 'N') "
						} else {
						" and bp.sex in ('"+S.params(sex_param_name).filter(_ != "").map(_.toString).reduceLeft(_+"','"+_)+"')"
						}
					case _ => " and 1=1 "
				}

				val status_param_name = S.param("status[]") match {
					case Full(p) => "status[]"
					case _ => "status"
				}

				def status:String = S.param(status_param_name) match {
					case Full(s) => if(s == "All") {
						" and bp.status in (1,4,0) "
						} else {
						" and bp.status in ("+S.params(status_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
						}
					case _ => " and 1=1 "
				}

				def civilstatus = S.param("civilstatus") match {
					case Full(p) if(p != "")=> " and bp.civilstatus in(%s)".format(p)
					case _ => S.param("civilstatus[]") match {
						case Full(p) if(p != "") => " and bp.civilstatus in(%s)".format(S.params("civilstatus[]").foldLeft("0")(_+","+_))
						case _ => "" 
					}
				}

				def agerange:Long = S.param("agerange") match {
					case Full(p) if(p != "") => p.toLong
					case _ => 01
				}			

				val sql = """select nome, qtde from (select ai.name as nome, ai.startmonths, count (meses) as qtde from (select birthday, ((DATE_PART('year', now()) - DATE_PART('year', birthday)) * 12) 
					+ (DATE_PART('month', date (now())) - DATE_PART('month', birthday)) as meses
					from business_pattern bp where company = ? and birthday is not null and is_person = true 
					%s %s %s %s %s %s %s %s %s %s 
					order by birthday)as data1
					inner join agerangeinterval ai on ai.startmonths < meses and ai.endmonths >= meses and ai.agerange = ? and (ai.company =  ? or ai.company = 26)
					group by ai.startmonths, ai.name 
					order by ai.startmonths, ai.name) as data2
					""";
				toResponse(sql.format (unit, sex, status, civilstatus, mapicon, projectclass, noprojectclass, project, noproject, offsale),
					List(AuthUtil.company.id.is, agerange, AuthUtil.company.id.is/*,start,end*/))
			}
			case "report" :: "treatments_simple" :: Nil Post _=> {
				def user = S.param("user") match {
					case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) if(p != "") => " and bp.id in(%s)".format(S.params("user[]").foldLeft("0")(_+","+_))
						case _ => " and 1=1 " 
					}
				}			
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			
				val SQL = """
					select tr.start_c, bp.name, pr.name, bc.name, 
					trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative) as telefone, 
					bc.email as email, os.short_name, 
					tr.status, tr.obs, bc.id from treatment tr
					inner join business_pattern bc on bc.id = tr.customer
					left join business_pattern bp on bp.id = tr.user_c
					inner join treatmentdetail td on td.treatment = tr.id
					inner join product pr on pr.id = td.activity or pr.id = td.product
					left join companyunit cu on cu.id = tr.unit
					left join offsale os on os.id = td.offsale
					where tr.company = ? and tr.status <> '5' and tr.dateevent between ? and ?  
										%s
										%s
					order by bp.name, tr.start_c
					"""
					//LogActor ! SQL
				toResponse(SQL.format(unit,user),List(AuthUtil.company.id.is, start, end))
			}

			case "report" :: "bpmonthly_ending" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}						
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and bm.unit =%S".format(p) 
					case _ => " and " + BpMonthly.unitsToShowSql
				}			

				val sql = """select bc.name as cliente,
				    trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative) as telefone, 
				    bc.email as email, bm.value/bm.bpmcount as valor, bm.valuediscount/bm.bpmcount as valor_com_disconto, 
				    bm.endat as termino, pr.name as produto, bp.name as profissional, cu.short_name as unidade, bc.id from bpmonthly bm 
					inner join business_pattern bc on bc.id = bm.business_pattern
					inner join product pr on pr.id = bm.product
					left join business_pattern bp on bp.id = bm.user_c
					left join companyunit cu on cu.id = bm.unit
					where bm.company = ?
					and bm.endat between ? and ?
					and bm.product not in (select product from bpmonthly bm1 where bm1.business_pattern = bm.business_pattern 
					        and bm1.company = bm.company    
					        and bm1.product = bm.product and (bm1.startat > date (now()) or bm1.startat > bm.endat))
		            and (select typeproduct from product where id = bm.product) not in (select pr.typeproduct from bpmonthly bm1 
		            inner join product pr on pr.id = bm1.product
		            where bm1.business_pattern = bm.business_pattern 
		            and bm1.company = bm.company    
		            and (bm1.startat > date (now()) or bm1.startat > bm.endat))
					and bm.canceled = false %s
					order by bm.endat, bc.name
				""";
//				info (payment_type)
//				info (sql.toString)
//				payment_type
				toResponse(sql.format(unit),List(AuthUtil.company.id.is,start,end))
			}

			case "report" :: "active_bpmonthly" :: Nil Post _=> {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}			
				val activity:Long = S.param("activity") match {
					case Full(p) if(p != "")=> p.toLong
					case _ => 0
				}
				val activity_type:List[String] = S.param("activity_type") match {
					case Full(p) if(p != "")=> S.params("activity_type")
					case _ => Nil
				}			
				val activity_where = if(activity == 0){
					" and 1 = 1"
				}else{
					" and bm.product="+activity
				}
				val activity_type_where = activity_type match {
					case Nil => " and 1 = 1"
					case ats:List[String] => " and bm.product in (select id from product where typeproduct in(%s))".format(ats.reduceLeft(_+","+_))
					case _ => " and 1 = 1"
				}
	/*			val active:Long = S.param("active") match {
					case Full(p) if(p != "")=> 1
					case _ => 0
				}			
				val rel_mini:Long = S.param("rel_mini") match {
					case Full(p) if(p != "")=> 1
					case _ => 0
				}			
	*/			
				val customer:Long = S.param("customer") match {
					case Full(p) if(p != "")=> p.toLong
					case _ => 0
				}

				val unitLong:Long = S.param("unit") match {
					case Full(p) if(p != "")=> p.toLong
					case _ => 0
				}			
				
				val unit_where = if(unitLong == 0){
					" and 1 = 1"
				}else{
					" and bm.unit="+unitLong
				}

				val customer_where = if(customer == 0){
					" and 1 = 1"
				}else{
					" and bc.id="+customer
				}

				var SQL = """
				select bc.id, bc.name, 
					trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative), bc.email, 
					(select count (1) from bpmonthly bm2 where bm2.business_pattern = bm.business_pattern 
						    and bm2.product = bm.product
						    and date (now()) + 7 > bm2.endat 
					            and bm2.canceled = false
					            and bm2.product not in (select product from bpmonthly bm1 where bm1.business_pattern = bm2.business_pattern 
					            and bm1.company = bm2.company    
					            and bm1.product = bm2.product and (bm1.startat > date (now()) or bm1.startat > bm2.endat))) as alert, 
		            bm.startat, bm.endat, pr.name, pr.saleprice, bm.value, bm.valuediscount, bm.valuediscount/bm.bpmcount, numsession, valuesession, canceled, bm.obs 
		            from bpmonthly bm
					inner join business_pattern bc on bc.id = bm.business_pattern
					left join product pr on pr.id = bm.product
					left join companyunit cu on cu.id = bm.unit
					where bm.company = ? and date (?) between bm.startat and bm.endat 
					%s %s %s %s
					order by bc.name, bm.startat
				"""
				toResponse(SQL.format(activity_where, customer_where, unit_where, 
						activity_type_where),List(AuthUtil.company.id.is, start /*, end*/))
			}

			case "report" :: "summary_bpmonthly" :: Nil Post _=> {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}			
				val activity:Long = S.param("activity") match {
					case Full(p) if(p != "")=> p.toLong
					case _ => 0
				}
				val activity_type:List[String] = S.param("activity_type") match {
					case Full(p) if(p != "")=> S.params("activity_type")
					case _ => Nil
				}			
				val activity_where = if(activity == 0){
					" and 1 = 1"
				}else{
					" and bm.product="+activity
				}
				val activity_type_where = activity_type match {
					case Nil => " and 1 = 1"
					case ats:List[String] => " and bm.product in (select id from product where typeproduct in(%s))".format(ats.reduceLeft(_+","+_))
					case _ => " and 1 = 1"
				}
	/*			val active:Long = S.param("active") match {
					case Full(p) if(p != "")=> 1
					case _ => 0
				}			
				val rel_mini:Long = S.param("rel_mini") match {
					case Full(p) if(p != "")=> 1
					case _ => 0
				}			
	*/			
				val customer:Long = S.param("customer") match {
					case Full(p) if(p != "")=> p.toLong
					case _ => 0
				}

				val unitLong:Long = S.param("unit") match {
					case Full(p) if(p != "")=> p.toLong
					case _ => 0
				}			
				
				val unit_where = if(unitLong == 0){
					" and 1 = 1"
				}else{
					" and bm.unit="+unitLong
				}

				val customer_where = if(customer == 0){
					" and 1 = 1"
				}else{
					" and bc.id="+customer
				}

				var SQL = """
					select bc.name, bm.startat, bm.endat, pr.short_name, bm.valuediscount, 
					bm.valuediscount/bm.bpmcount, 
					to_number (to_char (bm.valuesession,'999999999.99'),'999999999.99'), bm.numsession,
					(select count (1) from treatment tr where tr.customer = bm.business_pattern 
					and tr.dateevent between bm.startat and bm.endat
					and tr.company = bm.company and tr.status = 0) as agendado,
					(select count (1) from treatment tr where tr.customer = bm.business_pattern 
					and tr.dateevent between bm.startat and bm.endat
					and tr.company = bm.company and tr.status2 = 3) as atendido,
					(select count (1) from treatment tr where tr.customer = bm.business_pattern 
					and tr.dateevent between bm.startat and bm.endat
					and tr.company = bm.company and tr.status2 = 1) as faltou,
					(select count (1) from treatment tr where tr.customer = bm.business_pattern 
					and tr.dateevent between bm.startat and bm.endat
					and tr.company = bm.company and tr.status2 = 8) as remarcou,
					bm.id, bc.id from bpmonthly bm 
					inner join business_pattern bc on bc.id = bm.business_pattern
					left join product pr on pr.id = bm.product
					left join companyunit cu on cu.id = bm.unit
					where bm.company = ? and date (?) between bm.startat and bm.endat 
					%s %s %s %s
					order by bc.name, bm.startat
				"""
				toResponse(SQL.format(activity_where, customer_where, unit_where, 
						activity_type_where),List(AuthUtil.company.id.is, start /*, end*/))
			}

			case "report" :: "account_history" :: Nil Post _ => {
				def users:String = S.param("user") match {
					case Full(s) => if(s == "SELECT_ALL" || s == "") {
						" and 1 = 1 " 
						} else {
						" and ah.createdby = %s".format(s)
						}
					case _ => " and 1 = 1 "
				}
				def units:String = S.param("unit") match {
					case Full(s) if(s != "") => " and ap.unit = %s".format(s)
					case _ => " and " + AccountPayable.unitsToShowSql
				}

				def account:String = S.param("account") match {
					case Full(s) if(s != "") => s
					case _ => " nada " // tem que ter 
				}

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				lazy val SQL_REPORT = """
					select ac.short_name as conta, bu.short_name as usuario, 
					ah.createdat, ah.value, ah.description, ah.currentvalue as saldo, 
					ap.obs, aa.name as categoria, ap.duedate, ap.value, bp.name, 
					cu.short_name, ah.accountpayable
					from accounthistory ah 
					inner join account ac on ac.id = ah.account
					inner join business_pattern bu on bu.id = ah.createdby
					left join accountpayable ap on ap.id = ah.accountpayable 
						and ap.toconciliation = false
					left join accountcategory aa on aa.id = ap.category
					left join business_pattern bp on bp.id = ap.user_c
					left join companyunit cu on cu.id = ap.unit %s 
					where ah.company = ?
					and date (ah.createdat) between ? and ?
					and ah.account in (%s)
					%s
					order by ah.createdat desc
					"""
				toResponse(SQL_REPORT.format(units, account, users),List(AuthUtil.company.id.is, start, end))
			}


			case "report" :: "account_bc_conciliation" :: Nil Post _ => {

				def account:Long = S.param("account") match {
					case Full(s) if(s != "") => s.toLong	
					case _ => 0l // tem que ter account
				}

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def category = AccountCategory.balanceControlCategory.id.is;

				lazy val SQL_REPORT = """
					select ah.currentvalue, ah.paymentdate, 
					ah.paymentdate + 1, ah.id 
					from accounthistory ah
					inner join accountpayable ap on ap.company = ah.company
						and ap.account = ah.account and ap.id = ah.accountpayable 
						and ap.category = ?
					where ah.company = ? and ah.account = ?
					and ah.paymentdate < ?
					and ah.unit = ?
					order by ah.paymentdate desc, ah.id desc
					limit 10
					"""
				toResponse(SQL_REPORT,
					List(category, AuthUtil.company.id.is, 
						account, start, AuthUtil.unit.id.is));
			}

			case "report" :: "account_conciliation" :: Nil Post _ => {

				def account:String = S.param("account") match {
					case Full(s) if(s != "") => s
					case _ => " nada " // tem que ter 
				}

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				lazy val SQL_REPORT = """
					select ap.id,
					case when (ap.duedate <> ap.paymentdate) then ap.duedate else null end as vencimento, 
					case when (ap.paymentdate is not null) then ap.paymentdate else ap.duedate end as pagamento, 
					ct.short_name, 
					bp.short_name, ap.obs, 
					ap.typemovement,
					case when (ap.typemovement = 0) then ap.value else (ap.value * (-1)) end as valor , 
--					case when (ap.typemovement = 0) then ap.value else null end as entrada , 
--					case when (ap.typemovement = 1) then ap.value else null end as saida , 
					case when (ap.typemovement = 0) then ap.aggregateValue else (ap.aggregateValue * (-1)) end as agregado , 
					ap.paid, conciliate, ap.id, ap.id, ap.category, null, null, ap.account, ap.unit
					from accountpayable ap 
					inner join accountcategory ct on ct.id = ap.category
					left join business_pattern bp on bp.id = ap.user_c
					where ap.company = ?
					and ap.toconciliation = false
					and ap.account = %s
					and (ap.paymentdate between ? and ? or
					(ap.paymentdate is null
					and duedate between ? and ?))
					and ap.unit = ?
					order by 3 asc, aggregateId desc, aggregateValue desc, ap.id
					"""
				toResponse(SQL_REPORT.format(account),
					List(AuthUtil.company.id.is, start, end, 
						start, end, AuthUtil.unit.id.is))
			}
			case "report" :: "account_ofx_conciliation" :: Nil Post _ => {

				def account_ofx:String = S.param("account_ofx") match {
					case Full(s) if(s != "") => " and ap.account = %s ".format (s)
					case _ => " and 1 = 1 " 
				}

				def account_fin:String = S.param("account_fin") match {
					case Full(s) if(s != "") => " and ap1.account = %s ".format (s)
					case _ => " and 1 = 1 " 
				}

				val show_conciliated:String = S.param("show_conciliated") match {
					case Full(p) if(p != "")=> " and 1 = 1 "
					case _ => " and ap1.conciliate = 0 "
				}			

				val verunit:String = S.param("verunit") match {
					case Full(p) if(p != "")=> " and ap1.unit = ap.unit "
					case _ => " and 1 = 1 "
				}			

				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def margin:Double = S.param("margin") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => 0;
				}
				def days:Int = S.param("days") match {
					case Full(p) if(p != "") => p.toInt
					case _ => 0;
				}
				def obsFilterofx = if (S.param("obs_search_ofx") != "") {
					(" and lower (ap.obs) like '%"+(S.param("obs_search_ofx") openOr "")+"%' ").toLowerCase;
				} else {
					" and 1 = 1 "
				}

				def obsFilterfin = if (S.param("obs_search_fin") != "") {
					(" and lower (ap1.obs) like '%"+(S.param("obs_search_fin") openOr "")+"%' ").toLowerCase;
				} else {
					" and 1 = 1 "
				}

				val payment_type_param_name = S.param("payment_type[]") match {
							case Full(p) => "payment_type[]"
							case _ => "payment_type"
				}
				def payment_type:String = S.param(payment_type_param_name) match {
					case Full(s) if(s != "") => " and ap1.paymenttype in " +
					"(%s) ".format(S.params(payment_type_param_name).foldLeft("0")(_+","+_))
					case _ => " and 1=1 "
				}

				lazy val SQL_REPORT = """
					select ap.duedate, ap.obs, ap.typemovement, 
					ap.value, ap.id, 
					trim (coalesce(ap1.aggregateLabel,'') || ' ' || ap1.obs), 
					ap1.value, ap1.duedate, 
					ap1.id,
					ap1.aggregatevalue, ap1.aggregateid,
					ap1.conciliate,
					ap.category, ap1.category
					from accountpayable ap 
					left join accountpayable ap1 on 
-- alterado 06/03/2018					
--					  ((ap1.paymentdate = ap.paymentdate or ap1.duedate = ap.paymentdate 
--					  or ap1.paymentdate = ap.duedate or ap1.duedate = ap.duedate) 
--					  and (ap1.value = ap.value or (ap1.aggregatevalue > (ap.value * ((100-?)/100)) and ap1.aggregatevalue < (ap.value * ((100+?)/100))))
--					  or (ap1.duedate between date(ap.duedate-?) and date (ap.duedate+?) and
					  --or (ap1.duedate between date(?) and date (?) and
--					     (ap1.value = ap.value or (ap1.aggregatevalue > (ap.value * ((100-?)/100)) and ap1.aggregatevalue < (ap.value * ((100+?)/100))))))
					  (
					  ((ap1.duedate between date(ap.duedate-?) and date (ap.duedate+?)) or
					  ap1.paymentdate = ap.duedate) and
					  ((ap1.value > (ap.value * ((100.0-?)/100.0)) and ap1.value < (ap.value * ((100.0+?)/100.0))
					  or (ap1.aggregatevalue > (ap.value * ((100.0-?)/100.0)) and ap1.aggregatevalue < (ap.value * ((100.0+?)/100.0))))) 
					  )
					  and ap1.toconciliation = false and ap.company = ap1.company
					  and ap1.typemovement = ap.typemovement
					  %s
					  %s
					  %s
					where ap.company = ? 
					and ap.duedate between date(?) and date (?)
					and ap.toconciliation = true
					and ap.unit = ?
					%s
					%s
					%s
					%s
					order by 
					ap.duedate, ap.id, ap1.duedate, (ap1.value = ap.value)
					"""
				toResponse(SQL_REPORT.format(verunit, account_fin, show_conciliated, 
					account_ofx,payment_type, 
					obsFilterofx, obsFilterfin),
					List(days, days, margin, margin, 
						margin, margin, 
						AuthUtil.company.id.is, start, end,
						AuthUtil.unit.id.is))
			}
			case "report" :: "offsaleproduct_cost" :: Nil Post _ =>{
				val offsales_param_name = S.param("offsales[]") match {
					case Full(p) => "offsales[]"
					case _ => "offsales"
				}
				
				def offsales:String = S.param(offsales_param_name) match {
					case Full(p) if(p!="") => " op.offsale in ("+S.params(offsales_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
					case _ => " 1=1 "
				}

				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " ic.unit =%S ".format(p) 
					case _ => " 1=1"
				}			

				val SQL_REPORT = """
					select os.short_name as convenio, pr.name as Produto, 

					ic.averageprice as "CTM", ic.averageindic1 as "Frete", 
					ic.averageindic2 as "Dif ICMS", ic.averageindic3 as "IPI", 

					round (((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /*+ 
					op.indic1 + op.indic2 Embalagem e armazenagem nao entram na perda*/)/100 * ic.averageindic4),2) as "Perda",

					op.indic1 as "Armazenagem", op.indic2 as "Embalagem", 

					round((((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 + 
					op.indic1 + op.indic2 ) + ((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /*+ 
					op.indic1 + op.indic2 */)/100 * ic.averageindic4))/100 * op.indic3),2) as "Markup",

					round (((ic.averageprice + ic.averageindic1 /* Frete */ + 
					ic.averageindic2 /* ICMS */ + ic.averageindic3 /* IPI */ +
					op.indic1 /* Armazenagem */ + op.indic2 /* Embalagem */ +
					round (((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /* + 
					op.indic1 + op.indic2 */)/100 * ic.averageindic4),2) /* perda */ +
					round((((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 + 
					op.indic1 + op.indic2 ) + ((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /*+ 
					op.indic1 + op.indic2 */)/100 * ic.averageindic4))/100 * op.indic3),2) /* Markup */
					)/100 * op.indic4),2) as "Simples+Cartão",
					ic.averageindic5, 
					/* total */
					ic.averageprice /* CTM */ + ic.averageindic1 /* Frete */ +
					ic.averageindic2 /* "Dif ICMS */ + ic.averageindic3 /* IPI */ + 
					op.indic1 /* Armazenagem */ + op.indic2 /* Embalagem */ +

					round (((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /*+ 
					op.indic1 + op.indic2 */)/100 * ic.averageindic4),2) /* Perda */ +

					round((((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 + 
					op.indic1 + op.indic2 ) + ((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /* + 
					op.indic1 + op.indic2 */ )/100 * ic.averageindic4))/100 * op.indic3),2) /* "Markup" */ +

					round (((ic.averageprice + ic.averageindic1 /* Frete */ + 
					ic.averageindic2 /* ICMS */ + ic.averageindic3 /* IPI */ +
					op.indic1 /* Armazenagem */ + op.indic2 /* Embalagem */ +
					round (((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 /* + 
					op.indic1 + op.indic2 */)/100 * ic.averageindic4),2) /* perda */ +
					round((((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 + 
					op.indic1 + op.indic2 ) + ((ic.averageprice + ic.averageindic1 + ic.averageindic2 + ic.averageindic3 + 
					op.indic1 + op.indic2 )/100 * ic.averageindic4))/100 * op.indic3),2) /* Markup */
					)/100 * op.indic4),2) /* Simples+Cartão */ +
					ic.averageindic5 as "Sugerido", 

					op.offprice as "Efetivo", op.id
					from offsaleproduct op
					inner join product pr on pr.id = op.product
					inner join offsale os on os.id = op.offsale
					left join inventorycurrent ic on ic.product = pr.id
					where op.company = ?
					and %s
					and %s
					order by 1,2
				"""
				toResponse(SQL_REPORT.format(offsales, unit),List(AuthUtil.company.id.is)) 
			} 

		}
}