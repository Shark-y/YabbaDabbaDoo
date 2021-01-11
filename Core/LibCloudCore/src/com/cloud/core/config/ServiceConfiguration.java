package com.cloud.core.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.config.ConfigGroup;
import com.cloud.core.config.ConfigItem;
import com.cloud.core.config.ConfigRow;
import com.cloud.core.config.FileWidget;
import com.cloud.core.config.IConfiguration;
import com.cloud.core.config.OrderedProperties;
import com.cloud.core.config.XmlConfig;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.EncryptionTool;
import com.cloud.core.services.CloudServices;


/**
 * This class wraps a configuration file of key = value pairs. It allows for METADATA used to describe
 * a dynamic configuration of the form:<pre>
 * KEYNAME = { value}
 * KEYNAME_attribute_label = A Label
 * KEYNAME_attribute_widget = {Widget Type: label, boolean, textbox (default), dualmultiselect}
 * KEYNAME_attribute_group = GROUP NAME
 * KEYNAME_attribute_placeholder = Placeholder Label (valid for INPUT type text only). </pre>
 * Properties can have attributes. Attributes can be:
 * <li> label: Label shown on the left side of the property.
 * <li> widget: The type of HTML widget use to render the property.
 * <li> group: Properties can be grouped using HTML {FIELDSET}
 * <li> placeholder: A label shown as a placeholder or hint for test box widgets only.
 * <li> The widget attribute describes the HTML widget used to render. Widget values are:
 * <ul>
 * <li> label: A plain HTML label {LABEL}
 * <li>	boolean: A true/false combo box  {SELECT}
 * <li>	boolean+hyperlink: A Boolean SELECT with a hyperlink checkbox INPUT type=checkbox
 * <li>	combo: Renders an single selection combo (HTML SELECT).
 * <li>	textbox: Default. An HTML text box: INPUT type=”text”
 * <li>	textarea: Renders an HTML TEXTAREA
 * <li>	dualmultiselect: renders two multiple selection (Available/Selected) HTML SELECT widgets. The format is: dualmultiselect&{LEFTPROPS _RESOURCE} where LEFTPROPS _RESOURCE is the class path to a Properties file that contains the left side available properties. There are two types of dual selects:
 * <ul>
 * <li>Editable: It adds two key/value entry input boxes whose values can be added to the left side SELECT. Its format is: dualmultiselect&val1,val2,..valn
 * <li>	Non-Editable: New values cannot be inserted (no key/value entry boxes). Left side values are loaded from an umutable properties file. The format is: dualmultiselect&{LEFTPROPS _RESOURCE_FILEPATH}
 * </ul>
 * </ul>
 * This class is mostly used by the configuration UI.
 * @author vsilva
 * @version 1.0.1 - 7/11/2017 Added new widget type file upload.
 */
public class ServiceConfiguration { 
    private static final Logger log = LogManager.getLogger(ServiceConfiguration.class);

	// These are used by the config JSP to manage the configs
	public static final String CONFIG_KEY_SERVICE_CHAT 		= "chat_";
	public static final String CONFIG_KEY_SERVICE_OM 		= "om_";

	public static final String WIDGET_SUFFIX = "_attribute_widget";
	
	// HTML INPUT password mask
	public static final String PASSWORD_MASK = "***********************";

	// widget attribute field sep
	private static final String WIDGET_ATTR_FIELD_SEP = "&";
	
	// widget keys specified in the properties file
	public static final String WIDGET_TYPE_KEY_LABEL 		= "label";
	public static final String WIDGET_TYPE_KEY_COMBO 		= "combo";
	public static final String WIDGET_TYPE_KEY_DUALSELECT	= "dualmultiselect";
	public static final String WIDGET_TYPE_KEY_CHECKBOX 	= "checkbox";
	public static final String WIDGET_TYPE_KEY_TEXTAREA 	= "textarea";
	public static final String WIDGET_TYPE_KEY_TEXTBOX 	= "textbox";
	public static final String WIDGET_TYPE_KEY_BOOL_WLINK 	= "boolean+hyperlink";
	public static final String WIDGET_TYPE_KEY_SSELECT 	= "listbox";
	public static final String WIDGET_TYPE_KEY_LINK 		= "link";
	public static final String WIDGET_TYPE_KEY_PWD 		= "password";
	public static final String WIDGET_TYPE_KEY_BOOLEAN 	= "boolean";
	public static final String WIDGET_TYPE_KEY_FILE 		= "file";
	
