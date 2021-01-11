package com.cloud.cluster.hazelcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.ClusterMember;
import com.cloud.cluster.IClusterInstance;
import com.cloud.cluster.IClusterTopic;
import com.cloud.cluster.IClusterTopicListener;
import com.cloud.cluster.IMap;
import com.cloud.cluster.hazelcast.DistributedAtomicLong;
import com.cloud.cluster.hazelcast.DistributedList;
import com.cloud.cluster.hazelcast.DistributedLock;
import com.cloud.cluster.hazelcast.DistributedMap;
import com.cloud.cluster.hazelcast.DistributedTopic;
import com.cloud.cluster.hazelcast.HzClusterInstance;
import com.cloud.core.io.ObjectCache.ObjectType;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.OSMetrics;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

/**
 * Cluster Instance implementation. Use this class to access cluster objects
 * provided by the opaque cluster implementation (Hazelcast).
 * 
 * @author VSilva
 *
 */
public class HzClusterInstance implements IClusterInstance {

	static final Logger log = LogManager.getLogger(HzClusterInstance.class);
	
	private HazelcastInstance 	hazelcastInstance;
	
	/**
	 * Initialize the cluster.
	 * 
	 * @param attributes Map of key, value pair attributes such as:
	 * <ul>
	 * <li> The context root of the node (KEY_CTX_PATH)
	 * <li> The node URL (KEY_CTX_URL).
	 * </ul>
	 * <p>For example:  {KEY_CTX_PATH=/CloudContactCenterNode001, KEY_CTX_URL=http://192.168.41.3:8080/CloudContactCenterNode001/index.jsp}
	 */
	public /*static*/ void initialize(Map<String, Object> attributes) {
		if ( hazelcastInstance != null ) {
			log.warn("Cluster Init: ALREADY initialized!");
			return;
		}
		Config config 					= new Config();
		MemberAttributeConfig memCfg 	= new MemberAttributeConfig();
		
		memCfg.setAttributes(attributes);
		
		// inject OS metrics to the member attributes
		OSMetrics.injectOSMetrics(memCfg.getAttributes());
		
		setDefaultStartupProperties(config);
		
		config.setMemberAttributeConfig(memCfg);

		// multi-cluster: check for KEY_NODE_GRP
		if ( attributes.containsKey(CloudCluster.KEY_NODE_GRP) ) {
			log.warn("Cluster Init: Using cluster group " + attributes.get(CloudCluster.KEY_NODE_GRP));
			config.getGroupConfig().setName(attributes.get(CloudCluster.KEY_NODE_GRP).toString());
		}
		
		// set the instance name to the context path
		if ( attributes.containsKey(CloudCluster.KEY_CTX_PATH) ) {
			log.debug("Cluster Init: Using instance name " + attributes.get(CloudCluster.KEY_CTX_PATH));
			config.setInstanceName(attributes.get(CloudCluster.KEY_CTX_PATH).toString());
		}
		else {
			/* This is a BUG. If there is no instance name HZ lib init will fail */
			log.fatal("Cluster Init: An instance name is required for this node. Initialization will fail!");
		}
		
		// Optional: Add tcp members (disable multi cast)
		if ( attributes.containsKey(CloudCluster.KEY_NODE_MEMBERS) ) {
			String members = attributes.get(CloudCluster.KEY_NODE_MEMBERS).toString();
			if ( !members.isEmpty()) {
				setTcpConfiguration(config, members);
			}
		}
		
		// dump startup info...
		log.info("--------------------- CLUSTER INFO -------------------------");
		log.info("Cluster Instance Name     : " + config.getInstanceName());
		log.info("Cluster Group Name        : " + config.getGroupConfig().getName());
		log.info("Cluster Start Attributes  : " + attributes);
		dumpClusterConfig(config);
		log.info("------------------------------------------------------------");
		
		hazelcastInstance 	= Hazelcast.getOrCreateHazelcastInstance(config); // newHazelcastInstance(config);
//		instance			= getClusterInstance();
		
		//configureLoggingService();
	}

