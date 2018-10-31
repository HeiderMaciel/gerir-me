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


object ContSelfUtil extends net.liftweb.common.Logger  {
	lazy val access_token = "CX0fV+oSgF35PMkmqrIxo5AkIJmEwB2zSbP88Ssace7zoT00KEY1BFAMZIsyCea1VOeB37+dlqoDu4T8fz/z+w=="
	implicit val formats = DefaultFormats // Brings in default date formats etc.
	lazy val host = "app.contself.com.br"
	lazy val apiv2 = :/(host)

	def createOS(access_token1:String,paymentDate:Date) = {
		val http = new Http
		val request = ContSelfUtil.apiv2/"ApiMobileV2/IntegracaoProcessamento_OrdemServico"
		val ret = http(request.secure <<? authParams(access_token) << mapParams(access_token,formatDate(paymentDate)) as_str)
println ("vaiiiii ================ " + ret)
		info(ret)
		val json = parse(ret)
	/*	val faceEvent = (json.extract[FacebookEventReturn])
		
		faceEvent
	*/
	}

	def authParams (token:String) = {
		Map("Authorization"->token)
	}

	def mapParams(token:String,paymentDate:String) = 
    	Map("ChavePessoa" -> "966488d7-adac-4ea9-866c-30a4b144f278",
    		"DataOS" -> paymentDate,
    		"CodigoOS" -> "1",
    		"Convenio" -> "Particular",
    		"ConvenioParticular" -> "1",
    		"CodigoPaciente" -> "3",
    		"IdentificadorPaciente" -> "55118593620",
    		"CodigoResponsavelFinanceiro" ->  "",
    		"IdentificadorResponsavelFinanceiro" -> "",
    		"ValorOS" -> "100",
    		"DataEnvioRegistro" -> paymentDate)

/*
    	Map("ChavePessoa" -> "966488d7-adac-4ea9-866c-30a4b144f278",
    		"DataOS" -> paymentDate,
    		"CodigoOS" -> "1",
    		"Convenio" -> "Particular",
    		"ConvenioParticular" -> 1,
    		"CodigoPaciente" -> "3",
    		"IdentificadorPaciente" -> "55118593620",
    		"CodigoResponsavelFinanceiro" ->  "",
    		"IdentificadorResponsavelFinanceiro" -> "",
    		"ValorOS" -> 100,
    		"DataEnvioRegistro" -> paymentDate
    		)
*/

	def formatDate(date:Date)= {
        val formatted = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
        formatted.substring(0, 22) + ":" + formatted.substring(22);
    }	
}

