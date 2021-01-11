package junit.docker;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.docker.DockerHttpHijack;
import com.cloud.ssh.StreamIO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDockerImageAsync {

	static void LOGD(String text) {
		System.out.println("[DOCKER-IMAGE-ASYNC] "  + text);
	}

	static RestClient rest;

//	String keyStorePath = "C:\\Users\\vsilva\\.docker\\machine\\certs\\docker.jks"; 
//	String keyStorePassword = "certpass";
	static String keyStorePath = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node1.jks"; 
	static String keyStorePassword = "password";
	
	static String url = "https://192.168.99.100:2376/";

	// https://docs.docker.com/engine/api/v1.24/#32-images
	// https://hub.docker.com/_/busybox
	// Cloud Connector 437MB docker hub
	//static String image = "cloud/cloud-connector-aes";	
	static String image = "nginx:latest";	// 120 MB
	static String tag = "latest";	

	@BeforeClass
	public static void init() {
		rest = new RestClient();
	}

	/**
	 * 
	 */
	@Test
	public void test01ImageDownloadFromHubAsync() {
		try {
			// https://docs.docker.com/engine/api/v1.24/#32-images
			// POST /v1.24/images/create?fromImage=busybox&tag=latest HTTP/1.1
			String Url =  url + "images/create?fromImage=" + image + "&tag=latest";
			String payload = "";
			//String payload = "{ \"Detach\": false, \"Tty\": true}";

			LOGD("Async Image download url " + Url + " Payload " + payload + " Image: " + image);

			//final DisplayJSONMessagesStream 
			DockerHttpHijack http = new DockerHttpHijack(new URI(Url));
			http.setSSLParams("TLS", keyStorePath, keyStorePassword, true);
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type",  CoreTypes.CONTENT_TYPE_JSON);
			
			http.post(headers, payload, false);

			// 400MB ~ 1min - stdout to System.out
			http.pipeStdout(new StreamIO.OutputSink() {
				@Override
				public PrintStream getPrintStream() {
					return null; //System.out;
				}

				@Override
				public boolean closeAfterWrite() {
					return false;
				}

				@Override
				public void onChunkReceived(byte[] chunk) throws IOException {
					//https://github.com/moby/moby/blob/master/pkg/jsonmessage/jsonmessage.go#L281
					String buf = new String(chunk, CoreTypes.CHARSET_UTF8);
					String json = buf.contains("{") ? buf.substring(buf.indexOf("{"), buf.lastIndexOf("}") + 1 ) : "";
					if ( json.isEmpty()) {
						return;
					}
					//System.out.println("CHUNK: " + buf + "|");
					System.out.println("JSON:" + json);
				}

				@Override
				public boolean receiveChunks() {
					return true;
				}
			} );

			// wait for download
			/* STDOUT Format LEN1(HEX)\n<JSON1>\nLEN2(HEX)\n<JSON2>...\n0s
			47
			{"status":"Pulling fs layer","progressDetail":{},"id":"f7e2b70d04ae"}

			47
			{"status":"Pulling fs layer","progressDetail":{},"id":"08dd01e3f3ac"}

			47
			{"status":"Pulling fs layer","progressDetail":{},"id":"d9ef3a1eb792"}

			b1
			{"status":"Downloading","progressDetail":{"current":203,"total":203},"progress":"[==================================================\u003e]     203B/203B","id":"d9ef3a1eb792"}

			49
			{"status":"Verifying Checksum","progressDetail":{},"id":"d9ef3a1eb792"}

			48
			{"status":"Download complete","progressDetail":{},"id":"d9ef3a1eb792"}

			bb
			{"status":"Downloading","progressDetail":{"current":228503,"total":22496034},"progress":"[\u003e                                                  ]  228.5kB/22.5MB","id":"f7e2b70d04ae"}

			bc
			{"status":"Downloading","progressDetail":{"current":228503,"total":22262142},"progress":"[\u003e                                                  ]  228.5kB/22.26MB","id":"08dd01e3f3ac"}

			bb
			{"status":"Downloading","progressDetail":{"current":687368,"total":22496034},"progress":"[=\u003e                                                 ]  687.4kB/22.5MB","id":"f7e2b70d04ae"}

			bc
			{"status":"Downloading","progressDetail":{"current":1144814,"total":22496034},"progress":"[==\u003e                                                ]  1.145MB/22.5MB","id":"f7e2b70d04ae"}

			bc
			{"status":"Downloading","progressDetail":{"current":457992,"total":22262142},"progress":"[=\u003e                                                 ]    458kB/22.26MB","id":"08dd01e3f3ac"}

			bc
			{"status":"Downloading","progressDetail":{"current":1603566,"total":22496034},"progress":"[===\u003e                                               ]  1.604MB/22.5MB","id":"f7e2b70d04ae"}

			bc
			{"status":"Downloading","progressDetail":{"current":687368,"total":22262142},"progress":"[=\u003e                                                 ]  687.4kB/22.26MB","id":"08dd01e3f3ac"}

			5e
			{"status":"Digest: sha256:98efe605f61725fd817ea69521b0eeb32bef007af0e3d0aeb6258c6e6fe7fc1a"}

			3e
			{"status":"Status: Downloaded newer image for nginx:latest"}

			0
			*/ // END STDOUT
			
			LOGD("Async Image download waiting ...");
			Thread.sleep(20000);
			
			// close socket
			http.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
	
	/**
	 * Test image download from a file
	 */
	@Test
	public void test03ImageDownloadFromFile() {
		try {
			//FileReader fr = new File
			InputStreamReader isr = new InputStreamReader(TestDockerImageAsync.class.getResourceAsStream("nginx-download-stdout.txt"));
			BufferedReader br = new BufferedReader(isr);
			String line = null ;
			do {
				line = br.readLine();
				System.out.println(line);
			}
			while ( line != null);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	
	
}
