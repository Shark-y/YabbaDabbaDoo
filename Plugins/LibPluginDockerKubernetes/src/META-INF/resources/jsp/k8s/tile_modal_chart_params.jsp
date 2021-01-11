		<%@page import="com.cloud.console.SkinTools"%>

		<!-- modal 8 Get HELM chart params  -->
		<div id="modal8" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div id="modal8Dlg" class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<!--  
					<div class="modal-header <%=SkinTools.cssFormGroupClass() %>">
						<h3 id="modal8Title" class="modal-title"><span id="modal8ChartPref"></span> <span id="modal8ChartId"></span> <span id="modal8ChartVer"></span>&nbsp;&nbsp;&nbsp;<small><span id="modal8ChartHelp"></span></small> <i id="modal8spinner" class="fas fa-spinner fa-spin"></i></h3>
						<input type="text" id="modal8TxtChart" class="form-control md-input" placeholder="Type a chart repo/chart name here..." style="display: none">
					</div>
					-->
					<div class="modal-body">
						<span id="modal8StatusMsg" style="display: none;"></span>
						<input id="modal8RepoUrl" type="hidden" />
						<div class="modal-header <%=SkinTools.cssFormGroupClass() %>">
							<h3 id="modal8Title" class="modal-title">
								<span id="modal8ChartPref"></span> <span id="modal8ChartId"></span> <span id="modal8ChartVer"></span>&nbsp;&nbsp;&nbsp;<small><span id="modal8ChartHelp"></span></small> <i id="modal8spinner" class="fas fa-spinner fa-spin uk-icon-spinner uk-icon-spin"></i>
							</h3>
							<input type="text" id="modal8TxtChart" class="form-control md-input" placeholder="Type a chart repo/chart name here..." style="display: none">
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<span id="modal8ChartDesc"></span>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<div class="<%=SkinTools.cssFormGroupLabelClass()%>">
								<label class="control-label"><a href="#" title="Click to add a namespace" onclick="togglens()">Namespace</a></label>
							</div>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<select id="modal8Namespace" class="form-control"></select>
								<div id="modal8DivNS" style="display: none" class="nsAdd">
									<input type="text" id="modal8TxtNamespace" class="form-control" placeholder="Type a name and click save..." required="required">
								</div>
							</div>
						</div>
						<div class="<%=SkinTools.cssFormGroupClass() %>">
							<div class="<%=SkinTools.cssFormGroupLabelClass()%>">Parameters
							</div>
							<div class="<%=SkinTools.cssFormGroupLabelClass()%>">
								<input type="radio" id="text" name="kind" value="text" onclick="setType(this)" checked="checked"><label for="text">Text</label>
							</div>
							<div class="col-sm-4">
								<input type="radio" id="yaml" name="kind" value="yaml" onclick="setType(this)">
								<label for="yaml">YAML 
								&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="#" title="Click to fetch YAML." onclick="return fetchYaml()">Fetch</a>
								</label>
								
							</div>
							<div id="divYAMLSel" class="col-sm-4">
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new">Browse</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="fileYaml" onchange="modal8OpenFile(event, 'modal8ChartParams')">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a> 
								</div>
							</div>								
							
						</div>
						<textarea id="modal8ChartParams" rows="5" class="form-control TEXT md-input" placeholder="key1=value1,key2=value2,..."></textarea>
						
						<pre class="YAML" id="modal8Editor" style="width: 100%; height: 300px; display:none"></pre>
					</div>
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
						<button class="btn btn-raised btn-primary md-btn md-btn-primary" data-dismiss="modal" onclick="helm_params_click()"><i id="modal8InstallSpinner" class="uk-icon-spinner uk-icon-spin" style="color: white"></i> Install</button>
					</div>
				</div>
			</div>
		</div>
		<button id="btnModal8" data-toggle="modal" data-target="#modal8" style="display: none" data-uk-modal="{target:'#modal8'}"></button>	
		
		<script>

		var m8editor = ace.edit("modal8Editor");
		m8editor.session.setMode("ace/mode/yaml");

		// fetrch values.yaml GET http://localhost:9080/CloudClusterManager/K8S?op=HelmShowChart&node=KubeFoghornLeghorn&chart=convergeone/cc-cisco
		function fetchYaml () {
			// get the chart from the SPAN or INPUT box depending on visibility
			var id 			= $('#modal8ChartId').is(":visible") ? $('#modal8ChartId').html() : $('#modal8TxtChart').val();
			var repoUrl		= $('#modal8RepoUrl').val();
			
			LOGD('Fetch YAML node: ' + _node + ' chart:' + id + ' repoUrl:' + repoUrl);
			
			var posting = $.get(url + '&op=HelmShowChart&node=' + _node + '&chart=' + id + '&repoUrl=' + repoUrl);
			$('#modal8spinner').show();
			
			posting.done(function( data ) {
				$('#modal8spinner').hide();
				if ( data.status >= 400) {
					modal8SetStatus (data.message, 'danger');
					return;
				}
				m8editor.setValue(data.values);									
			});
			return false;
		}
		
		function setType (input) {
			if ( input.value == 'yaml') {
				$('#modal8Editor').show();
				$('#modal8ChartParams').hide();
				if ( m8editor.getValue() == '') {
				}
				$('#modal8Dlg').addClass('uk-modal-dialog-large');
			}
			else {
				$('#modal8Editor').hide();
				$('#modal8ChartParams').show();
				$('#modal8Dlg').removeClass('uk-modal-dialog-large');
			}
		}
		
		var _basePath;
		var _node;

		function togglens() {
			$('#modal8Namespace').toggle();
			$('.nsAdd').toggle();
			
			if ( $("#modal8Namespace").is(":visible")) {
				$('#modal8spinner').show();
				loadNamespaces (_basePath, _node, 'modal8Namespace', function () { $('#modal8spinner').hide() });
			}
		}
		
		function create_ns() {
			var name = $('#modal8TxtNamespace').val();
			LOGD('Cretate NS ' + name + ' Node:' + _node);
			
			createNamespace (_basePath, _node, name, function (json) { 
				// {"data":{"items":[]},"message":"{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"namespaces \\\"foo\\\" already exists\",\"reason\":\"AlreadyExists\",\"details\":{\"name\":\"foo\",\"kind\":\"namespaces\"},\"code\":409}\n","status":409}
				if ( json.status >= 400) {
					var obj = JSON.parse(json.message);
					modal8SetStatus (obj.message, 'danger');
				}
				else {
					modal8SetStatus ( json.data.metadata.name + ' ' + json.message);
				}
			});
			return false;
		}
		
		function modal8SetStatus (text, color) {
			modalSetStatus('modal8StatusMsg', text, color, '<%=SkinTools.getSkinName()%>');
		}

		// Fires when the install btn is clicked
		function helm_params_click() {
			var id 		= $('#modal8ChartId').html();
			var ver 	= $('#modal8ChartVer').html();
			var text 	= $('#text').prop("checked");
			var params	= text 
						? encodeURI($('#modal8ChartParams').val())
						: m8editor.getValue() ;
			var	ns		= $('#modal8Namespace').find('option:selected').val();
			var repoUrl	= $('#modal8RepoUrl').val();
			var desc	= $('#modal8ChartDesc').html();
			
			if ( id == '') {
				// use text box
				id = $('#modal8TxtChart').val();
				if ( id == '') {
					modal8SetStatus ('A chart id is required.', 'danger');
					//growl('A chart id is required.', 'danger');
					return;
				}
			}
			LOGD('Install chart ' + id + ' v: ' + ver + ' ns:' + ns + ' text: ' + text + ' repo:' + repoUrl); // + ' param:' + params
			$('#modal8InstallSpinner').show();

			// search box (if any)
			var app = $('#profile_card_selected_app').val() ? JSON.parse($('#profile_card_selected_app').val()) : null;

			if ( text ) {
				// using the params input text k1=v1,k2=v2,...
				// hubUrl, chart, version, advanced, params, ns, icon, repo
				hub_install( { hubUrl: (app ? app.hubUrl : ''), chart: id, version: ver, advanced: false, params: params, ns: ns
						, icon: (app ? app.icon : '')
						, desc: desc
						, repoUrl: repoUrl } );	
			}
			else {
				// selected YAML editor
				var ct 			= "text/yaml; charset=utf-8";
				var callback 	=  function (data) {
					$('#modal8InstallSpinner').hide();
					$('#btn_refresh_tblCharts').click();
					$('#btnRefreshCharts').click();
					
					if ( data.status >= 400) {
						modal8SetStatus(data.message, 'danger');
					}
				} 
				
				hubInstall /*helm_op*/ ( {method: 'POST', node: _node, operation: 'HelmInstall'
					, chart: id
					, version: ver
					, 'data': params
					, 'ns': ns
					, contentType: ct
					, icon: (app ? app.icon : '')
					, hubUrl: (app ? app.hubUrl : '')
					, desc: desc
					, 'repoUrl': repoUrl
					, callback: callback 
				}); 
			} 
		}
		
		/**
		 * Show the Chart install dialog
		 * @param basePath JSP base path relative to this tile
		 * @param node API server node
		 * @param chart Chart name: repo/name
		 * @param version Chart version
		 * @param hubUrl https://hub.helm.sh
		 * @param icon Optional icon path
		 * @param repoUrl Helm repository url.
		 */
		function modal8Show (args) {
			var basePath 	= args.basePath;
			var node		= args.node;
			var chart		= args.chart;
			var version		= args.version;
			var hubUrl		= args.hubUrl;
			var icon		= args.icon;
			var repoUrl		= args.repoUrl;
			
			icon	= icon			|| '';
			repoUrl	= repoUrl		|| '';
			
			LOGD('modal8Show Chart: ' + JSON.stringify(args));
			//LOGD('modal8Show basePath=' + basePath + ' node=' + node + ' chart=' + chart + ' Hub=' + hubUrl + ' icon=' + icon + ' repo:' + repoUrl + ' desc:' + chart.desc);
			
			$('#modal8InstallSpinner').hide();
			$('#modal8RepoUrl').val(repoUrl);
			$('#modal8ChartDesc').html(args.desc ? args.desc : '');
			
			// clear paramsYAML editor
			$('#modal8ChartParams').val('');
			m8editor.setValue('');
			
			loadNamespaces (basePath, node, 'modal8Namespace', function () { $('#modal8spinner').hide() });
			
			// if chart is empty show the chart intall box 
			if ( chart == '') {
				LOGD('modal8Show: Install empty chart');	
				$('#modal8Title').hide();
				$('#modal8TxtChart').show();
			}
			else {
				LOGD('modal8Show: Install chart ' + chart + ' v: ' + version + ' n:' + node + ' hub:' + hubUrl);	
				$('#modal8Title').show();
				$('#modal8TxtChart').hide();

				// set logo, chartid, version
				if ( icon != '') {
					$('#modal8ChartPref').html('<img src="' + hubUrl + icon + '" width="64" height="32"> ');
				}
				//$('#modal8ChartPref').html('<img src="' + hubUrl + '/api/chartsvc/v1/assets/' + chart + '/logo" height="32"> ');
				$('#modal8ChartId').html(chart);
				$('#modal8ChartVer').html(version);
				$('#modal8ChartHelp').html('<a target="_blank" href="' + hubUrl + '/charts/' + chart + '">MORE</a>' 
						+ '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a style="display:none" class="nsAdd" title="Create Namespace." href="#" onclick="return create_ns();"><i class="fa fa-2x fa-save material-icons md-36">save</i></a>');
			}
			
			$('#btnModal8').click();
			
			_basePath	= basePath;
			_node		= node;
		}
		
		// fires a file is selected from an INPUT type=file, sets the value to destination.
		function modal8OpenFile (event, destination ) {
		    var input 	= event.target;
		    var reader 	= new FileReader();
		    LOGD('openFile d:' + destination + " input:" + input + ' files[0]: ' + input.files[0]);
		    reader.onload = function() {
		    	var data 	= reader.result;
		    	//$('#' + destination).html(data);
		    	m8editor.setValue(data);
		    };
		    if ( input.files[0]) {
		    	reader.readAsText(input.files[0]);
		    }
		};
		
		function modal8IsVisible () {
			return $('#modal8').is(":visible");
		}
		
		</script>