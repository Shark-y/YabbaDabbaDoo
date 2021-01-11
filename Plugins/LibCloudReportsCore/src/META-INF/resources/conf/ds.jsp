<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.rts.jsp.JSPHandlerDataSource"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.ServiceStatus.Status"%>
<%@page import="com.rts.datasource.DataFormat"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="java.util.List"%>
<%@page import="com.rts.datasource.DataSourceManager"%>
<%@page import="com.rts.core.IDataService"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@ page language="java" contentType="text/html; UTF-8" pageEncoding="UTF-8"%>

<%!
static void LOGD(final String text) {
	System.out.println("[DATA_SOURCE] " + text);		
}
%>

<%
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	final String dsName			= (String)request.getParameter("li");		// DS name (or NULL if none)
	//String statusMsg			= request.getParameter("m");				// status message
	
	final String mode			= (String)request.getParameter("mode");		// Service Type: DAEMON, PLUGIN, etc
	final String id				= (String)request.getParameter("id");		// Servie id
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;

	String statusMessage		= "NULL";
	String statusType			= null;

	IDataService service 		= null;
	String serviceCls			= null;
	ServiceDescriptor sd		= null;
	LOGD("Mode: " + mode + " id:" + id );
	
	// 6/5/2020 Plugin support
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		sd						= PluginSystem.findServiceDescriptor(id);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (IDataService)p.getInstance();
		serviceCls				= sd.getClassName();
	}
	else {
		service 				= (IDataService)CloudServices.findService(ServiceType.DAEMON);
		sd						= CloudServices.findServiceDescriptor(ServiceType.DAEMON);
	}
	
	String action				= request.getParameter("action");
	IDataSource ds				= null; 
	String[] OUT				= new String[] {statusMessage, statusType};
	
	if ( action != null ) {
		JSPHandlerDataSource.handle(request, action, service, OUT);
		
		statusMessage 	= OUT[0];
		statusType		= OUT[1];
		
		// special case (edit) - get DS
		if ( action.equals("edit")) {
			ds = service.getDataSource(dsName);
		}
	}
	
	// get updated listeners (if saved)
	List<IDataSource> listeners = service.getDataSourceManager().getDataSources();
	
	// services configured?
	if ( statusMessage.equals("NULL")  && !CloudServices.isConfigured()) {
		// the node should be configured already.
		statusMessage 	= "<a href=\"../jsp/config/config_backend.jsp?mode=DAEMON\">"
			+ sd.getVendorName() + "</a> must be configured.";
		statusType		= "WARN";
	}
	final String crumbLbls = "Home,Pages," + JSPHandlerDataSource.getTitle();
	
	//LOGD("Name=" + dsName + " DS=" + ds);
%>
   
<!DOCTYPE html>
<html>
<head>


<!-- LibAdapterConsole: Bootstrap required styles & headers -->
<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
	<jsp:param value="../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Data Sources" name="title"/>
</jsp:include>

<script type="text/javascript" src="../js/notify.js"></script>
<script type="text/javascript" src="ds.js"></script>

<link href="../css/jquery.qtip.css" type="text/css" rel="stylesheet">

<script>

function del (name) {
	var r = confirm("Delete " + name + "?");
	
	if (r == true) {
		location = 'ds.jsp?action=del&name=' + name + '&mode=<%=mode%>&id=<%=id%>';
	}
}

function getObject (id) {
	return document.getElementById(id);
}

function require (id, label, bool) {
	var obj 		= getObject(id);
	if ( ! obj) 	return true;
	//LOGD('require id:' + id + ' val:' + obj.value + ' force:' + bool);
	
	if ( obj.value == '' && bool) {
		notify(label + ' is required.', 'danger');
		obj.focus();
		return false;
	}
	return true;
}

/**
 * Save listener
 */
