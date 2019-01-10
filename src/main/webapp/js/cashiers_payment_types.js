var requestCashiersPaymentTypes = function(params, grid_selector, total_selector, needDrawChart, totalfat_selector) {
    var fields = [];
    fields[1] = "real";
    fields[2] = "boolean";
    fields[3] = "boolean";
    var url = "/report/cashiers_payment_types";
    var total = 0.00;
    var totalfat = 0.00;
    renderReport(url, fields, Cachier.prepareCashierParams(params), grid_selector, function(data) {
      data = data.map(function(a){
        var lin = [];
        lin [0] = a[0];
        lin [1] = a[1];
        if (a[2] == "true" || a[3] == "true") {
            totalfat += parseFloat(a[1]);
        }
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
        var $totalfatElement = $(totalfat_selector);
        if ($totalfatElement.is(':input')) {
            $totalfatElement.val(totalfat.formatMoney());
        } else {
            $totalfatElement.html(totalfat.formatMoney());
        }
    });
};

