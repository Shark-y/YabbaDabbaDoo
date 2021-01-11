package com.cloud.core.config;

import java.io.IOException;
import java.io.InputStream;

import com.cloud.core.io.IOTools;

/**
 * This class represents a file or form item that was received within a multipart/form-data POST request. 
 * 
 * @author VSilva
 *
 */
public class FileItem {
	final String name;
	final String fileName;
	final long size;
	final InputStream is;
	
	public FileItem(String name, String fileName, long size, InputStream is) {
		super();
		this.name = name;
		this.fileName = fileName;
		this.size = size;
		this.is = is;
	}
	
	public boolean isFormField () {
		return fileName == null;
	}
	
	/**
	 * Returns the name of the field in the multipart form corresponding to this file item.
	 * @return The name of the form field.
	 */
	public String getFieldName() {
		return name;
	}

	/**
	 * Returns the original filename in the client's filesystem, as provided by the browser (or other client software).
	 */
	public String getName() {
		return fileName;
	}

	public String getString(String encoding) throws IOException  {
		return (is != null) ? IOTools.readFromStream(is, encoding) : null;
	}
	
	/**
	 * Returns an {@link InputStream} that can be used to retrieve the contents of the file.
	 * @return An {@link InputStream} that can be used to retrieve the contents of the file.
	 */
	public InputStream getInputStream() {
		return is;
	}
	
	/**
	 * Returns the size of the file item.
	 * @return The size of the file item, in bytes.
	 */
	public long getSize () {
		return size;
	}
	
	@Override
	public String toString() {
		return String.format("name=%s fname=%s size=%d isform=%s", name, fileName, size, isFormField());
	}
}
