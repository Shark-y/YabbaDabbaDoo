package com.cloud.console;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.performance.JMXTomcatMetrics;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.OSMetrics;
import com.cloud.core.profiler.VMAttach;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.types.CoreTypes;


/**
 * Utilities for dumping {@link HttpServletRequest} information.
 * 
 * <ul>
 * <li> Headers
 * <li> Parameters
 * </ul>
 * 
 * @author Administrator
 * @version 1.0.1
 *
 */
public class HTTPServerTools {
	static final Logger log = LogManager.getLogger(HTTPServerTools.class);
	
	/** Content type: Text Plain - UTF-8 */
	public static final String CONTENT_TYPE_TEXT_PLAIN 		= "text/plain; charset=utf-8";

	/** Content type: Text/XML- UTF-8 */
	public static final String CONTENT_TYPE_TEXT_XML 		= "text/xml; charset=utf-8";

	/** Content type: JSON - UTF-8 */
	public static final String CONTENT_TYPE_JSON 			= "application/json; charset=utf-8";

	/** Content type: ZIP  */
	public static final String CONTENT_TYPE_ZIP 			= "application/zip";

	/** Content type: octet-stream */
	public static final String CONTENT_TYPE_OCTET 			= "application/octet-stream";
	
	/** FORM POST content type (for POST requests) */
	public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	/** SOAP content type (for SOAP requests) UTF-8 */
	public static final String CONTENT_TYPE_SOAP 			= "application/soap+xml; charset=utf-8";
	
	/** UTF-8 encoding */
	public static final String 	ENCODING_UTF8				= "UTF-8";

	/**
	 * Dump HTTP request headers into the default string buffer.
	 * <pre>
	 * -- LABEL --
	 * HDR1 = VAL1
	 * -- END LABEL --</pre>
	 * @param label Optional display label.
	 * @param request {@link HttpServletRequest}.
	 */
	public static String dumpHeaders(String label, HttpServletRequest request) {
		Enumeration<String> names 	=  request.getHeaderNames();
		StringBuffer buf 			= new StringBuffer();
		
		if ( label != null) {
			buf.append("-- START " + label + " --\n");
		}
		while ( names.hasMoreElements()) {
			String name = names.nextElement();
			String val = request.getHeader(name);
			buf.append(name + " = " + val +  "\n");
		}
		if ( label != null) {
			buf.append("-- END " + label + "--");
		}
		else {
			buf.deleteCharAt(buf.length() - 1);	// chop \n
		}
		return buf.toString();
	}

	/**
	 * Dump HTTP request headers into a string buffer.
	 * <pre>
	 * -- LABEL --
	 * HDR1 -> VAL1
	 * -- END LABEL --</pre>
	 * @param label Optional display label.
	 * @param desired String array of desired header names (to be displayed). Others will be ignored.
	 * @param request {@link HttpServletRequest}.
	 */
	public static String dumpHeaders(String label, HttpServletRequest request, String[] desired) {
		Enumeration<String> names 	=  request.getHeaderNames();
		StringBuffer buf 			= new StringBuffer();
		
		if ( label != null) {
			buf.append("-- START " + label + " --\n");
		}
		while ( names.hasMoreElements()) {
			String name = names.nextElement();
			String val = request.getHeader(name);
			
			for ( String str : desired) {
				if ( str.equalsIgnoreCase(name)) {
					buf.append(name + " = " + val +  "\n");
					break;
				}
			}
		}
		if ( label != null) {
			buf.append("-- END " + label + "--");
		}
		else {
			buf.deleteCharAt(buf.length() - 1);	// chop \n
		}
		return buf.toString();
	}
	
