package com.cluster;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.cluster.CloudCluster;
import com.cloud.cluster.IClusterInstance;
import com.cloud.console.HTTPServerTools;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.WebClient;


/**
 * This Servlet is used to get cluster member information:
 * <h1>Hazelcast</h1>
 * <pre>
 * {"message":"OK","status":200, "response": [{"address":"/192.168.30.1:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"},"isLocal":true}] } 
 * </pre>
 * 
 * Note: all content in JSON format.
 */
@WebServlet("/Cluster")
public class ClusterManagerServlet extends HttpServlet {
	private static final long serialVersionUID 		= 1L;
    
	private static final String CONTENT_TYPE_JSON 	= "application/json";
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
			// FindBugs 11/29/16 Possible null pointer dereference of operation in com.cloud.cloud.cluster.ClusterManagerServlet.doGet(HttpServletRequest, HttpServletResponse)
			return;
		}
		try {
			
			/** FindBugs 11/29/16 Possible null pointer dereference of operation in com.cloud.cloud.cluster.ClusterManagerServlet.doGet(HttpServletRequest, HttpServletResponse)
			 * java.lang.NullPointerException
			 * 	at com.cloud.cloud.cluster.ClusterManagerServlet.doGet(ClusterManagerServlet.java:55)
			 * 	at javax.servlet.http.HttpServlet.service(HttpServlet.java:620)
			 */
			if ( operation.equals("getmembers")) {
				IClusterInstance instance 	= CloudCluster.getInstance().getClusterInstance(); 
				
				/* this shouldn't happen :(
				if (instance == null ) {
					out.print(buildResponse(200, "Cluster not yet initialized."));
					return;
				} */
				JSONArray array 			= instance != null ? instance.getMembersAsJSON() : new JSONArray();
				JSONObject root 			= buildResponse(200, "OK");

				// Add Zeroconf members
				JSONArray msgs = ZeroConfService.getInstance().getMessages();
				
				for (int i = 0; i < msgs.length(); i++) {
					array.put(msgs.getJSONObject(i));
				}
				
				root.put(KEY_RESPONSE, array);
	
				// add the leader
				String leader = instance != null ? instance.getLeader() : null;
				
				if ( leader != null) {
					root.put(KEY_LEADER, leader); //.get());
				}
				
				// {"message":"OK","status":200, "response": 
				// 	  [{"uuid": "ID", "address":"/192.168.30.1:5701","attributes":{"KEY_CTX_PATH":"/ClusterManager"},"isLocal":true}] }
				out.print(root.toString());
			}
			else if ( operation.equals("describe")) {
				// { "data": [[ROW1],[ROW2],...]} where ROW [NAME, TYPE, CONTENT, EXPIRED]
				JSONObject root = ZeroConfService.getInstance().describe();
				out.print(root.toString());
			}
			else {
				// throw new ServletException("Invalid operation: " + operation);
				// ZAP Security Scan Buffer Overflow/Format String Error - http://localhost:8080/CloudConnectorNode002/ClusterServlet?rq_operation=UhrDFBTQfXmay...
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request operation.");
			}
		} catch (JSONException e) {
			e.printStackTrace();
			out.print(buildResponse(500, e.getMessage()));
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String operation = request.getParameter("rq_operation") != null ? request.getParameter("rq_operation") : "";
		
		try {
			if ( operation.equals("savecfg")) {
				remoteSaveSeviceCfg(request, response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void remoteSaveSeviceCfg (HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException {
		/*
		 * -- START HEADERS --
		host = localhost:9080
		user-agent = Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:64.0) Gecko/20100101 Firefox/64.0
		accept = text/html...
		accept-language = en-US,en;q=0.5
		accept-encoding = gzip, deflate
		referer = http://localhost:9080/CloudClusterManager/jsp/config/config_backend.jsp?json=%7B%22store%22%3A%7B%22headers%22%3A%7B%7D%2C%22method%22%3A%22POST%22%2C%22url%22%3A%22http%3A%2F%2F192.168.56.1%3A8080%2FCloudConnectorNode002%2F%2FSysAdmin%3Frq_operation%3Dconfstore%26productType%3DCALL_CENTER%22%7D%2C%22get%22%3A%7B%22headers%22%3A%7B%7D%2C%22method%22%3A%22GET%22%2C%22url%22%3A%22http%3A%2F%2F192.168.56.1%3A8080%2FCloudConnectorNode002%2F%2FSysAdmin%3Fop%3Dconfget%26productType%3DCALL_CENTER%22%7D%7D&productType=CALL_CENTER&vendor=Avaya%20AES
		content-type = multipart/form-data; boundary=---------------------------18232709712547
		content-length = 34999
		dnt = 1
		connection = keep-alive
		cookie = JSESSIONID=40D344078E6B9ACBC174CE9CABCEFE20
		upgrade-insecure-requests = 1
		-- END HEADERS--
		-- HTTP BODY
		-----------------------------230771675119937
		Content-Disposition: form-data; name="encodedJSON"
		
		%7B%22store%22%3A%7B%22headers%22%3A%7B%7D%2C%22method%22%3A%22POST%22%2C%22url%22%3A%22http%3A%2F%2F192.168.56.1%3A8080%2FCloudConnectorNode002%2F%2FSysAdmin%3Frq_operation%3Dconfstore%26productType%3DCALL_CENTER%22%7D%2C%22get%22%3A%7B%22headers%22%3A%7B%7D%2C%22method%22%3A%22GET%22%2C%22url%22%3A%22http%3A%2F%2F192.168.56.1%3A8080%2FCloudConnectorNode002%2F%2FSysAdmin%3Fop%3Dconfget%26productType%3DCALL_CENTER%22%7D%7D
		-----------------------------230771675119937
		Content-Disposition: form-data; name="productType"
		
		CALL_CENTER		
		-----------------------------18232709712547
		Content-Disposition: form-data; name="CALL_CENTER01_00_defaultNotReadyReasonCode"
		
		0
		-----------------------------18232709712547
		Content-Disposition: form-data; name="multivals_CALL_CENTER01_09_wrapupReasonCodes"
		...
		 */
		// http://localhost:9080/CloudClusterManager/jsp/config/config_backend.jsp?json=[ENCODED]
		final String referer 		= request.getHeader("referer");
		final String[] tmp 			= referer.split("\\?");
		final String refererQS 		= tmp.length > 1 ? tmp[1] : null;

		final Map<String, String> params 	= HTTPServerTools.queryStringToMap(refererQS);
		final Map<String, String> hdrs 		= HTTPServerTools.getRequestHeaders(request);
		
		// {"store":{"headers":{},"method":"POST","url":"http://192.168.56.1:8080/CloudConnectorNode002//SysAdmin?rq_operation=confstore&productType=CALL_CENTER"}
		//	,"get":{"headers":{},"method":"GET","url":"http://192.168.56.1:8080/CloudConnectorNode002//SysAdmin?op=confget&productType=CALL_CENTER"}}
		final String json 			= URLDecoder.decode(params.get("json"), "UTF-8");
		final String contentType	= request.getContentType();
		final JSONObject root		= new JSONObject(json);
		final String url			= root.getJSONObject("store").getString("url");
		
		hdrs.remove("cookie");
		hdrs.remove("referer");
		
		// QUERY STRING: op=savecfg&action=save
		/*
		System.out.println("QUERY STRING: " + request.getQueryString());
		System.out.println("Referer=" + referer);
		System.out.println("JSON=" + json );
		System.out.println("HEADERS=" + hdrs); */
		
		String body = HTTPServerTools.getRequestBody(request);
		//System.out.println("-- POST " + url + " BODY " + body.length() + " bytes.");
		
		// URL http://192.168.56.1:8080/CloudConnectorNode002//SysAdmin?rq_operation=confstore&productType=CALL_CENTER
		WebClient wc = new WebClient(url);
		wc.setDebug(true);
		wc.setVerbosity(true);
		
		// {"message":"confstore Ok.","status":200}
		/*final String resp = */wc.doPost(body, contentType, hdrs);
		
		// GARBAGE System.out.println("-- GOT HTTP RESPONSE\n" + resp);
		
		// return to service config
		final String ctx 	= request.getServletContext().getContextPath();
		response.sendRedirect(ctx + "/jsp/config/config_backend.jsp?message=Configuration saved.");
	}
	
	private JSONObject buildResponse(int status, String message) {
		return CoreTypes.buildBaseResponse(status, message); 
	} 

}
