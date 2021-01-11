package com.cloud.core.cron;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.cron.AutoUpdateUtils;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.io.MailTool;
import com.cloud.core.io.ZipTool;
import com.cloud.core.logging.Container;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.net.DropboxClient;
import com.cloud.core.services.CloudServicesMasterTask;
import com.cloud.core.types.CoreTypes;

/**
 * Pro active error notification system
 * 
 * <ul>
 * <li> Check container logs for exceptions
 * <li> Sends a message via SMTPS to cloud_git@convegeone.com
 * <li> Polls continuously for errors.
 * </ul>
 * 
 * @author vsilva
 * @version 1.0.0 8/11/2018
 *
 */
public class ErrorNotificationSystem {

	private static final Logger log 			= LogManager.getLogger(ErrorNotificationSystem.class);
	
	// The prefix used for all HTML form keys
	public static final String CFG_KEY_PREFIX 	= "notification_";

	// Configuration keys
	public static final String KEY_VENDOR 		= "notification_vendor";
	public static final String KEY_FREQ 		= "notification_frequency";
	public static final String KEY_FILTER 		= "notification_filter";
	public static final String KEY_TO 			= "notification_to";

	// Dropbox logs folder upload token
	public static final String KEY_UPLOAD_LOGSFTOK 	= "notification_logsFolderToken";
	
	// If true, upload logs to cloud storage (default true)
	public static final String KEY_UPLOAD_LOGS 		= "notification_logsUploadToCloudStorage";

	private static boolean executed;
	
	// Tick count
	private static long tickCount;
	
	/**
	 * Get the default configuration values from the class path /configuration/pen_cfg.json.
	 * @return JSON see /configuration/pen_cfg.json.
	 * @throws JSONException
	 * @throws IOException
	 */
	public static JSONObject getDefaultConfiguration () throws JSONException, IOException {
		InputStream is 	= ErrorNotificationSystem.class.getResourceAsStream("/configuration/pen_cfg.json");
		String json 	= IOTools.readFromStream(is);
		JSONObject root = new JSONObject(json);
		return root;
	}


	/**
	 * ZIP the container log folder.
	 * @param nodeName A prefix used in the file name: NodeLogs{PREFIX}_MM-DD-YYYY.zip
	 * @return The full path of the compressed file.
	 * @throws IOException
	 */
	public static String zipLogFolder(final String nodeName) throws IOException {
		final SimpleDateFormat fmt 	= new SimpleDateFormat("MM-dd-yyyy");
		final String fileName 		= "NodeLogs" + nodeName + "_" + fmt.format(new Date()) + ".zip";
		final String  path 			=  CoreTypes.TMP_DIR + File.separator + fileName;
		
		ZipTool.zipFolder(Container.getDefautContainerLogFolder(), path);
		return path;
	}
	
	/**
	 * Execute a single interaction of the process.
	 * Meant to be called multiple times within a polling thread.
	 */
	public static void tick () throws JSONException, IOException, AddressException, MessagingException {
		String temp					= AutoUpdateUtils.configGetProperty("notification_frequency");
		if ( temp == null ) {
			return;
		}
		AutoUpdateUtils.Frequency freq 		= AutoUpdateUtils.Frequency.valueOf(temp);
		boolean shouldExecute			 	= AutoUpdateUtils.checkFrequency(freq);
		//System.out.println("***[CHECKERRORS] TICK FREQ=" + freq + " exec=" + shouldExecute);
		
		if ( shouldExecute && ! executed ) {
			checkForErrorsSync();
			executed = true;
		}
		// reset?
		if ( !shouldExecute ) {
			executed = false;
		}
	}
	
