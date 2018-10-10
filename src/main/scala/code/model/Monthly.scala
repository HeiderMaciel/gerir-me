package code
package model

import net.liftweb._
import mapper._
import http._
import SHtml._
import util._
import code.util._
import _root_.java.math.MathContext;
import net.liftweb.common.{ Box, Full, Empty }
import java.util.Date
import org.joda.time.Days
import org.joda.time.DateTime

class Monthly extends Audited[Monthly] with LongKeyedMapper[Monthly] 
  with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
	with CompanyIdable[Monthly] with ActiveInactivable[Monthly] 
  with MultiEntityRelated {
  def getSingleton = Monthly
  object description extends MappedPoliteString(this, 255)
  object transationId extends MappedPoliteString(this, 255) //PagueSeguro
  // varia aqui com account_payable e transformar em id obj
  object treatment extends MappedLongForeignKey(this,Treatment) 
  object company_customer extends MappedLongForeignKey(this, Company) // só para a vilarika
  object business_pattern extends MappedLongForeignKey(this, Customer) 
  object paymentDate extends EbMappedDate(this) with LifecycleCallbacks { 
      override def beforeSave() {
          super.beforeSave;
          if ((paid.is) && (this.get == Empty || this.get == null)) {
            this.set(dateExpiration.is)
          }
      } 
    }
  object efetiveDate extends EbMappedDate(this)
  object originalDate extends EbMappedDate(this) with LifecycleCallbacks { 
      override def beforeSave() {
          super.beforeSave;
          if (this.get == Empty || this.get == null) {
            this.set(dateExpiration.is)
          }
      } 
  }  
  object dateExpiration extends EbMappedDate(this)
  object paid extends MappedBoolean(this)  with LifecycleCallbacks { 
      override def beforeSave() {
          super.beforeSave;
          if ((!paid.is) && (paymentDate.get != Empty && paymentDate.get != null)) {
            this.set(true)
          }
      } 
    } 
  object value extends MappedCurrency(this)
  object paidValue extends MappedCurrency(this)
  object increseValue extends MappedCurrency(this)
  object liquidValue extends MappedCurrency(this) // com o desconto da tarifa do banco
  object obs extends MappedString(this, 400)
  object barCode extends MappedString(this, 100) with LifecycleCallbacks { 
      override def beforeSave() {
          super.beforeSave;
          if (!isNew) {
            this.set(_barCode)
          }
      } 
  }
  object editableLine extends MappedString(this, 100) with LifecycleCallbacks { 
      override def beforeSave() {
          super.beforeSave;
          if (!isNew) {
            this.set(_editableLine)
          }
      } 
  }
  override def delete_! = {
    super.delete_!
  }

  def company_customerName = company_customer.obj match {
      case Full(t) => t.name.is
      case _ => ""
  }

  def bpName = business_pattern.obj match {
      case Full(t) => t.name.is
      case _ => ""
  }

  def barCode1 = {
    val  bank = "001";
    var  strAux = bank + "9"
    strAux
  }

  def barCode2 = {
    // Calcula-se o número de dias corridos entre a 
    // data base (“Fixada” em 07/10/1997) e a do vencimento desejado
    val   firstdate  = new DateTime("1997-10-07").toDateMidnight()
    // rigel 11/10/2017 - concatenei o  + "T03:00:00" após o dtformat
    // pq estava dando este erro na semana que atecedeu p horário de verão
    // Cannot parse "2017-10-15": Illegal instant due to time zone offset transition (America/Sao_Paulo)
    val   seconddate = new DateTime(Project.dtformat(dateExpiration,"yyyy-MM-dd") + "T03:00:00").toDateMidnight() 
    val   factor = Days.daysBetween(firstdate, seconddate).getDays();
    val   valor = BusinessRulesUtil.clearString (("%.2f".format (value.toFloat)))  
    
    if (factor > 9999) {
        // após 9999 o fator volta para 1000      
        // 9999 21/02/2025
        // 1000 22/02/2025*
        // 1001 23/02/2025
        (factor - 9000).toString + BusinessRulesUtil.zerosLimit(valor,10)
      } else {
        factor.toString + BusinessRulesUtil.zerosLimit(valor,10)
      }
  }

  def barCode3 = {
    val lenconvenio = 7;
    val  convenio = BusinessRulesUtil.limitSpaces ("2863040",7) // novo - 2550720 antigo
    val   complemento = if (lenconvenio == 7) {
      "000000"
    } else {
      ""
    }
    complemento + convenio + BusinessRulesUtil.zerosLimit(idForCompany.toString,10) + "17" // carteira nova
  }

  def _barCode = {
    barCode1 + barCodeDv + barCode2 + barCode3
  }

  def barCodeDv = {
    BusinessRulesUtil.dv_modulo_11 (barCode1 + barCode2 + barCode3).toString
  }

  def _editableLine = {
    barCode1 + barCode3.slice (0,1) + "." + barCode3.slice (1,5) +
    BusinessRulesUtil.dv_modulo_10 (barCode1 + barCode3.slice (0,1) + barCode3.slice (1,5)) +
    " " +
    _barCode.slice (24,29) + "." + _barCode.slice (29,34) +
    BusinessRulesUtil.dv_modulo_10 (_barCode.slice (24,29) + _barCode.slice (29,34)) +
    " " +
    _barCode.slice (34,39) + "." + _barCode.slice (39,44) +
    BusinessRulesUtil.dv_modulo_10 (_barCode.slice (34,39) + _barCode.slice (39,44)) +
    " " + barCodeDv + " " +
    barCode2
  }

  def toRemessa (sequencial:Int, banknumber: String, cotpinsc:String, coinsc:String) = {
     // val  bank = "001";
     val  valor = BusinessRulesUtil.clearString (("%.2f".format (value.toFloat)));
     var strXml:String = "" +
     banknumber + 
     "0001" + // lote
     "3" + // tipo registro
     BusinessRulesUtil.zerosLimit (sequencial.toString,5) + 
     "J" + "0" + "00" + 
     banknumber + "9" + 
     barCodeDv + 
     BusinessRulesUtil.zerosLimit(valor,14) + barCode3 + 
     BusinessRulesUtil.limitSpaces (AuthUtil.company.search_name.toUpperCase,30) +
     Project.dtformat(dateExpiration, "ddMMyyyy") + 
     BusinessRulesUtil.zerosLimit(valor,15) +
     BusinessRulesUtil.zerosLimit("0",15) + // desconto 
     BusinessRulesUtil.zerosLimit("0",15) + // mora
     BusinessRulesUtil.zerosLimit("0",8) + // dt pagto
     BusinessRulesUtil.zerosLimit("0",15) + // valor pagto
     "000000000000000" + //quantidade da moeda
     BusinessRulesUtil.limitSpaces (description,20) + //descricao
     BusinessRulesUtil.limitSpaces ("",20) + //doc atrib banco
     "09" + "      " + "0000000000" + "\r\n" // + 
     // _barCode + "\n" + _editableLine + " " + company + "\n" 
      strXml
  }

  def toRemessaJ52 (sequencial:Int, banknumber:String, cotpinsc:String, coinsc:String) = {
     // val  bank = "001";
     val bc = Customer.findByKey (this.business_pattern.obj.get.id).get
     val  tpinsc = if (bc.document_company != "") {
          "2" // cnpj
      } else {
          "1" // cpf
      }
     val  insc = if (bc.document_company != "") {
          BusinessRulesUtil.zerosLimit (BusinessRulesUtil.clearString(bc.document_company),14); // cnpj 
      } else if (bc.document != "") {
          BusinessRulesUtil.zerosLimit (BusinessRulesUtil.clearString(bc.document),14); // cpf
      } else {
          // cpf rigel 
          "00055118593620"        
      }
     var strXml:String = banknumber + "0001" + // lote
     "3" + // tipo registro
     BusinessRulesUtil.zerosLimit (sequencial.toString,5) + 
     "J" + " " + "00" + // inclusao
     "52" +
     tpinsc + "0" + insc + // cliente
     BusinessRulesUtil.limitSpaces (bc.search_name.toUpperCase,40) + 
     cotpinsc + "0" + coinsc + // company
     BusinessRulesUtil.limitSpaces (AuthUtil.company.search_name.toUpperCase,40) +
     tpinsc + "0" + insc + // cliente
     BusinessRulesUtil.limitSpaces (bc.search_name.toUpperCase,40) + 
     BusinessRulesUtil.limitSpaces ("",53) + "\r\n" // 
     strXml
  }

  //def  bank = "001";
 def toRemessaP (sequencial:Int, banknumber: String, cotpinsc:String, 
      coinsc:String, agencia:String , dvagencia:String , 
      conta:String , dvconta:String) = {
     // val  bank = "001";
     val bc = Customer.findByKey (this.business_pattern.obj.get.id).get

     val  valor = BusinessRulesUtil.clearString (("%.2f".format (value.toFloat)));
     var strXml:String = "" +
     banknumber + 
     "0001" + // lote
     "3" + // tipo registro
     BusinessRulesUtil.zerosLimit (sequencial.toString,5) + 
     "P" + 
     " " + 
     "01" + // entrada de título
     agencia + dvagencia + conta + dvconta + " " +
     BusinessRulesUtil.zerosLimit(idForCompany.toString,10) +
     "01" + // parcela
     "01" + // odalidade
     "4" + // a4 sem envelopamento
     "     " +
     "1" + // carteira
     "0" + // forma cadastro
     " " + // tipo documento 
     "2" + // beneficiario emite
     "2" + // beneficiario distribui
     BusinessRulesUtil.zerosLimit(idForCompany.toString,15) +
     Project.dtformat(dateExpiration, "ddMMyyyy") + 
     BusinessRulesUtil.zerosLimit(valor,15) +
     "00000" + // agencia cobrança
     " " + // dv agencia
     "04" + // duplicata de serviço
     "A" + // Aceite 
     Project.dtformat(createdAt, "ddMMyyyy") + // emissão
     "2" + // juros taxa mensal
     Project.dtformat(dateExpiration, "ddMMyyyy") + 
     "000000000000200" + // 2% ao mês
     "0" + // sem desconto
     Project.dtformat(dateExpiration, "ddMMyyyy") + // data desconto
     "000000000000000" + // % desconto
     "000000000000000" + // iof 
     "000000000000000" + // abatemento 
     // 25 posicoes ideinfica titulo na empresa
     BusinessRulesUtil.zerosLimit(idForCompany.toString,6) + 
     " " +
     BusinessRulesUtil.limitSpaces (bc.search_name.toUpperCase,18) + 
     "3" + // nao protestar
     "00" + // prazo protesto
     "0" + // código baixa
     "   " + // prazo baixa 
     "09" + // real 
     BusinessRulesUtil.zerosLimit("0",10) + // contrato
     " \r\n" 
     strXml
  }

  def toRemessaQ (sequencial:Int, banknumber:String, cotpinsc:String, coinsc:String) = {
     // val  bank = "001";
     val bc = Customer.findByKey (this.business_pattern.obj.get.id).get
     val  tpinsc = if (bc.document_company != "") {
          "2" // cnpj
      } else {
          "1" // cpf
      }
     val  insc = if (bc.document_company != "") {
          BusinessRulesUtil.zerosLimit (BusinessRulesUtil.clearString(bc.document_company),14); // cnpj 
      } else if (bc.document != "") {
          BusinessRulesUtil.zerosLimit (BusinessRulesUtil.clearString(bc.document),14); // cpf
      } else {
          // cpf rigel 
          "00055118593620"        
      }
     var strXml:String = "" + 
     banknumber + 
     "0001" + // lote
     "3" + // tipo registro
     BusinessRulesUtil.zerosLimit (sequencial.toString,5) + 
     "Q" + 
     " " + 
     "01" + // entrada de título
     tpinsc + "0" + insc + // cliente
     BusinessRulesUtil.limitSpaces (bc.search_name.toUpperCase,40) + 
     BusinessRulesUtil.limitSpaces (BusinessRulesUtil.clearString(bc.street).toUpperCase,40) + 
     BusinessRulesUtil.limitSpaces (BusinessRulesUtil.clearString(bc.district).toUpperCase,15) + 
     BusinessRulesUtil.limitSpaces (BusinessRulesUtil.clearString (bc.postal_code).toUpperCase,8) + 
     BusinessRulesUtil.limitSpaces (BusinessRulesUtil.clearString(bc.cityName).toUpperCase,15) + 
     BusinessRulesUtil.limitSpaces (bc.stateShortName.toUpperCase,2) + 
     tpinsc + "0" + insc + // cliente
     BusinessRulesUtil.limitSpaces (bc.search_name.toUpperCase,40) + 
     "000" +
     BusinessRulesUtil.limitSpaces (" ",20) + 
     "        \r\n";
     strXml
  }


}

