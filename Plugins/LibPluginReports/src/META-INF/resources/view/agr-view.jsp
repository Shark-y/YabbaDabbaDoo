<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.jsp.AggregatesManager.Aggregate"%>
<%@page import="com.rts.jsp.AggregatesManager"%>
<%@page import="com.rts.datasource.DataSourceManager"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%!
/** Relative path of the page end section for a particular skin. This tile must be included @ the end </body> of all JSPs **/
public static final String TILE_PATH_PAGE_END_JS	= SkinTools.SKIN_PATH + "tiles/tile_page_end_js.jsp";

public static String buildPageEndTilePathJS(String prefix) {
	return prefix + TILE_PATH_PAGE_END_JS;
}

%>  
<%
	final String contextPath 	= getServletContext().getContextPath();
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	final String name			= request.getParameter("name");				// Aggregate name
	
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
	//DataSourceManager dsMgr		= service.getDataSourceManager();
	AggregatesManager mgr		= AggregatesManager.getInstance();
	Aggregate ag				= mgr.find(name);
	final String dsName			= ag.getDataSource();
	final String groupByMetric	= ag.getGroupByMetric();
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

<script type="text/javascript" src="../dash/ui.js"></script>
<script type="text/javascript" src="../js/notify.js"></script>
<script type="text/javascript" src="../js/poll.js"></script>

<style>
html, body {
	margin: 0;
	padding:0;
	border:0;
	width: 100%;
	height: 100%;
}
</style>

<script type="text/javascript">

//GLOBAL WINDOW ID sent in every poll op.
var windowId 	= Math.floor(Math.random() * 100000);

// {"groupByMetric":"NONE","dataSource":"CSPLIT Table","metrics":[{"metric":"ACD","label":"Total ACD"}],"description":"Totals by SLPIT","name":"n","type":"SUM"}
var AGGREGATOR = <%=ag.toJSON()%>;

function resetMetrics () {
	var metrics		= AGGREGATOR.metrics;
	for (var i = 0; i < metrics.length; i++) {
		metrics[i].value = 0;
	}
}

var TOTALS = {};

/**
 * @param josn Batch: { "batchDate": 1507919114573, "status": 200, "listenerName": "CSPLIT Table", "batchData": [{ROW1},{ROW2},...],"batchFormat": {BATCHFORMAT}}
 *  BATCHFORMAT =	{ "fields": "F1,F2,..." }
 *  ROW 		=	{ METRIC1: V1, METRIC2: V2,...}
 */
function poll_done( json) {
	// {"message":"Missing data source CSPLIT Table","status":500}
	if ( json.status > 300 ) {
		notify(json.message, 'danger');
		return;
	}
	// no data
	if ( !json.batchData) {
		return;
	}
	//LOGD("Got batch: " + JSON.stringify(json) );
	
	var GRP_METRIC	= '<%=groupByMetric%>';		// group by metric
	var data 		= json.batchData;			// batch array
	var metrics		= AGGREGATOR.metrics;		// desired metrics (array)

	resetMetrics();
	
	// init
	if ( GRP_METRIC != 'NONE') {
		tableRemoveColumns(1);
		
		for (var i = 0; i < data.length; i++) {
			var row 		= data[i];
			var col			= row[GRP_METRIC];
			//LOGD("** ADD COL=" + col);
			tableAddColumn(col);
			TOTALS [col] 	= {};
			for (var j = 0; j < metrics.length; j++) {
				var m 		= metrics[j];
				TOTALS [col] [m.metric] = 0;
			}
		}
	}
	
	for (var i = 0; i < data.length; i++) {
		var row = data[i];	// Json objec {m1:v1, m2:v2,...}
		
		if ( GRP_METRIC == 'NONE') {
			// compute 
			for (var j = 0; j < metrics.length; j++) {
				var m 		= metrics[j];
				m.value 	+= row[m.metric];
				//LOGD(m.metric + ' = ' + m.value);
				
				// display
				getElem (m.metric).innerHTML = m.value;
			}
		}
		else {
			for (var j = 0; j < metrics.length; j++) {
				var m 		= metrics[j];
				var value 	= row[m.metric];
				var col		= row[GRP_METRIC];
				
				//LOGD('split=' + col + ' metric=' + m.metric + ' val=' + value);
				TOTALS [ row[GRP_METRIC] ] [m.metric] += value;
			}
		}
	}
	//LOGD('Totals=' + JSON.stringify(TOTALS));
	// set vals
	for (var i = 0; i < data.length; i++) {
		var row = data[i];
		if ( GRP_METRIC != 'NONE') {
			for (var j = 0; j < metrics.length; j++) {
				var m 		= metrics[j];
				var id 		= row[GRP_METRIC].replace(' ','') + '-' + m.metric;
				var val		= TOTALS [row[GRP_METRIC]] [m.metric];
				//LOGD(id + ' = ' + val);
				getElem(id).innerHTML = val;
			}
		}
	}
}

function poll_fail( jqXHR, textStatus ) {
	LOGE("Fail: " + textStatus );
}

