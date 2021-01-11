package com.cloud.console.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloud.console.JSPLoggerTool;
import com.cloud.console.filter.OWASPConfig.ElementType;
import com.cloud.console.filter.OWASPConfig.PatternDescriptor;
import com.cloud.security.CloudSecurity;

/**
 * Part of OWASP - https://www.owasp.org/index.php/Main_Page
 * WebApp security
 * 
 * @author VSilva
 * @see https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project
 *
 */
public class OWASPValidator {

	/** Max size of an HTTP parameter value */
	private static final int LIMIT_BUFFER_SIZE = 1024;
	
	/**
	 * Fail first check for a suspicious value. 
	 * @param uri The request URI.
	 * @param key The key such as parameter or header name.
	 * @param value The value to check.
	 * @param type The {@link ElementType}: HEADER, PARAMETER, etc.
	 * @throws IOException if a suspicious value is detected indicating a security scan violation.
	 */
	public static void validate(String uri, String key, String value, ElementType type ) throws IOException {
		// Null values not allowed.
		if ( value == null) {
			throw new IOException("Parameter " + key + " value cannot be NULL.");
		}
		
        // Avoid null characters
    	value = value.replaceAll("\0", "");
    	
        // check global patterns
    	validateValueEncoding(key, value);
    	validateGlobals(uri, key, value);
    	validateParams(uri, key, value, type);
	}

	/**
	 * Check for invalid characters in the input.
	 * @param key Parameter name.
	 * @param value Parameter value.
	 * @throws IOException
	 */
	static void validateValueEncoding(String key, String value) throws IOException {
        // NOTE: It's highly recommended to use the ESAPI library and uncomment the following line to
        // avoid encoded attacks.
        // value = ESAPI.encoder().canonicalize(value);
		// 10/11/2017 - Skip check 4 invalid chars if there is a validator for this param (key)
		if (  CloudSecurity.hasInputValidator (key) ) {
			return;
		}
		try {
			canonicalize(value);
		} 
		catch (Exception e) {
			throw new IOException("Invalid characters for parameter " + key + " = " + value);
		}
	}
	
	/**
	 * Any header/param that matches a GlobalRejectPattern will be rejected.
	 * @param uri Request URI.
	 * @param key Header or parameter name.
	 * @param value Header/parameter value.
	 * @throws IOException If key is rejected.
	 */
	static void validateGlobals(String uri, String key, String value) throws IOException {
        // check global patterns
        for (Pattern pattern : com.cloud.console.filter.OWASPConfig.GlobalRejectionPatterns) {
        	Matcher m = pattern.matcher(value);

        	if ( m.find()) {
        		throw new IOException("Potential ATTACK for parameter " + key + " = " + value + " @ " + uri + " Pattern:" + m.pattern().pattern());
        	}
        }
	}

	static void validateParams(String uri, String key, String value, ElementType type ) throws IOException {
		if ( type != ElementType.PARAM) {
			return;
		}

    	// If no param validators available...
    	if ( ! CloudSecurity.hasInputValidator (key) ) {
    		// Enforce the HARD limit for param value size
    		// For Buffer overflow attacks. Size Limit = LIMIT_BUFFER_SIZE 
    		if ( value.length() > LIMIT_BUFFER_SIZE /* && !CloudSecurity.isParamExempt(key,"BUFFER_OVERFLOW") */) {
    			throw new IOException( String.format("Buffer overflow for HTTP param %s of size %d (limit %d bytes)", key , value.length(), LIMIT_BUFFER_SIZE));	
    		}
    		
    		// Check parameter rejection patterns 
            for (PatternDescriptor scriptPattern : OWASPConfig.ParamRejectionPatterns) {
            	/* 1/7/2017 This will punch a hole in the OWASP defeating the whole thing
            	// check for exemptions...
            	boolean exempt 	= OWASPConfig.isParamExempt(key, scriptPattern);
            	
            	if ( exempt) {
            		continue;
           		} */
            	
            	Matcher m 		= scriptPattern.pattern.matcher(value);
            	
            	if ( m.find()) {
            		throw new IOException("Detected " + scriptPattern.types + " ATTACK for input " + key + " = " + value + " @ " + uri + " Pattern:" + scriptPattern);
            	}
            }
    	}
    	
    	// warn if above buffer limit
    	if ( value.length() > LIMIT_BUFFER_SIZE ) {
    		JSPLoggerTool.JSP_LOGW("[SECURITY]", String.format("HTTP param %s of size %d has exceeded the buffer limit of %d", key, value.length(), LIMIT_BUFFER_SIZE));
    	}
        // Validate input via regular expressions 
    	CloudSecurity.validateParamInput (key, value);
	}
	
	/**
	 * A poor man implementation of ESAPI.encoder().canonicalize(value)
	 * This sub will try to decode the input but will fail if there are are invalid characters.
	 * @param value The value to decode.
	 * @return Decoded value.
	 * @throws Exception If there are invalid characters.
	 */
	static String canonicalize (String value) throws Exception {
		try {
			/**
			 * Reject any suspicious stuff like these...
			 * java.lang.IllegalArgumentException: URLDecoder: Illegal hex characters in escape (%) pattern - For input string: "x%"
			 * at java.net.URLDecoder.decode(Unknown Source)
			 * URLDecoder: Illegal hex characters in escape (%) pattern - For input string: "n%" for input ZAP%n%s%n%s%n
			 * Illegal hex characters in escape (%) pattern - For input string: "1!" for input ZAP %1!s%2!s%3!s%
			 */
			return URLDecoder.decode(value, "UTF-8");
		} catch (Exception e) {
			//System.err.println("[SECURITY] " + e + " @ " + value);
			throw e;
		}
	}

}
