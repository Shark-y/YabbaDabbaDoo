package com.cloud.cluster.hazelcast;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import com.cloud.cluster.IClusterLock;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

/**
 * Wraps a distributed lock (provided by hazelcast).
 * @author VSilva
 *
 */
public class DistributedLock implements IClusterLock {
	private ILock lock;
	
	public DistributedLock(HazelcastInstance hazelcastInstance, String name) {
		lock = hazelcastInstance.getLock(name);
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterLock#tryLock()
	 */
	@Override
	public boolean tryLock () {
		return lock.tryLock();
	}

	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterLock#tryLock(long)
	 */
	@Override
	public boolean tryLock (long millis, TimeUnit tunit) throws InterruptedException {
		return lock.tryLock(millis, tunit); 
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterLock#unlock()
	 */
	@Override
	public void unlock() {
		lock.unlock();
	}

	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterLock#isLocked()
	 */
	@Override
	public boolean isLocked() {
		return lock.isLocked();
	}

	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterLock#destroy()
	 */
	@Override
	public void destroy() {
		lock.destroy();
	}

	@Override
	public void lock() {
		lock.lock();
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		lock.lockInterruptibly();
	}

	@Override
	public Condition newCondition() {
		return lock.newCondition();
	}
}