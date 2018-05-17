  var last_value = 0.0;
  var atualCustomerIndex = -1;

  var global_paymentTypes = [];

  var global_activitiesObj = [];

  var globals_bank = [];

  var globals_treatments_to_ignore = [];
  var global_cashier = null;

  var paymentOfAccount = false;

  // criei para controlar exclusão de itens no grid 
  // e evitar pagamento de treatment vazio
  // rigel 16/05/2018
  var itensInGrid = 0;

  var prodCustomerAccount = 0;
  var hasCustomerAccount = false;
  var prodCustomerCredit = 0;
  var hasCustomerCredit = false;
  var canBePaidWithPoints = 0.0;

  mousePrepare = function() {
    Mousetrap.init();
    Mousetrap.bind('!', function() {
      $('#command').focus();
      setTimeout("$('#command').val('');", 10);
    });

    Mousetrap.bind('+', function() {
      $('#command').blur();
      val = $('#command').val();
      if (val) {
        val = window.parseInt(val) + 1;
        $('#command').val(val);
      } else {
        $('#command').val(1);
      }
    });
    Mousetrap.bind(['.'], function() {
      $("#add_prduct").click();
    });
    Mousetrap.bind(['m', 'M'], function() {
      $(".b_payment_finalize:first").click();
    });

    Mousetrap.bind(['p', 'P'], function() {
      $("#b_payment").click();
    });

    Mousetrap.bind('*', function() {
      $('#command').val("");
    });

    Mousetrap.bind(['-', '_'], function() {
      $('#command').blur();
      val = $('#command').val();
      if (val) {
        val = window.parseInt(val) - 1;
        $('#command').val(val);
      } else {
        $('#command').val(1);
      }
    });

    Mousetrap.bind('@', function() {
      $(".customer_search_button:first").click();
      setTimeout("$('.customer_name').focus();", 500);
    });
  };
  var treatments = [];
  // > undefined
  var payments = [];
  // > undefined

  var clearData = function() {
    globals_treatments_to_ignore = [];
    treatments = [];
    PaymentController.clearPayment();
    $('#command').val("");
    $('#command').val("");
    $('#user').val("").change();
    $('#payment_type').val("");
    prepareTreatmentsInUi();
    preparePaymentsInUi();
    clearInstallment();
    if (gup("command")) {
      window.close();
    }
    try {
      PaymentController.$scope.$apply();
    } catch (e) {}
  };
  // > undefined
  window.removePaymentById = function(id) {
    payments = payments.filter(function(item) {
      return item.id != id;
    });
    PaymentController.$scope.paymentDetails = payments;
    preparePaymentsInUi();
    setValuesToUi();
  };
  // > ReferenceError: window is not defined
  // >     at repl:1:1
  // >     at REPLServer.self.eval (repl.js:112:21)
  // >     at repl.js:249:20
  // >     at REPLServer.self.eval (repl.js:122:7)
  // >     at Interface.<anonymous> (repl.js:239:12)
  // >     at Interface.EventEmitter.emit (events.js:95:17)
  // >     at Interface._onLine (readline.js:202:10)
  // >     at Interface._line (readline.js:531:8)
  // >     at Interface._ttyWrite (readline.js:767:16)
  // >     at ReadStream.onkeypress (readline.js:99:10)

  var gatDateTreatmentOr0 = function() {
    if ($("#date_treatment").val()) {
      return encodeURIComponent($("#date_treatment").val());
    } else {
      return 0;
    }
  };

  var gatDateTreatmentIni = function() {
    if ($("#date_treatment_ini").val()) {
      return encodeURIComponent($("#date_treatment_ini").val());
    } else {
      return gatDateTreatmentOr0();
    }
  };

  var global_treatmentIndex = 0;
  var global_activityIndex = 0;
  var editPrice = function(treatmentIndex, activityIndex, element) {
    var value = $(element).val();
    // rigel 13/10/2017 evitar NaN qdo limpa o valor
    if (value == "NaN" || value == "") {
      value = "0";
    }
    value = value.replace(',', '.');
    global_treatmentIndex = treatmentIndex;
    global_activityIndex = activityIndex;
    changeValue(value);
  };

  var changeValue = function(value) {
    treatments[global_treatmentIndex].activitys[global_activityIndex].price = parseFloat(value);
    saveTreatments(); // rigel 02/2017
    prepareTreatmentsInUi();
  };
  var getActivityIcon = function(activity) {
    var path = "/images/";
    var icon = "";
    if (activity.activityType == "activity") {
      icon = "cut.png";
      alt = "Serviço";
    } else {
      icon = "product.png";
      alt = "Produto";
    }
    if (activity.parent_delivery) {
      icon = "bag.png";
      alt = "Venda de pacote, o preço é sempre 0.00!";
    } else if (activity.for_delivery) {
      icon = "delivery.png";
      alt = "Item de Pacote, cliente irá obter sessões";
    } else if (Customer.hasDelivery(activity.activityId)) {
      icon = "delivery_ok.png";
      alt = "Utilização de pacote, preço do item no pacote!";
    } else if (Customer.hasBpMonthly(activity.activityId)) {
      icon = "bpmonthly.png";
      alt = "Utilização de mensalidade, preço do valor da sessão!";
    } else if (Customer.hasBudget(activity.activityId)) {
      icon = "budget.png";
      alt = "Utilização de orçamento, preço do item orçado!";
    }
    return "<img class='img_alt' width='16px' src='" + path + icon + "' alt='" + alt + "'/>";
  };

  var ignoreTreatment = function(treatmentIndex, activityIndex) {
    var treatment = treatments[treatmentIndex];
    var details = treatment.activitys;
    itensInGrid --;      
    if (details.length > 1) {
      $("#user").val(treatment.userId);
      var treatmentAtual = buildTreatmentFromUi();
      for (var i = details.length - 1; i >= 0; i--) {
        if (i != activityIndex) {
          treatmentAtual.activitys.push(details[i]);
        }
      };
      treatment.activitys = [details[activityIndex]]
      treatments.push(treatmentAtual);
    }
    treatment.ignored = true;
    globals_treatments_to_ignore.push(treatments[treatmentIndex].id);
    saveTreatments(function() {
      prepareTreatmentsInUi();
    }, true);
  };
  var removeActivity = function(treatmentIndex, activityIndex) {
    treatments[treatmentIndex].activitys[activityIndex].removed = true;
    var hasActivityToShow = treatments[treatmentIndex].activitys.length > 1;
    itensInGrid --;
    saveTreatments(function() {
      prepareTreatmentsInUi();
    }, true); //!hasActivityToShow);
  };
  var setAuxiliarActivity = function(treatmentIndex, activityIndex) {
    var customerId = treatments[treatmentIndex].customerId;
    var detailId = treatments[treatmentIndex].activitys[activityIndex].id
    var userId = $("#auxiliar").val();
    var msgAux = "";
    if (!userId) {
      userId = "0";  
      msgAux = "\nComo o campo está vazio, um possível assistente no atendimento será excluído!"
    } else {
      msgAux = "";
    }
    if (confirm("Tem certeza que deseja atribuir este assistente a este serviço?" + msgAux)) {
      //
      // usado tambem na comanda e na agenda e no caixa
      // duplicado identico ao treatmentManger
      //
      return $.post("/command/setaux", {
        "user": userId,
        "tdid": detailId,
        "command": "0" // agenda 1 seria commanda
      }, function(results) {
        if(results === 1 || results == "1"){
          if (userId == "0") {
            alert("Assistente excluído com sucesso");
          } else {
            alert("Assistente cadastrado com sucesso");
          }
          getTreatmentbyCommand(false, customerId);
        }else{
          alert(eval(results));
        }
      });
    }
  };
  var setOffSaleActivity = function(treatmentIndex, activityIndex) {
    var customerId = treatments[treatmentIndex].customerId;
    var detailId = treatments[treatmentIndex].activitys[activityIndex].id
    var offsaleId = $("#offsale").val();
    var msgAux = "";
    if (!offsaleId) {
      offsaleId = "0";  
      msgAux = "\nComo o campo está vazio, um possível convênio no atendimento será excluído!"
    } else {
      msgAux = "";
    }
    if (confirm("Tem certeza que deseja atribuir este convênio a este serviço?" + msgAux)) {
      //
      // usado tambem na comanda e na agenda e no caixa
      // duplicado identico ao treatmentManger
      //
      return $.post("/command/setoff", {
        "offsale": offsaleId,
        "tdid": detailId,
        "command": "0" // agenda 1 seria commanda
      }, function(results) {
        if(results === 1 || results == "1"){
          if (offsaleId == "0") {
            alert("Convênio excluído com sucesso");
          } else {
            alert("Convênio cadastrado com sucesso");
          }
          getTreatmentbyCommand(false, customerId);
        }else{
          alert(eval(results));
        }
      });
    }
  };
  var setPetActivity = function(treatmentIndex, activityIndex) {
    var customerId = treatments[treatmentIndex].customerId;
    var detailId = treatments[treatmentIndex].activitys[activityIndex].id
    var petId = $("#animal").val();
    var msgAux = "";
    if (!petId) {
      petId = "0";  
      msgAux = "\nComo o campo está vazio, um possível pet no atendimento será excluído!"
    } else {
      msgAux = "";
    }
    if (confirm("Tem certeza que deseja atribuir este pet a este serviço?" + msgAux)) {
      //
      // usado tambem na comanda e na agenda e no caixa
      // duplicado identico ao treatmentManger
      //
      return $.post("/command/setpet", {
        "animal": petId,
        "tdid": detailId,
        "command": "0" // agenda 1 seria commanda
      }, function(results) {
        if(results === 1 || results == "1"){
          if (petId == "0") {
            alert("Pet excluído com sucesso");
          } else {
            alert("Pet cadastrado com sucesso");
          }
          getTreatmentbyCommand(false, customerId);
        }else{
          alert(eval(results));
        }
      });
    }
  };
  var setToothActivity = function(treatmentIndex, activityIndex) {
    var customerId = treatments[treatmentIndex].customerId;
    var detailId = treatments[treatmentIndex].activitys[activityIndex].id
    var tooth = $("#tooth").val();
    var msgAux = "";
    if (!tooth) {
      tooth = "";  
      msgAux = "\nComo o campo está vazio, um possível dente no atendimento será excluído!"
    } else {
      msgAux = "";
    }
    if (confirm("Tem certeza que deseja atribuir este dente a este serviço?" + msgAux)) {
      //
      // usado tambem na comanda e na agenda e no caixa
      // duplicado identico ao treatmentManger
      //
      return $.post("/command/settooth", {
        "tooth": tooth,
        "tdid": detailId,
        "command": "0" // agenda 1 seria commanda
      }, function(results) {
        if(results === 1 || results == "1"){
          if (tooth == "") {
            alert("Dente excluído com sucesso");
          } else {
            alert("Dente cadastrado com sucesso");
          }
          getTreatmentbyCommand(false, customerId);
        }else{
          alert(eval(results));
        }
      });
    }
  };

  var removeTreatments = function() {
      for (var i = treatments.length - 1; i >= 0; i--) {
        var treatment = treatments[i];
        for (var j = treatment.activitys.length - 1; j >= 0; j--) {
          treatment.activitys[j].removed = true
        };
      };
      saveTreatments(function() {
        //alert('Excluído com sucesso!');
        clearData();
      }, true);
    }
    //var getPaymentTypeById

  var getPaymentDescriptionById = function(typePayment) {
    for (var i = global_paymentTypes.length - 1; i >= 0; i--) {
      if (global_paymentTypes[i].id == typePayment) {
        return global_paymentTypes[i].name;
      }
    }
    return "";
  };

  var preparePaymentsInUi = function() {
    setTimeout(showOrHideChequeInfor, 2);
  };

  var prepareTreatmentsInUi = function(changeCutomer) {
    $("#treatments_details tbody tr").remove();
    var paid = false;
    var total = 0;
    var totalInPoints = 0;
    var totalPointsOnBuy = 0;
    if (treatments.length > 0) {
      treatment = treatments[0]; // rigel
      treatments = treatments.filter(function(t) {
        //return t.treatmentStatus != 'Paid';
        return t.treatmentStatus != 4; // paid
      });
      if (treatments.length == 0) {
        // rigel para mostrar o cliente mesmo já pago
        if ($(".id_customer_search:visible").val() === "" || changeCutomer) {
          $(".id_customer_search:visible").val(treatment.customerId).change();
          $(".name_customer_search").val(treatment.customerName);
        } else {
          if (treatment.customerId != $(".id_customer_search").val()) {
            treatment.customerId = window.parseInt($(".id_customer_search").val());
          }
        }
        // até aqui
        alert("Atendimento já foi pago!");
      }
    }
    hasCustomerAccount = false;
    hasCustomerCredit = false;
    for (var i = treatments.length - 1; i >= 0; i--) {
      treatment = treatments[i];
      if (treatment.ignored) {
        continue;
      }
//    if (treatment.treatmentStatus == 'Paid') {
      if (treatment.treatmentStatus == 4 /* paid */) {
        treatments = [];
        return;
      }
      if ($(".id_customer_search:visible").val() === "" || changeCutomer) {
        $(".id_customer_search:visible").val(treatment.customerId).change();
        $(".name_customer_search").val(treatment.customerName);
      } else {
        if (treatment.customerId != $(".id_customer_search").val()) {
          treatment.customerId = window.parseInt($(".id_customer_search").val());
        }
      }
      $("#date_treatment").val(getDateBr(new Date(treatment.dateTreatment)));
      validateOpenDate();
      activitys = treatment.activitys;
      var lines = "";
      var getChagePrice = function(activity) {
        var price = activity.price;
        if (!Customer.hasDelivery(activity.activityId)) {
          return "<input type='text' class='small' step='0.01' min='0' onchange='editPrice(" + i + "," + j + ",this)' value='" + (price.toFixed(2)) + "' class='price_product'/>";
        } else {
          return price.toFixed(2);
        }
      }
      var canUsePoints = true;
      itensInGrid = 0;
      for (var j = activitys.length - 1; j >= 0; j--) {
        activity = activitys[j];
        if (!activity.removed) {
          itensInGrid ++;
          var price = activity.price;
          var hasUnitModule = $('.has-unit-module').length > 0;
          var hasAuxiliarModule = $('.has-auxiliar-module').length > 0;
          var hasOffSaleModule = $('.has-offsale-module').length > 0;
          var hasFidelityModule = $('.has-fidelity-module').length > 0;
          var hasPetSystem = $('.has-pet-system').length > 0;
          var hasEsmileSystem = $('.has-esmile-system').length > 0;
          var amount = activity.amount = activity.amount || 1;

          if (activity.activityId == prodCustomerAccount) {
            hasCustomerAccount = true;
            //alert ("tem conta cliente")
          }
          if (activity.activityId == prodCustomerCredit) {
            hasCustomerCredit = true;
            //alert ("tem crédito cliente")
          }
          total += activity.price * amount;
          if (activity.priceInPoints <= 0.0) {
            canUsePoints = false;
          }
          totalInPoints += activity.priceInPoints * amount;
          totalPointsOnBuy += activity.pointsOnBuy * amount;
          lines += "<tr>" + 
          "<td>" + getActivityIcon(activity) + "</td>" +
            "<td>" + treatment.userShortName + "</td>" +
            (hasUnitModule ? "<td>" + treatment.unitShortName + "</td>" : "" ) +
            (hasAuxiliarModule ? "<td>" + activity.auxiliarShortName + "</td>" : "" ) +
            (hasPetSystem ? "<td>" + activity.animalShortName + "</td>" : "" ) +
            "<td>" + activity.activity + "</td>" +
            (hasFidelityModule ? "<td>" + activity.priceInPoints + "</td>" : "" ) +
            (hasEsmileSystem ? "<td>" + activity.tooth + "</td>" : "" ) +
            "<td>" + getChagePrice(activity) + "</td>" +
            "<td><input type='number' step='1' min='0' class='mini' onchange='editQtd(" + i + "," + j + ",this)' value='" + amount + "'/></td>" +
            (hasOffSaleModule ? "<td>" + activity.offsaleShortName + "</td>" : "" ) +
            "<td><a title='Excluir item' href='#' onclick=\"if(confirm('Tem certeza que deseja excluir!')){removeActivity(" + i + "," + j + "); return false;}else{ return false};\"><img width='16px' src='../images/delete.png'/></a>  " + 
            "<a title='Ignorar este item neste pagamento' href='#' onclick=\"if(confirm('Tem certeza que deseja ignorar o atendimento!')){ignoreTreatment(" + i + "," + j + "); return false;}else{return false};\"><img width='16px' src='../images/cancel.png'/></a>   " + 
            (hasPetSystem ? "<a title='Atribuir pet' href='#' onclick=setPetActivity(" + i + "," + j + ")><img width='16px' src='/images/addpet.png'/></a>" : "") +
            (hasAuxiliarModule ? "<a title='Atribuir assistente' href='#' onclick=setAuxiliarActivity(" + i + "," + j + ")><img width='16px' src='/images/user.png'/></a>" : "") +
            (hasEsmileSystem ? "<a title='Atribuir dente' href='#' onclick=setToothActivity(" + i + "," + j + ")><img width='16px' src='/images/addtooth.png'/></a>" : "") +
            (hasOffSaleModule ? "<a title='Atribuir convênio' href='#' onclick=setOffSaleActivity(" + i + "," + j + ")><img width='16px' src='/images/agreement.png'/></a>" : "") +
            "</td>" +
            "</tr>";
        }
      }
      var points = 0.0
      var valueToPoints = 0.0;
      points = $("#valueinpoints").val();
      canBePaidWithPoints = 0.0;
      var pointsAux = 0.0
      pointsAux = parseFloat (points) + parseFloat (totalPointsOnBuy)
      if (pointsAux >= totalInPoints) {
        canBePaidWithPoints = total
      } else {
        canBePaidWithPoints = pointsAux * total / totalInPoints
      }
      if (!canUsePoints && canBePaidWithPoints > 0) {
        // se tem itens que não podem ser pagos em pontos
        canBePaidWithPoints = 0.0; 
      }

      $("#canBePaidWithPoints").val((canBePaidWithPoints).formatMoney());
      valueToPoints = totalInPoints / total
      $("#valueToPoints").val(valueToPoints); // nao fazer formatmoney a virgula dava erro 

      $("#treatments_details tbody").append(lines);
      $('.img_alt').qtip();
      $("#total").val((total).formatMoney());
      $("#total_baixo").val((total).formatMoney());
      $("#totalInPoints").val((totalInPoints).formatMoney());
      $("#totalInPoints_baixo").val((totalInPoints).formatMoney());
      $("#totalPointsOnBuy").val((totalPointsOnBuy).formatMoney());
      $("#totalPointsOnBuy_baixo").val((totalPointsOnBuy).formatMoney());
    }
  };
  var removeTreatmentbyCommand = function() {
    // antes exclusao de pagamento era por comanda apenas, nada data
    // agora se tem cliente, ele vai junto
    // rigel 12/07/2017
    if (!$('#customer').val()) {
      $('#customer').val("0")
    }
    url = "/cash/removePayment/" + $('#command').val() + 
    '/' + $('#customer').val() + '/'+ gatDateTreatmentOr0();
    $.get(url, function(t) {
      t = eval(t);
      if (t == "1") {
        alert("Pagamento excluído com sucesso!");
        $("#command_get_from_server").click();
      } else {
        alert(t);
      }
    }, "text").error(function() {
      alert("Ocorreu um erro desconhecido na exclusão do pagamento verifique a comanda!");
    });
  };
  var showCommand = function() {
    url = "/financial_cashier/comman_conference?startDate=" + gatDateTreatmentOr0() + "&endDate=" + gatDateTreatmentOr0() + "&productclass=1%2C0&commands=" + $('#command').val();
    window.open(url);
  };
  var getTreatmentbyCommand = function(callback, customer) {
    //url = $('#cash_form').attr('action');
    var url = ""
    if (gatDateTreatmentIni() != gatDateTreatmentOr0()) {
      url = "/cash/getTreatment/" + "0" + '/' + $('#customer').val() + '/' +gatDateTreatmentIni() + '/' + gatDateTreatmentOr0();
    } else {
      // 21/11/2017 - Rigel - tinha feito só qdo informava cliente
      if (prodCustomerAccount == 0 && prodCustomerCredit == 0) {
        // chama com customer 0 só para trazer os produtos
        // credito e conta cliente
        url = "/cash/getProductPreviousDebts/0"
        $.get(url, function(t) {
          eval("var prodObj = " + t);
          // 13/10/2017 rigel - salva o produto conta cliente
          prodCustomerAccount = prodObj.id
          prodCustomerCredit = prodObj.credit
//          $('#valueinpoints').val(prodObj.customerValueInPoints);
        }, "text");
      }  

      if ($('#command').val() != "0") {
        if (!$('#customer').val()) {
          $('#customer').val("0")
        }
        url = "/cash/getTreatment/" + $('#command').val() + 
        '/' + $('#customer').val() + '/' +gatDateTreatmentIni() + '/' + gatDateTreatmentOr0();
      } else {
        url = "/cash/getTreatment/" + $('#command').val() + 
        '/' + $('#customer').val() + '/' +gatDateTreatmentIni() + '/' + gatDateTreatmentOr0();
      }        
    }
    $.get(url, function(t) {
      eval("var resultCommandCall = " + t);
      if (resultCommandCall.status == 'success') {
        treatments = resultCommandCall.data;
        $(document).focus();
      } else {
        alert(resultCommandCall.message);
      }
      treatments = resolveTreatments(treatments, customer);
      prepareTreatmentsInUi(true);
      if (callback) {
        callback(treatments);
      }
    }, "text");
  };


  var _openDateValidated = false;
  var validateOpenDate = function() {
      if (_openDateValidated) return;
      var openDate = getDateBr(new Date($("#cashiers_select")[0].cashiers_map[$("#cashiers_select").val()].openerDate));
      var dateTreatment = $("#date_treatment").val() !== "" ? $("#date_treatment").datepicker("getDate") : null;
      
      if (dateTreatment && openDate != getDateBr(dateTreatment)) {
        _openDateValidated = true;
        alert('A data do atendimento é diferente da data do caixa!');        
      }
  };

  var getCashiers = function() {
    $("#cashiers_select").cashierField(true, 'open', function() {
      if ($("#cashiers_select").val() === '') {
        $("#cashiers_select option:nth(1)").attr("selected", true);
        if ($("#cashiers_select").val() === '') {
          //alert ("Não existe nenhum caixa aberto!")
          if (confirm("Não existe nenhum caixa aberto, clique ok para abrir um," +
            "\nou após cancelar vá em menu financeiro, primeiro ícone à esquerda.")) {
            // $("#checkout_open").click();
            window.open("/financial_cashier/checkout_open")
          }
        }
      }

      validateOpenDate();

    }, true);
  };

  var getUser = function(id) {
    for (var i = userObj.length - 1; i >= 0; i--) {
      if (userObj[i].id == id) {
        return userObj[i];
      }
    }
  };
  var getUsers = function() {
    url = $('#cash_form').attr('action');
    //url += "/getUsers"; trazia profs independente de unidade e flag de showincashier
    // 13/06/2017 - rigel
    url += "/getUsersCurrentUnitCashier";
    $.get(url, function(t) {
      eval("userObj = " + t);
      if (userObj.length > 1) {
        $('#user').append("<option value=''>Selecione um profissional</option>");
      } 
      for (var i in userObj) {
        $('#user').append("<option value='" + userObj[i].id + "'>" + userObj[i].name + "</option>");
      }
    });
    $('#user').change(function() {
      if ($('#user').val()) {
        getActivities();
        ProductSearch.updatePrice();
      };
      return;
    });
  };
  var getActivities = function() {
    DataManager.getActivities($("#user").val(), function(activitysObj) {
      global_activitiesObj = activitysObj;
      $('#activity option').remove();
      var ret = "<option value=''>Selecione um serviço</option>";
      for (var i in activitysObj) {
        ret += "<option value='" + activitysObj[i].id + "'>" + activitysObj[i].name + "</option>";
      }
      $('#activity').append(ret);
      $('#activity').change().select2('open');
    });
  };
  var getPaymentTypes = function() {
    DataManager.getPaymentTypes(function(paymentTypes) {
      var ret = "";
      global_paymentTypes = paymentTypes;
      for (var i in paymentTypes) {
        ret += "<option value='" + paymentTypes[i].id + "'>" + paymentTypes[i].name + "</option>";
      }
      $('#payment_type').append(ret);
    });
  };

  // var getProcuts = function(){
  //     url ="/product/for_sale";
  //     $.get(url, function(t){
  //       eval("productsObj = "+t);
  //       $('#product').append("<option value=''>Selecione um Produto</option>");
  //       for(var i in productsObj){
  //         $('#product').append("<option value='"+productsObj[i].id+"'>"+productsObj[i].external_id+" "+productsObj[i].name+" R$ "+productsObj[i].price.formatMoney()+"</option>");
  //       }
  //     });
  //     $('#product').change(function(){
  //       return;
  //     });
  // };

  var getActivity = function(id) {
    for (var i = global_activitiesObj.length - 1; i >= 0; i--) {
      if (global_activitiesObj[i].id == id) {
        return global_activitiesObj[i];
      }
    }
    return null;
  };

  var getProduct = function(id) {
    return ProductSearch.selectedProduct;
  };

  var buildTreatmentFromUi = function() {
    if ($(".id_customer_search").val() === "") {
      alert("Selecione um cliente!");
      return false;
    }

    if ($("#user").val() === "" && !paymentOfAccount) {
      alert("Selecione um profissional!");
      return false;
    }
    paymentOfAccount = false;

    var dateTreatment = $("#date_treatment").val() !== "" ? $("#date_treatment").datepicker("getDate") : new Date();
    var obj = {
      for_delivery: false,
      customerId: window.parseInt($(".id_customer_search").val()),
      customerName: $(".name_customer_search").val(),
      treatmentStatus: "Open",
      userId: window.parseInt($("#user").val() || "0"),
      userName: ($("#user option:selected").text() || ""),
      id: 0,
      activitys: [],
      removed: false,
      ignored: false,
      dateTreatment: dateTreatment
    };
    return obj;
  };
  var calculatePrice = function(objAcitivity) {
    if (Customer.hasDelivery(objAcitivity.activityId)) {
      objAcitivity.price = Customer.useDeliveryReturnPrice(objAcitivity.activityId);
      objAcitivity.useDelivery = true;
    } else if (Customer.hasBudget(objAcitivity.activityId)) {
      objAcitivity.price = Customer.useBudgetReturnPrice(objAcitivity.activityId);
      objAcitivity.useBudget = true;
    } else {
      objAcitivity.price = OffSaleCurrent.calculatePrice(objAcitivity.activityId, objAcitivity.price);
    }
  };
  var addActivityToAtualTreatment = function(objAcitivity, notSave) {
    if (!objAcitivity.parentBom) {
      objAcitivity.parentBom = 0;
    }

    if (!objAcitivity.offsale) {
      objAcitivity.offsale = parseInt($("#offsale").val()) || 0;
    }

    if (!objAcitivity.tooth) {
      objAcitivity.tooth = $("#tooth").val() || "";
    }

    if (!objAcitivity.animal) {
      objAcitivity.animal = parseInt($("#animal").val()) || 0;
    }

    if (!objAcitivity.auxiliar) {
      objAcitivity.auxiliar = parseInt($("#auxiliar").val()) || 0;
    }

    calculatePrice(objAcitivity);
    var user = getUser($("#user").val());
    var isNew = true;
    var position = 0;
    for (var i = treatments.length - 1; i >= 0; i--) {
      if (user && treatments[i].userId == user.id) {
        isNew = false;
        position = i;
        break;
      }
    }
    if (isNew) {
      treatmentAtual = buildTreatmentFromUi();
      if (treatmentAtual) {
        position = treatments.length;
        treatments.push(treatmentAtual);
      } else {
        return false;
      }
    }
    treatments[position].activitys.push(objAcitivity);
    prepareTreatmentsInUi();
    if (!notSave)
      saveTreatments();
    return true;
  };

  var getValueTotalTreatments = function() {
    var total = 0;
    for (var i = treatments.length - 1; i >= 0; i--) {
      var treatment = treatments[i];
      for (var j = treatment.activitys.length - 1; j >= 0; j--) {
        if (!treatment.removed && !treatment.ignored && !treatment.activitys[j].removed)
          total += treatment.activitys[j].price * treatment.activitys[j].amount;
      }
    }
    return total;
  };

  var getWhitoutPayment = function() {
    return getTotalToPaid() - getTotalPaid();
  };

  var getTotalToPaid = function() {
    return getValueTotalTreatments() + window.parseFloat($('#increase').val());
  };

  var getTotalPaid = function() {
    var total = 0.0;
    for (var i = payments.length - 1; i >= 0; i--) {
      if (!payments[i].removed) {
        total += parseFloat(payments[i].value);
      }
    }
    return total;
  };

  var getResturnValue = function() {
    return Math.max(last_value - getTotalToPaid(), 0.00);
  };

  var setValuesToUi = function() {
    var total = getValueTotalTreatments();
    var totalToPaid = getTotalPaid();
    $("#customer_payment").val($("#customer").val());
    $("#name_customer_payment").val(Customer.current.name);
    $("#command_payment").val($("#command").val());
    $('#payment_type').change();
    $("#value").val(parseFloat(total).formatMoney());
    $("#payment_type_value").val(Math.max(total - totalToPaid, 0.00).toFixed(2));
    $("#discount_value").val(0.00);
    $("#discount_percent").val(0.00);
    $("#value_total_to_payed").val(getTotalToPaid().formatMoney());
    $("#total_paiad").val(totalToPaid.formatMoney());
    $("#return_value").val(getResturnValue().formatMoney());
    if (totalToPaid == 0.00) {
      $("#add_payment").focus();
    } else {
      $(".b_payment_finalize:first").focus();
    }
  };

  $(function() {

    getCashiers();
    $("#product").productSearch({
      createName: false,
      iconElement: ".add-on",
      userThuch: true,
      userFieldSelector: '#user'
    });
    $('#auxiliar').auxiliarField(false);
    $("#tooth").toothField(true);
    $("#offsale").offSaleField(true);
    $("#offsale").change(function() {
      OffSaleCurrent.getProcuts($(this).val());
    });
    getUsers();
    //getProcuts();
    getPaymentTypes();

    $("#discount_percent").change(function() {
      if ($(this).data("value_in") != $(this).val()) {
        var discount_percent = window.parseFloat($(this).val());
        if (discount_percent > 100.0) {
          alert ("Desconto " + $(this).val() + 
            "% não pode ser maior que 100% ");
          $(this).val(0);
          return;
        }
        if (confirm ("Confirma desconto de " + $(this).val() + "% ?")) {
          $(this).data("changed", true);
          ajustPrice((window.parseFloat(discount_percent) / 100) * -1.0);
        } else {
          $(this).val(0);
        }  
      }
    }).focusout(function() {
      if (!$(this).data("changed")) {
        $(this).change();
      }
      $(this).data("changed", true);
    }).focusin(function() {
      $(this).data("changed", false);
      $(this).data("value_in", $(this).val());
    })
    // rigel - fiz o desconto de valor
    $("#discount_value").change(function() {
      if ($(this).data("value_in") != $(this).val()) {
        var discount1_value = window.parseFloat($(this).val());
        var totalaux = window.parseFloat($("#value").val());
        if (discount1_value > totalaux) {
          alert ("Desconto " + $(this).val() + 
            " não pode ser maior que o valor a pagar " + $("#value").val());
          $(this).val(0);
          return;
        }
        if (confirm ("Confirma desconto de " + $(this).val() + " ?")) {
          $(this).data("changed", true);
          discount1_value = window.parseFloat(discount1_value) * 100 / window.parseFloat(totalaux);
          ajustPrice((window.parseFloat(discount1_value) / 100) * -1.0);
        } else {
          $(this).val(0);
        }
      }
    }).focusout(function() {
      if (!$(this).data("changed")) {
        $(this).change();
      }
      $(this).data("changed", true);
    }).focusin(function() {
      $(this).data("changed", false);
      $(this).data("value_in", $(this).val());
    })
    $("#command_remove_from_server").click(function() {
      // rigel 11/06/2014
      if (confirm("Tem certeza que deseja excluir o pagamento?")) {
        removeTreatmentbyCommand();
      }
    });
    $("#command_show").click(function() {
      showCommand();
    });
    $("#date_treatment").change(function() {
      if ($("#opener_date").datepicker("getDate")) {
        if ($(this).datepicker("getDate").getTime() < $("#opener_date").datepicker("getDate").getTime()) {
          alert("Data anterior a abertura do caixa");
        }
      }
    });
    $("#command_get_from_server").click(function() {
      getTreatmentbyCommand();
    });

    $("#add_activity").click(function() {
      var activiId = $("#activity").val();
      var activityObj = getActivity(activiId);
      addActivityToAtualTreatment({
        activity: activityObj.name,
        activityId: activityObj.id,
        price: activityObj.price,
        id: 0,
        activityType: 'activity',
        removed: false,
        for_delivery: false
      });
      $("#activity").select2('focus');
    });
    var getAllActivitys = function() {
      return treatments.map(function(treatment) {
        return treatment.activitys;
      }).reduce(function(a, b) {
        return a.concat(b);
      }, []);
    }
    var hasDeliveryToProduct = function(product) {
      var activitys = getAllActivitys();
      // rodar com f12 para ver estrutura de objeto
      //console.log(activitys)

      for (var i in activitys) {
        //console.log(activitys[i])
        //alert (activitys[i].name)
      }
      return activitys.filter(function(activity) {
        return activity.activityId == product.id;
      }).length > 0;
    };
    $("#add_prduct").click(function() {
      var prodObj = getProduct();
      if (prodObj.is_bom) {
        if (hasDeliveryToProduct(prodObj)) {
          alert("Já existe um pacote " + prodObj.name + " na tabela, para adicionar mais sessões aumente a quantidade! Para vender mais de um pacote faça em vendas separadas");
          return;
        } else {
          for (var i in prodObj.products) {
            paymentOfAccount = true;
            var p_bom = prodObj.products[i];
            if (!addActivityToAtualTreatment({
                amount: p_bom.qtd_bom,
                activity: p_bom.product.name,
                activityId: p_bom.product.id,
                price: p_bom.price_bom,
                id: 0,
                activityType: 'product',
                removed: false,
                for_delivery: p_bom.parceled,
                parentBom: prodObj.id
              }, true)) {
              return;
            }
            prodObj.price = 0.00;
          }
        }

      }
      paymentOfAccount = true;
      addActivityToAtualTreatment({
        activity: prodObj.name,
        activityId: prodObj.id,
        price: prodObj.price,
        id: 0,
        activityType: 'product',
        removed: false,
        for_delivery: false
      });
      $("#activity").select2('focus');
    });
    //
    $("#add_customer_account_payment").click(function() {
      var url = "/cash/getProductPreviousDebts/" + $(".id_customer_search").val();
      $.get(url, function(t) {
        eval("var prodObj = " + t);
        if (prodObj.price < 0) {
          paymentOfAccount = true;
          // 13/10/2017 rigel - salva o produto conta cliente
          prodCustomerAccount = prodObj.id
          prodCustomerCredit = prodObj.credit
          addActivityToAtualTreatment({
            activity: prodObj.name,
            activityId: prodObj.id,
            price: prodObj.price * -1,
            id: 0,
            activityType: 'product',
            removed: false,
            for_delivery: false
          });
        } else {
          if (confirm("Cliente não possui débitos em aberto! Deseja adicionar crédito?")) {
            addActivityToAtualTreatment({
              activity: prodObj.name,
              activityId: prodObj.credit,
              price: prodObj.price * -1,
              id: 0,
              activityType: 'product',
              removed: false,
              for_delivery: false
            });
          }

        }
      }, "text");
    });

    $('#increase').change(function() {
      setValuesToUi();
    });

    function isValid() {
      messages = [];
      if ($("#cashiers_select").val() === "") {
        messages.push("Selecione um caixa");
      }

      if (treatments.length === 0 || itensInGrid < 1) {
        messages.push("Não existem itens a serem pagos!");
      }

      if (hasCustomerAccount && hasCustomerCredit) {
        messages.push("Pagamento de Conta cliente e compra de Crédito cliente, devem ser efetuadas em transações separadas!" +
          "\n\n" + "Você pode ignorar um dos itens fazer o pagamento e depois acessar o cliente/comanda para fazer um segundo pagamento"); 
      }

      if (messages.length !== 0) {
        alert(messages.join("\n"));
        return false;
      }
      return true;
    }

    $(".b_payment").click(function() {
      if (isValid()) {
        $('#payment_form').modal({
          "show": true,
          "keyboard": true,
          "backdrop": true
        });
        PaymentController.clearPayment();
        setValuesToUi();
        preparePaymentsInUi();
        setValuesToUi();
        setTimeout(function() {
          $("#payment_type").focus();
        }, 500);
      }
    });
    $(".print_command").click(function() {
      print_command();
    });
    var print_command = function() {
      saveTreatments(function() {
        var hasAnvisaModule = $('.has-anvisa-module').length > 0;
        if (hasAnvisaModule) {
          window.open('/financial_cashier/expense_ticket?command=' + 
            $('#command').val() + 
            "&date=" + $('#date_treatment').val() + 
            "&customerId=" + $('#customer').val(),"_command_maste");
        } else {
          window.open('/financial_cashier/print_command?command=' + 
            $('#command').val() + 
            "&unit=" + AuthUtil.unit.id + 
            "&date=" + $('#date_treatment').val() +
            "&customerId=" + $('#customer').val(),"_command_maste");
        }
      }, true);
    }
    window.saveTreatments = function(callback, notCallCommand) {
      $.post("/cash/saveTreatments", {
        "date": $("#date_treatment").val() || getDateBr(new Date()),
        "data": JSON.stringify(treatments),
        "command": $("#command").val()
      }, function(t) {
        eval("paymentReturn = " + t);
        if (paymentReturn.status == 'success') {
          if (!notCallCommand)
            getTreatmentbyCommand(false, treatments[0].customerId)
          if (callback)
            callback();
        } else {
          alert(paymentReturn.message);
        }
      }, "text");

    }
    window.resetPricesTreatments = function(callback) {
      var treatments_id = treatments.map(function(t) {
        return t.id
      }).join(",")
      $.post("/treatment/util/revert_prices", {
        "treatments": treatments_id
      }, function(t) {
        $("#command_get_from_server").click();
        alert("Restaurado com sucesso!");
      }, "text");

    }
    $(".revert_price").click(function() {
      if (itensInGrid < 1) {
        return alert ("Não existem itens para restauração de preços!")
      }
      if (confirm("Tem certeza que deseja restaurar o preço original dos itens abaixo?")) {
        resetPricesTreatments();
      }
    });
    $(".remove_treatment").click(function() {
      if (confirm("Tem certeza que deseja excluir os atendimentos?")) {
        removeTreatments();
      }
    });
    var paymentRuning = false;
    $(".b_payment_finalize").click(function() {
      if (confirm("Tem certeza que deseja finalizar o pagamento?")) {
        if (!paymentRuning) {
          paymentRuning = true;
          var valToPoints = 0.0;
          if ($("#valueToPoints").val() && 
              $("#valueToPoints").val() != 'NaN' &&
              $("#valueToPoints").val() != 'undefined') {
            valToPoints = parseFloat ($("#valueToPoints").val());
          }
          var value_current = $("#payment_type_value").val();
          if ((value_current !== "" && 
              window.parseFloat(value_current) !== 0.0) || 
              (payments.length == 0)) {
            $("#add_payment").click();
          }
          url = $('#cash_form').attr('action');
          url += "processPayment";
          var paymentsToSend = payments.map(function(item) {
            item.dateDetailStr = item.dateDetail.getDateBr();
            item.valueToPoints = valToPoints;
            if (item.chequeInfo) {
              item.chequeInfo.cheque_date_str = item.chequeInfo.cheque_date;
            };
            return item;
          });
          
          request_payment = {
            "treatments": treatments,
            "payments": paymentsToSend,
            "command": $("#command").val(),
            "dataTreatments": $("#date_treatment").val(),
            'cashier': $("#cashiers_select").val(),
            "status2": 4,
            "customer": $(".id_customer_search").val(),
            "valueToPoints": valToPoints
          };
          $.post(url, {
            "data": JSON.stringify(request_payment)
          }, function(t) {
            eval("paymentReturn = " + t);
            if (paymentReturn.status == 'success') {
              alert(paymentReturn.message);
              clearData();
              $('#payment_form').modal("hide");
              setTimeout(function() {
                $('#command').focus();
              }, 200)
            } else {
              alert(paymentReturn.message);
            }
            paymentRuning = false;
          }, "text").error(function() {
            alert("Erro ao processar pagamento tente novamente!");
            paymentRuning = false;
          });
          return true;
        } else {
          alert("Aguarde um instante, já existe um pagamento em andamento!");
        }
      }
    });

    $("#new_value_button").click(function(e) {
      changeValue();
    });
    $("#payment_type").keypress(function(e) {
      if (e.keyCode == 13) {
        $(".b_payment_finalize:first").click();
      }
    });
    $("#new_value").keypress(function(e) {
      if (e.keyCode == 13) {
        changeValue();
      }
    });
    $("#payment_type").change(showOrHideInstallment);
    $("#payment_type_installment").click(showOrHideInstallment);
    showOrHideInstallment();
  });
  var showOrHideChequeInfor = function() {
    var hasCheque = payments.filter(function(payment) {
      return (payment.chequeInfo != undefined);
    }).length > 0;
    if (!hasCheque) {
      $(".has_cheque").hide();
    } else {
      $(".has_cheque").show();
      setTimeout(function() {
        $("#bank").focus();
      }, 100)
    } 
  }
  var showOrHideInstallment = function(argument) {
    var paymentType = getPaymentTypeById($('#payment_type').val());

    if (paymentType && paymentType.accept_installment) {
      $(".installment").show();
      if ($("#payment_type_installment").is(':checked')) {
        $(".installment_true").show();
      }
    } else {
      $(".installment").hide();
      $(".installment_true").hide();
      clearInstallment();
    }

    if (paymentType && paymentType.cheque) {
      $(".cheque_data").show();
    } else {
      $(".cheque_data").hide();
    }

    if (paymentType && paymentType.needCardInfo) {
      $(".card_data").show();
    } else {
      $(".card_data").hide();
    }

  }
  var clearInstallment = function() {
    $("#payment_type_installment").attr("checked", false);
    $("#payment_type_installment").is(':checked')
  };
  var getPaymentTypeById = function(id) {
    for (var i in global_paymentTypes) {
      if (global_paymentTypes[i].id == id) {
        return global_paymentTypes[i];
      }
    }
    return null;
  };

  var paymentRegister = function(paymentType, chequeInfo) {
    if (chequeInfo) {
      try {
        if (paymentType.cheque) {
          validateChequeInfos(chequeInfo);
        }
      } catch (e) {
        alert(e);
        return;
      }
    }
    if ($("#payment_type_installment").is(':checked')) {
      var installments = $("#payment_type_installment_number").val()
      var total = window.parseFloat($('#payment_type_value').val());
      var chequenum = 0;
      for (var i = 0; i < installments; i++) {
        var date = new Date();
        // 08/2016 - rigel antes não projetava os dias para o parcelado e nao projetava 
        // o cartão sem a opção nextmonth que vem antes do tratamento parcelado
        date.setDate (date.getDate () + paymentType.numDaysForReceive);
        date.setMonth(date.getMonth() + i);
        var value = (total / installments);
        if (i == 0) {
          value += window.parseFloat((total - ((total / installments).toFixed(2) * installments)).toFixed(2))
        } else {
          value = window.parseFloat(value.toFixed(2));
        }
        if (chequeInfo) {
          //chequeInfo.cheque_number = i + 1;
          if (i>0) {
/*
            chequenum = Number (chequeInfo.cheque_number);
            chequenum = chequenum + 1
            chequeInfo.cheque_number = chequenum
*/
            chequeInfo.cheque_number = Number (chequeInfo.cheque_number) + 1;
          }
        }
        paymentDetailAdd(value, window.parseInt(paymentType.id), false, chequeInfo, date)
      };
    } else {
      var total = window.parseFloat($('#payment_type_value').val());
      last_value = total;
      if (paymentType.accept_installment) {
        // 08/2016 - rigel antes não projetava os dias para o parcelado e nao projetava 
        // o cartão sem a opção nextmonth que vem antes do tratamento parcelado
        var date = new Date();
        date.setDate (date.getDate () + paymentType.numDaysForReceive);
        paymentDetailAdd(total, window.parseInt(paymentType.id), false, chequeInfo, date)
      } else {
        paymentDetailAdd(total, window.parseInt(paymentType.id), false, chequeInfo)
      }
    }
    setValuesToUi();
    preparePaymentsInUi();
  };
  var PaymentModel = angular.module('PaymentModel', ['ui.directives']);
  //$("#bank").html()
  var PaymentController = function($scope) {
    PaymentController.$scope = $scope;
    $scope.paymentDetails = window.payments;
    DataManager.getBanks(function(banks) {
      $scope.banks = banks;
    });
    $scope.addPayment = function() {
      paymentType = getPaymentTypeById($('#payment_type').val());
      if (paymentType.customerRegisterDebit || paymentType.customerUseCredit) {
        if (hasCustomerCredit || hasCustomerAccount) {
           alert ("Produtos especiais de conta cliente e/ou crédito cliente,\n" +
           "não podem ser pagos com formas de pagamento especiais que alteram o saldo do cliente.\n\n" +
           "Caso necessário ignore estes itens e faça um pagamento separado.")
           return;
        }
      }
      if (paymentType.fidelity) {
        if (canBePaidWithPoints > 0) {
           if (canBePaidWithPoints >= parseFloat($('#payment_type_value').val())) {
              canBePaidWithPoints -= parseFloat($('#payment_type_value').val());            
           } else {
             alert ("O valor máximo a ser pago com pontos fidelidade é de \n\n" + canBePaidWithPoints.formatMoney())
             return;
           }
        } else {
          alert ("Este cliente não pode pagar com pontos \nVerifique se existem ítens que não tem preço em pontos")
        }
      }

      if (paymentType.cheque) {
        chequeInfoObj = {
          agency: $(":input[name=agency]").val(),
          bank: parseInt($(":input[name=bank]").val()),
          bank_name: $(":input[name=bank] option[selected]").text(),
          account: $(":input[name=account]").val(),
          cheque_number: $(":input[name=cheque_number]").val()
        };
        paymentRegister(getPaymentTypeById($('#payment_type').val()), chequeInfoObj);
      } else if (paymentType.needCardInfo) {
        chequeInfoObj = {
          agency: "",
          bank: 0,
          bank_name: "",
          account: "",
          cheque_number: $(":input[name=card_number]").val()
        };
        paymentRegister(getPaymentTypeById($('#payment_type').val()), chequeInfoObj);
      } else {
        paymentRegister(paymentType);
        $scope.paymentDetails = window.payments;
      }
    }
  };
  PaymentController.clearPayment = function() {
    payments.splice(0, payments.length);
    PaymentController.$scope.paymentDetails = payments;
    showOrHideChequeInfor();
  }
  var directives = angular.module('directives', []);
  directives.directive('date', function() {
    return function(scope, element, attrs) {
      element.datepicker();
    }
  });

  /*directives.directive('bank', function() {
     return function(scope, element, attrs) {element.bankField();}
  });*/
  var paymentDetailAdd = function(value, typePayment, removed, chequeInfo, paymentDate) {
    if (!paymentDate) {
      paymentDate = $("#date_treatment").datepicker("getDate");
    }
    if (chequeInfo) {
      chequeInfo.date_for_payment = paymentDate.getDateBr();
      chequeInfo.cheque_date = $("#date_treatment").datepicker("getDate").getDateBr();
    }
    payments.push({
      'id': payments.length,
      'value': Math.min(getWhitoutPayment(), window.parseFloat(window.parseFloat(value).toFixed(2))),
      'typePayment': typePayment,
      removed: removed,
      "chequeInfo": angular.copy(chequeInfo),
      dateDetail: paymentDate,
      desc: getPaymentDescriptionById(typePayment),
      remove: function() {
        removePaymentById(this.id);
      },
      accept_installment: getPaymentTypeById(typePayment).accept_installment
    });
  };

  var showChequeForm = function(notClear) {
    if (!notClear) {
      $("#chequeInfo :input:not(.success)").val("");
      $("#cheque_value").val($("#payment_type_value").val());
    }
    $("#chequeInfo").modal({
      "show": true,
      "keyboard": true,
      "backdrop": true
    });

  };

  var validateChequeInfos = function(chequeInfo) {
    if (chequeInfo.cheque_value > $('#payment_type_value').val()) {
      throw "O valor do cheque deve ser o mesmo do pagamento!";
    }
    for (var i in chequeInfo) {
      if (chequeInfo[i] === "" && i != 'account') {
        throw "Existem informações em branco no cheque por favor preencha todos os campos!";
      }
    }
  };
  var ajustPrice = function(percent) {
    for (var i = treatments.length - 1; i >= 0; i--) {
      var treatment = treatments[i];
      for (var j = treatment.activitys.length - 1; j >= 0; j--) {
        treatment.activitys[j].price += treatment.activitys[j].price * percent;
      };
    };
    saveTreatments();
    setValuesToUi();
    preparePaymentsInUi();
  }
  var editQtd = function(treatmentIndex, activityIndex, element) {
    var value = $(element).val();
    if (!value || value == "") { // 02/03/2018 rigel
      value = "1"; // evita exception na conversao pra número
    }
    treatments[treatmentIndex].activitys[activityIndex].amount = parseInt(value);
    saveTreatments(); // rigel 02/2017
    prepareTreatmentsInUi();
  }
  var getCommandByServer = function(customerId) {
    // 13/10/2017 - rigel para nao deixar pagar conta cliente e credito cliente na mesma transação
    if (prodCustomerAccount == 0 && prodCustomerCredit == 0) {
      var url = "/cash/getProductPreviousDebts/" + customerId;
      $.get(url, function(t) {
        eval("var prodObj = " + t);
        // 13/10/2017 rigel - salva o produto conta cliente
        prodCustomerAccount = prodObj.id
        prodCustomerCredit = prodObj.credit
      }, "text");
    }  

    $.get("/cash/getCommand/" + customerId + "/" + gatDateTreatmentOr0(), function(t) {
      var commandData = {};
      eval("commandData=" + t);
      if (!commandData.isNew || $("#command").val() == "") {
        clearData();
        /*if(commandData.command == "" || commandData.command == "0"){
          prompt("Existe um atendimento para este cliente porem ainda não possui numero de comanda. Por favor inform!");
        }*/
        $("#command").val(commandData.command);
        if (!commandData.isNew) {
          getTreatmentbyCommand(false, customerId);
        } else {
          // parametrizar aqui o select prof automaticamente
          $("#user").select2('open');
        }
      }
    });
  }
  var resolveTreatments = function(treatments, customer) {
    var treatStatus = function() {
      var customers = {};
      var numOfcustomers = 0;
      var treatmentsOpen = treatments.filter(function(t) {
        //return t.status != "Paid";
        return t.status != 4; // paid 
      });
      // rigel 16/03/2018
      if (treatmentsOpen.length > 0) {
        $('#valueinpoints').val(treatmentsOpen[0].customerValueInPoints);
        if (treatmentsOpen[0].customerValueInAccount < 0) {
          $('#valueinaccount').val(treatmentsOpen[0].customerValueInAccount.formatMoney());
        } else if (treatmentsOpen[0].customerValueInAccount > 0) {
          $('#valueinaccountcredit').val(treatmentsOpen[0].customerValueInAccount.formatMoney());
        }
      }        
      treatmentsOpen.forEach(function(item) {
        if (customers[item.customerId]) {
          customers[item.customerId].push(item);
        } else {
          numOfcustomers++;
          customers[item.customerId] = Array(item);
        }
      });
      if (numOfcustomers > 1) {
        if (customer) {
          return customers[customer] || [];
        } else {
          alert("Existem " + numOfcustomers + " clientes com esse número de comanda por favor selecione um cliente");
          for (var i in customers) {
            if (confirm("Deseja utilizar " + customers[i][0].customerName)) {
              return customers[i];
            }
          }
          return [];
        }
      } else {
        return treatments;
      }
    };
    var prepareParrentDelivery = function(treatments) {
      treatments.forEach(function(treatment) {
        treatment.activitys.forEach(function(activity) {
          if (activity.parentBom) {
            //console.log (activity)
            var parent = treatment.activitys.filter(
              function(candidateParent) {
                //console.log (candidateParent.activityId)
                return candidateParent.activityId == activity.parentBom;
              }
            );
            if (parent.length > 0) {
              parent[0].parent_delivery = true;
            }
          }
        });
      });
      return treatments;
    }
    var treatmentsDelivery = prepareParrentDelivery(treatStatus()).filter(function(t) {
      var ignored = globals_treatments_to_ignore.indexOf(t.id) != -1;
      t.ignored = ignored;
      return !ignored;
    });

    var treatmentsAnotherUnit = treatmentsDelivery.filter(function(treatment) {
      // tive problema com 80 caixas abertos na renata valentina
      // para esquema de franquia trazer so da unidade selecionada
      //
      return global_cashier.unit_id != treatment.unit;
    });
    if (treatmentsAnotherUnit.length > 0 && !confirm("Existem atendimentos de uma unidade diferente do caixa!\n Deseja continuar?")) {
      treatmentsDelivery = [];
    }
    return treatmentsDelivery;
  };
  var processPrice = function() {
    treatments.forEach(function(treatment) {
      for (i in treatment.activitys) {
        var activity = treatment.activitys[i];
        if (Customer.hasDelivery(activity.activityId)) {
          activity.price = Customer.useDeliveryReturnPrice(activity.activityId);
        } else if (Customer.hasBudget(activity.activityId)) {
          activity.price = Customer.useBudgetReturnPrice(activity.activityId);
        }
      }
    });
    prepareTreatmentsInUi();
  };
  var processOffSale = function() {
    $("#offsale").val(Customer.current.offsale).change();
  };
  $(function() {
    $("#cashiers_select").change(function() {
      global_cashier = $("#cashiers_select")[0].cashiers_map[$(this).val()];
      // troca a unidade corrente para unidade do caixa se for diferente
      // rigel 27/02/2017
      if (global_cashier.unit_id != AuthUtil.unit.id) {
        $.get("/system/changeUnit/"+global_cashier.unit_id,function(t){
            //alert("Unidade alterada com sucesso!");
        });
      }
    });
    $("#bank").change(function() {
      $("#cheque_number").focus();
    });
    $("#cash_form").submit(function() {
      return false;
    });
    Customer.addonsListeners.push(processPrice);
    Customer.addonsListeners.push(processOffSale);
    $("#bank").bankField();
    $("#payment_type_value").keypress(function(e) {
      if (e.keyCode == 13) {
        $("#add_payment").click();
        return false;
      }
    });
    $('#activity').change(function() {
      if ($(this).val() != "") {
        $("#add_activity").focus();
      }
    });
    $('#product').change(function() {
      if ($(this).val() != "") {
        $("#add_prduct").focus();
      }
    });
    $('#payment_type').change(function() {
      $(".b_payment_finalize:last").focus();
    });

    $("#command").keypress(function(e) {
      if (e.keyCode == 13) {
        $("#command_get_from_server").click();
        return false;
      }
    });
    $("#command").keydown(function(e) {
      if (e.keyCode == 9) {
        $(".name_customer_search:visible").select2('open');
        return false;
      }
    });
    $(".id_customer_search[name=customer]").change(function() {
      if ($("#command").val() == "" || treatments.length == 0) {
        getCommandByServer($(this).val());
      } else {
        if (treatments.length > 0) {
          if (treatments[0].customerId != $(this).val()) {
            if (confirm("Deseja iniciar novo Atendimento?")) {
              clearData();
              getCommandByServer($(this).val());
            } else {
              $(this).val(treatments[0].customerId);
              $("#user").select2('open');
            }
          }
        } else {
          $("#user").select2('open');
        }
      }
      var hasPetSystem = $('.has-pet-system').length > 0;
      if (hasPetSystem) {
        getAnimals();
      }
    });
    $("#activity").select2({
      formatResult: Customer.hasEspecialFormat,
      formatSelection: Customer.hasEspecialFormat
    });
    mousePrepare();
    if (gup("command")) {
      setTimeout(function() {
        $("#date_treatment").val(gup("date")).change();
        getTreatmentbyCommand();
        $("#command").addClass("disabled");
        $("#command").attr("disabled", true);
      }, 500);
      $("#command").val(gup("command"));
    } else {
      setTimeout(function() {
        $("#command").focus();
      }, 500);
    }
  });