	/**
	 * Set the TCP config member list. This will
	 * <li> Disable multi-cast (automated discovery).
	 * <li> Enable TCP config (manual) node discovery.
	 * <li> Add a manual list of cluster memebers
	 * @param config HZ {@link Config}
	 * @param list Comma separated list of IP addresses.
	 */
	private void setTcpConfiguration (Config config, String list) {
		String[] members = list.split(",");
		
		log.debug("Cluster Init: Disabling multicast & using manual TCP discovery w/ memebers: " + list);
		
		// disable multicast
		JoinConfig jc = config.getNetworkConfig().getJoin();
		jc.getMulticastConfig().setEnabled(false);
		
		// enable tcp/ip discovery
		jc.getTcpIpConfig().setEnabled(true); //.setMembers(members);
		
		for (String member : members) {
			jc.getTcpIpConfig().addMember(member);
		}
	}

	/**
	 * Set default cluster properties
	 * @see hazelcast-3.3.2/docs/manual/html/configurationproperties.html#advanced-configuration-properties
	 * @param config
	 */
	private void setDefaultStartupProperties(Config config) {
		// Number of input and output threads.
		config.setProperty("hazelcast.io.thread.count", "2"); 
		
		// Number of partition based operation handler threads. -1 means CPU core count x 2.
		config.setProperty("hazelcast.operation.thread.count", "3"); 
		
		// Number of generic operation handler threads. -1 means CPU core count x 2.
		config.setProperty("hazelcast.operation.generic.thread.count", "3"); 
		
		// Number of event handler threads.
		config.setProperty("hazelcast.event.thread.count", "2"); 
		
		// Use log4j
		config.setProperty( "hazelcast.logging.type", "log4j"); //, none
	}
	
	private void dumpClusterConfig(Config config) {
		log.info("Thread Count I/O       : " + config.getProperty("hazelcast.io.thread.count"));
		log.info("Thread Count Operation : " + config.getProperty("hazelcast.operation.thread.count"));
		log.info("Thread Count Generic   : " + config.getProperty("hazelcast.operation.generic.thread.count"));
		log.info("Thread Count Events    : " + config.getProperty("hazelcast.event.thread.count"));
		log.info("Log Type               : " + config.getProperty("hazelcast.logging.type"));
	}

	/**
	 * Constructor
	 * @param hazelcastInstance Hazelcast object. See {@link HazelcastInstance}.
	 * @param paranet The parent {@link CloudCluster}.
	 */
	public HzClusterInstance(Map<String, Object> attributes) { 
		initialize(attributes);
		
		// Add a membership listener. Used to select a leader + other stuff.
		hazelcastInstance.getCluster().addMembershipListener(new MembershipListener() {
			
			public void memberAdded(MembershipEvent ev) {
				IAtomicReference<String> leaderId 	= leaderGet(hazelcastInstance);
				
				log.debug("MemberAdded " + ev.getMember().getUuid() + " Leader: " + leaderId.get());
				//leaderCheckStatus(ev.getCluster().getMembers(), ev.getMember());
			}

			public void memberRemoved(MembershipEvent ev) {
				IAtomicReference<String> leader = leaderGet(hazelcastInstance);
				//Member local 					= hazelcastInstance.getCluster().getLocalMember();
				Member remote					= ev.getMember();

				log.debug("MemberRemoved " + remote.getUuid() + " Leader:" + leader.get());
				
				if ( leader.get() != null && remote.getUuid().equals(leader.get())) {
					// Leader has left
					log.debug("MemberRemoved Leader " + leader.get() + " has left. Attemting refresh.");
					leaderRefreshId(hazelcastInstance);
					//leaderCheckStatus(ev.getCluster().getMembers(), ev.getMember());
				}
			}

			/**
			 * Fires for each member attribute change.
			 */
			@Override
			public void memberAttributeChanged(MemberAttributeEvent ev) {
				Member local 	= hazelcastInstance.getCluster().getLocalMember();
				Member remote	= ev.getMember();
				
				// Check the leaders status when the statusCode attribute 
				// is received: statusCode: HTTP-CODE
				if ( ev.getKey().equals(CloudCluster.KEY_STATUS)) {
					int status = Integer.parseInt/* Findbugs valueOf*/(ev.getValue().toString());
					
					if ( isClusterManager(local)) {
						log.warn("Member Status Change: Ignoring MANAGER @ " + local.getUuid());
						return;
					}
					// 500 = Internal server error, 503 = Services offline
					//if ( status > 500) {
						log.debug("Member Status Change: Remote Id: " 
								+ remote.getUuid() + " Status: " + status 
								+ " Msg: " + remote.getStringAttribute(CloudCluster.KEY_STMSG)
								);
						
						leaderCheckStatus(ev.getCluster().getMembers(), remote );
					//}
				}
			}
		});

		IAtomicReference<String> leader = leaderSelect(hazelcastInstance);
		log.debug("Automatically selected leader: " + leader.get());
		
		dumpMembers("HzClusterInstance Init", hazelcastInstance.getCluster().getMembers());
	}
	
