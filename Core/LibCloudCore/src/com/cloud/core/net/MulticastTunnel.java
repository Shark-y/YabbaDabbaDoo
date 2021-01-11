package com.cloud.core.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.net.NetMulticaster;
import com.cloud.core.types.CoreTypes;

/**
 * A Multicast tunnel is used to move multi-cast packets across two subnets.
 * It is known that routes drop multicast packets from different subntes (10.10.x.x/10.20.x.x).
 * This code allows you to tunnel those packets over unicast UDP between subnets. For this purpose it uses
 * <ul>
 * <li>A UDP server: to receive multicast packets from the remote subnte.
 * <li>A UDP client to tunnel packets from the local subnet.
 * <li>A {@link NetMulticaster} used to listen in the local subnet.
 * </ul>
 * When a multicast packet is received from the local subnet, the packet is sent by the UDP client to the UDP
 * server in the remote subnet which in turn broadcasts to its local subnet.
 * </p>
 * <b>Note: This tunnel requires 2 endpoints ({@link MulticastTunnel}) on each subnet with the UDP client pointing o the other UDP server.</b>
 * 
 * <p>See:
 * <ul>
 * <li>http://www.firewall.cx/networking-topics/general-networking/107-network-multicast.html
 * <li>https://www.baeldung.com/udp-in-java
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - 02/13/2019 Initial implementation.
 */
public class MulticastTunnel {

	static void LOGE(String text) {
		if (debug) {
			System.err.println("[MCAST-TUNNEL] " + text);
		}
	}

	static void LOGD(String text) {
		if (debug) {
			System.out.println("[MCAST-TUNNEL] " + text);
		}
	}
	
	static final int DEFAULT_UDP_SERVER_PORT = 4445;
	
	// Used to receive from the remote endpoint
	private UDPServer server;

	// Used to send to the remote endpoint
	private UDPClient client;
	
	// Used to listen/bcast in the local subnet
	private NetMulticaster multicaster;
	
	// Used to prevent feedback loops
	private static final List<String> hashes = new CopyOnWriteArrayList<String>();
	
	// If true dump info to stdout
	private static boolean debug;
	
	// metric: total packets send/received
	private long packetsSent;

	// metric: total bytes sent/received
	private long bytesSent;

	// metric: uptime (ms)
	private long upTime;
	
	/**
	 * The UDP server listens for packets in the local endpoint
	 * 
	 * @author VSilva
	 *
	 */
	static class UDPServer extends Thread {
		private DatagramSocket socket;
	    private boolean running;
	    private byte[] buf 				= new byte[8192];
	    NetMulticaster multicaster;
		private long bytesRecv, packetsRecv;	// metrics
	    
	    public UDPServer(int port , NetMulticaster caster) throws IOException {
	    	super("MULTICAST-TUNNEL-" + CoreTypes.NODE_NAME);
	    	this.socket 		= new DatagramSocket(port);
	    	this.multicaster	= caster;
	    	LOGD("Started UDP Server @ port " + port);
		}
	    
	    @Override
	    public void run() {
	    	running = true;
	    	LOGD("Running UDP server @ " + socket.getLocalAddress() + ":" + socket.getLocalPort());
	    	
	    	while (running) {
	            DatagramPacket packet = new DatagramPacket(buf, buf.length);
	            try {
	            	// this blocks execution until a packet is received...
		            socket.receive(packet);
		            
					final int len 		= packet.getLength();
					final byte[] data 	= Arrays.copyOf(buf, len);
					final String hash 	= HASH(data);
					
					// must be sorted. Else won't work!
					final int idx 		= Collections.binarySearch(hashes, hash);
					
					if ( idx < 0 ) {
						LOGD("UDP Server received " + len + "/" + buf.length + " bytes. Found: " + idx + " Multicasting");
						multicaster.send(data);
						
						// metrics
						packetsRecv	++;
						bytesRecv 	+= len;
						
						addSortHashes(hash);
					}
					else {
						LOGD("UDP Server received " + len + "/" + buf.length + " bytes. Hash: " + hash + " Packet already sent.");
					}
				} 
	            catch (SocketException se ) {
	            	// Swallow
	            }
	            catch (Exception e) {
					e.printStackTrace();
					LOGE("UDPServer:run(): " + e.toString());
				}
	    	}
	    }
	    
	    public void shutdown () throws IOException {
	    	running = false;
	    	if ( socket != null) { 
	    		socket.close();
	    	}
	    	packetsRecv = bytesRecv = 0;
	    }
	}
	
	/**
	 * Hashed packets must be sorted, else Collections.binarySearch won't work!
	 * @param hash
	 */
	private static void addSortHashes (final String hash) {
		hashes.add(hash);
		//Collections.sort(hashes); Cant sort CopyOnWriteArrayList - throws UnsuportedOperationException
		
		// How to sort CopyOnWriteArrayList
		// https://stackoverflow.com/questions/28805283/how-to-sort-copyonwritearraylist
		Object[] a = hashes.toArray();
	    Arrays.sort(a);
	    for (int i = 0; i < a.length; i++) {
	        hashes.set(i, (String) a[i]);
	    }	
	}
	
