/**
 * 
 */

// level: info, danger, warning, success
function growl ( text, level, delay) {
	level = level || 'info';
	delay = delay || 30000;
	$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
}

/*
 * http://localhost:9080/CloudClusterManager/K8S?node=KubeFoghornLeghorn&op=ListNamespaces&_=1585358995234
 * {"data":{"metadata":{"resourceVersion":"36234476","selfLink":"/api/v1/namespaces"},"apiVersion":"v1","kind":"NamespaceList","items":[{"metadata":{"uid":"fb98fbad-7078-11ea-a941-000c297a5447","resourceVersion":"36211787","name":"acme","creationTimestamp":"2020-03-27T22:19:10Z","selfLink":"/api/v1/namespaces/acme"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}},{"metadata":{"uid":"4e6d1119-b53f-11e9-9378-000c297a5447","resourceVersion":"35508950","name":"conversations","creationTimestamp":"2019-08-02T16:05:11Z","selfLink":"/api/v1/namespaces/conversations","deletionTimestamp":"2020-03-23T13:18:03Z"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Terminating"}},{"metadata":{"uid":"cf3b6ee0-6a9b-11e9-ad15-000c297a5447","resourceVersion":"151","name":"default","creationTimestamp":"2019-04-29T16:28:24Z","selfLink":"/api/v1/namespaces/default"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}},{"metadata":{"uid":"7b702598-b53f-11e9-9378-000c297a5447","resourceVersion":"14655387","name":"elastic-system","creationTimestamp":"2019-08-02T16:06:27Z","annotations":{"kubectl.kubernetes.io/last-applied-configuration":"{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"annotations\":{},\"name\":\"elastic-system\"}}\n"},"selfLink":"/api/v1/namespaces/elastic-system"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}},{"metadata":{"uid":"cd34f6d2-6a9b-11e9-ad15-000c297a5447","resourceVersion":"8","name":"kube-node-lease","creationTimestamp":"2019-04-29T16:28:20Z","selfLink":"/api/v1/namespaces/kube-node-lease"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}},{"metadata":{"uid":"cd34a004-6a9b-11e9-ad15-000c297a5447","resourceVersion":"7","name":"kube-public","creationTimestamp":"2019-04-29T16:28:20Z","selfLink":"/api/v1/namespaces/kube-public"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}},{"metadata":{"uid":"cd342980-6a9b-11e9-ad15-000c297a5447","resourceVersion":"5","name":"kube-system","creationTimestamp":"2019-04-29T16:28:20Z","selfLink":"/api/v1/namespaces/kube-system"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}},{"metadata":{"uid":"1857ae50-b859-11e9-b491-000c297a5447","resourceVersion":"15288995","name":"kubernetes-dashboard","creationTimestamp":"2019-08-06T14:47:21Z","annotations":{"kubectl.kubernetes.io/last-applied-configuration":"{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"annotations\":{},\"name\":\"kubernetes-dashboard\"}}\n"},"selfLink":"/api/v1/namespaces/kubernetes-dashboard"},"spec":{"finalizers":["kubernetes"]},"status":{"phase":"Active"}}]},"message":"OK","status":200}
 */
function loadNamespaces (basepath, node, combo, callback) {
	LOGD('loadNamespaces basepath=' + basepath + ' node=' + node + ' combo=' + combo);
	
	var posting = $.get(basepath +  '/K8S?node=' + node + '&op=ListNamespaces' );
	
	posting.done(function( data ) {
		LOGD('Load Namespaces: ' + combo + ' ' + JSON.stringify(data));
		$('#' + combo).empty();
		$.each(data.data.items, function (key, entry) {
			$('#' + combo).append($('<option></option>').attr('value', entry.metadata.name ).text(entry.metadata.name + ' (' + entry.status.phase + ')' ));
		});
		if ( callback ) {
			callback();
		}
	});
}

function createNamespace (basepath, node, name, callback) {
	var posting = $.post(basepath +  '/K8S?node=' + node + '&op=CreateNamespace&name=' + name + '&template=namespace.json' );
	
	posting.done(function( data ) {
		LOGD('Create Namespace: ' + JSON.stringify(data));
		if ( callback ) {
			callback(data);
		}
	});
}

function delNamespace (basepath, node, name, callback) {
	var url =  basepath +  '/K8S?node=' + node + '&op=DeleteNamespace&name=' + name;
	LOGD('Del NS ' + name + ' Url:' + url);
	
	//var posting = $.post(url);
	var posting = $.ajax( { url: url, type:"DELETE" } );

	posting.done(function( data ) {
		LOGD('Delete Namespace: ' + JSON.stringify(data));
		if ( callback ) {
			callback(data);
		}
	});
}

