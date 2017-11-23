
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


class  ProductSnippet  extends BootstrapPaginatorSnippet[Product] with SnippetUploadImage  {
	
	/**
	* Pagination Methods
	*/
	def pageObj = Product

	def findForListParamsWithoutOrder: List[QueryParam[Product]] = List(Like(Product.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"))

	def findForListParams: List[QueryParam[Product]] = List(
		Like(Product.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
		BySql (productTypeList,IHaveValidatedThisSQL("","")),
		BySql (inventory_controlList,IHaveValidatedThisSQL("","")),
		BySql (discountList,IHaveValidatedThisSQL("","")),
		BySql (bomList,IHaveValidatedThisSQL("","")),
		OrderBy(Product.name, Ascending), StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage), ByList(Product.status,statusFilter))
	override def page = {
		if(!showAll){
			super.page
		}else{
			Product.findAllInCompanyWithInactive(findForListParams :_*)
		}
	}	

	def statusFilter: List[Int] = if(showAll){
		List(Product.STATUS_OK, Product.STATUS_INACTIVE)
	}else{
		List(Product.STATUS_OK)
	}

	def types = AuthUtil.company.productTypes.map(t => (t.id.is.toString,t.name.is))
	def brands = ("0", "Selecione um Fabricante") :: Brand.findAllInCompany(OrderBy(Brand.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def igroups = InvoiceGroup.findAllInCompanyOrDefaultCompany(OrderBy(InvoiceGroup.name, Ascending)).map(t => (t.id.is.toString,t.name.is))
	def uoms = ("0" -> "Selecione uma unidade de medida")::UnitofMeasure.findAll(OrderBy(UnitofMeasure.name, Ascending)).map(t => (t.id.is.toString,t.name.is))

	def productType = S.param("productType") match {
		case Full(s) => s
		case _ => ""
	}	
    val productTypeList = if(productType != ""){
        "typeproduct = %s ".format (productType)
    }else{
        " 1 = 1 "
    }
	def inventory_control = S.param("inventory_control") match {
		case Full(p) if(p != "")=> 1
		case _ => 0
	}	
    val inventory_controlList = if(inventory_control == 1){"is_inentory_control = true"}else{ " 1 = 1 " }
	
	def discount = S.param("discount") match {
		case Full(p) if(p != "")=> 1
		case _ => 0
	}	
    val discountList = if(discount == 1){"is_discount = true"}else{ " 1 = 1 " }
	
	def bom = S.param("bom") match {
		case Full(p) if(p != "")=> 1
		case _ => 0
	}	
    val bomList = if(bom == 1){"is_bom = true"}else{ " 1 = 1 " }

	def costcenters = ("0" -> "Selecione um Centro de Custo")::
	CostCenter.findAllInCompany(OrderBy(CostCenter.name, Ascending)).map(cc => (cc.id.is.toString,cc.name.is))
	def accountcategories = ("0" -> "Selecione uma Categoria")::
	AccountCategory.findAllInCompany(OrderBy(AccountCategory.name, Ascending)).map(cc => (cc.id.is.toString,cc.name.is))

	lazy val all_products_and_services = Product.findAllInCompany().map(t => (t.id.is.toString,t.name.is))

	def products(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val p = Product.findByKey(id.toLong).get	
		  				p.delete_!
		  				S.notice("Produto excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Produto não existe!")
		  				case e:Exception => S.error(e.getMessage)
		  				case _ => S.error("Produto não pode ser excluído!")
		  			}
			}

			page.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"thumb" -> ac.thumb ("36"),
							"purchaseprice" -> Text(ac.purchasePrice.is.toString),
							"obs" -> Text(ac.obs.is),
							"type" -> Text(ac.typeName),
							"saleprice" -> Text(ac.salePrice.is.toString),
							"commission" -> Text(ac.commission.is.toString),
							"pointsonbuy" -> Text(ac.pointsOnBuy.is.toString),
							"actions" -> <a class="btn" href={"/product_admin/edit?id="+ac.id.is}>Editar</a>,
							"currentstock" -> <table>{ac.inventoryUnits.map((ip) => <tr><td>{ip.unit.obj.get.name} </td> <td>{ip.currentStock.is.toString}</td></tr>)}</table>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message" -> {" Excluir o produtodo "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"ext_id" ->Text(ac.external_id.is.toString),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def types(xhtml: NodeSeq): NodeSeq = {
			var id:String = ""
		 	def delete(): Unit ={
			  		try{
		  				val ac = ProductType.findByKey(id.toLong).get	
		  				ac.delete_!
		  				S.notice("Tipo de produto excluído com sucesso!")
		  			}catch{
		  				case e: NoSuchElementException => S.error("Tipo de produto não existe!")
		  				case _ => S.error("Tipo de produto não pode ser excluído!")
		  			}
			
			}

			ProductType.findAllProduct.flatMap(ac => 
			bind("f", xhtml,"name" -> Text(ac.name.is),
							"obs" -> Text(ac.obs.is),
							"igname" -> Text(ac.invoiceGroupName),
							"actions" -> <a class="btn" href={"/product_admin/edit_type?id="+ac.id.is}>Editar</a>,
							"delete" -> SHtml.submit("Excluir",delete,"class" -> "btn danger","data-confirm-message"->{" excluir o tipo de produto/serviço "+ac.name.is}),
							"_id" -> SHtml.text(ac.id.is.toString, id = _),
							"id" ->Text(ac.id.is.toString)
				)
			)
	}

	def getProduct:Product = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => Product.create
			case _ => Product.findByKey(id.toLong).get
		}
	}
	
