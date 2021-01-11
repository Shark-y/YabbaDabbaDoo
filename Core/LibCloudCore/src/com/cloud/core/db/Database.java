package com.cloud.core.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.db.JDBCClient;
import com.cloud.core.db.JDBCResultSet;

/**
 * Database singleton to do all kinds of stuff.
 * <pre>
 * Database db = Database.getInstance();
 * db.initialize("com.microsoft.sqlserver.jdbc.SQLServerDriver");
 * db.connect("jdbc:sqlserver://localhost:55811;databaseName=tempdb;integratedSecurity=true;", "", "");
 * 
 * System.out.println("Tables=" + db.getTables("tempdb", "dbo", null, null));
 * System.out.println("Columns=" + db.getColumns("tempdb", null, "AGENTS", null));
 * System.out.println("Pimary Keys=" + db.getPrimaryKeys("tempdb", null, "AGENTS"));
 * 
 * db.insert("AGENTS", new Object[]{"user6", "pwd", "WIN32", "name", "email1", "av1"});
 * db.upsert("AGENTS", new Object[]{"user6", "pwd2", "WIN32", "name2", "email2", "av1"});
 * </pre>
 * 
 * This class wraps a single DB connection via {@link JDBCClient}.
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation
 * @version 1.0.1 - 9/23/2017 Moved upsert code to JDBCClient
 *
 */
public class Database {

	public static final String KEY_DRIVER 	=  "DB_DRIVER";
	public static final String KEY_URL 		=  "DB_URL";
	public static final String KEY_USER 	=  "DB_USER";
	public static final String KEY_PWD 		=  "DB_PASSWORD";
	
	private static final Database INSTANCE = new Database();

	private JDBCClient db;
	
	/**
	 * Get a {@link Database} singleton instance.
	 * @return The {@link Database} singleton.
	 */
	public static Database getInstance() {
		return INSTANCE;
	}
	
	private Database() {
	}
	
	public boolean isInitialized() {
		return db != null;
	}
	
	/**
	 * Initialize using a properties object.
	 * @param config A {@link Properties} map with a single K,V for the database driver
	 * <ul>
	 * <li>DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
	 * </ul>
	 * @throws IOException On error loading the DB driver.
	 */
	public void initialize(Properties config) throws IOException {
		String driverName 		= config.getProperty(KEY_DRIVER);
		db = new JDBCClient(driverName);
	}

	/**
	 * Initialize using a Db driver name.
	 * @param driverName Driver class name. For example: com.microsoft.sqlserver.jdbc.SQLServerDriver.
	 * @throws IOException
	 */
	public void initialize(String driverName ) throws IOException {
		if ( db != null) {
			return;
		}
		db = new JDBCClient(driverName);
	}

	/**
	 * Connect using {@link Properties}.
	 * <ul>
	 * <li> URL = jdbc:sqlserver://localhost:55811;databaseName=tempdb;integratedSecurity=true
	 * <li> USER = user1
	 * <li> PASSWORD = secret
	 * </ul>
	 * @param config Configuration hash map.
	 * @throws IOException  - on connection errors.
	 */
	public void connect (Properties config) throws IOException {
		String connectionURL 	= config.getProperty(KEY_URL);
		String user			 	= config.getProperty(KEY_USER);
		String pwd 				= config.getProperty(KEY_PWD);
		connect(connectionURL, user, pwd);
	}

	/**
	 * Connect to the DB.
	 * @param connectionURL DB url: jdbc:sqlserver://localhost:55811;databaseName=tempdb;integratedSecurity=true;
	 * @param user User name.
	 * @param pwd Password.
	 * @throws IOException if there is an error.
	 */
	public void connect (String connectionURL, String user, String pwd) throws IOException {
		db.connect(connectionURL, user, pwd);
	}
	
	public void disconnect () {
		if ( db != null) {
			db.disconnect();
		}
	}
	
	public boolean isClosed() {
		return db.isClosed();
	}

	/**
	 * Cleanup & dispose.
	 */
	public void destroy () {
		if ( db != null) {
			db.shutdown();
			db = null;
		}
	}

	/**
	 * Execute a query SQL.
	 * @param SQL Query SQL: SELECT fields FROM table WHERE condition.
	 * @return A {@link JDBCResultSet}.
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException
	 */
	public JDBCResultSet query (String SQL ) throws SQLException, JSONException {
		return db.query(SQL);
	}

	/**
	 * Execute an update SQL.
	 * @param SQL Update SQL. For example: INSERT INTO table VALUES(...).
	 * @return The number of records updated.
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 */
	public int update (String SQL ) throws SQLException {
		return db.update(SQL);
	}

