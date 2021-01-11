package com.cloud.cluster.zeroconf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.ClusterMember;
import com.cloud.cluster.IClusterInstance;
import com.cloud.cluster.IClusterLock;
import com.cloud.cluster.IClusterTopic;
import com.cloud.cluster.IClusterTopicListener;
import com.cloud.cluster.IMap;
import com.cloud.cluster.db.DBClusterInstance;
import com.cloud.cluster.multicast.EndPoint;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroDescriptorService;
import com.cloud.cluster.multicast.ZeroMessageContainer;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.cluster.multicast.ZeroDescriptorObject.UpdateType;
import com.cloud.cluster.zeroconf.ZList;
import com.cloud.cluster.zeroconf.ZMap;
import com.cloud.cluster.zeroconf.ZTopic;
import com.cloud.core.io.IOTools;
import com.cloud.core.io.ObjectCache;
import com.cloud.core.io.ObjectCache.ObjectType;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.OSMetrics;

/**
 * An implementation of {@link IClusterInstance} using the {@link ZeroConfDiscovery} protocol
 * for all dev-ops.
 * <ul>
 * <li> It uses the {@link ZeroConfDiscovery} protocol to multi cast a service endpoint via {@link ZeroDescriptorServiceUp}.
 * <li> It listens for {@link ZeroDescriptorClusterUp} messages to provide a cluster implementation similar to Hazelcast via {@link DBClusterInstance}.
 * <li> It uses an instance of {@link DBClusterInstance} to connect to the universal console cluster DB for clustered collections: Maps, Lists, etc.
 * </ul>
 * @author VSilva
 * @version 1.0.0 12/31/2018 - Initial implementation.
 */
public final class ZeroClusterInstance implements IClusterInstance {
	
	static final Logger log 		= LogManager.getLogger(ZeroClusterInstance.class);
	
	// multi cast discovery
	final ZeroConfDiscovery ds;
	
	// Used to store objects
	private final ObjectCache cache;
	
	// Local Node service descriptor
	private final ZeroDescriptorService service;
	
	// collects cluster members
	private final ZeroMessageContainer members;
	
	// Id of the service descriptor for this node (must be static so It is the same across all instances).
	private static final String uuid 	= UUID.randomUUID().toString();
	
	// local node member
	private final ClusterMember local = new ClusterMember(uuid, true, IOTools.getHostIp(), new HashMap<String, Object>());

	/**
	 * Start the zero conf cluster.
	 * @param attributes Startup parameters: {KEY_NODE_GRP=dev, KEY_CTX_PATH=/CloudConnectorNode002, server_failOverType=CLUSTER_ZEROCONF, KEY_CTX_URL=http://192.168.56.1:8080/CloudConnectorNode002/}
	 * @throws IOException on I/O errors.
	 */
	public ZeroClusterInstance(final Map<String, Object> attributes) throws IOException {
		final StringBuffer buf = new StringBuffer("---- ZEROCONF INIT with attributes --------\n" );
		
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			buf.append("-- " + entry.getKey() + " = " + entry.getValue() + "\n");
		}
		members				= new ZeroMessageContainer();
		cache				= new ObjectCache();
		
		if (!attributes.containsKey(CloudCluster.KEY_CTX_URL)) {
			throw new IOException("A node base URL (KEY_CTX_URL) is required.");
		}
		if (!attributes.containsKey(CloudCluster.KEY_PRODUCT_TYPE)) {
			throw new IOException("A node product type (productType) is required.");
		}
		
		final String baseURL 		= attributes.get(CloudCluster.KEY_CTX_URL).toString();
		final String productType 	= attributes.get(CloudCluster.KEY_PRODUCT_TYPE).toString();
		
		// End-point URLs.
		EndPoint start 		= EndPoint.post(baseURL + "/SysAdmin?rq_operation=start");
		EndPoint stop 		= EndPoint.post(baseURL + "/SysAdmin?rq_operation=stop");
		EndPoint status 	= EndPoint.get(baseURL 	+ "/OSPerformance");
		EndPoint confget 	= EndPoint.get(baseURL 	+ "/SysAdmin?op=confget&productType=" + productType);
		EndPoint confstore 	= EndPoint.post(baseURL + "/SysAdmin?rq_operation=confstore&productType=" + productType);
		EndPoint logget 	= EndPoint.get(baseURL 	+ "/LogServlet");
		EndPoint logview 	= EndPoint.get(baseURL 	+ "/log/logview.jsp");
		EndPoint logclear 	= EndPoint.post(baseURL + "/LogServlet?op=clear&len=0");
		
