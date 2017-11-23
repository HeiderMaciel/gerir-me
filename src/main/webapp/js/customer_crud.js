$(function() {
  var emailValidation = /^([a-z0-9._%\-+]+@(?:[a-z0-9\-]+\.)+[a-z]{2,4}$)/;
  var nameInvalidChars = ["(",")","/","*","-",".",",","|","+","?",":","0","1","2","3","4","5","6","7","8" , "9"];
  var CustomerValidations = {
    validate : function(customerName){
      var nameValid =  !nameInvalidChars.some(function(charapter){
        return customerName.indexOf(charapter) != -1;
      });
      var contactLength =  customerName.length > 3;
      alert("nome ===>"+customerName);
      return {customerName : nameValid, customerNameLength : contactLength}
    }
  }
  // errado com 1 de proposito pq nao pega o conteudo do name por causa do snippet
  $("#customer_form1").submit(function(){
    var message = "";
    var customerValidate = CustomerValidations.validate($("#name").val());
    if(!customerValidate.customerName){
      message  += 'Existem caracteres especiais no nome do cliente, remova-os antes de continuar!\n';
    }
    if(!customerValidate.customerNameLength){
      message  += 'Nome não pode conter menos de 4 caracteres!!\n';
    }
    if(message){
      alert(message);
    }
    return  customerValidate.customerName 
        && customerValidate.customerNameLength;
  })
  var renderHistory = function() {
    var filter = {
      customer: gup("id"),
      startDate: $("#history_startDate").val(),
      endDate: $("#history_endDate").val()
    };
    var hasUnitModule = $('.has-unit-module').length > 0;
    var fields = [];
    //fields[0] = "date";
    fields[0] = "time";
    fields[1] = "none";
    fields[2] = "none";
    if (!hasUnitModule) {
      fields[5] = "none";
    }
    fields [6] = {
      type : "format",
      decode: function(name, row) {
        trStatus = 6;
        trStatus2 = 15; // nao tem
        return trStatusdecode (name,row)
      }
    };
/*
    fields[6] = {
      type: "format",
      decode: function(status, row) {
    // duplicado no treatmentManger.js
          //alert (" vaiiiii ======== " + row[15])
          var status2 = row[15]
          var statstr = '' 
          var title = ''
          if (status == 'Open' || status == '0') {
            statstr = 'open'
            title ='agendado'
          } else if (status == 'Missed' || status == '1') {
            statstr = 'Missed'
            title ='faltou'
          } else if (status == 'Arrived' || status == '2') {
            statstr = 'Arrived'
            title ='chegou'
          } else if (status == 'Ready' || status == '3') {
            statstr = 'Ready'
            title ='atendido'
          } else if (status == 'Paid' || status == '4') {
            if (status2 == 'Ready' || status2 == '3') {
              statstr = 'ready_paid'
              title ='atendido / pago'
            } else if (status2 == 'Missed' || status2 == '1') {
              statstr = 'missed_paid'
              title ='faltou / pago'
            } else {
              statstr = 'paid'
              title ='pago'
            }
          } else if (status == 'Deleted' || status == '5') {
            statstr = 'Deleted'
            title ='excluído'
          } else if (status == 'Confirmed' || status == '6') {
            statstr = 'Confirmed'
            title ='confirmado'
          } else if (status == 'PreOpen' || status == '7') {
            statstr = 'PreOpen'
            title ='pré agendado'
          } else if (status == 'ReSchedule' || status == '8') {
            statstr = 'ReSchedule'
            title ='desmarcou'
          } else if (status == 'Budget' || status == '9') {
            statstr = 'Budget'
            title ='orçamento'
          }
        // "<img title='" + title + "'' src='/images/treatment_" + statstr.toLowerCase() + ".png' width='24'/>" +
        return "<img title='" + title + "' src='/images/treatment_"+statstr.toLowerCase()+"1.png' width='24'/>";
      }
    };
*/
    fields[7] = "real";
    fields[12] = "none";
    fields[14] = "none";
    renderReport("/treatments/getTreatmentsByFilter", fields, filter, "#grid_history");
  }
  if (gup("id")) {
    //To create events
    setTimeout(function(){    
      if($("#event_link").length > 0){
        $("#event_link").attr("href", $("#event_link").attr("href").replace("##", gup("id")));
      }
      if($("#stakeholder_link").length > 0){
        $("#stakeholder_link").attr("href", $("#stakeholder_link").attr("href").replace("##", gup("id")));
      }
      if($("#bprelationship_link").length > 0){
        $("#bprelationship_link").attr("href", $("#bprelationship_link").attr("href").replace("##", gup("id")));
      }
      if($("#bprelationship_link1").length > 0){
        $("#bprelationship_link1").attr("href", $("#bprelationship_link1").attr("href").replace("##", gup("id")));
      }
      // inserir pet como dono / tutor
      if($("#pet_link").length > 0){
        $("#pet_link").attr("href", $("#pet_link").attr("href").replace("##", gup("id")));
      }
      // inserir pet como indicação
      //if($("#pet_indic_link").length > 0){
      //  $("#pet_indic_link").attr("href", $("#pet_indic_link").attr("href").replace("##", gup("id")));
      //}
      if($("#new_pet_link").length > 0){
        $("#new_pet_link").attr("href", $("#new_pet_link").attr("href").replace("##", $("#bp_manager").val()));
      }
    },600);
    var renderAccount = function() {
      $.get("/treatments/customer_account/" + gup("id"), function(data) {
        var dataset = [];
        eval("dataset=" + data);
        var total = dataset.total;
        var table = "";
        for (var i in dataset.details) {
          var detail = dataset.details[i];
          var date_payment = getDateBr(new Date(detail.due_date));
          var url = "/financial_cashier/comman_conference?startDate=" + encodeURIComponent(date_payment) + "&endDate=" + encodeURIComponent(date_payment) + "&commands=" + detail.command + "&cashier=" + detail.cashier;
          table += "<tr><td>" + date_payment + "</td><td>" + (detail.value).formatMoney() + "</td><td>Atendimento/Produto</td><td>" + detail.command + "</td><td><a href='" + url + "'>Ir</a></td></tr>";
        }
        if (!total) total = 0;
        $("#value").val((total).formatMoney());
        $("#grid tbody").html(table);
      });
    }

    var renderIndications = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[2] = 'date'
      fields[3] = {
        type: "format",
        decode: function(value) {
          return '<a href="/customer/edit?id=' + value + '" class="btn sucess">Ir</a>';
        }
      }
      renderReport("/customer_report/indications", fields, filter, "#grid_indications");
    }   
    var renderConsiderations = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[1] = {
        type: "select",
        selector: "#considerations_notification_type"
      }
      fields[2] = "date"
      fields[3] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger consideration_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".consideration_remove").click(function() {
          if (confirm("Deseja excluir o Alerta sobre o cliente?")) {
            var id = $(this).data("id");
            $.ajax("/customer_api/consideration/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Alerta excluído com sucesso!");
                renderConsiderations();
              }
            })
          }
        });
      }
      renderReport("/customer_report/considerations", fields, filter, "#grid_considerations", bindEvent);
    }
    var renderQuiz = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[0] = "date"
      fields[1] = "age"
