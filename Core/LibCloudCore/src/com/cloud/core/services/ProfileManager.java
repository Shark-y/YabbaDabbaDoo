package com.cloud.core.services;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Profile Manager is meant control connection profiles under:
 * $USER_HOME/.cloud/CloudAdapter/Profiles
 * @author VSilva
 *
 */
public class ProfileManager implements Serializable 
{
	private static final long serialVersionUID 		= 3343561528711051773L;

	private static final Logger log 				= LogManager.getLogger(ProfileManager.class);
	
	private static final String BASE_PROFILE_FOLDER	= "Profiles";
	
	// base location of the configuration $HOME/.cloud/CloudAdapter
	private String basePath;

	// List of all profiles under basePath
	private List<ProfileDescriptor> profiles;
	
	/**
	 * Single profile descriptor.
	 * @author VSilva
	 *
	 */
	public static class ProfileDescriptor implements Serializable {
		private static final long serialVersionUID = -2345874974177815063L;
		
		public String name;		// profile name
		public String path;		// base path $user.home/.cloud/CloudAdapter/Profiles/{NAME}
		
		@Override
		public String toString() {
			return "[" + name + " @ " + path + "]";
		}
	}
	
	private static void LOGD(String text) {
		//System.out.println(text);
		log.debug(text);
	}

	private static void LOGE(String text) {
		//System.err.println(text);
		log.error(text);
	}
	
	/**
	 * Constructor.
	 * @param basePath Base folder location: $HOME/.cloud/CloudAdapter
	 */
	public ProfileManager(String basePath) throws IOException  { 
		super();
		this.basePath 	= basePath;
		this.profiles	= new ArrayList<ProfileManager.ProfileDescriptor>();

		loadProfiles(); 
	}

	/*
	 * Load all profiles from the base path.
	 */
	private void loadProfiles() throws IOException { 
		// profiles are all folders under basePath + "/Profiles";
		File dir = new File(basePath + File.separator + BASE_PROFILE_FOLDER);
		
		LOGD("Profiles Base: " + dir);
		
		// create basePath/Profiles if missing.
		if ( !dir.exists()) {
			if ( !dir.mkdirs() ) {
				LOGE("Load Profiles: Failed to create dir " + dir);
				// Can't create profiles dir? This is a fatal error!
				throw new IOException("Profile Manager: Failed to create dir " + dir );
			}
		}

		if ( !dir.isDirectory()) {
			LOGE("Load Profiles: File " + dir + " is NOT a directory");
			throw new IllegalArgumentException("Load Profiles: File " + dir + " is NOT a directory");
		}

		File[] profilePaths = IOTools.getDirectories(dir);
		
		for (File file : profilePaths) {
			loadSingleProfile(file.getAbsolutePath()) ; 
		}
	}

	/**
	 * Load a single profile from the base path.
	 * @param basePath Base: $HOME/.cloud/CloudAdadpter/Profiles.
	 * @param mbResource {@link IMessageBroker} ini file name.
	 * @param ccResource {@link IContactCenter} ini file name.
	 */
	private void loadSingleProfile (String basePath) { 
		try {
			LOGD("Loading profile " + basePath ); 
			File f = new File(basePath);
			
			ProfileDescriptor pd 	= new ProfileDescriptor();
			pd.name					= f.getName();
			pd.path					= basePath;

			profiles.add(pd);
		} catch (Exception e) {
			LOGE("Profile " + basePath + " failed with " + e.toString());
		}
	}
	
	public void dump(String label) {
		LOGD("----- PROFILES " + label + " SIZE (" + getSize() + ") ---------");
		for (ProfileDescriptor pd : profiles) {
			LOGD(pd.toString());
		}
		LOGD("-------------- PROFILES ----------------");
	}
	
	/**
	 * Find a profile by name.
	 * @param name Profile name
	 * @return {@link ProfileDescriptor} or null if not found.
	 */
	public ProfileDescriptor find (String name) {
		if ( name == null )
			return null;
		
		for (ProfileDescriptor pd : profiles) {
			if ( pd.name != null ) { 
				if ( !IOTools.OS_IS_WINDOWS ) {
					if ( pd.name.equals(name)) {
						return pd;
					}
				}
				else {
					if ( pd.name.equalsIgnoreCase(name)) {
						return pd;
					}
				}
			}
		}
		return null;
	}
	
