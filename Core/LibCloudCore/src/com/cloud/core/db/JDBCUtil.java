package com.cloud.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.db.JDBCClient;
import com.cloud.core.db.JDBCMetaData;

/**
 * This class was created to off load code from {@link JDBCClient}.
 * 
 * @author VSilva
 * @version 1.0.0 - 11/15/2017 Initial implementation.
 * 
 */
class JDBCUtil {

	/**
	 * Batch upsert a JSON batch using either of to formats:
	 * <ul>
	 * <li> Format #1: {@link JSONArray} of {@link JSONObject} [{K1:V1, K2:V2,...},{K1:v1, K2:V2,...}...] where each {@link JSONObject} represents a row of (FIELD, VALUE) pairs.
	 * <li> Format #2: {@link JSONArray} of {@link JSONArray} [ [V1,V2,..], [V1,V2,..]...]. Note: the values sequence must match the fields in the DB.
	 * </ul>
	 * @param client The parent {@link JDBCClient}.
	 * @param table The DB table to update.
	 * @param batch The JSON document as a {@link JSONArray} of {@link JSONObject} or {@link JSONArray} of {@link JSONArray}..
	 * @return Array of integers (updated counts per row 1 = OK, 0 = Fail).
	 * @throws SQLException on DB errors.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.0
	 */
	static int[] batchUpsert ( JDBCClient client, final String table, JSONArray batch) throws SQLException, JSONException {
		// Decide if an INSERT or UPDATE should be done
		long t0					= System.currentTimeMillis();
		Object row1				= batch.get(0);

		JSONArray primaryKeys 	= getPrimaryKeys(client, table); 	// ~100ms
		JDBCClient.LOGT("Upsert [" + table + "] fetch primary keys took " + (System.currentTimeMillis() - t0) + " ms");

		long t1					= System.currentTimeMillis(); 
		JDBCMetaData meta		= client.getMetaData();
		JSONArray cols			= meta.getColumns(meta.getCatalog(), null, table, null); // ~100ms
	
		JDBCClient.LOGT("Upsert [" + table + "] Get columns descriptors took " + (System.currentTimeMillis() - t1) + " ms");

		t1						= System.currentTimeMillis(); 
		String condition 		= buildDefaultCondition(client, table, cols, primaryKeys, row1, false); 	// ~1ms
		JDBCClient.LOGT("Upsert [" + table + "] Prepare conditions took " + (System.currentTimeMillis() - t1) + " ms");
		
		boolean update 			= client.exists(table, condition);	// 4ms
		JDBCClient.LOGT("Upsert [" + table + "] Figure out if INSERT or UPDATE took " + (System.currentTimeMillis() - t0) + " ms");
		
		final String SQL 		= batchUpsertBuildSQL(client, table, cols, update, row1 , primaryKeys);
		int[] rows				= executeBatch(client, table, cols, SQL, update, batch, primaryKeys);
		
		return batchUpsertComplete(rows, client, table, batch, cols, primaryKeys, update); // rows;
	}

	/**
	 * Complete the upsert by checking for row failures and performing the reverse op on those failures.
	 * 
	 * @param rows The result rows array from the initial upsert operation [1,1,0,..] where 1 = OK, 0 = FAIL.
	 * @param client The parent {@link JDBCClient}.
	 * @param table The DB table to update.
	 * @param batch The JSON document as a {@link JSONArray} of {@link JSONObject} or {@link JSONArray} of {@link JSONArray}..
	 * @param cols Column meta data for table: [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}...]
	 * @param primaryKeys Primary key meta data: [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}...]
	 * @param update If true UPDATE else INSERT.
	 * 
	 * @return The result rows array from the initial upsert operation [1,1,0,..] where 1 = OK, 0 = FAIL.
	 * 
	 * @throws SQLException if an update fails for any row in the batch.
	 * @throws JSONException on JSON parse errors.
	 */
	private static int[] batchUpsertComplete (int[] rows, JDBCClient client, final String table, JSONArray batch, JSONArray cols, JSONArray primaryKeys , boolean update) 
		throws SQLException, JSONException 
	{
		Object row1					= batch.get(0);
		
		// Get row failures [1,1,1,0,1,...]
		List<Integer> failedIndices = new ArrayList<Integer>();
		JSONArray failedBatch		= new JSONArray();
		
		for (int i = 0; i < rows.length; i++) {
			if ( rows[i] == 0) {
				failedIndices.add(i);
				failedBatch.put(batch.get(i));
			}
		}
		if ( failedIndices.size() == 0 ) {
			return rows;	// No failures. All OK
		}
		// Do the reverse op for the failed indices: [629, 630, 631, 632, 633, 635, 636, 637, 638, 639, 640, 642, 644] 
		int[] rows1 	= null;
		if ( update) {
			// Insert failed indices 
			final String SQL  	= batchUpsertBuildSQL(client, table, cols, false, row1 , primaryKeys);			
			rows1				= executeBatch(client, table, cols, SQL, false, failedBatch, primaryKeys);
		}
		else {
			// UPDATE failed indices
			final String SQL  	= batchUpsertBuildSQL(client, table, cols, true, row1 , primaryKeys);			
			rows1				= executeBatch(client, table, cols, SQL, true, failedBatch, primaryKeys);
		}
		// At this point all rows should be either inserted or updated. Row Failures are not allowed
		for (int i = 0; i < rows1.length; i++) {
			if ( rows1[i] == 0) {
				throw new SQLException(table + " failed " + (update ? "INSERT" : "UPDATE") + " row " + i);
			}
		}
		return rows;
	}
	
