/**
 * HELM REPO DEVOPS
 */

// fires when the list repos link is clicked
function helmListRepos(name) {
	var node 		= name || $('#selNodes').children('option:selected').val();
	//return helm_op ( 'GET', node, 'HelmRepoList', '', '', 'Repos for ' + node);
	return helm_op ({ method: 'GET', 'node': node, 'operation': 'HelmRepoList', modalTitle: 'Repos for ' + node });
}

//fires when the update repos link is clicked
function helmUpdateRepos(name) {
	var node 		= name || $('#selNodes').children('option:selected').val();
	//return helm_op ('GET', node, 'HelmRepoUpdate', '', '', 'Update Repos for ' + node);
	return helm_op ({ method: 'GET', 'node': node, 'operation': 'HelmRepoUpdate',  modalTitle: 'Update Repos for ' + node });
}

// fires when the install option is selected
function helmInstallChart() {
	var node 	= $('#selNodes').children('option:selected').val();
	// basePath, node, chart, version, hubUrl, icon, repoUrl
	modal8Show( { basePath: '../../../', node: node, chart: '', version: '', hubUrl: ''});
}

// fires when the Add repo link is clicked
function helmAddRepo() {
	$('#btnAddRepo').click();
	return false;
}

// fires when the add repo btn is clicked in the modal dialog
function repo_add(name) {
	var rname 	= $('#repoName').val();
	var rurl	= $('#repoURL').val();
	var node 	= name != '' ? name : $('#selNodes').children('option:selected').val();
	$('#btnCloseModal4').click();
	
	LOGD('Add repo name: ' + rname + " U:" + rurl + ' node:' + node);
	
	if ( !rname || !rurl) {
		growl('All fields are required', 'danger');
		return false;
	}
	if ( !node ) {
		growl('Select a cluster.', 'danger');
		return false;
	}
	// add repo
	// {"message":"Error: looks like \"https://foo.com/\" is not a valid chart repository or cannot be reached: Get https://foo.com/index.yaml: x509: certificate is valid for loadbalancer.localdomain, not foo.com\n (1)","status":500} callback: undefined
	var callback = function (data) {
		if ( data.status >= 400) {
			modal4SetStatus(data.message, 'danger');
		}
		else {
			modal4SetStatus('Saved ' + rname);
		}
	}
	var args 		= { method: 'POST', 'node': node, operation: 'HelmRepoAdd', modalTitle: 'Add Repo ' + rname, data: {repoName: rname, repoUrl: rurl} };
	args.callback 	= callback;
	
	//return helm_op ('POST', node, 'HelmRepoAdd', '', '', 'Add Repo ' + rname, {repoName: rname, repoUrl: rurl});
	return helm_op (args);
}

/**
 * Generic HELM operation used by all the links, stdout is shown in a modal dialog
 * @param method HTTP method: GET, POST.
 * @param node Node name.
 * @param operation Op name: HelmRepoAdd, HelmRepoList, HelmrepoDel, etc.
 * @param chart Chart name.
 * @param version Chart version.
 * @param modalTitle stdout modal title.
 * @param data stdout data.
 * @param params Optional install params: key1=val1,key2=val2
 * @param ns Optional namespace.
 * @returns {Boolean} false to abort DOM event.
 */
