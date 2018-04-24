package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

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
import java.util.Date
import java.io.File
import _root_.java.util.Calendar

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object SystemApi extends RestHelper {
	serve {
		case "system" :: "timezone"  :: Nil JsonGet _ => {
			JString(BusinessRulesUtil.md5("1"));
		}
		case "system" :: "date"  :: Nil JsonGet _ => {
			JInt(new Date().getTime);
		}
		case "system" :: "test"  :: Nil JsonGet _ => {
			JString(BusinessRulesUtil.md5("1"));
		}
		case "system" :: "addCommisionCustomer" :: Nil JsonGet _ => {
			CommisionQueeue.start
			JInt(1)
		}		
		case "system" :: "migrationAjustsStart" :: Nil JsonGet _ => {
			BusinessPatternAjustsQueeue.start
			JInt(1)
		}
		case "system" :: "migrationTreatmentStart" :: Nil JsonGet _ => {
			Treatment.findAll(BySql("detailTreatmentAsText isnull  and user_c IS NOT NULL",IHaveValidatedThisSQL("start_c","01-01-2012 00:00:00"))).map(_.detailTreatmentAsText("").save)
			JInt(1)
		}		
		case "system" :: "migrationAjustsCompany" :: company  :: Nil JsonGet _ => {
			Customer.findAll(By(Customer.company, company.toLong))
					.foreach((bp) => {  
						BusinessPatternAjustsQueeue.enqueeue(BusinessPatternQueeueDto(bp.id.is))
					})
			JInt(1)
		}//
		case "system" :: "migrationAjustsAllCompanies" :: company  :: Nil JsonGet _ => {
			Customer.findAllInCompanies.foreach((bp) => {  
						BusinessPatternAjustsQueeue.enqueeue(BusinessPatternQueeueDto(bp.id.is))
					} )
			JInt(1)
		}//				

		case "system" :: "notificationsStart"  :: Nil JsonGet _ => {
			NotificationQueeue.start
			JInt(1)
		}//
		case "system" :: "commisionQueeueProcesso"  :: Nil JsonGet _ => {
			Payment.findAll(By(Payment.commision_processed_?, false)).foreach((p) => CommisionQueeue.processPaymentEnqueeue(p));
			JInt(1)
		}		
		//case "system" :: "createMonthly" :: company :: datePaymet :: desc :: Nil JsonGet _ => {
			//Monthly.createMonthly(Company.findByKey(company.toLong).get,Project.strOnlyDateToDate(datePaymet),"Mensalidade referente a %s".format(desc))
		//	JInt(1)  
		//}
		case "system" :: "createMonthlys" :: Nil JsonGet _ => {
			MonthlyService.generateMonthlyAllCompanies(new Date())
			JInt(1)  
		}		
		case "system" :: "sendDailyMonthly"  :: Nil JsonGet _ => {
			code.daily.DailyReport.sendAllTodayMonthlyMail
			JInt(1)
		}		
		case "system" :: "sendTomorrowTreatmentsReport"  :: Nil JsonGet _ => {
			//SendTomorrowTreatmentsReport.send(tomorrow, Company.notifyForTomorrow)
			SendTomorrowTreatmentsReport.send
			JInt(1)
		}
		case "system" :: "sendDailyTreatmentsReport"  :: Nil JsonGet _ => {
			//SendDailyTreatmentsReport.send(new Date(), Company.notifyForToday1Am)
	        Company.findAllActiveToChangeFinancialToPaid.foreach( company => {
	        	AccountPayable.findAllToChangeToPaid(new Date(), company)
	        });
			SendDailyTreatmentsReport.send
			JInt(1)
		}

		// ESTA SENDO CHAMADA na sendDailyTreatment
		// tirar de lá se colocar na cron
		case "system" :: "autoChangeToPaid"  :: Nil JsonGet _ => {
	        Company.findAllActiveToChangeFinancialToPaid.foreach( company => {
	        	AccountPayable.findAllToChangeToPaid(new Date(), company)
	        });
			JInt(1)
		}

		case "system" :: "sendDiskSpaceAlert"  :: Nil JsonGet _ => {
			EmailUtil.sendDiskSpaceAlert
			JInt(1)
		}
		
		case "system" :: "sendDailyUtilization"  :: Nil JsonGet _ => {
			EmailUtil.sendDailyUtilization
			JInt(1)
		}
		case "system" :: "sendDailyAccount"  :: Nil JsonGet _ => {
			code.daily.DailyReport.sendAllAccountPayablesMail (new Date(), Company.notifyForToday1Am)
			JInt(1)
		}		
		case "system" :: "sendDailyAccountTomorrow"  :: Nil JsonGet _ => {
			code.daily.DailyReport.sendAllAccountPayablesMail (Project.tomorrow, Company.notifyForTomorrow)
			JInt(1)
		}		
		
		case "system" :: "makeMailAsRead" :: id :: Nil JsonGet _ => {
			val ac = LogMailSend.find(id.toLong).get
			    ac.read_?(true).save;
			    ac.times(ac.times + 1).save;
			JInt(1)
		}
		case "system" :: "createJobs"  :: Nil JsonGet _ => {
			//QuartzUtil.startJobs
			JInt(1)
		}
		
		case "system" :: "createIssue" :: Nil Post _ => {
			def identityInfo = "{Company:"+AuthUtil.company.id.is+", User:"+AuthUtil.user.name.is+"}"
			def body = S.param("body") openOr ""
			def title = S.param("title") openOr ""
			IssueActor ! Issue(title.toString,body+identityInfo)
			JInt(1)
		}

		case "system" :: "memory"  :: Nil JsonGet _ => {
			JsObj(("free",JInt(Runtime.getRuntime.freeMemory)),("total",JInt(Runtime.getRuntime.totalMemory)));
		}

		case "system" :: "threads"  :: Nil JsonGet _ => {
			JsObj(("active",JInt(Thread.activeCount)));
		}

		case "system"::"disk"  :: Nil JsonGet _ => {
			val file = new File("/");
			System.gc
			JsObj(("free",JInt(file.getFreeSpace)),("total",JInt(file.getTotalSpace)));
		}

		case "system"::"database"  :: Nil JsonGet _ => {
			JsObj(("total",dbConnectionsCount));
		}
		case "system"::"changeUnit" :: unit :: Nil JsonGet _ => {
			AuthUtil << CompanyUnit.findByKey(unit.toLong).get
			JInt(1)
		}
	}
	
	def dbConnectionsCount = DB.performQuery("select count(1) as num from pg_stat_activity")._2(0)(0) match {
           case a:Any => a.toString.toInt
           case _ => 0
        }

/*
    def tomorrow = {
      val cal = Calendar.getInstance()
      cal.setTime(new Date()); 
      cal.add(java.util.Calendar.DATE, 1);
      cal.getTime()
    }
*/
}