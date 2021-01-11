package com.cloud.console.filter;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.cloud.console.JSPLoggerTool;
import com.cloud.console.ServletAuditor;
import com.cloud.console.filter.OWASPConfig.ElementType;
import com.cloud.console.servlet.FileUpload;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.Auditor.AuditSource;
import com.cloud.core.logging.Auditor.AuditVerb;
import com.cloud.core.security.JWT;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.NodeConfiguration;
import com.cloud.core.types.CoreTypes;
import com.cloud.core.w3.OAuth1;
import com.cloud.security.CloudSecurity;

/**
 * <h2>Stronger Anti Cross-Site Scripting (XSS) Filter.</h2>
 * Here is a good and simple anti cross-site scripting (XSS) filter written for Java web applications.
 * What it basically does is remove all suspicious strings from request parameters before returning them to the application.
 *  
 * <p>You should configure it as the first filter in your chain (web.xml) and it’s generally a good idea to let it catch every request made to your site.</p>
 * 
 * @see https://dzone.com/articles/stronger-anti-cross-site
 * @author VSilva
 * 
 * @version 1.0.1 - 07/16/2017 Added security checks for multipart/form-data content types.
 * @version 1.0.2 - 10/19/2017 Check for fatal startup exceptions.
 */
@WebFilter (filterName = "OWASPGateKeeper", urlPatterns = "/*", asyncSupported = true)
public class OWASPGateKeeper implements Filter {

	@Override
	public void init(FilterConfig config) throws ServletException {
		try {
			CloudSecurity.loadSecurityDescriptors();
		} catch (Throwable  e) {
			// save the error on session for the admin console
			config.getServletContext().setAttribute(CloudServices.CTX_STARTUP_EXCEPTION, e);
			
			JSPLoggerTool.JSP_LOGE("[SECURITY]","Security descriptor load failed", e);
		}
	}

