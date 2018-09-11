package code.daily

import net.liftweb._
import http._
import mapper._ 
import code.model.{Company, CompanyUnit, Customer, User, Monthly, UserMessage, Treatment, InventoryMovement, AccountPayable}
import code.service.TreatmentService
import code.util.{Project, EmailUtil}
import java.util.Date
import java.util.Calendar
import java.io.File
import scala.xml.XML

object DailyReport{

    def sendTreatmentReport (date: Date, notify: Int) = {
        Company.findAllActiveToSendNotification (notify).foreach( company => {
            if (company.customerNotification == notify) {
                val customers = TreatmentService.customersWithTreatments(company, date)
                customers.foreach((customer)=>{
                    var cunit = if (!customer.unit.isEmpty) {
                            CompanyUnit.findByKey(customer.unit).get
                        } else {
                            // unidade vilarika
                            CompanyUnit.findByKey(7).get
                        }
                    if(customer.email.is != ""){
                        val mail = code.daily.DailyReport.treatmentsTodayCustomer(customer,company, date)
                        //EmailUtil.sendMailTo(customer.email.is.toString,mail,"Atendimento "+company.name.is,company)
                        EmailUtil.sendMailCustomer(cunit,
                            Company.findByKey (customer.company).get, 
                            customer.email.is, mail, "Atendimento "+company.name.is, 
                            customer.id.is)
                    }
                })
            }
            if (company.userNotification == notify) {
                val users = TreatmentService.usersWithTreatments(company, date)
                users.foreach((user)=>{
                    var uunit = if (!user.unit.isEmpty) {
                            CompanyUnit.findByKey(user.unit).get
                        } else {
                            // unidade vilarika
                            CompanyUnit.findByKey(7).get
                        }
                    if(user.email.is != ""){
                        val mail = code.daily.DailyReport.treatmentsTodayUser(user,company, date)
                        //EmailUtil.sendMailTo(user.email.is.toString,mail,"Agenda "+company.name.is,company)
                        EmailUtil.sendMailCustomer(uunit,
                            Company.findByKey (user.company).get, 
                            user.email.is, mail, "Agenda "+company.name.is, 
                            user.id.is)
                    }
                })
            }
        })        
    }

	def sendAllTodayMonthlyMail = Monthly.findAllToday.foreach( monthly => {
	        val company_customer = Company.findByKey (monthly.company_customer.is).get
			val usersToNotify = Monthly.usersToNotify(company_customer)

			val of = User.findByKey(1).get				
			usersToNotify.map((user:User)=>{
		        val mail = DailyReport.monthlyHtml(company_customer,monthly,user)
				val users = user.id.is:: Nil
				val message = ("Olá %s, informamos que vence hoje a %s, clique <a href='/financial/monthly'>aqui</a> para gerar o boleto!" +
				"</br>Caso o pagamento já tenha sido efetuado, favor desconsiderar a mensagem. ").format(user.name.is, monthly.description)
				val calendar = Calendar.getInstance()
				calendar.set(Calendar.YEAR,2029);
				val expirationdate = calendar.getTime()
				UserMessage.build(monthly.description, 
					message.replace("/financial/monthly","http://" + company_customer.appShortName + ".vilarika.com.br/financial/monthly"), 
					of, 0, users, company_customer, UserMessage.SYSTEM, expirationdate)
				//EmailUtil.sendMailTo(user.email.is, 
	            EmailUtil.sendMailTo(user.email.is,mail, company_customer.name.is + " boleto vencendo hoje")			
	        });
	    })
    
	def sendAllAccountPayablesMail (date: Date, notify: Int) = Company.findAllActiveToSendNotification (notify).foreach( company => {
		if (company.financialNotification == notify) {
			val usersToNotify = Monthly.usersToNotify(company)
			val of = User.findByKey(1).get				
			if(code.daily.DailyReport.hasAccountsFor(company, date)){
		        val mail = code.daily.DailyReport.accountPayablesHtml(company, date)
				usersToNotify.map((user:User)=>{
                    if(user.email.is != ""){
	                    var uunit = if (!user.unit.isEmpty) {
	                            CompanyUnit.findByKey(user.unit).get
	                        } else {
	                            // unidade vilarika
	                            CompanyUnit.findByKey(7).get
	                        }
				        //EmailUtil.sendMailTo(company,mail, company.name.is + " contas a pagar/receber hoje")			
	                    EmailUtil.sendMailCustomer(uunit,
	                        Company.findByKey (user.company).get, 
	                        user.email.is, mail, company.name.is + " contas a pagar/receber " + extDay(date), 
	                        user.id.is)
	                }
			    })    
			}
		}
    })

