package com.rts.service;

import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * OS Alerts that can be displayed in a web page.
 * <ul>
 * <li> Disk almost full.
 * <li> High threads, etc.
 * </ul>
 * @author VSilva
 *
 */
public class OSAlerts {

	/**
	 * Get all DISK alerts.
	 * @return JSON [ { 'message' : 'Low disk 5%', type: danger, device : DISK}, ...]
	 */
	public static JSONArray getDiskAlerts() {
		JSONArray array = new JSONArray();
		
		// Loop thru dirs
		for (Path root : FileSystems.getDefault().getRootDirectories()) {
		    try {
		    	FileStore store = Files.getFileStore(root);
		    	final long available 	= store.getUsableSpace();
		    	final long total 		= store.getTotalSpace();
		    	final int freePct 		= (int) (((float)available / total) * 100);
		    	
		    	// low free disk %
		    	if ( freePct < 50 ) {
		    		final JSONObject alert = new JSONObject();
		    		alert.put("message", "Low free disk for " + root + " @ " + freePct + "%");
		    		alert.put("type", "danger");
		    		alert.put("device", "DISK");
		    		array.put(alert);
		    	}
		    } catch (Exception e) {
		        //System.out.println("error querying space: " + e.toString());
		    }
		}
		return array;
	}
	
	/**
	 * Get all OS alerts: disk, threads, etc.
	 * @return JSON [ { 'message' : 'Low disk 5%', type: danger, device : DISK}, ...]
	 */
	public static JSONArray getAlerts() {
		JSONArray disk = getDiskAlerts();
		
		// TODO: Add thread & others
		return disk;
	}
}
