var buildCalendar = function(users,treatments,interval,intervalAlt,startCalendar,endCalendar,
	    commandControl, autoOpenCalendar, permissions,
	    features, unit, calendarShowLight,
	    calendarShowInterval, ignoreParameter){
			if(!ignoreParameter){
				global_calendar_params = Array.prototype.slice.call(arguments);//this means clone;
				userListFromGlobal(users);
				if(CalendarManager.hasLastUsers(unit)){
					lastUsers = CalendarManager.lastUsers(unit);
					if(lastUsers.length !== users.length && lastUsers.length < global_max_user_in_select_calendar){
						$("#user").val(lastUsers.map(function(obj){
							return obj.id;
						})).change();
						return;
					}
				}
			}
			if(permissions){
				CalendarManager.calendarPermitions = permissions;
			}

			var overlapEventsSeparate = true;
			if(features){
				CalendarManager.calendarFeatures = features;
				if(CalendarManager.calendarFeatures.useTreatmentClass){
					CalendarManager.calendarFeatures.useTreatmentClass = !CalendarManager.calendarFeatures.useTreatmentClass;
					overlapEventsSeparate = false;
					// $("#show_conflits").show().click(function(){
					// 	CalendarManager.calendarFeatures.useTreatmentClass = !CalendarManager.calendarFeatures.useTreatmentClass;
					// 	$('#calendar').weekCalendar("refresh");
					// });
				}else{
					$("#show_conflits").hide();

					var toggle = true;

					$('#toggle_interval').click(function() {
						if (toggle) {
							$('#calendar').weekCalendar('option', 'timeslotsPerHour', 60/intervalAlt);
							global_interval = intervalAlt; //15;
						} else {
							$('#calendar').weekCalendar('option', 'timeslotsPerHour', 60/interval);
							global_interval = interval;
						}

						toggle = !toggle;

						$('#calendar').weekCalendar("refresh");
					});
				}
			}

			if(!interval){
				interval = 30;
			}

			if(!intervalAlt){
				intervalAlt = 15;
			}

			if(!startCalendar){
				startCalendar = 8;
			}

			if(!endCalendar){
				endCalendar = 24;
			}else{
				endCalendar;
			}
			global_interval = interval;
			global_start = startCalendar;
			global_start_hour = startCalendar;
			global_end_hour = endCalendar;
			global_end = endCalendar;
			global_users_obj = filterUsers(users, !ignoreParameter);
			global_users = treatUsersServer(global_users_obj);
			global_usersIds = treatUsersServerId(global_users_obj);
			global_treatments = treatTreatmentsServer(treatments);
			global_commandControl = commandControl;
			global_calendarShowLight = calendarShowLight;
			resizeCalendar();
			global_load = users.length==1 || global_load || autoOpenCalendar;
			if(global_load ){
				var daysAux = 1;
				if (global_users.length <=3){
					daysAux = 3;
				}
				var $calendar = $('#calendar').weekCalendar({
						allowCalEventOverlap : true,
						overlapEventsSeparate: overlapEventsSeparate,
						defaultEventLength : 1,
						timeslotHeight : 60,
						date : gup("datestart") ? FactoryDate.byTime(parseInt(gup("datestart"))) : Date.toDay(),
						startOnFirstDayOfWeek :function(calendar) {return false;},
						allowEventDelete : CalendarManager.calendarPermitions.deleteEvent,
						title: " ",
						timeslotsPerHour: 60/global_interval,
						showAllTime : false,
						use24Hour:true,
						scrollToHourMillis : 0,
						buttonText : {
							today : "Hoje",
							lastWeek : "&nbsp;&lt;&nbsp;",
							nextWeek : "&nbsp;&gt;&nbsp;"
						},
						eventDelete: function(calEvent, element, dayFreeBusyManager,calendar, clickEvent) {

							if(CalendarManager.calendarPermitions.deleteEvent){
								var message = "";
								if(calEvent.status == "event" || calEvent.status  == "not_work"){
									message = "Tem certeza que deseja excluir o bloqueio do profissional " +calEvent.userName+ "?";
								}else{
									message = "Tem certeza que deseja excluir o atendimento do cliente "+calEvent.customerName+ "?";
								}
								if(confirm(message)){
									TreatmentManger.removeTreatmentById(calEvent.id,calendar,calEvent);
								}
							} else {
								alert ("Suas permissões não permitem excluir agendamento");
							}
						},
						businessHours: {start: startCalendar, end: endCalendar, limitDisplay: true},
						height: function($calendar){
							return $(window).height() - $('h1').outerHeight(true);
						},
						eventRender : function(calEvent, $event) {
							if(calEvent.status == 'not_work'){
								$event.css("backgroundColor", "#9C9C9C");
								$event.find(".wc-time").css({backgroundColor: " #9C9C9C ", border:"1px solid #000000"});
							} else {
								// antes nao tinha o else. isso vin direto depois do if
								// por isso que refazia no final, eu acho
								// o trecho comentado
								var color = getColor(calEvent.status, calEvent.status2);
								// antes o controno do header era preto - deixei da cor da agenda
								//$event.find(".wc-time").css({backgroundColor: color.headColor, color:color.text, border:"1px solid #000000"});
								if (!calendarShowLight) {
									$event.find(".wc-time").css({backgroundColor: color.headColor, color:color.text, border:"1px solid " + color.headColor});
								} else {
									// usa cores claras o que antes era header vira só a borda e usa a cor
									// do complemento no header
									$event.find(".wc-time").css({backgroundColor: color.color, color:color.text, border:"1px solid " + color.headColor});
								}
								$event.css("backgroundColor", color.color);
								$event.css("color", color.text);
								//$event.css("font-size", "28px");
								// coloquei essa borda - mas para de mover para primeira e ultima coluna na agenda
								//$event.css ("border", "1px solid " + color.headColor);
								// colocando só no botom funcionou
								$event.css ("border-bottom", "1px solid " + color.headColor);
							}
/*
							if(calEvent.status == 'not_work'){
								$event.css("backgroundColor", "#9C9C9C");
								$event.find(".wc-time").css({backgroundColor: " #9C9C9C ", border:"1px solid #000000"});
							}
*/
						},
						timeSeparator : ' Ate ',
						eventHeader: function(calEvent, calendar){
							var options = calendar.weekCalendar('option');
							var one_hour = 3600000;
							var displayTitleWithTime = calEvent.end.getTime() - calEvent.start.getTime() <= (one_hour / options.timeslotsPerHour);
							var timeAux = "";
					      	var hasEdoctusSystem = $('.has-edoctus-system').length > 0;
						    if (hasEdoctusSystem) {
							//if (document.location.href.indexOf("edoctus") != -1) {
								if (!calendarShowInterval) {
									timeAux = calendar.weekCalendar('formatTime', calEvent.start) + " "
								}
							}

							if (displayTitleWithTime) {
								//return calendar.weekCalendar('formatTime', calEvent.start) + " " + calEvent.title;
								return timeAux + calEvent.title;
							} else {
								//return calendar.weekCalendar('formatTime', calEvent.start) + " " + calEvent.title+" "+getStatus(calEvent.status, calEvent.hasFlit);
								return timeAux + calEvent.title+" "+getStatus(calEvent.status, calEvent.hasFlit);
							}
						},
						freeBusyRender:function(freeBusy, $freeBusy, calendar){
							if(!freeBusy.free){
								$freeBusy.addClass("free-busy-busy");
								$freeBusy.addClass("event");
							}else{
								$freeBusy.addClass("free-busy-free");
							}
							if(freeBusy.description)
								$freeBusy.html(freeBusy.description);
							return $freeBusy;
						},
						eventBody: function(calEvent, calendar) {
							var options = calendar.weekCalendar('option');
							if (calendarShowInterval) {
								return calendar.weekCalendar('formatTime', calEvent.start) +options.timeSeparator + calendar.weekCalendar('formatTime', calEvent.end);
							} else {
								return ""
							}
						},
						eventClick: function(calEvent, element, dayFreeBusyManager, calendar, clickEvent){
							if(CalendarManager.calendarPermitions.editEvent){
								if(click1 === null && !isMobile.any){
									click1 = Date.toDay();
								}else if(isMobile.any || (Date.toDay().getTime() - click1.getTime() < 300)  ){
									if(calEvent.customerId){
										if(calEvent.treatmentsIn && calEvent.treatmentsIn.length > 0 ){
											CalendarScreen.showCustomers(calEvent);
										}else{
											CalendarScreen.showTreatment(calEvent);
										}
									}else{
										// se nao tem cliente é bloqueio de profissional
										if (calEvent.title == "") {
											obs = "Bloqueio sem descrição"
										} else {
											obs = calEvent.title
										}
										alert(calEvent.userName + " - " + obs);
									}
								}
								click1 = Date.toDay();
							}
						},
						draggable : function(){
							return CalendarManager.calendarPermitions.moveEvent;
						},
						eventDrag: function(calEvent, element) {},
						eventDrop: function(calEvent, element) {
							if(CalendarManager.calendarPermitions.moveEvent){
								saveMovement(calEvent);
							}
						},
						eventResize: function(calEvent, element) {
							saveMovement(calEvent);
						},
						eventNew : function(calEvent, $event, FreeBusyManager, calendar) {
							if(CalendarManager.calendarPermitions.newEvent){
								$("#treatment_add_form_id").val('');
								$("#imgthumb_customer").attr("src", '');

								$(calendar).weekCalendar('removeEvent',calEvent.id);
								CalendarScreen.creatModalTreatment(calEvent,true);
							}else{
								$event.remove();
							}
						},
						data: function(start, end, callback) {
							TreatmentManger.getTreatmentsByApi(start,end,callback);
							$("#data_calendar").val(getDateBr(start));
							$("#data_calendar_end").val(getDateBr(FactoryDate.byTime(end.getTime()-1)));
							buildDataField(start,end);
						},
						users: global_users,
						showAsSeparateUser: true,
						displayOddEven: true,
						displayFreeBusys: true,
						daysToShow: daysAux,  // era 1 - rigel
						switchDisplay: {'1 Dia': 1, '3 Dias': 3, '5 Dias': 5, '7 Dias': 7},
						headerSeparator: ' ',
						useShortDayNames: false,
						// I18N
						firstDayOfWeek: $.datepicker.regional['pt-BR'].firstDay,
						shortDays: $.datepicker.regional['pt-BR'].dayNamesShort,
						longDays: $.datepicker.regional['pt-BR'].dayNames,
						shortMonths: $.datepicker.regional['pt-BR'].monthNamesShort,
						longMonths: $.datepicker.regional['pt-BR'].monthNames,
						dateFormat: "d/m/y"
					});
				}else{
					$("#calendar").html(
						'<form class="vr-form">' +
						'	<input size="19" name="data_calendar_end" id="data_calendar_end" class="date">' +
						'</form>'
					);

					$("#data_calendar").datepicker({
						beforeShow: function() {
							setTimeout(function(){
								$('#ui-datepicker-div').css('z-index',10000);

							}, 0);
						}
					}).val(Date.toDay().getDateBr());

					$("#data_calendar_end").datepicker({
						beforeShow: function() {
							setTimeout(function(){
								$('#ui-datepicker-div').css('z-index',10000);

							}, 0);
						}
					}).val(Date.toDay().getDateBr());

				}
			};

