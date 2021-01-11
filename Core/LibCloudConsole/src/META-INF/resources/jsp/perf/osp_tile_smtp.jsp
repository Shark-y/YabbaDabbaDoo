<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.io.MailTool"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.cron.AutoUpdateUtils"%>
<%@page import="com.cloud.core.cron.ErrorNotificationSystem"%>

<%
	// default display
	final String display		= request.getParameter("display") != null ? request.getParameter("display") : "block" ;
	
	final String prefix			= request.getParameter("idPrefix") != null ? request.getParameter("idPrefix") : "smtp_" ;

	// Load the smtp config from $HOME/.cloud
	JSONObject smtpCfg = null;
	try {
		smtpCfg = AutoUpdateUtils.getConfiguration();
	}
	catch (Exception e) {
		//e.printStackTrace();
		// No cfg in $HOME/.cloud - Load default from the class path
		smtpCfg = ErrorNotificationSystem.getDefaultConfiguration();		
	}
	
	// Configuration as JSON
	final JSONObject jds		= smtpCfg;
	
	JSONObject params			= jds != null 						? jds.optJSONObject("params") 	: new JSONObject();
	final String dsType			= jds != null && jds.has("type")	? jds.getString("type") 		: "smtps";
	final String frequency		= params.optString(ErrorNotificationSystem.KEY_FREQ, "WEEKLY");

%>

			<input type="hidden" name="<%=prefix%>proto" value="<%=dsType%>">
	
				
			<table>
				<% if ( display.equals("block"))  {%>
				<!--  Check Frequency -->
				<tr class="form-group" id="smtpr9">
					<td> <label class="col-sm-2 control-label">Frequency</label></td>
					<td class="col-sm-10">
						<select id="<%=ErrorNotificationSystem.KEY_FREQ%>" class="form-control" name="<%=ErrorNotificationSystem.KEY_FREQ%>" 
							title="Frequency at which errors are checked.">
							<option value="DISABLED" <%=frequency.equals(AutoUpdateUtils.Frequency.DISABLED.name()) ? "selected" : ""%>>Disabled</option>
							<option value="DAILY" <%=frequency.equals(AutoUpdateUtils.Frequency.DAILY.name()) ? "selected" : ""%>>Daily</option>
							<option value="WEEKLY" <%=frequency.equals(AutoUpdateUtils.Frequency.WEEKLY.name()) ? "selected" : ""%>>Weekly</option>
							<option value="MONTHLY" <%=frequency.equals(AutoUpdateUtils.Frequency.MONTHLY.name()) ? "selected" : ""%>>Monthly</option>
						</select>
					</td>
				</tr>
				
        		<!--  Customer -->
        		<tr class="form-group" id="smtpr9">
					<td><label class="col-sm-2 control-label">Customer</label></td>
					<td class="col-sm-10">
						<input id="<%=ErrorNotificationSystem.KEY_VENDOR%>" type="text" class="form-control" 
							name="<%=ErrorNotificationSystem.KEY_VENDOR%>" 
							value="<%=params.optString(ErrorNotificationSystem.KEY_VENDOR, IOTools.getHostname())%>"
							title="Customer name for notification information." required="required">
					</td>
				</tr>

       			<tr class="form-group" id="smtpr9">
					<td><label class="col-sm-2 control-label">Search Filter</label></td>
					<td class="col-sm-10">
						<input id="<%=ErrorNotificationSystem.KEY_FILTER%>" type="text" class="form-control" 
							name="<%=ErrorNotificationSystem.KEY_FILTER%>" 
							value="<%=params.optString(ErrorNotificationSystem.KEY_FILTER, "RegExp:(?s).*(NullPointerException|OutOfMemory).*")%>"
							title="Search regular expression.." required="required">
					</td>
				</tr>
				<% } %>
				
				<!-- Delivery method SMTP values -->
				<tr class="form-group" id="smtpr1">
					<td><label class="col-sm-2 control-label">Host</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_HOST%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_HOST%>" value="<%=params.optString(prefix + MailTool.KEY_SMTP_HOST, "")%>"
							title="Mail host name or IP address.">
					</td>
				</tr>

				<!-- PORT def 465 -->
				<tr class="form-group" id="smtpr2">
					<td> <label class="col-sm-2 control-label">Port</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_PORT%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_PORT%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_PORT, "465")%>"
							title="Mail service port.">
					</td>
				</tr>
				<!-- FROM -->
				<tr class="form-group" id="smtpr3">
					<td> <label class="col-sm-2 control-label">From</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_FROM%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_FROM%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_FROM, request.getContextPath().substring(1) + "@" + IOTools.getHostname() )%>"
							title="Mail service from.">
					</td>
				</tr>
				<!-- TO -->
				<tr class="form-group" id="smtpr4" >
					<td><label class="col-sm-2 control-label">Recipient</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_TO%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_TO%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_TO, "")%>"
							title="Mail service recipient(s).">
					</td>
				</tr>
				
				<tr class="form-group" id="smtpr5">
					<td><label class="col-sm-2 control-label">User</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_USER%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_USER%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_USER, "")%>"
							title="Mail service user name.">
					</td>
				</tr>
				
				<tr class="form-group" id="smtpr6" >
					<td><label class="col-sm-2 control-label">Password</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_PWD%>" type="password" class="form-control" autocomplete="off"
							name="<%=prefix%><%=MailTool.KEY_SMTP_PWD%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_PWD, "")%>"
							title="Mail service password.">
					</td>
				</tr>
				
				<tr class="form-group" id="smtpr7">
					<td><label class="col-sm-2 control-label">Folder</label></td>
					<td class="col-sm-10">
						<input id="<%=prefix%><%=MailTool.KEY_SMTP_FOLDER%>" type="text" class="form-control" 
							name="<%=prefix%><%=MailTool.KEY_SMTP_FOLDER%>" 
							value="<%=params.optString(prefix + MailTool.KEY_SMTP_FOLDER, "Inbox")%>"
							title="Mail service password.">
					</td>
				</tr>

				<tr id="smtpr8" class="form-group">
					<td><label class="col-sm-2 control-label">Options</label></td>
					<td class="col-sm-10">
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
					</td>
				</tr>

       			<% if (AutoUpdateUtils.configExists() && display.equals("block")) { %>
        		<!-- test btn -->
				<tr class="form-group">
					<td><input type="hidden" name="test_pen" id="test_pen" value="false" /></td>
		        	<td class="col-sm-12">
				        <button id="btnError" type="submit" class="btn btn-success" onclick="document.getElementById('test_pen').value = 'true'">Check for Errors</button>
					</td>
				</tr>
        		<% } %>

				<!-- END SMTP VALUES -->        	
			
			</table>
        		
				
 