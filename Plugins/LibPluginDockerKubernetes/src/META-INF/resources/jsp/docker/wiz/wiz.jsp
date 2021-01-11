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
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	final String pageTitle		= "Docker Wizards"; 
	
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
	<jsp:param value="Cluster - Docker" name="title"/>
</jsp:include>

<link rel="stylesheet" type="text/css" href="../../../css/jquery.dataTables.css">
<link rel="stylesheet" type="text/css" href="../../../css/jquery.json-viewer.css">
<link rel="stylesheet" type="text/css" href="../typeahead.css">
<link rel="stylesheet" type="text/css" href="../../../css/snackbar.css">
<link rel="stylesheet" href="../../../xterm/dist/xterm.css" />

<script src="../../../xterm/dist/xterm.js"></script>
<script src="../../../xterm/dist/addons/attach/attach.js"></script>
<script src="../docker-common.js"></script>

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

			<div class="panel panel-info" data-widget='{"draggable": "false"}'>
				<div class="panel-heading">
					<h2>Docker RUN</h2>
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				
				<div class="panel-body">
					<p>Use this wizard to deploy and run a container in one or more nodes. <a target="_blank" href="https://docs.docker.com/engine/reference/run/">About Docker Run</a></p>
					
					<!-- -->
					<jsp:include page="tile_wiz.jsp">
						<jsp:param value="bar" name="foo"/>
					</jsp:include>
					
				</div>
				<!-- end panel body -->
			</div>
					
		</div>
	</div>	
	
	
	
