package com.rts.datasource.sock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataMatcher;

/**
 * Simple reusable TCP network service handler.
 * <ul>
 * <li> Receive data from socket.
 * <li> Match the socket data with the fields of {@link DataFormat} to build a JSON object..
 * <li> Invoke the event listener {@link IBatchEventListener} with the matched JSON object.
 * </ul>
 * 
 * Given Raw Data: F1|  65710|Eastern Sales|   2|:00||   5|53: 7|  60|33:11|  68 
 * 
 * <h3>JSON Batch</h3>
 * 
 * <pre>{"batchData":[{"F1":"F1","VDN":"65710","ACDCALLS":"60"
 *  ,"ABNCALLS":"5" ,"INPROGRESS-ATAGENT":"2","AVG_ACD_TALK_TIME":"33:11"
 *  ,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"53: 7"
 *  ,"OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"68"}
 *  ]}</pre>

 * @author VSilva
 *
 */
class TCPSockHandler implements Runnable {
	private static final Logger log = LogManager.getLogger(TCPSockHandler.class);
	
	/** Default receive buffer size (SO_RCVBUF) set by the server socket in the {@link TCPSockDataSource}. */
	static final int DEFAULT_SORECV_BUFFER_SIZE = 1048576;

	/** Default size of the buffer used to read from an incoming socket */
	static final int DEFAULT_READ_BUFFER_SIZE = 16384;
	
	/** Client Socket that pump the raw data */
	private final Socket socket;
	private final InputStream is;
	private final OutputStream os;

	/** Used to id this handler */
	private final String name;
	private boolean done;

	/** Data format describing the socket buffer */
	private final DataFormat format;
	
	/** Batch JSON receiver */
	private final IBatchEventListener listener;
	
	/** matches the TCP raw data w/ the {@link DataFormat} fields to build a JSOn object */
	private final IDataMatcher matcher;
	
	static void LOGD(final String text) {
		log.debug("[NET-HANDLER] " + text);
	}

	static void LOGE(final String text) {
		log.error("[NET-HANDLER] " + text);
	}

	/**
	 * Construct
	 * @param name Handler name/id.
	 * @param socket TCP Socket being handled.
	 * @param fmt {@link DataFormat} used to match socket data.
	 * @param listener Event listener {@link IBatchEventListener}.
	 * @throws IOException
	 */
	TCPSockHandler(final String name, final Socket socket, final DataFormat fmt, final IBatchEventListener listener) throws IOException {
		this.socket = socket;
		this.is		= socket.getInputStream();
		this.os		= socket.getOutputStream();
		this.name 	= name;
		this.format	= fmt;
		this.listener = listener;
		this.matcher = new TCPSockDataMatcher();
	}

	public void run() { 
		// read and service request on socket
		LOGD("Running handler " + name);
		
		final byte[] data 	= new byte[DEFAULT_READ_BUFFER_SIZE];
		int read 			= -1;
		done				= false;
		
		String temp			= null;
		final StringBuffer records 	= new StringBuffer(); 	// 1+ rows
		final StringBuffer batch 	= new StringBuffer();	// HEADER + RECORDS + FOOTER

		while ( !done && !socket.isClosed()) {
			try {
				read = is.read(data);
				
				if ( read > 0) {
					temp = new String(Arrays.copyOf(data, read));
					
					/* Handle broken record writes */
					if ( temp.contains(IDataMatcher.LINE_FEED)) {
						// Read buffer could contain a broken record (chunked) so copy up to the last LF
						int lfPos = temp.lastIndexOf(IDataMatcher.LINE_FEED) + 1;
						
						// (1+ recs) F1|1|40250||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|
						records.append(temp.substring(0, lfPos));
						
						if (records.toString().contains(format.getFooter())) {
							batch.append(records.toString());
							
							//System.out.println("CONSUME:" + batch);
							// consume
							matcher.matchFormat(name, batch.toString() , format, listener );
							
							// clean batch
							batch.delete(0, batch.length());

						}
						else {
							// 11/12/2017 - Half batch detected? On large buffers there could be a HDR - N-RECORS (NO FOOTER)
							if (records.toString().contains(format.getHeader())) {
								log.warn("[" + name + "] Half buffer detected of size " + read + " bytes. This may occur on large buffers.");
							}
							// Broken/half buffer? copy remainder to the record ( if any)
							if ( lfPos < temp.length()) {
								records.append(temp.substring(lfPos,temp.length()));
							}
							batch.append(records.toString());
						}

						// clean record
						records.delete(0, records.length());
					}
					else {
						records.append(temp);
					}
				}
			} 
			catch (Exception e) {
				LOGE(name + " run: " + e.toString());
				done = true;
			}
		}
		if ( matcher != null) {
			matcher.resetMetrics();
		}
		done = true;
		LOGD("Handler " + name + " main loop done.");
	}
	
	String getSocketStatus() {
		return socket.toString();
	}
	
	boolean isRunning() {
		return !done;
	}
	
	void close () {
		try {
			LOGD("Handler " + name + " closing socket.");
			done = true;
			if ( is != null ) is.close();
			if ( os != null ) os.close();
			if ( socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			LOGE("Close: " + e.toString());
		}
	}
	
	/**
	 * A matcher is used to match socket raw data with a {@link DataFormat} to build a JSON object.
	 * The object may be sent to an event receiver {@link IBatchEventListener}.
	 * @return See {@link IDataMatcher}.
	 */
	IDataMatcher getMatcher () {
		return matcher;
	}
	
	@Override
	public String toString() {
		return name != null ? name + " Runing:" + (!done) : "";
	}
 }