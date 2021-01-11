package com.cluster.update;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.cron.AutoUpdateUtils;
import com.cloud.core.cron.ErrorNotificationSystem;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.MailTool;
import com.cloud.core.logging.Container;
import com.cloud.core.net.DropboxClient;
import com.cloud.core.net.DropboxClient.DropboxFile;

/**
 * Automated Update System.
 * <ul>
 * <li> Reads the local container web-apps folder
 * <li> Compares versions against cloud storage archives (Dropbox)
 * <li> If update found, downloads & copies the file (installs)
 * <li> Send a notification about it.
 * </ul>
 * 
 * Cloud Storage via Dropbox - https://www.dropbox.com/login
 * 
 * @author VSilva
 * @version 1.0.0
 */
public class AutoUpdate {

	// The prefix used for all HTML form keys
	static final String CFG_KEY_PREFIX 		= "update_";

	// Configuration keys
	public static final String KEY_FREQ 	= "update_frequency";
	public static final String KEY_TOKEN 	= "update_token";
	public static final String KEY_TC_URL 	= "update_tomcatURL";
	public static final String KEY_TC_USER 	= "update_tomcatUser";
	public static final String KEY_TC_PWD 	= "update_tomcatPassword";

	// used to avoid multiple executions
	private static boolean executed;

	/**
	 * Wraps a WebApp installed in the Tomcat Container webapps folder.
	 * @author VSilva
	 *
	 */
	public static class WebApp {
		File file; 
		
		public WebApp(File file) {
			this.file = file;
		}
		
		public String getName() {
			return file.getName();
		}

		@Override
		public boolean equals(Object obj) {
			return getNamePrefix().equals(((WebApp)obj).getNamePrefix());
		}
		
		@Override
		public int hashCode() {
			return getName().hashCode();
		}
		
		/**
		 * @return Get the file name minus the version and extension: NAME##VERSION.war => NAME 
		 */
		public String getNamePrefix() {
			return file.getName().split("##")[0];
		}

		public String getVersion () {
			String name 	= getName();
			String dotExt 	= "." + FileTool.getFileExtension(name);
			return name.contains("##") ? name.split("##")[1].replace(dotExt, "") : null;
		}
		
		@Override
		public String toString() {
			return getName() + " version=" + getVersion() + " path=" + file.toString();
		}
	}
	
	/**
	 * Execute a single tick of the auto update. It can be called multiple times within a
	 * continuous thread.
	 * 
	 * @throws JSONException on configuration errors.
	 * @throws IOException on I/O errors.
	 */
	public static void tick () throws JSONException, IOException {
		String temp					= AutoUpdateUtils.configGetProperty(KEY_FREQ);
		if ( temp == null ) {
			//System.out.println("***UPDATE TICK no notif freqeuncey");
			return;
		}
		AutoUpdateUtils.Frequency freq 		= AutoUpdateUtils.Frequency.valueOf(temp);
		boolean shouldExecute			 	= AutoUpdateUtils.checkFrequency(freq);
		//System.out.println("***TICK UPDATE FREQ=" + freq + " exec=" + shouldExecute);
		
		if ( shouldExecute && ! executed ) {
			checkForUpdates();
			executed = true;
		}
		// reset?
		if ( !shouldExecute ) {
			executed = false;
		}
	}
	
	private static String getContainerWebAppsFolder () {
		return Container.TOMCAT_HOME_PATH + File.separator + "webapps";
	}
	
	/**
	 * Get the latest Web apps installed in the tomcat container
	 * @param filters List of regular expression used to match for names. For example: new String[] {"Cloud.*"} matches Cloud apps.
	 * @return A list of installed {@link WebApp}. Only the latest by web-app version.
	 */
	private static List<WebApp> getInstalledApps(String[] filters) {
		final String basePath 	= getContainerWebAppsFolder(); 
		final File[] files 		= /*AutoUpdateUtils*/FileTool.listFiles(basePath, new String[]{"war"}, filters);
		List<WebApp> apps 		= new ArrayList<WebApp>();
		Map<String, WebApp> latest = new HashMap<String, AutoUpdate.WebApp>();
		
		for ( File file : files) {
			final WebApp app = new WebApp(file);
			//System.out.println(file + " exists=" + apps.contains(app));
			
			// store the latest by version only
			if (! apps.contains(app)) {
				latest.put(app.getNamePrefix(), app);
			}
			else {
				//System.out.println(app.getNamePrefix() + " V1=" + latest.get(app.getNamePrefix()).getVersion() + " V2=" + app.getVersion());
				if ( latest.get(app.getNamePrefix()).getVersion().compareTo(app.getVersion()) < 0 ) {
					latest.put(app.getNamePrefix(), app);
				}
			}
		}
		//System.out.println("** LATEST:" + latest);
		for ( Map.Entry<String, WebApp> entry : latest.entrySet() ) {
			apps.add(entry.getValue());
		}
		return apps;
	}
	
