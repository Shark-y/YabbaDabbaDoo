package com.cloud.core.w3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.w3.RestException;
import com.cloud.core.w3.WebClient;
import com.cloud.core.io.IOTools;
import com.cloud.core.security.SecurityTool;
import com.cloud.core.types.CoreTypes;

/**
 * A general purpose REST client. It supports
 * <ul>
 * <li> Dual SSL via JKS client certificates.
 * <li> Basic authorization
 * <li> It reads end points from a JSON descriptor.
 * </ul>
 * <pre>{
  "version": "v1.24",
  "endPoints": [
	{
	  "name": "GetContainers",
	  "desc": "Get Containers",
	  "uri": "containers/json?all=1",
	  "method": "GET",
	  "headers": [
		{ "key1" : "val1"}
	  ]
	}
  ]
}</pre>
 * @author VSilva
 * @version 1.0.0 - 2/24/2020
 *
 */
public class RestClient {

	static {
		try {
			enablePATCHInHttpURLConnection();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This class wrap an endpoint within a REST descriptor: <pre>{ "name": "GetContainers",
	  "desc": "Get Containers",
	  "uri": "containers/json?all=1",
	  "method": "GET",
	  "headers": [
		{ "key1" : "val1"}
	  ]
	} </pre>
	
	 * @author VSilva
	 *
	 */
	static class EndPoint implements Serializable {
		private static final long serialVersionUID = -6098565809825505427L;
		final String name;
		final String desc;
		final String uri;
		final String method;
		final String contentType;
		final Map<String, String> headers;
		final Map<String, String> data;
		
		public EndPoint(JSONObject root) throws JSONException {
			name		= root.getString("name");
			desc		= root.optString("desc");
			uri 		= root.getString("uri");
			method		= root.getString("method");
			contentType	= root.has("contentType") ? root.getString("contentType") : null;
			headers		= new HashMap<String, String>();
			data		= new HashMap<String, String>();
			
			// optional (may be null)
			JSONArray array = root.optJSONArray("headers");
			
			if ( array != null) {
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj 		= array.getJSONObject(i);
					Set<String> keys 	= obj.keySet();
					
					for ( String key : keys) {
						//System.out.println(key + "=" + obj.getString(key));
						headers.put(key, obj.getString(key));
					}
				}
			}
			// Payload data - optional (may be null)
			array = root.optJSONArray("data");
			
			if ( array != null) {
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj 		= array.getJSONObject(i);
					data.put(obj.getString("key"), obj.getString("val"));
				}
			}
			
		}

		@Override
		public String toString() {
			return name + " "  + desc + " " + method + " " +  uri + " " + headers;
		}
	}
	
	/**
	 * This class wraps URL request information such as:
	 * <ul>
	 * <li> Endpoint URL, 
	 * <li> Request content,
	 * <li> SSL vs non-SSL information, and 
	 * <li> Java key store information: path, password (for Dual SSL).
	 * </ul>
	 * @author VSilva
	 *
	 */
	public static class HTTPDestination implements Serializable {
		private static final long serialVersionUID = -156591772387056212L;
		
		String baseUrl;
		String content;
		String contentType;
		
		String sslProto;
		String keyStorePath;
		String keyStorePassword;

		// 2/24/2020 Basic Auth
		String user;
		String password;
		
		boolean debug;
		
		/**
		 * Construct
		 * @param baseUrl Request URL
		 * @param keyStorePath Dual SSL: client JKS full path.
		 * @param keyStorePassword Dual SSL: client JKS password.
		 * @throws RestException On security errors
		 */
		public HTTPDestination(String baseUrl, String keyStorePath, String keyStorePassword) throws RestException {
			this(baseUrl, null, null, "TLS", keyStorePath, keyStorePassword);
		}
		
