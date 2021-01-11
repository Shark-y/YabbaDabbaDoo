package com.rts.datasource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.cloud.core.db.JDBCClient;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus.Status;
import com.cloud.core.types.CoreTypes;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.IDataSource.DataSourceType;
import com.rts.datasource.db.DBDataSink;
import com.rts.datasource.db.DBDataSource;
import com.rts.datasource.ext.PrometheusDataSource;
import com.rts.datasource.fs.FileSystemDataSource;
import com.rts.datasource.media.JavaMailDataSource;
import com.rts.datasource.media.SMSTwilioDataSource;
import com.rts.datasource.sock.TCPSockDataSource;

/**
 * <pre>
 * &lt;dataSources>
	&lt;dataSource>
		&lt;name>CSPLIT Table&lt;/name>
		&lt;port>7000&lt;/port>
		&lt;description>CSPLIT Table Listener&lt;/description>
		&lt;format>
			&lt;header>&lt;/header>
			&lt;footer>F3|END_OF_RECORDS&lt;/footer>
			&lt;fieldsep>|&lt;/fieldsep>
			&lt;fields>F1,SPLIT,INQUEUE+INRING,AVAILABLE,ANSTIME/ACDCALLS,ABNCALLS,ACD,OLDESTCALL,ACDCALLS,ACDTIME/ACDCALLS,ABNTIME/ABNCALLS,AGINRING,ONACD,INACW,OTHER,INAUX,STAFFED,EWTHIGH,EWTMEDIUM,EWTLOW,DA_INQUEUE+DA_INRING,100*(ACCEPTABLE/CALLSOFFERED),SERVICELEVEL,CALLSOFFERED&lt;/fields>
		&lt;/format>
	&lt;/dataSource>
	&lt;dataSource>
		&lt;name>CVDN Table&lt;/name>
		&lt;port>7001&lt;/port>
		&lt;description>CVDN Table Listener&lt;/description>
		&lt;format>
			&lt;header>&lt;/header>
			&lt;footer>F3|END_OF_RECORDS&lt;/footer>
			&lt;fieldsep>|&lt;/fieldsep>
			&lt;fields>F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS&lt;/fields>
		&lt;/format>
	&lt;/dataSource>
&lt;/dataSources> </pre>

 * @author VSilva
 * 
 * @version 1.0.1 - 09/15/2017 Data source XML format deprecated in favor of JSON.
 * @version 1.0.2 - 11/23/2017 Made this class a singleton.
 * @version 1.0.3 - 06/14/2019 New methid getDataSources(type)
 *
 */
public class DataSourceManager {

	private static final Logger log = LogManager.getLogger(DataSourceManager.class);
	
	/** List of Data sources */
	final List<IDataSource> listeners;
	
	/** Thread pool used to run the TCP Servers in a seprate thread */
	private final ExecutorService pool;
	
	/** Path to the XML descriptor */
	private String basePath;
	
	/** XML descriptor name */
	private String fileName;
	
	/** Singleton instance */
	private static DataSourceManager SINGLETON;
	
	/**
	 * XML SAX Parser for the data source XML. This code cannot handle JDBC data sources.
	 * @deprecated 9/15/2017 XML no longer used to store data sources (JSON used instead). This is kept for compatibility.
	 * @author VSilva
	 */
	private class DataHandler extends DefaultHandler {
		private StringBuffer buffer;
		String name, description, header, footer, fieldSep, fields, recSep;
		int port;
		
