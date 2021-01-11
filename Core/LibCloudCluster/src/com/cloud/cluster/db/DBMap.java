package com.cloud.cluster.db;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;

import com.cloud.cluster.IMap;
import com.cloud.core.db.Database;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.io.ObjectIO;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

public class DBMap<K, V> implements IMap<K, V> {

	static final Logger log 	= LogManager.getLogger(DBMap.class);
	
	private Database db 		= Database.getInstance();
	private String name;
	private String nodeGroup;
	
	public DBMap(String name, String nodeGroup) {
		this.name = name;
		this.nodeGroup = nodeGroup;
	}
	
	private boolean dbUpdate (String SQL ) {
		try {
			int rows = db.update(SQL);
			if ( rows == 0) {
				throw new IOException("Update returned 0 record size.");
			}
			return true;
		} catch (Exception e) {
			log.error("DB Update " + SQL, e);
			return false;
		}
	}

	private Object dbQuery (String label, String SQL, int row, int col ) {
		try {
			JDBCResultSet rs = db.query(SQL);
			JSONArray rows	 = rs.getResultSet();
			if ( rows.length() == 0 ) {
				log.warn("No data for SQL " + SQL);
				return null;
			}
			return rows.getJSONArray(row).get(col);
		} catch (Exception e) {
			log.error(label + " with Query " + SQL, e);
			return null;
		}
	}

	/**
	 * @return name = '" + mapName + "' AND nodeGroup = '" + nodeGroup + "'";
	 */
	private String getDefaultSQLCondition () {
		return String.format("name = '%s' AND nodeGroup = '%s'", name, nodeGroup);
	}

	@Override
	public void clear() {
		dbUpdate(String.format("DELETE FROM MAPS WHERE %s", getDefaultSQLCondition()));
	}

	@Override
	public boolean containsKey(Object key) {
		String SQL = String.format("SELECT mapValue FROM MAPS WHERE %s AND mapKey = '%s'", getDefaultSQLCondition(), key.toString());
		Object obj = dbQuery("containsKey " + key ,SQL, 0, 0);
		return obj != null;
	}

	@Override
	public boolean containsValue(Object value) {
		try {
			String b64 = ObjectIO.encodeObjectAsB64(value);
			String SQL = String.format("SELECT mapValue FROM MAPS WHERE %s AND mapValue = '%s'", getDefaultSQLCondition(), b64);
			Object obj = dbQuery("containsValue " + value ,SQL, 0, 0);
			return obj != null;
		} catch (Exception e) {
			log.error(name + " containsValue " + value, e);
			return false;
		}
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> entries = new HashSet<java.util.Map.Entry<K, V>>();
		try {
			String SQL 			= String.format("SELECT mapKey, mapValue FROM MAPS WHERE %s", getDefaultSQLCondition());
			JDBCResultSet rs 	= db.query(SQL);
			JSONArray rows	 	= rs.getResultSet();
			if ( rows.length() == 0 ) {
				log.warn("No data for SQL " + SQL);
				return entries;
			}
			for (int i = 0; i < rows.length(); i++) {
				JSONArray row = rows.getJSONArray(i);
				String key = row.getString(0); 
				String b64 = row.getString(1);
				entries.add(new AbstractMap.SimpleEntry<K,V>((K)key, (V)ObjectIO.decodeObjectFromB64(b64)));
			}
		} catch (Exception e) {
			log.error(name + "/" + nodeGroup + " entrySet", e);
		}

		return entries;
	}

	@Override
	public V get(Object key) {
		String SQL = String.format("SELECT mapValue FROM MAPS WHERE %s AND mapKey = '%s'", getDefaultSQLCondition(), key);
		Object b64 = dbQuery("Get " + key, SQL, 0, 0);
		
		try {
			return b64 != null ? (V)ObjectIO.decodeObjectFromB64(b64.toString()) : null;
		} catch (ClassNotFoundException e) {
			log.error("get() " + key, e);
		} catch (IOException e) {
			log.error("get() " + key, e);
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Set<K> keySet() {
		throw new RuntimeException("Method Set<K> keySet() not implemented for efficiency. Use Set<java.util.Map.Entry<K, V>> entrySet() instead.");	
	}

	@Override
	public V put(K key, V value) {
		long time 	= System.currentTimeMillis();
		try {
			String b64		= ObjectIO.encodeObjectAsB64(value);
			boolean exists 	= db.exists("MAPS", String.format("%s AND mapKey = '%s'", getDefaultSQLCondition(), key));

			String SQL 		= exists 
					? String.format("UPDATE MAPS SET mapValue = '%s' WHERE %s AND mapKey = '%s'", b64, getDefaultSQLCondition(), key)
					: String.format("INSERT INTO MAPS values ( '%s' , '%s' , %d , '%s' , '%s')", name, nodeGroup, time, key, b64);
					
			boolean ret 	= dbUpdate(SQL);
			return ret 		? value : null;
		} catch (Exception e) {
			log.error("Map Put (" + key + "," + value + ")", e);
			return null;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new RuntimeException("Method putAll(Map<? extends K, ? extends V> m) not implemented");
	}

	@Override
	public V remove(Object key) {
		V value = get(key); // FIXME: Too slow ? 
		dbUpdate(String.format("DELETE FROM MAPS WHERE %s AND mapKey = '%s'", getDefaultSQLCondition(), key));
		return value;
	}

	@Override
	public int size() {
		String SQL = String.format("SELECT mapValue FROM MAPS WHERE %s", getDefaultSQLCondition());
		try {
			JDBCResultSet rs = db.query(SQL);
			JSONArray rows	 = rs.getResultSet();
			return rows.length();
		} 
		catch (Exception e) {
			log.error("Map " + name + "/" + nodeGroup + " Get Size", e);
		}
		return 0;
	}

	@Override
	public Collection<V> values() {
		throw new RuntimeException("Method Collection<V> values() not implemented.");
	}

	@Override
	public V put(K key, V value, int duration, TimeUnit unit) {
		return put(key, value);
	}

}
