package com.web.console;

import java.io.IOException;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.provider.IServiceLifeCycleV2;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.services.ServiceStatus.Status;

public class ConsoleDaemon implements IServiceLifeCycleV2 {

	private static final Logger log 	= LogManager.getLogger(ConsoleDaemon.class);
	
	private ServiceStatus status = new ServiceStatus();
	
	/** service config fromcluster_mgr.xml */
	private ServiceConfiguration cfg;
	
	@Override
	public void onServiceInitialize(ServiceContext context) throws IOException {
		try {
			/* 3/29/2020 Don't auto save cfg
			ServiceDescriptor[] svcUpdated		= new ServiceDescriptor[1];
			int[] cfgVersions					= new int[2];
			final boolean isServiceCfgUpdated	= CloudServices.serviceUpdateDetected(svcUpdated, cfgVersions);
			*/
			cfg =  CloudServices.getServiceConfig(ServiceType.DAEMON);
			
			// save default DAEMON config
			if ( !CloudServices.isConfigured()) {
				log.info("Saving default service config " + cfg );
				cfg.save();
			}
			/*
			if ( isServiceCfgUpdated) {
				log.debug("Service update detected. Default save " + cfg );
				cfg.save();
			} */
			
		} catch (Exception e) {
			log.error("CloudProfiler service init.", e);
			throw new IOException(e);
		}
		
	}

	@Override
	public void onServiceStart(ServiceContext context) throws IOException {
		status.setStatus(Status.ON_LINE, "Up");
	}

	@Override
	public void onServiceStop() {
		status.setStatus(Status.OFF_LINE, "Down");
	}

	@Override
	public void onServiceDestroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServiceStatus getServiceStatus() {
		return status;
	}

	@Override
	public void onServiceValidateConfig(IConfiguration config) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServiceType getServiceType() {
		return ServiceType.DAEMON;
	}

	@Override
	public void onServiceSaveConfig(IConfiguration config) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getConfigProperty(String key) {
		return cfg.getProperty(key);
	}

	/**
	 * Get hub search end points.
	 * @return Name1:URL1,Name2:URL2,...
	 */
	public String getHubSearchUrls () {
		return getConfigProperty("k01_00");
	}
	
}
