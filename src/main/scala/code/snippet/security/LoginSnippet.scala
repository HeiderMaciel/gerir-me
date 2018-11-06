package code
package snippet

import net.liftweb._
import http._
import code.actors._
import code.util._
import model._
import http.js._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text }
import java.util.Random

object LoginSnippet extends net.liftweb.common.Logger {
  val rand = new Random(System.currentTimeMillis())

  def randomNumber = rand.nextInt(4)

  def logOut(in: NodeSeq): NodeSeq = {
   
    val isegrex = AuthUtil.company.appType.isEgrex
    val website = "http://"+AuthUtil.company.website
    val user = AuthUtil.user.id.is
    // pra ver se é cliente simples de agenda online
    // não é profissional
    val userTrue = AuthUtil.user.is_employee_?;
    val appShortName = AuthUtil.company.appShortName;
    val calendarUrl = AuthUtil.company.calendarUrl
    AuthUtil >>;

/*  voltar quando estabilizar o login por e-mail
    colocar tb o portal ebelle na lista
*/
    val straux = "https://www.facebook.com/VilaRikaSolucoes"::
    "https://pt-br.facebook.com/ebellegestao"::
//    "http://www.vilarika.com.br/":: tava caindo no boleto - para heider arrumar
    "http://www.vilarika.com"::
    "https://www.facebook.com/ephysiogestao"::
    "http://www.gerirme.com.br"::
    Nil
      // para diferenciar ebelle e gerirme
    if (userTrue) {
      if (!isegrex) {
        // se local ou meu usuario rigel - volta pro login
        // se não for user true é cliente, ai vai no random mesmo
        if (S.hostName.contains ("local") && userTrue) {
          S.redirectTo("http://"+S.hostName+":7171/v2/login")
        } else if (user == 3 /* rigel*/) {
          S.redirectTo("http://"+S.hostName+"/v2/login")
        } else {
          S.redirectTo(straux(randomNumber))
          //S.redirectTo("http://"+S.hostName+"/v2/login")
        }
      } else {
        S.redirectTo(website)
      }
    } else {
      S.redirectTo("http://"+appShortName+".com.br/agenda.html?id="+calendarUrl);
    }
//       AuthUtil >>;
    in
  }

  def userName (in: NodeSeq): NodeSeq = {
    Text(AuthUtil.user.short_name.substring(0,scala.math.min(AuthUtil.user.short_name.length, 10)))
  }

  def companyInfos (in: NodeSeq): NodeSeq = {
    val straux1 = if  (AuthUtil.company.name.is != AuthUtil.unit.name.is) AuthUtil.unit.short_name.is else ""
    val straux0 = if (AuthUtil.user.isSuperAdmin) AuthUtil.company.id.is+" " else " - "
    Text(straux0 +AuthUtil.company.short_name.is+ " " + straux1)
  }

  def imageGravatar (in: NodeSeq): NodeSeq = {
    val email = AuthUtil.email
    val mail = email match {
      case mail:String if(mail != null) => Project.md5(mail.trim.toLowerCase)
      case _ => ""
    }
    
    
    <img id="avatar" style="width:24px" src={AuthUtil.user.thumbPath}/>
  }

}