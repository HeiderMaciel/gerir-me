package code
package model

import code.util._

import net.liftweb._
import mapper._
import http._
import SHtml._
import util._
import _root_.java.math.MathContext;
import scalendar._
import Month._
import Day._
import net.liftweb.common.{ Box, Full, Empty }

import java.util.Date
import java.util.Calendar


class AccountCategory extends Audited[AccountCategory] with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy with NameSearchble[AccountCategory] with ActiveInactivable[AccountCategory] {
  def getSingleton = AccountCategory
  override def updateShortName = false
  object obs extends MappedPoliteString(this, 255)
  object color extends MappedPoliteString(this, 55)
  object userAssociated extends MappedBoolean(this)
  object parent_? extends MappedBoolean(this) {
    override def dbColumnName = "isparent"
  }
  object orderInReport extends MappedInt(this)
  object typeMovement extends MappedInt(this) {
    override def dbIndexed_? = true
    override def defaultValue = AccountPayable.OUT
  }
  object managerLevel extends MappedInt(this) {
    override def defaultValue = 0;
  }
  object treeLevel extends MappedInt(this) with LifecycleCallbacks {
      override def defaultValue = 0;
      override def beforeSave() {
          super.beforeSave;
          this.set(_treeLevel)
      }
  }

  object treeLevelstr extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def defaultValue = "";
      override def beforeSave() {
          super.beforeSave;
          this.set(_treeLevelstr)
      }
  }

  object fullName extends MappedPoliteString(this,255) with LifecycleCallbacks {
      override def beforeSave() {
          super.beforeSave;
          this.set(BusinessRulesUtil.toCamelCase(_fullName))
      }
  }
  object parentAccount extends MappedLongForeignKey(this, AccountCategory) {
    override def dbIndexed_? = true
  }
  object minTreeNode extends MappedLong(this) {
    override def defaultValue = 1
  }
  object maxTreeNode extends MappedLong(this) {
    override def defaultValue = 2
  }
  object balanceControl_? extends MappedBoolean(this) {
    override def defaultValue = false
    override def dbColumnName = "balanceControl"
  }

  private def _treeLevel : Int = {
      var level = 0
      var i = 0;
      if (fullName != "") {
        while (fullName.indexOf ("->",i) != -1) {
          level = level + 1
          i = fullName.indexOf ("->",i) + 1
        }
      } else {
        level = 0;
      }
      level
 }

  def _treeLevelstr : String = {
      var level = ""
      var i = 0;
      if (fullName != "") {
        while (fullName.indexOf ("->",i) != -1) {
          level = level + ". "
          i = fullName.indexOf ("->",i) + 1
        }
      } else {
        level = "";
      }
      level
 }

  def nameStatus =  {
      if (this.isInactive) {
        // se inativo é pra deixar o nome mesmo
        name.is + " (* inativa)"
      } else {
        short_name.is
      }
  }
  private def _fullName : String =  {
      def parrentNames = parentAccount.obj match {
        case Full(parent) => parent._fullName +" -> "
        case _ => "";
      }
      parrentNames+name.is
  }

  def hasParent = {
    // diferente dos outros para pegar campos "cheios" 
    // cuja referencia foi deletada
      if (AccountCategory.count (
        By (AccountCategory.id, this.parentAccount)) < 1) {
        false
      } else {
        true
      }        
  }
  
  def hasChilds = directyChilds.size > 0

  private def saveWithoutUpdateTree = {
    if (this.testIfDuplicatedName (this.company, this.id, this.name)) {
      this.name.set (this.name + "  1")
    }
    super.save
  }
  override def save = {
    AccountCategory.validateAccount(this)
    if(this.isInactive){
      this.maxTreeNode(-1);
      this.minTreeNode(-1);
    }
    val r = super.save
    AccountCategory.updateTreeNodes
    r
  }
  lazy val directyChilds = AccountCategory.findAllInCompany(By(parentAccount, this.id.is))
  def accountsCount: Long = AccountPayable.count(By(AccountPayable.category, this))
  def hasAccount: Boolean = accountsCount > 0l
  override def delete_! = {
    if (hasAccount) {
      throw new RuntimeException("Existem lançamentos para essa categoria! ")
    }
    if(PaymentType.count(By(PaymentType.defaltCategory,this.id)) > 0){
        throw new RuntimeException("Existe forma de pagamento faturada nesta categoria! ")
    }
    if(PaymentType.count(By(PaymentType.defaltDicountCategory,this.id)) > 0){
        throw new RuntimeException("Existe forma de pagamento faturada como desconto nesta categoria! ")
    }
    super.delete_!;
  }

  /*
  Se for fazer outro levar para o nome searchable trai
  tem accountcategory e productmapper já
  */
  def reorgCategory = {
    val category = AccountCategory.findAllInCompany (OrderBy (maxTreeNode, Descending))
    var i = 1;
    category.foreach((c)=>{
      println ("vaiii ================ category full name === " + c.fullName)
      if (c.testIfDuplicatedName (c.company, c.id, c.name)) {
        c.name (c.name + " " + i.toString)
        i = i + 1;
      }
      if (!c.hasParent) {
          // garante que nao tem pai para o caso de exclusao nas 
          // migrações
          c.parentAccount (0l)
      }
      c.obs (c.obs+" ")
      c.save        
    })
  }
  def balanceControlCategory = {
    val ac = AccountCategory.findAllInCompany (By(balanceControl_?, true))
    if (ac.isEmpty) {
        throw new RuntimeException("Não foi encontrada categoria para consolidação de conta, \npara tanto deve haver pelo menos uma categoria com o parâmetro controle de saldo marcado")
      } else {
        ac(0)
      }
  }
}

