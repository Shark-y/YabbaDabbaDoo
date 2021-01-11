package com.cloud.core.logging;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.net.SMTPAppender;
import org.json.JSONObject;

import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.Container;
import com.cloud.core.logging.L4JAudit;
import com.cloud.core.logging.L4JAuditLayout;
import com.cloud.core.logging.L4JSMTPAppender;
import com.cloud.core.services.NodeConfiguration;

/**
 * A simple Audit & Notification system using Log4J as the audit manager and
 * {@link SMTPAppender} as the notification engine.
 * <ul>
 * <li>It uses the log4j {@link SMTPAppender} to trigger SMTP notifications.
 * <li> *NEW* Log4J RollingFile appender to keep an audit trail that can be viewed from the console.
 * <li>Events are buffered by the appender & notifications triggered when an ERROR occurs.
 * event occurs.
 * <li>TODO: Extend with other types of notifications.
 * </ul>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li> 1/9/2017 Notification protocol key KEY_NOTIFY_PROTO changed to server_audit_proto
 * </ul>
 * @author VSilva
 * @version 1.0.1
 * 
 */
public class L4JAudit {

	/** SMTP Notification logger */
	private static final Logger log 	= Logger.getLogger(L4JAudit.class);

	/** Notification Audit appender name */
	private static final String APPENDER_NAME 		= "AuditAppender";

	// smtp appender keys
	static final String KEY_NOTIFY_PROTO 		= "server_audit_proto"; 	
	static final String KEY_NOTIFY_SMTP_DEBUG 	= "server_audit_mail.debug";
	static final String KEY_NOTIFY_SMTP_USER 	= "server_audit_mail.user";
	static final String KEY_NOTIFY_SMTP_PWD 	= "server_audit_mail.password";
	static final String KEY_NOTIFY_SMTP_HOST 	= "server_audit_mail.host";
	static final String KEY_NOTIFY_SMTP_PORT 	= "server_audit_mail.port";
	static final String KEY_NOTIFY_SMTPS_TLS 	= "server_audit_mail.smtps.starttls.enable";
	static final String KEY_NOTIFY_SMTP_FROM 	= "server_audit_mail.from";
	static final String KEY_NOTIFY_SMTP_TO 		= "server_audit_mail.to";

	// Log4J Appender
	private static AppenderSkeleton appender;

	static void LOGD(String text) {
		System.out.println("[AUDIT-DBG] " + text);
	}

	static void LOGE(String text) {
		System.out.println("[AUDIT-ERR] " + text);
	}

	/**
	 * Initialize the SMTP Notification Audit System. Requires an SMTP account configured in the cloud console.
	 * 
	 * @param config
	 */
	private static void initializeNotificationSystem(Properties config) {
		if (appender != null) {
			return;
		}
		L4JAuditLayout layout = new L4JAuditLayout();
		layout.setOriginURL(config.getProperty(NodeConfiguration.KEY_CTX_URL));
		
		appender = new L4JSMTPAppender();
		appender.setName(APPENDER_NAME);
		appender.setLayout(layout); 
		
		// This will display Audit events in the root (cloud console) log window.
		//Logger.getRootLogger().addAppender(appender);

		// Add the appender to this logger only so events won't show up @ the root level (cloud console)
		log.addAppender(appender);
		
		// Always enabled so log events will be recorded		
		log.setLevel(Level.DEBUG);
		
		// Don't cascade events to the ancestors. See https://logging.apache.org/log4j/1.2/manual.html
		log.setAdditivity(false);
	}

	/**
	 * Remove the notification appender only.
	 */
	public static void clearNotificationAppender () {
		log.removeAppender(APPENDER_NAME);
		if ( appender != null) {
			((L4JSMTPAppender)appender).dispose();
		}
		appender 	= null;
	}
	
	/**
	 * Remove all appenders: Audit notifications & Audit trail.
	 * Invoke this on container shutdown.
	 */
	static void destroy() {
		LOGD("Audit Destroy: Cleaning up all appenders.");
		
		Logger.getRootLogger().removeAppender(APPENDER_NAME);
		clearNotificationAppender();
		
		log.removeAllAppenders();
	}

	static void update(Properties config) throws IOException {
		String proto = config.getProperty(KEY_NOTIFY_PROTO);
		if ( proto == null ) {
			//LOGE("Update L4JAudit. Invalid (NULL) protocol. Abort update.");
			//return;
			throw new IOException("Audit configuration update. A protocol is required.");
		}
		// SMTP(S)
		if ( proto.contains(Auditor.KEY_PROTO_SMTP)) {
			updateSMTP(config);
		}
	}

