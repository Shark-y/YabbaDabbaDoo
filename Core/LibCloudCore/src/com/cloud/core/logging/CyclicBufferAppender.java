package com.cloud.core.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.CyclicBuffer;
import org.apache.log4j.helpers.Transform;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;

/**
 * Log4j {@link CyclicBuffer} appender used by the cloud console log view display.
 * This appender returns logging events encoded as JSON which in turn are displayed in an HTML table
 * by the cloud console log event viewer.
 * @author vsilva
 *
 */
public class CyclicBufferAppender extends AppenderSkeleton {

	/** Appender name*/
	public static final String NAME = "CYCLIC_BUFFER_APPENDER";

	/** Max # of events */
	private int bufferSize = 512;
	
	/** LOG4J {@link CyclicBuffer} */
	protected CyclicBuffer cb = new CyclicBuffer(this.bufferSize);

	// 11/29/16 FindBugs Call to method of static java.text.DateFormat in com.cloud.core.logging.CyclicBufferAppender.toJSON(CyclicBuffer)
	// As the JavaDoc states, DateFormats are inherently unsafe for multithreaded use. The detector has found a call to an instance of DateFormat that has been obtained via a static field. This looks suspicous. 
	// For more information on this see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6231579">Sun Bug #6231579 and <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6178997">Sun Bug #6178997. 
	/** Used to format dates */
	//static final SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss:SSS");

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		cb.add(event);
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
		this.cb.resize(bufferSize);
	}

	public int getBufferSize() {
		return this.bufferSize;
	}

	
	/**
	 * Return the Loj4J buffer as JSON.
	 * @return {@link JSONObject} of the form:
	 * <pre>{
	 *  "data": [
	 *  ["2016-02-04 10:11:56:101", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Checking if a sys admin pwd has been setup.&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:102", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: sys admin pwd has been setup. Checking for service descriptors @ C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:103", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Found descriptor C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default\\avaya-cfg.xml&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:104", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Node has been configured.&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:105", "NodeConfiguration", "WARN", "&lt;div style=\"color:black;background-color:yellow;\">Add URL: localhost is NOT a valid URL host name!&lt;\/div>"],
	 *  ...
	 * ]
	 * } </pre>
	 */
	public JSONObject toJSON() {
		return toJSON(cb);
	}
	
	static JSONObject toJSON(CyclicBuffer cb) {
		JSONObject root 			= new JSONObject();
		JSONArray data 				= new JSONArray();
		final SimpleDateFormat df 	= new SimpleDateFormat(LogManager.DATE_FORMAT);
		
		for (int i = 0; i < cb.length(); i++) {
			LoggingEvent ev = cb.get(i);
			JSONArray row = new JSONArray();
			
			// strip pkg name from logger name
			final String loggerName = ev.getLoggerName();
			final String className = loggerName.contains(".") 
					? loggerName.substring(loggerName.lastIndexOf(".") + 1) 
					: loggerName;

			// add an i (msec) so the events can be sorted by date.
			row.put(df.format(new Date(ev.getTimeStamp() + i  )));	// Time
			row.put(className);										// Source
			row.put(ev.getLevel().toString());						// Level

			// stack trace: New coll span = 4
			String[] s = ev.getThrowableStrRep();
			
			if ( s != null) {
				row.put(ev.getMessage() + " <stack> " + getThrowableAsHTML(s) + "</stack>");
			}
			else {
				row.put(ev.getMessage().toString());
			}
			data.put(row);
		}
		try {
			root.put("data", data);
		} 
		catch (JSONException e) {
		}
		return root;
	}

	/**
	 * Format an event stack trace for display.
	 * @param s
	 * @return
	 */
	static String getThrowableAsHTML(final String[] s) {
		final StringBuffer sbuf = new StringBuffer();
		if (s != null) {
			int len = s.length;
			if (len == 0)
				return "";
			sbuf.append(Transform.escapeTags(s[0]));
			sbuf.append(Layout.LINE_SEP);
			for (int i = 1; i < len; i++) {
				//sbuf.append("     "); //TRACE_PREFIX);
				sbuf.append(Transform.escapeTags(s[i]));
				sbuf.append(Layout.LINE_SEP);
			}
		}
		return sbuf.toString();
	}

	/**
	 * Clear the entire buffer.
	 */
	public void clear () {
		int len = cb.length();
		for (int i = 0; i < len; i++) {
			cb.get();
		}
	}
	
	/**
	 * Clean N items from the buffer.
	 * @param len
	 */
	public void clear (int len) {
		for (int i = 0; i < len; i++) {
			cb.get();
		}
	}

	/**
	 * Initialize a default {@link CyclicBufferAppender} with name CyclicBufferAppender.NAME 
	 */
	public static void initializeCloudConsoleAppender () {
        CyclicBufferAppender cba = new CyclicBufferAppender();
        cba.setName(CyclicBufferAppender.NAME);
        //cba.setLayout(new PatternLayout("%d{yy-MM-dd HH:mm} %c{1} [%p] %m%n"));
        Logger.getRootLogger().addAppender(cba);
	}
	
}
