<%@page import="com.cloud.docker.DockerSwarm"%>
<%@page import="com.cluster.jsp.JSPDocker"%>
<%@page import="com.cloud.docker.Docker"%>
<%@page import="com.cloud.docker.DockerNode"%>
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
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	//String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	final String action			= request.getParameter("action");
	final String name			= request.getParameter("name");			// swarm id
	final String master			= DockerSwarm.findManager(name);
	final DockerNode node		= master != null ? Docker.get(master) : null;
	final String pageTitle		= "Manage Swarm <small>" + name + "</small>";
	
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
	<jsp:param value="../../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="<%=pageTitle%>" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../../css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="../../../css/jquery.json-viewer.css">
<link rel="stylesheet" type="text/css" href="../typeahead.css">
<link rel="stylesheet" type="text/css" href="../../../css/snackbar.css">

<style type="text/css">
</style>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
		<jsp:param value="../../../" name="commonPath"/>
		<jsp:param value="<%=pageTitle%>" name="pageTitle"/>
		<jsp:param value="Home,Swarms,Manage" name="crumbLabels"/>
		<jsp:param value="../../../index.jsp,view.jsp,class_active" name="crumbLinks"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../../\") %>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>

<!-- CONTENT -->
	<div class="row">
		<div class="col-md-12">
		<div class="tab-container tab-default">
		
			<ul class="nav nav-tabs">
				<li id="tabServices" class="active"><a href="#tab1" data-toggle="tab">Services</a></li>
				<li id="tabTasks"><a href="#tab2" data-toggle="tab">Tasks</a></li>
			</ul>
			
			<div class="tab-content">
			
				<!-- tab1: services -->
				<div class="tab-pane active" id="tab1">
					<!-- panel panel-default -->
					<div class="" data-widget='{"draggable": "false"}'>
						<div class="panel-controls dropdown" style="float: right;">
							
		                    <button id="btnRefreshServices" class="btn btn-icon-rounded refresh-panel" onclick="javascript:TBL1.ajax.reload();"><span class="material-icons inverted">refresh</span></button>
		                    
		                    <button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>
		                    <ul class="dropdown-menu" role="menu">
		                        <li><a href="" data-toggle="modal" data-target="#modal5" onclick="return onServiceCreate()">Create Service</a></li>
		                        <!-- 
		                        <li><a href="">Something</a></li>
		                        -->
		                        <li class="divider"></li>
		                        <li><a target="_blank" href="https://docs.docker.com/get-started/part3/">About Docker Services</a></li>
		                    </ul>
		                </div>						
						<div class="panel-body">
							
							<div>
								Toggle column: <a class="toggle-vis" data-column="2" data-table="TBL1">Id</a> &nbsp;&nbsp;<a class="toggle-vis" data-column="3" data-table="TBL1">Updated</a>
							</div>
							
							<table id="tblServices" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<th>Name</th><th>Version</th><th>Id</th><th>Updated</th><th>Container Spec</th><th>Endpoint</th><th>Action</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>				
						</div>
					</div>
				</div>
				
				<!-- tab2: tasks -->
				<div class="tab-pane" id="tab2">
					<!-- panel panel-default -->
					<div class="" data-widget='{"draggable": "false"}'>
						<div class="panel-controls dropdown" style="float: right;">
							
							<button id="btnRefreshTasks" class="btn btn-icon-rounded refresh-panel" onclick="javascript:TBL2.ajax.reload();"><span class="material-icons inverted">refresh</span></button>
		                    <button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>

		                    <ul class="dropdown-menu" role="menu">
		                    	<!-- 
		                        <li><a href="" data-toggle="modal" data-target="#modal2">Create</a></li>
		                        <li class="divider"></li>
		                        -->
		                        <li><a target="_blank" href="https://docs.docker.com/engine/swarm/how-swarm-mode-works/swarm-task-states/">About Docker Tasks</a></li>
		                    </ul>
		                </div>						
						<div class="panel-body">
							
							<div>
								Toggle column: <a class="toggle-vis" data-column="1" data-table="TBL2">Id</a>&nbsp;&nbsp;&nbsp;<a class="toggle-vis" data-column="2" data-table="TBL2">Created</a>
							</div>
							
							<table id="tblTasks" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<th>Version</th><th>Id</th><th>Created</th><th>Status</th><th>Container Spec</th><th>Action</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>				
						</div>
					</div>
				</div>
				<!-- end tab2 -->
				

								
			</div>
			<!-- end tab content -->
		
		</div>
		<!-- tab-container -->
		
		</div>
	</div>	
		
	<jsp:include page="tile_modals.jsp">
		<jsp:param value="<%=name%>" name="swarm"/>
	</jsp:include>

	
