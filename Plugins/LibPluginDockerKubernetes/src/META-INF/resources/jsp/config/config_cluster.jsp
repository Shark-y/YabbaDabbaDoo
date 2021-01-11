<%@page import="com.cluster.ClusterDaemon"%>
<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cluster.update.AutoUpdate"%>
<%@page import="com.cloud.core.cron.AutoUpdateUtils"%>
<%@page import="com.cloud.core.cron.ErrorNotificationSystem"%>
<%@page import="com.cluster.jsp.JSPConfigCluster.StatusResponse"%>
<%@page import="com.cluster.jsp.JSPConfigCluster"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.cluster.CloudCluster"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>

<%
	final boolean showLeftNav	= request.getParameter("leftnav") != null;

	final String KEY_GRP 		= CloudCluster.KEY_NODE_GRP; 				// Cluster group name param key
	final String KEY_MEMBERS 	= CloudCluster.KEY_NODE_MEMBERS; 			// Cluster memebers (for tcp discovery)
	final String KEY_EPS 		= ClusterDaemon.KEY_EPS; 					// service endpoints
	
	final String contextPath 	= getServletContext().getContextPath();
	
	//boolean loggedIn 			= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	final String action			= request.getParameter("action");

	NodeConfiguration cfg		= null; 
	
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)

	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	
	// load a config from: session, then servlet ctx
	cfg 						= (NodeConfiguration)session.getAttribute(NodeConfiguration.SKEY_CFG_SERVER);
	

	if ( cfg == null) {
		//JSPConfigCluster.LOGD("Unable to load cfg from session. Trying servlet ctx.");
		cfg = (NodeConfiguration)getServletContext().getAttribute(NodeConfiguration.SKEY_CFG_SERVER);
	}
	if ( cfg == null) {
		JSPConfigCluster.LOGE("** FATAL: No server cfg available!");
		response.sendRedirect("../../login.jsp?action=loginshow&r=." + (showLeftNav ? "&leftnav=1" : "") + "&m=Session+Expired." );
		return;
	}
	if ( action != null ) {
		StatusResponse resp = JSPConfigCluster.execute(request);
		statusType 		= resp.statusType;
		statusMessage	= resp.statusMessage;
	}
	
	final String clusterGroup 	= cfg.get(KEY_GRP) != null 
			? cfg.get(KEY_GRP).toString() 
			: "" ; //ClusterManager.getClusterGroupName() ;

	// Use a cluster TCP memeber list (if available) else use the HTTP request.
	final String clusterMembers	= cfg.get(KEY_MEMBERS) != null ? cfg.get(KEY_MEMBERS).toString() : "";

	
	//JSPConfigCluster.LOGD(/*"TCPCfg members:" + list +*/ "Configuration members:" + cfg.get(KEY_MEMBERS) 
	//		+ " CM:" + clusterMembers + " LeftNav:" + showLeftNav + " title:" + title + " theme:" + theme);
	
	/* Node should be down before changes...
	if ( CloudServices.servicesOnline()) {
		statusType 		= "WARN";
		statusMessage	= "Stop the node before any changes.";
	} */
	
	//ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, request, "Visitor entered cluster configuration.");

	// smtp config
	JSONObject smtpCfg = null;
	try {
		smtpCfg = AutoUpdateUtils.getConfiguration();
	}
	catch (Exception e) {
		//e.printStackTrace();
		statusType 		= "WARN";
		statusMessage	= "Unable to load the notification/auto-update configuration. Please save it.";
		
		smtpCfg = ErrorNotificationSystem.getDefaultConfiguration();		
	}
%>
    
<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Configure" name="title"/>
</jsp:include>


<script type="text/javascript">

var STOP_POLL = false;

/**
 * Fires when the restart link is clicked.
 */
function restart_onclick() {
	var statusObj = document.getElementById('statusMsg');
	if ( statusObj) {
		statusObj.innerHTML = 'Please wait...';
	}
	STOP_POLL 	= true;		// stop polling
	location 	= 'config_cluster.jsp?action=restart<%=showLeftNav ? "&leftnav" : ""%>';
}

