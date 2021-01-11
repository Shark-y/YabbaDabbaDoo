package com.plugin.sample;

import java.io.IOException;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.services.ServiceStatus.Status;
import com.cloud.core.services.ServiceStatus;

public class SamplePlugin implements IServiceLifeCycle {

	ServiceStatus mStatus = new ServiceStatus();
	
	static void LOGD(final String text) {
		System.out.println("[SAMPLE] " + text);
	}
	
	@Override
	public void onServiceInitialize(ServiceContext context) throws IOException {
		// TODO Auto-generated method stub
		LOGD("onServiceInitialize");
	}

	@Override
	public void onServiceStart(ServiceContext context) throws IOException {
		// TODO Auto-generated method stub
		LOGD("onServiceStart");
		mStatus.setStatus(Status.ON_LINE, "Plugin started.");
	}

	@Override
	public void onServiceStop() {
		// TODO Auto-generated method stub
		LOGD("onServiceStop");
		mStatus.setStatus(Status.OFF_LINE, "Plugin stopped.");
	}

	@Override
	public void onServiceDestroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServiceStatus getServiceStatus() {
		return mStatus;
	}

	@Override
	public void onServiceValidateConfig(IConfiguration config) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServiceType getServiceType() {
		// TODO Auto-generated method stub
		return null;
	}

}
