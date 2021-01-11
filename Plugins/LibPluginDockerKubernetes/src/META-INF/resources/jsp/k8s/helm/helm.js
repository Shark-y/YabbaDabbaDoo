/**
 * JS functions for helm.jsp
 */

// Charts data table
function initializeChartsTable() {
	TBL1 = $('#tblCharts').DataTable( getChartsTableConfig(getChartsTableAjaxDeferLoadConfig()) );
}

// used by manage.jsp
function getChartsTableAjaxDefaultConfig() {
	return { 
		"url": url + '&op=HelmList',
		"dataSrc": function ( json ) {
			// {"message":"A node name is required.","Releases":[],"status":500}
			// {"Next":"","Releases":[{"Name":"alliterating-toad","Revision":1,"Updated":"Wed Apr 10 18:28:38 2019","Status":"DEPLOYED","Chart":"nginx-ingress-1.4.0","AppVersion":"0.23.0","Namespace":"default"},{"Name":"killjoy-buffoon","Revision":1,"Updated":"Wed Apr 10 13:51:28 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"},{"Name":"singing-magpie","Revision":1,"Updated":"Wed Apr 10 13:40:11 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"}]} 
			LOGD('JSON=' + JSON.stringify(json));
			if (json.status >= 400) {
				var reason = json.message;
				growl('Helm list: ' + reason, 'danger');	
			}
			return json.Releases;
		}
	}	
}

// invoked by helm.jsp (for deferred loading)
function getChartsTableAjaxDeferLoadConfig() {
	return { 
		//"url": url + '&op=HelmList',
		"dataSrc": function ( json ) {
			// {"message":"A node name is required.","Releases":[],"status":500}
			// {"Next":"","Releases":[{"Name":"alliterating-toad","Revision":1,"Updated":"Wed Apr 10 18:28:38 2019","Status":"DEPLOYED","Chart":"nginx-ingress-1.4.0","AppVersion":"0.23.0","Namespace":"default"},{"Name":"killjoy-buffoon","Revision":1,"Updated":"Wed Apr 10 13:51:28 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"},{"Name":"singing-magpie","Revision":1,"Updated":"Wed Apr 10 13:40:11 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"}]} 
			LOGD('JSON=' + JSON.stringify(json));
			if (json.status >= 400) {
				var reason = json.message;
				growl('Helm list: ' + reason, 'danger');	
			}
			return json.Releases;
		}
	}
}

// get charts tbl default config
function getChartsTableConfig (ajax) {
	// Charts
	var config = {
		"deferLoading": 0, 
		"ajax": ajax,
		"columns": [
				{ "data": "name" }, 
				{ "data": "chart" },
				{ "data": "app_version" },
				{ "data": "updated" },
				{ "data": "namespace" },
				{ "data": "status" }
		],
		"columnDefs": [
			// colorize status
			{ "targets": 5, 
				"render": function ( data, type, full, meta ) {
					var color = data == 'DEPLOYED' ? 'green' : data == 'DELETED' ? 'red' : 'black';
					return '<font color="' + color +'">' + data + '</font>';
			}},
			// Actions col(4)
			{ "targets": 6, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					return '<div class="dropdown uk-button-dropdown" data-uk-dropdown><a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="material-icons">more_vert</span></a>'
						+ '<div class="uk-dropdown">'
						+ '<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">'
						+ ' <li><a href="#" onclick="return helmDelete(\'' + full.name + '\',\'' + full.app_version + '\',\'' + full.namespace  + '\', function () { $(\'#btnRefreshCharts\').click(); $(\'#btn_refresh_tblCharts\').click(); } )">Delete</a>'
						+ '</ul>'
						+ '</div>'
						+ '</div>';
			}} 
		],
		stateSave: true,
		paging: false,
		searching: false
	};
	return config;
}

/**
 * @param Url http://localhost:8080/WebConsole/K8S?node=PRD201&op=ListServices&_=1609099802530
 * @param TBL1 Data table
 * @param apiUrl API server used to open apps https://192.168.40.84:6443/ 
 */
