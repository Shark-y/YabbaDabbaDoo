package com.rts.datasource.media;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;

public class SMSTwilioDataSource extends BaseMapDataSource implements IDataSource  {

	private static final Logger log = LogManager.getLogger(SMSTwilioDataSource.class);
	
	/** Used to describe the {@link DataFormat} of this {@link IDataSource} */
	private final DataFormat fmt;

	private final JSONArray batches ;
	
	public SMSTwilioDataSource(String name, String description, String appId, String token, String from/*, String to*/) throws JSONException {
		super(name, description);
		params.put(KEY_TWISMS_APPID, appId);
		params.put(KEY_TWISMS_TOKEN, token);
		params.put(KEY_TWISMS_FROM, from);
		//params.put(KEY_TWISMS_TO, to);
		
		fmt 	= new DataFormat(null, null, null, null, "number,text" , null);
		batches	= new JSONArray();
	}

	public SMSTwilioDataSource (JSONObject ds) throws JSONException {
		super( ds.getString("name"), ds.optString("description"));
		params 	= ds.getJSONObject("params");
		fmt 	= new DataFormat(null, null, null, null, "number,text" , null);
		batches	= new JSONArray();
	}
	
	@Override
	public void run() {
		status.setStatus(Status.ON_LINE);
	}

	@Override
	public void stop() {
		status.setStatus(Status.OFF_LINE);		
	}

	@Override
	public void shutdown() {
		stop();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public DataFormat getFormat() {
		return fmt;
	}

	@Override
	public ServiceStatus getStatus() {
		return status;
	}

	@Override
	public long getTotalBatches() {
		return 0;
	}

	@Override
	public long getTotalRecords() {
		return 0;
	}

	@Override
	public void setEventListener(IBatchEventListener l) {
	}

	@Override
	public String toXML() throws IOException {
		throw new IOException("toXML() is deprecated.");	
	}

	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject root = new JSONObject();

		root.put("type", getType().name());
		root.put("name", name);
		root.putOpt("description", description);

		root.put("params", params);
		return root;
	}

	@Override
	public DataSourceType getType() {
		return DataSourceType.SMS_TWILIO;
	}

	/**
	 * Get a JSON array of batches of data processed by this datasource. 
	 * The data is displayed by the console diagnostic page.
	 * @return JSON Array of batches. It cannot be null.
	 */
	@Override
	public JSONArray getBatches() {
		// Note: This cannot be null
		return batches;
	}

	public void sendSMS (String destNumber, String message) throws IOException, JSONException {
		String appId = params.getString(KEY_TWISMS_APPID);
		String token = params.getString(KEY_TWISMS_TOKEN);
		String from = params.getString(KEY_TWISMS_FROM);
		
		log.debug("Send SMS to: " + destNumber + " Msg: " + message + " appId:" + appId + " Tok:" + token + " From:" + from);
		Auditor.twilioSendSMS(message, appId, token, destNumber, from);
		
		// save it.
		JSONObject record = new JSONObject();
		record.put("number", destNumber);
		record.put("text", message);
		batches.put(record);
	}
	
	@Override
	public JSONObject getParams() {
		return params;
	}

}
