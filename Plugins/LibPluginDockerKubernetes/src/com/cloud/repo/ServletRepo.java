package com.cloud.repo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.console.JSPLoggerTool;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestException;
import com.cloud.docker.DockerHandlerImage;
import com.cloud.kubernetes.Helm;
import com.cloud.kubernetes.K8SNode;
import com.cloud.kubernetes.Kubernetes;
import com.cloud.kubernetes.Helm.HelmResponse;
import com.cloud.repo.PrivateRepo.RepoType;
import com.jcraft.jsch.JSchException;

/**
 * Servlet to handle the console private REPO manager operations. Repositories can be of 3 types:
 * <ul>
 * <li> Google Image Registry (GCR).
 * <li> Docker Hub Private repositories.
 * <li> HELM privates.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - 6/3/2019 Initial commit
 */
@WebServlet("/Repo")
public class ServletRepo extends HttpServlet  {
	private static final Logger log 			= LogManager.getLogger(ServletRepo.class);
	private static final long serialVersionUID 	= 3483054708867683212L;

	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String op 		= request.getParameter("op");
		final PrintWriter pw 	= response.getWriter();
		
		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		
		if  (op == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "An operation (op) is requred.").toString());
			return;
		}

		try {
			if ( op.equals("getRepos")) {
				getRepos(request, response);
				return;
			} 
			if ( op.equals("getTags")) {
				getTags(request, response);
				return;
			} 
			throw new IOException("Invalid operation " + op);
		} 
		catch (RestException e) {
			pw.write(HTTPServerTools.buildBaseResponse(e.getHttpStatus(), e.getMessage()).toString());
		}
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("REPO " + op, e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());
		}

	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)	throws ServletException, IOException {
		final String op 		= request.getParameter("op");
		final PrintWriter pw 	= response.getWriter();
		
		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		
		if  (op == null) {
			pw.write(HTTPServerTools.buildBaseResponse(500, "An operation (op) is requred.").toString());
			return;
		}
		try {
			if ( op.equals("install")) {
				install(request, response);
			} 
			else if ( op.equals("saverepo")) {
				repoSave(request, response);
			}
			else {
				throw new IOException("Invalid operation " + op);
			}
		} 
		catch (RestException e) {
			JSPLoggerTool.JSP_LOGE("REPO " + op, e);
			pw.write(HTTPServerTools.buildBaseResponse(e.getHttpStatus(), e.getMessage()).toString());
		}
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("REPO " + op, e);
			pw.write(HTTPServerTools.buildBaseResponse(500, e.getMessage()).toString());
		}
	}
	
	/**
	 * {"message":"OK","status":200,"data":[{"name":"cloud/connector","type":"DOCKER","user":"cloud","password":"Thenewcti1","url":"https://cloud.docker.com/api/repo/v1/inspect/v2/"},{"name":"cloud-bots","type":"GOOGLE","user":"_json_key","password":"","url":"https://us.gcr.io/v2/"}]}
	 * @param request HTTP request.
	 * @param response HTTP response
	 * @throws IOException On missing HTTP request parameters.
	 * @throws RestException On docker daemon client errors.
	 * @throws JSONException On JSON parsing errors.
	 * @throws IllegalAccessException On docker daemon client errors.
	 */
	private void getRepos (HttpServletRequest request, HttpServletResponse response) throws IOException, RestException, JSONException, IllegalAccessException {
		final PrintWriter pw 	= response.getWriter();
		JSONArray tags 			= PrivateRepoManager.toJSON();
		final JSONObject root 	= new JSONObject();
		
		root.put("data", tags);
		HTTPServerTools.injectStatus(root, 200, "OK");
		pw.write(root.toString());
	}
	
	/**
	 * {"message":"OK","status":200,"data":["ca_c1msaes","ca_c1mscisco","ca_lpaes","ca_rntaes","ca_rntcisco","ca_sfaes","ca_sfcisco","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen-scb","cc_gen","cc_ucce"]}
	 * @param request HTTP request.
	 * @param response HTTP response
	 * @throws IOException On missing HTTP request parameters.
	 * @throws RestException On docker daemon client errors.
	 * @throws JSONException On JSON parsing errors.
	 */
	private void getTags (HttpServletRequest request, HttpServletResponse response) throws IOException, RestException, JSONException {
		final String name 		= request.getParameter("name");
		final PrintWriter pw 	= response.getWriter();

		if ( name == null ) 	throw new IOException("REPO name is required");
		
		JSONObject tags 		= PrivateRepoManager.fetchTags(name);
		final JSONObject root 	= new JSONObject();
		
		root.put("data", tags);
		HTTPServerTools.injectStatus(root, 200, "OK");
		pw.write(root.toString());
	}
	
	/**
	 * Install an image from a given repo.
	 * @param request HTTP request.
	 * @param response HTTP response
	 * @throws IOException On missing HTTP request parameters.
	 * @throws RestException On docker daemon client errors.
	 * @throws JSONException On JSON parsing errors.
	 */
	private void install (HttpServletRequest request, HttpServletResponse response) throws IOException, RestException, JSONException {
		final String node 		= validateRequestParameter(request, "node", "Node name", false);
		final String name 		= validateRequestParameter(request, "repo", "Repo name", false);
		final String image 		= validateRequestParameter(request, "image", "Image name", false);
		final String tag 		= request.getParameter("tag") != null ? request.getParameter("tag") : "latest";
		
		try {
			PrivateRepo  repo 	= PrivateRepoManager.getRepo(name);
			RepoType type		=  RepoType.valueOf(repo.getType());
			
			if ( type == RepoType.DOCKER ) {
				// IMAGE=[repo-name] ,  TAG=[image-name]
				log.debug("DOCKER INSTALL " + type + " @NODE " + node + " REPO=" + name + " IMAGE=" + image + " USER=" + repo.getUser() ); 
				DockerHandlerImage.handleCreateImage(node, name, image, repo.getUser(), repo.getPassword(), response);
			}
			else if (type == RepoType.GOOGLE) {
				// IMAGE=us.gcr.io/[PROJECT]/IMAGE TAG=latest for example: us.gcr.io/cloud-bots/agentaggregator
				// Extract host-name from URL=https://us.gcr.io/v2/
				final String host 		= (new URL(repo.getUrl())).getHost();
				final String gcrImage	= host + "/" + name + "/" + image;

				log.debug("GOOGLE INSTALL " + type + " @NODE " + node + " REPO=" + name + " IMAGE=" + gcrImage + " USER=" + repo.getUser() ); 
				DockerHandlerImage.handleCreateImage(node, gcrImage, tag, repo.getUser(), repo.getPassword(), response);
			}
			else {
				// HELM
				final String chart 		= name + "/" + image;
				final String version	= "";
				final String namespace 	= request.getParameter("ns");
				
				log.debug("HELM INSTALL NODE=" + node + " REPO=" + name + " IMG=" + image + " TAG=" + tag + " chart=" + chart + " ver=" + version + " ns=" + namespace);
				
				HelmResponse hr = Helm.newInstance(Kubernetes.get(node)).install(chart, null, version, null, namespace, null);
				
				if ( hr.getExitStatus() > 0 ) {
					throw new IOException("HELM install " + chart + " " + hr.toString());
				}
				// Install OK, send stdout
				final PrintWriter pw 	= response.getWriter();
				final JSONObject root 	= new JSONObject();
				
				root.put("data", hr.getResponse());
				
				HTTPServerTools.injectStatus(root, 200, "OK");
				pw.write(root.toString());
				//throw new AbstractRestException("Unimplemented repo type " + type + " for " + name);
			}
		} catch (Exception e) {
			throw new RestException("Install " + image + " from " + name + " @ " + node + ": " + e.getMessage(), 500, "INTERNAL_SERVER_ERROR");
		}
	}

	/**
	 * Save a repository to disk 
	 * @param request HTTP request.
	 * @param response HTTP response
	 * @throws IOException On missing HTTP request parameters.
	 * @throws RestException On docker daemon client errors.
	 * @throws JSONException On JSON parsing errors.
	 * @throws IllegalAccessException On save repo to disk errors.
	 * @throws JSchException On HELM access errors.
	 */
	private void repoSave (HttpServletRequest request, HttpServletResponse response) throws IOException, RestException, JSONException, IllegalAccessException, JSchException {
		final String type 		= validateRequestParameter(request, "rb_repoType", "REPO type", false);
		final String name 		= validateRequestParameter(request, "rb_repoName", "REPO Name", false);
		final String url 		= validateRequestParameter(request, "rb_repoUrl", " REPO Url", false);
		final String user 		= validateRequestParameter(request, "rb_repoUser", "User name", type.equals(RepoType.HELM.name()));
		final String password 	= validateRequestParameter(request, "rb_repoPwd", "Password", type.equals(RepoType.HELM.name()));
		final String node 		= validateRequestParameter(request, "node", "Node name", false);
		
		// Add repo
		PrivateRepoManager.addRepo(type, url, name, user, password);

		// validate repo by invoking get-tags, will fail if error.
		try {
			PrivateRepoManager.fetchTags(name);
			
			// HELM add repo to the HELM server. This is required to install charts.
			if ( type.equals(RepoType.HELM.name())) {
				K8SNode knode 	= Kubernetes.get(node);
				HelmResponse hr = Helm.newInstance(knode).addRepo(name, url);
				
				if ( hr.getExitStatus() > 0 ) {
					throw new IOException("HELM add-repo " + name + " " + hr.toString());
				}
			}
			
		} catch (Exception e) {
			PrivateRepoManager.remove(name);
			throw new IOException(e);
		}
		// OK, proceed.
		PrivateRepoManager.save();

		final PrintWriter pw 	= response.getWriter();
		pw.write(HTTPServerTools.buildBaseResponse(200, "Saved " + name).toString());
	}
	
	private String validateRequestParameter (HttpServletRequest request, final String name, final String label, final boolean optional) throws IOException {
		final String value 	= request.getParameter(name);
		if ( optional) {
			return value;
		}
		if ( value == null || value.isEmpty()) throw new IOException(label + " is required");
		return value;
	}
}