	// the name of the configuration file name
	private String configResource;
	private String configLocation;

	// widget types
	public enum WidgetType { Label			// Plain label
			, Boolean						// Boolean(true/false) Combo box (SELECT)
			, BooleanWithLink				// Boolean(true/false) Combo box with hyperlink
			, CheckBox						// Checkbox
			, TextBox						// INPUT type=text (default)
			, TextArea						// TEXTAREA
			, ComboBox						// Single SELECT
			, DualMultiSelectNonEditable	// DUAL SELECT NON_EDITABLE (Requires a properties file of key=val pairs)
			, DualMultiSelectEditable		// DUAL SELECT EDITABLE
			, SingleMultiSelectEditable		// ONE SELECT W/ ADD DELETE BUTTONS
			, Hyperlink						// <a HREF=....
			, Password						// <INPUT TYPE=password
			, File							// <INPUT TYPE=file (file upload)
			};		
	
	// property items
	private final List<ConfigItem> items 		= new ArrayList<ConfigItem>();
	
	// Group keys. They render as HTML <FIELDSET>
	private final List<ConfigGroup> groupKeys 	= new ArrayList<ConfigGroup>();
	
	// used to render multiple widgets in 1 row
	private final List<ConfigRow> itemRows		= new ArrayList<ConfigRow>();
	
	// wrapper id
	private String id;
	
	// the configuration being wrapped
	private IConfiguration config;
	
	/**
	 * Common standard HTML 5 validators used in INPUY type=text pattern attribute
	 */
	private static final Map<String, String> HTML5_VALIDATORS = new HashMap<String, String>();
	
