package com.cloud.core.logging;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.json.JSONObject;

import com.cloud.core.logging.L4JAudit;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.w3.OAuth1WebClient;
import com.cloud.core.w3.WebClient;


/**
 * A simple Audit & Notification system using:
 * <ul>
 * <li>Log4j {@link SMTPAppender} to trigger SMTP notifications.
 * <li>Events are buffered by the appender & notifications triggered when an ERROR event occurs.
 * <li>Capable of extending to other types of log systems/notifications.
 * </ul>
 * <pre>
 * Properties config = new Properties();
 *  
 * config.put(KEY_NOTIFY_SMTP_TO, "vsilva@converge-one.com");
 * config.put(KEY_NOTIFY_SMTP_DEBUG, "true");
 *  
 * // SMTP
 * config.put(KEY_NOTIFY_SMTP_HOST,"smtp.acme.com");
 * config.put(KEY_NOTIFY_SMTP_PORT, "25");
 * config.put(KEY_NOTIFY_SMTP_FROM, "L4JAudit@ServiceNode01");
 *  
 * // SMTPS
 * config.put(KEY_NOTIFY_SMTP_HOST,"smtp.mail.yahoo.com");
 * config.put(KEY_NOTIFY_SMTP_PORT, "465");
 * config.put(KEY_NOTIFY_SMTPS_TLS, "true");
 * config.put(KEY_NOTIFY_SMTP_USER, "converge_one@yahoo.com");
 * config.put(KEY_NOTIFY_SMTP_PWD, "Thenewcti1");
 * config.put(KEY_NOTIFY_SMTP_FROM, "converge_one@yahoo.com");
 *  
 * Auditor.update(config); // initialize/de-initialize
 * Auditor.sendTestNotification();
 * Auditor.info(AuditSource.OPERATING_SYSTEM ,"Hello World!");
 * Auditor.warn(AuditSource.SERVICE_VENDOR, "Warning message");
 * Auditor.danger(AuditSource.CLOUD_CONSOLE, "Danger: This will trigger a notification!");
 * </pre>
 * @author VSilva
 * @version 1.0.2
 * @version 1.0.3 - 10/22/2017 Added SMS notifications via Twilio.
 */

public class Auditor {

	/** The source that triggers the Audit event */
	public enum AuditSource {
		CLOUD_CONSOLE		// Cloud console
		, SERVICE_CORE		// Container
		, SERVICE_VENDOR	// Vendor
		, OPERATING_SYSTEM	// OS
		, SECURITY
	}

	/** The action being performed by a particular audit event*/
	public enum AuditVerb {
		SERVICE_LIFECYCLE			// Service Start/stop attempts
		, ACCESS_ATTEMPT			// Login, logoffs
		, CONFIGURATION_CHANGED		// Config changes
		, CLUSTER_LIFECYCLE			// Cluster start, stop, etc
		, AUTHORIZATION_VIOLATION	// Authorization Security violation
		, SECURIY_VIOLATION			// Generic Security violation
		, GENERIC_VERB
	}

	/* Notification Protocol types */
	public static final String KEY_PROTO_DISABLED 		= "none";
	public static final String KEY_PROTO_SMTP 			= "smtp";
	public static final String KEY_PROTO_SMTPS 			= "smtps";
	public static final String KEY_PROTO_TWITTER 		= "twitter";
	public static final String KEY_PROTO_TWILIOSMS 		= "twilioSMS";

	/** Auto recover without notifications */
	public static final String KEY_PROTO_PLAIN 			= "plain";

	/* Log4J SMTP/SMTPS Appender Keys */
	public static final String KEY_NOTIFY_PROTO 		= L4JAudit.KEY_NOTIFY_PROTO;
	public static final String KEY_NOTIFY_SMTP_DEBUG 	= L4JAudit.KEY_NOTIFY_SMTP_DEBUG;
	public static final String KEY_NOTIFY_SMTP_USER 	= L4JAudit.KEY_NOTIFY_SMTP_USER;
	public static final String KEY_NOTIFY_SMTP_PWD 		= L4JAudit.KEY_NOTIFY_SMTP_PWD;
	public static final String KEY_NOTIFY_SMTP_HOST 	= L4JAudit.KEY_NOTIFY_SMTP_HOST;
	public static final String KEY_NOTIFY_SMTP_PORT 	= L4JAudit.KEY_NOTIFY_SMTP_PORT;
	public static final String KEY_NOTIFY_SMTPS_TLS 	= L4JAudit.KEY_NOTIFY_SMTPS_TLS;
	public static final String KEY_NOTIFY_SMTP_FROM 	= L4JAudit.KEY_NOTIFY_SMTP_FROM;
	public static final String KEY_NOTIFY_SMTP_TO 		= L4JAudit.KEY_NOTIFY_SMTP_TO;

