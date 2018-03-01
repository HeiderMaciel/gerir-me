package code
package util

import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import S._
import code.model._

object AuthUtil {
  val EmptyString = ""

  private object curUser extends SessionVar[Box[User]](Empty) {
    override lazy val __nameSalt = Helpers.nextFuncName
  }

  private object curUnit extends SessionVar[Box[CompanyUnit]](Empty) {
    override lazy val __nameSalt = Helpers.nextFuncName
  }
  def currentUserId: Box[User] = curUser.is

  def ? = {
    currentUserId.isDefined
  }

 def <<(companyUnit: CompanyUnit) = {
    curUnit(Full(companyUnit))
  }
  

  def <<(userP: User) = {
    ExtSession.userDidLogin(userP)
    curUser(Full(userP))
  }

  def <<(userPi: Customer) = {
    val userP = User.findByKey (userPi.id).get
    ExtSession.userDidLogin(userP)
    curUser(Full(userP))
  }

  def >> = {
    curUser.remove()
    S.session.foreach(_.destroySession())
    ExtSession.userDidLogout(_)
  }

  def company :Company = {
      val user = curUser.is openOr User.create
      user.company.obj openOr Company.create
  }

  def checkSuperAdmin {
    if(!AuthUtil.user.isSuperAdmin){
      throw new SecurityException("Acesso negado")
    }
  }

  def checkSupportAdmin {
    if(!AuthUtil.user.isSupportAdmin){
      throw new SecurityException("Acesso negado")
    }
  }

  def unit :CompanyUnit = {
      curUnit.is match {
        case Full(unit) => unit
        case _ => user.unit.obj openOr CompanyUnit.createInCompany
      }
  }  
  def user = curUser.is openOr null;
  def userId = curUser.is match {
    case Full(u) => u.id.is
    case _ => 0.toLong
  }
  
  def name :String = {
      curUser.is match {
        case Full(u) => u.userName
        case _ => EmptyString
      }
  }

  def email :String = {
      curUser.is match {
        case Full(u) => user.email
        case _ => EmptyString
      }
  }
  
  def ! ={
    ! ?
  }
}
class SecurityException(e:String) extends RuntimeException(e)