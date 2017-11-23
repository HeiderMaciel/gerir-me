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


object Reports extends RestHelper with ReportRest with net.liftweb.common.Logger {
	
/*	def listUserProf = S.params("user")
	def userProf = {
			if(listUserProf.size > 0) {
				val listParm = listUserProf.foldLeft("")(_+_)
				" id in(select distinct customer from treatment t where t.company=%s and t.user_c in(%s))".format(AuthUtil.company.id.is.toString, listParm)
			} else {
				"1=1"
			}
		}
*/
		//inventory_movements.jrxml
		val reportFile = "/reports/inventory_movements.jasper"
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
		serve {
			case "report" :: "teste" :: Nil Get _ => {
				val params = new HashMap[String, Object]();
	            params.put("company",8.toLong.asInstanceOf[Object]);
	            val path = getClass.getClassLoader().getResource(reportFile).getFile()
	           	val bytes = JasperRunManager.runReportToPdf(path, params, cn);
					OutputStreamResponse( (out) => out.write(bytes) )
			}
/*
			case "report" :: "inventory_current" :: Nil Get _ => {
				val reportFileId = "/reports/EbelleReports/current_inventory.jasper"
				val params = new HashMap[String, Object]();
	            params.put("company",AuthUtil.company.id.is.asInstanceOf[Object]);
	            val path = getClass.getClassLoader().getResource(reportFileId).getFile()
	           	val bytes = JasperRunManager.runReportToPdf(path, params, cn);
				OutputStreamResponse( (out) => out.write(bytes) )
			}

			case "report" :: "inventory_distrib" :: Nil Get _ => {
				val reportFileId = "/reports/EbelleReports/inventory_distr.jasper"
				val params = new HashMap[String, Object]();
	            params.put("company",AuthUtil.company.id.is.asInstanceOf[Object]);
	            val path = getClass.getClassLoader().getResource(reportFileId).getFile()
	           	val bytes = JasperRunManager.runReportToPdf(path, params, cn);
					OutputStreamResponse( (out) => out.write(bytes) )
			}
*/
			case "report" :: "prof_services" :: Nil Post _ => {
				def prod = 	 S.param("product") match {
					case Full(p) if(p != "")=> "pr.id in(%s)".format(p)
					case _ => S.param("product[]") match {
						case Full(p) if(p != "") => "pr.id in(%s)".format(S.params("product[]").foldLeft("0")(_+","+_))
						case _ => " 1=1 " 
					}
				}
				def user = S.param("user") match {
					case Full(p) if(p != "")=> "bp.id in(%s)".format(p)
					case _ => S.param("user[]") match {
						case Full(p) if(p != "") => "bp.id in(%s)".format(S.params("user[]").foldLeft("0")(_+","+_))
						case _ => " 1=1 " 
					}
				}			
				val sql = """
						select
						pr.name , pr.duration, ua.duration, pr.saleprice , ua.price, 
						pr.commission, ua.commission, pr.commissionAbs, ua.commissionAbs, bp.short_name Profissional
						from product pr
						inner join useractivity ua on ua.activity = pr.id
						inner join business_pattern bp on bp.id = ua.user_c and bp.status = 1
						left join producttype pt on pt.id = pr.typeproduct
						where  pr.company = ? and ua.company=pr.company and pr.status = 1 and bp.userStatus = 1 
						and pr.productclass = 0 and %s and  %s
						order by 1,5				
							""".format(prod,user); //, prod,user);
//				info (prod)
				toResponse(sql,List(AuthUtil.company.id.is)) //, AuthUtil.company.id.is))
/*						union
						select
						pr.name , pr.duration, ua.duration, pr.saleprice , ua.price, 
						pr.commission, ua.commission,  bp.short_name Profissional
						from product pr
						left join useractivity ua on ua.producttype = pr.typeproduct and ua.activity is null and ua.company = pr.company
						inner join business_pattern bp on bp.id = ua.user_c and bp.status = 1
						inner join producttype pt on pt.id = ua.producttype
						where  pr.company = ?
						and pr.status = 1
						and bp.userStatus = 1
						and pr.productclass = 0 and %s and  %s
*/
			}
			case "report" :: "fat_period" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}						
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and ca.unit =%S".format(p) 
					case _ => ""
				}			
				val sql = """select date,sum(price) from (
					select pd.value as price, date_part ('year', pa.datepayment)||'/'|| to_char (pa.datepayment, 'MM') as date
					from payment pa
					inner join paymentdetail pd on (pd.payment = pa.id)
					inner join cashier ca on (ca.id = pa.cashier)
					inner join paymenttype pt on (pt.id = pd.typepayment and 
					(pt.receive = true or pt.receiveatsight = true))
					where pa.company = ? and pa.datepayment between ? and ? %s
					) as data group by date order by date""";
				toResponse(sql.format(unit),List(AuthUtil.company.id.is,start,end))
			}

			case "report" :: "prof_ranking" :: Nil Post _ => {
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
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
					case _ => " and " + Treatment.unitsToShowSql
				}			
				def maxprof:Int = S.param("maxprof") match {
					case Full(p) => p.toInt
					case _ => 20
				}

				val sql = """select row_number () OVER (ORDER BY valor_total desc) AS pos, * from (
					select * from (select bp.name profissional , count(td.price) quantidade, sum(td.price) valor_total,
					trim (bp.mobile_phone || ' ' || bp.phone || ' ' || bp.email_alternative) as telefone , bp.email as email, bp.id as idp	
					from treatment tr
					inner join treatmentdetail td on (td.treatment = tr.id and td.company = tr.company and td.price <> 0)
					left join product pr on (pr.id = td.product or pr.id = td.activity)
					inner join business_pattern bp on (bp.id = tr.user_c and bp.company = tr.company)
					inner join payment p on (p.id = tr.payment)
					where 
					tr.company = ? and p.datepayment between ? and ? 
		            and pr.productclass in(%s)
					%s
					and p.deleted = false
					group by profissional, telefone, email, idp
					order by profissional, telefone, email, idp) as data1 order by valor_total desc limit ?) as data2
					""";
				toResponse(sql.format(productclass,unit),List(AuthUtil.company.id.is,start,end, maxprof))
			}
			case "report" :: "prof_ranking_csv.csv" :: Nil Post _ => {
				def start:Date = S.param("start") match { 
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				val sql = """select bp.name profissional , sum(td.price) valor_total, count(td.price) quantidade from treatment tr, treatmentdetail td, business_pattern bp, payment p
							where 
							p.id = tr.payment and td.treatment = tr.id
							and bp.id = tr.user_c and
							tr.company = ? and p.datepayment between ? and ? 
							group by profissional
							order by profissional
							""";
				val data: Array[Byte] = toCsv(sql,List(AuthUtil.company.id.is,start,end)).getBytes// get your data here
				val headers =  ("Content-type" -> "text/csv") ::  ("Content-length" -> data.length.toString) :: ("Content-disposition" -> "attachment; filname=download.csv") :: Nil
				StreamingResponse(
				  new java.io.ByteArrayInputStream(data),
				  () => {},
				  data.length, 
				  headers, Nil, 200)
			}			
			case "report" :: "product_participation" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}						

