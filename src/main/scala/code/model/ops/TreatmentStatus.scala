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

/*
trait StatusConstants{
  val STATUS_OK = 1
  val STATUS_BLOCKED = 2
  val STATUS_DELETED = 3
  val STATUS_INACTIVE = 4
}
object StatusConstants extends StatusConstants{

}
*/
trait TreatmentStatus {
    val Open = 0
    val Missed = 1
    val Arrived = 2
    val Ready = 3
    val Paid = 4
    val Deleted = 5
    val Confirmed = 6
    val PreOpen = 7
    val ReSchedule = 8
    val Budget = 9
    val ReScheduled = 10
    val OnGoing = 11
    val Canceled = 12 // by company
    val ReOpen = 13 // by company

//        val Open, Missed, Arrived,Ready,Paid, Deleted, Confirmed, PreOpen, ReSchedule, Budget = Value
    // no pilates é imperativo separar a falta (missed) da remarcação (reschedule)
}
object TreatmentStatus extends TreatmentStatus {

}