<!-- END CONTENT -->

	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
		<jsp:param value="../../../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../../../js/jquery.dataTables.js"></script>
	<script type="text/javascript" src="../../../js/poll.js"></script>
	<script type="text/javascript" src="../../../js/jquery.json-viewer.js"></script>
	<script type="text/javascript" src="../../../js/snackbar.js"></script>
	
	<!-- typeahead -->
	<script type="text/javascript" src="../../../js/handlebars.min.js"></script>
	<script id="result-template" type="text/x-handlebars-template">
		<div class="ProfileCard u-cf"> 
			{{#if logo_url.small}}
			<img class="ProfileCard-avatar" src="{{logo_url.small}}">
			{{else}}
			<i class="fab fa-docker fa-2x" style="color:rgb(66, 66, 66); float: left"></i>
			{{/if}}
			<div class="ProfileCard-details">
				<div class="ProfileCard-realName">{{name}}</div>
				<div class="ProfileCard-description">{{short_description}}</div>
			</div>
			<div class="ProfileCard-stats">
				<div class="ProfileCard-stat"><span class="ProfileCard-stat-label">Downloads:</span> {{popularity}}</div>
				{{#if operating_systems.[0].name}}
				<div class="ProfileCard-stat"><span class="ProfileCard-stat-label">OS:</span> {{operating_systems.[0].name}}</div>
				{{/if}}
				{{#if categories.[0].label}}
				<div class="ProfileCard-stat"><span class="ProfileCard-stat-label">Type:</span> {{categories.[0].label}}</div>
				{{/if}}
			</div>
		</div>
	</script>
	<script id="empty-template" type="text/x-handlebars-template">
		<div class="EmptyMessage">Your search turned up 0 results.</div>
	</script>
	
	<script>
	
	var TBL1;	// services
	var TBL2;	// tasks
	
	var swarmId	= '<%=name%>';
	var url 	= '<%=contextPath%>/Docker?swarmId=' + swarmId + '&node=<%=master%>';
	
	// Fires when the create servie link is clicked.
	function onServiceCreate () {
		// load images into the 'Image' SELECT
		/*
		$('#selImage').empty();
		TBL2.rows().every( function ( rowIdx, tableLoop, rowLoop ) {
			// {"ParentId":"","RepoDigests":["busybox@sha256:061ca9704a714ee3e8b80523ec720c64f6209ad3f97c0ff7cb9ec7d19f15149f"],"SharedSize":-1,"VirtualSize":1199417,"RepoTags":["busybox:latest"],"Size":1199417,"Containers":-1,"Labels":null,"Id":"sha256:d8233ab899d419c58cf3634c0df54ff5d8acc28f8173f09c21df4a07229e1205","Created":1550189977}
			var data = this.data();
			$.each(data.RepoTags, function (key, entry) {
				$('#selImage').append($('<option></option>').attr('value', entry).text(entry));
			});
		}); */
		return false;
	} 
	
	// Initialization: recalculate data tables colums widths: {table}.columns.adjust().draw();
	$(document).ready(function() {
		
		initializeDataTables (); 
		
		// modified pacifier: .refresh-panel , .tab-pane , interval
		register_pacifier('.refresh-panel', '.tab-pane', 1500);
		
		// show search
		//$('#search-box').show();
		//$('#search-input').attr("placeholder", "Search/Install images from Docker Hub...");
		
       	$('#tabTasks').click ( function () {
			setTimeout('TBL2.columns.adjust().draw()', 200); 
    	}); 
      	$('#tabServices').click ( function () {
			setTimeout('TBL1.columns.adjust().draw();', 200);
    	}); 

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
	

	// name = OperationnName, id = service id, data = payload
	function serviceAction (name, id, data) {
		var uri 	= url + '&op=' + name + '&Id=' + id;
		var method 	= name.indexOf('Inspect') != -1 ? 'GET' : 'POST';
		var data	= data || '';
		
		LOGD(name + " " + method + " Url:" + uri + ' data:' + data);
		
		//var posting = $.post( uri , '');
		var posting = $.ajax( { method: method , url: uri, data : data } );
		
		posting.done(function( data ) {
			LOGD("Resp: " + JSON.stringify(data));
			var error	= ( data.status && (data.status >= 400)) ? true : false;	
			var type  	=  error ? 'danger' : 'info';
			
			if ( error && data.message ) {
				growl (data.message, type);
				return;
			}
			// {"message":"OK","status":200,"data":""} - refresh
			$('#btnRefreshServices').click();
			
			if ( name.indexOf('Inspect') != -1 ) {
				$('#inspectTitle').html('<small>' + id + '</small>');
				$('#json-renderer').jsonViewer(data.data);
				$('#btnInspect').click();
			}
		});
		return false;
	}

	</script>
	 
	<script type="text/javascript" src="manage.js"></script>
	
</body>
</html>