	private boolean isClusterManager (Member member) {
		String ctxPath	= member.getStringAttribute(CloudCluster.KEY_CTX_PATH);
		return ctxPath != null && ctxPath.contains("Manager");
	}
	
	/*
	 * LEADER STUFF. THIS IS UDED TO DEAL W/ DUPLICATE EVENTS.
	 * BY CHOOSING A LEADER, EACH NODE CAN DECIDE IF DUPLICATE EVENTS SHOULD BE CONSUMED.
	 */

	/**
	 * Check the status of the leader. Invoke when a member changes its running status.
	 * @param members Cluster member set.
	 * @param local Member invoking the request.
	 */
	private void leaderCheckStatus(Set<Member> members, Member local) {
		IAtomicReference<String> leaderId 	= leaderGet(hazelcastInstance);
		Member leader						= null;
		
		// ignore cluster managers
		if ( isClusterManager(local)) {
			log.warn("MemberStatus Ignoring ClusterManager " + local.getUuid() 
					+ " CtxPath: " + local.getStringAttribute(CloudCluster.KEY_CTX_PATH) );
			return;
		}
		
		dumpMembers("Leader check by " + local.getUuid() + " Leader Id: " + leaderId.get(), members);
		
		// check the status of the leader... if offline then take over.
		for (Member member : members) {
			if ( leaderId.get() != null && member.getUuid().equals(leaderId.get())) {
				leader = member;
				break;
			}
		}
		
		// local becomes the leader if the previous leader is offline
		if ( leader != null ) {
			Map<String, Object> attribs = leader.getAttributes();
			
			log.debug("MemberStatus FOUND leader " + leader.getUuid() + " among members. Attribs: " + attribs);
			
			if ( attribs.containsKey(CloudCluster.KEY_STATUS)) {
				int status = leader.getIntAttribute(CloudCluster.KEY_STATUS);
				
				if ( status >= 500) {
					log.debug("MemberStatus LEADER OFFLINE (" + status + "). Member " + local.getUuid() + " will attempt refresh.");
					
					leaderRefreshId(hazelcastInstance);  
				}
			}
			else {
				log.warn("MemberStatus Leader " + leader.getUuid() + " is missing a STATUS attribute!");
			}
		}
		else {
			log.warn("MemberStatus: Local member " + local.getUuid() + " couldn't find a leader. Attempting refresh.");
			//dumpMembers(local.getUuid(), members);
			
			// vsilva 9/24/15 Try to refresh
			leaderRefreshId(hazelcastInstance);
		}
	}
	
	/**
	 * Get an {@link IAtomicReference} representing a cluster member leader.
	 * @param instance {@link HazelcastInstance}
	 * @return {@link IAtomicReference} representing a cluster member leader.
	 */
	private IAtomicReference<String> leaderGet(HazelcastInstance instance) {
		return instance.getAtomicReference("leader");
	}
	
	private void dumpMembers (String label, Collection <Member> members ) {
		if ( members.size() == 0) {
			return;
		}
		int count 						= 0;
		IAtomicReference<String> ref 	= leaderGet(hazelcastInstance);
		String leader					= ref != null ? ref.get() : null;
		
		log.debug("---- Member Dump. Size: " + members.size() + " [" + label + "] ----");
		
		for (Member m : members) {
			Map<String, Object> attribs = m.getAttributes();
			String ctxPath				= m.getStringAttribute(CloudCluster.KEY_CTX_PATH);
			int status 					= attribs.containsKey(CloudCluster.KEY_STATUS) 
					? m.getIntAttribute(CloudCluster.KEY_STATUS) : -1;

			log.debug("Member [" + count + "]: " + m.getUuid() + " " +  ctxPath
					+ " Status:" + status 
					+ ( leader != null && leader.equals(m.getUuid()) ? " *LEADER*" : ""));
			count ++;
		}
		log.debug("---------------------------------------------------");
	}
	
