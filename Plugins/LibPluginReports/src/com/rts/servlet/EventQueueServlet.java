package com.rts.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.console.JSPLoggerTool;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.rts.core.EventQueue;
import com.rts.service.OSAlerts;
import com.rts.service.RealTimeStatsService;

/**
 * Real Time Stats Servlet. Here is some sample output:
 * <h2>Operation: pop (Pop an event from a queue)</h2>
 * <pre>
 * {"batchDate":1451759155433
 *  ,"batchData":[{"F1":"F1","VDN":"66147","ACDCALLS":"20","ABNCALLS":"6","INPROGRESS-ATAGENT":"6"
 *  	,"AVG_ACD_TALK_TIME":"16:43","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"23: 3","OLDESTCALL":":00"
 *  	,"AVG_ANSWER_SPEED":"","ACTIVECALLS":"15"},{"F1":"F1","VDN":"86724","ACDCALLS":"87","ABNCALLS":"9"
 *  	,"INPROGRESS-ATAGENT":"0","AVG_ACD_TALK_TIME":"13: 8","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"49:49"
 *  	,"OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}
 *  ]
 *  ,"listenerName":"CVDN Table"
 *  , status: 200
 *  , message: "OK"
 *  }
 * </pre>
 * <h2>Operation: getalerts</h2>
 * <pre>{
 * "message": "OK",
 * "thresholds": [{
 * "alerts": [{
 * 	"weight": 2,
 * 	"level": 1,
 * 	"color": "red"
 * 	}],
 * "metric": "SERVICELEVEL",
 * "listener": "CSPLIT Table"
 * }],
 * "status": 200
 * } </pre>
 * <h2>Operation: getdatasources</h2>
 * <pre>
 * {"message":"OK","status":200
 *  ,"dataSources":[
 *     {"port":7002,"description":"ACMETest","name":"ACMETest"}
 *     ,{"port":7001,"description":"CVDN Table Listener","name":"CVDN Table"}
 *     ,{"port":7000,"description":"CSPLIT Table Listener","name":"CSPLIT Table"}
 *     ,{"port":7003,"description":"CSPLIT 1","name":"CSPLIT T1"}
 * ]}
 * </pre>
 */
