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
  //val START_DETAILS = 2
  // mateus tinha colocado 44 a 57, mas acho que pegava um zero do convênio

  def execute(file:File, account:String):String = {
    var total = 0.0;
    var countGood = 0;
    var countBad = 0;
    var strAux = "";
    var strGood = "";
    var strBad = "";
    val lines = fromFile(file).getLines.toList
    val details = for(i <- 1 to lines.size-3 if(i % 2 == 0 )) yield {
      factory(i, lines, account)
    }
    details.map((d) => {
/*
println ("vaiiiii ===============" +  "\n\n" +
      " dt pgto -" + d.paymentDate +  "-\n\n" +
      " dt credito -" + d.efetiveDate + "-\n\n" +
      " val pago -" + d.paidValue +  "-\n\n" +
      " val juros -" + d.increseValue +  "-\n\n" +
      " val liqquido -" + d.liquid + "-\n\n");
*/

      //val mo = Monthly.findByKey(d.monthlyId)
      val mo = Monthly.findAllInCompany(
        By(Monthly.idForCompany,d.monthlyId))
      if (Account.bankNum(account.toLong) != d.bankId) {
        countBad += 1;
        strBad += " Banco selecionado " + Account.bankNum(account.toLong) + " banco encontrado " + d.bankId + " "; 
      } else if (mo.length < 1) {
        countBad += 1;
        strBad += " Boleto ***** " + d.monthlyId + " Não encontrado"; 
      } else {
        //val company = Company.findByKey(mo.company)
        val co = Customer.findByKey (mo(0).business_pattern).get
        if (!mo.isEmpty) {
          val obsAux : String = if (mo(0).obs.indexOf (" CnabUtil") != -1) {
                mo(0).obs;
             } else {
                mo(0).obs + " CnabUtil";
             }
          if (!mo(0).paid) {
            if (d.liquid > 0.0) {
              mo(0).paymentDate(d.paymentDate)
              .efetiveDate(d.efetiveDate)
              .paidValue(d.paidValue)
              .increseValue(d.increseValue)
              .liquidValue(d.liquid)
              .obs(obsAux)
              .paid(true) 
              .save
              strGood += " Boleto " + d.monthlyId + " " + co.name + " " + d.paidValue;
              total += d.paidValue;
              countGood += 1;
              LogObj.wLogObj(AuthUtil.company.id, "Boleto " + d.monthlyId + " " + co.name + " " + d.paidValue, "importação Cnab 240")
            } else {
              countBad += 1;
              strBad += " Boleto ***** " + d.monthlyId + " " + co.name + " Valor zero 000000 "; 
            }
          } else {
            countBad += 1;
            strBad += " Boleto " + d.monthlyId + " " + co.name + " JÁ ESTAVA MARCADO como pago " + d.paidValue;
            LogObj.wLogObj(AuthUtil.company.id, "Boleto " + d.monthlyId + " " + co.name + " JÁ ESTAVA MARCADO como pago " + d.paidValue, "importação Cnab 240")
            if ((mo(0).efetiveDate.get == Empty || mo(0).efetiveDate.get == null) &&
                (d.efetiveDate != Empty && d.efetiveDate != null)) {
              mo(0).efetiveDate(d.efetiveDate)
              .save
              strBad += " Boleto " + d.monthlyId + " " + co.name + " DATA EFETIVA atualizada " + d.paidValue;
            }
          }
        } else {
            strBad += "vaii =========== boleto NAO ECONTRADO " + d.monthlyId
        }
      }
//      strAux = strAux + str1 + "\n\r"
    })
    var ret = "" 
    if (countBad > 0) {
      ret += strBad + " ................... Rejeitados " + countBad + " ..............   "
    }
    if (countGood > 0) {
       ret += strGood + " ................. Qtde Boletos OK " + countGood + " Total " + total
    }  
    println ("vaiii =========================== " + ret)
    ret    
  }
  def factory(i:Int, lines:List[String], account:String):Detail ={
    val BANK_ID = (0, 3)
    val MONTHLY_ID = if (Account.bankNum(account.toLong) == "001") {
      (45, 57)
    } else {
      (59, 73)
    }
    val PAYMENT_DATE = (137, 145)
    val EFETIVE_DATE = (145, 153)
    val PAID_VALUE = (77, 92)
    val INCREASE_VALUE = (18, 32)
    val LIQUID_VALUE = (92, 107)
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
            lines(dataLine).substring(LIQUID_VALUE._1, LIQUID_VALUE._2),
            lines(dataLine).substring(BANK_ID._1, BANK_ID._2)
          )
  }
}


case class Detail(
                  private val _monthlyId:String,
                  private val _paymentDate:String, 
                  private val _efetiveDate:String, 
                  private val _paidValue:String, 
                  private val _increseValue:String, 
                  private val _liquid:String,
                  private val _bankId:String
                  ){
    def monthlyId = if (_monthlyId.trim != "") {
      _monthlyId.trim.toInt //toLong
    } else {
      0;
    }

    def paymentDate = cnabDataToDate(_paymentDate)
    def efetiveDate = cnabDataToDate(_efetiveDate)
    def paidValue =  valueCnabToValue(_paidValue)
    def increseValue =  valueCnabToValue(_increseValue)
    def liquid =  valueCnabToValue(_liquid)
    def bankId = _bankId

    def cnabDataToDate(dateStr:String) = {
      if (dateStr != "00000000") {
        val date = Calendar.getInstance
        date.set(Calendar.DATE, dateStr.substring(0, 2).toInt)
        date.set(Calendar.MONTH, dateStr.substring(2, 4).toInt - 1) // o mês é menos 1 mesmo de 0 a 11
        date.set(Calendar.YEAR, dateStr.substring(4, 8).toInt)
        date.set(Calendar.HOUR, 3) // por causa de horário de verão antes mateus passava 0 e caia no dia anterior
        val tz = TimeZone.getTimeZone("GMT");
        date.setTimeZone(tz);      
        date.getTime
      } else {
        // data efetive no siccob parece que só seta qdo faz o credito mesmo
        // o belto aparece como pago mas a data do crédito ainda vem zerada
        null
      }
    }

    def valueCnabToValue(value:String) = {
      value.trim.toFloat / 100
    }
}

