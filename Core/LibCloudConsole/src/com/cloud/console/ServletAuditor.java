package com.cloud.console;

import javax.servlet.http.HttpServletRequest;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;

/**
 * A simple class to invoke {@link Auditor} methods from JSP(s) and/or servlet.
 * This class simply invokes {@link Auditor} with extra information about the {@link HttpServletRequest}.
 * @author VSilva
 *
 */
public class ServletAuditor {

	/** Header names to be displayed by the auditor */
	static final String[] DESIRED_HEADERS = {"cookie", "user-agent"};

	/**
	 * Get {@link HttpServletRequest} information: Remote host, Method, URL + a subset of HTTP headers.
	 * @param request  {@link HttpServletRequest}
	 * @return Format &lt;request>...&lt;/request>
	 */
	public static String getRequestInfo(HttpServletRequest request) {
		return  "<request>\nRemote Host = " + request.getRemoteHost() + "\nURL = " + request.getMethod() + " " + request.getRequestURL() 
				+ "\n" + HTTPServerTools.dumpHeaders( null, request, DESIRED_HEADERS) + "</request>";
	}

	/**
	 * Get full {@link HttpServletRequest} information: Remote host, Method, URL + All HTTP headers.
	 * @param request  {@link HttpServletRequest}
	 * @return Format &lt;request>...&lt;/request>
	 */
	public static String getFullRequestInfo(HttpServletRequest request) {
		return  "<request>\n" + HTTPServerTools.dumpRequestInfo(null, request)
				+ "\n" + HTTPServerTools.dumpHeaders( "HEADERS", request) + "</request>";
	}

	public static void info (AuditSource source, AuditVerb verb, HttpServletRequest request, String text) {
		Auditor.info(source, verb, text + getRequestInfo(request));  
	}

	public static void info (AuditSource source, String verb, HttpServletRequest request, String text) {
		Auditor.info(source, verb, text + getRequestInfo(request));  
	}

	public static void warn (AuditSource source, AuditVerb verb, HttpServletRequest request, String text) {
		Auditor.warn(source, verb, text + getRequestInfo(request)); 
	}

	public static void warn (AuditSource source, String verb, HttpServletRequest request, String text) {
		Auditor.warn(source, verb, text + getRequestInfo(request)); 
	}

	public static void danger (AuditSource source, AuditVerb verb, HttpServletRequest request, String text) {
		Auditor.danger(source, verb, text + getFullRequestInfo(request)); 
	}

	public static void danger (AuditSource source, String verb, HttpServletRequest request, String text) {
		Auditor.danger(source, verb, text + getFullRequestInfo(request)); 
	}

}
