<%@page import="org.json.JSONException"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%!

static void LOGD(final String text) {
	System.out.println("[DT] " + text);	
}

static String menusToHTML (JSONArray menus) throws JSONException {
	StringBuffer html = new StringBuffer();
	for ( int i = 0 ; i < menus.length() ; i++) {
		JSONObject menu = menus.getJSONObject(i);
		String lbl 		= menu.getString("label");
		
		if ( lbl.equals("DIVIDER")) {
			html.append("<li class=\"divider uk-nav-divider\"></li>");
		}
		else if (menu.has("href")) {
			// <li><a target="_blank" href="https://kubernetes.io/docs/concepts/storage/persistent-volumes/">About Kubernetes Volumes</a></li>
			html.append(String.format("<li><a target=\"_%s\" href=\"%s\">%s</a></li>"
				, menu.getString("target")
				, menu.getString("href")
				, menu.getString("label")));
		}
		else if (menu.has("onclick")) {
			// <li><a href="#" onclick="return addVolume('../../', node)">Add Volume</a></li>
			html.append(String.format("<li><a onclick=\"%s\" href=\"#\">%s</a></li>"
				, menu.getString("onclick")
				, menu.getString("label")));
		}
		html.append("\n");
	}
	return html.toString();
}
%>

<%
	final String desc 	= request.getParameter("descriptor");

	//LOGD("Descriptor: " + desc);

	JSONObject json		= new JSONObject(desc);
	JSONObject hdr		= json.getJSONObject("header");
	JSONArray lbls		= hdr.getJSONArray("labels");
	JSONArray tb		= json.getJSONArray("panelToolbar");
	
	final String tblid 		= json.getString("id");
	final boolean panel		= json.getBoolean("usePanel");
	final boolean colVis	= json.optBoolean("colVis");
	
%>
				<% if (panel) { %>
					<div class="panel panel-default md-card" data-widget='{"draggable": "false"}'>
				<% } %>
						<div class="panel-controls dropdown" style="float: right;">
						<% for (int i = 0 ; i < tb.length() ; i++) { 
								JSONObject widget 	= tb.getJSONObject(i);
								String type 		= widget.getString("widget");
								
								if (type.equals("icon-button")) {
						%>
							<button class="<%=SkinTools.cssPanelToolbarBtnClass()%>" title="<%=widget.getString("title")%>" onclick="<%=widget.getString("onclick")%>"><span class="material-icons inverted"><%=widget.get("icon")%></span></button>
							
							<% 	} else if (type.equals("refresh-button")) {
									String onclick = widget.has("onclick") ? widget.getString("onclick") : "javascript:" + tblid + ".ajax.reload();";
							%>
							
							<button id="btn_refresh_<%=tblid%>" class="<%=SkinTools.cssPanelToolbarBtnClass()%> refresh-panel" onclick="<%=onclick%>"><span class="material-icons inverted">refresh</span></button>
							
							<% 	} else if (type.equals("dropdown")) {
									JSONArray menus = widget.getJSONArray("menus");
							%>
		                    <div class="uk-button-dropdown" data-uk-dropdown="">
		                    	<button class="<%=SkinTools.cssPanelToolbarBtnClass()%> dropdown-toggle" data-toggle="dropdown"><span class="material-icons inverted">more_vert</span></button>
								<div class="uk-dropdown">
		                    		<ul class="dropdown-menu uk-nav uk-nav-dropdown" role="menu">
		                    			<%=menusToHTML(menus) %>
		                    		</ul>
		                    	</div>
		                    </div>
							<%  } 
							} 
						%>
		                </div>	
		                
		            <% if (panel) { %>					
						<div class="panel-body md-card-content">
							<br/>
					<% } %>
							<% if (colVis) { %>
							<div>
								Visibility: 
								<% for (int i = 0 ; i < lbls.length() ; i++) { %>
								<a class="toggle-vis md-btn md-btn-small md-btn-flat md-btn-flat-primary" data-column="<%=i %>" data-table="<%=tblid%>"><%=lbls.getString(i) %></a>
								<% } %>
							</div>
							<% } %>
							
							<table id="<%=tblid%>" class="table m-n" style="width: 100%">
								<thead>
									<tr>
										<% for (int i = 0 ; i < lbls.length() ; i++) { %>
										<th><%=lbls.getString(i) %></th>
										<% } %>
									</tr>
								</thead>
								<tbody>
								</tbody>
							</table>
								
					<% if (panel) { %>
						</div>
					</div>
					<% } %>

<script>

var <%=tblid%>;

function init_<%=tblid%> (config) {
	<%=tblid%> = $('#<%=tblid%>').DataTable(config);
}

</script>					
							