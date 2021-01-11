package com.cloud.core.profiler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.types.CoreTypes;

/**
 * Simple Class used to track real time license usage as well as simple historical peak license usage.
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation.
 */
public class LicenseMetrics {
	private static final Logger log = LogManager.getLogger(LicenseMetrics.class);
	
	/** Number of used licenses */
	int used;
	
	/** Total # of licenses */
	int total;
	
	/** Daily Max (peak) usage */
	int peakUsage;

	/** Singleton instance */
	private static final LicenseMetrics INSTANCE = new LicenseMetrics();
	
	/**
	 * Singleton class.
	 * @return A singleton instance of this class.
	 */
	public static LicenseMetrics getInstance() {
		return INSTANCE;
	}
	
	private LicenseMetrics() {
	}
	
	/**
	 * Runnable task to save the metrics to disk. The format is:
	 * <ul>
	 * <li>DATE,PEAK-USAGE</li>
	 * </ul>
	 * The default file path is $home/.cloud/[PRODUCT-NAME]/licensemetrics-{NODENAME}.properties.
	 * The DATE format is: yy-MM-dd HH:mm
	 */
	public final Runnable SAVE_TO_DISK_TASK = new Runnable() {
		public void run() {
			try {
				if (! CloudServices.supportsLicense()) {
					log.info("License metrics task: Product diesn;t support a license. Abort save task." );
					return;
				}
				save();
			} catch (IOException e) {
				log.error("Save to disk task", e);
			}
		}
	};

	/**
	 * Update metrics as well as peak usage.
	 * @param used # of used licenses.
	 * @param total Total # of licenses.
	 */
	public void update ( final int used, final int total) {
		if (used > 0 ) 	{ this.used 	= used; }
		if (total > 0)	{ this.total	= total;}
		
		// record peak usage.
		if ( this.used > this.peakUsage) {
			this.peakUsage = this.used;
		}
	}
	
	/**
	 * Save the {@link LicenseMetrics} to disk. The format per line is:
	 * <ul>
	 * <li>DATE,PEAK-USAGE</li>
	 * </ul>
	 * The default file path is $home/.cloud/[PRODUCT-NAME]/licensemetrics-{NODENAME}.properties.
	 * The DATE format is: yy-MM-dd HH:mm
	 * @throws IOException if there is an I/O error.
	 */
	void save () throws IOException {
		Date now 		= new Date();
		// ss mm HH dd MM yyyy
		final String date 	= new SimpleDateFormat("yy-MM-dd HH:mm").format(now); 
		
		// Format DATE(yy-MM-dd HH:mm),PEAK-USAGE
		final String path 	= getBasePath();
		
		/** 6/22/2019 Create if missing java.io.FileNotFoundException: C:\Users\vsilva\.cloud\CloudConnector\licensemetrics-CloudConnectorNode002.log (The system cannot find the file specified)
	at java.io.FileInputStream.open0(Native Method)
	at java.io.FileInputStream.open(FileInputStream.java:195)
	at java.io.FileInputStream.<init>(FileInputStream.java:138)
	at java.io.FileInputStream.<init>(FileInputStream.java:93) */
		if ( !FileTool.fileExists(path)) {
			final String payload 	= String.format("%s,%d%n", date, peakUsage);
			appendToFile(path, payload, false); //true);
			return;
		}
		// 2/6/2020 if record exists, yy-MM-dd HH:mm,PEAK update file, else append.
		fixAndUpdateFile(date, path, peakUsage);
	}
	
	/**
	 * By default, alerts are stored in $home/.cloud/[PRODUCT-NAME]/licensemetrics-{NODENAME}.properties
	 * @return $home/.cloud/[PRODUCT]/licensemetrics-{NODENAME}.properties
	 * @throws IOException 
	 */
	private static String getBasePath() throws IOException {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		
		// $home/.cloud/Product
		final String basePath 		= cfg.getConfigLocation();
		
		// Node name (/CloudConnectorNode002)
		String fileName			= cfg.getProperty(NodeConfiguration.KEY_CTX_PATH);
		
		if ( fileName == null) {
			throw new IOException("Save license metrics: A node name is required to save.");
		}
		// remove start '/'
		if ( fileName.startsWith("/")) fileName = fileName.substring(1);	
		return basePath + File.separator + "licensemetrics-" + fileName + ".log";
	}

	/**
	 * Creates a file output stream to write to the file with the specified name. If the second argument is true, 
	 * then bytes will be written to the end of the file rather than the beginning. A new FileDescriptor object is created to represent this file connection. 
	 * @param path Full path of the target file.
	 * @param payload Payload to write.
	 * @param append if true, then bytes will be written to the end of the file rather than the beginning 
	 * @throws IOException 
	 */
	static void appendToFile (final String path, final String payload, final boolean append) throws IOException {
		OutputStream fos = null;
		try {
			fos = new BufferedOutputStream( new FileOutputStream(path, append)); 
			fos.write(payload.getBytes(CoreTypes.CHARSET_UTF8));
		} finally {
			IOTools.closeStream(fos);
		}
	}
	
