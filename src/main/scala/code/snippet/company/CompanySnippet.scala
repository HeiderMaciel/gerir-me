
package code 
package snippet 

import net.liftweb._
import http._
import model._
import code.util._
import actors._
import http.js._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{NodeSeq, Text}

object CompanySnippet{
	def apptypes = {
		Seq(
      "1" -> "ebelle",
      "2" -> "gerirme",
      "3" -> "esmile",
      "4" -> "edoctus",
      "5" -> "egrex",
      "6" -> "ephysio",
      "7" -> "ebellepet",
      "8" -> "ebarber"
		)
	}

	def cmdControl = if (PermissionModule.unit_?) {
		Seq(
		Company.CmdNever.toString -> "Nunca incrementa, usuário informa" ,
		Company.CmdDaily.toString -> "Incrementa no dia, no momento do agendamento" ,
		Company.CmdEver.toString -> "Sempre incrementa, no momento da venda" ,
		Company.CmdDailyCompany.toString -> "Incrementa no dia, no momento do agendamento, ignora unidade" ,
		Company.CmdEverCompany.toString -> "Sempre incrementa, no momento da venda, ignora unidade" 
		)
	} else {
		Seq(
		Company.CmdNever.toString -> "Nunca incrementa, usuário informa" ,
		Company.CmdDaily.toString -> "Incrementa no dia, no momento do agendamento" ,
		Company.CmdEver.toString -> "Sempre incrementa, no momento da venda" 
		)
	}

	val cPubControl = Seq(
		Company.CPubNoone.toString -> "Ninguém" ,
		Company.CPubCustomer.toString -> "Clientes" ,
		Company.CPubEveryone.toString -> "Todos"
	)

	def render(in: NodeSeq):NodeSeq  = {
		for {
			r <- S.request if r.post_?
				name <- S.param("name")
				phone <- S.param("phone")
				contact <- S.param("contact")
				email  <- S.param("email")

		}{
			var ac = AuthUtil ? match {
				case true => AuthUtil company
				case _ => Company.create
			}
//			println (S.hostName + "++++++++++++++++++============================")
			  def product1:String = if (S.hostName.contains ("gerir")) {
					ac.appType(2)
					ac.calendarInterval(36)
			      " gerir-me "
			    }else if (S.hostName.contains ("egrex")) {
					ac.appType(5)
			      " e-grex "
			    }else if (S.hostName.contains ("esmile")) {
					ac.appType(3)
					ac.calendarInterval(36)
					ac.bpIdForCompany(2)
			      " e-smile "
			    }else if (S.hostName.contains ("edoctus")) {
					ac.appType(4)
					ac.calendarInterval(30)
					ac.historyInterval(36)
					ac.bpIdForCompany(2)
			      " e-doctus "
			    }else if (S.hostName.contains ("efisio") || S.hostName.contains ("ephysio")) {
			    	ac.calendarStart(7)
			    	ac.calendarEnd(21)
					ac.appType(6)
					ac.calendarInterval(60)
					ac.bpIdForCompany(2)
			      " e-physio "
			    }else if (S.hostName.contains ("ebellepet")) {
			    	ac.calendarStart(8)
			    	ac.calendarEnd(19)
					ac.appType(7)
					ac.calendarInterval(30)
			      " e-bellepet "
			    }else if (S.hostName.contains ("ebarber")) {
			    	ac.calendarStart(8)
			    	ac.calendarEnd(19)
					ac.appType(8)
					ac.calendarInterval(30)
			      " e-bellepet "
			    }else{
					ac.appType(1)
					ac.calendarInterval(30)
			      " e-belle "
			    }
			ac.name(name)
			ac.phone(phone)
			ac.contact(contact)
			ac.email(email)
			ac.obs(product1)
			ac.validate match {
				case Nil =>{
						ac.save
						UserCreateActors ! ac
						return{
								<h2>
									Processo de cadastro iniciado<br/> 
									com sucesso para empresa<br/>
									{ac.name}.
								</h2>

								<div>
									Aguarde o recebimento do e-mail de confirmação.
								</div>
								<div>
									Atenção: a mensagem pode cair na caixa de spam, <br/>
									junk, ou lixo eletrônico, verifique caso o e-mail <br/>
									não chegue em alguns minutos.
								</div>
						}
				}
				case xs => {
					S.error(xs)
				}
			}
		}
		in
	}
	def ac = AuthUtil company
	def name = {
		<span>{ac.name.is}</span>
	}
	def phone = {
		<span>{ac.phone.is}</span>
	}	

