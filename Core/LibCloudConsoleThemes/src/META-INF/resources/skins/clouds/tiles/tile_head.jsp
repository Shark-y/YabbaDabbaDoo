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
	<link rel="icon" sizes="192x192" href="<%=commonPath%>img/favicon.png">
    <link type='text/css'  href="<%=basePath%>css/Material_Icons.css"  rel="stylesheet"> 
	
	<!-- Font Awesome -->
    <link href="<%=basePath%>../bootstrap/font-awesome/css/font-awesome.css" rel="stylesheet" type="text/css">

	<!-- 3/2/2019 Font Awesome v5 brands-->
    <link href="<%=basePath%>css/font-awesome-v572brands.css" rel="stylesheet" type="text/css">
    
     <!-- Core CSS with all styles -->
    <link href="<%=basePath%>css/styles.css" type="text/css" rel="stylesheet">                                    

	<!-- Code Prettifier -->
    <link href="<%=basePath%>css/prettify.css" type="text/css" rel="stylesheet">                

    <link href="<%=basePath%>css/jquery.dropdown.css" type="text/css" rel="stylesheet">            
    <link href="<%=basePath%>css/skylo.css" type="text/css" rel="stylesheet">
     
	<!-- advanced select boxes Select2 -->    
	<link href="<%=basePath%>css/select2.css" type="text/css" rel="stylesheet"> 
	
	<!-- Tokenfield -->       
	<link href="<%=basePath%>css/bootstrap-tokenfield.css" type="text/css" rel="stylesheet">   
	
	<!-- Touchspin -->
	<link href="<%=basePath%>css/jquery.bootstrap-touchspin.css" type="text/css" rel="stylesheet"> 
	
	<!-- 2/2/2019 Tooltips -->
	<link href="<%=commonPath%>css/jquery.qtip.css" type="text/css" rel="stylesheet">
	