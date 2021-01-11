package junit.cluster;

import java.util.HashMap;
import java.util.Map;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.multicast.EndPoint;
import com.cloud.cluster.multicast.ZeroDescriptorService;
import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.core.profiler.OSMetrics;


public class TestZeroSender {

	public static void main(String[] args) {
		try {
			String baseURL = "http://localhost:8080/Node001/index.jsp";
			Map<String, Object> attribs = new HashMap<String, Object>();
			attribs.put("KEY_CTX_PATH", "/Node001");
			attribs.put("KEY_CTX_URL", baseURL);
			
			attribs.put("statusCode", 200);
			attribs.put("statusMessage", "Online");
			attribs.put("vendor", "ACME");
			
			OSMetrics.injectOSMetrics(attribs);
			//String base = "http://localhost:8080";
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Authorization", "bearer ACESS_TOKEN");
			headers.put("Foo", "Bar");
			
			EndPoint start 		= EndPoint.post(baseURL + "/SysAdmin?rq_operation=start", headers);
			EndPoint stop 		= EndPoint.post(baseURL + "/SysAdmin?rq_operation=stop", headers);
			EndPoint status 	= EndPoint.get(baseURL + "/OSPerformance", headers);
			EndPoint confget 	= EndPoint.get(baseURL + "/SysAdmin?op=confget");
			EndPoint confstore 	= EndPoint.post(baseURL + "/SysAdmin?op=constore");
			EndPoint logget 	= EndPoint.get(baseURL + "/LogServlet");
			EndPoint logview 	= EndPoint.get(baseURL + "/log/logview.jsp");
			EndPoint logclear 	= EndPoint.post(baseURL + "/LogServlet?op=clear&len=0");
			
			ZeroDescriptorService up = ZeroDescriptorService.createServiceUp("123", start, stop, status, confget, confstore, logget, logview, logclear, attribs);
//			ZeroDescriptorServiceUp sd = ZeroDescriptorServiceUp.create("123",start, stop, status, confget, confstore, logget, logview, logclear, attribs);
					
//			ZeroDescriptorClusterUp cd = ZeroDescriptorClusterUp.create("com.mysql.jdbc.Driver"
//					, "jdbc:mysql://localhost:3306/cloudcluster?useSSL=true&verifyServerCertificate=false", "cloud", "Thenewcti1");
			
			// 
//			ZeroDescriptorClusterUp cd = ZeroDescriptorClusterUp.create("org.apache.derby.jdbc.ClientDriver"
//					, "jdbc:derby://localhost:1527/clusterdb", "APP", "APP");
			
			ZeroConfDiscovery ds = ZeroConfDiscovery.getInstance();

			ds.setTimeToLive(128);
			ds.setBCastFrequency(20);
			ds.joinGroup();
			
			System.out.println("SENDER: TTL=" + ds.getTimeToLive());
			ds.sendAndQueue(up);
//			ds.queue(cd);
			
			Thread.sleep(5000);
			
			ds.send(ZeroDescriptorService.createServiceDown());
			
//			ds.send(ZeroDescriptorServiceDown.create());
			/*
			ds.send(ZeroDescriptorObject.create(MessageType.OBJECT_NEW, ObjectType.T_LIST, "LIST1"));
			ds.send(ZeroDescriptorObject.create(MessageType.OBJECT_NEW, ObjectType.T_MAP, "MAP1"));
			
			ds.send(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.ADD,  "LIST1", null, "val1"));
			ds.send(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.ADD, "MAP1", "key1", "val1"));
			*/
			
			Thread.sleep(60000);
			
			System.out.println("Sender: Zero Shutdown.");
			ds.shutdown(); 
			
			CloudCluster.getInstance().shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
