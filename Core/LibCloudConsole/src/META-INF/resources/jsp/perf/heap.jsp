<%@page import="com.cloud.core.io.FileTool"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.cloud.core.types.CoreTypes"%>
<%@page import="java.util.Date"%>
<%@page import="com.sun.tools.hat.internal.model.Snapshot"%>
<%@page import="com.sun.tools.hat.internal.model.JavaClass"%>
<%@page import="com.sun.tools.hat.internal.parser.HprofReader"%>
<%@page import="java.io.File"%>
<%@page import="com.c1as.profiler.HeapDumper"%>
<%@page import="com.cloud.core.profiler.OSHeap"%>
<%@page import="com.cloud.core.profiler.OSMetrics"%>
<%@page import="com.cloud.core.provider.IHTMLFragment"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%!
	static final String TAG = "[HEAP]";

	static void LOGD(String text) {
		System.out.println(TAG + " " + text);
	}

	static void LOGE(String text) {
		System.err.println(TAG + " " + text);
	}
	
	private static long timerStart() {
		return System.currentTimeMillis();
	}

	private static void timerEnd(long start, String label) {
		LOGD(label + " "	+ (System.currentTimeMillis() - start) + " ms");
	}
	
	/**
	 * Get a Heap dump as an {@link IHTMLFragment}.
	 * @param fileName Name of the heap dump to view (format: dump_yyyyMMdd_hhmmss_SSS.hprof). If null a new dump will be generated in java.io.tmpdir.
	 * @return Heap dump HTML fragment (as an HTML TABLE).
	 */
	public static String getOSHeap(String contextPath, String fileName) throws Exception {
		// generate a temp file name for the heap dump.
		final String tmpDir 	= System.getProperty("java.io.tmpdir");
		final StringBuffer html = new StringBuffer(); 
		
		// dump it to the temp dir: java.io.tmpdir
		if ( fileName == null) {
			/*String*/ fileName = HeapDumper.dumpHeap(new File(tmpDir), true);
		}

		LOGD("Heap Dump @ " + fileName);
		
		// extract the date from the file name: path\dump_yyyyMMdd_hhmmss_SSS.hprof
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss_SSS");
		Date date 			= df.parse(fileName.substring(fileName.indexOf("_") + 1, fileName.lastIndexOf(".")));
		
		// read it
		long t = timerStart();
		Snapshot snap = HprofReader.readFile(fileName, true, 0);
		timerEnd(t, "Read time:");

		// must resolve
		t = timerStart();
		snap.resolve(true);
		timerEnd(t, "Resolve time:");

		JavaClass[] classes = snap.getClassesArray();

		LOGD("Total Classes: " + classes.length);

		// Display
		t = timerStart();
		int i = 0;
		
		html.append("<h2>Heap Dump <small><a href=\"" + contextPath + "/LogServlet?op=download&f=" + FileTool.getFileName(fileName) + "\">Download</a></small></h2>");
		html.append("\n" + fileName + "&nbsp;&nbsp;&nbsp;");
		html.append("\n<b>Date:</b> " + date);
		html.append("\n<b>Total Classes:</b> " + classes.length);
		html.append("\n<br><br>");
		html.append("\n<table id=\"tblHeap\" class=\"display\">");
		html.append("\n<thead><tr><th>#</th><th>Class Name</th><th>Instances</th><th>Size (bytes)</th></tr></thead>");
		html.append("\n<tbody>");
		
		for (JavaClass jClass : classes) {
			final int count = jClass.getInstancesCount(true);
			final long size = jClass.getTotalInstanceSize();

			// only display classes whose # of instances & size > 0 (filter junk)
			if (count > 0 && size > 0) {
				html.append("\n<tr><td>" + (i++) + "</td>"
						+ "<td>" + jClass.getName() + "</td>" 
						// FIXME The JS table sorter can't sort comma formatted numbers :( 
						+ "<td>" + count + "</td>"	// formatNumber(count)
						+ "<td>" + size + "</td>" 	// formatNumber(size)
						+ "</tr>");
			}
		}
		timerEnd(t, "Display time:");
		
		html.append("\n</tbody>");
		html.append("\n</table>");

		return html.toString();
	}
	
%>

<%
	String contextPath 	= getServletContext().getContextPath();
	String file			= request.getParameter("f");
	//String fileName		= null;
	
	// must prepend java.oi.tmpdir. File name format: dump_yyyyMMdd_hhmmss_SSS.hprof
	if ( file != null)  {
		//fileName	= file;
		file 		= CoreTypes.TMP_DIR + File.separator + file;
	}
%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">


<!--  common styles -->
<link rel="stylesheet" type="text/css" href="../../css/jquery.dataTables.css">


<style type="text/css">
body {
	font-family: Arial;
}

/*H2, B*/ BODY {
	padding-left: 10px;
}

</style>

<script type="text/javascript" src="../../js/jquery.js"></script>
<script type="text/javascript" src="../../js/log.js"></script>

 
<script type='text/javascript'>
</script>



<title>Heap</title>
</head>
<body>

	<!-- 
 	<h2>Heap Dump</h2>
 	-->
 	<%
 	try {
 		
 		out.println(getOSHeap(contextPath, file)); 
 	}
 	catch (Exception e) {
 		//e.printStackTrace();
 		out.println("<center><font color=red size=4>" + e.toString() + "</font></center>");
 	}
 	%>

	<script type="text/javascript" src="../../js/jquery.dataTables.js"></script>

	<script type="text/javascript">
	
        $().ready(function() {
        	$('#tblHeap').DataTable({
        		stateSave: true, 
        		"language": {
        			"lengthMenu": 'Display <select>'+
        				'<option value="100">100</option>'+
        				'<option value="500" selected>500</option>'+
        				'<option value="1000">1000</option>'+
        				'<option value="2000">2000</option>'+
        				'<option value="3000">3000</option>'+
        				'<option value="-1">All</option>' +
        				'</select>'
        			} 
        	});
        			        			
        });
            
    </script>
		
</body>
</html>