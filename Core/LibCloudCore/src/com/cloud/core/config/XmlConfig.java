package com.cloud.core.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * XML implementation of the {@link IConfiguration} interface.
 * Format:<pre>
 *&lt;configuration>
&lt;version>2&lt;/version>
&lt;property>
	&lt;key>test_00_05_primaryProviderPassword&lt;/key>
	&lt;name>00_05_primaryProviderPassword&lt;/name>
	&lt;value>&lt;![CDATA[Hello World]]>&lt;/value>
	&lt;attributes>
		&lt;label>Provider Password&lt;/label>
		&lt;group>cfg_group00_backend&lt;/group>
		&lt;widget>&lt;![CDATA[password]]>&lt;/widget>
	&lt;/attributes>
&lt;/property>
&lt;property>
	&lt;key>test_backend_capabilities&lt;/key>
	&lt;name>backend_capabilities&lt;/name>
	&lt;value>&lt;![CDATA[CAP_ACD_LOGIN]]>&lt;/value>
	&lt;attributes>
		&lt;label>Backend Capabilities&lt;/label>
		&lt;group>cfg_group01_backend&lt;/group>
		&lt;widget>&lt;![CDATA[dualmultiselect&/configuration/capabilities.ini]]>&lt;/widget>
	&lt;/attributes>
&lt;/property>
&lt;group>
	&lt;name>cfg_group07_lco&lt;/name>
	&lt;title>Last Calls Options&lt;/title>
&lt;/group>
&lt;private>&lt;name>private_TQueue&lt;/name>&lt;value>&lt;/value>&lt;/private>
&lt;/configuration> </pre>

 * <h2>Change log</h2>
 * <ul>
 * <li> 10/26/2019 Added support for include files: &lt;include file="bar.xml" group="cfg_group10_mscrm" sortPrefix="10" /&gt;
 * </ul>
 * See the Cloud Configuration doc in GIT.
 * @author vsilva
 * @version 1.0.1
 */
public class XmlConfig extends BaseConfig  {
	private static final Logger log 		= LogManager.getLogger(XmlConfig.class);
	
	
	/**
	 * SAX Parser Data handler
	 * @author vsilva
	 *
	 */
	private class DataHandler extends DefaultHandler {
		private StringBuffer buffer;
		String key;
		String value;
		boolean inAttribs;
		boolean inGroup;
		
		final String configLocation; //, configResource; 
		final boolean searchFileSystem, searchClassPath , checkForUpdate, checkForIni;
		
		public DataHandler(final String configLocation, /*String configResource,*/ boolean searchFileSystem, boolean searchClassPath, boolean checkForUpdate, boolean checkForIni) {
			this.configLocation 	= configLocation;
			//this.configResource = configResource;
			this.searchFileSystem 	= searchFileSystem;
			this.searchClassPath 	= searchClassPath;
			this.checkForUpdate 	= checkForUpdate;
			this.checkForIni 		= checkForIni; 
		}
		
