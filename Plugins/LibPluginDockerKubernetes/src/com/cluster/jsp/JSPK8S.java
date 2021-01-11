package com.cluster.jsp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.cloud.core.security.SecurityTool;
import com.cloud.core.w3.WebClient;
import com.cloud.kubernetes.K8SNode;
import com.cloud.kubernetes.KubeAdm;
import com.cloud.kubernetes.Kubernetes;
import com.cloud.ssh.SSHExec;

/**
 * Helper class to off-load logic from nodes.jsp
 * @author VSilva
 *
 */
public class JSPK8S {

	/**
	 * Execute a JSP action such as: add/remove a node.
	 * @param ctxRoot The web-app context root.
	 * @param request HTTP request.
	 * @return A result message such as: "Added node ..", "Deleted ..."
	 * @throws Exception if something goes wrong.
	 */
	public static String execute ( String ctxRoot, HttpServletRequest request) throws Exception {
		final String action			= request.getParameter("action");
		final String name			= request.getParameter("name");
		String response				= null;
		
		if ( action == null) {
			return "NULL";
		}
		if ( name == null ) {
			throw new IOException("Node name is required.");
		}
		// add node
		if ( action.equals("add")) {
			final String apiServer		= request.getParameter("apiServer");	// required
			final String accessToken	= request.getParameter("accessToken");	// required
			final String sshUser		= request.getParameter("sshUser");		// optional
			String sshPwd				= request.getParameter("sshPwd");		// optional
			final String chkSA			= request.getParameter("chkSA");
			final String acctNS			= request.getParameter("acctNS");		// serviceaccount:namespace
			final String pemKey			= request.getParameter("pemKey");		// optional ssh key
			
			final boolean grantAccess	= chkSA != null ? chkSA.equalsIgnoreCase("on") : false;
			final boolean useSshKey		= pemKey != null && !pemKey.isEmpty();
			
			if ( apiServer == null || apiServer.isEmpty())		throw new IOException("API Server is required.");
			if ( accessToken == null || accessToken.isEmpty())	throw new IOException("An Access Token is required.");
			
			//System.out.println("SAVE name=" + name + " host: " + apiServer + " chkSA=" + chkSA + " sshuser=" + sshUser + " sshP=" + sshPwd + " sshkey=" + pemKey );
			
			// 12/30/2020 validate ApiServer: GET https://prd210:6443/ Resp code 403 Payload JSON: { kind: Status, code :403,....}
			try {
				// This is required to avoid errors with self signed certificates :(
				SecurityTool.disableClientSSLVerificationFromHttpsURLConnection();
				JSONObject root 		= new JSONObject((new WebClient(apiServer)).doGet());
			} catch (Exception e) {
				throw new IOException("Server " + apiServer + " appears invalid: " + e.getMessage());
			}
			
			// save key & set pwd
			if ( useSshKey ) {
				// sshPwd = PKEY:[PATH TO FILE]
				sshPwd = SSHExec.KEY_PREFIX + Kubernetes.saveKey(name, pemKey);
			}
			Kubernetes.addNode(name, apiServer, accessToken, sshUser, sshPwd);
			Kubernetes.saveNodes();
			
			// grant access to read all clustre resources
			if ( grantAccess) {
				final String[] tmp		= acctNS.split(":");
				final String host 		= apiServer.startsWith("http") ? (new URL(apiServer)).getHost() : apiServer;
				
				KubeAdm.createClusterAdminRoleBinding(tmp[0], tmp[1], host, sshUser, sshPwd);
			}
			response = "Saved " + name;
		}
		else if ( action.equals("delete")) {
			Kubernetes.removeNode(name);
			Kubernetes.saveNodes();
			response = "Deleted " + name;
		}
		return response;
	}
	
	/**
	 * Build a reusable tool bar used by the manage.jsp page and other pages.
	 * Items: Shell, HELM Menu {List Repos, Add Repo, List Charts}, ...
	 * @param node Node name.
	 * @return Tool bar HTML string.
	 * @throws MalformedURLException
	 */
	public static String buildManangeToolbar(K8SNode node) throws MalformedURLException {
		final String host			= (new URL(node.getApiServer())).getHost();
		
		final String toolBar 		= 
				// Shell Menu
				"<a title=\"Open a shell\" href=\"#\" onclick=\"window.open('../ssh/ssh.jsp?host=" + host +  "&user=" + node.getSSHUser() + "&identity=" + node.getSSHPwd() + "')\"><i class=\"fas fa-desktop\"></i></a>"
				+ " &nbsp;&nbsp;"
				// REPO BROWSER
				//+ "<a title=\"Repo browser\" href=\"#\" onclick=\"return false\" data-toggle=\"modal\" data-target=\"#modal5\"><i class=\"fas fa-cloud-download-alt\"></i></a>"
				+ "<a title=\"Repo browser\" href=\"#\" onclick=\"return modal5Open()\"><i class=\"fas fa-cloud-download-alt\"></i></a>"
				;
		return toolBar;
	}
}
