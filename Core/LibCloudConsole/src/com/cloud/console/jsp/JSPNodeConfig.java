package com.cloud.console.jsp;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.CloudFailOverService.FailOverType;

/**
 * This class contains helper functions used by the JSP Node configuration (config_node.jsp).
 * It is meant to simplify the JSP and make it easier to understand.
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class JSPNodeConfig {

	public static void LOGD(String text) {
		System.out.println("[CFG-NODE-DBG] " + text);
	}

	public static void LOGW(String text) {
		System.err.println("[CFG-NODE-WRN] " + text);
	}

	public static void LOGE(String text) {
		System.err.println("[CFG-NODE-ERR] " + text);
	}

	public static void LOGE(String text, Throwable tr) {
		System.err.println("[CFG-NODE-ERR] " + text);
		tr.printStackTrace();
	}

	/**
	 * Format a integer as a 2 digit hour {dd}
	 * @param value An interger 0-24.
	 * @return Two digit representation. 1 -> 01.
	 */
	public static String formatAs2DigitHour(int value) {
		return String.format("%02d", value);
	}

	/**
	 * Set a property value to from the HttpRequest into the corresponding Hashmap. Only keys that begin w/ prefix are consumed.
	 * @param request The {@link HttpServletRequest}.
	 * @param prefix Indicates the target hashmap : gui. go to the GUI HM, server. (Server) or backend. (Backend) 
	 * @param removePrefix If true remove the prefix value from name (key).
	 * @param name The Key of the property.
	 * @param value Property value.
	 * @param properties The update destination {@link ServiceConfiguration}.
	 */
	/* 9/4/17 removed from node cfg
	static void wrapperSetProperty(HttpServletRequest request, String prefix,  boolean removePrefix, String name, String value, ServiceConfiguration properties) {
		if ( properties == null) {
			LOGE("Invalid config wrapper for prefix " + prefix + " " + name + ":" + value);
			return;
		}
		if ( name.startsWith(prefix)) {
			// remove the prefix?
			String key = name;
			
			if ( removePrefix) {
				key = name.replaceFirst(prefix, "");
			}
			
			// the real val is formated as: label|real value
			properties.setProperty(key, value.trim()); // realValue);
		}
	} */

	/**
	 * Update a property in the {@link NodeConfiguration}.
	 * @param request The {@link HttpServletRequest}.
	 * @param alwaysSet If true. Ignore prefix set values always.
	 * @param prefix Destination name: server for the {@link NodeConfiguration} or CALL_CENTER, MESSAGE_BROGER, DAEMON for the specific {@link ServiceConfiguration}.
	 * @param removePrefix if true remove the prefix from the name (key).
	 * @param name Property key.
	 * @param value Property value.
	 * @param properties Destination {@link NodeConfiguration} which is really a set of {@link Properties}.
	 */
	static void serverSetProperty(HttpServletRequest request, boolean alwaysSet, String prefix,  boolean removePrefix, String name, String value, Properties properties) {
		//for (String prefix : prefixes) {
			if ( name.startsWith(prefix) || alwaysSet) {
				// remove the prefix?
				String key = name;
				
				if ( removePrefix) {
					key 	= name.replaceFirst(prefix, "");
				}
				
				// set value
				if ( value != null && !value.equalsIgnoreCase("null") /* 1/10/2019 Empty is fine && !value.isEmpty() */) {
					// OLD=" + properties.getProperty(key) +  " NEW=" + value);
					if ( !value.equals(properties.getProperty(key))) {
						LOGD ("     Item changed (" + key + ") " + properties.getProperty(key) + " => " + value);
						Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.CONFIGURATION_CHANGED,  "Node configuration item changed (" + key + ") " + properties.getProperty(key) + " => " + value); 
					}
					properties.setProperty(key, value.trim());
				}
				else {
					LOGE("Unable to set " + name +  ". Value is NULL!"); // or EMPTY!"); 
				}
			}
			else {
				LOGE("Unable to set " + name +  " = " + value + ". Unmatched prefix " + prefix);
			} 
		//}
	}


	/**
	 * Get a default profile name. From the HTTP rq or Server cfg.
	 */
	public static String getDefaultProfile(HttpServletRequest request, NodeConfiguration config) {
		String name1		= request.getParameter("n");			// profile name from http rq
		String name2 		= config.getConnectionProfileName();	// profile name from cfg

		if ( name1 != null)		return name1;
		if ( !name2.isEmpty())	return name2;
		return null;
	}

	/**
	 * Update the server and/or backend configurations from the incoming HTTP request
	 * @param request The {@link HttpServletRequest}.
	 * @param cfgServer Destination {@link NodeConfiguration}
	 * @param alwaysSet If true. Ignore prefix set values always.
	 * @param acceptPrefixes Array of key prefixes to accept for saving.
	 * @throws Exception - if an error occurs.
	 */
	public static void updateProperties(HttpServletRequest request, NodeConfiguration cfgServer , boolean alwaysSet ) // , String[] acceptPrefixes )
		throws Exception
	{
		/**
		 * Save properties. Server props begin with server_ 
		 * Message broker props begin with chat_
		 * Contact center with om_
		 */
		Enumeration<String> names = request.getParameterNames();
		
		// Audit props
		Properties auditProps 	= new Properties();
		boolean auditTest 		= false;
		
		while ( names.hasMoreElements()) {
			String name 		= names.nextElement().toString();
			String value		= request.getParameter(name);
			String[] vals		= request.getParameterValues(name); // never null!
			boolean multiVal 	= vals.length > 1;
			
			if ( multiVal) {
				value = IOTools.join(vals, ",");
			}
			LOGD(" Set Param: " + name + "=" + value);
			
			/* set property values from the HTTP request (based on prefix) 9/4/17 - removed from node cfg
			if ( cfgChatBackend != null)
				wrapperSetProperty(request, ServiceConfiguration.CONFIG_KEY_SERVICE_CHAT , true, name, value, cfgChatBackend);
			if ( cfgOpenMedia != null)
				wrapperSetProperty(request, ServiceConfiguration.CONFIG_KEY_SERVICE_OM, true, name, value, cfgOpenMedia);
			*/
			serverSetProperty(request, alwaysSet, /*acceptPrefixes*/  "server_", false, name,  value, cfgServer);
			
			// grab audit sys props
			if ( (name.startsWith("server_audit") || name.startsWith("server_notification"))  &&  !value.isEmpty() ) {
				auditProps.put(name, value);
			}
			
			// audit test?
			if ( name.equals("test_audit") && value.equals("true")) {
				auditTest = true;
			} 
			
			/* 9/4/17 - removed from node cfg. update the config locations from the server config
			if ( name.equalsIgnoreCase("server_configPath")) { 
				if ( cfgChatBackend != null && cfgOpenMedia != null ) {
					cfgChatBackend.setLocation(value);
					cfgOpenMedia.setLocation(value);
				}
				else {
					LOGE("Backend Configs are NULL! Unable to update backend locations to " + name);
				}
			} */
		}
		// debug?
		auditProps.put(Auditor.KEY_NOTIFY_SMTP_DEBUG, cfgServer.getProperty(Auditor.KEY_NOTIFY_SMTP_DEBUG, "false"));
		
		// add useful info such as ctx root, node url, cluster grp
		auditProps.putAll(cfgServer.getClusterParams());
		
		// Init audit system? 1/15/2020 didsabled in favor of the alert system
		updateAuditSystem(request, auditProps, auditTest);
		
		// must store audit changes/additions into config!
		cfgServer.putAll(auditProps);
	}

	static void updateAuditSystem( HttpServletRequest request, Properties auditProps, boolean test) throws Exception {
		LOGD("Update Audit System with props: " + auditProps);

		String proto = auditProps.getProperty(Auditor.KEY_NOTIFY_PROTO);
		
		if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_PROTO) 
				|| proto.equals(Auditor.KEY_PROTO_DISABLED)) 
		{
			if ( test ) throw new Exception("The audit system must be configured."); 
			LOGD("Audit system appears disabled.");
			Auditor.stopNotifications(); // destroy();
			return;
		}
		
		// Validate SMTP(s)
		if ( proto.contains(Auditor.KEY_PROTO_SMTP)) {
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_SMTP_HOST)) throw new Exception("Audit: Missing SMTP(s) host name.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_SMTP_PORT)) throw new Exception("Audit: Missing SMTP(s) port.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_SMTP_TO)) 	throw new Exception("Audit: Missing recipient.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_SMTP_FROM)) throw new Exception("Audit: Missing sender (from).");
			
			// ssl?
			String tls = auditProps.getProperty(Auditor.KEY_NOTIFY_PROTO).equals(Auditor.KEY_PROTO_SMTPS) ? "true" : "false";
			auditProps.put(Auditor.KEY_NOTIFY_SMTPS_TLS, tls);
			
			// add sender (from). If tls use the user name else contextPath@hostname
			if ( tls.equals("true")) {
				if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_SMTP_USER)) throw new Exception("Audit: Missing STMPS user.");
				if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_SMTP_PWD)) throw new Exception("Audit: Missing STMPS password.");
			}
			
			else {
				//auditProps.put(Auditor.KEY_NOTIFY_SMTP_FROM, request.getContextPath().substring(1) + "@" + IOTools.getHostname());
				// Reset the user/pwd to avoid javax.mail.AuthenticationFailedException: 535 5.7.3 Authentication unsuccessful
				auditProps.put(Auditor.KEY_NOTIFY_SMTP_USER, "");
				auditProps.put(Auditor.KEY_NOTIFY_SMTP_PWD, "");
			} 
		}
		if ( proto.equals(Auditor.KEY_PROTO_TWITTER)) {
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWIT_CK)) throw new Exception("Twitter: Missing consumer key.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWIT_CS)) throw new Exception("Twitter: Missing consumer secret.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWIT_TK)) throw new Exception("Twitter: Missing token key.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWIT_TS)) throw new Exception("Twitter: Missing token secret.");
		}
		if ( proto.equals(Auditor.KEY_PROTO_TWILIOSMS)) {
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWISMS_APPID)) throw new Exception("Twilio: Missing application id.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWISMS_TOKEN)) throw new Exception("Twilio: Missing application token.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWISMS_FROM)) throw new Exception("Twilio: Missing Twilio From number.");
			if ( !auditProps.containsKey(Auditor.KEY_NOTIFY_TWISMS_TO)) throw new Exception("Twilio: Missing destination number.");
		}
		
		Auditor.update(auditProps);
		
		if ( test ) {
			LOGD("Sending an audit test notification.");
			Auditor.sendTestNotification();
		}
	}

	/**
	 * Validate the node configuration save request.
	 * @param request HTTp request.
	 * @throws Exception - if validation fails.
	 */
	public static void validate(HttpServletRequest request)	throws Exception {
		//FindBugs: Absolute path traversal in com.cloud.console.jsp.JSPNodeConfig.validate(HttpServletRequest)
		final String logFolder = request.getParameter(NodeConfiguration.KEY_LOG_PATH);
		
		if ( logFolder == null ) {
			return;
		}
		// log folder must exist 
		// FindBugs: Absolute path traversal in com.cloud.console.jsp.JSPNodeConfig.validate(HttpServletRequest) - CHEAT: replace("", "")
		if ( !FileTool.fileExists(logFolder.replace("", ""))) {
			throw new Exception("Log folder " + logFolder + " doesn't exist.");
		}
	}

	/**
	 * Check if the log sub system has changed. Check the log path and rotation policy.
	 * @param request
	 * @param config
	 * @return True if changed.
	 */
	public static boolean logSubSystemChanged(HttpServletRequest request, NodeConfiguration config) {
    	final String rqlogFolder	= request.getParameter(NodeConfiguration.KEY_LOG_PATH);
    	final String rqlogRotPol	= request.getParameter(NodeConfiguration.KEY_LOG_ROTATION_POL);
    	final String rqlogMask		= request.getParameter(NodeConfiguration.KEY_LOG_MASK);
    	final String rqlogMaskRE	= request.getParameter(NodeConfiguration.KEY_LOG_MASK_REGEXP);

    	final String logFolder		= config.getLogFolder();
    	final String logRotPol		= config.getLogRotationPolicy();
    	final String logMask		= config.getProperty(NodeConfiguration.KEY_LOG_MASK, "");		// optional
    	final String logMaskRE		= config.getProperty(NodeConfiguration.KEY_LOG_MASK_REGEXP, "");
    	
    	boolean changed 			= !logFolder.equals(rqlogFolder) || !logRotPol.equals(rqlogRotPol)
    			|| !logMask.equals(rqlogMask) || !logMaskRE.equals(rqlogMaskRE);
    	
		LOGD(String.format("Log sub system changed? %s RQ[%s,%s,%s,%s] CFG[%s,%s,%s,%s]"
				, changed, rqlogFolder ,rqlogRotPol, rqlogMask, rqlogMaskRE, logFolder, logRotPol, logMask, logMaskRE ));
		return changed;
	}
	
	/**
	 * Check if the cluster mode has changed.
	 * @param request HTTP request.
	 * @param config Node configuration.
	 * @return True if the HTTP request cluster mode is different from the {@link NodeConfiguration}.
	 */
	public static boolean clusterSubSystemChanged(HttpServletRequest request, NodeConfiguration config) {
    	final String rqType		= request.getParameter(NodeConfiguration.KEY_FAILOVER_TYPE);
    	final String cfType		= config.getFailOverType().name();
    	// 1/17/2020 java.lang.NullPointerException at com.cloud.console.jsp.JSPNodeConfig.clusterSubSystemChanged(JSPNodeConfig.java:321)
    	if ( rqType == null) {
    		return false;
    	}
    	boolean changed 		= !rqType.equals(cfType);
    	
    	LOGD("Cluster mode changed? HTTP Request " + rqType + " Node CONFIG " + cfType);
    	return changed;
	}

	/**
	 * Check if the cluster is disabled within an HTTP request from the console.
	 * @param request HTTP console request
	 * @return True if the HTTP parameter server_failOverType is SERVLET which indicates no cluster.
	 */
	public static boolean clusterSubSystemDisabled(HttpServletRequest request) {
		final String rqType		= request.getParameter(NodeConfiguration.KEY_FAILOVER_TYPE);
		// 10/3/2019 FindBugs: This method calls equals(Object) on two references of different class types and analysis suggests they will be to objects of different classes at runtime.
		return rqType.equals(FailOverType.SERVLET.name());
	}

	/**
	 * Use this to cleanup unwanted properties.
	 * @param config {@link NodeConfiguration}
	 * @param prefixes Prefixes to remove.
	 */
	private static final Object obj = new Object();	// Used for java.util.ConcurrentModificationException
	
	public static void cleanupProps (NodeConfiguration config, String[] prefixes) {
		/** Fix for
		 * java.util.ConcurrentModificationException
	at java.util.Hashtable$Enumerator.remove(Hashtable.java:1388)
	at com.cloud.console.jsp.JSPNodeConfig.cleanupProps(JSPNodeConfig.java:354)
	at org.apache.jsp.jsp.config.config_005fnode_jsp._jspService(config_005fnode_jsp.java:208)
		 */
		synchronized (obj) {
			Iterator<Object> iterate = config.keySet().iterator();
			while  (iterate.hasNext()) {
				Object key = iterate.next();
				for ( String prefix : prefixes) {
					if ( key.toString().startsWith(prefix)) {
						LOGD("Removing config " + key + " = " + config.getProperty(key.toString()));
						iterate.remove();
					}
				}
			}
		}
	}
}
