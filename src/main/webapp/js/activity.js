(function() {
  $(function() {
    var index = $("#duration").val().indexOf(":");
    if (index == -1){
      return alert ("Formato de duração inválido " + $("#duration").val() + " use hh:mm")
    }
    var hour = 0;
    hour = $("#duration").val().substring(0,index)
    if (hour > 8) {
      alert ("Observe que a duração do serviço " + $("#duration").val() + " é superior a 8 horas")
    }
    var commissionabs = 0.0;
    commissionabs = parseFloat($("#commissionAbs").val())
    var price = 0.0;
    price = parseFloat($("#price").val())
    if (commissionabs >= price && price > 0.0) {
      alert ("Observe que a comissão absoluta é maior ou igual ao preço do serviço")
    }
  });

}).call(this);
