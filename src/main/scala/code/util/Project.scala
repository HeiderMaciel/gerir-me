package code
package util

import net.liftweb._
import http._
import java.security.MessageDigest
import java.util.Date
import code.model._
import net.liftweb.common.{Box,Full,Empty}
import scalendar._
import Month._
import Day._
import java.util.Calendar

object Project{



  val date_format_year = new java.text.SimpleDateFormat ("yyyy")
	val date_format_hour = new java.text.SimpleDateFormat ("HH:mm")
	val date_format_hourss = new java.text.SimpleDateFormat ("HH:mm:ss")
	var date_format = new java.text.SimpleDateFormat ("dd/MM/yyyy")
  var dateTime_format = new java.text.SimpleDateFormat ("dd/MM/yyyy HH:mm:ss")
	var date_format_js = new java.text.SimpleDateFormat ("yyyy-MM-dd HH:mm:ss")
	var date_format_db = new java.text.SimpleDateFormat ("yyyy-MM-dd")
	val date_format_day_and_month = new java.text.SimpleDateFormat ("dd/MM")
  val date_format_month_and_year = new java.text.SimpleDateFormat ("MM/YYYY")
	lazy val isLinuxServer = sys.props("os.name") == "Linux"

  lazy val isLocalHost = if (S.hostName.contains ("localhost") || 
    S.hostName.contains ("rigel")) {
    true } else { false }


	def md5(s: String) = {
		val m = java.security.MessageDigest.getInstance("MD5")
    	val b = s.toLowerCase.getBytes("UTF-8")
    	m.update(b, 0, b.length)
    	new java.math.BigInteger(1, m.digest()).toString(16)
	}

  def today = Project.date_format_db.parse(Project.date_format_db.format(new Date()));
  
  def tomorrow = {
    val cal = Calendar.getInstance()
    cal.setTime(new Date()); 
    cal.add(java.util.Calendar.DATE, 1);
    cal.getTime()
  }

  def nextMonth (date:Date) : Date = {
      val cal = Calendar.getInstance()
      cal.setTime(date); 
      cal.add(java.util.Calendar.MONTH, 1);
      cal.getTime()
  }

  def prevMonth (date:Date) : Date = {
      val cal = Calendar.getInstance()
      cal.setTime(date); 
      cal.add(java.util.Calendar.MONTH, -1);
      cal.getTime()
  }

  def lastDayOfMonth (date: Date) : Int = {
      31
  }

