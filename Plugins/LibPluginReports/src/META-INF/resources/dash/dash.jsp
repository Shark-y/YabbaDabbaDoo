<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.rts.ui.Dashboard.Branding"%>
<%@page import="com.rts.service.ServiceUtils"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.rts.ui.Threshold"%>
<%@page import="com.rts.ui.ThresholdList"%>
<%@page import="com.rts.ui.Dashboard.WidgetType"%>
<%@page import="com.rts.ui.Dashboard.MetricType"%>
<%@page import="com.rts.ui.Dashboard.Metric"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="com.rts.ui.DashboardList"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.rts.ui.Dashboard"%>
<%@page import="com.cloud.console.ThemeManager"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
private static final String EMPTY = "NONE";

static void LOGD(String text) {
	System.out.println("[DASH-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[DASH-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[DASH-ERR] " +text);
}

static String formatMetrics (List<Metric> metrics) {
	StringBuffer buf = new StringBuffer();
	for ( Metric m : metrics) {
		buf.append(m.getName() + "(" + m.getDescription() + ")<br>");
	}
	return buf.toString();
}

static String formatArray (Object[] array, int selected) {
	StringBuffer html = new StringBuffer();
	
	for ( int i = 0 ; i < array.length ; i++) {
		String sel = i == selected ? " selected=\"selected\"" : "";
		html.append("<option value=\"" + array[i] + "\"" + sel + ">" + array[i] + "</option>");
	}
	return html.toString();
}
/*
static String formatWidgetTypes (int selected) {
	return formatArray(WidgetType.values(), selected);
} */

static String formatMetricTypes (int selected) {
	return formatArray(MetricType.values(), selected);
}

%>

<%
	/* Must include this in all pages to check for expiration :( Can't find a way to put this in a JSP tile */
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String pageTitle			= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	// session expired? login & back to HOME
	if ( pageTitle == null) {
		response.sendRedirect("../login.jsp?m=Session+expired.&r=.");
		return;
	}
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
%> 
 
