  var treatments_del = [];
  var hasUnitModule = $('.has-unit-module').length > 0;
  var hasFinancialAccess = $('.has-financial-access').length > 0;

  var executa_rel = function(){
    var url = "/treatments/getTreatmentsByFilterPet";
    var request = function(){
      var total = 0.00;
      var count = 0.00;
      var user = $('#user').val()
      var fields = [];
      fields[1] = "date";
      fields[2] = { // cliente
        type: "format",
        decode: function(name, row) {
          return "<a href='/customer/edit?id=" + row[16] + "' target='_customer_maste'>" + name + "</a>";
        }
      }
      fields[4] = { // profissional
        type: "format",
        decode: function(name, row) {
          return "<a href='/user/edit?id=" + row[19] + "' target='_user_maste'>" + name + "</a>";
        }
      }
      fields[5] = { // pet
        type: "format",
        decode: function(name, row) {
          return "<a href='/animal/edit_animal?id=" + row[15] + "' target='_animal_maste'>" + name + "</a>";
        }
      }
      fields[6] = { // tutor
        type: "format",
        decode: function(name, row) {
          return "<a href='/customer/edit?id=" + row[17] + "' target='_customer_maste'>" + name + "</a>";
        }
      }
      fields[8] = { // auxiliar requisitante
        type: "format",
        decode: function(name, row) {
          return "<a href='/customer/edit?id=" + row[18] + "' target='_customer_maste'>" + name + "</a>";
        }
      }
      fields[9] = {
        type: "format",
        decode: function(name, row) {
          if (row[20] != "" && row[20] != "0") {
            return "<a href='/activity/edit?id=" + row[20] + "' target='_activity_maste'>" + name + "</a>";
          } else {
            return "<a href='/product_admin/edit?id=" + row[21] + "' target='_product_maste'>" + name + "</a>";
          }
        }
      }
      fields [10] = {
        type : "format",
        decode: function(name, row) {
          trStatus = 10;
          trStatus2 = 10;
          return trStatusdecode (name,row)
        }
      };
      if (!hasFinancialAccess) {
        fields[12] = "none";
      } else {
        fields[12] = "real";
      }
      fields[13] = "intNull";
      if (!hasUnitModule) {
        fields[14] = "none";
      }
      fields[15] = "none";
      fields[16] = "none";
      fields[17] = "none";
      fields[18] = "none";
      fields[19] = "none";
      fields[20] = "none"; // atividade serviço
      fields[21] = "none"; // produto
      renderReport(url,fields,$("#form_report").serializeObject(),"#grid", function(data){
        data.forEach(function(row){
          total += parseFloat(row[12]);
          count += 1;
        });
        $("#total").val(total.formatMoney());
        $("#count").val(count);
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
    