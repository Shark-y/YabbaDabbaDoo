package com.rts.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* 9/4/2019 apache commons FU removed
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
*/
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.console.JSPLoggerTool;
import com.cloud.console.servlet.FileUpload;
import com.cloud.core.config.FileItem;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudServices;
import com.cloud.core.services.ServiceStatus;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;
import com.rts.datasource.ext.PrometheusDataSource;
import com.rts.ui.Dashboard;
import com.rts.ui.Dashboard.Branding;
import com.rts.ui.Dashboard.Metric;

/**
 * Misc utilities used by servlets/JSPs.
 * @author vsilva
 *
 */
public class ServiceUtils {

	private static final Logger log = LogManager.getLogger(ServiceUtils.class);
	
	/**
	 * Dump all parameters from an HTTP servlet/JSP request to STDOUT.
	 * @param label A string label used to describe the output.
	 * @param request {@link HttpServletRequest}.
	 */
	public static void dumpHTTPServletRequestParams (String label, HttpServletRequest request) {
		Set<Map.Entry<String, String[]>> params = request.getParameterMap().entrySet();
		System.out.println("-- HTTP request params for: " + label);
		for ( Map.Entry<String, String[]> entry : params) {
			System.out.println(entry.getKey() + " = " + IOTools.join(entry.getValue() , ",") );
		}
		System.out.println("-----------------------");
	}
	
	/**
	 * Search a branding logo from a dashboard and write the image buffer into the HTTP response.
	 * @param dash {@link Dashboard} with {@link Branding} information.
	 * @param response HTTP response.
	 * @throws ServletException
	 */
	public static void brandingFetchLogo ( Dashboard dash, HttpServletResponse response) throws ServletException {
		if ( dash.getBranding() == null || dash.getBranding().getLogo() == null) {
			return;
		}
		// no logo
		if ( dash.getBranding().getLogo().isEmpty()) {
			return;
		}
		// 1. search WEB-APP/images first
		InputStream is = null;
		try {
			// C:/PATH/Cloud-UnifiedReports/CloudRealTimeReports/WEB-INF/classes/
			String clsPath 	= IOTools.getResourceAbsolutePath("/");
			String webPath	= clsPath.replaceFirst("WEB-INF/classes/", "");
			String logo		= dash.getBranding().getLogo();		// file name
			String path 	= webPath + "images/" + logo;		// full path

			// 2. Not found: search  $HOME/.cloud/CloudReports/Profiles/Deafult/logos
			if (! FileTool.fileExists(path)) {
				path = brandingGetDefaultLogoFolder() + File.separator + logo;

				// give up
				if (! FileTool.fileExists(path)) {
					throw new ServletException("Invalid path " + path);
				}
			}
			
			// write to the HTTP response as an image
			response.setContentType("image/" + FileTool.getFileExtension(logo));
				
			is = new FileInputStream(path);
			IOTools.pipeStream(is, response.getOutputStream());
		} 
		catch (IOException e) {
			log.error("brandingFetchLogo", e);
		}
		finally {
			IOTools.closeStream(is);
		}
	}
	
	/**
	 * Get logo names from the FS.
	 * @return File names array.
	 */
	public static String[] brandingGetLogosFromFileSystem () {
		String path = brandingGetDefaultLogoFolder() ;
		File dir 	= new File(path);
		
		if ( !dir.exists()) {
			log.warn("BrandingGetLogosFromFileSystem Creating folder " + path);
			IOTools.mkDir(path);
		}

		return dir.list();
	}
	
	/**
	 * Get logos from the file system as a set of HTML OPTIONs.
	 * @param dashb invoking {@link Dashboard} which {@link Branding} logo is used to select a default.
	 * @return HTML OPTIONs w/ logo names from the file system,
	 */
	public static String brandingListLogosAsHTMLOptions (Dashboard dashb) {
		StringBuffer html 	= new StringBuffer();
		String[] names 		= brandingGetLogosFromFileSystem();
		String selected 	= dashb != null && dashb.getBranding() != null && dashb.getBranding().getLogo() != null
				? dashb.getBranding().getLogo() : "";
				
		for (String name : names) {
			html.append("\n<option value=\"" + name + "\" " + (name.equals(selected) ? "selected" : "") + ">" + name + "</option>");
		}
		return html.toString();
	}
	
