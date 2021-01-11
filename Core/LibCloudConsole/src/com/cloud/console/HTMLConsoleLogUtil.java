package com.cloud.console;


import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.rolling.FixedWindowRollingPolicy;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.apache.log4j.rolling.TimeBasedRollingPolicy;
import org.apache.log4j.spi.LoggingEvent;

import com.cloud.core.logging.L4JConfigurator;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;

/**
 * Log4J Cloud Console Log configuration helper tool.
 * 
 * <ul>
 * <li> Initialize the node log system: by size (RollingFileAppender) or date (DailyRollingFileAppender).
 * <li> See https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/DailyRollingFileAppender.html
 * <li> By size see https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/RollingFileAppender.html
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation.
 * @version 1.0.1 - 09/13/2017 Added tomcat 8 log support methods 
 * @version 1.0.2 - 10/19/2017 Replaced DailyRollingFileAppender due to synchronization issues and data loss with org.apache.log4j.rolling.RollingFileAppender.
 * @version 1.0.3 - 12/19/2017 Added a log4j layout to mask sections of a message using a regular expression.
 *
 */
public class HTMLConsoleLogUtil {

	/** See https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/DailyRollingFileAppender.html */
	public static final String ROLLOVER_TWICEADAY 	= "'.'yyyy-MM-dd-a";
	
	/** Rollover at the top of every hour (DailyRollingFileAppender). */
	public static final String ROLLOVER_HOURLY 		= "'.'yyyy-MM-dd-HH";

	/** Rollover at the first day of each week. (DailyRollingFileAppender). */
	public static final String ROLLOVER_WEEKLY 		= "'.'yyyy-ww";

	/** https://logging.apache.org/log4j/extras/apidocs/org/apache/log4j/rolling/RollingFileAppender.html */
	public static final String ROLLING_ONCEADAY 	= "%d.log"; 	// Default: yyyy-MM-dd

	/** https://logging.apache.org/log4j/extras/apidocs/org/apache/log4j/rolling/TimeBasedRollingPolicy.html */
	public static final String ROLLING_WEEKLY 		= "%d{yyyy-ww}.log";

	/** https://logging.apache.org/log4j/extras/apidocs/org/apache/log4j/rolling/TimeBasedRollingPolicy.html */
	public static final String ROLLING_ZIPPEDWEEKLY = "%d{yyyy-ww}.zip";

	static private void LOGD(String text) {
		System.out.println("[HTML-LOG] " + text);
	}

	@SuppressWarnings("unused")
	static private void LOGE(String text) {
		System.err.println("[HTML-LOG] " + text);
	}

	/**
	 * Log4j {@link PatternLayout} capable of masking sections of a message using a regular expression.
	 * 
	 * @author VSilva
	 * @see http://vozis.blogspot.com/2012/02/log4j-filter-to-mask-payment-card.html
	 */
	private static final class PatternLayoutWithMask extends PatternLayout {
		private final String mask;
		private final Pattern regexp;
		
		/**
		 * Construct.
		 * @param pattern The log4j pattern used to format the message.
		 * @param maskRegExp A regular expression used to match data for masking.
		 * @param mask The mask used to replace all matches by the regular expression.
		 */
		public PatternLayoutWithMask( final String pattern, final String maskRegExp, final String mask) {
			super(pattern);
			this.mask		= mask;
			this.regexp 	= maskRegExp != null && !maskRegExp.isEmpty() ? Pattern.compile(maskRegExp) : null;
		}
		
		public String format(LoggingEvent event) {
			if ( regexp == null || mask == null) {
				return super.format(event);
			}
			if (event.getMessage() instanceof String) {
				String message 	= event.getRenderedMessage();
				Matcher matcher = regexp.matcher(message);
				
				if (matcher.find()) {
					 String maskedMessage 		= matcher.replaceAll(mask);
					 Throwable throwable 		= event.getThrowableInformation() != null ? event.getThrowableInformation().getThrowable() : null;
					 LoggingEvent maskedEvent 	= new LoggingEvent(event.fqnOfCategoryClass
							 , Logger.getLogger(event.getLoggerName())
							 , event.timeStamp
							 , event.getLevel()
							 , maskedMessage
							 , throwable );
					 return super.format(maskedEvent);		 
				}
			}
			return super.format(event);
		};
	}

