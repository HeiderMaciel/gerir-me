$(function() {
	AuthUtil.updateDataCompany();
	AuthUtil.updateDataUnit();
	AuthUtil.updateDataUser(); // rigel 18/09/2017
	$.ajaxSetup({
		dataType: "text",
		error: function(t) {
			//alert("Erro ao processar transação! Tente novamente mais tarde")
		}
	});
	messages();
	$(document).ajaxStop(messages);
	$(document).ajaxStart(function() {
		$("#ajax_loader").show();
	});
	$(document).ajaxComplete(function() {
		$("#ajax_loader").hide();
	});
	$('.datetime').datetimepicker({
		format: 'd/m/Y H:i',
		formatTime: 'H:i',
		formatDate: 'd/m/Y'
	});
	jQuery.fn.datetimeDecode = function() {
		$(this).each(function() {
			var value_ = $(this).val();
			if (value_ != '') {
				var date = new Date(parseInt(value_));
				$(this).val(date.getDateBr() + " " + date.getHourBr());
			}
		});
	};
	$(".datetime").datetimeDecode();
	$("#ajax_loader").hide();
	setTimeout(function() {
		$('.local_storage').localStorageField().change();
	}, 200);
	$('[alt]').qtip();
	$(".cnpj").mask_input("99.999.999/9999-99");
	$(".cpf").mask_input("999.999.999-99");
	$(".cep").mask_input("99999-999");
	$(".time").mask_input("99:99");
	$(".filter_button").click(function() {
		$('.filter_form').toggle();
	});
	$(".currency").maskMoney({
		thousands: ''
	});
	$(".translate_date").each(function() {
		$(this).html(getDateBr(new Date(parseInt($(this).text()))));
	});
	$(".translate_money").each(function() {
		$(this).html(parseFloat($(this).text()).formatMoney())
	});
	$(".phone").phoneMask().each(function() {
		if ($(this).val() == "") {
			if (AuthUtil.unit.defaultDDD) {
				$(this).val('(' + AuthUtil.unit.defaultDDD + ') ');
			} else {
				$(this).val('(31) ');
			}
		}
	});
	$(".date").datepicker({
		beforeShow: function() {
			setTimeout(function() {
				$('#ui-datepicker-div').css('z-index', 10000);
			}, 0);
		}
	}).change(function() {
		if ($(this).datepicker('getDate') && $(this).datepicker('getDate').getYearGap() > 10) {
			alert('Verifique data futura!');
		} else if ($(this).datepicker('getDate') && $(this).datepicker('getDate').getYearGap() < -50) {
			alert('Verifique data passada!');
		}
	}); //Validar datas 10 anos para frente 50 anos para traz....
	$(".date").each(function() {
		var value_ = $(this).val();
		if (value_ != '') {
			$(this).val(getDateBr(new Date(parseInt(value_))))
		}
	});
	$('select.state_field').stateField().change(function() {
		var value = $(this).val();
		$('select.city_field').cityField({
			'state': value
		});
	});
	$('select.city_field').cityField({
		'state': $('.state_field_value').val()
	});
	$(".date").mask_input("99/99/9999");
	$(".a_year_ago").val(getDateBr(new Date().getAYearAgo()));
	$(".to_day").val(getDateBr(new Date()));
	$(".start_of_month").val(getDateBr(new Date().getStartOfMonth()));
	$(".end_of_month").val(getDateBr(new Date().getEndOfMonth()));
	$(".start_of_week").val(getDateBr(new Date().getStartOfWeek()));
	$(".next_week").val(getDateBr(new Date().getNextWeek()));
	$(".next_month").val(getDateBr(new Date().getNextMonth()));
	$('.tabs').each(function() {
		var tabs = this;
		$('.tab_content').hide();
		$('.tab_content:first').show();
		$('.li:first').addClass('active');
		$('li', this).click(function() {
			$('li', tabs).removeClass('active');
			$(this).addClass('active');
			$('.tab_content').hide();
			var target = $('a', this).attr('href');
			$(target).show();
		});
	});
	$("._help").click(function() {
		var url = $(this).data('help-url');
		$('#help_iframe').attr('src', url);
		$('#help_modal').modal({
			"backdrop": true,
			'keyboard': true,
			'show': true
		});

	});
	$("._popover_basic").popover();
	$("._popover").popover();
	$("._popover").each(function(item) {
		$(this).parent().append($(this).data("original-title"));
	});
	$(".danger").click(function(e) {
		var result = true;
		if ($(this).attr("data-confirm-message"))
			if (!confirm("Você tem certeza " + $(this).attr("data-confirm-message") + "?")) {
				e.stopPropagation();
				result = false;
			} else {
				result = true;
			}
		return result;
	});
	$('.value_to_env').valueToEnvironment();
	$(".login_form [name=user]").val(gup("user"));
	$(".login_form [name=company]").val(gup("company"));
	loginInformationProcess();
	processMessages();
	try {
		$("select:not(.notselect2)").select2();
		$("select.alow_all").change(function() {
			var val = $(this).val();
			if (val && val.filter(function(value) {
					return value == "SELECT_ALL";
				}).length > 0) {
				var options = [];
				$("option", this).each(function() {
					if ($(this).val() != 'SELECT_ALL' && $(this).val() != '')
						options.push($(this).val());
				});
				$(this).select2('val', options);
			}
		});
	} catch (e) {}
	$(".url_parameter").each(function(i, item) {
		var name = $(item).attr("data-url-name") || $(item).attr("name");
		if (name) {
			var val = gup(name);
			var type = $(item).attr("type");
			if (type === 'checkbox') {
				if (val) {
					$(item).attr("checked", "checked");
				} else {
					$(item).removeAttr("checked");
				}
			} else {
				if (val) {
					$(item).val(val).change();
				}
			}
		}
	});
	$('.topbar_fallback').click(function() {
		$('.topbar').toggle();
		$('.sidebar').toggle();
		$('.sidebar').css('top', $('.topbar').height());
	});
	$(".add-on").click(function() {
		$(this).next().focus()
	}); //setfous in datepiker ao clicar no calendariozinho	
	validateEmail();
	if (document.location.href.indexOf("gerir") != -1) {
		toGerirme();
	} else if (document.location.href.indexOf("egrex") != -1) {
		toEgrex();
	} else if ((document.location.href.indexOf("esmile") != -1) || (document.location.href.indexOf("e-smile") != -1)){
		toEsmile();
	} else if (document.location.href.indexOf("edoctus") != -1) {
		toEdoctus();
	} else if ((document.location.href.indexOf("efisio") != -1) || (document.location.href.indexOf("ephysio") != -1)) {
		toEphysio();
	} else if (document.location.href.indexOf("ebellepet") != -1) {
		toEbellepet();
	} else {
		toEbelle();
	}
});
//Analitcs
$(function() {
	var local = document.location.href.split("/");
	var label = local[local.length - 2] + "/" + local[local.length - 1];
	var company = localStorage.getItem("_companyForLogin");
	if (AuthUtil.company) {
		_gaq.push(['_trackEvent', AuthUtil.company.name + " (" + company + ")", label, 'Using']);
	} else {
		_gaq.push(['_trackEvent', "Company(" + company + ")", label, 'Using']);
	}
});

