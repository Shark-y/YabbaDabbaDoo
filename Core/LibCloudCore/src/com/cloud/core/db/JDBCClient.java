package com.cloud.core.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Enumeration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.db.JDBCMetaData;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * A generic JDBC client that can be used to perform any type of SQL query against any database.
 * <ul>
 * <li> SQL queries are translated into JSON.
 * </ul>
 * <pre>String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
 * String hostName = "192.168.45.48";
 * String dbName = "lab90_awdb";
 * String userName ="sa";
 * String password = "Thenewcti1";
 * String url = "jdbc:sqlserver://"+ hostName + ";databaseName=" + dbName;
 * String SQL = "select sg.PeripheralName as SkillGroupName,sg.SkillTargetID as SkillGroupID,sgrt.RouterCallsQNow as SessionsInQueue,case when sgrt.CallsAnsweredTo5 = 0 then 0 "
 *  + "else sgrt.AnswerWaitTimeTo5/sgrt.CallsAnsweredTo5 end as QueueWaitTime,sgrt.CallsHandledTo5/300 as Throughput,sgrt.LoggedOn as QueueOnline "
 *  + "from t_Skill_Group_Real_Time sgrt, t_Skill_Group sg where sgrt.SkillTargetID = sg.SkillTargetID "; 
 * JDBCClient c = new JDBCClient(driver);
 * c.connect(url, userName, password);
 * System.out.println(c.query(SQL).toJSON().toString(1));
 * c.disconnect(); 
 * c.shutdown();
 * </pre>
 * <h2>Change Log</h2>
 * <ul>
 * <li> 03/10/2017 - v1.0.2 Added {@link JDBCMetaData} to get connection information.
 * <li> ??/??/2016 - v1.0.1 Initial commit.
 * </ul>
 * @author vsilva
 * @version 1.0.2
 * @version 1.0.3 - 9/23/2017 Added new upsert (UPDATE or INSERT) code.
 *
 */
public class JDBCClient {
	
	static final Logger log = LogManager.getLogger(JDBCClient.class);
			
	static void LOGD(String text) {
		log.debug("[JDBC] " + text);
	}

	static void LOGE(String text, Exception ex) {
		log.error("[JDBC] " + text, ex);
	}

	static void LOGE(String text) {
		log.error("[JDBC] " + text);
	}

	static void LOGT(String text) {
		log.trace("[JDBC] " + text);
	}

	/** Single JDBC {@link Connection} */
	Connection conn;
	
	/** DB Meta data: catalogs, schemas, tables, etc. */
	private JDBCMetaData meta;
	
	/**
	 * Construct.
	 * @param driverName JDBC driver class. For example: com.microsoft.sqlserver.jdbc.SQLServerDriver
	 * @throws IOException
	 */
	public JDBCClient(String driverName) throws IOException {
		/*
		 * Load the driver. The container will unload this automatically on destroy.
		 */
		try {
			LOGD("Load JDBC driver " + driverName);
        	Class.forName(driverName);
		} catch (Exception e) {
			throw new IOException("Failed to load JDBC driver " + driverName );
		}
	}
	
	/**
	 * Single Connection to the DB.
	 * @param connectionURL Something like jdbc:sqlserver://localhost;databaseName=HELLODB"
	 * @param userName User name.
	 * @param password User password.
	 * @throws IOException if something bad occurs.
	 */
	public void connect ( String connectionURL, String userName, String password) throws IOException {
    	try {
    		// Must load the driver first
    		LOGD("Connect to " + connectionURL + " as " + userName);
			conn 		= DriverManager.getConnection(connectionURL, userName, password);
			meta		= new JDBCMetaData(conn);
		} catch (SQLException e) {
			throw new IOException("DB Initialize: " + e.toString());
		}
	}
	
	/**
	 * Disconnect from the inner database.
	 */
	public void disconnect () {
		try {
			if ( conn != null ) {
				LOGD("Disconnect from catalog " + conn.getCatalog());
				conn.close();
			}
			conn = null;
		} catch (SQLException e) {
			LOGE("Close Connection " + e.toString());
		}
	}
	
	/**
	 * Shutdown the {@link JDBCClient}. This method will:
	 * <ul>
	 * <li>Unregister all drivers.
	 * </ul>
	 */
	public void shutdown () {
		unloadDrivers();
	}
	
