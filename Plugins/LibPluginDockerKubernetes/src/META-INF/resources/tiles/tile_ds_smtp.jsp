<%@page import="com.cloud.core.io.MailTool"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.util.Properties"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%
	//final String dsName			= (String)request.getParameter("ds");		// DS name (or NULL if none)
	
	// Configuration JSON
	final String dsJSON			= request.getParameter("dsJSON");
	
	// default display
	final String display		= request.getParameter("display") != null ? request.getParameter("display") : "block" ;
	
	final String prefix			= request.getParameter("idPrefix") != null ? request.getParameter("idPrefix") : "smtp_" ;

	// Configuration as JSON
	final JSONObject jds		= dsJSON != null && !dsJSON.isEmpty() && !dsJSON.equals("null")  ? new JSONObject(dsJSON) : null;
	
	JSONObject params			= jds != null 						? jds.optJSONObject("params") 	: new JSONObject();
	final String dsType			= jds != null && jds.has("type")	? jds.getString("type") 		: "smtps";
	
	if ( params == null )		params = new JSONObject();
	
	//System.out.println("**TILE-DS-SMTP dsType =" + dsType + " params=" + params + " jsd=" + jds + " JSON:" + dsJSON + " display:" + display);
	
	if ( dsType.startsWith("smtp") || dsType.startsWith("pop3") || dsType.startsWith("imap")) {
%>								
				<input type="hidden" name="<%=prefix%>proto" value="<%=dsType%>">

				<div class="form-group" id="smtpr1" style="display: <%=display%>">
					<label class="col-sm-2 control-label">Host</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_HOST%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_HOST%>" value="<%=params.optString(prefix + MailTool.KEY_SMTP_HOST, "")%>"
							title="Mail host name or IP address.">
					</div>
				</div>
				<!-- PORT def 465 -->
				<div class="form-group" id="smtpr2" style="display: <%=display%>">
					<label class="col-sm-2 control-label">Port</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_PORT%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_PORT%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_PORT, "465")%>"
							title="Mail service port.">
					</div>
				</div>
				<!-- FROM -->
				<div class="form-group" id="smtpr3" style="display: <%=display%>">
					<label class="col-sm-2 control-label">From</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_FROM%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_FROM%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_FROM, request.getContextPath().substring(1) + "@" + IOTools.getHostname() )%>"
							title="Mail service from.">
					</div>
				</div>
				<!-- TO -->
				<div class="form-group" id="smtpr4" style="display: <%=display%>">
					<label class="col-sm-2 control-label">Recipient</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_TO%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_TO%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_TO, "")%>"
							title="Mail service recipient(s).">
					</div>
				</div>
				
				<div class="form-group" id="smtpr5" style="display: <%=display%>">
					<label class="col-sm-2 control-label">User</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_USER%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_USER%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_USER, "")%>"
							title="Mail service user name.">
					</div>
				</div>
				<div class="form-group" id="smtpr6" style="display: <%=display%>">
					<label class="col-sm-2 control-label">Password</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_PWD%>" type="password" class="form-control" autocomplete="off"
							name="<%=prefix%><%=MailTool.KEY_SMTP_PWD%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_PWD, "")%>"
							title="Mail service password.">
					</div>
				</div>
				<div class="form-group" id="smtpr7" style="display: none">
					<label class="col-sm-2 control-label">Folder</label>
					<div class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_FOLDER%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_FOLDER%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_FOLDER, "Inbox")%>"
							title="Mail service password.">
					</div>
				</div>
				
				<div id="smtpr8" class="form-group" style="display: <%=display%>">
					<label class="col-sm-2 control-label">Options</label>
					<div class="col-sm-10">
						<!-- checkbox-inline checkbox-primary -->
						<div class="checkbox">
                        	<label title="Enable the use of the STLS command (if supported by the server) to switch the connection to a TLS-protected connection.">
								<input type="checkbox" id="<%=prefix%><%=MailTool.KEY_SMTP_TLS%>" 
									name="<%=prefix%><%=MailTool.KEY_SMTP_TLS%>" <%=params.optString(prefix + MailTool.KEY_SMTP_TLS).equals("on") ? "checked" : "" %>>
									
								<!-- Unchecked values are not submitted. see: http://dustinbolton.com/submit-unchecked-checkbox-value/ -->
								<input type="hidden" name="<%=prefix%><%=MailTool.KEY_SMTP_TLS%>" value="off" />
								
								<i class="input-helper"></i>
								Start TLS
							</label>
							&nbsp;&nbsp;&nbsp;&nbsp;

							<!-- debug -->
                       		<label title="Enable message debugging to stdout.">
								<input type="checkbox" id="<%=prefix%><%=MailTool.KEY_SMTP_DEBUG%>" 
									name="<%=prefix%><%=MailTool.KEY_SMTP_DEBUG%>" <%=params.optString(prefix + MailTool.KEY_SMTP_DEBUG).equals("on") ? "checked" : "" %>>
									
								<!-- Unchecked values are not submitted. see: http://dustinbolton.com/submit-unchecked-checkbox-value/ -->
                       			<input type="hidden" name="<%=prefix%><%=MailTool.KEY_SMTP_DEBUG%>" value="off" />
                       			
								<i class="input-helper"></i>
								Debug
							</label>

						</div>
						
					</div>
				</div>
				
<%
	}
%>	
