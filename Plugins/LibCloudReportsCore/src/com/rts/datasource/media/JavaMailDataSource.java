package com.rts.datasource.media;

import java.io.IOException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;
import com.cloud.core.io.MailTool;
import com.sun.mail.imap.IMAPFolder;

/**
 * <h2>Notes</h2>
 * <ul>
 * <li>POP3 : Awful multi part performance, cannot see new messages, see https://stackoverflow.com/questions/16334776/java-mail-listening-for-messages-pop3
 * <li>IMAP : Much faster performance for multi part messages plus supports mail notifications (new messages).
 * <ul>
 * @author VSilva
 *
 */
public class JavaMailDataSource extends BaseMapDataSource implements IDataSource  {

	private static final Logger log = LogManager.getLogger(JavaMailDataSource.class);

	/**
	 * IMAP Event notifications.
	 * 
	 * @see https://javaee.github.io/javamail/docs/api/index.html?com/sun/mail/imap/IMAPFolder.html
	 * 
	 * @author VSilva
	 *
	 */
	private static class IMAPMessageCountListener implements MessageCountListener {
		private JavaMailDataSource parent;
		private int removed;
		
		public IMAPMessageCountListener(JavaMailDataSource parent) {
			this.parent = parent;
		}
		
		@Override
		public void messagesAdded(MessageCountEvent ev) {
			Message[] messages = ev.getMessages();
			
			for (int i = 0; i < messages.length; i++) {
				Message  m 			= messages[i];
				JSONObject remote 	= null; 
				try {
					remote = MailTool.serializeMessage(m, false);
					
					log.debug("Adding new message: " + remote);
					parent.addRemote(remote);
				} catch (Exception e) {
					log.error("Add remote " + remote + " " + e);
				}
			}
		}

		@Override
		public void messagesRemoved(MessageCountEvent ev) {
			Message[] messages = ev.getMessages();
			
			// Note: cannot serialize expunged (removed messages) - javax.mail.MessageRemovedException
			// 		at com.sun.mail.imap.IMAPMessage.checkExpunged(IMAPMessage.java:277)
			for (int i = 0; i < messages.length; i++) {
				Message  m 	= messages[i];
				int remote 	= m.getMessageNumber() - 1;		// Note: 1-based index
				// java.lang.IndexOutOfBoundsException: Index: 49, Size: 49
				// remote index may not match batch size (local) if msgs removed
				try {
					// FIXME: This does not always works. A better solution is required.
					int local = remote > parent.messages.length() ? remote - removed : remote;
					
					if ( local >= parent.messages.length()) {
						log.error("Unable to remove local message: " + local + " Out of bouds remote: " + remote + " removed count:" + removed);
					}
					else {
						log.debug("Removing message remote:" + remote + " local:" + local + " removed count:" + removed); 
						parent.messages.remove(local);
					}
					removed++;
				} catch (Exception e) {
					log.error("Remove remote " + remote, e);
				}
			}
		}
	}

	/** Mail ptotocol: POP3, IMAP */
	private final String proto;
	
	/** Java mail {@link Session} */
	private final Session session;
	
	/** Java mail {@link Session} */
	private Store store; 

	/** Email messages */
	private final JSONArray messages = new JSONArray();
	
	/** Used to get messages in the background */
	private Thread poller ;

	/** Used for IMAP event notifications. See {@link MessageCountListener} */
	private final IMAPMessageCountListener events;
	
	/** Used to describe the {@link DataFormat} of this {@link IDataSource} */
	private final DataFormat fmt;
	
	// This must match the JSON created by MailTool.java
	private static final String FORMAT_FIELDS = String.format("%s,%s,%s,%s,%s,%s"
			, MailTool.JSONK_NUMBER, MailTool.JSONK_FROM, MailTool.JSONK_SUBJECT
			, MailTool.JSONK_SIZE, MailTool.JSONK_SENTDATE, MailTool.JSONK_CONTENTTYPE ); //,content";
	
