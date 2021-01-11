<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="com.cloud.core.profiler.OSMetrics"%>
<%@page import="org.json.JSONObject"%>
<%@page import="com.cloud.core.logging.Container"%>
<%@page import="com.cloud.console.HTMLConsoleLogUtil"%>
<%@page import="com.cloud.console.CloudConsole"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.DateFormat"%>
<%@page import="com.cloud.core.license.License.LicenseDescriptor"%>
<%@page import="com.cloud.core.io.FileTool"%>
<%@page import="java.io.File"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@page import="com.cloud.core.services.CloudFailOverService.FailOverType"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="java.util.List"%>
<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.core.license.License"%>
<%@page import="javax.print.attribute.standard.Severity"%>
<%@page import="com.cloud.core.provider.IServiceLifeCycle"%>
<%@page import="com.cloud.core.services.ServiceDescriptor.ServiceType"%>
<%@page import="com.cloud.core.services.ServiceDescriptor"%>
<%@page import="com.cloud.core.services.ServiceHoursScheduler"%>
<%@page import="com.cloud.core.services.NodeConfiguration.RunMode"%>
<%@page import="com.cloud.core.services.ProfileManager"%>
<%@page import="com.cloud.core.services.ServiceStatus"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="java.text.SimpleDateFormat"%>

<%!

static final String TAG 		= "[CLOUD-CONSOLE]";

static void LOGD(String text) {
	System.out.println(TAG + " " + text);
}

static void LOGE(String text) {
	System.err.println(TAG + " " +  text);
}

static String formatError(String text) {
	return "<font color=red>" + text + "</font>";
}

static String formatWarn(String text) {
	return "<font color=yellow>" + text + "</font>";
}

static String formatDbg(String text) {
	return "<font color=blue>" + text + "</font>";
}

static String formatOk(String text) {
	return "<font color=green>" + text + "</font>";
}

/**
 * Loop thru the FS and get the first service descriptor which is missing a configuration.
 * This helps the console user redirect to the service which has not been configured.
 * @param config Node configuration.
 * @return First service which is missing a config or NULL if all services are configured.
 */
static ServiceDescriptor getMissingConfigService ( NodeConfiguration config) {
	List<ServiceDescriptor> descriptors = config.getServiceDescriptors();
	String basePath 					= config.getDefaultProfileBasePath();

	if ( basePath == null || basePath.isEmpty()) {
		LOGD("ServiceConfigured: Profile base path is null or empty. Node MUST be configured.");
		return null;
	}
	
	LOGD("ServiceConfigured: Checking for service descriptors @ " + basePath);

	for ( ServiceDescriptor descriptor : descriptors) {
		String filePath = basePath + File.separator + descriptor.getConfigFileName();

		if ( !FileTool.fileExists(filePath)) {
			LOGD("ServiceConfigured: " + filePath + " NOT found." + descriptor.getVendorName() + " NOT configured.");
			return descriptor;
		}
	}
	//LOGD("ServiceConfigured: Node has been configured.");
	return null;
}

/**
 * Get the remaining days in a {@link com.cloud.cloud.core.license.License.LicenseDescriptor}.
 */
static int getRemainingDays (LicenseDescriptor ld ) throws Exception {
	DateFormat dateFormat	= new SimpleDateFormat("MM/dd/yyyy");
	Date today 				= new Date();
	Date expiryDate 		= dateFormat.parse(ld.expirationDate);
	return (int) ((expiryDate.getTime() - today.getTime())/(1000 * 60 * 60 * 24));
}

%>