object AccountCategory extends AccountCategory with LongKeyedMapperPerCompany[AccountCategory] with OnlyActive[AccountCategory] {
  val SQL_REPORT_GRAPHIC = """
  select ac.name,sum(ap.value) from accountpayable ap
inner join accountcategory ac on(ac.id = ap.category)
where 
ap.toconciliation = false
and ap.company  =?
and ap.typemovement=1
and ap.duedate between ? and ?
group by ac.name;"""
  def updateTreeNodes = {

    def treatTree(accs: List[AccountCategory], accValue: Long): Long = {
      if (accs.isEmpty) accValue
      else {

        val account = accs.head
        account.minTreeNode(accValue)
        account.maxTreeNode(treatTree(account.directyChilds, accValue + 1))
        account.saveWithoutUpdateTree
        treatTree(accs.tail, account.maxTreeNode.is + 1)
      }
    }
    treatTree(findAllInCompanyWithInactive(OrderBy(createdAt, Ascending), BySql("parentAccount IS NULL", IHaveValidatedThisSQL("", ""))), 1)
  }
  type Validation = (AccountCategory) => Unit

  /**
   * Validate has child
   */
  def vhc = (c: AccountCategory) => {
    if (!c.parent_?.is && c.hasChilds)
      throw new RuntimeException("Contas que possuem filhas não podem ser desmarcadas como pai!")
  }
  /**
   * Validate parent parent
   */
  def vpp = (c: AccountCategory) => {
    c.parentAccount.obj match {
      case Full(pa) => if (pa.saved_? && !pa.parent_?.is)
        throw new RuntimeException("Conta pai não aceita conta filha!")
      case _ =>
    }
  }

  /**
   * Validate parent not this
   */
  def vpt = (c: AccountCategory) => {
    c.parentAccount.obj match {
      case Full(pa) =>
        if (pa.saved_? && pa.id.is == c.id.is)
          throw new RuntimeException("Conta não pode ser pai dela mesma! " + c.name)
      case _ =>
    }
  }

  /**
   * Validate parent not child of this
   */
  def vpncot = (c: AccountCategory) => {
    c.parentAccount.obj match {
      case Full(pa) => {
        if (pa.saved_? && (pa.minTreeNode.is > c.minTreeNode.is && pa.minTreeNode.is < c.maxTreeNode.is))
          throw new RuntimeException("Conta pai não pode ser filha de uma conta filha da conta atual " + c.name)
      }
      case _ =>
    }
  }

  /**
   * Validate parent desabled when has childs enabled
   */
  def vphce = (c: AccountCategory) => {
    if(c.isInactive && c.hasChilds){
      throw new RuntimeException("Não é possível inativar conta que possui filhos ativos")
    }
  }

  def listValidation: List[Validation] = vpp :: vpt :: vpncot :: vhc :: vphce :: Nil

  def validateAccount(acc: AccountCategory) {
    listValidation.foreach(_(acc))
  }
}