	/**
	 * Execute a query by table name.
	 * @param table Table name.
	 * @param condition A query condition. For example: field1 = 'value1' AND fields2 = val2
	 * @return A {@link JDBCResultSet}.
	 * @throws SQLException  if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException On JSON parse errors.
	 */
	public JDBCResultSet query(String table, String condition) throws SQLException, JSONException {
		String SQL = String.format("SELECT * FROM %s", table);
		if ( condition != null ) SQL += " WHERE " + condition;
		return db.query(SQL);
	}

	/**
	 * Test if a record exists in some table for a given condition. 
	 * @param table DB table name.
	 * @param condition SQL Condition to test for. For example: SSN = '123' AND name = 'John' (without a WHERE clause).
	 * @return True if a record exists (the result-set > 0) for that condition.
	 * @throws SQLException  if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException On JSON parse errors.
	 */
	public boolean exists(String table, String condition) throws SQLException, JSONException  {
		JDBCResultSet rs 	= query(table, condition);
		int len 			= rs.getResultSet().length();
		return len > 0;
	}

	/**
	 * Execute an insertion by looking automatically at the schema of a given table.
	 * @param table Table name.
	 * @param values Field values.
	 * @return Number of rows inserted.
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException
	 */
	public int insert ( String table, Object[] values) throws SQLException, JSONException {
		// insert
		String SQL = buildInsertSQL(table, values);
		return db.update(SQL);
	}

	public int update ( String table, String[] fields, Object[] values, String condition) throws SQLException, JSONException {
		// insert
		String SQL 		= buildUpdateSQL(table, fields, values, condition);
		int rows 		= db.update(SQL);
		if ( rows == 0 )	throw new SQLException("Update failed: " + SQL);
		return rows;
	}

	/**
	 * Insert or Update a table using arrays of column names and values. <b>Note: The order and size of both arrays must be equivalent.</b>
	 * @param table Table name.
	 * @param fields Array of column names.
	 * @param values Array of column values. Must match fields in size and order.
	 * @return Number of rows updated.
	 * @throws SQLException - on DB error or update failure (the number of updated rows == 0).
	 * @throws JSONException
	 */
	public int upsert ( String table, String[] fields, Object[] values) throws SQLException, JSONException {
		return db.upsert(table, fields, values);
	}

	/**
	 * Insert or Update a table using an array of values.
	 * @param table Table name.
	 * @param values Java array of values. <b>The element order must match the order of the fields in the table.</b> 
	 * @return Number of rows updated.
	 * @throws SQLException - on DB error or update failure (the number of updated rows == 0).
	 * @throws JSONException
	 */
	public int upsert ( String table, Object[] values) throws SQLException, JSONException {
		return db.upsert(table, values);
	}

	/**
	 * Get column names for a given table.
	 * @param table Table name.
	 * @return Java array: [ COL1, COL2,...]
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException
	 */
	public String[] getColumnNames( String table) throws SQLException, JSONException {
		return db.getColumnNames(table);
	}
	
	/**
	 * Dynamically create an INSERT statement from the table meta data.
	 * @param table Table name.
	 * @param values Array of values for all the table fields.
	 * @return INSERT INTO AGENTS VALUES ( 'user', 'pwd', 'WIN32', 'name', 'email1', 'av1')
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException
	 */
	public String buildInsertSQL (String table, Object[] values) throws SQLException, JSONException {
		return db.buildInsertSQL(table, values);
	}

	/**
	 * Build an UPDATE SQL statement.
	 * @param table Table name.
	 * @param values values Array of values for all the table fields.
	 * @param condition Update SQL condition. For example PK = val.
	 * @return UPDATE AGENTS SET  name = 'name1', password = 'pwd1' WHERE userName = 'user'
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException
	 */
	public String buildUpdateSQL (String table, Object[] values, String condition) throws SQLException, JSONException {
		return db.buildUpdateSQL(table, values, condition);
	}
	
	/**
	 * Build an UPDATE SQL statement.
	 * @param table Table name.
	 * @param columns Array of names for all the table fields.
	 * @param values values Array of values for all the table fields.
	 * @param condition Update SQL condition. For example PK = val.
	 * @return UPDATE AGENTS SET  name = 'name1', password = 'pwd1' WHERE userName = 'user'
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException
	 */
	public String buildUpdateSQL (String table, String[] columns, Object[] values, String condition) throws SQLException, JSONException {
		return db.buildUpdateSQL(table, columns, values, condition);
	}
	
	public String buildDeleteSQL (String table, Object[] values) throws SQLException, JSONException {
		//return "DELETE FROM " + table + (values != null ?  " WHERE " + buildDefaultCondition(table, values) : "");
		return db.buildDeleteSQL(table, values);
	}
			
