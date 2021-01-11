package junit.docker;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;




import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerContainerNoTLS {

	static RestClient rest;
	
	static void LOGD(String text) {
		System.out.println("[JUNIT-DOCKER-CONTAINER] "  + text);
	}

//	String keyStorePath = "C:\\Users\\vsilva\\.docker\\machine\\certs\\docker.jks"; 
//	String keyStorePassword = "certpass";
	static String keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node1.jks"; 
	static String keyStorePassword = "password";
	
	static String url = "https://192.168.99.100:2376/";
	//String url = "https://10.0.2.15:2376/";
	//String url = "https://172.17.0.1:2376/";
	
	static String id ;
	static String execid ;
	
	@BeforeClass
	public static void init() {
		rest = new RestClient();
		try {
			LOGD("Loading API descriptor from classpath /configuration/docker_api.json");
			InputStream is = Docker.class.getResourceAsStream("/configuration/docker_api.json");
			rest.load(is);
			LOGD(rest.toString());

			// load nodes file from $HOME/.cloud/
			String path = System.getProperty("user.home") + "\\.cloud\\CloudAdapter";
			LOGD("Loading nodes file from " + path);
			
			Map<String, String> config = new HashMap<String, String>();
			config.put(Docker.CONFIG_BASEPATH, path);
			Docker.initialize(config);
			Docker.load();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	

	/*
	 * Exec Create

POST /containers/(id or name)/exec

Sets up an exec instance in a running container id

Example request:

POST /v1.24/containers/e90e34656806/exec HTTP/1.1
Content-Type: application/json
Content-Length: 12345

{
  "AttachStdin": true,
  "AttachStdout": true,
  "AttachStderr": true,
  "Cmd": ["sh"],
  "DetachKeys": "ctrl-p,ctrl-q",
  "Privileged": true,
  "Tty": true,
  "User": "123:456"
}
Example response:

HTTP/1.1 201 Created
Content-Type: application/json

{
     "Id": "f90e34656806",
     "Warnings":[]
}

	 */
	@Test
	public void test02ContainerExecCreateNoTLS() {
		try {
			// cloud/cloud-connector-aes , Name = /loving_mayer @ 192.168.42.248:2375
			url = "http://192.168.42.248:2375/";
			id = "9399aa83cf787a32fcad7262ee5b2f3f3c641d85ffdd0e1a8e7d312b80e4c777";
			LOGD("Container Exec Create Id " + id);
			
			String raw = IOTools.readFromStream(TestDockerContainerNoTLS.class.getResourceAsStream("/resources/docker/container_exec.json"));
			Map<String, String> request = new HashMap<String, String>();
			request.put("Id", id);
			request.put("Cmd", "ls -l");
			
			Map<String, Object> args  = DockerParams.extractParamsFromMap(request);
			raw = DockerParams.replace(args, raw);
			
			LOGD("Container Exec Create params " + request + " => " + args);
			LOGD("Container Exec Create payload : " + raw);
			
			//Object resp = rest.invoke(HTTPDestination.create(url, raw, CoreTypes.CONTENT_TYPE_JSON,  keyStorePath, keyStorePassword), "ContainerExec", params);
			Object resp = rest.invoke(HTTPDestination.create(url, raw, CoreTypes.CONTENT_TYPE_JSON,  null, null), "ContainerExecCreate", args);
			
			// Container down: Conflict (409): {"message":"Container 645399b14cd8e381c283954589bb82c37fc7540245b12c7b84c1da9a24ff3cea is not running"}
			// OK HTTP 201 192.168.99.100:2376 {"Id":"ff905b3513063e9a6eaf037cd994e151178f450b0ebc57b96527e9d6b16be326"}
			// 192.168.42.248:2375 {"Id":"604ba3fafc3ed8702ba30e94ff0a63725e294b57853e2710e80f8a2fd2fda867"}
			LOGD("ExecCreate Server Replied: " + resp.toString());
			
			JSONObject root = (JSONObject) resp;
			execid = root.getString("Id");
			LOGD("ExecCreate exec id=" + execid);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test05ContainerExecStartNoTLS() {
		try {
			// cloud/cloud-connector-aes , Name = /loving_mayer @ 192.168.42.248:2375
			if ( execid == null) execid = "604ba3fafc3ed8702ba30e94ff0a63725e294b57853e2710e80f8a2fd2fda867";
			
			url = "http://192.168.42.248:2375/exec/" + execid + "/start";
			String payload = "{ \"Detach\": false, \"Tty\": true}";
			
			LOGD("Container Exec start url " + url + " Payload " + payload);
			/* Use webclient intead 
			DockerHttpHijack http = new DockerHttpHijack(new URI(url));
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type",  CoreTypes.CONTENT_TYPE_JSON);
			
			http.post(headers, payload);
			
			LOGD("GOT\n" + http.read()); */
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test07ExecShellCommandsNoTLS() {
		try {
			if (id == null) {
				id = "9399aa83cf787a32fcad7262ee5b2f3f3c641d85ffdd0e1a8e7d312b80e4c777";
			}
			String[] cmds = new String[] {"date", "uname -a", "ls -l", "top"};
			for (int i = 0; i < cmds.length; i++) {
				String stdout = Docker.execShellCommand("Node2", id, cmds[i]);
				LOGD(stdout);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
}