function modalSetStatus (id, text, color, skin) {
	color 	= color || 'info'; 
	skin 	= skin || 'clouds'; 
	
	var html = '<div class="alert alert-dismissable alert-' + color + '">' + text + '<button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button></div>' ;

	if ( skin == 'altair') {
		html = '<div class="uk-alert alert-dismissable uk-alert-' + color + '" data-uk-alert>' + text + '<a href="#" class="uk-alert-close uk-close"></a></div>' ;
	}
	$('#' + id).show();
	$('#' + id).html(html);
}

/**
 * fires when an open-file button is pressed and a file is selected from an INPUT type=file, sets the value to destination.
 * @param event INPUT type=file event
 * @param destination HTML target (textarea or text input). Note: for text, if data is 1+ lines removes all LF(\n).
 */ 
function openFile (event, destination ) {
    var input 	= event.target;
    var reader 	= new FileReader();
    LOGD('openFile d:' + destination + " input:" + input + ' files[0]: ' + input.files[0]);
    reader.onload = function() {
    	var data 	= reader.result;
    	var target	= $('#' + destination);
    	
    	if (target.is("textarea")) {
    		target.html(data);
    	}
    	else {
    		// input Set data - remove LF 
    		target.val(data.replace(/\n/g,""));    		
    	}
    };
    if ( input.files[0]) {
    	reader.readAsText(input.files[0]);
    }
};

/**
 * Fires when the main header search icon is clicked
 * @returns
 */
function main_search_click() {
	var val 	= $('#search-input').val();
	var node	= $('#search-input-node').val();
	
	LOGD('Search ' + val + ' Node: ' + node);
	
	if ( hub_install && val ) {
		// hubUrl, chart, version, advanced, params, ns, icon, repo
		hub_install ( { hubUrl:'', chart: val, version: '', advanced: true, params: '', ns: '', icon: '', repo: '' } );
	}
	return false;
}

/**
 * Helm hub install
 * {"hubUrl":"https://artifacthub.io","chart":"bitnami/apache","version":"8.0.3","advanced":false,"params":"","ns":"","icon":"/image/ee08f467-cfcd-4831-b313-711d5b088acb","repo":"https://charts.bitnami.com/bitnami","desc":"Chart for Apache HTTP Server"}
 * @param basePath Context root of the caller page (used to configure URLs in the chart dialog)
 * @param node Kube node name
 * @param hubUrl Hub install url: https://hub.helm.sh/ or https://192.168.40.84:32543/
 * @param chart Chart name
 * @param version Version
 * @param advanced (mode) Optional: If true open the advanced dialog to ask for install params
 * @param params Optional: User entered params from advanced dialog: key1=val1,key2=val2,...
 * @param icon Optional: Chart icon for dialog display
 * @returns {Boolean}
 */
function hubInstall (args) {
	args.advanced		= args.advanced 	|| false;
	args.params			= args.params 		|| '';
	args.ns				= args.ns			|| '';
	args.icon			= args.icon			|| '';
	args.repoUrl		= args.repoUrl		|| ''; 	// chart repo url
	
	// payload (args.data) can be YAML
	var data			= args.data ? args.data : {repoUrl: args.repoUrl, params: args.params, ns: args.ns}

	LOGD('hubInstall Chart: ' + JSON.stringify(args));
	
	if ( args.advanced) {
		// from search box {"hubUrl":"https://artifacthub.io","chart":"bitnami/apache","version":"8.0.3","advanced":true,"params":"","ns":"","icon":"/image/ee08f467-cfcd-4831-b313-711d5b088acb","repoUrl":"https://charts.bitnami.com/bitnami","desc":"Chart for Apache HTTP Server"}
		var box		= $('#profile_card_selected_app').val(); 
		
		if ( box && !args.icon) {
			var json = JSON.parse (box);
			LOGD('Using search box data: ' + box);
			args.icon 		= json.icon;
			args.repoUrl	= json.repoUrl;
			args.hubUrl		= json.hubUrl;
			args.desc		= json.desc;
		}
		// basePath, node, chart, version, hubUrl, icon, repoUrl
		modal8Show(args);
		return;
	}

	// save in local storage - chart = repo/chart-name
	if ( args.node ) {
		var name = args.chart.includes('/') ? args.chart.split('/')[1] : args.chart;
		
		LOGD('HubInstall save to (local storage) chart: ' + args.node + '-' + args.chart + ' = ' + JSON.stringify(args));
		window.localStorage.setItem(args.node + '-' + name, JSON.stringify(args));
	}
	
	args.data 		= data;
	args.operation 	= 'HelmInstall';
	
	if( !modal8IsVisible () ) {
		Snackbar.show({duration: 20000, pos: 'top-center', text: '<i class="fas fa-spinner fa-spin fa-2x uk-icon-refresh uk-icon-medium uk-icon-spin"></i> PLEASE WAIT. This may take a while for large images.'});
	}

	// method, node, operation, chart, version, modalTitle, data, params, ns, contentType, callback, repo
	return helm_op ( args);
} 

