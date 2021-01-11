package com.cloud.core.io;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.security.EncryptionTool;

/**
 * Java mail helper tool
 * <ul>
 * <li> For POP3 see https://javaee.github.io/javamail/docs/api/com/sun/mail/pop3/package-summary.html
 * <li> For SMTP(s) see https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - 11/25/2017 Initial implementation
 */
public class MailTool {

	private static final Logger log = LogManager.getLogger(MailTool.class); 

	public static final String KEY_SMTP_PROTO 	= "proto";
	
	public static final String KEY_SMTP_DEBUG 	= "debug";
	public static final String KEY_SMTP_USER 	= "user";
	public static final String KEY_SMTP_PWD 	= "password";
	public static final String KEY_SMTP_HOST 	= "host";
	public static final String KEY_SMTP_PORT 	= "port";
	public static final String KEY_SMTP_TLS 	= "starttls.enable";
	public static final String KEY_SMTP_FROM 	= "from";
	public static final String KEY_SMTP_TO 		= "to";
	public static final String KEY_SMTP_FOLDER 	= "folder";

	/*
	 * JSON Serialization keys.
	 */
	public static final String JSONK_NUMBER 		= "number";
	public static final String JSONK_FROM 			= "from";
	public static final String JSONK_SUBJECT 		= "subjet";
	public static final String JSONK_SIZE 			= "size";
	public static final String JSONK_SENTDATE 		= "sentDate";
	public static final String JSONK_CONTENTTYPE 	= "contentType";
	public static final String JSONK_CONTENT 		= "content";

	static public interface IMessageListener {
		void onMessage (JSONObject message);
	}
	
	/**
	 * Create a java mail session for a given protocol.
	 * @param proto Protocol name: SMTP, SMTPS or POP3.
	 * @param host Host name (mail.pop3.host).
	 * @param port Port  (mail.pop3.port)
	 * @param startTls (mail.pop3.starttls.enable) f true, enables the use of the STLS command (if supported by the server) to switch the connection to a TLS-protected connection before issuing any login commands. 
	 * @param debug If true print debug stuff to stdout
	 * @return Java mail {@link Session}.
	 */
	public static Session getSession (final String proto, final String host, final String port, final boolean startTls,final  boolean debug) {
		final String prefix = "mail." + proto.toLowerCase() + ".";
		
		Properties props = new Properties();
		
		props.put(prefix + KEY_SMTP_HOST, host);
		props.put(prefix + KEY_SMTP_PORT, port);
		props.put(prefix + KEY_SMTP_TLS, String.valueOf(startTls));
		props.put("mail.debug", String.valueOf(debug));
		
		if ( debug) {
			log.debug("JavaMail Session Properties: " + props);
		}

		Session session = Session.getInstance(props);
		return session;
	}

	/**
	 * Simple send mail command.
	 * @param proto Protocol name: SMTP, SMTPS or POP3.
	 * @param host Host name (mail.pop3.host).
	 * @param port Port  (mail.pop3.port)
	 * @param startTls (mail.pop3.starttls.enable) f true, enables the use of the STLS command (if supported by the server) to switch the connection to a TLS-protected connection before issuing any login commands. 
	 * @param debug If true print debug stuff to stdout
	 * @param from Sender email.
	 * @param to recipient email.
	 * @param subject Email subject.
	 * @param text Email message.
	 * @param user User name (mail.pop3.user)
	 * @param password The password
	 * 
	 * @return The time in milli-seconds that takes to send the email.
	 * @throws IOException
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public static long sendMail(final String proto, final String host, final String port, final boolean startTls, final boolean debug
			, final String from, final String to, final String subject, final String text, final String user, final String password, File[] attachements  ) 
			throws IOException, AddressException, MessagingException 
	{
		Session session = getSession(proto, host, port, startTls, debug);
		return sendMail(session, proto, from, to, subject, text, user, password, attachements);
	}

	/**
	 * Simple send mail command.
	 * @param prefix Prefix used on each JSON configuration key. For example: notification_ . Do not send null (use an empty string instead).
	 * @param config Protocol Configuration JSON. For example: {"notification_password":"Thenewcti1","notification_user":"converge_one@yahoo.com","notification_debug":"on","notification_to":"cloud_git@convergeone.com","notification_proto":"smtps","notification_host":"smtp.mail.yahoo.com","notification_port":"465","notification_starttls.enable":"on","notification_from":"converge_one@yahoo.com","notification_vendor":"ACME","notification_folder":"Inbox"}
	 * @param subject Email subject.
	 * @param text Email message.
	 * @param attachements Array of file attachments or NULL if no files are to be attached.
	 * @return The time in milli-seconds that takes to send the email.
	 * @throws IOException on I/O errors.
	 * @throws AddressException on email errors.
	 * @throws MessagingException on email errors
	 * @throws JSONException on JSON configuration errors.
	 */
	public static long sendMail(final String prefix, JSONObject config, final String subject, final String text, File[] attachements  ) 
			throws IOException, AddressException, MessagingException, JSONException 
	{
		final String proto 	= config.getString(prefix + KEY_SMTP_PROTO);
		final String host 	= config.getString(prefix + KEY_SMTP_HOST);
		final String port 	= config.getString(prefix + KEY_SMTP_PORT);
		final String from 	= config.getString(prefix + KEY_SMTP_FROM);
		final String to 	= config.getString(prefix + KEY_SMTP_TO);
		final String user 	= config.getString(prefix + KEY_SMTP_USER);
		final String password = EncryptionTool.decryptTaggedPassword(config.getString(prefix + KEY_SMTP_PWD));
		boolean startTls;
		boolean debug;
		try {
			startTls 	= config.getString(prefix + KEY_SMTP_TLS).equals("on") ;
			debug 		= config.getString(prefix + KEY_SMTP_DEBUG).equals("on");
		} catch (Exception e) {
			startTls 	= config.getBoolean(prefix + KEY_SMTP_TLS);
			debug 		= config.getBoolean(prefix + KEY_SMTP_DEBUG);
		}
		Session session = getSession(proto, host, port, startTls, debug);
		return sendMail(session, proto, from, to, subject, text, user, password, attachements);
	}