	/**
	 * Check for updates by comparing local web apps against Dropbox archives.
	 * 
	 * @return A description message: 'Updated X web-apps' OR 'No updates found'.
	 */
	public static String checkForUpdates () throws JSONException, IOException {
		// get the latest update config { params: { key1: val1, key2: val2, ...}}
		JSONObject root 		= AutoUpdateUtils.getConfiguration();
		JSONObject conf 		= root.getJSONObject("params");
		final String fileFilter	= "^Cloud.*$"; 
		
//		System.out.println("CHECK 4 UPDATES conf=" + conf.toString(1));
		
		List<WebApp> apps = getInstalledApps(new String[] {fileFilter});
		
//		for ( WebApp app : apps) {
//			System.out.println("WEBAPP:" + app);
//		}
		
		// List all files from the Dropbox update site
		Map<String, String> params 	= new HashMap<String, String>();
		params.put(DropboxClient.KEY_TOKEN, conf.getString(KEY_TOKEN));
		
		DropboxClient db 			= new DropboxClient(params);
		List<DropboxFile> remotes	= db.listFolder("");
		
//		for (DropboxFile remote : remotes) {
//			if ( remote.name.matches(fileFilter)) {
//				System.out.println("REMOTE: " + remote);
//			}
//		}
		// find updates....
		List<DropboxFile> updates	= new ArrayList<DropboxFile>();

		// Updated apps to be stopped
		List<WebApp> oldApps	= new ArrayList<WebApp>();

		StringBuffer html			= new StringBuffer();

		for ( WebApp app : apps) {
			for (DropboxFile remote : remotes) {
				// split name-prefix and version
				String[] temp = remote.getName().split("##");
				if ( temp.length != 2) {
					continue;
				}
				String remotePrefix 	= temp[0];
				String remoteVersion	= temp[1];
				
				// remove extension from remote (including DOT)
				remoteVersion 			= remoteVersion.replace("." + FileTool.getFileExtension(remoteVersion), "");
				
				//System.out.println("UPDATE Check local: " + app.getName() + " remote:" + remote.name);
				if ( app.getNamePrefix().equals(remotePrefix)) {
					// Got one. compare versions
					if ( app.getVersion().compareTo(remoteVersion) < 0) {
						//System.out.println("UPDATE local: " + app.getName() + " with remote " + remote.name);
						
						html.append("<li>Updated " + app.getName()  + " from version " + app.getVersion() + " to version " + remoteVersion);
						updates.add(remote);
						oldApps.add(app);
					}
				}
			}
		}
		
		// no updates? return.
		if ( updates.size() == 0) {
			return "No updates found.";
		}
		
		// process/install updates
		for ( DropboxFile update : updates) {
			// 1. Download /{FILE} to TOMCAT_ROOT/webapps
			final String file		 = "/" + update.getName();
			final String destPath 	= getContainerWebAppsFolder() + File.separator + update.getName();
			
			System.out.println("DOWNLOADING " + file + " to " + destPath);
			
			// this should not fail. If it does, it will abort the whole thing.
			// C:\Program Files (x86)\Apache Software Foundation\Tomcat 7.0\webapps (Access is denied) 
			db.download(file, destPath);
		}
		
		// 2. Send a notification
		final String vendor 	= conf.getString(ErrorNotificationSystem.KEY_VENDOR);
		final String subject 	= "Automated Update System for Custoner " + vendor;
		
		try {
			MailTool.sendMail(ErrorNotificationSystem.CFG_KEY_PREFIX, conf , subject, html.toString(), null);
		} catch (Exception e) {
			e.printStackTrace();
			// roll back - delete updates
			for ( DropboxFile update : updates) {
				final String destPath 	= getContainerWebAppsFolder() + File.separator + update.getName();
				File f 					= new File(destPath);

				//System.out.println("ROLLBACK DELETING " + destPath);
				f.delete();
			}
		}
		
		// Stop the previous version(s) in the local tomcat manager
		final String url	= conf.getString(KEY_TC_URL);
		final String tcuser = conf.getString(KEY_TC_USER);
		final String tcpwd 	= conf.getString(KEY_TC_PWD);
		TomcatManagerClient tc = new TomcatManagerClient(url, tcuser, tcpwd);
		
		for ( WebApp app : oldApps) {
			try {
				System.out.println("STOP " + app);
				tc.stopWebApp(app.getNamePrefix(), app.getVersion());
			} catch (Exception e) {
				System.err.println("AUTO-UPDATE FAILED TO STOP: " + app);
			}
		}
		return "Updated " + updates.size() + " web-apps. Notification sent to " + conf.getString(ErrorNotificationSystem.CFG_KEY_PREFIX + MailTool.KEY_SMTP_TO);
	}
	
}
