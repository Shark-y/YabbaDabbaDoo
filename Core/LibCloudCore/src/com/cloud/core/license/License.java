package com.cloud.core.license;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Key;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.license.LicenseRuntime;
import com.cloud.core.security.KeyTool;
import com.cloud.core.security.RSATool;
import com.cloud.core.security.KeyTool.KeyType;
import com.cloud.core.types.CoreTypes;

/**
 * A very simple license client. It has code to decrypt the existing license format:
 * <p>The format is:
 * 
 * clientId^productId^Number-of-seats^expiration-date^privileges^system-mac
 * 
 * @author VSilva
 * @version 1.1.1 - 2/2/2019 Moved the INSTANCE_ID logic to CoreTypes.
 *
 */
public class License 
{
	private static final Logger log = Logger.getLogger(License.class);
		
	private static String lastError;
	
	private static final String FORMAT 					= "<cid>^<pid>^<seats>^<exp-date>^<privs>^<mac>";
	
	// used this to cache the descriptor in session
	public static final String SESSION_LIC_DESCRIPTOR 	= "SESSION_LIC_DESCRIPTOR";
	
	// Unique MD5 of the classpath root "/" 
	// @deprecated 2/2/2019 This code is kept so old deployments won't complain with 'License Error: Invalid instance id.' New Builds: User CoreTypes.INSTANCE_ID instead.
	private static final String INSTANCE_ID 			= CoreTypes.INSTANCE_ID; // initRootClassPathMD5(); 
	
	// system MAC (Hardware address). FIXME: 2/5/2019 This takes too long: Almost 6secs!
	private static final String SYSTEM_MAC				= getFirstHardwareAddress();
	
	/**
	 * License description information class.
	 * @author sharky
	 *
	 */
	public static class LicenseDescriptor {
		public String companyId;		// Company id
		public String productId;		// product
		public int 	  numOfSeats;		// Number of seats
		public String expirationDate;	// mm/dd/yyyy
		public String privileges;		// ?
		public String MACAddress;		// System Mac (hardware address)
		public String instanceId;		// server instance
		
		public JSONObject toJSON() throws JSONException {
			JSONObject root = new JSONObject();
			
			if ( companyId != null ) 	root.put("companyId", companyId);
			if ( productId != null)		root.put("productId", productId);

			root.put("numOfSeats", numOfSeats);
			
			if ( privileges != null ) 		root.put("privileges", privileges);
			if ( expirationDate != null ) 	root.put("expirationDate", expirationDate);
			if ( MACAddress!= null )		root.put("MACAddress", MACAddress);
			if ( instanceId != null ) 		root.put("instanceId", instanceId);
			return root;
		}
	}
	
    /**
     * Get the System MAC/IP address
     * @return System MAC/IP address
     */
    public static String getSystemMACAddress() {
    	return SYSTEM_MAC;
    }
    
    /**
     * 
     * @return The 1st MAC/HW address.
     */
    public static String getFirstHardwareAddress() {
    	List<String> addrs = null;
    	
		try {
			addrs = getHardwareAddresses();
		} 
		catch (SocketException e) {
			lastError = "GetSystemMAC: " + e.getMessage();
		}
    	return addrs != null ? addrs.get(0) : null; 
    }

    /**
     * Get all Hardware (MAC) addresses for the host. This should work in both Windows & Linux.
     * @return {@link ArrayList} of MAC addresses of the form: E0-DB-55-E9-E2-AC
     * @throws SocketException
     */
	public static List<String> getHardwareAddresses() throws SocketException {
		List<String> list = new ArrayList<String>();
		
		Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
		
		if ( nis == null) {
			return null;
		}
		while( nis.hasMoreElements()) {
			NetworkInterface network = nis.nextElement();
  
			byte[] mac = network.getHardwareAddress();
  
			// mac len may be zero!
			if ( mac != null && mac.length > 0) {
				StringBuilder sb = new StringBuilder();

				// enoce as: E0-DB-55-E9-E2-AC
				for (int i = 0; i < mac.length; i++) {
					sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
				}
				list.add(sb.toString());
			}
		}
		return list;
	}

