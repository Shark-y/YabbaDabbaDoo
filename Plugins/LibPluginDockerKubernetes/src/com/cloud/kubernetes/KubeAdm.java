package com.cloud.kubernetes;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.ssh.SSHExec;
import com.cloud.ssh.SSHExec.SSHExecResponse;
import com.jcraft.jsch.JSchException;

/**
 * Misc administration tasks via SSH
 * 
 * @author VSilva
 *
 */
public class KubeAdm {
	private static final Logger log 			= LogManager.getLogger(KubeAdm.class);
	
	private static void LOGD(final String text) {
		log.debug(text);
	}

	/**
	 * Get the Authorization token from a master by running the command via SSH <pre>kubectl get secrets -o jsonpath="{.items[?(@.metadata.annotations['kubernetes\.io/service-account\.name']=='default')].data.token}"|base64 -d</pre>
	 * <pre>{
    "apiVersion": "v1",
    "items": [
        {
            "apiVersion": "v1",
            "data": {
                "ca.crt": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUN5RENDQWJDZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcwQkFRc0ZBREFWTVJNd0VRWURWUVFERXdwcmRXSmwKY201bGRHVnpNQjRYRFRFNU1EUXdNekUyTURJeU5sb1hEVEk1TURNek1URTJNREl5Tmxvd0ZURVRNQkVHQTFVRQpBeE1LYTNWaVpYSnVaWFJsY3pDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBTTdjCnhPWHd6b0c4cTJjdndYOVJ3akxBSDQrUHJtMHFoUWQ4QzdYZGtOaDZoMUVIOXB5alhzNGpaRlp3ZFEyNzJDcmkKL1dNb0lET0p6czcyR1JsWjVBQ2swY0UvY09vU1pVRXZkUmtvREh2NVZwVEcvT1g3a2hmVHZZZlo0bmZVSkV1UQpUUEh4UW1TTUR2amxLSVJ1SWtSOEZCQmhic2NRUk9rZ0F1OGtub3p2eW9UaXd0YUJsQ1N3bzBYeFJUN2t4dnhKCk9XSWdUeFl5WUE5S1JxcXRsbVMzdDNJT1ZxL25mYldHeXBqUWhCOU5PYVI2emZSaWo0eitXR3ZpcCtxZmVpQVAKSW5YNElHYnVmbk9qMU1yNG5LSkhnR0h3d21GbjYwR1I2WE0wZkdLMUd3ckQ4Y1RNNDRtUzF2Z05qL1pPS0wwdgprMlNUb2poUUJUZW1CcGNDZUwwQ0F3RUFBYU1qTUNFd0RnWURWUjBQQVFIL0JBUURBZ0trTUE4R0ExVWRFd0VCCi93UUZNQU1CQWY4d0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFJeEFjZXpPaUZ3L2hjMEF5b3BNQkdqQ1JxRHYKTzNJZGtkLzQ0ODJhTmRmSGVFTkZWSDhjTnFWRXpUaDNtTFR0NHQxV0EzbUk5QjZvYnZjdkxkMElkTFJnaDNYUgpZdzBWTUlsRnByOC91YjBEMlFuVldBMy9VK1Jpd0ROeVlBSzdOczJOaG8vN3d5L1JsOGtwYVB0OEZVU0xNWEsrCkNYejIrVVMybldkOVQ0bzlaZkhlZkhEV2JHY2c1Z1RZUlJOYSs1M1VTMlBCQnUrLzE4YlJVMjNja0Z1TXdzY0MKRnNMa1lpaTMrK01IZVV3L3o0TUROcG5OSjAzVytlVWRNeisrUHEwajJPSGhnZmcxYS9hTUVqaFYvSmtGT1BPTgo0bFlIVW1yTGZPdnp6RmFQSW91Q0lDU3NBYTZ5R2d0ZlpkTXlvSDlCTHIyRWwwQm5SY3NacHNSQTVNQT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=",
                "namespace": "ZGVmYXVsdA==",
                "token": "ZXlKaGJHY2lPaUpTVXpJMU5pSXNJbXRwWkNJNklpSjkuZXlKcGMzTWlPaUpyZFdKbGNtNWxkR1Z6TDNObGNuWnBZMlZoWTJOdmRXNTBJaXdpYTNWaVpYSnVaWFJsY3k1cGJ5OXpaWEoyYVdObFlXTmpiM1Z1ZEM5dVlXMWxjM0JoWTJVaU9pSmtaV1poZFd4MElpd2lhM1ZpWlhKdVpYUmxjeTVwYnk5elpYSjJhV05sWVdOamIzVnVkQzl6WldOeVpYUXVibUZ0WlNJNkltUmxabUYxYkhRdGRHOXJaVzR0Y2pnMlptZ2lMQ0pyZFdKbGNtNWxkR1Z6TG1sdkwzTmxjblpwWTJWaFkyTnZkVzUwTDNObGNuWnBZMlV0WVdOamIzVnVkQzV1WVcxbElqb2laR1ZtWVhWc2RDSXNJbXQxWW1WeWJtVjBaWE11YVc4dmMyVnlkbWxqWldGalkyOTFiblF2YzJWeWRtbGpaUzFoWTJOdmRXNTBMblZwWkNJNkltWXdNamcxWVRkbUxUVTJNamt0TVRGbE9TMDVOakZqTFRBNE1EQXlOMlV4TmpSaU1DSXNJbk4xWWlJNkluTjVjM1JsYlRwelpYSjJhV05sWVdOamIzVnVkRHBrWldaaGRXeDBPbVJsWm1GMWJIUWlmUS5TT1ZOaHVRbGZWY2QtN0JCcF81R1E3Ylp5YzBndTdwMC1EQTBnVVhoMkZSUkZVd3dyN2Z3a3pFdDBSbkhCWHhBZHBMVV9WQXV2OEp3Z09LdXgybGlScUp6QnlodnNxbUlNRVIzeklCck92QzRGdDRIX0dCRjhmUHdubFBHQnNSWFVPU3EtWE9Tb0xxR2o0azVkR3V6VkJzQ2V6anMtZmRzb1lQYnFKY3BOTXhacGk3VlJVRFh0M2NiZkpIZjEyaXI1SjhQOUhCdUUyOE9DQmdOZmlZMWFLVHVhT1B0QU9vT3NyYkdwclVvSzMyQnkwSzdiRkpHc2FBQXdHOTA0WjNJbXlNT0ZRYVBydkVaZXVkTlluRDVzUl8xTXZDNnlnZWU1dWRDVlBldklXR0pVbUYwYS02ekw1R2pxZEg4TVZYWU1iQWlWVzd5NlktaUxyOTBFXy1veFE="
            },
            "kind": "Secret",
            "metadata": {
                "annotations": {
                    "kubernetes.io/service-account.name": "default",
                    "kubernetes.io/service-account.uid": "f0285a7f-5629-11e9-961c-080027e164b0"
                },
                "creationTimestamp": "2019-04-03T16:02:53Z",
                "name": "default-token-r86fh",
                "namespace": "default",
                "resourceVersion": "317",
                "selfLink": "/api/v1/namespaces/default/secrets/default-token-r86fh",
                "uid": "f029e721-5629-11e9-961c-080027e164b0"
            },
            "type": "kubernetes.io/service-account-token"
        }
    ],
    "kind": "List",
    "metadata": {
        "resourceVersion": "",
        "selfLink": ""
    }
}</pre>
	 * @param host Kube SSH host.
	 * @param serviceAccount Service account name.
	 * @param ns Namespace.
	 * @param user Kube SSH user.
	 * @param identity Kube SSH pwd.
	 * @return The authorization token for the API Server.
	 * @throws JSchException On SSH errors.
	 * @throws IOException On I/O errors.
	 * @throws JSONException 
	 */
	public static String getApiToken (final String host, final String serviceAccount, final String ns,  final String sshUser, final String sshIdentity) throws JSchException, IOException, JSONException {
		final String command = "kubectl get secrets -n " + ns + " -o jsonpath=\"{.items[?(@.metadata.annotations['kubernetes\\.io/service-account\\.name']=='" + serviceAccount + "')].data.token}\"|base64 -d";
		SSHExecResponse resp = SSHExec.execute(host, sshUser, sshIdentity, command);
		return resp.response;
	}
	
	
	/**
	 * Get Kube Clusters by running the command via SSH <pre>kubectl config view -o jsonpath='{range .clusters[*]}{.name}{\",\"}{.cluster.server}{end}'</pre>
	 * @param host Kube SSH host.
	 * @param user Kube SSH user.
	 * @param identity Kube SSH pwd.
	 * @return Line fed separated list of CLUSTER-NAME,SERVER,... For example: kubernetes,https://192.168.42.59:6443
	 * @throws JSchException On SSH errors.
	 * @throws IOException On I/O errors.
	 * @throws JSONException 
	 */
	public static JSONArray getClusters (String host, String user, String identity) throws JSchException, IOException, JSONException {
		return getConfig(host, user, identity).getJSONArray("clusters");
	}

