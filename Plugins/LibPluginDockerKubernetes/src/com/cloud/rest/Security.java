package com.cloud.rest;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Security {

	/**
	 * Fix for javax.net.ssl.SSLException by setting https.protocols = TLSv1,TLSv1.1,TLSv1.2 <pre>ClientHello, TLSv1
	 * main, READ: TLSv1 Alert, length = 2
	 * main, RECV TLSv1 ALERT:  fatal, protocol_version
	 * javax.net.ssl.SSLException: Received fatal alert: protocol_version</pre>
	 */
	public static void fixSSLFatalProtocolVersion () {
		// USE TLS v0,v1,v2
		java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
	}

	/**
     * Create a default all trusting socket factory using TLSv1.2 by default. Use this factory to fix
     * <ul>
     * <li> SSL "protocol version" errors.
     * <li> Ignore self-signed certificates.
     * </ul>
     * @return A {@link SocketFactory} that ignores self-signed certificates.
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
	public static  SSLSocketFactory createAllTrustingSocketFactory () throws NoSuchAlgorithmException, KeyManagementException {
    	// This will avoid SSL exception when using self signed certificates.
		TrustManager[] trustMgr = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[]{};
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}};

		SSLContext sc = SSLContext.getInstance("TLSv1.2"); 
		sc.init(null, trustMgr, null); 
		return sc.getSocketFactory();
    }

}
