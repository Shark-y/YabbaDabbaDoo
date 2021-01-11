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

%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="Cluster - Configure" name="title"/>
</jsp:include>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="Load Balancer" name="pageTitle"/>
		<jsp:param value="Home,Pages,Load Balancer" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>


	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>

</body>
</html>