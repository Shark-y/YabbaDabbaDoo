package com.cloud.core.profiler;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.profiler.OSMetrics;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Get JVM metrics from externals VMs in the same host using the attach API
 * https://docs.oracle.com/javase/7/docs/technotes/guides/attach/index.html
 * 
 * @see https://www.javaworld.com/article/2071330/the-attach-api.html
 * @author VSilva
 *
 */
public class VMAttach {
	
	static final Logger log = LogManager.getLogger(VMAttach.class);
	
	static final String KEY_JMX_ADDR = "com.sun.management.jmxremote.localConnectorAddress";

	/** Map used to track VM JMX connections */
	private static final Map<String, RemoteVirtualMachine> vms = new HashMap<String, VMAttach.RemoteVirtualMachine>();
	
	private static void LOGD (final String text) {
		log.debug(text);
	}
	
	/**
	 * get a remote JXM connection URL - service:jmx:rmi://127.0.0.1/stub/[KEY]
	 * @param id VM process id.
	 * @return service:jmx:rmi://127.0.0.1/stub/[KEY]
	 * @throws Exception on {@link VirtualMachine}
	 */
	static String getConnectorAddr(final String id) throws Exception {
		VirtualMachine vm 		= VirtualMachine.attach(id);
		String connectorAddr 	= vm.getAgentProperties().getProperty(KEY_JMX_ADDR);
		if (connectorAddr == null) {
			final String agent = vm.getSystemProperties().getProperty("java.home")
					+ File.separator + "lib" + File.separator
					+ "management-agent.jar";
			vm.loadAgent(agent);
			connectorAddr = vm.getAgentProperties().getProperty(KEY_JMX_ADDR);
		}
		vm.detach();
		return connectorAddr;
	}
	
	/**
	 * Class to track a virtual machine process id and {@link ThreadMXBean} and {@link OperatingSystemMXBean} beans.
	 * @author VSilva
	 *
	 */
	static class RemoteVirtualMachine {
		final JMXConnector connector;
		final String connectorAddr;
		final String pid;
		final OperatingSystemMXBean osMXBean;
		final ThreadMXBean threadMXBean; 
		
		/**
		 * Construct: Connect to the service URL and fetch {@link ThreadMXBean} and {@link OperatingSystemMXBean}.
		 * @param pid VM process id.
		 * @param connectorAddr VM address: service:jmx:rmi://127.0.0.1/stub/[KEY]
		 * @throws Exception If anything goes wrong :(
		 */
		public RemoteVirtualMachine(String pid, String connectorAddr ) 
			throws Exception
		{
			super();
			this.pid = pid;
			this.connectorAddr = connectorAddr;
			JMXServiceURL serviceURL 	= new JMXServiceURL(connectorAddr);

			connector 					= JMXConnectorFactory.connect(serviceURL);
			MBeanServerConnection mbsc 	= connector.getMBeanServerConnection();

			LOGD(pid + " Connected to " + connectorAddr);
			
			// Threads
			Set<ObjectName> mbeans 		= mbsc.queryNames(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME), null);
			
