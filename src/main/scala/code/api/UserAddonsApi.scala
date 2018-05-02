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

object UserAddonsApi extends RestHelper with ReportRest {
		
		def user = S.param("user").get.toLong		
		serve {
			case "user_api" :: "companyunit" :: Nil Post _ => {
				for {
					user <- S.param("user") ?~ "user parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					unit <- S.param("unit") ?~ "unit parameter missing" ~> 400
					showincalendar <- S.param("showincalendar") ?~ "showincalendar parameter missing" ~> 400

				} yield {
					val showcal = (showincalendar == "true" || showincalendar == "on");
					JBool(UserCompanyUnit.createInCompany.
						user(user.toLong).obs(obs).
						showInCalendar_?(showcal).
						unit(unit.toLong).save)
				}
			}
			case "user_api" :: "companyunit" :: id :: Nil Delete _ => {
				JBool(UserCompanyUnit.findByKey(id.toLong).get.delete_!)
			}
			case "user_api" :: "list" :: "companyunit" :: Nil Post _ => {
				val sql_companyunit = """
				select cu.name, uu.showincalendar, uu.obs, uu.id from usercompanyunit uu
					inner join companyunit cu on cu.id = uu.unit
					where uu.company = ? and uu.user_c = ?
					order by cu.name;
				"""
				toResponse(sql_companyunit,List(AuthUtil.company.id.is, user))
			}
			case "user_api" :: "usergroup" :: Nil Post _ => {
				for {
					user <- S.param("user") ?~ "user parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					group <- S.param("group") ?~ "group parameter missing" ~> 400

				} yield {
					JBool(UserUserGroup.createInCompany.user(user.toLong).obs(obs).group(group.toLong).save)
				}
			}
			case "user_api" :: "usergroup" :: id :: Nil Delete _ => {
				JBool(UserUserGroup.findByKey(id.toLong).get.delete_!)
			}
			case "user_api" :: "list" :: "usergroup" :: Nil Post _ => {
				val sql_usergroup = """
				select ug.name, uu.obs, uu.id from userusergroup uu
					inner join usergroup ug on ug.id = uu.group_c
					where uu.company = ? and uu.user_c = ?
					order by ug.name;
				"""
				toResponse(sql_usergroup,List(AuthUtil.company.id.is, user))
			}
		}

}