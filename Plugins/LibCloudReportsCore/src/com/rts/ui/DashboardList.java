package com.rts.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.rts.ui.Dashboard.Branding;
import com.rts.ui.Dashboard.Metric;
import com.rts.ui.Dashboard.MetricType;
import com.rts.ui.Dashboard.WidgetType;

/**
 * <pre>
 * &lt;dashboards>
 * &lt;dashboard>
 *  &lt;listener>CSPLIT Table&lt;/listener>
 *  &lt;key>SPLIT&lt;/key>
 *  &lt;keyRange>1,5&lt;/keyRange>
 *  &lt;metric>
 * 	  &lt;name>ACD&lt;/name>
 * 	  &lt;description>Abandoned&lt;/description>
 * 	  &lt;type>NUMBER&lt;/type>
 * 	  &lt;widget>GAUGE&lt;/widget>
 *  &lt;/metric>
 * &lt;/dashboard>
 * &lt;/dashboards>
 * </pre>
 * @author VSilva
 *
 */
public class DashboardList {
	
	private static final Logger log = LogManager.getLogger(DashboardList.class);

	/** Path to the XML descriptor */
	private final String basePath;
	
	/** XML descriptor name */
	private final String fileName;

	private final List<Dashboard> dashes;
	
	/**
	 * XML SAX Parser for the XML above.
	 * @author VSilva
	 *
	 */
	private class DataHandler extends DefaultHandler {
		private StringBuffer buffer;
		private String title, listener, key, keyRange, heading, metricName, metricDesc, metricType, metricWid; //, thresholdId;
		private List<Metric> metrics = new ArrayList<Dashboard.Metric>();
		
		// Branding
		private String bgColor, logo;
		
		@Override
		public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
			buffer = new StringBuffer();
			if ( name.equals("dashboard")) {
				metrics.clear();
			}
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			String data = buffer.toString();

			if ( name.equals("title")) 			title		= data;		// Dash title
			if ( name.equals("listener")) 		listener 	= data;		// Data source name/id
			if ( name.equals("key")) 			key 		= data;		// Group-by (display) key/field
			if ( name.equals("keyRange")) 		keyRange 	= data;		// Key range: numeric 1-100, Alpha: A,B,...
			if ( name.equals("displayHeading")) heading 	= data;		// 5/14/2019: New optional field heading - shown next to the key 
			
			// metric stuff
			if ( name.equals("name"))			metricName 	= data;
			if ( name.equals("description")) 	metricDesc 	= data;
			if ( name.equals("type")) 			metricType 	= data;
			if ( name.equals("widget")) 		metricWid 	= data;
			
			// branding (optional)
			if ( name.equals("bgColor")) 		bgColor 	= data;
			if ( name.equals("logo")) 			logo 		= data;
			
			if ( name.equals("metric")) {
				metrics.add(new Metric(metricName, metricDesc , MetricType.valueOf(metricType), WidgetType.valueOf(metricWid) )); //, thresholdId));
			}
			if ( name.equals("dashboard")) {
				try {
					Branding branding = bgColor != null ? new Branding(bgColor, logo) : null;
					
					log.debug("Loading dash from FS " + title + " DS:" + listener + " Braning:" + branding);
					addDashboard(title, listener, key, keyRange, heading, metrics, branding);
					
					// must reset optional stuff
					bgColor = logo = null;
				} catch (IOException e) {
					// TODO: Handle parse error
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer != null) {
            	buffer.append(ch ,start ,length);
            }
		}
	}
	
	/**
	 * Construct a dashboard list
	 * @param basePath Base path of the XML descriptor.
	 * @param fileName XML descriptor file name.
	 * @throws IOException
	 * @throws JSONException 
	 */
	public DashboardList(final String basePath, final String fileName) throws IOException {
		super();
		this.basePath 	= basePath;
		this.fileName 	= fileName;
		this.dashes		= new CopyOnWriteArrayList<Dashboard>();
		
		if ( fileName.contains(".xml")) {
			parseXML(basePath, fileName);
		}
		else {
			parseAsJSON(basePath, fileName);
		}
	}

