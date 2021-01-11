/**
 * Chat.js Configurations
 */
var configCpu = {
	type : 'line',
	data : {
		labels : [ "", "", "", "", "" ],
																	
		// labels: ["1:00", "1:05", "1:10", "1:20", "1:30"],
		datasets : [ {
			label : "System",
			data : [ 0, 0, 0, 0, 0 ], 
			fill : false,
			borderDash : [ 5, 5 ],
			borderColor: 'rgb(0,0,255)',
			backgroundColor: 'rgb(0,0,255)'
		}, 
		{
			label : "JVM",
			data : [ 0, 0, 0, 0, 0 ] 
		} ]
	},
	options : {
		responsive : true,
		title : {
			display : true,
			text : 'Historic CPU Usage'
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
				// type: 'logarithmic',
				scaleLabel : {
					display : true,
					labelString : 'Value'
				}
			} ]
		}
	}
};

/**
 * Memory: Total, Free, Max
 */
var configMem = {
		type : 'line',
		data : {
			labels : [ "", "", "", "", "" ],
																		
			// labels: ["1:00", "1:05", "1:10", "1:20", "1:30"],
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

/**
 * Threads chart: Daemon, Peak.
 */
var configTr = {
		type : 'line',
		data : {
			labels : [ "", "", "", "", "" ],
																		
			// labels: ["1:00", "1:05", "1:10", "1:20", "1:30"],
			datasets : [ {
				label : "Daemon",
				data : [ 0, 0, 0, 0, 0 ], 
				fill : false,
				borderDash : [ 5, 5 ],
				borderColor: 'rgb(0,0,255)',
				backgroundColor: 'rgb(0,0,255)'
			}, 
			{
				label : "Peak",
				data : [ 0, 0, 0, 0, 0 ] 
			},
			{
				label : "Live",
				data : [ 0, 0, 0, 0, 0 ],
				borderColor: 'rgb(0,255,0)',
				backgroundColor: 'rgb(0,255,0)'
			}]
		},
		options : {
			responsive : true,
			title : {
				display : true,
				text : 'Threads'
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
						labelString : 'Value'
					}
				} ]
			}
		}
	};

/**
 * Container Threads
 */
var configContainer = {
		type : 'bar',
		data: {
			labels: [],
		    datasets: [{
		            label: 'Connections',
		            backgroundColor: "rgba(220,220,220,0.5)",
		            data: []
		        }, {
		            label: 'Threads',
		            backgroundColor: "rgba(151,187,205,0.5)",
		            data: []
		        }, {
		            label: 'Busy Threads',
		            backgroundColor: "rgba(0,0,255,0.5)",
		            data: []
		        }, {
		            label: 'Max Threads',
		            backgroundColor: "rgba(151,0,205,0.5)",
		            data: [],
		            hidden: true
		        }]
		},
		options : {
			responsive : true,
			title : {
				display : true,
				text : 'Container Metrics'
			}
		}
};

/**
 * Container Global Request Processor
 */
var configCGRP = {
		type : 'horizontalBar',
		data: {
			labels: [],
		    datasets: [{
		            label: 'Requests/Sec',
		            backgroundColor: "rgba(220,220,220,0.5)",
		            data: []
		        }, {
		            label: 'Throughput (KB/s)',
		            backgroundColor: "rgba(151,187,205,0.5)",
		            data: []
		        }, {
		            label: 'Error Count',
		            backgroundColor: "rgba(255,0,0,0.5)",
		            data: []
		        }]
		},
		options : {
			responsive : true,
			title : {
				display : true,
				text : 'Global Request Processor Transfer Rates'
			}
		}
};

/**
 * "container":{"threadPool":[{"maxConnections":200,"currentThreadsBusy":7,"maxThreads":200,"connectionCount":8,"instance":"Catalina:type=ThreadPool,name=\"http-bio-8080\"","currentThreadCount":10},{"maxConnections":10000,"currentThreadsBusy":0,"maxThreads":2048,"connectionCount":1,"instance":"Catalina:type=ThreadPool,name=\"http-nio-8443\"","currentThreadCount":0}],"serverInfo":"Apache Tomcat/7.0.53"}
 * @param json
 */
