package com.cloud.core.types;

import org.json.JSONException;
import org.json.JSONObject;


public class OMEvent {

	/**
	 * Events from the Contact Center to the Chat service
	 * @author vsilva
	 *
	 */
	public enum EventType 
	{
		submitSkill				// Submit skill: adapter -> chat service
		, agentAssign 			// Assign an agent to a chat: agent -> adapter -> chat service 
		, agentAssignByANI 		// accept a voice call: agent -> adapter -> chat service 
		, agentAssignBySession	// agent -> adapter -> chat service	
		, sessionReleased		// chat service -> adapter -> agent visitor closes window
		, releaseByDNIS			// agent -> adapter -> chat service 
		, transfer				// transfer a chat: Agent -> adapter -> Chat service
		, blindTransfer			// chat service -> adapter -> agent 
		, getSessionByDNIS		// adapter -> chat service
		, statusResponse		// Status response type: adapter -> agent
		, contextDestoyed		// Server stopping/sleeping
		, sessionAborted
		, setAgentState			// Set the state of the agent: { "agent" : "vlad", "type" : "setAgentState" , "channel" : "CHAT" , "available" : True, "agentId" : "2037"}
	};
	
	public static final int STATUS_OK 				= 200;
	public static final int STATUS_SERVER_ERROR 	= 500;
	public static final int STATUS_SERVER_OFFLINE 	= 501;
	
	/**
	 * These keys apply to LP Only :(
	 */
	public static final String KEY_TYPE 		= "type";
	public static final String KEY_STATUS 		= "status";
	public static final String KEY_MESSAGE 		= "message";
	public static final String KEY_DNIS_L 		= "dnis";
	public static final String KEY_DNIS_U 		= "DNIS";
	public static final String KEY_SESSION 		= "session";
	public static final String KEY_AGENT 		= "agent";			// agent or xfer originator
	public static final String KEY_REQUIREAGENT	= "requireAgent";	// agent (xfer receiver)
	public static final String KEY_REQUIRESKILL	= "requireSkill";	// agent 2 skill xfer
	public static final String KEY_REASON 		= "reason";
	public static final String KEY_WORKITEM_ID	= "workItemId";
	public static final String KEY_AGENT_ID		= "agentId";

	protected JSONObject payload;
	EventType type;
	
	public OMEvent(EventType type, JSONObject payload) {
		super();
		this.payload 	= payload;
		this.type 		= type;
	}
	
	public JSONObject getPayload () {
		return payload;
	}

	public void setPayload ( JSONObject payload) {
		this.payload = payload;
	}

	public EventType getType() {
		return type;
	}
	
	public String getStringValue(String key) {
		if (payload == null) {
			return null;
		}
		try {
			return payload.getString(key);
		} catch (JSONException e) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return "type: " + type 
				+ (payload != null ?  " Payload:" + payload.toString() : "");
	}
	
	public JSONObject toJSON() throws JSONException {
		if ( !payload.has(KEY_TYPE) )
			payload.put(KEY_TYPE, type.toString());
		return payload;
	}

	/**
	 * Convert a {@link JSONObject} to a raw Hash String of key=val pairs;
	 * @param o {@link JSONObject}
	 * @return Hash string of the form: k1=v1,k2=v2,...
	 */
	public static String toHashString(JSONObject o) {
		String s = o.toString();
		s = s.replaceAll("[{}\"]", "").replaceAll(":", "=");
		return s;
	}

}
