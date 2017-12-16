$(function() {
  $("#product").productSearch({
    createName: false,
    iconElement: ".add-on",
    userThuch: true,
    userFieldSelector: '#user'
  });
  if (gup("id")) {
    var renderOsproduct = function() {
      var filter = {
        offsaleId: gup("id")
      };
      var fields = [];
      fields[3] = {
        type : "format",
        decode : function(value){
          var floatValue = parseFloat(value);
          if(!isNaN(floatValue)) {
            //total_serv += floatValue;
          }
          return floatValue.formatMoney();
        }
      }
      fields[7] = {
        type: "format",
        decode: function(value) {
          return "<span style='margin-right:4px'><a class='btn' href='/offsale/offsaleproduct?id=" + value + "' target='_offsaleproduct_maste'>Editar</a></span>" +
          '<input data-id="' + value + '" type="button" class="btn danger osproduct_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".osproduct_remove").click(function() {
          if (confirm("Deseja excluir o produto/serviço?")) {
            var id = $(this).data("id");
            $.ajax("/api/v2/osproduct/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Convênio/Prod-Serv excluído com sucesso!");
                renderOsproduct();
              }
            })
          }
        });
      }
      renderReport("/api/v2/osproduct/list", fields, filter, "#grid_osproduct", bindEvent);
    }

    renderOsproduct();
    
    $("#osproduct_add").click(function() {
      var prodserv = 0
      var product = 0
      var activity = 0
      var producttype = 0
      var offprice = 0.0
      var percentoff = 0.0

      if (!gup("id") || gup("id") == "") {
        return alert('É preciso salvar o convênio antes de inserir itens ao mesmo');
      }

      if ($("#osproduct_offprice").val()) {
        offprice = $("#osproduct_offprice").val()
      }
      if ($("#osproduct_percentoff").val()) {
        percentoff = $("#osproduct_percentoff").val()
      }
      if (offprice != 0.0 && percentoff != 0.0) {
        return alert ("Preencha apenas uma das opções, o preço do item no convênio ou percentual de desconto em relação ao preço de lista")
      }
      if ($("#osproduct_activity").val()) {
        activity = $("#osproduct_activity").val()
        prodserv = activity
      } 
      if ($("#product").val()) {
        product = $("#product").val()
        prodserv = product
      } 
      if ($("#osproduct_producttype").val()) {
        producttype = $("#osproduct_producttype").val()
      } 
      if (product != 0 && activity != 0) {
        return alert ("Preencha apenas uma das opções, serviço/procedimento ou produto")
      }
      if (prodserv != 0 && producttype != 0) {
        return alert ("Preencha apenas uma das opções, produto ou serviço/procedimento ou tipo de produto/serviço-procedimento")
      }

      if (prodserv == 0 && producttype == 0) {
        return alert ("É necessário preencher uma das opções, produto ou serviço/procedimento ou tipo de produto/serviço-procedimento")
      }

      if (producttype != 0 && offprice != 0.0) {
        return alert ("Para tipo de produto/serviço, um percentual de desconto deve ser informado, e não um preço")
      }

      $.post("/api/v2/osproduct", {
        obs: $("#osproduct_obs").val(),
        product: prodserv,
        producttype: producttype,
        offprice: offprice,
        percentoff: percentoff,
        offsale: gup("id")
      }, function() {
        alert("Tipo/Produto adicionado com sucesso!");
        renderOsproduct();
      });
    });
  }
})