  def diffYYMM(maior1:Date, menor1:Date, str:String ) : String = {
        val dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
        val maior = new java.util.GregorianCalendar();
        maior.setTime(maior1);
        val menor = new java.util.GregorianCalendar();
        menor.setTime(menor1);
        val dif = new java.util.GregorianCalendar();
        dif.setTimeInMillis(maior.getTimeInMillis() - menor.getTimeInMillis());
  
        val li_dia_inic = menor.get(java.util.Calendar.DAY_OF_MONTH)
        val li_mes_inic = menor.get(java.util.Calendar.MONTH)
        val li_ano_inic = menor.get(java.util.Calendar.YEAR)
        val li_dia_fim  = maior.get(java.util.Calendar.DAY_OF_MONTH)
        val li_mes_fim  = maior.get(java.util.Calendar.MONTH)
        val li_ano_fim  = maior.get(java.util.Calendar.YEAR)

        var ai_anos = li_ano_fim - li_ano_inic;
        var ai_meses = 0;
        var ai_dias = 0;
        if (li_mes_inic > li_mes_fim) {
            /* ainda nao fez aniversario este ano */
            ai_anos = ai_anos - 1;
            ai_meses = 12 - li_mes_inic + li_mes_fim;
            if (li_dia_inic > li_dia_fim) {
                if (li_dia_fim < lastDayOfMonth (maior1)) {
                    /* ainda nao fez "mesversario" este mes */
                    ai_meses = ai_meses -1;
                    ai_dias = lastDayOfMonth (prevMonth(maior1)) - 
                      li_dia_inic + 1 + li_dia_fim; 
                    if (ai_meses < 0) {
                        ai_meses = 11;
                        ai_anos = ai_anos - 1;
                    }
                } else {
                    ai_dias = 0;
                }
            } else {
              ai_dias = li_dia_fim - li_dia_inic + 1;
            }
        } else {
          ai_meses = li_mes_fim - li_mes_inic;
          if (li_dia_inic > li_dia_fim) {
            if (li_dia_fim < lastDayOfMonth (maior1)) {
              /* ainda nao fez "mesversario" este mes */
              ai_meses = ai_meses -1;
              if (ai_meses < 0) {
                ai_meses = 11;
                ai_anos = ai_anos -1;
              }
              ai_dias = lastDayOfMonth (maior1) - li_dia_inic + li_dia_fim + ( 31 - lastDayOfMonth (maior1));
            } else {
              ai_dias = 0;
            }
          } else {
             ai_dias = li_dia_fim - li_dia_inic + 1;
          }
        }

  /*
        val y1 = (maior.get(java.util.Calendar.YEAR)
          -menor.get(java.util.Calendar.YEAR))

        //testar se mês já é maior

        val years = if ( y1 == 1) {
          y1 + " ano";
        } else if ((maior.get(java.util.Calendar.YEAR)
          -menor.get(java.util.Calendar.YEAR)) > 0) {
          y1 + " anos";
        } else {
          ""
        }
        val m1 = (dif.get(java.util.Calendar.MONTH))
        val months = if ( m1 == 1) {
          m1 + " mês"
        } else if (m1 > 0) {
          m1 + " meses"
        } else {
          ""
        }
        val d1 = dif.get(java.util.Calendar.DAY_OF_MONTH)
        val days = if (d1 == 1) {
          d1 + " dia";
        } else if (d1 > 0) {
          d1 + " dias";
        } else {
          ""
        }  
*/
        val years = if ( ai_anos == 1) {
          ai_anos + " ano";
        } else if ( ai_anos != 0) {
          ai_anos + " anos";
        } else {
          ""
        }

        val months = if ( ai_meses == 1) {
          ai_meses + " mês"
        } else if (ai_meses > 0) {
          ai_meses + " meses"
        } else {
          ""
        }
        
        val days = if (ai_dias == 1) {
          ai_dias + " dia";
        } else if (ai_dias > 0) {
          ai_dias + " dias";
        } else {
          ""
        }  

        var strAux = years;

        if (str != "YEARS") {
          if (strAux != "" && (months + days) != "") {
            strAux += ", "
          }
          if (months != "") {
            strAux += months
            if (days != "") {
              strAux += ", "
            }
          }
          strAux += days
        }
          
        strAux
  }

	def dateToAge(date:Date) = {
		// rigel calcular idade com data de hoje
		if(date != null)
			diffYYMM (today, date, "ALL")
		else
			""
	}

  def dtformat (date:Date, format:String) = {
    val aux_format = new java.text.SimpleDateFormat (format)
    aux_format.format(date)
  }

  def extToMonth (ext:String):String = {
    val month = Array("xxxxxx",
      "janeiro",  "fevereio",   "março",   "abril",
      "maio",     "junho",      "julho",   "agosto",
      "setembro", "outubro",    "novembro","dezembro");
    var i = 0;
    month.foreach ((m) => {
      if (m.indexOf (ext.toLowerCase) != -1) {
         return ("%02d".format(i))
      } else {
        i += 1;
      }
    })
    return ("%02d".format(0))
  }


  def monthToExt (date:Date) : String = {
    val date1 = new java.util.GregorianCalendar();
    date1.setTime(date);
    val month = date1.get(java.util.Calendar.MONTH) + 1
    if (month == 1) {
      "Janeiro"
    } else if (month == 2) {
      "Fevereiro"
    } else if (month == 3) {
      "Março"
    } else if (month == 4) {
      "Abril"
    } else if (month == 5) {
      "Maio"
    } else if (month == 6) {
      "Junho"
    } else if (month == 7) {
      "Julho"
    } else if (month == 8) {
      "Agosto"
    } else if (month == 9) {
      "Setembro"
    } else if (month == 10) {
      "Outubro"
    } else if (month == 11) {
      "Novembro"
    } else if (month == 12) {
      "Dezembro"
    } else {
      "Mês inválido!"
    }
  }
  
