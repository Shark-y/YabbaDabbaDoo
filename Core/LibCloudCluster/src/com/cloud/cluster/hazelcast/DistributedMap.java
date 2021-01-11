package com.cloud.cluster.hazelcast;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;

//import com.cloud.cluster.IClusterMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * This class wraps a Hazelcast {@link IMap} which implements {@link IClusterMap}.
 * 
 * @author VSilva
 *
 */
public class DistributedMap<K, V> implements /*ICluster*/com.cloud.cluster.IMap<K, V> {
	IMap<K, V> map;

	public DistributedMap(HazelcastInstance hazelcastInstance, String name) {
		map = hazelcastInstance.getMap(name);
	}

	@Override
	public void clear() {
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
		return map.get(key);
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
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override
	public V remove(Object key) {
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
	public V put(K key, V value, int duration, TimeUnit unit) {
		return map.put(key, value, duration, unit);
	}
	
}