		service 			= ZeroDescriptorService.createServiceUp(uuid, start, stop, status, confget, confstore, logget, logview, logclear, new HashMap<String, Object>(attributes));
		ds 					= ZeroConfDiscovery.getInstance();

		ds.joinGroup();

		buf.append("-- Time to live (TTL) = " + ds.getTimeToLive() + "\n");
		buf.append("---------------------------------");
		log.info(buf.toString());
		
		ds.receive(new ZeroConfDiscovery.MessageListener() {
			@Override
			public void onTextMessage(String message) {
				//System.out.println("ZERO Receiver: Got " + message);
				try {
					
					final JSONObject root 	= new JSONObject(message);
					final String type		= root.optString(ZeroConfDiscovery.JSONK_MTYPE);
					
					if ( type.equals(ZeroConfDiscovery.MessageType.SERVICE_UP.name())) {
						// add member
						members.add(message);
					}
					else if ( type.equals(ZeroConfDiscovery.MessageType.SERVICE_DOWN.name())) {
						members.remove(root.getString(ZeroConfDiscovery.JSONK_ID));
					}
					else if ( type.equals(ZeroConfDiscovery.MessageType.OBJECT_NEW.name())) {
						handleObjectNew(root); 
					}
					else if ( type.equals(ZeroConfDiscovery.MessageType.OBJECT_UPDATE.name())) {
						handleObjectUpdate(root); 
					}
				} catch (Exception e) {
					log.error("ZeroConf onTextMessage (invalid message) " + message, e);
				}
			}
		});
		
