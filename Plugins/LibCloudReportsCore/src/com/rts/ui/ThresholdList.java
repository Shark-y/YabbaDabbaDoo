package com.rts.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.rts.ui.Dashboard.Metric;

/**
 * <pre>
 * [{
    "alerts": [{
      "weight": 2,
      "level": 1,
      "color": "red"
    }],
    "metric": "F1",
    "listener": "CSPLIT Table"
  },
  {
    "alerts": [{
      "weight": 2,
      "level": 1,
      "color": "red"
    }],
    "metric": "SERVICELEVEL",
    "listener": "CSPLIT Table"
  }] </pre>
 * @author VSilva
 *
 */
public class ThresholdList implements Iterable<Threshold> {
	
	private static final Logger log = LogManager.getLogger(ThresholdList.class);
	

	public static class Alert {
		
		static public final String KEY_LEVEL 	= "level";
		static public final String KEY_WEIGHT = "weight";
		static public final String KEY_COLOR 	= "color";
		static public final String KEY_TRIGGER = "trigger";
		
		/** alert level/value */
		final int level;
		
		/** weight */
		final int weight;
		
		/** HTML color #RRGGBB */
		final String color;

		/** An action that triggers on display: NOTIFICATION (WebApi) or WINDOWFOCUS (focuses the browser window when hidden). */
		final String trigger;
		
		public Alert(final int level, final int weight, final String color, final String trigger) {
			super();
			this.level 		= level;
			this.weight 	= weight;
			this.color 		= color;
			this.trigger	= trigger;
		}
		
		/**
		 * @return
		 * <pre>{
		 * "weight": 2,
		 * "level": 1,
		 * "color": "red"
		 * } </pre>
		 * @throws JSONException
		 */
		JSONObject toJSON () throws JSONException {
			JSONObject root = new JSONObject();
			root.put(KEY_LEVEL, 	level);
			root.put(KEY_WEIGHT, 	weight);
			root.put(KEY_COLOR, 	color);
			root.put(KEY_TRIGGER,	trigger);
			return root;
		}
	}

	/** Path to the JSON descriptor */
	private final String basePath;
	
	/** XML descriptor name */
	private final String fileName;

	//final JSONArray list = new JSONArray();
	final List<Threshold> list = new CopyOnWriteArrayList<Threshold>();
	
	/**
	 * Load thresholds from the file system
	 * @param basePath Base folder.
	 * @param fileName Threshold file name.
	 */
	public ThresholdList(final String basePath, final String fileName) {
		this.basePath 	= basePath;
		this.fileName 	= fileName;
		try {
			load();
		} 
		catch (FileNotFoundException e) {
			log.warn("Failed to load thresholds from disk " + e.toString());
		}
		catch ( Exception ex) {
			log.error("Failed to load thresholds from disk ", ex);
		}
	}
	
	/**
	 * Load from the file system
	 * @throws Exception
	 */
	private void load() throws FileNotFoundException, JSONException, IOException {
		// load from FS
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream fis = new FileInputStream(basePath + File.separator + fileName); 
		IOTools.pipeStream(fis, bos);
		bos.close();
		fis.close();
		JSONArray a = new JSONArray(bos.toString());
		for ( int i = 0 ; i < a.length() ; i++) {
			addThreshold(new Threshold(a.getJSONObject(i).toString()));
		}
	}
	
	public Threshold find (Threshold  t) {
		for (Threshold tr  : list) {
			if ( tr.equals(t))
				return tr;
		}
		return null;
	}

	/**
	 * Find a {@link Threshold} by id: NAME@LISTENER-NAME
	 * @param id NAME@LISTENER-NAME
	 * @return A {@link Threshold}
	 */
	public Threshold find (final String  id) {
		for (Threshold tr  : list) {
			if ( tr.getId().equals(id))
				return tr;
		}
		return null;
	}
	
	/**
	 * Find a {@link Threshold} by metric name, data source & optional dashboard.
	 * @param metric {@link Metric} name.
	 * @param listener Data source name
	 * @param dashboard Optional {@link Dashboard} name.
	 * @return A {@link Threshold}.
	 */
	public Threshold find (final String  metric, final String listener, final String dashboard) {
		Threshold thresh = null;
		for (Threshold tr  : list) {
			if ( tr.getDashboard() != null && dashboard != null ) {
				if ( tr.getMetric().equals(metric) 
						&& tr.getListener().equals(listener) 
						&& tr.getDashboard().equals(dashboard)) 
				{
					thresh = tr;
					break;
				}
			}
			else {
				if ( tr.getMetric().equals(metric) 
						&& tr.getListener().equals(listener)
						&& (tr.getDashboard() == null) ) 
				{
					thresh = tr;
				}
			}
		}
		return thresh;
	}
	

	/**
	 * A a {@link Threshold} to the list. No duplicates are allowed.
	 * @param t {@link Threshold}
	 * @throws IOException If there is a duplicate name & listener matches.
	 */
	public void addThreshold (Threshold  t) throws IOException {
		// A min of 2 alerts are required
		if ( t.getAlerts() != null && t.getAlerts().length() < 2) {
			throw new IOException(t.getId() + " requires a minimum of 2 aterts.");
		}
		
		// Update? 
		Threshold t1 = find(t);
		
		if ( t1 != null ) { 
			//throw new IOException(t.getId() + " already exists.");
			list.remove(t1);
		}
		list.add(t);
	}
	
	/**
	 * Remove a threshold by listener and metric.
	 * @param listener Listener/queue name.
	 * @param metric Metric bound.
	 * @return
	 */
	public boolean remove (final String listener, final String metric) {
		log.debug("Remove metric " + metric + " from listener " + listener);
		for ( Threshold th : list) {
			//System.out.println("L:" + th.getListener() + " M:" + th.getMetric());
			if ( th.getListener().equals(listener) && th.getMetric().equals(metric)) {
				return list.remove(th);
			}
		}
		return false;
	}
	
	/**
	 * JSON serialize.
	 * @return <pre>[{
	 * "alerts": [{
	 * "weight": 2,
	 * "level": 1,
	 * "color": "red"
	 * }],
	 * "metric": "SERVICELEVEL",
	 * "listener": "CSPLIT Table"
	 * }]</pre>
	 * @throws JSONException
	 */
	public JSONArray toJSON () throws JSONException {
		JSONArray array = new JSONArray();
		for ( Threshold t : list) {
			array.put(t.toJSON());
		}
		return array; 
	}
	
	/**
	 * Save to the file system.
	 * @throws IOException
	 */
	public void save() throws Exception {
		if ( basePath == null) 	throw new IOException("Save: Missing descriptor base path.");
		if ( fileName == null)	throw new IOException("Save: Missing descriptor name.");
		
		JSONArray json 			= toJSON();
		FileOutputStream fos 	= null;
		String path				= basePath + File.separator + fileName;
		try {
			log.debug("Saving Threshold JSON to " + path);
			fos = new FileOutputStream(path);
			fos.write(json.toString(2).getBytes(CoreTypes.CHARSET_UTF8));
		} 
		finally {
			IOTools.closeStream(fos);
		}
	}

	public int size () {
		return list.size(); 
	}

	@Override
	public Iterator<Threshold> iterator() {
		return list.iterator();
	}
}