	/*
	 * Get a list of online members (Ignores the cluster manager).
	 */
	private List<Member> getOnlineMembers(Set<Member> members) {
		List<Member> online	= new ArrayList<Member>();
		
		for (Member m : members) {
			Map<String, Object> attribs = m.getAttributes();
			String ctxPath				= m.getStringAttribute(CloudCluster.KEY_CTX_PATH);
			
			if (! attribs.containsKey(CloudCluster.KEY_STATUS)) {
				log.debug("LeaderSelect: Member "  + m.getUuid()  + " " + ctxPath + " has no status attribute");
				continue;
			}
			int status 	= m.getIntAttribute(CloudCluster.KEY_STATUS);
			
			// ignore any /CloudClusterManager
			if ( isClusterManager(m)) { // ctxPath.contains("Manager")) {
				log.debug("LeaderSelect: Ignoring (MANAGER?) " + ctxPath + " Id:" + m.getUuid() );
				continue;
			}
			
			//log.debug("LeaderSelect: Member [" + count + "]: " + m.getUuid() + " " +  ctxPath + " Status:" + status);
			
			if ( status == 200) {
				//return m;
				online.add(m);
			}
			//count++;
		}
		return online;
	}
	
	/**
	 * Get the first randomly selected online member.
	 * @param members Cluster members.
	 * @return
	 */
	private Member getFirstOnlineMember (Set<Member> members) {
		List<Member> online	= getOnlineMembers(members);

		dumpMembers("ONLINE", online);

		// return a random online member
		Member m = !online.isEmpty() ? online.get((int)(Math.random() * online.size())) : null;
		return m;
	}
	
	/**
	 * <p>Automatically select a "leader" node from the cluster member list.
	 * A leader is a node that can be used to send unique events to an outside entity
	 * when such functionality is required. For example when you have duplicate
	 * events are received by all nodes, but only one node should push them somewhere.</p>
	 * 
	 * <b>Note:</b> When a leaders stops/crashes a new leader is automatically selected
	 * using membership listeners.

	 * @param instance {@link HazelcastInstance}
	 * @return An {@link IAtomicReference} representing a cluster member leader.
	 */
	private IAtomicReference<String> leaderSelect(HazelcastInstance instance) {
		// get the 1st member on the list
		Set<Member> members = instance.getCluster().getMembers();
		Member member  		= null; 
		
		// select the 1st active member
		log.debug("LeaderSelect: Selecting a leader from " + members.size() + " members...");

		// get 1st online member
		member = getFirstOnlineMember(members);
		
		// no one online? they may be booting up, 
		if ( member == null ) {
			log.warn("LeaderSelect: No member is online! Choosing first in line."); 
					
			// loop again, select the 1st that is not /CloudClusterManager
			for (Member m : members) {
				String ctxPath = m.getStringAttribute(CloudCluster.KEY_CTX_PATH);

				if ( isClusterManager(m)) { // ctxPath != null && ctxPath.contains("Manager")) {
					log.debug("LeaderSelect: Ignoring (MANAGER) " + ctxPath + " Id:" + m.getUuid() );
					continue;
				}
				member = m;
				break;
			}
			if ( member != null) {
				log.debug("LeaderSelect: Selected member is " + member.getUuid() 
						+ " CtxPath " + member.getStringAttribute(CloudCluster.KEY_CTX_PATH));
			}
		}
		
		// add member UUID to an atomic reference
		IAtomicReference<String> leader = leaderGet(instance);
		
		// FIXME: only set if empty. It will set 1 time only (first that gets here).
		if ( leader.isNull() || leader.get() == null) {
			if ( member != null ) {
				log.debug("LeaderSelect: Setting leader to " + member.getUuid());
				leader.set(member.getUuid());
			}
			else {
				log.warn("LeaderSelect: Can't find a potential leader among members list."); 
			}
		}
		else {
			log.warn("LeaderSelect: Unable to set leader. There is a leader already with id " + leader.get());
		}
		return leader;
	}
	