			LOGD(pid + " Beans " + ManagementFactory.THREAD_MXBEAN_NAME + " size=" + mbeans.size());
			threadMXBean 	=  !mbeans.isEmpty()
					? ManagementFactory.newPlatformMXBeanProxy(mbsc, mbeans.iterator().next().toString(), ThreadMXBean.class)
					: null;
			// OS
			mbeans 		= mbsc.queryNames(new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME), null);
			osMXBean 	= !mbeans.isEmpty()
						? ManagementFactory.newPlatformMXBeanProxy(mbsc, mbeans.iterator().next().toString(), OperatingSystemMXBean.class)
						: null;
			LOGD(pid + " Beans " + ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME + " Size=" + mbeans.size());
		}
		
		public void unregisterBeans () {
			try {
				MBeanServerConnection mbsc 		= connector.getMBeanServerConnection();
				if ( threadMXBean != null) {
					LOGD(pid + " UREGISTER THREADS BEAN");
					mbsc.unregisterMBean(threadMXBean.getObjectName());
				}
				if ( osMXBean != null ) {
					LOGD(pid + " UREGISTER OS BEAN");
					mbsc.unregisterMBean(osMXBean.getObjectName());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		} 
		
		public void close() throws IOException {
			unregisterBeans();
			if ( connector != null ) {
				LOGD(pid + " JMX close " + connector.getConnectionId());
				connector.close();
			}
		}
	}
	
	/**
	 * Get Thread information from a remote VM as a JSON array of arrays: [[t1],[t2]...] using {@link ThreadMXBean}.
	 * <p>Where T(n) = Id, State, CPUTime, User Time.</p>
	 * @param id VM process ID.
	 * 
	 * @return JSON format compatible with Data Tables:
	 * 
	 * <pre> [
	 *  [ tid1, "Thread1Name", "STATE1", CpuTime1, UserTime1 ],
	 *  [ tid2, "Thread2Name", "STATE2", CpuTime2, UserTime2 ]
	 *  ]
	 * </pre>
	 * 
	 * @throws JSONException on JSON errors.
	 */
	public static JSONArray getThreadInfo(final String id) throws Exception {
		if ( !vms.containsKey(id)) {
			final String connectorAddr	= getConnectorAddr(id);
			vms.put(id, new RemoteVirtualMachine(id, connectorAddr));
		}
		RemoteVirtualMachine vm = vms.get(id);
		return vm.threadMXBean != null ? OSMetrics.getThreadInfo(vm.threadMXBean) : new JSONArray();

	}

	/**
	 * OS metrics encoded as JSON using {@link OperatingSystemMXBean} and {@link ThreadMXBean} 
	 * @param id VM process ID.
	 * 
	 * @return JSON string of the form <pre>
	 * {"operatingSystem":
	 *  {"FreePhysicalMemorySize":"4294967295"
	 *  ,"FreeSwapSpaceSize":"4294967295"
	 *  ,"AvailableProcessors":"4"
	 *  ,"ProcessCpuLoad":"-1.0"
	 *  ,"TotalSwapSpaceSize":"4294967295"
	 *  ,"ProcessCpuTime":"124800800"
	 *  ,"Name":"Windows 7","Arch":"x86"
	 *  ,"SystemLoadAverage":"-1.0"
	 *  ,"TotalPhysicalMemorySize":"4294967295"
	 *  ,"CommittedVirtualMemorySize":"44785664","ObjectName":"java.lang:type=OperatingSystem"
	 *  ,"Version":"6.1"
	 *  ,"SystemCpuLoad":"-1.0"
	 *  }}</pre>
	 * 
	 * @throws JSONException on JSON errors.
	 */
	public static JSONObject getOSMetrics(final String id) throws Exception {
		if ( !vms.containsKey(id)) {
			final String connectorAddr	= getConnectorAddr(id);
			vms.put(id, new RemoteVirtualMachine(id, connectorAddr));
		}
		RemoteVirtualMachine vm = vms.get(id);
		
		return vm.osMXBean != null && vm.threadMXBean != null ? OSMetrics.getOSMetrics(vm.osMXBean, vm.threadMXBean) : new JSONObject();
	}
	
	/**
	 * Get remote VMs in data tables format [[id, display-name, provider]...]
	 * @return Data tables JSON: [["5384","com.vm.VMAttach","sun"],["2084","","sun"]]
	 */
	public static JSONArray getRemoteVMs () {
		JSONArray array = new JSONArray();
		List<VirtualMachineDescriptor> list = VirtualMachine.list();

		for (VirtualMachineDescriptor vmd : list) {
			JSONArray row = new JSONArray();
			row.put(vmd.id());
			row.put(vmd.displayName());
			row.put(vmd.provider().name());
			array.put(row);
		}
		return array;
	}

	/**
	 * Unregister all (OS-Thread) JMX beans and close all JMX client connections.
	 */
	public static void shutdown () {
		// disconnect
		try {
			for ( Map.Entry<String, RemoteVirtualMachine> entry : vms.entrySet()) {
				entry.getValue().close();
			}
			vms.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
