package com.cloud.docker;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
//import com.cloud.docker.DockerHttpHijack.PrintStreamSource;
import com.cloud.core.w3.RestException;
import com.cloud.ssh.StreamIO;


/**
 * Docker Terminal {@link WebSocketContainer}. <i>Note: A new instance of this class gets created for each WebSocket connection.</i>
 * <p>
 * This class listens for Docker WebSocket XTerm.js connections and invokes the command via the Docker exec API, then returns the stdout.
 * </p>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li> 03/18/2019 Initial commit.
 * <li> 03/29/2019 Set TLSv1.2 for HttpHijack to fix javax.net.ssl.SSLException: Received fatal alert: protocol_version
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 01/15/19
 */
@ServerEndpoint(value = "/DockerTerminal") 
public class DockerTerminalConnector {

	private static final Logger log = LogManager.getLogger(DockerTerminalConnector.class);

	/** The client id for this WS */
	String nodeId;
	
	/** The window id for this WS */
	String containerId;

	/** Websocket session */
	Session session;
	
	/** For interactive shells */
	DockerHttpHijack http;
	
	/**
	 * Construct: Note - the container will create 1 instance of this class for every WS client.
	 */
	public DockerTerminalConnector() {
	}
	
	private String getSessionParameter (Session session, String key) {
		if ( ! session.getRequestParameterMap().containsKey(key)) {
			return null;
		}
		return session.getRequestParameterMap().get(key).get(0);	
	}
	
	
	/**
	 * Send a message to a give WS {@link Session}.
	 * @param session The WS {@link Session}.
	 * @param message The message.
	 * @return true if the message was dispatched successfully else false.
	 */
	static boolean unicast (Session session, String message) {
		// java.lang.IllegalStateException: The WebSocket session has been closed and no method (apart from close()) may be called on a closed session
		try {
			if ( session.isOpen()) {
				session.getBasicRemote().sendText(message);
				return true;
			}
			else {
				log.warn("Session closed: Failed to dispatch " + message);
				return false;
			}
		} catch (IOException e) {
			log.error("Unicast ", e);
			return false;
		}
	}
	
	@OnOpen
	public void open(Session session) {
		nodeId 				= getSessionParameter(session, "node");
		containerId 		= getSessionParameter(session, "containerId");
		boolean attach 		= getSessionParameter(session, "attach") != null;
		final String shell	= getSessionParameter(session, "shell");
		
		if ( nodeId == null || nodeId.isEmpty() ) unicast(session, "Node Id is required.");
		if ( containerId == null || containerId.isEmpty() ) unicast(session, "Container Id is required.");
		
		/* 
		 * 11/20/2017 https://tomcat.apache.org/tomcat-7.0-doc/web-socket-howto.html
		 * The write timeout used when sending WebSocket messages in blocking mode defaults to 20000 milliseconds (20 seconds).
		 * http://acme208.acme.com:6091/issue/UNIFIED_CC-417
		 */
		session.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 5000); //1500);
		
		log.debug("WSOpen: " + nodeId + "/" + containerId + " " + session.getRequestURI());
		
		this.session = session;
		if ( attach ) {
			attach( shell != null ? shell : "sh"); //"/bin/bash");
		}
	}

	/**
	 * Attach terminal to container with a given shell.
	 * @param shell Shell name e.g. /bin/bash) Defaults to sh 
	 */
	private void attach (final String shell) {
		try {
			DockerNode node 		= Docker.get(nodeId);
			final String execId 	= Docker.execCreate(nodeId, containerId, shell);
			final String url 		= node.buildBaseUrl() + "exec/" + execId + "/start";
			final String payload 	= "{ \"Detach\": false, \"Tty\": true}";
			
			log.debug("Attach to node " + node + " with shell " + shell+  " Container: " + containerId);
			
			http 					= new DockerHttpHijack(new URI(url));
			if ( node.tlsEnabled) {
				// Use TLSv1.2 to fix javax.net.ssl.SSLException: Received fatal alert: protocol_version
				http.setSSLParams("TLSv1.2", node.keyStorePath, node.keyStorePassword, true);
			}
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type",  CoreTypes.CONTENT_TYPE_JSON);

			http.post(headers, payload, true);
			http.pipeStdout(new StreamIO.PrintStreamSource () {
				@Override
				public PrintStream getPrintStream() throws IOException {
					//return System.out;
					return new PrintStream(session.getBasicRemote().getSendStream());
				}

				@Override
				public boolean closeAfterWrite() {
					return true;
				}
			} );
		} 
		catch (Exception e) {
			log.error("Attach " + nodeId + "/" + containerId, e);
			try {
				session.close(new CloseReason(CloseCodes.PROTOCOL_ERROR, e.toString()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@OnClose
	public void end() {
		log.debug("Closing socket to " + nodeId + "/" + containerId);
		
		if ( http != null) {
			try {
				http.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@OnMessage
	public void incoming(String message) {
		//System.out.println("Message: " + message + " Node:" + nodeId + " Container:" + containerId + " attached:" + (http != null));
		// Hijacked
		if ( http != null) {
			try {
				http.send(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		try {
			final String stdout = Docker.execShellCommand(nodeId, containerId, message);
			unicast(/*cn.*/session, stdout);
			
		} catch (RestException ex) {
			String msg = DockerParams.parseServerErrorReponse(ex.getMessage());
			unicast(/*cn.*/session, HTTPServerTools.buildBaseResponse(ex.getHttpStatus(), msg).toString() );
		} catch (Exception e) {
			log.error("OnMessage", e);
			unicast(/*cn.*/session, HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
		}
	}
	
	@OnError
	public void onError(Throwable t) throws Throwable {
		log.error("WSError: ", t);
	}

}

