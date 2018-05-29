var makeSelect = function(field, methodName, allow_null, allow_null_message, callback, selectAll){
	DataManager[methodName](function(items) {
		var html = ""
		if (allow_null) {
			html = "<option value='' selected='true'> "+allow_null_message+"</option>";
		}
		if (selectAll) {
			html += "<option value='SELECT_ALL'>Todos</option>";
		}		
		for (var i in items) {
			var id = items[i].id || i;
			var desciption = (items[i].name || items[i].title || items[i]);
			html += "<option value='" + id + "'>" + desciption + "</option>";
		}
		if (callback) {
			callback(items);
		}
		$(field).html(html);
		$(field).change();
	});	
};

jQuery.fn.weekDaysField = function(allow_null, callback) {
	makeSelect(this, 'i18nDays', allow_null, 'Nenhum dia selecionado', callback);
};
jQuery.fn.costcenterField = function(allow_null, callback) {
	makeSelect(this, 'getCostCenters', allow_null, 'Selecione um centro de custo', callback);
};
jQuery.fn.offSaleField = function(allow_null, callback) {
	makeSelect(this, 'getOffSales', allow_null, 'Selecione um convênio', callback);
};

jQuery.fn.paymentTypeField = function(allow_null, selectAll, callback) {
	makeSelect(this, 'getPaymentTypes', allow_null, 'Selecione uma forma de pagamento', callback, selectAll);
};
jQuery.fn.productField = function(allow_null, callback) {
	makeSelect(this, 'getProducts', allow_null, 'Selecione um produto', callback);
};
jQuery.fn.lineField = function(allow_null, callback) {
	makeSelect(this, 'getLines', allow_null, 'Selecione uma linha', callback);
};

jQuery.fn.productTypeField = function(allow_null, callback) {
	makeSelect(this, 'getProductType', allow_null, 'Selecione um tipo produto', callback);
};


jQuery.fn.packagesField = function(allow_null, callback) {
	makeSelect(this, 'getPackages', allow_null, 'Selecione um pacote', callback);
};
jQuery.fn.brandsField = function(allow_null, callback) {
	makeSelect(this, 'getBrands', allow_null, 'Selecione uma marca', callback);
};
jQuery.fn.supplierField = function(allow_null, callback) {
	makeSelect(this, 'getSuppliers', allow_null, 'Selecione um fornecedor', callback);
};

jQuery.fn.accountField = function(allow_null, callback) {
	makeSelect(this, 'getAccount', allow_null, 'Selecione uma conta', callback);
};

jQuery.fn.operatorField = function(allow_null, callback) {
	makeSelect(this, 'getOperator', allow_null, 'Selecione uma operadora', callback);
};

jQuery.fn.originField = function(allow_null, callback) {
	makeSelect(this, 'getOrigin', allow_null, 'Selecione uma origem', callback);
};

jQuery.fn.monthField = function(allow_null, select_current_month) {
	makeSelect(this, 'i18nMonths', allow_null, 'Selecione um mês');
	if (select_current_month) {
		$(this).val(new Date().getMonth());
	}
	$(this).change();
};





jQuery.fn.termsField = function(allow_all, callback, termsSelector) {
	var field = this;
	_termsSelector = termsSelector || ".terms_and_conditions";
	$(field).change(function() {
		var filtered = field.items.filter(function(item) {
			return item.id == $(field).val();
		});
		filtered.forEach(function(item) {
			$(_termsSelector).data("terms", item.message);
			Environment.reprocess();
		});
	});
	DataManager.getTerms(function(terms) {
		field.items = terms;
		var html = "<option selected='true' value=''>Nenhum termo</option>"
		if (allow_all) {
			html += "<option value='SELECT_ALL'>Todos</option>";
		}
		for (i in terms) {
			html += "<option value='" + terms[i].id + "'>" + terms[i].name + "</option>";
		}
		$(field).html(html);
	});
};

jQuery.fn.chequeField = function(allow_null, callback, allow_all, show_all) {
	var field = this;
	DataManager.getCheques(function(items) {
		var html = ""
		if (allow_null) {
			html = "<option value='' selected='true'> Selecione um cheque</option>";
		}
		if (allow_all) {
			html += "<option value='SELECT_ALL'>Todas</option>";
		}
		for (var i = items.length - 1; i >= 0; i--) {
			html += "<option value='" + items[i].id + "'>" + 
			items[i].chequeDesc + "</option>";
		};
		if (callback) {
			callback(items);
		}
		$(field).html(html);
		$(field).change();
	}, show_all);
};