function init() {
	var GRP_METRIC	= '<%=groupByMetric%>';
	var table		= getElem ('tblMetrics');
	
	// table title
	getElem ('title').innerHTML = AGGREGATOR.description;
	
	// Create an empty <thead> element and add it to the table:
	var header 	= table.createTHead();
	var metrics	= AGGREGATOR.metrics;
	
	// Create an empty <tr> element and add it to the first position of <thead>:
	var row 		= header.insertRow(0);
	row.className 	= 'info';
	
	// Insert a new cell (<td>) at the first position of the "new" <tr> element:
	row.appendChild(document.createElement("th")).innerHTML = 'Metric'; //insertCell(0);
	
	if ( GRP_METRIC == 'NONE') {
		row.appendChild(document.createElement("th")).innerHTML = 'Value';
	}
	// cells
	for (var i = 0; i < metrics.length; i++) {
		// Create an empty <tr> element and add it to the 1st position of the table:
		metrics[i].value = 0;
		row = table.insertRow(i + 1);
		
		row.insertCell(0).innerHTML = metrics[i].label;
		if ( GRP_METRIC == 'NONE') {
			row.insertCell(1).innerHTML = metrics[i].value;
			row.cells[1].id 			= metrics[i].metric;	// set id to metric name
		}
	}
	//tableAddColumn('Skill 1');tableAddColumn('Skill 2');//tableAddColumn('foo3');
	//tableRemoveColumns (1);
}

function tableAddColumn(label) {
	var table	= getElem ('tblMetrics');
	var header 	= table.createTHead();
	header.rows[0].appendChild(document.createElement("th")).innerHTML = label;
	var metrics	= AGGREGATOR.metrics;
	
	//LOGD('rows=' + table.rows.length + ' cols=' + header.rows[0].cells.length);
	for (var i = 0; i < table.rows.length - 1; i++) {
		var id = label.replace(' ','') + '-' + metrics[i].metric;
		var cell = table.rows[i + 1].insertCell(header.rows[0].cells.length - 1);
		//LOGD('Cell [' + (i + 1) + ' , ' + (header.rows[0].cells.length -1) + '] id:' + id);
		cell.id = id;
	}
}

function tableRemoveColumns (from) {
	var table	= getElem ('tblMetrics');
	var header 	= table.createTHead();
	var row		= header.rows[0];
	var len 	= row.cells.length - from;
	for (var i = 0; i < len ; i++) {
		//LOGD("***REMOVE hdr col [" + i  + '] len:' + len);
		row.removeChild(row.childNodes[1]);
	}
	for (var i = 0; i < table.rows.length - 1 ; i++) {
		for (var j = 0; j < len; j++) {
			//LOGD("***REMOVE row [" + (i+1)  + '] len:' + len);
			table.rows[i + 1].removeChild(table.rows[i + 1].childNodes[1]);
		}
	}
}

/**
 * Set the table theme
 */
function theme (name) {
	//LOGD('Set theme: ' + name);
	getElem('panel').className = 'panel panel-' + name;
	getElem('tblMetrics').createTHead().rows[0].className = name;
}
</script>

</head>

<body>

	<!-- CONTENT -->
	<div id="panel" class="panel panel-info" data-widget='{"draggable": "false"}'>
		<div class="panel-controls dropdown">
			<button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons">add</span></button>
            <ul class="dropdown-menu" role="menu">
            	<li><a href="javascript:theme('info')">Theme - Cyan</a></li>
            	<li><a href="javascript:theme('primary')">Theme - Blue</a></li>
                <li><a href="javascript:theme('success')">Theme - Green</a></li>
                <li><a href="javascript:theme('danger')">Theme - Red</a></li>
                <!-- 
                <li class="divider"></li>
				<li><a href="">Separated link</a></li>
				-->
			</ul>
        </div> 
		<div class="panel-heading">
			<h2><span id="title"></span></h2>
			<!-- 
			<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
			<div class="options">
			</div> -->
		</div>
		<div class="panel-body no-padding">	
			<table id="tblMetrics" class="table table-striped">
				<tbody>	
				</tbody>
			</table>	
		</div>
	</div>
	
	<!-- END CONTENT -->
	
	<jsp:include page="<%=buildPageEndTilePathJS(\"../\") %>">
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
	
		// this will track the # of clients in the server.
		LOGD('Initializing datasource <%=dsName%> windowId:' + windowId );
		$.get( '<%=contextPath%>/Amq?op=init&name=<%=dsName%>&windowId=' + windowId );
		
		// start polling
		var cfg 	= {};
		cfg.url 	= '<%=contextPath%>/Amq?op=pop&name=<%=dsName%>&windowId=' + windowId;
		cfg.done 	= poll_done;
		cfg.fail 	= poll_fail;
		cfg.timeoutDone = 10000;
		cfg.abortOnFail = false;
		cfg.debug 	= false;
		
		LOGD('Polling w/ config ' + JSON.stringify(cfg));
		Poller (cfg).start();
		init();
	});
	
	window.onbeforeunload = function() {
		shutdown();
	    return "";
	};
	
	function shutdown() {
		// tell the server to stop tracking this client
		$.get( '<%=contextPath%>/Amq?op=shutdown&name=<%=dsName%>&windowId=' + windowId );
	}
	</script>
</body>
</html>