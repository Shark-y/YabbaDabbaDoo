package com.rts.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.db.JDBCClient;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;

/**
 * Servlet used to perform cloud reports core operations
 * <ul>
 * <li>Get a list of tables from a DB
 * <li>Others
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - 9/22/2017 Initail implementation.
 *
 */
@WebServlet("/CoreReports")
public class ServletReports extends HttpServlet {

	private static final Logger log 			= LogManager.getLogger(ServletReports.class);
	private static final long serialVersionUID 	= -8144657569797480385L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)	throws ServletException, IOException {
		// POST:{action=[Ljava.lang.String;@1d390f5, ds-type=[Ljava.lang.String;@d1c8d6, name=[Ljava.lang.String;@d88414, desc=[Ljava.lang.String;@c7636f, port=[Ljava.lang.String;@5133ca, fmt-hdr=[Ljava.lang.String;@11f2ea2, fmt-ftr=[Ljava.lang.String;@2e9468, fmt-fsep=[Ljava.lang.String;@117683, fmt-flds=[Ljava.lang.String;@7b9698, db_drv=[Ljava.lang.String;@1dbef0c, db_url=[Ljava.lang.String;@2b25c9, db_user=[Ljava.lang.String;@bde8a7, db_pwd=[Ljava.lang.String;@1f53cbc, db_flds=[Ljava.lang.String;@16664e0, db_refresh=[Ljava.lang.String;@168d3c5}
		response.setContentType(CoreTypes.CONTENT_TYPE_JSON);
		//final String action = request.getParameter("action");
		doGetTables(request, response);
	}
	
	/**
	 * Get the tables and fields for a database: <pre>
	 * { status: 200 , message: 'Something', data: [{"TABLE_NAME":"vdn","REMARKS":"","TABLE_TYPE":"TABLE","TABLE_CAT":"metrics", "columns": [COL1, COL2,..]},...] }
	 * COL[n] Format: {"TABLE_NAME":"vdn","CHAR_OCTET_LENGTH":64,"SQL_DATETIME_SUB":0,"REMARKS":"","BUFFER_LENGTH":65535,"NULLABLE":0,"IS_NULLABLE":"NO","TABLE_CAT":"metrics","NUM_PREC_RADIX":10,"SQL_DATA_TYPE":0,"COLUMN_SIZE":64,"TYPE_NAME":"VARCHAR","IS_AUTOINCREMENT":"NO","COLUMN_NAME":"NAME","ORDINAL_POSITION":2,"DATA_TYPE":12,"IS_GENERATEDCOLUMN":"NO"},...
	 * </pre>
	 * @param request HTTP request.
	 * @param response HTTP response.
	 * @throws IOException on HTTP error.
	 */
	private void doGetTables (HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			final String driver = Validators.requireNotNullAndNotEmpty(request.getParameter("db_drv") ,"DB driver is required.");
			final String url 	= Validators.requireNotNullAndNotEmpty(request.getParameter("db_url") ,"DB URL is required.");
			final String usr 	= Validators.requireNotNullAndNotEmpty(request.getParameter("db_user") ,"DB user is required.");
			final String pwd 	= Validators.requireNotNullAndNotEmpty(request.getParameter("db_pwd") ,"DB password is required.");
			
			//System.out.println("GET TBLS: Url:" + url + " drv:" + driver + " Usr:" + usr + " pwd:" + pwd );
			JDBCClient db = new JDBCClient(driver);
			db.connect(url, usr, pwd);

			// Try to get the catalog (table) from the server
			final String dbCatalog	= db.getMetaData().getCatalog();

			// if DB catalog is null, get it from URL. This only works in MySQL URls. MS SQL fails (jdbc:sqlserver://192.168.43.125;databaseName=DB)
			final int pos			= url.lastIndexOf("/");
			final String catalog 	= dbCatalog != null ? dbCatalog : pos > 0 ? url.substring(pos + 1, url.length()) : url;
			
			// [{"TABLE_NAME":"vdn","REMARKS":"","TABLE_TYPE":"TABLE","TABLE_CAT":"metrics"},...]
			JSONArray tbls 			= db.getMetaData().getTables(catalog, null, null, null);	// All DB tables
			JSONArray filtered 		= new JSONArray();

			// FIXME Schema names to be ignored: MSSQL: sys,INFORMATION_SCHEMA. This should be configurable somehow... 
//			IServiceLifeCycleV2 service = (IServiceLifeCycleV2)CloudServices.findService(ServiceType.DAEMON);
//			service.getConfigProperty(arg0)
			final String[] ignoreNames = new String[] {"sys", "INFORMATION_SCHEMA"};

			// Fort each table, inject cols
			for (int i = 0; i < tbls.length(); i++) {
				JSONObject row 		= tbls.getJSONObject(i);
				final String tbl 	= row.getString("TABLE_NAME");
				final String schem  = row.optString("TABLE_SCHEM");

				// Ignore schemas. Or this will take forever (~1m) in MSSQL
				if ( schem != null ) {
					boolean skip = false;
					for (int j = 0; j < ignoreNames.length; j++) {
						if (schem.equalsIgnoreCase(ignoreNames[j])) {
							skip = true;
							break;
						}
					}
					if ( skip) {
						continue;
					}
				}
				JSONArray cols 		= db.getMetaData().getColumns(catalog, null, tbl, null);
				row.put("columns", cols);
				filtered.put(row);
			}
		
			db.disconnect();
			
			JSONObject root = HTTPServerTools.buildBaseResponse(200, "OK");
			root.put("data", filtered); //tbls);
		
			response.getWriter().print(root.toString());
		} catch (Exception e) {
			log.error("[GetTables] Intenal Server Error", e);
			response.getWriter().print(HTTPServerTools.buildBaseResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
		}
		
	}
	
}
