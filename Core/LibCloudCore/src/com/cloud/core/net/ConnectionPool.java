package com.cloud.core.net;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.net.IConnection;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;

/**
 * Simple-reusable connection pooling for your webapp.
 * 
 * <ul>
 * <li> Loads connections that implement IConnection via reflection.
 * <li> Tracks clients and grows on demand when clients exceed a configured threshold.
 * </ul>
 * 
 * @author VSilva
 *
 */
public class ConnectionPool {
	
	private static final Logger log = LogManager.getLogger(ConnectionPool.class);
	
	// Initialization Options
	/** min  # of connections to start @ boot time */
	public static final String OPT_MIN_CONNECTIONS 			= "OPT_MIN_CONNECTIONS";
	
	/** Class name that implements IConnnection (loaded via reflection */
	public static final String OPT_CN_CLASSNAME 			= "OPT_CN_CLASSNAME";

	/** connection string */
	public static final String OPT_CN_STRING 				= "OPT_CN_STRING";

	/** ID assigned to the connection by the pool */
	public static final String OPT_CN_ID 					= "OPT_CN_ID";
	
	/** max # of clients/connection before auto growing the pool (add more connections */
	public static final String OPT_MAX_CLIENTS_BEFORE_GROW  = "OPT_MAX_CLIENTS_BEFORE_GROW";
	
	/** default cn poll size */
	public static final String DEFAULT_POOL_SIZE			= "5";

	/** Default # of connections before grow */
	public static final String DEFAULT_MAX_SLOTS			= "10";
	
	// Holds pool of connections
	private static final Map<String, IConnection> connections 	= new ConcurrentHashMap<String, IConnection>();

	// clients for each connection above
	private static final Map<String, List<String>> clients 		= new ConcurrentHashMap<String, List<String>>();

	// Min # of cns in the map above
	private static int minConnections 			= Integer.parseInt(DEFAULT_POOL_SIZE); // 5;
	
	// Auto grow? Add connections on demand?
	private static boolean AUTOGROW 			= true;
	
	//Max # of clients before grow
	private static int maxClientsBeforeGrow 	= Integer.parseInt(DEFAULT_MAX_SLOTS); // 10;
	
	// connection prefix
	private static String cnPrefix				= "CONNECTION";

	// IConnection class to load via reflectionn
	private static String cnClassName			= null;

	// pointer to the current pool connection
	private static int poolIndex;
	
	// connection options
	private static Map<String, String> options					= null;;
	
	private static void LOGD ( final String text) {
		//System.out.println(String.format("[CN-POOL] %s", text));
		log.debug(String.format("[CN-POOL] %s", text));
	}
	
	private ConnectionPool() {
	}
	
	/**
	 * Initialize the connection pool with the options above.
	 * @param options Map of options (see above)
	 * @return The time in ms that akes to start.
	 */
	public static long initialize(Map<String, String> options) throws IOException {
		if ( options.containsKey(OPT_MIN_CONNECTIONS)) {
			minConnections = Integer.parseInt(options.get(OPT_MIN_CONNECTIONS));
		}
		if ( options.containsKey(OPT_MAX_CLIENTS_BEFORE_GROW)) {
			maxClientsBeforeGrow = Integer.parseInt(options.get(OPT_MAX_CLIENTS_BEFORE_GROW));
		}
		if ( ! options.containsKey(OPT_CN_CLASSNAME)) {
			throw new IOException("A class name that implements IConnection is required (OPT_CN_CLASSNAME)");
		}
		cnClassName 			= options.get(OPT_CN_CLASSNAME);
		ConnectionPool.options 	= options;
		return initializePool();
	}

