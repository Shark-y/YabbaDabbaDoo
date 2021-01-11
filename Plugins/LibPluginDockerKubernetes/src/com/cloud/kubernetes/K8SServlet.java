package com.cloud.kubernetes;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.EncryptionTool;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestException;
import com.cloud.ssh.SSHExec;

/**
 * Servlet implementation class K8SServlet
 */
@WebServlet("/K8S")
public class K8SServlet extends HttpServlet {
	static final Logger log = LogManager.getLogger(K8SServlet.class);

	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public K8SServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String op 		= request.getParameter("op");
		final String node 		= request.getParameter("node");
		final PrintWriter pw 	= response.getWriter();
		
		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		
		if  (op == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "An operation (op) is requred.").toString());
			return;
		}
		if ( op.equals("getToken")) {
			getAccessToken(request, response);
			return;
		} 
		if ( op.contains("search")) {
			K8SServletHandler.handleHelmHubSearch(request, response, op.contains("pre"));
			return;
		} 
		if ( op.equals("HelmList")) {
			K8SServletHandler.handleHelmChartList(request, response);
			return;
		} 
		if ( op.equals("HelmRepoList")) {
			K8SServletHandler.handleHelmRepoList(request, response);
			return;
		}
		if ( op.equals("HelmRepoUpdate")) {
			K8SServletHandler.handleHelmRepoUpdate(request, response);
			return;
		}
		if ( op.equals("HelmShowValues")) {
			K8SServletHandler.handleHelmShowValues(request, response);
			return;
		} 
		if ( op.equals("HelmShowChart")) {
			K8SServletHandler.handleHelmShowChart(request, response);
			return;
		} 
		if ( op.equals("GetYamlResource")) {
			K8SServletHandler.handleGetYamlReosurce(request, response);
			return;
		} 
		
		if  (node == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "A node name (node) is requred.").toString());
			return;
		}
		if ( op.equals("describe")) {
			K8SServletHandler.handleDescribe(request, response);
			return;
		} 
		operation("GET", request, response);
	}


	/*
	 * Get the API token from the API Server
	 */
	private void getAccessToken (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String server 	= request.getParameter("server");
		final String user 		= request.getParameter("user");
		String pwd 				= request.getParameter("password");	// ssh pwd or key
		final String acct 		= request.getParameter("acct");		// serviceaccount:namespace
		final String name 		= request.getParameter("alias");	// node name
		
		final PrintWriter pw 	= response.getWriter();

		try {
			final boolean useKey	= pwd.startsWith("PuTTY-User-Key");
			final String[] tmp		= acct.split(":");
			final String host 		= server.startsWith("http") ? (new URL(server)).getHost() : server;
			
			if ( !acct.contains(":")) {
				throw new IOException("Service account and namespace are required (sa:ns)");
			}

			if ( useKey ) {
				// Note: The key here is in UNIX format \n, need to be converted to windows format \n\r for SSH to work
				// else You'll get SSH error: Invalid Key ....
				pwd = pwd.replaceAll("\n", "\r\n");
				pwd = SSHExec.KEY_PREFIX + Kubernetes.saveKey(name, pwd);
			}
			final String tok 		= KubeAdm.getApiToken(host, tmp[0], tmp[1], user, pwd);
			final JSONObject root 	= new JSONObject();
			
			root.put("token", tok);
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (Exception e) {
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());
		}
	}
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String op 		= request.getParameter("op");
		final String node 		= request.getParameter("node");
		final PrintWriter pw 	= response.getWriter();
		
		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		
		if  (op == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "An operation (op) is requred.").toString());
			return;
		}
		if ( op.equals("HelmInstall")) {
			K8SServletHandler.handleHelmInstall(request, response);
			return;
		} 
		if ( op.equals("HelmDelete")) {
			K8SServletHandler.handleHelmDelete(request, response);
			return;
		} 
		if ( op.equals("HelmRepoAdd")) {
			K8SServletHandler.handleHelmRepoAdd(request, response);
			return;
		} 
		JSONObject root 				= null;
		try {
			// 6/7/2019 extract params from request and massage them for the REST client
			Map<String, Object> params 	= K8SParams.extractParamsFromRequest(request);
			
			final String body			= HTTPServerTools.getRequestBody(request);
			final String contentType	= CoreTypes.CONTENT_TYPE_JSON;
			final String rqContentType	= request.getContentType();
			final String template		= request.getParameter("template");
			final String bodyKey		= request.getParameter("bodyKey");
			String content				= null;
			params.put("DEBUG"			, request.getParameter("debug") != null);
			
			if ( template != null) {
				params.put(bodyKey, body);
				params.put("DATECREATED"	, IOTools.formatDateRFC339(new Date()));
				params.put("UID"			, EncryptionTool.UUID());
				
				final String raw			= IOTools.readFromStream(K8SServlet.class.getResourceAsStream("/resources/k8s/" + template));
				content						= RestClient.replaceParams(params, raw);
			}
			else {
				content						= body;
			}
			// convert yml to JSON. Note CT may be null for POST /K8S?node=KubeFoghornLeghorn&op=CreateNamespace&name=acme&template=namespace.json
			if ( rqContentType != null && rqContentType.contains("yaml")) {
				Yaml yaml 			= new Yaml();
				JSONObject yml 		= new JSONObject((Map)yaml.load(body));
				content				= yml.toString();
			}
			log.trace(String.format("POST: %s Node: %s Params: %s", op, node, params.toString()) + " RQ ct: " + rqContentType);
			log.trace(String.format("POST: PAYLOAD %s", content));

			// Invoke: 
			root 						= Kubernetes.invokeApi(op, node, content, contentType, params);
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (RestException ex1) {
			// Docker errors,..
			pw.write( K8SServletHandler.handleRecoverableError(ex1.getHttpStatus(), ex1.getMessage()).toString());
		} catch (IOException ioe) {
			// Connection timeouts, service not available, bad SSL
			log.error(node + " " + op, ioe);
			pw.write( K8SServletHandler.handleRecoverableError(503, ioe.getMessage()).toString());
		} catch (Exception e) {
			// Bugs
			log.error("Exception @ " + node + " " + op, e);
			pw.write(HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
		}
		
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		operation("DELETE", request, response);
	}
	
	/*
	 * GET, DELETE Handler
	 */
	private void operation (final String type, HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject root 		= null;
		final PrintWriter pw 	= response.getWriter();
		final String op 		= request.getParameter("op");	// op=NAME1,NAME2,... or op=NAME
		final String node 		= request.getParameter("node");

		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);

		try {
			// 6/7/2019 extract params in docker format
			Map<String, Object> params 	= K8SParams.extractParamsFromRequest(request);

			String[] names 				= op.split(",");
			
			log.trace(String.format("%s: %s Node: %s Params: %s", type, op, node, params.toString()));
			
			if ( names.length == 1 ) {
				// Invoke in DT format : {"data": {...}}
				root = Kubernetes.invokeApi(op, node, null, null, params);
			}
			else {
				// Invoke: { "name1" : {...}, "name2": {..},...}
				root = new JSONObject();
				for (int i = 0; i < names.length; i++) {
					JSONObject part = Kubernetes.invokeApi(names[i], node, null, null, params, false);
					root.put(names[i], part);
				}
			}
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			pw.write(root.toString());
		} catch (RestException ex1) {
			// Docker errors,..
			pw.write( K8SServletHandler.handleRecoverableError(ex1.getHttpStatus(), ex1.getMessage()).toString());
		} catch (IOException ioe) {
			// Connection timeouts, service not available, bad SSL
			log.error(node + " " + op, ioe);
			pw.write( K8SServletHandler.handleRecoverableError(503, ioe.getMessage()).toString());
		} catch (Exception e) {
			// Bugs
			log.error("Exception @ " + node + " " + op, e);
			//pw.write(HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
			pw.write( K8SServletHandler.handleRecoverableError(500, "Internal server error: " + e.getMessage() + ". See server logs for details.").toString());
		}
	}
}
