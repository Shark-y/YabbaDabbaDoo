/**
 * Functions used by dash-view.jsp
 */

/**
  * View/Open dashboard
  * @param name dash name/title
  * @param listener Queue/ listener name
  * @param key Group By field name
  */
 function dash_view (name, listener, key) {
 	window.location = 'dash-view.jsp?name=' + listener + '&key=' + encodeURIComponent(key) + '&title=' + escape(name) ;
 }

/**
 * Status message functions 
 */
 function clearStatusMessage() {
 	document.getElementById('message').innerHTML = '';
 }

 function setStatusMessage(text, color) {
 	document.getElementById('message').innerHTML = '<font color=' + color + ' size=5>' + text + '</font>';
 }

 function setOKStatus(text) {
 	setStatusMessage(text, "blue");
 }

 function setErrorStatus(text) {
 	setStatusMessage(text, "red");
 }

 /**
  * Format Panel HTML
  */
 function formatPanel (panelId, valueId, labelId) {
 	var html = "" //"<div class=\"col-md-3\">"
 		+ "<div id=\"" + panelId + "\" class=\"panel\">"
 		+ "<div class=\"panel-heading\">"
 		+ "<div class=\"row\">"
 		+ "<div class=\"text-center\">"
 		+ "<div class=\"huge\" id=\"" + valueId + "\">0</div>"
 		+ "<div id=\"" + labelId + "\">Empty</div>"
 		+ "</div></div></div></div>";
 	return html;
 }

 /**
  * Wrap all metrics of type (NUMBER) widget (AREA_CHART) in a single area chart!
  */
 function formatAreaChart (chartId) {
	 // MORRIS var html = '<div class="chartWrapper" id="' + chartId + '"></div>';
	 // Chart.js
	 var html = '<div class="chartWrapper" id="' + chartId + '"><canvas id="canvas-' + chartId + '"></canvas></div>'; 
	 return html;
 }

 /**
  * Get the alert color for a given metric.
  * @param alerts JSON alert array: [{"weight":0,"level":0,"color":"FFFFFF"},{"weight":0,"level":50,"color":"77FFD4"}]
  * @param value  Metric value
  * @return HTML background color for this value/alert as RRGGBB
  */
 function getAlertColor (alerts, value) {
 	var color = '0b62a4'; 	// Default HTML BG color (blue)
 	 
 	if ( typeof (alerts) == 'undefined') 
 		return color;
 	
 	for ( var i = 0 ; i < alerts.length ; i++) {
 		// {"weight":0,"level":0,"color":"FFFFFF"}
 		var alert = alerts[i]; 
 		if ( value > alert.level ) {
 			color = alert.color;
 		}
 	}
 	return color;
 }

 /**
  * Convert a JSON alert array to a Just gauge sector JSON array
  * @param label A label used to debug such as metric name or description.
  * @param alerts Threshold alert array: [{"weight":0,"level":0,"color":"FFFFFF"},{"weight":0,"level":50,"color":"77FFD4"}, ...]
  * @param max The max value of the gauge range: [0..max]
  * @returns Just gauge sector array: [ {lo: 0, hi: 30, color: "#ffffff"}, {lo: 30, hi: 70, color: "#FF0000"}, ...]
  */
 function alertsToGaugeSectors (label, alerts, max) {
	max = max || 100;
 	if ( alerts == null || typeof(alerts) == 'undefined' || !Array.isArray(alerts) ) {
 		//LOGW('Init Gauge sectors for ' + label + ' alerts:' + alerts);
 		// default
 		return [];
 		//return [{lo: 0, hi: 30, color: "#00ff00"}, {lo: 30, hi: 70, color: "#FF0000"}, {lo: 30, hi: 70, color: "#0000FF"}];
 	}
 	var sectors = [];
 	for ( var i = 0 ; i < alerts.length ; i++) {
 		// {"weight":0,"level":0,"color":"FFFFFF"}
 		var alert = alerts[i];
 		
 		// {lo: 0, hi: 30, color: "#00ff00"}
 		var high = (i + 1) < alerts.length ? alerts[i + 1].level : max; // 5/10/2019 100;
 		sectors.push ({ lo: alert.level, hi: high, color: '#' + alert.color });
 	}
 	//LOGD('Init Gauge sectors for ' + label + ': ' + JSON.stringify(sectors));
 	return sectors;
 }

 /**
  * Initialize a marquee HTML
  * @param mId Marquee id.
  * @returns {String}
  */
 function formatMarquee (mId) {
	 var html = '<div class="marquee" id="' + mId + '">&nbsp;</div>'; 
	 return html;
 }

 /**
  * Update a marquee metric {SPAN}
  * @param id metric id
  * @param text Text to display.
  * @param style CSS style. For example: 'background-color: yellow;color: black;font-size:30px;'
  * @returns The updated DOM {SPAN}
  */
 function marqueeUpdateMetric(id, text, style) {
	var span	= getElem(id);
	var text 	= document.createTextNode(text); 
	if ( !span ) {
		LOGE("MARQUEE-UPDATE: Cannot find metric with id " + id);
		return;
	}
	span.removeChild(span.childNodes[0]);
	span.appendChild(text);             // Append the text to <span>
	span.style.cssText = style; 		//'background-color: yellow;color: black;font-size:30px;';
	return span;
 }

 /**
  * Add a metric to a marquee
  * @param id metric id
  * @param text Text to display.
  * @param style CSS style. For example: 'background-color: yellow;color: black;font-size:30px;'
  * @returns A new DOM {SPAN} decribing the metric to be inserted into the marquee {DIV}
  */
 function marqueeCreateMetric(id, text, style) {
	var span	= document.createElement("SPAN");
	var text 	= document.createTextNode(text); 	// Create a text node
	span.appendChild(text);                         // Append the text to <span>
	span.style.cssText = style; 
	span.id = id; 
	return span;
 }

 /**
  * Get the array of metrics from a dashboard by metric type.
  * @param dash Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param type Desired metric type: ARAE_CHART, GAUGE, PANEL or MARQUEE
  * @returns Desired metrics JSON array for the given type: [{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},...]
  */
 function  dashGetMetricsbyType (dash, type) {
 	var metrics = dash.metrics;
 	var desired = [];
 	for ( var i = 0 ; i < metrics.length ; i++) {
 		var metric = metrics[i];
 		if ( metric.widget == type) {
 			desired.push (metric)
 		}
 	}
 	return desired;
 }


 /**
  * Format options for a marquee : Add metrics from the dash-board with deafuls styles.
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param marqueeId HTML ID of the marquee.
  */
 /*
 function marqueeFormatOptions (dashboard, marqueeId) {
 	var marquee = getElem (marqueeId);
 	if ( ! marquee) {
 		LOGE('MARQUEE-FORMAT: Cannot find marquee with Id: ' + marqueeId);
 		return;
 	}
 	var metrics = dashGetMetricsbyType(dashboard, 'MARQUEE');
 	for ( var i = 0 ; i < metrics.length ; i++) {
 		var metric 	= metrics[i];
 		var id 		= marqueeId + '-' + metric.name; // MARQUEEID-METRICNAME
 		var text	= metric.name + ' ';
 		var style 	= '';
 		var span  	= marqueeCreateMetric(id, text , style);

 		marquee.appendChild(span);
 	}
 } */

 /**
  * Update marquee
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param marqueeId Id of the marquee.
  * @param batch JSON object of (metric, value) pairs: {"VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7",...}
  * @param thresholds Map of all thresholds indexed by metric@data-source: THRESHOLDS['ABNCALLS@CVDN Table'] = [{"weight":0,"level":0,"color":"D9FFE0"}, ...]
  * @param dataSource Data source name, used to get thresholds for a given metric.
  */
 /*
 function marqueeDraw (dashboard, marqueeId, batch, thresholds) { 
 	var alerts;		// [{"weight":0,"level":0,"color":"FFFFFF"},{"weight":0,"level":50,"color":"77FFD4"}] OR 'undefined'
 	var color;		// BG
 	var textColor;	// Foreground
 	var value;
 	var marquee = getElem (marqueeId);
 	var metricId;
 	var style;
 	
 	if ( ! marquee) {
 		LOGE('MARQUEE-FORMAT: Cannot find marquee with Id: ' + marqueeId);
 		return;
 	}
	var metrics 	= dashGetMetricsbyType(dashboard, 'MARQUEE');
	var dataSource 	= dashboard.listener;
	
 	for ( var i = 0 ; i < metrics.length ; i++) {
 		var metric 	= metrics[i];						// metric
 		var trId	= metric.name + '@' + dataSource;	// threshold id - METRIC-NAME@DATA_SOURCE
 	 	metricId	= marqueeId + '-' + metric.name; 
	 	value 		= batch[metric.name];
	 	alerts		= thresholds[trId];	
	 	color		= getAlertColor(alerts, value);			// background color
	 	textColor	= color != 'FFFFFF' ? '#ffffff' : '#000000';
	 	style 		= 'background-color: #' + color + ';color: ' + textColor; // white;font-size:30px;'
	 	
	 	LOGD('Marqee Update: Metric: ' + metric.name +' Value: ' + value + ' Alerts: ' + alerts + ' BGColor: ' + color + ' MetricId:' + metricId + ' Style: ' + style);
	 	
	 	marqueeUpdateMetric(metricId, '  ' + metric.name + ':' + value, style);
 	}
 } */

 /**
  * Format options for a marquee : Add metrics from the dash-board with deafuls styles.
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param marqueeId HTML ID of the marquee.
  * @param key Optional display (group-by) key. For example SPLIT1, VDN1000,...
  */
 function marqueeFormatOptions (dashboard, marqueeId, key) {
 	key = key || '';
 	var marquee = getElem (marqueeId);
 	if ( ! marquee) {
 		LOGE('MARQUEE-FORMAT: Cannot find marquee with Id: ' + marqueeId);
 		return;
 	}
 	// add the display key
 	marquee.appendChild(marqueeCreateMetric(key, ' ' + key + ': ', ''));
 	
 	var metrics = dashGetMetricsbyType(dashboard, 'MARQUEE');
 	for ( var i = 0 ; i < metrics.length ; i++) {
 		var metric 	= metrics[i];
 		var id 		= marqueeId + ( key != '' ? '-' + key + '-' : key ) + metric.name; // MARQUEEID-METRICNAME
 		var text	= metric.name + ' ';
 		var style 	= '';
 		var span  	= marqueeCreateMetric(id, text , style);

 		marquee.appendChild(span);
 	}
 }

 /**
  * Update marquee
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param marqueeId Id of the marquee.
  * @param batch JSON object of (metric, value) pairs: {"VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7",...}
  * @param thresholds Map of all thresholds indexed by metric@data-source: THRESHOLDS['ABNCALLS@CVDN Table'] = [{"weight":0,"level":0,"color":"D9FFE0"}, ...]
  * @param key Optional display (group-by) key. For example SPLIT1, VDM1000,...
  */
 function marqueeDraw (dashboard, marqueeId, batch, thresholds, key) { 
 	var alerts;		// [{"weight":0,"level":0,"color":"FFFFFF"},{"weight":0,"level":50,"color":"77FFD4"}] OR 'undefined'
 	var color;		// BG
 	var textColor;	// Foreground
 	var value;
 	var marquee = getElem (marqueeId);
 	var metricId;
 	var style;
 	key = key || '';
 	
 	if ( ! marquee) {
 		LOGE('MARQUEE-FORMAT: Cannot find marquee with Id: ' + marqueeId);
 		return;
 	}
 	var metrics 	= dashGetMetricsbyType(dashboard, 'MARQUEE');
 	var dataSource 	= dashboard.listener;
 	
 	for ( var i = 0 ; i < metrics.length ; i++) {
 		var metric 	= metrics[i];						// metric
 		var trId	= metric.name + '@' + dataSource;	// threshold id - METRIC-NAME@DATA_SOURCE
 	 	metricId	= marqueeId + ( key != '' ? '-' + key + '-' : key ) + metric.name; 
 	 	value 		= batch[metric.name];
 	 	alerts		= thresholds[trId];	
 	 	color		= getAlertColor(alerts, value);			// background color
 	 	textColor	= color != 'FFFFFF' ? '#ffffff' : '#000000';
 	 	style 		= 'background-color: #' + color + ';color: ' + textColor; // white;font-size:30px;'
 	 	
 	 	LOGD('Marqee Update: Metric: ' + metric.name +' Value: ' + value + ' Alerts: ' + alerts + ' BGColor: ' + color + ' MetricId:' + metricId + ' Style: ' + style);
 	 	
 	 	marqueeUpdateMetric(metricId, '  ' + metric.name + ':' + value, style);
 	}
 }

 /**
  * Initialize The Chart.JS Area Bar. Each metric is assigned to a separate data set
  * to manipulate the bar colors
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param chartId The Id of the arae chart.
  */
 function chartJSFormatOptions (dashboard, chartId) {
 	//var labels = [];
 	var dsets 		= [];
 	var metrics 	= dashGetMetricsbyType(dashboard, 'AREA_CHART');
 	
 	for ( var i = 0 ; i < metrics.length ; i++) {
 		var metric 	= metrics[i];
 		//labels.push(metric.description);
 		dsets.push ({ label: metric.description, 
 			backgroundColor: '0000ff' , // deafult bg (blue)
 			data: []
 			});
 	}
 	return {
 		type: 'bar',
 		data:  { 
 			labels: [], 
 		    datasets: dsets 
 		} , 
 		options: {
 			scales: {
 				//display: false, 
                 xAxes: [{
                     //stacked: false,
 					gridLines: {
 						display: false,
 					}
                 }],
                 yAxes: [{
                     ticks: {
                         beginAtZero:true,
                         suggestedMax: 100
                     }
                 }]                
             } 
 		}
 	};
 }

 /**
  * Draw are achart using Chart.js.
  * Note: each metric is assigned to a separate data set so bar colors can be set via THRESHOLDS
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param chartId Id of the chart
  * @param batch JSON obj of metric, value pairs: {"VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7",...}
  */
 function chartJSDrawAreaChart (dashboard, chartId, batch ) { 
 	var i 			= 0;
 	var alerts ;
 	var color;

	var metrics 	= dashGetMetricsbyType(dashboard, 'AREA_CHART');
	var dataSource 	= dashboard.listener;
 	
 	for ( var j = 0 ; j < metrics.length ; j++) {
 		var metric 	= metrics[j];
 		var trId	= metric.name + '@' + dataSource;	// Global threshold: METRIC@DATASOURCE
 		
 		// missing metric value from batch? ignore
 		if ( !batch[metric.name] ) {
 			continue;
 		}

 		// set the value
	 	CHARTS[chartId].data.datasets[i].data[0] = parseInt(batch[metric.name]);
	 	
	 	// set the DS bg color from the THRESHOLDS map (GLOBAL THRESH - METRIC@DATASOURCE)
	 	alerts 	= THRESHOLDS[trId];
	 	
	 	// No global thresh. Try LOCAL: METRIC@DATASOURCE@DASHBOARD
	 	if ( typeof(alerts) == 'undefined') {
	 		alerts 	= THRESHOLDS[ trId + '@' + dashboard.title];
	 	}
	 	
	 	color	= getAlertColor (alerts, parseInt(batch[metric.name]));
	 	
	 	CHARTS[chartId].data.datasets[i++].backgroundColor = '#' + color;
 	}
 	CHARTS[chartId].update();
 }
 
 /**
  * Draw a panel metric values.
  * @param dashboard Dashboard JSON: {"title":"Call Metrics by SPLIT","displayKey":"SPLIT","metrics":[{"description":"Calls Waiting","name":"CALLS_WAITING","type":"NUMBER","widget":"AREA_CHART"},{"description":"Abandoned","name":"ABNCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"ACD Calls","name":"ACDCALLS","type":"NUMBER","widget":"AREA_CHART"},{"description":"Avg. speed of answer","name":"AVG_SPEED_ANS","type":"NUMBER","widget":"PANEL"},{"description":"Agents Available","name":"AVAILABLE","type":"NUMBER","widget":"GAUGE"},{"description":"Agents: Ringing","name":"AGINRING","type":"NUMBER","widget":"PANEL"},{"description":"Agents: ACD","name":"ONACD","type":"NUMBER","widget":"PANEL"}],"listener":"CSPLIT Table","keyRange":"1-3"};
  * @param batch JSON obj of metric, value pairs: {"VDN":"74153","ACDCALLS":"68"	,"ABNCALLS":"7",...}
  * @param key Group By key: SPLIT, VDN, etc.
  */
 function panelDraw ( dashboard, batch, key) {
 	 var value;		// metric value 
 	 var desc;		// metric description
 	 var alerts;	// JSON array [{"weight":0,"level":0,"color":"FFFFFF"}, ...
 	 var color;		// HTML color

 	var metrics 	= dashGetMetricsbyType(dashboard, 'PANEL');
	var dataSource 	= dashboard.listener;
 	
 	for ( var j = 0 ; j < metrics.length ; j++) {
 		var metric 	= metrics[j];
 		var trId	= metric.name + '@' + dataSource;	// Global threshold: METRIC@DATASOURCE
 	 
 		// missing metric value from batch, ignore
 		if ( !batch[metric.name] ) {
 			continue;
 		}
	 	value 	= batch[metric.name];
	 	desc 	= metric.description;
	 	alerts	= THRESHOLDS[trId];
	 	color	= getAlertColor(alerts, value);
	 	
	 	//LOGD("Draw Panels: Metric: " + metric + " Value: " + value + " Alerts: " + alerts + " Color:" + color);
	 	
	 	getElem('val_' + key + '_' + metric.name).innerHTML = value; // set value
	 	getElem('lbl_' + key + '_' + metric.name).innerHTML = desc; // set description
	 	
	 	// set the color
	 	var panel = getElem('panel_' + key + '_' + metric.name);
	 	if ( color != '') {
	 		panel.style['background-color'] = '#'  + color;
	 		panel.style['color'] 			=  color != 'FFFFFF' ? '#ffffff' : '#000000';
	 	}
	 	else {
	 		// default: panel-primary
	 		panel.style['background-color'] = '#337ab7';
	 		panel.style['color'] 			= '#ffffff';
	 	}
 	} 
 }

