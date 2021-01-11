<%
	final String basePath = request.getParameter("basePath") != null ? request.getParameter("basePath") : "../../";
%>

	<script type="text/javascript" src="<%=basePath%>js/handlebars.min.js"></script>
	<script id="result-template" type="text/x-handlebars-template">
		<div class="ProfileCard u-cf" onclick="profile_card_click({hubUrl: '{{hubUrl}}', chart: '{{id}}', version: '{{relationships.latestChartVersion.data.version}}', advanced: true, params: '', ns: '', icon: '{{attributes.icon}}', repoUrl: '{{attributes.repo.url}}', desc: '{{attributes.description}}' })"> 
			{{#if attributes.icon}}
			<img class="ProfileCard-avatar" src="{{hubUrl}}{{attributes.icon}}">
			{{else}}
			<i class="fas fa-file-image-o fa-2x uk-icon-file-picture-o uk-icon-large" style="color:rgb(66, 66, 66); float: left"></i>
			{{/if}}
			<div class="ProfileCard-details">
				<div class="ProfileCard-realName">{{id}}</div>
				<div class="ProfileCard-description">{{attributes.description}} VERSION : {{relationships.latestChartVersion.data.version}} 
				&nbsp;&nbsp;<a class="ProfileCard-link" href="#" onclick="return hub_install({hubUrl: '{{hubUrl}}', chart: '{{id}}', version: '{{relationships.latestChartVersion.data.version}}', advanced: false, params: '', ns: '', icon: '{{attributes.icon}}', repoUrl: '{{attributes.repo.url}}', desc: '{{attributes.description}}' })">INSTALL</a>
				&nbsp;&nbsp;<a class="ProfileCard-link" href="#" onclick="return hub_install({hubUrl: '{{hubUrl}}', chart: '{{id}}', version: '{{relationships.latestChartVersion.data.version}}', advanced: true, params: '', ns: '', icon: '{{attributes.icon}}', repoUrl: '{{attributes.repo.url}}', desc: '{{attributes.description}}' })">ADVANCED</a>
				&nbsp;&nbsp;<a class="ProfileCard-link" target="_blank" href="{{hubUrl}}/charts/{{id}}">MORE</a>
				</div>
			</div>
			<div class="ProfileCard-stats">
			</div>
		</div>
	</script>
	<script id="empty-template" type="text/x-handlebars-template">
		<div class="EmptyMessage">Your search turned up 0 results.</div>
	</script>
	
	<input id="profile_card_selected_app" type="hidden" />
	
	<script>
	// json: {"hubUrl":"https://artifacthub.io","chart":"bitnami/apache","version":"8.0.3","advanced":true,"params":"","ns":"","icon":"/image/ee08f467-cfcd-4831-b313-711d5b088acb","repoUrl":"https://charts.bitnami.com/bitnami","desc":"Chart for Apache HTTP Server"}
	function profile_card_click (json) {
		// save selected
		LOGD('profile_card_click: Saving ' + JSON.stringify(json));
		$('#profile_card_selected_app').val(JSON.stringify(json));
	}
	</script>
