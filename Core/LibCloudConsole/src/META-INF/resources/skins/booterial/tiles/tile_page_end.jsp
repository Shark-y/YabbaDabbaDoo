<%
	// relative Base path of the resources for this skin
	String basePath 	= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
			
	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	
%>
		</div>
		<!--end container-->
         
	</section>
	<!-- END CONTENT -->

	<!-- //////////////////////////////////////////////////////////////////////////// -->
	<!-- START RIGHT SIDEBAR NAV-->
	<!-- LEFT RIGHT SIDEBAR NAV-->


    </section>
    <!-- END MAIN -->

    <!-- //////////////////////////////////////////////////////////////////////////// -->

    <!-- START FOOTER -->
    <footer id="footer">
    </footer>
    <!-- END FOOTER -->

    <!-- Page Loader -->
    <div class="page-loader">
    	<div class="preloader pls-blue">
        	<svg class="pl-circular" viewBox="25 25 50 50">
            	<circle class="plc-path" cx="50" cy="50" r="20" />
            </svg>

			<p>Please wait...</p>
		</div>
	</div>

    <!-- Older IE warning message -->
    <!--[if lt IE 9]>
        <div class="ie-warning">
            <h1 class="c-white">Warning!!</h1>
            <p>You are using an outdated version of Internet Explorer, please upgrade <br/>to any of the following web browsers to access this website.</p>
            <div class="iew-container">
                <ul class="iew-download">
                    <li>
                        <a href="http://www.google.com/chrome/">
                            <div>Chrome</div>
                        </a>
                    </li>
                    <li>
                        <a href="https://www.mozilla.org/en-US/firefox/new/">
                            <div>Firefox</div>
                        </a>
                    </li>
                    <li>
                        <a href="http://www.opera.com">
                            <div>Opera</div>
                        </a>
                    </li>
                    <li>
                        <a href="https://www.apple.com/safari/">
                            <div>Safari</div>
                        </a>
                    </li>
                    <li>
                        <a href="http://windows.microsoft.com/en-us/internet-explorer/download-ie">
                            <div>IE (New)</div>
                        </a>
                    </li>
                </ul>
            </div>
            <p>Sorry for the inconvenience!</p>
        </div>   
    <![endif]-->
  
    <!-- ================================================
    Scripts
    ================================================ -->
    
    <!-- Common  jquery -->
    <script type="text/javascript" src="<%=commonPath%>js/jquery.js"></script>  
    <!-- <script src="<%=basePath%>js/jquery-2.1.4.js"></script> -->

	<!-- Common JS logging -->
	<script type="text/javascript" src="<%=commonPath%>js/log.js"></script>
 
 	<!-- 11/2/2017 Bootstrap Growl notifications -->
	<script type="text/javascript" src="<%=commonPath%>js/notify.js"></script>
 
    <!-- Bootstrap Core JavaScript -->
    <!-- <script src="<%=basePath%>../bootstrap/js/bootstrap.js"></script> -->
    <script src="<%=basePath%>js/bootstrap-3.3.6.js"></script>
  
	<script src="<%=basePath%>js/jquery.mCustomScrollbar.concat.min.js"></script>
	<script src="<%=basePath%>js/waves.js"></script>
	<script src="<%=basePath%>js/bootstrap-growl.js"></script>
	<script src="<%=basePath%>js/sweet-alert.js"></script>
	<script src="<%=basePath%>js/autosize.js"></script>
	  
	<!-- Placeholder for IE9 -->
	<!--[if IE 9 ]>
	<script src="<%=basePath%>js/jquery.placeholder.min.js"></script>
	<![endif]-->
	
	<script src="<%=basePath%>js/functions.js"></script>