    /**
     * By get hardware MAC by hostname
     * @param hostName if null gets the local system's address: InetAddress.getLocalHost()
     * @deprecated THIS WILL NOT WORK IN LINUX!
     * @return HW Address bytes.
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static byte[] getHardwareAddress(String hostName) throws SocketException, UnknownHostException
    {
    	if ( hostName == null || hostName.equalsIgnoreCase("localhost")) {
    		// This InetAddress.getLocalHost() won't work in linux
    		return NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
    	}
    	else {
    		NetworkInterface net = NetworkInterface.getByInetAddress(InetAddress.getByName(hostName));
    		
    		if ( net == null) {
    			System.err.println("Unable to get NetworkInterface for host:" + InetAddress.getByName(hostName));
    			return null;
    		}
    		
    		return net.getHardwareAddress();
    	}
    } 
    
    /**
     * Get the hostname for a given byte[] address
     * @param address
     * @return
     * @throws UnknownHostException
     */
    public static String getHostName(byte[] address) throws UnknownHostException {
    	return InetAddress.getByAddress(address).getHostName();
    }
    
    /**
     * Get Hardware (MAC) address for ha host.
     * @param hostName
     * @deprecated This will NOT work in Linux!
     * @return String encoded address of the form: E0-DB-55-E9-E2-AC
     */
    public static String getMACAddress(String hostName)
    {
        try {
    		
    		byte[] mac = getHardwareAddress(hostName);
    		
    		if ( mac == null ) {
    			lastError = "Unable to find hardware address for host:" + hostName;
    			return null;
    		}
    		StringBuilder sb = new StringBuilder();
    		
    		for (int i = 0; i < mac.length; i++) {
    			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
    		}
    		return sb.toString();
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
        	System.err.println(ex.toString());
        	return null;
        }
    }
    
    /**
     * Decrypt the license secret
     * @param secret
     * @return
     * @throws Exception 
     */
    static private String decryptSecret(Key key, String secret) throws Exception {
    	//return RSADecrypt.decrypt(key, secret);
    	return RSATool.decrypt(key, secret);
    }
    
    /**
     * Get license information.
     * @param pubKey {@link Key} public (decryption) key.
     * @param secret The encoded license string.
     * @return {@link LicenseDescriptor} information class
     */
	public static LicenseDescriptor describe(Key key, String secret) {
		try {
			//lastError 		= null;
			
			if ( secret == null || secret.isEmpty()) {
				throw new /*IllegalArgument*/Exception("Missing license.");
			}
			
			String dec 		= decryptSecret(key, secret);
			String[] fields = dec.split("\\^");
			
			LicenseDescriptor descriptor = new LicenseDescriptor();
			
			if ( fields.length < 6) {
				throw new IllegalArgumentException("Invalid license format for " + dec + " Format:" + FORMAT);
			}

			descriptor.companyId 		= fields[0];
			descriptor.productId		= fields[1];
			
			try {
				descriptor.numOfSeats	= Integer.parseInt(fields[2]);
			} catch (Exception e) {
				log.error("License: Invalid # of seats:" + e.toString());
			}
			
			descriptor.expirationDate	= fields[3];	// mm/dd/yyyy
			descriptor.privileges		= fields[4];
			descriptor.MACAddress		= fields[5];	// hardware address (MAC)
			descriptor.instanceId		= fields.length > 6 ? fields[6] : null; // optional (server instance id)

			return descriptor;
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println("License::describe Error:" + e.toString());
			lastError = /*"License Error: " +*/ e.getMessage();
			return null;
		}
	}
    
	/**
	 * Verify.
	 * @param key Decryption key.
	 * @param secret license string.
	 * @param productID
	 * @return
	 */
	static public boolean verify(Key key, String secret, String productID ) {
		LicenseDescriptor descriptor = describe(key, secret);
		return verify(/*key,*/ descriptor, productID);
	}
	
