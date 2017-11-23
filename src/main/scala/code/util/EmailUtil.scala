package code
package util

import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import S._
import code.model._
import net.liftweb.util.Mailer
import Mailer._
import scala.xml._
import javax.mail._
import javax.mail.internet._
import java.util.Properties

object EmailUtil {

  val serverSmtp:String = Props.get("mail.smtp") openOr ""
  val userSmtp:String = Props.get("mail.user") openOr ""
  val paswordSmtp:String = Props.get("mail.password") openOr ""
  val portSmtp:String = Props.get("mail.port") openOr ""
  // (!S.hostName.contains ("local")) - Resolver - rigel no linux traz o nome da máquina
  val local1:String = if (Project.isLocalHost) {
      " - Base local "
    }else{
      " "
    }

  def prefix = if (Project.isLocalHost) { "baselocal" } else {""}

  def product1:String = if (S.hostName.contains ("gerir")) {
      " gerir-me "
    }else if (S.hostName.contains ("egrex")) {
      " e-grex "
    }else if (S.hostName.contains ("esmile") || S.hostName.contains ("e-smile")) {
      " e-smile "
    }else if (S.hostName.contains ("edoctus")) {
      " e-doctus "
    }else if (S.hostName.contains ("efisio") || S.hostName.contains ("ephysio")) {
      " e-physio "
    }else if (S.hostName.contains ("ebellepet")) {
      " e-bellepet "
    }else{
      " e-belle "
    }

  def lnkProduct1:String = if (S.hostName.contains ("gerir")) {
      "gerirme"
    }else if (S.hostName.contains ("egrex")) {
      "egrex"
    }else if (S.hostName.contains ("esmile") || S.hostName.contains ("e-smile")) {
      "esmile"
    }else if (S.hostName.contains ("edoctus")) {
      "edoctus"
    }else if (S.hostName.contains ("efisio") || S.hostName.contains ("ephysio")) {
      "ephysio" // trocar quando link pronto
    }else if (S.hostName.contains ("ebellepet")) {
      "ebellepet"
    }else{
      "ebelle"
    }
  
  def authDefalt {
    System.setProperty("mail.smtp.starttls.enable","true");
    // Set the host name
    System.setProperty("mail.smtp.ssl.enable", "true")
    System.setProperty("mail.debug", "true")//Em produção o jetty ta dando pal deixar false por hora
    System.setProperty("mail.smtp.host", serverSmtp) // Enable authentication
    System.setProperty("mail.smtp.port", portSmtp) // SMTP port
    System.setProperty("mail.smtp.auth", "true") // Provide a means for authentication. Pass it a Can, which can either be Full or Empty    
    Mailer.authenticator = Full(new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication(userSmtp, paswordSmtp)
    })    
  }


  def authDefaltUnit(companyUnit:CompanyUnit) {
    System.setProperty("mail.smtp.starttls.enable","true");
    // Set the host name
    System.setProperty("mail.smtp.ssl.enable", "false")
    System.setProperty("mail.debug", "true")//Em produção o jetty ta dando pau deixar false por hora
    System.setProperty("mail.smtp.host", companyUnit.smtpServer.is) // Enable authentication
    System.setProperty("mail.smtp.port", companyUnit.port.is) // SMTP port
    System.setProperty("mail.smtp.auth", "true") // Provide a means for authentication. Pass it a Can, which can either be Full or Empty    
    System.setProperty("mail.transport.protocol", "smtp");
    System.setProperty("mail.smtp.socketFactory.port", companyUnit.port.is);
    System.setProperty("mail.smtp.socketFactory.fallback", "false");
    System.setProperty("mail.smtp.starttls.enable", "true");

    Mailer.authenticator = Full(new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication(companyUnit.userSmtp.is, companyUnit.passwordSmtp.is)
    })    
  }  


  val from:String = Props.get("mail.user") openOr ""
  val admins = if ((S.hostName.contains ("local")) || (S.hostName.contains ("rigel"))) {
      "rigel.ferreira@vilarika.com.br" :: Nil
    } else {
      "mateus.freira@gmail.com" :: "rigel.ferreira@hotmail.com" :: 
      "rigel.ferreira@gmail.com" :: 
      "suporte@vilarika.com.br" :: 
      "karina_pisa@hotmail.com" :: "hrmaciel@vilarika.com.br" :: Nil
  }
  //val rigeltest = "rigel.ferreira@gmail.com" :: "rigel.ferreira@hotmail.com" :: "rigel.ferreira@vilarika.com.br" :: Nil