	/**
	 * Update the audit configuration. May be invoked multiple times.
	 * 
	 * @param config
	 *            service configuration properties:
	 * 
	 *            <pre>
	 * {mail.smtp.port=25, mail.debug=true
	 * , mail.password=
	 * , audit_pattern=%d{yy-MM-dd HH:mm:ss} %-20c{1} [%-5p] %m%n
	 * , mail.to=vsilva@converge-one.com, mail.user=
	 * , mail.smtp.host=smtp.acme.com}
	 * </pre>
	 * @throws Exception
	 */
	static void updateSMTP(Properties config) throws IOException {
		String host = config.getProperty(KEY_NOTIFY_SMTP_HOST);
		String port = config.getProperty(KEY_NOTIFY_SMTP_PORT, "25");
		String user = config.getProperty(KEY_NOTIFY_SMTP_USER);
		String pwd 	= config.getProperty(KEY_NOTIFY_SMTP_PWD);
		String tls 	= config.getProperty(KEY_NOTIFY_SMTPS_TLS);
		String from = config.getProperty(KEY_NOTIFY_SMTP_FROM);
		String to 	= config.getProperty(KEY_NOTIFY_SMTP_TO);
		boolean debug = config.containsKey(KEY_NOTIFY_SMTP_DEBUG) 
				? Boolean.parseBoolean(config.getProperty(KEY_NOTIFY_SMTP_DEBUG))
				: false;
				
		//  Audit ptotocol: none, smtp or smtps
		String proto 	= config.getProperty(KEY_NOTIFY_PROTO);	
		
		boolean enabled = proto != null && !proto.equalsIgnoreCase("none");
		
		LOGD("================================= AUDIT NOTIFICATION SYSTEM =================================");
		LOGD("Audit Proto: " + proto + " ENABLED: " + enabled + " Host: " + host + " Port: " + port);
		LOGD("Audit User : " + user + "/" + pwd + " From: " + from + " Recipient: " + to);
		LOGD("Audit TLS  : " + tls + " Debug: " + debug); 

		if (enabled) {
			// validate (if enabled)
			if ( host == null ) throw new IOException("Missing SMTP host.");
			if ( from == null ) throw new IOException("Missing SMTP Sender (from).");
			if ( to == null ) 	throw new IOException("Missing SMTP Receipient (to).");
			
			initializeNotificationSystem(config);
			L4JSMTPAppender smtp = (L4JSMTPAppender) appender;

			smtp.setSMTPDebug(debug);
			smtp.setSMTPHost(host);
			
			// empty vals throw javax.mail.AuthenticationFailedException: 535 5.7.3 Authentication unsuccessful
			if ( user != null && !user.isEmpty()) {
				smtp.setSMTPUsername(user);
			}
			
			if ( pwd != null && ! pwd.isEmpty()) {
				smtp.setSMTPPassword(pwd);
			}
			
			smtp.setFrom(from);
			smtp.setTo(to);
			smtp.setSubject("Audit Message from " + from);

			if (debug) {
				System.setProperty("mail.debug", "true");
				System.setProperty("mail.debug.auth", "true");
			}
			if (port != null)
				System.setProperty("mail.smtp.port", port);

			if (tls != null && tls.equalsIgnoreCase("true") ) {
				LOGD("Audit: Using TLS/SMTPS");
				
				// set SSL sys props...
				System.setProperty("mail.transport.protocol", "smtps");
				System.setProperty("mail.smtps.starttls.enable", tls);
				System.setProperty("mail.smtps.auth", "true");
				System.setProperty("mail.smtps.host", host);
				System.setProperty("mail.smtps.port", port);
			}
			else {
				LOGD("Audit: Using SMTP (NO TLS)");
				System.setProperty("mail.transport.protocol", "smtp");
				System.setProperty("mail.smtps.starttls.enable", "false");
			}
			smtp.activateOptions();

			// no error stacks
			// LogLog.setQuietMode(true);

		} else {
			clearNotificationAppender(); // destroy();
			LOGD("Audit system disabled.");
		}
		LOGD("=============================================================================================");
	}

	static void info(Object message) {
		log.info(message);
	}

	static void warn(Object message) {
		log.warn(message);
	}

	static void danger(Object message) {
		log.error(message);
	}

	static void error(Object message) {
		log.error(message);
	}

	static void error(Object message, Exception e) {
		log.error(message, e);
	}

	static void sendNotification(String text) throws MessagingException {
		// Note: error events trigger smtp notifications
		if ( appender == null) {
			throw new MessagingException("Audit system has not been initialized.");
		}
		((L4JSMTPAppender) appender).sendTestMessage(text);
	}