	/**
	 * De-register JDBC drivers in this context's ClassLoader.
	 */
	public static void unloadDrivers() {
		// Now deregister JDBC drivers in this context's ClassLoader:
	    // Get the webapp's ClassLoader
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    
		 // Loop through all drivers
	    Enumeration<Driver> drivers = DriverManager.getDrivers();
	    
	    while (drivers.hasMoreElements()) {
	        Driver driver = drivers.nextElement();
	        
	        if (driver.getClass().getClassLoader() == cl) {
	            // This driver was registered by the webapp's ClassLoader, so deregister it:
	            try {
	                LOGD("Unregister JDBC driver " + driver.getClass().getCanonicalName());
	                DriverManager.deregisterDriver(driver);
	            } 
	            catch (SQLException ex) {
	                // The container will take care of unloading if this fails!
	            }
	        } 
	    }		
	}
	
	/**
	 * Perform a SQL query and return the results as JSON.
	 * <pre>
	 * {
 "fetchSize": 128,
 "resultSet": [
  [
   "000047561914",
   5001,
   0,
   null,
   null,
   0
  ], ...
 ],
 "metaData": {
  "columns": [
   {
    "typeName": "varchar",
    "name": "SkillGroupName",
    "className": "java.lang.String",
    "displaySize": 32,
    "label": "SkillGroupName"
   }, ...
  ],
  "colCount": 6
 }
} </pre>
	 * @param SQL Query.
	 * @return See the JSON format.
	 * @throws SQLException If a DB error occurs.
	 * @throws JSONException If a JSON error occurs.
	 */
	public JDBCResultSet query ( final String SQL) throws SQLException, JSONException {
		PreparedStatement stmt	= null;
		ResultSet rs			= null;
		JDBCResultSet set		= null;
		long t0					= System.currentTimeMillis();
		
		try {
			if ( isClosed()) {
				throw new SQLException("Query DB: connection is closed.");
			}
			
			stmt 				= conn.prepareStatement(SQL);
			rs 					= stmt.executeQuery();
			set 				= new JDBCResultSet(rs);
		}
		finally {
			/* If we close the statement then the result set will close too giving an error
			 * when toJSON is invoked */
			if ( stmt != null ) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
			if ( rs != null ) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			
			long t1 		= System.currentTimeMillis();
			long latency 	= (t1 - t0);
			if (latency > 0 ) {
				LOGD("Executed [" + (SQL.length() < 60 ? SQL : SQL.substring(0, 60) + "...") + "] in " + latency + " ms." );
			}
		}
		return set;
	}
	
	/**
	 * Perform an SQL update: INSERT, UPDATE or DELETE
	 * @param SQL Update SQL: INSERT, UPDATE or DELETE
	 * @return Number of records updated.
	 * @throws SQLException on DB errors.
	 */
	public int update ( String SQL) throws SQLException {
		Statement stmt 	= null;
		long t0			= System.currentTimeMillis();
		int rows		= -1;
		
		try {
			if ( isClosed() ) {
				throw new SQLException("Update DB: connection is closed.");
			}
			stmt 	= conn.createStatement();
			rows 	= stmt.executeUpdate(SQL);
			long t1	= System.currentTimeMillis();

			LOGD("Executed UPDATE [" + (SQL.length() < 60 ? SQL : SQL.substring(0, 60) + "...") + "] in " + (t1 - t0) + " ms." );
			return rows;
		}
		finally {
			if ( stmt != null ) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
		}
	}
	
	/**
	 * Check for a closed connection.
	 * @return True if the {@link Connection} is NULL or closed.
	 */
	public boolean isClosed() {
		try {
			return conn == null || conn.isClosed();
		} catch (SQLException e) {
			log.error("Connection is closed: " + e.toString());
			return true;
		}
	}
	
	/**
	 * Get database meta data for the DB connection.
	 * @return See {@link JDBCMetaData} for details.
	 */
	public JDBCMetaData getMetaData() {
		return meta;
	}
	
	/**
	 * Insert or Update a table using arrays of column names and values. <b>Note: The order and size of both arrays must be equivalent.</b>
	 * @param table Table name.
	 * @param fields Array of column names.
	 * @param values Array of column values. Must match fields in size and order.
	 * @return Number of rows updated.
	 * @throws SQLException on DB error or update failure (the number of updated rows == 0).
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3.
	 */
	public int upsert ( String table, String[] fields, Object[] values) throws SQLException, JSONException {
		String condition = buildDefaultCondition(table, values);
		String SQL = exists(table, condition) 
				? buildUpdateSQL(table, fields, values, condition) 
				: buildInsertSQL(table, values);
		int rows 			= update(SQL);
		if ( rows == 0 )	throw new SQLException("Upsert failed: " + SQL);
		return rows;
	}

