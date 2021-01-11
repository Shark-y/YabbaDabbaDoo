package com.cloud.docker;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
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

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.types.CoreTypes;
import com.cloud.ssh.StreamIO;


/**
 * Docker Terminal {@link WebSocketContainer}. <i>Note: A new instance of this class gets created for each WebSocket connection.</i>
 * <p>
 * This class listens for Docker WebSocket XTerm.js connections and installs an image, creates a container, starts the container , then returns the stdout.
 * </p>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li> 03/18/2019 Initial commit
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 01/15/19
 */
@ServerEndpoint(value = "/DockerWizard") 
public class DockerWizardConnector {

	private static final Logger log 	= LogManager.getLogger(DockerWizardConnector.class);
	private static final String CRLF 	= "\n\r";
	
	/** The node id for this WS */
	String nodeId;
	
	/** The window id for this WS */
	String imageId;

	String tag;
	
	/** Websocket session */
	Session session;
	
	/** For interactive shells */
	DockerHttpHijack http;
	
	/** Request parameters */
	Map<String, String> request;
	
	/**
	 * Construct: Note - the container will create 1 instance of this class for every WS client.
	 */
	public DockerWizardConnector() {
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
		nodeId 			= getSessionParameter(session, "node");
		imageId 		= getSessionParameter(session, "imageId");
		tag				= getSessionParameter(session, "tag");
		//boolean attach 	= getSessionParameter(session, "attach") != null;
		
		if ( nodeId == null || nodeId.isEmpty() ) unicast(session, "Node Id is required.");
		if ( imageId == null || imageId.isEmpty() ) unicast(session, "Container Id is required.");
		
		/* 
		 * 11/20/2017 https://tomcat.apache.org/tomcat-7.0-doc/web-socket-howto.html
		 * The write timeout used when sending WebSocket messages in blocking mode defaults to 20000 milliseconds (20 seconds).
		 * http://acme208.acme.com:6091/issue/UNIFIED_CC-417
		 */
		session.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 5000); //1500);
		
		log.debug("WSOpen: " + nodeId + "/" + imageId + " " + session.getRequestURI());
		
