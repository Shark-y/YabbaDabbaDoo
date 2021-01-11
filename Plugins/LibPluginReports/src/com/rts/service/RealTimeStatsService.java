package com.rts.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.XmlConfig;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.cloud.core.w3.WebClient;
import com.rts.core.EventQueue;
import com.rts.core.IBatchEventListener;
import com.rts.core.IDataService;
import com.rts.datasource.DataFormat;
import com.rts.datasource.DataSourceManager;
import com.rts.datasource.IDataSource;
import com.rts.datasource.IDataSource.DataSourceType;
import com.rts.datasource.ext.PrometheusDataSource;
import com.rts.ui.Dashboard;
import com.rts.ui.DashboardList;
import com.rts.ui.Threshold;
import com.rts.ui.ThresholdList;
import com.rts.ui.Dashboard.Branding;
import com.rts.ui.Dashboard.Metric;

/**
 * Real time statistics Generic service. This service is capable of listening from TCP clients
 * an matching raw socket data against a generic {@link DataFormat} to produce a JSON object.
 * 
 * Socket raw data like this:
 * <pre>
 * [MATCHER] F1|  74500|Eastern Sales|   2|:00||   8|58:54|  46|23:37|  54 val size:11 Fld Size:11 FSep:\|
 * [MATCHER] F1|  78808|Eastern Sales|   3|:00||   6|40:58|  11|56: 1|  99 val size:11 Fld Size:11 FSep:\|
 * [MATCHER] F1|  35611|Eastern Sales|   1|:00||   9|12:51|  78|23:45|  99 val size:11 Fld Size:11 FSep:\|
 * [MATCHER] F1|   3503|Eastern Sales|   7|:00||   3|46:40|  97|50: 0|  59 val size:11 Fld Size:11 FSep:\|
 * [MATCHER] F1|  55400|Eastern Sales|   4|:00||   7|30:46|  27|46:43|  80 val size:11 Fld Size:11 FSep:\|
 * [MATCHER] F1|   5292|Eastern Sales|   5|:00||   5|13:38|  33| 3:39|  87 val size:11 Fld Size:11 FSep:\|
 * [MATCHER] F1|  48870|Eastern Sales|   1|:00||   5|20: 3|  66|24:30|  39 val size:11 Fld Size:11 FSep:\|</pre>
 * 
 * Produces the JSON object
 * <pre>
 * {"batchDate":1451855183491,"batchData":[{"F1":"F1","VDN":"74500","ACDCALLS":"46","ABNCALLS":"8","INPROGRESS-ATAGENT":"2","AVG_ACD_TALK_TIME":"23:37","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"58:54","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"54"},{"F1":"F1","VDN":"78808","ACDCALLS":"11","ABNCALLS":"6","INPROGRESS-ATAGENT":"3","AVG_ACD_TALK_TIME":"56: 1","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"40:58","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"99"},{"F1":"F1","VDN":"35611","ACDCALLS":"78","ABNCALLS":"9","INPROGRESS-ATAGENT":"1","AVG_ACD_TALK_TIME":"23:45","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"12:51","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"99"},{"F1":"F1","VDN":"3503","ACDCALLS":"97","ABNCALLS":"3","INPROGRESS-ATAGENT":"7","AVG_ACD_TALK_TIME":"50: 0","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"46:40","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"59"},{"F1":"F1","VDN":"55400","ACDCALLS":"27","ABNCALLS":"7","INPROGRESS-ATAGENT":"4","AVG_ACD_TALK_TIME":"46:43","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"30:46","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"80"},{"F1":"F1","VDN":"5292","ACDCALLS":"33","ABNCALLS":"5","INPROGRESS-ATAGENT":"5","AVG_ACD_TALK_TIME":"3:39","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"13:38","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"87"},{"F1":"F1","VDN":"48870","ACDCALLS":"66","ABNCALLS":"5","INPROGRESS-ATAGENT":"1","AVG_ACD_TALK_TIME":"24:30","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"20: 3","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"39"}],"listenerName":"CVDN Table"}
 * </pre>
 * @author VSilva
 *
 */
public class RealTimeStatsService implements IServiceLifeCycle, IDataService {

	private static final Logger log 		= LogManager.getLogger(RealTimeStatsService.class);
	
	/** Listeners file name */
	static final String TCP_LISTENER_FNAME 	= "datasources";
	// 9/15/2017 static final String TCP_LISTENER_FNAME 	= "datasources.xml";

	/** Dashboards default file name */
	static final String DASHBOARDS_FNAME 	= "dashboards.json"; // xml";

	/** Thresholds IN JSON! */
	static final String THRESHOLDS_FNAME 	= "thresholds.json";
	
	/** Data sources mananger */
	private DataSourceManager dsManager; 
	
	/** Product base batch  ${user.home}/.cloud/CloudReports */
	private String basePath;
	
