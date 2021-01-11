package com.cloud.core.profiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Latency Tracker Tool. Tracks averages for the  values of a user defined metric.
 * <pre>
 * final LatencyTracker tracker = new LatencyTracker("ctiTracker", "Average CT Latencies (ms)" );
 * tracker.addLatency("Login"); 
 * tracker.addLatency("Logoff");
 * 
 * for (int i = 0; i < 20; i++) {
 *   tracker.start("Login"); 
 *   
 *   long sleep = (long)(Math.random() * 1000);
 *   System.out.println("Sleep ms:" + sleep);
 *   Thread.sleep(sleep);

 *   tracker.stop("Login");

 *   Thread.sleep((long)(Math.random() * 1000));
 *   tracker.start("Logoff");

 *   Thread.sleep((long)(Math.random() * 1000));
 *   tracker.stop("Logoff");
 *   
 *   tracker.dump("Test");
 *   System.out.println(tracker.toJSON());
 * }
 * </pre>
 * 
 * @author VSilva
 * @version 1.0.1
 *
 */
public class LatencyTracker {

	/**
	 * Describes a Latency.
	 * <pre>
	 * final LatencyTracker tracker = new LatencyTracker("ctiTracker", "Average CT Latencies (ms)" );
	 * tracker.start("something");
	 * ....
	 * tracker.stop("something");
	 * print(tracker.getlatency("something").getAverage());
	 * 
	 * </pre>
	 * @author VSilva
	 *
	 */
	public static class LatencyDescriptor {
		//static final int MAX_SIZE = 10;
		private long startTime;
		private int tickCount;
		//private final long[] values = new long[MAX_SIZE];
		private long accumulator;
		
		public LatencyDescriptor() { 
			accumulator = startTime = 0;
			tickCount = 0;
		}
		
		void start () {
			startTime = System.currentTimeMillis();
		}
		
		void stop () {
			long latency =  System.currentTimeMillis() - startTime;
			//if ( tickCount > MAX_SIZE - 1) tickCount = 0;
			//values[tickCount] = latency;
			accumulator += latency;
			tickCount ++;
		}
		
		public float getAverage() {
			float avg = (float)accumulator; // 0f;
			int count = tickCount; // 0;
//			for (int i = 0; i < values.length; i++) {
//				avg += values[i];
//				if ( values[i] != 0) count++;
//			}
			return count > 0 ? (float)avg/count : (float)avg;
		}
		
		public int getCount () {
			return tickCount;
		}
		
		@Override
		public String toString() {
			return "[" + " StartTime:" + startTime + " Count:" + tickCount + " Avg:" + getAverage() + "]";
		}
	}
	
	private String id;
	private String description;
	private Map<String, LatencyDescriptor> latencies;
	
	/**
	 * Construct.
	 * <pre>final LatencyTracker tracker = new LatencyTracker("ctiTracker", "Average CT Latencies (ms)" );</pre>
	 * @param id Tracker id.
	 * @param description Description.
	 */
	public LatencyTracker(String id, String description) {
		this.id				= id;
		this.description 	= description;
		this.latencies 		=  new HashMap<String, LatencyTracker.LatencyDescriptor>();
	}
	
	public String getId() {
		return id;
	}
	
	public String getDescription() {
		return description;
	}
	
	/**
	 * Get he latencies Map. 
	 * @return Hash map of {@link LatencyDescriptor} indexed by tracker id.
	 */
	public Map<String, LatencyDescriptor> getLatencies() {
		return latencies;
	}
	
	/**
	 * Get a latency from the tracker.
	 * @param name Id or name of the latency.
	 * @return See {@link LatencyDescriptor}.
	 */
	public LatencyDescriptor getLatency (String name) {
		return latencies.get(name);
	}
	
	protected void addLatency(String name) { 
		if ( name == null  ) return;
		if (!latencies.containsKey(name)) {
			latencies.put(name, new LatencyDescriptor()); 
		}
	}

	/**
	 * Start tracking a latency.
	 * @param name Name to track: Login, Logout, Dispose, etc.
	 */
	public void start (String name) {
		if (!latencies.containsKey(name)) {
			addLatency(name);
			//return;
		}
		LatencyDescriptor ld 	= latencies.get(name);
		ld.start();
	}

	/**
	 * Stop tracking a latency.
	 * @param name Name to track: Login, Logout, Dispose, etc.
	 */
	public void stop (String name) {
		if ( !latencies.containsKey(name)) {
			return;
		}
		LatencyDescriptor ld = latencies.get(name);
		ld.stop();
	}

	public void dump (String title) {
		System.out.println("-- LATENCY TRACKER [" + title + " " + id + "/" + description  + "] --");
		for (Entry<String,LatencyDescriptor> entry : latencies.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		System.out.println("-----------------------------");
	}
	
	/**
	 * Serialize this tracker to JSON.
	 * @return <pre>
	 * {"id":"ctiTracker","latencies":[{"Login":434},{"Logoff":448.5}], "latencyCounts":[{"Login":5},{"Logoff":5}] , "description":"Average CT Latencies (ms)"}
	 * </pre>
	 * @throws JSONException
	 */
	public JSONObject toJSON() throws JSONException {
		JSONObject root  = new JSONObject();
		root.put("id", id);
		root.put("description", description);
//		root.put("sampleSize",  LatencyDescriptor.MAX_SIZE); 
		JSONArray array = new JSONArray();
		
		// averages
		for (Entry<String,LatencyDescriptor> entry : latencies.entrySet()) {
			JSONObject row = new JSONObject();
			row.put(entry.getKey(),  entry.getValue().getAverage());
			array.put(row);
		}
		root.put("latencies", array);
		
		// sample sizes
		JSONArray array1 = new JSONArray();
		
		for (Entry<String,LatencyDescriptor> entry : latencies.entrySet()) {
			JSONObject row = new JSONObject();
			row.put(entry.getKey(),  entry.getValue().getCount());
			array1.put(row);
		}
		root.put("latencyCounts", array1);
		
		return root;
	}
	
	
}
