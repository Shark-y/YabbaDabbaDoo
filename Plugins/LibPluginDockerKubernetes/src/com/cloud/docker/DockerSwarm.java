package com.cloud.docker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.Base64;
import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestException;

public class DockerSwarm {
	/** map of swarms indexed by swarm id */
	static final Map<String, JSONObject> swarms 	= new ConcurrentHashMap<String, JSONObject>();


	/**
	 * <pre>{ "SwarmId": "k3p1nxnmn7vuzob2ivsp7t07i", 
	 *  "JoinTokens": {  "Worker": "SWMTKN-1-29brtf4axnl4g510cm91a21dmtbvi5ldjt2yr34z9tkz4fsk08-2ryuq0k64lh5ivfjkmy9796q5",
	 *           "Manager": "SWMTKN-1-29brtf4axnl4g510cm91a21dmtbvi5ldjt2yr34z9tkz4fsk08-0pt22e72e9dwfpgni4qrrv71l" 
	 *   },
	 *  "Members": [], "Master": "Node3"}</pre>
	 */
	static class Swarm {
		JSONObject root;
		public Swarm(JSONObject root) {
			this.root = root;
		}
		public String getId() throws JSONException {
			return root.getString("SwarmId");
		}
		public String getMaster() throws JSONException {
			return root.getString("Master");
		}
		public JSONObject getJoinTokens() throws JSONException {
			return root.getJSONObject("JoinTokens");
		}
		public JSONArray getMembers() throws JSONException {
			return root.getJSONArray("Members");
		}
		
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
		JSONObject swarm 	= new JSONObject();
		swarm.put("SwarmId", swarmId);
		swarm.put("Master", master);
		swarm.put("JoinTokens", joinTokens);
		
		JSONArray array 	= new JSONArray();
		for (int i = 0; i < members.length; i++) {
			array.put(members[i]);
		}
		swarm.put("Members", array);
		swarms.put(swarmId, swarm);
		return swarm;
	}

	/**
	 * Serialize swarms map.
	 * @param fetchNodeInfo If true get node information from the master using the ListNodes RET API call
	 * @return <pre>[{ "SwarmId": "4gqyosoukhqwyy5vsih0oe8yw",
 "JoinTokens": {
  "Worker": "SWMTKN-1-53rsff1j3m905t9wlvn5q61oraylieabhouiitsid5539kbusq-0ohfg44bos84e6m28tqp649py",
  "Manager": "SWMTKN-1-53rsff1j3m905t9wlvn5q61oraylieabhouiitsid5539kbusq-6rwqcmx3pxzix4npxuw8ql9ip"
 },
 "members": ["Node3"] },...] </pre>
	 * @throws JSONException 
	 */
	static JSONArray swarmsToJSON(boolean fetchNodeInfo) throws JSONException  {
		JSONArray array 		= new JSONArray();

		for ( Map.Entry<String, JSONObject> entry : swarms.entrySet()) {
			JSONObject swarm  	= entry.getValue();
			JSONArray  errors	= new JSONArray();
			JSONArray  warnings	= new JSONArray();
			
			// Fetch node info from the manager
			if ( fetchNodeInfo) {
				final String swarmId	= swarm.getString("SwarmId");
				String master			= null;
				
				// Find master here...
				try {
					String curMaster	= swarm.getString("Master");
					
					// FIXME: Master(managers) may switch at boot time. 
					master				= findManager(swarmId);
					
					// manager switched.
					if ( !curMaster.equals(master)) {
						swarm.put("Master", master);
						warnings.put("Manager " + curMaster + " has switched. Using " + master + " instead.");
					}
					JSONObject nodes 	= Docker.invokeApi("ListNodes", master , null, null, null);
					swarm.put("nodes", nodes.optJSONArray("data"));
				}
				catch (Exception e) {
					errors.put((master != null ? master + ": " : swarmId + ": ") + DockerParams.parseServerErrorReponse(e.getMessage()));
					swarm.put("nodes", new JSONArray());
				}
				if ( errors.length() > 0) {
					swarm.put("errors", errors);
				}
				if ( warnings.length() > 0) {
					swarm.put("warnings", warnings);
				}
			}
			else {
				// remove in case of previous fetch (don't keep node info, changes all the time)
				swarm.remove("nodes");
				swarm.remove("errors");
				swarm.remove("warnings");
			}
			array.put(swarm);
		}
		return array;
	}
	
