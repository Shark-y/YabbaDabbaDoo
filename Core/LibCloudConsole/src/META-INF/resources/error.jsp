<%@page import="java.io.File"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.io.StringWriter"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<%!
/*
 * Convert an exception to a String
 */
String exceptionToString(Exception ex) {
	StringWriter errors = new StringWriter();
	ex.printStackTrace(new PrintWriter(errors));
	return /*errors*/ ex.toString();	// 8/28/2020 Pen-Tets
}
%>   

<%
	final String alertClass 	= "alert-danger";
	final Exception ex			= (Exception)getServletContext().getAttribute(CloudServices.CTX_STARTUP_EXCEPTION);
	final Exception ex1			= (Exception)getServletContext().getAttribute(CloudServices.CTX_STARTUP_EXCEPTION + "-CLONE");
	
	final String baseHome		= System.getProperty("user.home") + File.separator + ".cloud";
	final File	home			= new File(baseHome);
	
	if ( ex == null && ex1 == null) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return;
	}
	String statusMsg 			= request.getParameter("statusMsg");

	if ( statusMsg == null )	statusMsg = "An unrecoverable error has occured.";
	
	// append log link
	statusMsg  += " Please look at the <a target=_new href=\"log/logview.jsp\">server logs</a> ";

%>    
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Error</title>

<jsp:include page="skins/bootstrap/tiles/tile_head.jsp" >
	<jsp:param value="skins/bootstrap/" name="basePath"/>
	<jsp:param value="" name="commonPath"/>
</jsp:include>



</head>
<body>
		<div class="container-fluid">
                <div class="row">
                    <div class="col-lg-12">
                        <div class="alert <%=alertClass%> alert-dismissable">
                            <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
                            <i class="fa fa-info-circle"></i>  <%=statusMsg%>
                            <% if ( ex != null ) out.print("<br/><br/>" + exceptionToString(ex).replaceAll("at ", "<br/>at "));
                            else if ( ex1 != null ) out.print("<br/><br/>" + exceptionToString(ex1).replaceAll("at ", "<br/>at ")); %>
                        </div>
                    </div>
                </div>
                
                <h2>Node Environment</h2>
                <div class="alert <%=(home.canRead() && home.canWrite() ? "alert-info" : "alert-danger")%>">
                	Configuration Base <%=baseHome%>
                	&nbsp;&nbsp;&nbsp;Is Folder:<%=home.isDirectory()%>, Readable: <%=home.canRead()%>, Writable: <%=home.canWrite()%>
				</div>
				                
				<h2>System</h2>
                <table class="table">
                	<tr>
                		<td>user.home</td>
                		<td><%=System.getProperty("user.home") %></td>
                	</tr>
                	<tr>
                		<td>user.name</td>
                		<td><%=System.getProperty("user.name") %></td>
                	</tr>
                	<tr>
                		<td>os.name</td>
                		<td><%=System.getProperty("os.name") %></td>
                	</tr>
                	<tr>
                		<td>os.arch</td>
                		<td><%=System.getProperty("os.arch") %></td>
                	</tr>
                	<tr>
                		<td>os.version</td>
                		<td><%=System.getProperty("os.version") %></td>
                	</tr>
                </table>
		</div>
</body>
</html>