package com.cloud.cluster;

import java.util.concurrent.locks.Lock;

public interface IClusterLock extends Lock{

//	public abstract boolean tryLock();
//
//	public abstract boolean tryLock(long millis, TimeUnit timeUnit) throws InterruptedException;
//
//	public abstract void unlock();

	public abstract boolean isLocked();

	public abstract void destroy();

}