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

import java.util.Date

class QuizSection extends Audited[QuizSection] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[QuizSection] with ActiveInactivable[QuizSection]{ 
    def getSingleton = QuizSection
    override def updateShortName = false
    // diferente de serviço categoria etc, 
    // seção, questão e item de dominio podem ser duplicados
    override def allowDuplicated_? = true;
    object quiz extends  MappedLongForeignKey(this, Quiz)
    object obs extends MappedPoliteString(this,255)
    object orderInQuiz extends MappedInt(this){
	    override def defaultValue = 1000
	}
    object weight extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object rank extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object rankPercent extends MappedDecimal(this,MathContext.DECIMAL64,4)

    def quizName = quiz.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    def questions = {
        QuizQuestion.findAll(By(QuizQuestion.quizSection, this.id.is), 
            OrderBy(QuizQuestion.orderInSection, Ascending), OrderBy(QuizQuestion.id, Ascending))
    }

    override def delete_! = {
        if(QuizQuestion.count(By(QuizQuestion.quizSection,this.id)) > 0){
            throw new RuntimeException("Existe questão para esta seção!")
        }

        super.delete_!
    }
    /*
    Select para testar o update
    select name, orderinquiz, 
    ((select count (*) from quizsection t1 where t1.company = quizsection.company 
        and t1.quiz = quizsection.quiz 
        and (t1.orderinquiz < quizsection.orderinquiz or (t1.orderinquiz = quizsection.orderinquiz and t1.id < quizsection.id)))+1) * 10
    from quizsection where company = 398 and quiz = 7;
    */
    val SQL_UPDATE_ORDER_10_10 = """
    update quizsection set orderinquiz = 
    ((select count (*) from quizsection t1 where t1.company = quizsection.company 
        and t1.quiz = quizsection.quiz 
        and (t1.orderinquiz < quizsection.orderinquiz or (t1.orderinquiz = quizsection.orderinquiz and t1.id < quizsection.id)))+1) * 10
    where company = ? and quiz = ?;
    """
    override def save() = {
        val r = super.save

        DB.runUpdate(SQL_UPDATE_ORDER_10_10, this.company.obj.get.id.is :: this.quiz.is :: Nil)

        r
    }

}

object QuizSection extends QuizSection with LongKeyedMapperPerCompany[QuizSection]  
    with  OnlyActive[QuizSection]