	/**
	 * Insert or Update a table using an array of values.
	 * @param table Table name.
	 * @param values Java array of values. <b>The element order must match the order of the fields in the table.</b> 
	 * @return Number of rows updated.
	 * @throws SQLException on DB error or update failure (the number of updated rows == 0).
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public int upsert ( String table, Object[] values) throws SQLException, JSONException {
		String condition = buildDefaultCondition(table, values);
		String SQL = exists(table, condition) 
				? buildUpdateSQL(table, values, condition) 
				: buildInsertSQL(table, values);
		int rows 			= update(SQL);
		if ( rows == 0 )	throw new SQLException("Upsert failed: " + SQL);
		return rows;
	}

	/**
	 * Build a default SQl condition by looking at all the primary keys in a table.
	 * @param table DB table to inspect.
	 * @param values The values to assign to the primary keys.
	 * @return PK1 = VAL1 AND PK2 = VAL2
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public String buildDefaultCondition ( String table, Object[] values) throws SQLException, JSONException {
		JDBCMetaData meta 	= getMetaData();

		// [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
		JSONArray keys = meta.getPrimaryKeys(meta.getCatalog(), null, table);

		StringBuffer buf = new StringBuffer();
		
		for (int i = 0; i < keys.length(); i++) {
			JSONObject pk 	= keys.getJSONObject(i);
			String pkName 	= pk.getString("COLUMN_NAME");

			// [{"SS_DATA_TYPE":39,"TABLE_NAME":"AGENTS","SS_IS_SPARSE":0,"CHAR_OCTET_LENGTH":30,"SS_IS_COLUMN_SET":0,"TABLE_SCHEM":"dbo","BUFFER_LENGTH":30,"NULLABLE":0,"IS_NULLABLE":"NO","TABLE_CAT":"tempdb","SQL_DATA_TYPE":12,"COLUMN_SIZE":30,"TYPE_NAME":"varchar","IS_AUTOINCREMENT":"NO","COLUMN_NAME":"userName","ORDINAL_POSITION":1,"SS_IS_COMPUTED":0,"DATA_TYPE":12}, ...]
			// need the SQL type too
			JSONArray cols 	= meta.getColumns(meta.getCatalog(), null, table, pkName);
			int dataType	= cols.getJSONObject(0).getInt("DATA_TYPE");
			boolean quote 	= JDBCUtil.needsQuotes(dataType);	
			
			buf.append(pkName + " = " + ( quote ? "'" : "") + values[i] + ( quote ? "'" : "")  + " AND ");
		}
		// no primary keys, use all cols: COL1 = VAL1 AND COL2 = VAL2 ...
		if ( keys.length() == 0 ) {
			String[] colNames = getColumnNames(table);
			for (int i = 0; i < colNames.length; i++) {
				JSONArray cols 	= meta.getColumns(meta.getCatalog(), null, table, colNames[i]);
				int dataType	= cols.getJSONObject(0).getInt("DATA_TYPE");
				boolean quote 	= JDBCUtil.needsQuotes(dataType);	
				
				buf.append(colNames[i] + " = " + ( quote ? "'" : "") + values[i] + ( quote ? "'" : "")  + " AND ");
			}
		}
		// remove last AND
		if ( buf.length() > 0) {
			buf.delete(buf.length() - 5, buf.length() - 1);
		}
		return buf.toString();
	}
	
	/**
	 * Test if a record exists in some table for a given condition. 
	 * @param table DB table name.
	 * @param condition SQL Condition to test for. For example: SSN = '123' AND name = 'John' (without a WHERE clause).
	 * @return True if a record exists (the result-set > 0) for that condition.
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException On JSON parse errors.
	 * @since 1.0.3
	 */
	public boolean exists(String table, String condition) throws SQLException, JSONException  {
		JDBCResultSet rs 	= query(table, condition);
		int len 			= rs.getResultSet().length();
		return len > 0;
	}

	/**
	 * Execute a query by table name.
	 * @param table Table name.
	 * @param condition A query condition. For example: field1 = 'value1' AND fields2 = val2
	 * @return A {@link JDBCResultSet}.
	 * @throws SQLException  if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException On JSON parse errors.
	 * @since 1.0.3
	 */
	public JDBCResultSet query(final String table, final String condition) throws SQLException, JSONException {
		String SQL = String.format("SELECT * FROM %s", table);
		if ( condition != null && !condition.isEmpty() ) SQL += " WHERE " + condition;
		return query(SQL);
	}

	public String buildDeleteSQL (String table, Object[] values) throws SQLException, JSONException {
		return "DELETE FROM " + table + (values != null ?  " WHERE " + buildDefaultCondition(table, values) : "");
	}

