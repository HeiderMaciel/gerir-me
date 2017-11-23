package code
package util


import net.liftweb._
import mapper._

import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import S._
import code.model._
import scala.xml._
import scala.collection.JavaConverters._

import java.io.{File,FileInputStream}
import java.util.Calendar;
import java.util.TimeZone;
import scala.io.Source._


object CnabUtil extends net.liftweb.common.Logger {
  val START_DETAILS = 2
  // mateus tinha colocado 44 a 57, mas acho que pegava um zero do convênio
  val MONTHLY_ID = (45, 57)
  val PAYMENT_DATE = (137, 145)
  val EFETIVE_DATE = (145, 153)
  val PAID_VALUE = (77, 92)
  val INCREASE_VALUE = (18, 32)
  val LIQUID_VALUE = (92, 107)



  def execute(file:File):String = {
    var strAux = "";
    var str1 = "";
    val lines = fromFile(file).getLines.toList
    val details = for(i <- 1 to lines.size-3 if(i % 2 == 0 )) yield {
      factory(i, lines)
    }
    details.map((d) => {
      //val mo = Monthly.findByKey(d.monthlyId)
      val mo = Monthly.findAllInCompany(
        By(Monthly.idForCompany,d.monthlyId))
      //val company = Company.findByKey(mo.company)
      val co = Customer.findByKey (mo(0).business_pattern).get
      if (!mo.isEmpty) {
        val obsAux : String = if (mo(0).obs.indexOf (" CnabUtil") != -1) {
              mo(0).obs;
           } else {
              mo(0).obs + " CnabUtil";
           }
        if (!mo(0).paid) {
                mo(0).paymentDate(d.paymentDate)
                .efetiveDate(d.efetiveDate)
                .paidValue(d.paidValue)
                .increseValue(d.increseValue)
                .liquidValue(d.liquid)
                .obs(obsAux)
                .paid(true) 
                .save
          str1 = "Boleto " + d.monthlyId + " pagamento confirmado " + co.name + " " + d.paidValue;
          LogObj.wLogObj(AuthUtil.company.id, str1, "importação Cnab 240")
        } else {
          str1 = "Boleto " + d.monthlyId + " " + co.name + " JÁ ESTAVA MARCADO como pago " + d.paidValue;
          println ("vaii =========== boleto " + str1)
          LogObj.wLogObj(AuthUtil.company.id, str1, "importação Cnab 240")
        }
      } else {
          str1 = "vaii =========== boleto NAO ECONTRADO " + d.monthlyId
          println (str1)
      }
      strAux = strAux + str1 + "\n\r"
    })
    strAux
  }
  def factory(i:Int, lines:List[String]):Detail ={
    val dataLine = i+1
    //println ("vaiiiii ===========" + lines(i).substring(MONTHLY_ID._1, MONTHLY_ID._2) +"=====")
    //println ("vaiiiii payment ===========" + lines(dataLine).substring(PAYMENT_DATE._1, PAYMENT_DATE._2) +"=====")
    //println ("vaiiiii efetive ===========" + lines(dataLine).substring(EFETIVE_DATE._1, EFETIVE_DATE._2) +"=====")
    Detail(
            lines(i).substring(MONTHLY_ID._1, MONTHLY_ID._2),
            lines(dataLine).substring(PAYMENT_DATE._1, PAYMENT_DATE._2),
            lines(dataLine).substring(EFETIVE_DATE._1, EFETIVE_DATE._2),
            lines(dataLine).substring(PAID_VALUE._1, PAID_VALUE._2),
            lines(dataLine).substring(INCREASE_VALUE._1, INCREASE_VALUE._2),
            lines(dataLine).substring(LIQUID_VALUE._1, LIQUID_VALUE._2)
          )
  }
}


case class Detail(
                  private val _monthlyId:String,
                  private val _paymentDate:String, 
                  private val _efetiveDate:String, 
                  private val _paidValue:String, 
                  private val _increseValue:String, 
                  private val _liquid:String){
    def monthlyId = _monthlyId.trim.toInt //toLong

    def paymentDate = cnabDataToDate(_paymentDate)
    def efetiveDate = cnabDataToDate(_efetiveDate)
    def paidValue =  valueCnabToValue(_paidValue)
    def increseValue =  valueCnabToValue(_increseValue)
    def liquid =  valueCnabToValue(_liquid)

    def cnabDataToDate(dateStr:String) = {
      val date = Calendar.getInstance
      date.set(Calendar.DATE, dateStr.substring(0, 2).toInt)
      date.set(Calendar.MONTH, dateStr.substring(2, 4).toInt - 1) // o mês é menos 1 mesmo de 0 a 11
      date.set(Calendar.YEAR, dateStr.substring(4, 8).toInt)
      date.set(Calendar.HOUR, 3) // por causa de horário de verão antes mateus passava 0 e caia no dia anterior
      val tz = TimeZone.getTimeZone("GMT");
      date.setTimeZone(tz);      
      date.getTime
    }

    def valueCnabToValue(value:String) = {
      value.trim.toFloat / 100
    }
}

