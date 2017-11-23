package code
package snippet

import net.liftweb._
import http._
import code.util._
import code.service._
import model._
import code.actors._
import http.js._
import JE._
import JsCmds._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text }
import java.util.Date

object  ReportSales extends net.liftweb.common.Logger{

	def strToDate(s:String) = try{
			Project.strToDate(s+" 00:00")
			}catch{ 
				case _ => new Date()
	}

	def cashier:List[Long] = {
		S.param("cashier") match {
			case Full(s) if(s != "") => S.params("cashier").map(_.toLong)
			case _ => S.param("number") match {
				case Full(number) if(number != "") => number.split(",").map( (n) => Cashier.findOpenCashierByIdAndCompany(n.toInt).id.is ).toList
				case _ => Nil
			}
		}		
	}

	def payment_type = S.param("payment_type") match {
		case Full(s) if(s != "") => S.params("payment_type").map(_.toLong)
		case _ => Nil
	}	

	def startDate = S.param("startDate") match {
		case Full(s) if(s != "") => strToDate(s)
		case _ => if(cashier != Nil) new Date(1l) else new Date();
	}

	def showProducts = S.param("productclass") match {
		case Full(s) if(s != "") => s.contains("1")
		case _ => false
	}

	def showServices = S.param("productclass") match {
		case Full(s) if(s != "") => s.contains("0")
		case _ => false
	}	

	def user = S.param("user") match {
		case Full(s) if(s != "") => s.toLong
		case _ => 0l;
	}

	def commands:List[String] = S.param("commands") match {
		case Full(s) if(s != "") => s.split(",").map(_.trim).toList
		case _ => Nil;
	}	

	def endDate =  S.param("endDate") match {
		case Full(s) if(s != "") => strToDate(s)
		case _ => new Date();
	}
	
	def report(xhtml: NodeSeq): NodeSeq = {

		TreatmentService.treatmentsPayadBetween(startDate,endDate).flatMap(t => 
			bind("f", xhtml,"name_customer" -> Text(t.customerName),
							"name_user" ->Text(t.userName),
							"details" -> Text(t.descritionDetails),
							"total" -> Text("!!!"),
							"start" -> Text(t.start.is.toString),
							"end" -> Text(t.end.is.toString)
				)
			)
	}
	
	def paymentsFiltered:List[Payment] = {
		val payments = (cashier match {
				case (cs:List[Long]) if(!cs.isEmpty)=> {
					cs.map((c:Long) => {
						PaymentService.paymentsBetween(startDate,endDate,c.toInt,commands)
					}).reduceLeft(_:::_)
				}
				case _ => PaymentService.paymentsBetween(startDate,endDate,commands)
			}).filter((p) => {
			payment_type.isEmpty || p.details.exists((d)=> payment_type.contains(d.typePayment.is))
		})
		val paymentPerUser =  if(user != 0){
				payments.filter((p) => p.treatments.exists(_.user == user))
			}else{
				payments
			}
		if(showServices && showProducts){
			paymentPerUser
		}else{
			def filterType: (Payment) => Boolean =  if(showServices){
					(p:Payment) =>{ p.treatments.filter((t) => {t.hasService}).size > 0 }
				}else{
					(p:Payment) =>{ p.treatments.filter((t) => {t.hasProduct}).size > 0 }
				}
			def typeLog =  S.param("productclass") match {
				case Full(s) => s
				case _ => ""
			}		
			paymentPerUser.filter(filterType)
		}
	}

	def reportCommandConference(xhtml:NodeSeq):NodeSeq = {
		paymentsFiltered.flatMap(p => 
			bind("f", xhtml,
							"cashier" -> Text(p.cashier.obj match {
								case Full(c) => c.idForCompany.is.toString
								case _ => ""
							}),
							"date" -> Text(p.datePayment.is.getTime.toString),
							"command" -> Text(p.command.is.toString),
							"customerid" -> Text(p.customer.is.toString),
							"customername" -> Text(p.customerName),							
							"treatments" ->tableTreatments(p.treatments.toList),
							"payments" ->tablePayments(p.details.toList),
							"total" ->Text(p.value.is.toString)
				)
			)
	}

