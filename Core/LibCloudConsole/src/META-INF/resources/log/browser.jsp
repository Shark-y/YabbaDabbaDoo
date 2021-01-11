<%@page import="com.cloud.core.logging.Container"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.core.io.FileTool"%>
<%@page import="java.io.FileFilter"%>
<%@page import="java.io.File"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
	final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aaa");

	File[] listFiles(String path, final String[] extensions, final String[] nameFilters) {
		File dir = new File(path);
		return dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				boolean foundExt 	= false;
				boolean foundName	= nameFilters != null ? false : true;
				
				for ( String ext : extensions) {
					if ( pathname.getName().contains("." + ext) 
							// No AUDIT (shown audit trail) or stdout (invalid format) files
							&& !pathname.getName().startsWith("AUDIT")
							&& !pathname.getName().contains("stdout") ) 
					{ 
						foundExt = true;
						break;
					}
				}
				if ( nameFilters != null ) {
					for ( String name : nameFilters) {
						if ( pathname.getName().contains(name) ) { 
							foundName = foundExt = true;
							break;
						}
					}
				}
				return foundExt && foundName;
			}
		});
	}
%>

<%
	final String contextRoot 	= request.getServletContext().getContextPath();
	final String path 			= Container.getDefautContainerLogFolder(); // HTTPServerTools.TOMCAT_HOME_PATH + File.separator + "logs";
	final String names			= request.getParameter("names");	// comma sep list of name filters name1,name2,...
	
	// extension filters
	final String[] exts 		= new String[] {"log"};
	
	// name filters
	final String[] filters		= names != null ? names.split(",") : null;
			
	File[] files 				= listFiles(path, exts, filters);
%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Cache-Control" content="NO-CACHE">

<title><%=(names != null && names.contains("AUDIT") ? "Audit Trail" : "File Browser") %></title>

<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">

<script type="text/javascript" src="../js/jquery.js"></script>
<script type="text/javascript" src="../js/jquery.dataTables.js"></script>
<script type="text/javascript" src="../js/log.js"></script>

<script type="text/javascript">

$(document).ready(function() {
	
	TABLE = $('#table1').DataTable( {
		stateSave: true,
		"language": {
			"lengthMenu": 'Display <select>'+
				'<option value="10">10</option>'+
				'<option value="20">20</option>'+
				'<option value="30">30</option>'+
				'<option value="40">40</option>'+
				'<option value="50">50</option>'+
				'<option value="-1">All</option>'+
				'</select>' +
				'&nbsp;&nbsp;&nbsp;<a href="../LogServlet?op=ziplogs" title="Gather node logs as a ZIP archive.">Download</a>'
			} 
		
	});
	
});

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
</style>

</head>

<body>
	<%=path%>
	<br><br>
	<table id="table1" class="display" width="100%">
		<thead>
			<tr>
				<th>Name</th>
				<th>Date modified</th>
				<th>Size (KB)</th>
			</tr>
		</thead>
		<tfoot>
			<tr>
				<th>Name</th>
				<th>Date modified</th>
				<th>Size (KB)</th>
			</tr>
		</tfoot>
		<tbody>
		<% for ( File f : files) { %>
			<tr>
				<td><a target="_blank" href="<%=contextRoot%>/log/logview.jsp?op=view&file=<%=f.getName()%>"><%=f.getName()%></a></td>
				<td><%=sdf.format(f.lastModified()) %></td>
				<td><%=(f.length() > 0 ? f.length()/1024 + 1 : 0) %></td>
			</tr>
		<% } %>
		</tbody>
	</table>

</body>
</html>