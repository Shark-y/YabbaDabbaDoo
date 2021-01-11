package com.cloud.core.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.console.ThemeManager;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.ServiceContext;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceStatus;
import com.cloud.security.CloudSecurity;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.services.ServiceDescriptor.ServiceType;

/**
 * A dead simple plugin system:
 * <ul>
 * <li> Scans all JARs webapp lib folder for plugin.json
 * <li> Manages the plugin life cycle using the {@link IServiceLifeCycle}.
 * </ul>
 * <pre>
 * {
	"service.class" : "com.cluster.ClusterDaemon",
	"service.config" : "cluster_mgr.xml",
	"service.vendor" : "Cluster Manager",
	"service.vendorId" : "C1ASCM",
	
	"ui.sideBarMenus": [
		{
			"id": "00_cmCfg",
			"label" : "Cluster Manager",
			"parent" : "Configuration",
			"url" : "jsp/config/config_backend.jsp?mode=PLUGIN&id=C1ASCM",
			"iconCss" : ""
		},
		{
			"id": "00_docker",
			"label" : "Docker",
			"parent" : "/",
			"url" : "",
			"iconCss" : "directions_boat"
		},
	]
} </pre>

 * @author VSilva
 * @version 1.0.0 5/21/2020 Initial implementation.
 *
 */
public class PluginSystem {

	private static final Logger  log 	= Logger.getLogger(PluginSystem.class);
	
	static void LOGD(final String text) {
		//System.out.println("[PLUGIN] " + text);
		log.debug(text);
	}
	static void LOGE(final String text) {
		//System.err.println("[PLUGIN] " + text);
		log.error(text);
	}
	
	public static class Plugin {
		final String id;
		final String file;
		Object instance;
		JSONObject json;
		
		public Plugin(/*final String id,*/ final String json, final String file) throws JSONException {
			super();
			this.json 	= new JSONObject(json);
			this.id	 	= this.json.getString("service.vendorId"); // id;
			this.file 	= file;
		}
		
		@Override
		public String toString() {
			return file + " " + json;
		}
		
		public String getName() {
			return json.optString("service.vendor");
		}

		public String getVersion() {
			return json.optString("version");
		}

		public String getServiceClass() {
			return json.optString("service.class");
		}
		
		public String getVendorId() {
			return id;
		}

		public Object getInstance() {
			return instance;
		}
		
		public ServiceStatus getStatus () {
			return ((IServiceLifeCycle)instance).getServiceStatus();
		}
		
		public ServiceDescriptor toService() throws JSONException {
			ServiceDescriptor sd = new ServiceDescriptor(
					ServiceType.PLUGIN
					, json.getString("service.class")
					, json.getString("service.config") 
					, id
					, json.getString("service.vendor")
					, null
					, "false");
			return sd;
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject root = new JSONObject();
			root.put("id", id);
			root.put("name", getName());
			root.put("version", getVersion());
			root.put("status", getStatus().toString());
			return root;
		}
	}
	
	/** Plugin list */
	private static final List<Plugin> plugins = new ArrayList<Plugin>();
	
