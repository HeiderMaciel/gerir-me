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

class Quiz extends Audited[Quiz] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[Quiz] with ActiveInactivable[Quiz]{ 
    def getSingleton = Quiz
    override def updateShortName = false
    object userGroup extends  MappedLongForeignKey(this, UserGroup)
    object obs extends MappedPoliteString(this,255)
    object message extends MappedPoliteString(this,40000)
    object rank extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object share_? extends MappedBoolean(this) {
        override def defaultValue = true
        override def dbColumnName = "share"
    }
    object showInRecords_? extends MappedBoolean(this) {
        override def defaultValue = false
        override def dbColumnName = "showinrecords"
    }

    def userGroupName = userGroup.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    def sections (quizapplying:Long, print:Boolean)= {
        def sql = if (print) {
            """ id in (select qq.quizsection from quizanswer 
            inner join quizquestion qq on
            qq.id = quizquestion
            where quizapplying = ?) """
        } else {
            " ( 1 = 1 or ? > -2 )"
        }
        QuizSection.findAll(
            By(QuizSection.quiz, this.id.is), 
            BySql (sql, IHaveValidatedThisSQL("",""), quizapplying),
            OrderBy(QuizSection.orderInQuiz, Ascending), 
            OrderBy(QuizSection.id, Ascending))
    }

    def quizLabel = if(AuthUtil.? && (AuthUtil.company.appType.isEsmile||AuthUtil.company.appType.isEdoctus||
       AuthUtil.company.appType.isEphysio)){
        "Prontuário"
    } else {
        "Questionário"
    }

    override def delete_! = {
        if(QuizApplying.count(By(QuizApplying.quiz,this.id)) > 0){
            throw new RuntimeException("Existe aplicação desta avaliação!")
        }
        if(QuizSection.count(By(QuizSection.quiz,this.id)) > 0){
            throw new RuntimeException("Existe seção nesta avaliação!")
        }

        super.delete_!
    }

    override def save() = {
        println ("vaiiiiiii ==================== ANTES " + this.message.length)
/* criar método no project */
        this.message.set (message.replaceAll ("align=\"left\"", ""))
        this.message.set (message.replaceAll ("font-stretch: normal;", ""))
        this.message.set (message.replaceAll ("mso-bidi-font-weight:normal", ""))
        this.message.set (message.replaceAll ("font-size-adjust: none;", ""))

        this.message.set (message.replaceAll ("border-color: rgb(0, 0, 0);", ""))
        this.message.set (message.replaceAll ("border-image: none;", ""))
        this.message.set (message.replaceAll ("border-right-width: initial;", ""))
        this.message.set (message.replaceAll ("border-right-style: none;", ""))
        this.message.set (message.replaceAll ("border-top: none;", ""))
        this.message.set (message.replaceAll ("solid rgb(0, 0, 0)", "solid"))
        this.message.set (message.replaceAll ("solid #000000", "solid"))

        println ("vaiiiiiii ==================== DEPOIS " + this.message.length)

        if (this.message.length >= 40000) {
            throw new RuntimeException("Texto muito grande, verifique se o conteúdo nao foi truncado! " + this.message.length + " de um máximo de 40.000 caracteres")
        }

        super.save  
        if(QuizSection.count(By(QuizSection.quiz,this.id)) < 1){
            val ac = QuizSection.createInCompany.
            quiz(this.id).
            name(this.name)
            ac.save
        }
        super.save
    }

}

object Quiz extends Quiz with LongKeyedMapperPerCompany[Quiz]  
    with  OnlyActive[Quiz] {
    def findAllRecords = findAllInCompany()
}
