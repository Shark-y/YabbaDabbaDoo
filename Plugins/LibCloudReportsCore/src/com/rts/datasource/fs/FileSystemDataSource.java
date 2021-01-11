package com.rts.datasource.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus;
import com.cloud.core.services.ServiceStatus.Status;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.BaseDataSource;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;

/**
 * File system data source capable of storing files on local disk.
 * 
 * @author VSilva
 * @version 1.0.0
 *
 */
public class FileSystemDataSource extends BaseDataSource implements IDataSource {

	private static final Logger log 	= LogManager.getLogger(FileSystemDataSource.class);
			
	/** Request variables */
	public static final String KEY_FS_PATH = "fsPath";
	public static final String KEY_FS_EXTS = "fsExts";

	/** Default file extensions displayed b this DS */
	//public static final String[] DEFAULT_EXTENSIONS = new String[] {"json", "txt", "js", "xml", "properties", "html", "yaml"};
	public static final String DEFAULT_EXTENSIONS = "json,txt,js,xml,properties,html,yaml";
	
	/** File system path */
	final String path;

	/** File extensions filter */
	final String exts;

	// This must match the JSON created by getBatches()
	private static final String FORMAT_FIELDS = "parent,parentFile,hidden,freeSpace,length,totalSpace,usableSpace,canonicalFile,directory,path,absoluteFile,file,absolute,name,canonicalPath,absolutePath,lastModified"; 

	/** Used to describe the {@link DataFormat} of this {@link IDataSource} */
	private final DataFormat fmt;

	/**
	 * Construct
	 * @param name data source name.
	 * @param description data source description.
	 * @throws IOException if the name or description or path are null.
	 */
	public FileSystemDataSource(final String name, final String description, final String path, final String exts) throws IOException {
		super(DataSourceType.FILESYSTEM, name, description);
		if ( path ==  null) {
			throw new IOException("File system path is required.");
		}
		if ( !FileTool.fileExists(path)) {
			throw new IOException(path + " must exist in local file system.");
		}
		if ( new File(path).isFile()) {
			throw new IOException(path + " is not a folder.");
		}
		this.path = path;
		this.exts = exts;
		fmt = new DataFormat(null, null, null, null, FORMAT_FIELDS , null);
	}

	/**
	 * Construct from a JSON object.
	 * @param ds
	 * @throws JSONException 
	 */
	public FileSystemDataSource ( JSONObject ds) throws JSONException {
		super( ds); 
		path = params.getString(KEY_FS_PATH);
		exts = params.optString(KEY_FS_EXTS);
		fmt = new DataFormat(null, null, null, null, FORMAT_FIELDS , null);
	}
	
	@Override
	public void run() {
		status.setStatus(Status.ON_LINE, "Up");		
	}

	@Override
	public void stop() {
		status.setStatus(Status.OFF_LINE, "");
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public String getName() {
//		return name;
//	}
//
//	@Override
//	public String getDescription() {
//		return description;
//	}

	@Override
	public int getPort() {
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalRecords() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setEventListener(IBatchEventListener l) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toXML() throws IOException {
		throw new IOException("toXML() is deprecated.");	
	}

	/**
	 * Serialize to JSON.
	 * @return { "type": TYPE, "name": "NAME", "description": "DESC", "params": {"fsPath": "PATH"} }
	 */
	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject root = super.toJSON();

		params.put(KEY_FS_PATH, path);
		params.put(KEY_FS_EXTS, exts);
		
		root.put("params", params);
		
		return root;
	}

	@Override
	public DataSourceType getType() {
		return type;
	}

	public String getPath () {
		return path;
	}

	public String getPath (boolean escape) {
		return escape ? path.replaceAll("\\\\", "/") : path;
	}
	
	/**
	 * List files in the file system using the default file extensions.
	 * @return <pre>[{
	"parent": "c:\\temp\\junk",
	"parentFile": "c:\\temp\\junk",
	"hidden": false,
	"freeSpace": 68882628608,
	"length": 2287,
	"totalSpace": 255953555456,
	"usableSpace": 68882628608,
	"canonicalFile": "C:\\Temp\\JUNK\\areachart.html",
	"directory": false,
	"path": "c:\\temp\\junk\\areachart.html",
	"absoluteFile": "c:\\temp\\junk\\areachart.html",
	"file": true,
	"absolute": true,
	"name": "areachart.html",
	"canonicalPath": "C:\\Temp\\JUNK\\areachart.html",
	"absolutePath": "c:\\temp\\junk\\areachart.html",
	"lastModified": 1497732642287
	}]</pre>
	 * @throws JSONException
	 */
	public JSONArray listFiles () throws JSONException {
		return listFiles(path);
	}
	
	public JSONArray listFiles (final String path) throws JSONException {
		JSONArray list 	= new JSONArray();
		String [] exts	= this.exts != null ? this.exts.split(",") : DEFAULT_EXTENSIONS.split(",");

		File[] files 	= FileTool.listFiles(path, exts , null, true );

		if ( files == null) {
			throw new JSONException(path + " returned no files");
		}
		
		// list using default extensions for now...
		for (File file : files) {
			JSONObject f = new JSONObject(file);
			
			// Add some useful extra info
			f.put("length", file.length());
			f.put("lastModified", file.lastModified());
			list.put(f);
		}
		return list;
	}

	/**
	 * Get all batches (files). This method is invoked by the System > Diagnostics page in the console to display DS records (files).
	 * @return JSON: [ JSON-FORMAT_FROM_LISTFILES, ...]
	 * @throws JSONException On JSON parse errors.
	 */
	public JSONArray getBatches() throws JSONException {
		// The keys in this JSON must ,match the fields in FORMAT_FIELDS
		JSONArray list = listFiles();
		return list;
	}
	
	/**
	 * Save a file at the data source path.
	 * @param path File path.
	 * @param buffer data payload.
	 * @throws IOException On I/O (file) errors.
	 */
	public void store (final String path, String buffer) throws IOException {
		final String fullPath = path; // + File.separator + name;
		if ( name == null || name.isEmpty()) {
			throw new IOException("A file name is required.");
		}
		if ( buffer == null || buffer.isEmpty()) {
			throw new IOException("File buffer cannot be empty.");
		}
		log.debug("FILESYSTEM: SAVE " + name + " @ " + fullPath);
		IOTools.saveText(fullPath, buffer);
	}
	
	/**
	 * Get contents of a text file.
	 * @param path File full path.
	 * @return File contents.
	 * @throws FileNotFoundException If the file is not found on disk.
	 * @throws IOException In I/O errors,
	 */
	public String getText (final String path) throws FileNotFoundException, IOException {
		final String fullPath = path; //  path + File.separator + name;
		return IOTools.readFileFromFileSystem(fullPath);
	}
	
	@Override
	public JSONObject getParams() {
		return params;
	}

}
