<%@page import="com.cloud.kubernetes.K8SNode"%>
<%@page import="com.cloud.kubernetes.Kubernetes"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.io.IOException"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	final String contextPath 	= getServletContext().getContextPath();
	final String nodeName		= request.getParameter("name");
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	final String pageTitle		= "<a target=\"_blank\" href=\"https://helm.sh/\"><img width=\"56\" src=\"../../../img/helm.png\"></a> Package Manager"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;<button class=\"md-btn md-btn-flat\" onclick=\"window.location='helm.jsp'\" title=\"Switch to card mode\"><i class=\"md-icon material-icons\">credit_card</i></button>"; 	
	
	final String wsUrl 			= (request.getScheme().equals("http") ? "ws://" : "wss://")
		+ request.getServerName() + ":" + request.getServerPort() + contextPath;

	// all nodes
	Map<String, K8SNode> nodes 	= Kubernetes.getClusters();
	boolean nodesAvailable		= true;
	
	if ( nodes.size() == 0 ) {
		statusMessage 	= "Add a <a href=\"../nodes.jsp\">cluster</a> first.";
		statusType		= "ERROR";
		nodesAvailable	= false;
	}
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
	<jsp:param value="../../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Kubernetes" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../../css/jquery.json-viewer.css">
<link rel="stylesheet" type="text/css" href="../../docker/typeahead.css">
<link rel="stylesheet" type="text/css" href="../../../css/snackbar.css">

<style>
/* altair input place holder color */
.uk-form ::placeholder {
  color: white;
  opacity: 1; /* Firefox */
}
.twitter-typeahead {
  width: 100%;
}
</style>

<script src="../common.js"></script>

</head>
<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
		<jsp:param value="../../../" name="commonPath"/>
		<jsp:param value="<%=pageTitle%>" name="pageTitle"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../../\") %>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>


