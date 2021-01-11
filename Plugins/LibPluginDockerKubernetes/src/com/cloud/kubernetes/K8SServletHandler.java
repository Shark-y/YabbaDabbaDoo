package com.cloud.kubernetes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.SecurityTool;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.w3.RestException;
import com.cloud.core.w3.WebClient;
import com.cloud.kubernetes.Helm.HelmResponse;
import com.cloud.ssh.Scp;
import com.cluster.ClusterDaemon;
import com.jcraft.jsch.JSchException;

/**
 * THis class has handling code for the {@link K8SServlet}.
 * 
 * @author VSilva
 *
 */
public class K8SServletHandler {
	
	static final Logger log = K8SServlet.log;

	/**
	 * Search request: http://localhost:9080/CloudClusterManager/K8S?node=KubeFoghornLeghorn&op=presearch
	 * <ul>
	 * <li>Helm hub https://hub.helm.sh/api/chartsvc/v1/charts (250)
	 * <li>https://hub.kubeapps.com/api/chartsvc/v1/charts (600)
	 * <li>Helm repos https://hub.helm.sh/assets/js/repos.json
	 * </ul>
	 * @param request - http://localhost:9080/CloudClusterManager/K8S?node=KubeFoghornLeghorn&op=presearch
	 * @param response [ {OBJ1},... ]
	 * @param isPreSearch
	 * Must return a JSON array for type ahead
	 */
	static void handleHelmHubSearch (HttpServletRequest request, HttpServletResponse response, boolean isPreSearch) throws IOException {
		final PrintWriter pw 		= response.getWriter();
		try {
			// Get Helm Hub search endpoints from service config

			// Helm Hub search: java.lang.ClassCastException: com.cloud.console.ConsoleDaemon cannot be cast to com.cloud.cluster.ClusterDaemon
			//ClusterDaemon service 	= (ClusterDaemon)CloudServices.findService(ServiceType.DAEMON); 
			ClusterDaemon service 	= (ClusterDaemon)PluginSystem.findInstance(ClusterDaemon.class.getName()).getInstance();
			
			// Name1:URL1,Name2:URL2,...
			final String urls 			= service.getHubSearchUrls();
			final String[] tmp			= urls.split(",");
			
			log.debug("Hub Search: URLS " + urls);
			
			// Fix for javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
			// JDK 8-131 SecurityTool.disableClientSSLVerificationFrom..,. GIVES javax.net.ssl.SSLException: Unsupported record version Unknown-0.0 for
			// https://stackoverflow.com/questions/23324807/randomly-sslexception-unsupported-record-version-unknown-0-0
			// https://www.oracle.com/java/technologies/javase-jce8-downloads.html
			SecurityTool.disableClientSSLVerificationFromHttpsURLConnection("TLSv1.2");
			JSONArray full = new JSONArray();
			
			// https://artifacthub.io/api/v1/packages/search?limit=60&offset=0&ts_query_web=
			for (int i = 0; i < tmp.length; i++) {
				final String nameUrl 	= tmp[i];					// Helm Hub:https://hub.helm.sh/api/chartsvc/v1/charts
				final String hubName	= nameUrl.split(":")[0];	// Helm Hub
				String url				= nameUrl.contains(":") ? nameUrl.substring(nameUrl.indexOf(":") + 1, nameUrl.length() ): null;
				boolean skipUrl			= false;
				final String searchKey	= !isPreSearch ? request.getParameter("q") : null;
				
				if ( url == null ) {
					continue;
				}
				try {
					if ( isPreSearch) {
						skipUrl = url.contains("?") && url.endsWith("=");
					}
					else {
						skipUrl = !url.endsWith("=");
						// append search key to url: https://artifacthub.io/api/v1/packages/search?limit=60&offset=0&ts_query_web=SEARCH-KEY
						url += searchKey;
					}
					if ( skipUrl ) {
						log.warn("Skipping url " + url + " presearch: " + isPreSearch);
						continue;
					}
					JSONArray array = hubSearch(url);
					log.debug("Hub Search Url: " + url + " got " + ( array != null ? array.length() : "NULL") + " results.");
					
					for (int j = 0; j < array.length(); j++) {
						JSONObject obj = array.getJSONObject(j);
						
						// Inject hub info : name, url
						obj.put("hubName", 	hubName);
						obj.put("hubUrl", 	getBaseUrl(url));
						
						// MONOCULAR: add prefix /api/chartsvc to attributes.icon
						if ( !obj.has("source")) {
							JSONObject attrs = obj.getJSONObject("attributes");
							attrs.put("icon", "/api/chartsvc" + attrs.getString("icon"));
						}
						full.put(obj); //array.get(j));
					}
				} catch (Exception e) {
					log.error("Search " + url, e);
				}
			}
			
			// Note: response must be [] for Typeahead
			pw.write(full/*data*/.toString());
		} catch (Exception e) {
			log.error("Helm Hub search: " + e.getMessage(), e);
			response.getWriter().print("[]");
		}
	}

