package code
package model 

import net.liftweb._ 
import mapper._ 
import http._ 
import SHtml._ 
import util._ 
import code.util._
import code.service._
import java.util.Calendar
import net.liftweb.common.{Box,Full}

class AnimalPartner extends BusinessPattern[AnimalPartner]{
  def getSingleton = AnimalPartner
  override val isPersonalName_? = false
  override def isAnimalName_? = true
  override val isAnimalDefaultValue_? = true
  override def defaultValueIsCustomer = false
  override def cityDefaultValue:String = ""
  override def stateDefaultValue:String = ""
  override def cityrefDefaultValue:Long = 1821 //"Belo Horizonte"
  override def staterefDefaultValue:Long = 1 //"MG"

  override def save() = {
    if (bp_manager == 0 && bp_indicatedby == 0) {
      //info ("************************* caixa fechado alteracao")
      throw new RuntimeException("Não é permitido cadastrar Pet sem tutor e quem indicou")
    }
    if (this.image.is == "" || this.image.is == "cliente.png") {
        this.image.set ("animal.png")
    }
    if (this.imagethumb.is == "" || this.imagethumb == "cliente.png") {
        this.imagethumb.set ("animal.png")
    }
    super.save
  }

}

object AnimalPartner extends AnimalPartner with BusinessPatternMeta[AnimalPartner]

