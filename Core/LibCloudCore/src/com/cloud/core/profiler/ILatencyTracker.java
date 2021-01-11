package com.cloud.core.profiler;

import java.util.List;

import com.cloud.core.profiler.LatencyTracker;

/**
 * Implement this interface to provide latency information to the admin console & others.
 * <pre>
 * public class JTAPIBackend implements ILatencyTracker {
 * ...
 * 	public List<LatencyTracker> getLatencyTrackers() {
 * 		List<LatencyTracker> trackers = new ArrayList<LatencyTracker>();
 * 		trackers.add(client.getLatenyTracker());
 * 		return trackers;
 * 	}
 * }
 * </pre>
 * @author VSilva
 *
 */
public interface ILatencyTracker {

	/**
	 * Get a list of trackers to be rendered by the admin console or others.
	 * @return See {@link LatencyTracker}.
	 */
	List<LatencyTracker> getLatencyTrackers ();
	
}
