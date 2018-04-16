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


class UploadOfx extends Logger {

  object imageFile extends RequestVar[Box[FileParamHolder]](Empty)
  object fileName extends RequestVar[Box[String]](Full(Helpers.nextFuncName))
  private def prepareOfxFile(file:File){
    // eu baixei arquivo no windows e trouxe por email pro linux
    // dava erro com UTF8 - troquei para ISO como já tinha feito no
    // contaazul - rigel 21/07/2017 - lá 
    // val fileContent = Source.fromFile( file )(Codec.UTF8).mkString
    var fileContent = Source.fromFile( file )(Codec.ISO8859).mkString
    val out = new PrintWriter( file , "UTF-8")
    if (fileContent.indexOf ("<FI>") == -1) {
      // provavelmente é itau, geram essa tag
      fileContent = fileContent.replaceAll ("<SONRS>",
        """<SONRS>
            <FI>
                <ORG>ITAU
                <FID>ITAU
            </FI>
        """)
    }
    try{ 
        out.print( fileContent.replaceAll("\\,","\\.")
        // rigel 21/07/2017  
        // a biblioteca java net.sf.ofx4j.io.AggregateUnmarshaller
        // reclama do fechamento dessas tags que não tem sub tabs, só valor
        // o santander não fecha mas o banco do brasil fecha
        .replaceAll ("</CODE>","")
        .replaceAll ("</SEVERITY>","") 

        .replaceAll ("</DTSERVER>","")
        .replaceAll ("</LANGUAGE>","") 
        .replaceAll ("</ORG>","")
        .replaceAll ("</FID>","") 
        .replaceAll ("</TRNUID>","") 
        .replaceAll ("</CURDEF>","") 

        .replaceAll ("</BANKID>","") 
        .replaceAll ("</BRANCHID>","") 
        .replaceAll ("</ACCTID>","") 
        .replaceAll ("</ACCTTYPE>","") 

        .replaceAll ("</DTSTART>","") 
        .replaceAll ("</DTEND>","") 

        .replaceAll ("</TRNTYPE>","") 
        .replaceAll ("</DTPOSTED>","") 
        .replaceAll ("</TRNAMT>","") 
        .replaceAll ("</FITID>","") 
        .replaceAll ("</CHECKNUM>","") 
        .replaceAll ("</REFNUM>","") 
        .replaceAll ("</MEMO>","") 
        .replaceAll ("</CURRATE>","") 
        .replaceAll ("</CURSYM>","") 

        .replaceAll ("</BALAMT>","") 
        .replaceAll ("</DTASOF>","") 
        // rigel 25/08/2017
        // cartão de credito a ofx4j não trata
        // no BB tive que substituir estas tags a seguir
        .replaceAll ("CREDITCARDMSGSRSV1","BANKMSGSRSV1") 
        .replaceAll ("CCSTMTTRNRS","STMTTRNRS") 
        .replaceAll ("CCSTMTRS","STMTRS") 
        .replaceAll ("CCACCTFROM","BANKACCTFROM") 

        ) 
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
        val msg = OfxUtil.execute(oFile, S.param("category").get.toLong, S.param("account").get.toLong);
        S.notice("Arquivo importado com sucesso!" + msg)
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