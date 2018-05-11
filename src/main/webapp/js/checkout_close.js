var cashiersObj = {};
var cashierValuesObj = {};
$(function() {
  //$("#payment_types").hide("show");
  url = $('#checkoutClose_form').attr('action');
  url += "/openCheckouts";
  $.get(url, function(t) {
    eval("cashiersObj = " + t)
    if (cashiersObj.length > 1) {
      $('#cashiers_select').append("<option value=''>Selecione um Caixa</option>")
    }
    for (i in cashiersObj) {
      var dateAsDate = new Date(cashiersObj[i].openerDate);
      $('#cashiers_select').append("<option value='" + cashiersObj[i].idForCompany + "'>" + cashiersObj[i].idForCompany + " - " + getDateBr(dateAsDate) + " - " + cashiersObj[i].userName + " - " + cashiersObj[i].unit + " - id " + cashiersObj[i].id + "</option>")
    }
    if (cashiersObj.length == 1) {
      $('#cashiers_select').change();
    }
  })
  $('#cashiers_select').change(function() {
    //$("#payment_types").hide("hide");
    var cashierId = $(this).val();
    for (i in cashiersObj) {
      cashier = cashiersObj[i];
      if (cashierId == cashier.idForCompany) {
        $("#start_value").val(cashier.startValue.formatMoney());
        $("#opener_date").val(getDateBr(new Date(parseInt(cashier.openerDate))));
        callApiGetValue();
        return;
      }
    }
    $("#start_value").val("");
    $("#opener_date").val("");
    return;
  });
})
var callApiLock = false;
var callApiClose = function() {
  if (!callApiLock) {
    callApiLock = true
    url = $('#checkoutClose_form').attr('action');
    url += "/checkoutClose/" + $('#cashiers_select').val();
    $.get(url, function(t) {
      eval("var obj = " + t)
      if (obj.status == 'success') {
        alert("Caixa [" + obj.id + "] fechado com valor de [" + obj.endValue + "] com sucesso! ")
        callApiLock = false;
      } else {
        if (obj.message) {
          alert(obj.message)
          callApiLock = false;
        } else {
          alert("Erro ao abrir o caixa. Verifique os campos e tente novamente!")
          callApiLock = false;
        }
      }
    });
  } else {
    alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
  }
}
var openDetail = function(cmd) {
  var id = null;
  cashierId = $('#cashiers_select').val();
  if (!cashierId){
     alert("Caixa inválido!")
  } else {
    for (i in cashiersObj) {
      cashier = cashiersObj[i];
      if (cashierId == cashier.idForCompany) {
        id = cashier.id;
      }
    }
    if (cmd) {
      document.location.href = "/financial_cashier/comman_conference?cashier=" + id + "&productclass=1,0";
    } else {
      document.location.href = "/financial_cashier/paymenttype_details?cashier=" + id + "&startDate=" + $("#opener_date").val() + "&endDate=" + $("#opener_date").val();
    }
  }
}
var callApiGetValue = function() {
  var params = $('#checkoutClose_form').serializeObject();
  params.cashier = params.cashiers;
  params.isIdForCompany = true;
  requestCashiersPaymentTypes(params, "#grid", "#total_payment_types");
  url = $('#checkoutClose_form').attr('action');
  url += "/checkoutValues/" + $('#cashiers_select').val();
  $.get(url, function(t) {
    eval("cashierValuesObj = " + t)
    $("#payment_types").show("show");
    $("#money_value").val(cashierValuesObj.paidValueInMoney.formatMoney());
    $("#cheque_value").val(cashierValuesObj.paidValueInCheque.formatMoney());
    $("#card_value").val(cashierValuesObj.paidValueInCard.formatMoney());
    $("#out_value").val(cashierValuesObj.outsValue.formatMoney());
    $("#out_cheque").val(cashierValuesObj.outsCheque.formatMoney());
    $("#in_value").val(cashierValuesObj.insValue.formatMoney());
    $("#total_value").val(cashierValuesObj.totalValueToConference.formatMoney())
    $("#final_value").val(
      (cashierValuesObj.startValue + 
       cashierValuesObj.paidValueInMoney - 
       cashierValuesObj.outsValue + 
       cashierValuesObj.insValue).formatMoney())

  });
}