<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.core.profiler.CloudNotificationService"%>
<%@page import="com.cloud.core.io.MailTool"%>
<%@page import="com.cloud.core.cron.ErrorNotificationSystem"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="com.cloud.console.iam.Rbac.Role"%>
<%@page import="com.cloud.console.iam.IAM"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 
	final String contextPath 		= getServletContext().getContextPath();
	
	// error notification system params from class path /configuration/pen_cfg.json
	/*	{"params": {
 "notification_password": "Thenewcti1",
 "notification_user": "converge_one@yahoo.com",
 "notification_debug": false,
 "notification_to": "cloud_git@convergeone.com",
 "notification_proto": "smtps",
 "notification_host": "smtp.mail.yahoo.com",
 "notification_starttls.enable": false,
 "notification_port": "465",
 "notification_from": "converge_one@yahoo.com",
 "notification_frequency": "DAILY",
 "notification_filter": "RegExp:(?s).*(NullPointerException|OutOfMemory|ATTACK).*",
 "notification_folder": "Inbox"
}} */
	JSONObject params = ErrorNotificationSystem.getDefaultConfiguration().optJSONObject("params");

	String host = cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_HOST);
	String port = cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_PORT);
	String user = cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_USER);
	String pwd 	= cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_PWD );
	String to	= cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_TO);
	String from	= cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_FROM);

	if ( host == null || host.isEmpty()) 	host 	= params.optString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_HOST);
	if ( port == null || port.isEmpty()) 	port 	= params.optString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_PORT);
	
	// cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_FROM, request.getContextPath().substring(1) + "@" + IOTools.getHostname() )
	if ( from == null || from.isEmpty()) 	from 	= params.optString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_FROM);
	if ( to == null || to.isEmpty()) 		to 		= params.optString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_TO);
	if ( user == null || user.isEmpty()) 	user 	= params.optString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_USER);
	if ( pwd == null || pwd.isEmpty()) 		pwd 	= params.optString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_PWD);

	//final String frequency	= params.optString(ErrorNotificationSystem.KEY_FREQ, "WEEKLY");

