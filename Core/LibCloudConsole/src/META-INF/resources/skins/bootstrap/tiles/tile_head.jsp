<%@page import="com.cloud.console.ThemeManager"%>
<%
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");

	String basePath = request.getParameter("basePath");
	String theme 	= request.getParameter("theme");
	String title	= request.getParameter("title");
	
	if ( basePath == null) 		basePath 	= "";
	if ( theme == null )		theme 		= ThemeManager.DEFAULT_THEME;
	if ( title == null )		title		= "Cloud Console";
	if ( commonPath == null) 	commonPath 	= "";
	
%>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
	<meta http-equiv="Cache-Control" content="NO-CACHE,NO-STORE">

	<%if (title != null) { %>
    <title><%=title%></title>
	<%} %>
    <!-- Bootstrap Core CSS -->
    <link href="<%=basePath%>css/bootstrap-common/bootstrap.css" rel="stylesheet">

    <!-- Custom CSS -->
    <link href="<%=basePath%>themes/<%=theme%>/sb-admin.css" rel="stylesheet">

    <!-- Morris Charts CSS -->
    <link href="<%=commonPath%>css/plugins/morris.css" rel="stylesheet">
	
    <!-- Custom Fonts -->
    <link href="<%=basePath%>font-awesome/css/font-awesome.css" rel="stylesheet" type="text/css">

   