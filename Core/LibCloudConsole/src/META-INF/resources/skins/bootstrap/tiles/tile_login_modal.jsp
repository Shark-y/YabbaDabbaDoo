<%
	// relative Base path of the resources for this skin
	String basePath 	= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
			
	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	
%>

	<div class="row" style="display:none">
		<p>
			<button id="loginTrigger" data-toggle="modal" data-target="#loginModal">Login Modal</button>
		</p>
	</div>
	
	<form method="post" action="login.jsp?action=login&r=<%=commonPath%>index.jsp">
		<div id="loginModal" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-body">
						<h1>Please Sign In</h1>
						<p>This site requires cloud sign in.</p>
						<input name="txt_pwd" id="txt_pwd" type="password" placeholder="Password" class="form-control" autocomplete="off">
					</div>
					<div class="modal-footer">
						<input type="submit" value="Sign In" class="btn btn-primary">
					</div>
				</div>
			</div>
		</div>
	</form>