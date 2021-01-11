<%@page import="com.cloud.console.SkinTools"%>
<%
	String statusMsg 	= request.getParameter("statusMsg");
	String statusType	= request.getParameter("statusType");
	String alertClass 	= "alert-info";
	
	if ( statusType == null ) 			statusType = "";
	
	if ( statusType.equals("SUCCESS")) 	alertClass = "alert-success";
	if ( statusType.equals("INFO")) 	alertClass = "alert-info";
	if ( statusType.equals("WARN")) 	alertClass = "alert-warning";
	if ( statusType.equals("ERROR")) 	alertClass = "alert-danger";
	
%>

				<% if ( statusMsg != null && !statusMsg.equals("NULL")) { %>
                         <div class="alert <%=alertClass%> alert-dismissable uk-alert uk-<%=alertClass%>" data-uk-alert>
                         	<%if ( SkinTools.SKIN_PATH.contains("altair")) { %>
                         	<a href="#" class="uk-alert-close uk-close"></a>
                         	<%} else { %>
                            <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>
                            <i class="fa fa-info-circle"></i>
                            <% } %>
                            <%=statusMsg%>
                        </div>
 				<% } %>
