<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	final String contextPath 	= getServletContext().getContextPath();
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	//String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	final String basePath		= "../";
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(basePath) %>">
	<jsp:param value="<%=SkinTools.buildBasePath(basePath) %>" name="basePath"/>
	<jsp:param value="<%=basePath%>" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="View Title" name="title"/>
</jsp:include>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(basePath) %>">
		<jsp:param value="<%=SkinTools.buildBasePath(basePath) %>" name="basePath"/>
		<jsp:param value="<%=basePath%>" name="commonPath"/>
		<jsp:param value="Sample Heading" name="pageTitle"/>
		<jsp:param value="Home,Pages,Sample View" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(basePath)%>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>

	<!-- CONTENT START -->
	<jsp:include page="tile_icons.jsp" />
	<!-- CONTENT END -->
	
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(basePath) %>">
		<jsp:param value="<%=SkinTools.buildBasePath(basePath) %>" name="basePath"/>
		<jsp:param value="<%=basePath%>" name="commonPath"/>
	</jsp:include>

	<script type="text/javascript">
	
	$(document).ready(function() {
	});
	
	</script>

</body>
</html>