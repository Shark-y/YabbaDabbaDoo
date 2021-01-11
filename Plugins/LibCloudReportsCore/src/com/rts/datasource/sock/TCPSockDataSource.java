package com.rts.datasource.sock;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudExecutorService;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;
import com.rts.datasource.IDataSource.DataSourceType;

/**
 * A basic TCP server socket Data Source.
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation
 * @version 1.0.1 - Set the default socket receive buffer size to 1MB + optimizations.
 *
 */
public class TCPSockDataSource implements /*Runnable,*/ IDataSource {
	private static final Logger log = LogManager.getLogger(TCPSockDataSource.class);
	
	/** The Server socket this data source wraps */
	private ServerSocket serverSocket;
	
	/** TCK socket handler */
	private TCPSockHandler handler;
	private boolean done;

	/** service name */
	private final String name;
	
	/** TCP listen port */
	private final int port;
	
	/** description */
	private final String description;
	
	/** See {@link DataFormat} */
	private final DataFormat format;
	
	/** Event receiver - after the raw sock data is matched agains the {@link DataFormat} to create a JSON object. */
	private IBatchEventListener listener;
	
	/** {@link ServiceStatus} of the data source */
	private final ServiceStatus status = new ServiceStatus(Status.OFF_LINE, "Stopped");
	
	/** Either SOCKET or SOCKET_WITH_STORAGE */
	private DataSourceType type = DataSourceType.SOCKET;
	
	static void LOGD(final String text) {
		log.debug("[TCP-DATASOURCE] " + text);
	}

	static void LOGE(final String text) {
		log.error("[TCP-DATASOURCE] " + text);
	}
	
	/**
	 * Construct a TCP listener.
	 * @param name TCP Listener name ir id.
	 * @param port Port to listen to.
	 * @param description Service description.
	 * @param format {@link DataFormat} used to match socket raw data with a given format to build a JSON object.
	 * @throws IOException
	 */
	public TCPSockDataSource(final String name, final int port, final String description, final DataFormat format) throws IOException {
		this.name 			= name;
		this.port 			= port;
		this.description 	= description;
		this.format 		= format;
		//LOGD("Construct " + name + " port: " + port);
	}

	/**
	 * Set a target JSON event receiver.
	 * @param l
	 */
	public void setEventListener (IBatchEventListener l) {
		listener = l;
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#run()
	 */
	@Override
	public void run() { // run the service
		boolean cleanStop 	= true;
		try {
			LOGD("Starting TCP server socket in port " + port + " Status:" + status.getStatus());

			if ( status.getStatus() == Status.CONNECTING || status.getStatus() == Status.ON_LINE) {
				LOGD("Datasource " + name + " already started. Abort start.");
				cleanStop = false;
				return;
			}
			status.setStatus(Status.CONNECTING, "Connecting  to " + port);
			serverSocket 	= new ServerSocket(port);

			done 			= false;
			Socket  socket  = null;
			
			// 8/3/17 Default receive buffer size (SO_RCVBUF) set to 1MB.
			serverSocket.setReceiveBufferSize(TCPSockHandler.DEFAULT_SORECV_BUFFER_SIZE);
			
			while ( !done ) {
				status.setStatus(Status.ON_LINE, "Accepting @ " + serverSocket.getLocalPort());
				LOGD("Accepting: " + name + " @ " + serverSocket + " Receive BufSize:" + serverSocket.getReceiveBufferSize());
				
				// This will block execution...
				/*Socket */ socket = serverSocket.accept();

				if ( handler != null) {
					LOGD("Handler " + name + " aleady exists. Only 1 handler per port.");
					handler.close();
				}
				
				handler = new TCPSockHandler(name, socket, format, listener);
				
				CloudExecutorService.execute(handler);
			}
		}
		catch (BindException ex) {
			cleanStop = false;
			LOGE(name + " " + ex.toString() + " (" + port + ")");
			status.setStatus(Status.SERVICE_ERROR, name + " " + ex.toString() + " (" + port + ")");
		}
		catch (SocketException ex) {
			LOGE(name + " " + ex.toString());
			status.setStatus(Status.OFF_LINE, "Disconnected.");
		}
		catch (IOException ex) {
			cleanStop = false;
			log.error(name + " run()", ex);
			status.setStatus(Status.SERVICE_ERROR, name + " " + ex.toString());
		}
		finally {
			if ( cleanStop) {
				LOGD(name + " main loop done.");
				status.setStatus(Status.OFF_LINE, "");
				serverSocket = null;
			}
		}
	}

	private void closeServerSocket () {
		if ( serverSocket == null )
			return;
		try {
			LOGD("Shutdown Closing server sock @ port " + port);
			serverSocket.close();
		} catch (IOException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#stop()
	 */
	@Override
	public void stop() {
		LOGD("Stoping listener " + name);
		done = true;
		
		if ( handler != null ) {
			handler.close();
			handler = null;
		}
		closeServerSocket();
	}

	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#shutdown()
	 */
	@Override
	public void shutdown() {
		stop();

		LOGD("Shutdown listener " + name);
		// FIXME shutdownAndAwaitTermination(pool);
	}

	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#getFormat()
	 */
	@Override
	public DataFormat getFormat() {
		return format;
	}
	
	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#getStatus()
	 */
	@Override
	public ServiceStatus getStatus () {
		// Sync the status with handler thread
		if ( handler != null) {
			if (handler.isRunning()) {
				status.setStatus(Status.ON_LINE, "Connected to " + handler.getSocketStatus());
			}
			else {
				status.setStatus(Status.ON_LINE, "Disconnected.");
			}
		}
		//LOGD("GetStatus:" + name + " " + status);
		return status;
	}
	
	/**
	 * Serialize to XML. See the reports document for the XMl format.
	 * @return
	 * @throws IOException
	 */
	public String toXML () throws IOException {
		if ( name == null) throw new IOException("Service name cannot be null.");
		if ( port == 0) throw new IOException("Invalid service port " + port);
		
		StringBuffer buf = new StringBuffer("<dataSource>");
		
		buf.append("\n\t<name><![CDATA[" + name + "]]></name>");
		buf.append("\n\t<port>" + port + "</port>");
		buf.append("\n\t<description><![CDATA[" + (description != null ? description : "") + "]]></description>");
		buf.append("\n" + format.toXML());
		
		buf.append("\n</dataSource>");
		
		return buf.toString();
	}
	
	/**
	 * Get listener as a {@link JSONObject}
	 * @return {"name": "name1", "port", 1000, "description", "desc", "format": {....}}
	 * @throws JSONException
	 */
	public JSONObject toJSON () throws JSONException {
		JSONObject root = new JSONObject();
		
		root.put("type", type.name());
		root.put("name", name);
		root.put("port", port);
		
		// optional
		if ( description != null) {
			root.put("description", description);
		}
		
		root.put("format", format.toJSON());
		return root;
	}
	

	@Override
	public long getTotalBatches () {
		// Note: the handler will be null if the sock is accepting & there is no connection!
		return handler != null ? handler.getMatcher().getTotalBatches() : 0;
	}

	/* (non-Javadoc)
	 * @see com.cloud.rts.datasource.IDataSource#getTotalRecords()
	 */
	@Override
	public long getTotalRecords () {
		return handler != null ? handler.getMatcher().getTotalRecords() : 0;
	}

	/**
	 * @return See {@link DataSourceType}.
	 */
	public DataSourceType getType() {
		return type;
	}

	public void setType (DataSourceType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("[%s %d %s]",  name, port, type);
	}
	
	@Override
	public JSONObject getParams() {
		return new JSONObject();
	}

}