	/**
	 * Get file upload items from the {@link HttpServletRequest}using Apache Commons {@link FileUpload} 
	 * @param request The HTTP request.
	 * @return A list of {@link FileItem}
	 * @throws FileUploadException If error occurs parsing the request.
	 */
	/* 9/4/2019 apache commons removed 
	static public List<FileItem> getFileItems (HttpServletRequest request ) throws FileUploadException {
		// Create a factory for disk-based file items
		List<FileItem> uploadItems 	= new ArrayList<FileItem>();
		FileItemFactory factory 	= new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		List<FileItem> items = upload.parseRequest(request);		
		for (FileItem fileItem : items) {
			if ( ! fileItem.isFormField()) {
				uploadItems.add(fileItem);
			}
		}
		return uploadItems;
	} */

	/**
	 * Handle a logo file upload from the Dash JSP/Branding servlet.
	 * @param request
	 * @throws FileUploadException
	 * @throws IOException
	 */
	static public void brandingHandleLogoUpload ( HttpServletRequest request ) throws /*FileUploadException, */ Exception {
		OutputStream os = null;
		try {
			// upload files
			// 9/4/2019 commons fileupload removed - List<FileItem> items = getFileItems(request);
			
			// logo folder C:\Users\vsilva\.cloud\CloudReports\Profiles\Default\logos
			final String folder 					= brandingGetDefaultLogoFolder();

			// {keyRange=[name=keyRange fname=null size=3 isform=true], met-name2=[name=met-name2 fname=null size=8 isform=true], met-name3=[name=met-name3 fname=null size=8 isform=true], met-name1=[name=met-name1 fname=null size=13 isform=true], listener=[name=listener fname=null size=13 isform=true], met-desc1=[name=met-desc1 fname=null size=13 isform=true], met-name6=[name=met-name6 fname=null size=8 isform=true], met-th6=[name=met-th6 fname=null size=22 isform=true], met-th5=[name=met-th5 fname=null size=23 isform=true], met-name7=[name=met-name7 fname=null size=5 isform=true], met-name4=[name=met-name4 fname=null size=13 isform=true], met-name5=[name=met-name5 fname=null size=9 isform=true], met-th7=[name=met-th7 fname=null size=0 isform=true], title=[name=title fname=null size=28 isform=true], met-desc7=[name=met-desc7 fname=null size=11 isform=true], met-desc6=[name=met-desc6 fname=null size=15 isform=true], met-th2=[name=met-th2 fname=null size=0 isform=true], met-desc5=[name=met-desc5 fname=null size=16 isform=true], met-widget5=[name=met-widget5 fname=null size=5 isform=true], met-th1=[name=met-th1 fname=null size=27 isform=true], met-desc4=[name=met-desc4 fname=null size=20 isform=true], met-widget6=[name=met-widget6 fname=null size=5 isform=true], met-desc3=[name=met-desc3 fname=null size=9 isform=true], met-th4=[name=met-th4 fname=null size=0 isform=true], met-widget7=[name=met-widget7 fname=null size=5 isform=true], brand-logo=[name=brand-logo fname=null size=26 isform=true], met-desc2=[name=met-desc2 fname=null size=9 isform=true], met-th3=[name=met-th3 fname=null size=22 isform=true], action=[name=action fname=null size=3 isform=true], key=[name=key fname=null size=5 isform=true], heading=[name=heading fname=null size=4 isform=true], brand-file=[name=brand-file fname=bg_new.jpg size=34580 isform=false], met-widget1=[name=met-widget1 fname=null size=10 isform=true], met-widget2=[name=met-widget2 fname=null size=10 isform=true], met-widget3=[name=met-widget3 fname=null size=10 isform=true], met-widget4=[name=met-widget4 fname=null size=5 isform=true], brand-bgcol=[name=brand-bgcol fname=null size=6 isform=true], met-type1=[name=met-type1 fname=null size=6 isform=true], met-type2=[name=met-type2 fname=null size=6 isform=true], met-type5=[name=met-type5 fname=null size=6 isform=true], met-type6=[name=met-type6 fname=null size=6 isform=true], met-type3=[name=met-type3 fname=null size=6 isform=true], met-type4=[name=met-type4 fname=null size=6 isform=true], met-type7=[name=met-type7 fname=null size=6 isform=true]}
			final Map<String, List<FileItem>> items = FileUpload.parseMultiValuedRequest(request, false, folder);
			
			// bad (web.xml) ?
			if ( items.size() == 0 ) {
				final String msg		= "Unable to extract items from HTTP request. If the content type is multipart/form-data this may indicate an invalid web descriptor (web.xml).";
				final IOException e 	= new IOException(msg);
				JSPLoggerTool.JSP_LOGE("REPORTS", msg, e);
				throw e;
			}
			
			// create folder if missing
			if ( !FileTool.fileExists(folder)) {
				IOTools.mkDir(folder);
			}
			// 1st file only: brand-file=[name=brand-file fname=bg_new.jpg size=34580 isform=false]
			//FileItem item = items.get(0);
			FileItem item = items.get("brand-file").get(0);
			
			//System.out.println("f0=" + item.getName() + " S:" + item.getSize() + " folder=" + folder);
			
			if ( item.getName().isEmpty()) {
				throw new IOException("A file is required.");
			}
			// save it
			os = new FileOutputStream(folder + File.separator + item.getName());
			IOTools.pipeStream(item.getInputStream(), os);
			os.close();
		} 
		finally {
			IOTools.closeStream(os);
		}
	}
	
