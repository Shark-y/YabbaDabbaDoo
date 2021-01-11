<%@page import="com.cloud.docker.DockerNode"%>
<%@page import="com.cloud.kubernetes.K8SNode"%>
<%@page import="com.cloud.docker.Docker"%>
<%@page import="com.cloud.kubernetes.Kubernetes"%>
<%@page import="com.cloud.repo.PrivateRepo"%>
<%@page import="com.cloud.docker.DockerParams"%>
<%
	final String node 		= request.getParameter("node");
	final String provider 	= request.getParameter("provider");

	final DockerNode dnode	= Docker.get(node);
	
	// Look for node name and node IP in kubernetes
	final K8SNode knode		= dnode != null ? Kubernetes.getByAddr(dnode.getHost()) : null;
	final boolean isK8S		= provider != null && provider.contains("kube"); // Kubernetes.contains(node) || (knode != null );
	final boolean isDocker	= provider != null && provider.equalsIgnoreCase("docker"); //Docker.contains(node);
	
	//System.out.println("[MODAL-REPOS] DOCKER=" + isDocker + " (" + dnode + ") K NODE=" + knode + " D-IP=" + dnode.getHost());
%>
		<!-- MODAL REPO-BROWSER -->
		<div id="modal5" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>

						<h3 class="modal-title">Repo Browser &nbsp;&nbsp;<a href="#" title="Toggle add a new Repo." onclick="$('.repoAdd').toggle();$('.repoBrowse').toggle();return false;"><i class="fas fa-plus-square"></i></a>
							 &nbsp;&nbsp;<a title="Install selected Tags." href="#" onclick="return tag_install();"><i class="fas fa-cloud-download-alt"></i></a>
							 &nbsp;&nbsp;<span class="repoAdd"><a title="Save Repo." href="#" onclick="return repo_save();"><i class="fa fa-save"></i></a> </span>
							 <% if ( isK8S) { %>
							 &nbsp;&nbsp;<i id="modal5spinner" class="fas fa-spinner fa-spin"></i>
							 <% } %>
						</h3>
					</div>
					<div class="modal-body">
						
						<span id="statusMsg"></span>
			
						<div class="form-group repoBrowse">
							<label class="col-sm-2 control-label">Name</label>
							<div class="col-sm-<%=(isK8S ? 4 : 10)%>">
								<select class="form-control" name="sel_name" id="sel_name" onchange="repo_onchange()">
								</select>
							</div>
							<% if ( isK8S) { %>
							<label class="col-sm-2 control-label">Namespace</label>
							<div class="col-sm-4">
								<select class="form-control" name="sel_namespace" id="sel_namespace">
								</select>
							</div>
							<% } %>
						</div>

						<form id="frm5">
						<div class="form-group repoAdd">
							<label class="col-sm-2 control-label">Type</label>
							<div class="col-sm-10">
								<select class="form-control" name="rb_repoType" id="rb_repoType" onchange="return repo_type_onchange()">
									<% if ( isDocker) { %>
									<option value="<%=PrivateRepo.RepoType.DOCKER%>"><%=PrivateRepo.RepoType.DOCKER%></option>
									<option value="<%=PrivateRepo.RepoType.GOOGLE%>"><%=PrivateRepo.RepoType.GOOGLE%></option>
									<% } %>
									<% if ( isK8S) { %>
									<option value="<%=PrivateRepo.RepoType.HELM%>"><%=PrivateRepo.RepoType.HELM%></option>
									<% } %>
								</select>
							</div>
						
						</div>
						<div class="form-group repoAdd">
							<label class="col-sm-2 control-label">Name</label>
							<div class="col-sm-10">
								<input class="form-control" id="rb_repoName" name="rb_repoName" required data-toggle="tooltip"
									pattern="[A-z0-9_\-/:\.]+" title="Image name (no spaces).">
							</div>
						</div>
						<div class="form-group repoAdd">
							<label class="col-sm-2 control-label">Url</label>
							<div class="col-sm-10">
								<input class="form-control" id="rb_repoUrl" name="rb_repoUrl" required data-toggle="tooltip"
									pattern="[A-z0-9_\-/:\.]+" title="Image name (no spaces)." value="">
							</div>
						</div>
						<div class="form-group repoAdd">
							<label class="col-sm-2 control-label">User</label>
							<div class="col-sm-10">
								<input type="text" id="rb_repoUser" name="rb_repoUser" class="form-control" title="Username: (required for private images)" placeholder=""  required="required"/>
							</div>
						</div>
						<div class="form-group repoAdd">
							<div class="col-sm-2">
								<label class="control-label">Password</label>
								<a href="#" title="Click to toggle file selection" style="" onclick="$('#divPwdSel5').toggle();$('#divPwd5').toggleClass('col-sm-10').toggleClass('col-sm-8')">[+]</a>
							</div>
							<div id="divPwd5" class="col-sm-10">
								<input type="text" class="form-control" id="rb_repoPwd" name="rb_repoPwd"  
									title="Required password (for private images)">
							</div>
							<div id="divPwdSel5" class="col-sm-2" style="display: none;">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="filePwd" onchange="openFile(event, 'rb_repoPwd')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>
						</div>
						</form>
						
							<table id="tblTags" class="table m-n compact" style="width: 100%">
								<thead>
									<tr>
										<th>Tag</th><th>Details</th>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>	

					</div>
					<!-- modal body -->

				</div>
			</div>
		</div>
		<!-- end modal  (repo-browser) -->

		<button id="btnModal5" data-toggle="modal" data-target="#modal5" style="display: none" data-uk-modal="{target:'#modal5'}"></button>	
	
	<script type="text/javascript">
	
		var TBL_TAGS;
		var NODE	= '<%=node%>';

		// get repo names
		function fetchRepos(clear) {
			if ( clear) {
				$('#sel_name').empty();
			}
			var posting = $.get( '../../Repo?op=getRepos' );
			
			posting.done(function( data ) {
				// {"message":"OK","status":200,"data":[{"name":"cloud/connector","type":"DOCKER","user":"cloud","password":"Thenewcti1","url":"https://cloud.docker.com/api/repo/v1/inspect/v2/"},{"name":"cloud-bots","type":"GOOGLE","user":"_json_key","password":"","url":"https://us.gcr.io/v2/"}]}
				//LOGD("Repos: " + JSON.stringify(data));
				var isDocker 	= <%=isDocker%>;
				var isHelm		= <%=isK8S%>;
				$.each(data.data, function (key, entry) {
					if ( (entry.type == 'DOCKER' || entry.type == 'GOOGLE') && !isDocker) {
						return;
					}
					if ( entry.type == 'HELM' && !isHelm) {
						return;
					}
					$('#sel_name').append($('<option></option>').attr('value', entry.name + ':' + entry.type).text(entry.name + ' (' + entry.type + ')' ));
				});
				
				fetchTags();
			});
		}

		// Get tags for a repo name
		function fetchTags() {
			// Repo name - NAME:TYPE
			var nameType	= $('#sel_name').find('option:selected').val();
		
			// 6/4/2020 Uncaught TypeError: Cannot read property 'split' of undefined (nameType)
			if ( !nameType ) {
				LOGW('FetchTags: Unable to obtain a selected value for SELECT(sel_name): ' + nameType + '. Abort');
				return;
			}
			var name		= nameType.split(':')[0];
			var type		= nameType.split(':')[1];
			var posting 	= $.get( '../../Repo?op=getTags&name=' + name );
			LOGD('Fetch tags: Name=' + name + ' type: ' + type + ' nameType: ' + nameType );

			posting.done(function( data ) {
				// LOGD(name + " tags: " + JSON.stringify(data));
				// GOOGLE: {"child":["agentaggregator","ca_rntcisco","customeradministration","customeraggregator","dialogflow"],"manifest":{},"name":"cloud-bots","tags":[]}
				// DOCKER: {"name":"cloud/connector","tags":["ca_c1msaes","ca_c1mscisco","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen-scb","cc_gen","cc_ucce"]}
					/* HELM { "entries": {
					  "ca-rntcisco": [{
					   "maintainers": [{
					    "name": "vsilva",
					    "email": "vsilva@convergeone.systems"
					   }],
					   "appVersion": "1",
					   "urls": ["https://charts_cloud.storage.googleapis.com/ca-rntcisco-20190603.tgz"],
					   "apiVersion": "v1",
					   "created": "2019-06-05T14:55:44.230717618-04:00",
					   "digest": "6da66a05bb8362c630f0ff4acc9a8441f34604cec2efe8db2e88de42f1bc73f2",
					   "icon": "https://www.convergeone.com/hubfs/Convergeone_September2017_Theme/Images/C1-logo_35H.png",
					   "name": "ca-rntcisco",
					   "description": "Cloud Connector ca_rntcisco",
					   "version": "20190603",
					   "home": "https://www.convergeone.com/"
					  }],
					  "cc-finesse": [{
						...
					  }]
					 },
					 "apiVersion": "v1",
					 "generated": "2019-06-05T14:55:44.226549632-04:00"
					} */
				if ( data.status >= 400) {
					setStatus(name + ': ' + data.message, 'danger');
					return;
				}
				TBL_TAGS.clear();
				var root	= data.data;
				var tags 	= type == 'GOOGLE' ? root.child : type == 'DOCKER' ? root.tags : root.entries;
				
				// DOCKER/GOOGLE
				var idx = 0;
				$.each(tags /*data.data*/, function (key, entry) {
					if ( type == 'DOCKER' || type == 'GOOGLE') {
						LOGD(name + " tags: " + tags );
						var html 	= '<input id="chkTag' + key + '" type="checkbox" value="' + entry + '">' + ' ' + entry;
					}
					else {
						// key = ca-rntcisco
						// entry = [{"maintainers":[{"name":"vsilva","email":"vsilva@convergeone.systems"}],"appVersion":"1","urls":["https://charts_cloud.storage.googleapis.com/ca-rntcisco-20190603.tgz"],"apiVersion":"v1","created":"2019-06-05T14:55:44.230717618-04:00","digest":"6da66a05bb8362c630f0ff4acc9a8441f34604cec2efe8db2e88de42f1bc73f2","icon":"https://www.convergeone.com/hubfs/Convergeone_September2017_Theme/Images/C1-logo_35H.png","name":"ca-rntcisco","description":"Cloud Connector ca_rntcisco","version":"20190603","home":"https://www.convergeone.com/"}]
						// LOGD (idx + ' k=' + key + ' entry=' + JSON.stringify(entry));
						var desc 	= entry[0].description;
						var html 	= '<input id="chkTag' + idx + '" type="checkbox" value="' + key + '"> ' /*+ key + ' ' */ + desc;
						idx++;
					}
					TBL_TAGS.row.add ( [html , ''] );
				});
				TBL_TAGS.draw();
			});
		}
	
		function tag_install() {
			// NAME:TYPE
			var nameType	= $('#sel_name').find('option:selected').val();
			var repo		= nameType.split(':')[0];
			
			if ( NODE == '') { setStatus ('A Node is required', 'danger'); return; };
			
			TBL_TAGS.rows().every( function (rowIdx, tableLoop, rowLoop) {
				var id 	= 'chkTag' + rowIdx; 
				var d 	= this.data();
				//LOGD( rowIdx + ' ' + d);
				
				if ( $('#' + id).is(':checked')) {
					var image 	= $('#' + id).val();
					var ns		= $('#sel_namespace').find('option:selected').val();
					
					LOGD('Install ' + image + ' ns: ' + ns);
					TBL_TAGS.cell(rowIdx, 1).data('<i class="fas fa-spinner fa-spin"></i> Installing ' + image + '(' + ns + ') @ ' + NODE + '.').draw();
					
					var posting = $.post( '../../Repo?op=install&image=' + image + '&repo=' + repo + '&node=' + NODE + '&ns=' + ns);
					posting.done(function( data ) {
						LOGD('Install ' + image + " Resp: " + JSON.stringify(data));
						// TBL_TAGS.cell(rowIdx, 1).data(JSON.stringify(data));
						// {"message":"OK","status":200,"data":[{"status":"Trying to pull repository us.gcr.io/cloud-bots/agentaggregator ... "},{"status":"Pulling repository us.gcr.io/cloud-bots/agentaggregator"},{"errorDetail":{"message":"unauthorized: authentication required"},"error":"unauthorized: authentication required"}]}
						// {"message":"Install ca-c1mscisco from cloud-charts @ KubeMaster: Unimplemented repo type HELM for cloud-charts","status":500}
						// (HELM) {"message":"OK","status":200, "data" : "HELM install stdout"}
						if ( data.status >= 400 ) {
							table_setText (rowIdx, 1, data.message, 'red');
						}
						if ( Array.isArray(data.data) ) {
							for (var i = 0 ; i < data.data.length ;i++) {
								var obj = data.data[i];
								// look 4 errors
								if ( obj.error) {
									table_setText (rowIdx, 1, obj.error, 'red');
								}
								else if (obj.status) {
									table_setText (rowIdx, 1, obj.status);
								}
							}
						}
						else {
							if ( data.status < 400) {
								// HELM: data.data has the install STDOUT
								table_setText (rowIdx, 1, 'Installed ' + image + ' @ ' + NODE + '.');
							}
						}
						// Draw once all updates are done
						TBL_TAGS.draw();
					});
				}
			});
		}
		
		// set colorized text in the table @ (row,col)
		function table_setText (row, col, data, color ) {
			color = color || 'black';
			TBL_TAGS.cell(row, col).data('<font color="' + color + '">' + data + '</font>');
		}
		
		// fires when the repo name combo changes
		function repo_onchange() {
			fetchTags();
		}
		
		// fires when the repo type combo changes
		function repo_type_onchange() {
			var type = $('#rb_repoType').find('option:selected').val();
			
			if ( type == 'GOOGLE') $('#rb_repoUrl').val('https://us.gcr.io/v2/');
			if ( type == 'DOCKER') $('#rb_repoUrl').val('https://cloud.docker.com/api/repo/v1/inspect/v2/');
		}
		
		// init the tags data table		
		function initTagsDataTable () {
			TBL_TAGS = $("#tblTags").DataTable (); // {paging: false}
		}
		
		// invoke on doc ready
		function initializeReposDialog() {
			// load repo combo
			fetchRepos(false);
			// init data table
			initTagsDataTable ();
			// hide login stuff
			$('.repoAdd').toggle();
			// repo types
			repo_type_onchange();
		}
		
		// fires when the save icon is clicked
		function repo_save() {
			var data 	= $('#frm5').serialize();
			
			// HELM, GOOGLE, DOCKER
			var type	= $('#rb_repoType').find('option:selected').val();
			
			// HELM: User/pwd optional
			var ids 	= type == 'HELM' ? ['#rb_repoName', '#rb_repoUrl'] :  ['#rb_repoName', '#rb_repoUrl', '#rb_repoUser', '#rb_repoPwd'];
			LOGD('type=' + type + ' ids:' + JSON.stringify(ids));
			
			// user/pwd not required for HELM
			for ( var i = 0 ; i < ids.length; i ++) {
				if ( !$.trim($(ids[i]).val()) ) {
					setStatus('Missing required field ' + ids[i] + '.', 'danger');
					return;
				}
			}
			LOGD('REPO Save ' + data);
			var posting = $.post( '../../Repo?op=saverepo&node=' + NODE , data);
			
			posting.done(function( data ) {
				var name 		= $('#repoName').val();
				LOGD("Save " + name + ": " + JSON.stringify(data));
				
				// setStatus(name + ' ' + data.message, data.status >= 400 ? 'danger' : 'info');
				if ( data.status < 400) {
					setStatus(data.message, 'info');
					fetchRepos(true);
				}
				else {
					setStatus(name + ' ' + data.message, 'danger');
				}
			});
			
			return false;
		}
		
		/**
		 * Use this one for a file is selected from an INPUT type=file: <input type="file" id="filePwd" onchange="openFile(event, 'password')">
		 */ 
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
		
		function setStatus (text, color) {
			color = color || 'info'; //'black';
			//var html = '<font color="' + color + '">' + text + '</font>';
			var html = '<div class="alert alert-dismissable alert-' + color + '">' + text + '<button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button></div>' ;
			$('#statusMsg').html(html);
		}
		
		function modal5Open (basePath) {
			basePath 	= basePath || '../../';
			var node 	= '<%=node%>';
			
			loadNamespaces (basePath, node, 'sel_namespace', function () { $('#modal5spinner').hide() });
			
			$('#btnModal5').click();
			return false;
		}
	</script>										
