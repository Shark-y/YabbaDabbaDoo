package com.cloud.console.iam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.JSPLoggerTool;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;

/**
 * Role Based Access Control: Used to grant console access via users/roles.
 * 
 * <h2>Current Logic</h2>
 * <ul>
 * <li> By default only the console admin can start/configure services, all other users can only view</li>
 * </ul>
 * 
 * <h2>TODO List</h2>
 * <ul>
 * <li> Map logged in users to roles.
 * <li> Some other stuff I cannot think of...
 * </ul>
 * 
 * @author VSilva
 * @version 1.0 5/17/2020 - Basic: Only sysadmin can start, configure services default Roles: Administrator, Agent, Supervisor, Guest
 *
 */
public class Rbac {

	/** Session key: Logged in user - set by login.jsp */
	public static final String SKEY_LOGGED_USER 		= "session_logged_user";

	/** Default roles */
	public enum Role { Administrator, Agent, Supervisor, Guest };

	/** Role bindings (ROLE, "user1,user2,...") */
	private static JSONObject roleBindings = new JSONObject();
	
	static {
		try {
			load();
		} catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("RBACK", "Load", e);
		}
	}
	
	/**
	 * @param user Logged in user
	 * @return True if user is the console administrator or MASTER user (user == sysadmin)
	 */
	static public boolean isConsoleAdmin (final String user) {
		if ( user == null) {
			return false;
		}
		NodeConfiguration cfg 	= CloudServices.getNodeConfig(); 
		
		// sysadmin user name (this must not be null)
		final String adminName 	= cfg.getSysAdminUser();
		return adminName.equals(user);
	}

	/**
	 * @param session {@link HttpSession}.
	 * @return True if user is the console administrator or MASTER user (user == sysadmin)
	 */
	static public boolean isConsoleAdmin (HttpSession session) {
		final String user = session.getAttribute(Rbac.SKEY_LOGGED_USER).toString();
		return isConsoleAdmin(user);
	}
	
	/**
	 * Only the built in sysadmin or a user the belongs to the Administrator role can start/stop services.
	 * @param user Logged in user
	 * @return True if user can start services. 
	 */
	static public boolean canStartServices (HttpSession session) {
		final String user = session.getAttribute(Rbac.SKEY_LOGGED_USER).toString();
		return isConsoleAdmin(user) || belongsTo(user, Role.Administrator);
	}

	/**
	 * Only the built in sysadmin or a user the belongs to the Administrator role can configure services.
	 * @param user Logged in user
	 * @return True if user can configure services
	 */
	static public boolean canConfigureServices (HttpSession session) {
		final String user = session.getAttribute(Rbac.SKEY_LOGGED_USER).toString();
		return isConsoleAdmin(user) || belongsTo(user, Role.Administrator);
	}
	
	/**
	 * @param user Logged in user.
	 * @param role Some {@link Role}
	 * @return True if user belongs to role X.
	 */
	static boolean belongsTo (final String user, Role role) {
		final String users = roleBindings.optString(role.name());
		return users != null && !users.isEmpty() ? users.contains(user) : false;
	}
	
	/**
	 * By default, alerts are stored in $home/.cloud/[PRODUCT-NAME]/ospalerts.ini
	 * @return $home/.cloud/[PRODUCT]
	 */
	private static String getBasePath() {
		NodeConfiguration cfg 	= CloudServices.getNodeConfig();
		
		// $home/.cloud/Product
		String basePath 		= cfg.getConfigLocation();
		String filePath			= basePath + File.separator + "rbac.json";
		return filePath;
	}
	
	public static void load () throws FileNotFoundException, JSONException, IOException {
		final String path		= getBasePath(); 
		
		if ( FileTool.fileExists(path)) {
			JSPLoggerTool.JSP_LOGD("RBAC", "Loading role bindings from " + path);
			
			roleBindings = new JSONObject(IOTools.readFileFromFileSystem(path));
			//System.out.println("RBAC Loaded role bindings from " + path);
		}
	}
	
	/**
	 * Save a (user, role) tuple in $HOME/.cloud/[PRODUCT]
	 * @param request HTTP servlet request.
	 * @throws IOException On I/O errors.
	 * @throws JSONException On JSON errors.
	 */
	public static void save(HttpServletRequest request ) throws IOException, JSONException {
		final String role 		= request.getParameter("rbac_role");
		final String users 		= request.getParameter("rbac_users");
		final String path		= getBasePath(); 
		
		JSPLoggerTool.JSP_LOGD("RBAC", "Saving role " + role + " users=" + users + " @ " + path);
		
		roleBindings.put(role, users);
		IOTools.saveText(path, roleBindings.toString(1));
	}
	
	public static String toHtml (final int identFactor) {
		try {
			return roleBindings.toString(identFactor);
		} 
		catch (JSONException e) {
			JSPLoggerTool.JSP_LOGE("RBAC", "RBAC to HTML", e);
			return "";
		}
	}
	
	/**
	 * Get the list of users for a given role.
	 * @param role See {@link Role},
	 * @return user1,usere2,...
	 */
	public static String getRoleBinding (Role role) {
		try {
			return roleBindings.getString(role.name());
		} catch (JSONException e) {
			return "";
		}
	}
}
