package com.cloud.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;


/**
 * SSH Terminal {@link WebSocketContainer}. <i>Note: A new instance of this class gets created for each WebSocket connection.</i>
 * <p>
 * This class listens for SSH (ssh.jsp) WebSocket XTerm.js connections and invokes the command via the Docker exec API, then returns the stdout.
 * </p>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li> 03/18/2019 Initial commit.
 * </ul>
 * @author VSilva
 * @version 1.0.0 - 01/15/19
 */
@ServerEndpoint(value = "/SSHTerminal") 
public class SSHTerminalConnector {

	private static final Logger log = LogManager.getLogger(SSHTerminalConnector.class);

	/** The client id for this WS */
	private String hostName;
	
	/** The window id for this WS */
	private String user;

	/** Websocket session */
	private Session session;
	
	/** SSH */
	private com.jcraft.jsch.Session ssh;

	/** I/O handler */
	private ChannelHandler handler;
	
	
	
	/*
	public static class MyLogger implements com.jcraft.jsch.Logger {
		static java.util.Hashtable name = new java.util.Hashtable();
		static{
		  name.put(new Integer(DEBUG), "DEBUG: ");
		  name.put(new Integer(INFO), "INFO: ");
		  name.put(new Integer(WARN), "WARN: ");
		  name.put(new Integer(ERROR), "ERROR: ");
		  name.put(new Integer(FATAL), "FATAL: ");
		}
		public boolean isEnabled(int level){
		  return true;
		}
		public void log(int level, String message){
		  System.out.print(name.get(new Integer(level)));
		  System.out.println(message);
		}
	} */

	/**
	 * Construct: Note - the container will create 1 instance of this class for every WS client.
	 */
	public SSHTerminalConnector() {
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
		hostName 			= getSessionParameter(session, "host");
		user 				= getSessionParameter(session, "user");
		String identity 	= getSessionParameter(session, "identity");
		
		//boolean attach 		= getSessionParameter(session, "attach") != null;
		//final String shell	= getSessionParameter(session, "shell");
		
		if ( hostName == null 	|| hostName.isEmpty() ) unicast(session, "Host name is required.");
		if ( user == null 		|| user.isEmpty() )	 	unicast(session, "User name is required.");
		if ( identity == null 	|| identity.isEmpty() ) unicast(session, "Identity is required.");
		
		/* 
		 * 11/20/2017 https://tomcat.apache.org/tomcat-7.0-doc/web-socket-howto.html
		 * The write timeout used when sending WebSocket messages in blocking mode defaults to 20000 milliseconds (20 seconds).
		 * http://acme208.acme.com:6091/issue/UNIFIED_CC-417
		 */
		session.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 5000); //1500);
		
		log.debug("WSOpened: " + hostName + "/" + user + " " + session.getRequestURI());
		
		this.session = session;
		attach(hostName, user, identity); // shell != null ? shell : "sh"); //"/bin/bash");
	}


	/**
	 * Attach terminal to container with a given shell.
	 * @param host SSH host.
	 * @param user SSH user.
	 * @param identity SSH password or private key (PKEY:/oath/to/putty-formatted.key)
	 */
	private void attach (final String host, String user, String identity) {
		try {
			String pwd 		= null;
			String kPath 	= null;
			
			if ( identity.startsWith(SSHExec.KEY_PREFIX)) {
				kPath = identity.replace(SSHExec.KEY_PREFIX, "");
			}
			else {
				pwd = identity;
			}
			
			JSch jsch 	= new JSch();
			//JSch.setLogger(new MyLogger());
			if ( kPath != null ) {
				jsch.addIdentity(kPath);
			}

			log.debug("Start SSH session to " + host + "/" + user + " " + identity );

			ssh 		= jsch.getSession(user, host, 22);
			ssh.setPassword(pwd); //identity);	
			
		    UserInfo ui = new SSHDefaultUserInfo(){
		        public void showMessage(String message){
		          unicast(session, message);
		        }
		        public boolean promptYesNo(String message) {
		        	//System.out.println("PROMPT: " + message);
		        	//unicast(session, message.replaceAll("\n", "\n\r"));
		        	return true;
		        }

		        // If password is not given before the invocation of Session#connect(),
		        // implement also following methods,
		        //   * UserInfo#getPassword(),
		        //   * UserInfo#promptPassword(String message) and
		        //   * UIKeyboardInteractive#promptKeyboardInteractive()

		    };

		    ssh.setUserInfo(ui);
		    //session.connect();
		    ssh.connect(5000);   // making a connection with timeout.
		    
		    Channel channel = ssh.openChannel("shell");
		    
		    handler = new ChannelHandler(channel);
			handler.pipeStdout(new StreamIO.PrintStreamSource () {
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
			
		    channel.connect(3*1000);
		} 
		catch (Exception e) {
			log.error("Attach " + hostName + "/" + user, e);
			try {
				session.close(new CloseReason(CloseCodes.PROTOCOL_ERROR, e.toString()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Class to read/write asynchronously from the SSH/WebSocket I/O streams.
	 * 
	 * @author VSilva
	 */
	static class ChannelHandler  {
		private Thread reader;
		final private OutputStream out;
		final private InputStream in;
		final String id ;
		
		public ChannelHandler(Channel channel ) throws IOException, JSchException {
	    	if ( channel == null) {
	    		throw new IOException("SSH Socket Channel can't be null");
	    	}
			this.in 	= channel.getInputStream();
			this.out 	= channel.getOutputStream();
			this.id 	= "SSH-SOCKET-READER-" + channel.getSession().getHost();
		}
		
		/**
	     * Start reading from the socket input stream in the background and writing in the stream provided.
	     * @param ps Provides an output stream and tells the reader if it should be closed after each write. See {@link PrintStreamSource}.
	     * @throws IOException on I/O errors.
		 * @throws JSchException  on SSH errors.
	     */
	    public void pipeStdout (StreamIO.PrintStreamSource ps) throws IOException, JSchException {
	    	reader = new Thread(new StreamIO.InReader(id, in , ps), id);
	    	reader.start();
	    }
		private void send (String command) throws IOException {
			out.write(command.getBytes("UTF-8"));
			out.flush();
		}
	}
  	
	@OnClose
	public void end() {
		log.debug("Closing socket to " + hostName + "/" + user);
		
		if ( ssh != null) {
			ssh.disconnect();
		}
	}

	@OnMessage
	public void incoming(String message) {
		//System.out.println("Message: [" + message +"]" );
		if ( handler != null) {
			try {
				handler.send(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
	}
	
	@OnError
	public void onError(Throwable t) throws Throwable {
		log.error("WSError: ", t);
	}

}

