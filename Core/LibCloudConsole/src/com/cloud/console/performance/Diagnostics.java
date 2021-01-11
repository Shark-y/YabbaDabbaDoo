package com.cloud.console.performance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.performance.JMXTomcatMetrics;
import com.cloud.core.io.FileTool;
import com.cloud.core.logging.Container;
import com.cloud.core.profiler.OSMetrics;

/**
 * A Simple class to run diagnostics on the containe and OS
 * 
 * @author VSilva
 * @version 1.0.0 2/1/2019 Initial implementation.
 *
 */
public class Diagnostics {

	public final static String KEY_FINAL_STATUS 	= "FINAL";
	public final static String KEY_OS 				= "OS";
	public final static String KEY_HEAP 			= "Memory";
	public final static String KEY_LOGS 			= "Log Folder";
	public final static String KEY_CONTAINER_VER 	= "Container Version";
	public final static String KEY_CONTAINER_THR 	= "Container Threads";
	public final static String KEY_CONTAINER_ALV 	= "Container AccessLog Valve";
	
	/**
	 * The status of a {@link Result}.
	 * 
	 * @author VSilva
	 *
	 */
	public enum Status {
		INCOMPLETE, PASS, FAIL;
		
		/**
		 * Returns an HTML colorized Status name.
		 */
		public String toString() {
			String color = "black";

			switch (this) {
			case INCOMPLETE:
				break;
			case PASS:
				color = "green";
				break;
			case FAIL:
				color = "red";
				break;
			default:
				break;
			}
			return "<font color="+ color + ">" + name() + "</font>";
		};
	}
	
	/**
	 * Class used to track a diagnostic.
	 * 
	 * @author VSilva
	 *
	 */
	public static class Result {
		String name;
		Object result;
		Object expected;
		String solution;
		Status status = Status.INCOMPLETE;
		
		public Result(String name, Object result, Object expected, Status status) {
			this.name		= name;
			this.result		= result;
			this.expected	= expected;
			this.status		= status;
			this.solution	= "";
		}
		public String getColorizedStatus() {
			String color = "black";
			switch (status) {
			case INCOMPLETE:
				break;
			case PASS:
				color = "green";
				break;
			case FAIL:
				color = "green";
				break;
			default:
				break;
			}
			return "<font color="+ color + ">" + status + "</font>";
		}
		public String getName() {
			return name;
		}
		public Object getResult() {
			return result;
		}
		public Object getExpected() {
			return expected;
		}
		public String getSolution() {
			return solution;
		}
		public Status getStatus() {
			return status;
		}
		
	}
	
	static Result diagnoseOS () {
		final String os 	= System.getProperty("os.name");
		final String arch	= System.getProperty("os.arch");
		
		Result res = new Result(KEY_OS, os + "/" + arch, "64-Bit", Status.PASS);
		if ( arch.contains("x86")) {
			res.status 		= Status.FAIL;
			res.solution	= "Production nodes must use a 64-bit OS.";
		}
		return res;
	}

	/** OSMetrics
	 * {"operatingSystem": {
	"CommittedVirtualMemorySize": "95879168",
	"FreePhysicalMemorySize": "5097242624",
	"SystemLoadAverage": "-1.0",
	"Arch": "x86",
	"threadCount": 32,
	"heapTotal": 44191744,
	"ProcessCpuLoad": "8.088974414841639E-4",
	"FreeSwapSpaceSize": "13574012928",
	"TotalPhysicalMemorySize": "8454844416",
	"Name": "Windows 7",
	"ObjectName": "java.lang:type=OperatingSystem",
	"TotalSwapSpaceSize": "16907784192",
	"ProcessCpuTime": "18891721100",
	"heapMax": 259522560,
	"SystemCpuLoad": "0.13771533774829492",
	"Version": "6.1",
	"AvailableProcessors": "4",
	"heapFree": 3293096,
	"daemonThreadCount": 31,
	"peakThreadCount": 38
	}}
	 * @throws IOException 
	 */
	static Result diagnoseHeap (final JSONObject os) throws JSONException {
		final long totalRAM 	= Long.parseLong(os.getString("TotalPhysicalMemorySize"))/(1024*1024);
		final long heapMAX 	= os.getLong("heapMax")/(1024*1024);
		
		Result res = new Result(KEY_HEAP, "RAM: " + totalRAM + "MB, Heap: " + heapMAX + "MB", "Greater than 512 MB", Status.PASS);
		if ( heapMAX < 512) {
			res.status 		= Status.FAIL;
			res.solution	= "Node heap (" + heapMAX + " MB) is too low. Should be at least 1GB or 50% of total RAM (" + totalRAM + " MB).";
		}
		return res;
	}

	/**
	 * Using the default log folder. See {@link Container}.
	 * @return A diagnosis {@link Result}.
	 */
	static Result diagnoseLogFolderSize () throws JSONException, IOException {
		final String logFolder 		= Container.getDefautContainerLogFolder();
		final long logFolderSize 	= logFolder != null ? FileTool.sizeOfDirectory(logFolder) : 0; // bytes
		final float sizeMB			= (logFolderSize/(1024f*1024f));

		Result res = new Result(KEY_LOGS, logFolder + " (" + String.format("%.2f", sizeMB) + "MB)", "Size less than 2 GB", Status.PASS);

		if ( logFolderSize > (double)(2 * 1024d * 1024d * 1024d) ) {
			res.status 		= Status.FAIL;
			res.solution	= "Logs are too big (" + String.format("%.2f", sizeMB) + "MB).";
		}
		return res;
	}