	/**
	 * Build the upsert SQL (INSERT or UPDATE).
	 * 
	 * @param client The {@link JDBCClient} client.
	 * @param table The DB table to update.
	 * @param update If true do an update else insert.
	 * @param row A {@link JSONObject} row
	 * @param pkSize Size of the primary keys array.
	 * 
	 * @return UPDATE cvdn SET acceptable = ?, ...   WHERE vdn = ? or INSERT INTO table VALUES (x,x,x)
	 * 
	 * @throws SQLException on data base errors.
	 * @throws JSONException on JSON Errors.
	 */
	static String batchUpsertBuildSQL (JDBCClient client, final String table, JSONArray columns, boolean update, Object row, JSONArray primaryKeys /*int pkSize*/) 
			throws SQLException, JSONException 
	{
		StringBuffer flds	= new StringBuffer();		// INSERT INTO table (fields)
		StringBuffer vals 	= new StringBuffer("(");	// INSERT INTO table VALUES (?, ?, ?)   [prepared]
		StringBuffer upd	= new StringBuffer();		// UPDATE table SET F1 = ?, F2 = ? 		[prepared]
		long t0				= System.currentTimeMillis();
		
		// format #1 - JSONArray of JSONObject - [ {k1:v1, k2:v2, ...}, {ROW2}, ...]
		int pkSize 			= primaryKeys.length();
		
		if ( row instanceof JSONObject) {
			flds.append("(");
			for ( Object k :  ((JSONObject)row).keySet() ) {
				if ( update && isPrimaryKey(primaryKeys , k.toString())) {
					continue;
				}
				vals.append("? ,");
				upd.append(k.toString() + " = ? ,");
				flds.append(k.toString() + ",");
			}
			flds.deleteCharAt(flds.length() - 1);
			flds.append(")");
		}
		else {
			// Format #2 JSONArray of JSONArray -  [ [v1,v2,..], [row2],...]
			String[] colNames = client.getColumnNames(table);
			
			// assert colNames size == row1 size
			if ( colNames.length < ((JSONArray)row).length()) {
				throw new JSONException("Batch column size " + ((JSONArray)row).length() + " must match DB cols size " + colNames.length + " for " + Arrays.toString(colNames));
			}
			for (int i = 0; i < ((JSONArray)row).length(); i++) {
				// ignore PKs (the first n values in the array)
				if ( update && i < pkSize) {
					continue;
				}
				vals.append("? ,");
				upd.append(colNames[i] + " = ? ,");
			}
		}
		upd.deleteCharAt(upd.length() - 1);		// delete last comma
		vals.deleteCharAt(vals.length() - 1);	// delete last comma
		vals.append(")");

		String SQL = null;

		if ( !update ) {
			// insert
			SQL = String.format("INSERT INTO %s %s VALUES %s", table, flds, vals);
		}
		else {
			// update
			final String condition2 = buildDefaultCondition(client, table, columns, primaryKeys, row, true);
			SQL = String.format("UPDATE %s SET %s WHERE %s", table, upd, condition2);
		}
		JDBCClient.LOGT("Upsert [" + table + "] SQL " + (!update ? "INSERT" : "UPDATE") + " construct took " + (System.currentTimeMillis() - t0) + " ms");
		return SQL;
	}
	
