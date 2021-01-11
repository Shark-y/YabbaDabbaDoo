package com.cloud.kubernetes;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.ssh.SSHExec;
import com.cloud.ssh.SSHExec.SSHExecResponse;
import com.jcraft.jsch.JSchException;

/**
 * An SSH interface to the Helm package manager in remote cluster.
 * 
 * <h2>Notes</h2>
 * <ul>
 * <li> This class simply invokes the server commands and returns whatever the node spits out.
 * <li> Server access may require access control setup: <pre>kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=cluster-admin --serviceaccount=kube-system:default </pre>
 * </ul>
 * 
 * @author VSilva
 *
 */
public class Helm {

	static final Logger log = LogManager.getLogger(Helm.class);
	
	public static class HelmResponse {
		SSHExecResponse resp;

		public HelmResponse(SSHExecResponse resp) {
			super();
			this.resp = resp;
		}
		
		@Override
		public String toString() {
			return resp.toString();
		}
		
		public int getExitStatus() {
			return resp.getExitStatus();
		}

		public String getResponse() {
			return resp.getResponse();
		}
		
	}
	
	public static Helm newInstance(K8SNode node) throws IOException {
		return new Helm(node);
	}
	
	final String host 		;
	final String user 		;
	final String identity 	;
	
	/**
	 * Construct a Helm instance for a given node.
	 * @param node
	 * @throws IOException If the node is null.
	 */
	public Helm(K8SNode node) throws IOException {
		super();
		//this.node = Objects.requireNonNull(node, "A node is required.");
		if ( node == null ) {
			throw new IOException("Helm: A node is required.");
		}
		host 		= node.getHostName();
		user 		= node.getSSHUser();
		identity 	= node.getSSHPwd();
		
	}

	/**
	 * Helm install via SSH helm install [CHART-NAME] --version [VERSION]--set [KEY=VAL,...] --generate-name 2>&1
	 * @param chart Chart version. For example: bitnami/rabbitmq
	 * @param name Optional chart name (if null a name will be generated).
	 * @param version Chart version
	 * @param params Optional  parameters. For example: service.type=NodePort
	 * @param namespace Optional namespace. if null charts are installed in the default NS.
	 * @return Command execution, see {@link HelmResponse}.
	 * @throws JSchException On SSH errors.
	 * @throws IOException ON network, I/O errors.
	 */
	public HelmResponse install (final String chart, final String name, final String version, final String params, final String namespace, final String path) throws JSchException, IOException {
		// helm exists? - which: no helm in (/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/var/lib/snapd/snap/bin)
		SSHExecResponse resp = SSHExec.execute(host, user, identity, "which helm 2>&1");
		if ( resp.exitStatus > 0 ) {
			throw new IOException(resp.response + " (" + resp.exitStatus + ")");
		}
		// install - (127) bash: helm: command not found - 2/24/2020 helm 3 requires --generate-name
		// helm install [NAME] [CHART] [flags]
		final String command 	= "helm install " + ( name != null ? name : "")	+ " " + chart 
				+ (version 		!= null && !version.isEmpty() 	? " --version " 	+ version : "") 
				+ (params 		!= null && !params.isEmpty()  	? " --set " 		+ params 	: "")
				+ (namespace 	!= null && !namespace.isEmpty()	? " --namespace " 	+ namespace : "")
				+ (path 		!= null && !path.isEmpty()		? " -f " 			+ path : "")
				+ ( name == null ? " --generate-name 2>&1" : " 2>&1" )
				;
		
		log.debug("HELM Install User " + user + " command: " + command );
		resp 					= SSHExec.execute(host, user, identity, command);
		
		// TODO: ERROR - configmaps is forbidden: User "system:serviceaccount:kube-system:default" cannot list resource "configmaps"
		// GRANT HELM_INSTALL: kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=cluster-admin --serviceaccount=kube-system:default
		// Error: could not find a ready tiller pod
		checkResponse(resp);
		return new HelmResponse(resp);
	}
	