function save_onclick () {
	var type = getObject('ds-type').value;
	var bool = require('port', 'Port', type == 'SOCKET') && require('fmt-ftr', 'Format Footer', type == 'SOCKET')
		// DB
		&& require('db_url', 'Database URL', type == 'DATABASE') && require('db_user', 'Database User', type == 'DATABASE') 
		&& require('db_pwd', 'Database Password', type == 'DATABASE') && require('db_flds', 'Database Fields', type == 'DATABASE')
		// SMTP(s)
		&& require('host', 'Host', type.indexOf('SMTP') != -1) && require('port', 'Port', type.indexOf('SMTP') != -1)
		&& require('from', 'From', type.indexOf('SMTP') != -1)
		// Twilio
		&& require('twilioAppId', 'Application Id', type == 'SMS_TWILIO') && require('twilioToken', 'Token', type == 'SMS_TWILIO') 
		&& require('twilioFrom', 'Sender (From)', type == 'SMS_TWILIO') && require('twilioTo', 'Destination (To)', type == 'SMS_TWILIO')
		// FILESYSTEM
		&& require('fsPath', 'File system path', type == 'FILESYSTEM')
		// PROMETHEUS
		&& require('pmUrl', 'Prometheus URL', type == 'PROMETHEUS')
		
		;
	LOGD('save click return: ' + bool);
	return bool;
}

/**
 * Add a listener
 */
function add () {
	setVisibility('tblListener', true, 'table');
	setVisibility('btnSave', true);
}


