package com.cloud.core.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.Container;
import com.cloud.core.io.FileTool;
import com.cloud.core.types.CoreTypes;


/**
 * Web Container helper tools
 * 
 * @author VSilva
 * @version 1.0.0
 */
public class Container {

	/** Base path of the Tomcat container (catalina.base) or null if not running in Tomcat  */
	public static final String TOMCAT_BASE_PATH				= System.getProperty("catalina.base");

	/** Base path of the Tomcat container (catalina.home) or null if not running in Tomcat  */
	public static final String TOMCAT_HOME_PATH				= System.getProperty("catalina.home");
	
	/** True if running within a Tomcat container */
	public static final boolean IS_CONTAINER_TOMCAT			= TOMCAT_BASE_PATH != null;

	/** Default name of the container logs folder */
	public static final String DEFAULT_LOGS_FOLDER_NAME 	= "logs";

	/** Tomcat date boundary regexp Jul 21, 2016 10:46:28 AM */
	static final String JULI_BOUNDARY_PATTERN = "(?<DATE>\\w+\\s\\d+,\\s\\d{4}\\s\\d+:\\d{2}:\\d{2}\\s\\w+)";

	/** Tomcat Event log pattern: Jul 21, 2016 10:46:28 AM org.apache.coyote.AbstractProtocol init ... */
	static final String JULI_PATTERN = JULI_BOUNDARY_PATTERN + "\\s(?<SOURCE>.*?\\s\\w+).(?<LEVEL>\\w+):\\s(?<MSG>.*)";

	/** Log4j boundary Date: 16-07-21 10:48:54 */
	public static final String LOG4J_BOUNDARY_PATTERN = "(?<DATE>\\d{2}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})";

	/** Matches Log4j Event: 16-07-21 10:48:54 CloudServices          [DEBUG] IsNodeConfigured: Checking if a sys admin pwd has been setup. */
	public static final String LOG4J_PATTERN = LOG4J_BOUNDARY_PATTERN + "\\s(?<SOURCE>.*?)\\s+\\[(?<LEVEL>.*?)\\]\\s(?<MSG>.*)";

	/** Matches: 16-09-10 15:36:50 L4JAudit [INFO ] ADMIN_CONSOLE: Message */
	static final String AUDIT_PATTERN = LOG4J_BOUNDARY_PATTERN + "\\s.*?\\s+\\[(?<LEVEL>.*?)\\]\\s(?<SOURCE>\\w+):\\s*(?<MSG>.*)";
	//static final String AUDIT_PATTERN = LOG4J_BOUNDARY_PATTERN + "\\s.*?\\s+\\[(?<LEVEL>\\w+)\\s*\\]\\s(?<SOURCE>\\w+):\\s*(?<MSG>.*)";

	/** Matches TC8 08-Aug-2017 13:51:03.959  NOTE: Tomcat 8 see https://tomcat.apache.org/tomcat-8.5-doc/api/org/apache/juli/OneLineFormatter.html */
	public static final String TC8_BOUNDARY_PATTERN 	= "(?<DATE>\\d{2}-\\w+-\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})";

	/** Matches  INFO [localhost-startStop-1] Message */ 
	static final String TC8_PATTERN 			= TC8_BOUNDARY_PATTERN + "\\s(?<LEVEL>.*?)\\s+\\[(?<SOURCE>.*?)\\]\\s(?<MSG>.*)";

	/**
	 * Try to guess the default container log folder by looking at:
	 * <ul>
	 * <li> If catalina.bhome exists the use (catalina.home)/logs
	 * <li> If catalina.base exists the use (catalina.base)/logs
	 * <li> if none exists return 'logs'.
	 * </ul>
	 * @return Full path to the default container log folder or logs if unable to find a container.
	 */
	public static String getDefautContainerLogFolder () {
		if ( TOMCAT_HOME_PATH != null ) return TOMCAT_HOME_PATH + File.separator + "logs";
		if ( TOMCAT_BASE_PATH != null ) return TOMCAT_BASE_PATH + File.separator + "logs";
		return "logs";	// defaults to the logs folder in the CWD
	}

