<%@page import="com.cloud.console.ThemeManager"%>
<%
	String basePath = request.getParameter("basePath");
	String theme 	= request.getParameter("theme");
	String title	= request.getParameter("title");
	
	if ( basePath == null) 	basePath 	= "";
	if ( theme == null )	theme 		= ThemeManager.DEFAULT_THEME;
	if ( title == null )	title		= "Cloud Console";
%>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="msapplication-tap-highlight" content="no">

	<%if (title != null) { %>
    <title><%=title%></title>
	<%} %>
	
    <!-- CORE CSS-->
	<link href="<%=basePath%>css/animate.css" rel="stylesheet">
	<link href="<%=basePath%>css/sweet-alert.css" rel="stylesheet">
	<link href="<%=basePath%>css/material-design-iconic-font.css" rel="stylesheet">
	<link href="<%=basePath%>css/jquery.mCustomScrollbar.css" rel="stylesheet">        
	     
	 <!-- CSS -->
	<link href="<%=basePath%>css/app.min.1.css" rel="stylesheet">
	<link href="<%=basePath%>css/app.min.2.css" rel="stylesheet">
    
	
