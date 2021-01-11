<%@page import="com.cloud.core.services.PluginSystem"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.console.jsp.JSPBackendConfig"%>
<%@page import="com.cloud.core.provider.IServiceLifeCycleV2"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.core.provider.IServiceLifeCycle"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.config.ConfigGroup"%>
<%@page import="com.cloud.core.config.ServiceConfiguration.WidgetType"%>
<%@page import="com.cloud.core.config.ServiceConfiguration"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.config.ConfigItem"%>
<%@page import="java.io.IOException"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.io.File"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Enumeration"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Properties"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>


<%
	/**
	 * Configuration Page.
	 */
	final String contextPath 		= getServletContext().getContextPath();
	final String mode				= request.getParameter("mode");		// service type: DAEMON, MESSAGE_BROKER, CALL_CENTER...
	final String id					= request.getParameter("id");		// service id
	
	String action  					= request.getParameter("action");
	String uiMessage				= request.getParameter("message");	// status messages
	String statusType				= "INFO";							// Type of status msg (INFO, ERROR)
	String theme					= (String)session.getAttribute("theme");
	String title					= (String)session.getAttribute("title");
	
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 

	/** REMOTE?
	{"get": {"method": "GET", "url": "http://localhost:8080/Node001/Confget", "headers": {}},
		"store" : {"method": "POST", "url": "http://localhost:8080/Node001/Confstore", "headers": {}}
	}*/
	final String json			= request.getParameter("json") != null ? request.getParameter("json") : (session.getAttribute("json") != null ? session.getAttribute("json").toString() : null);
	final String productType	= request.getParameter("productType") != null ? request.getParameter("productType") : session.getAttribute("productType") != null ?  session.getAttribute("productType").toString() : null;
	final String vendor			= request.getParameter("vendor") != null ? request.getParameter("vendor") : session.getAttribute("vendor") != null ? session.getAttribute("vendor").toString() : null;
	String pageTitle			= null; 
	boolean remote				= false;
	
	if ( json != null ) {
		session.setAttribute("json", json);
		session.setAttribute("productType", productType);
		session.setAttribute("vendor", vendor);
		remote = true;
	}
	

	/* This occurs if the session expires or the users tries to bypass the login. Redirect to login then back to HOME
	if (theme == null || title == null) {
		JSPBackendConfig.LOGW("No default theme or title. Redirect to login.");
		response.sendRedirect("../../login.jsp?action=loginshow&r=.&m=Session expired." );
		return;		
	} */
	
	ServiceConfiguration cfgWrapper = null;
	ServiceConfiguration[] wrappers = null;
	String formAction				= remote 
			? contextPath + "/Cluster?rq_operation=savecfg&action=save&mode=" + mode 
			: "config_backend.jsp?action=save&mode=" + mode + (id != null ? "&id=" + id : "");
	
	if ( remote ) {
		try {
			if ( productType == null )		throw new Exception("Product type (productType) is required.");
			if ( json == null )				throw new Exception("Missing service lifecycle JSON.");
			
			if ( json != null ) {
				cfgWrapper = JSPBackendConfig.remoteLoadServiceConfig(json, productType);
				//cfgWrapper.setId(productType);
			}
			// validate?
			if ( cfgWrapper.getGroups().isEmpty()) {
				throw new Exception ("No group KEYS found in backend configuration! (probably an invalid config)");		
			} 
		}
		catch (Exception e1) {
			e1.printStackTrace();
		
			uiMessage 	= HTTPServerTools.exceptionToString(e1);
			statusType	= "ERROR";
		}
		
		// backend config wrappers
		wrappers 	= new ServiceConfiguration[] { cfgWrapper }; 
		pageTitle	= "Configure " + vendor; 
	}
	else {
		// Check for a deafult connection profile.
		String cnProfile				= JSPBackendConfig.getDefaultProfile(request, cfgServer);
	
		if ( cnProfile == null) {
			JSPBackendConfig.LOGW("No default connection profile. Redirect to PM.");
			response.sendRedirect("profiles.jsp?m=Please+add+or+select+a+connection+profile.");
			return;
		}
	
		cfgServer.setConnectionProfileName(cnProfile);
	
		// Security. Buffer overflow attack?
		ServiceType type				= null;
		try {
			type = ServiceType.valueOf(mode);
		}
		catch ( Exception ex ) {
			ServletAuditor.danger(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request
					, "Potential security/buffer overlow attack for service (mode): " + request.getParameter("mode") );
			
			getServletContext().setAttribute(CloudServices.CTX_STARTUP_EXCEPTION
					, new IOException("Potential security/buffer overlow attack for service (mode): " + request.getParameter("mode"), ex));
	
			// Abort
			response.sendRedirect("../../error.jsp");
			return;
		}
		
		// This will always load the latest config from disk
		ServiceDescriptor sd		= null; //CloudServices.findServiceDescriptor(type);
		cfgWrapper					= null; //CloudServices.getServiceConfig(type);
		
		if ( id != null && (type == ServiceType.PLUGIN) ) {
			sd			= PluginSystem.findServiceDescriptor(id);
			cfgWrapper	= PluginSystem.getServiceConfig(id);
		}
		else {
			sd			= CloudServices.findServiceDescriptor(type);
			cfgWrapper	= CloudServices.getServiceConfig(type);
		}
		
		pageTitle					= sd.getVendorName();
		
		// backend config wrappers
		wrappers 					= new ServiceConfiguration[] { cfgWrapper }; 
		
		
		JSPBackendConfig.LOGD("Action:" + action + " CN profile: " + cnProfile + " Backend Type:" + type);  
		//cfgWrapper.dumpItems("INIT");
		
		// action?
		String[] temp 	= JSPBackendConfig.action(action, session, request, cfgWrapper, cfgServer, type, id);
		uiMessage		= temp[0];
		statusType		= temp[1];
		
		// Node should be done after cfg save.
		if ( uiMessage == null && CloudServices.servicesOnline()) {
			statusType 	= "WARN";
			uiMessage	= "Node should be stopped before any changes.";
		}
	
		// For service config updates (only the 1st that has been updated)
		ServiceDescriptor[] svcUpdated		= new ServiceDescriptor[1];
		int[] cfgVersions					= new int[2];
		final boolean isServiceCfgUpdated	= CloudServices.serviceUpdateDetected(svcUpdated, cfgVersions);
	
		if ( isServiceCfgUpdated) {
			String text = "Configuration update detected (New:" + cfgVersions[0] + " Previous:" + cfgVersions[1] + ". Save/Restart required)."; 
			if ( uiMessage == null ) {
				uiMessage = text;
				statusType = "WARN";
			}
			else {
				uiMessage += "&nbsp;" + text; 
			}
		}
		ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request, "Visitor entered service configuration for " + type + " Profile: " + cnProfile);
	}
	