	@Override
	public void destroy() {
	}

	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
			throws IOException, ServletException 
	{
		final HttpServletResponse resp 	= (HttpServletResponse) response;
		final HttpServletRequest req 		= (HttpServletRequest)request;

		/**
		 * Note: Reading parameters from an HTTP POST w/ CT application/x-www-form-urlencoded will empty the request body
		 * Causing servlets like ServletRawMessage (or any servlet that reads from an POST/urlencoded) to fail.
		 */
		boolean skip 				= false;
		final String URI 			= req.getRequestURI();						// not null
		final String ctxPath 		= req.getServletContext().getContextPath();	// not null
		final String method			= req.getMethod().toLowerCase();			// not null
		final String contentType	= req.getContentType() != null ? req.getContentType() : "";
		final String queryString	= req.getQueryString();
		
		/**
		 * Clickjacking Defense Cheat Sheet
		 * https://www.owasp.org/index.php/Clickjacking_Defense_Cheat_Sheet
		 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options
		 */
		if ( CloudSecurity.preventClickJacking(URI)) {
			resp.setHeader("X-Frame-Options", "DENY");
		}
		/**
		 * Security Headers - X-XSS-Protection: In order to improve the security of your site against some types of XSS (cross-site scripting) attacks, it is recommended that you add the following header to your site:
		 * X-XSS-Protection: 1; mode=block
		 * https://kb.sucuri.net/warnings/hardening/headers-x-xss-protection
		 */
		resp.setHeader("X-XSS-Protection", "1; mode=block");
		
		/**
		 * MIME-sniffing on the response body
		 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
		 */
		resp.setHeader("X-Content-Type-Options", "nosniff");
		
		/* Check for fatal startup ex? */
		/* 10/21/2017 recursive call issues if ( req.getServletContext().getAttribute(CloudServices.CTX_STARTUP_EXCEPTION) != null ) {
			req.getServletContext().setAttribute(CloudServices.CTX_STARTUP_EXCEPTION + "-CLONE", req.getServletContext().getAttribute(CloudServices.CTX_STARTUP_EXCEPTION));
			req.getServletContext().removeAttribute(CloudServices.CTX_STARTUP_EXCEPTION); // avoid recursive calls
			resp.sendRedirect(ctxPath +  "/error.jsp");
			return;
		} */

		// All JSPs require authentication (except login.jsp). Including the base context path.
		final boolean requiresAuth	= URI.equalsIgnoreCase(ctxPath + "/")  || CloudSecurity.requiresAuthentication(URI);
		final boolean loggedIn 		= req.getSession().getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
		
		// Servlet exceptions: Only for POST x-www-form-urlencoded requests
		if ( CloudSecurity.isUriExempt(URI, queryString, method, contentType)) {
			skip = true;
		}
		
		// Check for authentication. Note: Skip the login resource (login.jsp). Redirect to login if required.
		if ( requiresAuth ) {
			//final boolean loggedIn 	= req.getSession().getAttribute(NodeConfiguration.SKEY_LOGGED_IN) != null;
			final boolean isRoot	= URI.endsWith("/");

			if ( ! loggedIn ) {
				final boolean sendToLogin 	= URI.matches(".*\\.jsp.*") || isRoot;

				if (sendToLogin ) {
					// JSP: to login.
					resp.sendRedirect(ctxPath +  "/login.jsp?r=" + ctxPath + "/" ); // URI);
				}
				else {
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				}
				return;
			}
		}
		// Check for valid params, headers. Return Bad request (400) if failed.
		try {
			if ( !skip ) {
				// 5/15/2017 Set the default character encoding to unicode. This is required by UNICODE POST requests request.getParameter(...)
				if (request.getCharacterEncoding() == null) {
				    request.setCharacterEncoding(com.cloud.core.types.CoreTypes.ENCODING_UTF8);
				}
				
				checkHeaders(req);
				checkParameters(req);
				checkParts(req);
			}
		} 
		catch (Exception e) {
			// For the console
			JSPLoggerTool.JSP_LOGE("[SECURITY]", e.toString() + " " + ServletAuditor.getFullRequestInfo(req));
			
			//Audit Trail
			ServletAuditor.danger(AuditSource.SECURITY, AuditVerb.SECURIY_VIOLATION, req, e.getMessage());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Security check violation.");
			return;
		}
		
		/**
		 * Check for authorization within the security descriptor. Return 401 Unauthorized if failed.
		 * Exceptions are permitted.
		 */
		if ( !skip ) {
			NodeConfiguration config = CloudServices.getNodeConfig(); // may be null for the CloudClusterManager
			
			if ( config != null && config.isAuthorizationEnabled()) {
				// Authorize external requests only (loggeIn == false)
				if ( CloudSecurity.requiresAuthorization(URI) && !loggedIn ) {
					try {
						final String type = CloudSecurity.getAuthorizationType(URI);
						if ( type.equals("oauth1")) {
							verifySignatureOAuth1(req, CloudServices.getNodeConfig().getAuthorizationOAuth1Key());
						}
						else {
							// default JWT
							verifySignatureJWT(req, CloudServices.getNodeConfig().getAuthorizationConsumerSecret());
						}
					}
					// Authorization failed - return 401 Unauthorized.
					catch (SecurityException e) {
						// For the console
						JSPLoggerTool.JSP_LOGE("[SECURITY]", "Authorization failed: " + e.getMessage());
						
						//Audit Trail
						ServletAuditor.danger(AuditSource.SECURITY, AuditVerb.AUTHORIZATION_VIOLATION, req, e.getMessage());
						resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization required.");
						return;
					}
					// Some verification error (a bug). Return 400 - bad request
					catch ( IOException ioe) {
						JSPLoggerTool.JSP_LOGE("[SECURITY]", "Authorization Verification I/O Error: " + ioe.toString());
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request.");
						return;
					}
				}
			}
		}
		
		chain.doFilter(request, response);		
	}

    /**
     * Verify a signature using the OAuth1 header format: authorization: <i>OAuth k1="v1", k2="v2", ... Kn="Vn" </i>
     * <pre>
     * OAuth oauth_signature="IrWre5020eJm9QfJJ8m3TvzlpjY%3D", oauth_nonce="eefd26f5-b171-4c32-8fb5-8cbd50039b69", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="d13eba0e667a41178280bd77d6daa890", oauth_token="c54bf947392f408bb6ef956d3802428c", oauth_timestamp="1447806308"
     * </pre>
     * @param request The {@link HttpServletRequest}.
     * @param Key The secret used to verify the signature contained in the authorization header.
     * @throws IOException If an I/O error occurs in the verification process.
     * @throws SecurityException If authorization fails.
     */
    public static void verifySignatureOAuth1 (javax.servlet.http.HttpServletRequest request, String key) throws SecurityException, IOException {
    	// OAuth oauth_signature="IrWre5020eJm9QfJJ8m3TvzlpjY%3D", oauth_nonce="eefd26f5-b171-4c32-8fb5-8cbd50039b69", oauth_version="1.0", oauth_signature_method="HMAC-SHA1", oauth_consumer_key="d13eba0e667a41178280bd77d6daa890", oauth_token="c54bf947392f408bb6ef956d3802428c", oauth_timestamp="1447806308"
    	String authorization 	= request.getHeader("authorization");	// required
    	String url				= request.getRequestURL().toString();	// not null
		String method 			= request.getMethod().toUpperCase();	// not null
    	String qs				= request.getQueryString();				// optional
    	
    	if ( authorization == null ) 	throw new SecurityException("Missing Authorization header from: " + url);
    	
    	OAuth1.verifySignature(method, url, qs, authorization, key);
    } 

