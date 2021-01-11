package junit.docker;

import static org.junit.Assert.*;

import java.net.URL;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.IOTools;
import com.cloud.docker.Docker;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerSwarm {

	
	static void LOGD(String text) {
		System.out.println("[DOCKER-IMAGE-ASYNC] "  + text);
	}

	// Manager
	static String mgrName = "Node3";
	static String node1url = "https://192.168.99.101:2376/";
	static String N1keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node3.jks"; 
	static String N1keyStorePassword = "password";
	
	// Worker
	static String workerName = "Node4";
	static String node2url = "https://192.168.99.102:2376/";
	static String N2keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node4.jks"; 
	static String N2keyStorePassword = "password";
	
	static String SWARM_ID;
	static String JOINTOK_WORKER;
	static String JOINTOK_MANAGER;

	@BeforeClass
	public static void init() {
		try {
			MockObjects.initRestAPIClient();
			MockObjects.fixSSLFatalProtocolVersion();
			MockObjects.loadNodesFile();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/* REQUEST POST /v1.24/swarm/init HTTP/1.1
	Content-Type: application/json
	Content-Length: 12345

	{
	  "ListenAddr": "0.0.0.0:2377",
	  "AdvertiseAddr": "192.168.1.1:2377",
	  "ForceNewCluster": false,
	  "Spec": {
	    "Orchestration": {},
	    "Raft": {},
	    "Dispatcher": {},
	    "CAConfig": {}
	  }
	} 
	--- RESPONSE
	HTTP/1.1 200 OK
Content-Length: 28
Content-Type: application/json
Date: Thu, 01 Sep 2016 21:49:13 GMT
Server: Docker/1.12.0 (linux)

"7v2t30z9blmxuhnyo6s4cpenp"
	*/
	@Test
	public void test02SwarmInit() {
		try {
			// https://docs.docker.com/engine/api/v1.24/#38-swarm
			// POST /swarm/init
			String nodeName = "Node3";
			String advertiseAddress = new URL(node1url).getHost() + ":2377";
			
			/*
			 * [POST] HTTP Response msg: Bad Request
[POST]  [HDR] null = [HTTP/1.1 400 Bad Request]
[POST]  [HDR] Api-Version = [1.39]
[POST]  [HDR] Date = [Mon, 25 Mar 2019 15:32:03 GMT]
[POST]  [HDR] Content-Length = [170]
[POST]  [HDR] Ostype = [linux]
[POST]  [HDR] Docker-Experimental = [false]
[POST]  [HDR] Content-Type = [application/json]
[POST]  [HDR] Server = [Docker/18.09.3 (linux)]
Bad Request (400): {"message":"could not choose an IP address to advertise since this system has multiple addresses on different interfaces (10.0.2.15 on eth0 and 192.168.99.101 on eth1)"}

			 */
			// ERROR  HTTP 400: {"message":"could not choose an IP address to advertise since this system has multiple addresses on different interfaces (10.0.2.15 on eth0 and 192.168.99.101 on eth1)"}
			// OK (200) SWARM-ID "c3hjgrihiknh3o0p9vizl1yox"
			LOGD("Swarm Init Server " + nodeName + " Url: " + node1url + " advertiseAddress: " + advertiseAddress);
			
			SWARM_ID = Docker.swarmInit(nodeName, advertiseAddress);
			LOGD("Swarm Init Server Replied Swarm ID:" + SWARM_ID);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test04SwarmInspect() {
		try {
			LOGD("Swarm  Inspect " + node1url);
			/*
			Object resp = MockObjects.rest.invoke(HTTPDestination.create(node1url, null, null,  N1keyStorePath, N1keyStorePassword), "SwarmInspect");
			
			// {"UpdatedAt":"2019-03-25T15:53:20.555269823Z","JoinTokens":{"Worker":"SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-3j0hurzmutqgwhj4u5podwz1m","Manager":"SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-c1h56fasl7o1v3arqq7smxau0"},"TLSInfo":{"CertIssuerSubject":"MBMxETAPBgNVBAMTCHN3YXJtLWNh","CertIssuerPublicKey":"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAbsDW2ynbwE2HPEDqbfKADfzxlk8qgEyni1wQgFIt5k80G4Xro7F9E9GXHLlkn7742d2OKXVwm6lZghUM0V+5g==","TrustRoot":"-----BEGIN CERTIFICATE-----\nMIIBazCCARCgAwIBAgIUdCDXw5VdR48CvtMH2Rr72WFKiHQwCgYIKoZIzj0EAwIw\nEzERMA8GA1UEAxMIc3dhcm0tY2EwHhcNMTkwMzI1MTU0ODAwWhcNMzkwMzIwMTU0\nODAwWjATMREwDwYDVQQDEwhzd2FybS1jYTBZMBMGByqGSM49AgEGCCqGSM49AwEH\nA0IABAG7A1tsp28BNhzxA6m3ygA388ZZPKoBMp4tcEIBSLeZPNBuF66OxfRPRlxy\n5ZJ+++Nndjil1cJupWYIVDNFfuajQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMB\nAf8EBTADAQH/MB0GA1UdDgQWBBTrOtxni1Vd2uxFAIkCwBBLr9EAoTAKBggqhkjO\nPQQDAgNJADBGAiEA+xZ1/p38XkVOt2NzCNbJHc/v2rPS6AvGZ9Arwfy2JtQCIQDg\nNGnMXE0wfd5PHD5EnhPTuAdfgbHTnpP9D0enPl0MSA==\n-----END CERTIFICATE-----\n"},"SubnetSize":24,"DefaultAddrPool":["10.0.0.0/8"],"Spec":{"Dispatcher":{"HeartbeatPeriod":5000000000},"Name":"default","Orchestration":{"TaskHistoryRetentionLimit":5},"Labels":{},"EncryptionConfig":{"AutoLockManagers":false},"CAConfig":{"NodeCertExpiry":7776000000000000},"TaskDefaults":{},"Raft":{"KeepOldSnapshots":0,"HeartbeatTick":1,"ElectionTick":10,"LogEntriesForSlowFollowers":500,"SnapshotInterval":10000}},"ID":"jnirz6k2qc6qqbk9ofxc86wxi","RootRotationInProgress":false,"Version":{"Index":10},"CreatedAt":"2019-03-25T15:53:19.880849214Z"}
			LOGD("Swarm Inspect Server Replied: " + resp.toString());
			JSONObject root = (JSONObject)resp; 
			*/
			// NOTE Thois returns DAT-TABLES FORMAT: {"data": SERVER_JSON }
			JSONObject root = 	Docker.swarmInspect(mgrName);
			LOGD("Swarm Inspect Server Replied: " + root);
			
			// extract join tokens
			/*{ "UpdatedAt": "2019-03-25T15:53:20.555269823Z",
				"JoinTokens": {
					"Worker": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-3j0hurzmutqgwhj4u5podwz1m",
					"Manager": "SWMTKN-1-0izadorcbcb0rhkd25409dbuakqbsfkedn971nf9d3gb80ndab-c1h56fasl7o1v3arqq7smxau0"
				}, ..... "ID": "jnirz6k2qc6qqbk9ofxc86wxi",....} */	

			// Watch for data-tables format
			JSONObject tokens = root.has("data") ? root.getJSONObject("data").getJSONObject("JoinTokens") : root.getJSONObject("JoinTokens");
			
			JOINTOK_MANAGER = tokens.getString("Manager");
			JOINTOK_WORKER = tokens.getString("Worker");
			
			LOGD("Swarm Inspect Join Tok Mgr: " + JOINTOK_MANAGER + " Worker:" + JOINTOK_WORKER);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/*

    Ports 2377 and 2376

    Always run docker swarm init and docker swarm join with port 2377 (the swarm management port), or no port at all and let it take the default.

    The machine IP addresses returned by docker-machine ls include port 2376, which is the Docker daemon port. Do not use this port or you may experience errors.
	*/
	@Test
	public void test06SwarmJoin() {
		// worker (node2) joins swarm as worker with manager (node1)
		try {
			String raw = IOTools.readFromStream(TestDockerSwarm.class.getResourceAsStream("/resources/docker/swarm_join.json"));
			String advertiseAddr = new URL(node2url).getHost() + ":2377";
			String joinToken = JOINTOK_WORKER;
			// manager addr: addr1,addr2,...
			String remoteAddrs = new URL(node1url).getHost() + ":2377";

			LOGD("Swarm Join as  WORKER:" + node2url + " Token: " + joinToken +  " with Mananger:" + node1url + "  RemoteArrds: " + remoteAddrs);
			
/*
			// worker (node2)
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("ADVERTISEADDR", new URL(node2url).getHost() + ":2377");
			params.put("JOINTOKEN", JOINTOK_WORKER);
			
			// mgrs (node1)
			//String[] mgrs = new String[]{ new URL(node1url).getHost() + ":2377"};
			JSONArray arr = new JSONArray();
			arr.put(new URL(node1url).getHost() + ":2377");
			
			params.put("REMOTEADDRS", arr);
			
			String json = DockerParams.replace(params, raw);
			
			// validate
			JSONObject root = new JSONObject(json);
			LOGD("Swarm Join as  WORKER:" + node2url + " with Mananger:" + node1url + "  Payload: " + json);

			Object resp = MockObjects.rest.invoke(HTTPDestination.create(node2url, root.toString(), CoreTypes.CONTENT_TYPE_JSON,  N2keyStorePath, N2keyStorePassword), "SwarmJoin");
			// OK (200) Empty response Content-Length = 0
			// Service Unavailable (503): {"message":"This node is already part of a swarm. Use \"docker swarm leave\" to leave this swarm and join another one."}
			LOGD("Swarm Join Worker Replied: " + resp.toString());
			
*/
			// OK: Empty resp
			// ERROR: throws ex
			Docker.swarmJoin(workerName, advertiseAddr, joinToken, remoteAddrs);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
	// Leave swarm: mster first, the memebers.
	@Test
	public void test20SwarmLeave() {
		try {
			// worker
			LOGD("Leaving swarm worker " + node2url);
			/*
			params = new HashMap<String, Object>();
			params.put("FORCE", "true");
			
			resp = MockObjects.rest.invoke(HTTPDestination.create(node2url, null, null,  N2keyStorePath, N2keyStorePassword), "SwarmLeave", params);
			
			// Service Unavailable (503): {"message":"This node is not part of a swarm"}
			// Service Unavailable (503): {"message":"You are attempting to leave the swarm on a node that is participating as a manager. Removing the last manager erases all current state of the swarm. Use `--force` to ignore this message. "}
			// OK (200) Empty response.
			LOGD("Swarm Leave Worker Replied: " + resp.toString());
			*/
			Docker.swarmLeave(workerName, true);

			// mgr
			LOGD("Leaving swarm manager " + node1url);
			/*
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("FORCE", "true");
			
			Object resp = MockObjects.rest.invoke(HTTPDestination.create(node1url, null, null,  N1keyStorePath, N1keyStorePassword), "SwarmLeave", params);
			
			// Service Unavailable (503): {"message":"This node is not part of a swarm"}
			// Service Unavailable (503): {"message":"You are attempting to leave the swarm on a node that is participating as a manager. Removing the last manager erases all current state of the swarm. Use `--force` to ignore this message. "}
			// OK (200) Empty response.
			LOGD("Swarm Leave Manager Replied: " + resp.toString());
			*/
			Docker.swarmLeave(mgrName, true);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	
}
