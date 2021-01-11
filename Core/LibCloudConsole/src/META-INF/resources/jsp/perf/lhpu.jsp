<%@page import="org.json.JSONArray"%>
<%@page import="com.cloud.core.profiler.LicenseMetrics"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	LicenseMetrics metrics 	= LicenseMetrics.getInstance();
	JSONArray rows 			= null;
	String statusMessage	= null;
	try {
		rows = metrics.toJSON();
	}
	catch (Exception e) {
		statusMessage = e.getMessage();
	}
%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; UTF-8">
<meta http-equiv="Cache-Control" content="NO-CACHE">

<title>Historical License Peak Usage</title>

<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">

<script type="text/javascript" src="../../js/jquery.js"></script>
<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>
<script type="text/javascript" src="../../js/log.js"></script>

<script type="text/javascript">

$(document).ready(function() {
	
	TABLE = $('#table1').DataTable( {
		stateSave: true,
		"language": {
			"lengthMenu": 'Display <select>'+
				'<option value="20">20</option>'+
				'<option value="100">100</option>'+
				'<option value="500">500</option>'+
				'<option value="-1">All</option>'+
				'</select>' 
			} 
	});
});

</script>

<style type="text/css">
body {
	font-family: Arial;
}
</style>

</head>

<body>

<% if ( statusMessage != null) { %>
	<p><font size="3" color="blue"><%=statusMessage%></font></p>
<% } %>

	<table id="table1" class="display" width="100%">
		<thead>
			<tr>
				<th>Date</th>
				<th>Peak Usage</th>
			</tr>
		</thead>
		<tfoot>
			<tr>
				<th>Date</th>
				<th>Peak Usage</th>
			</tr>
		</tfoot>
		<tbody>
		<% 
		if ( rows != null ) {
			for ( int i = 0 ; i < rows.length() ; i ++) { %>
			<tr>
				<td><%=rows.getJSONArray(i).get(0)%></td>
				<td><%=rows.getJSONArray(i).get(1)%></td>
			</tr>
		<% 
			} 
		} %>
		</tbody>
	</table>

</body>
</html>