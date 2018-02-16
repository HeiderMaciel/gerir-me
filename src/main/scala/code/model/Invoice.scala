package code
package model 

import code.actors._
import code.service._
import net.liftweb._ 
import mapper._ 
import net.liftweb.common._
import scalendar._
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext;
import net.liftweb.common.{Box,Full}
import code.util._
import _root_.java.util.Calendar
import _root_.java.util.Date


class Invoice extends Audited[Invoice] 
    with PerCompany with IdPK 
    with CompanyIdable[Invoice] 
	with CreatedUpdated with CreatedUpdatedBy with ActiveInactivable[Invoice] 
    with net.liftweb.common.Logger {
    def getSingleton = Invoice
    object business_pattern extends MappedLongForeignKey(this, Customer)
    object startAt extends EbMappedDate(this) 
    object endAt extends EbMappedDate(this)
    object obs extends MappedPoliteString(this,255)
    object unit extends MappedLongForeignKey(this,CompanyUnit) 
    object offsale extends MappedLongForeignKey(this, OffSale)
    object efectiveDate extends EbMappedDate(this) 
    object value extends MappedDecimal(this,MathContext.DECIMAL64,2)

    def bpName = business_pattern.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def createInvoice(offsale:Long, unit:Long, start:Date, end:Date, obs:String):Invoice = {
        val ac = OffSale.findByKey(offsale).get
        val invoice = Invoice.createInCompany
        .offsale(offsale)
        .business_pattern(ac.partner)
        .unit(unit)
        .startAt(start).endAt(end)
        .efectiveDate(new Date())
        .obs (obs)
        invoice.save
        invoice
    }

    def invoicing (start:Date, end:Date, offsale:Long, unit:Long, typeHosp:String)= {
        val unitaux = if (unit.toLong == 0) {
            AuthUtil.unit.id.is
        } else {
            unit
        }
        var obs = if (typeHosp == "0") {
            "Spsadt"
        } else {
            "Internação"
        }
        val invoice = createInvoice (offsale, unitaux, start, end, obs)
        InvoiceService.fat(invoice, start, end, typeHosp)

        if (InvoiceTreatment.countTreatments(invoice.id) <= 0) {
            invoice.value(0.0).save
            invoice.delete_!
        }
        invoice
    }
}

object Invoice extends Invoice with LongKeyedMapperPerCompany[Invoice] 
	with OnlyCurrentCompany[Invoice]  with OnlyActive[Invoice] {

    def toXmlTiss (invoice:Long) {
        val now  = new Date()
        //val nowTime  = now.getTime()
       var  iv = Invoice.findAllInCompany(By(Invoice.idForCompany,invoice.toInt))(0)
       var  os = OffSale.findByKey (iv.offsale).get
       var  bs = Customer.findByKey (os.partner).get
       var strXml =
     """<?xml version="1.0" encoding="ISO-8859-1" ?>
<ans:mensagemTISS xmlns="http://www.w3.org/2001/XMLSchema" xmlns:ans="http://www.ans.gov.br/padroes/tiss/schemas">
            <ans:cabecalho>
                <ans:identificacaoTransacao>
                    <ans:tipoTransacao>ENVIO_LOTE_GUIAS</ans:tipoTransacao>
                    <ans:sequencialTransacao>""" + invoice.toString + """</ans:sequencialTransacao>
                    <ans:dataRegistroTransacao>""" + Project.dateToDb(now) + """</ans:dataRegistroTransacao>
                    <ans:horaRegistroTransacao>""" + Project.dateToHourss(now) + """</ans:horaRegistroTransacao>
                </ans:identificacaoTransacao>
                <ans:origem>
                    <ans:identificacaoPrestador>
                        <ans:codigoPrestadorNaOperadora>""" + bs.document_offsale + """</ans:codigoPrestadorNaOperadora>
                    </ans:identificacaoPrestador>
                </ans:origem>
                <ans:destino>
                    <ans:registroANS>"""+ os.document_ans.is+"""</ans:registroANS>
                </ans:destino>
                <ans:versaoPadrao>3.02.00</ans:versaoPadrao>
            </ans:cabecalho>
            <ans:prestadorParaOperadora>
                <ans:loteGuias>
                    <ans:numeroLote>"""+ invoice.toString +"""</ans:numeroLote>
                    <ans:guiasTISS>
       """
       InvoiceTreatment.findAllInCompany(By(InvoiceTreatment.invoice,iv.id))
       .foreach ((it) => {
       println ("========= treatment " + it.treatment)
            var tr = Treatment.findByKey(it.treatment).get
            //info (tr.toXmlTiss(it.treatment).toString)
            strXml += tr.toXmlTiss(it.treatment)
       })
       strXml += """</ans:guiasTISS>
                </ans:loteGuias>
            </ans:prestadorParaOperadora>
            <ans:epilogo>
                <ans:hash>9f0619a9b4cf87c0206afc66fd9617f0</ans:hash>
            </ans:epilogo>
        </ans:mensagemTISS>
        """

        val filePath = if(Project.isLinuxServer){
          (Props.get("tissxml.path") openOr "/tmp/")
        }else{
          "c:\\vilarika\\"
        }
        
        println ("vaiiii ================ gerar " + filePath + "tiss_" + AuthUtil.company.id.toString + "_" 
        + os.xmlname.is + "_" + invoice.toString +".xml")

       scala.tools.nsc.io.File(filePath + "tiss_" + AuthUtil.company.id.toString + "_" 
        + os.xmlname.is + "_" + invoice.toString +".xml").writeAll(strXml)
    }
}

class InvoiceTreatment extends Audited[InvoiceTreatment] 
    with PerCompany with IdPK 
    with CreatedUpdated with CreatedUpdatedBy 
    with ActiveInactivable[InvoiceTreatment] 
    with net.liftweb.common.Logger {
    def getSingleton = InvoiceTreatment
    object invoice extends MappedLongForeignKey(this, Invoice)
    object treatment extends MappedLongForeignKey(this, Treatment)
    object obs extends MappedPoliteString(this,255)
    object value extends MappedDecimal(this,MathContext.DECIMAL64,2)

    def countTreatments (invoice:Long) = {
        InvoiceTreatment.count(By(InvoiceTreatment.invoice, invoice))
    }

    def countInvoiceTreatment (treatment:Long) = {
        InvoiceTreatment.count(By(InvoiceTreatment.treatment, treatment))
    }
     
    def createInvoiceTreatment(invoice:Long, treatment:Long):Long = {
        val itcount = InvoiceTreatment.countInvoiceTreatment (treatment)
        if (itcount < 1) {
            val invoiceTreatment = InvoiceTreatment.createInCompany.invoice(invoice)
            .treatment(treatment).obs("teste")
            invoiceTreatment.save
            invoiceTreatment.id.is
        } else {
            // este treatment já está em alguma fatura, nessa ou outra
            0
        }
    }

}

object InvoiceTreatment extends InvoiceTreatment with LongKeyedMapperPerCompany[InvoiceTreatment] 
    with OnlyCurrentCompany[InvoiceTreatment]  with OnlyActive[InvoiceTreatment] {
}

