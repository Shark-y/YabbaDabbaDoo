<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="java.util.Date"%>
<%@page import="org.apache.log4j.spi.LoggingEvent"%>
<%@page import="org.apache.log4j.helpers.CyclicBuffer"%>
<%@page import="com.cloud.core.logging.L4JSMTPAppender"%>
<%@page import="org.apache.log4j.Logger"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.logging.L4JAudit"%>

<%!
/**
 * Format the Log4J Audit buffer
 */
static String formatAuditBuffer () {
	StringBuffer html		= new StringBuffer();
	L4JSMTPAppender smtp 	= L4JAudit.getAppender(); //(L4JSMTPAppender)Logger.getRootLogger().getAppender("AuditAppender");
	CyclicBuffer buf		= smtp != null ? smtp.getBuffer() : null; 

	if ( buf != null ) { 
		for (int i = 0; i < buf.length(); i++) {
			final LoggingEvent event 	= smtp.getBuffer().get(i);
			final String tmp			= event.getMessage().toString();
			
			// Split the message on SOURCE, TEXT if SOURCE:TEXT
			final int sepIdx			= tmp.indexOf(":");
			final String source 		= sepIdx > 0 ? tmp.substring(0, sepIdx) : event.getLoggerName();
			final String message 		= sepIdx > 0 ? tmp.substring(sepIdx + 2, tmp.length()) : tmp;
			
			String level 		= event.getLevel().toString().toLowerCase();
	
			if (level.equals("warn"))
				level += "ing";
			
			html.append("\n\t<li class=\"media notification-" + level + "\">");
			html.append("\n\t\t<a href=\"#\">");
			html.append("\n\t\t<div class=\"media-body\">\n");
			html.append("\n\t\t<h4 class=\"notification-heading\">" 
					+ "<span class=\"label label-" + level + "\">" + level + "</span>"					
					+ " " + source + " "
					+ (message.length() > 40 ? (message.substring(0, 40) + "...") : message)
					+ "</h4>");
			html.append("\n\t\t<span class=\"notification-time\">" + new Date(event.getTimeStamp()) + "</span>");
			html.append("\n\t\t</div>");
			html.append("\n\t</a>");
			html.append("\n\t</li>");
		}
	}
	return html.toString();
}
%>

<%
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
	String basePath 	= request.getParameter("basePath");
	String title		= request.getParameter("title");		// page top left title
	
	if ( basePath == null) 		basePath = "";
	if ( title == null)			title = "";
	if ( commonPath == null) 	commonPath 	= "../../";	
	
%>

    <!-- START HEADER -->
    <header id="topnav" class="navbar navbar-default navbar-fixed-top" role="banner">
		<div class="logo-area">
			<a class="navbar-brand navbar-brand-primary" href="#" onclick="return false">
				
				<img class="show-on-collapse img-logo-white" alt="Paper" src="<%=basePath%>img/logo-icon-white.png">
				<img class="show-on-collapse img-logo-dark" alt="Paper" src="<%=basePath%>img/logo-icon-dark.png">
				
				<img class="img-white" alt="Paper" src="<%=basePath%>img/logo-white.png">
				<img class="img-dark" alt="Paper" src="<%=basePath%>img/logo-dark.png">
			</a>
	
			<span id="trigger-sidebar" class="toolbar-trigger toolbar-icon-bg stay-on-search">
				<a data-toggle="tooltips" data-placement="right" title="Toggle Sidebar">
					<span class="icon-bg">
						<i class="material-icons">menu</i>
					</span>
				</a>
			</span>
			
			<!-- 3/10/2019 vsilva Search box:  Utility.toggle_search(); 
			<span id="trigger-search" class="toolbar-trigger toolbar-icon-bg ov-h">
				<a data-toggle="tooltips" data-placement="right" title="Toggle Search bar">
					<span class="icon-bg">
						<i class="material-icons">search</i>
					</span>
				</a>
			</span>
			class="form-control"  -->
			<div id="search-box" style="display: none; padding-top: 20px">
				<input type="text" placeholder="Search..." id="search-input">
			</div>
			
		</div><!-- logo-area -->
    
    	 
    	<ul class="nav navbar-nav toolbar pull-right">
    		<!-- 3/10/2019 close search 
			<li class="toolbar-icon-bg appear-on-search ov-h" id="trigger-search-close">
		        <a class="toggle-fullscreen">
		        	<span class="icon-bg"><i class="material-icons">close</i></span>
		        </a>
		    </li>
			-->
			<li class="toolbar-icon-bg hidden-xs" id="trigger-fullscreen">
		        <a href="#" class="toggle-fullscreen"><span class="icon-bg">
		        	<i class="material-icons">fullscreen</i>
		        </span></a>
		    </li>

			<%if ( Auditor.getBufferSize() > 0) { %>
			<!-- Notifications -->
			<li class="dropdown toolbar-icon-bg">
				<a href="#" class="hasnotifications dropdown-toggle" data-toggle='dropdown'><span class="icon-bg"><i class="material-icons">notifications</i></span><span class="badge badge-danger"><%=Auditor.getBufferSize()%></span></a>
				<div class="dropdown-menu animated notifications">
					<div class="topnav-dropdown-header">
						<span><%=Auditor.getBufferSize()%> new notifications</span>
					</div>
					<div class="scroll-pane">
						<ul class="media-list scroll-content">
						<%=formatAuditBuffer()%>
						</ul>
					</div>
					<div class="topnav-dropdown-footer">
						<a href="<%=commonPath%>log/logview.jsp?op=audit" target="_blank">See all notifications</a>
					</div>
				</div>
			</li>
			<% } %>
			
			<!-- Profile -->
	        <li class="dropdown toolbar-icon-bg hidden-xs">
				<a href="#" class="hasnotifications dropdown-toggle" data-toggle='dropdown'><span class="icon-bg"><i class="material-icons">more_vert</i></span><span
				class="badge badge-info"></span></a>
				<div class="dropdown-menu animated notifications">
					<div class="topnav-dropdown-header">
						<span>Profile</span>
					</div>
					<div class="scroll-pane">
						<ul class="media-list scroll-content">
							<li class="media notification-success">
								<a href="<%=commonPath%>login.jsp?action=loginshow&r=index.jsp">
									<div class="media-left">
										<i class="material-icons">perm_contact_calendar</i>
									</div>
									<div class="media-body">
										<h4>Login</h4>
									</div>
								</a>
							</li>
							<li class="media notification-teal">
								<a href="<%=commonPath%>login.jsp?action=logout">
									<div class="media-left">
										<i class="material-icons">exit_to_app</i>
									</div>
									<div class="media-body">
										<h4>Log Out</h4>
									</div>
								</a>
							</li>
							<% if ( Rbac.isConsoleAdmin(session)) { %>
							<li class="media notification-message">
								<a href="<%=commonPath%>login.jsp?action=changepwd&r=index.jsp">
									<div class="media-left">
										<i class="material-icons">settings</i>
									</div>
									<div class="media-body">
										<h4>Password</h4>
									</div>
								</a>
							</li>
							<% } %>
						</ul>
					</div>
				</div>
			</li>
     		
     	</ul>
     	
     	
    </header>
    <!-- END HEADER -->

  		
