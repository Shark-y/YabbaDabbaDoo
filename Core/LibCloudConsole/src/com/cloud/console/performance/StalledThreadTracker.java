package com.cloud.console.performance;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cloud.console.servlet.PerformanceServlet;

/**
 * Poor man's stalled thread detector based on "Detecting and handling stalled threads in JavaSE" and others.
 * <ul>
 * <li> http://coopsoft.com/ar/StalledArticle.html
 * <li> http://www.javaworld.com/article/2074310/java-app-dev/java-hanging-thread-detection-and-handling.html
 * <li> This class is meant to be called from a ticking tracker logic at regular intervals.
 * <li> The "Thread Inspector" of the cloud console invokes this guy via the {@link PerformanceServlet}.
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 1/25/2017
 */
public class StalledThreadTracker implements Serializable {

	private static final long serialVersionUID 		= 9034020955047375907L;
	
	/** Any thread that is idle for this period of time (ms) is assumed as "Probably Stuck" */
	static final long STALLED_THRESHOLD_MS			= 240000;
	
	/** Mem cleanup every 300 ticks */
	static final int TICK_CLEANUP_LIMIT				= 300;
	
	/**
	 * This class is used to track thread state change latencies for probably stuck thread states: RUUNABLE, WAITING, TIMED_WAITING.
	 * <p>For each one of these states an initial latency is saved, then if (current check time - latency ) is grater than
	 * STALLED_THRESHOLD_MS the thread is marked as probably stuck for that state.</p>
	 * <p>If the state changes, the latencies are reset and the cycle beings again.</p>
	 * @author VSilva
	 * @version 1.0.0 - 1/25/2017
	 */
	static class ThreadStateLatencyTracker implements Serializable  {
		private static final long serialVersionUID = 4009013117710974736L;
		
		long tid;
		long latencyRunnable;
		long latencyWaiting;
		long latencyTimedWaitng;
		Thread.State state;
		String stackTraceHash;
		
		public ThreadStateLatencyTracker(long tid, Thread.State state, String stackTraceHash) {
			super();
			this.tid = tid;
			this.stackTraceHash = stackTraceHash;
			set(state);
		}

		void set ( Thread.State state) {
			this.state = state;
			switch (state) {
			case RUNNABLE:
				latencyRunnable = System.currentTimeMillis();
				break;
			case WAITING:
				latencyWaiting = System.currentTimeMillis();
				break;
			case TIMED_WAITING:
				latencyTimedWaitng = System.currentTimeMillis();
				break;
			default:
				break;
			}
		}
		
		void update (Thread.State state, String stackTraceHash) {
			if ( this.state != state) {
				latencyRunnable = latencyTimedWaitng = latencyWaiting = 0;
				set(state);
			}
			this.stackTraceHash = stackTraceHash;
		}
		
		boolean isStalled() {
			long now = System.currentTimeMillis();
			switch (state) {
			case RUNNABLE:
				return latencyRunnable > 0 && (now - latencyRunnable) > STALLED_THRESHOLD_MS ;
			case WAITING:
				return latencyWaiting > 0 && (now - latencyWaiting) > STALLED_THRESHOLD_MS ;
			case TIMED_WAITING:
				return latencyTimedWaitng > 0 && (now - latencyTimedWaitng) > STALLED_THRESHOLD_MS;
			default:
				return false;
			}
		}
		
		@Override
		public String toString() {
			return String.format("Tid: %d Runnable: %d,  Waiting: %d, TimedWaitiong: %d StHash: %s"
					, tid, latencyRunnable, latencyWaiting, latencyTimedWaitng , stackTraceHash);
		}
	}
	
	/** List used to track thread latencies by state */
	private final List<ThreadStateLatencyTracker> latencies = new CopyOnWriteArrayList<ThreadStateLatencyTracker>();

	/** This class is a singleton. Only 1 instance is allowed */
	private static final StalledThreadTracker INSTANCE = new StalledThreadTracker();
	
	/**
	 * Get a singleton instance.
	 * @return {@link StalledThreadTracker} singleton.
	 */
	public static StalledThreadTracker getinstance() {
		return INSTANCE;
	}
	
	private StalledThreadTracker() {
	}
	
	/**
	 * Add a {@link ThreadStateLatencyTracker} information.
	 * @param latency
	 */
	public void add (ThreadStateLatencyTracker latency) {
		latencies.add(latency);
	}

	/**
	 * Start tracking a thread.
	 * @param tid Thread  id.
	 * @param state Thread {@link State}.
	 * @param stackTraceHash MD5 of the stack trace. This can be used to check if the stack trace has changed.
	 */
	public void add (long tid, Thread.State state, String stackTraceHash) {
		latencies.add(new ThreadStateLatencyTracker(tid, state, stackTraceHash));
	}

	/**
	 * Clean the list (memory saver).
	 */
	public void reset() {
		latencies.clear();
	}
	
	public boolean isTracked (long tid ) {
		return find(tid) != null;
	}
	
	/**
	 * Update thread tracker internal state. This should be called on every tick of the tracking logic.
	 * @param tid Thread id.
	 * @param state Thread {@link State}.
	 * @param stackTraceHash MD5 of the stack trace. It can be used to check if the stack trace has changed.
	 */
	public void update (long tid, Thread.State state, String stackTraceHash) {
		ThreadStateLatencyTracker latency = find(tid);
		if (latency != null) {
			latency.update(state, stackTraceHash);
		}
		else {
			add( tid, state, stackTraceHash);
		}
	}
	
	/**
	 * Is a thread stalled?
	 * @param tid Thread id.
	 * @return True if "probably stuck".
	 */
	public boolean isStalled (long tid ) {
		ThreadStateLatencyTracker item = find(tid);
		return item != null && item.isStalled() ? true : false;
	}
	
	public int getTotalTracked() {
		return latencies.size();
	}

	public int getProbablyStuck() {
		int stuck = 0;
		for ( ThreadStateLatencyTracker item : latencies) {
			if ( item.isStalled()) {
				stuck++;
			}
		}
		return stuck;
	}
	
	private ThreadStateLatencyTracker find ( long tid ) {
		for ( ThreadStateLatencyTracker item : latencies) {
			if ( item.tid == tid) {
				return item;
			}
		}
		return null;
	}
	
	
}