//								sum (td.amount) qtde_total,

				val sql = """select * from (
								select 
								pr.name produto,
								sum (td.price) valor_total,
								sum (td.amount) qtde_total
								from treatment tr, treatmentdetail td, product pr, payment p
								where 
								tr.company = ? and
								td.treatment = tr.id and pr.id = td.product and 
								p.deleted = false and
								p.id = tr.payment and p.datepayment between ? and ?
								and pr.productclass in(1)
								group by pr.name	
							) as data
							where valor_total>0
							order by valor_total desc
							""";
				toResponse(sql,List(AuthUtil.company.id.is,start,end))
			}
			case "report" :: "product_ranking" :: Nil Post _ => {
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

				val sql = """select * from (select pr.name produto, sum (td.amount) quantidade, sum (td.price) valor_total
							from treatment tr
							inner join treatmentdetail td on (td.treatment = tr.id and td.company = tr.company)
							inner join payment p on (p.id = tr.payment and p.deleted = false)
							inner join product pr on (pr.id = td.product and pr.company = td.company and pr.productclass in(1)) 
							where 
							tr.company = ? and
							td.treatment = tr.id and pr.id = td.product and 
					        p.datepayment between ? and ? %s
						    and td.price <> 0
							group by pr.name
							order by pr.name) as data1 order by valor_total desc
							""";
				toResponse(sql.format(unit),List(AuthUtil.company.id.is,start,end))
			}
			case "report" :: "geral" :: Nil Post _ => {
				toResponse(Company.SQL_REPORT_DATA,List(AuthUtil.company.id.is))
			}			
			case "report" :: "EB_rel_produtos_c_desconto" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def cashier = S.param("cashier") match {
					case Full(p) if(p !="") => "pa.cashier = "+p.toLong
					case _ => "1 =1"
				}
				def productclass = S.param("productclass") match {
					case Full(p) if(p !="") => "pd.productclass in ( "+p+" )"
					case _ => "1 =1"
				}
				 val sql = """
					 select * from (
					select c.idForcompany, pa.command,pa.datepayment data_venda, pd.name produto , pd.saleprice preco_original, 
					round (td.price / td.amount,2) preco_venda, 
					round ((td.price/td.amount)-pd.saleprice,2) diferenca_unit, round ((td.price)-(pd.saleprice*td.amount),2) diferenca_total, round (((td.price/td.amount)-pd.saleprice)*100/pd.saleprice,2) percentual, td.amount quantidade, bp.name cliente_profissional, 
					bp.is_employee from treatment tr
					inner join treatmentdetail td on td.treatment = tr.id
					inner join payment pa on pa.id = tr.payment
					inner join product pd on (td.product = pd.id or td.activity=pd.id)
					inner join business_pattern bp on tr.customer = bp.id
					inner join cashier c on(c.id = pa.cashier)
					where pd.saleprice <> 0 and td.amount <> 0 and round ((td.price/td.amount)-pd.saleprice,2) <> 0 and td.price <> 0 
					and pd.company = ?
					and pa.datePayment between date(?) and date(?)
					and %s
					and %s
				) as data1 order by data_venda desc, cliente_profissional limit 500
				 """
				toResponse(sql.format(cashier, productclass),List(AuthUtil.company.id.is, start, end))
			}
			case "report" :: "commissions_filter" :: Nil Post _ => {
				def treatment = S.param("treatment") match {
					case Full(p) if(p != "") => " td.treatment = "+p
					case _ => "1 = 1"
				}
				def payment = S.param("payment") match {
					case Full(p) if(p !="") =>  " payment.id = "+p
					case _ => "1 = 1"
				}				
				lazy val SQL_REPORT_WITH_USER = """
					select
					prof.name, 
					cashier.idforcompany as cashier, 
					payment.command, 
					payment.datepayment,
					co.payment_date,
					customer.name as customer,
					p.name product,
					td.price,
					co.value,
					pt.name as paymenttype
					from commision co
					inner join payment on(payment.id = co.payment)
					left join cashier on(cashier.id = payment.cashier)
					inner join paymentdetail pd on(pd.id = co.payment_detail)
					inner join paymenttype pt on(pt.id=pd.typepayment)
					inner join business_pattern prof on(prof.id = co.user_c)
					inner join business_pattern customer on(customer.id = payment.customer)
					inner join treatmentdetail  td  on(td.id = co.treatment_detail)
                    inner join treatment tr on tr.id = td.treatment and tr.user_c = co.user_c
					inner join product p on(p.id = td.product or p.id = td.activity)
					where co.company = ? and %s
					order by datepayment desc;"""  

					//                          (pd.commisionnotprocessed) as open,
					//                          COALESCE((select sum(c2.value) from commision c2 where  c2.payment_date > co.payment_date and c2.payment_detail = co.payment_detail ),0.00) as paid,

				val where  = treatment+" and "+payment
				val sql = SQL_REPORT_WITH_USER.format(where)
				// info(sql)
				toResponse(sql,List(AuthUtil.company.id.is))
			}
			case "report" :: "commissions_fat" :: Nil Post _ => {

/*				val user_param_name = S.param("user[]") match {
					case Full(p) => "user[]"
					case _ => "user"
				}
				def user = S.param(user_param_name) match {
					case Full(p) if(p != "")=> "%s".format(p)
					case _ => S.param(user_param_name) match {
						case Full(p) if(p != "") => "%s".format(S.params(user_param_name).foldLeft("0")(_+","+_))
						case _ => " 01 " 
					}
				}			
*/
				val user_param_name = S.param("user[]") match {
					case Full(p) => "user[]"
					case _ => "user"
				}

				def user = S.param(user_param_name) match {
					case Full(p) if(p != "")=> "%s".format(p)
					case _ => S.params(user_param_name) match {
						case primeiro :: r if(primeiro == "") => {
							info(r)
							"%s".format(r.mkString(","))
						}
						case primeiro :: resto => primeiro
						case _ => " 01 " 
					}
				}			
				def productclass:String = S.param("productclass") match {
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
				def units:String = S.param("unit") match {
					case Full(s) if(s != "") => " and tr.unit = %s".format(s)
					case _ => " and " + Treatment.unitsToShowSql
				}
				toResponse("""select sum (td.price)
								from treatment tr 
								inner join payment pa on pa.id = tr.payment
								inner join treatmentdetail td on td.treatment = tr.id
								inner join product p on(p.id = td.product or p.id = td.activity)
								where tr.company = ? 
								and tr.user_c in (%s)
								and p.productclass in(%s) %s
								and date(pa.datepayment) between date(?) and date(?)
								""".format(user, productclass, units),List(AuthUtil.company.id.is, start, end))
			}

			case "report" :: "commissions" :: Nil Post _ => {
/*
				def user = S.param("user") match {
					case Full(p) => p.toLong
					case _ => 0l
				}
*/				
// REVER hora vai 145 ora vai ,145 o list de vales e o total faturado tb não funcionam com mais de 1
				val user_param_name = S.param("user[]") match {
					case Full(p) => "user[]"
					case _ => "user"
				}

				def user = S.param(user_param_name) match {
					case Full(p) if(p != "")=> "%s".format(p)
					case _ => S.params(user_param_name) match {
						case primeiro :: r if(primeiro == "") => {
							info(r)
							"%s".format(r.mkString(","))
						}
						case primeiro :: resto => primeiro
						case _ => " 01 " 
					}
				}			
				def units:String = S.param("unit") match {
					case Full(s) if(s != "") => " and tr.unit = %s".format(s)
					case _ => " and " + Treatment.unitsToShowSql
				}

				def productclass:String = S.param("productclass") match {
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
				val rel_mini:Long = S.param("rel_mini") match {
					case Full(p) if(p != "")=> 1
					case _ => 0
				}			
				lazy val SQL_REPORT = """
                          select
                          ca.idforcompany as cashier, 
                          pa.command, 
                          pa.datepayment,
                          co.payment_date,
					      cu.short_name,
                          customer.short_name as customer,
                          p.short_name product,
                          td.price,
                          co.value,
                          ba.short_name,
                          pt.short_name as paymenttype,
                          customer.id,
                          td.activity,
                          td.product
                          from commision co
                          inner join payment pa on(pa.id = co.payment)
                          left join cashier ca on(ca.id = pa.cashier)
                          inner join paymentdetail pd on(pd.id = co.payment_detail)
                          inner join paymenttype pt on(pt.id=pd.typepayment)
                          inner join business_pattern prof on(prof.id = co.user_c)
                          inner join business_pattern customer on(customer.id = pa.customer)
                          inner join treatmentdetail  td  on(td.id = co.treatment_detail)
                          inner join treatment tr on tr.id = td.treatment and 
							(tr.user_c = co.user_c or co.user_c = td.auxiliar or 
							tr.user_c in (select bs.id from business_pattern bs where bs.company = co.company and
							bs.parent = co.user_c))
                          inner join companyunit cu on cu.id = tr.unit
                          inner join product p on(p.id = td.product or p.id = td.activity)
                          left  join business_pattern ba on ba.id = td.auxiliar
                          where co.company = ? and co.user_c in (%s) and date(co.payment_date) between date(?) and date(?) 
                          and p.productclass in(%s) %s
                          order by datepayment desc, pa.command, customer.short_name, p.short_name;"""
					lazy val SQL_REPORT_MINI = """
					        select
					        ca.idforcompany as cashier, 
					        pa.command, 
					        pa.datepayment,
					        --pa.datepayment,
					        co.payment_date,
					        cu.short_name,
					        customer.short_name as customer,
					        p.short_name product,
					        td.price,
					        sum(co.value),
							ba.short_name,
					        pa.detailPaymentAsText as paymenttype,
					        --(select min (name)||'*' from paymenttype pt where pd.typepayment = pt.id)
					        customer.id,
	                        td.activity,
	                        td.product
					        from commision co
					        inner join payment pa on(pa.id = co.payment)
					        left join cashier ca on(ca.id = pa.cashier)
					        inner join paymentdetail pd on(pd.id = co.payment_detail)
					        inner join paymenttype pt on(pt.id=pd.typepayment)
					        inner join business_pattern prof on(prof.id = co.user_c)
					        inner join business_pattern customer on(customer.id = pa.customer)
					        inner join treatmentdetail  td  on(td.id = co.treatment_detail)
                            inner join treatment tr on tr.id = td.treatment and 
								(tr.user_c = co.user_c or co.user_c = td.auxiliar or 
								tr.user_c in (select bs.id from business_pattern bs where bs.company = co.company and
								bs.parent = co.user_c))
	                        inner join companyunit cu on cu.id = tr.unit
					        inner join product p on(p.id = td.product or p.id = td.activity)
	                        left  join business_pattern ba on ba.id = td.auxiliar
					        where co.company = ? and co.user_c in (%s) 
					        and date(co.payment_date) between date(?) and date(?)
					        and p.productclass in(%s) %s
					        group by  td.treatment, ca.idforcompany, pa.command, 
					        pa.datepayment, pa.detailPaymentAsText,
					        cu.short_name,customer.short_name, customer.id, td.price,ba.short_name,p.short_name, co.payment_date, td.product, td.activity
					        order by datepayment desc, pa.command, pa.detailPaymentAsText, cu.short_name, customer.short_name, customer.id, p.short_name, co.payment_date;  
					"""
				if(rel_mini == 0){
					//info (user + " = = = = = = = = = = = = = = = =  = = == = = = = = = = = =")
					toResponse(SQL_REPORT.format(user, productclass, units),List(AuthUtil.company.id.is, start, end))
				} else {
					toResponse(SQL_REPORT_MINI.format(user, productclass, units),List(AuthUtil.company.id.is, start, end))
				}
			}

			case "report" :: "sales_and_commission" :: Nil Post _=> {
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
				def productclass:String = S.param("productclass") match {
					case Full(p) => p
					case _ => "0,1";
				}
				def dttypes:String = S.param("dttype") match {
					case Full(p) => p
					case _ => "0"; // vencimento
				}

		        val sqldt : String = if (dttypes == "1") { // competencia
		            " and date(exerciseDate) between date(?) and date(?) "
		          } else if (dttypes == "2") { // pagamento
		            " and date(paymentDate) between date(?) and date(?) "
		          } else { // 0 vencimento
		            " and date(dueDate) between date(?) and date(?) "
		          }

				val SQL = """
					select id, prof, telefone, email, unidade, sum (preco), sum (comissao), 
					(select sum (case when typemovement = 1 then value * -1 when typemovement = 0 then value end)
					from accountpayable where paid = true """ + sqldt + """
					and toconciliation = false
					and company = ?
					and user_c = data1.id
					) as vales,
					id from (
					select bp.id as id, bp.name as prof, trim (mobile_phone || ' ' || phone || ' ' || email_alternative) as telefone, bp.email as email,
					cu.short_name as unidade, td.price as preco, 
					(select sum (co.value) from commision co where co.company = tr.company 
						and co.treatment_detail = td.id
						and co.due_date between date(?) and date(?)
						and (co.user_c = bp.id)
					) as comissao 
					from treatment tr
					inner join treatmentdetail td on td.treatment = tr.id
					left join business_pattern bp on (bp.id = tr.user_c or bp.id = td.auxiliar)
					inner join product pr on td.activity = pr.id or td.product = pr.id
					left join companyunit cu on cu.id = bp.unit
					where tr.dateevent between date(?) and date(?)
					and tr.status = 4
					and tr.company = ?
					and pr.productclass in (%s)
					%s
					%s
					order by bp.name, cu.name) as data1
					group by prof, id, telefone, email, unidade
					order by prof, id, telefone, email, unidade
					"""
					//LogActor ! SQL
				toResponse(SQL.format(productclass,unit,user),List(start, end, AuthUtil.company.id.is, start, end, start, end, AuthUtil.company.id.is))
			}

			case "report" :: "sales_purchase_margin" :: Nil Post _=> {
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
				def productclass:String = S.param("productclass") match {
					case Full(p) => p
					case _ => "0,1";
				}
				def margin_value:Double = S.param("margin_value") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => 0.1;
				}
				val SQL = """
					select * from (select pr.id, pr.name, sum((pr.purchaseprice * amount)) as compra, sum ((pr.purchaseprice * amount * ?)) as margem, sum (td.price) as venda, sum (td.amount), 
					sum (td.price - ((pr.purchaseprice * amount) + (pr.purchaseprice * amount * ?))) as lucro from treatment tr
					inner join treatmentdetail td on td.treatment = tr.id
					left join business_pattern bp on bp.id = tr.user_c
					inner join product pr on pr.id = td.product and pr.productclass in (%s)
					inner join producttype pt on pt.id = pr.typeproduct --and pt.typeclass in (1)
					where tr.company = ? and tr.dateevent between (?) and (?)
					%s
					%s
					and tr.status = 4 
					group by pr.id, pr.name
					order by pr.id, pr.name) as data1 
					order by lucro desc
					"""
					//LogActor ! SQL
				toResponse(SQL.format(productclass,unit,user),List(margin_value, margin_value, AuthUtil.company.id.is, start, end))
			}

			case "report" :: "dre" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				toResponse(AccountPayable.SQL_DRE,List(start, end, start, end, AuthUtil.company.id.is))
			}

			case "report" :: "dre_tree" :: Nil Post _ => {
				def dttypes:String = S.param("dttype") match {
					case Full(p) => p
					case _ => "0"; // vencimento
				}
				val accounts_param_name = S.param("accounts[]") match {
					case Full(p) => "accounts[]"
					case _ => "accounts"
				}
				
				def accounts:String = S.param(accounts_param_name) match {
					case Full(p) if(p!="") => " ac.id in ("+S.params(accounts_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
					case _ => " 1=1 "
				}
				val costcenters_param_name = S.param("costcenters[]") match {
					case Full(p) => "costcenters[]"
					case _ => "costcenters"
				}
				
				def costcenters:String = S.param(costcenters_param_name) match {
					case Full(p) if(p!="") => " ap.costcenter in ("+S.params(costcenters_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
					case _ => " 1=1 "
				}
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " ap.unit =%S ".format(p) 
					case _ => " 1=1"
				}			
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def idsToshow:List[Long] = S.param("ids") match {
					case Full(ids) => ids.split(",").toList.map(_.trim.toLong)
					case _ => Nil
				}

				def idsToshowStr = idsToshow.map(_.toString).reduceLeft(_+" , "+_)
		        val sqldt : String = if (dttypes == "1") { // competencia
		            " and date(ap.exerciseDate) between date(?) and date(?) "
		          } else if (dttypes == "2") { // pagamento
		            " and date(ap.paymentDate) between date(?) and date(?) "
		          } else { // 0 vencimento
		            " and date(ap.dueDate) between date(?) and date(?) "
		          }
				val SQL_DRE_TREE = """
				select * from (
				    select id,name, mintreenode, maxtreenode, parentaccount,isparent,
				(
				(
				  select COALESCE(sum(value),0) as total    
				  from accountpayable ap where 
				  ap.toconciliation = false and
				  ap.company=? and 
				  ap.category in (
				      select id from 
				      accountcategory acc 
				      where acc.company=? and acc.mintreenode between ac.mintreenode and ac.maxtreenode
				      )
				      and ap.paid=true and ap.typemovement=0 """ + sqldt + """
				      and (%s) and (%s)
				) 
				-
				(
				  select COALESCE(sum(value),0) as total    
				  from accountpayable ap where 
				  ap.toconciliation = false and
				  ap.company=? and 
				  ap.category in (
				      select id from 
				      accountcategory acc 
				      where acc.company=? and acc.mintreenode between ac.mintreenode and ac.maxtreenode
				      )
				      and ap.paid=true and ap.typemovement=1 """ + sqldt + """
				      and (%s) and (%s)
				)
				)
				as total
				from 
				accountcategory ac
				where company=? and (%s)
				order by maxtreenode desc,orderinreport
				) as data where total <>0
				"""

				if(idsToshow.isEmpty) {
					toResponse(SQL_DRE_TREE.format(unit, costcenters, unit, costcenters, accounts),List(AuthUtil.company.id.is, AuthUtil.company.id.is, start, end, AuthUtil.company.id.is, AuthUtil.company.id.is,start, end, AuthUtil.company.id.is));
				} else  {
					toResponse(AccountPayable.SQL_DRE_TREE_WITHID.format(sqldt, idsToshowStr, 
					sqldt, idsToshowStr),List(AuthUtil.company.id.is, AuthUtil.company.id.is, start, end, AuthUtil.company.id.is, AuthUtil.company.id.is,start, end, AuthUtil.company.id.is))
				}

			}

			case "report" :: "costcenters_tree" :: Nil Post _ => {
				toResponse(CostCenter.SQL_TREE,List(AuthUtil.company.id.is))
			}
			case "report" :: "accounts_tree" :: Nil Post _ => {
				toResponse(AccountPayable.SQL_TREE,List(AuthUtil.company.id.is))
			}			

			case "report" :: "cashiers_payment_types" :: Nil Post _ =>{
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def unit:Long = S.param("units") match {
					case Full(p) if(p!="") => p.toLong
					case _ => 0
				}
				def paymentTypesWhere = filterSqlIn("payment_type", " t.id in(%s)")
				def searchByParams(cashier:String) = {
					S.param("cashier") match {
						case Full(p) if(p !="") => toResponse(Cashier.SQL_REPORT_PAYMENT_TYPES.format(paymentTypesWhere),List(AuthUtil.company.id.is, cashier.toLong))
						case _ => {
							if(unit != 0)
								toResponse(Cashier.SQL_REPORT_PAYMENT_TYPES_UNIT.format(paymentTypesWhere),List(unit, start, end))
							else
								toResponse(Cashier.SQL_REPORT_PAYMENT_TYPES_COMPANY.format(paymentTypesWhere),List(AuthUtil.company.id.is, start, end))
						}
					}					
				}
				val cashier = S.param("cashier") openOr "0";
				S.param("isIdForCompany") match {
					case Full(p) if(p !="" && cashier != "") => {
						searchByParams(Cashier.findOpenCashierByIdAndCompany(cashier.toInt).id.is.toString)
					}
					case _ => {
						searchByParams(S.param("cashier") openOr "0")
					}
				}
			}
			case "report" :: "paymenttype_details" :: fat :: Nil Post _ =>{
				def start:Date = S.param("startDate") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}

				def end:Date = S.param("endDate") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				
				def unit:String = S.param("unit") match {
					case Full(p) if(p!="") => " ca.unit = %s ".format (p)
					case _ => " 1=1 "
				}
				def commands:String = S.param("commands") match {
					case Full(s) if(s != "") => " pa.command in ('" + s.replace (",", "','") + "')"
					case _ => " 1=1 ";
				}	

				val parmcashier = S.param("cashier") openOr "0";
				def cashier:String = S.param("isIdForCompany") match {
					case Full(p) if(p !="" && parmcashier != "") => {
						" pa.cashier = %s ".format (
							Cashier.
							findOpenCashierByIdAndCompany(parmcashier.toInt).id.is.toString);
					}
					case _ => {
						S.param("cashier") match {
						case Full(p) if(p!="") => " pa.cashier = %s ".format (p)
						case _ => " 1=1 "
						}
					}
				}
				def paymentTypesWhere = filterSqlIn("payment_type", " pt.id in(%s)")
				def paymentTypesFatWhere = filterSqlIn("payment_type", " pd.typepayment in(%s)")
				val SQL = """
					select bc.id, bc.name as cliente, 
					ca.idforcompany as caixa, pa.command as comanda, 
					pa.value as total, pd.value, pt.name as forma, pa.datepayment as pagamento, 
					pd.duedate, cu.name as unidade,
					bu.short_name || ' ' || to_char (pd.createdat,'dd/mm/yy hh24:mi' )
					from payment pa
					inner join paymentdetail pd on pa.id = pd.payment
					inner join business_pattern bu on bu.id = pd.createdby
					inner join paymenttype pt on pt.id = pd.typepayment
					inner join cashier ca on ca.id = pa.cashier
					inner join business_pattern bc on bc.id = pa.customer
					inner join companyunit cu on cu.id = ca.unit
					where pa.company = ? 
					and pa.datepayment between ? and ?
					and %s
					and %s
					and %s
					and %s
					order by pa.datepayment, ca.idforcompany, pd.duedate, bc.name, pa.command, pt.name
				"""
				val sqlFat = """
					select sum (
					pa.value) as total, count (pa.value) as count
					from payment pa
					inner join cashier ca on ca.id = pa.cashier
					inner join business_pattern bc on bc.id = pa.customer
					inner join companyunit cu on cu.id = ca.unit
					where pa.company = ?
					and pa.datepayment between ? and ?
					and pa.id in (select pd.payment from paymentdetail pd where pd.payment = pa.id and %s)
					and %s
					and %s
					and %s
				"""
				if (fat == "false") {
					toResponse(SQL.format(paymentTypesWhere, unit, cashier, commands),List(AuthUtil.company.id.is, start, end)) 
				} else {
					toResponse(sqlFat.format(paymentTypesFatWhere, unit, cashier, commands),List(AuthUtil.company.id.is, start, end)) 
				}
			}
			case "report" :: "accountpayable" :: Nil Post _ =>{
				def dttypes:String = S.param("dttype") match {
					case Full(p) => p
					case _ => "0"; // vencimento
				}
				val accounts_param_name = S.param("accounts[]") match {
					case Full(p) => "accounts[]"
					case _ => "accounts"
				}
				
				def accounts:String = S.param(accounts_param_name) match {
					case Full(p) if(p!="") => " ac.id in ("+S.params(accounts_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
					case _ => " 1=1 "
				}
				val costcenters_param_name = S.param("costcenters[]") match {
					case Full(p) => "costcenters[]"
					case _ => "costcenters"
				}
				
				def costcenters:String = S.param(costcenters_param_name) match {
					case Full(p) if(p!="") => " ap.costcenter in ("+S.params(costcenters_param_name).filter(_ != "").map(_.toLong).map(_.toString).reduceLeft(_+","+_)+")"
					case _ => " 1=1 "
				}
				def unit:String = S.param("unit") match {
					case Full(p) if(p != "") => " ap.unit =%S ".format(p) 
					case _ => " 1=1"
				}			
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def inverse:Boolean = S.param("inverse") match {
					case Full(p) if(p!="") => true
					case _ => false
				}
		        val sqldt : String = if (dttypes == "1") { // competencia
		            " and date(ap.exerciseDate) between date(dates.start_of_month) and date(dates.end_of_month) "
		          } else if (dttypes == "2") { // pagamento
		            " and date(ap.paymentDate) between date(dates.start_of_month) and date(dates.end_of_month) "
		          } else { // 0 vencimento
		            " and date(ap.duedate) between date(dates.start_of_month) and date(dates.end_of_month) "
		          }
				val SQL_REPORT_ACCOUNTPAYABLE = """
				select * from (
				    select dates.short_name_year, ac.treelevelstr || ac.name,
				    ( (
				      select COALESCE(sum(value),0) as total    
				      from accountpayable ap where
					  ap.toconciliation = false and
				      ap.company=ac.company and 
				      ap.category in (
				          select id from 
				          accountcategory acc 
				          where acc.company= ac.company and acc.mintreenode between ac.mintreenode and ac.maxtreenode
				          )
				          and ap.paid=true and ap.typemovement=0 """ + sqldt + """
				          and (%s) and (%s)
				    ) 
				    -
				    (
				      select COALESCE(sum(value),0) as total    
				      from accountpayable ap where
					  ap.toconciliation = false and
				      ap.company=ac.company and
				      ap.category in (
				          select id from 
				          accountcategory acc 
				          where acc.company=ac.company and acc.mintreenode between ac.mintreenode and ac.maxtreenode
				          )
				          and ap.paid=true and ap.typemovement=1 """ + sqldt + """
				          and (%s) and (%s)
				    )
				    )
				    as total, ac.id, to_char (dates.start_of_month,'DD/MM/YYYY'), to_char (dates.end_of_month,'DD/MM/YYYY')
				    from 
				    accountcategory ac
				    inner join dates on(day=1)
				    where ac.company=? and dates.start_of_month between date(?) and date(?) and (%s)
				    order by dates.date_c, maxtreenode desc, orderinreport
				    ) as data where total <>00
				"""
				//if(!inverse)
					toResponse(SQL_REPORT_ACCOUNTPAYABLE.format(unit, costcenters, unit, costcenters, accounts),List(AuthUtil.company.id.is, start, end)) 
				//else
				//	toResponse(AccountPayable.SQL_REPORT_ACCOUNT_MONTH,List(AuthUtil.company.id.is, unit, start, end, true, true))
			} 

			case "report" :: "offsaleproduct" :: Nil Post _ =>{
				val quantity:String = S.param("quantity") match {
					case Full(p) if(p != "")=> "1"
					case _ => "0"
				}			

				def markup_value:Double = S.param("markup_value") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => 0;
				}
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

				val SQL_QTTY = if (quantity == "1") {
	    			"""
					union
					select '2.Qtde', 
					pr.name  || ' __ ' || coalesce (um.short_name,''), 
					coalesce (ic.currentstock,0),0
					from product pr 
					left join unitofmeasure um on um.id = pr.unitofmeasure
					left join inventorycurrent ic on ic.product = pr.id
					where pr.company = ? and pr.productclass = 1
					and %s
					"""
				} else {
					""
				}

				val SQL_REPORT = """
				select os.short_name, 
				pr.name  || ' __ ' || coalesce (um.short_name,''), 
				op.offprice + ((op.offprice * ?) / 100),
				op.id
				from offsaleproduct op 
				inner join offsale os on os.id = op.offsale
				inner join product pr on pr.id = op.product
				left join unitofmeasure um on um.id = pr.unitofmeasure
				where op.company = ?
				and %s
				""" + SQL_QTTY + """
				union
					select '1.Embal', 
					pr.name  || ' __ ' || coalesce (um.short_name,''), 
					coalesce(to_number (to_char (pr.measureinunit, '9990.99'),'9999.99'),'0'),0
					from product pr 
					left join unitofmeasure um on um.id = pr.unitofmeasure
					where pr.company = 275 and pr.productclass = 1
				order by 1,2
				"""
				if (quantity == "1") {
					toResponse(SQL_REPORT.format(offsales, unit),List(markup_value, AuthUtil.company.id.is, AuthUtil.company.id.is)) 
				} else {
					toResponse(SQL_REPORT.format(offsales, unit),List(markup_value, AuthUtil.company.id.is)) 
				}
			} 

			case "report" :: "sales" :: Nil Post _ =>{
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
				val valueOrQuantity:String = S.param("quantity") match {
					case Full(p) if(p != "")=> "count (td.amount)"
					case _ => "sum (td.price)"
				}			
				val userbreak:String = S.param("userbreak") match {
					case Full(p) if(p != "")=> "coalesce (bp.short_name,'') || ' - ' ||"
					case _ => ""
				}			

				val SQL = """
					select short_name_year, %s pr.name, %s from dates 
					left join treatment tr on tr.company = ? and tr.status = 4 
						and tr.dateevent between start_of_month and end_of_month 
						--and tr.user_c is not null
						--não faz mais pra trazer venda de produto sem user
						--o coalesce no user break resolve o name null 
					left join business_pattern bp on bp.id = tr.user_c
					left join treatmentdetail td on td.treatment = tr.id
					left join product pr on (pr.id = td.activity or pr.id = td.product) 
					left join producttype pt on pt.id = pr.typeproduct
					where date_c between date (?) and date (?) and day = 1 
					%s
					%s
					%s
					%s
                    and pr.productclass in (%s)
					group by date_c, short_name_year, %s pr.name
					order by date_c, short_name_year, %s pr.name;
				"""
				toResponse(SQL.format(userbreak, valueOrQuantity, user,prod,unit,producttype, classes, userbreak, userbreak),
					List(AuthUtil.company.id.is, start, end)) 
			} 

			case "report" :: "cheques" :: Nil Post _ => {

				var params = AuthUtil.company.id.is :: List[Any]()
				
				def startFilter = S.param("start") match {
					case Full(p) if(p != "")=> {
						 params	= params ::: List(Project.strToDateOrToday(p)) 
						" and date(ch.duedate) >= date(?)"
					}
					case _ => " " 
				}
				def endFilter:String = S.param("end") match {
					case Full(p) if(p != "") => {
						params	= params ::: List(Project.strToDateOrToday(p))
						" and date(ch.duedate) <= date(?)"
					}
					case _ => ""
				}
				def valueStartFilter:String = S.param("value_start") match {
					case Full(p) if(p !="") => {
						params	= params ::: List(p.toDouble)
						" and ch.value >= ?"
					}
					case _ => ""
				}				
				def valueEndFilter:String = S.param("value_end") match {
					case Full(p) if(p !="") => {
						params	= params ::: List(p.toDouble)
						" and ch.value <= ?"
					}
					case _ => ""
				}
				def customerFilter = S.param("customer") match {
					case Full(p) if(p != "")=> {
						 params	= params ::: List(p.toLong)
						" and ch.customer = ?"
					}
					case _ => " " 
				}
				def receovedStartFilter = S.param("receivedDate_start") match {
					case Full(p) if(p != "") => {
						 params	= params ::: List(Project.strOnlyDateToDate(p)) 
						" and ch.receiveddate >= date(?) "
					}
					case _ => " " 
				}				
				def receovedEndFilter = S.param("receivedDate_end") match {
					case Full(p) if(p != "") => {
						 params	= params ::: List(Project.strOnlyDateToDate(p)) 
						" and ch.receiveddate <= date(?) "
					}
					case _ => " " 
				}								

				def efetivepaymentdateStartFilter = S.param("efetivepaymentdate_start") match {
					case Full(p) if(p != "") => {
						 params	= params ::: List(Project.strOnlyDateToDate(p)) 
						" and ch.efetivepaymentdate >= date(?) "
					}
					case _ => " " 
				}				
				def efetivepaymentdateEndFilter = S.param("efetivepaymentdate_end") match {
					case Full(p) if(p != "") => {
						 params	= params ::: List(Project.strOnlyDateToDate(p)) 
						" and ch.efetivepaymentdate <= date(?) "
					}
					case _ => " " 
				}								

				def bankFilter = S.param("bank") match {
					case Full(p) if(p != "") => {
						 params	= params ::: List(p.toLong) 
						" and ch.bank = ?"
					}
					case _ => " " 
				}
				val SQL_REPORT_CHEQUE = """
									select
									cu.id,
									cu.name as customer,
									cashier.idforcompany,
									b.short_name as bank,
									ch.agency,
									ch.account,
									ch.number_c,
									p.command,
									ch.received,
									ch.value,
									ch.duedate,
									ch.receivedDate,
									ch.efetivepaymentdate
									from   
									cheque ch
									inner join business_pattern cu on(ch.customer = cu.id)
									inner join paymentdetail pd on(pd.id = ch.paymentdetail)
									inner join payment p on(pd.payment = p.id)
									inner join cashier on(cashier.id = p.cashier)
									left join bank b on(b.id = ch.bank)
									where ch.company = ? and ch.movementtype = 0
								"""
				val sql = {
					SQL_REPORT_CHEQUE + endFilter + startFilter + customerFilter + bankFilter + receovedStartFilter + receovedEndFilter + efetivepaymentdateStartFilter + efetivepaymentdateEndFilter + valueStartFilter+ valueEndFilter +  " order by ch.duedate "
				}
				toResponse(sql,params)
			}
			case "report" :: "monthly" :: Nil Post _ => {
				toResponse(Monthly.SQL_REPORT,List(AuthUtil.company.id.is))
			}

			case "report" :: "presumed_income" :: Nil Post _ => {
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
				val sql = """
					select 
					     company,
					     unidade,
					     grupo,
					     profissional,
					     tipo,
					     produto, 
					     sum(price) as price, 
					     sum (coust) as coust,
					     sum(gain) as gain
					from (
					select
					     co.name as company,
					     cu.name as unidade,
					     ug.name as grupo,
					     bp.name as profissional,
					     tp.name as tipo,
					     pr.name as produto, 
					     td.price as price,
					     COALESCE ((select sum (value) from commision where treatment_detail = td.id),0) + coalesce(pr.purchaseprice,0) as coust,
					     td.price - (COALESCE ((select sum (value) from commision where treatment_detail = td.id),0)+ coalesce(pr.purchaseprice,0)) as gain
					from  treatment tr
						inner join treatmentdetail td on tr.id = td.treatment
						inner join product pr on (td.activity = pr.id or td.product = pr.id)
						inner join company co on (co.id = tr.company)
						inner join companyunit cu on(cu.id = tr.unit)
						left join business_pattern bp on (bp.id = tr.user_c)
						left join usergroup ug on( ug.id = bp.group_c)
						left join producttype tp on(tp.id = pr.typeproduct)
					where
						tr.company =  ? and
						productclass  in (%s)
						and tr.status = 4
						and  tr.dateevent between date(?) and date(?)
						order by bp.name, pr.name,cu.name, co.name							
					) as data1
					group by produto, profissional, grupo,tipo, unidade,company
					order by profissional, produto,unidade, company						
				"""
				toResponse(sql.format (productclass),List(AuthUtil.company.id.is, start, end))
			}

			case "report" :: "presumed_income_product" :: Nil Post _ => {
				def start:Date = S.param("start") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}
				def end:Date = S.param("end") match {
					case Full(p) => Project.strToDateOrToday(p)
					case _ => new Date()
				}			