	/**
	 * Simple send mail command.
	 * @param session JavaMail Session.
	 * @param proto Protocol name: POP3
	 * @param from Sender email.
	 * @param to recipient email.
	 * @param subject Email subject.
	 * @param text Email message (Plain text or HTML).
	 * @param user User name (mail.pop3.user)
	 * @param password The password
	 * 
	 * @return The time in milli-seconds that takes to send the email.
	 * @throws IOException on I/O Errors.
	 * @throws AddressException If invalid email addresses.
	 * @throws MessagingException on message errors
	 */
	public static long sendMail(Session session, final String proto, final String from, final String to, final String subject, final String text
			, final String user, final String password) 
			throws IOException, AddressException, MessagingException 
	{
		return sendMail(session, proto, from, to, subject, text, user, password, null);
	}
	
	/**
	 * Simple send mail command.
	 * @param session JavaMail Session.
	 * @param proto Protocol name: POP3
	 * @param from Sender email.
	 * @param to recipient email.
	 * @param subject Email subject.
	 * @param text Email message (Plain text or HTML).
	 * @param user User name (mail.pop3.user)
	 * @param password The password
	 * @param attachements Array of file attachments or NULL if no files are to be attached.
	 * 
	 * @return The time in milli-seconds that takes to send the email.
	 * @throws IOException on I/O Errors.
	 * @throws AddressException If invalid email addresses.
	 * @throws MessagingException on message errors
	 */
	public static long sendMail(final Session session, final String proto, final String from, final String to, final String subject, final String text
			, final String user, final String password, File[] attachments ) 
			throws IOException, AddressException, MessagingException 
	{
		// Java mail properties prefix (mail.PROTOCOL.PROPERTY = VALUE)
		long t0 = System.currentTimeMillis();

		log.debug("Sendmail to " + to + " Subj:" + subject + " Msg=" + text + " Config:" + session.getProperties() + " User:" + user + "/" + password);
		
		//Transport.send(message);
		Transport tr = session.getTransport(proto.toLowerCase());
		if ( user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
			tr.connect(user, password);
		}
		else {
			tr.connect();
		}
		
		Message message 	= new MimeMessage(session);
		Multipart multipart = new MimeMultipart( "alternative" );
		
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO,	InternetAddress.parse(to));
		message.setSubject(subject);
		
		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent( text, "text/html; charset=utf-8" );
		
		multipart.addBodyPart(htmlPart);

		// add attachements
		// https://www.tutorialspoint.com/javamail_api/javamail_api_send_email_with_attachment.htm
		if ( attachments != null) {
			for ( File f : attachments) {
				MimeBodyPart part = new MimeBodyPart();
				DataSource source = new FileDataSource(f);
				part.setDataHandler(new DataHandler(source));
				part.setFileName(f.getName());
				multipart.addBodyPart(part);
			}
		}
		
		message.setContent(multipart);
		
