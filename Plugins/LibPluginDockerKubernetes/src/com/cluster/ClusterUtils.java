package com.cluster;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.cloud.cluster.zeroconf.ZeroClusterInstance;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.io.ZipTool;

/**
 * Misc utility functions for the {@link ClusterManager} console.
 * 
 * @author VSilva
 * @version 1.0.1 1/16/2019 Derby stuff has been removed.
 *
 */
public class ClusterUtils {
	
	private static final Logger log = Logger.getLogger(ClusterUtils.class);

	static void LOGI(final String text) {
		log.info(text);
		//System.out.println(text);
	}
	
	/**
	 * Install the default cluster DB etc/clusterdb to $home/.cloud/CloudAdapter
	 * @deprecated 1/16/2019 Derby functionality has been replaced by {@link ZeroClusterInstance}
	 * @throws IOException 
	 */
	private static void derbyDeployDefaultClusterDB(final String baseFolder) throws IOException {
		final String destPath = baseFolder; 
		
		if ( FileTool.fileExists(destPath + File.separator + "clusterdb") ) {
			LOGI("Cluster Database already installed @ " + destPath);
			return;
		}
		
		// Path to  clusterdb.zip within webapp CloudClusterManager/WEB-INF/classes/
		final String zipPath = IOTools.getResourceAbsolutePath("/../../etc/clusterdb.zip");
		
		LOGI("Installing default cluster DB " + zipPath + " to " + destPath);
		ZipTool.unzip(zipPath, destPath);
	}
	
	/**
	 * Set the Derby system home (derby.system.home) so the NetworkServer servlet can find the database.
	 * @param basePath Defaults to $home/.cloud/CloudAdapter
	 */
	public static void derbySetSysHome (final String basePath) {
		LOGI("Setting the defult Derby system home (derby.system.home) to " + basePath); 
		System.setProperty("derby.system.home", basePath);
	}
	
	/**
	 * Initialize the embedded Database (DERBY/Java DB)
	 * @param basePath  Database home: default $home/.cloud/CloudAdapter/clusterdb
	 * @deprecated 1/16/2019 Derby functionality has been replaced by {@link ZeroClusterInstance}
	 * @throws IOException on I/O errors.
	 */
	public static void initalizeEmbededDatabase (final String basePath) throws IOException {
		derbySetSysHome(basePath);
		derbyDeployDefaultClusterDB(basePath);
	}

}