	/*
	 * Invoked by batchUpsert
	 */
	static int[] executeBatch(JDBCClient client, final String table, JSONArray columns, final String SQL, boolean update, JSONArray batch, JSONArray primaryKeys) 
			throws SQLException, JSONException 
	{
		PreparedStatement ps 	= null;
		int[] rows 				= null;
		long t0					= System.currentTimeMillis();
		try {
			ps 					= client.conn.prepareStatement(SQL);
			JDBCClient.LOGT("Upsert [" + table + "] Prepare statement took " + (System.currentTimeMillis() - t0) + " ms");
			long t12			= System.currentTimeMillis();
			
			// This map speeds things up from 35254ms to 574ms!
			Map<String, Object> DBTYPES = new HashMap<String, Object>();

			// load column types
			for (int i = 0; i < columns.length(); i++) {
				JSONObject col = columns.getJSONObject(i);
				String colName  = col.getString("COLUMN_NAME");
				String type		= col.getString("TYPE_NAME");
				DBTYPES.put(colName, type);
			}
			// Another DB optimization to save looking up keys
			int pkSize	  		= primaryKeys.length();
			
			// This loop takes ~ 99% of the entire time.
			for (int i = 0; i < batch.length(); i++) {
				Object obj = batch.get(i);

				// format #1 - JSONArray of JSONObject - [ {k1:v1, k2:v2, ...}, {ROW2}, ...]
				if ( obj instanceof JSONObject) {
					JSONObject row 	= (JSONObject)obj; 
					int j 			= 1;
					//long t10 		= System.currentTimeMillis();

					for ( Object k :  row.keySet() ) {
						if ( update &&  isPrimaryKey(primaryKeys /*table*/, k.toString())) {
							continue;
						}
						Object val = row.get(k.toString());
						//System.out.println("SET " + k + " = " + val + " type:" + val.getClass() + " idx=" + j + " SQL=" + SQL);
						setValue(client, ps, DBTYPES, columns, table, k.toString(), val, j);
						j++;
					}
					// Add PKs
					if ( SQL.startsWith("UPDATE")) {
						for ( Object k :  row.keySet() ) {
							if ( isPrimaryKey(primaryKeys /*table*/, k.toString())) {
								Object val = row.get(k.toString());
								setValue(client, ps, DBTYPES, columns, table, k.toString(), val, j);
							}
						}
					}
					//LOGT("Upsert [" + table + "] ROW [" + i + "]  Set " + j + " Column values took " + (System.currentTimeMillis() - t10) + " ms");
				}
				// Format #2 JSONArray of JSONArray -  [ [v1,v2,..], [row2],...]
				else {
					JSONArray row 	= (JSONArray)obj;
					int k			= 1;
					
					for (int j = 0; j < row.length(); j++) {
						if ( update && j < pkSize) {
							continue;
						}
						setValue(client, ps,  DBTYPES, columns, table, String.valueOf(j), row.get(j).toString(), k);
						k++;
					}
					// Add PKs
					if ( SQL.startsWith("UPDATE")) {
						for (int j = 0; j < pkSize; j++) {
							//System.out.println("PK-SET [" + j + "] = " + row.get(j) + " type:" + row.get(j).getClass() + " idx=" + k);
							setValue(client, ps, DBTYPES, columns, table, String.valueOf(j), row.get(j).toString(), k);
						}
					}
				}
				ps.addBatch();
			}
			JDBCClient.LOGT("Upsert [" + table + "] Set-Values/Add-Batch took " + (System.currentTimeMillis() - t12) + " ms");
			//t12		= System.currentTimeMillis();
			rows 	= ps.executeBatch();
		}
		finally {
			JDBCClient.LOGT("Upsert [" + table + "] SQL " + (!update ? "INSERT" : "UPDATE") + " Batch execution took " + (System.currentTimeMillis() - t0) + " ms");
			if ( ps != null ) {
				ps.close();
			}
		}
		return rows;
	}

