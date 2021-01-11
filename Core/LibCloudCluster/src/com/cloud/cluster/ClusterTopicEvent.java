package com.cloud.cluster;

import java.io.Serializable;

import org.json.JSONObject;

/**
 * An event sent via topic to all members of the cluster.
 * @author VSilva
 *
 */
public class ClusterTopicEvent implements Serializable {
	private static final long serialVersionUID = -6420831017564114682L;
	
	// java.io.NotSerializableException: org.json.JSONObject
	// FIXME: This must be Serializable  -  JSONObject payload;
	String publisherId;
	String payload;
	
	public ClusterTopicEvent(String publisherId, JSONObject payload) {
		if ( publisherId == null) {
			throw new IllegalArgumentException("Topic publisher id cannot be null!");
		}

		if ( payload == null) {
			throw new IllegalArgumentException("Topic payload cannot be null!");
		}
		this.publisherId	= publisherId;
		this.payload 		= payload.toString();
	}
	
	public String getPayload() {
		return payload;
	}
	
	public String getPublisherId() {
		return publisherId;
	}

	@Override
	public String toString() {
		return publisherId + "@" + payload;
	}
}