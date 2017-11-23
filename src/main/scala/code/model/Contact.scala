package code
package model

import code.util._

import net.liftweb._
import mapper._
import http._
import SHtml._
//import util._
import _root_.java.math.MathContext;
import scalendar._
import Month._
import Day._
import net.liftweb.common.{ Box, Full, Empty }
import net.liftweb.proto._

import java.util.Date
import java.util.Calendar

class Contact extends LongKeyedMapper[Contact] 
  with PerCompany 
  with PerUnit
  with IdPK {

  def getSingleton = Contact

  object origin extends MappedPoliteString(this,255)

  object name extends MappedPoliteString(this,255)

  object birthday extends EbMappedDate(this){
    override def toLong: Long = is match {
      case null => 0l//(2206303200000l)*(-1)
      case d: java.util.Date => d.getTime
    }
  }

  object email extends MappedPoliteString(this,150) with LifecycleCallbacks {
    override def beforeSave() {
        super.beforeSave;
        if(this.get != ""){
          this.get.toLowerCase.trim.split(",|;").foreach((email1) => {
            if (!emailPattern.matcher(email1).matches /*&& !isNew*/) {
              throw new RuntimeException("E-mail inválido! " + email1)
            }
          })
          this.set(this.get.toLowerCase.trim)
        }
    } 
    def emailPattern = ProtoRules.emailRegexPattern.vend
  }
  object phone extends MappedPoliteString(this,20)  with LifecycleCallbacks {
    override def beforeSave() {
        super.beforeSave;
//          if(this.get == "(31) "){
        if(this.get.length < 8){
          this.set("")
        }
    } 
  }
  object mobilePhone extends MappedPoliteString(this,20) with LifecycleCallbacks {
    override def dbColumnName = "mobile_phone"
      override def beforeSave() {
        super.beforeSave;
//          if(this.get == "(31) "){
        if(this.get.length < 8){
          this.set("")
        }
      } 
  }

  object date1 extends EbMappedDate(this){
    override def toLong: Long = is match {
      case null => 0l//(2206303200000l)*(-1)
      case d: java.util.Date => d.getTime
    }
  }

  object value extends MappedCurrency(this) with LifecycleCallbacks {
    override def defaultValue = 0.0;
  }

  object obs extends MappedPoliteString(this,255)

  object business_pattern extends MappedLong(this)

  def makeAsCustomer = {
    if (business_pattern.is == 0 || business_pattern == null) {
      val ac = Customer.createInCompany
      .name (name)
      .email (email)
      .mobilePhone (phone)
      .birthday (birthday)
      .obs (obs + " " + origin)
      try {
        ac.save
      } catch {
        case e:Exception => {
          println ("vaiiiii ==== " + e.getMessage)
          throw e
        }
      }
    } else {
      val ac = Customer.findByKey(business_pattern).get
      if (ac.email == "" && email != "") {
        ac.email (email)
      }
      if (ac.phone == "" && phone != "") {
        ac.phone (phone)
      }
      try {
        ac.save
      } catch {
        case e:Exception => {
          println ("vaiiiii ==== " + e.getMessage)
          throw e
        }
      }
    }
    this.delete_!
  }

  lazy val firstName:String = {
    if (name.is.indexOf(" ") > 0) {
      name.is.substring (0,name.is.indexOf(" "))
    } else {
      name.is
    }
  }
  
  def replaceMessage (ac:Contact, message:String) = {
      var message_aux = message;
      //val extenso = WrittenForm (123.999.467.89)
      //println ("vaiiii ===================== " + extenso.humanize());
      //println ("vai =================== fora ")
      if (ac.name.is != "") {
          //println ("vai =================== dentro ")

          message_aux = message_aux.replaceAll("##hoje##", Project.dateToExt(new Date()));
          message_aux = message_aux.replaceAll("##mescorrente##", Project.monthToExt(new Date()));
          message_aux = message_aux.replaceAll("##messeguinte##", Project.monthToExt(Project.nextMonth (new Date())));
          message_aux = message_aux.replaceAll("##mesanterior##", Project.monthToExt(Project.prevMonth (new Date())));

          message_aux = message_aux.replaceAll("##logo##", "<img width='100px' src='" + AuthUtil.company.thumb_web + "'/>");

          message_aux = message_aux.replaceAll("##nome##", ac.name.is)
          message_aux = message_aux.replaceAll ("##prinome##", ac.firstName)
          message_aux = message_aux.replaceAll ("##telefone##", ac.phone)
          message_aux = message_aux.replaceAll ("##celular##", ac.mobilePhone)
          message_aux = message_aux.replaceAll ("##email##", ac.email)
          message_aux = message_aux.replaceAll ("##nasc_data##", Project.dateToStr(ac.birthday))
          message_aux = message_aux.replaceAll ("##nasc_idade##", Project.dateToAge(ac.birthday))
          message_aux = message_aux.replaceAll ("##nasc_anos##", Project.dateToYears(ac.birthday))
          message_aux = message_aux.replaceAll ("##nasc_ext##", Project.dateToExt(ac.birthday))

      }
      message_aux
  }

}

object Contact extends Contact with LongKeyedMapperPerCompany[Contact]


