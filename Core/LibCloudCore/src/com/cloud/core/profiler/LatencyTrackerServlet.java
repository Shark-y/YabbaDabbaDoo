package com.cloud.core.profiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.profiler.ILatencyTracker;
import com.cloud.core.profiler.LatencyTracker;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.services.CloudServices;

/**
 * Latency Tracker Servlet. Respose format:
 * <pre>
 * [
 *  {"id":"ctiTracker","latencies":[{"Login":564.7000122070312},{"Logoff":280}],"description":"Average CT Latencies (ms)"}
 *  , {"id":"tracker2","latencies":[{"Foo":564.7000122070312},{"Bar":280}],"description":"Average Latencies (ms)"}
 *  ]
 * </pre>
 * @author VSilva
 *
 */
@WebServlet(
		name = "LatenyTracketServlet",
		description = "Latency Tracker Servlet", 
		urlPatterns = { 
				"/LatencyTracker" 
		}, 
		initParams = { 
				@WebInitParam(name = "key1", value = "val1", description = "")
		})
public class LatencyTrackerServlet extends HttpServlet {

	private static final long serialVersionUID = 6291904580900035032L;

	// Keys used by servlets in this pkg
	static final String CONTENT_TYPE_JSON 	= "application/json";
	static final String KEY_STATUS 			= "status";
	static final String KEY_MESSAGE 		= "message";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		// MUST SET. response in json format.
		resp.setContentType(CONTENT_TYPE_JSON);

		try {
			JSONArray array = getLatencyTrackers();

			// write response.
			resp.getWriter().print(array.toString());
		} catch (Exception e) {
			PrintWriter pw = resp.getWriter();
			pw.print( buildResponse(500, (e.getMessage() != null ? e.getMessage() : e.toString())));
		}
	}
	
	/**
	 * Tracker JSON format:
	 * <pre>
	 * [
	 *  {"id":"ctiTracker","latencies":[{"Login":564.7000122070312},{"Logoff":280}],"description":"Average CT Latencies (ms)"}
	 *  , {"id":"tracker2","latencies":[{"Foo":564.7000122070312},{"Bar":280}],"description":"Average Latencies (ms)"}
	 *  ]
	 * </pre>
 	 * @return {@link JSONArray}. See format above.
	 * @throws JSONException
	 */
	public static JSONArray getLatencyTrackers() throws JSONException {
		List<IServiceLifeCycle> services 	= CloudServices.getServices();
		List<LatencyTracker> latencies 		= new ArrayList<LatencyTracker>();
		
		// add latencies for all  services
		for (IServiceLifeCycle service : services) {
			if ( service instanceof ILatencyTracker) {
				if ( ((ILatencyTracker)service).getLatencyTrackers() != null) {
					for ( LatencyTracker tracker : ((ILatencyTracker)service).getLatencyTrackers() ) {
						latencies.add(tracker);
					}
				}
			}
			/*
			else {
				System.err.println("service " + service.getServiceType() + " must implement ILatencyTracker");
			} */
		}
		JSONArray array = new JSONArray();
		for ( LatencyTracker tracker : latencies) {
			//tracker.dump("LATENCIES SERVLET");
			
			// TRACKER JSON: {"id":"ctiTracker","latencies":[{"Login":564.7000122070312},{"Logoff":280}],"description":"Average CT Latencies (ms)"}
			array.put(tracker.toJSON());
		}
		return array;
	}
	
	public static JSONObject buildResponse(int status, String message) {
		JSONObject root = new JSONObject();
		try {
			root.put(KEY_STATUS, status);
			root.put(KEY_MESSAGE, message);
		} catch (JSONException e) {
			System.err.println("BUILD_RESPONSE: \tUnable to build response:" + e);
		}
		return root;
	}
	
}
