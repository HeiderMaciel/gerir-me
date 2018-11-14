package code
package util

import net.liftweb._ 
import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import mapper._ 
import S._
import code.model._
import scala.xml._
import scala.collection.JavaConverters._

import java.io.{File,FileInputStream}
import java.util.Calendar;
import java.util.TimeZone;
import scala.io.Source._

import java.util.Date


object ContactsUtil extends net.liftweb.common.Logger {
  var sqlInsert = "";
  //var bi = 0;
  var filePath = if(Project.isLinuxServer){
    "/tmp/"
  }else{
    "c:\\vilarika\\"
  }
  def execute(file:File, origin:String, nameasis: Long, generatesql: Long){
    sqlInsert = "";
    val lines = fromFile(file).getLines.toList
    val schema = if (origin.indexOf (".") > 0) {
      ""
    } else {
      "diversos."
    }

println ("vaiiii ======= " + lines(1))
    println ("vaiiii virg " +lines(1).count(_ == ','));

    val separator:String = if (lines(1).count(_ == '.') >
      lines(1).count(_ == ';') && lines(1).count(_ == '.') >
      lines(1).count(_ == ',')) {
        "."
      } else if (lines(1).count(_ == ';') >
      lines(1).count(_ == ',')) {
        ";"
      } else {
        ","
      }
    // o for começa de um para saltar a primeira linha 
    // cabeçalho das colunas - parametrizar
    //val details = 
    println ("vaiiii ======== gerar " + lines.size + " linhas! separador " + separator)

    sqlInsert += "--Drop table " + schema + origin  + ";\n\n"
    sqlInsert += "Create table " + schema + origin  + " ( \n"
    var listCol = lines(0).split(separator);
    var j = 0;
    listCol.foreach((column) => {
      var colAux = column.replaceAll ("\"", "")
      colAux = colAux.replaceAll (" ", "")
      println ("vaiiii ===================== ANTES " + colAux);
      colAux = BusinessRulesUtil.convertChars (colAux)
      println ("vaiiii ===================== APOS " + colAux);
      if (BusinessRulesUtil.clearString(colAux) == "") {
        colAux = "nada" + j
      }
      sqlInsert += BusinessRulesUtil.clearString(colAux) + " varchar (255)"
      j += 1;
      if (j < listCol.length) {
        sqlInsert += ",\n"
      } else {
        sqlInsert += """)
            WITH (
            OIDS=FALSE
            );
            ALTER TABLE """ + schema + origin + 
            " OWNER TO postgres;\n ";
      }
    })  

    sqlInsert += "--Delete from " + schema + origin  + ";\n"

    for(i <- 1 to (lines.size)-1 ) {
      //println ("vaiii ====== " + lines(i) + " === " + i + " serapardor " + separator)
      var line = lines(i);
      factory(i, line, separator, schema + origin, nameasis, generatesql)
    }
    // gerar arquivo aqui
    if (generatesql > 0) {
     scala.tools.nsc.io.File(filePath + origin + ".sql").
     writeAll(sqlInsert);
     //println ("vaiiiii ======= " + sqlInsert)
    } else {
/*    details.map((d) => {

      removeInventory(d.customer.id, d.purchasePrice, 0l, d.price, d.amount, 
        d.product, "venda importada", " sem doc", d.unit, 
        d.iCause, d.today)
      if (d.product.salePrice == 0 && d.amount != 0) {
        d.product.salePrice (d.price / d.amount).save
      }

      })
*/
      // verificar email no bp e ligar
      UtilSqlContacts.updateContacts(AuthUtil.company, origin);
    } 
  }

  def linkContactCustomer (ac:Contact) = {
    if (ac.name != "" || ac.email != "" || ac.phone != "") {
      // procura cliente com o mesmo email que o contato e
      // faz a ligção em caso de sucesso
      val bp = Customer.findAllInCompany (
        By(Customer.status, 1),
        BySql (""" (name <> '' and trim (name) = trim (?)) 
          or (email <> '' and trim (email) = trim (?)) 
          or (phone <> '' and phone = ?)
          or (mobile_phone <> '' and mobile_phone = ?) 
          or (email_alternative <> '' and email_alternative = ?)
          """, IHaveValidatedThisSQL("", ""), ac.name, ac.email, ac.phone,
          ac.phone, ac.phone)          
        )
      if (bp.length > 0) {
        println ("vaiii ============== " + ac.email + " " + ac.phone);
        ac.business_pattern (bp(0).id.is)
        ac.save
      }
    }
  }

