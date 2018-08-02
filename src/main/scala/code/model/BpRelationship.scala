package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import json._
import net.liftweb.common.{Box,Full}

import java.util.Date

class BpRelationship extends Audited[BpRelationship] with KeyedMapper[Long, BpRelationship] with BaseLongKeyedMapper
     with PerCompany with IdPK with CreatedUpdated with CreatedUpdatedBy 
     with ActiveInactivable[BpRelationship] {
    def getSingleton = BpRelationship

    object business_pattern extends MappedLongForeignKey(this, Customer)
    object bp_related extends MappedLongForeignKey(this, Customer)
    object startAt extends EbMappedDate(this) {
        override def defaultValue = new Date()
    }
    object endAt extends EbMappedDate(this)
	object obs extends MappedString(this, 4000)
    object relationship extends  MappedLongForeignKey(this, Relationship)
    object reverse extends  MappedLongForeignKey(this, Relationship)
    object lastRelationship extends MappedLong(this) with LifecycleCallbacks {
        override def defaultValue = fieldOwner.relationship
        override def beforeSave() {
          super.beforeSave;
          this.set(this.defaultValue);
        }
    }

    def bpName = business_pattern.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def relatedName = bp_related.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def relationshipName = relationship.obj match {
        case Full(t) => t.name.is
        case _ => ""
    }

    def clearReverseRelationship {
        BpRelationship.findAll(By(reverse, this.id.is)).foreach( _.deleteWithouReverseRelationship)
    }
    def addReverseRelationship {
        if(relationship.obj.get.hasReverseRelationship){
            val relationshipReverse = relationship.obj.get.relationshipReverse.is
            val reverseRelation = BpRelationship.createInCompany
            .business_pattern(bp_related)
            .bp_related(business_pattern)
            .relationship(relationshipReverse)
            .reverse(this.id.is)
            .startAt(startAt)
            .endAt(endAt)
            .obs(obs)
            .saveWithouReverseRelationship
            this.reverse(reverseRelation.id.is).saveWithouReverseRelationship
        }
    }
    def deleteWithouReverseRelationship = super.delete_!
    override def delete_! = {
        clearReverseRelationship
        super.delete_!
    }
    override def save = {
        if (relationship.isEmpty) {
          throw new RuntimeException("Um tipo de relacionamento precisa ser informado")
        }
        val isNew = this.id.is match {
            case p:Long if(p > 0) => false
            case _ => true
        }
        val lr = lastRelationship.is
        val r = super.save
        if(isNew || lr != relationship.is){
            clearReverseRelationship
            addReverseRelationship
        }
        addressSync (this.business_pattern);
        r
    }
    def saveWithouReverseRelationship = {
       super.save
       this
    }
    //relationshipReverse

    def relationshipByBP (bpId:Long, bpRelated:Long, relationship:Long) = {
        BpRelationship.count(
                            By(BpRelationship.business_pattern, bpId), 
                            By(BpRelationship.bp_related, bpRelated),
                            By(BpRelationship.relationship, relationship)
                        )
    }

    def addressSync (bpId:Long) {
        val ac = Customer.findByKey(bpId).get  
        val relList = BpRelationship.findAllInCompany (
            By(BpRelationship.business_pattern, bpId),
            //  1 pai 2 filho 3 conjuge 30 - 31 resp fin
            BySql ("relationship in (1,2,3,30,31)",IHaveValidatedThisSQL("",""))
            );
        relList.foreach((rel)=>{
            var saveac1 = false;
            val ac1 = Customer.findByKey(rel.bp_related).get
            if (ac.email != "" && ac1.email == "" && !ac1.is_employee_?) {
                ac1.email (ac.email)
                saveac1 = true
            }             
            if (ac.email_alternative != "" && ac1.email_alternative == "") {
                ac1.email_alternative (ac.email_alternative)
                saveac1 = true
            }             
            if (ac.phone != "" && ac1.phone == "") {
                ac1.phone (ac.phone)
                saveac1 = true
            }             
            if (ac.mobilePhone != "" && ac1.mobilePhone == "") {
                ac1.mobilePhone (ac.mobilePhone)
                saveac1 = true
            }             
            if (ac.street != "" && ac1.street == "") {
                ac1.street (ac.street)
                saveac1 = true
            }             
            if (ac.number != "" && ac1.number == "") {
                ac1.number (ac.number)
                if (ac.complement != "" && ac1.complement == "") {
                    ac1.complement (ac.complement)
                }
                saveac1 = true
            }             
            if (ac.district != "" && ac1.district == "") {
                ac1.district (ac.district)
                saveac1 = true
            }             
            if (ac.postal_code != "" && ac1.postal_code == "") {
                ac1.postal_code (ac.postal_code)
                saveac1 = true
            }             
            if (ac.pointofreference != "" && ac1.pointofreference == "") {
                ac1.pointofreference (ac.pointofreference)
                saveac1 = true
            }             
            if (ac.city != "" && ac1.city == "") {
                ac1.city (ac.city)
                saveac1 = true
            }             
            if (ac.state != "" && ac1.state == "") {
                ac1.state (ac.state)
                saveac1 = true
            }             
            if (ac.cityName != "" && ac1.cityName == "") {
                ac1.cityRef (ac.cityRef)
                saveac1 = true
            }             
            if (ac.stateShortName != "" && ac1.stateShortName == "") {
                ac1.stateRef (ac.stateRef)
                saveac1 = true
            }             
            if (ac.lat != "" && ac1.lat == "") {
                ac1.lat (ac.lat)
                ac1.lng (ac.lng)
                saveac1 = true
            }             
            if (saveac1) {
                ac1.insecureSave
            }
        });
    }

    def addBpRelationship(bpId:Long, bpRelated:Long, relationship:Long){
        val stakes = BpRelationship.relationshipByBP(bpId, bpRelated, relationship)
        if (stakes > 0) {
        } else {
            BpRelationship
                .createInCompany
                .relationship(relationship)
                //.startAt(this.startAt)
                //.endAt()
                .business_pattern(bpId)
                .bp_related(bpRelated)
                .save
        }
    }

}

object BpRelationship extends BpRelationship with LongKeyedMapperPerCompany[BpRelationship] with OnlyActive[BpRelationship]{
    def createFromJson(json: JsonAST.JObject) = decodeFromJSON_!(json, true)
}

