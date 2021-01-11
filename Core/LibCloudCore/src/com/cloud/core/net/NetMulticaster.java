package com.cloud.core.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.cloud.core.types.CoreTypes;

/**
 * A very simple multi-cast sender/receiver: IP Multicast is an extension to the standard IP network-level protocol. RFC 1112.
 * IP Multicasting as: "the transmission of an IP datagram to a ‘host group’, a set of zero or more hosts identified by a single IP
 * destination address. A multicast datagram is delivered to all members of its destination host group with the same ‘best-efforts’
 * 
 * <ul>
 * <li>See https://docs.oracle.com/javase/7/docs/api/java/net/MulticastSocket.html
 * <li>https://www.csie.ntu.edu.tw/~course/u1580/Download/Multicast/howipmcworks.html
 * </ul>
 * 
 * <h2>Issues</h2>
 * <ul>
 * <li> In UDP there is no reading in chunks so if the size of the message > receive buffer size (bufferSize), the extra data will be dropped.
 * See: https://stackoverflow.com/questions/15446887/udp-read-data-from-the-queue-in-chunks </li>
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 1/8/2019 Initial implementation
 *
 */
public class NetMulticaster {

	/** Default address */
	public static final String DEFAULT_ADDR = "224.1.2.3";
	
	/** Default port */
	public static final int 	DEFAULT_PORT = 6789;

	/** Several standard settings for TTL are specified for the MBONE: 1 for local net, 15 for site, 63 for region and 127 for world. */
	public static final int 	DEFAULT_TTL = 15;

	String 	address 	= DEFAULT_ADDR;
	int 	port		= DEFAULT_PORT;
	
	// receive buffer size (8K). The send buffer size cannot exceed this value or extra data will be dropped!.
	int		bufferSize	= 8192;
	
	// The socket
	protected MulticastSocket 	s;
	
	// Multi cast group
	protected InetAddress 		group;
	
	// 1 thread to receive stuff.
	protected Thread 			receiver;
	
	// Listener used to send messages.
	protected MessageListener 	listener;
	
	public static interface MessageListener {
		void onMessage (byte[] bytes);
	}
	
	public NetMulticaster()  {
	}

	/**
	 * Construct
	 * @param address Multicast UDO address.
	 * @param port Multicast port.
	 */
	public NetMulticaster(String address, int port)  {
		this.address 	= address;
		this.port		= port;
	}

	/**
	 * Create a {@link MulticastSocket} with the default TTL (15).
	 * @throws IOException On socket errors.
	 */
	public void open () throws IOException {
		open(DEFAULT_TTL);
	}
	
	/**
	 * Create a {@link MulticastSocket} with the given TTL.
	 * @param TTL Time-to-live: Several standard settings for TTL are specified for the MBONE: 1 for local net, 15 for site, 63 for region and 127 for world.
	 * @throws IOException On socket errors.
	 */
	public void open (int TTL) throws IOException {
		s 	= new MulticastSocket(port);
		// Several standard settings for TTL are specified for the MBONE: 1 for local net, 15 for site, 63 for region and 127 for world.
		s.setTimeToLive(TTL);
	}
	
	/**
	 * Set the multicast arguments. See https://docs.oracle.com/javase/7/docs/api/java/net/MulticastSocket.html
	 * @param address Multi-cast address. Default: 224.1.2.3
	 * @param port Multi-cast port. Default: 6789
	 * @param bufferSize Multi-cast receive buffer size. Default: 2048
	 */
	public void configure (String address, int port , int bufferSize) {
		this.address 	= address;
		this.port		= port;
		this.bufferSize	= bufferSize;
	}

	/**
	 * Set the multicast arguments. See https://docs.oracle.com/javase/7/docs/api/java/net/MulticastSocket.html
	 * @param address Multi-cast address. Default: 224.1.2.3
	 * @param port Multi-cast port. Default: 6789
	 */
	public void configure (String address, int port ) {
		configure(address, port, DEFAULT_PORT);
	}

	public void setListener (MessageListener l) {
		listener = l;
	}
	
	/**
	 * Join the multicast group given by the default address (224.1.2.3) and port (6789).
	 * @throws IOException
	 */
	public void joinGroup () throws IOException {
		group 	= InetAddress.getByName(address);
		s.joinGroup(group);		
	}
	
	public void shutdown () throws IOException {
		if ( s != null) {
			s.leaveGroup(group);
			s.close();
			s = null;
		}
		
		stopThread();
	}
	
	public boolean isClosed () {
		if ( s == null) {
			return true;
		}
		return s.isClosed();
	}
	
	private void stopThread() {
		if ( receiver == null ) {
			return;
		}
		try {
			receiver.interrupt();
			receiver.join(2000);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Send a packet to the multicast group.
	 * @param msg Message bytes.
	 * @throws IOException On I/O errors.
	 */
	public void send (final byte[] msg) throws IOException {
		// guard against someone trying to send when the socket has been shutdown (null)
		if ( s == null) {
			return;
		}
		DatagramPacket hi = new DatagramPacket(msg, msg.length, group, port);
		s.send(hi);
	}
	
	/**
	 * Send a text message.
	 * @param text Text to send.
	 * @throws IOException On I/O errors.
	 */
	public void send (final String text) throws IOException {
		byte[] bytes = text.getBytes(Charset.defaultCharset());
		// In UDP there is no reading in chunks . If the size of the message > receive buffer size
		// then data is dropped. See https://stackoverflow.com/questions/15446887/udp-read-data-from-the-queue-in-chunks
		if ( bytes.length > bufferSize) {
			throw new IOException("Send failure for [" + text.substring(0, 32) + " ...] (MAX BUFFER SIZE " + bufferSize + "/" + bytes.length + " EXCEEDED)");
		}
		send(bytes);
	}
	
	public void receive () {
		receiver = new Thread(new Runnable() {
			public void run() {
				while ( true) {
					try {
						read();
					}
					catch (SocketException so) {
						// socket closed?
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "MULTICAST-RECEIVER-" + CoreTypes.NODE_NAME);
		receiver.start();
	}
	
	private void read () throws IOException {
		// get their responses!
		byte[] buf 			= new byte[bufferSize];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		
		// this blocks until data is received
		s.receive(recv);
		
		int len = recv.getLength();
		if ( listener != null) {
			listener.onMessage(Arrays.copyOf(buf, len));
		}
	}
	
	/**
	 * Get the {@link MulticastSocket} time to live (TTL)
	 * @return Default TTL.
	 * @throws IOException On I/O errors.
	 */
	public int getTimeToLive() throws IOException {
		return s != null ? s.getTimeToLive() : 0;
	}

	/**
	 * Set the time to live. See https://www.csie.ntu.edu.tw/~course/u1580/Download/Multicast/howipmcworks.html
	 * Several standard settings for TTL are specified for the MBONE: 1 for local net, 15 for site, 63 for region and 127 for world.
	 * @param TTL The TTL field controls the number of hops that a IP Multicast packet is allowed to propagate.
	 * @throws IOException On I/O errors.
	 */
	/*
	public void setTimeToLive(final int TTL) throws IOException {
		if ( s != null) {
			s.setTimeToLive(TTL);
		}
	} */
}
