<%
	// relative Base path of the resources for this skin
	String basePath 	= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
			
	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	
%>

  		</div>
  	</div>
  		
    
    <!-- ================================================
    Scripts
    ================================================ -->
     <!-- common functions -->
    <script src="<%=basePath%>js/common.js"></script>
    
    <!-- uikit functions -->
    <script src="<%=basePath%>js/uikit_custom.min.js"></script>
    
    <!-- altair common functions/helpers -->
    <script src="<%=basePath%>js/altair_admin_common.js"></script>
    
    <!-- Page JS -->
        <!-- peity (small charts) 
        <script src="<%=basePath%>js/jquery.peity.js"></script>
		-->
        <script src="<%=basePath%>js/jquery.sparklines.min.js"></script>
    
        <!-- countUp -->
        <script src="<%=basePath%>js/countUp.js"></script>

		<!-- Notifications  -->
        <script src="<%=basePath%>js/bootstrap-growl.js"></script>
     	<script src="<%=basePath%>js/typeahead.bundle.js"></script>
     	<script src="<%=basePath%>js/jquery.bootstrap-touchspin.js"></script>
  	<!-- End Page JS -->

	<script type="text/javascript" src="<%=commonPath%>js/log.js"></script>
  	
	<!-- 11/2/2017 Bootstrap Growl notifications -->
	<script type="text/javascript" src="<%=commonPath%>js/notify.js"></script>

	<!-- 2/2/2019 Tooltips -->
	<script type="text/javascript" src="<%=commonPath%>js/jquery.qtip.js"></script>
	
	<!-- Data tables -->
	<script type="text/javascript" src="<%=commonPath%>js/jquery.dataTables.js"></script>
	
	<jsp:include page="tile_switcher.jsp"></jsp:include>
	
	
	<script type="text/javascript">
	$(document).ready(function() {
		// QTip -tooltips:  A bit better. Grab elements with a title attribute that isn't blank.
		$('[title!=""]').qtip({
			style: {
				classes: 'qtip-shadow qtip-bootstrap'
			},
			position: {
		        at: 'bottom center'
		    } 
		}); 

	});
	</script>
	