	/**
	 * Get a base url from a full url
	 * @param url Full url
	 * @return https://hub.helm.sh/api/chartsvc/v1/charts => https://hub.helm.sh, https://192.168.40.84:32543/api/chartsvc/v1/charts => https://192.168.40.84:32543
	 * @throws MalformedURLException
	 */
	public static String getBaseUrl (String url) throws MalformedURLException {
		URL u = new URL(url);
		return u.getProtocol() + "://" + u.getHost() + ( u.getPort() > 0 ? ":" + u.getPort() : "");
	}
	
	/**
	 * Generic Hub Search 
	 * @param url Supports MONOCULAR: https://hub.helm.sh/api/chartsvc/v1/charts ARTIFACTHUB: https://artifacthub.io/api/v1/packages/search?limit=60&offset=0&ts_query_web=KEY
	 * @return JSON Array: [ {id:id, type: chart, attributes: {}} , ...] 
	 */
	private static JSONArray hubSearch (final String url) throws IOException, JSONException {
		WebClient wc 			= new WebClient(url); 
		//wc.logToStdOut(true);
		//wc.setVerbosity(true);
		
		// TWO formats: MONOCULAR, ARTIFACT HUB
		// MONOCULAR FORMAT { data: [ {id, type, attributes { name, repo{}, description, icon}},...]}
		// ARTIFACT HUB: { data: { packages: [ {}]
		final JSONObject root 	= new JSONObject(wc.doGet());
		final JSONArray data 	= new JSONArray();
		
		if ( !root.has("data")) {
			log.error("HUB Search: Invalid response payload for url " + url + " (data OBJ required).");
			return new JSONArray();
		}
		// MONOCULAR: { data: [ {id, type, attributes { name, repo{}, description, icon}},...]}
		if ( root.optJSONArray("data") != null) {
			log.debug("HUB Search: Detected MONOCULAR response for url " + url);
			return root.getJSONArray("data");
		}
		else if ( root.optJSONObject("data").optJSONArray("packages") != null)  {
			// ARTIFACT HUB: { data: { packages: [ {name, description, logo_image_id, repository: { url, kind: 0, display_name} }]
			// extract data & reformat as MONOCULAR for Typeahead
			log.debug("HUB Search: Detected ARTIFACT-HUB response for url " + url);
			JSONArray packages 	= root.getJSONObject("data").getJSONArray("packages");
			
			for (int i = 0; i < packages.length(); i++) {
				JSONObject pkg 		= packages.getJSONObject(i);
				JSONObject repo 	= pkg.getJSONObject("repository");
				
				// skip repo.kind > 0 (not helm charts)
				if ( repo.getInt("kind") > 0) {
					continue;
				}
				// remove junk (description is optional)
				final String description = pkg.has("description") 
						? pkg.getString("description").replaceAll("[\"\\n\\\\]", "")
						: " ";
				
				// re- format as MONOCULAR for typeahead UI
				final String attribs = String.format("{\"description\": \"%s\", \"icon\": \"%s\", \"repo\": {\"name\": \"%s\", \"url\": \"%s\" }}"
						// chart desc - watch size
						, ( description.length() > 80 ? description.substring(0, 80) + "..." : description)
						// chart logo: https://artifacthub.io/image/0503add5-3fce-4b63-bbf3-b9f649512a86
						// NOTE: Logo is optional https://artifacthub.io/static/media/placeholder_pkg_helm.png
						, pkg.has("logo_image_id") 
							? "/image/" + pkg.getString("logo_image_id")
							: "/static/media/placeholder_pkg_helm.png"
						, repo.get("name")
						, repo.getString("url")
				);
				
				JSONObject item 	= new JSONObject();
				JSONObject attrs 	= new JSONObject(attribs);
				
				// repo-name/chart-name
				item.put("id", repo.getString("name") + "/" +  pkg.getString("name")); 
				
				// attributes: description, logo
				item.put("attributes", attrs);
				
				// chart version: relationships.latestChartVersion.data.version
				JSONObject version 		= new JSONObject(String.format("{\"latestChartVersion\": { \"data\": { \"version\": \"%s\" }}}", pkg.getString("version")));
				item.put("relationships", version);
				item.put("source", "artifacthub");
				data.put(item);
			}
			log.debug("HUB Search: Reformatted response as " + data);
		}
		else {
			log.warn("HUB Search: Failed to extract JSON for " + url + " payload " + root);
			return new JSONArray();
		}
		wc.close();
		return data;
	}
	
