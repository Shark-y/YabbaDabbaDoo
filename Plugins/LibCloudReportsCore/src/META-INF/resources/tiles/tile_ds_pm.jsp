<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.rts.datasource.ext.PrometheusDataSource"%>
<%@page import="java.util.Arrays"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.rts.datasource.fs.FileSystemDataSource"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.core.IDataService"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="org.json.JSONObject"%>

<%
	final String dsName			= (String)request.getParameter("ds");		// DS name (or NULL if none)
	final String dsJSON			= request.getParameter("dsJSON");
	final JSONObject jds		= dsJSON != null && !dsJSON.isEmpty() && !dsJSON.equals("null")  ? new JSONObject(dsJSON) : null;
	
	final JSONObject params		= jds != null 		? jds.optJSONObject("params") 	: new JSONObject();
	final String dsType			= jds != null 		? jds.getString("type") 		: IDataSource.DataSourceType.PROMETHEUS.name();
	final String serviceCls		= (String)request.getParameter("serviceCls");	// If not NULL the this is a plugin
	
	IDataService service 		= serviceCls != null 	? (IDataService)PluginSystem.findInstance(serviceCls).getInstance()
										: (IDataService)CloudServices.findService(ServiceType.DAEMON);
	IDataSource ds				= service.getDataSource(dsName);
	
	
	//System.out.println("DSTYPE=" + dsType + " JSON=" + dsJSON + " name=" + dsName);
	
	if ( dsType.equals(IDataSource.DataSourceType.PROMETHEUS.name())) {
%>								


				<div class="<%=SkinTools.cssFormGroupClass() %>" id="pm1" style="display: none">
					<label class="<%=SkinTools.cssFormGroupLabelClass() %>">Base URL</label>
					<div class="<%=SkinTools.cssFormGroupContentClass() %>">
						<input id="pmUrl" type="text" class="form-control md-input" data-toggle="tooltip"
							name="pmUrl" value="<%=params.optString(PrometheusDataSource.KEY_PM_URL, "")%>"
							title="Prometheous base URL." placeholder="Prometheous base URL.">
					</div>
				</div>
				<div class="<%=SkinTools.cssFormGroupClass() %>" id="pm2" style="display: none">
					<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Poll Frequency (ms)</label>
					<div class="<%=SkinTools.cssFormGroupContentClass() %>">
						<input id="pmPollFreq" type="text" class="form-control md-input" data-toggle="tooltip"
							name="pmPollFreq" value="<%=params.optString(PrometheusDataSource.KEY_PM_FREQ, "30000")%>"
							title="Server poll frequency." placeholder="Server poll frequency.">
					</div>
				</div>

<%
	 }
%>	
				