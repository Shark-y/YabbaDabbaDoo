<%
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");

	String basePath 	= request.getParameter("basePath");
	String title		= request.getParameter("title");		// page top left title
	String pageTitle	= request.getParameter("pageTitle");	// page main title
	String crumbLbls	= request.getParameter("crumbLabels");	// page crumbs
	String crumbLinks	= request.getParameter("crumbLinks");	// page crumbs

	String [] cLbls		= crumbLbls != null ? crumbLbls.split(",") : null;
	String [] cLinks	= crumbLinks != null ? crumbLinks.split(",") : null;

	boolean hideLeftNav = request.getParameter("hideLeftNav") != null 
			&& Boolean.parseBoolean(request.getParameter("hideLeftNav"));
	
	boolean showLeftNav = !hideLeftNav;
	
	// look for a title in session.
	if ( title == null) 	title = (String)session.getAttribute("title");
	if ( title == null)		title = "";
	
	if ( basePath == null) 	basePath = "";
	
%>

<% if ( showLeftNav) { %>
    <div id="wrapper">
<% } %>

        <!-- Navigation -->
        <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
        
        	<!-- Top Bar -->
        	<jsp:include page="tile_top_bar.jsp">
        		<jsp:param value="<%=basePath%>" name="basePath"/>
        		<jsp:param value="<%=title%>" name="title"/>
        	</jsp:include>
        	
        	<!-- Side bar -->
        	<%if ( showLeftNav) { %>
        	<jsp:include page="tile_side_bar.jsp">
        		<jsp:param value="<%=basePath%>" name="basePath"/>
        	</jsp:include>
        	<% } %>
            <!-- /.navbar-collapse -->
        </nav>

        <div id="page-wrapper">

			<div class="container-fluid">

                <!-- Page Heading -->
                <div class="row">
                    <div class="col-lg-12">
                    	<% if ( pageTitle != null) { %>
                        <h1 class="page-header">
                            <%=pageTitle%> <small>Overview</small>
                        </h1>
                        <% } %>
                        <% if ( crumbLbls != null && (cLinks.length == cLbls.length) ) { %>
                        <ol class="breadcrumb">
                        <% 	for (int i = 0 ; i  < cLbls.length ; i++ ) {  %>
							<% if ( ! cLinks[i].equals("class_active")) { %>
							<li><a href="<%=cLinks[i]%>"><%=cLbls[i]%></a></li>
							<% } else { %>
							<li class="active"><%=cLbls[i]%></li>
							<% } %>
                        <% 	} %>
                        </ol>
                        <% } %>
                    </div>
                </div>
                <!-- /.row -->

