package com.cloud.kubernetes;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.cloud.core.w3.RestClient;

public class K8SParams {

	private static final String W3_PARAM_POD 			= "pod";
	private static final String W3_PARAM_NAMESPACE 		= "namespace";
	private static final String W3_PARAM_LOGARGS 		= "logargs";
	private static final String W3_PARAM_TYPE 			= "type";
	private static final String W3_PARAM_NAME 			= "name";
	private static final String W3_PARAM_DEPLOYMENT 	= "deployment";
	private static final String W3_PARAM_SERVICEACCOUNT = "serviceaccount";

	/**
	 * Replace all occurrences of ${VARIABLE} with {@link Map} (VALUE) within a RAW string.
	 * This method is used as a template mechanism to replace input parameters in a RAW JSON string.
	 * @param params {@link Map} of (VARIABLE, VALUE) pairs.
	 * @param raw A raw string (JSON) containing replacement variables such as { 'key' : ${VARIABLE} }
	 * @return Given Map (VARAIABLE, VALUE) and RAW { 'key' : ${VARIABLE} } returns { 'key' : VALUE }
	 */
	public static String replace (Map<String, Object> params , String raw) {
		String str = raw;
		for ( Map.Entry<String, Object> entry: params.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			if ( val == null ) {
				continue;
			}
			str = str.replaceAll("\\$\\{" + key  +"\\}", val.toString());
		}
		return str;
	}

	/**
	 * Extract HTTP request K8S query string params into a Map for use by JSPs and servlets.
	 * <ul>
	 * <li> Current Params: pod, namespace, logargs.
	 * <li> All possible K8S params are searched, NULL values are removed. KEYS are upper cased.
	 * </ul>
	 * @param request HTTP request.
	 * @return A {@link Map} with the (Key,value) pairs for pod, namespace, logargs, ...
	 */
	public static Map<String, Object> extractParamsFromRequest(HttpServletRequest request) {
		final String pod 				= request.getParameter(W3_PARAM_POD);
		final String namespace 			= request.getParameter(W3_PARAM_NAMESPACE);
		final String type				= request.getParameter(W3_PARAM_TYPE);
		final String name				= request.getParameter(W3_PARAM_NAME);
		final String deployment			= request.getParameter(W3_PARAM_DEPLOYMENT);
		final String serviceaccount		= request.getParameter(W3_PARAM_SERVICEACCOUNT);
		
		// container logs args tailLines=10 (URL ENCODED =(%3D) &(%26) https://www.w3schools.com/tags/ref_urlencode.asp
		final String logargs 			= request.getParameter(W3_PARAM_LOGARGS);
		
		Map<String, String> params 	= new HashMap<String, String>();
		params.put(W3_PARAM_POD, 			pod);
		params.put(W3_PARAM_NAMESPACE, 		namespace);
		params.put(W3_PARAM_LOGARGS, 		logargs);
		params.put(W3_PARAM_TYPE, 			type);
		params.put(W3_PARAM_NAME, 			name);
		params.put(W3_PARAM_DEPLOYMENT, 	deployment);
		params.put(W3_PARAM_SERVICEACCOUNT, serviceaccount);
		
		return extractParamsFromMap(params);
	}

	/**
	 * Extract K8S params from a {@link Map} of (key, value) pairs into another {@link Map}(String, Object) for use by the {@link RestClient}.
	 * This method can be invoked from stand alone classes (non-servlets).
	 * 
	 * @param request A {@link Map} extracted from an HTTP request via extractParamsFromRequest().
	 * 
	 * @return A new {@link Map}(String, Object) to be used by {@link RestClient}. <b>NOTE: The keys in the return map are UPPER-CASED.</b> (Nulls are removed)
	 */
	private static Map<String, Object> extractParamsFromMap( Map<String, String> request) {
		final String pod 				= request.get(W3_PARAM_POD);
		final String namespace 			= request.get(W3_PARAM_NAMESPACE);
		// container logs: tailLines=10
		final String logargs 			= request.get(W3_PARAM_LOGARGS);

		final String type				= request.get(W3_PARAM_TYPE);
		final String name				= request.get(W3_PARAM_NAME);
		final String deployment			= request.get(W3_PARAM_DEPLOYMENT);
		final String serviceaccount		= request.get(W3_PARAM_SERVICEACCOUNT);

		Map<String, Object> params 		= new HashMap<String, Object>();

		if ( pod != null) {
			params.put(W3_PARAM_POD.toUpperCase(), pod);
		}
		if ( namespace != null) {
			params.put(W3_PARAM_NAMESPACE.toUpperCase(), namespace);
		}
		// CONTAINER LOG ARGS: tailLines=10
		if ( logargs != null) {
			params.put(W3_PARAM_LOGARGS.toUpperCase(), logargs);
		}
		// SECRETS
		if ( name != null) {
			params.put(W3_PARAM_NAME.toUpperCase(), name);
		}
		if ( type != null) {
			params.put(W3_PARAM_TYPE.toUpperCase(), type);
		}
		if ( deployment != null) {
			params.put(W3_PARAM_DEPLOYMENT.toUpperCase(), deployment);
		}
		if ( serviceaccount != null) {
			params.put(W3_PARAM_SERVICEACCOUNT.toUpperCase(), serviceaccount);
		}
		return params;
	}

}
