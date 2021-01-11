<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.jsp.AggregatesManager"%>
<%@page import="com.rts.jsp.FrameSetManager.Frame"%>
<%@page import="com.cloud.console.JSPLoggerTool"%>
<%@page import="java.io.File"%>
<%@page import="com.rts.jsp.FrameSetManager"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.rts.jsp.FrameSetManager.FrameSet"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%!
static void LOGD(String text) {
	System.out.println("[VIEW-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[VIEW-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[VIEW-ERR] " +text);
}
%>

<%
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	String statusMessage		= "NULL";
	String statusType			= null;
	final String action			= request.getParameter("action");

	final String mode			= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id				= request.getParameter("id");		// service id

	// 6/5/2020 Plugin support
	RealTimeStatsService service = null;
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (RealTimeStatsService )p.getInstance();
	}
	else {
		service = (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
	}
	
	FrameSetManager mgr			= FrameSetManager.getInstance();
	FrameSet fs					= null;
	
	if ( action != null ) {
		if ( action.equalsIgnoreCase("add")) {
			final String name	= request.getParameter("name");
			LOGD("Save name: " + name);
			
			// read up to 20 metrics (20 should be more tha enough...)
			FrameSet fs1 		= new FrameSet(name);
			for ( int i = 1 ; i < 10 ; i ++) {
				final String dashName 	= request.getParameter("met-name" + i);
				final String frameStyle = request.getParameter("met-style" + i);
				
				if ( dashName == null || frameStyle == null) {
					LOGW("Save " + name + ". Missing frame data 4 index: " + i);
					continue; // Don't break. Metric can be removed at will... break;
				}
				fs1.add(new FrameSetManager.Frame(dashName, frameStyle));
			}
			// PATH=C:\Users\vsilva\.cloud\CloudReports\Profiles\Default 
			// {"frameSets":[{"frames":[{"dash":"Call Metrics By VDN 1","style":"width:50%;height:50%"},{"dash":"Test C1AS Agent","style":"width:50%;height:50%"}],"name":"test"}]}
			// final String path = CloudServices.getNodeConfig().getDefaultProfileBasePath() + File.separator + FrameSetManager.DEFAULT_SAVE_FILE_NAME;
			mgr.add(fs1);
			LOGD("Save @ " + mgr.toJSON().toString());
			try{
				mgr.save(); //path);
			}
			catch (Exception e) {
				JSPLoggerTool.JSP_LOGE("view.jsp", "Save " + name, e);
				statusMessage 	= e.getMessage();
				statusType		= "ERROR";
			}
		}
		else if ( action.equals("edit")) {
			fs = mgr.find(request.getParameter("name"));
		}
	}
%>
    
<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
	<jsp:param value="../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Grid Views" name="title"/>
</jsp:include>

<script type="text/javascript" src="../js/notify.js"></script>
<script type="text/javascript" src="../dash/ui.js"></script>

<link href="../css/jquery.qtip.css" type="text/css" rel="stylesheet">

<script type="text/javascript">
// dashboards (JSON)
var DASHES 		= <%=service.getDashboards().toJSON()%>;
var AGGREGATES	= <%=AggregatesManager.getInstance().toJSON().getJSONArray("aggregates")%>;

//the row # used when adding new rows...
var TOP_ROW 	= 1;

/**
 * Extract a field from an array of objects.
 * @return a coma sp list of values: v1,v2,...
 */
function extractArrayField (ARRAY, key) {
	var comma 	= false;
	var csv 	= '';
	for (var i = 0; i < ARRAY.length; i++) {
		if ( comma ) csv += ',';
		csv += ARRAY[i][key];
		comma = true;
	}
	return csv;
}

/**
 * Add a metric to the dash table. Fires when the Add (metric) btn is pressed.
 * @param name Metric name
 * @param desc Metric description
 * @param type Data type
 * @param widget Widget type.
 * @param thresh Threshold id: METRIC@LISTENER.
 */
function add_metric (name, desc, type, widget, thresh) {
	var table		= getElem ('tblMetrics');
	var row 		= table.insertRow(table.rows.length);
	var idx 		= TOP_ROW++; //table.rows.length - 1;

	// metric name: COMBO - onchange="metric_onchange(this,' + idx + ')"
	row.insertCell(0).innerHTML = '<select  name="met-name' + idx + '" id="met-name' + idx + '" class="form-control" data-toggle="tooltip" title="Metric name."></select>';
	
	// metric desc INPUT
	row.insertCell(1).innerHTML = '<input name="met-style' + idx + '" id="met-style' + idx + '" placeholder="width:100%; height:100%" class="form-control" maxlength="80" required value="' + desc + '">';
	
	// action
	row.insertCell(2).innerHTML = '<button onclick="tableDelRowByKey (\'tblMetrics\', 0, \'met-name' + idx + '\')">X</button>';

	// Add dashes to the combo
	var fields 			= extractArrayField(DASHES, 'title');
	var aggregates		= extractArrayField(AGGREGATES, 'name');
	
	var target1			= getElem ('met-name' + idx);
	comboAddOptions (target1, fields + ',' + aggregates, name);
	
}

function save_onclick () {
	var table		= getElem ('tblMetrics');
	if (table.rows.length < 3 ) {
		 notify('At least 2 Dashboards are required.', 'danger');
		return false;
	}
	return true; 
}
/**
 * View/Open dashboard (short poll)
 * @param title dash name/title
 */
function dash_view (title) {
	window.open('../view/frm-view.jsp?id=<%=id%>&mode=<%=mode%>&name='+ escape(title),'_blank');
}

</script>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Grid Views" name="pageTitle"/>
		<jsp:param value="Home,Pages,Grid Views" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
	<div class="container-fluid">
			
			<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../\") %>">
				<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
				<jsp:param value="<%=statusType%>" name="statusType"/>
			</jsp:include>
            <!-- /.row -->
			<!--  CONTENTS -->

			<% if (  mgr.getFrameViews().size() > 0) { %>
            <div class="row">
				<div class="col-lg-12">
					<div class="panel panel-info" data-widget='{"draggable": "false"}'>
						<div class="panel-heading">
							<h2>Views</h2>
							<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
						</div>
						<div class="panel-body no-padding">                   
							<table id="table2" class="table table-striped">
								<thead>
									<tr class="info">
										<th data-field="name" data-sortable="true">Name</th>
										<th data-field="id" data-sortable="true">Frames</th>
										<th data-field="actions">Action</th>
									</tr>
								</thead>
								<tbody>
								<% for ( FrameSet view : mgr.getFrameViews()) { %>
									<tr>
										<td>
											<a href="javascript:location='views.jsp?action=edit&id=<%=id%>&mode=<%=mode%>&name=' + escape('<%=view.getName()%>')"><%=view.getName()%></a>
										</td>
										<td>
											<%=view.getFrames().toString()%>
										</td>
										<td>
										<a href="javascript:dash_view('<%=view.getName()%>')">View</a>
										</td>
									</tr>
								<% } %>
								</tbody>
							</table>
						</div>                   
					</div>
				</div>
			</div>			
			<% } %>
				
			<div class="panel panel-default card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading card-header">		
					<h2><%=fs != null ? fs.getName() : "Add Grid"%></h2>
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				
				<div class="panel-body card-body card-padding">	
				
					<form action="views.jsp" class="form-horizontal row-border">
						<input type="hidden" id="action" name="action" value="add">
						<input type="hidden" id="id" name="id" value="<%=id%>">
						<input type="hidden" id="mode" name="mode" value="<%=mode%>">
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Name</label>
							<div class="col-sm-9">
								<input name="name" id="name" class="form-control"
									data-toggle="tooltip" title="Frameset name (Alpha numeric only)"
									maxlength="80" required
									pattern="[A-Za-z0-9 ]+" 
									value="<%=fs != null ? fs.getName() : ""%>">
							</div>
						</div>

						<div class="col-md-12">
							<button onclick="return add_metric('','','','','')" class="btn btn-success btn-raised">Add</button>
							
							<table id="tblMetrics" class="table table-bordered table-hover">
								<thead>
									<tr>
										<th>Dashboard</th>
										<th>HTML Style</th>
										<th>Action</th> 
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>
						</div>

						<div class="col-sm-9">
							<button type="submit" id="btnSave" value="Save" onclick="return save_onclick ()" class="btn btn-primary btn-raised">Save</button>
						</div>
					
					</form>
					
				</div>
			</div>					
			<!-- END CONTENTS -->
	</div>
	<!-- container-fluid -->
				
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../js/jquery.qtip.js"></script>
	
	<script type="text/javascript">
	
	$(document).ready(function() {
		<% if ( fs != null) { %>
		<% 	 for ( Frame frm : fs.getFrames() ) { %>
		add_metric ('<%=frm.getDashboard()%>', '<%=frm.getStyle()%>');
		<% 	} %>
		<% } %>
	});
	
	</script>
	
</body>
</html>