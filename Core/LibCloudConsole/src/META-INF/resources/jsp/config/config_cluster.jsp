<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.cluster.CloudCluster"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
static void LOGD(String text) {
	System.out.println("[CFG-CLUSTER-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[CFG-CLUSTER-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[CFG-CLUSTER-ERR] " +text);
}
%>

<%
	final String KEY_GRP 		= CloudCluster.KEY_NODE_GRP; 				// Cluster group name param key
	final String KEY_MEMBERS 	= CloudCluster.KEY_NODE_MEMBERS; 			// Cluster memebers (for tcp discovery)
	
	final String contextPath 	= getServletContext().getContextPath();
	final boolean showLeftNav	= request.getParameter("leftnav") != null;
	//boolean loggedIn 			= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	final String action			= request.getParameter("action");

	NodeConfiguration cfg		= CloudServices.getNodeConfig(); 			// 8/3/2019
	
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)

	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	
	// request params: Cluster group
	String paramGrp				= request.getParameter(KEY_GRP);
	String paramMembers			= request.getParameter(KEY_MEMBERS);

	// load a config from: session, then servlet ctx
	/* 8/3/2019 Oboslete
	cfg 						= (NodeConfiguration)session.getAttribute(NodeConfiguration.SKEY_CFG_SERVER);
	

	if ( cfg == null) {
		LOGD("unable to load cfg from session. Trying servlet ctx.");
		cfg = (NodeConfiguration)getServletContext().getAttribute(NodeConfiguration.SKEY_CFG_SERVER);
	}
	if ( cfg == null) {
		LOGE("** FATAL: No server cfg available!");
		response.sendRedirect("../../login.jsp?action=loginshow&r=." + (showLeftNav ? "&leftnav=1" : "") + "&m=Session+Expired." );
		return;
	} */
	if ( action != null ) {
		if ( action.equals("save")) {
			if ( paramGrp != null) {
				LOGD("Save grp " + paramGrp + " Members:" + paramMembers);
				
				// required
				cfg.put(KEY_GRP, paramGrp);
				
				// optional
				if ( paramMembers != null && !paramMembers.isEmpty()) {
					cfg.put(KEY_MEMBERS, paramMembers);
				}
				else {
					cfg.remove(KEY_MEMBERS);
				}
				
				Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Saving cluster group: " 
					+ paramGrp + (paramMembers != null && !paramMembers.isEmpty() ? ". Members " + paramMembers : ""));
				
				cfg.save();
				
				statusMessage = "<span id=\"statusMsg\">Configuration saved. <a href=\"javascript:restart_onclick()\">Service restart is required.</a></span>";
			}
		}
		
		if ( action.equals("restart")) {
			LOGD("Cluster restart w/ params: " + cfg.getClusterParams());
			
			Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Cluster restart w/ params " 
					+ cfg.getClusterParams());

			CloudCluster.getInstance().shutdown();
			try {
				CloudCluster.getInstance().initialize(cfg.getClusterParams());
			}
			catch (Exception e) {
				statusType 		= "ERROR";
				statusMessage	= "Cluster Initialization failed: " + e.toString() 
						+ ". This may occur when other nodes are running in a different mode. Shutdown other nodes and try again later"
						+ ". See the <a target=_new href='../../log/logview.jsp'>log view</a> for details.";
			}
		}
		
		if ( action.equals("start")) {
			LOGD("Cluster start w/ params: " + cfg.getClusterParams());
			try {
				Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Cluster start w/ params " 
						+ cfg.getClusterParams());
				
				CloudCluster.getInstance().initialize(cfg.getClusterParams());
			}
			catch (Exception e) {
				statusType 		= "ERROR";
				statusMessage	= "Cluster Initialization failed: " + e.toString() + ". Try again later"; 
			}
		}
		
		if ( action.equals("stop")) {
			LOGD("Cluster & services shutdown.");
			
			Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Cluster services shutdown."); 

			CloudServices.stopServices();
			CloudCluster.getInstance().shutdown();
		}
		CloudCluster.getInstance().dumpAllInstances("CLUSTER-CFG");
	}
	
	final String clusterGroup 	= cfg.get(KEY_GRP) != null 
			? cfg.get(KEY_GRP).toString() 
			: CloudCluster.getInstance().getClusterGroupName();

	// Use a cluster TCP memeber list (if available) else use the RQ param.
	final List<String> list 	= CloudCluster.getInstance().getClusterTcpMembers();
	final String clusterMembers	= cfg.get(KEY_MEMBERS) != null ? cfg.get(KEY_MEMBERS).toString() : "";

	//if ( list != null && !list.isEmpty() ) 	clusterMembers = IOTools.join(IOTools.convertList(list), ",");
	//if ( clusterMembers == null) 			clusterMembers = cfg.get(KEY_MEMBERS) != null ? cfg.get(KEY_MEMBERS).toString() : "";
	
	LOGD("TCPCfg members:" + list + " CFG members:" + cfg.get(KEY_MEMBERS) 
			+ " CM:" + clusterMembers + " LeftNav:" + showLeftNav + " title:" + title + " theme:" + theme);
	
	// Node should be down before changes...
	if ( CloudServices.servicesOnline()) {
		statusType 		= "WARN";
		statusMessage	= "Stop the node before any changes.";
	}
	
	ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, request, "Visitor entered cluster configuration.");

