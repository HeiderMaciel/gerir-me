  var treatments_del = [];
  var hasUnitModule = $('.has-unit-module').length > 0;
  var hasFinancialAccess = $('.has-financial-access').length > 0;

  var removeTreatmentById = function(id, element){
    var hasDeleteCalendar = $('.has-delete-calendar').length > 0;
    if (hasDeleteCalendar) {
      if(confirm("Tem certeza que deseja excluir este atendimento?")){
          var url = "/treatment/"+id;
          $.ajax(url,{"type": "DELETE", "success" : function(){
              $(element).parent().parent().hide();
          }, "error" : function(response){
            alert("Erro ao excluir atendimento!\n Verifique se o atendimento não foi pago");
          }});
        }
    } else {
      alert ("Suas permissões não permitem excluir agendamento/atendimento");
    }
  };

  var removeTreatments = function(){
    var hasDeleteCalendar = $('.has-delete-calendar').length > 0;
    if (hasDeleteCalendar) {
      if (!$('#customer').val()) {
          alert ("Para excluir vários atendimentos um cliente precisa ser informado!")
          return
      }
      if (!$('#activity').val()) {
          alert ("Para excluir vários atendimentos um serviço/procedimento precisa ser informado!")
          return
      }
      if (treatments_del.length <= 1) {
          alert ('Você precisa "Buscar" os agendamentos/atendimentos para vê-los antes de excluir!')        
          return
      }
      var message = "";
      if(confirm("Tem certeza que deseja excluir os " + treatments_del.length + " atendimentos?")){
        var url = "";
        var count = 0;
        for (var i = treatments_del.length - 1; i >= 0; i--) {
          obj = treatments_del[i];
          if (obj.customerid != $('#customer').val()) {
            message += "cliente diferente " + obj.customername + " diferente do selecionado\n\n"
            count += 1;
          } else {
            url = "/treatment/"+obj.id;
            $.ajax(url,{"type": "DELETE", "success" : function(){
                //$(element).parent().parent().hide();
            }, "error" : function(response){
              message += "Erro ao excluir atendimento! Verifique se o atendimento não foi pago\n\n";
              count += 1;
            }});
          }
        }
        message += "Foram excluídos " + (treatments_del.length - count) + " de " + treatments_del.length + " atendimentos "
        if (message != "") {
          alert (message);
        }
      }
    } else {
      alert ("Suas permissões não permitem excluir agendamento/atendimento");
    }
  };
  var decodeStatus = function(status, status2){
          trStatus = 0;
          trStatus2 = 1; // nao tem
          var row = [];
          row.push (status, status2)
          return trStatusdecode ('',row)
/*
    // duplicado no treatmentManger.js
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
          //"<img title='" + title + "' src='/images/treatment_"+statstr.toLowerCase()+".png' width='24'/>" +
    return "<img title='" + title + "' src='/images/treatment_"+statstr.toLowerCase()+"1.png' width='24'/>"
*/
  };
  var executa_rel = function(){
        $.post("/treatments/getTreatmentsByFilter",$("#form_search").serialize(),function(t){
          var treatments = [];
          var total = 0.0;
          eval("treatments = "+t);
          var ret = "";
          treatments_del = treatments
          for (var i = treatments.length - 1; i >= 0; i--) {
            obj = treatments[i];
            total += obj.total;              
            ret += "<tr>" +
            "<td>"+obj.command+"</td>" +
            "<td>"+getDateBr(FactoryDate.byTime(obj.date))+"</td>" +
            "<td>"+getHourBr(FactoryDate.byTime(obj.date))+"</td>" +
            "<td>"+getHourBr(FactoryDate.byTime(obj.end))+
            "</td>" + 
            "<td>"+obj.customerid+"</td>" + 
            "<td>"+"<a href='/customer/edit?id="+obj.customerid+"' target='_customer_maste'>"+obj.customername+"</a>"+"</td>" +
            "<td>"+obj.phone+"</td>" + // tem tb email conctenado
            "<td>"+obj.obs+"</td>" +
            "<td>"+obj.username+"</td>"+
            (hasUnitModule ? "<td>"+obj.unitname+"</td>" : '')+ 
            "<td>"+decodeStatus(obj.status, obj.status2)+"</td>" + 
            "<td>"+obj.details+"</td>" +
            "<td>"+obj.payments+"</td>" +
            (hasFinancialAccess ? "<td>"+obj.total.formatMoney()+"</td>" : '') +
            "<td>"+obj.cashier+
            "</td>" +
            "<td><a target='_commission' href='/financial/commission_report_filter?treatment="+obj.id+"' ><img class='hide_on_print' alt='Ver comissões deste atendimento' src='/images/commision_payment.png' width='24'></a>" +
            "<a href='#' onclick='removeTreatmentById("
              +obj.id+", this)'><img class='hide_on_print' alt='excluir este atendimento'  src='/images/delete.png'></a></td>";
          };
          $("#table_result tbody").html(ret);
          $("#count").val(treatments.length);
          $("#total").val(total.formatMoney());
          $("#table_result").tablesorter();
        })
  };  
  $(function(){
      $('form').submit(function(){
        return false;
      });
      $("#product").productField(true);
      $("#godelete").click(function(){
        removeTreatments();
      });
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
    