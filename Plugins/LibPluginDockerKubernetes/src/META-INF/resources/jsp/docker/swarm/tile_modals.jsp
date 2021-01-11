<%@page import="com.cloud.docker.DockerParams"%>
<%
%>
	<!--  modal 1: Swarm Init -->	
	<form id="frm1" class="form-horizontal">
		<input type="hidden" id="action" name="action" value="init">
		<div id="modal1" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Initialize Swarm <small>Subtitle here</small></h3>
					</div>
					<div class="modal-body">
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Node Name</label>
							<div class="col-sm-10">
								<!-- onchange="nodes_onchange()" -->
								<select class="form-control node" id="selNode" name="selNode"></select>
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Advertise Address</label>
							<div class="col-sm-10">
								<input type="text" id="AdvertiseAddr" name="AdvertiseAddr" class="form-control" required="required" title="Host IP Address:Port" 
									placeholder="IP ADDRESS:2377" value=""/>
							</div>
						</div>

						
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<button class="btn btn-raised btn-primary" onclick="return swarmInit()">Go</button>
					</div>
					
				</div>
			</div>
		</div>
	</form>

	<!--  modal 2: Swarm Join method="post" action="view.jsp" -->	
	<form id="frm2" class="form-horizontal">
		<input type="hidden" id="action" name="action" value="join">
		<div id="modal2" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button id="btnCloseJoinModal" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Join a Node to the Swarm</h3>
					</div>
					<div class="modal-body">
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Node Name</label>
							<div class="col-sm-10">
								<select class="form-control node" id="selNode1" name="selNode1"></select>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Advertise Address</label>
							<div class="col-sm-10">
								<input type="text" id="AdvertiseAddr1" name="AdvertiseAddr1" class="form-control" required="required" title="Host IP Address:Port" 
									placeholder="IP ADDRESS:2377" value=""/>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Join as</label>
							<div class="col-sm-10">
								<select class="form-control" id="selJoinAs" name="selJoinAs">
									<option value="Worker">Worker</option>
									<option value="Manager">Manager</option>
								</select>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label">Swarm</label>
							<div class="col-sm-10">
								<select class="form-control" id="selSwarm" name="selSwarm">
								</select>
							</div>
						</div>
						<!-- 
						<div class="form-group">
							<label class="col-sm-2 control-label">Join Token</label>
							<div class="col-sm-10">
								<input type="text" name="JoinToken" class="form-control" required="required" title="Join Token" value=""/>
							</div>
						</div>
						-->
						<div class="form-group">
							<label class="col-sm-2 control-label">Manager Address</label>
							<div class="col-sm-10">
								<input type="text" id="RemoteAddrs" name="RemoteAddrs" class="form-control" required="required" title="Host IP Address:Port" 
									placeholder="IP ADDRESS:2377" value=""/>
							</div>
						</div>

						
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<button class="btn btn-raised btn-primary" onclick="return swarmJoin()">Go</button>
					</div>
					
				</div>
			</div>
		</div>
	</form>
	
	<!--  modal 3: Swarm Leave -->	
	<form class="form-horizontal">
		<input type="hidden" id="action" name="action" value="leave">
		<div id="modal3" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Leave Swarm</h3>
					</div>
					<div class="modal-body">
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Node Name</label>
							<div class="col-sm-10">
								<select class="form-control node" id="selNode2" name="selNode2"></select>
							</div>
						</div>

						
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<button onclick="return swarmLeave()" class="btn btn-raised btn-primary">Save</button>
					</div>
					
				</div>
			</div>
		</div>
	</form>
	
	<!-- modal 4 inspect swarms -->
	<div id="modal4" class="modal fade" tabindex="-1" role="dialog">
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
	<button id="btnInspect" data-toggle="modal" data-target="#modal4" style="display: none"></button>	
	
	<!-- modal 5 create service class="form-horizontal" -->
	<form id="frm5">
		<div id="modal5" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title">Create Service <small><a target="_blank" href="https://docs.docker.com/get-started/part3/">About Services</a></small></h3>
					</div>
					<div class="modal-body">
					
						<div class="form-group">
							<label class="col-sm-2 control-label">Image Name</label>
							<div id="image-box" class="col-sm-10">
								<input class="" id="<%=DockerParams.W3_PARAM_IMAGE%>" name="<%=DockerParams.W3_PARAM_IMAGE%>" required data-toggle="tooltip"
									pattern="[A-z0-9/_-]+" title="Image name (no spaces)." placeholder="Type to search Docker Hub...">
							</div>
						</div>

						<div class="form-group">
							<label class="col-sm-2 control-label">Service Name</label>
							<div class="col-sm-10">
								<input class="form-control"  name="<%=DockerParams.W3_PARAM_NAME%>" required data-toggle="tooltip"
									pattern="[A-z0-9_-]+" title="Service name (no spaces).">
							</div>
						</div>
						
						<div class="form-group">
							<label class="col-sm-2 control-label">Replicas</label>
							<div class="col-sm-10">
								<input class="form-control" name="<%=DockerParams.W3_PARAM_REPLICAS%>" required data-toggle="tooltip"
									pattern="\d+" title="Number of service instances." value="1">
							</div>
						</div>
						
						<!-- 
						<div class="form-group">
							<label class="col-sm-2 control-label">Image</label>
							<div class="col-sm-10">
								<select class="form-control" id="sel<%=DockerParams.W3_PARAM_IMAGE%>" name="<%=DockerParams.W3_PARAM_IMAGE%>" title="Image installed in the host"></select>
							</div>
						</div>
						-->
						<div class="form-group">
							<div class="col-sm-2"> 
								<label class="control-label">Command</label> 
								<!-- 
								<a href="#" onclick="return fetchArgs();" title="Click to fetch arguments from the image descriptor."><i class="material-icons">help</i></a>
								-->
							</div>
							<div class="col-sm-10">
								<input type="text" id="<%=DockerParams.W3_PARAM_COMMAND%>" name="<%=DockerParams.W3_PARAM_COMMAND%>" 
									class="form-control" title="Optional command arguments to execute: (e.g. date)" >
							</div>
						</div>
	
						<div class="form-group">
							<label class="col-sm-2 control-label">Port Bindings</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_PORTS%>" value="" placeholder="8080/tcp:8080,80/tcp:80"
									title="Optional comma separated list of GuestPort:HostPort: (e.g. 8080/tcp:8080,80/tcp:80,...)">
							</div>
						</div>
	
						<div class="form-group">
							<label class="col-sm-2 control-label">Mount Points</label>
							<div class="col-sm-10">
								<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_MOUNTS%>" value="" placeholder="/tmp:/tmp,/var/log:/var/log"
									title="Optional comma separated list of GuestMount:HostMount: (e.g. /tmp:/tmp,/var/log:/var/log,...)">
							</div>
						</div>
						<!-- TODO: volumes, labels  
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
						-->
					</div>
					<!-- modal body -->
					
					<div class="modal-footer">
						<button id="btnCloseModal5" type="button" class="btn btn-raised btn-default" data-dismiss="modal">Close</button>
						&nbsp;&nbsp;&nbsp;
						<button type="submit" class="btn btn-raised btn-primary" onclick="return service_create()">Create</button>
					</div>
					
				</div>
			</div>
		</div>
	</form>
	
	<script>
	function service_create () {
		var data 	= $('#frm5').serialize();
		var id		= '';
		
		LOGD('Create Service data: ' + data);
		serviceAction ("CreateService", id, data)
		
		// close modal
		$('#btnCloseModal5').click();
		
		// don't leave page
		return false;
	}
	</script>
		