	/**
	 * Build a default SQl condition by looking at all the primary keys in a table.
	 * @param table DB table to inspect.
	 * @param values The values to assign to the primary keys.
	 * @return PK1 = VAL1 AND PK2 = VAL2
	 * @throws SQLException
	 * @throws JSONException
	 */
	public String buildDefaultCondition ( String table, Object[] values) throws SQLException, JSONException {
		return db.buildDefaultCondition(table, values);
	}

	boolean needsQuotes (int dataType) {
		return JDBCClient.needsQuotes(dataType);
	}

	/**
	 * Retrieves this Connection object's current catalog name.
	 * @return the current catalog name or null if there is none
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 */
	public String getCatalog() throws SQLException {
		return db.getMetaData().getCatalog();
	}

	/**
	 * Retrieves this Connection object's current schema name.
	 * @return the current schema name or null if there is none
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 */
	public String getSchema() throws SQLException {
		return db.getMetaData().getSchema();
	}

	/**
	 * Retrieves the schema names available in this database. The results are ordered by TABLE_CATALOG and TABLE_SCHEM.
	 * The schema columns are:
	 * <li> TABLE_SCHEM String => schema name
	 * <li> TABLE_CATALOG String => catalog name (may be null) 
	 * @return ["S1","S2",...]
	 * @throws SQLException
	 * @throws JSONException 
	 */
	public JSONArray getSchemas () throws SQLException, JSONException {
		return db.getMetaData().getSchemas();
	}
	
	/**
	 * Retrieves the catalog names available in this database. The results are ordered by catalog name.
	 * The catalog column is:
	 * <li> TABLE_CAT String => catalog name
	 * @return ["master","model","msdb","tempdb"]
	 * @throws SQLException
	 */
	public JSONArray getCatalogs () throws SQLException {
		return db.getMetaData().getCatalogs();
	}
	
	/**
	 * Retrieves a description of the tables available in the given catalog. Only table descriptions matching the catalog, schema, table name and type criteria are returned. They are ordered by TABLE_TYPE, TABLE_CAT, TABLE_SCHEM and TABLE_NAME.
	 * <p>Each table description has the following columns:</p>
	 * <ol>
	 *  <li>TABLE_CAT String => table catalog (may be null)
	 *  <li>TABLE_SCHEM String => table schema (may be null)
	 *  <li>TABLE_NAME String => table name
	 *  <li>TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
	 *  <li>REMARKS String => explanatory comment on the table
	 *  <li>TYPE_CAT String => the types catalog (may be null)
	 *  <li>TYPE_SCHEM String => the types schema (may be null)
	 *  <li>TYPE_NAME String => type name (may be null)
	 *  <li>SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
	 *  <li?REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)
	 *  <ol>
	 *  Note: Some databases may not return information for all tables.
	 *  
	 * @param catalog - a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
	 * @param schemaPattern - a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
	 * @param tableNamePattern - a table name pattern; must match the table name as it is stored in the database
	 * @param types - a list of table types, which must be from the list of table types returned from getTableTypes(),to include; null returns all types 
	 * 
	 * @return [{"TABLE_NAME":"AGENTS","TABLE_TYPE":"TABLE","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"},{"TABLE_NAME":"QUEUES","TABLE_TYPE":"TABLE","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
	 * 
	 * @throws SQLException
	 * @throws JSONException
	 */
	public JSONArray getTables (String catalog, String schemaPattern, String tableNamePattern,String[] types) 
			throws SQLException, JSONException 
	{
		return db.getMetaData().getTables(catalog, schemaPattern, tableNamePattern, types);
	}
	