	/**
	 * Parse the listener XML from the file system.
	 * @param path Descriptor path.
	 * @param descriptorName XML file name.
	 * @throws IOException
	 */
	private void parseXML (final String path, final String descriptorName) throws IOException {
		InputStream is	= null;
		try {
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			is 			= IOTools.findStream(path, descriptorName) ; //, searchFileSystem, searchClassPath);
			
			if ( is == null) {
				throw new Exception("Missing resource " + descriptorName + " @ " + path);
			}
			p.parse(is, new DataHandler());
		}
		catch ( Exception e) {
			throw new IOException(e);
		}
		finally {
			IOTools.closeStream(is);
		}
	}

	private void parseAsJSON (final String path, final String descriptorName) throws IOException {
		log.debug("Loading dashboards from JSON " + path + " file: " + descriptorName );
		InputStream is		= null;
		OutputStream out	= new ByteArrayOutputStream();
		try {
			is 				= IOTools.findStream(path, descriptorName) ; 
			IOTools.pipeStream(is, out);
			final String json = out.toString();
			
			if ( json == null || json.isEmpty()) {
				log.warn("Load dashboards. No JSON found @ class-path or " + path + "[" + descriptorName + "]");
				return;
			}
			JSONObject root = new JSONObject(out.toString());
			JSONArray array = root.optJSONArray("dashboards");
			
			for (int i = 0; i < array.length(); i++) {
				dashes.add(new Dashboard(array.getJSONObject(i)));
			}
		} 
		catch (IOException e) {
			log.warn("LOAD: " + e.toString());
		}
		catch (JSONException e) {
			throw new IOException(e);
		}
		finally {
			IOTools.closeStream(is);
			IOTools.closeStream(out);
		}
	}
	
	/**
	 * Save to the file system.
	 * @throws IOException
	 */
	public void save() throws IOException {
		if ( basePath == null) 	throw new IOException("Save: Missing descriptor base path.");
		if ( fileName == null)	throw new IOException("Save: Missing descriptor name.");
		
		JSONObject root 		= new JSONObject();
		FileOutputStream fos 	= null;
		String path				= basePath + File.separator + fileName;
		try {
			root.put("dashboards", toJSON());
			
			log.debug("Saving Dashboards to " + path);

			//final String buf = toXML();
			final String buf = root.toString(1);
			
			fos = new FileOutputStream(path);
			fos.write(buf.getBytes(CoreTypes.CHARSET_UTF8));
			fos.close();
		} 
		catch (JSONException ex) {
			throw new IOException(ex);
		}
		finally {
			IOTools.closeStream(fos);
		}
	}
	
