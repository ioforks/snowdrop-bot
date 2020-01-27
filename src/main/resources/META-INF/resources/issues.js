var startTime;
var endTime;

$(document).ready(function() {
  $("#report-enable").click(function() {
    $.get("/reporting/enable", function(data, status) {
      $("#report-enable").attr("disabled", true);
      $("#report-disable").attr("disabled", false);
    });
  });

  $("#report-disable").click(function() {

    $.get("/reporting/disable", function(data, status) {
      $("#report-enable").attr("disabled", false);
      $("#report-disable").attr("disabled", true);
    });
  });

  $("#report-issues-collect").click(function() {
    $.get("/reporting/collect/issues", function(data, status) {
    });
  });

  $.ajax({url: "/reporting/status"}).then(function(data) {
    var enabled = JSON.parse(data);
    if (enabled) {
      $("#report-enable").attr("disabled", true);
      $("#report-disable").attr("disabled", false);
    } else {
      $("#report-enable").attr("disabled", false);
      $("#report-disable").attr("disabled", true);
    }
  });

  $.ajax({url: "/reporting/start-time"}).then(function(data) {
    if (data) {
      startTime = new Date(data);
      startTime.setHours(12)
      startTime.setMinutes(00)
      var dateText =  formatDate(startTime) + " 12:00 AM";
      $('#start-date').datetimepicker({
        defaultDate: startTime,
        sideBySide: true,
      }).on('dp.change', function (e) {
        startTime=e.date.toDate()
        refreshDataTable()
      });
      refreshDataTable()
    }
  });

  $.ajax({url: "/reporting/end-time"}).then(function(data) {
    if (data) {
      endTime = new Date(data);
      endTime.setHours(12)
      endTime.setMinutes(00)
      var dateText =  formatDate(endTime) + "12:00 AM";
      $('#end-date').datetimepicker({
        defaultDate: endTime,
        sideBySide: true,
      }).on('dp.change', function (e) {
        endTime=e.date.toDate()
        refreshDataTable()
      });
      refreshDataTable()
    }
  });


  function refreshDataTable() {
    if (startTime == null || endTime == null) {
      return
    }
    $('#issues-table').DataTable( {
      destroy: true,
      processing: true,
      dom: 'Bfrtip',
      buttons: [
        'copy', 'csv', 'excel', 'pdf'
      ],
      columns: [
        {data: 'assignee', type: 'string' },
        {data: 'repository', type: 'string' },
        {data: 'url', type : 'num', render: dataTablesRenderNumber},
        {data: 'title', type: 'string' },
        {data: 'open', type: 'boolean' },
        {data: 'createdAt', type: 'string', render: dataTablesRenderDate},
        {data: 'updatedAt', type: 'string', render: dataTablesRenderDate},
        {data: 'closedAt', type: 'string', render: dataTablesRenderDate}
      ],
      order: [[3, "desc"]],
      ajax: "/reporting/data/issues?startTime=" + formatDate(startTime) + "&endTime=" + formatDate(endTime)
    });
  }
});
