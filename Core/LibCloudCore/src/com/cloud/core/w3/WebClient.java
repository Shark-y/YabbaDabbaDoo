package com.cloud.core.w3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.cloud.core.io.Base64;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;


/**
 * Very basic and simple Web client tool using the {@link HttpURLConnection} class. It supports:
 * <ul>
 * <li> GET, POST (url-encoded), PUT, DELETE
 * <li> HTTPS. See {@link HttpURLConnection}.
 * <li> Basic authentication via User, password.
 * </ul>
 * <pre>
 * WebClient c = new WebClient("https://www.google.com");
 * String html = c.doGet();
 * System.out.println(c.getResponseCode() + " " + c.getResponseMessage());
 * c.dumpHeaders();
 * c.close();
 * System.out.println(html);</pre>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li>10/19/2018 Added support for HTTPS client certificates (Two-way SSL).</li>
 * <li>10/28/2017 doPut method name changed from POST to PUT plus new method setVerbosity and log4j support</li>
 * <li>6-8-2017 New method getHeaderValue() plus doGet: Check the error stream if the connection failed but the server sent useful data nonetheless.
 * <li>1-7-2017 New method setUrl()
 * <li>Initial implementation.
 * </ul>
 * 
 * @author Owner
 * @version 1.0.1 - Initial implementation.
 * @version 1.0.2 - 6/8/2017 GET Check the error stream if the connection failed but the server sent useful data nonetheless.
 * @version 1.0.3 - 10/28/2017 doPut changed method name from POST to PUT plus new method seVerbosity and log4j support.
 * @version 1.0.4 - 06/13/2018 Added new method doPost(OutputStream out, String content, String contentType, Map<String, String> requestProperties) for binary file download.
 * @version 1.0.5 - 10/19/2018 Added support for HTTPS client certificates (Two-way SSL).
 * @version 1.0.6 - 02/23/2019 doInputOutputRequest() visibility changed to public plus signature updated to support GET/POST requests.
 */
public class WebClient
{
	private static final Logger log = LogManager.getLogger(WebClient.class);
	
	/** Default User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14 */
	public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14";
	
	/** FORM POST content type (for POST requests) */
	public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	/** SOAP content type (for SOAP requests) */
	public static final String CONTENT_TYPE_SOAP 			= "application/soap+xml; charset=utf-8";
	
	/** Request timeouts */
	protected int readTimeout 		= 5000;	
	protected int connectTimeout 	= 5000;
	
    protected Map<String, List<String>> headers;
	protected URL url;
	
	/** Response HTTP status code */
	protected int status;
    private HttpURLConnection uc;
    
    /** The HTML response name associated with the status code 200 = OK, 500 = SERVER ERROR, etc... THIS IS NOT THE HTML RESPONSE. */
    private String responseMessage;

    /** Headers used for basic authorization */
    private String authHeader;
    
    /** custom user agent */
    private String userAgent;

    protected boolean debug;
    
    protected boolean log2Stdout;
    
    /** verbosity log buffer */
	protected final StringBuffer dbgbuf = new StringBuffer();

	/** For 2-way SSL client certificate */
	protected SSLSocketFactory sslSocketFactory;

    protected void LOGD(String text) {
    	if ( debug ) {
    		dbgbuf.append(String.format("[WWWW] %s%n", text));
    	}
    }

    protected void LOGD(String TAG, String text) {
    	if ( debug ) {
    		dbgbuf.append(String.format("[%s] %s%n", TAG, text));
    	}
    }
    
    /**
     * Flush the log buffer to stdout or something else.
     */
    protected void LOGFLUSH() {
    	if ( log2Stdout) {
    		System.out.print(dbgbuf.toString());
    	}
    	if ( dbgbuf.length() > 0 ) {
    		log.debug("<pre>" + dbgbuf.toString() + "</pre>");
    	}
    	dbgbuf.delete(0, dbgbuf.length());
    }
    
