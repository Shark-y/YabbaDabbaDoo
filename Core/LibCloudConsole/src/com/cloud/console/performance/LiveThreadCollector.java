package com.cloud.console.performance;

import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import com.cloud.console.performance.StalledThreadTracker;
import com.cloud.console.JSPLoggerTool;
import com.cloud.core.profiler.OSMetrics;
import com.cloud.core.security.EncryptionTool;

/**
 * This guy collects Live Thread information such as:
 * <ul>
 * <li> Name, ID, Priority, State, Interrupted?, Stack Trace.
 * <li> It also check for deadlocked threads using the {@link OSMetrics} findDeadlockedThreads. If found sets the thread state to "DEADLOCKED".
 * <li> Includes stalled thread detection logic using {@link StalledThreadTracker} singleton. if stalled sets the state to "POSIBLY STALLED".
 * <li> This information is mostly used by the cloud console Thread Inspector page.
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 1/27/2017
 *
 */
public class LiveThreadCollector {

	/** Limit used to cleanup the {@link StalledThreadTracker} memory */
	static final int TICK_CLEANUP_LIMIT						= 300;
	
	/** See {@link StalledThreadTracker} */
	private static final StalledThreadTracker tracker		= StalledThreadTracker.getinstance();
	
	/** Tick count used for cleanup */
	private static int tickCount;

	/**
	 * Get LIVE tread stack information.
	 * @return JSON: [T1, T2, T3,...] where T = [NAME, ID, PRIORITY, STATE, ISINTERRUPTED, STACK-TRACE]  
	 * @throws JSONException If there is an error.
	 * @since 1.0.0
	 */
	public static JSONArray getAllThreadStackTraces() throws JSONException {
		final JSONArray array 					= new JSONArray();
		Map<Thread, StackTraceElement[]> map 	= Thread.getAllStackTraces();

		Set<?> keySet 		= map.keySet();
		Thread[] threads 	= new Thread[keySet.size()];
		keySet.toArray(threads);

		// get deadlocked ids. Note this may be null
		long[] deadlocked 	= OSMetrics.findDeadlockedThreads();
		
		final String actionHTML = "<a href=\"javascript:interrupt('interrupt', %d)\">Interrupt</a>" 
				+ "  [<a data-toggle=\"tooltip\" title=\"Forcefully stoping a thread is dangerous.\" href=\"javascript:interrupt('stop', %d)\">Stop</a>]";
		
		for (int i = 0; i < threads.length; i++) {
			JSONArray jinfo 	= new JSONArray();
			Thread t 			= threads[i];
			
			// may be null if thread not alive
			ThreadInfo tinfo	= OSMetrics.getThreadInfo(t.getId());
			
			jinfo.put(t.getName());
			jinfo.put(t.getId());
			jinfo.put(t.getPriority());
			
			// State == deadlocked?
			Thread.State state 		= t.getState();
			boolean isdeadLocked 	= false;
			
			if ( deadlocked != null) {
				for (int j = 0; j < deadlocked.length; j++) {
					if ( deadlocked[j] == t.getId()) {
						//state = "DEADLOCKED";
						isdeadLocked = true;
					}
				}
			}
			
			jinfo.put(isdeadLocked ? String.format("DEADLOCKED " + actionHTML, t.getId(), t.getId()) : state.name());
			
			//jinfo.put(t.isDaemon());
			//jinfo.put(t.isAlive());
			jinfo.put(t.isInterrupted());
			
			// Add the thread stack trace
			StackTraceElement[] stes 	= (StackTraceElement[]) map.get(t);
			StringBuffer stackTrace		= new StringBuffer("<pre>");
			
			for (int j = 0; j < stes.length; j++) {
				StackTraceElement ste 	= stes[j];
				String line 			= ste.getClassName() + "." + ste.getMethodName()
						+ "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")";

				stackTrace.append("\nat " + line);
			}
			
			// Add lock info (if available)
			if ( tinfo != null && tinfo.getLockName() != null) {
				stackTrace.append("\n\n<b>Lock: " + tinfo.getLockName() + "</b>");
			}
			stackTrace.append("</pre>");
			
			jinfo.put(stackTrace.toString());
			
			// Stalled detection.
			String stHash			= EncryptionTool.HASH/*MD5*/(stackTrace.toString());
			tracker.update(t.getId(), t.getState(), stHash);
			
			if ( tracker.isStalled(t.getId())) {
				jinfo.put(3, state + String.format(" [PROBABLY STUCK] " + actionHTML, t.getId(), t.getId()));
			}
			
			array.put(jinfo);
		}
		
		// clean after X ticks... just in case
		if ( tickCount++ > TICK_CLEANUP_LIMIT) {
			resetStalledThreadTracker();
		}
		return array;
	}

	/**
	 * Reset the {@link StalledThreadTracker} state to save memory. Invoke this at regular intervals to clean up.
	 * @since 1.-0.0
	 */
	public static void resetStalledThreadTracker() {
		tracker.reset();
		tickCount = 0;
	}
	
	/**
	 * Try to interrupt Thread by invoking interrupt() or stop()  on the thread object.
	 * @param tid Thread id.
	 * @param interrupt If true try to interrupt the thread by invoking it's interrupt method.
	 * @param stop If true invoke the VERY DANGEROUS {@link Thread} stop() method.
	 */
	public static void processThread(long tid , boolean interrupt, boolean stop ) {
		Map<Thread, StackTraceElement[]> map 	= Thread.getAllStackTraces();

		Set<?> keySet 		= map.keySet();
		Thread[] threads 	= new Thread[keySet.size()];
		keySet.toArray(threads);
		
		for (int i = 0; i < threads.length; i++) {
			Thread t = threads[i];
			if ( t.getId() == tid ) {
				if ( interrupt) {
					JSPLoggerTool.JSP_LOGD("[STUCK-TRACK]","Interrupting " + t.getName() + " (" + t.getId() + ")");
					t.interrupt();
				}
				if ( stop ) {
					JSPLoggerTool.JSP_LOGW("[STUCK-TRACK]","Forcefully stoping " + t.getName() + " (" + t.getId() + ") is dangerous.");
					// Dangerous. deprecated.
					t.stop();
				}
			}
		}
	}

}
