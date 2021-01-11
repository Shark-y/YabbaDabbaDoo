package com.web.console;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.cloud.console.HTMLConsoleLogUtil;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;

/**
 * Application Lifecycle Listener implementation class ClusterManagerWebListener
 *
 */
@WebListener
public class ConsoleWebListener implements ServletContextListener {

	private static final Logger log 		= LogManager.getLogger(ConsoleWebListener.class);
	
	private static void LOGD(String text) {
		System.out.println("[CLUSTER-MGR] " + text);
	}

	private static void LOGE(String text) {
		System.err.println("[CLUSTER-MGR] " + text);
	}

    /**
     * Default constructor. 
     */
    public ConsoleWebListener() {
    }

	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent ctx)  { 
    	// Creates a node logger under the file system $CWD/logs/<CONTEXT_PATH>.log
        try {
        	LOGD(">> Initializing node logger.");
        	NodeConfiguration config1 	= new NodeConfiguration(false, false);
        	HTMLConsoleLogUtil.initializeNodeLogger(ctx.getServletContext().getContextPath(), config1);
        	
        	Map<String, Object> params = new HashMap<String, Object>();
        	
        	// Node context root: CloudAdapetrNode00X
        	params.put(NodeConfiguration.KEY_CTX_PATH, ctx.getServletContext().getContextPath());
        	
        	LOGD(">> Initializing cluster manager.");
        	CloudConsole.initialize(params);
        	
        	/**
        	 *  Save global info into the application context.
        	 *  This info can be shared among servlets, JSPs, etc.
        	 */
        	NodeConfiguration config 	= CloudConsole.getNodeConfig();
        	
        	if (config == null) {
        		throw new Exception("Unable to load a server configuration for CtxRoot " + ctx.getServletContext().getContextPath());
        	}
        	
        	// Base path of the configuration available to other servlets/JSPs
        	LOGD(">> Configuration location: " + config.getConfigLocation());
        	
        	ctx.getServletContext().setAttribute(CloudServices.CTX_CONFIG_LOCATION, config.getConfigLocation());
        	
        	// for login.jsp
        	ctx.getServletContext().setAttribute(NodeConfiguration.SKEY_CFG_SERVER, config);
        	
		} catch (Exception e) {
			// this should NEVER happen!
			LOGD("FATAL: Failed to initialize cluster manager.");
			e.printStackTrace();

			// save the error on session for the admin console
			ctx.getServletContext().setAttribute(CloudServices.CTX_STARTUP_EXCEPTION, e);
    		
    		// for the admin console log view
			log.error("Fatal startup error", e);
		}
    	
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent ctx)  { 
    	CloudConsole.destroy();
    }
	
}
