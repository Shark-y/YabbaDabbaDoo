<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="com.cloud.console.iam.Rbac.Role"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 

%>
			<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading card-header md-card-toolbar">
					<h2 class="panel-title md-card-toolbar-heading-text" data-toggle="tooltip" data-placement="top" title="">Security</h2>
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				<div class="panel-body card-body md-card-content">
					<!-- Authorization  style="display: none" -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Authorization Protocol</label>
						<div class="col-sm-10 uk-width-3-4">
							<select	class="form-control md-select" data-toggle="tooltip" name="<%=NodeConfiguration.KEY_AUTH_ENABLED%>" 
								id="<%=NodeConfiguration.KEY_AUTH_ENABLED%>" onchange="authorization_proto_change()"
								title="Set to true to enable Agent/API authorization." data-md-selectize>
								<option value="true" <%=cfgServer.isAuthorizationEnabled() ? "selected" : "" %>>JSON Web Token</option>
								<option value="false" <%=!cfgServer.isAuthorizationEnabled() ? "selected" : "" %>>Disabled</option>
							</select>
						</div>
					</div>
					<!-- OAuth1 -->
					<div class="form-group" id="p3r1" style="display: none">
						<label class="col-sm-2 control-label">Consumer Key</label>
						<div class="col-sm-10">
							<input id="<%=NodeConfiguration.KEY_AUTH_CON_KEY%>" name="<%=NodeConfiguration.KEY_AUTH_CON_KEY%>" 
								value="<%=cfgServer.getAuthorizationConsumerKey()%>"
								type="text" size="60" class="form-control" 
								required data-toggle="tooltip"
								title="The consumer key.">
						</div>
					</div>
					<!--  -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1" id="p2r2" style="display: none">
						<label class="col-sm-2 control-label uk-width-1-4">Consumer Secret</label>
						<div class="col-sm-10 uk-width-3-4">
							<input id="<%=NodeConfiguration.KEY_AUTH_CON_SECRET%>" type="text" size="60" class="form-control md-input"
								name="<%=NodeConfiguration.KEY_AUTH_CON_SECRET%>" 
								value="<%=cfgServer.getAuthorizationConsumerSecret()%>"
								required data-toggle="tooltip"
								title="The consumer secret." disabled="disabled">
						</div>
					</div>
					<div class="form-group" id="p3r3" style="display: none">
						<label class="col-sm-2 control-label">Token</label>
						<div class="col-sm-10">
							<input id="<%=NodeConfiguration.KEY_AUTH_TOKEN%>" name="<%=NodeConfiguration.KEY_AUTH_TOKEN%>" 
								value="<%=cfgServer.getAuthorizationToken()%>"
								type="text" size="60" class="form-control" 
								required data-toggle="tooltip"
								title="The token.">
						</div>
					</div>
					<div class="form-group" id="p3r4" style="display: none">
						<label class="col-sm-2 control-label">Token Secret</label>
						<div class="col-sm-10">
							<input id="<%=NodeConfiguration.KEY_AUTH_TOK_SECRET%>" name="<%=NodeConfiguration.KEY_AUTH_TOK_SECRET%>" 
								value="<%=cfgServer.getAuthorizationTokenSecret()%>"
								type="text" size="60" class="form-control" 
								required data-toggle="tooltip"
								title="The token secret.">
						</div>
					</div>
					<!-- RBAC -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Role Based Access Control</label>
						<div class="col-sm-10 uk-width-3-4">
							<a href="#" data-toggle="modal" data-target="#modal4" data-uk-modal="{target:'#modal4'}">Edit</a>
							<pre id="rback"><%=Rbac.toHtml(1) %></pre>
						</div>
					</div>
					<!-- IAM -->
					<div class="form-group">
						<div class="col-sm-12">
							<button type="button" class="btn btn-primary md-btn md-btn-primary" data-toggle="modal" data-target="#modal6" data-uk-modal="{target:'#modal6'}" title="Identity Access Manegement">Identity Access (IAM)</button>
						</div>
					</div>
					
				</div>
			</div>


