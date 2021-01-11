package com.cloud.core.profiler;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * Operating System Metrics. Here is the list
 * <pre>
 * CommittedVirtualMemorySize = 47411200
 * FreePhysicalMemorySize = 4294967295
 * FreeSwapSpaceSize = 4294967295
 * ProcessCpuTime = 296401900
 * SystemCpuLoad = 0.027608721565511085
 * TotalPhysicalMemorySize = 4294967295
 * TotalSwapSpaceSize = 4294967295
 * ProcessCpuLoad = 0.0
 * ObjectName = java.lang:type=OperatingSystem
 * Arch = x86
 * SystemLoadAverage = -1.0
 * AvailableProcessors = 4
 * Name = Windows 7
 * Version = 6.1</pre>
 * 
 * @author VSilva
 * @version 1.0.1 - 1/22/2017
 * @version 1.0.2 - 2/2/2020 Cleanup plus support for remote metrics.
 *
 */
public class OSMetrics {

	private static final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
	private static final ThreadMXBean threadMXBean 		= ManagementFactory.getThreadMXBean();
	
	public static final String KEY_OS 			= "operatingSystem";
	public static final String KEY_DAEMON_THR 	= "daemonThreadCount";
	public static final String KEY_PEAK_THR 	= "peakThreadCount";
	public static final String KEY_THR_COUNT 	= "threadCount";
	public static final String KEY_SYS_CPU 		= "SystemCpuLoad";
	public static final String KEY_PROC_CPU 	= "ProcessCpuLoad";
	public static final String KEY_NUM_CPUS 	= "AvailableProcessors";
	public static final String KEY_FREE_MEM 	= "FreePhysicalMemorySize";
	public static final String KEY_OS_NAME 		= "Name";
	
	public static final String KEY_HEAP_FREE 	= "heapFree";
	public static final String KEY_HEAP_MAX 	= "heapMax";
	public static final String KEY_HEAP_TOTAL 	= "heapTotal";
	
	
	/**
	 * OS metrics encoded as JSON.
	 * 
	 * @return JSON string of the form <pre>
	 * {"operatingSystem":
	 *  {"FreePhysicalMemorySize":"4294967295"
	 *  ,"FreeSwapSpaceSize":"4294967295"
	 *  ,"AvailableProcessors":"4"
	 *  ,"ProcessCpuLoad":"-1.0"
	 *  ,"TotalSwapSpaceSize":"4294967295"
	 *  ,"ProcessCpuTime":"124800800"
	 *  ,"Name":"Windows 7","Arch":"x86"
	 *  ,"SystemLoadAverage":"-1.0"
	 *  ,"TotalPhysicalMemorySize":"4294967295"
	 *  ,"CommittedVirtualMemorySize":"44785664","ObjectName":"java.lang:type=OperatingSystem"
	 *  ,"Version":"6.1"
	 *  ,"SystemCpuLoad":"-1.0"
	 *  }}</pre>
	 * 
	 * @throws JSONException
	 */
	public static JSONObject getOSMetrics() throws JSONException {
		return getOSMetrics(osMXBean, threadMXBean);
	}

	/**
	 * OS metrics encoded as JSON.
	 * @param osMXBean see {@link OperatingSystemMXBean}
	 * @param threadMXBean see {@link ThreadMXBean}
	 * 
	 * @return JSON string of the form <pre>
	 * {"operatingSystem":
	 *  {"FreePhysicalMemorySize":"4294967295"
	 *  ,"FreeSwapSpaceSize":"4294967295"
	 *  ,"AvailableProcessors":"4"
	 *  ,"ProcessCpuLoad":"-1.0"
	 *  ,"TotalSwapSpaceSize":"4294967295"
	 *  ,"ProcessCpuTime":"124800800"
	 *  ,"Name":"Windows 7","Arch":"x86"
	 *  ,"SystemLoadAverage":"-1.0"
	 *  ,"TotalPhysicalMemorySize":"4294967295"
	 *  ,"CommittedVirtualMemorySize":"44785664","ObjectName":"java.lang:type=OperatingSystem"
	 *  ,"Version":"6.1"
	 *  ,"SystemCpuLoad":"-1.0"
	 *  }}</pre>
	 * 
	 * @throws JSONException on JSON errors.
	 */
	public static JSONObject getOSMetrics(OperatingSystemMXBean osMXBean, ThreadMXBean threadMXBean) throws JSONException {
		JSONObject os 	= new JSONObject();
		JSONObject root = new JSONObject();
		
		for (Method method : osMXBean.getClass().getMethods()) {
			method.setAccessible(true);
			String methodName = method.getName();
			
			if ( methodName.startsWith("get")
					&& Modifier.isPublic(method.getModifiers())
					&& OperatingSystemMXBean.class.isAssignableFrom(method.getDeclaringClass())) 
			{
				try {
					// get{NAME} = val
					final String key = methodName.substring(3);
					final Object val = method.invoke(osMXBean);
					os.put(key, val.toString());
				} catch (Throwable ex) {
					// Ignore
				}
			}
		}
		
		// Add thread info: DaemonThreadCount, PeakThreadCount
		os.put(KEY_DAEMON_THR /* "daemonThreadCount"*/, threadMXBean.getDaemonThreadCount());
		os.put(KEY_PEAK_THR /* "peakThreadCount" */, threadMXBean.getPeakThreadCount());
		os.put(KEY_THR_COUNT /* "threadCount" */, threadMXBean.getThreadCount());
		
		// Heap (in bytes)
		Runtime runtime = Runtime.getRuntime();
		
		os.put(KEY_HEAP_FREE, runtime.freeMemory());
		os.put(KEY_HEAP_MAX, runtime.maxMemory());
		os.put(KEY_HEAP_TOTAL, runtime.totalMemory());
		
		root.put(KEY_OS, os );
		return root;
	}

