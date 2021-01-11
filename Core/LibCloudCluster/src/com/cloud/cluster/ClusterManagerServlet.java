package com.cloud.cluster;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.zeroconf.ZeroClusterInstance;


/**
 * This Servlet is used to get cluster member information:
 * <h1>Hazelcast</h1>
 * <pre>
 * {"message":"OK","status":200, "response": [{"address":"/192.168.30.1:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"},"isLocal":true}] } 
 * </pre>
 * 
 * Note: all content in JSON format.
 */
@WebServlet("/ClusterServlet")
public class ClusterManagerServlet extends HttpServlet {
	private static final long serialVersionUID 		= 1L;
    
	private static final String CONTENT_TYPE_JSON 	= "application/json";
	private static final String KEY_STATUS 			= "status";
	private static final String KEY_MESSAGE 		= "message";
	private static final String KEY_RESPONSE 		= "response";
	private static final String KEY_LEADER 			= "clusterLeader";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClusterManagerServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String operation = request.getParameter("rq_operation");

		// results in JSON
		response.setContentType(CONTENT_TYPE_JSON);
		
		PrintWriter out = response.getWriter();
		
		if ( operation == null) {
			// ZAP Security scan throw new ServletException("A request operation is required.");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A request operation is required.");
			// FindBugs 11/29/16 Possible null pointer dereference of operation in com.cloud.cluster.ClusterManagerServlet.doGet(HttpServletRequest, HttpServletResponse)
			return;
		}
		try {
			
			/** FindBugs 11/29/16 Possible null pointer dereference of operation in com.cloud.cluster.ClusterManagerServlet.doGet(HttpServletRequest, HttpServletResponse)
			 * java.lang.NullPointerException
			 * 	at com.cloud.cluster.ClusterManagerServlet.doGet(ClusterManagerServlet.java:55)
			 * 	at javax.servlet.http.HttpServlet.service(HttpServlet.java:620)
			 */
			IClusterInstance instance 	= CloudCluster.getInstance().getClusterInstance(); 

			if ( operation.equals("getmembers")) {
				
				// this shouldn't happen :(
				if (instance == null ) {
					out.print(buildResponse(200, "Cluster not yet initialized."));
					return;
				}
				JSONArray array 			= instance.getMembersAsJSON();
				JSONObject root 			= buildResponse(200, "OK");
				
				root.put(KEY_RESPONSE, array);
	
				// add the leader
				/*IAtomicReference<String>*/ String leader = instance.getLeader();
				
				if ( leader != null) {
					root.put(KEY_LEADER, leader); //.get());
				}
				
				// {"message":"OK","status":200, "response": 
				// 	  [{"address":"/192.168.30.1:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"},"isLocal":true}] }
				out.print(root.toString());
			}
			else if ( operation.equals("describe")) {
				//IClusterInstance instance 	= CloudCluster.getInstance().getClusterInstance();
				
				// this shouldn't happen :(
				if (instance == null ) {
					out.print("{\"data\": []}");
					return;
				}
				// { "data": [[ROW1],[ROW2],...]} where ROW [NAME, TYPE, EXPIRED, CONTENT]
				out.print(instance.describe().toString());
			}
			else if ( operation.equals("queue")) {
				// Get the Zeroconf Multicast message queue
				// this shouldn't happen :(
				if (instance == null ) {
					out.print("{\"data\": []}");
					return;
				}
				out.print( ((ZeroClusterInstance)instance).getMessageQueue());
			}
			else {
				// throw new ServletException("Invalid operation: " + operation);
				// ZAP Security Scan Buffer Overflow/Format String Error - http://localhost:8080/CloudConnectorNode002/ClusterServlet?rq_operation=UhrDFBTQfXmay...
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request operation.");
			}
		} catch (Exception e) {
			out.print(buildResponse(500, e.getMessage()));
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		throw new ServletException("doPost is not supported.");
	}

	void dumpMembers() {
		List<ClusterMember> members = CloudCluster.getInstance().getClusterInstance().getMembers();
		
		for (ClusterMember clusterMember : members) {
			System.out.println(clusterMember);
		}
	}
	
	private JSONObject buildResponse(int status, String message) {
		JSONObject root = new JSONObject();
		try {
			root.put(KEY_STATUS, status);
			root.put(KEY_MESSAGE, message);
		} catch (JSONException e) {
		}
		return root;
	}

}
