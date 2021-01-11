package com.cluster.jsp;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import com.cloud.docker.Docker;

/**
 * Helper class to off-load logic from nodes.jsp
 * @author VSilva
 *
 */
public class JSPDocker {

	/**
	 * Execute a JSP action such as: add/remove a node.
	 * @param ctxRoot The web-app ontext root.
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
		// add node
		if ( action.equals("add")) {
			final String hostPort		= request.getParameter("hostPort");
			final String pemCert		= request.getParameter("pemCert");	// empty
			final String pemKey			= request.getParameter("pemKey");	// empty
			final String ksPwd			= request.getParameter("ksPwd");	// empty
			final String chkTLS			= request.getParameter("chkTLS");	// null (off)/on
			final String certsPath		= chkTLS != null ? Docker.getCertsPath() : null;
			final String ksPath			= certsPath != null ? certsPath + File.separator + name + ".jks" : "";
			
			//System.out.println("SAVE name=" + name + " host: " + hostPort + " tls=" + chkTLS + " kspwd=" + ksPwd + " cert=" + pemCert + " key=" + pemKey );

			Docker.addNode(name, hostPort, chkTLS != null, ksPath, ksPwd);
			Docker.saveNodes();
			if ( chkTLS != null) {
				Docker.saveCerts(name, pemCert, pemKey, ksPwd);
			}
			response = "Saved " + name;
		}
		else if ( action.equals("delete")) {
			Docker.removeNode(name);
			Docker.saveNodes();
			response = "Deleted " + name;
		}
		return response;
	}
}
