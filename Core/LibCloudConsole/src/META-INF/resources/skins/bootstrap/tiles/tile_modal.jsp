<%
	final String ctxPath	= request.getContextPath();

	// relative Base path of the resources for this skin
	String basePath 		= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 		= request.getParameter("commonPath");

	String modalId 			= request.getParameter("modalId");

	String modalTitle 		= request.getParameter("modalTitle");

	if ( basePath == null) 		basePath 	= "";
	if ( commonPath == null) 	commonPath 	= "";
	if ( modalId == null) 		modalId 	= "ModalDialog";
%>
	<script type="text/javascript">

	function modalPoll(uri) {
		LOGD("Polling " + uri);

		$.ajax({
			type 	: 'GET',
			url 	: uri,
			// request response in json!
			headers : {
				"Accept" : "application/json; charset=utf-8",
			},
			cache : false
			//data: { rq_clientId: clientId, rq_windowId: windowId, rq_operation: 'poll' }
		})
		.done(function (data) {
			//LOGD('Got ' + data);
			modalSetText (data);
		})
		.fail(function (jqXHR, textStatus) {
			LOGE( "Request failed: " + uri + ' ' + textStatus );
		});
	}

	function modalSetText (text) {
		// split log strings w/ no spaces in chuncks of 70
		var split = text.length > 80 && !text.match(/ /g);
		var text1 = split ? text.match(/.{1,70}/g).join(' ') : text;
		
		document.getElementById('<%=modalId%>-text').innerHTML = text1;
	}

	function modalSetTextFromUrl (ctx) {
		var uri = '<%=ctxPath%>/' + ctx;
		modalPoll(uri);
	}

	</script>
	<!-- 
	<div class="row" style="display:none">
		<p>
			<button id="modalTrigger" data-toggle="modal" data-target="#<%=modalId%>">Login Modal</button>
		</p>
	</div>
	-->
	<div id="<%=modalId%>" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-body">
					<%if ( modalTitle !=null) { %>
					<h1><%=modalTitle %></h1>
					<% } %>
					<p id="<%=modalId%>-text"></p>
				</div>
				<!-- 
				<div class="modal-footer">
					<input type="submit" value="Sign In" class="btn btn-primary">
				</div>
				-->
			</div>
		</div>
	</div>
