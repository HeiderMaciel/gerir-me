var TreatmentStatusManager = {
	markAsArrived : function(){
		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'arrived');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsConfirmed : function(){
		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'confirmed');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsPreOpen : function(){
			if (AuthUtil.user.id == 3) {
alert (global_calEvent.start)
}
//		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'preopen');
		TreatmentManger.updateTreatmentStatus(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.end, 'preopen');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsOpen : function(){
		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'open');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsMissed : function(){
		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'missed');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsReSchedule : function(){
		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'rescheduled');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	},
	markAsReady : function(){
		TreatmentManger.updateTreatmentData(global_calEvent.id, global_usersIds[global_calEvent.userId],global_calEvent.start,global_calEvent.end, 'ready');
		$('#treatment_add').modal('hide');
		//$('#treatment_class').modal('hide');
	}
};