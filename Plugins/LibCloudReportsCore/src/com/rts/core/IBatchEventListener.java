package com.rts.core;

import org.json.JSONObject;

/**
 * Interface used to receive batches in JSON.
 * @author VSilva
 *
 */
public interface IBatchEventListener {
	public void onBatchReceived (JSONObject batch);
}