%>    



<!DOCTYPE html>
<html lang="en">

<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<jsp:include page="<%=SkinTools.buildHeadTilePath(\"../../\") %>">
	<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
	<jsp:param value="../../" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
</jsp:include>

<script type="text/javascript" src="config.js"></script>

<script>

/* page load driver */
function win_onload() 
{
	// Do something...
}

/** 
 * Fires when the run mode combo changes 
 */
function runModeSelectOnChange() 
{
	return validateFailOver( 
			'<%=NodeConfiguration.KEY_PRIMARY%>'
			,'<%=NodeConfiguration.KEY_FAILOVER_INT%>'
			,'<%=NodeConfiguration.KEY_RUN_MODE%>'); 
	//return true;
}

/* fires when the save btn is pressed */
function config_save() 
{
	var valid = true;
	
	// validation: failover. TODO: More validations here?
	//valid = runModeSelectOnChange();

	// force selection of MV widgets. Required to sebd data to the server.
	<%
	if ( cfgWrapper != null) {
		for (int i = 0 ; i < wrappers.length ; i++) {
			ServiceConfiguration wrapper = wrappers[i];
			
			for ( ConfigItem witem : wrapper.getItems(WidgetType.DualMultiSelectEditable)) {
				out.write("uiSelectMultiValues('" + witem.key + "');");				// right side
				out.write("uiSelectMultiValues('multivals_" + witem.key + "');"); 	// left side
			}
			for ( ConfigItem witem : wrapper.getItems(WidgetType.DualMultiSelectNonEditable)) {
				out.write("uiSelectMultiValues('" + witem.key + "');"); 
				out.write("uiSelectMultiValues('multivals_" + witem.key + "');");
			}
		}
	}
	%>
	
	return valid;
}