    /**
     * Verify a license string. This sub should be called only once. 
     * @param descriptor {@link LicenseDescriptor}.
     * @param productID Product id string.
     * @return true if valid else call getLastError.
     */
	static public boolean verify(/*Key key,*/ LicenseDescriptor descriptor, String productID ) { //String secret, String productID ) {
		//LicenseDescriptor descriptor = describe(key, secret);
		
		// lastError will be set by describe(...)
		if ( descriptor == null) {
			return false;
		}
		
		//String companyID 		= descriptor.companyId;
		String productId		= descriptor.productId;
		int numOfSeats			= descriptor.numOfSeats;
		String expirationDate	= descriptor.expirationDate;	// mm/dd/yyyy
		//String privileges		= fields[4];
		String MACAddress		= descriptor.MACAddress;
		String instanceId		= descriptor.instanceId; 		// optional
		
		if ( !productID.equals(productId)) {
			lastError = "License failed: Invalid product id.";
			return false;
		}
		
		if (!MACAddress.equals(getSystemMACAddress())) {
			lastError = "License failed: Invalid MAC.";
			return false;
		}
		
		try {
			// check the expiration date.
			String expDate 			= expirationDate.replaceAll("/", "");
			DateFormat dateFormat	= new SimpleDateFormat("MMddyyyy");
			Date today 				= new Date();
			Date expiryDate 		= dateFormat.parse(expDate);
			
			if ( !expiryDate.after(today)) {
				lastError = "License expired.";
				return false;
			}
		} catch (Exception e) {
			lastError = "License: Date parse error:" + e.getMessage();
			return false;
		}
		
		// check the server instance id if available.
		if ( instanceId != null ) {
			String serverInstanceId = getInstanceId();
			
			if ( serverInstanceId == null) {
				lastError = "Unable to get instance id from server.";
				return false;
			}
			if ( !instanceId.equalsIgnoreCase(serverInstanceId)) {
				lastError = "Invalid instance id.";
				return false;
			}
		}
		log.debug("License verified ProdId:" + productID + " # Seats:" + numOfSeats + " Expiration:" + expirationDate + " MAC:" + MACAddress);
		return true;
	}
	
	/**
	 * Dump {@link LicenseDescriptor} information.
	 * @param key Public (decryption) key.
	 * @param license See {@link LicenseDescriptor}.
	 */
	public static void dump(Key key, LicenseDescriptor license) {
		StringBuffer buf = new StringBuffer("<license>");
		buf.append("\nLicence: CompanyID: " +  license.companyId);
		buf.append("\nLicence: ProductID: " +  license.productId);
		buf.append("\nLicence: Num Seats: " +  license.numOfSeats);
		buf.append("\nLicence: Exp Date : " +  license.expirationDate);
		buf.append("\nLicence: MAC      : " +  license.MACAddress);
		
		if ( license.instanceId != null) {
			buf.append("\nLicence: Instance Id: " 	+  license.instanceId);
		}
		buf.append("\n</license>");
		log.debug(buf.toString());
	}
	
	/**
	 * Dump some values for informational purposes
	 * @param secret
	 */
	public static void dump(Key key, String secret) {
		try {
			System.out.println("--------- Start License/Key Dump ---------");
			System.out.println("Key:" + key);
			System.out.println("Secret:[" + secret + "]");
			
			String dec = decryptSecret(key, secret);
			
			System.out.println("Decrypted:[" + dec + "]");
			
			String[] fields = dec.split("\\^");
			
			if ( fields.length < 6) {
				throw new IllegalArgumentException("Invalid license format for " + dec + " Format:" + FORMAT);
			}
			dump(key, describe(key, secret));
		} 
		catch (Exception e) {
			log.error("License Dump Error:" + e.toString());
			//e.printStackTrace();
		}
		finally {
			System.out.println("-----------------------------------");
		}
	}
	
