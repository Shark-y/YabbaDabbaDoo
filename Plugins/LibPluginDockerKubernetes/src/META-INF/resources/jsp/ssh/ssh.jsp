<!doctype html>

<%
	final String contextPath 	= getServletContext().getContextPath();
	final String name			= request.getParameter("host") != null ? request.getParameter("host") : "";			// host
	final String id				= request.getParameter("user") != null ? request.getParameter("user") : "";			// user
	final String identity		= request.getParameter("identity") != null ? request.getParameter("identity") : "";	// pwd/key
	
	// Protocol (both cases) HTTP/1.1 - scheme: http/https
	String wsUrl 				= (request.getScheme().equals("http") ? "ws://" : "wss://")
		+ request.getServerName() + ":" + request.getServerPort() + contextPath;
%>
  <html>
    <head>
    
	  <!-- Core CSS with all styles -->
      <link href="../../skins/clouds/css/styles.css" type="text/css" rel="stylesheet">                                    
      <link rel="stylesheet" href="../../xterm/dist/xterm.css" />
	  
	  <script src="../../js/jquery.js"></script>
	  <script src="../../skins/clouds/../bootstrap/js/bootstrap.js"></script>
	  
	  <script src="../../js/log.js"></script>
      <script src="../../skins/clouds/js/bootstrap-growl.js"></script>
      
	  <script src="../../xterm/dist/xterm.js"></script>
	  <script src="../../xterm/dist/addons/attach/attach.js"></script>
	  
	  
	  <script src="ssh.js"></script>
	  <style type="text/css">
		@media (min-width: 768px) {
		  .modal-dialog {
		    width: 650px;
		    margin: 30px auto;
		  }
		  .modal-content {
		    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.5);
		  }
		  .modal-sm {
		    width: 500px;
		  }
		}	  
	  </style>
    </head>
    <body>
    	<!-- 
		<button onclick="ws_attach()">Attach</button>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  <button onclick="ws_detach()">Detach</button>
		-->
     	<div id="terminal"></div>
     	
		
		<div id="modal1" class="modal fade form-horizontal" tabindex="-1" role="dialog">
			<div class="modal-dialog modal-sm">
				<div class="modal-content">
					<div class="modal-header">
						<button id="btnCloseModal" type="button" class="close" data-dismiss="modal" aria-hidden="true" style="display: none">&times;</button>
						<h3 class="modal-title">SSH <small>to a remote host.</small></h3>
					</div>
					<div class="modal-body">
					
						<div class="form-group">
							<label class="col-sm-2 control-label">Host</label>
							<div class="col-sm-10">
								<input class="form-control" id="node" required data-toggle="tooltip">
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">User</label>
							<div class="col-sm-10">
								<input type="text" id="id" class="form-control" required="required" />
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Password</label>
							<div class="col-sm-10">
								<input type="password" id="identity" class="form-control" required="required">
							</div>
						</div>
						
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<button type="submit" class="btn btn-raised btn-primary" onclick="return start_click()">Start</button>
					</div>
					
				</div>
			</div>
		</div>
		
		<!-- end modal 1 (login) -->
	  	<button id="btnLogin" data-toggle="modal" data-target="#modal1" style="display: none"></button>	
	  	
    <script>
	  	var node		= '<%=name%>';		// host
      	var id 			= '<%=id%>';		// user
      	var identity	= '<%=identity%>';	// pwd/key
      	var bAttach 	= true;
      	var wsurl		= '<%=wsUrl%>/SSHTerminal?' + (bAttach ? '&attach=1' : ''); 
      	
    	// Initialization
    	$(document).ready(function() {
          	if ( node == '' || id == '' || identity == '') {
          		$('#node').val(node);
          		$('#id').val(id);
          		$('#identity').val(identity);
          		$('#btnLogin').click();
          		return;
          	}
    		wsurl += '&host=' + node + '&user=' + id + '&identity=' + identity 
    		connect();
    	});
		
    	function start_click() {
    		node 		= $('#node').val();
    		id 			= $('#id').val();
    		identity 	= $('#identity').val();
    		if ( !validate()) { 
    			$('#btnLogin').click();
    			return false; 
    		}
    		$('#btnCloseModal').click();
    		wsurl += '&host=' + node + '&user=' + id + '&identity=' + identity 
    		connect();
			return false;    		
    	}
    </script>
	
	<script>
	
	var socket;
	var term;

	function connect () {
		if ( bAttach) {
			//console.log('Addon attach');
			Terminal.applyAddon(attach);
		}
	    term = new Terminal({ cols: 120 });
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
	            		socket.send(term.buffer.join('') + '\n');
	                    //term.prompt();
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
	                	else {
	                		//term.write('\b');
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
	        	if ( !bAttach) {
	            	term.write(data);
	            	term.buffer.push(data);
	        	}
	        });
	    }
	    runFakeTerminal();
	    ws_connect () ;
	}

	function ws_connect () {
		console.log('New WS @ ' + wsurl);
		socket = new WebSocket(wsurl);
		var myTextDecoder 	= new TextDecoder();
		
		socket.onopen = function (ev) {
			if ( bAttach) {
				console.log('WS opened attaching to socket.');
				term.attach(socket, true, false);  // Attach the above socket to `term`
				return;
			}
			//term.write('Connected to ' + node + '/' + id);
			term.prompt();
		}
		socket.onclose = function (evt) {
			growl ('Socket closed unexpectedly. Code:' + evt.code + ' ' + evt.reason, 'danger');
		}
		if ( bAttach) {
			console.log('Skiping onmessage callback');
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

	function validate () {
		if ( node == '') {
			growl('Hostname is required.','danger');
			return false;
		}
		if ( id == '') {
			growl('User is required.','danger');
			return false;
		}
		if ( identity == '') {
			growl('Password is required.','danger');
			return false;
		}
		return true;
	}

	/*
	function ws_attach() {
		console.log("Attach");
		// The following line of code:
		//   1. Attaches the given socket to `term`
		//   2. Sets up bidirectional communication (sends to stdin and renders stdout/stderr)
		//   3. Buffers rendering for better performance
		//term.attach(socket, true, true);
		//if ( !validate()) { return; }
		//connect();
	}

	function ws_detach() {
		console.log("Detach");
		// https://xtermjs.org/docs/api/addons/attach/
		//term.detach(socket);  // Now the socket is detached. Nothing will be rendered or sent back.
	} */

	//level: info, danger, warning, success
	function growl ( text, level, delay) {
		delay = delay || 30000;
		level = level || 'info'; 
		$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
	}
		
	
	</script>
    </body>
  </html>
