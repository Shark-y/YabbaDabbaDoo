package com.rts.datasource.sock;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataMatcher;

/**
 * Default raw socket data a listener descriptor matcher.
 * 
 * For example, a raw socket buffer provided by the Avaya RT socket VDN table (see the rt-socket tech description) such as
 * 
 * <h2>F1 Record Type - CVDN Table</h2>
 * <pre>
 * DATA ITEM           Field size	Type	Description
 * F1                   2	string	Static “F1” string
 * VDN                  7	number	VDN number
 * VDN                  20	synonym	VDN synonym
 * INPROGRESS-ATAGENT   4	number	Calls waiting
 * OLDESTCALL           5	mm:ss	Oldest call
 * AVG_ANSWER_SPEED     5	mm:ss	Average answer speed
 * ABNCALLS             4	number	Abandoned calls
 * AVG_ABANDON_TIME     5	mm:ss	Average Abandon Time
 * ACDCALLS             4	number	ACD calls
 * AVG_ACD_TALK_TIME    5	mm:ss	Avg. ACD talk time
 * ACTIVECALLS         4	Number	Active calls
 * 
 * F1|  74500|Eastern Sales|   2|:00||   8|58:54|  46|23:37|  54 
 * F1|  78808|Eastern Sales|   3|:00||   6|40:58|  11|56: 1|  99 
 * 
 * Will produce the JSON object:
 * <pre>
 * {"batchDate":1451855183491
 *  ,"batchData":[
 *  	{"F1":"F1","VDN":"74500","ACDCALLS":"46","ABNCALLS":"8","INPROGRESS-ATAGENT":"2"
 *  	,"AVG_ACD_TALK_TIME":"23:37","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"58:54","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"54"}
 *  	,{"F1":"F1","VDN":"78808","ACDCALLS":"11","ABNCALLS":"6","INPROGRESS-ATAGENT":"3","AVG_ACD_TALK_TIME":"56: 1"
 *  	,"VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"40:58","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"99"}
 *  ]}</pre>
 *  
 * @author VSilva
 *
 */
public class TCPSockDataMatcher implements IDataMatcher {

	private static final Logger log = LogManager.getLogger(TCPSockDataMatcher.class);
	
	private long totalBatches;
	private long totalRemoteBatches;
	private long totalRecords;
	
	static void LOGD(final String text) {
		log.debug(text);
	}

	static void LOGE(final String text) {
		log.error(text);
	}

	/**
	 * Check if a record data contains a header or footer
	 * @param label A debug/display label: HEADER or FOOTER.
	 * @param hdrFtr The header or footer described in the {@link DataFormat}.
	 * @param record The character data to check if it is a header or footer.
	 * @param sep The field separator described in the {@link DataFormat}.
	 * @return True if record is a header or footer and therefore should be ignored.
	 */
	/*
	private boolean skipHeaderFooter(String label , String hdrFtr, String record, String sep) {
		if ( hdrFtr == null || hdrFtr.isEmpty()) {
			//LOGD("Format " + label + " [" + hdrFtr + "] can't be NULL/EMPTY. Rec: " + record);
			return false;
		}
		final String fieldSep 	= sep.replaceAll("\\\\", "");	// must remove escape char (\)
		final String key 		= hdrFtr.contains(fieldSep) ? hdrFtr.substring(0, hdrFtr.indexOf(fieldSep)) : null;

		if ( key != null && record.contains(key)) {
			LOGD("Ignoring batch " + label + " " + record);
			return true;
		}
		return false;
	} */
	