%>
    
<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Configure" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="../../css/jquery.json-viewer.css">

<script type="text/javascript">

var STOP_POLL = false;

/**
 * Fires when the restart link is clicked.
 */
function restart_onclick() {
	var statusObj = document.getElementById('statusMsg');
	if ( statusObj) {
		statusObj.innerHTML = 'Please wait...';
	}
	STOP_POLL 	= true;		// stop polling
	location 	= 'config_cluster.jsp?action=restart<%=showLeftNav ? "&leftnav" : ""%>';
}

function start_onclick() {
	location = 'config_cluster.jsp?action=start<%=showLeftNav ? "&leftnav" : ""%>';
}

function stop_onclick() {
	location = 'config_cluster.jsp?action=stop<%=showLeftNav ? "&leftnav" : ""%>';
}
</script>

<script type="text/javascript">

function delRow(row) {
	while ( row.parentNode && row.tagName.toLowerCase() != 'tr') {
		row = row.parentNode;
	}
	if ( row.parentNode && row.parentNode.rows.length > 1) {
		row.parentNode.removeChild(row);
	}
}

function cleanTable (table) {
	var rowCount 	= table.rows.length;
	
	// don't delete the 1st row (headings)
	for ( var i = 1 ; i < rowCount ; i++ ) {
		delRow (table.rows[1]);
	}
}

/**
 * Poll success callback
 * response format is in JSON. Get Members response: <pre>
 {
	    "response": [
	        {
	            "address": "/XX.XX.XX.XXX:5701",
	            "attributes": {
	                "KEY_CTX_PATH": "/ClusterManager"
	            },
	            "isLocal": true
	        },
	        {
	            "address": "/XX.XXX.X.XX:5702",
	            "attributes": {
	                "statusCode": 200,
	                "KEY_CTX_PATH": "/CloudAdapterNode001",
	                "KEY_CTX_URL": "http://VLADS5014:8080/CloudAdapterNode001/",
	                "statusMessage": "Online",
	                AvailableProcessors=4, SystemCpuLoad=0.021457471, Name=Windows 7, 
	                peakThreadCount=85, FreePhysicalMemorySize=4294967295, heapFree=1234
	            },
	            "uuid": "fcb061f0-ff75-445f-820e-60472fcd5c43",
	            "isLocal": false
	        },
	        ...
	    ],
	    "message": "OK",
	    "status": 200,
	    "clusterLeader": "7189aa89-8186-402a-9413-e3c632efbece"
	}</pre>
 */
