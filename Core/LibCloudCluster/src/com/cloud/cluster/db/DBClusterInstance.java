package com.cloud.cluster.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.cloud.core.db.Database;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.OSMetrics;

/**
 * Implementation of {@link IClusterInstance} using a persistent StatStore (MySQL)
 * <pre>
 * CloudCluster cluster = CloudCluster.getInstance();
 * 
 * attributes.put(Database.KEY_DRIVER, "com.mysql.jdbc.Driver");
 * attributes.put(Database.KEY_URL, "jdbc:mysql://localhost:3306/cluster?useSSL=true&verifyServerCertificate=false");
 * attributes.put(Database.KEY_USER, "cloud");
 * attributes.put(Database.KEY_PWD, "Thenewcti1");
 * 
 * cluster.initialize(attributes);
 * </pre>
 * @author VSilva
 *
 */
public class DBClusterInstance implements IClusterInstance {

	static final Logger log 		= LogManager.getLogger(DBClusterInstance.class);
	static final String IP_ADDRESS	= IOTools.getHostIp();
	
	private Database db 			= Database.getInstance();
	
	final Object nodeId;
	final Object nodeName;
	final Object nodeGroup;
	
	// used to track Topics & other for dispatch/cleanup.
	private final List<Object> tracked = new ArrayList<Object>();
	
	/**
	 * Construct with connection arguments:
	 * <pre>
	 * attributes.put(Database.KEY_DRIVER, "com.mysql.jdbc.Driver");
	 * attributes.put(Database.KEY_URL, "jdbc:mysql://localhost:3306/cluster?useSSL=true&verifyServerCertificate=false");
	 * attributes.put(Database.KEY_USER, "cloud");
	 * attributes.put(Database.KEY_PWD, "Thenewcti1");
	 * </pre>
	 * @param attributes DataStore attributes.
	 * 
	 * @throws IOException
	 */
	public DBClusterInstance(Map<String, Object> attributes) throws IOException {
		Object driver 	= attributes.get(Database.KEY_DRIVER);
		Object url 		= attributes.get(Database.KEY_URL);
		Object user 	= attributes.get(Database.KEY_USER);
		Object pwd 		= attributes.get(Database.KEY_PWD);
		
		nodeName		= attributes.get(CloudCluster.KEY_CTX_PATH);
		nodeGroup		= attributes.get(CloudCluster.KEY_NODE_GRP);
		nodeId			= nodeName + "@" + IP_ADDRESS;
	
		if ( nodeName == null) 	throw new IOException("Node name is required.");
		if ( nodeGroup == null) throw new IOException("Node group is required.");
		
		if ( driver == null ) 	throw new IOException("DataStore-Cluster Initialize: Missing Database driver.");
		if ( url == null ) 		throw new IOException("DataStore-Cluster Initialize: Missing Database url.");
		if ( user == null ) 	throw new IOException("DataStore-Cluster Initialize: Missing Database user.");
		if ( pwd == null ) 		throw new IOException("DataStore-Cluster Initialize: Missing Database password.");

		db.initialize(driver.toString());
		db.connect(url.toString(), user.toString(), pwd.toString());
		
		try {
			registerNode(attributes);
		} catch (Exception e) {
			throw new IOException("Register Node failure.", e);
		}
	}
	
	/*
	 * Add a record to the NODES table.
	 */
	private void registerNode(Map<String, Object> attributes) throws SQLException, JSONException, IOException {
		Object url 			= attributes.get(CloudCluster.KEY_CTX_URL);
		if ( url == null) 	throw new IOException("Register: node URL is required.");
		
		final int status 	= 0;
		JSONObject params	= new JSONObject(attributes);
		
		// remove DB stuff
		//params.remove(Database.KEY_USER);
		params.remove(Database.KEY_PWD);

		// add status
		params.put(CloudCluster.KEY_STATUS, status);
		params.put(CloudCluster.KEY_STMSG, "");
		
		// Put OS Metrics inside attributes
		JSONObject metrics 	= OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);
		@SuppressWarnings("unchecked")
		Set<String> keys 	= metrics.keySet();
		
		for (String key : keys) {
			params.put(key, metrics.get(key));
		}
		
		// IP (0), Name(1), Grp(2)  Url(3), Attribs (4) status(5), desc(6) metrics (7)
//		final String SQL 	= String.format("INSERT INTO NODES VALUES ('%s' , '%s' , '%s' , '%s' , '%s' , %d , '%s' , '%s')"
//				, IP_ADDRESS, nodeName ,nodeGroup, url, params.toString(), status, "", metrics.toString());
		boolean found 	= db.exists("NODES", getDefaultSQLCondition());
		
