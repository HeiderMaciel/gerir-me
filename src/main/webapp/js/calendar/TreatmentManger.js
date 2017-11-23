var TreatmentManger = {
    updateTreatmentData: function(id, user, startDate, endDate, status, validate) {
        if (CalendarManager.calendarPermitions.editEvent) {
            if (validate === undefined) {
                validate = true;
            }
            var start = getDateBr(startDate) + " " + getHourBr(startDate);
            var end = getDateBr(endDate) + " " + getHourBr(endDate);
            var url = "";
            if (status == "event" || status == "not_work") {
                url = "/userEvent/" + id;
            } else {
                url = "/treatment/" + id;
            }
            $.post(url, {
                "user": user,
                "start": start,
                "end": end,
                "status": status,
                "validate": validate
            }, function(t) {
                eval("t=" + t);
                if (t != 1) {
                    if (confirm(t + "\n\n Deseja salvar assim mesmo?")) {
                        TreatmentManger.updateTreatmentData(id, user, startDate, endDate, status, false);
                    } else {
                        refreshCalendarByAjax();
                    }
                }
            });
        } else {
            alert ("Suas permissões não permitem editar agendamento");
                // o refresh aqui nao funcionou - 05/05/2017 rigel
                //    refreshCalendarByAjax();
                $('#calendar').weekCalendar("refresh");
        }
    },
    saveTreatment: function(force) {
        if (force === undefined) {
            force = false;
        }
        var customerId = $("#cutomer_id_treatment").val();
        if (customerId) {
            var url = "/treatment";
            var event_data_local = {
                "id": $("#treatment_id").val(),
                "command": $("#command_treatment").val(),
                "customerId": $("#cutomer_id_treatment").val(),
                "customer": $("#cutomer_id_treatment").val(),
                "userId": $("#user_treatment").val(),
                "status": "",
                "date": $("#date_treatment").val(),
                "obs": $("#obs_treatment").val(),
                "force": force,
                "hour_start": $("#hour_treatment").val(),
                "hour_end": $("#hour_treatment_end").val(),
                "treatmentConflit": $("#treatment_parent_id").val()
            };
            $.ajax(url, {
                "type": "PUT",
                "success": function(t) {
                    eval("var eventData = " + t);
                    if (eventData.status == 'success') {
                        eventData.start = FactoryDate.byTime(eventData.start);
                        eventData.end = FactoryDate.byTime(eventData.end);
                        $("#treatment_id").val(eventData.id);
                        $("#command_treatment").val(eventData.command);
                        $("#hour_treatment_end").val(getHourBr(eventData.end));
                        $("#when_treatment").html(eventData.end.getTextWhen());
                        eventData.userId = global_usersIds.indexOf(parseInt($("#user_treatment").val()));
                        global_calEvent = eventData;
                        TreatmentManger.creatTreatmentDetail(eventData);
                    } else {
                        if (confirm(eventData.message + "\n\n Deseja agendar assim mesmo?")) {
                            TreatmentManger.saveTreatment(true);
                        }
                    }
                },
                "error": function(response) {
                    alert("Erro ao adicionar atendimento!");
                },
                "data": {
                    "force": event_data_local.force,
                    "id": event_data_local.id,
                    "customer": event_data_local.customerId,
                    "user": event_data_local.userId,
                    "date": event_data_local.date,
                    "hour_start": event_data_local.hour_start,
                    "hour_end": event_data_local.hour_end,
                    "command": event_data_local.command,
                    "obs": event_data_local.obs,
                    "treatmentConflit": event_data_local.treatmentConflit
                }
            });
        }
    },
    addDetail: function(treatmentId, activityId, auxiliar, animal, tooth, offsale, valida) {
        if (!valida) {
            valida = "true";
        }
        $.post("/treatment_detail", {
            "activity": activityId,
            "auxiliar": auxiliar,
            "animal": animal,
            "tooth": tooth,
            "offsale": offsale,
            "id": treatmentId,
            "validate": valida
        }, function(t) {
            eval("t=" + t)
            if (t == 1) {
                TreatmentManger.creatTreatmentDetail(global_calEvent);
            } else {
                if (t.indexOf ("Já existe este serviço") != -1) {
					if (!confirm(t + "\n\n" + "Clique em **<cancelar>** para agendar assim mesmo")) {
						TreatmentManger.addDetail(treatmentId, activityId, auxiliar, animal, tooth, offsale, "false");
					}
				} else {
					if (confirm(t + "\n\n" + "Deseja agendar assim mesmo?")) {
						TreatmentManger.addDetail(treatmentId, activityId, auxiliar, animal, tooth, offsale, "false");
					}
				}
            }
        });
    },
    removeDetail: function(detailId) {
        if (CalendarManager.calendarPermitions.deleteEvent) {
            if (confirm("Tem certeza que deseja excluir este serviço?")) {
                var url = "/treatment/detail/" + detailId;
                $.ajax(url, {
                    "type": "DELETE",
                    "success": function() {
                        TreatmentManger.creatTreatmentDetail(global_calEvent);
                    },
                    "error": function(response) {
                        alert("Erro ao exluir atendimento!\n Verifique se o atendimento não foi pago!");
                    }
                });
            }
        } else {
            alert ("Suas permissões não permitem excluir agendamento");
        }
    },
    setAuxiliarDetail: function(detailId) {
        if (CalendarManager.calendarPermitions.editEvent) {
            var userId = $("#auxiliar").val();
            var msgAux = "";
            if (!userId) {
                userId = "0";
                msgAux = "\nComo o campo está vazio, um possível assistente no atendimento será excluído!"
            } else {
                msgAux = "";
            }
            if (confirm("Tem certeza que deseja atribuir este assistente a este serviço?" + msgAux)) {
                //
                // usado tambem na comanda e na agenda e no caixa
                // duplicado no register_payment.js
                //
                return $.post("/command/setaux", {
                  "user": userId,
                  "tdid": detailId,
                  "command": "0" // agenda 1 seria commanda
                }, function(results) {
                  if(results === 1 || results == "1"){
                      if (userId == "0") {
                        alert("Assistente excluído com sucesso");
                      } else {
                        alert("Assistente cadastrado com sucesso");
                      }
                    TreatmentManger.creatTreatmentDetail(global_calEvent);
                  }else{
                    alert(eval(results));
                  }
                });
            }
        } else {
            alert ("Suas permissões não permitem editar agendamento");
        }
    },
    setAnimalDetail: function(detailId) {
        if (CalendarManager.calendarPermitions.editEvent) {
            var petId = $("#animal").val();
            var msgAux = "";
            if (!petId) {
                petId = "0";
                msgAux = "\nComo o campo está vazio, um possível pet no atendimento será excluído!"
            } else {
                msgAux = "";
            }
            if (confirm("Tem certeza que deseja atribuir este pet a este serviço?" + msgAux)) {
                //
                // usado tambem na comanda e na agenda e no caixa
                // duplicado no register_payment.js
                //
                return $.post("/command/setpet", {
                  "animal": petId,
                  "tdid": detailId,
                  "command": "0" // agenda 1 seria commanda
                }, function(results) {
                  if(results === 1 || results == "1"){
                      if (petId == "0") {
                        alert("Pet excluído com sucesso");
                      } else {
                        alert("Pet cadastrado com sucesso");
                      }
                    TreatmentManger.creatTreatmentDetail(global_calEvent);
                  }else{
                    alert(eval(results));
                  }
                });
            }
        } else {
            alert ("Suas permissões não permitem editar agendamento");
        }
    },
    setToothDetail: function(detailId) {
		if (CalendarManager.calendarPermitions.editEvent) {
			var tooth = $("#tooth").val();
			var msgAux = "";
			if (!tooth) {
				tooth = "";
				msgAux = "\nComo o campo está vazio, um possível dente no atendimento será excluído!"
			} else {
				msgAux = "";
			}
			if (confirm("Tem certeza que deseja atribuir este dente a este serviço?" + msgAux)) {
				//
				// usado tambem na comanda e na agenda e no caixa
				// duplicado no register_payment.js
				//
		        return $.post("/command/settooth", {
		          "tooth": tooth,
		          "tdid": detailId,
		          "command": "0" // agenda 1 seria commanda
		        }, function(results) {
		          if(results === 1 || results == "1"){
		          	if (tooth == "") {
			            alert("Dente excluído com sucesso");
		          	} else {
			            alert("Dente cadastrado com sucesso");
		          	}
					TreatmentManger.creatTreatmentDetail(global_calEvent);
		          }else{
		            alert(eval(results));
		          }
		        });
			}
		} else {
			alert ("Suas permissões não permitem editar agendamento");
		}
	},
    creatTreatmentDetail: function(calEvent) {
        var decodeStatus = function(status){
            trStatus = 0;
            trStatus2 = 1; // nao tem
            var row = [];
            row.push (status, '')
            return trStatusdecode ('',row)
        };
        var url = "/treatment/by_customer/" + calEvent.customerId + "/" + encodeURIComponent(getDateBr(calEvent.start)) + "/details";
        $.get(url, function(ret) {
            var details = [];
            eval("details=" + ret);
            var table = "";
            var hasAuditModule = $('.has-audit-module').length > 0;
            var hasAuxiliarModule = $('.has-auxiliar-module').length > 0;
            var hasUnitModule = $('.has-unit-module').length > 0;
            var hasPetSystem = $('.has-pet-system').length > 0;
            var hasEsmileSystem = $('.has-esmile-system').length > 0;
            for (var i = details.length - 1; i >= 0; i--) {
                detail = details[i];
                var auxAux = "<a href='/customer/edit?id=" + detail.auxiliarId + "' target='_customer_maste'>" + detail.auxiliar + "</a>"
                var auxPet = "<a href='/animal/edit_animal?id=" + detail.animalId + "' target='_animal_maste'>" + detail.animal + "</a>"
                table +=
                "<tr>" +
                "<td>" + detail.user +
                (hasUnitModule ? "</td><td>" + detail.unit : "") +
                (hasAuxiliarModule ? "</td><td>" + auxAux : "") +
                (hasPetSystem ? "</td><td>" + auxPet : "") +
                "</td><td>" + detail.activity +
                (hasEsmileSystem ? "</td><td>" + detail.tooth : "") +
                "</td><td>" + getHourBr(FactoryDate.byTime(detail.start)) +
                "</td><td>" + getHourBr(FactoryDate.byTime(detail.end)) +
                //"</td><td class='treatment-status'>" + detail.status +
                "</td><td>" + decodeStatus(detail.status) +
                "</td><td><a title='Excluir item' href='#' onclick='TreatmentManger.removeDetail(" + detail.id + ")'><img src='/images/delete.png'/></a></td>" +
                (hasPetSystem ? "</td><td><a title='Atribuir pet' href='#' onclick='TreatmentManger.setAnimalDetail(" + detail.id + ")'><img width='24px' src='/images/addpet.png'/></a></td>" : "") +
                (hasEsmileSystem ? "</td><td><a title='Atribuir dente' href='#' onclick='TreatmentManger.setToothDetail(" + detail.id + ")'><img width='24px' src='/images/addtooth.png'/></a></td>" : "") +
                (hasAuxiliarModule ? "</td><td><a title='Atribuir assistente' href='#' onclick='TreatmentManger.setAuxiliarDetail(" + detail.id + ")'><img width='24px' src='/images/user.png'/></a></td>" : "") +
                (hasAuditModule ? "</td><td><a title='" + detail.auditstr + "' href='#' ><img width='24px' src='/images/audit.png'/></a></td>" : "") +
                "</tr>";
                if (detail.treatment == calEvent.id) {
                    $("#hour_treatment_end").val(getHourBr(FactoryDate.byTime(detail.end)));
                    // rigel 09/08/2017 se trocasse status ou mesmo clicasse no
                    // status agendado antes de fechar o agendamento setava o
                    // default da divisão de agenda, mesmo o serviço tendo
                    // duração maior
                    calEvent.end = FactoryDate.byTime(detail.end);
                }
            }
            // rigel 09/08/2017
            global_calEvent = calEvent;
            $("#table_detail_table tbody").html(table);
            translateStatus();
            $("#treatment_detail_div").show();

            console.log('Treatment added');

            /*
            $("#activitys").select2('open');
            */
        });
        global_calEvent = calEvent;
    },
    removeTreatmentById: function(id, calendar, calEvent) {
        var url = "";
        if (calEvent.status == "event" || calEvent.status == "not_work") {
            url = "/userEvent/" + id;
        } else {
            url = "/treatment/" + id;
        }
        $.ajax(url, {
            "type": "DELETE",
            "success": function(response) {
                //alert ("vaiii sucess " + response)
                eval("var obj = " + response)
                if (obj.status == "error") {
                    alert (obj.message)
                } else {
                    //alert("Excluído com sucesso!");
                    if (calendar) {
                        calendar.weekCalendar('removeEvent', calEvent.id);
                    }
                }
            },
            "error": function(response) {
                alert("Erro ao excluir atendimento!\n Verifique se o atendimento não foi pago!");
            }
        });
    },
    getTreatmentsByApi: function(start, end, callback) {
        if (((Date.toDay().getTime() - lastCallTreatmentsByApi) > 1000) || (global_start != start || global_end != end)) { //Chamar com intevalo minimo de 1 segundo
            lastCallTreatmentsByApi = Date.toDay().getTime();
            global_start = start;
            global_end = end;
            var customer = $("#customer_for_seach").val();
            var currentUnit = "false";
            if (global_notUnit) {
                currentUnit = "true";
            }
            var filter = {
                "start": getDateBr(start),
                "end": getDateBr(end),
                "group": gup("group"),
                "user": gup("user"),
                "currentUnit": currentUnit
            };
            $.post("/calendar/treatments", filter, function(resp) {
                eval("localTreatments = " + resp);
                global_treatments = treatTreatmentsServer(localTreatments);
                callback({
                    options: {
                        defaultFreeBusy: {
                            free: true
                        }
                    },
                    events: global_treatments,
                    freebusys: global_freebusys
                });
                TreatmentManger.getFreebusys(callback);
            });
        }
    },
    getFreebusys: function(callback) {


        $.get("/calendar/freebusys/" + getDateBrAsUri(global_start) + "/" + getDateBrAsUri(global_end), function(resp) {});
    }
};