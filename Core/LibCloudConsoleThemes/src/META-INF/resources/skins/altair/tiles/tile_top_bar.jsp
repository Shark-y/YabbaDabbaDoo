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
			
			html.append("\n\t<li>");
			html.append("\n\t\t<div class=\"md-list-addon-element\">");
			html.append("\n\t\t\t<i class=\"md-list-addon-icon material-icons uk-text-" + level + "\">&#xE88F;</i>");
			html.append("\n\t\t</div>");
			html.append("\n\t\t<div class=\"md-list-content\">\n");
			html.append("\n\t\t" 
					+ "<span class=\"md-list-heading\">" + level + " " +  source  + "</span>"					
					+ "<span class=\"uk-text-small uk-text-muted uk-text-truncate\">" + new Date(event.getTimeStamp())
					+ (message.length() > 40 ? (message.substring(0, 40) + "...") : message)
					+ "</span>"
					);
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

   <!-- main header -->
    <header id="header_main">
        <div class="header_main_content">
            <nav class="uk-navbar">
                                
                <!-- main sidebar switch -->
                <a href="#" id="sidebar_main_toggle" class="sSwitch sSwitch_left">
                    <span class="sSwitchIcon"></span>
                </a>
                
                <!-- secondary sidebar switch 
                <a href="#" id="sidebar_secondary_toggle" class="sSwitch sSwitch_right sidebar_secondary_check">
                    <span class="sSwitchIcon"></span>
                </a>
                -->
                <div id="search-box" class="uk-float-left header_main_search_form uk-width-1-2">
	            	<i class="md-icon header_main_search_close material-icons" onclick="$('#search-input').val('');$('#search-input').focus()">&#xE5CD;</i>
		            <form class="uk-form">
		                <input id="search-input" type="text" class="header_main_search_input uk-width-1-1" />
		                <input id="search-input-node" type="hidden" />
		                <button class="header_main_search_btn uk-button-link" onclick="return main_search_click()"><i class="md-icon material-icons">&#xE8B6;</i></button>
		            </form>
	        	</div>
 				
                <div class="uk-navbar-flip">
                    <ul class="uk-navbar-nav user_actions">
                    	<!-- search  
                    	<li>
		                    <div id="search-box" class="header_main_search_form">
					            <i class="md-icon header_main_search_close material-icons">&#xE5CD;</i>
					            <form class="uk-form">
					                <input id="search-input" type="text" class="header_main_search_input" />
					                <button class="header_main_search_btn uk-button-link"><i class="md-icon material-icons">&#xE8B6;</i></button>
					            </form>
					        </div>
                    	</li>
                    	-->
						<!-- full screen -->
                        <li><a href="#" id="full_screen_toggle" class="user_action_icon uk-visible-large"><i class="material-icons md-24 md-light">&#xE5D0;</i></a></li>
                        
						<!-- search 
                        <li><a href="#" id="main_search_btn" class="user_action_icon"><i class="material-icons md-24 md-light">&#xE8B6;</i></a></li>
						-->
						
						<%if ( Auditor.getBufferSize() > 0) { %>
						<!-- messages /alerts -->
                        <li data-uk-dropdown="{mode:'click',pos:'bottom-right'}">
                            <a href="#" class="user_action_icon"><i class="material-icons md-24 md-light">&#xE7F4;</i><span class="uk-badge"><%=Auditor.getBufferSize()%></span></a>
                            <div class="uk-dropdown uk-dropdown-xlarge">
                                <div class="md-card-content">
                                    <ul class="uk-tab uk-tab-grid" data-uk-tab="{connect:'#header_alerts',animation:'slide-horizontal'}">
                                        <li class="uk-width-1-2 uk-active"><a href="#" class="js-uk-prevent uk-text-small">Messages (<%=Auditor.getBufferSize()%>)</a></li>
                                        <!-- 
                                        <li class="uk-width-1-2"><a href="#" class="js-uk-prevent uk-text-small">Alerts (4)</a></li>
                                        -->
                                    </ul>
                                    <ul id="header_alerts" class="uk-switcher uk-margin">
                                    	<!-- Messages -->
                                        <li>
                                            <ul class="md-list md-list-addon">
                                            	<%=formatAuditBuffer()%>
                                            	<!-- 
                                                <li>
                                                    <div class="md-list-addon-element">
                                                        <span class="md-user-letters md-bg-cyan">yd</span>
                                                    </div>
                                                    <div class="md-list-content">
                                                        <span class="md-list-heading"><a href="pages_mailbox.html">Enim neque est.</a></span>
                                                        <span class="uk-text-small uk-text-muted">Fugit non sit neque commodi molestiae quia nobis quia.</span>
                                                    </div>
                                                </li>
                                                -->
                                            </ul>
                                            <div class="uk-text-center uk-margin-top uk-margin-small-bottom">
                                                <a href="page_mailbox.html" class="md-btn md-btn-flat md-btn-flat-primary js-uk-prevent">Show All</a>
                                            </div>
                                        </li>
										<!-- alerts -->
										<!-- 
                                        <li>
                                            <ul class="md-list md-list-addon">
                                                <li>
                                                    <div class="md-list-addon-element">
                                                        <i class="md-list-addon-icon material-icons uk-text-warning">&#xE8B2;</i>
                                                    </div>
                                                    <div class="md-list-content">
                                                        <span class="md-list-heading">Voluptates assumenda.</span>
                                                        <span class="uk-text-small uk-text-muted uk-text-truncate">Necessitatibus non vel magnam neque.</span>
                                                    </div>
                                                </li>
                                            </ul>
                                        </li>
                                        -->
                                    </ul>
                                </div>
                            </div>
                        </li>
                        <% } %>
                        
                        <li data-uk-dropdown="{mode:'click',pos:'bottom-right'}">
                            <a href="#" class="user_action_icon"> <i class="material-icons md-24 md-light">person</i> </a>
                            <div class="uk-dropdown uk-dropdown-small">
                                <ul class="uk-nav js-uk-prevent">
                                    <li><a href="<%=commonPath%>login.jsp?action=loginshow&r=index.jsp">Login</a></li>
                                    <li><a href="<%=commonPath%>login.jsp?action=logout">Logout</a></li>
                                    <% if ( Rbac.isConsoleAdmin(session)) { %>
                                    <li><a href="<%=commonPath%>login.jsp?action=changepwd&r=index.jsp">Password</a></li>
                                    <% } %>
                                </ul>
                            </div>
                        </li>
                    </ul>
                </div>
				
				
            </nav>
        </div>
        <!-- 
        <div class="header_main_search_form">
            <i class="md-icon header_main_search_close material-icons">&#xE5CD;</i>
            <form class="uk-form">
                <input type="text" class="header_main_search_input" />
                <button class="header_main_search_btn uk-button-link"><i class="md-icon material-icons">&#xE8B6;</i></button>
            </form>
        </div>
        -->
    </header>
    <!-- main header end -->

  		