	/**
	 * Look for a global container log file catalina.YYY-MM-DD.log
	 * @return The file name if found for tomcat 7/8: $CATALINA_HOME/logs/catalina.YYY-MM-DD.log or NULL if not found.
	 */
	public static String getGlobalContainerLog () {
		if ( !Container.IS_CONTAINER_TOMCAT) {
			return null;
		}
		// catalina.YYY-MM-DD.log
		final SimpleDateFormat fmt 	= new SimpleDateFormat("yyyy-MM-dd");
		final String name = "catalina." + fmt.format(new Date()) + ".log";
		final String path = Container.TOMCAT_HOME_PATH + File.separator + DEFAULT_LOGS_FOLDER_NAME + File.separator + name;
		if ( FileTool.fileExists(path) ) return name;
		return null;
	}
	
	/**
	 * Look for a container error log: tomcat7-stderr.YYY-MM-DD.log or tomcat8-stderr.YYY-MM-DD.log
	 * Note: the folder searched is $CATALINA_HOME/logs
	 * @return The full path to the specified std error log or NULL if not found.
	 */
	public static String getContainerStdErrLog () {
		if ( !Container.IS_CONTAINER_TOMCAT) {
			return null;
		}
		// TC7: tomcat7-stderr.YYY-MM-DD.log
		// TC8: tomcat7-stderr.YYY-MM-DD.log
		final SimpleDateFormat fmt 	= new SimpleDateFormat("yyyy-MM-dd");
		final String name1 = "tomcat7-stderr." + fmt.format(new Date()) + ".log";
		final String name2 = "tomcat8-stderr." + fmt.format(new Date()) + ".log";
		final String path1 = Container.TOMCAT_HOME_PATH + File.separator + DEFAULT_LOGS_FOLDER_NAME + File.separator + name1;
		final String path2 = Container.TOMCAT_HOME_PATH + File.separator + DEFAULT_LOGS_FOLDER_NAME + File.separator + name2;
		
		if ( FileTool.fileExists(path1) ) return name1;
		if ( FileTool.fileExists(path2) ) return name2;
		return null;
	}

