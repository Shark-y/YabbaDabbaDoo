package com.cloud.core.provider;

import java.io.IOException;

import com.cloud.core.services.ServiceStatus;

/** 
 * Reusable failover interface
 * @author VSilva
 *
 */
public interface IServiceFailover {

	/**
	 * Failover to a secondary server, URL, socket, etc.
	 * @return The result {@link ServiceStatus}.
	 */
	ServiceStatus onFailover () throws IOException ;
	
}
