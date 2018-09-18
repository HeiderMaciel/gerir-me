var customers = [];
var deleteStakeHolder = function(id) {
	if (confirm("Deseja excluir o participante?")) {
		$.post("/project/remove_stakeholder/" + gup('id'), {
				id: id
			})
			.success(function() {
				$("#bp_stakeholder").val('').change();
				updateReportStake();
			});
	}
};
var deletePaymentCondition = function(id) {
	if (confirm("Deseja excluir a condição de pagamento?")) {
		$.post("/project/remove_paymentcondition/" + id, {
				id: id
			})
			.success(function() {
				updateReportCondition();
			});
	}
};

var deleteSection = function(id) {
	if (confirm("Deseja excluir a seção?")) {
		$.post("/project/remove_projectsection/" + id, {
				id: id
			})
			.success(function(t) {
	          if (t=="1" || t==1) {
				updateReportSection();
			  } else {
	            eval("var obj = " + t)
	            return alert("Erro ao excluir seção!\n\n"+obj);
			  } 
			});
	}
};

var deleteItems = function(id) {
	if (confirm("Deseja excluir o item?")) {
		$.post("/project/remove_projectitem/" + id, {
				id: id
			})
			.success(function(t) {
	          if (t=="1" || t==1) {
				updateReportItems();
			  } else {
	            eval("var obj = " + t)
	            return alert("Erro ao excluir item!\n\n"+obj);
			  } 
			});
	}
};

var updateReportStake = function() {
	var fields = ['text', {
		type: "format",
		decode: function(name, row) {
			return "<a href='/customer/edit?id=" + row[3] + "' target='_customer_maste'>" + name + "</a>";
		}
	}, 'text', {
		type: "format",
		decode: function(id, row) {
			return "<span style='margin-right:4px'><a class='btn' href='/project/edit_stakeholder?id=" + row[4] + "' target='_blank'>Ir</a></span>" +
				"<span><a class='btn danger' target='_blank' onclick='deleteStakeHolder(" + row[4] + ")'>Excluir</a></span>";
		}
	}, 'none'];
	renderReport("/report/stakeholder_by_project", fields, {
		project: gup('id')
	}, "#table_stakeholders");
};;
var updateReportCondition = function() {
	var fields = ['text', 'date', 'text', 'text', 'text',{
		type: "format",
		decode: function(id, row) {
			return "<span style='margin-right:4px'><a class='btn' href='/project/edit_paymentcondition?id=" + row[5] + "' target='_blank'>Ir</a></span>" +
				"<span><a class='btn danger' target='_blank' onclick='deletePaymentCondition(" + row[5] + ")'>Excluir</a></span>";
		}
	}, 'none'];
	renderReport("/report/paymentcondition_by_project", fields, {
		project: gup('id')
	}, "#table_paymentconditions");
};

var updateReportSection = function() {
	var fields = ['int', 'text', 'text',{
		type: "format",
		decode: function(id, row) {
			return "<span style='margin-right:4px'><a class='btn' href='/project/edit_project_section?id=" + row[3] + "' target='_blank'>Ir</a></span>" +
				"<span><a class='btn danger' target='_blank' onclick='deleteSection(" + row[3] + ")'>Excluir</a></span>";
		}
	}, 'none'];
	renderReport("/report/section_by_project", fields, {
		project: gup('id')
	}, "#table_sections");
};

