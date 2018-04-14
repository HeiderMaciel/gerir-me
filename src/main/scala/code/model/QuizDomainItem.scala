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
    object quizDomain extends  MappedLongForeignKey(this, QuizDomain)
    object obs extends MappedPoliteString(this,255)
    object orderInDomain extends MappedInt(this){
	    override def defaultValue = 10
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

}

object QuizDomainItem extends QuizDomainItem with LongKeyedMapperPerCompany[QuizDomainItem]  
    with  OnlyActive[QuizDomainItem]
