package com.rts.jsp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.PluginSystem;
import com.cloud.core.services.PluginSystem.Plugin;
import com.cloud.core.services.ServiceDescriptor;
import com.cloud.core.services.ServiceDescriptor.ServiceType;
import com.cloud.core.services.ServiceStatus;
import com.rts.core.IDataService;
import com.rts.datasource.IDataSource;
import com.rts.datasource.db.DBDataSink;
import com.rts.datasource.db.DBDataSource;
import com.rts.datasource.fs.FileSystemDataSource;
import com.rts.datasource.media.BaseMapDataSource;

/**
 * Diagnostics Servlet invoked by the System > Diagnostics JSP (conf/disg.jsp) of the Cloud Storage and other micro-services.
 * 
 * <h2>HTTP Operations</h2>
 * <ul>
 * <li> Dump the contents of the data source:  GET /Diagnostics?op=dump&name=(data source name).
 * <li> POST : No-op.
 * </ul>
 * 
 * <h2>Response Format (JSON)</h2>
 * <ul>
 * <li> Dump Contents: { "status":200, "message":"OK", "eventQueue": [{"listenerName":"VDN","batchDate":1451759155433,"batchData":[{K1:V1,..},{K1:V1,..},..]}
 * </ul>
 * @version 1.0.0 - 10/01/2017 Initial implementation.
 * @version 1.0.1 - 11/26/2017 Made this code reusable for for all services (conf/disg.jsp).
 */
