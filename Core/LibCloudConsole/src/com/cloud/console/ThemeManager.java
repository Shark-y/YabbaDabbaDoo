package com.cloud.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceDescriptor.ServiceType;

/**
 * Dynamic menu loader for the cloud console.
 * <p>Menus are loaded from a theme descriptor in the service descriptor from the class path. For example:</p>
 * <pre>
 * # Sample Contact Center - classpath: /configuration/call_center.ini
 * service_class = com.simplicti.jtapi.general.JTAPIBackend
 * service_config = avaya.ini
 * service_vendor = Avaya AES
 * service_vendorId = AVAYA
 * # Console Dynamic Menu descriptor
 * service_theme = aes_theme.ini
 * </pre>
 * Sample load
 * <pre>
 * NodeConfiguration sc = new NodeConfiguration();
 * ThemeManager tm = ThemeManager.getInstance();
 * tm.load(sc.getServiceDescriptors());
 * tm.load(sc.getServiceDescriptors());
 * tm.dumpMenus("TEST");
 * </pre>
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation
 * @version 1.0.1 - 6/6/2017 Added ability to hide static menus.
 *
 */
public class ThemeManager {

	/** Default console theme */
	public static final String DEFAULT_THEME = "bootstrap-blue";
	
	/**
	 * Dynamic Menu descriptor. Loaded from the service descriptor @ /configuration/{@link ServiceType}.ini
	 * <h2>The Format is: </h2>
	 * <pre>
	 * # Samples: Format: menuid = Label, URL, Parent, CSS class, URL Target
	 * menu_AES2 = Connection Profiles,perf/profiles.jsp,/,fa fa-fw fa-arrows-v
	 * menu_AES3 = Test,test/test.jsp,/,fa fa-fw fa-table
	 * </pre>
	 * @author VSilva
	 *
	 */
	public static class MenuDescriptor {
		public String menuId;		// menu id
		public String label;		// menu label
		public String url;			// url 
		public String parent;		// parent menu name
		public String cssClass;		// optional: CSS class
		public String urlTarget;	// optional: target within <a href=... target=XYZ>...
		
		@Override
		public String toString() {
			// FindBugs 11/29/16 Unread public/protected field: com.cloud.console.ThemeManager$MenuDescriptor.cssClass
			return "Menu: " + menuId + "," + label + "," + url + "," + parent 
					+ (cssClass != null ? "," + cssClass : "")
					+ (urlTarget != null ? "," + urlTarget : "");
		}
		
		public static Comparator<MenuDescriptor> IdComparator = new Comparator<MenuDescriptor>() {
			public int compare(MenuDescriptor m1, MenuDescriptor m2) {
				final String id1 = m1.menuId;
				final String id2 = m2.menuId;
				
				//ascending order
				return id1.compareTo(id2);
			}
		};
	}
	
	// singleton
	private static final ThemeManager instance = new ThemeManager();
	
	// menu list
	private List<MenuDescriptor> menus;
	
	/** Available themes: bootstrap-blue, bootstrap-dark, bootstrap-light or bootstrap-green */ 
	private String themeName;
	
	/** Title displayed in the top left corner of the console */
	private String themeTitle;
	
	/** comma separated list of static menus marked as hidden */
	private String hiddenMenus;
	
	/**
	 * Constructor.
	 */
	private ThemeManager() {
		menus = new ArrayList<ThemeManager.MenuDescriptor>();
		
		// load the main theme: /configuration/main_theme.ini
		try {
			loadMenus("main_theme.ini");
		} catch (IOException e) {
			System.err.println("[THEME] Failed to load main theme (/configuration/main_theme.ini) from CP: " + e.toString());
		}
	}
	
	public static ThemeManager getInstance() {
		return instance;
	}
	
