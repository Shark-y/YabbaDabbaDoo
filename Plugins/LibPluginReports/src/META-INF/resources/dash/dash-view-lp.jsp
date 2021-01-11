<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.rts.ui.Threshold"%>
<%@page import="com.rts.ui.Dashboard.WidgetType"%>
<%@page import="com.rts.ui.Dashboard.Metric"%>
<%@page import="com.rts.ui.Dashboard"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
static void LOGD(String text) {
	System.out.println("[DASHVIEW-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[DASHVIEW-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[DASHVIEW-ERR] " +text);
}

%>
 
<%
	final String contextPath 	= getServletContext().getContextPath();
	final String listener		= request.getParameter("name");				// Queue (data source) name
	final String key			= request.getParameter("key");				// Group by key (field)
	final String dashName		= request.getParameter("title");			// Dash name/title
	
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;

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
		service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
	}
	Dashboard dash 				= service.getDashboards().find(dashName, listener, key);
	IDataSource ds				= service.getDataSourceManager().getDataSource(listener);

	// Group by key label (optional field name)
	final String keylbl			= dash.getHeadingField() != null ?  dash.getHeadingField() : request.getParameter("keyLbl");			

	// group by key range value
	final String range			= request.getParameter("range") != null ?  request.getParameter("range") : dash.getKeyRange();
	
	LOGD("View DS:" + listener + " GBK:" + key + " Dash Name:" + dashName + " Dash:" + dash + " GroupByRange: " + range);
	
	// special case: single marquee 
	if ( (dash.hasMarquees() || dash.hasPanels()) && !dash.isMixed() ) {
		LOGD("Single marquee detected. Redirect to dash-view-merged.jsp with QS:" + request.getQueryString());
		response.sendRedirect("dash-view-custom.jsp?" + request.getQueryString());
	}
%>
   
<!DOCTYPE html>
<html>
<head>
<title><%=dash.getTitle() %></title>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<!-- THIS PAGE REQUIRES THE BOOTSTRAP BLUE THEME -->

<!-- Bootstrap Core CSS -->
<link href="../skins/bootstrap/css/bootstrap-common/bootstrap.css" rel="stylesheet">

<link href="../skins/bootstrap/themes/bootstrap-blue/sb-admin.css" rel="stylesheet">

<!-- Custom Fonts -->
<link href="../skins/bootstrap/font-awesome/css/font-awesome.css" rel="stylesheet" type="text/css">

<link href="dash-view.css" rel="stylesheet" type="text/css">

<style type="text/css">
body {
	font-family: Arial;
	background-color: <%=dash.getBranding() != null ? dash.getBranding().getBgColor() : "#f5f5f5"%> ;
	margin: 40px;
	margin-top: 5px;
}

h2 {
	color: gray;
	padding-left: 20px;
}
</style>

<script type="text/javascript" src="ui.js"></script>
<script type="text/javascript" src="dash-view.js"></script>
<script type="text/javascript" src="../js/Chart.js"></script>

<script type="text/javascript">
/**
 * View/Open dashboard
 * @param name dash name/title
 * @param listener Queue/ listener name
 * @param key Group By field name
 */
function dash_view_lp (name, listener, key) {
	window.location = 'dash-view-lp.jsp?name=' + listener + '&key=' + encodeURIComponent(key) + '&title=' + escape(name) + '&mode=<%=mode%>&id=<%=id%>' ;
}

/**
 * Poll callbacks
 * response format is in JSON
 {"message":"OK","status":200
	,"batchDate":1451950416719
	,"batchData":[ 
	  {"F1":"F1","VDN":"74153","ACDCALLS":"68"
		,"ABNCALLS":"7","INPROGRESS-ATAGENT":"4","AVG_ACD_TALK_TIME":"15:25"
		,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"24:50","OLDESTCALL":":00"
		,"AVG_ANSWER_SPEED":"","ACTIVECALLS":"11"}
	,{"F1":"F1","VDN":"74154","ACDCALLS":"36"
		,"ABNCALLS":"6","INPROGRESS-ATAGENT":"1","AVG_ACD_TALK_TIME":"1:47"
		,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"30:54","OLDESTCALL":":00"
		,"AVG_ANSWER_SPEED":"","ACTIVECALLS":"62"}
	,{"F1":"F1","VDN":"74155","ACDCALLS":"1"
		,"ABNCALLS":"1","INPROGRESS-ATAGENT":"8","AVG_ACD_TALK_TIME":"31: 5"
		,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"8:26","OLDESTCALL":":00"
		,"AVG_ANSWER_SPEED":"","ACTIVECALLS":"34"}
	]
	,"listenerName":"CVDN Table"
	}
 */