object Monthly extends Monthly with LongKeyedMapperPerCompany[Monthly] with OnlyCurrentCompany[Monthly] with OnlyActive[Monthly] {

  lazy val SQL_REPORT = """
    SELECT mo.id, mo.idforcompany, mo.description, 
     mo.originaldate, 
     mo.dateexpiration, mo.value, 
     mo.paid, mo.paymentdate, mo.id, co.name,
     mo.obs 
     FROM monthly mo
     inner join company co on co.id = mo.company_customer
     where mo.company_customer=? and mo.status = 1 order by mo.dateexpiration desc, mo.id desc
  """
  def createMonthly(company: Company, company_customer: Company, value:Double, dateToPayment: Date, description: String) = {
    val monthly = Monthly.create
      monthly.company(company)
      .company_customer(company_customer)
      .value(value)
      .description(description)
      .dateExpiration(dateToPayment)
      .originalDate(dateToPayment) // rigel 29/04/2017 
      .obs("")
      .paid(false)
      .createdBy(6)
      .updatedBy(6)
      .save
    monthly
  }

  // pega as mensalidades vencendo hoje e se for segunda feira
  // pega as que venceram sábado e domingo tb e envia novamete
  def findAllToday = findAll(By(Monthly.paid, false), 
      By(Monthly.status, Monthly.STATUS_OK), 
      BySql("""date (dateexpiration) = date(now()) 
        or (extract (DOW from now()) = 1 -- segunda feira
        and (dateexpiration between date(now())-2 and date(now())))""",
        IHaveValidatedThisSQL("1 = 1","")))


