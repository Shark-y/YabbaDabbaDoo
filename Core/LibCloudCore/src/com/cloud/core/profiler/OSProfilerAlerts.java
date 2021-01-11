package com.cloud.core.profiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.CloudNotificationService;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;

/**
 * This class manages the saving and loading of the OS Profiler (osp.jsp) alert thresholds:
 * <ul>
 * <li> Average CPU usage (defaults to 70%).
 * <li> Live thread count (defaults: 150).
 * <li> Free heap (10 MB).
 * </ul>
 * @author VSilva
 * @version 1.0.0
 * @version 1.0.1 - Added new support for license alerts thresholds.
 */
public class OSProfilerAlerts {

	private static final Logger log = LogManager.getLogger(OSProfilerAlerts.class);
	
	// alerts file name
	private static final String FILE_NAME = "ospalerts.ini";
	
	// If true, hide the Alerts tab in the OSP JSP.
	private static boolean hideOSPAlertsTab;
	
	// Property file threshold keys
	private static final String KEY_CPU 		= "cpu";
	private static final String KEY_THREADS 	= "threads";
	private static final String KEY_HEAP 		= "heap";
	private static final String KEY_LICENSE 	= "license";
	
	/**
	 * By default, alerts are stored in $home/.cloud/[PRODUCT-NAME]/ospalerts.ini
	 * @return $home/.cloud/[PRODUCT]
	 */
	private static String getBasePath() {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		
		// $home/.cloud/Product
		String basePath 		= cfg.getConfigLocation();
		String filePath			= basePath + File.separator + FILE_NAME;
		return filePath;
	}
	
	/**
	 * If true the alerts tab in the OS Profiler JSP will be hidden.
	 * @return True if alerts tab hidden flag is set.
	 */
	public static boolean hideAlertsTab() {
		return hideOSPAlertsTab;
	}
	
	/**
	 * Invoke to hide the alerts tab in the OS Profiler page (OSP.jsp).
	 */
	public static void hideOSPAlertsTab() {
		hideOSPAlertsTab = true;
	}
	
	/**
	 * Invoke multiple times to load alerts.
	 * @since 1.0.0
	 */
	public static void reload() {
		FileInputStream fis		= null;
		Properties props 		= new Properties();
		String filePath			= getBasePath();
		try {
			if ( !FileTool.fileExists(filePath)) {
				return;
			}
			log.debug("[OSProfiler] Alerts load from " + filePath);
			
			fis = new FileInputStream(filePath);
			props.load(fis);
			int cpu 	= Integer.parseInt(props.getProperty(KEY_CPU));
			int threads = Integer.parseInt(props.getProperty(KEY_THREADS));
			int heap 	= Integer.parseInt(props.getProperty(KEY_HEAP));
			
			CloudNotificationService.setThresholds(cpu, threads, heap);
			
			// 5/21/2017 - Optional for products that support licensing.
			if ( props.containsKey(KEY_LICENSE)) {
				CloudNotificationService.setLicenseThreshold(Integer.parseInt(props.getProperty(KEY_LICENSE)));
			}
		} catch (Exception e) {
			log.error("[OSProfiler] Reload alerts.", e);
		}
		finally {
			IOTools.closeStream(fis);
		}
	}

	/**
	 * Save alerts. Invoke from a JSP file.
	 * @param request JSP HTPP request.
	 * @throws IOException On invalid params: txtCPU, txtThreads, txtHeap.
	 * @since 1.0.0
	 */
	public static void save(HttpServletRequest request ) throws IOException {
		String p1 = request.getParameter("txtCPU");
		String p2 = request.getParameter("txtThreads");
		String p3 = request.getParameter("txtHeap");
		String p4 = request.getParameter("txtPeakLic");		// optional for license enabled products
		
		if ( p1 == null) 	throw new IOException("Invalid CPU threshold.");
		if ( p2 == null) 	throw new IOException("Invalid Threads threshold.");
		if ( p3 == null) 	throw new IOException("Invalid Heap threshold.");
		
		int cpu 	= Integer.parseInt(p1);
		int threads = Integer.parseInt(p2);
		int heap 	= Integer.parseInt(p3);
		
		if ( p4 != null) {
			save(cpu, threads, heap, Integer.parseInt(p4));
		}
		else {
			save(cpu, threads, heap);
		}
	}
	
	/**
	 * Save option #2.
	 * @param cpu Average CPU usage.
	 * @param threads Live thread count.
	 * @param heap Free heap.
	 * @since 1.0.0
	 */
	public static void save(int cpu, int threads, int heap) {
		Properties props 		= new Properties();
		props.put(KEY_CPU, 		String.valueOf(cpu));
		props.put(KEY_THREADS, 	String.valueOf(threads));
		props.put(KEY_HEAP, 	String.valueOf(heap));
		saveProperties(props);
		CloudNotificationService.setThresholds(cpu, threads, heap);
	}

	/**
	 * Save option #3.
	 * @param cpu Average CPU usage.
	 * @param threads Live thread count.
	 * @param heap Free heap.
	 * @param license License threshold alert (optional).
	 * @since 1.0.1
	 */
	public static void save(int cpu, int threads, int heap, int license) {
		Properties props 		= new Properties();
		props.put(KEY_CPU, 		String.valueOf(cpu));
		props.put(KEY_THREADS, 	String.valueOf(threads));
		props.put(KEY_HEAP, 	String.valueOf(heap));
		props.put(KEY_LICENSE, 	String.valueOf(license));
		
		saveProperties(props);
		CloudNotificationService.setThresholds(cpu, threads, heap);
		CloudNotificationService.setLicenseThreshold(license);
	}

	private static void saveProperties(Properties props){
		FileOutputStream fos 	= null;
		String filePath			= getBasePath();
		
		try {
			log.debug("[OSProfiler] Alerts save to " + filePath);
			
			fos = new FileOutputStream(filePath);
			props.store(fos, "");
		} catch (Exception e) {
			log.error("[OSProfiler] Save alerts.", e);
		}
		finally {
			IOTools.closeStream(fos);
		}
	}

}