	/*
	 * Guess the plugin folder using class.getProtectionDomain().getCodeSource()
	 */
	private static String getPluginFolder () throws URISyntaxException {
		// file:/C:/Temp/Workspaces/CloudServices/Cloud-UnifiedContactCenter/.metadata/.plugins/org.eclipse.wst.server.core/tmp7/wtpwebapps/Test/WEB-INF/classes/
		File cls 	= new File(PluginSystem.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		File lib 	=  cls.isFile() 
				? new File(FileTool.getBasePath(cls.getPath()))
				: new File(cls.getPath() + File.separator + ".." + File.separator + "lib");
				
		LOGD("PLUGIN CLS folder:" + cls);
		LOGD("PLUGIN LIB folder:" + lib);
		return lib.getPath();
	}
	
	private static File[] getJars () throws URISyntaxException {
		final String path 	= getPluginFolder();
		File[] jars 		= FileTool.listFiles(path, new String[] {"jar"}, null);
		return jars;
	}

	private static void loadPlugins(File[] jars) {
		// load plugins- loop thru jars
		for (File jar : jars) {
			//System.out.println(jar);

			try {
				// walk thru / within jar looking for plugin.json
				FileSystem fs 		= FileSystems.newFileSystem(Paths.get(jar.getPath()), null);
				Stream<Path> walk 	= Files.walk(fs.getPath("/"), 1);
				//String id			= EncryptionTool.HASH(jar.getPath());
				
				for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
					Path path = it.next();
					Path file = path.getFileName();
					
		            //System.out.println("\t" + path + " f:" + file);
		            
		            // read plugin.json from JAR
		            if ( file != null && file.endsWith("plugin.json")) {
		            	List<String> lines 	= Files.readAllLines(path);
		            	String json 		= IOTools.join(lines.toArray(), "");
		            	Plugin p 			= new Plugin(/*id,*/ json, jar.getAbsolutePath());
		            	plugins.add(p);
		            	
		            	LOGD("ADDED PLUGIN " + p.getName() + " from " + jar);
		            }
		            
		            // process security descriptor (security.json)
		            if ( file != null && file.endsWith("security.json")) {
		            	List<String> lines 		= Files.readAllLines(path);
		            	final String json 		= IOTools.join(lines.toArray(), "");
		            	final String resource 	= FileTool.getFileName(jar.getPath());
		            	
		            	LOGD("ADDING security descriptor (security.json) from " + resource);
		            	
		            	CloudSecurity.addDescriptor(resource, new JSONObject(json));
		            }
		        }
			} 
			catch (Exception e) {
				log.error("Load plugin from " + jar, e);
			}
		}
	}
	
	/**
	 * <ul>
	 * <li> Scan lib JARS for plugins
	 * <li> Load plugin UI elements into the {@link ThemeManager}.
	 * </ul>
	 */
	public static void initialize () {
		try {
			File[] jars = getJars();
			
			if ( jars == null ) {
				LOGE("Got zero libs! Abort plugin load");
				return;
			}
			loadPlugins(jars);
			
			// load ui elements into the theme manager
			ThemeManager mgr = ThemeManager.getInstance();
			
			for (Plugin plugin : plugins) {
				JSONArray pmenus = plugin.json.optJSONArray("ui.sideBarMenus");
				
				if ( pmenus != null) {
					mgr.load(pmenus);
				}
			}
			
			LOGD("LOADED " + plugins.size() + " PLUGINS ");
			//mgr.dumpMenus("PLUIGIN INIT");
		} 
		catch (Exception e) {
			log.error ( "Initialize plugin system", e);
		}
	}
	
	private static ServiceContext getDefaultContext () {
		NodeConfiguration config = CloudServices.getNodeConfig();
		ServiceContext context = new ServiceContext(
				config.getConfigLocation()				// Base cfg path: $user.home/.cloud/CloudAdapter
				,  //profile != null && !profile.isEmpty()
					/*?*/ config.getDefaultProfileBasePath() // $user.home/.cloud/CloudAdapter/Profiles/{PROFILE} 
					//: null
				, CloudServices.getServices()
				, config
				, config.isClusterEnabled() ? CloudCluster.getInstance().getClusterInstance() : null
				, CloudServices.isNodeConfigured()
				);
		return context;
	}
	
	/**
	 * Invoke the {@link IServiceLifeCycle} onServiceInitialize() on each plugin.
	 */
	public static void initPlugins () {
		
		for (Plugin p : plugins) {
			try {
				String srvClass = p.getServiceClass(); 
				
				if ( srvClass != null ) {
					p.instance = Class.forName(srvClass).newInstance();
					
					if ( p.instance instanceof IServiceLifeCycle) {
						ServiceContext context = getDefaultContext();
						((IServiceLifeCycle)p.instance).onServiceInitialize(context);
					}
				}
			} catch (Exception e) {
				log.error ( "Initialize plugin " + p.getName(), e);
			}
		}
	}

