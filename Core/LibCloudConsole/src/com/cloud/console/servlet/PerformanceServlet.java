package com.cloud.console.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.console.JSPLoggerTool;
import com.cloud.console.performance.LiveThreadCollector;
import com.cloud.console.performance.OSCommands;
import com.cloud.core.net.MulticastTunnel;
import com.cloud.core.profiler.OSMetrics;
import com.cloud.core.profiler.VMAttach;

/**
 * A Servlet to get performance metrics from the OS. Invoke as:
 * 
 * <b>http://host:port/CntextRoot//OSPerformance</b>
 * 
 * The output is in JSON format:
 * 
 * <pre> { "status": 200,
 *  "SystemCpuLoad": "0.04581372190268807",
 *  "daemonThreadCount": 5,
 *  "peakThreadCount": 5,
 *  "heapFree": 117411840,
 *  "heapMax": 117411840,
 *  "heapTotal": 117411840,
 *  "ProcessCpuLoad": "0.0026520683563266423",
 *  } </pre>
 *  
 * @author vsilva
 * @version 1.0.4 - 2/15/2019 Multicast tunnel initial implementation.
 * @version 1.0.3 - 2/12/2017 NetStat information.
 * @version 1.0.2 - 1/24/2017 Thread information + stack traces.
 * @version 1.0.0 - 1/1/2016
 *
 */
@WebServlet( name = "OSPerformanceServlet", urlPatterns = {"/OSPerformance"})
public class PerformanceServlet extends HttpServlet {

	private static final long serialVersionUID 		= 5202518713548847655L;

	// Keys used by servlets in this pkg
	static final String CONTENT_TYPE_JSON 			= "application/json";
	static final String KEY_STATUS 					= "status";
	static final String KEY_MESSAGE 				= "message";

	private transient MulticastTunnel tunnel;
	
