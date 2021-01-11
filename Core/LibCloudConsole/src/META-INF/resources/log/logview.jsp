<%@page import="com.cloud.core.logging.Container"%>
<%@page import="com.cloud.console.HTMLConsoleLogUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	// optional log endpoint (for remote view)
	final String baseEndPoint 	= request.getParameter("ep");;
	final String contextRoot 	= request.getServletContext().getContextPath();
	final String queryString 	= request.getQueryString();
	
	// Log file to view (optional for AUDIT trail)
	final String file			= request.getParameter("file");
	
	// One of: audit
	final String operation		= request.getParameter("op");
	final boolean refresh		= queryString == null || ( request.getParameter("refresh") != null) ;
	final boolean isAudit		= ( operation != null && operation.equals("audit"))
									|| ( file != null  && file.startsWith("AUDIT"));
	
	final String globalLog		= Container.getGlobalContainerLog();
	final String stdErrLog		= Container.getContainerStdErrLog();
	final boolean serverSide	= request.getParameter("server") != null;	// 10/20/2019 
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Cache-Control" content="NO-CACHE">

<title><%=(file != null ? file : ( isAudit ? "Audit Trail": "Log Viewer"))%></title>

<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">

<script type="text/javascript" src="../js/jquery.js"></script>
<script type="text/javascript" src="../js/jquery.dataTables.js"></script>
<script type="text/javascript" src="../js/log.js"></script>

<script type="text/javascript">
<% if ( baseEndPoint == null) { %>
var END_POINT 	= '<%=contextRoot%>/LogServlet<%=(queryString != null ? "?" + queryString : "")%>';
<% } else { %>
var END_POINT 	= '<%=baseEndPoint%>';
<%} %>
var TABLE;
var INTERVAL	= -1;

function clearLog() {
	$.ajax({
		type : 'POST',
		url : END_POINT + '?op=clear&len=' + TABLE.data().length,
		// request response in json!
		headers : {
			"Accept" : "application/json; charset=utf-8"
		},
		cache : false
	});
	TABLE.clear().draw();
}

function toggle_refresh (cb ) {
	LOGD("Toggle refresh Interval: " + INTERVAL + " Checked:" + cb.checked);
	
	if ( cb.checked) {
		startPolling();
	}
	else {
		stopPolling();
	}
}

$(document).ready(function() {
	TABLE = $('#table1').DataTable( {
		stateSave: true, 
		"ajax": END_POINT ,
		"language": {
			"lengthMenu": 'Display <select>'+
				'<option value="10">10</option>'+
				'<option value="20">20</option>'+
				'<option value="50">50</option>'+
				'<option value="100">100</option>'+
				'<option value="500">500</option>'+
				'<option value="1000">1000</option>'+
				<% if (!serverSide) { %>
				'<option value="-1">All</option>'+
				<% } %>
				'</select>' 
				<% if (queryString == null) { %>
				+ '&nbsp;&nbsp;&nbsp;<a href="javascript:clearLog()">Clear</a>'
				+ '&nbsp;&nbsp;&nbsp;<a target="_blank" href="<%=contextRoot%>/log/log4j.jsp">Filters</a>'
				<% } %>
				
				+ '&nbsp;&nbsp;&nbsp; <input type="checkbox" onclick="toggle_refresh(this)" <%=(refresh ? "checked=\"true\"" : "")%> /> Auto Refresh'
				
				<% if (queryString == null) { %>
				+ '&nbsp;&nbsp;&nbsp;<a href="../LogServlet?op=ziplogs" title="Gather node logs as a ZIP archive.">Download</a>'	
				<% } %>
				
				<% if (operation == null && globalLog != null) { %>
				+ '&nbsp;&nbsp;&nbsp;<a href="logview.jsp?op=view&file=<%=globalLog%>" title="View the global container log">Container Log</a>'	
				<% } else if ( globalLog != null ) { %>
				+ '&nbsp;&nbsp;&nbsp;<a href="logview.jsp" title="Back to the node log">Node Log</a>'
				<% } %>
				
				<% if (operation == null && stdErrLog != null) { %>
				+ '&nbsp;&nbsp;&nbsp;<a href="logview.jsp?op=view&file=<%=stdErrLog%>" title="View the container standard error log">STDERR Log</a>'	
				<% } %>
				
			} ,
			
		"createdRow": function ( row, data, index ) {
				var LEVEL = data[2];
		        $('td', row).eq(2).addClass(LEVEL);		// LEVEL
		        $('td', row).eq(3).addClass(LEVEL);		// MESSAGE
		} ,
			
		// poor man's XML escape for the mesage col (3)
		"columnDefs": [ {
				"targets": 3,
				"render": function ( data, type, full, meta ) {
				    return type == 'display' && meta.col == 3 ? escapeXML(data) : data;
				}
		}] 		
			
		<% if (serverSide) { %>
		// DT server side processing - https://datatables.net/manual/server-side
		, "serverSide": true
		, "processing": true
		<% } %>
			
	} );
	
	<% if ( refresh /*queryString == null*/) { %>
	startPolling ();	
	<% } %>

	<% if (serverSide) { %>
	// hide pagination buttons
	$('.dataTables_paginate').hide();
	<% } %>
	
} );

function startPolling () {
	if ( INTERVAL > 0) {
		LOGD("Start: Already polling.");
		return;
	}
	INTERVAL = setInterval(function () {
		TABLE.ajax.reload();
	}, <%=(serverSide ? 12000 : 5000) %>); //5000);
	LOGD("Poll start Interval: " + INTERVAL);
}

function stopPolling () {
	clearInterval(INTERVAL);
	INTERVAL = -1;
	LOGD("Poll stop.");
}

function escapeXML(str) {
	str = str.replace(/&/g, '&amp;')
	    .replace(/</g, '\n&lt;')
	    .replace(/>/g, '&gt;')
	    .replace(/"/g, '&quot;')
	    .replace(/'/g, '&apos;');
	//if ( str.indexOf('?xml') != -1) str ='<pre>' + str + '<pre>';
	if ( str.indexOf('&lt;/') != -1) str ='<pre>' + str + '<pre>'; 
	return str;
}

</script>

<style type="text/css">
body {
	font-family: Arial;
}
.INFO {
	color: blue;
}
.DEBUG {
}
.WARN {
	background-color: yellow;
}
.ERROR {
	color: white;
	background-color: red;
}
.DANGER {
	color: white;
	background-color: red;
}
.SEVERE {
	color: white;
	background-color: red;
}

</style>

</head>

<body>

		<table id="table1" class="display compact" cellspacing="0" width="100%">
			<thead>
				<tr>
					<th>Date</th>
					<th><%=isAudit ? "Source" : "Logger" %></th>
					<th>Level</th>
					<th>Message</th>
				</tr>
			</thead>
			<tfoot>
				<tr>
					<th>Date</th>
					<th><%=isAudit ? "Source" : "Logger" %></th>
					<th>Level</th>
					<th>Message</th>
				</tr>
			</tfoot>
		</table>

</body>
</html>