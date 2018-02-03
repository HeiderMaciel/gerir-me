var getCashiers = function(){
    DataManager.getUsers(function(userObj){
      user_str = "";
      user_str += "<option value=''>Todos</option>";
      for(var i in userObj){
            user_str += "<option value='"+userObj[i].id+"'>"+userObj[i].name+"</option>";
      }
      $("#user").html(user_str);     
    });
    $('#cashier').cashierField(false,'all',function(){
      $('#cashier').val(gup("cashier"));
      renderOuts($('#form_search').serializeObject());
    });
};
$(function(){
  $("#payment_type").paymentTypeField(true, true, function(){
    $("#payment_type").val(gup('payment_type')).change();
  });
  $("#offsale").offSaleField(true);
  $("#unit").unitField(true);
  var total = 0.0;
  var count = 0;
  $("#in_commands tr").each(function(i, item){
    if(i > 0) {
      total += parseFloat($($(item).find('td').get(8)).text());
      count += 1;
    }
  });
  $("#total").val(total.formatMoney());
  $("#count").val(count);
  getCashiers();
  $("#detail_print").click(function(){
    window.open('/financial_cashier/comman_conference_print?'+$("#form_search :input").serialize(),'print_window_command')
  });
})