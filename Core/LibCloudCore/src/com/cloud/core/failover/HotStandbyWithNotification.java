package com.cloud.core.failover;

import java.util.Date;
import java.util.Map;

import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;
import com.cloud.core.services.CloudFailOverService;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;

/**
 * Hot standby with notification fail over implementation.
 * <ol>
 * <li> All nodes are online and connected.
 * <li> When a service fails for node A, the node stops and enters auto recovery. A notification is sent to the sys admin.
 * <li> In auto recovery the node attempts to start at a given interval.
 * <li> If the node successfully starts after an attempt it goes off auto recovery & notifies the sys admin it is back online.
 * <li> If the node fails to start then the sequence repeats.
 * <li> Requires notifications to be enabled.
 * </ol>
 * @author VSilva
 * @version 1.0.0 - Initial commit.
 * @version 1.0.1 - 10/21/2017 New auto recovery mode (plain) - auto recover without notifications.
 * @version 1.0.2 - 11/07/2017 Trigger recovery when node goes offline too.
 * @version 1.0.3 - 06/10/2020 Added fail-over functionality.
 */
public class HotStandbyWithNotification {

	private static final Logger log = LogManager.getLogger(HotStandbyWithNotification.class);
			
	// used to count ticks
	private static int tickCount;
	
	// if true attempt to recover
	private static boolean inAutoRecovery;

	/**
	 * This will be called by the {@link CloudFailOverService} run method.
	 * @throws Exception
	 */
	public static void run () throws Exception {
		// about once/min
		if ( (tickCount++ % 60) != 0 || tickCount <= 2) {
			return;
		}
		
		if ( !CloudServices.isConfigured(false)) {
			return;
		}
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		
		// notification proto: if "none" then failover disabled.
		// 10/21/2017 - "plain" means auto recover without any notifications
		String proto 			= cfg.getProperty(Auditor.KEY_NOTIFY_PROTO);
		
		if ( proto == null || proto.equalsIgnoreCase(Auditor.KEY_PROTO_DISABLED)) {
			return;
		}
		
		// 1. Get a list of failed services: A failed service contains 'ERR' in its status
		Map<ServiceDescriptor, ServiceStatus> statuses = CloudServices.getFailedServices();
		
		// 2. all ok? Return if the node in online, if the node is offline auto recover in the next tick.
		if ( statuses.size() == 0 ) {
			// 11/07/2017 Trigger recovery when the node if offline too
			if ( CloudServices.isNodeOnline()) {	
				inAutoRecovery 	= false;
				tickCount		= 0;
				return;
			}
			else {
				// required to get the vendor
				statuses = CloudServices.getServiceStatuses(); 
			}
		}
		// 3. something failed: There are some failed services OR the node is offline
		log.debug("[FAIL-OVER] Tick " + tickCount + " In AutoRecovery=" + inAutoRecovery + " Services Statuses: " + statuses);

		String vendor 	= statuses.entrySet().iterator().next().getKey().getVendorName();
		String node		= cfg.getProperty(NodeConfiguration.KEY_CTX_PATH); // not null
		Date now 		= new Date();
		
		// 4. First tick (inAutoRecovery == false), stop and optionally notify
		if ( !inAutoRecovery ) {
			// Notify & stop
			final String msg = String.format("Service %s has failed for node %s on %s.", vendor, node, now );
			
			if ( !proto.equals(Auditor.KEY_PROTO_PLAIN)) {
				Auditor.sendNotification(msg);
			}
			else {
				Auditor.warn(AuditSource.SERVICE_VENDOR, AuditVerb.SERVICE_LIFECYCLE, msg);
			}

			inAutoRecovery = true;
			
			// 4.1 Node offline? Stop services, don't failover
			if ( CloudServices.getFailedServices().size() == 0) {
//				CloudServices.stopServices();
//				inAutoRecovery = true;
				log.debug("[FAIL-OVER] Node appears OFFLINE. Starting services.");
				CloudServices.startServices();
				
				// start ok, reset
				if ( CloudServices.getFailedServices().size() == 0 ) {
					inAutoRecovery = false;
				}
			}
			else {
				// 4.2 Fail-over to secondary, if successful reset
				log.debug("[FAIL-OVER] FAILOVER Started...");
				CloudServices.failOverServices();
				
				Map<ServiceDescriptor, ServiceStatus> offline 	= CloudServices.getServiceStatuses(Status.OFF_LINE);
				Map<ServiceDescriptor, ServiceStatus> failed 	= CloudServices.getFailedServices();
				
				log.debug("[FAIL-OVER] Result: Services  OFFLINE=" + offline + " FAILED:" + failed);
				
				if ( offline.size() == 0 && failed.size() == 0) {
					inAutoRecovery 	= false;
					tickCount		= 0;
					log.debug("[FAIL-OVER] SUCCEDDED: Resetting state.");
				} 
			}
		}
		else {
			// 5. Next tick (inAutoRecovery == true) - start and optionally notify
			CloudServices.startServices();
			
			if ( CloudServices.isNodeOnline() && CloudServices.getFailedServices().size() == 0 ) {
				final String msg = String.format("Service %s is back ONLINE for node %s on %s.", vendor, node, now );
				
				if ( !proto.equals(Auditor.KEY_PROTO_PLAIN)) {
					Auditor.sendNotification(msg);
				}
				else {
					Auditor.info(AuditSource.SERVICE_VENDOR, AuditVerb.SERVICE_LIFECYCLE, msg);
				}
			}
			else {
				//Auditor.sendNotification(String.format("Service %s failed to recover for node %s on %s.", vendor, node, now ));
				// Failed to start - still in recovery mode ((inAutoRecovery == true)
				CloudServices.stopServices();
			}
		}
	}

}
