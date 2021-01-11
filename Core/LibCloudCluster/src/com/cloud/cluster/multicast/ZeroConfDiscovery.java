package com.cloud.cluster.multicast;

import java.io.IOException;
import java.net.MulticastSocket;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;

import com.cloud.cluster.multicast.BaseDescriptor;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroDescriptorObject;
import com.cloud.cluster.multicast.ZeroDescriptorService;
import com.cloud.cluster.multicast.ZeroDescriptorObject.UpdateType;
import com.cloud.core.net.NetMulticaster;
import com.cloud.core.types.CoreTypes;

/**
 * Poor man's implementation of a zerconf-like multi-cast discovery service.
 * <ul>
 * <li> A thread is used to continuously send JSON messages via {@link MulticastSocket}.
 * <li> Message 1: Service lifecycle: {@link ZeroDescriptorService}.
 * <li> Message 2: Cluster message: {@link ZeroDescriptorObject}.
 * <li> Total number of threads used: {@link ZeroConfDiscovery} (1), {@link NetMulticaster} (1)
 * </ul>
 * See
 * <ul>
 * 	<li> Zeroconf - http://www.zeroconf.org/
 *  <li> Multi-cast socket - https://docs.oracle.com/javase/7/docs/api/java/net/MulticastSocket.html
 * </ul>
 * <pre>
 * // SENDER
 * ZeroDescriptorService sd = ZeroDescriptorService.create("start", "stop", "status", "confget", "confstore", "logget", "logclear");
   ZeroDescriptorObject cd = ZeroDescriptorObject.create("driver", "url", "user", "pwd");
			
   ZeroConfDiscovery ds = ZeroConfDiscovery.getInstance();
   ds.joinGroup();
   ds.send(sd);
   ds.send(cd);
   
   // RECEIVER
   DiscoveryService ds = DiscoveryService.getInstance();
   ds.joinGroup();
   ds.receive(new DiscoveryService.MessageListener() {
	public void onTextMessage(String message) {
		System.out.println("Receiver: Got " + message);
	}
   });
 * </pre>
 * 
 * @author VSilva
 *
 */
public class ZeroConfDiscovery {

	// JSON Message Keys
	public static final String JSONK_ID 		= "uuid";
	public static final String JSONK_MTYPE		= "messageType";
	public static final String JSONK_TIME_CR	= "timeCreated";
	public static final String JSONK_TIME_SEN	= "timeSent";
	
	/**  default message send frequency in seconds. */
	public static final int DEFAULT_SEND_FREQUENCY	= 60;
	
	/**
	 * ZeroConf Discovery Service message types.
	 * @author VSilva
	 *
	 */
	public enum MessageType {
		SERVICE_UP,				// Service/Node descriptor is UP message.
		SERVICE_DOWN,			// Node is disconnecting/shutting down.
		CLUSTER_DB,				// Zeroconf Master cluster DB is up.
		OBJECT_NEW,				// Create a distributed Object: MAP, LIST, Queue, Primitive.
		OBJECT_UPDATE			// Set the value of the object: add, remove, clear etc.
	}

	/**
	 * Multi-cast text message listener.
	 * @author VSilva
	 *
	 */
	public static interface MessageListener {
		void onTextMessage (String message);
	}
	
	// Multicat tool
	protected final NetMulticaster	m;
	
	// 1 Thread used to bcast messages
	protected Thread 		sender;
	
	// Send frequency in sec.
	protected int			frequency 				= DEFAULT_SEND_FREQUENCY; 
	
	// singleton
	private static ZeroConfDiscovery instance; // 	= new ZeroConfDiscovery();
	
	// messages
	private final Queue<BaseDescriptor> messageQueue = new ConcurrentLinkedQueue<BaseDescriptor>();
	
	// For thread safety
	private static final Object lock = new Object();
	
	// Time-to-live: Several standard settings for TTL are specified for the MBONE: 1 for local net, 15 for site, 63 for region and 127 for world.
	private int ttl	= NetMulticaster.DEFAULT_TTL;
	
	/**
	 * Get a singleton instance of this class.
	 * @return Singleton {@link ZeroConfDiscovery}.
	 * @throws IOException When there is an error with the {@link MulticastSocket}.
	 */
	public static ZeroConfDiscovery getInstance ()  {
		synchronized (lock ) {
			if ( instance == null) {
				instance = new ZeroConfDiscovery();
			}
		}
		return instance;
	}
	
