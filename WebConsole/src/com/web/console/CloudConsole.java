package com.web.console;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.cluster.CloudCluster;
import com.cloud.core.services.CloudExecutorService;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.CloudFailOverService.FailOverType;

/**
 * This class is the entry point to all login in the {@link CloudConsole}.
 * 
 * @author VSilva
 *
 */
public class CloudConsole {

	private static final Logger 		log 			= Logger.getLogger(CloudConsole.class);
	
	// CONFIGURATION KEYS
	
	// service end points
	public static final String 			KEY_EPS  		= "KEY_NODE_EPS";
	
	
	/** Node config */
	private static NodeConfiguration 	config;

	
	/**
	 * Invoke once to initialize the web app:
	 * <li> Initialize logging
	 * <li> Load the {@link NodeConfiguration}
	 * <li> start the cluster
	 * <li> Start the polling: pro-active error notifications, auto updates, etc.
	 * @param params
	 * @throws IOException
	 */
	public static void initialize(Map<String, Object> params) throws IOException {
		CloudServices.initialize(params);
		config		= CloudServices.getNodeConfig();
		
		log.debug("Cloud Console init CFG: " + config);
		
		// add any startup params to the config. Just in case there is no ClusterMananger.ini @ the FS.
		config.addParams(params);		

	}
	
	
	/**
	 * Shutdown the web app.
	 */
	public static void destroy () {
		log.debug("Cloud Console destroy.");
		CloudExecutorService.destroy();
		CloudServices.destroy();
	}
	
	
	public static NodeConfiguration getNodeConfig() {
		return config;
	}
	
	public static String getClusterGroupName() {
		return config.getFailOverType() == FailOverType.SERVLET ? null : CloudCluster.getInstance().getClusterGroupName();
	}
	
}