	def getProductType:ProductType = {
		def id = S.param("id") openOr "0"
		id match {
			case "0" => ProductType.create
			case _ => ProductType.findByKey(id.toLong).get
		}
	}


	def maintainType = {
		try{
			var ac:ProductType = getProductType
			def process(): JsCmd= {
				try {
					ac.company(AuthUtil.company)
				   	ac.save	
				   	S.notice("Tipo de produto salvo com sucesso!")
					S.redirectTo("/product_admin/edit_type?id="+ac.id.is)
				}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case e:RuntimeException => S.error(e.getMessage)
					case _ => S.error("Erro desconhecido tente novamente")
				}
			}
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=invoiceGroup" #> (SHtml.select(igroups,Full(ac.invoiceGroup.is.toString),(s:String) => ac.invoiceGroup( s.toLong)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Tipo de produto não existe!")
		    "#product_form *" #> NodeSeq.Empty
  		}
  	}

	def maintain = {
		try{
			var ac:Product = getProduct
			def process(): JsCmd= {
				try{
					ac.company(AuthUtil.company)
					ac.clearLines
					S.params("lines").map(_.toLong).foreach((id) =>{
						ProductLineTag.join(id,ac.id.is)
					}) 
				   	ac.save
				   	S.notice("Produto salvo com sucesso!")
				   	S.redirectTo("/product_admin/edit?id="+ac.id.is.toString)
				}catch{
					case (e:net.liftweb.http.ResponseShortcutException) =>{
						throw e
					}
					case (e:Exception) => {
						S.error(e.getMessage)
					}
				}
			}

			"name=allowshowonsite" #> (SHtml.checkbox(ac.allowShowOnSite_?, ac.allowShowOnSite_?(_)))&
			"name=allowshowonportal" #> (SHtml.checkbox(ac.allowShowOnPortal_?, ac.allowShowOnPortal_?(_)))&
			"name=moderatedportal" #> (SHtml.checkbox(ac.moderatedPortal_?, ac.moderatedPortal_?(_)))&
		    "name=sitetitle" #> (SHtml.text(ac.siteTitle.is, ac.siteTitle(_)))&
		    "name=sitedescription" #> (SHtml.textarea(ac.siteDescription.is, ac.siteDescription(_)))&
		    "name=name" #> (SHtml.text(ac.name.is, ac.name(_)))&
		    "name=short_name" #> (SHtml.text(ac.short_name.is, ac.short_name(_)))&
		    "name=showInCommand" #> (SHtml.checkbox(ac.showInCommand_?.is, ac.showInCommand_?(_)))&
		    "name=orderInCommand" #> (SHtml.text(ac.orderInCommand.is.toString, (s:String) => ac.orderInCommand(s.toInt))) &
			"#img_product" #> ac.thumb("192")&
		    "name=type" #> (SHtml.select(types,Full(ac.typeProduct.is.toString),(v:String) => ac.typeProduct(v.toLong)))&
		    "name=brand" #> (SHtml.select(brands,Full(ac.brand.is.toString),(v:String) => ac.brand(v.toLong)))&
		    "name=pointsonbuy" #> (SHtml.text(ac.pointsOnBuy.is.toString, (v:String) => { 
					try{
						ac.pointsOnBuy(v.toDouble)
					}catch{
						case _ => ac.pointsOnBuy(0.00)
					}
			}))&
			"name=salePrice" #> (SHtml.text(ac.salePrice.is.toString, (v:String) => { 
					try{
						ac.salePrice(BigDecimal(v))
					}catch{
						case _ => ac.salePrice(BigDecimal(0))
					}
				}))&
			"name=purchasePrice" #> (SHtml.text(ac.purchasePrice.is.toString, (v:String) => { 
					try{
						ac.purchasePrice(BigDecimal(v))
					}catch{
						case _ => ac.purchasePrice(BigDecimal(0))
					}
				}))&
			"name=measureinunit" #> (SHtml.text(ac.measureinUnit.is.toString, (v:String) => { 
					try{
						ac.measureinUnit(BigDecimal(v))
					}catch{
						case _ => ac.measureinUnit(BigDecimal(0))
					}
				}))&
			"name=lines_text" #> (SHtml.text(ac.lines_text, (v:String) => {}))&
			"name=external_id" #> (SHtml.text(ac.external_id, (v:String) => { ac.external_id(v)}))&
			"name=barcode" #> (SHtml.text(ac.barcode, (v:String) => { ac.barcode(v)}))&
		    "name=costcenter" #> (SHtml.select(costcenters,Full(ac.costCenter.is.toString),(s:String) => ac.costCenter( s.toLong)))&
		    "name=unitofmeasure" #> (SHtml.select(uoms,Full(ac.unitofMeasure.is.toString),(s:String) => ac.unitofMeasure( s.toLong)))&
		    "name=accountCategory" #> (SHtml.select(accountcategories,Full(ac.accountCategory.is.toString),(s:String) => ac.accountCategory( s.toLong)))&
		    "name=discountAccountCategory" #> (SHtml.select(accountcategories,Full(ac.discountAccountCategory.is.toString),(s:String) => ac.discountAccountCategory( s.toLong)))&
			"name=commission" #> (SHtml.text(ac.commission.is.toString, (v:String) => ac.commission(BigDecimal(v))))&
			"name=currentStock" #> (<span>{ac.currentStock.toString}</span>)&
			"name=is_bom" #> (SHtml.checkbox(ac.is_bom_?, ac.is_bom_?(_)))&
			"name=is_bomaux" #> (SHtml.text(ac.is_bom_?.toString, (a:String) => {}))&
			"name=allowSaleByUser" #> (SHtml.checkbox(ac.allowSaleByUser_?, ac.allowSaleByUser_?(_)))&
			"name=is_discount" #> (SHtml.checkbox(ac.is_discount_?, ac.is_discount_?(_)))&
			"name=is_for_sale" #> (SHtml.checkbox(ac.is_for_sale_?, ac.is_for_sale_?(_)))&
			"name=allow_negative_inventory" #> (SHtml.checkbox(ac.allow_negative_inventory_?, ac.allow_negative_inventory_?(_)))&
			"name=is_inentory_control" #> (SHtml.checkbox(ac.is_inentory_control_?, ac.is_inentory_control_?(_)))&
			"name=minStock" #> (SHtml.text(ac.minStock.is.toString, (v:String) => ac.minStock(v.toInt)))&
			"name=status" #> (SHtml.select(status,Full(ac.status.is.toString),(v:String) => ac.status(v.toInt)))&
			"name=obs" #> (SHtml.textarea(ac.obs.is, ac.obs(_))++SHtml.hidden(process))

		}catch {
		    case e: NoSuchElementException => S.error("Produto não existe!")
		    "#activity_form *" #> NodeSeq.Empty
  		}
  	}
	def setImageToEntity(homeName:String, thumbName:String){
		val product = getProduct
		product.image(homeName).imagethumb(thumbName).save
	}
  	def imageFolder:String = Product.imagePath
  	def thumbToShow:NodeSeq = getProduct.thumb("128")
}

