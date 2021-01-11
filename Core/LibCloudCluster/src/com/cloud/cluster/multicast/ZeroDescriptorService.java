package com.cloud.cluster.multicast;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.cloud.cluster.multicast.BaseDescriptor;
import com.cloud.cluster.multicast.EndPoint;
import com.cloud.cluster.multicast.ZeroDescriptorService;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.core.profiler.OSMetrics;

/**
 * <pre> {
	"uuid": "12376557",
	"address": "10.11.3.122",
	"messageType": "SERVICE",
	"timeCreated": 1546188156325,
	"timeSent": 1546188156512,
	"attributes": {
		"statusCode": 200,
		"KEY_CTX_PATH": "/Node001",
		"KEY_CTX_URL": "http://localhost:8080/Node001",
		"statusMessage": "Online"
	},
	"lifeCycle": {
		"start": {
			"method": "GET",
			"url": "http://localhost:8080/start",
			"headers": null
		},
		"stop": {
			"method": "GET",
			"url": "http://localhost:8080/stop",
			"headers": null
		},
		"status": {
			"method": "GET",
			"url": "http://localhost:8080/status",
			"headers": null
		}
	},
	"configure": {
		"get": {
			"method": "GET",
			"url": "http://localhost:8080/get",
			"headers": null
		},
		"store": {
			"method": "POST",
			"url": "/store",
			"headers": null
		}
	},
	"logging": {
		"get": {
			"method": "GET",
			"url": "http://localhost:8080/logget",
			"headers": null
		},
		"clear": {
			"method": "POST",
			"url": "http://localhost:8080/logclear",
			"headers": null
		}
	}
}</pre>

 * @author VSilva
 * @version 1.0.1 -1/11/2019 Initial commit.
 *
 */
public class ZeroDescriptorService extends BaseDescriptor {
	// array used to st
	final EndPoint[] urls = new EndPoint[8];
	
	
	public ZeroDescriptorService(MessageType type, Map<String, Object> attribs) throws IOException {
		super(type, attribs);
	}
	
	public ZeroDescriptorService(final String id, MessageType type, final Map<String, Object> attribs) throws IOException {
		super(type, id, attribs);
	}
	
	public ZeroDescriptorService setLifecycleUrls( final EndPoint start, final EndPoint stop, final EndPoint status) throws IOException  {
		if ( start == null ) 	throw new IOException("Service lifecycle (START) URL is required.");
		if ( stop == null ) 	throw new IOException("Service lifecycle (STOP) URL is required.");
		if ( status == null )	throw new IOException("Service lifecycle (STATUS) URL is required.");
		
		urls[0] = start;
		urls[1] = stop;
		urls[2] = status;
		return this;
	}
	public ZeroDescriptorService setConfigurationUrls( final EndPoint get, final EndPoint store) throws IOException {
		if ( get == null ) 		throw new IOException("Service configuration (GET) URL is required.");
		if ( store == null ) 	throw new IOException("Service configuration (STORE) URL is required.");

		urls[3] = get;
		urls[4] = store;
		return this;
	}
	public ZeroDescriptorService setLoggingUrls(final EndPoint get,  final EndPoint view, final EndPoint clear) throws IOException {
		if ( get == null ) 	throw new IOException("Service logging (GET) URL is required.");
		//if ( view == null ) 	throw new IOException("Service logging (VIEW) URL is required.");
		//if ( clear == null ) 	throw new IOException("Service logging (CLEAR) URL is required.");

		urls[5] = get;
		urls[6] = view;
		urls[7] = clear;
		return this;
	}
	
	public ZeroDescriptorService setAttribute (final String key, final Object value) throws IOException {
		if ( attribs == null) {
			throw new IOException("Attributes for object " + id + " are NULL.");
		}
		attribs.put(key, value);
		return this;
	}
	
	public Map<String, Object> getAttributes () {
		return attribs;
	}
	
	public String toJSON () throws IOException, URISyntaxException {
		//jar:file:/C:/Temp/Workspaces/CloudServices/Cloud-UnifiedContactCenter/.metadata/.plugins/org.eclipse.wst.server.core/tmp1/wtpwebapps/CloudConnectorNode002/WEB-INF/lib/LibCloudCluster.jar!/com/cloud/cluster/zeroconf/service.json
		final URI uri 		= type == MessageType.SERVICE_UP 
				? BaseDescriptor.class.getResource("service_up.json").toURI()
				: BaseDescriptor.class.getResource("service_down.json").toURI();
				
		// load JSON
		final String str 	= BaseDescriptor.loadResourceFromJARorFS(uri);
		final long timeSent	= System.currentTimeMillis();

		// attributes
		final JSONObject attr	= new JSONObject(attribs);
		
		// optional logging
		final String json 	= type == MessageType.SERVICE_UP 
			? String.format(str, id, nodeAddress, nodeName, MessageType.SERVICE_UP.name(), timeCreated, timeSent, attr.toString()
				, urls[0], urls[1], urls[2]		// life cycle
				, urls[3], urls[4]				// configuration
				, urls[5], urls[6], urls[7]		// logging
				)
			: String.format(str, id, nodeAddress, nodeName, MessageType.SERVICE_DOWN.name(), timeCreated, timeSent, attr.toString() );
		
		return json;
	}

