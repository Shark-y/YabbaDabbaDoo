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
	final String dsType			= jds != null 		? jds.getString("type") 		: IDataSource.DataSourceType.FILESYSTEM.name();
	final String serviceCls		= (String)request.getParameter("serviceCls");	// If not NULL the this is a plugin
	
	IDataService service 		= serviceCls != null 	? (IDataService)PluginSystem.findInstance(serviceCls).getInstance()
										: (IDataService)CloudServices.findService(ServiceType.DAEMON);
	IDataSource ds				= service.getDataSource(dsName);
	
	final String filter			= FileSystemDataSource.DEFAULT_EXTENSIONS;
	
	if ( dsType.equals(IDataSource.DataSourceType.FILESYSTEM.name())) {
%>								


				<div class="form-group" id="fs1" style="display: none">
					<label class="col-sm-2 control-label">Path</label>
					<div class="col-sm-10">
						<input id="fsPath" type="text" class="form-control" data-toggle="tooltip"
							name="fsPath" value="<%=params.optString(FileSystemDataSource.KEY_FS_PATH, "")%>"
							title="Full path to a file system folder." placeholder="Full path to a file system folder.">
					</div>
				</div>
				<div class="form-group" id="fs2" style="display: none">
					<label class="col-sm-2 control-label">Extensions Filter</label>
					<div class="col-sm-10">
						<input id="fsExts" type="text" class="form-control" data-toggle="tooltip"
							name="fsExts" value="<%=params.optString(FileSystemDataSource.KEY_FS_EXTS, filter)%>"
							title="Comma separated file extensions to filter." placeholder="<%=filter%>">
					</div>
				</div>

<%
	 }
%>	
				