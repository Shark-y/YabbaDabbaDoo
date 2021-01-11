package com.cloud.core.cron;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.cron.ErrorNotificationSystem;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.EncryptionTool;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;

/**
 * Utilities used by the auto update and error notification systems.
 * 
 * <ul>
 * <li> TC org.apache.catalina.filters.CSRF_NONCE - http://www.techpaste.com/2013/11/setup-cross-site-request-forgery-prevention-filter-tomcat/
 * <li> TC manager API - https://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html
 * <li> START: http://host:8080/manager/html/start?path=/CloudConnectorAES01&version=release-1.1-20180525&org.apache.catalina.filters.CSRF_NONCE=ECE3E04943A7780BDEB6C31A9B9B5C0E
 * </ul>
 * 
 * @author vsilva
 * 
 * @version 1.0.0 8/11/2028
 *
 */
public class AutoUpdateUtils {

	private static final Logger log 			= LogManager.getLogger(AutoUpdateUtils.class);
	
	/** Weekly Scan startup threshold (Monday) **/
	private static final int DAY_THRESHOLD 			= Calendar.MONDAY;
	
	/** Daily Scan startup threshold (5AM) **/
	private static final int HOUR_THRESHOLD 		= 5;
	
	/** Monthly Scan startup threshold (1ast day of the month) **/
	private static final int DAY_OF_MONTH_THRESHOLD = 1;
	
	/** Update/notification configuration */
	private static final Properties config = new Properties();
	
	/** Frequency at which errors are checked. */ 
	static public enum Frequency {
		DISABLED, DAILY, WEEKLY, MONTHLY
	}
	
	/** Notification password from notifications.properties */
	public static final String KEY_PASSWORD 		= "notification_password";

	/**
	 * @param key Configuration key.
	 * @return True if key belongs in the update system, else false.
	 */
	public static boolean configIsUpdateKey (final String key) {
		return key.startsWith(ErrorNotificationSystem.CFG_KEY_PREFIX); 
			/* enable with auto update || key.startsWith(AutoUpdate.CFG_KEY_PREFIX); */
	}
	
	/**
	 * Set an update system configuration property.
	 * @param key Key name.
	 * @param value Key Value.
	 */
	public static void configSetProperty (final String key, final String value) {
		config.setProperty(key, value);
	}

	public static String configGetProperty (final String key) {
		return config.getProperty(key);
	}

	public static String configGetProperty (final String key, final String def) {
		return config.getProperty(key, def);
	}
	
	/**
	 * Get the notification system configuration from disk @ $USER_HOME/.cloud/PRODUCT/notification.properties
	 * @return Configuration path @ $USER_HOME/.cloud/PRODUCT/notification.properties
	 * @throws IOException on I/O errors.
	 */
	private static String configGetPath () throws IOException {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		final String base 		= cfg.getConfigLocation();
		if ( base == null )		throw new IOException("Invalid configuration base path.");
		final String path 		= base + File.separator + "notification.properties";
		return path;
	}
	
	/**
	 * Save the Auto Update/Notification properties to $HOME/.cloud/PRODUCT/notification.properties
	 * 
	 * @throws FileNotFoundException If the file can't be found.
	 * @throws IOException on I/O errors.
	 */
	public static void configSave() throws FileNotFoundException, IOException {
		String path 		= configGetPath();
		OutputStream fos	= null;
		try {
			fos 			= new FileOutputStream(path);
			config.store(fos, "AUTOMATED UPDATE SYSTEM");
		}
		finally {
			IOTools.closeStream(fos);
		}
	}

	public static boolean configExists () throws IOException {
		return FileTool.fileExists(configGetPath());
	}
	
