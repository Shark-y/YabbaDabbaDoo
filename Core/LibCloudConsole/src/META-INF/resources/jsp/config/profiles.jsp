
<%@page import="com.cloud.core.config.FileItem"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.console.servlet.FileUpload"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.core.services.ProfileManager.ProfileDescriptor"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.ProfileManager"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>

<%!

static void LOGD(String text) {
	System.out.println("[PM-DBG] " +text);
}

static void LOGW(String text) {
	System.out.println("[PM-WRN] " +text);
}

static void LOGE(String text) {
	System.err.println("[PM-ERR] " +text);
}

static String formatMessageError(String text) {
	return "<font color=red size=4>" + text + "</font>";
}

%>

<%
	/* 10/22/2017 Unnecesary - check if logged in.
	boolean loggedIn 		= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	
	if ( ! loggedIn ) {
		LOGW("Not logged in. Redirect. Session expired: " + session.isNew());
		response.sendRedirect("../../login.jsp?action=loginshow&r=jsp/config/profiles.jsp" + (session.isNew() ? "&m=Session expired." : "") );
		return;
	} */
%>

<%
	NodeConfiguration cfg	= CloudServices.getNodeConfig();
	ProfileManager pm 		= cfg.getProfileManager();
	final String action 	= request.getParameter("action");
	final String name 		= request.getParameter("n");		// profile name
	String statusMsg		= request.getParameter("m");		// status message
	String defaultName		= cfg.getConnectionProfileName();	// default profile

	final String theme		= (String)session.getAttribute("theme");
	final String title		= (String)session.getAttribute("title");

	final String ctxPath	= getServletContext().getContextPath();
	
	// always reload in case other nodes add/remove profiles!
	pm.reload();
	
	LOGD("Action: " + action + " P Name:" + name);

	
	if ( action != null ) {
		try {
			if (action.equalsIgnoreCase("del")) {
				Auditor.danger(AuditSource.CLOUD_CONSOLE, AuditVerb.SERVICE_LIFECYCLE, "Deleted connection profile " + name);  
				pm.delete(name);
			}
			else if (action.equalsIgnoreCase("add")) {
				Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.SERVICE_LIFECYCLE, "Created connection profile " + name);  
				pm.add(name);
			}
			else if (action.equalsIgnoreCase("duplicate")) {
				String src = request.getParameter("src");
				String dst = request.getParameter("dst");
				
				LOGD("Duplicatre src=" + src + " Dst=" + dst);
				pm.copy(src, dst);
			}
			else if (action.equalsIgnoreCase("activate")) {
				LOGD("Activating profile " + name);
				
				Auditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.SERVICE_LIFECYCLE, "Activated connection profile " + name);  
				response.sendRedirect("config_node.jsp?n=" + name);
				return;
			}
			/*
			else if (action.equals("importProf")) {
				LOGD("Parts:" + request.getParts().size());
				List<FileItem> files 	= FileUpload.parseRequest(request, false, null);
				FileItem zip			= files.get(0);
			} */
		}
		catch ( Exception ex) {
			statusMsg = formatMessageError(ex.getMessage());
		}
	}
	else {
		pm.dump("PROFILE MGR");
	}
%>


<!DOCTYPE html>
<html lang="en">

<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
</jsp:include>

<script>
function del (name)
{
	var r = confirm("Delete " + name + "?");
	
	if ( name == '<%=defaultName%>') {
		alert("Can't delete active profile.");
		return;
	}
	
	if (r == true) {
		location = 'profiles.jsp?action=del&n=' + name;
	}
}

function add ()
{
	var name = prompt("Please enter a name");
	
	if (name == null || name.trim() == '') {
		return false;	
	}
	
	location = 'profiles.jsp?action=add&n=' + name;
	return true;
}

function edit (name)
{
	var r = confirm("Activate " + name + "?");
	
	if (r == true) {
		//window.location = 'config_node.jsp?n=' + name;
		window.location = 'profiles.jsp?action=activate&n=' + name;
	}
}

function duplicate (name)
{
	var name1 = prompt("Please enter a name", "Copy of " + name);

	if (name1 == null || name1.trim() == '') {
		return false;	
	}

	location = 'profiles.jsp?action=duplicate&src=' + name + '&dst=' + name1;
	return true;
}

function import_submit() {
	var file = $('#name').val();
	LOGD('Import file: ' + file);
	if ( file == '') {
		notify('Click Browse to select a file first.', 'warning');
		return false;
	}
	return true;	// submit
}

</script>

</head>

<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="Connection Profiles" name="pageTitle"/>
		<jsp:param value="Home,Pages,Connection Profiles" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>
	
	<div class="container-fluid">

		<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
			<jsp:param value="<%=statusMsg != null ? statusMsg : \"NULL\"%>" name="statusMsg"/>
			<jsp:param value="INFO" name="statusType"/>
		</jsp:include>
				
		<!-- /.row -->
		<div class="row">
			<form class="form-horizontal" action="<%=ctxPath%>/SysAdmin?rq_operation=importProf" 
				method="post" enctype="multipart/form-data" accept-charset="UTF-8" onsubmit="return import_submit()">
				<div class="form-group">
					<label class="col-sm-3 control-label">Import Profile</label>
					<div class="col-sm-6">
						<!-- 
						<input id="name" name="name" type="file" title="Click to select a file."> -->
						<div class="fileinput fileinput-new" data-provides="fileinput">
							<span class="btn btn-default btn-file">
								<span class="fileinput-new">Select file</span>
								<span class="fileinput-exists">Change</span>
								<input type="file" id="name" name="name">
							</span>
							<span class="fileinput-filename"></span>
							<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a>
						</div>						
					</div>
					<div class="col-sm-2">
						<input type="submit" class="btn btn-success" value="Import">
					</div>
				</div>
			</form>
		</div>
		
        <div class="row">
			<div class="col-lg-12">
 					
				<table class="table table-bordered table-hover table-striped">
					<tr>
						<th>Name</th><th>Location</th><th>Action</th>
					</tr>
				<% for ( ProfileDescriptor pd : pm.getProfiles()) { %>
					<tr>
						<td><a href="javascript:edit('<%=pd.name%>')"><%=pd.name%></a> &nbsp;
						<% if ( defaultName.equals(pd.name)) out.println("(Active)"); %>
						</td>
						
						<td>
						<%=pd.path%>	
						</td>
						
						<td>
							<button onclick="del('<%=pd.name%>')">X</button>
							&nbsp;&nbsp;&nbsp;
							<a href="#" onclick="duplicate('<%=pd.name%>')">Duplicate</a> 
							&nbsp;&nbsp;&nbsp;
							<a href="<%=ctxPath%>/SysAdmin?op=exportProf&name=<%=pd.name%>">Export</a>
						</td>
					</tr>
				<% } %>
				
				</table>
                        
        	</div>
        </div>
		<!-- /.row -->

		<div class="row">
			<div class="col-lg-12">
				<button onclick="return add()" class="btn btn-primary">Add</button>
			</div>
		</div>

		<!-- put some space @ the bottom --> 
		<div class="row" style="height: 200px;">
			&nbsp;
		</div>
		<!-- /.row -->


    </div>
    <!-- /.container-fluid -->

	<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
	</jsp:include>


</body>

</html>
