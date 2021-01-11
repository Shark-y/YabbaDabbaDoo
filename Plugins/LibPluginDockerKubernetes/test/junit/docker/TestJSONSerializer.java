package junit.docker;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.docker.TestJSONSerializer.Person.Type;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.json.cloud.JSONSerializer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJSONSerializer {

	
	static void LOGD(String text) {
		System.out.println("[DOCKER] "  + text);
	}
	
	public static class Person {

		public enum Type  {Man, Woman};
		
		String name;
		int age;
		long age1;
		char char1;
		boolean bool;
		Type type;
		
		List<String> list;
		Map<String, Object> map;
		
		public Person() {
		}
		
		
		public Person(Type type, String name, int age, long age1, char char1, boolean bool, List<String> list, Map<String, Object> map) {
			super();
			this.type = type;
			this.name = name;
			this.age = age;
			this.age1 = age1;
			this.char1 = char1;
			this.bool = bool;
			this.list = list;
			this.map = map;
		}


		@Override
		public String toString() {
			return "n:" + name +  " age:" + age + " long:" + age1 + " char:" + char1 + " bool:" + bool + " list:" + list  + " map:" + map + " type:" + type;
		}
	}
	
	@BeforeClass
	public static void init() {
	}
	


	@Test
	public void test10JSONSerializer() {
		try {
			// serialize
			List<String> list = new ArrayList<String>();
			list.add("elem1");
			Map map = new HashMap<String, Object>();
			map.put("k1", "v1"); 

			Person p = new Person(Type.Man, "John", 10, 11, 'z', true, list, map); 

			// System.out.println(populateMap(p));
			JSONObject root = JSONSerializer.serialize(p);
			assertEquals("Person name == 'John'","John",root.getString("name"));
			assertEquals("Person age == 10",10,root.getInt("age"));
			assertEquals("Person bool == true",true,root.getBoolean("bool"));
			
			LOGD(root.toString());
			
			// deserialize
			String json = "{\"name\":\"foo\",\"age\":10,\"char1\":\"a\",\"map\":{\"key1\":\"val1\"},\"list\":[\"test1\"],\"bool\":true,\"age1\":200, \"type\":{\"type\":\"Woman\"}}";
			root = new JSONObject(json);
			
			Person p1 = (Person)JSONSerializer.deserialize(root, Person.class);
			LOGD("Person: " + p1.toString());

			json = "{\"name\":\"Moo\",\"age\":20,\"char1\":\"a\",\"map\":{\"key1\":\"val1\"},\"list\":[\"test1\"],\"bool\":true,\"age1\":200, \"type\": \"Man\"}";
			root = new JSONObject(json);
			
			p1 = (Person)JSONSerializer.deserialize(root, Person.class);
			LOGD("Person: " + p1.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
}
