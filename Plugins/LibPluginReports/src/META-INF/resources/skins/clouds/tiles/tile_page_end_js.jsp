<%
	/*
	 * CHANGLELOG
	 * 5/1/2019 This page was taken/modified from LibConsoleThemes and is used by the agr-view.jsp to use clouds skin material theme.
	 */
	 
	// relative Base path of the resources for this skin
	String basePath 	= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
			
	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	
%>

     <!-- ================================================
    Scripts
    ================================================ -->
    
	<!-- Common JS logging -->
	<script type="text/javascript" src="<%=commonPath%>js/log.js"></script>
 
    <!-- Common  jquery NOTE JQUI must go immediately after JQUERY -->
    <script type="text/javascript" src="<%=commonPath%>js/jquery.js"></script>  
	<script src="<%=basePath%>js/jqueryui.min.js"></script> 							<!-- Load jQueryUI -->

    <!-- Bootstrap Core JavaScript MUST GO AFTER JQ UI or tool-tips won't render! -->
    <script src="<%=basePath%>../bootstrap/js/bootstrap.js"></script>

	<script src="<%=basePath%>js/enquire.min.js"></script> 									<!-- Load Enquire -->

	<script src="<%=basePath%>js/velocity.min.js"></script>					<!-- Load Velocity for Animated Content -->
	<script src="<%=basePath%>js/velocity.ui.min.js"></script>
	
	<script src="<%=basePath%>js/skylo.js"></script> 		<!-- Skylo -->
	
	<script src="<%=basePath%>js/wijets.js"></script>     						<!-- Wijet -->
	
	<script src="<%=basePath%>js/jquery.sparklines.min.js"></script> 			 <!-- Sparkline -->
	
	<script src="<%=basePath%>js/prettify.js"></script> 				<!-- Code Prettifier  -->
	
	<script src="<%=basePath%>js/bootstrap-tabdrop.js"></script>  <!-- Bootstrap Tabdrop -->
	
	<script src="<%=basePath%>js/jquery.nanoscroller.min.js"></script> <!-- nano scroller -->
	
	<script src="<%=basePath%>js/jquery.dropdown.js"></script> <!-- Fancy Dropdowns -->
	<script src="<%=basePath%>js/material.js"></script> <!-- Bootstrap Material -->
	<script src="<%=basePath%>js/ripples.js"></script> <!-- Bootstrap Material -->
	
	<script src="<%=basePath%>js/application.js"></script>
	<!-- 2/2/2019 
	<script src="<%=basePath%>js/demo.js"></script>
	-->
	<script src="<%=basePath%>js/demo-switcher.js"></script>
	
	<script src="<%=basePath%>js/bootstrap-growl.js"></script>
	
	<!-- End loading site level scripts -->
    
    <!-- Load page level scripts-->
    
	<script src="<%=basePath%>js/jquery.quicksearch.min.js"></script>           			<!-- Quicksearch to go with Multisearch Plugin -->
	<script src="<%=basePath%>js/typeahead.bundle.js"></script>                 	<!-- Typeahead for Autocomplete -->
	<script src="<%=basePath%>js/select2.js"></script>                     			<!-- Advanced Select Boxes -->
	<script src="<%=basePath%>js/jquery.autosize-min.js"></script>            			<!-- Autogrow Text Area -->
	<script src="<%=basePath%>js/bootstrap-colorpicker.js"></script> 			<!-- Color Picker -->
	
	<script src="<%=basePath%>js/jquery.bootstrap-touchspin.js"></script>      <!-- Touchspin -->
	
	
	<script src="<%=basePath%>js/fileinput.js"></script>               			<!-- File Input -->
	<script src="<%=basePath%>js/bootstrap-tokenfield.js"></script>     		<!-- Tokenfield -->
	
	
	<script src="<%=basePath%>js/jquery.chained.js"></script> 						<!-- Chained Select Boxes -->
	
	<script src="<%=basePath%>js/jquery.mousewheel.min.js"></script> <!-- MouseWheel Support -->
    <!-- End loading page level scripts-->
	


