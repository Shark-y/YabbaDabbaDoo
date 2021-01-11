package com.cloud.console.jsp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.JSPLoggerTool;
import com.cloud.console.servlet.FileUpload;
//import com.cloud.console.servlet.FileUpload.FileItem;
import com.cloud.core.config.ConfigGroup;
import com.cloud.core.config.ConfigItem;
import com.cloud.core.config.FileWidget;
import com.cloud.core.config.FileItem;

import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.config.ServiceConfiguration.WidgetType;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.Auditor;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;
import com.cloud.core.provider.IServiceLifeCycle;
import com.cloud.core.provider.IServiceLifeCycleV2;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.PluginSystem.Plugin;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.WebClient;

/**
 * This class contains helper functions used by the JSP Node configuration (config_backend.jsp).
 * It is meant to simplify the JSP and make it easier to understand.
 * 
 * @author VSilva
 * 
 * @version 1.0.0
 * @version 1.0.1 - 3/1/2019 Included the remote configuration logic from the cluster manager app.
 *
 */
public class JSPBackendConfig {

	/** Max size of an HTTP INPUT type=text */
	public static final int MAX_HTTP_TXT_LEN 		= 128;

	/** Max size of an HTTP textarea */
	public static final int MAX_HTTP_TXTAREA_LEN 	= 256;

	/** Max size of a file upload item 20K */
	public static final int MAX_FILE_UPLOAD_LEN 	= 20480;

	public static void LOGD(String text) {
		System.out.println("[CFG-BACKEND-DBG] " +text);
	}

	public static void LOGW(final String text) {
		System.err.println("[CFG-BACKEND-WRN] " +text);
	}

	public static void LOGE(final String text) {
		System.err.println("[CFG-BACKEND-ERR] " +text);
	}