<%
	boolean loggedIn 			= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	String statusMessage		= "NULL";
	String statusType			= null;
	final String mode			= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id				= request.getParameter("id");		// service id
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;

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
	
	final String action			= request.getParameter("action");
	Dashboard dashb				= null;		// When action = edit 
	
	LOGD("Action:" + action);
	
	if ( action != null ) {
		if ( action.equalsIgnoreCase("add")) {
			// title, listener, key, keyRange, met-name1, met-desc1, met-type1, met-widget1
			final String title		= request.getParameter("title");
			final String key 		= request.getParameter("key");
			final String keyRange 	= request.getParameter("keyRange");
			final String listener 	= request.getParameter("listener");
			final String heading 	= request.getParameter("heading");
			int count 				= 0;
			
			LOGD("Save L:" + listener + " K:" + key + " KR:" + keyRange + " heading:" + heading);
			ServiceUtils.dumpHTTPServletRequestParams("Save " + title + " " + key + "@" + listener, request);
			
			List<Metric> metrics = new ArrayList<Metric>();
			
			// read up to 20 metrics (20 should be more tha enough...)
			for ( int i = 1 ; i < 20 ; i ++) {
				final String metricName = request.getParameter("met-name" + i);
				final String metricDesc = request.getParameter("met-desc" + i);
				String metricType 		= request.getParameter("met-type" + i);
				String metricWidg 		= request.getParameter("met-widget" + i);
				String metricTh 		= request.getParameter("met-th" + i);
				
				if ( metricName == null || metricDesc == null || metricType == null || metricWidg == null) {
					LOGW("Save dash " + key + "@" + listener + ". Missing alert data 4 index: " + i);
					continue; // Don't break. Metric can be removed at will... break;
				}
				metricType 	= metricType.toUpperCase();
				metricWidg 	= metricWidg.toUpperCase();
				
				LOGD("Add Metric: " + metricName + " " + metricDesc + " " + metricType + " Th:" + metricTh);
				metrics.add(new Metric(metricName, metricDesc, MetricType.valueOf(metricType), WidgetType.valueOf(metricWidg))); 
			}
			
			// branding: bgCol required, logo - optional
			String bgCol 	= request.getParameter("brand-bgcol");
			String logo		= request.getParameter("brand-logo");
			Branding brand	= null;
			
			if ( bgCol != null) {
				brand = new Branding(bgCol, logo != null && !logo.equals("None") ? logo : null);	
			}
			try {
				if ( metrics.size() == 0) {
					throw new Exception("Failed to save dashboard. One or more metrics are required.");
				}
				if ( title == null )	throw new Exception("Dashboard title is required.");
				
				Dashboard db 	= service.addDashboard(title, listener, key, keyRange, (heading != null && !heading.equals(EMPTY) ? heading : null), metrics, brand);
				
				// validate dash
				String result	= service.validate(db, true);
				if ( result != null ) {
					throw new Exception(result);
				}
				service.saveDashboardList();
			}
			catch (Exception e) {
				statusMessage 	= e.getMessage();
				statusType		= "ERROR";
			}
		}
		else if ( action.equals("del")) {
			// Dash title
			final String name 	= request.getParameter("name");
			
			if ( !service.removeDashboard(name) ) {
				statusMessage 	= "Failed to delete " + name;
				statusType		= "ERROR";
			}
			else {
				statusMessage 	= "Removed " + name + ". <a href=\"dash.jsp?action=save&id=" + id + "&mode=" + mode + "\">Save required.</a>";
				statusType		= "WARN";
			}
		}
		else if ( action.equals("save")) {
			service.saveDashboardList();
		}
		else if ( action.equals("edit")) {
			// Dash title
			final String name 	= request.getParameter("name");
			
			dashb = name != null ? service.getDashboards().find(name) : null;
			
			if ( name == null ) {
				statusMessage 	= "Dashboard name is required for action " + action;
				statusType		= "ERROR";
			}
		}
		else if ( action.equals("status")) {
			statusMessage 	= request.getParameter("sm");
			statusType		= request.getParameter("st");
		}
		else {
			statusMessage 	= "Invalid action " + action;
			statusType		= "ERROR";
		}
	}

	List<IDataSource> listeners 		= service.getDataSourceManager().getDataSources();
	List<Dashboard> dashes				= service.getDashboards().getList();
	ThresholdList thresholds			= service.getThresholdList();
	
	LOGD("Status Msg: " + statusMessage + " Type:" + statusType);
%>
   
<!DOCTYPE html>
<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<!-- LibAdapterConsole: Bootstrap required styles & headers -->
<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
	<jsp:param value="../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Dashboard Editor" name="title"/>
</jsp:include>


<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">

<script type="text/javascript" src="../js/jscolor.js"></script>
<script type="text/javascript" src="ui.js"></script>

<script type="text/javascript">

// Fields for all listeners: Hash by listener name
// { 'Sample CSPLIT' : 'F1,F2,...', 'DataSource-2': 'F1,F2,...', ...}
var FIELDS = { 
<% for ( int i = 0 ; i < listeners.size() ; i++) {
		IDataSource ds = listeners.get(i); 
		//LOGD("DS=" + ds.toJSON());
%>
	'<%=ds.getName()%>' : '<%=((ds.getType() == IDataSource.DataSourceType.PROMETHEUS) ? ds.getParams().get("@fields")  : ds.getFormat().getFields()) %>'
	<% if (i < listeners.size() ) { out.print(","); } %>	
<%}%>
};

var METRICS = { 
<% for ( int i = 0 ; i < listeners.size() ; i++) {
		IDataSource ds = listeners.get(i); 
		//LOGD("DS=" + ds.toJSON());
%>
	'<%=ds.getName()%>' : '<%=ds.getFormat().getFields()%>'
	<% if (i < listeners.size() ) { out.print(","); } %>	
<%}%>
};

// all thresholds hash by thresh id
var THRESHOLDS	= {};	
<% for ( Threshold t : thresholds ) { %>
THRESHOLDS['<%=t.getId()%>'] = <%=t.getAlerts()%>;
<% } %>

