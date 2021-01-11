package com.cloud.docker;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.rest.Security;
import com.cloud.ssh.StreamIO;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An implementation of the Docker Hijack HTTP protocol taken from https://gist.github.com/missedone/76517e618486db746056
 * <pre>
 * String Url = "http://192.168.42.248:2375/exec/{ID}/start";
 * String payload = "{ \"Detach\": false, \"Tty\": true}";
 * DockerHttpHijack http = new DockerHttpHijack(new URI(Url));
 * http.setSSLParams("TLS", keyStorePath, keyStorePassword, true);
 * 
 * Map<String, String> headers = new HashMap<String, String>();
 * headers.put("Content-Type",  CoreTypes.CONTENT_TYPE_JSON);
 * 
 * http.post(headers, payload);
 * http.pipeStdout(new PrintStreamSource() {
 *  public PrintStream getPrintStream() {
 *    return System.out;
 *  }
 *  public boolean closeAfterWrite() {
 *    return false;
 *  }
 * });
 * http.send("ls- -l");
 * http.send("uname -a");
 * </pre>
 * 
 * @author VSilva
 * 
 *
 */
public class DockerHttpHijack {

    private static final Logger log = LogManager.getLogger(DockerHttpHijack.class);
    
	static void LOGD(String text) {
		//System.out.println("[DOCKER-HTTP-HIJACK] " + text);
		log.debug(text);
	}
	/** request URI */
    private URI uri;

    /** Requet/response Socket */
    private Socket socket;

    private boolean handshakeCompleted;

    /* socket streams */
    private InputStream chin;

    private OutputStream chout;

	/** For 2-way SSL client certificate */
	protected SSLSocketFactory dualSslSocketFactory;

	/** used to read from the ipnput asynchronously */
	private Thread reader;
	
	
    public DockerHttpHijack(URI url) {
        uri = url;
    }

    /**
     * Start reading from the socket input stream in the background and writing in the stream provided.
     * @param ps Provides an output stream and tells the reader if it should be closed after each write. See {@link PrintStreamSource}.
     * @throws IOException on I/O errors.
     */
    public void pipeStdout (StreamIO.PrintStreamSource ps) throws IOException {
    	if ( socket == null) {
    		throw new IOException("Socket can't be null");
    	}
    	final String id = "SOCKET-READER-" + uri;
    	reader = new Thread(new StreamIO.InReader(id, socket.getInputStream(), ps), id);
    	reader.start();
    }
    
    void shutdownThreads () {
    	if ( reader != null) {
    		reader.interrupt();
    		try {
				reader.join(1000);
			} catch (InterruptedException e) {
			}
    	}
    }
    
    /**
     * Start the handshake before reading from the socket. Optionally upgrade the HTTP/1.1 Connection to a WebSocket (see the upgrade mechanism).
     * <h2>Request</h2>
     * <pre>Upgrade: tcp
     * Connection: Upgrade
     * Host: 192.168.99.100
     * Content-Type: application/json; charset=utf-8
     * Content-Length: 31
     * 
     * { "Detach": false, "Tty": true}</pre>
     * <h2>Response</h2>
     * <pre>Content-Type: application/vnd.docker.raw-stream
     * Connection: Upgrade
     * Upgrade: tcp
     * Api-Version: 1.38
     * Docker-Experimental: false
     * Ostype: linux
     * Server: Docker/18.06.1-ce (linux)</pre>
     * 
     * At this point, invoke pipeStdout() to start reading from the socket and writing to the output stream.
     * 
     * @param headers Optional request headers.
     * @param payload Usually { "Detach": false, "Tty": true}
     * @param requireUpgrade If true, check for the "Protocol upgrade mechanism" using the Upgrade header field. See https://developer.mozilla.org/en-US/docs/Web/HTTP/Protocol_upgrade_mechanism
     * 
     * @throws java.io.IOException On I/O errors.
     */
    public void post(Map<String, String> headers, String payload, boolean requireUpgrade) throws java.io.IOException {
    	service("POST", headers, payload, requireUpgrade, "tcp");
    }
    
