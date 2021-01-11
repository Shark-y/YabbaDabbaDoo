
/**
 * Storage data-table: PVs, PVCs
 */
function initializeStorageTable() {
	// Services
	var config = {
		"ajax": { 
			"url": url + '&op=ListPVs,ListPVCs,ListStorageClasses',
			"dataSrc": /*"data.items" */function ( json ) {
				// {"data":{"items":[]},"message":"400 Bad Request","status":400}
				if (json.status >= 400) {
					var reason = json.message;
					growl('Get Storage: ' + reason, 'danger');
					return [];
				}
				//LOGD('Strorage: ' + JSON.stringify(json));
				
				var pvs 	= json["ListPVs"].items;
				var pvcs 	= json["ListPVCs"].items;
				var scs 	= json["ListStorageClasses"].items;
				
				// inject kinds
				for ( var i = 0 ; i < pvs.length ;i++) {
					pvs[i].metadata.kind = "Volume";
				}
				for ( var i = 0 ; i < pvcs.length ;i++) {
					pvcs[i].metadata.kind = "VolumeClaim";
				}
				for ( var i = 0 ; i < scs.length ;i++) {
					scs[i].metadata.kind = "StorageClass";
				}
				var items 	= pvs.concat(pvcs, scs);
				
				return items; //json.data.items;
			}
		},
		"columns": [
				{ "data": "metadata.kind" }, 
				{ "data": "metadata.name" },
				{ "data": "status.phase", "defaultContent": "" },
				{ "data": "metadata.creationTimestamp" },
				{ "data": "metadata.name" },
		],
		"columnDefs": [ 
 			{ "targets": 0, 
				"render": function ( data, type, full, meta ) {
					var key 	= full.metadata.name;
					var html 	= '<input id="chk-' + key + '" type="checkbox" value="' + key + '"> '; 
					return html +  data;
			}} , 
   			{ "targets": 2, 
				"render": function ( data, type, full, meta ) {
					return wrapInLabel (data);
			}} , 
		    // message
			{ "targets": 4, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					var meta 	= full.metadata;
					var status	= full.status;
					var spec 	= full.spec;
					var vol		= spec && spec.claimRef && spec.claimRef.name ? ' Claim: ' + spec.claimRef.name : '';
					vol			+= spec && spec.volumeName ? ' Volume: ' + spec.volumeName : '';
					
					var data 	= meta.annotations && meta.annotations["Description"] ? '<i>' + meta.annotations["Description"] +'</i>' : ''
					data		+= '<ul>'
					data		+= meta.namespace					? '<li>Namespace: ' + meta.namespace : '';
					data 		+= status && status.message 		? '<li>Message: ' 	+ status.message : '';
					data		+= vol != '' 						? '<li>' + vol : '';
					data		+= spec && spec.storageClassName 	? '<li>Storage Class: ' + spec.storageClassName : '';
					data		+= spec && spec.accessModes 		? '<li>Access Modes: ' + spec.accessModes : '';
					data		+= spec && spec.capacity && spec.capacity.storage 	? '<li>Capacity: ' + spec.capacity.storage : '';
					data		+= spec && spec.nfs					? '<li>NFS: ' + spec.nfs.server + ':' + spec.nfs.path : '';
					// storage classes
					data		+= full.provisioner					? '<li> Provisioner: ' + full.provisioner : '';
					data		+= full.reclaimPolicy				? '<li> Reclaim Policy: ' + full.reclaimPolicy : '';
					data		+= '</ul>'
					return data;
			}} 
		],
		stateSave: true
		// paging: false 
		// searching: false
	};
	//TBL7 = $('#tblStorage').DataTable(config);
	init_tblStorage (config);
}

/**
 * Fires when the add volume menu option is slected.
 * @returns {Boolean}
 */
function addVolume(basePath, node) {
	var tbHtml = '<a href="#" onclick="return templateNfsPV()">NFS</a>'
	modalYmlOpen (null, null, 'Volume' , function (yml) {
		// add volume
		var installUrl 	= url + '&op=CreatePV';
		
		LOGD('Create PV @ ' + installUrl);
		storageExecute (installUrl, yml);
	}, tbHtml);
	return false;
}

/**
 * Fires when the add volume claim menu is selected.
 * @returns {Boolean}
 */
function addVolumeClaim(basePath, node) {
	var tbHtml = '<a href="#" onclick="return templateNfsPVC()">NFS</a>'

	modalYmlOpen (basePath, node, 'Volume Claim', function (yml, ns) {
		var installUrl 	= url + '&op=CreatePVC&namespace=' + ns;

		LOGD('Create PVC @ ' + installUrl);
		storageExecute (installUrl, yml);
		
	}, tbHtml);
	return false;
}

function storageExecute (url, yml) {
	var posting = $.ajax( { url: url, type:"POST", data: yml, contentType:"text/yaml; charset=utf-8", dataType:"json" } );
	
	// get results 
	posting.done(function( data ) {
		// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"the object provided is unrecognized (must be of type PersistentVolume): couldn't get version/kind; json parse error: invalid character 'a' looking for beginning of value (61706956657273696f6e3a2076310d0a6b696e643a205065727369737465 ...)\",\"reason\":\"BadRequest\",\"code\":400}\n","status":400}
		LOGD("Create PV Resp: " + JSON.stringify(data));
		
		if ( data.status >= 400) {
			var json 		= JSON.parse(data.message);
			modalYmlSetStatus  (json.message, 'danger');
			return;
		}
		modalYmlSetStatus ('Saved resource.');
		refreshStorageTable ();
	});
}

