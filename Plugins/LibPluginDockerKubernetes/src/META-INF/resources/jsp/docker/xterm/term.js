var socket;
var term;

$(function () {
	if ( bAttach) {
		console.log('Addon attach');
		Terminal.applyAddon(attach);
	}
    term = new Terminal({ cols: 140 });
    term.open(document.getElementById('terminal'));
	
    function runFakeTerminal() {
        if (term._initialized) {
            return;
        }

        term._initialized = true;

        term.prompt = function () {
            term.write('\r\n$ ');
        };
        term.x 		= 2;
        term.buffer = [];
        
        term.on('key', function(key, ev) {
            const printable = !ev.altKey && !ev.altGraphKey && !ev.ctrlKey && !ev.metaKey;
            var key 		= ev.keyCode;
            
            //console.log ('ev.keyCode ' + key + ' term.x=' + term.x + ' Attaced:' + bAttach + ' useK8S:' + useK8S );
            
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
        	// 6/17/2019
        	if ( !bAttach) {
        		term.write(data);
        		term.buffer.push(data);
        	}
        });
    }
    runFakeTerminal();
    ws_connect () ;
	
});

function ws_connect () {
	// sh @ cloud/connector-foo , Name = /elegant_stonebraker
	//var url = 'ws://192.168.42.248:2375/exec/e3196ceb5c677376d7dc93a40984e05255b5e96f26a0ab59e4755225b7cacc1f/start';
    //var url = 'ws://localhost:9080/Test/WSDocker?clientId=e3196ceb5c677376d7dc93a40984e05255b5e96f26a0ab59e4755225b7cacc1f&windowId=123';
	//var url ='ws://192.168.42.248:2375/containers/8d2a9bcb5d94284beeafcaeeaf8fa4512be3f195c249065a0918dcb267e22c65/attach/ws?logs=1&stream=1&stdin=1&stdout=1&stderr=1';
	
	console.log('New WS @ ' + wsurl);
	socket = new WebSocket(wsurl);
	
	socket.onopen = function (ev) {
		if ( bAttach) {
			console.log('WS opened' + JSON.stringify(ev) + ' attaching.');
			term.attach(socket, true, false);  // Attach the above socket to `term`
			return;
		}
		term.write('Connected to ' + node + '/' + id);
		term.prompt();
	}
	socket.onclose = function (evt) {
		console.log('WS closed: ' + JSON.stringify(evt));
		growl ('Socket closed unexpectedly. Code:' + evt.code + ' ' + evt.reason, 'danger');
	}
	if ( bAttach) {
		console.log('skiping onmessage callback');
		return;
	}
	socket.onmessage = function (ev) {
		var data = ev.data;
		console.log('OnMessage: typeof(ev.data) ' + typeof(data) );

       	if (typeof data === 'object') {
            if (data instanceof ArrayBuffer) {
                str = myTextDecoder.decode(data);
                displayData(str);
            }
            else {
                var fileReader_1 = new FileReader();
                fileReader_1.addEventListener('load', function () {
                    str = myTextDecoder.decode(fileReader_1.result);
                    displayData(str);
                });
                fileReader_1.readAsArrayBuffer(data);
            }
        }
        else if (typeof data === 'string') {
			displayData(data);  
        }
        else {
            throw Error("Cannot handle \"" + typeof data + "\" websocket message.");
        }
	};

	function displayData(str) {
		// {"message":"Container 9399aa83cf787a32fcad7262ee5b2f3f3c641d85ffdd0e1a8e7d312b80e4c777 is not running","status":500}
		var json = str.startsWith('{') ? JSON.parse(str) : null; 
		if ( json && json.status >= 400) {
			growl (json.message, 'danger');
			return;
		}
		
		// Fix ANSI esc codes
		var esc = str.replace(/\\x1b/gim, '\x1b');
		
		// fix linux \n => \r\n
		if ( !bAttach ) {
			esc = esc.replace(new RegExp('\n', 'g'), '\r\n');
			esc = esc.slice(6, esc.length - 4);
		}
		//console.log('Write ' + esc);
		term.write(esc);
		term.prompt();
	}
	
}

function ws_attach() {
	console.log("Attach");
	// The following line of code:
	//   1. Attaches the given socket to `term`
	//   2. Sets up bidirectional communication (sends to stdin and renders stdout/stderr)
	//   3. Buffers rendering for better performance
	//term.attach(socket, true, true);
}

function ws_detach() {
	console.log("Detach");
	// https://xtermjs.org/docs/api/addons/attach/
	//term.detach(socket);  // Now the socket is detached. Nothing will be rendered or sent back.
}

//level: info, danger, warning, success
function growl ( text, level, delay) {
	delay = delay || 30000;
	$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
}
	
