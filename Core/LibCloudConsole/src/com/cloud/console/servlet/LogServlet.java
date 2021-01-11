package com.cloud.console.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;
import com.cloud.core.io.ZipTool;
import com.cloud.core.logging.Container;
import com.cloud.core.logging.CyclicBufferAppender;
import com.cloud.core.logging.L4JAudit;
import com.cloud.core.types.CoreTypes;

/**
 * Servlet to extract LOG4J events from a {@link CyclicBufferAppender}.
 * This servlet gets invoked via AJAX by the cloud console log viewer. Events are displayed in an HTML TABLE. 
 * <h2>JSON Event Encoding</h2> <pre>{
 *  "data": [
 *  ["2016-02-04 10:11:56:101", "CloudServices", "DEBUG", "IsNodeCongigured: Checking if a sys admin pwd has been setup."],
 *  ["2016-02-04 10:11:56:102", "CloudServices", "DEBUG", "IsNodeCongigured: sys admin pwd has been setup. Checking for service descriptors @ C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default"],
 *  ["2016-02-04 10:11:56:103", "CloudServices", "DEBUG", "IsNodeCongigured: Found descriptor C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default\\avaya-cfg.xml"],
 *  ["2016-02-04 10:11:56:104", "CloudServices", "DEBUG", "IsNodeCongigured: Node has been configured."],
 *  ["2016-02-04 10:11:56:105", "NodeConfiguration", "WARN", "Add URL: localhost is NOT a valid URL host name!"],
 *  ...
 * ]
 * } </pre>
 * 
 * @author VSilva
 * 
 * @version 1.0.0 - Initial implementation.
 * @version 1.0.1 - 9/13/2017 Added support for Tomcat 8 log display (catalina*, tomcat8-stderr*) plus an simple keyword filter for the message.
 */
