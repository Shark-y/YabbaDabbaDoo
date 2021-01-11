package com.cloud.cluster.zeroconf;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.cloud.cluster.IMap;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroDescriptorObject;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.cluster.multicast.ZeroDescriptorObject.UpdateType;
import com.cloud.cluster.zeroconf.ZIO;
import com.cloud.cluster.zeroconf.ZMap;
import com.cloud.cluster.zeroconf.ZeroClusterInstance;
import com.cloud.core.io.ObjectCache;
import com.cloud.core.io.ObjectCache.ObjectType;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;


/**
 * This class wraps a distributed {@link IMap} for the {@link ZeroClusterInstance}.
 * 
 * @author VSilva
 *
 */
public class ZMap<K, V> implements IMap<K, V> {
	static final Logger log = LogManager.getLogger(ZMap.class);
	
	String name;
	Map<K, V> map;
	ZeroConfDiscovery ds;
	ObjectCache cache;
	
	public ZMap(ObjectCache cache, ZeroConfDiscovery ds, String name) {
		this.ds 	= ds;
		this.name	= name;
		this.cache	= cache;
		
		if ( cache.containsKey(name)) {
			map = (Map<K, V>)cache.get(name).getObject();
		}
		else {
			map = new HashMap<K, V>();
			cache.add(name, ObjectType.T_MAP, map);
			try {
				// replicate
				ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_NEW, ObjectType.T_MAP, name));
			} catch (IOException e) {
				log.error("CreateMap(" + name + ")", e);
			}
		}
	}

	@Override
	public void clear() {
		try {
			// replicate
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.CLEAR, name)); 
		} catch (IOException e) {
			log.error("ClearMap(" + name + ")", e);
		}
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Object key) {
		V v = map.get(key);
		if ( v != null) {
			try {
				// Unzip/Unserialize
				String ostr = ((Object)v).toString();
				if (ostr.startsWith(ZIO.ENC_OBJ_PREFIX) ) {
					return (V)ZIO.decodeObject(ostr, true);
				}
			} catch (Exception e) {
				log.error("Map." + name + ".get(" + key+")", e);
			}
		}
		return v;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public V put(K key, V value) {
		try {
			// replicate: Compress/serialize/B64
			String b64 			= ZIO.encodeObject(value, true);
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.ADD, name, key, b64)); 
		} catch (IOException e) {
			log.error("PutMap(" + name + ")", e);
		}
		return map.put(key, value);
	}

	/**
	 * Add a (key, value) pair to the map with an expiration interval.
	 * @param key The map key.
	 * @param value Map value.
	 * @param duration Time to live.
	 * @param unit TTL unit.
	 * @return The value inserted.
	 */
	public V put(K key, V value, int duration, TimeUnit unit) {
		try {
			// replicate: Compress/serialize/B64
			String b64 					= ZIO.encodeObject(value, true);
			ZeroDescriptorObject zdo	= ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.ADD, name, key, b64);
			
			zdo.setTimeToLive(TimeUnit.MILLISECONDS.convert(duration, unit));
			ds.sendAndQueue(zdo); 
		} catch (IOException e) {
			log.error("PutMap(" + name + ")", e);
		}
		return map.put(key, value);
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<K, V> entry : map.entrySet() ) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(Object key) {
		try {
			// replicate
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.REMOVE, name, key, null));
		} catch (IOException e) {
			log.error("Map.Remove(" + name + ")", e);
		}
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}
	
	@Override
	public String toString() {
		//return map.toString();
		StringBuffer buf 	= new StringBuffer("{");
		boolean comma 		= false;
		for ( Map.Entry<K, V> entry : map.entrySet()) {
			K key = entry.getKey();
			V val = entry.getValue();
			if ( comma ) {
				buf.append(",");
			}
			// key
			buf.append(key + " = ");
			// val
			if ( val.toString().startsWith(ZIO.ENC_OBJ_PREFIX) ) {
				try {
					buf.append(ZIO.decodeObject(val.toString()).toString());
				} catch (Exception e) {
					buf.append(val.toString());
				}
			}
			else {
				buf.append(val.toString());
			}
			comma = true;
		}
		buf.append("}");
		return buf.toString(); 
	}
}
