package com.cloud.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;

/**
 * This class wraps a Hazelcast cluster member.
 * @author VSilva
 *
 */
public class ClusterMember {
	/** Id of the member */
	public static final String KEY_UUID 	= "uuid";
	
	/** True if the member is local to the current node */
	public static final String KEY_ISLOCAL 	= "isLocal";
	
	/** IP address:PORT of the node */
	public static final String KEY_IP 		= "address";

	/** IP address of the node */
	public static final String KEY_NODEIP 	= "nodeAddress";

	/** User defined Hashmap of node attributes */
	public static final String KEY_ATTRIBS 	= "attributes";

	String uuid;
	boolean isLocal;
	String address;
	Map<String, Object> attributes;
	
	public ClusterMember() {
		uuid 		= UUID.randomUUID().toString();
		isLocal		= true;
		address		= IOTools.getHostIp();
		attributes	= new HashMap<String, Object>();
	}
	
	/**
	 * Create a New Cluster member.
	 * @param uuid Unique Hazelcast Id of the member.
	 * @param isLocal True if the member is a local node.
	 * @param address IP address if the member.
	 * @param attributes User defined hash map of member attributes.
	 */
	public ClusterMember(String uuid, boolean isLocal, String address,	Map<String, Object> attributes) {
		super();
		this.uuid		= uuid;
		this.isLocal 	= isLocal;
		this.address 	= address;
		this.attributes = attributes;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	
	public String getUuid () {
		return uuid;
	}
	
	@Override
	public String toString() {
		return uuid + " Local: " + isLocal + ", Adress: " + address + ", Attribs: " + attributes;
	}
	
	/**
	 * Serialize the member to a {@link JSONObject}.
	 * @return <pre>{'uuid': 'id', 'islocal': true
	 * , 'address': 'IP'
	 * , 'attributes': {'k1':'v1','k2':'v2',...}}</pre>
	 * @throws JSONException
	 */
	public JSONObject toJSON () throws JSONException {
		JSONObject root = new JSONObject();
		root.put(KEY_UUID,		uuid);
		root.put(KEY_ISLOCAL, 	isLocal);
		root.put(KEY_IP, 		address);
		// 8/2/2019 fix for JS error TypeError: node.nodeAddress is undefined in jsp/config/config_cluster.jsp
		root.put(KEY_NODEIP, 	address);
		root.put(KEY_ATTRIBS, 	new JSONObject(attributes));
		return root;
	}
}