	/**
	 * Invoke the {@link IServiceLifeCycle} onServiceStart() on each plugin.
	 */
	public static void startPlugins () {
		for (Plugin p : plugins) {
			try {
				if ( p.instance != null ) {
					if ( p.instance instanceof IServiceLifeCycle) {
						ServiceContext context = getDefaultContext();
						((IServiceLifeCycle)p.instance).onServiceStart(context);
					}
				}
			} catch (Exception e) {
				log.error ( "Start plugin " + p.getName(), e);
			}
		}
	}
	
	/**
	 * Invoke the {@link IServiceLifeCycle} onServiceStop() on each plugin.
	 */
	public static void stopPlugins () {
		for (Plugin p : plugins) {
			try {
				if ( p.instance != null ) {
					if ( p.instance instanceof IServiceLifeCycle) {
						((IServiceLifeCycle)p.instance).onServiceStop();
					}
				}
			} catch (Exception e) {
				log.error ( "Stop plugin " + p.getName(), e);
			}
		}
	}

	/**
	 * <pre>
	 *  {"data":[["11016","org.apache.catalina.startup.Bootstrap start","sun"],["6328","","sun"],["9500","org.ets.ibt.delivery.grepractice.Launcher","sun"]]}
	 *  {"data":[["d25cf66b86f02c0520b55802c7a021ec7e52f139dadbd8e1803b186200f217a6","Cluster Manager","ON_LINE Up"]]}
	 *  </pre>
	 */
	public static JSONObject describe() {
		JSONObject root = new JSONObject();
		JSONArray data = new JSONArray();
		
		try {
			// id, name, status
			for (Plugin p : plugins) {
				/* 5/31/2020
				JSONArray row = new JSONArray();
				row.put(p.id);
				row.put(p.getName());
				row.put(((IServiceLifeCycle)p.instance).getServiceStatus().toString());
				*/
				data.put(p.toJSON()); // row);
			}
			root.put("data", data);
		} catch (Exception e) {
			log.error ( "Describe plugin system ", e);
		}
		return root;
		
	}
	
	/**
	 * Save plugin {@link InputStream} into the WebApp LIB folder
	 * @param name Plugin name.
	 * @param in Plugin JAR stream contents.
	 */
	public static void savePlugin(final String name, InputStream in) {
		try {
			final String path 	= getPluginFolder() + File.separator + name;
			LOGD("Save " + name + " to " + path);
			
			FileOutputStream out = new FileOutputStream(path);
			IOTools.pipeStream(in, out);
			IOTools.closeStream(in);
			IOTools.closeStream(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ServiceDescriptor findServiceDescriptor (String id) throws JSONException {
		for (Plugin p : plugins) {
			if ( p.getVendorId().equals(id)) {
				return p.toService();
			}
		}
		return null;
	}
	
	/**
	 * Get a {@link ServiceConfiguration} by id.
	 * @param id Service id (service_vendorId from the product ini file)
	 * @return See {@link ServiceConfiguration}.
	 */
	public static ServiceConfiguration getServiceConfig (final String id) {
		try {
			NodeConfiguration config 	= CloudServices.getNodeConfig();
			String basePath 			= config.getDefaultProfileBasePath(); 
			ServiceDescriptor desc 		= findServiceDescriptor(id); // config.findServiceDescriptor(type);
			
			LOGD("Get service " + id + " Config Base: " + basePath + " File: " + desc.getConfigFileName());
			
			return new ServiceConfiguration(basePath, desc.getConfigFileName() , desc.getType().name());
		} catch (Exception e) {
			log.error("Unable to load service config for id " + id , e);
			return null;
		}
	}
	
	/**
	 * Find a plugin by the unique service class.
	 * @param serviceClass Full class name of the plugin.
	 * @return See {@link Plugin}.
	 */
	public static Plugin findInstance(final String serviceClass) {
		for (Plugin p : plugins) {
			if ( p.getServiceClass().equals(serviceClass)) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Shutdown the plugin system. Invoke on container destroy.
	 */
	public static void shutdown () {
		for (Plugin p : plugins) {
			try {
				if ( p.instance != null ) {
					if ( p.instance instanceof IServiceLifeCycle) {
						((IServiceLifeCycle)p.instance).onServiceDestroy();
					}
				}
			} catch (Exception e) {
				log.error ( "Shutdown plugin " + p.getName(), e);
			}
		}
	}

}