<%
	final String contextPath 	= getServletContext().getContextPath();
	final ThemeManager tm 		= ThemeManager.getInstance();
	final String theme			= tm.getThemeName() != null 	? tm.getThemeName() 	: "bootstrap-blue";
	final String title			= tm.getThemeTitle() != null 	? tm.getThemeTitle() 	: "Cloud Services";
	final String pageTitle		= contextPath.substring(1).replace("Cloud", "");
	final SimpleDateFormat df	= new SimpleDateFormat("hh:mm");
	
	String uiMessage			= request.getParameter("m");	// status messages
	String statusType			= "INFO";						// Type of status msg (INFO, WARN, ERROR)

	final JSONObject os 		= OSMetrics.getOSMetrics().getJSONObject(OSMetrics.KEY_OS);

	LOGD("Theme: " + theme + " Skin:" + SkinTools.SKIN_PATH);
	LOGD("Title: " + title);
	
	// Global Page settings...
	if ( session.getAttribute("theme") == null) {
		session.setAttribute("theme", theme );
	}
	if ( session.getAttribute("title") == null) {
		session.setAttribute("title", title);
	}
	

	String lastError 					= CloudServices.getLastError();		// Server status message
	NodeConfiguration cfg				= CloudServices.getNodeConfig(); 	// Cloud Services cfg
	String action						= request.getParameter("raction");

	//LOGD("Node Config: " + cfg);

	// invalid config?
	if ( cfg == null ) {
		response.sendRedirect("error.jsp?statusMsg=Invalid+node+configuration.");
		return;
	}
	
	// fatal startup error?
	if ( getServletContext().getAttribute(CloudServices.CTX_STARTUP_EXCEPTION) != null ) {
		response.sendRedirect("error.jsp");
		return;
	}
	
	final boolean loggedIn 				= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	final boolean inDevMode				= false; //12/19/2018 Devmode backddor removed CloudServices.isInDeveloperMode(); 
	final String instanceId				= License.getInstanceId();	
	final boolean isConfigured 			= CloudServices.isConfigured();
	final ServiceDescriptor descriptor	= !isConfigured ? getMissingConfigService(cfg) : null;
	
	ProfileManager profileMgr			= cfg.getProfileManager();
	final String profileName			= cfg.getConnectionProfileName();
	
	// For service config updates (only the 1st that has been updated)
	ServiceDescriptor[] svcUpdated		= new ServiceDescriptor[1];
	int[] cfgVersions					= new int[2];
	final boolean isServiceCfgUpdated	= CloudServices.serviceUpdateDetected(svcUpdated, cfgVersions);

	// Container STDERR log file
	final String stdErrLog				= Container.getContainerStdErrLog();

	int[] startHHMM = new int[2];
	int[] stopHHMM = new int[2];
	
	// get service hrs
	cfg.getServiceScheduleStartHours(startHHMM);
	cfg.getServiceScheduleStopHours(stopHHMM);

	// Add the URL of this node to the config. If the URL has localhost it will be replaced by the IP addr.
	cfg.addNodeURL(request.getRequestURL().toString(), true);
	
	// add to the cluster config too.
	//CloudCluster.getInstance().setLocalMemberAttribute(NodeConfiguration.KEY_CTX_URL, conf.getProperty(NodeConfiguration.KEY_CTX_URL));
	
	// Server cfg is required by some pages.
	if ( session.getAttribute(NodeConfiguration.SKEY_CFG_SERVER) == null) {
		LOGD("Storing the server cfg in session.");
		session.setAttribute(NodeConfiguration.SKEY_CFG_SERVER, cfg); //conf);	
	}
	
	if (action != null ) {
		LOGD("Action:" + action);
		
		if ( ! loggedIn ) {
			// vsilva 11/26/2016 DEPRECATED - always back home. response.sendRedirect("login.jsp?action=loginshow&r=index.jsp&" + request.getQueryString());
			response.sendRedirect("login.jsp?action=loginshow");
			return;
		}
		else {
			if ( action.equalsIgnoreCase("start")) {
				
				LOGD("Starting services...");
				ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.SERVICE_LIFECYCLE, request, "Administrator is attempting to START the service node.");
				
				// notify listeners
				CloudConsole.getInstance().consoleStarted();
				CloudServices.startServices();
				
				// refresh if error.
				lastError = CloudServices.getLastError();
			}
			else if ( action.equalsIgnoreCase("stop")) {
				LOGD("Stopping services...");
				
				ServletAuditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.SERVICE_LIFECYCLE, request, "Administrator is attempting to STOP the service node.");
				
				CloudConsole.getInstance().consoleStoped();
				CloudServices.stopServices();
			}
		}
	}

	// check the remaining days in the license (if available)
	if ( cfg.requiresLicense() && isConfigured ) {
		LicenseDescriptor license 	= CloudServices.getLicenseDescriptor(true);
		
		if ( license != null ) {
			int daysLeft 				= getRemainingDays(license) + 1;
			
			if ( daysLeft < 20 ) {
				uiMessage = "Your license " + (daysLeft <= 0 ? "has expired. " : "expires in " + daysLeft + " day(s). ")  
					//+ "<a href=\"mailto:register@convergeone.com&Subject=License%20Request&Body=Product: Cloud Connector%0D%0AInstance%20Id: " + instanceId 
					//+ "%0D%0AMAC: " + License.getSystemMACAddress() + "%0D%0AHost: " + request.getLocalName() + "\">Request a license</a>"
					;
				statusType = "WARN";
			}
		}
	}
	
	final String licensed 	= inDevMode 
			? formatDbg("License Disabled (In developer mode).")
			: CloudServices.isLicensed() 
				? "Yes. " + CloudServices.getLicenseDescriptor(false).numOfSeats 
						+ " seats. Expires: " + CloudServices.getLicenseDescriptor(false).expirationDate
				: License.getLastError() != null 
					? "<font color=red>License Error: " + License.getLastError() + "</font>" 
					: "No.";

						
