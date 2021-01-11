package console.kubernetes;

import com.cloud.kubernetes.Helm;
import com.cloud.kubernetes.K8SNode;
import com.cloud.kubernetes.Helm.HelmResponse;

import junit.kubernetes.MockObjects;

public class TestHelm {

	static void test1  () {
		try {
			K8SNode node = new K8SNode("node1", MockObjects.apiServerN208, MockObjects.accessTokenN208, MockObjects.sshUserLab, MockObjects.sshPwdLab );

			Helm h = Helm.newInstance(node);
			HelmResponse r = h.listRepos();
			System.out.println(r);
			System.out.println("bitnami exists? " + h.hasRepo("monocular"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		test1();
	}

}
