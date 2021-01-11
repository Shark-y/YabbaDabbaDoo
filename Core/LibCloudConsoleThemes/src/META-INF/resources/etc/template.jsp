<%@page import="com.cloud.console.ThemeManager"%>
<%@page import="com.cloud.console.SkinTools"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	final String contextPath 	= getServletContext().getContextPath();
	String theme				= (String)session.getAttribute("theme");	// console theme (should not be null)
	//String title				= (String)session.getAttribute("title");	// Top Left title (should not be null)
	
	if ( theme == null)			theme = ThemeManager.DEFAULT_THEME;
	
	String statusMessage		= "NULL";
	String statusType			= null;
	final String basePath		= "../";
%>

<!DOCTYPE html>
<html>
<head>

<jsp:include page="<%=SkinTools.buildHeadTilePath(basePath) %>">
	<jsp:param value="<%=SkinTools.buildBasePath(basePath) %>" name="basePath"/>
	<jsp:param value="<%=basePath%>" name="commonPath"/>
	<jsp:param value="<%=theme%>" name="theme"/>
	<jsp:param value="View Title" name="title"/>
</jsp:include>

</head>
<body>

	<jsp:include page="<%=SkinTools.buildPageStartTilePath(basePath) %>">
		<jsp:param value="<%=SkinTools.buildBasePath(basePath) %>" name="basePath"/>
		<jsp:param value="<%=basePath%>" name="commonPath"/>
		<jsp:param value="Sample Heading" name="pageTitle"/>
		<jsp:param value="Home,Pages,Sample View" name="crumbLabels"/>
		<jsp:param value="../index.jsp,#,class_active" name="crumbLinks"/>
	</jsp:include>

	<!-- STATUS MESSAGE -->
	<jsp:include page="<%=SkinTools.buildStatusMessagePath(basePath)%>">
		<jsp:param value="<%=statusMessage%>" name="statusMsg"/>
		<jsp:param value="<%=statusType%>" name="statusType"/>
	</jsp:include>

	<!-- CONTENT START -->
          <!-- statistics (small charts) -->
            <div class="uk-grid uk-grid-width-large-1-4 uk-grid-width-medium-1-2 uk-grid-medium uk-sortable sortable-handler hierarchical_show" data-uk-sortable data-uk-grid-margin>
                <div>
                    <div class="md-card">
                        <div class="md-card-content">
                            <div class="uk-float-right uk-margin-top uk-margin-small-right"><span class="peity_visitors peity_data">5,3,9,6,5,9,7</span></div>
                            <span class="uk-text-muted uk-text-small">Visitors (last 7d)</span>
                            <h2 class="uk-margin-remove"><span class="countUpMe">0<noscript>12456</noscript></span></h2>
                        </div>
                    </div>
                </div>
                <div>
                    <div class="md-card">
                        <div class="md-card-content">
                            <div class="uk-float-right uk-margin-top uk-margin-small-right"><span class="peity_sale peity_data">5,3,9,6,5,9,7,3,5,2</span></div>
                            <span class="uk-text-muted uk-text-small">Sale</span>
                            <h2 class="uk-margin-remove">$<span class="countUpMe">0<noscript>142384</noscript></span></h2>
                        </div>
                    </div>
                </div>
                <div>
                    <div class="md-card">
                        <div class="md-card-content">
                            <div class="uk-float-right uk-margin-top uk-margin-small-right"><span class="peity_orders peity_data">64/100</span></div>
                            <span class="uk-text-muted uk-text-small">Orders Completed</span>
                            <h2 class="uk-margin-remove"><span class="countUpMe">0<noscript>64</noscript></span>%</h2>
                        </div>
                    </div>
                </div>
                <div>
                    <div class="md-card">
                        <div class="md-card-content">
                            <div class="uk-float-right uk-margin-top uk-margin-small-right"><span class="peity_live peity_data">5,3,9,6,5,9,7,3,5,2,5,3,9,6,5,9,7,3,5,2</span></div>
                            <span class="uk-text-muted uk-text-small">Visitors (live)</span>
                            <h2 class="uk-margin-remove" id="peity_live_text">46</h2>
                        </div>
                    </div>
                </div>
            </div>
            
<!-- OS TAB -->
	<div class="row uk-grid" data-uk-grid-margin data-uk-grid-match="{target:'.md-card-content'}">
		<div class="col-md-6 uk-width-medium-1-2">
	
		    <div id="mainPanel" class="panel panel-default card md-card">
		        <div id="mainPanelHeader" class="panel-heading card-header md-card-toolbar">
		            <h2 class="panel-title" >WebConsole <span id="nodeDetails"></span></h2> 
		        </div>
		        <div class="panel-body card-body md-card-content">
		 	
		
				<div class="form-horizontal"> 
				
					<div class="form-group">
						<label class="col-sm-4 control-label">Container</label>
						<div class="col-sm-8">Windows 7 / Apache Tomcat/8.5.11 / Java 1.8.0_131 @ C:\Program Files (x86)\Java\jdk1.8.0_131\jre</div>
					</div>
					<div class="form-group">
						<label class="col-sm-4 control-label">Node Status</label>
						<div class="col-sm-8">
						<span id='nodeStatus'><font color=green>On-line</font> &nbsp;&nbsp;<a href='index.jsp?raction=stop'>Stop</a></span>
							&nbsp;&nbsp;&nbsp;&nbsp;<a target="_blank" href="log/logview.jsp">Log View</a>
						 </div>
					</div>
					<div class="form-group">
						<label class="col-sm-4 control-label">Vendor</label>
						<div id="serviceInfo" class="col-sm-8">
						
						</div>
					</div>
				</div> <!-- form-horizontal -->
			
		        </div>
			</div>
	 
		</div> <!-- col1 -->
		
		<div class="col-md-6 uk-width-medium-1-2">
		
            <div class="panel panel-white md-card">
                <div class="panel-heading md-card-toolbar">
                    <h2>CPU</h2>
                </div>
                <div class="panel-body md-card-content">
                    <div class="full-bg">
                    	
                    	<canvas id="cpuchart"></canvas>
                    	
                    </div>
                    <div class="pt-md pull-left">
                        <h4 class="mt-n mb-n pt-xs"><small class="mt-n mb-sm">System</small><span id="cpuSys">0</span></h4>
                    </div>
                    <div class="pt-md pull-right text-right">
                        <h4 class="mt-n mb-n pt-xs"><small class="mt-n mb-sm">JVM</small><span id="cpuJvm">0</span></h4>
                    </div>
                </div>
            </div>
            
		</div>
		
	</div>          
   	<!-- END MAIN TAB -->            
	<!-- CONTENT END -->
	
	<jsp:include page="<%=SkinTools.buildPageEndTilePath(basePath) %>">
		<jsp:param value="<%=SkinTools.buildBasePath(basePath) %>" name="basePath"/>
		<jsp:param value="<%=basePath%>" name="commonPath"/>
	</jsp:include>

	<!--  dashbord functions -->
    <script src="dashboard.js"></script>

	<script type="text/javascript">
	
	$(document).ready(function() {
	});
	
	</script>

</body>
</html>