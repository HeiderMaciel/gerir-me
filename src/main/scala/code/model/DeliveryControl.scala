package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import _root_.java.math.MathContext; 
import net.liftweb.common.{Box,Full,Empty}
import code.util._

class DeliveryControl extends LongKeyedMapper[DeliveryControl] 
    with PerCompany 
    with IdPK 
    with CreatedUpdated 
    with CreatedUpdatedBy 
    with WithCustomer{
    def getSingleton = DeliveryControl
    object treatment extends MappedLongForeignKey(this,Treatment)
    object product extends MappedLongForeignKey(this,Product)
    object efetivedate extends EbMappedDate(this)
    object payment extends MappedLongForeignKey(this,Payment)
    object completed_? extends MappedBoolean(this){
        override def dbColumnName = "completed"
        override def dbIndexed_? = true
    }
    object giftId extends MappedPoliteString(this,200)
    object bp_used extends MappedLongForeignKey(this, Customer) // quem usou no caso de presente

    def usedDetails = DeliveryDetail.findAll(By(DeliveryDetail.delivery,this), By(DeliveryDetail.used_?, true))
    def notDetails = DeliveryDetail.findAll(By(DeliveryDetail.delivery,this), By(DeliveryDetail.used_?, false))
    def details = DeliveryDetail.findAll(By(DeliveryDetail.delivery,this))

    def paymentType (customer:Customer, activity:Long) = {
        val product = Product.findByKey(activity.toInt).get
        val dc = DeliveryDetail.findPriceByCustomerProduct(customer,product)(0).delivery;
        val pa = dc.obj.get.payment
        val pd = PaymentDetail.findAll (
            By (PaymentDetail.payment, pa),
            OrderBy (PaymentDetail.value, Descending))(0)
        pd.typePaymentObj    
    }
}

object DeliveryControl extends DeliveryControl with LongKeyedMapperPerCompany[DeliveryControl]  with  OnlyCurrentCompany[DeliveryControl]{
    val SQL_REPORT = """
                        select 
                        cid,
                        customername,
                        pname,
                        pbomname,
                        total,
                        used,
                        un_used,
                        coalesce (price, 0),                        
                        (price * total) as total_price,
                        (price * un_used) as reamaining,
                        command,
                        idforcompany,
                        efetivedate,
                        pid,
                        pbomid
                        from
                            (                  
                                select
                                customer.id as cid,
                                customer.short_name as customername,
                                p.name as pname,
                                pbom.name as pbomname,
                                (select count(1) from deliverydetail dd where dd.delivery=dc.id and dd.product=pbom.id ) as total,
                                (select count(1) from deliverydetail dd where dd.delivery=dc.id and used=true and dd.product=pbom.id ) as used,
                                (select count(1) from deliverydetail dd where dd.delivery=dc.id and used=false and dd.product=pbom.id ) as un_used,
                                (select avg(price) from deliverydetail dd where dd.delivery=dc.id and dd.product=pbom.id ) as price,
                                pay.command as command,
                                c.idforcompany,
                                dc.efetivedate,
                                p.id as pid,
                                pbom.id as pbomid
                                from
                                deliverycontrol dc
                                inner join product p on(p.id = dc.product)
                                inner join productbom pb on(p.id = pb.product)
                                inner join product pbom on (pb.product_bom=pbom.id)
                                inner join business_pattern customer on(dc.customer=customer.id)
                                left join payment pay on( pay.id = dc.payment)
                                left join cashier c on( c.id = pay.cashier)
                                where dc.company=? and %s and %s and %s and %s and %s and %s and %s
                                and %s order by dc.efetivedate, customer.short_name, p.name, 4 
                            ) as data
                        """
    val SQL_REPORT_MINI = """
                            select 
                        cid,
                        customername,
                        pname,
                        pbomname,
                        total,
                        used,
                        un_used,
                        price/total,                        
                        (price) as total_price,
                        ((price/total) * un_used) as reamaining,
                        command,
                        idforcompany,
                        efetivedate,
                        pid,
                        pid1 -- dobrado mesmo
                        from
                        (
                            select 
                            customer.id as cid, 
                            customer.short_name as customername, 
                            p.name as pname, 
                            'Todos do pacote' as pbomname,
                            (select trunc (sum  (amount)) from treatmentdetail tdc where tdc.for_delivery = true and tdc.treatment in (select trc.id from treatment trc where trc.payment = dc.payment)) as total,
                            (select count  (1) from deliverydetail dd where dd.delivery = dc.id and dd.used = true) as used,
                            (select count  (1) from deliverydetail dd where dd.delivery = dc.id and dd.used = false) as un_used,
                            (select sum (price) from treatmentdetail tdc where tdc.treatment in (select trc.id from treatment trc where trc.payment = dc.payment)) as price,
                            pay.command as command,
                            c.idforcompany,
                            dc.efetivedate,
                            p.id as pid,
                            p.id as pid1 -- pbom.id p.id 2 vezes mesmo pq nao tem pbom aqui
                            from deliverycontrol dc 
                            inner join business_pattern customer on customer.id = dc.customer
                            inner join product p on p.id = dc.product and p.is_bom = true
                            left join treatment tr on tr.id = dc.treatment
                            left join payment pay on( pay.id = dc.payment)
                            left join cashier c on( c.id = pay.cashier)
                            where dc.company=? and %s and %s and %s and %s and %s
                        and %s order by dc.efetivedate, customer.short_name, p.name, 4 
                        ) as data """
                    //   inner join treatmentdetail td on td.treatment = tr.id 
                    //   and td.price = 0
}