	/**
	 * Initialize the node logger using values from bootstrap.ini. A file will be created under ${CWD}/logs/{CONTEXT_ROOT}.log
	 * 
	 * @param baseName Name of the log file, usually the node name of web app context root.
	 * @throws IOException 
	 */
	public static void initializeNodeLogger(final String baseName) throws IOException {
    	Map<String, Object> params 	= new HashMap<String, Object>();
    	
    	// Node context root: CloudAdapetrNode00X
    	params.put(NodeConfiguration.KEY_CTX_PATH, baseName);

    	final NodeConfiguration config 	= new NodeConfiguration(params);
    	initializeNodeLogger(baseName, config);
	}
	
	/**
	 * Initialize a WebApp node logger using values from bootstrap.ini.
	 * A file will be created under ${CWD}/logs/{CONTEXT_ROOT}.log
	 * 
	 * @param baseName Name of the log file, usually the node name of the web app context root.
	 * @param config Server config {@link NodeConfiguration}.
	 * @throws IOException 
	 */
	public static void initializeNodeLogger(final String baseName, final NodeConfiguration config) throws IOException {
    	final String thresHold			= config.getLogThresHold();	
    	final String pattern			= config.getLogPattern();
    	final boolean logToConsole		= config.getLogConsole();
    	final String folder				= config.getLogFolder();
    	final String rotationPolicy		= config.getLogRotationPolicy();
    	
    	final String maskRegExp 		= config.getProperty(NodeConfiguration.KEY_LOG_MASK_REGEXP); // "(META_DNIS.)[0-9]{1,}"; 
    	final String mask 				= config.getProperty(NodeConfiguration.KEY_LOG_MASK);		// "$1x";   
    	
		final String basePath 			= folder + File.separator + baseName.replaceAll("/", ""); // + ".log";
    			
		Logger rootLogger 				= Logger.getRootLogger();
		
		LOGD("======================================== NODE LOGGER =========================================");
		LOGD("Node logger Folder    : " + folder);
		LOGD("Node logger Name      : " + baseName + " [" + basePath + "]");
		LOGD("Node logger Threshold : " + thresHold);
		LOGD("Node logger Pattern   : " + pattern);
		LOGD("Node logger Log2Cons  : " + logToConsole);
		LOGD("Root Logger Name      : " + rootLogger.getName() + " Level: " + rootLogger.getLevel());
		LOGD("Log Rotation Policy   : " + rotationPolicy);
		
		// if rotationPolicy begins with  '.' use DailyRollingFileAppender else use RollingFileAppender
		FileAppender appender 	= null;
		Layout layout			= new PatternLayoutWithMask(pattern, maskRegExp, mask);
		
		// Legacy DailyRollingFileAppender
		if ( rotationPolicy.startsWith("'.'")) {
			// By date: '.'yyyy-MM-dd-a" (twice a day),....
			LOGD("NodeLog: Using DATE rotation policy with " + rotationPolicy);
			appender 	= new DailyRollingFileAppender(layout , basePath  + ".log", rotationPolicy); 
		}
		// Legacy (by size) RollingFileAppender
		else if ( rotationPolicy.contains("MB") )  {
			// By size: 10MB, ... https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/RollingFileAppender.html
			LOGD("NodeLog: Using SIZE rotation policy with " + rotationPolicy);
			appender 	= new RollingFileAppender(layout , basePath  + ".log");
			((RollingFileAppender)appender).setMaxFileSize(rotationPolicy); 
		}
		// NEW (prevents data loss) Log4j extras org.apache.log4j.rolling.RollingFileAppender
		else if ( rotationPolicy.startsWith("%d")) {
			appender 						= new org.apache.log4j.rolling.RollingFileAppender();
			TimeBasedRollingPolicy policy 	= new TimeBasedRollingPolicy();
			
			// log/{NODENAME}.%d{yyyy-MM}.gz 
			final String fNamePattern		= basePath + "." + rotationPolicy;
			policy.setFileNamePattern(fNamePattern);
			//policy.activateOptions();
			
			LOGD("NodeLog: Using log4j.rolling.RollingFileAppender (TimeBasedRollingPolicy) with pattern " + fNamePattern);				
			((org.apache.log4j.rolling.RollingFileAppender)appender).setRollingPolicy(policy);
			appender.setLayout(layout);
			appender.activateOptions();
		}
		// NEW (by size) Log4j extras org.apache.log4j.rolling.RollingFileAppender
		else {
			appender 						= new org.apache.log4j.rolling.RollingFileAppender();
			FixedWindowRollingPolicy policy = new FixedWindowRollingPolicy();
			
			// logs/{NODENAME}.%i.log
			final String fNamePattern		= basePath + ".%i.log";
			policy.setFileNamePattern(fNamePattern);

			LOGD("NodeLog: Using log4j.rolling.RollingFileAppender (FixedWindowRollingPolicy/SizeBasedTriggeringPolicy) with pattern " + fNamePattern);
			((org.apache.log4j.rolling.RollingFileAppender)appender).setRollingPolicy(policy);

			// maxFileSize - rollover threshold size in bytes.
			((org.apache.log4j.rolling.RollingFileAppender)appender).setTriggeringPolicy(new SizeBasedTriggeringPolicy(Long.parseLong(rotationPolicy)));
			appender.setLayout(layout);
			appender.activateOptions();
		}
		
		appender.setAppend(false);
		appender.setThreshold(Level.toLevel(thresHold)); 
		appender.setName(baseName);
		
		// add a console appender? (only 1 allowed)
		if ( logToConsole ) {
			final String consoleName 	= "CLOUD_CONSOLE";
			ConsoleAppender console 	= new ConsoleAppender(layout);
			console.setName(consoleName);
			
			if ( rootLogger.getAppender(consoleName) == null) {
				rootLogger.addAppender(console);
				LOGD("NodeLog: Log2Consose is true. Added Console appender to the root logger.");
			}
			else {
				LOGD("NodeLog: Log2Consose is true. Console appender already exists. Unable to add.");
			}
			//rootLogger.setLevel(Level.DEBUG);
		}
		else {
			LOGD("NodeLog: Node logger Log2Console is OFF. Setting ALL current loggers to ERROR.");

			// Better for the console...
			L4JConfigurator.log4jSetCurrentLoggersLevel(Level.ERROR);
		} 
		
		// Add the daily appender to root logger. Remove it if already exists.
		if ( rootLogger.getAppender(baseName) != null ) {
			LOGD("NodeLog: Found existing appender " + baseName + ". Removing it!");
			Appender ap = rootLogger.getAppender(baseName);
			ap.close();
			rootLogger.removeAppender(baseName);
		}
		rootLogger.addAppender(appender);
		
		// Always set the ROOT logger level to WARN (So errors will display).
		LOGD("Setting Root Logger Level to WARN.");
		rootLogger.setLevel(Level.WARN);
		LOGD("==============================================================================================");
	}

