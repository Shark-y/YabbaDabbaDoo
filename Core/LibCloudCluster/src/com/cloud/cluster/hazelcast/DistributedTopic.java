package com.cloud.cluster.hazelcast;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.ClusterTopicEvent;
import com.cloud.cluster.IClusterTopic;
import com.cloud.cluster.IClusterTopicListener;
import com.cloud.cluster.hazelcast.DistributedTopic;
import com.cloud.cluster.hazelcast.HzClusterInstance;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

/**
 * Cluster wide topic for Pub/Sub operations.
 * @author VSilva
 *
 */
public class DistributedTopic implements IClusterTopic {
	private ITopic<ClusterTopicEvent> topic;
	private HazelcastInstance hazelcastInstance;
	private String nodeId;
	
	/**
	 * Cluster wide Pub/Sub {@link DistributedTopic}.
	 * @param name Name of the topic.
	 * @param listener Message listener. See {@link IClusterTopicListener}.
	 */
	public DistributedTopic(HazelcastInstance hazelcastInstance, String nodeId, String name, final IClusterTopicListener listener) {
		if ( listener == null) 	throw new IllegalArgumentException("Topic listener cannot be null.");
		if ( nodeId == null) 	throw new IllegalArgumentException("Node Id cannot be null.");
		if ( name == null) 		throw new IllegalArgumentException("Topic name cannot be null.");
		
		this.hazelcastInstance 	= hazelcastInstance;
		this.topic 				= hazelcastInstance.getTopic( name );
		this.nodeId				= nodeId;
		
		// add a listener
	    topic.addMessageListener(  new MessageListener<ClusterTopicEvent>() {
			@Override
			public void onMessage(Message<ClusterTopicEvent> e) {
				try {
					// Don't send the message to the publisher (local member).
					if ( ! e.getPublishingMember().localMember() ) {
						JSONObject root = new JSONObject(e.getMessageObject().getPayload());
						listener.onMessage(new ClusterTopicEvent(e.getPublishingMember().getUuid(), root));
					}
				} catch (JSONException e2) {
					HzClusterInstance.log.error("Cluster::onMessage", e2);
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterTopic#publish(org.json.JSONObject)
	 */
	@Override
	public void publish (JSONObject message) {
		// inject member attributes: sender, URL.
		Map<String, Object> attrs = hazelcastInstance.getConfig().getMemberAttributeConfig().getAttributes();
		
		try {
			message.put(CloudCluster.KEY_SENDER, attrs.get(CloudCluster.KEY_CTX_PATH));
			
			// optional
			if ( attrs.containsKey(CloudCluster.KEY_CTX_URL) )
				message.put(CloudCluster.KEY_SENDER_URL, attrs.get(CloudCluster.KEY_CTX_URL));
			
		} catch (JSONException e) {
			HzClusterInstance.log.error("Publish: ", e);
		}
		
		topic.publish(new ClusterTopicEvent(nodeId, message));
	}

	
	/* (non-Javadoc)
	 * @see com.cloud.cluster.hazelcast.IClusterTopic#destroy()
	 */
	@Override
	public void destroy () {
		topic.destroy();
	}
}