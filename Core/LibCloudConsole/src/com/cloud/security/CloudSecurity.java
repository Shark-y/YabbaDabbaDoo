package com.cloud.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.OAuth1;

/**
 * This class handles the security descriptors used to:
 * <ul>
 * <li>Validate HTTP request parameters and headers.
 * <li>Validate request URis.
 * <li>Authorize REST API calls via the {@link OAuth1} protocol.
 * <li>Authenticate JSP resources.
 * </ul>
 * <p>
 * This class works by loading a set of security descriptors within packages of any library as long as they follow the naming convention:
 * </p>
 * <i>/configuration/security-{PKG}.json</i>
 * <p>
 * Where <i>/configuration</i> is a folder within the library and <i>{PKG}</i> is the package name of the class that implements {@link IServiceLifeCycle}.
 * </p>For example, for the connector server (LibConnectorServer-x.jar) the descriptor would be /configuration/security-com.connector.core.json
 * <br>The descriptor format is:
 * <pre>{ "exceptions": [
 *  { "uri": "RawMessage", "method": "post", "contentType": "x-www-form-urlencoded"},
 *  { "param": "CALL_CENTER10_00_mscrmSearchUri", "types": "EXTERNAL_REDIRECT,SQL_INJECT"}
 *  ],
 * "validators": [
 *  { "param": "CALL_CENTER10_02_mscrmLogUri", "regexp": "http://localhost"},
 *  { "param": "server_runMode", "regexp": "\\b(PRIMARY|SECONDARY)\\b"}
 * 	],
 * "authenticate": [
 *  { "uri": "^(?!.*login.jsp).*\\.jsp.*" , "type": "BASIC"}
 * ],
 * "authorize": [
 *  { "uri": "/Amq", "type:": "oauth1" },
 * ]}</pre>
 * 
 * <ul>
 * <li> Exceptions: { "uri": "RawMessage", "method": "post", "contentType": "x-www-form-urlencoded"} means skip security scan violations for URI /RawMessage.
 * <li> Validators: { "param": "server_runMode", "regexp": "\\b(PRIMARY|SECONDARY)\\b"} means HTTP param server_runMode can only accept values: PRIMARY, SECONDARY.
 * <li> Authentication: { "uri": "^(?!.*login.jsp).*\\.jsp.*" , "type": "BASIC"} means authenticate (via user/password) all JSPs except login.jsp.
 * <li> Authorization: { "uri": "/Amq", "type:": "oauth1" } means authorize the REST API call URI /Amq via the {@link OAuth1} protocol.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation.
 * @version 1.0.1 - 8/11/2017 VM Optimizations & better loadDescriptors().
 *
 */
public class CloudSecurity {

	private static final Logger log = LogManager.getLogger(CloudSecurity.class);
	
	/** 
	 * Global Security Scan Descriptor: {"exceptions" : [ ex1, ex2,....], "validators": [ v1, v2,...]} 
	 * Where: 	<li>ex(n) = { "param": "SOMENAME", "types": "EXTERNAL_REDIRECT,SQL_INJECT"}
	 * 			<li>v(n) = {"param": "NAME", "regexp": "SOMEREGEXP"}
	 */
	private static final JSONObject security = new JSONObject();
	
	/**
	 * Load security descriptors from the classpath using valid package names: /configuration/security-{PKG1}.json, /configuration/security-{PKG2}.json...
	 * 
	 * <p>JSON format: { "exceptions" : [ ex1, ex2,....], "validators": [v1, v2, ...], ...}
	 * <p>Where ex(n) = { "param": "PARAM CALL_CENTER10_00_mscrmSearchUri", "types": "EXTERNAL_REDIRECT,SQL_INJECT"}
	 * <p>See The Cloud Services document for details.</p>
	 */
	public static void loadSecurityDescriptors () throws IOException, JSONException {
		// Load by Package
		final Package[] pkgs = Package.getPackages();

		for (int i = 0; i < pkgs.length; i++) {
			// resource name format: /configuration/security-{PKGNAME}.json
			final String resource 			= "/configuration/security-" + pkgs[i].getName() + ".json";
			final InputStream in 			= CloudSecurity.class.getResourceAsStream(resource);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			if ( in == null ) {
				IOTools.closeStream(in);
				IOTools.closeStream(out);
				continue;
			}

			IOTools.pipeStream(in, out);
			JSONObject json 	= null;
			try {
				json  			= new JSONObject(out.toString(CoreTypes.ENCODING_UTF8));
			} catch (JSONException e) {
				//log.error("[SECURITY] Loading " + resource, e);
				//continue;
				throw new IOException("[SECURITY] Loading " + resource, e);
			}
			finally {
				IOTools.closeStream(in);
				IOTools.closeStream(out);
			}
			addDescriptor(resource, json);
		}
	}
	
