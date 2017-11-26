    $(function(){
      $('#id_product').productSearch({createName:false, iconElement:".add-on", userThuch:true});
      $("#causes").inventoryCauseField(true, true);
      $("#units").unitField(false, true);
      function typeTranslate(typeMovement){
        if(typeMovement == "In"){
          return '<img src="/images/add.png"/>'
        }else{
          return '<img src="/images/remove.png"/>';
        }
      }
      $("#filter").click(function(){
          if ($("#causeh").val() == "9") {
            $("#causes").val('9'); // compras
          }

          $.get("/inventory/movements_filter?"+$("#filter_form").serialize(),function(t){
            eval("var dataSet = "+t)
            var ret = "";
            var total = 0.00;
            var totalSale = 0.00;
            var totalPurchase = 0.00;
            var entradas = 0.00;
            var saidas = 0.00;
            var hasInventoryManager = $('.has-inventory-manager').length > 0;

            for (var i = dataSet.length - 1; i >= 0; i--) {
                 var obj = dataSet[i];
                 if(obj.type_movement == 'In'){
                  total += obj.amount;
                  entradas += obj.amount;
                  totalSale -= obj.price;
                  totalPurchase += (obj.purchaseprice * obj.amount);
                 }else{
                  total -= obj.amount;
                  saidas += obj.amount;
                  totalSale += obj.price;
                  totalPurchase -= (obj.purchaseprice * obj.amount);
                 }
                 var purchaseOrder = $("#causeh").val() == "9"
                 ret += "<tr>" +
                  (!purchaseOrder ? "<td>"+obj.unit+"</td>" : "" ) +
                  "<td>"+obj.product_id+"</td>" +
                  "<td>"+"<a href='/product_admin/edit?id=" + obj.product_id + "' target='_product_maste'>" + obj.product_name + "</a>"+"</td>" +
                  "<td>"+"<a href='/customer/edit?id=" + obj.bp_id + "' target='_customer_maste'>" + obj.bp_name + "</a>"+"</td>" +
                  "<td>"+obj.invoice+"</td>" +
                  "<td>"+obj.amount+"</td>" + 
                  (hasInventoryManager ? "<td>" + obj.purchaseprice + "</td>" : "" ) +
                  (!purchaseOrder ? "<td>"+obj.price+"</td>" : "" ) + 
                  "<td>"+obj.obs+"</td>" +
                  (!purchaseOrder ? "<td>"+typeTranslate(obj.type_movement)+"</td>" : "" ) +
                  (!purchaseOrder ? "<td>"+obj.cause+"</td>" : "" ) + 
                  "<td>"+new Date(obj.efetive_date).getDateBr()+"</td>" +
                  (purchaseOrder ? "<td>"+"<a class='btn danger' onclick='del_detail(" + obj.im_id +")'  target=''>Excluir</a>"+"</td>" : "" ) + 
                  "<td>" + "<a href= '#' title='" + obj.auditStr + "'><img src='/images/audit.png' width='24'/></a>" + "</td>" +
                  "</tr>"
            };
            $("#grid tbody").html(ret);
            $("#total").val(total);
            $("#totalSale").val((totalSale).formatMoney());
            $("#totalPurchase").val((totalPurchase).formatMoney());
            $("#entradas").val(entradas);
            $("#saidas").val(saidas);
             try{
                $("#grid").trigger('update');
              }catch(e){}
          })
      });
    });


    var del_detail = function (imId) {
        return $.post("/inventory/del_purchaseOrderItem", {
          "imid": imId
        }, function(results) {
          if(results === 1 || results == "1"){
            alert("Item de solicitação de compras excluído com sucesso");
          }else{
            alert(eval(results));
          }
          $("#filter").click();
        });
      }

