<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="com.cloud.core.cron.ErrorNotificationSystem"%>
<%@page import="com.cloud.core.cron.AutoUpdateUtils"%>
<%@page import="com.cloud.core.profiler.OSProfilerAlerts"%>
<%@page import="com.cloud.console.JSPLoggerTool"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.console.HTMLConsoleLogUtil"%>
<%@page import="com.cloud.console.jsp.JSPNodeConfig"%>
<%@page import="com.cloud.console.CloudConsole"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cloud.core.services.NodeConfiguration.RunMode"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.config.ServiceConfiguration.WidgetType"%>
<%@page import="com.cloud.core.config.ConfigItem"%>
<%@page import="java.io.IOException"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.io.File"%>
<%@page import="com.cloud.core.config.ServiceConfiguration.WidgetType"%>
<%@page import="com.cloud.core.config.ServiceConfiguration"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Enumeration"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Properties"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	/** Theme checks */
	String theme					= (String)session.getAttribute("theme");
	String title					= (String)session.getAttribute("title");
	/*	
	// This occurs if the session expires or the users tries to bypass the login. Redirect to login then back to HOME
	if (theme == null || title == null) {
		response.sendRedirect("../../login.jsp?action=loginshow&r=.&m=Session expired." );
		return;		
	} */
%>

<%
	/**
	 * Configuration Page.
	 */
	String action  					= request.getParameter("action");
	String uiMessage				= null;		// status messages
	String statusType				= "INFO";	// Type of status msg (INFO, WARN, ERROR)
	
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 

	// Check for a deafult connection profile.
	final String cnProfile			= JSPNodeConfig.getDefaultProfile(request, cfgServer);
	//final String defLogFolder		= HTTPServerTools.getDefautContainerLogFolder();		// defaut container log
			
	if ( cnProfile == null) {
		JSPNodeConfig.LOGW("No default connection profile. Redirect to PM.");
		response.sendRedirect("profiles.jsp?m=Please+add+or+select+a+connection+profile.");
		return;
	}

	cfgServer.setConnectionProfileName(cnProfile);

	// new profile activated? Initialize the services for that profile
	if ( request.getParameter("n") != null) {
		CloudServices.initializeServices("NODE-CFG-PROFILE-ACTIVE " + cnProfile);
	}
	
	JSPNodeConfig.LOGD("Action:" + action + " CN profile: " + cnProfile); 
	
	// action?
	if ( action != null ) {
		if ( action.equals("save")) {
			try {
				final boolean logSysChanged 	= JSPNodeConfig.logSubSystemChanged(request, cfgServer);
				final boolean clusterSysChanged = JSPNodeConfig.clusterSubSystemChanged(request, cfgServer);
				
				JSPNodeConfig.validate(request);
				JSPNodeConfig.updateProperties(request,  cfgServer, true); //, new String[] {"server_", "oauth1_", "log", "sysadmin" ,"product_"} ); 
				
				// create dest location?
				if ( !IOTools.mkDir(cfgServer.getConfigLocation()) ) {
					throw new IOException("Unable to save. Can't create " + cfgServer.getConfigLocation());
				}
				
				/* 8/2/2019 Cluster sys changed
				if ( clusterSysChanged ) {
					JSPNodeConfig.LOGD("==> Cluster Mode has changed. Executing a cluster shutdown.");
					com.cloud.cluster.CloudCluster.getInstance().shutdown();
				} */

				// 1/15/2020 cleanup unwanted stuff
				JSPNodeConfig.cleanupProps(cfgServer, new String[] {"txt", "notification_", "action", "test_", "rbac_"} );

				// Save & notify listeners
				CloudServices.nodeConfigurationSaved();			
				CloudConsole.getInstance().nodeConfigSaved(cfgServer);
				// vsilva 12/6/2016 cfgServer.save();
				
				// 9/4/17 reinit node logger if cfg changed
				if ( logSysChanged) {
					HTMLConsoleLogUtil.reinitializeNodeLogger();
				}
				Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CONFIGURATION_CHANGED, "Node configuration saved.");
				
				// audit test?
				String auditTest 	= request.getParameter("test_audit");
				String penTest 		= request.getParameter("test_pen");
				
				if ( auditTest != null && auditTest.equals("true")) {
					uiMessage 	= "Message sent."; // + request.getParameter(Auditor.KEY_NOTIFY_SMTP_TO);
				}
				else  if ( penTest != null && penTest.equals("true")) {
					uiMessage 	= ErrorNotificationSystem.checkForErrorsConsole();
				}
				else {
					uiMessage 	= "Node configuration updated. Please make sure you save all components.";
				}
				statusType	= "WARN"; //"SUCCESS";
					
				// clear the last error.
				// vsilva 12/6/2016 CloudServices.clearLastError();
				
				// 1/15/2020 
				OSProfilerAlerts.save(request);		// alerts: thread , cpu, license
				AutoUpdateUtils.save(request);		// container exceptions
				Rbac.save(request);					// 5/17/2020 role based access control
				
				// for warn if leaving the page
				session.setAttribute("nodesaved", "true");
			}
			catch (Exception e1) {
				JSPNodeConfig.LOGE("Config Save Error: " + e1.toString(), e1);	// stdout
				JSPLoggerTool.JSP_LOGE("Config Save", e1.toString(), e1);		// console log
				uiMessage 	= e1.getMessage();
				statusType	= "ERROR";
			}
		}
		else {
			JSPNodeConfig.LOGW("Invalid action:" + action);
			statusType 	= "ERROR";
			uiMessage	= "Invalid action:" + action;
		}
	}
	
	// Node should be down before changes...
	if ( uiMessage == null && CloudServices.servicesOnline()) {
		statusType 	= "WARN";
		uiMessage	= "Node should be stopped before any changes.";
	}

	ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request, "Visitor entered node configuration.");
	
	//  get the log rotation pol (or default twice a day)
	//final String logRotationPolicy = cfgServer.getProperty(NodeConfiguration.KEY_LOG_ROTATION_POL, HTMLConsoleLogUtil.ROLLOVER_TWICEADAY);
	
	// load alerts
	OSProfilerAlerts.reload();