	/**
	 * Refresh the leader UUID. Invoke when a leaders quits/stops/crashes & needs to be refreshed.
	 * The leader UUID will be changed iif:
	 * <ul>
	 * <li> The member uuid == the leader uui OR
	 * <li> Take over (force) is true.
	 * </ul
	 * @param instance {@link HazelcastInstance}.
	 * @param member The local member that is doing the refresh.
	 * @param takeOver If true become the leader (force).
	 */
	private void leaderRefreshId (HazelcastInstance instance) {
		IAtomicReference<String> leader = leaderGet(instance);
		String old = leader.get();

		leader.clear();
			
		leaderSelect(instance);
		log.debug("Leader Refresh Old: " + old + " New: " + leader.get());
	}
	
	/**
	 * <p>Get a "leader" node from the cluster member list.
	 * A leader is a node that can be used to send unique events to an outside entity
	 * when such functionality is required. For example when you have duplicate
	 * events are received by all nodes, but only one node should push them somewhere.</p>
	 * 
	 * <b>Note:</b> When a leaders stops/crashes a new leader is automatically selected
	 * using membership listeners.
	 * 
	 * @return An {@link IAtomicReference} representing a cluster node leader.
	 */
	public /*IAtomicReference<String>*/ String getLeader () {
		return leaderGet(hazelcastInstance).get();
	}

	/**
	 * Is the local member the leader? A node will invoke this to decide if a duplicate event
	 * should be consumed.
	 * <p>A leader is a node that can be used to send unique events to an outside entity
	 * when such functionality is required. For example when you have duplicate
	 * events are received by all nodes, but only one node should push them somewhere.</p>
	 * 
	 * <b>Note:</b> When a leaders stops/crashes a new leader is automatically selected
	 * using membership listeners.

	 * @return true if the local node has been selected a s the "leader"
	 */
	public boolean isClusterLeader () {
		Member local 					= hazelcastInstance.getCluster().getLocalMember();
		IAtomicReference<String> leader = leaderGet(/*AdapterCluster.*/hazelcastInstance);
		return leader.get().equalsIgnoreCase(local.getUuid());
	}
	
	/**
	 * Relinquish leadership & have the server choose a new random leader.
	 * Note:
	 * <li> Only online nodes can take over leadership.
	 * <li> Only the leader can relinquish.
	 */
	public void relinquishLeadership () {
		Member local = hazelcastInstance.getCluster().getLocalMember();
		if ( ! isClusterLeader()) {
			log.warn("RelinquishLeadership: Member " + local.getUuid() + " is NOT a leader. Only a leader can relinquish.");
			return;
		}
		// don't relinquish if no online nodes or only 1 is online (note: online is NOT NULL)
		List<Member> online = getOnlineMembers(hazelcastInstance.getCluster().getMembers());
		
		if ( online.size() < 2 ) {
			log.warn("RelinquishLeadership: Member " + local.getUuid() + " NOT enough ONLINE members (" + online.size() + ") to relinquish.");
			return;
		}
		log.warn("RelinquishLeadership: Member " + local.getUuid() + " is relinquishing leadership.");
		leaderRefreshId(hazelcastInstance);
	}
	
	/*
	 * END LEADER STUFF
	 */
	
	/**
	 * Get a distributed lock.
	 */
	@Override
	public DistributedLock getLock(String name) {
		return new DistributedLock(hazelcastInstance, name);
	}

	/**
	 * Get a cluster wide long
	 */
	@Override
	public DistributedAtomicLong getAtomicLong(String name) {
		return new DistributedAtomicLong(hazelcastInstance, name);
	}

	/**
	 * A topic for cluster wide messages.
	 */
	@Override
	public IClusterTopic getTopic(String name, IClusterTopicListener listener) {
		ClusterMember local = getLocalMember();
		return new DistributedTopic(hazelcastInstance, local.getUuid(), name, listener);
	}

	/**
	 * Get the current node attributes {@link Map}.
	 */
	@Override
	public Map<String, Object> getMemberAttributes() {
		return hazelcastInstance.getConfig().getMemberAttributeConfig().getAttributes();
	}
	
