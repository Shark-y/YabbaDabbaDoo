package com.cloud.core.services;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import com.cloud.cluster.CloudCluster;
import com.cloud.core.services.CloudExecutorService;
import com.cloud.core.services.CloudFailOverService;
import com.cloud.core.services.CloudMessageService;
import com.cloud.core.services.CloudServicesMasterTask;
import com.cloud.core.services.ClusterTool;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.cron.CloudCronService;
import com.cloud.core.io.FileTool;
import com.cloud.core.license.License;
import com.cloud.core.license.License.LicenseDescriptor;
import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.CyclicBufferAppender;
import com.cloud.core.logging.L4JConfigurator;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;
import com.cloud.core.provider.IServiceFailover;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.security.KeyTool;
import com.cloud.core.services.CloudFailOverService.FailOverType;
import com.cloud.core.services.CloudMessageService.Destination;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.types.OMEvent;
import com.cloud.core.types.OMEvent.EventType;

/**
 * This is the main entry point to all functionality in the cloud services platform.
 * <ul>
 * <li> Contains private references to all services.
 * <li> Controls the service life cycle: initialize, destroy, start and stop.
 * </ul>
 * <h2>Change Log</h2>
 * <ul>
 * <li> 5/30/2017 - v1.0.5 Added logic for the new {@link CloudCronService} in initialize() and destroy().
 * <li> 4/21/2017 - v1.0.4 If the cluster fails to initialize fall back to SINGLE node mode (FailOverType = SERVLET).
 * <li> 4/15/2017 - v1.0.3 nodeConfigurationSaved: Update cluster status (start/stop).
 * <li> 4/03/2017 - v1.0.2 Removed service hours scheduler code: deprecated methods: isOutsideServiceHours, enforceServiceHours.
 * </ul>
 * @author vsilva
 * @version 1.0.5
 *
 */
public class CloudServices {
	
	/*private*/ static final Logger 		log = LogManager.getLogger(CloudServices.class);
	
	/** Global context keys: Location of the adapter configuration */
	public static final String 			CTX_CONFIG_LOCATION		= "CTX_CONFIG_LOCATION";

	/** Key used to add an fatal startup exception to the container session */
	public static final String 			CTX_STARTUP_EXCEPTION	= "CTX_STARTUP_EXCEPTION";

	/* Http response codes */
	public static final int 			SC_OK 			= 200;
	public static final int 			SC_UNAUTHORIZED = 401;
	public static final int 			SC_SERVERERROR 	= 500;
	public static final int 			SC_OFFLINE 		= 503;
	
	/* services */
	private static List<IServiceLifeCycle> services		= new ArrayList<IServiceLifeCycle>();
	
	/* server config */
	/*private*/ static NodeConfiguration 	config;
	
	/* service hrs scheduler*/
	/*private*/ /* disabled 4/3/2017 static ServiceHoursScheduler 	sched; */
	
	/* used to get the status of the server */
	/*private */ static String 				lastError;
	
	private static boolean 				servicesOnline;

	// server license (If required)
	private static LicenseDescriptor license;

	// Valid license?
    private static boolean licensed;

	/**
	 * Shutdown the cluster. If the product doesn't support it then nothing will be done and it will simply return.
	 * If the cluster is disabled it will still shut it down for cleanup purposes.
	 * @param force If true force cluster shutdown. The product must support clustering for this to execute.
	 */
	private static void clusterShutdown (boolean force) {
		if ( !config.isClusterEnabled() && !force) {
			return;
		}
		if ( ! config.productSupportsClustering()) {
			log.info("Cluster Shutdown: Product doesn't support clsutering.");
			return;
		}
		log.info("Cluster Shutdown: Shutting down cluster.");
		CloudCluster.getInstance().shutdown();
	}

	public static void clusterSetLocalMemberStatus (int code, String message) {
		clusterSetLocalMemberStatus(code, message, false); //true);
	}
	
	/**
	 * Set cluster member status.
	 * @param code HTTP status code.
	 * @param message Status message.
	 * @param force If true force setting the status.
	 */
	private static void clusterSetLocalMemberStatus (int code, String message, boolean force) {
		// Null config? Somethis is very wrong!
		if ( config == null) {
			log.error("ClusterSetMemberStatus: Node configuration cannot be null. Something is wrong.");
			return;
		}
		if ( !config.isClusterEnabled() && !force) {
			log.warn("Cluster: Unable to set member status to " + message 
					+ " Cluster enabled: " + config.isClusterEnabled() + " Force Start: " + force );
			return;
		}
		log.info("Cluster: Set local member status: " + code + " " + message); 
		CloudCluster.getInstance().setLocalMemberStatus(code, message); 
	}
	
	/**
	 * Load a class using reflection & create a new instance.
	 * @param clazz Class name (com.acme.MyClass).
	 * @return New instance of the class.
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static Object loadInstance(String clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException  {
		//log.debug("Loading instance for " + clazz);
		return Class.forName(clazz).newInstance();
	}
	
	/**
	 * Load dynamic interfaces
	 * @throws Exception
	 */
	private static void loadInterfaces() throws Exception {
		List<ServiceDescriptor> descriptors =  config.getServiceDescriptors();
		
		log.info("================= Service Interface Loader =====================");
		for (ServiceDescriptor descriptor : descriptors) {
			Object service 	= loadInstance(descriptor.getClassName());
			
			services.add((IServiceLifeCycle)service);
			
			log.info("Service Vendor : " + descriptor.getVendorId() + " " + descriptor.getVendorName());
			log.info("Service Class  : " + descriptor.getClassName());
			log.info("Service Config : " + descriptor.getConfigFileName());
		}
		log.info("===============================================================");
	}