	private void loadMenus(String file) throws IOException {
		InputStream is 		= null;
		Properties props 	= new Properties();
		// FindBugs 11/29/16 com.cloud.console.ThemeManager.loadMenus(String) may fail to clean up java.io.InputStream on checked exception
		try {
			is = ThemeManager.class.getResourceAsStream("/configuration/" + file);
			if ( is == null) {
				return;
			}
			props.load(is);
		}
		finally {
			if ( is != null) {
				is.close();
			}
		}
		Set<Object> keys = props.keySet();
		
		// get menus. The key must start w/ menu
		for (Object key : keys) {
			// reject non menus
			if ( !key.toString().startsWith("menu")) {
				continue;
			}
			
			// format: menuid = Label, resource, Parent, CSS class
			String value 	= props.getProperty(key.toString());
			String[] fields = value.split(",");
			
			if ( fields.length < 3) {
				System.out.println(file +  " Invalid menu " + key + " = " + value);
				continue;
			}
			MenuDescriptor md = new MenuDescriptor();
			
			md.menuId 	= key.toString();
			md.label 	= fields[0];
			md.url 		= fields[1];
			md.parent	= fields[2];
			
			// optional
			md.cssClass		= fields.length >= 4 ? fields[3] : null;
			md.urlTarget 	= fields.length >= 5 ? fields[4] : null;
			
			if ( findMenu(md.menuId) == null)
				menus.add(md);
		}
		
		// get theme stuff (optional)
		if (props.containsKey("theme_name")) 	themeName 	= props.getProperty("theme_name");
		if (props.containsKey("theme_title")) 	themeTitle 	= props.getProperty("theme_title");
		
		// 6/6/2017 - hidden menus
		if (props.containsKey("hide_menus")) 	hiddenMenus = props.getProperty("hide_menus");
	}

	private void addMenu (String id, String label, String url, String parent, String cssClass, String urlTarget) {
		MenuDescriptor md = new MenuDescriptor();
		
		md.menuId 	= id;
		md.label 	= label;
		md.url 		= url;
		md.parent	= parent;
		
		// optional
		md.cssClass		= cssClass ; 
		md.urlTarget 	= urlTarget; 
		
		if ( findMenu(md.menuId) == null){
			menus.add(md);
		}
	}
	
	public void load (JSONArray array) {
		for (int i = 0; i < array.length(); i++) {
			try {
				JSONObject menu = array.getJSONObject(i);
				
				addMenu(menu.getString("id")
						, menu.getString("label")
						, menu.getString("url")
						, menu.getString("parent")
						, menu.optString("iconCss")
						, menu.optString("urlTarget"));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Is a menu marked as hidden?
	 * @param mnuName Mane label (name).
	 * @return true if it should be hidden.
	 * @since 1.0.1
	 */
	public boolean isMarkedAsHidden(String mnuName) {
		return hiddenMenus != null && hiddenMenus.contains(mnuName) ? true : false;
	}
	
	/**
	 * Find any menu by id.
	 * @param id Menu id from the theme descriptor.
	 * @return a {@link MenuDescriptor}.
	 */
	public MenuDescriptor findMenu(String id) {
		for (MenuDescriptor md : menus) {
			if ( md.menuId.equals(id))
				return md;
		}
		return null;
	}

	/**
	 * Find a {@link MenuDescriptor} list by parent name.
	 * @param parentName Any parent name. <b>The ROOT menu is called /</b>
	 * @return {@link MenuDescriptor} {@link List}. If none available the list size will be zero.
	 */
	public List<MenuDescriptor> findMenusByParent(String parentName) {
		List<MenuDescriptor> list = new ArrayList<ThemeManager.MenuDescriptor>();
		for (MenuDescriptor md : menus) {
			if ( md.parent != null && md.parent.equals(parentName))
				list.add(md);
		}
		
		// sort by id
		Collections.sort(list, MenuDescriptor.IdComparator);
		return Collections.unmodifiableList(list);
	}

	/**
	 * Load menus from a list of {@link ServiceDescriptor}.
	 * @param list List of {@link ServiceDescriptor}.
	 * @throws IOException
	 */
	public void load(List<ServiceDescriptor> list) throws IOException {
		for (ServiceDescriptor sd : list) {
			loadMenus(sd.getMenuDescriptor());
		}
	}
	
	public void dumpMenus(String label) {
		System.out.println(">> MENUS " + label + " = " + menus.size());
		for (MenuDescriptor md : menus) {
			System.out.println(md);
		}
	}
	
	/**
	 * @return Available themes: bootstrap-blue, bootstrap-dark, bootstrap-light or bootstrap-green 
	 */
	public String getThemeName() {
		return themeName;
	}
	
	/**
	 * @return Title displayed in the top left corner of the console or NULL if missing.
	 */
	public String getThemeTitle() {
		return themeTitle;
	}

}
