// Generated by CoffeeScript 1.6.3
(function() {
  var AccountPayable, AccountMovement, Category, accountsId;
  var callApiLock = false;

  var chequeid = ""

  var hasFinancialadModule = false;

  accountsId = [];

  Category = (function() {
    function Category() {
    }

    return Category;

  })();

  Category.list = [];

  Category.getListFromServer = function() {
    $.get("/account/category/list/all", function(results) {
      var i, ret, _i, _len;
      eval("results = " + results);
      ret = "";
      for (_i = 0, _len = results.length; _i < _len; _i++) {
        i = results[_i];
        ret += "<option value='" + i.id + "'>" + i.name + "</option>";
      }
      $("#account_categories_filter:input").html(ret);
      $("#account_categories_filter:input").val($("#account_categories_filter1:input").val());
      $("#account_categories_filter").change();
      return $("#account_categories_filter").prepend("<option value='SELECT_ALL'>Todas</option>");
    });
    return $.get("/account/category/list", function(results) {
      var i, ret, _i, _len;
      eval("results = " + results);
      Category.list = results;
      ret = "";
      for (_i = 0, _len = results.length; _i < _len; _i++) {
        i = results[_i];
        ret += "<option value='" + i.id + "'>" + i.name + "</option>";
      }
      $("#category_select:input").html(ret);
      return $("#category_select:input").change();
    });
  };

  AccountMovement = (function() {
    function AccountMovement() {}

    return AccountMovement;

  })();

  AccountMovement.list = [];

  AccountMovement.getById = function(id) {
    var category, _i, _len, _ref;
    if (id) {
      _ref = AccountMovement.list;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        category = _ref[_i];
        if (category.id === parseInt(id)) {
          return category;
        }
      }
    }
  };

  Category.getById = function(id) {
    var category, _i, _len, _ref;
    _ref = Category.list;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      category = _ref[_i];
      if (category.id === parseInt(id)) {
        return category;
      }
    }
  };

  AccountPayable = (function() {
    var validate;

    function AccountPayable() {
      this.value = $("#value").val();
      this.dueDate = $("#date").val();
      this.exerciseDate = $("#exerciseDate").val();
      this.paymentDate = $("#paymentDate").val();
      this.paid = $("#paid").is(':checked');
      this.obs = $("#obs_account").val();
      this.complement = $("#complement_account").val();
      this.category = $("#category_select").val();
      this.recurrence = $("#recurrence").is(':checked');
      this.recurrence_type = $("#recurrence_type").val();
      this.recurrence_term_type = $("#recurrence_term_type").val();
      this.recurrence_term = $("#recurrence_term").val();
      this.user = $("#user_select").val();
      this.type = AccountPayable.getType();
      this.user_parceled = $("#user_parcels").val() > 1;
      this.user_parcels = $("#user_parcels").val();

      this.out_of_cacashier = $("#cashiers_select").val() !== "" || ($("#cashier_number").val() !== "" && parseFloat($("#cashier_number").val()) != 0);
      this.account = $("#account_select").val();
      this.cashier = $("#cashiers_select").val();
      this.cashier_number = $("#cashier_number").val();

      this.out_of_cacashier_to = $("#cashiers_select_to").val() !== "" || ($("#cashier_number_to").val() !== "" && parseFloat($("#cashier_number_to").val()) != 0);
      this.account_to = $("#account_select_to").val();
      this.cashier_to = $("#cashiers_select_to").val();
      this.cashier_number_to = $("#cashier_number_to").val();

      this.transfer = $("#type_select option:checked").data('transfer') === true;
      this.amount = $("#amount").val();
      this.parcelnum = $("#parcelnum").val();
      this.parceltot = $("#parceltot").val();
      this.unit = $("#unit_select").val();
      this.costcenter = $("#costcenter_select").val();
      this.paymenttype = $("#paymenttype_select").val();
      // toString pq cheque agora é múltiplo
      if (!$("#cheque_select").val() || $("#cheque_select").val() == "") {
        this.cheque = "0"
      } else {
        this.cheque = $("#cheque_select").val().toString();
        if (this.cheque.indexOf(",")==0) {
          this.cheque = this.cheque.substring (1,this.cheque.length-1)
        }
      }
     // console.log(this.cheque)
      if (hasFinancialadModule) {
        this.ch_bank = $("#ch_bank").val();
        this.ch_agency = $("#ch_agency").val();
        this.ch_account = $("#ch_account").val();
        this.ch_number = $("#ch_number").val();
      }
      this.unitvalue = $("#unitvalue").val();
      this.recurrence_id = $("#recurrence_id").val();
      this.recurrence_all = $("#recurrence_all").is(":checked");
      this.recurrence_just_this = $("#recurrence_just_this").is(":checked");
      validate(this);
      if (this.cashier === "") {
        this.cashier = "0";
      }
    }

    validate = function(account) {
      var res = account.cheque.split(",");
      if (res.length > 1 && account.recurrence) {
        throw "Lançamento de múltiplos cheques não pode ser recorrente. Operação cancelada!";
      }

      var category;
      category = Category.getById(account.category);
      if (category.userAssociated) { 
        if (account.user === "0" || account.user == "") {
          if (!confirm("Tem certeza que deseja cadastrar um lançamento sem profissional para esta categoria?")) {
            throw "Operação cancelada!";
          }
        }
      }
      if (category.isparent) { 
            throw "Não é permitido fazer lançamento em categoria totalizadora. Operação cancelada!";
      }
      var accountAux;
      accountAux = AccountMovement.getById(account.account);
      if (accountAux && accountAux.allowCashierOut) {
        if (!account.cashier && 
         (!account.cashier_number || parseFloat(account.cashier_number) == 0)) {
          if (!confirm("Tem certeza que deseja cadastrar um lançamento numa conta caixa sem informar um caixa aberto?")) {
            throw "Operação cancelada!";
          }
        }
      }  
      if ((!account.value) || (parseFloat(account.value) == 0)) {
        if (!account.cheque) {
          if (!confirm("Tem certeza que deseja cadastrar um lançamento como valor zero?")) {
            throw "Operação cancelada!";
          }
        } else {
          // rigel 13/09/2017
          // o valor do cheque vai ser assumido como sendo 
          // o valor do lançamento
        }
      }
      if (!account.dueDate) {
        throw "Data é obrigatória!";
      }
      if (($("#date").datepicker("getDate").getTime() > (new Date().getTime())) && account.paid) {
        if (!confirm("Tem certeza que deseja cadastrar um lançamento como pago com o vencimento superior a hoje?")) {
          throw "Operação cancelada!";
        }
      }
    };

    return AccountPayable;

  })();

  AccountPayable.getType = function() {
    if ($("#type_select").val() === '2') {
      return 1;
    } else {
      return $("#type_select").val();
    }
  };

  AccountPayable.startDate = function() {
    return $("#start_date").val();
  };

  AccountPayable.startCreateDate = function() {
    return $("#start_create_date").val();
  };

  AccountPayable.startValue = function() {
    return $("#start_value").val();
  };

  AccountPayable.endCreateDate = function() {
    return $("#end_create_date").val();
  };

  AccountPayable.endDate = function() {
    return $("#end_date").val();
  };

  AccountPayable.endValue = function() {
    return $("#end_value").val();
  };

  AccountPayable.defaultDate = new Date();

  AccountPayable.addUpd_account = function (partiallySecure) {
    var e;
    if (!callApiLock) {
      callApiLock = true;
      try {
        if (AccountPayable.actualId) {
          //alert ("vaiii ==== " + $("#recurrence_id").val())
          if ($("#recurrence_all").is(":checked")) {
             if (confirm("Tem certeza que deseja atualizar este lançamento e inclusive os futuros?")) {
             } else {
              callApiLock = false
              return
             } 
          }
          var url = "";
          if (partiallySecure) {
            url = "/accountpayable/edit/" + AccountPayable.actualId + "/true";
          } else {
            url = "/accountpayable/edit/" + AccountPayable.actualId + "/false";
          }
          return $.post(url, new AccountPayable(), function(t) {
            if (t == 'true') {
              alert("Lançamento alterado com sucesso!");
              AccountPayable.getListFromServer();
              AccountPayable.actualId = false;
              $("#account_modal").modal({
                "hide": true
              });
              callApiLock = false;
              return;
            } else {
              callApiLock = false;
              return alert("Erro ao alterar lançamento! \n\n" + eval (t));
            }
          });
        } else {
          return $.post("/accountpayable/add", new AccountPayable(), function(results) {
            if($.isNumeric (results)){
              if (parseInt(results) == 1) {
                alert("Lançamento cadastrado com sucesso!");
              } else {
                alert(results + " (" + writtenForm (parseInt(results),"","")+ ") Lançamentos cadastrados com sucesso!");
              }
              AccountPayable.getListFromServer();
              AccountPayable.actualId = false;
              $("#account_modal").modal({
                "hide": true
              });
              callApiLock = false;
              return;
            } else {
              callApiLock = false;
              return alert("Erro ao cadastrar lançamento! \n\n" + eval (results));
            }
          });
        }
      } catch (_error) {
        e = _error;
        callApiLock = false;
        return alert(e);
      }
    } else {
      alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
      return;
    }
  };

  AccountPayable.editAccount = function(id) {
    var obj, _i, _len, _ref;
    _ref = AccountPayable.list;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      obj = _ref[_i];
      if (String(obj.id) === id) {
        chequeid = obj.cheque_id
        $("#cheque_select").chequeField(true, obj.cheque_id);
        AccountPayable.actualId = obj.id;
        $("#value").val(obj.value);
        $("#date").val(getDateBr(new Date(obj.dueDate)));
        $("#exerciseDate").val(getDateBr(new Date(obj.exerciseDate)));
        $("#paymentDate").val(obj.paymentDate !== "" ? getDateBr(new Date(parseInt(obj.paymentDate))) : "");
        $("#paid").attr("checked", obj.paid);
        $("#obs_account").val(obj.obs);
        $("#complement_account").val(obj.complement);
        $("#type_select").val(obj.type);
        $("#category_select").val(obj.category_id).change();
        $("#user_select").val(obj.user_id).change();
        $("#type_select").val(obj.type);
        $("#account_select").val(obj.account).change();
        $("#out_of_cacashier").attr('checked', obj.out_of_cacashier);
        $("#cashiers_select").val(obj.cashier_id).change();
        $("#recurrence").attr("checked", false);
        $("#amount").val(obj.amount);
        $("#parcelnum").val(obj.parcelnum);
        $("#parceltot").val(obj.parceltot);
        $("#unit_select").val(obj.unit_id).change();
        $("#costcenter_select").val(obj.costcenter_id).change();
        $("#paymenttype_select").val(obj.paymenttype_id).change();
        $("#cheque_account").val(obj.cheque_desc);
        setTimeout(function() {
        $("#cheque_select").val(chequeid).change();
        }, 1000);
        $("#unitvalue").val(obj.unitvalue);
//        $("#unitvalue").val(obj.unitvalue);
        $("#conciliate").val(obj.conciliate);
        $("#auditstr").val(obj.auditstr);
        if (obj.recurrence_id) {
          $('.recurrence_exists').show();
        } else {
          $('.recurrence_exists').hide();
        }
        if (obj.cashier_id && obj.cashier_id !== parseInt($("#cashiers_select").val())) {
          $("#cashier_number").val(obj.cashier);
        } else {
          $("#cashier_number").val("");
        }
        if (obj.paid && obj.cashier_id && obj.cheque_desc) {
          $('#b_mark_as_unpaid').show();
        } else {
          $('#b_mark_as_unpaid').hide();
        }
        if (obj.cashier_id || obj.conciliate > 0) {
          $('#b_upd_cashier_account').show();
        } else {
          $('#b_upd_cashier_account').hide();
        }
      }
    }
    return $("#account_modal").modal({
      "show": true,
      "keyboard": true,
      "backdrop": true
    });
  };

  AccountPayable.newAccount = function() {
    AccountPayable.actualId = null;
    $("#value").val(0.00);
    $("#paid").attr("checked", true);
    $("#recurrence").attr("checked", false);
    $("#obs_account").val("");
    $("#cheque_account").val("");
    $("#complement_account").val("");
    $("#recurrence_id").val("");
    $("#cashier_number").val("");
    $("#account_select").change();
    $("#unit_select").val(AuthUtil.unit.id).change();
    $("#cheque_select").val("").change();
    $("#unitvalue").val(0.0);
    $("#amount").val(1);
    $("#parcelnum").val(1);
    $("#parceltot").val(1);
    return $("#auditstr").val("");
  };

  AccountPayable.actualId = false;

  AccountPayable.deleteAccount = function(id, recid) {
    if (recid != "" && recid != "0") {
      if (confirm("Este laçamento é recorrente, deseja excluir inclusive os lançamentos futuros?")) {
        return $.get("/accountpayable/remove/" + id + "/" + recid, function(results) {
          if(results === 1 || results == "1"){
            alert("Lançamento(s) recorrente(s) excluído(s) com sucesso!");  
          }else{
            alert(eval(results));
          }
          
          return AccountPayable.getListFromServer();
        });
      } else {
        if (confirm("Tem certeza que deseja excluir o lançamento?")) {
          return $.get("/accountpayable/remove/" + id + "/" + recid, function(results) {
            if(results === 1 || results == "1"){
              alert("Lançamento excluído com sucesso!");  
            }else{
              alert(eval(results));
            }
            
            return AccountPayable.getListFromServer();
          });
        }
      }
    } else {
      if (confirm("Tem certeza que deseja excluir o lançamento?")) {
        return $.get("/accountpayable/remove/" + id + "/" + recid, function(results) {
          if(results === 1 || results == "1"){
            alert("Lançamento excluído com sucesso!");  
          }else{
            alert(eval(results));
          }
          
          return AccountPayable.getListFromServer();
        });
      }
    }
  };

  AccountPayable.list = [];

  AccountPayable.categoriesToFilter = function() {
    if ($('#account_categories_filter').val()) {
      return $('#account_categories_filter').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.showtransferToFilter = function() {
    if ($('#showtransfer_filter').is(":checked")) {
      return "1";
    } else {
      return "0";
    }
  };

  AccountPayable.cashiersToFilter = function() {
    if ($('#cashier').val()) {
      return $('#cashier').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.unitsToFilter = function() {
    if ($('#unit_select_filter').val()) {
      return $('#unit_select_filter').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.accountsToFilter = function() {
    if ($('#account_account_filter').val()) {
      return $('#account_account_filter').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.costCenterToSearch = function() {
    if ($("#costcenter_select_filter").val()) {
      return $("#costcenter_select_filter").val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.paymentTypeToSearch = function() {
    if ($("#paymenttype_select_filter").val()) {
      return $("#paymenttype_select_filter").val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.obsSearchTooFilter = function() {
    if ($('#obs_search').val()) {
      return $('#obs_search').val().toString();
    } else {
      return "";
    }
  };

  AccountPayable.dttype = function() {
    if ($('#dttype').val()) {
      return $('#dttype').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.statusToFilter = function() {
    if ($('#status_select_filter').val()) {
      return $('#status_select_filter').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.typesToFilter = function() {
    if ($('#type_select_filter').val()) {
      return $('#type_select_filter').val().toString();
    } else {
      return 0;
    }
  };

  AccountPayable.usersToFilter = function() {
    var user;
    user = $('#user').val();
    if (user !== "") {
      return $('#customer').val() + ", " + user;
    } else {
      return user;
    }
  };

  AccountPayable.getListFromServer = function(callBack) {
    var request;
    request = {
      "dttype": AccountPayable.dttype(),
      "start": AccountPayable.startDate(),
      "end": AccountPayable.endDate(),
      "status": AccountPayable.statusToFilter(),
      "categories": AccountPayable.categoriesToFilter(),
      "showtransfer": AccountPayable.showtransferToFilter(),
      "cashier": AccountPayable.cashiersToFilter(),
      "units": AccountPayable.unitsToFilter(),
      "users": AccountPayable.usersToFilter(),
      "startCreate": AccountPayable.startCreateDate(),
      "endCreate": AccountPayable.endCreateDate(),
      "types": AccountPayable.typesToFilter(),
      "startValue": AccountPayable.startValue(),
      "endValue": AccountPayable.endValue(),
      "accounts": AccountPayable.accountsToFilter(),
      "obsSearch": AccountPayable.obsSearchTooFilter(),
      "costcenters": AccountPayable.costCenterToSearch(),
      "paymenttypes": AccountPayable.paymentTypeToSearch()
    };
    if (request.start === "" && request.end === "" && request.startCreate === "" && request.endCreate === "") {
      return alert("Selecione pelo menos uma data!");
    } else {
      return $.post("/accountpayable/list", request, function(results) {
        var credit, debit, obj, ret, total, _i, _len;
        var hasUnitModule = $('.has-unit-module').length > 0;
        var hasCostcenterModule = $('.has-costcenter-module').length > 0;
        var aggregIcon = "";
        var aggregTitle = "";
        eval("results = " + results);
        AccountPayable.list = results;
        AccountPayable.ids = AccountPayable.list.map(function(item) {
          return item.id;
        });
        ret = "";
        total = 0;
        credit = 0;
        debit = 0;
        for (_i = 0, _len = results.length; _i < _len; _i++) {
          obj = results[_i];
          if (obj.type === 0) {
            total += obj.value;
            credit += obj.value;
          } else {
            total -= obj.value;
            debit += obj.value;
          }
          if (obj.aggregateId > 0) {
            if (obj.aggregateValue != 0.0) {
              aggregIcon = "<img src='/images/aggregate.png' width='18'/>"
              aggregTitle = " title='Total agredado = " + obj.aggregateValue.formatMoney() + "'";
            } else {
              aggregIcon = "<img src='/images/aggregate.png' width='14'/>"
              aggregTitle = "";
            }  
          } else {
            aggregIcon = "";
            aggregTitle
          }
          ret += "<tr>" + 
            "<td><input type='checkbox' class='account_payable' value='" + 
            obj.id + "'/></td>" +
            (obj.color == "" || obj.color == "#FFFFFF" ? "<td>" + 
            "<a " + aggregTitle + " href='#' data-id='" + 
            obj.id + "' class='action_edit'>" + obj.id + aggregIcon + "</a>" + "</td>" : 
            "<td style='background-color:" + obj.color + "'>" + 
            "<a " + aggregTitle + " href='#' data-id='" + 
            obj.id + "' class='action_edit'>" + 
            obj.id + aggregIcon + "</a>" + "</td>" ) + 
            "<td>" + (getDateBr(new Date(obj.dueDate))) + "</td>" +
//            "<td>" + obj.category + "</td>" +
            "<td>"+"<a href='/financial_admin/account_category?id="+obj.category_id+"' target='_customer_maste'>"+obj.category+"</a>"+"</td>" +
            "<td>" + obj.obs_trunc + "</td>" +
            "<td>" + 
            (obj.value.formatMoney()) + "</td>" + 
            //"<td>" + 
            //(obj.aggregateValue.formatMoney()) + "</td>" + 
            "<td><img src=\"/images/" + 
            (obj.type === 0 ? 'add' : 'remove') + ".png\"/></td><td><img src=\"/images/" + 
            (obj.paid ? 'good' : 'bad') + ".png\"/></td>" + 
            //"<td>" + obj.user_name + "</td></td>" +
            "<td>"+"<a href='/customer/edit?id="+obj.user_id+"' target='_customer_maste'>"+obj.user_name+"</a>"+"</td>" +
            (hasUnitModule ? "<td>" + obj.unit_name + "</td>" : "") + 
            "<td>" + obj.cashier + "</td>" +
            "<td>" + obj.account_name + "</td>" +
            (hasCostcenterModule ? "<td>" + obj.costcenter_name + "</td>" : "") + 
            "<td>" + obj.paymenttype_name + "</td>" + 
            (hasFinancialadModule ? "<td>" + 
              "<img width='16px' src=\"/images/" + 
              (obj.conciliate == "0" ? 'audit'     : (obj.conciliate == "1" ? 'tick'       : 'consolidate')) + ".png\" title=\"" + 
              (obj.conciliate == "0" ? 'em aberto' : (obj.conciliate == "1" ? 'conciliado' : 'consolidado')) + "\"/>" +
            "</a>" + "</td>" : "") + 
            "<td><a href='#' data-id='" + 
            obj.id + "' class='action_edit'><img src='/images/edit.png' /></a></td>" +
            "<td><a href='#' data-id='" + obj.id + "' data-recid='" + 
            obj.recurrence_id + "' class='action_delete'><img src='/images/delete.png'/></a></td>" +
            "</td><td><a title='" + obj.auditstr + "' href='#' ><img width='24px' src='/images/audit.png'/></a></td>" +
            "</tr>";
        }

        $("#print_message").click(function(){
            var message_print = $('#grid_dre').html();
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
            '<img width="70px" style="padding-right: 10px" src="http://images.vilarika.com.br/company/'+AuthUtil.company.image+'"/>' + 
            " <br></p>"
            var printWindow = window.open("", "MsgPrintWindow");
            printWindow.document.write(header + message_print);
            printWindow.print();
        });

        $("#grid tbody").html(ret);
        $(".action_edit").click(function() {
          return AccountPayable.editAccount($(this).attr("data-id"));
        });
        $(".action_delete").click(function() {
          return AccountPayable.deleteAccount($(this).attr("data-id"),
            $(this).attr("data-recid"));
        });
        $("#total").val(total.formatMoney());
        $("#credit").val(credit.formatMoney());
        $("#debit").val(debit.formatMoney());
        $("#ids").val(AccountPayable.ids.join(','));
        if (callBack) {
          return callBack();
        }
      });
    }
  };

  $((function() {
    $(".b_aggregate").click(function() {
      var checkeds, idsToMark;
      checkeds = $('.account_payable:checked').toArray();
      idsToMark = checkeds.map(function(item) {
        return $(item).val();
      });
      if (idsToMark.length < 1) {
         return alert("Não há nenhum lançamento marcado!");
      } else if (idsToMark.length < 2) {
         return alert("É preciso marcar pelo menos 2 (dois) lançamentos para agregá-los!");
      }
      if (confirm("Tem certeza que deseja agregar estes " + idsToMark.length + " laçamentos?")) {
        return $.post("/accountpayable/aggregate", {
          "ids": idsToMark.join(',')
        }, function(t) {
          if (t == 'true') {
            alert("Lançamentos agregados com sucesso!");
            return AccountPayable.getListFromServer();
          } else {
            return alert("Erro ao agregar lançamentos!\n\n" + eval (t));
          }
        });
      }
    });

    $(".b_mark_as_paid").click(function() {
      var checkeds, idsToMark;
      checkeds = $('.account_payable:checked').toArray();
      idsToMark = checkeds.map(function(item) {
        return $(item).val();
      });
      if (idsToMark.length < 1) {
         return alert("Não há nenhum lançamento marcado!");
      }
      if (confirm("Tem certeza que deseja marcar este(s) " + idsToMark.length + " laçamento(s) como pagos?")) {
        return $.post("/accountpayable/mark_as_paid", {
          "ids": idsToMark.join(',')
        }, function(t) {
          if (t) {
            alert("Lançamento(s) marcado(s) com sucesso!");
            return AccountPayable.getListFromServer();
          } else {
            return alert("Erro ao marcar lançamentos como pagos!");
          }
        });
      }
    });

    $(".b_remove_checked").click(function() {
      var checkeds, idsToMark;
      checkeds = $('.account_payable:checked').toArray();
      idsToMark = checkeds.map(function(item) {
        return $(item).val();
      });
      if (idsToMark.length < 1) {
         return alert("Não há nenhum lançamento marcado!");
      }
      if (confirm("Tem certeza que deseja excluir este(s) " + idsToMark.length + " lançamento(s)?")) {
        return $.post("/accountpayable/remove_checked", {
          "ids": idsToMark.join(',')
        }, function(results) {
          if(results === 1 || results == "1"){
            alert("Lançamento(s) excluído(s) com sucesso!");
          }else{
            alert(eval(results));
          }
          return AccountPayable.getListFromServer();
        });
      }
    });
    $("#check_all").click(function() {
      if ($(this).is(":checked")) {
        return $('.account_payable').attr("checked", true);
      } else {
        return $('.account_payable').removeAttr("checked");
      }
    });
    // estes campos estão no cadastro rápido de cliente
    // definido no template
    $("#rapid_customer_offsale").offSaleField(true);
    $("#rapid_customer_origin").originField(true);
    $("#rapid_customer_mobile_phone_op").operatorField(true);

    $("#user").userField();
    $(".user_select_span").hide();
    $(".cashier_select_span").hide();
    DataManager.getUsers(function(users) {
      var ret, user, _i, _len;
      ret = "<option value='0'> Selecione um profissional</option>";
      for (_i = 0, _len = users.length; _i < _len; _i++) {
        user = users[_i];
        ret += "<option value='" + user.id + "'>" + user.name + "</option>";
      }
      return $("#user_select").html(ret);
    });
    $("#unit_select_filter, #unit_select").unitField(false, false, true);
    $("#costcenter_select_filter, #costcenter_select").costcenterField(true);
    $("#paymenttype_select_filter, #paymenttype_select").paymentTypeField(true, true);

    $("#category_select").change(function() {
      var category;
      category = Category.getById($(this).val());
      $("#type_select").val(category.typeMovement);
      $("#name_customer").data('just_user', category.userAssociated);
      if (category.userAssociated) {
        $(".user_select_span").show();
      } else {
        $(".user_select_span").hide();
        $("#user_select").val("");
      }
      if (category.typeMovement == 2) {
        $(".cashier_select_to_span").show();
        $(".account_to").show();
        return 
      } else {
        $(".cashier_select_to_span").hide();
        $(".account_to").hide();
        return 
      }
      return $("#account_select").focus().select2('open');
    });
    $("#account_select").change(function() {
      var account;
      account = AccountMovement.getById($(this).val());
      if (account && account.allowCashierOut) {
        $(".cashier_select_span").show();
      } else {
        $(".cashier_select_span").hide();
        $("#cashiers_select").val("").change();
      }
      return $("#obs_account").focus();
    });
    $("#account_select_to").change(function() {
      var account;
      account = AccountMovement.getById($(this).val());
      if (account && account.allowCashierOut) {
        $(".cashier_select_to_span").show();
      } else {
        $(".cashier_select_to_span").hide();
        $("#cashiers_select_to").val("").change();
      }
      return $("#obs_account").focus();
    });
    $(".recurenc_container").hide();
    $(".recurrence_exists").hide();
    $("#recurrence").click(function() {
      if ($(this).attr("checked")) {
        return $(".recurenc_container").show();
      } else {
        return $(".recurenc_container").hide();
      }
    });
    Category.getListFromServer();
    $(".color_picker").miniColors({
      letterCase: 'uppercase'
    });
    $(".new_account").click(function() {
      AccountPayable.newAccount();
      // não faz sentido reverter lançamento novo
      $('#b_mark_as_unpaid').hide();
      // nem atualizar lançamento de caixa
      $('#b_upd_cashier_account').hide();
      $("#account_modal").modal({
        "show": true,
        "keyboard": true,
        "backdrop": true
      });
      $("#cheque_select").chequeField(true, "0");
      return setTimeout(function() {
        return $("#category_select").select2('open');
      }, 100);
    });
    $("#category_add_button").click(function() {
      return $("#category_rapid_add").modal({
        "show": true,
        "keyboard": true,
        "backdrop": true
      });
    });
    $("#category_remove_button").click(function() {
      var id;
      if (confirm("Tem certeza que deseja excluir a categoria?")) {
        id = $("#category_select").val();
        return $.get("/account/category/remove/" + id, function(results) {
          alert("Categoria excluída com sucesso!");
          return Category.getListFromServer();
        });
      }
    });
    $(".b_category_add").click(function() {
      return Category.save();
    });

    if ($("#start_date").val() == "") {
      $("#start_date").val(getDateBr(new Date(new Date().setDate(1))));
    }

    if ($("#end_date").val() == "") {
      $("#end_date").val(getDateBr(new Date()));
    }

    $(".b_search").click(function() {
      return AccountPayable.getListFromServer();
    });

    callApiLock = false;
    $(".b_add_account").click(function() {
      return AccountPayable.addUpd_account (false);
    });
    $(".b_upd_cashier_account").click(function() {
      if (confirm("Tem certeza que deseja alterar lançamento de caixa fechado e/ou conciliado?")) {
        return AccountPayable.addUpd_account (true);
      }
    });

    $(".b_mark_as_unpaid").click(function() {
      if (!callApiLock) {
        callApiLock = true;
        if (AccountPayable.actualId) {
          if (confirm("Tem certeza que deseja reverter o pagamento deste lançamento?")) {
            return $.post("/accountpayable/mark_as_unpaid", {
              "ids": AccountPayable.actualId
            }, function(t) {
              if (t) {
                alert("Lançamento revertido com sucesso!");
                AccountPayable.getListFromServer();
                AccountPayable.actualId = false;
                $("#account_modal").modal({
                  "hide": true
                });
                callApiLock = false;
                return;
              } else {
                alert("Erro ao reverter lançamento pago!");
                callApiLock = false;
                return;
              }
            });
          } else {
            callApiLock = false;
          }
        } else {
          alert ("Lançamento não existe portanto não pode ser revertido")
          callApiLock = false;
          return;
        }
      } else {
        alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
        return;
      }
    });

    $("#ch_bank").bankField();
    $("#cashier").cashierField(false, "all");
    $('.currency').calculator({
      showOn: 'button'
    });
    $("#cashiers_select").cashierField(true, "open");
    $("#cashiers_select_to").cashierField(true, "open");
    $("#type_select").change(function() {
      if ($("#type_select option:checked").data('transfer')) {
        $(".cashier_select_to_span").show();
        $(".account_to").show();
        return 
      } else {
        $(".cashier_select_to_span").hide();
        $(".account_to").hide();
        return 
      }
    });

    hasFinancialadModule = $('.has-financialad-module').length > 0;

    $("#unit").unitField(false, false, true);
    $("#costcenter").costcenterField();
    $("#paymenttype").paymentTypeField();
    // acho que este campo nao existe vaiiiiiii
    //$("#cheque").chequeField();
    $("#date").change(function() {
      return AccountPayable.defaultDate = $(this).datepicker("getDate") || new Date();
    });
    $(".account_select:input").accountField(false, function(items) {
      return AccountMovement.list = items;
    });

    $(".cashier_select_to_span").hide();
    $(".account_to").hide();

    // rigel 23/08/2017 
    // tentativa de entrar buscando qdo vier com dados na url
    // tipo do crostab e da conciliação não rolou
    // falta ainda testar o campo, mas mesmo ante não rolou
    // alert ("aqui")
    // AccountPayable.getListFromServer();

    return $(".b_dre_send").click(function() {
      $("#grid_dre").html("Aguarde um instante, o DRE está sendo gerado...");
      $("#dre_modal").modal({
        "show": true,
        "keyboard": true,
        "backdrop": true
      });
      return AccountPayable.getListFromServer(function() {
        return requestDreData(0, 0, 0, AccountPayable.dttype(), AccountPayable.startDate(), AccountPayable.endDate(), AccountPayable.ids.join(" , "));
      });
    });
  }));

}).call(this);

