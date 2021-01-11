/**
 * Javascript functions for manage.jsp
 */

// level: info, danger, warning, success
function growl ( text, level, delay) {
	level = level || 'info';
	delay = delay || 30000;
	$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
}


function initializeDataTables () {
	// services Names[, ]
	TBL1 = $('#tblServices').DataTable( {
		"ajax": url + '&op=ListServices',
		"columns": [
				{ "data": "Spec.Name" },
				{ "data": "Version.Index" },
				{ "data": "ID" },
				{ "data": "UpdatedAt" },
				{ "data": "Spec.TaskTemplate.ContainerSpec" },
				{ "data": "Endpoint.Spec" }
		],
		"columnDefs": [{
			"targets": 6, // Actions col(5)
			"render": function ( data, type, full, meta ) {
				//LOGD("full=" + JSON.stringify(full));
				return '<div class="dropdown"><a class="dropdown-toggle" data-toggle="dropdown" href="#">Action<span class="caret"></span></a>'
					+ '<ul class="dropdown-menu" role="menu">'
					+ '<li><a href="#" onclick="return serviceAction(\'RemoveService\',\'' + full.ID + '\')">Remove</a></li>'
					/*+ '<li><a href="#" onclick="return containerAction(\'StopContainer\',\'' + full.Id + '\')">Stop</a></li>'
					
					+ '<li><a target="_blank" href="logs.jsp?node=' + node + '&op=ContainerLogs&Id=' + full.Id + '">Logs</a></li>'
					+ '<li><a target="_blank" href="xterm/xterm.jsp?node=' + node + '&Id=' + full.Id + '">XTerm</a></li>'
					+ '<li><a href="#" onclick="return containerAction(\'InspectContainer\',\'' + full.Id + '\')">Inspect</a></li>'
					+ '<li><a href="#" onclick="return containerAction(\'RemoveContainer\',\'' + full.Id + '\')">Remove</a></li>'
					*/
					+ '</ul></div>';
			}} ,
			// Wrap name with an inspect href
			{
				"targets": 0, 
				"render": function ( data, type, full, meta ) {
					return '<div><a href="#" onclick="return serviceAction(\'InspectService\',\'' + full.ID + '\')">' + data + '</a>'
						+ '</div>' ;
			}},
			// JSON: container spec, endpoint
			{
				"targets": [4,5],
				"createdCell": function (td, cellData, rowData, row, col) {
					$(td).jsonViewer(cellData);	
			}}				
		],
		stateSave: true,
		paging: false,
		searching: false
	});

	/* tasks  RepoTags[, ] */
	TBL2 = $('#tblTasks').DataTable( {
		"ajax": url + '&op=ListTasks',
		"columns": [
				{ "data": "Version.Index" },
				{ "data": "ID" },
				{ "data": "CreatedAt" },
				{ "data": "Status" },
				{ "data": "Spec.ContainerSpec" }
		],
		"columnDefs": [
			// Actions
			{
				"targets": 5,
				"render": function ( data, type, full, meta ) { 
					return 	'<a href="#" onclick="return serviceAction(\'InspectTask\',\'' + full.ID + '\')">Inspect</a>'
						//+ '&nbsp;&nbsp;&nbsp;<a href="#" onclick="return imageAction(\'RemoveImage\',\'' + full.RepoTags[0] + '\')">Remove</a>'
						;

				}
			},
			// JSON: status(3), container spec(4)
			{
				"targets": [3, 4],
				"createdCell": function (td, cellData, rowData, row, col) {
					$(td).jsonViewer(cellData);	
			}}				
			
		],
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
	TBL1.column(2).visible (false);
	TBL2.column(1).visible (false);
	
}


/**
 * Type ahead - https://twitter.github.io/typeahead.js/examples/
 * for docker hub - https://hub.docker.com/api/content/v1/products/search?page_size=50&q=busybox&type=image
 */

// templates
var template = Handlebars.compile($("#result-template").html());
var empty = Handlebars.compile($("#empty-template").html());

$('#Image').addClass('typeahead');

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

$('#image-box .typeahead').typeahead(null, 
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


