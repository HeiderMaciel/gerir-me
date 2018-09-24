$(function() {
    $('#classattr').hide();
    $("#terms").termsField();
    $('#tooth').toothField(false);
    $("#offsale").offSaleField(true);
    setTimeout(function(){
      $("#terms").val($("#terms1").val()).change();
    }, 500);
    $("#bp_manager").change(); // força substituir termo ##var##
//    $("#bp_sponsor").change(); // força substituir termo ##var##
    $("#new_item").click(function() {
alert (" na var function click aqui não chega NUNCA !!!!!! ")
      var agora = getHourBr(FactoryDate.byTime(Date.toDay().getTime()));
      $("#cutomer_id_treatment").val("")
      $("#cutomer_id_treatment").change();
      var agora = getHourBr(FactoryDate.byTime(Date.toDay().getTime()));
      if (!$("#hour_start").val()) {
         $("#hour_start").val(agora);
      } else {
        // aceita hora de atendiemto existente do cliente
      }
      newItem ();
    });
    $("#product").productSearch({
      createName: false,
      iconElement: ".add-on",
      userThuch: true,
      userFieldSelector: '#user'
    });

    $(".b_add_budget").click(function() {
      return saveBudget (false);
    });

    $(".b_addnew_budget").click(function() {
      return saveBudget (true);
    });

    $("#cutomer_id_treatment").change(function(){
      var hasPetSystem = $('.has-pet-system').length > 0;
      if (hasPetSystem) {
        getAnimals();
      }
    });

    $("#activity").change(function(){
      $("#price").val("");
    });

    $("#print_message").click(function(){
        //var message_print = $('#toprint').html();
        var message_print = extract_table ("table_items", 
          true, $("#projectClassHeaderStyle").val(),//  " class='withnoborder' style='border:0'"
          $("#projectClassBodyStyle").val(), 
          $("#projectClassContentStyle").val(), 
          $("#projectClassColumns").val());
//        var message_print = extract_table ("table_items", false, "style='padding-right:20px'");
        var header = "";
        var logo = "";
        if ($('.has-pet-system').length > 0) {
          logo = "ebellepet"
          header = "ebellepet"
        } else if ($('.has-edoctus-system').length > 0) {
          logo = "edoctus"
          header = "edoctus"
        } else if ($('.has-esmile-system').length > 0) {
          logo = "esmile"
          header = "esmile"
        } else if ($('.has-ephysio-system').length > 0) {
          logo = "ephysio"
          header = "ephysio"
        } else if ($('.has-gerirme-system').length > 0) {
          header = "gerirme"
          logo = "gerirme"
        } else {
          logo = "ebelle"
          header = "ebelle"
        }

        // um trailer gerar 
        //header = '<img width="70px" style="padding-right: 10px" src="/images/logo_'+logo+'.png"/>' + header + " Gestão Integrada <br></p>"
        header = "<head> " + 
    '<link href="/css/bootstrap.css" rel="stylesheet"/>' +
    '<link href="/css/ebelle-responsive.css" rel="stylesheet"/>' +
    '<link rel="stylesheet" type="text/css" href="/skins/default.css" />' +
          " <meta charset='utf-8'> " +
          " <title>" + header + "</title> " +
          " </head> " +
/*
        '<img width="35px" style="padding-right: 10px" src="/images/logo_ftr_'+logo+'.png"/>' + 
        header + " Gestão Integrada " + 
        '<img width="70px" style="padding-right: 10px" src="http://images.vilarika.com.br/company/'+AuthUtil.company.image+'"/>' + 
*/
        " <br></p>" + $("#terms_and_conditions").html();
        header = header.replace("##itens##", message_print);
        var printWindow = window.open("", "MsgPrintWindow");
        // o close garante que se a janela já tinha sido 
        // aberta com algum conteudo anterior este de agora 
        // não seja acrescentado
        printWindow.close();
        // aqui abre de novo zerada
        printWindow = window.open("", "MsgPrintWindow");
        printWindow.document.write(header);
        setTimeout(function(){
          printWindow.print();
        }, 500);
    });


    var today = new Date();
    //var tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000);
    var start = gup("start") || getDateBr(today);
    $("#start").val(start)

    getUsersCurrentUnitCommand ();
    getAuxiliarsCurrentUnitCommand ();

    return;
})