	/**
	 * Set the value of a DB column in a {@link PreparedStatement} and cache the column DB type for the next loop.
	 * @param ps The prepared statement to update.
	 * @param dbTypes A {@link HashMap} of column DB data types. The map is used to cache column DB types to lookup in the next loop and save a ton of time.
	 * @param table The DB table in question.
	 * @param colName The table column name to set the value into.
	 * @param val The value to be set for column name in the table
	 * @param j The row in the prepared statement.
	 * @throws SQLException - on DB errors.
	 */
	static void setValue(JDBCClient client, PreparedStatement ps, Map<String,Object> dbTypes, JSONArray columns, String table, String colName, Object val, int j) throws SQLException {
		// check the DB data type. This is ultra expensive & repeats for every row
		Object dbType 	= null;
		if ( dbTypes.containsKey(colName)) {
			dbType 	= dbTypes.get(colName); 
			//System.out.println("SETVAL DBTYPES=" + dbTypes);
		}
		else {
			dbType 	= client.getColumnMetaValue(columns, table, colName, "TYPE_NAME");
			//System.out.println("DB TYPE=" + dbType + "tbl:" + table + "@" + colName);
		}
		String valType 	= val.getClass().getCanonicalName();
		//System.out.println("** SET " + table + "@" + colName + " = " + val + " idx:" + j + " DB Type:" + dbType + " Val Type:" + valType + " DBTYPES:" + dbTypes);
		
		// validate data types
		if ( dbType != null && valType.contains("String") && dbType.toString().contains("INT")) {
			throw new SQLException("Data type for [" + colName + "] = " + val + " (" + valType + ") does not match Database Type: " + dbType + " Table: " + table);
		}
		
		if ( val instanceof Integer) {
			ps.setInt(j, (Integer)val);
		}
		else if ( val instanceof String) {
			ps.setString(j, val.toString());
		}
		else {
			throw new SQLException("Invalid data type for value: " + val + " " + val.getClass() + " ColName:" + colName);
		}
		// save the DB type for the next loop. Without this, this method will eat a ton of EXEC time.
		if ( !dbTypes.containsKey(colName) ) dbTypes.put(colName, dbType);
	}
	
	/**
	 * Build a default SQL condition by looking at all the primary keys in a table.
	 * @param client The {@link JDBCClient}.
	 * @param table DB table to inspect.
	 * @param keys Primary keys for table: [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
	 * @param values A row {@link JSONObject} {K1:V1, K2:V2,...} or {@link JSONArray} [V1, V2,...] representing the values to assign to the primary keys.
	 * @param prepared If true use ? instead of the values (for a prepared statement) else use the values themselves.
	 * @return PK1 = VAL1 AND PK2 = VAL2 ... (Non-prepared) or PK1 = ? AND PK2 = ? ... (Prepared statement)
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 * @throws JSONException on JSON parse errors.
	 * @since 1.0.3
	 */
	static String buildDefaultCondition (JDBCClient client, String table, JSONArray columns, JSONArray keys, Object values, boolean prepared ) throws SQLException, JSONException {
		JDBCMetaData meta 	= client.getMetaData();

		// [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
		if ( keys == null ) keys = meta.getPrimaryKeys(meta.getCatalog(), null, table);
		StringBuffer buf 	= new StringBuffer();
		boolean comma 		= false;
		
		for (int i = 0; i < keys.length(); i++) {
			JSONObject pk 		= keys.getJSONObject(i);
			String pkName 		= pk.getString("COLUMN_NAME");

			// [{"SS_DATA_TYPE":39,"TABLE_NAME":"AGENTS","SS_IS_SPARSE":0,"CHAR_OCTET_LENGTH":30,"SS_IS_COLUMN_SET":0,"TABLE_SCHEM":"dbo","BUFFER_LENGTH":30,"NULLABLE":0,"IS_NULLABLE":"NO","TABLE_CAT":"tempdb","SQL_DATA_TYPE":12,"COLUMN_SIZE":30,"TYPE_NAME":"varchar","IS_AUTOINCREMENT":"NO","COLUMN_NAME":"userName","ORDINAL_POSITION":1,"SS_IS_COMPUTED":0,"DATA_TYPE":12}, ...]
			// need the SQL type too
			//JSONArray attribs 	= meta.getColumns(meta.getCatalog(), null, table, pkName);
			
			Object autoInc		= client.getColumnMetaValue(columns, table, pkName, "IS_AUTOINCREMENT"); // sometimes this gives null
			Object dataType		= client.getColumnMetaValue(columns, table, pkName, "DATA_TYPE"); //attribs.getJSONObject(0).getInt("DATA_TYPE");
			boolean quote 		= needsQuotes(dataType != null ? Integer.parseInt(dataType.toString()) : Types.INTEGER) && !prepared;

			// may happen over a VPN?
			if ( autoInc == null ) autoInc = "YES";
			//System.out.println("** PRIM KEY " + pkName + " CATALG: " + meta.getCatalog() + " TABLE: " + table + " AUTOINC:" + autoInc + " data type:" + dataType);

			// Get the value for the PK, checking if PK exists in JSON doc
			String value 	= null;
			
			if ( !prepared) {
				if ( values instanceof JSONObject ) {
					boolean pkExistsInValues 	= ((JSONObject)values).has(pkName);
					boolean pkIsAutoIncrement	= autoInc.toString().equalsIgnoreCase("YES"); // : false;
					//System.out.println("**** PK NAME=" + pkName + " AUTO INC=" + attribs +  " TABLE=" + table);
					
					if ( !pkExistsInValues && !pkIsAutoIncrement) {
						throw new JSONException("Missing primary key " + pkName + " in doc " + ((JSONObject)values).toString());
					}
					value = pkExistsInValues ? ((JSONObject)values).get(pkName).toString() : null;
				}
				else {
					value = ((JSONArray)values).get(i).toString();
				}
			}
			else {
				value = "?";
			}
			if ( comma )  { buf.append(" AND "); }
			buf.append(pkName + " = " + ( quote ? "'" : "") +  value + ( quote ? "'" : "") ) ; // + " AND ");
			comma = true;
		}
		// no primary keys, use all cols: COL1 = VAL1 AND COL2 = VAL2 ...
		if ( keys.length() == 0 ) {
			String[] colNames 	= client.getColumnNames(table);
			comma				= false;
			
			for (int i = 0; i < colNames.length; i++) {
				JSONArray cols 	= meta.getColumns(meta.getCatalog(), null, table, colNames[i]);
				int dataType	= cols.getJSONObject(0).getInt("DATA_TYPE");
				boolean quote 	= needsQuotes(dataType);
				
				// verify DB field colNames[i] exists in JSON
				if ( !prepared && values instanceof JSONObject && ! ((JSONObject)values).has(colNames[i])) {
					throw new JSONException("Database field (" + colNames[i] + ") not found in doc " + values);
				}
				String value 	= !prepared 
						? values instanceof JSONObject 
								? ((JSONObject)values).get(colNames[i]).toString()
								: ((JSONArray)values).get(i).toString()  
						: "?";
				
				if ( comma )  { buf.append(" AND "); }
				buf.append(colNames[i] + " = " + ( quote ? "'" : "") + value + ( quote ? "'" : "") ) ; //  + " AND ");
				comma = true;
			}
		}
		return buf.toString();
	}

