package junit.docker;

import static org.junit.Assert.*;


import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.docker.DockerSwarm;

/*
 * Need a live swarm to run these. Use TestDockerSwarm to create one.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerSwarmServices {

	
	static void LOGD(String text) {
		System.out.println("[DOCKER-SERVICE] "  + text);
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
	
	// required: swarm ufh1ytudbgifspdxfwlna3qm5
	static String SWARM_ID = "prvcn43clt0mde3te4tsowqma";
	static String JOINTOK_WORKER;
	static String JOINTOK_MANAGER;

	static String master = "Node3";
	static String SERVICEID;
	
	@BeforeClass
	public static void init() {
		try {
			MockObjects.initRestAPIClient();
			MockObjects.fixSSLFatalProtocolVersion();
			MockObjects.loadNodesFile();
			
			master = DockerSwarm.findManager(SWARM_ID);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}


	/*
	 * Deploy service - https://docs.docker.com/engine/swarm/swarm-tutorial/deploy-service/
	 */
	@Test
	public void test08Createservice() {
		try {
			String image = "nginx";
			//String tag = "latest";
			String authObj = null; //"";
			String ports = "80/tcp:80";
			String command 	= "nginx -g 'daemon off;'";
			String args 	= null;
			
			JSONObject root = DockerSwarm.serviceCreate(master, "web", image, command, args, 1, ports, authObj);

			// 200 OK: {"data":{"ID":"j16zepohtxmmay8dcl9remh67"}}
			// Bad Request (400): {"message":"rpc error: code = InvalidArgument desc = port '8080' is already in use by service 'web' (j2keu808fxwdjd6d74uj6ele9) as an ingress port"}
			LOGD("CreateService Server replied:" + root);
			SERVICEID = root.getJSONObject("data").getString("ID");
			LOGD("Got service ID=" + SERVICEID);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
	// {"data":[{"UpdatedAt":"2019-03-28T23:07:34.188916754Z","Spec":{"Name":"web","UpdateConfig":{"Delay":30000000000,"Order":"stop-first","MaxFailureRatio":0,"FailureAction":"pause","Parallelism":2},"TaskTemplate":{"RestartPolicy":{"Condition":"on-failure","MaxAttempts":10,"Delay":10000000000},"LogDriver":{"Name":"json-file","Options":{"max-file":"3","max-size":"10M"}},"Runtime":"container","ForceUpdate":0,"Placement":{"Constraints":["node.role == worker"]},"ContainerSpec":{"User":"root","Command":["cmd","arg1"],"Args":["arg2","arg3"],"Image":"nginx","Isolation":"default"},"Resources":{"Reservations":{},"Limits":{}}},"Labels":{},"Mode":{"Replicated":{"Replicas":1}},"EndpointSpec":{"Mode":"vip","Ports":[{"PublishMode":"ingress","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]}},"ID":"j2keu808fxwdjd6d74uj6ele9","Endpoint":{"Spec":{"Mode":"vip","Ports":[{"PublishMode":"ingress","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]},"VirtualIPs":[{"NetworkID":"wgtbxfl32jml7rit12d92i60m","Addr":"10.255.0.4/16"}],"Ports":[{"PublishMode":"ingress","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]},"Version":{"Index":30},"CreatedAt":"2019-03-28T23:07:34.187621728Z"}]}
	@Test
	public void test09ListServices() {
		try {
			//String master = "Node3";
			LOGD("List services using mgr " + master + " Swarm " + SWARM_ID);

			JSONObject root = DockerSwarm.servicesList(master);
			LOGD("List services Got: " +  root.toString());
			
			if ( SERVICEID == null) {
				JSONArray array = root.getJSONArray("data");
				SERVICEID = array.length() > 0 ?  array.getJSONObject(0).getString("ID") : null;
				LOGD("List services using Service ID " + SERVICEID);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test10InpectService() {
		try {
			LOGD("Inspect service " + SERVICEID + " @ mgr " + master + " Swarm " + SWARM_ID);

			JSONObject root = DockerSwarm.serviceInspect(master, SERVICEID);
			LOGD("Inspect service got: " + root.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	static String TASKID;
	
	@Test
	public void test11ListTasks() {
		try {
			//String master = "Node3";
			LOGD("List tasks using mgr " + master + " Swarm " + SWARM_ID);

			// {"data":[{"UpdatedAt":"2019-03-29T18:32:47.221225312Z","Status":{"Err":"no suitable node (1 node not available for new tasks; scheduling constraints not satisfied on 1 node)","State":"pending","Message":"pending task scheduling","PortStatus":{},"Timestamp":"2019-03-29T18:32:47.221126921Z"},"ServiceID":"tbkqfwt6cao6vynm2sogn9g5q","Labels":{},"Spec":{"RestartPolicy":{"Condition":"on-failure","MaxAttempts":10,"Delay":10000000000},"LogDriver":{"Name":"json-file","Options":{"max-file":"3","max-size":"10M"}},"ForceUpdate":0,"Placement":{"Constraints":["node.role == worker"]},"ContainerSpec":{"User":"root","Command":["cmd","arg1"],"Args":["arg2","arg3"],"Image":"nginx","Isolation":"default"},"Resources":{"Reservations":{},"Limits":{}}},"Slot":1,"ID":"sloanokwwiuq3rroq5huyv2c7","NetworksAttachments":[{"Addresses":["10.255.0.5/16"],"Network":{"UpdatedAt":"2019-03-29T18:32:45.333892166Z","Spec":{"Ingress":true,"Name":"ingress","DriverConfiguration":{},"Labels":{},"IPAMOptions":{"Driver":{},"Configs":[{"Gateway":"10.255.0.1","Subnet":"10.255.0.0/16"}]},"Scope":"swarm"},"ID":"wgtbxfl32jml7rit12d92i60m","IPAMOptions":{"Driver":{"Name":"default"},"Configs":[{"Gateway":"10.255.0.1","Subnet":"10.255.0.0/16"}]},"Version":{"Index":132},"DriverState":{"Name":"overlay","Options":{"com.docker.network.driver.overlay.vxlanid_list":"4096"}},"CreatedAt":"2019-03-28T13:55:07.52507303Z"}}],"DesiredState":"running","Version":{"Index":139},"CreatedAt":"2019-03-29T14:44:09.974623995Z"}]}
			JSONObject root = DockerSwarm.tasksList(master);
			LOGD("List tasks: " + root.toString());
			
			JSONArray array = root.getJSONArray("data");
			TASKID = array.length() > 0 ? array.getJSONObject(0).getString("ID") : null;
			
			LOGD("List tasks using task ID " + TASKID); 
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test13InspectTask() {
		try {
			LOGD("Inspect task " + TASKID + " @ mgr " + master + " Swarm " + SWARM_ID);

			JSONObject root = DockerSwarm.taskInspect(master, TASKID);
			LOGD("Inspect task got: "  + root.toString());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test20RemoveService() {
		try {
			// "ID": "j2keu808fxwdjd6d74uj6ele9"
			if ( SERVICEID == null) {
				SERVICEID = "tbkqfwt6cao6vynm2sogn9g5q";
			}
			JSONObject root = DockerSwarm.serviceRemove(master, SERVICEID);
			// 200 OK response EMPTY: {"data":""}
			LOGD("Remove service response: " + root);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	
	
}