		tr.sendMessage(message, message.getAllRecipients());
		tr.close();
		return System.currentTimeMillis() - t0;
	}
	
	
	/**
	 * Serialize a Java mail message to JSON.
	 * <ul>
	 * <li>POP3: multi part messages take too long to serialize the content (4-5min with attachments of 2MB).
	 * <li>IMAP: Much faster performance for multi part (~2s with attachments of 2MB).
	 * </ul>
	 * @param m Mail {@link Message}
	 * @param skipMultiPart If true skip milti part messages which take too long in POP3.
	 * @return {index: 1, from: "vsilva@foo.com", subject: "Test", size: 0, sentDate: "Date", contentType : "text/html", "content": "test"} 
	 * 	or NULL if skip multipart is true.
	 * 
	 * @throws JSONException on JSON errors
	 * @throws MessagingException on Mail errors.
	 * @throws IOException on errors fetching message content
	 */
	public static JSONObject serializeMessage (Message m, boolean skipMultiPart) throws JSONException, MessagingException, IOException {
		JSONObject msg 	= new JSONObject();
		
		msg.put(JSONK_NUMBER, 	m.getMessageNumber());
		msg.put(JSONK_FROM, 	m.getFrom()[0]);		
		msg.put(JSONK_SUBJECT, 	m.getSubject());
		msg.put(JSONK_SIZE, 	m.getSize());
		msg.put(JSONK_SENTDATE, m.getSentDate());
		
		// javax.mail.internet.MimeMultipart@1d7fc31
		Object content 		= m.getContent();
		
		if ( content instanceof MimeMultipart) {
			if ( skipMultiPart) {
				return null;
			}
			MimeMultipart multipart = (MimeMultipart)content;
			
			//long t2 = System.currentTimeMillis();
			// POP3: This takes too long depending on the size of the message ~ (200-4000)ms 
			final int count			= multipart.getCount();	
			//System.out.println(i + " Get MP count " + count + " took " + (System.currentTimeMillis() - t2));
			
			for (int j = 0; j < count ; j++) {
				BodyPart bodyPart 	= multipart.getBodyPart(j);
				//String disposition 	= bodyPart.getDisposition(); // inline imgs, etc
				String contentType 	= bodyPart.getContentType();
				
				if ( contentType.toLowerCase().startsWith("text")) {
					msg.put(JSONK_CONTENTTYPE, contentType);
					msg.put(JSONK_CONTENT, bodyPart.getContent().toString());
				}
			}
			if (!msg.has(JSONK_CONTENTTYPE)) {
				msg.put(JSONK_CONTENTTYPE, "text/plain");
			}
			if (!msg.has(JSONK_CONTENT)) {
				msg.put(JSONK_CONTENT, content.toString());
			}
		}
		else {
			// String
			msg.put(JSONK_CONTENTTYPE, "text/plain");
			msg.put(JSONK_CONTENT, content.toString());
		}
		return msg;
	}
	
	/**
	 * JavaMail: get messages from a store using the POP3, IMAP protocol(s).
	 * <h2>Notes</h2>
	 * <ul>
	 * <li> This takes a long time even in fast IMAP connections.
	 * <li> POP3: Awful performance with multi part attachments (2MB PDF) takes 2min to parse 1 lousy message.
	 * <li> IMAP: Much faster than POP3. At least takes 3s to parse the same multi part 2MB PDF attached message.
	 * </ul>
	 * @param store The java mail {@link Store} that contains the messages. Obtain one using the getSession() method.
	 * @param proto Protocol name: POP3, IMAP.
	 * @param host Host name (mail.PROTO.host).
	 * @param port Port  (mail.PROTO.port)
	 * @param user User name (mail.PROTO.user)
	 * @param password The password.
	 * @param folder Folder that contains the messages. Default: Inbox.
	 * @param limit Max number of messages to retrieve.
	 * @param skipMultiPart if true skip multi part content messages (to slow for POP3).
	 * @param listener an {@link IMessageListener} to receive a notification when new messages are consumed.
	 * 
	 * @return JSON: [{"content": "TEST", "index": 0, "subject": "Reminder to use the new Ultipro link", "from": "sender", "size": 15823},..]
	 * @see https://javaee.github.io/javamail/docs/api/com/sun/mail/pop3/package-summary.html
	 * 
	 * @throws MessagingException on Java mail errors.
	 * @throws JSONException on Mail to JSON conversion errors.
	 * @throws IOException Any other HTTPP errror.
	 */
	public static JSONArray getMessages(Store store , String proto, String user, String password, final String folder
			, int limit, boolean skipMultiPart, IMessageListener listener
			) 
			throws MessagingException, JSONException, IOException 
	{
		final long t0 		= System.currentTimeMillis();
		//long t1 			= System.currentTimeMillis();

		Folder inbox 		= store.getFolder(folder);
		inbox.open(Folder.READ_ONLY);

		// get the list of inbox messages
		Message[] messages 	= inbox.getMessages();
		int max 			= (limit < 0) || ((limit > 0) && (messages.length < limit)) ? messages.length : limit;
		JSONArray array		= new JSONArray();

		log.trace("Get folder messages took " + (System.currentTimeMillis() - t0) + " ms.");
		long t1 			= System.currentTimeMillis();
		
		for (int i = 0; /*i < max*/ i < messages.length && array.length() < max; i++) {
			Message m 		= messages[i];
			JSONObject msg 	= serializeMessage(m, skipMultiPart);
			
			// null if skip multi part (for POP3 which takes too long)
			if ( msg == null) {
				continue;
			}
			if ( listener != null) {
				listener.onMessage(msg);
			}
			else {
				array.put(msg);
			}
		}
		log.trace("Loop thru messages took " + (System.currentTimeMillis() - t1) + " ms");
		
		inbox.close(true);
		// Don't close store.close();
		
		long time = System.currentTimeMillis()  - t0;
		log.debug("Total execution time: " + time + " ms");
		
		return array;
	}

}
