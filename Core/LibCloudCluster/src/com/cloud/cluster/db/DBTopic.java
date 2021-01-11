package com.cloud.cluster.db;


import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.ClusterTopicEvent;
import com.cloud.cluster.IClusterTopic;
import com.cloud.cluster.IClusterTopicListener;
import com.cloud.core.db.Database;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Database implementation of {@link IClusterTopic}.
 * <pre>
 * IClusterTopic topic = cluster.getClusterInstance().getTopic("TOPIC1", new IClusterTopicListener() {
 *  @Override
 *  public void onMessage(JSONObject obj) {
 *    LOGD("Got JSON message " + obj);
 *  }
 * });
 * 
 * topic.publish(new JSONObject(json));
 * </pre>
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class DBTopic implements IClusterTopic  {

	static final Logger log 					= LogManager.getLogger(DBTopic.class);
		
	private final static String DB_TABLE_NAME	= "TOPICS";
	
	private Database db 						= Database.getInstance();

	private String 					nodeId;
	private String 					topicName;
	private IClusterTopicListener 	listener;
	
	private long ticks;
	
	private long lastDispatchTime;
	
	public DBTopic(String nodeId, String topicName, IClusterTopicListener listener) {
		super();
		this.topicName 	= topicName;
		this.listener 	= listener;
		this.nodeId 	= nodeId;
	}

	private void dbUpdate (String label, String SQL) {
		try {
			int rows = db.update(SQL);
			if ( rows == 0) {
				throw new IOException("DB Update returned 0 results: " + SQL);
			}
		} catch (Exception e) {
			log.error(label + " " + SQL, e);
		}
	}
	
	/**
	 * This method gets invoked by {@link CloudCluster} -> {@link DBClusterInstance} -> {@link DBTopic} ~ 2/sec
	 */
	void dispatch () {
		// ~ every 5s
		if ( (ticks++ % 10) != 0) {
			return;
		}
		//System.out.println("** DISPATCH ticks:" + ticks);
		try {
			// Get the last message - name (Str), createdTime (long) payload (JSON str)
			String SQL 				= String.format("select * from %s WHERE name = '%s' ORDER BY createdTime DESC LIMIT 1", DB_TABLE_NAME, topicName);
			JDBCResultSet results 	= db.query(SQL);
			JSONArray rows 			= results.getResultSet();

			// no data?
			if ( rows.length() == 0) {
				return;
			}

			// get results
			JSONArray row0			= rows.getJSONArray(0);
			
			String publisher		= row0.getString(1);
			long time				= row0.getLong(2);
			String payload			= row0.getString(3);

			// only the publisher can dispatch
			if ( !publisher.equals(nodeId)) {
				return;
			}
			if ( time > lastDispatchTime) {
				listener.onMessage(new ClusterTopicEvent(publisher, new JSONObject(payload)));
				lastDispatchTime = time;
			}
		} catch (Exception e) {
			log.error("dispatch()", e);
		}
	}
	
	@Override
	public void publish(JSONObject message) {
		long time 	= System.currentTimeMillis();
		String SQL 	= String.format("INSERT INTO %s VALUES( '%s', '%s' , %d , '%s' )", DB_TABLE_NAME, topicName, nodeId, time, message.toString());
		dbUpdate("publish()", SQL);
	}

	@Override
	public void destroy() {
		String SQL 	= String.format("DELETE FROM %s WHERE name = '%s'", DB_TABLE_NAME, topicName);
		dbUpdate("destroy()", SQL);
	}

}