function addNodes (json) {
	var table		= document.getElementById('tblNodes');
	var response	= json.response;
	var leader		= typeof(json.clusterLeader) != 'undefined' ? json.clusterLeader : null;
	
	// del old rows
	cleanTable (table);
	
	// Poll Response: {"message":"Cluster not yet initialized.","status":200}
	if ( typeof(response) == 'undefined') return;
	
	for ( var i = 0 ; i < response.length ; i++ ) {
		var row 	= table.insertRow(table.rows.length);
		var node 	= response[i];
		var attribs = node.attributes;
		var uuid	= typeof(node.uuid) != 'undefined' ? node.uuid : null;
		var ipPort	= node.nodeAddress.startsWith("/") ? node.nodeAddress.substring(1) : node.nodeAddress;		//	/XX.XXX.XX.XXX:5702
		var ip		= ipPort.indexOf(':') != -1 ? ipPort.substring(0, ipPort.indexOf(':')) : ipPort ;
			
		if ( typeof(attribs) == 'undefined') continue;
			
		var path	= attribs["KEY_CTX_PATH"];		// Context root /CloudContact...
		var url 	= typeof(attribs["KEY_CTX_URL"]) != 'undefined' 	? attribs["KEY_CTX_URL"] : 'http://' + ip + ':8080' + path;
		var status 	= typeof(attribs["statusMessage"]) != 'undefined' 	? attribs["statusMessage"] : 'Unknown';
		var details = (attribs["Name"] && attribs["AvailableProcessors"] ) ? attribs["Name"] + ', ' + attribs["AvailableProcessors"] + " Cpu(s)" : "";
		
		// missing KEY_CTX_URL means node needs configuration.
		if ( typeof(attribs["KEY_CTX_URL"]) == 'undefined' ) 	status = 'Configuration required.';
		
		LOGD('Node: ' + path + ' Status:' + status + ' Url:' + url);
		
		if ( typeof(path) == 'undefined') {
			LOGE('Invalid node (missing attributes): ' + JSON.stringify(node) );
			continue;
		}
		
		// ignore CM
		if ( path.indexOf('ClusterManager') != -1 ) continue;
		
		// color code the status
		var htmlColor 	= status.indexOf('Online') != -1 ? 'green' : 'red';
		var htmlLeader 	= (leader && uuid && (leader == uuid)) ? " <font color=blue>*LEADER*</font>" : "";
		
		// add row
		row.insertCell(0).innerHTML = '<a target=_new href="' + url + '">' +  (path.startsWith('/') ? path.substring(1) : path) + "</a> @ " + ipPort + htmlLeader;
		row.insertCell(1).innerHTML = '<font color=' + htmlColor + '>' + status + '</font>';
		row.insertCell(2).innerHTML = details;
	}
}

/**
 * Poll callbacks
 * response format is in JSON:
 * {"message":"OK","status":200, "response": 
 *	 [{"address":"/XX.XXX.XX.X:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"},"isLocal":true}] }
 *  
 */
function poll_cb_success(json) {
	//LOGD("Got Poll Response: " + JSON.stringify(json));
	
	// consume json: {"status": 200, "message": 'OK', ...}
	if (json.status != 200) {
		setErrorStatus(json.message);
		return;
	}
	//clearStatusMessage();
	addNodes(json);
	
	if ( STOP_POLL) {
		LOGD("Poll canceled.");
		return;
	}
	// poll recurse
	setTimeout("poll()", 5000);
}

function poll_cb_error(jqXHR, textStatus) {
	LOGW("Poll failed with status: " + textStatus);
	//setErrorStatus("Poll failed with status: " + textStatus);

	// recurse in case the long poll expired
	setTimeout("poll()", 10000);
}

/**
 * Start os METRICS polling interval
 */
function poll() {
	var pollEndPoint 	= '<%=contextPath%>/ClusterServlet?rq_operation=getmembers';
	var url 			= pollEndPoint; ;
	LOGD("Polling " + url);

	$.ajax({
		type : 'GET',
		url : url,
		// request response in json!
		headers : {
			"Accept" : "application/json; charset=utf-8",
			"Content-Type" : "application/json; charset=utf-8"
		},
		cache : false
		//data: { rq_clientId: clientId, rq_windowId: windowId, rq_operation: 'poll' }
	})
	.done(poll_cb_success)
	.fail(poll_cb_error);
}

window.onload = function() 
{
	//setOKStatus("Please wait...");

	// start polling...
	setTimeout("poll()", 5000);
	setTimeout("describe()", 10000);
}


