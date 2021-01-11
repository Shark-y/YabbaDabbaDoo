<%@page import="com.cluster.ClusterDaemon"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
    
<%!
static void LOGD(String text) {
	System.out.println("[CLUSTER-MGR-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[CLUSTER-MGR-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[CLUSTER-MGR-ERR] " +text);
}
%>

<%
	final String clusterGroup 	= ClusterDaemon.getClusterGroupName();
	final ThemeManager tm 		= ThemeManager.getInstance();
	final String theme			= tm.getThemeName() != null 	? tm.getThemeName() 	: "bootstrap-blue";
	final String title			= tm.getThemeTitle() != null 	? tm.getThemeTitle() 	: "Cluster Manager (" + clusterGroup + ")";
	final String mode			= request.getParameter("mode") != null ? request.getParameter("mode") : "tbl";
	final boolean showLeftNav	= true;
	
	LOGD("Theme: " + theme);
	LOGD("Title: " + title + " Mode:" + mode);
	
	// Global Page settings...
	if ( session.getAttribute("theme") == null) {
		session.setAttribute("theme", theme );
	}
	if ( session.getAttribute("title") == null) {
		session.setAttribute("title", title);
	}

	final String contextPath 	= getServletContext().getContextPath();
	//final int MAX_CHARTS 		= 20;	// Max # of nodes that can be displayed
	
	boolean loggedIn 			= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	
	NodeConfiguration conf 		= (NodeConfiguration)getServletContext().getAttribute(NodeConfiguration.SKEY_CFG_SERVER);
	
	if ( conf != null) {
		LOGD("Adding node url " + request.getRequestURL() + " to the CM config.");
		conf.addNodeURL(request.getRequestURL().toString(), true);
	}
%>
    
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="PRAGMA" CONTENT="NO-CACHE">

<jsp:include page="<%=SkinTools.TILE_PATH_HEAD%>" flush="true">
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="<%=SkinTools.SKIN_PATH%>" name="basePath"/>
</jsp:include>


<link rel="stylesheet" type="text/css" href="css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="css/jquery.json-viewer.css">

<!--  JS console logging -->
<script type="text/javascript" src="js/log.js"></script>

<!-- New chart lib -->
<script type='text/javascript' src="js/Chart.js"></script>

<script type='text/javascript' src="js/poll.js"></script>
<script type='text/javascript' src="js/notify.js"></script>
<script type='text/javascript' src="js/dash_poll.js"></script>
<script type="text/javascript" src="jsp/config/cfg_sep.js"></script>


<script type='text/javascript' src="js/dash_charts.js"></script>

<script type='text/javascript' >
pollActive = true;

window.onload = function() 
{
	// start polling...
	setTimeout("describe()", 5000);
}
function describe() {
	if ( !pollActive ) {
		return;
	}
	//LOGD("Describe");
	TABLE.ajax.reload();

	// recurse
	setTimeout("describe()", 5000);
} 

</script>

</head>
<body>

<!-- 
    <div id="wrapper">
-->


<jsp:include page="<%=SkinTools.TILE_PATH_PAGE_START%>">
	<jsp:param value="<%=title%>" name="title"/>
	<jsp:param value="<%=SkinTools.SKIN_PATH%>" name="basePath"/>
	<jsp:param value="Cluster Data" name="pageTitle"/>
	<jsp:param value="<%=!showLeftNav%>" name="hideLeftNav"/>
</jsp:include>


<div id="page-wrapper">
        
        <!-- 
        <div class="page-heading">
			<h1>Cluster Data Structures</h1>
        </div>
        <hr>
        -->
		<div class="row">
        	<div class="col-lg-12">

				<table id="table1" class="display compact" width="100%">
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

<!-- /#page-wrapper -->
<!-- /#wrapper -->


<jsp:include page="<%=SkinTools.TILE_PATH_PAGE_END%>">
	<jsp:param value="<%=SkinTools.SKIN_PATH%>" name="basePath"/>
	<jsp:param value="" name="commonPath"/>
</jsp:include>

<script type="text/javascript" src="js/jquery.dataTables.js"></script>
<script type="text/javascript" src="js/jquery.json-viewer.js"></script>

<script>
//Cluster view
var TABLE;

$(document).ready(function() {
	// {"data":[["CLUSTER_CLIENT_MAP","T_MAP",{},false],["CLUSTER_NODE_HEARTBEAT","T_MAP",{"192.168.56.1_96cd8ea7-9cb3-4445-89b3-2af38f397b8a":1547400582539},false]]}
	var END_POINT = '<%=contextPath%>/Cluster?rq_operation=describe';

	TABLE = $('#table1').DataTable( {
		stateSave: true, 
		"ajax": END_POINT,
		"language": {
			"lengthMenu": '&nbsp;<input type="checkbox" onclick="toggle_refresh(this)" checked="true" /> Auto Refresh'
		},
		// poor man's Stringify for the mesage col (2) - value
		"columnDefs": [ {
			"targets": 2,
			"createdCell": function (td, cellData, rowData, row, col) {
				$(td).jsonViewer(cellData);	
			} 			
			/*
			"render": function ( data, type, full, meta ) {
				//LOGD("render type=" + type + " col=" + meta.col + " data: " + JSON.stringify(data));
			    return type == 'display' && meta.col == 2 ? JSON.stringify(data) : data;
			} */
		}] 		
	});
});

function toggle_refresh (cb ) {
	LOGD("Toggle refresh Checked:" + cb.checked);
	pollActive = cb.checked;
	
	if ( cb.checked) {
		describe();
	}
}
</script>

</body>
</html>