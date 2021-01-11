package com.cloud.core.services;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.provider.IServiceLifeCycle;

/**
 * Generic Service descriptor helper class. A service can be:
 * <ul>
 * <li>A LivePerson, Oracle RNT or SalesForce message broker
 * <li>A Genesys, Avaya, Cisoc contact center
 * </ul>
 * @author VSilva
 *
 */
public class ServiceDescriptor implements Serializable  {
	private static final long serialVersionUID = -781012693517950870L;

	/* Default vendors */
	public static final String VENDORID_SALESFORCE 	= "SALESFORCE";
	public static final String VENDORID_LIVEPERSON 	= "LIVEPERSON";
	public static final String VENDORID_AVAYA 		= "AVAYA";
	public static final String VENDORID_CISCO 		= "CISCO";
	public static final String VENDORID_GENESYS 	= "GENESYS";
	
	/** Service descriptor configuration keys. */
	static final String KEY_SERVICE_CLASS 	= "service_class";
	static final String KEY_SERVICE_CFG 	= "service_config";
	static final String KEY_SERVICE_VENDOR	= "service_vendor";
	static final String KEY_VENDOR_ID		= "service_vendorId";
	static final String KEY_SERVICE_THEME	= "service_theme";
	static final String KEY_SERVICE_REQLIC	= "service_requiresLicense";

	// 8/13/2019 CN pooling 
	public static final String KEY_SERVICE_SUPPOOL	= "service_supportsConnectionPooling";
	public static final String KEY_SERVICE_POOLCLS	= "service_connectionPoolingClass";
	
	/** Supported service types: 
	 * <li>Message broker: Adapter 
	 * <li> Contact center: Adapter
	 * <li> Call Center: Connector 
	 * <li> Voice Recording
	 * <li> Licensing
	 * <li> Generic Daemon
	 */
	public enum ServiceType {
		MESSAGE_BROKER		// Cloud Adapter
		, CONTACT_CENTER	// Cloud Adapter
		, CALL_CENTER		// Cloud Connector
		, VOICE_RECORDING	// Voice recording implementations
		, LICENSING			// Licensing
		, DAEMON			// Generic server daemon
		, PLUGIN			// 5/21/2020 Console or service plugin 
	}
	
	/** The name of the bootstrap class that loads the service */
	private String className;
	
	/** The name of the configuration file (acme.ini) for the service */
	private String configFileName;
	
	/** The name of the vendor (provider) of the service */
	private String vendorName;

	/** Id of the vendor: SALESFORCE, AVAYA, ORACLE, LIVEPERSON, GENESYS */
	private String vendorId;

	/** See {@link ServiceType} */
	private ServiceType type;
	
	/** fname of the menu properties file */
	private String menuDescriptor;
	
	/** True if the service requires a license */
	private boolean requiresLicense;
	
	/** 8/13/2019 Custom service properties */
	private Properties properties;
	
	/**
	 * Service descriptor
	 * @param className The name of the class used to load the service. Must implement {@link IServiceLifeCycle}.
	 * @param configFileName Configuration file name (acme.ini).
	 * @param vendorId Id of the vendor. One of: SALESFORCE, AVAYA, ORACLE, LIVEPERSON, GENESYS.
	 * @param vendorName Name of the vendor.
	 * @param menuFile Dynamic menu descriptor file name. Stored in the class-path @ /configuration.
	 */
	public ServiceDescriptor(ServiceType type
			, String className			// Service class, implements IServiceLifeCycle
			, String configFileName		// Service config file name.
			, String vendorId			// Vendor ID: One of: SALESFORCE, AVAYA, ORACLE, LIVEPERSON, GENESYS.
			, String vendorName			// Vendor name
			, String menuFile			// dynamic menu file (/configuration)
			, String requiresLicense 	// True/False if requires license.
		) 
	{
		super();
		this.type			= type;
		this.className 		= className;
		this.configFileName = configFileName;
		this.vendorName 	= vendorName;
		this.vendorId		= vendorId;
		this.menuDescriptor = menuFile;
		this.requiresLicense = requiresLicense != null && !requiresLicense.isEmpty() ? Boolean.parseBoolean(requiresLicense) : false;
	}

	/**
	 * Load a {@link ServiceDescriptor} from {@link Properties}.
	 * @param type {@link ServiceType}.
	 * @param props Service descriptor {@link Properties}.
	 * @throws IOException
	 */
	public ServiceDescriptor(ServiceType type, Properties props	) throws IOException 
	{
		this(type, props.getProperty(KEY_SERVICE_CLASS) // class
				, props.getProperty(KEY_SERVICE_CFG)		// config file name
				, props.getProperty(KEY_VENDOR_ID)			// vendor Id
				, props.getProperty(KEY_SERVICE_VENDOR)		// vendor name
				, props.getProperty(KEY_SERVICE_THEME)		// dynamic menus
				, props.getProperty(KEY_SERVICE_REQLIC)		// True/false if requires a license
				);	
		
		// validate required keys
		if ( getClassName() == null) 		throw new IOException("Missing KEY " + KEY_SERVICE_CLASS );
		if ( getConfigFileName() == null) 	throw new IOException("Missing KEY " + KEY_SERVICE_CFG );
		if ( getVendorId() == null) 		throw new IOException("Missing KEY " + KEY_VENDOR_ID );
		if ( getVendorName() == null) 		throw new IOException("Missing KEY " + KEY_SERVICE_VENDOR );
		
		// optional: menu descriptor
		if ( getMenuDescriptor() == null)				System.err.println(type + " Missing OPTIONAL KEY " + KEY_SERVICE_THEME );
		if ( !props.containsKey(KEY_SERVICE_REQLIC))	System.err.println(type + " Missing OPTIONAL KEY " + KEY_SERVICE_REQLIC);
		this.properties = props;	// 8/13/2019
	}
	
	public String getClassName() {
		return className;
	}

	public String getConfigFileName() {
		return configFileName;
	}


	public String getVendorName() {
		return vendorName;
	}


	public String getVendorId() {
		return vendorId;
	}

	public ServiceType getType() {
		return type;
	}

	public String getMenuDescriptor() {
		return menuDescriptor;
	}

	public boolean requiresLicense () {
		return requiresLicense;
	}
	
	@Override
	public String toString() {
		return "[" + type + " " + vendorName + "/" + vendorId + " @ " + className + "(" + configFileName + ") License: " +  requiresLicense + "]";
	}
	
	public String getProperty ( String key, String def) {
		return properties != null ? properties.getProperty(key, def) : null;
	}
}
