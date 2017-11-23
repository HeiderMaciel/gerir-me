var DataManagerClass = function() {
	var datas = [];
	var userGroups = [];
	var users = [];
	var auxiliars = [];
	var animals = [];
	var all_users = [];
	var paymentTypes = [];
	var cheques = [];
	var activitys = [];
	var projectsections = [];
	var units = [];
	var chashiers = [];
	var causes = [];
	var banks = [];
	var civilstatuses = [];
	var quizzes = [];
	var quizsections = [];
	var quetions = [];
	var domains = [];
	var accounts = [];
	var operators = [];
	var categorys = [];
	var notifications = [];
	var brands = [];
	var suppliers = [];
	var terms = [];
	var costCenters = [];
	var teeths = [];


	this.getSelectDataSource = function(url_source, callback, params) {
		var name = url_source;
		if(params){
			for(var key in params){
				name += "_" + key + "_" + params[key]; 
			}
		}		
		if (datas[name]) {
			callback(datas[name]);
		} else {
			$.get(url_source, params, function(t) {
				eval("datas['"+name+"'] = " + t);
				callback(datas[name])
			});
		}
	}
	this.i18nDays = function(callback){
		return callback(i18n_days);
	};
	this.i18nMonths = function(callback){
		return callback(i18n_months);
	};

	this.getIcds = function(callback) {
		return this.getSelectDataSource("/edoctus/icd", callback);
	};

	this.getMapIcons = function(callback) {
		return this.getSelectDataSource("/mapicon", callback);
	};
	this.getNotifications = function(callback) {
		return this.getSelectDataSource("/notification/message", callback);
	};
	this.getTerms = function(callback) {
		return this.getSelectDataSource("/api/v2/terms", callback);
	};
	this.getStates = function(callback) {
		return this.getSelectDataSource("/crud/states", callback);
	};
	this.getCity = function(state, callback) {
		return this.getSelectDataSource("/crud/cities", callback, { state : state});
	};	
	this.getCostCenters = function(callback) {
		return this.getSelectDataSource("/api/v2/costcenter", callback);
	};
	this.getOffSales = function(callback) {
		return this.getSelectDataSource("/offsale", callback);
	};
	this.getProductType = function(callback) {
		return this.getSelectDataSource("/product/product_type", callback);
	};
	this.getLines = function(callback) {
		return this.getSelectDataSource("/product/product_line", callback);
	};

	this.getActivityTypes = function(callback) {
		var url = "/cash/getActivityTypes";
		return this.getSelectDataSource(url, callback);
	};

	this.getCRMActivities = function(callback) {
		var url = "/cash/getCRMActivities";
		return this.getSelectDataSource(url, callback);
	};

	this.getPackages = function(callback) {
		var url = "/cash/getPackages";
		return this.getSelectDataSource(url, callback);
	};
	this.getProducts = function(callback) {
		var url = "/product/for_sale";
		return this.getSelectDataSource(url, callback);
	};
	this.getAnimals = function(customer, callback) {
		if (arguments.length == 1) {
			callback = customer;
			customer = false;
		}
		// comentei pq não estava carregando novo pet no cdastro rápido 
		// na agenda - rigel dez 2017
//		if (animals[customer || 0] && animals[customer || 0].length > 0) {
			//callback(animals[customer || 0])

//		} else {
			if (customer)
				url = "/cash/getAnimals/" + customer;
			else {
				alert ("vaiii nao deve chegar aqui cash api animals =============== ")
				url = "/cash/getAnimals";
				customer = 0;
			}
			$.get(url, function(t) {
				eval("animals[customer] = " + t);
				callback(animals[customer])
			});
//		}
	};

	this.getActivities = function(user, callback) {
		if (arguments.length == 1) {
			callback = user;
			user = false;
		}
		if (activitys[user || 0] && activitys[user || 0].length > 0) {
			callback(activitys[user || 0])

		} else {
			if (user)
				url = "/cash/getActivities/" + user;
			else {
				url = "/cash/getActivities";
				user = 0;
			}
			$.get(url, function(t) {
				eval("activitys[user] = " + t);
				callback(activitys[user])
			});
		}
	};

	this.getProjectSections = function(project, callback) {
		if (arguments.length == 1) {
			callback = project;
			project = false;
		}
		url = "/project/getProjectSections/" + project;
		$.get(url, function(t) {
			eval("projectsections[project] = " + t);
			callback(projectsections[project])
		});
	};

	this.getPaymentTypes = function(callback) {
		if (paymentTypes.length > 0) {
			callback(paymentTypes)
		} else {
			url = "/cash/getPaymentTypes";
			$.get(url, function(t) {
				eval("paymentTypes = " + t);
				callback(paymentTypes);
			});
		}
	};
	this.getCheques = function(callback) {
		if (cheques.length > 0) {
			callback(cheques)
		} else {
			url = "/cash/getCheques";
			$.get(url, function(t) {
				eval("cheques = " + t);
				callback(cheques);
			});
		}
	};
	this.getBanks = function(callback) {
		if (banks.length > 0) {
			callback(banks)
		} else {
			url = "/crud/banks";
			$.get(url, function(t) {
				eval("banks = " + t);
				callback(banks);
			});
		}
	};

	this.getCivilStatuses = function(callback) {
		if (civilstatuses.length > 0) {
			callback(civilstatuses)
		} else {
			url = "/crud/civilstatuses";
			$.get(url, function(t) {
				eval("civilstatuses = " + t);
				callback(civilstatuses);
			});
		}
	};

	this.getTeeths = function(callback) {
		if (teeths.length > 0) {
			callback(teeths)
		} else {
			url = "/crud/teeths";
			$.get(url, function(t) {
				eval("teeths = " + t);
				callback(teeths);
			});
		}
	};

	this.getQuizzes = function(callback) {
		if (quizzes.length > 0) {
			callback(quizzes)
		} else {
			url = "/customer_report/quizzes";
			$.get(url, function(t) {
				eval("quizzes = " + t);
				callback(quizzes);
			});
		}
	};

	this.getQuizSections = function(callback) {
		if (quizsections.length > 0) {
			callback(quizsections)
		} else {
			url = "/customer_report/quizsections";
			$.get(url, function(t) {
				eval("quizsections = " + t);
				callback(quizsections);
			});
		}
	};

	this.getQuestions = function(quiz, callback) {
		if (arguments.length == 1) {
			callback = quiz;
			quiz = false;
		}
		if (quetions[quiz || 0] && quetions[quiz || 0].length > 0) {
			callback(quetions[quiz || 0])
		} else {
			if (quiz)
				url = "/api/v2/getQuestions/" + quiz;
			else {
				url = "/api/v2/getQuestions/";
				quiz = 0;
			}
			$.get(url, function(t) {
				eval("quetions[quiz] = " + t);
				callback(quetions[quiz])
			});
		}
	};

	this.getDomains = function(callback) {
		if (domains.length > 0) {
			callback(domains)
		} else {
			url = "/customer_report/domains";
			$.get(url, function(t) {
				eval("domains = " + t);
				callback(domains);
			});
		}
	};


	this.getUsers = function(callback) {
		if (users.length > 0) {
			callback(users);
		} else {
			$.get("/cash/getUsers", function(resp) {
				eval("users = " + resp);
				callback(users);
			});
		}
	}

	this.getAuxiliars = function(callback) {
		if (users.length > 0) {
			callback(auxiliars);
		} else {
			$.get("/cash/getAuxiliars", function(resp) {
				eval("auxiliars = " + resp);
				callback(auxiliars);
			});
		}
	}

	this.getUsersCurrentUnitCommand = function(callback) {
		if (users.length > 0) {
			callback(users);
		} else {
			$.get("/cash/getUsersCurrentUnitCommand", function(resp) {
				eval("users = " + resp);
				callback(users);
			});
		}
	}

	this.getUsersCurrentUnit = function(callback) {
		if (users.length > 0) {
			callback(users);
		} else {
			$.get("/cash/getUsersCurrentUnit", function(resp) {
				eval("users = " + resp);
				callback(users);
			});
		}
	}

	this.getAllUsers = function(callback) {
		if (all_users.length > 0) {
			callback(all_users);
		} else {
			$.get("/cash/getAllUsers", function(resp) {
				eval("all_users = " + resp);
				callback(all_users);
			});
		}
	}

	this.getBrands = function(callback) {
		if (brands.length > 0) {
			callback(brands);
		} else {
			$.get("/cash/brands", function(resp) {
				eval("brands = " + resp);
				callback(brands);
			});
		}
	}

	this.getSuppliers = function(callback) {
		if (suppliers.length > 0) {
			callback(suppliers);
		} else {
			$.get("/cash/suppliers", function(resp) {
				eval("suppliers = " + resp);
				callback(suppliers);
			});
		}
	}

	this.getUserGroupsForCalendar = function(callback) {
		return this.getSelectDataSource("/calendar/groups/for_calendar", callback);
	};
	
	this.getProject = function(callback) {
		return this.getSelectDataSource("/api/v2/project", callback);
	};

	this.getProjectClass = function(callback) {
		return this.getSelectDataSource("/api/v2/projectclass", callback);
	};

	this.getProjectStage = function(callback) {
		return this.getSelectDataSource("/api/v2/projectstage", callback);
	};

	this.getStakeholderType = function(callback) {
		return this.getSelectDataSource("/api/v2/stakeholdertype", callback);
	};

	this.getRelationshipType = function(callback) {
		return this.getSelectDataSource("/api/v2/relationshiptype", callback);
	};

	this.getAgeRange = function(callback) {
		return this.getSelectDataSource("/api/v2/agerange", callback);
	};

	this.getUserGroups = function(callback) {
		if (userGroups.length > 0) {
			callback(userGroups);
		} else {
			$.get("/calendar/groups", function(results) {
				eval("userGroups = " + results);
				callback(userGroups);
			});
		}
	}
	this.getInventoryCauses = function(callback) {
		if (causes.length > 0) {
			callback(causes);
		} else {
			$.get("/inventory/causes", function(results) {
				eval("var causes = " + results);
				callback(causes);
			});
		}
	};
	this.getPayrollEvents = function(callback) {
		if (causes.length > 0) {
			callback(causes);
		} else {
			$.get("/payroll/events", function(results) {
				eval("var events = " + results);
				callback(events);
			});
		}
	};
	this.getUnis = function(callback) {
		if (units.length > 0) {
			callback(units);
		} else {
			$.get("/inventory/units", function(results) {
				eval("units = " + results);
				callback(units);
			});
		}
	}
	this.getCategoryAccount = function(callback, all) {
		var url = "/account/category/list";
		if (all) {
			url += "/all";
		}
		if (categorys.length > 0) {
			callback(categorys)
		} else {
			$.get(url, function(ret) {
				eval("categorys=" + ret);
				callback(categorys);
			});
		}
	}

	this.getAccount = function(callback) {
		if (accounts.length > 0) {
			callback(accounts);
		} else {
			$.get("/account/list", function(results) {
				eval("accounts = " + results);
				callback(accounts);
			});
		}
	};	

	this.getOperator = function(callback) {
		if (operators.length > 0) {
			callback(operators);
		} else {
			$.get("/cash/operators", function(results) {
				eval("operators = " + results);
				callback(operators);
			});
		}
	};	

	this.getCashiers = function(callback, open_or_close) {
		if (!open_or_close)
			open_or_close = "open";
			
		// rigel 22/06/2017
		// coloque um false aqui na condição pq evitar carregar de novo tava 
		// furando o teste pra nao abrir outro caixa na mesma data
		//
		if (false) { //(chashiers[open_or_close] && chashiers[open_or_close].length > 0) {
			callback(chashiers[open_or_close]);
		} else {
			var url = "/cash/openCheckouts";
			if (open_or_close && open_or_close == "closed")
				url = "/cash/closedCheckouts";
			else if (open_or_close == "all")
				url = "/cash/allCheckouts";
			$.get(url, function(results) {
				eval("chashiers[open_or_close] = " + results);
				callback(chashiers[open_or_close]);
			});
		}
	}
};
var DataManager = new DataManagerClass();