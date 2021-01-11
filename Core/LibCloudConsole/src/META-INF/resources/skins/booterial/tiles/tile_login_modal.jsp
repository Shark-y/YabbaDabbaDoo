<%
	// relative Base path of the resources for this skin
	String basePath 	= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
			
	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	
%>

	<form method="post" action="login.jsp?action=login&r=<%=commonPath%>index.jsp">
		<div id="loginModal" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Please Sign In</h4>
					</div>
					<div class="modal-body">
						<p>This site requires cloud sign in.</p>
						<input name="txt_pwd" id="txt_pwd" type="password" placeholder="Password" class="form-control" autocomplete="off">
					</div>
					<div class="modal-footer">
						<button type="submit" class="btn btn-primary">Submit</button>
					</div>
				</div>
			</div>
		</div>
	</form>  
	
	<div class="row" style="display:none">
		<p>
			<button id="loginTrigger" data-toggle="modal" data-target="#loginModal">Login Modal</button>
		</p>
	</div>
	
 