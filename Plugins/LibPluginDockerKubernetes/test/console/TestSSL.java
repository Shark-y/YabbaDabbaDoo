package console;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.security.SecurityTool;
import com.cloud.core.w3.WebClient;

public class TestSSL {

	static void testSSL1 () throws Exception {
		// gives javax.net.ssl.SSLException: Unsupported record version Unknown-0.0
		SecurityTool.disableClientSSLVerificationFromHttpsURLConnection("TLSv1.2");
		//SecurityTool.disableClientSSLVerificationFromHttpsURLConnection("SSL");
		WebClient wc = new WebClient("https://artifacthub.io/api/v1/packages/search?limit=60&offset=0&ts_query_web=");
		String json = wc.doGet();
		System.out.println(json);
	}
	
	static void testSSL2 () throws Exception {
		SecurityTool.disableClientSSLVerificationFromHttpsURLConnection("TLSv1.2");
		
		WebClient wc = new WebClient("https://192.168.40.84:31865/api/chartsvc/v1/charts");
		String json = wc.doGet();
		System.out.println(json);
	}

	public static void main(String[] args) {
		try {
			//testSSL1();
			//testSSL2();
			// remove duplicate keys
			String bad = "{\"level\":\"info\",\"cmd\":\"hub\",\"bytes_in\":\"\",\"bytes_out\":1868,\"host\":\"10.244.0.0\",\"method\":\"GET\",\"port\":\"\",\"status\":200,\"took\":0.47308,\"time\":\"2020-12-26T00:33:36Z\",\"time\":\"2020-12-26T00:33:36Z\",\"message\":\"/static/media/logo.png\"}";
			JSONObject o = new JSONObject(bad) {
				@Override
				public JSONObject putOnce(String key, Object value) throws JSONException {
			        if (key != null && value != null) {
			            if (this.opt(key) != null) {
			                System.out.println("Duplicate key \"" + key + "\"");
			            }
			            this.put(key, value);
			        }
			        return this;
				}
			};
			System.out.println(o);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
