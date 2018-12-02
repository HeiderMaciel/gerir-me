var requestDreData = function(unit, costcenters, accounts, dttype, start, end, status_select_filter, ids) {
  var url = "/report/dre_tree";
  $.post(url, {
    unit: unit,
    costcenters: costcenters, 
    accounts: accounts,
    dttype: dttype,
    start: start,
    end: end,
    status_select_filter: status_select_filter,
    ids: ids
  }, function(t) {
    eval("var treeData = " + t);
    var tops = treeData.filter(function(account) {
      return account[4] == '0';
    });
    var parents = treeData.filter(function(account) {
      return account[5] == 'true';
    });
    parents.forEach(function(account) {
      account.childs = treeData.filter(function(accountChild) {
        return accountChild[4] == account[0];
      });
    });

    function printTree(tree) {
      return "<ul class='tree'>" + tree.map(function(item) {
        var value = parseFloat(item[6]);
        var ret = "<li>" + item[1] + " " + " R$ " + value.formatMoney();
        if (item.childs && item.childs.length > 0) {
          ret += printTree(item.childs);
        }
        ret += "</li>";
        return ret;
      }).join("") + "</ul>";
    }
    $("#grid_dre").html(printTree(tops));
  });
}