	/**
	 * Invoke this ONLY after the config is loaded!!
	 */
	private static void dumpServerInfo() {
		log.info("================== Server Information =========================");
		log.info("Java Home          " 	+ System.getProperty("java.home"));
		log.info("Java Vendor        " 	+ System.getProperty("java.vendor"));
		log.info("Java Version       " 	+ System.getProperty("java.version"));
		log.info("Config Location    " 	+ config.getConfigLocation());
		log.info("Failover Type      " 	+ config.getFailOverType());
		log.info("Failover Int (ms)  "	+ config.getFailOverInterval());
		log.info("Requires License   "	+ config.requiresLicense());
		log.info("License Type       "	+ config.getLicenseDecryptionKeyResource() + " / " + config.getLicenseKeyType());
		log.info("===============================================================");
	}
	
	private static void loadBootstrapConfig() throws IOException {
		try {
			config = new NodeConfiguration();
		} catch (Exception e) {
			lastError = "Failed to load server configuration " + e.getMessage();
			throw new IOException("Failed to load server configuration", e);
		}
	}
	
	
	/**
	 * Initialize the Audit System
	 * @param config System configuration. See {@link NodeConfiguration}
	 */
	private static void initAuditSystem(NodeConfiguration config) {
		try {
			log.debug("Initializing the Audit System with config. Host: " + config.getProperty(Auditor.KEY_NOTIFY_SMTP_HOST) 
					+ " Port " + config.getProperty(Auditor.KEY_NOTIFY_SMTP_PORT)
					+ " Proto " + config.getProperty(Auditor.KEY_NOTIFY_PROTO)
					);
			Auditor.update(config);
		} catch (IOException e) {
			log.warn("Audit System failed to initialize: " + e.toString());
		}
	}
	
	/**
	 * Initialize the server facade
	 * @param params {@link Properties}. Any Dynamic startup parameters sent by the caller.
	 * @throws IOException
	 */
	public static void initialize(Map<String, Object> params ) throws IOException {
		// load the config
		lastError = null;
		
		//loadBootstrapConfig();
		config = new NodeConfiguration(params);

		// add any startup params to the config.
		config.addParams(params);		

		// Initialize the appender used by the cloud console log viewer. 
		CyclicBufferAppender.initializeCloudConsoleAppender();
		
		initAuditSystem(config);
		
		// Initialize the executor services
		CloudExecutorService.initalize(new Properties());
		
		// configure multi-file logging
		// DEPRECATED - installAssets(config.getConfigLocation());
		
		L4JConfigurator.initializeLoggingSystem();
		
		// Clustering must be always loaded for this FO type? 
		try {
			if ( config.isClusterEnabled() /* config.getFailOverType() == FailOverType.CLUSTER */) {
				ClusterTool.clusterUpdate(config, true );
			} 
		} catch (IOException e) {
			// 4/21/2017 Cluster failed - log error & fall back to SINGLE node.
			log.error("Cluster failed to initalize. Please update the Cloud Console > Node Configuration (under cluster) to get rid of this error.", e);
			log.debug("Cluster initialization failed. Falling back to SINGLE node mode.");
			config.setFailOverType(FailOverType.SERVLET);
		}
		
		// load ALL service interfaces. Catch everything: exceptions and ERRORS!
		try {
			loadInterfaces();
		} 
		catch (Throwable /*Exception*/ e) {
			lastError = "Failed to load service interfaces " + e.getMessage();
			clusterSetLocalMemberStatus(SC_SERVERERROR, lastError, false);
			
			throw new IOException("Failed to load services. " + e.toString(), e);
		}

		// Messaging system
		CloudMessageService.initialize();
		CloudCronService.initialize();		// 5/30/2017
		CloudServicesMasterTask.start();
		
		// Initialize services
		String profile 				= config.getConnectionProfileName();
		
		// add a default cn profile if available.
    	if ( profile != null && !profile.isEmpty()) {
    		if ( !config.getProfileManager().exists(profile)) {
        		log.debug("Initialize: Adding new profile " + profile);
    			config.getProfileManager().add(profile);
    		}
    	}

    	// dump server info...
		dumpServerInfo();
    	
		// publish node info to the cluster (if available)
		clusterUpdateMemberStatus();
		
		initializeServices("NODE-INITIALIZE");

		// 5/22/2020 Plugin system
		PluginSystem.initialize();
		PluginSystem.initPlugins();
		// 8/5/2020 PluginSystem.startPlugins();

		boolean isNodeConfigured 	= isConfigured();

		// abort if node not configured
		if ( ! isNodeConfigured ) {
			log.debug("Initialize: Server MUST be configured!");
			clusterUpdateMemberStatus();
			return;
		}
		
		if ( config.isPrimary()) {
			/* diabled 4/3/2017
			if ( config.isServiceScheduleEnabled()) {
				serviceHoursInitialize();
			}
			else { */
				startServices();	// 24x7
			//}
		}
		else {
			log.info("Initialize: I am NOT a PRIMARY host. Going to sleep zzzz...");

			// service hours?
			/* if ( config.isServiceScheduleEnabled()) {
				serviceHoursInitialize();
			} */

			// Initialize fail-over using cluster APIs.
			if ( isNodeConfigured) {
				initFailover();
			}
		}
		// 8/5/2020 start plugins after services
		//PluginSystem.startPlugins(); 
	}
	
