package com.cloud.cluster.jsp;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cloud.core.config.ConfigGroup;
import com.cloud.cloud.core.config.ConfigItem;
import com.cloud.cloud.core.config.ServiceConfiguration;
import com.cloud.cloud.core.w3.WebClient;

/**
 * Helper class for config_backend.jsp
 * 
 * @author VSilva
 *
 */
public class JSPConfigBackend {

	/**
	{"get": {"method": "GET", "url": "http://localhost:8080/Node001/SysAdmin?op=confget&productType=CALL_CENTER", "headers": {}},
		"store" : {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=confstore&productType=CALL_CENTER", "headers": {}}
	}
	 * @throws JSONException 
	 * @throws IOException */
	public static ServiceConfiguration loadRemoteServiceConfig ( String json, String productType) throws JSONException, IOException {
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

	/**
	{"get": {"method": "GET", "url": "http://localhost:8080/Node001/SysAdmin?op=confget&productType=CALL_CENTER", "headers": {}},
		"store" : {"method": "POST", "url": "http://localhost:8080/Node001/SysAdmin?rq_operation=confstore&productType=CALL_CENTER", "headers": {}}
	}
	 * @throws JSONException 
	 * @throws IOException 
	 */
	/*
	public static void saveRemoteServiceConfig (String json, HttpServletRequest request, final ServiceConfiguration serviceCfg) throws Exception {
		JSONObject root = new JSONObject(json);

		// http://localhost:8080/Node001/SysAdmin?op=confget&productType=CALL_CENTER
		String url 		= root.getJSONObject("store").getString("url");
		System.out.println("SAVE " + url);
		
		System.out.println(HTTPServerTools.dumpHeaders("HEADERS", request));
		
		String body = HTTPServerTools.getRequestBody(request);
		System.out.println("-- HTTP BODY\n" + body);
		//updateServiceConfiguration("SAVE", request, serviceCfg);
	}
	*/
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
	/*
	public static void updateServiceConfiguration(final String label , HttpServletRequest request, final ServiceConfiguration serviceCfg) 
			throws  IOException, ServletException 
	{	
		final String saveFolder 				= ServiceConfiguration.getDefaultFileFolder();
		final Map<String, List<FileItem>> items = FileUpload.parseMultiValuedRequest(request, false, saveFolder);
		
		JSPLoggerTool.JSP_LOGD(serviceCfg.getId() + "-" + label, "Got " + items.size() + " items from HTTP request.");
		
		// bad (web.xml) ?
		if ( items.size() == 0 ) {
			final String msg		= "Unable to extract items from HTTP request. If the content type is multipart/form-data this may indicate an invalid web descriptor (web.xml).";
			final IOException e 	= new IOException(msg);
			JSPLoggerTool.JSP_LOGE(serviceCfg.getId(), msg, e);
			throw e;
		}
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
				
				System.out.println("---FrM FLD " + name + "=" + values);
				//processFormField(name, values, serviceCfg);
			}
			else {
				System.out.println("FILe upload " + item);
				//processUploadedFile(item, serviceCfg);
			}
		} 
	} */

	static Properties jsonCfgToProperties (JSONObject config) throws JSONException {
		Properties props = new Properties();
		Set<Object> keys = config.keySet();
		
		for (Object key : keys) {
			props.put(key, config.get(key.toString()));
		}
		return props;
	}
}
