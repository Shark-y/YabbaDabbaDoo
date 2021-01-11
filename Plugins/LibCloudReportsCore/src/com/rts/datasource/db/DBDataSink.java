package com.rts.datasource.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.db.JDBCClient;
import com.cloud.core.db.JDBCMetaData;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;
import com.rts.datasource.IDataSource.DataSourceType;

/**
 * JDBC Implementation of {@link IDataSource}
 * 
 * @author VSilva
 * @version 1.0.0 - 9/21/2017 Initial implementation.
 *
 */
public class DBDataSink implements IDataSource {
	private static final Logger log = LogManager.getLogger(DBDataSink.class);
	
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
	final JDBCClient db;
	
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
	public DBDataSink(final String name, final String description, final String driver, final String url, final String user, final String password, final String table, final String fields, final String refreshType) 
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
		// This will load the driver...
		this.db				= new JDBCClient(driver);
		//loadDriver();
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
	public DBDataSink(JSONObject ds ) throws JSONException, IOException {
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
		// This will load the driver.
		this.db				= new JDBCClient(driver);
	}

	@Override
	public void run() {
		try {
			log.debug("DB connect to [" + name + "] " + url + " " + user + "/" + password);
			db.connect(url, user, password);
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
		return 0; //matcher.getTotalBatches();
	}

	@Override
	public long getTotalRecords() {
		return 0; 
	}

	@Override
	public void setEventListener(IBatchEventListener l) {
		//this.listener = l;
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
		root.put("type", DataSourceType.DATASTORE.name());
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
		return DataSourceType.DATASTORE;
	}

	public String getTable () {
		return table;
	}
	
	/**
	 * Add or Update a document {@link JSONArray} in either of two formats:
	 * <ul>
	 * <li> Format #1: {@link JSONArray} of {@link JSONObject} [{K1:V1, K2:V2,...},{K1:v1, K2:V2,...}...] where each {@link JSONObject} represents a row of (FIELD, VALUE) pairs.
	 * <li> Format #2: {@link JSONArray} of {@link JSONArray} [ [V1,V2,..], [V1,V2,..]...]. Note: the values sequence must match the fields in the DB.
	 * </ul>
	 * @param table The Db table to update.
	 * @param doc The document properly formatted.
	 * @return A response message.
	 * @throws SQLException on DB errors.
	 * @throws JSONException on JSON parsing errors.
	 * @throws IOException If an DB update failed for row(i).
	 */
	public String upsert (String table, JSONArray doc) throws SQLException, JSONException, IOException {
		// online?
		if ( status.getStatus() != Status.ON_LINE) {
			throw new IOException("Upsert: " + name + " is not online. " + status);
		}
		// let's time this. Its taking too long in MSSQL
		long t0 	= System.currentTimeMillis();
		int[] rows 	= db.batchUpsert(table, doc);
		long t1 	= System.currentTimeMillis();
		
		// verify: Count failures [1,1,1,0,1,...]
		List<Integer> failed = new ArrayList<Integer>();
		
		for (int i = 0; i < rows.length; i++) {
			if ( rows[i] == 0) {
				failed.add(i);
			}
		}
		String result = table + " updated " + (rows.length - failed.size()) + " out of " + rows.length + " row(s)  in " + (t1 -t0) + " ms.";
		log.trace(name + "/" + result);
		
		if ( failed.size() == rows.length) {
			throw new IOException(table + " update failed for " + rows.length + " row(s).");
		}
		if ( failed.size() > 0) {
			log.warn(name + " failed to update " + failed.size() + " out of " + rows.length + " row(s). Indices are " + failed.toString());
		}
		return result ; //table + " updated " + (rows.length - failed.size()) + " out of " + rows.length + " row(s)  in " + (t1 -t0) + " ms.";
	}
	
	/**
	 * Delete a record by id (primary key).
	 * @param id
	 * @return The number of rows deleted.
	 * @throws IOException If id is null or the number of rows delete is 0.
	 * @throws SQLException on DB errors.
	 * @throws JSONException on JSON bugs.
	 */
	public int delete (String id) throws IOException, SQLException, JSONException {
		//if ( id == null ) 	throw new IOException("A document id is required.");
		if ( id == null ) {
			log.warn("Document id is NULL. Will delete all records in " + table);
		}
		final Object[] values 	= id != null ? new Object[] {id} : null;
		final String SQL 		= db.buildDeleteSQL(table, values);
		final int rows 			= db.update(SQL);
		
		if ( rows == 0) {
			throw new IOException("Failed to delete " + table  + "/" +(id != null ? id : "ALL") );
		}
		return rows;
	}

	/**
	 * List records (all by default) in a table.
	 * @param table Data base table.
	 * @return {@link JSONArray} of {@link JSONArray} [ [ROW1], [ROW2],...]
	 * @throws SQLException on any DB errors (bugs).
	 * @throws JSONException on JSON parse errors (bugs).
	 */
	public JSONArray get (final String table) throws SQLException, JSONException {
		JDBCResultSet rs = db.query("SELECT * FROM " + table);
		return rs.getResultSet();
	}

	/**
	 * Search by looking at all the fields in the table for key
	 * @param key Search key.
	 * @return JSON: { data : [ {ROW1] , [ROW2] .. ]} Where ROW = [F1,F2,...]
	 * @throws IOException If the search key is null.
	 * @throws SQLException if no manes can be extracted from table or any other SQL error.
	 * @throws JSONException on any JSON document error.
	 */
	public JSONObject search (final String key) throws IOException, SQLException, JSONException {
		if ( key == null )		throw new IOException("A search key is required.");
		
		final String[] cols  	= db.getColumnNames(table);
		boolean commate 		= false;
		StringBuffer condition 	= new StringBuffer();
		
		if ( cols.length == 0 )	throw new SQLException("Unable to get column names for table " + table + "@" + name);
		
		for (int i = 0; i < cols.length; i++) {
			if ( commate) {
				condition.append(" OR ");
			}
			condition.append(cols[i] + " LIKE '%" + key + "%'");
			commate = true;
		}
		//System.out.println("** COND=" + condition + " flds=" + Arrays.toString(cols));
		JDBCResultSet rs 		= db.query(table, condition.toString());
		JSONObject root 		= new JSONObject();

		root.put("data", rs.getResultSet());
		return root;
	}

	/**
	 * Validate {@link DBDataSink}
	 * <ul>
	 * <li> 9/24/2017 At least 1 primary key is required for table.
	 * </ul>
	 * @throws IOException If validation fails. A user friendly message is sent along.
	 */
	public void validate () throws IOException {
		try {
			JDBCMetaData meta 	= db.getMetaData();
			JSONArray pks 		= meta.getPrimaryKeys(meta.getCatalog(), null, table);
			
			// Validation #1 at least 1 PK is required
			if ( pks.length() == 0 ) {
				throw new IOException("At least 1 PRIMARY KEY is required for table " + table);
			}
		} catch (SQLException | JSONException e) {
			log.error("Validate", e);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s driver: %s", name, table, driver);
	}
	
	public String getUrl () {
		return url;
	}
	
	@Override
	public JSONObject getParams() {
		return new JSONObject();
	}

}
