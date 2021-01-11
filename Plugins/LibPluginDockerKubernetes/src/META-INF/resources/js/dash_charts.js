/**
 * Charting functions. Here is the AJAX JSON format:
 * {
    "response": [
        {
            "address": "/192.168.30.1:5701",
            "attributes": {
                "FreePhysicalMemorySize": 4294967295,
                "Name": "Windows 7",
                "AvailableProcessors": 4,
                "KEY_CTX_PATH": "/ClusterManager",
                "peakThreadCount": 9,
                "heapFree": 3826160,
                "SystemCpuLoad": -1
            },
            "isLocal": true
        },
        {
            "address": "/192.168.30.1:5702",
            "attributes": {
                "FreePhysicalMemorySize": 4294967295,
                "statusCode": 200,
                "Name": "Windows 7",
                "AvailableProcessors": 4,
                "ProcessCpuLoad": -1,
                "KEY_CTX_PATH": "/CloudAdapterNode002",
                "peakThreadCount": 155,
                "KEY_CTX_URL": "http://VLADS5014:8080/CloudAdapterNode002/",
                "heapFree": 19505952,
                "heapTotal": 106467328,
                "statusMessage": "Online",
                "SystemCpuLoad": -1,
                "heapMax": 259522560
            },
            "isLocal": false
        }
    ],
    "message": "OK",
    "status": 200
}
 */


	function Charts_Init() {
		var t0 = new Date();
		for ( var i = 0 ; i < CHARTS_SIZE; i ++) {
			CHARTS[i] = {};
			
			var configCpu = {
					type : 'line',
					data : {
						labels : [ "", "", "", "", "" ],
																					
						datasets : [ {
							label : "System CPU",
							data : [ 0, 0, 0, 0, 0 ], 
							fill : false,
							borderDash : [ 5, 5 ],
							borderColor: 'rgb(0,0,255)',
							backgroundColor: 'rgb(0,0,255)'
						}, 
						{
							label : "Process CPU",
							data : [ 0, 0, 0, 0, 0 ] 
						} ]
					},
					options : {
						responsive : true,
						maintainAspectRatio : true,
						title : {
							display : true,
							text : 'CPU Load'
						},
						scales : {
							xAxes : [ {
								display : true,
								scaleLabel : {
									display : true,
									labelString : 'Time'
								}
							} ],
							yAxes : [ {
								display : true,
								ticks: { beginAtZero : true },
								scaleLabel : {
									display : true,
									labelString : 'Value'
								}
							} ]
						}
					}
				};
			
			// CPU
			CHARTS[i].CPU = {};
			CHARTS[i].CPU.Chart 	= null; //new Chart(document.getElementById('chart_cpu' + i).getContext("2d"), configCpu);
			CHARTS[i].CPU.Config	= configCpu;
			/*
			// DEPRECATED CHARTS[i].CPU.Chart = new google.visualization.AreaChart(document.getElementById('chart_cpu' + i));
			CHARTS[i].CPU.Chart =  new JustGage({id: 'chart_cpu' + i, value: 0, title: 'CPU % (Process)'});
			CHARTS[i].CPU.AxisX = ["", "", "", ""];
			CHARTS[i].CPU.AxisY1 = [0,  0, 0 , 0];
			CHARTS[i].CPU.AxisY2 = [0,  0, 0 , 0]; */
			
			var configMem = {
					type : 'line',
					data : {
						labels : [ "", "", "", "", "" ],
																					
						datasets : [ {
							label : "Total",
							data : [ 0, 0, 0, 0, 0 ], 
							fill : false,
							borderDash : [ 5, 5 ],
							borderColor: 'rgb(0,0,255)',
							backgroundColor: 'rgb(0,0,255)'
						}, 
						{
							label : "Used",
							data : [ 0, 0, 0, 0, 0 ],
							borderColor: 'rgb(0,255,0)',
							backgroundColor: 'rgb(0,255,0)'
						},
						{
							label : "Max",
							data : [ 0, 0, 0, 0, 0 ] 
						} ]
					},
					options : {
						responsive : true,
						maintainAspectRatio : true,
						title : {
							display : true,
							text : 'Memory (MB)'
						},
						scales : {
							xAxes : [ {
								display : true,
								scaleLabel : {
									display : true,
									labelString : 'Time'
								}
							} ],
							yAxes : [ {
								display : true,
								scaleLabel : {
									display : true,
									labelString : 'Value (MB)'
								}
							} ]
						}
					}
				};
			
			// MEM
			CHARTS[i].MEM = {};
			CHARTS[i].MEM.Chart 	= null; //new Chart(document.getElementById('chart_mem' + i).getContext("2d"), configMem);
			CHARTS[i].MEM.Config 	= configMem;
			/*
			// DEPRECATED CHARTS[i].MEM.Chart = new google.visualization.AreaChart(document.getElementById('chart_mem' + i));
			CHARTS[i].MEM.Chart = Morris.Donut( { element: 'chart_mem' + i, resize: true, data: Morris_Data(0,0,0) } );
			CHARTS[i].MEM.AxisX = ["", "", "", ""];
			CHARTS[i].MEM.AxisY1 = [0,  0, 0 , 0];
			CHARTS[i].MEM.AxisY2 = [0,  0, 0 , 0];
			CHARTS[i].MEM.AxisY3 = [0,  0, 0 , 0];
			*/
		}
		
		var t1 = new Date();
		LOGI("Initialized " + CHARTS_SIZE + " charts " + (t1-t0) + " ms." );
	}
	
	/**
	 * Draw CPU load.
	 * @param idx Chart index from CHARTS array.
	 * @param cpu1 SYS CPU Load value
	 * @param cpu2 PROC CPU value
	 * @param time X-Axis value (time)
	 */
    function Charts_DrawCPULoad(idx, cpu1, cpu2, time) {
    	if ( idx < 0 ) { 
    		LOGE("Draw: Invalid chart idx " + idx);
    		return;
    	}
    	if ( typeof(CHARTS[idx]) == "undefined" ) {
    		return;
    	}
    	
    	var config 	= CHARTS[idx].CPU.Config;
    	
    	if ( ! CHARTS[idx].CPU.Chart) {
    		LOGD('Init chart_cpu' + idx);
    		CHARTS[idx].CPU.Chart = new Chart(document.getElementById('chart_cpu' + idx).getContext("2d"), config);
    	}
    	// Time label
    	config.data.labels.shift();
      	config.data.labels.push(time);
      	
      	// Sys cpu
      	config.data.datasets[0].data.shift();
      	config.data.datasets[0].data.push(cpu1 * 100);

      	// proc cpu
      	config.data.datasets[1].data.shift();
      	config.data.datasets[1].data.push(cpu2 * 100);
      	
      	CHARTS[idx].CPU.Chart.update();
    	
    	/* Deprecated Just Gauge
    	var tx 		= CHARTS[idx].CPU.AxisX;
    	var y1 		= CHARTS[idx].CPU.AxisY1;
    	var y2 		= CHARTS[idx].CPU.AxisY2;
    	
		// shift Data
		y1.shift(); y1.push(cpu1);
		y2.shift(); y2.push(cpu2);
		tx.shift(); tx.push(time);
		
		//LOGD("CPU["+ idx + "] SYS: " + tp + " PROC:" + cpu2  + " Time:" + time);
		
		// Gauge PROC CPU
		chart.refresh(cpu2 * 100);
		*/
		//setObjHTML("chart_title", idx, '<h4>' + time + '</h4>');
		
		/* deprecated Google charts
		var data = google.visualization.arrayToDataTable([
	        ['Time', ' System CPU', 'Process CPU']
	        , [tx[0],  y1[0],    y2[0]]
	        , [tx[1],  y1[1],     y2[1]]
	        , [tx[2],  y1[2],     y2[2]]
	        , [tx[3],  y1[3],     y2[3]]
    	]);

     	var options = {
			width : 450,
			height : 300,
        	title: 'CPU Load',
        	hAxis: {title: 'Time',  titleTextStyle: {color: '#333'}},
        	vAxis: {minValue: 0}
      	};
      	chart.draw(data, options); */
    }
    
	/* deprecated
    function Morris_Data (free, total, max) {
		var data = [];
		data.push ({label: "Free (MB)", value: free}, {label: "Total (MB)", value: total}, {label: "MEM Max", value: max});
		return data;
    } */
    
    /**
     * Draw Memory in MB.
     * @param idx HTML Table Row index.
     * @param total Total mem (MB).
     * @param free Free mem (MB).
     * @param max Max mem (MB).
     * @param time X-axis (time) hh:mm:ss AM/PM.
     */
    function Charts_DrawMem(idx, total, free, max, time) {
    	if ( idx < 0 || (CHARTS.length == 0) ) { 
    		//LOGE("DrawMem: Invalid chart idx " + idx);
    		return;
    	}

    	var config 	= CHARTS[idx].MEM.Config;
    	
    	if( ! CHARTS[idx].MEM.Chart ) {
    		LOGD('Init chart_mem' + idx)
    		CHARTS[idx].MEM.Chart = new Chart(document.getElementById('chart_mem' + idx).getContext("2d"), config);
    	}
    	
      	config.data.labels.shift();
      	config.data.labels.push(time);
      	
      	// total
      	config.data.datasets[0].data.shift();
      	config.data.datasets[0].data.push(total);

      	// free
      	config.data.datasets[1].data.shift();
      	config.data.datasets[1].data.push(free);

      	// Max
      	config.data.datasets[2].data.shift();
      	config.data.datasets[2].data.push(max);
      	
      	CHARTS[idx].MEM.Chart.update(); 
    	
    	/* Deprecated Morris domut
    	// Mem chart dimensions
    	var m1 = CHARTS[idx].MEM.AxisY1;
    	var m2 = CHARTS[idx].MEM.AxisY2;
    	var m3 = CHARTS[idx].MEM.AxisY3;
    	var mx = CHARTS[idx].MEM.AxisX;
    	
		// shift Data
		m1.shift(); m1.push(total);
		m2.shift(); m2.push(free);
		m3.shift(); m3.push(max);
		mx.shift(); mx.push(time);

		//LOGD("MEM["+ idx + "] Total: " + total + " Free:" + free  + " Max:" + max + " Time:" + time);
		
		// morris donut
		chart.setData( Morris_Data (free, total, max));
		*/
    	
		/* deprecated - Google charts
		var data = google.visualization.arrayToDataTable([
	        ['Time', ' Total', 'Free',   'Max']
	        , [mx[0],  m1[0],    m2[0],  m3[0]]
	        , [mx[1],  m1[1],    m2[1],  m3[1]]
	        , [mx[2],  m1[2],    m2[2],  m3[2]]
	        , [mx[3],  m1[3],    m2[3],  m3[3]]
      	]);

     	var options = {
			width : 450,
			height : 300,
     		title: 'Memory Heap (MB)',
        	hAxis: {title: 'Time',  titleTextStyle: {color: '#333'}},
        	vAxis: {minValue: 0}
      	};
      	chart.draw(data, options); */
    }

	/**
	 * { "uuid": "fcb061f0-ff75-445f-820e-60472fcd5c43", "address": "/10.194.29.156:5702", "attributes": { 
	 *	 	"statusCode": 200, "KEY_CTX_PATH": "/CloudAdapterNode001",
	 *		"KEY_CTX_URL": "http://VLADS5014:8080/CloudAdapterNode001/",
	 *		"statusMessage": "Online"
	 * 		}, 
	 * 	"isLocal": false,
	 *  "messageType" : "SERVICE",
	 *	"lifeCycle": {
		"start": {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=start", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}},
		"stop" : {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=stop", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}},
		"status": {"method": "GET", "url": "http://localhost:8080/Node001/OSPerformance", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}}
	},
	"configure": {
		"get": {"method": "GET", "url": "http://localhost:8080/Node001/Confget", "headers": {}},
		"store" : {"method": "POST", "url": "http://localhost:8080/Node001/Confstore", "headers": {}}
	},
	"logging": {
		"view": {"method": "GET", "url": "http://localhost:8080/Node001/log/logview.jsp", "headers": {}},
		"clear" : {"method": "POST", "url": "http://localhost:8080/Node001/LogServlet?op=clear&len=0", "headers": {}}
	}
	 * }
	 * Metrics (under attributes): AvailableProcessors=4, SystemCpuLoad=0.021457471, Name=Windows 7, peakThreadCount=85, FreePhysicalMemorySize=4294967295
	 */
	function Charts_AddMember (idx, json, leader) {
		var isLocal 	= json.isLocal;
		var attributes 	= json.attributes;
		
		var ctxPath		= attributes.KEY_CTX_PATH;
		var statusCode	= attributes.statusCode;
		var statusMsg 	= attributes.statusMessage;
		var url 		= attributes.KEY_CTX_URL;		// optional for the CM!
		var runMode		= "";		// PRIMARY, SECONDARY or CLUSTER
		var vendor		= "";		// MB Vendor/CC Vendor
		var isLeader	= json.uuid == leader ? " <font color=blue>(Leader)</font>" : "";
			
		LOGD("Add member[" + idx + "] Local :" + isLocal + " Name: " + ctxPath + " Status:" + statusCode + " Msg: " + statusMsg + " Url: " + url );
		LOGD("Add member[" + idx + "] Leader: " + leader + " My Id: " + json.uuid + " MsgType: " + json.messageType);
		
		if ( typeof(ctxPath) == "undefined" ) {
			LOGE("Invalid member attributes (missing ctx path): " + JSON.stringify(attributes));
			Charts_HideRow(idx);
			return;
		}
		
		// ignore local CM
		if ( isLocal) {
			LOGI("Add member[" + idx + "] Hiding slot for local member " + ctxPath + " Slot:" + idx);
			Charts_HideRow(idx);
			return;	
		}
		
		// reject other cluster managers: ctxPath = CloudClusterMananger...
		if ( ctxPath.indexOf('ClusterManager') != -1) {
			LOGI("Add member[" + idx + "] Rejecting cluster manager @ row " + idx + " " + ctxPath + ' Url:' + url);
			Charts_HideRow(idx);
			return;
		}
		
		// No status? This happens if the poll occurs when the server is booting up!
		if ( typeof(statusMsg) == "undefined" ) {
			statusMsg = "Unknown";
		}
		
		// missing URL? Attempt to build it from the member json.address = /10.194.29.156:5702
		// This happens if the admin console is not opened for that node.
		// The URL will get refreshed when the console is opened
		if ( typeof(url) == "undefined") {
			url = json.nodeAddress.indexOf(":") != -1 
				? "http:/" + json.nodeAddress.split(":")[0] + ":8080" + ctxPath + "/"
				: "http:/" + json.nodeAddress + ":8080" + ctxPath + "/";
			LOGW("Missing member url for " + ctxPath + ". Assuming " + url);
		}
		
		// some times URL is http://HOST:PORT/CloudAdapterNode001/index.jsp (strip index.jsp)
		if ( url.charAt(url.length - 1) != '/' ) {
			url = url.substring(0, url.lastIndexOf('/') + 1) ;
		}

		// Optional: attributes.runMode & attributes.connectionProfile
		if ( attributes.runMode ) {
			runMode = attributes.runMode;
		}
		if ( attributes.connectionProfile) {
			runMode += " @ " + attributes.connectionProfile;
		}
		
		// MB Vendor/CC Vendor
		if ( attributes.vendor ) {
			vendor = '<font color=gray>' + attributes.vendor + '</font>';
		}
		
		// add row
		var statusHTML 	= statusCode == 200 ? '<font color=green>' : '<font color=red>';
		statusHTML 		+= statusMsg + '</font>'
		statusHTML 		+= isLeader;
			
		var processors 	= attributes.AvailableProcessors ? attributes.AvailableProcessors : 'N/A'; 
 		var sysCpuLoad	= attributes.SystemCpuLoad  ? parseFloat(attributes.SystemCpuLoad.toFixed(3)) : 0;
 		var procCpuLoad	= attributes.ProcessCpuLoad ? parseFloat(attributes.ProcessCpuLoad.toFixed(3)) : 0;
 		
 		//var heapFree	= toMB(attributes.heapFree);
 		var threads		= attributes.peakThreadCount ? attributes.peakThreadCount : 0;
 		var osName		= attributes.Name ? attributes.Name : 'N/A';

 		var link 		= '<a target=_blank href="' + url + '">' + ctxPath + '</a> @ ' + json.nodeAddress;
 		var logLink 	= '<a target=_blank href="' + url + 'log/logview.jsp">Log</a>';
 		
 		// zeroconf?
 		if ( json.messageType) {
 			if (json.messageType == 'SERVICE_UP') {
	 			var logQs	= 'json=' + encodeURIComponent(JSON.stringify(json.configure)) 
	 						+ '&productType=' + attributes.productType + '&vendor=' + attributes.vendor;
	 			
	 			logLink 	=  (json.logging && json.logging.get ) 
	 						? '<a target=_blank href="log/logview.jsp?ep=' + json.logging.get.url + '">Log</a>&nbsp;&nbsp;&nbsp;&nbsp;' 
	 						:  (json.logging && json.logging.view ) 
	 							? '<a target=_blank href="' + json.logging.view.url + '">Log</a>&nbsp;&nbsp;&nbsp;&nbsp;'
	 							: '';
	 			
	 			logLink 	+= '<a href="jsp/config/config_backend.jsp?' + logQs  + '">Configure</a>';
	 			/** {
					"start": {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=start", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}},
					"stop" : {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=stop", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}},
					"status": {"method": "GET", "url": "http://localhost:8080/Node001/OSPerformance", "headers": {"Authorization":"bearer ACESS_TOKEN","Foo":"Bar"}}
				} */
	 			var meta_lc = JSON.stringify(json.lifeCycle);
	 			$("#meta_type" + idx).val(json.messageType);
	 			$("#meta_lc" + idx).val(meta_lc);
 			}
 			
 			else if (json.messageType == 'SERVICE_DOWN') {
 				// TODO find uuid/CTX_ROOT & remove it or let it expire.
 				LOGI("Got " + json.messageType + " @ row " + idx + " Path: " + ctxPath + ". Hiding row " + idx);
 				Charts_HideRow(idx);
 				return;
 			} 
 		}
 		
		// Name: context path + metrics
		// Metrics: AvailableProcessors=4, SystemCpuLoad=0.021457471, Name=Windows 7, peakThreadCount=85, FreePhysicalMemorySize=4294967295
		// Draw CPU Load (area)
		var d 		= new Date();
		var n 		= d.toLocaleTimeString();
		var slot 	= idx;
		
		/*
		LOGD("Chart member[" + idx + "] Name: " + ctxPath + " Status:" + statusCode 
				+ " Msg: " + statusMsg + " Url: " + url + " Processors:" + processors
				+ " SysCpu:" + sysCpuLoad + " ProcCpu:" + procCpuLoad
				+ " Threads: " + threads
				+ " X: " + n); */
		
		// CPU 
		Charts_DrawCPULoad(slot , sysCpuLoad, procCpuLoad, n) ;
		
		// MEM: "attributes":{"peakThreadCount":9,"heapFree":3826160,"heapMax":3826160,"heapTotal":3826160,...}
		var max 	= attributes.heapMax   ? attributes.heapMax : 0;		// max (bytes)
		var total 	= attributes.heapTotal ? attributes.heapTotal : 0;  	// total (bytes)
		var free 	= attributes.heapFree  ? attributes.heapFree : 0; 		// free (bytes)
		var used 	= total - free;				// used (bytes) NOT in OS METRICS

		Charts_DrawMem(slot, toMB(total), toMB(used), toMB(max), n);
		
		// Node info...
		setObjHTML("ep", slot , link);
		setObjHTML("status", slot , statusHTML);
		setObjHTML("osname", slot , osName);
		setObjHTML("procs", slot , processors);
		setObjHTML("threads", slot , threads);
		setObjHTML("runmode", slot , runMode);
		setObjHTML("vendor", slot , vendor);
		setObjHTML("log", slot , logLink);
	 }

	 /**
	  * Set the InnerHTML of an obj.
	  */
	 function setObjHTML(key, idx, html) {
		 var id 	= key + idx;
		 var obj 	= document.getElementById(id);
		 //LOGD("id:" + id + " obj:" + obj);
		 if ( !obj)  {
			 LOGE("SetValue: Invalid object with id: " + id);
			 return;
		 }
		 obj.innerHTML = html
	 }

	 /**
	  * Hide a specific node row.
	  * @param idx row index.
	  */
	 function Charts_HideRow(idx) {
		 var row = document.getElementById("row" + idx);
		 if ( row ) {
			 row.style.display = 'none';
		 }
		 else {
			 LOGE("Unable to hide row " + idx);
		 }
	 }

	 function Charts_UnHideRow(idx) {
		 var row = document.getElementById("row" + idx);
		 if ( row ) {
			 //row.style.display = 'table-row';
			 row.style.display = 'block';
		 }
	 }
	 
	 /**
	  * Hide all node rows.
	  */
	 function Charts_HideAllRows () {
		 for ( var i = 0 ; i < CHARTS_SIZE ; i++) {
			 Charts_HideRow(i);
		 }
	 }