	private ZeroConfDiscovery()  {
		m = new NetMulticaster();
	}
	
	/**
	 * Set the discovery service arguments.
	 * @param address Multicast address.
	 * @param port Multicast port.
	 * @param bufferSize Multicast socket receive buffer size.
	 * @param frequency The frequency (in secs) for the broadcast interval.
	 */
	public void configure (final String address, final int port , final int bufferSize, final int frequency) {
		/*
		m.address 		= address;
		m.port			= port;
		m.bufferSize	= bufferSize; */
		m.configure(address, port, bufferSize);
		this.frequency	= frequency;
	}

	/**
	 * Configure the Zeroconf musticast parameters.
	 * @param address Multicast socket address.
	 * @param port Multicast socket port.
	 */
	public void configure (final String address, final int port ) {
		m.configure(address, port, NetMulticaster.DEFAULT_PORT);
	}

	/**
	 * Join the multi-cast group and start broadcasting.
	 * @return {@link ZeroConfDiscovery}.
	 * @throws IOException On I/O errors.
	 */
	public ZeroConfDiscovery joinGroup () throws IOException {
		m.open(ttl);
		m.joinGroup();
		broadcast();
		return this;
	}
	
	public boolean isClosed() {
		if ( m == null) {
			return true;
		}
		return m.isClosed();
	}
	
	public void setBCastFrequency(final int freq) {
		this.frequency = freq;
	}
	
	private void stopThread() {
		if ( sender == null ) {
			return;
		}
		try {
			sender.interrupt();
			sender.join(2000);
		} catch (InterruptedException e) {
		}
	}
	