	/**
	 * Get the size of the event appender buffer.
	 * @return Log buffer size.
	 */
	static int getBufferSize() {
		// not yet initialized?
		if ( appender == null) {
			return -1;
		}
		return ((L4JSMTPAppender) appender).getBuffer().length();
	}
	
	/**
	 * Initialize a WebApp node logger using values from bootstrap.ini.
	 * A file will be created under ${CWD}/logs/{CONTEXT_ROOT}.log
	 * 
	 * @param config Server config {@link NodeConfiguration}.
	 */
	public static void initializeConsoleAuditNodeLogger(String baseName, Properties config) {
		try {
			final String thresHold		= config.getProperty(NodeConfiguration.KEY_LOG_THRESHOLD); 
	    	final String pattern		= config.getProperty(NodeConfiguration.KEY_LOG_PATTERN); 
	    	final String basePath		= config.getProperty(NodeConfiguration.KEY_LOG_PATH, Container.getDefautContainerLogFolder());
			initializeConsoleAuditNodeLogger(basePath, baseName, thresHold, pattern); 
		} catch (Exception e) {
			LOGE("AuditNodeLogger Initialization Error: " + e.toString());
		}
	}
	
	/**
	 * Initialize a WebApp audit console logger using values from bootstrap.ini.
	 * A file will be created under ${CWD}/logs/{CONTEXT_ROOT}.log
	 * @param basePath Full path to the folder containing the audit log. Should be ${CONTAINER-ROOT}/logs
	 * @param baseName Logger name: /ContextRoot.
	 * @param thresHold Initial appender threshold: DEBUG, INFO, WARN, etc.
	 * @param pattern Log4j log pattern. For example: %d{yy-MM-dd HH:mm:ss} %-22c{1} [%-5p] %m%n
	 */
	public static void initializeConsoleAuditNodeLogger(final String basePath, final String baseName, final String thresHold, final String pattern ) {
		try {
			final String appenderName = "AUDIT-" + (baseName.startsWith("/") ? baseName.substring(1) : baseName);
			
			if ( log.getAppender(appenderName) != null) {
				LOGD("initializeConsoleAuditNodeLogger AUDIT appender " + appenderName + " aleary initialized. (duplicates are forbidden).");
				return;
			}
			// Path relative to the container $HOME/logs/AUDIT-[NODENAME].log
			//final String basePath 			= "logs/" + appenderName + ".log";
			final String rootPath 			= basePath + File.separator + appenderName + ".log";
			
			LOGD("============================== CONSOLE NODE AUDIT LOGGER ====================================");
			LOGD("Audit Node logger path      : " + rootPath);
			LOGD("Audit Node logger Threshold : " + thresHold);
			LOGD("Audit Node logger Pattern   : " + pattern);
			LOGD("Audit Node Appender Name    : " + appenderName);
			
			DailyRollingFileAppender appender 	= new DailyRollingFileAppender(
				new PatternLayout(pattern) 
				, rootPath /*basePath */
				, "'.'yyyy-MM-dd-a");
			
			appender.setAppend(false);
			appender.setThreshold(Level.toLevel(thresHold)); 
			appender.setName(appenderName); // baseName);
			
			//Add the daily appender to root logger
			log.addAppender(appender);
			log.setLevel(Level.DEBUG);
			
			// don't cascade events to ancestors
			log.setAdditivity(false);
			LOGD("=============================================================================================");
			
		} catch (Exception e) {
			e.printStackTrace();
			LOGE("Console AUDIT Initialization failed with " + e.toString());
		}
	}

	public static L4JSMTPAppender getAppender() {
		return (L4JSMTPAppender)appender;
	}
	
	/**
	 * Return the Loj4J buffer as JSON.
	 * @return {@link JSONObject} of the form:
	 * <pre>{
	 *  "data": [
	 *  ["2016-02-04 10:11:56:101", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Checking if a sys admin pwd has been setup.&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:102", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: sys admin pwd has been setup. Checking for service descriptors @ C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:103", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Found descriptor C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default\\avaya-cfg.xml&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:104", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Node has been configured.&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:105", "NodeConfiguration", "WARN", "&lt;div style=\"color:black;background-color:yellow;\">Add URL: localhost is NOT a valid URL host name!&lt;\/div>"],
	 *  ...
	 * ]
	 * } </pre>
	 */
	public static JSONObject toJSON() {
		return ((L4JSMTPAppender)appender).toJSON();
	}
	
}
