/**
 * Misc funcs
 */

/**
 * Initialize page data tables: containers, images, networks, volumes
 * @param node Node name
 */
function initializeDataTables (node) {
	// containers
	TBL1 = $('#tblContainers').DataTable( {
		"ajax": url + '&op=ListContainers',
		"columns": [
				{ "data": "Names[, ]" },
				{ "data": "Id" },
				{ "data": "Image" },
				{ "data": "Command" },
				{ "data": "Status" }
		],
		"columnDefs": [{
			"targets": 5, // Actions col(5)
			"render": function ( data, type, full, meta ) {
				//LOGD("full=" + JSON.stringify(full));
				return '<div class="dropdown"><a class="dropdown-toggle" data-toggle="dropdown" href="#">Action<span class="caret"></span></a>'
					+ '<ul class="dropdown-menu" role="menu">'
					+ '<li><a href="#" onclick="return containerAction(\'StartContainer\',\'' + full.Id + '\')">Start</a></li>'
					+ '<li><a href="#" onclick="return containerAction(\'StopContainer\',\'' + full.Id + '\')">Stop</a></li>'
					+ '<li><a target="_blank" href="logs.jsp?node=' + node + '&op=ContainerLogs&Id=' + full.Id + '">Logs</a></li>'
					//+ '<li><a target="_blank" href="xterm/xterm.jsp?node=' + node + '&Id=' + full.Id + '">XTerm</a></li>'
					+ '<li><a href="#" onclick="return xterm(\'' + node + '\', \'' + full.Id + '\')">XTerm</a></li>'
					+ '<li><a href="#" onclick="return containerAction(\'InspectContainer\',\'' + full.Id + '\')">Inspect</a></li>'
					+ '<li><a href="#" onclick="return containerAction(\'RemoveContainer\',\'' + full.Id + '\')">Remove</a></li>'
					+ '</ul></div>';
			}} ,
			// Wrap name with an inspect href
			{
				"targets": 0, 
				"render": function ( data, type, full, meta ) {
					return '<div><a href="#" onclick="return containerAction(\'InspectContainer\',\'' + full.Id + '\')">' + data + '</a>'
						+ '</div>' ;
			}} ,
			// wrap status in an alert
			{
				"targets": 4, 
				"render": function ( data, type, full, meta ) {
					var cls 	= data.indexOf('Up') != -1 ? 'label label-success' : '';
					var link 	= '';
					
					// add a View link  (if running) using "Ports": [{ "Type": "tcp", "IP": "0.0.0.0",	"PrivatePort": 80, "PublicPort": 80	}]
					if ( (cls != '') && full.Ports && full.Ports.length > 0 ) {
						var port 	= full.Ports[0].PublicPort ? full.Ports[0].PublicPort : full.Ports[0].PrivatePort;
						link 		= ' <a target="_blank" href="http://'+ nodeIP + ':' + port + '">View</a>' 
					}
					return '<span class="' +  cls + '">' + data + '</span>' + link;
			}} 
		],
		stateSave: true,
		paging: false
		//searching: false
	});

	// Images
	TBL2 = $('#tblImages').DataTable( {
		"ajax": url + '&op=ListImages',
		"columns": [
				{ "data": "RepoTags[, ]" },
				{ "data": "Id" },
				{ "data": "Created" },
				{ "data": "Size" }
		],
		"columnDefs": [{
			"targets": 0,	
			"render": function ( data, type, full, meta ) {
				//LOGD("Tag=" + data);
				return data.replace(/</gi,'&lt;'); // fix 4 <none>:<none>
			}},
			{
			"targets": 3,	// Size
			"render": function ( data, type, full, meta ) {
				//LOGD("full=" + JSON.stringify(full));
				return formatBytes(data, 0); 
			}},
			// Actions
			{
				"targets": 4,
				"render": function ( data, type, full, meta ) { 
					var image = full.RepoTags ? full.RepoTags[0] : full.Id;
					return 	'<a href="#" onclick="return imageAction(\'InspectImage\',\'' + image + '\')">Inspect</a>'
						+ '&nbsp;&nbsp;&nbsp;<a href="#" onclick="return imageAction(\'RemoveImage\',\'' + image + '\')">Remove</a>';

				}
			},
			// Date
			{
				"targets": 2,
				"render": function ( data, type, full, meta ) { 
					return formatUnixDate (data); 
				}
			}
		],
		stateSave: true,
		paging: false,
		searching: false
	});

	// Networks
	// [{"Ingress":false,"Containers":{},"Labels":{},"ConfigFrom":{"Network":""},"Options":{},"IPAM":{"Driver":"default","Config":[],"Options":null},"Name":"host","Driver":"host","EnableIPv6":false,"Created":"2018-10-14T13:36:40.514378993Z","Id":"d9c6ebbc3a677a8767113050e905282d96569b101c2f86c52549a6ca93d8f313","Internal":false,"Attachable":false,"ConfigOnly":false,"Scope":"local"},{"Ingress":false,"Containers":{},"Labels":{},"ConfigFrom":{"Network":""},"Options":{"com.docker.network.bridge.enable_icc":"true","com.docker.network.bridge.name":"docker0","com.docker.network.bridge.host_binding_ipv4":"0.0.0.0","com.docker.network.driver.mtu":"1500","com.docker.network.bridge.default_bridge":"true","com.docker.network.bridge.enable_ip_masquerade":"true"},"IPAM":{"Driver":"default","Config":[{"Gateway":"172.17.0.1","Subnet":"172.17.0.0/16"}],"Options":null},"Name":"bridge","Driver":"bridge","EnableIPv6":false,"Created":"2019-03-12T22:36:44.967614446Z","Id":"44ee064c4d5470b896119d4d64c314e2b642a37476522f3adcf9204bd751b886","Internal":false,"Attachable":false,"ConfigOnly":false,"Scope":"local"},{"Ingress":false,"Containers":{},"Labels":{},"ConfigFrom":{"Network":""},"Options":{},"IPAM":{"Driver":"default","Config":[],"Options":null},"Name":"none","Driver":"null","EnableIPv6":false,"Created":"2018-10-14T13:36:40.500821242Z","Id":"573dd8ded77214dd3e9a17cc0d8859551002df50ca79d6a30164955b055dec44","Internal":false,"Attachable":false,"ConfigOnly":false,"Scope":"local"}]
	TBL3 = $('#tblNetworks').DataTable( {
		"ajax": url + '&op=ListNetworks',
		"columns": [
				{ "data": function (data, type, dataToSet) {
			        return data.Name + "/" + data.Driver;
			    }},
				{ "data": function (data, type, dataToSet) {
			        return (typeof(data.Ingress) != 'undefined' ? data.Ingress : 'None' ) + "/" + data.Internal + "/" + data.Attachable;
			    }},
				{ "data": "Options" },
				{ "data": "IPAM" }
		],
		// JSON cols 2,3
		"columnDefs": [ {
			"targets": [2,3],
			"createdCell": function (td, cellData, rowData, row, col) {
				$(td).jsonViewer(cellData);	
			} 			
		}], 		
		
		stateSave: true,
		paging: false,
		searching: false
	});

	// Volumes
	// {"Warnings":null,"Volumes":[{"Name":"0a2550b694f65e987cef4583dfe7534988b3f277bf280eca79f3255ce24437fb","Driver":"local","Mountpoint":"/mnt/sda1/var/lib/docker/volumes/0a2550b694f65e987cef4583dfe7534988b3f277bf280eca79f3255ce24437fb/_data","Labels":null,"Options":null,"CreatedAt":"2019-03-08T21:51:11Z","Scope":"local"},{"Name":"757850231daf3736314d6cfc4344257be6490bcc934c0021f81a6ffa8dc4fd62","Driver":"local","Mountpoint":"/mnt/sda1/var/lib/docker/volumes/757850231daf3736314d6cfc4344257be6490bcc934c0021f81a6ffa8dc4fd62/_data","Labels":null,"Options":null,"CreatedAt":"2019-03-08T20:10:57Z","Scope":"local"},{"Name":"9a11e91f9eaf8902dc03aaf835742b803d5c6683e1221d3283f0e759bc78bf3f","Driver":"local","Mountpoint":"/mnt/sda1/var/lib/docker/volumes/9a11e91f9eaf8902dc03aaf835742b803d5c6683e1221d3283f0e759bc78bf3f/_data","Labels":null,"Options":null,"CreatedAt":"2019-03-08T20:06:24Z","Scope":"local"},{"Name":"f9609f3799068ca54e350bdb069b88cd6ba776b4094a26b89114d99228427e4f","Driver":"local","Mountpoint":"/mnt/sda1/var/lib/docker/volumes/f9609f3799068ca54e350bdb069b88cd6ba776b4094a26b89114d99228427e4f/_data","Labels":null,"Options":null,"CreatedAt":"2019-03-11T17:07:02Z","Scope":"local"}]}
	TBL4 = $('#tblVolumes').DataTable( {
		"ajax": {
			"url" : url + '&op=ListVolumes',
			"dataSrc": "data.Volumes"
		},
		"columns": [
				{ "data": function (data, type, dataToSet) {
			        return data.Driver + "/" + data.Scope;
			    }},
			    // CreatedAt is optional
				{ "data": "CreatedAt", "defaultContent": "<i>Not set</i>" },
				{ "data": "Mountpoint" }
		],
		"columnDefs": [{
			"targets": 3, 
			"render": function ( data, type, full, meta ) {
				//LOGD("full=" + JSON.stringify(full));
				return '<div class="dropdown"><a class="dropdown-toggle" data-toggle="dropdown" href="#">Action<span class="caret"></span></a>'
					+ '<ul class="dropdown-menu" role="menu">'
					+ '<li><a href="#" onclick="return volumeAction(\'RemoveVolume\',\'' + full.Name + '\')">Remove</a></li>'
					+ '</ul></div>';
			}} 
		] ,
		stateSave: true,
		paging: false,
		searching: false
	});
	
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
	TBL1.column(1).visible (false);
	TBL2.column(1).visible (false);
}