	/**
	 * A a security descriptor from a given JSON
	 * @param resource File/JAR/Package that contains the descriptor.
	 * @param json Descriptor JSON (see above for the fomat).
	 */
	public static void addDescriptor (final String resource, JSONObject json) throws IOException, JSONException {
		log.debug("[SECURITY] Loading security descriptor: " + resource);

		// Concatenate all arrays/elements into a single descriptor (security) { "exceptions" : [ ex1, ex2,....]}
		// ex(n) = { "param": "PARAM CALL_CENTER10_00_mscrmSearchUri", "types": "EXTERNAL_REDIRECT,SQL_INJECT"}
		final JSONArray names = json.names();
		
		for (int j = 0; j < names.length(); j++) {
			final String key = names.getString(j);
			final Object obj = json.get(key);
			
			// ignore comments
			if ( key.startsWith("META_")) {
				continue;
			}
			
			if ( security.has(key)) {
				// append obj[] elements into security[key]
				JSONArray dest = security.getJSONArray(key);
				
				if ( obj instanceof JSONArray) {
					JSONArray src = (JSONArray)obj;
					for (int k = 0; k < src.length() ; k++) {
						dest.put(src.get(k));
					}
				}
				else {
					dest.put(obj);
				}
			}
			else {
				// set
				security.put(key, obj);
			}
		}
		//JSPLoggerTool.JSP_LOGD("[SECURITY]", "Global Descriptor: " + security);
	}
	
	/**
	 * Search the security JSON descriptor for a value for a particular parameter (name).
	 * @param rootKey Security JSON root key: exceptions, validators
	 * @param subKeyType Name of the parameter inner key within rootKey: param, uri etc.
	 * @param name Name of the parameter to search for.
	 * @param paramKey Attribute key: types, regexp
	 * @return The value for paramKey for parameter name within the root key or null if not found.
	 * @throws JSONException If any JSON syntax error.
	 */
	static String getValueFromJSONArrayForParam (final String rootKey, final String subKeyType, final String name, final String paramKey) throws JSONException {
		final JSONArray array = security.optJSONArray(rootKey);
		
		if ( array != null ) {
			for (int i = 0; i < array.length(); i++) {
				final JSONObject ex 	= array.getJSONObject(i);
				final String param	= ex.optString(subKeyType); 
	
				if ( !param.isEmpty() && param.equals(name)) {
					// found..
					return ex.getString(paramKey);
				}
			}
		}
		return null;
	}

	/**
	 * Given an HTTP param name get the list of security exceptions from the global JSON descriptor (security)
	 * @param name {@link HttpServletRequest} Parameter name.
	 * @return List of pattern (security scan) exceptions: EXTERNAL_REDIRECT,SQL_INJECT,...
	 * @throws JSONException
	 */
	/* exceptions have been removed - punch holes, defeat the OWASP
	public static String getExceptionsForParam (String name) throws JSONException {
		return getValueFromJSONArrayForParam("exceptions", "param", name, "types");
	} */

