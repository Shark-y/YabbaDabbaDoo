package com.rts.jsp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.JSPLoggerTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.services.CloudServices;

/**
 * Manages JSON aggregate sets in %HOME/.cloud/PRODUCT/Profiles/NAMNE/aggregates.json
 * 
 * <pre>{"aggregates":[{"groupByMetric":"NONE","dataSource":"C1AS Agent",
 * 	"metrics":[{"metric":"SPLIT","label":"skso"}],"description":"","name":"Global Queue","type":"SUM"},...]} </pre>
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class AggregatesManager {
	public static final String JSONK_FRAMES = "metrics";
	public static final String JSONK_METRIC = "metric";
	public static final String JSONK_MLBL	= "label";
	public static final String JSONK_NAME	= "name";
	public static final String JSONK_DESC	= "description";
	public static final String JSONK_DS		= "dataSource";
	public static final String JSONK_GBM	= "groupByMetric";
	public static final String JSONK_TYPE	= "type";
	
	public static final String DEFAULT_SAVE_FILE_NAME = "aggregates.json";
	
	public enum Type {SUM, AVERAGE};
	
	/**
	 * <pre>{"groupByMetric":"NONE" 
	 *  ,"dataSource":"C1AS Agent"
	 *  ,"metrics":[{"metric":"SPLIT","label":"skso"}]
	 *  ,"description":"","name":"Global Queue","type":"SUM"}</pre>
	 * @author VSilva
	 *
	 */
	public static class Aggregate {
		final String name;
		final String description;
		final String dataSource;
		final String groupByMetric;
		final List<Metric> metrics;
		final Type type;
		
		public Aggregate(String name, String description, String dataSource, Type type, String groupByMetric) {
			super();
			this.name 			= Objects.requireNonNull(name, "Aggregate name can't be null.");
			this.description 	= Objects.requireNonNull(description, "Aggregate name can't be null.");
			this.dataSource 	= Objects.requireNonNull(dataSource, "Aggregate dataSource can't be null.");
			this.metrics 		= new ArrayList<Metric>();
			this.type			= type;
			this.groupByMetric	= groupByMetric;
		}
		public Aggregate(JSONObject root) throws JSONException {
			this(root.getString(JSONK_NAME), root.getString(JSONK_DESC), root.getString(JSONK_DS), Type.valueOf(root.getString(JSONK_TYPE)), root.getString(JSONK_GBM));
			JSONArray jframes 	= root.getJSONArray(JSONK_FRAMES);

			for (int i = 0; i < jframes.length(); i++) {
				JSONObject frm = jframes.getJSONObject(i);
				metrics.add(new Metric(frm.getString(JSONK_METRIC), frm.getString(JSONK_MLBL)));
			}
		}
		
		public void add (Metric frm) {
			metrics.add(frm);
		}
		
		public JSONObject toJSON () throws JSONException {
			JSONObject root 	= new JSONObject();
			JSONArray jframes 	= new JSONArray();
			
			root.put(JSONK_NAME, name);
			root.put(JSONK_DESC, description);
			root.put(JSONK_DS, dataSource);
			root.put(JSONK_TYPE, type.name());
			root.put(JSONK_GBM, groupByMetric);

			for ( Metric f : metrics) {
				jframes.put(f.toJSON());
			}
			root.put(JSONK_FRAMES, jframes);
			return root;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public String getDataSource () {
			return dataSource;
		}
		public String getGroupByMetric() {
			return groupByMetric;
		}
		public Type getType() {
			return type;
		}
		public List<Metric> getMetrics() {
			return Collections.unmodifiableList(metrics);
		}
	}
	
	/**
	 * <pre>{"metric":"SPLIT","label":"Label"}</pre>
	 * @author VSilva
	 *
	 */
	public static class Metric {
		final String name;
		final String label;
		
		public Metric(String name, String style) {
			super();
			this.name 	= Objects.requireNonNull(name, "Aggregate: metric name can't be null.");
			this.label 	= Objects.requireNonNull(style, "Aggregate: metric label can't be null.");
		}
		
		public Metric(JSONObject root) throws JSONException {
			this(root.getString(JSONK_METRIC), root.getString(JSONK_MLBL));
		}
		
		/**
		 * @return JSON {"metric":"SPLIT","label":"label"}
		 * @throws JSONException
		 */
		public JSONObject toJSON () throws JSONException {
			JSONObject root = new JSONObject();
			root.put(JSONK_METRIC, name);
			root.put(JSONK_MLBL, label);
			return root;
		}
		public String getName() {
			return name;
		}
		public String getLabel() {
			return label;
		}
		@Override
		public String toString() {
			return String.format("%s %s", name, label);
		}
	}
	
	/** List of FrameSet */
	private List<Aggregate> frameSets;
	
	/** Singleton instance */
	private static final AggregatesManager INSTANCE = new AggregatesManager();
	
	/** Load path %HOME%/.cloud/PRODUCT/Profiles/NAME */
	private final String loadPath ;
	
	public static AggregatesManager getInstance () {
		return INSTANCE;
	}
	
	/**
	 * Constructor
	 */
	private AggregatesManager()  {
		// load from disk at $user.home/.cloud/CloudReports/Default/aggregates.json
		frameSets 	= new CopyOnWriteArrayList<Aggregate>();
		loadPath	= CloudServices.getNodeConfig().getDefaultProfileBasePath() + File.separator + AggregatesManager.DEFAULT_SAVE_FILE_NAME;
		try {
			//load(loadPath);
			parse(IOTools.readFileFromFileSystem(loadPath));
		} 
		catch (FileNotFoundException e) {
			JSPLoggerTool.JSP_LOGW("AGGREGATES", "Load from " + loadPath + " " +  e + ". Using class path.");
			
			// Not found, load samples from class path
			try {
				parse(IOTools.readFromStream(AggregatesManager.class.getResourceAsStream("/configuration/" + AggregatesManager.DEFAULT_SAVE_FILE_NAME)));
			} catch (Exception e2) {
				JSPLoggerTool.JSP_LOGE("FRAMESET", "Load from classpath /configuration/aggregates.json", e);
			}
		}
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("AGGREGATES", "Load from " + loadPath, e);
		}
	}
	
	public void add (Aggregate fs) {
		Aggregate old = find(fs.getName());
		if ( old != null) {
			frameSets.remove(old);
		}
		frameSets.add(fs);
	}
	
	public Aggregate find (final String name) {
		for ( Aggregate fs : frameSets) {
			if ( fs.name.equals(name)) {
				return fs;
			}
		}
		return null;
	}
	
	public List<Aggregate> getFrameViews() {
		return Collections.unmodifiableList(frameSets);
	}
	
	/**
	 * @return {"aggregates":[{"groupByMetric":"NONE","dataSource":"C1AS Agent","metrics":[{"metric":"SPLIT","label":"skso"}],"description":"n","name":"n","type":"SUM"},...]}
	 * @throws JSONException on JSON parse errors.
	 */
	public JSONObject toJSON () throws JSONException {
		JSONObject root 	= new JSONObject();
		JSONArray jframes 	= new JSONArray();
		for ( Aggregate fs : frameSets) {
			jframes.put(fs.toJSON());
		}
		root.put("aggregates", jframes);
		return root;
	}
	
//	private void load(final String location) throws FileNotFoundException, JSONException, IOException {
//	}
	
	private void parse (final String contents) throws JSONException { 
		JSONObject root = new JSONObject(contents); // IOTools.readFileFromFileSystem(location));
		// parse
		JSONArray jframes 	= root.getJSONArray("aggregates");
		for (int i = 0; i < jframes.length(); i++) {
			JSONObject fs = jframes.getJSONObject(i);
			frameSets.add(new Aggregate(fs));
		}
	}
	
	public void save(final String location) throws IOException, JSONException {
		IOTools.saveText(location, toJSON().toString(1));
	}
	
	public void save() throws IOException, JSONException {
		IOTools.saveText(loadPath, toJSON().toString(1));
	}
	
}
