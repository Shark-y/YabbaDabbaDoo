<%@page import="com.cloud.docker.DockerParams"%>
<%
	final String contextPath 	= getServletContext().getContextPath();
	final String node 			= request.getParameter("name");
%>
  
		<div id="modal2" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Create Image <small>by pulling it from the <a href="https://hub.docker.com/" target="_blank">registry</a> or by importing it.</small></h3>
					</div>
					<div class="modal-body">
					
						<div class="form-group">
							<label class="col-sm-2 control-label">Image</label>
							<div class="col-sm-10">
								<input class="form-control" id="<%=DockerParams.W3_PARAM_IMAGE%>" name="<%=DockerParams.W3_PARAM_IMAGE%>" required data-toggle="tooltip"
									pattern="[A-z0-9_\-/:\.]+" title="Image name (no spaces)." value="">
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Tag</label>
							<div class="col-sm-10">
								<input type="text" name="<%=DockerParams.W3_PARAM_TAG%>" class="form-control" title="A version (tag) value." placeholder="latest" value="latest"/>
							</div>
						</div>

						<div id="divCred" class="form-group" style="display: none;">
							<label class="col-sm-2 control-label">Credential</label>
							<div class="col-sm-10">
 								<input type="hidden" id="credName" style="width:100% !important" value="brown, red, green"/>
 							
								<!--   
								<input type="text" name="credName" title="Enter a credential name to save it." placeholder="" />
								-->
								<!-- class="form-control" 
								<select id="credName" name="credName" style="width: 100%">
									<option>white</option>
									<option>Foo</option>
								</select>
								-->
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">User</label>
							<div class="col-sm-10">
								<input type="text" name="<%=DockerParams.W3_PARAM_USERNAME%>" class="form-control" title="Optional username: (for private images)" placeholder="" value=""/>
							</div>
						</div>

						<div class="form-group">
							<div class="col-sm-2">
								<label class="control-label">Password</label>
								<a href="#" title="Click to toggle file selection" style="" onclick="$('#divPwdSel').toggle();$('#divPwd').toggleClass('col-sm-10').toggleClass('col-sm-8')">[+]</a>
							</div>
							<div id="divPwd" class="col-sm-10">
								<input type="text" class="form-control" id="<%=DockerParams.W3_PARAM_PASSWORD%>" name="<%=DockerParams.W3_PARAM_PASSWORD%>"  
									title="Optional password (for private images)">
							</div>
							<div id="divPwdSel" class="col-sm-2" style="display: none;">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="filePwd" onchange="openFile(event, 'password')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>
						</div>
						<!-- 
						<div class="form-group">
							<label class="col-sm-2 control-label">Options</label>
							<div class="col-sm-4">
								<div class="checkbox">
									<label title="Check to save or load your credentials.">
										<input type="checkbox" id="chkCred" name="chkCreds" <%=(false ? "checked" : "")%> >
										<i class="input-helper"></i> Save/Load Creds
									</label>
								</div>
							</div>
						</div>
						-->
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<!-- 
						<a href="#" title="Click for advanced options." style="" onclick="$('#divCred').toggle();">Advanced</a>
						
						<button id="btnCloseModal2" type="button" class="btn btn-raised btn-info" onclick="return image_advanced()">Advanced</button>
						-->
						&nbsp;&nbsp;&nbsp;
						<button id="btnCloseModal2" type="button" class="btn btn-raised btn-default" data-dismiss="modal">Close</button>
						&nbsp;&nbsp;&nbsp;
						<button type="submit" class="btn btn-raised btn-primary" onclick="return image_create()">Create</button>
					</div>
					
				</div>
			</div>
		</div>
		<!-- end modal 2 (images) -->
		
		<!-- modal 3 inspect images/containers -->
		<div id="modal3" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 id="inspectTitle" class="modal-title"></h3>
					</div>
					<div id="json-renderer"></div>
				</div>
			</div>
		</div>
		<button id="btnInspect" data-toggle="modal" data-target="#modal3" style="display: none"></button>	
		
		<script>
		// fires a file is selected from an INPUT type=file
		var openFile = function(event, destination) {
		    var input 	= event.target;
		    var reader 	= new FileReader();
		    LOGD('openFile d:' + destination + " input:" + input + ' files[0]: ' + input.files[0]);
		    reader.onload = function() {
		    	var data 	= reader.result;
		    	// Set data - remove LF 
		    	$('#' + destination).val(data.replace(/\n/g,""));
		    };
		    if ( input.files[0]) {
		    	reader.readAsText(input.files[0]);
		    }
		};
		
		// Fires when the Create button is clicked
		function image_create() {
			var data = $('#frm2').serialize();
			LOGD('Create Image: ' + data);
			
			//growl ('<i class="fas fa-spinner fa-spin fa-2x"></i> PLEASE WAIT. This may take a while for large images.', 'info');
			Snackbar.show({duration: 0, text: '<i class="fas fa-spinner fa-spin fa-2x"></i> PLEASE WAIT. This may take a while for large images.'});
			
			//pacify_start ('.tab-pane', 3000);
			
			// validate: dismiss modal2
			if ( $('#Image').val() != '' ) {
				$('#btnCloseModal2').click();
				// send data
				var url 	= '<%=contextPath%>/Docker?op=CreateImage&node=<%=node%>';
				var posting = $.post( url , data);
				
				// get results 
				posting.done(function( data ) {
					// {"message":"OK","status":200,"data":  {"id":"latest","status":"Pulling from library/busybox"}}
					// HTTP 404 : {"message":"pull access denied for busybox_foo, repository does not exist or may require 'docker login'"}
					LOGD("Resp: " + JSON.stringify(data));
					var error	= ( data.status && (data.status >= 400)) ? true : false;	
					var type  	=  error ? 'danger' : 'info';
					
					if ( error && data.message ) {
						growl (data.message, type);
						return;
					}
					if ( data.data && data.data.id) {
						growl ('Created ' + data.data.id + ': ' + data.data.status, type);
					}
					if ( data.data && data.data.status) {
						growl(data.data.status, type);
					}
					// {"message":"OK","status":200,"data":[{"status":"Trying to pull repository us.gcr.io/cloud-bots/agentaggregator ... "},{"status":"Pulling repository us.gcr.io/cloud-bots/agentaggregator"},{"errorDetail":{"message":"unauthorized: authentication required"},"error":"unauthorized: authentication required"}]}
					if ( Array.isArray(data.data) ) {
						for (var i = 0 ; i < data.data.length ;i++) {
							var obj = data.data[i];
							// look 4 errors
							if ( obj.error) {
								growl (obj.error, 'danger');
								
							}
							else if (obj.status) {
								Snackbar.setText (obj.status);
							}
						}
					}
					// refresh
					$('#btnRefreshImages').click();
				});
			}
			return true;
		}
		
		function initModalImage() {
			LOGD('Modal img init.');
			
			// https://select2.org/tagging
			// 3.4.2 - Error: Option 'tags' is not allowed for Select2 when attached to a <select> element.
			//$('#credName').select2({width: "100%", tags:["red", "white", "purple", "orange", "yellow"]});
		}
		
		</script>
		
		<!-- modal 4 select shell -->
		<div id="modal4" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog modal-sm">
				<div class="modal-content">
					<div class="modal-header">
						<button id="btnCloseModal4" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 id="inspectTitle" class="modal-title">Select a Shell</h3>
					</div>
					<div class="modal-body">
						<select id="shell" class="form-control">
							<option value="sh">Basic (sh)</option>
							<option value="/bin/bash">Bash (/bin/bash)</option>
						</select>
						<a id="linkshell" target="_blank" style="display: none;">XTerm</a>
					</div>
					<div class="modal-footer">
						<button class="btn btn-raised btn-primary" onclick="return shell_start()">Start</button>
					</div>
					
				</div>
			</div>
		</div>
		<button id="btnShell" data-toggle="modal" data-target="#modal4" style="display: none"></button>
		
		<script>
		// fires when the XTerm action is clicked in the container Action menu.
		function xterm (node, containerId) {
			LOGD("Xterm node: " + node + " containerId:" + containerId);

			// set href <a target="_blank" href="xterm/xterm.jsp?node=' + node + '&Id=' + full.Id + '">XTerm</a>
			$("#linkshell").attr("href", 'xterm/xterm.jsp?node=' + node + '&Id=' + containerId);
			$('#btnShell').click();		// open select shell modal
			return false;
		}
		// Fires when the Start btn is pressed on the select shell modal
		function shell_start() {
			var shell 	= $('#shell').children("option:selected").val();
			var href 	= $("#linkshell").attr("href") + '&shell=' + shell;
			LOGD("Xterm href: " + href);
			
			// set link
			$("#linkshell").attr("href", href);
			$("#linkshell")[0].click();		// click anchor, will open a window using href
			$('#btnCloseModal4').click();	// close modal
			return false;	
		}
		</script>	
		