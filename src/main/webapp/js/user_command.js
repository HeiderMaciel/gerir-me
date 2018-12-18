(function() {
  var Manager;

  Manager = (function() {

    function Manager() {}

Manager.notifyMe = function() {

/**
 * @function _guid
 * @description Creates GUID for user based on several different browser variables
 * It will never be RFC4122 compliant but it is robust
 * @returns {Number}
 * @private
 */
var guid = function() {

// 45011866453736710357880537363768136624

    var nav = window.navigator;
    var screen = window.screen;
    var guid = nav.mimeTypes.length;
    guid += nav.userAgent.replace(/\D+/g, '');
    guid += nav.plugins.length;
    guid += screen.height || '';
    guid += screen.width || '';
    guid += screen.pixelDepth || '';

    return guid;
};

var options = {
  body: 'Este é um teste de notificação ',
  silent: false,
  vibrate: [200, 100, 200]
}
//  alert ("vaiii notifications " + Notification.permission + " " + guid ())
  // Verifica se o browser suporta notificações
  if (!("Notification" in window)) {
    alert("Este browser não suporta notificações de Desktop");
  }

  // Let's check whether notification permissions have already been granted
  else if (Notification.permission === "granted") {
    // If it's okay let's create a notification
    var notification = new Notification("Hi there! granted", options);
  }

  // Otherwise, we need to ask the user for permission
  else if (Notification.permission !== 'denied') {
    Notification.requestPermission(function (permission) {
      // If the user accepts, let's create a notification
      if (permission === "granted") {
        var notification = new Notification("Hi there! not denied", options);
      }
    });
  } else {
    alert ("denied")
  }

  // At last, if the user has denied notifications, and you 
  // want to be respectful there is no need to bother them any more.
};

    Manager.remove = function(id) {
      var url;
      url = "/calendar/remove_freebusy/" + id;
      return $.get(url, function() {
        alert("Excluído com sucesso!");
        return Manager.getListFromServer();
      });
    };

    Manager.getUsersCurrentUnitCommand = function() {
      var url;
      url = "/cash/getUsersCurrentUnitCommand";
      return $.get(url, function(t) {
        var obj, _i, _len, _results;
        eval("userObj = " + t);
        if (userObj.length > 1) {
          $('#user, #user').append("<option value='0'>Selecione um profissional</option>");
        }
        _results = [];
        for (_i = 0, _len = userObj.length; _i < _len; _i++) {
          obj = userObj[_i];
          _results.push($('#user, #user').append("<option value='" + obj.id + "'>" + 
            obj.name + " " + obj.idForCompany + "</option>"));
        }
        return _results;
      });
    };

    Manager.getAuxiliarsCurrentUnitCommand = function() {
      var url;
      url = "/cash/getAuxiliarsCurrentUnitCommand";
      return $.get(url, function(t) {
        var obj, _i, _len, _results;
        eval("userObj = " + t);
        $('#auxiliar, #auxiliar_edit').append("<option value='0'>Selecione um auxiliar</option>");
        _results = [];
        for (_i = 0, _len = userObj.length; _i < _len; _i++) {
          obj = userObj[_i];
          _results.push($('#auxiliar, #auxiliar_edit').append("<option value='" + obj.id + "'>" + 
            obj.name + " " + obj.idForCompany + "</option>"));
        }
        return _results;
      });
    };

    Manager.getCustomers = function() {
      var url;
      dataaux = $("#day").val();
      url = "/command/getCustomers" + "?day=" + dataaux;
      return $.get(url, function(t) {
        $('#customer option').remove();
        var obj, _i, _len, _results;
        eval("customerObj = " + t);
        $('#customer, #customer').append("<option value=''>Selecione um cliente</option>");
        _results = [];
        for (_i = 0, _len = customerObj.length; _i < _len; _i++) {
          obj = customerObj[_i];
          _results.push($('#customer, #customer').append("<option value='" + obj.id + "'>" + obj.name + "</option>"));
        }
        return _results;
      });
    };
    Manager.getActivities = function() {
      DataManager.getActivities($("#user").val(), function(activitysObj) {
        global_activitiesObj = activitysObj;
        $('#activity option').remove();
        var ret = "<option value=''>Selecione um serviço</option>";
        for (var i in activitysObj) {
          ret += "<option value='" + activitysObj[i].id + "'>" + activitysObj[i].name + "</option>";
        }
        $('#activity, #activity_edit').append(ret);
        if (($("#customer").val()) && (parseFloat($("#customer").val()) != 0)) {
          // só abre select de atividades se já tem serviço
          $('#activity').change().select2('open');
        }
      });
    };

    Manager.user = function() {
      var user;
      user = $("#user").val();
      if (!user || user == "0") {
        user = AuthUtil.user.id
      }
      $("#user").val(user).change();
      return user
    };

    Manager.getListFromServer = function() {
      var password;
      password = $("#password").val();
//      if (document.location.href.indexOf("edoctus") != -1) {
      var hasEdoctusSystem = $('.has-edoctus-system').length > 0;
      if (hasEdoctusSystem) {
        if ($("#password").val() != undefined) {
          // trazer aqui passwd do user e setar tb user
          if (!$("#password").val()) {
            $("#password").val("edoctus")
            password = "edoctus"
            //$("#user").val("0")
          }
        } else {
          password = "edoctus"
        }
      } else {
        if ($("#password").val() != undefined) {
          if (!$("#password").val()) {
            return alert('Informe senha do profissional');
          }
        }
      }
      var hasAuxiliarModule = $('.has-auxiliar-module').length > 0;
      var hasOffSaleModule = $('.has-offsale-module').length > 0;
      var hasNotMedical = $('.has-not-medical').length > 0;
      var hasPetSystem = $('.has-pet-system').length > 0;
      var hasEsmileSystem = $('.has-esmile-system').length > 0;

      var print = $("input[@id=print_command]:checked").length == 1;

      var fields = [];
      //fields[5] = "dateTime"; a data já tá formatada hh24:mi no sql
      if (hasNotMedical || print) { // chegou
        fields[1] = "none";
      }
      fields[2] = {
        type: "format",
        decode: function(name, row) {
          return "<a href='/customer/edit?id=" + row[13] + 
          "' target='_customer_maste'>" + name + "</a>";
        }
      }
      if (!hasAuxiliarModule) {
        fields[3] = "none";
      } else {
        fields[3] = {
          type: "format",
          decode: function(name, row) {
            return "<a href='/user/edit?id=" + row[16] + 
            "' target='_user_maste'>" + name + "</a>";
          }
        }
      }
      if (!hasPetSystem) {
        fields[4] = "none";
      } else {
        fields[4] = {
          type: "format",
          decode: function(name, row) {
            return "<a href='/animal/edit_animal?id=" + row[17] + 
            "' target='_animal_maste'>" + name + "</a>";
          }
        }
      }
      if (!hasEsmileSystem) { // tooth
        fields[6] = "none";
      } 
      if (!hasOffSaleModule) { // offsale
        fields[7] = "none";
      } 
      if (!hasNotMedical) { // qtde
        fields[8] = "none";
      }
      if (!hasNotMedical) { // valor
        fields[9] = "none";
      }
      fields [10] = {
        type : "format",
        decode: function(name, row) {
          trStatus = 10;
          trStatus2 = 18;
          if (print) {
            return trStatusdecode (name,row, true)
          } else {
            return trStatusdecode (name,row, false)
          }
        }
      };
      fields[11] = { // obs
        type : "format",
        decode: function(name, row) {
          return "<p style='line-height: 120%'>" + 
              row[11] + "<p/>"
        }
      }

      if (print){
        $("#tharrived").hide();
        $("#thwait").hide();
        $("#thaction").hide();
      }

      if ((hasNotMedical) || (print)) { // espera
        fields[12] = "none";
      }

      if (print) {
        fields[13] = "none";
      } else {
        var hasEdoctusSystem = $('.has-edoctus-system').length > 0;
        var hasEphysioSystem = $('.has-ephysio-system').length > 0;
        if (hasEdoctusSystem || hasEphysioSystem) {
          fields[13] = {
            type : "format",
            decode: function(name, row) {
              var strAux = "";
    //          if ((document.location.href.indexOf("edoctus") != -1) ||
    //          (document.location.href.indexOf("ephysio") != -1)) {
                strAux = '<a title="Novo prontuário" href="/quiz/quizapply?business_pattern=' + 
                row[13] + '&quiz=' + row[15] + 
                '"> <img width="24" src="/images/add.png"/></a>' +
                    '<a title="Prontuário" href="/records/edit_patient?id=' + 
                    row[13] + '"> <img width="24" src="/images/records.png"/></a>'
              return strAux 
               // agora ir para cadastro é link no nome
               //+ <a title="Cadastro" href="/customer/edit?id=' + row[11] + '"> <img width="24" src="/images/customers.png"/></a>'
            }
          }
        } else {
          fields[13] = "none";
        }
      }
      if (print) {
        fields[14] = "none"
      } else {
        fields[14] = {
          type : "format",
          decode: function(name, row) {
            return "" +
            "<a class='btn primary btdim1' onclick='Manager.new_detail(" + 
            row[13] +',"' +row[0]+ '"' + ")'" + 
            " title='Inserir novo serviço para este cliente/paciente' target=''>Novo</a> " +
            "<a class='btn btdim1' onclick='Manager.edit_detail(" + 
            row[14] + ',' + //td.id
            '"' +row[2]+ '",' + // customer name 
            '"' +row[0] + '",' + // start 
            '"' +row[22] + '",' + // end
            '"' +row[23] +'",' + // obs
            row[16] + ',' + // aux.id
            row[20] + ',' + // activity
            row[21] + ',' + // product
            row[17] + ',' + // animal
            '"' +row[6] +'",' + // tooth
            row[9] + ',' + // price
            row[8] + ',' + // amount
            row[19] + '' + // offsale
            ")'" + 
            " title='Editar o serviço deste cliente/paciente' target=''>Editar</a> " +
            "<a class='btn danger btdim1' onclick='Manager.del_detail(" + 
            row[14] +")'  target=''>Excluir</a>"
            //      "<a class='btn primary' onclick='Manager.new_fit(" +row[0].replace (':','.') +")' title='Inserir novo serviço neste mesmo horário' target=''>Encaixar</a> " +
          }
        };
      }
      fields[15] = "none" // questionario/prontuário default
      fields[16] = "none" // id assistente
      fields[17] = "none" // id animal
      fields[18] = "none" // tr.status2
      fields[19] = "none" // offsale id
      fields[20] = "none" // td.activity
      fields[21] = "none" // td.product
      fields[22] = "none" // tr.end_c
      dataaux = $("#day").val();
      renderReport("/command/usersales" + 
        "?user=" + (Manager.user())+
        "&password=" + password +
        "&day=" + dataaux, 
        fields, {
        project: gup('id')
      }, "#grid");

      var fields1 = [];
      fields1[1] = {
        type: "format",
        decode: function(name, row) {
          return "<a href='/customer/edit?id=" + row[8] + 
          "' target='_customer_maste'>" + name + "</a>";
        }
      }
      fields1[2] = {
        type: "format",
        decode: function(name, row) {
          return "<a href='/user/edit?id=" + row[9] + 
          "' target='_user_maste'>" + name + "</a>";
        }
      }
      fields1[3] = {
        type: "format",
        decode: function(name, row) {
          return "<a href='/user/edit?id=" + row[10] + 
          "' target='_user_maste'>" + name + "</a>";
        }
      }
      if (!hasPetSystem) {
        fields1[4] = "none";
      } else {
        fields1[4] = {
          type: "format",
          decode: function(name, row) {
            return "<a href='/animal/edit_animal?id=" + row[11] + 
            "' target='_animal_maste'>" + name + "</a>";
          }
        }
      }
      if (!hasEsmileSystem) {
        fields1[6] = "none";
      } 


      fields1[7] = {
        type : "format",
        decode: function(name, row) {
          // porque a 1a opção nao funciona
          // por que a função no Manager não funciona
//          return "<a class='btn success' href='/command/setaux?user=" + (Manager.user())+ "&tdid="+row[5]+"'>XML</a>"
//          return "<a class='btn success' onclick='Manager.set_auxiliar(" + (Manager.user()) + "," +row[5]+")'  target='_tissxml_maste'>XML1</a>"
          return "<a class='btn primary btdim1' onclick='Manager.new_detail("+
          row[8]+ ',"' +row[0]+ '"' + ")'" + 
          " title='Inserir novo serviço para este cliente/paciente' target=''>Novo</a> " +
          "<a class='btn success btdim1' title='Gravá-lo como assistente desse atendimento' onclick='Manager.set_auxiliar(" + (Manager.user()) + "," +row[7]+")'  target=''>Gravar</a> " +
          "<a class='btn danger btdim1' title='Excluí-lo como assistente desse atendimento' onclick='Manager.del_auxiliar(" + (Manager.user()) + "," +row[7]+")'  target=''>Excluir</a>"
        }
      };
      fields1[8] = "none"
      fields1[9] = "none"
      fields1[10] = "none"
      fields1[11] = "none"

      dataaux = $("#day").val();
      renderReport("/command/treataux" + 
        "?user=" + (Manager.user())+
        "&password=" + password +
        "&day=" + dataaux, 
        fields1, {
        project: gup('id')
      }, "#grid1");

    };

    Manager.new_fit = function (hour_start) {
      //alert (hour_start)
      $("#hour_start").val(hour_start);
      Manager.new ();
    };

    Manager.new_detail = function (customerId, start_hour) {
        $("#customer").val(customerId)
        $("#hour_start").val(start_hour)
        $("#new").click();
    };

    Manager.del_detail = function (tdId) {
        var message = "Tem certeza que deseja excluir o atendimento?";
        if(confirm(message)){
          return $.post("/command/del_detail", {
            "tdid": tdId
          }, function(results) {
            if(results === 1 || results == "1"){
              alert("Serviço excluído com sucesso");
            }else{
              alert(eval(results));
            }
            return Manager.getListFromServer();
          });
        }
    };

    Manager.set_auxiliar = function (userId, tdId) {
        //
        // usado tambem na Agenda na comanda e no caixa
        //
        return $.post("/command/setaux", {
          "user": userId,
          "tdid": tdId,
          "command": "1"
        }, function(results) {
          if(results === 1 || results == "1"){
            alert("Assistente cadastrado com sucesso");
          }else{
            alert(eval(results));
          }
          return Manager.getListFromServer();
        });
    };
  
    Manager.del_auxiliar = function (userId, tdId) {
      return $.post("/command/delaux", {
        "user": userId,
        "tdid": tdId
      }, function(results) {
        if(results === 1 || results == "1"){
          alert("Assistente excluído com sucesso");
        }else{
          alert(eval(results));
        }
        return Manager.getListFromServer();
      });
    };

    Manager.getCustomerbyCommand = function () {
      dataaux = $("#day").val();
      command = $("#command").val();
      return $.post("/command/getcustomer", {
        "command": command,
        "day": dataaux
      }, function(results) {
        eval("var resultCommandCall = " + results);
        if (resultCommandCall.status == 'success') {
          treatments = resultCommandCall.data;
          $("#customer").val(treatments[0].customerId).change();
          //alert ("vaiii ==== " + treatments[0].customerId)
        } else {
          alert(resultCommandCall.message);
        }
      });
    };

    var callApiLock = false;

    Manager.save = function() {
      var end, obs, start, user, password, auxiliar, 
      animal, tooth, price, amount, offsale;
      start = $("#start").val() + " " + $("#hour_start").val();
      // aqui é start mesmo pq dt fim não é informada
      end = $("#start").val() + " " + $("#hour_end").val();
      user = $("#user").val();
      password = $("#password").val();
      auxiliar = $("#auxiliar").val();
      animal = $("#animal").val() || 0 ;
      offsale = $("#offsale").val() || 0;
      customer = $("#customer").val();
      price = $("#price").val();
      amount = $("#amount").val();
      obs = $("#obs").val();
      activity = $("#activity").val();
      product = $("#product").val();
      tooth = $("#tooth").val();
      var valid = false;
      if ((!$("#customer").val()) || (parseFloat($("#customer").val()) == 0)) {
        return alert('Um cliente precisa ser selecionado');
      }
      if (($("#activity").val()) && (parseFloat($("#activity").val()) != 0)) {
        if (!$("#start").val() || !$("#hour_start").val() || !user) {
          return alert('Verifique os dados obrigatórios: hora início!');
        } else {
          valid = true;
        } 
      } else {
        if (($("#product").val()) && (parseFloat($("#product").val()) != 0)) {
          if (!$("#hour_start").val()) {
            start = $("#start").val() + " 05:00"
            end = $("#start").val() + " 05:15"
          }
          valid = true;
        } else {
          return alert('Um serviço ou produto precisa ser selecionado');
        }
      }

      if ($("#password").val() != undefined) {
        if (!$("#password").val()) {
           return alert('Informe senha do profissional');
        } else {
          valid = true;
        }
      } else {
        valid = true;
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
            "tooth": tooth,
            "offsale": offsale,
            "password": password,
            "customer": customer,
            "obs": obs,
            "price": price,
            "amount": amount,
            "activity": activity,
            "product": product
          }, function(results) {
            if(results === 1 || results == "1"){
              alert("Cadastrado com sucesso");
              $("#command_modal").modal({
                "hide": true
              });
            }else{
              alert(eval(results));
            }
            callApiLock = false
            return Manager.getListFromServer();
          });
        }
      } else {
        alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
      }
    };

    Manager.update = function() {
      var tdItd, end, obs, start, user, password, auxiliar, 
      animal, tooth, price, amount, offsale;
      tdId = $("#tdid_edit").val();
      start = $("#start").val() + " " + $("#hour_start_edit").val();
      // aqui é start mesmo pq dt fim não é informada
      end = $("#start").val() + " " + $("#hour_end_edit").val();
      user = $("#user").val();
      password = $("#password").val();
      auxiliar = $("#auxiliar_edit").val();
      animal = $("#animal_edit").val() || 0 ;
      offsale = $("#offsale_edit").val() || 0;
      // nunca troca o cliente
      //customer = $("#customer_edit").val();
      price = $("#price_edit").val();
      amount = $("#amount_edit").val();
      obs = $("#obs_edit").val();
      activity = $("#activity_edit").val() || "0";
      product = $("#product_edit").val() || "0";
      tooth = $("#tooth_edit").val();
      var valid = false;
      if (($("#activity_edit").val()) && (parseFloat($("#activity_edit").val()) != 0)) {
        if (!$("#start").val() || !$("#hour_start_edit").val() || !user) {
          return alert('Verifique os dados obrigatórios: hora início!');
        } else {
          valid = true;
        } 
      } else {
        if (($("#product_edit").val()) && (parseFloat($("#product_edit").val()) != 0)) {
          if (!$("#hour_start_edit").val()) {
            start = $("#start").val() + " 05:00"
            end = $("#start").val() + " 05:15"
          }
          valid = true;
        } else {
          return alert('Um serviço ou produto precisa ser selecionado');
        }
      }

      // acho que não precisa
      if ($("#password").val() != undefined) {
        if (!$("#password").val()) {
           return alert('Informe senha do profissional');
        } else {
          valid = true;
        }
      } else {
        valid = true;
      }
  
      if (!callApiLock) {
        callApiLock = true
        if (valid) {
          if (!activity) {
            activity = "0"
          }
          return $.post("/command/upd_command", {
            "tdId": tdId,
            "start": start,
            "end": end,
            "user": user,
            "auxiliar": auxiliar,
            "animal": animal,
            "tooth": tooth,
            "offsale": offsale,
            "password": password,
            // nunca troca o cliente
            //"customer": customer,
            "obs": obs,
            "price": price,
            "amount": amount,
            "activity": activity,
            "product": product
          }, function(results) {
            if(results === 1 || results == "1"){
              alert("Atualizado com sucesso");
              $("#edit_command_modal").modal({
                "hide": true
              });
            }else{
              alert(eval(results));
            }
            callApiLock = false
            return Manager.getListFromServer();
          });
        }
      } else {
        alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
      }
    };

    Manager.new = function() {
      if ((!$("#user").val()) || (parseFloat($("#user").val()) == 0)) {
        return alert('Um profissional precisa ser selecionado!');
      }
      if ($("#password").val() != undefined) {
        if (!$("#password").val()) {
          return alert('Informe senha do profissional!');
        } 
      }
      Manager.getActivities();

      // para exibir um possivelcliente setado ao clicar na linha
      $("#customer").change();

      return $("#command_modal").modal({
        "show": true,
        "keyboard": true,
        "backdrop": true
      });
    };

    Manager.edit_detail = function(tdid, customer, start_hour, 
      end_hour, obs, auxiliar, activity, product,
      animal, tooth, price, amount, offsale) {
      $("#tdid_edit").val(tdid);
      $("#hour_start_edit").val(start_hour);
      $("#hour_end_edit").val(end_hour);
      $("#auxiliar_edit").val(auxiliar || 0);
      $("#animal_edit").val(animal || 0);
      $("#offsale_edit").val(offsale || 0) 
      $("#customer_edit").val(customer);
      $("#price_edit").val(price || 0);
      $("#amount_edit").val(amount);
      $("#obs_edit").val(obs);
      $("#activity_edit").val(activity);
      $("#product_edit").val(product);
      $("#tooth_edit").val(tooth);

//      Manager.getActivities();

      $("#product_edit").change();
      $("#activity_edit").change();
      $("#offsale_edit").change();
      $("#auxiliar_edit").change();

      return $("#edit_command_modal").modal({
        "show": true,
        "keyboard": true,
        "backdrop": true
      });
    };

    return Manager;

  })();

  $(function() {
    $('#tooth').toothField(true);
    $("#offsale").offSaleField(true);
    $('#tooth_edit').toothField(true);
    $("#offsale_edit").offSaleField(true);
    $("#forget").click(function() {
      $("#password").val("1234").change(); 
      $("#user").val("0");
      $("#user").change();
      $("#grid tbody").html("");
      // dava erro agora que o reports.js dá msg no caso de exceção
      //return Manager.getListFromServer();
    });

    $("#customer").change(function(){
      var hasPetSystem = $('.has-pet-system').length > 0;
      if (hasPetSystem) {
        getAnimals();
      }
    });

    $("#new").click(function() {
      var agora = getHourBr(FactoryDate.byTime(Date.toDay().getTime()));
      //alert (" agora ==== " + getHourBr(FactoryDate.byTime(agora)))
      
      if (!$("#hour_start").val()) {
         $("#hour_start").val(agora);
      } else {
        // aceita hora de atendiemto existente do cliente
      }
      Manager.new ();
/*
      if ((!$("#user").val()) || (parseFloat($("#user").val()) == 0)) {
        return alert('Um profissional precisa ser selecionado!');
      }
      if ($("#password").val() != undefined) {
        if (!$("#password").val()) {
          return alert('Informe senha do profissional!');
        } 
      }
      Manager.getActivities();

      // para exibir um possivelcliente setado ao clicar na linha
      $("#customer").change();

      return $("#command_modal").modal({
        "show": true,
        "keyboard": true,
        "backdrop": true
      });
*/
    });

    $("#command_get_from_server").click(function() {
      return Manager.getCustomerbyCommand();
    });

    $(".b_add_command").click(function() {
      return Manager.save();
    });
    $(".b_update_command").click(function() {
      return Manager.update();
    });
    Manager
    //$("#start_date").val(new Date().getDateBr());
    //$("#end_date").val(new Date().getDateBr());
    //var startDate = function(){
    //  return  encodeURIComponent($("#start").val() != "" ? $("#start").val() : getDateBr(new Date().getStartOfMonth()));
    //}
    //var endDate = function(){
    // return encodeURIComponent($("#end").val() != "" ? $("#end").val() : getDateBr(new Date()));
    //}     
    $("#product").productSearch({
      createName: false,
      iconElement: ".add-on",
      userThuch: true,
      userFieldSelector: '#user'
    });
    // rigel 03/07/2018 - tive que duplicar o product_search2
    // para ter 2 campos de produto na mesma window, embora em modais diferentes
    // o set só valia para o ultimo atribuido, a descrição
    // do primeiro vinha no lugar do id e dava erro no sql
    $("#product_edit").productSearch2({
      createName: false,
      iconElement: ".add-on",
      userThuch: true,
      userFieldSelector: '#user'
    });
    var start = gup("start") || getDateBr(new Date());
    $("#start").val(start)
    Manager.getUsersCurrentUnitCommand();
    Manager.getAuxiliarsCurrentUnitCommand();
    // Manager.getCustomers(); agora tem que ver a data do dia
    // comentadoo para nao entrar buscando
    // Manager.getListFromServer();
    $("#send").click(function() {
      if (AuthUtil.user.id == 3) {
        Manager.notifyMe ();
      }
      $("#start").val($("#day").val())
      Manager.getCustomers();
      return Manager.getListFromServer();
    });
    $("#user").change(function () {
      return Manager.getActivities();
    });
    $("#start_date,#end_date").change(function() {
      return Manager.getListFromServer();
    });
    var hasEdoctusSystem = $('.has-edoctus-system').length > 0;
    if (hasEdoctusSystem) {
//    if (document.location.href.indexOf("edoctus") != -1) {
      var min = 5 // minutos para refresh
      setTimeout(function(){    
        window.location.reload();
      },1000*60*min);
      return Manager.getListFromServer();
    }
  });

  window.Manager = Manager;

}).call(this);
