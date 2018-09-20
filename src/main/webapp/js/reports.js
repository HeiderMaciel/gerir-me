// rigel

var auditDecode = function (value) {
  var ret = "";
  return "<a title='" + value + "' href='#' ><img width='24px' src='/images/audit.png'/></a>";
}



var trStatus = 0;
var trStatus2 = 0;
var trStatusdecode = function(name, row, showhint){
// duplicado do treatments_conference.js
  var status = row[trStatus]
  var statstr = '' 
  var title = ''
  if (status == '0') {
    statstr = 'open'
    title ='agendado'
  } else if (status == '1') {
    statstr = 'missed'
    title ='faltou'
  } else if (status == '2') {
    statstr = 'Arrived'
    title ='chegou'
  } else if (status == '3') {
    statstr = 'Ready'
    title ='atendido'
  } else if (status == '4') {
    if (row[trStatus2] == '3') {
      statstr = 'ready_paid'
      title ='atendido / pago'
    } else {
      statstr = 'paid'
      title ='pago'
    }
  } else if (status == '5') {
    statstr = 'Deleted'
    title ='excluído'
  } else if (status == '6') {
    statstr = 'Confirmed'
    title ='confirmado'
  } else if (status == '7') {
    statstr = 'PreOpen'
    title ='pré agendado'
  } else if (status == '8') {
    statstr = 'ReSchedule'
    title ='desmarcou'
  } else if (status == '9') {
    statstr = 'Budget'
    title ='orçamento'
  } else if (status == '10') {
    statstr = 'ReScheduled'
    title ='remarcou'
  } else if (status == '11') {
    statstr = 'OnGoing'
    title ='em atendimento'
  } else if (status == '12') {
    statstr = 'Canceled'
    title ='desmarcado empresa'
  } else if (status == '13') {
    statstr = 'ReOpen'
    title ='remarcado empresa'
  }
  if (showhint) {
    return "<p>" + title + "</p>"
  } else {
    return "<img title='" + title + "' src='/images/treatment_"+statstr.toLowerCase()+"1.png' width='24'/>"
  }
}

var ageDecode = function (value) {
  var res = value;
  if (res.indexOf("0 years") == 0) {
    var res = res.replace("0 years", "");
  }
  var res = res.replace(" 0 mons", "");
  var res = res.replace(" 0 days", "");
  var res = res.replace(" 0 hours", "");
  var res = res.replace(" 0 mins", "");
  var res = res.replace(" 0.00 secs", "");
  var res = res.replace("00:00:00", "hoje");
  var res = res.replace("year", "ano");
  var res = res.replace("mons", "meses");
  var res = res.replace("mon", "mês");
  var res = res.replace("day", "dia");
  if (res == "") {
    res = "hoje"
  }
  return (res)
}

var dateDecodeDDMM = function (value) {
  return !value || value == "" ? "" : (getDateBr(FactoryDate.byTime(value))).substr(0,5);
}

var dateDecode = function (value) {
  return !value || value == "" ? "" : getDateBr(FactoryDate.byTime(value));
}

// fazer aqui um substr para trazer só HH:mm
// ninguém usa com data tem a soma dos 2 decodes
var timeDecode = function (value) {
  return !value ? "" : getHourBr(FactoryDate.byTime(value));
}

var realDecode = function (value) {
  var ret = (parseFloat(value).formatMoney());
  return ret;
}

var realNullDecode = function (value) {
  var ret = "";
  //alert ("vaiiii   ="+value )
  if (!value || value == "" || value == "0.0" || value == 0.0) {
    ret = ""
  } else {
    ret = (parseFloat(value).formatMoney());
  }
  return ret;
}

/**
 * [renderReport description]
 * @param  {[type]}  url
 * @param  {[type]}  fields
 * @param  {[type]}  filter
 * @param  {[type]}  grid_selector
 * @param  {[type]}  callback_report
 * @param  {Boolean} isCrosstab
 * @param  {[type]}  crossFieldType
 * @param  {[type]}  crossFieldLabel
 * @param  {[type]}  crossDrowChart
 * @param  {[type]}  configGroup
 * @param  {[type]}  decodeCrossTabValue
 * @return {[type]}
 */
