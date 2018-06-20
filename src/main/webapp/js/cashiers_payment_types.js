var requestCashiersPaymentTypes = function(params, grid_selector, total_selector, needDrawChart) {
    var fields = [];
    fields[1] = "real";
    var url = "/report/cashiers_payment_types";
    var total = 0.00;
    renderReport(url, fields, Cachier.prepareCashierParams(params), grid_selector, function(data) {
      data = data.map(function(a){
        var lin = [];
        lin [0] = a[0];
        lin [1] = a[1];
        return lin
      })
        if (needDrawChart) {
            drawChart(jQuery.merge([
                ["Form Pag", "Valor"]
            ], data));
        }
        data.forEach(function(row) {
            total += parseFloat(row[1]);
        });
        var $totalElement = $(total_selector);
        if ($totalElement.is(':input')) {
            $totalElement.val(total.formatMoney());
        } else {
            $totalElement.html(total.formatMoney());
        }
    });
};