package code
package model 

import code.actors._
import code.service._
import net.liftweb._ 
import mapper._ 
import net.liftweb.common._
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 
import net.liftweb.widgets.gravatar.Gravatar
import code.util._
import _root_.java.util.Calendar
import _root_.java.util.Date
//Class de cliente
class Customer extends BusinessPattern[Customer]{
    def getSingleton = Customer
    override def updateShortName = false
    override def updateIdForCompany = if(AuthUtil.?) {
        AuthUtil.company.bpIdForCompany == 0 || AuthUtil.company.bpIdForCompany == 1
    } else {
        false
    }
    
    def valueInAccountAtPayment(payment:Payment):Double  = {
        def date = payment.createdAt.is
        val r = DB.performQuery(Customer.SQL_GET_CUSTOMER_ACCOUNT_HISTORY, List(this.id.is,date , date, this.id.is))
        try{
            //LogActor ! "Retorno consulta conta cliente : "+date.toString
            r._2(0)(0) match {
               case a:Any => a.toString.toDouble
               case _ => 0.00
            }
        }catch{
            case _ => {
                LogActor ! "Erro pegando histórico de conta cliente payment[ "+payment.id.is+" ]"
                throw new RuntimeException("Erro pegando histórico de conta cliente payment")
            }
        }
    }
    object valueInPoints extends MappedCurrency(this)
    object valueInAccount extends MappedDouble(this)with LifecycleCallbacks {
        override def defaultValue = 0.00
        override def beforeSave() {
            super.beforeSave;
            if(this.get > -0.01 && this.get < 0.009){
                this.set(0)
            }
        } 
    }

    def replaceMessage (ac:Customer, message:String) = {
        var message_aux = message;
        //val extenso = WrittenForm (123.999.467.89)
        //println ("vaiiii ===================== " + extenso.humanize());
        //println ("vai =================== fora ")
        if (ac.name.is != "") {
            //println ("vai =================== dentro ")

            message_aux = message_aux.replaceAll("##hoje##", Project.dateToExt(new Date()));
            message_aux = message_aux.replaceAll("##mescorrente##", Project.monthToExt(new Date()));
            message_aux = message_aux.replaceAll("##messeguinte##", Project.monthToExt(Project.nextMonth (new Date())));
            message_aux = message_aux.replaceAll("##mesanterior##", Project.monthToExt(Project.prevMonth (new Date())));

            message_aux = message_aux.replaceAll("##logo##", "<img width='100px' src='" + AuthUtil.company.thumb_web + "'/>");

            message_aux = message_aux.replaceAll("##nome##", ac.name.is)
            message_aux = message_aux.replaceAll ("##apelido##", ac.short_name.is)
            message_aux = message_aux.replaceAll ("##prinome##", ac.firstName)
            message_aux = message_aux.replaceAll ("##telefone##", ac.phone)
            message_aux = message_aux.replaceAll ("##celular##", ac.mobilePhone)
            message_aux = message_aux.replaceAll ("##telefone2##", ac.email_alternative)
            message_aux = message_aux.replaceAll ("##email##", ac.email)
            message_aux = message_aux.replaceAll ("##doc##", ac.document + ac.document_company)
            message_aux = message_aux.replaceAll ("##doc_rg##", ac.document_identity)
            message_aux = message_aux.replaceAll ("##nasc_data##", Project.dateToStr(ac.birthday))
            message_aux = message_aux.replaceAll ("##nasc_idade##", Project.dateToAge(ac.birthday))
            message_aux = message_aux.replaceAll ("##nasc_anos##", Project.dateToYears(ac.birthday))
            message_aux = message_aux.replaceAll ("##nasc_ext##", Project.dateToExt(ac.birthday))

            message_aux = message_aux.replaceAll ("##peso##", ac.weight.toString)
            message_aux = message_aux.replaceAll ("##altura##", ac.height.toString)
            message_aux = message_aux.replaceAll ("##imc_num##", ac.BMI.substring(0,scala.math.min(ac.BMI.length, 5)))
            message_aux = message_aux.replaceAll ("##imc_classe##", ac.BMI)

            message_aux = message_aux.replaceAll ("##convenio##", ac.offSaleShortName)
            message_aux = message_aux.replaceAll ("##profissao##", ac.occupationShortName)

            message_aux = message_aux.replaceAll ("##end_rua##", ac.street)
            message_aux = message_aux.replaceAll ("##end_nro##", ac.number)
            message_aux = message_aux.replaceAll ("##end_compl##", ac.complement)
            message_aux = message_aux.replaceAll ("##end_cep##", ac.postal_code)
            message_aux = message_aux.replaceAll ("##end_cid##", ac.cityName)
            message_aux = message_aux.replaceAll ("##end_bairro##", ac.district)
            message_aux = message_aux.replaceAll ("##end_ref##", ac.pointofreference)
            message_aux = message_aux.replaceAll ("##end_uf##", ac.stateShortName)
        }
        message_aux
    }

