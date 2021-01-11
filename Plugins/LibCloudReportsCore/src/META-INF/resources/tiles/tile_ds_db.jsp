<%@page import="com.rts.datasource.db.DBDataSource"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.rts.datasource.IDataSource"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%
	final String ctxPath 				= getServletContext().getContextPath();
	//final String dsName					= request.getParameter("ds");		// DS name (or NULL if none)
	final String dsJSON					= request.getParameter("dsJSON");
	final JSONObject jds				= dsJSON != null && !dsJSON.isEmpty() && !dsJSON.equals("null")  ? new JSONObject(dsJSON) : null;

	final JSONObject params				= jds != null 		? jds.optJSONObject("params") : null;
	final String dsType					= jds != null 		? jds.getString("type") : "DATA";
	
	final String driver					= params != null 	? params.optString("driver") : null;
	//System.out.println("**** DS JSON:" + jds);
	
	if ( dsType.startsWith("DATA")) {
%>
		<!-- DATABASE -->
		<div class="form-group" id="dbr1" style="display: none">
			<label class="col-sm-2 control-label">Driver</label>
			<div class="col-sm-10">
				<select name="db_drv" id="db_drv" class="form-control" onchange="dbdriver_onchange(this)">
					<option value="com.microsoft.sqlserver.jdbc.SQLServerDriver" <%=(driver != null && driver.contains("sqlserver")) ? "selected" : "" %>>MS SQLServer</option>
					<option value="com.mysql.jdbc.Driver" <%=(driver != null && driver.contains("mysql")) ? "selected" : "" %>>MySQL</option>
				</select>
			</div>
		</div>
		<%-- 5/28/2019 if ( params != null && params.has("url")) { --%>
		<div class="form-group" id="dbr2" style="display: none">
			<label class="col-sm-2 control-label">Database URL</label>
			<div class="col-sm-10">
				<input id="db_url" type="text" value="<%=(params != null ? params.getString("url") : "jdbc:sqlserver://HOST:1433;databaseName=DATABASE")%>"
					class="form-control" data-toggle="tooltip"
					name="db_url"
					placeholder="jdbc:VENDOR://HOST:PORT/DATABASE"
					title="Datastore endpoint URL.">
			</div>
		</div>
		<%-- } --%>
		<div class="form-group" id="dbr3" style="display: none">
			<label class="col-sm-2 control-label">User</label>
			<div class="col-sm-10">
				<input id="db_user" type="text" value="<%=(params != null ? params.getString("user") : "")%>"
					class="form-control" data-toggle="tooltip"
					name="db_user"
					title="Datastore user name.">
			</div>
		</div>
	
		<div class="form-group" id="dbr4" style="display: none">
			<label class="col-sm-2 control-label">Password</label>
			<div class="col-sm-10">
				<input id="db_pwd" type="password" value="<%=(params != null ? params.getString("password") : "")%>"
					class="form-control" data-toggle="tooltip"
					name="db_pwd"
					title="Datastore password.">
			</div>
		</div>
		<div class="form-group" id="dbr5" style="display: none">
			<label class="col-sm-2 control-label"><a data-toggle="tooltip" href="#" 
				onclick="return loadTables()" 
				title="Click to load tables">Table</a></label>
				
			<div class="col-sm-10">
				<!-- 
				<input id="txt_table" list="db_table" name="db_table" class="form-control" data-toggle="tooltip" title="Enter or Select a DB table." onchange="setColumns(this.value)">
				<datalist id="db_table" onchange="setColumns()">
				</datalist>
				-->
				<select name="db_table" id="db_table" class="form-control col-sm-8" data-toggle="tooltip" title="Select a DB table." onchange="setColumns()">
				</select>
			</div>
		</div>
		<div class="form-group" id="dbr6" style="display: none">
			<label class="col-sm-2 control-label">Table Fields</label>
			<div class="col-sm-10">
				<input name="db_flds" id="db_flds" class="form-control" data-toggle="tooltip" maxlength="640"
					value="<%=(params != null ? params.getString("fields") : "")%>"
					placeholder="Comma separated list of field names (NO spaces or special characters)."
					pattern="[A-Za-z0-9,_]+"
					title="Comma separated list of field names (NO spaces or special characters).">
			</div>
		</div>
		<div class="form-group" id="dbr7" style="display: none">
			<label class="col-sm-2 control-label">Refresh Interval</label>
			<div class="col-sm-10">
				<select name="db_refresh" id="db_refresh" class="form-control" data-toggle="tooltip" title="Interval at which the data will be queried.">
					<option value="0" <%=(params != null && params.getString("refreshType").equals("0")) ? "selected" : "" %>>Once</option>
					<option value="20000" <%=(params != null && params.getString("refreshType").equals("20000")) ? "selected" : "" %>>20s</option>
					<option value="60000" <%=(params != null && params.getString("refreshType").equals("60000")) ? "selected" : "" %>>1m</option>
					<option value="300000" <%=(params != null && params.getString("refreshType").equals("300000")) ? "selected" : "" %>>5m</option>
					<option value="600000" <%=(params != null && params.getString("refreshType").equals("600000")) ? "selected" : "" %>>10m</option>
				</select>
			</div>
		</div>
		<!-- END DATABASE -->
	
		<script type="text/javascript">
		
		/** Fires when the 'Quety Table' link is clicked. Load DB tables */
		function loadTables () {
			$.post('<%=ctxPath%>/CoreReports?action=gettables', $('#frm1').serialize(), cbGetTablesOK).fail(cbGetTablesFail);
			return false;
		}
		
		/** 
		 * fires when the Driver combo changes. Set a default URL
		 * Informix: 	jdbc:informix-sqli://<HOST>:1533/<DATABASE>
		 * MySql: 		jdbc:mysql://<HOST>:3306/<DATABASE>
		 * MS SQL: 		jdbc:sqlserver://192.168.40.138:1433;databaseName=hcadb
		 */
		function dbdriver_onchange(cmb) {
			// set a def value
			$('#db_url').val( ( cmb.selectedIndex == 0) ? 'jdbc:sqlserver://<HOST>:1433;databaseName=<DATABASE>' : 'jdbc:mysql://<HOST>:3306/<DATABASE>');
			$('#db_url').focus();
		}

		/* DB schema cache */
		var DBSCHEMA_JSON;

		/**
		 * GetTables: Success Call back for $.post(URL, data, callback(data, status))
		 * @param data Always JSON: { status: 200 , message: 'Something', data: [{"TABLE_NAME":"vdn","REMARKS":"","TABLE_TYPE":"TABLE","TABLE_CAT":"metrics", "columns": [COL1, COL2,..]},...] }
		 */
		function cbGetTablesOK(data, status) {
			 // {"message":"OK","status":200,"data":[{"TABLE_NAME":"vdn","REMARKS":"","TABLE_TYPE":"TABLE","TABLE_CAT":"metrics"}]}
			 LOGD('Get tables callback. JSON: ' + JSON.stringify(data) + ' status:' + status);
			 
			 // App error?
			 if ( data.status != 200) {
				 notify(data.message, 'danger');
				 return;
			 }
			 // "data":[{"TABLE_NAME":"vdn","REMARKS":"","TABLE_TYPE":"TABLE","TABLE_CAT":"metrics", "columns": [COL1, COL2,..]}]
			 // COL[n] Format: {"TABLE_NAME":"vdn","CHAR_OCTET_LENGTH":64,"SQL_DATETIME_SUB":0,"REMARKS":"","BUFFER_LENGTH":65535,"NULLABLE":0,"IS_NULLABLE":"NO","TABLE_CAT":"metrics","NUM_PREC_RADIX":10,"SQL_DATA_TYPE":0,"COLUMN_SIZE":64,"TYPE_NAME":"VARCHAR","IS_AUTOINCREMENT":"NO","COLUMN_NAME":"NAME","ORDINAL_POSITION":2,"DATA_TYPE":12,"IS_GENERATEDCOLUMN":"NO"},...
			 for ( var i = 0 ; i < data.data.length ; i++) {
				 var tbl 	= data.data[i]['TABLE_NAME'];
				 $('#db_table').append('<option value="' + tbl + '">' + tbl + '</option>');
			 }
			 
			 /* set the input val
			 if ( data.data && (data.data.length > 0 ) ) {
			 	$('#txt_table').val(data.data[0]['TABLE_NAME']);
			 } */
			 DBSCHEMA_JSON = data.data; // save it for later
			 setColumns (0); 
		}

		/** GetTables: HTTP request Error call back */
		function cbGetTablesFail(jqXHR, textStatus, errorThrown) {
			notify('Request failed. See the the log view for details.', 'danger');
		}
		 
		/** 
		 * Set the TABLE columns in the fields INPUT (db_flds) from the cached table schema (DBSCHEMA_JSON)
		 * @param idx index in the schmema JSON that represents a TABLE meta data. Note: can bel null, an index number or TABLE name.
		 */
		function setColumns (idx) {
			if ( !DBSCHEMA_JSON ) {
				return;
			}
			if ( typeof(idx) == 'undefined') {
				idx = getObject('db_table').options.selectedIndex;
			}
			if ( typeof(idx) == 'string') {
				for ( var i = 0 ; i < DBSCHEMA_JSON.length ; i++) {
					if ( DBSCHEMA_JSON[i]['TABLE_NAME'] == idx) {
						idx = i;
					}
				}
			}
			if ( typeof(idx) == 'string') {
				return;
			}
			LOGD("SetColumns idx=" + idx);
			var cols 	= DBSCHEMA_JSON[idx]['columns'];
			var flds 	= '';
			var comma 	= false;
			for ( var i = 0 ; i < cols.length ; i++) {
				var col = cols[i];
				if ( comma ) flds += ',';
				flds += col['COLUMN_NAME'];
				comma = true;
			}
			$('#db_flds').val(flds);
		}
		</script>
<%
	}
%>	
