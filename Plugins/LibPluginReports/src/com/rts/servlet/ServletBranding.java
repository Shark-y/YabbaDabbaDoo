package com.rts.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloud.core.services.CloudServices;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.PluginSystem.Plugin;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.rts.service.RealTimeStatsService;
import com.rts.service.ServiceUtils;
import com.rts.ui.Dashboard;

/**
 * Used to perform branding operations like 
 * <ul>
 * <li>Fetching logos from the file system & writing them to the HTTP response.
 * </ul>
 */
@WebServlet("/Branding")
@MultipartConfig()
public class ServletBranding extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServletBranding() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String operation	= request.getParameter("op");
		
		// Branding: get logo for a given dash
		if ( operation.equalsIgnoreCase("logo")) {
			handleBrandingFetchLogo(request, response);
			return;
		}
		
	}

	/**
	 * Handle HTTP request /Branding?op=logo&dash=NAME. It writes the logo to the response as an image.
	 * @param request
	 * @param response
	 * @throws ServletException
	 */
	private void handleBrandingFetchLogo(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		final String name 					= request.getParameter("dash");
		final String id 					= request.getParameter("id");	// Plugin support
		final String mode 					= request.getParameter("mode");
		
		if ( name == null)				throw new ServletException("Dashboard name is required.");

		RealTimeStatsService service = null;
		try {
			if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
				ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
				Plugin p 				= PluginSystem.findInstance(sd.getClassName());
				service 				= (RealTimeStatsService )p.getInstance();
			}
			else {
				service = (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
			}
			Dashboard dash 					= service.getDashboards().find(name);
			
			ServiceUtils.brandingFetchLogo(dash, response);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//ServiceUtils.dumpHTTPServletRequestParams("POST", request);
		String statusMsg 	= null;
		String statusType	= null;
		try {
			ServiceUtils.brandingHandleLogoUpload(request);
		} catch (Exception e) {
			//e.printStackTrace();
			statusMsg 	= e.getMessage();
			statusType	= "ERROR";
		}
		// back to dashboards
		response.sendRedirect("dash/dash.jsp" + (statusMsg != null ? "?action=status&sm=" + statusMsg + "&st=" + statusType : "") );
	}

}