	/**
	 * Add a dashboard
	 * @param title	Name (required)
	 * @param listener Data source (required)
	 * @param key Group by key (required)
	 * @param keyRange Group by key range (optional)
	 * @param heading A field name to be displayed in the dash heading alongside the key (for example SPLIT_NAME => SPLIT: SPLIT_NAME.
	 * @param metrics List of metrics (required- NOT NULL)
	 * @param branding Optional {@link Branding} (optional -may be NULL).
	 * @throws IOException
	 */
	public Dashboard addDashboard(final String title, final String listener, final String key, final String keyRange, final String heading, final List<Metric> metrics, final Branding branding) throws IOException {
		// create a new Dash & add metrics...
		final Dashboard dash = new Dashboard(title, listener, key, keyRange, heading);
		
		for ( Metric m : metrics) {
			dash.addMetric(m);
		}
		
		dash.setBranding(branding);
		
		// Edit dashboard? 
		for ( Dashboard d : dashes) {
			if (d.getTitle().equals(title)) {
				//throw new IOException(title + " already exists.");
				dashes.remove(d);
				break;
			}
		}
		dashes.add(dash);
		return dash;
	}
	
	
	/**
	 * Remove a dashboard by its title.
	 * @param title Unique title.
	 * @return True if removed.
	 */
	public boolean remove(final String title)  {
		for ( Dashboard  dash : dashes) {
			if ( dash.getTitle().equals(title)) {
				dashes.remove(dash);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Serialize to XML.
	 * @return
	 * <pre>
	 * &lt;dashboards>
	 * &lt;dashboard>
	 *  &lt;listener>CSPLIT Table&lt;/listener>
	 *  &lt;key>SPLIT&lt;/key>
	 *  &lt;keyRange>1,5&lt;/keyRange>
	 *  &lt;metric>
	 * 	  &lt;name>ACD&lt;/name>
	 * 	  &lt;description>Abandoned&lt;/description>
	 * 	  &lt;type>NUMBER&lt;/type>
	 * 	  &lt;widget>GAUGE&lt;/widget>
	 *  &lt;/metric>
	 * &lt;/dashboard>
	 * &lt;/dashboards>
	 * </pre>
	 * @throws IOException
	 */
	public String toXML() throws IOException {
		StringBuffer buf = new StringBuffer("<dashboards>");
		for (Dashboard dash : dashes) {
			buf.append("\n" + dash.toXML());
		}
		buf.append("\n</dashboards>");
		return buf.toString();
	}

	/**
	 * Get dash boards.
	 * @return JSON Array of dash-board objects: <pre>
	 * [{ "title": "Call Metrics by SPLIT",
		"displayKey": "SPLIT",
		"metrics": [{
			"description": "Calls Waiting",
			"name": "CALLS_WAITING",
			"type": "NUMBER",
			"widget": "AREA_CHART"
		}, {
			"description": "Abandoned",
			"name": "ABNCALLS",
			"type": "NUMBER",
			"widget": "AREA_CHART"
		}, ...	],
		"listener": "CSPLIT Table",
		"keyRange": "1-3"
	}, {
		"title": "Call Metrics By VDN",
		"displayKey": "VDN",
		"metrics": [{
			"description": "Calls Waiting",
			"name": "CALLS_WAITING",
			"type": "NUMBER",
			"widget": "GAUGE"
		}, ... ],
		"listener": "CVDN Table",
		"keyRange": "1000,1001"
	}, ...	] </pre>
	 * @throws JSONException
	 */
	public JSONArray toJSON () throws JSONException {
		JSONArray array = new JSONArray();
		for ( Dashboard dash : dashes ) {
			array.put(dash.toJSON());
		}
		return array; 
	}

	public List<Dashboard> getList() {
		return Collections.unmodifiableList(dashes);
	}

	/**
	 * Find all dashboards for a given data source.
	 * @param listenerName Data source (listener) name.
	 * @return List of {@link Dashboard} matching data source name.
	 */
	public List<Dashboard> findByDataSource( String listenerName) {
		final List<Dashboard> list = new ArrayList<Dashboard>();
		for (Dashboard dash : dashes) {
			if ( dash.getListener().equals(listenerName)) {
				list.add(dash);
			}
		}
		return list;
	}

	/**
	 * Find all dashboards NAMES for a given data source.
	 * @param listenerName Data source (listener) name.
	 * @return List of {@link Dashboard} names(titles) matching data source name.
	 */
	public List<String> findNamesByDataSource( String listenerName) {
		List<String> list = new ArrayList<String>();
		for (Dashboard dash : dashes) {
			if ( dash.getListener().equals(listenerName)) {
				list.add(dash.getTitle());
			}
		}
		return list;
	}
	
	/**
	 * Find the 1st dashboard by it's listener name and display key
	 * @param dashName Dashboard name or title.
	 * @param listenerName. Listener (data source) name.
	 * @param displayKey Key used to filter records from the TCP buffer.
	 * @return {@link Dashboard} or null if not found.
	 */
	public Dashboard find (String dashName, String listenerName, String displayKey) {
		for (Dashboard dash : dashes) {
			if ( dash.getTitle().equals(dashName) && dash.getListener().equals(listenerName) && dash.getKey().equals(displayKey)) {
				return dash;
			}
		}
		return null;
	}
	
	/**
	 * Find the 1st dashboard by it's title (name)
	 * @param title. Dashboard name/title. Note titles may not be unique.
	 * @return {@link Dashboard} or null if not found.
	 */
	public Dashboard find (String title) {
		for (Dashboard dash : dashes) {
			if ( dash.title.equals(title)) {
				return dash;
			}
		}
		return null;
	}

}
