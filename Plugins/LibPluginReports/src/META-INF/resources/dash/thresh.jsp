<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.service.ServiceUtils"%>
<%@page import="com.rts.ui.ThresholdList.Alert"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="org.json.JSONException"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
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
static void LOGD(String text) {
	System.out.println("[THRESH-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[THRESH-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[THRESH-ERR] " +text);
}

static String formatAlerts (JSONArray alerts) throws JSONException {
	StringBuffer buf = new StringBuffer();
	for ( int i = 0 ; i < alerts.length(); i++) {
		JSONObject alert = alerts.getJSONObject(i);
		buf.append("<div style=\"background-color: #" + alert.get("color") + "\">"  + alert.get("color") + " L:" + alert.getInt("level") + " W:" + alert.getInt("weight") + "</div>\n");
	}
	return buf.toString();
}

%>

<%
	/* Must include this in all pages to check for expiration :( Can't find a way to put this in a JSP tile */
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	// session expired? login & back to HOME
	if ( title == null) {
		response.sendRedirect("../login.jsp?m=Session+expired.&r=.");
		return;
	}
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
%> 

<%
	String statusMessage		= "NULL";
	String statusType			= null;
	final String mode			= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String pid			= request.getParameter("id");		// service id

	// 6/5/2020 Plugin support
	RealTimeStatsService service = null;
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(pid);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (RealTimeStatsService )p.getInstance();
	}
	else {
		service = (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
	}
	
	final String action			= request.getParameter("action");
	String metric				= request.getParameter("metric");
	String listener				= request.getParameter("listener");
	String dashboard			= request.getParameter("dashboard");
	Threshold thresh			= null;		// When action = edit 
	
	if ( action != null ) {
		if ( action.equalsIgnoreCase("add")) {
			// Add/Save new threshold key(metric)
			metric 		= request.getParameter("key");
			int count 	= 0;
			
			//LOGD("Save Thresh L:" + listener + " M:" + metric);
			ServiceUtils.dumpHTTPServletRequestParams("Save Thresh " + metric + "@" + listener, request);
			
			Threshold th = new Threshold(metric, listener);
			
			try {
				// set the optional dash
				th.setDashboard(dashboard);
				
				// check for up to 10 alerts in the request params
				for ( int i = 1 ; i < 10 ; i ++) {
					final String level 	= request.getParameter("al-lev" + i);
					final String weight = request.getParameter("al-wei" + i);
					String color 		= request.getParameter("al-col" + i);
					String trigger 		= request.getParameter("al-tri" + i);
					
					if ( level == null || weight == null || color == null) {
						LOGW("Add thresh " + metric + "@" + listener + ". Missing alert data 4 index: " + i);
						continue; // Don't break, alerts can be removed. break;
					}
					
					LOGD("Metric Lev:" + level + " " + weight + " " + color + " Trigger:" + trigger);
					th.addAlert(Integer.valueOf(level), Integer.valueOf(weight), color, trigger);
				}
				
				if ( th.getAlertsSize() == 0) {
					throw new Exception("Failed to save threshold. One or more alerts are required.");
				}
				service.addThreshold(th);
				service.saveThresholdList();
			}
			catch (Exception e) {
				statusMessage 	= e.getMessage() != null ? e.getMessage() : e.toString();
				statusType		= "ERROR";
			}
		}
		else if ( action.equals("del")) {
			listener 	= request.getParameter("name");
			//final String metric 	= request.getParameter("metric");
			LOGD("Del " + listener + " M:" + metric);
			
			if ( !service.removeThreshold(listener, metric) ) {
				statusMessage 	= "Failed to delete " + metric + " @ " + listener;
				statusType		= "ERROR";
			}
			else {
				statusMessage 	= "Removed " + metric + " @ " + listener + ". <a href=\"thresh.jsp?id=" + pid + "&mode=" + mode + "&action=save\">Save required.</a>";
				statusType		= "WARN";
			} 
		}
		else if ( action.equals("save")) {
			service.saveThresholdList();
		}
		else if ( action.equals("edit")) {
			if ( metric != null && listener != null ) {
				thresh = service.getThresholdList().find(metric, listener, dashboard ); 
			}
			else {
				// try by id
				String id = request.getParameter("id");
				
				if ( id != null) {
					thresh = service.getThresholdList().find(id);
				}
				else {
					statusMessage 	= "Threshold is required for action " + action;
					statusType		= "ERROR";
				}
			}
		}
		else {
			statusMessage 	= "Invalid action " + action;
			statusType		= "ERROR";
		}
	}

	List<IDataSource> listeners 	= service.getDataSourceManager().getDataSources();

	ThresholdList thresholds		= service.getThresholdList();
	DashboardList dashboards		= service.getDashboards();
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
	<jsp:param value="Threshold Editor" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">

<script type="text/javascript" src="../js/jscolor.js"></script>

<script type="text/javascript" src="ui.js"></script>

<script type="text/javascript">

// Fields for all listeners: Hash by listener name
var FIELDS = { '<%=listeners.get(0).getName()%>' : '<%=listeners.get(0).getFormat().getFields()%>'
<% for ( int i = 1 ; i < listeners.size() ; i++) { %>
	, '<%=listeners.get(i).getName()%>' : '<%=listeners.get(i).getFormat().getFields()%>'
<%}%>
};

// All dashboards by Data source (listener)
var ALL_DASHES = {
	<% for (IDataSource ds : listeners) { %>
	'<%=ds.getName()%>' :'<%=ServiceUtils.listToCSV(dashboards.findNamesByDataSource(ds.getName()))%>',
	<% } %>
};

/**
 * Fill the key combo with the listener fields when the listener option changes.
 * @param keyName Default key name (metric) used to select a value from the key (metric) combo
 */
function listener_onchange(keyName) {
	var combo 			= getElem ('listener');		// Data source combo
	var target1			= getElem ('key');			// metric combo
	var listenerName 	= combo.options[combo.selectedIndex].value;
	var fields 			= FIELDS[listenerName]; 
	
	// clear & update metrics
	comboClear (target1);
	comboAddOptions (target1, fields, keyName);
	<% if ( thresh != null) { 
		JSONArray alerts = thresh.getAlerts();
		for ( int i = 0 ; i < alerts.length() ; i++) {
			JSONObject alert = alerts.getJSONObject(i);
	%>
	add_alert (<%=alert.getInt(Alert.KEY_LEVEL) %>, <%=alert.getInt(Alert.KEY_WEIGHT) %>, '<%=alert.getString(Alert.KEY_COLOR) %>','<%=alert.optString(Alert.KEY_TRIGGER) %>');
	<% } 
	} 
	%>
	// update dahboard list
	var dashCombo = getElem ('dashboard');
	var dashNames =  '<%=Threshold.TYPE_GLOBAL%>,' + ALL_DASHES[listenerName];
	
	comboClear (dashCombo);
	comboAddOptions (dashCombo, dashNames, '<%=thresh != null && thresh.getDashboard() != null ? thresh.getDashboard() : Threshold.TYPE_GLOBAL%>');
}

//the row # used when adding new rows...
var TOP_ROW = 1;

/**
 * Add a metric to the dash table. Fires when the Add (metric) btn is pressed.
 */
function add_alert (lev, weight, col, trigger) {
	var table	= getElem ('tblMetrics');
	var row 	= table.insertRow(table.rows.length);
	var idx 	= TOP_ROW++; //table.rows.length - 1;
	
	row.insertCell(0).innerHTML = '<input type="number" min="0" pattern="\d+" name="al-lev' + idx + '" id="al-lev' + idx + '" required data-toggle="tooltip" title="Low value that that will trigger the alert." class="form-control" maxlength="10" required value="' + lev + '">';
	row.insertCell(1).innerHTML = '<input type="number" min="0" pattern="\d+" name="al-wei' + idx + '" id="al-wei' + idx + '" required data-toggle="tooltip" title="Value used to choose this alert when multiple alerts are triggered." class="form-control" maxlength="10" value="' + weight + '">';
	row.insertCell(2).innerHTML = '<input name="al-col' + idx + '" id="al-col' + idx + '" class="form-control" maxlength="10" required value="' + col + '">';
	/* disabled 8/2/2017
	row.insertCell(3).innerHTML = '<select name="al-tri' + idx + '" id="al-tri' + idx + '" class="form-control">'
		+ '<option value="NONE">Disabled</option>'
		//+ '<option value="WINDOWFOCUS"' + (trigger == 'WINDOWFOCUS' ? 'selected' : '') + '>Window Focus</option>'
		+ '<option value="NOTIFICATION"' + (trigger == 'NOTIFICATION' ? 'selected' : '') + '>WebAPI Notification</option>'
		+ '</select>'; */
		
	row.insertCell(3).innerHTML = '<button onclick="tableDelRowByKey (\'tblMetrics\', 0, \'al-lev' + idx + '\')">X</button>';
		
	/* INITIALIZE THE NEW JSCOLOR INSTANCE HERE */
	var myPicker = new jscolor(getElem('al-col' + idx), {});
	myPicker.fromString(col);
}

/**
 * Delete dashboard
 */
function del (listener, metric) {
	var r = confirm("Delete " + metric + " @ " + listener +  "?");
	
	if (r == true) {
		location = 'thresh.jsp?id=<%=pid%>&mode=<%=mode%>&action=del&name=' + escape(listener) + '&metric=' + encodeURIComponent(metric);
	}
}

function save_onclick () {
	return true; //false;	
}

function on_load ()  {
	// load vals
	<%if ( listener != null) { %>
	listener_onchange('<%=metric%>');
	<% } else { %>
	listener_onchange('');
	<% } %>
}

window.onload = on_load;

</script>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Threshold Editor" name="pageTitle"/>
		<jsp:param value="Home,Dashboard Editor,Threshold Editor" name="crumbLabels"/>
		<jsp:param value="../index.jsp,dash.jsp,class_active" name="crumbLinks"/>
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
						<% if ( thresholds != null && thresholds.size() > 0 ) { %>
						<div class="fresh-table toolbar-color-red">
							<!-- table-bordered table-hover -->
							<table id="table2" class="table">
								<thead>
									<tr>
										<th data-field="metric" data-sortable="true">Metric</th>
										<th data-field="listener" data-sortable="true">Data Source</th>
										<th>Alerts</th>
										<th>Action</th>
									</tr>
								</thead>
								<tbody>
								<% for ( Threshold th : thresholds) { %>
								<tr>
									<td>
										<a href="javascript:location='thresh.jsp?id=<%=pid%>&mode=<%=mode%>&action=edit&metric=' + escape('<%=th.getMetric()%>') + '&listener=<%=th.getListener()%>' + '<%=th.getDashboard() != null ? "&dashboard=" + th.getDashboard() : "" %>'">
											<%=th.getMetric()%>
										</a>
										<%=th.getDashboard() != null ? " / " + th.getDashboard() : "" %>
									</td>
									<td><%=th.getListener()%></td>
									<td> 
										<%=formatAlerts(th.getAlerts())%>
									</td>
									<td>
										<a href="javascript:del('<%=th.getListener()%>', '<%=th.getMetric()%>')">Delete</a>
									</td>
								</tr>
								<% } %>
								</tbody>
							</table>
						</div>
						<% } %>
						
						<p>&nbsp;</p>
						
						<div class="panel panel-default card" data-widget='{"draggable": "false"}'>
							<div class="panel-heading card-header">
								<h2><%=thresh != null ? thresh.getId() : "Add Threshold" %> </h2>
							</div>
							<div class="panel-body card-body card-padding">
							
							<form action="thresh.jsp" class="form-horizontal">
								<input type="hidden" name="action" value="add">
								<input type="hidden" name="id" value="<%=pid%>">
								<input type="hidden" name="mode" value="<%=mode%>">
								
								<div class="form-group">
									<label class="col-sm-2 control-label">Metric</label>
									<div class="col-sm-8">
												<select name="key" id="key" class="form-control" required data-toggle="tooltip" title="Metric.">
												</select>
									</div>
								</div>
								
								<div class="form-group">
									<label class="col-sm-2 control-label">Data Source</label>
									<div class="col-sm-8">
												<select name="listener" id="listener" class="form-control" onchange="listener_onchange('');tableClean(getElem('tblMetrics'))">
												<% for ( IDataSource l : listeners) { %>
													<option value="<%=l.getName()%>" <%=listener != null && l.getName().equals(listener) ? "selected" : ""%>><%=l.getName()%></option>
												<% } %>
												</select>
									</div>
								</div>
	
								<div class="form-group">
									<label class="col-sm-2 control-label">Dashboard</label>
									<div class="col-sm-8">
												<select name="dashboard" id="dashboard" class="form-control">
												</select>
									</div>
								</div>
	
								<div class="form-group">
									<div class="col-sm-10">
												<h3>Alerts &nbsp;&nbsp;&nbsp;<button onclick="return add_alert(0,'','','')" class="btn btn-sm btn-success btn-raised">Add</button></h3> 
												<p><b>Note:</b> A minimum of 2 alerts are required per threshold.</p>
												<table id="tblMetrics" class="table table-bordered table-hover">
													<thead>
														<tr>
															<th>Lower Boundary</th>
															<th>Weight</th>
															<th>HTML Color</th>
															<!-- Disabled 8/2/2017<th>Display Trigger</th> -->
															<th>Action</th>
														</tr>
													</thead>
													<tbody>
													</tbody>
												</table>
									</div>
								</div>
								
								<p>
									<button type="submit" id="btnSave" value="Save" onclick="return save_onclick ()" class="btn btn-primary btn-raised">Save</button>
								</p>
							</form>
						
						<!-- Panel body -->
						</div>
						
						<!-- Panel -->
						</div>
						
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
        //var $table = $('#table2');
            
        $().ready(function() {
        	$('#table2').DataTable();
            
        });
            
    </script>

	
</body>
</html>