package com.cloud.core.logging;

/**
 * A simple class used to wrap a Log4J logger class.
 * This is used to centralize all logging in one place so it can be easily switched
 * (log4j 2 for example) if desired.
 * @author VSilva
 *
 */
public class Logger {

	private org.apache.log4j.Logger logger;

	public Logger(org.apache.log4j.Logger logger) {
		this.logger = logger;
	}

	public void info (Object message) {
		logger.info(message);
	}

	public void warn (Object message) {
		logger.warn(message);
	}

	public void debug (Object message) {
		logger.debug(message);
	}

	public void trace (Object message) {
		logger.trace(message);
	}

	public void trace (Object message, Throwable t) {
		logger.trace(message, t);
	}

	public void error (Object message) {
		logger.error(message);
	}

	public void error (Object message, Throwable t) {
		logger.error(message, t);
	}
	
	public void fatal (Object message) {
		logger.fatal(message);
	}

	public void fatal (Object message, Throwable t) {
		logger.fatal(message,t);
	}

}
