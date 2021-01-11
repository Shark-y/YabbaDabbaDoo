<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.services.ServiceStatus.Status"%>
<%@page import="com.rts.datasource.DataFormat"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="java.util.List"%>
<%@page import="com.rts.datasource.DataSourceManager"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>

<%!
static void LOGD(String text) {
	System.out.println("[TCP-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[TCP-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[TCP-ERR] " +text);
}
%>

<%
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)

	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;

	/* 8/3/2017 NO NEED FOR THIS ANYMORE Check for session expiration. Paset this snippet in all pages that require this check
	if ( title == null) {
		response.sendRedirect("../login.jsp?action=loginshow&r=conf/diag.jsp&m=Session expired.");
		return;
	} */
%>

<%
	String contextPath 				= getServletContext().getContextPath();

	String statusMessage			= "NULL";
	String statusType				= null;
	final String mode				= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id					= request.getParameter("id");		// service id

	// 6/5/2020 Plugin support
	RealTimeStatsService service = null;
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (RealTimeStatsService )p.getInstance();
	}
	else {
		service	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
	}
	// get updated listeners ( if saved)	
	List<IDataSource> listeners = service.getDataSourceManager().getDataSources();
	
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
	<jsp:param value="Diagnostics" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="../css/jquery.json-viewer.css">

<script type="text/javascript">

// Total records by data source
var TOTALS_BYDS = {};

/**
 * @param array : JSON Array of all records for all data sources.  
 	[
  	 {"batchDate":1451759155433,"batchData":[{"F1":"F1","VDN":"66147","ACDCALLS":"20","ABNCALLS":"6","INPROGRESS-ATAGENT":"6","AVG_ACD_TALK_TIME":"16:43","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"23: 3","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"15"},{"F1":"F1","VDN":"86724","ACDCALLS":"87","ABNCALLS":"9","INPROGRESS-ATAGENT":"0","AVG_ACD_TALK_TIME":"13: 8","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"49:49","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
  	,{"batchDate":1451762800814,"batchData":[{"F1":"F1","VDN":"46699","ACDCALLS":"1","ABNCALLS":"4","INPROGRESS-ATAGENT":"9","AVG_ACD_TALK_TIME":"45:16","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"21:21","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
  	,{"batchDate":1451762800814,"batchData":[],"listenerName":"CVDN Table"}
  	,{}
	]
 */
function displayEvents (array) {
	//LOGD('Display events: ' + array.length + ' records.');
	
	INVALIDATED = false;
	TABLE.clear().rows.add(array).draw();
	
	// collapse all JSON
	$('#tblBuffer').find('a.json-toggle').click();

	// alwyas reset before counting
	for ( var key in TOTALS_BYDS) {
		TOTALS_BYDS[key] = 0;
	}
	
	// count totals by data source
	for ( var i = 0 ; i < array.length ; i++) {
		var ds = array[i].listenerName;
		if ( ! TOTALS_BYDS[ds] ) {
			TOTALS_BYDS[ds] = 0;
		}
		TOTALS_BYDS[ds] += array[i].batchData.length;
	}
	LOGD('Displayed events: ' + array.length + ' batches. Record Totals: ' + JSON.stringify(TOTALS_BYDS));
	
	// display totals...
	for ( var key in TOTALS_BYDS) {
		$('#' + key.replace(/ /g, '')).html( '<a href="diag-ds.jsp?name=' + key + '&mode=<%=mode%>&id=<%=id%>&parent=diag1.jsp">' + TOTALS_BYDS[key] + ' record(s)</a>');
	}
}

/**
 * @param array JSON :[{"message":"Low free disk @ 43%","device":"DISK","type":"danger"}]
 */
var ALERTS_DISPLAYED = false;

function displayAlerts (array) {
	if ( ALERTS_DISPLAYED ) return;
	
	for ( var i = 0 ; i < array.length ; i++ ) {
		 var alert = array[i];
		 
		 // notify('Welcome back.', 'warning');
		 notify (alert.message, alert.type);
	}
	ALERTS_DISPLAYED = true;
}

</script>

<script type="text/javascript">

/**
 * Poll callbacks
 * response format is in JSON
 { status: 200, message: "OK",
	  eventQueue : [
		{"batchDate":1451759155433,"batchData":[{"F1":"F1","VDN":"66147","ACDCALLS":"20","ABNCALLS":"6","INPROGRESS-ATAGENT":"6","AVG_ACD_TALK_TIME":"16:43","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"23: 3","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"15"},{"F1":"F1","VDN":"86724","ACDCALLS":"87","ABNCALLS":"9","INPROGRESS-ATAGENT":"0","AVG_ACD_TALK_TIME":"13: 8","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"49:49","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
		,{"batchDate":1451762800814,"batchData":[{"F1":"F1","VDN":"46699","ACDCALLS":"1","ABNCALLS":"4","INPROGRESS-ATAGENT":"9","AVG_ACD_TALK_TIME":"45:16","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"21:21","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
		,{"batchDate":1451762800814,"batchData":[],"listenerName":"CVDN Table"}
		,{}
		]
	}
 */