	/**
	 * Dynamically create an INSERT statement from the table meta data.
	 * @param table Table name.
	 * @param values Array of values for all the table fields.
	 * @return INSERT INTO AGENTS VALUES ( 'user', 'pwd', 'WIN32', 'name', 'email1', 'av1')
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public String buildInsertSQL (String table, Object[] values) throws SQLException, JSONException {
		String catalog 		= getMetaData().getCatalog();
		JSONArray cols 		= getMetaData().getColumns(catalog, null, table, null);
		StringBuffer buf 	= new StringBuffer("INSERT INTO "  + table + " VALUES (");
		
		if ( values.length != cols.length()) {
			throw new SQLException("Values of size " + values.length + " " + Arrays.toString(values) + " mismatch for column meta-data size: " + cols.length());
		}
		for (int i = 0; i < cols.length() ; i++) {
			JSONObject obj 	= cols.getJSONObject(i);
			int dataType	= obj.getInt("DATA_TYPE");
			boolean quote 	= JDBCUtil.needsQuotes(dataType);	

			buf.append(" " + ( quote ? "'" : "") + values[i] + ( quote ? "'" : "")  + ",");
		}
		// remove last comma
		buf.deleteCharAt(buf.length() - 1);
		buf.append(")");
		return buf.toString();
	}

	/**
	 * Build an UPDATE SQL statement.
	 * @param table Table name.
	 * @param values values Array of values for all the table fields.
	 * @param condition Update SQL condition. For example PK = val.
	 * @return UPDATE AGENTS SET  name = 'name1', password = 'pwd1' WHERE userName = 'user'
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public String buildUpdateSQL (String table, Object[] values, String condition) throws SQLException, JSONException {
		String[] columns	= getColumnNames(table); 
		return buildUpdateSQL(table, columns, values, condition);
	}
	
	/**
	 * Build an UPDATE SQL statement.
	 * @param table Table name.
	 * @param columns Array of names for all the table fields.
	 * @param values values Array of values for all the table fields.
	 * @param condition Update SQL condition. For example PK = val.
	 * @return UPDATE AGENTS SET  name = 'name1', password = 'pwd1' WHERE userName = 'user'
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public String buildUpdateSQL (String table, String[] columns, Object[] values, String condition) throws SQLException, JSONException {
		StringBuffer buf 	= new StringBuffer("UPDATE "  + table + " SET ");
		JDBCMetaData meta 	= getMetaData();
		
		if ( condition == null) {
			// Use the primary keys to derive a condition using the values
			condition = buildDefaultCondition(table, values);
		}
		
		// get all columns for the table
		//JSONArray cols 	= meta.getColumns(db.getMetaData().getCatalog(), null, table, null);
		for (int i = 0; i < columns.length ; i++) {
			String name 	= columns[i]; 

			// need the type
			// [{"SS_DATA_TYPE":39,"TABLE_NAME":"AGENTS","SS_IS_SPARSE":0,"CHAR_OCTET_LENGTH":30,"SS_IS_COLUMN_SET":0,"TABLE_SCHEM":"dbo","BUFFER_LENGTH":30,"NULLABLE":0,"IS_NULLABLE":"NO","TABLE_CAT":"tempdb","SQL_DATA_TYPE":12,"COLUMN_SIZE":30,"TYPE_NAME":"varchar","IS_AUTOINCREMENT":"NO","COLUMN_NAME":"userName","ORDINAL_POSITION":1,"SS_IS_COMPUTED":0,"DATA_TYPE":12},...]
			JSONArray cols 	= meta.getColumns(getMetaData().getCatalog(), null, table, name);
			int dataType	= cols.length() == 0 ? Types.VARCHAR : cols.getJSONObject(0).getInt("DATA_TYPE");
			boolean quote 	= JDBCUtil.needsQuotes(dataType);	
			
			if ( i < values.length) {
				buf.append(" " +  name +  " = " + ( quote ? "'" : "") + values[i] + ( quote ? "'" : "") + ",");
			}
		}
		// remove last comma
		buf.deleteCharAt(buf.length() - 1);
		buf.append(" WHERE " + condition);
		return buf.toString();
	}

	/**
	 * Get column names for a given table.
	 * @param table Table name.
	 * @return Java array: [ COL1, COL2,...]
	 * @throws SQLException  if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public String[] getColumnNames( String table) throws SQLException, JSONException {
		JDBCMetaData meta 	= getMetaData();
		JSONArray array 	= meta.getColumns(meta.getCatalog(), null, table, null);
		String[] names 		= new String[array.length()];
		for (int i = 0; i < names.length; i++) {
			JSONObject obj = array.getJSONObject(i);
			names[i] = obj.getString("COLUMN_NAME");
		}
		return names;
	}
	
	/**
	 * Batch upsert a JSON batch using either of to formats:
	 * <ul>
	 * <li> Format #1: {@link JSONArray} of {@link JSONObject} [{K1:V1, K2:V2,...},{K1:v1, K2:V2,...}...] where each {@link JSONObject} represents a row of (FIELD, VALUE) pairs.
	 * <li> Format #2: {@link JSONArray} of {@link JSONArray} [ [V1,V2,..], [V1,V2,..]...]. Note: the values sequence must match the fields in the DB.
	 * </ul>
	 * @param table The DB table to update.
	 * @param batch The JSON document as a {@link JSONArray} of {@link JSONObject} or {@link JSONArray} of {@link JSONArray}..
	 * @return Array of integers (updated counts per row).
	 * @throws SQLException on DB errors.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public int[] batchUpsert ( String table, JSONArray batch) throws SQLException, JSONException {
		return JDBCUtil.batchUpsert(this, table, batch);
	}
	
	/**
	 * Commit a transaction to the data bse.
	 * @throws SQLException on DB errors.
	 * @since 1.0.3
	 */
	public void commit() throws SQLException {
		conn.commit();
	}