		/**
		 * Construct
		 * @param baseUrl Request URL
		 * @param content Request payload.
		 * @param contentType Content type.
		 * @param sslProto Default: TLS.
		 * @param keyStorePath Dual SSL: client JKS full path.
		 * @param keyStorePassword Dual SSL: client JKS password.
		 * @throws RestException
		 */
		public HTTPDestination(String baseUrl, String content, String contentType, String sslProto, String keyStorePath, String keyStorePassword) throws RestException {
			super();
			this.baseUrl 			= baseUrl;
			this.content			= content;
			this.contentType		= contentType;
			this.sslProto 			= sslProto;
			this.keyStorePath 		= keyStorePath != null && !keyStorePath.isEmpty() ? keyStorePath : null;
			this.keyStorePassword 	= keyStorePassword != null && !keyStorePassword.isEmpty() ? keyStorePassword : null;
			
			// some validations...
			/* FIXME Some HTTPS destinations don't required DUAL SSL
			if ( this.baseUrl.startsWith("https") && ( keyStorePath == null || keyStorePassword == null)) {
				throw new AbstractRestException("A keystore and ketstore password are required when protocol is HTTPS " + baseUrl, 400, "BAD REQUEST");
			} */
		}

		/**
		 * Construct.
		 * @param baseUrl Request URL
		 * @param content Request payload.
		 * @param contentType Content type.
		 * @param sslProto SSL protocol. Default: TLS.
		 * @param keyStorePath Dual SSL: client JKS full path.
		 * @param keyStorePassword Dual SSL: client JKS password.
		 * @throws RestException On REST/HTTP request errors.
		 * @return {@link HTTPDestination}.
		 */
		public static HTTPDestination create (String baseUrl, String content, String contentType, String sslProto, String keyStorePath, String keyStorePassword) throws RestException {
			return new HTTPDestination(baseUrl, content, contentType, sslProto, keyStorePath, keyStorePassword);
		}
	
		public static HTTPDestination create (String baseUrl, String content, String contentType, String keyStorePath, String keyStorePassword) throws RestException {
			return new HTTPDestination(baseUrl, content, contentType, "TLS", keyStorePath, keyStorePassword);
		}

		public static HTTPDestination create (String baseUrl, String keyStorePath, String keyStorePassword) throws RestException {
			return new HTTPDestination(baseUrl, keyStorePath, keyStorePassword);
		}
		
		public boolean isSSL () {
			return sslProto != null && keyStorePath != null && keyStorePassword != null;
		}

		public void setAuthorization (String user, String password) throws RestException {
			this.user 		= user;
			this.password 	= password;
		}
		
		public void setDebug (boolean debug) {
			this.debug = debug;
		}

		public void setContentType (final String ct) {
			this.contentType = ct;
		}
	
		@Override
		public String toString() {
			return baseUrl;
		}
	}
	
	/**  API version */
	String version;
	
	/** Endpoint list */
	final List<EndPoint> endPoints;
	
	/** HTTP client workhorse */
	final WebClient wc;
	
	public RestClient() {
		endPoints 	= new ArrayList<EndPoint>();
		wc			= new WebClient();

		try {
			// Fixes: javax.net.ssl.SSLHandshakeException: java.security.cert.CertificateException: No subject alternative names matching IP address 10.0.2.15 found
			SecurityTool.disableClientSSLVerificationFromHttpsURLConnection("TLS");
		} catch (Exception e) {
		}
	}
	
	/**
	 * Load a JSON API descriptor from an {@link InputStream}.
	 * @param is The stream to read from.
	 * @throws JSONException on JSON parse errors.
	 * @throws IOException on I/O errors.
	 */
	public synchronized void load (InputStream is) throws JSONException, IOException {
		load(new JSONObject(IOTools.readFromStream(is)));
	}
	
