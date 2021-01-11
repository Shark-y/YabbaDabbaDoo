package junit.docker;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.cloud.core.io.IOTools;
import com.cloud.core.w3.RestClient;
import com.cloud.docker.Docker;

public class MockObjects {

	static void LOGD(String text) {
		System.out.println("[DOCKER-MOCKOBJS] "  + text);
	}

	static RestClient rest = new RestClient();
	static public final String DOCKER_API_CP = "/configuration/api_docker.json";
	
	static String gcr_json_key = "/junit/resources/cloud-bots-key.json";
	
	// https://cloud.docker.com/repository/registry-1.docker.io/cloud/connector/tags
	static String dockerHubUser = "cloud";
	static String dockerHubPwd = "Thenewcti1";

	static void initRestAPIClient () throws JSONException, IOException {
		LOGD("Loading API descriptor from classpath " + DOCKER_API_CP);
		InputStream is = MockObjects.class.getResourceAsStream(DOCKER_API_CP);
		rest.load(is);
	}
	
	/**
	 * Load nodes/swarms file from the default location $HOME/.cloud/CloudADApter/{nodes,swarms}.json
	 * @throws JSONException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	static void loadNodesFile () throws JSONException, IOException, InstantiationException, IllegalAccessException {
		// load nodes file from $HOME/.cloud/
		String path = System.getProperty("user.home") + "\\.cloud\\CloudAdapter";
		LOGD("Loading nodes file from " + path);
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(Docker.CONFIG_BASEPATH, path);
		Docker.initialize(config);
		Docker.load();
	}
	
	/**
	 * <pre>*** ClientHello, TLSv1
main, READ: TLSv1 Alert, length = 2
main, RECV TLSv1 ALERT:  fatal, protocol_version
main, called closeSocket()
main, handling exception: javax.net.ssl.SSLException: Received fatal alert: protocol_version
main, called close()
main, called closeInternal(true)
javax.net.ssl.SSLException: Received fatal alert: protocol_version</pre>
	 */
	public static void fixSSLFatalProtocolVersion () {
		// USE TLS v0,v1,v2
		java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
	}
	
	/**
	 * Load a test GCR json key from the classpath @ /junit/resources/cloud-bots-key.json
	 * @return /junit/resources/cloud-bots-key.json
	 * @throws Exception
	 */
	public static String loadGCRJsonkeyFromCP () throws Exception {
		InputStream is = TestRegistries.class.getResourceAsStream(gcr_json_key);
		if ( is == null) {
			throw new IOException("Failed to load " + gcr_json_key + " fromm class path");
		}
		return IOTools.readFromStream(is);
	}

}
