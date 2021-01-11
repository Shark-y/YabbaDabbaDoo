package junit.kubernetes;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.docker.MockObjects;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.yaml.snakeyaml.Yaml;

import com.cloud.core.io.IOTools;
import com.cloud.core.security.EncryptionTool;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestClient.HTTPDestination;
import com.cloud.core.w3.WebClient;

/*
 * Cluster name	Server
kubernetes	https://192.168.42.59:6443
Cluster      kubernetes
API server   https://192.168.42.59:6443
Access Token eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tdzQycnMiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjMxYzNlNzdmLTU2NGQtMTFlOS1hNGQwLTA4MDAyNzU4YTNiNSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.sXAOLZ2cRdinsE2N0VGTKm8x2IZjPs4Xu5MXb4odWYemG57lxGVec-4ox2DYcntPlihtNTbhz5JPuxlxi8zihHGLWc9bo8k2icV8Lhd6hVw9wS2IaECHufQEiVeMWcJH0vBShynrL_XZOTcRm292fScA0ounZuX8uqSppjLwQfEF9JGMk_NCFLaDrE93ErHll7eMY-Bo_jDojd3GIfagvF54CkfPVrGPcpJAj5yvd4V--FKDx4tRinh9WTNXPPVsDjxemDkS01sxA15P1n98qtvJwZApwJssNhnpHbhhqRtEgwkNwN9kH2wMNuEQMKwkD_NgzdzUW8BczgM0Tbi1fQ

# Explore the API with TOKEN
curl -X GET $APISERVER/api --header "Authorization: Bearer $TOKEN" --insecure
{
  "kind": "APIVersions",
  "versions": [
    "v1"
  ],
  "serverAddressByClientCIDRs": [
    {
      "clientCIDR": "0.0.0.0/0",
      "serverAddress": "192.168.42.59:6443"
    }
  ]
}
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestK8SConfigStorageApis {

	static void LOGD(String text) {
		System.out.println("[K8S-WORKLOAD] "  + text);
	}

//	static String apiServer = junit.kubernetes.MockObjects.apiServer;
//	static String accessToken = junit.kubernetes.MockObjects.accessToken;
	static String apiServer = junit.kubernetes.MockObjects.apiServerN208;
	static String accessToken = junit.kubernetes.MockObjects.accessTokenN208;
	
	static WebClient wc = new WebClient();
	static Map<String, String> hdrs = new HashMap<String, String>();
	
	static RestClient client = new RestClient();
	static HTTPDestination dest ;
	static Map<String, Object> params = new HashMap<String, Object>();
	
	@BeforeClass
	public static void init() {
		hdrs.put("Authorization", "Bearer " + accessToken);

		wc.setVerbosity(true);
		wc.logToStdOut(true);
		
		try {
			//dest = HTTPDestination.create(apiServer, null, null);
			params.put("TOKEN", accessToken);
			client.load(TestK8SConfigStorageApis.class.getResourceAsStream("/configuration/api_k8s.json"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		// javax.net.ssl.SSLException: Received fatal alert: protocol_version
		MockObjects.fixSSLFatalProtocolVersion();
	}
	
	@Test
	public void test00GetSecrets () {
		try {
			//String url = apiServer + "/api/v1/namespaces/default/secrets";
			String url = apiServer + "api/v1/secrets";
			LOGD("Get secrets api from " + url);

			wc.setUrl(url);
			String out = wc.doGet(hdrs);

			/* {  "kind": "APIVersions",  "versions": [    "v1"  ],  "serverAddressByClientCIDRs": [  {  "clientCIDR": "0.0.0.0/0",  "serverAddress": "192.168.42.59:6443" } ]}*/
			LOGD("Get SECRETS api response " + out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test01CreateSecret () {
		try {
			// create
			String raw = IOTools.readFromStream(TestK8SConfigStorageApis.class.getResourceAsStream("/resources/k8s/secret.json"));
			String namespace = "default";
			String name = "foo";
			
			JSONObject data = new JSONObject();
			data.put(".dockerconfigjson", "eyJhdXRocyI6eyJodHRwczovL3VzLmdjci5pbyI6eyJ1c2VybmFtZSI6Il9qc29uX2tleSIsInBhc3N3b3JkIjoie1xuICBcInR5cGVcIjogXCJzZXJ2aWNlX2FjY291bnRcIixcbiAgXCJwcm9qZWN0X2lkXCI6IFwiYzFhcy1ib3RzXCIsXG4gIFwicHJpdmF0ZV9rZXlfaWRcIjogXCJjYWI5N2E1Mzc2Nzk1MWExMjIzMGE0NjNhMzVhNDY2NjJmYzgxYmY4XCIsXG4gIFwicHJpdmF0ZV9rZXlcIjogXCItLS0tLUJFR0lOIFBSSVZBVEUgS0VZLS0tLS1cXG5NSUlFdlFJQkFEQU5CZ2txaGtpRzl3MEJBUUVGQUFTQ0JLY3dnZ1NqQWdFQUFvSUJBUURHSktYd1ZBOUR6TkNxXFxubUY5MUpLMVBOdWxESWZ5OFFNWjZNT2hkZ3I0NkE4cmFrSENUYjJjc0VTMDR6dUVDUFVQaVNESEFpK2lFTHFBYVxcbnRXWUJ5bHlwMWdQbmlkYXBCSzZnV0s3M0c4TmRHYUcydzhGVUhoNVlmeDVIVk5sdUJOZnFHWHVVM0tQV1ZXSDlcXG5GMHBqQVVaYjBnbGlRVWRrL1h2Tk11Zitha0M0N0xMNDIvWkFENHdvOVIwZExZVThSVW9nVlo0ME1yWmZtUldYXFxuTTcyVEcxZW5QRUxSR1E3OGZmSzhTeGo4U2VZbHpPRnEwK1psL2t1aEhzQ1NZcXVTakRBdFNhV2dCenJJRnl2clxcblI4Z2tsOXUzckI1QnkzdFRON1IyY2RGM3A4aklFbk8rMFhFc282Z0x2UmtXUEVmUGYrQXltQ0xYTnc1TmxITHNcXG5FcU0vbERPN0FnTUJBQUVDZ2dFQUVxblBDNFVQYndaeXZTN2ZCdXl1ZW1Da3RoTVVlREx0dndLdVVHaUpQSE93XFxuL3NkT0kwVUNCYytVWDk1MHFUNVdENUZYUmxpblRSUUxOcGpSZytlb3RNS1kyWTFOTDl4bUNtcHdDSXlQNlV3SlxcbnhHcEo0bjkzd2tRdStPOFFEK0hhNkl6LzYvU2daZ2JpMVBFRnE2K3FEZW9Kd2p4OTJoQmFRZWpDbTE4c3UzNDlcXG5sMzdPSkFxUnV2d3pWUGJoT1FaZkV5eGRGS2NaZC80Zm8rUzdhS3lQOWl3aytDNmVRZ3lSdnlrMkdZWDNwdVc4XFxuZUxOa2F2YlZrUUJBL3pFR01wR3hJOXh4RUsxMGxndWlpSTlCTHVLeUtLRG93dzIxQ2dYZytUZzVFU3NpZjd3UlxcbnRFOC9wclNJZTY0bWJKeS9kS29YT0lLbUEwNjFLVHhGZ2pDMmNwekVhUUtCZ1FEdlk0NGo5TWhLMjBqTCsrWlJcXG5id2dzQVVaWUV3c2xaeTZXRjVMai9SQ3k2MUpnb3pmanpVRGNhcUdxQ1c5Ui9xWktpQ3UrZDVLUDY3OFFqSUFoXFxuL2p4MHRSeHVEeGlNQlhMeTJOdG44ZEZMU1pOWFo2WWQ2OUtDOTVSbEhZWkl1Rm43RG90by91V3RRYUlGekw5ZVxcblJKOEJJaEREU05BczFnbjVjQTdNRlQ3NHh3S0JnUURUNUduK3pIam52cVNNZDlHMUhLd2hsUkZXUXRKTUNyaTZcXG5PSDJCRWYvVnJrUHBOenBGZmFyVDVNMXJXZ2Zia2t1bUs0YmtNY3oxNWlhcVNCSldWcERhakxqYVN3aENZRU5uXFxuNG5ORlJ5U1RzVndiL2xTalN5Z3dsRUY3RHRwTzl4clNmN2FMVDhnM0ZFLzg1eEgvVnM5SzNURjNIUkxNdTkzM1xcblZGd1FiRmlCYlFLQmdDaDBOM29HREs3eHZheVRCZ251N0grYk80cjR0T0orUEZZcTU2elZnRFBzSm9Da05IYXZcXG5vZTFkcTdJelkvZUVSQS9HVVRZZnVNblBlZnRPOHZ4TG5XWFBrVjFiK2JiMzJNUUhONVNxUGNjdVNOTDFkaElLXFxuWHdtNDB2Ly9iRWp2dG0wQnVUZG1FK1FpZWtIbXB0UkVYbVp2bmt1U0MzUDRPN2xHWVluRmNWM1RBb0dCQUozcVxcbjd1QTU3YVh4bkZzZjlZZkFYYjBOaHdVOStkTTRibUpETmE0YkJ3dHV2Q2cwdzlZRWlXc3dhN1FsUGhQem5UT3pcXG4zdTEwL0NjTHB5ZGpYVjlCVnVXNjJRMi91K0VVTTBoYUtjU285K2FtMlZuM200YXp2Ymt1MVBwc29HRVhvc0xjXFxuOGVRenlxamFGNThKYTQxY1dsT1dOSVpzZ1pWYU1seGhYOWZSbDhpbEFvR0FmallRQlBsV0Z3bHhpWDE1V3JTZlxcbnZHRjFGbU9pcUV3eFhXblptSndDSDdBK0piMXY4K1htTHVJUFV3VGk3bzVsMWZJWGtoQXQxMVJVNmsyczhFcVpcXG5PdXpPSDdaNFlJQW5DSy9oanRtZGZFZ042U1RGaUpjb1VnaVk4ZXJtcEtpazczVlVzRzVWdkw5Mk9RTjVrYVNFXFxuZmNjTENBM2JmS29NSXpaNEsrbGk5Q0k9XFxuLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLVxcblwiLFxuICBcImNsaWVudF9lbWFpbFwiOiBcIm5hY3IyMDgtZ2NyLXVuaWZpZWRjb25zb2xlQGMxYXMtYm90cy5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbVwiLFxuICBcImNsaWVudF9pZFwiOiBcIjEwNzExNTk3MzY0MDU4MTgxOTA2OFwiLFxuICBcImF1dGhfdXJpXCI6IFwiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tL28vb2F1dGgyL2F1dGhcIixcbiAgXCJ0b2tlbl91cmlcIjogXCJodHRwczovL29hdXRoMi5nb29nbGVhcGlzLmNvbS90b2tlblwiLFxuICBcImF1dGhfcHJvdmlkZXJfeDUwOV9jZXJ0X3VybFwiOiBcImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL29hdXRoMi92MS9jZXJ0c1wiLFxuICBcImNsaWVudF94NTA5X2NlcnRfdXJsXCI6IFwiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9uYWNyMjA4LWdjci11bmlmaWVkY29uc29sZSU0MGMxYXMtYm90cy5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbVwiXG59IiwiYXV0aCI6IlgycHpiMjVmYTJWNU9uc0tJQ0FpZEhsd1pTSTZJQ0p6WlhKMmFXTmxYMkZqWTI5MWJuUWlMQW9nSUNKd2NtOXFaV04wWDJsa0lqb2dJbU14WVhNdFltOTBjeUlzQ2lBZ0luQnlhWFpoZEdWZmEyVjVYMmxrSWpvZ0ltTmhZamszWVRVek56WTNPVFV4WVRFeU1qTXdZVFEyTTJFek5XRTBOalkyTW1aak9ERmlaamdpTEFvZ0lDSndjbWwyWVhSbFgydGxlU0k2SUNJdExTMHRMVUpGUjBsT0lGQlNTVlpCVkVVZ1MwVlpMUzB0TFMxY2JrMUpTVVYyVVVsQ1FVUkJUa0puYTNGb2EybEhPWGN3UWtGUlJVWkJRVk5EUWt0amQyZG5VMnBCWjBWQlFXOUpRa0ZSUkVkS1MxaDNWa0U1UkhwT1EzRmNibTFHT1RGS1N6RlFUblZzUkVsbWVUaFJUVm8yVFU5b1pHZHlORFpCT0hKaGEwaERWR0l5WTNORlV6QTBlblZGUTFCVlVHbFRSRWhCYVN0cFJVeHhRV0ZjYm5SWFdVSjViSGx3TVdkUWJtbGtZWEJDU3pablYwczNNMGM0VG1SSFlVY3lkemhHVlVob05WbG1lRFZJVms1c2RVSk9abkZIV0hWVk0wdFFWMVpYU0RsY2JrWXdjR3BCVlZwaU1HZHNhVkZWWkdzdldIWk9UWFZtSzJGclF6UTNURXcwTWk5YVFVUTBkMjg1VWpCa1RGbFZPRkpWYjJkV1dqUXdUWEphWm0xU1YxaGNiazAzTWxSSE1XVnVVRVZNVWtkUk56aG1aa3M0VTNocU9GTmxXV3g2VDBaeE1DdGFiQzlyZFdoSWMwTlRXWEYxVTJwRVFYUlRZVmRuUW5weVNVWjVkbkpjYmxJNFoydHNPWFV6Y2tJMVFua3pkRlJPTjFJeVkyUkdNM0E0YWtsRmJrOHJNRmhGYzI4MloweDJVbXRYVUVWbVVHWXJRWGx0UTB4WVRuYzFUbXhJVEhOY2JrVnhUUzlzUkU4M1FXZE5Ra0ZCUlVOblowVkJSWEZ1VUVNMFZWQmlkMXA1ZGxNM1prSjFlWFZsYlVOcmRHaE5WV1ZFVEhSMmQwdDFWVWRwU2xCSVQzZGNiaTl6WkU5Sk1GVkRRbU1yVlZnNU5UQnhWRFZYUkRWR1dGSnNhVzVVVWxGTVRuQnFVbWNyWlc5MFRVdFpNbGt4VGt3NWVHMURiWEIzUTBsNVVEWlZkMHBjYm5oSGNFbzBiamt6ZDJ0UmRTdFBPRkZFSzBoaE5rbDZMell2VTJkYVoySnBNVkJGUm5FMkszRkVaVzlLZDJwNE9USm9RbUZSWldwRGJURTRjM1V6TkRsY2Jtd3pOMDlLUVhGU2RYWjNlbFpRWW1oUFVWcG1SWGw0WkVaTFkxcGtMelJtYnl0VE4yRkxlVkE1YVhkckswTTJaVkZuZVZKMmVXc3lSMWxZTTNCMVZ6aGNibVZNVG10aGRtSldhMUZDUVM5NlJVZE5jRWQ0U1RsNGVFVkxNVEJzWjNWcGFVazVRa3gxUzNsTFMwUnZkM2N5TVVObldHY3JWR2MxUlZOemFXWTNkMUpjYm5SRk9DOXdjbE5KWlRZMGJXSktlUzlrUzI5WVQwbExiVUV3TmpGTFZIaEdaMnBETW1Od2VrVmhVVXRDWjFGRWRsazBOR281VFdoTE1qQnFUQ3NyV2xKY2JtSjNaM05CVlZwWlJYZHpiRnA1TmxkR05VeHFMMUpEZVRZeFNtZHZlbVpxZWxWRVkyRnhSM0ZEVnpsU0wzRmFTMmxEZFN0a05VdFFOamM0VVdwSlFXaGNiaTlxZURCMFVuaDFSSGhwVFVKWVRIa3lUblJ1T0dSR1RGTmFUbGhhTmxsa05qbExRemsxVW14SVdWcEpkVVp1TjBSdmRHOHZkVmQwVVdGSlJucE1PV1ZjYmxKS09FSkphRVJFVTA1QmN6Rm5ialZqUVRkTlJsUTNOSGgzUzBKblVVUlVOVWR1SzNwSWFtNTJjVk5OWkRsSE1VaExkMmhzVWtaWFVYUktUVU55YVRaY2JrOUlNa0pGWmk5V2NtdFFjRTU2Y0VabVlYSlVOVTB4Y2xkblptSnJhM1Z0U3pSaWEwMWplakUxYVdGeFUwSktWMVp3UkdGcVRHcGhVM2RvUTFsRlRtNWNialJ1VGtaU2VWTlVjMVozWWk5c1UycFRlV2QzYkVWR04wUjBjRTg1ZUhKVFpqZGhURlE0WnpOR1JTODROWGhJTDFaek9Vc3pWRVl6U0ZKTVRYVTVNek5jYmxaR2QxRmlSbWxDWWxGTFFtZERhREJPTTI5SFJFczNlSFpoZVZSQ1oyNTFOMGdyWWs4MGNqUjBUMG9yVUVaWmNUVTJlbFpuUkZCelNtOURhMDVJWVhaY2JtOWxNV1J4TjBsNldTOWxSVkpCTDBkVlZGbG1kVTF1VUdWbWRFODRkbmhNYmxkWVVHdFdNV0lyWW1Jek1rMVJTRTQxVTNGUVkyTjFVMDVNTVdSb1NVdGNibGgzYlRRd2RpOHZZa1ZxZG5SdE1FSjFWR1J0UlN0UmFXVnJTRzF3ZEZKRldHMWFkbTVyZFZORE0xQTBUemRzUjFsWmJrWmpWak5VUVc5SFFrRktNM0ZjYmpkMVFUVTNZVmg0YmtaelpqbFpaa0ZZWWpCT2FIZFZPU3RrVFRSaWJVcEVUbUUwWWtKM2RIVjJRMmN3ZHpsWlJXbFhjM2RoTjFGc1VHaFFlbTVVVDNwY2JqTjFNVEF2UTJOTWNIbGthbGhXT1VKV2RWYzJNbEV5TDNVclJWVk5NR2hoUzJOVGJ6a3JZVzB5Vm00emJUUmhlblppYTNVeFVIQnpiMGRGV0c5elRHTmNiamhsVVhwNWNXcGhSalU0U21FME1XTlhiRTlYVGtsYWMyZGFWbUZOYkhob1dEbG1VbXc0YVd4QmIwZEJabXBaVVVKUWJGZEdkMng0YVZneE5WZHlVMlpjYm5aSFJqRkdiVTlwY1VWM2VGaFhibHB0U25kRFNEZEJLMHBpTVhZNEsxaHRUSFZKVUZWM1ZHazNielZzTVdaSldHdG9RWFF4TVZKVk5tc3ljemhGY1ZwY2JrOTFlazlJTjFvMFdVbEJia05MTDJocWRHMWtaa1ZuVGpaVFZFWnBTbU52VldkcFdUaGxjbTF3UzJsck56TldWWE5ITlZaMlREa3lUMUZPTld0aFUwVmNibVpqWTB4RFFUTmlaa3R2VFVsNldqUkxLMnhwT1VOSlBWeHVMUzB0TFMxRlRrUWdVRkpKVmtGVVJTQkxSVmt0TFMwdExWeHVJaXdLSUNBaVkyeHBaVzUwWDJWdFlXbHNJam9nSW01aFkzSXlNRGd0WjJOeUxYVnVhV1pwWldSamIyNXpiMnhsUUdNeFlYTXRZbTkwY3k1cFlXMHVaM05sY25acFkyVmhZMk52ZFc1MExtTnZiU0lzQ2lBZ0ltTnNhV1Z1ZEY5cFpDSTZJQ0l4TURjeE1UVTVOek0yTkRBMU9ERTRNVGt3TmpnaUxBb2dJQ0poZFhSb1gzVnlhU0k2SUNKb2RIUndjem92TDJGalkyOTFiblJ6TG1kdmIyZHNaUzVqYjIwdmJ5OXZZWFYwYURJdllYVjBhQ0lzQ2lBZ0luUnZhMlZ1WDNWeWFTSTZJQ0pvZEhSd2N6b3ZMMjloZFhSb01pNW5iMjluYkdWaGNHbHpMbU52YlM5MGIydGxiaUlzQ2lBZ0ltRjFkR2hmY0hKdmRtbGtaWEpmZURVd09WOWpaWEowWDNWeWJDSTZJQ0pvZEhSd2N6b3ZMM2QzZHk1bmIyOW5iR1ZoY0dsekxtTnZiUzl2WVhWMGFESXZkakV2WTJWeWRITWlMQW9nSUNKamJHbGxiblJmZURVd09WOWpaWEowWDNWeWJDSTZJQ0pvZEhSd2N6b3ZMM2QzZHk1bmIyOW5iR1ZoY0dsekxtTnZiUzl5YjJKdmRDOTJNUzl0WlhSaFpHRjBZUzk0TlRBNUwyNWhZM0l5TURndFoyTnlMWFZ1YVdacFpXUmpiMjV6YjJ4bEpUUXdZekZoY3kxaWIzUnpMbWxoYlM1bmMyVnlkbWxqWldGalkyOTFiblF1WTI5dElncDkifX19");
			
			params.put("NAMESPACE", namespace);
			params.put("NAME", name);
			params.put("DATECREATED", IOTools.formatDateRFC339(new Date()));
			params.put("TYPE", "kubernetes.io/dockerconfigjson");
			params.put("UID", EncryptionTool.UUID());
			params.put("DATAOBJ", data);
			
			String content = RestClient.replaceParams(params, raw);

			dest = HTTPDestination.create(apiServer, content, CoreTypes.CONTENT_TYPE_JSON, null, null);
			dest.setDebug(true);
			
			LOGD("== CREATE SECRET " + name + " @ " + namespace);
			LOGD("CREATE SECRET PAYLOAD: " + content);
			
			JSONObject root =  (JSONObject)client.invoke(dest, "CreateSecret", params);

			// {"metadata":{"uid":"6bbe941b-6910-11ea-a941-000c297a5447","resourceVersion":"34775959","name":"foo","namespace":"default","creationTimestamp":"2020-03-18T12:03:03Z","selfLink":"/api/v1/namespaces/default/secrets/foo"},"apiVersion":"v1","data":{".dockerconfigjson":"eyJhdXRocyI6eyJodHRwczovL3VzLmdjci5pbyI6eyJ1c2VybmFtZSI6Il9qc29uX2tleSIsInBhc3N3b3JkIjoie1xuICBcInR5cGVcIjogXCJzZXJ2aWNlX2FjY291bnRcIixcbiAgXCJwcm9qZWN0X2lkXCI6IFwiYzFhcy1ib3RzXCIsXG4gIFwicHJpdmF0ZV9rZXlfaWRcIjogXCJjYWI5N2E1Mzc2Nzk1MWExMjIzMGE0NjNhMzVhNDY2NjJmYzgxYmY4XCIsXG4gIFwicHJpdmF0ZV9rZXlcIjogXCItLS0tLUJFR0lOIFBSSVZBVEUgS0VZLS0tLS1cXG5NSUlFdlFJQkFEQU5CZ2txaGtpRzl3MEJBUUVGQUFTQ0JLY3dnZ1NqQWdFQUFvSUJBUURHSktYd1ZBOUR6TkNxXFxubUY5MUpLMVBOdWxESWZ5OFFNWjZNT2hkZ3I0NkE4cmFrSENUYjJjc0VTMDR6dUVDUFVQaVNESEFpK2lFTHFBYVxcbnRXWUJ5bHlwMWdQbmlkYXBCSzZnV0s3M0c4TmRHYUcydzhGVUhoNVlmeDVIVk5sdUJOZnFHWHVVM0tQV1ZXSDlcXG5GMHBqQVVaYjBnbGlRVWRrL1h2Tk11Zitha0M0N0xMNDIvWkFENHdvOVIwZExZVThSVW9nVlo0ME1yWmZtUldYXFxuTTcyVEcxZW5QRUxSR1E3OGZmSzhTeGo4U2VZbHpPRnEwK1psL2t1aEhzQ1NZcXVTakRBdFNhV2dCenJJRnl2clxcblI4Z2tsOXUzckI1QnkzdFRON1IyY2RGM3A4aklFbk8rMFhFc282Z0x2UmtXUEVmUGYrQXltQ0xYTnc1TmxITHNcXG5FcU0vbERPN0FnTUJBQUVDZ2dFQUVxblBDNFVQYndaeXZTN2ZCdXl1ZW1Da3RoTVVlREx0dndLdVVHaUpQSE93XFxuL3NkT0kwVUNCYytVWDk1MHFUNVdENUZYUmxpblRSUUxOcGpSZytlb3RNS1kyWTFOTDl4bUNtcHdDSXlQNlV3SlxcbnhHcEo0bjkzd2tRdStPOFFEK0hhNkl6LzYvU2daZ2JpMVBFRnE2K3FEZW9Kd2p4OTJoQmFRZWpDbTE4c3UzNDlcXG5sMzdPSkFxUnV2d3pWUGJoT1FaZkV5eGRGS2NaZC80Zm8rUzdhS3lQOWl3aytDNmVRZ3lSdnlrMkdZWDNwdVc4XFxuZUxOa2F2YlZrUUJBL3pFR01wR3hJOXh4RUsxMGxndWlpSTlCTHVLeUtLRG93dzIxQ2dYZytUZzVFU3NpZjd3UlxcbnRFOC9wclNJZTY0bWJKeS9kS29YT0lLbUEwNjFLVHhGZ2pDMmNwekVhUUtCZ1FEdlk0NGo5TWhLMjBqTCsrWlJcXG5id2dzQVVaWUV3c2xaeTZXRjVMai9SQ3k2MUpnb3pmanpVRGNhcUdxQ1c5Ui9xWktpQ3UrZDVLUDY3OFFqSUFoXFxuL2p4MHRSeHVEeGlNQlhMeTJOdG44ZEZMU1pOWFo2WWQ2OUtDOTVSbEhZWkl1Rm43RG90by91V3RRYUlGekw5ZVxcblJKOEJJaEREU05BczFnbjVjQTdNRlQ3NHh3S0JnUURUNUduK3pIam52cVNNZDlHMUhLd2hsUkZXUXRKTUNyaTZcXG5PSDJCRWYvVnJrUHBOenBGZmFyVDVNMXJXZ2Zia2t1bUs0YmtNY3oxNWlhcVNCSldWcERhakxqYVN3aENZRU5uXFxuNG5ORlJ5U1RzVndiL2xTalN5Z3dsRUY3RHRwTzl4clNmN2FMVDhnM0ZFLzg1eEgvVnM5SzNURjNIUkxNdTkzM1xcblZGd1FiRmlCYlFLQmdDaDBOM29HREs3eHZheVRCZ251N0grYk80cjR0T0orUEZZcTU2elZnRFBzSm9Da05IYXZcXG5vZTFkcTdJelkvZUVSQS9HVVRZZnVNblBlZnRPOHZ4TG5XWFBrVjFiK2JiMzJNUUhONVNxUGNjdVNOTDFkaElLXFxuWHdtNDB2Ly9iRWp2dG0wQnVUZG1FK1FpZWtIbXB0UkVYbVp2bmt1U0MzUDRPN2xHWVluRmNWM1RBb0dCQUozcVxcbjd1QTU3YVh4bkZzZjlZZkFYYjBOaHdVOStkTTRibUpETmE0YkJ3dHV2Q2cwdzlZRWlXc3dhN1FsUGhQem5UT3pcXG4zdTEwL0NjTHB5ZGpYVjlCVnVXNjJRMi91K0VVTTBoYUtjU285K2FtMlZuM200YXp2Ymt1MVBwc29HRVhvc0xjXFxuOGVRenlxamFGNThKYTQxY1dsT1dOSVpzZ1pWYU1seGhYOWZSbDhpbEFvR0FmallRQlBsV0Z3bHhpWDE1V3JTZlxcbnZHRjFGbU9pcUV3eFhXblptSndDSDdBK0piMXY4K1htTHVJUFV3VGk3bzVsMWZJWGtoQXQxMVJVNmsyczhFcVpcXG5PdXpPSDdaNFlJQW5DSy9oanRtZGZFZ042U1RGaUpjb1VnaVk4ZXJtcEtpazczVlVzRzVWdkw5Mk9RTjVrYVNFXFxuZmNjTENBM2JmS29NSXpaNEsrbGk5Q0k9XFxuLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLVxcblwiLFxuICBcImNsaWVudF9lbWFpbFwiOiBcIm5hY3IyMDgtZ2NyLXVuaWZpZWRjb25zb2xlQGMxYXMtYm90cy5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbVwiLFxuICBcImNsaWVudF9pZFwiOiBcIjEwNzExNTk3MzY0MDU4MTgxOTA2OFwiLFxuICBcImF1dGhfdXJpXCI6IFwiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tL28vb2F1dGgyL2F1dGhcIixcbiAgXCJ0b2tlbl91cmlcIjogXCJodHRwczovL29hdXRoMi5nb29nbGVhcGlzLmNvbS90b2tlblwiLFxuICBcImF1dGhfcHJvdmlkZXJfeDUwOV9jZXJ0X3VybFwiOiBcImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL29hdXRoMi92MS9jZXJ0c1wiLFxuICBcImNsaWVudF94NTA5X2NlcnRfdXJsXCI6IFwiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9uYWNyMjA4LWdjci11bmlmaWVkY29uc29sZSU0MGMxYXMtYm90cy5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbVwiXG59IiwiYXV0aCI6IlgycHpiMjVmYTJWNU9uc0tJQ0FpZEhsd1pTSTZJQ0p6WlhKMmFXTmxYMkZqWTI5MWJuUWlMQW9nSUNKd2NtOXFaV04wWDJsa0lqb2dJbU14WVhNdFltOTBjeUlzQ2lBZ0luQnlhWFpoZEdWZmEyVjVYMmxrSWpvZ0ltTmhZamszWVRVek56WTNPVFV4WVRFeU1qTXdZVFEyTTJFek5XRTBOalkyTW1aak9ERmlaamdpTEFvZ0lDSndjbWwyWVhSbFgydGxlU0k2SUNJdExTMHRMVUpGUjBsT0lGQlNTVlpCVkVVZ1MwVlpMUzB0TFMxY2JrMUpTVVYyVVVsQ1FVUkJUa0puYTNGb2EybEhPWGN3UWtGUlJVWkJRVk5EUWt0amQyZG5VMnBCWjBWQlFXOUpRa0ZSUkVkS1MxaDNWa0U1UkhwT1EzRmNibTFHT1RGS1N6RlFUblZzUkVsbWVUaFJUVm8yVFU5b1pHZHlORFpCT0hKaGEwaERWR0l5WTNORlV6QTBlblZGUTFCVlVHbFRSRWhCYVN0cFJVeHhRV0ZjYm5SWFdVSjViSGx3TVdkUWJtbGtZWEJDU3pablYwczNNMGM0VG1SSFlVY3lkemhHVlVob05WbG1lRFZJVms1c2RVSk9abkZIV0hWVk0wdFFWMVpYU0RsY2JrWXdjR3BCVlZwaU1HZHNhVkZWWkdzdldIWk9UWFZtSzJGclF6UTNURXcwTWk5YVFVUTBkMjg1VWpCa1RGbFZPRkpWYjJkV1dqUXdUWEphWm0xU1YxaGNiazAzTWxSSE1XVnVVRVZNVWtkUk56aG1aa3M0VTNocU9GTmxXV3g2VDBaeE1DdGFiQzlyZFdoSWMwTlRXWEYxVTJwRVFYUlRZVmRuUW5weVNVWjVkbkpjYmxJNFoydHNPWFV6Y2tJMVFua3pkRlJPTjFJeVkyUkdNM0E0YWtsRmJrOHJNRmhGYzI4MloweDJVbXRYVUVWbVVHWXJRWGx0UTB4WVRuYzFUbXhJVEhOY2JrVnhUUzlzUkU4M1FXZE5Ra0ZCUlVOblowVkJSWEZ1VUVNMFZWQmlkMXA1ZGxNM1prSjFlWFZsYlVOcmRHaE5WV1ZFVEhSMmQwdDFWVWRwU2xCSVQzZGNiaTl6WkU5Sk1GVkRRbU1yVlZnNU5UQnhWRFZYUkRWR1dGSnNhVzVVVWxGTVRuQnFVbWNyWlc5MFRVdFpNbGt4VGt3NWVHMURiWEIzUTBsNVVEWlZkMHBjYm5oSGNFbzBiamt6ZDJ0UmRTdFBPRkZFSzBoaE5rbDZMell2VTJkYVoySnBNVkJGUm5FMkszRkVaVzlLZDJwNE9USm9RbUZSWldwRGJURTRjM1V6TkRsY2Jtd3pOMDlLUVhGU2RYWjNlbFpRWW1oUFVWcG1SWGw0WkVaTFkxcGtMelJtYnl0VE4yRkxlVkE1YVhkckswTTJaVkZuZVZKMmVXc3lSMWxZTTNCMVZ6aGNibVZNVG10aGRtSldhMUZDUVM5NlJVZE5jRWQ0U1RsNGVFVkxNVEJzWjNWcGFVazVRa3gxUzNsTFMwUnZkM2N5TVVObldHY3JWR2MxUlZOemFXWTNkMUpjYm5SRk9DOXdjbE5KWlRZMGJXSktlUzlrUzI5WVQwbExiVUV3TmpGTFZIaEdaMnBETW1Od2VrVmhVVXRDWjFGRWRsazBOR281VFdoTE1qQnFUQ3NyV2xKY2JtSjNaM05CVlZwWlJYZHpiRnA1TmxkR05VeHFMMUpEZVRZeFNtZHZlbVpxZWxWRVkyRnhSM0ZEVnpsU0wzRmFTMmxEZFN0a05VdFFOamM0VVdwSlFXaGNiaTlxZURCMFVuaDFSSGhwVFVKWVRIa3lUblJ1T0dSR1RGTmFUbGhhTmxsa05qbExRemsxVW14SVdWcEpkVVp1TjBSdmRHOHZkVmQwVVdGSlJucE1PV1ZjYmxKS09FSkphRVJFVTA1QmN6Rm5ialZqUVRkTlJsUTNOSGgzUzBKblVVUlVOVWR1SzNwSWFtNTJjVk5OWkRsSE1VaExkMmhzVWtaWFVYUktUVU55YVRaY2JrOUlNa0pGWmk5V2NtdFFjRTU2Y0VabVlYSlVOVTB4Y2xkblptSnJhM1Z0U3pSaWEwMWplakUxYVdGeFUwSktWMVp3UkdGcVRHcGhVM2RvUTFsRlRtNWNialJ1VGtaU2VWTlVjMVozWWk5c1UycFRlV2QzYkVWR04wUjBjRTg1ZUhKVFpqZGhURlE0WnpOR1JTODROWGhJTDFaek9Vc3pWRVl6U0ZKTVRYVTVNek5jYmxaR2QxRmlSbWxDWWxGTFFtZERhREJPTTI5SFJFczNlSFpoZVZSQ1oyNTFOMGdyWWs4MGNqUjBUMG9yVUVaWmNUVTJlbFpuUkZCelNtOURhMDVJWVhaY2JtOWxNV1J4TjBsNldTOWxSVkpCTDBkVlZGbG1kVTF1VUdWbWRFODRkbmhNYmxkWVVHdFdNV0lyWW1Jek1rMVJTRTQxVTNGUVkyTjFVMDVNTVdSb1NVdGNibGgzYlRRd2RpOHZZa1ZxZG5SdE1FSjFWR1J0UlN0UmFXVnJTRzF3ZEZKRldHMWFkbTVyZFZORE0xQTBUemRzUjFsWmJrWmpWak5VUVc5SFFrRktNM0ZjYmpkMVFUVTNZVmg0YmtaelpqbFpaa0ZZWWpCT2FIZFZPU3RrVFRSaWJVcEVUbUUwWWtKM2RIVjJRMmN3ZHpsWlJXbFhjM2RoTjFGc1VHaFFlbTVVVDNwY2JqTjFNVEF2UTJOTWNIbGthbGhXT1VKV2RWYzJNbEV5TDNVclJWVk5NR2hoUzJOVGJ6a3JZVzB5Vm00emJUUmhlblppYTNVeFVIQnpiMGRGV0c5elRHTmNiamhsVVhwNWNXcGhSalU0U21FME1XTlhiRTlYVGtsYWMyZGFWbUZOYkhob1dEbG1VbXc0YVd4QmIwZEJabXBaVVVKUWJGZEdkMng0YVZneE5WZHlVMlpjYm5aSFJqRkdiVTlwY1VWM2VGaFhibHB0U25kRFNEZEJLMHBpTVhZNEsxaHRUSFZKVUZWM1ZHazNielZzTVdaSldHdG9RWFF4TVZKVk5tc3ljemhGY1ZwY2JrOTFlazlJTjFvMFdVbEJia05MTDJocWRHMWtaa1ZuVGpaVFZFWnBTbU52VldkcFdUaGxjbTF3UzJsck56TldWWE5ITlZaMlREa3lUMUZPTld0aFUwVmNibVpqWTB4RFFUTmlaa3R2VFVsNldqUkxLMnhwT1VOSlBWeHVMUzB0TFMxRlRrUWdVRkpKVmtGVVJTQkxSVmt0TFMwdExWeHVJaXdLSUNBaVkyeHBaVzUwWDJWdFlXbHNJam9nSW01aFkzSXlNRGd0WjJOeUxYVnVhV1pwWldSamIyNXpiMnhsUUdNeFlYTXRZbTkwY3k1cFlXMHVaM05sY25acFkyVmhZMk52ZFc1MExtTnZiU0lzQ2lBZ0ltTnNhV1Z1ZEY5cFpDSTZJQ0l4TURjeE1UVTVOek0yTkRBMU9ERTRNVGt3TmpnaUxBb2dJQ0poZFhSb1gzVnlhU0k2SUNKb2RIUndjem92TDJGalkyOTFiblJ6TG1kdmIyZHNaUzVqYjIwdmJ5OXZZWFYwYURJdllYVjBhQ0lzQ2lBZ0luUnZhMlZ1WDNWeWFTSTZJQ0pvZEhSd2N6b3ZMMjloZFhSb01pNW5iMjluYkdWaGNHbHpMbU52YlM5MGIydGxiaUlzQ2lBZ0ltRjFkR2hmY0hKdmRtbGtaWEpmZURVd09WOWpaWEowWDNWeWJDSTZJQ0pvZEhSd2N6b3ZMM2QzZHk1bmIyOW5iR1ZoY0dsekxtTnZiUzl2WVhWMGFESXZkakV2WTJWeWRITWlMQW9nSUNKamJHbGxiblJmZURVd09WOWpaWEowWDNWeWJDSTZJQ0pvZEhSd2N6b3ZMM2QzZHk1bmIyOW5iR1ZoY0dsekxtTnZiUzl5YjJKdmRDOTJNUzl0WlhSaFpHRjBZUzk0TlRBNUwyNWhZM0l5TURndFoyTnlMWFZ1YVdacFpXUmpiMjV6YjJ4bEpUUXdZekZoY3kxaWIzUnpMbWxoYlM1bmMyVnlkbWxqWldGalkyOTFiblF1WTI5dElncDkifX19"},"kind":"Secret","type":"kubernetes.io/dockerconfigjson"}
			LOGD("CREATE SECRET RESPONSE: " + root);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test01DeleteSecret () {
		try {
			// Delete
			String namespace = "default";
			String name = "foo";

			dest = HTTPDestination.create(apiServer, null, CoreTypes.CONTENT_TYPE_JSON, null, null);
			dest.setDebug(true);
			
			LOGD("== DELETE SECRET " + name + " @ " + namespace);
			
			JSONObject root =  (JSONObject)client.invoke(dest, "DeleteSecret", params);

			// {"metadata":{},"apiVersion":"v1","kind":"Status","details":{"uid":"6bbe941b-6910-11ea-a941-000c297a5447","kind":"secrets","name":"foo"},"status":"Success"}
			LOGD("DELETE SECRET RESPONSE: " + root);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test03ListNamespaces() {
		try {
			String url = apiServer + "api/v1/namespaces";
			LOGD("Get all Namespaces " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all NS response " + out);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test04ListPersistentVolumes() {
		try {
			String url = apiServer + "api/v1/persistentvolumes";
			LOGD("Get all PersistentVolumes " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all PersistentVolumes response " + out);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test05ListPVCs() {
		try {
			String url = apiServer + "api/v1/persistentvolumeclaims";
			LOGD("Get all PVCs " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all PVCs response " + out);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
	@Ignore
	@Test
	public void test07ListEvents() {
		try {
			String url = apiServer + "api/v1/events";
			LOGD("Get all EVENTSs " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all EVENTs response " + out);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test08ListStorageClasses() {
		try {
			String url = apiServer + "apis/storage.k8s.io/v1/storageclasses";
			LOGD("Get all Storage classes " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all Storage classes response " + out);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test10ListVolumeAttachements() {
		try {
			String url = apiServer + "apis/storage.k8s.io/v1/volumeattachments";
			LOGD("Get all VolumeAttachments " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all VolumeAttachmentq response " + out);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test12CreatePV() {
		try {
			// create
			String raw = IOTools.readFromStream(TestK8SConfigStorageApis.class.getResourceAsStream("/resources/k8s/yml/pv-nfs.yml"));
			String namespace = "default";
			String name = "nfs-" + System.currentTimeMillis();
			
			JSONObject data = new JSONObject();
			
//			params.put("NAMESPACE", namespace);
			params.put("NAME", name);
			params.put("SHARE_NAME", "/var/nfs/ns1");
			params.put("HOST_NAME", "192.68.40.84");
			
			String yml = RestClient.replaceParams(params, raw);

			// convert to JSON  using SnakeYaml
			Yaml yaml 			= new Yaml();
			JSONObject root 	= new JSONObject((Map)yaml.load(yml));
			String content		= root.toString();
			
			dest = HTTPDestination.create(apiServer, content, CoreTypes.CONTENT_TYPE_JSON, null, null);
			dest.setDebug(true);
			
			LOGD("== CREATE PV " + name);
			LOGD("CREATE PV PAYLOAD: " + content);
			
			root =  (JSONObject)client.invoke(dest, "CreatePV", params);

			// {"metadata":{"uid":"8581bce7-7176-11ea-a941-000c297a5447","resourceVersion":"36423032","name":"nfs-pv01","creationTimestamp":"2020-03-29T04:34:04Z","selfLink":"/api/v1/persistentvolumes/nfs-pv01"},"apiVersion":"v1","kind":"PersistentVolume","spec":{"storageClassName":"manual","mountOptions":["hard","nfsvers=3"],"nfs":{"server":"192.68.40.84","path":"/var/nfs/ns1"},"persistentVolumeReclaimPolicy":"Recycle","accessModes":["ReadWriteMany"],"capacity":{"storage":"1Gi"},"volumeMode":"Filesystem"},"status":{"phase":"Pending"}}
			LOGD("CREATE PV RESPONSE: " + root);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test13CreatePVC() {
		try {
			// create
			String raw = IOTools.readFromStream(TestK8SConfigStorageApis.class.getResourceAsStream("/resources/k8s/yml/pvc-nfs.yml"));
			String namespace = "default";
			
			JSONObject data = new JSONObject();
			
			params.put("NAMESPACE", namespace);
			
			String yml = RestClient.replaceParams(params, raw);

			// convert to JSON  using SnakeYaml
			Yaml yaml 			= new Yaml();
			JSONObject root 	= new JSONObject((Map)yaml.load(yml));
			String content		= root.toString();
			
			dest = HTTPDestination.create(apiServer, content, CoreTypes.CONTENT_TYPE_JSON, null, null);
			dest.setDebug(true);
			
			LOGD("== CREATE PVC @ " + namespace);
			LOGD("CREATE PV PAYLOAD: " + content);
			
			root =  (JSONObject)client.invoke(dest, "CreatePVC", params);

			// {"metadata":{"uid":"8595bbc8-7176-11ea-a941-000c297a5447","resourceVersion":"36423037","name":"nfs-pvc01","namespace":"default","creationTimestamp":"2020-03-29T04:34:04Z","selfLink":"/api/v1/namespaces/default/persistentvolumeclaims/nfs-pvc01"},"apiVersion":"v1","kind":"PersistentVolumeClaim","spec":{"storageClassName":"manual","resources":{"requests":{"storage":"1Gi"}},"accessModes":["ReadWriteMany"],"volumeMode":"Filesystem"},"status":{"phase":"Pending"}}
			LOGD("CREATE PVC RESPONSE: " + root);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test14CleanPVCs() {
		try {
			// create
			String namespace = "default";
			
			JSONObject data = new JSONObject();

			params.put("NAME", "nfs-pv01");
			params.put("NAMESPACE", namespace);
			
			String content		= null; //root.toString();
			
			dest = HTTPDestination.create(apiServer, content, CoreTypes.CONTENT_TYPE_JSON, null, null);
			dest.setDebug(true);
			
			LOGD("== DEL PV @ " + namespace);
			JSONObject root =  (JSONObject)client.invoke(dest, "DeleteVolume", params);
			LOGD("DEL PV RESPONSE: " + root);

			// DEL PVC
			params.put("NAME", "nfs-pvc01");
			
			LOGD("== DEL PVC @ " + namespace);
			root =  (JSONObject)client.invoke(dest, "DeleteVolumeClaim", params);
			// {"metadata":{"deletionGracePeriodSeconds":0,"uid":"3f095fb8-7178-11ea-a941-000c297a5447","finalizers":["kubernetes.io/pvc-protection"],"resourceVersion":"36424437","name":"nfs-pvc01","namespace":"default","creationTimestamp":"2020-03-29T04:46:25Z","annotations":{"pv.kubernetes.io/bound-by-controller":"yes","pv.kubernetes.io/bind-completed":"yes"},"selfLink":"/api/v1/namespaces/default/persistentvolumeclaims/nfs-pvc01","deletionTimestamp":"2020-03-29T04:46:25Z"},"apiVersion":"v1","kind":"PersistentVolumeClaim","spec":{"storageClassName":"manual","volumeName":"nfs-pv01","resources":{"requests":{"storage":"1Gi"}},"accessModes":["ReadWriteMany"],"volumeMode":"Filesystem"},"status":{"phase":"Bound","accessModes":["ReadWriteMany"],"capacity":{"storage":"1Gi"}}}
			LOGD("DEL PVC RESPONSE: " + root);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