/*
				select
				     co.name,
				     u.name,
				     tp.name,
				     p.name, 
				     sum(td.price) as price, 
				     sum(COALESCE(c.value,0) +p.purchaseprice) as coust,
				    sum((td.price-(COALESCE(c.value,0)+p.purchaseprice))) gain
				from  
				product p
				inner join producttype tp on(tp.id = p.typeproduct)
				inner join treatmentdetail td on(td.product = p.id)
				inner join treatment t on(t.id=td.treatment)
				left join commision c on (c.treatment_detail = td.id)
				inner join companyunit u on(u.id = t.unit)
				inner join company co on (co.id = p.company)
				where
				p.company =  ? and
				productclass  =1
				and t.status = 4
				and  t.dateevent between date(?) and date(?)
				group by p.id,tp.id, u.id,co.name,p.name, tp.name, u.name
				order by p.name,u.name, co.name
*/
				val sql = """
					select 
					     company,
					     unidade,
					     grupo,
					     profissional,
					     tipo,
					     produto, 
					     sum(price) as price, 
					     sum (coust) as coust,
					     sum(gain) as gain
					from (
					select
					     co.name as company,
					     cu.name as unidade,
					     ug.name as grupo,
					     bp.name as profissional,
					     tp.name as tipo,
					     pr.name as produto, 
					     td.price as price,
					     COALESCE ((select sum (value) from commision where treatment_detail = td.id),0) + coalesce(pr.purchaseprice,0) as coust,
					     td.price - (COALESCE ((select sum (value) from commision where treatment_detail = td.id),0)+ coalesce(pr.purchaseprice,0)) as gain
					from  treatment tr
						inner join treatmentdetail td on tr.id = td.treatment
						inner join product pr on (td.activity = pr.id or td.product = pr.id)
						inner join company co on (co.id = tr.company)
						inner join companyunit cu on(cu.id = tr.unit)
						left join business_pattern bp on (bp.id = tr.user_c)
						left join usergroup ug on( ug.id = bp.group_c)
						left join producttype tp on(tp.id = pr.typeproduct)
					where
						tr.company =  ? and
						productclass  in (1)
						and tr.status = 4
						and  tr.dateevent between date(?) and date(?)
						order by bp.name, pr.name,cu.name, co.name							
					) as data1
					group by produto, profissional, grupo,tipo, unidade,company
					order by profissional, produto,unidade, company						
				"""
			toResponse(sql,List(AuthUtil.company.id.is, start, end))
		}

		case "report" :: "treatment_audit" :: Nil Post _=> {
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}

			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}

			def event:String = S.param("event") match {
				case Full(p) if(p != "All") => p
				case _ => "%"
			}

			def table_c:String = S.param("table_c") match {
				case Full(p) if(p != "All") => p
				case _ => "%"
			}

			def jsobj:String = S.param("jsobj") match {
				case Full(p) => "%"+p+"%"
				case _ => "%"
			}

			def user_where:String = S.param("user") match {
				case Full(p) if(p != "") => " createdby = %s".format(p)
				case _ => "1=1"
			}			

			def idobj:String = S.param("idobj") match {
				case Full(p) if(p != "") => " idobj = %s".format(p)
				case _ => "1=1"
			}			

			val SQL = """
				select idobj,jsobj,createdat,event, createdby
				from log.auditmapper 
				where table_c like ?
				and jsobj like ?
				and company=?
				and date(createdat) between date(?) and date(?)
				and event like ?
				and %s
				and %s
				order by createdat desc
				limit 1000
			"""
			toResponse(SQL.format(user_where,idobj),List(table_c, jsobj, AuthUtil.company.id.is, start, end, event))
		}
		case "report" :: "treatment_create_update" :: Nil Post _=> {
			def customer:String = S.param("customer") match {
				case Full(p) if(p != "") => " and bc.id =%S".format(p) 
				case _ => ""
			}			
			def user = S.param("user") match {
				case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
				case _ => S.param("user[]") match {
					case Full(p) if(p != "") => " and bp.id in(%s)".format(S.params("user[]").foldLeft("0")(_+","+_))
					case _ => " " 
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
			def startlog:Date = S.param("startlog") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def endlog:Date = S.param("endlog") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}				
			def unit:String = S.param("unit") match {
				case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
				case _ => ""
			}		

			val SQL = """ select bc.id, bc.name as cliente, tr.id, tr.status, tr.start_c, tr.end_c, 
				cu.short_name, bp.short_name as profissional, tr.detailtreatmentastext,
				bui.short_name as incluiu, tr.createdat, bua.short_name as atualizou, tr.updatedat 
				from treatment tr 
				inner join business_pattern bc on bc.id = tr.customer 
				left join business_pattern bp on bp.id = tr.user_c 
				inner join companyunit cu on cu.id = tr.unit
				inner join business_pattern bui on bui.id = tr.createdby
				inner join business_pattern bua on bua.id = tr.createdby
				where tr.company = ? %s %s %s and tr.dateevent between ? and ? and date(tr.createdat) between ? and ? 
				order by tr.createdat limit 300
				"""
			toResponse(SQL.format(customer, user, unit),List(AuthUtil.company.id.is, start, end, startlog, endlog))
		}
		case "report" :: "treatmentdetail_deletelog" :: Nil Post _=> {
			def customer:String = S.param("customer") match {
				case Full(p) if(p != "") => " and bc.id =%S".format(p) 
				case _ => ""
			}			
			def user = S.param("user") match {
				case Full(p) if(p != "")=> " and bp.id in(%s)".format(p)
				case _ => S.param("user[]") match {
					case Full(p) if(p != "") => " and bp.id in(%s)".format(S.params("user[]").foldLeft("0")(_+","+_))
					case _ => " " 
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
			def startlog:Date = S.param("startlog") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def endlog:Date = S.param("endlog") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}				
			def unit:String = S.param("unit") match {
				case Full(p) if(p != "") => " and tr.unit =%S".format(p) 
				case _ => " and " + Treatment.unitsToShowSql
			}		

			val SQL = """ select bc.id, bc.short_name as cliente, 
			tr.id, tr.status2, tr.start_c as inicio, tr.end_c as fim, 
			cu.short_name, bp.short_name as profissional, am.idobj, 
			pr.name as servico, bu.short_name as excluido_por, 
			am.createdat as excluido_em 
			from treatment tr 
			inner join log.auditmapper am on (am.company = tr.company and am.jsobj like '%'||trim (to_char (tr.id,'99999999'))||'%' 
			and am.table_c = 'treatmentdetail' and am.event = 'afterDelete' and am.jsobj not like '%_activity_: null,%')
			inner join product pr on pr.company = tr.company and pr.id = to_number (substr (am.jsobj,position ('activity": ' in lower (am.jsobj)) + 11,7 ),'9999999')
			inner join business_pattern bc on bc.id = tr.customer
			inner join companyunit cu on cu.id = tr.unit
			left join business_pattern bp on bp.id = tr.user_c
			inner join business_pattern bu on bu.id = am.createdby
			where tr.company = ? and tr.dateevent between date(?) and date(?)
			and date (am.createdat) between date(?) and date(?)

			"""
			val SQL1 = """ union select bc.id, bc.short_name as cliente, 
			tr.id, tr.status2, tr.start_c as inicio, tr.end_c as fim, 
			cu.short_name, bp.short_name as profissional, td.id, 
			pr.name as servico, bu.short_name as excluido_por, 
			am.createdat as excluido_em 
			from treatment tr 
			inner join log.auditmapper am on (am.company = tr.company and am.idobj = tr.id
			and am.table_c = 'treatment' and (am.event = 'afterLogicalDelete' or am.event = 'afterSave')
			and am.id = (select max (am1.id) from log.auditmapper am1 where am1.company = tr.company and am1.idobj = tr.id
			and am1.table_c = 'treatment' and date (am1.createdat) between date(?) and date(?)))
			inner join treatmentdetail td on td.treatment = tr.id
			inner join product pr on pr.company = tr.company and pr.id = td.activity
			inner join business_pattern bc on bc.id = tr.customer
			inner join companyunit cu on cu.id = tr.unit
			left join business_pattern bp on bp.id = tr.user_c
			inner join business_pattern bu on bu.id = am.createdby
			where tr.company = ? and tr.dateevent between date(?) and date(?) and tr.status = 5
			and date (am.createdat) between date(?) and date(?)

			"""
			// tr.status = 0 and tr.hasdetail = false and 
			// o sql foi quebrado porque a presença do % dos likes atrapalhavam a substituição dos para string %s
			val SQL2 = """  %s %s %s
			"""
			val SQL3 = """ 	order by inicio, cliente 
			"""
			val SQL4 = SQL + SQL2.format(customer, user, unit) + SQL1 + SQL2.format(customer, user, unit) + SQL3;
//			LogActor ! SQL4
			toResponse(SQL4,List(AuthUtil.company.id.is, start, end, startlog, endlog, startlog, endlog,AuthUtil.company.id.is, start, end, startlog, endlog))
		}
		case "report" :: "operacional_results" :: Nil Post _=> {
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}

			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def unit:String = S.param("unit") match {
				case Full(s) => s
				case _ => S.params("unit[]").filter( _!= "").reduceLeft(_+" , "+_)		
			}

			val SQL = """
				select (venda-commision-preco_compra) as lucro, *
						from(
						select (select sum(value) as venda
						from (
						select distinct pd.*, pt.name,pt.id as pid
						from    treatment tr
						--inner join treatmentdetail td on(td.treatment = tr.id)
						inner join payment p on(p.id = tr.payment)
						inner join paymentdetail pd on(pd.payment = p.id)
						inner join paymenttype pt on(pt.id =  pd.typepayment)
						inner join cashier c on(c.id = p.cashier)
						inner join companyunit cu on (cu.id = c.unit)
						where 
							tr.status=4 
							and tr.company=?
							and tr.unit in( %s )
							and pt.sumtoconference = true
							and date(p.datepayment) between date(?) and date(?)
						order by pd.id,name desc
						) as data) as venda,
						(
						select sum(value) as commision from (select distinct co.*
						from    treatment tr
						inner join payment p on(p.id = tr.payment)
						inner join cashier c on(c.id = p.cashier)
						inner join companyunit cu on (cu.id = c.unit)
						inner join commision co on(co.payment = p.id)
						where 
							tr.status=4 
							and tr.company=? 
							and tr.unit in( %s )
							and date(p.datepayment) between date(?) and date(?)
						order by co.id desc
						) as data) as commision,
						(
						select sum(purchaseprice)
						from (
						select pro.*
						from    treatment tr
						inner join treatmentdetail td on(td.treatment = tr.id)
						inner join payment p on(p.id = tr.payment)
						inner join cashier c on(c.id = p.cashier)
						inner join companyunit cu on (cu.id = c.unit)
						inner join product pro on (pro.id = td.product)
						where 
							tr.status=4 
							and tr.company=?
							and tr.unit in( %s )
							and pro.productclass = 1
							and pro.is_bom = false
							and date(p.datepayment) between date(?) and date(?)
						) as data
						) as preco_compra) as data;
			"""
			toResponse(SQL.format(unit, unit, unit),List(AuthUtil.company.id.is, start, end,AuthUtil.company.id.is, start, end,AuthUtil.company.id.is, start, end))
		}		

		case "report" :: "inventory_current_status" :: Nil Post _=> {

			val SQL = """
			select c.name as company, u.name unit, pt.name, p.name, COALESCE(i.currentstock,0), p.purchaseprice, p.saleprice 
			from product p
			inner join producttype pt on(pt.id = p.typeproduct) 
			inner join inventorycurrent i on(i.product = p.id)
			inner join companyunit u on(u.id=i.unit)
			inner join company c on(c.id=u.company)
			where c.id=?"""
			toResponse(SQL,List(AuthUtil.company.id.is))
		}

		case "report" :: "deliverydetail" :: Nil Post _=> {
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
			val product_type:List[String] = S.param("product_type") match {
				case Full(p) if(p != "")=> S.params("product_type")
				case _ => Nil
			}			
			val active:Long = S.param("active") match {
				case Full(p) if(p != "")=> 1
				case _ => 0
			}			
			val rel_mini:Long = S.param("rel_mini") match {
				case Full(p) if(p != "")=> 1
				case _ => 0
			}			
			
			val activity_where = if(activity == 0){
				"1 = 1"
			}else{
				"pbom.id="+activity
			}
			val activity_type_where = activity_type match {
				case Nil => "1 = 1"
				case ats:List[String] => "pbom.id in (select id from product where typeproduct in(%s))".format(ats.reduceLeft(_+","+_))
				case _ => "1 = 1"
			}

			val customer:Long = S.param("customer") match {
				case Full(p) if(p != "")=> p.toLong
				case _ => 0
			}

			val packageLong:Long = S.param("package") match {
				case Full(p) if(p != "")=> p.toLong
				case _ => 0
			}
			val unitLong:Long = S.param("unit") match {
				case Full(p) if(p != "")=> p.toLong
				case _ => 0
			}			
			
			val package_where = if(packageLong == 0){
				"1 = 1"
			}else{
				"p.id="+packageLong
			}
			val product_type_where = product_type match {
				case Nil => "1 = 1"
				case ats:List[String] => "p.typeproduct in (%s)".format(ats.reduceLeft(_+","+_))
				case _ => "1 = 1"
			}

// LogActor ! product_type_where

			val unit_where = if(unitLong == 0){
				"1 = 1"
			}else{
				"c.unit="+unitLong
			}

			val customer_where = if(customer == 0){
				"1 = 1"
			}else{
				"customer.id="+customer
			}
			val active_where = if(active == 0){
				"1 = 1"
			}else{
				if(rel_mini == 0){
    	            "(select count(1) from deliverydetail dd where dd.delivery=dc.id and used=false and dd.product=pbom.id ) > 0" 
   				}else{
                	"(select count(1) from deliverydetail dd where dd.delivery = dc.id and dd.used = false) > 0"
                }
				//"un_used >0"
			}
			val dateFilter = " date(pay.datepayment) between date(?) and date(?) "

			if(rel_mini == 0){
				//info (DeliveryControl.SQL_REPORT)
				toResponse(DeliveryControl.SQL_REPORT.format(activity_where, customer_where, package_where, unit_where, 
					dateFilter, activity_type_where, product_type_where, active_where),List(AuthUtil.company.id.is, start, end))
			}else{
				toResponse(DeliveryControl.SQL_REPORT_MINI.format(customer_where, package_where, unit_where, 
					dateFilter, product_type_where, active_where),List(AuthUtil.company.id.is, start, end))
			}
		}

		case "report" :: "graphic_accounts" :: Nil Post _ =>{
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}

			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			toResponse(AccountCategory.SQL_REPORT_GRAPHIC,List(AuthUtil.company.id.is,start, end ))
		}

		case "report" :: "inventory_control" :: Nil Post _ =>{
			def unit = S.param("unit") match {
				case Full(p) => p.toLong
				case _ => AuthUtil.unit.id.is
			}
			
			def line = S.param("line") match {
				case Full(p) if(p != "") => " pr.id in(select product from productlinetag where line="+p.toLong+") "
				case _ => "1 = 1"
			}
			def brand = S.param("brand") match {
				case Full(p) if(p != "") => "pr.brand = "+p.toLong
				case _ => "1 = 1"
			}
			def category = S.param("category_select") match {
				case Full(s) if(s != "") => {
					"pr.id in (select id from product where typeproduct in("+S.params("category_select").reduceLeft(_+","+_)+"))"
				}
				case _ => {
					"1 =1"
				}		
			}
			val rel_zero:String = S.param("rel_zero") match {
				case Full(p) if(p != "")=> " " // mostra todos
				case _ => " and ic.currentstock > 0 " // só estoque mior que zero pierre
			}			

			toResponse(InventoryMovement.SQL_REPORT_INVENTORY.format(
				rel_zero, line, brand, category ),List(AuthUtil.company.id.is, unit))
		}
		case "report" :: "inventory_control_no_unit" :: Nil Post _ =>{


			def line = S.param("line") match {
				case Full(p) if(p != "") => " pr.id in(select product from productlinetag where line="+p.toLong+") "
				case _ => "1 = 1"
			}
			def brand = S.param("brand") match {
				case Full(p) if(p != "") => "pr.brand = "+p.toLong
				case _ => "1 = 1"
			}
			def category = S.param("category_select") match {
				case Full(s) if(s != "") => {
					"pr.id in (select id from product where typeproduct in("+S.params("category_select").reduceLeft(_+","+_)+"))"
				}
				case _ => {
					"1 =1"
				}		
			}
			val rel_zero:String = S.param("rel_zero") match {
				case Full(p) if(p != "")=> " " // mostra todos
				case _ => " and ic.currentstock > 0 " // só estoque mior que zero pierre
			}			
			toResponse(InventoryMovement.SQL_REPORT_INVENTORY_UNIT_NO_FILTER.format(
				rel_zero, line, brand, category),List(AuthUtil.company.id.is))
		}

		case "report" :: "commission_sumary" :: Nil Post _ =>{
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}

			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}			
			def unitA:List[String] = S.param("unit") match {
				case Full(p) if(p != "")=> S.params("unit")
				case _ => CompanyUnit.findAllInCompany.map(_.id.is.toString)
			}
			lazy val SQL_REPORT_SUMARY = """
			  select * from (
			    select name, 
			    (select sum(c.value) from commision c where c.user_c=bp.id and payment_date between date(?) and date(?)) as commision,
			    (select sum(a.value) from accountpayable a where 
				a.toconciliation = false and
			    a.user_c=bp.id and date(duedate) between date(?) and date(?) ) as payments
			    from business_pattern bp
			    where 
			    is_employee=true
			    and userstatus=1
			    and company=?
			    and unit in(%s)
			    ) data where commision > 0 or payments > 0
			"""
			toResponse(SQL_REPORT_SUMARY.format(unitA.reduceLeft(_+" , "+_)),List(start, end, start, end, AuthUtil.company.id.is))
		}
		case "report" :: "payslip" :: Nil Post _ =>{
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}			
			def user:Long = S.param("user") match {
				case Full(p) if(p != "")=> p.toLong
				case _ => 0l
			}
			toResponse(PayrollEvent.SQL_PAYSHIP,List(user, start, end, user, start, end))
		}
		case "report" :: "payroll_liquid" :: Nil Post _ =>{
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
			def start_value:Double = S.param("start_value") match {
				case Full(p) if(p != "") => p.toDouble
				case _ => -9999999;
			}
			def end_value:Double = S.param("end_value") match {
				case Full(p) if(p != "") => p.toDouble
				case _ => 9999999;
			}
			val zero_val:String = S.param("zero_val") match {
				case Full(p) if(p != "")=> ""
				case _ => " and valor_liquido <> 0.0 "
			}			
			toResponse(PayrollEvent.SQL_LIQUID.format(unit, zero_val),
				List(AuthUtil.company.id.is, start, end, start_value, end_value))
		}		
		case "report" :: "offsale" :: Nil Post _ =>{
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def units:String = S.param("unit") match {
				case Full(s) if(s != "") => " and tr.unit = %s".format(s)
				case _ => " and " + Treatment.unitsToShowSql
			}
			def offsales:String = S.param("offsale") match {
				case Full(s) if(s != "") => " and td.offsale = %s".format(s)
				case _ => " and 1 = 1 "
			}
			def status:String = S.param("status") match {
				case Full(s) if(s != "All") => {
					"and tr.status = %S".format(s) 
				}
				case _ => ""
			}
			val SQL_OFFSALE = """
                select os.short_name as convenio, tr.dateevent, tr.command, bc.short_name as cliente, bp.short_name as profissional, pp.short_name as produto, pa.short_name as servico, 
				td.price, td.amount from treatmentdetail td 
				inner join treatment tr on tr.id = td.treatment --and tr.company = td.company
				inner join business_pattern bc on bc.id = tr.customer
				left join business_pattern bp on bp.id = tr.user_c
				left join product pp on pp.id = td.product
				left join product pa on pa.id = td.activity
				inner join offsale os on os.id = td.offsale
				where td.company = ? and tr.dateevent between ? and ?
				%s %s %s
				and td.offsale is not null
				order by td.offsale, tr.dateevent, bc.name
        	"""

			toResponse(SQL_OFFSALE.format(status, units, offsales),List(AuthUtil.company.id.is, start, end))
		}

		case "report" :: "invoice" :: Nil Post _ =>{
			def start:Date = S.param("start") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def end:Date = S.param("end") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def units:String = S.param("unit") match {
				case Full(s) if(s != "") => " and iv.unit = %s".format(s)
				case _ => " and 1 = 1 "
			}
			def offsales:String = S.param("offsale") match {
				case Full(s) if(s != "") => " and iv.offsale = %s".format(s)
				case _ => " and 1 = 1 "
			}

			val SQL_INVOICE = """
				select iv.id, iv.idforcompany, os.name as convenio, efectivedate, 
				iv.value, iv.startat, iv.endat, cu.name, iv.obs, iv.idforcompany 
				from invoice iv
				inner join offsale os on os.id = iv.offsale
				inner join companyunit cu on cu.id = iv.unit
				where iv.company = ? and iv.efectivedate between ? and ?
				%s %s
				order by iv.idforcompany desc
        	"""

			toResponse(SQL_INVOICE.format(units, offsales),List(AuthUtil.company.id.is, start, end))
		}

		case "report" :: "companies" :: Nil Post _ =>{
			val status:String = S.param("status") match {
				case Full(p) if(p != "")=> " status in (1,0) "
				case _ => " status in (1) "
			}			
			AuthUtil.checkSuperAdmin
			def SQL = """select id, name,id, phone, 
				contact, email, status, 
				fu_dt_age (date (now()), date (createdat)), 
				(select count (*) from business_pattern where company = company.id and is_employee = true and userstatus = 1 and showincalendar = true)
				|| '/' || (select count (*) from business_pattern where company = company.id and is_employee = true and userstatus = 1),
				createdat 
				from company where %s order by id desc"""
			toResponse(SQL.format (status), Nil)
		}

		case "report" :: "modules" :: Nil Post _ =>{
			val status:String = S.param("status") match {
				case Full(p) if(p != "")=> " pm.status in (1,0) "
				case _ => " pm.status in (1) "
			}			
			AuthUtil.checkSuperAdmin
			def SQL = """select pm.name, dt.name, pm.status, pm.id from permissionmodule pm 
				inner join company co on co.id = pm.company
				left join domaintable dt on dt.domain_name = 'modulos' and dt.cod = pm.name
				where pm.company = ? and %s order by pm.name;"""
			toResponse(SQL.format (status), List (AuthUtil.company.id.is));
		}

		case "report" :: "paymenttype_summary" :: Nil Post _ => {
			def units:String = S.param("unit") match {
				case Full(s) if(s != "") => " and ca.unit = %s".format(s)
				case _ => " and " + Cashier.unitsToShowSql
			}

			def start:Date = S.param("startDate") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			def end:Date = S.param("endDate") match {
				case Full(p) => Project.strToDateOrToday(p)
				case _ => new Date()
			}
			val payment_type_param_name = S.param("payment_type[]") match {
				case Full(p) => "payment_type[]"
				case _ => "payment_type"
			}
			def payment_type:String = S.param(payment_type_param_name) match {
				case Full(s) if(s != "") => " and pd.typepayment in " +
				"(%s) ".format(S.params(payment_type_param_name).foldLeft("0")(_+","+_))
				case _ => " and 1=1 "
			}
			//info ("============== " + payment_type)
			val cashier_param_name = S.param("cashier[]") match {
				case Full(p) => "cashier[]"
				case _ => "cashier"
			}
			def cashier:String = S.param(cashier_param_name) match {
				case Full(s) if(s != "") => " and pa.cashier in " +
				"(%s) ".format(S.params(cashier_param_name).foldLeft("0")(_+","+_))
				case _ => " and 1=1 "
			}
			//info ("============== " + cashier)
			lazy val SQL_REPORT = """
				select pa.datepayment, pt.name, sum (pd.value), count (pt.name) from paymentdetail pd 
				inner join paymenttype pt on pt.id = pd.typepayment
				inner join payment pa on pa.id = pd.payment
				inner join cashier ca on ca.id = pa.cashier
				where pd.company = ? and pa.datepayment between ? and ?
				%s %s %s
				group by pa.datepayment, pt.name
				order by pa.datepayment, pt.name
				;"""
			toResponse(SQL_REPORT.format(units, payment_type, cashier),List(AuthUtil.company.id.is, start, end))
		}
		
	}
}