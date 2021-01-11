package com.cloud.core.io;

import java.lang.reflect.Field;

/**
 * This code is used to extract metrics from the JTAPI lib using reflection
 * 
 * @author VSilva
 *
 */
public class ReflectionTool {

	/**
	 * Set all fields in a class to public
	 * @param cls
	 */
	public static void setAllFieldsAccesible ( Class<?> cls) {
		Field[] fields = cls.getDeclaredFields();
		for (Field field : fields) {
			//final String key 		= field.getName();
			field.setAccessible(true);
			//System.out.println(cls.getCanonicalName() +  " field " + key);
		}
	}
	
	/**
	 * Get a class field/member variable.
	 * @param cls {@link Class}
	 * @param name Member name.
	 * @return
	 */
	public static Field getField ( Class<?> cls, String name) {
		Field[] fields = cls.getDeclaredFields();
		for (Field field : fields) {
			final String key 		= field.getName();
			if ( key.equals(name)) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	/**
	 * Get the value of a field given an object instance and {@link Field}.
	 * @param field a {@link Field}.
	 * @param obj Object instance.
	 * @return The field value as Object.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Object getValue (  Field field, Object obj) throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		return field.get(obj);
	}

	public static Object invokeMethod (String typeName, String method, Object instance) throws Exception  {
		Class<?> Cls = Class.forName(typeName);
		return Cls.getMethod(method).invoke(instance);
	}

}