	/**
	 * {
"serverInfo": "Apache Tomcat/7.0.91",
"threadPool": [{
	"currentThreadsBusy": 10,
	"instance": "Catalina:type=ThreadPool,name=\"http-bio-8080\"",
	"connectionCount": 11,
	"currentThreadCount": 14,
	"maxThreads": 2048,
	"maxConnections": 2048
}],
"requestProcessor": [{
	"requestCount": 41,
	"maxTime": 593,
	"bytesReceived": 9,
	"instance": "Catalina:type=GlobalRequestProcessor,name=\"http-bio-8080\"",
	"modelerType": "org.apache.coyote.RequestGroupInfo",
	"bytesSent": 2167447,
	"processingTime": 1402,
	"errorCount": 3
}]}
	 */
	static /*Status*/ Result[]  diagnoseContainer() throws JSONException, IOException {
		JSONObject container = JMXTomcatMetrics.getContainerMetrics();
		final String info 	= container.getString("serverInfo");
		final int version 	= info.contains("/") ? Integer.parseInt( (info.split("/")[1]).split("\\.")[0] ) : -1;
		
		// Container version > 7
		Result res1 = new Result(KEY_CONTAINER_VER, info, "Version > 7", Status.PASS);
		if ( version <= 7) {
			res1.status 	=  Status.FAIL;
			res1.solution	= "Version too old (" + version + "). Consider upgrading.";
		}

		// Container maxThreads > 1024
		JSONArray array = container.getJSONArray("threadPool");
		Result res2 = new Result(KEY_CONTAINER_THR, "<ul>", "maxThreads > 1024", Status.PASS);
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject pool = array.getJSONObject(i);
			
			// javax.management.ObjectName
			final String instance = pool.get("instance").toString();
			final int maxThreads = pool.getInt("maxThreads");
			
			if ( maxThreads <= 1024) {
				res2.result 	+= "<li>" + instance + " <b>maxThreads too low " + maxThreads + "</b></li>";
				res2.status 	=  Status.FAIL;
				res2.solution	= "Increase maxThreads to 2048 or more in server.xml.";
			}
			else {
				res2.result 	+= "<li>" + instance + " maxThreads " + maxThreads + " OK</li>";
			}
		}
		res2.result += "</ul>";
		return new Result[] {res1, res2}; 
	}

	/*
	 * The Tomcat access log valve in server.xml creates unnecessary large files xxx_access_log.txt.
	 * Should be disabled in production.
	 */
	static Result diagnoseContainerAccessLogValve () throws JSONException, IOException {
		final String logFolder 		= Container.getDefautContainerLogFolder();
		final File[] files 			= FileTool.listFiles(logFolder, new String[] {"txt"}, new String[] {".*access_log.*"});
		
		Result res = new Result(KEY_CONTAINER_ALV, "Container acesss log valve ON", "Acesss log valve should be OFF", Status.PASS);
		if ( files != null && files.length > 0) {
			res.status 		= Status.FAIL;
			res.solution	= "AccessLog valve should be OFF (disabled) @ " + Container.TOMCAT_HOME_PATH;
		}
		return res;
	}
	
	/**
	 * Run {@link Diagnostics}.
	 * @return A Map of {@link Result} values to be rendered in the cloud console.
	 * 
	 * @throws JSONException On JSON errors.
	 * @throws IOException On I/O errors.
	 */
	public static Map<String, Object> run () throws JSONException, IOException {
		final JSONObject root 		= OSMetrics.getOSMetrics();
		final JSONObject os 		= root.getJSONObject(OSMetrics.KEY_OS);
		Status FINAL 				= Status.INCOMPLETE;
		Map<String, Object> map 	= new HashMap<String, Object>();
		List<Result> list			= new ArrayList<Result>();
		
		// 1. OS
		Result res = diagnoseOS();
		//map.put(KEY_OS, res);
		list.add(res);
		if ( res.status != Status.PASS) {
			FINAL = Status.FAIL;
		}
		
		// 2. RAM vs heap (MB)
		res = diagnoseHeap(os);
		//map.put(KEY_HEAP, res);
		list.add(res);
		if ( res.status != Status.PASS) {
			FINAL = Status.FAIL;
		}
		
		// 3. Log folder size < 1GB
		res = diagnoseLogFolderSize();
		//map.put(KEY_LOGS, res);
		list.add(res);
		if ( res.status != Status.PASS) {
			FINAL = Status.FAIL;
		}
		
		// 4,5. container version, container maxThreads
		Result[] status = diagnoseContainer(); //map);
		for ( Result rs : status) {
			list.add(rs);
			if ( rs.status != Status.PASS) {
				FINAL = Status.FAIL;
			}
		}
		
		// Container access_log valve disabled (unnecessary)
		res = diagnoseContainerAccessLogValve();
		list.add(res);
		if ( res.status != Status.PASS) {
			FINAL = Status.FAIL;
		}
		
		// TODO Cluster - multicast (Hazelcast)
		// TODO Cluster Zeroconf

		map.put(KEY_FINAL_STATUS, FINAL);
		map.put("RESULTS", list);
		
		return map;
	}
}
