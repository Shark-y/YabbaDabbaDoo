package com.cloud.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Implement this interface to support multiple types of configurations:
 * From {@link Properties}, or XML or JSON, etc...
 * @author vsilva
 *
 */
public interface IConfiguration {

	/** Class path separator */
	public static final String PATH_SEP 	= "/";

	/** Configuration file version */
	static final String KEY_VERSION 		= "_version";

	public abstract String getProperty(String key);

	public abstract void setProperty(String key, String value);

	public abstract boolean isEmpty();

	public abstract Set<Entry<Object, Object>> entrySet();

	public abstract Set<Object> keySet();

	/**
	 * Attempt to save the config to the file system only.
	 * @param comment
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public abstract void save(String comment) throws FileNotFoundException,IOException;

	/**
	 * Set a configuration value key, value pair.
	 * @param key Configuration key.
	 * @param value Configuration value.
	 * @return
	 */
	public abstract Object put(Object key, Object value);

	/**
	 * Set the store location.
	 * @param basePath
	 */
	public abstract void setLocation(String basePath);
	
	/**
	 * Get the location in the FS or class path where this configuration is stored.
	 * @return
	 */
	public abstract String getLocation();
	
	/**
	 * Get the base name of the file.
	 * @return
	 */
	public abstract String getBaseName();
	
	/**
	 * Get configuration version from the file system.
	 * @return
	 */
	public abstract int getVersion();
	
	/**
	 * The the configuration keys.
	 * @return
	 */
	public abstract Enumeration<Object> getKeys();
	
	
	/**
	 * Get an integer property.
	 * @param key Property key.
	 * @return integer value.
	 */
	public abstract int getInteger(String key);
	
	
	/**
	 * Get a boolean property.
	 * @param key Property key.
	 * @return boolean value.
	 */
	public abstract boolean getBoolean(String key);

	/**
	 * Test of the configuration has a given key.
	 * @param key Configuration key.
	 * @return True if key exists.
	 */
	public boolean containsKey (String key);
	
	/**
	 * Destructor.
	 */
	public abstract void destroy();
	
	/**
	 * Removes the key (and its corresponding value) from this config. This method does nothing if the key is not in the hashtable.
	 * @param key the key that needs to be removed 
	 * @return the value to which the key had been mapped in this hashtable, or null if the key did not have a mapping 
	 */
	public abstract Object remove (Object key);
	
	/**
	 * Check if there is a version update between the file system and class path.
	 * @return true if the class path version > file system version thus a version update.
	 */
	public abstract boolean versionUpdateDetected ();
	
	/**
	 * Get the previous configuration version. It may be different from the file system version if the configuration has been updated.
	 * @return Previous version (usually the file system version).
	 */
	public abstract int getPreviousVersion();

	/**
	 * True if a configuration key is new from an update.
	 * @param key Configuration key.
	 * @return True if new from an update.
	 */
	public abstract boolean isNew(String key);
	
}