package com.cloud.core.provider;

import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.config.IConfiguration;

/**
 * This interface has been created to implement missing methods from {@link IServiceLifeCycle}
 * without interfering with current implementations:
 * <h2>Implemented Methods</h2>
 * <ul>
 * <li> onServiceSaveConfig: Fires when the service is saved from the cloud console. 
 * </ul> 
 * @author vsilva
 * @version 1.0.0 - Initial implementation.
 * @version 1.0.1 - 9/27/2017 New method String getConfigProperty(key) to get a configuration value.
 *
 */
public interface IServiceLifeCycleV2 extends IServiceLifeCycle {

	/**
	 * Fires when the service {@link IConfiguration} is saved.
	 * @param config service {@link IConfiguration}.
	 * @throws Exception On any kind of error.
	 * @since 1.0.0
	 */
	public void onServiceSaveConfig (IConfiguration config) throws Exception;

	/**
	 * Get a value from the service configuration.
	 * @param key Configuration key.
	 * @return Configuration value
	 * @since 1.0.1
	 */
	public String getConfigProperty( String key) ;
}