	private void LOGD(String text) {
		log.debug(String.format("[%s/%s] %s", name, proto, text));
	}
	
	public JavaMailDataSource(final String proto, final String name, final String description
			, final String host, final String port	, final String from 
			, final String user, final String password, final String folder
			, boolean startTls, boolean debug
	) 
			throws IOException, JSONException 
	{
		super(name, description);

		if ( proto == null )	throw new IOException("Media Store protocol is required.");
		if ( host == null )		throw new IOException("SMTP(s) Host name/ip is required.");
		if ( port == null )		throw new IOException("SMTP(s) Port is required.");
		if ( from == null )		throw new IOException("SMTP(s) Sender (From) is required.");
		if ( folder == null )	throw new IOException("Store folder is required.");
		
		this.proto = proto;
		
		params.put(KEY_SMTP_HOST, host);
		params.put(KEY_SMTP_PORT, port);
		params.put(KEY_SMTP_FROM, from);
		params.put(KEY_SMTP_FOLDER, folder);

		// optional (SMTPS)
		params.putOpt(KEY_SMTP_USER, user);
		params.putOpt(KEY_SMTP_PWD, password);
		
		params.put(KEY_SMTP_DEBUG, debug);
		params.put(KEY_SMTP_TLS, startTls);
		
		session = MailTool.getSession(proto, host, port, startTls, debug);
		events 	= new IMAPMessageCountListener(this);
		
		fmt = new DataFormat(null, null, null, null, FORMAT_FIELDS , null);
	}
	
