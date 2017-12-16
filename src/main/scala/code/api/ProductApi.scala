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


object ProductApi extends RestHelper {
		def id = S.param("id") match {
			case Full(s) if(s != "") => {
				"p.id = "+s
			}
			case _ => {
				"1 =1"
			}
		}
		def user = S.param("user") match {
			case Full(s) if(s != "") => {
				s.toLong
			}
			case _ => {
				0
			}		
		}			
		def category = S.param("category_select") match {
			case Full(s) if(s != "") => {
				" typeproduct in("+S.params("category_select").reduceLeft(_+","+_)+")"
			}
			case _ => {
				" 1 =1"
			}	
		}
		def nameStr =  BusinessRulesUtil.clearString(S.param("name") openOr "")
		def external_idStr = S.param("external_id") openOr ""
		def barcodeStr = S.param("barcode") openOr ""
		def name = S.param("name") match {
			case Full(s) if(s != "") => {
				// cliente era contem - produto estava começa com - rigel 31/07/2014
				Like(Product.search_name,"%"+BusinessRulesUtil.clearString(s)+"%")
			}
			case _ => {
				BySql[code.model.Product]("1 =1",IHaveValidatedThisSQL("",""))
			}		
		}
		def external_id = S.param("external_id") match {
			case Full(s) if(s != "") => {
				// aqui estava como igual - rigel coloquei contem
				Like(Product.external_id,"%"+s+"%")
			}
			case _ => {
				BySql[code.model.Product]("1 =1",IHaveValidatedThisSQL("",""))
			}		
		}	
		def barcode = S.param("barcode") match {
			case Full(s) if(s != "") => {
				Like(Product.barcode,"%"+s+"%")
			}
			case _ => {
				BySql[code.model.Product]("1 =1",IHaveValidatedThisSQL("",""))
			}		
		}	

	serve {
		case "product" :: "product_search" :: Nil JsonGet _ =>{
				def curPage = S.param("page") match {
								case Full(s) if(s != "") => {
									s.toInt
								}
								case _ => {
									0
								}		
							}						
			JsArray(Product.findAllForSearch(AuthUtil.company.id.is, "%"+nameStr+"%", external_idStr, 
				barcodeStr, curPage, user).map(asJson))
		}
		case "product" :: "product_search" :: Nil Post _ =>{
			JsArray(Product.findAllForSearch(AuthUtil.company.id.is, "%"+nameStr+"%", "%"+external_idStr+"%", 
				"%"+barcodeStr+"%", 0, user, category + " and "+id).map(asJson))
		}
		case "product" :: "product_line" :: Nil JsonGet _ =>{
			JsArray(ProductLine.findAllInCompany(OrderBy (ProductLine.name, Ascending)).map((pl) =>{
				JsObj(("name",pl.name.is), ("id", pl.id.is))
			}))
		}
		case "product" :: "product_type" :: Nil JsonGet _ =>{
			JsArray(ProductType.findAllInCompany(OrderBy (ProductType.name, Ascending)).map((pl) =>{
				JsObj(("name",pl.name.is+" - " + pl.typeClass), ("id", pl.id.is))
			}))
		}		
		case "product" :: "product_line" :: Nil Put _ =>{
			for{
				 name <- S.param("name") ?~ "name parameter missing" ~> 400
				 obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
			}yield{
				ProductLine.createInCompany.name(name).obs(obs).save
			}
			JInt(1)
		}
		case "product" :: Nil JsonGet _ =>{
			JsArray(AuthUtil.company.products.map(asJson(_)))
		}

		case "product"  :: "for_sale" :: Nil JsonGet _ =>{
			lazy val product:ProductPreviousDebts = ProductPreviousDebts.productPreviousDebts
			lazy val productCredit:ProductPreviousDebts = ProductPreviousDebts.productCredits
			lazy val productJson =  JsObj(("status","success"),("name",product.name.is), ("id", product.id.is))
			lazy val productCreditJson =  JsObj(("status","success"),("name",productCredit.name.is), ("id", productCredit.id.is))
			JsArray(productCreditJson::productJson::(Product.findAllForSale.map(asJson(_)) ))
		}

		case "product"  :: "discount" :: Nil JsonGet _ =>{
			JsArray(Product.findAllDiscount.map(asJson(_)))
		}
	}

	def asJson (p:Product):JsObj = JsObj(
					("status","success"),
					("name",p.name.is),
					("id",p.id.is),
					("external_id",p.external_id.is),
					("price",p.salePrice.toDouble),
					("is_bom", p.is_bom_?.is),
					("allowSaleByUser", p.allowSaleByUser_?.is),
					("products", if(p.is_bom_?.is) {
						JsArray(
							p.products_bom.map(
								(pb) => 
									JsObj(
										("price_bom",pb.price.toDouble),
										("qtd_bom",pb.qtd.toDouble),
										("parceled",pb.praceled_?.is),
										("product",asJson(pb.product_bom.obj.get)),
										("order_bom",pb.orderInReport.is)
									)
								)
							) 
					}else{
						JsArray(Nil)
					}
					)
				);
				def asJson (p:ProductSearch):JsObj = JsObj(
					("status","success"),
					("name",p.name),
					("id",p.id),
					("external_id",p.external_id),
					("price",p.price),
					("brandname",p.brandname),
					("is_bom", p.is_bom),
					("products", if(p.is_bom) {
						JsArray(
							p.products_bom.map(
								(pb) => 
									JsObj(
										("price_bom",pb.price.toDouble),
										("qtd_bom",(pb.qtd.toDouble)),
										("parceled",pb.praceled_?.is),
										("product",asJson(pb.product_bom.obj.get)),
										("order_bom",pb.orderInReport.is)
									)
								)
							) 
					}else{
						JsArray(Nil)
					}
					)
				);
}