var stopProcess = false;

var processOffSale = function() {
	$("#offsale").val(Customer.current.offsale).change();
};

$(function(){
	$('#treatment_add').on('hidden.bs.modal', function (e) {
		/* comentei a pedido da dayana da pierre o pessoal entrava para excluir e vinha a mensagem eles clicavam no ok sem ler
		e a exclusão era revertida
		if( $("#cutomer_id_treatment").val() && $("#table_detail_table tbody > *").length === 0 && $("#activitys").val() && confirm("Deseja salvar o atendimento atual?")){
			$("#add_detail_button").click();
		}
		*/
	});
	$("#customer_arrived").click(TreatmentStatusManager.markAsArrived);
	$("#customer_confirmed").click(TreatmentStatusManager.markAsConfirmed);
	$("#customer_open").click(TreatmentStatusManager.markAsOpen);
	$("#customer_preopen").click(TreatmentStatusManager.markAsPreOpen);
	$("#customer_missed").click(TreatmentStatusManager.markAsMissed);
	$("#customer_reschedule").click(TreatmentStatusManager.markAsReSchedule);
	$("#customer_ready").click(TreatmentStatusManager.markAsReady);
	$("#user_treatment").change(function(){
		if($("#treatment_id").val() !== "0" && $("#treatment_id").val() !== ""){
			$("#treatment_id").val("");
			onCustomerSelect();
		}
	});

	$("#date_treatment, #hour_treatment, #hour_treatment_end, #obs_treatment").change(function(){
		TreatmentManger.saveTreatment();
	});

	/*
	$("#activitys").select2({
		formatResult : Customer.hasEspecialFormat,
		formatSelection: Customer.hasEspecialFormat
	});
	*/

	$("#treatment_fit_show").click(function(){
		var treatment = global_treatments.filter(function(a){ return a.id==global_calEvent.id })[0];
		$("#treatment_add").modal('hide');
		setTimeout(function(){ CalendarScreen.showTreatment(treatment.conflits[0]);},500);
	});
	$("#treatment_fit").click(function(){
		$("#animal").val("");
		$("#offsale").val("");

		$("#cutomer_id_treatment").val("");
		$("#command_treatment").val("");
		$("#obs_treatment").val(""); // rigel 23/10/2017
		$("#imgthumb_customer").attr("src", "");
		//$("#treatment_parent_id").val($("#treatment_id").val());
		$("#treatment_id").val("")
		$($(".name_customer_search")[2]).select2("open")
		$(".name_customer_search span").html("");
	});

	$("#treatment_div").click(function(){
		TreatmentManger.saveTreatment();
	});


	var callApiLock = false;
	$("#add_detail_button").click(function(){
		if (!callApiLock) {
			callApiLock = true
			var aux = $("#auxiliar").val() || 0;
			var ani = $("#animal").val()   || 0;
			var tooth = $("#tooth").val()  || 0;
			var off = $("#offsale").val()  || 0;

			var treatmentId = $("#treatment_id").val();
			var activities = $("#activitys").val();

			TreatmentManger.addDetail(treatmentId, activities, aux, ani, tooth, off);

			$("#auxiliar").val("").change();

			setTimeout(function(){
				callApiLock = false;
			}, 200);

		} else {
			alert("Já existe um processo em andamento. Aguarde o fim do processamento para clicar novamente!");
		}
	});

	$(".send_email_customer").click(function(){
		sendEmailCustomer({id:$("#treatment_id").val()});
	});

	$(".send_email_user").click(function(){
		sendEmailUser({id:$("#treatment_id").val()});
	});

	$("#cutomer_id_treatment").change(function(){
		var hasPetSystem = $('.has-pet-system').length > 0;
		if (hasPetSystem) {
			getAnimals();
		}
	});

	$("#user_treatment").change(function(){
		CalendarScreen.getActivities();
	});
	$("#activitys").change(function(){
		setTimeout(function(){
			$("#add_detail_button").focus();
		},10);
	});
	DataManager.getUsers(function(userObj){
			user_str = "";
			for(var i in userObj){
				user_str += "<option value='"+userObj[i].id+"'>"+userObj[i].name+"</option>";
			}
			$("#user_treatment").html(user_str);
			$("#user_treatment").val("");
			// rigel 19/11/2016
			// tentativa de limpar animal e convenio de um atend para outro
			// zerou o select de aminal, mas nao tirou o set do campo
			// pelo menos quando agenda nao seta o animal anterior
			getAnimals();
			$("#animal").val("").change();
			$("#offsale").val("").change();
	});
	$('#tooth').toothField(true);
	$('#auxiliar').auxiliarField(false);
    $("#offsale").offSaleField(true);
    Customer.addonsListeners.push(processOffSale);
	$("#customer_for_seach").change(function(){
		refreshCalendarByAjax();
	});
	unitListFromServer();
	groupsListFromServer();
	var dateUrlParameter = function(){
		return "&datestart="+$("#data_calendar").datepicker('getDate').getTime()+"&dateend="+$("#data_calendar_end").datepicker('getDate').getTime();
	}
	$("#user").change(function() {
		$('#calendar *').remove();
		var users = $(this).val();
		var params =  Array.prototype.slice.call(global_calendar_params);//this means clone
		if(!users){
			params[userParameterIndex] = [];
		}else{
			params[userParameterIndex] = params[userParameterIndex].filter(function(user){
				if (params[userParameterIndex].length == 1) {
					// setar 3 dias
				}
				return users.some(function(filterUser){
					return  filterUser== user.id;
				});
			});
		}
		params[userParameterIndex] = params[userParameterIndex].length != 0 ? params[userParameterIndex] : global_calendar_params[userParameterIndex];
		CalendarManager.lastUsers($("#units").val(),params[userParameterIndex]);
		params[ignoreParameterIndex] = true;//ignoreParameter
		buildCalendar.apply(this, params);
		$('#calendar').weekCalendar("refresh");
	});
	$("#groups").change(function(){
		var unit = $("#units").val();
		if(!unit){
			window.location.href="/calendar?load=true&notUnit=true&group="+$(this).val()+dateUrlParameter();
		}else{
			window.location.href="/calendar?load=true&unit="+$("#units").val()+"&group="+$(this).val()+dateUrlParameter();
		}
	});
	$('#rapid_customer_obs').change(function(){
		$('#obs_treatment').val($('#rapid_customer_obs').val());
	});
	$('#treatment_add').on('hidden', function () {
 		global_calEvent = undefined;
 	});

	$("#units").change(function(){
		var unit = $("#units").val();
		$.get("/calendar/currentUnit/"+unit,function(results){
			window.location.href="/calendar?load=true&unit="+$("#units").val()+dateUrlParameter();
		});
	});
	$(".command").change(function(){
		if($("#treatment_id").val() !=='' && $("#treatment_id").val() !=='0'){
			var newCommand = $(this).val();
			$.post("/calendar/changeCommandId",{"treatmentId":$("#treatment_id").val(), "command" : newCommand},function(result){
				result = eval(result);
				if(result == "1")
					global_calEvent.command = newCommand;
				else{
					alert(result);
					$(".command").val('0');
				}

			});
		}
	});
	//setTimeout("createHours()",100);

	$("#keep_with_customer").click(function(){
		if(Customer.current){
			setTimeout('$("#cutomer_id_treatment").val('+Customer.current.id+').focus().change();', 500);
		}else{
			alert("Não há cliente selecionado");
		}
	});

	$("#start_block").click(function() {
		if(confirm("Tem certeza que deseja bloquear o horário de "+$("#hour_treatment").val()+" até "+$("#hour_treatment_end").val())){
			var start = $("#date_treatment").val()+" "+$("#hour_treatment").val();
			var end = $("#date_treatment").val()+" "+$("#hour_treatment_end").val();
			var user = $("#user_treatment").val();
			var obs = $("#obs_treatment").val();
			EventBusyController.save(new EventBussy(start, end, user, obs), function(sucess, error){
				if(sucess){
					alert("Bloqueio salvo com sucesso!");
					// 09/10/2017 - rigel para mostrar bloqueio
					// fora do dia corrente
					// antes tinha que dar setinha pra frente e voltar
					$('#calendar').weekCalendar("refresh");
				}else{
					alert(eval(error));
					CalendarManager.closeTreatmentPopUp();
				}
			});
		}
	});
});