	/**
	 * Initialize loaded services. A connection profile is required. This method may be invoked by
	 * <ul>
	 * <li>The cloud console node config (config_node.jsp) when a new profile is created/activated (on 1st run)
	 * <li> By the main initialization method initialize() when the node is already configured & booting up.
	 * </ul>
	 * @param label Debug label (displayed on stdout).
	 * @throws IOException if there is an error.
	 */
	public static void initializeServices(String label) throws IOException {
		boolean isNodeConfigured 	= isConfigured();
		String profile 				= config.getConnectionProfileName();
		
		log.debug("Initlializing services. " + label + " Current CN Profile:" + profile);
		
		if ( profile == null || profile.isEmpty()) {
			log.warn("Unable to initialize services. No connection profile available.");
			return;
		}
		ServiceContext ctx = new ServiceContext(
				config.getConfigLocation()				// Base cfg path: $user.home/.cloud/CloudAdapter
				,  //profile != null && !profile.isEmpty()
					/*?*/ config.getDefaultProfileBasePath() // $user.home/.cloud/CloudAdapter/Profiles/{PROFILE} 
					//: null
				, services
				, config
				, config.isClusterEnabled() ? CloudCluster.getInstance().getClusterInstance() : null
				, isNodeConfigured
				);
		
		// Initialize services...
		for (IServiceLifeCycle service : services) {
			service.onServiceInitialize(ctx);
		}
	}
	
	/**
	 * Is the node (excluding services) configured?
	 * @return true if the node only is configured (excluding services).
	 */
	public static boolean isNodeConfigured() {
		// Too much log - log.debug("IsNodeConfigured: Checking if a sys admin pwd has been setup.");
		
		// when the sysadmin pwd is NULL we assume the server has not been configured.
		String pwd = config.getSysAdminPwd();
		
		if ( (pwd == null || pwd.isEmpty())) {
			log.debug("IsNodeConfigured: sys admin pwd null or empty. Node NOT configured.");
			return false;
		}
		return true;
	}
	
	/**
	 * Is the whole thing (Node + services) configured?
	 * @return True if the server has been configured already.
	 */
	public static boolean isConfigured() {
		return isConfigured(true);
	}

	/**
	 * Is the whole thing (Node + services) configured?
	 * @param logEnabled If true log debug messages. Set to false to reduce stdout.
	 * @return True if the server has been configured already.
	 */
	public static boolean isConfigured(boolean logEnabled) {
		// Fatal error?
		if ( ! isValidConfig() ) 	return false;

		if ( ! isNodeConfigured()) 	return false;
				
		// loop thru all service descriptors (check if a config exists in the file system)
		List<ServiceDescriptor> descriptors = config.getServiceDescriptors();
		String basePath 					= config.getDefaultProfileBasePath();

		if ( basePath == null || basePath.isEmpty()) {
			log.debug("IsNodeCongigured: Profile base path is null or empty. Node NOT configured.");
			return false;
		}
		if ( logEnabled) {
			log.debug("IsNodeConfigured: sys admin pwd has been setup. Checking for service descriptors @ " + basePath);
		}
		for ( ServiceDescriptor descriptor : descriptors) {
			String filePath = basePath + File.separator + descriptor.getConfigFileName();

			if ( !FileTool.fileExists(filePath)) {
				log.debug("IsNodeConfigured: " + filePath + " NOT found. Node NOT configured.");
				return false;
			}
			else {
				if ( logEnabled) {
					log.debug("IsNodeConfigured: Found descriptor " + filePath);
				}
			}
		}
		if ( logEnabled) {
			log.debug("IsNodeConfigured: Node has been configured.");
		}
		return true;
	}
	
	/**
	 * Failover initialization. There are 2 types of FOs:
	 * 
	 * <li> CLUSTER (better) for non firewalled envs
	 * <li> SERVLET : For fire walls (NO CLUSTERING WILL BE AVAILABLE)
	 */
	private static void initFailover() {
		CloudFailOverService.initialize(config.getFailOverType());
	}
	
	
	/**
	 * Schedule service hours. For example MON-FRI 8AM-5PM (see server.ini)
	 */
	/* disabled 4/3/2017 - never worked  - deprecated
	private static void serviceHoursInitialize() {
		int[] startHHMM = new int[2]; 
		config.getServiceScheduleStartHours(startHHMM);

		int[] stopHHMM = new int[2]; 
		config.getServiceScheduleStopHours(stopHHMM);

		Trigger start = new Trigger("t-start", startHHMM[0], startHHMM[1], new Runnable() {
			public void run() {
				try {
					if ( servicesOnline ) {
						log.debug("ServiceHours-Start: Node already online. Abort.");
						return;
					}
					if ( !config.isServiceScheduleEnabled()) {
						log.debug("ServiceHours-Start: Service hours disabled. Abort.");
						return;
					}
					startServices();
				} catch (Exception e) {
					log.fatal("Server startup failure: ", e);
				}
			}
		});

		Trigger stop = new Trigger("t-stop", stopHHMM[0], stopHHMM[1], new Runnable() {
			public void run() {
				stopServices();
			}
		});
		
		ServiceHoursScheduler.WeekDay[] days = ServiceHoursScheduler.parseDayString(config.getServiceScheduleDays());
		
		log.debug("Service Hours: Initialize " + start.getDesiredTime() + " Til:" + stop.getDesiredTime() + " Days:" + config.getServiceScheduleDays());
		
		if ( sched == null ) {
			sched = new ServiceHoursScheduler();
		}
		else {
			log.warn(String.format("Service Hours: scheduler already exists. Re-Using service hrs: Start: %d %d Stop: %d %d"
					, startHHMM[0], startHHMM[1], stopHHMM[0], stopHHMM[1]));
			sched.stop();
			sched.clear();
		}
		sched.schedule(new ServiceHoursScheduler.TriggerTuple(start, stop, days));
		// FIXME removed on 12/6/2016 for Thread consolidation - sched.start();
		
		if ( isOutsideServiceHours()) {
			Auditor.warn(AuditSource.SERVICE_CORE, AuditVerb.SERVICE_LIFECYCLE, "Start attempt failed: Outside service hours.");
			log.warn("Service Hours: SERVER IS OUTSIDDE SERVICE HOURS.");
			
			clusterUpdateMemberStatus();
		}
	} */
	
