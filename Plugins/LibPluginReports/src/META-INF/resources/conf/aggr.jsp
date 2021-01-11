<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="com.rts.datasource.DataSourceManager"%>
<%@page import="java.util.Objects"%>
<%@page import="java.io.IOException"%>
<%@page import="com.rts.jsp.AggregatesManager.Metric"%>
<%@page import="com.rts.jsp.AggregatesManager.Type"%>
<%@page import="com.rts.jsp.AggregatesManager.Aggregate"%>
<%@page import="com.rts.jsp.AggregatesManager"%>
<%@page import="com.cloud.console.JSPLoggerTool"%>
<%@page import="java.io.File"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%!
static void LOGD(String text) {
	System.out.println("[AGGR-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[AGGR-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[AGGR-ERR] " +text);
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
	DataSourceManager dsMgr		= service.getDataSourceManager();
	AggregatesManager mgr		= AggregatesManager.getInstance();
	Aggregate fs				= null;
	
	try {
		if ( action != null ) {
			if ( action.equalsIgnoreCase("add")) {
				final String name	= Objects.requireNonNull(request.getParameter("name"), "Aggregate name is required.");
				final String desc	= Objects.requireNonNull(request.getParameter("desc"), "Aggregate description is required.");
				final String dsName	= Objects.requireNonNull(request.getParameter("dsName"), "Aggregate data source name is required.");
				final String groupBy = Objects.requireNonNull(request.getParameter("groupBy"), "Aggregate GroupBy is required.");
				
				LOGD("Save name: " + name + " desc:" + desc + " DS:" + dsName + " GrpBy:" + groupBy);
				
				// read up to 20 metrics (20 should be more tha enough...)
				Aggregate fs1 		= new Aggregate(name, desc, dsName, Type.SUM, groupBy);
				
				for ( int i = 1 ; i < 10 ; i ++) {
					final String metName 	= request.getParameter("met-name" + i);
					final String metLbl 	= request.getParameter("met-lbl" + i);
					
					if ( metName == null || metLbl == null) {
						LOGW("Save " + name + ". Missing metric data for index: " + i);
						continue; // Don't break. Metric can be removed at will... break;
					}
					fs1.add(new AggregatesManager.Metric(metName, metLbl));
				}
				if ( fs1.getMetrics().size() == 0 ) {
					throw new IllegalStateException("At least 1 metric is required for " + name);
				}
				// PATH=C:\Users\vsilva\.cloud\CloudReports\Profiles\Default 
				mgr.add(fs1);
				LOGD("Save @ " + mgr.toJSON().toString());
				//try{
					mgr.save(); //path);
				/*}
				catch (Exception e) {
					JSPLoggerTool.JSP_LOGE("aggregates.jsp", "Save " + name, e);
					statusMessage 	= e.getMessage();
					statusType		= "ERROR";
				} */
			}
			else if ( action.equals("edit")) {
				fs = mgr.find(request.getParameter("name"));
			}
		}
	}
	catch (Exception e) {
		JSPLoggerTool.JSP_LOGE("aggregates.jsp", "Action: " + action , e);
		statusMessage 	= e.getMessage();
		statusType		= "ERROR";
	}
%>
    
<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
	<jsp:param value="../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Aggregates" name="title"/>
</jsp:include>

<script type="text/javascript" src="../js/notify.js"></script>
<script type="text/javascript" src="../dash/ui.js"></script>

<link href="../css/jquery.qtip.css" type="text/css" rel="stylesheet">

<script type="text/javascript">
// dashboards (JSON)
var DASHES 		= <%=service.getDashboards().toJSON()%>;
var DATASOURCES = <%=dsMgr.toJSON().getJSONArray("dataSources")%>;

//the row # used when adding new rows...
var TOP_ROW 	= 1;

function extractDashField (key) {
	var comma = false;
	var csv = '';
	for (var i = 0; i < DASHES.length; i++) {
		if ( comma ) csv += ',';
		csv += DASHES[i][key];
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
	row.insertCell(1).innerHTML = '<input name="met-lbl' + idx + '" id="met-lbl' + idx + '" class="form-control" maxlength="80" required value="' + desc + '">';
	
	// action
	row.insertCell(2).innerHTML = '<button onclick="tableDelRowByKey (\'tblMetrics\', 0, \'met-name' + idx + '\')">X</button>';

	var combo 			= getElem ('dsName');
	var listenerName 	= combo.options[combo.selectedIndex].value;	
	var fields 			= getMetrics (listenerName) ; //extractDashField('title'); 
	var target1			= getElem ('met-name' + idx);
	comboAddOptions (target1, fields, name);
	
}

function getMetrics (dsName) {
	for (var i = 0; i < DATASOURCES.length; i++) {
		var ds = DATASOURCES[i];
		if ( ds.name == dsName) {
			return ds.format.fields;
		}
	}
}
 
/**
 * Fill the metric combo with the listener fields when the listener option changes.
 * @param keyName Default key name used to select a value from the key combo
 */
function listener_onchange(keyName) {
	var combo 			= getElem ('dsName');
	var target1			= getElem ('groupBy');
	var listenerName 	= combo.options[combo.selectedIndex].value;
	var fields 			= 'NONE,' + getMetrics(listenerName); 
	
	comboClear (target1);
	comboAddOptions (target1, fields, keyName);
}

function save_onclick () {
	var table		= getElem ('tblMetrics');
	if (table.rows.length < 2 ) {
		notify('At least 1 metric is required.', 'danger');
		return false;
	} 
	return true; 
}

/**
 * View/Open dashboard (short poll)
 * @param title dash name/title
 */
function dash_view (title) {
	window.open('../view/agr-view.jsp?id=<%=id%>&mode=<%=mode%>&name='+ escape(title),'_blank');
}

</script>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Aggregates" name="pageTitle"/>
		<jsp:param value="Home,Pages,Aggregates" name="crumbLabels"/>
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
							<h2>Frame Views</h2>
							<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
						</div>
						<div class="panel-body no-padding">                   
							<table id="table2" class="table table-striped">
								<thead>
									<tr class="info">
										<th data-field="name" data-sortable="true">Name</th>
										<th data-field="id" data-sortable="true">Metrics</th>
										<th data-field="actions">Action</th>
									</tr>
								</thead>
								<tbody>
								<% for ( Aggregate view : mgr.getFrameViews()) { %>
									<tr>
										<td>
											<a href="javascript:location='aggr.jsp?id=<%=id%>&mode=<%=mode%>&action=edit&name=' + escape('<%=view.getName()%>')"><%=view.getName()%></a>
											( <%=view.getType()%>)
										</td>
										<td>
											<%=view.getMetrics().toString()%>
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
					<h2><%=fs != null ? fs.getName() : "Add Aggregate"%></h2>
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				
				<div class="panel-body card-body card-padding">	
				
					<form action="aggr.jsp" class="form-horizontal row-border">
						<input type="hidden" id="action" name="action" value="add">
						<input type="hidden" id="id" name="id" value="<%=id%>">
						<input type="hidden" id="mode" name="mode" value="<%=mode%>">
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Name</label>
							<div class="col-sm-9">
								<input name="name" id="name" class="form-control" data-toggle="tooltip" title="Aggregate name (Alpha numeric only)"
									maxlength="80" required	pattern="[A-Za-z0-9 ]+" 
									value="<%=fs != null ? fs.getName() : ""%>">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Description</label>
							<div class="col-sm-9">
								<input name="desc" id="desc" class="form-control" data-toggle="tooltip" 
									maxlength="80" required	pattern="[A-Za-z0-9 ]+" 
									value="<%=fs != null ? fs.getDescription() : ""%>">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Type</label>
							<div class="col-sm-9">
								<select name="type" id="type" class="form-control" data-toggle="tooltip" title="Aggregation type: SUM, AVG, etc.">
									<option value="<%=Type.SUM.name()%>"><%=Type.SUM.name()%></option>
								</select>
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Data Source</label>
							<div class="col-sm-9">
								<select name="dsName" id="dsName" class="form-control" onchange="listener_onchange('')">
								<% for ( IDataSource ds : dsMgr.getDataSources()) { %>
									<% String selected = fs != null && fs.getDataSource().equals(ds.getName()) ? " selected" : ""; %>
									<option value="<%=ds.getName()%>" <%=selected%>><%=ds.getName()%></option>
								<% } %>
								</select>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Group By Metric</label>
							<div class="col-sm-9">
								<select name="groupBy" id="groupBy" class="form-control" title="Select NONE aggregate all values or a metric to group calculations by.">
								</select>
							</div>
						</div>

						<div class="col-md-12">
							<button onclick="return add_metric('','','','','')" class="btn btn-success btn-raised">Add</button>
							
							<table id="tblMetrics" class="table table-bordered table-hover">
								<thead>
									<tr>
										<th>Metric</th>
										<th>Label</th>
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
		// QTip -tooltips:  A bit better. Grab elements with a title attribute that isn't blank.
		$('[title!=""]').qtip({
			style: {
				classes: 'qtip-shadow qtip-bootstrap'
			} ,
			position: {
		        at: 'bottom center'
		    }			
		}); 		
		<% if ( fs != null) { %>
		<% 	 for ( Metric frm : fs.getMetrics() ) { %>
		add_metric ('<%=frm.getName()%>', '<%=frm.getLabel()%>');
		<% 	} %>
		listener_onchange('<%=fs.getGroupByMetric()%>');	
		<% }  else { %>
		listener_onchange('NONE');
		<% } %>
		
	});
	
	</script>
	
</body>
</html>