    def custInfo = {
        var parm = "##nasc_idade##, ##apelido##, ##convenio##"
        replaceMessage (this, parm)
    }
    
    private def _registerDebit(value:Double) = {
        this.valueInAccount(this.valueInAccount.is+value).save
    }

    def registerPoints(value:Double, payment:Payment, treatmentDetail:TreatmentDetail, obs:String) = {
        this.valueInPoints(this.valueInPoints.is+value).save
    }

    // rigel 07/082017 - criar history 
    def registerPointsPt(value:Double, payment:Payment, paymentDetail:PaymentDetail, obs:String) = {
        this.valueInPoints(this.valueInPoints.is+value).save
    }

    // rigel 07/082017 - criar history 
    def unRegisterPoints(value:Double, payment:Payment, paymentDetail:PaymentDetail, obs:String) = {
        this.valueInPoints(this.valueInPoints.is-value).save
    }

    def registerDebit(value:Double, payment:Payment, paymentDetail:PaymentDetail, obs:String) = {
        _registerDebit(value)
        CustomerAccountHistory.create
                              .customer(this)
                              .company(this.company)
                              .currentValue(this.valueInAccount)
                              .value(value)
                              .payment(payment)
                              .paymentDetail(paymentDetail)
                              .description(obs)
                              .save        
    }
    def registerDebit(value:Double, treatment:Treatment, treatmentDetail:TreatmentDetail, payment:Payment, obs:String) = {
        _registerDebit(value)
        CustomerAccountHistory.create
                              .customer(this)
                              .company(this.company)
                              .currentValue(this.valueInAccount)
                              .value(value)
                              .treatment(treatment)
                              .treatmentDetail(treatmentDetail)
                              .payment(payment)
                              .description(obs).save
    }
    override def totalDebit:Double = this.valueInAccount.is
//    def thumb = Gravatar(email,16,"R")
    def images = ImageCustomer.findAll(By(ImageCustomer.customer,this))

    def registerDelivery(productId:Long,treatmentDetailId:Long,paymentDetailId:Long, amount:Int, price:Float, efetivedate:Date){
        val deliveryNotUseds = deliveryDetailNotUsed(productId)
        if(deliveryNotUseds.size >= amount){
            for(i <- 0 to amount-1) {
                val deliveryToUse = deliveryNotUseds(i)
                if(!BusinessRulesUtil.almostEquals(deliveryToUse.price.is.toFloat, price)){
                    throw new SessionValueWrong("Valor da sessão de pacote %s errado (pc %s cx %s), verifique o valor no cadastro do cliente!".format(Product.findByKey(productId).get.name.is,
                        deliveryToUse.price, price))
                }
                deliveryToUse.treatmentDetail(treatmentDetailId).paymentDetail(paymentDetailId).used_?(true).efetivedate(efetivedate).save
            }
        }else{
            throw new PaymentDeliveryNotEnough(
                """
                %s não possui pacote(s) suficiente(s) para %s!

                Formas de pagamento marcadas como baixa de pacote só podem ser usadas para itens constantes de pacotes ativos, e não devem ser combinadas com outros tipos de forma de pagamento.

                É possível usar a opção de ignorar itens do caixa, usar a baixa de pacote e posteriormente pagar os demais itens.

                """.
                format(this.name, Product.findByKey(productId).get.name.is))
        }
    }

