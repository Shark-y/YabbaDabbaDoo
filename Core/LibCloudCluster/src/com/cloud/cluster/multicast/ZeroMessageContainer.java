package com.cloud.cluster.multicast;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.ClusterMember;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroMessageContainer;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Collection used to store messages received via {@link ZeroDescriptorServiceUp}.
 * <ul>
 * <li> Accepts messages of type {@link MessageType} SERVICE only.
 * <li> No duplicates allowed.
 * <li> Messages can be updated and removed if expired.
 * </ul>
 * @author VSilva
 *
 */
public final class ZeroMessageContainer {
	static final Logger log 		= LogManager.getLogger(ZeroMessageContainer.class);
			
	// Received messages
	private final List<String> messages;

	public ZeroMessageContainer() {
		messages 	= new CopyOnWriteArrayList<String>();
	}
	
	/**
	 * Add a message. No duplicates allowed.
	 * @param message Service JSON descriptor: {"uuid" : "cfa3b034-660d-4be0-90f4-4f6b997aa008", "messageType" : "SERVICE", "timeCreated": 1234, "attributes": {...}...}
	 * @throws JSONException on JSON errors.
	 */
	public void add (final String message) throws JSONException {
		// {"uuid" : "cfa3b034-660d-4be0-90f4-4f6b997aa008", "messageType" : "SERVICE", "timeCreated": 1234, ...}
		final JSONObject root 	= new JSONObject(message);
		final String type		= root.optString(ZeroConfDiscovery.JSONK_MTYPE);
		final String id 		= root.getString(ZeroConfDiscovery.JSONK_ID);
		
		// accept SERVICE messages only. No duplicates.
		if ( isServiceType(type) ) {
			// reject duplicates
			if ( id == null) {
				log.error ("ADD-MESSAGE: Cannot find an id (UUID) in message: " + message);
				return;
			}
			if ( !exists(id) ) { 
				messages.add(message);
			}
			else {
				log.trace("Updating " + id  + " with " + message);
				update(id, message);
			}
		}
		else {
			log.trace("Rejected message (Invalid type) " + message);
		}
	}

	/**
	 * Remove a message from the queue based on UUID.
	 * @param uuid The message id (uuid).
	 * @throws JSONException On JSON Errors.
	 */
	public void remove (final String uuid) throws JSONException {
		for (String str : messages) {
			JSONObject m = new JSONObject(str);
			if ( m.getString(ZeroConfDiscovery.JSONK_ID).equals(uuid)) {
				messages.remove(str);
			}
		}
	}
	
	private boolean isServiceType (String type) {
		return type != null && type.contains("SERVICE_") ? true : false;
	}
	
	/**
	 * Check if a message exists in the queue.
	 * @param id Unique ID of the message to check.
	 * @return True if exists.
	 * @throws JSONException On JSON errors.
	 */
	public boolean exists (final String id) throws JSONException {
		for (String str : messages) {
			JSONObject m = new JSONObject(str);
			if ( m.getString(ZeroConfDiscovery.JSONK_ID).equals(id)) {
				return true;
			}
		}
		return false;
	}

	void update (final String id, final String message) throws JSONException {
		for (String str : messages) {
			JSONObject m = new JSONObject(str);
			if ( m.getString(ZeroConfDiscovery.JSONK_ID).equals(id)) {
				messages.remove(str);
				messages.add(message);
			}
		}
	}

	/**
	 * Clean expired messages
	 */
	public void cleanup ()  {
		try {
			for (String str : messages) {
				final JSONObject m 	= new JSONObject(str);
				final String id 	= m.optString(ZeroConfDiscovery.JSONK_ID);
				final long sent 	= m.getLong(ZeroConfDiscovery.JSONK_TIME_SEN);
				final long now	 	= System.currentTimeMillis();
				final long EXP 		= ZeroConfDiscovery.DEFAULT_SEND_FREQUENCY * 2000; // twice the default mcast frequency // 600000 ; 
				
				// check expiration: 10m
				if ( (now - sent) > EXP) {
					log.debug("Removing expired message " + id + " Sent: " + sent + " Now: " + now + " ExpInt: " + EXP);
					messages.remove(str);
				}
			}
		} catch (Exception e) {
			log.error("ZeroConf message cleanup.", e);
		}
	}
	
	/**
	 * Build a {@link JSONArray} of service descriptors using the Cluster member format.
	 * @return {@link ClusterMember} JSON format: [{"uuid":"id", "address":"/192.168.30.1:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"}...},...]
	 * @throws JSONException
	 */
	public JSONArray toJSON () throws JSONException {
		JSONArray array = new JSONArray();
		for (String str : messages) {
			array.put(new JSONObject(str));
		}
		return array;
	}
}