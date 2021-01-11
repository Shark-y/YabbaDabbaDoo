package com.cloud.console;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Use this tool to log JSP messages/errors to the cloud console.
 * 
 * @author vsilva
 * @version 1.0.0
 *
 */
public class JSPLoggerTool {
	static final Logger log = LogManager.getLogger(JSPLoggerTool.class);
	
	/**
	 * A JSP log helper. Use this to log JSP errors into the cloud console.
	 * @param prefix JSP Name?
	 * @param message The message
	 */
	public static void JSP_LOGE(String prefix, Object message) {
		log.error(prefix + " " + message);
	}
	
	/**
	 * A JSP log helper. Use this to log JSP errors into the cloud console.
	 * @param prefix JSP Name?
	 * @param message The message
	 * @param ex An exception. It will be displayed in the log console by enabling the {@link HTTPServerTools} logger.
	 */
	public static void JSP_LOGE(String prefix, Object message, Throwable ex) {
		log.error(prefix + " " + message, ex);
	}

	/**
	 * A JSP log helper. Use this to log JSP debug messages into the cloud console.
	 * @param prefix JSP Name?
	 * @param message The message to log
	 */
	public static void JSP_LOGD(String prefix, Object message) {
		log.debug(prefix + " " + message);
	}

	/**
	 * A JSP log helper. Use this to log JSP warning messages into the cloud console.
	 * @param prefix JSP Name?
	 * @param message The message to log
	 */
	public static void JSP_LOGW(String prefix, Object message) {
		log.warn(prefix + " " + message);
	}

	/**
	 * A JSP log helper. Use this to log JSP debug messages into the cloud console.
	 * @param prefix JSP Name?
	 * @param message The message to log
	 */
	public static void JSP_LOGI(String prefix, Object message) {
		log.info(prefix + " " + message);
	}

}