jQuery.fn.categoryAccountField = function(allow_null, callback, allow_all, show_all) {
	var field = this;
	DataManager.getCategoryAccount(function(items) {
		var html = ""
		if (allow_null) {
			html = "<option value='' selected='true'> Selecione uma categoria</option>";
		}
		if (allow_all) {
			html += "<option value='SELECT_ALL'>Todas</option>";
		}
		for (var i = items.length - 1; i >= 0; i--) {
			html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
		};
		if (callback) {
			callback(items);
		}
		$(field).html(html);
		$(field).change();
	}, show_all);
};


jQuery.fn.cashierField = function(allow_null, type, callback, useLocalStorage) {
	var cashierOuts = function(cashierId, field) {
		for (var i in field.cashiers) {
			cashier = field.cashiers[i];
			if (cashierId == cashier.id) {
				var dateAsInt = window.parseInt(cashier.openerDate);
				var dateAsDate = new Date(dateAsInt);
				var dateAsSting = getDateBr(dateAsDate);
				$(".cashier_opener_date").val(dateAsSting);
				$(".cashier_unit").val(cashier.unit);
				$(".cashier_user").val(cashier.userName);
				$(".cashier_staus").val(cashier.cashierStatus == 'Open' ? "Aberto" : "Fechado");
				return;
			}
		}
		$(".cashier_opener_date").val("");
		$(".cashier_unit").val("");
		$(".cashier_user").val("");
		return;
	}
	return this.each(function() {
		var field = this;
		$(field).removeClass('mini').removeClass('small').addClass("xlarge");
		$(field).change(function() {
			cashierOuts($(this).val(), this);
		})
		DataManager.getCashiers(function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'> Selecione um caixa</option>";
			}
			for (var i = 0; i < items.length; i++) {
				var dateAsInt = window.parseInt(items[i].openerDate);
				var dateAsDate = new Date(dateAsInt);
				html += "<option value='" + items[i].id + "'>" + items[i].idForCompany + " - " + getDateBr(dateAsDate) + " - " + items[i].userName + " - " + items[i].unit + "</option>";
			};
			field.cashiers = items;
			field.cashiers_map = items.reduce(function(current, obj) {
				current[obj.id] = obj;
				return current;
			}, {});
			$(field).html(html);
			if (useLocalStorage) {
				$(field).localStorageField();
			}
			if (callback)
				callback();
			$(field).change();
		}, type);
	});
};
jQuery.fn.cityField = function() {
	var params = arguments[0];
	var state = false;
	var allow_null = false;
	if (params && params.state) {
		state = params.state;
	} else {
		allow_null = params;
	}
	return this.each(function() {
		var field = this;
		var $field = $(this);
		var parent = $field.parent();
		$(field).change(function() {
			$('.city_field_value').val($field.val());
		});
		DataManager.getCity(state, function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'> Selecione uma cidade</option>";
			}
			for (var i in items) {
				html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$field.html(html);
			var backEndValue = $('.city_field_value', parent).val();
			if (backEndValue) {
				$field.val(backEndValue).change();
			}
		});
	});
};
jQuery.fn.stateField = function(allow_null) {
	return this.each(function() {
		var field = this;
		var $field = $(field);
		var parent = $field.parent();
		$(field).change(function() {
			$('.state_field_value').val($field.val());
		});
		DataManager.getStates(function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'> Selecione um estado</option>";
			}
			for (var i in items) {
				html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$field.html(html);
			var backEndValue = $('.state_field_value', parent).val();
			if (backEndValue) {
				$field.val(backEndValue).change();
			}
		});
	});
};
jQuery.fn.bankField = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getBanks', allow_null, "Selecione um banco");
	});
};

jQuery.fn.civilStatusField = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getCivilStatuses', allow_null, "Selecione um estado civil");
	});
};

jQuery.fn.toothField = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getTeeths', allow_null, "Selecione um dente");
	});
};

jQuery.fn.quizField = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getQuizzes', allow_null, "Selecione um questionário");
	});
};
jQuery.fn.quizSectionField = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getQuizSections', allow_null, "Selecione uma seção");
	});
};

jQuery.fn.quizDomainField = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getDomains', allow_null, "Selecione um domínio");
	});
};

jQuery.fn.mapIcon = function(allow_null) {
	return this.each(function() { 
		makeSelect(this, 'getMapIcons', allow_null, "Selecione um ícone");
	});
};
jQuery.fn.activityField = function(allow_null) {
	return this.each(function() {
		makeSelect(this, 'getActivities', allow_null, "Selecione um serviço");
	});
};
jQuery.fn.activityTypeField = function(allow_null) {
	return this.each(function() {
		makeSelect(this, 'getActivityTypes', allow_null, "Selecione um tipo serviço");
	});
};
jQuery.fn.notificationField = function(allow_null) {
	return this.each(function() {
		makeSelect(this, 'getNotifications', allow_null, "Selecione uma mensagem");
	});
};