function poll_cb_success(json) {
	//LOGD("Got Poll JSON: " + JSON.stringify(json));
	
	// consume json: {"status": 200, "intakeVolume": 100, "totalEventSize": 123, "usedMem": 123, "totalMem": 120}
	if (json.status != 200) {
		//setErrorStatus(json.message + ". <a href='dash-view-lp.jsp?name=<%=listener%>&key=<%=key%>'>Refresh</a>");
		setErrorStatus(json.message + ". <a href=\"javascript:dash_view_lp('<%=dashName%>', '<%=listener%>', '<%=key%>')\">Refresh</a>");
		return;
	}
	
	clearStatusMessage();
	
	if ( json.dash && (json.dash != '<%=dashName%>') ) {
		LOGW('Skipping batch for dashboard ' + json.dash + ' Required: <%=dashName%>');
		//setOKStatus("No events in queue.");
	}
	else {
		if (json.batchData) {
			displayEvents (json.batchData);
		}
		else {
			setOKStatus("No events in queue.");
		}
	}
	// LONG poll recurse
	setTimeout("poll()", 100); //5000);
}

 
function poll_cb_error(jqXHR, textStatus) {
	LOGW("Poll failed with status: " + textStatus);
	setErrorStatus("Poll failed with status: " + textStatus);
	
	// recurse in case the long poll expired
	setTimeout("poll()", 10000);
}

// GLOBAL WINDOW ID sent in every poll op.
var windowId 	= Math.floor(Math.random() * 100000);

/**
 * Send a dash shutdown message to the backend. 
 */
function shutdown() {
	var url 		= '<%=contextPath%>/Amq?op=shutdown&name=<%=listener%>&windowId=' + windowId;
	$.get(url);
}

/**
 * Send an Initalize message to the backend. 
 */
function init() {
	var url = '<%=contextPath%>/Amq?op=init&name=<%=listener%>&windowId=' + windowId;
	
	LOGD("Init " + url);
	setOKStatus("Please wait.");
	$.get(url);
}

/**
 * Start os METRICS LONG polling interval
 */
function poll() {
	var url 		= '<%=contextPath%>/Amq?op=pop&name=<%=listener%>&windowId=' + windowId + '&dash=<%=dashName%>';
	
	LOGD("Polling " + url);

	$.ajax({
		type : 'GET',
		url : url,
		// request response in json!
		headers : {
			"Accept" : "application/json; charset=utf-8"
		},
		cache : false
	})
	.done(poll_cb_success)
	.fail(poll_cb_error);
}
</script>

<script type="text/javascript">

function on_load ()  {
	init();
	
	// start polling...
	setTimeout("poll()", 1000);
}

window.onload = on_load;

window.onbeforeunload = function() {
	shutdown();
    return "";
};

</script>

<script type="text/javascript">

var CHARTS 			= {};		// all chart refs
var THRESHOLDS		= {};		// all thresholds
var MARQUEE_INITED	= false;	// Marque single call initialization

// Dash JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
var DASHBOARD 		= <%=dash.toJSON()%>;

// All thresholds JSON ARRAY: [{"alerts":[{"weight":0,"level":0,"color":"5EFF46"},{"weight":0,"level":30,"color":"F7FF2F"},{"weight":0,"level":70,"color":"FF2C13"}],"metric":"ACDCALLS","listener":"CVDN Table"},...]
var THRESH_ARRAY 	= <%=service.getThresholdList().toJSON()%>;

// Data source type for the dashboard
var DS_TYPE 		= '<%=ds.getType().name()%>';

// Normalize thresholds
// Format: THRESHOLDS['ABNCALLS@CVDN Table'] = [{"weight":0,"level":0,"color":"D9FFE0"}, ...]
for ( var i = 0 ; i < THRESH_ARRAY.length; i++) {
	THRESHOLDS[ THRESH_ARRAY[i].metric + '@' + THRESH_ARRAY[i].listener ] = THRESH_ARRAY[i].alerts;
}

/**
 * [ {"F1":"F1","VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7","INPROGRESS-ATAGENT":"4","AVG_ACD_TALK_TIME":"15:25" ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"24:50","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"11"}
 	, {"F1":"F1","VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7","INPROGRESS-ATAGENT":"4","AVG_ACD_TALK_TIME":"15:25" ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"24:50","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"11"}
 	]
 */
