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

class QuizDomainItem extends Audited[QuizDomainItem] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with NameSearchble[QuizDomainItem] with ActiveInactivable[QuizDomainItem]{ 
    def getSingleton = QuizDomainItem
    override def updateShortName = false
    // diferente de serviço categoria etc, 
    // seção, questão e item de dominio podem ser duplicados
    override def allowDuplicated_? = true;
    object quizDomain extends  MappedLongForeignKey(this, QuizDomain)
    object obs extends MappedPoliteString(this,255)
    object orderInDomain extends MappedInt(this){
	    override def defaultValue = 1000
	}
    object rank extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object valueStr extends MappedPoliteString (this,255)
    object valueNum extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object valueNumEnd extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object color extends MappedPoliteString(this, 55) with LifecycleCallbacks {
      override def defaultValue = "";
      override def beforeSave() {
          super.beforeSave;
          // não deixa setar branco pq é o default do color picker
          if(this.get == "#FFFFFF"){ 
            this.set("")
          }
      } 
    }
    
    def quizDomainName = quizDomain.obj match {
        case Full(t) => t.short_name.is
        case _ => ""
    }

    override def delete_! = {
        if(QuizAnswer.count(By(QuizAnswer.quizDomainItem,this.id)) > 0){
           throw new RuntimeException("Existe resposta com este item de domínio!")
        }

        super.delete_!
    }

/*
Select para testar o update
select name, orderindomain, 
((select count (*) from quizdomainitem t1 where t1.company = quizdomainitem.company 
    and t1.quizdomain = quizdomainitem.quizdomain 
    and (t1.orderindomain < quizdomainitem.orderindomain or (t1.orderindomain = quizdomainitem.orderindomain and t1.id < quizdomainitem.id)))+1) * 10
from quizdomainitem where company = 398 and quizdomain = 7;
*/
  val SQL_UPDATE_ORDER_10_10 = """
    update quizdomainitem set orderindomain = 
    ((select count (*) from quizdomainitem t1 where t1.company = quizdomainitem.company 
        and t1.quizdomain = quizdomainitem.quizdomain 
        and (t1.orderindomain < quizdomainitem.orderindomain or (t1.orderindomain = quizdomainitem.orderindomain and t1.id < quizdomainitem.id)))+1) * 10
    where company = ? and quizdomain = ?;
  """
  override def save() = {
    if (isNew) {
      if (this.valueStr == "") {
        this.valueStr(BusinessRulesUtil.toCamelCase(this.name))
      }
    }

    if ((quizDomain.isEmpty)) {
      throw new RuntimeException("Não é permitido item sem domínio")
    }

    val r = super.save

    DB.runUpdate(SQL_UPDATE_ORDER_10_10, this.company.obj.get.id.is :: this.quizDomain.is :: Nil)

    r
  }

}

object QuizDomainItem extends QuizDomainItem with LongKeyedMapperPerCompany[QuizDomainItem]  
    with  OnlyActive[QuizDomainItem]
