<%@page import="com.cluster.jsp.JSPDocker"%>
<%@page import="com.cloud.docker.Docker"%>
<%@page import="com.cloud.docker.DockerNode"%>
<%@page import="java.io.IOException"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	final String contextPath 	= getServletContext().getContextPath();
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	//String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	final String action			= request.getParameter("action");
	
	try {
		statusMessage 	= JSPDocker.execute(contextPath, request);
		
		if ( statusMessage !=  null) {
			statusType = "INFO";
		}
	}
	catch ( IOException e) {
		statusMessage 	= e.getMessage();
		statusType		= "ERROR";
	}
	catch ( Exception ex) {
		statusMessage 	= HTTPServerTools.exceptionToString(ex);
		statusType		= "ERROR";
	}
	Map<String, DockerNode> clusters = Docker.getClusters();
	
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Docker" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="Docker <i class='fab fa-docker'></i>" name="pageTitle"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>

<!-- CONTENT -->
	<div class="row">
		<div class="col-md-12">
			<div class="panel panel-default" data-widget='{"draggable": "false"}'>
				<div class="panel-heading">
					<h2>Nodes</h2>
					
					<div class="panel-ctrls">
						<button id="btnAdd" class="btn btn-raised" data-toggle="modal" data-target="#modal1">Add</button>
					</div>
				</div>
				<div class="panel-body">
					<table id="tblNodes" class="table m-n">
						<thead>
							<tr>
								<th>Name</th>
								<th>Host</th>
								<th>Action</th>
							</tr>
						</thead>
						<tbody>
						<% for ( Map.Entry<String, DockerNode> entry : clusters.entrySet()) { 
							DockerNode node 		= entry.getValue();
							final String name 		= node.getName();	
							final String host		= node.getHost();
						%>
							<tr>
								<td><a title="Click to edit" href="nodes.jsp?name=<%=name%>"><%=name%></a></td>
								<td>
									<!--  <i class="material-icons">lock<%=(node.isTlsEnabled() ? "" : "_open") %></i> -->
									<% if(!node.isTlsEnabled()) { %>
										<span class="label label-danger">Insecure</span>&nbsp;
									<% } %>
									<%=node.getHostPort() %>
								</td>
								<td>
									<a href="#" onclick="return sysinfo('<%=name%>')">Ping</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									<a href="manage.jsp?name=<%=name%>">Manage</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									<a href="#" onclick="return deleteNode('<%=name%>')">Delete</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									<a href="#" onclick="return ssh('<%=host%>','','')">Shell</a>
								</td>
							</tr>
						<% }  %>
						</tbody>
					</table>				
				</div>
			</div>
		</div>
	</div>		

<%
	String name				= request.getParameter("name");
	final DockerNode node	= name != null ? Docker.get(name) : null;
	String host 			= "";
	String keyStorePwd		= "" ; 
	String pemCert			= "" ; 
	String pemKey			= "" ; 
	
	boolean tls				= true;
	
	// form data
	if ( node != null) {
		host 			= node.getHostPort();
		keyStorePwd		= node.getKeyStorePassword();
		tls				= node.isTlsEnabled();
		pemCert			= Docker.getCertPEM(name);
		pemKey			= Docker.getKeyPEM(name);
	}
	
	// default name
	if ( name == null)	name = "Node" + (clusters.size() + 1);
	
