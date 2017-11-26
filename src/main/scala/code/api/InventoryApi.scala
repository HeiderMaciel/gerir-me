package code
package api

import code.model._
import code.util._
import code.service._

import net.liftweb._
import mapper._ 
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers
import InventoryMovement._

import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.

object InventoryApi extends RestHelper {
	serve {

		case "inventory" :: "movements_filter" :: Nil JsonGet _ =>{
			def filters:List[QueryParam[InventoryMovement]] = {

				def customer = S.param("customer") match {
					case Full(s) if(s != "") => By(InventoryMovement.business_pattern,s.toLong)
					case _ => BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
				}

				def id = S.param("id") match {
					case Full(s) if(s != "") => {
						By(InventoryMovement.product,s.toLong)
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}
				}
				
				def line = S.param("line_select") match {
					case Full(s) if(s != "") => {
						BySql[code.model.InventoryMovement]("product in (select product from productlinetag where line in("+S.params("line_select").foldLeft("0")(_+","+_)+"))",IHaveValidatedThisSQL("",""))
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}
					
				}	
				def category = S.param("category_select") match {
					case Full(s) if(s != "") => {
						BySql[code.model.InventoryMovement]("product in (select id from product where typeproduct in("+S.params("category_select").reduceLeft(_+","+_)+"))",IHaveValidatedThisSQL("",""))
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}		
				}

				def types = S.param("types") match {
					case  Full(a) if(a != "") => {
						val s = S.params("types").reduceLeft(_+_)
						def In = if(s.contains("In")){
							List(InventoryMovement.InventoryMovementType.In)
						}else{
							Nil
						}
						def Out = if(s.contains("Out")){
							List(InventoryMovement.InventoryMovementType.Out)
						}else{
							Nil
						}						

						ByList(InventoryMovement.typeMovement,In:::Out)
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}					
				}

				def unit = S.param("units") match {
					case Full(s) if(s != "") => {
						BySql[code.model.InventoryMovement]("unit in ("+S.params("units").reduceLeft(_+","+_)+")",IHaveValidatedThisSQL("",""))
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}		
				}				

				
				def causes = S.param("causes") match {
					case Full(s) if(s == "not_sale") => {
						BySql[code.model.InventoryMovement]("inventoryCause <> "+AuthUtil.company.inventoryCauseSale.is+"",IHaveValidatedThisSQL("",""))
					}					
					case Full(s) if(s != "") => {
						BySql[code.model.InventoryMovement]("inventoryCause in ("+S.params("causes").reduceLeft(_+","+_)+")",IHaveValidatedThisSQL("",""))
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}
				}

				def name = S.param("name") match {
					case Full(s) if(s != "") => {
						BySql[code.model.InventoryMovement]("product in (select id from product where search_name like '%"+BusinessRulesUtil.clearString(s)+"%')",IHaveValidatedThisSQL("",""))
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}
					
				}

				def qtd_query = S.param("qtd_start") match {
					case Full(s) if(s != "") => {
						def end =  S.param("qtd_end") match {
							case Full(se) if(se != "") => se.toInt
							case _ => 10000
						}
						
						BySql[code.model.InventoryMovement]("amount between ? and ?",IHaveValidatedThisSQL("",""), s.toInt,  end)
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}

				}
				def date_query = S.param("start_date") match {
					case Full(s) if(s != "") => {
						def start  = Project.strOnlyDateToDate(s)
						def end =  S.param("end_date") match {
							case Full(se) if(se != "") => Project.strOnlyDateToDate(se)
							case _ => new Date()
						}
						
						BySql[code.model.InventoryMovement](" efetivedate between date(?) and date(?)",IHaveValidatedThisSQL("datepayment","01-01-2012 00:00:00"),start,end)//fica preso ao postgress
					}
					case _ => {
						BySql[code.model.InventoryMovement]("1 =1",IHaveValidatedThisSQL("",""))
					}

				}				
				date_query :: qtd_query :: id :: customer :: line :: category :: name :: causes :: unit :: types ::
				OrderBy(InventoryMovement.efetiveDate, Ascending) :: OrderBy(InventoryMovement.id, Ascending) :: Nil
			}			
			JsArray(
					InventoryMovement.findAllInCompany(filters :_*).map( (im)=> {
					JsObj(
							("type_movement",im.typeMovement.is.toString),
							("obs",im.obs.is.toString),
							("invoice",im.invoice.is),
							("amount",im.amount.is.toFloat),
							("price",im.totalSalePrice.is.toFloat),
							("purchaseprice",im.purchasePrice.is.toFloat),
							("unit",im.unit_name),
							("cause",im.cause_name),
							("product_id",im.product.is),
							("efetive_date", im.efetiveDate.is.toString()),
							("product_name",im.product_name),
							("bp_name",im.bp_name),
							("bp_id",im.business_pattern.is),
							("auditStr", im.auditStr),
							("im_id",im.id.is)
						)
				})
			)
		}
		case "inventory" :: "del_purchaseOrderItem" :: Nil Post _ => {
			try { 
				def imId:String = S.param("imid") openOr "0"
  				val ac = InventoryMovement.findByKey(imId.toLong).get	
  				ac.delete_!
				JInt(1)
			} catch {
			  case e: Exception => JString(e.getMessage)
			}			
		}

		case "inventory" :: "transferCause" :: Nil JsonGet _ => {
			JInt(AuthUtil.company.inventoryCauseTrasfer.is)
		}

		case "inventory" :: "causes" :: Nil JsonGet _ => {
			//JsArray(InventoryCause.findAllInCompany(OrderBy(InventoryCause.name, Ascending)).map(c => JsObj(("id",c.id.is),("name", c.name.is),("forSale",c.forSale_?), ("forPurchase",c.forPurchase_?), ("forTasfer", c.forTasfer_?))))
			JsArray(InventoryCause.findAll/*InCompanyOrDefaultCompany*/(OrderBy(InventoryCause.name, Ascending)).map(c => JsObj(("id",c.id.is),("name", c.name.is),("forSale",c.forSale_?), ("forPurchase",c.forPurchase_?), ("forTasfer", c.forTasfer_?))))
		}

		case "inventory" :: "units" :: Nil JsonGet _ => {
			val currentUnit = AuthUtil.unit
			val currentCompany = AuthUtil.company.id
			val currentUser = AuthUtil.user.id
			if (AuthUtil.user.isAdmin) {
				JsArray(CompanyUnit.findAllInCompany(
					OrderBy(CompanyUnit.name, Ascending)).map(u => JsObj(("id",u.id.is),("name", u.name.is),("isCurrent",u.id.is == currentUnit.id.is))))
			} else {
				JsArray(CompanyUnit.findAllInCompany(
					//BySql ("(id in (select unit from usercompanyunit where user_c = ? and company = ?) or (1 > (select count (1) from usercompanyunit where user_c = ? and company = ?)))",IHaveValidatedThisSQL("",""),currentCompany,currentUser,currentCompany,currentUser),
					BySql(" (id = ? or (id in (select uu.unit from usercompanyunit uu where uu.user_c = ? and uu.company = ?))) ",
	                IHaveValidatedThisSQL("",""), AuthUtil.user.unit, AuthUtil.user.id, AuthUtil.company.id),
					OrderBy(CompanyUnit.name, Ascending)).map(u => JsObj(("id",u.id.is),("name", u.name.is),("isCurrent",u.id.is == currentUnit.id.is))))
			}
		}

		case "inventory" :: "currentstock" :: productId :: companyUnit :: Nil JsonGet _ => {
			def unit:CompanyUnit = if(companyUnit == "0"){
				AuthUtil.unit
			}else{
				CompanyUnit.findByKey(companyUnit.toLong).get
			}
			JDouble(InventoryCurrent.currentStock(Product.findByKey(productId.toLong).get,unit).toFloat)
		}
		case "inventory" :: "currentstock" :: productId :: Nil JsonGet _ => {
			def prod = Product.findByKey(productId.toLong).get
			JsArray(
				CompanyUnit.findAllInCompany.map((u)=>{
					JsObj(
						("name",u.name.is),
						("amount",InventoryCurrent.currentStock(prod, u))
					)
				})
			)
		}		

		case "inventory" :: "purchasePrice" :: productId :: Nil JsonGet _ => {
			JDouble(Product.findByKey(productId.toLong).get.purchasePrice.is.toDouble)
		}

		case "inventory" :: "salePrice" :: productId :: Nil JsonGet _ => {
			JDouble(Product.findByKey(productId.toLong).get.salePrice.is.toDouble)
		}

		case "inventory" :: "save" :: Nil Post _ => {
			for {
				data <- S.param("data") ?~ "data parameter missing" ~> 400
			} yield {
				val json = parse(data)
				val movements = json.extract[scala.List[InventoryMovementRequest]]
				DB.use(DefaultConnectionIdentifier) {
					 conn =>
					 try{				
							movements.foreach((m) => {
								def movementRegister(type_movement:String, unit:CompanyUnit,obs:String ) = {
										val movement = type_movement match {
											case "In" => {
												add(m.purchase_price, m.indic1, m.indic2, m.indic3, m.indic4, m.indic5, 
													m.sale_price, m.amount)
											}case "Out" => {
												remove(m.amount)
											}
									}
									if (m.supplier_obj != null) {
										movement item m.productSelected obs(obs) company(AuthUtil.company) toInvoice(m.invoice) business_pattern (m.supplier_obj) cause(m.cause_obj) day(m.date_date) from unit
									} else {
										movement item m.productSelected obs(obs) company(AuthUtil.company) toInvoice(m.invoice) cause(m.cause_obj) day(m.date_date) from unit
									}
								}
								m.type_movement match {
									case "In" => {
										movementRegister("In",m.unit_of_obj,m.obs)
									}case "Out" => {
										movementRegister("Out",m.unit_of_obj,m.obs)
									}case _ => {
										if (m.obs != "Transferência") {
											movementRegister("Out",m.unit_of_obj, "Transferência para "+m.unit_to_obj.name.is+" "+m.obs)
											movementRegister("In",m.unit_to_obj,"Transferência de "+m.unit_of_obj.name.is+" "+m.obs)
										} else {
											movementRegister("Out",m.unit_of_obj, "Transferência para "+m.unit_to_obj.name.is)
											movementRegister("In",m.unit_to_obj,"Transferência de "+m.unit_of_obj.name.is)
										}
									}
								}
							})
							JInt(0)
						}catch{
							case e:InsufficientInventoryException => {conn.rollback
								JString("Estoque insuficiente!")
							}
							case e:Exception => { conn.rollback
								throw e
							}
						}
					}
			}
			
		}

	}
}

case class InventoryMovementRequest(date:String, product:Long, type_movement:String, 
	unit_to:Long, unit_of:Long, sale_price:Double, purchase_price:Double, 
	indic1:Double, indic2:Double, indic3:Double, indic4:Double, indic5:Double,
	invoice:String, amount:Float, obs:String, 
	id:Long, supplier:Long, cause:Long){
	def productSelected = Product.findByKey(product).get
	def unit_to_obj = CompanyUnit.findByKey(unit_to).get
	def unit_of_obj = CompanyUnit.findByKey(unit_of).get
	def supplier_obj = if (supplier != 0) {
		Customer.findByKey(supplier).get
		} else {
			null
		}
	def cause_obj = InventoryCause.findByKey(cause).get
	def date_date = Project.strOnlyDateToDate(date)
}


