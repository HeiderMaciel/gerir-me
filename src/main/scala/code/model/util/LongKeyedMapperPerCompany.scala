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

trait LongKeyedMapperPerCompany[A <: LongKeyedMapper[A]] extends OnlyCurrentCompany[A] with LongKeyedMetaMapper[A]  {
    self: A  with PerCompany with MetaMapper[A] => 
    override def findByKey(id:Long):Box[A] = {
      val ret = super.findByKey(id)
      ret match {
        case Full(obj) => {
          if( (AuthUtil.? && obj.asInstanceOf[PerCompany].company.is != AuthUtil.company.id.is) && 
            (!AuthUtil.user.isSuperAdmin && !AuthUtil.user.isSupportAdmin)){
            throw new RuntimeException("Não Encontrado " + id)
          }
        }
        case _ => 
      }
      
      ret
    }
  }