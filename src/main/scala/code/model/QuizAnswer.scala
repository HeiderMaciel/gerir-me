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

class QuizAnswer extends Audited[QuizAnswer] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
    with ActiveInactivable[QuizAnswer]{ 
    def getSingleton = QuizAnswer
    object quizApplying extends MappedLongForeignKey(this, QuizApplying)
    object quizQuestion extends MappedLongForeignKey(this, QuizQuestion)
    object obs extends MappedPoliteString(this,255)
    object quizDomainItem extends MappedLongForeignKey(this, QuizDomainItem)
    object rank extends MappedDecimal(this,MathContext.DECIMAL64,4)
    object valueStr extends MappedPoliteString (this,40000)
    object valueNum extends MappedDecimal(this,MathContext.DECIMAL64,4)
}

object QuizAnswer extends QuizAnswer with LongKeyedMapperPerCompany[QuizAnswer]  
    with  OnlyActive[QuizAnswer]

