package code
package model 

import net.liftweb.common.{Box,Full,Empty}
import net.liftweb._ 
import mapper._
import http._ 
import SHtml._ 
import util._ 
import code.util._
import _root_.java.math.MathContext; 
import java.util.Date
import http.js._

class TreatEdoctus extends Audited[TreatEdoctus] with IdPK with CreatedUpdated with CreatedUpdatedBy with PerCompany with net.liftweb.common.Logger{
    def getSingleton = TreatEdoctus
    object treatment extends MappedLongForeignKey(this,Treatment)
    object icd extends MappedLongForeignKey(this,Icd)
    object attendanceType extends MappedString(this, 20) {//caraterAtendimento
        override def defaultValue = "1" // eletiva 2 - urgencia;
    }    
    object newBornAttendance extends MappedString(this, 20) {//Recem nascido
        override def defaultValue = "N"
    }    
    object hospitalizationType extends MappedString(this, 20) {//tipoInternacao
        override def defaultValue = "1"
    }
    object hospitalizationRegime extends MappedString(this, 20) {//regimeInternacao
        override def defaultValue = "1"
    }
    object accidentIndicator extends MappedString(this, 20) {//indicadorAcidente
        override def defaultValue = "9" // não acidente
    }    
    object closingCause extends MappedString(this, 20) // motivoEncerramento
    object InvoiceType extends MappedString(this, 20) {// tipoFaturamento
        override def defaultValue = "4"
    }
    object offsale extends MappedLongForeignKey(this,OffSale)
    object obs extends MappedString(this, 4000)
    object obsLate extends MappedString(this, 255)
    object arrivedAt extends EbMappedDateTime(this)
    object startAtt extends EbMappedDateTime(this) // attendance
    object endAtt extends EbMappedDateTime(this)

    def hasOffSale = offsale.obj  match {
        case Full(a) => true
        case _ => false
    }

    override def delete_! = {
        treatment.obj match {
            case Full(t)  if(t.isPaid)=> { throw new RuntimeException(" Não é permitido excluir atendimento pago!") }
            case _ => 
        } 
        super.delete_!
    }

    def toXmlTiss = {
       var strXml:String ="""
       """
        strXml
    }

}

object TreatEdoctus extends TreatEdoctus with LongKeyedMapperPerCompany[TreatEdoctus] with OnlyCurrentCompany[TreatEdoctus]{
    
}
