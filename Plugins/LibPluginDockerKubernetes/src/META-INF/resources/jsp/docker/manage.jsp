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
	
	final String addr			= request.getParameter("addr");		// comming from swarms.jsp
	String name					= request.getParameter("name");		// comming from nodes.jsp

	final DockerNode node		= name != null ? Docker.get(name) : addr != null ? Docker.getByAddr(addr) : null;
	if ( name == null && node != null) {
		name = node.getName();
	}
	final String pageTitle		= "Manage " + name + "<small>" + node.getHostPort() + "</small>";
	final String[] crumbs		= addr != null ? new String[] {"Home,Docker Swarms,Manage", "../../index.jsp,swarm/view.jsp,class_active"} 
		: new String[] {"Home,Docker Nodes,Manage", "../../index.jsp,nodes.jsp,class_active"};
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Docker" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="../../css/jquery.json-viewer.css">
<link rel="stylesheet" type="text/css" href="typeahead.css">
<link rel="stylesheet" type="text/css" href="../../css/snackbar.css">

<style type="text/css">
/* 6/2/2019
.dataTables_filter {
   float: left !important;
} */
</style>

</head>
<body>

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
		<div class="col-md-12">
		<div class="tab-container tab-default">
		
			<ul class="nav nav-tabs">
				<li id="tabImages" class="active"><a href="#tab2" data-toggle="tab">Images</a></li>
				<li id="tabContainers"><a href="#tab1" data-toggle="tab">Containers</a></li>
				<li id="tabNetworks"><a href="#tab3" data-toggle="tab">Networks</a></li>
				<li id="tabVolumes"><a href="#tab4" data-toggle="tab">Volumes</a></li>
			</ul>
			
			<div class="tab-content">
			
				<!-- tab1: containers -->
				<div class="tab-pane" id="tab1">
					<!-- panel panel-default -->
					<div class="" data-widget='{"draggable": "false"}'>
						<div class="panel-controls dropdown" style="float: right;">
							
		                    <button id="btnRefreshContainers" class="btn btn-icon-rounded refresh-panel" onclick="javascript:TBL1.ajax.reload();"><span class="material-icons inverted">refresh</span></button>
		                    
		                    <button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>
		                    <ul class="dropdown-menu" role="menu">
		                        <li><a href="" data-toggle="modal" data-target="#modal1" onclick="return onContainerCreate()">Create Container</a></li>
		                        <!-- 
		                        <li><a href="">Something</a></li>
		                        -->
		                        <li class="divider"></li>
		                        <li><a target="_blank" href="https://www.docker.com/resources/what-container">About Docker Containers</a></li>
		                    </ul>
		                </div>						
						<div class="panel-body">
							<!-- <br/> -->
							<div>
								Toggle column: <a class="toggle-vis" data-column="1" data-table="TBL1">Id</a> &nbsp;&nbsp;<a class="toggle-vis" data-column="2" data-table="TBL1">Image</a>
							</div>
							<table id="tblContainers" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<th>Name</th><th>Id</th><th>Image</th><th>Command</th><th>Status</th><th>Action</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>				
						</div>
					</div>
				</div>
				
				<!-- tab2: images -->
				<div class="tab-pane active" id="tab2">
					<!-- panel panel-default -->
					<div class="" data-widget='{"draggable": "false"}'>
						<div class="panel-controls dropdown" style="float: right;">
							
							<button id="btnRepoBrowser" data-toggle="modal" data-target="#modal5" class="btn btn-icon-rounded" title="Repo browser."><span class="material-icons inverted">cloud_download</span></button> 
							<button id="btnRefreshImages" class="btn btn-icon-rounded refresh-panel" onclick="javascript:TBL2.ajax.reload();"><span class="material-icons inverted">refresh</span></button>
		                    <button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>

		                    <ul class="dropdown-menu" role="menu">
		                        <li><a href="" data-toggle="modal" data-target="#modal2">Create Image</a></li>
		                        <!-- 
		                        <li><a href="">Something</a></li>
		                        -->
		                        <li class="divider"></li>
		                        <li><a target="_blank" href="https://docs.docker.com/get-started/#images-and-containers">About Docker Images</a></li>
		                    </ul>
		                </div>						
						<div class="panel-body">
							<div>
								Toggle column: <a class="toggle-vis" data-column="1" data-table="TBL2">Id</a>
							</div>
							<table id="tblImages" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<th>Tag</th><th>Id</th><th>Created</th><th>Size</th><th>Action</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>				
						</div>
					</div>
				</div>
				<!-- end tab2 -->
				
				<!-- tab3 networks -->
				<div class="tab-pane" id="tab3">
					<div class="" data-widget='{"draggable": "false"}'>
						<div class="panel-controls dropdown" style="float: right;">
							
							<button id="btnRefreshNetworks" class="btn btn-icon-rounded refresh-panel" onclick="javascript:TBL3.ajax.reload();"><span class="material-icons inverted">refresh</span></button>
		                    <!-- 
		                    <button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>

		                    <ul class="dropdown-menu" role="menu">
		                        <li><a href="" data-toggle="modal" data-target="#modal2">Create</a></li>
		                        <li class="divider"></li>
		                        <li><a href="">Separated link</a></li>
		                    </ul> -->
		                </div>						
						<div class="panel-body">
							<br/>
							<table id="tblNetworks" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<th>Name/Driver</th><th>Ingress/Internal/Attachable</th><th>Options</th><th>IPAM</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>				
						</div>
					</div>
				</div>
				<!-- end tab3 -->

				<!-- tab4 volumes -->
				<div class="tab-pane" id="tab4">
					<div class="" data-widget='{"draggable": "false"}'>
						<div class="panel-controls dropdown" style="float: right;">
							
							<button id="btnRefreshVolumes" class="btn btn-icon-rounded refresh-panel" onclick="javascript:TBL4.ajax.reload();"><span class="material-icons inverted">refresh</span></button>
		                </div>						
						<div class="panel-body">
							<br/>
							<table id="tblVolumes" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<th>Driver/Scope</th><th>Created</th><th>Mount Point</th><th>Action</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>				
						</div>
					</div>
				</div>
				<!-- end tab4 -->
								
			</div>
			<!-- end tab content -->
		
		</div>
		<!-- tab-container -->
		
		</div>
	</div>	
		
	<form id="frm1" onsubmit="return false">
		<jsp:include page="tile_modal_container.jsp">
			<jsp:param value="<%=name%>" name="node"/>
		</jsp:include>
	</form>

	<form id="frm2" onsubmit="return false">
		<jsp:include page="tile_modal_image.jsp">
			<jsp:param value="<%=name%>" name="node"/>
		</jsp:include>
	</form>
	
	<!-- repo-browser modal -->
	<jsp:include page="tile_modal_repos.jsp">
		<jsp:param value="<%=name%>" name="node"/>
		<jsp:param value="docker" name="provider"/>
	</jsp:include>
	