	def dateToExt(date:Date) = {
		// rigel calcular extenso - dd de mes de aaaa
		if(date != null) {
      val date1 = new java.util.GregorianCalendar();
      date1.setTime(date);
      val day = date1.get(java.util.Calendar.DAY_OF_MONTH)
      val year = date1.get(java.util.Calendar.YEAR)
			day + " de " + monthToExt (date) + " de " + year
		} else {
			"retornar idade"
    }
	}

	def dateToYears(date:Date) = {
		// rigel calcular só anos
		if(date != null) {
      diffYYMM (today, date, "YEARS")
    } else {
			"retornar idade"
    }
	}

	def dateToHourss(date:Date) = {
		date_format_hourss.format(date)
	}
	def dateToHours(date:Date) = {
		date_format_hour.format(date)
	}
	def dateToDb(date:Date) = {
		date_format_db.format(date)
	}

  def dateTimeToStr(date:Date) = {
    if(date != null)
      dateTime_format.format(date)
    else
      ""
  }

  def dateToYear(date:Date) = {
    date_format_year.format(date)
  }

	def dateToStr(date:Date) = {
		if(date != null)
			date_format.format(date)
		else
			""
	}

	def dateToStrOrEmpty(date:Date) = {
		if(date != null)
			dateToStr(date)
		else
			""
	}
	def dateToStrJs(date:Date) = {
		date_format_js.format(date)
	}
	def dateToMonthAndDay(date:Date) = {
		date_format_day_and_month.format(date)
	}

  def dateToMonthAndYear(date:Date) = {
    date_format_month_and_year.format(date)
  }

	def strToDateOrToday(date:String) = date match {
				case (s:String) if(s != "") => Project.strOnlyDateToDate(s)
				case _ => new Date();
	}

  def strToDateOrNull(date:String) = date match {
        case (s:String) if(s != "") => Project.strOnlyDateToDate(s)
        case _ => null;
  }

	def strToDateOrLongTimeAgo(date:String) = date match {
				case (s:String) if(s != "") => Project.strOnlyDateToDate(s)
				case _ => new Date(1l);
	}
	def strToDateBox(date:String):Box[Date] = date match {
				case (s:String) if(s != "") => Full(Project.strOnlyDateToDate(s))
				case _ => Empty
	}
	// 03/11/2015 - rigel era 07:00 passei pra 05:00 pq as academias de pilates tem aula as 7:00 tava dando erro no peiódico
	// de tentativa de inserir em atendimento pago qdo o pagamento da mensalidade era no mesmo dia de uma aula as 07:00
	// pra mim o ideal seria no caixa inserir a hora corrente e no periódico sempre criar outro treatment e não inserir um novo
	// detalhe, se é que é esse o erro ou se o treatment é outro mas a validação cerca
	def strOnlyDateToDate(data:String) = strToDate(data+" 05:00") // 07:00

	def strToDate(data:String) = {
		data match {
			case s:String if(s.length > 15 ) => {
				val df = new java.text.SimpleDateFormat ("dd/MM/yyyy HH:mm")
				df parse data
			}
			case _ => {
				null
			}
		}
	}
}

object BusinessRulesUtil{
	val EMPTY = ""
	val notCamelize = "de" :: "da" :: "do" :: "das" :: "dos" :: "a" :: "à" :: 
    "e" :: "em" :: "com" :: "no" :: "ou" :: "por" :: "para" :: Nil
	def toShortString(value:String) = {
    var len = toCamelCase(value.trim()).length
		toCamelCase(value.trim()).substring(0,scala.math.min(len, 20))
	}

	def md5(value:String) = Project.md5(value)

