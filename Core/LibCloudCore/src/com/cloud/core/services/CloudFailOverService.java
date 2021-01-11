package com.cloud.core.services;


import com.cloud.core.services.CloudFailOverService;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.failover.HotStandbyWithNotification;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Master Fail over service manager. There are 2 types of fail overs:
 * 
 * <ul>
 * <li>CLUSTER: For non fire-walled hosts (better). Use it if possible.
 * <li>SERVLET: a Servlet based fall back FO for fire-walled hosts. (OBSOLETE - NEVER IMPLEMENTED. Don't use).
 * </ul>
 * 
 * If the FO type is SERVLET the node will run with NO clustering and there won't be any fail over.
 * 
 * <<h2>Change Log</h2>
 * <ul>
 * <li>1/1/2017 Removed obsolete servlet fail over code and other garbage.
 * <li>1/12/2016 v.1.0.1 Began a proper cluster fail over implementation.
 * <li>?/?/2016 Original implementation full of bugs.
 * </ul>
 * @author VSilva
 * @version 1.0.1
 *
 */
public class CloudFailOverService {
	
	private static final Logger 		log = LogManager.getLogger(CloudFailOverService.class);
	
	/** 
	 * Fail-over types
	 * 
	 * <li> SERVLET: Fire wall friendly (NEVER WORKED - DON'T USE).
	 * <li> CLUSTER HAZELCAST: Bulky, slow - deprecated.
	 * <li> CLUSTER ZEROFONF: A simple multicast service discovery and cluster solution.
	 * 
	 * @author VSilva
	 *
	 */
	public enum FailOverType {SERVLET, CLUSTER_HAZELCAST, CLUSTER_ZEROCONF};
	
	
	static void initialize(FailOverType type) {
		/*
		if ( type == FailOverType.CLUSTER) {
			// TODO initClusterFailover();
		} */
		/* OBSOLETE
		else {
			initServletReplication();
		} */
	}
	
	/**
	 * Main sub
	 */
	static void run () {
		try {
			HotStandbyWithNotification.run();
		} catch (Exception e) {
			log.error("CloudFailover error", e);
		}
	}
	

	/**
	 * FO service running?
	 * @return true if running.
	 */
	/*
	public static boolean isRunning() {
		FailOverType type 	= CloudServices.getNodeConfig().getFailOverType();
		boolean running 	= type == FailOverType.CLUSTER 
				? true 	// TODO ClusterFailOverManager.isRunning()
				: false; //ServletFailOverManager.isRunning();
				
		log.debug("Cloud Failover of type " + type + " Running: " + running);
		return running;
	} */

	/**
	 * Stop the fail-over service. Invoke on container shutdown.
	 */
	static void destroy () {
		NodeConfiguration conf 	= CloudServices.getNodeConfig();
		/*
		 *  CentOS7 fix : SEVERE: Exception sending context destroyed event to listener instance of class com.rts.server.CloudNodeWebListener
		 *  java.lang.NullPointerException
		 *  at com.cloud.core.services.CloudFailOverService.destroy(CloudFailOverService.java:221)
		 */
		if ( conf == null ) {
			return;
		}
		FailOverType type 		= conf.getFailOverType();
		log.debug("Failover destroy type " + type);
		
		/*
		if ( type == FailOverType.CLUSTER) {
			// TODO ClusterFailOverManager.destroy();
		} */
		/* NEVER WORKED. OBSOLETE
		else {
			ServletFailOverManager.destroy();
		} */
	}
}
