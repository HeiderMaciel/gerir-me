var max_colunms_to_show = 35;
var global_hours_slot_top_start = 0;
var global_activitiesObj = [];
var global_animalsObj = [];
var global_calEvent = null;
var global_days = 1;
var global_treatments_obj = null;
var global_treatments = null;
var global_freebusys = [];
var global_users_obj = null;
var global_users = null;
var click1 = null;
var global_start = null;
var global_interval = null;
var global_calendarShowLight = false;
var global_end= null;
var global_commandControl = 1; /* daily */
var global_currentGroup = gup("group");
var global_start_hour = 8;
var global_end_hour = 24;
var userParameterIndex = 0;
var ignoreParameterIndex = 13;
var global_max_user_in_select_calendar = 15;
var global_load = gup('load');
var global_notUnit = gup("notUnit");
var global_calendar_params = {};
var updateParameter = function(){
    var parameters = localStorage.getItem("calendar_parameters");
    if(parameters){
      var calendarParametes = JSON.parse(parameters);
      if(calendarParametes.parameters){
        parameters = calendarParametes.parameters.split("&")
        if(parameters.filter(function(item){ return item.indexOf("datestart") > 0; }).length == 0){
          parameters.push("datestart=0");
          parameters.push("dateend=0");
        }
        var result = parameters.map(function(item){
            if(item.indexOf('datestart')==0){
              item = "datestart="+global_start.getTime();
            }
            if(item.indexOf('dateend')==0){
              item = "dateend="+global_end .getTime();
            }
            return item;
          });
        processGlobalParameters(result.join('&'));
      }
    }
}
var processGlobalParameters = function(parameters){
  if(!parameters){
    parameters = document.location.href.split('?')[1];
  }
  localStorage.setItem("calendar_parameters", JSON.stringify({"time": Date.toDay().getTime(), "parameters" : parameters}));
};
processGlobalParameters();
//SocialManager
var sendEmailCustomer = function(calEvent){
  if(confirm("Tem certeza que deseja enviar um e-mail para o cliente?")){
    var url = "/social/treatments/notify_customer/"+calEvent.id;
    $.ajax(url,{"type": "GET", "success" : function(response){
      // TESTAR no sendEmailUser também
      //
      eval("var t ="+response);
      if (t.message) {
	    alert("Erro ao enviar E-mail! " + t.message);
      } else {
      	alert("Enviado com sucesso!");
      }
    }, "error" : function(response){
      eval("var t ="+response);
      alert("Erro ao enviar E-mail! " + t.message);
    }});
  } 
}

var sendEmailUser = function(calEvent){
  if(confirm("Tem certeza que deseja enviar um e-mail para o profissional?")){
    var url = "/social/treatments/notify_user/"+calEvent.id;
    $.ajax(url,{"type": "GET", "success" : function(){
      alert("Enviado com sucesso!");
    }, "error" : function(response){
      alert("Erro ao enviar E-mail!");
    }});
  } 
}

