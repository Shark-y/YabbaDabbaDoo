<%@page import="com.cloud.core.net.ConnectionPool"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>

<%
	NodeConfiguration cfg = CloudServices.getNodeConfig();
	final String poolSize = cfg.getProperty(NodeConfiguration.KEY_CNPOOL_SIZE, ConnectionPool.DEFAULT_POOL_SIZE);
	final String poolGrow = cfg.getProperty(NodeConfiguration.KEY_CNPOOL_AUTOGROW, "true");
	final String maxSlots = cfg.getProperty(NodeConfiguration.KEY_CNPOOL_MAXSLOTS, ConnectionPool.DEFAULT_MAX_SLOTS);
	
%>
				<!-- CN POOLING VALUES -->
				<div class="form-group uk-form-row uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4">Connection Pool Size</label>
					<div class="col-sm-10 uk-width-3-4">
						<select	class="form-control" id="<%=NodeConfiguration.KEY_CNPOOL_SIZE%>" name="<%=NodeConfiguration.KEY_CNPOOL_SIZE%>" title="Connection Pool Size" data-md-selectize>
							<option value="5" <%=poolSize.equals("5") ? "selected" : ""%>>5</option>
							<option value="10" <%=poolSize.equals("10") ? "selected" : ""%>>10</option>
							<option value="15" <%=poolSize.equals("15") ? "selected" : ""%>>15</option>
							<option value="20" <%=poolSize.equals("20") ? "selected" : ""%>>20</option>
						</select>
					</div>
				</div>
		
				<div class="form-group uk-form-row uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4">Connection Pool Autogrow</label>
					<div class="col-sm-10 uk-width-3-4">
						<select	id="<%=NodeConfiguration.KEY_CNPOOL_AUTOGROW%>" name="<%=NodeConfiguration.KEY_CNPOOL_AUTOGROW%>" title="Autogrow Connection Pool" class="form-control" data-md-selectize>
							<option value="true" <%=poolGrow.equalsIgnoreCase("true") ? "selected" : ""%>>True</option>
							<option value="false" <%=poolGrow.equalsIgnoreCase("false") ? "selected" : ""%>>False</option>
						</select>
					</div>
				</div>
				
				
		
				<div class="form-group uk-form-row uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4">Connection Pool Max Terminals before Grow</label>
					<div class="col-sm-10 uk-width-3-4">
						<select	class="form-control" id="<%=NodeConfiguration.KEY_CNPOOL_MAXSLOTS%>" name="<%=NodeConfiguration.KEY_CNPOOL_MAXSLOTS%>" title="Max Terminals before Pool Autogrow" data-md-selectize>
							<option value="10" <%=maxSlots.equals("10") ? "selected" : ""%>>10</option>
							<option value="20" <%=maxSlots.equals("20") ? "selected" : ""%>>20</option>
							<option value="30" <%=maxSlots.equals("30") ? "selected" : ""%>>30</option>
							<option value="40" <%=maxSlots.equals("40") ? "selected" : ""%>>40</option>
							<option value="50" <%=maxSlots.equals("50") ? "selected" : ""%>>50</option>
							<option value="100" <%=maxSlots.equals("100") ? "selected" : ""%>>100</option>
						</select>
					</div>
				</div>
				<!-- END CN POOLING -->
