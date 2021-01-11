package com.cloud.cluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.db.DBClusterInstance;
import com.cloud.core.profiler.OSMetrics;

/**
 * Main cluster provider interface. Allows for multiple vendor implementations. There are 2 current providers:
 * <ul>
 * <li>Hazelcast - see www.hazelcast.com
 * <li>Persistent DataStore using MySQL. See {@link DBClusterInstance}.
 * </ul>
 * @author VSilva
 * @version 2.0 - 4/18/2017
 *
 */
public interface IClusterInstance {

	/**
	 * Get a distributed lock. It can be used to lock critical sections across the cluster.
	 * @param name Lock name.
	 * @return See {@link DistributedLock}.
	 * @since 1.0
	 */
	public IClusterLock getLock (String name);

	/**
	 * Get a distributed Map. Identical to the standard {@link Map}.
	 * @param name Map name.
	 * @return See {@link IClusterMap} or {@link Map}.
	 * @since 2.0
	 */
	public <K, V> IMap<K, V> getMap (String name);

	/**
	 * Get a distributed {@link List}. Identical to the standard {@link List}.
	 * @param name List name.
	 * @return See {@link List}.
	 * @since 2.0
	 */
	public <V> List<V> getList (String name);
	
	/**
	 * Get a distributed {@link AtomicLong}.
	 * @param name Object name.
	 * @return See {@link DistributedAtomicLong}.
	 * @since 1.0
	 */
	public Object /*DistributedAtomicLong*/ getAtomicLong (String name);

	/**
	 * Topics are used to send messages to all cluster members.
	 * @param name Topic name.
	 * @param listener Message receiver. see {@link IClusterTopicListener}
	 * @return For Hazelcast a {@link DistributedTopic}.
	 */
	public IClusterTopic /*DistributedTopic*/ getTopic ( String name, IClusterTopicListener listener);
	
	/**
	 * Get this cluster member attributes.
	 * @return Memeber attributes {@link Map}.
	 */
	public Map<String, Object> getMemberAttributes();

	/**
	 * Get the cluster members.
	 * @return {@link List} of {@link ClusterMember}.
	 */
	public List<ClusterMember> getMembers();
	
	/**
	 * Get members as a {@link JSONObject}. The format depends on the cluster provider.
	 * @return members as a {@link JSONObject}. Hazelcast Format: [{'uuid': 'id', 'islocal': true, 'address': 'IP', 'attributes': {'k1':'v1','k2':'v2',...}},...]
	 * @throws JSONException On parse errors.
	 */
	public JSONArray getMembersAsJSON() throws JSONException;
	
	/**
	 * Set the status of the local member: Online, Offline, Server error, etc..
	 * @param code HTTP status code: 200 = online, 503 = Service down, 401 = Auth failed, 500 = Server error. 
	 * @param message Status message.
	 */
	public void setLocalMemberStatus (int code , String message );
	
	/**
	 * Set a local member attribute.
	 * @param key Attribute key.
	 * @param value Attribute value.
	 */
	public void setLocalMemberAttribute(String key, String value);
	
	/**
	 * Get the opaque cluster provider (the inner object that implements the actual cluster API).
	 * via the {@link IClusterInstance} interface only. This method is provided so clients can
	 * access cluster objects unimplemented by {@link IClusterInstance}.
	 * @deprecated Hazelcast will be removed soon to the lack of security: SSL, basic authentication.
	 * @return For Hazelcast: {@link HazelcastInstance}.
	 */
	/* 12/31/2018 Removed Direct access to the Hazel cast cluster
	public com.hazelcast.core.HazelcastInstance getClusterProvider(); */

	/**
	 * <p>Get or select a "leader" node from the cluster member list.
	 * A leader is a node that can be used to send unique events to an outside entity
	 * when such functionality is required. For example when you have duplicate
	 * events are received by all nodes, but only one node should push them somewhere.</p>
	 * 
	 * <b>Note:</b> When a leaders stops/crashes a new leader is automatically selected
	 * using membership listeners.
	 * 
	 * @return An {@link IAtomicReference} representing a cluster node leader.
	 */
	public /*IAtomicReference<String> */ String getLeader () ;
	
	/**
	 * Is the local node the leader? A node will invoke this to decide if a duplicate event
	 * should be consumed.
	 * <p>A leader is a node that can be used to send unique events to an outside entity
	 * when such functionality is required. For example when you have duplicate
	 * events are received by all nodes, but only one node should push them somewhere.</p>
	 * 
	 * <b>Note:</b> When a leaders stops/crashes a new leader is automatically selected
	 * using membership listeners.

	 * @return true if the local node has been selected a s the "leader"
	 */
	public boolean isClusterLeader ();
	
	/**
	 * Relinquish leadership & have the server choose a new random leader.
	 * Note:
	 * <li> Only online nodes can take over leadership.
	 * <li> Only the leader can relinquish.
	 */
	public void relinquishLeadership ();
	
	/**
	 * Shut down the cluster. Invoke this when the application stops.
	 */
	public void shutdown();

	/**
	 * Get the group name this node belongs to.
	 * @return The cluster group (used for multi-cluster) support.
	 */
	public String getClusterGroupName();
	
	/**
	 * Get a list of the HZ Tcp IP configuration members used for manual join (when multi-cast is disabled).
	 * @return A list of member IP addresses.
	 */
	public List<String> getClusterTcpMembers ();
	
	/**
	 * Is the cluster instance down?
	 * @return True if shut down.
	 */
	public boolean isShutdown () ;
	
	/**
	 * A debug sub to display instance information into the local logging system.
	 * @param label A display label.
	 */
	public void dumpAllInstances(String label);
	
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
	public void setLocalMemberOSMetrics ();
	
	/**
	 * Get the local member node.
	 * @return Local {@link ClusterMember}.
	 */
	public ClusterMember getLocalMember ();
	
	/**
	 * Method used to perform maintenance & garbage collection tasks.
	 */
	public void collectGarbage();
	
	/**
	 * Get a JSON description of all distributed collections in Data tables format.
	 * @return { "data": [[ROW1],[ROW2],...]}
	 */
	public JSONObject describe () throws Exception;
}