/**
 * Fires when the validate btn is pressed.
 */
function config_validate (qryStr) 
{
	var frm 	= document.forms["frmConfig"];
	var suffix	= typeof(qryStr) != 'undefined' ? qryStr : "";
	
	allow_leave();
	
	frm.action = "?action=validate" + suffix;
	
	//LOGD("Validate URL: " + frm.action);
	frm.submit();
}

/**
 * Check for save before leaving
 */
var CAN_LEAVE = <%=session.getAttribute( request.getParameter("mode") + "saved") != null%>;

function allow_leave () {
	CAN_LEAVE = true;
}

window.onbeforeunload = function (e) {
	if ( ! CAN_LEAVE ) {
		return "You haven't saved?";
	}
}

window.onload = win_onload;

</script>

</head>

<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(\"../../\") %>">
		<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
		<jsp:param value="../../" name="commonPath"/>
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="<%=pageTitle %>" name="pageTitle"/>
		<jsp:param value="Home,Pages,Service Configuration" name="crumbLabels"/>
		<jsp:param value="../../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>

<%
	int[] startHHMM = new int[2];
	int[] stopHHMM = new int[2];
	
	// get service hrs
	cfgServer.getServiceScheduleStartHours(startHHMM);
	cfgServer.getServiceScheduleStopHours(stopHHMM);

%>

				<!--  STATUS MESSAGE -->
				<jsp:include page="<%=SkinTools.buildStatusMessagePath(\"../../\") %>">
					<jsp:param value="<%=uiMessage != null ? uiMessage : \"NULL\"%>" name="statusMsg"/>
					<jsp:param value="<%=statusType%>" name="statusType"/>
				</jsp:include>


                <!-- Action buttons -->
                <div class="row">
                	<div class="col-lg-12">
					<button onclick="document.getElementById('btn_submit').click()" title="Save" class="btn btn-sm btn-primary md-btn md-btn-primary">Save</button>
					<%if ( !remote ) { %>
					<button onclick="config_validate('&mode=<%=mode%>')" title="Please Validate before save." class="btn btn-sm btn-warning md-btn md-btn-warning">Validate</button>
					<% } %>
					&nbsp;&nbsp;&nbsp;* Means Required.
					</div>
	            </div>
				
				<p>&nbsp;</p>
				
				<!-- Page contents 	-->

				<!-- All Server Configuration information -->
				<form class="form-horizontal" role="form" id="frmConfig" method="post" enctype="multipart/form-data" 
					action="<%=formAction%>" 
					onsubmit="return config_save()" accept-charset="UTF-8">
					
					<input type="hidden" name="<%=NodeConfiguration.KEY_FAILOVER_TYPE%>" value="<%=cfgServer.getFailOverType()%>">
	
<%

ServiceConfiguration wrapper		= null;

for (int i = 0 ; i < wrappers.length ; i++) {
	wrapper = wrappers[i];
%>

 
<%
	if ( wrapper == null) {
		JSPBackendConfig.LOGE("Invalid Configuration wrapper for index " + i);
		continue; //return;
	}

	List<ConfigGroup> grpKeys = wrapper.getGroups();
	
	if ( grpKeys == null || grpKeys.isEmpty() ) {
		JSPBackendConfig.LOGE("No group KEYS found in backend configuration! (probably an invalid config)");
		wrapper.dumpItems("index.jsp");
	}
	
	for ( ConfigGroup groupKey : grpKeys) {
%>
	<!-- Backend configuration  wrapper # <%=i%> -->
		<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
			<div class="panel-heading card-header md-card-toolbar">
				<h2 class="md-card-toolbar-heading-text"><%=wrapper.getProperty(groupKey.name) %></h2>
				<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
			</div>
		
			<div class="panel-body card-body md-card-content">
			
		<%
			List<ConfigItem> citems =  wrapper.getItems(groupKey.name);
			
			//wrapper.dumpItems(groupKey.name);
			
			// Help/Tool tip...
			if ( groupKey.description != null) {
		%>
				<div class="form-group">
					<label class="col-sm-2 control-label"></label>
					<div class="col-sm-10">
						<p class="help-block"><%=groupKey.description%></p>
					</div>
				</div>
		
		<% 
			}	// END IF
			
			for ( ConfigItem citem : citems) {
				
				// check for row support (text box only ... for now)
				if ( citem.rowId != null ) {
					JSPBackendConfig.renderInSingleRow(wrapper, citem, out);
					continue;
				}
				String lblSufix = wrapper.isNew(citem.key) ? " <font color=blue>(New)</font>" : "";
				
				switch (citem.type) {
				case BooleanWithLink:
				case Boolean:
		%>
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<select	id="<%=citem.key%>" name="<%=citem.key%>" title="<%=citem.title != null ? citem.title : citem.label%>" class="form-control">
							<option value="true" <%=citem.value.equalsIgnoreCase("true")? "selected" : "" %>>True</option>
							<option value="false" <%=citem.value.equalsIgnoreCase("false")? "selected" : "" %>>False</option>
						</select>
					</div>
				</div>
				
				<% if (citem.type == WidgetType.BooleanWithLink) { %>	
				<% 	
					// the companion link property
					String linkKey = ServiceConfiguration.deriveBoolComboLinkKey(citem.key); 
					String linkVal = wrapper.getProperty(linkKey) != null ? /*cfgBackend*/wrapper.getProperty(linkKey) : "false";
					
					//LOGD("** LINK key=" + linkKey + " V:" + linkVal);
					
					if ( wrapper.getProperty(linkKey) == null )
						wrapper.put(linkKey, "false"); 
				%>	
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"></label>
					<div class="col-sm-10 uk-width-3-4">
						<label>Hyperlink?</label>
						<input name="<%=linkKey%>" <%=linkVal.equalsIgnoreCase("true") ? "checked" : "" %> type="checkbox" value="true" class="hyperlink">
					</div>
				</div>
				<%} %>
		<%
					break;
				case ComboBox:
		%>
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<select	class="form-control" id="<%=citem.key%>" name="<%=citem.key%>" title="<%=citem.title != null ? citem.title : citem.label%>">
							<%=citem.getMultiValuesAsHTML(citem.value) %>
						</select>
					</div>
				</div>
		<%
					break;
				case Hyperlink:
		%>
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4">
					</label>
					<div class="col-sm-10 uk-width-3-4">
						<a id="<%=citem.key%>" href="<%=citem.value%>" <%=citem.getOnclickAttribute()%>><%=citem.label%></a>
					</div>
				</div>
		<%
					break;
				case SingleMultiSelectEditable:
		%>
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<table>
						<tr>
							<td>
								<select	 id="<%=citem.key%>" name="<%=citem.key%>" multiple="multiple" title="<%=citem.label%>" style="width:160px;height:100px;" size="6px">
									<%=citem.getSelectedAsHTML() %>
								</select>
							</td>
							<td valign="top"><input type="button" onclick="listBoxDel('<%=citem.key%>')" value='Delete'></td>
						</tr>
						<tr>
							<td><input id="txt_<%=citem.key%>" type="text" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>"></td>
							<td><input type="button" onclick="listBoxAdd('txt_<%=citem.key%>','<%=citem.key%>')" value='Add'></td>
						</tr>
						</table>
					</div>
				</div>
				
		<%
					break;
				case DualMultiSelectNonEditable:
				case DualMultiSelectEditable:
		%>
				<div class="form-group uk-grid uk-width-1-1">
					<!-- 4/6/2020
					<label class="col-sm-2 control-label"><%=citem.label != null && !citem.label.equals(citem.key) ? citem.label : ""%><%=lblSufix%></label>
					-->
					<div class="col-sm-12 uk-width-1-1">
					<table style="margin:auto">
						<tr>
							<td colspan="3">
								 <h4><%=citem.label != null && !citem.label.equals(citem.key) ? citem.label : ""%><%=lblSufix%></h4>
							</td>
						</tr>
						<tr>
							<td align="center" style="font-weight:bold">
								Available Attributes
							</td>
							<td>
							</td>
							<td align="center" style="font-weight:bold">
								Selected Attributes
							</td>
						</tr>
						<tr>
							<td>
								<select id="multivals_<%=citem.key%>" name="multivals_<%=citem.key%>" multiple="multiple" style="width:300px;height:150px;" size="8">
									<%=citem.getMultiValuesAsHTML(null) %>
								</select>
							</td>
							<td align="center" valign="middle">
								<input type="button" onClick="move(this.form.multivals_<%=citem.key%>,this.form.<%=citem.key%>, 1)" value=">"><br />
								<input type="button" onClick="move(this.form.<%=citem.key%>,this.form.multivals_<%=citem.key%>, 0)" value="<">
							</td>
							<td>
								<select id="<%=citem.key%>" name="<%=citem.key%>" multiple="multiple" style="width:100%;height:150px;">  
									<%=citem.getSelectedAsHTML() %>  
								</select>
							</td>
						</tr>
						<tr>
							<td colspan="3">
								<button title="Move Down Available" onclick="uiMoveDownAvailableAttrib('multivals_<%=citem.key%>', '<%=citem.label != null ? citem.label : ""%>');return false;">&darr;</button>
								&nbsp;&nbsp;
								<button title="Delete Available" onclick="uiRemoveAvailableAttrib('multivals_<%=citem.key%>', '<%=citem.label != null ? citem.label : ""%>');return false;">X</button> 
								&nbsp;&nbsp;* Hold down the CTRL key to select more than one option at a time.
							</td>
						</tr>
						<% if ( citem.type == WidgetType.DualMultiSelectEditable) { %>
						<tr>
							<td colspan="3">
								<br />
								<div id="addCustFields" style="margin:auto">
									<p><label class="field" for="fieldName">Field Name</label> <input type="text" name="<%=citem.key%>_fieldName" id="<%=citem.key%>_fieldName" size="50" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>">
									<p><label class="field" for="fieldValue">Field Value</label> <input type="text" name="<%=citem.key%>_fieldValue" id="<%=citem.key%>_fieldValue" size="50" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>">
									<input type="button" id="addCust" onClick="addField(this.form.multivals_<%=citem.key%>, this.form.<%=citem.key%>_fieldName, this.form.<%=citem.key%>_fieldValue)" value="Add" title="Add Field">
								</div>
							</td>
						</tr>
						<%} %>
					</table>
					<% if ( citem.type == WidgetType.DualMultiSelectEditable) { %>
					<!-- 4/6/2020 
					<br />
					<div id="addCustFields" style="margin:auto">
						<p><label class="field" for="fieldName">Field Name</label> <input type="text" name="<%=citem.key%>_fieldName" id="<%=citem.key%>_fieldName" size="50" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>">
						<p><label class="field" for="fieldValue">Field Value</label> <input type="text" name="<%=citem.key%>_fieldValue" id="<%=citem.key%>_fieldValue" size="50" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>">
						<input type="button" id="addCust" onClick="addField(this.form.multivals_<%=citem.key%>, this.form.<%=citem.key%>_fieldName, this.form.<%=citem.key%>_fieldValue)" value="Add" title="Add Field">
					</div>
					-->
					<%} %>
					</div>
				</div>
		<%
					break;
				// Labels ...
				case CheckBox:
		%>		
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<input name="<%=citem.key%>" <%=citem.value.equalsIgnoreCase("true")? "checked" : "" %> type="checkbox" value="true" class="hyperlink">
					</div>
				</div>
		<%
					break;
				// Labels ...
				case Label:
		%>		
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%></label>
					<div class="col-sm-10 uk-width-3-4"><%=citem.value%></div>
				</div>
		<%
					break;
				// Labels ...
				case TextArea:
		%>		
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<textarea name="<%=citem.key%>" class="form-control" cols="40" rows="3" title="<%=citem.label%>" 
							maxlength="<%=JSPBackendConfig.MAX_HTTP_TXTAREA_LEN%>"><%=citem.value%></textarea>
					</div>
				</div>
		<%
					break;
				// Labels ...
				case Password:
		%>		
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=citem.required ? " *" : ""%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<input name="<%=citem.key%>" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>"
							type="password" class="form-control" autocomplete="off"
							value="<%=JSPBackendConfig.maskPassword(citem.value)%>" <%=citem.required ? "required" : ""%>>
					</div>
				</div>

		<%
					break;
				// File upload ...
				case File:
		%>		
				<div class="form-group uk-grid uk-width-1-1">
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=citem.required ? " *" : ""%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<table>
						<tr>
							<td>
								<!-- 8/16/2019 This won't display properly under the new theme
								<input name="<%=citem.key%>" type="file" class="" <%=citem.required ? "required" : ""%>>
								-->
								<div class="fileinput fileinput-new" data-provides="fileinput">
									<span class="btn btn-default btn-file">
										<span class="fileinput-new">Select file</span>
										<span class="fileinput-exists">Change</span>
										<input type="file" id="<%=citem.key%>" name="<%=citem.key%>">
									</span>
									<span class="fileinput-filename"></span>
									<a href="#" class="close fileinput-exists" data-dismiss="fileinput" style="float: none">&times;</a>
								</div>								
							</td>
							<td><%=citem.multiValues != null ? JSPBackendConfig.getFileUploadHelperHTML(citem): ""%></td>
						</tr>
						</table>
					</div>
				</div>
				
		<%			break;
		
				// Textbox default.
				default:
					String collapseHTML		= citem.collapseRow != null ? "id=\"" + citem.collapseRow + "\" style=\"display:none\"" : "";
		%>	
				<div class="form-group uk-grid uk-width-1-1" <%=collapseHTML%>>
					<label class="col-sm-2 control-label uk-width-1-4"><%=citem.label%><%=citem.required ? " *" : ""%><%=lblSufix%></label>
					<div class="col-sm-10 uk-width-3-4">
						<!-- All textboxes are required -->
						<input id="<%=citem.key%>" name="<%=citem.key%>" type="text" class="form-control md-input" maxlength="<%=JSPBackendConfig.MAX_HTTP_TXT_LEN%>"
							value="<%=IOTools.evalCommonSystemVars(citem.value)%>"
							<%=citem.getAttributeAsHTML("placeholder", citem.placeHolder)%> 
							<%=citem.getAttributeAsHTML("pattern", citem.pattern)%>
							<%=citem.getAttributeAsHTML("title", citem.title)%>
							<%=citem.required ? "required" : ""%>>
					</div>
				</div>
		<%
				}	// END SWICTH
				
				if ( citem.title != null) {
		%>
				<!-- title (help) LOOKS BAD- Too much clutter! -->
				<!-- 
				<tr>
					<td></td>
					<td colspan=1>
						<label class="field_title"><%=citem.title%></label>
					</td>
				</tr>
				 -->
		<% 
				}	// END IF
			}		// END INNER FOR
		%>
		
		<!-- panel body -->
		</div>
		
		<!-- panel -->
		</div>

	
<% } // END GROUP KEYS FOR LOOP %>

 	
<% 
} // End WRAPPERS FOR LOOP 
%>
			<br/>
			<button id="btn_submit" type="submit" class="btn btn-primary md-btn md-btn-primary" title="Save" onclick="allow_leave()">Save</button>
			
		</form>
		<!-- End main (contents) form -->

		<jsp:include page="<%=SkinTools.buildBasePath(\"../../\", \"tiles/tile_modal.jsp\")%>">
			<jsp:param value="true" name="modalFooter"/>
		</jsp:include>
		
		<jsp:include page="<%=SkinTools.buildPageEndTilePath(\"../../\") %>">
			<jsp:param value="<%=SkinTools.buildBasePath(\"../../\") %>" name="basePath"/>
			<jsp:param value="../../" name="commonPath"/>
		</jsp:include>

	</body>

</html>
