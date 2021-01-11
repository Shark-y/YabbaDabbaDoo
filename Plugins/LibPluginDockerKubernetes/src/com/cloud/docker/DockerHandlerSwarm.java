package com.cloud.docker;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.w3.RestException;

public class DockerHandlerSwarm {
	
	private static final Logger log = LogManager.getLogger(DockerHandlerSwarm.class);

	static void handleSwarm (final String op, final String node, HttpServletRequest request, HttpServletResponse response) throws IOException, RestException, Exception {
		if ( op.equals("SwarmInit")) {
			handleSwarmInit(node, request, response);
		}
		else if ( op.equals("SwarmLeave")) {
			handleSwarmLeave(node, request, response, true);
		}
		else if ( op.equals("SwarmJoin")) {
			handleSwarmJoin(node, request, response);
		}
		else if ( op.equals("SwarmInspect")) {
			handleSwarmInspect(node, request, response);
		}
		else if ( op.equals("SwarmRemove")) {
			handleSwarmRemove(request, response);
		}
		else if ( op.equals("CreateService")) {
			handleServiceCreate(request, response);
		}
		else if ( op.equals("RemoveService")) {
			handleServiceRemove(request, response);
		}
		else {
			throw new RestException("HandleSwarm: Invalid operation " + op, 400, "BAD REQUEST");
		}
	}
	
	static void handleSwarmInit (final String node, HttpServletRequest request, HttpServletResponse response) throws RestException, Exception {
		final String advertiseAddr 	= request.getParameter(DockerParams.W3_PARAM_ADVERTISEADDR);
		
		if ( advertiseAddr == null) {
			throw new IOException("Advetise address is required.");
		}
		// Invoke
		// OK (200) SWARM-ID "c3hjgrihiknh3o0p9vizl1yox"
		String swarmId = Docker.swarmInit(node, advertiseAddr);
		log.debug("SwarmInit swarm id : " + swarmId);
		
		// response
		JSONObject root = new JSONObject();
		root.put("SwarmId", swarmId);
		
		// get tokens
		JSONObject inspect =  Docker.swarmInspect(node);

		/*{ "UpdatedAt": "2019-03-25T15:53:20.555269823Z",
		"JoinTokens": {
			"Worker": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-3j0hurzmutqgwhj4u5podwz1m",
			"Manager": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-c1h56fasl7o1v3arqq7smxau0"
		}, ..... "ID": "jnirz6k2qc6qqbk9ofxc86wxi",....} */	

		// Watch for data-tables format
		JSONObject tokens = inspect.has("data") ? inspect.getJSONObject("data").getJSONObject("JoinTokens") : inspect.getJSONObject("JoinTokens");

		// SAVE
		Docker.addSwarm(swarmId, node, tokens, new String[] {});
		Docker.saveSwarms();
		
		HTTPServerTools.injectStatus(root, 200, "OK");
		response.getWriter().print(root.toString()); 
	}

	static void handleSwarmJoin (final String node, HttpServletRequest request, HttpServletResponse response) throws RestException, Exception {
		final String swarmId 		= request.getParameter(DockerParams.W3_PARAM_ID);	// Swarm ID
		final String advertiseAddr 	= request.getParameter(DockerParams.W3_PARAM_ADVERTISEADDR);
		final String remoteAddrs 	= request.getParameter(DockerParams.W3_PARAM_REMOTEADDRS);
		final String joinAs			= request.getParameter("selJoinAs");	// Worker, Manager

		if ( swarmId == null) {
			throw new IOException("Swarm Id is required.");
		}
		if ( advertiseAddr == null) {
			throw new IOException("Advetise address is required.");
		}
		if ( remoteAddrs == null) {
			throw new IOException("Remote addresses are required.");
		}
		if ( joinAs == null) {
			throw new IOException("Join as Worker/Manager is required.");
		}
		if ( !joinAs.equals("Worker") && !joinAs.equals("Manager")) {
			throw new IOException("Invalid join mode " + joinAs);
		}
		
		// Get
		//JSONObject swarm 	= DockerSwarm.getSwarm(swarmId);
		//final String token 	= swarm.getJSONObject("JoinTokens").getString(joinAs);
		DockerSwarm.Swarm swarm 	= DockerSwarm.getSwarm(swarmId);
		final String token 			= swarm.getJoinTokens().getString(joinAs);
		
		// Invoke OK (200) - EMPTY RESPONSE else  REST ex
		log.debug("Swarm Join Id:" + swarmId + " Node:" + node + " Mode:" + joinAs + " Tok:" + token);
		Docker.swarmJoin(node, advertiseAddr, token, remoteAddrs);
		
		// Add member to descriptor
		Docker.swarmAddMember(swarmId, node);
		
		// response
		JSONObject root = new JSONObject();
		
		HTTPServerTools.injectStatus(root, 200, "OK");
		response.getWriter().print(root.toString()); 
	}
	