	/**
	 * Default start services method. Fires when the START button is pressed @ the console.
	 */
	public static void startServices()  {
		startServices(false);
	}
	
	/**
	 * Start services that can be called by either primary or secondary.
	 * @param force A boolean used to force startup when the SECONDARY wakes up!
	 */
	static void startServices(boolean force)  {
		// Start services...
		try {
			// Invalid license? Don't do anything
			if ( !checkLicense() ) {
				return;
			}
			
			//serviceHoursInitialize(); 
			
			// start the executor service
			CloudExecutorService.start();

			// Initialize & start Replication (secondary only) ....
			if ( !config.isPrimary()) {
				log.debug("StartServices: SECONDARY: Starting FailOver service...");
				initFailover();
			} 
			
			// By default, the SECONDARY is not allowed to start the services unless failover.
			if ( !config.isPrimary()  && !force) {
				log.warn("StartServices: SECONDARY host cannot start Cloud Services!");
				return;
			}

			// Don't start if outside service hours: (PRIMARY or SECONDARY)
			if ( isOutsideServiceHours()) {
				log.warn("StartServices: Can't start services: Outside service hours.");

				Auditor.info(AuditSource.SERVICE_CORE, AuditVerb.SERVICE_LIFECYCLE, "Node is outside service hours.");
				clusterSetLocalMemberStatus(SC_OFFLINE, "Outside service hours.");
				return;
			}
			
			// 10/24/2016 Don't start if service update detected
			ServiceDescriptor[] svcUpdated		= new ServiceDescriptor[1];
			int[] cfgVersions					= new int[2];
			final boolean isServiceCfgUpdated	= CloudServices.serviceUpdateDetected(svcUpdated, cfgVersions);

			if ( isServiceCfgUpdated) {
				String text = "Configuration update detected (New:" + cfgVersions[0] + " Previous:" + cfgVersions[1] + ". Save/Restart required).";				
				log.warn(text);
				clusterSetLocalMemberStatus(SC_OFFLINE, text);
				return;
			}
			
			// double start?
			if ( servicesOnline) {
				log.warn("Services are already online. Attempt to start ignored. Stop services first!");
				return;
			}

			log.debug("Start - Config Base  : " + config.getConfigLocation());
			log.debug("Start - Profile Base : " + config.getDefaultProfileBasePath());
			log.debug("Start - Run Mode     : " + config.getRunMode());

			// Start services
			ServiceContext params = new ServiceContext(
					config.getConfigLocation()				// Base cfg path: $user.home/.cloud/CloudAdapter
					, config.getDefaultProfileBasePath()	// $user.home/.cloud/CloudAdapter/Profiles/{PROFILE}
					, services
					, config
					, config.isClusterEnabled() ? CloudCluster.getInstance().getClusterInstance() : null
					, isConfigured()	// This should will always be true at this point
					);

			Auditor.info(AuditSource.SERVICE_CORE, AuditVerb.SERVICE_LIFECYCLE,"Node starting... Configuration: " + config.getDefaultProfileBasePath()
					+ " Run Mode: " + config.getRunMode() 
					+ " Failover Type: " + config.getFailOverType());
			
			// start services...
			for (IServiceLifeCycle service : services) {
				service.onServiceStart(params);
			}
			
			servicesOnline 	= true;
			lastError		= null;
			
			// 12/25/2020
			PluginSystem.startPlugins(); 
			
			Auditor.info(AuditSource.SERVICE_CORE, AuditVerb.SERVICE_LIFECYCLE, "Node started successfully.");
			
			// Update the node status.
			clusterUpdateMemberStatus();
		} 
		catch (IOException e) {
			lastError = e.getMessage() != null ? e.getMessage() : e.toString();

			log.error("StartServices" , e);
			Auditor.error(AuditSource.SERVICE_CORE, AuditVerb.SERVICE_LIFECYCLE, "Start failure: " + lastError);
			
			clusterSetLocalMemberStatus(SC_SERVERERROR, lastError);
			internalStop();
		}
	}

