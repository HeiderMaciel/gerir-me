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


object Reports3 extends RestHelper with ReportRest with net.liftweb.common.Logger {
		def customer:String = S.param("customer") match {
			case Full(p) if(p != "") => " and bp.id =%S".format(p) 
			case _ => ""
		}			
		def basecustomer:String = S.param("customer") match {
			case Full(p) if(p != "") => " and bb.id =%S".format(p) 
			case _ => ""
		}			
/*
		def mapicon:String = S.param("mapIcon") match {
			case Full(s) if(s == "")=> " and 1=1 "
			case _ => " and bp.mapicon in(%s)".format(S.params("mapIcon[]").filter( _!= "").reduceLeft(_+" , "+_))		
		}
*/
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

		def start_value:Double = S.param("start_value") match {
			case Full(p) if(p != "") => p.toDouble
			case _ => -9999999;
		}
		def end_value:Double = S.param("end_value") match {
			case Full(p) if(p != "") => p.toDouble
			case _ => 9999999;
		}

		serve {
			case "report" :: "customer_list" :: Nil Post _=> {
				val strAux = if (PermissionModule.anvisa_?) {
						" bp.barcode  || ' ' || "
					} else {
						""
					}

				// MOSTRA OS RELACIONAMENTOS SÓ PARA EGREX
				val SQL = """select bp.id, """ + 
				strAux + """ bp.name, trim (mobile_phone || ' ' || phone || ' ' || email_alternative), email, 
				cs.name,
				trunc ((((DATE_PART('year', now()) - DATE_PART('year', birthday)) * 12) 
					+ (DATE_PART('month', date (now())) - DATE_PART('month', birthday)))/12) as anos,
				birthday, sex,
				mi.short_name, 
				oc.name, id.name,
				trim (trim (bp.street || ', ' || bp.number_c || ' ' || bp.complement)
				|| ', ' || bp.district || ', ' || ci.name || '-' || st.short_name || ', ' || postal_code),
				cu.name, 

				(select max (bp1.name) from business_pattern bp1 where id in 
				(select bp_related from bprelationship where company = 88 and business_pattern = bp.id and relationship = 3)) as conjuge, 

				(select name from business_pattern bp1 where id in (select bp_related from (select row_number() over (order by id nulls last) as rownum, * from bprelationship 
				where company = 88 and business_pattern = bp.id and relationship = 1
				order by id 
				) as data1 where rownum = 1)) as filho1,

				(select name from business_pattern bp1 where id in (select bp_related from (select row_number() over (order by id nulls last) as rownum, * from bprelationship 
				where company = 88 and business_pattern = bp.id and relationship = 1
				order by id 
				) as data1 where rownum = 2)) as filho2,

				(select name from business_pattern bp1 where id in (select bp_related from (select row_number() over (order by id nulls last) as rownum, * from bprelationship 
				where company = 88 and business_pattern = bp.id and relationship = 1
				order by id 
				) as data1 where rownum = 3)) as filho3,

				(select name from business_pattern bp1 where id in (select bp_related from (select row_number() over (order by id nulls last) as rownum, * from bprelationship 
				where company = 88 and business_pattern = bp.id and relationship = 1
				order by id 
				) as data1 where rownum = 4)) as filho4,

				(select name from business_pattern bp1 where id in (select bp_related from (select row_number() over (order by id nulls last) as rownum, * from bprelationship 
				where company = 88 and business_pattern = bp.id and relationship = 1
				order by id 
				) as data1 where rownum = 5)) as filho5,

				mobile_phone, phone, email_alternative, -- separados para a CCZS
				
				bp.id
				from business_pattern bp  
				left join companyunit cu on cu.id = bp.unit
				left join mapicon mi on mi.id = bp.mapicon
				left join civilstatus cs on cs.id = bp.civilstatus
				left join occupation oc on oc.id = bp.occupation
				left join instructiondegree id on id.id = bp.instructiondegree
				left join city ci on ci.id = bp.cityref
				left join state st on st.id = bp.stateref
				where (case when birthday is not null then ((DATE_PART('year', now()) - DATE_PART('year', birthday)) * 12) 
					+ (DATE_PART('month', date (now())) - DATE_PART('month', birthday))
					when birthday is null then 0 end) between ?*12 and ?*12 and bp.company=? 
					and bp.is_unit = false
				%s %s %s %s %s %s %s %s %s %s %s order by bp.name """
				toResponse(SQL.format(customer, unit, sex, status, civilstatus, mapicon, projectclass, noprojectclass, project, noproject, offsale),
					List(start_value, end_value, AuthUtil.company.id.is))
			}
			case "report" :: "customer_radius" :: Nil Post _=> {
				def radius:Double = S.param("radius") match {
					case Full(p) if(p != "") => p.toDouble
					case _ => -9999999;
				}
				val SQL = """select bp.id, bp.name, trim (bp.mobile_phone || ' ' || bp.phone || ' ' || bp.email_alternative), bp.email, 
				cs.name,
				trunc ((((DATE_PART('year', now()) - DATE_PART('year', bp.birthday)) * 12) 
					+ (DATE_PART('month', date (now())) - DATE_PART('month', bp.birthday)))/12) as anos,
				bp.birthday, round (fu_distlatlong (bb.lat, bb.lng /*'-19.8301272', '-43.9884651'*/, bp.lat, bp.lng ),2),
				bp.sex,
				mi.short_name, 
				oc.name, id.name,
				trim (trim (bp.street || ', ' || bp.number_c || ' ' || bp.complement)
				|| ', ' || bp.district || ', ' || coalesce (ci.name, '') || '-' || coalesce (st.short_name,'') || ', ' || bp.postal_code),
				cu.name, 
				bp.id
				from business_pattern bp
				left join business_pattern bb on 1=1 %s  
				left join companyunit cu on cu.id = bp.unit
				left join mapicon mi on mi.id = bp.mapicon
				left join civilstatus cs on cs.id = bp.civilstatus
				left join occupation oc on oc.id = bp.occupation
				left join instructiondegree id on id.id = bp.instructiondegree
				left join city ci on ci.id = bp.cityref
				left join state st on st.id = bp.stateref
				where (case when bp.birthday is not null then ((DATE_PART('year', now()) - DATE_PART('year', bp.birthday)) * 12) 
					+ (DATE_PART('month', date (now())) - DATE_PART('month', bp.birthday))
					when bp.birthday is null then 0 end) between ?*12 and ?*12 and bp.company=? 
					and fu_distlatlong (bb.lat, bb.lng /*'-19.8301272', '-43.9884651'*/, bp.lat, bp.lng ) <= %s
					and bp.lat <> '' and bp.lng <> ''
					and bp.is_unit = false
				%s %s %s %s %s %s %s %s %s %s order by 8, bp.name """
				toResponse(SQL.format(basecustomer, radius, unit, sex, status, civilstatus, mapicon, projectclass, noprojectclass, project, noproject, offsale),
					List(start_value, end_value, AuthUtil.company.id.is))
			}
		}
}

