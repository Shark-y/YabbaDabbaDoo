package com.cloud.docker;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestException;

/**
 * This class is used to translate HTTP request parameters into JSON so they can be passed to the {@link RestClient} for execution
 * 
 * @author VSilva
 *
 */
public class DockerParams {

	/* HTTP Request parameters: This match Docker's JSON keys */
	public static final String W3_PARAM_CONTAINER_NAME 	= "ContainerName";
	public static final String W3_PARAM_IMAGE 			= "Image";
	public static final String W3_PARAM_RESTART_POL 	= "RestartPolicy";
	public static final String W3_PARAM_ENV 			= "Env";
	public static final String W3_PARAM_CMD 			= "Cmd";
	public static final String W3_PARAM_EXPOSEDPORTS 	= "ExposedPorts";
	public static final String W3_PARAM_PORTBINDINGS 	= "PortBindings";
	public static final String W3_PARAM_BINDS 			= "Binds";
	public static final String W3_PARAM_VOLUMES 		= "Volumes";
	public static final String W3_PARAM_LABELS 			= "Labels";

	// Image params
	public static final String W3_PARAM_TAG 			= "Tag";
	public static final String W3_PARAM_USERNAME 		= "username";
	public static final String W3_PARAM_PASSWORD 		= "password";

	// Container logs
	public static final String W3_PARAM_ID 				= "Id";
	public static final String W3_PARAM_LOGARGS 		= "logargs";

	// Swarm
	public static final String W3_PARAM_ADVERTISEADDR 	= "AdvertiseAddr";
	public static final String W3_PARAM_JOINTOKEN 		= "JoinToken";
	public static final String W3_PARAM_REMOTEADDRS		= "RemoteAddrs";
	
	// Services
	public static final String W3_PARAM_NAME 			= "Name";
	public static final String W3_PARAM_REPLICAS 		= "Replicas";
	public static final String W3_PARAM_COMMAND 		= "Command";
	public static final String W3_PARAM_ARGS 			= "Args";
	public static final String W3_PARAM_NETWORKS 		= "Networks";
	public static final String W3_PARAM_MOUNTS 			= "Mounts";
	public static final String W3_PARAM_PORTS 			= "Ports";
	
	/* These are simple work variables to reduce code duplication. Shouldn't take any mem at all */
	private static String[] tmp 	= null;
	private static JSONArray array 	= null;
	private static JSONObject jobj 	= null;

	/**
	 * Replace all occurrences of ${VARIABLE} with {@link Map} (VALUE) within a RAW string.
	 * This method is used as a template mechanism to replace input parameters in a RAW JSON string.
	 * @param params {@link Map} of (VARIABLE, VALUE) pairs.
	 * @param raw A raw string (JSON) containing replacement variables such as { 'key' : ${VARIABLE} }
	 * @return Given Map (VARAIABLE, VALUE) and RAW { 'key' : ${VARIABLE} } returns { 'key' : VALUE }
	 */
	public static String replace (Map<String, Object> params , String raw) {
		String str = raw;
		for ( Map.Entry<String, Object> entry: params.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			if ( val == null ) {
				continue;
			}
			str = str.replaceAll("\\$\\{" + key  +"\\}", val.toString());
		}
		return str;
	}

