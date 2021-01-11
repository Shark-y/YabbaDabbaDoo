package org.json.cloud;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * A simple JSON {@link Object} serializer/deserializer based on code from {@link JSONObject} capable of understanding:
 * <ul>
 * <li>primitive types: String, char, int, long, boolean
 * <li>Maps, Lists
 * </ul>
 * <pre>// SERIALIZE OBJECT OF TYPE PERSON
 *  List list = new ArrayList<String>();
 *  list.add("elem1");
 *  Map map = new HashMap<String, Object>();
 *  map.put("k1", "v1"); 
 *  Person p = new Person("name", 10, 11, 'z', true, list, map); 
 *  JSONObject root = serialize(p);
 *  System.out.println(root);
 * 
 *  // DESERIALIZE PERSON-JSON INTO CLS PERSON
 *  String json = "{\"name\":\"foo\",\"age\":10,\"char1\":\"a\",\"map\":{\"key1\":\"val1\"},\"list\":[\"test1\"],\"bool\":true,\"age1\":200}";
 *  root = new JSONObject(json);
 *  Person p1 = (Person)deserialize(root, Person.class);
 *  System.out.println(p1); </pre>
 * 
 * <h2>ChangeLog</h2>
 * <ul>
 * <li> 3/14/2019 Custom member objects (objects that do not belong to java.lang or javax pkgs will be ignored).
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 3/4/2019
 *
 */
public class JSONSerializer {

	private static final Logger log = LogManager.getLogger(JSONSerializer.class);
	
	/**
	 * This code was taken from {@link JSONObject} and modified to get an object fields using reflection.
	 * <b>Note: Private fields are changed using field.setAccessible(true) </b>. So watch out!
	 * @param bean Any {@link Object} instance.
	 * @return A {@link Map} of the object (field-name, value) pairs.
	 * @throws IllegalAccessException
	 */
	private static Map<String, Object> populateMap(Object bean) throws IllegalAccessException {
		Map<String, Object> map 		= new HashMap<String, Object>();
		Class<? extends Object> klass 	= bean.getClass();
		
		Field[] fields 					= klass.getDeclaredFields();
		JSONClass clsmeta 				= klass.getAnnotation(JSONClass.class);

		// serialze base class?
		if ( clsmeta != null && clsmeta.serializeBaseClass()) {
			Field[] superFields = klass.getSuperclass().getDeclaredFields();
			Field[] allfields 	= new Field[fields.length + superFields.length];
			
			System.arraycopy(fields, 0, allfields, 0, fields.length);
			System.arraycopy(superFields, 0, allfields, fields.length, superFields.length);
			fields 				= allfields;
		}

		for (int i = 0; i < fields.length; i += 1) {
			Field field 	= fields[i];
			field.setAccessible(true);
			final String key 			= field.getName();
			final Object val 			= field.get(bean);
			
			// NOTE: this will; not work with Objects that have enums unless the enum has a get{ENUM-NAME}
			Object wrapped				= JSONObject.wrap(val);

			final String typeName 		= field.getType().getName(); 
			boolean isPrimitive			= field.getType().isPrimitive();
			final boolean isEnum		= field.getType().isEnum();			// 6/1/2019
			final boolean isFinal		= Modifier.isFinal(field.getModifiers());
			
			// check annotation JSONField
			JSONField meta = field.getAnnotation(JSONField.class);
			if ( meta != null) {
				if ( meta.skip()) {
					log.debug("IGNORE " + key + " " + val );
					continue;
				}
				else {
					isPrimitive = true;
				}
			}
			
			log.debug ("JSON::SERIALIZE (" + typeName  + ") "  + key + " = " + val + " (" + wrapped + ") enum=" + isEnum 
					+ " final=" + isFinal + " Primitive=" + isPrimitive);
			
			// Ignore custom objects 
			if ( !typeName.startsWith("java.") && !typeName.startsWith("javax") && !isPrimitive && !isEnum ) {
				log.debug("JSONSerializer::SERIALIZE IGNORING OBJECT " + key + " (" + typeName  + ") with val " + val );
				continue;
			}
			
			/* unable to wrap object?
			if (wrapped.toString().equals("{}")) {
				wrapped = val.toString();
			} */
			//map.put(key, wrapped == JSONObject.NULL ? "" : wrapped );
			map.put(key, wrapped );
		}
		return map;
	}

	/**
	 * Serialize some object into JSON.
	 * <pre>// SERIALIZE OBJECT OF TYPE PERSON
	 *  List list = new ArrayList<String>();
	 *  list.add("elem1");
	 *  Map map = new HashMap<String, Object>();
	 *  map.put("k1", "v1"); 
	 *  Person p = new Person("name", 10, 11, 'z', true, list, map); 
	 *  JSONObject root = serialize(p);
	 *  System.out.println(root); </pre>
	 * @param bean {@link Object} to convert.
	 * @return Object fields as JSON (Lists, Maps supported).
	 * @throws IllegalAccessException on access errors.
	 */
	static public JSONObject serialize (Object bean) throws IllegalAccessException {
		return new JSONObject(populateMap(bean));
	}
	