function wrapApps (Url, TBL1, apiUrl) {
	LOGD('Match Hem/Services ' + Url + ' ApiUrl:' + apiUrl);
	var posting = $.get (Url);
	
	posting.done(function( json ) {
		//LOGD('All Services: ' + JSON.stringify(json));

		// {data: {}, status: 200, mesage: xxx}
		if ( json.status != 200) {
			return;
		}
		var items = json.data.items;
		
		TBL1.rows().every( function (rowIdx, tableLoop, rowLoop) {
			// {"app_version":"1","name":"c1convs-1585927083","namespace":"default","updated":"2020-04-03 11:18:03.929001351 -0400 EDT","chart":"c1convs-20200326","revision":"1","status":"deployed"}
			var d 		= this.data();
			var name	= d.name;		// helm name (c1convs-1585927083)
			
			//LOGD( rowIdx + ' ' + JSON.stringify(d) );
			
			for ( var i = 0 ; i < items.length ; i++ ) {
				var servName	= items[i].metadata.name;
				var spec 		= items[i].spec;
				var type		= spec.type; 	// NodePort, LoadBalancer
				
				if ( type != 'NodePort' && type != 'LoadBalancer' ) {
					continue;
				}
				// [{"protocol":"TCP","port":4369,"name":"epmd","targetPort":"epmd","nodePort":31590},{"protocol":"TCP","port":5672,"name":"amqp","targetPort":"amqp","nodePort":30495},{"protocol":"TCP","port":25672,"name":"dist","targetPort":"dist","nodePort":32441},{"protocol":"TCP","port":15672,"name":"stats","targetPort":"stats","nodePort":30751}]
				var ports		= spec.ports	
				
				// match helm name (kibana-1586100279,nginx-ingress-1585514119) with service name (kibana-1586100279,nginx-ingress-1585514119-controller)
				//LOGD('Compare ' + servName + ' == ' + name);
				if ( servName.indexOf(name) != -1 ) {
					var href 	= parseHref (apiUrl);
					var url 	= buildUrl (href.hostname , ports, ['http', 'stats']);
					//LOGD('Got match ' + name + ' row: ' + rowIdx + ' stype: ' + type + ' Ports: ' + JSON.stringify(ports));
					//LOGD('Got match ' + name + ' row: ' + rowIdx + ' stype: ' + type + ' Url: ' + url);
					if ( url) {
						TBL1.cell(rowIdx, 0).data('<a target="_blank" href="' + url + '">' + name + '</a>');
					}
				}
			}
		});
		
		TBL1.draw();
	});
}

// https://stackoverflow.com/questions/736513/how-do-i-parse-a-url-into-hostname-and-path-in-javascript
function parseHref (href) {
	var parser 	= document.createElement('a');
	parser.href = href;
	return {'proto': parser.protocol, 'host': parser.host, 'hostname': parser.hostname};
}

/**
 * Try to build a url from s service ports array
 * [{"protocol":"TCP","port":80,"name":"http","targetPort":"http","nodePort":32212},{"protocol":"TCP","port":443,"name":"https","targetPort":"https","nodePort":32543}]
 * [{"protocol":"TCP","port":4369,"name":"epmd","targetPort":"epmd","nodePort":31590},{"protocol":"TCP","port":5672,"name":"amqp","targetPort":"amqp","nodePort":30495},{"protocol":"TCP","port":25672,"name":"dist","targetPort":"dist","nodePort":32441},{"protocol":"TCP","port":15672,"name":"stats","targetPort":"stats","nodePort":30751}]
 */
function buildUrl (node, ports, names) {
	var proto;
	var port;
	for ( var i = 0 ; i < ports.length ; i++ ) {
		var oPort 	= ports[i];
		for ( var j = 0 ; j < names.length ; j++ ) {
			if (oPort.name && oPort.name.startsWith(names[j])) {
				proto 	= oPort.name.startsWith('http') ? oPort.name : 'http';
				port	= oPort.nodePort;
				break;
			}
		}
	}
	return proto ? (proto + '://' + node + ':' + port) : null;
}