	def thumbh = {	
		// mostrar logo da unidade ao invés do da empresa
		// tem que tirar o false do if para validar
		// verificar 
		// vilarika  exemplo  pierre sergio hugo aptus
		val logoUnit = if(AuthUtil.?) {
			AuthUtil.unit.imagethumb.is match {
			case img:String if(img != "") => {
				AuthUtil.unit.thumb_web
			}
			case _ => ""
			}
		} else {
			""
		}
		ac.imagethumb.is match {
			case img:String if(img != "") => {
				if (logoUnit != "" && 
					PermissionModule.unit_? && false) {
					//<img width="70px" src={ac.thumb_web}/>
	    			<img height="30px" src={logoUnit}/>
				} else {
					if (Project.isLocalHost) {
						<img height="24px" class="borderLocal" src={ac.thumb_web}/>
					} else {
						<img height="30px" src={ac.thumb_web}/>
					}
				}
			}
			case _ => <img height="30px" style="padding-right: 10px" src="/images/logo.png"/>
		}
	}

	def thumb = {	
		// mostrar logo da unidade ao invés do da empresa
		// tem que tirar o false do if para validar
		// verificar 
		// vilarika  exemplo  pierre sergio hugo aptus
		val logoUnit = if(AuthUtil.?) {
			AuthUtil.unit.imagethumb.is match {
			case img:String if(img != "") => {
				AuthUtil.unit.thumb_web
			}
			case _ => ""
			}
		} else {
			""
		}
		ac.imagethumb.is match {
			case img:String if(img != "") => {
				if (logoUnit != "" && PermissionModule.unit_? && false) {
					//<img width="70px" src={ac.thumb_web}/>
	    			<img width="70px" src={logoUnit}/>
				} else {
					if (Project.isLocalHost) {
						<img width="64px" class="borderLocal" src={ac.thumb_web}/>
					} else {
						<img width="70px" src={ac.thumb_web}/>
					}
				}
			}
			case _ => <img width="70px" style="padding-right: 10px" src="/images/logo.png"/>
		}
	}

