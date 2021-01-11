<%@page import="com.cloud.ssh.SSHExec"%>
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
	
	final String header = "<img width='48' src='../../img/k8s.png'> Kubernetes  &nbsp;&nbsp;&nbsp;&nbsp;" 
		+ "<button title=\"Click to add a cluster.\" id=\"btnAdd\" class=\"btn btn-raised md-btn\" data-toggle=\"modal\" data-target=\"#modal1\" data-uk-modal=\"{target:'#modal1'}\">+</button>";
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

<style>
@media (min-width:1220px) {
 .uk-modal-dialog-large {
  width:980px
 }
}
</style>
</head>
<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=header %>" name="pageTitle"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>

<!-- CONTENT -->
	<!-- 
	<div class="row">
		<div class="col-md-12">
			<button id="btnAdd" class="btn btn-raised" data-toggle="modal" data-target="#modal1">Add</button>
		</div>
	</div>
	-->
	<div class="row uk-grid uk-grid-width-medium-1-3 uk-sortable sortable-handler" data-uk-grid="{gutter:24}" data-uk-sortable>
	
	<% for ( Map.Entry<String, K8SNode> entry : clusters.entrySet()) { 
		K8SNode node 			= entry.getValue();
		final String name 		= node.getName();
		final String host		= (new URL(node.getApiServer())).getHost();
		final String identity	= node.getSSHPwd().startsWith(SSHExec.KEY_PREFIX) ? node.getSSHPwd().replaceAll("\\\\", "/") : node.getSSHPwd();
	%>
		
		<div class="col-md-3 col-sm-6">
			<div class="panel panel-white ov-h md-card" data-widget='{"draggable": "false"}'>
				<%if ( SkinTools.isCloudsTheme()) { %>
				<div class="panel-controls dropdown">
					<button class="btn btn-icon-rounded refresh-panel" onclick="return sysinfo('<%=name%>')"><span class="material-icons inverted">refresh</span></button>
					<button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="#" onclick="return ssh('<%=host%>','<%=node.getSSHUser()%>','<%=identity%>')">Shell</a></li>
                        <li class="divider"></li>
                        <li><a href="#" onclick="return deleteNode('<%=name%>')">Delete</a></li>
                    </ul>						
				</div>
				<div class="panel-heading">
					<h2><a title="Click to edit" href="nodes.jsp?name=<%=name%>"><%=name%></a></h2>
				</div>
				<% } else if ( SkinTools.isAltAirTheme()) { %>
				<!-- md-card-toolbar -->
				<div class="md-card-head head_background" style="background-image: url('../../img/kube.png')">
					<!-- md-card-toolbar-actions -->
					<div class="md-card-head-menu">
						<i class="md-icon material-icons md-icon-light" onclick="return sysinfo('<%=name%>')">refresh</i>
						<div class="md-card-dropdown" data-uk-dropdown="{pos:'bottom-right'}">
                        	<i class="md-icon material-icons md-icon-light">&#xE5D4;</i>
                        	<div class="uk-dropdown uk-dropdown-small">
                            	<ul class="uk-nav uk-nav-dropdown">
			                        <li><a href="#" onclick="return ssh('<%=host%>','<%=node.getSSHUser()%>','<%=identity%>')">Shell</a></li>
			                        <li class="uk-nav-divider"></li>
			                        <li><a href="javascript: deleteNode('<%=name%>')">Delete</a></li>
                            	</ul>
                        	</div>
						</div>						
					</div>
					<!-- md-card-toolbar-heading-text -->
					<h3 class="md-card-head-text"> <a class="md-btn md-btn-primary" title="Click to edit" href="nodes.jsp?name=<%=name%>"><%=name%></a></h3>
				</div>
				<% } %> 
				<div class="panel-body ov-h md-card-content">
					<table style="height: 120px">
						<tr>
								<td><%=node.getApiServer() %>
								</td>
						</tr>
						<tr><td><span id="platform<%=name%>"></span></td></tr>
						<tr><td><span class="label" id="nodes<%=name%>"></span></td></tr>
						<tr><td><span id="images<%=name%>"></span></td></tr>
						<tr>
							<td>
								<a href="manage.jsp?name=<%=name%>">Manage</a>&nbsp;&nbsp;&nbsp;
								<a href="helm/helm.jsp?name=<%=name%>">Apps</a>
							</td>
						</tr>
						
					</table>
				</div>	<!-- panel body -->
			</div>	<!-- end panel -->
			
		</div>	<!-- col-md3 -->
	<% }  %>

	</div>	<!-- row -->

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
		<div id="modal1" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog uk-modal-dialog-large">
				<% if ( SkinTools.isAltAirTheme()) { %>
				<button type="button" class="uk-modal-close uk-close"></button>
				<% } %>

				<div class="modal-content">
					<div class="modal-header uk-modal-header">
						<% if ( SkinTools.isCloudsTheme()) { %>
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<% } %>
						<h3 class="modal-title">Add Cluster</h3>
					</div>
					<div class="modal-body">
						<span id="modal1StatusMsg"></span>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass() %>">Name</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input class="<%=SkinTools.cssInputClass() %>" id="name" name="name"
									required data-toggle="tooltip"
									pattern="[A-z0-9/]+"
									title="Node name (no spaces)." value="<%=name%>">
							</div>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass()%>">API Server</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input type="text" name="apiServer" id="apiServer" class="<%=SkinTools.cssInputClass() %>" 
									required="required" title="Host IP Address:Port" placeholder="https://IP-ADDRESS:6443" value="<%=host%>"/>
							</div>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<div class="<%=SkinTools.cssFormGroupLabelClass() %>">
								<label class="control-label"><a href="#" title="Click to fetch token. SSH information is required." onclick="return fetch_token()">Access Token</a></label>
								<!-- 
								<a href="#" title="Click to toggle file selection" style="float: right;" onclick="$('#divCertSel').toggle();$('#divCert').toggleClass('col-sm-10').toggleClass('col-sm-8')">Advanced</a>
								-->
							</div>
							<div id="divCert" class="col-sm-7 uk-width-2-4">
								<textarea class="<%=SkinTools.cssInputClass() %>" id="accessToken" name="accessToken" rows="2" cols="80" class="form-control" title="Access Token" required="required"><%=accessToken%></textarea>
							</div>
							<div class="col-sm-3 uk-width-1-4">
								<input type="text" name="acctNS" id="acctNS" class="form-control" required="required" value="default:default" title="Service Acoount:Namespace" />
							</div>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass() %>">SSH User</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input class="<%=SkinTools.cssInputClass() %>" id="sshUser" name="sshUser" value="<%=sshUser%>" required="required"
									title="An SSH User is required for HELM system administration.">
							</div>
						</div>
						<!-- SSH pwd or key -->
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<div class="<%=SkinTools.cssFormGroupLabelClass() %>">
								<!-- style="float: right;" -->
								<label class="control-label"><a href="#" 
									title="Click to toggle an SSH Private Key file selection"  
									onclick="$('.sshKey').toggle();$('#divKey').toggleClass('col-sm-10').toggleClass('col-sm-8')">SSH Pwd</a></label>
							</div>
							<div id="divKey" class="col-sm-10 uk-width-2-4">
								<input type="password" class="<%=SkinTools.cssInputClass()%> sshKey" id="sshPwd" name="sshPwd" value="<%=sshPwd%>"
									title="SSH Password for HELM system administration.">
									
								<textarea id="pemKey" name="pemKey" rows="2" cols="80" class="<%=SkinTools.cssInputClass()%> sshKey" style="display: none;"
									title="Private Key - PEM encoded (required for TLS only)"></textarea>
							</div>
							<div id="divKeySel" class="col-sm-2 sshKey uk-width-1-4" style="display: none;">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file md-btn md-btn-default">
										<span class="fileinput-new">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="fileKey" onchange="openFile(event, 'pemKey')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>
						</div>
						
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass() %>">Options</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
							
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
					
					<div class="modal-footer uk-modal-footer uk-text-right">
							<input type="submit" value="Save" class="btn btn-raised btn-primary md-btn md-btn-primary">
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
	
	// fires when the Access tokenlin is clicked (fetch tok)
	function fetch_token() {
		var server 	= $('#apiServer').val();
		var user 	= $('#sshUser').val();
		var pwd 	= $('#sshPwd').val();
		var acct	= $('#acctNS').val();
		var key		= $('#pemKey').val();
		var alias	= $('#name').val();
		
		if ( !user || (!pwd && !key) || !server || !acct) {
			modalNodesSetStatus ('API Server, Service Account and SSH information are required to fetch the access token.', 'danger');
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
		var url = '<%=contextPath%>/K8S?op=describe&node=' + node;
		//notify ('<strong>PLEASE WAIT</strong>', 'info');
		
		$.get( url ,  function( json ) {
			//{"message":"connect timed out","status":500}
			LOGD('SysInfo:'  + JSON.stringify(json));
			
			if ( json.status >= 400) {
				notify ('PING FAILED: ' + json.message, 'danger');
				return;
			}
			updateSysInfo (node, json);
			
			// save it
			setCookie(node, JSON.stringify(json));
		}, 'json');	
		return false;
	}
	
	function updateSysInfo ( node, json ) {
		// {"message":"OK","status":200,"data":{"gitVersion":"v1.14.0","platform":"linux/amd64","minor":"14","gitTreeState":"clean","compiler":"gc","buildDate":"2019-03-25T15:45:25Z","gitCommit":"641856db18352033a0d96dbc99153fa3b27298e5","goVersion":"go1.12.1","major":"1"}}
		// {"totalImages":289,"data":{"gitVersion":"v1.18.3","gitCommit":"2e7996e3e2712684bc73f0dec0200d64eec7fe40","major":"1","minor":"18","goVersion":"go1.13.9","buildDate":"2020-05-20T12:43:34Z","compiler":"gc","gitTreeState":"clean","platform":"linux/amd64"},"nodesTotal":4,"message":"OK","nodesOnline":6,"status":200} 
		var html = json.data.platform + ' (' + json.data.goVersion + ') '  + json.data.buildDate; 
		//$.growl({ message : html }, {type : 'info', placement : {from : 'top', align : 'center'}, delay : 20000, offset : {x : 20, y : 85} } );
		var label = json.nodesOnline != json.nodesTotal ? 'label-danger uk-badge uk-badge-danger' : 'label-info uk-badge';
		
		$('#platform' + node).html(html);
		$('#nodes' + node).removeClass('label-info label-danger uk-badge uk-badge-danger');
		$('#nodes' + node).addClass(label).html ('Nodes : ' + json.nodesOnline + '/' + json.nodesTotal + ' online');
		$('#images' + node).html('Images: ' + json.totalImages);
	}
	
	// Initialization
	$(document).ready(function() {
		<% for ( Map.Entry<String, K8SNode> entry : clusters.entrySet()) { 
			final String key  = entry.getValue().getName();
		%>
		var node = '<%=key%>';
		var json = getCookie(node);
		//LOGD('<%=key%> cookie = ' + json );
		
		if ( json ) {
			updateSysInfo (node, JSON.parse(json));
		}
		<% } %>
		
		<% if ( action == null && request.getParameter("name") != null ) { %>
		setTimeout(function() { $('#btnAdd').click(); }, 500);
		//$('#btnAdd').click();
		<% } %>
	});
	
	function modalNodesSetStatus (text, color) {
		modalSetStatus ('modal1StatusMsg', text, color, '<%=SkinTools.getSkinName()%>') ;
	}

	function setCookie (key, value) {
		LOGD('Set cookie ' + key + ' = ' + value);
		window.localStorage.setItem(key, value);
	}

	function getCookie (key) {
		LOGD('Get cookie ' + key);
		return window.localStorage.getItem(key);
	}

	</script>

</body>
</html>