		this.session = session;
		//if ( attach ) {
		attach();
		//}
	}

	private void attach () {
		try {
			DockerNode node 		= Docker.get(nodeId);
			//final String execId 	= Docker.execCreate(nodeId, containerId, "/bin/bash");
			final String url 		= node.buildBaseUrl() + "images/create?fromImage=" +  imageId + ( tag != null ? "&tag=" + tag : "&tag=latest");
			final String payload 	= ""; //"{ \"Detach\": false, \"Tty\": true}";
			
			log.debug(logPrefix() +  " Attach to node " + node + " Image: " + imageId + " Tag:" + tag);
			/* TODO TEST ONLY
			new Thread(new Runnable() {
				public void run() {
					try {
						// TEST
						BufferedReader br = new BufferedReader(new InputStreamReader(DockerImageConnector.class.getResourceAsStream("/junit/docker/nginx-download-stdout.txt")));
						String line = null;
						Thread.sleep(50);
						do {
							line = br.readLine();
							//System.out.println(line);
							Thread.sleep(300);
							if ( line != null) {
								PrintStream ps = new DisplayJSONMessagesStream(session.getBasicRemote().getSendStream(), true);
								ps.write(line.getBytes());
								ps.close();
							}
						}
						while ( line != null);
						br.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
			*/
			log.debug(logPrefix() +  " Attaching to URL: " + url);
			http 					= new DockerHttpHijack(new URI(url));
			if ( node.tlsEnabled) {
				http.setSSLParams("TLS", node.keyStorePath, node.keyStorePassword, true);
			}
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type",  CoreTypes.CONTENT_TYPE_JSON);

			http.post(headers, payload, false);
			http.pipeStdout(new StreamIO.OutputSink () {
				@Override
				public PrintStream getPrintStream() throws IOException {
					//return System.out;
					//return new PrintStream(session.getBasicRemote().getSendStream());
					return new DisplayJSONMessagesStream(session.getBasicRemote().getSendStream(), true);
				}

				@Override
				public boolean closeAfterWrite() {
					return true;
				}

				@Override
				public void onChunkReceived(byte[] chunk) throws IOException {
					final String str 	= new String(chunk);
					
					// Pretty lame: Done if : {"status":"Status: Image is up to date for nginx:latest"}
					// {"status":"Status: Downloaded newer image for nginx:latest"}
					boolean done 		= str.contains("up to date") || str.contains("Downloaded newer image");
					
					if ( done ) {
						try {
							if ( request == null) {
								Thread.sleep(500);
							}
							// request: {Cmd="nginx -g 'daemon off;', Args=, Ports=, PortBindings="80/tcp:80", Volumes=, Env=, Image=nginx, Mounts=, ExposedPorts=, Labels=, Networks=, Binds=}
							// params: {CMD=["nginx","-g","daemon off;"], ARGS=[], PORTS=[], PORTBINDINGS={"80/tcp":[{"HostPort":"80"}]}, VOLUMES={}, ENV=[], IMAGE=nginx, MOUNTS=, EXPOSEDPORTS={}, LABELS={}, NETWORKS=, BINDS=[]}
							Map<String, Object> params =  DockerParams.extractParamsFromMap(request);
							
							log.debug(logPrefix() + " Container CREATE PARAMS=" + params);
							
							// Np Cmd sent? Inspect Image and extract Cmd oor an entry point
							if ( !request.containsKey(DockerParams.W3_PARAM_CMD) || request.get(DockerParams.W3_PARAM_CMD).isEmpty()) {
								warning("No command line given. Inspecting image " + imageId + " for command line." + CRLF);
								
								// {"data":{"GraphDriver":{"Name":"aufs","Data":null},"Parent":"","Config":{"ArgsEscaped":true,"User":"","OnBuild":null,"Tty":false,"StdinOnce":false,"Labels":{"maintainer":"NGINX Docker Maintainers <docker-maint@nginx.com>"},"ExposedPorts":{"80/tcp":{}},"Cmd":["nginx","-g","daemon off;"],"WorkingDir":"","Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","NGINX_VERSION=1.15.10-1~stretch","NJS_VERSION=1.15.10.0.3.0-1~stretch"],"StopSignal":"SIGTERM","Entrypoint":null,"AttachStdout":false,"Domainname":"","AttachStderr":false,"Image":"sha256:eb70ea14d4ac658e54090a984eaf06ed1bc41efed0f688020d7b88d26ba38920","AttachStdin":false,"Hostname":"","Volumes":null,"OpenStdin":false},"Comment":"","Author":"","Architecture":"amd64","Os":"linux","Created":"2019-03-26T23:13:42.01289097Z","RootFS":{"Layers":["sha256:5dacd731af1b0386ead06c8b1feff9f65d9e0bdfec032d2cd0bc03690698feda","sha256:dd0338cdfab32cdddd6c30efe8c89d0229d9f939e2bb736fbb0a52f27c2b0ee9","sha256:7e274c0effe81c48f9337879b058c729c33bd0199e28e2c55093d79398f5e8c0"],"Type":"layers"},"RepoDigests":["nginx@sha256:c8a861b8a1eeef6d48955a6c6d5dff8e2580f13ff4d0f549e082e7c82a8617a2"],"DockerVersion":"18.06.1-ce","ContainerConfig":{"ArgsEscaped":true,"User":"","OnBuild":null,"Tty":false,"StdinOnce":false,"Labels":{"maintainer":"NGINX Docker Maintainers <docker-maint@nginx.com>"},"ExposedPorts":{"80/tcp":{}},"Cmd":["/bin/sh","-c","#(nop) ","CMD [\"nginx\" \"-g\" \"daemon off;\"]"],"WorkingDir":"","Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","NGINX_VERSION=1.15.10-1~stretch","NJS_VERSION=1.15.10.0.3.0-1~stretch"],"StopSignal":"SIGTERM","Entrypoint":null,"AttachStdout":false,"Domainname":"","AttachStderr":false,"Image":"sha256:eb70ea14d4ac658e54090a984eaf06ed1bc41efed0f688020d7b88d26ba38920","AttachStdin":false,"Hostname":"6c02a05b3d09","Volumes":null,"OpenStdin":false},"Id":"sha256:2bcb04bdb83f7c5dc30f0edaca1609a716bda1c7d2244d4f5fbbdfef33da366c","Metadata":{"LastTagTime":"0001-01-01T00:00:00Z"},"VirtualSize":109294563,"RepoTags":["nginx:latest"],"Container":"6c02a05b3d095c6e0f51aa3d6ff84c3cac8c76b8464ee4930c151b5afffce9ad","Size":109294563}}
								JSONObject inspect = Docker.imageInspect(nodeId, imageId);
								
								// Look @ Config.Cmd to get args: ["nginx", "-g", "daemon off;"]
								JSONArray cmd = inspect.getJSONObject("data").getJSONObject("Config").optJSONArray("Cmd");
								
								if ( cmd != null) {
									unicast(session, "Using command line " + cmd + CRLF);
									params.put(DockerParams.W3_PARAM_CMD.toUpperCase(), cmd);
								}
								else {
									// Try Config.Entrypoint ["/bin/tini","--", "/usr/local/bin/jenkins.sh" ]
									cmd = inspect.getJSONObject("data").getJSONObject("Config").optJSONArray("Entrypoint");
									
									if ( cmd != null ) {
										unicast(session, "Using Config.Entrypoint as command line " + cmd + CRLF);
										params.put(DockerParams.W3_PARAM_CMD.toUpperCase(), cmd);
									}
									else {
										danger("Unable to fetch command line arguments from " + imageId + ". Start may fail." + CRLF);
									}
								}
							}
							
							// {"data":{"Id":"a5cd86e220292e7d2d2a1d2eefcc75ed33f43b3f0a1c10d609a237b9540529d3","Warnings":["linux does not support CPU percent. Percent discarded."]}}
							JSONObject root 	= Docker.containerCreate(nodeId, params);
							final String id 	= root.getJSONObject("data").getString("Id");
							JSONArray warnings	= root.getJSONObject("data").optJSONArray("Warnings");
							
							unicast(session, "Created Container " + id + CRLF);
							unicast(session, "Starting container..." + CRLF);
							
							// Response: OK {"data":""}
							root = Docker.containerStart(nodeId, id);
							info("Started container " + id);
							
						} catch (Exception e) {
							log.error("Container Create/Start", e);
							//unicast(session, HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
							danger(HTTPServerTools.exceptionToString(e));
						}
					}
				}

				@Override
				public boolean receiveChunks() {
					return true;
				}
			} );
			
		} 
		catch (Exception e) {
			log.error("Attach " + nodeId + "/" + imageId, e);
			try {
				session.close(new CloseReason(CloseCodes.PROTOCOL_ERROR, e.toString()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@OnClose
	public void end() {
		log.debug("Closing socket to " + nodeId + "/" + imageId);
		
		if ( http != null) {
			try {
				http.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			DisplayJSONMessagesStream.clear();
		}
	}

	@OnMessage
	public void incoming(String message) {
		// { "request": "selNodes=Node1&Image=nginx&Tag=latest&username=&password=&Env=&Cmd=nginx+-g+'daemon+off%3B'&ExposedPorts=&PortBindings=80%2Ftcp%3A80&Binds=&Volumes=&Labels="}
		log.debug( nodeId + "/" + imageId  + " OnMessage: " + message + " attached:" + (http != null));
		/* Hijacked
		if ( http != null) {
			try {
				http.send(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		} */
		
		try {
			//final String stdout = Docker.execShellCommand(nodeId, containerId, message);
			//unicast(session, stdout);
			if ( message.startsWith("{")) {
				JSONObject root = new JSONObject(message);
				request 		= queryStringToMap(root.getString("request"));
			}
		/*} catch (AbstractRestException ex) {
			String msg = DockerParams.parseServerErrorReponse(ex.getMessage());
			unicast(session, HTTPServerTools.buildBaseResponse(ex.getHttpStatus(), msg).toString() ); */
		} catch (Exception e) {
			log.error("OnMessage", e);
			unicast(session, HTTPServerTools.buildBaseResponse(500, HTTPServerTools.exceptionToString(e)).toString());
		} 
	}
	
	@OnError
	public void onError(Throwable t) throws Throwable {
		log.error("WSError: ", t);
	}

	/**
	 * Convert a URL query string k1=v1&k2=v2 to a {@link HashMap}
	 * @param qs URL query string k1=v1&k2=v2 
	 * @return {@link HashMap} of (K,V) pairs. <b>If the qs argument is NULL returns an empty map</b>.
	 * @throws UnsupportedEncodingException 
	 */
	static Map<String, String> queryStringToMap (String qs) throws UnsupportedEncodingException {
		Map<String, String> map = new HashMap<String, String>();
		
		if ( qs == null) {
			log.warn("Query string to map. NULL string received.");
			return map;
		}
		String[] tmp 			= qs.split("&");
		
		for (String str : tmp) {
			final String[] kvp 	= str.split("=");
			final String key	= kvp[0];
			final String value	= kvp.length == 2 ? URLDecoder.decode(kvp[1], "UTF-8")  : "";
			map.put(key, value);
		}
		return map;
	}
	
	private String logPrefix () {
		return nodeId + "/" + imageId ;
	}
	
	void warning (final String text) {
		unicast(session, AnsiEscapeCodes.yellow() + text + AnsiEscapeCodes.white());
	}
	
	void danger (final String text) {
		unicast(session, AnsiEscapeCodes.red() + text + AnsiEscapeCodes.white());
	}
	
	void info (final String text) {
		unicast(session, AnsiEscapeCodes.blue() + text + AnsiEscapeCodes.white());
	}

}

