package com.cloud.core.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

//import com.cloud.console.servlet.FileUpload.FileItem;
import com.cloud.core.config.ConfigItem;
import com.cloud.core.config.FileWidget;
import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * File Upload widget helper code.
 * 
 * @author VSilva
 *
 */
public class FileWidget {

	private static final Logger log = LogManager.getLogger(FileWidget.class);
	
	/** Max size of a file upload item 20K */
	public static final int MAX_FILE_UPLOAD_LEN 	= 20480;

	/**
	 * Process a file upload item. Store its value into the file upload folder: ${HOME}\.cloud\${PRODUCT}\Profiles\${PROFILE}\files.
	 * Where the file name is: upload_{FILE_NAME}
	 * @param item The file upload item.
	 * @param config The service configuration. See {@link ServiceConfiguration}.
	 * @return file name. Format: upload_[FILE_NAME]
	 * @throws IOException if there is an I/O error storing the data.
	 */
	public static String upload(final FileItem item, final ServiceConfiguration config) throws IOException {
		// upload files folder C:\Users\vsilva\.cloud\CloudReports\Profiles\Default\files
		final String folder 	= ServiceConfiguration.getDefaultFileFolder();
		final String field 		= item.getFieldName();

		if ( item.getName() == null || item.getName().isEmpty()) {
			log.error("Process file upload: Invalid/empty file name for item " + field);
			return null;
		}
		
		// Remove the service prefix from the field name (if possible) CALL_CENTER04_09_callLogCustomFields => 04_09_callLogCustomFields
		// 10/3/2019 final String fileName 	= config.getId() != null ? field.replaceFirst(config.getId(), "") : field;
		final String fileName 	= item.getName();
		
		// file name format: upload_[FIELD_NAME]
		final String value		= "upload_" + fileName; 
		final String path		= folder + File.separator + value;
		

		// create folder if missing
		if ( !FileTool.fileExists(folder)) {
			IOTools.mkDir(folder);
		}

		// read file data...
		final String data 		= IOTools.readFromStream(item.getInputStream());
		
		// check size limit
		if ( data.length() > MAX_FILE_UPLOAD_LEN) {
			throw new IOException("File upload size limit exceeded for " + fileName + ": " + data.length() + " > " + MAX_FILE_UPLOAD_LEN);
		}
		
		// save file
		IOTools.saveText(path, data);

		return value;
		// set the file contents as the value for the property.
		//wrapperSetProperty( config.getId() , true, field, value, config);
	}

	/**
	 * Set the value for a File {@link ConfigItem} or upgrade from a previous widget type when changed to type 'File'
	 * @param item The configuration item {@link ConfigItem}.
	 * @param baseKey The name (id) of the configuration item.
	 * @param filePath The file upload folder. Defaults to upload %USERPROFILE%\.cloud\CloudReports\Profiles\[PROFILE]\files
	 * @param fileName The name of the file to process. It may be different from the configuration item key.
	 * @throws IOException If the file name is empty or there is an I/O error writting the file
	 */
	public static void setValue(ConfigItem item, final String baseKey, final String filePath, String fileName) throws IOException  {
		
		if ( fileName == null || fileName.isEmpty()) {
			log.warn("File upload item " + baseKey + " value (file name) is invalid (cannot be empty).");
			return;
		}
		// if the item.value is not empty and does not start w/ upload_ then it may be a widget upgrade?
		// 1/2/2019 if ( !fileName.startsWith("upload_")  ) {
		if ( !fileName.startsWith("upload_") || !FileTool.fileExists(filePath) || !FileTool.fileExists(filePath + fileName) ) {
			fileName = "upload_" + baseKey; // item.key;
			
			log.warn("File upload item " + baseKey + " value (file name) appears invalid: " + item.value);
			log.warn("File upload storing data as " + filePath + fileName);
			
			// create folder if missing
			if ( !FileTool.fileExists(filePath)) {
				IOTools.mkDir(filePath);
			}

			// Store data as file upload
			IOTools.saveText(filePath + fileName, item.value);
			item.value = fileName;
			//return;
		}
		//System.out.println("*** LOAD FILE:" + getDefaultFileFolder() + File.separator + item.value);
		if ( item.multiValues == null) {
			item.multiValues = new Properties();
		}
		// the key is item.value (file name)
		item.multiValues.put(item.value, IOTools.readFileFromFileSystem(filePath + fileName));
		
	}
}
