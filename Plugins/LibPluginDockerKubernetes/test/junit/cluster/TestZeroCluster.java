package junit.cluster;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.ClusterTopicEvent;
import com.cloud.cluster.IClusterInstance;
import com.cloud.cluster.IClusterTopic;
import com.cloud.cluster.IClusterTopicListener;
import com.cloud.cluster.zeroconf.ZMap;
import com.cloud.core.services.ServiceDescriptor.ServiceType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestZeroCluster {

	//static CloudCluster cluster = CloudCluster.getInstance();
	static IClusterInstance instance; 
	
	public static class Person implements Serializable {
		private static final long serialVersionUID = 3226058582334562394L;
		String name ;
		Map<String, Object> attribs;
		
		public Person(String name) {
			this.name = name;
			this.attribs = new HashMap<String, Object>();
			attribs.put("name", name);
		}
		public String getName() {
			return name;
		}
		public Map<String, Object> getAttribs () {
			return attribs;
		}
		
		@Override
		public String toString() {
			return "(" + name + ", attribs=" + attribs + ")";
		}
	}
	
	@BeforeClass
	public static void init () throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(CloudCluster.KEY_PROVIDER, CloudCluster.Provider.CLUSTER_ZEROCONF.name());
		params.put(CloudCluster.KEY_CTX_PATH, "Node01");
		params.put(CloudCluster.KEY_CTX_URL, "http://localhost:8080/Node01/");
		params.put(CloudCluster.KEY_PRODUCT_TYPE, ServiceType.CALL_CENTER.name());
		
		CloudCluster cluster = CloudCluster.getInstance();
		cluster.initialize(params);
		instance = CloudCluster.getInstance().getClusterInstance();
	}
	
	@AfterClass
	public static void shutdown () {
		System.out.println("Cluster: Shutdown.");
		instance.shutdown();
	}
	
	@Test
	public void test01Map () {
		try {
			System.out.println("Cluster: Map create.");
			Map<String, Object> map = instance.getMap( "MAP1");
			assertNotNull(map);
			
			System.out.println("Cluster: Map set values");
			map.put("john", new Person("john"));
			map.put("k1", "v1");
			map.put("k2", "v2");
			//((ZMap)map).put("k3", "v3", 1, TimeUnit.MINUTES);
			
			assertEquals("Map size must be 3", 3, map.size()  ); 
			
			//Thread.sleep(5000);
			Person p = (Person)map.get("john");
			assertNotNull(p);
			assertEquals("Map('john') must be of cls Person", Person.class.getName(), p.getClass().getName());
			
			System.out.println("MAP1 = " + map);
			System.out.println("MAP1 Person(john) = " + p);
			
			// describe data
			JSONObject data = instance.describe();
			System.out.println("CLUSTER:DATA: " + data);
			
			assertNotNull(data.getJSONArray("data"));
			assertEquals("Cluster data size must be 1", data.getJSONArray("data").length(), 1);

			Thread.sleep(5000);

			// CLEAR
			System.out.println("Cluster: Map clear");
			map.clear();
			assertEquals("Map size must be 0", 0, map.size()  ); 
			
			System.out.println("Map Get k1 after clear() =" + map.get("k1"));
			System.out.println("Map get null = " + map.get(null));
			assertNull(map.get(null));
			assertNull(map.get("k1"));
			
			/*
			map = null;
			Map map1 = instance.getMap( "MAP1");
			System.out.println("Map instance.getMap(MAP1)=" + map1);
			map1.remove("k1");*/
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test04List() {
		try {
			
			System.out.println("Cluster: LIST create");
			List<Object> list = instance.getList("LIST1");
			assertNotNull(list);
			
			Thread.sleep(5000);
			
			// UPDATE
			
			// UPD LIST
			list.add(123);
			list.add("Hello");
			list.add(new Person("jane"));
			//assertEquals("LIST1 size must be 3", 3, list.size());
			// list size here is zero!
			
			Thread.sleep(5000);
			assertEquals("LIST1 size must be 3", 3, list.size());
			
			System.out.println("LIST1 SIZE: " + list.size() + " : " + list);
	
			// describe data
			JSONObject root = instance.describe();
			System.out.println("CLUSTER:DATA: " + root);
			
			assertNotNull(root);
			assertNotNull(root.getJSONArray("data"));
			assertTrue("Cluster data size must be >= 2", root.getJSONArray("data").length() >= 2 );
			
			int idx = 2;
			Person p1 = (Person)list.get(idx);
			assertEquals("List('jane') must be of cls Person", Person.class.getName(), p1.getClass().getName());
			assertEquals(p1.name, "jane");
			assertNotNull(p1.attribs);
			assertEquals(p1.attribs.get("name"), "jane");
			
			System.out.println("LIST1 SIZE: "  + list.size() + " GET (" + idx + "): " + p1);
	
			System.out.println("LIST1 clear.");
			list.clear();
			
			Thread.sleep(1000);
			assertEquals("List size after clear() must be 0.", 0, list.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
	}

	JSONObject resp = null;
	
	@Test
	public void test06Topic() {
		try {
			IClusterTopic topic = instance.getTopic( "TOPIC1", new IClusterTopicListener() {
				
				@Override
				public void onMessage(ClusterTopicEvent ev) {
					System.out.println("TOPIC1 Got " + ev.getPayload());
					try {
						resp = new JSONObject(ev.getPayload());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			assertNotNull(topic);
			System.out.println("Cluster: Publish to TOPIC1");
			topic.publish(new JSONObject("{\"test\": 123}"));
			
			Thread.sleep(2000);
			assertNotNull(resp);
			assertEquals("Topic message (test) must be 123", 123, resp.getInt("test"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
	}
	
	/*
	static Map<String, Object> decodeMap (Map<String, Object> map) {
		Map<String, Object> map1 = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String k = entry.getKey();
			Object v = entry.getValue();
			if ( v.toString().startsWith(ObjectCache.ENC_OBJ_PREFIX)) {
				try {
					map1.put(k, ObjectCache.decodeObject(v.toString()));
				} catch (Exception e) {
					//e.printStackTrace();
					map1.put(k, v);
				}
			}
			else { 
				map1.put(k, v);
			}
		}
		return map1;
	} 
	
	public static void main(String[] args) {

		try {
			//"k1":"ENCODED-B64H4sIAAAAAAAAAFvzloG1hIGpzBAAfojsiwkAAAA=","k2":"ENCODED-B64H4sIAAAAAAAAAFvzloG1hIGpzAgAxNnlEgkAAAA="
			System.out.println("INIT");
			Map<String, Object> map = new ConcurrentHashMap<String, Object>();
			map.put("k1","ENCODED-B64H4sIAAAAAAAAAFvzloG1hIGpzBAAfojsiwkAAAA=");
			map.put("k2", 123);
			map.put("person", "ENCODED-B64H4sIAAAAAAAAAFvzloG1uIhBJas0L7NELzmntLgktUgvJLW4JCq1KN8ZwlcJSC0qzs87HXTXZx6X3i4mBiYfBvbEkpKizKTiEgZ+n6zEskT90pLMHH3fxAJrHwaWvMTc1BIGIYhETmJeun4wUHFeunVFAdAyQZCwHki9nkdicQZQDyv7rYOHxRIuMjMwuTFw5eQnprglJpfkF3kycJZkFKUWZ+TnpFQU2DswgABPOQeQFABixhKYVSxZ+Rl5FYUMdQzsABnObLPSAAAA");
			map.put("agent", "ENCODED-B64H4sIAAAAAAAAAEWQPU7EMBBGR4FkydIsIHEFuvzQUgACrYgUKg4QLMdKzHptY09IKhpug6Ci3gJxAkTHHbgDcTawLsajT8/2G7/8gG8NHFG1jGhKbESVlIyiMhEVjUVmoov1fl4xia9vT4vV8+6XB2EOYUGJEDm3iHCQ35EHEjfIRXxFbH1N9MlACN4fy0qE/TUhiKziGzRcVj0wLViHTFqu5D08gpfDrJCqZJfMUsM1bvKw0MTaVpnyPzBsqZBlegyCAtWC/fGTorHMZBu65bJU7Rh0uh96zwlFTjkalf3J9/vH4e3nFnhzmApFyjlxf5FBiLVhtlai7PTpGbg1a3dcdW2HsJ0mSeLuDhAAIRhsh8cmQ0HwnVDak8c9+Qsjdme/eAEAAA==");
			
			System.out.println(map);
			Map<String, Object> map1 =  decodeMap(map);
			System.out.println(map1);
			
			init();
	
			testMap();
			testList();
			
			// TOPIC
			System.out.println("Cluster: Create TOPIC1");
			IClusterTopic topic = instance.getTopic( "TOPIC1", new IClusterTopicListener() {
				
				@Override
				public void onMessage(ClusterTopicEvent ev) {
					System.out.println("TOPIC1 Got " + ev.getPayload());
					
				}
			});
			System.out.println("Cluster: Publish to TOPIC1");
			topic.publish(new JSONObject("{\"test\": 123}"));
			
			Thread.sleep(20000);

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			CloudCluster.getInstance().shutdown();
		}
	}
	*/
	
}