	/**
	 * The UDP client forward packets to the remote {@link UDPServer}.
	 * @author VSilva
	 *
	 */
	static class UDPClient {
		private DatagramSocket socket;
		private InetAddress remoteHost;
		private int remotePort;
		
		public UDPClient(String remoteHost, int remotePort) throws SocketException, UnknownHostException {
			super();
			this.remoteHost = InetAddress.getByName(remoteHost);
			this.remotePort = remotePort;
			this.socket 	= new DatagramSocket();
			LOGD("Started UDP Client to remote " + remoteHost + " @ " + remotePort);
		}
		
		public void send(byte[] buf) throws IOException {
			//LOGD("UDP Client: sending " + buf.length + " bytes to " + remoteHost + ":" + remotePort);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, remoteHost, remotePort);
			socket.send(packet);
		}
		
		public void shutdown () {
			if ( socket != null) {
				socket.close();
			}
		}
	}

	public MulticastTunnel() throws IOException {
	}

	/**
	 * Construct a multicast tunnel.
	 * @param udpServerPort The port of the local {@link UDPServer}. Default: 9876
	 * @param remoteHost IP address or host name of the remote endpoint {@link UDPServer}.
	 * @param remotePort Port of the remote {@link UDPServer}. Default: 9876
	 * @throws IOException on I/O/Socket errors.
	 */
	public MulticastTunnel(int udpServerPort , String remoteHost, int remotePort) throws IOException {
		init(udpServerPort, remoteHost, remotePort);
	}
	
	private void init (int udpServerPort , String remoteHost, int remotePort ) throws IOException {
		// 224.1.2.3 : 6789
    	this.multicaster 	= new NetMulticaster();
		this.server 		= new UDPServer(udpServerPort, multicaster);
		this.client 		= new UDPClient(remoteHost, remotePort);
    	
    	this.multicaster.setListener(new NetMulticaster.MessageListener() {
			
			@Override
			public void onMessage(byte[] bytes) {
				final String hash 		= HASH(bytes);
				
				// must e sorted. Else won't work!
				final int idx 			= Collections.binarySearch(hashes, hash);
				
				//LOGD("Multicast: got " + bytes.length + " bytes. Hash:" + hash + " found:" + idx); // + " List:" + hashes);
				try {
					if ( idx < 0 ) {
						LOGD("Multicast: Tunneling " + + bytes.length + " bytes."); // + new String(bytes));
						client.send(bytes);
						
						// metrics
						packetsSent ++;
						bytesSent 	+= bytes.length;
						
						addSortHashes(hash);
					}
					else {
						//LOGD("Multicast: Packet " + hash + " already sent.");
					}
					
					if ( hashes.size() > 200) {
						LOGD("Cleaning hash list.");
						hashes.clear();
					}
				} catch (IOException e) {
					LOGE(e.toString());
				}
			}
		});
    	this.multicaster.open();
    	this.multicaster.joinGroup();
    	this.multicaster.receive();
	}
	
	public void start () throws IOException {
		if ( server == null || client == null || multicaster == null) {
			throw new IOException("Invalid constructor invoked.");
		}
		upTime	= System.currentTimeMillis();
		server.start();
	}

	public void start (int udpServerPort , String remoteHost, int remotePort ) throws IOException {
		if (isRunning()) {
			return;
		}
		upTime	= System.currentTimeMillis();
		init(udpServerPort, remoteHost, remotePort);
		server.start();
	}

	public void shutdown () throws IOException {
		LOGD("Shutting down multicast tunnel.");
		upTime = packetsSent = bytesSent = 0;
		if ( server != null) {
			server.shutdown();
		}
		if ( client != null) {
			client.shutdown();
		}
		if ( multicaster != null ) {
			multicaster.shutdown();
		}
		server 		= null;
		client 		= null; 
		multicaster = null;
	}

	public boolean isRunning () {
		return server != null && client != null && multicaster != null;
	}
	
	/**
	 * MD5 digest tool. It should only be used to generate an instance ID. MD5 hashes are not secure.
	 * @param string String to digest.
	 * @return Hex encoded MD5.
	 */
	static String HASH(final byte[] bytes) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256"); // Removed weak "MD5");
			byte[] digest = md.digest(bytes);
			
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < digest.length; ++i) {
				sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1,3));
				//sb.append(String.format("%2X", (digest[i] & 0xFF) | 0x100 ) );
			}
			return sb.toString().toUpperCase();
		} 
		catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	public static void setDebug (boolean debug) {
		MulticastTunnel.debug = debug;
	}
	
	public long getPacketsSent () {
		return packetsSent;
	}
	public long getBytesSent () {
		return bytesSent;
	}

	public long getPacketsRecv () {
		return server != null ? server.packetsRecv : 0;
	}
	
	public long getBytesRecv () {
		return server != null ? server.bytesRecv : 0;
	}
	
	public long getUptime () {
		return upTime;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject root = new JSONObject();
		root.put("running", isRunning());
		root.put("bytesReceived", getBytesRecv());
		root.put("packetsReceived", getPacketsRecv());
		root.put("bytesSent", getBytesSent());
		root.put("packetsSent", getPacketsSent());
		return root;
	}
}