function start_onclick() {
	location = 'config_cluster.jsp?action=start<%=showLeftNav ? "&leftnav" : ""%>';
}

function stop_onclick() {
	location = 'config_cluster.jsp?action=stop<%=showLeftNav ? "&leftnav" : ""%>';
}
</script>

<script type="text/javascript" src="cfg_hz.js"></script>
<script type="text/javascript" src="cfg_sep.js"></script>

<script type="text/javascript">

function delRow(row) {
	while ( row.parentNode && row.tagName.toLowerCase() != 'tr') {
		row = row.parentNode;
	}
	if ( row.parentNode && row.parentNode.rows.length > 1) {
		row.parentNode.removeChild(row);
	}
}

function cleanTable (table) {
	var rowCount 	= table.rows.length;
	
	// don't delete the 1st row (headings)
	for ( var i = 1 ; i < rowCount ; i++ ) {
		delRow (table.rows[1]);
	}
}


window.onload = function() 
{
	//setOKStatus("Please wait...");
	// start polling...
	// 6/8/2018 DISABLED - setTimeout("poll('<%=contextPath%>/ClusterServlet?rq_operation=getmembers')", 1000);
}

function form_submit() {
	var btn1 = document.getElementById('btnError');
	var btn2 = document.getElementById('btnUpdate');
	
	btn1.disabled = btn2.disabled = true;
	return true;
}
</script>

</head>

