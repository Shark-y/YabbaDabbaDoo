package com.cloud.core.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.cloud.core.services.CloudFailOverService;
import com.cloud.core.services.ProfileManager;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.config.IConfiguration;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.Container;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.EncryptionTool;
import com.cloud.core.services.CloudFailOverService.FailOverType;
import com.cloud.core.services.ServiceDescriptor.ServiceType;

/**
 * Node Configuration descriptor, this class extends {@link Properties}. NOT to be confused with {@link IConfiguration}:
 * <ul>
 * <li>It is always a {@link Properties} hash map object. {@link IConfiguration} can be anything JSON, XML, etc.
 * <li>It is always read from the container class path (/bootstrap/bootstrap.ini). 
 * 	{@link IConfiguration} is read from the file system, then the class-path.
 * <li>It reads the {@link IMessageBroker} class path descriptor /configuration/message_broker.ini
 * <li>It reads the {@link IContactCenter} class path descriptor /configuration/contact_center.ini
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.2 - 06/06/2017 - New property: supports clustering.
 * @version 1.0.3 - 09/04/2017 - New log  keys LOG_PATH, LOG_ROTATION_POL
 * @version 1.0.4 - 12/19/2017 - New log  keys LOG_MASK_REGEXP LOG_MASK
 */
public class NodeConfiguration extends Properties {
	private static final Logger log 				= LogManager.getLogger(NodeConfiguration.class);
	
	private static final long serialVersionUID 		= -8464097803611059386L;

	private static void LOGD(String text) {
		System.out.println("[SRV-CFG] " + text);
	}

	private static void LOGW(String text) {
		System.out.println("[SRV-CFG] " + text);
	}

	/** server run modes */
	public enum RunMode { PRIMARY, SECONDARY, CLUSTER};
	
	/** Session key: Server configuration */
	public static final String SKEY_CFG_SERVER 		= "config_srv";
	
	/** Session key: user logged in */
	public static final String SKEY_LOGGED_IN 		= "logged_in";

	/** Server configuration class-path resource */
	private static final String RESOURCE 			= "/bootstrap/bootstrap.ini"; 
	
	/** Cloud Adapter (Compound) Services */
	private static final String DESCRIPTOR_BROKER 	= "/configuration/message_broker.ini";
	private static final String DESCRIPTOR_CC 		= "/configuration/contact_center.ini";

	/** cloud connector */
	private static final String DESCRIPTOR_CONNECTOR= "/configuration/call_center.ini";

	/** Other service descriptors */
	private static final String DESCRIPTOR_VR 		= "/configuration/voice_recording.ini";
	private static final String DESCRIPTOR_LIC 		= "/configuration/licensing.ini";
	private static final String DESCRIPTOR_DAEMON 	= "/configuration/daemon.ini";
	
	/** License information keys */
	public static final String KEY_SERVER_LIC 		= "server_license";
	public static final String KEY_PRODUCT_ID 		= "server_license.productId";
	public static final String KEY_LICKEY_LOCATION 	= "server_license.key.classpath";
	public static final String KEY_LICKEY_TYPE 		= "server_license.key.type";
	
	/** Developer mode keys */
	public static final String KEY_DEV_MODE			= "server.dev.mode";
	public static final String KEY_DEV_XDOMAIN		= "server.dev.xdomain";
	
	/** Config: Build number */
	public static final String KEY_SERVER_VER		= "server_buildNumber";

	/** config location */
	public static final String KEY_CONFIG_PATH 		= "server_configPath";

	public static final String KEY_SSHED_ENABLED 	= "server_serviceScheduleEnabled";
	public static final String KEY_SSHED_START 		= "server_serviceScheduleStartHHMM";
	public static final String KEY_SSHED_STOP		= "server_serviceScheduleStopHHMM";
	public static final String KEY_SSHED_DAYS 		= "server_serviceScheduleDays";

	/** failover mode :PRIMARY, SECONDARY, CLUSTER */
	public static final String KEY_RUN_MODE 		= "server_runMode";	
	
	/** Primary HOST if FO type is SERVLET */
	public static final String KEY_PRIMARY 			= "server_primary";
	
	/** fail-over interval (ms) */
	public static final String KEY_FAILOVER_INT 	= "server_failOverInterval";
	
	/** FO Type: SERVLET (non fire-walls NO CLUSTERING AT ALL), CLUSTER */
	public static final String KEY_FAILOVER_TYPE 	= "server_failOverType";
	
	/** [4/22/2017 For Cluster Type DataStore] Cluster Data Store Endpoint URL */
	public static final String KEY_CLUSTER_DSURL	= "server_cluster_DataStoreUrl";
	public static final String KEY_CLUSTER_DSUSER	= "server_cluster_DataStoreUser";
	public static final String KEY_CLUSTER_DSPWD	= "server_cluster_DataStorePwd";
	// 4/22/2107
	
	/** OAuth1 consumer key/secret */
	public static final String KEY_AUTH_CON_SECRET 	= "oauth1_consumerSecret";
	public static final String KEY_AUTH_CON_KEY 	= "oauth1_consumerKey";
	
	/** OAuth1 token and secret */
	public static final String KEY_AUTH_TOK_SECRET 	= "oauth1_tokenSecret";
	public static final String KEY_AUTH_TOKEN 		= "oauth1_token";
	
	/** Authorization enabled: true or false */
	public static final String KEY_AUTH_ENABLED 	= "server_authEnabled";
	
	public static final String KEY_CONN_PROFILE		= "server_connectionProfile";
	
