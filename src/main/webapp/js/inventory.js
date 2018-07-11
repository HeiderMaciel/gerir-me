// Generated by CoffeeScript 1.6.3
(function() {
  var Inventory, InventoryMovement;

  Inventory = (function() {
    function Inventory() {}

    return Inventory;

  })();

  InventoryMovement = (function() {
    function InventoryMovement() {
      this.product = parseInt($("#product").val());
      this.product_name = $('#product').parent().find('.select2-container span').text();
      this.type_movement = $("#type").val();
      this.type_name = $("#type option:selected").text();
      this.unit_to = parseInt($("#unit_to").val());
      this.unit_to_name = $("#unit_to option:selected").text();
      this.unit_of = parseInt($("#unit_of").val());
      this.purchase_price = parseFloat($("#purchasePrice").val());
      this.indic1 = parseFloat($("#indic1").val());
      this.indic2 = parseFloat($("#indic2").val());
      this.indic3 = parseFloat($("#indic3").val());
      this.indic4 = parseFloat($("#indic4").val());
      this.indic5 = parseFloat($("#indic5").val());
      this.sale_price = parseFloat($("#salePrice").val());
      this.invoice = $("#invoice").val();
      this.amount = parseFloat($("#amount").val());
      this.obs = $("#obs").val();
      this.date = $("#date").val();
      this.cause = parseInt($("#cause").val());
      this.cause_name = $("#cause option:selected").text();
      this.supplier = parseInt($("#supplier").val() || '0');
      this.supplier_name = $("#supplier option:selected").text();
      this.id = Inventory.movements_list.length;
      this.valid();
    }

    InventoryMovement.prototype.valid = function() {
      if (!this.amount) {
        throw "Quantidade obrigatória";
      }
/*    
      if (!this.purchase_price) {
        throw "Preço obrigatório";
      }
*/
      if (!this.date) {
        throw "Data obrigatória";
      }
      if (!this.cause) {
        if (this.type_movement === "Out") {
          $("#cause").val('7').change(); // uso
          throw "Motivo obrigatório, padrão >> Uso << será atribuído";
        } else {
          throw "Motivo obrigatório";
        }
      }
      if (!this.product) {
        throw "Produto obrigatório";
      }
      if (!this.indic1) {
        this.indic1 = 0.0
      }
      if (!this.indic2) {
        this.indic2 = 0.0
      }
      if (!this.indic3) {
        this.indic3 = 0.0
      }
      if (!this.indic4) {
        this.indic4 = 0.0
      }
      if (!this.indic5) {
        this.indic5 = 0.0
      }

    };

    return InventoryMovement;

  })();

  var callApiLock = false
  Inventory.saveAll = $.throttle(1000, function() {
    var data;
    if (Inventory.movements_list.length <= 0) {
      alert("Adicione itens à tabela de movimentações antes de salvar");
      return 
    }
    if (!callApiLock) {
      callApiLock = true

      data = JSON.stringify(Inventory.movements_list);
      return $.post("/inventory/save", {
        "data": data
      }, function(r) {
        if (r === "0") {
          alert("Movimentações efetivadas com sucesso!");
          Inventory.movements_list = [];
          callApiLock = false
          return Inventory.renderTable();
        } else {
          callApiLock = false
          return alert(r);
        }
      });
    }  
  });

  Inventory.getPurchasePrice = function(idProduct) {
    return $.get("/inventory/purchasePrice/" + idProduct, function(r) {
      var str_val;
      str_val = (window.parseFloat(r)).formatMoney() + "";
      return $("#purchasePrice").val(str_val.replace(",", "."));
    });
  };

  Inventory.getSalePrice = function(idProduct) {
    return $.get("/inventory/salePrice/" + idProduct, function(r) {
      var str_val;
      str_val = (window.parseFloat(r)).formatMoney() + "";
      return $("#salePrice").val(str_val.replace(",", "."));
    });
  };

  Inventory.movements_list = [];

  Inventory.editById = function(id) {
    var obj, objs;
    objs = Inventory.movements_list.filter(function(c) {
      return c.id === id;
    });
    obj = objs[0];
    $("#product").val(obj.product);
    $("#product").change();
    $("#type").val(obj.type_movement);
    $("#unit_to").val(obj.unit_to);
    $("#unit_of").val(obj.unit_of);
    $("#purchasePrice").val(obj.purchase_price);
    $("#indic1").val(obj.indic1);
    $("#indic2").val(obj.indic2);
    $("#indic3").val(obj.indic3);
    $("#indic4").val(obj.indic4);
    $("#indic5").val(obj.indic5);
    $("#salePrice").val(obj.sale_price);
    $("#invoice").val(obj.invoice);
    $("#amount").val(obj.amount);
    $("#obs").val(obj.obs);
    return Inventory.removeById(id);
  };

  Inventory.removeById = function(id) {
    Inventory.movements_list = Inventory.movements_list.filter(function(c) {
      return c.id !== id;
    });
    return Inventory.renderTable();
  };

  Inventory.addCurrentMovement = $.throttle(1000, function() {
    var e;
    try {
      return Inventory.movements_list.push(new InventoryMovement());
    } catch (_error) {
      e = _error;
      return alert(e);
    }
  });

  Inventory.renderTable = function() {
    var isGerirmeSystem = $('.is-gerirme-system').length > 0;
    var movement, ret, _i, _len, _ref;
    ret = "";
    _ref = Inventory.movements_list;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      movement = _ref[_i];
      ret += "<tr>\n	" + 
      "<td>" + movement.product_name + "</td>\n	" + 
      "<td>" + movement.type_name + "</td>\n	" + 
      "<td>" + movement.unit_to_name + "</td>\n	" + 
      "<td>" + movement.purchase_price + "</td>\n	" + 
      (isGerirmeSystem ? ("<td>" + movement.indic1 + "</td>\n ") : "") + 
      (isGerirmeSystem ? ("<td>" + movement.indic2 + "</td>\n ") : "") + 
      (isGerirmeSystem ? ("<td>" + movement.indic3 + "</td>\n ") : "") + 
      (isGerirmeSystem ? ("<td>" + movement.indic4 + "</td>\n ") : "") + 
      (isGerirmeSystem ? ("<td>" + movement.indic5 + "</td>\n ") : "") + 
      "<td>" + movement.sale_price + "</td>\n " + 
      "<td>" + movement.invoice + "</td>\n	" + 
      "<td>" + movement.amount + "</td>\n	" + 
      "<td>" + movement.supplier_name + "</td>\n " + 
      "<td>" + movement.cause_name + "</td>\n	" + 
      "<td>" + movement.obs + "</td>\n	" + 
      "<td>\n		<a href='#' onclick='editById(" + movement.id + ")' class='action_delete'>\n			<img src='/images/edit.png'/>\n		</a>					\n		<a href='#' onclick='removeById(" + movement.id + ")' class='action_delete'>\n			<img src='/images/delete.png'/>\n		</a>\n	</td>\n" + 
      "</tr>";
    }
    return $("#table_data tbody").html(ret);
  };

  $(function() {
    $.get("/inventory/transferCause", function(t) {
      return Inventory.transferCause = t;
    });
    $("#product").productSearch({
      createName: false,
      iconElement: ".add-on",
      userThuch: true,
      allUnits: true
    });
    $(".unit").unitField(false, true);
    $("#supplier").supplierField(true);
    $("#cause").inventoryCauseField(true);
    //  Nao funcionou aqui - setei no add 
/*
    if ($("#causeh").val() == "9") {
      alert ("vaiii " + $("#causeh").val())
      $("#cause").val('9');
      alert ("vaiii depois " + $("#cause").val())
      $("#cause").change();
    }
*/
    $("#cause").change(function() {
      if ($("#causeh").val() == "9") {
        if ($("#cause").val() != "9") {
          $("#cause").val('9').change(); // solicitacao de compra nao atualiza estoque
        }
      }
    });

    $(".transfer").hide();
    $(".type").change(function() {
      if ($(this).val() === "Transfer") {
        $(".transfer").show();
        $("#cause").val(Inventory.transferCause).change();
      } else if ($(this).val() === "In") {
        $(".transfer").hide();
        if ($("#causeh").val() == "9") {
          $("#cause").val('9').change(); // solicitacao de compra nao atualiza estoque
        } else {
          $("#cause").val('5').change(); // compra
        }
      } else {
        $(".transfer").hide();
        $("#cause").val('').change();
      }
      return $("#obs").val($(".type option:selected").text());
    });
    $(".b_save").click(function() {
      return Inventory.saveAll();
    });
    
    $("#b_add").click(function() {
      if ($("#type").val() == "Transfer") {
        if ($("#unit_of").val() == $("#unit_to").val()) {
          return alert ("Não faz sentido transferir produtos de uma unidade para ela mesma!")
        }
      }
      if ($("#causeh").val() == "9") {
        if ($("#cause").val() != null && $("#cause").val() != 0 && $("#cause").val() != 9) {
          alert ("Apenas o motivo solcitação de compras pode ser utilizado nesta transação ");
          $("#cause").val('9').change(); // solicitacao de compra nao atualiza estoque
          $("#type").val("In").change();
        }
      }
      if ($("#cause").val() == null || $("#cause").val() == 0) {
        if ($("#type").val() == "In") {
           if ($("#causeh").val() == "9") {
             $("#cause").val('9').change(); // solicitacao de compra nao atualiza estoque
           } else {
             $("#cause").val('5').change(); // compra
           }
        }
      }
      
      if (!$("#purchasePrice").val()) {
        $("#purchasePrice").val(0);
      }
      if (!$("#salePrice").val()) {
        $("#salePrice").val(0);
      }

      Inventory.addCurrentMovement();
      return Inventory.renderTable();
    });
    $("#product").change(function() {
      if ($("#type").val() == "In") {
         if ($("#causeh").val() == "9") {
           $("#cause").val('9').change(); // solicitacao de compra nao atualiza estoque
         } else {
            if ($("#cause").val() == null || $("#cause").val() == 0) {
              $("#cause").val('5').change(); // compra
            }
         }
      }

      Inventory.getSalePrice($(this).val());
      return Inventory.getPurchasePrice($(this).val());
    });
    return $("#date").val(getDateBr(new Date()));
  });

  window.removeById = Inventory.removeById;

  window.editById = Inventory.editById;

}).call(this);