/**
 * Fill the key combo with the listener fields when the listener option changes.
 * @param keyName Default key name used to select a value from the key combo
 */
function listener_onchange(keyName, headingName) {
	headingName = headingName || 'NONE';
	fill_combo ('key', keyName, true);
	fill_combo ('heading', headingName, false);
}

/**
 * Fill a field-names  combo (using the global FILEDS array above) from a data source combo
 * @param comboId Id of the field set combo
 * @param keyName Selected field name in comoboId or NONE if there is no selected field.
 * @param required If not true (optional) insert the word 'NONE' on top of the values.
 */
function fill_combo (comboId, keyName, required) {
	var combo 			= getElem ('listener');		// data source comobo
	var target1			= getElem (comboId);
	
	if ( combo.options.length == 0) {
		LOGE('Datasources combo is empty.');
		notify('Add a datasource first.', 'warning');
		return;
	}
	var listenerName 	= combo.options[combo.selectedIndex].value;
	var fields 			= FIELDS[listenerName]; 	// Combo options/values as a CSV string: F1,F2,...

	LOGD('Fill combo: len:' + combo.options.length + ' Selected Idx: ' + combo.selectedIndex + " Ds:" + listenerName);
	
	if ( listenerName == '') {
		notify('Add a datasource first.', 'warning');
		return;
	}
	
	// special case: optional combo: If keyName == 'NONE', add it to the fields array (@ the begining)
	// 5/24/2019 - Add NONE for optional fields
	if ( !required /*keyName && keyName == 'NONE'*/) {
		fields = 'NONE' /*keyName */ + ',' + fields;
	}
	//LOGD('combo=' + comboId + ' Selected=' + keyName + ' required=' + required);
	comboClear (target1);
	comboAddOptions (target1, fields, keyName);
}

/**
 * Format WidgetTypes as HTML SLECT OPTIONS.
 */
function formatWidgetTypes(selected) {
	var html = "";
	var sel;
	
	<% Object[] array =  WidgetType.values();
	for ( int i = 0 ; i < array.length ; i++) { %>
	sel 	= '<%=array[i]%>' == selected ? ' selected="selected"' : '';
	html 	+= '<option value="<%=array[i]%>"' + sel + '><%=array[i]%></option>';
	<% } %>
	
	return html;
}

/**
 * Build Threshold HTML SELCT OPTIONS.
 */
function formatThresholds(selected) {
	var html 	= "";
	var sel 	= "";
	
	<% for ( Threshold th : thresholds) { %>
	sel 	= '<%=th.getId()%>' == selected ? ' selected="selected"' : '';
	html 	+= '<option value="<%=th.getId()%>"' + sel + '><%=th.getId()%></option>';
	<%} %>
	return html;
}

/**
 * Fires whn the metric combo changes. Set the threshold column value
 */
function metric_onchange(cmb, idx) {
	var metric 		= comboGetValue(cmb.id);
	var listener 	= comboGetValue('listener');
	var thresId		= metric + '@' + listener;
	var trDivId		= 'met-th' + idx;
	LOGD('ThrId:' + thresId + ' HM[tid]:' + THRESHOLDS[thresId] + " Idx:" + idx);
	
	var html = '';
	if ( typeof(THRESHOLDS[thresId]) != 'undefined') {
		html = thresId;
	}
	else {
		//html = '<a href="thresh.jsp?metric=' + encodeURIComponent(metric) + '&listener=' + escape(listener) + '">Add</a>';
	}
	getElem(trDivId).innerHTML = html;	// set thresh col value
}

// the row # used when adding new rows...
var TOP_ROW = 1;

/**
 * Add a metric to the dash table. Fires when the Add (metric) btn is pressed.
 * @param name Metric name
 * @param desc Metric description
 * @param type Data type
 * @param widget Widget type.
 * @param thresh Threshold id: METRIC@LISTENER.
 */
