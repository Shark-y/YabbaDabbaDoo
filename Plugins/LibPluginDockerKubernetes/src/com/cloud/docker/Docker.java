package com.cloud.docker;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.Base64;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestException;
import com.cloud.core.w3.WebClient;
import com.cloud.core.w3.RestClient.HTTPDestination;
import com.cloud.rest.KeyStoreTool;

import org.json.cloud.JSONSerializer;

/**
 * Entry point to all docker operations. This class should be invoked by the container or a JSP page.
 * 
 * @author VSilva
 *
 */
public class Docker {

	private static final Logger log = LogManager.getLogger(Docker.class);

	private static final boolean DEBUG = true;
	
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
	private static final Map<String, DockerNode> clusters = new ConcurrentHashMap<String, DockerNode>();

	
	/**
	 * Invoke once when the container starts.
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static void initialize(Map<String, String> config) throws JSONException, IOException {
		basePath = config.get(CONFIG_BASEPATH);
		//  Fix for javax.net.ssl.SSLException by setting https.protocols = TLSv1,TLSv1.1,TLSv1.2 <pre>ClientHello, TLSv1
		// MOVED to DAEMON module Security.fixSSLFatalProtocolVersion();
	}

	/**
	 * Invoke once when the container stops.
	 */
	public static void shutdown() {
	}

	public static void addNode (final String name, String hostPort, boolean tlsEnabled, String keyStorePath, String keyStorePassword) throws JSONException, IOException {
		clusters.put(name, new DockerNode(name, hostPort, tlsEnabled, keyStorePath, keyStorePassword));
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

		for ( Map.Entry<String, DockerNode> entry : clusters.entrySet()) {
			DockerNode dn 			= entry.getValue();
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
		final String path = basePath + File.separator + "nodes.json";
		
		IOTools.saveText(path, toJSON().toString(1));
		TRACE("Saved nodes to " + path);
	}

	/**
	 * Load nodes from $home/.cloud/{PRODUCT}/nodes.json and swarms from $home/.cloud/{PRODUCT}/swarms.json
	 */
	public static void load() throws JSONException, InstantiationException, IllegalAccessException, IOException {
		loadNodes();
		loadSwarms();
	}
	
	/**
	 * Load nodes from $home/.cloud/{PRODUCT}/nodes.json
	 * @throws IOException If the storage path cannot is null or doesn't exist.
	 * @throws JSONException On JSON syntax errors.
	 * @throws IllegalAccessException On Object/JSON serialization errors.
	 * @throws InstantiationException On Object/JSON serialization errors.
	 */
	public static void loadNodes() throws JSONException, InstantiationException, IllegalAccessException, IOException {
		final String path 	= basePath + File.separator + "nodes.json";
		
		if (! FileTool.fileExists(path)) {
			return;
		}
		
		JSONArray array 	= new JSONArray(IOTools.readFileFromFileSystem(path));
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject jobj = array.getJSONObject(i);
			DockerNode node = (DockerNode)JSONSerializer.deserialize(jobj, DockerNode.class); //new Docker(array.getJSONObject(i));
			clusters.put(node.name, node);
		}
		TRACE("Loaded clusters: " + clusters);
	}

	public static Map<String, DockerNode> getClusters () {
		return Collections.unmodifiableMap(clusters);
	}

	/**
	 * Get a docker node.
	 * @param name Node name.
	 * @return A {@link DockerNode} or null if not found.
	 */
	public static DockerNode get (final String name) {
		return clusters.get(name);
	}

	public static boolean contains (final String name) {
		return clusters.containsKey(name);
	}
	
	/**
	 * Get a node descriptor {@link DockerNode} by it's IP address.
	 * @param addr An IP address.
	 * @return The {@link DockerNode} for that IP or null if not found.
	 */
	public static DockerNode getByAddr (final String addr) {
		for ( Map.Entry<String, DockerNode> entry : clusters.entrySet()) {
			DockerNode node = entry.getValue();
			if ( node.hostPort.contains(addr)) {
				return node;
			}
		}
		return null;
	}
	
	public static String getCertsPath () {
		return basePath != null ? basePath + File.separator + "certs" : null;
	}
	
