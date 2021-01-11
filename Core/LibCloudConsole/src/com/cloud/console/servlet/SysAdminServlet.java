package com.cloud.console.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.helpers.DefaultHandler;

import com.cloud.console.HTTPServerTools;
import com.cloud.console.JSPLoggerTool;
import com.cloud.console.ThemeManager;
import com.cloud.console.iam.IAM;
import com.cloud.console.servlet.FileUpload;
import com.cloud.core.config.FileItem;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.io.ZipTool;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.ProfileManager;
import com.cloud.core.services.ProfileManager.ProfileDescriptor;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.types.CoreTypes;

/**
 * Master Cloud Adapter Servlet. Meant to be invoked by the cluster manager to do
 * system administration tasks.
 * <ol>
 * <li> Start/Stop services.
 * <li> Other Sys admin tasks.
 * <li> Import connection profiles.
 * </ol>
 *  
 * @author VSilva
 * @version 1.0.1 - 10/22/2017 Import a connection profile.
 * @version 1.0.2 - 01/03/2019 Get/Store service configuration.
 * @version 1.0.3 = 09/05/2020 Added IAM support.
 *
 */
@WebServlet( name = "SysAdminServlet", urlPatterns = {"/SysAdmin"})
@MultipartConfig
public class SysAdminServlet extends HttpServlet {

	private static final long serialVersionUID 		= 5202518713548847655L;
	
	static final String PARAM_OPERATION 	= "rq_operation";
	
	private static final String OP_START 	= "start";
	private static final String OP_STOP 	= "stop";
	private static final String OP_IMPORT 	= "importProf";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		final String op = req.getParameter("op");
	
