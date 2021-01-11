package com.cluster.update;

import java.io.IOException;

import com.cloud.core.w3.WebClient;

/**
 * https://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html
 * 
 * <pre>
 * TomcatManagerClient tc = new TomcatManagerClient("http://localhost:8080/manager", "tomcat", "tomcat");
 * String name = "CloudConnectorAES01";
 * String version = "release-1.1-20180411";
 * tc.stopWebApp(name, version);
 * </pre>
 * 
 * @version 1.0.0 - 6/14/2018 Initial commit.
 */
public class TomcatManagerClient {

	String baseUrl;
	WebClient w3;
	
	/**
	 * Construct.
	 * @param url Manager App URL: http://localhost:8080/manager
	 * @param user User name.
	 * @param pwd Password.
	 */
	public TomcatManagerClient(final String url, final String user, final String pwd) {
		super();
		this.baseUrl = url;
		this.w3 = new WebClient();
		w3.setAuthorization(user, pwd);
	}
	
	/**
	 * See - https://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html#Stop_an_Existing_Application
	 * @param name Application name.
	 * @param version Application version.
	 * 
	 * @return The manager response: OK - Stopped application at context path /CloudConnectorAES01##release-1.1-20180411
	 * 
	 * @throws IOException on Network errors.
	 */
	public String stopWebApp(final String name, final String version) throws IOException {
		if ( name == null ) {
			throw new IOException("App name is required.");
		}
		String url = baseUrl + "/text/stop?path=/" + name + (version != null ?  "&version=" + version : "");
		//w3.logToStdOut(true);
		//w3.setDebug(true);
		w3.setUrl(url);
		String resp = w3.doGet();
		
		if ( w3.getStatus() >= 300) {
			throw new IOException(w3.getResponseCode() + " " + w3.getResponseMessage());
		}
		return resp;
	}
	
}