    def unRegisterDelivery(productId:Long, treatmentDetailId:Long, paymentDetailId:Long, amount:Int){
        val deliveryUseds = deliveryDetailUsed(productId, paymentDetailId)
        for(i <- 0 to amount-1) {
            deliveryUseds(i).treatmentDetail(0).paymentDetail(0).used_?(false).efetivedate(null).save
        }
    }

    
    def allDebits = PaymentDetail.findAll(  
                                            By(PaymentDetail.customer,this),
                                            ByList(PaymentDetail.typePayment,PaymentType.PaymentDebitsIds(this.company.is)), 
                                            BySql("(commisionnotprocessed<>0)", IHaveValidatedThisSQL("",""))
                                        )
    def allDebits(payment:Payment) =
        PaymentDetail.findAll(
                                By(PaymentDetail.customer,this),
                                ByList(PaymentDetail.typePayment,PaymentType.PaymentDebitsIds(this.company.is)),
                                BySql("(commisionnotprocessed<>0) and (payment in ( select id from payment p where p.id= paymentdetail.payment and p.datePayment<= ?) )", IHaveValidatedThisSQL("",""), payment.datePayment)
                            )

    def deliveryDetailNotUsed(productId:Long) = DeliveryDetail.findAll(
        By(DeliveryDetail.customer,this), 
        By(DeliveryDetail.used_?,false), 
        By(DeliveryDetail.product,productId),
        OrderBy(DeliveryDetail.createdAt, Ascending) // pra o antigo vir primeiro
        )
    
    def deliveryDetailUsed(productId:Long, paymentDetailId:Long) = DeliveryDetail.findAll(By(DeliveryDetail.customer,this), By(DeliveryDetail.used_?,true), By(DeliveryDetail.product,productId), By(DeliveryDetail.paymentDetail, paymentDetailId))

    def history(start:Date, end:Date) = code.service.TreatmentCalendarService.treatmentsForCalendarAsJson(this.company.obj.get, 
        this.unit.obj.get, this, start, end)

    def phones = mobilePhone + " " + phone;

    def validateDuplicatedDocument = {
        if (this.document != "") {
            if (Customer.count(By(company, AuthUtil.company.id.is), By(document, this.document), NotBy(id, this.id)) > 0) {
                throw new RuntimeException("Já existe um registro com esse cpf : %s".format(this.document.is))
            }  
        }
    }

    def validateDuplicatedBarcode = {
        if (PermissionModule.anvisa_?) {
            if (this.idForCompany != 0) {
                if (Customer.count(By(company, AuthUtil.company.id.is), By(idForCompany, this.idForCompany), NotBy(id, this.id)) > 0) {
                    throw new RuntimeException("Já existe um registro com esse código gerador de prontuário : %s".format(this.idForCompany.is))
                }  
            }
            if (this.barcode != "") {
                if (Customer.count(By(company, AuthUtil.company.id.is), By(barcode, this.barcode), NotBy(id, this.id)) > 0) {
                    throw new RuntimeException("Já existe um registro com esse prontuário : %s".format(this.barcode.is))
                }  
            }
        }
    }

    def bp_managerName : String = {
        if (bp_manager != 0) {
          val ac = Customer.findByKey(bp_manager).get
          ac.name;
        } else {
          ""
        }
    }
    def bp_indicatedbyName : String = {
        if (bp_indicatedby != 0) {
          val ac = Customer.findByKey(bp_indicatedby).get
          ac.name;
        } else {
          ""
        }
    }

    def hasUnit = unit.obj  match {
        case Full(a) => true
        case _ => false
    }