var updateReportItems = function() {

	// tem um settimeout na primeira chamada e/ou no f5 
	// para pegar o check
    var print = $("input[@id=print_command]:checked").length == 1;

    var fields = [];
	fields[0] = "none";
	fields[1] = "none";
    fields[2] = {
      type: "format",
      decode: function(name, row) {
        if (row[13] != "" && row[13] != "0") {
          return "<p style='line-height: 105%'>" + "<a href='/activity/edit?id=" + row[13] + "' target='_activity_maste'>" + name + "</a>" + "</p>";
        } else {
          return "<a href='/product_admin/edit?id=" + row[14] + "' target='_product_maste'>" + name + "</a>";
        }
      }
    }
    fields[3] = "textNull";
    // 4 obs
    if (print) {
	    fields[5] = "none";
	    fields[6] = "none";
	    fields[7] = "none";
	    fields[8] = "none";
	    fields[9] = "none";
	    fields[10] = "none";
	    fields[11] = "none";
    } else {
	    fields[5] = "real";
	    fields[6] = "real";
	    fields[7] = "real";
	    fields[8] = "real";
	    fields[8] = {
	      type : "format",
	      decode: function(name, row) {
	        var color = ""
	        if (parseFloat(row[8]) < parseFloat(row[5])) {
	          color = "green" 
	        } else if (parseFloat(row[8]) > parseFloat(row[5])) {
	          color = "red" 
	        } else {
	          // vai assumir cor default	
	        }
		    return "<p style='color:" + color + "'>" + 
		        realDecode(row[8]) + "<p/>"
	      }
	    }
	    fields[9] = "real";
	    fields[11] = {
			type: "format",
			decode: function(id, row) {
				return "<span style='margin-right:4px'><a class='btn' href='/treatment/treatmentdetail?id=" + row[11] + "' target='_treatdetail_maste'>Editar</a></span>" +
					"<span><a class='btn danger' target='_blank' onclick='deleteItems(" + row[12] + ")'>Excluir</a></span>";
			}
		}
	}
    fields[12] = "none";
    fields[13] = "none"; // activity
    fields[14] = "none"; // product
    
  	var group = function(row,value){
    	return parseFloat(value) + (parseFloat(row[9]));
  	};
  	var formater = function(value){
    	return value.formatMoney();
  	};        
	var group_meta_data = {
		"key":0,
		"name":0, 
		"groupFunction" : group,
        "formater" : formater,
        "show" : true,
        "childGroup" : {
            "key":1, 
            "name":1, 
            "groupFunction" : group, 
            "show" : true,
            "collapsed" : false,
            "formater" : formater
      	}
    };
	renderReport("/project/budget/" + gup('id'), fields, {
		project: gup('id')
	}, "#table_items", false, false, false, false. false, false, group_meta_data);

    $.post("/projectsum/budget/" + gup('id'), 
    	function(r){
      var precolista = parseFloat(eval(r)[0][0]);
      var precototal = parseFloat(eval(r)[0][1]);
      $("#totallista").val((precolista).formatMoney());
      $("#totaldesc").val((precolista-precototal).formatMoney());
      if (precolista == 0) {
      	  $("#totalperc").val((0).formatMoney());
      } else {
	      $("#totalperc").val((100-(precototal * 100 / precolista)).formatMoney());
      }
      $("#totalbudget").val((precototal).formatMoney());
      $("#totalbudget").change();
      // payment conditions
      $("#totalbudgetpc").val((precototal).formatMoney());
    });

};

$(function() {
	if (gup('id')) {
		updateReportStake();
		updateReportCondition();
		updateReportSection();
        setTimeout(function() {
			updateReportItems();
        }, 1000);
		$("#add_stakeholder").click(function() {
			$.post("/project/add_stakeholder/" + gup('id'), {
					bp_stakeholder: $("#bp_stakeholder").val(),
					stakeholdertype: $("#stakeholdertype").val()
				})
				.success(function() {
					$("#bp_stakeholder").val('').change();
					updateReportStake();
				});
		});
		$("#add_paymentcondition").click(function() {
			if (!$("#paymentconditions_days").val() &&
				!$("#paymentconditions_paymentdate").val()) {
				return alert ("Ou Qtde Dias ou a Data Pagamento precisam ser informados")
			}
			if (!$("#paymentconditions_days").val()) {
				$("#paymentconditions_days").val("0")
			} else {
				$("#paymentconditions_paymentdate").val("null")
			}

			if (!$("#paymentconditions_percent").val() &&
				!$("#paymentconditions_value").val()) {
				return alert ("Ou Percentual ou Valor a ser pago precisam ser informados")
			}
			if (!$("#paymentconditions_percent").val()) {
				$("#paymentconditions_percent").val("0")
			} else {
				$("#paymentconditions_value").val("0")
			}

			$.post("/project/add_paymentcondition/" + gup('id'), {
					days: $("#paymentconditions_days").val(),
					paymentdate: $("#paymentconditions_paymentdate").val(),
					obs: $("#paymentconditions_obs").val(),
					percent: $("#paymentconditions_percent").val(),
					value: $("#paymentconditions_value").val()
				})
				.success(function() {
					updateReportCondition();
				});
		});
		$("#add_section").click(function() {
			if (!$("#sections_orderInReport").val()) {
				return alert ("Uma Ordem para impressão da seção precisa ser informada")
			}
			if (!$("#sections_title").val()) {
				return alert ("Um Título para a seção precisa ser informado")
			}
			$.post("/project/add_section/" + gup('id'), {
					orderInReport: $("#sections_orderInReport").val(),
					title: $("#sections_title").val(),
					obs: $("#sections_obs").val()
				})
				.success(function() {
					updateReportSection();
				});
		});
	}
    $('#stakeholdertype').stakeholderTypeField();
});

