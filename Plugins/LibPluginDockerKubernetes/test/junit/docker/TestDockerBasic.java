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
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestClient.HTTPDestination;
import com.cloud.docker.Docker;
import com.cloud.docker.DockerParams;
import com.cloud.docker.DockerServlet;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerBasic {
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

	/**
	 * Test 01
	 */
	@Test
	public void test01LoadApiJSON() {
		try {
			InputStream is = Docker.class.getResourceAsStream("/configuration/docker_api.json");
			rest.load(is);
			LOGD(rest.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/*
	 * 
	 */
	@Test
	public void test02InvokeListEndPoints() {
		
		String[] names = rest.getEndPointNames();
		
		for (int i = 0; i < names.length; i++) {
			try {
				// Skip POST
				if (names[i].contains("List") || names[i].contains("SysInfo")) {
					LOGD("==== Invoke " + names[i]);
					Object root = rest.invoke(HTTPDestination.create(url, keyStorePath, keyStorePassword), names[i]);
					LOGD(names[i] + ": " + root.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}
	}
	
	@Test
	public void test03DockerCreateContainerParams() {
		try {
			Map<String, String> map = new HashMap<String, String>();
			
			// ENV=["VAR1=VAl1","foo=bar"], IMAGE=ubuntu, CMD=date
			map.put(DockerParams.W3_PARAM_IMAGE, "ubuntu");
			map.put(DockerParams.W3_PARAM_ENV, "VAR1=VAl1,foo=bar");
			map.put(DockerParams.W3_PARAM_CMD, "date");
			
			// EXPOSEDPORTS={"22/tcp":{},"1028/udp":{}}}
			map.put(DockerParams.W3_PARAM_EXPOSEDPORTS, "22/tcp,1028/udp");

			// PORTBINDINGS={"80/tcp":"[{ \"HostPort\": \"80\" }]","8080":"[{ \"HostPort\": \"8080\" }]"}
			map.put(DockerParams.W3_PARAM_PORTBINDINGS, "8080:8080,80/tcp:80");

			// BINDS=["/tmp:/tmp","/foo:/foo"]
			map.put(DockerParams.W3_PARAM_BINDS, "/tmp:/tmp,/foo:/foo");
			
			// VOLUMES={"/volumes/data1":{},"/volumes/data":{}}
			map.put(DockerParams.W3_PARAM_VOLUMES, "/volumes/data,/volumes/data1");

			// LABELS={"com.sample.vendor":"ACME","com.sample.version":"1.0"}
			map.put(DockerParams.W3_PARAM_LABELS, "com.sample.vendor:ACME,com.sample.version:1.0");
			
			LOGD("REQUEST PARAMS: " + map.toString());
			
			Map <String, Object> map1 = DockerParams.extractParamsFromMap(map);
			LOGD("EXTRACTED PARAMS:" + map1.toString());

			// get raw json 
			String raw = IOTools.readFromStream(DockerServlet.class.getResourceAsStream("/resources/docker/container_create.json"));
			
			// replace params in raw json
			String json = DockerParams.replace(map1, raw);
			LOGD("Docker JSON <pre>" + json + "</pre>");
			
			// validate
			JSONObject root = new JSONObject(json);
			LOGD("Request JSON syntax OK.");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test04DockerMiscParams() {
		try {
			
			Map<String, String> map = new HashMap<String, String>();
			
			// test singlequotes params
			map.put(DockerParams.W3_PARAM_CMD, "nginx -g 'daemon off;'");

			LOGD("TEST-SINGLEQUOTED-PARAMS: REQUEST PARAMS: " + map.toString());
			
			// expected: [ngingx, g , 'daemon off;'] got ["nginx","-g","'daemon","off;'"]
			Map <String, Object> map1 = DockerParams.extractParamsFromMap(map);
			
			String expected = "[\"nginx\",\"-g\",\"daemon off;\"]";
			String received = map1.get("CMD").toString();
			
			LOGD("TEST-SINGLEQUOTED-PARAMS: EXTRACTED PARAMS:" + map1);

			JSONArray cmds = new JSONArray(received);
			
			//assertEquals("Params size must be 3 [ngingx, g , 'daemon off;'] "  , 3, cmds.length());
			assertTrue("Invalid single quoted params expected: " + expected + " but got " + received , expected.equals(received));
			assertTrue("Cmd array size must be 3 for " + cmds, cmds.length() == 3);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
