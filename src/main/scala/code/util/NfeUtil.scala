package code
package util

import code.model._
import dispatch._
import net.liftweb._
import json._
import common._
import http.js._
import JE._
import net.liftweb.util.Helpers

import java.util.Date


object NfeUtil extends net.liftweb.common.Logger  {
	lazy val access_token = "0155f419c07965d45070ab0699137a93"
	implicit val formats = DefaultFormats // Brings in default date formats etc.
	lazy val host = "inovedados.com.br"
	lazy val apiv2 = :/(host)

	def createOS(access_token1:String,paymentDate:Date) = {
        try {
		val http = new Http
		val request = NfeUtil.apiv2/"nfse/empresas/cadastrar"
        val ret = http((request.POST <<? mapParams(access_token,formatDate(paymentDate)) <:< authParams(access_token)) as_str)
		//info(ret)
		val json = parse(ret)
println (" vaiiii ============================ " + json)        
        }catch{
          case e:Exception => {
//            e.printStackTrace
println (" vaiiii ============================ MSG " + JString(e.getMessage))        
            JString(e.getMessage)
          }
          case _ =>
        }

	}

	def authParams (token:String) = {
		Map("token"-> token,
            "Content-Type" -> "application/json")
	}

	def mapParams(token:String,paymentDate:String) = 
    	Map(

             "nameCompany"     -> "Teste novo", // Nome da empresa (max 100 caracteres) 
              "codeCompany"    -> "90307069000155", // CNPJ da empresa 
              "imCompany"       -> "22345678901", // Inscrição municipal da empresa (max 11 caracteres) 
              "taxRegimeCompany"  -> "1", // Regime de tributação da empresa (1 – Microempresa Municipal; 2 – Estimativa; 3 – Sociedade de Profissionais; 4 – Cooperativa; 5 – MEI – Simples Nacional; 6 – ME EPP – Simples Nacional) 
              "simplesCompany"   -> "1" /// Optante do SIMPLES nacional? (1 - Sim, 2 - Não) 
            )

	def formatDate(date:Date)= {
        val formatted = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
        formatted.substring(0, 22) + ":" + formatted.substring(22);
    }	
}

