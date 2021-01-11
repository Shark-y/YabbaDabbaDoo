package com.cloud.kubernetes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestException;
import com.cloud.core.w3.RestClient.HTTPDestination;

import org.json.cloud.JSONSerializer;

import com.cloud.docker.DockerHttpHijack;
import com.cloud.ssh.StreamIO;

public class Kubernetes {
	private static final Logger log 			= LogManager.getLogger(Kubernetes.class);
	private static final boolean DEBUG 			= true;
	
	/** name of the nodes files stores @ $home/.cloud/{PRODUCT} */
	private static final String NODES_FILE_NAME = "k8s-nodes.json";
	
	static void TRACE(final String text) {
		if ( DEBUG ) {
			//System.out.println("[DOCKER] " + text); // TODO remove after test
			log.trace(text);
		}
	}
	
	/** Descriptor base path */
	public static final String CONFIG_BASEPATH = "CFG_BASE_PATH";

	// Configuration descriptor path
	private static String basePath;
	
	/** List of clusters (proxies) */
	private static final Map<String, K8SNode> clusters = new ConcurrentHashMap<String, K8SNode>();

	/**
	 * Invoke once when the container starts.
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static void initialize(Map<String, String> config) throws JSONException, IOException {
		basePath = config.get(CONFIG_BASEPATH);
	}
	
	/**
	 * Invoke once when the container stops.
	 */
	public static void shutdown() {
	}

	public static void addNode (final String name, String apiServer, String accessToken, String sshUser, String sshPwd) throws JSONException, IOException {
		clusters.put(name, new K8SNode(name, apiServer, accessToken, sshUser, sshPwd));
	}
	
	public static void removeNode(String name ) {
		clusters.remove(name);
	}
	
	/**
	 * Convert to JSON: [ NODE1, NODE2, ...]
	 * @return [{ "keyStorePath": "C:\\Users\\vsilva/.cloud/CloudAdapter\\certs\\Node1.jks", "name": "Node1", "tlsEnabled": true, "hostPort": "192.168.99.100:2376", "keyStorePassword": "password"}]
	 * @throws JSONException On JSON errors.
	 * @throws IllegalAccessException On serialization errors.
	 */
	static JSONArray toJSON() throws IllegalAccessException {
		JSONArray array = new JSONArray();

		for ( Map.Entry<String, K8SNode> entry : clusters.entrySet()) {
			K8SNode dn 				= entry.getValue();
			JSONObject serialized 	= JSONSerializer.serialize(dn);
			array.put(serialized);
		}
		return array;
	}

	/**
	 * Save nodes file to the default destination $home/.cloud/{PRODUCT}/nodes.json
	 * 
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 */
	public static void saveNodes() throws IOException, JSONException, IllegalAccessException {
		if ( basePath == null ) {
			throw new IOException("Descriptor storage path cannot be null.");
		}
		if (! FileTool.fileExists(basePath)) {
			throw new IOException("Path " + basePath + " doesn't exist.");
		}
		final String path = basePath + File.separator + NODES_FILE_NAME;
		
		IOTools.saveText(path, toJSON().toString(1));
		TRACE("Saved nodes to " + path);
	}

	/**
	 * Load nodes from $home/.cloud/{PRODUCT}/nodes.json and swarms from $home/.cloud/{PRODUCT}/swarms.json
	 */
	public static void load() throws JSONException, InstantiationException, IllegalAccessException, IOException {
		loadNodes();
	}
	