%>
	
	<!--  modal 1: Add node -->	
	<form method="post" action="nodes.jsp" class="form-horizontal">
		<input type="hidden" id="action" name="action" value="add">
		<div id="modal1" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Add Node</h3>
					</div>
					<div class="modal-body">
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Name</label>
							<div class="col-sm-10">
								<input class="form-control" id="name" name="name"
									required data-toggle="tooltip"
									pattern="[A-z0-9/]+"
									title="Node name (no spaces)." value="<%=name%>">
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Host/Port</label>
							<div class="col-sm-10">
								<input type="text" name="hostPort" class="form-control" required="required" title="Host IP Address:Port" placeholder="IP ADDRESS:2376" value="<%=host%>"/>
							</div>
						</div>
						<div class="form-group">
							<div class="col-sm-2">
								<label class="control-label">Client Certificate (PEM)</label>
								<a href="#" title="Click to toggle file selection" style="float: right;" onclick="$('#divCertSel').toggle();$('#divCert').toggleClass('col-sm-10').toggleClass('col-sm-8')">Advanced</a>
							</div>
							<div id="divCert" class="col-sm-10">
								<textarea id="pemCert" name="pemCert" rows="2" cols="80" class="form-control" title="Client Certificate - PEM encoded (required for TLS only)"><%=pemCert%></textarea>
							</div>
							<div id="divCertSel" class="col-sm-2" style="display: none;">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="fileCert" onchange="openFile(event, 'pemCert')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>
						</div>
						<div class="form-group">
							<div class="col-sm-2">
								<label class="control-label">Client Private Key (PEM)</label>
								<a href="#" title="Click to toggle file selection" style="float: right;" onclick="$('#divKeySel').toggle();$('#divKey').toggleClass('col-sm-10').toggleClass('col-sm-8')">Advanced</a>
							</div>
							<div id="divKey" class="col-sm-10">
								<textarea id="pemKey" name="pemKey" rows="2" cols="80" class="form-control" title="Provate Key - PEM encoded (required for TLS only)"><%=pemKey%></textarea>
							</div>
							<div id="divKeySel" class="col-sm-2" style="display: none;">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="fileKey" onchange="openFile(event, 'pemKey')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">KeyStore Password</label>
							<div class="col-sm-10">
								<input class="form-control" id="ksPwd" name="ksPwd" value="<%=keyStorePwd%>"
									title="Password used to protect the identity keystore (for TLS).">
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Options</label>
							<div class="col-sm-4">
							
								<div class="checkbox">
									<label title="Check to enable mutual authentication via SSL.">
										<input type="checkbox" id="chkTLS" name="chkTLS" <%=(tls ? "checked" : "")%> >
										<i class="input-helper"></i> TLS Enabled
									</label>
								</div>
							</div>
						</div>
						
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
							<input type="submit" value="Save" class="btn btn-raised btn-primary">
					</div>
					
				</div>
			</div>
		</div>
	</form>
		
<!-- END CONTENT -->

	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>
	<script type="text/javascript" src="../../js/poll.js"></script>
	
	<script>
	<% if ( action == null && request.getParameter("name") != null ) { %>
	$('#btnAdd').click();
	<% } %>
	
	// fires when the SSH link is clicked
	function ssh (host, user, identity) {
		LOGD('SSH start host:' + host + ' user:' + user + '/' + identity);
		var href = '../ssh/ssh.jsp?host=' + host + '&user=' + user + "&identity=" + identity;
		window.open(href);
	}

	// fires a file is selected from an INPUT type=file
	var openFile = function(event, destination) {
	    var input 	= event.target;
	    var reader 	= new FileReader();
	    LOGD('openFile d:' + destination + " input:" + input + ' files[0]: ' + input.files[0]);
	    reader.onload = function() {
	    	var data 	= reader.result;
	    	$('#' + destination).html(data);
	    };
	    if ( input.files[0]) {
	    	reader.readAsText(input.files[0]);
	    }
	};
	
	function deleteNode (name) {
		var r = confirm('Delete ' + name + '?');
		if ( r == true ) {
			location = "nodes.jsp?action=delete&name=" + name;
			return true;
		}
		return false;
	}
	
	function sysinfo (node) {
		var url = '<%=contextPath%>/Docker?op=SysInfo&node=' + node;
		notify ('<strong>PLEASE WAIT</strong>', 'info');
		
		$.get( url ,  function( json ) {
			//{"message":"connect timed out","status":500}
			LOGD('SysInfo:'  + JSON.stringify(json));
			
			if ( json.status >= 400) {
				notify ('PING FAILED: ' + json.message, 'danger');
				return;
			}
			// OK: <i class="fab fa-docker"></i> - http://localhost:9080/CloudClusterManager/Docker?op=SysInfo&node=Node1
			// {"message":"OK","status":200,"data":{"BridgeNfIp6tables":true,"DockerRootDir":"/mnt/sda1/var/lib/docker", ...
			var html = json.data.Name + ' (' + json.data.OSType + ') '  + json.data.OperatingSystem 
			$.growl({ message : html }, {type : 'info', placement : {from : 'top', align : 'center'}, delay : 20000, offset : {x : 20, y : 85} } );
				
		}, 'json');	
		return false;
	}
	
	// Initialization
	$(document).ready(function() {
		$('#tblNodes').DataTable( {stateSave: true, paging: false, searching: false});		
	});
	
	</script>

</body>
</html>