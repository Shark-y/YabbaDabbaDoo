<!doctype html>
<!--
https://xtermjs.org/docs/api/addons/attach/
The attach addon provides methods for attaching a terminal to a WebSocket stream, such as Docker’s WebSocket attach endpoint.

This allows easy hooking of the terminal front-end to background processes and interact with it, just like you would do with in a local terminal. This means that the front-end terminal (a Terminal instance) will render the stdout and stderr logs of the back-end process and will send to it all keyboard and mouse events captured.


import * as Terminal from 'xterm';
import * as attach from 'xterm/lib/addons/attach/attach';

Terminal.applyAddon(attach);  // Apply the `attach` addon

var term = new Terminal();
var socket = new WebSocket('wss://docker.example.com/containers/mycontainerid/attach/ws');

term.attach(socket);  // Attach the above socket to `term`


Methods
attach(socket[, bidirectional[, bufferred]])

    socket - WebSocket - The socket to attach to the current terminal
    bidirectional - Boolean - Whether the terminal should send data to the socket (defaults to true)
    bufferred - Boolean - Whether the terminal should buffer output rendering for better performance (defaults to false)

Attaches the given WebSocket to the current terminal.
Example

var socket = new WebSocket('wss://docker.example.com/containers/mycontainerid/attach/ws');

// The following line of code:
//   1. Attaches the given socket to `term`
//   2. Sets up bidirectional communication (sends to stdin and renders stdout/stderr)
//   3. Buffers rendering for better performance
term.attach(socket, true, true);


detach(socket)

    socket - WebSocket - The socket to detach from the current terminal

Detaches the given WebSocket to the current terminal. After calling this method, no stdout or stderr will be rendered and no data will be sent to stdin.
Example

var socket = new WebSocket('wss://docker.example.com/containers/mycontainerid/attach/ws');

term.attach(socket);

// ... user code here ...

term.detach(socket);  // Now the socket is detached. Nothing will be rendered or sent back.

-->

<!--
Exec Create

POST /containers/(id or name)/exec

Sets up an exec instance in a running container id

Example request:

POST /v1.24/containers/e90e34656806/exec HTTP/1.1
Content-Type: application/json
Content-Length: 12345

{
  "AttachStdin": true,
  "AttachStdout": true,
  "AttachStderr": true,
  "Cmd": ["sh"],
  "DetachKeys": "ctrl-p,ctrl-q",
  "Privileged": true,
  "Tty": true,
  "User": "123:456"
}
Example response:

HTTP/1.1 201 Created
Content-Type: application/json

{
     "Id": "f90e34656806",
     "Warnings":[]
}

Exec Start

POST /exec/(id)/start

Starts a previously set up exec instance id. If detach is true, this API returns after starting the exec command. Otherwise, this API sets up an interactive session with the exec command.

Example request:

POST /v1.24/exec/e90e34656806/start HTTP/1.1
Content-Type: application/json
Content-Length: 12345

{
 "Detach": false,
 "Tty": false
}

Example response:

HTTP/1.1 200 OK
Content-Type: application/vnd.docker.raw-stream

{{ STREAM }}

JSON parameters:

    Detach - Detach from the exec command.
    Tty - Boolean value to allocate a pseudo-TTY.

Status codes:

    200 – no error
    404 – no such exec instance
    409 - container is paused

Stream details:

Similar to the stream behavior of POST /containers/(id or name)/attach API
-->

<%
	final String contextPath 	= getServletContext().getContextPath();
	final String name			= request.getParameter("node");	// node name
	
	// Docker parameters
	final String id				= request.getParameter("Id");	// container id
	final String shell			= request.getParameter("shell") != null ? request.getParameter("shell") : "sh";
	
	// 6/17/2019 Kubernetes
	final String pod			= request.getParameter("pod");		
	final String namespace		= request.getParameter("namespace");
	final boolean useK8S		= pod != null && namespace != null;
			
	// WS URL: Note:  RQ Protocol = HTTP/1.1 for http/https - scheme: http/https
	final String wsUrl 			= (request.getScheme().equals("http") ? "ws://" : "wss://")
		+ request.getServerName() + ":" + request.getServerPort() + contextPath;
%>
  <html>
    <head>
    
	  <!-- Core CSS with all styles -->
      <link href="../../../skins/clouds/css/styles.css" type="text/css" rel="stylesheet">                                    
      <link rel="stylesheet" href="../../../xterm/dist/xterm.css" />
	  
	  <script src="../../../js/jquery.js"></script>
	  <script src="../../../skins/clouds/../bootstrap/js/bootstrap.js"></script>
	  
	  <script src="../../../js/log.js"></script>
      <script src="../../../skins/clouds/js/bootstrap-growl.js"></script>
      
	  <script src="../../../xterm/dist/xterm.js"></script>
	  <script src="../../../xterm/dist/addons/attach/attach.js"></script>
	  
	  <script src="term.js"></script>
    </head>
    <body>
    	<!-- 
		<button onclick="ws_attach()">Attach</button>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  <button onclick="ws_detach()">Detach</button>
		-->
      <div id="terminal"></div>
	  
    <script>
	  	var node	= '<%=name%>';
		var url 	= '<%=contextPath%>/Docker?node=' + node;
      	var id 		= '<%=id%>';
      	var shell	= '<%=shell%>';
      	var useK8S	= <%=useK8S%>;
      	var bAttach = true; //useK8S ? false : true;
      	
      	var wsurl	= useK8S 
      			? '<%=wsUrl%>/PodTerminal?node=' + node + '&containerId=' + id + '&pod=<%=pod%>&namespace=<%=namespace%>' + (bAttach ? '&attach=1' : '') + '&shell=' + shell
      			: '<%=wsUrl%>/DockerTerminal?node=' + node + '&containerId=' + id + (bAttach ? '&attach=1' : '') + '&shell=' + shell;
    </script>
	  
    </body>
  </html>
