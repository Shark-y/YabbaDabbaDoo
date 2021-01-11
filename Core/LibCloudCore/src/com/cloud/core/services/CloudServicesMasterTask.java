package com.cloud.core.services;

import com.cloud.core.services.CloudFailOverService;
import com.cloud.core.services.CloudMessageService;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.ServiceHoursScheduler;
import com.cloud.core.cron.CloudCronService;
import com.cloud.core.cron.ErrorNotificationSystem;
import com.cloud.core.cron.LogCleanerService;
import com.cloud.core.profiler.CloudNotificationService;
import com.cloud.core.types.CoreTypes;

/**
 * A Master thread/task is used for task consolidation. It takes care of:
 * <ul>
 * <li> Check/handle the node service hours.
 * <li> Message Dispatch and Activity of the {@link CloudMessageService} used to send messages among modules.
 * <li> All repetitive tasks executed by the node should run here.
 * </ul>
 * <p><b>This thread starts when the node is initialized and must always run, even when the node is down.</b> It shuts down when the container shuts down.</p>
 * 
 * Note: All threads/executors in {@link CloudMessageService} and {@link ServiceHoursScheduler} have been removed.
 * 
 * @version 1.0.1
 * @version 1.0.2 - 5/24/2017: Added {@link CloudCronService} run/tick logic to the master task.
 * 
 * @author VSilva
 */
public class CloudServicesMasterTask {

	/** This matches with the sleep interval (1s) of this master task to produce a default tick period of ~1/minute */
	public static final int DEFAULT_TICK_PERIOD = 60;
	
	/**
	 * Note: this thread uses the CloudServices logger and static objects from {@link CloudServices}.
	 */
    private static Thread masterServicesTask 	= new Thread(new Runnable() {
    	public void run() {
			NodeConfiguration config = CloudServices.getNodeConfig();

			// Loop forever. This thread will be interrupted when a shutdown is requested.
			while ( true ) { 
				/*
				 *  Disabled 4/3/2017 - Tick the Node service hours scheduler
				 */ /*
				if ( CloudServices.sched != null) {
					CloudServices.sched.run();
				} */
				
				/*
				 *  Tick the Cloud sub-services...
				 */
				CloudMessageService.run();
				CloudFailOverService.run();
				CloudNotificationService.run();
				CloudCronService.run();				// 5/24/2017
				
				// 10/18/2018 This one may take a long time 5+ min but runs at midnight 
				ErrorNotificationSystem.run();		
				
				// 02/01/2019 Clean logs once a week.
				LogCleanerService.run(config);
				
				// Wait 1s. It is probably too long for some like the CloudMessageService.
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e) {
					CloudServices.log.debug("CloudServices Master Task interrupted.");
					
					// required to preserve the interrupted status for the parents.
					Thread.currentThread().interrupt();
					break;
				}
			}
			CloudServices.log.debug("CloudServices Master Task terminated.");
		}
	}, "CloudServicesMasterTask-" + CoreTypes.NODE_NAME);

    /**
     * Start the master task. Invoke on container initialization.
     * @since 1.0.0
     */
	static void start () {
		masterServicesTask.start(); 
	}
	
	/**
	 * Stop the master task. This must be called on container shutdown.
	 * @since 1.0.0
	 */
	static void stop () {
		masterServicesTask.interrupt(); 
		try {
			// Wait for the task to finish cleanly.
			masterServicesTask.join(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
