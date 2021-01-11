package com.cloud.cluster;


/**
 * Implement this to listen to a cluster wide message.
 * 
 * @author VSilva
 * @version 1.0.2
 *
 */
public interface IClusterTopicListener {
	
	/**
	 * Cluster wide message.
	 * @param ev See {@link ClusterTopicEvent}..
	 */
	void onMessage(ClusterTopicEvent ev) ;
}