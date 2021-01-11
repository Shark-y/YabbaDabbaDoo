package com.cloud.core.w3;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.cloud.core.w3.OAuth1;
import com.cloud.core.w3.WebClient;
import com.cloud.core.w3.OAuth1.Parameters;
import com.cloud.core.w3.OAuth1.Secrets;

/**
 * A Web Client that supports OAuth1 protocol authorization.
 * <pre>
 * String consumerKey    = "d13eba0e667a41178280bd77d6daa890";
 * String consumerSecret = "78d1e06a80e5eeeb";
 * String token          = "c54bf947392f408bb6ef956d3802428c";
 * String tokenSecret    = "552f0997ed0c6c8e";
 * String url            = "https://cloud.lprnd.net/api/account/D74952746/externalAvailability/skillAvailability.json?v=1";
 *
 * String payload = "{\"skillInformation\":{\"skill\":[{ \"id\":\"OM Work Inbound\",\"QueueWaitTime\":111,\"AvailableSlots\":1,\"QueueOnline\":true,\"SessionInQueue\":0,\"Throughput\":3,\"mediaType\":\"CHAT\"}]}}";
 *
 * OAuth1WebClient oc = new OAuth1WebClient(consumerKey, consumerSecret, token, tokenSecret);
 * oc.doPost( url, payload, CoreTypes.CONTENT_TYEPE_JSON);
 *</pre>
 * 
 * <b>Note: </b> This class extends {@link WebClient} and has no external dependencies.
 * 
 * @author vsilva
 * @version 1.0.0
 * @see https://oauth1.wp-api.org/docs/basics/Signing.html
 *
 */
public class OAuth1WebClient extends WebClient {

	private OAuth1.Parameters params;
	private OAuth1.Secrets secrets;
	
	/**
	 * Construct a client.
	 * @param consumerKey Application Id 
	 * @param consumerSecret Application secret
	 * @param token Token Id
	 * @param tokenSecret token secret
	 * @param debug Show request/response information
	 */
	public OAuth1WebClient(String consumerKey, String consumerSecret, String token, String tokenSecret ) {
		super();
		this.params 	= new OAuth1.Parameters(consumerKey, token);
		this.secrets	= new OAuth1.Secrets(consumerSecret, tokenSecret);
	}

	/**
	 * Construct a client.
	 * @param consumerKey Application Id 
	 * @param consumerSecret Application secret
	 * @param token Token Id
	 * @param tokenSecret token secret
	 * @param connectTimeout Connect timeout in milliseconds.
	 * @param readTimeout read timeout in milliseconds.
	 * @param debug Show request/response information
	 */
	public OAuth1WebClient(String consumerKey, String consumerSecret, String token, String tokenSecret, int connectTimeout, int readTimeout )
	{
		super();
		this.params 			= new OAuth1.Parameters(consumerKey, token);
		this.secrets			= new OAuth1.Secrets(consumerSecret, tokenSecret);
		super.connectTimeout 	= connectTimeout;
		super.readTimeout		= readTimeout;
	}

	/**
	 * Construct an OAuth1 client.
	 * @param url The URL to connect to.
	 * @param consumerKey Application Id 
	 * @param consumerSecret Application secret.
	 * @param token Token Id.
	 * @param tokenSecret token secret.
	 * @throws IOException On I/O errors.
	 */
	public OAuth1WebClient(String url, String consumerKey, String consumerSecret, String token, String tokenSecret ) 
			throws IOException 
	{
		this(url, new OAuth1.Parameters(consumerKey, token),  new OAuth1.Secrets(consumerSecret, tokenSecret));
	}
	
	/**
	 * Construct an OAuth1 client.
	 * @param url The URL to connect to.
	 * @param params OAuth1 {@link Parameters}.
	 * @param secrets OAuth1 {@link Secrets}.
	 * @throws IOException On I/O errors.
	 */
	public OAuth1WebClient(String url, OAuth1.Parameters params, OAuth1.Secrets secrets) throws IOException {
		super(url);
		this.params 	= params;
		this.secrets	= secrets;
		
	}

