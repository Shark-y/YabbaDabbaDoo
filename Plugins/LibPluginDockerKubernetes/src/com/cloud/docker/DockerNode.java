package com.cloud.docker;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;

import com.cloud.core.io.IOTools;
import com.cloud.core.w3.RestClient;

/**
 * Wrap information about a local or remote Docker daemon such as
 * <ul>
 * <li> Name
 * <li> Host IP:port
 * <li> If TLS is enabled
 * <li> Java keystore info  for TLS connections
 * </ul>
 * 
 * This class uses the {@link RestClient} to load a docker API descriptor used to invoke node operations.
 * 
 * @author VSilva
 *
 */
public class DockerNode {
	
	String name;
	String hostPort;
	boolean tlsEnabled;
	
	String keyStorePath;
	String keyStorePassword;
	
	// REST clients cannot be shared among TLS, non-TLS connections (nodes) or else we get
	// java.lang.ClassCastException: sun.net.www.protocol.http.HttpURLConnection cannot be cast to javax.net.ssl.HttpsURLConnection
	final RestClient rest = new RestClient();
	
	public DockerNode() throws JSONException, IOException {
		super();
		
		// Load Docker Api
		InputStream is 	= null ;
		try {
			is 			= Docker.class.getResourceAsStream("/configuration/api_docker.json");
			rest.load(is);
		}
		finally {
			IOTools.closeStream(is);
		}
	}
	
	public DockerNode(final String name, final String hostPort, final boolean tlsEnabled, final String keyStorePath, final String keyStorePassword) throws JSONException, IOException {
		this();
		this.name = name;
		this.hostPort = hostPort;
		this.tlsEnabled = tlsEnabled;
		this.keyStorePath = keyStorePath;
		this.keyStorePassword = keyStorePassword;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return The node name plus the host:port. (e.g Node1:10.0.0.1:2376)
	 */
	public String getFullName () {
		return name + " (" + hostPort + ")";
	}

	/**
	 * @return The IP/host part from the IP:PORT
	 */
	public String getHost() {
		return hostPort != null ? hostPort.split(":")[0] : null;
	}

	public String getHostPort() {
		return hostPort;
	}

	public boolean isTlsEnabled() {
		return tlsEnabled;
	}

	public String getKeyStorePath() {
		return keyStorePath;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}
	
	public String buildBaseUrl () {
		return tlsEnabled ? "https://" + hostPort + "/" : "http://" + hostPort + "/";
	}
	
	@Override
	public String toString() {
		return name + " H:" + hostPort + " tls:" + tlsEnabled + " ks:" + keyStorePath + " ksp:" + keyStorePassword;
	}
}