jQuery.fn.unitField = function(allow_null, selectCurret, selectAll) {
	return this.each(function() {
		var field = this;
		DataManager.getUnis(function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'>Selecione uma unidade</option>";
			}
			for (i in items) {
				if ((selectCurret && items[i].isCurrent) || selectAll)
					html += "<option selected='true' value='" + items[i].id + "'>" + items[i].name + "</option>";
				else
					html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$(field).html(html);
			$(field).change()
		});
	});
};

jQuery.fn.payrollEventsField = function(allow_null, allow_all, selectAll) {
	return this.each(function() {
		var field = this;
		DataManager.getPayrollEvents(function(items) {
			var html = ""
			if (allow_null) {
				html += "<option value='' selected='true'>Selecione uma verba</option>";
			}
			if (allow_all) {
				html += "<option value='SELECT_ALL'>Todos</option>";
			}
			for (i in items) {
				if (selectAll) {
					selected = "selected='true'";
				} else {
					selected = "";
				}
				html += "<option value='" + items[i].id + "' " + selected + ">" + items[i].name + "</option>";
			}
			$(field).html(html);
			$(field).change();
		});
	});
};
jQuery.fn.inventoryCauseField = function(allow_null, notSale) {
	return this.each(function() {
		var field = this;
		DataManager.getInventoryCauses(function(items) {
			var html = ""
			if (allow_null) {
				html += "<option value='' selected='true'>Selecione um motivo</option>";
			}
			if (notSale) {
				html += "<option value='SELECT_ALL'>Todas</option>";
			}
			for (i in items) {
				html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$(field).html(html);
			$(field).change();
		});
	});
};
jQuery.fn.userGroupField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getUserGroups(function(groups) {
			var html = "";
			if (allow_null) {
			    var hasEdoctusSystem = $('.has-edoctus-system').length > 0;
			    var hasEsmileSystem = $('.has-esmile-system').length > 0;
			    var hasEphysioSystem = $('.has-ephysio-system').length > 0;
			    if (hasEdoctusSystem || hasEphysioSystem || hasEsmileSystem) {
					html += "<option selected='true' value=''>Selecione uma especialidade</option>"
				} else {
					html += "<option selected='true' value=''>Selecione um grupo</option>"
				}
			}
			for (i in groups) {
				html += "<option value='" + groups[i].id + "'>" + groups[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};
jQuery.fn.projectField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getProject(function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'> Selecione um projeto</option>";
			}
			for (i in items) {
				html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};
jQuery.fn.projectClassField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getProjectClass(function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'> Selecione uma classe de projeto</option>";
			}
			for (i in items) {
				html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};
jQuery.fn.projectStageField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getProjectStage(function(items) {
			var html = ""
			if (allow_null) {
				html = "<option value='' selected='true'> Selecione um estágio de projeto</option>";
			}
			for (i in items) {
				html += "<option value='" + items[i].id + "'>" + items[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};
jQuery.fn.stakeholderTypeField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getStakeholderType(function(stakeholderTypes) {
			var html = "";
			if (allow_null) {
				html += "<option selected='true' value=''>Nenhum participante</option>"
			}
			for (i in stakeholderTypes) {
				html += "<option value='" + stakeholderTypes[i].id + "'>" + stakeholderTypes[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};

jQuery.fn.relationshipTypeField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getRelationshipType(function(relationshipTypes) {
			var html = "";
			if (allow_null) {
				html += "<option selected='true' value=''>Selecione tipo relacionamento</option>"
			}
			for (i in relationshipTypes) {
				html += "<option value='" + relationshipTypes[i].id + "'>" + relationshipTypes[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};

jQuery.fn.ageRangeField = function(allow_null) {
	return this.each(function() {
		var field = this;
		DataManager.getAgeRange(function(ageRanges) {
			var html = "";
			if (allow_null) {
				html += "<option selected='true' value=''>Nenhum tipo faixa etária</option>"
			}
			for (i in ageRanges) {
				html += "<option value='" + ageRanges[i].id + "'>" + ageRanges[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};

jQuery.fn.userField = function(allow_all) {
	return this.each(function() {
		var field = this;
		DataManager.getUsers(function(users) {
			var html = "<option selected='true' value=''>Nenhum profissional</option>"
			if (allow_all) {
				html += "<option value='SELECT_ALL'>Todos</option>";
			}
			for (i in users) {
				html += "<option value='" + users[i].id + "'>" + users[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};

jQuery.fn.auxiliarField = function(allow_all) {
	return this.each(function() {
		var field = this;
		DataManager.getAuxiliars(function(auxiliars) {
			var html = "<option selected='true' value=''>Nenhum assistente</option>"
			if (allow_all) {
				html += "<option value='SELECT_ALL'>Todos</option>";
			}
			for (i in auxiliars) {
				html += "<option value='" + auxiliars[i].id + "'>" + auxiliars[i].name + "</option>";
			}
			$(field).html(html);
		});
	});
};
