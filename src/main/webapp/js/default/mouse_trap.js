$(function() {
	var desableKeys = function() {
		return $(event.target).is('.jqte_editor, .select2-choice');
	};
	
	Mousetrap.init();

	Mousetrap.bind(['alt+r', 'r', 'R'], function() {
		if (!desableKeys()) {
			// aqui não dá ainda para usar   
	        //var hasEgrexSystem = $('.has-egrex-system').length > 0;
	        // precisaria estar em todos os templates
			if (document.location.href.indexOf("egrex") != -1) {
				window.location.href = "/reports/center_egrex";
			} else {
				window.location.href = "/reports/center";
			}
		}
	});

	Mousetrap.bind(['alt+u', 'u', 'U'], function() {
		if (!desableKeys())
			window.location.href = "/unit/list";
		//alert("Use K para acessar a conferência de comandas!");
	});

	Mousetrap.bind(['alt+j', 'j', 'J'], function() {
		if (!desableKeys())
			window.location.href = "/customer/todo_list_report";
	});

	Mousetrap.bind(['alt+k', 'k', 'K'], function() {
		if (!desableKeys())
			window.location.href = "/financial_cashier/comman_conference";
	});

	Mousetrap.bind(['alt+h', 'h', 'H'], function() {
		if (!desableKeys())
			window.location.href = "/docs/tutorial";
	});

	// só super user ATENCAO
	Mousetrap.bind(['alt+m', 'm', 'M'], function() {
		if (!desableKeys())
			window.location.href = "/manager/companies.html";
	});

	// só super user ATENCAO
	Mousetrap.bind(['alt+q', 'q', 'Q'], function() {
		if (!desableKeys())
			window.location.href = "/company_log/list_sqlcommand.html";
	});

	Mousetrap.bind(['alt+o', 'o', 'O'], function() {
		if (!desableKeys())
			//window.location.href = "/command_full/user_command_full";
	        window.open("/command_full/user_command_full","_command_maste")
	});

	Mousetrap.bind(['alt+e', 'e', 'E'], function() {
		if (!desableKeys())
			window.location.href = "/product/control_panel";
	});

	Mousetrap.bind(['alt+x', 'x', 'X'], function() {
		if (!desableKeys())
			window.location.href = "/financial_cashier/register_payment";
	});

	Mousetrap.bind(['alt+c', 'c', 'C'], function() {
		if (!desableKeys())
			window.location.href = "/customer/list";
	});

	Mousetrap.bind(['#'], function() {
		if (!desableKeys())
			window.location.href = "/financial_cashier/checkout_open";
	});

	Mousetrap.bind(['$'], function() {
		alert("Use F para acessar o Financeiro!");
	});

	Mousetrap.bind(['alt+f', 'f', 'F'], function() {
		if (!desableKeys())
			window.location.href = "/financial/account_register";
	});

	Mousetrap.bind(['alt+p', 'p', 'P'], function() {
		if (!desableKeys())
			window.location.href = "/user/list";
	});

	Mousetrap.bind(['alt+t', 't', 'T'], function() {
		if (!desableKeys())
			window.open('/treatments_conferenc', 'print_window');
	});
	Mousetrap.bind(['alt+d', 'd', 'D'], function() {
		if (!desableKeys())
			window.location.href = '/activity/dash_board_treatments';
	});
	Mousetrap.bind(['alt+a', 'a', 'A'], function() {
		var parameters = JSON.parse(localStorage.getItem("calendar_parameters"));
		if (!parameters) {
			parameters = "";
		}
		if (!desableKeys())
			window.location.href = "/calendar?" + parameters.parameters;
	});
	Mousetrap.bind(['alt+y', 'Y', 'y'], function() {
		if (!desableKeys())
			window.location.href = "/customer/scheduling";
	});
	Mousetrap.bind(['alt+n', 'N', 'n'], function() {
		if (!desableKeys())
			window.location.href = "/customer/notification_list";
	});
	Mousetrap.bind(['alt+b', 'B', 'b'], function() {
		if (!desableKeys())
			window.location.href = "/activity/busy_manager";
	});

	Mousetrap.bind(['alt+s', 's', 'S'], function() {
		if (!desableKeys())
			window.location.href = "/activity/list";
		//alert("Use L para acessar a Profissionais x Serviços!");
	});

	Mousetrap.bind(['alt+l', 'l', 'L'], function() {
		if (!desableKeys())
			window.location.href = "/customer/services_executors";
	});
});

