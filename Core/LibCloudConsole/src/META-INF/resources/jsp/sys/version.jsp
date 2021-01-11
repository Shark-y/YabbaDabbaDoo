
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="java.io.StringReader"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page import="java.io.ByteArrayInputStream"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.util.Properties"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.core.services.ProfileManager.ProfileDescriptor"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.ProfileManager"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>

<%!

static void LOGD(String text) {
	System.out.println("[VER-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[VER-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[VER-ERR] " +text);
}

static String formatMessageError(String text) {
	return "<font color=red size=4>" + text + "</font>";
}

/**
 * Get Build information from the clas path @ /version.ini
 * Format: commit={hash}|branch={branch}|date={date}
 */
static Properties getBuildInfo (Class<?> cls) {
	Properties info 	= new Properties();
	InputStream is		= cls.getResourceAsStream("/version.ini");
	if ( is == null ) 	return null;
	try {
		// convert | to \n & load
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOTools.pipeStream(is, out);
		out.close();
		String temp = out.toString().replaceAll("\\|", "\n");
		info.load(new StringReader(temp));
	}
	catch ( Exception e) {
	}
	finally {
		IOTools.closeStream(is);
	}
	return info;
}

%>

<%
	String theme			= (String)session.getAttribute("theme");
	String title			= (String)session.getAttribute("title");
%>

<%
	// Session expired or user trying to bypass the home page? back to HOME. 
	if (theme == null || title == null) {
		response.sendRedirect("../../");
		return;		
	}
%>

<%
	NodeConfiguration cfg	= CloudServices.getNodeConfig();
	String nodeVersion		= HTTPServerTools.getNodeVersion();
	Package[] pkgs 			= Package.getPackages();
	List<String> titles		= new ArrayList<String>();		// to remove duplicate pkg titles
	List<String> names		= new ArrayList<String>();		// to sort pkgs by name

	Properties info			= getBuildInfo(getClass());
	
	LOGD("Build-Version: " + info);
	
	for ( Package pkg : pkgs)  {
		final String name	= pkg.getName();
		final String vendor = pkg.getImplementationVendor();
		boolean hasSpec		= false;
		
		if ( /*vendor == null || */ name.contains("sun") ) {
			continue;
		} 
		if ( pkg.getSpecificationTitle() != null && !titles.contains(pkg.getSpecificationTitle())) {
			titles.add(pkg.getSpecificationTitle());
			names.add(pkg.getName());
			hasSpec = true;
		}
		// no spec? try impl
		if ( hasSpec ) {
			continue;
		}
		if ( pkg.getImplementationTitle() != null && !titles.contains(pkg.getSpecificationTitle())) {
			titles.add(pkg.getSpecificationTitle());
			names.add(pkg.getName());
		}
	}
	Collections.sort(names);
%>


<!DOCTYPE html>
<html lang="en">

<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
</jsp:include>

<script>
</script>

</head>

<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Version Information" name="pageTitle"/>
		<jsp:param value="Home,Pages,Version Information" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
             <!-- /.row -->

			<% if ( info != null) { %>
             <div class="row">
                 <div class="col-lg-12">
                 	<table>
                 		<tr><td><b>Build Branch</b></td><td><%=info.getProperty("branch") %></td></tr>
                 		<tr><td><b>Build Revision</b>&nbsp;&nbsp;</td><td><%=info.getProperty("commit") %></td></tr>
                 		<tr><td><b>Build Date</b>&nbsp;&nbsp;</td><td><%=info.getProperty("date") %></td></tr>
                 		<tr><td><b>Node Name</b></td><td><%=getServletContext().getContextPath().substring(1) %></td></tr>
                 		<tr><td><b>Node Version</b></td><td><%=(nodeVersion != null ? nodeVersion : "Not available")%></td></tr>
                 	</table>
                 	<p>&nbsp;</p>
                 </div>
             </div>
             <!-- /.row -->
			<% } %>
			
             <div class="row">
                 <div class="col-lg-12">
				                        
					<table class="table table-bordered table-hover table-striped uk-table">
						<tr>
							<th>Specification</th>
							<th>Version</th>
							<th>Build</th>
							<th>Vendor</th>
						</tr>
						<%  for ( String name: names)  {
							final Package pkg 	= Package.getPackage(name);
							final String vendor = pkg.getImplementationVendor();
						%>
							
							<tr>
								<!-- <td><%=pkg.getName() %></td> -->
								<td><%=pkg.getSpecificationTitle() != null ? pkg.getSpecificationTitle() : pkg.getImplementationTitle()%></td>
								<td><%=pkg.getSpecificationVersion() != null ? pkg.getSpecificationVersion() : pkg.getImplementationVersion() %></td>
								<td><%=pkg.getImplementationVersion() != null ? pkg.getImplementationVersion() : ""%></td>
								<td><%=vendor%></td>
								
							</tr>
						<%}%>					
					</table>
                     
                 </div>
             </div>
             <!-- /.row -->


			<!-- put some space @ the bottom --> 
			<div class="row" style="height: 200px;">
				&nbsp;
			</div>
            <!-- /.row -->


	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>

</body>

</html>
