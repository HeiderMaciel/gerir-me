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

class QuizQuestion extends Audited[QuizQuestion] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[QuizQuestion] with ActiveInactivable[QuizQuestion]{ 
    def getSingleton = QuizQuestion
    override def updateShortName = false
    // diferente de serviço categoria etc, 
    // seção, questão e item de dominio podem ser duplicados
    override def allowDuplicated_? = true;
    object quizSection extends  MappedLongForeignKey(this, QuizSection)
    object obs extends MappedPoliteString(this,255)
    object orderInSection extends MappedInt(this){
	    override def defaultValue = 1000
	}
    object history_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "history"
    }    
    object saveIfNoAnswer_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "saveifnoanswer"
    }    
    object printIfNoAnswer_? extends MappedBoolean(this){
        override def defaultValue = false
        override def dbColumnName = "printifnoanswer"
    }    
    object autoComplete_? extends MappedBoolean(this){
        override def defaultValue = true
        override def dbColumnName = "autoComplete"
    }    
    object rank extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object quizDomain extends MappedLongForeignKey(this, QuizDomain) // opcional
    object quizDomainItemRight extends MappedLongForeignKey(this, QuizDomainItem) // opcional
    object quizQuestionFormat extends MappedInt(this)with LifecycleCallbacks { // em função do número de dias
        override def defaultValue = 0 
        // 0 questões curtas - label
        // 1 questoes longas - paragrafo topo
        // 2 questoes curtas - paragrafo topo
        // 3 questoes longas - paragrafo - label
    }
    object quizQuestionType extends MappedInt(this)with LifecycleCallbacks { 
        /*
        0 Texto
        1 Texto longo
        2 Escolha de uma lista select (domínio)
        3 Múltipla escolha (domínio)
        4 Escolha radio (+ de uma opção)
        5 valor
        6 data
        7 Escolha select (+ de uma opção)
        */
    }
    object quizQuestionSize extends MappedInt(this)with LifecycleCallbacks { 
        override def defaultValue = 3 
        // mini 0
        // short 1
        // medium 2
        // large 3
        // xlarge 4
    }
    object quizQuestionPosition extends MappedInt(this)with LifecycleCallbacks { 
        override def defaultValue = 0 
        // só 0
        // inicio 1
        // meio 2
        // fim 3
    }

    object addon extends MappedPoliteString(this,255)
    object message extends MappedPoliteString(this,40000)
    object sufix extends MappedPoliteString(this,255)
    object printControl extends MappedInt(this)with LifecycleCallbacks { 
        override def defaultValue = 1
        // nunca 0 prontuári confidencial do médico
        // se preenchido 1
        // sempre 2
        // sem borda 3
    }
    // é um auto relacionamento não transformar em FK
    object defaultQuestion extends MappedLong(this) {
        override def defaultValue = 0
    }
    object quizQuestionStyle extends MappedPoliteString(this,255) {
        override def defaultValue = "";
    }


    def quizSectionName = quizSection.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    def quizDomainName = quizDomain.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    def quizQuestionPositionName:String = {
        val aclist = DomainTable.findAll (
            By(DomainTable.cod, this.quizQuestionPosition.toString),
            By(DomainTable.domain_name,"quizQuestionPosition"))
        if (aclist.length > 0) {
            aclist(0).short_name
        } else {
            ""
        }
    }

    def quizQuestionFormatName:String = {
        val aclist = DomainTable.findAll (
            By(DomainTable.cod, this.quizQuestionFormat.toString),
            By(DomainTable.domain_name,"quizQuestionFormat"))
        if (aclist.length > 0) {
            aclist(0).short_name
        } else {
            ""
        }
    }

    def quizQuestionSizeName:String = {
        val aclist = DomainTable.findAll (
            By(DomainTable.cod, this.quizQuestionSize.toString),
            By(DomainTable.domain_name,"quizQuestionSize"))
        if (aclist.length > 0) {
            aclist(0).short_name
        } else {
            ""
        }
    }

    def quizQuestionTypeName:String = {
        val aclist = DomainTable.findAll (
            By(DomainTable.cod, this.quizQuestionType.toString),
            By(DomainTable.domain_name,"quizQuestionType"))
        if (aclist.length > 0) {
            aclist(0).short_name
        } else {
            ""
        }
    }

    def domain (question: Long, quizApplyingId: Long, print:Boolean): List[QuizDomainItem] = quizDomain.obj match {
        case Full(d) => {
            def sql = if (print) {
                """ (id in (select  to_number (qa.valuestr,'999999') from quizanswer qa
                where qa.quizapplying = ? and qa.quizquestion = ?)) 
                """
            } else {
                " ( 1 = 1 or ? > -2 or ? > -2)"
            }
            QuizDomainItem.findAll(
            By(QuizDomainItem.quizDomain, quizDomain.is), 
            By(QuizDomainItem.status, 1), 
            BySql (sql, IHaveValidatedThisSQL("",""), quizApplyingId, question),
            OrderBy(QuizDomainItem.orderInDomain, Ascending))
        }
        case _ => Nil
    }

    def domainItemNoAnswer = {
        val ac = QuizDomainItem.findAll (
            By (QuizDomainItem.quizDomain, quizDomain),
            By (QuizDomainItem.valueStr, ""));
        if (ac.length > 0) {
            ac(0).id
        } else {
            0;
        }
    }
    override def delete_! = {
        if(QuizAnswer.count(By(QuizAnswer.quizQuestion,this.id)) > 0){
           throw new RuntimeException("Existe resposta para esta questão!")
        }
        super.delete_!
    }

