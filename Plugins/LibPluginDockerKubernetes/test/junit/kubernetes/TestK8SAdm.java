package junit.kubernetes;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.kubernetes.Helm;
import com.cloud.kubernetes.K8SNode;
import com.cloud.kubernetes.KubeAdm;
import com.cloud.kubernetes.Kubernetes;
import com.cloud.kubernetes.Helm.HelmResponse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestK8SAdm {

	static void LOGD(String text) {
		System.out.println("[K8S-ADM] "  + text);
	}

	@Test
	public void test01GetAPIToken() {
		try {
			LOGD("Get API token from " + MockObjects.sshHost);
			String tok = KubeAdm.getApiToken(MockObjects.sshHost, "default", "default", MockObjects.sshUser, MockObjects.sshPwd);
			
			LOGD("API token:" + tok);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test02GetClusters() {
		try {
			LOGD("Get CLUSTERS from " + MockObjects.sshHost);
			JSONArray clusters = KubeAdm.getClusters(MockObjects.sshHost, MockObjects.sshUser1, MockObjects.sshPwd1);
			
			LOGD("CLUSTERS: [" + clusters + "]");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test02HelmInstall() {
		try {
			String chart ="helm install stable/airflow";
			String version = null; //"2.4.4";
			LOGD("Install " + chart);
			//SSHExecResponse r= KubeAdm.helmInstall(chart, version, MockObjects.sshHostC1, MockObjects.sshUserC1, MockObjects.sshPwdC1);

			String node = "KubeMaster";
			K8SNode server 		= Kubernetes.get(node);
			LOGD("HELM Install Chart: " + chart + " version=" + version + " @ server " + server);
			
			Helm helm 				= Helm.newInstance(server);
			HelmResponse r 			= helm.install(chart, null, version, null, null, null);
			
			LOGD("INSTALL: " + chart + "[" + r + "]");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
