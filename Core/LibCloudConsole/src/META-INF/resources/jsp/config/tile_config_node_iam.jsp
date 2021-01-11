<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.console.iam.IAM"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 
	final String contextPath 		= getServletContext().getContextPath();

%>

		<!--  modal6: IAM editor -->
		<div id="modal6" class="modal fade uk-modal" tabindex="-1" role="dialog">
				<div class="modal-dialog uk-modal-dialog">
					<div class="modal-content">
						<div class="modal-header">
							<%if ( SkinTools.SKIN_PATH.contains("clouds")) { %>
							<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
							<% } %>
							<h3>Identity Access Manager (IAM)
								<button class="btn btn-default md-btn md-btn-flat" data-toggle="tooltip" title="Add Identity" onclick="$('.iam_detail').toggle();return false;">+</button>
								&nbsp;&nbsp;&nbsp;<small style="display:none" class="azuread"><a target="_blank" href=" https://portal.azure.com/">Azure Console</a></small>
							</h3>
						</div>
						<div class="modal-body">
							<span id="modal6StatusMsg"></span>
							
							<!--  master -->
							<table id="tblIdentities" class="table iam_detail" style="width: 90%">
								<thead>
									<tr>
										<th>Name</th>
										<th>Type</th>
										<th>Domain</th>
										<th>Action</th>
									</tr>
								</thead>
								<tbody></tbody>
							</table>
							
							<!--  detail -->
							<table style="width: 100%;display: none" class="iam_detail">
							<tr class="form-group">
								<td><label class="control-label">Type</label></td>
								<td class="col-sm-10">
									<select	class="form-control" data-toggle="tooltip" id="iam_type" onchange="iam_onchange()">
									<% for (IAM.Types type : IAM.Types.values()) { %>
										<option value="<%=type.name()%>"><%=type.name()%></option>
									<% } %>
									</select>
								</td>
							</tr>
							<!--  WINDOWS-AD  col-sm-4 -->
							<tr class="form-group">
								<td><label class="control-label">Label</label></td>
								<td class="col-sm-10">
									<input id="iam_name" value="" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip" title="Common name shown in login window.">
								</td>
							</tr>
							<tr class="form-group winad">
								<td><label class="control-label">Name Server</label></td>
								<td class="col-sm-10">
									<input id="iam_winad_server" value="" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip" title="LDAP/Name server">
								</td>
							</tr>
							<tr class="form-group winad azuread">
								<td><label class="control-label">Domain</label></td>
								<td class="col-sm-10">
									<input id="iam_domain" value="" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip" title="Domain used for login">
								</td>
							</tr>
							
							<!-- Azure AD -->
							<tr class="form-group azuread" style="display:none">
								<td><label class="control-label">Tenant Id</label></td>
								<td class="col-sm-10">
									<input id="iam_azuread_tenant" value="" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip">
								</td>
							</tr>
							<tr class="form-group azuread" style="display:none">
								<td><label class="control-label">Client Id</label></td>
								<td class="col-sm-10">
									<input id="iam_azuread_client" value="" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip">
								</td>
							</tr>
							<tr class="form-group azuread" style="display:none">
								<td><label class="control-label">Secret</label></td>
								<td class="col-sm-10">
									<input id="iam_azuread_secret" value="" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip">
								</td>
							</tr>
							<tr class="form-group azuread" style="display:none">
								<td><label class="control-label">Resource</label></td>
								<td class="col-sm-10">
									<input id="iam_azuread_res" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip" value="https://graph.microsoft.com/">
								</td>
							</tr>
							<tr class="form-group azuread" style="display:none">
								<td><label class="control-label">Token EndPoint</label></td>
								<td class="col-sm-10">
									<input id="iam_azuread_ep" type="text" size="60" class="<%=SkinTools.cssInputClass()%>" data-toggle="tooltip" value="https://login.microsoftonline.com/[TENANT-ID]/oauth2/token">
								</td>
							</tr>
							</table>
							
						<!-- modal body -->
						</div> 
						<div class="modal-footer uk-modal-footer uk-text-right">
							<button class="btn btn-raised btn-primary iam_detail md-btn md-btn-flat md-btn-flat-primary" onclick="return iam_save()" style="display: none">Save</button>
							<button class="btn btn-raised btn-default md-btn md-btn-flat uk-modal-close" data-dismiss="modal">Close</button>
						</div>
						
					</div> <!-- modal content -->
				</div> <!-- dialog -->
		</div> <!-- modal5 -->
		
		<script>
		var tableIAM;	// data table
		
		function modal6SetStatus (text, color) {
			modalSetStatus ('modal6StatusMsg', text, color, '<%=SkinTools.getSkinName()%>');
		}

		function iam_onchange () {
			$('.winad').toggle();
			$('.azuread').toggle();
		}
		
		function iam_save() {
			// WindowsAD, AzureActiveDirectory
			var type 	= $('#iam_type option:selected').val();
			var name	= $('#iam_name').val();
			var domain 	= $('#iam_domain').val();
			// WinAD
			var server 	= $('#iam_winad_server').val();
			
			// azure
			var tenant 	= $('#iam_azuread_tenant').val();
			var client 	= $('#iam_azuread_client').val();
			var secret 	= $('#iam_azuread_secret').val();
			var res 	= $('#iam_azuread_res').val();
			var ep	 	= $('#iam_azuread_ep').val();
			
			if ( (type == 'WindowsAD' && (!name || !domain || !server))
				||  (type == 'AzureActiveDirectory' && (!tenant || !client || !secret)) ) 
			{
				modal6SetStatus ('All fields are required', 'danger');
				return false;
			}
			// replace teanant in ep token url https://login.microsoftonline.com/[TENANT-ID]/oauth2/token
			ep = ep.replace("\[TENANT-ID\]", tenant);
			
			var data = type == 'WindowsAD' 
				? {'type': type, 'name': name, 'domain': domain, 'iam_winad_server': server} 
				: {'type': type, 'name': name, 'domain': domain, 'iam_azuread_tenant': tenant, 'iam_azuread_client' : client, 'iam_azuread_secret' : secret, 'iam_azuread_res' : res, 'iam_azuread_ep' : ep};
				
			LOGD('Save type: ' + type + ' data:' + JSON.stringify(data));
			
			$.post( "<%=contextPath%>/SysAdmin?rq_operation=iam_save", data )
				.done(function( json ) {
					LOGD('IAM Save: ' + JSON.stringify(json));
					if ( json.status >= 400) {
						modal6SetStatus (json.message, 'danger');
						return;
					}
					tableIAM.ajax.reload();
					modal6SetStatus ('Saved ' + name);
				})
				.fail(function() {
					LOGE('IAM save failed.');
				});
			return false;
		}
		
		function initIAMUI () {
			var url = '<%=contextPath%>/SysAdmin?op=iam_describe';
			
			tableIAM = $('#tblIdentities').DataTable({
				stateSave: true,
				searching: false, paging: false, info: false ,
				"ajax": url,
				"columns": [
							{ "data": "name[, ]" }, // Names[, ]
							{ "data": "type[, ]" },
							{ "data": "domain[, ]" }
				],
				"columnDefs": [
			   			/* Wrap name with an inspect href */
						{	"targets": 3, 
							"render": function ( data, type, full, meta ) {
								//LOGD("IAM full=" + JSON.stringify(full));
								return '<div><a href="#" onclick="return delIAM(\'' + full['name'] + '\')">' + 'Delete' + '</a></div>' ;
						}} 
				]
			});
		}
		
		function delIAM (name) {
			LOGD("Del IAM " + name);
			$.post( "<%=contextPath%>/SysAdmin?rq_operation=iam_del", {'name' : name} )
				.done(function( json ) {
					LOGD('IAM Del: ' + JSON.stringify(json));
					if ( json.status >= 400) {
						modal6SetStatus (json.message, 'danger');
						return;
					}
					tableIAM.ajax.reload();
					modal6SetStatus ('Deleted ' + name);
				})
				.fail(function() {
					LOGE('IAM Del failed.');
				});
			return false;
		}
		
		</script>