function poll_cb_success(json) {
	//LOGD("Got Poll JSON: " + JSON.stringify(json));
	
	// consume json: {"status": 200, "intakeVolume": 100, "totalEventSize": 123, "usedMem": 123, "totalMem": 120}
	if (json.status != 200) {
		setErrorStatus(json.message);
		return;
	}
	/*
	if ( !json.eventQueue) {
		LOGE("Missing 'eventQueue' element in json.");
		return;
	} */
	clearStatusMessage();
	
	if ( json.eventQueue) 	displayEvents (json.eventQueue);
	if ( json.alerts) 		displayAlerts (json.alerts);
	
	// poll recurse
	setTimeout("poll()", 10000);
}

function poll_cb_error(jqXHR, textStatus) {
	LOGW("Poll failed with status: " + textStatus);
	setErrorStatus("Poll failed with status: " + textStatus);
	
	// recurse in case the long poll expired
	setTimeout("poll()", 10000);
}

/**
 * Start os METRICS polling interval
 */
function poll() {
	var url = '<%=contextPath%>/RealTimeStats?op=dump';
	
	if ( INTERVAL < 0) {
		return;
	}
	LOGD("Polling " + url);

	$.ajax({
		type : 'GET',
		url : url,
		// request response in json!
		headers : {
			"Accept" : "application/json; charset=utf-8",
		},
		cache : false
	})
	.done(poll_cb_success)
	.fail(poll_cb_error);
}

</script>

<script>

function clearStatusMessage() {
	document.getElementById('message').innerHTML = '';
}

function setStatusMessage(text, color) {
	document.getElementById('message').innerHTML = '<font color=' + color + ' size=5>' + text + '</font>';
}

function setOKStatus(text) {
	setStatusMessage(text, "blue");
}

function setErrorStatus(text) {
	setStatusMessage(text, "red");
}


function on_load ()  {
	// start polling...
	setTimeout("poll()", 1000);
}

window.onload = on_load;

