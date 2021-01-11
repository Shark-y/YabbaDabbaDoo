package com.cloud.core.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class to get meta data information about a JDB {@link Connection} including
 * <ul>
 * <li> Catalog names
 * <li> Schemas
 * <li> Table structures/shemas.
 * </ul>
 * <pre>
 *  String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
 *  Class.forName(driver);
 *  // jdbc:sqlserver://localhost:55811;databaseName=tempdb;integratedSecurity=true;
 *  String hostName = "localhost:55811";
 *  String dbName   = "tempdb";
 *  String url      = "jdbc:sqlserver://"+ hostName + ";databaseName=" + dbName + ";integratedSecurity=true";
 *  Connection conn = DriverManager.getConnection(url);
 *  System.out.println("Catalogs=" + j.getCatalogs());
 *  System.out.println("Schemas= " + j.getSchemas());
 *  System.out.println(j.getTables("tempdb", "dbo", null, null));
 *  System.out.println(j.getColumns("tempdb", "dbo", "AGENTS", "EMAIL"));
 *  </pre>
 * @author vsilva
 * @version 1.0.0
 */
public class JDBCMetaData {

	private final Connection conn ;

	private final DatabaseMetaData meta ;
	
	public JDBCMetaData(Connection conn) throws SQLException {
		super();
		this.conn = conn;
		this.meta = conn.getMetaData();
	}

	/**
	 * Retrieves this Connection object's current catalog name.
	 * @return the current catalog name or null if there is none
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 */
	public String getCatalog() throws SQLException {
		return conn.getCatalog();
	}
	
	/**
	 * Retrieves this Connection object's current schema name.
	 * @return the current schema name or null if there is none
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 */
	public String getSchema() throws SQLException {
		return conn.getSchema();
	}
	
	/**
	 * Retrieves the schema names available in this database. The results are ordered by TABLE_CATALOG and TABLE_SCHEM.
	 * The schema columns are:
	 * <li> TABLE_SCHEM String => schema name
	 * <li> TABLE_CATALOG String => catalog name (may be null) 
	 * @return ["S1","S2",...]
	 * @throws SQLException
	 */
	public JSONArray getSchemas () throws SQLException {
		JSONArray array 		= new JSONArray();
		//DatabaseMetaData meta 	= conn.getMetaData();
		ResultSet rs 			= meta.getSchemas();
		
		while ( rs.next()) {
			String schema = rs.getString(1);
			array.put(schema);
		}
		rs.close();
		return array;
	}

	/**
	 * Retrieves the catalog names available in this database. The results are ordered by catalog name.
	 * The catalog column is:
	 * <li> TABLE_CAT String => catalog name
	 * @return ["master","model","msdb","tempdb"]
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 */
	public JSONArray getCatalogs () throws SQLException {
		JSONArray array 		= new JSONArray();
		//DatabaseMetaData meta 	= conn.getMetaData();
		ResultSet rs 			= meta.getCatalogs();
		
		while ( rs.next()) {
			String schema = rs.getString(1);
			array.put(schema);
		}
		rs.close();
		return array;
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
	 * @throws SQLException - if a database access error occurs or this method is called on a closed connection
	 * @throws JSONException
	 */
	public JSONArray getTables (String catalog, String schemaPattern, String tableNamePattern,String[] types) 
			throws SQLException, JSONException 
	{
		return getMetaData(/*conn.getMetaData()*/meta.getTables(catalog, schemaPattern, tableNamePattern, types));
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
		return getMetaData(/*conn.getMetaData()*/meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern));
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
		return getMetaData(/*conn.getMetaData()*/meta.getPrimaryKeys(catalog, schemaPattern, tableNamePattern));
	}

	/*
	 * Convert a JDBC ResultSet into a JSON array [ KEY: VAL, ...] 
	 */
	private JSONArray getMetaData(ResultSet rs) throws SQLException, JSONException {
		JSONArray array 		= new JSONArray();
		ResultSetMetaData meta 	= rs.getMetaData();
		int cols 				= meta.getColumnCount();
		while ( rs.next()) {
			JSONObject obj = new JSONObject();
			for (int i = 1; i <= cols; i++) {
				String lbl = meta.getColumnLabel(i);
				Object val = rs.getObject(i);
				obj.put(lbl, val);
			}
			array.put(obj);
		}
		return array;
	}


}