	static void handleSwarmLeave (final String node, HttpServletRequest request, HttpServletResponse response, boolean writeResponse) throws RestException, Exception {
		String force 	= request.getParameter("force");
		
		if ( force == null) {
			force = "true";
		}
		// Invoke
		// OK (200) empty response else REST Ex
		Docker.swarmLeave(node, Boolean.parseBoolean(force));
		
		// response
		if ( writeResponse) {
			JSONObject root = new JSONObject();
			
			HTTPServerTools.injectStatus(root, 200, "OK");
			response.getWriter().print(root.toString()); 
		}
	}
	
	static void handleSwarmInspect (final String node, HttpServletRequest request, HttpServletResponse response) throws RestException, Exception {
		// Invoke
		// OK (200) empty response else REST Ex
		JSONObject root = Docker.swarmInspect(node);
		
		HTTPServerTools.injectStatus(root, 200, "OK");
		response.getWriter().print(root.toString()); 
	}
	
	static void handleSwarmRemove (HttpServletRequest request, HttpServletResponse response) throws RestException, Exception {
		final String swarmId 		= request.getParameter(DockerParams.W3_PARAM_ID);	// Swarm ID
		if ( swarmId == null) {
			throw new IOException("Swarm Id is required.");
		}
		// Get swarm
		DockerSwarm.Swarm swarm 	= DockerSwarm.getSwarm(swarmId);
		String master 				= swarm.getMaster();

		// remove master
		JSONArray warnings = new JSONArray();
		try {
			log.debug("Removing master " + master);
			// node may been removed already
			handleSwarmLeave(master, request, response, false);
			
		} catch (RestException e) {
			warnings.put(master + " " + DockerParams.parseServerErrorReponse(e.getMessage()));
		}
		
		// remove members
		JSONArray members = swarm.getMembers();
		log.debug("Removing members: " + members.toString());
		
		for (int i = 0; i < members.length(); i++) {
			String member = members.getString(i);
			try {
				handleSwarmLeave(member, request, response, false);
			} catch (RestException e) {
				warnings.put(member + " " + DockerParams.parseServerErrorReponse(e.getMessage()));
			}
		}
		
		// REMOVE FROM DISK
		DockerSwarm.removeSwarm(swarmId);
		Docker.saveSwarms();
		
		JSONObject root = HTTPServerTools.buildBaseResponse(200, "OK");
		if ( warnings.length() > 0) {
			root.put("warnings", warnings);
		}
		response.getWriter().print(root.toString());
	}
	
