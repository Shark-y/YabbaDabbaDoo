//var TBL6;					// Secrets tbl
//var EVENTS_FULL  	= {};	// server secrets hash

/**
 * Events data-table
 */

function initializeEventsTable() {
	// Services
	var config = {
		"ajax": { 
			"url": url + '&op=ListEvents',
			"dataSrc": /*"data.items" */function ( json ) {
				// {"data":{"items":[]},"message":"400 Bad Request","status":400}
				if (json.status >= 400) {
					var reason = json.message;
					growl('Get Events: ' + reason, 'danger');	
				}
				return json.data.items;
			}
		},
		"columns": [
				{ "data": "involvedObject.kind" }, 
				{ "data": "metadata.namespace" },
				{ "data": "type" },
				{ "data": "metadata.creationTimestamp" },
				{ "data": "message" },
		],
		"columnDefs": [ 
   			{ "targets": 2, 
				"render": function ( data, type, full, meta ) {
					return wrapInLabel (data);
			}} ,
		    // message
			{ "targets": 4, 
				"render": function ( data, type, full, meta ) {
					//LOGD("full=" + JSON.stringify(full));
					// EVENTS_FULL[full.metadata.name] = full;
					return wrapInLabel (full.reason) + " " + data + ' <ul><li>Count: ' + full.count 
						+ '<li>Source: ' + full.source.component + (full.source.host ? ' @ ' + full.source.host  : '') +'</ul>';
			}}
		],
		stateSave: true
		// paging: false 
		// searching: false
	};
	//TBL6 = $('#tblEvents').DataTable( config );
	init_tblEvents(config);
}