//CalendarUtil
var filterUsers = function(users, updateUsers){
  users = users.filter(function(u){ return u.group==global_currentGroup || !global_currentGroup;});
  var currentUser = gup("user");
  if(currentUser){
    return users.filter(function(u){return u.id==currentUser; });
  }else{
    return users;
  }
};
var prepareTreatmentFromServer = function(treatment){
  treatment.start = FactoryDate.byTime(treatment.start);
  treatment.end = FactoryDate.byTime(treatment.end);
  treatment.userId = global_usersIds.indexOf(treatment.userId);
  if(treatment.noConflits > 0){
    treatment.conflits = global_treatments_obj.filter(function(item){
      return item.treatmentConflit == treatment.id;
    });
    treatment.hasFlit = treatment.conflits.length > 0;
  }else{
    treatment.hasFlit = false;
  }
};
//Needs a parameter
var prepareClassTreatment = function(parentTreatment){
  if(CalendarManager.calendarFeatures.useTreatmentClass && !parentTreatment.isBetween){
    parentTreatment.treatmentsIn = global_treatments_obj.filter(function(treatment) {
      treatment.isBetween =
          parentTreatment.start.getTime() <= treatment.start.getTime()
        &&  parentTreatment.end.getTime() >= treatment.end.getTime()
        &&  treatment.userId === parentTreatment.userId
        &&  !treatment.parentTreatment
        &&  treatment.id !== parentTreatment.id;

      if (treatment.isBetween) {
        treatment.parentTreatment = parentTreatment;
      }
      return treatment.isBetween;
    });   
  }
};
var treatTreatmentsServer = function(treatments){
  global_treatments_obj = treatments;
  try{
    treatments.forEach(prepareTreatmentFromServer);
  }catch(e){
    console.error(e);
  }
  treatments.forEach(prepareClassTreatment);
  var tempTreatments = treatments.filter(function(treatment){
    return !treatment.parentTreatment;
  });
  tempTreatments.forEach(function(treatment){
      if(treatment.treatmentsIn && treatment.treatmentsIn.length !== 0){
        var customers = treatment.treatmentsIn.length+1;
        treatment.oldTitle = treatment.title;
        var serviceName = treatment.title.split("<br/>")[1];
        treatment.title = " * "+serviceName+" ("+customers+") "
      }
  });
  return tempTreatments.filter(function(item){ return !item.treatmentConflit;});
};
var treatUsersServer = function(users){
  var usersret = [];
  for (var i = users.length - 1; i >= 0; i--) {
    usersret[i] = users[i].name;
  }
  return usersret;
};
var treatUsersServerId = function(users){
  var usersret = [];
  for (var i = users.length - 1; i >= 0; i--) {
    usersret[i] = users[i].id;
  }
  return usersret;
};
var onCustomerSelect = function(){
  if($("#cutomer_id_treatment").val() != "" ){
    if(($("#treatment_id").val() =='' || $("#treatment_id").val() =='0')){
      $("#command_treatment").val("");
    }else{
      if(global_calEvent && global_calEvent.customerId != $('#cutomer_id_treatment').val() && $("#treatment_id").val("")){
        if(confirm("Deseja iniciar novo Atendimento?")){
          $("#command_treatment").val("");
          $("#treatment_id").val("");
        }
      }
    }
    if(!global_calEvent || $("#cutomer_id_treatment").val() != global_calEvent.customerId){
      TreatmentManger.saveTreatment();
    }
  }     
};
Customer.addonsListeners.push($.throttle(500, onCustomerSelect));
var refreshCalendarByAjax = function(time){
  if(global_end && global_start && global_start.getTime() <= time && time <= global_end.getTime() ){
    $('#calendar').weekCalendar("refresh");
  }
};
var Open = 0;
var Missed =1; // faltou
var Arrived = 2;
var Ready = 3;
var Paid = 4; // pago
//  deleted = 5;
var Confirmed =6;
var PreOpen = 7; // pre agendado
var ReSchedule = 8;
var Budget = 9;
var getStatus = function(status, hasFlit){
  if(hasFlit){
    return "<img width='16' src='/images/bell.png'/>";
  }     
  switch(status){
    case Open:
      return "<img width='16' src='./images/open.png'/>";
    case PreOpen:
      return "<img width='16' src='./images/preopen.png'/>";
    case Confirmed:
      return "<img width='16' src='./images/confirmed.png'/>";
    case Missed:
      return "<img width='16' src='./images/missed.png'/>";   
    case ReSchedule:
      return "<img width='16' src='./images/treatment_reschedule.png'/>";   
    case Arrived:
      return "<img width='16' src='./images/good.png'/>";
    case Ready: 
      return "<img width='16' src='./images/tick.png'/>";
    case Paid: 
      return "<img width='16' src='./images/money.png'/>";
    case Budget:
      return "<img width='16' src='./images/treatment_budget.png'/>";   
    default:
      return "<img width='16' src='/images/clock.png'/>";
  }
};
var getColor = function(status,status2,colorAux){    
  //if (!global_calendarShowLight) {
  if (true) {
    switch(status){
      case Open:
        if (colorAux != "") {
          // cor veio do serviço/procedimento hj trata como borda
          // definir o heard todo ou criar uma forma/figura antes do title
          return { "color" : "#68a1e5", 'headColor': colorAux, 'text':"#fff"};
        } else {
          return { "color" : "#68a1e5", 'headColor':"#2b72d0", 'text':"#fff"};
        }
      case PreOpen:
        return { "color" : "rgb(169, 239, 239)", 'headColor':"rgb(43, 178, 208)", 'text':"#0E0E0E"};
      case Confirmed:
        // antigo amarelo muito proximo do chegou
        // return { "color" : "rgb(216, 215, 72)", 'headColor':"#FFD748", 'text':"#0E0E0E"};
        return { "color" : "rgb(220, 128, 224)", 'headColor':"rgb(244, 72, 255)", 'text':"#0E0E0E"};
      case Missed:
        return { "color" : "#D2B48C", 'headColor':"#8B4513"};
      case ReSchedule:
        // era marrom claro
        //return { "color" : "rgb(228, 211, 187)", 'headColor':"#D2B48C"};
        // ficou laranja
        return { "color" : "#eca84c", 'headColor':"#f79306"};
      case Arrived:
        return { "color" : "rgb(225, 239, 13)", 'headColor':"rgb(224, 217, 2)", 'text':"#0E0E0E"}; // rgb(255, 247, 0)
      case Ready: 
        return { "color" : "rgb(226, 36, 36)", 'headColor':"#FF0000"};
      case Paid: 
        switch(status2){
          case Ready:
            // o vermelho - vem com texto verde claro de pago 
            return { "color" : "rgb(226, 36, 36)", 'headColor':"#FF0000" , 'text':"rgb(102, 205, 0)"};
          case Missed:
            // o marron - vem com texto verde escuro de pago
            return { "color" : "#D2B48C", 'headColor':"#8B4513", 'text':"rgb(0, 128, 0)"};
          default:
            return { "color" : "rgb(102, 205, 0)", 'headColor':"rgb(0, 128, 0)"};
        }
      default:
        return  { "color" : "#68a1e5", 'headColor':"#2b72d0"};;
    }
  } else {
    switch(status){
      case Open:
        //return { "color" : "#95b8e6", 'headColor':"#95b8e6", 'text':"#fff"};
        return { "color" : "#95b8e6", 'headColor':"#2b72d0", 'text':"#fff"};
      case PreOpen:
        return { "color" : "#a9efef", 'headColor':"#a9efef", 'text':"#0E0E0E"};
      case Confirmed:
        return { "color" : "#dfb2e2", 'headColor':"#dfb2e2", 'text':"#0E0E0E"};
      case Missed:
        return { "color" : "#d08b5a", 'headColor':"#d08b5a"};
      case ReSchedule:
        return { "color" : "#D2B48C", 'headColor':"#D2B48C"};
      case Arrived:
        return { "color" : "#e9f35e", 'headColor':"#e9f35e", 'text':"#0E0E0E"}; // rgb(255, 247, 0)
      case Ready: 
        return { "color" : "#ef5858", 'headColor':"#ef5858"};
      case Paid: 
        switch(status2){
          case Ready:
            return { "color" : "#ef5858", 'headColor':"#ef5858"};
          case Missed:
            return { "color" : "#d08b5a", 'headColor':"#d08b5a"};
          default:
            return { "color" : "#4abf4a", 'headColor':"#4abf4a"};
        }
      default:
        return  { "color" : "#68a1e5", 'headColor':"#2b72d0"};;
    }    
  }
};    
var lastCallTreatmentsByApi = 0;
var refreshCalendar = function(treatments){
  global_treatments = treatTreatmentsServer(treatments);
  $('#calendar').weekCalendar("refresh");
};
var saveMovement = function(calEvent){
  $("#treatment_id").val(calEvent.id);
  var tmpUser = global_users_obj[calEvent.userId];
  TreatmentManger.updateTreatmentData(calEvent.id,tmpUser.id,calEvent.start,calEvent.end,calEvent.status);
};
var resizeCalendar = function(){
  var size = Math.max(1200,150*(global_users.length*global_days));
  $("#calendar").css("width",size);
  createHours();
};
var translateStatus = function(){
  $(".treatment-status").each(function(i,t){
    $(t).html(getStatus($(t).html()));
  });
};
var createHours = function(){
  var MINUTE_IN_MILES = 60*1000;
  var date = Date.toDay();
  var dayToday = Date.toDay().getDate();
  date.setHours(global_start_hour);
  date.setMinutes(0);
  var increment = global_interval * MINUTE_IN_MILES;
  var horarios = "<span class='horus'>"
  while(date.getHours() <= global_end_hour && date.getDate() === dayToday ){
    horarios+= "<div class='horus_slot'>"+getHourBr(date)+"</div>";
    date.setTime(date.getTime()+increment);
  }
  horarios += "</span>";
  $(".wc-column-odd, .wc-column-even").html(horarios);
};
var buildDataField = function(start, end){
  updateParameter();
  if(!stopProcess){
    stopProcess = true;
    if(($("#data_calendar").length === 0) && ($("#data_calendar_end").length === 0)){
      div = '<div style="float:left; padding-right:10px" class="input-prepend"><span class="add-on" style="height: 17px;"><img width="16" src="/images/calendar_addon.png"></span> <input type="text" size="19" name="data_calendar" id="data_calendar" class="input-small date" style="height: 17px;"></div>';
      div += '<div style="float:left; padding-right:10px" class="input-prepend"><span class="add-on" style="height: 17px;"><img width="16" src="/images/calendar_addon.png"></span> <input type="text" size="19" name="data_calendar_end" id="data_calendar_end" class="input-small date" style="height: 17px;"></div>';
      $(".wc-toolbar").prepend(div);
      $("#data_calendar").val(getDateBr(start)).datepicker({
                              beforeShow: function() {
                                  setTimeout(function(){
                                      $('#ui-datepicker-div').css('z-index',10000);
                                      
                                  }, 0);
                              }
                          });
      $("#data_calendar_end").val(getDateBr(end)).datepicker({
                              beforeShow: function() {
                                  setTimeout(function(){
                                      $('#ui-datepicker-div').css('z-index',10000);
                                      
                                  }, 0);
                              }
                          });
      $(".add-on").click(function(){
        $(this).next().focus();
      });//setfous in datepiker ao clicar no calendariozinho

      var changeDates = function(){
        date = $("#data_calendar").datepicker('getDate');
        date_end = $("#data_calendar_end").datepicker('getDate');
        if(!date){
          date = Date.toDay();
        }
        if(!date_end){
          date_end = date;
        }
        if(!date_end){
          date_end = Date.toDay();
        }
        var days = 1;
        if(date.getTime() < date_end.getTime()){
          days = window.parseInt(Math.max(1,((date_end.getTime()+86400000)-date.getTime() )/86400000));
          if (days < 1) {
            days = 1
          }
        }
        if((global_users.length*days) > max_colunms_to_show){
          var max_days = (max_colunms_to_show/global_users.length);
          days = window.parseInt(max_days);
          if (days < 1) {
            days = 1
          }
          alert("Seu intervalo foi alterado para "+days+" dias!");
        }
        global_days = days;
        $("#calendar").weekCalendar("setDaysToShowOnly",days);
        $("#calendar").weekCalendar("gotoDate",date);
        updateParameter();
      };
      $("#data_calendar").change(function() {
	        date = $("#data_calendar").datepicker('getDate');
	        date_end = $("#data_calendar_end").datepicker('getDate');
	        if(date.getTime() < date_end.getTime()){
	          $("#data_calendar_end").val ($("#data_calendar").val());
	        }
	        changeDates();
	  });
      $("#data_calendar_end").change(function() {
          changeDates();
      });
    }
    var days = window.parseInt(Math.max(1,Math.abs((start.getTime() - end.getTime()))/86400000));
    if (days < 1) {
      days = 1
    }
    global_days = days;
    resizeCalendar();
    stopProcess = false;
  }
};
var unitListFromServer = function(){
    $.get("/calendar/units",function(results){
        $("#units").html("");
        $("#units").append("<option value=''>Selecione Unidade</option>");
        eval("results = "+results);
        for(var i in results){
          obj = results[i];
          if((obj.isCurrent && global_load) && !global_notUnit  ) {
            $("#units").append("<option selected='true' value='"+obj.id+"'>"+obj.name+"</option>");
          }else{
            $("#units").append("<option value='"+obj.id+"'>"+obj.name+"</option>");
          }
        }
        if(results.length == 1 && !global_load)
          $("#units").change();
      }
    );
};
var userListFromGlobal = function(users){
  var users_html = "<option value=''>Selecione Profissional</option>";
  users = [].concat(users).sort(function(a,b) {
          if (a.name < b.name)
             return -1;
          if (a.name > b.name)
            return 1;
          return 0;
        })
  for(var i in users){
    obj = users[i];
    users_html += "<option value='"+obj.id+"'>"+obj.name+"</option>";
  }
  $("#user").html(users_html);
  $("#user").val(gup("user"));
};
var groupsListFromServer = function(){
    DataManager.getUserGroupsForCalendar(function(results){
      groups = "<option value=''>Selecione Grupo de Profissionais</option>";
      for(var i in results){
        obj = results[i];
        if(global_currentGroup == obj.id)
          groups += "<option selected='true' value='"+obj.id+"'>"+obj.name+"</option>";
        else
          groups += "<option value='"+obj.id+"'>"+obj.name+"</option>";
      }
      $("#groups").html(groups);
    });
};
var setTreatmentGlobalFromId  = function(id){
  global_calEvent = global_treatments_obj.filter(function(t){
    return t.id === id;
  })[0];
};


