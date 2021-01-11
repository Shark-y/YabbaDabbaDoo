package com.cloud.core.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.XmlConfig;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;


/**
 * Configuration resource reader. It implements {@link IConfiguration}.
 * Reads information from a java {@link Properties} file.
 * @deprecated Properties based configuration files are deprecated in favor of XML format: {@link XmlConfig}.
 * @author sharky
 *
 */
public class PropertiesConfig /*implements IConfiguration*/ extends BaseConfig {
	private static final Logger log 	= LogManager.getLogger(PropertiesConfig.class);
	
	/**
	 * Configuration wrapper. Reads a configuration from a {@link Properties} file by default.
	 * Other sources can be added in the future.
	 * @param location. Base path.
	 * @param resourceName resource name in the file system or class path.
	 */
	public PropertiesConfig(String location, String resourceName) throws IOException {
		this(location, resourceName, true, true, true);
	}

	public PropertiesConfig(String location, String resourceName, boolean checkForUpdate) throws IOException {
		this(location, resourceName, true, true, checkForUpdate);
	}
	
	/**
	 * Constructor.
	 * @param location Base path of the configuration.
	 * @param resourceName Name of the configuration file.
	 * @param searchFileSystem If true search the file system (under location).
	 * @param searchClassPath If true search the class path (under location & /configuration).
	 * @param checkForUpdate If true check for resourceName update betwenn the file system and the class path.
	 * @throws IOException If resource not found.
	 */
	public PropertiesConfig(String location, String resourceName, boolean searchFileSystem, boolean searchClassPath, boolean checkForUpdate) 
			throws IOException 
	{
		super(location, resourceName);
		log.debug("New Configuration: " + location + " - " + resourceName + " SearchFS: " + searchFileSystem + " SearchCP: " + searchClassPath);
		
		Properties updated =  checkForUpdate ? check4Update(location, resourceName) : null;

		if ( updated != null ) {
			loadFrom(updated);
		}
		else {
			// Note: Must use & close an InputStream so the file won't be locked!
			InputStream inputStream = IOTools.findStream(location, resourceName, searchFileSystem, searchClassPath);
			resource.load(inputStream);
			inputStream.close();
		}
	}

	public PropertiesConfig( Properties properties ) {
		super(properties);
	}
	
	/**
	 * Check 4 updates to the {@link IConfiguration} resource by comparing the file system version
	 * with the class path version of the config resource.
	 * 
	 * @param configPath Base path.
	 * @param configResource File name.
	 * 
	 * @return Updated {@link Properties}.
	 */
	private Properties check4Update(String configPath, String configResource)  {
		Properties fs = new Properties();
		Properties cp = new Properties();
		
		log.debug("==> Check4Update START (" + configPath + ") " + configResource);
		
		InputStream is1 = null;		// resource @file system?
		InputStream is2 = null;		// resource @ class path?
		
		// look in the file system only
		try {
			is1 = IOTools.findStream(configPath, configResource, true, false );
			fs.load(is1);
			
		} catch (Exception e) {
			log.warn("Check4Update " + e.toString());
		}
		finally {
			try {
				if ( is1 != null) is1.close();
			} catch (IOException e) {
			} 
		}
		
		// look in the CP.
		try {
			is2 = IOTools.findStream(configPath, configResource, false, true ); 
			cp.load(is2);
		} catch (Exception e) {
			log.error("Check4Update " + e.toString());
		}
		finally {
			try {
				if ( is2 != null ) is2.close();
			} catch (IOException e) {
			}
		}
		
		
		if ( fs != null && cp != null) {
			int fsv 			= fs.getProperty(KEY_VERSION) != null ? Integer.parseInt(fs.getProperty(KEY_VERSION)) : 0 ;
			int cpv 			= cp.getProperty(KEY_VERSION) != null ? Integer.parseInt(cp.getProperty(KEY_VERSION)) : 0 ;
			previousVersion 	= fsv;

			if ( cpv > fsv ) {
				log.debug("Check4Update UPDATING (" + configPath + ") " + configResource + " CPV (" + cpv + ") > FSV (" +  fsv + ")" );
				
				updateProperties(fs, cp);
				versionUpdated = true;
				
				log.debug("==> Check4Update END Returning updated props."); 
				return cp;
			}
			else {
				log.warn("Check4Update: (" + configPath + ") " + configResource + ". Unable to update. CP version (" + cpv + ") must be > FS version (" + fsv + ")");
			}
		}
		else {
			log.debug("Check4Update: (" + configPath + ") " + configResource + ". Unable to check 4 update (FS/CP streams are null)");
		}
		
		log.debug("==> Check4Update END Returnig null.");
		return null;
	} 

	
	private void saveInternal (String localPath, String comment) throws FileNotFoundException, IOException {
    	FileOutputStream fos 	= new FileOutputStream(localPath);
    	OutputStreamWriter ow 	= new OutputStreamWriter(fos, IOTools.DEFAULT_ENCODING);

    	resource.store(fos, comment);
    	ow.close();
    	log.debug("Saved config resource @ " + localPath);
	}
	
	/**
	 * Attempt to save the {@link PropertiesConfig} to the file system only.
	 * @param
	 */
	@Override
	public void save(String comment) throws FileNotFoundException, IOException {
		// File system path.
		String localPath 	= basePath + File.separator + baseName;
		String classPath 	= basePath + IConfiguration.PATH_SEP + baseName; // must use /
		
    	/** Save config resource with Unicode support charset: UTF-8 */
    	try {
        	log.debug("Backend FILESYSTEM config save (" + baseName + ") @ " + localPath);
    		saveInternal(localPath, comment);
		} catch (FileNotFoundException e) {
			log.warn("Backend FILESYSTEM config save FAILED: " + e.toString() + " Trying class path [" + classPath + "]");
			saveInternal(IOTools.getResourceAbsolutePath(classPath), comment);
		}
	}
	
}
