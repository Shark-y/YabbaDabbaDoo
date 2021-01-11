package com.cluster;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.zeroconf.ZTopic;
import com.cloud.cluster.multicast.ZeroMessageContainer;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.cluster.multicast.ZeroDescriptorObject.UpdateType;
import com.cloud.core.io.ObjectCache;
import com.cloud.core.io.ObjectCache.ObjectType;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Multi-cast service implementation used by the Cluster Manager WeApp to receive node information
 * 
 * @author VSilva
 * 
 * @version 1.0.0 - 12/20/2018 Initial implementation.
 *
 */
public class ZeroConfService {
	
	static final Logger log 						= LogManager.getLogger(ZeroConfService.class);

	// singleton
	private static ZeroConfService instance;  // 	= new ZeroConfService();
	
	// Auto Discovery service
	private final ZeroConfDiscovery ds;
	
	// Used to hold messages
	private final ZeroMessageContainer container;
	
	// Stores distributed objects
	private final ObjectCache cache;

	// Id of this node (must be static so It is the same across all instances).
	private static final String uuid 	= UUID.randomUUID().toString();

	/**
	 * This class is a singleton.
	 * @return {@link ZeroConfService} singleton.
	 * @throws IOException 
	 */
	public static synchronized ZeroConfService getInstance() throws IOException {
		if ( instance == null) {
			instance = new ZeroConfService();
		}
		return instance;
	}
	
	private ZeroConfService() throws IOException {
		ds 			= ZeroConfDiscovery.getInstance();
		container	= new ZeroMessageContainer();
		cache		= new ObjectCache();
	}
	
	public void start() throws IOException {
		/* 1/16/2019 Deprecated Send DB connection message
		final String url 	= "jdbc:derby://" + IOTools.getHostIp() + ":1527/clusterdb";
		final String driver	= "org.apache.derby.jdbc.ClientDriver";
		
		// default derby user/pwd
		final String user	= "APP";	
		final String pwd	= "APP";
		
		ZeroDescriptorClusterUp cd = ZeroDescriptorClusterUp.create(driver, url, user, pwd);
		*/
		//ds.setBCastFrequency(10);
		ds.joinGroup();
		ds.receive(new ZeroConfDiscovery.MessageListener() {
			@Override
			public void onTextMessage(String message) {
				//System.out.println("CLUSTER-MANAGER Receiver: Got " + message);
				try {
					final JSONObject root 	= new JSONObject(message);
					final String type		= root.optString(ZeroConfDiscovery.JSONK_MTYPE);
					
					if ( type.equals(ZeroConfDiscovery.MessageType.SERVICE_UP.name())) {
						// add member
						container.add(message);
					}
					else if ( type.equals(ZeroConfDiscovery.MessageType.SERVICE_DOWN.name())) {
						container.remove(root.getString(ZeroConfDiscovery.JSONK_ID));
					}
					else if ( type.equals(ZeroConfDiscovery.MessageType.OBJECT_NEW.name())) {
						handleObjectNew(root); 
					}
					else if ( type.equals(ZeroConfDiscovery.MessageType.OBJECT_UPDATE.name())) {
						handleObjectUpdate(root); 
					}
					
					//container.add(message);
				} catch (Exception e) {
					log.error("Cluster ZeroConf Message receiver", e);
				}
			}
		});
		
		// log.info("Started ZeroConf Bcast with cluster descriptor " + url);
		// Deprecated ds.queue(cd);
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

		//System.out.println("ZERO-UPDATE:" + name + " Op:" + type + " k:" + key + " V:" + value + " CACHE:" + cache.toDataTables());
		// NUllPointer here when ADD invoked and name doesn't exist in the cache
		if ( !cache.containsKey(name)) {
			return;
		}
		
		switch (type) {
		case ADD:
			cache.get(name).add(key, value);
			break;
		case CLEAR:
			// Flush all ADD messages for 'name'
			ds.flushObjectMessages(MessageType.OBJECT_UPDATE, UpdateType.ADD, name);
			cache.get(name).clear();
			break;
		case REMOVE:
			cache.get(name).remove(value);
			break;
		case PUBLISH:
			/*
			if ( cache.containsKey(name)) {
				ZTopic topic =  (ZTopic)cache.get(name).getObject();
				topic.dispatch(value);
			}
			else {
				log.error("ZERO-UPDATE: Can't dispatch msg. Topic " + name + " not found.");
			}*/
			break;
			
		}
	}
	
	public void stop() {
		try {
			log.info("Stoping ZeroConf broadcast.");
			
			ds.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void collectGarbage() {
		// clean expired messages
		container.cleanup();
	}
	
	/**
	 * <pre> [{
	"uuid" : "cfa3b034-660d-4be0-90f4-4f6b997aa008",
	"messageType" : "SERVICE_UP",
	"address" : "IPADDR"
	"attributes" : {...}
	"lifeCycle": {
		"start": "start",
		"stop" : "stop",
		"status": "status"
	},
	"configure": {
		"get": "get",
		"store" : "store"
	}
	,"logging":{ "get": "logget", "clear": "logclear"}}...] </pre>
	 *
	 * @author VSilva
	 */
	public JSONArray getMessages() throws JSONException {
		return container.toJSON();
	}

	/**
	 * Get a JSON description of all distributed collections in Data tables format.
	 * @return { "data": [[ROW1],[ROW2],...]} where ROW [NAME, TYPE, EXPIRED, CONTENT]
	 * @throws JSONException 
	 */
	public JSONObject describe () throws JSONException {
		JSONObject root = new JSONObject();
		root.put("data", cache.toDataTables());
		return root;
	}
}
