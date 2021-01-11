<%@page import="com.cloud.core.cron.AutoUpdateUtils"%>
<%@page import="com.cloud.core.profiler.CloudNotificationService"%>
<%@page import="com.cloud.core.io.MailTool"%>
<%@page import="com.cloud.core.cron.ErrorNotificationSystem"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 

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

	final String frequency	= params.optString(ErrorNotificationSystem.KEY_FREQ, "WEEKLY");
	
%>

<% if (cfgServer.productSupportsNotifications()) { %>
				<!-- FAILOVER AND NOTIFICATIONS STUFF -->
				<div class="form-group uk-form-row uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4">Auto Recovery</label>
					<div class="col-sm-10 uk-width-3-4">
					<select	id="<%=Auditor.KEY_NOTIFY_PROTO%>" name="<%=Auditor.KEY_NOTIFY_PROTO%>" onchange="audit_proto_change()"
						title="Protocol to use for audit/failover notifications." data-toggle="tooltip" class="form-control" data-md-selectize>
						<option value="<%=Auditor.KEY_PROTO_DISABLED%>" <%=cfgServer.getProperty(Auditor.KEY_NOTIFY_PROTO, "").equals(Auditor.KEY_PROTO_DISABLED) ? "selected" : "" %>>Disabled</option>
						<!-- 2/2/2019 Deprecated - Email notifications only
						<option value="<%=Auditor.KEY_PROTO_SMTP%>" <%=cfgServer.getProperty(Auditor.KEY_NOTIFY_PROTO, "").equals(Auditor.KEY_PROTO_SMTP) ? "selected" : "" %>>Recover with Mail - SMTP notifications</option>
						-->
						<option value="<%=Auditor.KEY_PROTO_SMTPS%>" <%=cfgServer.getProperty(Auditor.KEY_NOTIFY_PROTO, "").equals(Auditor.KEY_PROTO_SMTPS) ? "selected" : "" %>>Recover with E-Mail notifications</option>
						<!-- 2/2/2019 Deprecated
						<option value="<%=Auditor.KEY_PROTO_TWITTER%>" <%=cfgServer.getProperty(Auditor.KEY_NOTIFY_PROTO, "").equals(Auditor.KEY_PROTO_TWITTER) ? "selected" : "" %>>Recover with Twitter notifications</option>
						<option value="<%=Auditor.KEY_PROTO_TWILIOSMS%>" <%=cfgServer.getProperty(Auditor.KEY_NOTIFY_PROTO, "").equals(Auditor.KEY_PROTO_TWILIOSMS) ? "selected" : "" %>>Recover with Twilio SMS notifications</option>
						-->
						<option value="<%=Auditor.KEY_PROTO_PLAIN%>" <%=cfgServer.getProperty(Auditor.KEY_NOTIFY_PROTO, Auditor.KEY_PROTO_PLAIN).equals(Auditor.KEY_PROTO_PLAIN) ? "selected" : "" %>>Recover without notifications</option>
					</select>
					</div>
				</div>
				
				<!-- Audit system SMTP Delivery Method recipient -->
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="ar6" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">Recipient <small><a id="linkModal1" href="#" data-toggle="modal" data-target="#modal1" data-uk-modal="{target:'#modal1'}">Advanced</a></small></label>
					<div class="col-sm-10 uk-width-3-4">
						<input id="<%=Auditor.KEY_NOTIFY_SMTP_TO%>" type="text" class="form-control md-input" 
							name="<%=Auditor.KEY_NOTIFY_SMTP_TO%>" value="<%=to%>"
							title="Mail service recipient(s).">
					</div>
				</div>
				
				<div class="form-group uk-form-row uk-grid uk-width-1-1" id="atypes" style="display: none">
					<label class="col-sm-2 control-label uk-width-1-4">Alert Types</label>
					
					<div class="col-sm-10 uk-width-3-4">
						<a id="linkModal2" href="#" data-toggle="modal" data-target="#modal2" data-uk-modal="{target:'#modal2'}">Node Alerts</a>
						&nbsp;&nbsp;&nbsp;&nbsp;
						<a id="linkModal3" href="#" data-toggle="modal" data-target="#modal3" data-uk-modal="{target:'#modal3'}">Container Exceptions</a>
					</div>
				</div>
<% } %>