	static void internalStop () {
		// shutdown webSocket(s) NO payload...
		log.debug("StopServices: Shutting down WebSockets...");
		CloudMessageService.postEvent(new OMEvent(EventType.contextDestoyed, null), Destination.Agent);

		for (IServiceLifeCycle service : services) {
			try {
				service.onServiceStop();
			} catch (Throwable e) {
				log.error("Shutdown service error", e);
			}
		}

		// DO NOT destroy the CloudMessageService. The WS Agent won't receive msgs on restart!!
		//CloudMessageService.destroy();
		Auditor.info(AuditSource.SERVICE_CORE, AuditVerb.SERVICE_LIFECYCLE, "Node has stopped.");
		
		servicesOnline = false;
	}
	
	/**
	 * Stop all services. Fires when the container stops or the STOP button
	 * is pressed @ the console.
	 */
	public static void stopServices() {
		CloudExecutorService.shutdown();
		
		// Don't shutdown the cluster, so error messages can be sent! clusterShutdown();
		internalStop();
		
		//12/25/2020
		PluginSystem.stopPlugins();
		
		clusterSetLocalMemberStatus(SC_OFFLINE, "Offline");

		// required to reload servioce hrs after start/stop
		// disabled 4/3/2017 serviceHoursShutdown();
		
		// FIXME: Always stop the FO service?
		//if ( !config.isPrimary() ) {
			log.debug("StopServices: Shutting down Failover.");
			//ClusterFailOverManager.destroy();
			CloudFailOverService.destroy();
		//}
	}
	
	/**
	 * Destroy Facade. Fires when the container is stopped.
	 */
	public static void destroy() {
		log.debug("=============== Cloud destroy started =================");
		CloudServicesMasterTask.stop();

		stopServices();
		
		for (IServiceLifeCycle service : services) {
			try {
				service.onServiceDestroy();
			} catch (Throwable e) {
				log.error("Service destroy error.", e);
			}
		}
		// 7/31/2020
		PluginSystem.shutdown();
		
		// Force shutdown to clean any background task.
		clusterShutdown(true); //false); 
		// disabled 4/3/2017 serviceHoursShutdown();
		
		CloudMessageService.destroy();
		CloudExecutorService.destroy();
		CloudCronService.destroy();			// 5/30/2017
		
		Auditor.destroy();

		log.debug("=============== Cloud destroy complete ================");
	}
	
	/* removed/disabled 4/3/2017
	private static void serviceHoursShutdown () {
		if ( config.isServiceScheduleEnabled() && sched != null) {
			log.debug("Service Hours: Shutdown - Removing service hours scheduler.");
			sched.stop();
			//sched = null;
		}
	}*/
	
	/**
	 * Invoked from the cloud console when the {@link NodeConfiguration} save button is pressed.
	 * @throws Exception 
	 */
	public static void nodeConfigurationSaved() throws Exception {
		log.debug("Node configuration saved invoked.");
		config.save();
		
		/* refresh service hrs scheduler disabled 4/3/2017
		serviceHoursShutdown();
		serviceHoursInitialize(); */
		
		// 4/15/2017 Update cluster
		// 5/9/2017 Only for cluster apps: See http://acme208.acme.com:6091/issue/CLOUD_CORE-63
		//if ( config.isClusterEnabled() /* 12/27/2018 getFailOverType() == FailOverType.CLUSTER*/) {
		if ( config.productSupportsClustering()) {	// 2/18/2019 - Some products don't pack LibCloudCluster.jar
			ClusterTool.clusterUpdate(config, false);
		}
		clearLastError();
	}
	
	/**
	 * Used to query the status of the server
	 * @return If != NULL then an errors has occurred & the server is offline
	 */
	public static String getLastError() {
		return lastError;
	}

	/**
	 * Clear the last error. Invoke after a successful save.
	 */
	public static void clearLastError() {
		lastError = null;
	}
	
	/**
	 * Returns true if the node online. <b>Note: The node may be online but the internal service have failed.</b>
	 * @deprecated This name is misleading. Use isNodeOnline instead.
	 * @return True if the node is online.
	 */
	public static boolean servicesOnline() {
		return servicesOnline;
	}

	/**
	 * Returns true if the node online. <b>Note: The node may be online but the internal service have failed.</b>
	 * @return True if the node is online.
	 */
	public static boolean isNodeOnline() {
		return servicesOnline;
	}

	/**
	 * Returns the status of all inner services. <b>Note: The node may be online but any internal service may have failed.</b>
	 * @return A Map<ServiceDescriptor, ServiceStatus> that has the ({@link ServiceDescriptor}, {@link ServiceStatus}) for all inner services.
	 * @since 1.0.2
	 */
	public static Map<ServiceDescriptor, ServiceStatus> getServiceStatuses() {
		List <IServiceLifeCycle> services 		= getServices();
		List<ServiceDescriptor> descriptors 	= config.getServiceDescriptors();
		Map<ServiceDescriptor, ServiceStatus> statuses = new HashMap<ServiceDescriptor, ServiceStatus>();
		
		for (IServiceLifeCycle service : services) {
			if ( service.getServiceStatus() != null ) {
				for ( ServiceDescriptor descriptor : descriptors) {
					if ( descriptor.getClassName().equals(service.getClass().getName())) {
						statuses.put(descriptor, service.getServiceStatus());
					}
				}
			}
		}
		return statuses;
	}