	/**
	 * Convert a {@link Map} of (name, value) docker HTTP input parameters into a {@link Map} of JSON (name, JSON-value) pairs that can be fed to the {@link RestClient}
	 * @param request input {@link Map}. For example: (Image, foo) => (Image, 'Foo'), (Env, 'FOO=bar,VAR1=VAL1') => (Env, '["FOO=bar","VAR1=VAL1"]')
	 * @return A {@link Map} of JSON (name, JSON-value) ready for submission.
	 * @throws JSONException On JSON errors.
	 * @throws RestException If there is something wrong with the parameters.
	 */
	public static Map<String, Object>  extractParamsFromRequest (HttpServletRequest request) throws JSONException, RestException {
		final String image 			= request.getParameter(W3_PARAM_IMAGE);
		// VAR1=VAl1,VAR2=VAL2,...
		final String env 			= request.getParameter(W3_PARAM_ENV);
		final String cmd 			= request.getParameter(W3_PARAM_CMD);
		// nn/tcp,nn/udp, ...
		final String exposedports 	= request.getParameter(W3_PARAM_EXPOSEDPORTS);
		// 8080:8080,80/tcp:80
		final String portbindings 	= request.getParameter(W3_PARAM_PORTBINDINGS);
		// /tmp:/tmp
		final String binds 			= request.getParameter(W3_PARAM_BINDS);
		// vol1,vol2
		final String volumes 		= request.getParameter(W3_PARAM_VOLUMES);
		// key:val,key1:val1
		final String labels 		= request.getParameter(W3_PARAM_LABELS);
		
		// container logs args stdout=1&stderr=1&tail=10 (URL ENCODED =(%3D) &(%26) https://www.w3schools.com/tags/ref_urlencode.asp
		final String logargs 		= request.getParameter(W3_PARAM_LOGARGS);
		// container id
		final String id 			= request.getParameter(W3_PARAM_ID);

		// 4/15/2019 container rstrt policy
		final String restartPol 	= request.getParameter(W3_PARAM_RESTART_POL);
		final String containerName 	= request.getParameter(W3_PARAM_CONTAINER_NAME);

		Map<String, String> params 	= new HashMap<String, String>();
		params.put(W3_PARAM_IMAGE, 	image);
		params.put(W3_PARAM_ENV, 	env);
		params.put(W3_PARAM_CMD, 	cmd);
		params.put(W3_PARAM_EXPOSEDPORTS, exposedports);
		params.put(W3_PARAM_PORTBINDINGS, portbindings);
		params.put(W3_PARAM_BINDS, binds);
		params.put(W3_PARAM_VOLUMES, volumes);
		params.put(W3_PARAM_LABELS, labels);
		params.put(W3_PARAM_LOGARGS, logargs);
		params.put(W3_PARAM_ID, id);
		params.put(W3_PARAM_RESTART_POL, restartPol);
		params.put(W3_PARAM_CONTAINER_NAME, containerName);
		
		return extractParamsFromMap(params);
	}
	
	/*
	 * Convert a command string to a JSON array: date -h => ["date", "-h"]
	 */
	private static JSONArray parseCommand (String cmd) {
		JSONArray array 	= new JSONArray();
		if ( cmd == null || cmd.isEmpty()) {
			return array;
		}
		String[] tmp 		= null;
		
		tmp					= cmd.split(" ");
		StringBuffer buf 	= new StringBuffer();
		
		for (int i = 0; i < tmp.length; i++) {
			boolean quote 	= false;
			String tok 		= tmp[i];
			
			if ( tok.matches("^[\"'].*") /*tok.startsWith("'") */ ) {
				quote = true; 
			}
			if ( tok.matches(".*['\"]$") /*tok.endsWith("'") */ ) {
				buf.append(tok.substring(0, tok.length() - 1)); // chop '
				quote 	= false;
				tok 	= buf.toString();
				buf 	= new StringBuffer();
			}
			if ( quote ) {
				// chop '
				buf.append(tok.substring(1) + " ");
			}
			else {
				array.put(tok);
			}
		}
		return array;
	}
	
