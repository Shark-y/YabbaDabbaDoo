package console;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.docker.MockObjects;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import com.cloud.core.w3.WebClient;

public class TestYaml {

	static void LOGD(String text) {
		System.out.println("[YML] " + text);
	}
	
	static void test01 () throws FileNotFoundException {
		Yaml yml = new Yaml();

		//String path = "C:\\GITREPOS\\CloudServices-UnifiedContactCenter\\Workspace-Server\\CloudClusterManager\\etc\\monocular\\cloud_values.yml";

		Map<String, Object> obj = yml.load(TestYaml.class.getResourceAsStream("/resources/k8s/yml/pv-nfs.yml"));
		//Map<String, Object> obj = yml.load(new FileReader(path));
		//JSONObject root = new JSONObject();
		LOGD(obj.toString()); //root.toString());
	}
	
	/**
	 * Get a base url from a full url
	 * @param url Full url
	 * @return https://hub.helm.sh/api/chartsvc/v1/charts => https://hub.helm.sh, https://192.168.40.84:32543/api/chartsvc/v1/charts => https://192.168.40.84:32543
	 * @throws MalformedURLException
	 */
	public static String getBaseUrl (String url) throws MalformedURLException {
		URL u = new URL(url);
		return u.getProtocol() + "://" + u.getHost() + ( u.getPort() > 0 ? ":" + u.getPort() : "");
	}

	
	static String apiServer = junit.kubernetes.MockObjects.apiServerN208;
	static String accessToken = junit.kubernetes.MockObjects.accessTokenN208;
	static WebClient wc = new WebClient();
	static Map<String, String> hdrs = new HashMap<String, String>();

	static void init () {
		// javax.net.ssl.SSLException: Received fatal alert: protocol_version
		MockObjects.fixSSLFatalProtocolVersion();
	}
	
	static void test02GetSecrets () throws FileNotFoundException {
		try {
			hdrs.put("Authorization", "Bearer " + accessToken);
			//String url = apiServer + "/api/v1/namespaces/default/secrets";
			String url = apiServer + "api/v1/secrets";
			LOGD("Get secrets api from " + url);

			wc.setUrl(url);
			wc.setDebug(true);
			wc.logToStdOut(true);
			
			String out = wc.doGet(hdrs);

			/* {  "kind": "APIVersions",  "versions": [    "v1"  ],  "serverAddressByClientCIDRs": [  {  "clientCIDR": "0.0.0.0/0",  "serverAddress": "192.168.42.59:6443" } ]}*/
			LOGD("Get SECRETS api response " + out.length());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	public static void main(String[] args) {
		try {
//			System.out.println(getBaseUrl("https://hub.helm.sh/api/chartsvc/v1/charts"));
//			System.out.println(getBaseUrl("https://192.168.40.84:32543/api/chartsvc/v1/charts"));
			init();
			
			//test01();
			long t0 = System.currentTimeMillis();
			test02GetSecrets();
			long t1 = System.currentTimeMillis();
			LOGD("Get Secrets time: " + (t1-t0) + " ms.");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