	/** dashboards */
	private DashboardList dashboard;
	
	/** thresholds (alerts) */
	private ThresholdList thresholds;
	
	/** Map used to track (count) dashboard windows */
	final private Map<String, Set<String>> clientTracker;
	
	/** Storage micro service */
//	private StorageService microService;
	
	/** Service status */
	private final ServiceStatus status = new ServiceStatus();
	
	/**
	 * Counts the number of web clients (dashes) by stata source & window ID.
	 * @param dataSourceName Data source name (required).
	 * @param windowId Window id (required).
	 */
	public void clientTrackerInit ( final String dataSourceName, final String windowId ) {
		if ( dataSourceName == null || windowId == null ) {
			return;
		}
		if ( clientTracker.containsKey(dataSourceName)) {
			clientTracker.get(dataSourceName).add(windowId);
		}
		else {
			Set<String> set = new HashSet<String>();
			set.add(windowId);
			clientTracker.put(dataSourceName, set);
		}
		log.debug("Client Tracker Init: " + dataSourceName + " added window Id " + windowId + " client count is " + clientTracker.get(dataSourceName).size());
	}
	
	/**
	 * Get the number of connected client dashboards [1 - n] where n is the # of clients..
	 * @param dataSourceName Data source name.
	 * @return Note; The count cannot be ZERO. When no dashboars are available will always be 1.
	 */
	/*private*/ int clientTrackerGetCount (final String dataSourceName) {
		return clientTracker.containsKey(dataSourceName) 
				// Size cannot be zero.
				? clientTracker.get(dataSourceName).size() > 0 ? clientTracker.get(dataSourceName).size() : 1
				: 1;
	}
	
	public void clientTrackerDecrement (final String dataSourceName, String windowId) {
		if ( !clientTracker.containsKey(dataSourceName)) {
			return;
		}
		Set<String> set = clientTracker.get(dataSourceName);
		set.remove(windowId);
		log.debug("Client Tracker Decrement: " + dataSourceName + " Removed Window Id " + windowId + " client count is " + set.size());
	}

	public void clientTrackerReset (final String dataSourceName) {
		clientTracker.remove(dataSourceName);
	}

	/**
	 * Batch Event receiver
	 */
	private IBatchEventListener listener = new IBatchEventListener() {
		/**
		 * @param batch JSON object of the form: {"batchFormat":{FORMAT},"batchDate":DATE,"batchData":[{ROW1}...]}. For example:
		 * <li>{"batchFormat":{"footer":"F3","recSep":"LF","storageFields":"","fieldSep":"\\|","header":"F0","fields":"SkillID,EWTLow,EWTMedium,EWTHigh,OldesCall"},"batchDate":1507116836701,"batchData":[{"EWTMedium":12,"EWTHigh":5,"SkillID":1,"EWTLow":86,"OldesCall":27},...],"listenerName":"Arvado2"}
		 */
		@Override
		public void onBatchReceived(JSONObject batch) {
			// a listener name is required!
			if ( !batch.has("listenerName")) {
				log.error("Unable to cosume batch: Missing data source name in batch: " + batch);
				return;
			}
			try {
				//log.debug("Got batch of len: " + batch.getJSONArray("batchData").length());
				final String dsName = batch.getString("listenerName");
				final int count		= clientTrackerGetCount(dsName);
				
				if ( count > 1) {
					log.debug("BATCH-RECEIVED: Pushing " + count + " batches (1/web client) for DS " + dsName);
				}
				
				for (int i = 0; i < count; i++) {
					EventQueue.push(dsName, batch);
				}
				// Invoke the Storage micro service
				final IDataSource ds = dsManager.getDataSource(dsName);
				
				if ( ds.getType() == DataSourceType.SOCKET_WITH_STORAGE) {
					throw new IOException("invalid storage type " + ds.getType());
					/* 10/18/2020
					if ( microService != null) {
						long date 		= batch.getLong ("batchDate");

						// { status; 200, message: 'OK'}
						JSONObject root = null;
						try {
							DataFormat format 	= ds.getFormat();
							boolean wipeTable	= format.getStorageOptWipeTable();
							
							log.trace("Upsert " + dsName + " Wipe Tbl: " + wipeTable + " Batch: " + batch);
							
							if ( wipeTable) {
								microService.deleteAll(dsName);
							}
							root = microService.upsert(dsName, String.valueOf(date), ServiceUtils.storageBuildBatch(batch, format) , null);
						
							if ( root.length() == 0 ) {
								log.error("Storage failed for " + dsName + " (response cannot be empty)");
							}
							else if ( root.getInt("status") >= 400 ) {
								log.error("Storage failed for " + dsName + " with " + root);
							}
						} catch (IOException e) {
							//e.printStackTrace();
							log.error("Storage failed for " + dsName + " with " + e.getMessage() + " " + microService.getEndPoint());
						}
					}
					else {
						// service not configured
						log.error("Storage Microservice not configured for data source " + dsName);
					}
					*/
				}
			} catch (Exception e) {
				//e.printStackTrace();
				log.error("Batch received: " + e.toString());
			}
		}
	};
	