function add_metric (name, desc, type, widget, thresh) {
	//LOGD("Adding metric with index key: " + TOP_ROW);
	 
	var table		= getElem ('tblMetrics');
	var row 		= table.insertRow(table.rows.length);
	var idx 		= TOP_ROW++; //table.rows.length - 1;
	
	// metric name: COMBO
	row.insertCell(0).innerHTML = '<select onchange="metric_onchange(this,' + idx + ')" name="met-name' + idx + '" id="met-name' + idx + '" class="form-control" data-toggle="tooltip" title="Metric name."></select>';
	
	// metric desc INPUT
	row.insertCell(1).innerHTML = '<input name="met-desc' + idx + '" id="met-desc' + idx + '" class="form-control" maxlength="80" required value="' + desc + '">';
	
	// metric type (COMBO): NUMBER, STRING
	row.insertCell(2).innerHTML = '<select name="met-type' + idx + '" id="met-type' + idx + '" class="form-control"><%=formatMetricTypes(0)%></select>';
	
	// widget type: COMBO: GAUGE,PANEL, AREA_CHART
	row.insertCell(3).innerHTML = '<select name="met-widget' + idx + '" id="met-widget' + idx + '" class="form-control">' + formatWidgetTypes(widget) + '</select>';
	
	// Threshold ID
	//row.insertCell(4).innerHTML = '<select name="met-th' + idx + '" id="met-th' + idx + '" class="form-control">' + formatThresholds(thresh) + '</select>';
	//row.insertCell(4).innerHTML = '<div name="met-th' + idx + '" id="met-th' + idx + '">' + (thresh ? thresh : '') + '</div>';
	row.insertCell(4).innerHTML = '<div>' + (thresh ? '<a href="thresh.jsp?action=edit&id=' + thresh + '">' + thresh + '</a>' : '') 
		+ '<input type="hidden" name="met-th' + idx + '" id="met-th' + idx + '" value="' + (thresh ? thresh + '"' : '"') + ' class="form-control" readonly>'
		+ '</div>';
	
	// action
	row.insertCell(5).innerHTML = '<button onclick="tableDelRowByKey (\'tblMetrics\', 0, \'met-name' + idx + '\')">X</button>';
	
	
	// fill metric name combo vals
	var combo 			= getElem ('listener');
	var listenerName 	= combo.options[combo.selectedIndex].value;
	var fields 			= METRICS[listenerName]; //FIELDS[listenerName]; 
	var target1			= getElem ('met-name' + idx);

	comboAddOptions (target1, fields, name);
}

/**
 * Delete dashboard
 */
function del (name) {
	var r = confirm("Delete " + name + "?");
	
	if (r == true) {
		location = 'dash.jsp?action=del&name=' + escape(name) + '&mode=<%=mode%>&id=<%=id%>';
	}
}

/**
 * View/Open dashboard (short poll)
 * @param title dash name/title
 * @param listener Queue/ listener name
 * @param key Group By field name
 */
function dash_view (title, listener, key) {
	window.open('dash-view.jsp?name='+ listener + '&key=' + encodeURIComponent(key) + '&title=' + escape(title),'_blank');
}

/**
 * Long Poll view
 */
function dash_view_lp (title, listener, key) {
	window.open('dash-view-lp.jsp?name='+ listener + '&key=' + encodeURIComponent(key) + '&title=' + escape(title) + '&mode=<%=mode%>&id=<%=id%>','_blank'); 
}

function save_onclick () {
	return true; //false;	
}

function on_load ()  {
	<% if (dashb != null) { %>
	//var hasThresh;
	<% 	 for ( Metric m : dashb.getMetrics() ) {%>
	//hasThresh 	= typeof(THRESHOLDS['<%=m.getName()%>@<%=dashb.getListener()%>']) != 'undefined';
	add_metric ('<%=m.getName()%>'
			, '<%=m.getDescription()%>'
			, '<%=m.getType().name()%>'
			, '<%=m.getWidgetType().name()%>'
			//, (hasThresh ? '<%=m.getName()%>@<%=dashb.getListener()%>' : '<%--m.getThresholdId() != null ? m.getThresholdId() : ""--%>')
			, '<%=service.getThresholdId(m.getName(), dashb.getListener(), dashb.getTitle())%>'
			);
	<% 	 } %>
	// load vals
	listener_onchange('<%=dashb.getKey()%>', '<%=(dashb.getHeadingField() != null && !dashb.getHeadingField().isEmpty()  ? dashb.getHeadingField() : EMPTY)%>');
	<% } else { %>
	// load vals
	listener_onchange('', 'NONE');
	<% } %>
}

