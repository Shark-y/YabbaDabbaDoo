package com.cloud.core.services;

import java.util.List;

import com.cloud.cluster.IClusterInstance;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.services.ServiceDescriptor.ServiceType;

/**
 * The {@link ServiceContext} wraps parameter information sent to the {@link IMessageBroker} and/or {@link IContactCenter} services at startup.
 * @author VSilva
 *
 */
public class ServiceContext {
	/** Base path of the server config ${user.home}/.cloud/CloudAdapter */
	private String configurationBasePath;
	
	/** Base path of the connection profile for this node: ${user.home}/.cloud/CloudAdapter/Profiles/{NAME}	 */
	private String profileBasePath;

	private List<IServiceLifeCycle> services;
	
	private NodeConfiguration config;
	
	/** Cluster instance */
	private IClusterInstance cluster;
	
	/** Has the node been configured? */
	private boolean configured;
	
	/**
	 * Constructor: Creates a context object with adapter startup parameters.
	 * 
	 * @param configurationBasePath Base path of the server config ${user.home}/.cloud/CloudAdapter.
	 * @param profileBasePath Base path of the connection profile for this node: ${user.home}/.cloud/CloudAdapter/Profiles/{NAME}.
	 * @param messageBrokerDescriptor {@link ServiceDescriptor} configuration descriptor for the message broker (Live Person, etc).
	 * @param broker The {@link IMessageBroker} service.
	 * @param contactCenterDescriptor {@link ServiceDescriptor} configuration descriptor for the contact center (Genesys, etc).
	 * @param contactCenter The {@link IContactCenter} service.
	 * @param cluster The {@link IClusterInstance}.
	 * @param configured True if the node has been configured already, false otherwise.
	 */
	public ServiceContext(String configurationBasePath
			, String profileBasePath
			, List<IServiceLifeCycle> services
			, NodeConfiguration config
			, IClusterInstance cluster
			, boolean configured
			) 
	{
		super();
		this.configurationBasePath 	= configurationBasePath;
		this.profileBasePath 		= profileBasePath;
		this.cluster				= cluster;
		this.configured				= configured;
		this.services				= services;
		this.config					= config;
	}

	/**
	 * Base path of the server configuration: ${user.home}/.cloud/CloudAdapter.
	 * @return ${user.home}/.cloud/CloudAdapter
	 */
	public String getConfigurationBasePath() {
		return configurationBasePath;
	}

	public ServiceDescriptor findServiceDescriptor (ServiceType type) {
		return config.findServiceDescriptor(type);
	}
	
	/**
	 * Find a service {@link IServiceLifeCycle}.
	 * @param type See {@link ServiceType}.
	 * @return {@link IServiceLifeCycle}.
	 */
	public IServiceLifeCycle findService(ServiceType type) {
		for (IServiceLifeCycle service : services) {
			if ( service.getServiceType() == type ) {
				return service;
			}
		}
		return null;
	}

	public String getServiceConfigurationFile(ServiceType type) {
		ServiceDescriptor sd = findServiceDescriptor(type);
		return sd != null ? sd.getConfigFileName() : null;
	}
	
	/**
	 * Configuration file name of the {@link IMessageBroker} as configured in bootstrap.ini.
	 * @deprecated Kept for Adapter compatibility reasons only.
	 * @return File name (salesforce.ini, lp.ini, etc)
	 */
	public String getMessageBrokerConfigurationFile() {
		ServiceDescriptor desc =  getMessageBrokerDescriptor(); //config.findServiceDescriptor(ServiceType.MESSAGE_BROKER); 
		return desc != null ? desc.getConfigFileName() : null;
	}

	/**
	 * Configuration file name of the {@link IContactCenter} as configured in bootstrap.ini.
	 * @deprecated Kept for Adapter compatibility reasons only.
	 * @return File name (aes.ini, ucce.ini, etc)
	 */
	public String getContactCenterConfigurationFile() {
		ServiceDescriptor desc =  getContactCenterDescriptor(); //config.findServiceDescriptor(ServiceType.CONTACT_CENTER); 
		return desc != null ? desc.getConfigFileName() : null;
	}
	
	/**
	 * Get the {@link IMessageBroker} descriptor.
	 * @deprecated Kept for Adapter compatibility reasons only.
	 * @return See {@link ServiceDescriptor} for details.
	 */
	public ServiceDescriptor getMessageBrokerDescriptor() {
		return config.findServiceDescriptor(ServiceType.MESSAGE_BROKER); // getMessageBrokerDescriptor();
	}

	/**
	 * Get the {@link IContactCenter} descriptor.
	 * @deprecated Kept for Adapter compatibility reasons only.
	 * @return See {@link ServiceDescriptor} for details.
	 */
	public ServiceDescriptor getContactCenterDescriptor() {
		return config.findServiceDescriptor(ServiceType.CONTACT_CENTER); // getContactCenterDescriptor();
	}

	/**
	 * Base path of the connection profile for this node: ${user.home}/.cloud/CloudAdapter/Profiles/{NAME}.
	 * <li> A connection profile is required for the node to start.
	 * <li> The profile name is stored in bootstrap.ini.
	 * @return ${user.home}/.cloud/CloudAdapter/Profiles/{NAME}
	 */
	public String getProfileBasePath() {
		return profileBasePath;
	}
	
	
	/**
	 * Cluster operations object.
	 * @return See {@link IClusterInstance}.
	 */
	public IClusterInstance getClusterInstance() {
		return cluster;
	}
	
	/**
	 * Is the node configured? A node is configured if:
	 * <li> The sys-admin password has been set in bootstrap.ini.
	 * @return True if the node is configured.
	 */
	public boolean isNodeConfigured() {
		return configured;
	}
}
