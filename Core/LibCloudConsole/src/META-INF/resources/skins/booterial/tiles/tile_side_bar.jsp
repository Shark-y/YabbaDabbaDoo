<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cloud.console.ThemeManager.MenuDescriptor"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration.RunMode"%>

<%!

static String formatAttribute(String name, String value) {
	return name.equals(value) ? " class=\"active\"" : "";	
}

%>

<%
	// path to resources common to all skins
	String commonPath 		= request.getParameter("commonPath");

	// lcal res base path
	String basePath 		= request.getParameter("basePath");
	String active			= request.getParameter("active");
	String theme			= (String)session.getAttribute("theme");
	
	boolean loggedIn 		= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;

	if ( theme == null)		theme 		= ThemeManager.DEFAULT_THEME;
	if ( basePath == null) 	basePath 	= "";
	if ( active == null)	active 		= "";
	
	List<ServiceDescriptor> list 	= CloudServices.getNodeConfig().getServiceDescriptors();
	ThemeManager themeMgr 			= ThemeManager.getInstance();
	themeMgr.load(list);
	
	List<MenuDescriptor> perfMenus	= themeMgr.findMenusByParent("Performance");
	
	//theme.dumpMenus("TEST");
%>

            <!-- START LEFT SIDEBAR NAV-->
            <aside id="sidebar" class="sidebar c-overflow">
 				
				<% if ( loggedIn) { %>
				<!-- Logged user? -->
                <div class="profile-menu">
					<a href="">
	                	<div class="profile-pic" style="height: 76px" >
	                		&nbsp;
	                		<!--  
							<img src="<%=basePath%>/img/avatar.png" />
							-->
						</div>
                    	<div class="profile-info">
                            Administrator

                            <i class="zmdi zmdi-caret-down"></i>
 	                   	</div>
					</a>
					<ul class="main-menu">
                    	<li>
                    		<a href="<%=commonPath%>login.jsp?action=changepwd&r=index.jsp"><i class="zmdi zmdi-settings"></i> Password</a>
                        </li>
						<li>
							<a href="<%=commonPath%>login.jsp?action=logout"><i class="zmdi zmdi-time-restore"></i> Logout</a>
                        </li>
                    </ul>
                </div>
 				<% } %>
 				
				<ul class="main-menu">
					<!-- Home -->
	                <li>
	                	<a href="<%=commonPath%>index.jsp" class="waves-effect waves-cyan"><i class="zmdi zmdi-home"></i> Home</a>
	                </li>
				
					<!-- Configure -->
	                <li class="sub-menu">
	                	<!-- href="" -->
	                	<a><i class="zmdi zmdi-settings-square"></i> Configure</a>
                	
						<ul>
							<li>
								<a href="<%=commonPath%>jsp/config/config_node.jsp">Node</a>
                            </li>
	                        <!-- Vendor configs -->
		                    <% for ( ServiceDescriptor desc : list) { 
		                        	if (desc.getConfigFileName() != null && !desc.getConfigFileName().isEmpty()) { 
		                    %>
	                        <li>
	                                <a href="<%=commonPath%>jsp/config/config_backend.jsp?mode=<%=desc.getType()%>"><%=desc.getVendorName()%></a>
	                        </li>
	                        <%	
	                        	}
	                        } 
	                        %>
	                        <% for ( MenuDescriptor md : themeMgr.findMenusByParent("Configuration")) { %>
	                        <!-- Dynamic menus for Configuration --> 
	                        <li>
	                        	<a href="<%=commonPath%><%=md.url%>" <%=md.urlTarget != null ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
							</li>
	                        <% } %>
	                            
	                        <% if (com.cloud.core.services.CloudServices.getNodeConfig().isClusterEnabled() // 12/27/2018 getRunMode() == RunMode.CLUSTER
	                            	/* || com.cloud.core.services.CloudServices.getNodeConfig().getFailOverType() == FailOverType.CLUSTER */
	                        	) { %>
	                        <li>
	                        	<a href="<%=commonPath%>jsp/config/config_cluster.jsp?leftnav=1">Cluster</a>
	                        </li>
	                        <%} %>
						</ul>
					</li>
				
					<!-- Performance --> 
					<% if ( !themeMgr.isMarkedAsHidden("Performance")) { %>
					<li class="sub-menu">
						<a><i class="zmdi zmdi-trending-up"></i> Performance</a>
 
						<ul>
                            <% for ( MenuDescriptor md : perfMenus) { %>
                            <li>
                                <a href="<%=basePath%><%=md.url%>" <%=md.urlTarget != null ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
                            </li>
                            <% } %>
                            <li>
                                <a href="<%=commonPath%>jsp/perf/osp.jsp?theme=<%=theme%>">Cloud Profiler</a>
                            </li>
                       	</ul>                        
					</li>
					<% } %>
					
                	<!-- Dynamic Menus --> 
                	<% for ( MenuDescriptor md : themeMgr.findMenusByParent("/")) { %>
                	<li>
						<a href="<%=commonPath%><%=md.url%>"><i class="<%=md.cssClass%>"></i> <%=md.label%></a>
                	</li>
                	<% } %>
				
                
	                <!-- System -->
					<li class="sub-menu">
						<a><i class="zmdi zmdi-shield-check"></i> System</a>
					
							<ul>
	                        	<% for ( MenuDescriptor md : themeMgr.findMenusByParent("System")) { %>
		                        <!-- Dynamic menus for System --> 
		                        <li>
		                        	<a href="<%=commonPath%><%=md.url%>" <%=md.urlTarget != null ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
								</li>
		                        <% } %>
	                            <li>
                                	<a href="<%=commonPath%>log/browser.jsp?names=AUDIT" target="_blank"> Audit Trail</a>
                            	</li>
		                        <li>
		                        	<a href="<%=commonPath%>log/logview.jsp" target="_blank"> Log View</a>
		                        </li>
		                        <li>
		                        	<!-- <a href="<%=commonPath%>log/log4j.jsp" target="_blank"> Log Filters</a> -->
		                        	<a href="<%=commonPath%>log/browser.jsp" target="_blank"> Log History</a>
		                        </li>
		                        <li>
		                        	<a href="<%=commonPath%>jsp/sys/version.jsp"> Version</a>
		                        </li>
	                                
							</ul>
	                </li>
                
            	</ul>
            
            </aside>
            <!-- END LEFT SIDEBAR NAV-->

			