	/**
	 * Load nodes from $home/.cloud/{PRODUCT}/nodes.json
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 * @throws InstantiationException On Object/JSON serialization errors.
	 */
	public static void loadNodes() throws JSONException, InstantiationException, IllegalAccessException, IOException {
		final String path 	= basePath + File.separator + NODES_FILE_NAME;
		
		if (! FileTool.fileExists(path)) {
			return;
		}
		
		JSONArray array 	= new JSONArray(IOTools.readFileFromFileSystem(path));
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject jobj = array.getJSONObject(i);
			K8SNode node 	= (K8SNode)JSONSerializer.deserialize(jobj, K8SNode.class); 
			clusters.put(node.name, node);
		}
		TRACE("Loaded clusters: " + clusters);
	}

	public static Map<String, K8SNode> getClusters () {
		return Collections.unmodifiableMap(clusters);
	}

	/**
	 * Get a {@link K8SNode} by name
	 * @param name Node name
	 * @return The node object or null if not found.
	 */
	public static K8SNode get (final String name) {
		return clusters.get(name);
	}

	public static boolean contains (final String name) {
		return clusters.containsKey(name);
	}

	public static JSONObject invokeApi (final String apiName, final String nodeName, final String content, final String contentType) 
			throws RestException, Exception 
	{
		return invokeApi(apiName, nodeName, content, contentType, null);
	}

	/**
	 * Get a {@link K8SNode} by name
	 * @param name Node name
	 * @return The node object or null if not found.
	 * @throws IOException If the node is not found (null)
	 */
	public static K8SNode getNode(final String nodeName) throws IOException {
		if ( nodeName == null) {
			throw new IOException("A node name is required.");
		}
		K8SNode node 	= clusters.get(nodeName);
		if ( node == null) {
			throw new IOException("Can't find node " + nodeName);
		}
		return node;
	}

	/**
	 * Get a node by its IP address.
	 * @param addr Node IP address.
	 * @return The {@link K8SNode} or null if not found.
	 * @throws MalformedURLException If the node has an invalid URL.
	 */
	public static K8SNode getByAddr(final String addr) throws MalformedURLException {
		for ( Map.Entry<String, K8SNode> entry : clusters.entrySet()) {
			K8SNode node = entry.getValue();
			if ( node.getHostName().equals(addr)) {
				return node;
			}
		}
		return null;
	}
	
	/**
	 * Invoke an API call.
	 * @param apiName The name of the API within the JSON descriptor.
	 * @param nodeName The target node name.
	 * @param content Optional request payload (may be null).
	 * @param contentType Optional content type or null.
	 * @param params A {@link Map} of parameter (KEY, VALUE) pairs to substitute in the request URI, and others.
	 * @return A {@link JSONObject} in data tables format: { 'data': [RESPOSE]}. Where RESPONSE = [JSONobect | JSONArray | Raw-response]
	 * @throws RestException If the target API is not found.
	 * @throws Exception On W3 I/O errors: connection timeouts, etc.
	 */
	public static JSONObject invokeApi (final String apiName, final String nodeName, final String content, final String contentType, Map<String, Object> params) 
			throws RestException, Exception 
	{
		return invokeApi(apiName, nodeName, content, contentType, params, true);
	}

	/**
	 * Invoke an API call.
	 * @param apiName The name of the API within the JSON descriptor.
	 * @param nodeName The target node name.
	 * @param content Optional request payload (may be null).
	 * @param contentType Optional content type or null.
	 * @param params A {@link Map} of parameter (KEY, VALUE) pairs to substitute in the request URI, and others.
	 * @param dataTables If true return the result in Data tables format { 'data': [RESPOSE]} else return the raw JSON response.
	 * @return A {@link JSONObject} in data tables format: { 'data': [RESPOSE]}. Where RESPONSE = [JSONobect | JSONArray | Raw-response]
	 * @throws RestException If the target API is not found.
	 * @throws Exception On W3 I/O errors: connection timeouts, etc.
	 */
	public static JSONObject invokeApi (final String apiName, final String nodeName, final String content, final String contentType, Map<String, Object> params, boolean dataTables) 
			throws RestException, Exception 
	{
		K8SNode node 		= getNode(nodeName);
		final String url 	= node.getApiServer();
		final boolean debug	= params.containsKey("DEBUG") ? Boolean.parseBoolean(params.get("DEBUG").toString()) : false ;
		
		// add access token
		params.put("TOKEN", node.getAcessToken());
		
		HTTPDestination dest 	= HTTPDestination.create(url, content, contentType, "TLS", null, null);
		dest.setDebug(debug);
		
		// create a client in memory for each request to speed things up in multithreaded envs
		final RestClient rest 	= new RestClient();
		rest.load(node.api);
		
		// 4/5/2020 This causes latencies of 20s+ in tomcat 
		//final Object obj		= node.rest.invoke(dest, apiName, params);
		final long t0			= System.currentTimeMillis();
		final Object obj		= rest.invoke(dest, apiName, params);
		final long t1			= System.currentTimeMillis();
		
		log.debug(String.format("[%6d] ms for %s @ %s", (t1-t0), apiName, nodeName));
		
		JSONObject root			= new JSONObject();
		
		// Data tables format?
		root.put("data", obj);
		
		return dataTables ? root : (JSONObject)obj;
	}
	
	/**
	 * Make an API call by API name and node name.
	 * @param apiName The name of the operation/call to invoke within the REST descriptor.
	 * @param nodeName Target node name
	 * @param descriptor if there is a payload to be delivered, the name of a payload template file. For example <i>/resources/docker/container_create.json</i> for <i>CreateContainer</i>.
	 * @param params A {@link Map} of (NAME, JSON-value) pairs to replace within the payload or URI. For example: <pre>{CMD=["nginx","-g","daemon off;"], ARGS=[], PORTS=[], PORTBINDINGS={"80/tcp":[{"HostPort":"80"}]}, VOLUMES={}, ENV=[], IMAGE=nginx, MOUNTS=, EXPOSEDPORTS={}, LABELS={}, NETWORKS=, BINDS=[]}</pre>
	 * @return Server response in data-tables format. For example for CretaeContainer: <pre>{"data":{"Id":"a5cd86e220292e7d2d2a1d2eefcc75ed33f43b3f0a1c10d609a237b9540529d3","Warnings":["linux does not support CPU percent. Percent discarded."]}}</pre>
	 * @throws RestException On docker server errors.
	 * @throws Exception On I/O, timeouts and other errors.
	 */
	static JSONObject invokeApi (final String apiName, final String nodeName, final String descriptor, Map<String, Object> params)
			throws RestException, Exception
	{
		String raw = IOTools.readFromStream(Kubernetes.class.getResourceAsStream(descriptor));
		
		// replace params in raw json
		String json = K8SParams.replace(params, raw);
		log.debug("Docker JSON <pre>" + json + "</pre>");
		
		// validate
		JSONObject root = new JSONObject(json);
		
		// Invoke
		JSONObject resp = invokeApi(apiName, nodeName, root.toString(), CoreTypes.CONTENT_TYPE_JSON, params);
		log.debug(apiName + ": Server replied:" + resp.toString());
		return resp;
	} 

	/**
	 * Execute a command inside a POD/container (all fields are required).
	 * @param nodeName Name of the node that contains all the information: API URL, token, etc.
	 * @param namespace The name space.
	 * @param podName Name of the pod.
	 * @param containerName Container name.
	 * @param commands Array of command to execute. For example: uname -a => ['uname', '-a']
	 */
	public static String executeCommandInPod(final String nodeName, final String namespace, final String podName, final String containerName, String[] command) 
			throws IOException, URISyntaxException 
	{
		K8SNode node 		= getNode(nodeName);
		
		final String cmd 	= "command=" + IOTools.join(command, "&command=");
		final String url 	= node.getApiServer() + "api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec" 
				+ "?container=" + containerName  + "&" + cmd // URLEncoder.encode(cmd, "UTF-8")
				+ "&stdin=false&stderr=false&stdout=true&tty=false";
		
		final String payload = "";
		
		log.debug("Execute CMD in ns/pod " + namespace + "/" +  podName + " URL=" + url);
		
		// Use the hijack stuff..
		DockerHttpHijack http 		= new DockerHttpHijack(new URI(url));

		// Required headers: see https://blog.openshift.com/executing-commands-in-pods-using-k8s-api/
		Map<String, String> headers = new HashMap<String, String>();			
		headers.put("Accept", "*/*");
		headers.put("Authorization", "Bearer " + node.getAcessToken());
		headers.put("Connection", "Upgrade");
		headers.put("Upgrade", "websocket");
		headers.put("Sec-WebSocket-Key", String.valueOf(System.currentTimeMillis()));
		headers.put("Sec-WebSocket-Version", "13");
		
		/* --------- HANDSHAKE
		 * GET /api/v1/namespaces/kube-system/pods/kube-scheduler-kubemaster/exec?command=/bin/date&stdin=true&stderr=true&stdout=true&tty=true&container=kube-scheduler HTTP/1.1
		Upgrade: websocket
		Connection: Upgrade
		Host: 10.226.67.20
		Sec-WebSocket-Version: 13
		Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tMjJqejkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjAyMzI0NDljLTViYjUtMTFlOS05N2VjLTA4MDAyN2UxNjRiMCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.rlchRPnBEkyW5GpTF2S-_guYMZMAk5cszA5KR6uJ0YGacEyoMYlQHcqwWEZF8S5sNyWnsVWAkZvl2moIdGTdpfeIW7mILPFwcTQGIk0rHUruUZnyGB17VsDG8wPfN-l9GdrIALGX3iHJeuIEt-hbsUcHia0Ivv4ITXfyfFz4htrxaxImJXsFxxzTDpOxz_SKnupAtlLslm0tS_r0xt4WsQuz9reSkTmNHsOHry-cPFDP99LT84AaJwq3Pv50qQ_JY-Wavj9ZdNCDUSjjsLhjQmz4wspjxaTkSoa88TplxmaQFsLDp6i0rX14amoeVMpGVD3O1eg_ZuKw-7UCfMnTEw
		Upgrade: websocket
		Sec-WebSocket-Key: 1560724735090
		Connection: Upgrade
		Accept: **
		Content-Length: 0
		
		
		[DOCKER-HTTP-HIJACK] Response line: HTTP/1.1 101 Switching Protocols
		[DOCKER-HTTP-HIJACK] Response line: Upgrade: websocket
		[DOCKER-HTTP-HIJACK] Response line: Connection: Upgrade
		[DOCKER-HTTP-HIJACK] Response line: Sec-WebSocket-Accept: xZvH8e7MVQxCxe2ZYYL945WqrV0=
		[DOCKER-HTTP-HIJACK] Response line: Sec-WebSocket-Protocol: 
		[DOCKER-HTTP-HIJACK] Response line: 
		[DOCKER-HTTP-HIJACK] Handshake complete
		‚‚‚Sun Jun 16 22:38:56 UTC 2019
		 */

		http.service("GET", headers, payload, true, "websocket");
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		http.pipeStdout( new StreamIO.PrintStreamSource() {
			@Override
			public PrintStream getPrintStream() {
				return new PrintStream(out);
			}

			@Override
			public boolean closeAfterWrite() {
				return false;
			}
		}); 
		
		try {
			// Gotta wait a bit for output :(
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		out.close();
		return out.toString();
	}
	
	/**
	 * @return Default certs path: $HOME/.cloud/$PRODUCT/certs
	 */
	public static String getCertsPath () {
		return basePath != null ? basePath + File.separator + "certs" : null;
	}

	/**
	 * Save a private key in PUTTY for SSH access. Keys are save in $HOME/.cloud/$PRODUCT/certs
	 * @param alias Node name or alias.
	 * @param ppkKey PuTTY private key.
	 * @return Path to the key.
	 * @throws Exception
	 */
	public static String saveKey (String alias, String ppkKey) throws Exception {
		final String path = getCertsPath();
		
		if ( path == null ) {
			throw new IOException("Descriptor storage path cannot be null.");
		}
		if ( !IOTools.mkDir(path) ) {
			throw new IOException("Failed to create " + path);
		}
		final String keyPath 	= path + File.separator + alias + "-key.ppk";
		
		// save Key
		IOTools.saveText(keyPath, ppkKey);
		return keyPath;
	}
	
}
