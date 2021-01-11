package com.cloud.core.io;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.ObjectIO;

/**
 * A basic object cache that can be used to create distributed object clones across multiple nodes.
 * <pre>
 * List<String> list1 = new ArrayList<String>();
list1.add("val1");

Map<String, Object> map1 = new HashMap<String, Object>();
map1.put("ke1", "Str1");
map1.put("key2", 123);

Queue<Long> queue1 = new LinkedBlockingDeque<Long>();
queue1.add(1000L);

ObjectCache cache = new ObjectCache();

cache.add("LIST1", ObjectType.T_LIST, list1, 1000);
cache.add("MAP1", ObjectType.T_MAP, map1);
cache.add("STR1", ObjectType.T_PRIMITIVE, "Hello");
cache.add("INT1", ObjectType.T_PRIMITIVE, 123);
cache.add("Q1", ObjectType.T_QUEUE, queue1);

Thread.sleep(1000);

cache.get("LIST1").value("val3").value("val4");

Thread.sleep(4000);
System.out.println("LIST1: " + cache.get("LIST1").toJSON());

cache.cleanup(); </pre>

 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class ObjectCache {

	/** Default expiration time (5min) in ms */
	public static final long DEFAULT_EXPIRATION_TIMEMS = 300000;
	
	/** Types of objects that can be cached: MAPS. LIST, QUEUES, PRIMITIVES, PUB/SUB TOPIS */
	public enum ObjectType {
		T_MAP,			// Hash map
		T_LIST,			// An object list
		T_QUEUE,		// An object queue
		T_PRIMITIVE,	// Long, int, String, ...
		T_TOPIC			// Pub/Sub mechanism
	}
	
	public static class CachedObject {
		final long timeCreated;
		final ObjectType type;
		final long expPeriodMs; 
		long timeUpdated;
		Object obj;


		public CachedObject(ObjectType type, Object obj) {
			this.obj = obj;
			this.type = type;
			this.timeCreated	= System.currentTimeMillis();
			this.timeUpdated	= timeCreated;
			this.expPeriodMs	= DEFAULT_EXPIRATION_TIMEMS;
		}
		
		public CachedObject(ObjectType type, Object obj , long exp) {
			this.obj = obj;
			this.type = type;
			this.timeCreated	= System.currentTimeMillis();
			this.timeUpdated	= timeCreated;
			this.expPeriodMs	= exp;
		}

		public ObjectType getType () {
			return type;
		}
		
		public Object getObject () {
			return obj;
		}
		
		public CachedObject add (Object value) throws IOException {
			return add(null, value);
		}
		
		public CachedObject add (String key, Object value) throws IOException {
			switch (type) {
			case T_MAP:
				if ( key == null ) {
					throw new IOException("Key required for type " + type);
				}
				((Map)obj).put(key, value);
				break;
			case T_LIST:
				if ( ! ((List)obj).contains(value) ) {
					((List)obj).add(value);
				}
				break;
			case T_QUEUE:
				if ( ! ((Queue)obj).contains(value) ) {
					((Queue)obj).add(value);
				}
				break;
			case T_PRIMITIVE:
				obj = value;
				break;
			default:
				throw new IOException("Invalid operation (add) for type " + type);
			}
			timeUpdated = System.currentTimeMillis();
			return this;
		}
		
		public Object remove (Object elem) throws IOException {
			// Guard against NPEs?
			if ( obj == null || elem == null) {
				return null;
			}
			Object e = null;
			switch (type) {
			case T_MAP:
				e = ((Map)obj).remove(elem);	// returns element
				break;
			case T_LIST:
				e = ((List)obj).remove(elem); 	// true/false ? value : null;
				break;
			case T_QUEUE:
				e = ((Queue)obj).remove(elem); 	// true/fqalse ? value : null;
				break;
			case T_PRIMITIVE:
				e = null;
				break;
			default:
				throw new IOException("Invalid operation (add) for type " + type);
			}
			timeUpdated = System.currentTimeMillis();
			return e; //this;
		}

		public void clear () throws IOException {
			switch (type) {
			case T_MAP:
				((Map)obj).clear();
				break;
			case T_LIST:
				((List)obj).clear();
				break;
			case T_QUEUE:
				((Queue)obj).clear();
				break;
			case T_PRIMITIVE:
				obj = null;
				break;
			default:
				throw new IOException("Invalid operation (add) for type " + type);
			}
			timeUpdated = System.currentTimeMillis();
		}

		public int size () throws IOException {
			int size = 0;
			switch (type) {
			case T_MAP:
				size = ((Map)obj).size();
				break;
			case T_LIST:
				size = ((List)obj).size();
				break;
			case T_QUEUE:
				size = ((Queue)obj).size();
				break;
			case T_PRIMITIVE:
				size = 0;;
				break;
			default:
				throw new IOException("Invalid operation (add) for type " + type);
			}
			timeUpdated = System.currentTimeMillis();
			return size;
		}
		
		public boolean isExpired() {
			long delta 	= System.currentTimeMillis() - timeUpdated;
			boolean exp = delta > expPeriodMs; 
			//System.out.println("T=" + type +  " EXP Int:" + expPeriodMs + " exp:" + exp + " Updated:" + timeUpdated + " delta:" + delta);
			return exp;
		}
		
		/**
		 * JSON serializer.
		 * @return {"expired":true,"value":"[val1, val2, val3, val4]","timeCreated":1547340978463,"timeUpdated":1547340979477,"type":"T_LIST"}
		 * @throws JSONException On JSON errors
		 */
		public JSONObject toJSON () throws JSONException {
			JSONObject root = new JSONObject();
			root.put("type", type.name());
			root.put("value", obj.toString());
			root.put("timeCreated", timeCreated);
			root.put("timeUpdated", timeUpdated);
			root.put("expired", isExpired());
			return root;
		}
	}
	
	private Map<String, CachedObject> objects = new ConcurrentHashMap<String, CachedObject>();
	
	public ObjectCache() {
	}
	

	public void add (String name, ObjectType type,  Object obj) {
		objects.put(name, new CachedObject(type, obj));
	}

	public void add (String name, ObjectType type,  Object obj, long expiration) {
		objects.put(name, new CachedObject(type, obj, expiration));
	}
	
	public CachedObject get (String name) {
		return objects.get(name);
	}

	public CachedObject remove (String name) {
		return objects.remove(name);
	}

	public boolean containsKey (String name) {
		return objects.containsKey(name);
	}

	public int size () {
		return objects.size();
	}

	/**
	 * Return cache in Data tables format: [NAME, TYPE, VALUE, EXPIRED]
	 * @return [["STR1","T_PRIMITIVE","Hello",false],["INT1","T_PRIMITIVE",123,false],...]
	 * @throws JSONException On JSON errors.
	 */
	public JSONArray toDataTables() throws JSONException {
		JSONArray array = new JSONArray();
		for ( Map.Entry<String, CachedObject> entry : objects.entrySet()) {
			JSONArray row 		= new JSONArray();
			CachedObject val 	= entry.getValue();

			row.put(entry.getKey());
			row.put(val.type.name());
			
			// decode value(s) in the data structures
			// TODO: decode lists
			switch (val.type) {
			case T_MAP:
				Map decoded = decodeMap((Map)val.obj);
				row.put(new JSONObject(decoded)); //.toString()));
				break;
			case T_LIST:
				row.put(new JSONArray((List)val.obj));
				break;
			default:
				//row.put(val.obj);
				row.put((new JSONArray()).put(val.obj));
				break;
			}
			row.put(val.isExpired());
		
			array.put(row);
		}
		return array;
	}

	public JSONArray toJSON() throws JSONException {
		JSONArray array = new JSONArray();
		for ( Map.Entry<String, CachedObject> entry : objects.entrySet()) {
			array.put(entry.getValue().toJSON());
		}
		return array;
	}

	@Override
	public String toString() {
		try {
			return toDataTables().toString(1);
		} catch (JSONException e) {
			return e.toString();
		}
	}
	
	public void clear() {
		objects.clear();
	}
	
	/**
	 * Remove expired objects and other cleanup tasks.
	 */
	public void cleanup () {
		for ( Map.Entry<String, CachedObject> entry : objects.entrySet()) {
			CachedObject obj = entry.getValue();
			if ( obj.isExpired()) {
				objects.remove(entry.getKey());
			}
		}
	}
	
	/** Prefix used to serialize objects sent via UDP, so receivers know what to do */
	public static final String ENC_OBJ_PREFIX = "ENCODED-B64";
	
	/**
	 * Encode an Object for delivery over UDPP.
	 * @param obj Any object type (It must implement the {@link Serializable} interface!
	 * @param compress Optionally GZIP the bytes.
	 * @return B64 encoded object withe the prefix (ENCODED-B64). For example: ENCODED-B64xxcccc...
	 * @throws IOException On I/O errors.
	 */
	public static String encodeObject(final Object obj, final boolean compress) throws IOException {
		// Unzziped - return "ENCODED-B64" + ObjectIO.encodeObjectAsB64(value);
		return compress ? ENC_OBJ_PREFIX + ObjectIO.b64Gzip(ObjectIO.serializeObject(obj)) : ENC_OBJ_PREFIX + ObjectIO.encodeObjectAsB64(obj);
	}

	/**
	 * Reverse operation of encodeObject()
	 * @param encoded A B64 encoded object (starts with ENC_OBJ_PREFIX). Note: The object must implement the {@link Serializable} interface.
	 * @param compressed If true the object is assumed to be compressed.
	 * @return The unserialized object.
	 * @throws IOException On I/O errors.
	 * @throws ClassNotFoundException On object casting errors.
	 */
	public static Object decodeObject(final String encoded, final boolean compressed) throws IOException, ClassNotFoundException {
		String b64 = encoded.replace(ENC_OBJ_PREFIX, "");
		return compressed ? ObjectIO.unserializeObject(ObjectIO.b64Gunzip(b64)) : ObjectIO.decodeObjectFromB64(b64);
	}

	/**
	 * Reverse operation of encodeObject()
	 * @param encoded A B64 encoded object (starts with ENC_OBJ_PREFIX). Note: The object must implement the {@link Serializable} interface.
	 * <b>Note: The object is assumed to be compressed.</b>
	 * @return The unserialized object.
	 * @throws IOException On I/O errors.
	 * @throws ClassNotFoundException On object casting errors.
	 */
	public static Object decodeObject(final String encoded) throws IOException, ClassNotFoundException {
		return decodeObject(encoded, true);
	}

	/**
	 * A helper methods the decoded values in a Map that are B64 encoded (for presentation purposes)
	 * @param map The map with b64 encoded values.
	 * @return A new Hashmap
	 */
	static Map<String, Object> decodeMap (Map<String, Object> map) {
		Map<String, Object> map1 	= new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String k = entry.getKey();
			Object v = entry.getValue();
			if ( v.toString().startsWith(ObjectCache.ENC_OBJ_PREFIX)) {
				try {
					// Throws ClassNotFound Exception if the CLS is not present.
					map1.put(k, ObjectCache.decodeObject(v.toString()));
				} catch (Exception e) {
					map1.put(k, v);
				}
			}
			else { 
				map1.put(k, v);
			}
		}
		return map1;
	} 

}
