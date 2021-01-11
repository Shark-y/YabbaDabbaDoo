package com.cloud.core.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.HTMLLayout;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.Transform;
import org.apache.log4j.spi.LoggingEvent;

class L4JAuditLayout extends HTMLLayout {
	
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	// URL of the origin
	private String nodeURL;
	
	@Override
	public String getHeader() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" + Layout.LINE_SEP);
		sbuf.append("<html>" + Layout.LINE_SEP);
		sbuf.append("<head>" + Layout.LINE_SEP);
		//sbuf.append("<title>" + getTitle() + "</title>" + Layout.LINE_SEP);
		sbuf.append("</head>" + Layout.LINE_SEP);
		sbuf.append("<body>" + Layout.LINE_SEP);
		
		if ( nodeURL != null) {
			sbuf.append("<B>Node:</B> " + nodeURL + Layout.LINE_SEP);
		}
		sbuf.append("<p/>" + Layout.LINE_SEP);
		sbuf.append("<table width='100%' style='border: 1px solid black'>" + Layout.LINE_SEP);
		sbuf.append("<tr>" + Layout.LINE_SEP);
		sbuf.append("<th>Time</th>" + Layout.LINE_SEP);
		sbuf.append("<th>Level</th>" + Layout.LINE_SEP);
		sbuf.append("<th>Category</th>" + Layout.LINE_SEP);
		sbuf.append("<th>Message</th>" + Layout.LINE_SEP);
		sbuf.append("</tr>" + Layout.LINE_SEP);
		
		return sbuf.toString();
	}

	public void setOriginURL (String value) {
		nodeURL = value;
	}
	
	@Override
	public String getFooter() {
		StringBuffer buf = new StringBuffer();
		buf.append("</table>" + Layout.LINE_SEP);
		buf.append("<br>" + Layout.LINE_SEP);
		buf.append("</body></html>");	
		return buf.toString();
	}

	@Override
	public String format(LoggingEvent event) {
		StringBuffer sbuf = new StringBuffer();

		// FindBugs 11/29/16 com.cloud.core.logging.L4JAuditLayout.format(LoggingEvent) uses the same code for two branches
		String rowStyle = event.getLevel().equals(Level.ERROR) || event.getLevel().equals(Level.FATAL) 
				? "background-color:red; color:white"
				: event.getLevel().equals(Level.WARN) 
					? "background-color:yellow; color:black"
					: event.getLevel().equals(Level.INFO)
						? "background-color:white; color:blue"
						: "background-color:white; color:black";

		sbuf.append(Layout.LINE_SEP + "<tr style='" + rowStyle + "'>" + Layout.LINE_SEP);

		// col1: Time tamp
		sbuf.append("<td>");
		// sbuf.append(event.timeStamp - LoggingEvent.getStartTime());
		sbuf.append(sdf.format(new Date(event.timeStamp)));
		sbuf.append("</td>" + Layout.LINE_SEP);

		// Col2: Debug level
		sbuf.append("<td title=\"Level\">");
		
		if (event.getLevel().equals(Level.DEBUG)) {
			sbuf.append("<font color=\"#339933\">");
			sbuf.append(Transform.escapeTags(String.valueOf(event.getLevel())));
			sbuf.append("</font>");
		} 
		else if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
			String color = event.getLevel().equals(Level.WARN) ? "#993300" : "white";
			// sbuf.append("<font color=\"#993300\"><strong>");
			sbuf.append("<font color=\"" + color + "\"><strong>");
			sbuf.append(Transform.escapeTags(String.valueOf(event.getLevel())));
			sbuf.append("</strong></font>");
		} 
		else {
			sbuf.append(Transform.escapeTags(String.valueOf(event.getLevel())));
		}
		sbuf.append("</td>" + Layout.LINE_SEP);

		// col 3: Category - Logger (only the name)
		String className = event.getLoggerName().contains(".") 
				? event.getLoggerName().substring(event.getLoggerName().lastIndexOf(".") + 1) 
				: event.getLoggerName();

		String escapedLogger 	= Transform.escapeTags(className);
		String message			= Transform.escapeTags(event.getRenderedMessage());
		String source 			= message != null && message.contains(":")
				? message.substring(0, message.indexOf(":")) 
				: null;
		sbuf.append("<td title=\"" + escapedLogger + " category\">");
		sbuf.append(source != null ? source : escapedLogger);
		sbuf.append("</td>" + Layout.LINE_SEP);


		// Col 4: message - Format SOURCE: MESSAGE
		sbuf.append("<td title=\"Message\">");
		sbuf.append(source != null ?  message.substring(message.indexOf(":") + 1) : message );
		sbuf.append("</td>" + Layout.LINE_SEP);
		sbuf.append("</tr>" + Layout.LINE_SEP);

		if (event.getNDC() != null) {
			sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : xx-small;\" colspan=\"6\" title=\"Nested Diagnostic Context\">");
			sbuf.append("NDC: " + Transform.escapeTags(event.getNDC()));
			sbuf.append("</td></tr>" + Layout.LINE_SEP);
		}

		// stack trace: New coll span = 4
		String[] s = event.getThrowableStrRep();
		if (s != null) {
			sbuf.append("<tr><td bgcolor=\"red\" style=\"color:White; font-size : small;\" colspan=\"4\">");
			appendThrowable(s, sbuf);
			sbuf.append("</td></tr>" + Layout.LINE_SEP);
		}

		return sbuf.toString();
	}
	
	void appendThrowable(final String[] s, final StringBuffer sbuf) {
		if (s != null) {
			int len = s.length;
			if (len == 0)
				return;
			sbuf.append(Transform.escapeTags(s[0]));
			sbuf.append(Layout.LINE_SEP);
			for (int i = 1; i < len; i++) {
				sbuf.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
				sbuf.append(Transform.escapeTags(s[i]));
				sbuf.append(Layout.LINE_SEP);
			}
		}
	}
	
}