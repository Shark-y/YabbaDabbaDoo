//var TBL5;					// Secrets tbl
var SECRETS_FULL  	= {};	// server secrets hash

/**
 * Secrets data-table
 */

function initializeSecretsTable() {
	// Services
	var config = {
		"ajax": { 
			"url": url + '&op=ListSecrets',
			"dataSrc": /*"data.items" */function ( json ) {
				// {"data":{"items":[]},"message":"400 Bad Request","status":400}
				if (json.status >= 400) {
					var reason = json.message;
					growl('Get secrets: ' + reason, 'danger');	
				}
				return json.data.items;
			}
		},
		"columns": [
				{ "data": "metadata.name" }, 
				{ "data": "metadata.namespace" },
				{ "data": "type" },
				{ "data": "metadata.creationTimestamp" },
				//{ "data": "spec.ports" },
		],
		"columnDefs": [ 
 			{ "targets": 0, 
				"render": function ( data, type, full, meta ) {
					var key 	= full.metadata.name;
					var html 	= '<input id="chk-' + key + '" type="checkbox" value="' + key + '"> '; 
					return html +  data;
			}} , 
			// Actions col(5)
			{ "targets": 4, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					SECRETS_FULL[full.metadata.name] = full;
					return '<div class="dropdown uk-button-dropdown" data-uk-dropdown><a class="dropdown-toggle" data-toggle="dropdown" href="#"><span class="material-icons">more_vert</span></a>'
						+ '<div class="uk-dropdown">'
						+ '<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">'
						+ '<li><a href="#" onclick="return hash2DDetails(\'' + full.metadata.name + '\',\'MetaData\', SECRETS_FULL,\'metadata\')">Meta Data</a>'
						+ '<li><a href="#" onclick="return hash2DDetails(\'' + full.metadata.name + '\',\'Payload\', SECRETS_FULL,\'data\', true)">Payload</a>'
						+ '<li class="divider"></li>'
						+ '<li><a href="#" onclick="return deleteSecret(\'' + full.metadata.name  + '\',\'' + full.metadata.namespace + '\' )">Delete</a>'
						//+ '<li><a href="#" onclick="return hash3DDetails(\'' + full.metadata.name + '\',\'Ports\', SECRETS_FULL,\'spec\',\'ports\')">Ports</a>'
						+ '</ul>'
						+ '</div>'
						+ '</div>';
			}} 
		],
		stateSave: true
		// paging: false 
		// searching: false
	};
	//TBL5 = $('#tblSecrets').DataTable( config);
	init_tblSecrets (config);
}

function addSecret() {
	var name 	= $('#secretName').val();
	var ns 		= $('#secretNS').val();
	var type 	= $('#secretType').val();
	var data	= {};
	var patchsa = $('#patch-sa-ips').prop('checked');	// patch SA img pull secret 
	var account	= $('#patch-account').val();			// service account (SA)
	
	// add Data hash (key,val)
	for ( var i = 1 ; i < 5 ; i++) {
		var key = $('#secretDataKey' + i);
		var val = $('#secretDataVal' + i);
		if ( key && val && key.val() != '' && val.val() != '') {
			data[key.val()] = val.val();
		}
	}
	if ( name == '' || ns == '' || type == '') {
		secretStatusMsg ('Name, namespace and type are required.', 'danger');
		return;
	}
	if ( Object.keys(data).length == 0 ) {
		secretStatusMsg ('At least 1 dat,key value pair is required.', 'danger');
		return;
	}
	LOGD('Add secret ' + name + ' NS: ' + ns + ' Type:' + type + ' PathSA:' + patchsa + ' SA:' + account + ' Data:' + JSON.stringify(data));

	var installUrl 	= url + '&op=CreateSecret&name=' + name + '&namespace=' + ns + '&type=' + type + '&template=secret.json&bodyKey=DATAOBJ';
	
	LOGD('Save secret @ ' + installUrl);
	var posting = $.ajax( { url: installUrl, type:"POST", data: JSON.stringify(data), contentType:"application/json; charset=utf-8", dataType:"json" } );
	
	// get results 
	posting.done(function( data ) {
		// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"Secret in version \\\"v1\\\" cannot be handled as a Secret: v1.Secret.ObjectMeta: v1.ObjectMeta.TypeMeta: Kind: Data: decode base64: illegal base64 data at input byte 0, error found in #10 byte of ...|{\\\"k1\\\":\\\"v1\\\"},\\r\\n    \\\"k|..., bigger context ...|{\\r\\n    \\\"apiVersion\\\": \\\"v1\\\",\\r\\n    \\\"data\\\": {\\\"k1\\\":\\\"v1\\\"},\\r\\n    \\\"kind\\\": \\\"Secret\\\",\\r\\n    \\\"metadata\\\": {\\r\\n    |...\",\"reason\":\"BadRequest\",\"code\":400}\n","status":400}
		// LOGD("Create Secret Resp: " + JSON.stringify(data));
		
		if ( data.status >= 400) {
			var json = JSON.parse(data.message);
			secretStatusMsg  (json.message, 'danger');
			return;
		}
		secretStatusMsg ('Saved ' + name);
		// patch SA?
		if ( patchsa ) {
			LOGD('Patching SA ' + account + ' image pull secret with ' + name);
			
			var payload = {"imagePullSecrets": [{"name": name }]};
			patchServiceAccount (account, ns, payload);
		}
		refreshSecretsTable ();
	});
	
	return false;	
}