	def logo = {	
		ac.image.is match {
			case img:String if(img != "") => {
				<img width="110px" src={ac.logo_web}/>
			}
			case _ => <img width="110px" src="/images/logo.png"/>
		}
	}	
	def form = {
			def process(): JsCmd= {
				try {
					ac.save	
					ac.calendarStatuses(S.params("calendarStatuses").foldLeft("")(_+","+_))
				   	S.notice("Alterado com sucesso!")
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
			//
//		    "name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&
		    "name=status" #>(SHtml.text(ac.status.is.toString,(s:String) => ac.status(s.toInt)))&
//		    "name=apptype" #>(SHtml.text(ac.appType.is.toString,(s:String) => ac.appType(s.toInt)))&
		    "name=apptype" #> (SHtml.select(apptypes,Full(ac.appType.is.toString),(v:String) => ac.appType(v.toInt)))&
		    "name=bpIdForCompany" #>(SHtml.text(ac.bpIdForCompany.is.toString,(s:String) => ac.bpIdForCompany(s.toInt)))&
			"name=userActivityAssociate" #> (SHtml.checkbox(ac.userActivityAssociate_?, ac.userActivityAssociate_?(_)))&
			"name=showSalesToUser" #> (SHtml.checkbox(ac.showSalesToUser_?, ac.showSalesToUser_?(_)))&
			"name=useTreatmentAsAClass" #> (SHtml.checkbox(ac.useTreatmentAsAClass_?, ac.useTreatmentAsAClass_?(_)))&
//			"name=autoIncrementCommand" #> (SHtml.checkbox(ac.autoIncrementCommand_?, ac.autoIncrementCommand_?(_)))&
		    "name=commandControl" #> (SHtml.select(cmdControl,Full(ac.commandControl.is.toString),(v:String) => ac.commandControl(v.toInt)))&
		    "name=calendarPub" #> (SHtml.select(cPubControl,Full(ac.calendarPub.is.toString),(v:String) => ac.calendarPub(v.toInt)))&
			"name=hoursInAdvancePub" #> (SHtml.text(ac.hoursInAdvancePub.is.toString, 
				(f:String) => ac.hoursInAdvancePub(BusinessRulesUtil.snippetToInt(f))))&
			"name=hoursToCancelPub" #> (SHtml.text(ac.hoursToCancelPub.is.toString, 
				(f:String) => ac.hoursToCancelPub(BusinessRulesUtil.snippetToInt(f))))&
			"name=hoursToConfirmPub" #> (SHtml.text(ac.hoursToConfirmPub.is.toString, 
				(f:String) => ac.hoursToConfirmPub(BusinessRulesUtil.snippetToInt(f))))&
		    "name=calendarUrl" #> (SHtml.text(ac.calendarUrl.is,ac.calendarUrl(_)))&
			"name=autoOpenCalendar" #> (SHtml.checkbox(ac.autoOpenCalendar_?, ac.autoOpenCalendar_?(_)))&
			"name=allowrepeatcommand" #> (SHtml.checkbox(ac.allowRepeatCommand_?, ac.allowRepeatCommand_?(_)))&
//			"name=notify" #> (SHtml.checkbox(ac.senNotifications_?, ac.senNotifications_?(_)))&
		    "name=financialNotification" #>(SHtml.text(ac.financialNotification.is.toString,(s:String) => ac.financialNotification(s.toInt)))&
		    "name=customerNotification" #>(SHtml.text(ac.customerNotification.is.toString,(s:String) => ac.customerNotification(s.toInt)))&
		    "name=userNotification" #>(SHtml.text(ac.userNotification.is.toString,(s:String) => ac.userNotification(s.toInt)))&
		    "name=partner" #>(SHtml.text(ac.partner.is.toString,(s:String) => ac.partner(s.toInt)))&
		    "name=name" #> (SHtml.text(ac.name.is,ac.name(_)))&
		    "name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_)))&
		    "name=toCancelAnAppointment" #> (SHtml.textarea(ac.toCancelAnAppointment.is, ac.toCancelAnAppointment(_)))&
		    "name=mailAppointmentSignature" #> (SHtml.textarea(ac.mailAppointmentSignature.is, ac.mailAppointmentSignature(_)))&
