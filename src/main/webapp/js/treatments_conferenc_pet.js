  var treatments_del = [];
  var hasUnitModule = $('.has-unit-module').length > 0;
  var hasFinancialAccess = $('.has-financial-access').length > 0;

  var decodeStatus = function(status, status2){
          trStatus = 0;
          trStatus2 = 1; // nao tem
          var row = [];
          row.push (status, status2)
          return trStatusdecode ('',row)
  };
  var executa_rel = function(){
    var url = "/treatments/getTreatmentsByFilterPet";
    var total_to_pay = 0.00;
    var request = function(){
      var total = 0.00;
      var total_commission = 0.00;
      var user = $('#user').val()
      var fields = [];
      renderReport(url,fields,$("#form_report").serializeObject(),"#grid", function(data){
        data.forEach(function(row){
          total_commission += parseFloat(row[8]);
        });
        $("#total_commission").val(total_commission.formatMoney());
        total_to_pay = total_commission;
        var unitparm = $("#unit").val() || '0';
      });
    }
    request();
  };  
  $(function(){
      $('form').submit(function(){
        return false;
      });
      $("#product").productField(true);
      $("#go").click(function(){
        executa_rel();
      });
      $('#cashier').cashierField(true,'all',function(){ $('#cashier').val(gup("cashier")); });
      $("#user").userField(true);
      $("#offsale").offSaleField(true);
      $("#unit").unitField(true);
      DataManager.getUsers(function(userObj){
        user_str = "";
        user_str += "<option value=''>Todos</option>";
        for(var i in userObj){
              user_str += "<option value='"+userObj[i].id+"'>"+userObj[i].name+"</option>";
          }
          $("#user").html(user_str);
          $("#user").val("");
      });
      DataManager.getPaymentTypes(function(paymentTypes){
            var ret = "";
            ret += "<option value='SELECT_ALL'>Todos</option>";
            for(var i in paymentTypes){
                ret += "<option value='"+paymentTypes[i].id+"'>"+paymentTypes[i].name+"</option>";
            }
        $('#payment_type').append(ret);
      });
      DataManager.getActivities(false,function(activitysObj){
        $('#activity option').remove();
        var ret ="<option value=''>Selecione um serviço</option>";
        for(var i in activitysObj){
          ret +="<option value='"+activitysObj[i].id+"'>"+activitysObj[i].name+"</option>";
        }
        $('#activity').append(ret);
      });
})
    