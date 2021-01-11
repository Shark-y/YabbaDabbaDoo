package com.cloud.cluster;

import org.json.JSONObject;

/**
 * This interface is used to publish cluster wide message.
 * <pre>
 * 
 * IClusterTopic topic = CloudCluster.getInstance().getClusterInstance().getTopic("FOO");
 * topic.publish(SOME-JSON);
 * 
 * </pre>
 * @author VSilva
 *
 */
public interface IClusterTopic {

	/**
	 * Publish a cluster wide message as a {@link JSONObject}.
	 * @param message Message as {@link JSONObject}.
	 */
	public abstract void publish(JSONObject message);

	/**
	 * Topic Cleanup
	 */
	public abstract void destroy();

}