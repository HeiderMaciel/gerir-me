Date.toDay = function(){
	return new Date();
};
//Date Utilvi
Date.prototype.getYearGap = function(){
	// 05/05/2018 - rigel antes instanciava ret com this.gettime
	// sempre retornava 0
	var ret = new Date();
	return this.getFullYear() - ret.getFullYear();
};
Date.prototype.getStartOfMonth = function(){
	var ret = new Date(this.getTime());
	ret.setDate(1)
	return ret;
};
Date.prototype.getEndOfMonth = function(){
	var lastDayOfMonth = new Date(this.getFullYear(), this.getMonth()+1, 0);
	return lastDayOfMonth;
}

Date.prototype.getSixMonthAgo= function(){
	var ret = new Date(this.getTime());
	ret.setMonth(ret.getMonth()-6)
	return ret;
};
Date.prototype.getAYearAgo = function(){
	var ret = new Date(this.getTime());
	ret.setFullYear(ret.getFullYear()-1)
	return ret;
};
Date.prototype.getStartOfWeek= function(){
	var d = new Date(this.getTime());
	d.setDate(d.getDate()-d.getDay())
	return d;
};
Date.prototype.getNextWeek= function(){
	var d = new Date(this.getTime());
	d.setDate(d.getDate()+7)
	return d;
};1
Date.prototype.getDateBr = function(){
	return window.getDateBr(this);
};
Date.prototype.getHourBr = function(){
	return window.getHourBr(this);
};
Date.prototype.getHourBr = function(){
	return window.getHourBr(this);
};
Date.prototype.getNextMonth = function(){
	var ret = new Date(this.getTime());
	ret.setMonth(ret.getMonth()+1);
	return ret;
}
Date.prototype.getMonthBrName = function(){
	return i18n_months[this.getMonth()];
};

Date.prototype.getServerTime = function(){
	var promise =  $.get("/system/date");
	var startRequest = new Date().getTime();
	promise.success(function(value){

		var current = (new Date().getTime());
		diff = current - value;
		real_diff = diff - (current - startRequest);
		localStorage.setItem('diff_time', real_diff);
	});
};
var FactoryDate = {};
FactoryDate.byTime = function(time){
	if(time.replace){
		return new Date(time.replace(/-/g, '/'));
	}else{
		return new Date(time);
	}
	
};
/**
 * Fix problems about date objects just to brazilian Daylight Saving
 * set hors to 4 hours
 * @return void
 */
Date.prototype.ajustDaylightSavingPrevent = function(){
	this.setHours(4);
};
Date.prototype.ajustDaylightSavingHour = function(){ 
	var testDate = new Date(this.getTime());
	testDate.setMinutes(0);
	testDate.setSeconds(0);
	testDate.setMilliseconds(0);
	testDate.setHours(0);
	if(testDate.getDate() != this.getDate()){ 
		this.setHours(this.getHours()+1);
	}
};
Date.prototype.getDateUs = function(){
	var d = new Date(this.getTime());
	month = d.getMonth()+1;
	if(month <10 ){
		month = "0"+month;
	}
	var day = d.getDate();
	if(day <10){
		day = "0"+day;
	}
	var date_str = d.getFullYear()+"-"+month+"-"+day;
	return date_str;
};
Date.prototype.getTextWhen = function(){
	return i18n_days[this.getDay()]+" "+this.getDate()+" de "+i18n_months[this.getMonth()]+" de "+this.getFullYear();
};

Date.prototype.getTextWhenShort = function(){
	return i18n_days[this.getDay()].substr(0,3)+" "+this.getDate()+" de "+i18n_months[this.getMonth()]+" de "+this.getFullYear();
};

Date.prototype.isItChristmas = function(){
	return ((this.getDate() >= 1 && this.getMonth() == 11) || (this.getDate() <= 5 && this.getMonth() == 0));
};

Date.prototype.isItCustomerDayTomorrow = function(){
	return (this.getDate() == 14 && this.getMonth() == 8);
};
Date.prototype.isItCustomerDay = function(){
	return (date.getDate() == 15 && date.getMonth() == 8);
};
Date.prototype.isBetween = function(start, end){
	return this.getTime() > start.getTime()  && end.getTime() > this.getTime();
};
var sumHours = function(a, b) {
	var ajustHour = function(hour) {
		if (hour < 10) {
			hour = "0" + hour;
		}
		return hour;
	};
	var intValueA = a.split(':').map(function(i) {
		return parseInt(i);
	});
	var intValueB = b.split(':').map(function(i) {
		return parseInt(i);
	});
	var horas = intValueB[0] + intValueA[0];
	var minutos = intValueB[1] + intValueA[1];
	horas += parseInt(minutos / 60);
	minutos = minutos % 60;
	return ajustHour(horas) + ":" + ajustHour(minutos);
};
var getHourBr = function(d) {
	hour = d.getHours();
	if (hour < 10) {
		hour = "0" + hour;
	}
	minutes = d.getMinutes();
	if (minutes < 10) {
		minutes = "0" + minutes;
	}
	var date_str = hour + ":" + minutes;
	return date_str;
}
var getDateBr = function(d) {
	month = d.getMonth() + 1;
	if (month < 10) {
		month = "0" + month;
	}
	day = d.getDate();
	if (day < 10) {
		day = "0" + day;
	}
	var date_str = day + "/" + month + "/" + d.getFullYear()
	return date_str;
}

var getDateBrAsUri = function(d) {
	return encodeURIComponent(getDateBr(d));
}

// Rigel 28/08/2017 recebe data no formato STR brasileiro dd/mm/yyyy e 
// retorna o data mesmo interna americano yyyy-mm-dd
var brToUsDate = function (brStrdate) {
	var arrDate = brStrdate.split('/');
    var newDate = new Date(arrDate[2], arrDate[1] -1, arrDate[0]);
	return newDate;
}

var date = new Date();
$(function() {
	if (Date.toDay().isItChristmas()) {
		$('#natal').show();
	}
	if (Date.toDay().isItCustomerDay()) {
		$('#customerdaytoday').show();
	}else if (Date.toDay().isItCustomerDayTomorrow()) {
		$('#customerdaytomorrow').show();
	}
});