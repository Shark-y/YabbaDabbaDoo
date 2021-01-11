/**
 * Manage - Deployments Logic 
 */

/**
 * Init Deployments data-table
 */
function initializeDeploymentsTable() {
	// Deployments
	var config = {
		"ajax": { 
			"url": url + '&op=ListDeployments',
			"dataSrc": /*"data.items" */function ( json ) {
				// ERROR {"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"nodes is forbidden: User \\\"system:serviceaccount:default:default\\\" cannot list resource \\\"nodes\\\" in API group \\\"\\\" at the cluster scope\",\"reason\":\"Forbidden\",\"details\":{\"kind\":\"nodes\"},\"code\":403}\n","status":403,"data":{"items":[]}}
				// ERROR {"message":"{\n  \"paths\": [\n    \"/apis\",\n    \"/apis/\",\n    \"/apis/apiextensions.k8s.io\",\n    \"/apis/apiextensions.k8s.io/v1beta1\",\n    \"/healthz\",\n    \"/healthz/etcd\",\n    \"/healthz/log\",\n    \"/healthz/ping\",\n    \"/healthz/poststarthook/crd-informer-synced\",\n    \"/healthz/poststarthook/generic-apiserver-start-informers\",\n    \"/healthz/poststarthook/start-apiextensions-controllers\",\n    \"/healthz/poststarthook/start-apiextensions-informers\",\n    \"/metrics\",\n    \"/openapi/v2\",\n    \"/version\"\n  ]\n}","status":404,"data":{"items":[]}}
				if (json.status >= 400) {
					var reason 	= typeof(json.message) === 'object' ? JSON.parse(json.message) : { message: json.message};
					growl('Deployments: ' + (reason.message ? reason.message : JSON.stringify(reason) ), 'danger');	
				}
				return json.data.items;
			}
		},
		"columns": [
				{ "data": "metadata.name" }, 
				{ "data": "metadata.namespace" },
				{ "data": "metadata.creationTimestamp" },
				{ "data": "spec.replicas" }
				//{ "data": "spec.ports" },
		],
		"columnDefs": [ 
		    // replicas (3)
   			{ "targets": 3,
   				"width": "30%",
				"render": function ( data, type, full, meta ) {
					var name	= full.metadata.name;
					var ns		= full.metadata.namespace;
					var id 		= "ts-replicas-" + name;
					return '<input onchange="deployReplicaChange(this,\'' + name + '\',\'' + ns + '\')" class="form-control md-input" id="' + id + '" type="text" value="' + data + '"><script>$("#' + id + '").TouchSpin()</script>';
			}} ,
		               
			// Actions col(4)
			{ "targets": 4, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					DEPLOY_FULL[full.metadata.name] = full;
					return '<div class="dropdown uk-button-dropdown" data-uk-dropdown><a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="material-icons">more_vert</span></a>'
						+ '<div class="uk-dropdown">'
						+ '<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">'
						+ '<li><a href="#" onclick="return hash2DDetails(\'' + full.metadata.name + '\',\'Status\', DEPLOY_FULL,\'status\')">Status</a>'
						+ '<li><a href="#" onclick="return hash2DDetails(\'' + full.metadata.name + '\',\'Meta\', DEPLOY_FULL,\'metadata\')">Meta-Data</a>'
						+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Spec\', DEPLOY_FULL,\'spec\',\'template\')">Spec Template</a>'
						+ '</ul>'
						+ '</div>'
						+ '</div>';
			}} 
		],
		stateSave: true,
		paging: true,
		searching: true
	};
	//TBL4 = $('#tblDeployments').DataTable( config );
	init_tblDeployments(config);
}

function deployReplicaChange(obj, name, ns) {
	var n 		= obj.value;
	var payload = {	"spec": { "replicas": parseInt(n) } };
	
	LOGD('Change replicas: ' + n + ' for ' + name + ' ns:' + ns + ' Payload:' + JSON.stringify(payload));
	patchDeployment(name, ns, payload);
}

function patchDeployment(name, ns, data) {
	var installUrl 	= url + '&op=PatchDeployment&deployment=' + name + '&namespace=' + ns ; // + '&debug=true' ;
	var postData	= JSON.stringify(data);
	
	LOGD('patch deployment @ ' + installUrl + ' data:' + postData);

	var posting = $.ajax( { url: installUrl, type:"POST", data: postData , contentType:"application/json; charset=utf-8", dataType:"json" } );
	
	// get results 
	posting.done(function( data ) {
		// ERROR: {"data":{"items":[]},"message":"Stream closed","status":503}
		// ERROR: {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"415: Unsupported Media Type\",\"reason\":\"UnsupportedMediaType\",\"details\":{},\"code\":415}\n","status":415}
		//LOGD("Patch Deployment Resp: " + JSON.stringify(data));
		
		if ( data.status >= 415) {
			var json = JSON.parse(data.message);
			growl(json.message, 'danger');
			return;
		}
		else if ( data.status >= 400) {
			growl('Failed with ' + data.message, 'danger');
			return;
		}
		growl ('Patched ' + name);
	});
	

}
