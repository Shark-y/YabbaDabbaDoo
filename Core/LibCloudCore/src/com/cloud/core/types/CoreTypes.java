package com.cloud.core.types;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import org.json.JSONObject;

import com.cloud.core.io.FileTool;
import com.cloud.core.license.License;


/**
 * Global Types used by the Core  classes
 * 
 * @author VSilva
 * 
 * @version 1.0.1 2/2/2019 Moved the Instance ID logic from {@link License} class here.
 * 
 */
public class CoreTypes {

	/** JVM temp dir @ system property: java.io.tmpdir */
	public static final String TMP_DIR 					= System.getProperty("java.io.tmpdir");
	
	/** 5/24/2017 Default line separator system properety: line.separator */
	public static final String LINE_SEPARATOR			= System.getProperty("line.separator");
	
	/** UTF-8 encoding */
	public static final String 	ENCODING_UTF8			= "UTF-8";

	/** Default encoding for stream operations: UTF-8 */
	public static final String 	DEFAULT_ENCODING		= ENCODING_UTF8;

	/** UTF Char set for IO operations */
	public static final Charset CHARSET_UTF8 			= Charset.forName("UTF-8");
	
	/** Random # generator */
	public static final SecureRandom RANDOM				= new SecureRandom();
	
	/** FORM POST content type (for POST requests) */
	public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	/** SOAP content type (for SOAP requests) */
	public static final String CONTENT_TYPE_SOAP 			= "application/soap+xml; charset=utf-8";

	/** JSON content type */
	public static final String CONTENT_TYPE_JSON 			= "application/json; charset=utf-8";

	/** Content type: Text Plain - UTF-8 */
	public static final String CONTENT_TYPE_TEXT_PLAIN 		= "text/plain; charset=utf-8";

	/** Content type: Text/XML- UTF-8 */
	public static final String CONTENT_TYPE_TEXT_XML 		= "text/xml; charset=utf-8";

	/** Content type: ZIP  */
	public static final String CONTENT_TYPE_ZIP 			= "application/zip";

	/** Content type: octet-stream */
	public static final String CONTENT_TYPE_OCTET 			= "application/octet-stream";

	/** Operating system name as returned by the system property os.name */
	public static final String OS_NAME						= System.getProperty("os.name");
	
	/** True if runing windows */
	public static final boolean OS_IS_WINDOWS				= OS_NAME.toLowerCase().contains("windows");
	
	/** Unique MD5 of the classpath root "/" */
	public static final String INSTANCE_ID 					= initRootClassPathMD5();

	/** The webapp/node name*/
	public static final String NODE_NAME					= getNodeName();
	

	/**
	 * Inject a response status and message into a {@link JSONObject}. Invoke this to insert a response status & message to
	 * an arbitrary JSON object.
	 * @param root JSON object with extra values : {"status": CODE, "message" : "Some message"}
	 * @param status Usually an HTTP status code.
	 * @param message A response message.
	 */
	public static void injectStatus (JSONObject root, int status, String message) {
		try {
			if ( root == null) return;
			root.put("status", status);
			root.put("message", message);
		} catch (Exception e) {
		}
	}

	/**
	 * Create a base JSON HTTP response of the form {"status": CODE, "message" : "Some message"}
	 * @param status HTTP status code.
	 * @param message Text message.
	 * @return JSON: {"status": CODE, "message" : "Some message"}
	 */
	public static JSONObject buildBaseResponse(int status, String message) {
		JSONObject root = new JSONObject();
		injectStatus(root, status, message);
		return root;
	}

	/**
	 * Create an MD5 of the absolute class path of resource '/'. This is used to derive the instance ID of a web node.
	 * @return An MD% of class path resource '/' (14E3F2B9D9FF6B63984701DA12A34C9D)
	 */
	private static String initRootClassPathMD5() {
		try {
			// 10/25/2016 Tomcat parallel deployment: chop the version from the FS path
			// PATH C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01##release-1.1-20161024/WEB-INF/classes
			// BECOMES C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01/WEB-INF/classes
			String path 	= getClassPathResourceAbsolutePath(CoreTypes.class, "/");
			String chopped	= path.replaceAll("##.*?/", "/");

			//log.debug("GetInstanceId: Using path " + chopped + " to derive instance id.");
			return MD5(chopped);
		} catch (Exception e) {
			//log.error("Unable to get instance id from server.", e);
			throw new RuntimeException("Unable to get instance id from server." + e);
		}
	}

	/**
	 * Get the absolute path of a resource (folder) in the class path.
	 * @param clazz Java Class used to load the resource.
	 * @param resourceName Resource (folder) name in the class path.
	 * @return The absolute path of the class path resource. Example: WIN32 - C:/CloudServices/Cloud-UnifiedContactCenter
	 * @throws IOException
	 */
	public static String getClassPathResourceAbsolutePath(Class clazz, String resourceName) throws IOException {
    	URL url 	= clazz.getResource(resourceName);
    	
    	if ( url == null )
    		throw new IOException("Invalid resource " + resourceName);
    	
    	String path = URLDecoder.decode(url.getFile(), "UTF-8");
    	
    	// Chop the / from a windows path: /C:/Temp/Workspaces/ => C:/Temp/Workspaces/
    	if ( path.startsWith("/") && OS_IS_WINDOWS) {
    		path = path.replaceFirst("/", "");
    	} 
    	return path;
	}

	/**
	 * MD5 digest tool. It should only be used to generate an instance ID. MD5 hashes are not secure.
	 * @param string String to digest.
	 * @return Hex encoded MD5.
	 */
	public static String MD5(final String string) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(string.getBytes(CoreTypes.CHARSET_UTF8));
			
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
			}
			return sb.toString().toUpperCase();
		} 
		catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	/**
	 * Try to guess the node name by looking at the file system and extracting the last name in the path name.
	 * @return The node name or the instance ID.
	 */
	private static String getNodeName() {
		try {
			// 10/25/2016 Tomcat parallel deployment: chop the version from the FS path
			// PATH C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01##release-1.1-20161024/WEB-INF/classes
			// BECOMES C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01/WEB-INF/classes
			final String path 		= getClassPathResourceAbsolutePath(CoreTypes.class, "/");
			final String chopped	= path.replaceAll("##.*", "").replaceAll("(/build|/WEB-INF).*", "");
			final String name 		= FileTool.getFileName(chopped);
			
			return name.isEmpty() ? INSTANCE_ID : name;
		} catch (Exception e) {
			return INSTANCE_ID;
		}
	}
	
}
