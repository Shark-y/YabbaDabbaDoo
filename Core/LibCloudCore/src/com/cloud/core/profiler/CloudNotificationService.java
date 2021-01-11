package com.cloud.core.profiler;

import java.util.Date;

import org.json.JSONObject;

import com.cloud.core.profiler.LicenseMetrics;
import com.cloud.core.profiler.OSMetrics;
import com.cloud.core.profiler.OSProfilerAlerts;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.CloudServicesMasterTask;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * A very simple notification service that fires when:
 * <ul>
 * <li>System CPU (Average processor usage) goes above 70%.
 * <li>Thread count goes above 150.
 * <li>Free heap goes below 10MB.
 * </ul>
 * @author vsilva
 * @version 1.0.0
 * @version 1.0.1 5/19/2017 - New license threshold alerts support.
 *
 */
public class CloudNotificationService {

	private static final Logger log = LogManager.getLogger(CloudNotificationService.class);
	
	/** Max # of alerts before bumming out */
	private static final int MAX_ALERTS = 20;

	/** CPU Usage threshold that triggers an alert if > 80% avg usage by default. */
	private static int THRESH_CPU 		= 80;
	
	/** Thread count alert threshold. */
	private static int THRESH_TR_COUNT 	= 200;
	
	/** Free heap alert threshold. Any value below this will alert. */
	private static int THRESH_FREE_HEAP = 10;
	
	/** used to count ticks */
	private static long tickCount;

	/** alert counter used to prevent infinite alerts */
	private static int alertCount;
	
	/** 5/19/2017 License Usage threshold: triggers if > 70%  */
	private static int THRESH_LIC 		= 70;

	/** License metrics. Optional for products that support licensing. */
	private final static LicenseMetrics licenseMetrics = LicenseMetrics.getInstance();
	
	/**
	 * Set {@link LicenseMetrics}. Values are set only if > 0. For products that support licensing.
	 * @param used Current usage count. Only set if > 0.
	 * @param total Total number of licenses. Should not be zero for a configured node. Only set if > 0.
	 */
	public static void setLicenseMetrics (int used, int total) {
		licenseMetrics.update(used, total);
	}
	
	/**
	 * Set the thresholds that fire notifications.
	 * @param cpu CPU usage average (0-100). Anything above this # will trigger an alert.
	 * @param threads Live threads (above values).
	 * @param heap Free heap in MB - below values trigger alerts.
	 */
	public static void setThresholds (int cpu, int threads, int heap) {
		THRESH_CPU 			= cpu;
		THRESH_TR_COUNT 	= threads;
		THRESH_FREE_HEAP	= heap;
		log.debug("[Notifications] Set thresholds CPU = " + cpu + " Threads = " + threads + " Heap = " + heap);
	}

	/**
	 * Set the license alert threshold. Only for products that support licensing.
	 * @param license License alert threshold.
	 * @since 1.0.1
	 */
	public static void setLicenseThreshold (int license) {
		THRESH_LIC = license;
	}
	
	public static int getThresholdCPU () {
		return THRESH_CPU;
	}

	public static int getThresholdThreads () {
		return THRESH_TR_COUNT;
	}
	
	public static int getThresholdHeap () {
		return THRESH_FREE_HEAP;
	}

	/**
	 * License threshold pct.
	 * @return License threshold [0-100]
	 * @since 1.0.1
	 */
	public static int getThresholdLicense () {
		return THRESH_LIC;
	}

	/**
	 * Execute 1 tick of this micro-service.
	 */
	public static void run () {
		try {
			tick();
		} catch (Exception e) {
			log.error("OS Profiler Notification Service", e);
		}
	}
	
	private static void tick () throws Exception  {
		// about once/min
		if ( (tickCount++ % CloudServicesMasterTask.DEFAULT_TICK_PERIOD) != 0 || tickCount <= 2) {
			return;
		}

		if ( !CloudServices.isConfigured(false)) {
			return;
		}
		
		// 1/15/2020 - load alerts
		OSProfilerAlerts.reload();
		
		/**
		 * Interested in: SystemCpuLoad, threadCount, heapFree
		 */
		JSONObject metrics 	= OSMetrics.getOSMetrics();
		JSONObject os		= metrics.getJSONObject(OSMetrics.KEY_OS);
		
		double cpuLoad 		= os.getDouble(OSMetrics.KEY_SYS_CPU);	// 0.0-1.0
		int threadCount		= os.getInt(OSMetrics.KEY_THR_COUNT);
		long heapFree		= os.getLong(OSMetrics.KEY_HEAP_FREE);	// in bytes
		
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		
		// notification proto: if "none" then notifications are disabled.
		String proto 		= cfg.getProperty(Auditor.KEY_NOTIFY_PROTO);
		
		// Notifications disabled?
		if ( proto == null || proto.equalsIgnoreCase(Auditor.KEY_PROTO_DISABLED) || proto.equalsIgnoreCase(Auditor.KEY_PROTO_PLAIN)) {
			alertCount = 0;
			return;
		}

		log.debug("[Notifications] Metrics/Thresholds AvgCPU=" + cpuLoad + "/" + THRESH_CPU + " Threads=" + threadCount + "/" + THRESH_TR_COUNT 
				+ " Free heap=" + heapFree + "/" + THRESH_FREE_HEAP + " LicenseMetrics: " + licenseMetrics);

		String node		= cfg.getProperty(NodeConfiguration.KEY_CTX_PATH); // not null
		Date now 		= new Date();
		
		if ( alertCount > MAX_ALERTS ) { 
			if ( alertCount < (MAX_ALERTS + 3)) {
				Auditor.sendNotification(String.format("Too many alerts sent for node %s. Please contact support.", node));
				alertCount++;
			}
			return;
		}
		
		// Alerts: CPU above 70%, Threads > 150, Free heap < 10MB
		int loadAvg = (int)(cpuLoad * 100); 
		if ( loadAvg > THRESH_CPU) { 
			log.debug("[Notifications] CPU Avg exceeded for " + loadAvg + "/" + THRESH_CPU);
			Auditor.sendNotification(String.format("High CPU usage %d%% for node %s on %s.", loadAvg, node, now ));
			alertCount++;
		}
		
		if ( threadCount > THRESH_TR_COUNT ) {
			log.debug("[Notifications] Thread count exceeded for " + threadCount + "/" + THRESH_TR_COUNT);
			Auditor.sendNotification(String.format("High thread count (%d) for node %s on %s.", threadCount, node, now ));
			alertCount++;
		}
		
		int heapFreeMB = (int)(heapFree / (1024 *1024));
		if ( heapFreeMB < THRESH_FREE_HEAP  ) {
			log.debug("[Notifications] FreeHeap below limit for " + heapFree + "/" + THRESH_FREE_HEAP);
			Auditor.sendNotification(String.format("Heap running low (%dMB) for node %s on %s.", heapFreeMB, node, now ));
			alertCount++;
		}
		
		// 5/20/2017: Check License threshold
		int pct = licenseMetrics.total > 0 ? (int) (((float)licenseMetrics.used / licenseMetrics.total) * 100) : 0;
		if ( pct > THRESH_LIC) {
			log.debug("[Notifications] License threshold exceeded  " + pct + "/" + THRESH_LIC);
			Auditor.sendNotification(String.format("License %% threshold exceeded (%d/%d) for node %s on %s.", pct, THRESH_LIC, node, now ));
			alertCount++;
		}
	}
}
