package com.cloud.cluster.multicast;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;

import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;

/**
 * Base descriptor for all Zeroconf service messages.
 * 
 * @author VSilva
 *
 */
public abstract class BaseDescriptor {
	// id of the message
	final String id;
	
	// IP address of the sender
	final String nodeAddress;
	
	// creation time
	final long timeCreated;
	
	// optional (key,value) map of attributes
	final Map<String, Object> attribs;
	
	// Message type enum
	final MessageType type;
	
	// expiration interval in milliseconds (time-t0-live)
	long duration;
	
	// Name of the sender node
	final String nodeName;
	
	/**
	 * Construct a message with a random unique UUID.
	 * @throws IOException on I/O errors.
	 */
	public BaseDescriptor(final MessageType type, Map<String, Object> attribs) throws IOException {
		this(type, UUID.randomUUID().toString(), attribs);
	}
	
	/**
	 * Construct a message with a given ID.
	 * @param id Unique ID of the message.
	 * @throws IOException on I/O errors.
	 */
	public BaseDescriptor(final MessageType type, final String id, Map<String, Object> attribs) throws IOException {
		if ( id == null ) 		throw new IOException("Base descriptor id is required.");
		this.type			= type;
		this.id 			= id;
		this.timeCreated 	= System.currentTimeMillis();
		this.nodeAddress	= IOTools.getHostIp();
		this.attribs		= attribs;
		this.nodeName		= CoreTypes.NODE_NAME;
	}
	
	public String getId() {
		return id;
	}
	
	public MessageType getType () {
		return type;
	}

	public String getAddress() {
		return nodeAddress;
	}
	
	/**
	 * Serialize to a JSON string
	 * @return JSON Serialized as implemented by each message. 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String toJSON () throws Exception {
		// attributes
		final JSONObject attr	= new JSONObject(attribs);

		return String.format("{\"uuid\" : \"%s\", \"nodeAddress\": \"%s\", \"nodeName\": \"%s\", \"messageType\" : \"%s\", \"timeCreated\": %d, \"timeToLive\": %d, \"attributes\" : %s }" 
				, id, nodeAddress, nodeName, type.name(), timeCreated, duration, attr.toString());
	}

	@Override
	public String toString() {
		return type.name();
	}
	
	/**
	 * @return True if the message has expired (now - timeCreated > duration).
	 */
	public boolean isExpired () {
		if ( duration == 0) {
			return false;
		}
		long now 		= System.currentTimeMillis();
		long delta 		= now - timeCreated;
		boolean expired = ( delta > duration) ? true : false;
		return expired;
	}
	
	// 	jar:file:/C:/Temp/Workspaces/CloudServices/Cloud-UnifiedContactCenter/.metadata/.plugins/org.eclipse.wst.server.core/tmp1/wtpwebapps/CloudConnectorNode002/WEB-INF/lib/LibCloudCluster.jar!/com/cloud/cluster/zeroconf/service.json
	static String loadResourceFromJARorFS (final URI uri) throws IOException {
		byte[] bytes = null;
		if ( uri.toString().startsWith("jar:") ) {
			final Map<String, String> env = new HashMap<String, String>();
			final String[] parts 	= uri.toString().split("!");
			final FileSystem fs 	= FileSystems.newFileSystem(URI.create(parts[0]), env);
			final Path path 		= fs.getPath(parts[1]);
			bytes 					= Files.readAllBytes(path);
			fs.close();
		}
		else {
			bytes 					= Files.readAllBytes(Paths.get(uri));
		}
		return new String(bytes, Charset.defaultCharset());
	}
	
}