	/**
	 * Reinitialize the node logger removing any existing appender. Invoke this method after the log configuration has changed and
	 * the node logger needs to be re-initialized.
	 * @throws IOException 
	 */
	static public void reinitializeNodeLogger () throws IOException {
		final NodeConfiguration cfg = CloudServices.getNodeConfig();
		final String baseName = cfg.getProperty(NodeConfiguration.KEY_CTX_PATH);
		
		// re-init node logger. Remove any previous appender
		initializeNodeLogger(baseName, cfg);
		dumpAllAppenders();
	}
	
	@SuppressWarnings("rawtypes")
	public static void dumpAllAppenders () {
		for  (Enumeration e = Logger.getRootLogger().getAllAppenders() ; e.hasMoreElements() ; ) {
			Appender a = (Appender) e.nextElement();
			LOGD("Log Appender " + a.getName() + " " + a.getClass().getCanonicalName());
		}
	}
	
	
	/**
	 * Shutdown the log system by closing all appenders for the ROOT logger.
	 * Invoke this when the web app is shutdown close file appenders, etc.
	 */
	@SuppressWarnings("rawtypes")
	public static void shutdownLogSystem () {
		// Close all appenders for the ROOT logger
		for  (Enumeration e = Logger.getRootLogger().getAllAppenders() ; e.hasMoreElements() ; ) {
			Appender a = (Appender) e.nextElement();
			LOGD("Closing ROOT logger appender " + a.getName() + " of type " + a.getClass().getCanonicalName());
			a.close();
		}
	}
	

}
