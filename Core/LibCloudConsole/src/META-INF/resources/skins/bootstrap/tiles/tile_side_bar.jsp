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
	String commonPath 	= request.getParameter("commonPath");

	String basePath 	= request.getParameter("basePath");
	String active		= request.getParameter("active");
	String theme		= (String)session.getAttribute("theme");
	
	if ( theme == null)			theme 		= ThemeManager.DEFAULT_THEME;
	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	if ( active == null)		active 		= "";
	
	List<ServiceDescriptor> list 	= CloudServices.getNodeConfig().getServiceDescriptors();
	ThemeManager themeMgr 			= ThemeManager.getInstance();
	themeMgr.load(list);
	
	List<MenuDescriptor> perfMenus	= themeMgr.findMenusByParent("Performance");
	
	//theme.dumpMenus("TEST");
%>

            <!-- Sidebar Menu Items - These collapse to the responsive navigation menu on small screens -->
            <div class="collapse navbar-collapse navbar-ex1-collapse">
                <ul class="nav navbar-nav side-nav">
                    <li>
                        <a href="<%=commonPath%>index.jsp"><i class="fa fa-fw fa-home"></i> Home</a>
                    </li>
                    <!-- Configuration -->
					<li>
						<!-- href="javascript:;" Fix for issue  CLOUD_CORE-20 -->
                        <a data-toggle="collapse" data-target="#menuConfig"><i class="fa fa-fw fa-wrench"></i> Configure <i class="fa fa-fw fa-caret-down"></i></a>
                        <ul id="menuConfig" class="collapse">
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
                            
                            <% if (com.cloud.core.services.CloudServices.getNodeConfig().isClusterEnabled() // getRunMode() == RunMode.CLUSTER
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
                    <li>
                    	<!-- href="javascript:;" -->
                        <a data-toggle="collapse" data-target="#menuPerformance"><i class="fa fa-fw fa-bar-chart-o"></i> Performance <i class="fa fa-fw fa-caret-down"></i></a>
						<ul id="menuPerformance" class="collapse">
						
                            <% for ( MenuDescriptor md : perfMenus) { %>
                            <!-- Dynamic menus for performance -->
                            <li>
                                <a href="<%=commonPath%><%=md.url%>" <%=md.urlTarget != null ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
                            </li>
                            <% } %>
                            
                            <li>
                                <a href="<%=commonPath%>jsp/perf/osp.jsp?theme=<%=theme%>">Cloud Profiler</a>
                            </li>
                            <!-- Disabled on 10/21/2017 
                            <li>
                                <a href="<%=commonPath%>jsp/perf/tr.jsp?theme=<%=theme%>" target="_new">Thread Inspector</a>
                            </li>
                            -->
                        </ul>                        
                    </li>
                    <% } %>
                    
                    <!-- Dynamic -->
                    <% for ( MenuDescriptor md : themeMgr.findMenusByParent("/")) { %>
                    <li>
                    	<%  List<MenuDescriptor> dynMenus	= themeMgr.findMenusByParent(md.label); %>
                    	<%  if ( dynMenus.size() > 0 ) { %>
                        <a data-toggle="collapse" data-target="#<%=md.menuId%>"><i class="<%=md.cssClass%>"></i> <%=md.label%> <i class="fa fa-fw fa-caret-down"></i></a>
						<ul id="<%=md.menuId%>" class="collapse">
							
							<% for ( MenuDescriptor smd : dynMenus) { %>
							<li>                    
                        		<a href="<%=commonPath%><%=smd.url%>"><%=smd.label%></a>
                        	</li>
                        	<% } %>
                        </ul>
                        <% } else { %>
                        <a href="<%=commonPath%><%=md.url%>"><i class="<%=md.cssClass%>"></i> <%=md.label%></a>
                        <% } %>
                    </li>
                    <% } %>
                    
                    <!-- System -->
                    <li>
                    	<!-- href="javascript:;" -->
                        <a data-toggle="collapse" data-target="#menuLog"><i class="fa fa-fw fa-filter"></i> System <i class="fa fa-fw fa-caret-down"></i></a>
						<ul id="menuLog" class="collapse">
						
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
            </div>
