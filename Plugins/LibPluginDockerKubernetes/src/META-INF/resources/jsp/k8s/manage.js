/**
 * 
 */

var NODE_FULL 		= {};
var POD_FULL  		= {};
var SERVICE_FULL  	= {};
var DEPLOY_FULL  	= {};

/**
 * Initialize page data tables: containers, images, networks, volumes
 * @param node Node name
 */
function initializeDataTables (node, user, identity) {
	user 		= user 		|| '';
	identity	= identity	|| '';

	// Apps
	init_tblCharts (getChartsTableConfig(getChartsTableAjaxDefaultConfig()));

	// Nodes
	var cfgNodes = {
		"ajax": { 
			"url": url + '&op=ListNodes',
			"dataSrc": /*"data.items"*/ function ( json ) {
				// {"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"nodes is forbidden: User \\\"system:serviceaccount:default:default\\\" cannot list resource \\\"nodes\\\" in API group \\\"\\\" at the cluster scope\",\"reason\":\"Forbidden\",\"details\":{\"kind\":\"nodes\"},\"code\":403}\n","status":403,"data":{"items":[]}}
				if (json.status >= 400) {
					var reason 		= typeof(json.message) === 'object' ? JSON.parse(json.message) : { message: json.message};
					growl('Nodes: ' + reason.message, 'danger');	
				}
				//LOGD('Nodes resp: ' +JSON.stringify(json));
				return json.data.items;
			}
		},
		"columns": [
				{ "data": "metadata.name" }, // Names[, ]
				{ "data": "metadata.uid" },
				{ "data": "status.images" },
				{ "data": "status.conditions" }
		],
		"columnDefs": [ 
			// details
			{ "targets": [2],
				"render": function ( data, type, full, meta ) {
					//LOGD('data=' + JSON.stringify(data));
					//NODE_STATUS_IMAGES[full.metadata.name] 	= data;
					// Add status label <span class="label label-success">...</span>
					var info 		= full.status.nodeInfo;
					
					// type = PIDPressure, DiskPressure, Ready, MemoryPressure - Need 'status'
					// [ reason: "NodeStatusUnknown, lastHeartbeatTime: TIME, type: "MemoryPressure", message	"Kubelet stopped posting node status.", status	"Unknown",...]
					var conditions	= full.status.conditions;
					var cls			= 'label-success uk-badge uk-badge-success';
					var status		= conditions[conditions.length - 1].status;
					
					// build a status label
					for ( var i = 0 ; i < conditions.length; i++) {
						var condition = conditions[i];
						if (condition.type == 'Ready') {
							status = condition.status;
						}
					}
					if ( status != 'True') {
						cls = 'label-danger uk-badge uk-badge-danger';
					}
					else {
						status = 'Ready';
					}
					return info.osImage + ' ' + info.architecture + ' Kube: ' + info.kubeProxyVersion + " " + info.containerRuntimeVersion
						+ ' &nbsp;<span class="label ' + cls +'">' + status + '</span>'; 
						; //+ ' &nbsp;&nbsp;<a href="#" onclick="return nodeDetails(\'' + full.metadata.name + '\', NODE_STATUS_IMAGES,\'Images\')">More</a>';
				}
			},
			// status
			{ "targets": [3],
				"createdCell": function (td, cellData, rowData, row, col) {
					$(td).jsonViewer(cellData);
				}
			},
			// Actions col(4)
			{ "targets": 4, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					NODE_FULL[full.metadata.name] = full;
					return '<div class="dropdown uk-button-dropdown" data-uk-dropdown><a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="material-icons">more_vert</span></a>'
						+ '<div class="uk-dropdown">'
						+ '<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">'
						+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Images\', NODE_FULL,\'status\',\'images\',true)">Images</a></li>'
						+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Addresses\',NODE_FULL,\'status\',\'addresses\')">Addresses</a></li>'
						+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Conditions\',NODE_FULL,\'status\',\'conditions\')">Status Conditions</a></li>'
						+ '<li class="divider"></li>'
						+ "<li><a href=\"#\" onclick=\"window.open('../ssh/ssh.jsp?host=" + full.metadata.name +  "&user=" + user + "&identity=" + identity + "')\">Terminal</a></li>"
						+ '</ul>'
						+ '</div>'
						+ '</div>';
			}} 
		],
		stateSave: true,
		paging: false,
		searching: false
	};

	init_tblNodes (cfgNodes);
	
	// Pods
	var cfgPods = {
		"ajax": { 
			"url": url + '&op=ListAllPods',
			"dataSrc": /*"data.items" */function ( json ) {
				// {"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"nodes is forbidden: User \\\"system:serviceaccount:default:default\\\" cannot list resource \\\"nodes\\\" in API group \\\"\\\" at the cluster scope\",\"reason\":\"Forbidden\",\"details\":{\"kind\":\"nodes\"},\"code\":403}\n","status":403,"data":{"items":[]}}
				if (json.status >= 400) {
					var reason 		= typeof(json.message) === 'object' ? JSON.parse(json.message) : { message: json.message};
					growl('Pods: ' + reason.message, 'danger');	
				}
				return json.data.items;
			}
		},
		"columns": [
				{ "data": "metadata.name" }, 
				{ "data": "metadata.uid" },
				{ "data": "metadata.namespace" },
				{ "data": "spec.nodeName", "defaultContent": "<i>Not set</i>" },
				{ "data": "status.phase" }
		],
		"columnDefs": [ 
			// status JSON - Note: data == status.phase
			{ "targets": [4],
				"render": function ( data, type, full, meta ) {
					var status 	= full.status;	
					var spec 	= full.spec;
					var name	= spec.serviceAccountName ? ' &nbsp;Service Account: ' + spec.serviceAccountName + ' ' : '';
					
					return  '<span class="label uk-badge ' + (data == 'Running' ? 'label-success uk-badge-success' :  data == 'Succeeded' ? 'label-info' : 'label-danger uk-badge-danger') + '">' + data + '</span>'
						+ name + ( status.podIP ? ' <i>IP: ' + status.podIP + '</i> ' : '' )
						//+ ' Node: ' + spec.nodeName
						; //+ ( data == 'Running' ? '<a href="#" onclick="return getPodLogs(\'' + full.metadata.name + '\',\'' + full.metadata.namespace + '\')">Logs</a>' : '' );
						
				}
			},
			// Actions col(5)
			{ "targets": 5, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					var phase 	= full.status.phase;	
					
					POD_FULL[full.metadata.name] = full;
					return '<div class="dropdown uk-button-dropdown" data-uk-dropdown><a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="material-icons">more_vert</span></a>'
						+ '<div class="uk-dropdown">'
						+ '<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">'
						+ '<li><a href="#" onclick="return hash2DDetails(\'' + full.metadata.name + '\',\'Status\', POD_FULL,\'status\',false,true)">Status</a>'
						+ '<li><a href="#" onclick="return hash2DDetails(\'' + full.metadata.name + '\',\'Spec\', POD_FULL,\'spec\')">Spec</a>'
						+ '<li><a href="#" onclick="return getPodLogs(\'' + full.metadata.name + '\',\'' + full.metadata.namespace + '\')">Logs</a>'
						+ '<li><a href="#" onclick="return getPodTerm(\'' + full.metadata.name + '\',\'' + full.metadata.namespace + '\', POD_FULL)">Terminal</a>'
						+ '</ul>'
						+ '</div>'
						+ '</div>';
			}} 
		],
		stateSave: true,
		paging: false
		/* searching: false */
	};
	
	init_tblPods(cfgPods);
	
	initializeServicesTable();
	initializeDeploymentsTable();
	initializeSecretsTable();
	initializeEventsTable();
	initializeStorageTable();
	
	// toggle visibility
	$('a.toggle-vis').on( 'click', function (e) {
		e.preventDefault();

		// Get the column API object
		var table 	= eval( $(this).attr('data-table') );
		var column 	= table.column( $(this).attr('data-column') );

		// Toggle the visibility
		column.visible( ! column.visible() );
	} );
	
	// Hide Ids (too long)
	tblNodes/*TBL1*/.column(1).visible (false);
	tblNodes/*TBL1*/.column(3).visible (false);
	tblPods/*TBL2*/.column(1).visible (false);
	
}	

