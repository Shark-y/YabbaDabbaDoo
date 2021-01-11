package com.cloud.core.net;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.services.ServiceStatus;

/**
 * Classes that are pooled must implement this class.
 * @author VSilva
 *
 */
public interface IConnection {

	/** Connection status */
	//public enum Status {IDLE, CONNECTING, CONNECTED, DISCONNECTED, UNKNOWN, ERROR};
	
	/**
	 * Used to keep track of the status of a connection.
	 *
	 */
	/*
	public static class ConnectionStatus {
		private Status status;
		private String description;

		public ConnectionStatus(final Status status, final String description) {
			super();
			this.status = status;
			this.description = description;
		}

		public Status getStatus() {
			return status;
		}

		public String getDescription() {
			return description;
		}
		public void setStatus (Status status, String description) {
			this.status = status;
			this.description = description;
		}
		@Override
		public String toString() {
			return status + ":" + description;
		}
	} */
	
	/**
	 * Initialize with an options MAp
	 * @param options
	 */
	public void initialize (final Map<String, String> options);

	/**
	 * Connect.
	 * @param waitForConnection If true wait for the connection to succeed.
	 * @throws ConnectionPoolException
	 */
	public void connect (final boolean waitForConnection) throws IOException;
	
	/**
	 * Disconnect.
	 * @throws ConnectionPoolException
	 */
	public void disconnect() throws IOException;
	
	/**
	 * Get connection status.
	 * @return See {@link ConnectionStatus}.
	 */
	public ServiceStatus getStatus ();
	
	/**
	 * ID assigned to the connection by the pool manager.
	 * @param id Unique cn id.
	 */
	public void setId (final String id );

	/**
	 * @return Connection ID assigned by the pool manager.
	 */
	public String getId ();

	/**
	 * Wait for a connections to be on {@link ServiceStatus} ON_LINE.
	 * @param delay Time in ms to wait for the connection.
	 * @throws InterruptedException if the wait is interrupted by a container shutdown or something else.
	 */
	public void waitForConnection (final long delay) throws InterruptedException ;
	
	/**
	 * Provide information about the connection as well as performance metrics
	 * @return A JSON object.
	 * @throws JSONException on JSON parse errrors.
	 */
	public JSONObject toJSON () throws JSONException;
}