    override def save() = {
        if (!hasUnit) {
           // se não tem unidade, pega a ativa mais antiga
           // feito para o cadastramento - online 
           // depois resolver/escolher unidade
           var ac = CompanyUnit.findAll (
              By(CompanyUnit.company, this.company),
              By(CompanyUnit.status, 1),
              OrderBy (CompanyUnit.id, Ascending)
           )
           this.unit.set (ac(0).id.is);
        }
        if (this.is_person_?) {
            if (this.image.is == "" || this.image.is == "empresa.png") {
                this.image.set ("cliente.png")
            }
            if (this.imagethumb.is == "" || this.imagethumb == "empresa.png") {
                this.imagethumb.set ("cliente.png")
            }
            // forma de testar animal
            if (this.species != 0) {
                if (this.image.is == "" || this.image.is == "cliente.png") {
                    this.image.set ("animal.png")
                }
                if (this.imagethumb.is == "" || this.imagethumb == "cliente.png") {
                    this.imagethumb.set ("animal.png")
                }
            }
        } else {
            if (this.is_animal_?) {
                if (this.image.is == "" || this.image.is == "cliente.png") {
                    this.image.set ("animal.png")
                }
                if (this.imagethumb.is == "" || this.imagethumb == "cliente.png") {
                    this.imagethumb.set ("animal.png")
                }
            } else {
                if (this.image.is == "" || this.image == "cliente.png") {
                    this.image.set ("empresa.png")
                }
                if (this.imagethumb.is == "" || this.imagethumb == "cliente.png") {
                    this.imagethumb.set ("empresa.png")
                }
            }
        }
        validateDuplicatedDocument
        validateDuplicatedBarcode
        // tirei no businesspattern - pq tava gerando prontuario
/*
        voltei pra lá - provavelmente não usava o insecureSave
        e tirando de lá não gerava o mapa para unidade e convenio - 02/04/2017
        if(!this.street.is.isEmpty || this.street.is != "") {
            BusinessPatternLocationQueeue.enqueeue(BusinessPatternQueeueDto(this.id.is))
        } else {
            this.lat.set ("");
            this.lng.set ("");
        }
  */
        super.save
    }

    def toXmlTiss (business:Long) = {
       var strXml:String ="""
        <ans:dadosBeneficiario>
            <ans:numeroCarteira>"""+ this.document_offsale.is + """</ans:numeroCarteira>
            <ans:atendimentoRN>N</ans:atendimentoRN>
            <ans:nomeBeneficiario>""" + this.search_name.is.toUpperCase +
            """</ans:nomeBeneficiario>""" +
//            <ans:numeroCNS>98765544</ans:numeroCNS>
//            <ans:identificadorBeneficiario>98765544</ans:identificadorBeneficiario>
        """</ans:dadosBeneficiario>
        """
        strXml
    }

    def animals = {
        // 27 é pet de 
        // 24 indicado por
        val animals = Customer.findAllInCompany(
            BySql[Customer]("""id in (select bpr.business_pattern from bprelationship bpr
            inner join business_pattern bpa on bpa.id = bpr.business_pattern and bpa.is_animal = true
            where bpr.company = ? and bpr.bp_related = ? 
            and bpr.status = 1
            and bpr.relationship in (27,24) and bpr.bp_related is not null)""",
            IHaveValidatedThisSQL("",""),AuthUtil.company.id.is, this.id.is),
            OrderBy (Customer.createdAt, Descending)
            )
            animals
        }

}