<!-- END CONTENT -->

	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../../\") %>" name="basePath"/>
		<jsp:param value="../../../" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript" src="../../../js/jquery.dataTables.js"></script>
	<script type="text/javascript" src="../../../js/poll.js"></script>
	<script type="text/javascript" src="../../../js/jquery.json-viewer.js"></script>
	<script type="text/javascript" src="../../../js/snackbar.js"></script>
	
	<!-- wizards -->
	<script type="text/javascript" src="../../../js/jquery.validate.min.js"></script>
	<script type="text/javascript" src="../../../js/jquery.stepy.js"></script>

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
				<div class="ProfileCard-description">{{short_description}} </div>
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
	
	//var node	= '';
	var url 	= '<%=contextPath%>/Docker?'; //?node=' + node;
	var term;			// XTerm
	var socket;			// websocket
	var bAttach = false;
	
	// Initialization: 
	$(document).ready(function() {
		
    	initializeWizards ();
    	//initalizeTerminal ();
    	
    	loadNodes();
	});
	
	function loadNodes() {
		<% for ( Map.Entry<String, DockerNode> entry : nodes.entrySet()) { 
			DockerNode node = entry.getValue();
		%>
		$('.nodes').append($('<option></option>').attr('value', '<%=node.getName()%>').text('<%=node.getFullName()%>'));
		<% } %>
	}
	
	function initializeWizards () {
		//Load Wizards
	    $('#runwizard').stepy({finishButton: true, titleClick: true, block: true, validate: true, legend: false});

	    //Add Wizard Compability - see docs
	    $('.stepy-navigator').wrapInner('<div class="pull-right"></div>');

	    //Make Validation Compability - see docs
	    $('#runwizard').validate({
	        errorClass: "help-block",
	        validClass: "help-block",
	        highlight: function(element, errorClass,validClass) {
	           $(element).closest('.form-group').addClass("has-error");
	        },
	        unhighlight: function(element, errorClass,validClass) {
	            $(element).closest('.form-group').removeClass("has-error");
	        }
	    });
		// Attach a submit handler to the form
		$( "#runwizard" ).submit(function( event ) {
			  // Stop form from submitting normally
			  event.preventDefault();
			  runwiz_onsubmit();
		});
	}

	function runwiz_onsubmit() {
		LOGD('RunWiz submit.');
		
		$('#terminal').show();
		initalizeTerminal ();
		  
		ws_attach();
	}
	
	function getRunWizData () {
		return $( "#runwizard" ).serialize();
	}
	
	function initalizeTerminal () {
		if ( term ) {
			LODG('Terminal already initialized.');
			return;
		}
		if ( bAttach) {
			console.log('Addon attach');
			Terminal.applyAddon(attach);
		}
		
	    term = new Terminal({ cols: 100 });
	    term.open(document.getElementById('terminal'));
	    
	    function runFakeTerminal() {
	        if (term._initialized) {
	            return;
	        }

	        term._initialized = true;

	        term.prompt = function () {
	            term.write('\r\n$ ');
	        };
	        term.x = 2;
	        term.buffer = [];
	        
	        term.on('key', function(key, ev) {
	            const printable = !ev.altKey && !ev.altGraphKey && !ev.ctrlKey && !ev.metaKey;

	            //console.log ('ev.keyCode ' + ev.keyCode + ' term.x=' + term.x );
	            // ENTER
	            if (ev.keyCode === 13) {
	            	if ( !bAttach) {
	            		console.log('Send: ' + term.buffer.join(''));
	            		socket.send(term.buffer.join(''));
	                    term.prompt();
	            	}
	                term.x = 2;
	                term.buffer = [];
	            } 
	            // BACKSPACE
	            else if (ev.keyCode === 8) {
	                // Do not delete the prompt
	                if (term.x > 2) {
	                	if ( !bAttach) {
	                		term.write('\b \b');
	                	}
	                    term.buffer.pop();
	                }
	                term.x--;
	            }
	            // PRINATBLE
	            else if (printable) {
	            	//console.log ('key ' + key);
	            	if ( !bAttach) {
	            		term.write(key);
	            	}
	                term.x++;
	                term.buffer.push(key);
	            }
	        });

	        term.on('paste', function(data) {
	            term.write(data);
	            term.buffer.push(data);
	        });
	    }
	    runFakeTerminal();
	    
	}
	
	function ws_attach() {
		var node 	= $('#selNodes').children('option:selected').val();
		var image 	= $('#Image').val();
		var tag 	= $('#Tag').val();
		var url 	= '<%=wsUrl%>/DockerWizard?node=' + node + '&imageId=' + image + '&tag=' + tag;
		
		LOGD("Image install via WS: " + url);
		
		ws_installImage(url);
	}
	
	function ws_installImage (url) {
		socket 				= new WebSocket(url);
		var myTextDecoder 	= new TextDecoder();
		
		socket.onopen = function (ev) {
			var node = $('#selNodes').children('option:selected').val();
			
			if ( bAttach) {
				console.log('WS opened' + JSON.stringify(ev) + ' attaching.');
				term.attach(socket, true, false);  // Attach the above socket to `term`
				return;
			}
			term.write('Connected to ' + node);
			term.prompt();
			
			var wizData = getRunWizData ();
			LOGD('Sending data ' + wizData);
			socket.send('{ "request": "' +  wizData + '"}');
		}
		socket.onclose = function (evt) {
			console.log('WS closed: ' + JSON.stringify(evt));
		}
		if ( bAttach) {
			console.log('skiping onmessage callback');
			return;
		}
		socket.onmessage = function (ev) {
			//console.log('OnMessage: typeof(ev.data) ' + typeof(ev.data) );
			
	       	if (typeof ev.data === 'object') {
	            if (ev.data instanceof ArrayBuffer) {
	                str = myTextDecoder.decode(ev.data);
	                displayData(str);
	            }
	            else {
	                var fileReader_1 = new FileReader();
	                fileReader_1.addEventListener('load', function () {
	                    str = myTextDecoder.decode(fileReader_1.result);
	                    displayData(str);
	                });
	                fileReader_1.readAsArrayBuffer(ev.data);
	            }
	        }
	        else if (typeof ev.data === 'string') {
	            // vsilva displayData(ev.data); 
				// fix linux \n => \r\n
				//console.log('Term write:' + ev.data);
				displayData(ev.data); // ev.data.replace(new RegExp('\n', 'g'), '\r\n') ); 
	        }
	        else {
	            throw Error("Cannot handle \"" + typeof ev.data + "\" websocket message.");
	        }
		};
		
		function displayData(str) {
			// Fix ANSI esc codes
			var esc = str.replace(/\\x1b/gim, '\x1b');
			//console.log('Write ' + esc);
			term.write(esc);
			
		}
	}
	
	
	</script>
	
	<script type="text/javascript" src="wiz.js"></script>
	
</body>
</html>