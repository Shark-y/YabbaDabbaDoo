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

    <!-- START MAIN -->
    <section id="main">
        
            
		<!-- Side bar -->
       	<jsp:include page="tile_side_bar.jsp">
       		<jsp:param value="<%=commonPath%>" name="commonPath"/>
       		<jsp:param value="<%=basePath%>" name="basePath"/>
       	</jsp:include>
		<!-- End SideBar -->
           
		<!-- //////////////////////////////////////////////////////////////////////////// -->

        <!-- START CONTENT -->
        <section id="content">

			<!--start container-->
			<div class="container">
				<% if ( pageTitle != null) { %>
				<div class="block-header">
					<h2><%=pageTitle %></h2>
				</div>
				<% } %>
				<!--breadcrumbs start -->
				<% if ( crumbLbls != null && (cLinks.length == cLbls.length) ) { %>
				<ol class="breadcrumb">
				<% for (int i = 0 ; i  < cLbls.length ; i++ ) {  %>
					<% if ( ! cLinks[i].equals("class_active")) { %>
					<li><a href="<%=cLinks[i]%>"><%=cLbls[i]%></a></li>
					<% } else { %>
					<li class="active"><%=cLbls[i]%></li>
					<% } %>
				<% } %>
				</ol>
				<% } %>
				<!-- breadcrumbs end -->
            	