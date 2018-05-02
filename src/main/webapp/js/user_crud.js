$(function() {
  var renderHistory = function() {
    var filter = {
      customer: gup("id"),
      startDate: $("#history_startDate").val(),
      endDate: $("#history_endDate").val()
    };
    var fields = [];
    //fields[0] = "date";
    fields[0] = "time";
    fields[1] = "none";
    fields[2] = "none";
    fields[6] = {
      type: "format",
      decode: function(status) {
        return "<img src='/images/treatment_" + status.toLowerCase() + ".png' width='24'/>";
      }
    };
    fields[7] = "real";
    fields[12] = "none";
    fields[14] = "none";
    renderReport("/treatments/getTreatmentsByFilter", fields, filter, "#grid_history");
  }
  if (gup("id")) {
    //To create events
    setTimeout(function(){    
      if($("#event_link").length > 0){
        $("#event_link").attr("href", $("#event_link").attr("href").replace("##", gup("id")));
      }
      if($("#stakeholder_link").length > 0){
        $("#stakeholder_link").attr("href", $("#stakeholder_link").attr("href").replace("##", gup("id")));
      }
      if($("#bprelationship_link").length > 0){
        $("#bprelationship_link").attr("href", $("#bprelationship_link").attr("href").replace("##", gup("id")));
      }
      if($("#bprelationship_link1").length > 0){
        $("#bprelationship_link1").attr("href", $("#bprelationship_link1").attr("href").replace("##", gup("id")));
      }
    },600);

    var renderQuiz = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[0] = "date"
/*
      fields[1] = {
        type: "select",
        selector: "#quiz_quiz"
      }
*/

      fields[4] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger quiz_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".quiz_remove").click(function() {
          if (confirm("Deseja excluir o questionário do cliente?")) {
            var id = $(this).data("id");
            $.ajax("/api/v2/quiz/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Questionário excluído com sucesso!");
                renderQuiz();
              }
            })
          }
        });
      }
      renderReport("/customer_report/quiz", fields, filter, "#grid_quiz", bindEvent);
    }
    var renderBankAccount = function() {
      var filter = {
        customer: gup("id")
      };
      var fields = [];
      fields[4] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger bankaccount_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".bankaccount_remove").click(function() {
          if (confirm("Deseja excluir a Conta Bancária do cliente?")) {
            var id = $(this).data("id");
            $.ajax("/customer_api/bankaccount/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Conta bancária excluída com sucesso!");
                renderBankAccount();
              }
            })
          }
        });
      }
      renderReport("/customer_report/bankaccount", fields, filter, "#grid_bankaccount", bindEvent);
    }

    var renderCompanyUnit = function() {
      var filter = {
        user: gup("id")
      };
      var fields = [];
      fields[1] = "boolean";
      fields[3] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger companyunit_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".companyunit_remove").click(function() {
          if (confirm("Deseja excluir a associação de unidade do profissional?")) {
            var id = $(this).data("id");
            $.ajax("/user_api/companyunit/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Associação de unidade excluída com sucesso!");
                renderCompanyUnit();
              }
            })
          }
        });
      }
      renderReport("/user_api/list/companyunit", fields, filter, "#grid_companyunit", bindEvent);
    }

    var renderUserGroup = function() {
      var filter = {
        user: gup("id")
      };
      var fields = [];
      fields[2] = {
        type: "format",
        decode: function(value) {
          return '<input data-id="' + value + '" type="button" class="btn danger usergroup_remove" value="Excluir">';
        }
      }
      var bindEvent = function() {
        $(".usergroup_remove").click(function() {
          if (confirm("Deseja excluir a associação de grupo do profissional?")) {
            var id = $(this).data("id");
            $.ajax("/user_api/usergroup/" + id, {
              "type": "DELETE",
              "success": function() {
                alert("Associação de grupo excluída com sucesso!");
                renderUserGroup();
              }
            })
          }
        });
      }
      renderReport("/user_api/list/usergroup", fields, filter, "#grid_usergroup", bindEvent);
    }

    $("#history_startDate").change(renderHistory);
    $("#history_startDate").val(getDateBr(new Date().getAYearAgo()));
    $("#history_endDate").change(renderHistory);
    $("#history_endDate").val(getDateBr(new Date()));
    renderQuiz();
    renderHistory();
    renderBankAccount();
    renderCompanyUnit();
    renderUserGroup();
    

    $("#quiz_add").click(function() {
      $.post("/api/v2/quiz", {
        quiz: $("#quiz_quiz").val(),
        obs: $("#quiz_obs").val(),
        customer: gup("id")
      }, function() {
        alert("Questionário adicionado com sucesso!");
        renderQuiz();
      });
    });

    $("#bankaccount_add").click(function() {
      $.post("/customer_api/bankaccount", {
        bank: $("#bankaccount_bank").val(),
        agency: $("#bankaccount_agency").val(),
        account: $("#bankaccount_account").val(),
        obs: $("#bankaccount_obs").val(),
        customer: gup("id")
      }, function() {
        alert("Conta bancária adicionada com sucesso!");
        renderBankAccount();
      });
    });    
    $("#companyunit_add").click(function() {
      var showincalendar = "false";
      if ($('#companyunit_showincalendar').is(":checked")) {
        showincalendar = "true";
      }
      $.post("/user_api/companyunit", {
        unit: $("#companyunit_unit").val(),
        obs: $("#companyunit_obs").val(),
        showincalendar : showincalendar,
        user: gup("id")
      }, function() {
        alert("Associação de unidade adicionada com sucesso!");
        renderCompanyUnit();
      });
    });    
    $("#usergroup_add").click(function() {
      $.post("/user_api/usergroup", {
        group: $("#usergroup_group").val(),
        obs: $("#usergroup_obs").val(),
        user: gup("id")
      }, function() {
        alert("Associação de grupo adicionada com sucesso!");
        renderUserGroup();
      });
    });    
  }
  $("#companyunit_unit").unitField();
  $("#usergroup_group").userGroupField();
  $("#bankaccount_bank").bankField();
  $("#quiz_quiz").quizField();
})
