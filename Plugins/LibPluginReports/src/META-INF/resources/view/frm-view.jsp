<%@page import="com.cloud.core.services.PluginSystem.Plugin"%>
<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.rts.jsp.AggregatesManager.Aggregate"%>
<%@page import="com.rts.jsp.AggregatesManager"%>
<%@page import="com.rts.ui.Dashboard"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.rts.service.RealTimeStatsService"%>
<%@page import="com.rts.jsp.FrameSetManager.Frame"%>
<%@page import="com.rts.jsp.FrameSetManager.FrameSet"%>
<%@page import="com.rts.jsp.FrameSetManager"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>

<%
	final String 	name 		= request.getParameter("name");
	FrameSetManager mgr  		= FrameSetManager.getInstance();
	AggregatesManager amgr		= AggregatesManager.getInstance();
	
	FrameSet fs					= mgr.find(name);		// Cannot be NULL.

	// 6/5/2020 Plugin support
	final String mode			= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id				= request.getParameter("id");		// service id
	
	RealTimeStatsService service = null;
	if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
		ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
		Plugin p 				= PluginSystem.findInstance(sd.getClassName());
		service 				= (RealTimeStatsService )p.getInstance();
	}
	else {
		service = (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
	}
%>
<!DOCTYPE html>
<html>
<head>

<title><%=name%></title>

<style>
html, body {
	margin: 0;
	padding:0;
	border:0;
	width: 100%;
	height: 100%;
}
iframe {
	border: 0;
	
	/* remove spacing between frames */
	/*display: table-cell; */
	display:block;
    float:left;
	
	/* center
	display: block;
	margin-left: auto;
	margin-right: auto; */
	
	/* shadow, rounded corners, and rotated it 20 degrees https://www.thoughtco.com/iframes-and-css-3468669 */
	/*
	margin-top: 15px;
	margin-bottom: 15px; 
	-moz-border-radius: 12px;
	-webkit-border-radius: 12px;
	border-radius: 12px;
	-moz-box-shadow: 4px 4px 14px #000;
	-webkit-box-shadow: 4px 4px 14px #000;
	box-shadow: 4px 4px 14px #000; */
	
}
</style>

</head>
<body>

<% 
for ( Frame frm : fs.getFrames())  {
	Dashboard dash = service.getDashboards().find(frm.getDashboard());
	if ( dash != null) {
%>

<iframe style="<%=frm.getStyle()%>" src="../dash/dash-view-lp.jsp?id=<%=id%>&mode=<%=mode%>&name=<%=dash.getListener()%>&key=<%=dash.getKey()%>&title=<%=dash.getTitle()%>"></iframe>

<% 	} 
	else {
		Aggregate ag = amgr.find(frm.getDashboard());
		if ( ag != null ) {
%>
<iframe style="<%=frm.getStyle()%>" src="../view/agr-view.jsp?id=<%=id%>&mode=<%=mode%>&name=<%=ag.getName()%>"></iframe>

<% 
		}
		else {
			out.print("<h3>Invalid or missing dahsboard " + frm.getDashboard() + "</h3>");
		}
	}
}
%>

</body>
</html>