/*
      fields[1] = {
        type: "select",
        selector: "#quiz_quiz"
      }
*/

      fields[5] = {
        type: "format",
        decode: function(value) {
          // tazer flag no rel e testar a chamada
          //'<span style="margin-right:4px"><a class="btn" target="_quiz_maste"  href="/quiz/quiz_applying?applying_id='+value+'">Editar</a></span>'+
          return '<span style="margin-right:4px"><a class="btn" target="_quiz_maste"  href="/quiz/quizapply?id='+value+'">Texto</a></span>'+
                '<span style="margin-right:4px"><a class="btn" target="_quiz_maste"  href="/quiz/quiz_applying?applying_id='+value+'">Questões</a></span>'+
                '<input data-id="' + value + '" type="button" class="btn danger quiz_remove" value="Excluir">';
        }
      }
      fields[6] = "none"
      fields[7] = "none"
      fields[8] = "none"
      fields[9] = "none"
      fields[10] = "none"
      var message_total = "";
      var bindEvent = function() {
        $(".quiz_remove").click(function() {
          if (confirm("Deseja excluir o questionário do cliente?")) {
            var id = $(this).data("id");
            $.ajax("/api/v2/quiz/" + id, {
              "type": "DELETE",
              "success": function(results) {
                if(results === 1 || results == "1"){
                  alert("Questionário excluído com sucesso!");
                  renderQuiz();
                  $('#message_total').jqte();
                }else{
                  alert(eval(results));
                }
              }
            })
          }
        });
      }
      renderReport("/customer_report/quiz", fields, filter, "#grid_quiz", function(data){
        var dataAnt = ""      
        var sessionAnt = ""  
        //message_total = "<h1>Paciente ";
        var parceiro = "";
        var hasMedicalSystem = $('.has-medical-system').length > 0;
        if (hasMedicalSystem) {
          parceiro = "Paciente"
        } else {
          parceiro = "Cliente"
        }

        message_total = ""
        data.forEach(function(row){
          //if (message_total == "<h1>Paciente ") {
          if (message_total == "") {
              message_total += "<h1>" + parceiro + " " + row[10] + "</h1><br><br>"        
          }
          if (row[7] != "" && row[9] == 'true') {
            if (dataAnt != row[0]) {
                dataAnt = row[0];
                message_total += "<span style='font-size:18px; color: #049cdb; font-weight:bold;'>" 
              + dateDecode (row[0]) + " - " + ageDecode(row[1])+"</span><br><br>" 
            }
            if (sessionAnt != row[2]) {
              sessionAnt = row[2];
              message_total += "<span style='font-size:16px; font-weight:bold;'>" + row[2] + "</span><br><br>"
            }
            message_total += row[8] + "<br>";
          }
        });
        bindEvent();
        $("#message_total").val(message_total);
        //$('#message_total').jqte();
        $("#message_total1").html(message_total);
      });

    }
    var renderBpmonthly = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[1] = {
        type: "select",
        selector: "#bpmonthly_product"
      }
      fields[2] = "date"
      fields[3] = "date"