	/**
	 * Save the TLS: Certificate (PEM), Key (PEM) and Java key store (JKS)  @ $home/.cloud/{PRODUCT}/certs
	 * @param alias The prefix of all files.
	 * @param pemCert Client certificate @ $home/.cloud/{PRODUCT}/certs/{alias}-cert.pem
	 * @param pemKey Client key @ $home/.cloud/{PRODUCT}/certs/{alias}-key.pem
	 * @param ksPwd Key store password @ $home/.cloud/{PRODUCT}/certs/{alias}.jks
	 * @throws Exception
	 */
	public static void saveCerts (String alias, String pemCert, String pemKey, String ksPwd) throws Exception {
		final String path = getCertsPath();
		if ( path == null ) {
			throw new IOException("Descriptor storage path cannot be null.");
		}
		if ( !IOTools.mkDir(path) ) {
			throw new IOException("Failed to create " + path);
		}
		final String certPath 	= path + File.separator + alias + "-cert.pem";
		final String keyPath 	= path + File.separator + alias + "-key.pem";
		final String storePath	= path + File.separator + alias + ".jks";
		
		// save PEMs
		IOTools.saveText(certPath, pemCert);
		IOTools.saveText(keyPath, pemKey);
		
		// Create Java KS
		KeyStoreTool.createStoreFromPEM(certPath, keyPath, ksPwd, alias, storePath);
	}
	
	static String getCertFile(final String node, final String postfix) throws IOException {
		final String path = getCertsPath();
		if ( path == null ) {
			throw new IOException("Descriptor storage path cannot be null.");
		}
		final String certPath 	= path + File.separator + node + postfix;
		return FileTool.fileExists(certPath) ? IOTools.readFileFromFileSystem(certPath) : "";
	}

	/**
	 * Get the node PEM certificate
	 * @param node Node name.
	 * @return PEM string.
	 * @throws IOException on I/O errors.
	 */
	public static String getCertPEM(final String node) throws IOException {
		return getCertFile(node, "-cert.pem");
	}

	/**
	 * Get the node PEM key
	 * @param node Node name.
	 * @return PEM string.
	 * @throws IOException on I/O errors.
	 */
	public static String getKeyPEM(final String node) throws IOException {
		return getCertFile(node, "-key.pem");
	}
	
	/**
	 * Invoke an API call.
	 * @param apiName The name of the API within the JSON descriptor.
	 * @param nodeName The target node name.
	 * @return A {@link JSONObject} in data tables format: { 'data': [RESPONSE]}. Where RESPONSE = [JSONobect | JSONArray | Raw-response]
	 * @throws RestException If the target API is not found.
	 * @throws Exception On W3 I/O errors: connections timeouts, etc.
	 */
	public static JSONObject invokeApi (final String apiName, final String nodeName) throws RestException, Exception {
		String content 		= null;
		String contentType 	= null;
		return invokeApi(apiName, nodeName, content, contentType);
	}

	public static JSONObject invokeApi (final String apiName, final String nodeName, final String content, final String contentType) 
			throws RestException, Exception 
	{
		return invokeApi(apiName, nodeName, content, contentType, null);
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
		DockerNode node 	= getNode(nodeName);
		final String url 	= node.buildBaseUrl();
		
		final Object obj	= node.rest.invoke(HTTPDestination.create(url, content, contentType, node.isTlsEnabled() ? "TLS" : null, node.keyStorePath, node.keyStorePassword), apiName, params);
		JSONObject root		= new JSONObject();
		
		// Data tables format
		root.put("data", obj);
		return root;
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
		String raw = IOTools.readFromStream(Docker.class.getResourceAsStream(descriptor));
		
		// replace params in raw json
		String json = DockerParams.replace(params, raw);
		log.debug("Docker JSON <pre>" + json + "</pre>");
		
		// validate
		JSONObject root = new JSONObject(json);
		
		// Invoke
		JSONObject resp = Docker.invokeApi(apiName, nodeName, root.toString(), CoreTypes.CONTENT_TYPE_JSON, params);
		log.debug(apiName + ": Server replied:" + resp.toString());
		return resp;
	} 
	