	// System admin
	private static final String KEY_SYSADMIN_USER 	= "sysadmin.user";
	private static final String KEY_SYSADMIN_PWD 	= "sysadmin.pwd";

	// Logging keys
	public static final String KEY_LOG_THRESHOLD	= "logThreshold";		// Threshold (DEBUG by default)
	public static final String KEY_LOG_PATTERN		= "logPattern";			// display pattern
	public static final String KEY_LOG_CONSOLE		= "logConsole";			// log to stdout (true/false)
	public static final String KEY_LOG_PATH			= "logPath";			// 9/4/17 path (full/relative) to the log file
	public static final String KEY_LOG_ROTATION_POL	= "logRotationPolicy";	// 9/4/17 daily, hourly, by size, etc...
	public static final String KEY_LOG_MASK_REGEXP	= "logMaskRegExp";		// 12/19/17 RE used to mask data from log events
	public static final String KEY_LOG_MASK			= "logMask";			// 12/19/17 Mask used to replace data on matches.
	public static final String KEY_LOG_CLEANER_POL	= "logCleanerPolicy";	// 2/2/19 by size, last modified. etc...
	
	// Web app context root session var keys
	public static final String KEY_CTX_PATH			= "KEY_CTX_PATH";
	public static final String KEY_CTX_URL			= "KEY_CTX_URL";
	
	/** Group (cluster) this node belongs to (for multi-cluster) support. */
    public static final String KEY_NODE_GRP         = "KEY_NODE_GRP";
    
	/** Cluster nodes (for TCP discovery). Note: default discovery is multicast (no members) */
	public static final String KEY_NODE_MEMBERS		= "KEY_NODE_MEMBERS";
	public static final String KEY_NODE_VENDOR      = "vendor";
	public static final String KEY_PRODUCT_TYPE		= "productType";
    
	// 6/9/2020 CN Pooling sonfig keys
	public static final String KEY_CNPOOL_SIZE		= "server_cnpool_poolSize";
	public static final String KEY_CNPOOL_AUTOGROW  = "server_cnpool_autoGrow";
	public static final String KEY_CNPOOL_MAXSLOTS	= "server_cnpool_maxSlotsBeforeGrow";
	
	// chat service interface + config. DEPRECATED: Kept for adapter compatibility reasons.
	private ServiceDescriptor messageBroker; 
	private ServiceDescriptor contactCenter; 
    
    /** all services loaded from the class path */
    private final List<ServiceDescriptor> serviceDescriptors = new ArrayList<ServiceDescriptor>();
    
	private String configLocation;
	
	// sys admin stuff
	private String sysAdminUser;
	private String sysAdminPwd;

	// used to manage connection profiles under $HOME/.cloud/CloudAdapter/Profiles
	private ProfileManager profileManager;
	
	/**
	 * Create a {@link NodeConfiguration} object from the container class-path: /bootstrap/bootstrap.ini
	 * Note: IT VALIDATES ALL DESCRIPTORS!
	 * <ul>
	 * <li>Loads /bootstrap/bootstrap.ini
	 * <li>Message broker descriptor: /configuration/message_broker.ini
	 * <li>Contact Center descriptor: /configuration/contact_center.ini
	 * </ul>
	 * @throws IOException If ANY descriptor is missing or invalid!
	 */
	public NodeConfiguration() throws IOException {
		this(true, true, null);
	}

	/**
	 * Load the server configuration. Optionally bypassing the {@link IMessageBroker}, 
	 * {@link IContactCenter} and {@link ProfileManager} configurations.
	 * 
	 * @param params Optional hash map of initialization parameters.
	 * @throws IOException If there is an error.
	 */
	public NodeConfiguration(Map<String, Object> params) throws IOException {
		this(true, true, params);
	}

	/**
	 * Load the server configuration. Optionally bypassing the {@link IMessageBroker}, 
	 * {@link IContactCenter} and {@link ProfileManager} configurations.
	 * 
	 * @param loadServices If true load the service descriptors: message_broker.ini, contact_center.init under /configuration.
	 * @param loadProfiles If true load the profile data using {@link ProfileManager}.
	 * 
	 * @throws IOException If there is an error.
	 */
	public NodeConfiguration(boolean loadServices, boolean loadProfiles) throws IOException {
		this(loadServices, loadProfiles, null);
	}
	
	/**
	 * Load the server configuration. Optionally bypassing the {@link IMessageBroker}, 
	 * {@link IContactCenter} and {@link ProfileManager} configurations.
	 * 
	 * @param loadServices If true load the service descriptors: message_broker.ini, contact_center.init under /configuration.
	 * @param loadProfiles If true load the profile data using {@link ProfileManager}.
	 * @param params Optional hash map of initialization parameters.
	 * @throws IOException If there is an error.
	 */
	public NodeConfiguration(boolean loadServices, boolean loadProfiles, Map<String, Object> params) throws IOException {
		loadInternal(params);
		
		if ( loadServices) {
			loadServiceDescriptors();
		}
		refreshVars();
		initialize();
		
		// save the initialization parameters
		if ( params != null ) {
			LOGD("Construct: Storing intialization parameters " + params);
			putAll(params);
		}
		if (loadProfiles) {
			// Connection profiles under ${user.home}/.cloud/CloudAdapter
			profileManager = new ProfileManager(configLocation); 
		}
	}

