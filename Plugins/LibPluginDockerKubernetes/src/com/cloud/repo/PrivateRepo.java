package com.cloud.repo;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import com.cloud.core.w3.RestException;
import com.cloud.core.w3.WebClient;

import org.json.cloud.JSONFileSerializer.BaseNode;

/**
 * Encapsulates repository information.Repositories can be of 3 types:
 * <ul>
 * <li> Google Image Registry (GCR)
 * <li> Docker Hub Private repos
 * <li> HELM privates.
 * </ul>
 * 
 * @author VSilva
 *
 */
public class PrivateRepo  implements BaseNode {

	/** Types of repositories */
	public enum RepoType { GOOGLE, DOCKER, HELM }
	
	private String url;
	private String name;
	private String user;
	private String password;
	//Must use String else the serializer will return an EMPTY object {} private RepoType type;
	private String type;
	private final WebClient wc ;
	
	public PrivateRepo() {
		type 	= RepoType.DOCKER.name();
		wc 		= new WebClient();
	}
	
	/**
	 * Construct
	 * @param type Repo type. See {@link RepoType}.
	 * @param url Repo URL.
	 * @param name Repo unique name or id.
	 * @param user Optional user if authentication is required.
	 * @param password Optional password.
	 */
	public PrivateRepo(final String type, final String url, final String name, final String user, final String password) {
		super();
		this.type		= type; //RepoType.valueOf(type);
		this.url 		= url;
		this.name 		= name;
		this.user 		= user;
		this.password 	= password;
		this.wc 		= new WebClient();
	}
	
	public /*RepoType*/ String getType () {
		return type;
	}
	
	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String getId() {
		return name;
	}
	
	@Override
	public String toString() {
		return name + "@" + url;
	}
	
	/**
	 * Get TAGs (images) from GCR, DOCKER or HELM. Return JSON:
	 * <ul>
	 * <li>GOOGLE <pre>{"child":["agentaggregator","ca_rntcisco","customeradministration","customeraggregator","dialogflow"],"manifest":{},"name":"cloud-bots","tags":[]}</pre>
	 * <li>DOCKER: <pre>{"name":"cloud/connector","tags":["ca_c1msaes","ca_c1mscisco","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen-scb","cc_gen","cc_ucce"]}</pre>
	 * <li>HELM <pre> { "entries": {
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
	} </pre>
	 * </ul>
	 * @return JSON (see above).
	 * @throws RestException on Docker client error.
	 * @throws IOException On I/O/Network errors.
	 * @throws JSONException On JSON parsing errors.
	 */
	public JSONObject fetchTags () throws RestException, IOException, JSONException {
		if ( type.equals(RepoType.GOOGLE.name())) {
			return fetchTagsGCR();
		}
		if ( type.equals(RepoType.DOCKER.name())) {
			return fetchTagsDocker();
		}
		if ( type.equals(RepoType.HELM.name())) {
			return fetchTagsHELM();
		}
		throw new IOException("Invalid REPO type " + type + " for " + name);
	}
	
