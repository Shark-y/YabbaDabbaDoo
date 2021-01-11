package com.cloud.cluster.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

/**
 * Wrap a distributed long. A value that can be replicated across the cluster.
 * @author VSilva
 *
 */
public class DistributedAtomicLong {
	private IAtomicLong item;
	
	public DistributedAtomicLong(HazelcastInstance hazelcastInstance, String name) {
		item = /*AdapterCluster.*/hazelcastInstance.getAtomicLong(name);
	}
	
	public void set(long value) {
		item.set(value);
	}
	
	public long get() {
		return item.get();
	}
	
	public void destroy() {
		item.destroy();
	}
}