	def sendMailBp (id: Long, title: String, mail: String) = {
        val customer = Customer.findByKey(id).get
        val company = Company.findByKey(customer.company).get
        var cunit = if (!customer.unit.isEmpty) {
            CompanyUnit.findByKey(customer.unit).get
        } else {
            // unidade vilarika
            CompanyUnit.findByKey(7).get
        }
        // o loadString - valida o xml - tem que estar bem formado
        //var mailxml = XML.loadString (mail) //<div> {mail} </div>
        var mailxml = scala.xml.Unparsed (mail)
        if(customer.email.is != ""){
            EmailUtil.sendMailCustomer(cunit,
                Company.findByKey (customer.company).get, 
                customer.email.is, mailxml, title + " " +company.name.is, 
                customer.id.is)
        }
    }

	def treatmentsTodaySms (customer:Customer,company:Company, date:Date = new Date) = {
		val treatments = TreatmentService.loadTreatmentByCustomer(customer, date,company)
		val num = treatments.size
		val treatment = treatments(0)
		val description = extDay (date);
		val message ="""Atendimento %s no %s, As %s""".format(description, company.name.is, Project.dateToHours(treatment.start.is), Project.dateToHours(treatment.end.is))
		message
	}

	def extDay (date : Date) = {
		val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
		def daySend = {
			val cal = Calendar.getInstance()
			cal.setTime(date)
			cal.get(Calendar.DAY_OF_YEAR)
		}

	    def tomorrow = {
	      val cal = Calendar.getInstance()
	      cal.setTime(new Date()); 
	      cal.add(java.util.Calendar.DATE, 1);
	      cal.get(Calendar.DAY_OF_YEAR)
	    }
		val description = if(today == daySend){
			"Hoje " + Project.dateToStr(date)
		}else{
			if (daySend == tomorrow) {
				"Amanhã " + Project.dateToStr(date)
			} else {
				Project.dateToStr(date)
			}
		}
		description
	}