function drawContainerChart (json) {
	var container = json.container;
	if ( ! container) {
		LOGE("DrawContainer: Missing container JSON in " + JSON.stringify(json));
		return;
	}
	var serverInfo = container.serverInfo;
	
	if ( !containerChart) {
		configContainer.options.title.text = serverInfo;
		containerChart = new Chart(document.getElementById('containerChart').getContext("2d"), configContainer);
	}
	
	// [{"maxConnections":200,"currentThreadsBusy":7,"maxThreads":200,"connectionCount":8,"instance":"Catalina:type=ThreadPool,name=\"http-bio-8080\"","currentThreadCount":10},
	var threadPool = container.threadPool;

	for ( var i = 0 ; i < threadPool.length ; i++ ) {
		var pool 		= threadPool[i];
		var connections	= pool.connectionCount;
		var threads		= pool.currentThreadCount;
		var busy		= pool.currentThreadsBusy;
		var max			= pool.maxThreads;
		//LOGD(pool.instance + ' Conns=' + connections + ' threads=' + threads + ' busy=' + busy + ' max=' + max);
		
		// Catalina:type=ThreadPool,name=\"http-bio-8080\"
		configContainer.data.labels[i] 				= pool.instance.indexOf("=") != -1 ? pool.instance.split("=")[2] : pool.instance ;	
		configContainer.data.datasets[0].data[i]	= connections;
		configContainer.data.datasets[1].data[i]	= threads;
		configContainer.data.datasets[2].data[i]	= busy;
		configContainer.data.datasets[3].data[i]	= max;
	}
	
	containerChart.update();
}

/**
 * Container Global Request Processor chart
 * @param json "container":{ "requestProcessor": [{"bytesSent": 0, "bytesReceived": 0,"processingTime": 0, "maxTime": 0, "errorCount": 0, "requestCount": 0, "instance": "Catalina:type=GlobalRequestProcessor,name=\"http-nio-8443\"" }...], "threadPool":[...],"serverInfo":"Apache Tomcat/7.0.53"}
 */

function drawGlobalRequestProcessorChart (json) {
	var container = json.container;
	if ( ! container) {
		LOGE("DrawContainer: Missing container JSON in " + JSON.stringify(json));
		return;
	}
	// "requestProcessor": [{"bytesSent": 0, "bytesReceived": 0,"processingTime": 0, "maxTime": 0, "errorCount": 0, "requestCount": 0, "instance": "Catalina:type=GlobalRequestProcessor,name=\"http-nio-8443\"" }...]
	var requestProcessor = container.requestProcessor;

	for ( var i = 0 ; i < requestProcessor.length ; i++ ) {
		var processor 		= requestProcessor[i];
		var bytesSent		= processor.bytesSent;
		var bytesReceived	= processor.bytesReceived;
		var processingTime	= processor.processingTime;		// MS
		var requestCount	= processor.requestCount;
		var errorCount		= processor.errorCount;
		
		var rps 			= requestCount * 1000 / processingTime;
		var throughput		= (bytesSent + bytesReceived) * 1000 / (processingTime * 1024); // KB/s

		configCGRP.data.labels[i] 			= processor.instance.indexOf("=") != -1 ? processor.instance.split("=")[2] : processor.instance ;	
		configCGRP.data.datasets[0].data[i]	= rps.toFixed(2);
		configCGRP.data.datasets[1].data[i]	= throughput.toFixed(2);
		configCGRP.data.datasets[2].data[i]	= errorCount;
	}
	requestChart.update();
}

