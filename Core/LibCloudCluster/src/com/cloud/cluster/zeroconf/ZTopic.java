package com.cloud.cluster.zeroconf;


import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.ClusterTopicEvent;
import com.cloud.cluster.IClusterTopic;
import com.cloud.cluster.IClusterTopicListener;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroDescriptorObject;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.cluster.multicast.ZeroDescriptorObject.UpdateType;
import com.cloud.cluster.zeroconf.ZTopic;
import com.cloud.core.io.ObjectCache;
import com.cloud.core.io.ObjectCache.ObjectType;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Database implementation of {@link IClusterTopic}.
 * <pre>
 * IClusterTopic topic = cluster.getClusterInstance().getTopic("TOPIC1", new IClusterTopicListener() {
 *  @Override
 *  public void onMessage(JSONObject obj) {
 *    LOGD("Got JSON message " + obj);
 *  }
 * });
 * 
 * topic.publish(new JSONObject(json));
 * </pre>
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class ZTopic implements IClusterTopic  {

	static final Logger log 					= LogManager.getLogger(ZTopic.class);
		
	private String 					nodeId;
	private String 					name;
	private IClusterTopicListener 	listener;
	private ZeroConfDiscovery 		ds;
	
	/**
	 * Construct a distributed topic.
	 * @param cache An {@link ObjectCache} used to store the topic.
	 * @param ds The {@link ZeroConfDiscovery} service used to send messages.
	 * @param nodeId The unique ID of the node that created the topic.
	 * @param name The topic name.
	 * @param listener A message listener. See {@link IClusterTopicListener}.
	 */
	public ZTopic(ObjectCache cache, ZeroConfDiscovery ds, String nodeId, String name, IClusterTopicListener listener) {
		super();
		this.ds 		= ds;
		this.name 		= name;
		this.listener 	= listener;
		this.nodeId 	= nodeId;
		if ( !cache.containsKey(name)) {
			try {
				// replicate
				ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_NEW, ObjectType.T_TOPIC, name));
			} catch (IOException e) {
				log.error("Topic.New(" + name + ")", e);
			}
		}
	}
	
	/**
	 * This method gets invoked by {@link CloudCluster} -> {@link DBClusterInstance} -> {@link ZTopic} ~ 2/sec
	 * @throws JSONException 
	 */
	public void dispatch (Object value) throws JSONException {
		if ( listener != null ) {
			final String publisher		= nodeId; 
			listener.onMessage(new ClusterTopicEvent(publisher, new JSONObject(value.toString())));
		}
		else {
			log.error("Dispatch TOPIC " + name + " can't dispatch. Listener is null");
		}
	}
	
	@Override
	public void publish(JSONObject message) {
		try {
			// replicate once
			ds.send(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.PUBLISH, name, null, message.toString()));
		} catch (IOException ex) {
			log.error("Topic.Publish(" + name + ")", ex);
		}
		
	}

	@Override
	public void destroy() {
	}

	@Override
	public String toString() {
		return "node=" + nodeId;
	}
}