	/*
	 * https://dev.twitter.com/oauth/overview/creating-signatures
	 */
	/** Twitter OAuth1 Consumer key. */
	public static final String KEY_NOTIFY_TWIT_CK 		= "server_notification_twitter_consumerKey";
	/** Twitter OAuth1 Consumer Secret. */
	public static final String KEY_NOTIFY_TWIT_CS 		= "server_notification_twitter_consumerSecret";
	/** Twitter OAuth1 Consumer token. */
	public static final String KEY_NOTIFY_TWIT_TK 		= "server_notification_twitter_token";
	/** Twitter OAuth1 Consumer token secret. */
	public static final String KEY_NOTIFY_TWIT_TS 		= "server_notification_twitter_tokenSecret";
	
	/*
	 * TWilio SMS - https://www.twilio.com/console/sms/dashboard
	 */
	/** Twilio SMS application ID - https://www.twilio.com/console/sms/dashboard */
	public static final String KEY_NOTIFY_TWISMS_APPID 	= "server_notification_twilioSMS_appId";
	/** Twilio SMS application token */
	public static final String KEY_NOTIFY_TWISMS_TOKEN 	= "server_notification_twilioSMS_token";
	/** Twilio Phone number (required to send) - https://www.twilio.com/console/sms/dashboard */
	public static final String KEY_NOTIFY_TWISMS_FROM 	= "server_notification_twilioSMS_from";
	/** Destination Phone number */
	public static final String KEY_NOTIFY_TWISMS_TO 	= "server_notification_twilioSMS_to";
	
	/**
	 * Initialize or update the Audit system. Note: This sub may get called multiple times.
	 * @param config Initialization arguments.
	 * @throws IOException
	 */
	public static void update (Properties config) throws IOException { 
		// KEY_CTX_PATH=/CloudContactCenterNode01
		String ctxPath = config.getProperty(NodeConfiguration.KEY_CTX_PATH);
		
		if ( ctxPath == null ) {
			throw new IOException("Auditor: A context path root is required for the audit node logger.");
		}
		// Console Audit logger: Note multiple calls to this thing will mess the logger file.
		L4JAudit.initializeConsoleAuditNodeLogger(ctxPath, config);
		
		// Audit notification logger
		L4JAudit.update(config);
	}

	/**
	 * Stop/remove the notifications subsystem.
	 */
	public static void stopNotifications() {
		L4JAudit.clearNotificationAppender();
	}
	
	/**
	 * Clear all audit sub systems. Invoke on container shutdown.
	 */
	public static void destroy() {
		L4JAudit.destroy();
	}
	
	public static void info (AuditSource src, AuditVerb verb, String message) {
		L4JAudit.info(src + ": (" + verb + ") " + message);
	}

	public static void info (AuditSource src, String verb, String message) {
		L4JAudit.info(src + ": (" + verb + ") " + message);
	}

	public static void warn (AuditSource src, AuditVerb verb, String message) {
		L4JAudit.warn(src + ": (" + verb + ") " + message);
	}

	public static void warn (AuditSource src, String verb, String message) {
		L4JAudit.warn(src + ": (" + verb + ") " + message);
	}

	public static void danger(AuditSource src, AuditVerb verb, String message) {
		L4JAudit.error(src + ": (" + verb + ") " + message);
	}

	public static void danger(AuditSource src, String verb, String message) {
		L4JAudit.error(src + ": (" + verb + ") " + message);
	}

	public static void error(AuditSource src, AuditVerb verb, String text) {
		L4JAudit.error(src + ": (" + verb + ") " + text);
	}
	