	def treatmentsTodayCustomer (customer:Customer,company:Company, date:Date = new Date) = {
		val treatments = TreatmentService.loadTreatmentByCustomer(customer, date,company)
		val num = treatments.size
		val description = extDay (date)
		val confirmDisplay = if (company.hoursToConfirmPub != 0) {
			"visible"
		} else {
			"none"
		}
		val cancelDisplay = if (company.hoursToCancelPub != 0) {
			"visible"
		} else {
			"none"
		}

		def url (company:Company) = if (Project.isLocalHost) {
           "localhost:7171/"
        } else {
	       "http://" + company.appShortName + ".vilarika.com.br/"
        }

		val xml = <div>
					<img width="100px" src={company.thumb_web}/>
					<br/>
					Olá {customer.name.is}, <br/><br/>{description} você tem {num} atendimento(s) marcado(s), abaixo detalhes:
					{
						treatments.map(
							a =>
							(
								<div>
									<br/>
									<b>Profissional</b> : {a.userName}<br/>
										<b>Horário</b> : de {Project.dateToHours(a.start.is)} até {Project.dateToHours(a.end.is)}
									<br/>
										<b>Atendimentos</b> : {a.descritionDetails}
									<br/>
										<b>Local</b> : {company.name.is} - Unidade {a.unit.obj.get.name}
									<br/>
										<b>Telefone</b> : {company.phone.is}
									<br/>
										<b>Endereço</b> : {a.unit.obj.get.getPartner.full_address}
									<br/>
									<hr/>
								</div>
								<div style={"display:"+ confirmDisplay +""}>
									<p><b>
									Para confirmar sua presença, por favor, <a href = {url (company) + 
									"customer_confirmation?id=" + 
									a.id + "," + a.company + "," + 
									a.customer + "," + a.user + 
									",6"}>clique aqui</a>.
									</b></p>
								</div>
								<div style={"display:"+ cancelDisplay +""}>
									<p><b>
									Para desmarcar esse atendimento, por favor, <a href = {url (company) + 
									"customer_confirmation?id=" + 
									a.id + "," + a.company + "," + 
									a.customer + "," + a.user + 
									",8"}>clique aqui</a>.
									</b></p>
								</div>
							)
						)
					}
					<p>
					{company.toCancelAnAppointment.is}
					</p>
				  </div>
		xml		
	}
	def treatmentsTodayUser (user:User,company:Company, date:Date = new Date) = {
		val treatments = TreatmentService.loadTreatmentByUser(user, date,company)
		val num = treatments.size
		def persontype = if (company.appType.isEgrex) {
			"Membro"
		}else{
			"Cliente"
		}

		val description = extDay (date);
		val xml = <div>
					<img width="100px" src={company.thumb_web}/>
					<br/>
					Olá {user.name.is}, <br/><br/>{description} você tem {num} atendimento(s) marcado(s), abaixo detalhes:
					{
						treatments.map(
							a => {
							val colornow = "background-color:" + a.colorByDetails + ";"
							(
								<div>
									<br/>
										<b>{persontype}</b> : {a.customerName}
									<br/>
										<b>Telefone</b> : {a.customer.obj.get.phone.is + ' ' + a.customer.obj.get.mobilePhone.is + ' ' + a.customer.obj.get.email_alternative.is}
									<br/>
										<b>Email</b> : {a.customer.obj.get.email.is}
									<br/>
										<b>Horário</b> : de {Project.dateToHours(a.start.is)} até {Project.dateToHours(a.end.is)}
									<br/>
									<span style={colornow}>
										<b>Atendimentos</b></span> : {a.descritionDetails}
									<br/>
										<b>Obs</b> : {a.obs}
									<br/>
										{a.whereIs}
									<br/>
										<b>Local</b> : {company.name.is} - Unidade {a.unit.obj.get.name}
									<hr/>
								</div>
							)}
						)
					}
				  </div>
		xml		
	}

	def treatmentsRankCustomer (customer:Customer,company:Company, date:Date = new Date) = {
		val treatments = TreatmentService.loadTreatmentByCustomer(customer, date,company)
		val num = treatments.size
		val description = extDay (date)

		def url (company:Company) = if (Project.isLocalHost) {
           "localhost:7171/"
        } else {
	       "http://" + company.appShortName + ".vilarika.com.br/"
        }

		val xml = <div>
					<img width="100px" src={company.thumb_web}/>
					<br/>
					Olá {customer.name.is}, <br/><br/>{description} você tem {num} atendimento(s) marcado(s), abaixo detalhes:
					{
						treatments.map(
							a =>
							(
								<div>
									<br/>
									<b>Profissional</b> : {a.userName}<br/>
										<b>Horário</b> : de {Project.dateToHours(a.start.is)} até {Project.dateToHours(a.end.is)}
									<br/>
										<b>Atendimentos</b> : {a.descritionDetails}
									<br/>
										<b>Local</b> : {company.name.is} - Unidade {a.unit.obj.get.name}
									<br/>
										<b>Telefone</b> : {company.phone.is}
									<br/>
										<b>Endereço</b> : {a.unit.obj.get.getPartner.full_address}
									<br/>
									<hr/>
								</div>

<table align="left" border="0" cellpadding="0" cellspacing="0">
<tbody>
<tr>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#B72025">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=0" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">0</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#D62027">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=1" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">1</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#F05223">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=2" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">2</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#F36F21">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=3" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">3</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#FAA823">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=4" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">4</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#FFCA27">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=5" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">5</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#ECDB12">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=6" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">6</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#E8E73D">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=7" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">7</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#C5D92D">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=8" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">8</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#AFD136">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=9" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">9</span> </a></td>
<td width="4" height="50" style="width:2px; height:50px; line-height:0; font-size:0">
</td>
<td align="center" valign="middle" style="width:50px; height:50px; border-radius:25px; background:#64B64D">
<a href="https:/tracksale.co/s/sao-marcos_fcc21a4d0e9b6f728ca3fa119fabb9f9?aNPS=10" target="_blank" rel="noopener noreferrer" data-auth="NotApplicable" style="font-size:16px; font-weight:bold; font-family:Helvetica Neue,Helvetica,Arial,sans-serif; text-decoration:none; line-height:50px; display:block"><span style="color:#ffffff">10</span> </a></td>
</tr>
</tbody>
</table>
<br/><br/><br/>
							)
						)
					}
				  </div>
		xml		
	}

