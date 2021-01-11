package junit.docker;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.yaml.snakeyaml.Yaml;

import com.cloud.core.security.SecurityTool;
import com.cloud.core.w3.WebClient;
import com.cloud.repo.PrivateRepo;
import com.cloud.repo.PrivateRepoManager;
import com.cloud.repo.PrivateRepo.RepoType;

import org.json.cloud.JSONFileSerializer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRepoManager {
	
	static void LOGD(String text) {
		System.out.println("[REPO-MGR] "  + text);
	}

	@Test
	public void test01CreateReposJSON() {
		try {
			String base = System.getProperty("user.home") + File.separator + ".cloud" + File.separator + "CloudAdapter";
			String key = MockObjects.loadGCRJsonkeyFromCP();
			String name = "cloud-bots";
			
			Map<String, String> config = new HashMap<String, String>();
			config.put(JSONFileSerializer.CONFIG_BASEPATH, base);
			
			PrivateRepoManager.init(config);
			PrivateRepoManager.loadNodes();
			LOGD("ONLOAD: " + PrivateRepoManager.getRepos());
			
			// GCR - Images
			PrivateRepoManager.addRepo(RepoType.GOOGLE.name(), "https://us.gcr.io/v2/", name , "_json_key", key);
			
			// DOCKER HUB - Images
			name = "cloud/connector";
			PrivateRepoManager.addRepo(RepoType.DOCKER.name(), "https://cloud.docker.com/api/repo/v1/inspect/v2/", name , MockObjects.dockerHubUser, MockObjects.dockerHubPwd);

			// HELM - Charts
			name = "cloud-charts";
			PrivateRepoManager.addRepo(RepoType.HELM.name(), "https://charts_cloud.storage.googleapis.com", name , "", "");
			
			PrivateRepoManager.save();
			LOGD("ONSAVE: " + PrivateRepoManager.getRepos());
			
			//Map<String, Object> repos = PrivateRepoManager.getRepos();
			PrivateRepo repo = PrivateRepoManager.getRepo(name);
			LOGD(name + " password " + repo.getPassword());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test02GetImageTagsFromGoogleAndDockerRepos() {
		try {
			// curl -u "_json_key:$(cat cloud-bots-key.json)" https://us.gcr.io/v2/cloud-bots/tags/list

			String name = "cloud-bots";
			//PrivateRepo repo = PrivateRepoManager.getRepo(name);
			
			JSONObject obj = PrivateRepoManager.fetchTags(name);
			LOGD("GCR TAGS for " + name + " " + obj);
			
			name = "cloud/connector";
			obj = PrivateRepoManager.fetchTags(name);
			LOGD("DOCKER TAGS for " + name + " " + obj);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/* HELM charts bucket from https://charts_cloud.storage.googleapis.com/index.yaml
apiVersion: v1
entries:
ca-c1msaes:
- apiVersion: v1
appVersion: "1"
created: "2019-06-05T14:55:44.228253965-04:00"
description: Cloud Connector ca_c1msaes
digest: aa2091f4456a949d9d299596d24f6bf30f9ecf776aae479981b0e585ad4821e8
home: https://www.convergeone.com/
icon: https://www.convergeone.com/hubfs/Convergeone_September2017_Theme/Images/C1-logo_35H.png
maintainers:
- email: vsilva@convergeone.systems
name: vsilva
name: ca-c1msaes
urls:
- https://charts_cloud.storage.googleapis.com/ca-c1msaes-20190603.tgz
version: "20190603" */			
	@Test
	public void test03GetHELMChartInfoFromGoogleBucket () {
		try {
			// required for SSL errors
			SecurityTool.disableClientSSLVerificationFromHttpsURLConnection();
			
			// Bucket: charts_cloud
			String uri = "https://charts_cloud.storage.googleapis.com/index.yaml";
			WebClient wc = new WebClient(uri);
			String str = wc.doGet();

			/* JSON FORMAT
			 * {
 "entries": {
  "ca-rntcisco": [{
   "maintainers": [{
    "name": "vsilva",
    "email": "vsilva@convergeone.systems"
   }],
   "appVersion": "1",
   "urls": ["https://charts_cloud.storage.googleapis.com/ca-rntcisco-20190603.tgz"],
   "apiVersion": "v1",
   "created": "2019-06-05T14:55:44.230717618-04:00",
   "digest": "6da66a05bb8362c630f0ff4acc9a8441f34604cec2efe8db2e88de42f1bc73f2",
   "icon": "https://www.convergeone.com/hubfs/Convergeone_September2017_Theme/Images/C1-logo_35H.png",
   "name": "ca-rntcisco",
   "description": "Cloud Connector ca_rntcisco",
   "version": "20190603",
   "home": "https://www.convergeone.com/"
  }],
  "cc-finesse": [{
	...
  }]
 },
 "apiVersion": "v1",
 "generated": "2019-06-05T14:55:44.226549632-04:00"
}
			 */
			
			/* Use SnakeYAml to convert to JSON */
			Yaml yaml = new Yaml();
			JSONObject foo = new JSONObject((Map)yaml.load(str));
			System.out.println(foo.toString(1));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test04GetChartTagsFromHELMRepo() {
		try {

			String name = "cloud-charts";
			//PrivateRepo repo = PrivateRepoManager.getRepo(name);
			
			JSONObject obj = PrivateRepoManager.fetchTags(name);
			LOGD("HELM CHART TAGS for " + name + " " + obj);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
