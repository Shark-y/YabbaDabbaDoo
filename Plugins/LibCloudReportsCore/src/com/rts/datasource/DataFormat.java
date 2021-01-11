package com.rts.datasource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A generic data format is used to match the socket raw data:
 * <pre>
 * F0| START DATE
 * F1|  74500|Eastern Sales|   2|:00||   8|58:54|  46|23:37|  54 
 * F1|  78808|Eastern Sales|   3|:00||   6|40:58|  11|56: 1|  99 
 * F1|  35611|Eastern Sales|   1|:00||   9|12:51|  78|23:45|  99
 * F3| END
 * </pre>
 * With a list of fields:
 * <pre>
 * F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS
 * </pre>
 * To produce a JSON object where each field is the key that matches against its corresponding value in the raw buffer:
 * <pre>
 * {"F1":"F1","VDN":"74500","ACDCALLS":"46","ABNCALLS":"8","INPROGRESS-ATAGENT":"2","AVG_ACD_TALK_TIME":"23:37","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"58:54","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"54"}
 * </pre>
 * 
 * @author VSilva
 * @version 1.1 - 11/16/2017 Added storage options.
 *
 */
public class DataFormat {

	/** Header: F0| START DATE */
	String header;
	
	/** Footer: F3| END */
	String footer;
	
	/** Field separator Default: | */
	String fieldSep;
	
	/** Record separator: Default: LienFeed (\n) */
	private String recSep		= "LF";
	
	/** List of fields that describe this data source */
	final List<String> fields 	= new ArrayList<String>();
	
	/** Comma sep list of fields to be stored (if any) */
	final String storageFields;
	
	/** 11/1/6/2017 If true clean table before updates*/
	final boolean storageOptWipeTable;
	
	static void LOGD(String text) {
		System.out.println("[FORMAT] " + text);
	}
	static void LOGE(String text) {
		System.err.println("[FORMAT] " + text);
	}

	/**
	 * Construct a data format.
	 * @param header Buffer header (optional).
	 * @param footer Buffer footer.
	 * @param fieldSep Field separator among fields within each record.
	 * @param recSep record separator. Default: \n.
	 * @param fields Comma separated list of field names matching the raw buffer.
	 * @param storageFields Comma separated list of fields names to be stored on disk/DB (or NULL if none).
	 */
	public DataFormat(final String header, final String footer, final String fieldSep, final String recSep, final String fields, final String storageFields) {
		this(header, footer, fieldSep, recSep, fields, storageFields, false);
	}
	
	/**
	 * Construct a data format.
	 * @param header Buffer header (optional).
	 * @param footer Buffer footer.
	 * @param fieldSep Field separator among fields within each record.
	 * @param recSep record separator. Default: \n.
	 * @param fields Comma separated list of field names matching the raw buffer.
	 * @param storageFields Comma separated list of fields names to be stored on disk/DB (or NULL if none).
	 * @param optWipeTable If true delete all records in the table before updates.
	 */
	public DataFormat(final String header, final String footer, final String fieldSep, final String recSep, final String fields, final String storageFields, boolean optWipeTable) {
		this.header 	= header;
		this.footer 	= footer;
		this.fieldSep 	= fieldSep;
		
		if ( recSep != null )  this.recSep = recSep;
		
		String[] tmp = fields.split(",");
		for (String field : tmp) {
			this.fields.add(field);
		}
		this.storageFields 			= storageFields;
		this.storageOptWipeTable	= optWipeTable;
	}

	/**
	 * Parse form JSON { header: "F0|" , footer: "F3|", fieldSep: "|", recSep: ",", fields: "F1,F2..."}
	 * @param root { header: "F0|" , footer: "F3|", fieldSep: "|", recSep: ",", fields: "F1,F2..."}
	 * @throws JSONException 
	 */
	public DataFormat (JSONObject root) throws JSONException {
		this(root.optString("header"), root.optString("footer")
				, root.optString("fieldSep"), root.optString("recSep") 
				, root.getString("fields"), root.optString("storageFields")
				, root.optBoolean("storageOptWipeTable")
				);
	}
	
	public String getHeader () {
		return header;
	}

	public String getFooter () {
		return footer;
	}

	public String getFieldSep () {
		return fieldSep;
	}
	
	/**
	 * Serialize to XML. See the schema document for details.
	 * @deprecated Use toJSON instead.
	 * @return XML {@link DataFormat}:
	 * <pre>
	 * &lt;format>
	 *   &lt;header>&lt;/header>
	 *   &lt;footer>F3|END_OF_RECORDS&lt;/footer>
	 *   &lt;fieldSep>\|&lt;/fieldSep>
	 *   &lt;fields>F1,VDN,VDN SYN,INPROGRESS-ATAGENT&lt;/fields>
	 * &lt;/format>
	 * </pre>
	 * @throws IOException
	 */
	public String toXML () throws IOException {
		//if ( header == null ) throw new IOException("Format header is required");
		if ( footer == null ) throw new IOException("Format footer is required.");
		if ( fieldSep == null ) throw new IOException("Format field separator is required.");
		
		StringBuffer buf = new StringBuffer("\t<format>");
		buf.append("\n\t\t<header>" + (header != null ? header : "" ) + "</header>");
		buf.append("\n\t\t<footer>" + footer + "</footer>");
		buf.append("\n\t\t<fieldSep>" + fieldSep + "</fieldSep>");
		buf.append("\n\t\t<recSep>" + recSep + "</recSep>");
		buf.append("\n\t\t<fields>" + getFields()  + "</fields>");
		buf.append("\n\t</format>");
		return buf.toString();
	}

	/**
	 * As JSON
	 * @return { header: "F0|" , footer: "F3|", fieldSep: "|", recSep: ",", fields: "F1,F2..."}
	 * @throws JSONException
	 */
	public JSONObject toJSON () throws JSONException {
		JSONObject root = new JSONObject();
		root.putOpt("header", header);
		root.putOpt("footer", footer);
		root.put("fieldSep", fieldSep);
		root.put("recSep", recSep);
		root.put("fields", getFields());
		root.putOpt("storageFields", storageFields);
		if ( storageOptWipeTable )	root.put("storageOptWipeTable", storageOptWipeTable);
		return root;
	}
	
	/**
	 * Format fields as CSV string (trimmed - no spaces between).
	 * @return field1, field2,...filed(n)
	 */
	public String getFields() {
		//return Arrays.toString(fields.toArray()).replaceAll("[\\[\\]]", "");
		Object[] array 	= fields.toArray();
		if ( array.length == 0) return "";
		
		StringBuffer buf = new StringBuffer(array[0].toString());
		
		for (int i = 1; i < array.length; i++) {
			buf.append("," + array[i].toString().trim());
		}
		return buf.toString();
	}
	
	@Override
	public String toString() {
		return  ( fields.size() < 20 
					?  "Fields: " + Arrays.toString(fields.toArray()).replaceAll("[\\[\\]]", "")
					: fields.size() + " fields." );
	}
	
	public List<String> getDataFields() {
		return Collections.unmodifiableList(fields);
	}
	
	public String getStorageFields() {
		return storageFields;
	}
	
	public boolean getStorageOptWipeTable () {
		return storageOptWipeTable;
	}
}