	/**
	 * Get the cluster members.
	 * @return {@link List} of {@link ClusterMember}.
	 */
	public List<ClusterMember> getMembers() {
		List<ClusterMember> members = new ArrayList<ClusterMember>();
		Cluster cluster 			= hazelcastInstance.getCluster();
		Set<Member> setMembers  	= cluster.getMembers();
		
		for ( Member member : setMembers ) {
			ClusterMember cm = new ClusterMember(member.getUuid()
					, member.localMember()
					, member.getSocketAddress().toString()
					, member.getAttributes());
			members.add(cm);
		}
		return members;
	}
	
	/**
	 * Get members as a {@link JSONObject}.
	 * @return members as a {@link JSONObject}. Format: [{'uuid': 'id', 'islocal': true, 'address': 'IP', 'attributes': {'k1':'v1','k2':'v2',...}},...]
	 * @throws JSONException On parse errors.
	 */
	public JSONArray getMembersAsJSON() throws JSONException {
		JSONArray root 				= new JSONArray();
		List<ClusterMember> members = getMembers();
		
		for (ClusterMember clusterMember : members) {
			root.put(clusterMember.toJSON());
		}
		return root;
	}
	

	/**
	 * Set a local member attribute.
	 * @param key Attribute key.
	 * @param value Attribute value.
	 */
	public void setLocalMemberAttribute(String key, String value) {
		if ( hazelcastInstance == null || hazelcastInstance.getCluster() == null) {
			return;
		}
		Member local = hazelcastInstance.getCluster().getLocalMember();
		
		log.debug("Cluster Set local member attribute: " + key + " = " + value);
		local.setStringAttribute(key, value);
	}
	
	/**
	 * Set the status of the local member: Online, Offline, Server error, etc..
	 * @param code HTTP status code: 200 = online, 503 = Service down, 401 = Auth failed, 500 = Server error. 
	 * @param message Status message.
	 */
	public void setLocalMemberStatus (int code , String message) {
		if ( hazelcastInstance == null || hazelcastInstance.getCluster() == null) {
			log.warn("Cluster Set member status to " + message + ". CLUSTER NOT INITIALIZED.");
			return;
		}
		Member local = hazelcastInstance.getCluster().getLocalMember();
		
		local.setIntAttribute(CloudCluster.KEY_STATUS, code);
		local.setStringAttribute(CloudCluster.KEY_STMSG, message);
		
		// plus OS metrics
		setLocalMemberOSMetrics();
	}
	