  // rigel 22/08/2017
  // usado nos snippets para receber info do html e converetr para
  // double - resolve erros qdo usuario usa virgula e não ponto
  // quando tenta salvar o campo vazio, e pode tratar o que mais 
  // aparecer
  def snippetToDouble (value:String) : Double = {
    if(value.trim != "") {
      var aux = "";
      aux = value.trim
      aux = aux.replaceAll(" ", "");
      aux = aux.replaceAll(",", ".");
      aux.toDouble
    } else {
      0.0
    }
  }
  def snippetToInt (value:String) : Int = {
    if(value.trim != "") {
      var aux = "";
      aux = value.trim
      aux = aux.replaceAll(" ", "");
      aux = aux.replaceAll(",", "");
      // nao sei pq isso nao funciona
      // testar println antes e depois
      //aux = aux.replaceAll(".", "");
      aux.toInt
    } else {
      0
    }
  }
  
  def snippetToLong (value:String) : Long = {
    if(value.trim != "") {
      var aux = "";
      aux = value.trim
      aux = aux.replaceAll(" ", "");
      aux = aux.replaceAll(",", "");
      // nao sei pq isso nao funciona
      // testar println antes e depois
      //aux = aux.replaceAll(".", "");
      aux.toLong
    } else {
      0
    }
  }

	def toCamelCase(value:String):String = {
		if (value != null && value.trim() != EMPTY){
			val val_camel = value split(" ") filter( _!="" ) map(_.toLowerCase) map( (current:String) => {  if( !notCamelize.contains(current)) { current.capitalize }else{ current } } ) reduceLeft(_+" "+_)
      var aux = "";
      // primeira letra sempre maiuscula antes "a Favorita"
      // agora "A Favorita"
      aux = val_camel.substring(0,1).toUpperCase + val_camel.substring(1,val_camel.length)
      // ultima letra se precedida de branco sempre maiúscula
      // antes "Studio a" agora "Studio A" 
      if (aux.slice (aux.length-2, aux.length-1) == " "){
        aux = aux.slice(0,aux.length-1) + aux.slice(aux.length-1,aux.length).toUpperCase
      }
      aux.trim;
		} else {
			EMPTY
		}
	}

  // fill at right with spaces to complete the length or limit the length
  def limitSpaces (value:String, length:Int) = {
    if (value.trim.length > length) {
      value.slice (0,length)
    } else if (value.trim.length == length) {
      value.trim
    } else {
      value.trim.padTo(length, " ").mkString
    }
  }


  def isNumeric(str:String): Boolean = str.matches("[-+]?\\d+(\\.\\d+)?")

  // fill at left with zeros to complete the length or limit the length
  def zerosLimit (value:String, len:Int) = {
    if (value.trim.length > len) {
      value.slice (0,len)
    } else if (value.trim.length == len) {
      value.trim
    } else {
      val aux = "".padTo (len - value.trim.length,"0").mkString
      aux + value.trim;
    }
  }

  // fill at left with zeros to complete the length 
  // but return the original value if > len
  def zerosNoLimit (value:String, len:Int) = {
    if (value.trim.length > len) {
      value.trim
    } else if (value.trim.length == len) {
      value.trim
    } else {
      val aux = "".padTo (len - value.trim.length,"0").mkString
      aux + value.trim;
    }
  }
  
