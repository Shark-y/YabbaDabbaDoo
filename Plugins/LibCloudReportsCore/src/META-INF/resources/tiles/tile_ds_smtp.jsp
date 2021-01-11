<%@page import="com.cloud.core.io.MailTool"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.util.Properties"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%
	final String dsName			= (String)request.getParameter("ds");		// DS name (or NULL if none)
	final String dsJSON			= request.getParameter("dsJSON");
	final JSONObject jds		= dsJSON != null && !dsJSON.isEmpty() && !dsJSON.equals("null")  ? new JSONObject(dsJSON) : null;
	
	final JSONObject params		= jds != null 		? jds.optJSONObject("params") 	: new JSONObject();
	final String dsType			= jds != null 		? jds.getString("type") 		: "SMTP";
	
	//IDataService service 		= (IDataService)CloudServices.findService(ServiceType.DAEMON);
	//IDataSource ds			= service.getDataSource(dsName);
	
	if ( dsType.startsWith("SMTP") || dsType.startsWith("POP3") || dsType.startsWith("IMAP")) {
%>								


				<div class="form-group" id="smtpr1" style="display: none">
					<label class="col-sm-2 control-label">Host</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_HOST%>" type="text" class="form-control" 
							name="<%=MailTool.KEY_SMTP_HOST%>" value="<%=params.optString(MailTool.KEY_SMTP_HOST, "")%>"
							title="Mail host name or IP address.">
					</div>
				</div>
				<!-- Port already defined in main tile
				<div class="form-group" id="smtpr2" style="display: none">
					<label class="col-sm-2 control-label">Port</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_PORT%>" type="text" class="form-control" 
							name="<%=MailTool.KEY_SMTP_PORT%>" value="<%=params.optString(MailTool.KEY_SMTP_PORT, "25")%>"
							title="Mail service port.">
					</div>
				</div>
				-->
				<div class="form-group" id="smtpr3" style="display: none">
					<label class="col-sm-2 control-label">From</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_FROM%>" type="text" class="form-control" 
							name="<%=MailTool.KEY_SMTP_FROM%>" value="<%=params.optString(MailTool.KEY_SMTP_FROM, request.getContextPath().substring(1) + "@" + IOTools.getHostname() )%>"
							title="Mail service from.">
					</div>
				</div>
				<!-- To is sent via the REST call
				<div class="form-group" id="smtpr4" style="display: none">
					<label class="col-sm-2 control-label">Recipient</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_TO%>" type="text" class="form-control" 
							name="<%=MailTool.KEY_SMTP_TO%>" value="<%=params.optString(MailTool.KEY_SMTP_TO, "")%>"
							title="Mail service recipient(s).">
					</div>
				</div>
				-->
				<div class="form-group" id="smtpr5" style="display: none">
					<label class="col-sm-2 control-label">User</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_USER%>" type="text" class="form-control" 
							name="<%=MailTool.KEY_SMTP_USER%>" value="<%=params.optString(MailTool.KEY_SMTP_USER, "")%>"
							title="Mail service user name.">
					</div>
				</div>
				<div class="form-group" id="smtpr6" style="display: none">
					<label class="col-sm-2 control-label">Password</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_PWD%>" type="password" class="form-control" autocomplete="off"
							name="<%=MailTool.KEY_SMTP_PWD%>" value="<%=params.optString(MailTool.KEY_SMTP_PWD, "")%>"
							title="Mail service password.">
					</div>
				</div>
				<div class="form-group" id="smtpr7" style="display: none">
					<label class="col-sm-2 control-label">Folder</label>
					<div class="col-sm-10">
						<input id="<%=MailTool.KEY_SMTP_FOLDER%>" type="text" class="form-control" 
							name="<%=MailTool.KEY_SMTP_FOLDER%>" value="<%=params.optString(MailTool.KEY_SMTP_FOLDER, "Inbox")%>"
							title="Mail service password.">
					</div>
				</div>
				
				<div id="smtpr8" class="form-group" style="display: none">
					<label class="col-sm-2 control-label">Options</label>
					<div class="col-sm-10">
						<!-- checkbox-inline checkbox-primary -->
						<div class="checkbox">
                        	<label title="Enable the use of the STLS command (if supported by the server) to switch the connection to a TLS-protected connection">
								<input type="checkbox" id="<%=MailTool.KEY_SMTP_TLS%>" 
									name="<%=MailTool.KEY_SMTP_TLS%>" <%=params.optBoolean(MailTool.KEY_SMTP_TLS) ? "checked" : "" %>>
								<i class="input-helper"></i>
								Start TLS
							</label>
						</div>
					</div>
				</div>
				
<%
	}
%>	
