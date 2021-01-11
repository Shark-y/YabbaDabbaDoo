package com.cloud.core.logging;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;

/**
 * A class to configure Log4J for multi-file logging.
 * <ul>
 * <li> Read all *.logger files in the base path.
 * <li> Merge the files into a single configuration.
 * </ul>
 * 
 * @version 1.0.2
 * @version 1.0.3 - 9/4/2017 The merge l4j properties logic has been deprecated.
 */
public class L4JConfigurator {

	/** Full path to the logger file (contains all saved loggers) @ ${java.io.tmpdir}/log4j.properties */
	public static final String LOGGER_FILE = CoreTypes.TMP_DIR + File.separator + "log4j.properties";
	
	private static void LOGD(String text) {
		System.out.println("[LOGGER-DBG] " + text);
	}

	private static void LOGE(String text) {
		System.out.println("[LOGGER-ERR] " + text);
	}

	/**
	 * A class that implements the Java FileFilter interface.
	 */
	private static class LoggerFileFilter implements FileFilter {
		private final String[] okFileExtensions = new String[] { "loggers"};

		public boolean accept(File file) {
			for (String extension : okFileExtensions) {
				if (file.getName().toLowerCase().endsWith(extension)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Turn on/off log4j debugging.
	 * @param debug
	 */
	public static void log4jTurnDebugging(boolean debug) {
		System.setProperty("log4j.debug", String.valueOf(debug));
	}

	/**
	 * Disable the log4j root logger (sets the level to OFF).
	 * Note: Nothing will display (not even errors).
	 * @see http://logging.apache.org/log4j/1.2/manual.html
	 */
	public static void log4jTurnOffRootLogger() {
		// If the ROOT logger is OFF. Nothing will display (including errors).
		// Must Set it to WARN so errors will display.
		LOGD("Setting Root Logger level to OFF.");
		Logger.getRootLogger().setLevel(Level.OFF);
	}
	
	/**
	 * Disable the default log4j initialization procedure.
	 * @see http://logging.apache.org/log4j/1.2/manual.html
	 */
	public static void log4jDisableDefaultInitialization () {
		// skip the default initialization procedure
		System.setProperty("log4j.defaultInitOverride", "true");
	}
	
	private static Properties mergePropertyFiles ( File[] files ) {
		Properties merged = new Properties();
		
		for (File file : files) {
			LOGD(file.toString());
			try {
				FileInputStream fis = new FileInputStream(file);
				merged.load(fis);
				fis.close();
				fis = null;
			} catch (Exception e) {
				LOGE("Merge Properties: " + e.toString());
			}
		}
		return merged;
	} 
	
	/**
	 * Configure Log4J for multi-file logging.
	 * @deprecated DEPRECATED 9/4/2017. This logic has been removed.
	 * <ul>
	 * <li> Read all *.logger files in the base path.
	 * <li> Merge the files into a single configuration.
	 * <li> Free the nerge to the log4j {@link PropertyConfigurator}.
	 * </ul>
	 * @param rootPath Base path that contains logging files (*.logger).
	 */
	public static void configure (String rootPath) {
		File dir 			= new File(rootPath);
		File[] files 		= dir.listFiles(new LoggerFileFilter());

		if ( files == null) {
			LOGD(L4JConfigurator.class.getName() + " No *.logger files @ " + rootPath);
			return;
		}
		Properties merged 	= mergePropertyFiles(files);
		PropertyConfigurator.configure(merged);
	}

	/**
	 * Set the level for all current loggers to OFF. No logging at all. Not even errors.
	 * Note: use with caution. It will disable all log output (including errors).
	 * <b>Disabling error messages is a bad idea.</b> (You don't know what is going on).
	 */
	public static void log4jDisableCurrentLoggers() {
		log4jSetCurrentLoggersLevel(Level.OFF);
	}
	
	/**
	 * Set the level for all current loggers taken from LogManager.getCurrentLoggers
	 * @param level Log4j level: DEBUG, WARN, etc.
	 * @since 1.0.2
	 */
	@SuppressWarnings("unchecked")
	public static void log4jSetCurrentLoggersLevel(Level level) {
		Enumeration<Logger> loggers = LogManager.getCurrentLoggers();
		
		while (loggers.hasMoreElements()) {
			Logger logger = loggers.nextElement();
			LOGD("SET LEVEL " + logger.getName() + " = " + level);
			logger.setLevel(level);
		}
	}
	
	/**
	 * Get default log4j saved loggers from  @ ${java.io.tmpdir}/log4j.properties
	 * @return Properties from  @ ${java.io.tmpdir}/log4j.properties
	 */
	public static Properties log4jGetDefaultLoggers () {
		return getSavedLoggers(LOGGER_FILE);
	}
	
	/**
	 * Get the saved loggers from a given path.
	 * @param path Full path of the log4j.loggers file.
	 * @return Properties <pre>
	 * com.x.y = DEBUG
	 * com.x.z = INFO
	 * </pre>
	 */
	public static Properties getSavedLoggers (String path) {
		 Properties props 	= new Properties();
		 FileInputStream in	= null;
		 try {
			in = new FileInputStream(path);
			if ( in != null)
				props.load(in);
		 }
		 catch (Exception e) { }
		 finally { IOTools.closeStream(in);}
		 return props;
	}

	/**
	 * Initialize the default logging system (log4j) by reading a logger file from
	 * <li>${java.io.tmpdir}/log4j.properties
	 * <li>Loading the logger and setting its level.
	 * The format of the logger file is: <pre>
	 * com.foo = DEBUG
	 * x.y.z = INFO
	 * </pre>
	 */
	public static void initializeLoggingSystem() {
		LOGD("Initalizing the logging system from " + LOGGER_FILE);
		
		Properties loggers = log4jGetDefaultLoggers();
		Set<Map.Entry<Object, Object>> entries =  loggers.entrySet();
		
		for (Map.Entry<Object, Object> entry : entries) {
			String logger 	= entry.getKey().toString();
			String level	= entry.getValue().toString();
			
			LOGD(logger + " = " + level);
			Logger.getLogger(logger).setLevel(Level.toLevel(level));
		}
	}
}
