package code
package api

import code.model._
import code.util._
import code.service._

import net.liftweb._
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers
import net.liftweb.mapper._ 

import scala.xml._

import java.text.ParseException
import java.util.Date
import java.util.Calendar

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.

object SecurityApi extends RestHelper with net.liftweb.common.Logger {
  serve {
    case "security" :: "companyParameters" :: Nil JsonGet _ => {
      AuthUtil.company.asJs
    }
    case "security" :: "unitParameters" :: Nil JsonGet _ => {
      if(AuthUtil ?)
        AuthUtil.unit.asJs
      else
        JInt(1)
    }    
    case "security" :: "userParameters" :: Nil JsonGet _ => {
      if(AuthUtil ?)
        AuthUtil.user.asJs
      else
        JInt(1)
    }    
    case "security" :: "emailRulesCheck" :: Nil JsonGet _ => {
      if(AuthUtil ?){
        JsObj(
          ("name", AuthUtil.user.name.is),
          ("email", AuthUtil.user.email.is)
        )
      }else{
        JInt(1)
      }
    }
    case "security" :: "changePassword" :: Nil Post _ => {
      try {
        if (AuthUtil.user.is_employee_$qmark) {
          AuthUtil.user.password(S.param("password").get).save
        } else {
          // é cliente de agenda publica
          // ai não pode validar por exemplo email duplicado
          val ac = Customer.findByKey (AuthUtil.user.id.is).get
          ac.password(S.param("password").get).save
        }
        JInt(1)
      }catch{
        case e:Exception => JString(e.getMessage)
        case e:RuntimeException => JString(e.getMessage)
      }     
    }
    case "security" :: "unRegisterFacebook" :: Nil Post _ => {
      AuthUtil.user.facebookId("").facebookUsername("").facebookAccessToken("").save
      JInt(1)
    }

    case "security" :: "remember_key" :: Nil JsonGet _ => {
      User.findByKey((S.param("_keepCalm") openOr "0").toLong) match {
        case Full(u) => {
          if (u.resetPasswordKey == (S.param("info") openOr "0")) {
            AuthUtil << u
            S.redirectTo("/security/change_password")
            JInt(1)
          } else {
            S.redirectTo("/")
            JInt(1)
          }
        }
        case _ => {
          S.redirectTo("/")
          JInt(1)
        }
      }
    }

    case "security" :: "remember_key_customer" :: Nil JsonGet _ => {
      Customer.findByKey((S.param("_keepCalm") openOr "0").toLong) match {
        case Full(u) => {
          if (u.resetPasswordKey == (S.param("info") openOr "0")) {
            AuthUtil << u
            S.redirectTo("/security/change_password")
            JInt(1)
          } else {
            S.redirectTo("/")
            JInt(1)
          }
        }
        case _ => {
          S.redirectTo("/")
          JInt(1)
        }
      }
    }

    case "security" :: "remember_user_password" :: Nil Post (r) => {
      val json = parse(new String(r.body.get))
      val remember = json.extract[RememberDto]
      try {
println ("vaiiiii ===== remember user company " + remember.company)
        EmailUtil.sendRememberEMail(remember.email,
          true /*user */, remember.company); 
        JInt(1)
      }catch{
        case e:Exception => JString(e.getMessage)
      }     
    }

    case "security" :: "remember_customer_password" :: Nil Post (r) => {
      val json = parse(new String(r.body.get))
      val remember = json.extract[RememberDto]
      try {
println ("vaiiiii ===== remember customer company " + remember.company)
        EmailUtil.sendRememberEMail(remember.email, 
          false/*customer*/, remember.company);
        JString ("1")
      }catch{
        case e:Exception => JString(e.getMessage)
      }     
    }
    case "security" :: "useCompany" :: Nil JsonGet (r) => {
      def coid:String = S.param("id") match {
        case Full(p) => p
        case _ => "0"
      }
      AuthUtil.checkSupportAdmin
      if (!AuthUtil.user.isSuperAdmin) {
        val ac = User.findByKey (AuthUtil.user.id).get
        if (UserCompanyUnit.findAll (
          By(UserCompanyUnit.status,1),
          By(UserCompanyUnit.company,coid.toLong),
          By(UserCompanyUnit.user,AuthUtil.user.id)
          ).length < 1 && (coid.toLong != ac.company.is)) {
          AuthUtil.checkSuperAdmin
        }
      }
      val company = Company.findByKey((S.param("id") openOr "0").toLong).get
      AuthUtil.user.company(company).unit(company.mainUnit)
      AuthUtil << AuthUtil.user
      AuthUtil << company.mainUnit
      S.redirectTo("/calendar")
      JInt(1)
    }
    case "security" :: "useModule" :: Nil JsonGet (r) => {
      AuthUtil.checkSuperAdmin
      val ac = PermissionModule.findByKey((S.param("id") openOr "0").toLong).get
      if (ac.status == 1) {
        ac.status (0);
      } else {
        ac.status (1);
      }
      ac.save
      S.redirectTo("/manager/modules")
      JInt(1)
    }
    case "security" :: "attrSupport" :: Nil JsonGet (r) => {
      AuthUtil.checkSuperAdmin
      def id:String = S.param("id") match {
        case Full(p) => p
        case _ => "0"
      }
      val ac = UserCompanyUnit.findAll(
        By (UserCompanyUnit.company, AuthUtil.company.id),
        By (UserCompanyUnit.user, id.toLong))
      if (ac.length < 1) {
        UserCompanyUnit.createInCompany.user(id.toLong).save
      } else {
        if (ac(0).status == 1) {
          ac(0).status (0).save;
        } else {
          ac(0).status (1).save;
        }
      }
      S.redirectTo("/manager/support")
      JInt(1)
    }
    case "security" :: "login_face" :: Nil Post (r) => {
      val json = parse(new String(r.body.get))
      val loginDto = json.extract[LoginFaceDto]
      val users = User.findByFacebook(loginDto.facebookId, loginDto.facebookAccessToken).filter( (u) =>{
        u.id.is == loginDto.id
      })
      if (!users.isEmpty) {
        if (users(0).userStatus != 1) {
          JsObj(("error", true), ("message", "Usuário inativo, se necessário entre em contato com o administrador!"));
        } else {
          AuthUtil << users(0)
          User.beginning;
          val companys = users.map((u) => {
            JsObj(("name", u.company.obj.get.name.is), ("logo", u.company.obj.get.thumb_web), ("id", u.company.is))
          })
          JsObj(("success", true), ("goTo", User.goTo), ("companys", JsArray(companys)));
        }
      } else {
        JsObj(("error", true), ("message", "Usuário ou senha inválida!"));
      }
   }
    case "security" :: "login_email" :: Nil Post (r) => {
      val json = parse(new String(r.body.get))
      val loginDto = json.extract[LoginEmailDto]
      val loginStatus: LoginStatus = if (loginDto.hasCompany) {
        User.loginEmail(loginDto.email, loginDto.password, loginDto.company.toLong)
      } else {
        User.loginEmail(loginDto.email, loginDto.password)
      }
      if (loginStatus.status) {
        AuthUtil << loginStatus.user
        User.beginning;
        val companys = loginStatus.users.map((u) => {
          JsObj(("name", u.company.obj.get.name.is), ("logo", u.company.obj.get.thumb_web), ("id", u.company.is))
        })
        JsObj(("success", true), ("goTo", User.goTo), ("companys", JsArray(companys)));
      } else {
        if (loginStatus.msg != "") {
            JsObj(("error", true), ("message", loginStatus.msg));
        } else {
            JsObj(("error", true), ("message", "Usuário e/ou senha inválidos!"));
        }
      }
    }
    case "security" :: "facebook_register_to_user" :: Nil Post _ =>{
      def facebookId = S.param("facebookId") openOr ""
      def facebookAccessToken = S.param("facebookAccessToken") openOr ""
      def facebookUsername = S.param("facebookUsername") openOr ""
      AuthUtil.user.facebookId(facebookId).facebookAccessToken(facebookAccessToken).facebookUsername(facebookUsername).save
      JInt(1)
    }
  }
}

case class LoginFaceDto(company: String, id: Long, facebookId: String, facebookAccessToken:String )

case class LoginEmailDto(email: String, password: String, company: String) {
  def hasCompany = company != ""
}

case class RememberDto(email: String, company: String) {
}