	public JavaMailDataSource(JSONObject ds ) throws JSONException, IOException {
		super( ds.getString("name"), ds.optString("description"));
		proto 				= ds.getString("type");
		params 				= ds.getJSONObject("params");
		String host 		= params.getString(KEY_SMTP_HOST);
		String port 		= params.getString(KEY_SMTP_PORT);
		boolean startTls 	= params.getBoolean(KEY_SMTP_TLS);
		boolean debug		= params.getBoolean(KEY_SMTP_DEBUG);
		session 			= MailTool.getSession(proto, host, port, startTls, debug);
		events 				= new IMAPMessageCountListener(this);
		
		fmt = new DataFormat(null, null, null, null, FORMAT_FIELDS, null);
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public void run() {
		String user 	= null;
		String password = null;

		try {
			user 	= params.getString(KEY_SMTP_USER);
			password = params.getString(KEY_SMTP_PWD);

			if ( proto.startsWith(DataSourceType.POP3.name()) || proto.startsWith(DataSourceType.IMAP.name())) {
				LOGD("Connect " + user);
				store 	= session.getStore(proto.toLowerCase());
				store.connect(user, password);
			}
			status.setStatus(Status.ON_LINE, "Connected.");
			
			if (messages.length() == 0) {
				pollerStart();
			}
//			else if ( isIMAP()) {
//				imapEnterIdleMode();
//			}
		} catch (Exception e) {
			log.error("JAVA-MAIL::RUN " + user , e);
			status.setStatus(Status.SERVICE_ERROR, e.toString());
		} 

	}

	@Override
	public void stop() {
		try {
			LOGD("Stop.");
			pollerStop();
			
			if ( isIMAP()) {
				imapCleanIdleMode();
			}
			if ( store != null) {
				store.close();
			}
			status.setStatus(Status.OFF_LINE, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() {
		stop();
	}

	@Override
	public int getPort() {
		return Integer.valueOf(params.optString(KEY_SMTP_PORT,"25"));
	}

	@Override
	public DataFormat getFormat() {
		return fmt;
	}

	@Override
	public ServiceStatus getStatus() {
		return status;
	}

	@Override
	public long getTotalBatches() {
		return 0;
	}

	@Override
	public long getTotalRecords() {
		return 0;
	}

	@Override
	public void setEventListener(IBatchEventListener l) {
	}

	@Override
	public String toXML() throws IOException {
		throw new IOException("toXML() is deprecated.");
	}

	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject root = new JSONObject();
		root.put("type", getType().name());
		root.put("name", name);
		root.putOpt("description", description);

		//JSONObject params = new JSONObject(config);
		root.put("params", params);
		
		return root;
	}

	@Override
	public DataSourceType getType() {
		return DataSourceType.valueOf(proto.toUpperCase());
	}

	public void sendMail( String to, String subject, String text) throws IOException {
		try {
			String from = params.getString(KEY_SMTP_FROM);
			String user = params.optString(KEY_SMTP_USER);
			String pwd  = params.optString(KEY_SMTP_PWD);
			
			MailTool.sendMail(session, proto, from, to, subject, text, user, pwd /*, startTls, debug*/);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	/**
	 * Invoked by the REST API to get messages. Note the mail folder is defined in the data source configuration.
	 * @param limit The max number of messages to retrieve.
	 * @return [ {number: 1, from: "vsilva@foo.com", subject: "Test", size: 0, sentDate: "Date", contentType : "text/html", "content": ""}, ...]
	 * @throws IOException
	 */
	public JSONArray getMessages( /*final String folder, */ int limit) throws IOException {
		return messages;
	}

	public JSONObject getMessageContent( int messageNumber) throws IOException, JSONException {
		for (int i = 0; i < messages.length(); i++) {
			JSONObject m 	= messages.getJSONObject(i);
			int number 		= m.getInt(MailTool.JSONK_NUMBER);
			if ( number == messageNumber) {
				return m;
			}
		}
		throw new IOException("Cannot find message number " + messageNumber + " in " + super.name);
		//return null;
	}
	
	/*
	 * Fetch messages from the server.
	 */
	private JSONArray fetchMessages( /*final String folder, int limit*/) throws IOException, MessagingException, JSONException {
		if ( store == null ) {
			return null;
		}
		String folder 		= params.optString(KEY_SMTP_FOLDER);
		final String user 	= params.optString(KEY_SMTP_USER);
		final String pwd  	= params.optString(KEY_SMTP_PWD);
		final int limit 	= -1; // ALL msgs
		
		if ( folder == null || folder.isEmpty()) {
			log.warn("FetchMessages: Folder is null or empty Using default (Inbox).");
			folder = "Inbox";
		}
		
		if ( user == null || user.isEmpty()) 	throw new IOException("FetchMessages: User name can't be null or empty.");
		if ( pwd == null || pwd.isEmpty()) 		throw new IOException("FetchMessages: Password can't be null or empty.");
		
		// POP3: Multipart msgs 2MB in size take 3min to parse. IMAP much faster
		return MailTool.getMessages(store, proto, user, pwd, folder,  limit, false, new MailTool.IMessageListener() {
			
			@Override
			public void onMessage(JSONObject message) {
				try {
					addRemote(message);
				} catch (JSONException e) {
					log.error(proto + " Add remote: " + e.toString());
				}
			}
		});
		//return MailTool.getMessages(store, proto, user, pwd, folder,  limit, true, null);
	}
	
	private void addRemote (JSONObject remote) throws JSONException {
		boolean found 		= false;
		final String key 	= MailTool.JSONK_NUMBER;
		
		for (int j = 0; j < messages.length(); j++) {
			JSONObject local = messages.getJSONObject(j);
			
			if ( local.getInt(key) == remote.getInt(key)) {
				found = true;
				break;
			}
		}
		if ( !found) {
			messages.put(remote);
		}
	}

	/*
	private void removeRemote (JSONObject remote) throws JSONException {
		int found 		= -1;
		
		for (int j = 0; j < messages.length(); j++) {
			JSONObject local = messages.getJSONObject(j);
			
			if ( local.getInt("index") == remote.getInt("index")) {
				found = j;
				break;
			}
		}
		if ( found > 0) {
			messages.remove(found);
		}
	} */
	
	private void pollerStart () {
		if ( ! pollerEnabled()) {
			return;
		}
		poller = new Thread(new Runnable() {
			public void run() {
				while ( true ) {
					try {
						//String folder  = params.optString(KEY_SMTP_FOLDER);
						// POP3: this takes forever...
						JSONArray array = fetchMessages(); //folder, -1); 
						
						if ( array != null) {
							for (int i = 0; i < array.length(); i++) {
								JSONObject remote 	= array.getJSONObject(i);
								addRemote(remote);
							}
						}
					} catch (Exception e) {
						log.error(proto + " Poller: " + e.toString());
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
					// IMAP: Loop once & use listeners to get notifications
					if ( isIMAP()) {
						break;
					}
				}
				LOGD("Poller finished fetching messages.");

				// IMAP Enter IDLE mode to receive events
				// https://javaee.github.io/javamail/docs/api/index.html?com/sun/mail/imap/IMAPFolder.html
				if ( isIMAP()) {
					while ( true) {
						/* java.lang.IllegalStateException: Not connected
						at com.sun.mail.imap.IMAPStore.checkConnected(IMAPStore.java:1903)
						at com.sun.mail.imap.IMAPStore.getFolder(IMAPStore.java:1686)
						at com.cloud.rts.datasource.media.JavaMailDataSource$2.run(JavaMailDataSource.java:435) */
						try {
							imapEnterIdleMode();
						}
						catch ( IllegalStateException iee) {
							log.error("Enter IDLE mode for notifications. Break from IDLE loop", iee);
							break;
						} catch (Exception e) {
							log.error("Enter IDLE mode for notifications.", e);
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				}
				LOGD("Poller loop terminated.");
			}
		}, proto + "-MessagePoller");
		poller.start();
		
		LOGD("Started message poller for " + name );
	}
	
	private boolean isIMAP () {
		return proto.startsWith(DataSourceType.IMAP.name());
	}
	
	private void pollerStop () {
		if ( poller == null ) {
			return;
		}
		LOGD("Interrupting message poller.");
		poller.interrupt();
		try {
			poller.join(5000);
		} catch (InterruptedException e) {
		}
	}
	
	/*
	 * Polling for messages is enabled in both POP3 and IMAP.
	 */
	private boolean pollerEnabled () {
		return proto.startsWith(DataSourceType.POP3.name()) || proto.startsWith(DataSourceType.IMAP.name()) ;
	}
	
	/*
	 * https://javaee.github.io/javamail/docs/api/index.html?com/sun/mail/imap/IMAPFolder.html
	 */
	private void imapEnterIdleMode () throws JSONException, MessagingException {
		final String name = params.getString(KEY_SMTP_FOLDER);
		IMAPFolder folder = (IMAPFolder)store.getFolder(name);
		
		if ( !folder.isOpen()) {
			folder.open(Folder.READ_ONLY);
		}
		LOGD("IMAP Enter idle mode for notifications");
		folder.removeMessageCountListener(events);
		folder.addMessageCountListener(events);
		folder.idle();
	}
	
	private void imapCleanIdleMode () {
		// Do something here...
	}
	
	/**
	 * Get all batches minus the content. Similar to getMessages() but invoked by the System > Diagnostics page in the console.
	 * @return JSON: [ {index: 1, from: "vsilva@foo.com", subject: "Test", size: 0, sentDate: "Date", contentType : "text/html"}, ...]
	 */
	public JSONArray getBatches() {
		// All messages minus the content. This must match FORMAT_FIELDS
		try {
			JSONArray batch = new JSONArray(messages.toString());
			
			for (int i = 0; i < batch.length(); i++) {
				JSONObject m = batch.getJSONObject(i);
				m.remove("content");
				//batch.put(m);
			}
			return batch;
		} catch (Exception e) {
			// This shouldn't happen
			log.error("Get batches", e);
		}
		return new JSONArray(); // cannot be null. batch; //messages;
	}
	
	@Override
	public JSONObject getParams() {
		return params;
	}
}