  // usado na importacão de planilha de vendas de produtos do conta azul
  def clearStrNum (name:String):String = {
    var aux:String = ""
    aux = name.trim
    // remove o separador . ou ; no final do valor
    if (aux.substring (aux.length-1) == ".") {
      aux = aux.substring (0,aux.length-1)
    }
    if (aux.substring (aux.length-1) == ";") {
      aux = aux.substring (0,aux.length-1)
    }
    // remove o primeiro ponto se o nro tiver mais de um ex 1.300.25
    if (aux.count (_ == '.') > 1) {
      aux = aux.substring(0,aux.indexOf('.')) +
        aux.substring (aux.indexOf('.')+1,aux.length)
    }
    aux
  }
    def clearString(name:String):String = {
    	if(name != null && name != EMPTY){
    		val nameNormalized = java.text.Normalizer.normalize(name.trim(), java.text.Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    		val nameCleared = nameNormalized.replaceAll("[^a-zA-Z0-9 ]", "").split(" ").filter( _!="" )
    		if(nameCleared.isEmpty)
    			EMPTY
    		else
    			nameCleared.reduceLeft(_+" "+_).toLowerCase()
    	}else{
			EMPTY
    	}
    }

    def convertChars (name:String):String = {
        var name_aux = name;
        name_aux = name_aux.replaceAll ("ǁ", "Á")
        name_aux = name_aux.replaceAll ("Ǉǃ", "ÇÃ")
        name_aux = name_aux.replaceAll ("Ǉ", "c")
        name_aux = name_aux.replaceAll ("ǚ", "Ú")
        name_aux = name_aux.replaceAll ("ǉ", "É")
        name_aux = name_aux.replaceAll ("Ǔ", "Ó")
        name_aux = name_aux.replaceAll ("ǂ", "Â")
        name_aux = name_aux.replaceAll ("SǃO", "SÃO")
        name_aux = name_aux.replaceAll ("Ǎ", "Í")
        name_aux
    }

    def roundHalfUp(d:Double) = {
    	val big = scala.math.BigDecimal(d)
	    big.setScale(2, scala.math.BigDecimal.RoundingMode.HALF_UP).toDouble
    }

    def almostEquals (x:Float, y:Float , precision:Float = 0.001f):Boolean = {
    	(scala.math.abs(x - y) <= precision)
    }

    def sunDate(date:Date,divisionType:Int,qtd:Int):Date = {
    	Scalendar(date.getTime)+division(qtd,divisionType)
    }

    def mgrDate (dateStr : String) : Date = {
      var dt_out = dateStr;
      var dt_ret = new Date();
      if (dt_out.trim == "") {
        dt_ret = null;
        return dt_ret
      }
      // testa data com 6 posições - só dia mês por extenso
      if (dt_out.length == 6) {
         dt_out = dt_out.substring (0,3) + Project.extToMonth (dt_out.substring (3,6)) + "/1904";
      }
      // testa data com 5 posições - só dia e mês
      if (dt_out.length == 5) {
         dt_out = dt_out + "/1904";
      }
      
      // testa dia com uma posição só acrescenta 0 zero
      if (dt_out.substring (1,2)== "/") {
         dt_out = "0" + dt_out;
      }
      // testa mes com uma posição só acrescenta 0 zero
      if (dt_out.substring (4,5) == "/") {
         dt_out = dt_out.substring (0,3) + "0" + 
          dt_out.substring(3,9);
      }
      if (dt_out.trim == "") {
          dt_out = "";
      }
      // coloca no formato americano para postgres converter
      if (dt_out.substring (2,3) == "/" && dt_out.substring(5,6) == "/") {
//        dt_out = dt_out.substring (6,10) + "-" + dt_out.substring (3,5) + "-" + dt_out.substring (0,2);
      } else {
        dt_out = ""
      }
      Project.strToDateOrNull (dt_out)
    }

  	def division(qtd:Int,divisionType:Int) = {
	    if (divisionType == Recurrence.MONTHLY) {
	      qtd.month
	    } else if (divisionType == Recurrence.WEEKLY) {
	      qtd.week
	    } else {
	      qtd.year
	    }
  	}

    def dv_modulo_10 (as_numero_str:String) = {
      var lc_numero_str = as_numero_str
      var ind  = lc_numero_str.length
      var result = 0;
      var resto  = 0;
      var total  = 0;
      var  result_str = "";
      var  digito_10 = 0;

      /* Calculo do digito verificador */

      for (i <- ind to 1 by -2) {
          result = lc_numero_str.slice (i-1,i).toInt * 2;
          //result_str = string (result, "00");
          result_str = zerosLimit (result.toString,2)
          total += result_str.slice (0,1).toInt + result_str.slice (1,2).toInt
      }
  
      for (i <- (ind - 1) to 1 by -2) {
        total += lc_numero_str.slice (i-1,i).toInt
      }
        
      resto = total % 10;

      if (resto == 0) {
         digito_10 = 0;
      } else {
         digito_10 = 10 - resto;
      }

      digito_10

    }

    def dv_cpf (as_cpf : String):Boolean = {
      // Descrição : Função para consistência de valores de CPF, através de
      //             método próprio. 
      //
      // Sintax....: f_Valida_CPF (as_cpf_aux) 
      //
      // Argumentos: as_cpf_aux -> String contendo o número do CPF que se deseja
      //                       consistir.
      //
      // Retorno...: TRUE ou FALSE, dependendo se o número estiver OK ou não.
      //
      // 24/10/01 - Patrícia - Verifica CPF que passa na conferência dos dígitos, porém é inválido
      //////////////////////////////////////////////////////////////////////////

      var li_i = 1 
      var li_dig = new Array[Int](10)
      var li_som = 0;
      var li_pridig = 0;
      var li_segdig = 0;
      var as_cpf_aux = clearString(as_cpf);

      for (li_i <- 1 to 9) {
        /* Verifica CPF que passa na conferência dos dígitos, porém é inválido */
        /* Ex: 11111111111, 22222222222, 33333333333, etc */

        if (as_cpf_aux.trim == "".padTo (11,li_i.toString).mkString) {
           return false
        }
        //li_dig[li_i] = Integer(Mid(as_cpf_aux,li_i,1))
        li_dig(li_i) = as_cpf_aux.slice (li_i-1,li_i).toInt;
      }

      li_som = 10*li_dig(1) + 9*li_dig(2) + 8*li_dig(3) + 
           7*li_dig(4) + 6*li_dig(5) + 5*li_dig(6) + 
           4*li_dig(7) + 3*li_dig(8) + 2*li_dig(9);

      //li_pridig = 11 - mod(li_som,11)
      li_pridig = 11 - (li_som % 11);

      if (li_pridig > 9) {
        li_pridig = 0;
      }

      li_som = 11*li_dig(1) + 10* li_dig(2) + 9*li_dig(3) + 
           8*li_dig(4) + 7*li_dig(5) + 6*li_dig(6) +
           5*li_dig(7) + 4*li_dig(8) + 3*li_dig(9) + 
           2*li_pridig;

      //li_segdig = 11 - Mod(li_som,11)
      li_segdig = 11 - (li_som % 11);

      if (li_segdig > 9) {
        li_segdig = 0
      }
      // Testa dígitos informados contra dígitos calculados, retornando o
      // resultado da validação:
      if ((li_pridig == as_cpf_aux.slice (10-1,10).toInt) &&
            (li_segdig == as_cpf_aux.slice (11-1,11).toInt)) {
        true
      } else {
        false
      }
    }

    def dv_modulo_11 (as_numero_str:String) = {

      var lc_numero_str = as_numero_str
      var base = new Array[Int](51)
      var ind = lc_numero_str.trim.length
      var resto = 0;
      var total = 0;

      var  digito_11 = 0;

      var j = 0;

      for (i <- 1 to 50) {
        base (i) = 0;
      }

      /* Monta base para calculo do digito */

      j = 2;
      for (i <- ind to 1 by -1) {
        base (i) = j;
        j += 1;
        if (j == 10) {
          j = 2;
        }
      }

      /* Calculo do digito verificador */

      for (i <- ind to 1 by -1) {
         //total += (integer (lc_numero_str [i]) - integer ('0')) * base (i);
         total += lc_numero_str.slice(i-1,i).toInt * base (i);
      }

      //resto = mod (total,11);

      resto = total % 11

      if (resto == 0 || resto == 1) {
         // digito_11 = 0;no arte era assim - para boleto o dv nao pode ser 0
         digito_11 = 1;
      } else {
         digito_11 = 11 - resto;
      }
      //println ("vaii ================ " + lc_numero_str + " === " + digito_11)
      digito_11
    }




    lazy val END_OF_THE_WORLD:Date = Scalendar(year = 3012,
                      month = December,
                      day = 31)

    lazy val START_OF_THE_WORLD:Date = Scalendar(year = 1012,
                      month = December,
                      day = 31)
}

/**
 * Created by 022280451 on 18/05/2015.
 */
class WrittenForm(val valor: Double) {