    /**
     * Verify a request signature using a JWT token from the request parameter "token" or header Authorization: Bearer [token].
     * @param request HTTP request.
     * @param secret The secret used to verify the token signature.
     * @throws SecurityException If the signature verification fails.
     * @throws IOException On I/O, token format errors.
     */
    public static void verifySignatureJWT (HttpServletRequest request, String secret) throws SecurityException, IOException {
		String token 		= request.getParameter("token");			// get token
		
		// Check Authorization header:  Authorization: Bearer <token> - https://swagger.io/docs/specification/authentication/bearer-authentication/
		if ( token == null) {
			final String hdr		= request.getHeader("Authorization");
			if ( hdr != null) {
				final String temp[] = hdr.split(" ");
				if ( temp[0].equalsIgnoreCase("Bearer")) {
					token = temp.length > 1 ? temp[1] : null;
				}
			}
		}
		if ( token == null ) 	throw new SecurityException("Token required");
		
		try {
			JWT.verify(token, secret);
		} catch (InvalidKeyException e) {
			throw new SecurityException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException(e);
		}
    }
    
	/**
	 * Check request parts for content type: mulitpart/form-data (HTTP file upload INPUT type=file)
	 * @param request {@link HttpServletRequest}.
	 * @throws IOException if we get a suspicious parameter that may indicate a security violation.
	 */
	private void checkParts (HttpServletRequest request ) throws IOException, ServletException {
		final String contentType = request.getContentType();
		
		// Avoid javax.servlet.ServletException: org.apache.tomcat.util.http.fileupload.FileUploadBase$InvalidContentTypeException: the request doesn't contain a multipart/form-data or multipart/mixed stream, content type header is null
		if ( contentType == null ) {
			return;
		}
		// tomcat 8.x - ignore non multi part requests
		if ( !contentType.toLowerCase().contains("multipart")) {
			return;
		}
		
		for (Part part : request.getParts()) {
			final String name 		= part.getName();
			final String fileName	= FileUpload.extractFileName(part);
			
			// ignore files
			if ( fileName != null) {
				continue;
			}
			// This may take some time depending on the size.
			final String value 		= IOTools.readFromStream(part.getInputStream(), CoreTypes.ENCODING_UTF8);
			
			com.cloud.console.filter.OWASPValidator.validate(request.getRequestURI(), name, value, ElementType.PARAM);
		}
	}

	/**
	 * Check request parameters
	 * @param request {@link HttpServletRequest}.
	 * @throws IOException if we get a suspicious parameter that may indicate a security violation.
	 */
	private void checkParameters (HttpServletRequest request ) throws IOException {
		Enumeration<String> names =  request.getParameterNames();
		
		while ( names.hasMoreElements()) {
			final String name 		= names.nextElement();
			final String[] vals		= request.getParameterValues(name); // Multi-valued params: combos, lists, never null
			final boolean multiVal 	= vals.length > 1;
			String value 			= request.getParameter(name);		// single valued params
			
			if ( multiVal) {
				value = IOTools.join(vals, ",");
			}

			com.cloud.console.filter.OWASPValidator.validate(request.getRequestURI(), name, value, ElementType.PARAM);
		}
	}
	
	/**
	 * Fail first header check. 
	 * @param request {@link HttpServletRequest}.
	 * @throws IOException if there is an invalid header indication a security scan violation.
	 */
	private void checkHeaders (HttpServletRequest request ) throws IOException {
		Enumeration<String> names =  request.getHeaderNames();
		while ( names.hasMoreElements()) {
			final String name 	= names.nextElement();
			final String value 	= request.getHeader(name);

			com.cloud.console.filter.OWASPValidator.validate(request.getRequestURI(), name, value, ElementType.HEADER);			
		}
	}

	
}
