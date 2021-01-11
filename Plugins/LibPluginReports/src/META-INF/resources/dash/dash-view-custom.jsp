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

static String getSubKey(Dashboard dash) {
	if ( dash.hasPanels()) {
		// metric[0].name
		return dash.getMetrics(WidgetType.PANEL).get(0).getName();
	}
	return "";
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

	// Group by key label (optional field name)
	final String keylbl			= dash.getHeadingField() != null ?  dash.getHeadingField() : request.getParameter("keyLbl");			

	// group by key range value
	final String range			= request.getParameter("range") != null ?  request.getParameter("range") : dash.getKeyRange();
	final boolean merge			= dash.hasMarquees() && !dash.isMixed();
	final String subKey			= getSubKey(dash);
	
	LOGD("View DS:" + listener + " GBK:" + key + " Subkey:" + subKey + " Dash Name:" + dashName + " Dash:" + dash + " GroupByRange: " + range);
	
%>
   
<!DOCTYPE html>
<html>
<head>
<title><%=dash.getTitle() %></title>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<link rel="icon" sizes="192x192" href="../img/favicon.png">

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
	margin: 10px;
	margin-top: 5px;
}

h2 {
	color: gray;
	padding-left: 20px;
}

/* default marquee style */
.marquee {
  width: 95%;
  height: 40px;
  overflow: hidden;
  font-size: 30px;
  display: inline-block;
  margin-top: 20px;
  margin-bottom: 20px;
}

/* wraps a set of complex panel divs */
.complexPanelWrapper {
	/*width:150px; 
	display:inline-flex;
	margin-right:20px;
	vertical-align: top; */
	margin-left: 1px;
}

/* from bootstrap */
.panel {
  /* width: 160px; */
  margin-right: 20px;
  background-color: #fff;
  border: 1px solid transparent;
  border-radius: 4px;
  -webkit-box-shadow: 0 1px 1px rgba(0, 0, 0, .05);
          box-shadow: 0 1px 1px rgba(0, 0, 0, .05);
}
.medium {
    font-size: 20px;
}
.huge {
    font-size: 30px;
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
	window.location = 'dash-view-lp.jsp?name=' + listener + '&key=' + encodeURIComponent(key) + '&title=' + escape(name) ;
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
	var url 		= '<%=contextPath%>/Amq?op=init&name=<%=listener%>&windowId=' + windowId;
	
	LOGD("Init " + url);
	setOKStatus("Please wait.");
	
	$.get(url);
}

/**
 * Start os METRICS LONG polling interval
 */