		ds.sendAndQueue(service);	
	}

	/**
	 * @param root { "objectName": "LIST1", "address": "192.168.56.1", "timeCreated": 1547349106513,
 "attributes": {},
 "uuid": "6bf320eb-3c1b-474c-ab50-4da4409d0bf8",
 "messageType": "OBJECT_NEW",
 "objectType": "T_LIST",
 "timeSent": 1547349106513
}
	 * @throws JSONException 
	 */
	private void handleObjectNew (final JSONObject root) throws JSONException {
		final ObjectType type 	= ObjectType.valueOf(root.getString("objectType"));
		final String name		= root.getString("objectName");
		//System.out.println("ZERO-NEW:" + name + " t:" + type + " CACHE: " + cache.toDataTables());
		if ( cache.containsKey(name)) {
			return;
		}
		switch (type) {
		case T_LIST:
			cache.add(name, type, new CopyOnWriteArrayList<Object>());
			break;
		case T_MAP:
			cache.add(name, type, new ConcurrentHashMap<String, Object>());
			break;
		case T_PRIMITIVE:
			cache.add(name, type, new Object());
			break;
		case T_QUEUE:
			cache.add(name, type, new ConcurrentLinkedDeque<Object>());
			break;
		case T_TOPIC:
			ZTopic topic = new ZTopic(cache, ds, uuid, name, null);
			cache.add(name, ObjectType.T_TOPIC, topic);
			break;
		}
		//System.out.println("ZERO-NEW:" + name + " t:" + type + " CACHE: " + cache.toDataTables());
	}

	/**
	 * @param root { "objectKey": "key1", "objectName": "MAP1", "objectUpdateType": "ADD",
 "address": "192.168.56.1",
 "timeCreated": 1547351019130,
 "attributes": {},
 "uuid": "cbb2e3f5-9660-437a-88dc-fd318d58b972",
 "objectVal": "val1",
 "messageType": "OBJECT_SETVALUE",
 "timeSent": 1547351019147
}
	 * @throws JSONException 
	 * @throws IOException 
	 */
	private void handleObjectUpdate (final JSONObject root) throws JSONException, IOException {
		final String name		= root.getString("objectName");
		final String key		= root.optString("objectKey");
		final Object value		= root.opt("objectVal");
		final UpdateType type	= UpdateType.valueOf(root.getString("objectUpdateType"));

		if ( !cache.containsKey(name)) {
			return;
		}
		//System.out.println("ZERO-UPDATE:" + name + " Op:" + type + " k:" + key + " V:" + value + " CACHE:" + cache.toDataTables());
		switch (type) {
		case ADD:
			// TODO Test this - FLush REMOVE messages in this case of new additions: ADD, ADD,... REMOVE, ADD, ....
			ds.flushObjectMessagesForKey(MessageType.OBJECT_UPDATE, UpdateType.REMOVE, name, key);
			cache.get(name).add(key, value);
			break;
		case CLEAR:
			// Flush all ADD messages for 'name'
			ds.flushObjectMessages(MessageType.OBJECT_UPDATE, UpdateType.ADD, name);
			cache.get(name).clear();
			break;
		case REMOVE:
			// Flush all ADD messages for 'name', 'key'
			ds.flushObjectMessagesForKey(MessageType.OBJECT_UPDATE, UpdateType.ADD, name, key);
			cache.get(name).remove(value);
			break;
		case PUBLISH:
			ZTopic topic =  (ZTopic)cache.get(name).getObject();
			topic.dispatch(value);
			break;
		}
	}
	
	@Override
	public IClusterLock getLock(String name) {
		throw new RuntimeException("getLock(String name) not avialable.");
	}

	@Override
	public <K, V> IMap<K, V> getMap(String name) {
		return new ZMap<K, V>(cache, ds, name);
	}

	@Override
	public <V> List<V> getList(String name) {
		return new ZList<V>(cache, ds, name);
	}

	@Override
	public Object getAtomicLong(String name) {
		throw new RuntimeException("getAtomicLong(String name) not avialable.");
	}

	@Override
	public IClusterTopic getTopic(String name, IClusterTopicListener listener) {
		ZTopic topic = new ZTopic(cache, ds, uuid, name, listener);
		cache.add(name, ObjectType.T_TOPIC, topic);
		return topic;
	}

	@Override
	public Map<String, Object> getMemberAttributes() {
		// TODO
		return null;
	}

	@Override
	public List<ClusterMember> getMembers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONArray getMembersAsJSON() throws JSONException {
		return members.toJSON();
	}

	@Override
	public void setLocalMemberStatus(int code, String message) {
		try {
			service.setAttribute("statusCode", code);
			service.setAttribute("statusMessage", message);
			
		} catch (Exception e) {
			log.error("Set Local Memeber status: " + code + "/" + message, e);
		}

		// plus OS metrics
		setLocalMemberOSMetrics();
	}

	@Override
	public void setLocalMemberAttribute(String key, String value) {
		try {
			service.setAttribute(key, value);
		} catch (IOException e) {
			log.error("Set memeber attribute " + key + "/" + value, e);
		}
	}

	@Override
	public String getLeader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isClusterLeader() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void relinquishLeadership() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown()  {
		try {
			log.info("--------- ZEROCONF SHUTDOWN -----------");
			// send shutdown msg
			ZeroConfDiscovery.getInstance().sendAndQueue(ZeroDescriptorService.createServiceDown(service.getId(), service.getAttributes()));
			
			// close the Zero service
			ZeroConfDiscovery.getInstance().shutdown();
			
		} catch (IOException e) {
			log.error("ZeroConf shutdown", e);
		}
	}

	@Override
	public String getClusterGroupName() {
		return "dev";	// default
	}

	@Override
	public List<String> getClusterTcpMembers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isShutdown() {
		return ds.isClosed();
	}

	@Override
	public void dumpAllInstances(String label) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLocalMemberOSMetrics() {
		OSMetrics.injectOSMetrics(service.getAttributes());
	}

	@Override
	public ClusterMember getLocalMember() {
		return local;
	}

	/**
	 * Method used to perform maintenance & garbage collection tasks.
	 */
	public void collectGarbage() {
		members.cleanup();
	}

	/**
	 * Get a JSON description of all distributed collections in Data tables format.
	 * @return { "data": [[ROW1],[ROW2],...]} where ROW [NAME, TYPE(T_LIST, T_MAP, T_TOPIC, T_PRIMITIVE), CONTENT, EXPIRED(boolean)]
	 * @throws JSONException On JSON errors.
	 */
	@Override
	public JSONObject describe() throws JSONException {
		JSONObject root = new JSONObject();
		root.put("data", cache.toDataTables());
		return root;
	}

	/**
	 * Get a JSON description of the multicast queue in Data tables format.
	 * @return { "data": [[ROW1],[ROW2],...]} where ROW [MESSAGET_TYPE, MESSAGE_CONTENT]
	 * @throws JSONException On JSON errors.
	 */
	public JSONObject getMessageQueue () throws JSONException {
		JSONObject root = new JSONObject();
		root.put("data", ds.toDataTables());
		return root;
	}
}