	/**
	 * Connect all elements in the {@link IConnection} tracker map in sequence.
	 * @param options Connection options hash map.
	 * @param waitForConnection If true, wait for every connection to finish.
	 * @return The time (ms) that took to execute the connections,
	 * @throws IOException On I/O, Network errors.
	 */
	public static long connect(final boolean waitForConnection) throws IOException {
		long t0 = System.currentTimeMillis();

		for ( Map.Entry<String, IConnection> entry : connections.entrySet()) {
			IConnection conn =  entry.getValue();
			try {
				conn.initialize(options);
				conn.connect(waitForConnection);
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
		return System.currentTimeMillis() - t0;
	}
	
	/*
	 * Load a ICoonection class via reflection
	 */
	static IConnection loadConnectionInstance () throws IOException {
		IConnection cn = null;
		try {
			final Class<?> obj 	= Class.forName(cnClassName);
			cn 					= (IConnection)obj.newInstance(); 
		} catch (Exception e) {
			throw new IOException(e);
		}
		return cn;
	}
	
	/**
	 * Private initializer.
	 * @return The time in ms that takes to initialize all connections.
	 */
	private static long initializePool () throws IOException {
		long t0 = System.currentTimeMillis();
		
		// Init-load N IConnections via reflection
		for (int i = 0; i < minConnections; i++) {
			initializeConnection(i); //, false);
		}
		return System.currentTimeMillis() - t0;
	}

	/**
	 * Load the ICoonection via reflection & save it in the local Map.
	 * @param index A connection index (position in the Map) used to derive a Map key.
	 */
	private static IConnection initializeConnection (final int index ) throws IOException {
		IConnection cn = loadConnectionInstance ();
	
		// save it in the CN map & init clients HM
		addConnection(index, cn);
		return cn;
	}
	
	private static void internalConnect ( IConnection cn, final boolean waitForConnection) {
		try {
			cn.initialize(options);
			cn.connect(waitForConnection);
		} catch (IOException e) {
			//e.printStackTrace();
		}
		// save it & init clients HM
		//addConnection(index, cn);
	}

	// Save the connection in the local Map. The key is derived from a PREFIX-INDEX.
	private static void addConnection (int index, IConnection cn) {
		final String key = cnPrefix + "-" + index; 
		cn.setId(key);
		connections.put(key, cn);
		clients.put(key, new CopyOnWriteArrayList<String>());
	}
	
	/**
	 * Shutdown the pool. Close connections.
	 * @return The time (ms) that takes to disconnect.
	 */
	public static long disconnect () {
		long t0 = System.currentTimeMillis();
		for ( Map.Entry<String, IConnection> entry : connections.entrySet()) {
			IConnection conn =  entry.getValue();
			try {
				conn.disconnect();
			} catch (IOException e) {
				log.error("disconnect " + entry.getKey(), e);
			}
		}
		return System.currentTimeMillis() - t0;
	}

	/**
	 * Disconnect and remove connections from tracker HM.
	 * @return The time (ms) that takes to shutdown
	 */
	public static long destroy () {
		long t0 = disconnect();
		connections.clear();
		clients.clear();
		return t0;
	}
	
	/**
	 * Restart after some config change.
	 * @param options An options map (see top options)
	 * @return Restarts time (ms).
	 * @throws ConnectionPoolException If there is a connection failure or reflection error.
	 */
	public static long reconnect(/*Map<String, String> options, */ final boolean waitForConnection) throws IOException {
		long t0 = disconnect();
		long t1 = connect(/*options,*/ waitForConnection);
		return t0 + t1;
	}

	private static String getCurrentId () {
		return cnPrefix + "-" + poolIndex;
	}
	
	/**
	 * Get a connection for use.
	 * @param clientId An Id of the client that will use the connection (client IDs are used to grow the pool on demand).
	 * @return An object that implements {@link IConnection}
	 * @throws IOException On connection failures, reflection errors, etc.
	 */
	public static IConnection getConnection (final String clientId) throws IOException {
		if ( AUTOGROW ) {
			String lowestKey = autogrow();
			// point to the lowest idx
			poolIndex = Integer.parseInt(lowestKey.split("-")[1]);
		}
		
		// see if client exists
		final String found 	= findClient(clientId);
		
		// Create and/or Load balance
		final String key 	= found != null ? found : getCurrentId();
		IConnection cn 		= connections.get(key);
		int size 			= connections.size();
		
		// check cn status
		ServiceStatus status = cn.getStatus();
		
		if ( status.getStatus() != /*IConnection.*/Status.ON_LINE) {
			if ( status.getStatus() == /*IConnection.*/Status.CONNECTING) {
				try {
					cn.waitForConnection(3000);
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
			else {
				throw new IOException(key + " " + cn.getStatus());
			}
		}
		if ( found == null ) {
			clients.get(key).add(clientId);
		}
		LOGD("GET Connection " + clientId + " PoolId: " + key + " PoolIdx: " + poolIndex + " Clients: " + clients);
		
		poolIndex++;
		if ( poolIndex % size == 0 ) {
			poolIndex = 0;
		}
		return cn;
	}

	/**
	 * Find a client in the clients HM-List
	 * @param clientId Id of the terminal/client.
	 * @return The ID of the Hash-map if found else null.
	 */
	private static String findClient(final String clientId) {
		for ( Map.Entry<String, List<String>> entry : clients.entrySet()) {
			List<String> list = entry.getValue();
			if ( list.contains(clientId) ) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	/*
	 * Grow the pool on demand by counting the number of clients per connection. If filled up a new IConnection is spwaned.
	 */
	private static synchronized String autogrow() {
		boolean grow 	= false;
		int growCount 	= 0;
		
		Entry<String, List<String>> first = clients.entrySet().iterator().next();
		int lowestSize 		= first.getValue().size();
		String lowestKey 	= first.getKey(); 
		
		// count how many lists's size > MAX CLIENT, also get the lowest list size
		for ( Map.Entry<String, List<String>> entry : clients.entrySet()) {
			List<String> list = entry.getValue();
			//LOGD("Grow clients " + entry.getKey() + "=" + list + " max clients=" + maxClientsBeforeGrow);
			
			if ( list.size() > maxClientsBeforeGrow) {
				growCount++;
			}
			if ( list.size() < lowestSize ) {
				lowestKey 	= entry.getKey();
				lowestSize 	= list.size();
			}
		}
		// if the # of lists's size > MAX CLIENT is >= client's size then grow pool
		if ( growCount >= clients.size()) {
			grow = true;
		}
		//System.out.println("AUTOGROW count=" + growCount + " clients size=" + clients.size() + " grow=" + grow + " Max clientsBeforeGrow=" + maxClientsBeforeGrow);
		if ( grow ) {
			int idx = connections.size();
			//LOGD("********** GROW POOL index:" + idx);
			
			try {
				IConnection cn =  initializeConnection(idx);
				internalConnect(cn, true);
			} catch (Exception e) {
				log.error("Autogrow connection " + idx, e);
			}
			// point to new index
			poolIndex = idx;
		}
		return lowestKey;
	}

	public static String dump() {
		StringBuffer buf = new StringBuffer();
		for ( Map.Entry<String, List<String>> entry : clients.entrySet()) {
			buf.append(entry.getKey() + " = " + entry.getValue() + " ");
		}
		return buf.toString();
	}
	
	/**
	 * Invoke this to release the client count per connection & keep things clean.
	 * @param clientId Id of the client to release.
	 */
	public static void releaseClient( final String clientId) {
		// Loops thru all lists & remove id. Inefficient but should work
		for ( Map.Entry<String, List<String>> entry : clients.entrySet()) {
			List<String> list = entry.getValue();
			list.remove(clientId);
		}
		LOGD("RELEASE " + clientId + " Clients:" + clients);
	}

	/**
	 * Get the status of the pool. If any {@link IConnection} failed to start the status will be STARTED_WITH_ERRORS.
	 * with the failed ID(s) in the description.
	 * @return See {@link ServiceStatus}.
	 */
	public static ServiceStatus getStatus () {
		boolean failed 			= false;
		StringBuilder desc 		= new StringBuilder();
		
		// 6/20/2020
		if ( connections.size() == 0 ) {
			return new ServiceStatus(Status.OFF_LINE, "Offline");
		}
		for ( Map.Entry<String, IConnection> entry : connections.entrySet()) {
			IConnection conn 		= entry.getValue();
			ServiceStatus status 	= conn.getStatus();
			
			if ( status.getStatus() != /*IConnection.*/Status.ON_LINE) {
				failed = true;
				desc.append(conn.getId() + " " + status.getStatus() + " " + status.getDescription() + "<br/>");
			}
		}
		// 9/2/2019 STARTED_WITH_ERRORS not handled by the auto recovery
		return new ServiceStatus(failed ? Status.SERVICE_ERROR /*STARTED_WITH_ERRORS*/ : Status.ON_LINE, desc.toString());
	}
	
	/**
	 * Get an unmodifiable list of pending {@link IConnection}. A connection is pending only if its {@link ServiceStatus} is CONNECTING.
	 * @return List of connections with CONNECTING {@link ServiceStatus}.
	 */
	public static List<IConnection> getPending () {
		List<IConnection> pending = new CopyOnWriteArrayList<IConnection>();
		for ( Map.Entry<String, IConnection> entry : connections.entrySet()) {
			IConnection conn 	= entry.getValue();
			if ( conn.getStatus().getStatus() == Status.CONNECTING) {
				pending.add (conn);
			}
		}
		return Collections.unmodifiableList(pending);
	}

	/**
	 * Wait for all pending connections. If there are no pending it does nothing.
	 * Note: The {@link IConnection} is responsible for notifying all waiting threads when its status becomes ON_LINE.
	 * @param delay time in ms to wait for each connection.
	 */
	public static void waitForPendingConnections (final long delay) {
		List<IConnection> pending = getPending();
		
		if ( pending.size() > 0 ) {
			log.debug("waitForPendingConnections() Waiting on " + pending.size() + " pending connections");
		}
		for (IConnection cn : pending) {
			try {
				cn.waitForConnection(delay);
			} catch (InterruptedException e) {
				log.error("waitForPendingConnections()", e);
				break;
			}
		}
	}
	
	/**
	 * @return [{"clients":"[45100]","id":"CONNECTION-4"},{"clients":"[]","id":"CONNECTION-3"},...]
	 * @throws JSONException On JSON errors.
	 */
	public static JSONArray toJSON () throws JSONException {
		JSONArray root = new JSONArray();
		
		for ( Map.Entry<String, List<String>> entry : clients.entrySet()) {
			final String id		= entry.getKey();
			List<String> terms	= entry.getValue();
			
			IConnection cn		= connections.get(id); 	// can't be null!
			JSONObject jcn 		= cn.toJSON(); 			// allows the cn to provide metrics
			
			// rogue connection? Use the basic.
			if ( jcn == null ) {
				jcn = new JSONObject();
				jcn.put("id", id);
				jcn.put("status", cn.getStatus().toString());
			}
			// inject terminals 
			jcn.put("clients", terms.toString());	// add clients
			
			root.put(jcn);
		}
		return root;
	}
	
	/**
	 * Is the CN pool initialized?
	 * @return True if the # of connections > 0.
	 */
	public static boolean initialized () {
		return connections != null && (connections.size() > 0);
	}
}