/**
 * DRAW ALL CHARTS FROM JSON:
 * {
	    "usedMem": 54131448,
	    "agentManagerFragment": "<h2>Agents</h2><table>\n<tr><th>Name</th><th>Status</th><th>Sessions</th></tr>\n</table>",
	    "avgOutLatency": 0,
	    "status": 200,
	    "SystemCpuLoad": "0.04581372190268807",
	    "chartData0": "[['Skills', 'CHAT', 'VOICE' ],['OM Work Inbound',0,0],['SMS Inbound',0,0]]",
	    "chartData1": "[['Skills', 'CHAT', 'VOICE' ],['OM Work Inbound',0,0],['SMS Inbound',0,0]]",
	    "avgInLatency": 0,
	    "totalMem": 117411840,
	    "chartOptions1": "{ animation: {duration: 1000, easing: 'out'} , title: 'LivePerson Throughput', hAxis: {title: 'Skill Names', titleTextStyle: {color: 'green'}}}",
	    "chartOptions0": "{ animation: {duration: 1000, easing: 'out'} , title: 'LivePerson Free Slots (Available Agents)', hAxis: {title: 'Skill Names', titleTextStyle: {color: 'green'}}}",
	    "chartCount": 2,
	    "intakeVolume": 0,
	    "fragment0": "LP HTML",
	    "ProcessCpuLoad": "0.0026520683563266423",
	    "totalEventSize": 0,
	    "fragmentCount": 1
	}
 * Memory values in bytes!
 */
function drawCharts(json) {
	
	// {"status": 200, "intakeVolume": 100, "totalEventSize": 123, "usedMem": 123, "totalMem": 120, "maxMem": 123}
	var mm = json.heapMax;		// max (bytes)
	var tm = json.heapTotal; 	// total mem (bytes)
	var fm = json.heapFree; 	//tm - um; 		// free mem (bytes)
	var um = tm - fm;			// used (bytes) NOT in OS METRICS

	//LOGD("Volume: " + iv + " Total Mem: " + tm + " Free:" + fm + " Used:" + um + " Max:" + mm);
	//LOGD("Total Mem: " + tm + " Free:" + fm  + " Max:" + mm + " Cpu:" + json.SystemCpuLoad);
	
	// Draw CPU Load (area)
	var d = new Date();
	var n = d.toLocaleTimeString();
	
	// load vals range from 0.0 - 1.0 (% vals)
	drawCPUChart(json.SystemCpuLoad * 100, json.ProcessCpuLoad * 100, n);
	
	// memory : total, used, max (MB) / time
	drawMemChart(toMB(tm), toMB(um), toMB(mm), n);
	
	// thread count
	drawThreadCountChart(json.daemonThreadCount, json.peakThreadCount, json.threadCount, n);
	
	// cpuAvgGauge
	cpuAvgGauge.refresh(json.SystemCpuLoad * 100);
	
	// container...
	drawContainerChart (json);
	drawGlobalRequestProcessorChart (json);
}


/* CPU load dimensions*/
function drawCPUChart(cpu1, cpu2, time) {
  	//LOGD("Draw Cpu: Sys: " + cpu1 + " proc:" + cpu2 + " time:" + time);
  	
  	configCpu.data.labels.shift();
  	configCpu.data.labels.push(time);
  	
  	// Sys cpu
  	configCpu.data.datasets[0].data.shift();
  	configCpu.data.datasets[0].data.push(cpu1);

  	// proc cpu
  	configCpu.data.datasets[1].data.shift();
  	configCpu.data.datasets[1].data.push(cpu2);
  	
  	cpuAreaChart.update();
}

  
function drawMemChart(total, free, max, time) {
  	
  	configMem.data.labels.shift();
  	configMem.data.labels.push(time);
  	
  	// total
  	configMem.data.datasets[0].data.shift();
  	configMem.data.datasets[0].data.push(total);

  	// free
  	configMem.data.datasets[1].data.shift();
  	configMem.data.datasets[1].data.push(free);

  	// Max
  	configMem.data.datasets[2].data.shift();
  	configMem.data.datasets[2].data.push(max);
  	
  	memChart.update();
}

function drawThreadCountChart(daemon, peak, live, time) {
 	configTr.data.labels.shift();
  	configTr.data.labels.push(time);
  	
  	// daemon
  	configTr.data.datasets[0].data.shift();
  	configTr.data.datasets[0].data.push(daemon);

  	// peak
  	configTr.data.datasets[1].data.shift();
  	configTr.data.datasets[1].data.push(peak);

  	// live
  	configTr.data.datasets[2].data.shift();
  	configTr.data.datasets[2].data.push(live);

  	//LOGD("Draw Threads daemon:" + daemon + " Peak:" + peak);
  	threadChart.update();
 	
}
