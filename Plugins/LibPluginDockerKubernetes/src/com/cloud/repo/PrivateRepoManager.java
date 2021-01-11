package com.cloud.repo;

import java.io.IOException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.w3.RestException;
import com.cloud.repo.PrivateRepo.RepoType;

import org.json.cloud.JSONFileSerializer;


/**
 * Private repo helper class for:
 * <ul>
 * <li>Google GCR
 * <li>Docker hub privates
 * <li>HELM privates
 * </ul>
 * 
 * <pre>Map<String, String> config = new HashMap<String, String>();
 * config.put(JSONFileSerializer.CONFIG_BASEPATH, "C:\\Users\\vsilva\\.cloud\\CloudAdapter");
 * PrivateRepoManager.init(config);
 * PrivateRepoManager.loadNodes();
 * System.out.println("ONLOAD: " + PrivateRepoManager.getRepos());
 * PrivateRepoManager.addRepo(RepoType.GOOGLE.name(), "https://us.gcr.io/v2/", "cloud-bots" , "_json_key", "[KEY]");
 * PrivateRepoManager.save(); </pre>
 * 
 * @author VSilva
 *
 */
public class PrivateRepoManager  {
	
	/** name of the nodes files stores @ $home/.cloud/{PRODUCT} */
	private static final String NODES_FILE_NAME = "repos.json";

	/** Used to serialize the {@link PrivateRepo} class */
	private static final JSONFileSerializer ser = new JSONFileSerializer();
	
	/**
	 * Invoke once when the container starts.
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static void init(Map<String, String> config) throws JSONException, IOException {
		config.put(JSONFileSerializer.CONFIG_FNAME, NODES_FILE_NAME);
		ser.initialize(config);
	}

	/**
	 * Save nodes file to the default destination $home/.cloud/{PRODUCT}/nodes.json
	 * 
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 */
	
	public static void save() throws IOException, JSONException, IllegalAccessException {
		ser.saveNodes();
	} 


	/**
	 * Load nodes from $home/.cloud/{PRODUCT}/nodes.json
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 * @throws InstantiationException On Object/JSON serialization errors.
	 */
	
	public static void loadNodes() throws JSONException, InstantiationException, IllegalAccessException, IOException {
		ser.loadNodes(PrivateRepo.class);
	} 

	/**
	 * Add a repo containing Docker images or HELM charts.
	 * @param type GOOGLE, DOCKER or HELM matching {@link RepoType}.
	 * @param url Repo URL.
	 * @param name Repo unique name or id.
	 * @param user Optional user if authentication is required.
	 * @param password Optional password.
	 */
	public static void addRepo (final String type, final String url, final String name, final String user, final String password) {
		PrivateRepo repo = new PrivateRepo(type, url, name, user, password);
		ser.addNode(repo);
	}
	
	/**
	 * Remove repository by id(name)
	 * @param name Repository name (id).
	 */
	public static void remove (final String name) {
		ser.removeNode(name);
	}
	
	public static Map<String, Object> getRepos () {
		return ser.getClusters();
	}

	public static JSONArray toJSON() throws IllegalAccessException {
		return ser.toJSON();
	}
	
	public static PrivateRepo getRepo (final String name) {
		return (PrivateRepo)ser.getClusters().get(name);
	}

	/**
	 * Get TAGs (images) from GCR, DOCKER or HELM
	 * @return JSOM Array: [ "tag1",....]
	 * @throws RestException on Docker client error.
	 * @throws IOException On REPO not found, I/O/Network errors.
	 * @throws JSONException On JSON parsing errors.
	 */
	public static JSONObject /*Array */ fetchTags (final String name) throws RestException, IOException, JSONException {
		PrivateRepo repo 	= getRepo(name);
		if ( repo == null) {
			throw new IOException("Can't find repo " + name);
		}
		return repo.fetchTags();
	}
	
}
