package com.cloud.core.services;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.services.CloudServices;
import com.cloud.core.services.ClusterTool;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.ProfileManager;
import com.cloud.core.io.ZipTool;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.NodeConfiguration.RunMode;

/**
 * A helper class to decouple the cluster stuff from the {@link CloudServices} class.
 * <b>Note: Clustering is optional in some products</b>.
 * 
 * @author VSilva
 * @version 1.0.1 2/18/2019 removed com.cloud.cluster.* imports for products that have clustering disabled or don't support it.
 *
 */
public class ClusterTool {

	private static final Logger 		log = LogManager.getLogger(ClusterTool.class);
	
	/** Topic message keys */
	public static final String TOPIC_MSGTYPE = "messageType";
	public static final String TOPIC_PROFILE = "profileName";
	public static final String TOPIC_PROFB64 = "profileBase64";
	public static final String TOPIC_CRED	 = "credential";

	/**
	 * Initialize or shutdown the cluster service.
	 * @param config Service configuration. See {@link NodeConfiguration}.
	 * @param force If true force a shutdown the cluster service.
	 */
	static void clusterUpdate (NodeConfiguration config , boolean force) throws IOException {
		Map<String, Object> params = config.getClusterParams();
	
		log.debug("Cluster Initialize: Cluster enabled: " + config.isClusterEnabled() + " Force: " + force + " Params: " + params);

		if ( !config.isClusterEnabled() && !force) {
			// in case of run mode switch.
			com.cloud.cluster.CloudCluster.getInstance().shutdown();
			return;
		}
		//log.info("Cluster Initialize: Params: " + params);
		com.cloud.cluster.CloudCluster.getInstance().initialize(params);
		
		// set the node status
		CloudServices.clusterUpdateMemberStatus();
		
		// used to receive cluster wide messages
		clusterSubscribeToGlobalTopic();
	} 

	/**
	 * Shutdown cluster.
	 */
	static void clusterShutdown () {
		com.cloud.cluster.CloudCluster.getInstance().shutdown();
	}
	
	/**
	 * Subscribe to a cluster wide topic to receive administration messages.
	 */
	private static void clusterSubscribeToGlobalTopic() {
		/* 4/21/2017 vsilva - TODO remove Hazelcast
		com.hazelcast.core.HazelcastInstance hazelcast 		= CloudCluster.getInstance().getClusterInstance().getClusterProvider();
		com.hazelcast.core.ITopic<ClusterTopicEvent> topic 	= hazelcast.getTopic(CloudCluster.CLOUD_CLUSTER_TOPIC);
		
		// add a listener
	    topic.addMessageListener(  new com.hazelcast.core.MessageListener<ClusterTopicEvent>() {
			@Override
			public void onMessage(com.hazelcast.core.Message<ClusterTopicEvent> e) {
				try {
					// Don't send the message to the publisher (local member).
					if ( ! e.getPublishingMember().localMember() ) {
						JSONObject root = new JSONObject(e.getMessageObject().getPayload());
						
						//System.out.println("**CONSUME " + root);
						clusterConsumeBroadcastMessage(root);
					}
				} catch (JSONException e2) {
					log.error("Cluster:onMessage", e2);
				}
			}
		}); */
		com.cloud.cluster.CloudCluster.getInstance().getClusterInstance().getTopic(com.cloud.cluster.CloudCluster.CLOUD_CLUSTER_TOPIC, new com.cloud.cluster.IClusterTopicListener() {
			@Override
			public void onMessage(com.cloud.cluster.ClusterTopicEvent ev) {
				com.cloud.cluster.ClusterMember local = com.cloud.cluster.CloudCluster.getInstance().getClusterInstance().getLocalMember();
				if ( local == null) {
					log.error("GetTopic " + com.cloud.cluster.CloudCluster.CLOUD_CLUSTER_TOPIC + " OnMessage: Local memeber cannot be null. This is a bug.");
					return;
				}
				try {
					// Don't consume if publisher (local member).
					if ( ! local.getUuid().equals(ev.getPublisherId()) ) {
						clusterConsumeBroadcastMessage(new JSONObject(ev.getPayload()));
					}
				} catch (JSONException e2) {
					log.error("Cluster:onMessage", e2);
				}
			}
		});
	}

	/**
	 * Consume a cluster message. Samples:<pre>
	 * {"messageType":"configure","profileName":"P1","base64Profile":"...","credential":"516b9783fca517eecbd1d064da2d165310b19759"}
	 * </pre>
	 * @param root
	 */
	static void clusterConsumeBroadcastMessage (JSONObject root) {
		try {
			NodeConfiguration config 	= CloudServices.getNodeConfig();
			String type 				= root.getString(TOPIC_MSGTYPE);

			// remote configure
			if ( type.equals("configure")) {
				String profile 	= root.getString(TOPIC_PROFILE);	// profile name
				String profB64 	= root.getString(TOPIC_PROFB64);	// profile data (b64)
				String cred		= root.getString(TOPIC_CRED);		// sysadmin pwd (hashed)
				ProfileManager pm = config.getProfileManager();

				//System.out.println("CONF prof=" + profile + " Cred=" + cred + " Pb64=" + profB64);
				
				// reject if already configured.
				/*
				if ( CloudAdapter.isConfigured()) {
					CloudAdapter.log.warn("Remote Configure: Node already configured. Abort.");
					CloudAdapter.clusterUpdateMemberStatus(CloudAdapter.SC_OFFLINE, profile + ". Node already configued.");
					return;
				} */
				
				// set profile, credential & save config
				config.setSysAdminPwd(cred);
				config.setConnectionProfileName(profile);
				config.setRunMode(RunMode.CLUSTER);
				config.save();
				
				// profile not found, install it ...
				if ( pm.find(profile) == null ) {
					//ProfileDescriptor pd = pm.add(profile);
					// unzip b64 into profiles home user.home/.cloud/CloudAdapter/Profiles
					ZipTool.unzipB64(profB64, pm.getProfilesHome());
					
					CloudServices.clusterUpdateMemberStatus(CloudServices.SC_OK, "Installed " + profile + ".");
					
					// must reload the profiles or the node will fail to start.
					pm.reload();
				}
				else {
					CloudServices.clusterUpdateMemberStatus(CloudServices.SC_OFFLINE, profile + " already exists.");
				}
				
			}
		} catch (Exception e) {
			log.error("BCAST Consume ",  e);
		}
	}

}
