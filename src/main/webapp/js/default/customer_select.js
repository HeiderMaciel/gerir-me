//Customer Serch
var parent_customer_search = null;
$(function() {
	var prepareSearch = function() {
		var recordsStr = ""
	    var hasEdoctusSystem = $('.has-edoctus-system').length > 0;
	    var hasEphysioSystem = $('.has-ephysio-system').length > 0;
	    var hasEsmileSystem = $('.has-esmile-system').length > 0;
	    if (hasEdoctusSystem || hasEphysioSystem || hasEsmileSystem) {
//		if ((document.location.href.indexOf("edoctus") != -1) ||
//			(document.location.href.indexOf("ephysio") != -1)) {
			recordsStr = '<a href="#" title="Ir para prontuário" class="openRecordsSelected"><img src="/images/records.png" class="img_customer_search" width="32">'
		}
		$(".name_customer_search").parent().append('<a href="#" title="Ir para cadastro" class="openCustomerSelected"><img src="/images/customers.png" class="img_customer_search" width="32">' + recordsStr);
		$(".openRecordsSelected").click(function(){
			$parent = $(this).parent();
			var id = $(".id_customer_search", $parent).val();
			if (id) {
				Customer.openRecordsSelected(id);
			} else {
				alert ("Nenhum paciente selecionado!");
			}
		});
		$(".openCustomerSelected").click(function(){
			$parent = $(this).parent();
			var id = $(".id_customer_search", $parent).val();
			if (id) {
				Customer.openCustomerSelected(id);
			} else {
				alert ("Nenhum cliente selecionado!");
			}
		});

	};	
	var autocompletCustomerPrepare = function() {
		$('.name_customer_search').each(function() {
			$(this).data("prevalue", $(this).val());
			var field = this;
			$(this).select2({
				placeholder: "Digite o nome do cliente",
				minimumInputLength: 3,
				formatNoMatches: function(val) {
					return "<a href='#' onclick='Customer.rapidCreate(\"" + val + "\");'><img src='/images/add.png' width='16'> (" + val + ").Deseja adicionar?</a>";
				},
				id: function(a) {
					return a.name;
				},
				ajax: {
					url: "/customer/search",
					dataType: 'text',
					quietMillis: 300,
					data: function(term, page) { // page is the one-based page number tracked by Select2
						return {
							name: term, //search term
							page: page,
							phone: '',
							user : !!$(field).data('just_user')
						};
					},
					results: function(data, page) {
						eval('data=' + data);
						parent_customer_search = $(field).parent();
						//var more = (page * 10) < data.total; // whether or not there are more results available
						// notice we return the value of more so Select2 knows if more results can be loaded
						return {
							results: data,
							more: data.length == 30
						};
					}
				},
				formatResult: Customer.customerSelectAutoCompleteFormat, // omitted for brevity, see the source of this page
				formatSelection: function(a) {
					$('.id_customer_search', parent_customer_search).val(a.id).change();
					return Customer.customerSelectAutoCompleteName(a)
				}, // omitted for brevity, see the source of this page
				dropdownCssClass: "bigdrop", // apply css that makes the dropdown taller
				escapeMarkup: function(m) {
						return m;
					} // we do not want to escape markup since we are displaying html in results
			});

		});
		$('.name_customer_search').each(function() {
			if ($(this).data('prevalue') != "") {
				$(".name_customer_search span").html($(this).data('prevalue'));
			}
		});
	};
	window.selectCustomer = function(id, name, messages) {
		$("#id_customer_search_form").val("");
		var last_id = $(".id_customer_search", parent_customer_search).val();
		if (last_id != id) {
			$(".id_customer_search", parent_customer_search).val(id);
			$(".id_customer_search", parent_customer_search).change();
		}
		$(".name_customer_search span", parent_customer_search).html(name);
		$("#customer_search").modal("hide");
		Customer.customerSelectAutoCompleteName({
			"name": name,
			"id": id
		});
	};	
	autocompletCustomerPrepare();
	prepareSearch();
	$(".id_customer_search").change(function() {
		parent_customer_search = $(this).parent();
		var $name_customer_search = $(".name_customer_search", parent_customer_search);
		var last_value = $(this).data("last");
		if (last_value != $(this).val() || $name_customer_search.val() == "") {
			$(this).data("last", $(this).val());
			if ($(this).val() == "") {
				$name_customer_search.val("");
				// tentativa de solução do loop - passar o parent aqui
				$(".name_customer_search span", parent_customer_search).html("");
				//$(".name_customer_search span").html("");
			} else {
				$("#id_customer_search_form").val($(this).val());
				$("#id_customer_search_form").parents("form").submit();
			}
		}
	});
	$(".customer_search_button").click(function() {
		parent_customer_search = $(this).parent().parent();
		$("#customer_search").modal({
			"backdrop": true,
			'keyboard': true,
			'show': true
		});
		$(".customer_name").val("");
		$(".id_customer_search_form").val("");
		setTimeout(function() {
			$(".customer_name").focus();
		}, 200);
	});
	$("#customer_add_button").click(function() {
		// acrescentei este parent aqui - as vezes inseria cliente na bola
		// verde e não mostrava o nome
		parent_customer_search = $(this).parent().parent();
		$("#customer_rapid_add").modal({
			"backdrop": true,
			'keyboard': true,
			'show': true
		});
	});
	$("#b_customer_rapid_add").click(function() {
        var opparm = $("#rapid_customer_mobile_phone_op").val() || '0';
        var offsaleparm = $("#rapid_customer_offsale").val() || '0';

		Customer.rapidCreate($("#rapid_customer_name").val(), 
			$("#rapid_customer_phone").val(), 
			$("#rapid_customer_mobile_phone").val(), 
			//$("#rapid_customer_mobile_phone_op").val(), 
			opparm,
			$("#rapid_customer_email").val(), 
			$("#rapid_customer_obs").val(), 
			$("#rapid_customer_bp_indicatedby").val(),
			offsaleparm,
			$("#rapid_customer_birthday").val()
			);
	})
	$("#rapid_customer_phone,#rapid_customer_name").keypress(function(e) {
		if (e.keyCode == 13) {
			$("#b_customer_rapid_add").click();
		}
	});
	$(".customer_name, .customer_document, .customer_email").keypress(function(e) {
		if (e.keyCode == 13) {
			$("#id_customer_search_form").val("");
			$(this).parents("form").submit();
		}
	});
	$(".id_customer_search").keypress(function(e) {
		if (e.keyCode == 13) {
			$(this).change();
		}
	});	
});
