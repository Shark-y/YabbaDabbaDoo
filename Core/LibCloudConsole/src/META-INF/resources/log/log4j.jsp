<%@page import="com.cloud.core.logging.L4JConfigurator"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.Set"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.util.Properties"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="org.apache.log4j.Level"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="org.apache.log4j.Logger"%>
<%@page import="java.util.Enumeration"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>

<%!

static void LOGD(String text) {
	System.out.println("[LOG4J] " + text);	
}

/**
 * Class that wraps a LOG4J logger. Used to get a sorted list of loggers.
 */
static class LoggerWrapper implements Comparable<LoggerWrapper> {
	private Logger logger;
	
	public LoggerWrapper(Logger logger) {
		this.logger = logger;
		if (this.logger.getLevel() == null) {
			this.logger.setLevel(Level.OFF);
		}
	}
	@Override
	public int compareTo(LoggerWrapper arg) {
		return logger.getName().compareTo(arg.getLogger().getName());
	}
	public Logger getLogger() {
		return logger;
	}
	public String getName() {
		return logger.getName();
	}
	public Level getLevel() {
		return logger.getLevel();
	}
	public void setLevel(Level level) {
		logger.setLevel(level);
	}
	
	@Override
	public String toString() {
		return logger.getName() + "=" + logger.getLevel();
	}
}

/**
 * Convert the Level of the wrapper logger to an HTML OPTION
 */
static String wrapperLevel2HTML(LoggerWrapper logger) {
	// ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF
	Level[] levels 		= new Level[] {Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN,  Level.ERROR, Level.FATAL, Level.OFF};
	StringBuffer html 	= new StringBuffer("<select name=\"" + logger.getName() + "\">");

	//System.out.println(logger);

	for (Level level : levels) {
		String selected =  ( logger.getLevel() != null && logger.getLevel().equals(level)) ? "selected" : "";
		html.append("<option value=\"" + level + "\" " + selected + ">" + level + "</option>");
	}
	html.append("</select>");
	return html.toString();
}

/**
 * Find a {@link LoggerWrapper} (Logger) from a list
 * @param logger List of {@link LoggerWrapper}
 * @param name Name to serach for.
 */
static LoggerWrapper findLogger( List<LoggerWrapper> loggers , String name) {
	for (LoggerWrapper wrapper : loggers) {
		if ( wrapper.getName().equals(name)) {
			return  wrapper;
		}
	}
	return null;
}
 
 static void saveLoggers (Properties props, String path) {
	 FileOutputStream fos = null;
	 if ( path == null ) return;
	 try {
		 LOGD("Save loggers @ " + path);
		 fos = new FileOutputStream(path);
		 props.store(fos, "LOG4J");
	 }
	 catch (Exception e) { }
	 finally {
		 try {
		 	if ( fos != null) fos.close();
		 } catch (Exception e) {}
	 }
 }
 
 %>

<%
	String action = request.getParameter("action");
	
	Enumeration<Logger> categories 	= Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
	List<LoggerWrapper> loggers 	= new ArrayList<LoggerWrapper>();
	Properties savedLoggers			= L4JConfigurator.log4jGetDefaultLoggers(); 
	
	Properties props				= new Properties();	// File saved
	//LOGD("Got saved:" + savedLoggers);
	
	// wrap log4j loggers 
	while (categories.hasMoreElements()) {
		Logger log = categories.nextElement();
		loggers.add(new LoggerWrapper(log));
	}
	
	// set levels from saved file
	for ( Object key : savedLoggers.keySet()) {
		LoggerWrapper wrapper 	= findLogger(loggers, key.toString());
		String level			= savedLoggers.getProperty(key.toString());
		//LOGD(key  + "=" + level + " wr:" + wrapper);
		if ( wrapper != null && !level.isEmpty() && !level.equals("OFF")) 	{
			wrapper.setLevel(Level.toLevel(level));
		}
		if ( wrapper != null ) {
			props.put(key.toString(), level);
		}
	}
	
	Collections.sort(loggers);

	if ( action != null ) {
		if (action.equals("update")) {
			// set logger levels
			Enumeration<String> names 	= request.getParameterNames();
			//Properties props			= new Properties();
			
			while ( names.hasMoreElements()) {
				String name 			= names.nextElement();			// logger name
				String level 			= request.getParameter(name);	// logger level
				LoggerWrapper wrapper 	= findLogger(loggers, name);
				
				//System.out.println(name + "->" + level  + " Old wrapper: " + wrapper);
				if ( wrapper != null) {
					wrapper.setLevel(Level.toLevel(level));
					props.put(name, level);
				}
			}
			//LOGD("Save " + props);
			saveLoggers(props, L4JConfigurator.LOGGER_FILE);
		}
	}

%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Log4J Logger Configuration</title>

<link rel="stylesheet" type="text/css" href="../css/jquery.dataTables.css">

<script type="text/javascript" src="../js/jquery.js"></script>
<script type="text/javascript" src="../js/jquery.dataTables.js"></script>
<script type="text/javascript" src="../js/log.js"></script>

<script>
function setLevel (level, filter) 
{
	var array = document.getElementsByTagName('select');
	for ( var i = 0 ; i < array.length ; i++ ) {
		var select 	= array[i];
		var options	= select.options;
		
		// skip auditors
		if ( select.name.toLowerCase().indexOf('audit') != -1) {
			continue;
		}
		for ( var j = 0 ; j < options.length ; j++ ) {
			//alert(options[j].value + " " + level);
			if ( options[j].value == level && (filter && (select.name.indexOf(filter) != -1) ) ) {
				options[j].selected = true;
			}
		} 
	}
}
	
</script>

</head>
<body>
<h2>Log4J Logger Configuration</h2>


<a href="javascript:setLevel('ERROR', 'com')">ALL ERROR</a>&nbsp;&nbsp;
<a href="javascript:setLevel('OFF', 'cloud')">CLOUD OFF</a>&nbsp;&nbsp;
<a href="javascript:setLevel('INFO', 'cloud')">CLOUD INFO</a>&nbsp;&nbsp;
<a href="javascript:setLevel('DEBUG', 'cloud')">CLOUD DEBUG</a>&nbsp;&nbsp;
<a href="javascript:setLevel('WARN', 'cloud')">CLOUD WARN</a>&nbsp;&nbsp;
<a href="javascript:setLevel('ERROR', 'cloud')">CLOUD ERROR</a>&nbsp;&nbsp;&nbsp;&nbsp;

<button onclick="document.getElementById('btnSubmit').click()">Save</button>

<p><%=L4JConfigurator.LOGGER_FILE %></p> 

<form method="post" action="log4j.jsp?action=update">
	<table id="table1" class="display compact">
		<thead>
		<tr>
			<th>Logger Name</th>
			<th>Logger Level</th>
		</tr>
		</thead>
		<tbody>
		<% for (LoggerWrapper object : loggers) {  %>
			<tr>
				<td><%=object.getName() %>
				<td><%=wrapperLevel2HTML(object) %>
			</tr>
		<%} %>
		</tbody>
	</table>
	<input id="btnSubmit" type="submit" value="Save">
</form>

<script type="text/javascript">

$(document).ready(function() {
	
	$('#table1').DataTable( {
		stateSave: true,
		"lengthMenu": [[-1, 10, 50, 100, 500], ["All", 10, 50, 100, 500]]
	});
		
});

</script>

</body>
</html>