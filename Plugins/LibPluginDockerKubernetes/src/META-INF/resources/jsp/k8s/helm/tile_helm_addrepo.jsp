<%@page import="com.cloud.console.SkinTools"%>
<%
	final String node = request.getParameter("node") != null ? request.getParameter("node") : "";
%>
	<!-- modal 4 Add repo -->
	<div id="modal4" class="modal fade uk-modal" tabindex="-1" role="dialog">
		<div class="modal-dialog uk-modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<!-- 
					<button id="btnCloseModal4" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					-->
					<h3 class="modal-title">Add Repo</h3>
				</div>
				<div class="modal-body">
					<span id="modal4StatusMsg"></span>
					<div class="<%=SkinTools.cssFormGroupClass()%>">
						<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Name</label>
						<div class="<%=SkinTools.cssFormGroupContentClass() %>">
							<input type="text" id="repoName" class="<%=SkinTools.cssInputClass()%>" required="required" pattern="[a-z0-9\-]{2,}"/>
						</div>
					</div>
					<div class="<%=SkinTools.cssFormGroupClass() %>">
						<label class="<%=SkinTools.cssFormGroupLabelClass()%>">URL</label>
						<div class="<%=SkinTools.cssFormGroupContentClass() %>">
							<input type="text" id="repoURL" class="<%=SkinTools.cssInputClass()%>" required="required"/>
						</div>
					</div>
				</div>
				<div class="modal-footer uk-modal-footer uk-text-right">
					<button type="button" class="close md-btn uk-modal-close" data-dismiss="modal">Close</button>
					<button class="btn btn-raised btn-primary md-btn md-btn-primary" onclick="return repo_add('<%=node%>')">Add</button>
				</div>
			</div>
		</div>
	</div>
	<button id="btnAddRepo" data-toggle="modal" data-target="#modal4" style="display: none" data-uk-modal="{target:'#modal4'}"></button>	

<script>
	function modal4SetStatus (text, color) {
		modalSetStatus('modal4StatusMsg', text, color, '<%=SkinTools.getSkinName()%>');
	}
</script>