	/**
	 * Check container logs for exceptions and send a notification via email. Called multiple times by the tick() method
	 * or once by the "Check for errors" button in the console.
	 * <p>Note: This method is synchronous and may take a long time for large folders.</p>
	 * For example, for a 500MB folder with 200+ files it takes ~5min.
	 */
	public static String checkForErrorsSync () throws JSONException, IOException, AddressException, MessagingException {
		// { params: { key1: val1, key2: val2,...}} load from disk or default (from class path)
		JSONObject config			= AutoUpdateUtils.configExists() ? AutoUpdateUtils.getConfiguration() : getDefaultConfiguration();
		JSONObject params			= config.getJSONObject("params");

		final String path 			= Container.getDefautContainerLogFolder();

		// Log file extension filters (.log)
		final String[] exts 		= new String[] {"log"};
		
		// File name filters (Cloud*)
		final String[] filters		= new String[] {"^Cloud.*$"};

		log.info("[Alerts] Checking for container errors in " + path + " Extensions: " + Arrays.toString(exts) + " Name Filter: " + Arrays.toString(filters));
		
		// List container log files
		File[] files 				= FileTool.listFiles(path, exts, filters);

		// Look for exceptions using the search filter (regexp)
		final String dataFilter		= params.getString(KEY_FILTER); 
		StringBuffer html			= new StringBuffer();

		// DataFilter Will trigger an exception alert - log.info("[Alerts] Processing " + files.length + " files. Data Filter: " + dataFilter);
		
		// build notification HTML
		long t0 					= System.currentTimeMillis();
		
		for ( File f : files) {
			try {
				//System.out.println("PROCESS FILE " + f.getAbsolutePath() + " len=" + f.length());
				if ( f.length() == 0 ) {
					continue;
				}
				// JSON: { data : [ [DATE, SOURCE, MESSAGE] ...] }
				JSONObject root = Container.filterLogFile(f.getAbsolutePath(), dataFilter);
				
				if ( !root.has("data")) {
					continue;
				}
					
				JSONArray data = root.getJSONArray("data");
				if ( data.length() == 0) {
					continue;
				}
				
				html.append("<h1>" + f.getName() + "</h1>");
				html.append("<table>");
				for (int i = 0; i < data.length(); i++) {
					JSONArray row = data.getJSONArray(i);
					html.append("<tr><td>" + row.getString(0) + "</td>"); 	// Date
					html.append("<td>" + row.getString(1) + "</td>"); 		// source
					html.append("<td><pre>" + cleanupText(row.getString(3)) + "</pre></td></tr>"); 	// Message
				}
				html.append("</table>");
				
				// MAX buf size = 2 MB
				if ( html.length() > 2097152) {
					html.append("<h1>Max Buffer Size Exceeded</h1>");
					break;
				}
				
			} catch (Exception e) {
				log.error("Error Notification System: Process log files.", e);
			}
		}
		log.info("[Alerts] Processed " + files.length + " files in " + (System.currentTimeMillis() - t0)  + " ms. Error response buffer size: " + html.length() + " bytes.");

		// Send it?
		if ( html.length() > 0) {
			// 2/9/2019 Add the node name to the body
			final String prefix 	= "<font color=blue><h2>Sender Node: " + CoreTypes.NODE_NAME + "</h2></font>";
			final String vendor 	= params.optString(KEY_VENDOR, IOTools.getHostname());
			final String subject 	= "Exception Notification System: Report for Customer " + vendor;
			
			// This can take a long time. On a 400MB folder it takes 1 min.
			t0						= System.currentTimeMillis();
			String logsPath 		= zipLogFolder(vendor);
			//File[] attachments 		= new File[] { new File(logsPath)};
			final long size 		= FileTool.sizeOfFile(logsPath);

			log.info("[Alerts] Compressed " + path + " to " + logsPath + " in " + (System.currentTimeMillis() - t0)  + " ms for vendor " + vendor + " File Size: " + size + " bytes.");

			/* 2/17/2019 The C1AS smtp server will reject attachements larger tha 1MB
			// Attachment larger than 15MB won't be sent!
			final boolean tooBig	= size > 15 * 1024 * 1024 ? true : false;

			if ( tooBig) {
				html.append("<font color=red><h1>Unable to attach Logs [too large @ " +  (size/1048576) + " MB]</h1></font>");
			}
			*/
			// Upload logs to (Cloud Storage and get view URL) dropbox and get
			final String logsLink 	= uploadLogsToCloudStorage(params, logsPath);
			
			if ( logsLink != null) {
				html.append("<font color=green><h2>View Logs @ " +  logsLink + "</h2></font>");
			}
			
			// May take ~45s on large messages
			t0 = MailTool.sendMail(CFG_KEY_PREFIX, params , subject, prefix + html.toString(), null) ; //tooBig ? null : attachments);
			
			log.info("[Alerts] Sent mail in " + t0  + " ms to " + params.getString(KEY_TO) + " with subject: " + subject);
			
			return "Errors found. Notification sent to " + params.getString(KEY_TO);
		}
		return "No errors found.";
	}

