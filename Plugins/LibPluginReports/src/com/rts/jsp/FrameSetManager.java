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
 * Manages JSON frame sets in %HOME/.cloud/PRODUCT/Profiles/NAMNE
 * 
 * <pre>{"frameSets": [ {
  "frames": [
   {
    "dash": "Call Metrics By VDN",
    "style": "width:49%; height: 100%"
   },
   {
    "dash": "Call Metrics by SPLIT",
    "style": "width:50%; height: 100%"
   }
  ],
  "name": "View1"
 },...]} </pre>

 * @author VSilva
 * @version 1.0.0
 *
 */
public class FrameSetManager {
	public static final String JSONK_FRAMES = "frames";
	public static final String JSONK_DASH 	= "dash";
	public static final String JSONK_STYLE	= "style";
	public static final String JSONK_NAME	= "name";
	
	public static final String DEFAULT_SAVE_FILE_NAME = "frames.json";
	
	/**
	 * <pre>{ "frames": [ {
    "dash": "Call Metrics By VDN",
    "style": "width:49%; height: 100%"
   },
   {
    "dash": "Call Metrics by SPLIT",
    "style": "width:50%; height: 100%"
   }
  ],
  "name": "View1"
 } </pre>
	 * @author VSilva
	 *
	 */
	public static class FrameSet {
		final String name;
		final List<Frame> frames;
		
		public FrameSet(String name) {
			super();
			this.name 	= Objects.requireNonNull(name, "FrameSet name can't be null.");
			this.frames = new ArrayList<Frame>();
		}
		public FrameSet(JSONObject root) throws JSONException {
			this(root.getString(JSONK_NAME));
			JSONArray jframes 	= root.getJSONArray(JSONK_FRAMES);

			for (int i = 0; i < jframes.length(); i++) {
				JSONObject frm = jframes.getJSONObject(i);
				frames.add(new Frame(frm.getString(JSONK_DASH), frm.getString(JSONK_STYLE)));
			}
		}
		
		public void add (Frame frm) {
			frames.add(frm);
		}
		
		public JSONObject toJSON () throws JSONException {
			JSONObject root 	= new JSONObject();
			JSONArray jframes 	= new JSONArray();
			
			root.put(JSONK_NAME, name);
			
			for ( Frame f : frames) {
				jframes.put(f.toJSON());
			}
			root.put(JSONK_FRAMES, jframes);
			return root;
		}
		public String getName() {
			return name;
		}
		public List<Frame> getFrames() {
			return Collections.unmodifiableList(frames);
		}
	}
	
	/**
	 * <pre>{  "dash": "Call Metrics By VDN",
	 *      "style": "width:49%; height: 100%"
	 *  } </pre>
	 * @author VSilva
	 *
	 */
	public static class Frame {
		final String dashboard;
		final String style;
		
		public Frame(String dashboard, String style) {
			super();
			this.dashboard 	= Objects.requireNonNull(dashboard, "Frame: dash board name can't be null.");
			this.style 		= Objects.requireNonNull(style, "Frame: dash board style can't be null.");
		}
		
		public Frame(JSONObject root) throws JSONException {
			this(root.getString(JSONK_DASH), root.getString(JSONK_STYLE));
		}
		
		/**
		 * @return   JSON {   "dash": "Call Metrics By VDN",   "style": "width:49%; height: 100%"  }
		 * @throws JSONException
		 */
		public JSONObject toJSON () throws JSONException {
			JSONObject root = new JSONObject();
			root.put(JSONK_DASH, dashboard);
			root.put(JSONK_STYLE, style);
			return root;
		}
		public String getDashboard() {
			return dashboard;
		}
		public String getStyle() {
			return style;
		}
		@Override
		public String toString() {
			return String.format("%s %s", dashboard, style);
		}
	}
	
	/** List of FrameSet */
	private List<FrameSet> frameSets;
	
	/** Singleton instance */
	private static final FrameSetManager INSTANCE = new FrameSetManager();
	
	/** Load path %HOME%/.cloud/PRODUCT/Profiles/NAME */
	private final String loadPath ;
	
	public static FrameSetManager getInstance () {
		return INSTANCE;
	}
	
	/**
	 * Constructor
	 */
	private FrameSetManager()  {
		// load from disk at $user.home/.cloud/CloudReports/Default/frames.json
		frameSets 	= new CopyOnWriteArrayList<FrameSet>();
		loadPath	= CloudServices.getNodeConfig().getDefaultProfileBasePath() + File.separator + FrameSetManager.DEFAULT_SAVE_FILE_NAME;
		try {
			load(loadPath);
		}
		catch (FileNotFoundException e) {
			JSPLoggerTool.JSP_LOGW("FRAMESET", "Load from " + loadPath + " " +  e + " Using classpath.");
			
			// Not found, load samples from class path
			try {
				parse(IOTools.readFromStream(FrameSetManager.class.getResourceAsStream("/configuration/" + FrameSetManager.DEFAULT_SAVE_FILE_NAME)));
			} catch (Exception e2) {
				JSPLoggerTool.JSP_LOGE("FRAMESET", "Load from classpath /configuration/frames.json", e);
			}
		}
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("FRAMESET", "Load from " + loadPath, e);
		}
	}
	
	public void add (FrameSet fs) {
		FrameSet old = find(fs.getName());
		if ( old != null) {
			frameSets.remove(old);
		}
		frameSets.add(fs);
	}
	
	public FrameSet find (final String name) {
		for ( FrameSet fs : frameSets) {
			if ( fs.name.equals(name)) {
				return fs;
			}
		}
		return null;
	}
	
	public List<FrameSet> getFrameViews() {
		return Collections.unmodifiableList(frameSets);
	}
	
	/**
	 * @return {"frameSets": [ {  "frames": [  {  "dash": "Call Metrics By VDN"  "style": "width:49%; height: 100%"  }, { "dash": "Call Metrics by SPLIT", "style": "width:50%; height: 100%" }  ],  "name": "View1" },...]
	 * @throws JSONException on JSON parse errors.
	 */
	public JSONObject toJSON () throws JSONException {
		JSONObject root 	= new JSONObject();
		JSONArray jframes 	= new JSONArray();
		for ( FrameSet fs : frameSets) {
			jframes.put(fs.toJSON());
		}
		root.put("frameSets", jframes);
		return root;
	}
	
	/**
	 * Load frames (frames.json) from a disk location.
	 * @param location Full path to frames.json
	 * @throws FileNotFoundException If frames.json doesn't exist on disk.
	 * @throws JSONException If there are syntax errors in frames.json
	 * @throws IOException On disk I/O errors.
	 */
	private void load(final String location) throws FileNotFoundException, JSONException, IOException {
		parse (IOTools.readFileFromFileSystem(location));
	}
	
	private void parse(final String content) throws JSONException {
		JSONObject root = new JSONObject(content); //IOTools.readFileFromFileSystem(location));
		// parse
		JSONArray jframes 	= root.getJSONArray("frameSets");
		for (int i = 0; i < jframes.length(); i++) {
			JSONObject fs = jframes.getJSONObject(i);
			frameSets.add(new FrameSet(fs));
		}
	}
	
	public void save(final String location) throws IOException, JSONException {
		IOTools.saveText(location, toJSON().toString(1));
	}
	
	public void save() throws IOException, JSONException {
		IOTools.saveText(loadPath, toJSON().toString(1));
	}
	
}
