<%@page import="com.cloud.console.performance.TomcatAutoTunner"%>
<%@page import="com.cloud.console.performance.Diagnostics.Status"%>
<%@page import="com.cloud.core.cron.LogCleanerService.CleanPolicy"%>
<%@page import="com.cloud.core.cron.LogCleanerService"%>
<%@page import="com.cloud.core.cron.ErrorNotificationSystem"%>
<%@page import="com.cloud.core.io.FileTool"%>
<%@page import="com.cloud.core.cron.AutoUpdateUtils"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.profiler.OSProfilerAlerts"%>
<%@page import="com.cloud.core.profiler.CloudNotificationService"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.c1as.profiler.HeapDumper"%>
<%@page import="com.cloud.core.types.CoreTypes"%>
<%@page import="java.io.FileFilter"%>
<%@page import="java.io.File"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.profiler.OSMetrics"%>
<%@page import="com.cloud.core.provider.IHTMLFragment"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.console.performance.Diagnostics"%>
<%@page import="com.cloud.console.performance.Diagnostics.Result"%>
<%@page import="java.util.Map"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.HashMap"%>

<%!
	static final String TAG = "[PERF]";

	static void LOGD(String text) {
		System.out.println(TAG + " " + text);
	}

	static void LOGE(String text) {
		System.err.println(TAG + " " + text);
	}
	
	String getSolution (Result res) {
		if (res.getName().equals(Diagnostics.KEY_LOGS) && res.getStatus() == Status.FAIL) {
			return "<a href=\"#\" onclick=\"return solutionCleanLogs()\">Clean</a>";
		}
		if ( (res.getName().equals(Diagnostics.KEY_CONTAINER_THR) || res.getName().equals(Diagnostics.KEY_CONTAINER_ALV)) && res.getStatus() == Status.FAIL) {
			return "<a href=\"#\" onclick=\"return solutionTuneContainer()\">Fix</a>";
		}
		return "";
	}
%>

<%
	final String contextPath 	= getServletContext().getContextPath();		// Begins with /CloudContact...
	final String theme		= (String)session.getAttribute("theme");
	final String title		= (String)session.getAttribute("title");
	
	//if ( theme == null) theme = request.getParameter("theme");
	//if ( theme == null) theme = ThemeManager.DEFAULT_THEME;
%>

<%
	String uiMessage	= request.getParameter("m");	// status messages
	String statusType	= "INFO";						// Type of status msg (INFO, WARN, ERROR)

	final JSONObject os = OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);
	final String tab	= request.getParameter("tab");
	
	// Check for session expiration or when a user tries to bypass the home page: redirect to login the back to HOME. 
	if (theme == null || title == null) {
		response.sendRedirect("../../login.jsp?action=loginshow&r=" + contextPath + "&m=Session+expired." );
		return;		
	}
	
	// get the node config. 
	NodeConfiguration cfgServer  = CloudServices.getNodeConfig(); 

	// heap dumps stored @ java.io.tmpdir
	File[] hprofDumps 	= FileTool.listFiles(CoreTypes.TMP_DIR, new String[] {"hprof"}, new String[] {"dump.*"});
	String action		= request.getParameter("action");
	
	if ( action != null) {
		LOGD("Action: " + action);
		try {
			String target = "";
			
			// alerts
			if ( action.equals("save")) {
				OSProfilerAlerts.save(request);
				target = "alerts.";
			}			
			// Notification system
			if ( action.equals("save_notif")) {
				AutoUpdateUtils.save(request);
				target = "notification values.";
			}
			// test proactive error notifications
			if ( action.equals("test_pen")) {
				target = ErrorNotificationSystem.checkForErrorsConsole();
			}
			// clean container log			
			if ( action.contains("cleanLogs")) {
				target = LogCleanerService.clean(CleanPolicy.REMOVE_OLDER_THAN_2WEEKS);
			}
			// tune container thread, access log valve
			if ( action.contains("tuneContainer") ) {
				target = TomcatAutoTunner.tuneUp();
			}
			uiMessage 	= action.startsWith("test_") ? ( target != null ? target : "Scan complete.")  : "Saved " + target;
			statusType	= "INFO";
		}
		catch (Exception e) {
			//e.printStackTrace();
			uiMessage 	= e.getMessage() != null ? e.getMessage() : HTTPServerTools.exceptionToString(e);
			statusType	= "ERROR";
		}
	}
	OSProfilerAlerts.reload();
