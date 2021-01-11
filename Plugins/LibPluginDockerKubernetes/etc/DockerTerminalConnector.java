package com.cloud.cloud.docker;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import com.cloud.cloud.console.HTTPServerTools;
import com.cloud.cloud.core.logging.LogManager;
import com.cloud.cloud.core.logging.Logger;
import com.cloud.cloud.rest.AbstractRestException;


/**
 * Docker Terminal {@link WebSocketContainer}. <i>Note: A new instance of this class gets created for each WebSocket connection.</i>
 * <p>
 * This class listens for Docker WebSocket XTerm.js connections and invokes the command via the Docker exec API, then returns the stdout.
 * </p>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li> 03/18/2019 Initial commit
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 01/15/19
 */
@ServerEndpoint(value = "/DockerTerminal") 
public class DockerTerminalConnector {

	private static final Logger log = LogManager.getLogger(DockerTerminalConnector.class);

	static class WSConnectionDescriptor {
		String nodeId;
		String containerId;
		Session session;
		
		public WSConnectionDescriptor(String nodeId, String windowId, Session session) {
			super();
			this.nodeId = nodeId;
			this.containerId = windowId;
			this.session = session;
		}
		
		@Override
		public String toString() {
			// 11/5/2018 Not safe to be called on a closed session : session.getRequestURI()
			// java.lang.IllegalStateException: The WebSocket session [11] has been closed and no method (apart from close()) may be called on a closed session
			return String.format("%s/%s %s", nodeId, containerId, session.getRequestURI());
		}
		
		public String getId() {
			// Safe to be called on a closed session.
			return String.format("%s:%s", nodeId, containerId);
		}
	}
	
	/** ALL Remote connections of type {@link DockerTerminalConnector}. */
	private static final Set<WSConnectionDescriptor> connections = new CopyOnWriteArraySet<WSConnectionDescriptor>();

	/** The client id for this WS */
	String nodeId;
	
	/** The window id for this WS */
	String containerId;

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
		nodeId = getSessionParameter(session, "node");
		containerId = getSessionParameter(session, "containerId");
	
		
		if ( nodeId == null || nodeId.isEmpty() ) unicast(session, "Node Id is required.");
		if ( containerId == null || containerId.isEmpty() ) unicast(session, "Container Id is required.");
		
		/* 
		 * 11/20/2017 https://tomcat.apache.org/tomcat-7.0-doc/web-socket-howto.html
		 * The write timeout used when sending WebSocket messages in blocking mode defaults to 20000 milliseconds (20 seconds).
		 * http://acme208.acme.com:6091/issue/UNIFIED_CC-417
		 */
		session.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 5000); //1500);
		
		log.debug("WSOpen: " + nodeId + "/" + containerId + " " + session.getRequestURI());
		
		// no duplicates?
		WSConnectionDescriptor conn = findConnection(nodeId, containerId);
		if ( conn != null) {
			unicast(conn.session, "Rejected duplicate session.");
		}
		else {
			connections.add(new WSConnectionDescriptor(nodeId, containerId, session));
		}
		dumpConnections("ONOPEN " +  nodeId + "/" + containerId, true); 
	}

	@OnClose
	public void end() {
		if ( !connections.remove(findConnection(nodeId, containerId))) {
			log.error("Unable to remove WS connection for " + nodeId + "/" + containerId);
		}
		
		log.debug(String.format("WSClose: %s/%s has disconnected.", nodeId, containerId));
		dumpConnections(String.format("ONCLOSE - %s %s", nodeId, containerId), false);
	}

	@OnMessage
	public void incoming(String message) {
		log.debug("Message: " + message + " Node:" + nodeId + " Container:" + containerId);
		WSConnectionDescriptor cn = findConnection(nodeId, containerId);
		try {
			
			if ( cn == null) {
				System.err.println(String.format("Cannot find a session for %s/%s" + nodeId, containerId));
				return;
			} 
			final String stdout = Docker.execShellCommand(nodeId, containerId, message);
			unicast(cn.session, stdout);
			
		} catch (AbstractRestException ex) {
			String msg = DockerParams.parseServerErrorReponse(ex.getMessage());
			unicast(cn.session, HTTPServerTools.buildBaseResponse(ex.getHttpStatus(), msg).toString() );
		} catch (Exception e) {
			log.error("OnMessage", e);
			unicast(cn.session, HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
		}
	}
	
	@OnError
	public void onError(Throwable t) throws Throwable {
		/* CHROME
		 * java.io.IOException: java.util.concurrent.ExecutionException: java.net.SocketException: Software caused connection abort: socket write error
		 * 	at org.apache.tomcat.websocket.WsRemoteEndpointImplBase.startMessageBlock(WsRemoteEndpointImplBase.java:243)
		 * 	at org.apache.tomcat.websocket.WsSession.sendCloseMessage(WsSession.java:487)
		 */
		log.error("WSError: ", t);
	}

	WSConnectionDescriptor findConnection ( String nodeId, String containerId) {
		for ( WSConnectionDescriptor conn : connections) {
			if ( conn.nodeId.equals(nodeId) && conn.containerId.equals(containerId)) {
				return conn;
			}
		}
		return null;
	}

	private void dumpConnections(String label, boolean safeState) {
		int count 			= 0;
		StringBuffer buf 	= new StringBuffer();
		buf.append(" -- WS Connections " + label + " --\n");
		
		for ( WSConnectionDescriptor conn : connections) {
			buf.append("[" + (count++) + "] " + ( safeState ? conn.toString() : conn.getId() ) + "\n");
		}
		buf.append(" -- End WS Connections --");
		log.debug("<websocket>\n"  + buf.toString() + "</websocket>");
	}
}

