INSERT INTO customeraccounthistory(
            value, id, 
            paymentdetail,
            payment,
            customer, 
            company,
            description,
            updatedby,
            createdby, 
            updatedat, 
            createdat, 
            currentvalue)
select 
pd.value*-1 as value, 
nextval('customeraccounthistory_id_seq'), 
pd.id as paymentdetail,
pd.payment, 
p.customer,
p.company, 
'Adicionando valor a conta cliente'  as obs, 
1,
1,
p.createdat,
p.createdat,
(
		select sum(v-d) from (
		select max(pd.value) as d , 0.00 as v, max(t.dateEvent) as dateEvent, max(t.detailTreatmentAsText)
		from paymentdetail pd
		inner join payment p on(p.id=pd.payment)
		inner join treatment t on(t.payment = p.id)
		where typepayment=84
		and p.customer=max(t_p.customer) and p.deleted=false group by p.id
		union
		select 0.00, td.price,t.dateEvent,p.name 
		from
		treatmentdetail td
		inner join product p on(p.id = td.product)
		inner join treatment t on(t.id = td.treatment)
		where product=100638 and  price is not null and t.customer=max(t_p.customer)
		) a where dateEvent <= max(t_p.dateEvent)
)
from paymentdetail pd
inner join payment p on(p.id=pd.payment)
inner join treatment t_p on(t_p.payment = p.id)
where typepayment in (select id from paymenttype  where customerregisterdebit=true) and p.deleted=false
and p.company=23 and p.customer=18083 group by pd.id,p.customer, p.company,p.createdat 
order by p.createdat;