package com.cloud.core.w3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import com.cloud.core.io.IOTools;
import com.cloud.core.security.EncryptionTool;
import com.cloud.core.types.CoreTypes;

/**
 * A very simple tool to perform OAuth1 tasks such as:
 * <ul>
 * <li>Generate OAuth1 authorization headers.
 * <li>Sign HTTP requests.
 * <li>Verify signatures.
 * </ul>
 * @author VSilva
 * @version 1.0.0
 * @see https://oauth1.wp-api.org/docs/basics/Signing.html
 */
public class OAuth1 {

	public static class Parameters {
		String consumerKey;
		String token;
		String nonce;
		long timeStamp;

		public Parameters(String consumerKey, String token) {
			this(consumerKey, token, UUID.randomUUID().toString(), System.currentTimeMillis()/1000);
		}
		
		public Parameters(String consumerKey, String token, String nonce, long timeStamp) {
			super();
			this.consumerKey = consumerKey;
			this.token 		= token;
			this.nonce		= nonce;
			this.timeStamp	= timeStamp;
		}
		
		public String nonce() {
			return UUID.randomUUID().toString();
		}
		/**
		 * Encode the OAuth parameters.
		 * @return oauth_nonce="eefd26f5-b171-4c32-8fb5-8cbd50039b69", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="d13eba0e667a41178280bd77d6daa890", oauth_token="c54bf947392f408bb6ef956d3802428c", oauth_timestamp="1447806308"
		 */
		@Override
		public String toString() {
			return String.format("oauth_nonce=\"%s\", oauth_version=\"1.0\", oauth_signature_method=\"HMAC-SHA1\", oauth_consumer_key=\"%s\", oauth_token=\"%s\", oauth_timestamp=\"%d\""
					, nonce, consumerKey, token, timeStamp);
		}
	}

	public static class Secrets {
		String consumerSecret;
		String tokenSecret;
		
		public Secrets(String consumerSecret, String tokenSecret) {
			super();
			this.consumerSecret = consumerSecret;
			this.tokenSecret = tokenSecret;
		}
		
		public String getKey() {
			return consumerSecret + "&" + tokenSecret;
		}
		
		@Override
		public String toString() {
			return String.format("Consumer: %s Token: %s", consumerSecret, tokenSecret);
		}
	}

	static void LOGD(String text) {
		System.out.println("[OAUTH1] " + text);
	}

	static void LOGE(String text) {
		System.err.println("[OAUTH1] " + text);
	}

	/**
	 * Normalize OAuth authorization header.
	 * From: <pre>
	 * oauth_signature="vEAkjFBR1kES5tDiAPzVI8XYug4%3D", oauth_nonce="2ad7fb21-ae71-41fe-ae66-76a466e046e9", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="d13eba0e667a41178280bd77d6daa890", oauth_token="c54bf947392f408bb6ef956d3802428c", oauth_timestamp="1447809152"
	 * To: 
	 * oauth_consumer_key=d13eba0e667a41178280bd77d6daa890&oauth_nonce=2ad7fb21-ae71-41fe-ae66-76a466e046e9&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1447809152&oauth_token=c54bf947392f408bb6ef956d3802428c&oauth_version=1.0
	 * </pre>
	 * <li> Remove oauth_signature
	 * <li> Sort
	 * <li> Concat with &
	 * @param header OAuth authorization header value (without the protocol)
	 * @return Normalized header for signature verification:
	 * oauth_consumer_key={KEY}&oauth_nonce={NONCE}&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1447809152&oauth_token={TOKEN}&oauth_version=1.0
	 */
	public static String normalizeHeader (String header) {
		// must remove oauth_signature="vEAkjFBR1kES5tDiAPzVI8XYug4%3D",
		String hdr = header.replaceFirst("oauth_signature=.*?,","");
		
		String[] vals = hdr.split(",");
		
		// trim & remove "
		for (int i = 0; i < vals.length; i++) {
			vals[i] = vals[i].trim().replaceAll("\"", "");
		}
		
		Arrays.sort(vals);
		return IOTools.join(vals, "&");
	}


