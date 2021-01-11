package com.rts.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.ObjectIO;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudExecutorService;

/**
 * JSON Event Queue
 * 
 * Given Raw Data: F1|  65710|Eastern Sales|   2|:00||   5|53: 7|  60|33:11|  68 
 * 
 * <h2>JSON Batch</h2>
 * 
 * <pre>{"batchData":[{"F1":"F1","VDN":"65710","ACDCALLS":"60"
 *  ,"ABNCALLS":"5" ,"INPROGRESS-ATAGENT":"2","AVG_ACD_TALK_TIME":"33:11"
 *  ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"53: 7"
 *  ,"OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"68"}
 *  ]}</pre>
 *  
 * @author VSilva
 * @version 1.0.0 - Initial implementation.
 * @version 1.0.1 - 8/13/2017 Element queue uses compressed objects to save memory.
 */
public class EventQueue {

	private static final Logger log = LogManager.getLogger(EventQueue.class);

	/** Map of compressed JSONObjects(s) used to track batches */
	private static final Map<String, Queue<byte[]>> events = new ConcurrentHashMap<String, Queue<byte[]>>();
	//private static final Map<String, Queue<JSONObject>> events = new ConcurrentHashMap<String, Queue<JSONObject>>();
	
	/** Interval (ms) at which garbage collection (expired batch removal ) is executed */
	private static int GARBAGE_COLLECT_INTERVAL	= 60000;

	/** Interval (ms) @ which a batch expires */
	private static int EXPIRATION_INTERVAL		= 60000;

	/**
	 * Initialize the event queue intervals: 
	 * <ul>
	 * <li> Event expiration in ms.
	 * <li> Garbage collection in ms.
	 * </ul>
	 * <p>
	 * <b>Note: expired batches are removed by the garbage collector automatically.</b>
	 * </p>
	 * @param eventExpirationMS Event expiration interval in ms.
	 * @param garbageCollectIntervalMS Garbage collection interval in ms.
	 */
	public static void initialize(final int eventExpirationMS, final int garbageCollectIntervalMS) {
		EXPIRATION_INTERVAL 		= eventExpirationMS;
		GARBAGE_COLLECT_INTERVAL	= garbageCollectIntervalMS;
		
		CloudExecutorService.scheduleAtFixedRate(new Runnable() {
			public void run() {
				collectGarbage();
			}
		}, GARBAGE_COLLECT_INTERVAL, GARBAGE_COLLECT_INTERVAL);
	}

	/**
	 * Initialize event queue with defaults: GARBAGE_COLLECT_INTERVAL = 60s.
	 */
	public static void initialize() {
		CloudExecutorService.scheduleAtFixedRate(new Runnable() {
			public void run() {
				collectGarbage();
			}
		}, GARBAGE_COLLECT_INTERVAL, GARBAGE_COLLECT_INTERVAL);
	}
	
