package code
package service

import net.liftweb._
import mapper._ 
import code.util._
import code.actors._
import code.comet._
import model._
import net.liftweb.util._
import http.js._
import JE._
import net.liftweb.common._
//import InventoryMovement._
import java.util.Date
import java.util.Calendar

import scalendar._
object InvoiceService extends net.liftweb.common.Logger {
	
	def fat(invoice:Invoice, start:Date, end:Date, typeHosp:String)={
		Treatment.treatmentstoInvoice(start,end).foreach (treatment => {
			var it:Long = 0
			var totval:BigDecimal = 0.0;
			val ac = treatment.getTreatEdoctus
			if ((ac.hospitalizationType == "" && typeHosp == "0") ||
				(ac.hospitalizationType != "" && typeHosp == "1")) {
				treatment.details.foreach (td => {
					if (td.offsale.is == invoice.offsale.is) {
						if (it == 0) {
							it = InvoiceTreatment.createInvoiceTreatment (invoice.id,td.treatment)
						}
						totval += td.price.is;
						invoice.value(invoice.value.is+td.price.is);
						invoice.save
					}
				})
				if (it != 0) {
					var itt = InvoiceTreatment.findByKey(it).get 
					itt.value (totval).save
				}
			}
		})
	}

	def desFat(cashier:Cashier)={
		AccountPayable.findAllInCompany(
				By(AccountPayable.cashier, cashier.id.is),
				By(AccountPayable.autoCreated_?,true)
			).foreach(_.delete_!)
	}

	def totalValues (treatment:Treatment)={
		var strXml:String ="";
		InvoiceGroup.findAll().foreach ((ig)=> {
			//println ("========= " + ig.name)
			strXml += "<" + ig.xmlTissTag + ">" + treatment.totalValue(ig.id) + "</" + ig.xmlTissTag + ">"
		})
		strXml
	}
	def totalValuesVal (treatment:Treatment)={
		var totAux:BigDecimal =0;
		InvoiceGroup.findAll().foreach ((ig)=> {
			//println ("========= " + ig.name)
			totAux += treatment.totalValue(ig.id)
		})
		totAux
	}
}