%>

<!DOCTYPE html>
<html lang="en">

<head>

<jsp:include page="<%=SkinTools.TILE_PATH_HEAD%>" flush="true">
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="<%=SkinTools.SKIN_PATH%>" name="basePath"/>
</jsp:include>

<script type="text/javascript" src="js/poll.js"></script>

<script type="text/javascript">
	function doStart() {
		var obj = document.getElementById("aStart");
		if ( obj ) obj.style.display = 'none';
		window.location = 'index.jsp?raction=start';
	}
</script>

<!-- Charts -->
<script type='text/javascript' src='js/Chart.js'></script>
<script type='text/javascript' src='jsp/perf/osp.js'></script>

<!--  gauges -->
<script type='text/javascript' src="js/plugins/morris/raphael.min.js"></script>
<script type='text/javascript' src='js/justgage.js'></script>
<script type='text/javascript' src='js/cookie.js'></script>

<script type='text/javascript'>

	var memChart;	// Memory
	
	// CPU load
	var cpuAreaChart;

	// Threads (daemon, peak)
	var threadChart;
	
	// CPU Gauge
	var cpuAvgGauge;
	
	// Container metrics
	var containerChart;

	// Container Global Requets processor
	var requestChart;

	function getRand(val) {
		return Math.ceil((val * Math.random()));
	}

	function initCharts() {
		memChart 		= new Chart(document.getElementById('memchart').getContext("2d"), configMem);
		cpuAreaChart 	= new Chart(document.getElementById('cpuchart').getContext("2d"), configCpu);
		threadChart 	= new Chart(document.getElementById('tchart').getContext("2d"), configTr);
		cpuAvgGauge		= new JustGage({ id: "cpuAvgGauge", value: 0, min: 0, max: 100, title: "AVG % CPU" }); //, label: '<%=os.getString("AvailableProcessors")%> Processor(s)' });
		containerChart	= null; // Lazy init - new Chart(document.getElementById('containerChart').getContext("2d"), configContainer);
		requestChart	= new Chart(document.getElementById('requestChart').getContext("2d"), configCGRP);
	}

	function toMB(bytes) {
		return Math.ceil(bytes / (1024 * 1024));
	}

</script>

</head>

