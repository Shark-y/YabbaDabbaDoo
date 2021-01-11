<%@page import="java.util.Date"%>
<%@page import="org.apache.log4j.spi.LoggingEvent"%>
<%@page import="com.cloud.core.logging.L4JSMTPAppender"%>
<%@page import="com.cloud.core.logging.L4JAudit"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%!
/**
 * Format the buffer for display as HTML (Using bootstrap)
 * 
 * @return Bootstrap HTML.
 */
static String formatAuditBuffer() {
	StringBuffer sbuf 		= new StringBuffer();
	L4JSMTPAppender smtp 	= L4JAudit.getAppender(); //(L4JSMTPAppender) appender;
	int len 				= smtp.getBuffer().length();
	
	for (int i = 0; i < len; i++) {
		final LoggingEvent event 	= smtp.getBuffer().get(i);
		final String tmp			= event.getMessage().toString();
		
		// Split the message on SOURCE, TEXT if SOURCE:TEXT
		final int sepIdx			= tmp.indexOf(":");
		final String source 		= sepIdx > 0 ? tmp.substring(0, sepIdx) : event.getLoggerName();
		final String message 		= sepIdx > 0 ? tmp.substring(sepIdx + 2, tmp.length()) : tmp;
		
		String level 		= event.getLevel().toString().toLowerCase();

		if (level.equals("warn"))
			level += "ing";

		sbuf.append("\t<li class=\"message-preview\">\n");
		sbuf.append("\t  <a href=\"#\">\n");
		sbuf.append("\t\t<div class=\"media\"><div class=\"media-body\">\n");
		sbuf.append("\t\t<h5 class=\"media-heading\"><strong>" + source	
				+ "</strong> <span class=\"label label-" + level + "\">"
				+ level + "</span></h5>\n");
		sbuf.append("\t\t<p class=\"small text-muted\"><i class=\"fa fa-clock-o\"></i> "
				+ new Date(event.getTimeStamp()) + "</p>\n");
		sbuf.append("\t  <p>" + Auditor.cleanXML(message) + "</p>\n");
		sbuf.append("\t</div></div></a></li>\n");
	}

	return sbuf.toString();
}

%>

<%
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");

	String basePath 	= request.getParameter("basePath");
	String title		= request.getParameter("title");		// page top left title

	if ( commonPath == null) 	commonPath 	= "";
	if ( basePath == null) 		basePath 	= "";
	if ( title == null)			title 		= "";
%>

            <!-- Brand and toggle get grouped for better mobile display -->
			
            <div class="navbar-header">
			
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-ex1-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <div class="navbar-brand"><!-- <i class="fa fa-lg fa-cloud"></i>--> <%=title%></div>
            </div>
			
			
            <!-- Top Menu Items -->
            <ul class="nav navbar-right top-nav">
            	<%if ( Auditor.getBufferSize() > 0) { %>
				<!-- Audit Items -->
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-lg fa-envelope"></i> <b class="caret"></b></a>
                    <ul class="dropdown-menu message-dropdown">
						
						<%=formatAuditBuffer()%>
						
                        <li class="message-footer">
                            <a href="<%=commonPath%>log/logview.jsp?op=audit" target="_blank">View All</a>
                        </li>
                    </ul>
                </li>
				<% } %>
				
            	<!-- Administer Menu -->
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-lg fa-user"></i> <b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <li>
                            <a href="<%=commonPath%>login.jsp?action=loginshow&r=index.jsp"><i class="fa fa-fw fa-user"></i> Login</a>
                        </li>
                        <li>
                            <a href="<%=commonPath%>login.jsp?action=logout"><i class="fa fa-fw fa-power-off"></i> Log Out</a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a href="<%=commonPath%>login.jsp?action=changepwd&r=index.jsp"><i class="fa fa-fw fa-gear"></i> Password</a>
                        </li>
                    </ul>
                </li>
            </ul>

            