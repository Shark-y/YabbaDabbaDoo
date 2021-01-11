<%
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");

	String basePath 	= request.getParameter("basePath");
	
	boolean hideLeftNav = request.getParameter("hideLeftNav") != null 
			&& Boolean.parseBoolean(request.getParameter("hideLeftNav"));
	
	boolean showLeftNav = !hideLeftNav;
	
	if ( basePath == null) 		basePath = "";
	if ( commonPath == null) 	commonPath 	= "../";
	
%>

				<!-- put some space @ the bottom --> 
				<div style="height: 200px; width: 100%">
					&nbsp;
				</div>
				
                <!-- /.row -->

            </div>
            <!-- /.container-fluid -->

        </div>
        <!-- /#page-wrapper -->
<% if ( showLeftNav) { %>
    </div>
    <!-- /#wrapper -->
<% } %>

    <!-- jQuery -->
    <script src="<%=commonPath%>js/jquery.js"></script>

    <!-- Bootstrap Core JavaScript -->
    <script src="<%=basePath%>js/bootstrap.js"></script>
    <script src="<%=basePath%>js/bootstrap-growl.js"></script>

	<!-- Common JS logging -->
	<script type="text/javascript" src="<%=commonPath%>js/log.js"></script>

	<!-- 11/2/2017 Bootstrap Growl notifications -->
	<script type="text/javascript" src="<%=commonPath%>js/notify.js"></script>

	<script>
		// Note: Tooltips must be initialized with jQuery: select the specified element and call the tooltip() method.
		$(document).ready(function(){
			$('[data-toggle="tooltip"]').tooltip();
		});
	</script>	