	/**
	 * Get the bootstrap adapter configuration. 
	 * @return Main server configuration (bootstart.ini). {@link NodeConfiguration}.
	 */
	public static NodeConfiguration getNodeConfig() {
		// this will happen when this code is invoked by the cluster manager.
		if ( config == null) {
			log.warn("Invoking get adapter config with a NULL configuration. Loading new!");
			try {
				loadBootstrapConfig();
			} 
			catch (IOException e) {
				log.error("LoadServerCfg: " + lastError);
			}
		}
		return config;
	}
	
	/**
	 * Get a {@link ServiceConfiguration} to the chat service configuration.
	 * This is used by the UI admin console to configure the {@link IMessageBroker}. 
	 * @deprecated Kept for compatibility reasons only.
	 * @return A {@link ServiceConfiguration} object used by the UI to configure the backend.
	 */
	public static ServiceConfiguration getMessageBrokerConfig() {
		try {
			String basePath = config.getDefaultProfileBasePath(); 
			log.debug("Get MessageBroker Config Base: " + basePath + " File: " + config.getMessageBrokerConfig());
			
			return new ServiceConfiguration(basePath, config.getMessageBrokerConfig(), ServiceConfiguration.CONFIG_KEY_SERVICE_CHAT);
		} catch (Exception e) {
			log.error("Unable to load chat service config: " + e.toString());
			return null;
		}
	}

	/**
	 * Get a {@link ServiceConfiguration} to the {@link IContactCenter} configuration.
	 * @deprecated Kept for compatibility reasons only.
	 * @return A {@link ServiceConfiguration} with GUI meta-data info used by the admin console.
	 */
	public static ServiceConfiguration getContactCenterConfig() {
		try {
			String basePath = config.getDefaultProfileBasePath(); 
			log.debug("Get ContactCenter Config Base: " + basePath + " File: " + config.getContactCenterConfig());
			
			return new ServiceConfiguration(basePath, config.getContactCenterConfig() , ServiceConfiguration.CONFIG_KEY_SERVICE_OM);
		} catch (Exception e) {
			log.error("Unable to load Open Media config: " + e.toString());
			return null;
		}
	}

	public static ServiceConfiguration getServiceConfig(ServiceType type) {
		try {
			String basePath 		= config.getDefaultProfileBasePath(); 
			ServiceDescriptor desc 	= config.findServiceDescriptor(type);
			
			log.debug("Get " + type + " Config Base: " + basePath + " File: " + desc.getConfigFileName());
			
			return new ServiceConfiguration(basePath, desc.getConfigFileName() , type.name());
		} catch (Exception e) {
			log.error("Unable to load " + type + " service config ", e);
			return null;
		}
	}

	/**
	 * Get a {@link ServiceConfiguration} by id.
	 * @param id Service id (service_vendorId from the product ini file)
	 * @return See {@link ServiceConfiguration}.
	 */
	public static ServiceConfiguration getServiceConfig (final String id) {
		try {
			String basePath 		= config.getDefaultProfileBasePath(); 
			ServiceDescriptor desc 	= config.findServiceDescriptor(id);
			
			log.debug("Get service ID " + id + " Config Base: " + basePath + " File: " + desc.getConfigFileName());
			
			return new ServiceConfiguration(basePath, desc.getConfigFileName() , desc.getType().name());
		} catch (Exception e) {
			log.error("Unable to load service with id " + id + " config: " + e.toString());
			return null;
		}
	}
	
	/**
	 * Outside service hours?
	 * @deprecated 4/3/2017 Service hours have been disabled. This method will always return false. It is kept for console compatibility reasons only (to be removed soon).
	 * @return True if outside service hours. Note: the enforce service hours flag must be true.
	 */
	public static boolean isOutsideServiceHours() {
		return false;
		/* disabled 4/3/2017  if ( ! enforceServiceHours())
			return false;
		
		return sched != null ? sched.isOutsideServiceHours() : false; */
	}
	
	/**
	 * Enforce service hours.
	 * @deprecated 4/3/2017 Service hours have been disabled and this sub will always return false. It will be removed soon.
	 * @return
	 */
	public static boolean enforceServiceHours() {
		return false; // deprecated 4/3/2017 - to be removed - config.isServiceScheduleEnabled();
	}
	
	/**
	 * Save one or more configurations. Fires when the "Save" button is pressed in the admin console.
	 * @deprecated Kept for compatibility reasons only.
	 * @param save if not null, save the {@link NodeConfiguration}.
	 * @param mbCfg if not null save the {@link IMessageBroker} configuration.
	 * @param ccCfg if not null save the {@link IContactCenter} configuration.
	 * @throws Exception
	 */
	public static void saveConfigs (NodeConfiguration server, ServiceConfiguration mbCfg, ServiceConfiguration ccCfg) throws Exception {
		if ( server != null) {
			server.save();
			server.validate();
		}
		if ( mbCfg != null )	mbCfg.save();
		if ( ccCfg != null)		ccCfg.save();
		
		clusterUpdateMemberStatus();
	}
	
	/**
	 * update the member current status.
	 */
	public static void clusterUpdateMemberStatus () {
		// default values.
		clusterUpdateMemberStatus (SC_OFFLINE, "Offline");
	}
	