  def saveContacts (listCol:Array[String], origin:String, nameasis:Long) = {
    val length = 5
    var temp = Array.ofDim[String](length)
    var maxLen = if (5 < listCol.length) {
      5 
    } else {
      listCol.length
    }
    for (i <- 0 to maxLen -1) {
      temp (i) = BusinessRulesUtil.convertChars(listCol (i))
    }  
    if (maxLen < 5) {
      for (i <- maxLen to 4) {
        temp (i) = "";
      }
    }
    var strAux = if (nameasis > 0) {
      //mantem o nome original
      temp(0)
    } else {
      BusinessRulesUtil.toCamelCase (BusinessRulesUtil.clearString(temp(0)))
    }
    val ac = Contact.createInCompany
    .name (strAux)
    .email ((temp(1)).toLowerCase)
    .phone (temp(2))
    .birthday (BusinessRulesUtil.mgrDate (temp(3)))
    .date1 (BusinessRulesUtil.mgrDate (temp(3)))
    .obs (temp(4))
    .origin (origin)
    try {
      ac.save
      linkContactCustomer (ac);
    } catch {
      case e:Exception => {
        try {
          ac.obs ((ac.email + " " + ac.obs).trim);
          ac.email ("")
          ac.save
          linkContactCustomer (ac);
        } catch {
          case e:Exception => {
            // gerar log obj e mostrar 
            println ("vaiiiii ====== " + e.getMessage + " segunda tentativa ")
            //JString(e.getMessage)
          }
        }
      }
    }
  }

/*
  def removeInventory(bp:Long, purchaseprice:Double, treatmentdetail:Long, price:Double, amount:Float, product:Product, obs:String, invoice:String, unit:CompanyUnit, inventorycause:InventoryCause, efetivedate:Date) {
    //AuthUtil.company.inventoryCauseSale.obj.get
    // remove(amount) item product company(AuthUtil.company) obs(obs) invoice(invoice) cause(AuthUtil.company.inventoryCauseSale.obj.get) efetiveDate(new Date()) from unit
    remove(amount) item product company(AuthUtil.company) purchasePrice (purchaseprice) treatment_detail (treatmentdetail) totalSalePrice (price) obs(obs) business_pattern (bp) invoice(invoice) cause(inventorycause) efetiveDate(efetivedate) from unit
  }
*/

  def factory(i:Long, lines:String, separator:String, origin:String, nameasis:Long, generatesql:Long):DetailContacts ={
    var listCol = lines.split(separator);
    var j = 0;
    var colAux = "";
    if (listCol.length < 1) {
      if (i < 5) {
        listCol = lines.split(';');
        println ("vaiiii --------- cols " + listCol.length + " line " + lines)
      }
    }
    if (listCol.length > 0) {
      sqlInsert += "\ninsert into " + origin + " values (";
    }
    if (generatesql > 0) {
      listCol.foreach((column) => {
        colAux = column.replaceAll ("\"","");
        colAux = colAux.replaceAll ("'"," ");
        sqlInsert += "'" + colAux + "'";
        j += 1;
        if (j < listCol.length) {
          sqlInsert += ","
        } else {
          sqlInsert += ");"
        }
  //      println ("vaiii ====== " + column);
      });
      // poderia ser um parm mostrar linha caso cancele 
      if (i % 500 == 0) {
        println ("vaiiiii ====== i " + i + " " + sqlInsert.length + " lin " + lines);
      } 
      /* excluir
      if (sqlInsert.length > 700000) {
        println ("vaiiiii ====== vai gerar um arq " + bi + " " + sqlInsert.length + " lin " + lines);
        scala.tools.nsc.io.File(filePath + origin + bi + ".sql").
        writeAll(sqlInsert);
        sqlInsert = "";
        bi += 1;
      }
      */
    } else {
      saveContacts (listCol, origin, nameasis);
    }

    DetailContacts("","","","")
/*
            lines(i).substring(0, index),
            lines(i).substring(index + 1, index_pr),
            lines(i).substring(index_pr +1, index_qt-1),
            lines(i).substring(index_ul+3, ((lines(i).length)))
          )
*/
  }
}


case class DetailContacts(
                  private val _customer:String,
                  private val _product:String, 
                  private val _qtty:String, 
                  private val _value:String){

/*
  val today = Project.date_format_db.parse(Project.date_format_db.format(new Date()));
  def iCause = AuthUtil.company.inventoryCauseSale.obj.get
  def unit = AuthUtil.unit
  def purchasePrice = 0.0

  def price = BusinessRulesUtil.clearStrNum (_value).toDouble;

  def amount:Float = BusinessRulesUtil.clearStrNum (_qtty).toFloat;

  def product = Product.findByName (_product);

  def customer = Customer.findByName (_customer);
*/

}

object UtilSqlContacts {
  val SQL_UPDATE_PHONE = """
  update contact set phone = fu_mgr_phone (phone, '%s') where company = ?
  and origin = ?;
  """
  def updateContacts(company:Company, origin:String) {
    DB.runUpdate(SQL_UPDATE_PHONE.format(AuthUtil.unit.defaultDDD), company.id.is::origin::Nil)
    Contact.findAllInCompany (
      By (Contact.origin, origin)).map ((ac) => {
        ContactsUtil.linkContactCustomer (ac);
    })
  }

}