function describe() {
	/*
	$.get('<%=contextPath%>/ClusterServlet?rq_operation=describe', function( json ) {
		// json: {"data":[["CLUSTER_CLIENT_MAP","T_MAP",{},false],["CLUSTER_NODE_HEARTBEAT","T_MAP",{"192.168.56.1_96cd8ea7-9cb3-4445-89b3-2af38f397b8a":1547400582539},false]]}
		LOGD('Describe: ' + JSON.stringify(json));

		// recurse
		setTimeout("describe()", 5000);

	}, 'json'); */
	
	if ( pollActive) {
		TABLE.ajax.reload();
	}
<% if ( cfg.getFailOverType() == FailOverType.CLUSTER_ZEROCONF) { %>	
	if ( pollActiveQ) {
		TABLE_Q.ajax.reload();
	}
<% } %>
	// recurse @ 10s
	setTimeout("describe()", 10000);
} 

</script>

</head>

<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Cluster Configuration" name="pageTitle"/>
		<jsp:param value="Home,Pages,Cluster Configuration" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
		<jsp:param value="<%=!showLeftNav%>" name="hideLeftNav"/>
	</jsp:include>

		<!-- STATUS MESSAGE -->
		<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
			<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
			<jsp:param value="<%=statusType%>" name="statusType"/>
		</jsp:include>

		<% if ( request.getParameter("debug") != null) { %>
		<div class="row">
        	<div class="col-lg-12">
        	Cluster Service Status:
        	<% if ( CloudCluster.getInstance().isShutdown() ) { %>
        	Down - <a href="javascript:start_onclick()">Start Service</a>
        	<% } else { %>
        	Up - <a href="javascript:stop_onclick()">Stop Service</a>
        	<% } %>
			</div>
		</div>
		<br/>
		<% } %>
		<% if ( cfg.getFailOverType() == FailOverType.CLUSTER_HAZELCAST) { %>
		<div class="panel panel-default card" data-widget='{"draggable": "false"}'>
		
			<div class="panel-heading card-header">
				<h2 class="panel-title">Configure</h2> 
				<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
			</div>
			
        	<div class="panel-body card-body">
 	        
			<form method="post" role="form" action="config_cluster.jsp?action=save<%=showLeftNav ? "&leftnav" : ""%>">
				<!-- Cluster Group -->
				<div class="form-group input-field col s12">
		            <label for="<%=KEY_GRP%>">Cluster Group</label>
		            <input type="text" id="<%=KEY_GRP%>" name="<%=KEY_GRP%>" class="form-control" value="<%=clusterGroup%>" required="true">
		            
		            <% if ( SkinTools.isBootstrapTheme()) {%>
		            <p class="help-block">Enter the name of the cluster group this node belongs to. Change it to join another cluster.</p>
		            <% } %>
				</div>
				
				<!-- row 2: TCP discovery -->
				<div class="form-group input-field col s12">
		            <label for="<%=KEY_MEMBERS%>">Cluster Members (TCP Discovery)</label>
		            
		            <input type="text" id="<%=KEY_MEMBERS%>" name="<%=KEY_MEMBERS%>" class="form-control" 
		            	value="<%=clusterMembers%>" maxlength="1024" 
		            	placeholder="Comma separated list of member IP addresses.">
		            	
		            <% if ( SkinTools.isBootstrapTheme()) {%>
		            <p class="help-block">Enter a comma separated list of member IP addresses. Leave empty to use multicast discovery (default).</p>
		            <% } %>
		        </div>
			    
		        <%-- if ( !CloudServices.servicesOnline()) { --%>.
		        <button type="submit" class="btn btn-primary">Save</button>
		        <%--} --%>
			</form>
			
			</div> 
		</div> 
		<!-- /.row -->
		
		<div class="row">
        	<div class="col-lg-12">
        		&nbsp;
			</div>
		</div>
		<!-- /.row -->
		<% } %>
    
    <!-- TAB HEADERS -->
	<ul class="nav nav-tabs">
		<li><a data-toggle="tab" href="#nodeTab" class="active">Cluster Nodes</a></li>
		<li><a data-toggle="tab" href="#objTab">Cluster Objects</a></li>
		<% if ( cfg.getFailOverType() == FailOverType.CLUSTER_ZEROCONF) { %>
		<li><a data-toggle="tab" href="#castTab">Multicast Queue</a></li>
		<% } %> 
	</ul>

