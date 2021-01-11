<%@page import="java.util.Arrays"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.core.IDataService.DataServiceType"%>
<%@page import="com.rts.core.IDataService"%>
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
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>

<%!
static void LOGD(String text) {
	System.out.println("[DIAG-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[DIAG-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[DIAG-ERR] " +text);
}

static String[] getDsFields(IDataSource ds) throws Exception {
	JSONObject params = ds.getParams();
	//LOGD("getDsFields params: " + params);
	
	String[] fields		= ds.getType() == IDataSource.DataSourceType.PROMETHEUS
			// get 1st element
			? params.getString("@fields").split(",")
					
			// use the format fields
			: ds.getFormat().getFields().split(",") ;
			
	return fields;
}

%>

<%
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)

	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;

%>

<%
	String contextPath 				= getServletContext().getContextPath();
	String dsName					= request.getParameter("name");
	String pageTitle				= "Diagnostics - " + dsName;
	//String statusMessage			= "NULL";
	//String statusType				= null;
	final String mode				= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id					= request.getParameter("id");		// service id
	
	// caller jsp (default - diagnostics [diag.jsp])
	final String parent				= request.getParameter("parent") != null ? request.getParameter("parent")  : "diag.jsp";	

	// 6/5/2020 Plugin support
	IDataService service = null;
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (IDataService)p.getInstance();
	}
	else {
		service			= (IDataService)CloudServices.findService(ServiceType.DAEMON);
	}
	final String pollServlet		= service.getDataServiceType() == DataServiceType.DATASOURCE ? "RealTimeStats" : "Diagnostics";
	
	// get updated listeners ( if saved)	
	IDataSource ds					= service.getDataSourceManager().getDataSource(dsName);
	
	//String[] fields				= ds.getFormat().getFields().split(",") ;  
	String[] fields					= getDsFields(ds);
	
	//LOGD("DS " + ds.getName() + " Fields:" + Arrays.toString(fields));
	
	String crumbsLabel				= "Home,Diagnostics," + dsName;
	String crumbsLink				= "../index.jsp," + parent + "?mode=" + mode + "&id=" + id + ",class_active";
	
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
	<jsp:param value="<%=pageTitle%>" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">

<script type="text/javascript">


/**
 * @param array : (eventQueue)J SON Array of all records for all data sources.  
    [
  	 {"batchDate":1451759155433,"batchData":[{"F1":"F1","VDN":"66147","ACDCALLS":"20","ABNCALLS":"6","INPROGRESS-ATAGENT":"6","AVG_ACD_TALK_TIME":"16:43","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"23: 3","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"15"},{"F1":"F1","VDN":"86724","ACDCALLS":"87","ABNCALLS":"9","INPROGRESS-ATAGENT":"0","AVG_ACD_TALK_TIME":"13: 8","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"49:49","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
  	,{"batchDate":1451762800814,"batchData":[{"F1":"F1","VDN":"46699","ACDCALLS":"1","ABNCALLS":"4","INPROGRESS-ATAGENT":"9","AVG_ACD_TALK_TIME":"45:16","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"21:21","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
  	,{"batchDate":1451762800814,"batchData":[],"listenerName":"CVDN Table"}
  	,{}
	]
 */
function displayEvents (array) {
	LOGD('Display events: Got ' + array.length + ' batches.');
	
	TABLE.clear(); 
	for ( var i = 0 ; i < array.length ; i++) {
		var batch 		= array[i].batchData;
		var dsName		= array[i].listenerName;
		
		if ( dsName != '<%=dsName%>') {
			continue;
		}
		
		// inject a date in every record :(
		for ( var j = 0 ; j < batch.length ; j++) {
			batch[j]["date"] = array[i].batchDate;
		}
		TABLE.rows.add(batch); 
	}
	TABLE.draw();
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
	var url = '<%=contextPath%>/<%=pollServlet%>?op=dump&name=<%=dsName%>';
	
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
		<jsp:param value="<%=pageTitle%>" name="pageTitle"/>
		<jsp:param value="<%=crumbsLabel%>" name="crumbLabels"/>
		<jsp:param value="<%=crumbsLink %>" name="crumbLinks"/>
	</jsp:include>
	
            <div class="container-fluid">
            
 
				<div id="message" style="margin: auto;"></div>
	
                <div class="row">
                    <div class="col-lg-12">
                    
						<!-- CONTENTS -->
						
						<!-- <h2><%=dsName%></h2> -->
						
						<table id="tblBuffer" class="display compact">
							<thead>
								<tr>
									<th>Date</th>
								<% for (String field : fields ) {%>
									<th><%=field%></th>
								<% } %>
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
	
    <script type="text/javascript">
        var TABLE;					// data table
        var INTERVAL	= 1;		// Poll intreval (used to start/stop the poll)
		
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
        				'<option value="500">500</option>'+
        				'<option value="1000">1000</option>'+
        				'</select>' 
        				+ '&nbsp;&nbsp;&nbsp; <input type="checkbox" onclick="toggle_refresh(this)" checked="true" /> Auto Refresh'
        		} ,
        			
        		"columns": [
					{ "data": "date" },
				<% for (String field : fields ) {%>
					{ "data": "<%=field%>" },
				<% } %>					
        		] 
        		,

        		"rowCallback": function ( row, data, index ) {
        			//LOGD("rowCallback idx=" + index);
        		} 

        		
        	}); 
        	
        	// disable DT DataTables warning: table id=tblBuffer - Requested unknown parameter 'container' for row 0, column 1
        	$.fn.dataTable.ext.errMode = 'none';
        	$('#tblBuffer').on( 'error.dt', function ( e, settings, techNote, message ) {
        	    console.log( message );
        	});
        	
        	//startPolling ();
        	
        });
 
         	
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