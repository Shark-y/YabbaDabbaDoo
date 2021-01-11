package com.cloud.cluster;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.OSMetrics;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.types.CoreTypes;

/**
 * This class encapsulates cluster operations such as:
 * <ul>
 * <li> Initialize the cluster.</li>
 * <li> Shutdown the cluster.</li>
 * </ul>
 * It is meant to hide the cluster implementation so it can be easily switched.
 * The current implementation used is Hazelcast (see www.hazelcast.com)
 * <p/>
 * Some of the distributed operations provided are:
 * <ul>
 * <li> Distributed locks: To prevent multiple nodes from processing the same batch.
 * <li> Distributed Atomic longs: Used to replicate values across the cluster.
 * <li> A Pub/Sub mechanism to send messages across.
 * </ul>
 * @see www.hazelcast.com
 * @author VSilva
 *
 */
public class CloudCluster  {

	static final Logger log = LogManager.getLogger(CloudCluster.class);
	
	/** A Topic name used to send cluster wide messages */
	public static final String CLOUD_CLUSTER_TOPIC = "CloudContactCenter";
	
	/*##################################################################################
	 *  Cluster member attribute keys. This must match the keys under NodeConfiguration.
	 */
	
	/** Context Path of the running node. For example: CloudContactCenterNode001 */
	public static final String KEY_CTX_PATH		= "KEY_CTX_PATH";
	
	/** Url of the node */
	public static final String KEY_CTX_URL		= "KEY_CTX_URL";

	/** Group (cluster) this node belongs to (for multi-cluster) support. */
	public static final String KEY_NODE_GRP		= "KEY_NODE_GRP";

	/** Cluster nodes (for TCP discovery). Note: default discovery is multicast (no members) */
	public static final String KEY_NODE_MEMBERS	= "KEY_NODE_MEMBERS";

	/** Desired cluster provider name: HAZELCAST or DATABASE.  See {@link Provider} */
	public static final String KEY_PROVIDER 	= "server_failOverType"; //"cluster_Provider";

	/** Product Type via {@link ServiceType} */
	public static final String KEY_PRODUCT_TYPE	= "productType";

	// JSON message keys
	public static final String KEY_SENDER		= "sender";
	public static final String KEY_SENDER_URL	= "senderUrl";
	public static final String KEY_STATUS		= "statusCode";
	public static final String KEY_STMSG		= "statusMessage";
	public static final String KEY_RUNMODE		= "runMode";
	public static final String KEY_CNPROFILE	= "connectionProfile";
	public static final String KEY_VENDOR		= "vendor";

	/*8 Cluster providers */
	public enum Provider { CLUSTER_HAZELCAST, CLUSTER_DATABASE, CLUSTER_ZEROCONF} ;
	
	/** Wrapper to the cluster services provider */
	private IClusterInstance instance; 

	/** Singleton main cluster access object */
	private static final CloudCluster cluster = new CloudCluster();
	
	/** Single thread used to do maintenance & garbage collection */
	private Thread masterTaskThread;
	
	/*
	 * Hidden singleton constructor.
	 */
	private CloudCluster() {
		//masterTaskInit();
		//masterTaskStart();
	}