	def monthlyHtml(company:Company,monthly:Monthly,user:User) = {
		val xml = 
			<div>
				Olá {user.name.is}, 
				<br/>
				<br/>
				Há um boleto de {company.name.is} vencendo hoje.
				<br/>
				<br/>
				Dados do Boleto
				<br/>
				Descrição: {monthly.description}
				<br/>
				Valor: <b>{"%.2f".format (monthly.value.is)}</b>
				<br/>
				Data Vencimento: {Project.dateToStr(monthly.dateExpiration)}
				<br/>
				<br/>
				Linha Digitável: <b>{"%s".format (monthly.editableLine.is)}</b>
				<br/>
				<br/>
				Caso o pagamento já tenha sido efetuado, favor desconsiderar a mensagem.
				<br/>
				<br/>
				Para gerar o boleto acesse menu superior, seu usuário, mensalidade
				<br/>
				<br/>
				<img src="http://ebelle.vilarika.com.br/images/mensal_menu1.png" style="width: 400px;"/>
				<br/>
				<br/>
				<img src="http://ebelle.vilarika.com.br/images/mensal_print1.png" style="width: 600px;"/>
		</div>
		xml
	}

	def accountPayablesHtml(company:Company, date: Date) = {
		val accounts = accountsFor (company, date)
		val num = accounts.size
		val description = extDay (date);
		// atentitiva de colocar o texto branco não funcionou #ffffff
		// tentei tirar class="bordered-table zebra-striped" , mas tb não funcionou
		// heigth="30" e 60 no <tr>  e nos <td> sem sucesso
		val xml = <span>
					<img width="100px" src={company.thumb_web}/>
					<br/>
					Bom dia, <br/>
					{description} você tem {num} conta(s) vencendo.
						<table style="border: 1px solid; border-collapse: collapse;" class="bordered-table zebra-striped">
							<thead style="border: 1px solid; border-collapse: collapse;" color="#ffffff" bgcolor="#006ca6">
								<th style="border: 1px solid; border-collapse: collapse;">Id</th>
								<th style="border: 1px solid; border-collapse: collapse;">Descrição</th>
								<th style="border: 1px solid; border-collapse: collapse;">Categoria</th>
								<th style="border: 1px solid; border-collapse: collapse;">Valor</th>
								<th style="border: 1px solid; border-collapse: collapse;">Ent/Sai</th>
								<th style="border: 1px solid; border-collapse: collapse;">Conta</th>
								<th style="border: 1px solid; border-collapse: collapse;">Pago?</th>
							</thead>
							<tbody>
								{
									accounts.map(a =>
										(
											<tr>
												<td style="border: 1px solid; border-collapse: collapse;">
													#{a.id}
												</td>
												<td style="border: 1px solid; border-collapse: collapse;">
													{a.obs.is}
												</td>
												<td style="border: 1px solid; border-collapse: collapse;">
													{a.category.obj.get.name}
												</td>
												<td style="border: 1px solid; border-collapse: collapse;">
													R$ {a.value.is}
												</td>
												<td style="border: 1px solid; border-collapse: collapse;">
												    {if(a.typeMovement == AccountPayable.IN)
												      "Entrada"
												    else
												      "Saída"
												    }
												</td>
												<td style="border: 1px solid; border-collapse: collapse;">
													{a.accountShortName}
												</td>
												<td style="border: 1px solid; border-collapse: collapse;">
												    {if(a.paid_?)
												      "Sim"
												    else
												      "Não"
												    }
												</td>
											</tr>
										)
									)
								}
							</tbody>
						</table>
				  </span>
		xml
	}