@WebServlet("/RealTimeStats")
public class EventQueueServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EventQueueServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String operation	= request.getParameter("op");		// operation
		final String name		= request.getParameter("name");		// data source/queue
		final PrintWriter pw 	= response.getWriter();
		JSONObject root 		= new JSONObject();
		
		// response type
		response.setContentType(HTTPServerTools.CONTENT_TYPE_JSON);

		// Op, listener name are required.
		if ( operation == null ) {
			root = new JSONObject();
			HTTPServerTools.injectStatus(root, 500, "Request opration (op) is required.");
			pw.print(root);
			pw.close();
			return;
		}
		/* Some ops require a queue name */
		if ( operation.equals("pop") && name == null) {
			root = new JSONObject();
			HTTPServerTools.injectStatus(root, 500, "A queue name (name) is required for operation " + operation);
			pw.print(root);
			pw.close();
			return;
		} 
		
		// check if listener name (data source) exists for ops: pop
		if ( operation.equals("pop") && !EventQueue.containsQueue(name)) {
			root = new JSONObject();
			HTTPServerTools.injectStatus(root, 500, "Missing data source " + name);
			pw.print(root);
			pw.close();
			return;
		}

		try {
			// pop last event
			if ( operation.equals("pop") ) {
				root 	= doPop(name); 
			}
			// Init dash-board
			else if ( operation.equals("init") ) {
				// optional
				String windowId = request.getParameter("windowId");
				doInit(name, windowId);
			}
			// Shutdown dash-board
			else if ( operation.equals("shutdown") ) {
				// optional
				String windowId = request.getParameter("windowId");
				doShutdown(name, windowId);
			}
			// dump event queue
			else if ( operation.equalsIgnoreCase("dump")) {
				/*root = */doDump(pw);
				pw.close();
				return; 
			}
			// Get thresholds/alerts
			else if ( operation.equalsIgnoreCase("getalerts")) {
				root = getAlerts();
			}
			else if ( operation.equalsIgnoreCase("getdashboards")) {
				root = getDashboards();
			}
			// Get all data sources
			else if ( operation.equalsIgnoreCase("getdatasources")) {
				root = getDataSources();
			}
			else {
				// Invalid op
				root = new JSONObject();
				HTTPServerTools.injectStatus(root, 500, "Invalid operation " + operation);
				pw.print(root);
				pw.close();
				return;
			}
			// inject an HTTP status
			HTTPServerTools.injectStatus(root, 200, "OK");
			root.write(pw);
		} 
		catch (Exception e) {
			HTTPServerTools.injectStatus(root, 500, e.toString());
			pw.print(root);
		}
	}

	/**
	 * Get data sources as JSON.
	 * @return {"dataSources" : [ {"name": "name1", "port", 1000, "description", "desc1"}, {"name": "name2", "port", 1001, "description", "desc2"}, ...]}
	 * @throws JSONException
	 */
	private JSONObject getDataSources() throws JSONException {
		RealTimeStatsService service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
		return service.getDataSourceManager().toJSON();
	}

	private JSONObject getAlerts() throws JSONException {
		JSONObject root 				= new JSONObject();
		RealTimeStatsService service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
		root.put("thresholds", service.getThresholdList().toJSON());
		return root;
	}
	
	/**
	 * Get dash-boards.
	 * @return JSON: <pre>
	 * { "message": "OK",
	"status": 200,
	"dashboards": [{
		"title": "Call Metrics by SPLIT",
		"displayKey": "SPLIT",
		"metrics": [{
			"description": "Calls Waiting",
			"name": "CALLS_WAITING",
			"type": "NUMBER",
			"widget": "AREA_CHART"
		}, {
			"description": "Abandoned",
			"name": "ABNCALLS",
			"type": "NUMBER",
			"widget": "AREA_CHART"
		}, ...	],
		"listener": "CSPLIT Table",
		"keyRange": "1-3"
	}, {
		"title": "Call Metrics By VDN",
		"displayKey": "VDN",
		"metrics": [{
			"description": "Calls Waiting",
			"name": "CALLS_WAITING",
			"type": "NUMBER",
			"widget": "GAUGE"
		}, ...],
		"listener": "CVDN Table",
		"keyRange": "1000,1001"
	}, ...	]} </pre>
	 * @throws JSONException
	 */
	private JSONObject getDashboards() throws JSONException {
		JSONObject root 				= new JSONObject();
		RealTimeStatsService service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
		root.put("dashboards", service.getDashboards().toJSON());
		return root;
	}

	/**
	 * Initialize a dashboard. Sent by the browser when a dash window is opened.
	 * @param dataSource Data source name (required).
	 * @param windowId Window id (optional).
	 */
	private void doInit ( final String dataSource, final String windowId) {
		JSPLoggerTool.JSP_LOGD("INIT-DASH", "Initalizing dashboard for data soucre: " + dataSource + " Window Id:" + windowId);
		
		//RealTimeStatsService service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
		RealTimeStatsService service 	= (RealTimeStatsService)PluginSystem.findInstance(RealTimeStatsService.class.getName()).getInstance();
		
		service.clientTrackerInit(dataSource, windowId);
	}

	/**
	 * Shutdown a dashboard. Sent by the browser when a dash window is closed.
	 * @param dataSource Data source name (required).
	 * @param windowId Window id (optional).
	 */
	private void doShutdown ( final String dataSource, final String windowId) {
		JSPLoggerTool.JSP_LOGD("SHUTDOWN-DASH", "Shutting down dashboard for data soucre: " + dataSource + " Window Id:" + windowId);

		//RealTimeStatsService service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
		RealTimeStatsService service 	= (RealTimeStatsService)PluginSystem.findInstance(RealTimeStatsService.class.getName()).getInstance();
		service.clientTrackerDecrement(dataSource, windowId);
	}

	/**
	 * Pop an event from a data source queue.
	 * @param dataSource Data source name (required).
	 * @return Event JSON.
	 */
	private JSONObject doPop (final String dataSource) throws Exception {
		JSONObject root = EventQueue.pop(dataSource);
		
		// no data?
		if ( root == null) {
			root = new JSONObject();
		}
		return root;
	}
	
	/**
	 * Dump queue.
	 * @return JSON: { eventQueue: [...], alerts: [{ 'message' : 'Low disk 5%', type: danger, device : DISK}, ...]}
	 * @throws Exception 
	 */
	private void doDump (Writer writer) throws Exception {
		writer.write("{ \"status\":200, \"message\":\"OK\", \"eventQueue\": ");
		EventQueue.dump(writer);
		
		// Add OS Alerts
		JSONArray alerts = OSAlerts.getAlerts();
		writer.write(", \"alerts\": ");
		alerts.write(writer);
		writer.write('}');
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/* 9/22/2017 - Moved to its own servlet under LibReportsCore
		// POST:{action=[Ljava.lang.String;@1d390f5, ds-type=[Ljava.lang.String;@d1c8d6, name=[Ljava.lang.String;@d88414, desc=[Ljava.lang.String;@c7636f, port=[Ljava.lang.String;@5133ca, fmt-hdr=[Ljava.lang.String;@11f2ea2, fmt-ftr=[Ljava.lang.String;@2e9468, fmt-fsep=[Ljava.lang.String;@117683, fmt-flds=[Ljava.lang.String;@7b9698, db_drv=[Ljava.lang.String;@1dbef0c, db_url=[Ljava.lang.String;@2b25c9, db_user=[Ljava.lang.String;@bde8a7, db_pwd=[Ljava.lang.String;@1f53cbc, db_flds=[Ljava.lang.String;@16664e0, db_refresh=[Ljava.lang.String;@168d3c5}
		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		//final String action = request.getParameter("action");
		doGetTables(request, response); */
	}

}