	/**
	 * Miscellaneous initialization.
	 */
	private void initialize() {
		// init secrets: consumer + token
		String secret1 = getAuthorizationConsumerSecret(); 
		String secret2 = getAuthorizationTokenSecret();

		if ( !containsKey(KEY_AUTH_CON_KEY)) {
			setProperty(KEY_AUTH_CON_KEY, EncryptionTool.HASH/*SHA1*/(EncryptionTool.UUID()));
		}
		if ( !containsKey(KEY_AUTH_TOKEN)) {
			setProperty(KEY_AUTH_TOKEN, EncryptionTool.HASH/*SHA1*/(EncryptionTool.UUID()));
		}
		if ( secret1 == null || secret1.isEmpty()) {
			setProperty(KEY_AUTH_CON_SECRET, EncryptionTool.HASH/*MD5*/(EncryptionTool.UUID()));
		}
		if ( secret2 == null || secret2.isEmpty()) {
			setProperty(KEY_AUTH_TOK_SECRET, EncryptionTool.HASH/*MD5*/(EncryptionTool.UUID()));
		}
	}
	
	/**
	 * Load the Server config from the file system @ $HOME/.cloud/CloudAdapter/{CTX_PATH}.ini
	 * @param params Startup parameters: CTX_PATH = Node name & probably others.
	 * @throws IOException if any error occurs during load (file missing, etc).
	 */
	private void loadInternalFromFileSystem(Map<String, Object> params) throws IOException {
		refreshVars();
		
		// use the context path as the prefix for bootstarp.ini
		// $HOME/.cloud/CloudAdapter/CTX_PATH.ini
		String prefix = getProperty(KEY_CTX_PATH) != null 
				? getProperty(KEY_CTX_PATH)
				: params != null 
					? params.get(KEY_CTX_PATH).toString() : null;

		String fileName = prefix + ".ini"; 

		if ( getConfigLocation() == null) {
			throw new IOException("Failed to load server cfg from the FS. Invalid config base path.");
		}
		if ( prefix == null) {
			throw new IOException("Can't load server cfg from the FS @ " + getConfigLocation() + ". Missing APPROOT prefix.");
		}
		if (! new File(getConfigLocation() + "/" + fileName).exists() ) {
			throw new IOException("Can't load server cfg from the FS. Can't find  " + getConfigLocation() + "/" + fileName);
		}
		
		// Overwrite w/ file system @ $HOME/.cloud/CloudAdapter
		InputStream is = null;

		LOGD("LoadFromFS: Loading srv cfg from  " + getConfigLocation() + " " + fileName);

		try {
			is =  IOTools.findStream(getConfigLocation(), fileName, true, false);
			
			// Findbugs: Redundant nullcheck of is, which is known to be non-null in com.cloud.core.services.NodeConfiguration.loadInternalFromFileSystem(Map)
			load (is);
			
		} catch (Exception e) {
			//System.err.println("SRVCFG Load: Failed " + e.toString());
			throw new IOException("SRVCFG Load: Failed " + e.toString());
		}
		finally {
			if ( is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * Internal configuration load
	 * @param params Optional startup parameters.
	 * @throws IOException
	 */
	private void loadInternal (Map<String, Object> params) throws IOException {
		// Must load from the CP first always so the config location & other defaults can be loaded.
		// Load: try the CP first.
		try {
			LOGD("LoadInternal: Loading server cfg from the class path @ " + RESOURCE);
			
			// Default: this should not fail. A DEFAULKT CONFIG IN THE CP IS REQUIRED!
			InputStream is = NodeConfiguration.class.getResourceAsStream(RESOURCE); 
			
			if ( is == null) {
				throw new IOException("Failed to load a server configuration from class-path " + RESOURCE);
			}
			load(is);
			is.close();
		} 
		catch (IOException e) {
			LOGW("[SRV-CFG] LoadInternal: ClassPath Node Configuration load failed: " + e.toString());
			throw e;
		}
		// overwrite w/ the FS config (if available).
		try {
			LOGD("[SRV-CFG] LoadInternal: Loading server cfg from the file file system w params: " + params);
			loadInternalFromFileSystem(params);
		} catch (Exception e) {
			LOGW("[SRV-CFG] LoadInternal: FileSystem Node Configuration load failed: " + e.toString());
		}
	}
	
	/**
	 * Load all service descriptors (if available)
	 * <li> Adapter: Message Broker + Contact Center.
	 * <li> Connector
	 * <li> Voice Recording
	 * <li> LIcensing
	 * 
	 * @throws IOException
	 */
	private void loadServiceDescriptors() throws IOException {
		// case #1 ( adapter - compound )
		messageBroker = loadServiceDescriptor(/*"message broker"*/ ServiceDescriptor.ServiceType.MESSAGE_BROKER, DESCRIPTOR_BROKER);
		contactCenter = loadServiceDescriptor(/*"contact center"*/ ServiceDescriptor.ServiceType.CONTACT_CENTER, DESCRIPTOR_CC);
		
		// Loaded Adapter, abort the rest.
		if ( messageBroker != null && contactCenter != null) {
			// 7/21/15: The Contact Center must go 1st so It can initialize 1st.
			serviceDescriptors.add(contactCenter);
			serviceDescriptors.add(messageBroker);
			return;
		}
		/** All service names: To be loaded (if available) */
		final String[] SINGLE_BACKEND__SERVICES = {
			DESCRIPTOR_CONNECTOR,	// Cloud connector (Call Center)
			DESCRIPTOR_VR,			// Voice recording
			DESCRIPTOR_LIC,			// Licensing
			DESCRIPTOR_DAEMON		// Generic Daemon
		};
		// These must match the order above
		final ServiceDescriptor.ServiceType[] types = { 
				ServiceDescriptor.ServiceType.CALL_CENTER, 
				ServiceDescriptor.ServiceType.VOICE_RECORDING, 
				ServiceDescriptor.ServiceType.LICENSING, 
				ServiceDescriptor.ServiceType.DAEMON
		};
		// case #2 - single backend services
		for (int i = 0; i < types.length; i++) {
			ServiceDescriptor desc = loadServiceDescriptor(types[i], SINGLE_BACKEND__SERVICES[i]);
			if ( desc!= null) {
				serviceDescriptors.add(desc);
			}
		}
		// FIXME: Should probably validate service loading rules...
		if ( serviceDescriptors.size() == 0) {
			throw new IOException("Failed to load any services from the class path.");
		}
	}

	/**
	 * Load an {@link IMessageBroker} or {@link IContactCenter} service descriptor.
	 * @param descriptor {@link ServiceDescriptor}.
	 * @param label Debug label.
	 * @param resource {@link IConfiguration} resource name for the service descriptor.
	 * @throws IOException
	 */
	private ServiceDescriptor loadServiceDescriptor (ServiceDescriptor.ServiceType type, /*String label,*/ String resource) throws IOException {
		ServiceDescriptor descriptor 	= null;
		Properties props 				= new Properties();
		InputStream is 					= null;
		
		try {
			System.out.printf("[SRV-CFG] Looking for service %16s ==> class path %s", type, resource);
			
			is = NodeConfiguration.class.getResourceAsStream(resource);
			
			if ( is == null ) {
				System.out.println(" (MISSING) "); 
				return null; //throw new IOException("Missing " + resource + " in class-path.");
			}
			
			props.load(is);
			
			descriptor = new ServiceDescriptor(type, props);
			
			System.out.println(" (FOUND) @ " + descriptor);

		} 
		catch (Exception e) {
			throw new IOException("Failed to load " + type  + " descriptor @ " + resource + " : " + e.getMessage());
		}
		finally {
			IOTools.closeStream(is);
		}
		props = null;
		return descriptor;
	}

	/**
	 * @deprecated Kept for compatibility reasons.
	 * @return Message Broker {@link ServiceDescriptor}.
	 */
	public ServiceDescriptor getMessageBrokerDescriptor() {
		return messageBroker;
	}

	/**
	 * @deprecated Kept for compatibility reasons.
	 * @return Contact Center {@link ServiceDescriptor}
	 */
	public ServiceDescriptor getContactCenterDescriptor() {
		return contactCenter;
	}


	/**
	 * Load all vars from the config
	 */
	private void refreshVars() {
		configLocation		= getProperty(KEY_CONFIG_PATH);
		
		// sys admin
		sysAdminUser		= getProperty(KEY_SYSADMIN_USER);
		sysAdminPwd			= getProperty(KEY_SYSADMIN_PWD);
		
		evaluateSystemProperties();
	}


	/**
	 * Evaluate any Java system properties such as ${user.home} in variable configLocation
	 * <ul>
	 * <li>Linux ${user.home}/.cloud becomes	$HOME/.cloud
	 * <li>Windows ${user.home}/.cloud becomes c:\Users\USER\.cloud
	 * </ul>
	 * Note: Windows: If installing as administrator Java's user.home evaluates to c:\
	 * so use System.getenv("USERPROFILE") instead. UPDATE: Don't use that either.
	 */
	private void evaluateSystemProperties() {
		/* DO NOT USE! If installing as administrator The user's home returns c:\windows\system32....!
		if ( configLocation.contains("${user.home}") && CoreTools.OS_IS_WINDOWS) {
			configLocation = configLocation.replace("${user.home}", CoreTools.getUserHome());
		} */
		// Note: configLocation may be null...
		if ( configLocation == null ) {
			return;
		}
		String tempConfig 	= configLocation.replace("user.home", "java.io.tmpdir");
		configLocation 		= IOTools.evalSystemProperties(configLocation);
		// CentOS7 default TC installation may have permission problems /usr/share/tomcat
		// Check folder permissions
		if ( !IOTools.OS_IS_WINDOWS) {
			if ( !IOTools.mkDir(configLocation)) {
				// use java.io.tmpdir instead of user.home
				configLocation = IOTools.evalSystemProperties(tempConfig);
			}
		}
	} 

	/**
	 * Get descriptors for all loaded services.
	 * @return List of {@link ServiceDescriptor}. 
	 */
	public List<ServiceDescriptor> getServiceDescriptors() {
		return serviceDescriptors;
	}
	
	/**
	 * Find a {@link ServiceDescriptor}.
	 * @param type Type of service. See {@link ServiceType}.
	 * @return
	 */
	public ServiceDescriptor findServiceDescriptor (ServiceType type) {
		for (ServiceDescriptor desc : serviceDescriptors) {
			if (desc.getType() == type) {
				return desc;
			}
		}
		return null;
	}

	public ServiceDescriptor findServiceDescriptor (String id) {
		for (ServiceDescriptor desc : serviceDescriptors) {
			if (desc.getVendorId().equals(id)) {
				return desc;
			}
		}
		return null;
	}
	
	/*
	public String getBuildNumber() {
		//return getProperty(KEY_SRV_BUILD);
		return getProperty(CloudAdapterBuildInfo.ADAPTER_VERSION);
	} */
	
	/**
	 * Concatenate all vendor names with a slash.
	 * @return
	 */
	public String getVendorNames() {
		StringBuffer buf = new StringBuffer();
		for (ServiceDescriptor desc : serviceDescriptors) {
			buf.append(desc.getVendorName() + "/");
		}
		// remove last /
		String names = buf.toString();
		return !names.isEmpty() ? names.substring(0, names.length() - 1) : names;
	}
	
	/**
	 * @deprecated Kept for compatibility reasons.
	 */
	public String getMessageBrokerClass() {
		return messageBroker.getClassName();  
	}

	/**
	 * @deprecated Kept for compatibility reasons.
	 */
	public String getMessageBrokerConfig() {
		return messageBroker.getConfigFileName();  
	}

	/**
	 * @deprecated Kept for compatibility reasons.
	 */
	public String getMessageBrokerVendor() {
		return messageBroker.getVendorName();  
	}
	
	/**
	 * @deprecated Kept for compatibility reasons.
	 */
	public String getContactCenterClass() {
		return contactCenter.getClassName();  
	}

	/**
	 * @deprecated Kept for compatibility reasons.
	 */
	public String getContactCenterConfig() {
		return contactCenter.getConfigFileName();  
	}

	/**
	 * @deprecated Kept for compatibility reasons.
	 */
	public String getContactCenterVendor() {
		return contactCenter.getVendorName();  
	}
	
	public boolean isServiceScheduleEnabled () {
		return Boolean.parseBoolean(getProperty(KEY_SSHED_ENABLED));
	}

	/**
	 * @deprecated Service hours are deprecated. Kept for compatibility reasons.
	 */
	public String getServiceScheduleStartHHMM() {
		return getProperty(KEY_SSHED_START);
	}

	/**
	 * @deprecated Service hours are deprecated. Kept for compatibility reasons.
	 */
	public void getServiceScheduleStartHours(int[] HHMM) {
		String s = getProperty(KEY_SSHED_START);

		// validate....
		if (s == null || s.trim().isEmpty()) {
			//log.warn("Invalid service hours (START). Using default.");
			HHMM[0] = 8;
			HHMM[1] = 0;
			return;
		}
		String[] vals = s.split(" ");
		HHMM[0] = getInteger(vals[0], 0);
		HHMM[1] = getInteger(vals[1], 0);
	}
	
	/**
	 * @deprecated Service hours are deprecated. Kept for compatibility reasons.
	 */
	public String getServiceScheduleStopHHMM() {
		return getProperty(KEY_SSHED_STOP);
	}
	
	/**
	 * @deprecated Service hours are deprecated. Kept for compatibility reasons.
	 */
	public void getServiceScheduleStopHours(int[] HHMM) {
		String s = getProperty(KEY_SSHED_STOP);
		
		// validate
		if (s == null || s.trim().isEmpty()) {
			//log.warn("Invalid service hours (STOP). Using default.");
			HHMM[0] = 17;
			HHMM[1] = 0;
			return;
		}
		
		String[] vals = s.split(" ");
		HHMM[0] = getInteger(vals[0], 0);
		HHMM[1] = getInteger(vals[1], 0);
	}
	
	/**
	 * @deprecated Service hours are deprecated. Kept for compatibility reasons.
	 */
	public String getServiceScheduleDays() {
		return getProperty(KEY_SSHED_DAYS);
	}
	
	private int getInteger(String key, int def) {
		try {
			return Integer.parseInt(key);
		} catch (Exception e) {
			return def;
		}
	}

	public String getSysAdminUser() {
		return sysAdminUser;
	}
	
	public String getSysAdminPwd() {
		return sysAdminPwd;
	}

	public void setSysAdminUser(String sysAdminUser) {
		this.sysAdminUser = sysAdminUser;
		setProperty(KEY_SYSADMIN_USER, sysAdminUser);
	}

	public void setSysAdminPwd(String sysAdminPwd) {
		this.sysAdminPwd = sysAdminPwd;
		setProperty(KEY_SYSADMIN_PWD, sysAdminPwd);
	}

	/**
	 * The server is considered PRIMARY iif:
	 * <li> The run mode is PRIMARY OR
	 * <li> The run mode is CLUSTER
	 * @return True if PRIMARY.
	 */
	public boolean isPrimary() {
		String runMode = getProperty(KEY_RUN_MODE);
		if ( runMode == null || runMode.isEmpty()) {
			log.warn("isPrimary Missing server config key " + KEY_RUN_MODE + " Assuming host as PRIMARY.");
			return true;
		}
		RunMode mode = RunMode.valueOf(runMode);
		return  mode == RunMode.PRIMARY || mode == RunMode.CLUSTER;
	}

	/**
	 * Primary host for {@link FailOverType} SERVLET.
	 * @deprecated This logic was never implemented. Do not use.
	 * @return Primary host for {@link FailOverType} SERVLET.
	 */
	public String getPrimaryHost() {
		return getProperty(KEY_PRIMARY);
	}

	/**
	 * Fail over interval applies to {@link FailOverType} SERVLET only.
	 * @return Interval in ms.
	 */
	public int getFailOverInterval() {
		int interval = 5000;
		try {
			interval = Integer.parseInt(getProperty(KEY_FAILOVER_INT));
		} catch (Exception e) {
			log.error("Missing fail-over interval key " + KEY_FAILOVER_INT + " in server config. Deafult to 5s");
		}
		return interval;
	}

	/**
	 * Get the {@link FailOverType} defined in the server configuration (server_failOverType). Defaults to CLUSTER.
	 * @return Failover type: SERVLET or CLUSTER.
	 */
	public CloudFailOverService.FailOverType getFailOverType () {
		String type = getProperty(KEY_FAILOVER_TYPE);

		if (  type != null && !type.isEmpty()) {
			try {
				return CloudFailOverService.FailOverType.valueOf(getProperty(KEY_FAILOVER_TYPE));
			} catch (Exception e) {
				log.error("Invalid failover type! Defaults to SERVLET.");
			}
		}
		// Default to SERVLET (No cluster)
		return CloudFailOverService.FailOverType.SERVLET;
	} 
	
	/**
	 * Force the fail over type.
	 * @param type: SERVLET or CLUSTER.
	 */
	public void setFailOverType(CloudFailOverService.FailOverType type) {
		setProperty(KEY_FAILOVER_TYPE, type.name());
	}
	
	/**
	 * Get the base location of the node configuration.
	 * @return defaults to ${user.home}/.cloud/CloudAdapter
	 */
	public String getConfigLocation() {
		return configLocation;
	}


	public String getConnectionProfileName() {
		return getProperty(KEY_CONN_PROFILE);
	}

	public void setConnectionProfileName(String name) {
		setProperty(KEY_CONN_PROFILE, name);
	}
	
	public String getDefaultProfileBasePath() {
		String name = getConnectionProfileName();
		
		if ( name == null || name.isEmpty()) {
			log.error("Default connection profile is NULL! Base: " + configLocation);
			//throw new IllegalArgumentException("No default connection profile available.");
			// Fix for UNIFIED_CC-50, UNIFIED_CC-51
			return null; 
		}
		return profileManager.getBasePath(name);
	}
	
	private void save (Properties props, String path) throws IOException {
    	FileOutputStream fos 	= null;
    	try {
    		fos = new FileOutputStream(path);
    		props.store(fos, "UPDATED BY TOMCAT! EDIT @ YOUR OWN RISK!");
    	}
    	finally {
    		if ( fos != null) {
    			try {
    				fos.close();
				} catch (Exception e) {
				}
    		}
    	}
	}
	
	/**
	 * Save the configuration to the file system (default) or the class path if
	 * unable to save to the FS.
	 * @throws Exception
	 */
	public void save() throws Exception {
		// vsilva 11/6/2016 DON'T SAVE TO THE CP path0: Class path location class.path/bootstrap.ini
    	//String path0 	= IOTools.getResourceAbsolutePath(RESOURCE);
    	
    	// path1: file system: user.home/.cloud/CloudAdapter/{NODE_CTX_ROOT}.ini
		String path1 	= getConfigLocation() != null && getProperty(KEY_CTX_PATH) != null
				? getConfigLocation() +  "/" + getProperty(KEY_CTX_PATH)  +  ".ini" 
				: null ; 
		
    	//LOGD("[SRV-CFG] Server config save @ (CLASSPATH)  " + path0);
    	LOGD("[SRV-CFG] Server config save @ (FILESYSTEM) " + path1);
    	
    	// 12/6/2016 DON'T save to the CP.
    	//save(this, path0);
    	
    	// save a clone on the FS @ $HOME/.cloud/CloudAdapter/{CTX_PATH}.ini.
    	if ( path1 != null) {
	    	Properties clone = new Properties();
	    	clone.putAll(this);
	    	save(clone, path1);
    	}
    	else {
    		log.warn("Unable to save server config to the FILESYSTEM. Context path key " + KEY_CTX_PATH + " is missing.");
    		//log.debug("Saving server config @ CLASSPATH " + path0);
    		
    		// save @ the class path.
    		//save(this, path0);
    	}
    	
    	// These are required so the {NODENAME}.ini file can be loaded from the file system {NODENAME} @ KEY_CTX_PATH 
    	Map<String, Object> params =  getClusterParams();
    	
    	// gotta refresh loaded vars!
    	clear();
    	loadInternal (params); //null);
    	refreshVars();
	}

	public void validate() throws IOException {
		// validate location. Default: ${user.home}/.cloud/CloudAdapter
		if ( getConfigLocation() == null)
			throw new IOException("Configuration path cannot be NULL!");
		
		File f = new File(getConfigLocation());
		if ( ! f.exists() )
			throw new IOException("Invalid configuration base path " + getConfigLocation());
	}
	
	/**
	 * Authorization enabled?.
	 * @return True if authorization is enabled (defaults to true).
	 */
	public boolean isAuthorizationEnabled() {
		return getProperty(KEY_AUTH_ENABLED) != null ?  Boolean.parseBoolean(getProperty(KEY_AUTH_ENABLED)) : true; //false;
	}

	public String getAuthorizationConsumerKey() {
		return getProperty(KEY_AUTH_CON_KEY);
	}

	public String getAuthorizationConsumerSecret() {
		return getProperty(KEY_AUTH_CON_SECRET);
	}

	public String getAuthorizationToken() {
		return getProperty(KEY_AUTH_TOKEN);
	}
	
	public String getAuthorizationTokenSecret() {
		return getProperty(KEY_AUTH_TOK_SECRET);
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}
	
	public String getLogThresHold() {
		return getProperty(KEY_LOG_THRESHOLD);
	}

	public String getLogPattern() {
		return getProperty(KEY_LOG_PATTERN);
	}

	public boolean getLogConsole() {
		return getProperty(KEY_LOG_CONSOLE) != null ? Boolean.parseBoolean(getProperty(KEY_LOG_CONSOLE)) : false;
	}

	/**
	 * Get the log folder (default to logs).
	 * @return Log folder path (full or relative) or 'logs' if not set (default).
	 * @since 1.0.3
	 */
	public String getLogFolder() {
		return getProperty(KEY_LOG_PATH, Container.getDefautContainerLogFolder()); // "logs");
	}

	/**
	 * Get the log rotation policy.
	 * @return rotation policy or '.'yyyy-MM-dd-a (twice a day) if not set.
	 * @since 1.0.3
	 */
	public String getLogRotationPolicy() {
		return getProperty(KEY_LOG_ROTATION_POL, "'.'yyyy-MM-dd-a");
	}

	/**
	 * @return Get the regular expression used to mask sensitive information from log messages.
	 * @since 1.0.4
	 */
	public String getLogMaskRegExp() {
		return getProperty(KEY_LOG_MASK_REGEXP);
	}

	/**
	 * @return The mask to hide sensitive information.
	 * @since 1.0.4
	 */
	public String getLogMask() {
		return getProperty(KEY_LOG_MASK);
	}

	/**
	 * The cluster service is considered enabled if
	 * <li>The run mode is CLUSTER OR,
	 * <li>The fail-over type is CLUSTER.
	 * 
	 * @return True is the cluster service is enabled.
	 */
	public boolean isClusterEnabled() {
		String runMode 		= getProperty(KEY_RUN_MODE);
		FailOverType type 	= getFailOverType();
		
		if ( runMode == null) {
			LOGD("isClusterEnabled Run mode missing from " + RESOURCE + " default to false.");
			return false;
		}
		return (RunMode.valueOf(runMode) == RunMode.CLUSTER)
				|| ( type == FailOverType.CLUSTER_HAZELCAST) || ( type == FailOverType.CLUSTER_ZEROCONF);
	}
	
	public RunMode getRunMode() {
		String runMode = getProperty(KEY_RUN_MODE);
		if ( runMode == null || runMode.isEmpty()) {
			log.warn("getRunMode Missing server config key " + KEY_RUN_MODE + ". Assuming PRIMARY!");
			runMode = "PRIMARY";
		}
		return RunMode.valueOf(runMode);
	}
	
	public void setRunMode(RunMode mode) {
		setProperty(KEY_RUN_MODE, mode.name());
	}
	
	/**
	 * Inject a collection of {@link Properties} into the main set.
	 * @param params
	 */
	public void addProperties(Properties params) {
		Set<Object> keys  = params.keySet();
		for (Object object : keys) {
			setProperty(object.toString(), params.getProperty(object.toString()));
		}
	}

	/**
	 * Inject a {@link Map} of parameters into the main set.
	 * @param params A {@link Map} of extra attributes.
	 */
	public void addParams(Map<String, Object> params) {
		/* Findbugs NodeConfiguration.java:848 com.cloud.core.services.NodeConfiguration.addParams(Map) makes inefficient use of keySet iterator instead of entrySet iterator [Of Concern(18), Normal confidence]
		Set<String> keys  = params.keySet();
		for (String key : keys) {
			setProperty(key, params.get(key).toString());
		} */
		Set<Map.Entry<String, Object>> entries = params.entrySet();
		for (Map.Entry<String, Object> entry : entries) {
			setProperty(entry.getKey(), params.get(entry.getKey()).toString());
		}
	}

	/**
	 * Get the node cluster parameters.
	 * @return {@link Map} of attributes used in clustering: 
	 * <ul>
	 * <li>WebApp Context Root (KEY_CTX_PATH)  - /CloudAdapterNode00X
	 * <li>Node URL (KEY_CTX_URL) - http://host:port/CloudAdapterNode00X.
	 * <li>Cluster Group Name (KEY_NODE_GRP) for multi cluster support
	 * </ul>
	 * @throws IOException In any error occurs setting cluster parameters.
	 */
	public Map<String, Object> getClusterParams() {
		Map<String, Object> params = new HashMap<String, Object>();
		
		if ( getProperty(KEY_CTX_PATH) != null ) {
			params.put(KEY_CTX_PATH, getProperty(KEY_CTX_PATH));
		}
		else {
			log.warn("Get cluster params: Missing node ContextRoot " + KEY_CTX_PATH);
		}
		if ( getProperty(KEY_CTX_URL) != null ) {
			params.put(KEY_CTX_URL, getProperty(KEY_CTX_URL));
		}
		else {
			log.warn("Get cluster params: Missing node URL " + KEY_CTX_URL);
		}
		if ( getProperty(KEY_NODE_GRP) != null ) {
			params.put(KEY_NODE_GRP, getProperty(KEY_NODE_GRP));
		}
		else {
			log.warn("Get cluster params: Missing cluster Group " + KEY_NODE_GRP + ". Using default: dev");
			params.put(KEY_NODE_GRP, "dev");	// set a default
		}
		if ( getProperty(KEY_NODE_MEMBERS) != null ) {
			params.put(KEY_NODE_MEMBERS, getProperty(KEY_NODE_MEMBERS));
		}
		else {
			log.warn("Get cluster params: Missing cluster TCP members " + KEY_NODE_MEMBERS);
		}
		// 12/27/2018 Add fail over type
		if ( getProperty(KEY_FAILOVER_TYPE) != null ) {
			params.put(KEY_FAILOVER_TYPE, getProperty(KEY_FAILOVER_TYPE));
		}
		else {
			log.warn("Get cluster params: Missing fail over type " + KEY_FAILOVER_TYPE);
		}
		// 12/29/2018
		final String vendor = getVendorNames();
		if ( vendor != null && !vendor.isEmpty() ) {
			params.put(KEY_NODE_VENDOR, vendor);
		}
		
		final ServiceType type = getProductType();
		if ( type != null) {
			params.put(KEY_PRODUCT_TYPE, type.name());
		}
		return params;
	}
	
	private void addParam(String key, String value, boolean force) {
		if ( (get(key) != null) && ! force)
			return;
		put(key, value);
	}
	
	/**
	 * Add the URL of the node to the server configuration. This info
	 * may be sent to the Cluster Manager when running in cluster mode.
	 * <b>Note: The value will NOT be set if already exists unless the force argument is true.</b>
	 * 
	 * @param url Node URL.
	 * @param force if true it will force setting the variable value
	 */
	public void addNodeURL(String url, boolean force) {
		if ( url == null) {
			return;
		}
		// localhost is NOT valid in URL!
		if ( url.contains("localhost")) {
			log.warn("Add Node URL: localhost is NOT a valid name @ " + url);
			url = url.replace("localhost", IOTools.getHostIp());
		}
		// 1/24/2019 cleanup rogue urls
		if ( url.contains("index.jsp")) {
			log.warn("Add Node URL: removing index.jsp from " + url);
			url = url.replace("index.jsp", ""); 
		}
		log.debug("Add/Set Node URL: " + url + " Force: " + force);
		addParam(KEY_CTX_URL, url, force);
	}
	
	/**
	 * Get server version from the configuration.
	 * @return Server version as x.y.z
	 */
	public String getServerVersion() {
		// Note: Old builds don't have this key!
		String version = getProperty(NodeConfiguration.KEY_SERVER_VER);
		if ( version == null) {
			log.warn("GetServerVersion: Missing build version " + NodeConfiguration.KEY_SERVER_VER + " from config.");
		}
		return version != null ? version : "0.0.0.0";	
	}

	public boolean requiresLicense() {
		for (ServiceDescriptor sd : serviceDescriptors) {
			if ( sd.requiresLicense())
				return true;
		}
		return false;
	}
	
	/**
	 * License checking enabled?
	 * @return true if the server is NOT running in developer mode.
	 */
	/* 12/19/2018 Devmode backdoor removed
	public boolean isLicenseCheckEnabled() {
		try {
			String inDevMode = getProperty(NodeConfiguration.KEY_DEV_MODE);
			
			if ( inDevMode == null || inDevMode.isEmpty()) {
				return true;
			}
			return !Boolean.parseBoolean(inDevMode);
		} catch (Exception e) {
			log.error("isLicenseCheckEnabled : " + e + " DEFAULT true.");
			return true;
		}
	} */

	public String getLicenseDecryptionKeyResource() {
		return getProperty(NodeConfiguration.KEY_LICKEY_LOCATION);
	}

	public String getLicenseKeyType() {
		return getProperty(NodeConfiguration.KEY_LICKEY_TYPE);
	}

	public String getLicenseProductId() {
		return getProperty(NodeConfiguration.KEY_PRODUCT_ID);
	}

	public String getLicense() {
		return getProperty(NodeConfiguration.KEY_SERVER_LIC);
	}

	/**
	 * Set a license secret.
	 * @param string License secret.
	 */
	public void setLicense(String string) {
		if ( string == null ) {
			LOGW("SetLicense: Invalid (NULL) secret.");
			return;
		}
		setProperty(NodeConfiguration.KEY_SERVER_LIC, string);
	}

	@Override
	public synchronized boolean equals(Object config) {
		// 11/29 16 Findbugs Bug: com.cloud.core.services.NodeConfiguration doesn't override java.util.Hashtable.equals(Object)
		return super.equals(config);
	}

	@Override
	public synchronized int hashCode() {
		return super.hashCode();
	}
	
	/**
	 * Return the OAuth1 key used for signature or verification.
	 * @return consumerSecret + "&" + tokenSecret;
	 * @throws IOException if either consumer or token secret is null.
	 * @see https://oauth1.wp-api.org/docs/basics/Signing.html
	 */
	public String getAuthorizationOAuth1Key () throws IOException {
		String consumerSecret 	= getAuthorizationConsumerSecret();
		String tokenSecret		= getAuthorizationTokenSecret();
		if ( consumerSecret == null ) 	throw new IOException("Missing OAuth1 cosumer secret.");
		if ( tokenSecret == null)		throw new IOException("Missing OAuth1 token secret.");
		return consumerSecret + "&" + tokenSecret;		
	}
	
	/**
	 * @return True if the product supports clustering. Defaults to true. If this property is false it means the WebApp should not include any clustering library.
	 * @since 1.0.2
	 */
	public boolean productSupportsClustering () {
		final String key = "product_supportsClustering";
		return  containsKey(key) ? Boolean.parseBoolean(getProperty(key)) : true;
	}

	/**
	 * @return True if the product supports notifications. Defaults to true. Can be used by the UI to hide notifications information.
	 * @since 1.0.2
	 */
	public boolean productSupportsNotifications () {
		final String key = "product_supportsNotifications";
		return  containsKey(key) ? Boolean.parseBoolean(getProperty(key)) : true;
	}
	
	/**
	 * Get the product type via {@link ServiceType}.
	 * @return ServiceType.CONTACT_CENTER for the Adapter or the corresponding ServiceType for single service products: CALL_CENTER, DAEMON, etc.
	 * @throws IOException In no {@link ServiceDescriptor} was loaded.
	 */
	public ServiceType getProductType () /*throws IOException*/ {
		for ( ServiceDescriptor d: serviceDescriptors) {
			if ( d.getType() == ServiceType.CONTACT_CENTER || d.getType() == ServiceType.MESSAGE_BROKER) {
				return ServiceType.CONTACT_CENTER;
			}
			return d.getType();
		}
		//throw new IOException("Missing product type in master server.");
		return null;
	}
}