	/**
	 * Check if an {@link HttpServletRequest} parameter is to be excluded from a {@link PatternDescriptor} of a security scan.
	 * @param name Parameter name.
	 * @param patternTypes The type of pattern to check for an exception: EXTERNAL_REDIRECT,SQL_INJECT, CMD_INJECT, BUFFER_OVERFLOW, etc.
	 * @return True if parameter name is exempt from security scan pattern.
	 */
	/* 1/7/2017 This will punch a hole in the OWASP defeating the whole thing
	public static boolean isParamExempt (String name, String patternTypes) {
		try {
			String types = getExceptionsForParam(name);
			
			// found?
			if ( types != null) {
				// exempt? true if types in pattern.types
				String[] a1 = types.split(",");
				String[] a2 = patternTypes.split(",");
				
				for (int i = 0; i < a1.length; i++) {
					for (int j = 0; j < a2.length; j++) {
						if ( a1[i].equals(a2[j])) {
							return true;
						}
					}
				}
			}
			return false;
		} 
		catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	} */

	/**
	 * Check if uri matches any of the patterns in the "exceptions" section of the security descriptor.
	 * @param uri Test URI.
	 * @param method The HTTP method of the URI: get, post, etc.
	 * @param contentType The content type of the {@link HttpServletRequest}.
	 * @return True if there is a regular expression that matches URI for the given method and content type.
	 */
	public static boolean isUriExempt (final String uri, final String queryString, final String method, final String contentType) {
		try {
			JSONArray array = security.optJSONArray("exceptions");
			
			if ( array != null) {
				for (int i = 0; i < array.length(); i++) {
					final JSONObject ex 	= array.getJSONObject(i);
					final String regexp		= ex.optString("uri"); 	
					final String method1	= ex.optString("method");
					final String ct			= ex.optString("contentType");
					final String matcher	= queryString != null && !queryString.isEmpty() ? uri + "?" + queryString : uri;
					
					final boolean b1 		= !regexp.isEmpty() && Pattern.compile(regexp).matcher(matcher /*uri*/).find();
					final boolean b2		= !method1.isEmpty() &&  method1.equals(method);
					final boolean b3		= contentType.isEmpty() || ( !ct.isEmpty() && /*ct.equals(contentType)*/ contentType.contains(ct) ) ;
					//System.out.println("**  EX-URI: " + matcher + " QS:" + queryString + " re:" + regexp + " b1:" + b1 + " b2:" + b2 + " b3:" + b3);
					
					if (b1 && b2 && b3) {
						return true;
					}
				}
			}
		} 
		catch (JSONException e) {
			log.error("[SECURITY] checkUriExemptions: " + e.toString());
		}
		return false;
	}

