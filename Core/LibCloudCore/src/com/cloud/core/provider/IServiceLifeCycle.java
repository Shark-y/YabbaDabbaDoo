package com.cloud.core.provider;

import java.io.IOException;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceDescriptor.ServiceType;

/**
 * Implement this to provide life cycle handlers for your service.
 * @author VSilva
 *
 */
public interface IServiceLifeCycle {

	/**
	 * Fires once when the container creates a service instance.
	 * @param params
	 * @throws IOException
	 */
	void onServiceInitialize(ServiceContext context) throws IOException;

	/**
	 * Fires multiple times when the cloud service is started.
	 * @param params Service startup parameters. See {@link ServiceContext}.
	 */
	void onServiceStart(ServiceContext context) throws IOException;
	
	/**
	 * Fires multiple times when the service is stopped.
	 */
	void onServiceStop();

	/**
	 * Fires once when the container stops
	 */
	void onServiceDestroy();

	/**
	 * Get the {@link ServiceStatus}.
	 * @return {@link ServiceStatus}.
	 */
	public ServiceStatus getServiceStatus() ;
	
	/**
	 * Validate the service {@link IConfiguration}.
	 * @param config
	 * @throws Exception
	 */
	public void onServiceValidateConfig (IConfiguration config) throws Exception;
	
	/**
	 * Get The type of service
	 * @return
	 */
	public ServiceType getServiceType() ;
}