	/**
	 * Upload the zipped logs to clous storage (Dropbox)
	 * @param params Notification system parameters (see pen_cfg.json)
	 * @param localPath Zipped logs full path.
	 * @return The dropbox public view (share) link.
	 */
	private static String uploadLogsToCloudStorage (JSONObject params, final String localPath) {
		try {
			if ( !params.optBoolean(KEY_UPLOAD_LOGS, true) ) {
				log.warn("[Alerts] Upload to cloud storage is DISABLED from the configuration.");
				return null;
			}
			long t0								= System.currentTimeMillis();
			final String DEF_LOGSUPLOAD_TOK 	= "F1S6eC-N7sAAAAAAAAAAJbGhvc4h17DcZjLRSCcztey9Ig-KKjwYp1InqylkyrGk";
			final String logUploadTok  			= params.optString(KEY_UPLOAD_LOGSFTOK, DEF_LOGSUPLOAD_TOK);
			final String remotePath 			= "/" + FileTool.getFileName(localPath);

			Map<String, String> cfg 			= new HashMap<String, String>();
			cfg.put(DropboxClient.KEY_TOKEN, DEF_LOGSUPLOAD_TOK);
			
			final DropboxClient dbx 			= new DropboxClient(cfg);
			dbx.upload(localPath, remotePath, logUploadTok);

			final String link 					= dbx.getSharedLink(remotePath, logUploadTok);
			long t1 							= System.currentTimeMillis() - t0;
			
			log.info("[Alerts] Upload to cloud storage local: " + localPath + " Remote: " + remotePath + " took " +  t1 + " ms. Link: " + link);
			return link;
		} catch (Exception e) {
			log.error("Upload to cloud storage " + localPath, e);
		}
		return null;
	}
	
	/**
	 * Poor man's text cleanup logic: clean rogue XML messages.
	 * @param text text to cleanup.
	 * @return
	 */
	private static String cleanupText (final String text) {
		// rogue XML cleanup
		return text.replaceAll("<", "&lt;");
	}
	
	/**
	 * This one can be called safely from the console. It check the size of the log folder.
	 * If < 100MB runs in sync mode, else runs in async mode.
	 * @return A process result message.
	 */
	public static String checkForErrorsConsole () throws Exception {
		final String path 	= Container.getDefautContainerLogFolder();
		long size 			= FileTool.sizeOfDirectory(path);
		
		// Asynch if > 100MB
		if ( size > 100 * 1024 * 1024) {
			return checkForErrorsAsync();
		}
		return checkForErrorsSync();
	}
	
	/**
	 * Asynchronous version of checkForErrorsSync()
	 * @return 'This process has been executed in the background. Please look at the node logs for details.'
	 */
	public static String checkForErrorsAsync ()  {
		Thread t = new Thread (new Runnable() {
			public void run() {
				try {
					checkForErrorsSync();
				} catch (Exception ex) {
					log.error("[Alerts] Check for container exceptions failed.", ex);
				}
			}
		}, "ERROR-NOTIFICATION-SYSTEM@" + System.currentTimeMillis());
		t.start();
		return "This process has been executed in the background. Please look at the node logs for details.";
	}
	
	/**
	 * To be executed from the {@link CloudServicesMasterTask}.
	 */
	public static void run ()  {
		// about once/5 min
		if ( (tickCount++ % 300) != 0 || tickCount <= 2) {
			return;
		}
		try {
			tick();
		} catch (Exception e) {
			log.error("Error Notification System:", e);
		}
	}
}
