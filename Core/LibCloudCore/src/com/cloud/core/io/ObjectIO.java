package com.cloud.core.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.cloud.core.io.Base64;
import com.cloud.core.io.IOTools;

/**
 * Object compression & serialization tools. These can be used to compress objects in the heap & save memory
 * for high frequency apps.
 * <pre>
 *  JSONObject root = new JSONObject();
 *  byte[] encoded 	= ObjectIO.compressObject(root);
 *  
 *  JSONObject root1 = (JSONOObject)ObjectIO.deCompressObject(encoded);
 *  
 *  print ( root == root1);
 *  
 * </pre>
 * @author VSilva
 *
 */
public class ObjectIO {

	/**
	 * Convert an array of bytes into the original object.
	 * @param bytes Serialized object.
	 * @return The original object.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object unserializeObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
		ObjectInputStream os = new ObjectInputStream(bos);
		Object obj = os.readObject();
		os.close();
		return obj;
	}

	/**
	 * Convert an object to an array of bytes.
	 * @param obj The object to encode. Must implement {@link Serializable}.
	 * @return Serialized array of bytes.
	 * @throws IOException
	 */
	public static byte[] serializeObject(Object obj) throws IOException {
		ByteArrayOutputStream bos 	= new ByteArrayOutputStream();
		ObjectOutputStream os 		= new ObjectOutputStream(bos);
		os.writeObject(obj);
		os.close();
		return bos.toByteArray();
	}

	/**
	 * Equivalent to C/C++ sizeof(Object). Serialize the object and return its size in memory.
	 * @param obj An object that implements the {@link Serializable} interface.
	 * @return The size it occupies in memory.
	 * @throws IOException
	 */
	public static int sizeOf (Object obj) throws IOException {
		return serializeObject(obj).length;
	}
	
	/**
	 * Serialize an object to a byte[] and encode it as {@link Base64}.
	 * @param obj the object to encode. Must implement the {@link Serializable} interface.
	 * @return {@link Base64} encoded object.
	 * @throws IOException
	 */
	public static String encodeObjectAsB64(Object obj) throws IOException {
		return Base64.encode(serializeObject(obj));
	}

	/**
	 * Decode a {@link Base64} encoded object.
	 * @param encoded The b64 encoded object.
	 * @return the original Object.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object decodeObjectFromB64(String encoded) throws IOException, ClassNotFoundException {
		return unserializeObject(Base64.decode(encoded));
	}

	/**
	 * Compress a buffer & encode in Base64.
	 * @param buf Byte buffer.
	 * @return B64 encoded of the GZIPed buffer.
	 * @throws IOException
	 */
	public static String b64Gzip (final byte[] buf) throws IOException {
		return Base64.encode(gzip(buf));
	}

	/**
	 * De-compress a B64 encoded buffer.
	 * @param b64 Base 64 encodded GZIped buffer.
	 * @return The original byte buffer.
	 * @throws IOException
	 */
	public static byte[] b64Gunzip (final String b64) throws IOException {
		return gunzip(Base64.decode(b64));
	}

	/**
	 * GZIP a byte buffer.
	 * @param buf The buffer.
	 * @return Compressed buffer
	 * @throws IOException
	 */
	public static byte[] gzip (final byte[] buf) throws IOException {
		final ByteArrayInputStream in = new ByteArrayInputStream(buf);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final GZIPOutputStream gzip = new GZIPOutputStream(out);
		IOTools.pipeStream(in, gzip);
		in.close();
		gzip.close();
		return out.toByteArray();
	}

	/**
	 * De compress a buffer using GUNZP..
	 * @param buf Compressed buffer.
	 * @return Original buffer.
	 * @throws IOException
	 */
	public static byte[] gunzip (final byte[] buf) throws IOException {
		final GZIPInputStream gzip 		= new GZIPInputStream(new ByteArrayInputStream(buf));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOTools.pipeStream(gzip, out);
		gzip.close();
		out.close();
		return out.toByteArray();
	}

	public static boolean isGZipCompressed(final byte[] bytes) throws IOException {
		if ((bytes == null) || (bytes.length < 2)) {
			return false;
		} else {
			return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
					&& (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
		}
	}
	
	/**
	 * Serialize & GZIP an object.
	 * @param obj Object to compress.
	 * @return byte array of serialized/gziped object.
	 * @throws IOException
	 */
	public static byte[] compressObject (Object obj) throws IOException {
		return gzip(serializeObject(obj));
	}

	/**
	 * Decompress/deserialize a byte buffer into the original object. Counterpart for compressObject.
	 * @param encoded Byte buffer representing an encoded object.
	 * @return The original object.
	 * @throws IOException on compression errors
	 * @throws ClassNotFoundException on serialization errors.
	 */
	public static Object deCompressObject (byte[] encoded) throws IOException, ClassNotFoundException {
		return unserializeObject(gunzip(encoded));
	}

}