		@Override
		public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
			buffer = new StringBuffer();
			if ( name.equalsIgnoreCase("attributes")) {
				inAttribs = true;
			}
			if ( name.equalsIgnoreCase("group")) {
				inGroup = true;
			}
			// XML include: <include file="inc_sforce.xml" group="cfg_group11_sfdc" sortPrefix="11" />
			if ( name.equalsIgnoreCase("include")) {
				final String file 	= attributes.getValue("file");			// include XML file name
				final String group 	= attributes.getValue("group");			// group the keys belong to
				final String prefix	= attributes.getValue("sortPrefix");	// Used to keep logical sorting
				
				log.debug("[XML include] file:" + file + " grp:" + group + " pref=" + prefix);
				try {
					XmlConfig cfg 		= new XmlConfig(configLocation, file, searchFileSystem, searchClassPath, checkForUpdate, checkForIni);
					Properties props 	= cfg.getProperties();
					Properties newprops	= new Properties();
					
					for (Map.Entry<Object, Object> entry : props.entrySet()) {
						// fix the key if prefix is available (this is required for sorting)
						final Object key 	= prefix!= null ?  prefix + "_" + entry.getKey(): entry.getKey();
						final Object val	= entry.getValue();
						
						// a group is required for classification
						final String grp	= key.toString() + "_attribute_group";

						// add the key, group
						newprops.put(key, val);
						log.debug(String.format("[XML include %s] %s = %s", file, key, val));

						if ( !key.toString().contains("_attribute_")) {
							log.debug(String.format("[XML include %s] %s = %s ", file, grp , group));
							newprops.put(grp, group);
						}
					}
					// add include (child) attributes to the master
					resource.putAll(newprops);
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			String data = buffer.toString().trim();
			if ( name.equalsIgnoreCase("name")) {
				key = data; 
			}
			else if ( name.equalsIgnoreCase("value")) {
				value = data; 
				resource.setProperty(key, value);
			}
			// Note: Group only. <property> also has <title>
			else if ( name.equalsIgnoreCase("title") && inGroup) {
				value = data; 
				resource.setProperty(key, value);
			}
			else if ( name.equalsIgnoreCase("version")) {
				resource.setProperty(KEY_VERSION, data);
			}
			if ( name.equalsIgnoreCase("attributes")) {
				inAttribs = false;
			}
			if ( name.equalsIgnoreCase("group")) {
				inGroup = false;
			}
			
			// attributes...
			if ( inAttribs) {
				final String attrib = key + "_attribute_" + name;
				resource.setProperty(attrib, data);
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer != null) {
            	buffer.append(ch ,start ,length);
            }
		}
	}

	/**
	 * XML implementation for {@link IConfiguration}. 
	 * @param configLocation XML File path.
	 * @param configResource XML File name.
	 * @throws IOException
	 */
	public XmlConfig(String configLocation, String configResource) throws IOException {
		this(configLocation, configResource, true, true, true, true);
	}
	
	/**
	 * XML implementation for {@link IConfiguration}. 
	 * @param configLocation XML File path.
	 * @param configResource XML File name.
	 * @param searchFileSystem If true search the file system (under location).
	 * @param searchClassPath If true search the class path (under location & /configuration).
	 * @param checkForUpdate If true check for resourceName update between the file system and the class path.
	 * @param checkForIni If true check and update from an .INI file in the file system.
	 * @throws IOException
	 */
	public XmlConfig(String configLocation, String configResource, boolean searchFileSystem, boolean searchClassPath, boolean checkForUpdate, boolean checkForIni) 
			throws IOException 
	{
		super(configLocation, configResource);

		InputStream is	= null;

		log.debug ("XmlConfig Construct Location: " + configLocation + " Resource:" + configResource);

		// Check for ini file. If found backup ini & convert into XML.
		boolean gotIni = checkForIni ? searchForIni(configLocation, configResource) : false;
		
		if ( gotIni ) {
			log.debug ("XmlConfig Construct Loaded config from FILESYSTEM INI.");
			log.warn( configResource +  " @ " + configLocation + " has been upgraded from the DEPRECATED INI format to XML. Please re-save from the admin console!");
		}
		else {
			// parse & load into resource Properties from the FS or CP.
			try {
				SAXParser p = SAXParserFactory.newInstance().newSAXParser();
				is 			= IOTools.findStream(configLocation, configResource, searchFileSystem, searchClassPath);
				
				/* FindBugs 11/29/26 Redundant nullcheck of is, which is known to be non-null in new com.cloud.core.config.XmlConfig(String, String, boolean, boolean, boolean, boolean)
				if ( is == null) {
					throw new Exception("Missing resource " + configResource + " @ " + configLocation);
				} */
				p.parse(is, new DataHandler(configLocation, /*configResource,*/ searchFileSystem, searchClassPath, checkForUpdate, checkForIni));
			}
			catch ( Exception e) {
				throw new IOException(e);
			}
			finally {
				IOTools.closeStream(is);
			}
		}
		// Check for version updates
		if ( checkForUpdate )	{
			check4Update(configLocation, configResource);
		}
	}
	
