package junit.kubernetes;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.SecurityTool;
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
public class DockerHttpHijack1 {

    private static final Logger log = LogManager.getLogger(DockerHttpHijack1.class);
    
	static void LOGD(String text) {
		System.out.println("[DOCKER-HTTP-HIJACK] " + text);
		//log.debug(text);
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
	
	
    public DockerHttpHijack1(URI url) {
        uri = url;
    }

    /**
     * Start reading from the socket input stream in the background and writing in the stream provided.
     * @param ps Provides an output stream and tells the reader if it should be closed after each write. See {@link PrintStreamSource}.
     * @throws IOException on I/O errors.
     */
    public void pipeStdout (StreamIO1.PrintStreamSource ps) throws IOException {
    	if ( socket == null) {
    		throw new IOException("Socket can't be null");
    	}
    	final String id = "SOCKET-READER-" + uri;
    	reader = new Thread(new StreamIO1.InReader(id, socket.getInputStream(), ps), id);
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
    	service("HTTP/1.1", "POST", headers, payload, requireUpgrade, "tcp");
    }
    
    public void service(final String protocol, final String method, Map<String, String> headers, String payload, boolean requireUpgrade, final String upgradeProto) throws java.io.IOException {
    
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
        request.append(method + " " + path + " " + protocol + "\r\n");
        
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
        if ( requireUpgrade && !header.contains("HTTP/1.1 101") /*  !header.equals("HTTP/1.1 101 UPGRADED") */ ) {
            //FIXME throw new IOException("Invalid handshake response: " + header);
        }
        
    	LOGD(String.format("%s", header));
        do {
            header = reader.readLine();
            LOGD(String.format("RECV: %s", header));
        } 
        while (!header.equals(""));
        //while ( header != null);

        handshakeCompleted = true;
        LOGD("Handshake complete");
    }

    private Socket createSocket() throws java.io.IOException {
        String scheme = uri.getScheme();
        String host = uri.getHost();

        int port = uri.getPort();
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
//        		throw new IOException("HTTPS requires an SSL Socker factory.");
        		try {
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
        			return sc.getSocketFactory().createSocket(host, port);
				} catch (Exception e) {
					// TODO: handle exception
				}

//        		LOGD("Using default SSL Socket factory for scheme " + scheme + " host " + host + "/" + port);
//        		return SSLSocketFactory.getDefault().createSocket(host, port);
        	}
        	return dualSslSocketFactory.createSocket(host, port);
        } else {
            return new Socket(host, port);
        }
    }

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
    
	public void setSSLSocketfactory ( SSLSocketFactory factory) {
		dualSslSocketFactory = factory;
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
//        if ( !uri.getScheme().toLowerCase().equals("https")) {
//        	socket.shutdownOutput();
//        }
        //return socket.getInputStream();
    }
	
    public void send(byte [] buf) throws java.io.IOException {
        if (!handshakeCompleted) {
            throw new IllegalStateException("Handshake not complete");
        }
        //LOGD("SEND " + command);
        chout.write(buf);
        chout.flush();
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