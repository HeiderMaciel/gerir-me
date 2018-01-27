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

trait Imageble{
  self: BaseMapper =>
  object image extends MappedPoliteString(this.asInstanceOf[MapperType],455)
  object imagethumb extends MappedPoliteString(this.asInstanceOf[MapperType],455)
  // rigel - 27/01/2018 thumb_web estava sem imagePath
  def thumb_web = Props.get("photo.urlbase").get+imagePath+"/"+imagethumb.is
  def logo_web = Props.get("photo.urlbase").get+imagePath+"/"+image.is
  //def logo_web = Props.get("photo.urlbase").get+image.is
  def image_web = Props.get("photo.urlbase").get+image.is
  def imagePath:String // diretorio definido em cada model

  def thumb = imagethumb.is match {
    case img:String if(img!="") => <img style="width:24px" src={thumbPath}/>
    case _ => <span/>
  }

  def thumb(size:String) = imagethumb.is match {
    case img:String if(img!="") => <img style={"width:"+size+"px"} src={thumbPath}/>
    case _ => <span/>
  }  
  
  def thumbPath = imagethumb.is match {
    case img:String if(img!="") => 
      if (!Project.isLocalHost || true) {
        Props.get("photo.urlbase").get+imagePath+"/"+img
      } else {
        "/images/"+imagePath+"/"+img
      }
    case _ => ""
  }  
}