	/**
	 * Get Thread information as a JSON array of arrays: [[t1],[t2]...]
	 * <p>Where T(n) = Id, State, CPUTime, User Time.</p>
	 * @return JSON format compatible with Data Tables:
	 * 
	 * <pre> [
	 *  [ tid1, "Thread1Name", "STATE1", CpuTime1, UserTime1 ],
	 *  [ tid2, "Thread2Name", "STATE2", CpuTime2, UserTime2 ]
	 *  ]
	 * </pre>
	 * 
	 * @throws JSONException
	 */
	public static JSONArray getThreadInfo() throws JSONException {
		return getThreadInfo(threadMXBean);
	}

	/**
	 * Get Thread information as a JSON array of arrays: [[t1],[t2]...]
	 * <p>Where T(n) = Id, State, CPUTime, User Time.</p>
	 * @param threadMXBean JSMX thread bean cleint.
	 * @return JSON format compatible with Data Tables:
	 * 
	 * <pre> [
	 *  [ tid1, "Thread1Name", "STATE1", CpuTime1, UserTime1 ],
	 *  [ tid2, "Thread2Name", "STATE2", CpuTime2, UserTime2 ]
	 *  ]
	 * </pre>
	 * 
	 * @throws JSONException on JSON errors.
	 */
	public static JSONArray getThreadInfo(ThreadMXBean threadMXBean) throws JSONException {
	
		final JSONArray array = new JSONArray();
		
		// all thread ids
		long[] ids = threadMXBean.getAllThreadIds();
		
		
		for (long id : ids) {
			ThreadInfo info 	= threadMXBean.getThreadInfo(id);
			/* java.lang.NullPointerException -fix for http://acme208.acme.com:6091/issue/CLOUD_CORE-54
				at com.cloud.core.profiler.OSMetrics.getThreadInfo(OSMetrics.java:140) */
			if ( info == null ) {
				continue;
			}
			JSONArray jinfo 	= new JSONArray();
			jinfo.put(info.getThreadId());
			jinfo.put(info.getThreadName() );
			jinfo.put(info.getThreadState().name());
			jinfo.put(threadMXBean.getThreadCpuTime(id) / 10e6 );
			jinfo.put(threadMXBean.getThreadUserTime(id) / 10e6 );
			array.put(jinfo);
		}
		return array;
	}
	
	/**
	 * This method returns a ThreadInfo object representing the thread information for the thread of the specified ID. 
	 * The stack trace, locked monitors, and locked synchronizers in the returned ThreadInfo object will be empty. 
	 * If a thread of the given ID is not alive or does not exist, this method will return null. A thread is alive if it has been started and has not yet died. 
	 * @param id Thread id.
	 * @return a ThreadInfo object for the thread of the given ID with no stack trace, no locked monitor and no synchronizer info; null if the thread of the given ID is not alive or it does not exist.
	 */
	public static ThreadInfo getThreadInfo(long id) {
		return threadMXBean.getThreadInfo(id);
	}
	
	/**
	 * Finds cycles of threads that are in deadlock waiting to acquire object monitors or ownable synchronizers. 
	 * Threads are deadlocked in a cycle waiting for a lock of these two types if each thread owns one lock while trying to acquire another lock already held by another thread in the cycle.
	 * This method is designed for troubleshooting use, but not for synchronization control. It might be an expensive operation.
	 * @return an array of IDs of the threads that are deadlocked waiting for object monitors or ownable synchronizers, if any; null otherwise. 
	 */
	public static long[] findDeadlockedThreads() {
		return threadMXBean.findDeadlockedThreads();
	}

	/**
	 * Inject OS metrics into a cluster member attributes. Invoked on cluster initialization.
	 * @param map Cluster member attributes where the metrics will be inserted.
	 */
	public static void injectOSMetrics (Map<String, Object> map) {
		try {
			JSONObject os = OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);
			
			// Note: The metrics injected here must match setLocalMemberOSMetrics!
			map.put(OSMetrics.KEY_SYS_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_SYS_CPU)));
			map.put(OSMetrics.KEY_PROC_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_PROC_CPU)));
			map.put(OSMetrics.KEY_PEAK_THR , os.getInt(OSMetrics.KEY_PEAK_THR));
			map.put(OSMetrics.KEY_OS_NAME , os.getString(OSMetrics.KEY_OS_NAME));
			map.put(OSMetrics.KEY_NUM_CPUS , os.getInt(OSMetrics.KEY_NUM_CPUS));
			map.put(OSMetrics.KEY_FREE_MEM , os.getLong(OSMetrics.KEY_FREE_MEM));
			map.put(OSMetrics.KEY_HEAP_FREE , os.getLong(OSMetrics.KEY_HEAP_FREE));
			map.put(OSMetrics.KEY_HEAP_MAX , os.getLong(OSMetrics.KEY_HEAP_FREE));
			map.put(OSMetrics.KEY_HEAP_TOTAL , os.getLong(OSMetrics.KEY_HEAP_FREE));
			
		} catch (JSONException e) {
		}
	}

}