%>    


<!DOCTYPE html>
<html lang="en">

<head>


<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
</jsp:include>

<!--  common styles -->
<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">
<link href="../../lib/dropzone/dropzone.css" type="text/css" rel="stylesheet">

<script type="text/javascript" src="config.js"></script>

<script>

/**
 * Set the state of the UI
 * @param state String: login, changepwd, createpwd, or save
 */
function uiSetState(state) {
	// hive all
	//uiHideAll();
}

/**
 * set the values of widgets that are NOT INPUT of type text
 */
function setPageValues() 
{
	audit_proto_change();
	authorization_proto_change();
}

/* page load driver */
function win_onload() 
{
	setPageValues();
}

/** 
 * Fires when the run mode combo changes 
 */
function runModeSelectOnChange() 
{
	// validateFailOver is DEPRECATED
	/*
	return validateFailOver( 
			'<%=NodeConfiguration.KEY_PRIMARY%>'
			,'<%=NodeConfiguration.KEY_FAILOVER_INT%>'
			,'<%=NodeConfiguration.KEY_RUN_MODE%>'); */
	return true; // TODO: More validations?
}

/* fires when the save btn is pressed */
function config_save() 
{
	// force selection of MV widgets
	//uiSelectMultiWidgets();
	var valid = true;
	
	// validation: failover. More validations here?
	valid = runModeSelectOnChange();
	
	// audit test? Need host port
	/*
	var auditTest 	= document.getElementById('test_audit').value == 'true';
	var host 		= document.getElementById('<%=Auditor.KEY_NOTIFY_SMTP_HOST%>');
	var port 		= document.getElementById('<%=Auditor.KEY_NOTIFY_SMTP_PORT%>');
	LOGD("CFGSAVE: AuditTest:"  + auditTest + " h:" + host.value + " p:" + port.value);
	
	if (auditTest && (host.value.length == 0) && (port.value.length == 0) ) {
		alert('Audit Test: A notification host & port are required.');
		valid = false;
	}*/
	return valid;
}

/**
 * Check for save before leaving
 */