	/**
	 * Load the REST end points from the given {@link JSONObject}. The format is described above.
	 * @param root JSON: { version: 1, endPoints: [ {name: xx, desc: xxx, uri: http..., method: GET },...]
	 * @throws JSONException On JSON errors.
	 * @throws IOException On I/O or W3 errors.
	 */
	public synchronized void load (JSONObject root) throws JSONException, IOException {
		//JSONObject root = new JSONObject(IOTools.readFromStream(is));
		version 		= root.optString("version");
		
		JSONArray array = root.getJSONArray("endPoints");
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			endPoints.add(new EndPoint(obj));
		}
	}
	
	private EndPoint findEndPoint (final String name) {
		for ( EndPoint ep : endPoints) {
			if ( ep.name.equals(name)) {
				return ep;
			}
		}
		return null;
	}
	
	public static String replaceParams (Map<String, Object> params , String raw) {
		String str = raw;
		for ( Map.Entry<String, Object> entry: params.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			// FIXME 5/16/2019 str = str.replaceAll("\\$\\{" + key  +"\\}", val.toString());
			str = str.replace("${" + key  + "}", val.toString());
		}
		return str;
	}
	
	/**
	 * Invoke an API call.
	 * @param dest Contains destination URL information. See {@link HTTPDestination}.
	 * @param apiKey The name of the target API within the REST descriptor.
	 * @return Either a {@link JSONObject}, a {@link JSONArray} or the raw response from the server.
	 * @throws RestException If the target API is not found.
	 * @throws Exception On W3 I/O errors: connections timeouts, etc.
	 */
	public synchronized Object invoke (HTTPDestination dest, String apiKey) throws RestException, Exception {
		return invoke(dest, apiKey, null);
	}

	/**
	 * Invoke an API call.
	 * @param dest Contains destination URL information. See {@link HTTPDestination}.
	 * @param apiKey The name of the target API within the REST descriptor.
	 * @param params A {@link Map} of parameter (KEY, VALUE) pairs to substitute in the request URI, and others.
	 * @return Either a {@link JSONObject}, a {@link JSONArray} or the raw response from the server.
	 * @throws RestException If the target API is not found.
	 * @throws Exception On W3 I/O errors: connections timeouts, etc.
	 */
	public synchronized Object invoke (HTTPDestination dest, String apiKey, Map<String, Object> params) throws RestException, IOException, Exception {
		EndPoint ep 		= findEndPoint(apiKey);
		if ( ep == null) {
			throw new RestException(apiKey + " not found.", 500, "Internal Server Error");
		}
		//final String url 	= dest.baseUrl + (version != null && !version.isEmpty() ? version + "/" : "") + ( params != null ? replaceParams(params, ep.uri) : ep.uri);
		// Add version?
		if ( version != null && !version.isEmpty() && (params != null) ) {
			params.put("API_VERSION", version);
		}
		final String url 	= dest.baseUrl +  ( params != null ? replaceParams(params, ep.uri) : ep.uri);
		
		wc.setUrl(url);

		if ( dest.isSSL()) {
			wc.setSSLParams(dest.sslProto, dest.keyStorePath, dest.keyStorePassword);
		}
		// 2/24/2020 Basic auth?
		if ( dest.user != null && dest.password != null) {
			wc.setAuthorization(dest.user, dest.password);
		}
		
		/* DEBUG ONLY */
		wc.setVerbosity(dest.debug);
		wc.logToStdOut(dest.debug); 
		
		String contentType			= dest.contentType != null 	? dest.contentType : CoreTypes.CONTENT_TYPE_JSON;
		byte[] content				= dest.content != null 		? dest.content.getBytes(CoreTypes.CHARSET_UTF8) : null;
		ByteArrayOutputStream bos 	= new ByteArrayOutputStream();
		
		// 3/23/2020 Support for HttpUSRLConnection PATCH 
		boolean doOutput 			= ep.method.toLowerCase().startsWith("p"); // POST/PATCH equalsIgnoreCase("post");
		boolean followRedirects 	= true;
		boolean useCaches 			= true;
	
		// substitute headers
		// Note: Cannot substitute ep.headers - that will destroy any VARS
		Map<String, String> hdrs = new HashMap<String, String>(ep.headers);
		replaceHeaders(hdrs, params);

		// 2/24/2020 Convert payload?
		if ( ep.data != null && (ep.data.size() > 0) ) {
			Map<String, String> data = new HashMap<String, String>(ep.data);
			replaceHeaders(data, params);
			content = mapToQueryString(data).getBytes(CoreTypes.CHARSET_UTF8);
		}
		// 3/23/2020 Override with the endpoint ct
		if ( ep.contentType != null ) {
			contentType = ep.contentType;
		}
		
		wc.doInputOutputRequest(bos, ep.method, content, contentType, hdrs, true, doOutput , followRedirects, useCaches);
		wc.close();
		bos.close();

		// JSONObject or JSONArray or RAW (logs) or Chunked JSON {JSON1}\n{JSON2}\n....
		final String response 		= bos.toString(CoreTypes.DEFAULT_ENCODING);

		// check the response: HTTP/1.1 400 Bad Request
		// {"message":"Requested CPUs are not available - requested 0,1, available: 0"}
		if ( wc.getResponseCode() >= 400) {
			throw new RestException(response, wc.getResponseCode(), wc.getResponseMessage());
		}
		
		/* 5/17/2019 Sample Chunked JSON Response: [POST]  [HDR] Transfer-Encoding = [chunked]
		{"status":"Trying to pull repository us.gcr.io/cloud-bots/agentaggregator ... "}
		{"status":"Pulling repository us.gcr.io/cloud-bots/agentaggregator"}
		{"errorDetail":{"message":"unauthorized: authentication required"},"error":"unauthorized: authentication required"}
		*/
		final String tenc 		= wc.getHeaderAtIndex("Transfer-Encoding", 0);
		//final String respCt		= wc.getHeaderAtIndex("Content-Type", 0);
		final boolean isJSON 	= response.startsWith("{") || response.startsWith("["); 

		// Chunked responses can be RAW too. So only split for JSON
		if ( tenc != null && tenc.equals("chunked") && isJSON) {
			// Split chunks & put them in a JSON array, return array (only if szie > 1)
			String[] chunks 	= response.split("\n");
			JSONArray jchunks 	= new JSONArray();

			if ( chunks.length > 1 ) {
				for (int i = 0; i < chunks.length; i++) {
					final String chunk = chunks[i];
					// 12/25/2020 JSON Bug: remove duplicate keys from JSON org.json.JSONException: Duplicate key "time"
					// {"level":"info","cmd":"hub","bytes_in":"","bytes_out":1868,"host":"10.244.0.0","method":"GET","port":"","status":200,"took":0.47308,"time":"2020-12-26T00:33:36Z","time":"2020-12-26T00:33:36Z","message":"/static/media/logo.png"}
					jchunks.put( chunk.startsWith("{") 
						? new JSONObject(chunk) {
							@Override
							public JSONObject putOnce(String key, Object value) throws JSONException {
						        if (key != null && value != null) {
						            this.put(key, value);
						        }
						        return this;
							}
						}
						: response.startsWith("[") ? new JSONArray(chunk) : response );
				}
				return jchunks;
			}
			else {
				return response.startsWith("{") ? new JSONObject(response) : response.startsWith("[") ? new JSONArray(response) : response;
			}
		}
		else {
			return response.startsWith("{") ? new JSONObject(response) : response.startsWith("[") ? new JSONArray(response) : response;
		}
		//return response.startsWith("{") ? new JSONObject(response) : response.startsWith("[") ? new JSONArray(response) : response;
	}
	
	private static void replaceHeaders (Map<String, String> headers, Map<String, Object> params) {
		if ( headers == null || params == null ) {
			return;
		}
		for ( Map.Entry<String, String> entry : headers.entrySet()) {
			entry.setValue(replaceParams(params, entry.getValue()));
		}
	}
	
	private String mapToQueryString (Map<String, String> data) {
		StringBuffer buf = new StringBuffer();
		for ( Map.Entry<String, String> entry : data.entrySet()) {
			//entry.setValue(replaceParams(params, entry.getValue()));
			buf.append(entry.getKey() + "=" + entry.getValue() + "&");
		}
		return buf.toString();
	}
	
	public String[] getEndPointNames () {
		String[] array = new String[endPoints.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = endPoints.get(i).name;
		}
		return array;
	}
	
	@Override
	public String toString() {
		return version + " " + endPoints;
	}

	/**
	 * {@link HttpURLConnection} does not support PATCH. This gives an errors in some API servers.
	 * This method uses reflection to update the methods static final array to
	 * ["GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH" ]
	 * @throws Exception On any reflection error.
	 */
	public static void enablePATCHInHttpURLConnection () throws Exception {
		// Enable PATCH in java.net.HttpURLConnection by updating the methods static final array
		// with ["GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH" ]
		Field field =  HttpURLConnection.class.getDeclaredField("methods");
		field.setAccessible(true);
		
		 //'modifiers' - it is a field of a class called 'Field'. Make it accessible and remove
        Field modifiersField = Field.class.getDeclaredField( "modifiers" );
        modifiersField.setAccessible( true );
        modifiersField.setInt( field, field.getModifiers() & ~Modifier.FINAL );
         
		field.set(null , new String[] { "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH" });
	}

}