object Customer extends Customer with BusinessPatternMeta[Customer]{
    def login(email:String, password:String, company:String):Customer = {
        val pwdMd5 = Project.md5(password)
        val customers = if (company == "") {
            findAll(By(Customer.email, email.trim.toLowerCase), 
            //By(Customer.password, Project.md5(password)
            BySql[code.model.Customer]("password = ? or password = ?",
                IHaveValidatedThisSQL("",""), password, pwdMd5)
            )
        } else {
            findAll(By(Customer.email, email.trim.toLowerCase), 
            By(Customer.company, Company.calPubCompany (company)),
            //By(Customer.password, Project.md5(password))
            BySql[code.model.Customer]("password = ? or password = ?",
                IHaveValidatedThisSQL("",""), password, pwdMd5)
            )
        }
        customers match {
            case customer::tail => {
                LogActor ! "Login email customer company " + customer.company.is.toString + " " + customer.name.is + " " +new Date().toString
                customer.lastLogin(new Date()).insecureSave
                customer
            }
            case _ => throw new RuntimeException("Cliente não existe!");
        }
    }
    lazy val SQL_GET_CUSTOMER_ACCOUNT_HISTORY = """
        select
            currentValue 
        from 
        customeraccounthistory 
        where   customer=? and 
                createdat <= ? and 
                createdat = (select max(createdat) from customeraccounthistory where createdat <= ? and customer=? limit 1)
    """

/*
    lazy val ranking_indications_query = """
            select bp.name, count (bp.id), bp.id from bprelationship br
            inner join business_pattern bp on bp.id = br.business_pattern
            where br.relationship = 25 and br.company = ?
            and br.startat between ? and ? %s
            group by bp.name, bp.id
            having count(bp.id)>0
            order by count(bp.id) desc
            limit ?
        """
*/
    lazy val indicatedby_query = """select bc.id, bc.name, br.startat, bc.id from bprelationship br
        inner join business_pattern bc on bc.id = br.bp_related
        where br.relationship = 25 and br.company =? and br.business_pattern = ? 
        order by br.startat
    """
    lazy val considerations_query = """
        select message,notify_type, date_c, id from bpconsideration where  company=? and business_pattern =?
    """
    lazy val bankaccount_query = """
        select bk.short_name as bank,agency, account, obs, bpaccount.id as id from bpaccount 
        inner join bank bk on bk.id = bpaccount.bank where  company=? and business_pattern =?
    """
    lazy val relationship_query = """
        select case when (bp.sex = 'M' or bp.sex is null or bp.sex = 'N') then re.name when bp.sex = 'F' then re.female_name end,
        br.name, date (bpr.startat), bpr.obs, bpr.id as id, bp_related, br.is_animal from bprelationship bpr
        inner join relationship re on re.id = bpr.relationship 
        inner join business_pattern br on br.id = bp_related
        inner join business_pattern bp on bp.id = business_pattern
        left  join relationshiptype rt on rt.id = re.relationshiptype
        where  bpr.company=? and business_pattern =? and bpr.status in (%s)
        order by rt.orderinreport, re.orderinreport, br.name
    """
    lazy val stakeholder_query = """
        select pr.name, sht.name, sh.startat, sh.obs, sh.approved, sh.id as id, pr.id from stakeholder sh 
        inner join project pr on pr.id = sh.project and pr.status in (%s)
        left join projectclass pc on pc.id = pr.projectclass
        left join projecttype pt on pt.id = pc.projecttype
        left join stakeholdertype sht on sht.id = sh.stakeholdertype
        where sh.company = ? and sh.business_pattern = ? and sh.status in (%s)
        and pt.class = ?
        order by sht.orderinreport asc, sh.startat desc, pr.name asc
    """
    lazy val account_query = """
            select * from (
                            select p.command,pd.value, 0.00 as Pindurado, p.datePayment,  (select t.detailTreatmentAsText from treatment t where t.payment = p.id limit 1) as texto
                            from paymentdetail pd
                            inner join payment p on(p.id=pd.payment)
                            where typepayment in( select id from paymenttype where customerregisterdebit =true)
                            and p.customer=? and p.deleted=false
                            and p.company=?
                            union
                            select 0.00, td.price,t.dateEvent,p.name 
                            from
                            treatmentdetail td
                            inner join product p on(p.id = td.product)
                            inner join treatment t on(t.id = td.treatment)
                            where product=100638 and  price is not null and t.customer=?
                            and td.company=?
                        ) a order by datePayment;        
    """    
    override def findAll(params: QueryParam[Customer]*): List[Customer] = {
        super.findAll(By(is_unit_?,false) :: By(is_offsale_?,false) :: params.toList :_*)
    }

    override def findAllInCompany(params: QueryParam[Customer]*): List[Customer] = {
        super.findAllInCompany(By(status, Customer.STATUS_OK) ::  params.toList :_*)
    }    

    override def count(params: QueryParam[Customer]*): Long = {
        super.count(By(is_unit_?,false) :: By(is_offsale_?,false) :: params.toList :_*)
    }

    override def findAllInCompany(): List[Customer] = {
        super.findAllInCompany(By(status, Customer.STATUS_OK))
    }


    override def findAll(): List[Customer] = {
        super.findAll(By(is_customer_?,true))
    }


    // rigel 18/09/2017
    // duplicados do user para login em agenda onine
    //    
    // like para emails separados por ,
    def countByEmail (email:String) = count(//Like(User.email,"%"+email+"%"))
        //By(Customer.userStatus,1),
        BySql[code.model.Customer]("(email like ? or email like ? or email like ?) ",
            IHaveValidatedThisSQL("",""), email+"%", "%,"+email+"%", "%;"+email+"%"))
    // like para emails separados por ,
    def findByEmail(email:String) = findAll(
        //By(Customer.userStatus,1),
        Like(Customer.email,"%"+email+"%"))
/*
        override def findAll(params: QueryParam[Customer]*): List[Customer] = {
            super.findAll(By(is_customer_?,false) :: params.toList :_*)

    }
*/