<body class="sidebar_main_open sidebar_main_swipe">

	<jsp:include page="<%=SkinTools.TILE_PATH_PAGE_START%>">
		<jsp:param value="<%=title%>" name="title"/>
		<jsp:param value="<%=SkinTools.SKIN_PATH%>" name="basePath"/>
	</jsp:include>
	
	<!--  STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.TILE_PATH_STATUS_MSG %>">
		<jsp:param value="<%=uiMessage != null ? uiMessage : \"NULL\"%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>
	&nbsp;
	
	<div class="row uk-grid uk-grid-width-large-1-4 uk-grid-width-medium-1-2 uk-grid-medium uk-sortable sortable-handler">
		<div>
        <div class="col-lg-3 col-md-6 col-sm-6 col-xs-12 md-card">
            <div class="info-tile info-tile-alt tile-indigo md-card-content">
                <div class="info uk-float-right uk-margin-top uk-margin-small-right" style="width: 50%">
                    <div class="tile-heading"><span>Processor(s)</span></div>
                    <div class="tile-body"><span><%=os.getString("AvailableProcessors")%></span></div>
                </div>
                <div class="stats">
                    <div class="tile-content">
 						<div id="cpuAvgGauge" style="width:100px; height: 64px;margin: 1px;"></div>
                    	<!-- <div id="sparkline-cpu" data-percent="0"></div> -->
                    </div>
                </div>
            </div>
        </div>
        </div>
        <div>
        <div class="col-lg-3 col-md-6 col-sm-6 col-xs-12 md-card">
            <div class="info-tile info-tile-alt tile-indigo md-card-content">
                <div class="info uk-float-right uk-margin-top uk-margin-small-right" style="width: 50%">
                    <div class="tile-heading"><span>Free RAM (MB)</span></div>
                    <div class="tile-body "><span id="tile-freemem">0</span></div>
                </div>
                <div class="stats">
                    <div class="tile-content"><div id="sparkline-heap"></div></div>
                </div>
            </div>
        </div>
        </div>
        <div>
        <div class="col-lg-3 col-md-6 col-sm-6 col-xs-12 md-card">
            <div class="info-tile info-tile-alt tile-primary md-card-content">
                <div class="info uk-float-right uk-margin-top uk-margin-small-right" style="width: 50%">
                    <div class="tile-heading"><span>Live Threads</span></div>
                    <div class="tile-body "><span id="tile-threads">0</span></div>
                </div>
                <div class="stats">
                    <div class="tile-content"><div id="sparkline-threads"></div></div>
                </div>
            </div>
        </div>
        </div>
        <div>
        <div class="col-lg-3 col-md-6 col-sm-6 col-xs-12 md-card">
            <div class="info-tile info-tile-alt tile-primary md-card-content">
                <div class="info uk-float-right uk-margin-top uk-margin-small-right" style="width: 50%;">
                    <div class="tile-heading"><span>Disk(s)</span></div>
                    <div class="tile-body "><span id="diskCount">0</span></div>
                </div>
                <div class="stats">
                    <div class="tile-content"><div id="sparkline-disk" style="height: 64px"></div></div>
                </div>
            </div>
        </div>
        </div>
    </div>
    <!-- ROW1:  -->
    
	 	
	<!-- OS TAB -->
	<div class="row uk-grid" data-uk-grid-margin data-uk-grid-match="{target:'.md-card-content'}">
		<div class="col-md-6 uk-width-medium-1-2">
	
	<% if ( !SkinTools.isMaterialTheme()) { %> 	
    <div id="mainPanel" class="panel panel-default card md-card">
        <div id="mainPanelHeader" class="panel-heading card-header md-card-toolbar">
            <h2 class="panel-title md-card-toolbar-heading-text" <% if ( theme.contains("booterial") ) { out.println("style='color:white'"); }%>><%=pageTitle%> <span id="nodeDetails"></span></h2> 
        </div>
        <div class="panel-body card-body md-card-content">
 	<% } %>

	<div class="form-horizontal uk-grid"> 
	
		<div class="<%=SkinTools.cssFormGroupClass() %>">
			<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Container</label>
			<div class="<%=SkinTools.cssFormGroupContentClass() %>"><%=System.getProperty("os.name")%> / <%=application.getServerInfo()%> </div>
		</div>

		<div class="<%=SkinTools.cssFormGroupClass() %>">
			<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Java</label>
			<div class="<%=SkinTools.cssFormGroupContentClass()%>"><%=System.getProperty("java.home")%></div>
		</div>
		
		<% if ( cfg.requiresLicense() ) { %>
		<div class="form-group uk-form-row">
			<label class="col-sm-4 control-label">Instance / MAC Address</label>
			<div class="col-sm-8"><%=instanceId%> / <%=License.getSystemMACAddress()%>
			</div>
		</div>
		<%} %>
		
		<div class="<%=SkinTools.cssFormGroupClass() %>">
			<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Node Status</label>
			<div class="<%=SkinTools.cssFormGroupContentClass()%>">
			<%
				if ( !isConfigured) {
					final String html = descriptor != null && CloudServices.isNodeConfigured()
							? "<a href=\"jsp/config/config_backend.jsp?mode=" + descriptor.getType() + "\">" + descriptor.getVendorName() + "</a>"  
							: "<a href=\"jsp/config/config_node.jsp\">Node</a>";
					out.println("<font color=gray>" + html + " must be configured.</font>");
				}
				else if ( lastError != null) {
					// skip license errors ( already shown under Licensed...)
					if ( License.getLastError() != null) {
						out.println("<a href=\"jsp/config/config_node.jsp\">License error.</a>");
					}
					else {
						out.println("<font color=red>" + lastError + "</font><br/><a id='aStart' href='javascript:doStart()'>Start</a>");
					}
				}
				else if ( CloudServices.isOutsideServiceHours() ) {
					out.println("<font color=gray>Outside service hours.</font>"); 
				}
				// Service Config update?
				else if ( isServiceCfgUpdated ) {
					String html = "<a href=\"jsp/config/config_backend.jsp?mode=" + svcUpdated[0].getType() + "\">" + svcUpdated[0].getVendorName() + "</a>";
					out.println("<font>Configuration update detected for " + html + ". Save/Restart required.</font>");
				}
				else if ( !CloudServices.servicesOnline()) {
					// 5/17/2020
					if ( Rbac.canStartServices(session) ) {
						out.println("<span id='nodeStatus'><font class=\"uk-badge uk-badge-danger\" color=gray>Off-line</font> &nbsp;&nbsp;<a id='aStart' href='javascript:doStart()'>Start</a></span>");
					}
				}
				else {
					if ( Rbac.canStartServices(session) ) {		// 5/17/2020
						out.println("<span id='nodeStatus'><font color=green>On-line</font> &nbsp;&nbsp;<a href='index.jsp?raction=stop'>Stop</a></span>");
					}
				}
			%>
				&nbsp;&nbsp;&nbsp;&nbsp;<a target="_blank" href="log/logview.jsp">Log View</a>
			 </div>
		</div>
		
		<% if ( cfg.requiresLicense() ) { %>
		<div class="form-group uk-form-row">
			<label class="col-sm-4 control-label">Licensed</label>
			
			<div class="col-sm-8"><%=licensed%>
				
			<%if ( !inDevMode && !isConfigured /*!CloudServices.isLicensed()*/) { %>
				<!-- vsilva 6/30/2016 removed duplicate msg. -->
				&nbsp;&nbsp;
				<a href="mailto:register@convergeone.com&Subject=License%20Request&Body=Product: Cloud Connector%0D%0AInstance%20Id: <%=instanceId%>%0D%0AMAC: <%=License.getSystemMACAddress()%>%0D%0AHost: <%=request.getLocalName()%>">Request a license</a>
				
			<% } %>
				
			</div>
		</div>
		<%} %>
			
		<% if ( profileMgr != null && !profileMgr.isEmpty() && isConfigured && (profileName != null) && !profileName.equals("Default")) { %>
		<div class="form-group uk-form-row">
			<label class="col-sm-4 control-label">Node Run Mode</label>
			
			<div class="col-sm-8"><%=!(cfg.getRunMode() == RunMode.CLUSTER) ? "<font color=gray>FAILOVER</font> - " : "<font color=gray>CLUSTER</font> - "%>
				<%=cfg.isPrimary()  
					? "<font color=green>Primary</font>" 
					//:  "<font color=gray>Secondary</font>" 
					: ( cfg.getPrimaryHost().isEmpty() || cfg.getPrimaryHost() == null ) 
						? "<font color=gray>Secondary</font>"
						: "<font color=gray>Secondary</font> (failover to " + cfg.getPrimaryHost() + ")"
				%>
			</div>
		</div>
		
		<div class="form-group uk-form-row">
			<label class="col-sm-4 control-label">Connection Profile</label>
			<div class="col-sm-8">
			 <%=cfg.getConnectionProfileName().isEmpty() ? "None" : "<a href=\"jsp/config/profiles.jsp\">" + cfg.getConnectionProfileName() + "</a>" %>
			<!-- &nbsp;&nbsp;&nbsp;<a href="jsp/config/profiles.jsp">Manage</a> -->
			</div>
		</div>
		<%} %>
		
		<div class="<%=SkinTools.cssFormGroupClass() %>">
			<label class="<%=SkinTools.cssFormGroupLabelClass()%>">Vendor</label>
			<div id="serviceInfo" class="<%=SkinTools.cssFormGroupContentClass()%>">
			
			</div>
		</div>
		
	</div> <!-- form-horizontal -->
	
	<% if ( !SkinTools.isMaterialTheme()) { %> 					 
           </div>
         </div>
	<% } %> 
	
		</div> <!-- col1 -->
		
		<div class="col-md-6 uk-width-medium-1-2">
		
            <div class="panel panel-white md-card">
                <div class="panel-heading md-card-toolbar">
                	<% if ( SkinTools.SKIN_PATH.contains("altair")) { %>
                    <div class="md-card-toolbar-actions">
                        <i class="md-icon material-icons md-card-fullscreen-activate">&#xE5D0;</i>
                    </div>
                    <% } %>
                    <h2 class="md-card-toolbar-heading-text">CPU</h2>
                </div>
                <div class="panel-body md-card-content">
                    <div class="full-bg">
                    	
                    	<canvas id="cpuchart"></canvas>
                    	
                    </div>
                    <div class="pt-md pull-left uk-float-right">
                        <h4 class="mt-n mb-n pt-xs"><small class="mt-n mb-sm">System</small> <span id="cpuSys">0</span></h4>
                    </div>
                    <div class="pt-md pull-right text-right">
                        <h4 class="mt-n mb-n pt-xs"><small class="mt-n mb-sm">JVM</small> <span id="cpuJvm">0</span></h4>
                    </div>
                </div>
            </div>
            
		</div>
		
	</div>          
   	<!-- END MAIN TAB -->
    
    <!-- ROW3: Container TAB	-->
	<div class="row uk-grid" data-uk-grid-margin data-uk-grid-match="{target:'.md-card-content'}">
		<div class="col-md-6 uk-width-medium-1-2">
			<div class="panel panel-white ov-h md-card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading md-card-toolbar">
					<h2 class="md-card-toolbar-heading-text">Container</h2>
	        	</div> 
		        <div class="panel-body ov-h md-card-content">
					<div>
						<canvas id="containerChart"></canvas>
					</div>
				</div>
			</div>
		</div>
		<div class="col-md-6 uk-width-medium-1-2">
			<div class="panel panel-white ov-h md-card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading md-card-toolbar">
					<h2 class="md-card-toolbar-heading-text">Network Performance</h2>
	        	</div> 
		        <div class="panel-body ov-h md-card-content">
					<div>
						<canvas id="requestChart"></canvas>
					</div>
				</div>
			</div>
		</div>
	</div>	
	
    <!-- ROW3: END CONTAINER TAB -->
    
    <!-- CPU TAB  -->
		<!-- Server metrics: IV & Mem Heap  style="width: 40%; margin: auto;" -->
		<div class="row" style="display: none;">
			<div class="col-lg-6">
				<!--  
				<div id="cpuAvgGauge" style="width:300px; height:300px;margin: auto;"></div>
				-->
			</div>
			<div class="col-lg-6">
				<!-- 
				<canvas id="cpuchart"></canvas>
				-->
			</div>
		</div>
		<div class="row" style="display: none;">
			<div class="col-lg-6">
				<canvas id="memchart"></canvas>
			</div>
			<div class="col-lg-6">
				<canvas id="tchart"></canvas>
			</div>
		</div>
    <!-- END CPU TAB -->
    

	<jsp:include page="<%=SkinTools.TILE_PATH_PAGE_END%>">
		<jsp:param value="<%=SkinTools.SKIN_PATH%>" name="basePath"/>
		<jsp:param value="" name="commonPath"/>
	</jsp:include>
	
  	<script type="text/javascript">
    	// Initialization
    	$(document).ready(function() {
    		
    		//initUIState();
    		initCharts();
    		
    		// poll for node status
    		var cfg 	= {};
    		cfg.url 	= '<%=contextPath%>/OSPerformance?op=os';
    		cfg.done 	= poll_done;
    		cfg.fail 	= poll_fail;
    		cfg.timeoutDone = 10000;
    		cfg.abortOnFail = false;
    		cfg.debug 	= false;
    		
    		Poller (cfg).start();
    	});
    	
    	// {"message":"OK","status":200
    	//	,"container":{"requestProcessor":[{"bytesSent":0,"bytesReceived":0,"processingTime":0,"maxTime":0,"errorCount":0,"requestCount":0,"modelerType":"org.apache.coyote.RequestGroupInfo","instance":"Catalina:type=GlobalRequestProcessor,name=\"http-nio-8443\""},{"bytesSent":21329,"bytesReceived":0,"processingTime":1327,"maxTime":750,"errorCount":1,"requestCount":8,"modelerType":"org.apache.coyote.RequestGroupInfo","instance":"Catalina:type=GlobalRequestProcessor,name=\"http-bio-8080\""}],"threadPool":[{"maxConnections":200,"currentThreadsBusy":1,"maxThreads":200,"connectionCount":2,"instance":"Catalina:type=ThreadPool,name=\"http-bio-8080\"","currentThreadCount":10},{"maxConnections":10000,"currentThreadsBusy":0,"maxThreads":2048,"connectionCount":1,"instance":"Catalina:type=ThreadPool,name=\"http-nio-8443\"","currentThreadCount":0}],"serverInfo":"Apache Tomcat/7.0.53"}
    	//	,"ProcessCpuLoad":0.0006188147817738354,"threadCount":33,"peakThreadCount":33,"heapFree":149945544,"heapTotal":259522560,"daemonThreadCount":31,"heapMax":518979584,"SystemCpuLoad":0.03774304687976837
    	//  , "nodeStatus":{"services":[{"statusCode":"SERVICE_ERROR","name":"Avaya AES","statusDesc":"Service is down."}],"nodeOnline":true} }
    	var count = 0;
    	function poll_done( json) {
    		//LOGD("Node reports: " + JSON.stringify(json) );
    		if ( json.status > 300 ) {
    			return;
    		}
    		var nodeStatus 	= json.nodeStatus;
    		var nodeOnline	= nodeStatus.nodeOnline; 	// true,false
    		var services	= nodeStatus.services; 		// [{"statusCode":"SERVICE_ERROR","name":"Avaya AES","statusDesc":"Service is down."}]
    		var failed		= -1;
    		for ( var i = 0 ; i < services.length ; i++ ) {
    			if ( services[i].statusCode.indexOf('ERROR') != -1) {
    				failed = i;
    				break;
    			}
    		}
    		if ( failed >= 0) {
    			//LOGE('Service Failed: ' + services[failed].name + ' ' + services[failed].statusDesc);
    			$('#mainPanel').attr('class', 'panel panel-danger card md-card');
    			$('#serviceInfo').html(services[failed].name + ' &nbsp;&nbsp;<font color=red>' + services[failed].statusDesc + '</font>');
    			
    			if ( count++ < 10 ) {
    				notify (services[failed].name + ': ' + services[failed].statusDesc, 'danger' );
    			}
    		}
    		else {
    			if ( services.length > 0) {
    				$('#mainPanel').attr('class', 'panel panel-success card md-card');	
    			}
    			var html 	= '';
    			var comma	= false;
    			for ( var i = 0 ; i < services.length ; i++ ) {
    				if ( comma ) {
    					html += " , ";
    				}
    				html += services[i].name + ( services[i].statusDesc ? ' - <font>' + services[i].statusDesc + '</font> &nbsp;&nbsp;' : '');
    				comma = true;
    			}
    			if ( html != '') {
    				$('#serviceInfo').html(html);	
    			}
    		}
    		// fix the node status labels
    		if ( nodeOnline) {
    			$('#nodeStatus').html("<font color=green>On-line</font> &nbsp;&nbsp;<a href='index.jsp?raction=stop'>Stop</a>");
    		}
    		else {
    			$('#mainPanel').attr('class', 'panel panel-danger card md-card');
    			$('#nodeStatus').html("<font class=\"uk-badge uk-badge-danger\" color=gray>Off-line</font> &nbsp;&nbsp;<a id='aStart' href='javascript:doStart()'>Start</a>");
    		}
    		
			// set node panel details
    		//$('#nodeDetails').html(' - Live Threads ' + json.threadCount + ' Free Memory ' + (json.heapFree/(1024*1024)).toFixed() + '/' + (json.heapTotal/(1024*1024)).toFixed() + ' MB');
			
			// charts (CPU tab)
			drawCharts(json);
			drawTiles(json);	//2/22/2020
    	}
    	
    	$(document).ready(function() {
    		var sparkResize;
    	    $(window).resize(function(e) {
    	        clearTimeout(sparkResize);
    	        sparkResize = setTimeout(drawTiles, 500);
    	    });	
    	});
    	
    	function poll_fail( jqXHR, textStatus ) {
    		//LOGE("Poll Failed: " + textStatus );
    	}

    	function peekAtDataset (cfg, idx) {
    		return cfg.data.datasets[idx].data[cfg.data.datasets[idx].data.length - 1];
    	}

    	/*
    	 * {"message":"OK","status":200,"container":{"requestProcessor":[{"bytesSent":0,"bytesReceived":0,"processingTime":0,"maxTime":0,"errorCount":0,"requestCount":0,"modelerType":"org.apache.coyote.RequestGroupInfo","instance":"Catalina:type=GlobalRequestProcessor,name=\"http-bio-8443\""},{"bytesSent":39491396,"bytesReceived":0,"processingTime":6906,"maxTime":843,"errorCount":1,"requestCount":1060,"modelerType":"org.apache.coyote.RequestGroupInfo","instance":"Catalina:type=GlobalRequestProcessor,name=\"http-bio-8080\""}],"threadPool":[{"maxConnections":2048,"currentThreadsBusy":0,"maxThreads":2048,"connectionCount":1,"instance":"Catalina:type=ThreadPool,name=\"http-bio-8443\"","currentThreadCount":10},{"maxConnections":2048,"currentThreadsBusy":42,"maxThreads":2048,"connectionCount":43,"instance":"Catalina:type=ThreadPool,name=\"http-bio-8080\"","currentThreadCount":54}],"serverInfo":"Apache Tomcat/7.0.91"}
    	 * ,"ProcessCpuLoad":0.025713345035910606
    	 * ,"nodeStatus":{"services":[{"statusCode":"OFF_LINE","name":"Cisco Finesse","type":"CALL_CENTER","statusDesc":"Unknown host cce11fin2.lab.com"}]
    	 * 		,"nodeOnline":false}
    	 * ,"threadCount":119,"peakThreadCount":122,"heapFree":17186152,"heapTotal":69451776,"daemonThreadCount":110,"heapMax":259522560,"SystemCpuLoad":0.6660934686660767}
    	 */
    	function drawTiles(json) {
    		if ( !json ) {
    			return;
    		}
    		//LOGD('JSON:' + JSON.stringify(json));
    		
    		// CPU: Sys (0), proc(1)
    		var cpuSys = peekAtDataset (configCpu, 0); 
    		var cpuJvm = peekAtDataset (configCpu, 1); 
    	    //$('#sparkline-cpu').sparkline( configCpu.data.datasets[0].data, { type: 'line', barColor: 'rgba(255,255,255,0.5)', height: '48px',width: '50%', chartRangeMin: 1, tooltipPrefix: '% CPU: '});
    	    $('#cpuSys').html(cpuSys.toFixed(2) + ' %');
    	    $('#cpuJvm').html(cpuJvm.toFixed(2) + ' %');

    	    // MEM: - total (0), used (1), max (2)
    	    var barColor 	= '<%=( SkinTools.SKIN_PATH.contains("altair") ? "rgba(0,0,0,0.5)" : "rgba(255,255,255,0.5)")  %>'; // rgba(0,0,0,0.5)
    	    var color 		= '<%=( SkinTools.SKIN_PATH.contains("altair") ? "black" : "white")  %>'; 
    	    
    	    $('#sparkline-heap').sparkline( configMem.data.datasets[0].data, { type: 'bar', fillColor: true, barColor: barColor, barWidth: 10, barSpacing: 4, height: '64px', width: '100%', chartRangeMin: 1, tooltipPrefix: 'Total: '});
    	    $('#sparkline-heap').sparkline( configMem.data.datasets[1].data, { type: 'line', fillColor: false, composite: true, height: '64px', width: '100%', chartRangeMin: 1, tooltipPrefix: 'Used: '});
    	    var free = peekAtDataset (configMem, 0) - peekAtDataset (configMem, 1); 
    	    $('#tile-freemem').html(free);
    	    
    	    // THREADS: live: configTr.data.datasets[2].data, peak: configTr.data.datasets[1].data, daemon: configTr.data.datasets[0].data
    	    var data = [ peekAtDataset (configTr, 2) 
    	    	, peekAtDataset (configTr, 1) 
    	    	, peekAtDataset (configTr, 0) 
    	    ];
    	    $('#tile-threads').html(data[0]);
    	    $('#sparkline-threads').sparkline( data, { type: 'bar', barColor: barColor, barWidth: 20, barSpacing: 4, height: '64px', width: '100%', chartRangeMin: 1 });

    	    // fileSystems : [ { name: 'c:\', free: 123, total, 123}, ... ]
    	    if ( json.fileSystems ) {
    	    	var disks 	= 0;
    	    	var html 	= '<font color="' + color + '">';
    	    	for ( var i = 0 ; i < json.fileSystems.length ; i++) {
    	    		var fs = json.fileSystems[i];
    	    		if (fs.total > 0) {
    	    			disks++;
    	    			html += fs.name + ' ' + (fs.free/(1024*1024*1024)).toFixed(1) + ' GB free of ' + (fs.total/(1024*1024*1024)).toFixed(0) + ' GB<br>';
    	    		}
    	    	}
    	    	$('#sparkline-disk').html(html + '</font>');
    	    	$('#diskCount').html(disks);
    	    }
    	    
    	}    	
   	</script>  
</body>

</html>
