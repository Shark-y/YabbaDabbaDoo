package com.cloud.core.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wraps an {@link ResultSet} to convert a DB query into JSON.
 *<pre> {
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

 * @author VSilva
 *
 */
public class JDBCResultSet {

	private JSONObject root = new JSONObject();

	public JDBCResultSet(java.sql.ResultSet rs) throws SQLException, JSONException {
		parse(rs);
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
	 */
	public JSONObject toJSON() {
		return root;
	}

	private /* JSONObject*/ void parse(java.sql.ResultSet rs) throws SQLException, JSONException  {

		JSONArray resultSet 	= new JSONArray();
		ResultSetMetaData meta 	= rs.getMetaData();
		int cols 				= meta.getColumnCount();
		
		// pack the result set an array of arrays: rs = [ [row1], [row2], ... [rowN] ]
		// row (n) = [ val1, val2, .. val]
		while (rs.next()) {
			JSONArray row = new JSONArray();
			
			// Columns start from 1...
			for (int i = 1; i <= cols; i++) {
				Object value	= rs.getObject(i);
				row.put(value);
			}
			resultSet.put(row);
		}
		root.put("resultSet", resultSet);
		root.put("fetchSize", rs.getFetchSize());
		
		// pack meta data: col info, names, types, etc.
		JSONObject oMeta = new JSONObject();
		JSONArray columns = new JSONArray();
		
		for (int i = 1; i <= cols; i++) {
			JSONObject column = new JSONObject();
			column.put("name", meta.getColumnName(i));
			column.put("label", meta.getColumnLabel(i));
			column.put("typeName", meta.getColumnTypeName(i));
			column.put("displaySize", meta.getColumnDisplaySize(i));
			column.put("className", meta.getColumnClassName(i));
			/* Mostly empty THIS SLOWS THIMGS DOWN!
			 * column.put("table", meta.getTableName(i));
			 * 	column.put("schema", meta.getSchemaName(i)); */
			columns.put(column);
		}
		oMeta.put("colCount", cols);
		oMeta.put("columns", columns);
		
		root.put("metaData", oMeta);
		//return root;
	}
	
	/**
	 * Get the result set portion of a JDBC query as a JSON array of rows.
	 * Each row represents an array of fields from the table: [[f1, f2,...], ...]
	 * @return JSON: <pre>
	 * [ [ "000047561914",
   5001,
   0,
   null,
   null,
   0
  ], ...
 ] </pre>
	 * @throws JSONException
	 */
	public JSONArray getResultSet() throws JSONException {
		return root.getJSONArray("resultSet");
	}
	
	/**
	 * Get the result set meta data. It describes the columns from the query result set.
	 * @return <pre>
	 * { "columns": [
   {
    "typeName": "varchar",
    "name": "SkillGroupName",
    "className": "java.lang.String",
    "displaySize": 32,
    "label": "SkillGroupName"
   }, ...
  ],
  "colCount": 6
 } </pre>
	 * @throws JSONException
	 */
	public JSONObject getMetaData () throws JSONException {
		return root.getJSONObject("metaData");
	}
	
	public void close ()  {
	}
	
	/**
	 * Convert a row X [ "000047561914", 5001, 0,  null, null,  0 ] into a map of key, value pairs
	 * where the key is the field name: {SkillGroupName=000047561914, Extension=5001,...}
	 * @param rowIdx
	 * @return Map of row X (Field,Value) pairs: [ "000047561914", 5001, 0,  null, null,  0 ] => {SkillGroupName=000047561914, Extension=5001,...}
	 * @throws JSONException
	 */
	public Map<String, String> rowToParams (int rowIdx) throws JSONException {
		Map<String, String> params = new HashMap<String, String>();
		JSONArray columns = getMetaData().getJSONArray("columns");
		
		JSONArray row 	= getResultSet().getJSONArray(rowIdx);
		for (int i = 0; i < row.length(); i++) {
			JSONObject column = columns.getJSONObject(i);
			params.put(column.getString("name"), row.get(i).toString());
		}
		return params;
	}
}