/*
      fields[8] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger bpmonthly_remove" value="Excluir">';
        }
      }
*/
      fields[8] = {
        type: "format",
        decode: function(value) {
          return '<span style="margin-right:4px"><a class="btn" target="_bpmonthly_maste"  href="/bpmonthly/bpmonthly?id='+value+'">Editar</a></span>'+
          '<span><input data-id="' + value + '" type="button" class="btn danger bpmonthly_remove" value="Excluir"></span>';
        }
      }
      var bindEvent = function() {
        $(".bpmonthly_remove").click(function() {
          if (confirm("Deseja excluir a mensalidade do cliente?")) {
            var id = $(this).data("id");
            $.ajax("/api/v2/bpmonthly/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Mensalidade excluída com sucesso!");
                renderBpmonthly();
              }
            })
          }
        });
      }
      renderReport("/customer_report/bpmonthly", fields, filter, "#grid_bpmonthly", bindEvent);
    }
    var renderBankAccount = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[4] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger bankaccount_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".bankaccount_remove").click(function() {
          if (confirm("Deseja excluir a Conta Bancária do cliente?")) {
            var id = $(this).data("id");
            $.ajax("/customer_api/bankaccount/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Conta bancária excluída com sucesso!");
                renderBankAccount();
              }
            })
          }
        });
      }
      renderReport("/customer_report/bankaccount", fields, filter, "#grid_bankaccount", bindEvent);
    }

    var renderRelationship = function() {
      var defineEdit = function (customer_id) {
        window.open("/customer/edit?id=" + customer_id,"_customer_maste")
      }
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[1] = {
        type: "format",
        decode: function(name, row) {
          if (row[6] == "true") {
            return "<a href='/animal/edit_animal?id=" + row[5] + "' target='_animal_maste'>" + name + "</a>";
          } else {
            return "<a href='/customer/edit?id=" + row[5] + "' target='_customer_maste'>" + name + "</a>";
          }
        }
      }
      fields[2] = "dateTime";
      fields[4] = {
        type: "format",
        decode: function(value) {
          return '<span style="margin-right:4px"><a class="btn" target="_relationshipt_maste"  href="/bprelationship/bprelationship?id='+value+'">Editar</a></span>'+
          '<span><input data-id="' + value + '" type="button" class="btn danger relationship_remove" value="Excluir"></span>';
        }
      }
      fields[5] = 'none'
      fields[6] = 'none'
      var bindEvent = function() {
        $(".relationship_remove").click(function() {
          if (confirm("Deseja excluir o relacionamento do cliente?")) {
            var id = $(this).data("id");
            $.ajax("/customer_api/relationship/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Relacionamento excluído com sucesso!");
                renderRelationship();
              }
            })
          }
        });
      }
      renderReport("/customer_report/relationship", fields, filter, "#grid_relationship", bindEvent);
    }

    var renderEvent = function() {
      var filter = {
        customer: gup("id"),
        event_all:$("#event_all").val()
      };
      var fields = [];
      fields[0] = {
        type: "format",
        decode: function(name, row) {
          return "<a href='/project/event?id=" + row[6] + "' target='_blank'>" + name + "</a>";
        }
      }
      fields[2] = "dateTime"
      fields[4] = "none"
      fields[5] = {
        type: "format",
        decode: function(value) {
          return '<span style="margin-right:4px"><a class="btn" target="_stakeholder_maste"  href="/project/edit_stakeholder?id='+value+'">Editar</a></span>'
          //+'<span><input data-id="' + value + '" type="button" class="btn danger relationship_remove" value="Excluir"></span>';
        }
      }
      fields[6] = 'none'
      renderReport("/customer_report/stakeholder", fields, filter, "#grid_event");
    }

    var renderClass = function() {
      var filter = {
        customer: gup("id"),
      };
      var fields = [];
      fields[0] = {
        type: "format",
        decode: function(name, row) {
          return "<a href='/project/event?id=" + row[6] + "' target='_blank'>" + name + "</a>";
        }
      }
      fields[2] = "dateTime"
      fields[4] = "approved"
      fields[5] = {
        type: "format",
        decode: function(value) {
          return '<span style="margin-right:4px"><a class="btn" target="_stakeholder_maste"  href="/project/edit_stakeholder?id='+value+'">Editar</a></span>'
          //+'<span><input data-id="' + value + '" type="button" class="btn danger relationship_remove" value="Excluir"></span>';
        }
      }
      fields[6] = 'none'
      renderReport("/customer_report/stakeholder_class", fields, filter, "#grid_class");
    }

    $.get("/treatments/customer_delivery/" + gup("id"), function(data) {
      var dataset = [];
      eval("dataset=" + data);
      var table = "";
      for (i in dataset) {
        var total = dataset[i].un_used + dataset[i].used;
        table += "<tr><td>" + dataset[i].delivery_name + "</td>";
        table += "<td>" + getDateBr(new Date(dataset[i].date)) + "</td>";
        table += "<td>" + dataset[i].product_name + "</td>";
        table += "<td>" + dataset[i].cashier + "</td>"
        table += "<td>" + dataset[i].command + "</td>"
        table += "<td>" + (dataset[i].saleprice).formatMoney() + "</td>";
        table += "<td>" + total + "</td>";
        table += "<td>" + dataset[i].used + "</td>";
        table += "<td>" + dataset[i].un_used + "</td></tr>";
      }
      $("#grid_package tbody").html(table);
    });

    // botão no edit_patient
    $("#edit_customer").click(function(){
        var hasPetSystem = $('.has-pet-system').length > 0;
        if (hasPetSystem) {
          window.open("/animal/edit_animal?id=" + gup("id"),"_animal_maste")
        } else {
          window.open("/customer/edit?id=" + gup("id"),"_customer_maste")
        }
    });

    $("#print_message").click(function(){
        var message_print = $('#message_total').val();
        var header = "";
        if ($('.has-pet-system').length > 0) {
          header = "ebellepet"
        } else if ($('.has-edoctus-system').length > 0) {
          header = "edoctus"
        } else if ($('.has-ephysio-system').length > 0) {
          header = "ephysio"
        } else if ($('.has-gerirme-system').length > 0) {
          header = "gerirme"
        } else {
          header = "ebelle"
        }

        var logo = "";
        if ($('.has-pet-system').length > 0) {
          logo = "ebellepet"
        } else if ($('.has-edoctus-system').length > 0) {
          logo = "edoctus"
        } else if ($('.has-ephysio-system').length > 0) {
          logo = "ephysio"
        } else if ($('.has-gerirme-system').length > 0) {
          logo = "gerirme"
        } else {
          logo = "ebelle"
        }

        // um trailer gerar 
        //header = '<img width="70px" style="padding-right: 10px" src="/images/logo_'+logo+'.png"/>' + header + " Gestão Integrada <br></p>"
        header = "<head> " + 
          " <meta charset='utf-8'> " +
          " <title>" + header + "</title> " +
          " </head> " +
        '<img width="35px" style="padding-right: 10px" src="/images/logo_ftr_'+logo+'.png"/>' + 
        header + " Gestão Integrada " + 
        '<img width="70px" style="padding-right: 10px" src="http://nb.vilarika.com.br/images/company/'+AuthUtil.company.image+'"/>' + 
        " <br></p>"
        var printWindow = window.open("", "MsgPrintWindow");
        printWindow.document.write(header + message_print);
        printWindow.print();
    });

    $("#event_all").change(renderEvent);
    $("#history_startDate").change(renderHistory);
    $("#history_startDate").val(getDateBr(new Date().getAYearAgo()));
    $("#history_endDate").change(renderHistory);
    $("#history_endDate").val(getDateBr(new Date()));
    renderAccount();
    renderConsiderations();
    renderQuiz();
    renderBpmonthly();
    renderIndications();
    renderHistory();
    renderBankAccount();
    renderRelationship();
    renderEvent();
    renderClass();
    
    $("#considerations_add").click(function() {
      $.post("/customer_api/consideration", {
        type: $("#considerations_notification_type").val(),
        obs: $("#considerations_obs").val(),
        customer: gup("id")
      }, function() {
        alert("Alerta adicionado com sucesso!");
        renderConsiderations();
      });
    });
    $("body").on('click', '.quiz_addd', function() {
      var button = $(this);

      var id = button.data('id');
      $("#quiz_quiz").val(id);
      $("#quiz_quiz").change();
      $("#quiz_add").click();
    });

    $("#quiz_add").click(function() {
      //alert ($("#quiz_quiz").val());
      if ($("#quiz_quiz").val() == "0" || $("#quiz_quiz").val() == ""
          || $("#quiz_quiz").val() == null) {
        alert ("Um modelo de prontuário/questionário precisa ser selecionado")
        return
      }
      $.post("/api/v2/quiz", {
        date: $("#quiz_date").val(),
        quiz: $("#quiz_quiz").val(),
        obs: $("#quiz_obs").val(),
        customer: gup("id")
      }, function(results) {
        // TESTAR o message para ver o tipo de abertura
        window.open("/quiz/quizapply?id=" + results, "_quiz_maste")
        // alert("Questionário adicionado com sucesso!");
        renderQuiz();
        $('#message_total').jqte();
      });
    });
    $("#bpmonthly_add").click(function() {
      $.post("/api/v2/bpmonthly", {
        product: $("#bpmonthly_product").val(),
        obs: $("#bpmonthly_obs").val(),
        customer: gup("id")
      }, function() {
        alert("Mensalidade adicionada com sucesso!");
        renderBpmonthly();
      });
    });
    $("#bankaccount_add").click(function() {
      $.post("/customer_api/bankaccount", {
        bank: $("#bankaccount_bank").val(),
        agency: $("#bankaccount_agency").val(),
        account: $("#bankaccount_account").val(),
        obs: $("#bankaccount_obs").val(),
        customer: gup("id")
      }, function() {
        alert("Conta bancária adicionada com sucesso!");
        renderBankAccount();
      });
    });    
  }
  $(".quiz_addd").click(function() {
//  $("#quiz_addd16").click(function() {
    //alert ("vaiii -- cliquei");
  });
/*
  Mudei a estratégia estava, o timeout com change era para mostrar o nome
  dos parceiros qdo registro já existia, entrava em loop em determindas 
  situações qdo os 2 parceiros era exibidos na tela. ficava flicando um e outro
  e parece que ao salvar flicando ora salva um ora outro.
  criei metodos no customer para trazer o nome pronto caso já existam e setei 
  via snippet, se o parceiro for alterado seta o nome normalmente. O timeout 
  ficou só no bp_manager que é menos comum que o indicated_by

  observar - rigel 20/06/2017

  setTimeout(function() { //toIndicatedBy
    if ($('#bp_indicatedby').val() != "" && $('#bp_indicatedby').val() != "0") {
      $('#bp_indicatedby').change();
    }
*/
    if ($('#bp_manager').val() != undefined) {
      setTimeout(function() { //toManager
        if ($('#bp_manager').val() != "" && $('#bp_manager').val() != "0") {
          $('#bp_manager').change();
        }
      }, 600);
    }
/*/
  }, 100);
*/

  $("#bankaccount_bank").bankField();
  $("#quiz_quiz").quizField();
  $("#bpmonthly_product").activityField();
})