	/**
	 * Dump a GET HTTP request parameters into the default log system {@link LogManager}.
	 * <b>This should be used for GET requests only.</b> (Use getRequestBody for POST requests).
	 * @param label Display label.
	 * @param request HTTP GET {@link HttpServletRequest}
	 * @return <pre>
	 * -- LABEL --
	 * HDR1 -> VAL1
	 * -- END LABEL --</pre>
	 */
	public static String dumpParameters(String label, HttpServletRequest request) {
		Enumeration<String> names 		= request.getParameterNames();
		Map<String, String[]> params 	= request.getParameterMap();
		StringBuffer buf 				= new StringBuffer();
		
		buf.append("\n-- START [" + label + "] --\n");
		
		while ( names.hasMoreElements()) {
			String name 	= names.nextElement();
			String[] vals 	= params.get(name);
			
			buf.append("\t" + name + " -> ");
			
			for (int i = 0; i < vals.length; i++) {
				buf.append(vals[i] + " ");
			}
			buf.append("\n");
		}
		buf.append("-- END [" + label + "] --");
		return buf.toString();
	}

	/**
	 * Dump request information.
	 * @param label
	 * @param request
	 * @return Request info as a string buffer.
	 */
	public static String dumpRequestInfo(String label, HttpServletRequest request) {
		StringBuffer buf 				= new StringBuffer();
		
		if (label != null) {
			buf.append("\n--  [" + label + "] --\n");
		}
		buf.append(request.getMethod() + " " + request.getContextPath() + "\n");
		
		if ( request.getQueryString() != null ) {
			buf.append("Query String : " + request.getQueryString() + "\n");
		}
		if ( request.getContentType() != null) {
			buf.append("Content Type : " + request.getContentType() + "\n");
		}
		buf.append("Remote Host  : " + request.getRemoteHost() + "\n");
		if (label != null) {
			buf.append("-- [" + label + "] --");
		}
		return buf.toString();
	}
	
