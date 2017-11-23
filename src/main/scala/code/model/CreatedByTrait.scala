package code
package model 

import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsExp
import net.liftweb._ 
import mapper._ 
import util._ 
import net.liftweb.util.{FieldError}
import code.util

import http._ 
import SHtml._ 
import util._
import util._ 

import net.liftweb.common.{Box,Full,Empty,Failure,ParamFailure}

trait CreatedByTrait {
  self: BaseMapper =>

  import net.liftweb.util._

  protected def createdByIndexed_? = false

  lazy val createdBy = new MyCreatedBy(this)

  protected class MyCreatedBy(obj: self.type) extends MappedLongForeignKey(obj.asInstanceOf[MapperType],User) {
    override def defaultValue = AuthUtil.userId
    override def dbIndexed_? = createdByIndexed_?
  }

  lazy val createdByName = createdBy.obj match {
        case Full(t:User) => t.short_name.is
        case _ => ""
  }
  lazy val createdById = createdBy.obj match {
        case Full(t:User) => t.id.is
        case _ => -1
  }
}

trait UpdatedByTrait {
  self: BaseMapper =>

  import net.liftweb.util._

  protected def updatedByIndexed_? = false

  lazy val updatedBy: MyUpdatedBy = new MyUpdatedBy(this)

  protected class MyUpdatedBy(obj: self.type) extends MappedLongForeignKey(obj.asInstanceOf[MapperType],User) with LifecycleCallbacks {
    override def beforeSave() {
      super.beforeSave; 
      if(AuthUtil.userId != 0){
        this.set(AuthUtil.userId)
      }
    }
    override def defaultValue = AuthUtil.userId
    override def dbIndexed_? = updatedByIndexed_?
  }

  lazy val updatedByName = updatedBy.obj match {
        case Full(t:User) => t.short_name.is
        case _ => ""
  }
  lazy val updatedById = updatedBy.obj match {
        case Full(t:User) => t.id.is
        case _ => -1
  }

}

trait CreatedUpdatedBy extends CreatedByTrait with UpdatedByTrait 
  with CreatedUpdated {
  self: BaseMapper =>

  protected def createdAtStr = 
    if (Project.dateToYear(createdAt) != Project.dateToYear(Project.today)) {
      Project.dateToStr (createdAt)
    } else {
      Project.dateToMonthAndDay(createdAt)
    }

  protected def updatedAtStr = 
    if (Project.dateToYear(updatedAt) != Project.dateToYear(Project.today)) {
      Project.dateToStr (updatedAt)
    } else {
      Project.dateToMonthAndDay(updatedAt)
    }

  lazy val auditStr:String = if (createdById != updatedById) {
    "Criado por " + createdByName + " " + 
      createdAtStr + " " +
      Project.dateToHours(createdAt) + "\n" +
    "Alterado por " + updatedByName + " " + 
      updatedAtStr + " " +
      Project.dateToHours(updatedAt)
  } else if (Project.dateToStr(createdAt) !=
    Project.dateToStr(updatedAt)) {
    "Criado por " + createdByName + " " + 
      createdAtStr + " " +
      Project.dateToHours(createdAt) + "\n" +
    "Alterado em " + 
      updatedAtStr + " " +
      Project.dateToHours(updatedAt)
  } else if (Project.dateToHours(createdAt) !=
    Project.dateToHours(updatedAt)) {
    "Criado por " + createdByName + " " + 
      createdAtStr + " " +
      Project.dateToHours(createdAt) + "\n" +
    "Alterado em " + 
      Project.dateToHours(updatedAt)
  } else {
    "Criado por " + createdByName + " " + 
      createdAtStr + " " +
      Project.dateToHours(createdAt)
  }
}