	/**
	 * Convert a {@link Map} of (name, value) docker HTTP input parameters into a {@link Map} of JSON (name, JSON-value) pairs that can be fed to the {@link RestClient}
	 * @param request input {@link Map}. For example: {Cmd="nginx -g 'daemon off;', Args=, Ports=, PortBindings="80/tcp:80", Volumes=, Env=, Image=nginx, Mounts=, ExposedPorts=, Labels=, Networks=, Binds=}
	 * @return A {@link Map} of JSON (name, JSON-value) ready for submission. For example: {CMD=["nginx","-g","daemon off;"], ARGS=[], PORTS=[], PORTBINDINGS={"80/tcp":[{"HostPort":"80"}]}, VOLUMES={}, ENV=[], IMAGE=nginx, MOUNTS=, EXPOSEDPORTS={}, LABELS={}, NETWORKS=, BINDS=[]}
	 * @throws JSONException On JSON errors.
	 * @throws RestException If there is something wrong with the parameters.
	 */
	public static Map<String, Object>  extractParamsFromMap (Map<String, String> request) throws JSONException, RestException {
		final String image 			= request.get(W3_PARAM_IMAGE);
		// VAR1=VAl1,VAR2=VAL2,...
		final String env 			= request.get(W3_PARAM_ENV);
		final String cmd 			= request.get(W3_PARAM_CMD);
		// nn/tcp,nn/udp, ...
		final String exposedports 	= request.get(W3_PARAM_EXPOSEDPORTS);
		// 8080:8080,80/tcp:80
		final String portbindings 	= request.get(W3_PARAM_PORTBINDINGS);
		// /tmp:/tmp
		final String binds 			= request.get(W3_PARAM_BINDS);
		// vol1,vol2
		final String volumes 		= request.get(W3_PARAM_VOLUMES);
		// key:val,key1:val1
		final String labels 		= request.get(W3_PARAM_LABELS);
		// container logs: stdout=1&stderr=1&tail=10
		final String logargs 		= request.get(W3_PARAM_LOGARGS);
		// container id
		final String id 			= request.get(W3_PARAM_ID);
		
		// 4/15/2019 container restart policy
		final String restartPol 	= request.get(W3_PARAM_RESTART_POL);
		final String containerName 	= request.get(W3_PARAM_CONTAINER_NAME);
		
		// Swarm:Advertise addr
		final String advertiseAddr	= request.get(W3_PARAM_ADVERTISEADDR);
		// Swarm: Joint token
		final String jointToken		= request.get(W3_PARAM_JOINTOKEN);
		// Swarm: remoteAddrs
		final String remoteAddrs	= request.get(W3_PARAM_REMOTEADDRS);
		// Swarm: Services
		final String name			= request.get(W3_PARAM_NAME);
		final String replicas		= request.get(W3_PARAM_REPLICAS);
		final String command		= request.get(W3_PARAM_COMMAND);
		final String args			= request.get(W3_PARAM_ARGS);
		final String networks		= request.get(W3_PARAM_NETWORKS);
		final String mounts			= request.get(W3_PARAM_MOUNTS);
		final String ports			= request.get(W3_PARAM_PORTS);
		
		Map<String, Object> params 	= new HashMap<String, Object>();
		/*String[] tmp				= null;
		JSONArray array				= null;
		JSONObject jobj				= null;*/

		// IMAGE name
		if ( image != null ) {
			params.put(W3_PARAM_IMAGE.toUpperCase(), image);
		}
		// 4/15/2019 Restart policy always required.
		params.put(W3_PARAM_RESTART_POL.toUpperCase(), restartPol != null ? restartPol : "");
		params.put(W3_PARAM_CONTAINER_NAME.toUpperCase(), containerName != null && !containerName.isEmpty() ? "name=" + containerName : "");
		
		// ENV: FOO=bar,VAR1=VAL1 => ["FOO=bar","VAR1=VAL1"]
		if ( env != null) {
			array 	= new JSONArray();
			if ( !env.isEmpty()) {
				tmp 	= env.split(",");
				for (int j = 0; j < tmp.length; j++) {
					array.put(tmp[j]);
				}
			}
			// ["FOO=bar","VAR1=VAL1"]
			params.put(W3_PARAM_ENV.toUpperCase(), array.toString());
		}
		
		// COMMAND: date -h => ["date", "-h"]
		if ( cmd != null ) {
			params.put(W3_PARAM_CMD.toUpperCase(), parseCommand(cmd).toString());
		}
		
		// EXPOSEDPORTS: nn/tcp,nn/udp,... => { "22/tcp": {} }
		if ( exposedports != null ) {
			jobj = new JSONObject();
			
			if ( !exposedports.isEmpty()) {
				tmp	= exposedports.split(",");
				for (int i = 0; i < tmp.length; i++) {
					jobj.put(tmp[i], new JSONObject());
				}
			}
			params.put(W3_PARAM_EXPOSEDPORTS.toUpperCase(), jobj.toString());
		}
		
		// PORTBINDINGS: 8080:8080,80/tcp:80 => { "22/tcp": [{ "HostPort": "11022" }] }
		if ( portbindings != null ) {
			jobj 			= new JSONObject();
			
			if ( !portbindings.isEmpty()) {
				tmp				= portbindings.split(",");
				String[] tmp1	= null;
				
				for (int i = 0; i < tmp.length; i++) {
					tmp1 = tmp[i].split(":");

					if ( tmp1.length == 2) {
						array 				= new JSONArray();
						JSONObject hostPort = new JSONObject();
						
						hostPort.put("HostPort", tmp1[1]);
						array.put(hostPort);
						
						//jobj.put(tmp1[0], String.format("[{ \"HostPort\": \"%s\" }]", tmp1[1]) );
						jobj.put(tmp1[0], array );
					}
					else {
						throw new RestException("Invalid portbindings format: " + portbindings, 400, "Bad Request");
					}
				}
			}
			params.put(W3_PARAM_PORTBINDINGS.toUpperCase(), jobj.toString());
		}

		// BINDS: /tmp:/tmp,/foo:/foo =>["/tmp:/tmp","/foo:/foo"]
		if ( binds != null) {
			array 	= new JSONArray();

			if ( !binds.isEmpty()) {
				tmp 	= binds.split(",");
				for (int j = 0; j < tmp.length; j++) {
					array.put(tmp[j]);
				}
			}
			params.put(W3_PARAM_BINDS.toUpperCase(), array.toString());
		}
		
		// VOLUMES: vol1,vol2 => {"vol1": {}, "vol2": {}}
		if ( volumes != null) {
			jobj 	= new JSONObject();

			if ( !volumes.isEmpty()) {
				tmp		= volumes.split(",");
				for (int i = 0; i < tmp.length; i++) {
					jobj.put(tmp[i], new JSONObject());
				}
			}
			// {"/volumes/data": {}}
			params.put(W3_PARAM_VOLUMES.toUpperCase(), jobj.toString());
		}

		// LABELS: key:val,key1:val1 => {"com.example.version": "1.0", "com.example.vendor": "ACME" }
		if ( labels != null) {
			jobj			= new JSONObject();
			if ( !labels.isEmpty() ) {
				tmp				= labels.split(",");
				String[] tmp1	= null;
				
				for (int i = 0; i < tmp.length; i++) {
					tmp1 = tmp[i].split(":");
					if ( tmp1.length == 2) {
						jobj.put(tmp1[0], tmp1[1]);
					}
					else {
						throw new RestException("Invalid label format: " + labels, 400, "Bad Request");
					}
				}
			}
			// {"com.example.version": "1.0", "com.example.vendor": "ACME" }
			params.put(W3_PARAM_LABELS.toUpperCase(), jobj.toString());
		}
		// CONTAINER LOG ARGS: stdout=1&stderr=1&tail=10
		if ( logargs != null) {
			params.put(W3_PARAM_LOGARGS.toUpperCase(), logargs);
		}
		// CONTAINER ID
		if ( id != null) {
			params.put(W3_PARAM_ID.toUpperCase(), id);
		}
		// SWARM
		if ( advertiseAddr != null) {
			params.put(W3_PARAM_ADVERTISEADDR.toUpperCase(), advertiseAddr);
		}
		// SWARM Join token
		if ( jointToken != null) {
			params.put(W3_PARAM_JOINTOKEN.toUpperCase(), jointToken);
		}
		// SWARM remote addrs => ["addr1",...]
		if ( remoteAddrs != null) {
			array 	= new JSONArray();
			tmp 	= remoteAddrs.split(",");
			for (int i = 0; i < tmp.length; i++) {
				array.put(tmp[i]);
			}
			params.put(W3_PARAM_REMOTEADDRS.toUpperCase(), array.toString());
		}
		// SERVICES
		if ( name != null) {
			params.put(W3_PARAM_NAME.toUpperCase(), name);
		}
		if ( replicas != null) {
			params.put(W3_PARAM_REPLICAS.toUpperCase(), replicas);
		}
		if ( command != null) {
			params.put(W3_PARAM_COMMAND.toUpperCase(), parseCommand(command).toString());
		}
		if ( args != null) {
			params.put(W3_PARAM_ARGS.toUpperCase(), parseCommand(args).toString());
		}
		else {
			params.put(W3_PARAM_ARGS.toUpperCase(), new JSONArray());
		}
		// SERVICE 
		extractServiceNetworks(networks, params);
		extractServiceMounts(mounts, params);
		extractServiceEndpointSpecPorts(ports, params);
		
		cleanUp();
		return params;
	}