function displayEvents (array) {
	var main 		= getElem("divMain");
	var skipped 	= 0; 			// # of skipped records
	//LOGD('Display Batch size: ' + array.length);
	
	for ( var i = 0 ; i < array.length ; i++ ) {
		// {"F1":"F1","VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7","INPROGRESS-ATAGENT":"4","AVG_ACD_TALK_TIME":"15:25" ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"24:50","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"11"}
		var batch 	= array[i];
	
		if ( batch.length == 0) continue;
		
		// some batches may have no group by key
		if ( !batch["<%=key%>"]) {
			//LOGW('(Mising key <%=key%>. Ingnoring batch [' + i + '] = ' + JSON.stringify(batch));
			continue;
		}
		var KEY 		= batch["<%=key%>"];	// Group By key (SPLIT, VDN, etc...)
		var KEYLBL 		= batch["<%=keylbl%>"];	// Optional Group By key label field
		var chartId 	= 'chart-' + KEY;		// gauge chart for each key (all NUMBER/GAUGE metrics)
		var areaId		= 'area-chart-' + KEY;	// 1 Area chart for each key (all NUMBER/AREA_CHART metrics)
		var marqueeId	= 'marquee-' 	+ KEY;	// 1 Marquee for each key (all MARQUEE metrics)

		// load gauge data from metrics (on every interaction - icluding its value)
		var GAUGES		= [];	// array of JSON objs
		var value;				// gauge value
		var json;				// gause json descriptor
		
		<% for ( Metric m : dash.getMetrics(WidgetType.GAUGE) ) { %>
		value 				= '<%=m.getType().name()%>' == 'NUMBER' ? parseInt(batch['<%=m.getName()%>']) : batch['<%=m.getName()%>'] ;
		json				= <%=m.toJSON().toString()%>;	// metric as JSON { name: '', description: '' ...}
		json.value			= value; 						// assign the value to JSON 
		json.thresholdId 	= '<%=(m.getName() + "@" + listener)%>'; // 5/9/2019 '<%=service.getThresholdId(m.getName(), listener, dashName)%>';
		
		// gauge JSON metric { name: 'Abc', description: '',  value: 10}
		GAUGES.push (json);
		<% } %>
		
		// Note: NO spaces in KEY!
		var hdrId = typeof(KEY) != 'number' 
			? '<%=key%>' + KEY.replace(/ /g,'')
			: '<%=key%>' + KEY;
		
		// check key range: 5/1/2019 - Allow number, or string ranges
		var range = "<%=range%>";
		//LOGD('Display Range check: Key:' + KEY +  ' Range:' + range + ' Typeof KEY:' + typeof(KEY));
		
		if ( range != '' ) {
			if ( !isInRange(range, KEY) && (DS_TYPE != 'PROMETHEUS') ) {
				//LOGW('<%=key%> value ' + KEY + ' not in range ' + range + '. Skip.');
				skipped++;
				continue;
			}
		}
		
		// Initialize all widgets by key (if not present)
		if ( ! getElem(hdrId)) { 
			//LOGD("Init KEY " + hdrId ); 

			// init Header
			var lbl		= typeof(KEY) != 'number' ? KEY : '<%=key%> ' + KEY + (KEYLBL ? ' - ' + KEYLBL : '' );
			
			// Init main (group) panel w/ label HTML
			var html	= '<div class="mainPanel">';
			html 		+= '<h4 id="' + hdrId + '"><a data-toggle="collapse" data-parent="#accordion" href="#collapse' + hdrId + '">' + lbl + '</a></h4>'; 
			//html 		+= '<a id="' + hdrId + '" data-toggle="collapse" data-parent="#accordion" href="#collapse' + hdrId + '"><%=key%>: ' + KEY + '</a>';
			html		+= '<div id="collapse' + hdrId + '" class="panel-collapse collapse in">';
			
			// create gauges div(s)
			<% if ( dash.hasGauges()) { %>
			for ( var j = 0 ; j < GAUGES.length ; j++) {
				chartId = 'gauge-chart-' + j + '-' + KEY;
				html 	+= '<div class="gaugeWrapper" id="' + chartId + '"></div>';
			}
			<% } %>
			
			// Init panels...			
			<% if (dash.hasPanels()) { %>
			<%
				for ( Metric m : dash.getMetrics() ) { 
					if ( m.isPanelWidget() ) {
			%>
				html += '<div class="panelWrapper">' + formatPanel ("panel_" + KEY + "_<%=m.getName()%>", "val_" + KEY + "_<%=m.getName()%>", "lbl_" + KEY + "_<%=m.getName()%>") + '</div>';
			<% 
					}
				} %>
			<% } %>

			<% if ( dash.hasAreaCharts()) { %>
			// init charts html
			html 			+= formatAreaChart(areaId); 
			<% } %>

			<% if ( dash.hasMarquees()) { %>
			// init marquee html
			html 			+= formatMarquee(marqueeId); 
			<% } %>
			
			html 			+= '</div>';	// body
			html 			+= '</div>';	// header
			
			var div1		= document.createElement("DIV");
			div1.innerHTML 	= html;
			
			main.appendChild(div1); 

			// must init the gauges, charts, etc after the HTML is inserted into the DOM tree!
			
			<% if ( dash.hasGauges()) { %>
			// Init GAUGES...
			for ( var j = 0 ; j < GAUGES.length ; j++) {
				var alerts			= THRESHOLDS [GAUGES[j].thresholdId]; // GAUGES[j].name + '@<%=listener%>']; 
				chartId 			= 'gauge-chart-' + j + '-' + KEY;
				CHARTS[chartId] 	= new JustGage({ id: chartId
					, title: GAUGES[j].description
					, value: !Number.isNaN(GAUGES[j].value) ? GAUGES[j].value : 0
					, max: 300
					// Thresholds
					, customSectors: alertsToGaugeSectors(chartId + '/' + GAUGES[j].description, alerts, 300)
					// Thinner with cool pointer
					, gaugeWidthScale: 0.1, pointer: true, pointerOptions: { toplength: 8, bottomlength: -20, bottomwidth: 6, color: '#8e8e93'}
				});
			}
			<% } %>
			
			<% if ( dash.hasAreaCharts()) { %>
			// Init the Chart JS Area Chart: add metrics, options, etc. A chart id (areaId) must exist in the DOM tree.
			var ctx 		= document.getElementById('canvas-' + areaId).getContext("2d");
			CHARTS[areaId]	= new Chart(ctx, chartJSFormatOptions(DASHBOARD, areaId));
			
			chartJSDrawAreaChart (DASHBOARD, areaId, batch);
			<% } %>

			panelDraw(DASHBOARD, batch, KEY);
			
			// Init marquee: Add metrics, etc...
			<% if ( dash.hasMarquees()) { %>
			marqueeFormatOptions (DASHBOARD, marqueeId, hdrId); 
			<% } %>
		}
		else {
			// update
			//LOGD("Update KEY " + hdrId ); 

			// update gauges
			for ( var j = 0 ; j < GAUGES.length ; j++) {
				var id = 'gauge-chart-' + j + '-' + KEY;
				if ( ! Number.isNaN(GAUGES[j].value) ) {
					//LOGD('Update gauge ' + id + ' = ' + GAUGES[j].value );
					CHARTS[id].refresh(GAUGES[j].value);
				}
			}

			<% if ( dash.hasAreaCharts()) { %>
			chartJSDrawAreaChart (DASHBOARD, areaId, batch);
			<% } %>
			
			<% if ( dash.hasPanels()) { %>
			panelDraw(DASHBOARD, batch, KEY);
			<% } %>
			
			<% if ( dash.hasMarquees()) { %>
			marqueeDraw(DASHBOARD, marqueeId, batch, THRESHOLDS, hdrId);
			<% } %>
		}
	}	// End FOR
	
	LOGD('Total Records Consumed ' + array.length + ' Skipped: ' + skipped);
	
	if ( ! MARQUEE_INITED) {
		// This must be called once after all marquees have been initialized.
		$('.marquee').marquee();
		MARQUEE_INITED = true;
	}
}


