<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.datasource.db.DBDataSink"%>
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

%>

<%
	String contextPath 			= getServletContext().getContextPath();
	String statusMessage		= "NULL";
	String statusType			= null;
	final String mode			= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id				= request.getParameter("id");		// service id

	// 6/5/2020 Plugin support
	IDataService service = null;
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (IDataService)p.getInstance();
	}
	else {
		service		= (IDataService)CloudServices.findService(ServiceType.DAEMON);
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
	/*	
	INVALIDATED = false;
	TABLE.clear().rows.add(array).draw();
	*/

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
		$('#' + key.replace(/ /g, '')).html( '<a href="diag-ds.jsp?name=' + key + '&mode=<%=mode%>&id=<%=id%>">' + TOTALS_BYDS[key] + ' record(s)</a>');
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
	var url = '<%=contextPath%>/Diagnostics?op=dump&mode=<%=mode%>&id=<%=id%>';
	/*
	if ( INTERVAL < 0) {
		return;
	} */
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
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../\") %>" name="basePath"/>
		<jsp:param value="../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Diagnostics" name="pageTitle"/>
		<jsp:param value="Home,Pages,Diagnostics" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
            <div class="container-fluid">
            
                <!-- Page Heading -->
                <!-- /.row -->

				<div id="message" style="margin: auto;"></div>
	
                <div class="row">
                    <div class="col-lg-12">
                    
						<!-- CONTENTS -->
						<div class="block-header">
							<h2>Data Stores</h2>
						</div>
						<table class="table table-bordered table-hover">
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
									<label data-toggle="tooltip" title="<%=listener.getFormat() != null ? listener.getFormat().toString() : listener.getName()%>"><%=listener.getName()%></label>
									
								</td>
								<td><%= listener instanceof DBDataSink ? ((DBDataSink)listener).getUrl() : listener.getPort()%></td> 
								<td>
									<%=listener.getStatus().getStatus() == Status.SERVICE_ERROR ? "<font color=red>" + listener.getStatus() + "</font>" : listener.getStatus()%>
									&nbsp;&nbsp;&nbsp;<span id="<%=listener.getName().replaceAll(" ", "")%>"></span>
								</td>
							</tr>
							<% } %>
							</tbody>
						</table>
						
						<p>&nbsp;</p>
						
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
         $().ready(function() {
        	/*
        	TABLE = $('#tblBuffer').DataTable({
        		//"dom": '<"top"iflp<"clear">>rt<"bottom"iflp<"clear">>',
        		"dom": '<"top"iflp<"clear">>rt',
        		stateSave: true ,
        		deferRender: true ,
        		

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
        		} ,

        		// Stringify JSON batch  col (2)
    			"columnDefs": [ {
    				"targets": 2,
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
        	*/
        });
 
     </script>

</body>
</html>