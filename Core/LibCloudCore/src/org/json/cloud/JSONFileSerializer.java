package org.json.cloud;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * A reusable class to load/save an array of JSON objects on disk.
 * <ul>
 * <li> The format: [{OBJ1},{OBJ2},...]
 * <li> Use it to track multiple JSON node files and save on code reuse.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 6/1/2019
 *
 */
public class JSONFileSerializer {
	private static final Logger log 		= LogManager.getLogger(JSONFileSerializer.class);
	
	private static final boolean DEBUG 		= true;
	
	static void TRACE(final String text) {
		if ( DEBUG ) {
			//System.out.println("[JSON-FILE] " + text); // TODO remove after test
			log.trace(text);
		}
	}

	/**
	 * Node objects must implement this interface so the elements can be stored in the internal hash map.
	 * @author VSilva
	 */
	public static interface BaseNode {
		String getId();
	}
	
	/** List of clusters (proxies) */
	private final Map<String, Object> clusters = new ConcurrentHashMap<String, Object>();

	/** Descriptor base path */
	public static final String CONFIG_BASEPATH 	= "CFG_BASE_PATH";
	
	/** Descriptor file name*/
	public static final String CONFIG_FNAME 	= "CFG_FILE_NAME";

	/** Descriptor class path (optional default load location from class-path) */
	public static final String CONFIG_CLASSPATH = "CFG_CLASS_PATH";
	
	/** Configuration descriptor path */
	private String basePath;

	/** default class path to use when the file is not found in the file syste */
	private String classPath;
	
	/** name of the nodes JSON file  @ $home/.cloud/[PRODUCT]/[FILE-NAME] */
	private /*static*/ String NODES_FILE_NAME; 

	/**
	 * Invoke once when the container starts.
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public void initialize(Map<String, String> config) throws JSONException, IOException {
		basePath 		= config.get(CONFIG_BASEPATH);
		classPath		= config.get(CONFIG_CLASSPATH);
		NODES_FILE_NAME = config.get(CONFIG_FNAME);
	}

	/**
	 * Initialize JSON file serializer
	 * @param basePath Base folder that contains the JSON file.
	 * @param fileName JSON file name.
	 * @throws JSONException on JSON parse errors.
	 * @throws IOException on I/O errors.
	 */
	public void initialize(final String basePath, final String classPath, final String fileName) throws JSONException, IOException {
		this.basePath 		= basePath;
		this.classPath		= classPath;
		NODES_FILE_NAME 	= fileName;
	}
	
	public void addNode (BaseNode node) {
		clusters.put(node.getId(), node);
	}
	
	public void removeNode(String name ) {
		clusters.remove(name);
	}

	/**
	 * Find an object of the serialized type.
	 * @param name Id of the object to search.
	 * @return Serialized object type or NULL if not found.
	 */
	public Object getNode(String name ) {
		return clusters.get(name);
	}

	private void checkValidity () throws IOException {
		if ( basePath == null ) {
			throw new IOException("Descriptor storage path cannot be null.");
		}
		if ( NODES_FILE_NAME == null ) {
			throw new IOException("Descriptor file name cannot be null.");
		}
	}
	
	/**
	 * Save nodes file to the given destination. For example: $home/.cloud/{PRODUCT}/nodes.json
	 * <b>Note:</b> Objects will be serialized as JSON using {@link JSONSerializer}. 
	 * 
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 */
	public void saveNodes() throws IOException, JSONException, IllegalAccessException {
		checkValidity();
		
		if (! FileTool.fileExists(basePath)) {
			throw new IOException("Path " + basePath + " doesn't exist.");
		}
		final String path = basePath + File.separator + NODES_FILE_NAME;
		
		IOTools.saveText(path, toJSON().toString(1));
		TRACE("Saved nodes to " + path);
	}

	/**
	 * Serialize objects to a JSON array of the form: [ NODE1, NODE2, ...]
	 * @return For example: [{ "keyStorePath": "C:\\Users\\vsilva/.cloud/CloudAdapter\\certs\\Node1.jks", "name": "Node1", "tlsEnabled": true, "hostPort": "192.168.99.100:2376", "keyStorePassword": "password"}]
	 * @throws JSONException On JSON errors.
	 * @throws IllegalAccessException On serialization errors.
	 */
	public JSONArray toJSON() throws IllegalAccessException {
		JSONArray array = new JSONArray();

		for ( Map.Entry<String, Object> entry : clusters.entrySet()) {
			Object val 				= entry.getValue();
			JSONObject serialized 	= JSONSerializer.serialize(val);
			array.put(serialized);
		}
		return array;
	}

	/**
	 * Load nodes from the initialized path. For example: $home/.cloud/{PRODUCT}/{FILENAME}.json
	 * <ul>
	 * <li> 10/19/2019: Added logic to load from the class path.
	 * </ul>
	 * @param clazz Any object type that is to serialized AS JSON and stored at the path given by initialize().
	 * 
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 * @throws InstantiationException On Object/JSON serialization errors.
	 */
	public void loadNodes(Class<?> clazz) throws JSONException, InstantiationException, IllegalAccessException, IOException {
		checkValidity();
		
		final String path 	= basePath + File.separator + NODES_FILE_NAME;
		String json 		= null;
		
		if (! FileTool.fileExists(path)) {
			// load from the class path if available
			if ( classPath != null) {
				json = IOTools.readFromStream(JSONFileSerializer.class.getResourceAsStream(classPath + "/" + NODES_FILE_NAME));
			}
		}
		else {
			json = IOTools.readFileFromFileSystem(path);
		}
		// not found anywhere
		if ( json == null ) {
			TRACE("LOAD: " + path + " not found.");
			return;
		}
		JSONArray array 	= new JSONArray(json);
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject jobj 	= array.getJSONObject(i);
			Object node 		= JSONSerializer.deserialize(jobj, clazz); 
			clusters.put( ((BaseNode)node).getId(), node);
		}
		TRACE("Loaded clusters: " + clusters);
	}

	/**
	 * Get an unmodifiable nodes map.
	 * @return A map of Object nodes indexed by id: {ID1 = OBJ1, ID2 = OBJ2, ...}
	 */
	public Map<String, Object> getClusters () {
		return Collections.unmodifiableMap(clusters);
	}

}