function poll() {
	var url 		= '<%=contextPath%>/Amq?op=pop&name=<%=listener%>&windowId=' + windowId;
	
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

var CHARTS 			= {};			// all chart refs
var THRESHOLDS		= {};			// all thresholds
var MARQUEE_INITED	= false;		// Marque single call initialization
var SINGLE_WIDGET	= <%=merge%>;	// Marquee: merge all metrics in a single widget

// Dash JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
var DASHBOARD 		= <%=dash.toJSON()%>;

// All thresholds JSON ARRAY: [{"alerts":[{"weight":0,"level":0,"color":"5EFF46"},{"weight":0,"level":30,"color":"F7FF2F"},{"weight":0,"level":70,"color":"FF2C13"}],"metric":"ACDCALLS","listener":"CVDN Table"},...]
var THRESH_ARRAY 	= <%=service.getThresholdList().toJSON()%>;

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
		var marqueeId	= 'marquee-' 	+ 'ROOT'; //KEY;	// 1 Marquee for each key (all MARQUEE metrics)
		var SUBKEY		= batch["<%=subKey%>"];	// Sub key (AGENTID, etc...) used to build a compund key
		var COMPOUNDKEY = (SUBKEY) ? KEY + '-' + SUBKEY : KEY;
		
		// load gauge data from metrics (on every interaction - icluding its value)
		var GAUGES		= [];	// array of JSON objs
		var value;				// gauge value
		var json;				// gause json descriptor
		
		<% for ( Metric m : dash.getMetrics(WidgetType.GAUGE) ) { %>
		/*
		value 				= '<%=m.getType().name()%>' == 'NUMBER' ? parseInt(batch['<%=m.getName()%>']) : batch['<%=m.getName()%>'] ;
		json				= <%=m.toJSON().toString()%>;	// metric as JSON { name: '', description: '' ...}
		json.value			= value; 						// assign the value to JSON 
		json.thresholdId 	= '<%=(m.getName() + "@" + listener)%>'; // 5/9/2019 '<%=service.getThresholdId(m.getName(), listener, dashName)%>';
		
		// gauge JSON metric { name: 'Abc', description: '',  value: 10}
		GAUGES.push (json); */
		<% } %>
		
		// Note: NO spaces in KEY!
		var hdrId = typeof(KEY) != 'number' 
			? '<%=key%>' + KEY.replace(/ /g,'')
			: '<%=key%>' + KEY;
		
		// check key range: 5/1/2019 - Allow number, or string ranges
		var range = "<%=range%>";
		
		//LOGD('Loop [' + i + ']  Range check: Key:' + KEY + ' SubKey:' + SUBKEY + ' Range:' + range + ' Typeof KEY:' + typeof(KEY));
		
		if ( range != '' ) {
			if ( ! isInRange(range, KEY)) {
				//LOGW('<%=key%> value ' + KEY + ' not in range ' + range + '. Skip.');
				skipped++;
				continue;
			}
		}
		
		// Initialize all widgets by key (if not present)
		if ( !getElem(hdrId)  <%= dash.hasPanels() ? " || !getElem('panel_' + COMPOUNDKEY) " : ""%>) { 
			// init Header
			var lbl		= typeof(KEY) != 'number' ? KEY : '<%=key%> ' + KEY + (KEYLBL ? ' - ' + KEYLBL : '' );
			
			// Init main (group) panel w/ label HTML
			var html				= '';
			var gotMainPanel 		= getElem('mainPanel' + hdrId );
			var gotCollapsePanel 	= getElem('collapse' + hdrId );
			var drawMainPanel		= ( SINGLE_WIDGET && (i < 1)) ||  ( !gotMainPanel && !SINGLE_WIDGET);
			var drawCollapsePanel	= !SINGLE_WIDGET  && !gotCollapsePanel;

			//LOGD( 'Loop [' + i + "]  INIT KEY " + hdrId + " COMPOUNDKEY:" + COMPOUNDKEY + " SingleWidget:" + SINGLE_WIDGET + " DrawMainP: " + drawMainPanel); 

			if ( drawMainPanel ) {
				html	= '<div id="mainPanel' + hdrId  + '" class="mainPanel">';
			}
			if ( drawCollapsePanel ) {
				html 		+= '<h4 id="' + hdrId + '"><a data-toggle="collapse" data-parent="#accordion" href="#collapse' + hdrId + '">' + lbl + '</a></h4>'; 
				html		+= '<div id="collapse' + hdrId + '" class="panel-collapse collapse in">';
			}			
			<% if ( dash.hasGauges()) { %>
			/* create gauges div(s)
			for ( var j = 0 ; j < GAUGES.length ; j++) {
				chartId = 'gauge-chart-' + j + '-' + KEY;
				html 	+= '<div class="gaugeWrapper" id="' + chartId + '"></div>';
			} */
			<% } %>
			
			// Init panels...			
			<% if (dash.hasPanels()) {
			%>
				var panelWrapper = getElem('panelWrapper' + hdrId );
				html +=  !panelWrapper 
							? '<div id="panelWrapper' + hdrId +  '" class="complexPanelWrapper row">' + formatPanelComplex (DASHBOARD, batch, COMPOUNDKEY) + '</div>' 
							: formatPanelComplex (DASHBOARD, batch, COMPOUNDKEY);
			<% 
			   } 
			%>

			<% if ( dash.hasAreaCharts()) { %>
			// init charts html
			html 			+= formatAreaChart(areaId); 
			<% } %>

			<% if ( dash.hasMarquees()) { %>
			// init marquee html
			html 			+= formatMarquee(marqueeId); 
			<% } %>
			
			if ( drawCollapsePanel  ) {
				html 			+= '</div>';	// body collapseDiv
			}
			if (  drawMainPanel ) {
				html 			+= '</div>';	// header: mainPannel
			}
			
			<% if ( dash.hasPanels()) { %>
			if ( panelWrapper ) { 
				panelWrapper.innerHTML += html;
			}
			else { 
				//main.appendChild(div1);
				main.innerHTML += html;
			}
			<% } else { %>
			main.innerHTML += html; //main.appendChild(div1); 
			<% } %>
			
			// must init the gauges, charts, etc after the HTML is inserted into the DOM tree!
			
			<% if ( dash.hasGauges()) { %>
			/* Init GAUGES...
			for ( var j = 0 ; j < GAUGES.length ; j++) {
				var alerts			= THRESHOLDS [GAUGES[j].thresholdId]; // GAUGES[j].name + '@<%=listener%>']; 
				chartId 			= 'gauge-chart-' + j + '-' + KEY;
				CHARTS[chartId] 	= new JustGage({ id: chartId
					, title: GAUGES[j].description
					, value: GAUGES[j].value
					, max: 300
					// Thresholds
					, customSectors: alertsToGaugeSectors(chartId + '/' + GAUGES[j].description, alerts, 300)
					// Thinner with cool pointer
					, gaugeWidthScale: 0.1, pointer: true, pointerOptions: { toplength: 8, bottomlength: -20, bottomwidth: 6, color: '#8e8e93'}
				});
			} */
			<% } %>
			
			<% if ( dash.hasAreaCharts()) { %>
			// Init the Chart JS Area Chart: add metrics, options, etc. A chart id (areaId) must exist in the DOM tree.
			var ctx 		= document.getElementById('canvas-' + areaId).getContext("2d");
			CHARTS[areaId]	= new Chart(ctx, chartJSFormatOptions(DASHBOARD, areaId));
			
			chartJSDrawAreaChart (DASHBOARD, areaId, batch);
			<% } %>

			panelDrawComplex(DASHBOARD, batch, COMPOUNDKEY); 
			
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
				CHARTS[id].refresh(GAUGES[j].value);
			}

			<% if ( dash.hasAreaCharts()) { %>
			chartJSDrawAreaChart (DASHBOARD, areaId, batch);
			<% } %>
			
			<% if ( dash.hasPanels()) { %>
			panelDrawComplex(DASHBOARD, batch, COMPOUNDKEY) ; //KEY);
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

/* 
function formatPanelComplex (key) { 
	var panelId		= 'panel_' + key;
	var labelId		= 'lbl_' + key;
	var valueId		= 'val_' + key;
	var value1Id	= 'val1_' + key;
	var footerId 	= 'footer_' + key;
	
	//LOGD("Format panel(" + panelId + ') labelId(' + labelId + ') ValueId(' + valueId + ') footerId(' + footerId + ')');
	var html = "" //"<div class=\"col-md-3\">"
		+ "<div id=\"" + panelId + "\" class=\"panel\">"
		+ "<div class=\"panel-heading\">"
		//+ "<div class=\"row\">"
		+ "<div class=\"text-center\">"
		+ "<div id=\"" + labelId + "\">Empty</div>"
		+ "<div class=\"medium\" id=\"" + valueId + "\">V0</div>"
		+ "<div class=\"huge\" id=\"" + value1Id + "\">V1</div>"
		+ "<div id=\"" + footerId + "\">Footer</div>"
		+ "</div>" 
		+ "</div>" 
		//+ "</div>" 
		+ "</div>";
	return html;
}
*/

/**
 * Format Complex Panel HTML: a panel with 4 metrics fro the same key
 */
 function formatPanelComplex (dashboard, batch, key) { 
	// [{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},..]
	var metrics 	= dashGetMetricsbyType(dashboard, 'PANEL');
	
 	var panelId		= 'panel_' + key;
	//LOGD("Format panel(" + panelId + ') labelId(' + labelId + ') ValueId(' + valueId + ') footerId(' + footerId + ')');

	// header
	var html =  '<div id="' + panelId + '" class="panel col-sm-2">'
		+ '<div class="panel-heading">'
		+ '<div class="text-center">'; // style="width:130px;"

	// metrics
	for ( var j = 0 ; j < metrics.length ; j++) {
		// {"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"}
		var metric 	= metrics[j];
		var id		= key + '_' + metric.name
		var cls		= j == 1 ? 'class="medium"' : j == 2 ? 'class="huge"' : '';
		html 		+= '<div ' + cls + ' id="' + id + '"> ' + id + '</div>';
	}
	// footer
	html +=  "</div>" 
		+ "</div>" 
		+ "</div>";
	return html;
 }
 
/**
 * Draw a panel metric values.
 * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
 * @param batch JSON obj of metric, value pairs: {"VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7",...}
 * @param key Group By key: SPLIT, VDN, etc.
 */
function panelDrawComplex ( dashboard, batch, key) {
	 var value;		// metric value 
	 var desc;		// metric description
	 var alerts;	// JSON array [{"weight":0,"level":0,"color":"FFFFFF"}, ...
	 var color;		// HTML color

	var metrics 	= dashGetMetricsbyType(dashboard, 'PANEL');
	var dataSource 	= dashboard.listener;
	
	for ( var j = 0 ; j < metrics.length ; j++) {
		var metric 	= metrics[j];
		var trId	= metric.name + '@' + dataSource;	// Global threshold: METRIC@DATASOURCE
	 
	 	value 	= batch[metric.name];
	 	desc 	= metric.description;
	 	alerts	= THRESHOLDS[trId];
	 	color	= getAlertColor(alerts, value);
	 	
	 	LOGD("Draw Panels: Key: " + key + " Metric: " + metric.name + " Desc:" + desc + " Value: " + value + " Alerts: " + alerts + " Color:" + color);
	 	/*
	 	if (j == 0) getElem('lbl_' + key ).innerHTML = value; 		// set lnl, 1st metric
	 	if (j == 1) getElem('val_' + key ).innerHTML = value; 		// set value, 2nd metric
	 	if (j == 2) getElem('val1_' + key ).innerHTML = value; 		// value 1, 3rd metric
	 	if (j == 3) getElem('footer_' + key ).innerHTML = value; 	// set footer, 3rd metric
	 	*/
	 	// missing value?
	 	if ( !value ) {
	 		continue;
	 	}
	 	getElem(key + '_' + metric.name).innerHTML = value; // set value
	 	//getElem('lbl_' + key + '_' + metric.name).innerHTML = desc; // set description
	 	
	 	// set the color
	 	//var panel = getElem('panel_' + key + '_' + metric.name);
	 	var panel = getElem('panel_' + key);
	 	
	 	if ( color != '') {
	 		panel.style['background-color'] = '#'  + color;
	 		panel.style['color'] 			=  color != 'FFFFFF' ? '#ffffff' : '#000000';
	 	}
	 	else {
	 		// default: panel-primary
	 		panel.style['background-color'] = '#337ab7';
	 		panel.style['color'] 			= '#ffffff';
	 	} 
	} 
}

</script>

</head>
<body>
	<% if ( dash.getBranding() != null && dash.getBranding().getLogo() != null && !dash.getBranding().getLogo().isEmpty() ) { %>
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