//
// essa função deve ir para o repots.js ou util.js
//
var extract_table = function(a, asTable, headerStyle,
  bodyStyle, contentStyle, columns) {
  a = a.nodeType ? a : document.getElementById(a);
  var cellEnd = "";
  var rowEnd = "";
  var listEnd = "";
  var tableEnd = "";
  var c = "";

  if (asTable) { // table
    cellEnd = "</td><td " + contentStyle + ">";
    rowEnd = "</tr><tr " + contentStyle + "><td " + contentStyle + ">";
    listEnd = "</tr>";
    tableEnd = "</tbody></table>";
    c = "<table " + headerStyle + "><tbody " + bodyStyle + "><tr " + bodyStyle + "><td " + contentStyle + ">";
  } else { // lista simples
    cellEnd = "</span><span " + contentStyle + ">";
    rowEnd = "</span><br/><br/><span " + contentStyle + ">";
    listEnd = "</span><br/>";
    tableEnd = "</span><br/>";
    c = "<span " + headerStyle + ">";
  }

  var l, k;
  for (d = 0; d < a.rows.length; d++) {
    l = a.rows[d];
    for (b = 0; b < l.cells.length; b++) {
      k = l.cells[b];
      var c = c + (b ? cellEnd : ""),
        m = "<span style='line-height: 105%'>" + k.textContent.trim() + "</span>";
      k = m;
      var t = -1 !== m.indexOf(cellEnd) || -1 !== m.indexOf("\r") || -1 !== m.indexOf("\n");
      (m = -1 !== m.indexOf('"')) && (k = k.replace(/"/g, '""'));
      if (t || m) k = '"' + k + '"';
      c += k
    }
    if (d+1 == a.rows.length) {
      c += listEnd
    } else {
      c += rowEnd
    }
  }
  return c + tableEnd;
};

var getActivities = function() {
  DataManager.getActivities($("#user_budget").val(), function(activitysObj) {
    global_activitiesObj = activitysObj;
    $('#activity option').remove();
    var ret = "<option value=''>Selecione um serviço</option>";
    for (var i in activitysObj) {
      ret += "<option value='" + activitysObj[i].id + "'>" + activitysObj[i].name + "</option>";
    }
    $('#activity').append(ret);
    //$('#activity').change().select2('open');
  });
};

var getProjectSections = function() {
  DataManager.getProjectSections(gup("id"), function(projectSectionObj) {
    global_activitiesObj = projectSectionObj;
    $('#projectSection option').remove();
    var ret = ""; //"<option value=''>Selecione uma seção</option>";
    for (var i in projectSectionObj) {
      ret += "<option value='" + projectSectionObj[i].id + "'>" + projectSectionObj[i].name + "</option>";
    }
    $('#projectSection').append(ret);
    //$('#activity').change().select2('open');
  });
};

var getAuxiliarsCurrentUnitCommand = function() {
  var url;
  url = "/cash/getAuxiliarsCurrentUnitCommand";
  return $.get(url, function(t) {
    var obj, _i, _len, _results;
    eval("userObj = " + t);
    $('#auxiliar, #auxiliar').append("<option value='0'>Selecione um auxiliar</option>");
    _results = [];
    for (_i = 0, _len = userObj.length; _i < _len; _i++) {
      obj = userObj[_i];
      _results.push($('#auxiliar, #auxiliar').append("<option value='" + obj.id + "'>" + 
        obj.name + " " + obj.idForCompany + "</option>"));
    }
    return _results;
  });
};


var getUsersCurrentUnitCommand = function() {
  var url;
  url = "/cash/getUsersCurrentUnitCommand";
  return $.get(url, function(t) {
    var obj, _i, _len, _results;
    eval("userObj = " + t);
    $('#user_budget, #user_budget').append("<option value='0'>Selecione um profissional</option>");
    _results = [];
    for (_i = 0, _len = userObj.length; _i < _len; _i++) {
      obj = userObj[_i];
      _results.push($('#user_budget, #user_budget').append("<option value='" + obj.id + "'>" + 
        obj.name + " " + obj.idForCompany + "</option>"));
    }
    return _results;
  });
};

var newItem = function() {
  if (!gup("id") || gup("id") == "") {
    return alert('É preciso salvar o orçamento antes de inserir itens ao mesmo');
  }
  var ps = $("#projectSection").val()
  getProjectSections ();
  if (ps) {
    $("#projectSection").val(ps).change();
  }
  var ac = $("#activity").val()
  getActivities ();    
  if (ac) {
    $("#activity").val(ac);//.change(); change só qdo alterar mesmo - pq zera o valor
  }
  return $("#budget_modal").modal({
    "show": true,
    "keyboard": true,
    "backdrop": true
  });
}

var callApiLock = false;

var saveBudget = function(newItem) {
  var end, obs, start, user, password, auxiliar, animal, 
  price, amount, tooth, offsale
  projectSection;
  start = $("#start").val() + " " + $("#hour_start").val();
  // aqui é start mesmo pq dt fim não é informada
  end = $("#start").val() + " " + $("#hour_end").val();
  user = $("#user_budget").val();
  password = ""//$("#password").val();
  auxiliar = $("#auxiliar").val();
  animal = $("#animal").val() || 0 ;
  offsale = $("#offsale").val() || 0;
  customer = $("#cutomer_id_treatment").val();
  price = $("#price").val();
  amount = $("#amount").val();
  obs = $("#obs").val();
  activity = $("#activity").val();
  product = $("#product").val();
  tooth = $("#tooth").val();
  projectSection = $("#projectSection").val();
  var valid = false;

  if ((!$("#cutomer_id_treatment").val()) || (parseFloat($("#cutomer_id_treatment").val()) == 0)) {
    return alert('Um cliente precisa ser selecionado');
  }
  if (($("#activity").val()) && (parseFloat($("#activity").val()) != 0)) {
    if (!$("#start").val() || !$("#hour_start").val() || !user) {
      return alert('Verifique os dados obrigatórios: hora início!');
    } else {
      valid = true;
    } 
    if ((!$("#user_budget").val()) || (parseFloat($("#user_budget").val()) == 0)) {
      return alert('Um profissional precisa ser selecionado!');
    }
  } else if (($("#product").val()) && (parseFloat($("#product").val()) != 0)) {
    // produto nao requer profissional
    valid = true;
  } else {
    return alert('Um serviço ou produto precisa ser selecionado');
  }

  if ((!$("#projectSection").val()) || (parseFloat($("#projectSection").val()) == 0)) {
    return alert('Uma seção precisa ser selecionado!');
  }

  if (!callApiLock) {
    callApiLock = true
    if (valid) {
      return $.post("/command/add_command", {
        "start": start,
        "end": end,
        "user": user,
        "auxiliar": auxiliar,
        "animal": animal,
        "offsale": offsale,
        "password": password,
        "customer": customer,
        "obs": obs,
        "price": price,
        "amount": amount,
        "activity": activity,
        "status": "9", // budget
        "project": gup("id"), // budget
        "projectSection": projectSection, // budget section
        "product": product,
        "tooth":tooth
      }, function(results) {
        if(results === 1 || results == "1"){
          alert("Cadastrado com sucesso");
          $("#budget_modal").modal({
            "hide": true
          });
          // tá lá no events.js
          updateReportItems();
        }else{
          alert(eval(results));
        }
        callApiLock = false
        //return Manager.getListFromServer();
        if (newItem) {
          new_detail ();
        }
        //$("#send").click();
      });
    }
  } else {
    alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
  }
};

var new_detail = function () {
//alert (" na var new detail")
    $("#cutomer_id_treatment").val($("#bp_sponsor").val())
    $("#cutomer_id_treatment").change();
//    $("#hour_start").val(start_hour);
    var agora = getHourBr(FactoryDate.byTime(Date.toDay().getTime()));
    if (!$("#hour_start").val()) {
       $("#hour_start").val(agora);
    } else {
      // aceita hora de atendiemto existente do cliente
    }
    $("#user_budget").val($("#bp_manager").val())
    $("#user_budget").change();
    $("#new_item").click();
    newItem ();
};

var del_detail = function (tdId) {
    return $.post("/command/del_detail", {
      "tdid": tdId
    }, function(results) {
      if(results === 1 || results == "1"){
        alert("Serviço excluído com sucesso");
      }else{
        alert(eval(results));
      }
      //return Manager.getListFromServer();
      $("#send").click();
    });
};