	private void broadcast () {
		sender = new Thread(new Runnable() {
			public void run() {
				long tick = 0;

				while ( true) {
					try {
						if ( tick++ % frequency == 0) {
							for (BaseDescriptor message : messageQueue) {
								final boolean expired = message.isExpired();
								
								if ( expired ) {
									messageQueue.remove(message);
								}
								else {
									m.send(message.toJSON());
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "MULTICAST-DISCOVERY-BROADCASTER-" + CoreTypes.NODE_NAME);
		sender.start();
	}
	
	/**
	 * Put a message in the delivery queue. It may take at most DEFAULT_SEND_FREQUENCY seconds to be delivered.
	 * @param message See the {@link ZeroDescriptorServiceUp} or {@link ZeroDescriptorClusterUp}.
	 * @return The {@link ZeroConfDiscovery} instance.
	 */
	public ZeroConfDiscovery queue (final BaseDescriptor message ) {
		messageQueue.offer(message);
		return this;
	}

	/**
	 * Send a message once only. (It will not be put in the delivery queue for replication).
	 * @param message See the {@link BaseDescriptor} for details.
	 * @return The {@link ZeroConfDiscovery} instance.
	 * @throws IOException on I/O or JSON errors.
	 */
	public ZeroConfDiscovery send (final BaseDescriptor message ) throws IOException {
		return send(message, false);
	}
	
	/**
	 * Send a message and immediately put in the delivery queue.
	 * @param message See the {@link ZeroDescriptorServiceUp} or {@link ZeroDescriptorClusterUp}.
	 * @return The {@link ZeroConfDiscovery} instance.
	 * @throws IOException on I/O or JSON errors.
	 */
	public ZeroConfDiscovery sendAndQueue (final BaseDescriptor message ) throws IOException {
		return send(message, true);
	}
	
	/**
	 * Send a message and optionally put in the delivery queue.
	 * @param message See the {@link ZeroDescriptorServiceUp} or {@link ZeroDescriptorClusterUp}.
	 * @param queue If true put it in the delivery queue for replication else send it once.
	 * @return The {@link ZeroConfDiscovery} instance.
	 * @throws IOException on I/O or JSON errors.
	 */
	public ZeroConfDiscovery send (final BaseDescriptor message , final boolean queue) throws IOException {
		try {
			m.send(message.toJSON());
			if ( queue) {
				messageQueue.offer(message);
			}
		} catch (Exception e) {
			throw new IOException("Send message",e);
		}
		return this;
	}

	/**
	 * Invoke this to receive messages using a listener.
	 * @param listener See the {@link MessageListener}.
	 */
	public void receive (final MessageListener listener) {
		m.setListener(new NetMulticaster.MessageListener() {
			@Override
			public void onMessage(byte[] bytes) {
				String s = new String(bytes, Charset.defaultCharset());
				if ( listener != null) {
					listener.onTextMessage(s);
				}
			}
		});
		m.receive();
	}
	
	public void shutdown () throws IOException {
		stopThread();
		m.shutdown();
		messageQueue.clear();
	}
	
	/**
	 * Get the {@link MulticastSocket} time to live (TTL)
	 * @return Default TTL.
	 * @throws IOException On I/O errors.
	 */
	public int getTimeToLive() throws IOException {
		return m.getTimeToLive();
	}

	/**
	 * Set the time to live. See https://www.csie.ntu.edu.tw/~course/u1580/Download/Multicast/howipmcworks.html
	 * Several standard settings for TTL are specified for the MBONE: 1 for local net, 15 for site, 63 for region and 127 for world.
	 * @param TTL The TTL field controls the number of hops that a IP Multicast packet is allowed to propagate.
	 * @throws IOException On I/O errors.
	 */
	public void setTimeToLive(final int TTL) throws IOException {
		this.ttl = TTL;
	} 

	/**
	 * Flush object messages for a given name. Use this to clear all ADD operations on an object when CLEAR is called.
	 * Else ADD will keep being received until the node closes.
	 * @param type A {@link MessageType}
	 * @param updateType An {@link UpdateType}
	 * @param objectName The name of the object to flush.
	 */
	public void flushObjectMessages ( MessageType type, UpdateType updateType, final String objectName) {
		for ( BaseDescriptor d : messageQueue) {
			if ( d.getType() == type ) {
				if ( d instanceof ZeroDescriptorObject) {
					ZeroDescriptorObject dobj = (ZeroDescriptorObject)d;
					if ( dobj.getUpdateType() == updateType && dobj.getObjectName().equals(objectName)) {
						//System.out.println("ZERO REMOVE " + dobj);
						messageQueue.remove(d);
					}
				}
			}
		}
	}

	/**
	 * Flush object messages for a given object (name, key). Use this to clear all ADD operations on an object when REMOVE is called.
	 * Else ADD will keep being received until the node closes.
	 * @param type A {@link MessageType}
	 * @param updateType An {@link UpdateType}
	 * @param objectName The name of the object to flush.
	 * @param key The key within the object (map).
	 */
	public void flushObjectMessagesForKey ( MessageType type, UpdateType updateType, final String objectName, final String key) {
		for ( BaseDescriptor d : messageQueue) {
			if ( d.getType() == type ) {
				if ( d instanceof ZeroDescriptorObject) {
					ZeroDescriptorObject dobj = (ZeroDescriptorObject)d;
					if ( dobj.getUpdateType() == updateType && dobj.getObjectName().equals(objectName) && dobj.getObjectKey().equals(key)) {
						messageQueue.remove(d);
					}
				}
			}
		}
	}

	/**
	 * Returns a data tables representation of the multicast queue.
	 * @return [["SERVICE_UP","SERVICE_UP..."],["OBJECT_NEW","CloudContactCenter null (null,null)"],["OBJECT_NEW","CloudContactCenter null (null,null)"],["OBJECT_NEW","OBJECT_NEW CLUSTER_CLIENT_MAP null (null,null)"],["OBJECT_NEW","OBJECT_NEW CLUSTER_NODE_HEARTBEAT null (null,null)"],["OBJECT_NEW","OBJECT_NEW MSCRM_CLUSTER_TOPIC null (null,null)"],["OBJECT_UPDATE","OBJECT_UPDATE CLUSTER_NODE_HEARTBEAT ADD (192.168.42.185_cc29ceea-98a6-4531-b3e7-cfcd42df97b7,ENCODED-B64H4sIAAAAAAAAAFvzloG1uIiBLyuxLFEvJzEvXc8nPy/duvvJhDP9yveZGBi9GFjLEnNKUyuKGAQQivxKc5NSi9rWTJXlnvKgm4mBoaKAgYEx48SuiT8Bnk2Pk1IAAAA=)"]]
	 */
	public JSONArray toDataTables () {
		JSONArray msgs 		= new JSONArray(); //messageQueue);
		
		for ( BaseDescriptor d : messageQueue) {
			JSONArray row 		= new JSONArray();
			row.put(d.type);
			row.put(d.duration);
			row.put(d.toString());
			
			msgs.put(row);
		}
		return msgs;
	}
}