	//
	// Vai ser descontinuado em 28/02/2017
	//
	def simpleReportCommandConference(xhtml:NodeSeq):NodeSeq = {
		paymentsFiltered.flatMap(p => 
			bind("f", xhtml,"cashier" -> Text(p.cashier.obj.get.idForCompany.is.toString),
							"date" -> Text(p.datePayment.is.getTime.toString),	
							"customerid" -> Text(p.customer.is.toString),
//							"customername" -> Text(p.customer.obj.get.short_name.is.toString),
							"customername" -> <a href={"/customer/edit?id="+p.customer.is.toString+""} target='_customer_maste'>{""+p.customer.obj.get.short_name+""}</a>,
							"command" -> Text(p.command.is.toString),
							"user" ->Text(treatmentsProfAsText(p.treatments.toList)),
							"treatments" ->Text(treatmentsAsText(p.treatments.toList)),
							"payments" ->Text(p.details.map(_.typePaymentTranslated+", ").foldLeft("")(_ + _)),
							"total" ->Text(p.value.is.toString),
							"link" -> <a href={"/financial/commission_report_filter?payment="+p.id.is.toString+""}><img alt="Ver comissões deste atendimento" src="/images/commision_payment.png" width="24"/></a>,
							"commandprint" -> <a target="_command_maste" href={"/financial_cashier/print_command?command="+p.command.is.toString+ "&unit="+p.unitId.toString+"&date="+Project.dateToStr(p.datePayment.is)+""}><img alt="Imprimir comanda" src="/images/print.png" width="24"/></a>,
							"ticket" -> <a target="_command_maste" href={"/financial_cashier/expense_ticket?command="+p.command.is.toString+"&date="+Project.dateToStr(p.datePayment.is)+""}><img alt="Imprimir ticket" src="/images/print.png" width="24"/></a>,
							"receipt" -> <a target="_command_maste" href={"/financial_cashier/expense_receipt?command="+p.command.is.toString+"&date="+Project.dateToStr(p.datePayment.is)+""}><img alt="Imprimir recibo" src="/images/print.png" width="24"/></a>,
							"audit" -> <a href= '#' title={p.auditStr}><img src="/images/audit.png" width="24"/></a>
				)
			)
	}

	def tablePayments(payments:List[PaymentDetail]) = {

	<table class="bordered-table zebra-striped">
        <thead>
          <tr>
            <th>Forma de Pagamento</th>
            <th>Valor</th>
            <th>Data</th>
          </tr>
        </thead>
        <tbody>
	        {
	        	payments.map((p) => {
		          <tr>
		            <td>{p.typePaymentTranslated}</td>
		            <td>{p.value.is}</td>
		            <td>{Project.dateToStr(p.dueDate.is)}</td>
		          </tr>
	      		})
	        }
        </tbody>
      </table> 
  	}

  	def treatmentsAsText(treatments:List[Treatment]) = treatments.map((t) => t.details.toList).foldLeft(List[TreatmentDetail]())(_:::_).map((a)=>{a.nameActivity+", "}).foldLeft("")( _ + _)
  	def treatmentsProfAsText(treatments:List[Treatment]):String = if(!treatments.isEmpty){
		treatments.map(
			(t) => t.userName
		).reduceLeft(_+", "+_)
	 }else{
	 	""
	 }

	def tableTreatments(treatments:List[Treatment]):NodeSeq ={
      <table class="bordered-table zebra-striped" style="float:left;margin-right:2%">
        <thead>
          <tr>
         	 <th><span data-i18n='Profissional'></span></th>
            <th>Serviço/Produto</th>
            <th>Quantidade</th>
            <th>Preço Total</th>
          </tr>
        </thead>
        <tbody>
        {

         treatments.map((t) => t.details.toList).foldLeft(List[TreatmentDetail]())(_:::_).map((a)=>{
	          <tr>
	          	<td>{a.userName}</td>
	            <td>{a.nameActivity}</td>
	            <td>{a.amount}</td>
	            <td>{a.price.is}</td>
	          </tr>
	       })
          }
        </tbody>
      </table>   		
	}

  	def usersJsonFromChartReport(html:NodeSeq):NodeSeq = {
	  		Script(
	  			OnLoad(
			  		Call("buildChartTreatments",
						 JsArray(AuthUtil.company.usersForCalendar().map(u => JsObj(("name",u.name.is),("total",TreatmentService.countTreatmentsPayadBetweenPerUser(startDate,endDate,u)))))
			  		)
			  		)
		  		)
  	}	
}