	/**
	 * Load the update configuration once from disk. It can be called multiple times.
	 * @param force If true force the load. Will not re-load unless force is true.
	 * @throws FileNotFoundException If the configuration is not on disk.
	 * @throws IOException On I/O errors.
	 */
	public static void configLoad(boolean force) throws FileNotFoundException, IOException {
		if ( config.isEmpty() || force) {
			final String path 	= configGetPath();
			InputStream fis		= null;
			try  {
				fis = new FileInputStream(path);
				config.load(fis);
			}
			finally {
				IOTools.closeStream(fis);
			}
		}
	}
	
	
	/**
	 * Get the notification configuration from the {@link NodeConfiguration} object.
	 * @return JSON format: {"params":{"notification_password":"Thenewcti1","notification_user":"converge_one@yahoo.com","notification_debug":"on","notification_to":"cloud_git@convergeone.com","notification_proto":"smtps","notification_host":"smtp.mail.yahoo.com","notification_port":"465","notification_starttls.enable":"on","notification_from":"converge_one@yahoo.com","notification_vendor":"ACME","notification_folder":"Inbox"}} JSON:{"params":{"notification_proto":"smtps","notification_vendor":"ACME","notification_from":"converge_one@yahoo.com","notification_folder":"Inbox","notification_user":"converge_one@yahoo.com","notification_password":"Thenewcti1","notification_debug":"on","notification_to":"cloud_git@convergeone.com","notification_starttls.enable":"on","notification_host":"smtp.mail.yahoo.com","notification_port":"465"}}
	 * @throws JSONException on JSON errors.
	 * @throws IOException on I/O errors.
	 */
	public static JSONObject getConfiguration () throws JSONException, IOException {
		if ( config.isEmpty()) {
			configLoad(false);
		}
		JSONObject root 			= new JSONObject();
		
		// copy node cfg
		JSONObject params			= new JSONObject(config);
		
		if ( params.length() == 0) {
			throw new IOException("Missing notification configuration.");
		}
		root.put("params", params);
		
		//System.out.println("getConfiguration(): " + root +  " CFG NODE:" + config);
		return root;
	}
	
	/**
	 * Validate an event frequency.
	 * @param freq See {@link Frequency}.
	 * @return True if the event should execute for the default threshold: WEEKLY: Every Monday, DAILY: @ 8AM.
	 */
	public static boolean checkFrequency (Frequency freq) {
		// https://docs.oracle.com/javase/7/docs/api/java/util/GregorianCalendar.html
		// create a GregorianCalendar with the current date and time
		Calendar cal =  new GregorianCalendar();
		cal.setTime(new Date());
		
		// https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html#HOUR_OF_DAY
		switch ( freq) {
			case WEEKLY:
				// Field number for get and set indicating the day of the week. This field takes values SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, and SATURDAY.
				return cal.get(Calendar.DAY_OF_WEEK) == DAY_THRESHOLD;
			case DAILY:
				// Field number for get and set indicating the hour of the day. HOUR_OF_DAY is used for the 24-hour clock. E.g., at 10:04:15.250 PM the HOUR_OF_DAY is 22.
				return cal.get(Calendar.HOUR_OF_DAY) == HOUR_THRESHOLD;
			case MONTHLY:
				// Field number for get and set indicating the day of the month. This is a synonym for DATE. The first day of the month has value 1.
				return cal.get(Calendar.DAY_OF_MONTH) == DAY_OF_MONTH_THRESHOLD;
			case DISABLED:
				return false;
		}
		return false;
	}
	

	/**
	 * Save alerts. Invoke from a JSP file.
	 * @param request JSP HTPP request.
	 * @throws FileNotFoundException 
	 * @throws IOException On invalid params: txtCPU, txtThreads, txtHeap.
	 * @since 1.0.0
	 */
	public static void save(HttpServletRequest request ) throws FileNotFoundException, IOException {
		Enumeration<String> names 	= request.getParameterNames();
		
		// loop thru all request params
		while ( names.hasMoreElements()) {
			String name = names.nextElement();
			String val	= request.getParameter(name);
			
			// save them: update stuff goes in a separate file
			if ( !name.equals("action") && val != null && !val.isEmpty()) {
				
				// update system
				if (AutoUpdateUtils.configIsUpdateKey(name)) {
					// encrypt passwords
					if ( name.equals(KEY_PASSWORD)) {
						val = EncryptionTool.encryptAndTagPassword(val);
					}

					log.debug("[Update] Save " + name + " = " + val);
					AutoUpdateUtils.configSetProperty(name, val);
					
				}
			}
		}
		// update system save
		AutoUpdateUtils.configSave();
		
	}
}
