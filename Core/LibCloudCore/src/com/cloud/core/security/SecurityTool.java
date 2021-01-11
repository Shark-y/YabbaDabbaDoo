package com.cloud.core.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Miscellaneous SSL helper methods.
 * 
 * @author VSilva
 * @version 1.0.1 - 2/12/2017
 * @version 1.0.2 - 3/23/2017 - Added SSL protocol names to static methods.
 *
 */
public class SecurityTool {

	/**
	 * If the system date of the server is altered to match the Genesys demo server then the
	 * <pre>RNT SOAP API Will throw:
	 * 		org.apache.axis2.AxisFault: Connection has been shutdown: 
	 * 			javax.net.ssl.SSLHandshakeException: 
	 * 			sun.security.validator.ValidatorException: 
	 * 			PKIX path validation failed: 
	 * 			java.security.cert.CertPathValidatorException: timestamp check failed
	 * 		....</pre>
	 * Call this to set a default SSLContext that ignores everything (This should be unsafe and only
	 * used against the Genesys demosrv)
	 * @since 1.0.0
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public static void useUntrustedSSLContext () throws KeyManagementException, NoSuchAlgorithmException {
		TrustManager tm = new javax.net.ssl.X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
			
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		};
	    
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, new TrustManager[]{tm}, new java.security.SecureRandom());
		SSLContext.setDefault(sc);
	}

	/**
	 * Set the {@link SSLContext} to use TLS v1.2. Usefult for SSL APIs that use their own Socket factories.
	 * Use this to fix SSL handshake_failure in SOAP calls.
	 * <p>For Example: Axis uses its own Socket factory (JSSESocketFactory.java) so this will NOT work -Dhttps.protocols=TLSv1.2 (Applies to HTTPSURLConnection)
	 *  <pre>faultDetail: 
	{http://xml.apache.org/axis/}stackTrace:javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
	at sun.security.ssl.Alerts.getSSLException(Unknown Source)
	at sun.security.ssl.Alerts.getSSLException(Unknown Source)
	at sun.security.ssl.SSLSocketImpl.recvAlert(Unknown Source)
	at sun.security.ssl.SSLSocketImpl.readRecord(Unknown Source)
	at sun.security.ssl.SSLSocketImpl.performInitialHandshake(Unknown Source)
	at sun.security.ssl.SSLSocketImpl.startHandshake(Unknown Source)
	at sun.security.ssl.SSLSocketImpl.startHandshake(Unknown Source)
	at org.apache.axis.components.net.JSSESocketFactory.create(JSSESocketFactory.java:186)
	at org.apache.axis.transport.http.HTTPSender.getSocket(HTTPSender.java:191) </pre>
	 * @since 1.0.0
	 * @throws Exception
	 */
	public static void useTLSV12() throws Exception {
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
	    sslContext.init(null, null, null);
	    SSLContext.setDefault(sslContext);		
	}
	
	/**
	 * Fixes the “java.security.cert.CertificateException: No subject alternative names present” error in SSL clients?
	 * <ul>
	 * <li>Disable SSL host name verifier
	 * <li>Trust self signed SSL certs.
	 * </ul>
	 * @see http://stackoverflow.com/questions/19540289/how-to-fix-the-java-security-cert-certificateexception-no-subject-alternative
	 * <pre>
	 * javax.net.ssl.SSLHandshakeException: java.security.cert.CertificateException: No subject alternative DNS name matching localhost found.
	 * 	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	 *  at sun.reflect.NativeConstructorAccessorImpl.newInstance(Unknown Source)
	 *  at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(Unknown Source)
	 *  
	 * java.security.cert.CertificateException: No subject alternative DNS name matching localhost found.
	 * 	at sun.security.util.HostnameChecker.matchDNS(Unknown Source)
	 * at sun.security.util.HostnameChecker.match(Unknown Source)
	 * </pre>
	 * @param protocol The SSL protocol. If NULL then SSL will be used.
	 * @since 1.0.2
	 */
	public static void disableClientSSLVerificationFromHttpsURLConnection(String protocol) throws NoSuchAlgorithmException, KeyManagementException {
		if ( protocol == null ) {
			protocol = "SSL";
		}
		
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance(protocol);
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	/**
	 * Fixes “java.security.cert.CertificateException: No subject alternative names present” error in SSL clients?
	 * <ul>
	 * <li>Disable SSL host name verifier
	 * <li>Trust self signed SSL certs.
	 * </ul>
	 * @see http://stackoverflow.com/questions/19540289/how-to-fix-the-java-security-cert-certificateexception-no-subject-alternative
	 * <pre>
	 * javax.net.ssl.SSLHandshakeException: java.security.cert.CertificateException: No subject alternative DNS name matching localhost found.
	 * 	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	 *  at sun.reflect.NativeConstructorAccessorImpl.newInstance(Unknown Source)
	 *  at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(Unknown Source)
	 *  
	 * java.security.cert.CertificateException: No subject alternative DNS name matching localhost found.
	 * 	at sun.security.util.HostnameChecker.matchDNS(Unknown Source)
	 * at sun.security.util.HostnameChecker.match(Unknown Source)
	 * </pre>
	 * 
	 * @since 1.0.1
	 */
	public static void disableClientSSLVerificationFromHttpsURLConnection() throws NoSuchAlgorithmException, KeyManagementException {
		disableClientSSLVerificationFromHttpsURLConnection(null);
	}
	
	/**
	 * Fix for Invalid TLS version - javax.net.ssl.SSLException by setting the SYSTEM VAR https.protocols = TLSv1,TLSv1.1,TLSv1.2 <pre>ClientHello, TLSv1
	 * main, READ: TLSv1 Alert, length = 2
	 * main, RECV TLSv1 ALERT:  fatal, protocol_version
	 * javax.net.ssl.SSLException: Received fatal alert: protocol_version</pre>
	 * @since 2/26/2020
	 */
	public static void fixSSLFatalProtocolVersion () {
		// USE TLS v0,v1,v2
		java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
	}

}