window.onload = on_load;

/**
 * Fires when the upload btn is pressed. Redirect to branding servlet for upload.
 */
function brand_upload () {
	var name 		= getElem('brand-file').value; 
	var frm 		= document.forms[0];
	var isImg		= false;
	
	if (name == '') {
		alert("Select a file.");
		return false;	
	}
	if (name.indexOf('.png') != -1 || name.indexOf('.gif') != -1 || name.indexOf('.jpg') != -1 ) {
		isImg = true;
	} 	
	if (!isImg) {
		alert("An image is required.");
		return false;	
	}
	frm.method 		= 'POST';					// required for FU
	frm.enctype		= 'multipart/form-data';	// required	
	frm.action 		= "../Branding";
	frm.submit();
}

</script>

</head>
<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=pageTitle%>" name="title"/>
		<jsp:param value="Dashboard Editor" name="pageTitle"/>
		<jsp:param value="Home,Pages,Dashboard Editor" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
	<div class="container-fluid">

			<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../\") %>">
				<jsp:param value="<%=statusMessage != null ? statusMessage : \"NULL\"%>" name="statusMsg"/>
				<jsp:param value="<%=statusType%>" name="statusType"/>
			</jsp:include>
               <!-- /.row -->

               <div class="row">
                   <div class="col-lg-12">

					<!-- CONTENTS -->
					
					<table id="table2" class="table">
						<thead>
								<tr>
									<th data-field="name" data-sortable="true">Name</th>
									<th data-field="id" data-sortable="true">Data Source</th>
									<th data-field="display" data-sortable="true">Display By</th>
									<th data-field="metrics">Metrics</th>
									<th data-field="actions">Action</th>
								</tr>
						</thead>
						<tbody>
							<% 
							int index = 0;
							for ( Dashboard dash : dashes) { 
								index++;
							%>
							<tr>
								<td>
									<a href="javascript:location='dash.jsp?action=edit&mode=<%=mode%>&id=<%=id%>&name=' + escape('<%=dash.getTitle()%>')"><%=dash.getTitle()%></a>
								</td>
								<td><a href="../conf/ds.jsp?action=edit&li=<%=dash.getListener()%>&mode=<%=mode%>&id=<%=id%>"><%=dash.getListener()%></a></td>
								<td><%=dash.getKey()%></td>
								<td>
									<!-- Collapsible metrics -->
									<div class="panel panel-collapse uk-accordion" data-uk-accordion="{ collapse: false }">
										<!-- Header  -->
										<div class="panel-heading uk-accordion-title" role="tab" id="heading<%=index%>">
											<div class="panel-title">
												<% if ( SkinTools.isCloudsTheme()) { %>
												<a class="collapsed" data-toggle="collapse" data-parent="#accordion" href="#collapse<%=index%>" aria-expanded="false" aria-controls="collapse<%=index%>">
                                        		<% } %>
                                        		<%=dash.getTitle()%> (<%=dash.getMetricsSize()%>)
                                        		<% if ( SkinTools.isCloudsTheme()) { %>
                                    			</a>
                                    			<% } %> 
                                    		</div>
                                    	</div>
                                    	<!-- Collapsible metrics -->
                                    	<div id="collapse<%=index%>" class="collapse uk-accordion-content" role="tabpanel" aria-labelledby="heading<%=index%>">
                                    		<div class="panel-body">								
												<%=formatMetrics(dash.getMetrics())%>
											</div>
										</div>
									</div>
								</td>
								<td>
									<a href="javascript:del('<%=dash.getTitle()%>')">X</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									<%
									String validity = service.validate(dash, false);
									
									if (  validity == null) { %>
									<!--  
									<a href="javascript:dash_view('<%=dash.getTitle()%>', '<%=dash.getListener()%>', '<%=dash.getKey()%>')">View</a>
									&nbsp;&nbsp;&nbsp;&nbsp;
									-->
									<a href="javascript:dash_view_lp('<%=dash.getTitle()%>', '<%=dash.getListener()%>', '<%=dash.getKey()%>')">View</a>
									<% } else { %>
									<font color="red"><%=validity%></font>
									<% } %>
								</td>
							</tr>
							<% } %>
						</tbody>
					</table>
					
			<% if ( loggedIn) { %>
			
			<p>&nbsp;</p>
				
			<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading card-header md-card-toolbar">		
						<h2 class="md-card-toolbar-heading-text"><%=dashb != null ? dashb.getTitle() : "Add Dashboard"%></h2>
				</div>
				
				<div class="panel-body card-body card-padding md-card-content">	
					<form action="dash.jsp" class="form-horizontal row-border">
				
					<div role="tabpanel" class="tab-container tab-default">
						<!-- class="nav-tabs" -->
						<ul role="tablist" class="tab-nav nav nav-tabs uk-tab" data-uk-tab="{connect:'#home'}">
						 	<li class="active"><a href="#home" aria-controls="home" role="tab" data-toggle="tab">Details</a></li>
						 	<li><a href="#metrics" aria-controls="metrics" role="tab" data-toggle="tab">Metrics</a></li>
						 	<li><a href="#branding" aria-controls="branding" role="tab" data-toggle="tab">Branding</a></li>
						</ul>
					
					<div class="tab-content">
					
						<!-- Details TAB -->
						<% if ( SkinTools.isAltAirTheme()) { %>
						<ul id="home" class="uk-switcher uk-margin">
							<li>
								<br/>
						<% } else { %>
						<div role="tabpanel" class="tab-pane active" id="home">
						<% } %>
							<input type="hidden" id="action" name="action" value="add">
							<input type="hidden" id="mode" name="mode" value="<%=mode != null ? mode : ""%>">
							<input type="hidden" id="id" name="id" value="<%=id != null ? id : ""%>">
							
							<div class="<%=SkinTools.cssFormGroupClass()%>">
								<label class="<%=SkinTools.cssFormGroupLabelClass() %>"><a href="../conf/ds.jsp?id=<%=id%>&mode=<%=mode%>">Data Source</a></label>
								<div class="<%=SkinTools.cssFormGroupContentClass() %>">
									<!-- data-md-selectize -->
									<select name="listener" id="listener" class="form-control" onchange="listener_onchange('')">
									<% for ( IDataSource tns : listeners) { %>
										<% String selected = dashb != null && dashb.getListener().equals(tns.getName()) ? " selected" : ""; %>
										<option value="<%=tns.getName()%>"<%=selected%>><%=tns.getName()%></option>
									<% } %>
									</select>
								</div>
							</div>
	
							<div class="<%=SkinTools.cssFormGroupClass()%>">
								<label class="<%=SkinTools.cssFormGroupLabelClass() %>">Name</label>
								<div class="<%=SkinTools.cssFormGroupContentClass() %>">
									<input name="title" id="title" class="form-control md-input"
										data-toggle="tooltip" title="Dashboard name (Alpha numeric only)"
										maxlength="80" required
										pattern="[A-Za-z0-9\-\\'=()/ ]+" 
										value="<%=dashb != null ? dashb.getTitle() : ""%>">
								</div>
							</div>
	
							<div class="<%=SkinTools.cssFormGroupClass() %>">
								<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Group By Field</label>
								<div class="<%=SkinTools.cssFormGroupContentClass() %>">
										<select name="key" id="key" class="form-control" 
											required data-toggle="tooltip" title="Field used to group the result-set by.">
										</select>
								</div>
							</div>
							<!-- 5/1/2019 [0-9,-]+ -->
							<div class="<%=SkinTools.cssFormGroupClass() %>">
								<label class="<%=SkinTools.cssFormGroupLabelClass() %>">Group By Range</label>
								<div class="<%=SkinTools.cssFormGroupContentClass() %>">
									<input name="keyRange" id="keyRange" class="form-control md-input"
										data-toggle="tooltip" title="Display key range (e.g numeric: 1-10 or alphabetic: Alice, Bob)"
										maxlength="128" pattern="[A-z0-9,\-\\'= ]+"
										value="<%=dashb != null && dashb.getKeyRange() != null && !dashb.getKeyRange().isEmpty() ? dashb.getKeyRange() : ""%>">
								</div>
							</div>
							<!-- 5/14/2019 Subheading -->
							<div class="<%=SkinTools.cssFormGroupClass() %>">
								<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Display Heading</label>
								<div class="<%=SkinTools.cssFormGroupContentClass() %>">
									<!-- data-md-selectize -->
									<select name="heading" id="heading" class="form-control" required data-toggle="tooltip" 
										title="Optional field to be displayed in the panel heading.">
									</select>
								</div>
							</div>
	  						
							<button type="submit" id="btnSave" value="Save" onclick="return save_onclick ()" class="btn btn-primary btn-raised md-btn md-btn-primary">Save</button>
					<% if ( SkinTools.isAltAirTheme()) { %>
						</li>
						<li>
							<br/>
					<% } else { %>	
					</div>
					<!-- END Details tab  -->
					
					<!-- Metrics TAB -->
					<div role="tabpanel" class="tab-pane" id="metrics">
					<% } %>
												
						<button onclick="return add_metric('','','','','')" class="btn btn-sm btn-success btn-raised md-btn md-btn-success">Add</button>
						
						<table id="tblMetrics" class="table table-bordered table-hover uk-table">
							<thead>
								<tr>
									<th>Name</th>
									<th>Description</th>
									<th>Type</th>
									<th>Widget</th>
									<th><a href="thresh.jsp?id=<%=id%>&mode=<%=mode%>">Threshold</a></th>
									<th>Action</th> 
								</tr>
							</thead>
							<tbody>
							</tbody>
						</table>
					<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
					<% } else { %>	
					</div>	<!-- END Metrics TAB -->
					
					<!-- Branding TAB  -->
					<div role="tabpanel" class="tab-pane" id="branding">
					<%} %>
						<table class="table">
							<tbody>
								<tr>
									<td>Backgroud Color</td>
									<td>
										<input id="brand-bgcol" name="brand-bgcol" 
											class="jscolor" type="text" 
											value="<%=dashb != null && dashb.getBranding() != null ? dashb.getBranding().getBgColor() : "#F5F5F5"%>">
									</td>
								</tr>
								<tr>
									<td>Logo</td>
									<td>
										<select name="brand-logo">
											<option value="None" >None</option>
											<%=ServiceUtils.brandingListLogosAsHTMLOptions(dashb) %>
										</select>
										&nbsp;&nbsp;&nbsp;
										
										<input id="brand-file" name="brand-file" type="file"> 
										<button id="brand-upload" class="btn btn-primary btn-raised md-btn md-btn-primary" onclick="return brand_upload()">Upload</button>
										
									</td>
								</tr>
							</tbody>
						</table> 
					<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					</ul>
					<% } else { %>	
					</div>
					<!-- END Branding TAB -->
					<%} %>
					
					</div>	
					<!-- END TAB Content -->
					
					</div>	
					<!-- END TAB Panel -->
					
					</form>
					
				</div>
				<!--  card body -->
				
			</div>
			<!--  card -->
			
			<% } %>

					<!-- END CONTENTS -->

                  </div>
               </div>
               <!-- /.row -->

			
	</div>
	<!-- container fluid -->
		
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
	</jsp:include>
	
	<script type="text/javascript" src="../js/jquery.dataTables.js"></script>
	
	<script type="text/javascript">
            
        $().ready(function() {
        	$('#table2').DataTable(); 
        });
            
    </script>
 
</body>
</html>