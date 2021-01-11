package com.cloud.console;

import java.util.ArrayList;
import java.util.List;

import com.cloud.console.IConsoleEventListener;
import com.cloud.core.config.IConfiguration;
import com.cloud.core.services.NodeConfiguration;

/**
 * A singleton controller for the cloud console.
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class CloudConsole {

	/** singleton instance */
	private static final CloudConsole INSTANCE = new CloudConsole();
	
	/** List of event listeners */
	private final List<IConsoleEventListener> listeners = new ArrayList<IConsoleEventListener>();
	
	public static CloudConsole getInstance() {
		return INSTANCE;
	}
	
	private CloudConsole() {
	}
	
	/**
	 * Add a console lifecycle lister.
	 * @param listener
	 */
	public void addLifecycleListener (IConsoleEventListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Invoked by the console when the start link is clicked.
	 */
	public void consoleStarted () {
		for ( IConsoleEventListener l : listeners) {
			l.onConsoleStart();
		}
	}

	/**
	 * Invoked by the console when the stop link is clicked.
	 */
	public void consoleStoped() {
		for ( IConsoleEventListener l : listeners) {
			l.onConsoleStop();
		}
	}

	/**
	 * Invoked by the console when the {@link NodeConfiguration} is saved.
	 * @param config The new {@link NodeConfiguration}.
	 */
	public void nodeConfigSaved (NodeConfiguration config) {
		for ( IConsoleEventListener l : listeners) {
			l.onNodeConfugurationSaved(config);
		}
	}

	/**
	 * Invoked by the console service configuration is saved.
	 * @param config The new service {@link IConfiguration}.
	 */
	public void serviceConfigSaved (IConfiguration config) {
		for ( IConsoleEventListener l : listeners) {
			l.onServiceConfugurationSaved(config);
		}
	}

}
