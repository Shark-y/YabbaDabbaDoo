<%@page import="com.cloud.core.logging.L4JAudit"%>
<%@page import="java.util.Date"%>
<%@page import="org.apache.log4j.spi.LoggingEvent"%>
<%@page import="org.apache.log4j.helpers.CyclicBuffer"%>
<%@page import="com.cloud.core.logging.L4JSMTPAppender"%>
<%@page import="org.apache.log4j.Logger"%>
<%@page import="com.cloud.core.logging.Auditor"%>

<%!
/**
 * Format the Log4J Audit buffer
 */
static String formatAuditBuffer () {
	StringBuffer html		= new StringBuffer();
	L4JSMTPAppender smtp 	= L4JAudit.getAppender(); // (L4JSMTPAppender)Logger.getRootLogger().getAppender("AuditAppender");
	CyclicBuffer buf		= smtp != null ? smtp.getBuffer() : null; 

	if ( buf != null ) { 
		for (int i = 0; i < buf.length(); i++) {
			final LoggingEvent event 	= smtp.getBuffer().get(i);
			final String tmp			= event.getMessage().toString();
			
			// Message format SOURCE: TEXT <XML>. Split the message on SOURCE, TEXT if SOURCE:TEXT
			final int sepIdx			= tmp.indexOf(":");
			final String source 		= sepIdx > 0 ? tmp.substring(0, sepIdx) : event.getLoggerName();
			String message 				= sepIdx > 0 ? tmp.substring(sepIdx + 2, tmp.length()) : tmp;
			
			// clean XML from message (if any) or else the HTML will get messed up.
			if ( message.contains("<")) {
				message = message.substring(0, message.indexOf("<"));
			}
			
			String level 		= event.getLevel().toString().toLowerCase();
	
			if (level.equals("warn"))
				level += "ing";

			html.append("\n\t<div class=\"lv-item\">");
			html.append("\n\t\t<div class=\"media\">");
			html.append("\n\t\t<div class=\"media-body\">\n");
			html.append("\n\t\t<div class=\"lv-title\"><strong>" + source	+ "</strong> <span class=\"label label-" + level + "\">" + level + "</span></div>");
			html.append("\n\t\t<small class=\"lv-small\">" + new Date(event.getTimeStamp()) + "</small>");
			html.append("\n\t  " + (message.length() > 40 ? (message.substring(0, 40) + "...") : message) );
			html.append("\n\t\t</div>");
			html.append("\n\t\t</div>");
			html.append("\n\t</div>");
			
			
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
    <header id="header" class="clearfix" data-current-skin="blue">
    	<ul class="header-inner">
    		<li id="menu-trigger" data-trigger="#sidebar">
            	<div class="line-wrap">
                	<div class="line top"></div>
                    <div class="line center"></div>
                    <div class="line bottom"></div>
				</div>
			</li>
			
            <li class="logo"> <!-- hidden-xs -->
                <a href="#"><%=title%></a>
            </li>

			<li class="pull-right">
				<ul class="top-menu">
			    	<li id="toggle-width">
			             <div class="toggle-switch">
			                 <input id="tw-switch" type="checkbox" hidden="hidden">
			                 <label for="tw-switch" class="ts-helper"></label>
			             </div>
					</li>
					
   					<%if ( Auditor.getBufferSize() > 0) { %>
   					<!-- Notifications -->
                    <li class="dropdown">
	                     <a data-toggle="dropdown" href="">
	                         <i class="tm-icon zmdi zmdi-notifications"></i>
	                         <i class="tmn-counts"><%=Auditor.getBufferSize()%></i>
	                     </a>
	                     <div class="dropdown-menu dropdown-menu-lg pull-right">
	                         <div class="listview" id="notifications">
	                             <div class="lv-header">
	                                 Audit Notifications
	
	                                 <ul class="actions">
	                                     <li class="dropdown">
	                                         <a href="<%=commonPath%>log/logview.jsp?op=audit" target="_blank" data-clear="notification">
	                                             <i class="zmdi zmdi-view-list"></i>
	                                         </a>
	                                     </li>
	                                 </ul>
	                             </div>
	                             <div class="lv-body">
	                             
	  								<%=formatAuditBuffer()%>
	   							</div>
	   							
	   							<a target="_blank" class="lv-footer" href="<%=commonPath%>log/logview.jsp?op=audit">View All</a>
	   						</div>
	   					</div>
   					</li>
   					<%} %>
   					
                    <li class="dropdown">
                            <a data-toggle="dropdown" href=""><i class="tm-icon zmdi zmdi-more-vert"></i></a>
                            <ul class="dropdown-menu dm-icon pull-right">
                                <li class="skin-switch hidden-xs">
                                    <span class="ss-skin bgm-lightblue" data-skin="lightblue"></span>
                                    <span class="ss-skin bgm-bluegray" data-skin="bluegray"></span>
                                    <span class="ss-skin bgm-cyan" data-skin="cyan"></span>
                                    <span class="ss-skin bgm-teal" data-skin="teal"></span>
                                    <span class="ss-skin bgm-orange" data-skin="orange"></span>
                                    <span class="ss-skin bgm-blue" data-skin="blue"></span>
                                </li>
                                <li class="divider hidden-xs"></li>
                                <li class="hidden-xs">
                                    <a data-action="fullscreen" href=""><i class="zmdi zmdi-fullscreen"></i> Toggle Fullscreen</a>
                                </li>
                            </ul>
                    </li>
     			</ul>
     		</li>
     	</ul>
    </header>
    <!-- END HEADER -->

  		
