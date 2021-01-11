<%
	String basePath 	= request.getParameter("basePath");
	String title		= request.getParameter("title");			// page top left title
	String pageTitle	= request.getParameter("pageTitle");		// page main title
	String crumbLbls	= request.getParameter("crumbLabels");		// page crumbs
	String crumbLinks	= request.getParameter("crumbLinks");		// page crumbs
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
	
	String [] cLbls		= crumbLbls != null ? crumbLbls.split(",") : null;
	String [] cLinks	= crumbLinks != null ? crumbLinks.split(",") : null;
	
	// look in session.
	if ( title == null) 	title = (String)session.getAttribute("title");
	if ( title == null)		title = "";
	
	if ( basePath == null) 		basePath = "";
	if ( pageTitle == null) 	pageTitle = "";
	if ( commonPath == null) 	commonPath 	= "";
	
%>

        
    <!-- //////////////////////////////////////////////////////////////////////////// -->
        
	<!-- Top Bar -->
    <jsp:include page="tile_top_bar.jsp">
    	<jsp:param value="<%=commonPath%>" name="commonPath"/>
    	<jsp:param value="<%=basePath%>" name="basePath"/>
    	<jsp:param value="<%=title%>" name="title"/>
    </jsp:include>
      	

    <!-- //////////////////////////////////////////////////////////////////////////// -->

          
            
		<!-- Side bar -->
       	<jsp:include page="tile_side_bar.jsp">
       		<jsp:param value="<%=commonPath%>" name="commonPath"/>
       		<jsp:param value="<%=basePath%>" name="basePath"/>
       	</jsp:include>
		<!-- End SideBar -->
           
		<!-- //////////////////////////////////////////////////////////////////////////// -->

    <div id="page_content">
        <div id="page_content_inner">
        
		<% if ( crumbLbls != null && (cLinks.length == cLbls.length) ) { %>
              	<ul class="uk-breadcrumb">
                    <% for (int i = 0 ; i  < cLbls.length ; i++ ) {  %>
					<% 	if ( ! cLinks[i].equals("class_active")) { %>
					<li><a href="<%=cLinks[i]%>"><%=cLbls[i]%></a></li>
					<% 	} else { %>
					<li class="uk-disabled"><%=cLbls[i]%></li>
					<% 		} %>
                    <% 	} %>
                </ul>
		<% } %>
        
		<% if ( pageTitle != null && !pageTitle.equals("")) { %>         
			<h3 class="heading_b uk-margin-bottom"><%=pageTitle %></h3>
    	<% } %>
  
  
  