</script>
</head>
<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="<%=JSPHandlerDataSource.getTitle()%>" name="pageTitle"/>
		<jsp:param value="<%=crumbLbls%>" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
            <div class="container-fluid">
 
				<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../\") %>">
					<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
					<jsp:param value="<%=statusType%>" name="statusType"/>
				</jsp:include>
                <!-- /.row -->

                <div class="row">
                    <div class="col-lg-12">
                    
						<!-- CONTENTS -->
						<!-- <h2>Available</h2> -->
						
						<table class="table table-bordered table-hover uk-table">
							<thead>
								<tr>
									<th>Name</th>
									<th>Description</th>
									<th>Port</th>
									<th>Status</th>
									<th>Action</th>
								</tr>
							</thead>
							<tbody>
							<% for ( IDataSource listener : listeners) { %>
							<tr>
								<td>
									<!-- data-toggle="tooltip" -->
									<label  title="<%=listener.getFormat() != null ? listener.getFormat().toString() : listener.getName()%>"><a href="ds.jsp?action=edit&li=<%=listener.getName()%>&mode=<%=mode%>&id=<%=id%>"><%=listener.getName()%></a></label>
								</td>
								<td><%=listener.getDescription()%></td>
								<td><%=listener.getPort()%></td>
								<td><%=listener.getStatus().getStatus() == Status.SERVICE_ERROR ? "<font color=red>" + listener.getStatus() + "</font>" : listener.getStatus()%></td>
								<td>
									<button onclick="del('<%=listener.getName()%>')">X</button>&nbsp;&nbsp;&nbsp;
									<%
										String lbl = listener.getStatus().getStatus() == Status.ON_LINE ? "Stop" : "Start";
									%>
									<a href="ds.jsp?action=<%=lbl.toLowerCase()%>&name=<%=listener.getName()%>&mode=<%=mode%>&id=<%=id%>"><%=lbl%></a>
								</td>
							</tr>
							<% } %>
							</tbody>
						</table>
					
						<p>&nbsp;</p>
						
						<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
						
							<div class="panel-heading card-header md-card-toolbar">
								<h2 class="md-card-toolbar-heading-text"><%=ds != null ? ds.getName() : "Add Data Source" %></h2>
							</div>
							
							<div class="panel-body card-body card-padding md-card-content">
							
							<form id="frm1" action="ds.jsp" class="form-horizontal">
								<input type="hidden" name="action" value="add">
								<% if ( mode != null) { %>
								<input type="hidden" name="mode" value="<%=mode%>">
								<% } %>
								<% if ( id != null) { %>
								<input type="hidden" name="id" value="<%=id%>">
								<% } %>
								
								<% String[] dsValues = JSPHandlerDataSource.getDataSourceTypes().split(","); %>
																
								<div class="form-group uk-grid uk-width-1-1" id="rowDs">
									<label class="col-sm-2 control-label uk-width-1-4">Type</label>
									<div class="col-sm-10 uk-width-3-4">
										<select name="ds-type" id="ds-type" class="form-control" onchange="ds_type_change()" 
											data-toggle="tooltip" title="Data Source type." data-md-selectize>
											<% for ( String kv : dsValues) {
													String[] tmp 			= kv.split("=");
													if ( tmp.length != 2) 	continue;
											%>
											<option value="<%=tmp[0].trim()%>"><%=tmp[1].trim()%></option>
											<% } %>
										</select>
									</div>
								</div>

								<jsp:include page="../tiles/tile_ds_main.jsp">
									<jsp:param value="<%=dsName%>" name="ds"/>
									<jsp:param value="<%=(ds != null ? ds.toJSON() : null)%>" name="dsJSON"/>
									<jsp:param value="<%=serviceCls%>" name="serviceCls"/>
								</jsp:include>
								
								<jsp:include page="../tiles/tile_ds_db.jsp">
									<jsp:param value="<%=dsName%>" name="ds"/>
									<jsp:param value="<%=(ds != null ? ds.toJSON() : null)%>" name="dsJSON"/>
								</jsp:include>

								<jsp:include page="../tiles/tile_ds_smtp.jsp">
									<jsp:param value="<%=dsName%>" name="ds"/>
									<jsp:param value="<%=(ds != null ? ds.toJSON() : null)%>" name="dsJSON"/>
								</jsp:include>

								<jsp:include page="../tiles/tile_ds_twilio.jsp">
									<jsp:param value="<%=dsName%>" name="ds"/>
									<jsp:param value="<%=(ds != null ? ds.toJSON() : null)%>" name="dsJSON"/>
									<jsp:param value="<%=serviceCls%>" name="serviceCls"/>
								</jsp:include>
								
								<jsp:include page="../tiles/tile_ds_fs.jsp">
									<jsp:param value="<%=dsName%>" name="ds"/>
									<jsp:param value="<%=(ds != null ? ds.toJSON() : null)%>" name="dsJSON"/>
									<jsp:param value="<%=serviceCls%>" name="serviceCls"/>
								</jsp:include>

								<jsp:include page="../tiles/tile_ds_pm.jsp">
									<jsp:param value="<%=dsName%>" name="ds"/>
									<jsp:param value="<%=(ds != null ? ds.toJSON() : null)%>" name="dsJSON"/>
									<jsp:param value="<%=serviceCls%>" name="serviceCls"/>
								</jsp:include>
								
								<p>
									<button type="submit" id="btnSave" value="Save" onclick="return save_onclick ()" class="btn btn-primary btn-raised md-btn md-btn-primary">Save</button>
								</p>
							
							</form>
						
							</div>
							<!--  Panel body -->
						
						</div>
						<!--  Panel -->
						
						<!-- END CONTENTS -->
						
                   </div>
                </div>
                <!-- /.row -->

			</div>
			<!-- container-fluid -->
				
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../js/jquery.qtip.js"></script>
	
	<script type="text/javascript">
	
	$(document).ready(function() {
		// Update the DS type combo. Default: SOCKET
		var type = '#ds-type option[value="<%=( ds!= null ? ds.getType().name() : "SOCKET")%>"]';
		$(type).attr("selected", "selected");
		
		// show/hide UI widgets
		ds_type_change();
		
		<% if ( ds != null && ds.getType() == IDataSource.DataSourceType.DATABASE) {%>
		// Load tables (async)
		loadTables ();

		// select current table. Must wait a bit until loadTables completes
		setTimeout( function() { 
			var type = '#db_table option[value="<%=ds.toJSON().getJSONObject("params").getString("table")%>"]';
			LOGD('Select TBL: ' + type);
			$(type).attr("selected", "selected");
			
			// set fields
			$('#db_flds').val('<%=ds.getFormat().getFields()%>');
		}, 3000);
		<%} %>
		
		// QTip -tooltips:  A bit better. Grab elements with a title attribute that isn't blank.
		$('[title!=""]').qtip({
			style: {
				classes: 'qtip-shadow qtip-bootstrap'
			},
			position: {
		        at: 'bottom center'
		    } 
		}); 

	});

	</script>
	
</body>
</html>