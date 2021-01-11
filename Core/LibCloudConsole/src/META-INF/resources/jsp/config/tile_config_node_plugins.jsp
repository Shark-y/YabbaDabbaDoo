<%
final String contextPath 	= getServletContext().getContextPath();
%>

	<div id="modal5" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-body">
						<h3>Plugins <small><a href="#" onclick="$('#my-awesome-dropzone').toggle()">Upload</a></small></h3>
						
						<table id="tblPLugins" class="table" style="width: 100%">
							<thead>
								<tr>
									<th>Id</th>
									<th>Name</th>
									<th>Version</th>
									<th>Status</th>
								</tr>
							</thead>
							<tbody></tbody>
						</table>
						
						<div>
							<form style="display:none" action="../../SysAdmin" class="dropzone"  id="my-awesome-dropzone"><h2>Plugin Dropzone</h2><small>Container restart required.</small></form>
						</div>
					</div>
					
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button id="btnClodeModal5" type="button" class="btn btn-primary md-btn md-btn-flat uk-modal-close" data-dismiss="modal">Close</button>
					</div>
					
				</div>
			</div>
	</div>


<script type="text/javascript">

function initDropZone () {
	// https://www.dropzonejs.com/#configuration
	Dropzone.options.myAwesomeDropzone = {
		// The name that will be used to transfer the file
		// paramName: "file",
		// Max upload size - MB
		maxFilesize: 5, 
		params : { rq_operation : 'pluginupload' },	// http request params
		/* accept */
		accept: function(file, done) {
			var exts 	= ['jar', 'html', 'xml'];
			var found 	= false;
			
			for (var i = 0 ; i < exts.length ; i++ ) {
				// LOGD('accept ' + exts[i]);			
				// 10/7/2019 Fix for IE SCRIPT438: Object doesn't support property or method 'includes' 
		    	//if ( file.name.includes('.' + exts[i]) ) {
		    	if ( file.name.indexOf('.' + exts[i]) >= 0 ) {
		      		found = true;
		      		break;
		    	}
			}
			if ( !found ) {
				done("Invalid extension for " + file.name); // reject
			}
			else {
				done();
			}
		}
	}
}

function initPluginsUI () {
	//var url = '<%=contextPath%>/OSPerformance?op=vms';
	var url = '<%=contextPath%>/SysAdmin?op=plugins';
	
  	tableVMs = $('#tblPLugins').DataTable({
		stateSave: true,
		searching: false, paging: false,
		"ajax": url,
		"columns": [
					{ "data": "id" }, // Names[, ]
					{ "data": "name" },
					{ "data": "version" },
					{ "data": "status" }
		],
		"columnDefs": [
	   			/* Wrap name with an inspect href
				{	"targets": 0, 
					"render": function ( data, type, full, meta ) {
						LOGD("Plugings full=" + JSON.stringify(full));
						return '<div><a href="#" onclick="return fetchVM(' + full[0] + ')">' + data + '</a></div>' ;
				}} */ 
		]
	}); 
	
}

</script>