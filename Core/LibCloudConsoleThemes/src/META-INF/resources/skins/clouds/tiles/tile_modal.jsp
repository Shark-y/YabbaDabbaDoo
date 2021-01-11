<%
	final String ctxPath	= request.getContextPath();

	// relative Base path of the resources for this skin
	String basePath 	= request.getParameter("basePath");
	
	// path to resources common to all skins
	String commonPath 	= request.getParameter("commonPath");
	String modalId 		= request.getParameter("modalId");
	String modalTitle 	= request.getParameter("modalTitle");
	
	
	boolean modalFooter 	= request.getParameter("modalFooter") != null;
	final String btn1Label 	= request.getParameter("btnSubmitLabel") != null ? request.getParameter("btn1Label") : "Save";

	boolean dataFormat	= request.getParameter("noDataFormat") == null;
	
	String textWidget 	= request.getParameter("textWidget");

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
		.done(function (data, status, jqXHR ) {
			var contentType = jqXHR.getResponseHeader("Content-Type");
			var payload;
			 
			if ( contentType.indexOf('xml') != -1 ) {
				payload = (new XMLSerializer()).serializeToString(data);
			}
			else {
				// formatted json or text
				payload = typeof(data) == 'object' ? JSON.stringify(data, null, 2) : data;	
			}
			
			//LOGD('Got ContentType ' + contentType); 
			//LOGD(payload);
			
			ed_set_mode (contentType);
			modalSetText (payload);
		})
		.fail(function (jqXHR, textStatus) {
			LOGE( "Request failed: " + uri + ' ' + textStatus );
		});
	}

	function modalSetText (text) {
		<% if ( dataFormat) {%>
		// split log strings w/ no spaces in chuncks of 70
		var split = text.length > 80 && !text.match(/ /g);
		var text1 = split ? text.match(/.{1,70}/g).join(' ') : text;
		<% } else { %>
		var text1 = text;
		<% } %>
		/*
		<% if ( textWidget != null ) { %>
		$('#<%=modalId%>-text').val(text1);
		<% } else { %>
		$('#<%=modalId%>-text').html(text1);
		<% } %>
		*/
		ed_set_value(text1);
		//editor.focus();
	}

	/*
	 * ctx = SysAdmin?op=fuget&f=upload_04_09_callLogCustomFields
	 */
	function modalSetTextFromUrl (ctx) {
		var uri 	= '<%=ctxPath%>/' + ctx;
		var uri1 	= uri.replace("op=fuget", "rq_operation=fuwrite");
		
		LOGD('modalSetTextFromUrl Get: ' + uri + ' Save:' + uri1 );
		
		// set save btn
		$('#editBtn1Url').val(uri1);
		
		// get file data
		modalPoll(uri);
	}

	function modalSetTitle (text) {
		$('.modal-title').html(text);
	}
	
	
	</script>

		<div id="<%=modalId%>" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<%if ( modalTitle !=null) { %>
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3 class="modal-title"><%=modalTitle%></h3>
					</div>
					<% } %>
					<div class="modal-body">
						<!-- 
						<% if ( textWidget != null ) { %>
						<textarea id="<%=modalId%>-text" name="<%=modalId%>-text" rows="15" cols="80" class="form-control autosize"></textarea>
						<% } else { %>
						<p id="<%=modalId%>-text"></p>
						<% } %>
						-->
  						<span id="edStatusMsg"></span>
						<pre id="<%=modalId%>-text" style="width: 100%; height: 400px"></pre>
					</div>
					
					<%if ( modalFooter ) { %>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
						<button type="submit" class="btn btn-raised btn-primary" onclick="return ed_btn1_click()"><%=btn1Label%></button>
					</div>
					<% } %>
				</div>
			</div>
		</div>
	
	<input type="hidden" id="editBtn1Url">
	
	<!--  ace code editor @ https://ace.c9.io -->	
	<script src="../../lib/ace/ace.js"></script>
	<script src="../../lib/ace/ext-searchbox.js"></script>
	
	<script>
	/* https://ace.c9.io/#nav=howto
	 *  editor.setValue("the new text here");
	 *	editor.session.setValue("the new text here"); // set value and reset undo history
	 * 	editor.getValue(); // or session.getValue
	*/
	var editor 		= ace.edit("<%=modalId%>-text");
	
	editor.getSession().setUseWrapMode(true);
    //editor.setTheme("ace/theme/twilight");
	
	function ed_btn1_click() {
		var url 		= $('#editBtn1Url').val();
		var id 			= $('#editFileTitle').html();
		var payload		= editor.getValue();
		var dataType	= 'text';
		
		LOGD('[EDITOR] BTN1 click url:' + url + ' id:' + id + ' dataType: ' + dataType); // + ' pay: ' + payload);

		if ( !url ) {
			LOGE('Invalid save url.');
			return;
		}
		$.ajax( { url: url
			, type:"POST"
			, data: payload
			, success: function( data, status, jqXHR ) {
					var json = typeof(data) == 'string' ? JSON.parse(data) : data;
					LOGD('[SAVE] response: ' + JSON.stringify(json));
					
					edSetStatus(json.message, json.status >= 400 ? 'danger' : 'info')
			},
			contentType:"text/plain; charset=utf-8",
			dataType:"text"
		});
		
	}
	
	function ed_set_value(value) {
		editor.setValue(value);	
	}

	function ed_set_mode(ct) {
		LOGD('[EDITOR] set mode ct: ' + ct);
		if ( ct.indexOf('javascript') != -1) {
			editor.session.setMode("ace/mode/javascript");
		}
		else if ( ct.indexOf('xml') != -1) {
			editor.session.setMode("ace/mode/xml");
		}
		else if ( ct.indexOf('json') != -1) {
			editor.session.setMode("ace/mode/json");
		}
		else if ( ct.indexOf('html') != -1) {
			editor.session.setMode("ace/mode/html");
		}
		else if ( ct.indexOf('yaml') != -1) {
			editor.session.setMode("ace/mode/yaml");
		}
		else {
			// default: text
			editor.session.setMode("ace/mode/text");
		}
	}

	function edSetStatus (text, color) {
		color = color || 'info'; 
		var html = '<div class="alert alert-dismissable alert-' + color + '">' + text + '<button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button></div>' ;
		$('#edStatusMsg').html(html);
	}

	</script>
<!-- END EDITOR MODAL -->	
 