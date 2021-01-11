package com.cloud.cluster;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface IMap<K, V> extends Map<K, V> {

	/**
	 * Add a (key, value) pair to the map with an expiration interval.
	 * @param key The map key.
	 * @param value Map value.
	 * @param duration Time to live.
	 * @param unit TTL unit.
	 * @return The value inserted.
	 */
	public V put(K key, V value, int duration, TimeUnit unit);

}