/**
 *  Install an image from docker hub by settings its name into the images modal
 */
function hub_install(name) {
	LOGD('Install from docker hub:' + name);
	$('#Image').val(name);
	image_create();
	return false;
}

function formatBytes ( bytes, decimals) {
	if(bytes == 0) return '0 Bytes';
	var k = 1024;
	var dm = decimals <= 0 ? 0 : decimals || 2;
	var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
	var i = Math.floor(Math.log(bytes) / Math.log(k));
	return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

// https://stackoverflow.com/questions/847185/convert-a-unix-timestamp-to-time-in-javascript
function formatUnixDate ( unix_timestamp) {
	// multiplied by 1000 so that the argument is in milliseconds, not seconds.
	var a = new Date(unix_timestamp*1000);
	var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
	var year = a.getFullYear();
	var month = months[a.getMonth()];
	var date = a.getDate();
	var hour = a.getHours();
	var min = a.getMinutes();
	var sec = a.getSeconds();
	var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;
	return time;		
}
	
function containerAction (name, id) {
	var uri 	= url + '&op=' + name + '&Id=' + id;
	LOGD(name + " " + uri);
	
	var posting = $.post( uri , '');
	posting.done(function( data ) {
		LOGD("Resp: " + JSON.stringify(data));
		var error	= ( data.status && (data.status >= 400)) ? true : false;	
		var type  	=  error ? 'danger' : 'info';
		
		if ( error && data.message ) {
			growl (data.message, type);
			return;
		}
		// {"message":"OK","status":200,"data":""} - refresh
		$('#btnRefreshContainers').click();
		
		if ( name.indexOf('Inspect') != -1 ) {
			$('#inspectTitle').html('<small>' + id + '</small>');
			$('#json-renderer').jsonViewer(data.data);
			$('#btnInspect').click();
		}
	});
	return false;
}

/**
 * Image actions
 * @param name: RemoveImage, InspectImage, etc
 * @param id The image id
 * @returns {Boolean}
 */
function imageAction (name, id) {
	// RemoveImage /CloudClusterManager/Docker?node=Node1&op=RemoveImage&name=cloud/connector-foo:latest
	var uri 	= url + '&op=' + name + '&name=' + id;
	LOGD(name + " " + uri);

	// {"message":"OK","status":200,"data":[{"Untagged":"busybox:latest"},{"Untagged":"busybox@sha256:061ca9704a714ee3e8b80523ec720c64f6209ad3f97c0ff7cb9ec7d19f15149f"},{"Deleted":"sha256:d8233ab899d419c58cf3634c0df54ff5d8acc28f8173f09c21df4a07229e1205"},{"Deleted":"sha256:adab5d09ba79ecf30d3a5af58394b23a447eda7ffffe16c500ddc5ccb4c0222f"}]}
	return ajaxAction (name, id, 'btnRefreshImages', 'POST', uri);
}

function volumeAction (name, id) {
	var uri 	= url + '&op=' + name + '&name=' + id;
	LOGD(name + " " + uri);
	return ajaxAction (name, id, 'btnRefreshVolumes', 'POST', uri);
}

function ajaxAction (op, id, refreshBtn, method, uri, data) {
	dat = data || '';
	var posting = $.ajax( { method: method , url: uri, data : data } );
	
	posting.done(function( data ) {
		LOGD("Resp: " + JSON.stringify(data));
		var error	= ( data.status && (data.status >= 400)) ? true : false;	
		var type  	=  error ? 'danger' : 'info';
		
		if ( error && data.message ) {
			growl (data.message, type);
			return;
		}
		// refresh
		$('#' + refreshBtn).click();
		
		if ( op.indexOf('Inspect') != -1 ) {
			$('#inspectTitle').html(id);
			$('#json-renderer').jsonViewer(data.data);
			$('#btnInspect').click();
		}
	});
	return false;
}
	
// level: info, danger, warning, success
function growl ( text, level, delay) {
	delay = delay || 30000;
	$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
}
	

/**
 * Type ahead - https://twitter.github.io/typeahead.js/examples/
 * for docker hub - https://hub.docker.com/api/content/v1/products/search?page_size=50&q=busybox&type=image
 */

/* https://stackoverflow.com/questions/34252817/handlebarsjs-check-if-a-string-is-equal-to-a-value
 * {{#ifEquals sampleString "This is a string"}}
 *   Your HTML here
 * {{/ifEquals}}
 */
Handlebars.registerHelper('ifeq', function(arg1, arg2, options) {
    return (arg1 == arg2) ? options.fn(this) : options.inverse(this);
});

// templates
var template = Handlebars.compile($("#result-template").html());
var empty = Handlebars.compile($("#empty-template").html());

$('#search-input').addClass('typeahead');

// Bloodhound - 
var engine = new Bloodhound({
	  datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name', 'slug'),
	  queryTokenizer: Bloodhound.tokenizers.whitespace,
	  
	  remote: {
		url: url + '&op=search&q=%QUERY', 
	    wildcard: '%QUERY'
	  } 
	}); 

var engine1 = new Bloodhound({
	  datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name', 'slug'),
	  queryTokenizer: Bloodhound.tokenizers.whitespace,
	  remote: {
		url: url + '&op=search&q=%QUERY&source=community', 
	    wildcard: '%QUERY'
	  } 
}); 

$('#search-box .typeahead').typeahead(null, 
		{
		  name: 'publisher',
		  display: 'name',
		  //displayKey: 'name',
		  source: engine,
		  templates: {
			
			suggestion: template
			//empty: empty 
		  }  
		},
		{
		  name: 'community',
		  display: 'name',
		  source: engine1,
		  templates: {
			suggestion: template,
			empty: empty 
		  }  
		}
);