	/**
	 * Get cluster config
	 * @param host SSH host.
	 * @param user SSH user.
	 * @param identity SSH pwd.
	 * @return <pre>{
    "kind": "Config",
    "apiVersion": "v1",
    "preferences": {},
    "clusters": [
        {
            "name": "kubernetes",
            "cluster": {
                "server": "https://192.168.42.59:6443",
                "certificate-authority-data": "DATA+OMITTED"
            }
        }
    ],
    "users": [
        {
            "name": "kubernetes-admin",
            "user": {
                "client-certificate-data": "REDACTED",
                "client-key-data": "REDACTED"
            }
        }
    ],
    "contexts": [
        {
            "name": "kubernetes-admin@kubernetes",
            "context": {
                "cluster": "kubernetes",
                "user": "kubernetes-admin"
            }
        }
    ],
    "current-context": "kubernetes-admin@kubernetes"
}</pre>
	 * @throws JSchException On SSH errors.
	 * @throws IOException On I/O errors.
	 * @throws JSONException
	 */
	public static JSONObject getConfig (String host, String user, String identity) throws JSchException, IOException, JSONException  {
		final String command = "kubectl config view -o json";
		SSHExecResponse resp = SSHExec.execute(host, user, identity, command);
		return new JSONObject(resp.response);
		
	}
	
