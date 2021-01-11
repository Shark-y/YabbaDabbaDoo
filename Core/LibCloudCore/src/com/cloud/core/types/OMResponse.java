package com.cloud.core.types;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.types.OMEvent;

/**
 * An extension of the {@link OMEvent} to describe operation responses
 * @author vsilva
 *
 */
public class OMResponse extends OMEvent {
	private int status;
	private String message;
	
	public static final OMResponse EVENT_OK 		= new OMResponse(STATUS_OK, "OK");
	
	public OMResponse(int status, String message) {
		super(EventType.statusResponse, null);
		this.status 	= status;
		this.message	= message;
		try {
			super.payload = new JSONObject().put(KEY_STATUS, status).put(KEY_MESSAGE, message);
		} catch (Exception e) {
		}
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
	
	public static JSONObject createStatusResponse(int status, String message) {
		JSONObject root = new JSONObject();
		try {
			root.put(KEY_TYPE, EventType.statusResponse.toString());
			root.put(KEY_STATUS, status);
			root.put(KEY_MESSAGE, message);
		} catch (JSONException e) {
		}
		return root;
	}
	/* NOT NEEDED
	public static JSONObject createServerErrorStatusResponse(int code, String message)  {
		return createStatusResponse(code, message);
	}
	*/
}