<!-- TAB CONTENT -->
	<div class="tab-content">
 	
	<!-- NODE TAB -->
	<div id="nodeTab" class="tab-pane fade in active">
		
		<div class="row">
        	<div class="col-lg-12">
        		<h4>Cluster Nodes</h4>
        		<table class="table" id="tblNodes">
        			<tr>
        				<th>Name</th>
        				<th>Status</th>
        				<th>Details</th>
        			</tr>
        		</table>
			</div>
		</div>
	</div>
	<!-- END NODE TAB  -->
	
	<!-- CLUSTER OBJ TAB -->
	<div id="objTab" class="tab-pane fade">
	
		<!-- Object cache -->
		<div class="row">
        	<div class="col-lg-12">
        		<h4>Cluster Object View &nbsp;&nbsp;&nbsp;<small><input type="checkbox" onclick="toggle_refresh(this)" checked="checked" /> Auto Refresh</small></h4>

				<table id="table1" class="display compact" style="width:100%">
					<thead>
						<tr>
							<th>Name</th>
							<th>Type</th>
							<th>Data</th>
							<th>Expired</th>
						</tr>
					</thead>
					<tfoot>
						<tr>
							<th>Name</th>
							<th>Type</th>
							<th>Data</th>
							<th>Expired</th>
						</tr>
					</tfoot>
				</table>
			</div>
		</div>
	</div>
	<!-- END OBJ TAB -->

<% if ( cfg.getFailOverType() == FailOverType.CLUSTER_ZEROCONF) { %>
	
	<!-- MCAST QUEUE -->
	<div id="castTab" class="tab-pane fade">
	
		<!-- Multicast queue -->
		<div class="row">
        	<div class="col-lg-12">
        		<h4>Event Queue &nbsp;&nbsp;&nbsp;<small><input type="checkbox" onclick="toggle_refresh_queue(this)" checked="checked" /> Auto Refresh</small></h4>

				<table id="table2" class="display compact" style="width:100%">
					<thead>
						<tr>
							<th>Type</th>
							<th>Time to Live</th>
							<th>Message</th>
						</tr>
					</thead>
				</table>
			</div>
		</div>
	
	</div>
	<!--  END MCAST QUEUE TAB -->
<% } %>	
	</div>
	<!-- END TAB CONTENT -->
	
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=!showLeftNav%>" name="hideLeftNav"/>
	</jsp:include>

<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>
<script type="text/javascript" src="../../js/jquery.json-viewer.js"></script>

<script>
//Cluster view
var TABLE;		// Object cache
var TABLE_Q;	// multicast queue

var pollActive 	= true;		// used to toggle refresh of data view
var pollActiveQ = true;		// used to toggle refresh of mcast queu 

$(document).ready(function() {
	zeroConfInit ();
});

function zeroConfInit () {
	// {"data":[["CLUSTER_CLIENT_MAP","T_MAP",{},false],["CLUSTER_NODE_HEARTBEAT","T_MAP",{"192.168.56.1_96cd8ea7-9cb3-4445-89b3-2af38f397b8a":1547400582539},false]]}
	var END_POINT = '<%=contextPath%>/ClusterServlet?rq_operation=describe';

	TABLE = $('#table1').DataTable( {
		stateSave: true, 
		"ajax": END_POINT,
		
		// poor man's Stringify for the mesage col (2) - value
		"columnDefs": [ {
			"targets": 2,
			"createdCell": function (td, cellData, rowData, row, col) {
				$(td).jsonViewer(cellData);	
			} 			
			/* "render": function ( data, type, full, meta ) {
				//LOGD("render type=" + type + " col=" + meta.col + " data: " + JSON.stringify(data));
			    return type == 'display' && meta.col == 2 ? JSON.stringify(data) : data;
			} */
		}] 		
	});

<% if ( cfg.getFailOverType() == FailOverType.CLUSTER_ZEROCONF) { %>	
	// mcast queue
	TABLE_Q = $('#table2').DataTable( {
		stateSave: true, 
		"ajax": '<%=contextPath%>/ClusterServlet?rq_operation=queue'
	});
<% } %>

}

function toggle_refresh (cb ) {
	LOGD("Toggle refresh Checked:" + cb.checked);
	pollActive = cb.checked;
	/*
	if ( cb.checked) {
		describe();
	} */
}

function toggle_refresh_queue (cb ) {
	LOGD("Toggle queue refresh Checked:" + cb.checked);
	pollActiveQ = cb.checked;
}

</script>

</body>
</html>