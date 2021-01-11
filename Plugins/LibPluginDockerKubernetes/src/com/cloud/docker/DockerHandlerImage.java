package com.cloud.docker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestException;

/**
 * Docker image handler helper class. This code is meant to be invoked within a servlet:
 * <ul>
 * <li> Install images
 * <li> Do misc operations such as inspect, remove, start, stop isung the {@link RestClient} and an REST API descriptor.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.1 - 6/2/2019
 */
public class DockerHandlerImage {
	
	private static final Logger log = LogManager.getLogger(DockerHandlerImage.class);

	/**
	 * Servlet handler for image creation.
	 * @param node Node name.
	 * @param request HTTP request.
	 * @param response HTTP response.
	 * @throws IOException on I/O errors.
	 * @throws RestException On Docker server errors: {status: STATUS, message; "MESSAGE"}.
	 * @throws JSONException  On JSON parse errors.
	 * @throws Exception On SSL errors.
	 */
	public static void handleCreateImage (final String node, HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException, RestException, Exception {
		final String image 			= request.getParameter(DockerParams.W3_PARAM_IMAGE);
		final String tag 			= request.getParameter(DockerParams.W3_PARAM_TAG);
		final String username		= request.getParameter(DockerParams.W3_PARAM_USERNAME);
		final String password 		= request.getParameter(DockerParams.W3_PARAM_PASSWORD);
		handleCreateImage(node, image, tag, username, password, response);
	}
	
	/**
	 * This method can be invoked from other servlets to install and image and send the result directly to the HTTP response.
	 * @param node Node where the image is to be installed.
	 * @param image Image name.
	 * @param tag Tag name (default:latest).
	 * @param username Optional user name (for private repos).
	 * @param password Optional password.
	 * @param response HTTP response where the result is to be sent.
	 * @throws IOException on I/O errors.
	 * @throws RestException On Docker server errors: {status: STATUS, message; "MESSAGE"}.
	 * @throws JSONException  On JSON parse errors.
	 * @throws Exception On SSL errors.
	 */
	public static void handleCreateImage (final String node, final String image, final String tag, final String username, final String password, HttpServletResponse response) throws IOException, JSONException, RestException, Exception {	
		if ( image == null) {
			throw new IOException("Image is required.");
		}
		if ( tag == null) {
			throw new IOException("Tag is required.");
		}
		JSONObject Auth = new JSONObject();

		if ( username != null && password != null) {
			Auth.put("username", username);
			Auth.put("password", password); 
		}
		
		// Invoke
		JSONObject resp = Docker.imageCreate(node, image, tag, Auth.toString() ); 
		log.debug("Server replied: " + resp.toString());

		HTTPServerTools.injectStatus(resp, 200, "OK");
		response.getWriter().print(resp.toString()); 
	}

	/**
	 * Handler for misc image ops:
	 * <ul>
	 * <li> InspectImage
	 * <li> RemoveImage Docker?node=Node1&op=RemoveImage&name=cloud/cloud-connector-aes:latest
	 * </ul>
	 * @param action One of: InspectImage, RemoveImage.
	 * @param node Node name.
	 * @param request HTTP request.
	 * @param response HTTP response.
	 * @throws IOException on I/O errors.
	 * @throws RestException On Docker server errors: {status: STATUS, message; "MESSAGE"}.
	 * @throws JSONException  On JSON parse errors.
	 * @throws Exception On SSL errors.
	 */
	public static void handleImageAction (final String action, final String node, HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException, RestException, Exception {
		final String id 			= request.getParameter("name");
		if ( id == null) {
			throw new IOException("Image name is required.");
		}
		Map<String, Object> params 	= new HashMap<String, Object>();
		params.put("NAME", id);
		
		// Invoke
		JSONObject resp = Docker.invokeApi(action , node, null, null, params);
		log.debug(action + ": Server replied:" + resp.toString());

		HTTPServerTools.injectStatus(resp, 200, "OK");
		response.getWriter().print(resp.toString()); 
	}

}
