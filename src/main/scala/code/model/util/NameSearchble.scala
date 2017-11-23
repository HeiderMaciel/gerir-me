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

trait NameSearchble [self <: net.liftweb.mapper.Mapper[self]]{
  self: BaseMapper with NameSearchble [self] with Mapper[self] with PerCompany with IdPK =>
  lazy val NOT_ALLOWED_NAME_CHARAPTERS = "(" :: ")" :: "/" :: "*" :: "-" :: "." :: "," :: "|" :: "+" :: "?" :: ":" :: "0" :: "1" :: "2" :: "3" :: "4" :: "5" :: "6" :: "7" :: "8"  ::  "9" ::  Nil

  protected def updateShortName = true

  protected def isPersonalName_? = false
  protected def isAnimalName_? = false // se true pode duplicar
  protected def allowDuplicated_? = false

  lazy val name: MyName = new MyName(this.asInstanceOf[self])

  lazy val search_name: MySearch = new MySearch(this.asInstanceOf[self])
  
  lazy val short_name: MyShortName = new MyShortName(this.asInstanceOf[self])
  
  protected class MySearch(obj: self) extends MappedPoliteString(obj,255) with LifecycleCallbacks {
    override def beforeSave() {
      super.beforeSave;
      if (fieldOwner.asInstanceOf[NameSearchble[self]].name.is.trim() == "") {
        throw new RuntimeException("Nome não pode ser vazio!")
      }
      this.set(BusinessRulesUtil.clearString(fieldOwner.asInstanceOf[NameSearchble[self]].name.is.trim()))
      if(this.is == BusinessRulesUtil.EMPTY){
        throw new RuntimeException("Nome não pode conter apenas caracteres especiais!")
      }
    } 
    override def dbIndexed_? = true
  }

  protected class MyName(obj: self) extends MappedPoliteString(obj,255) with LifecycleCallbacks {
    override def validations = validateNameMinLength _ ::Nil
    def validateNameMinLength(name : String) = {
      if (name.length < 4) {
        List(FieldError(this, "Nome não pode conter menos de 4 caracteres!"))
        } else {
          List[FieldError]()
        }
    }
    override def beforeCreate(){
      super.beforeCreate;
      if(isPersonalName_?){
        validateDuplicatedName
      }
    }
    override def beforeSave() {
      super.beforeSave;
      this.set (this.is.trim());
      if(this.is == BusinessRulesUtil.EMPTY){
        throw new RuntimeException("Nome não pode ser vazio")
      }
      if(isPersonalName_?){
          if(NOT_ALLOWED_NAME_CHARAPTERS.exists( (c:String) => { this.is.contains(c)})){
            throw new RuntimeException("Existem caracteres especiais no nome da pessoa! "  + this.is + " use o campo de observação!")
          }
          if (this.is.length < 4) {
            throw new RuntimeException("Nome não pode conter menos de 4 caracteres!" + this.is)
          }            
      } else {
          if (this.is.length < 2) {
            throw new RuntimeException("Nome não pode conter menos de 2 caracteres! " + this.is)
          }
          if (!isAnimalName_?) {
            validateDuplicatedName
          }
      }
      this.set(BusinessRulesUtil.toCamelCase(this.is.trim()));
    }
  }
      def testIfDuplicatedName ( company : Long, id1: Long, name1 : String) = {
        if(!allowDuplicated_? && self.getSingleton.count(
          By(self.company, company), 
          Like(self.name, BusinessRulesUtil.toCamelCase(name1)), //self.name.is.trim)), 
          NotBy(self.id, id1) //self.id.is)
          ) > 0){
          true
        } else {
          false
        }
      }

      def validateDuplicatedName = {
        if (testIfDuplicatedName (self.company.is, self.id.is, self.name.is.trim)) {
          throw new RuntimeException("Já existe um registro com esse nome : %s".format(self.name.is))
        }
/*
        if(!allowDuplicated_? && self.getSingleton.count(
          By(self.company, self.company.is), 
          Like(self.name, BusinessRulesUtil.toCamelCase(self.name.is.trim)), 
          NotBy(self.id, self.id.is)) > 0){
          throw new RuntimeException("Já existe um registro com esse nome : %s".format(self.name.is))
        }
*/
      }

      protected class MyShortName(obj: self) extends MappedPoliteString(obj,20) with LifecycleCallbacks {
        override def beforeCreate() {
          super.beforeCreate;
          if(this.get == BusinessRulesUtil.EMPTY) {
            this.set(BusinessRulesUtil.toShortString(fieldOwner.asInstanceOf[NameSearchble[self]].name.is.trim()))
          }
        }
        override def beforeSave() {
          super.beforeSave;
          if(updateShortName || this.is == BusinessRulesUtil.EMPTY){
            this.set(BusinessRulesUtil.toShortString(fieldOwner.asInstanceOf[NameSearchble[self]].name.is.trim()))
          }
        }         
      }
    //

    def asJsToSelect = {
      JsObj(
        ("name",this.name.is),
        ("id",this.id.is)
        )
    }
  }


  trait LogicalDeleteMapper[A <: LongKeyedMapper[A]] extends OnlyCurrentCompany[A] with LongKeyedMapper[A]  {
    self: A with LogicalDelete[A] with PerCompany with MetaMapper[A] => 
    override def findAllInCompany(params: QueryParam[A]*): List[A] = {
      super.findAllInCompany(By(self.deleted_?, false) :: params.toList :_*)
    }

    override def countInCompany(params: QueryParam[A]*): Long = {
      val deleteds = By(self.deleted_?, false);
      super.countInCompany(deleteds :: params.toList :_*)
    }

    def countInCompanyWithDeleteds(params: QueryParam[A]*): Long = {
      super.countInCompany(params.toList :_*)
    }

    def countWithDeleteds(params: QueryParam[A]*): Long = {
      self.count(params.toList :_*)
    }    

    def findAllInCompanyWithDeleteds(params: QueryParam[A]*): List[A] = {
      super.findAllInCompany(params.toList :_*)
    }  

  }