<!-- CONTENT -->
<%if (nodesAvailable) { %>
	<div class="row">
		<div class="col-md-12">
			<!-- data-widget='{"draggable": "false"}' -->
			<div class="panel panel-default md-card">
				<%if ( SkinTools.isCloudsTheme()) { %>
				<div class="panel-controls dropdown">
					<button id="btnRefreshCharts" class="btn btn-icon-rounded refresh-panel" onclick="refreshCharts()"><span class="material-icons inverted">refresh</span></button>
					<button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>
	                <ul class="dropdown-menu" role="menu">
	                    <li><a href="#" onclick="return helmListRepos()">List Repos</a></li>
	                    <li><a href="#" onclick="return helmAddRepo()">Add Repo</a></li>
	                    <li><a href="#" onclick="return helmUpdateRepos()">Update Repos</a></li>
	                    <li><a href="#" onclick="return helmInstallChart()">Install Chart</a></li>
	                    <li class="divider"></li>
	                    <li><a target="_blank" href="https://helm.sh/">About Helm</a></li>
	                    <li><a target="_blank" href="https://hub.helm.sh/">About Helm Hub</a></li>
	                </ul>					 
				</div>
				<div class="panel-heading">
					<h2><a href="../nodes.jsp">Cluster</a></h2>
					<select  id="selNodes" name="selNodes" class="form-control nodes" onchange="$('#btnRefreshCharts').click()"></select>
				</div>
				<% } else if ( SkinTools.isAltAirTheme()) { %>
				<div class="md-card-toolbar">
					<div class="md-card-toolbar-actions">
						<i id="btnRefreshCharts" class="md-icon material-icons" onclick="return refreshCharts()">refresh</i>
						<div class="md-card-dropdown" data-uk-dropdown="{pos:'bottom-right'}">
                        	<i class="md-icon material-icons">&#xE5D4;</i>
                        	<div class="uk-dropdown uk-dropdown-small">
                            	<ul class="uk-nav uk-nav-dropdown">
				                    <li><a href="#" onclick="return helmListRepos()">List Repos</a></li>
				                    <li><a href="#" onclick="return helmAddRepo()">Add Repo</a></li>
				                    <li><a href="#" onclick="return helmUpdateRepos()">Update Repos</a></li>
				                    <li><a href="#" onclick="return helmInstallChart()">Install Chart</a></li>
			                        <li class="uk-nav-divider"></li>
				                    <li><a target="_blank" href="https://helm.sh/">About Helm</a></li>
				                    <li><a target="_blank" href="https://hub.helm.sh/">About Helm Hub</a></li>
                            	</ul>
                        	</div>
						</div>						
					</div>
					<div class="uk-width-1-1">
						<h3 class="md-card-toolbar-heading-text"> <a href="../nodes.jsp">Cluster</a></h3> &nbsp;&nbsp;&nbsp;&nbsp;
						<select id="selNodes" name="selNodes" class="nodes" onchange="$('#btnRefreshCharts').click()" style="margin-top: 15px"></select>
					</div>
				</div>
				
				<% } %>
				<div class="panel-body md-card-content">
					<table id="tblCharts" class="table m-n" style="width: 100%">
						<thead>
							<tr>
								<th>App</th>
								<th>Chart</th>
								<th>Version</th>
								<th>Updated</th>
								<th>Namespace</th>
								<th>Status</th>
								<th>Action</th>
							</tr>
						</thead>
						<tbody>
						</tbody>							
					</table>
					
				</div>
				<!-- end panel body -->
			</div>
					
		</div>
	</div>	
	
	<!-- modal 3 show stdout messages -->
	<div id="modal3" class="modal fade uk-modal" tabindex="-1" role="dialog">
		<div class="modal-dialog uk-modal-dialog">
			<%if ( SkinTools.isAltAirTheme()) {%>
			<button type="button" class="uk-modal-close uk-close"></button>
			<%} %>
			<div class="modal-content">
				<div class="modal-header">
					<%if ( SkinTools.isCloudsTheme()){%>
					<button type="button" class="close uk-modal-close uk-close" data-dismiss="modal" aria-hidden="true"> &times;</button>
					<%} %>
					<h3 id="inspectTitle" class="modal-title"></h3>
				</div>
				<div id="json-renderer"></div>
			</div>
		</div>
	</div>
	<button id="btnInspect" data-toggle="modal" data-target="#modal3" style="display: none" data-uk-modal="{target:'#modal3'}"></button>	
	
	<!--  ace code editor @ https://ace.c9.io -->	
	<script src="../../../lib/ace/ace.js"></script>
	
	<!-- modal 8 chart params -->
	<jsp:include page="../tile_modal_chart_params.jsp" />
	
	<!-- modal 4 Add repo -->
	<jsp:include page="tile_helm_addrepo.jsp" />
	
<!-- END CONTENT -->
<% } %>
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
		<jsp:param value="../../../" name="commonPath"/>
	</jsp:include>
	
	<script type="text/javascript" src="../../../js/poll.js"></script>
	<script type="text/javascript" src="../../../js/jquery.json-viewer.js"></script>
	<script type="text/javascript" src="../../../js/snackbar.js"></script>
	
	<!-- wizards -->
	<script type="text/javascript" src="../../../js/jquery.validate.min.js"></script>
	<script type="text/javascript" src="../../../js/jquery.stepy.js"></script>

	<!-- typeahead 	-->
	<jsp:include page="../tile_type_ahead.jsp">
		<jsp:param value="../../../" name="basePath"/>
	</jsp:include>
	
	<script>
	
	//var node	= '';
	var url 	= '<%=contextPath%>/K8S?'; //?node=' + node;
	var TBL1;	// charts
	
	// Initialization: 
	$(document).ready(function() {
<%if (nodesAvailable) { %>    	
    	loadNodes();
		/* show search */
		$('#search-box').show();
		$('#search-input').attr("placeholder", "Search for Apps..."); 
		$('#search-input-node').val($('#selNodes').children('option:selected').val());
		
		initializeChartsTable();
		refreshCharts ();
		initializeTypeAhead();
<% } %>
	});
	
	function loadNodes() {
		<% for ( Map.Entry<String, K8SNode> entry : nodes.entrySet()) { 
			K8SNode node = entry.getValue();
		%>
		$('#selNodes').append($('<option></option>').attr('value', '<%=node.getName()%>').text('<%=node.getFullName()%>'));
		<% } %>
		<% if ( nodeName != null) { %>
		$('#selNodes option').filter (function() {
			// LOGD('opt ' + $(this).val() + ' == ' + '<%=nodeName%>');
			if ( $(this).val() == '<%=nodeName%>') {
				 $(this).prop('selected', true);
			}
		});
		<% } else { %>
		$('#selNodes option:eq(0)').prop('selected', true); // select first
		<% } %>
	}

	// fires when the refresh charts btn is clicked.
	function refreshCharts () {
		var node 		= $('#selNodes').children('option:selected').val();
		var Url			= url + 'node=' + node + '&op=HelmList';
		if ( !node || node == '') {
			growl('Select a node first', 'danger');
			return;
		}
		LOGD('Reload charts url: ' + Url + ' node: ' + node);
		//TBL1.ajax.reload();
		TBL1.ajax.url(Url).load();
		
		setTimeout(findApps,3000);
	}
	
	/**
	 * matchServicesAndCharts
	 * http://localhost:9080/CloudClusterManager/K8S?node=KubeFoghornLeghorn&op=ListServices
	 */
	function findApps () {
		var node 		= $('#selNodes').children('option:selected').val();
		var nodeName 	= $('#selNodes').children('option:selected').text(); 	// Name (URL)
		var apiUrl		= nodeName.split(' ')[1].replace(/[\(\)]/g,'');			// https://192.168.40.84:6443/
		var Url			= url + 'node=' + node + '&op=ListServices';
		
		wrapApps (Url, TBL1, apiUrl);
	}
	
	
	/**
	 * Helm hub install: 
	 * Chart JSON {"hubUrl":"https://artifacthub.io","id":"bitnami/apache","version":"8.0.3","advanced":false,"params":"","ns":"","icon":"/image/ee08f467-cfcd-4831-b313-711d5b088acb","repo":"https://charts.bitnami.com/bitnami","desc":"Chart for Apache HTTP Server"}
	 * @param hubUrl Hub intsall/doc url: https://hub.helm.sh/ or https://192.168.40.84:32543/
	 * @param chart Chart name
	 * @param version Version
	 * @param advanced Optional: If true open the advanced dialog to aks for install params
	 * @param params Optional: User entered params from advanced dialog: key1=val1,key2=val2,...
	 * @param ns Optional namespace
	 * @param icon Optional chart icon name (<img src="{{hubUrl}}{{attributes.icon}}">)
	 * @param repo HELm repo URL
	 * @returns {Boolean}
	 */
	 //function hub_install (hubUrl, chart, version, advanced, params, ns, icon, repo) {
	 function hub_install (chart) {
		LOGD('Helm.jsp:' + JSON.stringify(chart));		 
		
		var node 		= $('#selNodes').children('option:selected').val();
		
		var callback 	=  function (data) { 
			$('#btnRefreshCharts').click(); 
			$('#modal8InstallSpinner').hide();
			
			if ( data.status >= 400) {
				if ( modal8IsVisible ()) {
					modal8SetStatus(data.message, 'danger');
				}
				else {
					growl(data.message, 'danger');
				}
			}
		} 
		chart.basePath 	= '../../../';
		chart.node		= node;
		chart.callback	= callback;
		
		hubInstall ( chart );
	 }

	</script>
	 	
	<script type="text/javascript" src="helm.js"></script>
	<script type="text/javascript" src="helm-repo.js"></script>
</body>
</html>