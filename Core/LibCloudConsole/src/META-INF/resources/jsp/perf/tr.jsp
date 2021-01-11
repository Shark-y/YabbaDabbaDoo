<%@page import="com.cloud.console.performance.StalledThreadTracker"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.profiler.OSMetrics"%>
<%@page import="com.cloud.core.provider.IHTMLFragment"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%!
	static final String TAG = "[THREADS]";

	static void LOGD(String text) {
		System.out.println(TAG + " " + text);
	}

	static void LOGE(String text) {
		System.err.println(TAG + " " + text);
	}
%>

<%
	String contextPath 	= getServletContext().getContextPath();		// Begins with /CloudContact...
	String theme		= (String)session.getAttribute("theme");
	String title		= (String)session.getAttribute("title");
	
	if ( theme == null) theme = request.getParameter("theme");
	if ( theme == null) theme = ThemeManager.DEFAULT_THEME;
%>

<%
	final JSONObject os = OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);

%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html">

<title>Thread Inspector</title>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
</jsp:include>

<!--  common styles -->
<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">

<style type="text/css">
body {
	font-family: Arial;
	margin-top: 0px;
}
.STALLED {
	background-color: yellow;
}
.DEADLOCKED {
	color: white;
	background-color: red;
}
.RUNNABLE {
	color: white;
	background-color: lightgreen;
}

</style>

<script type="text/javascript" src="../../js/jquery.js"></script>
<script type="text/javascript" src="../../js/log.js"></script>

<script type='text/javascript'>

	// poll end point
	var pollEndPoint = '<%=contextPath%>/OSPerformance';
	
</script>

</head>
<body>

	
	<!-- 
	{"data":[["CloudExecutorService-3",48,5,"WAITING",false,true,false,"STACK1"],...]}
	{"data":[T1, T2, T3,...]} where T = [NAME, ID, PRIORITY, STATE, ISDAEMON, ISALIVE, ISINTERRUPTED, STACK-TRACE]  
	 -->	
	<div class="col-lg-12">
	
		<h2>Live Threads <small id="totals"></small></h2>

		<table id="tblThreads" class="table display compact" width="100%">
			<thead>
				<tr>
					<th>Name</th>
					<th>Id</th>
					<th>Priority</th>
					<th>State</th>
					<!-- 
					<th>Daemon</th>
					 <th>Alive</th> -->
					<th>Interrupted</th>
					<th>Stack Trace/Lock</th>
				</tr>
			</thead>
			<tbody></tbody>
		</table>
	
	</div>

	<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>

	<script type="text/javascript">
		var table;
		var stuckCount = 0;
		var deadlockedCount = 0;
		
		// interrup by thread id
		function interrupt(op, tid) {
			$.ajax({
				type : 'POST',
				url : pollEndPoint + '?op=' + op + '&tid=' + tid,
				// request response in json!
				headers : {
					"Accept" : "application/json; charset=utf-8"
				},
				cache : false
			});
			table.clear().draw();
		}

		// reset stalled thread detector information.
		function reset() {
			$.ajax({
				type : 'POST',
				url : pollEndPoint + '?op=reset',
				// request response in json!
				headers : {
					"Accept" : "application/json; charset=utf-8"
				},
				cache : false
			});
			table.clear().draw();
		}
	
        $().ready(function() {
        	table = $('#tblThreads').DataTable({
        		stateSave: true, 
        		"ajax": pollEndPoint + '?op=trst' ,
        		"language": {
	        		"lengthMenu": 'Display <select>'+
						'<option value="10">10</option>'+
						'<option value="50">50</option>'+
						'<option value="100">100</option>'+
						'<option value="500">500</option>'+
						'<option value="-1">All</option>'+
						'</select>' 
						+ '&nbsp;&nbsp;&nbsp;<a data-toggle="tooltip" href="javascript:reset()" title="Click to reset the warning status of all threads.">Reset</a>'
						+ '&nbsp;&nbsp;&nbsp;<a data-toggle="tooltip" href="heap.jsp" target="_blank" title="Click to view a memory snapshot.">Heap Dump</a>'
        		} ,
        		
    			"createdRow": function ( row, data, index ) {
    				var LEVEL = data[3];
    		        //$('td', row).eq(0).addClass(LEVEL);		// NAME
    		        //$('td', row).eq(3).addClass(LEVEL);		// STATE
    		        if ( LEVEL.indexOf('STUCK') != -1 ) {
    		        	$('td', row).eq(3).addClass('STALLED');
    		        	stuckCount++;
    		        }
    		        if ( LEVEL.indexOf('DEADLOCKED') != -1 ) {
    		        	$('td', row).eq(3).addClass(LEVEL);
    		        	deadlockedCount++;
    		        }
    			},
    			"headerCallback": function ( row, data, start, end, display ) {
    				var api = this.api(), data;
    				
    				// Update footer
    				var totals = '(Total: ' + api.data().length + ' Deadlocked: ' + deadlockedCount + ' Probably Stuck: ' + stuckCount + ')';
    				
    	            //$( api.column( 0 ).header() ).html( totals );
    	            $( '#totals' ).html( totals );
    	            stuckCount 		= 0;
    	            deadlockedCount = 0;
    			}
        	});
        	
        	// poll every 5s
        	setInterval(function () {
        		//LOGD("Thread table reload.");
        		table.ajax.reload();
        	}, 5000);
        	
        	// Note: Tooltips must be initialized with jQuery: select the specified element and call the tooltip() method.
        	//$('[data-toggle="tooltip"]').tooltip();
        	
        });
        
    </script>
    
 
    <!-- Bootstrap Core JavaScript -->
    <script src="../../skins/bootstrap/js/bootstrap.js"></script>

	<script>
		// Note: Tooltips must be initialized with jQuery: select the specified element and call the tooltip() method.
		$(document).ready(function(){
			$('[data-toggle="tooltip"]').tooltip();
		});
	</script>
		    
</body>
</html>