	/**
	 * Find a match for value in a key (subKey) within an array (key) of the global security descriptor. 
	 * @param value The value to match.
	 * @param key The array within the security descriptor. For example: "authenticate".
	 * @param subKey The sub key that contains the regular expression to match within the array. For example: "uri".
	 * @return A {@link JSONObject} for key if value matches the regexp in array (key, subKey) else NULL.
	 * @throws JSONException If there is a parse error.
	 */
	public static JSONObject /* boolean*/ getMatchFromSecurityDescriptor (final String value, final String key, final String subKey) throws JSONException {
		JSONArray array = security.optJSONArray(key);
		
		if ( array != null) {
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj 	= array.getJSONObject(i);
				String re		= obj.getString(subKey); 
				boolean b1 		= Pattern.compile(re).matcher(value).find();

				if ( b1 ) {
					return obj; //true;
				}
			}
		}
		return null; //false;
	}
	
	/**
	 * Check the input validator (via regular expressions) from the security descriptor: "validators": [ v1, v2,...]}.
	 * Where v(n) = {"param": "NAME", "regexp": "SOMEREGEXP"}
	 * @param name Name of the {@link HttpServletRequest} parameter to validate.
	 * @param value Parameter value to check.
	 * @throws IOException if the parameter is invalid by matching its value against its regular expression in the security descriptor.
	 */
	public static void validateParamInput (final String name, final String value) throws IOException {
		try {
			final String regexp = CloudSecurity.getValueFromJSONArrayForParam("validators", "param", name, "regexp");
			// no regexp for this param. Assume valid
			if ( regexp == null ) {
				return ; // true;
			}
			// If the regexp in the validator matches, then the input is valid
			final boolean found = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE).matcher(value).find();
			//System.out.println("*** FOUND regexp " + regexp + " param:" + name + " = " + value + " Matches:" + found);
			
			// no match, invalid input
			if ( !found) {
				throw new IOException(String.format("Failed data validation with Regexp (%s) for %s = %s", regexp, name, value));
			}
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check for an input validator for a given parameter.
	 * @param name Parameter name.
	 * @return True if a validator exists for parameter name.
	 */
	public static boolean hasInputValidator (final String name) {
		try {
			final String regexp = CloudSecurity.getValueFromJSONArrayForParam("validators", "param", name, "regexp");
			// no regexp for this param. Assume valid
			return ( regexp != null ) ;
		} 
		catch (JSONException e) {
			return false;
		}
	}
	
	/**
	 * Loop thru the "authenticate" elements of the security descriptor looking for an URI match.
	 * @param uri URI to scan.
	 * @return True if there is a match in the authenticate section for this URI meaning it requires authentication.
	 */
	public static boolean requiresAuthentication (final String uri) {
		try {
			// global(default) "authenticate"
			boolean global 		= CloudSecurity.getMatchFromSecurityDescriptor(uri, "authenticate", "uri") != null;
			boolean override 	= authenticate(uri, global);
			return override 	? override : global && override;
			// 8/15/17 return CloudSecurity.getMatchFromSecurityDescriptor(uri, "authenticate", "uri") != null;
		} 
		catch (JSONException e) {
			log.error("[SECURITY] requiresAuthentication: " + e.toString());
		}
		return false;
	}
	
	/**
	 * Loop thru the "authorize" elements of the security descriptor looking for an URI match.
	 * @param uri URI to scan.
	 * @return True if there is a match in the authorize section for this URI meaning it requires authorization.
	 */
	public static boolean requiresAuthorization (final String uri) {
		try {
			return CloudSecurity.getMatchFromSecurityDescriptor(uri, "authorize", "uri") != null;
		} 
		catch (JSONException e) {
			log.error("[SECURITY] requiresAuthorization: " + e.toString());
		}
		return false;
	}

	/**
	 * Loop thru the "authorize" elements of the security descriptor looking for the authorization type.
	 * @param uri URI to scan.
	 * @return The authorization type from {"type:":"oauth1","uri":"/Amq"}.
	 */
	public static String getAuthorizationType (final String uri) {
		try {
			// {"type:":"oauth1","uri":"/Amq"} or NULL
			JSONObject root = getMatchFromSecurityDescriptor(uri, "authorize", "uri");
			return root != null ? root.getString("type") : "jwt";
		} 
		catch (JSONException e) {
			log.error("[SECURITY] getAuthorizationType: " + e.toString());
		}
		return "jwt";	// default
	}
	
	/**
	 * Check an URI against the security descriptor for a clickjacking prevention JSON security key.
	 * @param uri URI to check.
	 * @return The value of "resources" [ { uri: URI , preventClickJacking: false}...] or true (default) if preventClickJacking is missing.
	 */
	public static boolean preventClickJacking (final String uri) {
		return checkBoolResource(uri, "preventClickJacking", true);
	}

	private static boolean authenticate (final String uri, boolean defaultValue) {
		return checkBoolResource(uri, "authenticate", defaultValue);
	}

	/**
	 * Check for a boolean (key) in the resources section of the security descriptor.
	 * @param uri The URI to check.
	 * @param key The key to search within "resources"
	 * @param defaultValue Default boolean value.
	 * @return The boolean value for resources["key"] that matches URI or defaultValue if no match.
	 */
	private static boolean checkBoolResource (final String uri, final String key, final boolean defaultValue) {
		try {
			JSONObject root = getMatchFromSecurityDescriptor(uri, "resources", "uri");
			if ( root != null) {
				return root.optBoolean(key, defaultValue);
			}
			return defaultValue;
		} 
		catch (JSONException e) {
			log.error("[SECURITY] " + key + ": " + e.toString());
		}
		return defaultValue;
	}

}