	/**
	 * [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
	 */
	static JSONArray getPrimaryKeys(JDBCClient client, String table) throws SQLException, JSONException {
		JDBCMetaData meta 	= client.getMetaData();
		// [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
		return meta.getPrimaryKeys(meta.getCatalog(), null, table);
	}

	/**
	 * Check if a column name is a primary key within an array of key descriptors.
	 * @param keys {@link JSONArray} of primary keys: [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
	 * @param name The name of the column to check.
	 * @return True if name is a primary key within keys array.
	 * @throws SQLException on DB errors.
	 * @throws JSONException on JSON errors.
	 */
	static boolean isPrimaryKey (JSONArray keys , String name) throws SQLException, JSONException {
		// This is ultra expensive - JSONArray keys = getPrimaryKeys(table);
		for (int i = 0; i < keys.length(); i++) {
			JSONObject pk 	= keys.getJSONObject(i);
			String pkName 	= pk.getString("COLUMN_NAME");
			if (pkName.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Ask if a DB data type needs to be quoted
	 * @param dataType DB data type: INT, CHAR, ....
	 * @return True if it needs single quotes.
	 */
	static boolean needsQuotes (int dataType) {
		switch (dataType) {
			case Types.CHAR:
			case Types.VARCHAR:
			// 11/11/2017 Fix for http://acme208.acme.com:6091/issue/UNIFIED_CC-413
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
				return true;
			default:
				return false;
		}
	}

	/*
	 * SELECT * FROM table WHERE primaryKey IN (v1,v2,...)
	 */
	/* This may be useful in the future 
	static String batchQueryBuidSQL (final String table, JSONArray batch, JSONArray primaryKeys) throws JSONException {
		StringBuffer SQL 	= new StringBuffer("SELECT * FROM " + table + (primaryKeys.length() > 0 ? " WHERE " : ""));
		
		// [{"KEY_SEQ":1,"TABLE_NAME":"AGENTS","COLUMN_NAME":"userName","PK_NAME":"PK__AGENTS__66DCF95D03317E3D","TABLE_SCHEM":"dbo","TABLE_CAT":"tempdb"}]
		for (int i = 0; i < primaryKeys.length(); i++) {
			JSONObject row 	= primaryKeys.getJSONObject(i);
			String pkName 	= row.getString("COLUMN_NAME");
			
			SQL.append(pkName + " IN (");
			
			for (int j = 0; j < batch.length(); j++) {
				JSONObject row1 = batch.getJSONObject(j);
				Object val		= row1.get(pkName);
			
				SQL.append(val);
				if ( j + 1 < batch.length()) {
					SQL.append(",");
				}
			}
			SQL.append(")");
			if ( i + 1 < primaryKeys.length()) {
				SQL.append(" AND ");
			}
		}
		return SQL.toString();
	} */

}