	/**
	 * Enable/Disable the data base auto commit feature (defaults to true). Used to speed up transactions
	 * @param val Commit value.
	 * @throws SQLException on DB errors.
	 * @since 1.0.3
	 */
	public void setAutoCommit( boolean val) throws SQLException {
		conn.setAutoCommit(val);
	}

	/**
	 * The the value for a column meta data key: [{ "SS_DATA_TYPE": 39, "TABLE_NAME": "AGENTS", "SS_IS_SPARSE": 0, "CHAR_OCTET_LENGTH": 64, "SS_IS_COLUMN_SET": 0,"TABLE_SCHEM": "dbo", "BUFFER_LENGTH": 64, "NULLABLE": 1, "IS_NULLABLE": "YES", "TABLE_CAT": "tempdb", "SQL_DATA_TYPE": 12, "COLUMN_SIZE": 64, "TYPE_NAME": "varchar", "IS_AUTOINCREMENT": "NO", "COLUMN_NAME": "email", "ORDINAL_POSITION": 5, "SS_IS_COMPUTED": 0, "DATA_TYPE": 12}] 
	 * @param table Table name.
	 * @param colName Column name.
	 * @param key Meta data key to retrieve.
	 * @return The meta data value. For example  "TABLE_NAME" = "AGENTS"
	 */
	public Object getColumnMetaValue (JSONArray cols , String table, String colName, String key) {
		//JDBCMetaData meta 	= getMetaData();
		try {
			// [ {ROW1} {ROW2}...]
			//JSONArray cols		=  meta.getColumns(meta.getCatalog(), null, table, colName);
			if ( cols == null ) {
				cols	=  meta.getColumns(meta.getCatalog(), null, table, colName);
			}
			for (int i = 0; i < cols.length(); i++) {
				JSONObject col 	= cols.getJSONObject(i);
				String thisName = col.optString("COLUMN_NAME");

				if ( thisName != null && thisName.equalsIgnoreCase(colName)) {
					return col.opt(key);
				}
			}
		} catch (Exception e) {
			log.error("getColumnMetaValue() Table:" + table + "@" + colName + " [" + key + "]  " + e.toString());
		}
		return null;
	}
	

	/**
	 * Build a default SQL condition by looking at all the primary keys in a table.
	 * @param table DB table to inspect.
	 * @param values A row {@link JSONObject} {K1:V1, K2:V2,...} or {@link JSONArray} [V1, V2,...] representing the values to assign to the primary keys.
	 * @param prepared If true use ? instead of the values (for a prepared statement) else use the values themselves.
	 * @return PK1 = VAL1 AND PK2 = VAL2 ... (Non-prepared) or PK1 = ? AND PK2 = ? ... (Prepared statement)
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	public String buildDefaultCondition ( String table, Object values, boolean prepared ) throws SQLException, JSONException {
		return JDBCUtil.buildDefaultCondition(this, table, null, null, values, prepared);
	}
	
	/**
	 * Ask if a DB data type needs to be quoted
	 * @param dataType DB data type: INT, CHAR, ....
	 * @return True if it needs single quotes.
	 */
	static boolean needsQuotes (int dataType) {
		return JDBCUtil.needsQuotes(dataType);
	}
	
}