@WebServlet("/Diagnostics")
public class DiagnosticsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final Logger log = LogManager.getLogger(DiagnosticsServlet.class);
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DiagnosticsServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String operation	= request.getParameter("op");		// operation
		final String name		= request.getParameter("name");		// data source/queue
		final String mode		= request.getParameter("mode");		// PLUGIN, DAEMON, (service type)
		final String id			= request.getParameter("id");		// PLUGIN id
		final PrintWriter pw 	= response.getWriter();
		JSONObject root 		= new JSONObject();
		
		// response type
		response.setContentType(HTTPServerTools.CONTENT_TYPE_JSON);

		try {
			// 6/5/2020 Plugin support
			IDataService service = null;
			if ( mode!= null && mode.equalsIgnoreCase("plugin")) {
				ServiceDescriptor sd	= PluginSystem.findServiceDescriptor(id);
				Plugin p 				= PluginSystem.findInstance(sd.getClassName());
				service 				= (IDataService)p.getInstance();
			}
			else {
				service 	= (IDataService)CloudServices.findService(ServiceType.DAEMON);
			}
			
			if ( operation.equalsIgnoreCase("dump") ) {
				doDump(pw, name, service);
			}
			HTTPServerTools.injectStatus(root, 200 , "OK.");
		} catch (Exception e) {
			log.error("[Diagnostics]", e);
			HTTPServerTools.injectStatus(root, 500, e.toString());
			pw.print(root);
		}
		pw.close();
	}

	/**
	 * Dump the contents of a {@link IDataSource} into the HTTP response print writer.
	 * @param pw HTTP servlet print writer where the contents are dumped.
	 * @param name Data store name.
	 * @param service The {@link IDataService}.
	 * @throws IOException on print writer errors.
	 * @throws SQLException on data store errors: Bad connection, missing tables, any SQL error.
	 * @throws JSONException on JSON doc parse errors.
	 */
	private void doDump (Writer pw, String name, IDataService service ) throws IOException, SQLException, JSONException {
		pw.write("{ \"status\":200, \"message\":\"OK\", \"eventQueue\": [");
		
		if ( name != null) {
			dumpDS(pw, service.getDataSource(name)) ; 
		}
		else {
			List<IDataSource> dataSources 	= service.getDataSourceManager().getDataSources();
			boolean comma 					= false;
			
			for (IDataSource ds : dataSources) {
				if ( ds.getStatus().getStatus() != ServiceStatus.Status.ON_LINE) {
					continue;
				}
				// if there is an SQL error w/ the DS this will give JSON parse errors...
				if (comma ) pw.write(',');
				dumpDS(pw, ds);
				comma 	= true;
			}
		}
		pw.write("]}");
	}
	
	/**
	 * Dump a single {@link DBDataSource} into the HTTP response print writer.
	 * 
	 * <p><i>Format: {"batchDate":1451762800814,"batchData":[{ROW1}, {ROW2}, ...],"listenerName":"CVDN Table"}</i>
	 * 
	 * @param writer HTTP response print writer.
	 * @param ds The data source.
	 * @throws IOException on print writer errors.
	 * @throws JSONException on JSON doc parse errors.
	 */
	private void dumpDS (Writer writer, IDataSource ds ) throws IOException, JSONException {
		if ( ds == null ) {
			return;
		}
		String name			= ds.getName();

		writer.write("{ \"listenerName\":\"" + name + "\", \"batchDate\" : " + System.currentTimeMillis() + ", \"batchData\": [");
		
		// [["1000","Eastern Sales",38,":00","",75,"48: 1",77,"52:22",56], ...]
		JSONArray rows 		= null;
		
		// this cannot fail or else the dump will get all messed up.
		try {
			// Get batches by data source type
			if ( ds instanceof DBDataSink) {
				// JSON Array of JSON Array [[ROW1], [ROW2],...]
				DBDataSink sink = (DBDataSink)ds;
				rows 			= sink.get(sink.getTable());
			}
			else if (ds instanceof BaseMapDataSource) {
				// JSON Array of JSON Object [{ROW1}, {ROW2},...]
				rows = ((BaseMapDataSource)ds).getBatches();
			}
			// 6/12/2019
			else if (ds instanceof FileSystemDataSource) {
				// JSON Array of JSON Object [{ROW1}, {ROW2},...]
				rows = ((FileSystemDataSource)ds).getBatches();
			}
			else {
				rows = new JSONArray();
			}
		} catch (Exception e) {
			log.error("[Diagnostics] " + ds.toString() + ": " + e);
			rows = new JSONArray();
		}
		
		// Used by JSOnArray of JSONArray only to assemble as below
		List<String> fields	= ds.getFormat() != null ? ds.getFormat().getDataFields() : null;
		
		/* This is eventQueue format required by the ds.jsp page.
	    [
	  	 {"batchDate":1451759155433,"batchData":[{"F1":"F1","VDN":"66147","ACDCALLS":"20","ABNCALLS":"6","INPROGRESS-ATAGENT":"6","AVG_ACD_TALK_TIME":"16:43","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"23: 3","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"15"},{"F1":"F1","VDN":"86724","ACDCALLS":"87","ABNCALLS":"9","INPROGRESS-ATAGENT":"0","AVG_ACD_TALK_TIME":"13: 8","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"49:49","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
	  	,{"batchDate":1451762800814,"batchData":[{"F1":"F1","VDN":"46699","ACDCALLS":"1","ABNCALLS":"4","INPROGRESS-ATAGENT":"9","AVG_ACD_TALK_TIME":"45:16","VDN SYN":"Eastern Sales","AVG_ABANDON_TIME":"21:21","OLDESTCALL":":00","AVG_ANSWER_SPEED":"","ACTIVECALLS":"64"}],"listenerName":"CVDN Table"}
	  	,{"batchDate":1451762800814,"batchData":[],"listenerName":"CVDN Table"}
		] */
//		boolean comma = false;
		
		for (int i = 0; i < rows.length(); i++) {
			Object obj = rows.get(i);

			// ["1000","Eastern Sales",38,":00","",75,"48: 1",77,"52:22",56]
			if (obj instanceof JSONArray) {
				JSONArray row = rows.getJSONArray(i); 
				
//				if ( comma) {
//					writer.write(',');
//				}
				writer.write("{");
				boolean comma1 = false;
				
				for (int j = 0; j < row.length(); j++) {
					if (comma1 ) writer.write(',');
					
					// Escape JSON - Row values that contain JSON will give JS errors in the browser
					writer.write("\"" + fields.get(j) + "\" : \"" + escapeValue(row, j) + "\"");
					comma1 = true;
				}
				writer.write("}");
//				comma = true;
			}
			else {
				// {"F1":"F1","VDN":"66147","ACDCALLS":"20","ABNCALLS":"6",...
				JSONObject row = (JSONObject)obj;
				row.write(writer);
			}
			if ( i + 1 < rows.length()) {
				writer.write(',');
			}
		}
		writer.write("]}");
	}
	
	/**
	 * Escape double quotes (") in the value of a {@link JSONArray} [index]
	 * @param row a {@link JSONArray} representing a row within a batch.
	 * @param idx The index of the row element.
	 * @return Escaped double quotes within value.
	 * @throws JSONException on JSON parsing errors.
	 */
	private String escapeValue (JSONArray row , int idx) throws JSONException {
		try {
			String payload = row.getString(idx);
			// escape json
			if ( payload.startsWith("{")) {
				payload = payload.replaceAll("\"", "\\\\\"");
			}
			return payload;
		} catch (Exception e) {
			return  row.opt(idx) != null ? row.get(idx).toString() : "" ;
		}
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

}
