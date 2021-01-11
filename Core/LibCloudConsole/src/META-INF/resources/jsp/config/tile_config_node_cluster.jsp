<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 

%>
		<%if (cfgServer.productSupportsClustering()) { %>
				<div class="form-group uk-form-row uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4">Cluster</label>
					<div class="col-sm-10 uk-width-3-4">
						<!-- onchange="cluster_proto_change()" -->
						<select	id="<%=NodeConfiguration.KEY_FAILOVER_TYPE%>" name="<%=NodeConfiguration.KEY_FAILOVER_TYPE%>" data-toggle="tooltip"
							title="Enable or disable cluster features." class="form-control" data-md-selectize>
							<option value="CLUSTER_ZEROCONF" <%=cfgServer.getFailOverType() == FailOverType.CLUSTER_ZEROCONF ? "selected" : "" %>>Auto-Discovery (Zeroconf)</option>
							<!-- 1/14/2019 hazelcast disabled -->
							<option value="CLUSTER_HAZELCAST" <%=cfgServer.getFailOverType() == FailOverType.CLUSTER_HAZELCAST ? "selected" : "" %>>Hazelcast</option>
							
							<option value="SERVLET" <%=cfgServer.getFailOverType() == FailOverType.SERVLET ? "selected" : "" %>>Disabled</option>
						</select>
					</div>
				</div>
				<!-- For cluster type datastore (default) FUTURE IMPLEMENTATION 
				<div class="form-group" id="clr1" style="display: none">
					<label class="col-sm-2 control-label">DataStore Url</label>
					<div class="col-sm-10">
						<input id="<%=NodeConfiguration.KEY_CLUSTER_DSURL%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=NodeConfiguration.KEY_CLUSTER_DSURL%>" value="<%=cfgServer.getProperty(NodeConfiguration.KEY_CLUSTER_DSURL, "jdbc:mysql://localhost:3306/db?useSSL=true&verifyServerCertificate=true")%>"
							title="Datastore endpoint URL.">
					</div>
				</div>
				<div class="form-group" id="clr2" style="display: none">
					<label class="col-sm-2 control-label">User</label>
					<div class="col-sm-10">
						<input id="<%=NodeConfiguration.KEY_CLUSTER_DSUSER%>" type="text" class="form-control" data-toggle="tooltip"
							name="<%=NodeConfiguration.KEY_CLUSTER_DSUSER%>" value="<%=cfgServer.getProperty(NodeConfiguration.KEY_CLUSTER_DSUSER, "")%>"
							title="Datastore basic authentication user.">
					</div>
				</div>
				<div class="form-group" id="clr3" style="display: none">
					<label class="col-sm-2 control-label">Password</label>
					<div class="col-sm-10">
						<input id="<%=NodeConfiguration.KEY_CLUSTER_DSPWD%>" type="password" class="form-control" data-toggle="tooltip"
							name="<%=NodeConfiguration.KEY_CLUSTER_DSPWD%>" value="<%=cfgServer.getProperty(NodeConfiguration.KEY_CLUSTER_DSPWD, "")%>"
							title="Datastore basic authentication password.">
					</div>
				</div> -->
				<!-- End cluster DS config-->
		<% } %>
