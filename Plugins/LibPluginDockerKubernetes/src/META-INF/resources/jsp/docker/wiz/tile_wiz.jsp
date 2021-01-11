<%@page import="com.cloud.docker.DockerParams"%>

				<!-- Docker run wizard -->
				<form id="runwizard" class="form-horizontal">
						<fieldset title="Step 1">
							<legend>Select a node</legend>
							
							<div class="form-group">
								<label for="fieldname" class="col-md-3 control-label">Node</label>
								<div class="col-md-6">
									<!-- multiple="multiple"  -->
									<select id="selNodes" name="selNodes" size="2" class="form-control nodes" style="height: 150px;" 
										required="required"></select>
								</div>
							</div>
							
						</fieldset>
						
						<fieldset title="Step 2">
							<legend>Select Image</legend>
							<div class="form-group">
								<label for="fieldnick" class="col-md-3 control-label">Image</label>
								<div class="col-md-6" id="image-box">
									<!--  class="form-control" -->
									<input id="<%=DockerParams.W3_PARAM_IMAGE%>"  
										name="<%=DockerParams.W3_PARAM_IMAGE%>" type="text" required placeholder="Search Docker Hub...">
								</div>
							</div>
							<div class="form-group">
								<label for="fieldnick" class="col-md-3 control-label">Tag</label>
								<div class="col-md-6" id="image-box">
									<!--  class="form-control" -->
									<input id="<%=DockerParams.W3_PARAM_TAG%>" class="form-control"
										name="<%=DockerParams.W3_PARAM_TAG%>" type="text" value="latest">
								</div>
							</div>
							<div class="form-group">
								<label class="col-sm-3 control-label">Username</label>
								<div class="col-sm-6">
									<input type="text" name="<%=DockerParams.W3_PARAM_USERNAME%>" class="form-control" title="Optional username: (for private images)" placeholder="" value=""/>
								</div>
							</div>
	
							<div class="form-group">
								<label class="col-sm-3 control-label">Password</label>
								<div class="col-sm-6">
									<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_PASSWORD%>" value="" 
										title="Optional password (for private images)">
								</div>
							</div>
						</fieldset>
						
						<fieldset title="Step 3">
							<legend>Select runtime parameters</legend>

							<div class="form-group">
								<label class="col-sm-3 control-label">Environment</label>
								<div class="col-sm-6">
									<input type="text" name="<%=DockerParams.W3_PARAM_ENV%>" class="form-control" title="Optional comma separated environment variables: VAR1=VAL1,VAR2=VAL2,..." placeholder="VAR1=VAL1,VAR2=VAL2,..." />
								</div>
							</div>
							<div class="form-group">
								<label class="col-sm-3 control-label">Command</label> 
								<div class="col-sm-6">
									<input type="text" id="<%=DockerParams.W3_PARAM_CMD%>" 
										name="<%=DockerParams.W3_PARAM_CMD%>" class="form-control" 
										title="Command arguments to execute: (e.g. date) or blank to extract from image" placeholder="Leave blank to extract from image." />
								</div>
							</div>
	
							<div class="form-group">
								<label class="col-sm-3 control-label">Exposed Ports</label>
								<div class="col-sm-6">
									<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_EXPOSEDPORTS%>" value="" placeholder="22/tcp,1024/udp,..."
										title="Optional comma separated list of ports to expose in the containder: (e.g. 22/tcp,...)">
								</div>
							</div>
							
							<div class="form-group">
								<label class="col-sm-3 control-label">Port Bindings</label>
								<div class="col-sm-6">
									<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_PORTBINDINGS%>" value="" placeholder="8080/tcp:8080,80/tcp:80"
										title="Optional comma separated list of GuestPort:HostPort: (e.g. 8080/tcp:8080,80/tcp:80,...)">
								</div>
							</div>
	
							<div class="form-group">
								<label class="col-sm-3 control-label">Mount Points</label>
								<div class="col-sm-6">
									<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_BINDS%>" value="" placeholder="/tmp:/tmp,/var/log:/var/log"
										title="Optional comma separated list of GuestMount:HostMount: (e.g. /tmp:/tmp,/var/log:/var/log,...)">
								</div>
							</div>
							
							<div class="form-group">
								<label class="col-sm-3 control-label">Volumes</label>
								<div class="col-sm-6">
									<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_VOLUMES%>" value="" placeholder="/volumes/data"
										title="Optional comma separated list of volumes: (e.g. /volumes/data,/volumes/data1,...)">
								</div>
							</div>
							<div class="form-group">
								<label class="col-sm-3 control-label">Labels</label>
								<div class="col-sm-6">
									<input type="text" class="form-control" name="<%=DockerParams.W3_PARAM_LABELS%>" value="" placeholder="com.example.vendor:ACME,com.example.version:1.0,..."
										title="Optional comma separated list of GuestMount:HostMount: (e.g. com.example.vendor:ACME,com.example.version:1.0)">
								</div>
							</div>
							
						</fieldset>
						<fieldset title="Step 4">
							<legend>Submit</legend>
							<div id="terminal"></div>
						</fieldset>
						
						<input type="submit" class="finish btn-success btn" value="Submit" />				
					</form>

