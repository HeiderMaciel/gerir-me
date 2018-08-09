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



trait SnippetUploadImage {

  object imageFile extends RequestVar[Box[FileParamHolder]](Empty)
  object fileName extends RequestVar[Box[String]](Full(Helpers.nextFuncName))
  def resize_? = false // rigel setei false 21/09/2015 true
  private def saveFile(fp: FileParamHolder): Unit = {
    fp.file match {
      case null =>
      case x if x.length == 0 => 
      case x =>{
        
        val filePath = if(Project.isLinuxServer){
          if (!Project.isLocalHost || 
            Props.get("photo.local") == "false") {
            (Props.get("photo.path") openOr "/tmp/")+imageFolder
          } else {
            "/var/www/html/images/"+imageFolder            
          }
        }else{
          "c:\\vilarika\\"+imageFolder
        }

        /**
         * Here we save the file to the File System
         * I'm 99% sure I could use open_! but I'd rather get
         * a broken link than a NPE
         */
        val name = fileName.is.openOr("BrokenLink") + fp.fileName.takeRight(4)
        val thumbName = "thumb_"+name+".png";
        val homeName = "home_"+name+".png"
        val oFileThumb = new File(filePath,  thumbName)
        val oFileHome = new File(filePath,  homeName)
        if(resize_?){
          ImageIO.write(ImageResizer.resize(new ByteArrayInputStream(fp.file),70,130), "png", oFileThumb);
          ImageIO.write(ImageResizer.resize(new ByteArrayInputStream(fp.file),430,230), "png", oFileHome);
        }else{
          try {
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(fp.file)),"png", oFileThumb);
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(fp.file)), "png", oFileHome);
          }catch{
            case e:Exception => S.error(e.getMessage + " " + filePath + " " + homeName)
          }
        }

        setImageToEntity(homeName, thumbName);
        S.notice("Imagem enviada com sucesso!")
      }
    }
  }
  def setImageToEntity(homeName:String, thumbName:String)
  def imageFolder:String
  def thumbToShow:NodeSeq
  def render ={
    // process the form
    def process() {

      (imageFile.is) match {
        case (Empty) => S.error("Selecione um arquivo")
        case (image) => {
          imageFile.is.map{ file => saveFile(file) }
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