	/**
	 * Construct
	 */
	public RealTimeStatsService() {
		clientTracker = new HashMap<String, Set<String>>();
	}
	
	/**
	 * Load data sources
	 * @param force If true force load
	 * @throws IOException
	 */
	private void loadDataSources (final boolean force) throws IOException {
		if ( dsManager != null && !force) {
			return;
		}
		dsManager = /*new DataSourceManager*/ DataSourceManager.getInstance(basePath, TCP_LISTENER_FNAME);
		dsManager.parse(basePath, TCP_LISTENER_FNAME);
		dsManager.setEventListener(listener);
	}
	
	/**
	 * Fires once on container start.
	 */
	@Override
	public void onServiceInitialize(ServiceContext context) throws IOException {
		// always load the listeners: from the file system or class path
		basePath = context.getProfileBasePath(); // getConfigurationBasePath();
		
		log.debug("Service Init: Using base path " + basePath);
		loadDataSources(false);
		
		dashboard 	= new DashboardList(basePath, DASHBOARDS_FNAME);
		
		// This will throw FileNotFoundEx if file missing
		thresholds	= new ThresholdList(basePath, THRESHOLDS_FNAME);
		
		// 12/25/2020 Do this on service start setupTasks();
	}

	private void setupTasks() {
		setupPrometheus();
	}
	
	private void setupPrometheus() {
		// Match PROMETHEUS DS(s) with dash metrics, else the Dashboards won't display
		List<IDataSource> promList = dsManager.getDataSources(DataSourceType.PROMETHEUS);

		for ( IDataSource ds : promList ) {
			// get dash
			List<Dashboard> dashes = dashboard.findByDataSource(ds.getName());
			
			for ( Dashboard dash : dashes) {
				final String range  	= dash.getKeyRange();
				List<Metric> metrics 	= dash.getMetrics();
				
				for ( Metric metric : metrics) {
					// container_cpu_load_average_10s{namespace=\"westlake-dev\"}
					final String query = range != null & !range.isEmpty() ? metric.getName() + "{" + range + "}" : metric.getName(); 
					((PrometheusDataSource)ds).addMetric (dash.getTitle(), query);
				}
			}
		}
	}
	
	/**
	 * Fires multiple times from the admin console start.
	 */
	@Override
	public void onServiceStart(ServiceContext context) throws IOException {
		final String path 	= context.getProfileBasePath();
		final String file	= context.getServiceConfigurationFile(ServiceType.DAEMON);
		
		final IConfiguration conf = new XmlConfig(path, file);
		
		// event queue exp intervals
		final String s1 = conf.getProperty("rts01_evExp");		// Event exp interval
		final String s2 = conf.getProperty("rts02_gcInt");		// Garbage collection interval
		final String s3	= conf.getProperty("rts03_mss");		// Storage micro service EP
		
		// Micro services client read timeouts (may be null)
		final String s4	= conf.getProperty("rts04_mss");		// Storage micro service read timeout (may be null)
		final String s5	= conf.getProperty("rts05_mss");		// Storage micro service connect timeout (optional)
		
		// Initialize the event queue interval (from the config)
		if ( s1 != null && s2 != null) {
			log.debug("Cloud Reports start: Evnt exp int: " + s1 + " Garbage collection int: " + s2);
			EventQueue.initialize(Integer.valueOf(s1), Integer.valueOf(s2));
		}
		else {
			// no config? Init anyway with defaults
			log.debug("Cloud Reports start: No event queue defaults in config. Initializing event queue with defaults.");
			EventQueue.initialize();
		}
		// load in case the node is not configured
		dsManager.startAll();
		
		// misc setup @ startup
		setupTasks();
		
		status.setStatus(Status.ON_LINE, "Online");
	}

	@Override
	public void onServiceStop() {
		if ( dsManager != null) {
			dsManager.stopAll();
		}
		// clear the event queue
		EventQueue.clear();
		status.setStatus(Status.OFF_LINE, "Offline");
	}

	/**
	 * Fires once on container shutdown.
	 */
	@Override
	public void onServiceDestroy() {
		if ( dsManager != null) {
			dsManager.shutdownAll();
		}
	}

	@Override
	public ServiceStatus getServiceStatus() {
		return status;
	}

	@Override
	public void onServiceValidateConfig(IConfiguration config) throws Exception {
		// validate storage URL
		final String url	= config.getProperty("rts03_mss");		// Storage micro service EP
		
		if ( url != null ) {
			WebClient wc 	= new WebClient(url);
			wc.doGet();
			if ( wc.getStatus() >= 400) {
				throw new IOException("Invalid storage URL " + url);
			}
		}
	}

