<%@page import="com.cloud.console.SkinTools"%>
<%
	final String contextPath 	= getServletContext().getContextPath();
	final String node 			= request.getParameter("name");
%>

		<!-- modal 20 add YML res -->
		<div id="modal20" class="modal fade uk-modal" tabindex="-1" role="dialog">
			<div class="modal-dialog uk-modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<!-- 
						<button id="btnCloseModal20" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						-->
						<h3 id="modal20Title" class="modal-title"></h3>
					</div>
					<div class="modal-body">
						<span id="modal20StatusMsg"></span>
						<div class="<%=SkinTools.cssFormGroupClass() %> modal20NS">
							<div class="<%=SkinTools.cssFormGroupLabelClass()%>">
								<label class="control-label">Namespace <i id="modal20spinner" class="fas fa-spinner fa-spin"></i></label>
							</div>
							<div class="<%=SkinTools.cssFormGroupContentClass() %>">
								<select id="modal20Namespace" class="form-control"></select>
							</div>
						</div>
						
						<pre id="modal20Editor" style="width: 100%; height: 400px"></pre>
					</div>
					<div class="modal-footer uk-modal-footer uk-text-right">
						<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
						<button id="modal20BtnSubmit" class="btn btn-raised btn-primary md-btn md-btn-primary" onclick="return modal20Submit()">Save</button>
					</div>
					
				</div>
			</div>
		</div>
		<button id="btnModal20" data-toggle="modal" data-target="#modal20" style="display: none" data-uk-modal="{target:'#modal20'}"></button>

		<!--  ace code editor @ https://ace.c9.io -->	
		<script src="../../lib/ace/ace.js"></script>

		<script>
		/* https://ace.c9.io/#nav=howto
		 *  editor.setValue("the new text here");
		 *	editor.session.setValue("the new text here"); // set value and reset undo history
		 * 	editor.getValue(); // or session.getValue
		*/
		var m20editor 		= ace.edit("modal20Editor");
		m20editor.session.setMode("ace/mode/yaml");
		
		var _submitCb;
		
		function modalYmlOpen (basePath, node, title, submitCb, tbHtml) {
			LOGD('YML Modal open BasePath: ' + basePath + ' Node: ' + node);
			
			$('#modal20Title').html(title + ' &nbsp;&nbsp;&nbsp;<small>' + tbHtml + '</small>' );
			$('#btnModal20').click();
			_submitCb = submitCb;

			modalYmlSetValue ('');
			if ( basePath && node ) {
				$('.modal20NS').show();
				loadNamespaces (basePath, node, 'modal20Namespace', function () { $('#modal20spinner').hide() });
			}
			else {
				LOGW('YML Modal basePth or node are null. No namespace display');
				$('.modal20NS').hide();
			}
			return false;
		}
		
		function modalYmlSetValue (val) {
			m20editor.setValue(val);
		}

		function modalYmlSetStatus (text, color) {
			modalSetStatus ('modal20StatusMsg', text, color, '<%=SkinTools.getSkinName()%>') ;
		}

		function modal20Submit () {
			var val 	= m20editor.getValue();
			var	ns		= $('#modal20Namespace').find('option:selected').val();
			
			_submitCb(val, ns);
		}
		
		</script>