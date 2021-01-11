package com.cloud.core.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;


/**
 * Miscellaneous IO helper tools.
 * @author vsilva
 * @version 1.0.1 7/11/2017 - New method to save character data.
 */
public class IOTools {
	
	private static final Logger log 					= LogManager.getLogger(IOTools.class);
	
	/**
	 * OS Helper vars.
	 */
	public static final String 	OS_NAME					= System.getProperty("os.name");
	public static final boolean OS_IS_WINDOWS 			= OS_NAME.toLowerCase().contains("windows");
	public static final String 	USER_HOME				= System.getProperty("user.home");
	public static final String 	BASE_CLASSPATH_CONFIG 	= "/configuration";
	
	public static final String 	DEFAULT_ENCODING		= "UTF-8";
	
	/**
	 * Get the base path of a resource available in the classpath
	 * @return Full path of the class-path resource.
	 * @throws UnsupportedEncodingException 
	 */
	public static String getResourceAbsolutePath(String resourceName) throws UnsupportedEncodingException {
    	URL url 	= IOTools.class.getResource(resourceName);
    	String path = URLDecoder.decode(url.getFile(), DEFAULT_ENCODING);
    	
    	// path -> Windows: /C:/Temp/Workspaces/.../30-LP-Genesys/
    	// path-> Linux: 	/home/users/foo...
    	if ( path.startsWith("/") && OS_IS_WINDOWS) {
    		// gotta remove the first / in Windows only!
    		path = path.replaceFirst("/", "");
    	}
    	return path;
	}

	/**
	 * Get the host name. Invokes InetAddress.getLocalHost().getHostName().
	 * @return name returned by InetAddress.getLocalHost().getHostName()
	 */
	static public String getHostname() {
		if ( OS_IS_WINDOWS)
			return getHostnameWin32();
		return getHostnameLinux();
	}

	/**
	 * Get the local host IP.
	 * @return
	 */
	static public String getHostIp() {
		if ( OS_IS_WINDOWS)
			return getHostIpWin32();
		return getHostnameLinux();
	}

