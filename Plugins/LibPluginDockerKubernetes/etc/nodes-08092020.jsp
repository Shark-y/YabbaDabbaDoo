<%@page import="java.net.URL"%>
<%@page import="com.cluster.jsp.JSPK8S"%>
<%@page import="com.cloud.kubernetes.Kubernetes"%>
<%@page import="com.cloud.kubernetes.K8SNode"%>
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
		statusMessage 	= JSPK8S.execute(contextPath, request);
		
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
	Map<String, K8SNode> clusters = Kubernetes.getClusters();
	
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Kubernetes" name="title"/>
</jsp:include>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="Kubernetes <img width='48' src='../../img/k8s.png'>" name="pageTitle"/>
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
					<h2>Clusters</h2>
					
					<div class="panel-ctrls">
						<button id="btnAdd" class="btn btn-raised" data-toggle="modal" data-target="#modal1">Add</button>
					</div>
				</div>
				<div class="panel-body">
					<table class="table m-n">
						<thead>
							<tr>
								<th>Name</th>
								<th>ApiServer</th>
								<th>Action</th>
							</tr>
						</thead>
						<tbody>
						<% for ( Map.Entry<String, K8SNode> entry : clusters.entrySet()) { 
							K8SNode node 		= entry.getValue();
							final String name 	= node.getName();
							final String host	= (new URL(node.getApiServer())).getHost();
						%>
							<tr>
								<td><a title="Click to edit" href="nodes.jsp?name=<%=name%>"><%=name%></a></td>
								<td><%=node.getApiServer() %></td>
								<td>
									<!-- 
									<a href="#" onclick="return ssh('<%=host%>','<%=node.getSSHUser()%>','<%=node.getSSHPwd()%>')">Shell</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									-->
									<a href="#" onclick="return sysinfo('<%=name%>')">Ping</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									<a href="manage.jsp?name=<%=name%>">Manage</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									<a href="#" onclick="return deleteNode('<%=name%>')">Delete</a>
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
	final K8SNode node		= name != null ? Kubernetes.get(name) : null;
	String host 			= "";
	String accessToken		= "" ; 
	String sshUser			= "" ; 
	String sshPwd			= "" ; 
	
	// form data
	if ( node != null) {
		host 			= node.getApiServer();
		accessToken		= node.getAcessToken();
		sshUser			= node.getSSHUser();
		sshPwd			= node.getSSHPwd();
	}
	
	// default name
	if ( name == null)	name = "KubeCluster" + (clusters.size() + 1);
	
%>
	
	<!--  modal 1: Add nodes -->	
	<form id="frm1" method="post" action="nodes.jsp" class="form-horizontal" onsubmit="return frm_submit()">
		<input type="hidden" id="action" name="action" value="add">
		<div id="modal1" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Add Cluster</h3>
					</div>
					<div class="modal-body">
						<span id="modal1StatusMsg"></span>
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
							<label class="col-sm-2 control-label">API Server</label>
							<div class="col-sm-10">
								<input type="text" name="apiServer" id="apiServer" class="form-control" required="required" title="Host IP Address:Port" placeholder="https://IP-ADDRESS:6443" value="<%=host%>"/>
							</div>
						</div>
						<div class="form-group">
							<div class="col-sm-2">
								<label class="control-label"><a href="#" title="Click to fetch token. SSH information is required." onclick="return fetch_token()">Access Token</a></label>
								<!-- 
								<a href="#" title="Click to toggle file selection" style="float: right;" onclick="$('#divCertSel').toggle();$('#divCert').toggleClass('col-sm-10').toggleClass('col-sm-8')">Advanced</a>
								-->
							</div>
							<div id="divCert" class="col-sm-7">
								<textarea id="accessToken" name="accessToken" rows="2" cols="80" class="form-control" title="Access Token" required="required"><%=accessToken%></textarea>
							</div>
							<div class="col-sm-3">
								<input type="text" name="acctNS" id="acctNS" class="form-control" required="required" value="default:default" title="Service Acoount:Namespace" />
							</div>
							<!-- 
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
							-->
						</div>
						<!-- 
						<div class="form-group">
							<div class="col-sm-2">
								<label class="control-label">Client Private Key (PEM)</label>
								<a href="#" title="Click to toggle file selection" style="float: right;" onclick="$('#divKeySel').toggle();$('#divKey').toggleClass('col-sm-10').toggleClass('col-sm-8')">Advanced</a>
							</div>
							<div id="divKey" class="col-sm-10">
								<textarea id="pemKey" name="pemKey" rows="2" cols="80" class="form-control" title="Provate Key - PEM encoded (required for TLS only)"></textarea>
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
						-->
						<div class="form-group">
							<label class="col-sm-2 control-label">SSH User</label>
							<div class="col-sm-10">
								<input class="form-control" id="sshUser" name="sshUser" value="<%=sshUser%>"
									title="Optional SSH User for system administration.">
							</div>
						</div>
						<!-- SSH pwd or key -->
						<div class="form-group">
							
							<label class="control-label col-sm-2"><a href="#" title="Click to toggle an SSH Private Key file selection" style="float: right;" onclick="$('.sshKey').toggle();$('#divKey').toggleClass('col-sm-10').toggleClass('col-sm-8')">SSH Pwd</a></label>
							
							<div id="divKey" class="col-sm-10">
								<input type="password" class="form-control sshKey" id="sshPwd" name="sshPwd" value="<%=sshPwd%>"
									title="Optional Password used for system administration (Tip: Click advanced for prirvate key selection).">
									
								<textarea id="pemKey" name="pemKey" rows="2" cols="80" class="form-control sshKey" style="display: none;"
									title="Provate Key - PEM encoded (required for TLS only)"></textarea>
							</div>
							<div id="divKeySel" class="col-sm-2 sshKey" style="display: none;">
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
							<label class="col-sm-2 control-label">Options</label>
							<div class="col-sm-6">
							
								<div class="checkbox">
									<label title="Check to grant access to resources such as Pods, Services, etc.">
										<input type="checkbox" id="chkSA" name="chkSA" checked="checked">
										<i class="input-helper"></i> Grant cluster resource access
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


	<script type="text/javascript" src="../../js/poll.js"></script>
	<script type="text/javascript" src="common.js"></script>
	
	<script>
	<% if ( action == null && request.getParameter("name") != null ) { %>
	$('#btnAdd').click();
	<% } %>
	
	// fires when the Access tokenlin is clicked (fetch tok)
	function fetch_token() {
		var server 	= $('#apiServer').val();
		var user 	= $('#sshUser').val();
		var pwd 	= $('#sshPwd').val();
		var acct	= $('#acctNS').val();
		var key		= $('#pemKey').val();
		var alias	= $('#name').val();
		
		if ( !user || (!pwd && !key) || !server || !acct) {
			modalNodesSetStatus /*growl*/('API Server, Service Account and SSH information are required to fetch the access token.', 'danger');
			return false;
		}
		var url 	= '<%=contextPath%>/K8S?op=getToken&acct=' + acct; 
		
		// use key?
		if ( key) {
			pwd = key;
			//LOGD('Using key ' + pwd);
		}
		LOGD('Fetch tok url:' + url);
		$.get( url, { alias:alias, server: server, user: user, password: pwd } ).done(function( data ) {
			// {"message":"OK","status":200,"token":"..."}
		    LOGD( "Fetch token: " + JSON.stringify(data) );
		    
		    if ( data.status == 200) {
		    	// FIXME: Won't set value 2nd time $('#accessToken').trigger("change");
		    	$('#accessToken').html(data.token);
		    	$('#accessToken').val(data.token);
		    }
		    else {
		    	modalNodesSetStatus /*growl*/('Fetch token error: ' + data.message, 'danger');
		    }
		});
		return false;
	}
	
	// fires when the SSH link is clicked
	function ssh (host, user, identity) {
		LOGD('SSH start host:' + host + ' user:' + user + '/' + identity);
		var href = '../ssh/ssh.jsp?host=' + host + '&user=' + user + "&identity=" + identity;
		window.open(href);
	}

	// fires when the add btn in the add node modal is clicked
	function frm_submit() {
		// validate
		if ( $('#name') == '') {
			growl('Node name is required.');
			return false;
		}
		if ( $('#apiServer') == '') {
			growl('API Server URL is required.');
			return false;
		}
		if ( $('#accessToken') == '') {
			growl('Access token is required.');
			return false;
		}
		return true;
	}
	
	
	function deleteNode (name) {
		var r = confirm('Delete ' + name + '?');
		if ( r == true ) {
			location = "nodes.jsp?action=delete&name=" + name;
			return true;
		}
		return false;
	}
	
	function sysinfo (node) {
		var url = '<%=contextPath%>/K8S?op=API&node=' + node;
		notify ('<strong>PLEASE WAIT</strong>', 'info');
		
		$.get( url ,  function( json ) {
			//{"message":"connect timed out","status":500}
			LOGD('SysInfo:'  + JSON.stringify(json));
			
			if ( json.status >= 400) {
				notify ('PING FAILED: ' + json.message, 'danger');
				return;
			}
			// {"message":"OK","status":200,"data":{"gitVersion":"v1.14.0","platform":"linux/amd64","minor":"14","gitTreeState":"clean","compiler":"gc","buildDate":"2019-03-25T15:45:25Z","gitCommit":"641856db18352033a0d96dbc99153fa3b27298e5","goVersion":"go1.12.1","major":"1"}}
			var html = json.data.platform + ' (' + json.data.goVersion + ') Build '  + json.data.buildDate; 
			$.growl({ message : html }, {type : 'info', placement : {from : 'top', align : 'center'}, delay : 20000, offset : {x : 20, y : 85} } );
				
		}, 'json');	
		return false;
	}
	
	// Initialization
	$(document).ready(function() {
	});
	
	function modalNodesSetStatus (text, color) {
		modalSetStatus ('modal1StatusMsg', text, color) ;
	}

	</script>

</body>
</html>