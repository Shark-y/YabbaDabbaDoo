<%@page import="com.cloud.core.io.JarResourceLoader"%>
<%@page import="com.cluster.jsp.JSPK8S"%>
<%@page import="java.net.URL"%>
<%@page import="com.cloud.kubernetes.K8SNode"%>
<%@page import="com.cloud.kubernetes.Kubernetes"%>
<%@page import="java.io.IOException"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
static final String TILE_DT		= "../../tiles/tile_datatable.jsp";
%>

<%
	final String contextPath 	= getServletContext().getContextPath();
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	//String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	
	//final String addr			= request.getParameter("addr");		// comming from swarms.jsp
	String name					= request.getParameter("name");		// comming from nodes.jsp

	final K8SNode node			= name != null ? Kubernetes.get(name) : null;
	if ( name == null && node != null) {
		name = node.getName();
	}
	final String host			= (new URL(node.getApiServer())).getHost();
	final String toolBar 		= JSPK8S.buildManangeToolbar(node);		
	final String pageTitle		= "Manage " + name + " &nbsp;&nbsp;" + toolBar + " <small class=\"uk-text-small\">" + node.getApiServer() + "</small>";
	final String[] crumbs		= new String[] {"Home,API Servers,Manage", "../../index.jsp,nodes.jsp,class_active"};
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Kubernetes" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../css/jquery.json-viewer.css">
<link rel="stylesheet" type="text/css" href="../docker/typeahead.css">
<link rel="stylesheet" type="text/css" href="../../css/snackbar.css">

<style type="text/css">
/* altair input place holder color */
.uk-form ::placeholder {
  color: white;
  opacity: 1; /* Firefox */
}
.twitter-typeahead {
  width: 100%;
}
</style>

</head>
<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=pageTitle%>" name="pageTitle"/>
		<jsp:param value="<%=crumbs[0]%>" name="crumbLabels"/>
		<jsp:param value="<%=crumbs[1]%>" name="crumbLinks"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>

<!-- CONTENT -->

	<div class="row">
		<div class="col-md-12 md-card">
		<div class="tab-container tab-default md-card-content uk-width-1-1">
		
			<ul class="nav nav-tabs uk-tab" data-uk-tab="{connect:'#tab1'}">
				<li id="tabApps" class="active"><a href="#tab0" data-toggle="tab">Apps</a></li>
				<li id="tabNodes"><a href="#tab1" data-toggle="tab">Nodes</a></li>
				<li id="tabPods"><a href="#tab2" data-toggle="tab">Pods</a></li>
				<li id="tabServices"><a href="#tab3" data-toggle="tab">Services</a></li>
				<li id="tabDeployments"><a href="#tab4" data-toggle="tab">Deployments</a></li>
				<li id="tabSecrets"><a href="#tab5" data-toggle="tab">Secrets</a></li>
				<li id="tabEvents"><a href="#tab6" data-toggle="tab">Events</a></li>
				<li id="tabStorage"><a href="#tab7" data-toggle="tab">Persistence</a></li>
			</ul>
			
			<div class="tab-content">
				<!-- tab0: Apps -->
				<% if ( SkinTools.isAltAirTheme()) { %>
				<ul id="tab1" class="uk-switcher uk-margin" style="overflow: unset">
					<li>
						<br/>
				<% } else { %>
				<div class="tab-pane active" id="tab0">
				<% } %>	

					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_apps.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>

				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				
				<!-- tab1: Nodes -->
				<div class="tab-pane" id="tab1">
				<% } %>

					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_nodes.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>
							
				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				
				<!-- tab2: Pods -->
				<div class="tab-pane" id="tab2">
				<% } %>

					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_pods.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>
				
				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				<!-- end tab2 -->
				
				<!-- tab3 services -->
				<div class="tab-pane" id="tab3">
				<% } %>

					<jsp:include page="<%=TILE_DT %>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_services.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>

				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				
				<!-- end tab3 -->

				<!-- tab4 Deployments -->
				<div class="tab-pane" id="tab4">
				<% } %>

					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_deployments.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>
					
				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				<!-- end tab4 -->

				<!-- tab5 Secrets -->
				<div class="tab-pane" id="tab5">
				<% } %>

					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_secrets.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>
					
				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				<!-- end tab5 -->

				<!-- tab6 Events -->
				<div class="tab-pane" id="tab6">
				<% } %>

					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_events.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>
					
				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
					<li>
						<br/>
				<% } else { %>	
				</div>
				<!-- end tab6 -->

				<!-- tab7 Storage: PV, PVCs -->
				<div class="tab-pane" id="tab7">
				<% } %>
				
					<jsp:include page="<%=TILE_DT%>">
						<jsp:param value="<%=JarResourceLoader.getTextResource(\"/resources/ui/dt_storage.json\", 3).getText()%>" name="descriptor"/>
					</jsp:include>
					
				<% if ( SkinTools.isAltAirTheme()) { %>
					</li>
				</ul>
				<% } else { %>	
				</div>
				<!-- end tab7 -->
				<%} %>
								
			</div>
			<!-- end tab content -->
		
		</div>
		<!-- tab-container -->
		
		</div>
	</div>	
	
	<!--  ace code editor @ https://ace.c9.io -->	
	<script src="../../lib/ace/ace.js"></script>
	<script src="../../lib/ace/ext-searchbox.js"></script>
	
	<!--  misc modals -->
	<jsp:include page="tile_modal_manage.jsp">
		<jsp:param value="<%=name%>" name="node"/>
	</jsp:include>

	<!--  misc modals -->
	<jsp:include page="tile_modal_ymlres.jsp">
		<jsp:param value="<%=name%>" name="node"/>
	</jsp:include>

	<!-- repo-browser modal -->
	<jsp:include page="../docker/tile_modal_repos.jsp">
		<jsp:param value="<%=name%>" name="node"/>
		<jsp:param value="kube" name="provider"/>
	</jsp:include>
	
	<!-- modal 4 Add repo -->
	<jsp:include page="helm/tile_helm_addrepo.jsp" />
	
