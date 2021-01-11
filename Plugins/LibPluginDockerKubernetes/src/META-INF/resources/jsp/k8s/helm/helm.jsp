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
	final String pageTitle		= ""; //"<a target=\"_blank\" href=\"https://helm.sh/\"><img width=\"56\" src=\"../../../img/helm.png\"></a> Package Manager"; 	
	
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
.ProfileCard-avatar1 {
  /*position: absolute; */
  /*top: 8px;
  left: 8px; */
  width: 64px;
  /*height: 52px; */
  /*border: 2px solid #ccd6dd;
  border-radius: 5px; */
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

			<div class="uk-grid uk-width-1-1">
				<h3> <a href="../nodes.jsp">Cluster</a></h3> &nbsp;&nbsp;&nbsp;&nbsp;
				<select id="selNodes" name="selNodes" class="nodes" onchange="$('#btnRefreshCharts').click()"></select>
				&nbsp;&nbsp;
				<button id="btnRefreshCharts" class="md-btn md-btn-flat" onclick="return refreshCharts()" title="Refresh"><i class="md-icon material-icons">refresh</i></button>
				<button class="md-btn md-btn-flat" onclick="window.location='helm_dt.jsp'" title="Switch to table mode"><i class="md-icon material-icons">view_comfy</i></button>
				<button class="md-btn md-btn-flat" onclick="openterm()" title="Open a Terminal"><i class="md-icon material-icons">computer</i></button>
				<!-- Helm dropdown -->
				<div class="uk-button-dropdown" data-uk-dropdown>
					<a href="#"><i class="md-icon material-icons">&#xE5D4;</i></a>
					<div class="uk-dropdown">
						<ul class="uk-nav uk-nav-dropdown">
		                    <li><a href="#" onclick="return helmListRepos()">List Repos</a></li>
		                    <li><a href="#" onclick="return helmAddRepo()">Add Repo</a></li>
		                    <li><a href="#" onclick="return helmUpdateRepos()">Update Repos</a></li>
	                        <li class="uk-nav-divider"></li>
		                    <li><a target="_blank" href="https://helm.sh/">About Helm</a></li>
		                    <li><a target="_blank" href="https://hub.helm.sh/">About Helm Hub</a></li>
						</ul>
					</div>
				</div>
			</div>

			<div id="divCards" class="row uk-grid uk-grid-width-medium-1-3 uk-sortable sortable-handler" data-uk-grid="{gutter:24}" data-uk-sortable>
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
		
		//initializeChartsTable();
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
		
		var posting = $.get (Url);
		
		posting.done(function( data ) {
			// HELM DATA: {"Releases":[{"app_version":"2.4.46","name":"apache","namespace":"default","updated":"2020-12-28 15:17:34.913293732 -0600 CST","chart":"apache-8.0.3","revision":"1","status":"deployed"},{"app_version":"0.35.0","name":"ingress-nginx","namespace":"default","updated":"2020-09-03 16:12:36.360857425 -0500 CDT","chart":"ingress-nginx-2.15.0","revision":"1","status":"deployed"},{"app_version":"v1.10.0","name":"monocular","namespace":"default","updated":"2020-09-03 16:10:51.32986299 -0500 CDT","chart":"monocular-1.4.15","revision":"1","status":"deployed"},{"app_version":"2.20.1","name":"prometheus","namespace":"default","updated":"2020-09-11 07:45:23.379248951 -0500 CDT","chart":"prometheus-11.13.0","revision":"1","status":"deployed"}],"message":"OK","status":200}
			LOGD('Get Charts: ' + JSON.stringify(data));
			var apps = data.Releases;
			var html = '';
			
			if ( data.status >= 400) {
				growl(data.message, 'danger');
				$('#divCards').html('');
				return;
			}
			// for each helm chart
			var namespaces 	= [];
			//var colors 		= ['607d8b', '00039f', '1976d2', '673ab7'] ;
			//var col 		= colors[Math.floor(Math.random() * colors.length)];	// bg color
			
			for ( var i = 0 ; i < apps.length ; i++) {
				var img 	= '../../../img/kube.png';
				var app		= apps[i];
				
				// HUB DATA: {"hubUrl":"https://artifacthub.io","chart":"bitnami/apache","version":"8.0.3","advanced":false,"params":"","ns":"","icon":"/image/ee08f467-cfcd-4831-b313-711d5b088acb","repo":"https://charts.bitnami.com/bitnami","desc":"Chart for Apache HTTP Server"}
				var metaKey	= node + '-' + app.name;
				var helm	= window.localStorage.getItem (metaKey);
				
				var json	= helm ? JSON.parse (helm) 			: null;
				
				var icon	= json && json.hubUrl && json.icon 	? json.hubUrl + json.icon : null;
				var more 	= json && json.hubUrl && json.chart ? ' &nbsp;&nbsp;<a target="_blank" href="' + json.hubUrl + '/charts/' + json.chart + '">MORE</a>' : '';
				var extra	= 		'<tr><td>Version</td><td>' 	+  app.app_version + '</td></tr>'
								+ 	'<tr><td>Status</td><td>' 	+ wrapInLabel(app.status) + '</td></tr>'
								+ 	'<tr><td>Updated</td><td>' 	+ app.updated.split('\.')[0] + '</td></tr>'
								+ 	'<tr><td><a href="../manage.jsp?name=' + node + '">Pods</a></td><td><span id="pods-' + i + '"></span></td></tr>';
								
				var desc 	= json && json.desc 
							? json.desc + more + '<table>' + extra  + '</table>'
							: '<table>' + extra + '</table>';
				
				// https://ui-avatars.com/api/?name=John+Doe&background=random&rounded=true&color=ff0000&size=128
				var colors 	= ['607d8b', '00039f', '1976d2', '673ab7'] ;
				var col 	= colors[Math.floor(Math.random() * colors.length)];
				img 		= 'https://ui-avatars.com/api/?background=' + col + '&color=ffffff&size=512&name=' + app.name; // &rounded=true
				
				LOGD ('Build app node: ' + node + ' name:' + app.name + ' Helm:' + helm + ' Key: ' + metaKey + ' icon:' + icon);
				
				html 		+= buildAppCard (i, img, icon, app, desc);
				
				namespaces.push(app.namespace); // save ns
			}
			namespaces = [...new Set(namespaces)];
			LOGD('Unique Namespaces: ' + JSON.stringify(namespaces));
			
			$('#divCards').html(html);
			
			// bind a click event to the del link: (used to avoid the twice click execution on onclick="...")
			for ( var i = 0 ; i < apps.length ; i++) {
				let app		= apps[i];
				$( "#linkDelete-" + i ).on( "click", function(ev) {
				    ev.preventDefault();
				    
				    //LOGD('Delete ' + app.name + ' v:' + app.app_version  + ' ns:' + app.namespace);
				    helmDelete( app.name , app.app_version , app.namespace , function () { deleteComplete() } );
				});
			}
			
			linkApps 	(apps);
			linkPods	(apps, namespaces);
		});
		
	}
	
	function linkPods (apps, namespaces) {
		var node 		= $('#selNodes').children('option:selected').val();
		var url 		= '../../../K8S?node=' + node + '&op=ListPods';

		// [{"app_version":"2.4.46","name":"apache","namespace":"default","updated":"2020-12-30 11:32:51.119194372 -0600 CST","chart":"apache-8.0.3","revision":"1","status":"deployed","podStatuses":{"Running":1,"Pending":0,"Succeeded":0}}]
		var callback	= function (apps) {
			LOGD('Fetch POD info COMPLETE Apps: ' + JSON.stringify(apps));
			
			for ( var rowIdx = 0 ; rowIdx < apps.length ; rowIdx++) {
				// {"Running":1,"Pending":0,"Succeeded":0}
				var statuses 	= apps[rowIdx].podStatuses;
				var running		= wrapInLabel('Running: ' +  statuses.Running);
				var pending		= statuses.Pending > 0 ? wrapInLabel('Pending: ' +  statuses.Pending) : '';
				//$('#details-'+ rowIdx).html($('#details-'+ rowIdx).html() + ' <br/>Pods: ' + running + ' ' + pending);
				$('#pods-'+ rowIdx).html(running + ' ' + pending);
			}
		}
		fetchPodInfo (url, apps, namespaces, callback);
	}
	
	
	// app: {"app_version":"2.4.46","name":"apache","namespace":"default","updated":"2020-12-28 15:17:34.913293732 -0600 CST","chart":"apache-8.0.3","revision":"1","status":"deployed"}
	function buildAppCard (idx, bg, icon, app, desc) {
		html = '<div id="panel-' + idx + '">'
			+ '<div class="md-card">'
			+ '	<div class="md-card-head head_background" style="background-image: url(' + bg +')">'
			+ '		<div class="md-card-head-menu">'
			+ '			<div class="md-card-dropdown" data-uk-dropdown="{pos:\'bottom-right\'}">'
			+ '				<i class="md-icon material-icons md-icon-light">&#xE5D4;</i>'
			+ '				<div class="uk-dropdown uk-dropdown-small">'
			+ '					<ul class="uk-nav uk-nav-dropdown">'
			// EXECUTES TWICE ( DON"T KNOW WHY ) + '		<li><a id="linkHelmDelete-' + idx + '" href="#" onclick="return helmDelete(\'' + app.name + '\',\'' + app.app_version + '\',\'' + app.namespace  + '\', function () { deleteComplete() } );">Delete</a></li>'
			+ '						<li><a id="linkDelete-' + idx + '">Delete</a></li>'
			+ '					</ul>'
			+ '				</div>'
			+ '			</div>'
			+ '		</div>'
			//+ '		<h3 class="md-card-head-text"> <a target="_blank" id="name-' + idx + '" class="md-btn md-btn-primary" title="Click to open" href="javascript: open_app(\'' + app.name + '\')">' + app.name + '</a></h3>'
			+ '		<button id="name-' + idx + '" style="margin:5px;" class="md-btn md-btn-primary" title="Click to open" onclick="return open_app(\'' + app.name + '\')">' + app.name + '</button>'
			+ '		<div class="md-card-head-subtext">' + ( icon ? '<img class="ProfileCard-avatar1" src="' + icon + '"> ' : '' ) + '</div>'
			+ '	</div>'
			+ '	<div id="details-' + idx + '" class="panel-body ov-h md-card-content" style="height:140px;">'
			//+ 	( icon ? '<img class="ProfileCard-avatar1" src="' + icon + '"> ' : '' ) 
			+ 	desc
			+ '	</div>'
			+ '</div>'
			+'</div>';
		return html;
	}
	
	// fires when a del op completes
	function deleteComplete() {
		LOGD ('Delet complete.');
		refreshCharts ();
	}
	
	function open_app ( name) {
		LOGD('open app ' + name);
		growl(name + ' is not visible outside the cluster.');
		return false;
	}
	
	/**
	 * matchServicesAndCharts
	 * http://localhost:9080/CloudClusterManager/K8S?node=KubeFoghornLeghorn&op=ListServices
	 * @param apps [{"app_version":"2.4.46","name":"apache","namespace":"default","updated":"2020-12-28 15:17:34.913293732 -0600 CST","chart":"apache-8.0.3","revision":"1","status":"deployed"},{"app_version":"0.35.0","name":"ingress-nginx","namespace":"default","updated":"2020-09-03 16:12:36.360857425 -0500 CDT","chart":"ingress-nginx-2.15.0","revision":"1","status":"deployed"},{"app_version":"v1.10.0","name":"monocular","namespace":"default","updated":"2020-09-03 16:10:51.32986299 -0500 CDT","chart":"monocular-1.4.15","revision":"1","status":"deployed"},{"app_version":"2.20.1","name":"prometheus","namespace":"default","updated":"2020-09-11 07:45:23.379248951 -0500 CDT","chart":"prometheus-11.13.0","revision":"1","status":"deployed"}]
	 */
	function linkApps (apps) {
		var node 		= $('#selNodes').children('option:selected').val();
		var nodeName 	= $('#selNodes').children('option:selected').text(); 	// Name (URL)
		var apiUrl		= nodeName.split(' ')[1].replace(/[\(\)]/g,'');			// https://192.168.40.84:6443/
		var Url			= url + 'node=' + node + '&op=ListServices';
		
		linkPanels (Url, apiUrl, apps);
	}
	
	/**
	 * @param Url Services fetch URL: http://localhost:8080/WebConsole/K8S?node=PRD201&op=ListServices&_=1609099802530
	 * @param TBL1 Data table
	 * @param apiUrl API server used to open apps https://192.168.40.84:6443/ 
	 */
	function linkPanels (Url, apiUrl, apps) {
		LOGD('Match Hem/Services ' + Url + ' ApiUrl:' + apiUrl);
		var posting = $.get (Url);
		
		posting.done(function( json ) {
			//LOGD('All Services: ' + JSON.stringify(json));

			// {data: {}, status: 200, mesage: xxx}
			if ( json.status != 200) {
				return;
			}
			var items = json.data.items;
			
			for ( var rowIdx = 0 ; rowIdx < apps.length ; rowIdx++) {
				// {"app_version":"2.4.46","name":"apache","namespace":"default","updated":"2020-12-28 19:03:33.576423124 -0600 CST","chart":"apache-8.0.3","revision":"1","status":"deployed"}
				var d 		= apps[rowIdx];
				var name	= d.name;		
				
				//LOGD( rowIdx + ' ' + JSON.stringify(d) );
				
				for ( var i = 0 ; i < items.length ; i++ ) {
					var servName	= items[i].metadata.name;
					var spec 		= items[i].spec;
					var type		= spec.type; 	// NodePort, LoadBalancer
					
					if ( type != 'NodePort' && type != 'LoadBalancer' ) {
						continue;
					}
					// [{"protocol":"TCP","port":4369,"name":"epmd","targetPort":"epmd","nodePort":31590},{"protocol":"TCP","port":5672,"name":"amqp","targetPort":"amqp","nodePort":30495},{"protocol":"TCP","port":25672,"name":"dist","targetPort":"dist","nodePort":32441},{"protocol":"TCP","port":15672,"name":"stats","targetPort":"stats","nodePort":30751}]
					var ports		= spec.ports	
					
					// match helm name (kibana-1586100279,nginx-ingress-1585514119) with service name (kibana-1586100279,nginx-ingress-1585514119-controller)
					//LOGD('Compare ' + servName + ' == ' + name);
					if ( servName.indexOf(name) != -1 ) {
						var href 	= parseHref (apiUrl);
						var url 	= buildUrl (href.hostname , ports, ['http', 'stats']);
						//LOGD('Got match ' + name + ' row: ' + rowIdx + ' stype: ' + type + ' Ports: ' + JSON.stringify(ports));
						//LOGD('Got match ' + name + ' row: ' + rowIdx + ' stype: ' + type + ' Url: ' + url);
						if ( url) {
							LOGD(rowIdx + ' <a target="_blank" href="' + url + '">' + name + '</a>');
							//$('#name-' + rowIdx).attr("href", url); // Set herf value
							$('#name-' + rowIdx).attr("onclick", 'window.open("' + url + '","_blank")'); // Set herf value
						}
					}
				}
			}
		});
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

	 function openterm() {
		 var text = $('#selNodes').children('option:selected').text(); // NODE (https://HOST:PORT/)
		 var host = text.substring(text.indexOf('/') + 2, text.indexOf(':', text.indexOf('/') + 1));
		 LOGD('XTerm Text: ' + text + ' Host: ' + host);
		 window.open ('../../ssh/ssh.jsp?host=' + host );
	 }
	 
	</script>
	 	
	<script type="text/javascript" src="helm.js"></script>
	<script type="text/javascript" src="helm-repo.js"></script>
</body>
</html>