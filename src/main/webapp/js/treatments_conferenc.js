  var treatments_del = [];
  var hasUnitModule = $('.has-unit-module').length > 0;
  var hasFinancialAccess = $('.has-financial-access').length > 0;
  var hasAuditModule = $('.has-audit-module').length > 0;

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
      if (!$('#activity').val() && !$('#product').val() ) {
          alert ("Para excluir vários atendimentos um serviço/procedimento ou produto precisa ser informado!")
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
          executa_rel();
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
  };
  var executa_rel = function(){
        $.post("/treatments/getTreatmentsByFilter",$("#form_report").serialize(),function(t){
          var treatments = [];
          var total = 0.0;
          var minutes = 0.0;
          eval("treatments = "+t);
          var ret = "";
          treatments_del = treatments
          for (var i = treatments.length - 1; i >= 0; i--) {
            obj = treatments[i];
            total += obj.total;
            minutes += obj.minutes;
            var iniAux = "";
            if (isMobile.any) {
              iniAux = getHourBr(FactoryDate.byTime(obj.date))
            }
            ret += "<tr>" +
            "<td>"+obj.command+"</td>" +
            "<td>"+getDateBr(FactoryDate.byTime(obj.date))+ " " + iniAux + "</td>" +
            //"<td>"+getHourBr(FactoryDate.byTime(obj.date))+"</td>" +
            (!isMobile.any ? "<td>"+getHourBr(FactoryDate.byTime(obj.date))+"</td>" : '')+ 
            (!isMobile.any ? "<td>"+getHourBr(FactoryDate.byTime(obj.end))+"</td>" : '')+ 
            (!isMobile.any ? "<td>"+obj.customerid+"</td>" : '')+ 
            "<td>"+"<a style='line-height: 105%' href='/customer/edit?id="+obj.customerid+"' target='_customer_maste'>"+obj.customername+"</a>"+"</td>" +
            "<td>"+"<p style='line-height: 105%'>" + 
              obj.phone + "<p/>"+"</td>" + // tem tb email conctenado
            "<td>"+"<p style='line-height: 105%'>" + 
              obj.obs + "<p/>"+"</td>" +
            "<td> <p style='line-height: 105%'>"+obj.username+"</p> </td>"+
            (hasUnitModule ? "<td>"+obj.unitname+"</td>" : '')+ 
            "<td>"+decodeStatus(obj.status, obj.status2)+"</td>" + 
            "<td> <p style='line-height: 105%'>" + obj.details + "</p> </td>" +
            "<td>"+obj.payments+"</td>" +
            (hasFinancialAccess ? "<td>"+obj.total.formatMoney()+"</td>" : '') +
            "<td>"+obj.cashier+"</td>" +
            "<td>"+
            "<a target='_commission' href='/financial/commission_report_filter?treatment="+obj.id+"' ><img class='hide_on_print' alt='Ver comissões deste atendimento' src='/images/commision_payment.png' width='24'></a>" +
            "<a href='#' onclick='removeTreatmentById("
              +obj.id+", this)'><img class='hide_on_print' alt='excluir este atendimento'  src='/images/delete.png'></a>"+
              "</td>"+
            "</td><td><a title='" + obj.auditstr + "' href='#' ><img width='24px' src='/images/audit.png'/></a></td>" +
            "</tr>";
          };
          $("#grid tbody").html(ret);
          $("#count").val(treatments.length);
          $("#total").val(total.formatMoney());
          var hours = "";
          hours = (Math.trunc(minutes/60)).toString() + ":" + (minutes % 60).toString()
          $("#hours").val(hours);
          $("#grid").tablesorter();
        })
  };  
  $(function(){
      $('form').submit(function(){
        return false;
      });
      $("#category_select").productTypeField(true);
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
    