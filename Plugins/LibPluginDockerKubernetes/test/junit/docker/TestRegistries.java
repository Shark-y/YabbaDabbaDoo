package junit.docker;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.Base64;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.RestClient;
import com.cloud.core.w3.WebClient;
import com.cloud.core.w3.RestClient.HTTPDestination;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRegistries {

	static void LOGD(String text) {
		System.out.println("[REPO] "  + text);
	}

	static String url = "http://10.226.67.32:2375/";
	static String keyStorePath = null; //"C:\\Users\\vsilva\\.cloud\\CloudAdapter\\certs\\Node1.jks"; 
	static String keyStorePassword = null; //"password";

	// private repo
	static String image = "us.gcr.io/cloud-bots/agentaggregator";
	static String tag = "latest";
//	static String json_key = "/junit/resources/cloud-bots-key.json";
	static String projectId = "cloud-bots";
	//static String priv_access_tok = "X2pzb25fa2V5OnsKICAidHlwZSI6ICJzZXJ2aWNlX2FjY291bnQiLAogICJwcm9qZWN0X2lkIjogImMxYXMtYm90cyIsCiAgInByaXZhdGVfa2V5X2lkIjogImNhYjk3YTUzNzY3OTUxYTEyMjMwYTQ2M2EzNWE0NjY2MmZjODFiZjgiLAogICJwcml2YXRlX2tleSI6ICItLS0tLUJFR0lOIFBSSVZBVEUgS0VZLS0tLS1cbk1JSUV2UUlCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQktjd2dnU2pBZ0VBQW9JQkFRREdKS1h3VkE5RHpOQ3Fcbm1GOTFKSzFQTnVsRElmeThRTVo2TU9oZGdyNDZBOHJha0hDVGIyY3NFUzA0enVFQ1BVUGlTREhBaStpRUxxQWFcbnRXWUJ5bHlwMWdQbmlkYXBCSzZnV0s3M0c4TmRHYUcydzhGVUhoNVlmeDVIVk5sdUJOZnFHWHVVM0tQV1ZXSDlcbkYwcGpBVVpiMGdsaVFVZGsvWHZOTXVmK2FrQzQ3TEw0Mi9aQUQ0d285UjBkTFlVOFJVb2dWWjQwTXJaZm1SV1hcbk03MlRHMWVuUEVMUkdRNzhmZks4U3hqOFNlWWx6T0ZxMCtabC9rdWhIc0NTWXF1U2pEQXRTYVdnQnpySUZ5dnJcblI4Z2tsOXUzckI1QnkzdFRON1IyY2RGM3A4aklFbk8rMFhFc282Z0x2UmtXUEVmUGYrQXltQ0xYTnc1TmxITHNcbkVxTS9sRE83QWdNQkFBRUNnZ0VBRXFuUEM0VVBid1p5dlM3ZkJ1eXVlbUNrdGhNVWVETHR2d0t1VUdpSlBIT3dcbi9zZE9JMFVDQmMrVVg5NTBxVDVXRDVGWFJsaW5UUlFMTnBqUmcrZW90TUtZMlkxTkw5eG1DbXB3Q0l5UDZVd0pcbnhHcEo0bjkzd2tRdStPOFFEK0hhNkl6LzYvU2daZ2JpMVBFRnE2K3FEZW9Kd2p4OTJoQmFRZWpDbTE4c3UzNDlcbmwzN09KQXFSdXZ3elZQYmhPUVpmRXl4ZEZLY1pkLzRmbytTN2FLeVA5aXdrK0M2ZVFneVJ2eWsyR1lYM3B1VzhcbmVMTmthdmJWa1FCQS96RUdNcEd4STl4eEVLMTBsZ3VpaUk5Qkx1S3lLS0Rvd3cyMUNnWGcrVGc1RVNzaWY3d1JcbnRFOC9wclNJZTY0bWJKeS9kS29YT0lLbUEwNjFLVHhGZ2pDMmNwekVhUUtCZ1FEdlk0NGo5TWhLMjBqTCsrWlJcbmJ3Z3NBVVpZRXdzbFp5NldGNUxqL1JDeTYxSmdvemZqelVEY2FxR3FDVzlSL3FaS2lDdStkNUtQNjc4UWpJQWhcbi9qeDB0Unh1RHhpTUJYTHkyTnRuOGRGTFNaTlhaNllkNjlLQzk1UmxIWVpJdUZuN0RvdG8vdVd0UWFJRnpMOWVcblJKOEJJaEREU05BczFnbjVjQTdNRlQ3NHh3S0JnUURUNUduK3pIam52cVNNZDlHMUhLd2hsUkZXUXRKTUNyaTZcbk9IMkJFZi9WcmtQcE56cEZmYXJUNU0xcldnZmJra3VtSzRia01jejE1aWFxU0JKV1ZwRGFqTGphU3doQ1lFTm5cbjRuTkZSeVNUc1Z3Yi9sU2pTeWd3bEVGN0R0cE85eHJTZjdhTFQ4ZzNGRS84NXhIL1ZzOUszVEYzSFJMTXU5MzNcblZGd1FiRmlCYlFLQmdDaDBOM29HREs3eHZheVRCZ251N0grYk80cjR0T0orUEZZcTU2elZnRFBzSm9Da05IYXZcbm9lMWRxN0l6WS9lRVJBL0dVVFlmdU1uUGVmdE84dnhMbldYUGtWMWIrYmIzMk1RSE41U3FQY2N1U05MMWRoSUtcblh3bTQwdi8vYkVqdnRtMEJ1VGRtRStRaWVrSG1wdFJFWG1adm5rdVNDM1A0TzdsR1lZbkZjVjNUQW9HQkFKM3Fcbjd1QTU3YVh4bkZzZjlZZkFYYjBOaHdVOStkTTRibUpETmE0YkJ3dHV2Q2cwdzlZRWlXc3dhN1FsUGhQem5UT3pcbjN1MTAvQ2NMcHlkalhWOUJWdVc2MlEyL3UrRVVNMGhhS2NTbzkrYW0yVm4zbTRhenZia3UxUHBzb0dFWG9zTGNcbjhlUXp5cWphRjU4SmE0MWNXbE9XTklac2daVmFNbHhoWDlmUmw4aWxBb0dBZmpZUUJQbFdGd2x4aVgxNVdyU2ZcbnZHRjFGbU9pcUV3eFhXblptSndDSDdBK0piMXY4K1htTHVJUFV3VGk3bzVsMWZJWGtoQXQxMVJVNmsyczhFcVpcbk91ek9IN1o0WUlBbkNLL2hqdG1kZkVnTjZTVEZpSmNvVWdpWThlcm1wS2lrNzNWVXNHNVZ2TDkyT1FONWthU0VcbmZjY0xDQTNiZktvTUl6WjRLK2xpOUNJPVxuLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLVxuIiwKICAiY2xpZW50X2VtYWlsIjogIm5hY3IyMDgtZ2NyLXVuaWZpZWRjb25zb2xlQGMxYXMtYm90cy5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsCiAgImNsaWVudF9pZCI6ICIxMDcxMTU5NzM2NDA1ODE4MTkwNjgiLAogICJhdXRoX3VyaSI6ICJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20vby9vYXV0aDIvYXV0aCIsCiAgInRva2VuX3VyaSI6ICJodHRwczovL29hdXRoMi5nb29nbGVhcGlzLmNvbS90b2tlbiIsCiAgImF1dGhfcHJvdmlkZXJfeDUwOV9jZXJ0X3VybCI6ICJodHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9vYXV0aDIvdjEvY2VydHMiLAogICJjbGllbnRfeDUwOV9jZXJ0X3VybCI6ICJodHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9yb2JvdC92MS9tZXRhZGF0YS94NTA5L25hY3IyMDgtZ2NyLXVuaWZpZWRjb25zb2xlJTQwYzFhcy1ib3RzLmlhbS5nc2VydmljZWFjY291bnQuY29tIgp9";
	static String priv_access_tok = "AIzaSyBjgHHl_RNEJ3cn-yC9_xtp_F8ZPzyJOgY";
	
	// public
	static String imagePub = "gcr.io/google-containers/busybox";
	static String access_token = "ya29.GlsJBxUUcr0pj4c_TwaWR3Yxror9JhEOUtcKTLYuvP_NJqGa6k0qOFMUIVpfWgpHEXOVmjOMzpH0TumVY6HXbR6bYwxSy36nIP_zeeaowEk_ZfYaPmCh7yQsnAGm";
	
	// https://cloud.docker.com/repository/registry-1.docker.io/cloud/connector/tags
	static String dockerHubUser = "cloud";
	static String dockerHubPwd = "Thenewcti1";
	
	static RestClient rest;
	
	@BeforeClass
	public static void init() {
		try {
			MockObjects.initRestAPIClient();
			MockObjects.fixSSLFatalProtocolVersion();
			MockObjects.loadNodesFile();
			rest = MockObjects.rest;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
	/**
	 * https://cloud.google.com/container-registry/docs/advanced-authentication
	 * Update #1
	 */
	
	@Test
	public void test01GCRInstallImageFormPublicRepo() {
		try {
			LOGD("Create Image " + imagePub + " TAG:" + tag + " Destination:" + url);
			
			// https://cloud.google.com/container-registry/docs/advanced-authentication
			JSONObject Auth = new JSONObject();
			Auth.put("username", "oauth2accesstoken");
			Auth.put("password", access_token); 
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("NAME", imagePub);
			params.put("TAG", tag);
			//params.put("AUTHOBJ", Base64.encode(Auth.toString().getBytes(CoreTypes.CHARSET_UTF8)));
			
			LOGD("Create Image params:" + params);
			
			// {"id":"latest","status":"Pulling from library/busybox"}
			// HTTP Not Found (404): {"message":"pull access denied for busybox_foo, repository does not exist or may require 'docker login'"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "CreateImage", params);
			LOGD("Server Replied: " + resp.toString());
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test03GCRInstallImageFormPrivateRepo() {
		try {
			String pwd = MockObjects.loadGCRJsonkeyFromCP(); 
			// cleanup pwd
			JSONObject key = new JSONObject(pwd);
			
			JSONObject Auth = new JSONObject();
			
			Auth.put("username", "_json_key");
			Auth.put("password",  key.toString());
			
//			Auth.put("username", "oauth2accesstoken");
//			Auth.put("password",  priv_access_tok) ;  
			
			Auth.put("serveraddress", "https://us.gcr.io");
			LOGD("GCR Create Image " + image + " TAG:" + tag + " Pwd:" + pwd);
			LOGD("GCR Create Image Auth: " + Auth.toString()); 
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("NAME", image);
			params.put("TAG", tag);
			params.put("AUTHOBJ", Base64.encode(Auth.toString().getBytes(CoreTypes.CHARSET_UTF8)));

			// {"id":"latest","status":"Pulling from library/busybox"}
			// HTTP Not Found (404): {"message":"pull access denied for busybox_foo, repository does not exist or may require 'docker login'"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "CreateImage", params);
			LOGD("Server Replied: " + resp.toString());
			
			/* RESP
			{"status":"Trying to pull repository us.gcr.io/cloud-bots/agentaggregator ... "}
			{"status":"Pulling repository us.gcr.io/cloud-bots/agentaggregator"}
			{"errorDetail":{"message":"unauthorized: authentication required"},"error":"unauthorized: authentication required"}
			*/
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test04DockerInstallImageFormPrivateRepo() {
		try {
			// https://cloud.docker.com/repository/registry-1.docker.io/cloud/connector/tags
			String user = "cloud";
			String pwd = "Thenewcti1";  
			String img = "cloud/connector";
			tag = "cc_ucce";
			JSONObject Auth = new JSONObject();
			
			Auth.put("username", user);
			Auth.put("password",  pwd) ;  
			
			LOGD("Create Image " + img + " TAG:" + tag +  " " + user + "/" + pwd + " Auth:" + Auth);
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("NAME", img);
			params.put("TAG", tag);
			params.put("AUTHOBJ", Base64.encode(Auth.toString().getBytes(CoreTypes.CHARSET_UTF8)));

			// {"id":"latest","status":"Pulling from library/busybox"}
			// HTTP Not Found (404): {"message":"pull access denied for busybox_foo, repository does not exist or may require 'docker login'"}
			Object resp = rest.invoke(HTTPDestination.create(url, null, null,  keyStorePath, keyStorePassword), "CreateImage", params);
			LOGD("Server Replied: " + resp.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test05GCRGetTagsListFromPrivateRepo() {
		// https://us.gcr.io/v2/cloud-bots/tags/list
		// curl -u "_json_key:$(cat cloud-bots-key.json)" https://us.gcr.io/v2/cloud-bots/tags/list
		try {
			String user = "_json_key";
			String pwd = MockObjects.loadGCRJsonkeyFromCP();  
			String url = "https://us.gcr.io/v2/" + projectId + "/tags/list";
			
			
			// get token {"expires_in":43200,"issued_at":"2019-05-17T16:26:04.522809682-07:00","token":"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"}
			//url = "https://us.gcr.io/v2/token?service=gcr.io&scopre=registry:catalog:*";
			
			// {"errors":[{"code":"DENIED","message":"Cloud Resource Manager API has not been used in project 34585912628 before or it is disabled. Enable it by visiting https://console.developers.google.com/apis/api/cloudresourcemanager.googleapis.com/overview?project=34585912628 then retry. If you enabled this API recently, wait a few minutes for the action to propagate to our systems and retry."}]}
			//url = "https://us.gcr.io/v2/_catalog";
			
			LOGD("Search private registry " + url + " User: " +  user + " Pwd: " + pwd);
			
			WebClient wc = new WebClient(url);
			wc.setAuthorization(user, pwd);
			wc.logToStdOut(true);
			wc.setVerbosity(true);
			String resp = wc.doGet(); //headers);
			
			LOGD("TAGS-LIST Url " + url);
			LOGD("TAGS-LIST Resp: " + resp);
			/* IMAGE DETAILS String url = "https://us.gcr.io/v2/" + projectId + "/agentservices/tags/list";
			 * {
	"child": [],
	"manifest": {
		"sha256:01cd8d23add076fe534333a47185fd8262b0057e105428e4c9b147436f9156e0": {
			"imageSizeBytes": "75486559",
			"layerId": "",
			"mediaType": "application/vnd.oci.image.manifest.v1+json",
			"tag": [],
			"timeCreatedMs": "0",
			"timeUploadedMs": "1557327263586"
		},...
	},
	"name": "cloud-bots/agentservices",
	"tags": ["latest"]
}
			 */
			// {"child":["agentaggregator","agentservices","authenticationservice","ca_c1msaes","ca_c1mscisco","ca_lpaes","ca_rntaes","ca_rntcisco","ca_sfaes","ca_sfcisco","cannedmessages","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen","cc_gen-scb","cc_ucce","chatrouter","conversationaggregator","conversationhistory","conversationmonitor","conversationnotes","customeradministration","customeraggregator","dialogflow","draftcomposer","elasticstorage","emailanalyzer","emailpoller","emailrouter","emailsender","facebookbot","intentionaggregator","troposmsbot","twiliosmsbot","uploadserver","webchatbot"],"manifest":{},"name":"cloud-bots","tags":[]}
			JSONObject root = new JSONObject(resp);
			JSONArray tags = root.optJSONArray("child") ;
			for (int i = 0; i < tags.length(); i++) {
				String name = tags.getString(i);
				// "https://us.gcr.io/v2/" + projectId + "/TAG/tags/list";
				url = "https://us.gcr.io/v2/" + projectId + "/" + name + "/tags/list";
				wc.setUrl(url);
				LOGD("TAG_DETAILS " + name + " Url:" + url);
				
				// {"child":[],"manifest":{"sha256:0140b98fc2692f1b97d3fbaf5ef7d35a692c364aa4434bf2a6495dbae37a51ff":{"imageSizeBytes":"77198156","layerId":"","mediaType":"application/vnd.oci.image.manifest.v1+json","tag":[],"timeCreatedMs":"0","timeUploadedMs":"1557628571394"}, ... },"name":"cloud-bots/troposmsbot","tags":["latest"]}
				resp = wc.doGet();
				LOGD("TAG_DETAILS " + name + ": " + resp);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test06GCRAuthRepoWithJsonKey() {
		try {
			String server = "https://us.gcr.io";
			String pwd = MockObjects.loadGCRJsonkeyFromCP(); 
			
			// cleanup pwd
			JSONObject key = new JSONObject(pwd);
			
			LOGD("GCR JSON Key: " + key.toString(1));
			
			JSONObject Auth = new JSONObject();
			
			Auth.put("username", "_json_key");
			Auth.put("password",  key.toString());
			Auth.put("serveraddress", server);

			LOGD("GCR Check  Registry " + server + " auth payload:" + Auth.toString());
			
			Map<String, Object> params = new HashMap<String, Object>();

			/* Payload {
     		"username": "hannibal",
     		"password": "xxxx",
     		"serveraddress": "https://index.docker.io/v1/"
			} */
			// {"Status":"Login Succeeded","IdentityToken":""}
			// Unauthorized (401): {"message":"Get https://us.gcr.io/v2/: unauthorized: Not Authorized."}
			Object resp = rest.invoke(HTTPDestination.create(url, Auth.toString(), CoreTypes.CONTENT_TYPE_JSON,  keyStorePath, keyStorePassword), "CheckAuth", params);
			LOGD("Server Replied: " + resp.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test07DockerAuthRepo() {
		try {
			//String server = "https://index.docker.io/v2/";
			String server = "https://index.docker.io/v1/cloud/connector";
			LOGD("Docker repo auth  " + server);
			
			JSONObject Auth = new JSONObject();
			
			Auth.put("username", "cloud");
			Auth.put("password",  "Thenewcti1");
			Auth.put("serveraddress", server);

			LOGD("Docker Check Registry auth payload:" + Auth.toString());
			
			Map<String, Object> params = new HashMap<String, Object>();

			// Unauthorized (401): {"message":"Get https://registry-1.docker.io/v2/: unauthorized: incorrect username or password"}
			// {"Status":"Login Succeeded","IdentityToken":""}
			Object resp = rest.invoke(HTTPDestination.create(url, Auth.toString(), CoreTypes.CONTENT_TYPE_JSON,  keyStorePath, keyStorePassword), "CheckAuth", params);
			LOGD("Server Replied: " + resp.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/*
	 * curl "https://cloud.docker.com/api/repo/v1/inspect/v2/cloud/connector/tags/list/" -H "User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0" -H "Accept: application/json" -H "Accept-Language: en-US,en;q=0.5" --compressed -H "Referer: https://cloud.docker.com/repository/docker/cloud/connector/general" -H "X-CSRFToken: 0tsSoSkkuVfSBHUB1yPEUiDDM3fY5/difhBPAYBzCb4=" -H "DNT: 1" -H "Connection: keep-alive" -H "Cookie: FLAG_CONSOLIDATION=true; ajs_user_id="%"22a0dce1697e81461ebf4f3374593df14d"%"22; ajs_group_id=null; ajs_anonymous_id="%"22fddc2c3e-3b45-47d0-a206-f4b337fd999f"%"22; session_hint=1; cloudid=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsIng1YyI6WyJNSUlDK2pDQ0FwK2dBd0lCQWdJQkFEQUtCZ2dxaGtqT1BRUURBakJHTVVRd1FnWURWUVFERXpzeVYwNVpPbFZMUzFJNlJFMUVVanBTU1U5Rk9reEhOa0U2UTFWWVZEcE5SbFZNT2tZelNFVTZOVkF5VlRwTFNqTkdPa05CTmxrNlNrbEVVVEFlRncweE9UQXhNVEl3TURJeU5EVmFGdzB5TURBeE1USXdNREl5TkRWYU1FWXhSREJDQmdOVkJBTVRPMUpMTkZNNlMwRkxVVHBEV0RWRk9rRTJSMVE2VTBwTVR6cFFNbEpMT2tOWlZVUTZTMEpEU0RwWFNVeE1Pa3hUU2xrNldscFFVVHBaVWxsRU1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBcjY2bXkveXpHN21VUzF3eFQ3dFplS2pqRzcvNnBwZFNMY3JCcko5VytwcndzMGtIUDVwUHRkMUpkcFdEWU1OZWdqQXhpUWtRUUNvd25IUnN2ODVUalBUdE5wUkdKVTRkeHJkeXBvWGc4TVhYUEUzL2lRbHhPS2VNU0prNlRKbG5wNGFtWVBHQlhuQXRoQzJtTlR5ak1zdFh2ZmNWN3VFYWpRcnlOVUcyUVdXQ1k1Ujl0a2k5ZG54Z3dCSEF6bG8wTzJCczFmcm5JbmJxaCtic3ZSZ1FxU3BrMWhxYnhSU3AyRlNrL2tBL1gyeUFxZzJQSUJxWFFMaTVQQ3krWERYZElJczV6VG9ZbWJUK0pmbnZaMzRLcG5mSkpNalpIRW4xUVJtQldOZXJZcVdtNVhkQVhUMUJrQU9aditMNFVwSTk3NFZFZ2ppY1JINVdBeWV4b1BFclRRSURBUUFCbzRHeU1JR3ZNQTRHQTFVZER3RUIvd1FFQXdJSGdEQVBCZ05WSFNVRUNEQUdCZ1JWSFNVQU1FUUdBMVVkRGdROUJEdFNTelJUT2t0QlMxRTZRMWcxUlRwQk5rZFVPbE5LVEU4NlVESlNTenBEV1ZWRU9rdENRMGc2VjBsTVREcE1VMHBaT2xwYVVGRTZXVkpaUkRCR0JnTlZIU01FUHpBOWdEc3lWMDVaT2xWTFMxSTZSRTFFVWpwU1NVOUZPa3hITmtFNlExVllWRHBOUmxWTU9rWXpTRVU2TlZBeVZUcExTak5HT2tOQk5sazZTa2xFVVRBS0JnZ3Foa2pPUFFRREFnTkpBREJHQWlFQXFOSXEwMFdZTmM5Z2tDZGdSUzRSWUhtNTRZcDBTa05Rd2lyMm5hSWtGd3dDSVFEMjlYdUl5TmpTa1cvWmpQaFlWWFB6QW9TNFVkRXNvUUhyUVZHMDd1N3ZsUT09Il19.eyJlbWFpbCI6IiIsImV4cCI6MTU2MTgxNDYzMSwiaWF0IjoxNTU5MjIyOTA3LCJqdGkiOiJYUzhkMkpKb1B5Q0F4NXVoN0NXVHp3PT0iLCJzZXNzaW9uX2lkIjoiWFM4ZDJKSm9QeUNBeDV1aDdDV1R6dz09Iiwic3ViIjoiYTBkY2UxNjk3ZTgxNDYxZWJmNGYzMzc0NTkzZGYxNGQiLCJ1c2VyX2lkIjoiYTBkY2UxNjk3ZTgxNDYxZWJmNGYzMzc0NTkzZGYxNGQiLCJ1c2VybmFtZSI6ImMxYXMifQ.pfwoKtZMpbRJtGbk0MfbsebVGQr-DpujCC65qy76fBnoe32S53kGEpCXEwKQsdV3UqAR54gPuH6aVakc8QlIOgMTr7ldM8KW_CPZzr3BiSyVjzU8lQuVfxHX72ZHFy1JZYR2OpHKMENeYvgsA-tLYYZuaWkiN-XvCZiN3mWvWq6JbYTfrRGS7jDzaWdNWLvQYp3PVyjMiJuHarKsdRz7s9KMUJXk1Nw750cfav_RXrrmty8O4_VgKcYbmhf3OENUqYBZf_OPyg7pxqyGeMcriOskirlYSVbq80YR5jwj-Y2E-MK1_J3_23-K8c1Y4HSm2vZPYHMSOap5wGGUzr_73w; locsrftoken=rUDQ0EcJ8iXNl8Otq1-OzLTvWl7neWhZEYAkwtmUOIQ=; csrftoken=0tsSoSkkuVfSBHUB1yPEUiDDM3fY5/difhBPAYBzCb4=" -H "Pragma: no-cache" -H "Cache-Control: no-cache"
https://cloud.docker.com/api/repo/v1/inspect/v2/cloud/connector/tags/list/

HTTP/1.1 200 OK
Date: Thu, 30 May 2019 14:19:59 GMT
Content-Type: application/json; charset=utf-8
Content-Length: 142
X-XSS-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Docker-Distribution-Api-Version: registry/2.0
X-Frame-Options: deny
X-Tutum-Served-By: ae78b4f38b8f
Server: nginx
Strict-Transport-Security: max-age=31536000

{"name":"cloud/connector","tags":["ca_c1msaes","ca_c1mscisco","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen-scb","cc_gen","cc_ucce"]}

	 */
	@Test
	public void test08DockerHubListTagsFromPrivateRepo() {
		try {
			String repo = "cloud/connector";
			String url = "https://cloud.docker.com/api/repo/v1/inspect/v2/" + repo + "/tags/list/";
			LOGD("Docker hub list tags from  " + url);
			
			WebClient wc = new WebClient(url);
			wc.logToStdOut(true);
			wc.setVerbosity(true);
			wc.setAuthorization(dockerHubUser, dockerHubPwd);
			String resp = wc.doGet();
			
			LOGD("Server Replied: " + resp);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/*
	 * https://cloud.docker.com/v2/repositories/cloud/connector/tags/?page_size=25
curl "https://cloud.docker.com/v2/repositories/cloud/connector/tags/?page_size=25" -H "User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0" -H "Accept: application/json" -H "Accept-Language: en-US,en;q=0.5" --compressed -H "Referer: https://cloud.docker.com/repository/docker/cloud/connector/general" -H "X-CSRFToken: 0tsSoSkkuVfSBHUB1yPEUiDDM3fY5/difhBPAYBzCb4=" -H "DNT: 1" -H "Connection: keep-alive" -H "Cookie: FLAG_CONSOLIDATION=true; ajs_user_id="%"22a0dce1697e81461ebf4f3374593df14d"%"22; ajs_group_id=null; ajs_anonymous_id="%"22fddc2c3e-3b45-47d0-a206-f4b337fd999f"%"22; session_hint=1; cloudid=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsIng1YyI6WyJNSUlDK2pDQ0FwK2dBd0lCQWdJQkFEQUtCZ2dxaGtqT1BRUURBakJHTVVRd1FnWURWUVFERXpzeVYwNVpPbFZMUzFJNlJFMUVVanBTU1U5Rk9reEhOa0U2UTFWWVZEcE5SbFZNT2tZelNFVTZOVkF5VlRwTFNqTkdPa05CTmxrNlNrbEVVVEFlRncweE9UQXhNVEl3TURJeU5EVmFGdzB5TURBeE1USXdNREl5TkRWYU1FWXhSREJDQmdOVkJBTVRPMUpMTkZNNlMwRkxVVHBEV0RWRk9rRTJSMVE2VTBwTVR6cFFNbEpMT2tOWlZVUTZTMEpEU0RwWFNVeE1Pa3hUU2xrNldscFFVVHBaVWxsRU1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBcjY2bXkveXpHN21VUzF3eFQ3dFplS2pqRzcvNnBwZFNMY3JCcko5VytwcndzMGtIUDVwUHRkMUpkcFdEWU1OZWdqQXhpUWtRUUNvd25IUnN2ODVUalBUdE5wUkdKVTRkeHJkeXBvWGc4TVhYUEUzL2lRbHhPS2VNU0prNlRKbG5wNGFtWVBHQlhuQXRoQzJtTlR5ak1zdFh2ZmNWN3VFYWpRcnlOVUcyUVdXQ1k1Ujl0a2k5ZG54Z3dCSEF6bG8wTzJCczFmcm5JbmJxaCtic3ZSZ1FxU3BrMWhxYnhSU3AyRlNrL2tBL1gyeUFxZzJQSUJxWFFMaTVQQ3krWERYZElJczV6VG9ZbWJUK0pmbnZaMzRLcG5mSkpNalpIRW4xUVJtQldOZXJZcVdtNVhkQVhUMUJrQU9aditMNFVwSTk3NFZFZ2ppY1JINVdBeWV4b1BFclRRSURBUUFCbzRHeU1JR3ZNQTRHQTFVZER3RUIvd1FFQXdJSGdEQVBCZ05WSFNVRUNEQUdCZ1JWSFNVQU1FUUdBMVVkRGdROUJEdFNTelJUT2t0QlMxRTZRMWcxUlRwQk5rZFVPbE5LVEU4NlVESlNTenBEV1ZWRU9rdENRMGc2VjBsTVREcE1VMHBaT2xwYVVGRTZXVkpaUkRCR0JnTlZIU01FUHpBOWdEc3lWMDVaT2xWTFMxSTZSRTFFVWpwU1NVOUZPa3hITmtFNlExVllWRHBOUmxWTU9rWXpTRVU2TlZBeVZUcExTak5HT2tOQk5sazZTa2xFVVRBS0JnZ3Foa2pPUFFRREFnTkpBREJHQWlFQXFOSXEwMFdZTmM5Z2tDZGdSUzRSWUhtNTRZcDBTa05Rd2lyMm5hSWtGd3dDSVFEMjlYdUl5TmpTa1cvWmpQaFlWWFB6QW9TNFVkRXNvUUhyUVZHMDd1N3ZsUT09Il19.eyJlbWFpbCI6IiIsImV4cCI6MTU2MTgxNDYzMSwiaWF0IjoxNTU5MjIyOTA3LCJqdGkiOiJYUzhkMkpKb1B5Q0F4NXVoN0NXVHp3PT0iLCJzZXNzaW9uX2lkIjoiWFM4ZDJKSm9QeUNBeDV1aDdDV1R6dz09Iiwic3ViIjoiYTBkY2UxNjk3ZTgxNDYxZWJmNGYzMzc0NTkzZGYxNGQiLCJ1c2VyX2lkIjoiYTBkY2UxNjk3ZTgxNDYxZWJmNGYzMzc0NTkzZGYxNGQiLCJ1c2VybmFtZSI6ImMxYXMifQ.pfwoKtZMpbRJtGbk0MfbsebVGQr-DpujCC65qy76fBnoe32S53kGEpCXEwKQsdV3UqAR54gPuH6aVakc8QlIOgMTr7ldM8KW_CPZzr3BiSyVjzU8lQuVfxHX72ZHFy1JZYR2OpHKMENeYvgsA-tLYYZuaWkiN-XvCZiN3mWvWq6JbYTfrRGS7jDzaWdNWLvQYp3PVyjMiJuHarKsdRz7s9KMUJXk1Nw750cfav_RXrrmty8O4_VgKcYbmhf3OENUqYBZf_OPyg7pxqyGeMcriOskirlYSVbq80YR5jwj-Y2E-MK1_J3_23-K8c1Y4HSm2vZPYHMSOap5wGGUzr_73w; locsrftoken=rUDQ0EcJ8iXNl8Otq1-OzLTvWl7neWhZEYAkwtmUOIQ=; csrftoken=0tsSoSkkuVfSBHUB1yPEUiDDM3fY5/difhBPAYBzCb4=" -H "Pragma: no-cache" -H "Cache-Control: no-cache"
HTTP/1.1 200 OK
Date: Thu, 30 May 2019 14:19:58 GMT
Content-Type: application/json
Transfer-Encoding: chunked
X-Frame-Options: deny
Allow: GET, HEAD, OPTIONS
Server: nginx
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000

{"count": 9, "next": null, "previous": null, "results": [{"name": "ca_c1mscisco", "full_size": 82440910, "images": [{"size": 82440910, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57448519, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:14:22.142192Z", "image_id": null, "v2": true}, {"name": "ca_c1msaes", "full_size": 83523849, "images": [{"size": 83523849, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57368930, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:14:07.141839Z", "image_id": null, "v2": true}, {"name": "cc_ucce", "full_size": 80121721, "images": [{"size": 80121721, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57450489, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:13:50.289980Z", "image_id": null, "v2": true}, {"name": "cc_gen-scb", "full_size": 86620019, "images": [{"size": 86620019, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57450457, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:13:36.994282Z", "image_id": null, "v2": true}, {"name": "cc_gen", "full_size": 86585374, "images": [{"size": 86585374, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57450428, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:13:24.051493Z", "image_id": null, "v2": true}, {"name": "cc_finesse", "full_size": 85917363, "images": [{"size": 85917363, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57450399, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:13:09.872495Z", "image_id": null, "v2": true}, {"name": "cc_ciscocti", "full_size": 79931522, "images": [{"size": 79931522, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57343172, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:12:52.472236Z", "image_id": null, "v2": true}, {"name": "cc_cisco", "full_size": 81657263, "images": [{"size": 81657263, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57343146, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:12:39.392135Z", "image_id": null, "v2": true}, {"name": "cc_aes", "full_size": 81371902, "images": [{"size": 81371902, "architecture": "amd64", "variant": null, "features": null, "os": "linux", "os_version": null, "os_features": null}], "id": 57343119, "repository": 7190840, "creator": 3138902, "last_updater": 3138902, "last_updated": "2019-05-30T14:12:25.601680Z", "image_id": null, "v2": true}]}
	 */
	@Test
	public void test10DockerHubGetTagInfoFromPrivateRepo() {
		try {
			String repo = "cloud/connector";
			String url = "https://cloud.docker.com/v2/repositories/" + repo + "/tags/"; // ?page_size=25
			LOGD("Docker hub get tag info from  " + url);
			
			WebClient wc = new WebClient(url);
			wc.logToStdOut(true);
			wc.setVerbosity(true);
			wc.setAuthorization(dockerHubUser, dockerHubPwd);
			String resp = wc.doGet();
			
			LOGD("TAG-INFO: Server Replied: " + resp);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