	def dailyUtilizationInHtml ={
		val day = dailyUtilization
		val xml = 
			<div>
					<h3>
						Resumo Utilização Diária
					</h3>
				<br/>
					<h3>
						Clientes: {day.customers}
					</h3>
				<br/>
					<h3>
						Usuários: {day.users}
					</h3>
				<br/>
					<h3>
						Atendimentos: {day.treatments}
					</h3>
				<br/>
					<h3>
						Empresas: {day.companies}
					</h3>
				<br/>
					<h3>
						Mov estoque: {day.inventoryMovements}
					</h3>
				<br/>
					<h3>
						Contas a pagar: {day.accountsCreatedToday}
					</h3>
				<br/>					
					<hr/>
				<br/>
			 </div>
		xml
	}

	def diskSpaceAlertInHtml ={
		val disk = diskSpace
		val xml = 
			<div>
					<h3>
						Utilização de Disco
					</h3>
				<br/>
					<h3>
						Usado: {disk.used}
					</h3>
				<br/>
				<br/>
					<h3>
						Livre: {disk.free} Total: {disk.total} GB
					</h3>
				<br/>
			 </div>
		xml
	}

	def diskSpace = 
		DiskSpace(diskUsed, diskFree, diskTotal)

	def diskUsed : Float = {
		val file = new File("/");
		System.gc
		(file.getTotalSpace - file.getFreeSpace) * 100 / file.getTotalSpace
	}
	def diskFree : Float = {
		val file = new File("/");
		System.gc
		(file.getFreeSpace) * 100 / file.getTotalSpace
	}

	def diskTotal : Float = {
		val file = new File("/");
		System.gc
		file.getTotalSpace / 1024 / 1024 / 1024
	}

	def dailyUtilization = 
		DailyUtilization(customerCreatedToday,
						 usersCreatedToday,
						 treatmentCreatedToday,
						 companiesCreatedToday,
						 inventoryMoventedToDay,
						 accountsCreatedToday
						 )

	def treatmentCreatedToday = Treatment.count(sqlForToDay, By(Treatment.hasDetail, true))
	def customerCreatedToday = Customer.count(sqlForToDay)
	def usersCreatedToday = User.count(sqlForToDay)
	def companiesCreatedToday = Company.count(sqlForToDay)
	def inventoryMoventedToDay = InventoryMovement.count(sqlForToDay)
	def accountsCreatedToday = AccountPayable.count(sqlForToDay)

	// retirei o By(AccountPayable.paid_?,false), pq vários clientes querem marcar o cheque como recebe a vista
	// mas querem ser lembrados do dia do depósito
	def accountsFor (company:Company, date: Date) = AccountPayable.findAll(
		By(AccountPayable.company,company),
		By(AccountPayable.toConciliation_?,false),
		BySql("""paymenttype is null or paymenttype in (select pt.id from paymenttype pt where
		pt.usernotification = true
		and pt.company = ?)""", IHaveValidatedThisSQL("dueDate", "01-01-2012 00:00:00"),
		company.id.is.toLong),
		BySql("date(dueDate) = date(?)",
		IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), date))
	def hasAccountsFor (company:Company, date: Date) = AccountPayable.count(
		By(AccountPayable.company,company),
		By(AccountPayable.toConciliation_?,false),
		BySql("""paymenttype is null or paymenttype in (select pt.id from paymenttype pt where
		pt.usernotification = true
		and pt.company = ?)""", IHaveValidatedThisSQL("dueDate", "01-01-2012 00:00:00"),
		company.id.is.toLong),
		BySql("date(dueDate) = date(?)",
		IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), date)) > 0

	def sqlForToDay[T <: net.liftweb.mapper.Mapper[T]] = BySql[T](
		"date(createdat) = date(?)",
		IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"),new Date())


}



case class DailyUtilization(val customers:Long,val users:Long,val treatments:Long,val companies:Long,val inventoryMovements:Long,accountsCreatedToday:Long);

case class DiskSpace(val used:Float, val free:Float, val total:Float);