//function helm_op (method, node, operation, chart, version, modalTitle, data, params, ns, contentType, callback, repo) {
function helm_op (args) {
	var method 		= args.method;
	var node		= args.node;
	var operation	= args.operation;
	var chart		= args.chart;
	var version		= args.version;
	var modalTitle	= args.modalTitle
	var data		= args.data;
	var params		= args.params;
	var ns			= args.ns;
	var contentType	= args.contentType;
	var callback	= args.callback;
	var repo		= args.repoUrl;

	method			= method		|| 'POST';
	chart			= chart			|| '';	// May be null for some ops: List Repos,...
	version			= version		|| '';	// May be null for some ops
	modalTitle		= modalTitle 	|| '';	// Result modal title
	data			= data 			|| {};	// request data
	params			= params 		|| '';	// Optional install params
	ns				= ns 			|| '';	// Optional namespace
	contentType		= contentType	|| 'application/x-www-form-urlencoded; charset=UTF-8';
	repo			= repo			|| ''; 	// chart repo url
	
	// derive a name from chart
	var name = chart.indexOf('/') != -1 ?  chart.split('/')[1]: chart;
	
	if ( ! url )		{ LOGE('helm_op: Url is required'); return }
	if ( ! operation )	{ LOGE('helm_op: operation is required'); return }
	//if ( ! chart )		{ LOGE('helm_op: chart is required'); return }
	//if ( ! version )	{ LOGE('helm_op: version is required'); return }
	if ( ! node )		{ LOGE('helm_op: node is required'); return }
	
	var installUrl 	= url + '&op=' + operation + '&node=' + node 
		+ (chart 	!= '' ? '&chart=' 	+ chart 	: '') 
		+ (version 	!= '' ? '&version=' + version 	: '') 
		+ (params 	!= '' ? '&params=' 	+ params 	: '')
		+ (ns 		!= '' ? '&ns=' 		+ ns 		: '') 
		+ '&name=' + name
		+ (repo 	!= '' ? '&repoUrl=' + repo 		: '')
		;
	
	
	if ( node == '') {
		growl('Please select a node.', 'danger');
		return;
	}
	LOGD('Helm-OP ' + operation + ' Url: ' + installUrl + ' Method:' + method);
	
	//var posting = $.post( installUrl , data);
	var posting = $.ajax( { method: method , url: installUrl, data : data , contentType: contentType} ); // , dataType:"json"
	
	// get results 
	posting.done(function( data ) {
		//  {"message":"which: no helm in (/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/var/lib/snapd/snap/bin)\n (1)","status":500}
		LOGD( operation + " Resp: " + JSON.stringify(data) + ' callback: ' + callback);
		/*
		if ( data.status >= 400) {
			growl(data.message, 'danger');
			return;
		} */
		// {"message":"OK","node":"KubeMaster","stdout":"..","status":200,"exitStatus":0,"chart":"ibm-charts/ibm-jenkins-dev"}
		if ( data.stdout) {
			showInspectModal (modalTitle, 'html', '<pre style="background-color:white;border: 0px;">' + data.stdout + '</pre>');
		}
		if ( callback ) {
			callback(data);
		}
	});
	return false;	
}


/**
 * Show the stdout modal.
 * @param json, text
 * @param payload JSON for type json else text
 */ 
function showInspectModal (title, type, payload, useEditor, large) {
	useEditor 	= useEditor || false;
	large		= large 	|| false;
	
	// set modal title
	$('#inspectTitle').html(title); //'<small>' + title  + '</small>');
	
	// reset b64 checkbox
	$('#json-decode-b64').prop('checked', false);
	
	// set content
	if (useEditor) {
		editor.setValue(JSON.stringify(payload));
		/*
		for(var key in payload) {
			LOGD(key + ' = ' + Base64.decode(payload[key]));
		} */
		$('#json-renderer').hide();
		$('#div-json-editor').show();
		//LOGD('Set ace ed ' + JSON.stringify(payload));
	}
	else {
		$('#json-renderer').show();
		$('#div-json-editor').hide();
		
		if ( type == 'json') {
			$('#json-renderer').jsonViewer(payload);
		}
		else {
			$('#json-renderer').html(payload);
		}
	}
	if ( large ) {
		$('#modal3_body').addClass('uk-modal-dialog-large');
	}
	else {
		$('#modal3_body').removeClass('uk-modal-dialog-large');
	}
	$('#btnInspect').click();
}


/**
 * fires when a delete chart link is clicked in helm.jsp.
 * @param chart Chart name to delete
 * @param version Chart version
 * @param ns Kube Namespace 
 * @param callback A callback function to execute after delete completes.
 */
function helmDelete (chart, version, ns, callback) {
	var _node 		= $('#selNodes').children('option:selected').val() || node ;
	ns				= ns || '';
	
	LOGD('helmDelete ' + chart + ' v=' + version + ' node:'  + _node + ' ns:' + ns);
	return helm_op ( { method: 'POST', 'node': _node, operation: 'HelmDelete', 'chart': chart, 'version': version, 'ns': ns, 'callback': callback });
}