	/**
	 * Set a property value to from the HttpRequest into the corresponding Hashmap.
	 * @param prefix Indicates the target hashmap : gui. go to the GUI HM, server. (Server) or backend. (Backend). Only keys that begin w/ prefix are consumed.
	 * @param removePrefix If true will remove the prefix  from the HTTP property name.
	 * @param name The name of the HTTP param (property to update).
	 * @param value The value of the HTTP param (property to update).
	 * @param properties The service configuration object. 
	 */
	public static void wrapperSetProperty(final String prefix, final boolean removePrefix, final String name, final String value, final ServiceConfiguration properties) {
		if ( properties == null) {
			//LOGE("Invalid config wrapper for pref " + prefix + " " + name + ":" + value);
			return;
		}
		// remove the prefix?
		final String key = removePrefix ? name.replaceFirst(prefix, "") : name;
		
		if ( name.startsWith(prefix)) {
			// the real val is formated as: label|real value
			// Audit...
			if ( !value.equals(properties.getProperty(key))) {
				Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.CONFIGURATION_CHANGED, "Service [" + properties.getId() + "] item changed (" + key + ") " 
					+ properties.getProperty(key) + " => " + value); 
			}
			properties.setProperty(key, value.trim()); 
		}
		// Add/set multi vals attribute to property: multivals_CALL_CENTER01_03_agentStateReasonCodes=e:f,...
		else if ( name.startsWith("multivals_")) {
			final String widgetKey 	= key.replaceFirst("multivals_", "") + "_attribute_widget";
			final String widgetVal	= "dualmultiselect&" + value.trim();
			
			//LOGD("Set multi vals " + widgetKey + " = " +  widgetVal);
			properties.setProperty(widgetKey, widgetVal);
		}
		else {
			LOGE(properties.getId() +  " Unable to update  " + name + ". Bad prefix " + prefix);
		}
	}

	/**
	 * @deprecated Updating server properties has been moved to config_node.jsp & JSPServiceConfig.java. Don't use this stuff.
	 * @param prefix Indicates the target hashmap : gui. go to the GUI HM, server. (Server) or backend. (Backend). Only keys that begin w/ prefix are consumed.
	 * @param removePrefix If true will remove the prefix  from the HTTP property name.
	 * @param name The name of the HTTP param (property to update).
	 * @param value The value of the HTTP param (property to update).
	 * @param properties The node configuration map {@link Properties}. 
	 */
	static void serverSetProperty(final String prefix, final boolean removePrefix, final String name, final String value, final Properties properties) {
		if ( name.startsWith(prefix)) {
			// remove the prefix?
			String key = name;
			
			if ( removePrefix) {
				key 	= name.replaceFirst(prefix, "");
			}
			
			// the real val is formated as: label|real value
			properties.setProperty(key, value.trim()); 
		}
	}

	/**
	 * Render a collection of items in a single row
	 */
	public static void renderInSingleRow(final ServiceConfiguration wrapper, final ConfigItem item, JspWriter out) throws IOException {
		//LOGD("Single row: " + item);

		final List<ConfigItem> items  = wrapper.getItemsForRowId(item.rowId);
		
		StringBuffer HTML 	= new StringBuffer("\n\t<div class=\"form-group\">");
		HTML.append(item.type != WidgetType.Hyperlink 
							? "\n\t<label class=\"col-sm-2 control-label\">" + item.label  + "</label>" + "\n\t<div class=\"col-sm-10\">"
							: "\n\t<label class=\"col-sm-2 control-label\"></label><div class=\"col-sm-10\">"); 
		
		int count = 0;
		
		for ( ConfigItem ci : items) {
			/*
			if ( count > 0)
				HTML += ci.label + " "; */
		
			HTML.append( ci.toHTML()); //HTML += ci.toHTML();
			
			if ( count++ < items.size() - 1) {
				HTML.append("&nbsp;"); //HTML += "&nbsp;";
			}
		}
		HTML.append("\n\t</div></div>"); //HTML += "\n\t</div></div>";
		
		if ( wrapper.isRowRendered(item.rowId)) {
			//LOGW("Row: " + item.rowId + " already rendered.");
			return;
		}
		out.println(HTML);
		
		// mark row as rendered! So It won't display multiple times!!	
		wrapper.setRowRendered(item.rowId);
	}

	/**
	 * Get a default profile name. From the HTTP rq or Server cfg.
	 */
	public static String getDefaultProfile(HttpServletRequest request, final NodeConfiguration config) {
		final String name1		= request.getParameter("n");			// profile name from http rq
		final String name2 		= config.getConnectionProfileName();	// profile name from cfg

		if ( name1 != null)		return name1;
		if ( !name2.isEmpty())	return name2;
		return null;
	}

	/**
	 * Update the node and service configurations from the HTTP request.
	 * @deprecated This function cannot handle file uploads. use updateWrapper instead.
	 * @param label A debug label.
	 * @param request The HTTP request.
	 * @param cfgServer The Node configuration.
	 * @param cfgWrapper The service configuration.
	 */
	public static void updateServerAndWrapper(final String label , HttpServletRequest request, final NodeConfiguration cfgServer, final ServiceConfiguration cfgWrapper) {	
		/**
		 * Save properties. Server props begin with server_ 
		 * Message broker props begin with chat_
		 * Contact center with om_
		 */
		final Enumeration<String> names = request.getParameterNames();
		
		while ( names.hasMoreElements()) {
			String name 		= names.nextElement().toString();
			String value		= request.getParameter(name);
			String[] vals		= request.getParameterValues(name); // never null!
			boolean multiVal 	= vals.length > 1;
			
			if ( multiVal) {
				value = IOTools.join(vals, ",");
			}
			//LOGD(cfgWrapper.getId() + "  " + label +  " Param: " + name + "=" + value);
			
			// set property values from the HTTP request (based on prefix)
			wrapperSetProperty( /*request,*/ cfgWrapper.getId() , true, name, value, cfgWrapper); 
			serverSetProperty(/*request,*/ "server_", false, name,  value, cfgServer);

			// update the config locations from the server config
			if ( name.equalsIgnoreCase("server_configPath")) { 
				cfgWrapper.setLocation(value);
			}
		}
	}

	/**
	 * Update the node and service configurations from the HTTP request.
	 * @param label A debug label.
	 * @param request The HTTP request.
	 * @param serviceCfgr The service configuration.
	 * @throws FileUploadException if file fails to upload.
	 * @throws IOException on I/O errors.
	 * @throws ServletException 
	 * @throws IllegalStateException 
	 */
	public static void updateServiceConfiguration(final String label , HttpServletRequest request, final ServiceConfiguration serviceCfg) 
			throws  IOException, ServletException 
	{	
		final String saveFolder 				= ServiceConfiguration.getDefaultFileFolder();
		//final List<FileItem> items 	= FileUpload.parseRequest(request, false, saveFolder); // CANT HANDLE HTTP MULTI SLECT - 
		final Map<String, List<FileItem>> items = FileUpload.parseMultiValuedRequest(request, false, saveFolder);
		
		JSPLoggerTool.JSP_LOGD(serviceCfg.getId() + "-" + label, "Got " + items.size() + " items from HTTP request.");
		
		// bad (web.xml) ?
		if ( items.size() == 0 ) {
			final String msg		= "Unable to extract items from HTTP request. If the content type is multipart/form-data this may indicate an invalid web descriptor (web.xml).";
			final IOException e 	= new IOException(msg);
			JSPLoggerTool.JSP_LOGE(serviceCfg.getId(), msg, e);
			throw e;
		}
		/* CANT HANDLE HTTP MULTI SLECT with multipart/form-data 
		for (FileItem item : items) {
			if (item.isFormField()) {
				processFormField(item, serviceCfg);
			}
			else {
				processUploadedFile(item, serviceCfg);
			}
		} */
		for ( Map.Entry<String, List<FileItem>> entry :  items.entrySet()) {
			final String name  			= entry.getKey();
			final List<FileItem> list 	= entry.getValue();
			final FileItem item			= list.get(0); 		// first
			
			if (item.isFormField()) {
				// join vals with comma
				final StringBuffer buf = new StringBuffer();
				for ( FileItem fi : list) {
					buf.append(fi.getString(CoreTypes.ENCODING_UTF8) + ",");
				}
				String values 	= buf.toString();
				values 			= values.substring(0, values.length() - 1);
				
				//System.out.println("---FrM FLD " + name + "=" + values);
				processFormField(name, values, serviceCfg);
			}
			else {
				processUploadedFile(item, serviceCfg);
			}
		} 
	}
	
	/**
	 * Process a standard HTTP form field. The its value into the {@link ServiceConfiguration} map.
	 * @deprecated This method can't handle multi values HTTP params.
	 * @param item The file upload {@link FileItem}.
	 * @param config The service configuration. See {@link ServiceConfiguration}.
	 * @throws UnsupportedEncodingException 
	 */
	static void processFormField(final FileItem item, final ServiceConfiguration config) throws IOException {
		processFormField(item.getFieldName(), item.getString(CoreTypes.ENCODING_UTF8), config);
	}
	
	private static void processFormField(final String name, final String value, final ServiceConfiguration config) throws IOException {
		wrapperSetProperty( config.getId() , true, name, value, config); 
		
		// update the config locations from the server config
		if ( name.equalsIgnoreCase("server_configPath")) { 
			config.setLocation(value);
		}
	}

	/**
	 * Process a file upload item. Store its value into the file upload folder: ${HOME}\.cloud\${PRODUCT}\Profiles\${PROFILE}\files.
	 * Where the file name is: upload_{HTTP_ITEM_FIELD_NAME}
	 * @param item The file upload item.
	 * @param config The service configuration. See {@link ServiceConfiguration}.
	 * @throws IOException if there is an I/O error storing the data.
	 */
	private static void processUploadedFile(final FileItem item, final ServiceConfiguration config) throws IOException {
		// upload files folder C:\Users\vsilva\.cloud\CloudReports\Profiles\Default\files
		//final String folder 	= ServiceConfiguration.getDefaultFileFolder();
		final String field 		= item.getFieldName();

		final String value		= FileWidget.upload(item, config);

		if ( value == null) {
			LOGE("Process file upload: Invalid/empty file name for item " + field);
			return;
		}
		/* 10/4/2019 File widget cleanup
		// Remove the service prefix from the field name (if possible) CALL_CENTER04_09_callLogCustomFields => 04_09_callLogCustomFields
		// 10/3/2019 final String fileName 	= config.getId() != null ? field.replaceFirst(config.getId(), "") : field;
		final String fileName 	= item.getName();
		
		// file name format: upload_[FIELD_NAME]
		final String value		= "upload_" + fileName; 
		final String path		= folder + File.separator + value;
		
		if ( item.getName() == null || item.getName().isEmpty()) {
			LOGE("Process file upload: Invalid/empty file name for item " + field);
			return;
		}

		// create folder if missing
		if ( !FileTool.fileExists(folder)) {
			IOTools.mkDir(folder);
		}

		// read file data...
		final String data 		= IOTools.readFromStream(item.getInputStream());
		
		// check size limit
		if ( data.length() > MAX_FILE_UPLOAD_LEN) {
			throw new IOException("File upload size limit exceeded for " + fileName + ": " + data.length() + " > " + MAX_FILE_UPLOAD_LEN);
		}
		
		// save file
		IOTools.saveText(path, data);
		*/
		// set the file contents as the value for the property.
		wrapperSetProperty( config.getId() , true, field, value, config);
	}
	
	/**
	 * Invoked by the service config JSP to display file upload information.
	 * @param citem File Upload {@link ConfigItem}.
	 * @return &lt;a href=\"#\" data-toggle=\"modal\" data-target=\"#ModalDialog\" onclick=\"modalSetTextFromUrl('SysAdmin?op=fuget&f=" + citem.value + "')\">" + citem.value + "&lt;/a>"
	 *  
	 */
	public static String getFileUploadHelperHTML (ConfigItem citem) {
		final int size = citem.multiValues.values().iterator().next().toString().length();
		return "<a href=\"#\" data-toggle=\"modal\" data-target=\"#ModalDialog\" onclick=\"modalSetTextFromUrl('SysAdmin?op=fuget&f=" + citem.value + "')\">" 
				+ citem.value + "</a> Size: "  + size;
	}
	
	/**
	 * Invoked for every action in the JSP: save/validate config, etc.
	 * @param action action string: save, validate, etc.
	 * @param session HTTP session.
	 * @param request HTTP request.
	 * @param cfgWrapper Service configuration. See {@link ServiceConfiguration}
	 * @param cfgServer Node configuration {@link NodeConfiguration}.
	 * @param type See {@link ServiceType}
	 * @return A string [] where 0 = Action Message, 1 = Message type: INFO, ERROR, WARN.
	 * @throws Exception
	 */
	public static String[] action (String action, HttpSession session, HttpServletRequest request, ServiceConfiguration cfgWrapper, NodeConfiguration cfgServer, ServiceType type, String serviceId) 
		//throws Exception
	{
		String uiMessage 	= null;
		String statusType	= null;
		
		if ( action == null) {
			return new String[] {null, null};
		}
		
		if ( action.equals("save")) {
			try {
				JSPBackendConfig.updateServiceConfiguration("SAVE", request, cfgWrapper);
				
				// create dest location
				if ( !IOTools.mkDir(cfgServer.getConfigLocation()) ) {
					throw new IOException("Unable to save. Can't create " + cfgServer.getConfigLocation());
				}
				cfgWrapper.save();
				//cfgWrapper.dumpItems("SAVE");
				//CloudServices.clusterUpdateMemberStatus();
				
				// notify service about the save...
				IServiceLifeCycle service = CloudServices.findService(type); 
				if ( (service != null) && (service instanceof IServiceLifeCycleV2) ) {
					((IServiceLifeCycleV2)service).onServiceSaveConfig(cfgWrapper.getConfiguration());
				}
				// Notify plugins
				if ( serviceId != null && (type == ServiceType.PLUGIN) ) {
					ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(serviceId);
					Plugin p 				= PluginSystem.findInstance(sd.getClassName());
					((IServiceLifeCycleV2)p.getInstance()).onServiceSaveConfig(cfgWrapper.getConfiguration());
				}

				// FindBugs: Nullcheck of cfgWrapper at line 394 of value previously dereferenced in com.cloud.console.jsp.JSPBackendConfig.action(String, HttpSession, HttpServletRequest, ServiceConfiguration, NodeConfiguration, ServiceDescriptor$ServiceType)
				//if ( cfgWrapper != null) {
					uiMessage 	= "Configuration saved. <a href=\"" + request.getServletContext().getContextPath() + "\">Restart</a> is required.";
					statusType	= "SUCCESS";
					
					// clear the last error.
					CloudServices.clearLastError();
				//}
			}
			catch (Exception e1) {
				e1.printStackTrace();
			
				JSPBackendConfig.LOGE("Config Save Error:" + e1.toString());
				
				uiMessage 	= e1.getMessage();
				statusType	= "ERROR";
				
			}
			session.setAttribute( request.getParameter("mode") + "saved", "true");
		}
		else if ( action.equals("validate")) {
			try {
				JSPBackendConfig.updateServiceConfiguration("VALIDATE", request, cfgWrapper);
				
				// must refresh after update!
				cfgWrapper.refresh();

				// now... validate them!
				// This cannot be NULL!
				CloudServices.findService(type).onServiceValidateConfig(cfgWrapper.getConfiguration());
				uiMessage = "Configuration appears valid. Click save now.";
			}
			catch (Exception ex) {
				uiMessage 	= ex.getMessage();
				statusType	= "ERROR";
			}
			//cfgChatBackend.dumpItems("VALIDATE");
		}
		else {
			JSPBackendConfig.LOGW("Invalid action:" + action);
		}
		return new String[] {uiMessage, statusType};
	}
	
	/**
	{"get": {"method": "GET", "url": "http://localhost:8080/Node001/SysAdmin?op=confget&productType=CALL_CENTER", "headers": {}},
		"store" : {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=confstore&productType=CALL_CENTER", "headers": {}}
	}
	 * @throws JSONException 
	 * @throws IOException */
	public static ServiceConfiguration remoteLoadServiceConfig ( String json, String productType) throws JSONException, IOException {
		JSONObject root 			= new JSONObject(json);

		// http://localhost:8080/Node001/SysAdmin?op=confget&productType=CALL_CENTER
		final String url 			= root.getJSONObject("get").getString("url");
		WebClient wc 				= new WebClient(url);
		final String resp 			= wc.doGet();	

		JSONObject conf				= new JSONObject(resp);
		ServiceConfiguration cfg 	= new ServiceConfiguration(jsonCfgToProperties(conf));
		List<ConfigGroup> groups	= cfg.getGroups();
		
		// no id in cfg?
		if ( cfg.getId() == null) {
			cfg.setId(productType);
		}
		// no groups? Add a default
		if ( groups.isEmpty()) {
			ConfigGroup def = new ConfigGroup("cfg_group00_default", "Default");
			cfg.setProperty(def.name, def.title);

			// bind items to grp
			List<ConfigItem> items =  cfg.getItems();
			for ( ConfigItem item : items) {
				item.group = def.name;
			}
			groups.add(def);
		}
		return cfg;
	}

	static Properties jsonCfgToProperties (JSONObject config) throws JSONException {
		Properties props = new Properties();
		Set<Object> keys = config.keySet();
		
		for (Object key : keys) {
			props.put(key, config.get(key.toString()));
		}
		return props;
	}
	
	public static String maskPassword (String value) {
		return value != null && !value.isEmpty() ? ServiceConfiguration.PASSWORD_MASK : value;
	}
}
