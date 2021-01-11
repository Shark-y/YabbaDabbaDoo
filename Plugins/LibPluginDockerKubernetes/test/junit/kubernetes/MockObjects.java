package junit.kubernetes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.cloud.docker.Docker;
import com.cloud.kubernetes.K8SNode;
import com.cloud.kubernetes.Kubernetes;

public class MockObjects {

	static String apiServer = "https://192.168.42.59:6443";
	static String accessToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tNGM2c3giLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImY2ZTJmZmNiLTU3OWYtMTFlOS1hNGQ5LTA4MDAyNzU4YTNiNSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.VA8GV13pYUJW4nit761AfGNK71uKuseTbxcaD1RTkC-BKPwcR6VdrgmcDlZYo4cQNXWc2jaRDMY9lVPDcZncRY8_nfNMCQz_cFgUOwFDrYAdHgc8j8uHb3ADVmmZgumU2gWSvmlu0mDaTz3WAEHHak88K-bmWc4p4CvCp1lIqWTIC7PK_MH4IqAfrjnTejlWs-kmlaUzzMFwA1k_46628YQKEDJ5al_h0XMFnXMch5C0q2Jw_zo0eUiAOiqVnGXq6C2lWmDrVsjfUwcvR82OuhiH5TfQYKB828AXDiT2hayf_3-oEiKCD6X7sL_IbUPt9hXM18js3i1KHhdLCuHPCg";

	// build laptop
	static String apiServerLaptop = "https://10.226.67.20:6443";
	static String accessTokenLaptop = "eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tMjJqejkiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjAyMzI0NDljLTViYjUtMTFlOS05N2VjLTA4MDAyN2UxNjRiMCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.rlchRPnBEkyW5GpTF2S-_guYMZMAk5cszA5KR6uJ0YGacEyoMYlQHcqwWEZF8S5sNyWnsVWAkZvl2moIdGTdpfeIW7mILPFwcTQGIk0rHUruUZnyGB17VsDG8wPfN-l9GdrIALGX3iHJeuIEt-hbsUcHia0Ivv4ITXfyfFz4htrxaxImJXsFxxzTDpOxz_SKnupAtlLslm0tS_r0xt4WsQuz9reSkTmNHsOHry-cPFDP99LT84AaJwq3Pv50qQ_JY-Wavj9ZdNCDUSjjsLhjQmz4wspjxaTkSoa88TplxmaQFsLDp6i0rX14amoeVMpGVD3O1eg_ZuKw-7UCfMnTEw";

	// FoghornLeghorn.cloudlab.com @ lab
	public static final String apiServerN208 = "https://192.168.40.84:6443/";
	public static final String accessTokenN208 = "eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tNDJnNWwiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImQ4YjM5NzE3LTZhOWItMTFlOS1hZDE1LTAwMGMyOTdhNTQ0NyIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.nDsRUGHd7aBUqcDbfRKqPQaiH6HJD9AyDssNW8yKqIyTHRAyJoMVBwGNHaeS_8xjrWot8mSrX54ZRHkx9CI3GFn6odFI0cWiKCvzw_lleARSMJPCXi2xOjMYfL7PspJg858cGLi45U-87xtBNWqQjDCkIWF7sFCBOwQ0053fKC54fthvimGhm-i209qNVeERSwYZs4Y7VTcYUV6lkshSzuIcbiTDNCajYSR7iHEeYE6QBA4vUsPmz53frrCVMkvZwg0H2Q0lHKP9pZhw5so4I8M3ZZ0JvWd8FyBZv0zvv0g2ZwSY338Aw1RLMTp8JsuU-dINGUjU6weFtiwAqnlztA";
	
	
	static String sshHost = "192.168.42.59";
	static String sshUser = "root";
	static String sshPwd = "osboxes.org";

	static String sshUser1 = "osboxes";
	static String sshPwd1 = "osboxes.org";

	static String sshHostC1 = "10.226.67.20";
	static String sshUserC1 = "root";
	static String sshPwdC1 = "osboxes.org";

	public static String sshUserLab = "labadmin";
	public static String sshPwdLab = "Thenewcti1!";
	

	
	static void LOGD(String text) {
		System.out.println("[K8S-MOCKOBJS] "  + text);
	}

	static void loadNodesFile () throws JSONException, IOException, InstantiationException, IllegalAccessException {
		// load nodes file from $HOME/.cloud/
		String path = System.getProperty("user.home") + "\\.cloud\\CloudAdapter";
		LOGD("Loading nodes file from " + path);
		
		Map<String, String> config = new HashMap<String, String>();
		config.put(Docker.CONFIG_BASEPATH, path);
		Kubernetes.initialize(config);
		Kubernetes.load();
	}
}
