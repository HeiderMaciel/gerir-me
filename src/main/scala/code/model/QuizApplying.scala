package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import json._
import net.liftweb.common.{Box,Full}
import _root_.java.math.MathContext; 
import _root_.java.util.Calendar
import _root_.java.util.Date

//import java.util.Date

class QuizApplying extends Audited[QuizApplying] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with ActiveInactivable[QuizApplying]{ 
    def getSingleton = QuizApplying
    object business_pattern extends MappedLongForeignKey(this, Customer)
    object applyDate extends EbMappedDate(this) {
        override def defaultValue = today;
    }
    object quiz extends  MappedLongForeignKey(this, Quiz)
    object obs extends MappedPoliteString(this,255)
    object rank extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object rankPercent extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object message extends MappedPoliteString(this,40000)

    def bpName = business_pattern.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }
    
    def quizName = quiz.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    val today = Project.date_format_db.parse(Project.date_format_db.format(new Date()));

    override def delete_! = {
        if((AuthUtil.company.appType.isEdoctus) || 
           (AuthUtil.company.appType.isEphysio)) {
            if (this.applyDate.before(today)) {
                throw new RuntimeException("Não é permitido excluir prontuário em data posterior à sua criação")
            }
        }
        // se vai deletar QuizApplying deleta antes 
        val ac = QuizAnswer.findAll (
            By (QuizAnswer.quizApplying, this.id))
        ac.foreach((qr)=>{
            qr.delete_!
        });

        val result = super.delete_!
        result
    }

    override def save() = {
        if((AuthUtil.company.appType.isEdoctus) || 
            (AuthUtil.company.appType.isEsmile) || 
            (AuthUtil.company.appType.isEbellepet) || 
           (AuthUtil.company.appType.isEphysio)) {
            if (isNew) {
                if (this.applyDate.before(today)) {
                    throw new RuntimeException("Não é permitido criar prontuário com data anterior à corrente")
                }
            } else {
                if (this.applyDate.before(today)) {
                    throw new RuntimeException("Não é permitido alterar prontuário em data posterior à sua criação")
                }
            }
        }

        if (this.message.length >= 40000) {
            throw new RuntimeException("Texto muito grande, verifique se o conteúdo nao foi truncado! " + this.message.length + " de um máximo de 40.000 caracteres")
        }

        // fazer um try para salvar o prontuário
/*        Treatment.findAll(By(Treatment.customer,this.business_pattern.is),
                          By(Treatment.hasDetail,true),
                          BySql("dateevent between date(?) and date(?)",IHaveValidatedThisSQL("dateevent","01-01-2012 00:00:00"), today, today)
                         ).
                map((a) => {
        })
*/
        super.save
    }

}


object QuizApplying extends QuizApplying with LongKeyedMapperPerCompany[QuizApplying]  
    with  OnlyActive[QuizApplying]
