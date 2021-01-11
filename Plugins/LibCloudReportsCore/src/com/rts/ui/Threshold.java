package com.rts.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rts.ui.ThresholdList.Alert;

/**
 * Threshold class.
 * <pre>{
    "alerts": [{
      "weight": 2,
      "level": 1,
      "color": "red"
    }],
    "metric": "F1",
    "listener": "CSPLIT Table"
  }</pre>
 * @author VSilva
 *
 */
public class Threshold {
	/** Global thresholds are applied to all dashborads */
	static public final String TYPE_GLOBAL 	= "Global";
	
	static public final String KEY_METRIC 	= "metric";
	static public final String KEY_LISTENER = "listener";
	static public final String KEY_DASH 	= "dashboard";
	static public final String KEY_ALERTS 	= "alerts";
	
	final String metric;
	
	final String listener;
	
	String dashboard;
	
	final JSONArray alerts = new JSONArray();

	/**
	 * Construct from a JSON string.
	 * <pre>{
	 *  "alerts": [{
	 *  "weight": 2,
	 *  "level": 1,
	 *  "color": "red"
	 * }],
	 * "metric": "F1",
	 * "listener": "CSPLIT Table"
	 * }</pre>
	 * @param JSONString
	 * @throws JSONException
	 */
	public Threshold(final String JSONString) throws JSONException {
		super();
		JSONObject root = new JSONObject(JSONString);
		
		this.metric		= root.getString(KEY_METRIC);
		this.listener 	= root.getString(KEY_LISTENER);
		this.dashboard	= root.has(KEY_DASH) ? root.getString(KEY_DASH) : null;
		
		JSONArray array = root.getJSONArray(KEY_ALERTS);
		
		for ( int i = 0 ; i < array.length() ; i++ ) {
			alerts.put(array.getJSONObject(i));
		}
	}

	public Threshold(final String metric, final String listener) {
		super();
		this.metric = metric;
		this.listener = listener;
	}
	
	public String getMetric() {
		return metric;
	}

	public String getId() {
		return dashboard != null 
				? metric + "@" + listener + "@" + dashboard
				: metric + "@" + listener ;
	}
	
	public String getListener() {
		return listener;
	}

	public JSONArray getAlerts() {
		return alerts;
	}

	/**
	 * Add an alert
	 * @param level An integer value that triggers the alert display.
	 * @param weight An priority value when you have multiple alerts with the same level for 2+ metrics.
	 * @param color The HTML display color.
	 * @param trigger An action that triggers on display: NOTIFICATION (WebApi) or WINDOWFOCUS (focuses the browser window when hidden).
	 * @throws JSONException
	 */
	public void addAlert(final int level, final int weight, final String color, final String trigger) throws JSONException {
		Alert a = new Alert(level, weight, color, trigger);
		alerts.put(a.toJSON());
	}
	
	public int getAlertsSize() {
		return alerts.length();
	}
	
	@Override
	public int hashCode() {
		if (  metric == null || listener == null) {
			return super.hashCode();
		}
		return ( dashboard != null) 
				? metric.hashCode() + listener.hashCode() + dashboard.hashCode()
				: metric.hashCode() + listener.hashCode();
	}
	
	public boolean equals(Object obj) {
		if ( obj == null || metric == null || listener == null) {
			return false;
		}
		Threshold t = (Threshold)obj;
		
		// FIXME: Thresholds by metric, data source and dashboard
		return( dashboard != null) 
				? metric.equals(t.metric) && listener.equals(t.listener) && dashboard.equals(t.dashboard)
				: metric.equals(t.metric) && listener.equals(t.listener);
	};
	
	/**
	 * Set an optional dashboard.
	 * @param name
	 */
	public void setDashboard (final String name) {
		if ( name == null || name.isEmpty())
			return;
		
		dashboard = !name.equals(TYPE_GLOBAL) ? name : null;
	}
	
	public String getDashboard () {
		return dashboard;
	}
	
	/**
	 * JSON serializer.
	 * @return <pre>{
	 *  "alerts": [{
	 *  "weight": 2,
	 *  "level": 1,
	 *  "color": "red"
	 * }],
	 * "metric": "F1",
	 * "listener": "CSPLIT Table"
	 * }</pre>
	 * @throws JSONException
	 */
	JSONObject toJSON () throws JSONException {
		JSONObject root = new JSONObject();
		// required
		root.put(KEY_METRIC, metric);
		root.put(KEY_LISTENER, listener);
		
		// optional
		if ( dashboard != null) root.put(KEY_DASH, dashboard);
		
		// required (alerts)
		root.put(KEY_ALERTS, alerts);
		return root;
	}
	
	@Override
	public String toString() {
		return getId() +  ( dashboard != null ? "@" + dashboard : "");
	}
}
