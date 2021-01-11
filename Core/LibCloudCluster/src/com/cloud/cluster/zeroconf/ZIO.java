package com.cloud.cluster.zeroconf;

import java.io.IOException;
import java.io.Serializable;

import com.cloud.core.io.ObjectCache;

public class ZIO {

	/** Prefix used to serialize objects sent via UDP, so receivers know what to do */
	static final String ENC_OBJ_PREFIX = ObjectCache.ENC_OBJ_PREFIX;
	
	/**
	 * Encode an Object for delivery over UDPP.
	 * @param obj Any object type (It must implement the {@link Serializable} interface!
	 * @param compress Optionally GZIP the bytes.
	 * @return B64 encoded object withe the prefix (ENCODED-B64). For example: ENCODED-B64xxcccc...
	 * @throws IOException On I/O errors.
	 */
	static String encodeObject(Object obj, boolean compress) throws IOException {
		return ObjectCache.encodeObject(obj, compress);
	}

	/**
	 * Reverse operation of encodeObject()
	 * @param encoded A B64 encoded object (starts with ENC_OBJ_PREFIX). Note: The object must implement the {@link Serializable} interface.
	 * @param compressed If true the object is assumed to be compressed.
	 * @return The unserialized object.
	 * @throws IOException On I/O errors.
	 * @throws ClassNotFoundException On object casting errors.
	 */
	static Object decodeObject(String encoded, boolean compressed) throws IOException, ClassNotFoundException {
		return ObjectCache.decodeObject(encoded, compressed);
	}

	/**
	 * Reverse operation of encodeObject()
	 * @param encoded A B64 encoded object (starts with ENC_OBJ_PREFIX). Note: The object must implement the {@link Serializable} interface.
	 * <b>Note: The object is assumed to be compressed.</b>
	 * @return The unserialized object.
	 * @throws IOException On I/O errors.
	 * @throws ClassNotFoundException On object casting errors.
	 */
	static Object decodeObject(String encoded) throws IOException, ClassNotFoundException {
		return decodeObject(encoded, true);
	}
	
}
