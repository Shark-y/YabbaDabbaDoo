package com.cloud.kubernetes;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;
import com.cloud.core.w3.RestClient;

/**
 * Wrap information about a local or remote Docker daemon such as
 * <ul>
 * <li> Name
 * <li> Host IP:port
 * </ul>
 * 
 * This class uses the {@link RestClient} to load a docker API descriptor used to invoke node operations.
 * 
 * @author VSilva
 *
 */
public class K8SNode {
	
	String name;
	String apiServer;
	
	String acessToken;
	String sshUser;
	String sshPwd;
	
	// REST clients cannot be shared among TLS, non-TLS connections (nodes) or else we get
	// java.lang.ClassCastException: sun.net.www.protocol.http.HttpURLConnection cannot be cast to javax.net.ssl.HttpsURLConnection
	// 4/5/2020 This locks multithreaded clients causing latencies of 20s+
	// final RestClient rest = new RestClient();
	
	/** The {@link JSONObject} that wraps this client. See {@link RestClient} for format details */
	final JSONObject api ;
	
	public K8SNode() throws JSONException, IOException {
		super();
		
		// Load Docker Api
		InputStream is 	= null ;
		try {
			is 			= Kubernetes.class.getResourceAsStream("/configuration/api_k8s.json");
			// 4/5/2020 removed due to bad latencies rest.load(is);
			// keep the api in memory to speed things up
			api 		= new JSONObject(IOTools.readFromStream(is));
		}
		finally {
			IOTools.closeStream(is);
		}
	}
	
	public K8SNode(final String name, final String apiServer,  final String accessToken, final String sshUser, final String sshPwd) throws JSONException, IOException {
		this();
		if ( name 		== null)		throw new IOException("Node name can't be null");
		if ( apiServer 	== null)		throw new IOException("API Server can't be null");
		if ( accessToken == null)		throw new IOException("Access Token can't be null");
		
		this.name 		= name;
		this.apiServer 	= apiServer.endsWith("/") ? apiServer : apiServer + "/";
		this.acessToken = accessToken;
		this.sshUser 	= sshUser;
		this.sshPwd		= sshPwd;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return The node name plus the host:port. (e.g Node1:10.0.0.1:2376)
	 */
	public String getFullName () {
		return name + " (" + apiServer + ")";
	}
	
	public String getApiServer() {
		return apiServer;
	}

	/**
	 * @return The Host name/IP address from the API Server URL.
	 * @throws MalformedURLException
	 */
	public String getHostName() throws MalformedURLException {
		return apiServer != null ? ( new URL(apiServer)).getHost() : null;
	}

	public String getAcessToken() {
		return acessToken;
	}

	public String getSSHUser() {
		return sshUser;
	}
	
	public String getSSHPwd () {
		return sshPwd;
	}
	
	@Override
	public String toString() {
		return name + " H:" + apiServer + " Tok:" + acessToken + " SSH User:" + sshUser;
	}
}
