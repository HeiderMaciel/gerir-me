// Define as partes do valor por extenso
  var extenso = [];

  extenso[1] = 'um';
  extenso[2] = 'dois';
  extenso[3] = 'tres';
  extenso[4] = 'quatro';
  extenso[5] = 'cinco';
  extenso[6] = 'seis';
  extenso[7] = 'sete';
  extenso[8] = 'oito';
  extenso[9] = 'nove';
  extenso[10] = 'dez';
  extenso[11] = 'onze';
  extenso[12] = 'doze';
  extenso[13] = 'treze';
  extenso[14] = 'quatorze';
  extenso[15] = 'quinze';
  extenso[16] = 'dezesseis';
  extenso[17] = 'dezessete';
  extenso[18] = 'dezoito';
  extenso[19] = 'dezenove';
  extenso[20] = 'vinte';
  extenso[30] = 'trinta';
  extenso[40] = 'quarenta';
  extenso[50] = 'cinquenta';
  extenso[60] = 'sessenta';
  extenso[70] = 'setenta';
  extenso[80] = 'oitenta';
  extenso[90] = 'noventa';
  extenso[100] = 'cem';
  extenso[200] = 'duzentos';
  extenso[300] = 'trezentos';
  extenso[400] = 'quatrocentos';
  extenso[500] = 'quinhentos';
  extenso[600] = 'seiscentos';
  extenso[700] = 'setecentos';
  extenso[800] = 'oitocentos';
  extenso[900] = 'novecentos';

function writtenForm(valor, moeda, moedas){
  var restante = valor;
  var retorno = '';


  if (restante == 0) {
    return ("zero " + moedas)    
  } else if (restante < 0) {
    restante = restante * -1;
  }
  
  var trilhao =   1000000000000,
    bilhao  =   1000000000,
    milhao  =   1000000;

  if(restante >= trilhao){

    var trilhoes = Math.round(restante / trilhao) ;
    restante = restante - (trilhoes * trilhao);

    if(trilhoes > 1){
      retorno += getCentena(trilhoes, "", "") + ' trilhões';
    }else{
      retorno += extenso[trilhoes] + ' trilhão';
    }

    if(restante > 0){
      retorno += ', ';
    }

  }

  if(restante >= bilhao){

    var bilhoes = Math.round(restante / bilhao) ;
    restante = restante - (bilhoes * bilhao);

    if(bilhoes > 1){
      retorno += getCentena(bilhoes, "", "") + ' bilhões';
    }else{
      retorno += extenso[bilhoes] + ' bilhão';
    }

    if(restante > 0){
      retorno += ', ';
    }

  }

  if(restante >= milhao){

    var milhoes = Math.round(restante / milhao) ;
    restante = restante - (milhoes * milhao);

    if(milhoes > 1){
      retorno += getCentena(milhoes, "", "") + ' milhões';
    }else{
      retorno += extenso[milhoes] + ' milhão';
    }

    if(restante > 0){
      retorno += ', ';
    }

  }

  if(restante >= 1000){

    var milhas = Math.round(restante / 1000)
    restante = restante - (milhas * 1000);
    retorno += getCentena(milhas, "", "") + ' mil';

    if(restante > 0){
      retorno += ', ';
    }

  }

  retorno += getCentena(restante, moeda, moedas);

  return retorno;

}

function getCentena(restante, moeda, moedas){

  var retorno = '';

  if(restante >= 100){

    var milhas = Math.round(restante / 100)
    restante = restante - (milhas * 100);
    if(milhas === 1){
      retorno += 'cento';
    }else{
      retorno += ' '+ extenso[milhas * 100];
    }

    if(restante > 0){
      retorno += ' e';
    }
  }

  if(restante >= 10){
    var teens = restante;

    if(restante == 10){
      teens = 10; 
      restante = 0;
      retorno += ' '+extenso[teens];
/*    }else if(restante <= 10){
      teens = Math.round(restante / 10.0)
      restante = restante - (teens * 10);
      retorno += ' '+extenso[teens];
*/
    }else if(restante < 20){
      teens = Math.floor (restante);
      restante = restante - (teens);
      retorno += ' ' + extenso[teens] + " " + moedas;
    }else{
      // >= 20
      teens = Math.floor(restante / 10.0)
      restante = restante - (teens * 10);
      retorno += ' '+extenso[teens * 10];
    }

    if(restante > 0){
      retorno += ' e';
    }

  }
//alert ("vaiii === " + retorno + " " + restante)
  if(restante >= 1){
    var unidades = Math.floor(restante / 1)
    restante = restante - unidades;
    retorno += ' '+extenso[unidades];

    if(restante > 0){
      retorno += " " + moedas + ' e';
    }

  }

  if (restante > 0){
    retorno += writtenForm (Math.round(restante * 100),
        "centavo", "centavos")  
  } else {
    retorno += " " + moedas
  }
  return retorno;

}
