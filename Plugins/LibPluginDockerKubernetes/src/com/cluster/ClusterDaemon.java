package com.cluster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.cloud.cluster.CloudCluster;
import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.provider.IServiceLifeCycleV2;
import com.cloud.core.services.CloudFailOverService.FailOverType;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
//import com.cloud.cluster.balancer.LoadBalancer;
import com.cloud.docker.Docker;
import com.cloud.kubernetes.Kubernetes;
import com.cloud.repo.PrivateRepoManager;
import com.cloud.rest.Security;

public class ClusterDaemon implements IServiceLifeCycleV2 {

	private static final Logger log 	= LogManager.getLogger(ClusterDaemon.class);
	
	// Node config CLUSTER : service end points (config_cluster.jsp)
	public static final String 		KEY_EPS  		= "KEY_NODE_EPS";
	
	private static final String KEY_HUB_URLS = "k01_00";

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
			//cfg =  CloudServices.getServiceConfig(ServiceType.DAEMON);
			PluginSystem.Plugin self = PluginSystem.findInstance(ClusterDaemon.class.getName());
			cfg =  PluginSystem.getServiceConfig(self.getVendorId());
			
			/*
			if ( !CloudServices.isConfigured()) {
				log.debug("Saving default service " + cfg );
				cfg.save();
			}
			if ( isServiceCfgUpdated) {
				log.debug("Service update detected. Default save " + cfg );
				cfg.save();
			} */
			
			log.info("Initializing Cluster Plugin...");
			
			Map<String, String> config = new HashMap<String, String>();

			//config.put(LoadBalancer.CONFIG_BASEPATH, context.getConfigurationBasePath());
			config.put(Docker.CONFIG_BASEPATH, context.getConfigurationBasePath());
			
			/* Load balancer JUNK
			LoadBalancer.initialize(config);
			LoadBalancer.load();
			*/
			//  Fix for javax.net.ssl.SSLException by setting https.protocols = TLSv1,TLSv1.1,TLSv1.2 <pre>ClientHello, TLSv1
			Security.fixSSLFatalProtocolVersion();

			Docker.initialize(config);
			Docker.load();
			
			Kubernetes.initialize(config);
			Kubernetes.load();

			// 6/1/2019
			PrivateRepoManager.init(config);
			PrivateRepoManager.loadNodes();
			
			status.setStatus(Status.ON_LINE, "Up");
		} catch (Exception e) {
			log.error("CloudProfiler service init.", e);
			throw new IOException(e);
		}
		
	}

	@Override
	public void onServiceStart(ServiceContext context) throws IOException {
		// TODO Auto-generated method stub
		
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
		log.debug("Saved plugin config " + config.getLocation());
		//  update hub urls k01_00
		final String oldhubs 	= getHubSearchUrls();
		final String hubs 		= config.getProperty(KEY_HUB_URLS);
		
		if ( !hubs.equals(oldhubs)) {
			log.debug("Updating prop " + KEY_HUB_URLS + " with " + hubs);
			cfg.put(KEY_HUB_URLS, hubs);
		}
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
		return getConfigProperty(KEY_HUB_URLS);
	}
	
	public static String getClusterGroupName() {
		NodeConfiguration config = CloudServices.getNodeConfig();
		return config.getFailOverType() == FailOverType.SERVLET ? null : CloudCluster.getInstance().getClusterGroupName();
	}

}