%>
		<!--  modal1: Audit system SMTP Delivery Method config --> 	
		<div id="modal1" class="modal fade uk-modal" tabindex="-1" role="dialog">
				<div class="modal-dialog uk-modal-dialog">
					<div class="modal-content">
						<div class="modal-body">
				
				<!-- SMTP advanced -->
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar3" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">Host</label>
					<div class="col-sm-10 uk-width-3-4">
						<input id="<%=Auditor.KEY_NOTIFY_SMTP_HOST%>" type="text" class="<%=SkinTools.cssInputClass()%>" 
							name="<%=Auditor.KEY_NOTIFY_SMTP_HOST%>" value="<%=host%>"
							title="Mail host name or IP address.">
					</div>
				</div>
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar4" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">Port</label>
					<div class="col-sm-10 uk-width-3-4">
						<input id="<%=Auditor.KEY_NOTIFY_SMTP_PORT%>" type="text" class="<%=SkinTools.cssInputClass()%>" 
							name="<%=Auditor.KEY_NOTIFY_SMTP_PORT%>" value="<%=port%>"
							title="Mail service port.">
					</div>
				</div>
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar5" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">From</label>
					<div class="col-sm-10 uk-width-3-4">
						<input id="<%=Auditor.KEY_NOTIFY_SMTP_FROM%>" type="text" class="<%=SkinTools.cssInputClass()%>" 
							name="<%=Auditor.KEY_NOTIFY_SMTP_FROM%>" value="<%=from%>"
							title="Mail service from.">
					</div>
				</div>
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar1" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">User</label>
					<div class="col-sm-10 uk-width-3-4">
						<input id="<%=Auditor.KEY_NOTIFY_SMTP_USER%>" type="text" class="<%=SkinTools.cssInputClass()%>" 
							name="<%=Auditor.KEY_NOTIFY_SMTP_USER%>" 
							value="<%=user%>"
							title="Mail service user name.">
					</div>
				</div>
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar2" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">Password</label>
					<div class="col-sm-10 uk-width-3-4">
						<input id="<%=Auditor.KEY_NOTIFY_SMTP_PWD%>" type="password" class="<%=SkinTools.cssInputClass()%>" autocomplete="off"
							name="<%=Auditor.KEY_NOTIFY_SMTP_PWD%>" 
							value="<%=pwd%>"
							title="Mail service password.">
					</div>
				</div>
				<!--  End SMTP -->
				
				<!-- Twitter -->
				<div class="form-group" id="tr1" style="display: none">
					<label class="col-sm-2 control-label" data-toggle="tooltip" title="Get this information from Twitter Apps."><a target="_blank" href="https://apps.twitter.com/">Consumer Key</a></label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWIT_CK%>" type="text" class="form-control" autocomplete="off"
							name="<%=Auditor.KEY_NOTIFY_TWIT_CK%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWIT_CK, "")%>"
							title="Consumer Key (API Key).">
					</div>
				</div>
				<div class="form-group" id="tr2" style="display: none">
					<label class="col-sm-2 control-label">Consumer Secret</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWIT_CS%>" type="text" class="form-control" autocomplete="off"
							name="<%=Auditor.KEY_NOTIFY_TWIT_CS%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWIT_CS, "")%>"
							title="Consumer Secret.">
					</div>
				</div>
				<div class="form-group" id="tr3" style="display: none">
					<label class="col-sm-2 control-label">Token</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWIT_TK%>" type="text" class="form-control" autocomplete="off"
							name="<%=Auditor.KEY_NOTIFY_TWIT_TK%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWIT_TK, "")%>"
							title="Token.">
					</div>
				</div>
				<div class="form-group" id="tr4" style="display: none">
					<label class="col-sm-2 control-label">Token Secret</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWIT_TS%>" type="text" class="form-control" autocomplete="off"
							name="<%=Auditor.KEY_NOTIFY_TWIT_TS%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWIT_TS, "")%>"
							title="Token Secret.">
					</div>
				</div>
				<!--  End Twitter -->
				
				<!-- Twilio SMS -->
				<div class="form-group" id="tw1" style="display: none">
					<label class="col-sm-2 control-label" data-toggle="tooltip" title="Get this information from the Twilio. Click to access the console.">
						<a target="_blank" href="https://www.twilio.com/console/sms/dashboard">Application Id</a>
					</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWISMS_APPID%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=Auditor.KEY_NOTIFY_TWISMS_APPID%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWISMS_APPID, "")%>"
							title="Application Id. Get this value from Twilio console.">
					</div>
				</div>
				<div class="form-group" id="tw2" style="display: none">
					<label class="col-sm-2 control-label">Token</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWISMS_TOKEN%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=Auditor.KEY_NOTIFY_TWISMS_TOKEN%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWISMS_TOKEN, "")%>"
							title="Application token. Get this value from Twilio console.">
					</div>
				</div>
				<div class="form-group" id="tw3" style="display: none">
					<label class="col-sm-2 control-label">From Number</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWISMS_FROM%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=Auditor.KEY_NOTIFY_TWISMS_FROM%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWISMS_FROM, "")%>"
							title="Twilio From number. Format: +1NNN1231234" placeholder="+1nnnNNNnnnn">
					</div>
				</div>
				<div class="form-group" id="tw4" style="display: none">
					<label class="col-sm-2 control-label">Destination</label>
					<div class="col-sm-10">
						<input id="<%=Auditor.KEY_NOTIFY_TWISMS_TO%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=Auditor.KEY_NOTIFY_TWISMS_TO%>" value="<%=cfgServer.getProperty(Auditor.KEY_NOTIFY_TWISMS_TO, "")%>"
							title="Destination number. Format: +1NNN1231234" placeholder="+1nnnNNNnnnn">
					</div>
				</div>
				<!-- End Twilio SMS -->
				
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar7" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4"><input type="hidden" name="test_audit" id="test_audit" value="false" />&nbsp;</label>
					<div class="col-sm-10 uk-width-3-4">
						<button class="btn btn-primary md-btn md-btn-primary" onclick="audit_test_click()">Send a Test Message</button>
					</div>
				</div>
						
						</div> <!-- modal body -->
						<div class="modal-footer uk-modal-footer uk-text-right">
							<input type="button" value="Close" class="btn btn-primary md-btn md-btn-flat uk-modal-close" data-dismiss="modal">
						</div>
						
					</div> <!-- modal content -->
				</div> <!-- dialog -->
		</div> <!-- modal -->
	
		<!--  modal2: Alert types --> 	
		<div id="modal2" class="modal fade uk-modal" tabindex="-1" role="dialog">
				<div class="modal-dialog uk-modal-dialog">
					<div class="modal-content">
						<div class="modal-body">
							<h3>Alert Types</h3>
							
			<table>
				<tr class="form-group">
					<td><label class="control-label">Peak CPU % usage above</label></td>
					<td class="col-sm-6">
						<input class="form-control" name="txtCPU" type="number" value="<%=CloudNotificationService.getThresholdCPU()%>" pattern="\d{2,3}" title="Live thread count." required="required">
					</td>
				</tr>
				<tr class="form-group">
					<td><label class="control-label">Peak Live thread count above</label></td>
					<td class="col-sm-6">
					<input class="form-control" name="txtThreads" type="number" value="<%=CloudNotificationService.getThresholdThreads()%>" pattern="\d{2,4}" title="Live thread count." required="required"> 
					</td>
				</tr>
				<tr class="form-group">
					<td><label class="control-label">Free heap (MB) below</label></td>
					<td class="col-sm-6">
					<input class="form-control" name="txtHeap" type="number" value="<%=CloudNotificationService.getThresholdHeap()%>" pattern="\d{1,3}" title="Free heap." required="required"> 
					</td>
				</tr>
				<%if (cfgServer.containsKey(NodeConfiguration.KEY_SERVER_LIC)) { %>
				<tr class="form-group">
					<td><label class="control-label">Peak license % usage above</label></td>
					<td class="col-sm-6">
						<input class="form-control" name="txtPeakLic" type="text" value="<%=CloudNotificationService.getThresholdLicense()%>" pattern="\d{1,3}" title="Peak license usage." required="required">
						&nbsp;&nbsp;&nbsp;&nbsp;<a href="../perf/lhpu.jsp" target="_blank">Historical peak usage</a>						
					</td>
				</tr>
				<% } %>
			</table>
						</div> <!-- modal body -->
						<div class="modal-footer uk-modal-footer uk-text-right">
							<input type="button" value="Close" class="btn btn-primary md-btn md-btn-flat uk-modal-close" data-dismiss="modal">
						</div>
						
					</div> <!-- modal content -->
				</div> <!-- dialog -->
		</div> <!-- modal -->

		<!--  modal3: Container Exceptions --> 	
		<div id="modal3" class="modal fade uk-modal" tabindex="-1" role="dialog">
				<div class="modal-dialog uk-modal-dialog">
					<div class="modal-content">
						<div class="modal-body">
							<h3>Container Exceptions</h3>

							<jsp:include page="../perf/osp_tile_smtp.jsp">
								<jsp:param value="block" name="display"/>
								<jsp:param value="notification_" name="idPrefix"/>
							</jsp:include>

						</div> <!-- modal body -->
						<div class="modal-footer uk-modal-footer uk-text-right">
							<input type="button" value="Close" class="btn btn-primary md-btn md-btn-flat uk-modal-close" data-dismiss="modal">
						</div>
						
					</div> <!-- modal content -->
				</div> <!-- dialog -->
		</div> <!-- modal -->


		<!--  modal4: RBAC editor --> 	
		<div id="modal4" class="modal fade uk-modal" tabindex="-1" role="dialog">
				<div class="modal-dialog uk-modal-dialog">
					<div class="modal-content">
						<div class="modal-body">
							<h3>Role Bindings</h3>

							<div class="<%=SkinTools.cssFormGroupClass()%>">
								<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Role</label>
								<div class="<%=SkinTools.cssFormGroupContentClass()%>">
									<select	class="form-control" data-toggle="tooltip" name="rbac_role" id="rbac_role" onchange="rbac_onchange()">
									<% for (Role role : Role.values()) { %>
										<option value="<%=role.name()%>"><%=role.name()%></option>
									<% } %>
									</select>
								</div>
							</div>
							<div class="<%=SkinTools.cssFormGroupClass()%>">
								<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Users</label>
								<div class="<%=SkinTools.cssFormGroupContentClass()%>">
									<textarea id="rbac_users" name="rbac_users" rows="3" cols="80" class="<%=SkinTools.cssInputClass()%>" title="User(s) separated by comma."><%=Rbac.getRoleBinding(Role.Administrator)%></textarea>
								</div>
							</div>

						<!-- modal body -->
						</div> 
						<div class="modal-footer uk-modal-footer uk-text-right">
							<input type="button" value="Close" class="btn btn-primary md-btn md-btn-flat uk-modal-close" data-dismiss="modal">
						</div>
						
					</div> <!-- modal content -->
				</div> <!-- dialog -->
		</div> <!-- modal -->

		<script>
		var Rbac = <%=!Rbac.toHtml(0).isEmpty() ? Rbac.toHtml(0) : "{}" %> ; 
		
		
		function rbac_onchange () {
			var role 	= $('#rbac_role').val();
			var users	= Rbac[role];
			
			LOGD('RBAC role: ' + role + ' Users:' + users);
			
			$('#rbac_users').html(users);
			$('#rbac_users').val(users);
		}
		</script>