<!-- END CONTENT -->

	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>
	<script type="text/javascript" src="../../js/poll.js"></script>
	<script type="text/javascript" src="../../js/jquery.json-viewer.js"></script>
	<script type="text/javascript" src="../../js/snackbar.js"></script>
	
	<!-- typeahead -->
	<script type="text/javascript" src="../../js/handlebars.min.js"></script>
	<script id="result-template" type="text/x-handlebars-template">
		<div class="ProfileCard u-cf"> 
			{{#if logo_url.small}}
			<img class="ProfileCard-avatar" src="{{logo_url.small}}">
			{{else}}
			<i class="fab fa-docker fa-2x" style="color:rgb(66, 66, 66); float: left"></i>
			{{/if}}
			<div class="ProfileCard-details">
				<div class="ProfileCard-realName">{{name}}</div>
				<div class="ProfileCard-description">{{short_description}} &nbsp;&nbsp;<a class="ProfileCard-link" href="#" onclick="return hub_install('{{name}}')">INSTALL</a>
				{{#ifeq source "publisher"}}
					&nbsp;&nbsp;&nbsp;&nbsp;<a class="ProfileCard-link" target="_blank" href="https://hub.docker.com/_/{{slug}}">MORE</a> 
				{{/ifeq}}
				{{#ifeq source "library"}}
					&nbsp;&nbsp;&nbsp;&nbsp;<a class="ProfileCard-link" target="_blank" href="https://hub.docker.com/_/{{slug}}">MORE</a> 
				{{/ifeq}}
				{{#ifeq source "community"}}
					&nbsp;&nbsp;&nbsp;&nbsp;<a class="ProfileCard-link" target="_blank" href="https://hub.docker.com/r/{{name}}">MORE</a> 
				{{/ifeq}}
				</div>
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
	
	var TBL1;	// containers
	var TBL2;	// images
	var TBL3;	// networks
	var TBL4;	// volumes
	
	var node	= '<%=name%>';
	var url 	= '<%=contextPath%>/Docker?node=' + node;
	var nodeIP	= '<%=node.getHostPort().split(":")[0]%>';
	
	function onContainerCreate () {
		// load images into the 'Image' SELECT
		$('#selImage').empty();
		TBL2.rows().every( function ( rowIdx, tableLoop, rowLoop ) {
			// {"ParentId":"","RepoDigests":["busybox@sha256:061ca9704a714ee3e8b80523ec720c64f6209ad3f97c0ff7cb9ec7d19f15149f"],"SharedSize":-1,"VirtualSize":1199417,"RepoTags":["busybox:latest"],"Size":1199417,"Containers":-1,"Labels":null,"Id":"sha256:d8233ab899d419c58cf3634c0df54ff5d8acc28f8173f09c21df4a07229e1205","Created":1550189977}
			var data = this.data();
			
			if ( !data.RepoTags || !Array.isArray(data.RepoTags) ) {
				return;
			}
			$.each(data.RepoTags, function (key, entry) {
				$('#selImage').append($('<option></option>').attr('value', entry).text(entry));
			});
		});
		return false;
	}
	
	// Initialization: recalculate data tables colums widths: {table}.columns.adjust().draw();
	$(document).ready(function() {
		
		initializeDataTables (node);
		
		// modified pacifier: .refresh-panel , .tab-pane , interval
		register_pacifier('.refresh-panel', '.tab-pane', 1500);
		
		// show search
		$('#search-box').show();
		$('#search-input').attr("placeholder", "Search/Install images from Docker Hub...");
		//Utility.toggle_search()
		
       	$('#tabImages').click ( function () {
			setTimeout('TBL2.columns.adjust().draw()', 200); 
    	});
      	$('#tabContainers').click ( function () {
			setTimeout('TBL1.columns.adjust().draw();', 200);
    	});
      	$('#tabVolumes').click ( function () {
			setTimeout('TBL4.columns.adjust().draw();', 200);
    	});
      	
      	initModalImage();
		initializeReposDialog();
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
	
	function pacify_start (closest, interval) {
        var panel = $(closest); //$(selector).closest(closest); //'.tab-pane');
        panel.append('<div class="panel-loading"><div class="panel-loader-circular"></div></div>');
        setTimeout(function () {
            panel.find('.panel-loading').remove();
        }, interval) 
	}
	
	function pacify_stop (closest) {
        $(closest).find('.panel-loading').remove(); 
	}

	
	</script>

	<script type="text/javascript" src="manage.js"></script>

</body>
</html>