/*
Select para testar o update
select name, orderinsection, 
((select count (*) from quizquestion t1 where t1.company = quizquestion.company 
    and t1.quizsection = quizquestion.quizsection 
    and (t1.orderinsection < quizquestion.orderinsection or (t1.orderinsection = quizquestion.orderinsection and t1.id < quizquestion.id)))+1) * 10
from quizquestion where company = 398 and quizsection = 7;
*/
  val SQL_UPDATE_ORDER_10_10 = """
    update quizquestion set orderinsection = 
    ((select count (*) from quizquestion t1 where t1.company = quizquestion.company 
        and t1.quizsection = quizquestion.quizsection 
        and (t1.orderinsection < quizquestion.orderinsection or (t1.orderinsection = quizquestion.orderinsection and t1.id < quizquestion.id)))+1) * 10
    where company = ? and quizsection = ?;
  """
  override def save() = {
    if (!this.quizDomain.isEmpty && 
        (this.quizQuestionType == 0)) {
        throw new RuntimeException ("Se tem domínio o tipo não deve ser texto")
    }
    if ((quizSection.isEmpty)) {
      throw new RuntimeException("Não é permitido questão sem seção")
    }
    if (this.message.length >= 40000) {
      throw new RuntimeException("Texto muito grande, verifique se o conteúdo nao foi truncado! " + this.message.length + " de um máximo de 40.000 caracteres")
    }
    val r = super.save

    DB.runUpdate(SQL_UPDATE_ORDER_10_10, this.company.obj.get.id.is :: this.quizSection.is :: Nil)

    r
  }

}

object QuizQuestion extends QuizQuestion with LongKeyedMapperPerCompany[QuizQuestion]  
    with  OnlyActive[QuizQuestion] {
/*
    object QuizQuestionType extends Enumeration {
        type QuizQuestionType = Value
        val text, paragraph, choosefromlist, multiplechoice, severaloptions = Value
    }
*/
/*
    val types = Seq(
                QuizQuestionType.text.toString -> "Texto",
                QuizQuestionType.paragraph.toString -> "Parágrafo",
                QuizQuestionType.choosefromlist.toString -> "Escolha de uma lista"
    )
*/
}