//		    "name=calendarStatuses" #> (SHtml.textarea(ac.calendarStatuses.is, ac.calendarStatuses(_)))&
		    "name=bpmStartDay" #>(SHtml.text(ac.bpmStartDay.is.toString,(s:String) => ac.bpmStartDay(s.toInt)))&
		    "name=bpmDaysToAlert" #>(SHtml.text(ac.bpmDaysToAlert.is.toString,(s:String) => ac.bpmDaysToAlert(s.toInt)))&
		    "name=bpmDaysToEmail" #>(SHtml.text(ac.bpmDaysToEmail.is.toString,(s:String) => ac.bpmDaysToEmail(s.toInt)))&
			"name=bpmCommissionOnSale" #> (SHtml.checkbox(ac.bpmCommissionOnSale_?, ac.bpmCommissionOnSale_?(_)))&
			"name=bpmCommissionOnReady" #> (SHtml.checkbox(ac.bpmCommissionOnReady_?, ac.bpmCommissionOnReady_?(_)))&
			"name=bpmCommissionOnMissed" #> (SHtml.checkbox(ac.bpmCommissionOnMissed_?, ac.bpmCommissionOnMissed_?(_)))&
			"name=offCommissionOnReady" #> (SHtml.checkbox(ac.offCommissionOnReady_?, ac.offCommissionOnReady_?(_)))&
			"name=offCommissionOnMissed" #> (SHtml.checkbox(ac.offCommissionOnMissed_?, ac.offCommissionOnMissed_?(_)))&
			"name=packCommissionOnReady" #> (SHtml.checkbox(ac.packCommissionOnReady_?, ac.packCommissionOnReady_?(_)))&
			"name=packCommissionOnMissed" #> (SHtml.checkbox(ac.packCommissionOnMissed_?, ac.packCommissionOnMissed_?(_)))&
			"name=cmdChangeDate" #> (SHtml.checkbox(ac.cmdChangeDate_?, ac.cmdChangeDate_?(_)))&
			"name=cmdAskCommand" #> (SHtml.checkbox(ac.cmdAskCommand_?, ac.cmdAskCommand_?(_)))&
			"name=cmdAskProfessional" #> (SHtml.checkbox(ac.cmdAskProfessional_?, ac.cmdAskProfessional_?(_)))&
			"name=categoryOnProduct" #> (SHtml.checkbox(ac.categoryOnProduct_?, ac.categoryOnProduct_?(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is,ac.short_name(_)))&
		    "name=phone" #>(SHtml.text(ac.phone.is,ac.phone(_)))&
		    "name=historyInterval" #>(SHtml.text(ac.historyInterval.is.toString,(s:String) => ac.historyInterval(s.toInt)))&
		    "name=calendarInterval" #>(SHtml.text(ac.calendarInterval.is.toString,(s:String) => ac.calendarInterval(s.toInt)))&
		    "name=calendarIntervalAlt" #>(SHtml.text(ac.calendarIntervalAlt.is.toString,(s:String) => ac.calendarIntervalAlt(s.toInt)))&
			"name=calendarStart" #>(SHtml.text(ac.calendarStart.is.toString,(s:String) => ac.calendarStart(s.toInt)))&		    
			"name=calendarEnd" #>(SHtml.text(ac.calendarEnd.is.toString,(s:String) => ac.calendarEnd(s.toInt)))&
			"name=calendarStatuses_text" #> (SHtml.text(ac.calendarStatuses, (a:String) => {}))&
			"name=calendarShowId" #> (SHtml.checkbox(ac.calendarShowId_?, ac.calendarShowId_?(_)))&
			"name=calendarShowPhone" #> (SHtml.checkbox(ac.calendarShowPhone_?, ac.calendarShowPhone_?(_)))&
			"name=calendarShowLight" #> (SHtml.checkbox(ac.calendarShowLight_?, ac.calendarShowLight_?(_)))&
			"name=calendarShowInterval" #> (SHtml.checkbox(ac.calendarShowInterval_?, ac.calendarShowInterval_?(_)))&
			"name=calendarShortActivity" #> (SHtml.checkbox(ac.calendarShortActivity_?, ac.calendarShortActivity_?(_)))&
			"name=calendarShowActivity" #> (SHtml.checkbox(ac.calendarShowActivity_?, ac.calendarShowActivity_?(_)))&
			"name=calendarShowDifUnit" #> (SHtml.checkbox(ac.calendarShowDifUnit_?, ac.calendarShowDifUnit_?(_)))&
			"name=calendarShowSponsor" #> (SHtml.checkbox(ac.calendarShowSponsor_?, ac.calendarShowSponsor_?(_)))&
			"name=calendarAllAsConfirmed" #> (SHtml.checkbox(ac.calendarAllAsConfirmed_?, ac.calendarAllAsConfirmed_?(_)))&
			"name=calendarAllAsArrived" #> (SHtml.checkbox(ac.calendarAllAsArrived_?, ac.calendarAllAsArrived_?(_)))&
		    "name=contact" #>  (SHtml.text(ac.contact.is,ac.contact(_)))&
		    "name=website" #> (SHtml.text(ac.website.is, ac.website(_))) &
		    "name=email" #> (SHtml.text(ac.email.is,ac.email(_)))&
			"name=email_dis" #> (SHtml.text(ac.email.is,s => s,"disabled" -> "true")++SHtml.hidden(process))
		
	}

}

