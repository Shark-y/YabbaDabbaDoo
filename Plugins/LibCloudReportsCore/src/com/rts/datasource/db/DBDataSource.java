package com.rts.datasource.db;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.db.Database;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudExecutorService;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataMatcher;
import com.rts.datasource.IDataSource;
import com.rts.datasource.IDataSource.DataSourceType;

/**
 * JDBC Implementation of {@link IDataSource}
 * 
 * @author VSilva
 * @version 1.0.0 - 9/21/2017 Initial implementation.
 *
 */
public class DBDataSource implements IDataSource {
	private static final Logger log = LogManager.getLogger(DBDataSource.class);
	
	final String name;
	final String description;
	final String driver;
	final String url;
	final String user;
	final String password;
	final String table;
	final String refreshInterval;

	/** {@link DataFormat} contains the SQL fields to query */
	final DataFormat format;

	/** Status of the service */
	final ServiceStatus status 	= new ServiceStatus();

	/** Database client */
	final Database db 			= Database.getInstance();
	
	/** Batch / DS descriptor matcher */
	final IDataMatcher matcher	= new DBDataMatcher();
	
	/** Event receiver - after the raw sock data is matched agains the {@link DataFormat} to create a JSON object. */
	private IBatchEventListener listener;

	/** Used to manage the repetitive table read task */ 
	private ScheduledFuture<?> taskFuture;
	
	/**
	 * Construct a DB data source.
	 * @param name Data source name.
	 * @param description DS description.
	 * @param driver JDBC Driver class name. For example: com.mysql.jdbc.Driver
	 * @param url The JDB URL. For example: jdbc:mysql://localhost:3306/metrics
	 * @param user DB user.
	 * @param password DB password.
	 * @param table The SQL table bound to this data source.
	 * @param fields The SQL fields from the table above.
	 * @param refreshType Poll interval at which to read from the table (in ms). ZERO means read once only.
	 * @throws IOException if loading the JDBC driver fails.
	 */
	public DBDataSource(final String name, final String description, final String driver, final String url, final String user, final String password, final String table, final String fields, final String refreshType) 
			throws IOException 
	{
		super();
		this.name			= Objects.requireNonNull(name, "Datasource name cannot be null.");
		this.description	= Objects.requireNonNull(description, "Datasource description cannot be null.");
		this.driver 		= Objects.requireNonNull(driver, "DB Driver cannot be null.");
		this.url 			= Objects.requireNonNull(url, "DB URL cannot be null.");
		this.user 			= Objects.requireNonNull(user, "DB User cannot be null.");
		this.password 		= Objects.requireNonNull(password, "DB Pwd cannot be null.");
		this.table 			= Objects.requireNonNull(table, "DB Table cannot be null.");
		//this.fields 	= Objects.requireNonNull(fields, "DB fields cannot be null.");
		this.refreshInterval 	= Objects.requireNonNull(refreshType, "DB refresh type cannot be null.");
		this.format			= new DataFormat("", "", ",", "\n", Objects.requireNonNull(fields, "DB fields cannot be null."), null);
		loadDriver();
	}

	/**
	 * <pre>{ "description": "Foo",
  "name": "Foo",
  "params": {
   "driver": "com.mysql.jdbc.Driver",
   "table": "vdn",
   "password": "Thenewcti1",
   "user": "cloud",
   "refreshType": "0",
   "url": "jdbc:mysql://localhost:3306/metrics",
   "fields": "VDN,NAME,CALLS_WAITNG,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS"
  },
  "type": "DATABASE"
 }</pre>
	 * @param ds JSON above.
	 * @throws JSONException
	 * @throws IOException 
	 */
	public DBDataSource(JSONObject ds ) throws JSONException, IOException {
		this.name			= ds.getString("name");
		this.description	= ds.optString("description");
		JSONObject params 	= ds.getJSONObject("params");
		this.driver 		= params.getString("driver");
		this.url 			= params.getString("url");
		this.user 			= params.getString("user");
		this.password 		= params.getString("password");
		this.table 			= params.getString("table");
		this.refreshInterval = params.getString("refreshType");
		this.format			= new DataFormat("", "", ",", "\n", params.getString("fields"), null);
		loadDriver();
	}

	private void loadDriver () throws IOException {
		// load the DB driver
		log.debug("Load DB driver " + driver);
		db.initialize(driver);
	}
	
	@Override
	public void run() {
		// refresh interval < 10000 exec once else repeat
		int interval = Integer.parseInt(refreshInterval);
		
		Runnable command = new Runnable() {
			public void run() {
				String SQL = String.format("SELECT %s FROM %s", format.getFields(), table);
				//System.out.println("TICK " + name + " sql:" + SQL);
				try {
					JDBCResultSet rs = db.query(SQL);
					matcher.matchFormat(name, rs.getResultSet().toString(), format, listener);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		};
		
		try {
			log.debug("DB connect " + url + " " + user + "/" + password);
			db.connect(url, user, password);
			
			if ( interval < 10000) {
				taskFuture = CloudExecutorService.schedule(command, interval, TimeUnit.MILLISECONDS);
			}
			else {
				taskFuture = CloudExecutorService.scheduleAtFixedRate(command, 0, interval, TimeUnit.MILLISECONDS);
			}
			status.setStatus(Status.ON_LINE, "Connected.");
		} catch (Exception e) {
			// handle unchecked exceptions
			log.error(e.getMessage(), e);
			status.setStatus(Status.SERVICE_ERROR, e.getMessage());
		}
	}

	@Override
	public void stop() {
		db.disconnect();
		if ( taskFuture != null ) {
			taskFuture.cancel(true);
		}
		status.setStatus(Status.OFF_LINE, "");
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public DataFormat getFormat() {
		return format;
	}

	@Override
	public ServiceStatus getStatus() {
		return status;
	}

	@Override
	public long getTotalBatches() {
		return matcher.getTotalBatches();
	}

	@Override
	public long getTotalRecords() {
		return matcher.getTotalRecords();
	}

	@Override
	public void setEventListener(IBatchEventListener l) {
		this.listener = l;
	}

	@Override
	public String toXML() throws IOException {
		throw new IOException("toXML() not implemented.");
	}

	/**
	 * Convert to JSON:
	 * <pre>{  "description": "Foo",
  "name": "Foo",
  "params": {
   "driver": "com.mysql.jdbc.Driver",
   "table": "vdn",
   "password": "Thenewcti1",
   "user": "cloud",
   "refreshType": "0",
   "url": "jdbc:mysql://localhost:3306/metrics",
   "fields": "VDN,NAME,CALLS_WAITNG,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS"
  },
  "type": "DATABASE"
 } </pre>
	 */
	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject root = new JSONObject();
		root.put("type", DataSourceType.DATABASE.name());
		root.put("name", name);
		root.putOpt("description", description);

		JSONObject params = new JSONObject();
		params.put("driver", driver);
		params.put("url", url);
		params.put("user", user);
		params.put("password", password);
		params.put("table", table);
		params.put("fields",  format.getFields()); // fields
		params.put("refreshType", refreshInterval);
		
		root.put("params", params);
		
		return root;
	}

	/**
	 * @return See {@link DataSourceType}.
	 */
	public DataSourceType getType() {
		return DataSourceType.DATABASE;
	}

	@Override
	public JSONObject getParams() {
		return new JSONObject();
	}

}