	/**
	 * Try to find the swarm manager.Node roles switch on the fly.
	 * @return Node manager.
	 * @throws Exception
	 */
	public static String findManager (String swarmId) throws Exception {
		Swarm swarm  	= getSwarm(swarmId);
		String manager 	= swarm.getMaster();

		try {
			// only the manager can list nodes
			Docker.invokeApi("ListNodes", manager , null, null, null);
			return manager;
		} catch (RestException e) {
			// Master switched, look at workers
			JSONArray members =  swarm.getMembers();
			
			for (int i = 0; i < members.length(); i++) {
				String member = members.getString(i);
				try {
					Docker.invokeApi("ListNodes", member , null, null, null);
					return member;
				} catch (RestException e2) {
				}
			}
		}
		// couldn't find it
		return manager;
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
	public static Swarm getSwarm(final String id) {
		return new Swarm(swarms.get(id));
	}

	public static Swarm removeSwarm(final String id) {
		return new Swarm(swarms.remove(id));
	}

	public static void addMember (final String swarmId, final String member) throws JSONException, IOException {
		JSONObject root = swarms.get(swarmId);
		JSONArray members = root.getJSONArray("Members");
		members.put(member);
	}

	/**
	 * Return services list running on the manager in data tables format { data: [SERVER-RESPONSE] }
	 * @param nodeName Node name within the descriptor (nodes.json) with connection information.
	 * @return <pre>{"data":[{"UpdatedAt":"2019-03-28T23:07:34.188916754Z","Spec":{"Name":"web","UpdateConfig":{"Delay":30000000000,"Order":"stop-first","MaxFailureRatio":0,"FailureAction":"pause","Parallelism":2},"TaskTemplate":{"RestartPolicy":{"Condition":"on-failure","MaxAttempts":10,"Delay":10000000000},"LogDriver":{"Name":"json-file","Options":{"max-file":"3","max-size":"10M"}},"Runtime":"container","ForceUpdate":0,"Placement":{"Constraints":["node.role == worker"]},"ContainerSpec":{"User":"root","Command":["cmd","arg1"],"Args":["arg2","arg3"],"Image":"nginx","Isolation":"default"},"Resources":{"Reservations":{},"Limits":{}}},"Labels":{},"Mode":{"Replicated":{"Replicas":1}},"EndpointSpec":{"Mode":"vip","Ports":[{"PublishMode":"ingress","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]}},"ID":"j2keu808fxwdjd6d74uj6ele9","Endpoint":{"Spec":{"Mode":"vip","Ports":[{"PublishMode":"ingress","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]},"VirtualIPs":[{"NetworkID":"wgtbxfl32jml7rit12d92i60m","Addr":"10.255.0.4/16"}],"Ports":[{"PublishMode":"ingress","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]},"Version":{"Index":30},"CreatedAt":"2019-03-28T23:07:34.187621728Z"}]} </pre>
	 * @throws Exception On SSL, I/O or timeout errors.
	 * @throws RestException on Docker daemon errors.
	 */
	public static JSONObject servicesList (final String nodeName) throws Exception, RestException {
		return Docker.invokeApi("ListServices", nodeName);
	}

	public static JSONObject serviceInspect (final String nodeName, String id) throws Exception, RestException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ID", id);
		
		return Docker.invokeApi("InspectService", nodeName, null, null, params);
	}

	/**
	 * Remove a service from a swarm.
	 * @param nodeName Swarm manager name. <b>The request must be sent to the manager</b>.
	 * @param id The service ID.
	 * @return Empty response on success: 200 OK response EMPTY: {"data":""}
	 * @throws Exception On I/O errors, timeouts and others.
	 * @throws RestException If the Docker daemon responds with an error.
	 */
	public static JSONObject serviceRemove (final String nodeName, String id) throws Exception, RestException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ID", id);
		
