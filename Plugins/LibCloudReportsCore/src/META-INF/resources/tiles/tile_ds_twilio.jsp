<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.core.IDataService"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.rts.datasource.media.BaseMapDataSource"%>

<%
	final String dsName			= (String)request.getParameter("ds");		// DS name (or NULL if none)
	final String dsJSON			= request.getParameter("dsJSON");
	final JSONObject jds		= dsJSON != null && !dsJSON.isEmpty() && !dsJSON.equals("null")  ? new JSONObject(dsJSON) : null;
	
	final JSONObject params		= jds != null 		? jds.optJSONObject("params") 	: new JSONObject();
	final String dsType			= jds != null 		? jds.getString("type") 		: IDataSource.DataSourceType.SMS_TWILIO.name();
	final String serviceCls		= (String)request.getParameter("serviceCls");	// If not NULL the this is a plugin
	
	IDataService service 		= serviceCls != null 	? (IDataService)PluginSystem.findInstance(serviceCls).getInstance()
														: (IDataService)CloudServices.findService(ServiceType.DAEMON);
	IDataSource ds				= service.getDataSource(dsName);
	
	if ( dsType.equals(IDataSource.DataSourceType.SMS_TWILIO.name())) {
%>								

				<!-- Twilio SMS -->
				<div class="form-group" id="tw1" style="display: none">
					<label class="col-sm-2 control-label" data-toggle="tooltip" title="Get this information from the Twilio. Click to access the console.">
						<a target="_blank" href="https://www.twilio.com/console/sms/dashboard">Application Id</a>
					</label>
					<div class="col-sm-10">
						<input id="<%=BaseMapDataSource.KEY_TWISMS_APPID%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=BaseMapDataSource.KEY_TWISMS_APPID%>" value="<%=params.optString(BaseMapDataSource.KEY_TWISMS_APPID, "")%>"
							title="Application Id. Get this value from Twilio console.">
					</div>
				</div>
				<div class="form-group" id="tw2" style="display: none">
					<label class="col-sm-2 control-label">Token</label>
					<div class="col-sm-10">
						<input id="<%=BaseMapDataSource.KEY_TWISMS_TOKEN%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=BaseMapDataSource.KEY_TWISMS_TOKEN%>" value="<%=params.optString(BaseMapDataSource.KEY_TWISMS_TOKEN, "")%>"
							title="Application token. Get this value from Twilio console.">
					</div>
				</div>
				<div class="form-group" id="tw3" style="display: none" title="Sender number. Get a number from the Twilio console.">
					<label class="col-sm-2 control-label">From Number</label>
					<div class="col-sm-10">
						<input id="<%=BaseMapDataSource.KEY_TWISMS_FROM%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=BaseMapDataSource.KEY_TWISMS_FROM%>" value="<%=params.optString(BaseMapDataSource.KEY_TWISMS_FROM, "")%>"
							title="Twilio From number. Format: +1NNN1231234" placeholder="+1nnnNNNnnnn">
					</div>
				</div>
				<!-- 
				<div class="form-group" id="tw4" style="display: none">
					<label class="col-sm-2 control-label">Destination</label>
					<div class="col-sm-10">
						<input id="<%=BaseMapDataSource.KEY_TWISMS_TO%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=BaseMapDataSource.KEY_TWISMS_TO%>" value="<%=params.optString(BaseMapDataSource.KEY_TWISMS_TO, "")%>"
							title="Destination number. Format: +1NNN1231234" placeholder="+1nnnNNNnnnn">
					</div>
				</div>
				-->
				<!-- End Twilio SMS -->
<%
	 }
%>	