<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Cluster Configuration" name="pageTitle"/>
		<jsp:param value="Home,Pages,Cluster Configuration" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
		<jsp:param value="<%=!showLeftNav%>" name="hideLeftNav"/>
	</jsp:include>

		<!-- STATUS MESSAGE -->
		<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
			<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
			<jsp:param value="<%=statusType%>" name="statusType"/>
		</jsp:include>

		<% if ( request.getParameter("debug") != null) { %>
		<div class="row">
        	<div class="col-lg-12">
        	Cluster Service Status:
        	<% if ( CloudCluster.getInstance().isShutdown() ) { %>
        	Down - <a href="javascript:start_onclick()">Start Service</a>
        	<% } else { %>
        	Up - <a href="javascript:stop_onclick()">Stop Service</a>
        	<% } %>
			</div>
		</div>
		<br/>
		<% } %>
		
		<form method="post" role="form" action="config_cluster.jsp?<%=showLeftNav ? "&leftnav" : ""%>" onsubmit="return form_submit()">
		
		<input type="hidden" id="action" name="action" value="save">
		
		<% if ( cfg.getFailOverType() == FailOverType.CLUSTER_HAZELCAST) { %>
		<div class="panel panel-default card">
		
			<div class="panel-heading card-header">
				<h3 class="panel-title">Hazelcast Cluster</h3> 
			</div>
			
        	<div class="panel-body card-body">
 	        
				<!-- Cluster Group -->
				<div class="form-group input-field col s12">
		            <label for="<%=KEY_GRP%>">Cluster Group</label>
		            <input type="text" id="<%=KEY_GRP%>" name="<%=KEY_GRP%>" class="form-control" value="<%=clusterGroup%>" required="true">
		            
		            <% if ( SkinTools.isBootstrapTheme()) {%>
		            <p class="help-block">Enter the name of the cluster group this node belongs to. Change it to join another cluster.</p>
		            <% } %>
				</div>
				
				<!-- row 2: TCP discovery -->
				<div class="form-group input-field col s12">
		            <label for="<%=KEY_MEMBERS%>">Cluster Members (for TCP Discovery)</label>
		            
		            <input type="text" id="<%=KEY_MEMBERS%>" name="<%=KEY_MEMBERS%>" class="form-control" 
		            	value="<%=clusterMembers%>" maxlength="1024" 
		            	placeholder="Comma separated list of member IP addresses.">
		            
		            <% if ( SkinTools.isBootstrapTheme()) {%>
		            <p class="help-block">Enter a comma separated list of member IP addresses. Leave empty to use multicast discovery (default).</p>
		            <% } %>
		        </div>

				<!-- row 3: Service Endpoints 
				<div class="form-group input-field col s12">
		            <label for="<%=KEY_EPS%>">Service EndPoints</label>
		            
		            <textarea id="<%=KEY_EPS%>" name="<%=KEY_EPS%>" rows="5" cols="20" class="form-control" ><%=cfg.get(KEY_EPS) != null ? cfg.get(KEY_EPS).toString() : "" %></textarea>
		            	
		            <% if ( SkinTools.isBootstrapTheme()) {%>
		            <p class="help-block">Enter a list of service endpoint URLs, one per line.</p>
		            <% } %>
		        </div>
			    -->
			    <!-- 6/9/2018 moved outside form 
		        <% if ( !CloudServices.servicesOnline()) { %>.
		        <button type="submit" class="btn btn-primary">Save</button>
		        <%} %>
				-->
			</div> 
		</div>
		<% } %>
		<div class="panel panel-default card">
			<div class="panel-heading card-header">
				<h3 class="panel-title">Proactive Error Notification System</h3> 
			</div>
			
        	<div class="panel-body card-body">
        	
        		<p>It scans the local container logs for errors. It sends a notification to the recipient if errors are found. </p>
        	
				<!--  Check Frequency -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Frequency</label>
					<div class="col-sm-10">
						<select id="<%=ErrorNotificationSystem.KEY_FREQ%>" class="form-control" name="<%=ErrorNotificationSystem.KEY_FREQ%>" 
							title="Frequency at which errors are checked.">
							<option value="DISABLED" <%=AutoUpdateUtils.configGetProperty(ErrorNotificationSystem.KEY_FREQ, "").equals("DISABLED") ? "selected" : ""%>>Disabled</option>
							<option value="DAILY" <%=AutoUpdateUtils.configGetProperty(ErrorNotificationSystem.KEY_FREQ, "").equals("DAILY") ? "selected" : ""%>>Daily</option>
							<option value="WEEKLY" <%=AutoUpdateUtils.configGetProperty(ErrorNotificationSystem.KEY_FREQ, "").equals("WEEKLY") ? "selected" : ""%>>Weekly</option>
						</select>
					</div>
				</div>

        		<!--  Customer -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Customer</label>
					<div class="col-sm-10">
						<input id="<%=ErrorNotificationSystem.KEY_VENDOR%>" type="text" class="form-control" 
							name="<%=ErrorNotificationSystem.KEY_VENDOR%>" 
							value="<%=AutoUpdateUtils.configGetProperty(ErrorNotificationSystem.KEY_VENDOR, "")%>"
							title="Customer name for notification information." required="required">
					</div>
				</div>

       			<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Search Filter</label>
					<div class="col-sm-10">
						<input id="<%=ErrorNotificationSystem.KEY_FILTER%>" type="text" class="form-control" 
							name="<%=ErrorNotificationSystem.KEY_FILTER%>" 
							value="<%=AutoUpdateUtils.configGetProperty(ErrorNotificationSystem.KEY_FILTER, "RegExp:(?s).*(NullPointerException|OutOfMemory).*")%>"
							title="Search regular expression.." required="required">
						<!--
						<select class="form-control" id="<%=ErrorNotificationSystem.KEY_FILTER%>" name="<%=ErrorNotificationSystem.KEY_FILTER%>">
							<option value="RegExp:(?s).*(NullPointerException|OutOfMemory).*">NullPointers and Out of Memory Errors</option>
							<option value="RegExp:(?s).*(NullPointerException|IOException).*">NullPointers and I/O Errors</option>
							<option value="Exception">All Exceptions</option>
						</select>
						-->
					</div>
				</div>
				
				<!-- smtps values -->
				<jsp:include page="../../tiles/tile_ds_smtp.jsp">
					<jsp:param value="<%=smtpCfg.toString()%>" name="dsJSON"/>
					<jsp:param value="block" name="display"/>
					<jsp:param value="notification_" name="idPrefix"/>
				</jsp:include>
        	
        		<% if (AutoUpdateUtils.configExists()) { %>
        		<!-- test btn -->
				<div class="form-group">
		        	<div class="col-sm-12">
				        <button id="btnError" type="submit" class="btn btn-success" onclick="document.getElementById('action').value = 'test_pen'">Check for Errors</button>
					</div>
				</div>
        		<% } %>
			</div>
		</div>

		<!--  Auto Update panel -->
		<div class="panel panel-default card">
			<div class="panel-heading card-header">
				<h3 class="panel-title">Automatic Update System - <a href="https://www.dropbox.com/login" target="_new">Cloud Storage</a></h3> 
			</div>
			
        	<div class="panel-body card-body">
        	
				<p>It automatically updates all your local C1AS products. It sends a notification when updates are completed.</p>
				
				<!--  Check Frequency -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Frequency</label>
					<div class="col-sm-10">
						<select id="<%=AutoUpdate.KEY_FREQ%>" class="form-control" name="<%=AutoUpdate.KEY_FREQ%>" 
							title="Frequency at which updates are checked.">
							<option value="DISABLED" <%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_FREQ, "").equals("DISABLED") ? "selected" : ""%>>Disabled</option>
							<option value="DAILY" <%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_FREQ, "").equals("DAILY") ? "selected" : ""%>>Daily</option>
							<option value="WEEKLY" <%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_FREQ, "").equals("WEEKLY") ? "selected" : ""%>>Weekly</option>
						</select>
					</div>
				</div>

        		<!--  Access Token -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Access Token</label>
					<div class="col-sm-10">
						<input id="<%=AutoUpdate.KEY_TOKEN%>" type="text" class="form-control" 
							name="<%=AutoUpdate.KEY_TOKEN%>" 
							value="<%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_TOKEN, "F1S6eC-N7sAAAAAAAAAADApR55PokUZoqQ909QXQR861ZX_qnZx-FfZvfFs1oEw-")%>"
							title="Dropbox Access Token." required="required">
					</div>
				</div>

       			<!--  URL -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Manager URL</label>
					<div class="col-sm-10">
						<input id="<%=AutoUpdate.KEY_TC_URL%>" type="text" class="form-control" 
							name="<%=AutoUpdate.KEY_TC_URL%>" 
							value="<%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_TC_URL, "http://localhost:8080/manager")%>"
							title="Tomcat container manager." required="required">
					</div>
				</div>

        		<!--  user -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Manager User</label>
					<div class="col-sm-10">
						<input id="<%=AutoUpdate.KEY_TC_USER%>" type="text" class="form-control" 
							name="<%=AutoUpdate.KEY_TC_USER%>" 
							value="<%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_TC_USER, "")%>"
							title="Tomcat container uuser name." required="required">
					</div>
				</div>
        		<!--  pwd -->
        		<div class="form-group" id="smtpr9">
					<label class="col-sm-2 control-label">Manager Password</label>
					<div class="col-sm-10">
						<input id="<%=AutoUpdate.KEY_TC_PWD%>" type="password" class="form-control" 
							name="<%=AutoUpdate.KEY_TC_PWD%>" 
							value="<%=AutoUpdateUtils.configGetProperty(AutoUpdate.KEY_TC_PWD, "")%>"
							title="Tomcat container password." required="required">
					</div>
				</div>
				
        		<% if (AutoUpdateUtils.configExists()) { %>
        		<!-- test btn -->
				<div class="form-group">
		        	<div class="col-sm-12">
				        <button id="btnUpdate" type="submit" class="btn btn-success" onclick="document.getElementById('action').value = 'test_update'">Check for Updates</button>
					</div>
				</div>
				<% } %>
			</div>
		</div>
							
		<!-- SAVE BTN /.row  -->
		<div class="row">
        	<div class="col-lg-12">
        		<% if ( !CloudServices.servicesOnline()) { %>.
		        <button type="submit" class="btn btn-primary">Save</button>
		        <%} %>
			</div>
		</div>

		<!-- /.row -->
    	<!-- 6/8/2018 DISABLED 
		<div class="row">
        	<div class="col-lg-12">
        		<h4>Cluster Nodes</h4>
        		<table class="table" id="tblNodes">
        			<tr>
        				<th>Name</th>
        				<th>Status</th>
        				<th>Details</th>
        			</tr>
        		</table>
			</div>
		</div>
		-->
		
		</form>
				
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=!showLeftNav%>" name="hideLeftNav"/>
	</jsp:include>

</body>
</html>