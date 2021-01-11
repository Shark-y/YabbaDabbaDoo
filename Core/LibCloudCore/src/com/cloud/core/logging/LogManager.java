package com.cloud.core.logging;

import org.apache.log4j.Logger;

/**
 * A class to centralize all logging into a single entry point. So it can be switched to other logging system.
 * <li> Log system: Log4J
 * <pre>
 * Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%d{yy-MM-dd HH:mm:ss} %-20c{1} [%-5p] %m%n")));
 * 
 * Logger L1 = LogManager.getLogger("foo");
 * Logger L2 = LogManager.getLogger("bar");
 * 
 * L1.info("Info message");
 * L1.debug("Debug message");
 * L1.error("Error message");
 * 
 * L2.info("Info message");
 * L2.debug("Debug message");
 * L2.error("Error message", new Exception("Error"));
 * </pre>
 * @author VSilva
 *
 */
public class LogManager {

	/** Format string used for formatting dates using SimpleDateFormat **/
	public static final String DATE_FORMAT = "YYYY-MM-dd HH:mm:ss:SSS";
	
	public static com.cloud.core.logging.Logger getLogger ( Class<?> clazz) {
		return new com.cloud.core.logging.Logger(Logger.getLogger(clazz));
	}
	
	public static com.cloud.core.logging.Logger getLogger ( String name) {
		return new com.cloud.core.logging.Logger(Logger.getLogger(name));
	}

}
