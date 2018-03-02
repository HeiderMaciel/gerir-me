var PayrollEvent = function() {
  this.save = function(id,user, event, obs, value, qtd, date, callback, errorCallback) {
    $.post("/payroll/bppayroll",{id:id,business_pattern:user, date:date, event:event, obs:obs, value:value, qtd:qtd} ,function(t){
      eval('t='+t);
      if(t && callback){
        callback(t);
      }else if(errorCallback){
        errorCallback(t);
      }
    });
  };
  this.remove = function(id, callback, errorCallback) {
    $.ajax("/payroll/bppayroll/"+id,{"type": "DELETE", "success" : function(){
        if(callback)
          callback();
      }, "error" : function(response){
       errorCallback();
      }});    
  };
  this.list = function(filters, callback, errorCallback){
    $.get("/payroll/bppayroll", filters,function(t){
      eval('t='+t);
      if(t && callback){
        callback(t);
      }else if(errorCallback){
        errorCallback(t);
      }
    });
  };
};
var payroll = new PayrollEvent();
var PayrollEventModule = angular.module('PayrollEventModule',[]);
var PayrollEventController = function($scope){
  PayrollEventController.$scope = $scope;
  PayrollEventController.updateList = function(filters){
    if(!filters){
      filters = []
    }
    payroll.list(filters, function(payrollEvents){
      $scope.payrollEvents = payrollEvents;
      $scope.total = 0.00;
      payrollEvents.forEach(function(payrollEvent){
          $scope.total += payrollEvent.value;
      })
      // acerta erro de preciao que colocava numero na notação
      // exponencial
      $scope.total = $scope.total.toFixed(2)
      // acerta o -0.00
      if ($scope.total > -0.0000009 && $scope.total < 0.00000001) {
        $scope.total = 0.0;
        $scope.total = $scope.total.toFixed(2)
      }
      $scope.$apply();
    });

  };
  $scope.edit = function(event) {
   $('#id').val(event.id);
   $("#user_select").val(event.business_pattern).change();
   $("#event").val(event.event);
   $("#obs").val(event.obs);
   $('#value').val(event.value.toFixed(2));
   $('#qtd').val(event.qtd);
   $("#date").val(new Date(event.date).getDateBr());
   $("#account_modal").modal({"show":true,"keyboard":true,"backdrop":true});
  };
  $scope.new = function() {
   $('#id').val(event.id);
  };  
  $scope.remove = function(event) {
    payroll.remove(event.id, function(){
        alert('Excluído com sucesso!');
         PayrollEventController.updateList();
    }, function(){
      alert('Erro ao excluir');
      PayrollEventController.updateList();
    });
  };  

};
$(function() {
  
  $("#search").click(function(){
      var user = $('#user').val();
      if (!user || user == "") {
        alert("Profissional inválido!");
        return
      }

      var events_filter = $('#events_filter').val();
      if (!events_filter || events_filter == "") {
        alert("Verba inválida!");
        return
      }

      PayrollEventController.updateList($(".filter_form form").serializeObject());
  });
  $("#user, #user_select").userField(true);
  $("#event").payrollEventsField();
  $("#events_filter").payrollEventsField(false,true);
  $('.b_add_account').click(function(){
      payroll.save( $('#id').val(), $("#user_select").val(), $("#event").val(), $("#obs").val(), $('#value').val() ,$('#qtd').val(),$("#date").val(),function(){
          alert('Salvo com sucesso!');
          PayrollEventController.updateList();
      }, function(){ 
        alert('Excluído com sucesso!');
      });
  });
  $("#new_account").click(function(){
    $('#id').val(0);
    var user = $('#user').val();
    if (user && user.length == 1) {
      $('#user_select').val(user)
      $('#user_select').change()
    }
    $("#account_modal").modal({"show":true,"keyboard":true,"backdrop":true});
  });
})