	/**
	 * curl -u "_json_key:$(cat cloud-bots-key.json)" https://us.gcr.io/v2/cloud-bots/tags/list
	 * @return JSON: {"child":["agentaggregator","agentservices","authenticationservice","ca_c1msaes","ca_c1mscisco","ca_lpaes","ca_rntaes","ca_rntcisco","ca_sfaes","ca_sfcisco","cannedmessages","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen","cc_gen-scb","cc_ucce","chatrouter","conversationaggregator","conversationhistory","conversationmonitor","conversationnotes","customeradministration","customeraggregator","dialogflow","draftcomposer","elasticstorage","emailanalyzer","emailpoller","emailrouter","emailsender","facebookbot","intentionaggregator","troposmsbot","twiliosmsbot","uploadserver","webchatbot"],"manifest":{},"name":"cloud-bots","tags":[]}
	 * @throws RestException on HTTP errors.
	 * @throws IOException On network I/O errors.
	 * @throws JSONException On JSON parse errors.
	 */
	private JSONObject /*Array*/ fetchTagsGCR () throws RestException, IOException, JSONException {
		// curl -u "_json_key:$(cat cloud-bots-key.json)" https://us.gcr.io/v2/cloud-bots/tags/list
		final String baseUrl = url + name + "/tags/list";
		
		wc.setUrl(baseUrl);
		wc.setAuthorization(user, password);

		final String resp = wc.doGet(); 
		if ( wc.getResponseCode() >= 400) {
			throw new RestException(wc.getStatus(), wc.getHeaderAtIndex("Content-Type", 0), resp);
		}
		// {"child":["agentaggregator","agentservices","authenticationservice","ca_c1msaes","ca_c1mscisco","ca_lpaes","ca_rntaes","ca_rntcisco","ca_sfaes","ca_sfcisco","cannedmessages","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen","cc_gen-scb","cc_ucce","chatrouter","conversationaggregator","conversationhistory","conversationmonitor","conversationnotes","customeradministration","customeraggregator","dialogflow","draftcomposer","elasticstorage","emailanalyzer","emailpoller","emailrouter","emailsender","facebookbot","intentionaggregator","troposmsbot","twiliosmsbot","uploadserver","webchatbot"],"manifest":{},"name":"cloud-bots","tags":[]}
		JSONObject root = new JSONObject(resp);
		//JSONArray tags 	= root.optJSONArray("child");
		return root; //tags;
	}
	
	/**
	 * GET https://cloud.docker.com/api/repo/v1/inspect/v2/cloud/connector/tags/list/
	 * @return JSON: {"name":"cloud/connector","tags":["ca_c1msaes","ca_c1mscisco","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen-scb","cc_gen","cc_ucce"]}
	 */
	private JSONObject /*Array*/ fetchTagsDocker () throws RestException, IOException, JSONException {
		// https://cloud.docker.com/api/repo/v1/inspect/v2/cloud/connector/tags/list/
		final String baseUrl = url + name + "/tags/list";
		
		wc.setUrl(baseUrl);
		wc.setAuthorization(user, password);

		final String resp = wc.doGet(); 
		if ( wc.getResponseCode() >= 400) {
			throw new RestException(wc.getStatus(), wc.getHeaderAtIndex("Content-Type", 0), resp);
		}
		
		// {"name":"cloud/connector","tags":["ca_c1msaes","ca_c1mscisco","cc_aes","cc_cisco","cc_ciscocti","cc_finesse","cc_gen-scb","cc_gen","cc_ucce"]}
		JSONObject root = new JSONObject(resp);
		//JSONArray tags 	= root.optJSONArray("tags");
		return root; //tags;
	}

	/**
	 * Fetch chart info from a <b>PUBLIC</b> HELM repo @ http://HOST/index.yaml
	 * @return <pre> { "entries": {
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
	} </pre>

	 * @throws IOException on Net/IO errors.
	 * @throws RestException On W3 errors.
	 * @throws JSONException On JSON parse errors.
	 */
	private JSONObject fetchTagsHELM () throws IOException, RestException, JSONException {
		// https://charts_cloud.storage.googleapis.com/index.yaml
		final String baseUrl = (url.endsWith("/") ? url  : url + "/" ) + "index.yaml";
		wc.setUrl(baseUrl);

		final String resp = wc.doGet(); 
		
		if ( wc.getResponseCode() >= 400) {
			throw new RestException(wc.getStatus(), wc.getHeaderAtIndex("Content-Type", 0), resp);
		}
		// convert to JSON  using SnakeYaml
		Yaml yaml 			= new Yaml();
		JSONObject root 	= new JSONObject((Map)yaml.load(resp));
		
		/* JSON FORMAT
		 * { "entries": {
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
		//JSONObject entries = root.getJSONObject("entrties");
		return root;
	}
}
