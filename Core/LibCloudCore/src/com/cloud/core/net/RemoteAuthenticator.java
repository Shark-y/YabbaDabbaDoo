package com.cloud.core.net;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.core.net.Win32ActiveDirectory;
import com.cloud.core.io.Base64;
import com.cloud.core.net.Win32ActiveDirectory.User;
import com.cloud.core.types.CoreTypes;

/**
 * Simple authenticator for Windows AD. Only Windows is supported now but other OSes can be implemented in the future.
 * <strong>This class can only authenticate against WinAD only.</strong>
 * @author VSilva
 * 
 * @version 1.0.0 8/16/2019 Only windows supported now.
 * @version 1.0.1 9/01/2020 REST Basic authentication (see https://en.wikipedia.org/wiki/Basic_access_authentication)
 */
public class RemoteAuthenticator {

	public enum AuthType { BASIC};
	
	public static class Principal {
		String user;
		String passwd;
		public Principal(String user, String passwd) {
			super();
			this.user = user;
			this.passwd = passwd;
		}
		public String getUser() {
			return user;
		}
		public String getPasswd() {
			return passwd;
		}
	}
	
	private RemoteAuthenticator() {
	}
	
	/**
	 * Authenticate a user.
	 * @param user User name.
	 * @param pwd Password.
	 * @param iamIdentity Optional (IAM) identity, may be null;
	 * @throws IOException If the authentication fails.
	 */
	public static void authenticate (final String user, final String pwd) throws IOException  {
		try {
			Win32ActiveDirectory.login(user, pwd);
		} catch (Exception e) {
			throw new IOException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
		}
	}
	
	/**
	 * @return True if remote authentication is enabled.
	 */
	public static boolean enabled () {
		// for now, windows only
		return true; //CoreTypes.OS_IS_WINDOWS;
	}
	
	/**
	 * Get a list of users as a {@link JSONArray}. 8/17/2019 don't use, Takes too long :(
	 * @param user user name.
	 * @param pwd password.
	 * @return JSON [{USER1}, ...]
	 * @throws IOException
	 */
	public static JSONArray getUsers (final String user, final String pwd) throws IOException {
		if ( !CoreTypes.OS_IS_WINDOWS ) {
			throw new IOException("Getusers: Unsupported operating system " + CoreTypes.OS_NAME);
		}
		JSONArray jusers = new JSONArray();
		try {
			// takes too long ~ 110s
			//long t0 = System.currentTimeMillis();
			List<User> users = Win32ActiveDirectory.getUsers(Win32ActiveDirectory.getConnection(user, pwd));
			//long t1 = System.currentTimeMillis();;

			// ~1ms
			//t0 = System.currentTimeMillis();
			for (User usr : users) {
				JSONObject juser = new JSONObject();
				juser.put("commonName", usr.getCommonName());
				juser.put("distinguishedName", usr.getDistinguishedName());
				juser.put("principal", usr.getUserPrincipal());
				jusers.put(juser);
			}
			//t1 =System.currentTimeMillis();
		} catch (Exception e) {
			throw new IOException(e.getCause() != null ? e.getCause() : e );
		}
		return jusers;
	}
	
	/**
	 * Authenticate based on auth-type. Use this method to autheticate REST api calls.
	 * @param type One of BASIC (w3), ...
	 * @param authentication For BASIC the authorization header.
	 * @throws IOException IF authentication fails.
	 */
	public static void authenticate (final AuthType type, final String authentication) throws IOException  {
		if ( type == AuthType.BASIC) {
			Principal p = decodeBasic(authentication);
			authenticate(p.getUser(), p.getPasswd());
		}
		else {
			throw new IOException("Unsupported authentication type " + type);
		}
	}
	
	/**
	 * https://en.wikipedia.org/wiki/Basic_access_authentication
	 * @param auth QWxhZGRpbjpPcGVuU2VzYW1l => Aladdin:OpenSesame
	 * @return A {@link Principal}.
	 * @throws IOException on decode errors.
	 */
	public static Principal decodeBasic (String auth) throws IOException {
		// user:pwd
		String principal 	= new String(Base64.decode(auth));
		if (!principal.contains(":")) { 
			throw new IOException("Invalid authorization principal");
		}
		String[] temp 		= principal.split(":");
		return new Principal(temp[0], temp[1]);
	}
	
	/**
	 * basic authentication Encode BASE64(user:password)
	 * @param user User name.
	 * @param pwd Password
	 * @return BASE64(user:password)
	 * @throws IOException
	 */
	public static String encodeBasic (final String user, final String pwd) throws IOException {
		// user:pwd
		return Base64.encode((user + ":" + pwd).getBytes());
	}

}
