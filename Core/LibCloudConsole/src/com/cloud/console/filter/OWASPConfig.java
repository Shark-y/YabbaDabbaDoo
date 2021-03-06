package com.cloud.console.filter;

import java.util.regex.Pattern;


/**
 * Part of OWASP - https://www.owasp.org/index.php/Main_Page
 * WebApp security Configuration constants
 * 
 * @author VSilva
 * @see https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project
 *
 */
public class OWASPConfig {

	/** Request element type */
	public enum ElementType { HEADER, PARAM, URI };

	/**
	 * Class used to describe an OWASP regexp pattern.
	 * @author VSilva
	 *
	 */
	final static class PatternDescriptor {
		Pattern pattern;
		String types;
		
		/**
		 * Construct
		 * @param pattern OWASP regular expression.
		 * @param types Expression target type: HEADER, PARAM, URI, etc.
		 */
		public PatternDescriptor(Pattern pattern, String types) {
			super();
			this.pattern = pattern;
			this.types = types;
		}
		@Override
		public String toString() {
			return "[" + pattern.pattern() + "," + types + "]";
		}
	}
	
	/**
	 * Any Header/Parameter that matches any of these patterns will be rejected.
	 * Note: These apply to both Headers and Parameters.
	 */
	public final static Pattern[] GlobalRejectionPatterns = new Pattern[]{
        // Script fragments
        Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
        
        // src='...'
        Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        
        // lonely script tags
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        
        // eval(...)
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        
        // expression(...)
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        
        // javascript:...
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        
        // vbscript:...
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        
        // onload(...)=...
        // 10/17/2018 Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onload(.{0,6})=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        
        // vsilva 10/31/2016 Other stuff generated by OWASP ZAP
        Pattern.compile("onMouseOver", Pattern.CASE_INSENSITIVE),
        
        /* SQL Boolean conditions
         * The page results were successfully manipulated using the boolean conditions [agent_id:user_name" AND "1"="1" -- ] and [agent_id:user_name" AND "1"="2" -- ]
         * https://www.owasp.org/index.php/Top_10_2010-A1
         * https://www.owasp.org/index.php/SQL_Injection_Prevention_Cheat_Sheet
         */
        /* FIXME: REMOVED 11/10/2017 This is giving too many problems with WebSockets headers 
         * Potential ATTACK for parameter sec-websocket-key = HN/4JThNuOr+fXUyh4swfw== @ /CloudConnectorFinesse01/WSPhone Pattern:.*(AND|OR)[\+\s].*?=.*
         */
        //Pattern.compile(".*(AND|OR)[\\+\\s].*?=.*", Pattern.CASE_INSENSITIVE),
        
        // Server side includes: http://localhost:8080/CloudConnectorNode002/LogServlet?_=%3C%21--%23EXEC+cmd%3D%22dir+%5C%22--%3E
        Pattern.compile("EXEC[\\+\\s]", Pattern.CASE_INSENSITIVE),
        
    };

	/**
	 * These apply to parameters only. Any param that matches any of these will be rejected.
	 */
	final static PatternDescriptor[] ParamRejectionPatterns = new PatternDescriptor[] {
        // External redirect - http://localhost:8080/CloudConnectorNode002/login.jsp?action=login&r=http%3A%2F%2F8123978617866305754.owasp.org...
		new PatternDescriptor(Pattern.compile("http", Pattern.CASE_INSENSITIVE), "EXTERNAL_REDIRECT") ,
		new PatternDescriptor(Pattern.compile("//", Pattern.CASE_INSENSITIVE), "EXTERNAL_REDIRECT"),
    };


	/**
	 * Check if an {@link HttpServletRequest} parameter is to be excluded from a {@link PatternDescriptor} of a security scan.
	 * @param name Parameter name.
	 * @param pattern The {@link PatternDescriptor} for a particular security scan.
	 * @return True if parameter name is exempt from security scan pattern.
	 */
	/* 1/7/2017 This will punch a hole in the OWASP defeating the whole thing
	static boolean isParamExempt (String name, PatternDescriptor pattern) {
		return CloudSecurity.isParamExempt(name, pattern.types);
	} */
	
}
