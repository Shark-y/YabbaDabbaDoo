/**
 * Hazelcast configuration code for config_cluster.jsp
 */

/**
 * Poll success callback
 * response format is in JSON. Get Members response: <pre>
 {
	    "response": [
	        {
	            "address": "/XX.XX.XX.XXX:5701",
	            "attributes": {
	                "KEY_CTX_PATH": "/ClusterManager"
	            },
	            "isLocal": true
	        },
	        {
	            "address": "/XX.XXX.X.XX:5702",
	            "attributes": {
	                "statusCode": 200,
	                "KEY_CTX_PATH": "/CloudAdapterNode001",
	                "KEY_CTX_URL": "http://VLADS5014:8080/CloudAdapterNode001/",
	                "statusMessage": "Online",
	                AvailableProcessors=4, SystemCpuLoad=0.021457471, Name=Windows 7, 
	                peakThreadCount=85, FreePhysicalMemorySize=4294967295, heapFree=1234
	            },
	            "uuid": "fcb061f0-ff75-445f-820e-60472fcd5c43",
	            "isLocal": false
	        },
	        ...
	    ],
	    "message": "OK",
	    "status": 200,
	    "clusterLeader": "7189aa89-8186-402a-9413-e3c632efbece"
	}</pre>
 */
function addNodes (json) {
	var table		= document.getElementById('tblNodes');
	var response	= json.response;
	var leader		= typeof(json.clusterLeader) != 'undefined' ? json.clusterLeader : null;
	
	// del old rows
	cleanTable (table);
	
	// Poll Response: {"message":"Cluster not yet initialized.","status":200}
	if ( typeof(response) == 'undefined') return;
	
	for ( var i = 0 ; i < response.length ; i++ ) {
		var row 	= table.insertRow(table.rows.length);
		var node 	= response[i];
		var attribs = node.attributes;
		var uuid	= typeof(node.uuid) != 'undefined' ? node.uuid : null;
		var ipPort	= node.address.substring(1);		//	/XX.XXX.XX.XXX:5702
		var ip		= ipPort.indexOf(':') != -1 ? ipPort.substring(0, ipPort.indexOf(':')) : ipPort ;
			
		if ( typeof(attribs) == 'undefined') continue;
			
		var path	= attribs["KEY_CTX_PATH"];		// Context root /CloudContact...
		var url 	= typeof(attribs["KEY_CTX_URL"]) != 'undefined' 	? attribs["KEY_CTX_URL"] : 'http://' + ip + ':8080' + path;
		var status 	= typeof(attribs["statusMessage"]) != 'undefined' 	? attribs["statusMessage"] : 'Unknown';
		var details = attribs["Name"] + ', ' + attribs["AvailableProcessors"] + " Cpu(s)";
		
		// missing KEY_CTX_URL means node needs configuration.
		if ( typeof(attribs["KEY_CTX_URL"]) == 'undefined' ) 	status = 'Configuration required.';
		
		LOGD('Node: ' + path + ' Status:' + status + ' Url:' + url);
		
		if ( typeof(path) == 'undefined') {
			LOGE('Invalid node (missing attributes): ' + JSON.stringify(node) );
			continue;
		}
		
		// ignore CM
		if ( path.indexOf('ClusterManager') != -1 ) continue;
		
		// color code the status
		var htmlColor 	= status.indexOf('Online') != -1 ? 'green' : 'red';
		var htmlLeader 	= (leader && uuid && (leader == uuid)) ? " <font color=blue>*LEADER*</font>" : "";
		
		// add row
		row.insertCell(0).innerHTML = '<a target=_new href="' + url + '">' +  path.substring(1) + "</a> @ " + ipPort + htmlLeader;
		row.insertCell(1).innerHTML = '<font color=' + htmlColor + '>' + status + '</font>';
		row.insertCell(2).innerHTML = details;
	}
}

/**
 * Poll callbacks
 * response format is in JSON:
 * {"message":"OK","status":200, "response": 
 *	 [{"address":"/XX.XXX.XX.X:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"},"isLocal":true}] }
 *  
 */
function poll_cb_success(json) {
	//LOGD("Got Poll Response: " + JSON.stringify(json));
	
	// consume json: {"status": 200, "message": 'OK', ...}
	if (json.status != 200) {
		setErrorStatus(json.message);
		return;
	}
	//clearStatusMessage();
	addNodes(json);
	
	if ( STOP_POLL) {
		LOGD("Poll canceled.");
		return;
	}
	// poll recurse
	setTimeout("poll('" + pollEndPoint + "')", 5000);
}

function poll_cb_error(jqXHR, textStatus) {
	LOGW("Poll failed with status: " + textStatus);
	//setErrorStatus("Poll failed with status: " + textStatus);

	// recurse in case the long poll expired
	setTimeout("poll('" + pollEndPoint + "')", 10000);
}

/**
 * Start polling against the local cluster node (Hazelcast)
 */
var pollEndPoint;

function poll (url) {
	LOGD("Polling " + url);
	pollEndPoint	= url;
	
	$.ajax({
		type : 'GET',
		url : url,
		// request response in json!
		headers : {
			"Accept" : "application/json; charset=utf-8",
			"Content-Type" : "application/json; charset=utf-8"
		},
		cache : false
		//data: { rq_clientId: clientId, rq_windowId: windowId, rq_operation: 'poll' }
	})
	.done(poll_cb_success)
	.fail(poll_cb_error);
}