	static {
		HTML5_VALIDATORS.put("number", "\\d+");			// matches numbers only!
		HTML5_VALIDATORS.put("latin_alpha", "^\\w+$");	// matches alpha numeric string (latin)
		HTML5_VALIDATORS.put("ipv4", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");	// matches ip addres v4
		HTML5_VALIDATORS.put("date_mmddYYY", "\\d{1,2}/\\d{1,2}/\\d{4}");			// matches a date mm/dd/yyyy
	}
	
	/**
	 * Constructor
	 * @param configLocation Base path of the configuration. $HOME/.cloud/CloudAdapter
	 * @param configResource Resource file name.
	 * @param Id of this configuration wrapper.
	 * @throws IOException
	 */
	public ServiceConfiguration(final String configLocation, final String configResource, final String id) throws IOException {
		this.configResource = configResource;
		this.configLocation = configLocation;
		this.id				= id;
		internalLoad("Constructor");
	}
	
	public ServiceConfiguration(Properties properties) {
		config = new PropertiesConfig(properties);
		parseConfig();
	}
	
	private void internalLoad(final String label) throws IOException {
		log.debug("Internal load [" + label + "] Loc:" + configLocation + " Name:" + configResource);
		
		//config = new PropertiesConfig(configLocation, configResource); 
		config = loadConfiguration(configLocation, configResource);
		parseConfig();
		//dumpItems("ServiceConfiguration");
	}
	
	/**
	 * Load the configuration based on the configResource file type: .ini (INI) or .xml (XML)
	 * @throws IOException
	 */
	public static IConfiguration loadConfiguration(final String configLocation, final String configResource) throws IOException {
		String fileExt = FileTool.getFileExtension(configResource);
		
		// XML?
		if ( fileExt != null && fileExt.toLowerCase().equals(FileTool.FILE_EXT_XML)) {
			log.debug("Using XML Configuration with " + configResource);
			return new XmlConfig(configLocation, configResource); 
		}
		// Default - INI (Properties)
		log.debug("Using default (INI) Properties Configuration with " + configResource);
		return new PropertiesConfig(configLocation, configResource); 
	}
	
	public String getId() {
		return id;
	}
	
	/**
	 * Set a configuration id (or product type)
	 * @param productType The type of configuration: CALL_CENTER, CONTACT_CENTER,...
	 */
	public void setId (final String productType) {
		id = productType;
	}
	
	/**
	 * Get an attribute value for a given property
	 * @param key Property key that contains the attribute
	 * @param name Name of the attribute: label, group, or widget
	 * @return The attribute value or the KEY name if the value is NULL.
	 */
	private String getAttribute(String key, String name) {
		String a = config.getProperty(key + "_attribute_" + name);
		//  vsilva 9/9/15 Bug: links not displayed w/ extra spaces (Trim spaces from the attribute) 
		return a != null ? a.trim() : key;
	}

	/**
	 * The same as getAttribute. Returns null if the key is not found!
	 * @param key Key name
	 * @param nameAttribute name.
	 * @return Trimmed Attribute value or NULL!
	 */
	private String getAttributeOrNull(String key, String name) {
		String a = config.getProperty(key + "_attribute_" + name); 
		return a != null ? a.trim() : null;
	}
	
	/**
	 * Get the trimmed value for a key.
	 * @param Key name
	 * @return Trimmed value.
	 */
	private String getValue(String key) {
		String p = config.getProperty(key);
		return p != null ? p.trim() : null;
	}

	/**
	 * Query for an attribute
	 * @param key
	 * @param name
	 * @return true if the {@link ConfigItem} has the attribute
	 */
	private boolean hasAttribute(String key, String name) {
		String a = config.getProperty(key + "_attribute_" + name);
		return a != null;
	}

	/**
	 * Assign the left side vals to a non-editable DUAL multi-select from a property file in the class path
	 * @param path Properties path (within the class path)
	 * @param item
	 */
	private void assignMultiValsFromPath(String path, ConfigItem item) {
		InputStream is = null;
		try {
			Properties values 	= new Properties();
			is 		= ServiceConfiguration.class.getResourceAsStream(path);
			
			if ( is == null) {
				log.error("CFG: Failed to load values from path " + path);
				return;
			}
			values.load(is);
			
			item.multiValues = values;
		} catch (Exception e) {
			log.error("CFG: Failed to load values from path " + path + ": " + e.toString());
		}
		finally {
			if ( is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Assign multivalues to a {@link ConfigItem} from a CSV string.
	 * @param csv String of the form: key1:val1, key2:val2 ,...
	 * @param item
	 */
	private void assignMultiValsFromCSV(String csv, ConfigItem item) {
		try {
			Properties values = new OrderedProperties();
			String[] vals = csv.split(",");
			for (int i = 0; i < vals.length; i++) {
				String key = vals[i];
				String val = vals[i];
				
				if ( key != null && key.length() > 0) {
					values.put(key, val);
				}
			}
			
			item.multiValues = values;
		} catch (Exception e) {
			System.err.println("CFG: Failed to load values from CSV " + csv + ":" + e.toString());
		}
	}

	/**
	 * Assign a widget. The format is:
	 * <ul>
	 * <li> (PROPERTY) KEY = VAL
	 * <li> (WIDGET ATTR) KEY_attribute_widget = multivalue:capabilities.ini
	 * </ul>
	 * Widgets can be: label, textbox, boolean, dualmultiselect&[VALS_RES_PATH]. See {@link WidgetType}.
	 * @param key property key
	 * @param item {@link ConfigItem}
	 */
	private void assignWidget(final String key, final ConfigItem item) {
		final String val 			= config.getProperty(key);
		final String widget 		= config.getProperty(key + WIDGET_SUFFIX);
		final boolean hasWidget 	= widget != null;

		// default type: textbox
		item.type = WidgetType.TextBox;

		// boolean combo?
		if ( ((val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) && !hasWidget)  
				|| (hasWidget && widget.equalsIgnoreCase(WIDGET_TYPE_KEY_BOOLEAN))) {
			item.type = WidgetType.Boolean;
		}
		
		// Bool + hyperlink
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_BOOL_WLINK)) {
			item.type = WidgetType.BooleanWithLink;
		}
		
		// if comma-sep then assume single select combo
		if ( val.split(",").length > 1 && !hasWidget) {
			item.type = WidgetType.ComboBox; 
		}
		
		// Text area
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_TEXTAREA)) {
			item.type = WidgetType.TextArea;
		}

		// <INPUT TYPE=text
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_TEXTBOX)) {
			item.type = WidgetType.TextBox;
		}

		// <INPUT TYPE=password
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_PWD)) {
			item.type 		= WidgetType.Password;
			// 10/31/2019 http://acme208.acme.com:6091/issue/UNIFIED_CC-595 
			// item.required	= true;		// All passwords are required by default.
		}
		
		// check box
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_CHECKBOX)) {
			item.type = WidgetType.CheckBox;
		}

		// single select combo. Fmt: combo&[VALUES_FILE_RESOURCE_PATH or val1,val2,....]
		// Example: combo&/configuration/values.properties
		// Example: combo&val1,val2,...
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_COMBO)) {
			item.type 			= WidgetType.ComboBox;
			item.widgetTypeRaw	= widget;
			
			if (widget.contains(WIDGET_ATTR_FIELD_SEP)) {
				String[] vals 	= widget.split(WIDGET_ATTR_FIELD_SEP);
				String path 	= vals.length > 1 ? vals[1] : null;
				
				if ( path != null) {
					if ( path.startsWith("/")) {
						assignMultiValsFromPath(path, item);
					}
					else {
						assignMultiValsFromCSV(path, item);
					}
				}
			}
		}
		
		// dual multi-select boxes
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_DUALSELECT)) {
			// Default multivalue
			item.type 			= WidgetType.DualMultiSelectEditable;
			item.widgetTypeRaw	= widget;
			
			// load multi-vals fmt : dualmultiselect&[VALUES_FILE_RESOURCE_PATH or val1,val2,....]
			if (widget.contains(WIDGET_ATTR_FIELD_SEP)) {
				String[] vals 	= widget.split(WIDGET_ATTR_FIELD_SEP);
				String path 	= vals.length > 1 ? vals[1] : null;
				
				if ( path != null) {
					if ( path.startsWith("/")) {
						assignMultiValsFromPath(path, item);
						item.type = WidgetType.DualMultiSelectNonEditable;
					}
					else {
						assignMultiValsFromCSV(path, item);
						item.type = WidgetType.DualMultiSelectEditable;
					}
				}
			}
		}
		
		// single select editable 
		if ( hasWidget && widget.contains(WIDGET_TYPE_KEY_SSELECT)) {
			item.type = WidgetType.SingleMultiSelectEditable;
		}

		// link
		if ( hasWidget && widget.equalsIgnoreCase(WIDGET_TYPE_KEY_LINK)) {
			item.type = WidgetType.Hyperlink;
		}
		
		// label
		if ( hasWidget && widget.equalsIgnoreCase(WIDGET_TYPE_KEY_LABEL)) {
			item.type = WidgetType.Label;
		}
	
		// 7/11/2017 File upload
		if ( hasWidget && widget.equalsIgnoreCase(WIDGET_TYPE_KEY_FILE)) {
			item.type 	= WidgetType.File;
			
			// load file contents into item multiValues map. Note item.value has the file name.
			// Note: item.value can have char data (on upgrades or be empty). Must validate
			final String filePath	= getDefaultFileFolder() + File.separator;
			String fileName			= item.value;	// could be empty, char data or a file name (upload_KEY).
			final String baseKey	= id != null ? item.key.replaceFirst(id, "") : item.key;
			
			try {
				FileWidget.setValue(item, baseKey, filePath, fileName);
				/* 10/4/2019 FileWidget cleanup
				if ( fileName == null || fileName.isEmpty()) {
					log.warn("File upload item " + baseKey + " value (file name) is invalid (cannot be empty)." );
					return;
				}
				// if the item.value is not empty and does not start w/ upload_ then it may be a widget upgrade?
				// 1/2/2019 if ( !fileName.startsWith("upload_")  ) {
				if ( !fileName.startsWith("upload_") || !FileTool.fileExists(filePath) || !FileTool.fileExists(filePath + fileName) ) {
					fileName = "upload_" + baseKey; // item.key;
					
					log.warn("File upload item " + baseKey + " value (file name) appears invalid: " + item.value);
					log.warn("File upload storing data as " + filePath + fileName);
					
					// create folder if missing
					if ( !FileTool.fileExists(filePath)) {
						IOTools.mkDir(filePath);
					}

					// Store data as file upload
					IOTools.saveText(filePath + fileName, item.value);
					item.value = fileName;
					//return;
				}
				//System.out.println("*** LOAD FILE:" + getDefaultFileFolder() + File.separator + item.value);
				if ( item.multiValues == null) {
					item.multiValues = new Properties();
				}
				// the key is item.value (file name)
				item.multiValues.put(item.value, IOTools.readFileFromFileSystem(filePath + fileName));
				*/
			} catch (Exception e) {
				log.error("Assign Wigdet: File upload failed for path " + filePath, e);
			}
		}
	}

	/**
	 * The config format:
	 * KEY = VAL
	 * KEY_attribute_label = Label
	 * KEY_attribute_widget = {@link WidgetType}
	 * if the property begins with the prefix (private) it will not be added to the config!
	 */
	private void parseConfig() {
		Set<Object> keys =  config.keySet();
		
		for ( Object okey : keys) {
			String key = okey.toString();
			
			// NON displayable props must contain the prefix private. -- private.{SOME-NAME}. Ignore _version too.
			if ( key.contains("private_") || key.equals("_version") ) {
				continue;
			}
			// If a group, grab the key & title. Fromat cfg_groupXXX = Title
			if ( key.startsWith("cfg_group")) {
				if ( !key.contains("_attribute_") ) {
					groupKeys.add(new ConfigGroup(key, getValue(key)));  
				}
				// TODO: Get a description attribute?
				continue;
			}
			if ( !key.contains("attribute_") ) {
				ConfigItem item = new ConfigItem();
				
				item.key 		= (id != null ? (id + key) : key);		// wrapper id + item key
				item.name		= key;
				item.value 		= getValue(key); // config.getProperty(key);
				item.label		= getAttributeOrNull(key, "label");
				item.group 		= getAttribute(key, "group");
				item.rowId		= getAttributeOrNull(key, "rowid");
				item.style		= getAttributeOrNull(key, "style");
				item.onclick	= getAttributeOrNull(key, "onclick");
				item.collapseRow = getAttributeOrNull(key, "collapserow");
				
				if (item.rowId != null) {
					itemRows.add(new ConfigRow(item.rowId, false));
				}
				
				// set the widget type
				assignWidget(key, item);
				
				// =================== Assign other attributes =====================
				// INPUT type=text optional attributes: placeholder, pattern
				if ( item.type == WidgetType.TextBox || item.type == WidgetType.Password ) {
					if (hasAttribute(key, "placeholder")) {
						item.placeHolder = getAttribute(key, "placeholder");
					}
					// HTML5 validator (pattern/regex)
					if (hasAttribute(key, "pattern")) {
						item.pattern 	= filterPattern(getAttribute(key, "pattern"));
					}
					// Get the HTML5 required attribute. All inputs type text are required by default.
					if (hasAttribute(key, "required")) {
						item.required	= getAttributeBoolean(key, "required", true);
					}
					else {
						item.required	= true;		// Required by default
					}
				}
				// title applies to all widgets.
				if (hasAttribute(key, "title")) {
					item.title 		= getAttribute(key, "title");
				}
				if ( item.group == null) {
					log.warn("No group for property " + key);
				}
				
				// decrypt pwds. Only if it starts w/ ENC:....
				if ( item.type == WidgetType.Password) {
					if ( item.value != null &&  EncryptionTool.isEncryptedText(item.value)) {
						String plain	= EncryptionTool.decryptTaggedPassword(item.value); 
						
						// Must set plain val for the props & items
						item.value 		= plain;
						setProperty(key, plain);
					}
				}
				
				items.add(item);
			}
		}
	}
	
	/**
	 * Parse a boolean attribute from the config (true/false)
	 * @param key property key.
	 * @param name desired attribute name.
	 * @param defValue Default value.
	 * @return True or false.
	 */
	private boolean getAttributeBoolean(String key, String name, boolean defValue) {
		String bool = getAttributeOrNull(key, name);
		try {
			return Boolean.parseBoolean(bool);
		} catch (Exception e) {
			log.error("Failed to parse boolean config key " + key + " " + name + ": " + e.toString());
			return defValue;
		}
	}
	
	/**
	 * Filter this pattern name thru a common set.
	 * @param key Pattern name (or regular expression). Example number evals to \d+
	 * @return Regexp pattern 
	 */
	private String filterPattern(String key) {
		if ( HTML5_VALIDATORS.containsKey(key)) {
			return HTML5_VALIDATORS.get(key);
		}
		// No name in default map. Return the key (it should be a valid regexp)
		return key;
	}

	public void dumpItems(String label) {
		System.out.println("-- " + label + " CONFIGURATION ID: " + id + " --");
		for (ConfigItem item : items) {
			System.out.println("[CFG ITEM] " + item);
		}
		for ( ConfigGroup grp : groupKeys) {
			System.out.println("[CFG GROUP] " + grp);
		}
	}

	public void dumpItems(String label, String group) {
		System.out.println("-- " + label + " --");
		for (ConfigItem item : items) {
			if ( item.group != null && item.group.equalsIgnoreCase(group)) {
				System.out.println("[CFG ITEM] " + item);
			}
		}
	}

	public List<ConfigItem> getItems() {
		return items;
	}
	
	/**
	 * Get items by group name
	 * @param groupName
	 * @return
	 */
	public List<ConfigItem> getItems(String groupName) {
		List<ConfigItem> gitems = new ArrayList<ConfigItem>();
		for (ConfigItem item : items) {
			if ( item.group != null && item.group.equalsIgnoreCase(groupName)) {
				gitems.add(item);
			}
		}
		return gitems;
	}

	/**
	 * Get a list if {@link ConfigItem} for a given row id.
	 * @param rowId
	 * @return
	 */
	public List<ConfigItem> getItemsForRowId (String rowId) {
		List<ConfigItem> ritems = new ArrayList<ConfigItem>();
		for (ConfigItem item : items) {
			if ( item.rowId != null && item.rowId.equalsIgnoreCase(rowId)) {
				ritems.add(item);
			}
		}
		return ritems;
	}
	
	public boolean isRowRendered (String rowId) {
		ConfigRow row = findRow(rowId);
		if ( row != null) {
			return row.rendered;
		}
		return false;
	}

	public void setRowRendered (String rowId) {
		ConfigRow row = findRow(rowId);
		if ( row != null) {
			row.rendered = true;
		}
	}
	
	private ConfigRow findRow(String rowId) {
		for (ConfigRow row: itemRows) {
			if ( row.id.equals(rowId)) {
				return row;
			}
		}
		return null;
	}
	
	public List<ConfigGroup> getGroups() {
		return groupKeys;
	}
	
	/**
	 * Get items of a given {@link WidgetType}
	 * @param type
	 * @return
	 */
	public List<ConfigItem> getItems(WidgetType type) {
		List<ConfigItem> witems = new ArrayList<ConfigItem>();
		
		for (ConfigItem item : items) {
			if ( item.type == type) {
				witems.add(item);
			}
		}
		return witems;
	}
	
	public String getConfigResource() {
		return configResource;
	}

	public String getConfigLocation() {
		return configLocation;
	}

	/**
	 * If any class properties are used, they must be refresehd after save!!
	 * @throws IOException 
	 */
	private void refreshVars(String label) throws IOException {
		// re-load any class vars here!!!
		// must clear items. They'll be reloaded by internaLoad
		items.clear();	
		groupKeys.clear();
		
		// reload stuff from Disk!
		internalLoad(label);
	}
	
	/**
	 * Refresh configuration. This will not save it to disk!
	 * Call this after the internal properties have been updated.
	 */
	public void refresh() {
		items.clear();	
		groupKeys.clear();
		parseConfig();
	}
	
	public void save() throws Exception {
		if ( !config.getLocation().equals(configLocation)) {
			log.debug("Save: config location has changed to " + configLocation);
			config.setLocation(configLocation);
		}
		
		// encrypt pwd. Format on disk ENC:...
		for ( ConfigItem item : items) {
			if ( item.type == WidgetType.Password) {
				// Note: item.key has the wrapper id concatenated.
				String key 		= id != null ? item.key.replace(id, "") : item.key; 
				String plain	= getProperty(key);			// new value to be encrypted (from html)
				
				// 8/27/2020 Password masking: value has not changed. use (old) item.value
				if ( plain.equals(PASSWORD_MASK)) {
					plain = item.value;
				}
				String secret 	= EncryptionTool.encryptAndTagPassword(plain); 

				setProperty(key, secret);
			}
		}
		config.save("UPDATED BY TOMCAT - DO NOT EDIT");
		
    	// This will reload the whole thing & refresh local vars.
    	refreshVars("RELOAD-AFTER-SAVE");
	}

	/**
	 * Append props to a {@link JSONObject}
	 * @param root {@link JSONObject} to append key value pairs to.
	 * @param ignoreMetadata If true ignore the service meta data: widget types, labels, etc.
	 * @param stripKeys If true strip the ordering prefix from every key nn_NN_KEY => KEY.
	 * @param appendId If true append the configuration id (type) to each key.
	 * @throws JSONException On JSON I/O errors.
	 */
	public void appendToJSON(JSONObject root, boolean ignoreMetadata, boolean stripKeys, boolean appendId) throws JSONException {
		appentToObject(root, ignoreMetadata, stripKeys, appendId);
	}

	/**
	 * Append this {@link Properties} to another set
	 * @param root {@link Properties} to append to.
	 * @param ignoreMetadata If true ignore the service meta data: widget types, labels, etc.
	 * @param stripKeys If true strip the ordering prefix from every key nn_NN_KEY => KEY.
	 */
	public void appendTo(Properties root, boolean ignoreMetadata, boolean stripKeys) {
		appentToObject(root, ignoreMetadata, stripKeys, false);
	}

	/**
	 * Append props to a {@link JSONObject}
	 * @param root {@link JSONObject} to append key value pairs to.
	 * @throws JSONException  On JSON I/O errors.
	 */
	public void appendToJSON(JSONObject root) throws JSONException {
		appentToObject(root, true, true, false);
	}

	/**
	 * Append this {@link Properties} to another set
	 * @param root Properties to append to.
	 */
	public void appendTo(Properties root) {
		appentToObject(root, true, true, false);
	}
	
	/**
	 * Strip prefix garbage from a key: a_b_c_name -> name
	 * @param key Some key
	 * @return key a_b_c_name -> name
	 */
	private String stripKey (String key ) {
		// strip garbage: a_b_c_name -> name
		String[] vals 		= key.split("_");
		return vals.length > 1 ? vals[vals.length -1] : vals[0];
	}
	
	/**
	 * Inject the {@link ServiceConfiguration} key,value pairs into a {@link JSONObject} or {@link Properties}.
	 * @param obj A {@link JSONObject} or {@link Properties} hash map.
	 */
	private void appentToObject (Object obj, boolean ignoreMeta, boolean stripKeys, boolean appendId) {
		Set<Object> keys = config.keySet();
		
		for (Object okey : keys) {
			String key 		= okey.toString();
			String value 	= null;
			
			boolean ignore = ( key.contains("attribute") 
					|| key.contains("cfg_group") || key.startsWith("private_") ) && ignoreMeta;

			String strippedKey 	= stripKeys ? stripKey(key) : key;
			
			try {
				if ( !ignore) {
					value = config.getProperty(key);
				}
				else {
					// Load the data for file upload widgets
					if ( key.contains(WIDGET_SUFFIX)) {
						final String dataKey = key.replaceFirst(WIDGET_SUFFIX, "");
						final String widget = config.getProperty(key);
						final String file	= config.getProperty(dataKey);
						
						if ( widget != null && widget.equals(WidgetType.File.name()) && !file.isEmpty()) {
							value 		= IOTools.readFileFromFileSystem(getDefaultFileFolder() + File.separator + file);
							strippedKey = stripKeys ? stripKey(dataKey) : dataKey;
						}
					}
				}
				if ( value == null) {
					continue;
				}
				if ( obj instanceof Properties) {
					((Properties)obj).put(appendId ? id + strippedKey : strippedKey, value);
				}
				else {
					((JSONObject)obj).put(appendId ? id + strippedKey : strippedKey, value);
				}
				
			} catch (Exception e) {
				log.error("Assign service config to object key: " + key, e);
			}
		}
	}
	
	static public String deriveBoolComboLinkKey(String comboKey) {
		return "hyperlink_" + comboKey;
	}
	
	/**
	 * Bool Combo Hyperlink checkboxes must be reset to false when saving 
	 */
	public void resetBoolComboHyperLinks () {
		for (ConfigItem item : items) {
			if ( item.type == WidgetType.BooleanWithLink) {
				String key = deriveBoolComboLinkKey(item.key);
				config.setProperty(key, "false");
			}
		}
	}
	
	public String getProperty(String key) {
		return config.getProperty(key);
	}

	public void setProperty(String key, String value) {
		config.setProperty(key, value);
	}

	public Object put (Object key, Object value) {
		return config.put(key, value);
	}

	/**
	 * Re-set the location (on disk) of the inner {@link IConfiguration}.
	 * @param location New location on disk.
	 */
	public void setLocation(String location) {
		if ( location != null && !location.equals(configLocation)) {
			log.debug("setLocation: Configuration location changed to " + location);
			configLocation = location;
			config.setLocation(location);
		}
	}
	
	/**
	 * Get the inner {@link IConfiguration}.
	 * @return Internal {@link IConfiguration}.
	 */
	public IConfiguration getConfiguration () {
		return config;
	}
	
	/**
	 * Get a group by key. Groups must start with the prefix cfg_group.
	 * @param key  Key of the group: cfg_groupXXX
	 * @return
	 */
	public ConfigGroup getGroup (String key) {
		for (ConfigGroup grp : groupKeys) {
			if ( grp.name.equals(key))
				return grp;
		}
		return null;
	}

	/**
	 * Has a group? Groups must start with the prefix cfg_group.
	 * @param key
	 * @return True if group exists (cfg_group)
	 */
	public boolean hasGroup (String key ) {
		for (ConfigGroup grp : groupKeys) {
			if ( grp.name.equals(key))
				return true;
		}
		return false;
	}

	/**
	 * Convert a service configuration into an XML format.
	 * @return Service configuration XML. See {@link XmlConfig} for the format.
	 */
	public String toXml () {
		StringBuffer xml = new StringBuffer("<configuration>");
		
		// add version
		if ( config.containsKey(PropertiesConfig.KEY_VERSION)) {
			xml.append("\n<version>" + config.getProperty(PropertiesConfig.KEY_VERSION) + "</version>");
		}
		// add items: <property>...</property>
		for (ConfigItem item : items) {
			xml.append(item.toXML());
		}
		// Add groups: <group>...</group>
		for ( ConfigGroup grp : groupKeys) {
			xml.append(grp.toXML());
		}
		// add privates (private_...)
		Set<Entry<Object, Object>> entries = config.entrySet();
		
		for (Entry<Object, Object> entry : entries) {
			if ( entry.getKey().toString().startsWith("private_")) {
				final String value = entry.getValue().toString().trim();
				
				xml.append("\n<private><name>" + entry.getKey() + "</name><value>" 	
						+ ( ! value.isEmpty() ? "<![CDATA[" + entry.getValue() + "]]>" : "" ) 
						+ "</value></private>");
			}
		}
		xml.append("\n</configuration>");
		return xml.toString();
	}
	
	/**
	 * True if a configuration key is new from an update.
	 * @param key Configuration key.
	 * @return True if new from an update.
	 */
	public boolean isNew(String key) {
		String realKey = key.replaceAll(id, "");
		return config.isNew(realKey);
	}
	
	/**
	 * Get Default file upload storage folder.
	 * @return $HOME\.cloud\CloudReports\Profiles\[PROFILE-NAME]\files
	 */
	static public String getDefaultFileFolder () {
		return CloudServices.getNodeConfig().getDefaultProfileBasePath() + File.separator + "files";
	}

	/**
	 * Convert to JSON.
	 * @return JSON properties: { key1: val1, ....}
	 * @throws JSONException On JSON I/O errors.
	 */
	public JSONObject toJSON () throws JSONException {
		JSONObject root = new JSONObject();
		appendToJSON(root, false, false, false);
		
		// add groups
		for (ConfigGroup g : groupKeys) {
			root.put (g.name, g.title);
		}
		return root;
	}
	
	/**
	 * Convert to JSON.
	 * @param appendId If true append the configuration id (type) to each key.
	 * @return JSON properties: { key1: val1, ....}
	 * @throws JSONException On JSON I/O errors.
	 */
	public JSONObject toJSON (boolean appendId) throws JSONException {
		JSONObject root = new JSONObject();
		appendToJSON(root, false, false, appendId);
		
		// add groups
		for (ConfigGroup g : groupKeys) {
			root.put (g.name, g.title);
		}
		return root;
	}
	
}