		@Override
		public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
			buffer = new StringBuffer();
		}
		
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			String data = buffer.toString();
			if ( name.equals("name")) 			this.name = data;
			if ( name.equals("description")) 	description = data;
			if ( name.equals("port")) 			port = Integer.valueOf(data);
			if ( name.equals("header")) 		header = data;
			if ( name.equals("footer")) 		footer = data;
			if ( name.equals("fieldSep")) 		fieldSep = data;
			if ( name.equals("recSep")) 		recSep = data;
			if ( name.equals("fields")) 		fields = data;
			if ( name.equals("dataSource")) {
				try {
					addSocketListener(DataSourceType.SOCKET, this.name, port, description, new DataFormat(header, footer, fieldSep, recSep, fields, null), null);
				} catch (IOException e) {
					// TODO: Handle parse error
					e.printStackTrace();
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer != null) {
            	buffer.append(ch ,start ,length);
            }
		}
	}	
	
	/**
	 * Construct a {@link DataSourceManager} singleton instance.
	 * @param path JSON descriptor path.
	 * @param descriptorName JSON File name. For example: datasources (will load datasources.json)
	 * @return A {@link DataSourceManager}.
	 * @throws IOException
	 */
	public static synchronized DataSourceManager getInstance(String path, String descriptorName) throws IOException {
		if ( SINGLETON == null) {
			SINGLETON = new DataSourceManager(path, descriptorName);
		}
		return SINGLETON;
	}

	/**
	 * Construct an empty {@link DataSourceManager}.
	 * @return A {@link DataSourceManager}.
	 * @throws IOException
	 */
	public static synchronized DataSourceManager getInstance() {
		if ( SINGLETON == null) {
			SINGLETON = new DataSourceManager();
		}
		return SINGLETON;
	}
	
	/**
	 * Construct a default data source manager.
	 */
	private DataSourceManager() {
		listeners 	= new ArrayList<IDataSource>();
		pool 		= Executors.newCachedThreadPool();
	}

	/**
	 * Construct from an JSON descriptor. <b>Note: The XML format has been deprecated.</b>
	 * @param path JSON descriptor path.
	 * @param descriptorName JSON File name. For example: datasources (will load datasources.json)
	 * @throws IOException
	 */
	private DataSourceManager(String path, String descriptorName) throws IOException {
		this();
		basePath 	= path;
		fileName	= descriptorName;
		//parse(path, descriptorName);
	}
	
	/**
	 * Parse the listener XML (deprecated) from the file system. Current format is JSON.
	 * @param path Descriptor path: ${USERHOME}/CloudReports\Profiles\Default
	 * @param descriptorName file name (without an extension - for example datasources).
	 * @throws IOException on I/O errors.
	 */
	public void parse (String path, String descriptorName) throws IOException {
		InputStream is	= null;
		try {
			// Try JSON first: HOME\CloudReports\Profiles\Default\datasources.json
			String fullPath = path + File.separator + descriptorName + ".json";
			try {
				log.debug("Loading datasources from JSON " + fullPath);
				
				// Parse JSON {"dataSources" : [ {"name": "name1", "port", 1000, "description", "desc1"}, {"name": "name2", "port", 1001, "description", "desc2"}, ...]}
				JSONObject root = new JSONObject(IOTools.readFileFromFileSystem(fullPath));
				JSONArray array = root.optJSONArray("dataSources");
				
				for (int i = 0; i < array.length(); i++) {
					JSONObject ds 		= array.getJSONObject(i);
					final String type 	= ds.getString("type"); 
					
					if ( type.equals(DataSourceType.SOCKET.name()) || type.equals(DataSourceType.SOCKET_WITH_STORAGE.name()) ) {
						addSocketListener(DataSourceType.valueOf(type)
								, ds.getString("name")
								, ds.optInt("port")
								, ds.optString("description")
								, new DataFormat(ds.optJSONObject("format"))
								, null);
					}
					else if (type.equals(DataSourceType.DATABASE.name())) {
						addDataSource(new DBDataSource(ds));
					}
					else if (type.equals(DataSourceType.DATASTORE.name())) {
						addDataSource(new DBDataSink(ds));
					}
					else if (type.startsWith(DataSourceType.SMTP.name()) 
							|| type.equals(DataSourceType.POP3.name()) 
							|| type.startsWith(DataSourceType.IMAP.name()))	
					{
						addDataSource(new JavaMailDataSource(ds));
					}
					else if (type.equals(DataSourceType.SMS_TWILIO.name())) {
						addDataSource(new SMSTwilioDataSource(ds));
					}
					// 6/12/2019
					else if (type.equals(DataSourceType.FILESYSTEM.name())) {
						addDataSource(new FileSystemDataSource(ds));
					}
					else if (type.equals(DataSourceType.PROMETHEUS.name())) {
						addDataSource(new PrometheusDataSource(ds));
					}
					else {
						throw new Exception("Invalid data source type " + type + " for " + ds.getString("name"));
					}
				}
			} 
			catch (IOException e) {
				//e.printStackTrace();
				
				// No JSON ... try XML
				log.debug("Found No JSON. Trying XML datasources from " + path + " " + descriptorName + ".xml");
				
				SAXParser p = SAXParserFactory.newInstance().newSAXParser();
				is 			= IOTools.findStream(path, descriptorName + ".xml"); 
				/*
				if ( is == null) {
					throw new Exception("Missing resource " + descriptorName + " @ " + path);
				}*/
				if ( is != null) {
					p.parse(is, new DataHandler());
					is.close();
				}
				else {
					log.debug("No data sources found @ " + path + " " + descriptorName);
				}
			}
		}
		catch ( Exception e) {
			log.error("Parse data sources " + path + " " + descriptorName, e);
			throw new IOException(e);
		}
		finally {
			IOTools.closeStream(is);
		}
	}

	/**
	 * Save into the file system given a base path & file name.
	 * @throws IOException
	 * @throws JSONException 
	 */
	public void save () throws IOException {
		if ( basePath == null) 	throw new IOException("Save: Missing descriptor base path.");
		if ( fileName == null)	throw new IOException("Save: Missing descriptor name.");
		
		// 9/15/2017 save as JSON instead - String text 			= toXML();
		FileOutputStream fos 	= null;
		String path				= basePath + File.separator + fileName + ".json";
		try {
			String text 		= toJSON().toString(1);
			
			log.debug("Saving to " + path);
			fos = new FileOutputStream(path);
			fos.write(text.getBytes(CoreTypes.CHARSET_UTF8));
			fos.close();
		}
		catch (JSONException ex) {
			throw new IOException(ex);
		}
		finally {
			IOTools.closeStream(fos);
		}
	}

	public void reload () throws IOException {
		// stop all
		stopAll();
		
		// clear
		listeners.clear();
		
		// parse
		parse(basePath, fileName);
	}
	
	/**
	 * Add a TCP network (Socket) data source. This method only supports Socket data sources.
	 * @deprecated Use addDataSource (IDataspurce) instead.
	 * @param subType Either SOCKET or SOCKET_WITH_STORAGE.
	 * @param name Unique Name or Id.
	 * @param port TCP port.
	 * @param description Service description.
	 * @param fmt See {@link DataFormat}.
	 * @param listener OPTIONAL event sink {@link IBatchEventListener}.
	 * @throws IOException on TCP or I/O errors.
	 */
	public void addSocketListener (DataSourceType subType, String name, int port, String description, DataFormat fmt, IBatchEventListener listener) throws IOException {
		if ( containsDataSource(port)) {
			IDataSource l = getDataSource(port);
			
			if ( !l.getName().equals(name)) {
				throw new IOException(l.getName() + " @ port " + port + " already exists.");
			}
		}
		// stop (if online) & remove.
		removeDataSource(name, true);
		
		/*
		 * Will throw java.net.BindException: Address already in use: JVM_Bind if 2 services @ the same port
		 */
		TCPSockDataSource ds = new TCPSockDataSource(name, port, description, fmt);
		ds.setEventListener(listener);
		ds.setType(subType);
		addDataSource(ds);
	}

	public void removeDataSource (String name) throws IOException {
		removeDataSource(name, false);
	}
	
	/**
	 * Remove and shutdown a listener
	 * @param name Listener name/id.
	 * @param force If false will throw an error if online.
	 * @throws IOException
	 */
	public void removeDataSource (String name, boolean force) throws IOException {
		IDataSource listener = null;
		for (IDataSource ns : listeners) {
			if ( ns.getName().equals(name)) {
				listener = ns;
				break;
			}
		}
		if ( listener != null) {
			if ( listener.getStatus().getStatus() == Status.ON_LINE && !force) {
				throw new IOException("Cannot remove online listener " + name + ".");
			}
			listener.shutdown();
			if ( !listeners.remove(listener)) {
				throw new IOException("Failed to remove " + name);
			}
		}
	}
	
	/**
	 * Set a {@link IBatchEventListener} to ALL the TCP services for this manager.
	 * @param listener The event listener.
	 */
	public void setEventListener( IBatchEventListener listener) {
		for (IDataSource ns : listeners) {
			ns.setEventListener(listener);
		}
	}
	
	/**
	 * Add a new {@link IDataSource} of any type {@link DataSourceType}.
	 * @param ds The data source.
	 * @throws IOException If any errors.
	 */
	public void addDataSource (IDataSource ds) throws IOException {
		if ( containsDataSource(ds.getName())) {
			// 9/22/2017 allow to update a DS throw new IOException("Data source " + ds.getName() + " already exists.");
			removeDataSource(ds.getName());
		}
		if ( containsDataSource(ds.getPort()) && ds.getType() == DataSourceType.SOCKET) {
			throw new IOException("Listener @ port " + ds.getPort() + " already exists.");
		}
		log.debug("Adding DS " + ds.getName() + " @ port " + ds.getPort());
		listeners.add(ds);
	}
	
	public boolean containsDataSource(String name) {
		for (IDataSource ns : listeners) {
			if ( ns.getName().equals(name))
				return true;
		}
		return false;
	}

	public boolean containsDataSource(int port ) {
		for (IDataSource ns : listeners) {
			if ( ns.getPort() == port)
				return true;
		}
		return false;
	}

	public void startAll () {
		for (IDataSource listener : listeners) {
			pool.execute(listener);
		}
	}

	public void start (String name) {
		for (IDataSource listener : listeners) {
			if ( listener.getName().equals(name)) {
				//11/25/2017 Made this synchronus pool.execute(listener);
				listener.run();
			}
		}
	}

	public void stopAll () {
		for (IDataSource listener : listeners) {
			listener.stop();
		}
	}

	public void stop (String name) {
		for (IDataSource listener : listeners) {
			if ( listener.getName().equals(name)) {
				listener.stop();
			}
		}
	}

	public void shutdownAll () {
		log.debug("Shutting down " + listeners.size() + " data sources.");
		for (IDataSource listener : listeners) {
			listener.shutdown();
		}
		// shutdown the main pool
		pool.shutdownNow();
		
		// DB-ds: Unload JDBC drivers
		JDBCClient.unloadDrivers();
	}

	/**
	 * @deprecated 9/22/20217 XML format is deprecated. Use toJSON() instead.
	 * @return XML serialized data sources.
	 * @throws IOException if invalid data source name or port.
	 */
	public String toXML() throws IOException {
		StringBuffer buf = new StringBuffer("<dataSources>");
		for (IDataSource ns : listeners) {
			buf.append("\n" + ns.toXML());
		}
		buf.append("\n</dataSources>");
		return buf.toString();
	}
	
	/**
	 * @return An unmodifiable list if {@link IDataSource}.
	 */
	public List<IDataSource> getDataSources() {
		return Collections.unmodifiableList(listeners);
	}

	/**
	 * Get the TCP service as a JSON object including data sources.
	 * @return {"dataSources" : [ {"name": "name1", "port", 1000, "description", "desc1"}, {"name": "name2", "port", 1001, "description", "desc2"}, ...]}
	 * @throws JSONException
	 */
	public JSONObject toJSON() throws JSONException {
		JSONObject root	= new JSONObject();
		JSONArray ds 	= new JSONArray();
		for ( IDataSource tns : listeners) {
			ds.put(tns.toJSON());
		}
		root.put("dataSources", ds);
		return root;
	}

	/**
	 * Find a data source by its name.
	 * @param name Name/id to look up.
	 * @return {@link TCPSockDataSource}.
	 */
	public IDataSource getDataSource(String name) {
		if ( name == null)	return null;
		for ( IDataSource service : listeners) {
			if ( service.getName().equals(name)) {
				return service;
			}
		}
		return null;
	}

	/**
	 * Find a data source by port.
	 * @param port The port.
	 * @return {@link IDataSource} for port.
	 */
	public IDataSource getDataSource(int port ) {
		for (IDataSource ns : listeners) {
			if ( ns.getPort() == port)
				return ns;
		}
		return null;
	}
	
	/**
	 * Get data sources by type.
	 * @param type See {@link DataSourceType}
	 * @return List of data sources by type.
	 * @since 1.0.3
	 */
	public List<IDataSource> getDataSources( DataSourceType type) {
		List<IDataSource>  list = new ArrayList<IDataSource>();
		for (IDataSource ds : listeners) {
			if ( ds.getType() == type) {
				list.add(ds);
			}
		}
		return list;
	}
}
