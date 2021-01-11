package junit.kubernetes;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import junit.docker.MockObjects;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.RestClient.HTTPDestination;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestServiceAccounts {

	static void LOGD(String text) {
		System.out.println("[K8S-SA] "  + text);
	}

	static String apiServer = junit.kubernetes.MockObjects.apiServerN208;
	static String accessToken = junit.kubernetes.MockObjects.accessTokenN208;

	static RestClient client = new RestClient();
	static HTTPDestination dest ;
	static Map<String, Object> params = new HashMap<String, Object>();

	@BeforeClass
	public static void init() {
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
	public void test01PatchImagePullSecretInSA() {
		try {
			String content = "{\"imagePullSecrets\": [{\"name\": \"${SECRET}\"}]}";
			String namespace = "default";
			String account = "default";
			String name = "regcred";
			
			RestClient.enablePATCHInHttpURLConnection();
			
			dest = HTTPDestination.create(apiServer, content, "application/strategic-merge-patch+json", null, null);
			dest.setDebug(true);

			params.put("NAMESPACE", namespace);
			params.put("SERVICEACCOUNT", account);
			params.put("SECRET", name);

			LOGD("== PATCH SA IMAGEPULLSECRET " + name + " @ NS " + namespace + " SA " + account);
			LOGD("PATCH PAYLOAD: " + content);
			
			JSONObject root =  (JSONObject)client.invoke(dest, "PatchServiceAccount", params);

			// {"metadata":{"generation":2,"uid":"66ab249a-6d0b-11ea-a941-000c297a5447","resourceVersion":"35532469","name":"agentaggregator","namespace":"default","creationTimestamp":"2020-03-23T13:37:12Z","annotations":{"kompose.version":"1.19.0 (f63a961c)","kompose.cmd":"kompose convert -c -f c1convs.yml --volumes hostPath","deployment.kubernetes.io/revision":"1"},"selfLink":"/apis/apps/v1/namespaces/default/deployments/agentaggregator","labels":{"io.kompose.service":"agentaggregator"}},"apiVersion":"apps/v1","kind":"Deployment","spec":{"template":{"metadata":{"creationTimestamp":null,"annotations":{"kompose.version":"1.19.0 (f63a961c)","kompose.cmd":"kompose convert -c -f c1convs.yml --volumes hostPath"},"labels":{"io.kompose.service":"agentaggregator"}},"spec":{"dnsPolicy":"ClusterFirst","terminationGracePeriodSeconds":30,"volumes":[{"name":"agentaggregator-hostpath0","hostPath":{"path":"/mnt/nfs/cloud/convs/config/AgentAggregatorConfig.json","type":""}}],"containers":[{"args":["-DrabbitmqHost=$(MQHOST)","-cp","/app/resources:/app/classes:/app/libs/*","com.cloud.agent.aggregator.AgentAggregatorRunner","--config","AgentAggregatorConfig.json"],"image":"us.gcr.io/cloud-bots/agentaggregator","imagePullPolicy":"Always","terminationMessagePolicy":"File","terminationMessagePath":"/dev/termination-log","name":"agentaggregator","resources":{},"env":[{"name":"MQHOST","valueFrom":{"secretKeyRef":{"name":"c1convs","key":"MQHOST"}}}],"command":["java"],"volumeMounts":[{"mountPath":"/AgentAggregatorConfig.json","name":"agentaggregator-hostpath0"}]}],"securityContext":{},"restartPolicy":"Always","schedulerName":"default-scheduler"}},"replicas":0,"selector":{"matchLabels":{"io.kompose.service":"agentaggregator"}},"revisionHistoryLimit":2147483647,"strategy":{"type":"Recreate"},"progressDeadlineSeconds":2147483647},"status":{"conditions":[{"reason":"MinimumReplicasAvailable","type":"Available","lastTransitionTime":"2020-03-23T13:37:14Z","message":"Deployment has minimum availability.","status":"True","lastUpdateTime":"2020-03-23T13:37:14Z"}],"observedGeneration":2}}
			LOGD("PATCH SA IMAGEPULLSECRET RESPONSE: " + root);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