  def humanize():String = {
    val parteInteira = valor.toInt
    val parteFracionaria = BigDecimal(valor).remainder(parteInteira)
    var centavos = ""
    if(parteFracionaria > 0) {
      var valorCentavos = parteFracionaria.toString.substring(2).toInt
      if(valorCentavos <  10){
        valorCentavos *= 10
      }
      centavos = " e " + humanize(valorCentavos.toInt) + " centavos"
    }
    humanize(parteInteira) + " reais" + centavos
  }

  private def humanize(valorEntrada: Int): String = {
    var resultado = Array[String]()

    val parteDezena = valorEntrada % 100
    if(parteDezena > 0) {
      resultado +:= humanizedDezena(parteDezena)
    }

    val parteCentena = (valorEntrada % 1000) - parteDezena
    if(parteCentena > 0) {
      if (parteCentena == 100 && resultado.length == 0) {
        resultado +:= "cem"
      } else {
        resultado +:= numbers.get(parteCentena.toInt).get
      }
    }

    val parteMilhao = valorEntrada / 1000000

    if(valorEntrada >= 1000) {
      var parteMilhar = valorEntrada / 1000
      if (parteMilhar > 1000) {
      	parteMilhar = parteMilhar - (parteMilhao * 1000)
      }
      if(parteMilhar != 1) {
        resultado +:= humanize(parteMilhar) + " mil"
      }else {
        resultado +:=  "mil"
      }
    }

    if(valorEntrada >= 1000000) {
      if(parteMilhao != 1) {
        resultado +:= humanize(parteMilhao) + " milhões"
      }else {
        resultado +:=  "milhão"
      }
    }

    resultado.mkString(" e ")
  }