	@Override
	public ServiceType getServiceType() {
		return ServiceType.DAEMON;
	}

	public DataSourceManager getDataSourceManager() {
		return dsManager;
	}

	public IDataSource getDataSource (final String name) {
		return dsManager.getDataSource(name);
	}
	
	public DashboardList getDashboards () {
		return dashboard;
	}
	
	public void saveDashboardList() throws IOException {
		dashboard.save();
	}

	public Dashboard addDashboard (final String title, final String listener, final String key, final String keyRange, final String heading, final List<Metric> metrics, final Branding branding) 
			throws IOException 
	{
		// If the dash exists, this will remove first, then add it again.
		Dashboard db 	= dashboard.addDashboard(title, listener, key, keyRange, heading, metrics, branding);
		IDataSource ds 	= dsManager.getDataSource(db.getListener());
		
		if ( ds.getType() == DataSourceType.PROMETHEUS )  {
			List<Metric> metrix 	= db.getMetrics();
			final String range  	= db.getKeyRange();

			for ( Metric m : metrix) {
				// container_cpu_load_average_10s{namespace=\"westlake-dev\"}
				final String query = range != null & !range.isEmpty() ? m.getName() + "{" + range + "}" : m.getName(); 

				((PrometheusDataSource)ds).addMetric(db.getTitle(), query);
			}
		}
		return db;
	}

	public boolean removeDashboard (final String title) throws IOException {
		Dashboard db 	= dashboard.find(title);
		IDataSource ds 	= dsManager.getDataSource(db.getListener());
		
		if ( ds.getType() == DataSourceType.PROMETHEUS )  {
			List<Metric> metrix = db.getMetrics();
			for ( Metric m : metrix) {
				((PrometheusDataSource)ds).removeMetric(db.getTitle(), m.getName());
			}
		}
		
		final boolean ok = dashboard.remove(title);
		return ok;
	}
	
	public void addSocketListener (DataSourceType subType, final String name, final int port, final String description, final DataFormat fmt) throws IOException {
		dsManager.addSocketListener(subType, name, port, description, fmt, listener);
	} 
	
	public void saveDataSources () throws IOException {
		dsManager.save();
	}
	
	public void removeDataSource (String name) throws IOException {
		dsManager.removeDataSource(name);
	}
	
	public void stopDataSource (String name) throws IOException {
		dsManager.stop(name);
	}

	public void startDataSource (String name) throws IOException {
		dsManager.start(name);
	}

	public ThresholdList getThresholdList() {
		return thresholds;
	}
	
	public void addThreshold(Threshold t) throws IOException {
		thresholds.addThreshold(t);
	}
	
	public void saveThresholdList() throws Exception {
		thresholds.save();
	}
	
	public boolean removeThreshold (final String listener, final String metric) {
		return thresholds.remove(listener, metric);
	}
	
	/**
	 * Get the {@link Threshold} id for a given {@link Metric}.
	 * @param m The {@link Metric}.
	 * @param dataSource Data source name bound to the metric.
	 * @param dashboard Optional {@link Dashboard} name the metric belongs to.
	 * @return The threshold id for this metric (if available) else returns an empty string.
	 */
	public String getThresholdId (final String metricName , final String dataSource, final String dashboard) {
		// Check 4 global thresh
		String id = metricName + "@" + dataSource;
		for ( Threshold tr : thresholds) {
			if ( tr.getId().equals(id)) {
				return id;
			}
		}
		// By dashboard
		id = metricName + "@" + dataSource + "@" + dashboard;
		for ( Threshold tr : thresholds) {
			if ( tr.getId().equals(id)) {
				return id;
			}
		}
		return "";
	}
	
	/**
	 * Validate a {@link Dashboard} by checking for:
	 * <ul>
	 * <li> Valid data source.
	 * <li> Valid display KEY.
	 * <li> Valid metrics.
	 * </ul>
	 * @param dash {@link Dashboard} to check. Dashboards may become invalid of the data source fields are changed after the dashboard is created.
	 * @return NULL if valid else an error message.
	 * @throws JSONException 
	 */
	public String validate( final Dashboard dash, boolean force) throws JSONException {
		// check 4 valid data source (listener)
		IDataSource ds = dsManager.getDataSource(dash.getListener());
		if (  ds == null ) {
			return "Invalid data source " + dash.getListener();
		}
		
		try {
			ServiceUtils.validateDashboard(ds, dash, force);
		} catch (IOException e) {
			return e.getMessage();
		}
		// All OK.
		return null;
	}

	@Override
	public DataServiceType getDataServiceType() {
		return DataServiceType.DATASOURCE;
	}
	
	public IBatchEventListener getEventListener() {
		return listener;
	}
}
