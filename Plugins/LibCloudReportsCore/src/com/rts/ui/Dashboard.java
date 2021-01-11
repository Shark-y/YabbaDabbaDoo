package com.rts.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Encapsulates a Dashboard.
 * <h2>XML Format</h2>
 *  <pre>
 *  &lt;dashboard>
 *  &lt;listener>CSPLIT Table&lt;/listener>
 *  &lt;key>SPLIT&lt;/key>
 *  &lt;keyRange>1,5&lt;/keyRange>
 *  &lt;metric>
 *    &lt;name>ACD&lt;/name>
 *    &lt;description>Abandoned&lt;/description>
 *    &lt;type>NUMBER&lt;/type>
 *    &lt;widget>GAUGE&lt;/widget>
 *  &lt;/metric>
 * &lt;/dashboard></pre>
 * 
 * @author VSilva
 *
 */
public class Dashboard {
	
	private static final Logger log = LogManager.getLogger(Dashboard.class);
			
	/** Metric Widget type */
	public enum WidgetType { GAUGE , PANEL, AREA_CHART, MARQUEE };
	
	/** Type of metric */
	public enum MetricType { NUMBER, STRING };
	
	/**
	 * Metric object.
	 * @author VSilva
	 *
	 */
	public static class Metric {
		final String 		name;
		final String 		description;
		final WidgetType 	widget;
		final MetricType 	type;
		
		/**
		 * Construct
		 * @param name Metric name.
		 * @param description Metric description.
		 * @param type See {@link MetricType}.
		 * @param widget See {@link WidgetType}.
		 * @param thresholdId Threshold id bound to this metric.  
		 */
		public Metric(final String name, final String description, final MetricType type, final WidgetType widget ) {
			super();
			this.name 			=	name;
			this.description 	= description;
			this.widget 		= widget;
			this.type 			= type;
		}

		public Metric(JSONObject root) throws JSONException {
			this(root.getString("name")
					, root.getString("description")
					, MetricType.valueOf(root.getString("type"))
					, WidgetType.valueOf(root.getString("widget")));
		}
		
		public WidgetType getWidgetType() {
			return widget;
		}

		public boolean isPanelWidget () {
			return widget == WidgetType.PANEL;
		}

		public boolean isAreaChartWidget () {
			return widget == WidgetType.AREA_CHART;
		}

		public boolean isGaugeWidget () {
			return widget == WidgetType.GAUGE;
		}

		public boolean isMarqueeWidget () {
			return widget == WidgetType.MARQUEE;
		}
		
		public MetricType getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		/**
		 * Serialize to XML.
		 * @return <pre>
		 * &lt;metric>
		 *  &lt;name>ACD&lt;/name>
		 *  &lt;description>Abandoned&lt;/description>
		 *  &lt;type>NUMBER&lt;/type>
		 *  &lt;widget>GAUGE&lt;/widget>
		 * &lt;/metric> </pre>
		 * @throws IOException
		 */
		public String toXML() throws IOException {
			StringBuffer buf = new StringBuffer("\t<metric>");
			buf.append("\n\t\t<name>" + name + "</name>");
			buf.append("\n\t\t<description><![CDATA[" + description + "]]></description>");
			buf.append("\n\t\t<type>" + type + "</type>");
			buf.append("\n\t\t<widget>" + widget + "</widget>");

			buf.append("\n\t</metric>");
			return buf.toString();
		}
		
		public JSONObject toJSON () {
			final JSONObject root = new JSONObject();
			try {
				root.put("name", 		name);
				root.put("description", description);
				root.put("type", 		type.name());
				root.put("widget", 		widget.name());
				
			} catch (JSONException e) {
				log.error("Metric " + name + " to JSON", e);
			}
			return root;
		}
		
		@Override
		public String toString() {
			return "{" + name + "," + description + "," + widget + "}";
		}
	}
	
	/**
	 * Basic branding information for a dashboard such as:
	 * <ul>
	 * <li>Background color
	 * <li>Logo file name
	 * </ul>
	 * @author vsilva
	 *
	 */
	static public class Branding {
		/** background color for the dash view */
		final String backgroundColor;
		
		/** A logo image rendered in the top left. Default size 245x26 */
		final String logo;
		
		public Branding() {
			backgroundColor = "#F5F5F5";
			logo 			= null;
		}

		public Branding(JSONObject root) {
			this(root.optString("bgColor"), root.optString("logo"));
		}
		
		public Branding(final String bgColor, final String logo) {
			this.backgroundColor 	= bgColor != null ? bgColor : "#F5F5F5";
			this.logo				= logo;
		}
		