/**
 * Patch a service account for secret for image pull
 * @param name SA name
 * @param ns namespace
 * @param data JSON payload, for example, to patch the image pull secret: {"imagePullSecrets": [{"name": name }]};
 */
function patchServiceAccount(name, ns, data) {
	var installUrl 	= url + '&op=PatchServiceAccount&serviceaccount=' + name + '&namespace=' + ns ; // + '&debug=true' ;
	var postData	= JSON.stringify(data);
	
	LOGD('Patch service account @ ' + installUrl + ' data:' + postData);

	var posting = $.ajax( { url: installUrl, type:"POST", data: postData , contentType:"application/json; charset=utf-8", dataType:"json" } );
	
	// get results 
	posting.done(function( data ) {
		// ERROR: {"data":{"items":[]},"message":"Stream closed","status":503}
		// ERROR: {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"415: Unsupported Media Type\",\"reason\":\"UnsupportedMediaType\",\"details\":{},\"code\":415}\n","status":415}
		LOGD("Patch SA pull secret Resp: " + JSON.stringify(data));
		/*
		if ( data.status >= 415) {
			var json = JSON.parse(data.message);
			secretStatusMsg ('Saved secret. ' + json.message, 'warn');
			return;
		} 
		else*/ if ( data.status >= 400) {
			secretStatusMsg ('Patch failed with ' + data.message, 'warning');
			return;
		}
		//growl ('Patched ' + name);
	});
}

function refreshSecretsTable () {
	$('#btn_refresh_tblSecrets').click();
}

function delSecrets() {
	//growl('Please wait.');
	
	// refresh after 5 secs
	//setTimeout(function() { refreshSecretsTable () }, 5000);
	
	tblSecrets.rows().every( function (rowIdx, tableLoop, rowLoop) {
		//{"metadata":{"uid":"9f03607f-714a-11ea-a941-000c297a5447","finalizers":["kubernetes.io/pv-protection"],"resourceVersion":"36389935","name":"nfs-pv","creationTimestamp":"2020-03-28T23:19:49Z","annotations":{"pv.kubernetes.io/bound-by-controller":"yes","kubectl.kubernetes.io/last-applied-configuration":"{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"annotations\":{},\"name\":\"nfs-pv\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"mountOptions\":[\"hard\",\"nfsvers=3\"],\"nfs\":{\"path\":\"/var/acmee\",\"server\":\"192.168.40.84\"},\"persistentVolumeReclaimPolicy\":\"Recycle\",\"storageClassName\":\"manual\",\"volumeMode\":\"Filesystem\"}}\n"},"selfLink":"/api/v1/persistentvolumes/nfs-pv","kind":"Volume"},"spec":{"claimRef":{"uid":"50955bd3-714c-11ea-a941-000c297a5447","apiVersion":"v1","kind":"PersistentVolumeClaim","resourceVersion":"36389932","namespace":"default","name":"nfs-pvc"},"storageClassName":"manual","mountOptions":["hard","nfsvers=3"],"nfs":{"server":"192.168.40.84","path":"/var/cloud"},"persistentVolumeReclaimPolicy":"Recycle","accessModes":["ReadWriteMany"],"capacity":{"storage":"1Gi"},"volumeMode":"Filesystem"},"status":{"phase":"Bound"}} 
		var d 			= this.data();
		var name		= d.metadata.name;
		var id			= 'chk-' + name;
		var namespace	= d.metadata.namespace;
		
		if ( $('#' + id).is(':checked')) {
			LOGD('Del row ' + rowIdx + ' ns: ' + namespace + ' name:' + name );
			deleteSecret (name, namespace);
		}
	});
}

function deleteSecret(name, namespace) {
	var installUrl 	= url + '&op=DeleteSecret&name=' + name + '&namespace=' + namespace;

	LOGD('Delete secret ' + name + ' ns: ' + namespace + ' @ ' + installUrl);
	var posting = $.ajax( { url: installUrl, type:"DELETE" } );
	
	// get results 
	posting.done(function( data ) {
		// OK {"data":{"metadata":{},"apiVersion":"v1","kind":"Status","details":{"uid":"019ec481-6977-11ea-a941-000c297a5447","kind":"secrets","name":"foo"},"status":"Success"},"message":"OK","status":200}
		// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"Secret in version \\\"v1\\\" cannot be handled as a Secret: v1.Secret.ObjectMeta: v1.ObjectMeta.TypeMeta: Kind: Data: decode base64: illegal base64 data at input byte 0, error found in #10 byte of ...|{\\\"k1\\\":\\\"v1\\\"},\\r\\n    \\\"k|..., bigger context ...|{\\r\\n    \\\"apiVersion\\\": \\\"v1\\\",\\r\\n    \\\"data\\\": {\\\"k1\\\":\\\"v1\\\"},\\r\\n    \\\"kind\\\": \\\"Secret\\\",\\r\\n    \\\"metadata\\\": {\\r\\n    |...\",\"reason\":\"BadRequest\",\"code\":400}\n","status":400}
		LOGD("Delete Secret Resp: " + JSON.stringify(data));
		
		if ( data.status >= 400) {
			//var json = JSON.parse(data.message);
			growl(data.message, 'danger');
			return;
		}
		growl ('Deleted ' + name);
		//$('#btnRefreshSecrets').click();
		refreshSecretsTable ();
	});
}

function secretStatusMsg (text, type) {
	modalSetStatus ('secretStatusMsg', text, type, 'altair');
}