/**
 * Services data-table
 */
function initializeServicesTable() {
	// Services
	config = {
		"ajax": { 
			"url": url + '&op=ListServices',
			"dataSrc": /*"data.items" */function ( json ) {
				// {"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"nodes is forbidden: User \\\"system:serviceaccount:default:default\\\" cannot list resource \\\"nodes\\\" in API group \\\"\\\" at the cluster scope\",\"reason\":\"Forbidden\",\"details\":{\"kind\":\"nodes\"},\"code\":403}\n","status":403,"data":{"items":[]}}
				if (json.status >= 400) {
					var reason 	= typeof(json.message) === 'object' ? JSON.parse(json.message) : { message: json.message};
					growl('Services: ' + reason.message, 'danger');	
				}
				return json.data.items;
			}
		},
		"columns": [
				{ "data": "metadata.name" }, 
				{ "data": "metadata.namespace" },
				{ "data": "metadata.creationTimestamp" },
				{ "data": "spec.clusterIP" }
				//{ "data": "spec.ports" },
		],
		"columnDefs": [ 
			// ports JSON		
			{ "targets": [3],
				//"createdCell": function (td, cellData, rowData, row, col) {
				"render": function ( data, type, full, meta ) {
					//$(td).jsonViewer(cellData);
					var spec 	= full.spec;
					var ports	= spec.ports;
					var last	= ports[ports.length - 1];
					var proto	= last.name.indexOf('http') != -1 ? last.name : 'http';
					var type 	= spec.type == 'NodePort' || spec.type == 'LoadBalancer' 
						? '<a target="_blank" href="' + proto + '://' + nodeIP + ':' + last.nodePort + '">' + spec.type + '</a>' 
						: spec.type;
					
					return type + '/' + spec.clusterIP;
				}
			},
			// Actions col(4)
			{ "targets": 4, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					SERVICE_FULL[full.metadata.name] = full;
					return '<div class="dropdown uk-button-dropdown" data-uk-dropdown><a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="material-icons">more_vert</span></a>'
						+ '<div class="uk-dropdown">'
						+ '<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">'
						+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Labels\', SERVICE_FULL,\'metadata\',\'labels\')">Labels</a>'
						+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Ports\', SERVICE_FULL,\'spec\',\'ports\')">Ports</a>'
						+ '</ul>'
						+ '</div>'
						+ '</div>';
			}} 
		],
		stateSave: true,
		paging: false 
		// searching: false
	};
	//TBL3 = $('#tblServices').DataTable(config);
	init_tblServices (config);
}

