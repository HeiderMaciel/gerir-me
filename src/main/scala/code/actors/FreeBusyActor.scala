package code
package actors


import java.text.ParseException
import java.util.Date
import java.util.Calendar

import net.liftweb._
import mapper._ 
import util._
import http._
import actor._
import model._
import code.model._
import code.util._
import java.util.Random

import code.comet._
object FreeBusyActor extends LiftActor with net.liftweb.common.Logger  {
  val DAY_IN_MILESECOUNDS = 86400000;
  def treat(freeBusy:FreeBusyRequest) {
        var wasChanged = false//Dont cray this was realy helpfull
        val now  = new Date()
        val nowTime  = now.getTime()
        val preStartDate = Project.strToDateOrToday(freeBusy.start)
        val endDate = Project.strToDateOrToday(freeBusy.end)
        val startDate = if(preStartDate.getTime < nowTime ) now else preStartDate

        if(endDate.getTime > nowTime){
            val c = Calendar.getInstance()
            c.setTime(startDate)
            lazy val weekDay = c.get(Calendar.DAY_OF_WEEK)-1
            lazy val users = User.findAll(
              //By(User.unit, freeBusy.unit),
              BySql ("id in (select user_c from usercompanyunit where unit = ? and status = 1) or unit = ?",
                IHaveValidatedThisSQL("",""), freeBusy.unit, freeBusy.unit),
              By(User.userStatus, User.STATUS_OK))
            lazy val users_intervals = users.filter(_.workHouers.
              filter( _.day.translateInt == weekDay ).filter( _.unit == freeBusy.unit).
              size > 0).map( u => {
              val workHouers = u.workHouers.filter( _.day.translateInt == weekDay).
              filter( _.unit == freeBusy.unit)
              //println ("vaiiii ======== " + u.name + " " + workHouers(0).start.is, workHouers(0).unit.is)
              (u.id.is, u.thumbAndName, workHouers(0).startLanch.is, workHouers(0).endLanch.is, u.userName.is, workHouers(0).start.is, workHouers(0).end.is, workHouers(0).unit.is)
            })
            lazy val company = Company.findByKey(freeBusy.company).get
            lazy val realEndCompany = company.calendarEnd
            lazy val realStartCompany = company.calendarStart
            def interval:Long = 0//(endDate.getTime - startDate.getTime) / DAY_IN_MILESECOUNDS
            def intToHour(hour:Int) = {
              val hourStr = if(hour < 10){
                "0"+hour.toString()
              }else{
                hour.toString()
              }
              hourStr+":00";
            }
            def startHour = {
              intToHour(realStartCompany)
            }
            def endHour = {
              intToHour(realEndCompany)
            }
            def cdIm(n:Long) = DAY_IN_MILESECOUNDS * n//days in milesecounds
            def assertedTime(date:String, hour:String, n:Long) = Project.strToDate(date+" "+hour).getTime + cdIm(n)
            (0l to interval).map(n => users_intervals.map(u => {
                  try{
                        def saveLanch = {
                          if(u._3 != u._4){
                            BusyEvent
                            .create
                            .is_employee_lanche_?(true)
                            .company(company)
                            .start(new Date(assertedTime(freeBusy.start,u._3,n)) )
                            .end( new Date(assertedTime(freeBusy.start,u._4,n)))
                            .user(u._1)
                            .unit(u._8)
                            .description("Horário de Almoço [%s]".format(u._5))
                            .save
                            true
                          }else{
                            false  
                          }
                          
                        }
                        def saveStartDelay = {
                          if(startHour != u._6){
                              BusyEvent
                              .create
                              .is_employee_lanche_?(true)
                              .company(company)
                              .start(new Date(assertedTime(freeBusy.start,"0"+realStartCompany+":00",n)) )
                              .end( new Date(assertedTime(freeBusy.start,u._6,n)))
                              .user(u._1)
                              .unit(u._8)
                              .description("Não trabalha início".format(u._5))
                              .save
                              true
                            }else{
                              false
                            }
                        }
                        def saveEndDelay = {
                          if(endHour != u._7){
                            BusyEvent
                            .create
                            .is_employee_lanche_?(true)
                            .company(company)
                            .start(new Date(assertedTime(freeBusy.start,u._7,n)) )
                            .end( new Date(assertedTime(freeBusy.start,"0"+realEndCompany+":00",n)) )
                            .user(u._1)
                            .unit(u._8)
                            .description("Não trabalha fim".format(u._5))
                            .save;
                            true
                          }else{
                            false
                          }
                        }

                        if(freeBusy.start != null && u._3 != null && u._3 != ""){

                          val date = assertedTime(freeBusy.start,u._3,n);
                          if(!BusyEvent.hasLanchEventoToUser(u._1, u._8, new Date(date))){
                            wasChanged = saveLanch | saveStartDelay | saveEndDelay
                            //info("Something change Here saveStartDelay")
                          }                          
                        }
                    }catch{
                      // acho que nao precisa desse log pq acho que nao é erro
                      case e:Exception => {
                        e.printStackTrace
                        wasChanged = false
                        LogActor ! "Erro ao processar FreeBusyRequest do usuario : " + u._1 +" - "+e.getMessage
                      }
                      case _ =>
                    }
                  }
                )
            )
            //folga
            users.filter( _.workHouers.size > 0 ).map((u) => {
              if(!BusyEvent.hasLanchEventoToUser(u.id.is, freeBusy.unit, startDate)){
                val hasClearance = u.workHouers.filter((wh) => {wh.day.translateInt == weekDay ||  wh.day.translateInt == 8}).size == 0
                if(hasClearance){
                  wasChanged = true
                  //info("Something change Here folga")
                  var iu = 0;
                  def units = CompanyUnit.findAllOfUser(
                    company.id.is,u.id.is)
                    units.foreach((cu)=>{
                      val ct = BusyEvent.countBE (
                      company,
                      new Date(assertedTime(freeBusy.start,"0"+realStartCompany+":00",0)), 
                      new Date(assertedTime(freeBusy.start,"0"+realEndCompany+":00",0)),
                      u.id.is,
                      cu.id.is) // vaiii teria que criar para as outras unidades do cara
                      if (ct < 1) {
                        BusyEvent
                        .create
                        .is_employee_lanche_?(true)
                        .company(company)
                        .start(new Date(assertedTime(freeBusy.start,"0"+realStartCompany+":00",0)) )
                        .end( new Date(assertedTime(freeBusy.start,"0"+realEndCompany+":00",0)))
                        .user(u.id.is)
                        .unit(cu.id.is) // vaiii teria que criar para as outras unidades do cara
                        .description("Não trabalha folga".format(u.name.is))
                        .save;
                      }
                      iu += 1;
                    })
                    if (iu == 0) {
                    // garantia caso tire todas as unidades da agenda ficaria em loop
                    BusyEvent
                      .create
                      .is_employee_lanche_?(true)
                      .company(company)
                      .start(new Date(assertedTime(freeBusy.start,"0"+realStartCompany+":00",0)) )
                      .end( new Date(assertedTime(freeBusy.start,"0"+realEndCompany+":00",0)))
                      .user(u.id.is)
                      .unit(u.unit.is) // vaiii teria que criar para as outras unidades do cara
                      .description("Não trabalha folga".format(u.name.is))
                      .save;
                    }
                }
              }
            })
            if(wasChanged){
                TratmentServer ! TreatmentMessage("updateTreatment", startDate, freeBusy.company, freeBusy.unit)
                LogActor ! "TratmentServer ! TreatmentMessage('updateTreatment', new Date(date))"+startDate              
            }

        }
  }

  protected def messageHandler = {
    case a:FreeBusyRequest => treat(a)
    case _ => 
  }
}


case class FreeBusyRequest(start:String,end:String, unit:Long, company:Long)