	// Clean work vars. Invoke before return;
	private static void cleanUp () {
		tmp		= null;
		array	= null;
		jobj	= null;
	}
	
	/* TODO: Mounts => "Mounts": [ { "ReadOnly": true, "Source": "web-data", "Target": "/usr/share/nginx/html", "Type": "volume"
	 * , "VolumeOptions": { "DriverConfig": { }, "Labels": { "com.example.something": "something-value"} } },... ],
	 */
	private static void extractServiceMounts (String mounts, Map<String, Object> params) throws RestException, JSONException {
		if ( mounts != null) {
			
		}
		else {
			params.put(W3_PARAM_MOUNTS.toUpperCase(), "");
		}
	}
	
	/*
	 * SERVICE (Networks) net1,net2,... => "Networks": [ { "Target": "net1" },...],
	 */
	private static void extractServiceNetworks (String networks, Map<String, Object> params) throws RestException, JSONException {
		if ( networks != null ) {
			String nets 	= "";
			if (!networks.isEmpty()) {
				tmp 	= networks.split(",");
				array 	= new JSONArray();
				for (int i = 0; i < tmp.length; i++) {
					jobj = new JSONObject();
					jobj.put("taget", tmp[i]);
					array.put(jobj);
				}
				nets = String.format("\"Networks\": %s,", array.toString());
			}
			params.put(W3_PARAM_NETWORKS.toUpperCase(), nets);
		}
		else {
			params.put(W3_PARAM_NETWORKS.toUpperCase(), "");
		}
	}
	
