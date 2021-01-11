package com.cloud.core.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
//import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;

import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.InitialLdapContext;
 
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
//Imports for changing password
/*import javax.naming.directory.ModificationItem;
import javax.naming.directory.BasicAttribute;
import javax.naming.ldap.StartTlsResponse;
import javax.naming.ldap.StartTlsRequest;
import javax.net.ssl.*;
*/

import com.cloud.core.net.Win32ActiveDirectory;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;


//******************************************************************************
//**  ActiveDirectory - see http://www.javaxt.com/wiki/Tutorials/Windows/How_to_Authenticate_Users_with_Active_Directory
//*****************************************************************************/
/**
 *   Provides static methods to authenticate users, change passwords, etc. 
 *
 ******************************************************************************/
 
public class Win32ActiveDirectory {
 
	private static final Logger log = LogManager.getLogger(Win32ActiveDirectory.class);
	
    private static String[] userAttributes = {
        "distinguishedName","cn","name","uid",
        "sn","givenname","memberOf","samaccountname",
        "userPrincipalName"
    };
 
    private Win32ActiveDirectory(){}
 
 
  //**************************************************************************
  //** getConnection
  //*************************************************************************/
  /**  Used to authenticate a user given a username/password. Domain name is
   *   derived from the fully qualified domain name of the host machine.
   */
    public static LdapContext getConnection(String username, String password) throws NamingException {
        return getConnection(username, password, null, null);
    }
 
 
  //**************************************************************************
  //** getConnection
  //*************************************************************************/
  /**  Used to authenticate a user given a username/password and domain name.
   */
    public static LdapContext getConnection(String username, String password, String domainName) throws NamingException {
        return getConnection(username, password, domainName, null);
    }
 
 
  //**************************************************************************
  //** getConnection
  //*************************************************************************/
  /** Used to authenticate a user given a username/password and domain name.
   *  Provides an option to identify a specific a Active Directory server.
   */
    public static LdapContext getConnection(String username, String password, String domainName, String serverName) throws NamingException {
    	if (username.contains("@")) {
    		String[] temp = username.split("@");
            username = temp[0];
            domainName = temp[1];
        }
        else if(username.contains("\\")){
    		String[] temp = username.split("\\\\");
            username = temp[0];
            domainName = temp[1];
        }
        else {
	        if (domainName==null){
	            try{
	                String fqdn = java.net.InetAddress.getLocalHost().getCanonicalHostName();
	                if (fqdn.split("\\.").length>1) domainName = fqdn.substring(fqdn.indexOf(".")+1);
	            }
	            catch(java.net.UnknownHostException e){}
	        }
        }
        //System.out.println("Authenticating user: " + username + " Domain:" + domainName + " Server: " + serverName);
 
        if (password!=null){
            password = password.trim();
            if (password.length() == 0) {
            	//password = null;
            	throw new NamingException("Password is required");
            }
        }
 
        //bind by using the specified username/password
        Hashtable<String, String> props = new Hashtable<String, String>();
        String principalName 			= username + "@"
        		// 5/17/2020 Linux clean principal domain: host.foo.com => foo.com, foo.com => foo.com
        		+ ( domainName.split("\\.").length > 2 
        				? domainName.substring(domainName.indexOf(".") + 1) 
        				: domainName);
        
        props.put(Context.SECURITY_PRINCIPAL, principalName);
        if (password!=null) props.put(Context.SECURITY_CREDENTIALS, password);
 
 
        String ldapURL = "ldap://" + ((serverName==null)? domainName : serverName + "." + domainName) + '/';
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, ldapURL);

        // vsilva: Specify timeout to be 5 seconds
        props.put("com.sun.jndi.ldap.connect.timeout", "5000");
        
        // vsilva https://docs.oracle.com/javase/jndi/tutorial/ldap/referral/jndi.html
        // fix for : javax.naming.PartialResultException: Unprocessed Continuation Reference(s);
        props.put(Context.REFERRAL, "follow");

