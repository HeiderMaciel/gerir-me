      var total_to_pay = 0;
      var getUsers = function(){
          var url = "/cash/getUsers/commission";
          $.get(url, function(t){
            eval("userObj = "+t);
            if (userObj.length > 1) {
              $('#user').append("<option value=''>Selecione um Profissional</option>");
            }
            for(i in userObj){
              $('#user').append("<option value='"+userObj[i].id+"'>"+userObj[i].name+"</option>");
            }
            $("#user").val(gup("user"));
            $("#user").change();
            if(gup("user")){
              $("#send").click();
            }
          });
      }      

      var startDate = function(){
        return  encodeURIComponent($("#startDate").val() != "" ? $("#startDate").val() : getDateBr(new Date()));
      }
      var endDate = function(){
        return encodeURIComponent($("#endDate").val() != "" ? $("#endDate").val() : getDateBr(new Date()));
      }      

      $(function(){
        getUsers()
        var start = gup("startDate") || getDateBr(new Date().getStartOfMonth());
        var end = gup("endDate") || getDateBr(new Date());
        try{
          $("#startDate").val(start)
        }catch(e){
        }

        try{        
          $("#endDate").val(end)
        }catch(e){

        }          
        $("#pay").click(function(){
          window.open("/financial/account_register?date="+getDateBr(new Date())+"&user_select="+$("#user").val()+"&value="+$("#value").val()+"&type_select=1");
        });
        $("#send").click(function(){
          url ="/payment/getTotal/"+startDate()+"/"+endDate()+"/"+$("#user").val();
          $.get(url, function(t){
            eval("totalObj = "+t);
            $("#total").val((totalObj.total).formatMoney());
            total_to_pay = totalObj.total;
            // unit /0
            var unitparm = $("#unit").val() || '0';
            var dttypeparm = $("#dttype_commission").val() || '1'; // competencia
            $.get("/accountpayable/list_user/"+startDate()+"/"+endDate()+"/"+unitparm+"/"+$("#user").val()+"/"+dttypeparm,function(results){
                        var hasUnitModule = $('.has-unit-module').length > 0;
                        var ret = ""
                        var payments = [];
                        var total = 0;
                        eval("payments = "+results);
                        for (var i = payments.length - 1; i >= 0; i--) {
                          payment = payments[i];
                          if(payment.type == 0)
                            total += payment.value
                          else
                            total -= payment.value              
//                          ret += "<tr><td>"+getDateBr(new Date(payment.dueDate))+"</td><td>"+payment.cashier+"</td><td>"+payment.account+"</td><td>"+payment.category+"</td><td>"+payment.obs+"</td><td>"+(payment.value).formatMoney()+"</td></tr>";
//                          ret += "<tr><td>"+getDateBr(new Date(payment.dueDate))+"</td><td>"+payment.category+"</td><td>"+payment.obs+"</td><td>"+(payment.value).formatMoney()+"</td></tr>";
                            ret += "<tr>" +
                            "<td>"+getDateBr(new Date(payment.dueDate))+"</td>" +
                            (hasUnitModule ? "<td>" + payment.unit_name + "</td>" : "") + 
                            "<td>"+payment.cashier+"</td>" +
                            "<td>"+payment.account+"</td>" +
                            "<td>"+payment.category+"</td>" +
                            "<td>"+payment.obs+"</td>" + 
                            "<td>"+(payment.value).formatMoney()+"</td>" +
                            (payment.aggregateValue != 0 ? "<td>" + (payment.aggregateValue).formatMoney() + "</td>" : "<td>" + "</td>") + 
                            "</tr>";
                        };
                        $("#grid tbody").html(ret);
                        $("#total_paid").val((total).formatMoney());
                        total_to_pay += total;
                        $("#to_pay").val((total_to_pay).formatMoney())
            });
          });

        })
      });