/**
 * Type ahead - https://twitter.github.io/typeahead.js/examples/ - https://github.com/twitter/typeahead.js
 * for docker hub - https://hub.docker.com/api/content/v1/products/search?page_size=50&q=busybox&type=image
 */
function initializeTypeAhead () {
	// templates
	var template = Handlebars.compile($("#result-template").html());
	var empty = Handlebars.compile($("#empty-template").html());
	
	$('#search-input').addClass('typeahead');
	
	// Bloodhound - 
	var engine = new Bloodhound({
		  datumTokenizer:  function (d) {
			  var t1 = Bloodhound.tokenizers.whitespace(d.id);
			  var t2 = Bloodhound.tokenizers.whitespace(d.attributes.description);
			  var t3 = Bloodhound.tokenizers.nonword(d.id);
			  return t1.concat(t2).concat(t3);
		  },
		  queryTokenizer: Bloodhound.tokenizers.whitespace,
		  
		  // https://github.com/twitter/typeahead.js/blob/master/doc/bloodhound.md#prefetch
		  prefetch: {
			  url	: url + '&op=presearch',
			  cache	: false
		  },
		  remote: {
				url: url + '&op=search&q=%QUERY', 
			    wildcard: '%QUERY'
		  } 
	}); 
	
	// required for prefetch to work
	engine.clearPrefetchCache();
	engine.initialize();
	
	$('#search-box .typeahead').typeahead(null, 
			{
			  name: 'publisher',
			  display: 'id',
			  //displayKey: 'id',
			  source: engine,
			  templates: {
				
				suggestion: template
				//empty: empty 
			  }  
			}
	);
}

/**
 * Wrap a (status) text in a colorized label (badge)
 * @param text
 * @returns
 */
function wrapInLabel (text) {
	if ( !text ) {
		return '';
	}
	var color 	= 'info';
	var txt		= text.toLowerCase();
	color 		= txt.indexOf('failed') != -1 ? 'danger' : txt.indexOf('warn') != -1 || txt.indexOf('pending') != -1 ? 'warning' : 'info'; 
	
	return 		'<span class="label label-' + color + ' uk-badge uk-badge-' + color + '">' + text + '</span>' ;
}

/**
 * Invoked by manage.jsp and helm, helm_dt.jsp to get pod information for an array of HELM apps.
 * @param url  ../../K8S?node=NODE&op=ListPods
 * @param apps HELM apps [{"app_version":"2.4.46","name":"apache","namespace":"default","updated":"2020-12-28 19:03:33.576423124 -0600 CST","chart":"apache-8.0.3","revision":"1","status":"deployed"}]
 * @param namespaces Array of available name spaces ['default']
 * @param callback Function to execute when results are obtained.
 */ 
function fetchPodInfo ( url, apps, namespaces, callback) {
	namespaces.forEach (function (namespace) {
		var Url 	= url + '&namespace=' + namespace;
		
		LOGD('Fetch Pod info ' + Url + ' ns:' + namespace);
		var posting = $.get (Url);
		
		posting.done(function( json ) {
			//LOGD(namespace + ' PODS : ' + JSON.stringify(json));
			
			// {data: {}, status: 200, mesage: xxx}
			if ( json.status != 200) {
				return;
			}
			var items = json.data.items;
			
			for ( var rowIdx = 0 ; rowIdx < apps.length ; rowIdx++) {
				var app 		= apps[rowIdx];
				var name		= app.name;			// HELM app name		
				app.podStatuses	= {Running: 0, Pending: 0 , Succeeded: 0};
				
				for ( var i = 0 ; i < items.length ; i++ ) {
					var pod			= items[i];
					var podName		= pod.metadata.name;	// pod name
					var phase		= pod.status.phase;		// status label: Running, Pending, Succeeded...
					//var spec 		= pod.spec;
					
					//LOGD('Fetch pods Compare ' + podName + ' == ' + name);
					if ( podName.indexOf(name) != -1 ) {
						app.podStatuses[phase]++; 
						//LOGD('App updated: ' + JSON.stringify(app));
					}
				}			
			}
			//LOGD('Fetch Pod info COMPLETE Apps: ' + JSON.stringify(apps));
			if ( callback ) {
				callback (apps);
			}
		});
	});
}