    /**
     * Default constructor.
     */
	public WebClient() {
	}

    /**
     * Construct a Web Client.
     * @param url Target URL.
     * @throws IOException on I/O Errors.
     */
	public WebClient(String url) throws IOException {
		this.url = new URL(encodeQueryString(url));
	}
	
	/**
	 * Construct a Web Client.
	 * @param url Target {@link URL}.
	 * @throws MalformedURLException
	 */
	public WebClient(URL url) throws MalformedURLException {
		this.url = url;
	}
	
	public synchronized String getBaseUrl() {
		String[] tmp 		= url.toString().split("\\?");
		return tmp[0];
	}

	public String getQueryString() {
		return url.getQuery();
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public synchronized void setReadTimeout(int millis) {
		readTimeout = millis;
	}

	public synchronized void setConnectTimeout(int millis) {
		connectTimeout = millis;
	}
	
	public synchronized void setUserAgent(String name) {
		userAgent = name;
	}
	
	/**
	 * Set a URL.
	 * @param url URL string.
	 * @throws MalformedURLException In the url is invalid.
	 * @throws UnsupportedEncodingException 
	 * @since 1.0.1
	 */
	public synchronized void setUrl(String url) throws MalformedURLException, UnsupportedEncodingException {
		this.url = new URL( encodeQueryString(url));
	}
	
	/**
	 * Set basic authorization to the the HTTP request. Will add the request header: Basic USER:PWD
	 * @param user User name.
	 * @param password User password.
	 */
	public synchronized void setAuthorization(String user, String password) {
		// FindBugs 11/29/16 Found reliance on default encoding in com.cloud.core.io.WebClient.setAuthorization(String, String): String.getBytes()
		authHeader = "Basic " + Base64.encode((user + ":" + password).getBytes(CoreTypes.CHARSET_UTF8));
	}

    /**
     * Simple HTTP Get request. It used the default user agent Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14
     * @param os stream where the output will be written.
     * @throws MalformedURLException
     * @throws IOException If there is an HTTP error.
     */
	public int doGet (OutputStream os ) throws  IOException {
		return doGet(os, null);
	}
	
    /**
     * Simple HTTP Get request using the default user agent Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14
     * @param os stream where the output will be written.
     * @param requestProperties HTTP headers to add to the request.
     * @throws MalformedURLException
     * @throws IOException If there is an HTTP error.
     */
    public synchronized int doGet (OutputStream os, Map<String, String> requestProperties) throws  IOException 
	{
    	BufferedOutputStream out = new BufferedOutputStream(os);
    	
    	try {
    		// Note: if running in multiple threads this will throw java.lang.IllegalStateException: Already connected
        	// if the connection is not closed first --> if ( uc == null) { uc = (HttpURLConnection)url.openConnection(); ...
    		uc = (HttpURLConnection)url.openConnection();
    		
    		// 2-way SSL client certificate.
    		if ( sslSocketFactory != null) {
    			((HttpsURLConnection)uc).setSSLSocketFactory(sslSocketFactory);
    		} 
    		if ( userAgent != null) {
    	    	uc.setRequestProperty("User-Agent", userAgent);
    	    }
    	    else {
    	    	uc.setRequestProperty("User-Agent", USER_AGENT);
    	    }
    	    uc.setConnectTimeout(connectTimeout);
    	    uc.setReadTimeout(readTimeout);
    	    uc.setDoInput(true);
    	    uc.setDoOutput(false);
    	    uc.setRequestMethod("GET");
    	    
    	    // Add authorization: Basic only!
    	    if ( authHeader != null ) {
    	    	uc.addRequestProperty("Authorization", authHeader);
    	    }

    	    // Add request properties (headers)
    	    if ( requestProperties != null) {
    	    	Set<Entry<String, String>> entries = requestProperties.entrySet();
    	    	
    	    	for (Entry<String, String> entry : entries) {
    	    		LOGD(uc.getRequestMethod(), " [ADD-HDR] " + String.format("%s = %s", entry.getKey().toString(), requestProperties.get(entry.getKey()) ));
    	    		uc.addRequestProperty(entry.getKey().toString(), requestProperties.get(entry.getKey()));
    			}
    	    }
       	    dumpRequest(uc.getRequestMethod());
 
    	    InputStream in  = new BufferedInputStream(uc.getInputStream());   
    	    IOTools.pipeStream(in, out);
    	    
    	    in.close();
    	    headers = uc.getHeaderFields();
    	    
		} 
    	catch (IOException e) {
			responseMessage = e.getMessage();
    		/**
    		 * 6/8/2017 Check the error stream if the connection failed but the server sent useful data nonetheless. 
    		 * The typical example is when an HTTP server responds with a 404, which will cause a FileNotFoundException to be thrown in connect, 
    		 * but the server sent an HTML help page with suggestions as to what to do. 
    		 */
    		InputStream err = uc.getErrorStream();

    		if ( err != null) {
    			IOTools.pipeStream(err, out);
    		}
    		else {
    			throw new IOException(statusCodeAsString(uc.getResponseCode()) + ". " + e.getMessage());
    		}
			// 6/8/2017 throw new IOException(statusCodeAsString(uc.getResponseCode()) + ". " + e.getMessage());
		}
		finally {
			headers 		= uc.getHeaderFields();
			status 			= uc.getResponseCode();
    	    responseMessage = uc.getResponseMessage();
			
			out.close();
			uc.disconnect();
			dumpResponse("GET");
		    LOGFLUSH();
		}
    	return status;
	}

    /**
     * Simple HTTP GET request.
     * <b>This is a inefficient way of doing a GET.</b> Use int rc = doGet(OutputStream html) instead.
     * @return HTTP response text.
     * @throws MalformedURLException
     * @throws IOException
     */
    public String doGet () 
		throws MalformedURLException, IOException
	{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	    try {
	    	doGet(new BufferedOutputStream(baos));
	    }
	    finally  {
	    	IOTools.closeStream(baos);
	    }
	    return baos.toString(CoreTypes.CHARSET_UTF8.name());
	}

    /**
     * Simple HTTP GET request.
     * <b>This is a inefficient way of doing a GET.</b> Use int rc = doGet(OutputStream html) instead.
     * @param requestProperties request headers.
     * @return HTTP response text.
     */
    public String doGet (Map<String, String> requestProperties) 
		throws MalformedURLException, IOException
	{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	    try {
	    	doGet(new BufferedOutputStream(baos), requestProperties);
	    }
	    finally  {
	    	IOTools.closeStream(baos);
	    }
	    return baos.toString(CoreTypes.CHARSET_UTF8.name());
	}

    /**
     * Do an HTTP Post. The default content type is: application/x-www-form-urlencoded.
     * @param content x-www-form-urlencoded sequence of parameters encoded as key1=value1&key2=value2&....
     * @return Server response as a String.
     * @throws MalformedURLException
     * @throws IOException
     */
    public String doPost(final String content)  throws MalformedURLException, IOException 
    {
    	return doPost(content, null, null);
    }

    /**
     * Do an HTTP Post.
     * @param content Sequence of parameters encoded as key1=value1&key2=value2&....
     * @param contentType Post content type, default is application/x-www-form-urlencoded
     * @param requestProperties Adds a general request property specified by a key-value pair (a.k.a request headers). 
     * This method will not overwrite existing values associated with the same key.
     * @return Server response as a String.
     * @throws MalformedURLException
     * @throws IOException
     */
    public String doPost(final String content, final String contentType, Map<String, String> requestProperties)  
    		throws MalformedURLException, IOException 
    {
    	return doInputOutputRequest("POST", content.getBytes(CoreTypes.CHARSET_UTF8), contentType, requestProperties);
    }

    /**
     * HTTP Post. Helpful to upload text files via POST.
     * @param out Output stream used to receive binary or character data.
     * @param content Request payload. For example: a sequence of parameters encoded as key1=value1&key2=value2&....
     * @param contentType Post content type, default is application/x-www-form-urlencoded
     * @param requestProperties Adds a general request property specified by a key-value pair (a.k.a request headers). 
     * This method will not overwrite existing values associated with the same key.
     * @throws MalformedURLException on bad URLs.
     * @throws IOException on HTTP/network  errors.
     * @since 1.0.4
     */
    public void doPost(OutputStream out, String content, String contentType, Map<String, String> requestProperties)  
    		throws MalformedURLException, IOException 
    {
    	doInputOutputRequest(out, "POST", content.getBytes(CoreTypes.CHARSET_UTF8), contentType, requestProperties, true, true, false, false);
    }

    /**
     * HTTP Post. Helpful to upload binary files via POST.
     * @param payload Request payload. For example: a binary file {@link FileInputStream}.
     * @param contentType Post content type, default is application/x-www-form-urlencoded
     * @param requestProperties Adds a general request property specified by a key-value pair (a.k.a request headers). 
     * This method will not overwrite existing values associated with the same key.
     * @throws MalformedURLException on bad URLs.
     * @throws IOException on HTTP/network  errors.
     * @since 1.0.5
     */
    public String doPost(InputStream payload, String contentType, Map<String, String> requestProperties)  
    		throws MalformedURLException, IOException 
    {
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	IOTools.pipeStream(payload, bos);
    	bos.close();
    	return doInputOutputRequest("POST", bos.toByteArray(), contentType, requestProperties);
    }
    
    /**
     * Do an HTTP PUT.
     * @param content Sequence of parameters encoded as key1=value1&key2=value2&.... OR content payload (json, xml or whatever).
     * @param contentType Content type header, default is application/x-www-form-urlencoded
     * @param requestProperties Adds a general request property specified by a key-value pair (a.k.a request headers). 
     * @return Server response as a String.
     * @throws MalformedURLException on bad URLs.
     * @throws IOException on HTTP request errors
     */
    public String doPut(final String content, final String contentType, Map<String, String> requestProperties)  
    		throws MalformedURLException, IOException 
    {
    	return doInputOutputRequest("PUT" /* 10/28/2017 POST*/, content.getBytes(CoreTypes.CHARSET_UTF8), contentType, requestProperties);
    }
    
    public String doDelete(final String content, final String contentType, Map<String, String> requestProperties)  
    		throws MalformedURLException, IOException 
    {
    	return doInputOutputRequest("DELETE", content.getBytes(CoreTypes.CHARSET_UTF8), contentType, requestProperties);
    }
    
    /**
     * Do a request/response with input /output required for operations such as: POST, PUT, etc.
     * @param method The HTTP method: POST, PUT, etc.
     * @param content Sequence of parameters encoded as key1=value1&key2=value2&....
     * @param contentType Post content type, default is application/x-www-form-urlencoded
     * @param requestProperties Adds a general request property specified by a key-value pair (a.k.a request headers). 
     * @return Server response as a String.
     * @throws MalformedURLException
     * @throws IOException
     */
    public /*protected*/ synchronized String doInputOutputRequest(String method, /*String*/ byte[] content, String contentType, Map<String, String> requestProperties)  
    		throws MalformedURLException, IOException 
    {
	    ByteArrayOutputStream baos 	= new ByteArrayOutputStream(); 
	    OutputStream out 			= new BufferedOutputStream(baos);

	    doInputOutputRequest(out, method, content, contentType, requestProperties, true, true, false, false);
	    return baos.toString(CoreTypes.CHARSET_UTF8.name());
    }

    /**
     * Do a request/response with input /output required for operations such as: POST, PUT, etc.
     * @param out Output stream used to receive binary or character data.
     * @param method The HTTP method: POST, PUT, etc.
     * @param content Sequence of parameters encoded as key1=value1&key2=value2&....
     * @param contentType Post content type, default is application/x-www-form-urlencoded
     * @param requestProperties Adds a general request property specified by a key-value pair (a.k.a request headers). 
     * @param doInput A URL connection can be used for input and/or output. Set the DoInput flag to true if you intend to use the URL connection for input (GET), false if not. The default is true.
     * @param doOutput A URL connection can be used for input and/or output. Set the DoOutput flag to true if you intend to use the URL connection for output (POST/PUT), false if not. The default is false.
     * @param followRedirects If true, automatically follow 3xx HTTP redirect responses.
     * @param useCaches Some protocols do caching of documents. Occasionally, it is important to be able to "tunnel through" and ignore the caches (e.g., the "reload" button in a browser). If the UseCaches flag on a connection is true, the connection is allowed to use whatever caches it can. If false, caches are to be ignored. The default value comes from DefaultUseCaches, which defaults to true.
     * @return Server response as a String.
     * @throws MalformedURLException on bad URLs.
     * @throws IOException on HTTP errors.
     */
    //protected synchronized void doInputOutputRequest(OutputStream out, String method, String content, String contentType, Map<String, String> requestProperties)  
    public /*protected*/ synchronized void doInputOutputRequest(OutputStream out, String method, byte[] content, String contentType, Map<String, String> requestProperties
    		, boolean doInput, boolean doOutput, boolean followRedirects, boolean useCaches)
    		throws MalformedURLException, IOException 
    {
		// Note: if running in multiple threads this will throw java.lang.IllegalStateException: Already connected
    	// if the connection is not closed first
    	//if ( uc == null) {
    	uc = (HttpURLConnection)url.openConnection();
    	//}
   		// 2-way SSL client certificate.
		if ( sslSocketFactory != null) {
			((HttpsURLConnection)uc).setSSLSocketFactory(sslSocketFactory);
		} 
    	
	    uc.setDoInput(doInput); // true);
	    uc.setDoOutput(doOutput); // true);
	    uc.setInstanceFollowRedirects(followRedirects); 
	    uc.setRequestMethod(method);
	    uc.setConnectTimeout(connectTimeout);
	    uc.setReadTimeout(readTimeout);
	    uc.setUseCaches (useCaches); //false);
	    
	    // Add authorization: Basic only!
	    if ( authHeader != null ) {
	    	uc.addRequestProperty("Authorization", authHeader);
	    }

	    if ( contentType != null) {
	    	uc.setRequestProperty("Content-Type", contentType);
	    }
	    else {
	    	// Default content-type 
	    	uc.setRequestProperty("Content-Type", CONTENT_TYPE_FORM_URLENCODED); 
	    }
	    dumpRequest(method);
	    
	    // Add request properties (headers)
	    if ( requestProperties != null) {
	    	Set<Entry<String, String>> entries = requestProperties.entrySet();
	    	
	    	for (Entry<String, String> entry : entries) {
	    		LOGD(method, " [ADD-HDR] " + String.format("%s = %s", entry.getKey().toString(), requestProperties.get(entry.getKey()) ));
	    		uc.addRequestProperty(entry.getKey().toString(), requestProperties.get(entry.getKey()));
			}
	    }

	    if ( content != null) {
		    uc.setRequestProperty("Content-Length", String.valueOf(content.length) ); 
			LOGD(method, " [SET-HDR] " + "Content-Length = " + content.length);
	    }
	    
	    //dumpRequestProperties();
	    
	    // Write the request data
	    if ( doOutput) {
		    DataOutputStream wr = new DataOutputStream(uc.getOutputStream ());
		    
		    if ( content != null) {
		    	//wr.writeBytes(content);
		    	wr.write(content);
		    }
		    wr.flush();
		    wr.close(); 
	    }
	    // Read the response
	    try {
		    BufferedInputStream in  = new BufferedInputStream(uc.getInputStream());   
		    IOTools.pipeStream(in, out);
		    in.close();
		} 
    	catch (Exception e) {
    		/**
    		 * Check the error stream if the connection failed but the server sent useful data nonetheless. 
    		 * The typical example is when an HTTP server responds with a 404, which will cause a FileNotFoundException to be thrown in connect, 
    		 * but the server sent an HTML help page with suggestions as to what to do. 
    		 */
    		responseMessage = e.getMessage();
    		InputStream err = uc.getErrorStream();

    		if ( err != null) {
    			IOTools.pipeStream(err, out);
    		}
    		else {
    			throw new IOException(statusCodeAsString(uc.getResponseCode()) + ". " + e.getMessage());
    		}
		}
		finally {
			headers 		= uc.getHeaderFields();
			status 			= uc.getResponseCode();
	   	    responseMessage = uc.getResponseMessage();
			
			out.close();
			uc.disconnect();
			dumpResponse(method);
		    LOGFLUSH();
		}
	    //return baos.toString(CoreTypes.CHARSET_UTF8.name());
    }
    
    private void dumpRequest (String method) {
    	LOGD(" ==== REQUEST " + uc.getRequestMethod() + " " + url);
    	LOGD(" [REQ] ReadTo: " + uc.getReadTimeout() + " ConnTo: " + uc.getConnectTimeout() + " DoIn:" + uc.getDoInput() + " DoOut:" + uc.getDoOutput() + " FolowRedirects:" + uc.getInstanceFollowRedirects());
    	dumpRequestProperties(method);
    }
    
    private void dumpResponse (String method) {
		LOGD(" ==== RESPONSE HTTP Status: " + status);
		LOGD(method, "HTTP Response msg: " + responseMessage);
		
		// Headers
    	Set<Entry<String, List<String>>> entries = headers.entrySet();
    	for (Entry<String, List<String>> entry : entries) { 
    		LOGD(method, " [HDR] " +  entry.getKey() + " = " + headers.get(entry.getKey()).toString());
		}
    }
    
    private void dumpRequestProperties (String method) {
    	if ( uc == null) return;
    	for (Entry<String, List<String>> entry : uc.getRequestProperties().entrySet()) {
    		LOGD(method, String.format(" [HDR] %s = %s", entry.getKey().toString(),  entry.getValue() ));
		}
    }
    
    /**
     * Dumps HTTP RESPONSE headers to stdout.
     * @deprecated To be removed soon. Don't use it.
     */
    public synchronized void dumpHeaders() {
    	Set<Entry<String, List<String>>> entries = headers.entrySet();
    	System.out.println("---- START HEADERS ----");
    	for (Entry<String, List<String>> entry : entries) { 
    		System.out.println(entry.getKey() + "=" + headers.get(entry.getKey()).toString());
		}
    	System.out.println("---- END HEADERS ----");
    }

    /**
     * Get the response code.
     * @return HTTP response code.
     */
    public int getResponseCode() {
    	return status;
    }
    
    public synchronized InputStream getInputStream() throws IOException {
    	if ( this.uc != null) {
    		return uc.getInputStream();
    	}
	    URLConnection uc = url.openConnection();
	    return uc.getInputStream();
    }
    
    /**
     * Returns an unmodifiable Map of the header fields. The Map keys are Strings that represent the response-header field names. 
     * Each Map value is an unmodifiable List of Strings that represents the corresponding field values.
     * @return HTTP response headers.
     */
	public synchronized Map<String, List<String>> getHeaders(){
		return headers;
	}
	
	public synchronized String getHeaderAtIndex (String name, int idx) {
		return headers.containsKey(name) ? headers.get(name).get(idx) : null;
	}

	/**
	 * Get a header key, value pair value. For example: Set-Cookie=[session_id=1bf5d6a5-d828-4d96-8f77-c26233bd990b; path=/api/account/68506600; ...]
	 * @param headerName HTTP header name. For example: Set-Cookie
	 * @param headerKey Name of the key of the inner (key, value) pair. For example: session_id of KVP session_id=1bf5d6a5-d828-4d96-8f77-c26233bd990b
	 * @return The value. For example 1bf5d6a5-d828-4d96-8f77-c26233bd990b within session_id=...
	 * @since 1.0.2
	 */
	public synchronized String getHeaderValue (String headerName, String headerKey) {
    	List<String> list = headers.get(headerName);
    	for (String keyValue : list) { 
    		// session_id=1bf5d6a5-d828-4d96-8f77-c26233bd990b; path=/api/account/68506600; domain=va.agentvep.liveperson.net; httponly
    		if ( keyValue.contains(headerKey)) {
    			if ( keyValue.contains(";")) {
    				// session_id=1bf5d6a5-d828-4d96-8f77-c26233bd990b; path=/api/account/68506600; domain=va.agentvep.liveperson.net; httponly
    				String[] tmp = keyValue.split(";");
    				for (String string : tmp) {
						if ( string.contains(headerKey)) {
							return string.contains("=") ? string.split("=")[1].trim() : string.trim() ;
						}
					}
    			}
    			else {
    				// Note: = may be optional.
    				return keyValue.contains("=") ? keyValue.split("=")[1].trim() : keyValue.trim() ;
    			}
    		}
		}
		return null;
	}

	public int getStatus (){
		return status;
	}
	
	public String getResponseMessage () {
		return responseMessage;
	}
	
	public synchronized String getContentType() {
		return uc.getContentType();
	}
	

	public void close () {
		if ( uc != null) {
			uc.disconnect();
		}
		/* 2/24/2019
		uc 		= null;
		headers = null;
		url 	= null; */
	}

	/**
	 * Translate an HTTP status code to a human readable representation.
	 * @param status HTTP status code. Right now only codes > 400 are checked.
	 * @return Bad Request for 400, ...
	 */
	public static String statusCodeAsString(int status) {
		String str  = null;
		switch (status) {
		case 200:
			str = "OK";
			break;
		case 201:
			str = "Created";
			break;
		case 202:
			str = "Accepted";
			break;

		case 400:
			str = "Bad Request";
			break;
		case 401:
			str = "Unauthorized";
			break;
		case 403:
			str = "Forbidden";
			break;
		case 404:
			str = "Not Found";
			break;
		case 405:
			str = "Method not allowed";
			break;
			
		case 500:
			str = "Internal Server Error";
			break;
		case 501:
			str = "Not Implemented";
			break;
		case 502:
			str = "Bad Gateway";
			break;
		case 503:
			str = "Service Unavailable";
			break;
			
		default:
			str = "Unknown";
		}
		return str + " (" + status + ")";
	}

	/**
	 * Utility for HTML form encoding. This class contains static methods for converting a String to the application/x-www-form-urlencoded MIME format. For more information about HTML form encoding, consult the HTML specification.
	 * When encoding a String, the following rules apply: 
	 * <ul>
	 * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0" through "9" remain the same. 
	 * <li>The special characters ".", "-", "*", and "_" remain the same. 
	 * <li>The space character " " is converted into a plus sign "+". 
	 * <li>All other characters are unsafe and are first converted into one or more bytes using some encoding scheme. Then each byte is represented by the 3-character string "%xy", where xy is the two-digit hexadecimal representation of the byte. The recommended encoding scheme to use is UTF-8. However, for compatibility reasons, if an encoding is not specified, then the default encoding of the platform is used.
	 * </ul>
	 * <p>For example using UTF-8 as the encoding scheme the string "The string ü@foo-bar" would get converted to "The+string+%C3%BC%40foo-bar" because in UTF-8 the character ü is encoded as two bytes C3 (hex) and BC (hex), and the character @ is encoded as one byte 40 (hex).</p>
	 * @param url URL String to be translated.
	 * @return the translated String.
	 * @throws UnsupportedEncodingException - If the named encoding is not supported
	 */
	public static String encodeQueryString (String url ) throws UnsupportedEncodingException {
		// split QS items by &
		String[] a = url.split("&");
		
		for (int i = 0; i < a.length; i++) {
			// for each item split a (KEY,VAL) pairs
			String[] kv = a[i].split("=");
			
			// encode the value only
			if ( kv.length == 2) {
				kv[1] = URLEncoder.encode(kv[1], "UTF-8");
				
				// URLEncoder converts SPACE to + thus conver to %20 
				kv[1] = kv[1].replaceAll("[\\+\\s]", "%20");
			}
			a[i] = IOTools.join(kv, "=");
		}
		return IOTools.join(a, "&");
	}

	/**
	 * @param verbosity Set to true to log information about all HTTP requests.
	 * @since 1.0.3
	 */
	public void setVerbosity(boolean verbosity) {
		this.debug = verbosity;
	}

	/**
	 * @param stdout Set to true to dump HTTP request info to stdout.
	 * @since 1.0.3
	 */
	public void logToStdOut(boolean stdout) {
		this.log2Stdout = stdout;
	}

	/**
	 * Two-way SSL (Client certificate) support. Use it to send a client certificate in the SSL handshake.
	 * 
	 * @param sslProto SSL Context protocol: TLS | TLSv1 | TLSv1.1 | TLSv1.2.
	 * @param keyStorePath Path to the java key store containing the client certificate and private key.
	 * @param storePwd Java Key store password.
	 * 
	 * <p>Note: The key store must contain a CERTIFICATE/PRIVATE KEY pair or the client will not send the certificate in the SSL handshake.</p>
	 * @throws Exception If an error occurs.
	 */
	public synchronized void setSSLParams (String sslProto, String keyStorePath, String storePwd) throws Exception {
		setSSLParams(sslProto, keyStorePath, storePwd, true);
	}

	/**
	 * Two-way SSL (Client certificate) support. Use it to send a client certificate in the SSL handshake.
	 * 
	 * @param sslProto SSL Context protocol: TLS | TLSv1 | TLSv1.1 | TLSv1.2.
	 * @param keyStorePath Path to the java key store containing the client certificate and private key.
	 * @param storePwd Java Key store password.
	 * @param trustAllCerts Set to true if you have a self-signed certificate and wish to avoid <i>ValidatorException: PKIX path building failed...</i>
	 * 
	 * <p>Note: The key store must contain a CERTIFICATE/PRIVATE KEY pair or the client will not send the certificate in the SSL handshake.</p>
	 * @throws Exception If an error occurs.
	 */
	public synchronized void setSSLParams (String sslProto, String keyStorePath, String storePwd, final boolean trustAllCerts) throws Exception {
		String format 			= KeyStore.getDefaultType(); 
		KeyManagerFactory kmf 	= KeyManagerFactory.getInstance("SunX509");
		
		// if null, it will use the JRE default CA keystore
		TrustManager[] trustMgr = null;
		
		// Fix for:  sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path 
		if ( trustAllCerts) {
			trustMgr = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}};
		}
		
		// CLIENT load key store
		KeyStore ksClient 		= KeyStore.getInstance(format); 
		InputStream fis			= null;
		try {
			fis					= new FileInputStream(keyStorePath);
			ksClient.load(fis, storePwd.toCharArray());
		}
		finally {
			IOTools.closeStream(fis);
		}
		// init keys & trust certs to the same KS
		kmf.init(ksClient, storePwd.toCharArray());

		// TLSv1 | TLSv1.1 | TLSv1.2
		SSLContext sc = SSLContext.getInstance(sslProto); 
		sc.init(kmf.getKeyManagers(), trustMgr , null); 
	
		if ( url == null || !url.getProtocol().toLowerCase().equals("https")) {
			throw new IOException("SSL URL required: " + url);
		}
		sslSocketFactory = sc.getSocketFactory();
	}
	
}
