<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	final String contextPath 	= getServletContext().getContextPath();
	final String node 			= request.getParameter("node");
	final String id 			= request.getParameter("Id");
	final String system 		= request.getParameter("system") != null ? request.getParameter("system") : "DOCKER";
	// K8S POD LOGS
	final String pod 			= request.getParameter("pod");
	final String namespace 		= request.getParameter("namespace");
	
%>

<!DOCTYPE html >
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UFT-8">

<link href="../../skins/bootstrap/css/bootstrap-common/bootstrap.css" type="text/css" rel="stylesheet">

<title>Container Logs</title>
</head>

<body>
<% if ( system.equals("DOCKER")) {%>
<input id="stdout" type="checkbox" checked="checked" value="stdout">stdout &nbsp;&nbsp;
<input id="stderr" type="checkbox" checked="checked" value="stderr">stderr &nbsp;&nbsp;
<input id="format" type="checkbox" checked="checked" value="format">Preformat 
<% } %>
 &nbsp;&nbsp;tail <input id="tail" type="text" value="300" title="A number of lines to display or (all) for everything." style="width: 60px">

<button id="refresh">Refresh</button>
<button id="clean">Clear</button>

<div id="logs" class="form-control" style="width: 100%; height: 100%; overflow: auto; border: 1px black;">
</div>

<script type="text/javascript" src="../../js/jquery.js"></script>  
<script type="text/javascript" src="../../js/log.js"></script>
<script type="text/javascript" src="../../skins/bootstrap/js/bootstrap.js"></script>
<script type="text/javascript" src="../../skins/clouds/js/bootstrap-growl.js"></script>
<script type="text/javascript" src="../../js/notify.js"></script>
 
<script type="text/javascript">

// Initialization: 
$(document).ready(function() {
   	$('#refresh').click ( function () {
		refresh();
	});
   	$('#clean').click ( function () {
   		$('#logs').html('');
	});
	refresh();
});

//level: info, danger, warning, success
function growl ( text, level, delay) {
	delay = delay || 30000;
	$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
}

function refresh() {
   	<% if ( system.equals("DOCKER")) {%>
	refreshDocker();
	<% } else { %>
	refreshK8S();
	<% } %>
}

function refreshDocker() {
	// http://localhost:9080/CloudClusterManager/Docker?node=Node1&op=ContainerLogs&Id=6b0d78368a5b098f2d2dfded877d5b37588ada681b1e9fe1d83cc941dfcf52be&logargs=stdout%3D1%26stderr%3D1%26tail%3D20
	var url 	= '<%=contextPath%>/Docker?op=ContainerLogs&node=<%=node%>&Id=<%=id%>';
	var args 	= 'stdout%3D' +  ( $("#stdout").is(':checked') ? '1' : '0' )
				+ '%26stderr%3D' +  ( $("#stderr").is(':checked') ? '1' : '0' )
				+ '%26tail%3D' +  $('#tail').val();
	
	LOGD('Logs ' + url + ' Args:' + args);
	
	var posting = $.get( url + '&logargs=' + args);
	
	// get results 
	posting.done(function( data ) {
		// DOCKER {"message":"OK","status":200,"data":BUF}
		// LOGD("Got: " + JSON.stringify(data));
		if ( data.status >= 400) {
			growl(data.message, 'danger');
			return;
		}
		// 5/20/2019 data.data is a an array now: {"message":"OK","status":200,"data": [BUFLINE1, BUFLINE2, ...]}
		// Each line in the buffer id a docker encoded log [HEADER{8bytes}][DATA]
		// https://docs.docker.com/engine/api/v1.24/#31-containers
		// header := [8]byte{STREAM_TYPE, 0, 0, 0, SIZE1, SIZE2, SIZE3, SIZE4} STREAM_TYPE: 0: stdin , 1: stdout, 2: stderr
		// SIZE1, SIZE2, SIZE3, SIZE4 are the four bytes of the uint32 size encoded as big endian.
		var buf 	= data.data;	
		var buf1	= "";
		
		if ( Array.isArray(buf) ) {
			for ( var i = 0 ; i < buf.length ; i++) {
				buf1 += buf[i].substring(8) + "<br>";
			}
		}
		else {
			// RAW Buffer: {"message":"OK","status":200,"data":RAW-BUF}, split by \n and display
			var tmp 	= buf.split('\n');
			for ( var i = 0 ; i < tmp.length ; i++) {
				buf1 += tmp[i].substring(8) + "<br>";
			}
		}
		var pre = $("#format").is(':checked');
		$('#logs').html( pre ? "<pre>" + buf1 + "</pre>" : buf1);
	});
	
}