	private void masterTaskInit () {
		if ( masterTaskThread != null ) {
			return;
		}
		// Initialize the master task thread
		masterTaskThread = new Thread(new Runnable() {
			public void run() {
				while ( true) {
					try {
						/*
						if ( instance instanceof DBClusterInstance) {
							((DBClusterInstance)instance).collectGarbage();
						} */
						if ( instance != null) {
							instance.collectGarbage();
						}
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				log.debug("Cluster master task loop done.");
			}
		}, "CloudClusterMasterTask-" + CoreTypes.NODE_NAME);
		masterTaskThread.setPriority(Thread.MIN_PRIORITY);
	}
	
	private void masterTaskStart () {
		masterTaskThread.start();
	}

	private void masterTaskStop () {
		if ( masterTaskThread == null ) {
			return;
		}
		masterTaskThread.interrupt();
		try {
			masterTaskThread.join(2000);
			masterTaskThread = null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Get an instance of the cluster. There is only 1 instance of this class.
	 * @return Singleton instance.
	 */
	public static CloudCluster getInstance() {
		return cluster;
	}

	
	/**
	 * Get a {@link IClusterInstance} which is the inner provider of the clustering platform. There are 2 current implementations:
	 * <ul>
	 * <li>Hazelcast - see www.hazelcast.com
	 * <li>Persistent DataStore using MySQL.
	 * </ul>
	 * @return {@link IClusterInstance} cluster provider implementation.
	 */
	public IClusterInstance getClusterInstance() {
		return instance;
	}

	
	/**
	 * Initialize the cluster.
	 * 
	 * @param attributes Map of key, value pair attributes such as:
	 * <ul>
	 * <li> The context root of the node (KEY_CTX_PATH)
	 * <li> The node URL (KEY_CTX_URL).
	 * </ul>
	 * <p>For example:  {KEY_CTX_PATH=/CloudContactCenterNode001, KEY_CTX_URL=http://192.168.41.3:8080/CloudContactCenterNode001/index.jsp}
	 */
	public void initialize(Map<String, Object> attributes) throws IOException {
		if ( instance == null ) {
			final Object provider = attributes.get(KEY_PROVIDER);

			if ( provider != null ) {
				if ( provider.toString().equals(Provider.CLUSTER_DATABASE.name())) {
					instance = new com.cloud.cluster.db.DBClusterInstance(attributes);
				}
				else if ( provider.toString().equals(Provider.CLUSTER_HAZELCAST.name())) {
					instance = new com.cloud.cluster.hazelcast.HzClusterInstance(attributes);
					//throw new IOException("Hazelcast clustering has been removed due to security reasons. Please update the node configuration from the cloud console.");
				}
				else if ( provider.toString().equals(Provider.CLUSTER_ZEROCONF.name())) {
					instance = new com.cloud.cluster.zeroconf.ZeroClusterInstance(attributes);
				}
				else {
					//throw new IOException("Invalid cluster provider " + provider);
					log.error("Invalid cluster provider " + provider + ". Using default (Zeroconf)");
					instance = new com.cloud.cluster.zeroconf.ZeroClusterInstance(attributes);
				}
			}
			else {
				// Default (Zeroconf)
				//instance = new com.cloud.cluster.hazelcast.HzClusterInstance(attributes);
				instance = new com.cloud.cluster.zeroconf.ZeroClusterInstance(attributes);
			}
			masterTaskInit();
			masterTaskStart();
		}
	}
	

	/* Not needed
	private void configureLoggingService() {
		LogListener listener = new LogListener() {
			public void log( LogEvent event ) {
				log.debug(event.getMember().getUuid() + ": " + event.getLogRecord().getMessage());
			}
		};
		hazelcastInstance.getLoggingService().addLogListener(Level.INFO, listener);
	}*/
	
	/**
	 * Shut down the cluster. Invoke this when the application stops.
	 */
	public void shutdown() {
		masterTaskStop();
		
		if ( instance != null ) {
			instance.shutdown();
		}
		instance			= null;
	}

	public void setLocalMemberAttribute(String key, String value) {
		if ( instance != null ) {
			instance.setLocalMemberAttribute(key, value);
		}
	}
	
	/**
	 * Set the status of the local member: Online, Offline, Server error, etc..
	 * @param code HTTP status code: 200 = online, 503 = Service down, 401 = Auth failed, 500 = Server error. 
	 * @param message Status message.
	 */
	public /*static */ void setLocalMemberStatus (int code , String message) {
		if ( instance != null ) {
			instance.setLocalMemberStatus(code, message);
		}
	}
	
	/**
	 * Set local member OS metrics. This can be invoked many times by the member to update
	 * real time {@link OSMetrics}:
	 * <ol>
	 * <li>OS Name: Operating system name.
	 * <li>Number of CPUS.
	 * <li>Peak threads.
	 * <li>CPU load.
	 * <li>Free memory.
	 * </ul>
	 */
	public void setLocalMemberOSMetrics () {
		instance.setLocalMemberOSMetrics();
	}
	
	/**
	 * Inject OS metrics into a cluster member attributes. Invoked on cluster initialization.
	 * @param map Cluster member attributes where the metrics will be inserted.
	 */
	/* Moved to OSMetrics class
	public static void injectOSMetrics (Map<String, Object> map) {
		try {
			JSONObject os = OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);
			
			// Note: The metrics injected here must match setLocalMemberOSMetrics!
			map.put(OSMetrics.KEY_SYS_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_SYS_CPU)));
			map.put(OSMetrics.KEY_PROC_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_PROC_CPU)));
			map.put(OSMetrics.KEY_PEAK_THR , os.getInt(OSMetrics.KEY_PEAK_THR));
			map.put(OSMetrics.KEY_OS_NAME , os.getString(OSMetrics.KEY_OS_NAME));
			map.put(OSMetrics.KEY_NUM_CPUS , os.getInt(OSMetrics.KEY_NUM_CPUS));
			map.put(OSMetrics.KEY_FREE_MEM , os.getLong(OSMetrics.KEY_FREE_MEM));
			map.put(OSMetrics.KEY_HEAP_FREE , os.getLong(OSMetrics.KEY_HEAP_FREE));
			map.put(OSMetrics.KEY_HEAP_MAX , os.getLong(OSMetrics.KEY_HEAP_FREE));
			map.put(OSMetrics.KEY_HEAP_TOTAL , os.getLong(OSMetrics.KEY_HEAP_FREE));
			
		} catch (JSONException e) {
		}
	} */
	
	/**
	 * Get the group name this node belongs to.
	 * @return The cluster group (used for multi-cluster) support.
	 */
	public String getClusterGroupName() {
		return instance.getClusterGroupName();
	}

	/**
	 * Get a list of the HZ Tcp IP configuration members used for manual join (when multi-cast is disabled).
	 * @return A list of member IP addresses.
	 */
	public List<String> getClusterTcpMembers () {
		return instance.getClusterTcpMembers ();
	}

	public boolean isShutdown () {
		return instance.isShutdown();
	}
	
	public void dumpAllInstances(String label) {
		instance.dumpAllInstances(label);
	}
}