	public PerformanceServlet() {
		try {
			//MulticastTunnel.setDebug(true);
			tunnel = new MulticastTunnel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void destroy() {
		try {
			if ( tunnel != null) {
				tunnel.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		// Enable unrestricted XDomain access. This is so UI clients can invoke this API via JQuery.
		resp.addHeader("Access-Control-Allow-Origin", "*");
		
		// MUST SET. ALL response operations in json format.
		resp.setContentType(CONTENT_TYPE_JSON);
		
		// Tell the client not to cache requests
		resp.addHeader("Cache-Control","no-cache,no-store");

		String op = req.getParameter("op") != null ? req.getParameter("op") : "os";
		
		if ( op.equals("os") ) {
			HTTPServerTools.getOSMetrics(req, resp);
		}
		else if ( op.equals("tr")) {
			getThreadInfo(req, resp);
		}
		else if ( op.equals("trst")) {
			getThreadStackTraces(req, resp);
		}
		else if ( op.equals("net")) {
			getNetStat(req, resp);
		}
		else if ( op.equals("mct-status")) {
			getMCastTunnelStatus (req, resp);
		}
		// 2/3/20320
		else if ( op.equals("vms")) {
			getVirtualMachines(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		// Always JSON
		resp.setContentType(CONTENT_TYPE_JSON);

		// Tell the client not to cache requests
		resp.addHeader("Cache-Control","no-cache,no-store");

		String op = req.getParameter("op") != null ? req.getParameter("op") : "reset";
		
		if ( op.equals("reset") ) {
			LiveThreadCollector.resetStalledThreadTracker();
		}
		else if ( op.equals("interrupt")) {
			int tid = Integer.parseInt(req.getParameter("tid"));
			LiveThreadCollector.processThread(tid, true, false);
		}
		else if ( op.equals("stop")) {
			int tid = Integer.parseInt(req.getParameter("tid"));
			LiveThreadCollector.processThread(tid, true, true);
		}
		else if ( op.equals("tunnel")) {
			tunnelAction(req, resp);
		}
		// Return an empty JSON response to avoid
		// XML Parsing Error: no root element found Location: http://localhost:8080/CloudConnectorNode002/OSPerformance?op=reset Line Number 1, Column 1:
		JSONObject root = new JSONObject();
		HTTPServerTools.injectStatus(root, 200, "OK");
		resp.getWriter().print(root.toString());
	}
	
	/**
	 * Return thread information in the default Data Tables format { "data": [[row1],[row2],...] }
	 * <p>This info is meant to be displayed by an HTML Data Table using AJAX.</p>
	 * 
	 * @param req {@link HttpServletRequest}
	 * @param resp {@link HttpServletResponse}
	 * @throws IOException If there is an error sendinf the HTTP response.
	 * @since 1.0.1
	 */
	private void getThreadInfo (HttpServletRequest req, HttpServletResponse resp)
		throws IOException 
	{
		try {
			// 2/4/2020 Check for a VM pid
			final String vmpId 	= req.getParameter("vmpid") != null ? req.getParameter("vmpid") : "-1";	
			JSONObject root 	= new JSONObject();

			root.put("data", Integer.parseInt(vmpId) <= 0 ? OSMetrics.getThreadInfo() : VMAttach.getThreadInfo(vmpId));

			// write response.
			resp.getWriter().print(root.toString());
		} 
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("[OSPerformance]", "Get thread information.", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
	}

	/**
	 * Prints thread stack information Data Tables JSON:
	 * <pre>{"data":[["CloudExecutorService-3",48,5,"WAITING",false,true,false,"TACK"],...]}</pre>
	 * <p>Invoke this from Data Tables.</p>
	 * @param req {@link HttpServletRequest}
	 * @param resp {@link HttpServletResponse}
	 * @throws IOException
	 * @since 1.0.2
	 */
	private void getThreadStackTraces (HttpServletRequest req, HttpServletResponse resp)
		throws IOException 
	{
		try {
			JSONObject root = new JSONObject();
			root.put("data", LiveThreadCollector.getAllThreadStackTraces());

			// write response.
			resp.getWriter().print(root.toString());
		} 
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("[OSPerformance]", "Get live threads stack traces.", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
	}

	/**
	 * Write netstat OS command information to the {@link HttpServletResponse} in the default Data Tables format { "data": [[row1],[row2],...] }
	 * <p>This info is meant to be displayed by an HTML Data Table using AJAX.</p>
	 * 
	 * @param req {@link HttpServletRequest}
	 * @param resp {@link HttpServletResponse}
	 * @throws IOException If there is any kind of error.
	 * @since 1.0.3
	 */
	private void getNetStat (HttpServletRequest req, HttpServletResponse resp)
		throws IOException 
	{
		try {
			JSONObject root = new JSONObject();

			// Linux (if no netstat installed): java.io.IOException: Cannot run program &quot;netstat&quot;: error=2, No such file or directory
			root.put("data", OSCommands.netstat());

			// write response.
			resp.getWriter().print(root.toString());
		} 
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("[NetStat]", "Get NetStat information.", e);
			// Must return valid DT JSON: resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			resp.getWriter().print("{\"data\": []}");
		}
	}

	/**
	 * Sends: {"message":"OK","status":200,"tunnel":{"bytesSent":0,"packetsSent":0,"packetsReceived":0,"bytesReceived":0,"running":false}}
	 * @param req {@link HttpServletRequest}
	 * @param resp {@link HttpServletResponse}
	 * @throws IOException If there is any kind of error.
	 */
	private void getMCastTunnelStatus (HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			JSONObject root = new JSONObject();
			HTTPServerTools.injectStatus(root, 200, "OK");
			
			if ( tunnel != null) {
				root.put("tunnel", tunnel.toJSON());
			}
			// write response.
			resp.getWriter().print(root.toString());
		} 
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("[McastTunnel]", "Get MulticastTunnel information.", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
	}
	
	/*
	 * Fires when a tunnel action is selected from the Cloud profiler (START/STOP) 
	 */
	private void tunnelAction (HttpServletRequest req, HttpServletResponse resp) {
		// {"action":"START","serverport":"9876","remotehost":"foo","remoteport":"9876"}
		final String action = req.getParameter("action"); 	// START/STOP
		
		if ( action == null || tunnel == null) {
			return;
		}
		try {
			if (action.equalsIgnoreCase("START")) {
				final int serverport 	= Integer.parseInt(req.getParameter("serverport"));
				final int remoteport 	= Integer.parseInt(req.getParameter("remoteport"));
				final String remotehost = req.getParameter("remotehost");
				
				tunnel.start(serverport, remotehost, remoteport);
			}
			else {
				tunnel.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return thread information in the default Data Tables format { "data": [[row1],[row2],...] }
	 * <p>This info is meant to be displayed by an HTML Data Table using AJAX.</p>
	 * 
	 * @param req {@link HttpServletRequest}
	 * @param resp {@link HttpServletResponse}
	 * @throws IOException If there is an error sendinf the HTTP response.
	 * @since 1.0.1
	 */
	private void getVirtualMachines (HttpServletRequest req, HttpServletResponse resp)
		throws IOException 
	{
		try {
			JSONObject root = new JSONObject();
			
			root.put("data", VMAttach.getRemoteVMs());

			// write response.
			resp.getWriter().print(root.toString());
		} 
		catch (Exception e) {
			JSPLoggerTool.JSP_LOGE("[OSPerformance]", "Get virtual machines.", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
	}

}