	public int getSize() {
		return profiles.size();
	}
	
	public boolean isEmpty () {
		return profiles.size() == 0;
	}
	
	public void reload() {
		LOGD("Profiles Reload Base: " + basePath); 
		profiles.clear();
		try {
			loadProfiles();
		} 
		catch (IOException e) {
			log.error("Failed to reload profiles", e);
		} 
	}
	
	/**
	 * Get all profile names.
	 * @return An array list of profile names.
	 */
	public List<String> getNames() {
		List<String> names = new ArrayList<String>();
		for (ProfileDescriptor pd : profiles) {
			names.add(pd.name);
		}
		return names;
	}
	
	/**
	 * Delete a profile from the file system.
	 * @param name profile name.
	 * @throws IOException
	 */
	public void delete (String name ) throws IOException {
		ProfileDescriptor pd = find(name);
		
		if ( pd == null) {
			throw new IOException("Unable to delete " + name + ": Profile not found.");
		}
		
		File dir = new File(pd.path);
		LOGD("Profile delete: " + dir);
		
		if ( ! FileTool.deleteTree(dir, true) ) {
			//LOGE("Unable to delete " + dir);
			throw new IOException("Unable to delete " + name + ": Delete failed for " + dir);
		}
		// remove from list
		profiles.remove(pd);
	}
	
	/**
	 * Profile exists?
	 * @param name
	 * @return true if exists under $user.home/.cloud/{SERVICE}/Profiles/{NAME}
	 */
	public boolean exists (String name) {
		return find(name) != null;
	}

	/**
	 * Add a new empty profile. This will not store any data on disk.
	 * @param name Profile name.
	 * @return A {@link ProfileDescriptor} for the newly created profile.
	 * @throws IOException If an error creating the profile occurs.
	 */
	public ProfileDescriptor add(String name) throws IOException {
		ProfileDescriptor found = find(name);
		
		if ( found != null) {
			throw new IOException(name + " already exists.");
		}
		
		ProfileDescriptor pd = new ProfileDescriptor();

		pd.name = name;
		pd.path = basePath + File.separator + BASE_PROFILE_FOLDER + File.separator + name;
		
		
		LOGD("Profile Add: " + pd);
		
		// create folder (if missing)
		File dir = new File(pd.path);
		
		if ( ! dir.exists() ) {
			if ( !dir.mkdirs() )
				throw new IOException("Unable to create " + name);
		}
		profiles.add(pd);
		return pd;
	}
	
	public List<ProfileDescriptor> getProfiles() {
		return profiles;
	}
	
	/**
	 * Get the base path for a single profile.
	 * @param name Profile name.
	 * @return Full path for the profile: $user.home/.cloud/CloudAdapter/Profiles/{NAME}
	 */
	public String getBasePath(String name) {
		ProfileDescriptor pd = find(name);
		
		if ( pd == null) {
			log.error("Can't find profile " + name + " in base folder " + getProfilesHome());
			return null;
		}
		return pd.path;
	}
	
	/**
	 * Get the profiles home directory.
	 * @return Profiles home: $user.home/.cloud/CloudAdapter/Profiles
	 */
	public String getProfilesHome() {
		return basePath + File.separator + BASE_PROFILE_FOLDER;
	}
	
	/**
	 * Duplicate two profile names. Use names only. 
	 * @param src Source name (NOT A full path).
	 * @param dst Destination name.
	 * @throws IOException
	 */
	public void copy(String src, String dst) throws IOException {
		LOGD("Duplicate src=" + src + " dst=" + dst);
		
		ProfileDescriptor pds = find(src);
		ProfileDescriptor pdd = find(dst);
		
		if ( pds == null) {
			throw new IOException(src + " not found.");
		}
		if ( pdd == null)
			pdd = add(dst);
		
		StandardCopyOption[] opts = { StandardCopyOption.REPLACE_EXISTING };
		FileTool.copyTree(new File(pds.path).toPath(), new File(pdd.path).toPath(), opts);
	}

	
}
