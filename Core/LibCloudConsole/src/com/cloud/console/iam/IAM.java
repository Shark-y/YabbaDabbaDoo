package com.cloud.console.iam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.iam.MsGraph;
import com.cloud.console.HTTPServerTools;
import com.cloud.console.JSPLoggerTool;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.net.RemoteAuthenticator;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;

/**
 * A very simple Identity Access Manager for
 * <ul>
 * <li> Windows Active Directory
 * <li> Azure AD: https://portal.azure.com/#home
 * <ul>
 * 
 * @author VSilva
 * @version 1.0.0 9/10/2020
 *
 */
public class IAM {

	private static final Logger log = LogManager.getLogger(IAM.class);
	
	/** Default roles */
	public enum Types { WindowsAD, AzureActiveDirectory };

	
	/** iam info */
	private static JSONObject root = new JSONObject();
	
	static {
		try {
			load();
		} catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("IAM", "Load", e);
		}
	}
	
	/**
	 * Load from $HOME/.cloud/[PRODUCT]/iam.json
	 */
	public static void load () throws FileNotFoundException, JSONException, IOException {
		final String path		= getBasePath(); 
		
		if ( FileTool.fileExists(path)) {
			JSPLoggerTool.JSP_LOGD("IAM", "Loading data from " + path);
			
			root = new JSONObject(IOTools.readFileFromFileSystem(path));
		}
		else {
			// 12/6/2020 fix for JS error Uncaught TypeError: aData is undefined
			root.put("data", new JSONArray());
		}
	}
	
	/**
	 * By default, alerts are stored in $home/.cloud/[PRODUCT-NAME]/filename.json
	 * @return $home/.cloud/[PRODUCT]/iam.json
	 */
	private static String getBasePath() {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		
		// $home/.cloud/Product
		String basePath 		= cfg.getConfigLocation();
		String filePath			= basePath + File.separator + "iam.json";
		return filePath;
	}
	
	/**
	 * Save an IAM record from an HTTP {@link ServletRequest}
	 * @param request HTTP servlet request.
	 * @throws IOException On I/O errors.
	 * @throws JSONException On JSON errors.
	 */
	public static void save(HttpServletRequest request ) throws IOException, JSONException {
		// java.lang.IllegalStateException: No modifications are allowed to a locked ParameterMap
		Map<String, String[]> map = new HashMap<String, String[]>(request.getParameterMap());
		save(map);
	}
	
	/**
	 * Get the parameter values from a request parameter map (String, String[])
	 * @param request the request parameter map.
	 * @param name The key of the map used to extract values
	 * @param required if true throw {@link IOException} if values is null.
	 * @param label String to be thrown in the {@link IOException}.
	 * @return Values as String[] or new String[] {null} if name doesn't exist.
	 */
	static String[] getParameters(Map<String, String[]> request, final String name, boolean required, final String label) throws IOException {
		return HTTPServerTools.getParameters(request, name, required, label);
	}

	/**
	 * Save an IAM record from an HTTP request parameter Map.
	 * <ul>
	 * <li> Common (to all): type (AzureActiveDirectory, WindowsAD), name, domain.
	 * <li> Azure: iam_azuread_tenant, iam_azuread_client, iam_azuread_secret, iam_azuread_res, iam_azuread_ep
	 * <li> WinAD: domain, iam_winad_server.
	 * </ul>
	 * @param request Request parameters: required: type (AzureActiveDirectory, WindowsAD), name.
	 */
	public static void save (Map<String, String[]> request ) throws IOException, JSONException {

		// common
		final String type 		= getParameters(request, "type", true, "Type is required")[0];
		final String name 		= getParameters(request, "name" , true, "A name/label is required")[0];
	
		// Win AD
		boolean isWinAD 		= type.equals(Types.WindowsAD.name());
		final String domain 	= getParameters(request, "domain", isWinAD, "Domain is required")[0];
		final String server 	= getParameters(request, "iam_winad_server", isWinAD, "Server is required")[0];
		
		// azure
		boolean isAzure 		= type.equals(Types.AzureActiveDirectory.name());
		final String tenant 	= getParameters(request, "iam_azuread_tenant", isAzure, "Tenant is required")[0];
		final String clientId 	= getParameters(request, "iam_azuread_client", isAzure, "Client Id is required")[0];
		final String secret 	= getParameters(request, "iam_azuread_secret", isAzure, "Secret is required")[0];
		final String resource 	= getParameters(request, "iam_azuread_res", isAzure, "Resource is required")[0];
		final String endPoint 	= getParameters(request, "iam_azuread_ep", isAzure, "Token EndPoint is required")[0];

		// cleanup request
		request.remove("rq_operation");

		// new request
		JSONObject id			= new JSONObject(request);

		// inject domains: required to login
		/* 12/30/2020 This requires sys admin permissions
		if ( isAzure) {
			// azure: get access token, if ok,  save
			final String tok 	= MsGraph.getAccessToken(endPoint, clientId, secret);
			JSONArray domains 	= new JSONArray(extractValues(MsGraph.getDomains(tok), "id"));
			
			id.put("domain", domains);
		} */
		
		// add record
		JSONArray ids 	= root.has("data") ? root.getJSONArray("data") : new JSONArray();
		ids.put(id);
		root.put("data", ids);
		
		internalSave();
	}
	
	private static void internalSave() throws IOException, JSONException {
		final String path		= getBasePath(); 

		log.debug("[IAM] Saving  " + root +  " @ " + path);
		IOTools.saveText(path, root.toString(1));
	}

	/**
	 * <pre>{"data": [{ "iam_azuread_secret": [".485Xp02Q~_x70Wq5D6IZ~Q1B_mHf8DL._"],
  "iam_azuread_res": ["https://graph.microsoft.com/"],
  "name": ["Test"],
  "type": ["AzureActiveDirectory"],
  "iam_azuread_ep": ["https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25/oauth2/token"],
  "iam_azuread_tenant": ["e45b00f2-be66-4d82-a9d5-362fc62f5c25"],
  "iam_azuread_client": ["e3c88a88-a888-4c2b-8437-d35d85da749c"] }...] </pre>
	 */
	public static JSONObject describe () {
		return root;
	}

	/**
	 * Delete IAM record. <pre>{"data": [{ "iam_azuread_secret": [".485Xp02Q~_x70Wq5D6IZ~Q1B_mHf8DL._"],
  "iam_azuread_res": ["https://graph.microsoft.com/"],
  "name": ["Test"],
  "type": ["AzureActiveDirectory"],
  "iam_azuread_ep": ["https://login.microsoftonline.com/e45b00f2-be66-4d82-a9d5-362fc62f5c25/oauth2/token"],
  "iam_azuread_tenant": ["e45b00f2-be66-4d82-a9d5-362fc62f5c25"],
  "iam_azuread_client": ["e3c88a88-a888-4c2b-8437-d35d85da749c"]
 } </pre>
	 * @param name Record name
	 */
	public static void del(String name) throws JSONException, IOException {
		if ( name == null ) {
			throw new IOException("A name is required");
		}
		JSONArray data = root.getJSONArray("data");
		for (int i = 0; i < data.length(); i++) {
			JSONObject obj = data.getJSONObject(i);
			if ( obj.has("name") && obj.getJSONArray("name").get(0).toString().equals(name)) {
				data.remove(i);
			}
		}
		internalSave();
	}
	
	/**
	 * @return A list of identity names or NULL if no ids available.
	 */
	public static List<String> getNames () {
		if ( !root.has("data")) {
			return null;
		}
		try {
			JSONArray data 		= root.getJSONArray("data");
			return extractValues(data, "name");
		} catch (Exception e) {
			log.error("[IAM] Get Names", e);
			return null;
		}
	}
	
	/*
	 * Extract values from a JSON array given a key (array or object)
	 */
	static List<String> extractValues (JSONArray data, final String key) throws JSONException {
		List<String> list 	= new ArrayList<String>();
		
		for (int i = 0; i < data.length(); i++) {
			JSONObject elem = data.getJSONObject(i);
			// "name": ["C1AS"]
			Object obj 		= elem.opt(key);	

			if ( obj == null ) {
				continue;
			}
			list.add (obj instanceof JSONArray 
					? ((JSONArray)obj).getString(0) // "name": ["C1AS"]
					: obj.toString());
		}
		return data.length() > 0 ? list : null; 
	}

	static List<String> extractValues (final String key) throws JSONException {
		return extractValues(root.getJSONArray("data"), key);
	}

	static String extractFirstValue (final String key) throws JSONException {
		return extractValues(root.getJSONArray("data"), key).get(0);
	}

	static String findFirstValue(final String searchKey, final String searchVal, final String desiredKey) throws JSONException {
		JSONArray data = root.getJSONArray("data");
		
		for (int i = 0; i < data.length(); i++) {
			JSONObject obj 	= data.getJSONObject(i);
			// "name": ["C1AS"]
			JSONArray vals 	= obj.getJSONArray(searchKey);
			
			if ( obj.has(searchKey) && vals.get(0).toString().equals(searchVal)) {
				// "type": ["AzureActiveDirectory1"]
				return obj.getJSONArray(desiredKey).getString(0);
			}
		}
		return null;
	}
	
	/**
	 * Authenticate a user.
	 * @param user User name. Format: user-name@full-domain or user-name.
	 * @param pwd user password.
	 * @param identity IAM identity name.
	 * @throws JSONException on JSON IO/parse errors.
	 * @throws IOException on Login failures.
	 */
	public static void authenticate (final String user, final String pwd, final String identity) throws JSONException, IOException {
		if ( identity == null)	throw new IOException("IAM identity is required");
		
		final String type 		= findFirstValue("name", identity, "type");
		final String domain 	= findFirstValue("name", identity, "domain");
		
		if (type == null) 		throw new IOException("Inavlid or missing identity " + identity);
		if (domain == null) 	throw new IOException("Missing domain for identity " + identity);
		
		String userName 		= user.contains("@") ? user : user + "@" + domain;

		log.debug("[AUTHENTICATE] " + user + "/" + pwd + " Identiry:" + identity + " Type:" + type + " Domain:" + domain);

		if ( type.equals(Types.AzureActiveDirectory.name()) ) {
			// azure
			final String endPoint 	= findFirstValue("name", identity, "iam_azuread_ep");
			final String clientId 	= findFirstValue("name", identity, "iam_azuread_client");
			final String secret 	= findFirstValue("name", identity, "iam_azuread_secret");
			
			log.debug("[AUTHENTICATE] AZURE ep=" + endPoint + " Cid=" + clientId + " User:" + userName);
			MsGraph.getAccessTokenViaPassword(endPoint, clientId, secret, userName, pwd);
		}
		else {
			// win ad: append server to user if missing. Note server cannot be an IP addr else login will fail.
			final String server =  findFirstValue("name", identity, "iam_winad_server");
			userName 			= user.contains("@") ? user : user + "@" + server;
			
			log.debug("[AUTHENTICATE] WINAD user=" + userName);
			RemoteAuthenticator.authenticate(userName, pwd);
		}
	}
}