	/**
	 * Clean expired batches.
	 */
	private static void collectGarbage() {
		final long now 		= System.currentTimeMillis();
		int totalRemoved	= 0;	// total batches
		int totalRecords	= 0;	// total records removed

		// Map of compressed JSONObjects(s)
		Set<Map.Entry<String, Queue<byte []>>> entries = events.entrySet();
		
		for ( Map.Entry<String, Queue<byte []>> entry : entries) {
			Queue<byte []> queue = entry.getValue();
			
			for (byte[]  encoded : queue) {
				try {
					JSONObject batch =  (JSONObject)ObjectIO.deCompressObject(encoded);
					
					// {"batchDate": 1234483838, "batchData":[{"F1":"F1","VDN": "1234", ...}]}
					if ( !batch.has("batchDate") ) 
						continue;
					
					final long date 	= batch.getLong("batchDate");
					final long delta	= now - date;
					
					if ( ( delta /*now - date*/) > EXPIRATION_INTERVAL ) {
						final JSONArray data 	= batch.getJSONArray("batchData");
						final int dataLen 		= data.length();

						log.debug("GarbageCollect: Got expired batch w/ delta: " + delta + " for "  + batch.getString("listenerName") + " records:" + data.length());

						if( queue.remove(encoded) ) {
							totalRemoved ++;
							totalRecords += dataLen;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		log.trace("Garbage collect done: Removed: " + totalRemoved + " batches " + totalRecords + " Records. Exp Int(ms):" + EXPIRATION_INTERVAL);
	}
	
	/**
	 * Push an event.
	 * <br>
	 * Data: F1|  65710|Eastern Sales|   2|:00||   5|53: 7|  60|33:11|  68 
	 * <h2>Batch Sample</h2>
	 * <pre>{"batchData":[{"F1":"F1","VDN":"65710","ACDCALLS":"60"
	 *  ,"ABNCALLS":"5" ,"INPROGRESS-ATAGENT":"2","AVG_ACD_TALK_TIME":"33:11"
	 *  ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"53: 7"
	 *  ,"OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"68"}
	 *  ]}</pre>
	 *  
	 * @param name Name or id of the listener associated with the event queue.
	 * @param obj JSON object representing a batch of data.
	 * @throws IOException 
	 */
	public static void push (final String name, final JSONObject obj) throws IOException {
		Queue<byte []> q = events.containsKey(name) ? events.get(name) : new ConcurrentLinkedQueue<byte[]>();

		// Store a compressed JSONObject
		q.offer(ObjectIO.compressObject(obj)); //.toString()));
		
		if ( !events.containsKey(name)) {
			events.put(name, q);
		}
	}

	/**
	 * Pop an event.
	 * Data: F1|  65710|Eastern Sales|   2|:00||   5|53: 7|  60|33:11|  68 
	 * <h2>Batch Sample</h2>
	 * <pre>{"batchData":[{"F1":"F1","VDN":"65710","ACDCALLS":"60"
	 *  ,"ABNCALLS":"5" ,"INPROGRESS-ATAGENT":"2","AVG_ACD_TALK_TIME":"33:11"
	 *  ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"53: 7"
	 *  ,"OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"68"}
	 *  ]}</pre>
	 *  
	 * @param name Name or id of the listener associated with the event queue.
	 * @throws IOException  when compressing/serializing
	 * @throws JSONException  when parsing JSON
	 * @throws ClassNotFoundException when serializing 
	 */
	public static JSONObject pop (final String name) throws ClassNotFoundException, JSONException, IOException {
		//return events.containsKey(name) ? events.get(name).poll() : null;
		if ( !events.containsKey(name) ) {
			return null;
		}
		else {
			final byte[] encoded = events.get(name).poll();
			return encoded != null ? (JSONObject) ObjectIO.deCompressObject(encoded) : null;
		}
	}

	/**
	 * Pop an event optionally searching for a (key,value) pair in the batch.
	 * @param name Queue name.
	 * @param key An optional key in the JSON.
	 * @param val The key value.
	 * @return The 1st JSON batch from the queue that matches (key, value) or pop() if the key is missing.
	 * @throws IOException  when compressing/serializing
	 * @throws JSONException  when parsing JSON
	 * @throws ClassNotFoundException when serializing 
	 */
	public static JSONObject pop (final String name, final String key, final String val) throws ClassNotFoundException, JSONException, IOException {
		//return events.containsKey(name) ? events.get(name).poll() : null;
		if ( !events.containsKey(name) ) {
			return null;
		}
		else if ( val == null) {
			return pop(name);
		}
		else {
			Iterator<byte[]> it =  events.get(name).iterator();
			boolean missingKey 	= false;
			
			// Search and return for (key, val), Note key may not exist in batch
			while (it.hasNext()) {
				final byte[] encoded 	= it.next();
				JSONObject batch 		= encoded != null ? (JSONObject) ObjectIO.deCompressObject(encoded) : null;
				
				// missing key?
				if ( !batch.has(key)) {
					missingKey = true;
					break;
				}
				if ( batch.optString(key).equals(val)) {
					it.remove();	// remove from queue
					return batch;
				}
			}
			return missingKey ? pop (name) :  null;
		}
	}
	
	/**
	 * Return the number of batches for the queue for a given data source.
	 * @param name Name or id of the listener (data source) associated with the event queue.
	 * @return The number of batches in the queue for data source name.
	 */
	public static int size(final String name) {
		return events.containsKey(name) ? events.get(name).size() : 0;
	}

	/**
	 * Is queue available?
	 * @param name Listener name
	 * @return true if queue name exists.
	 */
	public static boolean containsQueue (final String name) {
		return events.containsKey(name);
	}
	
	/**
	 * Dump events for all listeners (the whole enchilada).
	 * @deprecated This method cannot handle large amounts of batches. Use dump(Writer) instead.
	 * @return The entire event queue as a JSON array: [ {batchData:[], batchDate: X}, {},...]
	 */
	public static JSONArray dump  () throws Exception {
		JSONArray root = new JSONArray();
		
		Set<Map.Entry<String, Queue<byte []>>> entries = events.entrySet();
		
		for ( Map.Entry<String, Queue<byte []>> entry : entries) {
			for (byte[] encoded : entry.getValue() ) {
				root.put((JSONObject) ObjectIO.deCompressObject(encoded) );
			}
		}
		return root;
	}

	/**
	 * Write the event queue. Designed to handle large amounts of data.
	 * @param writer A print writer.
	 * @throws Exception
	 */
	public static void dump (Writer writer) throws Exception {
		boolean comma = false;
		writer.write('[');
		
		Set<Map.Entry<String, Queue<byte []>>> entries = events.entrySet();
		
		for ( Map.Entry<String, Queue<byte []>> entry : entries) {
			for (byte[] encoded : entry.getValue() ) {
				if ( comma ) {
					writer.write(',');
				}
				JSONObject obj = (JSONObject) ObjectIO.deCompressObject(encoded);
				obj.write(writer);
				comma = true;
			}
		}
		writer.write(']');
	}

	/**
	 * Clear all listeners.
	 */
	public static void clear() {
		events.clear();
	}
}