		if ( op == null) {
			return;
		}
		// file upload: get file contents
		if ( op.equals("fuget")) {
			final String folder = ServiceConfiguration.getDefaultFileFolder();
			final String name 	= req.getParameter("f");
			final String path 	= folder + File.separator + name.replaceAll("[\\\\/]\\.\\.", "");;
			
			final String data 	= IOTools.readFileFromFileSystem(path);
			
			resp.setContentType(getContentType(name, data)); // CoreTypes.CONTENT_TYPE_TEXT_PLAIN);
			resp.getWriter().print(data);
		}
		else if ( op.equals("exportProf")) {
			final String name 		= req.getParameter("name");
			final String filename 	= name + /*"-" + getServletContext().getContextPath().substring(1) + */ ".zip";
			
			ProfileManager pm 		= CloudServices.getNodeConfig().getProfileManager();
			ProfileDescriptor pd	= pm.find(name);
			
			// open the browser save as dlg
			resp.setHeader("Content-Type", "application/octet-stream");
			resp.setHeader("Content-Disposition", "attachement;filename=\"" + filename + "\"");
			ZipTool.zipFolder(pd.path, resp.getOutputStream());			
		}
		else if ( op.equals("confget")) {
			// get service config
			final String type = req.getParameter("productType");
			
			if ( type != null) {
				ServiceConfiguration conf 	= CloudServices.getServiceConfig(ServiceType.valueOf(type));
				try {
					resp.setContentType(CoreTypes.CONTENT_TYPE_JSON);
					resp.getWriter().print(conf.toJSON(true)); 
				} catch (JSONException e) {
					throw new IOException(e);
				}
			}
			else {
				throw new ServletException("Configuration: productType is required.");
			}
		}
		else if ( op.equals("plugins")) {
			// describe plugins
			resp.setContentType(CoreTypes.CONTENT_TYPE_JSON);
			resp.getWriter().print(PluginSystem.describe());
		}
		else if ( op.equals("iam_describe")) {
			// describe IAM
			resp.setContentType(CoreTypes.CONTENT_TYPE_JSON);
			resp.getWriter().print(IAM.describe());
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		// Enable unrestricted XDomain access. This is so UI clients can invoke this API via JQuery.
		// TODO: Security needs to be implemented around this stuff.
		resp.addHeader("Access-Control-Allow-Origin", "*");

		// response always in JSON format
		resp.setContentType(HTTPServerTools.CONTENT_TYPE_JSON);
		
		final String operation 	= req.getParameter(PARAM_OPERATION);
		
		// response.
		final PrintWriter out 	=  resp.getWriter();
		
		if ( operation == null) {
			out.print(HTTPServerTools.buildBaseResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Operation required.").toString());
			return;
		}
		
		try {
			if ( operation.equals(OP_START)) {
				CloudServices.startServices();
			}
			else if ( operation.equals(OP_STOP)) {
				CloudServices.stopServices();
			}
			// Import the 1st profile from a File upload
			else if ( operation.equals(OP_IMPORT)) {
				List<FileItem> files 	= FileUpload.parseRequest(req, false, null);
				final FileItem zip		= files.get(0);
				final String path 		= CoreTypes.TMP_DIR + File.separator + zip.getName(); // fileName;
				ProfileManager pm 		= CloudServices.getNodeConfig().getProfileManager();
				final String destPath	= pm.getProfilesHome();

				/**
				 *http://prd210.acme.com:6090/issue/CLOUD_CORE-127
				 * Check 4 malicious zip by cloning 2 streams: (1-check, 2-save)
				 */
				ByteArrayOutputStream bos 	= new ByteArrayOutputStream();
				IOTools.pipeStream(zip.getInputStream() /* is */, bos);
				byte[] data 				= bos.toByteArray();
				
				InputStream is1 = new ByteArrayInputStream(data);
				InputStream is2 = new ByteArrayInputStream(data);
				
				// 8/7/2020 - check for malicious zip using clone1
				try {
					ZipTool.checkForEvilZip(is1); 
				} catch (IOException evil) {
					JSPLoggerTool.JSP_LOGE("[IMPORT-PROFILE]", "Security violation", evil);
					is1.close(); is2.close(); bos.close();
					
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, evil.getMessage());
					return;
				}
				
				// ok, save using clone2
				IOTools.saveStream(path, is2); 
				ZipTool.unzip(path, destPath);
				
				// back to profiles
				is1.close(); is2.close(); bos.close();
				resp.sendRedirect("jsp/config/profiles.jsp");
				return;
			}
			// Save a remote configuration
			else if ( operation.equals("confstore")) {
				final String mode	= req.getParameter("productType");	// cannot be null
				//final String ctx 	= req.getServletContext().getContextPath();
				final String target = /*ctx +*/ "jsp/config/config_backend.jsp?action=save&mode=" + mode;
				
				// [SERVER] Creating Servlet session: 375CD41C8AE0D030B51AEF7082CE5379 Date Created:Thu Jan 03 18:45:47 EST 2019 Interval(s):1200
				// [CFG-BACKEND-WRN] Not logged in. Redirect. Session expired: true
				// [CFG-BACKEND-WRN] No default theme or title. Redirect to login.
				HttpSession session = req.getSession();
				session.setAttribute(NodeConfiguration.SKEY_LOGGED_IN, true);
				session.setAttribute("theme", ThemeManager.getInstance().getThemeName());
				session.setAttribute("title", ThemeManager.getInstance().getThemeTitle());
				
				req.getRequestDispatcher(target).forward(req, resp);
				return;
			}
			// Write/Save text data from console modal dialog editor
			else if ( operation.equals("fuwrite")) {
				final String folder 	= ServiceConfiguration.getDefaultFileFolder();
				final String name 		= req.getParameter("f");
				final String path 		= folder + File.separator + name.replaceAll("[\\\\/]\\.\\.", "");;
				final String payload	= HTTPServerTools.getRequestBody(req);
				
				IOTools.saveText(path, payload);
				out.print(HTTPServerTools.buildBaseResponse(HttpServletResponse.SC_OK,  "Saved " + path).toString());
				return;
			}
			// Plugin upload (save @ LIB folder)
			else if ( operation.equals("pluginupload")) {
				Map<String, List<FileItem>> map = FileUpload.parseMultiValuedRequest(req, false, null);
				List<FileItem> file				= getUploadItems("file", map);
				final String fName				= file.get(0).getName();
				
				PluginSystem.savePlugin(fName, file.get(0).getInputStream());
			}
			// save identity access management (IAM)
			else if ( operation.equals("iam_save")) {
				IAM.save(req);
			}
			// delete IAM record
			else if ( operation.equals("iam_del")) {
				// delete IAM record
				final String name 	= req.getParameter("name");
				IAM.del(name);
			}
			else {
				throw new IOException("Invald operation " + operation);
			}
			
			out.print(HTTPServerTools.buildBaseResponse(HttpServletResponse.SC_OK,  operation + " Ok.").toString());
		} 
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("[SYSADMIN]", "POST", e);
			out.print(HTTPServerTools.buildBaseResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage()).toString());
		}
	}
	

	private List<FileItem> getUploadItems(final String name, Map<String, List<FileItem>> map ) {
		for ( Entry<String, List<FileItem>> entry : map.entrySet()) {
			final String key 		= entry.getKey();
			List<FileItem> items 	= entry.getValue();
			
			if ( key.equals(name)) {
				return items;
			}
		}
		return null;
	}

	/**
	 * Try to guess a content type by file name or file contents.
	 * @param filename File name, if no extension then the contents are examined.
	 * @param data file contents.
	 * @return Content type, supported: JSON, XML only.
	 */
	static String getContentType (final String filename, final String data) {
		String ct = getContentTypeFromName(filename);
		if ( ct != null) {
			return ct;
		}
		return getContentTypeFromData(data);
	}
	
	static String getContentTypeFromName (final String filename) {
		final String ext = FileTool.getFileExtension(filename);
		if ( ext == null || ext.isEmpty()) {
			return null;
		}
		if ( ext.equalsIgnoreCase("json")) {
			return CoreTypes.CONTENT_TYPE_JSON;
		}
		if ( ext.equalsIgnoreCase("xml")) {
			return CoreTypes.CONTENT_TYPE_TEXT_XML;
		}
		return CoreTypes.CONTENT_TYPE_TEXT_PLAIN;
	}
	
	/*
	 * Guess the content type from a data string (json or xml only).
	 */
	static String getContentTypeFromData (final String data) {
		try {
			JSONObject o = new JSONObject(data);
			return CoreTypes.CONTENT_TYPE_JSON;
		} catch (Exception e) {
			// not JSON, try xml
			try {
				SAXParserFactory.newInstance().newSAXParser().parse(data, new DefaultHandler());
				return CoreTypes.CONTENT_TYPE_TEXT_XML;
			} catch (Exception e2) {
				// not xml, give up
				return CoreTypes.CONTENT_TYPE_TEXT_PLAIN;
			}
		}
	}
}