	/**
	 * Get Default logo folder.
	 * @return $HOME\.cloud\CloudReports\Profiles\Default\logos
	 */
	static public String brandingGetDefaultLogoFolder () {
		return CloudServices.getNodeConfig().getDefaultProfileBasePath() + File.separator + "logos";
	}
	
	/**
	 * JSP helper to check if a LOGO OPTION from the dash JSP should be marked as selected.
	 * @param dashb {@link Dashboard} object that contains the {@link Branding}.
	 * @param logo Logo file name.
	 * @return "selected" if logo matches the dashboard branding logo else it returns ""
	 */
	public static String brandingLogoAsSelected (Dashboard dashb, String logo) {
		return dashb != null 
				&& dashb.getBranding() != null 
				&& dashb.getBranding().getLogo() != null 
				&& dashb.getBranding().getLogo().equals(logo) ? "selected" : "";
	}

	/**
	 * Convert a {@link List} to e1,e2,... trimming elements
	 * Note: This won't trim elements: Arrays.toString(list.toArray()).replaceAll("[\\[\\]]", "")
	 * @param list A {@link List} of objects.
	 * @return e1,e2,....e(n)
	 */
	public static String listToCSV(List<?> list) {
		// This will NOT trim elements. Don'u use
		Object[] array 	= list.toArray();
		if ( array.length == 0) return "";
		
		StringBuffer buf = new StringBuffer(array[0].toString());
		
		for (int i = 1; i < array.length; i++) {
			buf.append("," + array[i].toString().trim());
		}
		return buf.toString();
	}
	
	/**
	 * Clone the original batch of the original by using the the {@link DataFormat} storage fields.
	 * @param batch JSON, for example: {"batchFormat":{"footer":"F3","recSep":"LF","storageFields":"","fieldSep":"\\|","header":"F0","fields":"SkillID,EWTLow,EWTMedium,EWTHigh,OldesCall"},"batchDate":1507116836701,"batchData":[{"EWTMedium":12,"EWTHigh":5,"SkillID":1,"EWTLow":86,"OldesCall":27},...],"listenerName":"Arvado2"}
	 * @param fmt The data source {@link DataFormat}.
	 * @return A new batch (clone with only storage fields) to be sent to the storage micro service.
	 * @throws IOException if there is an error building the storage JSON batch.
	 */
	static JSONObject storageBuildBatch ( final JSONObject batch, final DataFormat fmt) throws IOException {
		// f1,f2,...
		String storageFlds = fmt.getStorageFields();
		if ( storageFlds == null || storageFlds.isEmpty()) {
			return batch;
		}
		JSONObject storage 		= null;
		try {
			storage 			= new JSONObject(batch.toString());

			// [{"EWTMedium":12,"EWTHigh":5,"SkillID":1,"EWTLow":86,"OldesCall":27},...]
			JSONArray oldData 	= (JSONArray)storage.remove("batchData");
			JSONArray newData	= new JSONArray();
			String[] fields 	= storageFlds.split(",");
			
			for (int i = 0; i < oldData.length(); i++) {
				// {"EWTMedium":12,"EWTHigh":5,"SkillID":1,"EWTLow":86,"OldesCall":27}
				JSONObject oldRow = oldData.getJSONObject(i);
				JSONObject newRow = new JSONObject();
				
				for (int j = 0; j < fields.length; j++) {
					if ( fields[j].contains("=") ) {
						// auto generated field NAME=VALUE where VALUE=$DATETIME|$INT
						String[] tmp 	= fields[j].split("=");
						newRow.put(tmp[0], storageAutoGenerate(tmp[1]));
					}
					else {
						newRow.put(fields[j], oldRow.get(fields[j]));
					}
				}
				newData.put(newRow);
			}
			// insert new data.
			storage.put("batchData", newData);
		} catch (JSONException e) {
			log.error("Build Storage Batch: " + e.toString());
			return batch;
		}
		return storage;
	}
	