	/**
	 * Install a helm chart via the SSH interface.
	 * @param request HTTP request: must include (node=TAGET, chart, version)
	 * @param response HTTP response: <pre> {node: TARGET, verSsion: VERSION, stdout: SERVER-RESPONSE, exitStatus: SERVER-SSH-EXIT-STATUS}</pre>
	 * @throws IOException if missing node.
	 */
	static void handleHelmInstall (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		final String chart 		= request.getParameter("chart");	// bitnami/apache
		final String name 		= request.getParameter("name");
		final String version 	= request.getParameter("version");
		final String params 	= request.getParameter("params");	// Install params: e.g service.Type = NodePort
		final String namespace 	= request.getParameter("ns");		// Install namespace
		final String repo	 	= request.getParameter("repoUrl");	// Chart Install repo url
		final String ct			= request.getContentType();
		String yamlPath			= null;
		String remotePath		= null;
		
		try {
			if ( node == null ) {
				throw new IOException("A node name is required.");
			}
			
			K8SNode server 			= Kubernetes.get(node);
			
			// HELM Install Chart: k8s-dashboard/kubernetes-dashboard version=3.0.2 NS:acme @ server KubeLab
			log.debug("HELM Install Chart: " + chart + " Name: " + name + " version=" + version + " NS:" + namespace + " @ server " + server.getApiServer() + " repo " + repo);

			// save yaml?
			if ( ct != null && ct.toLowerCase().contains("yaml")) {
				final String yaml 		= HTTPServerTools.getRequestBody(request);
				final File tempFile		= File.createTempFile(chart, ".yaml");
				yamlPath				= tempFile.getAbsolutePath();
				remotePath				= "/tmp/" + FileTool.getFileName(yamlPath); // assume unix

				log.debug("HELM Install YAML file:" + tempFile + " remote:" + remotePath + " YAML " + (yaml != null ? yaml.length() + " bytes" : "NULL"));
				
				IOTools.saveText(tempFile.getAbsolutePath(), yaml);
				
				// Upload yaml to server (assume unix)
				Scp.copy(server.getHostName(), server.getSSHUser(), server.getSSHPwd(), yamlPath, remotePath);
			}
			
			Helm helm 				= Helm.newInstance(server);
			
			// 5/16/2019 Try installing with the version, if that fails try without the version
			// Some charts fail with a version but succeed without one, for example bitnami/apache
			HelmResponse r 			= null;
			try {
				// Note: Installation fails if repo is missing (find a way to obtain the repo & add it if missing)
				// Note: The repo name can be extracted from the chart id : k8s-dashboard/kubernetes-dashboard (eed the url)
				// $ helm repo add k8s-dashboard https://kubernetes.github.io/dashboard/
				// $ helm install kubernetes-dashboard/kubernetes-dashboard --name my-release
				addRepoIfMissing(helm, chart, repo);
				
				r = helm.install(chart, name, version, params, namespace, remotePath);
			} 
			catch (IOException e) {
				// 	java.io.IOException: Error: failed to download "bitnami/rabbitmq" (hint: running `helm repo update` may help)
				final String reason = e.getMessage();
				
				if ( reason != null && reason.contains("helm repo update") ) {
					log.debug("HELM Install Detected: " + reason + ". Trying a REPO UPDATE.");
					helm.updateRepos();
				}
				else {
					throw e;
				}
				/*
				log.warn("HELM Install FAILED for " + chart + " version " + version + (r != null ? r.toString() : " " + e.getMessage()));
				
				// Purge first
				if ( r == null) {
					log.warn("HELM Install Purging " + name + " ns:" + namespace + " before resintall.");
					helm.delete(name, true, namespace);
				}
				*/
				log.warn("HELM Install RE-Trying without a version (version=null).");
				r = helm.install(chart, name, null, params, namespace, remotePath);
				
			} 
			
			final JSONObject root 	= new JSONObject();
			
			root.put("node", node);
			root.put("chart", chart);
			root.put("stdout", r.resp.response);
			root.put("exitStatus", r.resp.exitStatus);
			
			// cleanup?
			if ( yamlPath != null) {
				log.debug("HELM Install YAML: Cleanup " + yamlPath);
				new File(yamlPath).delete();
			}
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm Install: ", e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());		
		}
	}
	
