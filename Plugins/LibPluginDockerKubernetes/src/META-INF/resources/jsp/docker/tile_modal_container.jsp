<%@page import="com.cloud.docker.DockerParams"%>
<%
	final String contextPath 	= getServletContext().getContextPath();
	final String node 			= request.getParameter("name");
%>

		<div id="modal1" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Create Container</h3>
					</div>
					<div class="modal-body">
						<!-- 
						<div class="form-group">
							<label class="col-sm-2 control-label">Image</label>
							<div class="col-sm-10">
								<input class="form-control" id="c_<%=DockerParams.W3_PARAM_IMAGE%>" name="<%=DockerParams.W3_PARAM_IMAGE%>" required data-toggle="tooltip"
									pattern="[A-z0-9_-]+" title="Image name (no spaces)." value="">
							</div>
						</div>
						-->
						<div class="form-group">
							<label class="col-sm-2 control-label">Container Name</label>
							<div class="col-sm-10">
								<input class="form-control" id="<%=DockerParams.W3_PARAM_CONTAINER_NAME%>" name="<%=DockerParams.W3_PARAM_CONTAINER_NAME%>" data-toggle="tooltip"
									pattern="[a-zA-Z0-9_-]+" title="Assign the specified name to the container." placeholder="Optional name.">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Image</label>
							<div class="col-sm-10">
								<select class="form-control" id="sel<%=DockerParams.W3_PARAM_IMAGE%>" name="<%=DockerParams.W3_PARAM_IMAGE%>" title="Image installed in the host"></select>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Restart Policy</label>
							<div class="col-sm-10">
								<select class="form-control" id="sel<%=DockerParams.W3_PARAM_RESTART_POL%>" name="<%=DockerParams.W3_PARAM_RESTART_POL%>" 
									title="The behavior to apply when the container exits.">
									<option value="">None</option>
									<option value="always">Always restart.</option>
									<option value="unless-stopped">Restart always except when user has manually stopped the container.</option>
									<option value="on-failure">Restart only when the container exit code is non-zero.</option>
								</select>
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Environment</label>
							<div class="col-sm-10">
								<input type="text" name="<%=DockerParams.W3_PARAM_ENV%>" class="form-control" title="Optional comma separated environment variables: VAR1=VAL1,VAR2=VAL2,..." placeholder="VAR1=VAL1,VAR2=VAL2,..." value=""/>
							</div>
						</div>
						<div class="form-group">
							<div class="col-sm-2"> 
								<label class="control-label">Command</label> <a href="#" onclick="return fetchArgs();" title="Click to fetch arguments from the image descriptor."><i class="material-icons">help</i></a>
							</div>
							<div class="col-sm-10">
								<input type="text" id="<%=DockerParams.W3_PARAM_CMD%>" name="<%=DockerParams.W3_PARAM_CMD%>" class="form-control" title="Optional command arguments to execute: (e.g. date)" placeholder="date" value=""/>
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Exposed Ports</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_EXPOSEDPORTS%>" value="" placeholder="22/tcp,1024/udp,..."
									title="Optional comma separated list of ports to expose in the containder: (e.g. 22/tcp,...)">
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Port Bindings</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_PORTBINDINGS%>" value="" placeholder="8080/tcp:8080,80/tcp:80"
									title="Optional comma separated list of GuestPort:HostPort: (e.g. 8080/tcp:8080,80/tcp:80,...)">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Mount Points</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_BINDS%>" value="" placeholder="/tmp:/tmp,/var/log:/var/log"
									title="Optional comma separated list of GuestMount:HostMount: (e.g. /tmp:/tmp,/var/log:/var/log,...)">
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Volumes</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_VOLUMES%>" value="" placeholder="/volumes/data"
									title="Optional comma separated list of volumes: (e.g. /volumes/data,/volumes/data1,...)">
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Labels</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_LABELS%>" value="" placeholder="com.example.vendor:ACME,com.example.version:1.0,..."
									title="Optional comma separated list of GuestMount:HostMount: (e.g. com.example.vendor:ACME,com.example.version:1.0)">
							</div>
						</div>
						
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<button id="btnCloseModal1" type="button" class="btn btn-raised btn-default" data-dismiss="modal">Close</button>
						&nbsp;&nbsp;&nbsp;
						<button type="submit" class="btn btn-raised btn-primary" onclick="return container_create()">Create</button>
					</div>
					
				</div>
			</div>
		</div>

		<script>
		function fetchArgs() {
			var image = $('#selImage').children("option:selected").val();
			var uri 	= url + '&op=InspectImage&name=' + image;
			LOGD("Fetch args for " + image + ' url:' + uri);
			
			$.post( uri , '').done(function( data ) {
				// {"data":{"RepoDigests":["busybox@sha256:061ca9704a714ee3e8b80523ec720c64f6209ad3f97c0ff7cb9ec7d19f15149f"],"Comment":"","VirtualSize":1199417,"Architecture":"amd64","Os":"linux","Parent":"","Config":{"User":"","Entrypoint":null,"AttachStderr":false,"ArgsEscaped":true,"Hostname":"","OpenStdin":false,"Labels":null,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],"Image":"sha256:896f6e65107acffcae30fe593503aebf407fc30ad4566a8db04921dbfb6c0721","StdinOnce":false,"AttachStdout":false,"WorkingDir":"","Volumes":null,"OnBuild":null,"Domainname":"","AttachStdin":false,"Tty":false,"Cmd":["sh"]},"Size":1199417,"Metadata":{"LastTagTime":"0001-01-01T00:00:00Z"},"GraphDriver":{"Data":null,"Name":"aufs"},"Created":"2019-02-15T00:19:37.830935034Z","ContainerConfig":{"User":"","Entrypoint":null,"AttachStderr":false,"ArgsEscaped":true,"Hostname":"197cb47b0e98","OpenStdin":false,"Labels":{},"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],"Image":"sha256:896f6e65107acffcae30fe593503aebf407fc30ad4566a8db04921dbfb6c0721","StdinOnce":false,"AttachStdout":false,"WorkingDir":"","Volumes":null,"OnBuild":null,"Domainname":"","AttachStdin":false,"Tty":false,"Cmd":["/bin/sh","-c","#(nop) ","CMD [\"sh\"]"]},"Container":"197cb47b0e98a00daefcb62c5fa84634792dd22f11aa29bc95fdd4e10d654d30","RootFS":{"Type":"layers","Layers":["sha256:adab5d09ba79ecf30d3a5af58394b23a447eda7ffffe16c500ddc5ccb4c0222f"]},"RepoTags":["busybox:latest"],"DockerVersion":"18.06.1-ce","Author":"","Id":"sha256:d8233ab899d419c58cf3634c0df54ff5d8acc28f8173f09c21df4a07229e1205"},"message":"OK","status":200}
				//LOGD("Resp: " + JSON.stringify(data));
				var cmd = '';
				if ( data.data.Config && data.data.Config.Entrypoint && (data.data.Config.Entrypoint.length > 0)  ) {
					cmd = data.data.Config.Entrypoint.join(' ') + ' ';
				}
				if ( data.data.Config && data.data.Config.Cmd ) {
					//$('#Cmd').val(data.data.Config.Cmd.join(' '));
					cmd += data.data.Config.Cmd.join(' ');
				}
				$('#Cmd').val(cmd);
			});
			return false;	
		}
		
		function container_create() {
			var data = $('#frm1').serialize();
			LOGD('Create container: ' + data);
			
			// validate: dismiss modal1
			//if ( $('#c_Image').val() != '' ) {
				$('#btnCloseModal1').click();
				// send data
				var url 	= '<%=contextPath%>/Docker?op=CreateContainer&node=<%=node%>';
				var posting = $.post( url , data);
				
				// get results 
				posting.done(function( data ) {
					// {"message":"Requested CPUs are not available - requested 0,1, available: 0","status":400}
					// {"message":"OK","status":200,"data":{"Id":"e6ef0d65a019e5575514b10c5fb5a194c9212c404d25b00ef9b6eb37b5908067","Warnings":["linux does not support CPU percent. Percent discarded.","Your kernel does not support Block I/O weight_device or the cgroup is not mounted. Weight-device discarded."]}}
					LOGD("Resp: " + JSON.stringify(data));
					var error	= ( data.status && (data.status >= 400)) ? true : false;	
					var type  	=  error ? 'danger' : 'info';
					
					if ( error && data.message ) {
						growl (data.message, type);
						return;
					}
					if ( data.data && data.data.Id) {
						growl ('Created ' + data.data.Id, type);
					}
					// warnings?
					if ( data.data && data.data.Warnings) {
						var html = '<ul>';
						for ( var i = 0 ; i < data.data.Warnings.length ; i++) {
							html += '<li>' + data.data.Warnings[i];
						}
						notify (html + '</ul>', 'warning');
					}
					// refresh
					$('#btnRefreshContainers').click();
				});
			//}
			return true;
		}
		</script>