	/**
	 * HTTP GET
	 * @param url The URL.
	 * @return Response output.
	 * @throws IOException If a request error occurs.
	 */
	public String doGet(String url) throws MalformedURLException, IOException {
		super.url = new URL(url);
		return super.doGet();
	}
	
	/**
	 * HTTP POST
	 * @param url HTTP Post Url.
	 * @param content Data payload.
	 * @param contentType Type of data. For example: application/json.
	 * @return HTTP response content.
	 * @throws IOException
	 */
	public String doPost(String url, String content, String contentType) throws MalformedURLException, IOException {
		return doPost(url, content, contentType, null);
	}

	/**
	 * HTTP POST
	 * @param url HTTP Url.
	 * @param content Data payload.
	 * @param contentType type of data. For example: application/json.
	 * @param requestProperties Optional request headers or null.
	 * @return  HTTP response.
	 * @throws IOException
	 */
	public String doPost(String url, String content, String contentType, Map<String, String> requestProperties) throws MalformedURLException, IOException {
		super.url = new URL(url);
		return super.doPost(content, contentType, requestProperties);
	}
	
	/**
	 * Do an HTTP PUT via a POST with the header X-HTTP-Method-Override: PUT
	 * @param url HTTP URL.
	 * @param payload Data payload 
	 * @param contentType see {@link MediaType}
	 * @return Response output.
	 * @throws IOException
	 */
	public String doPut(String url, String payload, String contentType ) throws IOException 
	{
		super.url = new URL(url);
		
		Map<String, String> hdrs 	= new HashMap<String, String>();
		hdrs.put("X-HTTP-Method-Override", "PUT");
		
		return super.doPost(payload, contentType, hdrs );
	}

	/**
	 * Currently DELETE is supported using a POST method with the "X-HTTP-Method-Override:DELETE" header.
	 * @param url HTTP URL.
	 * @param payload Data payload.
	 * @param contentType see {@link CoreTypes}
	 * @return Response output.
	 * @throws IOException
	 */
	public String doDelete(String url, String payload, String contentType ) throws IOException 
	{
		super.url = new URL(url);
		
		Map<String, String> hdrs 	= new HashMap<String, String>();
		hdrs.put("X-HTTP-Method-Override", "DELETE");
		
		return super.doPost(payload, contentType, hdrs );
	}

	@Override
	public int doGet(OutputStream os, Map<String, String> requestProperties) throws IOException {
		String queryString 	= getQueryString();
		String baseUrl		= getBaseUrl();
		
		// Generate the OAuth1 header
		String hdr 		= OAuth1.generateAuthorizationHeader("get", baseUrl, queryString, params, secrets);
		
		if ( requestProperties != null) {
			requestProperties.put("authorization", hdr);
			return super.doGet(os, requestProperties);
		}
		else {
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("authorization", hdr);
			return super.doGet(os, headers);
			
		}
	}
	
	@Override
	public /*protected*/ String doInputOutputRequest(String method, /*String*/byte[] content, String contentType, Map<String, String> requestProperties)
			throws MalformedURLException, IOException 
	{
		String queryString 	= getQueryString();
		String baseUrl		= getBaseUrl();
		
		// Generate the OAuth1 header
		LOGD("OAUTH1", String.format("==== %s Url = %s QueryString = %s", method, baseUrl, queryString));
		LOGD("OAUTH1", String.format("Params = %s", params));
		LOGD("OAUTH1", String.format("Secrets = %s", secrets));
		
		String hdr 		= OAuth1.generateAuthorizationHeader(method, baseUrl, queryString, params, secrets);
		
		if ( requestProperties != null) {
			requestProperties.put("authorization", hdr);
			return super.doInputOutputRequest(method, content, contentType, requestProperties);
		}
		else {
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("authorization", hdr);
			return super.doInputOutputRequest(method, content, contentType, headers);
			
		}
	}
	
	/**
	 * Get a header value for name at index (0). Note: Headers are multi-valued maps.
	 * @param name Header name.
	 * @return Multi-valued Header value at index 0.
	 */
	public String getHeaderFirst (String name) {
		return super.getHeaderAtIndex(name, 0);
	}

	public void destroy () {
		super.close();
		params = null;
		secrets = null;
	}

}