	/**
	 * Deserialize a {@link JSONObject} into an instance of klass.
	 *  <pre>// DESERIALIZE PERSON-JSON INTO CLS PERSON
	 *  String json = "{\"name\":\"foo\",\"age\":10,\"char1\":\"a\",\"map\":{\"key1\":\"val1\"},\"list\":[\"test1\"],\"bool\":true,\"age1\":200}";
	 *  root = new JSONObject(json);
	 *  Person p1 = (Person)deserialize(root, Person.class);
	 *  System.out.println(p1); </pre>
	 * @param jroot The {@link JSONObject} representation.
	 * @param klass The Class/Type sink.
	 * @return An instance of klass with its fields populated from JSON.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static public Object deserialize (JSONObject jroot, Class klass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, JSONException {
		final Object obj 		= klass.newInstance();
		Field[] fields 			= klass.getDeclaredFields();
		JSONClass clsmeta 		= (JSONClass)klass.getAnnotation(JSONClass.class);

		// process base class?
		if ( clsmeta != null && clsmeta.serializeBaseClass()) {
			Field[] superFields = klass.getSuperclass().getDeclaredFields();
			Field[] allfields 	= new Field[fields.length + superFields.length];
			System.arraycopy(fields, 0, allfields, 0, fields.length);
			System.arraycopy(superFields, 0, allfields, fields.length, superFields.length);
			fields 				= allfields;
		}
		
		for (int i = 0; i < fields.length; i += 1) {
			final Field field 		= fields[i];
			final String key 		= field.getName();
			field.setAccessible(true);
			final Object val 		= jroot.opt(key);
			final String typeName 	= field.getType().getName();
			
			log.debug ("JSON::DESERIALIZE (" + typeName  + ") " + key + " = " + val + " root=" + jroot);
			
			// check annotation JSONField
			JSONField meta 			= field.getAnnotation(JSONField.class);
			if ( meta != null && meta.skip()) {
				log.debug ("JSON::DESERIALIZE IGNORE " + key + " " + val );
				continue;
			}
			
			if ( typeName.contains("List")) {
				List list 		= new ArrayList();
				JSONArray jarr 	= (JSONArray)val;
				
				for (int j = 0; j < jarr.length(); j++) {
					list.add(jarr.get(j));
				}
				field.set(obj, list);
				continue;
			}
			else if ( typeName.contains("Map")) {
				Map map 			= new HashMap();
				JSONObject jobj 	=  (JSONObject)val;
				Set<Object> keys 	=  jobj.keySet();
				for ( Object k: keys)  {
					map.put(k, jobj.get(k.toString()));
				}
				field.set(obj, map);
				continue;
			}
			else if ( typeName.contains("char")) {
				field.set(obj, val.toString().charAt(0) );
				continue;
			}
			else if (field.getType().isEnum()) {
				//System.out.println("Set enum " + key + " = " + val);
				if ( val != null) {
					if ( val instanceof JSONObject ) {
						JSONObject jval = (JSONObject)val;
						if ( jval.has(key)) {
							field.set(obj, Enum.valueOf((Class)field.getType(), jval.getString(key) ));
						}
						else {
							log.debug("JSONSerializer::DESERIALIZE Missing value for field " + key  + " (" + typeName + ") in JSON " + jval);
						}
					}
					else {
						field.set(obj, Enum.valueOf((Class)field.getType(), val.toString() ));
					}
					//field.set(obj, Enum.valueOf((Class)field.getType(), val instanceof JSONObject ? ((JSONObject)val).getString(key) : val.toString() ) );
				}
				continue;
			}
			else {
				/*System.out.println("found posible obj " + key + " (" + typeName  + ") = " + val 
						+ " isSynthetic=" + field.getType().isSynthetic() + " isLocalClass=" + field.getType().isLocalClass() 
						+ " isMemberClass=" + field.getType().isMemberClass() + " primitive=" + field.getType().isPrimitive()); */
				// watch 4 switch tables: $SWITCH_TABLE$com$cloud$cloud$core$services$ServiceStatus$Status ([I)
				if ( key.startsWith("$SWITCH_TABLE")) {
					continue;
				}
				// deserialize object
				if ( !typeName.startsWith("java.lang") && !typeName.startsWith("javax") && !field.getType().isPrimitive() ) {
					//System.out.println("JSONSerializer::DESERIALIZE OBJECT " + key + " (" + typeName  + ") with val " + val );
					try {
						Object obj1 = deserialize((JSONObject)val, Class.forName(typeName));
						field.set(obj, obj1);
					} catch (Throwable e) {
					}
					continue;
				}
			}
			if ( val != JSONObject.NULL) {
				field.set(obj, val );
			}
		}
		return obj; 
	}
	
}