		public String getBgColor () {
			return backgroundColor.startsWith("#") ? backgroundColor : "#" + backgroundColor;
		}
		
		public String getLogo () {
			return logo;
		}
		
		public String toXML () {
			final StringBuffer buf = new StringBuffer("\t<branding>");
			
			if ( backgroundColor != null ) { 
				buf.append("\n\t\t<bgColor>" 	+ backgroundColor + "</bgColor>");
			}
			if ( logo != null ) { 
				buf.append("\n\t\t<logo>" 		+ logo + "</logo>");
			}
			buf.append("\n\t</branding>");
			return buf.toString();
		}
		
		public JSONObject toJSON() throws JSONException {
			final JSONObject root = new JSONObject();
			root.putOpt("bgColor", backgroundColor);
			root.putOpt("logo", logo);
			return root;
		}
		
		@Override
		public String toString() {
			return "BgColor:" + backgroundColor + " Logo:" + logo;
		}
	}
	
	/** Title of this dashboard */
	final String title;
	
	/** TCP listener used to pop events */
	final String listener;
	
	/** Display key used to group metrics by: for example: VDN */
	final String key;
	
	/** Range used to filter the display key. Format: min, max (only vals between will be displayed */
	final String keyRange;

	/** 5/14/2019 A field name to be displayed in the dash heading alongside the key (for example SPLIT_NAME => SPLIT: SPLIT_NAME */
	final String headingField;

	/** List of metrics */
	final List<Metric> metrics = new ArrayList<Dashboard.Metric>();

	/** See {@link Branding} */
	Branding branding;
	
	/**
	 * Construct a dashboard.
	 * @param listener TCP listener (data source).
	 * @param key Display key filed name, similar to the 'SQL Group by' key. It is used to group metrics.
	 * @param keyRange Display key. range : min, max., for example (1-100 for numeric, Alice, Bob for alphabetic)
	 * @param heading A field name to be displayed in the dash heading alongside the key (for example SPLIT_NAME => SPLIT: SPLIT_NAME
	 */
	public Dashboard(final String title, final String listener, final String key, final String keyRange, final String heading) {
		super();
		this.title			= title;
		this.listener 		= listener;
		this.key 			= key;
		this.keyRange 		= keyRange;
		this.headingField	= heading;
	}

	public Dashboard(JSONObject root) throws JSONException {
		super();
		title 		= root.getString("title");
		listener 	= root.getString("listener");
		key 		= root.getString("displayKey");
		keyRange	= root.optString("keyRange");
		headingField = root.optString("displayHeading");
		
		// Add metrics
		JSONArray metrix = root.getJSONArray("metrics");
		for (int i = 0; i < metrix.length(); i++) {
			metrics.add(new Metric(metrix.getJSONObject(i)));
		}
		// Optional branding
		JSONObject b = root.optJSONObject("branding");
		if ( b != null ) {
			branding = new Branding(b);
		}
	}

	public String getTitle() {
		return title;
	}
	
	public String getListener() {
		return listener;
	}

	public String getKey() {
		return key;
	}

	public String getKeyRange() {
		return keyRange;
	}

	public String getHeadingField() {
		return headingField;
	}

	public List<Metric> getMetrics() {
		return Collections.unmodifiableList(metrics);
	}

	public int getMetricsSize () {
		return metrics.size();
	}
	
	public boolean hasPanels () {
		return hasWidget(WidgetType.PANEL) ;
	}

	public boolean hasGauges () {
		return hasWidget(WidgetType.GAUGE);
	}

	public boolean hasAreaCharts () {
		return hasWidget(WidgetType.AREA_CHART);
	}

	public boolean hasMarquees () {
		return hasWidget(WidgetType.MARQUEE);
	}
	
	public boolean hasWidget (WidgetType type) {
		for ( Metric m : metrics) {
			if ( m.widget == type)
				return true;
		}
		return false;
	}
	
	public void addMetric(String name, String description, MetricType type, WidgetType widget/*, String thresholdId*/) throws IOException {
		Metric m = new Metric(name, description, type, widget); //, thresholdId);
		addMetric(m);
	}

	public void addMetric(Metric metric ) throws IOException  {
		if ( metric.name == null )	 		throw new IOException("Matric name is required.");
		if ( metric.description == null )	throw new IOException("Matric description is required.");
		metrics.add(metric);
	}
	
