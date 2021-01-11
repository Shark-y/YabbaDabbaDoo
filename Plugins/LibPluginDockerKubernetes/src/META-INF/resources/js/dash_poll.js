/**
 * Background member polling functions.
 */

	/**
	 * Poll success callback
	 * response format is in JSON. Get Members response: <pre>
	 {
		    "response": [
		        {
		            "address": "/10.194.29.156:5701",
		            "attributes": {
		                "KEY_CTX_PATH": "/ClusterManager"
		            },
		            "isLocal": true
		        },
		        {
		            "address": "/10.194.29.156:5702",
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
	function poll_cb_success(json) {
		//LOGD("Got Poll JSON: " + JSON.stringify(json));
		if ( !pollActive ) {
			return;
		}

		// consume json: {"status": 200, message: "OK", "response": [ARRAY]}
		if (json.status != 200) {
			setStatusError(json.message);
			return;
		}

		clearStatusMessage();
		
		// member array
		var members 	= json.response;
		var leader 		= typeof(json.clusterLeader) != "undefined" ? json.clusterLeader : null;

		// nodes available? Note: /CloudClusterManager is a member too
		//if ( members.length == 1) {
		if ( members.length == 0) {
			if ( !DATA_TABLE) {
				setStatusOK("No members yet.");
				Charts_HideAllRows();
			}
			else {
				DATA_TABLE.clear().draw();
			}
		}
		else { 
			if (DATA_TABLE) {
				dt_drawMembers(members); 
			}
			else {
				poll_consume(members, leader);
			}
			// Try to speed things up? 
			// STILL chokes @ 400ms setTimeout(poll_consume.bind (null, members, leader), 50);
		}
		
		// poll recurse
		if ( pollActive ) {
			setTimeout("poll()", pollInterval);
		}
	}
	
	// http://192.168.56.1:8080/CloudConnectorNode002/index.jsp -> http://192.168.56.1:8080/CloudConnectorNode002/
	function cleanUpUrl (url ) {
		return url.replace("index.jsp", "");
	}
	
	/*
	 * Draw members in data tables mode
	 * [{"address":"192.168.56.1","timeCreated":1548205262996,"logging":{"clear":{"headers":{},"method":"POST","url":"http://localhost:8080/Node01//LogServlet?op=clear&len=0"},"get":{"headers":{},"method":"GET","url":"http://localhost:8080/Node01//LogServlet"},"view":{"headers":{},"method":"GET","url":"http://localhost:8080/Node01//log/logview.jsp"}},"attributes":{"KEY_CTX_PATH":"Node01","server_failOverType":"CLUSTER_ZEROCONF","KEY_CTX_URL":"http://localhost:8080/Node01/","productType":"CALL_CENTER"},"uuid":"fc409cfe-ed16-4260-b7f1-eaca9117392c","lifeCycle":{"stop":{"headers":{},"method":"POST","url":"http://localhost:8080/Node01//SysAdmin?rq_operation=stop"},"status":{"headers":{},"method":"GET","url":"http://localhost:8080/Node01//OSPerformance"},"start":{"headers":{},"method":"POST","url":"http://localhost:8080/Node01//SysAdmin?rq_operation=start"}},"messageType":"SERVICE_UP","configure":{"store":{"headers":{},"method":"POST","url":"http://localhost:8080/Node01//SysAdmin?rq_operation=confstore&productType=CALL_CENTER"},"get":{"headers":{},"method":"GET","url":"http://localhost:8080/Node01//SysAdmin?op=confget&productType=CALL_CENTER"}},"timeSent":1548205263262}]
	 */
	function dt_drawMembers(members) {
		//LOGD("Data tables: " + JSON.stringify(members));
		DATA_TABLE.clear().draw();
		
		for ( var i = 0 ; i < members.length ; i++) {
			var member 		= members[i];
			var row			= [];

			// "attributes":{"KEY_CTX_PATH":"Node01","server_failOverType":"CLUSTER_ZEROCONF","KEY_CTX_URL":"http://localhost:8080/Node01/","productType":"CALL_CENTER"}
			var attributes	= member.attributes;
			
			if (!attributes.KEY_CTX_PATH) {
				LOGE("Member [" + i + "] missing CTX PATH")
				continue;
			}
			var url 		= attributes.KEY_CTX_URL;		
			var ctxPath		= attributes.KEY_CTX_PATH;
			var statusCode	= attributes.statusCode;
			var statusMsg 	= attributes.statusMessage ? attributes.statusMessage : 'Unknown';

			
			var statusHTML 	= statusCode == 200 ? '<font color=green>' : '<font color=red>';
			statusHTML 		+= statusMsg + '</font>'
			//statusHTML 		+= isLeader;
				
			var processors 	= attributes.AvailableProcessors; // ? attributes.AvailableProcessors : 'N/A'; 
	 		var sysCpuLoad	= attributes.SystemCpuLoad  ? parseFloat(attributes.SystemCpuLoad.toFixed(3)) : 0;
	 		var procCpuLoad	= attributes.ProcessCpuLoad ? parseFloat(attributes.ProcessCpuLoad.toFixed(3)) : 0;
	 		
	 		//var heapFree	= toMB(attributes.heapFree);
	 		var threads		= attributes.peakThreadCount ? attributes.peakThreadCount : 0;
	 		var osName		= attributes.Name; // ? attributes.Name : 'N/A';

	 		var link 		= (url ? '<input id="chkMember' + i + '" type="checkbox"> <a target=_blank href="' + url + '">' + ctxPath + '</a> @ ' + member.nodeAddress : ctxPath) ;
			
			var vendor 		= attributes.vendor ? attributes.vendor : '';
 			var logQs		= 'json=' + encodeURIComponent(JSON.stringify(member.configure)) 
				+ '&productType=' + attributes.productType + '&vendor=' + vendor;
			
 			var logLink 	= (member.logging && member.logging.get ) 
				? '<a target=_blank href="log/logview.jsp?ep=' + cleanUpUrl(member.logging.get.url) + '">Log</a>&nbsp;&nbsp;&nbsp;&nbsp;' 
				:  (member.logging && member.logging.view ) 
					? '<a target=_blank href="' + member.logging.view.url + '">Log</a>&nbsp;&nbsp;&nbsp;&nbsp;'
					: '';
	
 			logLink 		+= '<a href="jsp/config/config_backend.jsp?' + logQs  + '">Configure</a>';

 			var metrics		= /*(osName ? osName : '')  +*/ (processors ? /*', ' + */ processors + ' Cpu(s)' : '')
 							+ ', ' + threads + ' Threads'; // + logLink;
 			/*
 			row.push('');		// details control
			row.push(link);
			row.push(vendor);
			row.push(statusHTML);
			//row.push((osName ? 'OS: '+ osName : '')  + (processors ? ' , ' + processors + ' Cpu(s)' : ''));
			//row.push(threads + ' Threads &nbsp;&nbsp;&nbsp;' + logLink);
			row.push(details);
			row.push(member);
			*/
 			row = {'name': link, 'vendor': vendor, 'status': statusHTML, 'os': osName, 'details': logLink, 'metrics': metrics, 'member': member};
			DATA_TABLE.row.add(row).draw();
		}
	}
	
	/**
	 * Draw members for dashboard or tbl modes
	 * @param members
	 * @param leader
	 */
	function poll_consume (members, leader) {
		var t0 = new Date();
		for ( var i = 0 ; i < CHARTS_SIZE ; i++) {
			if ( i < members.length) { 
				Charts_UnHideRow(i);
				Charts_AddMember(i, members[i], leader);
			}
			else {
				//LOGD("Hide row " + i)
				Charts_HideRow(i);
			} 
		}
		var t1 = new Date();
		LOGI("Poll consumed " + members.length + " members in " + (t1-t0) + "ms." );
	}
	
	/**
	 * Poll error callback
	 */
	function poll_cb_error(jqXHR, textStatus) {
		LOGW("Poll failed with status: " + textStatus);
		setStatusError("Request failed with status: " + textStatus);

		// recurse in case the long poll expired
		setTimeout("poll()", pollInterval * 2);
	}
	
	/**
	 * Start polling for cluster members using ajax.
	 */
	function poll() {
		// get members operation
		var url = pollEndPoint + '?rq_operation=getmembers&cb=' + new Date().getTime();
		LOGD("Poll-Cluster " + url);
		
		$.ajax({
			type : 'GET',
			url : url,
			// request response in json!
			headers : {
				"Accept" : "application/json; charset=utf-8",
			},
			cache : false
			//data: { rq_clientId: clientId, rq_windowId: windowId, rq_operation: 'poll' }
		})
		.done(poll_cb_success)
		.fail(poll_cb_error);
		
		/* 1/19/2019 OBSOLETE
		try {
			// cfg_sep.js
			pollServices (serviceUrls.split(','));
		}
		catch (e) {
			LOGE("Poll services:" + e);
		} */
	}

	//--------------------------------------------------------------------------------
	
	/**
	 * Fires on when the Start/stop btn is pressed.
	 * Start/Stop a set of nodes.
	 */
	function clusterLoop (operation, mode)  {
		var table 		= document.getElementById(TBL_NODES);
		var rowCount 	= table.tBodies[0].rows.length;
		
		if ( !loggedIn) {
			alert("Login first.");
			return;
		}
		
		// count selected
		count = 0;
		//LOGD("row count=" + rowCount + " mode=" + mode);
		for ( var i = 0 ; i < rowCount ; i++) {
			//LOGD("display row [" + i + "]=" + table.rows[i].style.display );
			if ( table.rows[i].style.display == 'none') {
				continue;				
			}
			var realIdx = mode == 'tbl' && (i > 0) ? i - 1 : i;
			var chkbox 	= document.getElementById('chkMember'+ realIdx); 
			//LOGD("display chk [" + realIdx + "] checked =" + chkbox.checked);
			if (chkbox.checked )  {count++; }
		}
		
		LOGD("Cluster Loop op=" + operation + " mode=" + mode + " Checked count=" + count);
		
		if ( count == 0 ) {
			//alert("Select nodes first.");
			notify("Select nodes first.", 'warning');
			return;
		}
		
		//setStatusWait();
		notify("Please wait.", 'info');

		for ( var i = 0 ; i < rowCount ; i++) {
			if ( table.rows[i].style.display == 'none') {
				//LOGD('[' + i + '] Skip hidden row ' + i);
				continue;				
			}
			var realIdx 	= mode == 'tbl' && (i > 0) ? i - 1 : i;
			var chkbox 		= document.getElementById('chkMember'+ realIdx); 
			var checked 	= chkbox.checked;

			// Zeroconf INPUT(s)
			var type 		= $('#meta_type'+ realIdx).val();
			var json_lc 	= $('#meta_lc'+ realIdx).val();
			var zeroConf 	= type != '' && json_lc != '' && type.includes('SERVICE');
			
			LOGD('[' + i + '] Zeroconf: ' + zeroConf + ' i=' + i + ' realIdx=' + realIdx 
					+ ' type[' + realIdx + ']=' + type + ' checked=' + checked
					/*+ ' JSONLC[' + realIdx + ']=' + json_lc */ );

			if ( !zeroConf ) {
				var span = document.getElementById('ep' + realIdx);
				if ( span.childNodes && span.childNodes.length == 0 ) {
					LOGD('[' + i + '] HZ Skip span ' + realIdx + ' no EP @ span ' + 'ep' + realIdx);
					continue;
				}
			}
			
			if ( checked) {
				var sysUrl 	= null;
				
				// Zeroconf?
				if ( zeroConf ) {
		 			/** { "start": {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=start", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}},
					"stop" : {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=stop", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}},
					"status": {"method": "GET", "url": "http://localhost:8080/Node001/OSPerformance", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}} } */
					//LOGD("Zeroconf JSON  [" + i + "] = " + json_lc);
					
					// {"stop":{"headers":{},"method":"POST","url":"http://192.168.56.1:8080/CloudConnectorNode002//SysAdmin?rq_operation=stop"},"status":{"headers":{},"method":"GET","url":"http://192.168.56.1:8080/CloudConnectorNode002//OSPerformance"},"start":{"headers":{},"method":"POST","url":"http://192.168.56.1:8080/CloudConnectorNode002//SysAdmin?rq_operation=start"}}
					var json 	= JSON.parse(json_lc);
					var method 	= json[operation].method;
					sysUrl 		= json[operation].url;
					
					LOGD('[' + i + '] Zeroconf ' + operation + " " + method + " " + sysUrl);
					ajaxExecute(method, sysUrl, { });
				}
				else {
					// http://host:8080/CloudAdapterNode001/
					var rootUrl	= span.childNodes[0]; 
					sysUrl 		= rootUrl + 'SysAdmin';
	
					LOGD('[' + i + '] ' + operation +  " = " + sysUrl);
					ajaxExecute('POST', sysUrl, { 'rq_operation': operation});
				}
			} 
		}
	}

	function dt_ClusterLoop (operation, mode) {
		var _notify = false;
		DATA_TABLE.rows().every ( function () {
			var d 			= this.data();
			var member		= d['member']; //d[5];
			var index		= this.index();
			//LOGD("member: " + JSON.stringify(member));
			
			// {"stop":{"headers":{},"method":"POST","url":"http://192.168.42.185:8080/CloudConnectorNode002//SysAdmin?rq_operation=stop"},"status":{"headers":{},"method":"GET","url":"http://192.168.42.185:8080/CloudConnectorNode002//OSPerformance"},"start":{"headers":{},"method":"POST","url":"http://192.168.42.185:8080/CloudConnectorNode002//SysAdmin?rq_operation=start"}}
			var lifeCycle	= member.lifeCycle;
			var method 		= lifeCycle[operation].method;
			var sysUrl 		= lifeCycle[operation].url;
			var checked		= $('#chkMember' + index).prop('checked');
			
			if ( checked) {
				LOGD('Zeroconf ' + operation + " " + method + " " + sysUrl);
				ajaxExecute(method, sysUrl, { });
				_notify = true;
			}
		});
		if ( _notify ) {
			notify("Please wait.", 'info');
		}
	}
	
	function clusterStart(mode) {
		if ( mode == 'tbldt') {
			dt_ClusterLoop ('start', mode);
		}
		else {
			clusterLoop('start', mode);
		}
	}
	function clusterStop(mode) {
		if ( mode == 'tbldt') {
			dt_ClusterLoop ('stop', mode);
		}
		else {
			clusterLoop('stop', mode);
		}
	}
	
	/**
	 * Exec a JSON call. Used to invoke the start/stop op for a node.
	 * @param method: HTTP method type: GET, POST, etc.
	 * @param url Request URL
	 * @param data Request data: JSON object. { rq_operation: 'poll', key2, val2, ... }
	 */
	function ajaxExecute(method, url, rqdata) {
		$.ajax({
			type : method,
			url : url,
			// request response in json!
			headers : {
				"Accept" : "application/json; charset=utf-8",
			},
			cache : false,
			data: rqdata
		});
		//.done(poll_cb_success)
		//.fail(poll_cb_error);
	}
	