  private def humanizedDezena(valor: Double): String = {
    if(valor <= 20){
      numbers.get(valor.toInt).get
    }else{
      val parteDezena = valor - (valor % 10);
      val parteUnidade = valor % 10;
      val humanizedDezena = numbers.get(parteDezena.toInt).get
      val humanizedUnidade = numbers.get(parteUnidade.toInt) match {
        case n: Some[String] => " e " + n.get
        case _ => ""
      }
      humanizedDezena + humanizedUnidade
    }
  }

  private def numbers: Map[Int, String] = {
    Map(
      1 -> "um",
      2 -> "dois",
      3 -> "tres",
      4 -> "quatro",
      5 -> "cinco",
      6 -> "seis",
      7 -> "sete",
      8 -> "oito",
      9 -> "nove",
      10 -> "dez",
      11 -> "onze",
      12 -> "doze",
      13 -> "treze",
      14 -> "quatorze",
      15 -> "quinze",
      16 -> "dezesseis",
      17 -> "dezessete",
      18 -> "dezoito",
      19 -> "dezenove",
      20 -> "vinte",
      30 -> "trinta",
      40 -> "quarenta",
      50 -> "cinquenta",
      60 -> "sessenta",
      70 -> "setenta",
      80 -> "oitenta",
      90 -> "noventa",
      100 -> "cento",
      200 -> "duzentos",
      300 -> "trezentos",
      400 -> "quatrocentos",
      500 -> "quinhentos",
      600 -> "seicentos",
      700 -> "setecentos",
      800 -> "oitocentos",
      900 -> "novecentos"
    )
  }
}

object WrittenForm {
  def apply(valor: Double) = new WrittenForm(valor)
  implicit def doubleToWrittenForm(valor:Double) = WrittenForm(valor)
  implicit def intToWrittenForm(valor:Int) = WrittenForm(valor.toDouble)
}
