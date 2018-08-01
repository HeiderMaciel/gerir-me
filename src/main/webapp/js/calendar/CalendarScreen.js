var CalendarScreen = {
	creatModalTreatmentClass : function(calEvent){
		$("#treatment_class").modal({
			"show": true,
			"keyboard": true,
			"backdrop": true
		});
		$("#table_treatment_class tbody").html(  CalendarScreen.customerTableHtml(calEvent));
	},
	customerTableActions: function(id, status){
			if(status !== "Paid"){
				return "<div>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); CalendarScreen.showTreatment(global_calEvent);' type='button' class='btn' >"+
						"			<img src='/images/donw.png' style='width: 16px;'>"+
						"			Abrir"+
						"		</a>	"+
						"	</span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsPreOpen();' type='button' class='btn' >"+
						"			<img src='/images/treatment_preopen.png' style='width: 16px;'>"+
						"			Pré Agendado"+
						"		</a>	"+
						"	</span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsOpen();' type='button' class='btn' >"+
						"			<img src='/images/treatment_open.png' style='width: 16px;'>"+
						"			Agendado"+
						"		</a>	"+
						"	</span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsConfirmed();' type='button' class='btn'>"+
						"			<img src='/images/treatment_confirmed.png' style='width: 16px;'>"+
						"			Confirmado	"+
						"		</a>"+
						"	</span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsArrived();' type='button' class='btn'>"+
						"			<img src='/images/treatment_arrived.png' style='width: 16px;'>"+
						"			Chegou"+
						"		</a>"+
						"	</span>"+
						"	<span class='divider'></span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsReady();' type='button' class='btn'>"+
						"			<img src='/images/treatment_ready.png' style='width: 16px;'>"+
						"			Atendido"+
						"		</a>			            	"+
						"	</span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsMissed();' type='button' class='btn'>"+
						"			<img src='/images/treatment_missed.png' style='width: 16px;'>"+
						"			Faltou"+
						"		</a>"+
						"	</span>"+
						"	<span class='hide_on_paid'>"+
						"		<a onclick='setTreatmentGlobalFromId("+id+"); TreatmentStatusManager.markAsReSchedule();' type='button' class='btn'>"+
						"			<img src='/images/treatment_reschedule.png' style='width: 16px;'>"+
						"			Desmarcou"+
						"		</a>"+
						"	</span>"+
						"</div>";
		}else{
			return "";
		}
	},
//						 +"</td><td>"+getStatus(calEvent.detailAsText)
	customerTableHtml: function(calEvent){
		var treatments = "<tr><td>"+(calEvent.customerName)
						 +"</td><td>"+getStatus(calEvent.status)
						 +"</td><td>"+CalendarScreen.customerTableActions(calEvent.id, calEvent.status)
						 +"</td></tr>";
		if(calEvent.treatmentsIn && calEvent.treatmentsIn.length > 0){
			treatments += calEvent.treatmentsIn.map(function(treatment){
													return CalendarScreen.customerTableHtml(treatment);
												}).reduce(function(a,b){
													return a+b;
												});
		}
		return treatments;
	},
	creatModalTreatment: function(calEvent, isNew) {
		CalendarScreen.renewTratment();
		$('.hide_on_paid').show();
		if (calEvent.status && calEvent.status == Paid) {
			$('.hide_on_paid').hide();
		}
		if (calEvent.hasFlit) {
			$("#treatment_fit_show").show();
		} else {
			$("#treatment_fit_show").hide();
		}
		var tmpUser = global_users_obj[calEvent.userId];
		$("#user_treatment").val(tmpUser.id);
		$("#user_treatment").change();

		// Desabilita o campo "Profissional"
		$("#user_treatment").attr('disabled', 'disabled');

		$inputs = $("input", "#treatment_add");
		$("#treatment_add").modal({
			"show": true,
			"keyboard": true,
			"backdrop": true
		});
		if ((calEvent.command === '' || !calEvent.command) && 
			(global_commandControl == 1 /* daily */ ||
			 global_commandControl == 3 /* dailyCompany */)) {
			$.get("/calendar/" + encodeURIComponent(getDateBr(calEvent.start)) + 
				"/getNextCommandId", function(t) {
				$($inputs.get(0)).val(t);
				if (!isNew)
					$(".command").change();
			});
		} else {
			$($inputs.get(0)).val(calEvent.command);
		}
		if(calEvent.customerId){
			$("#cutomer_id_treatment").val(calEvent.customerId);
		}
		$("#cutomer_name_treatment").val(calEvent.customerName);
		$("#imgthumb_customer").attr("src", calEvent.customerThumb);
		$("#date_treatment").val(getDateBr(calEvent.start));
		$("#hour_treatment").val(getHourBr(calEvent.start));
		$("#obs_treatment").val(calEvent.obs);
		$("#auditstr_treatment").val(calEvent.auditstr);
		$("#hour_treatment_end").val(getHourBr(calEvent.end));
		$("#when_treatment").html(calEvent.end.getTextWhen());
		if (!isNew) {
			setTimeout('$("#cutomer_id_treatment").focus().change();', 500);
		} else {
			$("#cutomer_id_treatment").val('');
			setTimeout("$('#cutomer_name_treatment').select2('open');$('.name_customer_search span').html('');", 500)
		}	},
	showCustomers : function(calEvent){
		CalendarScreen.creatModalTreatmentClass(calEvent);
	},
	showTreatment: function(calEvent) {
		CalendarScreen.creatModalTreatment(calEvent, false);
		$("#treatment_id").val(calEvent.id);
		TreatmentManger.creatTreatmentDetail(calEvent);
	},
	closeTreatmentPopUp: function() {
		$('#treatment_add').modal('hide');
		CalendarScreen.renewTratment();
	},
	getActivities: function() {
		DataManager.getActivities($("#user_treatment").val(), function(activitysObj) {
			if (activitysObj.length < 1) {
				var msg = "Você não tem serviços/procedimentos cadastrados, " +
				"ou ainda não associou serviços/procedimentos a este profissional.\n\n" +
				"Faça a associação no cadastro do profissional na aba de serviços " +
				"ou verifique o parâmetro <Associa Profissional / Serviço> em Configurações"
				alert (msg)
			}
			global_activitiesObj = activitysObj;
			$('#activitys option').remove();
			var ret = "";
			for (var i in activitysObj) {
				ret += "<option value='" + activitysObj[i].id + "'>" + activitysObj[i].name + "</option>";
			}
			$('#activitys').append(ret);
		});
	},
	removeTreatmentShowModal: function() {
		if (CalendarManager.calendarPermitions.deleteEvent) {
			$("#treatment_remove").modal({
				"show": true,
				"keyboard": true,
				"backdrop": true
			});
		} else {
			alert ("Suas permissões não permitem excluir agendamento");
		}
	},
	removeTreatment: function() {
		TreatmentManger.removeTreatmentById($("#treatment_id").val(), null, global_calEvent);
		$('#treatment_remove').modal('hide');
		$('#treatment_add').modal('hide');
	},
	renewTratment: function() {
		$(":input:not(.disabled)", '#treatment_div_span').removeAttr("disabled", "false");
		$("#treatment_detail_div").hide();
		$("#treatment_id").val(0);
		$("#treatment_parent_id").val(0);
	},
	registerPayment: function() {
		if ($("#command_treatment").val() && $("#command_treatment").val() !== "0") {
			window.open("/financial_cashier/register_payment?command=" + $(".command").val() + "&date=" + encodeURIComponent($(".start_treatment").val()), "payment");
			CalendarScreen.closeTreatmentPopUp();
		} else {
			if (global_commandControl == 2 /* ever */ ||
				global_commandControl == 4 /* everCompany */) {
				$.get("/calendar/" + encodeURIComponent($(".start_treatment").val()) + 
					"/getNextCommandId", function(t) {
					//	$($inputs.get(0)).val(t);
					$(".command").val(t)
					$(".command").change();
					window.open("/financial_cashier/register_payment?command=" + $(".command").val() + "&date=" + encodeURIComponent($(".start_treatment").val()), "payment");
					CalendarScreen.closeTreatmentPopUp();
				});
			} else {
				// 0 never
				alert("É necessário inserir o número da comanda!");
			}
		}
	}
};
