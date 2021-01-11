package com.rts.datasource;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.services.ServiceStatus;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.sock.TCPSockDataSource;

/**
 * Base interface for all data source types. There are two known implementations:
 * <ul>
 * <li>TCP Server socket via {@link TCPSockDataSource}
 * <li>Database via JDBC - used for CMS Informaix data bases with optional support for MySQL
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation.
 * @version 1.0.1 - 11/26/2017 New data source types: SMTP(S), POP3, IMAP, SMS*, TWITTER.
 *
 */
public interface IDataSource extends Runnable {

	enum DataSourceType { 
		SOCKET						// TCP Server Data source
		, SOCKET_WITH_STORAGE		// TCP Server with Storage micro service
		, DATABASE					// Database
		, DATASTORE					// Writable database
		, SMTP						// Send Email via plain SMTP
		, SMTPS						// Send Secure email
		, POP3						// Read email via POP3
		, IMAP						// Read email via IMAP
		, IMAPS						// Read email via IMAP over SSL
		, SMS_TWILIO				// SMS Via Twilio - https://www.twilio.com
		, TWITTER					// Send Tweets
		, FILESYSTEM				// 6/11/2019 Used to store files 
		, PROMETHEUS				// 8/1/2020 Kubernetes metrics
		};
	
	/**
	 * Start (run) the data source loop/process
	 */
	public abstract void run();

	/**
	 * Stop it. Opposite of run().
	 */
	public abstract void stop();

	/**
	 * Shutdown. Invoked when the container shuts down.
	 */
	public abstract void shutdown();

	/**
	 * Get data source name.
	 * @return Name
	 */
	public abstract String getName();

	/**
	 * @return Data source description.
	 */
	public abstract String getDescription();

	/**
	 * Get port (optional)
	 * @return DS Port or zero if not available.
	 */
	public abstract int getPort();

	/**
	 * Data sources massage data using formats.
	 * @return See {@link DataFormat}.
	 */
	public abstract DataFormat getFormat();

	/**
	 * Get the run status.
	 * @return See {@link ServiceStatus}.
	 */
	public abstract ServiceStatus getStatus();

	/**
	 * Optional metric. These values are displayed by the cloud console diagnostics page.
	 * <li> A batch is a collection of records.
	 * <li> A record is a collection of data fields.
	 * @return Number of data batches processed by the DS or zero if not available.
	 */
	public abstract long getTotalBatches();

	/**
	 * Run time metrics displayed by the cloud console diagnostics page.
	 * @return Total records handled. Note: A batch is a collection of records.
	 * A record is a collection of data fields.
	 */
	public abstract long getTotalRecords();

	/**
	 * Set a target JSON event receiver. It is used to notify when batches are received.
	 * @param l batch Event listener - See {@link IBatchEventListener}.
	 */
	void setEventListener (IBatchEventListener l);
	
	/**
	 * Serialize to XML. See the reports document for the XMl format.
	 * @return
	 * @throws IOException
	 */
	public String toXML () throws IOException;
	
	/**
	 * Get listener as a {@link JSONObject}
	 * @return {"name": "name1", "port", 1000, "description", "desc"}
	 * @throws JSONException
	 */
	public JSONObject toJSON () throws JSONException;
	
	/**
	 * @return See {@link DataSourceType}.
	 */
	public DataSourceType getType();
	
	/**
	 * Get extra params
	 * @return
	 */
	public JSONObject getParams();
}