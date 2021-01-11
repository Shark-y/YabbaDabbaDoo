package com.rts.datasource.db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataMatcher;

public class DBDataMatcher implements IDataMatcher {

	private static final Logger log = LogManager.getLogger(DBDataMatcher.class);
			
	/**
	 * Match the db data against the DS fields.
	 * @param dataSourceName DS name
	 * @param data Batch JSON array: [["1000","vdn1000",5,"1:0:0","10",2,"20",8,"30",8],["1001","vdn1001",13,"2:0:0","8",3,"30",23,"40",13],...]
	 * @param format The DS {@link DataFormat}.
	 * @param listener Event sink.
	 */
	@Override
	public void matchFormat(String dataSourceName, String data,	DataFormat format, IBatchEventListener listener) throws JSONException {
		// Data [["1000","vdn1000",5,"1:0:0","10",2,"20",8,"30",8],["1001","vdn1001",13,"2:0:0","8",3,"30",23,"40",13],["1002","vdn1002",2,"1:3:0","1",12,"210",12,"10",2],["1003","vdn1003",15,"1:0:4","40",8,"90",2,"20",1]]
		// Fields: VDN,NAME,CALLS_WAITNG,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS
		JSONArray ar 	= new JSONArray(data);
		String[] fields = format.getFields().split(",");

		// no data
		if ( ar.length() == 0 || ar.getJSONArray(0).length() == 0) {
			return;
		}
		// data.row(0) len > = fields.len else error
		if ( ar.getJSONArray(0).length() < fields.length) {
			log.error(String.format("DB data ROW size %d must be >= DS descriptor fieds size %d (%s)" , ar.getJSONArray(0).length(), fields.length, format.getFields()));
			return;
		}
		final JSONObject root 	= new JSONObject();
		final JSONArray jbatch	= new JSONArray();

		for (int i = 0; i < ar.length(); i++) {
			final JSONObject jrecord 	= new JSONObject();
			final JSONArray row 		= ar.getJSONArray(i);
			
			for (int j = 0; j < fields.length; j++) {
				jrecord.put(fields[j], row.get(j));
			}
			jbatch.put(jrecord);
		}
		
		root.put("listenerName", dataSourceName); 
		root.put("batchDate", System.currentTimeMillis());
		root.put("batchData", jbatch);
		
		//System.out.println(root.toString(1));
		listener.onBatchReceived(root);
	}

	@Override
	public long getTotalBatches() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalRecords() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getRemoteTotalBatches() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void resetMetrics() {
		// TODO Auto-generated method stub
		
	}

}