	/**
	 * Set local member OS metrics. This can be invoked many times by the member to update
	 * real time {@link OSMetrics}:
	 * <ol>
	 * <li>OS Name: Operating system name.
	 * <li>Number of CPUS.
	 * <li>Peak threads.
	 * <li>CPU load.
	 * <li>Free memory.
	 * </ul>
	 */
	public void setLocalMemberOSMetrics () {
		Member local = hazelcastInstance.getCluster().getLocalMember();
		
		// injectOSMetrics(local.getAttributes()); GIVES java.lang.UnsupportedOperationException
		//		at com.core.cluster.AdapterCluster.injectOSMetrics(AdapterCluster.java:162)
		// Add optional OS metrics to the member attributes
		try {
			JSONObject os = OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);
			
			// sys cpu, peak threads, os name, # cpus, free mem (bytes). Must match injectOSMetrics!
			local.setFloatAttribute(OSMetrics.KEY_SYS_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_SYS_CPU)));
			local.setFloatAttribute(OSMetrics.KEY_PROC_CPU , Float.parseFloat(os.getString(OSMetrics.KEY_PROC_CPU)));
			local.setIntAttribute(OSMetrics.KEY_PEAK_THR , os.getInt(OSMetrics.KEY_PEAK_THR));
			local.setStringAttribute(OSMetrics.KEY_OS_NAME , os.getString(OSMetrics.KEY_OS_NAME));
			local.setIntAttribute(OSMetrics.KEY_NUM_CPUS , os.getInt(OSMetrics.KEY_NUM_CPUS));
			local.setLongAttribute(OSMetrics.KEY_FREE_MEM , os.getLong(OSMetrics.KEY_FREE_MEM));
			local.setLongAttribute(OSMetrics.KEY_HEAP_FREE , os.getLong(OSMetrics.KEY_HEAP_FREE));
			local.setLongAttribute(OSMetrics.KEY_HEAP_MAX , os.getLong(OSMetrics.KEY_HEAP_MAX));
			local.setLongAttribute(OSMetrics.KEY_HEAP_TOTAL , os.getLong(OSMetrics.KEY_HEAP_TOTAL));
		} catch (JSONException e) {
		}
		
	}

	/**
	 * Get the opaque cluster provider via the {@link IClusterInstance} interface only. 
	 * This method is provided so clients can access cluster objects unimplemented by {@link IClusterInstance}.
	 * @return Opaque {@link HazelcastInstance}.
	 */
	public HazelcastInstance getClusterProvider() {
		return hazelcastInstance;
	}

	/**
	 * Get the group name this node belongs to.
	 * @return The cluster group (used for multi-cluster) support.
	 */
	public String getClusterGroupName() {
		return hazelcastInstance != null 
				? hazelcastInstance.getConfig().getGroupConfig().getName()
				: null;
	}

	/**
	 * Get a list of the HZ Tcp IP configuration members used for manual join (when multi-cast is disabled).
	 * @return A list of member IP addresses.
	 */
	public List<String> getClusterTcpMembers () {
		return hazelcastInstance != null 
				? hazelcastInstance.getConfig().getNetworkConfig().getJoin().getTcpIpConfig().getMembers()
				: null;
	}

	public boolean isShutdown () {
		return hazelcastInstance == null;
	}
	
	public void dumpAllInstances(String label) {
		Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();

		log.info("---------- Cluster Instances Size:" + instances.size() + " " + label + " ----------------");
		for (HazelcastInstance instance : instances) {
			log.info("Cluster instance: " + instance.getName()  + " Group:" + instance.getConfig().getGroupConfig().getName() );
		}
		log.info("-----------------------------------------------------------");
	}

	/**
	 * Shut down the cluster. Invoke this when the application stops.
	 */
	public void shutdown() {
		if ( hazelcastInstance != null) {
			log.debug("Shutting down HAZELCAST cluster.");
			hazelcastInstance.shutdown();
		}
		hazelcastInstance 	= null;
	}

	@Override
	public <K,V> /*ICluster*/IMap<K,V> getMap(String name) {
		return new DistributedMap<K,V>(hazelcastInstance, name);
	}

	@Override
	public <V>List<V> getList(String name) {
		return new DistributedList<V>(hazelcastInstance, name);
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

	/**
	 * Method used to perform maintenance & garbage collection tasks.
	 */
	public void collectGarbage() {
	}

	/**
	 * Get a JSON description of all distributed collections in Data tables format.
	 * @return { "data": [[ROW1],[ROW2],...]} where ROW [NAME, TYPE(T_LIST, T_MAP, T_TOPIC, T_PRIMITIVE), CONTENT, EXPIRED(boolean)]
	 * @throws JSONException On JSON errors.
	 */
	@Override
	public JSONObject describe() throws JSONException {
		JSONObject root 					= new JSONObject();
		Collection<DistributedObject> objs 	= hazelcastInstance.getDistributedObjects();
		JSONArray array 					= new JSONArray();

		// Get HZ distributed object information.
		// Each row must have 4 cols: NAME, TYPE, VALUE, EXPIRED else the console will give an error.
		for (DistributedObject obj : objs) {
			JSONArray row 	= new JSONArray();

			if ( obj instanceof com.hazelcast.core.IMap) {
				com.hazelcast.core.IMap<?,?> map 	= ((com.hazelcast.core.IMap<?,?>)obj);
				
				row.put(obj.getName());
				row.put(ObjectType.T_MAP);
				row.put(new JSONObject(map));
			}
			// 2/27/2020
			else if ( obj instanceof com.hazelcast.core.IList) {
				com.hazelcast.core.IList<?> list 	= ((com.hazelcast.core.IList<?>)obj);
				
				row.put(obj.getName());
				row.put(ObjectType.T_LIST);
				row.put(new JSONArray(list));
			}
			else if ( obj instanceof com.hazelcast.topic.impl.TopicProxy) {
				com.hazelcast.topic.impl.TopicProxy<?> topic = ((com.hazelcast.topic.impl.TopicProxy<?>)obj);
				
				row.put(obj.getName());
				row.put(ObjectType.T_TOPIC);
				row.put(topic.toString());
			}
			if ( row.length() > 0) {
				row.put(false);		// expired
				array.put(row);
			}
		}
		root.put("data", array);
		return root;
	}
}