@WebServlet("/LogServlet")
public class LogServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public LogServlet() {
        super();
    }

	/**
	 * Log Servlet Get Operations:
	 * <ul>
	 * <li> Get log buffer as JSON (no params)
	 * <li> Zip server logs (op=ziplogs)
	 * </ul>
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String op 	= request.getParameter("op");
		
		// default, get the current log buffer as JSON
		if ( op == null) {
			CyclicBufferAppender cba 	= (CyclicBufferAppender)Logger.getRootLogger().getAppender(CyclicBufferAppender.NAME);
			JSONObject root 			= cba.toJSON();
			
			// 1/11/2019 Remote log view: Cross-Origin Request Blocked: The Same Origin Policy disallows reading the remote resource at .../LogServlet. (Reason: CORS header ‘Access-Control-Allow-Origin’ missing)
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType(CoreTypes.CONTENT_TYPE_JSON); // 5/4/2017 For UTF-8 (kanji & stuff) "application/json");
			response.getWriter().print(root.toString());
		}
		// Get the audit buffer as JSON
		else if ( op.equals("audit")) {
			response.setContentType(CoreTypes.CONTENT_TYPE_JSON); // 5/4/2017 For UTF-8 (kanji & stuff) "application/json");
			response.getWriter().print(L4JAudit.toJSON().toString());
		}
		// Zip & Download the container log folder. Fires when the Download link is clicked from the console log view.
		else if ( op.equals("ziplogs")) {
			// catalina.base=C:\Temp\Workspaces\CloudServices\Cloud-UnifiedContactCenter\.metadata\.plugins\org.eclipse.wst.server.core\tmp2
			// This will only work in tomcat
			final SimpleDateFormat fmt 	= new SimpleDateFormat("MM-dd-yyyy");
			final String path 			= Container.TOMCAT_HOME_PATH + File.separator + "logs";
			final String filename 		= "NodeLogs" + getServletContext().getContextPath().substring(1) + "_" + fmt.format(new Date()) + ".zip";
			
			// open the browser save as dlg
			// Fix for: Issue CLOUD_CORE-19: CONSOLE Node Log, download link crash in TC 7.0.32 
			//response.setHeader("Content-Type", "application/zip, application/octet-stream");
			response.setHeader("Content-Type", "application/octet-stream");
			response.setHeader("Content-Disposition", "attachement;filename=\"" + filename + "\"");
			ZipTool.zipFolder(path, response.getOutputStream());
		}
		// View log file as JSON. Only works in tomcat
		else if ( op.equals("view")) {
			final String name 	= request.getParameter("file");
			final String filter	= request.getParameter("filter");	// Optional MSG keyword filter (if null fetch ALL)
			
			if ( name == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			// FindBugs 11/29/16 Relative path traversal in com.cloud.console.servlet.LogServlet.doGet(HttpServletRequest, HttpServletResponse)
			// The software uses an HTTP request parameter to construct a pathname that should be within a restricted directory, 
			// but it does not properly neutralize sequences such as ".." that can resolve to a location that is outside of that directory. See <a href="http://cwe.mitre.org/data/definitions/23.html">http://cwe.mitre.org/data/definitions/23.html for more information. 
			// http://cwe.mitre.org/data/definitions/23.html
			// ORIG -- final String path 	= HTTPServerTools.TOMCAT_BASE_PATH + File.separator + "logs" + File.separator + name; 
			final String path 	= Container.TOMCAT_HOME_PATH /*BASE*/ + File.separator + "logs" + File.separator + name.replaceAll("[\\\\/]\\.\\.", "");
			
			try {
				JSONObject root = filterLogFile(path, filter);
				
				response.setContentType(CoreTypes.CONTENT_TYPE_JSON); 
				response.getWriter().print(root.toString());
			} catch (JSONException e) {
				throw new IOException("Log view failure for " + path, e);
			}
		}
		// 2/8/17: download a file from java.io.tmpdir only
		else if ( op.equals("download")) {
			// File name
			String fileName = request.getParameter("f");
			String filePath	= null;
			
			// must prepend java.oi.tmpdir.
			if ( fileName != null) {
				// FindBugs relative path traversal on fileName -  http://cwe.mitre.org/data/definitions/23.html
				filePath 			= CoreTypes.TMP_DIR + File.separator + fileName.replaceAll("\\.\\.", "");
				File f 				= new File(filePath);
				FileInputStream fos = null;
				
				try {
					fos = new FileInputStream(f);
					
					response.setHeader("Content-Type", "application/octet-stream");
					
					// This code directly writes an HTTP parameter to an HTTP header, which allows for a HTTP response splitting vulnerability. 
					// See http://en.wikipedia.org/wiki/HTTP_response_splitting for more information
					// Prevention: URL Encoding - https://en.wikipedia.org/wiki/Percent-encoding
					response.setHeader("Content-Disposition", "attachement;filename=\"" + URLEncoder.encode(fileName, CoreTypes.DEFAULT_ENCODING) + "\"");
					response.setHeader("Content-Length", String.valueOf(f.length()));
					
					IOTools.pipeStream(fos, response.getOutputStream());
				}
				finally {
					IOTools.closeStream(fos);
				}
			}
		}
		else {
			// ZAP Security scan: Format String error - http://localhost:8080/CloudConnectorNode002/LogServlet?op=clear&len=ZAP%25n%25s%25n%25s%25n%25s%
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request operation.");
		}
	}

	/**
	 * POST Operations:
	 * <ul>
	 * <li>op=clear&len=N - Clear N items from the log buffer.
	 * </ul>
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//String op 	= request.getParameter("op");
		int len		= 0;
		// ZAP security validation
		try {
			len 	= Integer.parseInt(request.getParameter("len"));
		} 
		catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request operation length paramater.");
		}
		
		CyclicBufferAppender cba = (CyclicBufferAppender)Logger.getRootLogger().getAppender(CyclicBufferAppender.NAME);
		
		if ( len > 0 ) {
			cba.clear(len);
		}
		else {
			cba.clear();
		}
	}

	/**
	 * Filter (extract) data from a log file given a string filter (search key)
	 * @param path Full path to the log file.
	 * @param filter Search key. For example: Exception - to get errors.
	 * @return JSON object for logging events in data tables format: {data: [[DATE, SOURCE< LEVEL, MSG]...]}
	 * @throws IOException on I/O Errors.
	 * @throws JSONException on JSON parse errors.
	 */
	public static JSONObject filterLogFile (final String path, final String filter) throws IOException, JSONException {
		return Container.filterLogFile (path, filter);
	}
	
	
}
