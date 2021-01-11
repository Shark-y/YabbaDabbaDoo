package com.cloud.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * A version of {@link Properties} that keeps the insertion ordering.
 * <pre>
 * Properties props = new OrderedProperties();
 * props.put("1", "1");
 * props.put("20", "20");
 * props.put("300", "300");
 * for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
 *  Object key = e.nextElement();
 *  System.out.println(key + "=" + props.getProperty(key.toString()));
 * }
 * </pre>
 *
 * @author Admin
 * @version 1.0.0
 */
public class OrderedProperties extends Properties {

	private static final long serialVersionUID = -759074113203020611L;

	// Thread safe private Vector<Object> _names;
	private List<Object> _names;
	
    public OrderedProperties() {
        super ();
        _names = new ArrayList<Object>(); // Vector<>();
    }

    public Enumeration<Object> propertyNames() {
        return Collections.enumeration(_names); // elements();
    }

    public Object put(Object key, Object value) {
        if (_names.contains(key)) {
            _names.remove(key);
        }
        _names.add(key);
        return super.put(key, value);
    }

    public Object remove(Object key) {
        _names.remove(key);
        return super.remove(key);
    }

    @Override
    public synchronized boolean equals(Object obj) {
    	return super.equals(obj);
    }

    @Override
    public synchronized int hashCode() {
    	return super.hashCode();
    }
    
 }