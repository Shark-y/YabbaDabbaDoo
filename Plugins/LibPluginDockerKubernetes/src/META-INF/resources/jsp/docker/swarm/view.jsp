<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
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
	final String pageTitle		= "Swarms <i class=\"material-icons\">blur_on</i>";
	
	final String wsUrl 			= (request.getScheme().equals("http") ? "ws://" : "wss://")
		+ request.getServerName() + ":" + request.getServerPort() + contextPath;

	// all nodes
	Map<String, DockerNode> nodes 	= Docker.getClusters();
	Map<String, JSONObject> swarms 	= Docker.getSwarms();
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
	<jsp:param value="../../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Docker Swarms" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../../css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="../../../css/jquery.json-viewer.css">
<link rel="stylesheet" type="text/css" href="../typeahead.css">
<link rel="stylesheet" type="text/css" href="../../../css/snackbar.css">

</head>
<body>

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
	<div class="row">
		<div class="col-md-12">
		
			<!-- Main panel -->
			<div class="panel panel-default" data-widget='{"draggable": "false"}'>
				<div class="panel-controls dropdown">
					
					<button id="btnRefreshContainers" class="btn btn-icon-rounded refresh-panel" onclick="TBL1.ajax.reload()"><span class="material-icons inverted">refresh</span></button>
				
		            <button class="btn btn-icon-rounded dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="" data-toggle="modal" data-target="#modal1">Initialize Swarm</a></li>
                        <li><a href="" data-toggle="modal" data-target="#modal2">Join Swarm</a></li>
                        <li><a href="" data-toggle="modal" data-target="#modal3">Leave Swarm</a></li>
						<li class="divider"></li>
 						<li><a target="_blank" href="https://docs.docker.com/engine/swarm/">About Swarms</a></li>
                    </ul>
				</div>
				<div class="panel-heading">
					<h2>Clusters</h2>
					<!--  
					<div class="panel-ctrls">
						<button id="btnAdd" class="btn btn-raised" data-toggle="modal" data-target="#modal1">Add</button>
					</div>					
					-->
				</div>
				<div class="panel-body">
					<table id="tblSwarms" class="table m-n" style="width: 100%">
						<thead>
							<tr>
								<th></th>
								<th>Id</th>
								<th>Manager</th>
								<th>Workers</th>
								<th>Action</th>
							</tr>
						</thead>
						<tbody>
						
						</tbody>
					</table>
				</div>		
				<!-- panel body -->
			</div>
			<!-- main panel -->
			
		</div>
	</div>	
		
	<jsp:include page="tile_modals.jsp">
		<jsp:param value="bar" name="foo"/>
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
	
	
	<script>
	var TBL1;
	var baseUrl = '<%=contextPath%>/Docker';
	
	// level: info, danger, warning, success
	function growl ( text, level, delay) {
		level = level || 'info';
		delay = delay || 30000;
		$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
	}
	
	function nodes_onchange() {
		$('#AdvertiseAddr').val($('#selNode').children('option:selected').val().replace(/237./,'2377'));
		$('#AdvertiseAddr1').val($('#selNode1').children('option:selected').val().replace(/237./,'2377'));
	} 
	
	function swarm_onchange (remotes) {
		var swarmId = $('#selSwarm').children('option:selected').val();
		$('#RemoteAddrs').val(remotes[swarmId]);
	}
	
	function loadNodes() {
		<% for ( Map.Entry<String, DockerNode> entry : nodes.entrySet()) { 
			DockerNode node = entry.getValue();
		%>
		$('.node').append($('<option></option>').attr('value', '<%=node.getHostPort()%>').text('<%=node.getName()%>'));
		<% } %>
		$('.node').change(function(){
			nodes_onchange()	
		});
		nodes_onchange();
		
		// swarm modal events
		var REMOTES = [];
		<% for ( Map.Entry<String, JSONObject> entry : swarms.entrySet()) { 
			JSONObject swarm 	= entry.getValue();
			String master 		= swarm.getString("Master");
			String url			= Docker.get(master).getHostPort().replaceFirst("237.", "2377");
		%>
		REMOTES['<%=entry.getKey()%>'] = '<%=url%>';
		$('#selSwarm').append($('<option></option>').attr('value', '<%=entry.getKey()%>').text('<%=entry.getKey()%>'));
		<% } %>
		$('#selSwarm').change(function(){
			swarm_onchange (REMOTES);
		});
		swarm_onchange (REMOTES);
	}
	
	function inspect (swarmId, node) {
		return swarmAction ('SwarmInspect', swarmId, node);
	}

	function removeSwarm (swarmId ) {
		return swarmAction ('SwarmRemove', swarmId, '', '', true, false);
	}

	function swarmAction (op, id, node, payload, refresh, notify) {
		payload 	= payload || '';		// optional
		refresh		= refresh || false;		// optional 
		notify		= notify  || false;		// optional
		
		var uri 	= baseUrl + '?op=' + op + '&Id=' + id + '&node=' + node;
		LOGD(op + " " + uri);
		
		var posting = $.post( uri , payload);
		posting.done(function( data ) {
			// {"message":"OK","status":200,"warnings":["Node3 This node is not part of a swarm","Node4 This node is not part of a swarm"]}
			LOGD("Resp: " + JSON.stringify(data));
			var error	= ( data.status && (data.status >= 400)) ? true : false;	
			var type  	=  error ? 'danger' : 'info';
			
			if ( error && data.message ) {
				growl (data.message, type);
				return;
			}
			if ( op.indexOf('Inspect') != -1 ) {
				$('#inspectTitle').html('<small>' + id + '</small>');
				$('#json-renderer').jsonViewer(data.data);
				$('#btnInspect').click();
			}
			// {"message":"OK","status":200}
			if ( refresh ) {
				//location = "view.jsp";
				$('#btnRefreshContainers').click();
			}
			if ( notify ) {
				// {"message":"OK","status":200,"warnings":["Node3 This node is not part of a swarm","Node4 This node is not part of a swarm"]}
				if ( data.warnings) {
					growl('<li>' + data.warnings.join('<li>'), 'warning');
				}
				else {
					growl('Operation succeeded.');
				}
			}
		});
		return false;
	}
	
	function swarmLeave() {
		var node = $('#selNode2').children('option:selected').text();
		LOGD("Leave swarm node:" + node);
		swarmAction ('SwarmLeave', '', node);
		return false;
	}
	
	function swarmInit() {
		var node = $('#selNode').children('option:selected').text();
		var data = $('#frm1').serialize() + "&node=" + node;
		LOGD("Swarm Init " + data);
		
		// send data
		var url 	= baseUrl + '?op=SwarmInit&node=' + node;
		var posting = $.post( url , data);
		
		// get results 
		posting.done(function( data ) {
			// {"message":"OK","SwarmId":"j36digs4dakwbdvzm6sg1sw4y","status":200} 
			// {"message":"This node is already part of a swarm. Use \"docker swarm leave\" to leave this swarm and join another one.","status":503}
			LOGD("Resp: " + JSON.stringify(data));
			var error	= ( data.status && (data.status >= 400)) ? true : false;	
			var type  	=  error ? 'danger' : 'info';
			
			if ( error && data.message ) {
				growl (data.message + ' <a target="_blank" href="../../../log/logview.jsp">VIEW LOGS</a>', type);
				return;
			}
			// refresh
			location = "view.jsp";
		});
		
		return false;
	}
	
	function swarmJoin() {
		var swarmId	= $('#selSwarm').children('option:selected').text();
		var data 	= $('#frm2').serialize() + "&node=" + $('#selNode1').children('option:selected').text();
		var node 	= $('#selNode1').children('option:selected').text();

		data = data.replace('AdvertiseAddr1', 'AdvertiseAddr');
		LOGD("Swarm Join Swarm " + swarmId + " Node:" + node + " Payload: " + data);
		
		swarmAction ('SwarmJoin', swarmId, node, data, true);
		// close modal
		$('#btnCloseJoinModal').click();
		return false;
	}
	
	// Initialization: recalculate data tables colums widths: {table}.columns.adjust().draw();
	$(document).ready(function() {
		
		TBL1 = $('#tblSwarms').DataTable({
			stateSave: true,
			paging: false,
			searching: false,
			"ajax": {
				"url" : baseUrl + "?op=SwarmView",
				"dataSrc": ""
			} ,
			"columns": [
				{
					"class":          "details-control",
					"orderable":      false,
					"data":           null,
					"defaultContent": ""
				},			    
				{ "data": "SwarmId" },
				{ "data": "Master" },
				{ "data": "Members[, ]" }
			],
			"columnDefs": [{
				"targets": 4, // Actions col(3)
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					return '<a href="manage.jsp?name=' + full.SwarmId + '">Manage</a>' 
						+ '&nbsp;&nbsp;&nbsp;&nbsp;<a href="#" onclick="return inspect(\'' + full.SwarmId + '\',\'' + full.Master + '\')">Inspect</a>'
						+ '&nbsp;&nbsp;&nbsp;&nbsp;<a href="#" onclick="return removeSwarm(\'' + full.SwarmId + '\')">Remove</a>';
				}}]			
		});
		
		// Add event listener for opening and closing details
	    $('#tblSwarms tbody').on('click', 'td.details-control', function () {
	        var tr 	= $(this).closest('tr');
	        var row = TBL1.row( tr );
	 
	        if ( row.child.isShown() ) {
	            // This row is already open - close it
	            row.child.hide();
	            tr.removeClass('shown');
	        }
	        else {
	            // Open this row
	            row.child( format(row.data()) ).show();
	            tr.addClass('shown');
	        }
	    } );
		
		loadNodes();
    	
 	});
	
	// Formatting function for row details - modify as you need 
	// [{"SwarmId":"zy8fmv87iysykdkxtktp9s5td","errors":["{\"message\":\"This node is not a swarm manager. Worker nodes can't be used to view or modify cluster state. Please run this command on a manager node or promote the current node to a manager.\"}\n"],"JoinTokens":{"Worker":"SWMTKN-1-1jqc9grkl4jj3jx12focnzs3179x0fckmiddfu94lkl5q192mr-dzzu2s8gtezrl6shn85yuivyn","Manager":"SWMTKN-1-1jqc9grkl4jj3jx12focnzs3179x0fckmiddfu94lkl5q192mr-cfqxu5vmw92jgbtqqkn70mbq2"},"nodes":"[]","Members":["Node4"],"Master":"Node3"}]
	function format ( d ) {
		//LOGD('Format: ' + JSON.stringify(d));
	    // `d` is the original data object for the row
	    var nodes 	= d.nodes;
	    var html 	= '';
	    var prefix 	= '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:100px; width:98%">';
	    var suffix 	= '</table>';
	    
	    if ( d.errors && (d.nodes.length == 0) ) {
	    	html = '<li>' + d.errors.join('<li>');
	    	growl(html, 'danger');
	    	return prefix + '<tr>' + html + '</tr>' + suffix;;
	    }
	    if ( d.warnings ) {
	    	growl('<li>' + d.warnings.join('<li>'), 'warning');
	    }
	    for (var i = 0; i < nodes.length; i++) {
			var node 	= nodes[i];
			var status	= node.Status;
			var desc	= node.Description;
			var spec 	= node.Spec;
			var sep		= '&nbsp;&nbsp;'
			html += '<tr>' + '<td>Member ' + (i+1) + '</td><td><a href="../manage.jsp?addr=' + status.Addr + '">' + status.Addr + '</a></td>' + '<td>' + status.State + (status.Message ? ' ' + status.Message : '') + '</td>'
				+ '<td>' + desc.Hostname + sep + joinObjVals(desc.Platform, sep) + '</td>'
				+ '<td>' + joinObjVals(spec, sep) + '</td>'
				+ '</tr>';
		}
	    return prefix + html + suffix;
	}
	/*
	function joinObjVals (obj, sep ) {
		sep = sep || ' ';
		var str = '';
		for ( var key in obj ) {
			if ( typeof(obj[key]) != 'object') {
				str += obj[key] + sep;
			}
		}
		return str;
	}*/
	function joinObjVals (obj, sep ) {
		sep = sep || ' ';
		var str = JSON.stringify(obj);
		return str.replace(/[{}"]/g, ' ').replace(/,/g, sep);
	}
	

	</script>
	 
</body>
</html>