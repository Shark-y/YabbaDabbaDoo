package junit.kubernetes;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import junit.docker.MockObjects;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.WebClient;
import com.cloud.docker.DockerHttpHijack;
import com.cloud.kubernetes.Kubernetes;
import com.cloud.ssh.StreamIO;

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
public class TestK8SWorkLoadApis {

	static void LOGD(String text) {
		System.out.println("[K8S-WORKLOAD] "  + text);
	}

//	static String apiServer = junit.kubernetes.MockObjects.apiServer;
//	static String accessToken = junit.kubernetes.MockObjects.accessToken;

	static String apiServer = junit.kubernetes.MockObjects.apiServerN208;
	static String accessToken = junit.kubernetes.MockObjects.accessTokenN208;

	static WebClient wc = new WebClient();
	static Map<String, String> hdrs = new HashMap<String, String>();
	
	@BeforeClass
	public static void init() {
		hdrs.put("Authorization", "Bearer " + accessToken);

		wc.setVerbosity(true);
		wc.logToStdOut(true);
		
		// javax.net.ssl.SSLException: Received fatal alert: protocol_version
		MockObjects.fixSSLFatalProtocolVersion();
		try {
			junit.kubernetes.MockObjects.loadNodesFile();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
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
	@Ignore
	@Test
	public void test02PodList() {
		try {
			String url = apiServer + "/api/v1/pods";
			LOGD("Get all pods " + url);
			
			wc.setUrl(url);
			String out = wc.doGet(hdrs);
			
			// Permission error? - see https://github.com/fabric8io/fabric8/issues/6840
			/* [WWWW]  ==== RESPONSE HTTP Status: 403
[GET] HTTP Response msg: Forbidden
[GET]  [HDR] null = [HTTP/1.1 403 Forbidden]
[GET]  [HDR] Date = [Thu, 04 Apr 2019 01:53:17 GMT]
[GET]  [HDR] Content-Length = [323]
[GET]  [HDR] Content-Type = [application/json]
[GET]  [HDR] X-Content-Type-Options = [nosniff]
[K8S-WORKLOAD] Get all pods NS response {
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {
    
  },
  "status": "Failure",
  "message": "pods is forbidden: User \"system:serviceaccount:default:default\" cannot list resource \"pods\" in API group \"\" at the cluster scope",
  "reason": "Forbidden",
  "details": {
    "kind": "pods"
  },
  "code": 403
}
---- FIX Create fabric8-rbac.yaml then kubectl apply -f fabric8-rbac.yaml  OR unbind with kubectl delete -f fabric8-rbac.yaml (see https://github.com/fabric8io/fabric8/issues/6840)
# NOTE: The service account `default:default` already exists in k8s cluster.
# You can create a new account following like this:
#---
#apiVersion: v1
#kind: ServiceAccount
#metadata:
#  name: <new-account-name>
#  namespace: <namespace>

---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: fabric8-rbac
subjects:
  - kind: ServiceAccount
    # Reference to upper's `metadata.name`
    name: default
    # Reference to upper's `metadata.namespace`
    namespace: default
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io 
*/
			LOGD("Get all pods NS response " + out);
			// 200 OK resp see PodList.txt
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/*
	 * https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.14/#-strong-misc-operations-pod-v1-core-strong-
	 * HTTP Request
GET /api/v1/namespaces/{namespace}/pods/{name}/log
Path Parameters
Parameter	Description
name	name of the Pod
namespace	object name and auth scope, such as for teams and projects
---------------------------
Query Parameters
Parameter	Description
container	The container for which to stream logs. Defaults to only container if there is one container in the pod.
follow	Follow the log stream of the pod. Defaults to false.
limitBytes	If set, the number of bytes to read from the server before terminating the log output. This may not display a complete final line of logging, and may return slightly more or slightly less than the specified limit.
pretty	If 'true', then the output is pretty printed.
previous	Return previous terminated container logs. Defaults to false.
sinceSeconds	A relative time in seconds before the current time from which to show logs. If this value precedes the time a pod was started, only logs since the pod start will be returned. If this value is in the future, no logs will be returned. Only one of sinceSeconds or sinceTime may be specified.
tailLines	If set, the number of lines from the end of the logs to show. If not specified, logs are shown from the creation of the container or sinceSeconds or sinceTime
timestamps	If true, add an RFC3339 or RFC3339Nano timestamp at the beginning of every line of log output. Defaults to false.
	 * 
	 */
	@Ignore
	@Test
	public void test03GetPodLogs() {
		try {
			
			String namespace= "kube-system";
			String pod = "kube-scheduler-kubemaster"; //"coredns-fb8b8dccf-jxj8j";
			String url = apiServer + "/api/v1/namespaces/" + namespace + "/pods/" + pod + "/log?tailLines=100";
			LOGD("Get pod " + pod + " logs " + namespace + " url: " + url);

			wc.setUrl(url);
			String out = wc.doGet(hdrs);

			LOGD("----Get POD LOGS response ---\n" + out);
			/*
			 * OUT
			 * E0607 00:35:27.385454       1 reflector.go:126] k8s.io/client-go/informers/factory.go:133: Failed to list *v1.ReplicaSet: replicasets.apps is forbidden: User "system:kube-scheduler" cannot list resource "replicasets" in API group "apps" at the cluster scope
E0607 00:35:27.385513       1 reflector.go:126] k8s.io/client-go/informers/factory.go:133: Failed to list *v1.StatefulSet: statefulsets.apps is forbidden: User "system:kube-scheduler" cannot list resource "statefulsets" in API group "apps" at the cluster scope
E0607 00:35:27.385572       1 reflector.go:126] k8s.io/kubernetes/cmd/kube-scheduler/app/server.go:223: Failed to list *v1.Pod: pods is forbidden: User "system:kube-scheduler" cannot list resource "pods" in API group "" at the cluster scope
E0607 00:35:27.385625       1 reflector.go:126] k8s.io/client-go/informers/factory.go:133: Failed to list *v1.Service: services is forbidden: User "system:kube-scheduler" cannot list resource "services" in API group "" at the cluster scope
I0607 00:35:29.428604       1 controller_utils.go:1027] Waiting for caches to sync for scheduler controller
I0607 00:35:29.529027       1 controller_utils.go:1034] Caches are synced for scheduler controller
I0607 00:35:29.529104       1 leaderelection.go:217] attempting to acquire leader lease  kube-system/kube-scheduler...
I0607 00:35:45.498672       1 leaderelection.go:227] successfully acquired lease kube-system/kube-scheduler
			 */
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	// https://blog.openshift.com/executing-commands-in-pods-using-k8s-api/
	// https://stackoverflow.com/questions/34373472/how-to-execute-command-in-a-pod-kubernetes-using-api
	// https://github.com/kubernetes/kubernetes/blob/release-1.7/pkg/kubectl/cmd/exec.go
	// https://medium.com/handy-tech/analysis-of-a-kubernetes-hack-backdooring-through-kubelet-823be5c3d67c
	// https://www.oreilly.com/library/view/managing-kubernetes/9781492033905/ch04.html
	@Ignore
	@Test
	public void test04ExecCommandInPod() {
		try {
			// wss://10.226.67.20:6443/api/v1/namespaces/default/pods/calico-termite-mongodb-5d6fbfdb44-srnhn/exec?container=calico-termite-mongodb&stdin=1&stdout=1&stderr=1&tty=1&command=%2Fbin%2Fbash&access_token=eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tMjJqejkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjAyMzI0NDljLTViYjUtMTFlOS05N2VjLTA4MDAyN2UxNjRiMCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.rlchRPnBEkyW5GpTF2S-_guYMZMAk5cszA5KR6uJ0YGacEyoMYlQHcqwWEZF8S5sNyWnsVWAkZvl2moIdGTdpfeIW7mILPFwcTQGIk0rHUruUZnyGB17VsDG8wPfN-l9GdrIALGX3iHJeuIEt-hbsUcHia0Ivv4ITXfyfFz4htrxaxImJXsFxxzTDpOxz_SKnupAtlLslm0tS_r0xt4WsQuz9reSkTmNHsOHry-cPFDP99LT84AaJwq3Pv50qQ_JY-Wavj9ZdNCDUSjjsLhjQmz4wspjxaTkSoa88TplxmaQFsLDp6i0rX14amoeVMpGVD3O1eg_ZuKw-7UCfMnTEw
			// wscat -H "Authorization:Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tMjJqejkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjAyMzI0NDljLTViYjUtMTFlOS05N2VjLTA4MDAyN2UxNjRiMCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.rlchRPnBEkyW5GpTF2S-_guYMZMAk5cszA5KR6uJ0YGacEyoMYlQHcqwWEZF8S5sNyWnsVWAkZvl2moIdGTdpfeIW7mILPFwcTQGIk0rHUruUZnyGB17VsDG8wPfN-l9GdrIALGX3iHJeuIEt-hbsUcHia0Ivv4ITXfyfFz4htrxaxImJXsFxxzTDpOxz_SKnupAtlLslm0tS_r0xt4WsQuz9reSkTmNHsOHry-cPFDP99LT84AaJwq3Pv50qQ_JY-Wavj9ZdNCDUSjjsLhjQmz4wspjxaTkSoa88TplxmaQFsLDp6i0rX14amoeVMpGVD3O1eg_ZuKw-7UCfMnTEw" -c  "wss://10.226.67.20:6443/api/v1/namespaces/kube-system/pods/kube-scheduler-kubemaster/exec?container=kube-scheduler&stdin=1&stdout=1&stderr=1&tty=0&command=date"
			
			// /api/v1/namespaces/NAMESPACE/pods/PODNAME/exec?command=/bin/bash&stdin=true&stderr=true&stdout=true&tty=true&container=CONTAINER
//			String namespace= "kube-system";
//			String pod = "kube-scheduler-kubemaster"; 
//			String container = "&container=kube-scheduler";
			
			String namespace = "default";
			String pod = "calico-termite-mongodb-5d6fbfdb44-srnhn";
			String container = "&container=calico-termite-mongodb";
			
			//String cmd = ""; //"command=ls&command=-l"; //"/bin/sh"; // "/bin/date";
			//String cmd = "command=/bin/sh&command=-c&command=date"; 
			String cmd = "command=/bin/bash"; 
			String token = ""; // "&access_token=" + accessToken;
	
			String url = apiServer + "/api/v1/namespaces/" + namespace + "/pods/" + pod + "/exec"
//			String url = apiServer + "/api/v1/namespaces/" + namespace + "/pods/" + pod + "/attach"
					+ "?" + cmd // URLEncoder.encode(cmd, "UTF-8")
					//+ "&stdin=true&stderr=false&stdout=true&tty=true" +  container + token;
					+ "&stdin=true&stderr=true&stdout=true&tty=true" +  container + token;
			String payload = "";

			LOGD("Exec " + cmd + " in pod " + pod + " namespace " + namespace + " url: " + url);

			/* --------- HANDSHAKE
			 * GET /api/v1/namespaces/kube-system/pods/kube-scheduler-kubemaster/exec?command=/bin/date&stdin=true&stderr=true&stdout=true&tty=true&container=kube-scheduler HTTP/1.1
			Upgrade: websocket
			Connection: Upgrade
			Host: 10.226.67.20
			Sec-WebSocket-Version: 13
			Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tMjJqejkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjAyMzI0NDljLTViYjUtMTFlOS05N2VjLTA4MDAyN2UxNjRiMCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.rlchRPnBEkyW5GpTF2S-_guYMZMAk5cszA5KR6uJ0YGacEyoMYlQHcqwWEZF8S5sNyWnsVWAkZvl2moIdGTdpfeIW7mILPFwcTQGIk0rHUruUZnyGB17VsDG8wPfN-l9GdrIALGX3iHJeuIEt-hbsUcHia0Ivv4ITXfyfFz4htrxaxImJXsFxxzTDpOxz_SKnupAtlLslm0tS_r0xt4WsQuz9reSkTmNHsOHry-cPFDP99LT84AaJwq3Pv50qQ_JY-Wavj9ZdNCDUSjjsLhjQmz4wspjxaTkSoa88TplxmaQFsLDp6i0rX14amoeVMpGVD3O1eg_ZuKw-7UCfMnTEw
			Upgrade: websocket
			Sec-WebSocket-Key: 1560724735090
			Connection: Upgrade
			Accept: **
			Content-Length: 0
			
			
			[DOCKER-HTTP-HIJACK] Response line: HTTP/1.1 101 Switching Protocols
			[DOCKER-HTTP-HIJACK] Response line: Upgrade: websocket
			[DOCKER-HTTP-HIJACK] Response line: Connection: Upgrade
			[DOCKER-HTTP-HIJACK] Response line: Sec-WebSocket-Accept: xZvH8e7MVQxCxe2ZYYL945WqrV0=
			[DOCKER-HTTP-HIJACK] Response line: Sec-WebSocket-Protocol: 
			[DOCKER-HTTP-HIJACK] Response line: 
			[DOCKER-HTTP-HIJACK] Handshake complete
			‚‚‚Sun Jun 16 22:38:56 UTC 2019
			ˆè
			 */

			DockerHttpHijack1 http = new DockerHttpHijack1(new URI(url));
			
			
			Map<String, String> headers = new HashMap<String, String>();			
			headers.put("Accept", "*/*");
			headers.put("Authorization", "Bearer " + accessToken);
//			headers.put("Connection", "Upgrade");
//			headers.put("Upgrade", "websocket");
			headers.put("Sec-WebSocket-Key", String.valueOf(System.currentTimeMillis()));
			headers.put("Sec-WebSocket-Version", "13");
			headers.put("X-Stream-Protocol-Version", "v4.channel.k8s.io");
			//headers.put("X-Stream-Protocol-Version", "channel.k8s.io");
			
//			wc.setUrl(url);
//			String out = wc.doGet(headers); // hdrs);
//			System.out.println(out);
			
			http.service("HTTP/1.1", "GET", headers, payload, true, "websocket");
			
			http.pipeStdout( new StreamIO1.OutputSink () { 
//			http.pipeStdout( new StreamIO.PrintStreamSource() {

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
					final String str 	= new String(chunk);
					// ‚‚<I have no name!@calico-termite-mongodb-5d6fbfdb44-srnhn:/$
					System.out.println("GOT CHUNCK=" + Arrays.toString(chunk) + " STR:" + str);
				}

				@Override
				public boolean receiveChunks() {
					return true;
				} 
				
			});
			
			/* Status 6/18/2019 Got shell prompt but Server aborts as soon as data is sent :(
			GOT CHUNCK=[-126, 1, 1] STR:‚
			GOT CHUNCK=[-126, 1, 1] STR:‚
			GOT CHUNCK=[-126, 60, 1, 73, 32, 104, 97, 118, 101, 32, 110, 111, 32, 110, 97, 109, 101, 33, 64, 99, 97, 108, 105, 99, 111, 45, 116, 101, 114, 109, 105, 116, 101, 45, 109, 111, 110, 103, 111, 100, 98, 45, 53, 100, 54, 102, 98, 102, 100, 98, 52, 52, 45, 115, 114, 110, 104, 110, 58, 47, 36, 32] STR:‚<I have no name!@calico-termite-mongodb-5d6fbfdb44-srnhn:/$ 
			SEND BYTES=[0, 108, 115, 32, 45, 108, 97, 13] 
			GOT CHUNCK=[-120, 2, 3, -22, -120, 2, 3, -24] STR:ˆêˆè
			Read a NULL chunk from socket. EOF? ERROR? Aborting reader loop.
			*/
			
			// wait for stdout
			Thread.sleep(2000);
			
			// Get a shell promt here with some garbage
			// ‚‚<I have no name!@calico-termite-mongodb-5d6fbfdb44-srnhn:/$
			
			// Something is wrong here. As soon as anything is sent, the server sends a NULL (EOF) 
			byte[] data = new byte[]{ 0, 108, 115, 32, 45, 108, 97, 13} ; //0, 100, 97, 116, 101, 10};
			String cmd1 = new String(data);
			System.out.println("SEND BYTES=" + Arrays.toString(data) + " " + cmd1 );
			
			//String cmd1 = "ls -la\n";
			//LOGD("SEND " + cmd1);
			http.send(data);
			
			Thread.sleep(5000);
		
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test05ExecCommandInPodViaSPDY() {
		try {
			
			// /api/v1/namespaces/NAMESPACE/pods/PODNAME/exec?command=/bin/bash&stdin=true&stderr=true&stdout=true&tty=true&container=CONTAINER
//			String namespace= "kube-system";
//			String pod = "kube-scheduler-kubemaster"; 
//			String container = "&container=kube-scheduler";
			
			String namespace = "default";
			String pod = "calico-termite-mongodb-5d6fbfdb44-srnhn";
			String container = "&container=calico-termite-mongodb";
			
			//String cmd = ""; //"command=ls&command=-l"; //"/bin/sh"; // "/bin/date";
			//String cmd = "command=/bin/sh&command=-c&command=date"; 
			String cmd = "command=" + "date" ; //"%2Fbin%2Fdate"; 
	
			String url = apiServer + "/api/v1/namespaces/" + namespace + "/pods/" + pod + "/exec"
					+ "?" + cmd // URLEncoder.encode(cmd, "UTF-8")
					+ "&stdin=true&stderr=true&stdout=true&tty=true" +  container ;
			String payload = "";

			LOGD("Exec " + cmd + " in pod " + pod + " namespace " + namespace + " url: " + url);


			DockerHttpHijack1 http = new DockerHttpHijack1(new URI(url));
			
			Map<String, String> headers = new HashMap<String, String>();			

			headers.put("Accept", "*/*");
			headers.put("Authorization", "Bearer " + accessToken);
//			headers.put("Connection", "Upgrade");
//			headers.put("Upgrade", "websocket");
			//headers.put("Sec-WebSocket-Key", String.valueOf(System.currentTimeMillis()));
			//headers.put("Sec-WebSocket-Version", "13");
			headers.put("X-Stream-Protocol-Version", "v4.channel.k8s.io");
			//headers.put("X-Stream-Protocol-Version", "channel.k8s.io");
			headers.put("User-Agent" ,"kubectl/v1.14.0 (linux/amd64) kubernetes/641856d");
			
			http.service("HTTP/1.1","POST", headers, payload, true, "SPDY/3.1");
			
			http.pipeStdout( new StreamIO1.OutputSink () { 
//			http.pipeStdout( new StreamIO.PrintStreamSource() {

				@Override
				public PrintStream getPrintStream() {
					return System.out;
				}

				@Override
				public boolean closeAfterWrite() {
					return false;
				}
				
				@Override
				public void onChunkReceived(byte[] chunk) throws IOException {
					final String str 	= new String(chunk);
					// ‚‚<I have no name!@calico-termite-mongodb-5d6fbfdb44-srnhn:/$
					System.out.println("GOT CHUNCK=" + Arrays.toString(chunk) + " STR:" + str);
				}

				@Override
				public boolean receiveChunks() {
					return true;
				} 
				
			});
			
			
			// wait for stdout
			Thread.sleep(2000);

//			String cmd1 = "ls -la\n";
//			LOGD("SEND " + cmd1);
//			http.send(cmd1);
			
			Thread.sleep(5000);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Ignore
	@Test
	public void test06ExecCommandInPodViaK8S() {
		try {
			String nodeName = "KubeMaster_BUILD"; // NACR208 
			//String nodeName = "KubeDevClusterLAB"; // local
			String namespace= "kube-system";
			String podName = "kube-scheduler-kubemaster"; 
			String containerName = "kube-scheduler";
			String[] commands = new String[] {"ls", "-l"};
			
			String stdout = Kubernetes.executeCommandInPod(nodeName, namespace, podName, containerName, commands);
			
			LOGD(Arrays.toString(commands) + "\n"+ stdout);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