<!-- END CONTENT -->

	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>
	
	<script type="text/javascript" src="../../js/poll.js"></script>
	<script type="text/javascript" src="../../js/jquery.json-viewer.js"></script>
	<script type="text/javascript" src="../../js/snackbar.js"></script>

	<!-- typeahead 	-->
	<jsp:include page="tile_type_ahead.jsp">
		<jsp:param value="../../" name="basePath"/>
	</jsp:include>
	
	
	<script>
	
	var node	= '<%=name%>';
	var url 	= '<%=contextPath%>/K8S?node=' + node;
	var nodeIP	= '<%=node.getHostName()%>';
	
	
	// Initialization: recalculate data tables colums widths: {table}.columns.adjust().draw();
	$(document).ready(function() {
		
		initializeDataTables (node, '<%=node.getSSHUser()%>', '<%=node.getSSHPwd()%>');
		
		// modified pacifier: .refresh-panel , .tab-pane , interval
		register_pacifier('.refresh-panel', '.tab-pane', 1500);
		
		// show search
		$('#search-box').show();
		$('#search-input').attr("placeholder", "Search for Apps...");
		$('#search-input-node').val(node);

      	$('#tabApps').click ( function () {
			setTimeout('tblCharts.columns.adjust().draw();', 200);
    	});
      	$('#tabNodes').click ( function () {
			setTimeout('tblNodes.columns.adjust().draw();', 200);
    	});
       	$('#tabPods').click ( function () {
       		setTimeout('tblPods.columns.adjust().draw()', 200);
    	}); 
      	$('#tabServices').click ( function () {
      		setTimeout('tblServices.columns.adjust().draw()', 200);
    	}); 
      	$('#tabDeployments').click ( function () {
			//setTimeout('TBL4.columns.adjust().draw()', 200);
      		setTimeout('tblDeployments.columns.adjust().draw()', 200);
    	});
      	$('#tabSecrets').click ( function () {
			//setTimeout('TBL5.columns.adjust().draw()', 200);
      		setTimeout('tblSecrets.columns.adjust().draw()', 200);
    	});
      	$('#tabEvents').click ( function () {
			//setTimeout('TBL6.columns.adjust().draw()', 200);
      		setTimeout('tblEvents.columns.adjust().draw()', 200);
    	});  
      	$('#tabStorage').click ( function () {
			//setTimeout('TBL7.columns.adjust().draw()', 200);
      		setTimeout('tblStorage.columns.adjust().draw()', 200);
    	});  
      	// repo browser
      	initializeReposDialog();
      	initializeTypeAhead();
      	
      	refreshCharts ();
	});
	
	function register_pacifier (selector, closest, interval) {
		$(selector).click(function () {
	        var panel = $(this).closest(closest); 
	        panel.append('<div class="panel-loading"><div class="panel-loader-circular"></div></div>');
	        setTimeout(function () {
	            panel.find('.panel-loading').remove();
	        }, interval) 
	    }); 
	}

	/**
	 * matchServicesAndCharts
	 * http://localhost:9080/CloudClusterManager/K8S?node=KubeFoghornLeghorn&op=ListServices
	 */
	function findApps () {
		var apiUrl		= '<%=node.getApiServer()%>';
		var Url			= url  + '&op=ListServices';
		
		LOGD('FindApps node: ' + node + ' Url: ' + Url + ' Api Srv: ' + apiUrl);
		wrapApps (Url, tblCharts, apiUrl);
	}

	// fires when the refresh charts btn is clicked.
	function refreshCharts () {
		LOGD('Reload charts  node: ' + node);
		tblCharts.ajax.reload();
		
		setTimeout(findApps, 4000);
	}

	</script>

	<script src="../../js/base64.js"></script>

	<script type="text/javascript" src="common.js"></script>
	<script type="text/javascript" src="manage.js"></script>
	<script type="text/javascript" src="secrets.js"></script>
	<script type="text/javascript" src="events.js"></script>
	<script type="text/javascript" src="storage.js"></script>
	<script type="text/javascript" src="deployments.js"></script>

	<script type="text/javascript" src="helm/helm.js"></script>
	<script type="text/javascript" src="helm/helm-repo.js"></script>
	
	<script>
	/* https://ace.c9.io/#nav=howto
	 *  editor.setValue("the new text here");
	 *	editor.session.setValue("the new text here"); // set value and reset undo history
	 * 	editor.getValue(); // or session.getValue
	*/
	var editor 		= ace.edit("json-editor");
	editor.getSession().setUseWrapMode(true);
	editor.session.setMode("ace/mode/json");
	
	/**
	 * Helm hub install
	 * Chart JSON {"hubUrl":"https://artifacthub.io","id":"bitnami/apache","version":"8.0.3","advanced":false,"params":"","ns":"","icon":"/image/ee08f467-cfcd-4831-b313-711d5b088acb","repo":"https://charts.bitnami.com/bitnami","desc":"Chart for Apache HTTP Server"}
	 * @param hubUrl Hub intsall/doc url: https://hub.helm.sh/ or https://192.168.40.84:32543/
	 * @param chart Chart name
	 * @param version Version
	 * @param advanced Optional If true open the advanced dialog to aks for install params
	 * @param params Optional user entered params from advanced dialog: key1=val1,key2=val2,...
	 * @param ns Optional namespace
	 * @param icon Optional chart icon name (<img src="{{hubUrl}}{{attributes.icon}}">)
	 * @param repo HELm repo URL
	 * @returns {Boolean}
	 */
	 //function hub_install (hubUrl, chart, version, advanced, params, ns, icon, repo) {
	 function hub_install (chart) {
		var node 		= '<%=name%>';
		LOGD('Manane.jsp: Node ' + node + ' Chart: ' + JSON.stringify(chart));
		
		var callback 	=  function (data) {
			$('#modal8InstallSpinner').hide();
			$('#btn_refresh_tblCharts').click();
			
			if ( data.status >= 400) {
				if ( modal8IsVisible ()) {
					modal8SetStatus(data.message, 'danger');
				}
				else {
					growl(data.message, 'danger');
				}
			}
		} 
	
		//chart.chart		= chart.id;
		chart.basePath 	= '../../';
		chart.node		= node;
		chart.callback	= callback;
		
		hubInstall ( chart );
	 }
	
	function hash3DDetails ( id, title , HASH, k1, k2 , large) {
		large	= large || false;
		//LOGD('hash=' + HASH);
		var json = HASH[id][k1][k2];
		showInspectModal (title + ' for ' + id , 'json', json, false, large);
		return false;	
	}
	function hash2DDetails ( id, title , HASH, k1, editor, large) {
		//LOGD('hash=' + HASH);
		editor 		= editor || false;
		large		= large || false;
		
		var json 	= HASH[id][k1];
		showInspectModal (title + ' for ' + id , 'json', json, editor, large);
		return false;	
	}
	
	function getPodLogs(pod, namespace) {
		var logsUrl 	= url + '&op=GetPodLogs&pod=' + pod + '&namespace=' + namespace;
		LOGD('Get logs Pod:' + pod +  ' NS:' + namespace + ' Url:' + logsUrl);
		
		// USe the docker logs page
		// http://localhost:9080/CloudClusterManager/jsp/docker/logs.jsp?node=Kube1NACR208&op=ContainerLogs&Id=a9be9ca6b5655a48dde9cc3b44c46b6eca9db8dbc37d224557a1b5c53a12750a
		window.open("../docker/logs.jsp?node=" + node + '&system=k8s&op=GetPodLogs&pod=' + pod + '&namespace=' + namespace, "_blank");
		return false;
	}
	
	
	</script>

</body>
</html>