	@Override
	public String toString() {
		return Arrays.toString(urls);
	}
	
	/**
	 * Create a service descriptor.
	 * @param start Life-cycle start-service {@link EndPoint}.
	 * @param stop Life-cycle stop-service {@link EndPoint}.
	 * @param status Life-cycle service status {@link EndPoint}.
	 * @param confget Configuration get {@link EndPoint}.
	 * @param confstore Configuration save {@link EndPoint}.
	 * @param logget Logging get events {@link EndPoint}.
	 * @param logview Logging optional log viewer {@link EndPoint}.
	 * @param logclear Logging log clear {@link EndPoint}.
	 * @return a {@link ZeroDescriptorService}.
	 * @throws IOException on I/O Errors.
	 */
	public static ZeroDescriptorService createServiceUp ( final EndPoint start, final EndPoint stop, final EndPoint status
			, final EndPoint confget, final EndPoint confstore
			, final EndPoint logget, final EndPoint logview, final EndPoint logclear
			) 
			throws IOException 
	{
		return createServiceUp(null, start, stop, status, confget, confstore, logget, logview, logclear, new HashMap<String, Object>());
	}

	/**
	 * Create a service descriptor.
	 * @param start Life-cycle start-service {@link EndPoint}.
	 * @param stop Life-cycle stop-service {@link EndPoint}.
	 * @param status Life-cycle service status {@link EndPoint}.
	 * @param confget Configuration get {@link EndPoint}.
	 * @param confstore Configuration save {@link EndPoint}.
	 * @param logget Logging get events {@link EndPoint}.
	 * @param logview Logging optional log viewer {@link EndPoint}.
	 * @param logclear Logging log clear {@link EndPoint}.
	 * @param attribs Optional message attributes. See {@link OSMetrics} for supported attributes.
	 * @return a {@link ZeroDescriptorService}.
	 * @throws IOException on I/O Errors.
	 */
	public static ZeroDescriptorService createServiceUp (final EndPoint start, final EndPoint stop, final EndPoint status
			, final EndPoint confget, final EndPoint confstore
			, final EndPoint logget, final EndPoint logview, final EndPoint logclear
			, final Map<String, Object> attribs
			) 
			throws IOException 
	{
		// FindBugs: There is an apparent infinite recursive loop in com.cloud.cluster.zeroconf.ZeroDescriptorServiceUp.create(EndPoint, EndPoint, EndPoint, EndPoint, EndPoint, EndPoint, EndPoint, EndPoint, Map)
		return createServiceUp(null, start, stop, status, confget, confstore, logget, logview, logclear, attribs);
	}
	
	/**
	 * Create a service descriptor.
	 * @param id Unique id of the message.
	 * @param start Life-cycle start-service {@link EndPoint}.
	 * @param stop Life-cycle stop-service {@link EndPoint}.
	 * @param status Life-cycle service status {@link EndPoint}.
	 * @param confget Configuration get {@link EndPoint}.
	 * @param confstore Configuration save {@link EndPoint}.
	 * @param logget Logging get events {@link EndPoint}.
	 * @param logview Logging optional log viewer {@link EndPoint}.
	 * @param logclear Logging log clear {@link EndPoint}.
	 * @param attribs Optional message attributes. See {@link OSMetrics} for supported attributes.
	 * @return a {@link ZeroDescriptorService}.
	 * @throws IOException on I/O Errors.
	 */
	public static ZeroDescriptorService createServiceUp (final String id, final EndPoint start, final EndPoint stop, final EndPoint status
			, final EndPoint confget, final EndPoint confstore
			, final EndPoint logget, final EndPoint logview, final EndPoint logclear
			, final Map<String, Object> attribs
			) 
			throws IOException 
	{
		MessageType type = MessageType.SERVICE_UP;
		ZeroDescriptorService sd = id != null ? new ZeroDescriptorService(id, type, attribs) : new ZeroDescriptorService(type, attribs);
		sd.setLifecycleUrls(start, stop, status);
		sd.setConfigurationUrls(confget, confstore);
		sd.setLoggingUrls(logget, logview, logclear);
		return sd;
	}
	
	public static ZeroDescriptorService createServiceDown () throws IOException {
		return new ZeroDescriptorService(MessageType.SERVICE_DOWN, null);
	}

	public static ZeroDescriptorService createServiceDown (final String uuid, Map<String, Object> attribs) throws IOException {
		return new ZeroDescriptorService(uuid, MessageType.SERVICE_DOWN, attribs);
	}
	
	public static ZeroDescriptorService createServiceDown (final Map<String, Object> attribs) throws IOException {
		if ( attribs == null) 		throw new IOException("ServiceDown: Attributes cannot be null. Use create() instead.");
		return new ZeroDescriptorService(null, MessageType.SERVICE_DOWN, attribs);
	}
	
}