	/**
	 * The the name of the current local host. Invokes InetAddress.getLocalHost().getHostName().
	 * @return Host name from InetAddress.getLocalHost().getHostName()
	 */
	static public String getHostnameWin32() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "unknown";
		}
	} 

	/**
	 * Get the local host address.
	 * @return Address returned by InetAddress.getLocalHost().getHostAddress().
	 */
	static public String getHostIpWin32() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "unknown";
		}
	} 

	/**
	 * Get the host name by scanning all interfaces in NetworkInterface.getNetworkInterfaces().
	 * <p>Linux: May NOT return the correct host name.</p>
	 * @return The host name of the first address that is NOT a loopback in NetworkInterface.getNetworkInterfaces() or localhost if not suitable address found.
	 */
	static public String getHostnameLinux() {
		String hostName = null;
		try {
			// look @ all the interfaces
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			
		    while (interfaces.hasMoreElements()) {
		        NetworkInterface nic 				= interfaces.nextElement();
		        Enumeration<InetAddress> addresses 	= nic.getInetAddresses();
		        
		        // look @ all the addresses for that interface
		        while (hostName == null && addresses.hasMoreElements()) {
		            InetAddress address = addresses.nextElement();
		            
		            if (!address.isLoopbackAddress() && !address.getHostName().contains(":")) {
		                //hostName = address.getHostName();
		            	return address.getHostName();
		            }
		        }
		    }
		} catch (Exception e) {
			log.error("LINUX Get Hostname failed with " + e.toString());
		}
		if ( hostName == null ) hostName = "localhost";
	    return hostName;
	}
	
	

	/**
	 * Join an array of strings.
	 * @param array Array of strings.
	 * @param sep Join Separator.
	 * @return
	 */
	static public String join(String[] array, String sep) {
		if (array.length == 0) 
			return "";
		
		StringBuilder sb = new StringBuilder();
		int i;
		
		for( i=0; i < array.length - 1 ;i++)
		    sb.append(array[i] + sep);
		
		return sb.toString() + array[i];
	}
	
	static public String join(Object[] array, String sep) {
		if (array.length == 0) 
			return "";
		
		StringBuilder sb = new StringBuilder();
		int i;
		
		for( i=0; i < array.length - 1 ;i++)
		    sb.append(array[i] + sep);
		
		return sb.toString() + array[i];
	}

	
	public static List<String> sort(Enumeration<String> enumeration) {
    	List<String> list = Collections.list(enumeration);
    	Collections.sort(list);
    	return list;
	}

	/**
	 * Get an {@link InputStream} from the file system or the class path.
	 * @param resourcePath Path in the file system or class path.
	 * @param resourceName Optional name of a file (appended to resourcePath). May be null.
	 * @return {@link InputStream}
	 * @throws IOException
	 */
	static public InputStream findStream(String resourcePath, String resourceName) throws IOException {
		return findStream(resourcePath, resourceName, true, true);
	}
	
	/**
	 * Get an {@link InputStream} from the file system or the class path.
	 * 
	 * @param resourcePath File system or class path.
	 * @param resourceName Optional name of a file (appended to resourcePath).m (OPTIONAL) - May be null.
	 * @param searchFileSystem If true search the file system for resourceName.
	 * @param searchClassPath If true the class path for resourceName.
	 * 
	 * @return {@link InputStream}
	 * @throws IOException
	 */
	static public InputStream findStream(String resourcePath, String resourceName, boolean searchFileSystem, boolean searchClassPath) throws IOException {
		/*if ( resourcePath == null) 
			throw new IOException("A resource path is required.");*/
		
		String resource =  resourceName != null ? resourcePath + File.separator + resourceName : resourcePath;
		try {
			if ( !searchFileSystem) {
				//log.warn("FindStream: Failed to load " + resource + " from File System. FS search is disabled."); 
				throw new IOException("File system search disabled.");
			}
			
			// Look in the file system first (could be a path c:\....)
			log.debug("FindStream: Loading " + resource + " from the file system.");
			
			return new FileInputStream(resource);
		} 
		catch (IOException e) {
			// not in the file system. try the class path. NOTE: Will fail if FS path!
			if ( !searchClassPath ) {
				log.warn("FindStream: Load Failed for " + resource + " from FS:" + e.toString() + ". Class path search disabled. Give up."); 
				
				throw new IOException(resource + ": Load failed. File system and Class path search are disabled.");
			}
			// here must use path sep / instead of \
			String cpResource 	=  resourceName != null ? resourcePath + "/" + resourceName : resourcePath;

			log.warn("FindStream: FILESYSTEM Load Failed for " + resource + " (" + e.getMessage() + "). Trying class path: " + cpResource);
			
			// try the classpath
			InputStream is		=  IOTools.class.getResourceAsStream(cpResource); // resource);
			
			if ( is == null ) {
				if ( resourceName == null) {
					throw new IOException("Failed to load " + resourcePath + " from class path.");
				}
				else {
					log.warn("FindStream: CLASSPATH Load Failed for " + cpResource /*resource*/ + " (Not found). Trying /configuration/" + resourceName);

					// try the class path /configuration/resourceName
					resource 	= BASE_CLASSPATH_CONFIG + "/" + resourceName;
					is 			= IOTools.class.getResourceAsStream(resource);
					
					if ( is == null) {
						throw new IOException("Unable to load " + resource + " fromm class path or file system @ " + resourcePath + ".");
						/*
						log.warn("FindStream: CLASSPATH missing " + resource + " in class path. Trying top CLASSPATH (" + resourceName + ")");
						
						// try top CP resourceName
						is = IOTools.class.getResourceAsStream("/" + resourceName);
						
						if ( is == null) {
							throw new IOException("No " + resourceName + " in class path.");
						} */
					}
					log.warn("FindStream: CLASSPATH Loaded class-path resource " + resource);
				}
			}
			else {
				log.debug("FindStream: CLASSPATH - Loaded from class-path " + cpResource);
			}
			return is;
		}
	}

	/**
	 * Pipe an Input stream to a output stream.
	 * @param in Source stream
	 * @param out receiver stream
	 * @throws IOException
	 */
	public static void pipeStream(InputStream in, OutputStream out) throws IOException {
	    BufferedInputStream bin  	= new BufferedInputStream(in);   
	    BufferedOutputStream bout 	= new BufferedOutputStream(out);
    	
	    int c;
	    while ((c = bin.read()) != -1) {
	    	bout.write(c);
	    } 
	    bout.close();
	}

	/**
	 * This is used to ignore (excessive logging) @ some threshold percentage/minute.
	 * Used to handle excessive logging.
	 * @param pct Percent threshold [0..1]
	 * @return true if a random number is less than the given threshold
	 */
	public static boolean diceRoll(float threshold) {
		return Math.random() < threshold;
	}

	/**
	 * MKDir FS utility
	 * @param path Path to be created
	 * @return true if the dir location was created.
	 */
	public static boolean mkDir(String path) {
		File f = new File(path);
		if ( f.exists()) return true;
		log.debug("MkDir: Creating " + path);
		return f.mkdirs();
	}
	
	public static short randomShort() {
		// FindBugs com.cloud.core.io.IOTools.randomShort() uses the nextDouble method of Random to generate a random integer; using nextInt is more efficient [Of Concern(18), Normal confidence]
		//return (short)(Short.MAX_VALUE * Math.random());
		return (short)CoreTypes.RANDOM.nextInt(Short.MAX_VALUE);
	}

	/**
	 * Date formats must follow RFC339 - http://www.ietf.org/rfc/rfc3339.txt
	 * @param d {@link Date} to format.
	 * @return RFC339 formatted date.
	 */
	public static String formatDateRFC339(Date d) {
		// FIXME: I don't know if this is correct but seems to get accepted by the LP server
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(d);
	}
	
	public static String createTimeStamp() {
		// Create a time stamp for application log window messages
		return new SimpleDateFormat("yyyy-dd-MM HH:mm:ss:SSS").format(new GregorianCalendar().getTime());
	}

	/**
	 * Derive an abstract ID by concatenating an array of objects with an '@'
	 * @param args
	 * @return
	 */
	public static String newForeignKey(Object ... args) {
		StringBuffer id = new StringBuffer(args[0].toString());
		
		for (int i = 1; i < args.length; i++) {
			id.append("@" + args[i]);
		}
		return id.toString();
	}

	public static String[] splitForeignKey(String foreignKey) {
		return foreignKey.split("@");
	}
	
	/**
	 * Copy an asset from the class path to the file system.
	 * @param asset Class path asset.
	 * @param dir Destination directory.
	 * @param force If true overwrite destination.
	 * @throws IOException If an I/O error occurs.
	 * @throws FileNotFoundException If an error occurs.
	 * @throws Exception If an error occurs.
	 */
	public static void installAssetFromClassPath (String asset, File dir, boolean force) throws FileNotFoundException, IOException {
		String name = new File(asset).getName();
		File dest 	= new File(dir + File.separator + name);
		
		if ( !dir.exists()) {
			if ( !dir.mkdirs()) {
				log.error("Failed to create dir " + dir);
			}
		}
		if ( dest.exists() && !force) {
			log.warn("InstallAsset: " + dest + " already exists!" );
			return;
		}
		log.debug("InstallAsset: " + asset + " to " + dest);
		InputStream is 	= null;
		OutputStream os = null;
		try {
			is = IOTools.class.getResourceAsStream(asset);
			os = new FileOutputStream(dest);
			pipeStream(is, os );
		}
		finally  {
			closeStream(is);
			closeStream(os);
		}
	}
	
	/**
	 * Helper sub to Close an Input/Output stream.
	 * @param stream {@link InputStream} or {@link OutputStream}.
	 */
	public static void closeStream (Closeable stream) {
		if ( stream == null ) return;
		try {
			stream.close();
		} catch (Exception e) {
		}
	}
	
	/**
	 * Install single asset into the file system ignoring any errors. DO NOT overwrite if the asset exists.
	 * 
	 * @param assetName  Asset name from class path. For example: /configuration/asset.txt
	 * @param destPath Base path of the global configuration ${home}/.cloud/CloudAdapter
	 */
	public static void installSingleAsset(String assetName, String destPath) {
		try {
			IOTools.installAssetFromClassPath (assetName
					, new File(destPath)
					, false);
		} catch (Exception e) {
			log.error("Asset " + assetName + " Install Error: " + e.toString());
		}
	}

	/**
	 * Evaluate an expression containing Java system properties of the form ${SYSPROP_NAME}.
	 * For example: <pre>
	 * evalSystemProperty("${user.home}/configuration"); 
	 * Evaluates to:
	 * 	c:\\users\\USER\\configuration (Windows) 
	 * 	/home/users/USER/configuration (linux)</pre>
	 * @param value Encoded string such as ${VAR_NAME}/something
	 * @return Evaluated string string ${user.home}/configuration => c:\\users\\USER\\configuration
	 */
	public static String evalSystemProperties(String src) {
		String regex	= "\\$\\{(.*?)\\}";
		String dst 		= src;
		
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(src);
		
		while (matcher.find()) {
			String tmp 	= matcher.group();
			String name	= tmp.replaceAll("[\\$\\{\\}]", "");
			String val	= System.getProperty(name);
			
			if ( val != null) {
				dst = dst.replace(tmp, val);
			}
		}
		return dst;
	}

	/**
	 * Evaluate an expression containing a variable of the form ${SYSPROP_NAME}.
	 * For example: <pre>
	 * evalSystemProperty("http://${host.ip}:8080/configuration","host.ip", getHostName()); 
	 * Evaluates to: http://1.2.3.4:8080/configuration

	 * @param value Encoded string such as ${VAR_NAME}/something
	 * 
	 * @return Evaluated string string ${user.home}/configuration => c:\\users\\USER\\configuration
	 */
	public static String evalSystemVariable(String src, String name, String value) {
		String regex	= "\\$\\{(.*?)\\}";
		String dst 		= src;
		
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(src);
		
		while (matcher.find()) {
			String tmp 	= matcher.group();
			dst = dst.replace(tmp, value);
		}
		return dst;
	}

	/**
	 * Evaluate common system variables:
	 * <li>host.ip Evaluates to the host IP address.
	 * <li>host.name Evaluates to the host name.
	 * @param src
	 * @return
	 */
	public static String evalCommonSystemVars(String src) {
		String dest = src;

		if ( src.contains("host.ip"))
			dest = evalSystemVariable(src, "host.ip", getHostIp() );
		
		if ( src.contains("host.name"))
			dest = evalSystemVariable(src, "host.name", getHostname());
		
		return dest;
	}


	/**
	 * Get the directories for a given {@link File} object.
	 * @param f {@link File} object representing a folder/directory.
	 * @return array of {@link File} directories.
	 */
	public static File[] getDirectories(File f) {
		return f.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
	}


	/**
	 * Convert a {@link List} od string to a String[]
	 * @param list {@link List} of string.
	 * @return String[].
	 */
	public static String[] convertList(List<String> list)  {
		String[] array = new String[list.size()];
		return list.toArray(array);
	}

	/**
	 * Encode a URL string using UTF-8.
	 * @param s
	 * @return
	 */
	static public String URLEncode(String s) {
		try {
			return URLEncoder.encode(s, DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	/**
	 * Decode a URL string using UTF-8.
	 * @param s
	 * @return
	 */
	static public String URLDecode(String s) {
		try {
			return URLDecoder.decode(s, DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}
	
	/**
	 * Simple function to read a small file from the file system using the default encoding (UTF-8).
	 * @param filePath File path. Absolute or relative to the current working directory.
	 * @return File contents
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	static public String readFileFromFileSystem(String filePath) throws FileNotFoundException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			IOTools.pipeStream(new FileInputStream(filePath), out);
		}
		finally { 
			closeStream(out);
		}
		return out.toString(DEFAULT_ENCODING);
	}
	
	/**
	 * Simple character data file save helper.
	 * @param fullPath Full path to the file.
	 * @param data Character data to save.
	 * @throws IOException If there are any file or IO errors.
	 * @since 1.0.1
	 */
	public static void saveText(final String fullPath , final String data) throws IOException {
		OutputStream os 			= null;
		ByteArrayInputStream is 	= null;
		try {
			// save it
			os = new FileOutputStream(fullPath);
			is = new ByteArrayInputStream(data.getBytes(CoreTypes.CHARSET_UTF8));
			IOTools.pipeStream(is, os);
		} 
		finally {
			IOTools.closeStream(is);
			IOTools.closeStream(os);
		}
	}

	/**
	 * Save {@link InputStream} data into a file.
	 * @param fullPath Full path to the file.
	 * @param is Data input stream.
	 * @throws IOException If there are any file or IO errors.
	 * @since 1.0.1
	 */
	public static void saveStream(final String fullPath , final InputStream is) throws IOException {
		OutputStream os 			= null;
		try {
			// save it
			os = new FileOutputStream(fullPath);
			IOTools.pipeStream(is, os);
		} 
		finally {
			IOTools.closeStream(is);
			IOTools.closeStream(os);
		}
	}
	
	/**
	 * Read from an input stream into a character data in UTF-8.
	 * @param is The input stream.
	 * @return Character data encoded as UTF-8.
	 * @throws IOException on I/O errors.
	 */
	public static String readFromStream (InputStream is) throws IOException {
		ByteArrayOutputStream out 	= new ByteArrayOutputStream();
		try {
			IOTools.pipeStream(is, out);
			out.close();
		}
		finally {
			closeStream(out);
		}
		return out.toString(CoreTypes.ENCODING_UTF8);
	}

	/**
	 * Read from an input stream into a character data in UTF-8.
	 * @param is The input stream.
	 * @param encoding Character encoding.
	 * @return Character data encoded as UTF-8.
	 * @throws IOException on I/O errors.
	 */
	public static String readFromStream (InputStream is, String encoding) throws IOException {
		ByteArrayOutputStream out 	= new ByteArrayOutputStream();
		try {
			IOTools.pipeStream(is, out);
			out.close();
		}
		finally {
			closeStream(out);
		}
		return out.toString(encoding);
	}

	/**
	 * Get the user's home.
	 * Window: If logged as administrator user.home returns c:\
	 * so use System.getenv(USERPROFILE) instead.
	 * @deprecated If installinga s administrator retunrs c:\Windows\system32....!
	 * @return User's home
	 */
	/*
	public static String getUserHome() {
		if ( CoreTools.OS_IS_WINDOWS) {
			String jhome = USER_HOME;
			// WARNING: may be c:\Windows\system32\....!! (Can'u use that!)
			String whome = System.getenv("USERPROFILE");
			
			// THis also returns c:\Windows\system32\ when installing as administrator!
			String local = System.getenv("LOCALAPPDATA");
			
			if ( !jhome.equalsIgnoreCase(whome)) {
				// better use %LOCALAPPDATA%
				return local;
			}
			else {
				return jhome;
			}
		}
		else {
			return USER_HOME;
		}
	} */
	
}