	/**
	 * Execute a shell command in a node/container. This methods creates:
	 * <ul>
	 * <li> Exec create: POST /containers/(id or name)/exec
	 * <li> Exec start with payload: { "Detach": false, "Tty": true}
	 * </ul>
	 * <b>Note: this method does not hijack the inner socket.</b> It returns immediately after the exec-start.
	 * @param nodeName The name of the node to use.
	 * @param containerId The Id of the container running in the node.
	 * @param command The command to execute.
	 * @return The stdout of the command
	 * @throws Exception on I/O errors.
	 * @throws RestException On REST call errors: {"message":"Requested CPUs are not available - requested 0,1, available: 0"}
	 */
	public static String execShellCommand (final String nodeName, final String containerId, final String command) throws Exception, RestException {
		DockerNode node 	= getNode(nodeName);
		final String url 	= node.buildBaseUrl();
		
		// 1. exec create (Id, Cmd)
		final String execid 	= execCreate(nodeName, containerId, command);
		
		// 2. exec start: http://192.168.42.248:2375/exec/{ID}/start
		final String startUrl 	= url + "exec/" + execid + "/start";
		final String payload 	= "{ \"Detach\": false, \"Tty\": true}";
		
		TRACE("Container Exec start url " + startUrl + " Payload " + payload);
		WebClient http 				= new WebClient(startUrl);
		
		if (startUrl.startsWith("https")) {
			http.setSSLParams("TLS", node.keyStorePath, node.keyStorePassword);
		}
		final String stdout 		= http.doPost(payload, CoreTypes.CONTENT_TYPE_JSON, null);
		return stdout;
	}
	
	public static DockerNode getNode(final String nodeName) throws IOException {
		if ( nodeName == null) {
			throw new IOException("A node name is required.");
		}
		DockerNode node 	= clusters.get(nodeName);
		if ( node == null) {
			throw new IOException("Can't find node " + nodeName);
		}
		return node;
	}
	
	/**
	 * Create a Docker shell-exec command to be used before hijacking the socket (for interactive shells). It creates:
	 * <ul>
	 * <li> Exec create: POST /containers/(id or name)/exec
	 * <li> Parses the response {"Id":"ff905b3513063e9a6eaf037cd994e151178f450b0ebc57b96527e9d6b16be326"} and return the exec-Id.
	 * </ul>
	 * <b>Note:</b> Use this method before hijacking the socket for an interactive /bin/bash or sh terminal.
	 * 
	 * @param nodeName The name of the node to use.
	 * @param containerId The Id of the container running in the node.
	 * @param command The command to execute: /bin/bash or sh for a terminal.
	 * 
	 * @return The Id within {"Id":"ff905b3513063e9a6eaf037cd994e151178f450b0ebc57b96527e9d6b16be326"}
	 * 
	 * @throws Exception on I/O errors.
	 * @throws RestException On REST call errors: {"message":"Requested CPUs are not available - requested 0,1, available: 0"}
	 */
	public static String execCreate (final String nodeName, final String containerId, final String command) throws Exception, RestException {
		DockerNode node 			= getNode(nodeName);
		final String url 			= node.buildBaseUrl();
		
		// exec create (Id, Cmd)
		String raw 					= IOTools.readFromStream(Docker.class.getResourceAsStream("/resources/docker/container_exec.json"));
		Map<String, String> request = new HashMap<String, String>();
		request.put("Id", containerId);
		request.put("Cmd", command);
		
		Map<String, Object> args  	= DockerParams.extractParamsFromMap(request);
		raw 						= DockerParams.replace(args, raw);
		HTTPDestination dest 		= HTTPDestination.create(url, raw, CoreTypes.CONTENT_TYPE_JSON,  node.keyStorePath, node.keyStorePassword);
		
		Object resp 				= node.rest.invoke(dest, "ContainerExecCreate", args);
		
		// Container down: Conflict (409): {"message":"Container 645399b14cd8e381c283954589bb82c37fc7540245b12c7b84c1da9a24ff3cea is not running"}
		// OK HTTP 201 192.168.99.100:2376 {"Id":"ff905b3513063e9a6eaf037cd994e151178f450b0ebc57b96527e9d6b16be326"}
		TRACE("ExecCreate Server Replied: " + resp.toString()  + " for RAW " + raw);
		
		JSONObject root 		= (JSONObject) resp;
		final String execid 	= root.getString("Id");
		return execid;
	}
	