	/**
	 * Filter (extract) data from a log file given a string filter (search key)
	 * @param path Full path to the log file.
	 * @param filter Search key. For example: Exception - to get errors.
	 * @return JSON object for logging events in data tables format: {data: [[DATE, SOURCE< LEVEL, MSG]...]}
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject filterLogFile (final String path, final String filter) throws IOException, JSONException {
		// Guess the log type: Tomcat JULI vs C1AS LOG4J or C1AS AUDIT.
		// Linux Bug? Path may contain tomcat (/var/log/tomcat) which will cause this to fail (return 0 results)
		final String fileName	= FileTool.getFileName(path);
		boolean isJuliStyle 	= ( /*path*/fileName.contains("catalina") || /*path*/fileName.contains("tomcat") || fileName.contains("localhost"));
		
		JSONObject root = !isJuliStyle 
				? ( path.contains("AUDIT") 
						? convertMultiLineLog(path, LOG4J_BOUNDARY_PATTERN, AUDIT_PATTERN, filter) 
						: convertMultiLineLog(path, LOG4J_BOUNDARY_PATTERN, LOG4J_PATTERN, filter) 
						)
				: convertMultiLineLog(path, JULI_BOUNDARY_PATTERN, JULI_PATTERN, filter);
		
		// No data? try tomcat 8...
		if ( isJuliStyle ) {
			JSONArray data = root.optJSONArray("data");
			
			if ( data.length() == 0 ) {
				root = convertMultiLineLog(path, TC8_BOUNDARY_PATTERN, TC8_PATTERN, filter);
			}
		}
		return root;
	}

	/**
	 * Parse a log file using:
	 * <ul>
	 * <li>The default node log4j pattern: %d{yy-MM-dd HH:mm:ss} %-22c{1} [%-5p] %m%n
	 * <li>The Tomcat container log pattern: Jul 21, 2016 10:46:28 AM [SOURCE] [LEVEL] [MESSAGE]
	 * <li>This method cannot parser large log files (200MB+): java.lang.OutOfMemoryError: Java heap space
	 * </ul>
	 * <B>NOTE: For large files use streamLog instead</B>
	 * @param path Full path to the log file to be converted.
	 * @param boundaryPattern RegExp used to define a log event boundary,
	 * @param eventPattern RegExp used to parse the log event (including the boundary).
	 * @param filter (Optional) A very simple keyword filter. Used to fetch messages that contain the filter. If null all events will be returned.
	 *  
	 * @return A JSON representation of the log events: <pre>
	 * {"data": [
  [
    "16-07-21 10:47:00",
    "NodeConfiguration",
    "WARN ",
    "Get cluster params: Missing node URL KEY_CTX_URL"
  ],...
]}</pre>
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject convertMultiLineLog (final String path, final String boundaryPattern, final String eventPattern, final String filter) 
			throws IOException, JSONException 
	{
		JSONObject root 	= new JSONObject();
		JSONArray matrix	= new JSONArray();
		//  FindBugs 11/29/16 Found reliance on default encoding in com.cloud.console.servlet.LogServlet.convertMultiLineLog(String, String, String): new java.io.FileReader(String)
		BufferedReader br 	= new BufferedReader(new InputStreamReader(new FileInputStream(path), CoreTypes.ENCODING_UTF8));
		String line 		= null;

		// Boundary pattern: Jul 21, 2016 10:46:28 AM
	    Pattern p1 			= Pattern.compile(boundaryPattern);
	    int dateMatchCount 	= 0;

    	StringBuffer event 	= new StringBuffer();

    	// Event pattern
    	Pattern evtPattern 	= Pattern.compile(eventPattern, Pattern.DOTALL);
    			
	    while ((line = br.readLine()) != null) {
	    	Matcher boundary = p1.matcher(line);
	    	
	    	if (boundary.find()) {
	    		dateMatchCount++;
	    	}
	    	
	    	if ( dateMatchCount > 0) {
	    		if ( event.length() > 0) {
	    		   	// Jul 21, 2016 10:46:28 AM org.apache.coyote.AbstractProtocol init
	    			JSONArray row = extractEvent(event.toString(), evtPattern); 
	    			
	    			if ( row != null ) {
	    				postProcessEvent(row, eventPattern);
	    				
	    				// Optional very simple keyword filter
	    				final String message = row.getString(3);
	    				
	    				// 6/14/2018 RegExp support - if ( filter == null || ( message.contains(filter)) ) {
	    				if ( filter == null || ( filter.startsWith("RegExp:") ? message.matches(filter.replace("RegExp:", "")) : message.contains(filter) ) ) {
	    					matrix.put(row);
	    				}
	    			}
	    		}
	    		event = new StringBuffer();
	    		event.append( line);
	    		dateMatchCount = 0;
	    	}
	    	else {
	    		event.append("\n" + line);
	    	}
	    }
	    // extract the last event
		JSONArray row = extractEvent(event.toString(), evtPattern); 
		
		if ( row != null ) {
			postProcessEvent(row, eventPattern);
			
			final String message = row.getString(3);
			
			// 6/14/2018 RegExp support - if ( filter == null || ( message.contains(filter)) ) {
			if ( filter == null || ( filter.startsWith("RegExp:") ? message.matches(filter.replace("RegExp:", "")) : message.contains(filter) ) ) {
				matrix.put(row);
			}
		}
	    
		br.close();
		root.put("data", matrix);
		return root;
	}

	/**
	 * Buffer receiver for Container.streamLog large log parser.
	 * @author VSilva
	 *
	 */
	public static interface ILogStreamListener {
		void onBuffer (final JSONArray buf, int total) throws JSONException, IOException;
	}
	
	/**
	 * Use this one to parse a large log files (200MB+).
	 * @param path Full path to the log file to be converted.
	 * @param boundaryPattern RegExp used to define a log event boundary,
	 * @param eventPattern RegExp used to parse the log event (including the boundary).
	 * @param filter (Optional) A very simple keyword filter. Used to fetch messages that contain the filter. If null all events will be returned.
	 *  
	 * @return A JSON representation of the log events: <pre>
	 * {"data": [
  [
    "16-07-21 10:47:00",
    "NodeConfiguration",
    "WARN ",
    "Get cluster params: Missing node URL KEY_CTX_URL"
  ],...
]}</pre>
	 * @param bufferSize Desired buffer size.
	 * @param listener Buffer reciever. See {@link ILogStreamListener}
	 * @throws IOException
	 * @throws JSONException
	 */
	public static void streamLog (final String path, final String boundaryPattern, final String eventPattern, final String filter, int bufferSize,  ILogStreamListener listener) 
			throws IOException, JSONException 
	{
		JSONArray matrix	= new JSONArray();
		BufferedReader br 	= new BufferedReader(new InputStreamReader(new FileInputStream(path), CoreTypes.ENCODING_UTF8));
		String line 		= null;

		// Boundary pattern: Jul 21, 2016 10:46:28 AM
	    Pattern p1 			= Pattern.compile(boundaryPattern);
	    int dateMatchCount 	= 0;

    	StringBuffer event 	= new StringBuffer();

    	// Event pattern
    	Pattern evtPattern 	= Pattern.compile(eventPattern, Pattern.DOTALL);
    	int count			= 0;
    	
	    while ((line = br.readLine()) != null) {
	    	Matcher boundary = p1.matcher(line);
	    	
	    	if (boundary.find()) {
	    		dateMatchCount++;
	    	}
	    	
	    	if ( dateMatchCount > 0) {
	    		if ( event.length() > 0) {
	    		   	// Jul 21, 2016 10:46:28 AM org.apache.coyote.AbstractProtocol init
	    			JSONArray row = extractEvent(event.toString(), evtPattern); 
	    			
	    			if ( row != null ) {
	    				postProcessEvent(row, eventPattern);
	    				
	    				// Optional very simple keyword filter
	    				final String message = row.getString(3);
	    				
	    				// 6/14/2018 RegExp support - if ( filter == null || ( message.contains(filter)) ) {
	    				if ( filter == null || ( filter.startsWith("RegExp:") ? message.matches(filter.replace("RegExp:", "")) : message.contains(filter) ) ) {
	    					matrix.put(row);
	    				}
	    				if ( matrix.length() >= bufferSize) {
    						// send
    						count += matrix.length();
							listener.onBuffer(matrix, count);
							
							// clear
	    					matrix = new JSONArray();
	    				}
	    			}
	    		}
	    		event = new StringBuffer();
	    		event.append( line);
	    		dateMatchCount = 0;
	    	}
	    	else {
	    		event.append("\n" + line);
	    	}
	    }
	    // extract the last event
		JSONArray row = extractEvent(event.toString(), evtPattern); 
		
		if ( row != null ) {
			postProcessEvent(row, eventPattern);
			
			final String message = row.getString(3);
			
			// 6/14/2018 RegExp support - if ( filter == null || ( message.contains(filter)) ) {
			if ( filter == null || ( filter.startsWith("RegExp:") ? message.matches(filter.replace("RegExp:", "")) : message.contains(filter) ) ) {
				matrix.put(row);
				// send
				count += matrix.length();
				listener.onBuffer(matrix, count);
			}
		}
	    
		br.close();
	}

	/**
	 * Extract event information: date, source, log level, message from the event using regular expressions.
	 * @param event Event multi-line string.
	 * @param pattern regexp pattern to use for extraction.
	 * @return Event serialized as JSON. Format: [ "DATE", "SOURCE", "LEVEL", "MESSAGE"]
	 */
	static JSONArray extractEvent (String event, Pattern pattern) {
    	JSONArray row 	= new JSONArray();
    	Matcher m 		= pattern.matcher(event);
      	if (m.find()) {
       		row.put(m.group("DATE"));	// 1 date
    		row.put(m.group("SOURCE"));	// 2 source
    		row.put(m.group("LEVEL"));	// 3 log level
    		row.put(m.group("MSG"));	// 4 message
     		return row;
    	}
      	return null;
	}

	/**
	 * Massage information on an event after extraction.
	 * @param event Audit/log event.
	 * @param eventPattern Pattern used to match it.
	 */
	static void postProcessEvent (JSONArray event, String eventPattern) {
		try {
			// special case (AUDIT) event: replace ERROR levels w/ DANGER.
			if ( eventPattern.equals(AUDIT_PATTERN)) {
				if ( event.getString(2).contains("ERROR")) {
					event.put(2, "DANGER");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