	/**
	 * Serialize to XML.
	 * @return <pre>
	 * &lt;dashboard>
	 *  &lt;listener>CSPLIT Table&lt;/listener>
	 *  &lt;key>SPLIT&lt;/key>
	 *  &lt;keyRange>1,5&lt;/keyRange>
	 *  &lt;metric>
	 *    &lt;name>ACD&lt;/name>
	 *    &lt;description>Abandoned&lt;/description>
	 *    &lt;type>NUMBER&lt;/type>
	 *    &lt;widget>GAUGE&lt;/widget>
	 *  &lt;/metric>
	 * &lt;/dashboard></pre>
	 * @deprecated 11/01/2017 XML formats are deprecated. Use JSON instead.
	 * @throws IOException
	 */
	public String toXML() throws IOException {
		if ( title == null ) 	throw new IOException("Dashboard title is required.");
		if ( listener == null ) throw new IOException("Data source name is required.");
		if ( key == null ) 		throw new IOException("Dashboard display key is required.");
		
		final StringBuffer buf = new StringBuffer("<dashboard>");
		buf.append("\n\t<title><![CDATA[" 	+ title + "]]></title>");
		buf.append("\n\t<listener>" + listener + "</listener>");
		buf.append("\n\t<key>" 		+ key + "</key>");
		buf.append("\n\t<keyRange>" + (keyRange != null ? keyRange : "") + "</keyRange>");
		// 5/14/2019
		buf.append("\n\t<displayHeading>" + (headingField != null ? headingField : "") + "</displayHeading>");
		
		// Add metrics
		for (Metric m : metrics) {
			buf.append("\n" + m.toXML());
		}
		
		if ( branding != null) {
			buf.append("\n" + branding.toXML());
		}
		buf.append("\n</dashboard>");
		return buf.toString();
	}


	/**
	 * JSON serialize.
	 * @return Dash-board object JSON: <pre>
	 * { "title": "Call Metrics by SPLIT",
		"displayKey": "SPLIT",
		"metrics": [{
			"description": "Calls Waiting",
			"name": "CALLS_WAITING",
			"type": "NUMBER",
			"widget": "AREA_CHART"
		}, ...	],
		"listener": "CSPLIT Table",
		"keyRange": "1-3" 
		} </pre>
	 * @throws JSONException
	 */
	public JSONObject toJSON () throws JSONException {
		final JSONObject root = new JSONObject();
		// required
		root.put("title", title);
		root.put("listener", listener);
		root.put("displayKey", key);
		root.putOpt("keyRange", keyRange);
		root.putOpt("displayHeading", headingField);
		
		// Add metrics
		JSONArray metrix = new JSONArray();
		for (Metric m : metrics) {
			metrix.put(m.toJSON());
		}
		root.put("metrics", metrix);
		
		// 11/01/2017 fix for http://acme208.acme.com:6091/issue/UNIFIED_REPORTS-44
		if ( branding != null) {
			root.put("branding", branding.toJSON());
		}
		return root;
	}

	/**
	 * Find a metric by description.
	 * @param description
	 * @return
	 */
	public Metric findMetricByDesc (String description) {
		for ( Metric m : metrics) {
			if ( m.getDescription().equals(description)) {
				return m;
			}
		}
		return null;
	}
	
	/**
	 * See {@link Branding}.
	 * @param branding
	 */
	public void setBranding (Branding branding) {
		this.branding = branding;
	}
	
	public Branding getBranding() {
		return branding;
	}
	
	@Override
	public String toString() {
		return "Dash: " + title + " Branding:" + branding + " Metrics:" + metrics;
	}

	/**
	 * Get an Collections.unmodifiableList of metrics by {@link WidgetType}. 
	 * @param type See {@link WidgetType}.
	 * @return Unmodifiable array list of metrics by type.
	 */
	public List<Metric> getMetrics(WidgetType type) {
		List<Metric> list = new ArrayList<Metric>();
		for ( Metric m : metrics) {
			if ( m.widget == type)
				list.add(m);
		}
		return Collections.unmodifiableList(list);
	}
	
	/**
	 * Check if a dashboard has mixed widget sets
	 * @return True if the dash has mixed types: MARQUE, PANEL, CHART or false if it only has 1 type.
	 */
	public boolean isMixed () {
		Boolean[] bools 	= new  Boolean[4]; 
		bools[0] 			= hasAreaCharts();
		bools[1]			= hasGauges();
		bools[2] 			= hasMarquees();
		bools[3]			= hasPanels();
		int count 			= 0;
		for (int i = 0; i < bools.length; i++) {
			if ( bools[i]) {
				count++;
			}
		}
		return count > 1;
	}

}
