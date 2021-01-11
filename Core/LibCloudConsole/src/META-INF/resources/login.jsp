<%@page import="com.cloud.console.iam.IAM"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.cloud.console.iam.Rbac"%>
<%@page import="java.io.IOException"%>
<%@page import="com.cloud.core.net.RemoteAuthenticator"%>
<%@page import="com.cloud.core.logging.Auditor.AuditVerb"%>
<%@page import="com.cloud.console.ServletAuditor"%>
<%@page import="com.cloud.core.logging.Auditor.AuditSource"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.security.EncryptionTool"%>


<%!static final String TAG = "[LOGIN]";

	static void LOGD(String text) {
		System.out.println(TAG + " " + text);
	}

	static void LOGE(String text) {
		System.err.println(TAG + " " + text);
	}%>

<%
	// resource base path (Requires skin theme: material)
	final String basePath 		= "skins/booterial";

	// CTX Root path for absolute paths
	final String contextPath 	= getServletContext().getContextPath();

	// HTTP request method
	final String method			= request.getMethod().toLowerCase();
	
	// The cluster manager sets the config into the servlet context
	NodeConfiguration cfgServer = (NodeConfiguration) getServletContext() .getAttribute(NodeConfiguration.SKEY_CFG_SERVER);

	// fatal startup error?
	if ( getServletContext().getAttribute(CloudServices.CTX_STARTUP_EXCEPTION) != null ) {
		response.sendRedirect("error.jsp");
		return;
	}

	// get config from session?
	if (cfgServer == null) {
		cfgServer = (NodeConfiguration) session.getAttribute(NodeConfiguration.SKEY_CFG_SERVER);
	}

	// NULL server cfg? May be the cluster manager, look in the CloudServices.
	if (cfgServer == null) {
		cfgServer = CloudServices.getNodeConfig();
	}

	// still null cfg? page will crash :(
	if (cfgServer == null) {
		//LOGE("**** FATAL: Unable to load the server configuration from session or disk.");
		getServletContext().setAttribute(CloudServices.CTX_STARTUP_EXCEPTION, new Exception(CloudServices.getLastError()));
		response.sendRedirect("error.jsp");
		return;
	}
	final String adminName 		= cfgServer.getSysAdminUser(); 	// sysadmin
	String adminPwd 			= cfgServer.getSysAdminPwd(); 	// Hashed pwd

	final boolean loggedIn 		= session.getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
	final String icon			= ""; //<i class=\"fab fa-windows fa-2x\"></i> &nbsp;&nbsp;";	// window icon
	//final String icon			= "<img src=\"skins/clouds/img/logo-dark.png\"> &nbsp;&nbsp;"; 
	final String queryStr 		= request.getQueryString();
	final boolean remoteAuth 	= RemoteAuthenticator.enabled();
			
	String action 		= request.getParameter("action"); 	// action
	//String redirect 	= request.getParameter("r"); 		// redirect page (optional)
	String redirect 	= contextPath; 						// vsilva 11/25/2016 always back to home.
	String uiMessage 	= request.getParameter("m"); 		// message (optional) - null;

	LOGD("Action=" + action + " redirect=" + redirect + " QueryString: " + queryStr + " adminName:" + adminName);

	// CANNOT change the pwd if NULL
	if (action != null && action.equals("changepwd") && (adminPwd == null || adminPwd.isEmpty())) {
		LOGE("Cannot change pwd if NOT yet set!");
		action = null;
	}

	// create a redirect query string. Add this QS to the login form!
	// Deprecated, always back home String redirectQS = redirect != null ? "&r=" + redirect + "&" + queryStr : "";
	String redirectQS = "&" + queryStr;
	
	if (action != null) {
		if (action.equals("logout")) {
			// logout
			session.removeAttribute(NodeConfiguration.SKEY_LOGGED_IN);

			Auditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, "LOGOFF successful.");

			// back home
			response.sendRedirect("index.jsp");
		} 
		else if (action.equals("login")) {
			// login 
			final String usr 			= request.getParameter("txt_usr");
			final String pwd 			= request.getParameter("txt_pwd");
			final String identity 		= request.getParameter("identity");	// may be null 9/6/2020
			
			if ( pwd == null ) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			String secret 		= EncryptionTool.HASH(pwd); 			// new  SHA-256;
			String oldSecret 	= EncryptionTool.HASH(pwd, "SHA-1"); 	// 4/18/2017 legacy SHA-1;
			
			LOGD("Login: Entered u:" + usr + " s:" + secret  + " Stored:" + adminPwd);
			
			/* This should ot happen if pwd != null if ( secret == null || oldSecret == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			} */
			// 5/8/2020 Note: user == null when (txt_usr) is deiabled (LINUX)
			if ( adminName.equals(usr) || (usr == null) ) {
				// local
				if (secret.equals(adminPwd)) {
					// login ok...
					session.setAttribute(NodeConfiguration.SKEY_LOGGED_IN, true);
					
					// 5/17/2020 Save the user in session for RBAC. Note user may be null in Linux
					session.setAttribute(Rbac.SKEY_LOGGED_USER, usr != null ? usr : adminName);
					
					// vsilva 11/25/2016 - always redirect to the home page
					String redirectUrl = contextPath; // redirect + "?" + queryStr;
					LOGD("Login ok. Redirect to: " + redirectUrl);
	
					ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request, "Login successful.");
	
					if (redirect != null) {
						response.sendRedirect(redirectUrl);
						return;
					} else {
						uiMessage = "Login ok but missing redirect page!";
					}
				}
				else if (oldSecret.equals(adminPwd)) {
					// 4/18/2017 Using a legacy SHA-1. Force reset by setting the admin pwd (adminPwd) to null.
					adminPwd 	= null;
					uiMessage	= "Password must be reset.";
				}
				else {
					ServletAuditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request, "Login failed: Bad password.");
	
					uiMessage 	= "Login failed.";
					action 		= "loginshow";
				}
			}
			else {
				// remote authentication via winAD, etc
				try {
					if ( identity != null ) {
						IAM.authenticate(usr, pwd, identity);
					}
					else {
						// Win AD only
						RemoteAuthenticator.authenticate(usr, pwd);
					}
					
					// login ok...
					session.setAttribute(NodeConfiguration.SKEY_LOGGED_IN, true);

					// 5/17/2020 Save the user in session for RBAC.
					session.setAttribute(Rbac.SKEY_LOGGED_USER, usr);
					
					// redirect to the home page
					response.sendRedirect(contextPath);
				}
				catch ( Exception ex) {
					uiMessage 	= ex.getMessage();
					action 		= "loginshow";
				}
			}
		} 
		else if (action.equals("savepwd")) {
			// change Pwd: Compare old w/ server config. If equal save new... 
			String oldPwd = request.getParameter("txt_pwd");
			String newPwd = request.getParameter("txt_pwd1");
			String oldSecret = EncryptionTool.HASH (oldPwd);

			//LOGD ("Save Pwd: Old: " + oldPwd + "/" + oldSecret + "  New:" + newPwd + " Stored:" + adminPwd);

			if (!oldSecret.equals(adminPwd)) {
				uiMessage = "Password mismatch";
				action = "changepwd";
			} else {
				// save new
				String newSecret = EncryptionTool.HASH/*SHA1*/(newPwd);

				cfgServer.setSysAdminPwd(newSecret);
				cfgServer.save();

				// login...
				LOGD("Save Pwd ok. Redirect to:" + redirect);
				ServletAuditor.warn(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request,	"Password changed.");

				if (redirect != null) {
					response.sendRedirect(redirect);
					return;
				} else {
					uiMessage = "Save pwd ok but missing redirect page!";
				}
			}
		} 
		else if (action.equals("createpwd")) {
			// save admin pwd	
			String pwd 	= request.getParameter("txt_pwd");
			String pwd1 = request.getParameter("txt_pwd1");

			//LOGD("Create Pwd p1=" + pwd + " p2=" + pwd1);

			if (!pwd.equals(pwd1)) {
				uiMessage = "Password mismatch";
			} 
			else {
				// set pwd. 
				String enc = EncryptionTool.HASH/*SHA1*/(pwd);

				cfgServer.setSysAdminPwd(enc);

				// Save server props
				cfgServer.save();

				ServletAuditor.info(AuditSource.CLOUD_CONSOLE, AuditVerb.ACCESS_ATTEMPT, request, "Password created.");

				// login...
				adminPwd = cfgServer.getSysAdminPwd();
				action = "loginshow";
			}
		}
		else if ( !action.equals("loginshow") && !action.equals("changepwd") ) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
	/*
	else {
		LOGE("Invalid or NULL action: " + action);
	} */
	/* deprecated
	if (redirect == null) {
		LOGE("A redirect page is required!");
	} */
	// defaults
	if ( uiMessage == null && (adminPwd == null || adminPwd.isEmpty()) ) {
		uiMessage = "Create a Password.";
	}
	if ( uiMessage == null) {
		uiMessage = "Please login.";	
	}
	uiMessage 				= icon +  "<p>" + uiMessage + "<p>";
	List<String> iamNames	= IAM.getNames();
