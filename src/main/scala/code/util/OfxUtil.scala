package code
package util

import net.liftweb._
import mapper._
import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import S._
import code.model._
import scala.xml._
import scala.collection.JavaConverters._

import java.io.{File,FileInputStream}
import net.sf.ofx4j.domain.data.MessageSetType
import net.sf.ofx4j.domain.data.ResponseEnvelope
import net.sf.ofx4j.domain.data.ResponseMessageSet
import net.sf.ofx4j.domain.data.banking.BankStatementResponse
import net.sf.ofx4j.domain.data.banking.BankStatementResponseTransaction
import net.sf.ofx4j.domain.data.banking.BankingResponseMessageSet
import net.sf.ofx4j.domain.data.common.Transaction
import net.sf.ofx4j.domain.data.signon.SignonResponse
import net.sf.ofx4j.io.AggregateUnmarshaller
import net.sf.ofx4j.io.OFXParseException

object OfxUtil {

  // para controlar vários nro(s) de cartão no mesmo ofx
  // pode ocorrer no caso de cancelamento de cartão e 
  // utilização de um novo
  // o primeiro teste da ieda tinha 3
  var alreadyValidated = false;

  def execute(file:File, category:Long, account:Long):String = {
     var trnGood = 0;
     var trnBad = 0;
    val a = new AggregateUnmarshaller(classOf[ResponseEnvelope]);
    val re = a.unmarshal(new FileInputStream(file));

     //objeto contendo informações como instituição financeira, idioma, data da conta.
    val sr = re.getSignonResponse();
     println("Org: " + sr.getFinancialInstitution().getOrganization());
     println("Org: " + sr.getFinancialInstitution().getId());
     // santander permite ofxid duplicado principalmente no caso 
     // de credito de operadora de cartão - verificado na getnet 
     val validate = !(sr.getFinancialInstitution().getId() == "SANTANDER")
     //como não existe esse get "BankStatementResponse bsr = re.getBankStatementResponse();"
     //fiz esse codigo para capturar a lista de transações
     val typeMessage = MessageSetType.banking;
     val message = re.getMessageSet(typeMessage);
      if (message != null) {
        val bank = message.asInstanceOf[BankingResponseMessageSet].getStatementResponses().asScala;
        bank.foreach((b) => {
           println("bc: " + b.getMessage().getAccount().getBankId());
           println("cc: " + b.getMessage().getAccount().getAccountNumber());
           println("ag: " + b.getMessage().getAccount().getBranchId());
           println("balanço final: " + b.getMessage().getLedgerBalance().getAmount());
           println("dataDoArquivo: " + b.getMessage().getLedgerBalance().getAsOfDate());          
           val strAccountId = BusinessRulesUtil.clearString (b.getMessage().getAccount().getBankId())+
            BusinessRulesUtil.clearString (b.getMessage().getAccount().getAccountNumber())+
            BusinessRulesUtil.clearString (b.getMessage().getAccount().getBranchId())
           validateAccount (account, strAccountId);
           val list = b.getMessage().getTransactionList().getTransactions().asScala;
           list.foreach((transaction)=>{
               val amount:Double = if(transaction.getAmount() != null){
                  transaction.getAmount()
                }else{
                  0.toDouble
                }
               val typeMovement = if(amount > 0){
                  AccountPayable.IN
                }else{
                  AccountPayable.OUT
                }
                val value:Double = if(amount >= 0){
                  amount
                }else{
                  amount * -1
                }
                // getCheckNumber() CHECKNUM
                // getReferenceNumber() REFNUM
                // getId () FITID
                // getPayeeId() PAYEEID
                // getTransactionType() TRNTYPE
                //     XFER DEP CASH DEBIT (taxas) OTHER
               var ofxid = "";
               ofxid = transaction.getId();
               if (ofxid != transaction.getCheckNumber()) {
                ofxid = ofxid + " * " + transaction.getCheckNumber()
               }
               ofxid = strAccountId + ofxid
               //println ("vaiii ================ " + invoice.trim);
               val ctMov = AccountPayable.count (
                // antes testava só a duedate, mas qdo o fx é conciliado,
                // pode ser que o lancto no financeiro esteja com vencimento diferente
                // no entanto a data de pagamento do financeiro conciliado 
                // vai ser o duedate do ofx - por isso troquei pro BySql
                // com or
//              By (AccountPayable.dueDate, transaction.getDatePosted()),
                BySql(""" ( duedate = ? or paymentdate = ? ) """, IHaveValidatedThisSQL("dueDate", "01-01-2012 00:00:00"),
                  transaction.getDatePosted(),transaction.getDatePosted()),
                By (AccountPayable.ofxId, ofxid.trim),
                By (AccountPayable.account, account));
               if (ctMov <= 0 || !validate) {
                 val movement = AccountPayable.createInCompany
                 movement.ofxId(ofxid.trim)
                         .dueDate(transaction.getDatePosted())
                         //.paymentDate(transaction.getDatePosted())
                         .paid_?(false) // nao preenche data e seta false 
                         // para não alterar o saldo. a concialiação do ofx 
                         // é que marca como pago
                         .toConciliation_?(true)
                         .value(value)
                         .typeMovement(typeMovement)
                         .obs(transaction.getMemo())
                         .category(category)
                         .account(account)
                         .save
                 trnGood += 1; 
               } else {
                println ("vaiiiii rejeitadas ========== " + ofxid.trim)
                 trnBad += 1; 
               }
          })
        });
      }
      alreadyValidated = false;
      //println ("vaiiii ============ Importadas " + trnGood + "\n\n Rejeitadas " + trnBad);
      (" Importadas " + trnGood + "\n\nRejeitadas " + trnBad)
  }
  def validateAccount (account : Long, strAccountId : String) {
    // para o caso de vários nros de cartão no mesmo ofx
    // só valida o primeiro 
    if (!alreadyValidated) {
      val ac = Account.findAll (
          By (Account.id, account))(0);

      val acu = AccountCompanyUnit.findAll (
          By (AccountCompanyUnit.account, account),
          By (AccountCompanyUnit.unit, AuthUtil.unit.id.is))(0)
       if (BusinessRulesUtil.clearString (acu.accountStr) == "") {
          throw new RuntimeException("Número da conta no cadastro precisa ser informado");
       } 
       if (strAccountId.indexOf (BusinessRulesUtil.clearString (acu.accountStr)) == -1) {
          throw new RuntimeException("Número da Conta " + BusinessRulesUtil.clearString (acu.accountStr) + " não encontrada no ofx " + strAccountId);
       }
       // no caso de cartão de crédito não vem agência branchid
       // e acctid é o nro do cartão ou parte dele
       // pelo menos no BB e no Bradesco
       //
       if (!ac.creditCard_?) {  
         if (BusinessRulesUtil.clearString (acu.agency) == "") {
            throw new RuntimeException("Número da agência no cadastro precisa ser informado");
         } 
         if (strAccountId.indexOf (BusinessRulesUtil.clearString (acu.agency)) == -1) {
            throw new RuntimeException("Número da Agência " + BusinessRulesUtil.clearString (acu.agency) + " não encontrada no ofx " + strAccountId);
         }
      }
    }
    alreadyValidated = true;
    alreadyValidated
  }
}