	/**
	 * Shortcut to invoke the CreateImage REST API call.
	 * @param nodeName name of the node to use.
	 * @param image Image name (e.g busybox).
	 * @param tag Image late ( e.g latest).
	 * @param authObj Optional authorization JSON { 'username' : "NAME", 'password' : "PASSWORD"} for images that require it.
	 * @return The server response: {status: docker-code, message: SERVER_MESSAGE}
	 * @throws RestException If an error occurs in the server.
	 * @throws Exception On bugs and other errors.
	 */
	public static JSONObject imageCreate (final String nodeName, final String image, final String tag, final String authObj) throws RestException, Exception {
		if ( image == null) {
			throw new RestException("Image is required.");
		}
		if ( tag == null) {
			throw new RestException("Tag is required.");
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("NAME", image);
		params.put("TAG", tag );
		
		//5/18/2019 This must be BASE64 encoded!
		params.put("AUTHOBJ", authObj != null ? Base64.encode(authObj.getBytes(CoreTypes.CHARSET_UTF8)) : "");
		return Docker.invokeApi("CreateImage", nodeName, null, null, params);
	}

	/**
	 * Inspect an image.
	 * @param nodeName The target node name.
	 * @param image Image name or ID.
	 * @return <pre>{"data":{"GraphDriver":{"Name":"aufs","Data":null},"Parent":"","Config":{"ArgsEscaped":true,"User":"","OnBuild":null,"Tty":false,"StdinOnce":false,"Labels":{"maintainer":"NGINX Docker Maintainers <docker-maint@nginx.com>"},"ExposedPorts":{"80/tcp":{}},"Cmd":["nginx","-g","daemon off;"],"WorkingDir":"","Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","NGINX_VERSION=1.15.10-1~stretch","NJS_VERSION=1.15.10.0.3.0-1~stretch"],"StopSignal":"SIGTERM","Entrypoint":null,"AttachStdout":false,"Domainname":"","AttachStderr":false,"Image":"sha256:eb70ea14d4ac658e54090a984eaf06ed1bc41efed0f688020d7b88d26ba38920","AttachStdin":false,"Hostname":"","Volumes":null,"OpenStdin":false},"Comment":"","Author":"","Architecture":"amd64","Os":"linux","Created":"2019-03-26T23:13:42.01289097Z","RootFS":{"Layers":["sha256:5dacd731af1b0386ead06c8b1feff9f65d9e0bdfec032d2cd0bc03690698feda","sha256:dd0338cdfab32cdddd6c30efe8c89d0229d9f939e2bb736fbb0a52f27c2b0ee9","sha256:7e274c0effe81c48f9337879b058c729c33bd0199e28e2c55093d79398f5e8c0"],"Type":"layers"},"RepoDigests":["nginx@sha256:c8a861b8a1eeef6d48955a6c6d5dff8e2580f13ff4d0f549e082e7c82a8617a2"],"DockerVersion":"18.06.1-ce","ContainerConfig":{"ArgsEscaped":true,"User":"","OnBuild":null,"Tty":false,"StdinOnce":false,"Labels":{"maintainer":"NGINX Docker Maintainers <docker-maint@nginx.com>"},"ExposedPorts":{"80/tcp":{}},"Cmd":["/bin/sh","-c","#(nop) ","CMD [\"nginx\" \"-g\" \"daemon off;\"]"],"WorkingDir":"","Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","NGINX_VERSION=1.15.10-1~stretch","NJS_VERSION=1.15.10.0.3.0-1~stretch"],"StopSignal":"SIGTERM","Entrypoint":null,"AttachStdout":false,"Domainname":"","AttachStderr":false,"Image":"sha256:eb70ea14d4ac658e54090a984eaf06ed1bc41efed0f688020d7b88d26ba38920","AttachStdin":false,"Hostname":"6c02a05b3d09","Volumes":null,"OpenStdin":false},"Id":"sha256:2bcb04bdb83f7c5dc30f0edaca1609a716bda1c7d2244d4f5fbbdfef33da366c","Metadata":{"LastTagTime":"0001-01-01T00:00:00Z"},"VirtualSize":109294563,"RepoTags":["nginx:latest"],"Container":"6c02a05b3d095c6e0f51aa3d6ff84c3cac8c76b8464ee4930c151b5afffce9ad","Size":109294563}}</pre>
	 * @throws RestException On docker server errors.
	 * @throws Exception On I/O, timeouts and other errors.
	 */
	public static JSONObject imageInspect (final String nodeName, final String image) throws RestException, Exception {
		if ( image == null) {
			throw new RestException("Image is required.");
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("NAME", image);
		return Docker.invokeApi("InspectImage", nodeName, null, null, params);
	}

	/**
	 * Initialize a swarm in the manager node.
	 * <h2>Ports 2377 and 2376</h2>
	 * <ul>
	 * <li>Always run docker swarm init and docker swarm join with port 2377 (the swarm management port), or no port at all and let it take the default.
	 * <li>The machine IP addresses returned by docker-machine ls include port 2376, which is the Docker daemon port. Do not use this port or you may experience errors.
	 * </ul>
	 * @param nodeName Swarm manager.
	 * @param advertiseAddress Manager IP:PORT <b>Always use 2377 or leave empty</b>.
	 * @return A swarm id (e.g c3hjgrihiknh3o0p9vizl1yox).
	 * @throws Exception on bugs.
	 * @throws RestException On docker errors: {"message":"could not choose an IP address to advertise since this system has multiple addresses on different interfaces (10.0.2.15 on eth0 and 192.168.99.101 on eth1)"}
	 */
	public static String swarmInit (final String nodeName, final String advertiseAddress) throws JSONException, IOException, RestException, Exception {
		DockerNode node 			= getNode(nodeName);
		final String url 			= node.buildBaseUrl();
		
		Map<String, Object> params 	= new HashMap<String, Object>();
		params.put("ADVERTISEADDR", advertiseAddress);
		
		String raw 					= IOTools.readFromStream(Docker.class.getResourceAsStream("/resources/docker/swarm_init.json"));
		String json 				= DockerParams.replace(params, raw);

		// validate
		JSONObject root 			= new JSONObject(json);
		HTTPDestination dest 		= HTTPDestination.create(url, root.toString(), CoreTypes.CONTENT_TYPE_JSON,  node.keyStorePath, node.keyStorePassword);
	
		TRACE("SwarmInit Node: " + nodeName  + " payload " + root.toString(1));
				
		Object resp 				= node.rest.invoke(dest, "SwarmInit", params);
		// There is a \n @ the end plus quotes
		final String swarmId 		= ((String)resp).replaceAll("[\"\n]", "").trim();
		
		// ERROR  HTTP 400: {"message":"could not choose an IP address to advertise since this system has multiple addresses on different interfaces (10.0.2.15 on eth0 and 192.168.99.101 on eth1)"}
		// OK (200) SWARM-ID "c3hjgrihiknh3o0p9vizl1yox"
		TRACE("SwarmInit Server Replied: " + resp.toString()  + " SwarmId; " + swarmId);
		return swarmId;
	}

	/**
	 * Retrieve swarm info: <pre>{"UpdatedAt":"2019-03-25T15:53:20.555269823Z","JoinTokens":{"Worker":"SWMTKN-1-","Manager":"SWMTKN-2-"},"TLSInfo":{"CertIssuerSubject":"MBMxETAPBgNVBAMTCHN3YXJtLWNh","CertIssuerPublicKey":"...","TrustRoot":"..."},"SubnetSize":24,"DefaultAddrPool":["10.0.0.0/8"],"Spec":{"Dispatcher":{"HeartbeatPeriod":5000000000},"Name":"default","Orchestration":{"TaskHistoryRetentionLimit":5},"Labels":{},"EncryptionConfig":{"AutoLockManagers":false},"CAConfig":{"NodeCertExpiry":7776000000000000},"TaskDefaults":{},"Raft":{"KeepOldSnapshots":0,"HeartbeatTick":1,"ElectionTick":10,"LogEntriesForSlowFollowers":500,"SnapshotInterval":10000}},"ID":"jnirz6k2qc6qqbk9ofxc86wxi","RootRotationInProgress":false,"Version":{"Index":10},"CreatedAt":"2019-03-25T15:53:19.880849214Z"}</pre>
	 * @param nodeName Node name.
	 * @return <b>Server response JSON in data-tables format: {data: "SERVER-JSON"}</b>. 
	 * @throws Exception
	 * @throws RestException on Docker daemon errors.
	 */
	public static JSONObject swarmInspect (final String nodeName) throws Exception, RestException {
		return invokeApi("SwarmInspect", nodeName);
	}
	
	/**
	 * Join a swarm described by <pre>{  "ListenAddr": "0.0.0.0:2377",  "AdvertiseAddr": "${ADVERTISEADDR}",  "RemoteAddrs": ${REMOTEADDRS},  "JoinToken": "${JOINTOKEN}"}</pre>
	 * If the call succeeds an empty response is received else an {@link RestException} is thrown with the server message.
	 * @param nodeName Node name.
	 * @param advertiseAddress Node advertise address.
	 * @param joinToken Join token: manager or worker.
	 * @param remoteAddrs Comma separated list of manager IP:PORTS. (e.g dode1:port1,node2:port2,...)
	 * @throws Exception On bugs.
	 * @throws RestException on Docker daemon errors with the server response code and message.
	 */
	public static void swarmJoin (final String nodeName, final String advertiseAddress, final String joinToken, final String remoteAddrs ) throws Exception, RestException {
		DockerNode node 			= getNode(nodeName);
		final String url 			= node.buildBaseUrl();
		String raw 					= IOTools.readFromStream(Docker.class.getResourceAsStream("/resources/docker/swarm_join.json"));
		
		Map<String, String> request = new HashMap<String, String>();
		request.put("AdvertiseAddr", advertiseAddress);
		request.put("JoinToken", joinToken);
		request.put("RemoteAddrs", remoteAddrs);
		
		Map<String, Object> params  = DockerParams.extractParamsFromMap(request);
		String json 				= DockerParams.replace(params, raw);

		// validate
		JSONObject root 			= new JSONObject(json);
		HTTPDestination dest 		= HTTPDestination.create(url, root.toString(), CoreTypes.CONTENT_TYPE_JSON,  node.keyStorePath, node.keyStorePassword);
	
		TRACE("SwarmJoin Node: " + nodeName  + " payload " + root.toString(1));

		// OK (200) Empty response Content-Length = 0
		// (Throws Ex) Service Unavailable (503): {"message":"This node is already part of a swarm. Use \"docker swarm leave\" to leave this swarm and join another one."}
		node.rest.invoke(dest, "SwarmJoin", params);
	}
	
	/**
	 * Leave a swarm.
	 * @param nodeName Target node name.
	 * @param force Request parameter: true/false,0/1.
	 * @throws Exception On bugs.
	 * @throws RestException On docker errors: {"message":"This node is not part of a swarm"}, {"message":"You are attempting to leave the swarm on a node that is participating as a manager. Removing the last manager erases all current state of the swarm. Use `--force` to ignore this message. "}
	 */
	public static void swarmLeave (final String nodeName, boolean force) throws Exception, RestException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("FORCE", force);
		
		// EX- Service Unavailable (503): {"message":"This node is not part of a swarm"}
		// EX Service Unavailable (503): {"message":"You are attempting to leave the swarm on a node that is participating as a manager. Removing the last manager erases all current state of the swarm. Use `--force` to ignore this message. "}
		// OK - EMPTY
		invokeApi("SwarmLeave", nodeName, null, null, params);
	}
	
	/**
	 * Add a swarm.
	 * @param swarmId Swarm Id.
	 * @param master Master node. The node that initialized the swarm.
	 * @param joinTokens {"Worker": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-3j0hurzmutqgwhj4u5podwz1m", "Manager": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-c1h56fasl7o1v3arqq7smxau0"	}
	 * @param members String array of members.
	 * @return <pre>{ "SwarmId": "ID", "members": [node1,...], "JoinTokens": {"Worker": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-3j0hurzmutqgwhj4u5podwz1m", "Manager": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-c1h56fasl7o1v3arqq7smxau0"	} </pre>
	 * @throws JSONException On JSON parse errors.
	 */
	public static JSONObject addSwarm (final String swarmId, String master, JSONObject joinTokens, String[] members) throws JSONException {
		return DockerSwarm.addSwarm(swarmId, master, joinTokens, members);
	}

	public static void saveSwarms() throws IOException, JSONException {
		if ( basePath == null ) {
			throw new IOException("Descriptor storage path cannot be null.");
		}
		if (! FileTool.fileExists(basePath)) {
			throw new IOException("Path " + basePath + " doesn't exist.");
		}
		final String path = basePath + File.separator + "swarms.json";
		
		// Don't save nodes info.
		JSONArray swarms = DockerSwarm.swarmsToJSON(false);
		
		IOTools.saveText(path, swarms.toString(1));
		TRACE("Saved swarms to " + path);
	}
	
	public static Map<String, JSONObject> getSwarms () {
		return Collections.unmodifiableMap(DockerSwarm.swarms);
	}

	public static void loadSwarms() throws JSONException, IOException {
		final String path 	= basePath + File.separator + "swarms.json";
		
		if (! FileTool.fileExists(path)) {
			return;
		}
		
		JSONArray array 	= new JSONArray(IOTools.readFileFromFileSystem(path));
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject jobj = array.getJSONObject(i);
			DockerSwarm.swarms.put(jobj.getString("SwarmId"), jobj);
		}
		TRACE("Loaded swarms: " + DockerSwarm.swarms);
	}

