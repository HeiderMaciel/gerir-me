package code
package snippet

import scala.xml.{NodeSeq, Text, Elem}

import java.io.{File, FileOutputStream, PrintWriter}
import code.util._

import net.liftweb._
import util._
import common._
import mapper._

import Helpers._
import http._
import S._
import js.JsCmds.Noop

import scala.io.{Source, Codec}


class UploadCnab extends Logger {

  def account = S.param("account240") match {
    case Full(s) => s
    case _ => ""
  } 

  object imageFile extends RequestVar[Box[FileParamHolder]](Empty)
  object fileName extends RequestVar[Box[String]](Full(Helpers.nextFuncName))
  private def prepareOfxFile(file:File){
    val fileContent = Source.fromFile( file )(Codec.UTF8).mkString
    val out = new PrintWriter( file , "UTF-8")
    try{ 
        out.print( fileContent.replaceAll("\\,","\\.") ) 
      }finally{ 
        out.close 
      }
  }
  private def saveFile(fp: FileParamHolder): Unit = {
    fp.file match {
      case null =>
      case x if x.length == 0 => info("File size is 0")
      case x =>{
        
        val filePath = "/tmp/"

        val oFile = new File(filePath,  fileName.is.openOr("BrokenLink") + fp.fileName.takeRight(4))
        val output = new FileOutputStream(oFile)
        output.write(fp.file)
        output.close()
        prepareOfxFile(oFile)
        info("File uploaded!")
        S.notice (CnabUtil.execute(oFile, account));
        //S.notice("Arquivo importado com sucesso!")
      }
    }
  }

  def render ={
    // process the form
    def process() {

      (imageFile.is) match {
        case (Empty) => S.error("Selecione um arquivo")
        case (image) => {
          info("The RequestVar content is: %s".format(imageFile.is))
          imageFile.is.map{ info("About to start the file upload"); file => saveFile(file)}
          info("Done")
        }
      }

    }
    uploadImg &
    "type=submit" #> SHtml.onSubmitUnit(process)
  }


  def uploadImg: CssBindFunc = {
    /**
     * If it is a GET request, show the upload field,
     * else show a link to the image we just uploaded.
     */
    (S.get_?, imageFile.is) match {
      case (true, _)  => "name=file" #> SHtml.fileUpload(s => imageFile(Full(s)))
      case (_, Empty) => "name=file" #> SHtml.fileUpload(s => imageFile(Full(s)))
      case (_, _)    => "name=file" #> SHtml.fileUpload(s => imageFile(Full(s)))
      case _ => "name=file" #> SHtml.fileUpload(s => imageFile(Full(s)))
    }
  }


}