		final String SQL 	= found 
				? String.format("UPDATE NODES SET nodeGroup = '%s' , url = '%s' , attributes = '%s'", nodeGroup, url, params.toString()) 
				: String.format("INSERT INTO NODES VALUES ('%s' , '%s' , '%s' , '%s' , '%s' , '%s' )"
						, nodeId, nodeName , nodeGroup, IP_ADDRESS, url, params.toString());

		int rows = db.update(SQL);

		if ( rows == 0) {
			throw new IOException("Register Node: DB Update failure with SQL: " + SQL);
		}
		leaderSelect();
	}
	
	private void unregisterNode() throws SQLException, IOException {
		final String SQL = "DELETE FROM NODES WHERE " + getDefaultSQLCondition(); 
		int rows = db.update(SQL);
		if ( rows == 0) {
			throw new IOException("Unregister Node: DB Update failure with SQL: " + SQL);
		}
	}

	/**
	 * Method used to perform maintenance & garbage collection tasks.
	 */
	public void collectGarbage() {
		for (Object obj : tracked) {
			// dispatch topic messages
			if ( obj instanceof DBTopic) {
				((DBTopic)obj).dispatch();
			}
		}
	}
	
	/**
	 * @return id = 'nodeID' AND name = '" + nodeName + "' AND nodeGroup = '" + nodeGroup + "'";
	 */
	private String getDefaultSQLCondition () {
		return String.format("id = '%s' AND name = '%s' AND nodeGroup = '%s'", nodeId, nodeName, nodeGroup);
	}
	
	@Override
	public IClusterLock getLock(String name) {
		throw new RuntimeException("Locks not supported by this provider.");
	}

	@Override
	public Object getAtomicLong(String name) {
		throw new RuntimeException("AtomicLong is not supported by this provider.");	
	}

	@Override
	public IClusterTopic getTopic(String name, IClusterTopicListener listener) {
		DBTopic topic = new DBTopic(nodeId.toString(), name, listener);
		tracked.add(topic);
		return topic;
	}

	private void copyAttributesToMap (JSONObject root, Map<String, Object> attribs) throws JSONException {
		String[] names 		= JSONObject.getNames(root);
		for (String name : names) {
			attribs.put(name, root.get(name));
		}
	}

	private Map<String, Object> attributesToMap (JSONObject root) throws JSONException {
		Map<String, Object> attribs = new HashMap<String, Object>();
		String[] names 		= JSONObject.getNames(root);
		for (String name : names) {
			attribs.put(name, root.get(name));
		}
		return attribs;
	}

	private Map<String, Object> attributesToMap (String json) throws JSONException {
		return attributesToMap(new JSONObject(json));
	}
	
	/**
	 * Get the local node attributes {@link Map}.
	 */
	@Override
	public Map<String, Object> getMemberAttributes() {
		Map<String, Object> attribs = new HashMap<String, Object>();
		final String SQL 			= "SELECT attributes FROM NODES WHERE " + getDefaultSQLCondition(); 
		try {
			JDBCResultSet rs 		= db.query(SQL);
			JSONArray rows 			= rs.getResultSet();

			if ( rows.length() == 0) {
				// no data?
				log.debug("GetMember Attributes: No record found in db.");
				return attribs;
			}
			JSONObject root 	= new JSONObject(rows.getJSONArray(0).getString(0));

			copyAttributesToMap(root, attribs);
		} catch (Exception e) {
			log.error("Get local member attributes (" + nodeName + ")", e);
		}
		return attribs;
	}

	@Override
	public List<ClusterMember> getMembers() {
		return getMembersForSQL("SELECT * FROM NODES");
	}

	/**
	 * Get a list of online members: Formally SELECT * FROM NODES WHERE status > 200 AND status < 300
	 * @return
	 */
	private List<ClusterMember> getOnlineMembers() {
		List<ClusterMember> all 	= getMembers();
		List<ClusterMember> online 	= new ArrayList<ClusterMember>();
		
		for (ClusterMember member : all) {
			Map<String, Object> attribs =  member.getAttributes();
			if ( attribs.containsKey(CloudCluster.KEY_STATUS)) {
				int status = Integer.parseInt(attribs.get(CloudCluster.KEY_STATUS).toString());
				
				if ( status > 200 & status < 300) {
					online.add(member);
				}
			}
		}
		return online;
	}
	
	private List<ClusterMember> getMembersForSQL(String SQL) {
		List<ClusterMember> members = new ArrayList<ClusterMember>();
		try {
			JDBCResultSet rs 	= db.query(SQL);
			JSONArray rows 		= rs.getResultSet();

			if ( rows.length() == 0) {
				// no data?
				log.debug("GetMembers: No record found in db.");
				return members;
			}
			for (int i = 0; i < rows.length(); i++) {
				JSONArray row = rows.getJSONArray(i);
				String id 			= row.getString(0);
				String name			= row.getString(1);
				//String group		= row.getString(2);
				String ip			= row.getString(3);
				//String url			= row.getString(4);
				String attributes	= row.getString(5);
				boolean isLocal		= name != null && name.equals(nodeName.toString());
				
				members.add(new ClusterMember(id, isLocal, ip, attributesToMap(attributes)));
			}
		} catch (Exception e) {
			log.error("Get cluster members (" + nodeName + ")", e);
		}
		return members;
	}

	/**
	 * Get members as a {@link JSONObject}.
	 * @return members as a {@link JSONObject}. Format: [{'uuid': 'id', 'islocal': true, 'address': 'IP', 'attributes': {'k1':'v1','k2':'v2',...}},...]
	 * @throws JSONException On parse errors.
	 */
	@Override
	public JSONArray getMembersAsJSON() throws JSONException {
		JSONArray root 				= new JSONArray();
		List<ClusterMember> members = getMembers();
		
		for (ClusterMember clusterMember : members) {
			root.put(clusterMember.toJSON());
		}
		return root;
	}

	@Override
	public void setLocalMemberStatus(int code, String message) {
		Map<String, Object> attribs = getMemberAttributes();
		// Add status to attributes
		attribs.put(CloudCluster.KEY_STATUS, code);
		attribs.put(CloudCluster.KEY_STMSG, message);
		storeAttributes("Set local member status", attribs);
	}
	
	private void storeAttributes(String label , Map<String, Object> attribs) {
		JSONObject params	= new JSONObject(attribs);

		//String SQL = String.format("UPDATE NODES SET status = %d , description = '%s' %s", code, message, getDefaultSQLCondition());
		String SQL = String.format("UPDATE NODES SET attributes = '%s' WHERE %s", params.toString() , getDefaultSQLCondition());
		try {
			int rows = db.update(SQL);
			if ( rows == 0) {
				throw new IOException("Set attributes: DB Update failure with SQL: " + SQL);
			}
		} catch (Exception e) {
			log.error(label + " (" + nodeName + ")", e);		
		} 
	}

	@Override
	public void setLocalMemberAttribute(String key, String value) {
		Map<String, Object> attribs = getMemberAttributes();
		attribs.put(key, value);
		storeAttributes("Set local member attribute", attribs);
	}

	@Override
	public String getLeader() {
		String SQL = String.format("SELECT * FROM LEADERS WHERE nodeGroup = '%s'", nodeGroup);
		try {
			JDBCResultSet rs 	= db.query(SQL);
			JSONArray rows		= rs.getResultSet();
			int len 			= rows.length();
			if (  len == 0 ) {
				return null;
			}
			if ( len > 1) {
				throw new IOException("Leader select: invalid response size (" + len + ") for SQL " + SQL);
			}
			// Node IP
			return rows.getJSONArray(0).getString(0);
		} catch (Exception e) {
			log.error("Get Leader (" + nodeName + ")", e);		
		}
		return null;
	}

	/**
	 * Leader?
	 * @return True if the leader (ip) == local node ip.
	 */
	@Override
	public boolean isClusterLeader() {
		String leader = getLeader();
		return leader != null && leader.equalsIgnoreCase(nodeId.toString());
	}

	@Override
	public void relinquishLeadership() {
		if ( ! isClusterLeader()) {
			log.warn("RelinquishLeadership: Member " + nodeName + "/" + nodeGroup + " is NOT a leader. Only a leader can relinquish.");
			return;
		}
		// don't relinquish if no online nodes or only 1 is online (note: online is NOT NULL)
		List<ClusterMember> online = getOnlineMembers();
		
		if ( online.size() < 2 ) {
			log.warn("RelinquishLeadership: Member " + nodeName + "/" + nodeGroup + " NOT enough ONLINE members (" + online.size() + ") to relinquish.");
			return;
		}
		// drop & select a new one
		leaderDrop(true);
	}
	
	/**
	 * Drop leadership.
	 * @param selectNew If true select a new one.
	 */
	private void leaderDrop (boolean selectNew) {
		String SQL = String.format("DELETE FROM LEADERS WHERE %s", getDefaultSQLCondition());
		try {
			db.update(SQL);
			if ( selectNew) {
				leaderSelect();
			}
		} catch (Exception e) {
			log.error("LeaderDrop (" + nodeName + ")", e);		
		}
	}

	private void leaderSelect () {
		// get 1st online member
		ClusterMember member = getFirstOnlineMember();
		
		if ( member == null) {
			// try again: select the 1st in line.
			List<ClusterMember> list = getMembers();
			member = list.size() > 0  ? list.get(0) : null;
		}
		if ( member != null) {
			// set
			Map<String, Object> attribs = member.getAttributes();
			Object name = attribs.get(CloudCluster.KEY_CTX_PATH);
			Object grp 	= attribs.get(CloudCluster.KEY_NODE_GRP);
			
			String SQL = String.format("INSERT INTO LEADERS VALUES ('%s' , '%s' , '%s')", member.getUuid(), name.toString(), grp.toString());
			try {
				int rows = db.update(SQL);
				if ( rows == 0){
					throw new IOException("Update failure for SQL " + SQL);
				}
			} catch (Exception e) {
				log.error("Leader Select (" + nodeName + ")", e);
			}
		}
	}
	
	/**
	 * Get the first randomly selected online member.
	 * @param members Cluster members.
	 * @return Randomly selected ONLINE {@link ClusterMember} or NULL if not found.
	 */
	private ClusterMember getFirstOnlineMember () {
		List<ClusterMember> online	= getOnlineMembers();

		//dumpMembers("ONLINE", online);

		// return a random online member
		ClusterMember m = !online.isEmpty() ? online.get((int)(Math.random() * online.size())) : null;
		return m;
	}

	@Override
	public void shutdown() {
		try {
			unregisterNode();
			leaderDrop(false);
			
			for ( Object obj : tracked) {
				// cleanup topics
				if ( obj instanceof DBTopic) {
					((DBTopic)obj).destroy();
				}
			}
		} catch (Exception e) {
			log.error("Cluster shutdown", e);
		}
		db.disconnect();
	}

	@Override
	public String getClusterGroupName() {
		return nodeGroup.toString();
	}

	/**
	 * Get a list of Tcp IP configuration members used for manual join (when multi-cast is disabled).
	 * @return A list of member IP addresses or host names.
	 */
	@Override
	public List<String> getClusterTcpMembers() {
		throw new RuntimeException("This method is invalid for this provider.");	
	}

	@Override
	public boolean isShutdown() {
		return db.isClosed();
	}

	@Override
	public void dumpAllInstances(String label) {
		throw new RuntimeException("This method is invalid for this provider.");		
	}

	/**
	 * Set the local member OS metrics. This can be invoked many times by the member to update its real time {@link OSMetrics}:
	 * <ol>
	 * <li>OS Name: Operating system name.
	 * <li>Number of CPUS.
	 * <li>Peak threads.
	 * <li>CPU load.
	 * <li>Free memory.
	 * </ul>
	 */
	@Override
	public void setLocalMemberOSMetrics() {
		try {
			JSONObject metrics 			= OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);
			Map<String, Object> attribs = getMemberAttributes();
			@SuppressWarnings("unchecked")
			Set<String> keys 			= metrics.keySet();
			
			for (String key : keys) {
				attribs.put(key, metrics.get(key));
			}
			storeAttributes("setLocalMemberOSMetrics", attribs);
		} catch (Exception e) {
			log.error("Set local member metrics (" + nodeName + ")", e);		
		}
	}

	@Override
	public <K,V> IMap<K,V> getMap(String name) {
		return new DBMap<K,V>(name, nodeGroup.toString());
	}

	@Override
	public <V>List<V> getList(String name) {
		return new DBList<V>(name, nodeGroup.toString());
	}

	@Override
	public ClusterMember getLocalMember() {
		List<ClusterMember> members = getMembers();
		ClusterMember local 		= null;
		for (ClusterMember m : members) {
			if ( m.isLocal()) {
				local = m;
			}
		}
		return local;
	}

	@Override
	public JSONObject describe() {
		throw new RuntimeException("This method has not been implemented for this provider.");	
	}

	/* 12/31/2018 Removed
	public com.hazelcast.core.HazelcastInstance getClusterProvider() {
		throw new RuntimeException("This method is invalid for this provider.");
	} */

}
