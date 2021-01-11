package com.rts.datasource.media;

import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.core.services.ServiceStatus;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;
import com.cloud.core.io.MailTool;

/**
 * Base class for any {@link IDataSource} that can be initialized with a Map of key,value pairs.
 * @author VSilva
 *
 */
public abstract class BaseMapDataSource {

	public static final String KEY_SMTP_DEBUG 	= MailTool.KEY_SMTP_DEBUG; 	//"debug";
	public static final String KEY_SMTP_USER 	= MailTool.KEY_SMTP_USER; 	// "user";
	public static final String KEY_SMTP_PWD 	= MailTool.KEY_SMTP_PWD; 	// "password";
	public static final String KEY_SMTP_HOST 	= MailTool.KEY_SMTP_HOST; 	// "host";
	public static final String KEY_SMTP_PORT 	= MailTool.KEY_SMTP_PORT; 	// "port";
	public static final String KEY_SMTP_TLS 	= MailTool.KEY_SMTP_TLS; 	// "starttls.enable";
	public static final String KEY_SMTP_FROM 	= MailTool.KEY_SMTP_FROM; 	// "from";
	public static final String KEY_SMTP_TO 		= MailTool.KEY_SMTP_TO; 	// "to";
	public static final String KEY_SMTP_FOLDER 	= MailTool.KEY_SMTP_FOLDER; // 

	/*
	 * TWilio SMS - https://www.twilio.com/console/sms/dashboard
	 */
	/** Twilio SMS application ID - https://www.twilio.com/console/sms/dashboard */
	public static final String KEY_TWISMS_APPID 	= "twilioAppId";

	/** Twilio SMS application token */
	public static final String KEY_TWISMS_TOKEN 	= "twilioToken";
	
	/** Twilio Phone number (required to send) - https://www.twilio.com/console/sms/dashboard */
	public static final String KEY_TWISMS_FROM 		= "twilioFrom";
	
	/** Destination Phone number */
	public static final String KEY_TWISMS_TO 		= "twilioTo";

	/** Data source naem */
	protected final String name;
	
	/** Data source description */
	protected final String description;

	/** Initialization parameters */
	protected JSONObject params; 
	
	/** Status of the service */
	protected final ServiceStatus status; 

	public BaseMapDataSource(final String name, final String description) {
		this.name 			= Objects.requireNonNull(name, "Data source name is required.");
		this.description 	= Objects.requireNonNull(description, "Data source description is required.");
		this.params			= new JSONObject();
		this.status 		= new ServiceStatus();
	}
	
	/**
	 * Get a JSON array of batches kept by this data source.
	 * @return JSON array : [{ROW1}, {ROW2}, ..]. The format changes depending on the data source type. See the {@link DataFormat} for details.
	 */
	public abstract JSONArray getBatches();
}