</script>
</head>
<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="" name="pageTitle"/>
		<jsp:param value="Home,Pages,Diagnostics" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
            <div class="container-fluid">
            
                <!-- Page Heading -->
                <!-- 
                <div class="row">
                    <div class="col-lg-12">
                        <h1 class="page-header">
                            Diagnostics <small>Overview</small>
                        </h1>
                    </div>
                </div>
                -->
                <!-- /.row -->

				<div id="message" style="margin: auto;"></div>
	
                <div class="row">
                    <div class="col-lg-12">
                    
						<!-- CONTENTS -->
						<div class="block-header">
							<h2>Data Sources</h2>
						</div>
						<table class="table table-bordered table-hover uk-table">
							<thead>
								<tr>
									<th>Name</th>
									<th>Port</th>
									<th>Status</th>
								</tr>
							</thead>
							<tbody>
							<% for ( IDataSource listener : listeners) { %>
							<tr>
								<td>
									<label data-toggle="tooltip" title="<%=listener.getFormat().toString()%>"><%=listener.getName()%></label>
									&nbsp;&nbsp;&nbsp;<span id="<%=listener.getName().replaceAll(" ", "")%>"></span>
								</td>
								<td><%=listener.getPort()%></td>
								<td><%=listener.getStatus().getStatus() == Status.SERVICE_ERROR ? "<font color=red>" + listener.getStatus() + "</font>" : listener.getStatus()%> </td>
							</tr>
							<% } %>
							</tbody>
						</table>
						
						<p>&nbsp;</p>
						
						<div class="block-header">
							<h2>Batches <span id="queueSize"></span></h2> 
						</div>
						
						<table id="tblBuffer" class="display compact">
							<thead>
								<tr>
									<th>Data Source</th>
									<th>Date</th>
									<th>Batch</th>
								</tr>
							</thead>
							<tbody>
							</tbody>
						</table>
						
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

	<script type="text/javascript" src="../js/jquery.dataTables.js"></script>
	<script type="text/javascript" src="../js/notify.js"></script>
	<script type="text/javascript" src="../js/jquery.json-viewer.js"></script>
	
    <script type="text/javascript">
        var TABLE;					// data table
        var INTERVAL	= 1;		// Poll intreval (used to start/stop the poll)
        var AUTO_FORMAT = false;	// Used to switch the batch display format between JSON (true:slow) ans String (false:fast)
		var INVALIDATED = false;	// Used to prevent the Jquery JSON/Data tables display problems
        var CELL		= {row: -1, col: -1};	// Invalid cell
		
        $().ready(function() {
        	
        	TABLE = $('#tblBuffer').DataTable({
        		//"dom": '<"top"iflp<"clear">>rt<"bottom"iflp<"clear">>',
        		"dom": '<"top"iflp<"clear">>rt',
        		stateSave: true ,
        		deferRender: true ,
        		
        		/* Data gets truncated when I/O > 90%
        		"ajax": { 
        			url: '<%=contextPath%>/RealTimeStats?op=dump',
        			"dataSrc": "eventQueue",
        			async : true,
        			complete : function (jqXHR, status ) {
        				// status: ("success", "notmodified", "nocontent", "error", "timeout", "abort", or "parsererror").
        			}
        		}, */

        		"language": {
        			"lengthMenu": 'Display <select>'+
        				'<option value="10">10</option>'+
        				'<option value="20">20</option>'+
        				'<option value="100">100</option>'+
        				'<option value="-1">All</option>'+
        				'</select>' 
        				+ '&nbsp;&nbsp;&nbsp; <input type="checkbox" onclick="toggle_refresh(this)" checked="true" /> Auto Refresh'
        				// Too slow + '&nbsp;&nbsp;&nbsp; <input type="checkbox" onclick="toggle_format(this)"  /> Format JSON'
        		} ,
        			
        		"columns": [
					{ "data": "listenerName" },
					{ "data": "batchDate" },
					{ "data": "batchData" }
        		] ,
        		
        		"rowCallback": function ( row, data, index ) {
        			//LOGD("rowCallback idx=" + index);
        			/*
        			if ( index == CELL.row ) { // AUTO_FORMAT ) {
            			var json = data.batchData;
        				$('td:eq(2)', row).jsonViewer(json);
        				
        				if ( INVALIDATED) {
        					$('td:eq(2)', row).find('a.json-toggle').click();	
        				}
        			} */
        		} ,

        		// Stringify JSON batch  col (2)
    			"columnDefs": [ {
    				"targets": 2,
    				/*
    				"createdCell": function (td, cellData, rowData, row, col) {
    					if ( AUTO_FORMAT) {
    						$(td).jsonViewer(cellData);	
    					}
    				} , */
    				"render": function ( data, type, full, meta ) {
    					//LOGD('Render: type: ' + type + ' r:' + meta.row + ' c:' + meta.col + ' invalid:' + INVALIDATED);
    				    //return !AUTO_FORMAT ? JSON.stringify(data) : data;
    				    var hide	= (data instanceof Array);
    				    var count 	= hide ? data.length : 0;
    				    var value 	= hide ? count + (count > 1 ? ' records' : ' record') 
    				    				+ ' <a href="#" onclick="return formatCell(' + meta.row + ',' + meta.col + ')">View</a> '
    				    				: data;
    				    if ( type == 'display' && INVALIDATED ) {
    				    	return JSON.stringify(data);
    				    }
    				    return type == 'display' && !INVALIDATED  ? value : JSON.stringify(data) ;
    				}
    			} ,
    			// Java date (System.currentTimeMillis() to JS date - col (1)
    			{
    				"targets": 1,
    				"render": function ( data, type, full, meta ) {
    					var date = new Date(data);
    				    return date.toLocaleString();
    				}
    			}]
        		
        	}); 
        	
        	//startPolling ();
        	
        });
 
        function formatCell(row, col) {
        	LOGD('Formatcell: row=' + row + ' col=' + col);
        	INVALIDATED = true;
        	AUTO_FORMAT = true;
        	CELL.row = row;
        	CELL.col = col;
        	TABLE.cell(row, col).invalidate().draw();
        	return false;
        }
        
        function toggle_format (cb ) {
        	AUTO_FORMAT = cb.checked;
        	INVALIDATED = true;
        	TABLE.rows().invalidate().draw();
        } 
         	
        function toggle_refresh (cb ) {
        	LOGD("Toggle refresh Interval: " + INTERVAL + " Checked:" + cb.checked);
        	
        	if ( cb.checked) {
        		startPolling();
        	}
        	else {
        		stopPolling();
        	}
        } 
        
        function stopPolling () {
        	LOGD("Poll stop.");
        	INTERVAL = -1;
        }
        
        function startPolling () {
        	if ( INTERVAL > 0) {
        		LOGD("Start: Already polling.");
        		return;
        	}
        	/*
        	INTERVAL = setInterval(function () {
        		// https://datatables.net/reference/api/ajax.reload()
        		TABLE.ajax.reload( null, false ); // user paging is not reset on reload
        	}, 15000); */
        	INTERVAL = 1;
        	poll();
        	LOGD("Poll start Interval: " + INTERVAL);
        } 
        
     </script>

</body>
</html>