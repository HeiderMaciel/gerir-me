
package code
package snippet

import net.liftweb._
import http._
import code.util._
import model._
import http.js._
import JE._
import JsCmds._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import scala.xml.{ NodeSeq, Text }
import net.liftweb.mapper._
import net.liftweb.mapper.{StartAt, MaxRows}
import java.util.{Date, Calendar};

class  OffSaleProductSnippet extends net.liftweb.common.Logger {

	def offsales = ("0", "Selecione um convênio") :: OffSale.findAllInCompany(OrderBy(OffSale.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def getOffSaleProduct:OffSaleProduct = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => OffSaleProduct.create
			case _ => OffSaleProduct.findByKey(id.toLong).get
		}
	}

	def maintain = {
		try{
			var ac:OffSaleProduct = getOffSaleProduct
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
					ac.save
				   	S.notice("Convênio/Produto salvo com sucesso!")
				   	S.redirectTo("/offsale/offsaleproduct?id="+ac.id.is)
		   		}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}
		    "name=offsale" #> (SHtml.select(offsales,Full(ac.offsale.is.toString),(s:String) => ac.offsale( s.toLong)))&
			"name=product" #> (SHtml.text(ac.product.is.toString, (p:String) => ac.product(p.toLong)))&
			"name=productName" #> (SHtml.text(ac.productName, (a:String) => {}))&
			"name=productTypeName" #> (SHtml.text(ac.productTypeName, (a:String) => {}))&
			"name=suggestedprice" #> (SHtml.text(ac.suggestedPrice.is.toString, (v:String) => { if(v !="")ac.suggestedPrice(v.toDouble)} ))&
			"name=offprice" #> (SHtml.text(ac.offPrice.is.toString, (v:String) => { if(v !="")ac.offPrice(BusinessRulesUtil.snippetToDouble(v))} ))&
			"name=percentoff" #> (SHtml.text(ac.percentOff.is.toString, (v:String) => { if(v !="")ac.percentOff(BusinessRulesUtil.snippetToDouble(v))} ))&
			"name=indic1" #> (SHtml.text(ac.indic1.is.toString, (v:String) => { if(v !="")ac.indic1(v.toDouble)} ))&
			"name=indic2" #> (SHtml.text(ac.indic2.is.toString, (v:String) => { if(v !="")ac.indic2(v.toDouble)} ))&
			"name=indic3" #> (SHtml.text(ac.indic3.is.toString, (v:String) => { if(v !="")ac.indic3(v.toDouble)} ))&
			"name=indic4" #> (SHtml.text(ac.indic4.is.toString, (v:String) => { if(v !="")ac.indic4(v.toDouble)} ))&
			"name=indic5" #> (SHtml.text(ac.indic5.is.toString, (v:String) => { if(v !="")ac.indic5(v.toDouble)} ))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))
		}catch {
		    case e: NoSuchElementException => S.error("Convênio/produto não existe!")
		    "#offsaleproduct_form *" #> NodeSeq.Empty
  		}
  	}
  	
}