	/**
	 * <pre>[{
	"SwarmId": "zy8fmv87iysykdkxtktp9s5td",
	"JoinTokens": {
		"Worker": "SWMTKN-1-1jqc9grkl4jj3jx12focnzs3179x0fckmiddfu94lkl5q192mr-dzzu2s8gtezrl6shn85yuivyn",
		"Manager": "SWMTKN-1-1jqc9grkl4jj3jx12focnzs3179x0fckmiddfu94lkl5q192mr-cfqxu5vmw92jgbtqqkn70mbq2"
	},
	"nodes": [{
		"UpdatedAt": "2019-03-27T22:46:09.964958979Z",
		"Status": {
			"State": "ready",
			"Addr": "192.168.99.102"
		},
		"Description": {
			"TLSInfo": {
				"CertIssuerSubject": "MBMxETAPBgNVBAMTCHN3YXJtLWNh",
				"CertIssuerPublicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaY4GjZWD1p3tO2+g3h/Z8AOjMQK85Q/GJbokJccEV9j8phX4D7q8aU9Lq9j+zMkgE9vWu5myruhV8jU839FVIg==",
				"TrustRoot": ""
			},
			"Engine": {
				"Plugins": [{
					"Name": "awslogs",
					"Type": "Log"
				} ],
				"Labels": {
					"provider": "virtualbox"
				},
				"EngineVersion": "18.09.3"
			},
			"Platform": {
				"OS": "linux",
				"Architecture": "x86_64"
			},
			"Hostname": "node1",
			"Resources": {
				"MemoryBytes": 1037418496,
				"NanoCPUs": 1000000000
			}
		},
		"Spec": {
			"Availability": "active",
			"Labels": {},
			"Role": "worker"
		},
		"ID": "242yi66tn9mtthnqe8g3yd25v",
		"Version": {
			"Index": 15
		},
		"CreatedAt": "2019-03-27T22:46:09.838457379Z"
	}, {
		"UpdatedAt": "2019-03-27T22:44:45.261110663Z",
		"Status": {
			"State": "ready",
			"Addr": "192.168.99.101"
		},
		"Description": {
			"TLSInfo": {
				"CertIssuerSubject": "MBMxETAPBgNVBAMTCHN3YXJtLWNh",
				"CertIssuerPublicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaY4GjZWD1p3tO2+g3h/Z8AOjMQK85Q/GJbokJccEV9j8phX4D7q8aU9Lq9j+zMkgE9vWu5myruhV8jU839FVIg==",
				"TrustRoot": ""
			},
			"Engine": {
				"Plugins": [{
					"Name": "awslogs",
					"Type": "Log"
				}],
				"Labels": {
					"provider": "virtualbox"
				},
				"EngineVersion": "18.09.3"
			},
			"Platform": {
				"OS": "linux",
				"Architecture": "x86_64"
			},
			"Hostname": "node2",
			"Resources": {
				"MemoryBytes": 1037418496,
				"NanoCPUs": 1000000000
			}
		},
		"ManagerStatus": {
			"Reachability": "reachable",
			"Leader": true,
			"Addr": "192.168.99.101:2377"
		},
		"Spec": {
			"Availability": "active",
			"Labels": {},
			"Role": "manager"
		},
		"ID": "zy8fmv87iysykdkxtktp9s5td",
		"Version": {
			"Index": 9
		},
		"CreatedAt": "2019-03-27T22:44:44.737703857Z"
	}],
	"Members": ["Node4"],
	"Master": "Node3"
}] </pre>
	 * @param request
	 * @param response
	 */
	static void handleSwarmView (HttpServletRequest request, HttpServletResponse response) {
		try {
			boolean fetchNodeInfo 	= request.getParameter("nodes") != null ? Boolean.parseBoolean(request.getParameter("nodes")) : true;
			JSONArray view 			= DockerSwarm.swarmsToJSON(fetchNodeInfo);
			response.getWriter().print(view.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** <pre>
	 * -- START [POST] --
	swarmId -> ufh1ytudbgifspdxfwlna3qm5 
	node -> Node4 
	op -> RemoveService 
	Id -> tbkqfwt6cao6vynm2sogn9g5q 
-- END [POST] --</pre>
	 * @param request
	 * @param response
	 * @throws Exception On I/O errors, timeouts and others.
	 * @throws RestException On Docker daemon errors.
	 */
	static void handleServiceRemove (HttpServletRequest request, HttpServletResponse response) throws RestException, Exception  {
		final String manager 	= request.getParameter("node");
		final String serviceId	= request.getParameter(DockerParams.W3_PARAM_ID);
		if ( manager == null)		throw new IOException("Swarm mananger is required.");
		if ( serviceId == null)		throw new IOException("A service id is required.");
		
		JSONObject root = DockerSwarm.serviceRemove(manager, serviceId);
		response.getWriter().print(root.toString());
	}
	
	/** <pre>
	 * -- START [POST] --
	swarmId -> prvcn43clt0mde3te4tsowqma 
	node -> Node3 
	op -> CreateService 
	Id ->  
	Name -> web
	Image -> nginx 
	Replicas -> 1 
	Command -> ngingx -d 'daemon off' 
	Ports ->  80/tcp:80
	Mounts ->  
-- END [POST] --
	 * @param request HTTP request.
	 * @param response HTTP response.
	 * @throws RestException
	 * @throws Exception
	 */
	static void handleServiceCreate (HttpServletRequest request, HttpServletResponse response) throws RestException, Exception  {
		final String manager 	= request.getParameter("node");
		final String name		= request.getParameter(DockerParams.W3_PARAM_NAME);
		final String image 		= request.getParameter(DockerParams.W3_PARAM_IMAGE);
		final String ports 		= request.getParameter(DockerParams.W3_PARAM_PORTS) ; // "80/tcp:80";
		final String command 	= request.getParameter(DockerParams.W3_PARAM_COMMAND); 	// "nginx -g 'daemon off;'";
		String replicas			= request.getParameter(DockerParams.W3_PARAM_REPLICAS);
		String args 			= null;
		String authObj 			= null; 

		if  (manager == null)	throw new IOException("Swarm manager is required.");
		if  (name == null)		throw new IOException("Service name is required.");
		if  (image == null)		throw new IOException("Image name is required.");
		if  (command == null)	throw new IOException("Image command is required.");
		if  ( replicas == null)	replicas = "1";
		
		// 200 OK: {"data":{"ID":"j16zepohtxmmay8dcl9remh67"}}
		// Bad Request (400): {"message":"rpc error: code = InvalidArgument desc = port '8080' is already in use by service 'web' (j2keu808fxwdjd6d74uj6ele9) as an ingress port"}
		JSONObject root = DockerSwarm.serviceCreate(manager, name, image, command.trim(), args, Integer.valueOf(replicas), ports, authObj);

		response.getWriter().print(root.toString());
	}

}