</script>

</head>
<body>
	<% if ( dash.getBranding() != null && dash.getBranding().getLogo() != null && !dash.getBranding().getLogo().isEmpty()) { %>
	<img alt="Logo" src="../Branding?op=logo&dash=<%=dash.getTitle()%>&mode=<%=mode%>&id=<%=id%>">
	<% } %>
	
	<!-- SPACER -->	
	<div style="height: 15px;"></div>
	
	<!-- CONTENTS -->
	<center>
		<div id="message"></div>
	</center>

	<!-- style="border:2px solid black" -->
	<div id="divMain"></div>

		
    <!-- jQuery -->
    <script src="../js/jquery.js"></script>

    <!-- jQuery Marquee -->
	<script type='text/javascript' src='../js/jquery.marquee.js'></script>

    <!-- Bootstrap Core JavaScript -->
    <script src="../skins/bootstrap/js/bootstrap.js"></script>

	<!-- Common JS logging -->
	<script type="text/javascript" src="../js/log.js"></script>

	<script>
		$(document).ready(function(){
			// Note: Tooltips must be initialized with jQuery: select the specified element and call the tooltip() method.
			// $('[data-toggle="tooltip"]').tooltip();
		}); 
	</script>	

 	<!-- Required by Morris/Justgage -->
    <script src="<%=contextPath%>/js/plugins/morris/raphael.min.js"></script>
    
  	<script src="../js/justgage.js"></script>
  
  </body>
</html>