%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html">

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
</jsp:include>


<!--  common styles -->
<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">

<script type='text/javascript' src='../../js/Chart.js'></script>
<script type='text/javascript' src='osp.js'></script>


<script type='text/javascript'>

	// poll end point
	var pollEndPoint = '<%=contextPath%>/OSPerformance';

	/**
	 * Poll callbacks
	 * response format is in JSON
	 */
	function poll_cb_success(json) {
		LOGD("Got Poll JSON: " + JSON.stringify(json));
		
		// consume json: {"status": 200, "intakeVolume": 100, "totalEventSize": 123, "usedMem": 123, "totalMem": 120}
		if (json.status != 200) {
			setErrorStatus(json.message);
			return;
		}
		
		clearStatusMessage();
		
		drawCharts(json);
		
		// poll recurse
		//setTimeout("poll()", 5000);
	}

	function poll_cb_error(jqXHR, textStatus) {
		LOGW("Poll failed with status: " + textStatus);
		setErrorStatus("Poll failed with status: " + textStatus);

		// recurse in case the long poll expired
		//setTimeout("poll()", 10000);
	}

	/**
	 * Start os METRICS polling interval
	 */
	function poll() {
		var url = pollEndPoint + '?op=os&vmpid=' + vmpid; 
		LOGD("Polling " + url);

		$.ajax({
			type : 'GET',
			url : url,
			// request response in json!
			headers : {
				"Accept" : "application/json; charset=utf-8",
			},
			cache : false
			//data: { rq_clientId: clientId, rq_windowId: windowId, rq_operation: 'poll' }
		})
		.done(poll_cb_success)
		.fail(poll_cb_error);
	}

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

	window.onload = function() {
		initCharts();

		//setOKStatus("Please wait...");

		// start polling...
		//setTimeout("poll()", 500);
	}
	
</script>

<script type='text/javascript'>

	var memChart;	// Memory
	
	// CPU load
	var cpuAreaChart;

	// Threads (daemon, peak)
	var threadChart;
	
	// CPU Gauge
	var cpuAvgGauge;
	
	// Container metrics
	var containerChart;

	// Container Global Requets processor
	var requestChart;

	function getRand(val) {
		return Math.ceil((val * Math.random()));
	}

	function initCharts() {
		memChart 		= new Chart(document.getElementById('memchart').getContext("2d"), configMem);
		cpuAreaChart 	= new Chart(document.getElementById('cpuchart').getContext("2d"), configCpu);
		threadChart 	= new Chart(document.getElementById('tchart').getContext("2d"), configTr);
		cpuAvgGauge		= new JustGage({ id: "cpuAvgGauge", value: 0, min: 0, max: 100, title: "Average % CPU Usage", label: '<%=os.getString("AvailableProcessors")%> Processor(s)' });
		containerChart	= null; // Lazy init - new Chart(document.getElementById('containerChart').getContext("2d"), configContainer);
		requestChart	= new Chart(document.getElementById('requestChart').getContext("2d"), configCGRP);
	}

	function toMB(bytes) {
		return Math.ceil(bytes / (1024 * 1024));
	}

	// Fires when the check for exceptions link is clicked
	function checkForExceptions() {
		document.getElementById('action').value = 'test_pen';
		document.forms[0].submit();
		return false;
	}
	
	// Fires when the clean log solution is clicked
	function solutionCleanLogs() {
		if ( confirm('Old logs will be removed')) {
			location = 'osp.jsp?action=test_cleanLogs';
		}
		return false;
	}
	
	// Fires when the fix container acceess log valve or threads is clicked.
	function solutionTuneContainer() {
		if ( confirm('Server configuration will be updated')) {
			location = 'osp.jsp?action=test_tuneContainer';
		}
		return false;
	}