//  def sendMailTo(monthly:Monthly,mail:NodeSeq,title:String){
//    sendMailTo(admins,mail,title)
//  }
  def sendMailTo(company:Company,mail:NodeSeq,title:String){
    sendMailTo(company.email.is.toString,mail,title,company)
  }
  def compleMail(mail:NodeSeq,footerHtml:NodeSeq):NodeSeq = <div>{mail}<br/>{footerHtml}</div>
  def simpleCompleMail(mail:NodeSeq,footerHtml:NodeSeq):NodeSeq = <div>{mail}<br/>{footerHtml}</div>
  def sendMailTo(email:List[String],mail:NodeSeq,title:String){
    email.foreach((em)=>{
        sendMailTo(em, mail, title)
      })
  }

  def sendMailTo(email:String,mail:NodeSeq,title:String, 
    company:Company = AuthUtil.company, attachment:Attachment = EmptyAttachment()){
      authDefalt
      email.split(",|;").foreach((email) => {
        val log = LogMailSend.create.
          message(mail.toString).
          to(prefix + email).from(from).
          subject(title).company(company.id)
        log.save
        val toSend:MailBodyType =  attachment match {
          case a:FullAttachment =>  if (!company.appType.isEgrex) {
            XHTMLPlusImages(compleMail(mail,teamfooterHtml(log.id.is,company.appShortName)), PlusImageHolder(a.name, a.mimeType, a.bytes))
          } else {
            XHTMLPlusImages(compleMail(mail,simpleFooterHtmlEgrex(log.id.is, company)), PlusImageHolder(a.name, a.mimeType, a.bytes))
          }
          case _ => if (!company.appType.isEgrex) {
              compleMail(mail,teamfooterHtml(log.id.is, company.appShortName))
            } else {
              compleMail(mail,simpleFooterHtmlEgrex(log.id.is, company))
            }
        }

        def product2 = if (company != null) {
            company.appShortName;
          } else {
            "Vila Rika"
          }

        // este teste nao funciona - por isso o remendo das var com 1 a frente 
        def companyEmail = if (company != null) {
            company.email.is;
          } else {
            "suporte@vilarika.com.br"
          }

        def companyName = if (company != null) {
            company.name.is;
          } else {
            "VilarikA Soluções OnLine"
          }

        var companyEmail1 = if (companyEmail != "") {
            companyEmail.split(",|;")(0)
          } else {
            "suporte@vilarika.com.br"
          }
        var companyName1 = if (companyName != "") {
            companyName
          } else {
            "VilarikA Soluções OnLine"
          }
        try {
  //    println ("vaii =================== try no email util GENERICO")

        Thread.sleep (500);
        Mailer.blockingSendMail(
           Mailer.From(from, Full("Notificações " + product2 + " " + local1)),
           Mailer.Subject(title),
           Mailer.To(prefix + email),
           Mailer.ReplyTo(prefix + companyEmail1, Full(companyName)),
           toSend
           )
        }catch{
          // acho que nao precisa desse log pq acho que nao é erro
          case e:Exception => {
            //e.printStackTrace
            println ("vaiiii ========================= Erro ao enviar email GENERICO - "+e.getMessage)
            log.subject(title + " ERRO ======= ").save
//            LogActor ! "Erro ao enviar email - "+e.getMessage
            Thread.sleep (60000);
            Mailer.blockingSendMail(
               Mailer.From(from, Full("Notificações " + product2 + " " + local1)),
               Mailer.Subject(title),
               Mailer.To(prefix + email),
               Mailer.ReplyTo(prefix + companyEmail1, Full(companyName)),
               toSend
               )
          }
          case _ =>
        }
      })
  }

  def sendMailCustomer(companyUnit:CompanyUnit,company:Company,email:String,
    mail:NodeSeq,title:String,bp:Long,attachment:Attachment = EmptyAttachment()){
      //authDefaltUnit(companyUnit)
      email.split(",|;").foreach((email) => {
        val log = LogMailSend.create.message(mail.toString).
        to(prefix + email).from(from).subject(title).
        business_pattern(bp).company(company.id)
        log.save

        val toSend:companyUnit.mailer.MailBodyType =  attachment match {
          case a:FullAttachment =>  if (!company.appType.isEgrex) {
            companyUnit.mailer.XHTMLPlusImages(compleMail(mail,teamfooterHtml(log.id.is,company.appShortName)), companyUnit.mailer.PlusImageHolder(a.name, a.mimeType, a.bytes))
          } else {
            companyUnit.mailer.XHTMLPlusImages(compleMail(mail,simpleFooterHtmlEgrex(log.id.is, company)), companyUnit.mailer.PlusImageHolder(a.name, a.mimeType, a.bytes))
          }
          case _ => if (!company.appType.isEgrex) {
              companyUnit.mailer.XHTMLMailBodyType(compleMail(mail,teamfooterHtml(log.id.is, company.appShortName)))
            } else {
              companyUnit.mailer.XHTMLMailBodyType(compleMail(mail,simpleFooterHtmlEgrex(log.id.is, company)))
            }
        }

/* 
        val toSend = if (!company.appType.isEgrex) {
          compleMail(mail,simpleFooterHtml(log.id.is, company.appShortName))
        } else {
          compleMail(mail,simpleFooterHtmlEgrex(log.id.is, company))
        }
*/
        try{
          Thread.sleep (500);
//      println ("vaii =================== try no email util")
          companyUnit.mailer.blockingSendMail(
             companyUnit.mailer.From(company.email.is.split(",|;")(0), Full(company.name.is)),
             companyUnit.mailer.Subject(title),
             companyUnit.mailer.To(prefix + email),
             companyUnit.mailer.ReplyTo(company.email.is.split(",|;")(0), Full(company.name.is)),
//             companyUnit.mailer.XHTMLMailBodyType(toSend)
             toSend
             )        
        }catch{
          // acho que nao precisa desse log pq acho que nao é erro
          case e:Exception => {
            //e.printStackTrace
            println ("vaiiii ========================= Erro ao enviar email - "+e.getMessage)
            log.subject(title + " ERRO ======= ").save
//            LogActor ! "Erro ao enviar email - "+e.getMessage
            Thread.sleep (60000);
            companyUnit.mailer.blockingSendMail(
               companyUnit.mailer.From(company.email.is.split(",|;")(0), Full(company.name.is)),
               companyUnit.mailer.Subject(title),
               companyUnit.mailer.To(prefix + email),
               companyUnit.mailer.ReplyTo(company.email.is.split(",|;")(0), Full(company.name.is)),
  //             companyUnit.mailer.XHTMLMailBodyType(toSend)
               toSend
               )        
          }
          case _ =>
        }
      })
  }


  def rememberPasswordEMailUser(company:Company, user:User, product:String, lnkproduct:String) = <div>
                <img width="100px" src={company.thumb_web}/>
                <br/>
                 <h1>Olá {user.name.is} </h1><br/>
                  Clique no link abaixo para redefinir sua senha em nosso ambiente.
                 <div>
                    <a href={
                      if (S.hostName.contains ("local")) {
                      "http://localhost:7171/security/remember_key?info="+user.resetPasswordKey+"&_keepCalm="+user.id.is
                      } else {
                      "http://"+lnkproduct+".vilarika.com.br/security/remember_key?info="+user.resetPasswordKey+"&_keepCalm="+user.id.is
                      }
                      }>Redefinir senha</a>
                 </div>
                 <br/>
                 <div>
                    <span>Para acessar o {product}</span> <a href={"http://"+lnkproduct+".vilarika.com.br"}>{lnkproduct}.vilarika.com.br</a>
                 </div> 
                 <br/>
                 <span>Caso não tenha sido você a solicitar esse reenvio, por favor, desconsidere a mensagem </span>
          </div>
  def rememberPasswordEMailCustomer(company:Company, user:Customer, product:String, lnkproduct:String) = <div>
                <img width="100px" src={company.thumb_web}/>
                <br/>
                 <h1>Olá {user.name.is} </h1><br/>
                  Clique no link abaixo para redefinir sua senha em nosso ambiente.
                 <div>
                    <a href={
                      if (S.hostName.contains ("local")) {
                      "http://localhost:7171/security/remember_key_customer?info="+user.resetPasswordKey+"&_keepCalm="+user.id.is
                      } else {
                      "http://"+lnkproduct+".vilarika.com.br/security/remember_key_customer?info="+user.resetPasswordKey+"&_keepCalm="+user.id.is
                      }
                      }>Redefinir senha</a>
                 </div>
                 <br/>
                 <div>
                    <span>Para acessar o {product}</span> <a href={"http://"+lnkproduct+".vilarika.com.br"}>{lnkproduct}.vilarika.com.br</a>
                 </div> 
                 <br/>
                 <span>Caso não tenha sido você a solicitar esse reenvio, por favor, desconsidere a mensagem </span>
          </div>
  def welcomeEMail(user:User,company:Company) = <div>
                          Olá {company.contact.is}, <br/><br/>seja bem-vindo(a) ao {company.appShortName},<br/><br/>
                            
                           <b>Você tem 30 dias para experimentar o {company.appShortName} gratuitamente<br/>
                            Clique no link abaixo para criar uma senha e começar</b>
                           <br/> 
                           <br/>
                           <a href={"http://" + company.appShortName + ".vilarika.com.br/security/remember_key?info="+user.resetPasswordKey+"&_keepCalm="+user.id.is}>Clique aqui para começar</a>
                           <br/>
                           <br/>
                           <div>
                              <span>Para acessar o {company.appShortName}</span> <a href={"http://" + company.appShortName + ".vilarika.com.br"}>{company.appShortName}.vilarika.com.br</a>
                           </div>
                     </div>

  def sendWelcomeEMail(user:User) = {
    val company:Company = user.company.obj openOr null
    sendMailTo(company, welcomeEMail(user,company) ,"Cadastro " + {company.appShortName} + local1)
    sendNotificationRegistration(company)
  }

  def sendRememberEMail(email:String) = {
    val ct = User.countByEmail(email)
    if (ct > 0) {
      User.findByEmail(email).map((ac)=>{
        //println ("vaiii ===================== " + ac.email)
        //sendMailTo(ac.email.is, rememberPasswordEMail(ac.company.obj.get, user, product1, lnkProduct1), "Recuperar senha" + product1 + local1)
        sendMailCustomer(CompanyUnit.findByKey(ac.unit).get,
            Company.findByKey (ac.company).get, 
            ac.email.is, rememberPasswordEMailUser(ac.company.obj.get, ac, product1, lnkProduct1), 
            "Recuperar senha" + product1 + local1, ac.id.is)
      })
    } else {
      val cl = Customer.countByEmail(email)
      if (cl > 0) {
        Customer.findByEmail(email).map((ac)=>{
          //println ("vaiii ===================== " + ac.email)
          //sendMailTo(ac.email.is, rememberPasswordEMail(ac.company.obj.get, user, product1, lnkProduct1), "Recuperar senha" + product1 + local1)
          sendMailCustomer(CompanyUnit.findByKey(ac.unit).get,
              Company.findByKey (ac.company).get, 
              ac.email.is, rememberPasswordEMailCustomer(ac.company.obj.get, ac, product1, lnkProduct1), 
              "Recuperar senha" + product1 + local1, ac.id.is)
        })
      } else {
        throw new RuntimeException ("Email " + email + " não foi encontrado em nossa base de dados, por favor verifique.")
      }
    }
  }

  def notificationRegistrationHtml(company:Company, product:String) = <div>
                                      A empresa {company.name} de código {company.id.is.toString}, <br/>
                                      de {company.contact} <br/>
                                      email {company.email} - telefone {company.phone} <br/>
                                      solicitou hoje o uso do {company.appShortName}
                                      </div>

  def sendNotificationRegistration(company:Company)={
    sendMailTo(admins,notificationRegistrationHtml(company, company.appShortName),"Novo cliente " + company.appShortName + local1);
  }

  def  limitDiskPercent = 45;
  def sendDailyUtilization = {
    sendMailTo(admins,code.daily.DailyReport.dailyUtilizationInHtml,"Report Diário " + code.daily.DailyReport.diskUsed +"%/"+limitDiskPercent)
  }
  def sendDiskSpaceAlert = {
    if (code.daily.DailyReport.diskUsed > limitDiskPercent) {
      sendMailTo(admins,code.daily.DailyReport.diskSpaceAlertInHtml,"Disk Space Alert " + code.daily.DailyReport.diskUsed +"/"+limitDiskPercent)
    }
  }