	/**
	 * Update the cluster member information based on the status of the services.
	 * @param code HTTP status code.
	 * @param status Node status message.
	 */
	public static void clusterUpdateMemberStatus (int code, String status) {
		if ( !config.isClusterEnabled()) {
			return;
		}
		
		log.debug("ClusterSetMemberStatus ENTER");
		
		if (! isConfigured() ) { 
			status 	= "Node must be configured.";
		}
		else if ( isOutsideServiceHours() ) { 
			status 	=  "Outside service hours.";
			//code 	=	SC_OK;
		}
		else if ( servicesOnline() ) {
			status 	= "Online";
			code 	= SC_OK;
		}

		log.debug("ClusterSetMemberStatus Status : " + status + " Code: " + code + " Vendor(s): " + config.getVendorNames() );
		log.debug("ClusterSetMemberStatus RunMode: " + config.getRunMode().name() + " Profile: " + config.getConnectionProfileName());
		
		clusterSetLocalMemberStatus(code, status);

		// Update extra cluster info: Run mode + cn profile.
		CloudCluster cluster = CloudCluster.getInstance();
		cluster.setLocalMemberAttribute(CloudCluster.KEY_RUNMODE, config.getRunMode().name());
		cluster.setLocalMemberAttribute(CloudCluster.KEY_CNPROFILE, config.getConnectionProfileName());
		
		// For some reason this fails to compile using the automated build :( - AdapterCluster.KEY_VENDOR
		cluster.setLocalMemberAttribute("vendor" /*AdapterCluster.KEY_VENDOR*/, config.getVendorNames() );
	}
	
	
	/**
	 * Find service by {@link ServiceType}.
	 * @param type See the {@link ServiceType}.
	 * @return A {@link IServiceLifeCycle}.
	 */
	public static IServiceLifeCycle findService(ServiceType type) {
		for (IServiceLifeCycle service : services) {
			if ( service.getServiceType() == type ) {
				return service;
			}
		}
		return null;
	}

	public static ServiceDescriptor findServiceDescriptor (ServiceType type) {
		return config.findServiceDescriptor(type);
	}

	public static ServiceDescriptor findServiceDescriptor (String id) {
		return config.findServiceDescriptor(id);
	}

	/**
	 * Load the {@link LicenseDescriptor} specified in {@link ConfigServer}.
	 * @param force If true force a license reload.
	 */
	private static /*LicenseDescriptor*/ void loadLicenceDescriptor (boolean force) {
		try {
			if  ( force || license == null  ) {
				String keyResource 	= config.getLicenseDecryptionKeyResource();
				String keyString	= config.getLicense();
				Key key 			= KeyTool.readKeyFromFile(keyResource, KeyTool.parseKeyType(config.getLicenseKeyType())); 

				license				= License.describe(key, keyString);
			}
			//return license;
		}
		catch (Exception e) {
			log.error("Load license:" + e.toString());
		}
	}

    public static boolean checkLicense() throws IOException  {
    	return checkLicense(true, true);
    }

	/**
	 * Check the license. The server will NOT start if failed. Notes:
	 * <ul>
	 * <li> In dev mode, license checks are ignored.
	 * <li> Not all service implementations require a license.
	 * </ul>
	 * @param reloadLicFromConfig If true always reload and verify the license from the {@link NodeConfiguration}.
     * @param dumpLicInfo If true dump the license information to the node log.
     * 
	 * @return If true then proceed with execution. If false license failed. Stop.
	 */
    private static boolean checkLicense(boolean reloadLicFromConfig, boolean dumpLicInfo) throws IOException {
    	if ( !config.requiresLicense()) {
    		return true;
    	}
    	
		// 12 19/2018 Devmode backdoor removed if ( config.isLicenseCheckEnabled()) {
	    	long t0 = System.currentTimeMillis();

			// clear all errors & force license reload
			License.clearLastError();
			lastError 	= null;
			
			loadLicenceDescriptor(reloadLicFromConfig); // true); 
			
			if ( license == null) {
				licensed 	= false;
				lastError 	= "Invalid License: " + License.getLastError();
				
				log.error("Load License: " + lastError);
				//throw new IOException(lastError); PROVIDER MUST BE LOADED!!
			}
			else {
				// dump it...
				if ( dumpLicInfo) {
					// expensive I/O
					Key key 	= KeyTool.readKeyFromFile(config.getLicenseDecryptionKeyResource(), KeyTool.parseKeyType(config.getLicenseKeyType()));
					License.dump(key, license); 
				}
				// verify...
				if ( ! License.verify(license , config.getLicenseProductId()) ) {
					licensed = false;
					lastError = License.getLastError();
					
					log.error("License Verification Error:" + lastError);
				}
				else {
			    	long t1 = System.currentTimeMillis();

					licensed = true;
					License.clearLastError();
					
					if ( (t1 - t0) > 0 ) {
						log.debug("License check OK in " + (t1 - t0) + " ms.");
					}
				}
			}
		/* }
		else {
			// in dev mode...
			log.warn("License check is disabled! Runing in developer mode?");
			licensed = true;
		} */
		return licensed;
    }

    /**
     * Server licensed?
     * @return
     */
    public static boolean isLicensed() {
    	/**
    	 * Always re check. In case the invocation comes from the rest API
    	 * fix for http://acme208.acme.com:6091/issue/UNIFIED_CC-87
    	 */
    	try {
			checkLicense(false, true);
		} 
    	catch (IOException e) {
    		log.error("Check licence error", e);
		}
    	return licensed;
    }

