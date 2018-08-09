package code
package snippet

import scala.xml.{NodeSeq, Text, Elem}

import java.io.{File,FileOutputStream,ByteArrayInputStream}
import javax.imageio._
import code.util._

import net.liftweb._
import util._
import common._
import mapper._

import Helpers._
import http._
import S._
import js.JsCmds.Noop

import code.model._



class UploadImageCustomer extends SnippetUploadImage {
  override def resize_? = false
  def setImageToEntity(homeName:String, thumbName:String){
  	val ac = Customer.findByKey (S.param("id").get.toLong).get
    ImageCustomer.createInCompany.
	    customer(ac.id.is).
    	image(homeName).
    	imagethumb(thumbName).save
//        S.notice("Imagem enviada com sucesso!")
   		if (ac.is_animal_?.is) {
	   		S.redirectTo("/animal/edit_animal?id="+ac.id.is)
   		} else {
	   		S.redirectTo("/customer/edit?id="+ac.id.is)
   		}
  }
  def imageFolder:String = ImageCustomer.imagePath
  def thumbToShow:NodeSeq = ImageCustomer.findByKey(S.param("id").get.toLong).get.thumb("128")
}

