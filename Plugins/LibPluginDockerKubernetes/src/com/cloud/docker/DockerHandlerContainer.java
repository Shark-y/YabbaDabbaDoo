package com.cloud.docker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestException;

/**
 * Servlet container helper class.
 * 
 * @author VSilva
 *
 */
public class DockerHandlerContainer {
	
	private static final Logger log = LogManager.getLogger(DockerHandlerContainer.class);

	/**
	 * <pre>POST /CloudClusterManager
Query String : op=CreateContainer&node=Node1
Content Type : application/x-www-form-urlencoded; charset=UTF-8
Remote Host  : 0:0:0:0:0:0:0:1
-- [POST] --
PARAMS:
-- START [POST] --
	op -> CreateContainer 
	node -> Node1 
	image -> foo 
	env ->  VAR1=VAL1,VAR2=VAL2
	cmd ->  date
	exposedports ->  22/tcp,1234/udp,..
	portbindings ->  8080:8080,80/tcp:80
	binds ->  /tmp:/tmp,...
	volumes ->  /volumes/data
	labels ->  com.example.vendor:ACME,com.example.version:1.0
-- END [POST]</pre> --
	 * @throws IOException on I/O errors.
	 * @throws RestException On Docker server errors: {status: STATUS, message; "MESSAGE"}.
	 * @throws JSONException  On JSON parse errors.
	 * @throws Exception On SSL errors.
	 */
	static  void handleContainerCreate (final String node, HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException, RestException, Exception {
		handleCreate("CreateContainer", "/resources/docker/container_create.json", node, request, response);
	}
	
	/**
	 * Container action handler: start, stop, pause, resume, remove.
	 * @param action one of: StartContainer, RemoveContainer, InspectContainer
	 * @param node Node name used to find the REST API descriptor
	 * @param request HTTP request.
	 * @param response HTTP response.
	 * @throws IOException on I/O errors.
	 * @throws RestException On Docker server errors: {status: STATUS, message; "MESSAGE"}.
	 * @throws JSONException  On JSON parse errors.
	 * @throws Exception On SSL errors.
	 */
	static void handleContainerAction (final String action, final String node, HttpServletRequest request, HttpServletResponse response) 
			throws IOException, JSONException, RestException, Exception 
	{
		final String id 			= request.getParameter("Id");
		if ( id == null) {
			throw new IOException("Container Id is required.");
		}
		Map<String, Object> params 	= new HashMap<String, Object>();
		params.put("ID", id);
		
		// Invoke
		JSONObject resp = Docker.invokeApi(action, node, null, null, params);
		log.debug(action + ": Server replied:" + resp.toString());

		HTTPServerTools.injectStatus(resp, 200, "OK");
		response.getWriter().print(resp.toString()); 
	}

	/**
	 * Handler to create an execution command in the node.
	 * @param node Node name.
	 * @param request HTTP request.
	 * @param response HTTP response.
	 * @throws IOException on I/O errors.
	 */
	static  void handleContainerExecCreate (final String node, HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException, RestException, Exception 
	{
		handleCreate("ContainerExecCreate", "/resources/docker/container_exec.json", node, request, response);
	}

	/**
	 * Abstract container create or exec-create method.
	 * @param apiName CreateContainer or ContainerExecCreate.
	 * @param descriptor JSON Payload template file (container_exec.json, container_create.json)
	 * @param node Node name used to fetch server:port/cert info within the descriptor (nodes.json).
	 * @param request HTTP request.
	 * @param response HTTP response
	 * @throws IOException on I/O errors.
	 * @throws RestException On Docker server errors: {status: STATUS, message; "MESSAGE"}.
	 * @throws JSONException  On JSON parse errors.
	 * @throws Exception On SSL errors.
	 */
	static void handleCreate (final String apiName, final String descriptor ,final String node, HttpServletRequest request, HttpServletResponse response) 
			throws IOException, JSONException, RestException, Exception
	{
		// extract params in docker format
		Map<String, Object> params = DockerParams.extractParamsFromRequest(request);
		log.debug(apiName + ": Docker params from HTTP request=" + params);
		
		// get raw json 
		String raw = IOTools.readFromStream(DockerServlet.class.getResourceAsStream(descriptor));
		
		// replace params in raw json
		String json = DockerParams.replace(params, raw);
		log.debug("Docker JSON <pre>" + json + "</pre>");
		
		// validate
		JSONObject root = new JSONObject(json);
		
		// Invoke
		JSONObject resp = Docker.invokeApi(apiName, node, root.toString(), CoreTypes.CONTENT_TYPE_JSON, params);
		log.debug(apiName + ": Server replied:" + resp.toString());
		
		HTTPServerTools.injectStatus(resp, 200, "OK");
		response.getWriter().print(resp.toString()); 
	}
	
}
