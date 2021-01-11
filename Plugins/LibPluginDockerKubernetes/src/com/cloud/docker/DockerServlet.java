package com.cloud.docker;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestException;
import com.cloud.core.w3.WebClient;

/**
 * Servlet implementation class DockerServlet
 */
@WebServlet("/Docker")
public class DockerServlet extends HttpServlet {
	private static final Logger log = LogManager.getLogger(DockerServlet.class);
	
	private static final long serialVersionUID = 1L;
    
	/** {@link WebClient} used for Docker Hub searches */
	private final WebClient wc;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DockerServlet() {
        super();
        wc = new WebClient();
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
		
		// Search docker hub - https://hub.docker.com/api/content/v1/products/search?page_size=50&type=image&q=
		if ( op.equals("search")) {
			handleDockerHubSearch(request, response);
			return;
		}

		if ( op.equals("SwarmView")) {
			DockerHandlerSwarm.handleSwarmView(request, response);
			return;
		}

		if  (node == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "A node name (node) is requred.").toString());
			return;
		}

		// invoke REST API: ListContainers,  ListImages, ListVolumes
		JSONObject root 				= null;
		try {
			// extract params in docker format
			Map<String, Object> params 	= DockerParams.extractParamsFromRequest(request);
			
			// Invoke: 
			root 						= Docker.invokeApi(op, node, null, null, params);
			
			// Special case ListVolumes => {"data":{"Volumes":null,"Warnings":null}} gives an error in data tables
			if ( op.equals("ListVolumes")) {
				// set empty array to fix console data tables
				if ( root.has("data") && root.getJSONObject("data").has("Volumes") && root.getJSONObject("data").get("Volumes") == JSONObject.NULL) {
					root.getJSONObject("data").put("Volumes", new JSONArray());
				}
			}
			
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

	/*
	 * Search for images @ the docker hub https://hub.docker.com
	 */
	private void handleDockerHubSearch (HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String query 	= request.getParameter("q");
		final String source = request.getParameter("source");
		
		wc.setUrl("https://hub.docker.com/api/content/v1/products/search?page_size=50&type=image&q=" + query + (source != null ? "&source=" + source : ""));
		//wc.setVerbosity(true);
		//wc.logToStdOut(true);
		try {
			response.setHeader("Access-Control-Allow-Origin", "*");
			JSONObject resp = new JSONObject(wc.doGet());
			response.getWriter().print(resp.getInt("count") > 0 ?  resp.getJSONArray("summaries") : "[]");
		} catch (Exception e) {
			log.error("Docker Hub search: " + e.toString());
			response.getWriter().print("[]");
		}
	}
	
	private JSONObject handleRecoverableError ( int status, String message) {
		JSONObject root = HTTPServerTools.buildBaseResponse(status, message);
		// must add data" [] or data tables will give a JS error.
		try {
			root.put("data", new JSONArray());
		} catch (JSONException e) {
			log.error(status + ":" + message, e);
		}
		return root;
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.debug("<stack>" + HTTPServerTools.dumpParameters("POST", request) + "</stack>");
		
		final String op 		= request.getParameter("op");
		final String node 		= request.getParameter("node");
		final PrintWriter pw 	= response.getWriter();

		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		if  (op == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "An operation (op) is requred.").toString());
			return;
		}
		if  (node == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "A node name (node) is requred.").toString());
			return;
		}
		try {
			if  ( op.equals("CreateContainer")) {
				DockerHandlerContainer.handleContainerCreate(node, request, response);
				return;
			}
			if  ( op.equals("ContainerExecCreate")) {
				DockerHandlerContainer.handleContainerExecCreate(node, request, response);
				return;
			}
			if  ( op.equals("RemoveContainer") || op.equals("StartContainer") || op.equals("StopContainer")	|| op.equals("InspectContainer") ) {
				DockerHandlerContainer.handleContainerAction(op, node, request, response);
				return;
			}
			if  ( op.equals("CreateImage") ) {
				DockerHandlerImage.handleCreateImage(node, request, response);
				return;
			}
			if  ( op.equals("RemoveImage") || op.equals("InspectImage") || op.equals("RemoveVolume") ) {
				DockerHandlerImage.handleImageAction(op, node, request, response);
				return;
			}
			// Swarm ops
			if  ( op.startsWith("Swarm") || op.contains("Service") ) {
				DockerHandlerSwarm.handleSwarm(op, node, request, response);
				return;
			}
			response.getWriter().print(HTTPServerTools.buildBaseResponse(500, "Invalid operation " + op));
		}
		catch (RestException ex) {
			// ERROR  HTTP 400: {"message":"could not choose an IP address to advertise since this system has multiple addresses on different interfaces (10.0.2.15 on eth0 and 192.168.99.101 on eth1)"}
			log.error(ex.toString());
			pw.write(HTTPServerTools.buildBaseResponse(ex.getHttpStatus(),  DockerParams.parseServerErrorReponse (ex.getMessage()) ).toString());
		}
		catch (IOException e) {
			// timeouts, etc.
			log.error(op, e);
			pw.write(HTTPServerTools.buildBaseResponse(503, op + ": " + e.toString()).toString());
		}
		catch (Exception e) {
			// SSL and other Bugs 
			log.error("Exception @ " + node + " " + op, e);
			pw.write(HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
		}
	}

}
