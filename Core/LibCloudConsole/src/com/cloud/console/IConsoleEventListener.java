package com.cloud.console;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.services.NodeConfiguration;

/**
 * Interface to listen for cloud console events such as:
 * <ul>
 * <li> Start
 * <li> Stop
 * <li> Node configuration saved.
 * <li> Service configuration saved.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public interface IConsoleEventListener {

	/**
	 * Fires when the start link in the console is clicked.
	 */
	public void onConsoleStart();
	
	/**
	 * Fires when the stop link in the console is clicked.
	 */
	public void onConsoleStop();
	
	/**
	 * Fires the the node configuration is saved.
	 * @param config The new {@link NodeConfiguration}.
	 */
	public void onNodeConfugurationSaved(NodeConfiguration config);
	
	/**
	 * Fires when the service configuration is saved.
	 * @param config The new {@link IConfiguration}.
	 */
	public void onServiceConfugurationSaved(IConfiguration config);
}