	/**
	 * Retrieves a description of table columns available in the specified catalog.
	 * Only column descriptions matching the catalog, schema, table and column name criteria are returned. They are ordered by TABLE_CAT,TABLE_SCHEM, TABLE_NAME, and ORDINAL_POSITION. 
	 * <p>Each column description has the following columns: </p>
	 * <ol>
	 * <li>TABLE_CAT String => table catalog (may be null)
	 * <li> TABLE_SCHEM String => table schema (may be null)
	 * <li>TABLE_NAME String => table name 
	 * <li>COLUMN_NAME String => column name
	 * <li>DATA_TYPE int => SQL type from java.sql.Types
	 * <li>TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified
	 * <li>COLUMN_SIZE int => column size.
	 * <li>BUFFER_LENGTH is not used.
	 * <li>DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable.
	 * <li>NUM_PREC_RADIX int => Radix (typically either 10 or 2)
	 * <li>NULLABLE int => is NULL allowed.<pre>
	 * 		columnNoNulls - might not allow NULL values
	 * 		columnNullable - definitely allows NULL values
	 * 		columnNullableUnknown - nullability unknown</pre>
	 * <li>REMARKS String => comment describing column (may be null)
	 * <li>COLUMN_DEF String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
	 * <li>SQL_DATA_TYPE int => unused
	 * <li>SQL_DATETIME_SUB int => unused
	 * <li>CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
	 * <li>ORDINAL_POSITION int => index of column in table (starting at 1)
	 * <li>IS_NULLABLE String => ISO rules are used to determine the nullability for a column.<pre>
	 * 		YES --- if the column can include NULLs
	 * 		NO --- if the column cannot include NULLs</pre>
	 * 		empty string --- if the nullability for the column is unknown
	 * <li>SCOPE_CATALOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
	 * <li>SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
	 * <li>SCOPE_TABLE String => table name that this the scope of a reference attribute (null if the DATA_TYPE isn't REF)
	 * <li>SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
	 * <li>IS_AUTOINCREMENT String => Indicates whether this column is auto incremented<pre>
	 * 		YES --- if the column is auto incremented
	 * 		NO --- if the column is not auto incremented
	 * 		empty string --- if it cannot be determined whether the column is auto incremented</pre>
	 * <li>IS_GENERATEDCOLUMN String => Indicates whether this is a generated column <pre>
	 * 		YES --- if this a generated column
	 * 		NO --- if this not a generated column
	 * 		empty string --- if it cannot be determined whether this is a generated column</pre>
	 * </ol>
	 * The COLUMN_SIZE column specifies the column size for the given column. For numeric data, this is the maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in bytes. Null is returned for data types where the column size is not applicable.
	 * 
	 * @param catalog - a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
	 * @param schemaPattern - a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
	 * @param tableNamePattern - a table name pattern; must match the table name as it is stored in the database
	 * @param columnNamePattern - a column name pattern; must match the column name as it is stored in the database 
	 * @return <pre>
	 * [{
 "SS_DATA_TYPE": 39,
 "TABLE_NAME": "AGENTS",
 "SS_IS_SPARSE": 0,
 "CHAR_OCTET_LENGTH": 64,
 "SS_IS_COLUMN_SET": 0,
 "TABLE_SCHEM": "dbo",
 "BUFFER_LENGTH": 64,
 "NULLABLE": 1,
 "IS_NULLABLE": "YES",
 "TABLE_CAT": "tempdb",
 "SQL_DATA_TYPE": 12,
 "COLUMN_SIZE": 64,
 "TYPE_NAME": "varchar",
 "IS_AUTOINCREMENT": "NO",
 "COLUMN_NAME": "email",
 "ORDINAL_POSITION": 5,
 "SS_IS_COMPUTED": 0,
 "DATA_TYPE": 12
}] </pre>
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException
	 */
	public JSONArray getColumns (String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) 
			throws SQLException, JSONException 
	{
		return db.getMetaData().getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
	}

	/**
	 * Get primary keys.
	 * @param catalog - a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
	 * @param schemaPattern - a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
	 * @param tableNamePattern - a table name pattern; must match the table name as it is stored in the database.
	 * 
	 * @return [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
	 * @throws SQLException
	 * @throws JSONException
	 */
	public JSONArray getPrimaryKeys(String catalog, String schemaPattern, String tableNamePattern) throws SQLException, JSONException {
		return db.getMetaData().getPrimaryKeys(catalog, schemaPattern, tableNamePattern);
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
	 * @since 1.0.1
	 */
	public int[] batchUpsert ( String table, JSONArray batch) throws SQLException, JSONException {
		return db.batchUpsert(table, batch);
	}
	
	/**
	 * Commit a transaction to the data bse.
	 * @throws SQLException on DB errors.
	 * @since 1.0.1
	 */
	public void commit() throws SQLException {
		db.commit();
	}

	/**
	 * Enable/Disable the data base auto commit feature (defaults to true). Used to speed up transactions
	 * @param val Commit value.
	 * @throws SQLException on DB errors.
	 * @since 1.0.1
	 */
	public void setAutoCommit( boolean val) throws SQLException {
		db.setAutoCommit(val);
	}

	/**
	 * Build a default SQL condition by looking at all the primary keys in a table.
	 * @param table DB table to inspect.
	 * @param values A row {@link JSONObject} {K1:V1, K2:V2,...} or {@link JSONArray} [V1, V2,...] representing the values to assign to the primary keys.
	 * @param prepared If true use ? instead of the values (for a prepared statement) else use the values themselves.
	 * @return PK1 = VAL1 AND PK2 = VAL2 ... (Non-prepared) or PK1 = ? AND PK2 = ? ... (Prepared statement)
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.1
	 */
	public String buildDefaultCondition ( String table, Object values, boolean prepared) throws SQLException, JSONException {
		return db.buildDefaultCondition(table, values, prepared);
	}
}
