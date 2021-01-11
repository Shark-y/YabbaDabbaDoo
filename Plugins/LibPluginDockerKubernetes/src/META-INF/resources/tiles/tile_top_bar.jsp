<%@page import="com.cluster.ClusterDaemon"%>
<%
	final String clusterGroup 		= ClusterDaemon.getClusterGroupName();
	final boolean showMenuAction 	= request.getParameter("menuClusterAction") == null;
	final boolean showHomeMenu 		= request.getParameter("showHomeMenu") != null;
	
	String basePath 	= request.getParameter("basePath");
	String commonPath 	= request.getParameter("commonPath");
	String mode 		= request.getParameter("mode");
	
	if ( basePath == null) 		basePath = "";
	if ( commonPath == null) 	commonPath = "";
	if ( mode == null)			mode = "tbldt";

%>
			
            <div class="navbar-header">
			
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-ex1-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                 <div class="navbar-brand">Cluster Manager
					<%
						if ( clusterGroup != null ) {
							out.print( "(" + clusterGroup + ")" );
						}
					%>
                 </div>
            </div>
			
			
            <!-- Top Menu Items -->
            <ul class="nav navbar-right top-nav">
            	<%if ( showHomeMenu ) { %>
            	 <li>
            		<a href="<%=basePath%>index.jsp" title="Home."><i class="fa fa-home"></i> Home</a>
                 </li>
            	<% } %>
            	<!-- View -->
                 <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-desktop"></i> View <b class="caret"></b></a>
                    <ul class="dropdown-menu">
		            	 <li>
		            		<a href="index.jsp?mode=dash" title="Node Dashboard view mode."><i class="fa fa-fw fa-dashboard"></i> Dashboard</a>
		                 </li>
		            	 <li>
		            		<a href="index.jsp?mode=tbldt"  title="Node Table view mode."><i class="fa fa-fw fa-table"></i> Table</a>
		                 </li>
		                 <li class="divider"></li>
		            	 <li>
		            		<a href="data.jsp"  title="Cluster data view."><i class="fa fa-fw fa-cubes"></i> Data</a>
		                 </li>
					</ul>
				</li>            	
                <%if ( showMenuAction) { %>
                 <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-globe"></i> Nodes <b class="caret"></b></a>
                    <ul class="dropdown-menu">
		            	 <li>
		            		<a href="javascript:clusterSelectAllNodes()" title="Select/Deselect nodes."><i class="fa fa-fw fa-check"></i> Select Nodes</a>
		                 </li>
		                 <li class="divider"></li>
		            	 <li>
		            		<a href="javascript:clusterStart('<%=mode%>')"  title="Start selected nodes."><i class="fa fa-fw fa-play"></i> Start Nodes</a>
		                 </li>
		            	 <li>
		            		<a href="javascript:clusterStop('<%=mode%>')" title="Stop selected nodes."><i class="fa fa-fw fa-stop"></i> Stop Nodes</a>
		                 </li>
		                 
		                 <li class="divider"></li>
		            	 <li>
		            		<a href="config/profiles.jsp?mode=cm" title="Configure multiple nodes with 1 click."><i class="fa fa-fw fa-wrench"></i> Configure</a>
		                 </li>
		                 
	                </ul>
                 </li>
                 <% } %>

				 <!-- Profiler -->
            	 <li>
            		<a href="<%=commonPath%>perf/osp.jsp?hideLeftNav=true" title="OS Profiler."><i class="fa fa-line-chart"></i> Profiler</a>
                 </li>
                 
                 <!-- Configure -->
            	 <li>
            		<a href="<%=commonPath%>config/config_cluster.jsp" title="Configure the cluster manager."><i class="fa fa-wrench"></i> Settings</a>
                 </li>
                 
                 <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-user"></i> Administer <b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <li>
                            <a href="<%=basePath%>login.jsp?action=loginshow&r=index.jsp"><i class="fa fa-fw fa-user"></i> Login</a>
                        </li>
                        <li>
                            <a href="<%=basePath%>login.jsp?action=logout"><i class="fa fa-fw fa-power-off"></i> Log Out</a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a href="<%=basePath%>login.jsp?action=changepwd&r=index.jsp"><i class="fa fa-fw fa-gear"></i> Password</a>
                        </li>
                        <li class="divider"></li>
                        <li>
                            <a href="<%=basePath%>log/logview.jsp" target="_new"><i class="fa fa-fw fa-list-alt"></i> Log</a>
                        </li>
                    </ul>
                </li>
            </ul>
    