    /**
     * Convert an OAuth authorization header <pre>
     *  oauth_signature="IrWre5020eJm9QfJJ8m3TvzlpjY%3D", oauth_nonce="eefd26f5-b171-4c32-8fb5-8cbd50039b69", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="d13eba0e667a41178280bd77d6daa890", oauth_token="c54bf947392f408bb6ef956d3802428c", oauth_timestamp="1447806308"
     *  </pre>
     *  To {@link Properties}
     * @param authorization
     * @return
     * @throws IOException
     */
   public static Properties headerToProperties (String authorization) throws IOException {
		String tmp = authorization.replaceAll(",","\n");
		tmp = tmp.replaceAll("\"", "");
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(tmp.getBytes(CoreTypes.CHARSET_UTF8)));
		return props;
    }

    /**
     * Verify a signature using the OAuth1 header format: authorization: <i>OAuth k1="v1", k2="v2", ... Kn="Vn" </i>
     * <pre>
     * OAuth oauth_signature="IrWre5020eJm9QfJJ8m3TvzlpjY%3D", oauth_nonce="eefd26f5-b171-4c32-8fb5-8cbd50039b69", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="d13eba0e667a41178280bd77d6daa890", oauth_token="c54bf947392f408bb6ef956d3802428c", oauth_timestamp="1447806308"
     * </pre>
     * @param method {@link HttpServletRequest} method: GET, POST, etc.
     * @param url URL to verify (Cannot contain a query string).
     * @param queryString The query string of the URL.
     * @param authorization The Authorization header from the request: OAuth oauth_signature="{SIGNATURE}", oauth_nonce="{NONCE}", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="{KEY}", oauth_token="{TOKEN}", oauth_timestamp="{TIMESTAMP}"
     * @param key The secret used to verify the signature within the authorization header.
     * @throws SecurityException If authorization fails.
     * @throws IOException If there is a parse or I/O error in the verification process.
     */
    public static void verifySignature (String method, String url, String queryString, String authorization, String key) throws SecurityException, IOException {
    	// url cannot contain a query string (bug)
    	if ( url.contains("?")) 		throw new IOException("Invalid URL " + url + ". Query string not allowed.");
    	if ( authorization == null) 	throw new IOException("Authorization header is required.");
    	
    	String proto 		= authorization.substring(0, authorization.indexOf(" "));
    	
    	// get rid of: OAuth + space
    	authorization 		= authorization.substring(proto.length() + 1);
    	Properties params 	= headerToProperties(authorization);

		// Normalize the authorization header + query string (if available): 
    	String qs 			= queryString != null ? "," + queryString.replaceAll("&", ",") : "";
		String normalized 	= normalizeHeader(authorization + qs);
		
		// verification payload: {METHOD}&{URL}&{AUTHORIZATIONHEADET+QUERY_STRING}
		String payload 		= method.toUpperCase() + "&"  + URLEncoder.encode(url, "UTF-8") + "&" + URLEncoder.encode(normalized, "UTF-8");

		String sig1 = EncryptionTool.signWithHmacSHA1(payload, key); //, "UTF-8");
		String sig2 = URLDecoder.decode(params.getProperty("oauth_signature"), "UTF-8");

		StringBuffer buf 	= new StringBuffer();
    	buf.append("Auth Header   : " + authorization + "\n");
    	buf.append("Normalized Hdr: " + normalized + "\n");
    	buf.append("Proto         : " + proto + "\n");
    	buf.append("URL           : " + url + "\n");
    	buf.append("QueryString   : " + queryString+ "\n");
    	buf.append("Payload       : " + payload + "\n");
    	buf.append("Key           : " + key + "\n");
    	buf.append("Signed   Sig1 : " + sig1 + "\n");
    	buf.append("Incoming Sig2 : " + sig2 + "\n");
		
    	LOGD(buf.toString());
    	
		if ( !sig1.equals(sig2)) {
			throw new SecurityException("Authorization failed for " + url);
		}
    }

    /**
     * Sign an HTTP request using the OAuth1 rule:  METHOD + "&"  + URLEncoder.encode(url, "UTF-8") + "&" + URLEncoder.encode(normalized, "UTF-8")
     * @param method {@link HttpServletRequest} method: GET, POST, etc.
     * @param url URL to sign (without a query string).
     * @param queryString The query string of the URL (if available).
     * @param params OAuth1 {@link Parameters}.
     * @param secrets OAuth1 {@link Secrets}.
     * @return The OAuth1 signature.
     * @see https://oauth1.wp-api.org/docs/basics/Signing.html
     */
    public static String sign (String method, String url, String queryString, Parameters params, Secrets secrets) throws IOException {
       	// url cannot contain a query string
    	if ( url.contains("?")) throw new IOException("Invalid URL " + url + ". Query string not allowed.");
     	
    	String parameters 		= params.toString();
    	
		// Normalize the authorization header + query string (if available): 
    	String qs 			= queryString != null ? "," + queryString.replaceAll("&", ",") : "";
		String normalized 	= normalizeHeader(parameters + qs);
    	
		// Signature payload: {METHOD}&{URL}&{AUTHORIZATIONHEADET+QUERY_STRING}
		String payload 			= method.toUpperCase() + "&"  + URLEncoder.encode(url, "UTF-8") + "&" + URLEncoder.encode(normalized, "UTF-8");
		return URLEncoder.encode(EncryptionTool.signWithHmacSHA1(payload, secrets.getKey()) , "UTF-8");
    }
    
    /**
     * Generate the OAuth1 HTTP header to be injected into the HTTP request.
     * @param signature The oauth1 signature.
     * @param params The OAuth1 {@link Parameters}.
     * @return OAuth oauth_signature="{SIGNATURE}", oauth_nonce="{NONCE}", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="{KEY}", oauth_token="{TOKEN}", oauth_timestamp="{TIMESTAMP}"
     */
    public static String generateAuthorizationHeader (String signature, Parameters params ) {
    	return String.format("OAuth oauth_signature=\"%s\", %s", signature, params.toString());
    }

    /**
     * Generate the OAuth1 HTTP header to be injected into the HTTP request.
     * @param method The HTTP method: GET, POST, etc.
     * @param url The HTTP URL without a query string.
     * @param queryString The URL query string (if any).
     * @param params The OAuth1 {@link Parameters}.
     * @param secrets The OAuth1 {@link Secrets}.
     * @return Authorization Header: OAuth oauth_signature="{SIGNATURE}", oauth_nonce="{NONCE}", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="{KEY}", oauth_token="{TOKEN}", oauth_timestamp="{TIMESTAMP}"
     */
    public static String generateAuthorizationHeader (String method, String url, String queryString,  Parameters params, Secrets secrets ) throws IOException {
    	String signature = sign(method, url, queryString, params, secrets);
    	return String.format("OAuth oauth_signature=\"%s\", %s", signature, params.toString());
    }

}