	/**
	 * List charts available for a cluster.
	 * @param request HTTP request (node).
	 * @param response HTTP response: <pre>{"Next":"","Releases":[{"Name":"alliterating-toad","Revision":1,"Updated":"Wed Apr 10 18:28:38 2019","Status":"DEPLOYED","Chart":"nginx-ingress-1.4.0","AppVersion":"0.23.0","Namespace":"default"},{"Name":"killjoy-buffoon","Revision":1,"Updated":"Wed Apr 10 13:51:28 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"},{"Name":"singing-magpie","Revision":1,"Updated":"Wed Apr 10 13:40:11 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"}]}</pre>
	 * @throws IOException If node is missing.
	 */
	static void handleHelmChartList (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		try {
			if ( node == null ) {
				throw new IOException("A node name is required.");
			}
			K8SNode server 		= Kubernetes.get(node);
			log.debug("HELM List charts: @ server " + server.getApiServer());
			
			// {"Next":"","Releases":[{"Name":"alliterating-toad","Revision":1,"Updated":"Wed Apr 10 18:28:38 2019","Status":"DEPLOYED","Chart":"nginx-ingress-1.4.0","AppVersion":"0.23.0","Namespace":"default"},{"Name":"killjoy-buffoon","Revision":1,"Updated":"Wed Apr 10 13:51:28 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"},{"Name":"singing-magpie","Revision":1,"Updated":"Wed Apr 10 13:40:11 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"}]} 
			Helm helm 		= Helm.newInstance(server);
			JSONObject root = helm.listCharts();
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm List: ", e);
			JSONObject root = HTTPServerTools.buildBaseResponse(500, e.getMessage());
			try {
				root.put("Releases", new JSONArray());
			} catch (Exception e2) {
				log.error("Helm List: ", e);
			}
			pw.write(root.toString());		
		}
	}

	/**
	 * Delete an app from Helm via the SSH interface.
	 * @param request HTTP request with (node, chart, version).
	 * @param response HTTP response: <pre> {node: TARGET, verSsion: VERSION, stdout: SERVER-RESPONSE, exitStatus: SERVER-SSH-EXIT-STATUS}</pre>
	 * @throws IOException
	 */
	static void handleHelmDelete (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		final String chart 		= request.getParameter("chart");
		final String version 	= request.getParameter("version");
		final String namespace 	= request.getParameter("ns");
		
		try {
			if ( node == null ) {
				throw new IOException("A node name is required.");
			}
			K8SNode server 		= Kubernetes.get(node);
			log.debug("HELM Delete: Chart: " + chart + " version=" + version + " ns: " + namespace + " @ server " + server);
	
			Helm helm 				= Helm.newInstance(server);
			HelmResponse r 			= helm.delete(chart, true, namespace);
			final JSONObject root 	= new JSONObject();
			
			root.put("node", node);
			root.put("chart", chart);
			root.put("stdout", r.resp.response);
			root.put("exitStatus", r.resp.exitStatus);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm Delete: ", e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());		
		}
	}

	/**
	 * List Helm repos via the SSH interface.
	 * @param request HTTP request with params: (node).
	 * @param response HTTP response: <pre> {node: TARGET, verSsion: VERSION, stdout: SERVER-TEXT-RESPONSE, exitStatus: SERVER-SSH-EXIT-STATUS}</pre>
	 * @throws IOException
	 */
	static void handleHelmRepoList (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		try {
			if ( node == null ) {
				throw new IOException("A node name is required.");
			}
			K8SNode server 			= Kubernetes.get(node);
			log.debug("HELM List charts: @ server " + server);
			
			// List repos: Return raw server response 
			Helm helm 				= Helm.newInstance(server);
			HelmResponse r 			= helm.listRepos();
			final JSONObject root 	= new JSONObject();
			
			root.put("node", node);
			root.put("stdout", r.resp.response);
			root.put("exitStatus", r.resp.exitStatus);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm List: ", e);
			JSONObject root = HTTPServerTools.buildBaseResponse(500, e.getMessage());
			pw.write(root.toString());		
		}
	}