	/*
	 * EndpointSpec.Ports: 8080/tcp:80,1024/udp:1024 => [ {"Protocol": "tcp","PublishedPort": 8080, "TargetPort": 80 } ]
	 */
	private static void extractServiceEndpointSpecPorts (String ports, Map<String, Object> params) throws RestException, JSONException {
		if ( ports != null ) {
			tmp 	= ports.split(",");
			array	= new JSONArray();
			for (int i = 0; i < tmp.length; i++) {
				// 8080/tcp:80
				String temp 	= tmp[i];
				String[] tmp1 	= temp.split(":");
				if (tmp1.length != 2 ) {
					throw new RestException("Invalid port " + temp, 400, "Bad request");
				}
				String source 	= tmp1[0]; // 8080/tcp
				String port2	= tmp1[1]; // 80
				tmp1			= source.split("/");
				if (tmp1.length != 2 ) {
					throw new RestException("Invalid published protocol " + source, 400, "Bad request");
				}
				String port1	= tmp1[0];
				String proto1	= tmp1[1];
				jobj			= new JSONObject();
				jobj.put("Protocol", 		proto1);
				jobj.put("PublishedPort", 	Integer.parseInt(port1));
				jobj.put("TargetPort", 		Integer.parseInt(port2));
				array.put(jobj);
			}
			params.put(W3_PARAM_PORTS.toUpperCase(), array.toString());
		}
		else {
			params.put(W3_PARAM_PORTS.toUpperCase(), "[]");
		}
	}
	
	/**
	 * Parse the docker daemon response {"message":"Requested CPUs are not available - requested 0,1, available: 0"}
	 * @param resp Something like: {"message":"Requested CPUs are not available - requested 0,1, available: 0"}
	 * @return The message part or the original response if there is an error.
	 */
	static String parseServerErrorReponse (final String resp) {
		JSONObject root = null;
		try {
			root = new JSONObject(resp);
			return root.has("message") ? root.getString("message") : resp;
		} catch (Exception e) {
			return resp;
		}
	}
}
