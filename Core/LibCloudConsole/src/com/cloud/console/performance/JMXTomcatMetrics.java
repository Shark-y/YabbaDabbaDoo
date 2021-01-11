package com.cloud.console.performance;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tool to do JMX queries against the Tomcat Catalina object name:
 * 
 * <pre> Set&lt;ObjectInstance> threadPool = query("Catalina:type=ThreadPool,name=\"http*\"");
 * System.out.println(dumpQuery(threadPool));</pre>
 * 
 * This class does extensive use the JMX {@link ManagementFactory}.
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class JMXTomcatMetrics {

	private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	/**
	 * Execute a JMX query.
	 * @param query A JMX query name. For example: "Catalina:type=ThreadPool,name=\"http*\""
	 * @return A set of JMX {@link ObjectInstance}.
	 * @since 1.0.0
	 * @throws MalformedObjectNameException
	 */
	private static Set<ObjectInstance> query(String query) throws MalformedObjectNameException {
		ObjectName name = new ObjectName(query); 
		return server.queryMBeans(name, null);
	}

	/**
	 * Execute a JMX query.
	 * @param query A JMX query name. For example: "Catalina:type=ThreadPool,name=\"http*\""
	 * @param desired An array of desired attributes from the query. For example: {"connectionCount", "currentThreadCount", "currentThreadsBusy", "maxConnections" ,"maxThreads"}
	 * @return A JSON array of the desired attributed and their values:
	 * <pre>[ {
   "maxConnections": 2048,
   "currentThreadsBusy": 0,
   "maxThreads": 2048,
   "connectionCount": 1,
   "instance": "Catalina:type=ThreadPool,name=\"http-bio-8080\"",
   "currentThreadCount": 2
  },
  {
   "maxConnections": 10000,
   "currentThreadsBusy": 0,
   "maxThreads": 150,
   "connectionCount": 1,
   "instance": "Catalina:type=ThreadPool,name="http-nio-8443",
   "currentThreadCount": 0
  }
 ]</pre>
 	 * @since 1.0.0
	 * @throws Exception on any kind of error.
	 */
	private static JSONArray query(String query, String[] desired) throws Exception {
		Set<ObjectInstance> beans 	= query(query);
		JSONArray array 			= new JSONArray();
		
		for ( ObjectInstance instance : beans) {
			MBeanInfo info 					= server.getMBeanInfo(instance.getObjectName());
			MBeanAttributeInfo[] attribs 	= info.getAttributes();

			JSONObject obj = new JSONObject();
			obj.put("instance", instance.getObjectName());
			
			for (MBeanAttributeInfo attrib : attribs) {
				int idx = desired != null ? Arrays.binarySearch(desired, attrib.getName()) : 0;

				// Ignore undesired
				if ( idx < 0 )	
					continue;
				
				// Get the attribute value
				Object value = "";
				try {
					value = server.getAttribute(instance.getObjectName(), attrib.getName());
				} catch (Exception e) {
				}
				obj.put(attrib.getName(), value);
			}
			array.put(obj);
		}
		return array;
	}
	
	/*
	 * Helper to dump a JMX query result as a string.
	 */
	static String dumpQuery (Set<ObjectInstance> beans) throws Exception {
		StringBuffer buf = new StringBuffer();
		
		for ( ObjectInstance instance : beans) {
			MBeanInfo info 					= server.getMBeanInfo(instance.getObjectName());
			MBeanAttributeInfo[] attribs 	= info.getAttributes();
			buf.append(instance.getObjectName() + "\n" );
			
			for (MBeanAttributeInfo attrib : attribs) {
				Object value = "";
				try {
					value = server.getAttribute(instance.getObjectName(), attrib.getName());
				} catch (Exception e) {
				}
				buf.append("\t" + attrib.getName() + "=" + value + "\n");
			}
		}
		return buf.toString();
	}
	
	/**
	 * Get container metrics from the JMX service.<pre>
 { "threadPool": [{
   "maxConnections": 2048,
   "currentThreadsBusy": 0,
   "maxThreads": 2048,
   "connectionCount": 1,
   "instance": "Catalina:type=ThreadPool,name=\"http-bio-8080\"",
   "currentThreadCount": 2
  },
  {
   "maxConnections": 10000,
   "currentThreadsBusy": 0,
   "maxThreads": 150,
   "connectionCount": 1,
   "instance": "Catalina:type=ThreadPool,name="http-nio-8443",
   "currentThreadCount": 0
  }],
 "requestProcessor": [{
   "bytesSent": 0,
   "bytesReceived": 0,
   "processingTime": 0,
   "maxTime": 0,
   "errorCount": 0,
   "requestCount": 0,
   "modelerType": "org.apache.coyote.RequestGroupInfo",
   "instance": "Catalina:type=GlobalRequestProcessor,name=\"http-nio-8443\""
  }],
 "serverInfo": "Apache Tomcat/7.0.53"
} </pre>

	 * @return JSON string for JMX Bean Catalina:type=ThreadPool,name=\"http*\"" or NULL is errors.
	 * @since 1.0.0
	 */
	public static JSONObject getContainerMetrics () {
		try {
			String[] desired 	= new String[] {"connectionCount", "currentThreadCount", "currentThreadsBusy", "maxConnections" ,"maxThreads"};
			JSONArray tp 		= query("Catalina:type=ThreadPool,name=\"http*\"", desired);
			JSONArray grp 		= query("Catalina:type=GlobalRequestProcessor,name=\"http*\"", null);
			JSONArray info 		= query("Catalina:type=Server", new String[] {"serverInfo"});
			
			JSONObject root = new JSONObject();
			root.put("serverInfo", info.getJSONObject(0).getString("serverInfo"));
			root.put("threadPool", tp);
			root.put("requestProcessor", grp);

			return root;
		} catch (Exception e) {
			return null;
		}
	}
}