	/**
	 * Hang tight while we grab the latest from your chart repositories...
	 * @param request HTTP request with params: (node).
	 * @param response HTTP response: <pre> {node: TARGET, verSsion: VERSION, stdout: SERVER-TEXT-RESPONSE, exitStatus: SERVER-SSH-EXIT-STATUS}</pre>
	 * @throws IOException
	 */
	static void handleHelmRepoUpdate (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		try {
			if ( node == null ) {
				throw new IOException("A node name is required.");
			}
			K8SNode server 			= Kubernetes.get(node);
			log.debug("HELM repo update: @ server " + server);
			
			// Update repos: Return raw server response 
			Helm helm 				= Helm.newInstance(server);
			HelmResponse r 			= helm.updateRepos();
			final JSONObject root 	= new JSONObject();
			
			root.put("node", node);
			root.put("stdout", r.resp.response);
			root.put("exitStatus", r.resp.exitStatus);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm List: ", e);
			JSONObject root = HTTPServerTools.buildBaseResponse(500, e.getMessage());
			pw.write(root.toString());		
		}
	}
	
	static void handleHelmRepoAdd (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		final String name 		= request.getParameter("repoName");
		final String url 		= request.getParameter("repoUrl");
		try {
			if ( node == null ) throw new IOException("A node name is required.");
			if ( name == null ) throw new IOException("A repo name is required.");
			if ( url == null ) throw new IOException("A repo URL is required.");
			
			K8SNode server 		= Kubernetes.get(node);
			log.debug("HELM ADD REPO : " + name + " URL=" + url + " @ server " + server);
	
			Helm helm 				= Helm.newInstance(server);
			HelmResponse r 			= helm.addRepo(name, url);
			final JSONObject root 	= new JSONObject();
			
			root.put("node", node);
			root.put("stdout", r.resp.response);
			root.put("exitStatus", r.resp.exitStatus);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm Install: ", e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());		
		}
	}

	/**
	 * Get the values.yaml from a chart via helm show values CHART-NAME (Note: Requires Helm 3.x)
	 * @param request Chart name example: bitnami/elasticsearch
	 * @param response values.yaml
	 * @throws IOException On I/O errors.
	 */
	static void handleHelmShowValues (HttpServletRequest request, HttpServletResponse response) throws IOException {
		handleHelmShow(1, request, response);
	}

	/**
	 * Get the chart meta data via helm show chart CHART-NAME (Note: Requires Helm 3.x)
	 * @param request HTTP GET request http://localhost:9080/CloudClusterManager/K8S?op=HelmShowChart&node=KubeFoghornLeghorn&chart=convergeone/cc-cisco with Chart name.
	 * @param response YAML: <pre>apiVersion: v1
appVersion: "1"
description: Cloud Connector cc_cisco
home: https://www.convergeone.com/
icon: https://www.convergeone.com/hubfs/Convergeone_September2017_Theme/Images/C1-logo_35H.png
maintainers:
- email: vsilva@convergeone.systems
  name: vsilva
name: cc-cisco
version: "20200323"</pre>
	 * @throws IOException On I/O errors.
	 */
	static void handleHelmShowChart (HttpServletRequest request, HttpServletResponse response) throws IOException {
		handleHelmShow(0, request, response);
	}
	
	static void handleHelmShow (final int kind, HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter pw 	= response.getWriter();
		final String node 		= request.getParameter("node");
		final String chart 		= request.getParameter("chart");	// prometheus-community/prometheus
		final String repoUrl	= request.getParameter("repoUrl");
		try {
			if ( node == null ) throw new IOException("A node name is required.");
			if ( chart == null ) throw new IOException("A chart name is required.");
			
			K8SNode server 		= Kubernetes.get(node);
			log.debug("HELM GET VALUES : " + chart + " @ server " + server.getApiServer() + " repo: " + repoUrl);
	
			Helm helm 				= Helm.newInstance(server);
			
			// Add repo if missing
			addRepoIfMissing(helm, chart, repoUrl);
			
			HelmResponse r 			= helm.showAll(chart);
			final JSONObject root 	= new JSONObject();
			
			// 0 = meta, 1 = values.yaml, 2 = readme.md
			String[] data 			= r.resp.response.split("---");
			
			root.put("node", node);
			root.put("stdout", data[kind] ) ; // r.resp.response);
			
			root.put("meta", data[0] ) ; 
			root.put("values", data[1] ) ; 
			root.put("readme", data[2] ) ; 

			root.put("exitStatus", r.resp.exitStatus);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			log.error("Helm Show Values: ", e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());		
		}
	}
	
