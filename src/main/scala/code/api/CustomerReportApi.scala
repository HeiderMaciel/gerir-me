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


object CustomerReportApi extends RestHelper with ReportRest {

	// dupliquei do bootstrap ou extende aqui ou vai pra um util
	def checkBooleanParamenter(name:String, defaultValue:Boolean = false) = {
		S.param(name) match {
			case Full(s) => !defaultValue
			case _ => defaultValue
		}
	}

		def customer = S.param("customer").get.toLong		
		serve {
			case "customer_report" :: "domains" :: Nil Get _ =>{
				JsArray(QuizDomain.findAllInCompanyOrDefaultCompany(OrderBy(QuizDomain.name, Ascending)).map((domain)=>{
					JsObj(("name",domain.name.is), ("id",domain.id.is))
				}
				))
			}
			case "customer_report" :: "quizsections" :: Nil Get _ =>{
				JsArray(QuizSection.findAllInCompanyOrDefaultCompany(OrderBy(QuizSection.name, Ascending)).map((section)=>{
					JsObj(("name",section.name.is), ("id",section.id.is))
				}
				))
			}
			case "customer_report" :: "quizzes" :: Nil Get _ =>{
				val issuperadm = if (AuthUtil.user.isSuperAdmin) {
					" or 1 = 1 "
				} else {
					" "
				}
				JsArray(Quiz.findAllInCompanyOrDefaultCompany(
			        BySql(""" (share = true or usergroup = (select group_c from business_pattern where id = ?) 
					    or usergroup in (select group_c from userusergroup where user_c = ?)
					    %s) """.format(issuperadm),IHaveValidatedThisSQL("",""), AuthUtil.user.id, AuthUtil.user.id),
					OrderBy(Quiz.name, Ascending)).map((quiz)=>{
					JsObj(("name",quiz.name.is), ("id",quiz.id.is), 
						("short_name",quiz.short_name.is))
				}
				))
			}
			case "customer_report" :: "quiz" :: Nil Post _ => {
				val issuperadm = if (AuthUtil.user.isSuperAdmin) {
					" or 1 = 1 "
				} else {
					" "
				}
				val sql_treat = """
				union
				select dateevent, age(date(now()), dateevent), pr.name, tr.obs, icd.namecomp, tr.id, null, false, 
				trim (COALESCE (icd.namecomp,'') || ' </span><br></span>' || tr.obs || '</span><br></span>' || td.obs || '</span><br></span>' || ted.obs || '</span><br></span>'),
				pr.showinrecords, bp.name, 0
				from treatment tr 
				inner join business_pattern bp on bp.id = tr.customer
				inner join treatmentdetail td on td.treatment = tr.id
				inner join product pr on pr.id = td.activity or pr.id = td.product
				left join treatedoctus ted on ted.treatment = tr.id
				left join icd on icd.id = ted.icd
				where tr.company = ? and tr.customer = ? and pr.showinrecords = true
				and tr.dateevent <= date(now())
				and tr.status in (3,4)
				"""
				// atendido 3 pago 4
				val sql_quiz = """
				select * from (
				select qa.applydate, age(date(now()), applydate), qu.name, qa.obs, ug.short_name, qa.id, qa.id,
					(qu.message <> ''), qa.message, qu.showinrecords, bp.name, 
					(select count(1) from quizquestion qq
					inner join quizsection qs on qs.id = qq.quizsection and qs.quiz = qu.id and qs.status = 1
					where qq.company = qu.company and qq.status = 1 )			  
				    from quizapplying qa 
					inner join business_pattern bp on bp.id = qa.business_pattern
					left join quiz qu on qu.id = qa.quiz
					left join usergroup ug on ug.id = qu.usergroup
					where qa.company = ? and bp.id = ?
					and (qa.quiz is null or qu.share = true or qu.usergroup = (select bp1.group_c from business_pattern bp1 where bp1.id = ?) 
					    or qu.usergroup in (select uu.group_c from userusergroup uu where uu.user_c = ?)
					    %s)
					--order by bp.id, qa.applydate desc
					%s
				) as data1 order by 1 desc, 6 asc
				"""
				if (AuthUtil.company.isMedical) {
					toResponse(sql_quiz.format(issuperadm, sql_treat),List(AuthUtil.company.id.is, customer, 
						AuthUtil.user.id.is, AuthUtil.user.id.is, AuthUtil.company.id.is, customer))
				} else {
					toResponse(sql_quiz.format(issuperadm, ""),List(AuthUtil.company.id.is, customer, 
						AuthUtil.user.id.is, AuthUtil.user.id.is))
				}
			}
			case "customer_report" :: "bpmonthly" :: Nil Post _ => {
				val sql_bpmonthly = """
				select pr.name, bm.obs, bm.startat, bm.endat, bm.value, bm.valueDiscount, bm.valuesession, bm.numsession, bm.id  from bpmonthly bm
					inner join business_pattern bp on bp.id = bm.business_pattern
					inner join product pr on pr.id = bm.product
					where bm.company = ? and bp.id = ?
					order by bm.startat desc;
				"""
				toResponse(sql_bpmonthly,List(AuthUtil.company.id.is, customer))
			}
			case "customer_report" :: "pet" :: Nil Post _ => {
				val sql_pet = """
				select ba.name, ba.birthday, ba.sex, sp.short_name, br.name from bprelationship bpr
				inner join business_pattern ba on ba.id = bpr.bp_related
				left join species sp on sp.id = ba.species
				left join breed br on br.id = ba.breed
				where bpr.company = ? and relationship = 26 and business_pattern = ?
				order by bpr.startat
				"""
				toResponse(sql_pet,List(AuthUtil.company.id.is, customer))
			}
			case "customer_report" :: "considerations" :: Nil Post _ => {
				toResponse(Customer.considerations_query,List(AuthUtil.company.id.is, customer))
			}
			case "customer_report" :: "bankaccount" :: Nil Post _ => {
				toResponse(Customer.bankaccount_query,List(AuthUtil.company.id.is, customer))
			}
			case "customer_report" :: "relationship" :: Nil Post _ => {
				val status = "1"
				toResponse(Customer.relationship_query.format(status),List(AuthUtil.company.id.is, customer))
			}
			case "customer_report" :: "stakeholder" :: Nil Post _ => {
				def showAll:Boolean = checkBooleanParamenter("event_all")		
				val status = if (showAll) {
					"1"
				} else {
					"1"
				}
				toResponse(Customer.stakeholder_query.format(status, status),
					List(AuthUtil.company.id.is, customer, false))
			}
			case "customer_report" :: "stakeholder_class" :: Nil Post _ => {
				def showAll:Boolean = checkBooleanParamenter("event_all")		
				val status = if (showAll) {
					"1"
				} else {
					"1"
				}
				toResponse(Customer.stakeholder_query.format(status, status),
					List(AuthUtil.company.id.is, customer, true))
			}
			case "customer_report" :: "account" :: Nil Post _ => {
				toResponse(Customer.account_query,List(customer, AuthUtil.company.id.is, customer, AuthUtil.company.id.is))
			}			
			case "customer_report" :: "indications" :: Nil Post _ => {
				toResponse(Customer.indicatedby_query,List(AuthUtil.company.id.is,S.param("customer").get.toLong))
			}
			case "customer_report" :: "history" :: Nil Post _ => {
				    val sql = """ select * from (select '2001-01-01' data_1, 1 tipo, bc.name obs1, 0 valor1, null obs2, 0 valor2 from business_pattern bc where id = ?
								union
								select pa.datepayment data_1, 2 tipo, 'prof: ' || bp.name obs1, null valor1, 'Pago' obs2, null valor2 from treatment tr 
								inner join payment pa on tr.payment = pa.id
								left join business_pattern bp on bp.id = tr.user_c
								where tr.customer = ? and tr.status = 4 and tr.hasdetail = true
								union
								select tr.start_c data_1, 2 tipo,  'prof: ' || bp.name obs1, null valor1, 'Agendado' obs2, null valor2 from treatment tr 
								left join business_pattern bp on bp.id = tr.user_c
								where tr.customer = ? and tr.status <> 4 and tr.hasdetail = 't'
								union
								select pa.datepayment data_1, 3 tipo, ps.name obs1, td.price valor1, null obs2, null from treatmentdetail td
								inner join treatment tr on td.treatment = tr.id
								inner join payment pa on pa.id = tr.payment
								inner join product ps on ps.id = td.activity
								where tr.customer = ? and tr.status = 4 and td.product is null and tr.hasdetail = 't'
								union
								select pa.datepayment data_1, 3 tipo, pp.name obs1, td.price valor1, null obs2, null from treatmentdetail td
								inner join treatment tr on td.treatment = tr.id
								inner join payment pa on pa.id = tr.payment
								inner join product pp on pp.id = td.product
								where tr.customer = ? and tr.status = 4 and td.activity is null and tr.hasdetail = 't'
								union
								select tr.start_c data_1, 3 tipo, ps.name obs1, td.price valor1, null obs2, null from treatmentdetail td
								inner join treatment tr on td.treatment = tr.id
								left join product ps on ps.id = td.activity
								where tr.customer = ? and tr.status <> 4  and td.product is null
								union
								select tr.start_c data_1, 3 tipo, pp.name obs1, td.price valor1, null obs2, null from treatmentdetail td
								inner join treatment tr on td.treatment = tr.id
								left join product pp on pp.id = td.product
								where tr.customer = ? and tr.status <> 4 and td.activity is null
								union
								select pa.datepayment data_1, 4 tipo, null obs1, null valor1, pt.name obs2, pd.value valor2 from treatment tr 
								inner join payment pa on tr.payment = pa.id
								inner join paymentdetail pd on pd.payment = pa.id
								inner join paymenttype pt on pd.typepayment = pt.id
								where tr.customer = ? and tr.status = 4
								union
								select null data_1, 5 tipo, 'TOTAL DO CLIENTE: ' obs1, null valor1, '>>>>>' obs2, sum (pd.value) valor2 from treatment tr 
								inner join payment pa on tr.payment = pa.id
								inner join paymentdetail pd on pd.payment = pa.id
								inner join paymenttype pt on pd.typepayment = pt.id
								where tr.customer = ? and tr.status = 4 and pt.deliverycontol <> 't'
								) as datain
								order by data_1, tipo, valor1;
								 """
				lazy val customer = S.param("customer").get.toLong
   				 toResponse(sql,List(customer,customer,customer,customer,customer,customer,customer,customer,customer))
			}		
			
		}
}