    /**
     * Service method capable of executing any HTTP operation.
     * @param method One of GET, POST, etc.
     * @param headers The HTTP headers.
     * @param payload The HTTP request content payload.
     * @param requireUpgrade If true send the WebSocket protocol upgrade headers. See https://developer.mozilla.org/en-US/docs/Web/HTTP/Protocol_upgrade_mechanism
     * @param upgradeProto Protocol to upgrade to: tcp or websocket.
     * @throws java.io.IOException When a HTTP request error occurs.
     */
    public void service(final String method, Map<String, String> headers, String payload, boolean requireUpgrade, final String upgradeProto) throws java.io.IOException {
    	
        String host = uri.getHost();
        String path = uri.getPath();
        if (path.equals("")) {
            path = "/";
        }

        String query = uri.getQuery();
        if (query != null) {
            path = path + "?" + query;
        }

        socket 						= createSocket();
        chout 						= socket.getOutputStream();
        StringBuffer extraHeaders 	= new StringBuffer();

        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                extraHeaders.append(entry.getKey() + ": " + entry.getValue() + "\r\n");
            }
        }

        StringBuffer request = new StringBuffer();
        request.append(method + " " + path + " HTTP/1.1\r\n");
        
        if ( requireUpgrade) {
        	request.append("Upgrade: " + upgradeProto + "\r\n");
        	request.append("Connection: Upgrade\r\n");
        }
        request.append("Host: " + host + "\r\n");

        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                request.append(entry.getKey() + ": " + entry.getValue() + "\r\n");
            }
        }

        request.append("Content-Length: " + payload.length() + "\r\n");
        request.append("\r\n");
        request.append(payload);
        
        LOGD("REQUEST " + request.toString());
        
        chout.write(request.toString().getBytes());
        chout.flush();

        // read response
        chin 					= socket.getInputStream();
        BufferedReader reader 	= new BufferedReader(new InputStreamReader(chin));
        String header 			= reader.readLine();
        
        // HTTP/1.1 200 OK when Detach : true
        // HTTP/1.1 101 Switching Protocols
        // HTTP/1.1 101 UPGRADED
        if ( requireUpgrade && !header.contains("HTTP/1.1 101") /* 6/17/2019 !header.equals("HTTP/1.1 101 UPGRADED") */ ) {
            throw new IOException("Invalid handshake response: " + header);
        }

        do {
            header = reader.readLine();
            LOGD(String.format("Response line: %s", header));
        } 
        while (!header.equals(""));
        //while ( header != null);

        handshakeCompleted = true;
        LOGD("Handshake complete");
    }

    private Socket createSocket() throws java.io.IOException {
        final String scheme 	= uri.getScheme();
        final String host 		= uri.getHost();
        int port 				= uri.getPort();
        
        if (port == -1) {
            if (scheme.equals("https")) {
                port = 443;
            } else if (scheme.equals("http")) {
                port = 80;
            } else {
                throw new IllegalArgumentException("Unsupported scheme");
            }
        }

        if (scheme.equals("https")) {
            //SocketFactory factory = SSLSocketFactory.getDefault();
            //return factory.createSocket(host, port);
        	//return createAllTrustingSSLSocket(host, port);
        	//return dualSslSocketFactory != null ? dualSslSocketFactory.createSocket(host, port) : createAllTrustingSSLSocket(host, port);
        	if ( dualSslSocketFactory == null) {
        		// 6/17/2019 throw new IOException("Docker over SSL requres a client (certificate, key) pair Java keystore.");
        		try {
					return Security.createAllTrustingSocketFactory().createSocket(host, port);
				} catch (Exception e) {
					throw new IOException(e);
				}
        	}
        	return dualSslSocketFactory.createSocket(host, port);
        } else {
            return new Socket(host, port);
        }
    }

    /**
     * Create a default all trusting socket factory using TLSv1.2 by default.
     * @return A {@link SocketFactory} that ignores self-signed certificates.
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    /* 6/20/2019 Moved to Security.java for resusability.
    private SocketFactory createDefaultSocketFactory () throws NoSuchAlgorithmException, KeyManagementException {
    	// This will avoid SSL exception when using self signed certificates.
		TrustManager[] trustMgr = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}};

		SSLContext sc = SSLContext.getInstance("TLSv1.2"); 
		sc.init(null, trustMgr, null); 
		return sc.getSocketFactory();
    } */
    
    /**
     * Required for Docker dual SSL client certificate/key
     * @param sslProto Use TLS.
     * @param keyStorePath Path to the java key store containing the client cet/key pair.
     * @param storePwd Key store password.
     * @param trustAllCerts If true ignore self-signed certificates.
     * @throws Exception
     */
	public synchronized void setSSLParams (String sslProto, String keyStorePath, String storePwd, final boolean trustAllCerts) throws Exception {
		String format 			= KeyStore.getDefaultType(); 
		KeyManagerFactory kmf 	= KeyManagerFactory.getInstance("SunX509");
		
		// if null, it will use the JRE default CA keystore
		TrustManager[] trustMgr = null;
		
		// Fix for:  sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path 
		if ( trustAllCerts) {
			trustMgr = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}};
		}
		
		// CLIENT load key store
		KeyStore ksClient 		= KeyStore.getInstance(format); 
		InputStream fis			= null;
		try {
			fis					= new FileInputStream(keyStorePath);
			ksClient.load(fis, storePwd.toCharArray());
		}
		finally {
			IOTools.closeStream(fis);
		}
		// init keys & trust certs to the same KS
		kmf.init(ksClient, storePwd.toCharArray());

		// TLSv1 | TLSv1.1 | TLSv1.2
		SSLContext sc = SSLContext.getInstance(sslProto); 
		sc.init(kmf.getKeyManagers(), trustMgr , null); 
	
		dualSslSocketFactory = sc.getSocketFactory();
	}
    
    /**
     * Send a command thru the socket. The background reader will pipe the response to the output stream
     * @param command
     * @return
     * @throws java.io.IOException
     */
    public /*InputStream*/ void send(String command) throws java.io.IOException {
        if (!handshakeCompleted) {
            throw new IllegalStateException("Handshake not complete");
        }
        //LOGD("SEND " + command);
        chout.write(command.getBytes("UTF-8"));
        chout.flush();

        // looks like "exit" can't explicitly close the session,
        // shutdown output stream to force close it
        // so that stdout/stderr can be consumed via inputstream
        /* if ( !uri.getScheme().toLowerCase().equals("https")) {
        	socket.shutdownOutput();
        }
        return socket.getInputStream(); */
    }
	
    
    public void close() throws java.io.IOException {
    	if ( chin != null) {
    		chin.close();
    	}
    	if ( chout != null) {
    		chout.close();
    	}
    	if ( socket != null) {
    		socket.close();
    	}
    	shutdownThreads();
    }

}