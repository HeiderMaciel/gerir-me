
package code
package util

import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import S._
import code.model._
import code.actors._
import net.liftweb.util.Mailer
import Mailer._
import scala.xml._
import javax.mail._
import javax.mail.internet._
import java.util.Properties

case class MailerUtil(companyUnit:CompanyUnit) extends Mailer with net.liftweb.common.Logger {
  if(companyUnit.smtpServer.is == "" || companyUnit.passwordSmtp.is == "" || companyUnit.passwordSmtp.is == ""){
    // Seta os valores default de configuração
    companyUnit.
    smtpServer(companyUnit.smtpServer.defaultValue).
    userSmtp(companyUnit.userSmtp.defaultValue).
    passwordSmtp(companyUnit.passwordSmtp.defaultValue).
    port(companyUnit.port.defaultValue).
    smtp_ssl_?(companyUnit.smtp_ssl_?.defaultValue).
    save    
    //info ("Config")
    LogActor ! "Configurações de envio email foram feitas na unidade corrente " + companyUnit.name.is
    //throw new RuntimeException("Configurações de envio email devem ser feitas na unidade corrente " + companyUnit.name.is)
  }
  customProperties = {
      println ("vaii =================== smtpmap ")
    companyUnit.SMTPMap
  }

  authenticator = Full(new Authenticator {
    override def getPasswordAuthentication = {
      try {
        println ("vaii =================== authenticator")
        new PasswordAuthentication(companyUnit.userSmtp.is, companyUnit.passwordSmtp.is)
      }catch{
        // acho que nao precisa desse log pq acho que nao é erro
        case e:Exception => {
          //e.printStackTrace
          println ("vaiiii ========================= Erro authenticator - "+e.getMessage)
          new PasswordAuthentication(companyUnit.userSmtp.is, companyUnit.passwordSmtp.is)
        }
        case _ => {
        new PasswordAuthentication(companyUnit.userSmtp.is, companyUnit.passwordSmtp.is)
      }
      }  
    }
  })
  override lazy val properties: Properties = {
    try {
      println ("vaii =================== try no Mailerutil")
      val p = System.getProperties.clone.asInstanceOf[Properties]
      customProperties.foreach {case (name, value) => p.put(name, value)}
      p
    }catch{
      // acho que nao precisa desse log pq acho que nao é erro
      case e:Exception => {
        //e.printStackTrace
        println ("vaiiii ========================= Erro ao enviar mailer util- "+e.getMessage)
//            LogActor ! "Erro ao enviar email - "+e.getMessage
        val p = System.getProperties.clone.asInstanceOf[Properties]
        customProperties.foreach {case (name, value) => p.put(name, value)}
        p
      }
      case _ => {
        val p = System.getProperties.clone.asInstanceOf[Properties]
        customProperties.foreach {case (name, value) => p.put(name, value)}
        p
      }
    }
  }
}
trait Attachment extends scala.Product
case class FullAttachment(name: String, mimeType: String, bytes: Array[Byte]) extends Attachment()
case class EmptyAttachment() extends Attachment()