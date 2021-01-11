package console.prometheus;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.ext.PrometheusDataSource;
import com.rts.ui.DashboardList;

public class TestPrometheus {

	static PrometheusDataSource ds;

	static String url = "http://192.168.40.84:32206/";
	
	static void LOGD (String txt) {
		System.out.println("[PROM] " + txt);
	}
	
	static class BatchListener implements IBatchEventListener {

		@Override
		public void onBatchReceived(JSONObject batch) {
			try {
				System.out.println("Got batch " + batch.toString(1));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	static void init ( ) throws Exception {
		ds = new PrometheusDataSource("Kube1", "P1Kube1", url, 20000, new BatchListener());
		LOGD("DS: " + ds.toJSON());
	}
	
	static void shutdown ( ) {
		LOGD("Shutdown");
		ds.shutdown();
	}
	
	
	static void test01 ( ) {
		DataFormat fmt =  ds.getFormat();
		LOGD("Name: " + ds.getName() + " Format: " + ds.getFormat().getFields());
		
		ds.run();

//		String metric = "some_mtric{QUREY}";
//		String name = metric.contains("{") ? metric.substring(0, metric.indexOf("{")) : metric;
//		System.out.println("****=" + name);
//		try {
//			String path = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\Profiles\\Default";
//			DashboardList list = new DashboardList(path, "dashboards.xml");
//			System.out.println(list.toXML());
//			System.out.println(list.toJSON());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
	}

	static void testAddMetrics ( ) {
		//ds.addMetric("container_cpu_load_average_10s{namespace'westlake-dev'}");
		ds.addMetric("dash1", "container_memory_usage_bytes");//{namespace'westlake-dev'}");
		
		ds.addMetric("dash2", "container_file_descriptors{namespace=\'westlake-dev\'}");
		
		//ds.setMetric("container_file_descriptors{namespace=\"westlake-dev\"}");
		//ds.setMetric("container_file_descriptors");
	}

	static void testValidate ( ) throws IOException {
		// bad query
		String query = "container_cpu_system_seconds_total{namespac'westlake-dev'}";
		try {
			ds.validate(query);
		} catch (Exception e) {
			System.err.println("BAD QUERY " + query);
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			init();
			
			test01();
			testAddMetrics();
			testValidate();
			
			Thread.sleep(30000);
			
			shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
