package com.cluster.jsp;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import com.cloud.cluster.CloudCluster;
import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cluster.update.AutoUpdate;
import com.cloud.core.cron.AutoUpdateUtils;
import com.cloud.core.cron.ErrorNotificationSystem;

/**
 * This class off loads logic from the config_cluster.jsp
 * 
 * @author vsilva
 *
 */
public class JSPConfigCluster {

	/**
	 * Controller response.
	 * @author vsilva
	 *
	 */
	public static class StatusResponse {
		public String statusType;		// INFO, ERROR< WARN
		public String statusMessage; 	// Message
		
		public StatusResponse () {
		}
		public StatusResponse(String type, String message) {
			this.statusType = type;
			this.statusMessage = message;
		}
	}
	
	public static void LOGD(final String text) {
		System.out.println("[CFG-CLUSTER-DBG] " +text);
	}

	public static void LOGW(final String text) {
		System.out.println("[CFG-CLUSTER-WRN] " +text);
	}

	public static void LOGE(final String text) {
		System.err.println("[CFG-CLUSTER-ERR] " +text);
	}

	/**
	 * Process controller
	 * @param request HTTp request.
	 * @return  {@link StatusResponse} object.
	 * @throws Exception on any tpe of error.
	 */
	public static StatusResponse execute (HttpServletRequest request) throws Exception {
		final String action			= request.getParameter("action");

		NodeConfiguration cfg		= CloudServices.getNodeConfig();
		StatusResponse response		= new StatusResponse();

		if ( action == null) {
			return null;
		}
		
		// SAVE CFG
		if ( action.equals("save")) {
			final String group = cfg.getProperty(CloudCluster.KEY_NODE_GRP);
			LOGD("Configuratiom Save. Group " + group); // + " Members:" + paramMembers);

			StringBuffer buf 			= new StringBuffer();
			Enumeration<String> names 	= request.getParameterNames();
			
			// loop thru all request params
			while ( names.hasMoreElements()) {
				String name = names.nextElement();
				String val	= request.getParameter(name);
				
				// save them: update stuff goes in a separate file
				if ( !name.equals("action") && val != null && !val.isEmpty()) {
					buf.append(name + " = " + val + " ");
					
					// update system
					if (AutoUpdateUtils.configIsUpdateKey(name)) {
						LOGD("Update Save " + name + " = " + val);
						AutoUpdateUtils.configSetProperty(name, val);
					}
					// Node
					else {
						LOGD("Cluster Save " + name + " = " + val);
						cfg.put(name, val);
					}
				}
			}
			
			// 6/14/2018 This mstuff is included in the error notification
			// Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Saving cluster config: " + buf.toString() );
			
			// node save
			cfg.save();
			
			// update system save
			AutoUpdateUtils.configSave();
			
			boolean clusterChanged = !group.equals(cfg.getProperty(CloudCluster.KEY_NODE_GRP));
			
			response.statusType = "INFO";
			response.statusMessage = "<span id=\"statusMsg\">Configuration saved." 
					+ (clusterChanged ? " <a href=\"javascript:restart_onclick()\">Service restart is required.</a></span>" : "");
		}
		
		if ( action.equals("restart")) {
			LOGD("Cluster restart w/ params: " + cfg.getClusterParams());
			
			Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Cluster restart w/ params " 
					+ cfg.getClusterParams());

			CloudCluster.getInstance().shutdown();
			try {
				CloudCluster.getInstance().initialize(cfg.getClusterParams());
			}
			catch (Exception e) {
				response.statusType 		= "ERROR";
				response.statusMessage	= "Cluster Initialization failed: " + e.toString() 
						+ ". This may occur when other nodes are running in a different mode. Shutdown other nodes and try again later"
						+ ". See the <a target=_new href='../../log/logview.jsp'>log view</a> for details.";
			}
		}
		
		if ( action.equals("start")) {
			LOGD("Cluster start w/ params: " + cfg.getClusterParams());
			try {
				Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Cluster start w/ params " 
						+ cfg.getClusterParams());
				
				CloudCluster.getInstance().initialize(cfg.getClusterParams());
			}
			catch (Exception e) {
				response.statusType 	= "ERROR";
				response.statusMessage	= "Cluster Initialization failed: " + e.toString() + ". Try again later"; 
			}
		}
		
		if ( action.equals("stop")) {
			LOGD("Cluster & services shutdown.");
			
			Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.CLUSTER_LIFECYCLE, "Cluster services shutdown."); 

			CloudServices.stopServices();
			CloudCluster.getInstance().shutdown();
		}
		
		try {
			String resp = null;
			
			// test proactive error notifications
			if ( action.equals("test_pen")) {
				resp = ErrorNotificationSystem.checkForErrorsConsole();
			}
			
			// check for updates.
			if ( action.equals("test_update")) {
				resp = AutoUpdate.checkForUpdates();
			}
			if ( action.startsWith("test_")) {
				response.statusType 	= "INFO";
				response.statusMessage	= resp != null ? resp : "Scan complete."; 
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.statusType 	= "ERROR";
			response.statusMessage	= e.getMessage(); 
		}
		// 1/29/2019 Cluster is disabled: java.lang.NullPointerException 
		// 	 com.cloud.cloud.cluster.CloudCluster.dumpAllInstances(CloudCluster.java:297)
		//	 com.cloud.cluster.jsp.JSPConfigCluster.execute
		//CloudCluster.getInstance().dumpAllInstances("CLUSTER-CFG");
		return response;
	}
}