	/**
	 * A Repo is required to perform Helm operations
	 * @param helm See {@link Helm}
	 * @param chart Chart name repo-name/chart-name
	 * @param repoUrl Repo url
	 */
	private static void addRepoIfMissing (Helm helm, final String chart, final String repoUrl) throws JSchException, IOException, JSONException {
		final String repoName 	= chart.contains("/") ? chart.split("/")[0] : chart;
		
		if ( !helm.hasRepo(repoName)) {
			log.debug("HELM GET VALUES : Adding repo " + repoName + " with url " + repoUrl);
			helm.addRepo(repoName, repoUrl);
		}
	}
	
	public static void handleGetYamlReosurce(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String name = request.getParameter("template");
		final PrintWriter pw 	= response.getWriter();
		InputStream is 			= null;
		try {
			is = K8SServletHandler.class.getResourceAsStream("/resources/k8s/yml/" + name);
			if ( is == null ) {
				throw new IOException("Invalid resource " + name);
			}
			final JSONObject root 	= new JSONObject();
			root.put(name, IOTools.readFromStream(is));

			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} 
		catch (Exception e) {
			log.error("Get YML: ", e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());		
		}
		finally {
			IOTools.closeStream(is);
		}
	}

	/**
	 * Describe a cluster: version, status, pod info , etc...
	 * @param request GET http://localhost:9080/CloudConsole/K8S?op=API&node=GCPCluster1
	 * @param response
	 * @throws IOException
	 */
	public static void handleDescribe(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String node 		= request.getParameter("node");
		final PrintWriter pw 	= response.getWriter();
		final String op 		= request.getParameter("op");
		
		try {
			if ( node == null ) throw new IOException("A node name is required.");
		
			Map<String, Object> params 	= K8SParams.extractParamsFromRequest(request);
			//K8SNode server 				= Kubernetes.get(node);
			
			// Get version
			JSONObject root 			= Kubernetes.invokeApi("API", node, null, null, params);

			// get node/pod info 
			/*
			String[] names				= new String[] {"ListNodes", "ListAllPods"};
			
			for (int i = 0; i < names.length; i++) {
				JSONObject part = Kubernetes.invokeApi(names[i], node, null, null, params, false);
				root.put(names[i], part);
			} */
			JSONObject nodes 	= Kubernetes.invokeApi("ListNodes", node, null, null, params, false);	// 1.7s
			//JSONObject pods 	= Kubernetes.invokeApi("ListAllPods", node, null, null, params, false);	// 13s (too long)
			JSONArray anodes	= nodes.getJSONArray("items");
			//JSONArray apods	 	= pods.getJSONArray("items");
			
			int online = 0;
			int images = 0;	// total images
			// count node statuses
			for (int i = 0; i < anodes.length(); i++) {
				try {
					JSONObject obj 	=  anodes.getJSONObject(i);
					JSONArray conds =  obj.getJSONObject("status").getJSONArray("conditions");
					images 			+= obj.getJSONObject("status").getJSONArray("images").length();
					
					for (int j = 0; j < conds.length(); j++) {
						JSONObject cond =  conds.getJSONObject(j);
						
						if ( cond.getString("type").equals("Ready")) {
							if ( cond.getString("status").equalsIgnoreCase("true")) {
								online++;
							}
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			root.put("nodesTotal", anodes.length());
			root.put("nodesOnline", online);
			root.put("totalImages", images);
			root.put("name", node);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
			
		} catch (RestException ex1) {
			// Docker errors,..
			pw.write( handleRecoverableError(ex1.getHttpStatus(), ex1.getMessage()).toString());
		} catch (IOException ioe) {
			// Connection timeouts, service not available, bad SSL
			log.error(node + " " + op, ioe);
			pw.write( handleRecoverableError(503, ioe.getMessage()).toString());
		} catch (Exception e) {
			// Bugs
			log.error("Exception @ " + node + " " + op, e);
			pw.write(HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
		}
	}
	
	public static JSONObject handleRecoverableError ( int status, String message) {
		JSONObject root = HTTPServerTools.buildBaseResponse(status, message);
		// The data tables UI expects data.items [] else data tables will give a JS error.
		try {
			JSONObject data = new JSONObject();
			data.put("items", new JSONArray());
			root.put("data", data);
		} catch (JSONException e) {
			log.error(status + ":" + message, e);
		}
		return root;
	}
	
}