</script>

</head>
<body>


	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Cloud Profiler" name="pageTitle"/>
		<jsp:param value="Home,Pages,Cloud Profiler" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>

	<!-- 
	<div class="container-fluid">
	-->
		
		<center>
			<div id="message"></div>
		</center>
		
		<!--  STATUS MESSAGE -->
		<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
			<jsp:param value="<%=uiMessage != null ? uiMessage : \"NULL\"%>" name="statusMsg"/>
			<jsp:param value="<%=statusType%>" name="statusType"/>
		</jsp:include>
		
		<ul class="nav nav-tabs">
			<li><a data-toggle="tab" href="#osTab">Details</a></li>
			<li id="tabCPU"><a data-toggle="tab" href="#cpuTab">CPU</a></li>
			<li id="tabThreads"><a data-toggle="tab" href="#threadsTab">Threads</a></li>
			<li id="tabHeap"><a data-toggle="tab" href="#heapTab">Heap</a></li>
			<li id="tabNet"><a data-toggle="tab" href="#netTab">NetStat</a></li>
			<li id="tabContainer"><a data-toggle="tab" href="#containerTab">Container</a></li>
			<% if ( !OSProfilerAlerts.hideAlertsTab()) { %>
			<li id="tabAlerts"><a data-toggle="tab" href="#alertsTab"><!-- Alerts -->Diagnostics</a></li>
			<% } %>
			<li id="tabMcast"><a data-toggle="tab" href="#mcastTab">Multicast Tunnel</a></li>
			<!-- 
			<li id="tabVMs"><a data-toggle="tab" href="#vmsTab">Virtual Machines</a></li>
			-->
		</ul>

	<div class="tab-content">
	<div id="osTab" class="tab-pane fade">
		&nbsp;
		<div class="panel panel-default card">
        	<div class="panel-heading card-header">
				<h3 class="panel-title">Details</h3>
			</div>
			<div class="panel-body card-body">
				<div class="form-horizontal">
					<div class="form-group">
						<label class="col-sm-4">OS Name</label>
						<div class="col-sm-8"><%=os.getString("Name")%>/<%=os.getString("Arch")%></div>
					</div>
					<div class="form-group">
						<label class="col-sm-4">Processors</label>
						<div class="col-sm-8"><%=os.getString("AvailableProcessors")%></div>
					</div>
					<div class="form-group">
						<label class="col-sm-4">Version</label>
						<div class="col-sm-8"><%=os.getString("Version")%></div>
					</div>
					<div class="form-group">
						<label class="col-sm-4">Total RAM (MB)</label>
						<div class="col-sm-8"><%=(Long.parseLong(os.getString("TotalPhysicalMemorySize"))/(1024*1024))%></div>
					</div>
				</div>			
			</div>
		</div>
		
	</div>
	<!-- OS TAB -->
	
	<!-- CPU -->
	<div id="cpuTab" class="tab-pane fade">
		<!-- disabled 4/30/2020 
		<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#modal2">Virtual Machines</button>
		-->
		<!-- Server metrics: IV & Mem Heap  width: 400px; height: 300px; -->
		<table id="tblCharts" style="width: 100%; table-layout: fixed; border: 0px">
			<tr>
				<td style="margin: auto;"><div id="cpuAvgGauge" style="width:300px; height:300px;margin: auto;"></div></td>
				<td style="width: 50%; height: 100%;"><canvas id="cpuchart"></canvas></td> 
			</tr>
			<tr>
				<td style="width: 50%; height: 100%;"><canvas id="memchart"></canvas></td> 
				<td style="width: 50%; height: 100%;"><canvas id="tchart"></canvas></td> 
			</tr>
		</table>
	
	</div>
	<!-- Heap TAB -->
	
	<!-- Threads TAB -->
	<div id="threadsTab" class="tab-pane fade">	
	
		<h3>Live Threads <small id="totals"></small> &nbsp;&nbsp;&nbsp; <!-- disabled 4/30/2020 <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#modal2">Virtual Machines</button> --> </h3>
		
		
		<table id="tblThreads" class="table" style="width: 100%">
			<thead>
				<tr>
					<th>Id</th><th>Name</th>
					<th>State</th>
					<th>CPU Time (ms)</th>
					<th>User Time (ms)</th>
				</tr>
			</thead>
			<tbody></tbody>
		</table>
	
	</div>
	<!-- threads tab -->

	<!--  HEAP -->
	<div id="heapTab" class="tab-pane fade">
		<P>
			<a href="heap.jsp" target="_blank">New Heap Dump</a>
		</P>

		<h3>Heap Dumps <small><a href="osp.jsp?tab=heapTab">Refresh</a></small></h3>
		<table id="tblHeapDumps" class="table" width="100%">
			<thead>
				<tr>
					<th>Name</th>
					<th>Size (MB)</th>
					<th>Action</th>
				</tr>
			</thead>
			<tbody>
			<% for ( File f : hprofDumps) { %>
				<tr>
					<td><%=f.getName() %></td>
					<td><%=f.length()/(1024 * 1024) %></td>
					<td><a target="_blank" href="heap.jsp?f=<%=f.getName() %>">View</a>&nbsp;&nbsp;&nbsp;&nbsp;
					<a href="<%=contextPath%>/LogServlet?op=download&f=<%=f.getName()%>">Download</a>
					</td>
				</tr>
			<% } %>
			</tbody>
		</table>
	</div>

	<!-- NETSTAT -->
	<div id="netTab" class="tab-pane fade">
		<h3>TCP Network Connections</h3>
		<table id="tblNetStat" class="table" width="100%">
			<thead>
				<tr>
					<th>Protocol</th>
					<th>Local Address</th>
					<th>Foreign Address</th>
					<th>State</th>
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>
	</div>

	<!-- Container -->
	<div id="containerTab" class="tab-pane fade">
		<div style="width: 80%; margin: auto;">
			<canvas id="containerChart"></canvas>
		</div>
		<div style="width: 80%; margin: auto;">
			<canvas id="requestChart"></canvas>
		</div>
	</div>
	
	<!-- Alerts -->
	<% if ( !OSProfilerAlerts.hideAlertsTab()) { %>
	<div id="alertsTab" class="tab-pane fade">
	
		<!-- 2/1/2019 Deprecated 
		<h3>Alerts <small> for CPU, Threads, Heap and License will fire only if the <a href="../config/config_node.jsp">notification system</a> is enabled.</small></h3>
		
		<form  method="post" action="osp.jsp?action=save">
		
			<table class="table" border="0">
				<tr>
					<td>Peak CPU % usage</td>
					<td>
						<input name="txtCPU" type="text" value="<%=CloudNotificationService.getThresholdCPU()%>" pattern="\d{2,3}" title="Live thread count." required="required">
					</td>
				</tr>
				<tr>
					<td>Peak Live thread count</td>
					<td><input name="txtThreads" type="text" value="<%=CloudNotificationService.getThresholdThreads()%>" pattern="\d{2,4}" title="Live thread count." required="required"> </td>
				</tr>
				<tr>
					<td>Free heap below (MB)</td>
					<td><input name="txtHeap" type="text" value="<%=CloudNotificationService.getThresholdHeap()%>" pattern="\d{1,3}" title="Free heap." required="required"> </td>
				</tr>
				<%if (cfgServer.containsKey(NodeConfiguration.KEY_SERVER_LIC)) { %>
				<tr>
					<td>Peak license % usage</td>
					<td>
						<input name="txtPeakLic" type="text" value="<%=CloudNotificationService.getThresholdLicense()%>" pattern="\d{1,3}" title="Peak license usage." required="required">
						&nbsp;&nbsp;&nbsp;&nbsp;<a href="lhpu.jsp" target="_blank">Historical peak usage</a>						
					</td>
				</tr>
				<% } %>
				<tr>
					<td><a href="#" data-toggle="modal" data-target="#modal1">Container Exceptions</a></td>
					<td>&nbsp;</td>
				</tr>
			</table>
			<button id="btn_submit" type="submit" class="btn btn-primary"  title="Save">Save</button>
		</form>
		-->
		<%
			Map<String, Object> results = null;
			List<Result> list = null;
			
			try {
				results = Diagnostics.run();
				list = (List<Result>)results.get("RESULTS");	
			} catch (Exception e) {
				if ( results == null) {
					results = new HashMap<String, Object> ();
					list 	= new ArrayList<Result>();
					results.put(Diagnostics.KEY_FINAL_STATUS, "<font color=red>" + HTTPServerTools.exceptionToString(e) + "</font>");
				}
			}
		%>
		
		<h3>Status: <small><%=results.get(Diagnostics.KEY_FINAL_STATUS)%></small></h3>
		
		<table id="tbl1" class="table table-striped m-n">
			<thead>
				<tr>
					<th>Diagnostic</th>
					<th>Result</th>
					<th>Expected</th>
					<th>Status</th>
					<th>Solution</th>
				</tr>
			</thead>
			<tbody>
		<%  for ( Result res : list) { %>	
				<tr>
					<td><%=res.getName() %></td>
					<td><%=res.getResult().toString()%></td>
					<td><%=res.getExpected() %></td>
					<td><%=res.getColorizedStatus()%></td>
					<td><%=(res.getSolution() + " &nbsp;&nbsp;" + getSolution(res))%></td>
				</tr>
		<% } %>
			</tbody>
		</table>
		<!-- 1/15/2020 - deprecated  in favor od node config alert system 
		<a href="#" onclick="return checkForExceptions()">Check/Notify of Container Exceptions</a> <small>(May take a a while)</small>&nbsp;&nbsp;&nbsp;
		<a href="#" data-toggle="modal" data-target="#modal1">Configure</a>&nbsp;&nbsp;&nbsp;&nbsp;
		-->
	</div>
	<!-- END Alerts -->
	<% } %>

	<!-- MCAST TAB -->
	<div id="mcastTab" class="tab-pane fade">
		<jsp:include page="osp_tile_mcast.jsp">
			<jsp:param value="bar" name="foo"/>
		</jsp:include>
	</div>
	<!-- END MCAST TAB -->

	<!-- VMS TAB 
	<div id="vmsTab" class="tab-pane fade">
	</div>
	END VMS TAB -->
	
	</div>
	<!-- tab content -->	

	<jsp:include page="osp_tile_vms.jsp">
		<jsp:param value="val1" name="param1"/>
	</jsp:include>

	<!--  modal 1: Container exceptions -->	
	<form method="post" action="osp.jsp">
		<input type="hidden" id="action" name="action" value="save_notif">
		<div id="modal1" class="modal fade" tabindex="-1" role="dialog">
				<div class="modal-dialog">
					<div class="modal-content">
						<div class="modal-body">
							<h3>Container Exception Alerts</h3>
							
							
							<jsp:include page="osp_tile_smtp.jsp">
								<jsp:param value="block" name="display"/>
								<jsp:param value="notification_" name="idPrefix"/>
							</jsp:include>
							
						</div>
						
						<div class="modal-footer">
							<input type="submit" value="Save" class="btn btn-primary">
						</div>
						
					</div>
				</div>
		</div>
	</form>
	
    <!-- /.container-fluid -->
	
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=theme%>" name="theme"/>
	</jsp:include>

	<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>

	<!--  gauges -->
	<script type='text/javascript' src="../../js/plugins/morris/raphael.min.js"></script>
	<script type='text/javascript' src='../../js/justgage.js'></script>

	<script type="text/javascript">
		var POLL_INTERVAL 	= 5000;
		var INTERVAL_ID		= -1;
		var tableThreads;
		var tableNetStat;
		var tableVMs;
		var vmpid			= -1;
		
        $().ready(function() {
        	tableThreads = $('#tblThreads').DataTable({
        		stateSave: true, 
        		"ajax": pollEndPoint + '?op=tr&vmpid=-1' ,
        		"language": {
        			"lengthMenu": 'Display <select>'+
        					'<option value="20">20</option>'+
        					'<option value="50">50</option>'+
        					'<option value="100">100</option>'+
        					'<option value="500">500</option>'+
        					'<option value="-1">All</option>'+
        					'</select>' 
        					
        		},
    			"headerCallback": function ( row, data, start, end, display ) {
    				var api = this.api(), data;
    				
    				// Update footer
    				var totals = '(Total: ' + api.data().length + ')';
    	            $( '#totals' ).html( totals );
    			}
        	});
        	
        	$('#tblHeapDumps').DataTable({
        		stateSave: true
        	});
        	
        	tableNetStat = $('#tblNetStat').DataTable( {
        		stateSave: true,
        		"ajax": pollEndPoint + '?op=net' ,
        		"lengthMenu": [[10, 50, 100, -1], [10, 50, 100, "All"]]
        	} );
        	
        	// 2/3/2020 VMs
        	/* disabled 4/30/2020 - fails in linux
          	tableVMs = $('#tblVMs').DataTable({
        		stateSave: true,
        		searching: false, paging: false,
        		"ajax": pollEndPoint + '?op=vms',
        		"columnDefs": [
   		   			// Wrap name with an inspect href
   					{	"targets": 0, 
   						"render": function ( data, type, full, meta ) {
   							//LOGD("full=" + JSON.stringify(full));
   							return '<div><a href="#" onclick="return fetchVM(' + full[0] + ')">' + data + '</a></div>' ;
   					}} 
        		]
        	}); */
  
        	// poll every 5s
        	startPolling(5000);
        	
        	$('#tabCPU').click ( function () {
				LOGD("CPU click"); 
				resetPolling (5000);
        	});
        	$('#tabThreads').click ( function () {
        		LOGD("Threads click");
        		resetPolling (5000);
        	});
        	$('#tabNet').click ( function () {
        		LOGD("NetState click");
        		resetPolling (60000);
        	});
        	
        	// Select a tab: defaut = CPU 
			<% if ( tab != null) { %>
			$('#<%=tab%>').toggleClass('in active');
			<% } else { %>
			$('#cpuTab').toggleClass('in active');
			<% } %>
        });
        
        function startPolling(interval) {
        	POLL_INTERVAL 	= interval;
        	INTERVAL_ID 	= setInterval(function () {
        		pollAll();	
        	}, interval); 
        }
        
        function stopPolling() {
        	clearInterval(INTERVAL_ID);
        }
        
        function resetPolling (interval) {
        	if ( POLL_INTERVAL != interval) {
        		LOGD("Poll reset old: " + POLL_INTERVAL + " New:" + interval);
        		stopPolling();
        		startPolling(interval);
        	}
        }
        
        function pollAll() {
    		//LOGD("AJAX table reload: Threads visible: " + $('#tblThreads').is(':visible') + " NetStat Visible:" + $('#tblNetStat').is(':visible') );
    		var interval = 5000;
    		if ( $('#tblCharts').is(':visible') || $('#containerChart').is(':visible')  ) {
    			poll();
    		}
    		if ( $('#tblThreads').is(':visible') ) {
    			tableThreads.ajax.reload();
    		}
    		if ( $('#tblNetStat').is(':visible') ) {
    			interval = 60000;
    			tableNetStat.ajax.reload();
    		}
    		if ( $('#mcastTab').is(':visible') ) {
    			pollMCastTunnel(); 
    		}
    		if ( $('#tblVMs').is(':visible') ) {
    			// 4/30/2020 disabled - doesn't work in linux tableVMs.ajax.reload();
    		}
    		resetPolling (interval);
        }
        
    </script>
</body>
</html>