  def usersToNotify(company:Company) = {
      User.findAll(By(User.company, company),
      BySql[User](" userstatus = 1 and (groupPermission LIKE '%,1'  or  groupPermission LIKE '%,1,%'  or groupPermission LIKE '1' " +
         " or groupPermission LIKE '%,8'  or  groupPermission LIKE '%,8,%'  or groupPermission LIKE '8' " +
         " or groupPermission LIKE '%,1000'  or  groupPermission LIKE '%,1000,%'  or groupPermission LIKE '1000') ",IHaveValidatedThisSQL("","") ), 
      NotBy(User.email,""))
  }


    def toRemessa240 (start:Date, end:Date, account:Account) {
       val ac = AccountCompanyUnit.findAll (
        By (AccountCompanyUnit.account, account),
        By (AccountCompanyUnit.unit, AuthUtil.unit)) (0)

       def  bu = Customer.findByKey (AuthUtil.unit.partner).get
 
       val now  = new Date()
        //val nowTime  = now.getTime()

       def  cotpinsc = if (ac.document_company != "") {
          "2" // cnpj
       } else {
          "1" // cpf
       }
       def  coinsc = if (ac.document_company != "") {
          BusinessRulesUtil.zerosLimit (BusinessRulesUtil.clearString(ac.document_company),14); // cnpj 
       } else {
          BusinessRulesUtil.zerosLimit (BusinessRulesUtil.clearString(ac.document),14); // cpf
       }

       //val  convenio = ("00" + "2863040" + "0126" + "       ") // novo - 2550720 antigo
       //val  convenio = ("00" + BusinessRulesUtil.zerosLimit (
       // BusinessRulesUtil.clearString(ac.agreement),7) + "0126" + "       ")
       val convenio = BusinessRulesUtil.limitSpaces (" ",20);
       //val  agencia = BusinessRulesUtil.zerosLimit ("0591",5);
       val  agencia = BusinessRulesUtil.zerosLimit (
        BusinessRulesUtil.clearString(ac.agency),5);
       //val  dvagencia = "6"
       val  dvagencia = BusinessRulesUtil.zerosLimit (ac.agencyVd,1);
       //val  conta = BusinessRulesUtil.zerosLimit ("17355",12)
       //val  dvconta = "X"
       val  conta = BusinessRulesUtil.zerosLimit (
        BusinessRulesUtil.clearString(ac.accountStr),12);
       val  dvconta = BusinessRulesUtil.zerosLimit (ac.accountVd,1);
       val  bk = Bank.findByKey (ac.bank).get;
       val  banknumber = bk.banknumber;
       val  bankname = BusinessRulesUtil.limitSpaces (bk.short_name.toUpperCase,30);
       val  layout = if (ac.bank == 1) {
          "040" // bb
        } else {
          "081" //sicoob
        }
       val  densidade = "00000"
       val  msg = BusinessRulesUtil.limitSpaces ("mensagem",40) // novo

       var strXml =
          // header de arquivo
          """""" + banknumber + 
          """0000""" + // lote 
          "0" + // registro 
          """         """ + // uso febraban
          cotpinsc + coinsc + 
          // BB convenio + 
          BusinessRulesUtil.limitSpaces(" ",20) + 
          agencia + dvagencia + conta + dvconta + "0" +
          BusinessRulesUtil.limitSpaces (AuthUtil.company.search_name.toUpperCase,30) + bankname + 
          "          " + "1" +
          Project.dtformat(now, "ddMMyyyy") + 
          Project.dtformat(now, "HHmmss") + 
          "000001" + 
          layout + 
          densidade + 
          "                    " + // uso febraban
          BusinessRulesUtil.limitSpaces ("mensalidade vilarika",20) + 
          "           " + "   " + "000" + "00" + "0000000000\r\n" +
          //
          // ******************************************************
          // header de lote
          // ******************************************************
          //
          banknumber + 
          "0001" + // lote
          "1" + // tipo registro
          //"C" + // minha documentacao estava R o suporte BB mandou por C para crédito 
          "R" +
          // bb "98" + "30" + "030" + " " + 
          "01" + // tipo serviço
          "  " + // febraban
          "040" + // layout lote
          " " + 
          cotpinsc + 
          "0" + // no sicoob o nro é com 15 neste reg
          coinsc + 
          // BB convenio + 
          BusinessRulesUtil.limitSpaces(" ",20) + 
          agencia + dvagencia + conta + dvconta + 
          // BB "0" +
          " " + //dv agencia conta 
          BusinessRulesUtil.limitSpaces (AuthUtil.company.search_name.toUpperCase,30) + 
          msg + // de 40 pos
          BusinessRulesUtil.limitSpaces(" ",40) + // mensagem 2
          /* BB
          BusinessRulesUtil.limitSpaces(bu.street.toString,30) + 
          BusinessRulesUtil.zerosLimit(bu.number.toString,5) +  
          BusinessRulesUtil.limitSpaces(bu.complement.toString,15) + 
          BusinessRulesUtil.limitSpaces(bu.cityName.toString,20) + 
          BusinessRulesUtil.zerosLimit(bu.postal_code.toString,8) + 
          BusinessRulesUtil.limitSpaces(bu.stateShortName.toString,2) + 
          "        " + "0000000000\n"
          */
          BusinessRulesUtil.limitSpaces(" ",8) + // remessa
          BusinessRulesUtil.limitSpaces(" ",8) + // data gravacao
          BusinessRulesUtil.zerosLimit("0",8) + // data crédito
          BusinessRulesUtil.limitSpaces(" ",33) + "\r\n"

       var sequencial = 0;
       var somatoria = 0.0;   
       var titulos = 0;
       Monthly.findAll(
        BySql("(dateExpiration between ? and ?)", IHaveValidatedThisSQL("",""), start, end),
        By(Monthly.status, 1),
        By(Monthly.paid, false),
        OrderBy(id, Ascending)).foreach ((mo) => {
          if (Project.isLocalHost && sequencial < 6) {
            println ("vaiii ========= boleto " + mo.description + " ==== " + mo.barCode2 + " ==== " + mo.company.toString);
            sequencial += 1;
            titulos += 1;
            if (ac.bank == 1 /* bb */) {
              strXml += mo.toRemessa (sequencial, banknumber, cotpinsc, coinsc);
            } else {  
              strXml += mo.toRemessaP (sequencial, banknumber, cotpinsc, coinsc, agencia , dvagencia , conta , dvconta);
            }
            somatoria += mo.value
            sequencial += 1;
            // identifica o sacado cliente
            if (ac.bank == 1 /* bb */) {
              strXml += mo.toRemessaJ52 (sequencial, banknumber, cotpinsc, coinsc);
            } else {
              strXml += mo.toRemessaQ (sequencial, banknumber, cotpinsc, coinsc);
            }
           if (mo.barCode == "") {
            mo.barCode.set("*")
            mo.save
           }
          }
       })

        val  valSum = BusinessRulesUtil.clearString (("%.2f".format (somatoria.toFloat)));
        val  qtdeLote = (sequencial+2).toString;
        //trailer de lote
        strXml += "" + 
        banknumber + 
        "0001" + 
        "5" + "         " + 
        BusinessRulesUtil.zerosLimit(qtdeLote,6)
        if (ac.bank == 1 /* bb */) {
          //BusinessRulesUtil.zerosLimit(valSum,18) + minha documentacao mandava por o total
          strXml += "" +
          BusinessRulesUtil.zerosLimit("",18) + //suporte mandou por zeros 
          "000000000000000000" + "000000" + 
          BusinessRulesUtil.limitSpaces("",165) +
          "0000000000" + "\r\n";
        } else {
          strXml += "" +
          BusinessRulesUtil.zerosLimit(titulos.toString,6) +
          BusinessRulesUtil.zerosLimit(valSum,17) + 
          BusinessRulesUtil.zerosLimit("0",6) + // vinculada
          BusinessRulesUtil.zerosLimit("0",17) + 
          BusinessRulesUtil.zerosLimit("0",6) + // caucionada
          BusinessRulesUtil.zerosLimit("0",17) + 
          BusinessRulesUtil.zerosLimit("0",6) + // descontada
          BusinessRulesUtil.zerosLimit("0",17) + 
          BusinessRulesUtil.limitSpaces(" ",8) +
          BusinessRulesUtil.limitSpaces(" ",117) +
          "\r\n";
        }
        //
        // ******************************************
        //trailer de arquivo
        // ******************************************
        //
        strXml += banknumber + "9999" + "9" + "         " + 
        BusinessRulesUtil.zerosLimit("1",6) + // um lote só
        BusinessRulesUtil.zerosLimit((sequencial + 4).toString,6) + 
        "000000" + 
        BusinessRulesUtil.limitSpaces("",205) +
        "\r\n";

        val filePath = if(Project.isLinuxServer){
          (Props.get("remessa.path") openOr "/tmp/")
        }else{
          "c:\\vilarika\\"
        }
       scala.tools.nsc.io.File(filePath + "remessa_" + AuthUtil.company.id.toString + "_" 
        + banknumber + "_" + Project.dtformat(now, "yyyyMMdd_HHmm") + ".txt").writeAll(strXml)
    }

}