	public static void error(AuditSource src, AuditVerb verb, Exception e) {
		L4JAudit.error(src + ": (" + verb + ") " + e.getMessage(), e);
	}

	public static void error(AuditSource src, String verb, Exception e) {
		L4JAudit.error(src + ": (" + verb + ") " + e.getMessage(), e);
	}

	public static void error(AuditSource src, AuditVerb verb, String message, Exception e) {
		L4JAudit.error(src + ": (" + verb + ") " + message, e);
	}

	private static String getNotificationProtocol () {
		NodeConfiguration cfg = CloudServices.getNodeConfig();
		return cfg.getProperty(KEY_NOTIFY_PROTO);
	}

	public static void sendTestNotification() throws Exception {
		sendNotification("This is a test message from the Audit System sent on " + new Date());
	}

	/**
	 * Send a notification to the inner audit system.
	 * @param text Message text.
	 * @throws Exception if an error occurred.
	 * @since 1.0.1
	 */
	public static void sendNotification(String text) throws Exception {
		String proto = getNotificationProtocol();
		if ( proto == null ) throw new IOException("Cannot find a notification protocol in node configuration");
		if ( proto.equals(KEY_PROTO_TWITTER)) {
			twitterSend(text);
			return;
		}
		if ( proto.equals(KEY_PROTO_TWILIOSMS)) {
			twilioSendSMS(text);
			return;
		}
		// SMTP(s) - Note: error events trigger smtp notifications
		L4JAudit.sendNotification(text);
	}
	
	/**
	 * See https://dev.twitter.com/rest/reference/post/statuses/update
	 * <ul>
	 * <li>Authorize - https://dev.twitter.com/oauth/overview/authorizing-requests
	 * <li>Create a Signature - https://dev.twitter.com/oauth/overview/creating-signatures
	 * </ul>
	 * @param text Text to send
	 * @throws IOException on send error.
	 * @since 1.0.1
	 */
	private static void twitterSend (String text) throws IOException  {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		String consumerKey 		= cfg.getProperty(KEY_NOTIFY_TWIT_CK);
		String consumerSecret 	= cfg.getProperty(KEY_NOTIFY_TWIT_CS);
		String token			= cfg.getProperty(KEY_NOTIFY_TWIT_TK);
		String tokenSecret		= cfg.getProperty(KEY_NOTIFY_TWIT_TS);
		twitterSend(text, consumerKey, consumerSecret, token, tokenSecret);
	}	

	/**
	 * See https://dev.twitter.com/rest/reference/post/statuses/update
	 * <ul>
	 * <li>Authorize - https://dev.twitter.com/oauth/overview/authorizing-requests
	 * <li>Create a Signature - https://dev.twitter.com/oauth/overview/creating-signatures
	 * </ul>
	 * @param text Text to send.
	 * @param consumerKey Consumer key from Twitter apps - https://apps.twitter.com/.
	 * @param consumerSecret Consumer secret
	 * @param token Token key.
	 * @param tokenSecret Token secret.
	 * @throws IOException on send error.
	 * @see Twittr Apps - https://apps.twitter.com/
	 * @since 1.0.2
	 */
	public  static void twitterSend (String text, String consumerKey, String consumerSecret, String token, String tokenSecret) throws IOException  {
		String url 				= "https://api.twitter.com/1.1/statuses/update.json?status=" + text;
		try {
			OAuth1WebClient wc 	= new OAuth1WebClient(url, consumerKey, consumerSecret, token, tokenSecret);
			//wc.setDebug(true);
			String resp 		= wc.doPost("");
			
			if ( wc.getStatus() != 200) {
				throw new IOException(resp);
			}
		} catch (Exception e) {
			throw new IOException("Twitter send failure: " + e.toString());
		}
	}

