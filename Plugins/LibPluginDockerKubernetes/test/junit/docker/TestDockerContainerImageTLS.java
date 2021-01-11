package junit.docker;

import static org.junit.Assert.*;

import java.io.InputStream;




import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestClient.HTTPDestination;
import com.cloud.docker.Docker;
import com.cloud.docker.DockerParams;
import com.cloud.docker.DockerServlet;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerContainerImageTLS {

	static RestClient rest;
	
	static void LOGD(String text) {
		System.out.println("[DOCKER] "  + text);
	}

//	String keyStorePath = "C:\\Users\\vsilva\\.docker\\machine\\certs\\docker.jks"; 
//	String keyStorePassword = "certpass";
	static String keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node1.jks"; 
	static String keyStorePassword = "password";
	
	static String url = "https://192.168.99.100:2376/";
	//String url = "https://10.0.2.15:2376/";
	//String url = "https://172.17.0.1:2376/";
	
	static String id ;
	
	@BeforeClass
	public static void init() {
		rest = new RestClient();
	}
	

	@Ignore
	@Test
	public void test05CreateContainer() {
		try {
			/*
			 * Create a container - https://docs.docker.com/engine/api/v1.24/#31-containers
POST /containers/create
POST /v1.24/containers/create HTTP/1.1
Content-Type: application/json
 Query parameters:
  name – Assign the specified name to the container. Must match /?[a-zA-Z0-9_-]+.
			 */
			String raw = IOTools.readFromStream(TestDockerContainerImageTLS.class.getResourceAsStream("/resources/docker/container_create.json"));
			
			// [ "date"]
			JSONArray cmd = new JSONArray();
			cmd.put("/hello");
			
			// {"com.example.version": "1.0", "com.example.vendor": "ACME" }
			JSONObject labels = new JSONObject();
			labels.put("com.example.vendor", "ACME");
			labels.put("com.example.version", "1.0");

			// {"/volumes/data": {}}
			JSONObject volumes = new JSONObject();
			volumes.put("/volumes/data", new JSONObject());

			// { "22/tcp": {} }
			JSONObject ports = new JSONObject();
			ports.put("22/tcp", new JSONObject());

			Map<String, Object> params = new HashMap<String, Object>();
			//params.put("ENV", "[\"FOO=bar\"]");
			params.put("ENV", "[]");
			params.put("CMD", cmd);
			params.put("IMAGE", "hello-world"); //"ubuntu");
			params.put("LABELS", labels);
			params.put("VOLUMES", volumes);
			
			params.put("EXPOSEDPORTS", ports);
			params.put("BINDS", "[\"/tmp:/tmp\"]");
			//params.put("BINDS", "[]");
			params.put("VOLUMES", volumes);
			params.put("PORTBINDINGS", "{ \"22/tcp\": [{ \"HostPort\": \"11022\" }] }");
			//params.put("PORTBINDINGS", "8080:8080");
			
			//System.out.println(raw);
			String json = DockerParams.replace(params, raw);
			//System.out.println(json);
			
			// validate
			JSONObject root = new JSONObject(json);
			//root.getJSONObject("HostConfig").remove("Tmpfs");
			
			LOGD(root.toString(1));
			
			// Bad Request (400): {"message":"Requested CPUs are not available - requested 0,1, available: 0"}
			// {"Id":"4995991c737c8da73b1c3afb80c63c7d04cb533453e56230f71c7405fa04b8e8","Warnings":["linux does not support CPU percent. Percent discarded.","Your kernel does not support Block I/O weight or the cgroup is not mounted. Weight discarded.","Your kernel does not support Block I/O weight_device or the cgroup is not mounted. Weight-device discarded."]}
			Object resp = rest.invoke(HTTPDestination.create(url, root.toString(), CoreTypes.CONTENT_TYPE_JSON,  keyStorePath, keyStorePassword), "CreateContainer");
			
			LOGD("Server Replied: " + resp.toString());
			id = ((JSONObject)resp).getString("Id");

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Ignore
	@Test
	public void test06StartContainer() {
		try {
			//id = "e6c37ee57f647f5324e59cc41b02bac4df612f9f0e4ecf5db9f797e558fd8790"; // bad
			//id = "b8725e2b5e451035296255bdbb40612d5c237440e6ee2914d323c172724ecb4d"; // good
			LOGD("Start container " + id);
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("ID", id);
			
			// {"message":"linux runtime spec resources: no such file or directory"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "StartContainer", params);
			LOGD("Server Replied: " + resp.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test07RemoveContainer() {
		try {
			//id = "b154fdf4b05a166ebc23ebf2e846422005f20d5d562a1d20882a64d8055a76b8"; // bad
			//id = "b8725e2b5e451035296255bdbb40612d5c237440e6ee2914d323c172724ecb4d"; // good
			LOGD("Remove container " + id);
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("ID", id);
			
			// {"message":"linux runtime spec resources: no such file or directory"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "RemoveContainer", params);
			LOGD("Server Replied: " + resp.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test08ContainerLogs() {
		// URL encode https://www.w3schools.com/tags/ref_urlencode.asp &(%26) =(%3D)
		// http://localhost:9080/CloudClusterManager/Docker?node=Node1&op=ContainerLogs&Id=6b0d78368a5b098f2d2dfded877d5b37588ada681b1e9fe1d83cc941dfcf52be&logargs=stdout%3D1%26stderr%3D1%26tail%3D20
		try {
			id = "6b0d78368a5b098f2d2dfded877d5b37588ada681b1e9fe1d83cc941dfcf52be"; 
			LOGD("Container logs for " + id);
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("ID", id);
			params.put("LOGARGS", "stdout=0&stderr=1&tail=all");
			
			// {"message":"linux runtime spec resources: no such file or directory"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "ContainerLogs", params);
			LOGD(resp.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	// https://docs.docker.com/engine/api/v1.24/#32-images
	// https://hub.docker.com/_/busybox
	static String image = "busybox";	
	static String tag = "latest";	
	
	@Ignore
	@Test
	public void test10CreateImage() {
		try {
			LOGD("Create Image " + image + " TAG:" + tag);
			/*
			JSONObject Auth = new JSONObject();
			Auth.put("username", "cloud");
			Auth.put("password", "Thenewcti1"); */
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("NAME", image);
			params.put("TAG", tag);
			params.put("AUTHOBJ", "");
			
			// {"id":"latest","status":"Pulling from library/busybox"}
			// HTTP Not Found (404): {"message":"pull access denied for busybox_foo, repository does not exist or may require 'docker login'"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "CreateImage", params);
			LOGD("Server Replied: " + resp.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test11RemoveImage() {
		try {
			LOGD("Remove Image " + image);
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("NAME", image + ":" + tag);
			
			// 200 [{"Untagged":"busybox:latest"},{"Untagged":"busybox@sha256:061ca9704a714ee3e8b80523ec720c64f6209ad3f97c0ff7cb9ec7d19f15149f"},{"Deleted":"sha256:d8233ab899d419c58cf3634c0df54ff5d8acc28f8173f09c21df4a07229e1205"},{"Deleted":"sha256:adab5d09ba79ecf30d3a5af58394b23a447eda7ffffe16c500ddc5ccb4c0222f"}]
			// Not Found (404): {"message":"No such image: busybox_foo:latest"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "RemoveImage", params);
			LOGD("Server Replied: " + resp.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
}
