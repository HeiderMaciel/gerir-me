package code
package api

import code.model._
import code.util._
import code.service._

import net.liftweb._
import common._
import http._
import rest._
import net.liftweb.mapper._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers

import scala.xml._

import java.text.ParseException
import java.util._

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object ProductBomApi extends RestHelper {

	serve {
		case "product" :: "product_bom" :: id :: Nil Delete _ =>{
			ProductBOM.findByKey(id.toLong).get.delete_!
			JInt(1)
		}

		case "product" :: "product_bom" :: Nil Post _ =>{
			for {
				 product <-  S.param("product") ?~ "product param missing" ~> 400
				 product_bom <-  S.param("product_bom") ?~ "product_bom param missing" ~> 400
				 obs <- S.param("obs") ?~ "obs param missing" ~> 400
				 qtd <- S.param("qtd") ?~ "qtd param missing" ~> 400
				 praceled <- S.param("praceled") ?~ "qtd param missing" ~> 400
				 pricezero <- S.param("priceZero") ?~ "priceZero param missing" ~> 400
				 price <- S.param("price") ?~ "price param missing" ~> 400
				 orderinreport <- S.param("orderinreport") ?~ "orderinreport param missing" ~> 400
			} yield {
				JBool(ProductBOM.createInCompany.product(product.toLong).
					product_bom(product_bom.toLong).
					obs(obs).qtd(BigDecimal (qtd)).
					praceled_?(praceled.toBoolean).
					priceZero_?(pricezero.toBoolean).
					salePrice(price.toDouble).
					orderInReport(orderinreport.toInt).save)
			}
		}
		case "product" :: productId:: "product_bom" :: "list" :: Nil JsonGet _ =>{
			JsArray(
				ProductBOM.findAllInCompany(By(ProductBOM.product,productId.toLong),
					OrderBy (ProductBOM.orderInReport, Ascending)).map((pb) =>{
					JsObj(("id",pb.id.is),("product_name",pb.product_bom.obj.get.name.is),
						("obs",pb.obs.is), 
						("qtd",pb.qtd.toDouble), 
						("price", pb.price.toDouble),
						("listprice", pb.listPrice.toDouble),
						("parceled", pb.praceled_?.is), 
						("pricezero", pb.priceZero_?.is), 
						("orderinreport",pb.orderInReport.is))
				})
			)
		}
	}
}
