package com.rts.jsp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.rts.core.IDataService;
import com.rts.datasource.DataFormat;
import com.rts.datasource.DataSourceManager;
import com.rts.datasource.IDataSource;
import com.rts.datasource.IDataSource.DataSourceType;
import com.rts.datasource.db.DBDataSink;
import com.rts.datasource.db.DBDataSource;
import com.rts.datasource.ext.PrometheusDataSource;
import com.rts.datasource.fs.FileSystemDataSource;
import com.rts.datasource.media.JavaMailDataSource;
import com.rts.datasource.media.SMSTwilioDataSource;

/**
 * This class is meant to off load code from the data source (ds.jsp) file.
 * 
 * @author VSilva
 *
 */
public class JSPHandlerDataSource {

	public static void LOGD(String text) {
		System.out.println("[DS-DBG] " +text);
	}

	public static void LOGW(String text) {
		System.out.println("[DS-WRN] " +text);
	}

	public static void LOGE(String text) {
		System.err.println("[DS-ERR] " +text);
	}

	private static void setStatus (String[] params, String statusMessage, String statusType ) {
		params[0] = statusMessage;
		params[1] = statusType;
	}
	
	private static void sleep (long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}
	
	/** JSP page static configuration */
	private static Properties config = new Properties();
	
	private static void loadConfig () {
		try {
			InputStream is = JSPHandlerDataSource.class.getResourceAsStream("/configuration/jsp_datasources.ini");
			config.load(is);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static {
		loadConfig();
	}
	
	/**
	 * NAME1=Label1, NAME2=Label2,...
	 * @return SOCKET=Socket,SOCKET_WITH_STORAGE=Socket with Storage
	 */
	public static String getDataSourceTypes () {
		return config.getProperty("ds.names");
	}

	public static String getTitle () {
		return config.getProperty("ds.title");
	}
	
	/**
	 * Process the JSP action when the save button is clicked.
	 * @param request HTTP request
	 * @param action JSP action: save, delete, etc.
	 * @param service The {@link RealTimeStatsService}.
	 * @param OUT Output parameters: Status message, status type (INFO, ERROR, WARN, etc).
	 * @throws IOException on errors.
	 */
	public static void handle (HttpServletRequest request, String action, IDataService service, String[] OUT) throws IOException {
		DataSourceManager mgr 	= service.getDataSourceManager();
		
		if ( action.equals("add")) {
			//LOGD("Save: " + request.getParameterMap());
			final String type 		= request.getParameter("ds-type");
			final String name 		= request.getParameter("name");
			final String desc		= request.getParameter("desc");

			// DATASTORE, DATABASE
			final String db_drv	= request.getParameter("db_drv");
			final String db_url	= request.getParameter("db_url");
			final String db_usr	= request.getParameter("db_user");
			final String db_pwd	= request.getParameter("db_pwd");
			final String db_tbl	= request.getParameter("db_table");
			final String db_fld	= request.getParameter("db_flds");
			final String db_ref	= request.getParameter("db_refresh");
			
			try {
				if ( type.equals(IDataSource.DataSourceType.SOCKET.name()) || type.equals(IDataSource.DataSourceType.SOCKET_WITH_STORAGE.name())) {
					final String port	= request.getParameter("port");
					final String f_hd	= request.getParameter("fmt-hdr");
					final String f_ft	= request.getParameter("fmt-ftr");
					final String f_se	= request.getParameter("fmt-fsep");
					final String r_se	= request.getParameter("fmt-rsep");
					final String f_fl	= request.getParameter("fmt-flds");
					final String f_sf	= request.getParameter("fmt-sflds");
					final String f_wt	= request.getParameter("chkWipeTable");
					
					final DataSourceType dsType = DataSourceType.valueOf(type);
					final boolean sOptWipe 		= Boolean.parseBoolean(f_wt != null && f_wt.equals("on") ? "true" : "false");
					
					LOGD("Save: " + dsType + "," + name + "," + port + "," + desc + "," + f_hd + "," + f_ft + "," + f_se + "," + f_fl);
					
					DataFormat fmt = new DataFormat(f_hd, f_ft, f_se, r_se, f_fl, f_sf, sOptWipe );
					mgr.addSocketListener(dsType, name, Integer.valueOf(port), desc, fmt, service.getEventListener());
				}
				else if ( type.equals(IDataSource.DataSourceType.DATABASE.name())) {
					
					IDataSource ds = new DBDataSource(name, desc, db_drv, db_url, db_usr, db_pwd, db_tbl, db_fld, db_ref);
					mgr.addDataSource(ds);
				}
				else if ( type.equals(IDataSource.DataSourceType.DATASTORE.name())) {
					
					DBDataSink ds = new DBDataSink(name, desc, db_drv, db_url, db_usr, db_pwd, db_tbl, db_fld, db_ref);
					
					// validations: #1. At least 1 primary is required & others
					ds.run();		// connect
					ds.validate();	// Throws IOEx if fails
					ds.stop();
					
					mgr.addDataSource(ds);
				}
				// SMTP, SMTPS
				else if ( type.startsWith(IDataSource.DataSourceType.SMTP.name()) 
						|| type.equals(IDataSource.DataSourceType.POP3.name())
						|| type.startsWith(IDataSource.DataSourceType.IMAP.name()) ) 
				{
					final String mhost 	= request.getParameter(JavaMailDataSource.KEY_SMTP_HOST);
					final String mport 	= request.getParameter(JavaMailDataSource.KEY_SMTP_PORT);
					final String mfrom 	= request.getParameter(JavaMailDataSource.KEY_SMTP_FROM);
					final String muser  = request.getParameter(JavaMailDataSource.KEY_SMTP_USER);
					final String mpwd 	= request.getParameter(JavaMailDataSource.KEY_SMTP_PWD);
					final String mfolder = request.getParameter(JavaMailDataSource.KEY_SMTP_FOLDER);
					final String tls	= request.getParameter(JavaMailDataSource.KEY_SMTP_TLS); 	// 'on' or NULL
					
					final boolean startTLS 	= Boolean.parseBoolean(tls != null && tls.equals("on") ? "true" : "false");
					
					JavaMailDataSource ds 	= new JavaMailDataSource(type, name, desc, mhost, mport, mfrom, muser, mpwd, mfolder, startTLS, false );
					mgr.addDataSource(ds);
				}
				else if ( type.equals(IDataSource.DataSourceType.SMS_TWILIO.name())) {
					final String appId 	= request.getParameter(JavaMailDataSource.KEY_TWISMS_APPID);
					final String token 	= request.getParameter(JavaMailDataSource.KEY_TWISMS_TOKEN);
					final String nfrom 	= request.getParameter(JavaMailDataSource.KEY_TWISMS_FROM);
					//final String nto  	= request.getParameter(JavaMailDataSource.KEY_TWISMS_TO);
					
					SMSTwilioDataSource ds = new SMSTwilioDataSource(name, desc, appId, token, nfrom); //, nto);
					mgr.addDataSource(ds);
				}
				else if ( type.equals(IDataSource.DataSourceType.FILESYSTEM.name())) {
					final String path 			= request.getParameter(FileSystemDataSource.KEY_FS_PATH);
					final String exts 			= request.getParameter(FileSystemDataSource.KEY_FS_EXTS);
					
					FileSystemDataSource fsds 	= new FileSystemDataSource(name, desc, path, exts);
					
					mgr.addDataSource(fsds);
				}
				else if ( type.equals(IDataSource.DataSourceType.PROMETHEUS.name())) {
					final String url 			= request.getParameter(PrometheusDataSource.KEY_PM_URL);
					final String freq 			= request.getParameter(PrometheusDataSource.KEY_PM_FREQ);
					
					PrometheusDataSource pmds	= new PrometheusDataSource(name, desc, url, Long.valueOf(freq), service.getEventListener());
					mgr.addDataSource(pmds);
				}
				else {
					throw new IOException("Invalid data source type " + type + " for " + name);
				}
				mgr.save();
				setStatus(OUT, "Saved " + name, "INFO");
			}
			catch (Exception e) {
				setStatus(OUT, e.getMessage(), "ERROR");
			}
		}
		else if ( action.equals("del")) {
			final String name = request.getParameter("name");
			final String mode = request.getParameter("mode");	// PLUGIN
			final String id   = request.getParameter("id");
			
			LOGD("Delete " + name + " mode=" + mode + " id=" + id);
			
			try {
				String qs = mode != null && id != null ? "&mode=" + mode + "&id=" + id : "";
				mgr.removeDataSource(name);
				setStatus(OUT, "Removed " + name + ". <a href='ds.jsp?action=save" + qs + "'>Save required.</a>", "WARN");
			}
			catch (Exception e) {
				setStatus(OUT, e.getMessage(), "ERROR");
			}
		}
		else if ( action.equals("stop")) {
			final String name = request.getParameter("name");
			mgr.stop(name);
			
			// start/stop are async must wait a little
			sleep(500);
		}
		else if ( action.equals("start")) {
			final String name = request.getParameter("name");
			
			// 11/25/2017 This method is synchronous now.
			mgr.start(name); 

			// Data source manager start/stop methods are async must wait a little
			//sleep(500);
		}
		else if ( action.equals("save")) {
			mgr.save();
			setStatus(OUT, "Saved data sources.", "INFO");
		}
		
	}
}
