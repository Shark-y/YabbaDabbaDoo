package com.cloud.core.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.CyclicBuffer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.CyclicBufferAppender;
import com.cloud.core.logging.L4JAudit;
import com.cloud.core.logging.LogManager;


/**
 * An extension of the Log4J {@link SMTPAppender} that stores audit or any ERROR event(s).
 * To be mailed using SMTP.
 * 
 * @author VSilva
 * 
 */
public class L4JSMTPAppender extends SMTPAppender {
	
	@Override
	public void append(LoggingEvent event) {
		// only append Audit events or any ERROR event to the buffer.
		final String name 	= event.getLogger().getName();
		final Level level	= event.getLogger().getLevel();
		
		if ( level == null || name == null ) return;
		
		// Not audit 
		if ( !name.contains("Audit") )	{
			// Skip level != ERROR || FATAL
			if ( level.toInt() != Level.ERROR_INT && level.toInt() != Level.FATAL_INT ) {
				return ; 	// skip it
			}
		}
		super.append(event);
	}

	
	public CyclicBuffer getBuffer() {
		return cb;
	}

	public void dispose () {
		this.msg = null;
		this.cb = null;
	}
	
	@Override
	public void activateOptions() {
	}
	
	@Override
	protected boolean checkEntryConditions() {
		if (layout == null) {
			this.errorHandler.error("No layout set for appender named [" + this.name + "].");
			return false;
		}
		return true;		
	}
	
	/**
	 * Send a test message.
	 * @param text
	 * @throws MessagingException
	 */
	public void sendTestMessage(String text) throws MessagingException {
		Session session = createSession();
		sendMessage(session, createMessage(session, text, layout.getContentType()));
	}


	@Override
	protected void sendBuffer() {
		try {
			StringBuffer sbuf 	= new StringBuffer();
			String t			= layout.getHeader();
			
			if (t != null)
				sbuf.append(t);
			
			int len = cb.length();
			
			for (int i = 0; i < len; i++) {
				LoggingEvent event = cb.get();
				sbuf.append(layout.format(event));
				
				if (layout.ignoresThrowable()) {
					String[] s = event.getThrowableStrRep();
					if (s != null) {
						for (int j = 0; j < s.length; j++) {
							sbuf.append(s[j]);
							sbuf.append(Layout.LINE_SEP);
						}
					}
				}
			}
			t = layout.getFooter();
			
			if (t != null) {
				sbuf.append(t);
			}
			
			// Note: Create a new message every time to preserve the content type
			//sendMessage(createMessage(sbuf.toString(), layout.getContentType()));
			
			// This should make the GUI more responsive.
			//asyncSendMessage(createMessage(sbuf.toString(), layout.getContentType()));
			asyncSendMessage(sbuf.toString(), layout.getContentType());
		} 
		catch (Exception e) {
			LogLog.error("Error occured while sending e-mail notification.", e);
		}
	}

	private Message createMessage(Session session, Object content, String contentType) throws MessagingException {
		Message message = new MimeMessage(session);
		addressMessage(message);
		message.setSubject(getSubject());
		message.setSentDate(new Date());
		message.setContent(content, contentType);
		return message;
	}
	
	/**
	 * Asynchronously send a notification message. This makes the GUI more responsive.
	 * @param content Message content.
	 * @param contentType text/plai, text/html, etc.
	 */
	private void asyncSendMessage(final String content, final String contentType) {
		Runnable r = new Runnable() {
			public void run() {
				try {
					Session session = createSession();
					Message message = createMessage(session, content, contentType);
					sendMessage(session, message);
				} catch (Exception e) {
					L4JAudit.LOGD("Send Notification Error: " + e.toString());
				}
			}
		};
		Thread t = new Thread( r, "L4JAuditSMTPMessageSender");
		t.start();
	}
	
	/**
	 * Send a MIME message thru the SMTP(s) channel 
	 * @throws MessagingException
	 */
	private void sendMessage (Session session, Message msg) throws MessagingException {
		/* 
		 * SMTP or SMTPS: This takes too long!
		 */
		long t0 = System.currentTimeMillis();
		
		if ( session != null) {
			Transport tr = session.getTransport();
			tr.connect(getSMTPUsername(), getSMTPPassword());
			tr.sendMessage(msg, msg.getAllRecipients());
		}
		else {
			// SMTP - no tls
			Transport.send(msg);
		}
		long t1 = System.currentTimeMillis();
		L4JAudit.LOGD("Sent notification in " + (t1 - t0) + " ms.");
	}
	
	/**
	 * Return the Loj4J buffer as JSON.
	 * @return {@link JSONObject} of the form:
	 * <pre>{
	 *  "data": [
	 *  ["2016-02-04 10:11:56:101", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Checking if a sys admin pwd has been setup.&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:102", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: sys admin pwd has been setup. Checking for service descriptors @ C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:103", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Found descriptor C:\\Users\\vsilva\\.cloud\\CloudReports\\Profiles\\Default\\avaya-cfg.xml&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:104", "CloudServices", "DEBUG", "&lt;div black>IsNodeCongigured: Node has been configured.&lt;\/div>"],
	 *  ["2016-02-04 10:11:56:105", "NodeConfiguration", "WARN", "&lt;div style=\"color:black;background-color:yellow;\">Add URL: localhost is NOT a valid URL host name!&lt;\/div>"],
	 *  ...
	 * ]
	 * } </pre>
	 */
	JSONObject toJSON() {
		JSONObject root 			= new JSONObject();
		JSONArray data 				= new JSONArray();
		final SimpleDateFormat df 	= new SimpleDateFormat(LogManager.DATE_FORMAT);

		for (int i = 0; i < cb.length(); i++) {
			LoggingEvent ev = cb.get(i);
			JSONArray row = new JSONArray();
			
			// Source if the first part in the msg itself SOURCE:MESSAGE
			final String className = ev.getMessage().toString().substring(0, ev.getMessage().toString().indexOf(":"));
			
			// add an i (msec) so the events can be sorted by date.
			row.put(/*CyclicBufferAppender.*/df.format(new Date(ev.getTimeStamp() + i  )));	// Time
			row.put(className);										// Source
			row.put(ev.getLevel().toString());						// Level

			// stack trace: New coll span = 4
			String[] s = ev.getThrowableStrRep();
			
			if ( s != null) {
				row.put(ev.getMessage() + " <stack> " + CyclicBufferAppender.getThrowableAsHTML(s) + "</stack>");
			}
			else {
				row.put(ev.getMessage().toString().substring(ev.getMessage().toString().indexOf(":") + 1).trim());
			}
			data.put(row);
		}
		try {
			root.put("data", data);
		} 
		catch (JSONException e) {
		}
		return root;
		
	}

}
