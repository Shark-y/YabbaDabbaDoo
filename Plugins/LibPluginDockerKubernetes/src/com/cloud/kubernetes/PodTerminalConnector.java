package com.cloud.kubernetes;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.rest.Security;

/**
 * WebSocket connector for a Web terminal to a pod/container tuple via WebSocket chains.
 * <ul>
 * <li> It connects to the API server via another websocket client.
 * <li> Receives text messages from the XTerm WS client and sends binary msgs to the API server.
 * <li> API server messages sent and received are of type (BINARY) where the 1st byte[0] is the stram type 0 = stdin, 1 = stdout, 2 = stderr
 * <li> API server messages are multiplexed BINARY. See https://blog.openshift.com/executing-commands-in-pods-using-k8s-api/
 * </ul>
 * 
 * @author vsilva
 * @version 1.0.0
 *
 */
@ServerEndpoint(value = "/PodTerminal")
public class PodTerminalConnector {
	
	private static final Logger log = LogManager.getLogger(PodTerminalConnector.class);

	
	/** The client id for this WS */
	String nodeId;
	
	/** The container id for this WS */
	String containerId;

	/** A namespace is required */
	String namespace;

	/** A pod is required */
	String pod;
	
	/** Websocket session */
	Session session;

	/**
	 * WS client to the K8S API server using the super cool WebSocket-1.4.0.jar.
	 * This clients must be capable of setting custom HTTP headers for authentication, etc.
	 * 
	 * @author VSilva
	 *
	 */
	public class ApiServerClient extends WebSocketClient {

		public ApiServerClient( URI serverUri , Draft draft ) {
			super( serverUri, draft );
		}

		public ApiServerClient( URI serverURI ) {
			super( serverURI );
		}

		public ApiServerClient( URI serverUri, Map<String, String> httpHeaders ) {
			super(serverUri, httpHeaders);
		}

		@Override
		public void onOpen( ServerHandshake handshakedata ) {
			//System.out.println( "opened connection" );
			// if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
		}

		@Override
		public void onMessage( String message ) {
			//System.out.println( "--- received TEXT: " + message );
		}

		@Override
		public void onMessage(ByteBuffer bytes) {
			byte[] data = bytes.array();
			
			if ( data.length == 1) {
				return;
			}
			// Binary messages are multiplexed. See https://blog.openshift.com/executing-commands-in-pods-using-k8s-api/
			//byte stream 	= data[0];
			final String payload 	= new String(Arrays.copyOfRange(data, 1, data.length ));
			
			//log.debug("---- GOT BINARY SIZE: " + data.length + " STREAM: " + stream + " " + payload );
			unicast(session, payload);
		}
		
		@Override
		public void onClose( int code, String reason, boolean remote ) {
			// The codecodes are documented in class org.java_websocket.framing.CloseFrame
			log.warn( "Connection closed by " + ( remote ? "remote peer" : "us" ) + " Code: " + code + " Reason: " + reason );
		}

		@Override
		public void onError( Exception ex ) {
			ex.printStackTrace();
			// if the error is fatal then onClose will be called additionally
		}
	}

	/** Web socket client to the API server */
	private ApiServerClient client;
	
	private String getSessionParameter (final Session session, final String key, final String def) {
		if ( ! session.getRequestParameterMap().containsKey(key)) {
			return def;
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
		nodeId 				= getSessionParameter(session, "node", null);
		containerId 		= getSessionParameter(session, "containerId", null);
		namespace 			= getSessionParameter(session, "namespace", null);
		pod 				= getSessionParameter(session, "pod", null);
		boolean attach 		= getSessionParameter(session, "attach", null) != null;
		final String shell	= getSessionParameter(session, "shell", "sh");	// default: sh
	
		if ( nodeId == null 	|| nodeId.isEmpty() ) 		unicast(session, "Node Id is required.");
		if ( containerId == null || containerId.isEmpty() ) unicast(session, "Container Id is required.");
		if ( namespace == null 	|| namespace.isEmpty() ) 	unicast(session, "Namesapce is required.");
		if ( pod == null 		|| pod.isEmpty() ) 			unicast(session, "A Pod is required.");

		log.debug("POD-WSOpen: " + nodeId + "/" + containerId + " " + session.getRequestURI());
		this.session = session;
		//unicast(session, "Connected to " + pod + "/" + containerId + "@" + namespace);

		if ( attach ) {
			K8SNode node 			= Kubernetes.get(nodeId);
			final String wsurl		= node.getApiServer().startsWith("https") ? node.getApiServer().replace("https", "wss") : node.getApiServer().replace("http", "wss");
			final String url 		= wsurl  + "api/v1/namespaces/" + namespace + "/pods/" + pod + "/exec"
					+ "?command=" + shell
					+ "&stdin=true&stderr=true&stdout=true&tty=true&container=" +  containerId;
					
			//final String payload 		= ""; 
			final String authorization	= "Bearer " + node.getAcessToken();
			
			log.debug("Attach to node " + node + " with shell " + shell+  " Container: " + containerId + " uel:" + url);
			attachToWS(url, authorization);
		}
	}
	
	private void attachToWS (final String url, final String authorization) {
		try {
			Map<String, String> headers = new HashMap<String, String>();			

			headers.put("Accept", "*/*");
			headers.put("Authorization", authorization);
			headers.put("X-Stream-Protocol-Version", "v4.channel.k8s.io");
			headers.put("User-Agent" ,"kubectl/v1.14.0 (linux/amd64) kubernetes/641856d");
			
			client = new ApiServerClient(new URI( url ), headers);
			
			// required to ignore self-signed certs & use TLSv1.2
			client.setSocketFactory(Security.createAllTrustingSocketFactory());
			client.connect(); 	// async
			
		} catch (Exception e) {
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
		if ( client != null) {
			client.close();
		}
	}
	
	@OnMessage
	public void incoming(final String message) {
		if ( client != null) {
			client.send(("\0" + message).getBytes());
		}
	}
}