	/**
	 * Get a swarm descriptor.
	 * @param id Swarm ID.
	 * @return <pre>{ "SwarmId": "k3p1nxnmn7vuzob2ivsp7t07i", 
	 *  "JoinTokens": {  "Worker": "SWMTKN-1-29brtf4axnl4g510cm91a21dmtbvi5ldjt2yr34z9tkz4fsk08-2ryuq0k64lh5ivfjkmy9796q5",
	 *           "Manager": "SWMTKN-1-29brtf4axnl4g510cm91a21dmtbvi5ldjt2yr34z9tkz4fsk08-0pt22e72e9dwfpgni4qrrv71l" 
	 *   },
	 *  "Members": [], "Master": "Node3"}</pre>
	 */
	public static DockerSwarm.Swarm getSwarm(final String id) {
		return DockerSwarm.getSwarm(id);
	}
	
	/**
	 * Add a member to the swarm and save the descriptor to disk.
	 * @param swarmId
	 * @param member
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void swarmAddMember (final String swarmId, final String member) throws JSONException, IOException {
		DockerSwarm.addMember(swarmId, member);
		saveSwarms();
	}
	
	/**
	 * Create a container given a node name and a map of execution parameters.
	 * @param nodeName Node name.
	 * @param params {@link Map} of execution params. For example: {CMD=["nginx","-g","daemon off;"], ARGS=[], PORTS=[], PORTBINDINGS={"80/tcp":[{"HostPort":"80"}]}, VOLUMES={}, ENV=[], IMAGE=nginx, MOUNTS=, EXPOSEDPORTS={}, LABELS={}, NETWORKS=, BINDS=[]}
	 * @return Success: {"data":{"Id":"a5cd86e220292e7d2d2a1d2eefcc75ed33f43b3f0a1c10d609a237b9540529d3","Warnings":["linux does not support CPU percent. Percent discarded."]}}
	 * @throws RestException On docker server errors.
	 * @throws Exception On I/O, timeouts and other errors.
	 */
	public static JSONObject containerCreate(final String nodeName, Map<String, Object> params) throws RestException, Exception {
		// get raw json 
		final String descriptor = "/resources/docker/container_create.json";
		final String apiName	= "CreateContainer";
		return invokeApi(apiName, nodeName, descriptor, params);
	}

	
	/**
	 * Start a container with a given ID.
	 * @param node The target node name.
	 * @param id Container ID.
	 * @return On success an empty data tables response: {"data":""}
	 * @throws RestException On docker server errors.
	 * @throws Exception On I/O, timeouts and other errors.
	 */
	public static JSONObject containerStart(final String node, final String id) throws RestException, Exception {
		return containerAction("StartContainer", node, id);
	}

	/**
	 * Execute a REST API endpoint for a container.
	 * @param apiName Container: REST API name: StartContainer, StopContainer, InspectContainer
	 * @param node Target node name.
	 * @param id Container ID.
	 * @return On success an empty data tables response: {"data":""}
	 * @throws RestException On docker server errors.
	 * @throws Exception On I/O, timeouts and other errors.
	 */
	static JSONObject containerAction(final String apiName, final String node, final String id) throws RestException, Exception {
		if ( id == null) {
			throw new IOException("Container Id is required.");
		}
		Map<String, Object> params 	= new HashMap<String, Object>();
		params.put("ID", id);
		
		// Invoke
		JSONObject resp = Docker.invokeApi(apiName, node, null, null, params);
		log.debug(apiName + ": Server replied:" + resp.toString());
		return resp;
	}
	
}
