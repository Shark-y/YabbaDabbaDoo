<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.docker.DockerParams"%>
<%
	final String contextPath 	= getServletContext().getContextPath();
	final String node 			= request.getParameter("name");
%>

		
		<!-- modal 3 inspect images/containers -->
		<div id="modal3" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div id="modal3_body" class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<!-- 
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						-->
						<h3 id="inspectTitle" class="modal-title"></h3>
					</div>
					<div class="modal-body">
						<div id="json-renderer"></div>
						<div id="div-json-editor">
							<input id="json-decode-b64" type="checkbox" onclick="return decodeb64(this)"> Toggle Base64
							<pre id="json-editor" style="width: 100%; height: 400px"></pre>
						</div>
					</div>
				</div>
			</div>
		</div>
		<button id="btnInspect" data-toggle="modal" data-target="#modal3" style="display: none" data-uk-modal="{target:'#modal3'}"></button>	
		

		<!-- modal 6 POD TERM: select container -->
		<div id="modal6" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog modal-sm uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<!-- 
						<button id="btnCloseModal6" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						-->
						<h3 class="modal-title">Select a Shell and Container</h3>
					</div>
					<div class="modal-body">
						<select id="shell" class="form-control">
							<option value="sh">Basic (sh)</option>
							<option value="/bin/bash">Bash (/bin/bash)</option>
						</select>
						<select id="container" class="form-control">
						</select>
						<a id="linkshell" target="_blank" style="display: none;">XTerm</a>
					</div>
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
						<button class="btn btn-raised btn-primary md-btn md-btn-primary" onclick="return shell_start()">Start</button>
					</div>
					
				</div>
			</div>
		</div>
		<button id="btnShell" data-toggle="modal" data-target="#modal6" style="display: none" data-uk-modal="{target:'#modal6'}"></button>
		
		<script>
		
		// Fires when the Terminal link is clicked from the pod table ctx menu
		function getPodTerm(pod, namespace, POD) {
			// [{"image":"us.gcr.io/acme-bots/cc_aes:latest","livenessProbe":{"failureThreshold":3,"timeoutSeconds":1,"periodSeconds":30,"successThreshold":1,"initialDelaySeconds":60,"httpGet":{"path":"/","scheme":"HTTP","port":8080}},"imagePullPolicy":"IfNotPresent","terminationMessagePolicy":"File","terminationMessagePath":"/dev/termination-log","name":"cc-aes","resources":{},"readinessProbe":{"failureThreshold":3,"timeoutSeconds":1,"periodSeconds":60,"successThreshold":1,"initialDelaySeconds":60,"httpGet":{"path":"/","scheme":"HTTP","port":8080}},"ports":[{"protocol":"TCP","containerPort":8080,"hostPort":8009}],"volumeMounts":[{"mountPath":"/var/run/secrets/kubernetes.io/serviceaccount","name":"default-token-22jz9","readOnly":true}]}]
			var containers = POD[pod].spec.containers;
			//LOGD('Term Pod: ' + pod +  ' NS:' + namespace + ' Containers:' + JSON.stringify(containers) );

			// fill container combo w/ container names
			$('#container').empty();
			$(containers).each(function () {
				var option = $('<option />');
     			option.attr('value', this.name).text(this.name);
     			$('#container').append(option);
			}); 
			
			// set href <a target="_blank" href="xterm/xterm.jsp?node=' + node + '&Id=' + full.Id + '">XTerm</a>
			$("#linkshell").attr("href", '../docker/xterm/xterm.jsp?node=' + node + '&pod=' + pod + '&namespace=' + namespace);
			$('#btnShell').click();		// open select shell modal
			
			return false;
		}
		
		// Fires when the Start btn is pressed on the select shell modal
		function shell_start() {
			// shell, container
			var shell 		= $('#shell').children("option:selected").val();
			var container 	= $('#container').children("option:selected").val();
			var href 		= $("#linkshell").attr("href") + '&Id=' + container + '&shell=' + shell;
			LOGD("Xterm href: " + href);
			
			// set link & click
			//$("#linkshell").attr("href", href);
			//$("#linkshell")[0].click();		// click anchor, will open a window using href
			window.open(href, '_blank');
			$('#btnCloseModal4').click();	// close modal
			
			return false;	
		}

		// fires when the b64 checkbox is clicked
		function decodeb64(cb) {
			var json = JSON.parse(editor.getValue());
			for(var key in json) {
				json[key] = cb.checked ? Base64.decode(json[key]) : Base64.encode(json[key]) ;
			}
			editor.setValue(JSON.stringify(json));
			return true;
		}
		</script>
		
		<!-- modal 7 add secret -->
		<div id="modal7" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog uk-modal-dialog-large">
				<div class="modal-content">
					<div class="modal-header">
						<!-- 
						<button id="btnCloseModal7" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						-->
						<h3 class="modal-title">Add Secret &nbsp;&nbsp;&nbsp;<small><a href="#" onclick="return templateGCR()">GCR</a></small></h3>
					</div>
					<div class="modal-body">
						<span id="secretStatusMsg"></span>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<div class="uk-width-1-4">
								<input id="json-decode-b64" type="checkbox" onclick="return modal7b64(this)" title="Click to encode/decode the data values of the secret."> Toggle Base64
							</div>
							<div class="uk-width-2-3">
								<input id="patch-sa-ips" type="checkbox" title="Check to use this secret for image pulls."> Set for image pull for account
								<input id="patch-account" type="text" value="default" style="width: 110px">
							</div>
						</div>
						
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Name</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input class="<%=SkinTools.cssInputClass()%>" id="secretName" name="secretName" required data-toggle="tooltip"
									pattern="[A-z0-9_\-\.]+" title="Secret name (no spaces).">
							</div>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Namespace</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input class="<%=SkinTools.cssInputClass() %>" id="secretNS" name="secretNS" required data-toggle="tooltip"
									pattern="[A-z0-9_\-\.]+" title="Namespace (no spaces)." value="">
							</div>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Type</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input type="text" id="secretType" name="secretType" class="<%=SkinTools.cssInputClass() %>" required="required" value=""/>
							</div>
						</div>
						<% for ( int i = 1 ; i < 5 ;  i++ ) { %>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<div class="col-sm-2">
								<label class="control-label">Data</label>
								<a href="#" title="Click to toggle file selection" style="" onclick="$('#divDataSel<%=i%>').toggle();$('#divDataValue<%=i%>').toggleClass('col-sm-6').toggleClass('col-sm-4')">[+]</a>
							</div>
							<div class="col-sm-4">
								<input type="text" id="secretDataKey<%=i%>" name="secretDataKey<%=i%>" class="form-control md-input" placeholder="Key<%=i%>" />
							</div>
							<div id="divDataValue<%=i%>" class="col-sm-6 uk-width-1-2">
								<input type="text" class="form-control md-input" id="secretDataVal<%=i%>" name="secretDataVal<%=i%>" placeholder="Val<%=i%>" />
							</div>
							<div id="divDataSel<%=i%>" class="col-sm-2" style="display: none;">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new md-btn md-btn-flat">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="filePwd" onchange="openFile(event, 'secretDataVal<%=i%>')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>											
						</div>
						<% } %>
					</div>
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
						<button class="btn btn-raised btn-primary md-btn md-btn-primary" onclick="return addSecret()">Save</button>
					</div>
					
				</div>
			</div>
		</div>
		
		<button id="btnModal7" data-toggle="modal" data-target="#modal7" style="display: none" data-uk-modal="{target:'#modal7'}"></button>
		
		<script>
		
		function modal7Show() {
			// always uncheck in show
			//$('#json-decode-b64').prop('checked', false);
			$('#btnModal7').click();
			return false;
		}
		
		// Modal7: fires when the GRC link is clicked
		function templateGCR() {
			$('#secretName').val('regcred');
			$('#secretType').val('kubernetes.io/dockerconfigjson');
			$('#secretDataKey1').val('.dockerconfigjson');
			return false;
		}
		
		// fires when th b64 checkbox encode/decode is clicked in 
		function modal7b64(chk) {
			for ( var i = 1 ; i < 5 ; i++) {
				var key = $('#secretDataKey' + i);
				var val = $('#secretDataVal' + i);
				
				if ( key && val && key.val() != '' && val.val() != '') {
					var b64 = chk.checked ? Base64.encode(val.val()) : Base64.decode(val.val()) ;
					val.val(b64);
				}
			}
			return true;
		}
		</script>
		
		<!-- modal 8 chart params -->
		<jsp:include page="tile_modal_chart_params.jsp" />
		
		
		<!-- modal 9 add namespace -->
		<div id="modal9" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<!-- 
						<button id="btnCloseModal9" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						-->
						<h3 class="modal-title">Add Namespace</h3>
					</div>
					<div class="modal-body">
						<span id="modal9StatusMsg"></span>
						
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Name</label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<input class="form-control md-input" id="modal9NSName" name="modal9NSName" required data-toggle="tooltip"
									pattern="[A-z0-9_\-\.]+" title="Type a namespace (no spaces).">
							</div>
						</div>
					</div>
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
						<button class="btn btn-raised btn-primary md-btn md-btn-primary" onclick="return modal9AddNS()">Save</button>
					</div>
					
				</div>
			</div>
		</div>
		<button id="btnModal9" data-toggle="modal" data-target="#modal9" style="display: none" data-uk-modal="{target:'#modal9'}"></button>
		
		<!-- modal 10 del namespace -->
		<div id="modal10" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<!-- 
						<button id="btnCloseModal10" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						-->
						<h3 class="modal-title">Delete Namespace</h3>
					</div>
					<div class="modal-body">
						<span id="modal10StatusMsg"></span>
						
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Name <i id="modal10spinner" class="fas fa-spinner fa-spin"></i></label>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<select id="modal10Namespace" class="form-control"></select>
							</div>
						</div>
					</div>
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
						<button class="btn btn-raised btn-primary md-btn md-btn-primary" onclick="return modal10DelNS()">Delete</button>
					</div>
					
				</div>
			</div>
		</div>
		<button id="btnModal10" data-toggle="modal" data-target="#modal10" style="display: none" data-uk-modal="{target:'#modal10'}"></button>
		
		<script>
		
		function modal10Show(basePath, node) {
			LOGD('Del NS: ' + basePath + ' Node:' + node);
			loadNamespaces (basePath, node, 'modal10Namespace', function () { $('#modal10spinner').hide() });

			$('#btnModal10').click();
			return false;
		}
		
		function modal9AddNS() {
			var name 	= $('#modal9NSName').val();
			var node	= '<%=node%>';
			
			LOGD('Cretate NS ' + name + ' Node:' + node);
			
			createNamespace ('../..', node, name, function (json) { 
				// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"namespaces \\\"foo\\\" already exists\",\"reason\":\"AlreadyExists\",\"details\":{\"name\":\"foo\",\"kind\":\"namespaces\"},\"code\":409}\n","status":409}
				if ( json.status >= 400) {
					var obj = JSON.parse(json.message);
					modal9SetStatus (obj.message, 'danger');
				}
				else {
					modal9SetStatus ( json.data.metadata.name + ' ' + json.message);
				}
			});
			return false;
		}

		function modal9SetStatus (text, color) {
			modalSetStatus ('modal9StatusMsg', text, color, '<%=SkinTools.getSkinName()%>');
		}

		function modal10DelNS() {
			var	name		= $('#modal10Namespace').find('option:selected').val();
			var node		= '<%=node%>';
			var basePath	= '../..';
			
			LOGD('Del NS ' + name + ' Node:' + node);
			
			delNamespace (basePath, node, name, function (json) { 
				// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"namespaces \\\"foo\\\" already exists\",\"reason\":\"AlreadyExists\",\"details\":{\"name\":\"foo\",\"kind\":\"namespaces\"},\"code\":409}\n","status":409}
				if ( json.status >= 400) {
					var obj = JSON.parse(json.message);
					modalSetStatus ('modal10StatusMsg', obj.message, 'danger', '<%=SkinTools.getSkinName()%>');
				}
				else {
					modalSetStatus ('modal10StatusMsg', json.data.metadata.name + ' ' + json.message, 'info', '<%=SkinTools.getSkinName()%>');
					$('#modal10spinner').show();
					loadNamespaces (basePath, node, 'modal10Namespace', function () { $('#modal10spinner').hide() });
				}
			});
		}
		</script>