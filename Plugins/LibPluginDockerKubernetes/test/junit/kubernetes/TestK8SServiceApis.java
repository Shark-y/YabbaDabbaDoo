package junit.kubernetes;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import junit.docker.MockObjects;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

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
public class TestK8SServiceApis {

	static void LOGD(String text) {
		System.out.println("[K8S-WORKLOAD] "  + text);
	}

	static String apiServer = junit.kubernetes.MockObjects.apiServer;
	static String accessToken = junit.kubernetes.MockObjects.accessToken;


	static WebClient wc = new WebClient();
	static Map<String, String> hdrs = new HashMap<String, String>();
	
	@BeforeClass
	public static void init() {
		hdrs.put("Authorization", "Bearer " + accessToken);

		wc.setVerbosity(true);
		wc.logToStdOut(true);
		// javax.net.ssl.SSLException: Received fatal alert: protocol_version
		MockObjects.fixSSLFatalProtocolVersion();
	}

	@Test
	public void test00GetApiVersion() {
		try {
			String url = apiServer + "/api";
			LOGD("Get api from " + url);

			wc.setUrl(url);
			String out = wc.doGet(hdrs);

			/* {  "kind": "APIVersions",  "versions": [    "v1"  ],  "serverAddressByClientCIDRs": [  {  "clientCIDR": "0.0.0.0/0",  "serverAddress": "192.168.42.59:6443" } ]}*/
			LOGD("Get api response " + out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test02ListEndpoints() {
		try {
			String url = apiServer + "/api/v1/endpoints";
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

	@Test
	public void test04ListNodes() {
		try {
			String url = apiServer + "/api/v1/nodes";
			LOGD("Get all Nodes " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all NODES response " + out);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test05ListServices() {
		try {
			String url = apiServer + "/api/v1/services";
			LOGD("Get all Services " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all SERVICES response " + out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test06ListDeployments() {
		try {
			String url = apiServer + "/apis/apps/v1/deployments";
			LOGD("Get all Deployments " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all Deployments response " + out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	// https://medium.com/@ManagedKube/kubernetes-troubleshooting-ingress-and-services-traffic-flows-547ea867b120
	// https://medium.com/@samwalker505/using-kubernetes-ingress-controller-from-scratch-35faeee8eca
	// BEST - https://www.linode.com/docs/applications/containers/how-to-deploy-nginx-on-a-kubernetes-cluster/
	@Test
	public void test07ListIngresses() {
		try {
			// acme208
			String url = junit.kubernetes.MockObjects.apiServerN208 + "/apis/networking.k8s.io/v1beta1/ingresses";
			hdrs.put("Authorization", "Bearer " + junit.kubernetes.MockObjects.accessTokenN208);
			/*
			 * {
  "kind": "IngressList",
  "apiVersion": "networking.k8s.io/v1beta1",
  "metadata": {
    "selfLink": "/apis/networking.k8s.io/v1beta1/ingresses",
    "resourceVersion": "28211"
  },
  "items": []
}
			 */
			LOGD("Get all Igresses " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// 200 OK see EndPointsList.txt
			LOGD("Get all Igresses response " + out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
