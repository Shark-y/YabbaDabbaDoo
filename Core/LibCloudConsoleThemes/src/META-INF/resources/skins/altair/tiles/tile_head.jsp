<%@page import="com.cloud.console.ThemeManager"%>
<%
	String basePath 	= request.getParameter("basePath");
	String theme 		= request.getParameter("theme");
	String title		= request.getParameter("title");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");

	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	
	if ( theme == null )		theme 		= ThemeManager.DEFAULT_THEME;
	if ( title == null )		title		= "Cloud Console";
	
	
%>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="msapplication-tap-highlight" content="no">

	<!-- 8/2/2019 No cache -->
	<meta http-equiv="cache-control" content="no-cache" />
	
	<%if (title != null) { %>
    <title><%=title%></title>
	<%} %>
	<link rel="icon" sizes="192x192" href="<%=commonPath%>img/favicon.ico">
	
    <!-- metrics graphics (charts) -->
    <link rel="stylesheet" href="<%=basePath%>css/bower_components_metrics-graphics_dist_metricsgraphics.css">
    
    <!-- chartist 
	<link rel="stylesheet" href="<%=basePath%>css/bower_components_chartist_dist_chartist.min.css">
    -->
    <!-- uikit -->
    <link rel="stylesheet" href="<%=basePath%>css/bower_components_uikit_css_uikit.almost-flat.min.css" media="all">

    <!-- flag icons -->
    <link rel="stylesheet" href="<%=basePath%>css/assets_icons_flags_flags.css" media="all">

    <!-- altair admin -->
    <link rel="stylesheet" href="<%=basePath%>css/assets_css_main.css" media="all">

   <link rel="stylesheet" href="<%=basePath%>css/jquery.bootstrap-touchspin.css" media="all">

	<!-- 2/2/2019 Tooltips -->
	<link href="<%=commonPath%>css/jquery.qtip.css" type="text/css" rel="stylesheet">
	
	<!-- data tables -->
	<link rel="stylesheet" type="text/css" href="<%=commonPath%>css/jquery.dataTables.css">
	
	