/*
 * GET /api/v1/namespaces/{namespace}/pods/{name}/log
 Path Parameters
 Parameter	Description
 name	name of the Pod
 namespace	object name and auth scope, such as for teams and projects
 ---------------------------
 Query Parameters
 Parameter	Description
 container	The container for which to stream logs. Defaults to only container if there is one container in the pod.
 follow	Follow the log stream of the pod. Defaults to false.
 limitBytes	If set, the number of bytes to read from the server before terminating the log output. This may not display a complete final line of logging, and may return slightly more or slightly less than the specified limit.
 pretty	If 'true', then the output is pretty printed.
 previous	Return previous terminated container logs. Defaults to false.
 sinceSeconds	A relative time in seconds before the current time from which to show logs. If this value precedes the time a pod was started, only logs since the pod start will be returned. If this value is in the future, no logs will be returned. Only one of sinceSeconds or sinceTime may be specified.
 tailLines	If set, the number of lines from the end of the logs to show. If not specified, logs are shown from the creation of the container or sinceSeconds or sinceTime
 timestamps	If true, add an RFC3339 or RFC3339Nano timestamp at the beginning of every line of log output. Defaults to false.
 */
function refreshK8S() {
	// GET http://localhost:9080/CloudClusterManager/K8S?node=KubeClusterNAC208&op=GetPodLogs&pod=kube-scheduler-kubemaster&namespace=kube-system
	var url 	= '<%=contextPath%>/K8S?op=GetPodLogs&pod=<%=pod%>&namespace=<%=namespace%>&node=<%=node%>';
	var lines	= $('#tail').val() == 'all' ? 300 : $('#tail').val();
	var args 	= 'tailLines%3D' +  lines;

	LOGD('K8S Url:' + url + ' Args:' + args);
	var posting = $.get( url + '&logargs=' + args);
	
	// get results 
	posting.done(function( data ) {
		// LOGD('K8SLOGS: ' + JSON.stringify(data));
		// Raw buff: {"data":"LINE1\nLINE2\n...","message":"OK","status":200}
		// Object (1 event) {"data":{"@timestamp":"2020-03-19T17:19:18Z","pid":1,"type":"log","message":"PollError No Living connections","tags":["warning","task_manager"]},"message":"OK","status":200}

		if ( data.status >= 400) {
			growl(data.message, 'danger');
			return;
		}
		var buf 	= data.data;
		
		if ( Array.isArray(buf) ) {
			// Array (1+ events) {"data":[{"@timestamp":"2020-03-19T17:19:18Z","pid":1,"type":"log","message":"PollError No Living connections","tags":["warning","task_manager"]},"message":"OK","status":200}
			// {"data":[{"level":"warn","cmd":"hub","time":"2020-12-26T00:02:33Z","message":"email...
			var html 	= '';
			LOGD('Got Array of size=' + buf.length);
			
			for ( var i = 0 ; i < buf.length ; i++) {
				var event 	= buf[i];
				
				if ( /*event["@timestamp"] && */ event["message"] ) {
					html 	+= (event["@timestamp"] ? event["@timestamp"] : '')
						+ (event["time"] 	? event["time"] : '')
						+ (event["level"] 	? ' ' + event["level"] : '')
						+ ' ' + event["message"] + '\n';
				}
				else {
					html 	+= event + '\n';
				}
			}
			$('#logs').html( "<pre>" + html + "</pre>");	
		}
		else if ( isObject(buf) ) {
			LOGD('Got object: ' + JSON.stringify(buf));
			
			// {"data":{"@timestamp":"2020-03-19T17:19:18Z","pid":1,"type":"log","message":"PollError No Living connections","tags":["warning","task_manager"]},"message":"OK","status":200}
			$('#logs').html( "<pre>" + buf["@timestamp"] + ' ' + buf["message"] + "</pre>");
		}
		else {
			$('#logs').html( "<pre>" + buf + "</pre>");	
		}
	});	
}

// return true if ob is an Object but not an array
function isObject(obj) {
	return obj === Object(obj) && !Array.isArray(obj);
}

</script>

</body>
</html>