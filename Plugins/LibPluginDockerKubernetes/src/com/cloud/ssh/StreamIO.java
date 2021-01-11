package com.cloud.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * This class is used by the SSH and Terminal emulators to
 * <ul>
 * <li> Read chunks of bytes from an {@link InputStream} in the background.
 * <li> Pipe the data to a {@link PrintStream} or {@link OutputStream}.
 * </ul>
 * 
 * It allows to build a Container or SSH Terminal in Javascript (via Xterm.js) and send the input to and SSH or HTTP server/socket.
 * 
 * @author VSilva
 *
 */
public class StreamIO {
	private static final Logger log = LogManager.getLogger(StreamIO.class);
	
	/**
	 * Interface used to receive the output synchronously (WebSocket) and for streams that must be closed after each write such as the {@link Basic} remote.
	 * @author VSilva
	 *
	 */
	public static interface PrintStreamSource {
		/** get a print stream for every write */
		PrintStream getPrintStream() throws IOException;
		
		/** Return true to close after each write */
		boolean closeAfterWrite();
	}

	/**
	 * Implement this interface to build your own data parser
	 * @author VSilva
	 *
	 */
	public static interface OutputSink extends PrintStreamSource {
		/**  Fires when a chunk is received from the server input stream socket. */
		void onChunkReceived( byte[] chunk) throws IOException;
		
		/** Return true to receive stdout data as byte[] chunks. */
		boolean receiveChunks();
	}
	
	/**
	 * Used to read from the {@link Socket} {@link InputStream} in the background
	 * @author VSilva
	 *
	 */
	public static class InReader implements Runnable {
		InputStream is;
		PrintStreamSource sink;
		String id;
		
		public InReader(String id, InputStream is, PrintStreamSource os) {
			this.is 	= is;
			this.sink 	= os;
			this.id 	= id;
		}
		@Override
		public void run() {
			while ( true) {
				try {
					PrintStream ps 	= this.sink.getPrintStream();
					byte[] chunck 	= readChunk(is);
					
					if ( chunck == null ) {
						log.warn("Read a NULL chunk from socket. EOF? ERROR? Aborting reader loop.");
						break;
					}
					if ( ps != null) {
						ps.write(chunck);
					}
					if ( this.sink.closeAfterWrite()) {
						if ( ps != null) {
							ps.close();
						}
					}
					if ( sink instanceof OutputSink) {
						if ( ((OutputSink)sink).receiveChunks() ) {
							((OutputSink)sink).onChunkReceived(chunck);
						}
					}
					Thread.sleep(20);
				} 
				catch (IOException e) {
					//e.printStackTrace();
					log.error(id + " " + e.toString());
					break;
				}
				catch (InterruptedException ie) {
					break;
				}
			}
			log.debug("SOCKET-READER loop terminated " + id );
		}
		
		private  byte[] readChunk (InputStream is) throws IOException {
			byte[] chunk	= new byte[2048];
			// This blocks execution
			int read 		= is.read(chunk);
			// Exception in thread "SOCKET-READER-http://10.226.67.20:2375/exec/1fc9cf2863d87f6495ace86dec9ad34fa2bc3b9eedf7ad4af994f6504dcca310/start" java.lang.IllegalArgumentException: 0 > -1
			return read > 0 ? Arrays.copyOfRange(chunk, 0, read) : null;
		} 
	
	}

}
