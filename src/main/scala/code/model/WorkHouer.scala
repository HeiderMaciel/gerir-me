
package code
package model

import net.liftweb._
import mapper._
import http._
import SHtml._
import util._
import json._
import code.util._

import java.util.Date

class WorkHouer extends Audited[WorkHouer] with PerCompany with PerUnit with IdPK with CreatedUpdatedBy with CreatedUpdated with Restable[WorkHouer] {
  def getSingleton = WorkHouer
  object start extends MappedPoliteString(this, 5) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get.length < 5){
            this.set("0" + AuthUtil.company.calendarStart +":00")
          }
      } 
    }
  object end extends MappedString(this, 255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get.length < 5){
            this.set(AuthUtil.company.calendarEnd +":00")
          }
      } 
    }
  object startLanch extends MappedPoliteString(this, 20) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get.length < 5){
            this.set("00:00")
          }
      } 
    }
  object endLanch extends MappedPoliteString(this, 150) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          if(this.get.length < 5){
            this.set("00:00")
          }
      } 
    }
  object user extends MappedLongForeignKey(this, User)
  object day extends MappedString(this, 10) {
    def translate = {
      this.is match {
        case "Mon" => "Segunda"
        case "Tue" => "Terça"
        case "Wed" => "Quarta"
        case "Thu" => "Quinta"
        case "Fri" => "Sexta"
        case "Sat" => "Sábado"
        case "Sun" => "Domingo"
        case "WorkDays" => "Dias de Semana"
        case _ => "Todos"
      }
    }
    def translateInt = {
      this.is match {
        case "Mon" => 1
        case "Tue" => 2
        case "Wed" => 3
        case "Thu" => 4
        case "Fri" => 5
        case "Sat" => 6
        case "Sun" => 0
        case "WorkDays" => 7
        case _ => 8
      }
    }
  }
  object daynumber extends MappedInt(this) with LifecycleCallbacks {
        override def beforeSave() {
          super.beforeSave;
          if(day.is == "Sun"){
            this.set(0)
          }
          if(day.is == "Mon"){
            this.set(1)
          }
          if(day.is == "Tue"){
            this.set(2)
          }
          if(day.is == "Wed"){
            this.set(3)
          }
          if(day.is == "Thu"){
            this.set(4)
          }
          if(day.is == "Fri"){
            this.set(5)
          }
          if(day.is == "Sat"){
            this.set(6)
          }
        } 
    }
  object work_? extends MappedBoolean(this){
      override def defaultValue = true
      override def dbColumnName = "work"
  }    

    def cleaHourOfUser {
        BusyEvent.findAllInCompanyWithDeleteds(
            By_>=(BusyEvent.dateEvent,new Date),
            By(BusyEvent.user,this.user),
            By(BusyEvent.unit,AuthUtil.unit.id),
            // só deleta se for horario de trabalho
            // rigel 28/02/2018
            By(BusyEvent.is_employee_lanche_?,true) 
            ).foreach((be) =>{
            be.insecureDelete_!
        })
    }
    override def delete_! = {
      val r = super.delete_!
      cleaHourOfUser
      r
    }
    override def save = {
      val r = super.save
      cleaHourOfUser
      r
    }  
}

object WorkHouer extends WorkHouer with LongKeyedMapperPerCompany[WorkHouer] with OnlyCurrentCompany[WorkHouer] with MetaRestable[WorkHouer]
object WeekDay extends Enumeration {
  type WeekDay = Value
  val Mon, Tue, Wed, Thu, Fri, Sat, Sun, All, WorkDays = Value
}