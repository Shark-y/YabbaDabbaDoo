package com.cloud.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.PropertiesConfig;
import com.cloud.core.config.XmlConfig;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.EncryptionTool;

/**
 * Base class for all configuration types: {@link XmlConfig}, {@link PropertiesConfig}.
 * This class cannot be instantiated. It must be inherited.
 * 
 * @author VSilva
 * @version 1.0.1 BaseConfig(Properties properties ) sort property keys.
 *
 */
abstract class BaseConfig implements IConfiguration {

	private static final Logger log 		= LogManager.getLogger(BaseConfig.class);
	
	// Default resource type: properties
	protected Properties 	resource;		// Properties HM.
	protected String 		basePath;
	protected String 		baseName;

	/** true if CP v > FS v */
	protected boolean 		versionUpdated;
	
	/** Used by the auto update system. May be different from the file system version */
	protected int			previousVersion;

	/** Keys of the updated properties if uodate detected */
	protected List<String>	updated = new ArrayList<String>();
	
	/**
	 * Create an {@link IConfiguration} from a base path and file name in the file system.
	 * @param configLocation Base path of the configuration.
	 * @param configResource File name.
	 */
	public BaseConfig(String configLocation, String configResource) {
		basePath 		= configLocation;
		baseName		= configResource;
		
		resource		= new Properties() { 
			private static final long serialVersionUID = 3302939255326113170L;

			@Override
			public Set<Object> keySet() {
				// This will sort the properties!
				return new TreeSet<Object>(super.keySet());
			}
		}; 
	}
	
	/**
	 * Create an {@link IConfiguration} from a set of {@link Properties}.
	 * @param properties Base {@link Properties}.
	 */
	public BaseConfig(Properties properties ) {
		resource = new Properties() { 
			private static final long serialVersionUID = 3302939255326113170L;

			@Override
			public Set<Object> keySet() {
				// This will sort the properties!
				return new TreeSet<Object>(super.keySet());
			}
		}; 

		loadFrom(properties);
	}

	/**
	 * Load an {@link IConfiguration} from a {@link Properties} object.
	 * @param conf
	 */
	protected void loadFrom(Properties conf) {
		Enumeration<Object> keys = conf.keys();
		
		while (keys.hasMoreElements()) {
			String key 	= keys.nextElement().toString();
			String val	= conf.getProperty(key);
			
			if ( !containsKey(key)) {
				//System.out.println("Updated " + key);
				updated.add(key);
			}
			setProperty(key, val);
		}
	}	

	protected void cleanUpdated () {
		updated.clear();
	}
	
	@Override
	public String getProperty(String key) {
		String value = resource.getProperty(key);
		
		// decrypt (if possible)
		if ( value != null && EncryptionTool.isEncryptedText(value) ) {
			return EncryptionTool.decryptTaggedPassword(value); 
		}
		return value != null ? value.trim() : value; 
	}

	@Override
	public void setProperty(String key, String value) {
		resource.setProperty(key, value);
	}

	@Override
	public boolean isEmpty() {
		return resource.isEmpty();
	}

	@Override
	public Set<Entry<Object, Object>> entrySet() {
		return resource.entrySet();
	}

	@Override
	public Set<Object> keySet() {
		return resource.keySet();
	}

	@Override
	public Object put(Object key, Object value) {
		return resource.put(key, value);
	}

	@Override
	public void setLocation(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public String getLocation() {
		return basePath;
	}

	public String getBaseName() {
		return baseName;
	}
	
	@Override
	public int getVersion() {
		String sVer = getProperty(KEY_VERSION);
		return sVer != null ? Integer.parseInt(sVer) : 0;
	}

	@Override
	public Enumeration<Object> getKeys() {
		return resource.keys();
	}
	
	/**
	 * Get an integer property.
	 * @param key Property key.
	 * @return integer value.
	 */
	public int getInteger(String key) {
		String val = getProperty(key);
		if ( val != null )
			return Integer.parseInt(val);
		throw new IllegalArgumentException("Invalid configuration key " + key);
	}

	/**
	 * Get a boolean property.
	 * @param key Property key.
	 * @return boolean value.
	 */
	public boolean getBoolean(String key) {
		String val = getProperty(key);
		if ( val != null )
			return Boolean.parseBoolean(val);
		throw new IllegalArgumentException("Invalid configuration key " + key);
	}

	public void destroy() {
		try {
			resource = null;
		} catch (Exception e) {
			log.error("Close: " + e.toString());
		}
	}
	
	public boolean containsKey (String key) {
		return resource.containsKey(key);
	}
	
	/**
	 * Removes the key (and its corresponding value) from this hashtable. This method does nothing if the key is not in the hashtable.
	 * @param key the key that needs to be removed 
	 * @return the value to which the key had been mapped in this hashtable, or null if the key did not have a mapping 
	 */
	public Object remove (Object key) {
		if ( key == null)
			return null;
		return resource.remove(key);
	}
	
	public Properties getProperties() {
		return resource;
	}

	/**
	 * Check if there is a version update between the file system and class path.
	 * @return True if the class path version > file system version thus a version update.
	 */
	public boolean versionUpdateDetected () {
		return versionUpdated;
	}

	/**
	 * Get the class path configuration version. It may be different from the file system version if the configuration has been updated.
	 * @return Class path version.
	 */
	public int getPreviousVersion() {
		return previousVersion > 0 ? previousVersion : getVersion();
	}

	@Override
	public void save(String comment) throws FileNotFoundException, IOException {
	}
	
	/**
	 * Update a {@link Properties} object from another by injecting all the key,value pairs from a source into a destination.
	 * @param src Source {@link Properties} (disk configuration).
	 * @param dst Destination {@link Properties} (class-path configuration).
	 */
	protected void updateProperties(Properties src, Properties dst) {
		Enumeration<Object> keys = src.keys();
		
		while (keys.hasMoreElements()) {
			String key 	= keys.nextElement().toString();
			String val	= src.getProperty(key);
			
			// don't set attributes or version
			if (key.contains("_attribute_") || key.contains("cfg_group") || key.equals(KEY_VERSION) ) {
				continue;
			}

			// vsilva 2/18/2016 Don't update links
			String widget = src.getProperty(key + "_attribute_widget");
			
			if ( widget != null && widget.equals/*contains*/("link")) {
				// 10/22/2017 Remove the original (disk) key if removed from updated config (class path).
				if ( !dst.containsKey(key)) {
					log.debug("Check4Update: Removing LINK "  + key + " = " + val);
					src.remove(key);
				}
				else {
					log.debug("Check4Update: Skip LINK update "  + key + " = " + val);
				}
				continue;
			}

			// 8/16/2019 delete removed (deprecated) keys
			if ( !dst.containsKey(key)) {
				src.remove(key);
				continue;
			}
			
			// vsilva 10/14/2016 Don't update labels either
			if ( widget != null && widget.equals("label")) {
				log.debug("Check4Update: Skip LABEL update "  + key + " = " + val);
				continue;
			}
			dst.setProperty(key, val);
		}
	} 

	/**
	 * True if a configuration key is new from an update.
	 * @param key Configuration key.
	 * @return True if new from an update.
	 */
	public boolean isNew(String key) {
		return updated.contains(key);
	}
	
}