        log.debug ("Win32 Connect LDAP props=" + props);
        try{
            return new InitialLdapContext(props, null);
        }
        catch(javax.naming.CommunicationException e){
            throw new NamingException("Failed to connect to " + domainName + ((serverName==null)? "" : " through " + serverName));
        }
        catch(NamingException e){
            throw new NamingException("Failed to authenticate " + username + "@" + domainName + ((serverName==null)? "" : " through " + serverName));
        }
    }
 
 
  //**************************************************************************
  //** getUser
  //*************************************************************************/
  /** Used to check whether a username is valid.
   *  @param username A username to validate (e.g. "peter", "peter@acme.com",
   *  or "ACME\peter").
   */
    /* 8/17/2019 This code is untested - test in needed and uncomment
    public static User getUser(String username, LdapContext context) {
        try{
            String domainName = null;
            if (username.contains("@")){
                username = username.substring(0, username.indexOf("@"));
                domainName = username.substring(username.indexOf("@")+1);
            }
            else if(username.contains("\\")){
                username = username.substring(0, username.indexOf("\\"));
                domainName = username.substring(username.indexOf("\\")+1);
            }
            else{
                String authenticatedUser = (String) context.getEnvironment().get(Context.SECURITY_PRINCIPAL);
                if (authenticatedUser.contains("@")){
                    domainName = authenticatedUser.substring(authenticatedUser.indexOf("@")+1);
                }
            }
 
            if (domainName!=null){
                String principalName = username + "@" + domainName;
                SearchControls controls = new SearchControls();
                controls.setSearchScope(SUBTREE_SCOPE);
                controls.setReturningAttributes(userAttributes);
                NamingEnumeration<SearchResult> answer = context.search( toDC(domainName), "(& (userPrincipalName="+principalName+")(objectClass=user))", controls);
                if (answer.hasMore()) {
                    Attributes attr = answer.next().getAttributes();
                    Attribute user = attr.get("userPrincipalName");
                    if (user!=null) return new User(attr);
                }
            }
        }
        catch(NamingException e){
            //e.printStackTrace();
        }
        return null;
    }
  */
 
  //**************************************************************************
  //** getUsers
  //*************************************************************************/
  /** Returns a list of users in the domain.
   * @throws IOException On search errors
   * @throws javax.naming.SizeLimitExceededException: [LDAP: error code 4 - Sizelimit Exceeded]
   */
    public static List<User> getUsers(LdapContext context) throws NamingException, IOException {
 
        List<User> users 			= new ArrayList<User>();
        String authenticatedUser 	= (String) context.getEnvironment().get(Context.SECURITY_PRINCIPAL);
        
        if (authenticatedUser.contains("@")){
            String domainName = authenticatedUser.substring(authenticatedUser.indexOf("@")+1);
            /*
             * vsilva Goota used paged results control - https://docs.oracle.com/javase/7/docs/api/javax/naming/ldap/PagedResultsControl.html
             * else with large results we get javax.naming.SizeLimitExceededException: [LDAP: error code 4 - Sizelimit Exceeded]; ...
             */
            /*
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SUBTREE_SCOPE);
            controls.setReturningAttributes(userAttributes);
            
            // Does not work controls.setCountLimit(1000); // max 100 results to avoid size exceptions
            NamingEnumeration answer = context.search( toDC(domainName), "(objectClass=user)", controls);

            while(answer.hasMore()) {
                Attributes attr = ((SearchResult) answer.next()).getAttributes();
                Attribute user = attr.get("userPrincipalName");
                
                if (user!=null){
                    users.add(new User(attr));
                }
            } */
           
            // Activate paged results
            int pageSize 	= 20; // 20 entries per page
            byte[] cookie 	= null;
            //int total;
            context.setRequestControls(new Control[]{ new PagedResultsControl(pageSize, Control.CRITICAL) });

            do {
                SearchControls scontrols = new SearchControls();
                scontrols.setSearchScope(SUBTREE_SCOPE);
                scontrols.setReturningAttributes(userAttributes);

            	// Perform the search
                NamingEnumeration results = context.search(toDC(domainName), "(objectclass=user)", scontrols);

                // Iterate over a batch of search results
                while (results != null && results.hasMore()) {
                    // Display an entry
                    SearchResult entry = (SearchResult)results.next();
                    //System.out.println(entry.getName());
                    //System.out.println(entry.getAttributes());
                    Attributes attr = entry.getAttributes();
                    Attribute user = attr.get("userPrincipalName");
                    
                    if (user != null){
                        users.add(new User(attr));
                    }

                    // Handle the entry's response controls (if any)
                    //if (entry instanceof HasControls) {
                        // ((HasControls)entry).getControls();
                    //}
                }
                // Examine the paged results control response
                Control[] controls = context.getResponseControls();
                if (controls != null) {
                    for (int i = 0; i < controls.length; i++) {
                        if (controls[i] instanceof PagedResultsResponseControl) {
                            PagedResultsResponseControl prrc = (PagedResultsResponseControl)controls[i];
                            //total = prrc.getResultSize();
                            cookie = prrc.getCookie();
                        } else {
                            // Handle other response controls (if any)
                        }
                    }
                }
                
                // Re-activate paged results
                context.setRequestControls(new Control[]{ new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });
            } while (cookie != null);            
        }
        return users; //.toArray(new User[users.size()]);
    }
 
 
    private static String toDC(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if(token.length()==0)   continue;   // defensive check
            if(buf.length()>0)  buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }
 
 
  //**************************************************************************
  //** User Class
  //*************************************************************************/
  /** Used to represent a User in Active Directory
   */
    public static class User {
        private String distinguishedName;
        private String userPrincipal;
        private String commonName;
        
        // {name=name: Suzanne Boehlefeld, givenname=givenName: Suzanne, samaccountname=sAMAccountName: sboehlefeld, memberof=memberOf: CN=Team Call Field,OU=Distribution,OU=Groups,OU=NACR,DC=acme,DC=com, CN=DocuSign Users,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=Avaya-PS-PMO,OU=Distribution,OU=Groups,OU=NACR,DC=acme,DC=com, CN=Duo Users,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=Sales_Central_Bailey,OU=L3,OU=Distribution,OU=Groups,OU=NACR,DC=acme,DC=com, CN=Client Computer Backup,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=SP_C1E_Central_PMs,OU=Security Sharepoint,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=Web_Ex_Users,OU=Distribution,OU=Groups,OU=NACR,DC=acme,DC=com, CN=VPN_Phone_Users,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=AVST_VM,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=AlertUsers,OU=Distribution,OU=Groups,OU=NACR,DC=acme,DC=com, CN=SSRS GROUP_PMs,OU=Security SSRS,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=test dialin,OU=Distribution,OU=Groups,OU=TEST,DC=acme,DC=com, CN=MeetingExchangeUsers,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=PLMUSERS,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=DS_Design Folder Full,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=PLMSERVICESONLY,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=NACR_All,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=ReportingGroup {4fdecf36-de53-4081-aa0e-ccdffdef9e02},DC=acme,DC=com, CN=Citrix Project Users,OU=Security,OU=Groups,OU=NACR,DC=acme,DC=com, CN=DialInUsers,OU=Security Groups,OU=Groups,DC=acme,DC=com, userprincipalname=userPrincipalName: sboehlefeld@convergeone.com, sn=sn: Boehlefeld, distinguishedname=distinguishedName: CN=Suzanne Boehlefeld,OU=Users,OU=NACR,DC=acme,DC=com, cn=cn: Suzanne Boehlefeld}
        public User(Attributes attr) throws javax.naming.NamingException {
            userPrincipal = (String) attr.get("userPrincipalName").get();
            commonName = (String) attr.get("cn").get();
            distinguishedName = (String) attr.get("distinguishedName").get(); 
        }
 
        public String getUserPrincipal(){
            return userPrincipal;
        }
 
        public String getCommonName(){
            return commonName;
        }
 
        public String getDistinguishedName(){
            return distinguishedName;
        }
 
        public String toString(){
            return userPrincipal;
        }
 
      /** Used to change the user password. Throws an IOException if the Domain
       *  Controller is not LDAPS enabled.
       *  @param trustAllCerts If true, bypasses all certificate and host name
       *  validation. If false, ensure that the LDAPS certificate has been
       *  imported into a trust store and sourced before calling this method.
       *  Example:
          String keystore = "/usr/java/jdk1.5.0_01/jre/lib/security/cacerts";
          System.setProperty("javax.net.ssl.trustStore",keystore);
       */
        /* 8/17/2019 Disabled - enabled if required - need to be tested
        public void changePassword(String oldPass, String newPass, boolean trustAllCerts, LdapContext context) 
        throws java.io.IOException, NamingException {
            String dn = getDistinguishedName();
 
 
          //Switch to SSL/TLS
            StartTlsResponse tls = null;
            try{
                tls = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
            }
            catch(Exception e){
                //"Problem creating object: javax.naming.ServiceUnavailableException: [LDAP: error code 52 - 00000000: LdapErr: DSID-0C090E09, comment: Error initializing SSL/TLS, data 0, v1db0"
                throw new java.io.IOException("Failed to establish SSL connection to the Domain Controller. Is LDAPS enabled?");
            }
 
 
          //Exchange certificates
            if (trustAllCerts){
                tls.setHostnameVerifier(DO_NOT_VERIFY);
                SSLSocketFactory sf = null;
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, TRUST_ALL_CERTS, null);
                    sf = sc.getSocketFactory();
                }
                catch(java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
                catch(java.security.KeyManagementException e) { throw new IOException(e); }
                tls.negotiate(sf);
            }
            else{
                tls.negotiate();
            }
 
 
          //Change password
            try {
                //ModificationItem[] modificationItems = new ModificationItem[1];
                //modificationItems[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", getPassword(newPass)));
 
                ModificationItem[] modificationItems = new ModificationItem[2];
                modificationItems[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("unicodePwd", getPassword(oldPass)) );
                modificationItems[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("unicodePwd", getPassword(newPass)) );
                context.modifyAttributes(dn, modificationItems);
            }
            catch(javax.naming.directory.InvalidAttributeValueException e){
                String error = e.getMessage().trim();
                if (error.startsWith("[") && error.endsWith("]")){
                    error = error.substring(1, error.length()-1);
                }
                System.err.println(error);
                //e.printStackTrace();
                tls.close();
                throw new NamingException(
                    "New password does not meet Active Directory requirements. " +
                    "Please ensure that the new password meets password complexity, " +
                    "length, minimum password age, and password history requirements."
                );
            }
            catch(NamingException e) {
                tls.close();
                throw e;
            }
 
          //Close the TLS/SSL session
            tls.close();
        }
 		*
        private static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
 
        private static TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
        };
 
 
        private byte[] getPassword(String newPass){
            String quotedPassword = "\"" + newPass + "\"";
            //return quotedPassword.getBytes("UTF-16LE");
            char unicodePwd[] = quotedPassword.toCharArray();
            byte pwdArray[] = new byte[unicodePwd.length * 2];
            for (int i=0; i<unicodePwd.length; i++) {
                pwdArray[i*2 + 1] = (byte) (unicodePwd[i] >>> 8);
                pwdArray[i*2 + 0] = (byte) (unicodePwd[i] & 0xff);
            }
            return pwdArray;
        } */
    }

    /**
     * Attempt to guess the local domain name using InetAddress.getLocalHost().getCanonicalHostName()
     * @return The domain name or NULL if getCanonicalHostName() gives no domain.
     */
	public static String getDomainName () {
        try{
            String fqdn = java.net.InetAddress.getLocalHost().getCanonicalHostName();
            if (fqdn.split("\\.").length> 1 ) {
            	return fqdn.substring(fqdn.indexOf(".") + 1);
            }
        }
        catch(java.net.UnknownHostException e) {}
        return null;
	}

	/**
	 * Try to login a user to the local windows domain
	 * @param user User name.
	 * @param pwd Password.
	 * @throws SecurityException If login failed (look at the cause for details). For example: javax.naming.NamingException: Failed to authenticate USER@DOMAIN.
	 */
	public static void login (final String user, final String pwd) throws SecurityException {
		try {
			String username = null;
			String domainName = null;
            if (user.contains("@")){
            	username = user.substring(0, user.indexOf("@"));
            	domainName = user.substring(user.indexOf("@")+1);
            }
            else if(user.contains("\\")){
            	domainName  = user.substring(0, user.indexOf("\\"));
                username = user.substring(user.indexOf("\\")+1);
            }
            else {
            	username = user;
            }
            /* incomplete domain may fail!
            if ( domainName != null && !domainName.contains(".")) {
            	domainName = null;
            } */
 		    LdapContext ctx = Win32ActiveDirectory.getConnection(username, pwd, domainName);
		    ctx.close();
		}
		catch(Exception e) {
			throw new SecurityException(e.getCause() != null ? e.getCause() : e);
		}		
	}

}