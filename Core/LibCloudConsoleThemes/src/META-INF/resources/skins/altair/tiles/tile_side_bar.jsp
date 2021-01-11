<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
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

	// lcal res base path
	String basePath 	= request.getParameter("basePath");
	String active 		= request.getParameter("active");
	String theme 		= (String) session.getAttribute("theme");

	boolean loggedIn 	= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;

	if (theme == null)		theme = ThemeManager.DEFAULT_THEME;
	if (basePath == null)	basePath = "";
	if (active == null)		active = "";

	List<ServiceDescriptor> list = CloudServices.getNodeConfig().getServiceDescriptors();
	ThemeManager themeMgr = ThemeManager.getInstance();
	themeMgr.load(list);

	List<MenuDescriptor> perfMenus = themeMgr.findMenusByParent("Performance");

	//theme.dumpMenus("TEST");
%>

   <!-- main sidebar -->
    <aside id="sidebar_main">
        
        <div class="sidebar_main_header">
            <div class="sidebar_logo">
                <a href="index.html" class="sSidebar_hide"><img src="<%=basePath%>img/logo_main.png" alt="" height="15" width="71"/></a>
                <a href="index.html" class="sSidebar_show"><img src="<%=basePath%>img/logo_main_small.png" alt="" height="32" width="32"/></a>
            </div>
            <!-- 
            <div class="sidebar_actions">
                <select id="lang_switcher" name="lang_switcher">
                    <option value="gb" selected>English</option>
                </select>
            </div>
            -->
        </div>
        
        <div class="menu_section">
            <ul>
					<% if ( !themeMgr.isMarkedAsHidden("Home")) { %>
					<li>
						<a href="<%=commonPath%>index.jsp"><span class="menu_icon"><i class="material-icons">home</i></span><span class="menu_title">Home</span></a>
					</li>
					<% } %>
                    <!-- Configuration -->
                    <% if ( !themeMgr.isMarkedAsHidden("Configure") && Rbac.canConfigureServices(session) ) { %>
					<li>
                        <a  href="javascript:;">
                        	<span class="menu_icon"><i class="material-icons">settings</i></span>
                        	<span class="menu_title">Configure</span>
                        </a>
                        <ul id="menuConfig">
                            <li>
                                <a href="<%=commonPath%>jsp/config/config_node.jsp">Node</a>
                            </li>
                            
                            <!-- Vendor configs -->
	                        <% for ( ServiceDescriptor desc : list) { 
	                        	if (desc.getConfigFileName() != null && !desc.getConfigFileName().isEmpty() && !themeMgr.isMarkedAsHidden(desc.getVendorName())) { 
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
                                <a href="<%=commonPath%><%=md.url%>" <%=md.urlTarget != null && !md.urlTarget.isEmpty() ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
                            </li>
                            <% } %>
                            
                            <% if (com.cloud.core.services.CloudServices.getNodeConfig().isClusterEnabled() // getRunMode() == RunMode.CLUSTER
                            	/*|| com.cloud.core.services.CloudServices.getNodeConfig().getFailOverType() == FailOverType.CLUSTER*/
                            	) { %>
                            <li>
                                <a class="withripple" href="<%=commonPath%>jsp/config/config_cluster.jsp?leftnav=1">Cluster</a>
                            </li>
                            <%} %>
						</ul>
	                </li>
					<% } %>
					
					<!-- Performance -->
					<% if ( !themeMgr.isMarkedAsHidden("Performance")) { %>
                    <li>
                        <a class="withripple" href="javascript:;">
                        	<span class="menu_icon"><i class="material-icons">dashboard</i></span>
                        	<span class="menu_title">Dashboards</span>
                        </a>
						<ul id="menuPerformance" class="acc-menu">
						
                            <% for ( MenuDescriptor md : perfMenus) { %>
                            <!-- Dynamic menus for performance -->
                            <li>
                                <a class="withripple" href="<%=commonPath%><%=md.url%>" <%=md.urlTarget != null && !md.urlTarget.isEmpty() ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
                            </li>
                            <% } %>
                            <% if ( !themeMgr.isMarkedAsHidden("Cloud Profiler")) { %>
                            <li>
                                <a class="withripple" href="<%=commonPath%>jsp/perf/osp.jsp?theme=<%=theme%>">Cloud Profiler</a>
                            </li>
                            <% } %>
                        </ul>                        
                    </li>
                    <% } %>
                    
                    <!-- Dynamic 
                    <% for ( MenuDescriptor md : themeMgr.findMenusByParent("/")) { %>
                    <li>
                        <a class="withripple" href="<%=commonPath%><%=md.url%>"><span class="menu_icon"><i class="material-icons"><%=md.cssClass%></i></span><span><%=md.label%></span></a>
                    </li>
                    <% } %>
					-->
                    
                  		<!-- Dynamic Menu/Sub-menu -->
                    <% for ( MenuDescriptor md : themeMgr.findMenusByParent("/")) { %>
                    <li>
                    	<%  List<MenuDescriptor> dynMenus	= themeMgr.findMenusByParent(md.label); %>
                    	<%  if ( dynMenus.size() > 0 ) { %>
                        <a class="withripple" href="javascript:;">
                        	<span class="menu_icon"><i class="<%=(md.cssClass.startsWith("fa") ? md.cssClass : "material-icons")%>"><%=(md.cssClass.startsWith("fa") ? "" : md.cssClass)%></i></span> 
                        	<span class="menu_title"><%=md.label%></span>
                        </a>
						<ul id="<%=md.menuId%>" class="acc-menu">
							
							<% for ( MenuDescriptor smd : dynMenus) { %>
							<li>                    
                        		<a class="withripple" href="<%=commonPath%><%=smd.url%>"><%=smd.label%></a>
                        	</li>
                        	<% } %>
                        </ul>
                        <% } else { %>
                        <a class="withripple" href="<%=commonPath%><%=md.url%>"><span class="icon"><i class="<%=(md.cssClass.startsWith("fa") ? md.cssClass : "material-icons")%>"><%=(md.cssClass.startsWith("fa") ? "" : md.cssClass)%></i></span><span><%=md.label%></span></a>
                        <% } %>
                    </li>
                    <% } %>
                    
                    <% if ( !themeMgr.isMarkedAsHidden("System") && Rbac.canConfigureServices(session) ) { %>
                    <!-- System -->
                    <li>
                        <a class="withripple" href="javascript:;">
                        	<span class="menu_icon"><i class="material-icons">security</i></span>
                        	<span class="menu_title">System</span>
                        </a>
						<ul id="menuLog" class="acc-menu">
						
						    <% for ( MenuDescriptor md : themeMgr.findMenusByParent("System")) { %>
                            <!-- Dynamic menus for System -->
                            <li>
                                <a class="withripple" href="<%=commonPath%><%=md.url%>" <%=md.urlTarget != null && !md.urlTarget.isEmpty() ? "target=" + md.urlTarget : "" %>><%=md.label%></a>
                            </li>
                            <% } %>
						
                            <li>
                                <a class="withripple" href="<%=commonPath%>log/logview.jsp" target="_blank"> Log View</a>
                            </li>
                            <li>
                                <!-- 11/6/2017 <a class="withripple" href="<%=commonPath%>log/log4j.jsp" target="_blank"> Log Filters</a> -->
                                <a class="withripple" href="<%=commonPath%>log/browser.jsp" target="_blank"> Log History</a>
                            </li>
                            <li>
                                <a class="withripple" href="<%=commonPath%>jsp/sys/version.jsp"> Version</a>
                            </li>
                        </ul>                        
                    </li>
					<% } %>               
            </ul>
        </div> <!--  menu_section -->
    </aside> <!-- main sidebar end -->