    // método usado pela importacao, is_person false
    // para ser mais tolerante com o nome
    def findByName(name:String):Customer = {
        Customer.findAllInCompany(
        By(Customer.name,BusinessRulesUtil.toCamelCase(name))
        ) match {
            case i::Nil => i match {
                case  ip:Customer => {
                    ip
                }
            }
            case _ => {
                var c = Customer.createInCompany
                c.name(name).is_person_?(false).save
                c
            }
        }    
    }

    val OR = " OR "
    val IS_TRUE = " = true"
    def searchCriteriaLowerCase(name:String, phone:String, email:String, showProspect:Boolean = true, showSuplier:Boolean = true, showProfessional:Boolean = true, showUser:Boolean = true, showCustomer:Boolean = true, showPerson:Boolean = true, showMember:Boolean = true, mapIcon:String = ""):List[QueryParam[Customer]] = {
        val restrictions = (showProspect, "is_prospect")::(showSuplier, "is_suplier")::(showProfessional, "is_employee")::(showUser, "is_user")::(showCustomer, "is_customer")::(showPerson, "is_person")::(showMember, "is_member")::Nil
        
        val restrictionsTrue = restrictions.filter(_._1).map(_._2+IS_TRUE)
        val restrictionsSql = if(!restrictionsTrue.isEmpty){
            restrictionsTrue.reduceLeft(_+OR+_)
        }else{
            " 1 = 1 "
        }
        val mapIconList = if(mapIcon != ""){
                List( BySql[Customer]("mapIcon = ? ", IHaveValidatedThisSQL("",""),mapIcon.toLong) )
            }else{
                Nil
            }

        List(
            Like(Customer.search_name,"%"+BusinessRulesUtil.clearString(name)+"%"),
            By(Customer.is_animal_?, false),
            BySql[Customer]("(phone like ? or mobile_phone like ? or email_alternative like ?)", IHaveValidatedThisSQL("",""),"%"+phone+"%","%"+phone+"%","%"+phone+"%"),
            BySql[Customer]("(email like ?)", IHaveValidatedThisSQL("",""),"%"+email+"%"),
            BySql[Customer]("("+restrictionsSql+")", IHaveValidatedThisSQL("",""))
        ) ::: mapIconList
    }
    def searchCustomer(company:Long,name:String,phone:String,email:String,maxResult:Int, page:Int=1):List[Customer] = {
        def maxOrderCriteria:List[QueryParam[Customer]] = List(MaxRows(maxResult), StartAt((page-1)*maxResult),OrderBy(Customer.search_name, Ascending)).asInstanceOf[List[QueryParam[Customer]]]
        findAllInCompany( (searchCriteriaLowerCase(name,phone,email) ::: maxOrderCriteria) :_* )
    }   
    def searchCustomerAsDto(company:Long,name:String,phone:String,email:String,maxResult:Int=30, page:Int=1, user:Boolean = false):List[SearchCustomerDto] = {
        def offset = (page-1)*maxResult
        def filterUser = if(user){
                " and is_employee = true "
            }else{
                ""
            }
        val sql_phone = if (!AuthUtil.user.isCustomer) {
            "'' as phone "
        } else {
            "trim (bc.mobile_phone || ' ' || bc.phone || ' ' || bc.email_alternative) as phone "
        }
        val sql = """SELECT bc.id, bc.name, 
            bc.email, 
            case 
                when (bc.is_employee = true and bc.userstatus <> 1) then 'Inativo ' 
                when (bc.is_employee = false or bc.userstatus = 1) then '' 
            end || bc.obs, """ + sql_phone + """, is_employee 
            FROM business_pattern bc
            WHERE company = ?
            AND is_animal = false 
            AND status = 1  AND search_name LIKE ? 
            AND  ( (phone like ? or mobile_phone like ?) )  
            AND  ( (email like ?) )  
            %s
            ORDER BY search_name  ASC   LIMIT ? OFFSET ?
        """
        def nameLiked = "%"+BusinessRulesUtil.clearString(name)+"%"
        def phoneLiked = "%"+phone+"%"
        def emailLiked = "%"+email+"%"
        val r = DB.performQuery(sql.format(filterUser),company::nameLiked::phoneLiked::phoneLiked::emailLiked::maxResult::offset::Nil)
        r._2.map(
                (p:List[Any])=> SearchCustomerDto(p(0).asInstanceOf[Long], p(1).asInstanceOf[String], p(2).asInstanceOf[String], p(3).asInstanceOf[String], p(4).asInstanceOf[String], p(5).asInstanceOf[Boolean])
            )

    }
    def unificCustomer(customerSource:Customer, customerDesc:Customer, bptype:String){
        if(customerSource.id.is == customerDesc.id.is && bptype == "customer"){
            throw new RuntimeException("Não é permitido unificar um cliente com ele mesmo!")
        }
        if(customerSource.id.is == customerDesc.id.is && bptype == "animal"){
            throw new RuntimeException("Não é permitido unificar um pet com ele mesmo!")
        }
        if(customerSource.id.is == customerDesc.id.is && bptype == "user") {
            throw new RuntimeException("Não é permitido unificar um profissional com ele mesmo!")
        }
        if (customerSource.is_employee_? && bptype == "customer") {
            throw new RuntimeException("Não é comum unificar um profissional com o cliente, se for mesmo o caso use o botão vermelho!")
        }

        if (!customerSource.is_employee_? && bptype == "user") {
            throw new RuntimeException("Essa opção só deve ser usada quando a origem for um profissional!")
        }

        DB.use(DefaultConnectionIdentifier) {
             conn =>
             try{      
                    UnificationCustomerHistory.createInCompany.customerSource(customerSource).customerDestination(customerDesc).save
                    val sqls = 
                    "update accountpayable set user_c=? where user_c=?;"::
                    "update bpaccount set business_pattern=? where business_pattern=?;"::
                    "update bprelationship set business_pattern=? where business_pattern=?;"::
                    "update bprelationship set bp_related=? where bp_related=?;"::
                    "update bpconsideration set business_pattern=? where business_pattern=?;"::
                    "update bpmonthly set business_pattern=? where business_pattern=?;"::
                    "update quizapplying set business_pattern=? where business_pattern=?;"::
                    "update business_pattern set bp_indicatedby=? where bp_indicatedby=?;"::
                    "update business_pattern set bp_manager=? where bp_manager=?;"::
                    "update businesspatternpayroll set business_pattern=? where business_pattern=?;"::
                    "update busyevent set user_c=? where user_c=?;"::
                    "update cheque set customer=? where customer=?;"::
                    "update commision set user_c=? where user_c=?;"::
                    "update company set customer=? where customer=?;"::
                    "update customeraccounthistory set customer=? where customer=?;"::
                    "update deliverycontrol set customer=? where customer=?;"::
                    "update deliverydetail set customer=? where customer=?;"::
                    "update imagecustomer set customer=? where customer=?;"::
                    "update inventorymovement set business_pattern=? where business_pattern=?;"::
                    "update payment set customer=? where customer=?;"::
                    "update paymentdetail set customer=? where customer=?;"::
                    "update project set bp_sponsor=? where bp_sponsor=?;"::
                    "update project set bp_manager=? where bp_manager=?;"::
                    "update stakeholder set business_pattern=? where business_pattern=?;"::
                    "update treatment set customer=? where customer=?;"::
                    "update treatment set user_c=? where user_c=?;"::
                    "update workhouer set user_c=? where user_c=?;"::
                    "update useractivity set user_c=? where user_c=?;"::
                    "update usercompanyunit set user_c=? where user_c=?;"::
                    "update userusergroup set user_c=? where user_c=?;"::
                    "update bpmonthly set user_c=? where user_c=?;"::
                    "update tdepet set animal=? where animal=?;"::
                    Nil
                    val sql1s = 
                    "update business_pattern set is_user = (select bps.is_user from business_pattern bps where bps.id = ? ) where id = ? and (is_user = false);"::
                    "update business_pattern set is_employee = (select bps.is_employee from business_pattern bps where bps.id = ? ) where id = ? and (is_employee = false);"::
                    "update business_pattern set showincalendar = (select bps.showincalendar from business_pattern bps where bps.id = ? ) where id = ? and (showincalendar = false or showincalendar is null);"::
                    "update business_pattern set grouppermission = (select bps.grouppermission from business_pattern bps where bps.id = ? ) where id = ? and (grouppermission = '' or grouppermission is null);"::
                    "update business_pattern set password = (select bps.password from business_pattern bps where bps.id = ? ) where id = ? and (password = '' or password is null);"::
                    "update business_pattern set mobile_phone = (select bps.mobile_phone from business_pattern bps where bps.id = ? ) where id = ? and (mobile_phone = '' or mobile_phone is null);"::
                    "update business_pattern set phone = (select bps.phone from business_pattern bps where bps.id = ? ) where id = ? and (phone = '' or phone is null);"::
                    "update business_pattern set email_alternative = (select bps.email_alternative from business_pattern bps where bps.id = ? ) where id = ? and (email_alternative = '' or email_alternative is null);"::
                    "update business_pattern set email = (select bps.email from business_pattern bps where bps.id = ? ) where id = ? and (email = '' or email is null);"::
                    "update business_pattern set website = (select bps.website from business_pattern bps where bps.id = ? ) where id = ? and (website = '' or website is null);"::
                    "update business_pattern set document = (select bps.document from business_pattern bps where bps.id = ? ) where id = ? and (document = '' or document is null);"::
                    "update business_pattern set document_company = (select bps.document_company from business_pattern bps where bps.id = ? ) where id = ? and (document_company = '' or document_company is null);"::
                    "update business_pattern set document_identity = (select bps.document_identity from business_pattern bps where bps.id = ? ) where id = ? and (document_identity = '' or document_identity is null);"::
                    "update business_pattern set document_professional = (select bps.document_professional from business_pattern bps where bps.id = ? ) where id = ? and (document_professional = '' or document_professional is null);"::
                    "update business_pattern set document_state = (select bps.document_state from business_pattern bps where bps.id = ? ) where id = ? and (document_state = '' or document_state is null);"::
                    "update business_pattern set document_city = (select bps.document_city from business_pattern bps where bps.id = ? ) where id = ? and (document_city = '' or document_city is null);"::
                    "update business_pattern set street = (select bps.street from business_pattern bps where bps.id = ? ) where id = ? and (street = '' or street is null);"::
                    "update business_pattern set number_c = (select bps.number_c from business_pattern bps where bps.id = ? ) where id = ? and (number_c = '' or number_c is null);"::
                    "update business_pattern set district = (select bps.district from business_pattern bps where bps.id = ? ) where id = ? and (district = '' or district is null);"::
                    "update business_pattern set complement = (select bps.complement from business_pattern bps where bps.id = ? ) where id = ? and (complement = '' or complement is null);"::
                    "update business_pattern set postal_code = (select bps.postal_code from business_pattern bps where bps.id = ? ) where id = ? and (postal_code = '' or postal_code is null);"::
                    "update business_pattern set obs = trim (obs || ' ' || (select bps.obs from business_pattern bps where bps.id = ? )) where id = ?;"::
                    "update business_pattern set obscomplement = trim (obscomplement || ' ' || (select bps.obscomplement from business_pattern bps where bps.id = ? )) where id = ?;"::
                    "update business_pattern set birthday = (select bps.birthday from business_pattern bps where bps.id = ? ) where id = ? and birthday is null;"::
                    "update business_pattern set bp_indicatedby = (select bps.bp_indicatedby from business_pattern bps where bps.id = ? ) where id = ? and bp_indicatedby is null;"::
                    "update business_pattern set instructiondegree = (select bps.instructiondegree from business_pattern bps where bps.id = ? ) where id = ? and instructiondegree is null;"::
                    "update business_pattern set occupation = (select bps.occupation from business_pattern bps where bps.id = ? ) where id = ? and occupation is null;"::
                    "update business_pattern set civilstatus = (select bps.civilstatus from business_pattern bps where bps.id = ? ) where id = ? and civilstatus is null;"::
                    Nil
                    customerSource.name (customerSource.name + " deleted").save;
                    customerDesc.valueInAccount(customerSource.valueInAccount.is+customerDesc.valueInAccount.is).save
                    sqls.foreach((sql)=>{
                        DB.runUpdate(sql,customerDesc.id.is::customerSource.id.is::Nil)
                    });
                    // aqui é invertido mesmo pq a fonte vem antes
                    sql1s.foreach((sql1)=>{
                        DB.runUpdate(sql1,customerSource.id.is::customerDesc.id.is::Nil)
                    });
                    customerSource.delete_!
                }catch{
                    case e:Exception => { conn.rollback
                         throw e
                    }
                }
        }        
    }     
}

case class SearchCustomerDto(id:Long, name:String, email:String, obs:String, phone:String, isemployee:Boolean)
