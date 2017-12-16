package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

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

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object OffSaleProductApi extends  RestHelper with ReportRest with net.liftweb.common.Logger {
		def offsaleId = S.param("offsaleId").get.toLong		
		serve {
			case "api" :: "v2" :: "osproduct" :: Nil Post _ => {
				for {
					offsale <- S.param("offsale") ?~ "offsale parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					product <- S.param("product") ?~ "product parameter missing" ~> 400
					offprice <- S.param("offprice") ?~ "offprice parameter missing" ~> 400
					producttype <- S.param("producttype") ?~ "producttype parameter missing" ~> 400
					percentoff <- S.param("percentoff") ?~ "percentoff parameter missing" ~> 400
				} yield {
					JBool(OffSaleProduct.createInCompany.offsale(offsale.toLong).obs(obs)
						.product(product.toLong)
						.productType(producttype.toLong)
						.percentOff(percentoff.toFloat)
						.offPrice(offprice.toFloat)
						.save)
				}
			}
			case  "api" :: "v2" :: "osproduct" :: id :: Nil Delete _ => {
				JBool(OffSaleProduct.findByKey(id.toLong).get.delete_!)
			}
			case  "api" :: "v2" :: "osproduct" :: "list" :: Nil Post _ => {
			    lazy val osproduct_query = """
			        select pt.name, pr.name, percentoff, offprice, 
			        op.obs, pr.is_bom, op.external_id, op.id 
			        from OffSaleProduct op
			        left join product pr on pr.id = product and pr.productclass in (0,1)
			        left join producttype pt on pt.id = producttype
			        where  op.company=? and offsale = ?
			        order by pr.productclass, pr.name
			    """
				toResponse(osproduct_query,List(AuthUtil.company.id.is, offsaleId)) //offsaleId.toLong))
			}
		}

}