var CAN_LEAVE = <%=session.getAttribute("nodesaved") != null%>;

function save_onclick() {
	CAN_LEAVE = true;
}

window.onbeforeunload = function (e) {
	if ( ! CAN_LEAVE ) {
		return "You haven't saved?";
	}
}

window.onload = win_onload;

</script>

<script type="text/javascript">

function audit_test_click() {
	var obj = document.getElementById('test_audit');
	obj.value = 'true';
	
	// submit - click
	var btn = document.getElementById('btn_submit');
	btn.click();
}

function getObject ( id ) {
	return document.getElementById(id);
}

function audit_proto_change() {
	var cmb 	= document.getElementById('<%=Auditor.KEY_NOTIFY_PROTO%>');
	
	// none (disabled), (alerts): smtp, smtps, twitter, twilioSMS, plain (no alerts)
	var proto	= cmb != null ? cmb.options[cmb.selectedIndex].value : null;
	
	// fix for http://skyfall.acmelab.com:6091/issue/CLOUD_CORE-102
	if ( !cmb ) {
		LOGW('audit_proto_change: Proto combo not found. Abort.');
		return;
	}
	/* open modal1 if alert sys cmb enabled
	if ( proto != 'none') {
		$('#linkModal1').trigger('click');
	} */

	var r1 		= document.getElementById('ar1');
	var r2		= document.getElementById('ar2');
	
	// SMTP basic audit fields
	var rows 	= [ getObject('ar3'), getObject('ar4'), getObject('ar5'), getObject('ar6') ];

	// SMTP Basic audit fields: 
	var style1	= proto.indexOf('smtp') != -1 ? 'block' : 'none';
	
	for ( var i = 0 ; i < rows.length ; i++) {
		rows[i].style.display = style1;
	}

	// smtps fields
	var style	= proto == 'smtps' ? 'block' : 'none';
	r1.style.display = style;
	r2.style.display = style;

	// Twitter
	style	= proto == 'twitter' ? 'block' : 'none';
	for ( var i = 1 ; i <= 4 ; i++) {
		getObject('tr' + i).style.display = style;
	}
	
	// Twilio SMS
	style	= proto == 'twilioSMS' ? 'block' : 'none';
	for ( var i = 1 ; i <= 4 ; i++) {
		getObject('tw' + i).style.display = style;
	}
	
	// test notification btn
	//getObject('ar7').style.display = proto != 'none' && proto != 'plain' ? 'block' : 'none';
	rows = ['ar7', 'atypes'];
	for ( var i = 0 ; i < rows.length ; i++) {
		getObject(rows[i]).style.display = proto != 'none' && proto != 'plain' ? 'block' : 'none';
	}
}


function authorization_proto_change() {
	var cmb 	= document.getElementById('<%=NodeConfiguration.KEY_AUTH_ENABLED%>');
	var proto	= cmb.options[cmb.selectedIndex].value;
	var len		= 4;
	var style	= proto != 'false' ? 'block' : 'none';
	
	for ( var i = 1 ; i <= len ; i++) {
		var obj = getObject('p2r' + i );
		if ( obj ) {
			obj.style.display = style;
		}
	}
}

function cluster_proto_change() {
	var cmb 	= document.getElementById('<%=NodeConfiguration.KEY_FAILOVER_TYPE%>');
	var proto	= cmb.options[cmb.selectedIndex].value;
	var len		= 3;
	var style	= proto == 'CLUSTER' ? 'block' : 'none';
	
	for ( var i = 1 ; i <= len ; i++) {
		getObject('clr' + i ).style.display = style;
	}
}

</script>

</head>