function renderReport(url, fields, filter, grid_selector, callback_report, isCrosstab, crossFieldType, crossFieldLabel, crossDrowChart, groupConfig,
  decodeCrossTabValue) {

  var objectToArray = function (row) {
    var row_a = [];
    var i;
    for (i in row) {
      row_a.push(row[i]);
    }
    return row_a;
  };
  if (!grid_selector) {
    grid_selector = "#grid";
  }

  var textNullDecode = function (value) {
    if (!value || value == 'null') {
      ret = ""
    } else {
      ret = value || "";
    } 
    return ret;
  };

  var textDecode = function (value) {

    return value || "";
  };
  var integerDecode = function (value) {
    return window.parseInt(value);
  };

  var integerNullDecode = function (value) {
    var ret = "";
    if (!value || value == "" || value == "0") {
      ret = ""
    } else {
      ret = (parseInt(value));
    }
    return ret;
  }

  var booleanDecode = function (value) {
    var _ = function () {
      return (value == "true" || value == "1") ? "good.png" : "bad.png"
    }
    return "<img src='/images/" + _() + "'/>";
  }
  var inOutDecode = function (value) {
    var _ = function () {
      return value == "0" ? "add.png" : "remove.png"
    }
    return "<img src='/images/" + _() + "'/>";
  }
  var approvedDecode = function (value) {
    var _ = function () {
      switch (value) {
      case "A":
        return "good.png"
      case "R":
        return "bad.png"
      case "N":
        return "bullet_star.png"
      }
    }
    return "<img src='/images/" + _() + "'/>";
  }
  var decode = function (type, value, row) {
    switch (type) {
    case "date":
      return dateDecode(value);
    case "dateDDMM":
      return dateDecodeDDMM(value);
    case "age":
      return ageDecode(value);
    case "dateTime":
      return dateDecode(value) + " " + timeDecode(value);
    case "time": // niguém usa deveria ser só time
      return dateDecode(value) + " " + timeDecode(value);
    case "boolean":
      return booleanDecode(value);
    case "inOut":
      return inOutDecode(value);
    case "int":
      return integerDecode(value);
    case "intNull":
      return integerNullDecode(value);
    case "real":
      return realDecode(value);
    case "realNull":
      return realNullDecode(value);
    case "textNull":
      return textNullDecode(value);
    case "approved":
      return approvedDecode(value);
    case "audit":
      return auditDecode(value);
    default:
      if (type && (type instanceof Object)) {
        if (type.type == "select") {
          return $(type.selector + " option[value=" + value + "]").text();
        } else if (type.type == "format") {
          return type.decode(value, row);
        }
      } else {
        return textDecode(value);
      }
    }
  }

  $.post(url, filter, function (resp) {
    eval("var dataset =" + resp);
    if (dataset.indexOf ("Erro no relatório") == 0) {
      // convencionei este erro no relatório na primeira posição nas exceptions
      alert (dataset);
      return;
    }
    var table = ""
    var getTrs = function (dataset, dataClass, cssClass) {
      if (!cssClass) {
        cssClass = "";
      }
      if (!dataClass) {
        dataClass = "";
      }
      var table = "";
      for (i in dataset) {
        var row = dataset[i];
        if (row instanceof Object && !(row instanceof Array)) {
          row = objectToArray(row);
        }
        table += "<tr class='" + cssClass + "' data-group='" + hashCode(dataClass) + "'>";
        table += row.map(function (value, position) {
          var typeField = (crossFieldType && position != 0) ? crossFieldType : fields[position];
          if (typeField !== "none") {
            return "<td>" + decode(typeField, value, row) + "</td>"
          } else {
            return "";
          }
        }).join("");
        table += "</tr>";
      }
      return table;
    }
    if (isCrosstab) {
      dataset = CrossTabDataSourceFactory.factory(dataset, grid_selector, crossFieldLabel, crossDrowChart, decodeCrossTabValue);
    }
    if (groupConfig) {
      var groupDataset = function (groupConfig, dataset, n, dataClass) {
        if (!dataClass)
          dataClass = "";
        var table = "";
        var groups = {};
        var key = groupConfig.key;
        var name = groupConfig.name;
        var groupFunction = groupConfig.groupFunction;
        var cssClass = groupConfig.show ? "" : "hide";
        var formater = groupConfig.formater;
        var collapsed = groupConfig.collapsed;
        var getGroup = function (key, current) {
          if (!groups[current[key]]) {
            groups[current[key]] = {
              "value": 0,
              "data": [],
              "name": current[name]
            };
          }
          return groups[current[key]];
        };
        for (i in dataset) {
          var current = dataset[i];
          var group = getGroup(key, current);
          group.value = groupFunction(current, group.value);
          current[name] = "";
          group.data.push(current);
        }

        for (var i in groups) {
          var group = groups[i];
          var hashKey = dataClass + group.name;
          var dataName = hashCode(hashKey);
          var dataGroup = hashCode(dataClass);
          if (dataGroup == dataName) {
            dataGroup += "parrent";
          }
          table += "<tr class='" + cssClass + " group' data-name='" + dataName + "' data-group='" + dataGroup + "'>";
          for (var i = 0; i < n; i++) {
            table += "<td></td>";
          };
          table += "<td colspan='" + group.data[0].length + "'><a>" + group.name + "( " + formater(group.value) + " )</a></td>"
          table += "</tr>";
          if (groupConfig.childGroup) {
            table += groupDataset(groupConfig.childGroup, group.data, n + 1, hashKey);
          } else {
            if (collapsed) { 
              table += getTrs(group.data, hashKey, "hide");
            } else { // rigel 23/11/2017 - trazer expandido
              table += getTrs(group.data, hashKey);
            }
          }
        };
        return table;
      }
      table = groupDataset(groupConfig, dataset, 0);
    } else {
      table = getTrs(dataset);
    }
    var excelButtonId = 'excel_' + grid_selector.replace('#', '');
    var excelButton = "<a id='" + excelButtonId + "' onclick=\"return ExcellentExport.excel(this, '" + grid_selector.replace('#', '') +
      "', 'data');\" href='#' download=\"data.xls\" title=\"Exportar para excel\" class=\"btn excel_buttton\"><img style=\"width: 16px;\" src=\"/images/excel.png\"></a>";
    $('#' + excelButtonId).remove();
    $(grid_selector).before(excelButton);

    var csvButtonId = 'csv_' + grid_selector.replace('#', '');
    var csvButton = "<a id='" + csvButtonId + "' onclick=\"return ExcellentExport.csv(this, '" + grid_selector.replace('#', '') +
      "', ';');\" href='#' download=\"data.csv\" title=\"Exportar para csv\" class=\"btn excel_buttton\"><img style=\"width: 16px;\" src=\"/images/generate.png\"></a>";
    $('#' + csvButtonId).remove();
    $(grid_selector).before(csvButton);

    var hideButtonId = 'hide_' + grid_selector.replace('#', '');
    var hideButton = "<a id='" + hideButtonId + "' onclick=\"$('" + grid_selector +
      "').toggle();return false;\" href='#' class=\"btn excel_buttton\" title=\"Ocultar tabela\"><img style=\"width: 16px;\" title=\"Ocultar tabela\" src=\"/images/collapse.png\"></a>";
    $('#' + hideButtonId).remove();
    $(grid_selector).before(hideButton);

    $(grid_selector + " tbody").html(table);
    $('.group').click(function () {
      var $childs = $("[data-group=" + $(this).data("name") + "]");
      if ($(this).is(":visible") && !$($childs[0]).is(":visible"))
        $("[data-group=" + $(this).data("name") + "]").show();
      else
        $("[data-group=" + $(this).data("name") + "]").hide().click();
    });
    try {
      $(grid_selector).tablesorter();
    } catch (e) {} finally {
      $(grid_selector).trigger('update');
    }
    if (dataset.length == 0) {
      //alert("Nenhum dado retornado para consulta");
    }
    if (callback_report)
      callback_report(dataset);
  })
}
var hashCode = function (str) {
  var hash = 0,
    i, char;
  if (str.length == 0) return hash;
  for (i = 0, l = str.length; i < l; i++) {
    char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash |= 0; // Convert to 32bit integer
  }
  if (hash == 0) {
    hash = "empty";
  }
  return hash;
};
var ArrayUtil = function () {};
ArrayUtil.hasValue = function (array, value) {
  return array.filter(function (item) {
    return item == value
  }).length > 0;
};

