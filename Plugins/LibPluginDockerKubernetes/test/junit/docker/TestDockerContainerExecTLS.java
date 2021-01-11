package junit.docker;

import static org.junit.Assert.*;

import java.io.InputStream;




import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestClient.HTTPDestination;
import com.cloud.docker.Docker;
import com.cloud.docker.DockerHttpHijack;
import com.cloud.docker.DockerParams;
import com.cloud.ssh.StreamIO;

/**
 * This junt requires container to be up:
 * /elegant_stonebraker	645399b14cd8e381c283954589bb82c37fc7540245b12c7b84c1da9a24ff3cea	cloud/cloud-connector-aes
 * 
 * @author VSilva
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerContainerExecTLS {

	static RestClient rest;
	
	static void LOGD(String text) {
		System.out.println("[JUNIT-DOCKER-CONTAINER-TLS] "  + text);
	}

	// TLS node Node1, container connector-aes
//	static String keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node1.jks"; 
//	static String keyStorePassword = "password";
//	static String url = "https://192.168.99.100:2376/";
//	static String id = "645399b14cd8e381c283954589bb82c37fc7540245b12c7b84c1da9a24ff3cea";
//	static String node = "Node1";

	// ngingx Node4
	static String keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node4.jks"; 
	static String keyStorePassword = "password";
	static String url = "https://192.168.99.102:2376/";
	static String id = "e83c1039e74dc464756c90d2a42e8cb0d58843776fd596de3f8e9bb8e3746a9b";
	static String node = "Node4";
	
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
	
	@Test
	public void test02ContainerExecCreateTLS() {
		try {
			// cloud/cloud-connector-aes , Name = /loving_mayer @ 192.168.42.248:2375
			//url = "https://192.168.99.100:2376/";
			//id = "645399b14cd8e381c283954589bb82c37fc7540245b12c7b84c1da9a24ff3cea";
			LOGD("Container Exec Create Id " + id);
			
			String raw = IOTools.readFromStream(TestDockerContainerNoTLS.class.getResourceAsStream("/resources/docker/container_exec.json"));
			Map<String, String> request = new HashMap<String, String>();
			request.put("Id", id);
			//request.put("Cmd", "ls -l");
			request.put("Cmd", "/bin/bash");
			
			Map<String, Object> args  = DockerParams.extractParamsFromMap(request);
			raw = DockerParams.replace(args, raw);
			
			LOGD("Container Exec Create params " + request + " => " + args);
			LOGD("Container Exec Create payload : " + raw);
			
			Object resp = rest.invoke(HTTPDestination.create(url, raw, CoreTypes.CONTENT_TYPE_JSON,  keyStorePath, keyStorePassword), "ContainerExecCreate", args);
			
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
	
	// Start a container (app) hijacking the the socket (docker attach protocol)
	@Test
	public void test05ContainerHijackExecStartTLS() {
		try {
			// cloud/cloud-connector-aes , Name = /loving_mayer @ 192.168.42.248:2375
			if ( execid == null) execid = "604ba3fafc3ed8702ba30e94ff0a63725e294b57853e2710e80f8a2fd2fda867";
			
			//url = "http://192.168.42.248:2375/exec/" + execid + "/start";
			String Url = url + "exec/" + execid + "/start";
			String payload = "{ \"Detach\": false, \"Tty\": true}";
			
			LOGD("Container Exec start url " + Url + " Payload " + payload);

			DockerHttpHijack http = new DockerHttpHijack(new URI(Url));
			http.setSSLParams("TLSv1.2", keyStorePath, keyStorePassword, true);
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type",  CoreTypes.CONTENT_TYPE_JSON);
			
			http.post(headers, payload, true);

			http.pipeStdout(new StreamIO.PrintStreamSource() {
				@Override
				public PrintStream getPrintStream() {
					return System.out;
				}

				@Override
				public boolean closeAfterWrite() {
					return false;
				}
			} );

			Thread.sleep(1000);

			String cmd = "ls -l\n";
			//System.err.println("Sending command " + cmd);
			http.send(cmd);

			Thread.sleep(5000);
			http.send("uname -a\n");
			//http.send("top\n");
			
			Thread.sleep(5000);

			http.close();
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test08ExecShellCommandsTLS() {
		// Run shell commands in cloud/cloud-connector-aes , Name = /loving_mayer @ 192.168.99.100:2376 (TLS)
		try {
			if (id == null) {
				id = "645399b14cd8e381c283954589bb82c37fc7540245b12c7b84c1da9a24ff3cea";
			}
			String[] cmds = new String[] {"date", "uname -a", "ls -l", "top"};
			LOGD("Executing commands in " + node + " id:" + id + " Cmds:" + Arrays.toString(cmds));
			
			for (int i = 0; i < cmds.length; i++) {
				String stdout = Docker.execShellCommand(node, id, cmds[i]);
				LOGD(stdout);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
}