<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Node Configuration" name="pageTitle"/>
		<jsp:param value="Home,Pages,Node Configuration" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>


		<!--  STATUS MESSAGE -->
		<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
			<jsp:param value="<%=uiMessage != null ? uiMessage : \"NULL\"%>" name="statusMsg"/>
			<jsp:param value="<%=statusType%>" name="statusType"/>
		</jsp:include>


		<!-- Page contents -->
    
		<!-- All Server Configuration information -->
		<form role="form" class="form-horizontal" id="frmConfig" method="post" action="config_node.jsp" onsubmit="return config_save()">
			<!-- 4/15/2017 Cluster disabled by default 
			<input type="hidden" name="<%=NodeConfiguration.KEY_FAILOVER_TYPE%>" value="<%=cfgServer.getFailOverType()%>">
			-->
			<!-- 4/23/15 FAILOVER/RUNMODE is CLUSTER -->
			<input type="hidden" name="<%=NodeConfiguration.KEY_RUN_MODE%>" value="PRIMARY">

			<!-- 1/8/2017 service hours always disabled -->
			<input type="hidden" name="<%=NodeConfiguration.KEY_SSHED_ENABLED%>" value="false">
		
			<input type="hidden" id="action" name="action" value="save">
			
			<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
			
				<div class="panel-heading card-header md-card-toolbar">
					<h2 class="panel-title md-card-toolbar-heading-text">Main</h2> 
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				
				<div class="panel-body card-body md-card-content">
				<!-- SERVER STUFF -->
					<div class="form-group uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Connection Profile</label>
						<div class="col-sm-10 uk-width-3-4">
							<p id="cnProfile" class="form-control-static"><%=cfgServer.getConnectionProfileName()%></p>
						</div>
					</div>
					
					<%if (cfgServer.containsKey(NodeConfiguration.KEY_SERVER_LIC)) { %>
					<div class="form-group">
						<label class="col-sm-2 control-label">License</label>
						<div class="col-sm-10">
							<textarea name="<%=NodeConfiguration.KEY_SERVER_LIC%>" rows="5" cols="80" class="form-control" required="required"><%=cfgServer.getLicense() %></textarea>
						</div>
					</div>
					<% } %>
					<!-- 5/20/2020 -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<div class="col-sm-12">
							<button type="button" class="btn btn-primary md-btn md-btn-primary" data-toggle="modal" data-target="#modal5" data-uk-modal="{target:'#modal5'}">Plugins</button>
						</div>
					</div>
				</div>
			</div>	
			
			<!-- END SERVER STUFF -->
			
			<!-- Authorization panel -->
			<jsp:include page="tile_config_node_auth.jsp"/>
			<!-- End authorization panel -->
				
			<%if (cfgServer.productSupportsClustering() || cfgServer.productSupportsNotifications() ) { %>
			<br/>
			<!-- Cluster & Event Notification system -->
			<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading card-header md-card-toolbar">
					<h2 class="panel-title md-card-toolbar-heading-text" data-toggle="tooltip" data-placement="top" 
						title="Enable the failover system to be notified of suspicious and/or error events for this node.">High Availability &amp; Notifications
					</h2>
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				<div class="panel-body card-body md-card-content">
				<%if ( cfgServer.productSupportsClustering() ) { %>
					<jsp:include page="tile_config_node_cluster.jsp"/>
				<% } %>
				<%if ( cfgServer.productSupportsNotifications() ) { %>
					<jsp:include page="tile_config_node_notif.jsp"/>
				<% } %>
					<jsp:include page="tile_config_node_cnpool.jsp"/>
				<!-- Cluster Notifications Card body -->
				</div>
			
			<!-- Cluster Notifications Card -->
			</div>
			<% } %>

			
			<!-- Log System panel -->
			<jsp:include page="tile_config_node_log.jsp"/>
			<!-- End log system panel -->	
			
			<!-- IAM/RBAC Modals -->
			<jsp:include page="tile_config_node_modals.jsp"/>
			<jsp:include page="tile_config_node_iam.jsp"/>
			
			<br/>		
			<button id="btn_submit" type="submit" class="btn btn-primary md-btn md-btn-primary"  title="Save" onclick="save_onclick()">Save</button>

		</form>
		<!-- End main (contents) form -->

		<!-- plugin System panel -->
		<jsp:include page="tile_config_node_plugins.jsp"/>
	
	
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>
	<script src="../../lib/dropzone/dropzone.js"></script>

	<script type="text/javascript">
	
	$().ready(function() {
		initPluginsUI ();
		initDropZone ();
		initIAMUI ();
	});
	
	</script>
	
</body>

</html>