	/**
	 * Match socket data values & format fields into an JSON obj.
	 * @param dsName Name or Id of the {@link TCPSockDataSource} that listens for and handles data (a.k.a Data source name).
	 * @param batchData Socket batch data of the form: 
	 * <li>Batch  = HEADER\nrecord1\nrecord2\n...record(N)\nFOOTER
	 * <li>Record = field1|field2|...|fieldN
	 * @param format The {@link DataFormat} used to parse the socket buffer.
	 * @param listener The {@link IBatchEventListener} object that consumes the batch.
	 */
	public void matchFormat (final String dsName, final String batchData, final DataFormat format, final IBatchEventListener listener) throws JSONException {
		// Listener should not be null
		if ( listener == null) {
			//System.out.print(batchData);
			return;
		}
		final JSONObject root 	= new JSONObject();
		final JSONArray jbatch	= new JSONArray();
		
		// Split records by \n
		final String[] records 	= batchData.split(LINE_FEED);	// val1|val2|...|val(n)
		final Object[] fields 	= format.getDataFields().toArray();		// key1,key2,...,key(n)
		final String TAG 		= "[" + dsName +  "]";
		
		totalRecords += records.length;
		totalBatches ++;
		
		for (String record : records) {
			final JSONObject jrecord 	= new JSONObject();
			final String[] values 		= record.split(format.getFieldSep());
			final String header			= format.getHeader();
			final String footer			= format.getFooter();

			// skip the header: F0|... (Note: HDR is optional)
			//if ( skipHeaderFooter("HEADER", format.header, record, format.fieldSep)) continue;
			if ( header != null && !header.isEmpty() && record.contains(header)) {
				log.trace(TAG + " Ignoring HEADER (" + record + ")");
				continue;
			}
			
			// skip the footer: F3|...
			//if ( skipHeaderFooter("FOOTER", format.footer, record, format.fieldSep)) continue;
			if ( record.contains(footer)) {
				log.trace(TAG + " Ignoring FOOTER (" + record + ")");
				continue;
			}

			// If the socket data field < the fields descriptor size we have a problem
			if ( values.length < /*!=*/ fields.length) {
				final String error = TAG + " Invalid size " + values.length  + " for socket record [" + record + "] MUST BE " 
						+ fields.length + " " + Arrays.toString(fields);
				
				LOGE(error);
				continue;
			}
			else if ( values.length >  fields.length) {
				// sock field size > df field size is ok, Continue with a warning
				log.warn(TAG + " Socket record size " + values.length  + " is GREATER THAN descriptor field size " 
						+ fields.length + ". (They should be equal)");
			}
			// Too much LOGD(record + " Rec size:" + values.length + " Fld Size:" + fields.length + " FSep:" + format.fieldSep);
			
			// match field(n) against values(n) for record(i)
			for (int j = 0 ; j < fields.length ; j++) {
				jrecord.put(fields[j].toString(), parseData(values[j].trim()));
			}
			jbatch.put(jrecord);
		}
		
		log.trace(TAG + " Consumed " + records.length + " records. Totals (Batches: " + totalBatches + " Records:" + totalRecords + ")");
		
		// no data...
		if ( jbatch.length() == 0 ) {
			LOGD ("Ignoring 0 size batch " + batchData);
			return;
		}
		root.put("listenerName", dsName); 
		root.put("batchDate", System.currentTimeMillis());
		root.put("batchData", jbatch);
		
		// 9/21/2017 Add the format for micro services
		root.put("batchFormat", format.toJSON());
		
		listener.onBatchReceived(root);
	}

	/**
	 * Try to parse a data value as an {@link Integer} or string.
	 * @param value String value to parse (integer or string).
	 * @return An {@link Integer} or {@link String} (if not int).
	 */
	private Object parseData(final String value) {
		// assume integer
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// Not an integer, assume string
			return value;
		}
	}
	
	public long getTotalBatches() {
		return totalBatches;
	}
	
	public long getTotalRecords() {
		return totalRecords;
	}

	@Override
	public void resetMetrics() {
		totalBatches = totalRecords = totalRemoteBatches = 0;
	}

	@Override
	public long getRemoteTotalBatches() {
		return totalRemoteBatches;
	}
	
}