	/**
	 * Get the last error declared by he verify method.
	 * @return
	 */
	public static String getLastError() {
		return lastError;
	}

	public static void clearLastError() {
		lastError = null;
	}

	/**
	 * Check if the # of seats has been exceeded.
	 * @param lr {@link LicenseRuntime}
	 * @param deckeyResourcePath Path to the decryption key resource (from the class path)
	 * @param keyString License string
	 * @return true if the number of seats in the license has been exceeded
	 * @throws IOException
	 */
	public static boolean hasExceededLicenseSeats(LicenseRuntime lr, String deckeyResourcePath, KeyType keyType, String keyString) 
			throws IOException 
	{
		LicenseDescriptor license	= License.describe(KeyTool.readKeyFromFile(deckeyResourcePath, keyType), keyString);
		int seats 					= license.numOfSeats;
		
		// NOTE: lr.getItems().size() will NOT include the last logged in agent (thus +1);
		int agents	= lr.getItems().size() + 1;

		return agents > seats;
	}

	/**
	 * Get the absolute path of a resource (folder) in the class path.
	 * @param clazz Java Class used to load the resource.
	 * @param resourceName Resource (folder) name in the class path.
	 * @return The absolute path of the class path resource. Example: WIN32 - C:/CloudServices/Cloud-UnifiedContactCenter
	 * @throws IOException
	 */
	/* 2/2/2019 Moved to CoreTypes
	public static String getClassPathResourceAbsolutePath(Class clazz, String resourceName) throws IOException {
    	URL url 	= clazz.getResource(resourceName);
    	
    	if ( url == null )
    		throw new IOException("Invalid resource " + resourceName);
    	
    	String path = URLDecoder.decode(url.getFile(), "UTF-8");
    	
    	// Chop the / from a windows path: /C:/Temp/Workspaces/ => C:/Temp/Workspaces/
    	if ( path.startsWith("/") && OS_IS_WINDOWS) {
    		path = path.replaceFirst("/", "");
    	}
    	return path;
	} */
	
	/**
	 * MD5 digest tool.
	 * @param string String to digest.
	 * @return Hex encoded MD5.
	 */
	public static String MD5(final String string) {
		/*
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(string.getBytes(CoreTypes.CHARSET_UTF8));
			
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
			}
			return sb.toString().toUpperCase();
		} 
		catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
		*/
		return CoreTypes.MD5(string);
	}
	
	/**
	 * Generate a unique instance id for this library. By creating an MD5 of the / folder in the class path.
	 * @return 32 character Hex encoded instance id (6c9f9015cad82ecfb00192031121528d).
	 * @throws IOException
	 */
	public static String getInstanceId() {
		return INSTANCE_ID;
	}

	/**
	 * Create an MD5 of the absolute class path of resource '/'. This is used to derive the instance ID of a web node.
	 * @return An MD% of class path resource '/' (14E3F2B9D9FF6B63984701DA12A34C9D)
	 * @deprecated 2/2/2019 This code is kept so old deployments won't complain with 'License Error: Invalid instance id.' User CoreTypes.INSTANCE_ID instead.
	 */
	/*
	private static String initRootClassPathMD5() {
		try {
			// 10/25/2016 Tomcat parallel deployment: chop the version from the FS path
			// PATH C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01##release-1.1-20161024/WEB-INF/classes
			// BECOMES C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/CloudConnectorAES01/WEB-INF/classes
			String path 	= CoreTypes.getClassPathResourceAbsolutePath(License.class, "/");
			String chopped	= path.replaceAll("##.*?/", "/");
			
			log.debug("GetInstanceId: Using path " + chopped + " to derive instance id.");
			return MD5(chopped);
		} catch (Exception e) {
			log.error("Unable to get instance id from server.", e);
			throw new RuntimeException("Unable to get instance id from server." + e);
		}
	}*/
	
}