//            <a href="http://" + {appName} + ".vilarika.com.br/"><img src="http://" + appName + ".vilarika.com.br/images/logo.png" style="width: 100px;"/></a>
//             <img src={"http://" + appName + ".vilarika.com.br/system/makeMailAsRead/"+id.toString} style="width: 0px;"/>

  def teamfooterHtml(id:Long, appName:String) = <div>
            Tenha um bom dia!<br/>
            Atenciosamente equipe {appName}
            <br/>
            <br/>
            <a href={"http://" + appName + ".vilarika.com.br/"}><img src={"http://" + appName + ".vilarika.com.br/images/logo_fbr_name_"+ appName+".png"} style="width: 50px;"/></a>
            <img src={if (Project.isLocalHost) {
              "localhost:7171/system/makeMailAsRead/"+id.toString
              } else {
              "http://" + appName + ".vilarika.com.br/system/makeMailAsRead/"+id.toString
              }
              } style="width: 0px;"/>
          </div>
  
  def simpleFooterHtml(id:Long, appName:String) = <div>
            <a href={"http://"+appName+".vilarika.com.br/"}> Enviado via <img src={"http://"+appName+".vilarika.com.br/images/logo_fbr_name_"+appName+".png"} style="width: 50px;"/></a>
            <img src={if (Project.isLocalHost) {
              "localhost:7171/system/makeMailAsRead/"+id.toString
              } else {
              "http://" + appName + ".vilarika.com.br/system/makeMailAsRead/"+id.toString
              }
              } style="width: 0px;"/>
          </div>
  def simpleFooterHtmlEgrex(id:Long, company:Company) = <div>
            <br/>
            <a href={"http://"+company.website}><img src="http://egrex.vilarika.com.br/images/Logo_cczs_colorida_s.png" style="width: 150px;"/></a>
            <br/><br/>
            <a href="http://egrex.vilarika.com.br/"> Enviado via <img src="http://egrex.vilarika.com.br/images/logo_vilarika.png" style="width: 60px;"/></a>
            <img src={"http://egrex.vilarika.com.br/system/makeMailAsRead/"+id.toString} style="width: 0px;"/>
          </div>

}
