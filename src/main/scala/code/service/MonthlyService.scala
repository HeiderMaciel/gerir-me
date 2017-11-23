package code
package service

import net.liftweb._
import mapper._ 
import code.util._
import code.comet._
import model._
import net.liftweb.util._
import http.js._
import JE._
import net.liftweb.common._
//import InventoryMovement._
import java.util.Date
import java.util.Calendar

object MonthlyService {
	val MONTHLY_ACTIVITY_TYPE = 983
	private def findMonthlyNotPaid(customer:Customer, company:Company, date:Date) = {
		Treatment.findAll(
			By(Treatment.company, 1),
			By(Treatment.customer, customer),
			NotBy(Treatment.status, Treatment.Paid),
			NotBy(Treatment.status, Treatment.Deleted),
			BySql("dateEvent >=? and dateEvent < (date(?)+12)",IHaveValidatedThisSQL("",""),date, date),
			BySql("id not in(select treatment from monthly where company_customer=? and treatment is not null)",IHaveValidatedThisSQL("",""),company.id.is),
			BySql(""" id in(
							select td.treatment
							from 
							treatmentdetail td 
							where
							td.treatment = treatment.id
							and 
							td.activity in (select id from product pd where pd.typeproduct = ?)
							and td.company=1
					)""",IHaveValidatedThisSQL("",""),MONTHLY_ACTIVITY_TYPE)
			)
	}
	private def hasDelivery(customer:Customer, productId:Long) = {
		customer.deliveryDetailNotUsed(productId).size > 0
	}
	def generateMonthly(customer:Customer, company:Company, date:Date) = {
		val monthlyNotPaid = findMonthlyNotPaid(customer, company, date)
		if(!monthlyNotPaid.isEmpty){
			val treatmentMonthly = monthlyNotPaid(0)
			if(!hasDelivery(customer, treatmentMonthly.details(0).activity.is)){
				if(!monthlyNotPaid.isEmpty){
					val monthlyDescription = "Mensalidade %s".format(treatmentMonthly.detailTreatmentAsText.is)
					val monthly = Monthly.createMonthly(Company.findByKey(1).get, 
						//company, 
						company,treatmentMonthly.totalValue(0).toDouble, treatmentMonthly.dateEvent, monthlyDescription)
//					val usersToNotify = User.findAll(By(User.company, company),BySql[User](" (groupPermission LIKE '%,1'  or  groupPermission LIKE '%,1,%'  or groupPermission LIKE '1') ",IHaveValidatedThisSQL("","") ), NotBy(User.email,""))
					val usersToNotify = Monthly.usersToNotify(company)
					val of = User.findByKey(1).get				
					monthly.treatment(treatmentMonthly)
					.table("treatment")
					.business_pattern(company.partner)
					.idObj(treatmentMonthly.id.is)
					.save
					usersToNotify.map((user:User)=>{
						val users = user.id.is:: Nil
						val message = """Olá %s,
							<br><br> informamos que já encontra-se disponível para pagamento a %s 
							<br>de <b>%s</b>, no valor de <b>%.2f</b> 
							<br><br>Para o dia: <b>%s</b>
							<br><br>Linha digitável: <b>%s</b>
							<br><br>faça seu login e posteriormente clique <a href='/financial/monthly'>aqui</a> para gerar o boleto!<br><br/>
							Ou já logado, acesse menu superior, seu usuário, mensalidade
							<br/>
							<br/>
							<img src="http://ebelle.vilarika.com.br/images/mensal_menu1.png" style="width: 400px;"/>
							<br/>
							<br/>
							<img src="http://ebelle.vilarika.com.br/images/mensal_print1.png" style="width: 600px;"/>
							""".format(user.name.is, monthlyDescription, company.name,monthly.value.is, Project.dateToStr(monthly.dateExpiration), monthly.editableLine.is)
						val calendar = Calendar.getInstance()
						calendar.set(Calendar.YEAR,2029);
						val expirationdate = calendar.getTime()
						UserMessage.build(monthlyDescription, message, of, 0, users, company, UserMessage.SYSTEM, expirationdate)
						EmailUtil.sendMailTo(user.email.is, 
							scala.xml.Unparsed(message.replace("/financial/monthly","http://" + company.appShortName + ".vilarika.com.br/financial/monthly")), 
							monthlyDescription, company)
					})
				}
			}
		}
	}

	def generateMonthlyAllCompanies(date:Date = new Date()) = {
		Company.findAll(By (Company.status, 1),
			OrderBy (Company.id, Ascending)).map((company:Company)=>{
			company.partner.obj match {
				case Full(customer)=>{
					generateMonthly(customer, company, date)
				}
				case _ => 
			}
		})
	}

}