function refreshStorageTable () {
	$('#btn_refresh_tblStorage').click();
}

function delVolumes() {
	growl('Please wait.');
	
	// refresh after 5 secs
	//setTimeout(function() { $('#btnRefreshStorage').click(); }, 5000);
	setTimeout(function() { refreshStorageTable () }, 5000);
	
	/*TBL7*/tblStorage.rows().every( function (rowIdx, tableLoop, rowLoop) {
		//{"metadata":{"uid":"9f03607f-714a-11ea-a941-000c297a5447","finalizers":["kubernetes.io/pv-protection"],"resourceVersion":"36389935","name":"nfs-pv","creationTimestamp":"2020-03-28T23:19:49Z","annotations":{"pv.kubernetes.io/bound-by-controller":"yes","kubectl.kubernetes.io/last-applied-configuration":"{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"annotations\":{},\"name\":\"nfs-pv\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"mountOptions\":[\"hard\",\"nfsvers=3\"],\"nfs\":{\"path\":\"/var/acme\",\"server\":\"192.168.40.84\"},\"persistentVolumeReclaimPolicy\":\"Recycle\",\"storageClassName\":\"manual\",\"volumeMode\":\"Filesystem\"}}\n"},"selfLink":"/api/v1/persistentvolumes/nfs-pv","kind":"Volume"},"spec":{"claimRef":{"uid":"50955bd3-714c-11ea-a941-000c297a5447","apiVersion":"v1","kind":"PersistentVolumeClaim","resourceVersion":"36389932","namespace":"default","name":"nfs-pvc"},"storageClassName":"manual","mountOptions":["hard","nfsvers=3"],"nfs":{"server":"192.168.40.84","path":"/var"},"persistentVolumeReclaimPolicy":"Recycle","accessModes":["ReadWriteMany"],"capacity":{"storage":"1Gi"},"volumeMode":"Filesystem"},"status":{"phase":"Bound"}} 
		var d 			= this.data();
		var kind		= d.metadata.kind;	// Volume, VolumeClaim
		var name		= d.metadata.name;
		var id			= 'chk-' + name;
		var namespace	= kind == 'VolumeClaim' ? d.metadata.namespace : '';
		
		if ( $('#' + id).is(':checked')) {
			LOGD('del ' + rowIdx + ' kind: ' + kind + ' name:' + name );
			var op = 'Delete' + kind;
			deleteVolume (op, name, namespace, false);
		}
	});
}

function deleteVolume (op, name, namespace, refresh) {
	refresh 		= refresh || false;
	var installUrl 	= url + '&op=' + op + '&name=' + name + '&namespace=' + namespace;

	LOGD('Delete volume ' + name + ' ns: ' + namespace + ' @ ' + installUrl);
	var posting = $.ajax( { url: installUrl, type:"DELETE" } );
	
	// get results 
	posting.done(function( data ) {
		// OK {"data":{"metadata":{},"apiVersion":"v1","kind":"Status","details":{"uid":"019ec481-6977-11ea-a941-000c297a5447","kind":"secrets","name":"foo"},"status":"Success"},"message":"OK","status":200}
		// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"Secret in version \\\"v1\\\" cannot be handled as a Secret: v1.Secret.ObjectMeta: v1.ObjectMeta.TypeMeta: Kind: Data: decode base64: illegal base64 data at input byte 0, error found in #10 byte of ...|{\\\"k1\\\":\\\"v1\\\"},\\r\\n    \\\"k|..., bigger context ...|{\\r\\n    \\\"apiVersion\\\": \\\"v1\\\",\\r\\n    \\\"data\\\": {\\\"k1\\\":\\\"v1\\\"},\\r\\n    \\\"kind\\\": \\\"Secret\\\",\\r\\n    \\\"metadata\\\": {\\r\\n    |...\",\"reason\":\"BadRequest\",\"code\":400}\n","status":400}
		LOGD("Delete Volume Resp: " + JSON.stringify(data));
		
		if ( data.status >= 400) {
			var json = JSON.parse(data.message);
			growl(json.message, 'danger');
			return;
		}
		if ( refresh ) {
			growl ('Deleted ' + name);
			//$('#btnRefreshStorage').click();
			refreshStorageTable ();
		}
	});
}

// load an nfs pv template
function templateNfsPV() {
	var installUrl 	= url + '&op=GetYamlResource&template=pv-nfs.yml';

	var posting = $.get (installUrl);
	
	// get results 
	posting.done(function( data ) {
		//{"data":{"items":[]},"message":"GetYamlReosurce not found.","status":500}
		//LOGD('NFS: ' + JSON.stringify(data));
		if ( data.status >= 400) {
			modalYmlSetStatus (data.message, 'danger');
		}
		else {
			modalYmlSetValue(data["pv-nfs.yml"]);
		}
		
	});
	return false;
}


// Load an nfs pvc template
function templateNfsPVC() {
	var installUrl 	= url + '&op=GetYamlResource&template=pvc-nfs.yml';

	var posting = $.get (installUrl);
	
	// get results 
	posting.done(function( data ) {
		if ( data.status >= 400) {
			modalYmlSetStatus (data.message, 'danger');
		}
		else {
			modalYmlSetValue(data["pvc-nfs.yml"]);
		}
	});
	return false;
}
