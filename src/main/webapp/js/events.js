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
			.success(function() {
				updateReportSection();
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
    var fields = [];
    fields[3] = "textNull";
    fields[4] = "real";
    fields[5] = "real";
    fields[6] = "real";
    fields[7] = "real";
    fields[8] = "real";
	renderReport("/project/budget/" + gup('id'), fields, {
		project: gup('id')
	}, "#table_items");
};

$(function() {
	if (gup('id')) {
		updateReportStake();
		updateReportCondition();
		updateReportSection();
		updateReportItems();
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