	/** A {@link SimpleDateFormat} used to generate SQL dates only */
	private static final SimpleDateFormat SQL_DFMT = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	
	/**
	 * @param spec $DATETIME or $INT or any constant
	 * @return A constant or an SQL Date/time (YYYY-MM-dd HH:mm:ss) or an INT (System.currentTimeMillis())
	 * @throws IOException 
	 */
	private static Object storageAutoGenerate(final String spec) throws IOException {
		if ( !spec.startsWith("$")) {
			return spec;	// constant
		}
		// $DATETIME | $INT 
		if( spec.contains("DATETIME")) {
			return SQL_DFMT.format(new Date(System.currentTimeMillis()));
		}
		if( spec.contains("INT")) {
			return System.currentTimeMillis();
		}
		throw new IOException("Unsupported auto generated spec " + spec);
	}

	/**
	 * Validate a {@link Dashboard}: metrics and more.
	 * @param ds DataSource.
	 * @param dash {@link Dashboard} to validate.
	 */
	public static void validateDashboard (IDataSource ds, Dashboard dash, boolean force) throws IOException, JSONException {
		final String key 		= dash.getKey();
		final String[] fields 	= ds.getType() == IDataSource.DataSourceType.PROMETHEUS 
				? ds.getParams().getString("@fields").split(",")
				: ds.getFormat().getFields().split(",");
		
		// Check 4 valid key. KEY must exist in fields
		boolean found = false;
		for (String field : fields) {
			if ( field.equals(key)) {
				found = true;
				break;
			}
		}
		if ( ! found) {
			throw new IOException ( "Invalid Display By field " + key);
		}
		// check status
		ServiceStatus status = ds.getStatus();
		
		if ( status.getStatus() != ServiceStatus.Status.ON_LINE) {
			throw new IOException ( "Data source " + ds.getName() + " is off line."); 
		}
		
		if  ( ds.getType() == IDataSource.DataSourceType.PROMETHEUS) {
			PrometheusDataSource pds = (PrometheusDataSource)ds;
			
			if (pds.getMetricCount() == 0 ) {
				throw new IOException ( ds.getName() + " requires a node restart");
			}
			
			if ( force ) {
				// this takes a long time
				prometheusValidate((PrometheusDataSource)ds, dash);
			}
		}
		else {
			// validate metrics: they must exist in the fields
			List<Metric> metrics = dash.getMetrics();
			
			for (Metric metric : metrics) {
				found = false;
				for (String field : fields) {
					if ( metric.getName().equals(field)) {
						found = true;
						break;
					}
				}
				if ( ! found) {
					throw new IOException ("Invalid metric " + metric.getName());
				}
			}
		}
	}
	
	public static void prometheusValidate (PrometheusDataSource ds, Dashboard dash) throws IOException{
		final String range  	= dash.getKeyRange();
		List<Metric> metrics 	= dash.getMetrics();
		
		for ( Metric metric : metrics) {
			// container_cpu_load_average_10s{namespace=\"westlake-dev\"}
			final String query = range != null & !range.isEmpty() ? metric.getName() + "{" + range + "}" : metric.getName(); 
			ds.validate(query);
		}
	}
	
}