	/**
	 * List installed Helm packages
	 * @return <pre> {"Next":"","Releases":[{"Name":"alliterating-toad","Revision":1,"Updated":"Wed Apr 10 18:28:38 2019","Status":"DEPLOYED","Chart":"nginx-ingress-1.4.0","AppVersion":"0.23.0","Namespace":"default"},{"Name":"killjoy-buffoon","Revision":1,"Updated":"Wed Apr 10 13:51:28 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"},{"Name":"singing-magpie","Revision":1,"Updated":"Wed Apr 10 13:40:11 2019","Status":"DEPLOYED","Chart":"ibm-jenkins-dev-1.0.0","AppVersion":"","Namespace":"default"}]} </pre>
	 * @throws JSONException On stdout parse errors.
	 * @throws IOException If the command fails, No helm ,etc.
	 * @throws JSchException On SSH errors: wrong password, user, etc.
	 */
	public JSONObject listCharts () throws JSchException, IOException, JSONException {
		// helm ls --all --output json
		final String cmd 		= "helm ls --all-namespaces --output json 2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		resp 					= checkResponse(resp);
		
		final boolean gotResp 	= resp.response != null && !resp.response.isEmpty();

		// 4/29/2019 Fix for empty response (return {"Releases":[]}).
		if ( !gotResp ) {
			return new JSONObject("{\"Releases\":[]}");
		}
		
		// Helm 2: {"Next":"","Releases": [{"name":"tomcat-1580406497","namespace":"default","revision":"1","updated":"2020-01-30 12:48:18.738363877 -0500 EST","status":"deployed","chart":"tomcat-0.4.1","app_version":"7.0"}]}
		// 2/24/2020 Helm 3: [{"name":"tomcat-1580406497","namespace":"default","revision":"1","updated":"2020-01-30 12:48:18.738363877 -0500 EST","status":"deployed","chart":"tomcat-0.4.1","app_version":"7.0"}]
		// 12/21/2020 Remove WARNING:
		//resp = cleanJSONResponse(resp);
		
		// convert array to {"Releases":[...]}
		if (  gotResp && resp.response.startsWith("[")) {
			JSONObject root = new JSONObject();
			root.put("Releases", new JSONArray(resp.response)); 
			return root;
		}
		return  new JSONObject(resp.response) ; 
	}

	/**
	 * Runs via SSH: helm ls --all --output json
	 * @return Server raw response text.
	 * @throws JSONException On stdout parse errors.
	 * @throws IOException If the command fails, No helm ,etc.
	 * @throws JSchException On SSH errors: wrong password, user, etc.
	 */
	public HelmResponse listRepos () throws JSchException, IOException, JSONException {
		// helm ls --all --output json
		final String cmd 		= "helm repo list  2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse(resp);
	}

	/**
	 * Runs via SSH: helm repo update
	 * @return Server raw response text.
	 * @throws JSONException On stdout parse errors.
	 * @throws IOException If the command fails, No helm ,etc.
	 * @throws JSchException On SSH errors: wrong password, user, etc.
	 */
	public HelmResponse updateRepos () throws JSchException, IOException, JSONException {
		// helm ls --all --output json
		final String cmd 		= "helm repo update  2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse(resp);
	}
	
	public HelmResponse addRepo (final String name , final String url) throws JSchException, IOException, JSONException {
		// helm repo add [NAME] [URL]
		final String cmd 		= "helm repo add " + name + " " + url + " 2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse(resp);
	}

	/**
	 * Delete a chart (optionally purge name)
	 * @param releaseName Chart name.
	 * @param purge Release (purge name) for reuse.
	 * @param namespace Optional namespace. If null will use default.
	 * @return Raw Server Response: release "bunking-sabertooth" deleted.
	 * @throws IOException If the command fails, No helm ,etc.
	 * @throws JSchException On SSH errors: wrong pwd, user, etc.
	 */
	public HelmResponse  delete (final String releaseName, boolean purge, final String namespace) throws JSchException, IOException {
		// helm del RELEASE
		// helm 3: Error: unknown flag: --purge
		// helm 2: final String cmd 		= "helm del " + releaseName + (purge ? " --purge" : "") + " 2>&1";
		final String cmd 		= "helm del " + releaseName + (purge ? "" : "") 
				+ (namespace 	!= null && !namespace.isEmpty() ? " --namespace " 	+ namespace : "")
				+ " 2>&1";
		
		log.debug("HELM Delete user " + user + " command: " + cmd );
		
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse (resp);
	}