	/**
	 * Grant a service account, namespace access to all cluster resources via a role binding.
	 * This is a fix for "message": "pods is forbidden: User \"system:serviceaccount:default:default\" cannot list resource \"pods\" in API group \"\" at the cluster scope"
	 * @param serviceAccount Service account.
	 * @param ns namespace.
	 * @param host SSH host.
	 * @param user SSH user.
	 * @param identity SSH password.
	 * @return SSH exit status.
	 */
	public static int createClusterAdminRoleBinding ( final String serviceAccount, final String ns, final String host, final String user, final String identity) throws JSchException, IOException, JSONException  {
		// GRANT POD_ACESS: kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=cluster-admin --serviceaccount=default:default
		// GRANT HELM_INSTALL: kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=cluster-admin --serviceaccount=kube-system:default
		// FIXME: For both POD ACESS, HELM INSTALL use: kubectl create clusterrolebinding default-admin --clusterrole cluster-admin --serviceaccount=default:default --serviceaccount=kube-system:default
		final String id 		= serviceAccount + "-" + ns + "-admin";  // default-admin
		final String command 	= "kubectl create clusterrolebinding " + id + " --clusterrole cluster-admin --serviceaccount=" + serviceAccount + ":" + ns; //default:default";

		LOGD("CreateClusterAdminRoleBinding command: " + command);
		
		SSHExecResponse resp 	= SSHExec.execute(host, user, identity, command);
		return resp.exitStatus;
	}

}