	/**
	 * Get the body of an HTTP (POST) request.
	 * @param request {@link HttpServletRequest}.
	 * @return Body as UTF-8 string.
	 * @throws IOException
	 */
	public static String getRequestBody(HttpServletRequest request) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			IOTools.pipeStream(request.getInputStream(), out);
			return out.toString(IOTools.DEFAULT_ENCODING);
		}
		finally {
			IOTools.closeStream(out);
		}
	}
	
	/**
	 * Get the  HTTP request headers into Map<String, String>.
	 * <pre>
	 * -- LABEL --
	 * HDR1 = VAL1
	 * -- END LABEL --</pre>
	 * @param request {@link HttpServletRequest}.
	 * @return Map<String, String> of header (key, value) pairs.
	 */
	public static Map<String, String> getRequestHeaders(HttpServletRequest request) {
		Enumeration<String> names 	=  request.getHeaderNames();
		Map<String, String> hdrs	= new HashMap<String, String>();
		
		while ( names.hasMoreElements()) {
			String name = names.nextElement();
			String val 	= request.getHeader(name);
			hdrs.put(name, val);
		}
		return hdrs;
	}
	
	/**
	 * Get the remote client IP address from the HTTP request.
	 * @param request
	 * @return client IP from the {@link HttpServletRequest}.
	 */
	public static String getRemoteIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }  
        if (ip == null || ip.length() == 0 ) {  
			try {
	        	// Referer format: http://HOST:PORT/CONTECT_RROT/...
				String referer = request.getHeader("Referer");
				if ( referer != null) {
					URI uri = new URI(referer);
		            ip 		= uri.getHost();
				}
				else {
					log.warn("getRemoteIpAddress NO referer header found int HTTP request!");
				}
			} catch (URISyntaxException e) {
			}
        }  
        return ip; 		
	}
	
	/**
	 * Convert a URL query string k1=v1&k2=v2 to a {@link HashMap}
	 * @param qs URL query string k1=v1&k2=v2 
	 * @return {@link HashMap} of (K,V) pairs. <b>If the qs argument is NULL returns an empty map</b>.
	 */
	public static Map<String, String> queryStringToMap (String qs) {
		Map<String, String> map = new HashMap<String, String>();
		
		if ( qs == null) {
			log.warn("Query string to map. NULL string received.");
			return map;
		}
		String[] tmp 			= qs.split("&");
		
		for (String str : tmp) {
			String[] kvp 	= str.split("=");
			
			/* 10/20/2019 java.lang.ArrayIndexOutOfBoundsException: 1
				at com.cloud.console.HTTPServerTools.queryStringToMap(HTTPServerTools.java:290)
			 */
			try {
				String key 		= URLDecoder.decode(kvp[0], CoreTypes.ENCODING_UTF8);
				String value 	= kvp.length == 2 ?  URLDecoder.decode(kvp[1], CoreTypes.ENCODING_UTF8) : "";
				map.put(key,  value);
			} catch (UnsupportedEncodingException e) {
				log.error("queryStringToMap " + Arrays.toString(kvp) + ": " + e.toString());
			}
			
		}
		return map;
	}

	/**
	 * The node version is defined by the Tomcat parallel deployment naming convention: NODENAME##{VERSION}.WAR.
	 * @return The version of the WAR/Node/WebApp as defined by the Tomcat parallel deployment naming convention.
	 * or NULL If there is no version. 
	 */
	public static String getNodeVersion() {
		try {
			// Absolute path C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01##release-1.1-20161024/WEB-INF/classes
			String path = IOTools.getResourceAbsolutePath("/");
			
			if ( !path.contains("##")) {
				return null;
			}
			// Extract everything between ## and /
			int idx = path.indexOf("##");
			return path.substring(idx + 2, path.indexOf("/", idx + 2));
		} 
		catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Inject a response status and message into a {@link JSONObject}. Invoke this to insert a response status & message to
	 * an arbitrary JSON object.
	 * @param root JSON object with extra values : {"status": CODE, "message" : "Some message"}
	 * @param status Usually an HTTP status code.
	 * @param message A response message.
	 */
	public static void injectStatus (JSONObject root, int status, String message) {
		CoreTypes.injectStatus(root, status, message);
	}

	/**
	 * Create a base JSON HTTP response of the form {"status": CODE, "message" : "Some message"}
	 * @param status HTTP status code.
	 * @param message Text message.
	 * @return JSON: {"status": CODE, "message" : "Some message"}
	 */
	public static JSONObject buildBaseResponse(int status, String message) {
		return CoreTypes.buildBaseResponse(status, message);
	}
	
	/** 
	 * Get the OS Metrics As JSON:
	 * <pre> { "status": 200,
	 *  "SystemCpuLoad": "0.04581372190268807",
	 *  "daemonThreadCount": 5,
	 *  "peakThreadCount": 5,
	 *  "threadCount": 5,
	 *  "heapFree": 117411840,
	 *  "heapMax": 117411840,
	 *  "heapTotal": 117411840,
	 *  "ProcessCpuLoad": "0.0026520683563266423",
	 *  }
	 * </pre>
	 * @param req {@link HttpServletRequest}
	 * @param resp {@link HttpServletResponse}
	 */
	public static void getOSMetrics (HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		// MUST SET. response in json format.
		resp.setContentType(CONTENT_TYPE_JSON);
		
		// Tell the client not to cache requests
		resp.addHeader("Cache-Control","no-cache,no-store");

		// 2/4/2020 optional VM pid
		final String vmpId 	= req.getParameter("vmpid") != null ? req.getParameter("vmpid") : "-1";	

		JSONObject root = new JSONObject();

		try {
			
			try {
				JSONObject os = Integer.parseInt(vmpId) <= 0 
						? OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS)
						: VMAttach.getOSMetrics(vmpId).getJSONObject(OSMetrics.KEY_OS) ;
				
				root.put(OSMetrics.KEY_SYS_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_SYS_CPU)));
				root.put(OSMetrics.KEY_PROC_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_PROC_CPU)));
				
				// Add Thread counts: daemonThreadCount, peakThreadCount, threadCount
				root.put(OSMetrics.KEY_DAEMON_THR , os.getInt(OSMetrics.KEY_DAEMON_THR));
				root.put(OSMetrics.KEY_PEAK_THR , os.getInt(OSMetrics.KEY_PEAK_THR));
				root.put(OSMetrics.KEY_THR_COUNT , os.getInt(OSMetrics.KEY_THR_COUNT));
				
				// heap
				root.put(OSMetrics.KEY_HEAP_FREE , os.getInt(OSMetrics.KEY_HEAP_FREE));
				root.put(OSMetrics.KEY_HEAP_MAX , os.getInt(OSMetrics.KEY_HEAP_MAX));
				root.put(OSMetrics.KEY_HEAP_TOTAL , os.getInt(OSMetrics.KEY_HEAP_TOTAL));
			} 
			catch (/*JSON */Exception e) {
				// ignore any JSON errors.
				log.error("OSMetrics", e);
			}
			
			// inject an OK status
			injectStatus(root, 200, "OK"); 

			// 2/18/2017 - Add JMX container stuff
			JSONObject container = JMXTomcatMetrics.getContainerMetrics();
			
			if ( container != null) {
				root.put("container", container);
			}
			
			// 11/5/2017 Add node status info
			// "nodeStatus":{"services":[{"statusCode":"SERVICE_ERROR","name":"Avaya AES","statusDesc":"Service is down."}],"nodeOnline":true}
			JSONObject nodeStatus 							= new JSONObject();
			JSONArray services								= new JSONArray();
			
			// Get the status of all loaded services
			Map<ServiceDescriptor, ServiceStatus> statuses 	= CloudServices.getServiceStatuses();
			
			nodeStatus.put("nodeOnline", CloudServices.isNodeOnline());
			nodeStatus.put("services" , services);
			
			for ( Map.Entry<ServiceDescriptor, ServiceStatus> entry : statuses.entrySet()) {
				ServiceDescriptor sd 	= entry.getKey();
				ServiceStatus st 		= entry.getValue();
				JSONObject service		= new JSONObject();
				
				service.put("name", 		sd.getVendorName());
				service.put("type", 		sd.getType().name());		// 12/21/2018 add type
				service.put("statusCode", 	st.getStatus().name());
				service.put("statusDesc", 	st.getDescription());
				
				services.put(service);
			}
			root.put("nodeStatus", nodeStatus);
			
			// 2/24/2019 - filesystem
			root.put("fileSystems", getFileSytems());
			
			// write response.
			resp.getWriter().print(root.toString());
			
		} catch (JSONException e) {
			injectStatus(root, 500, e.toString());
			resp.getWriter().print(root.toString());
		}
	}

	/**
	 * @return "fileSystems" : [ {"total": x, "free" : 123, "name": "/"},...]
	 */
	static JSONArray getFileSytems () throws JSONException {
		JSONArray array = new JSONArray();
		File[] roots = File.listRoots();
		for (File file : roots) {
			JSONObject jfile = new JSONObject();
			jfile.put("name", file);
			jfile.put("total", file.getTotalSpace());
			jfile.put("free", file.getFreeSpace());
			array.put(jfile);
		}
		return array;
	}

	/**
	 * Convert an exception stack trace to a string buffer.
	 * @param ex An {@link Exception}.
	 * @return Stack trace as string buffer.
	 */
	public static String exceptionToString(Exception ex) {
		StringWriter errors = new StringWriter();
		ex.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}
	
	/**
	 * Get parameter values from an HTTP request parameter map.
	 * @param request parameter map from HTTP request
	 * @param name Desired parameter name.
	 * @param required if true throw {@link IOException} if values is null.
	 * @param label String to be thrown in the {@link IOException}.
	 * @return Values as String[] or new String[] {null} if name doesn't exist.
	 */
	public static String[] getParameters(Map<String, String[]> request, final String name, boolean required, final String label) throws IOException {
		String[] values = request.containsKey(name) ? request.get(name) : new String[] {null};
		if ( required  && values[0] == null) {
			throw new IOException(label);
		}
		return values;
	}
}