	/**
	 * Update path with a value for date (date) removing duplicate lines and keeping the highest value of each set.
	 * @param date Date to look for using format 20-02-06 19:28
	 * @param path Full path to the file (1 record per line - format DATE,PEAK-USAGE)
	 * @param val Value to update (only if grater than disk)
	 * @throws IOException On I/O errors.
	 */
	static void fixAndUpdateFile (final String date, final String path, final int val) throws IOException {
		BufferedReader br 	= new BufferedReader(new InputStreamReader(new FileInputStream(path), CoreTypes.ENCODING_UTF8));
		String line 		= null;
		String prev			= "";
		StringBuffer buf 	= new StringBuffer();
		String[] temp 		= null;
		boolean bool 		= false;
		boolean save 		= false;
		int max 			= 0;
		
		List<String> list = new ArrayList<String>();
		
		while ((line = br.readLine()) != null) {
			// 20-02-06 19:28,PEAK 
			temp 		= line.split(" ");
			String key = temp[0];	// 20-02-06 
			
			// duplicate found
			if ( key.equals(prev)) {
				bool 		= true;
				save 		= true;
				int peak 	= Integer.parseInt(line.split(",")[1]); 
				max 		=  peak > max ? peak : max;
				//System.out.println("duplicate " + line);
			}
			else {
				if ( bool ) {
					int idx = list.size() - 1;
					list.set(idx, list.get(idx).split(",")[0] + "," + max);
				}
				list.add(String.format("%s" ,line));
				bool 	= false;
				max 	= 0;
			}
				
			prev = key;
		}
		br.close();
		
		// Here all duplicates have been removed and the max peak fixed for each entry
		// Grab last line
		line = list.get(list.size() -1 );

		// Update record if already exists & peak < val else append
		prev = date.split(" ")[0];	// YY-MM-DD
		
		if (line.startsWith(prev)) {
			// replace: remove last if disk < val
			temp 			= line.split(",");
	    	int peakDisk 	= Integer.parseInt(temp.length > 1 ? temp[1] : "0");
			if ( val > peakDisk) {
				list.remove(list.size() -1);
				list.add(String.format("%s,%d" ,date, val));
				save 		= true;
			}
		}
		else {
			// append
			list.add(String.format("%s,%d" ,date, val));
			save = true;
		}

		if ( save ) {
			for ( String s : list) {
				buf.append(String.format("%s%n" ,s));
			}
			IOTools.saveText(path, buf.toString());
		}
	}
	
	/*
	static void updateFile (final String date, final String path, final int val) throws IOException {
		BufferedReader br 	= new BufferedReader(new InputStreamReader(new FileInputStream(path), CoreTypes.ENCODING_UTF8));
		String line 		= null;
		
		StringBuffer buf 	= new StringBuffer();
		boolean found		= false;
		boolean save		= false;
		String[] temp 		= null;
		
	    while ((line = br.readLine()) != null) {
	    	temp = date.split(" ");
	    	// Compare 20-02-06
	    	if ( line.startsWith(temp[0])) {
	    		// used to skip duplicates, caveat: will grab the 1st one
	    		if ( ! found) {
	    			found 			= true;
		    		//  Update 20-02-06 19:28,PEAK with val
			    	temp 			= line.split(",");
			    	int peakDisk 	= Integer.parseInt(temp.length > 1 ? temp[1] : "0");

			    	buf.append(temp[0] + "," + (val > peakDisk ? val : peakDisk) + CoreTypes.LINE_SEPARATOR);
			    	save 			= val > peakDisk;
	    		}
	    	}
	    	else {
	    		buf.append(line + CoreTypes.LINE_SEPARATOR);
	    	}
	    }
	    br.close();
	    if ( !found ) {
	    	buf.append(date + "," + val);
	    	save = true;
	    }
	    if ( save ) {
	    	IOTools.saveText(path, buf.toString());
	    }
	}
	*/
	
	/**
	 * @return [Used: X Total: Y]
	 */
	@Override
	public String toString() {
		return String.format("[Used: %d Total: %d]", used, total);
	}
	
	/**
	 * Convert the historical license metrics to a JSON array <b>using the default data tables format.</b>
	 * Meant to be displayed by the cloud console.
	 * @return Historical license metrics in data tables JSON: [[DATE1,VAL1],[DATE2,VAL2]...]
	 * @throws IOException if I/O errors (file problems, etc)
	 */
	public JSONArray toJSON () throws IOException {
		final String path 	= getBasePath();
		// format per line: 17-24-03 06:00,10
		final String log	= IOTools.readFileFromFileSystem(path);
		String[] temp 	= log.split(CoreTypes.LINE_SEPARATOR);
		JSONArray root	= new JSONArray();
		
		for (int i = 0; i < temp.length; i++) {
			JSONArray row 	= new JSONArray();
			String line		= temp[i];			// DATE,NUMBER
			String[] vals	= line.split(",");
			if ( vals.length != 2) {
				continue;
			}
			row.put(vals[0]);
			row.put(vals[1]);
			root.put(row);
		}
		return root;
	}
}