var CrossTabDataSourceFactory = {};
CrossTabDataSourceFactory.factory = function (data, grid_selector, crossFieldLabel, drowChart, decodeValue) {


  var factoryDataSource = function (indexColumn, indexLine, decodeValue) {
    decodeValue = decodeValue || function (value) {
          return value;
    }; 
    var columns = [];
    var groups = [];
    data.forEach(function (item) {
      if (!ArrayUtil.hasValue(columns, item[indexColumn]))
        columns.push(item[indexColumn]);

    });

    data.forEach(function (item) {
      if (!ArrayUtil.hasValue(groups, item[indexLine]))
        groups.push(item[indexLine]);

    });
    var dataSet = groups.map(function (group) {
      var localData = data.filter(function (item) {
        return item[indexLine] == group;
      });
      var ret = [group];
      var values = columns.map(function (column) {
        return localData.filter(function (item) {
          return item[indexColumn] == column;
        }).map(function (item) {
          return decodeValue(item[2], item, column, group);
        })
      }).map(function (item) {
        return item[0];
      });
      return ret.concat(values);
    });
    return {
      dataSet: dataSet,
      columns: columns
    };
  };
  var datasetToReturn = factoryDataSource(0, 1, decodeValue);
  var columns_data = [crossFieldLabel].concat(datasetToReturn.columns);
  $(grid_selector + " thead").html(columns_data.map(function (column) {
    return "<th>" + column + "</th>"
  }).join(""));
  if (drowChart) {
    var dataSetToChart = factoryDataSource(1, 0);
    CrossTabDataSourceFactory.drowChart(dataSetToChart.dataSet, [crossFieldLabel].concat(dataSetToChart.columns));
  }
  return datasetToReturn.dataSet;
}
CrossTabDataSourceFactory.drowChart = function (data, columns) {
  var data_to_prepare = [columns].concat(data);
  data_to_prepare = data_to_prepare.map(function (item, j) {
    return item.map(function (item, i) {
      if (j == 0 || i == 0) {
        return item;
      } else {
        return isNaN(item) ? 0.00 : window.parseFloat(item);
      }
    });
  });
  var data = google.visualization.arrayToDataTable(data_to_prepare);

  var options = {
    title: $(".page-header").text()
  };

  var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
  chart.draw(data, options);
}