	/**
	 * Get the server license.
	 * @param forceReload If true, reload the license from disk.
	 * @return {@link LicenseDescriptor}.
	 */
	public static LicenseDescriptor getLicenseDescriptor (boolean forceReload) {
		loadLicenceDescriptor(forceReload);
		return license;
	}

	/**
	 * @return True if the server requires a license and the dev mode key is set in the config.
	 */
	/* 12/19/2018 devmode backdoor removed
    public static boolean isInDeveloperMode() {
    	// config null? BAD! A fatal error probably occurred!
    	if ( ! isValidConfig() ) {
    		return false;
    	}
    	return !config.isLicenseCheckEnabled();
    } */

    public static List<IServiceLifeCycle> getServices() {
    	return services;
    }
    
    private static boolean isValidConfig() {
    	if ( config == null) {
    		log.error("CloudServices: Method invoked with a NULL configuration. This is bad.");
    		return false;
    	}
    	return true;
    }
    
    /**
     * Check if a service has been updated by checking its version in the class path against the version in the file system.
     * @param descriptors A RESPONSE array of SIZE 1 of the {@link ServiceDescriptor} that has been updated.
     * @param versions Configuration versions: version[0] = CURRENT/CLASS PATH, version[1] = PREVIOUS/FILE SYSTEM.
     * @return True if CP version > FS version for the first available service.
     */
    public static boolean serviceUpdateDetected (ServiceDescriptor[] descriptors, int[] versions) {
    	for (IServiceLifeCycle service : services) {
    		ServiceConfiguration cfg = getServiceConfig(service.getServiceType());
    		
    		if (cfg.getConfiguration().versionUpdateDetected() ) {
    			descriptors[0]	= findServiceDescriptor(service.getServiceType());
    			versions[0]		= cfg.getConfiguration().getVersion();
    			versions[1]		= cfg.getConfiguration().getPreviousVersion();
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Get a list of failed services from a list of service statuses. Use along with getServiceStatuses to figure out which ones have failed.
     * @param statuses Map<ServiceDescriptor, ServiceStatus> of all available services
     * @return List of failed services. Use along with getServiceStatuses.
     * @since 1.0.2
     */
	public static Map<ServiceDescriptor, ServiceStatus> getFailedServices (Map<ServiceDescriptor, ServiceStatus> statuses) {
		Map<ServiceDescriptor, ServiceStatus> failed = new HashMap<ServiceDescriptor, ServiceStatus>();
		Set<Entry<ServiceDescriptor, ServiceStatus>> entries = statuses.entrySet();
		for (Entry<ServiceDescriptor, ServiceStatus> entry : entries) {
			ServiceStatus status = entry.getValue();
			
			// 9/2/2019 Better handling of SERVICE_ERROR and STARTED_WITH_ERRORS
			if ( status != null &&  status.getStatus().name().contains("ERROR") /*== Status.SERVICE_ERROR*/) {
				failed.put(entry.getKey(), status);
			}
		}
		return failed;
	}

    /**
     * Get a list of failed services from a list of service statuses. Use along with getServiceStatuses to figure out which ones have failed.
     * @return List of failed services.
     * @since 1.0.2
     */
	public static Map<ServiceDescriptor, ServiceStatus> getFailedServices () {
		return getFailedServices(getServiceStatuses());
	}

	/**
	 * @return True if the product supports a license (if the node config contains the key server_license)
	 */
	public static boolean supportsLicense () {
		boolean bool = config.containsKey(NodeConfiguration.KEY_SERVER_LIC);
		return bool;
	}
	
	/**
	 * Failover all running services that implement {@link IServiceFailover}.
	 * @return A Map of failed services indexed by {@link ServiceDescriptor} and {@link ServiceStatus}.
	 */
	public static void /*Map<ServiceDescriptor, ServiceStatus> */ failOverServices () {
		for (IServiceLifeCycle service : services) {
			try {
				if ( service instanceof IServiceFailover) {
					((IServiceFailover)service).onFailover();
				}
			} catch (Throwable e) {
				log.error("Failover service error", e);
			}
		}
		//return getServiceStatuses(Status.ON_LINE);
	}

	/**
	 * Returns the status of all inner services by filter. <b>Note: The node may be online but any internal service may have failed.</b>
	 * @param desired The desired status filter.
	 * @return A Map<ServiceDescriptor, ServiceStatus> that has the ({@link ServiceDescriptor}, {@link ServiceStatus}) for all inner services.
	 * @since 1.0.3
	 */
	public static Map<ServiceDescriptor, ServiceStatus> getServiceStatuses(ServiceStatus.Status desired) {
		List <IServiceLifeCycle> services 		= getServices();
		List<ServiceDescriptor> descriptors 	= config.getServiceDescriptors();
		Map<ServiceDescriptor, ServiceStatus> statuses = new HashMap<ServiceDescriptor, ServiceStatus>();
		
		for (IServiceLifeCycle service : services) {
			if ( service.getServiceStatus() != null ) {
				for ( ServiceDescriptor descriptor : descriptors) {
					if ( descriptor.getClassName().equals(service.getClass().getName())) {
						if ( service.getServiceStatus().getStatus() == desired) {
							statuses.put(descriptor, service.getServiceStatus());
						}
					}
				}
			}
		}
		return statuses;
	}
	
}