%>
<!DOCTYPE html>
<html>
<!--[if IE 9 ]><html class="ie9"><![endif]-->
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Cloud Sign-In</title>

<link rel="icon" sizes="192x192" href="img/favicon.png">

<!-- Vendor CSS -->
<link href="<%=basePath%>/css/animate.css" rel="stylesheet">
<link href="<%=basePath%>/css/material-design-iconic-font.css" rel="stylesheet">

<!-- CSS -->
<link href="<%=basePath%>/css/app.min.1.css" rel="stylesheet">
<link href="<%=basePath%>/css/app.min.2.css" rel="stylesheet">

<!-- Font Awesome -->
<link href="skins/clouds/../bootstrap/font-awesome/css/font-awesome.css" rel="stylesheet" type="text/css">

<!-- 3/2/2019 Font Awesome v5 brands-->
<link href="skins/clouds/css/font-awesome-v572brands.css" rel="stylesheet" type="text/css">

<style>
body.login-content:before {
  height: 100%;
  background: #28343d;
}  
</style>

</head>

<body class="login-content">

	<%
		if (adminPwd == null || adminPwd.isEmpty()) {
	%>

	<!-- Create Password -->
	<form id="frmCreatePwd" method="post" class="lc-block toggled" action="login.jsp?action=createpwd<%=redirectQS%>">

		<div id="l-register">
			<p class="text-left"><%=uiMessage %></p>

			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-account"></i></span>
				<div class="fg-line">
					<input type="text" class="form-control" placeholder="Username" value="<%=adminName%>" disabled="disabled">
				</div>
			</div>

			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-male"></i></span>
				<div class="fg-line">
					<input id="txt_pwd" name="txt_pwd" type="password" class="form-control" placeholder="Password" autofocus="autofocus" autocomplete="off">
				</div>
			</div>

			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-male"></i></span>
				<div class="fg-line">
					<input id="txt_pwd1" name="txt_pwd1" type="password" class="form-control" placeholder="Confirm Password" autocomplete="off">
				</div>
			</div>

			<div class="clearfix"></div>

			<button type="submit" class="btn btn-login btn-danger btn-float">
				<i class="zmdi zmdi-arrow-forward"></i>
			</button>
		</div>
	</form>

	<%
		} else if (action != null && action.equalsIgnoreCase("changepwd")) {
	%>

	<form id="frmChangePwd" method="post" class="lc-block toggled" action="login.jsp?action=savepwd<%=redirectQS%>">

		<!-- Change Password -->
		<div id="l-forget-password">
			<p class="text-left">Change Password for <%=adminName%>.</p>

			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-male"></i></span>
				<div class="fg-line">
					<input type="password" id="txt_pwd" name="txt_pwd" class="form-control" placeholder="Old Password" autofocus="autofocus" autocomplete="off">
				</div>
			</div>

			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-male"></i></span>
				<div class="fg-line">
					<input type="password" id="txt_pwd1" name="txt_pwd1" class="form-control" placeholder="New Password" autocomplete="off">
				</div>
			</div>

			<button type="submit" class="btn btn-login btn-danger btn-float">
				<i class="zmdi zmdi-arrow-forward"></i>
			</button>
		</div>
	</form>

	<%
		} else {
	%>


	<!-- Login --> 
	<form id="frmLogin" class="lc-block toggled" method="post" action="login.jsp?action=login<%=redirectQS%>">
		<div id="l-login">
			<p class="text-left"><%=uiMessage %></p>
			
			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-account"></i></span>
				<div class="fg-line">
					<!-- 9/2/2020
					<input type="text" id="txt_usr" name="txt_usr"  class="form-control" placeholder="Username" value="<%=adminName%>" <%=!remoteAuth ? "disabled=\"disabled\"" : "" %>>
					-->
					<input type="text" id="txt_usr" name="txt_usr"  class="form-control" autofocus="autofocus" placeholder="Username">
				</div>
			</div>

			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-male"></i></span>
				<div class="fg-line">
					<input id="txt_pwd" name="txt_pwd" type="password" class="form-control" placeholder="Password"  autocomplete="off"
						onkeydown="if (event.keyCode == 13) document.forms['frmLogin'].submit()">
				</div>
			</div>
			<% if ( iamNames != null) { %>
			<div class="input-group m-b-20">
				<span class="input-group-addon"><i class="zmdi zmdi-accounts"></i></span>
				<div class="fg-line">
					<select id="identity" name="identity" class="form-control">
					<% for (String name : iamNames ) { %>
						<option value="<%=name%>"><%=name%></option>
					<% } %>
					</select>
				</div>
			</div>
			<% } %>
			<div class="clearfix"></div>

			<button type="submit" class="btn btn-login btn-danger btn-float">
				<i class="zmdi zmdi-arrow-forward"></i>
			</button>
		</div>
	</form> 
	<%
		}
	%>


	<!-- Older IE warning message -->
	<!--[if lt IE 9]>
            <div class="ie-warning">
                <h1 class="c-white">Warning!!</h1>
                <p>You are using an outdated version of Internet Explorer, please upgrade <br/>to any of the following web browsers to access this website.</p>
                <div class="iew-container">
                    <ul class="iew-download">
                        <li>
                                <div>Chrome</div>
                        </li>
                        <li>
                                <div>Firefox</div>
                        </li>
                        <li>
                                <div>Opera</div>
                        </li>
                        <li>
                                <div>Safari</div>
                            </a>
                        </li>
                        <li>
                                <div>IE (New)</div>
                        </li>
                    </ul>
                </div>
                <p>Sorry for the inconvenience!</p>
            </div>   
        <![endif]-->

	<!-- Javascript Libraries -->
	<script src="js/jquery.js"></script>
	<script src="skins/bootstrap/js/bootstrap.js"></script>

	<script src="<%=basePath%>/js/waves.js"></script>

	<!-- Placeholder for IE9 -->
	<!--[if IE 9 ]>
            <script src="<%=basePath%>/js/jquery.placeholder.min.js"></script>
        <![endif]-->

	<script src="<%=basePath%>/js/functions.js"></script>

	<script type="text/javascript">
	
	function on_load() {
		var obj = document.getElementById("txt_usr"); // pwd");
		if ( obj ) {
			obj.focus();
		}
	}
	
	$(document).ready(function() {
		on_load();
	});
	
	
	</script>

</body>
</html>