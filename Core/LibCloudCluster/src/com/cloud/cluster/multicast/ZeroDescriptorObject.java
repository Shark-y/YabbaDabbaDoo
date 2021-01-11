package com.cloud.cluster.multicast;

import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;

import com.cloud.cluster.multicast.BaseDescriptor;
import com.cloud.cluster.multicast.ZeroDescriptorObject;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.core.io.ObjectCache.ObjectType;

/**
 * A descriptor used to multicast object messages over M-cast UDP such as: Maps, Lists, Primitives, etc.
 * <li> See {@link ObjectType}
 * <li> See {@link UpdateType}
 * <pre> {
 "objectKey": "key1",
 "objectName": "MAP1",
 "objectType": "T_MAP",
 "address": "192.168.56.1",
 "objectUpdateType": "ADD",
 "timeCreated": 1547351019130,
 "attributes": {},
 "uuid": "cbb2e3f5-9660-437a-88dc-fd318d58b972",
 "objectVal": "val1",
 "messageType": "OBJECT_UPDATE",
 "timeSent": 1547351019147
}
 * </pre>
 *
 * @author VSilva
 *
 */
public class ZeroDescriptorObject extends BaseDescriptor {

	public enum UpdateType {
		ADD, REMOVE, CLEAR, PUBLISH
	}
	
	final String objectName;
	final ObjectType objectType;
	final Object objectKey;
	final Object objectVal;
	final UpdateType updateType;
	

	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @throws IOException On I/O errors.
	 */
	public ZeroDescriptorObject(final MessageType mtype, ObjectType type, final String objectName) throws IOException {
		super(mtype, null);
		this.objectType = type;
		this.objectName = objectName;
		this.objectKey	= null;
		this.objectVal	= null;
		this.updateType = null;
	}
	
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @throws IOException On I/O errors.
	 */
	public ZeroDescriptorObject(final MessageType type, final UpdateType updateType, final String objectName) throws IOException {
		super(type, null);
		this.objectName = objectName;
		this.updateType = updateType;
		this.objectKey	= null;
		this.objectVal	= null;
		this.objectType = null;
	}
	
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @throws IOException On I/O errors.
	 */
	public ZeroDescriptorObject(final MessageType type, final UpdateType updateType, final String objectName, final Object key, final Object val) throws IOException {
		super(type, null);
		this.objectName = objectName;
		this.objectKey	= key;
		this.objectVal	= val;
		this.updateType = updateType;
		this.objectType = null;
	}
	
	/**
	 * Construct.
	 * @param Unique UUI for the message.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @param attribs Optional map of attributes.
	 * @throws IOException On I/O errors.
	 */
	public ZeroDescriptorObject(final MessageType type,final UpdateType updateType, final String id, final String objectName, final Object key, final Object val, final Map<String, Object> attribs) 
			throws IOException {
		super(type, id, attribs);
		this.objectName	= objectName;
		this.objectKey	= key;
		this.objectVal	= val;
		this.updateType = updateType;
		this.objectType = null;
	}
	
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @param attribs Optional map of attributes.
	 * @throws IOException On I/O errors.
	 */
	public ZeroDescriptorObject(final MessageType type, final UpdateType updateType, final String objectName, final Object key, final Object val, final Map<String, Object> attribs) throws IOException {
		super(type, attribs);
		this.objectName = objectName;
		this.objectKey	= key;
		this.objectVal	= val;
		this.updateType	= updateType;
		this.objectType = null;
	}

	
	public String getObjectName() {
		return objectName;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public Object getObjectKey() {
		return objectKey;
	}

	public Object getObjectVal() {
		return objectVal;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}

	/**
	 * { "objectKey": "key1",
 "objectName": "MAP1",
 "address": "192.168.56.1",
 "timeCreated": 1547351019130,
 "attributes": {},
 "uuid": "cbb2e3f5-9660-437a-88dc-fd318d58b972",
 "objectVal": "val1",
 "messageType": "OBJECT_SETVALUE",
 "timeSent": 1547351019147
}
	 */
	@Override
	public String toJSON() throws Exception {
		final JSONObject root = new JSONObject(super.toJSON());
		root.put("timeSent", System.currentTimeMillis());
		root.putOpt("objectType", objectType);
		root.put("objectName", objectName);
		root.putOpt("objectKey", objectKey);
		root.putOpt("objectVal", objectVal);
		root.putOpt("objectUpdateType", updateType);
		return root.toString(1);
	}

	@Override
	public String toString() {
		return objectName + " " + (updateType != null ? updateType : "") 
				+  ((objectKey != null) ? " Key: " + objectKey  : "" )
				+  ((objectVal != null) ? " Val: " + objectVal : "" );
	}
	
	/**
	 * Set the time to live or expiration interval of the message.
	 * @param ttl Expiration interval (time-to-live) in milliseconds.
	 */
	public void setTimeToLive (long ttl) {
		super.duration = ttl;
	}
	
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @return A {@link ZeroDescriptorObjectNew}.
	 * @throws IOException On I/O errors.
	 */
	public static ZeroDescriptorObject create (final MessageType mtype, final ObjectType type, final String objectName) throws IOException {
		return new ZeroDescriptorObject(mtype, type, objectName);
	}
	
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @return A {@link ZeroDescriptorObject}.
	 * @throws IOException On I/O errors.
	 */
	public static ZeroDescriptorObject create (final MessageType type, final UpdateType updateType, final String objectName) throws IOException {
		return new ZeroDescriptorObject(type, updateType, objectName);
	}
	
	/**
	 * Construct an object descriptor..
	 * @param type See {@link MessageType}.
	 * @param updateType The {@link UpdateType}.
	 * @param objectName Name of the object.
	 * @param key An optional key (for a MAP) used to store the value.
	 * @param val The value to assign to the object.
	 * @throws IOException On I/O errors.
	 */
	public static ZeroDescriptorObject create (final MessageType type, final UpdateType updateType, final String objectName, final Object key, final Object val ) throws IOException {
		return new ZeroDescriptorObject(type, updateType, objectName, key, val, null);
	}
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @param attribs Optional map of attributes.
	 * @return A {@link ZeroDescriptorObject}.
	 * @throws IOException On I/O errors.
	 */
	public static ZeroDescriptorObject create (final MessageType type, final UpdateType updateType, final String uuid, final String objectName, final Object key, final Object val, Map<String, Object> attribs) throws IOException {
		return new ZeroDescriptorObject(type, updateType, uuid,  objectName, key, val, attribs);
	}
	/**
	 * Construct.
	 * @param type See {@link ObjectType}.
	 * @param objectName Name of the object.
	 * @param attribs Optional map of attributes.
	 * @return A {@link ZeroDescriptorObject}.
	 * @throws IOException On I/O errors.
	 */
	public static ZeroDescriptorObject create (final MessageType type, final UpdateType updateType, final String objectName, final Object key, final Object val, final Map<String, Object> attribs) throws IOException {
		if ( attribs == null) 		throw new IOException("ObjectNew: Attributes cannot be null. Use create() instead.");
		return new ZeroDescriptorObject(type, updateType, objectName, key, val, attribs);
	}
}