		return Docker.invokeApi("RemoveService", nodeName, null, null, params);
	}

	/**
	 * Create a swarm service.
	 * @param nodeName Name of the node in (nodes.json). It hass connection info: URL, key store, etc.
	 * @param serviceName A name for the swarm service.
	 * @param image Docker image to use.
	 * @param cmd Command and any arguments separated by spaces (e.g commad arg1 arg2 ...).
	 * @param args Any optional arguments separated by space.
	 * @param replicas Number of cluster replicas to run.
	 * @param ports Comma separated list of exposed ports (e.g 80/tcp:80,...). Format: SRCPORT/PROTO:DSTPORT,...
	 * @param authObj Authorization object for private images.
	 * @return Data tables: {"data":{"ID":"b6qqm1e60icq1lz9hypb93ral"}}
	 * @throws RestException On docker daemon errors.
	 * @throws Exception On I/O errors, server timeputs, etc.
	 */
	public static JSONObject serviceCreate (final String nodeName, String serviceName, final String image, String cmd, String args, int replicas, String ports, final String authObj) 
			throws RestException, Exception 
	{
		if ( image == null) {
			throw new RestException("Image is required.");
		}
		
		Map<String, String> request = new HashMap<String, String>();
		request.put("Image", image);
		request.put("Name", serviceName);
		request.put("Replicas", String.valueOf(replicas));
		request.put("Command", cmd);	
		request.put("Args", args);		
		request.put("Ports", ports);
		
		//request.put("Mounts", "");
		//request.put("Networks", "overlay1,overlay2");		

		// service create json
		String raw 					= IOTools.readFromStream(Docker.class.getResourceAsStream("/resources/docker/service_create.json"));
		Map<String, Object> params  = DockerParams.extractParamsFromMap(request);
		raw 						= DockerParams.replace(params, raw);
		
		//5/18/2019 This must be BASE64 encoded!
		params.put("AUTHOBJ", authObj != null ? Base64.encode(authObj.getBytes(CoreTypes.CHARSET_UTF8)) : ""); //"{}");
	
		// validate
		JSONObject root 			= new JSONObject(raw);
		Docker.TRACE("Create Service <pre>" + root.toString(1) + "</pre>");

		return Docker.invokeApi("CreateService", nodeName, root.toString(), CoreTypes.CONTENT_TYPE_JSON, params);
	}

	/**
	 * Return services list running on the manager in data tables format { data: [SERVER-RESPONSE] }
	 * @param nodeName Node name within the descriptor (nodes.json) with connection information.
	 * @return <pre>{"data":[{"UpdatedAt":"2019-03-29T18:32:47.221225312Z","Status":{"Err":"no suitable node (1 node not available for new tasks; scheduling constraints not satisfied on 1 node)","State":"pending","Message":"pending task scheduling","PortStatus":{},"Timestamp":"2019-03-29T18:32:47.221126921Z"},"ServiceID":"tbkqfwt6cao6vynm2sogn9g5q","Labels":{},"Spec":{"RestartPolicy":{"Condition":"on-failure","MaxAttempts":10,"Delay":10000000000},"LogDriver":{"Name":"json-file","Options":{"max-file":"3","max-size":"10M"}},"ForceUpdate":0,"Placement":{"Constraints":["node.role == worker"]},"ContainerSpec":{"User":"root","Command":["cmd","arg1"],"Args":["arg2","arg3"],"Image":"nginx","Isolation":"default"},"Resources":{"Reservations":{},"Limits":{}}},"Slot":1,"ID":"sloanokwwiuq3rroq5huyv2c7","NetworksAttachments":[{"Addresses":["10.255.0.5/16"],"Network":{"UpdatedAt":"2019-03-29T18:32:45.333892166Z","Spec":{"Ingress":true,"Name":"ingress","DriverConfiguration":{},"Labels":{},"IPAMOptions":{"Driver":{},"Configs":[{"Gateway":"10.255.0.1","Subnet":"10.255.0.0/16"}]},"Scope":"swarm"},"ID":"wgtbxfl32jml7rit12d92i60m","IPAMOptions":{"Driver":{"Name":"default"},"Configs":[{"Gateway":"10.255.0.1","Subnet":"10.255.0.0/16"}]},"Version":{"Index":132},"DriverState":{"Name":"overlay","Options":{"com.docker.network.driver.overlay.vxlanid_list":"4096"}},"CreatedAt":"2019-03-28T13:55:07.52507303Z"}}],"DesiredState":"running","Version":{"Index":139},"CreatedAt":"2019-03-29T14:44:09.974623995Z"}]}</pre>
	 * @throws Exception On SSL, I/O or timeout errors.
	 * @throws RestException on Docker daemon errors.
	 */
	public static JSONObject tasksList (final String nodeName) throws Exception, RestException {
		return Docker.invokeApi("ListTasks", nodeName);
	}

	public static JSONObject taskInspect (final String nodeName, String id) throws Exception, RestException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ID", id);
		
		return Docker.invokeApi("InspectTask", nodeName, null, null, params);
	}

}
