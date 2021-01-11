<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="com.rts.datasource.DataSourceManager"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.core.IDataService"%>
<%
	final String dsName			= (String)request.getParameter("ds");			// DS name (or NULL if none)
	final String serviceCls		= (String)request.getParameter("serviceCls");	// If not NULL the this is a plugin
	
	IDataService service 		= serviceCls != null 	? (IDataService)PluginSystem.findInstance(serviceCls).getInstance()
														: (IDataService)CloudServices.findService(ServiceType.DAEMON);
	IDataSource ds				= service.getDataSource(dsName);
%>								
								<div class="form-group uk-grid uk-width-1-1">
									<label class="col-sm-2 control-label uk-width-1-4">Name</label>
									<div class="col-sm-10 uk-width-3-4">
										<input type="text" name="name" id="name" class="form-control md-input" maxlength="80" required
											placeholder="Name"
											pattern="[A-Za-z0-9,_\- ]+"
											data-toggle="tooltip" title="Data Source name or id." 
											value="<%=ds != null ? ds.getName() : ""%>">
									</div>
								</div>

								<div class="form-group uk-grid uk-width-1-1">
									<label class="col-sm-2 control-label uk-width-1-4">Description</label>
									<div class="col-sm-10 uk-width-3-4">
										<input type="text" name="desc" id="desc" class="form-control md-input" maxlength="80" required
											pattern="[A-Za-z0-9,_\- ]+"
											placeholder="Description"
											value="<%=ds != null ? ds.getDescription() : ""%>">
									</div>
								</div>
	
								<div id="ssr1" class="form-group">
									<label class="col-sm-2 control-label">Port</label>
									<div class="col-sm-10">
										<input type="text" name="port" id="port" class="form-control" maxlength="80" 
											placeholder="TCP port"
											pattern="\d+" 
											data-toggle="tooltip" title="Listener port."
											value="<%=ds != null ? ds.getPort() : ""%>">
									</div>
								</div>
								
								<!-- FORMAT -->
								<div id="ssr2" class="form-group">
									<label class="col-sm-2 control-label">Format Header</label>
									<div class="col-sm-10">
										<input type="text" name="fmt-hdr" id="fmt-hdr" class="form-control" maxlength="80"
											pattern="[A-Za-z0-9,_| ]+"
											placeholder="F0|"
											value="<%=ds != null && ds.getFormat() != null ? ds.getFormat().getHeader() : ""%>">
									</div>
								</div>

								<div id="ssr3" class="form-group">
									<label class="col-sm-2 control-label">Format Footer</label>
									<div class="col-sm-10">
										<input type="text" name="fmt-ftr" id="fmt-ftr" class="form-control" maxlength="80"
											pattern="[A-Za-z0-9,_| ]+"
											placeholder="F3|END_OF_DATA"
											value="<%=ds != null && ds.getFormat() != null ? ds.getFormat().getFooter() : ""%>">
									</div>
								</div>

								<div id="ssr4" class="form-group">
									<label class="col-sm-2 control-label">Field Separator</label>
									<div class="col-sm-10">
										<select name="fmt-fsep" id="fmt-fsep" class="form-control">
											<option value="\|" selected="selected">| (Pipe)</option>
											<option value=",">, (Comma)</option>
											<option value=" "> (Space)</option>
										</select>
									</div>
								</div>
								<!-- 
								<tr>
									<td>Record Separator</td>
									<td>
										<select name="fmt-rsep" id="fmt-rsep" class="form-control">
											<option value="LF" selected="selected">LF</option>
											<option value="CRLF">CRLF</option> 
										</select>
									</td>
								</tr>
								-->
								
								<div id="ssr5" class="form-group">
									<label class="col-sm-2 control-label">Format Fields</label>
									<div class="col-sm-10">
										<input name="fmt-flds" id="fmt-flds" class="form-control" 
											maxlength="640" 
											placeholder="Comma separated list of field names (NO spaces or special characters)."
											value="<%=ds != null && ds.getFormat() != null && (ds.getType() != IDataSource.DataSourceType.PROMETHEUS) ? ds.getFormat().getFields() : ""%>"
											data-toggle="tooltip" 
											pattern="[A-Za-z0-9,_]+"
											title="Comma separated list of field names (NO spaces or special characters).">
									</div>
								</div>
								<!-- 10/3/2017 Storage -->
								<div id="ssr6" class="form-group" style="display: none">
									<label class="col-sm-2 control-label">Storage Fields</label>
									<div class="col-sm-10">
										<input name="fmt-sflds" id="fmt-sflds" class="form-control" 
											maxlength="640" 
											placeholder="Comma separated list of field names (NO spaces or special characters)."
											value="<%=ds != null && ds.getFormat() != null ? ds.getFormat().getStorageFields() : ""%>"
											data-toggle="tooltip" 
											pattern="[A-Za-z0-9,_\$=]+"
											title="Comma separated list of field names (NO spaces or special characters).">
									</div>
								</div>
								<!-- 11/16/2017 Storage options -->
								<div id="ssr7" class="form-group" style="display: none">
									<label class="col-sm-2 control-label">Storage Options</label>
									<div class="col-sm-8">
										<div class="checkbox checkbox-inline checkbox-primary">
											<label title="Delete all records before an update.">
												<input type="checkbox" name="chkWipeTable" <%=ds != null && ds.getFormat() != null && ds.getFormat().getStorageOptWipeTable() ? "checked" : "" %>>Wipe Before Update
											</label>
										</div>
									</div>
								</div>