	/**
	 * helm show values bitnami/elasticsearch (Requires Helm 3.x)
	 * @param chart Full chart name. For example: bitnami/elasticsearch
	 * @return default values.yaml
	 * @throws JSchException On SSH errors: wrong pwd, user, etc.
	 * @throws IOException On I/O errors
	 */
	public HelmResponse  showValues (final String chart) throws JSchException, IOException {
		final String cmd 		= "helm show values " + chart 	+ " 2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse (resp);
	}

	/**
	 * helm show chart meta data (Requires Helm 3.x)
	 * @param chart Full chart name. For example: bitnami/elasticsearch
	 * @return <pre>apiVersion: v1
appVersion: "1"
description: Cloud Connector cc_cisco
home: https://www.convergeone.com/
icon: https://www.convergeone.com/hubfs/Convergeone_September2017_Theme/Images/C1-logo_35H.png
maintainers:
- email: vsilva@convergeone.systems
  name: vsilva
name: cc-cisco
version: "20200323"</pre>
	 * @throws JSchException On SSH errors: wrong pwd, user, etc.
	 * @throws IOException On I/O errors
	 */
	public HelmResponse  showChart (final String chart) throws JSchException, IOException {
		final String cmd 		= "helm show chart " + chart + " 2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse (resp);
	}

	/**
	 * Show all chart info.
	 * @param chart Chart full name.
	 * @return STUout separated by --- where 0 = meta, 1 = values.yaml, 2 = readme.md
	 * @throws JSchException
	 * @throws IOException
	 */
	public HelmResponse  showAll (final String chart) throws JSchException, IOException {
		final String cmd 		= "helm show all " + chart + " 2>&1";
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, cmd);
		checkResponse(resp);
		return new HelmResponse (resp);
	}
	
	/**
	 * Check {@link SSHExecResponse}. Throw an {@link IOException} if the exit status != 0. Optionally remove warnings (WARNING:...)
	 * @param in {@link SSHExecResponse} 
	 * @return Cleaned up response.
	 */
	private SSHExecResponse checkResponse (SSHExecResponse in) throws IOException {
		StringBuffer cleanBuffer  	= new StringBuffer();
		String[] tmp 				= in.response.split("\\n");
		
		for ( String line : tmp ) {
			// 12/21/2020 Cleanup warnings WARNING: ... [JSON]
			if ( !line.startsWith("WARNING:") ) {
				cleanBuffer.append(line + "\n");
			}
		}
		in.response = cleanBuffer.toString();
		
		// check exit status
		if ( in.exitStatus > 0 ) {
			throw new IOException(in.response + " (" + in.exitStatus + ")");
		}
		return in;
	}

	/**
	 * Runs via SSH: helm ls --all --output json
	 * @return Server raw response text.
	 * @throws JSONException On stdout parse errors.
	 * @throws IOException If the command fails, No helm ,etc.
	 * @throws JSchException On SSH errors: wrong password, user, etc.
	 */
	public boolean hasRepo (final String name) throws JSchException, IOException, JSONException {
		HelmResponse resp 	= listRepos();
		String out			= resp.getResponse();
		return out != null && out.contains(name);
	}

	/**
	 * Cleanup response: remove WARNING: .... and other junk
	 * @param in incoming {@link SSHExecResponse}
	 * @return Cleaned up {@link SSHExecResponse}
	 */
	/*
	private static SSHExecResponse cleanJSONResponse (SSHExecResponse in) {
		SSHExecResponse out = new SSHExecResponse();
		out.exitStatus 		= in.exitStatus;
		
		// 12/21/2020 FIX for warnings WARNING: ... [JSON]
		StringBuffer cleanBuffer  	= new StringBuffer();
		String[] tmp 				= in.response.split("\\n");
		
		for ( String line : tmp ) {
			if ( line.startsWith("{") || line.startsWith("[")) {
				cleanBuffer.append(line);	// assume all JSON is displayed in a single line
				break;
			}
		}
		out.response = cleanBuffer.toString();
		return out;
	} */

}