	/**
	 * Send an SMS via the Twilio REST API. See https://www.twilio.com/console/sms/dashboard
	 * <pre>
	 * curl 'https://api.twilio.com/2010-04-01/Accounts/ACe3e0ab836dd69b13a6519ac01503ae04/Messages.json' -X POST \
	 * --data-urlencode 'To=+19192399755' \
	 * --data-urlencode 'From=+19196268226' \
	 * --data-urlencode 'Body=hello' \
	 * -u ACe3e0ab836dd69b13a6519ac01503ae04:9045d280328314bdd00c858617c42b68 </pre>
	 * 
	 * @param text SMS text to send.
	 * @param appId Twilio SMS application ID.
	 * @param token application token.
	 * @param toNumber Destination number : +1NNN1231234
	 * @param fromNumber Twilio sender number: +1nnn1231234
	 * @throws IOException on send errors.
	 * @since 1.0.3
	 */
	public  static void twilioSendSMS (String text, String appId, String token, String toNumber , String fromNumber) throws IOException  {
		String url 						= "https://api.twilio.com/2010-04-01/Accounts/" + appId + "/Messages.json";
		try {
			final String payload 		= String.format("To=%s&From=%s&Body=%s", toNumber, fromNumber, text);
			final String contentType 	= "application/x-www-form-urlencoded";
			WebClient wc 				= new WebClient(url);
			
			wc.setAuthorization(appId, token);
			
			// SUCCESS - {"sid": "SMeb729911e26b41e6837dec197a67b8db", "date_created": "Sat, 21 Oct 2017 23:59:12 +0000", "date_updated": "Sat, 21 Oct 2017 23:59:12 +0000", "date_sent": null, "account_sid": "ACe3e0ab836dd69b13a6519ac01503ae04", "to": "+19192399755", "from": "+19196268226", "messaging_service_sid": null, "body": "Sent from your Twilio trial account - hello world 1508630350852", "status": "queued", "num_segments": "1", "num_media": "0", "direction": "outbound-api", "api_version": "2010-04-01", "price": null, "price_unit": "USD", "error_code": null, "error_message": null, "uri": "/2010-04-01/Accounts/ACe3e0ab836dd69b13a6519ac01503ae04/Messages/SMeb729911e26b41e6837dec197a67b8db.json", "subresource_uris": {"media": "/2010-04-01/Accounts/ACe3e0ab836dd69b13a6519ac01503ae04/Messages/SMeb729911e26b41e6837dec197a67b8db/Media.json"}}
			// ERROR {"code": 21212, "message": "The 'From' number  191962682261 is not a valid phone number, shortcode, or alphanumeric sender ID.", "more_info": "https://www.twilio.com/docs/errors/21212", "status": 400}
			String resp 		= wc.doPost(payload, contentType, null);

			// OK: HTTP status:201 Resp code:201 resp msg:CREATED
			if ( wc.getStatus() >= 300) {
				throw new IOException(resp);
			}
			// Check JSON
			JSONObject root = new JSONObject(resp);
			
			// Assume OK if "date_created": "Sat, 21 Oct 2017 23:59:12 +0000" , "status": "queued",
			if ( !root.has("date_created")) {
				// Assume error: "status": 400, "message": "....", "more_info": "https://www.twilio.com/docs/errors/21212
				throw new IOException("Status: " + root.optInt("status") + " " + root.optString("message") + " " + root.optString("more_info"));
			}
		} catch (Exception e) {
			throw new IOException("Twilio SMS send failure: " + e.getMessage());
		}
	}

	/**
	 * Send an SMS via Twilio using values from the {@link NodeConfiguration}.
	 * @param text SMS text.
	 * @throws IOException on send errors.
	 */
	private static void twilioSendSMS (String text) throws IOException  {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		String appId 			= cfg.getProperty(KEY_NOTIFY_TWISMS_APPID);
		String token 			= cfg.getProperty(KEY_NOTIFY_TWISMS_TOKEN);
		String from				= cfg.getProperty(KEY_NOTIFY_TWISMS_FROM);
		String to				= cfg.getProperty(KEY_NOTIFY_TWISMS_TO);
		twilioSendSMS(text, appId, token, to, from);
	}	

	/**
	 * Get the message buffer size.
	 * @return The # of messages in the buffer.
	 * @since 1.0.0
	 */
	public static int getBufferSize() {
		return L4JAudit.getBufferSize();
	}
	
	/**
	 * Simple sub to remove XML from a text message.
	 * @param text Any text message with XML in it.
	 * @return Message minus XML.
	 * @since 1.0.0
	 */
	public static String cleanXML(String text) {
		// Muli line XML
		return text.replaceAll("(?s)<.*?>.*</.*?>", "");
	}
}