	/**
	 * Update current {@link XmlConfig} if a config w/ a higher version exists in the class path.
	 * @param configLocation
	 * @param configResource
	 */
	private void check4Update (String configLocation, String configResource) {
		try {
			// Load from class path
			log.debug("==> Check4Update START Loading config from class-path " + IOTools.BASE_CLASSPATH_CONFIG + " " + configResource);
			
			XmlConfig cpXml 	= new XmlConfig(IOTools.BASE_CLASSPATH_CONFIG, configResource, false, true, false, false);
			
			int cpVer  			= cpXml.getVersion();
			int fsVer 			= getVersion();
			previousVersion 	= fsVer;
			
			log.debug("Check4Update FileSystem: " + configLocation + " " + configResource + " Ver:" + fsVer
					+ " Class Path:" + cpXml.getLocation() + " Ver:" + cpVer );
			
			// update if CP > current (FS)
			if ( cpVer > fsVer ) {
				log.debug("Check4Update CP Version " + cpVer + " > " + fsVer + ". Updating.");
				
				Properties cp = cpXml.getProperties();
				
				// load current props (resource) into the class path (newer)
				updateProperties(resource, cp );
				loadFrom(cp);			// refresh from CP
				
				versionUpdated = true;
			}
			else {
				log.debug("Check4Update CP version (" + cpVer + ") is less than or equal to FS (" + fsVer + "). Nothing done.");
				versionUpdated = false;	
				cleanUpdated();
			}
			log.debug("==> Check4Update END");
		} 
		catch (IOException e) {
			log.error("XMLConfig check 4 update failed", e);
		}
	}
	

	/**
	 * Look 4 an INI file in the file system then back it up, and upgrade to XML.
	 * @param configLocation
	 * @param configResource
	 */
	private boolean searchForIni (String configLocation, String configResource) {
		try {
			// look 4 ini
			log.debug("Look4AndBackup INI Looking for a FILESYSTEM INI file for " + configLocation + " XML:" + configResource);
			
			String iniFile = FileTool.getFileNameWithoutExtension(configResource) + ".ini";
			String iniPath = configLocation + File.separator + iniFile;
			
			if ( ! FileTool.fileExists(iniPath)) {
				log.warn("Look4AndBackup INI " + iniPath + " not found. Abort.");
				return false;
			}
			log.debug("Look4AndBackup FILESYSTEM INI Found: " + iniPath);
			InputStream fsin = IOTools.findStream(configLocation, iniFile, true, false);
			
			resource.load(fsin);
			
			// close stream or Backup will fail!
			IOTools.closeStream(fsin);
			
			// backup ini
			log.debug("Look4AndBackup Attempting backup " + iniPath + " to " + FileTool.fileBackUpGetFileName(iniPath));
			FileTool.fileBackUp(iniPath);
			return true;
			
		} catch (Exception e) {
			log.error("Backup INI failed for " + configLocation , e);
			return false;
		}
	}
	

	private void saveInternal (String localPath, String comment, String xml) throws FileNotFoundException, IOException {
    	FileOutputStream fos 	= new FileOutputStream(localPath);
    	OutputStreamWriter ow 	= new OutputStreamWriter(fos, IOTools.DEFAULT_ENCODING);
    	ow.write(xml);
    	ow.close();
    	log.debug("SaveInternal: Saved config @ " + localPath);
	}

	@Override
	public void save(String comment) throws FileNotFoundException, IOException {
		// File system path.
		String localPath 	= basePath + File.separator + baseName;
		String classPath 	= basePath + IConfiguration.PATH_SEP + baseName; // must use /
		
		// use the ServiceConfiguration to build the XML
		final String xml	= toString(); 
		
    	/** Save config resource with Unicode support charset: UTF-8 */
    	try {
        	log.debug("Save: Trying FILESYSTEM  (" + baseName + ") @ " + localPath);
    		saveInternal(localPath, comment, xml);
		} catch (FileNotFoundException e) {
			log.warn("Save: FILESYSTEM save attempt FAILED: " + e.toString() + " Trying class path [" + classPath + "]");
			saveInternal(IOTools.getResourceAbsolutePath(classPath), comment, xml);
		}
		
	}

	
	@Override
	public String toString() {
		// use the ServiceConfiguration to build the XML
		final ServiceConfiguration wrapper = new ServiceConfiguration(resource);
		return wrapper.toXml();
	}

}
