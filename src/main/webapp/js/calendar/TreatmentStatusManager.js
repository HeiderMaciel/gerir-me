var TreatmentStatusManager = {
	markAsArrived : function(){
		// rigel 2018 novembro - antes era updateTreatmentData
		// criei o updatestatus pq no horario de verão qdo alterava 
		// a obs e logo depois o status a data vinha melada e deslocava 
		// uma hora pra frente
		// o updatestatus não seta a hora só o status
		//TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'arrived');
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'arrived');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsConfirmed : function(){
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'confirmed');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsPreOpen : function(){
		if (AuthUtil.user.id == 1) {
			alert (global_calEvent.start)
		}
//		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'preopen');
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'preopen');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsOpen : function(){
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'open');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsMissed : function(){
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'missed');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsReSchedule : function(){
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'rescheduled');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsReady : function(){
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, 
			global_usersIds[global_calEvent.userId],global_calEvent.end, 'ready');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	}
};

