package com.cloud.console.performance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloud.core.io.FileTool;
import com.cloud.core.logging.Container;

/**
 * Simple logic to tune up the Tomcat Container. It updates $TOMCAT_ROOT/conf/server.xml:
 * <ul>
 * <li> Sets maxThreads to 4096 for all Connectors.
 * <li> Comment the AccessLog valve to reduce excessive/unnecessary logging.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 2/3/2019 - Initial implementation.
 *
 */
public class TomcatAutoTunner {

	static void LOGD(final String text) {
		//System.out.println("[TUNER] " + text);
	}
	
	/**
	 * Tune it up: Set maxThreads to 4096 & remove the access log valve in server.xml.
	 * @return A result message: Backed config to .... Set maxtThreads=4096, Removed AccessLogValve. Container restart required.
	 * @throws Exception On any kind of I/O/XML error.
	 */
	public static String tuneUp () throws Exception {
		if ( Container.TOMCAT_HOME_PATH == null) {
			throw new IOException("Missing Tomcat home var (catalina.home)");
		}
		final String base	 	= Container.TOMCAT_HOME_PATH + File.separator + "conf";
		final String path 		= base + File.separator + "server.xml";
		final String bkFname 	= base + File.separator + FileTool.fileBackUpGetFileName(path);
		StringBuffer buf		= new StringBuffer();
		
		// Backup to server.xml.BK-YYYY-MM-DD
		LOGD("backup:" + path + " to " +  bkFname);
		FileTool.fileCopyTo(path, bkFname);
		
		
		// https://stackoverflow.com/questions/28837786/how-to-find-and-replace-an-attribute-value-in-a-xml
		// change content https://www.rgagnon.com/javadetails/java-0625.html
		
		// 1- Build the doc from the XML file
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(path));
		
		// 2- Locate the Connector node(s) with xpath
		XPath xpath 	= XPathFactory.newInstance().newXPath();
		NodeList nodes 	= (NodeList)xpath.evaluate("//Connector", doc, XPathConstants.NODESET);
		
		// 3- set maxThreads to 4096
		for (int idx = 0; idx < nodes.getLength(); idx++) {
			Node node 			= nodes.item(idx);
			((Element)node).setAttribute("maxThreads", "4096");
		}
		
		// Comment/Remove AccessLog Valve
		nodes 	= (NodeList)xpath.evaluate("//Valve[contains(@className, 'org.apache.catalina.valves.AccessLogValve')]",doc, XPathConstants.NODESET);
		for (int idx = 0; idx < nodes.getLength(); idx++) {
			Node node 			= nodes.item(idx);
			
			// comment it
			Comment comment = doc.createComment(nodeToString(node));
			node.getParentNode().insertBefore(comment, node);
			node.getParentNode().removeChild(node);
		}
		
		// 4- Save the result to a new XML doc
		LOGD("Updating " + path);
		buf.append("Backed config to " + bkFname + ". Set maxtThreads=4096, Removed AccessLogValve. Container restart required.");
		
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(new File(path))));
		
		removeAccessLogs();
		
		//StringWriter sw = new StringWriter();
		//xformer.transform(new DOMSource(doc), new StreamResult(sw));
		return buf.toString();
	}

	/*
	 * Remove acess_log*.txt
	 */
	private static void removeAccessLogs () throws IOException {
		File[] files = FileTool.listFiles(Container.getDefautContainerLogFolder(), new String[] {"txt"}, new String[] {".*access_log.*"});
		
		if ( files != null) {
			for (int i = 0; i < files.length; i++) {
				if ( ! files[i].delete()) {
					throw new IOException("Failed to delete " + files[i]);
				}
			}
		}
	}
	
	/*
	 * Converts an XML node to its XML string.
	 */
	static String nodeToString(Node node) throws Exception {
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw 	= new StringWriter();
		xformer.transform(new DOMSource(node), new StreamResult(sw));
		
		// minus the annoying : <?xml version='1.0' encoding='utf-8'?>
		return sw.toString().replaceFirst("<\\?xml.*?>", "");
	}
	

}
