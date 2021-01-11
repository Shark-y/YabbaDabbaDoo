package com.cloud.console.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import com.cloud.core.config.FileItem;

/**
 * File upload helper using the Servlet 3.0 API.
 * 
 * <li>http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
 * <li>http://www.codejava.net/java-ee/servlet/java-file-upload-example-with-servlet-30-api
 * 
 * <pre>
 * &lt;!DOCTYPE html>
&lt;html lang="en">
    &lt;head>
        &lt;title>File Upload&lt;/title>
        &lt;meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    &lt;/head>
    &lt;body>
        &lt;form method="POST" action="upload" enctype="multipart/form-data" >
            File:
            &lt;input type="file" name="file" id="file" /> &lt;br/>
            Destination:
            &lt;input type="text" value="/tmp" name="destination"/>
            &lt;/br>
            &lt;input type="submit" value="Upload" name="upload" id="upload" />
        &lt;/form>
    &lt;/body>
&lt;/html>

POST /fileupload/upload HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; 
boundary=---------------------------263081694432439
Content-Length: 441
-----------------------------263081694432439
Content-Disposition: form-data; name="file"; filename="sample.txt"
Content-Type: text/plain

Data from sample file
-----------------------------263081694432439
Content-Disposition: form-data; name="destination"

/tmp
-----------------------------263081694432439
Content-Disposition: form-data; name="upload"

Upload
-----------------------------263081694432439--

 *  </pre>
 * @author VSilva
 *
 */
public class FileUpload {

	/**
	 * This class represents a file or form item that was received within a multipart/form-data POST request. 
	 * 
	 * @author VSilva
	 *
	 */
//	public static class FileItem {
//		final String name;
//		final String fileName;
//		final long size;
//		final InputStream is;
//		
//		public FileItem(String name, String fileName, long size, InputStream is) {
//			super();
//			this.name = name;
//			this.fileName = fileName;
//			this.size = size;
//			this.is = is;
//		}
//		
//		public boolean isFormField () {
//			return fileName == null;
//		}
//		
//		/**
//		 * Returns the name of the field in the multipart form corresponding to this file item.
//		 * @return The name of the form field.
//		 */
//		public String getFieldName() {
//			return name;
//		}
//
//		/**
//		 * Returns the original filename in the client's filesystem, as provided by the browser (or other client software).
//		 */
//		public String getName() {
//			return fileName;
//		}
//
//		public String getString(String encoding) throws IOException  {
//			return (is != null) ? IOTools.readFromStream(is, encoding) : null;
//		}
//		
//		/**
//		 * Returns an {@link InputStream} that can be used to retrieve the contents of the file.
//		 * @return An {@link InputStream} that can be used to retrieve the contents of the file.
//		 */
//		public InputStream getInputStream() {
//			return is;
//		}
//		
//		/**
//		 * Returns the size of the file item.
//		 * @return The size of the file item, in bytes.
//		 */
//		public long getSize () {
//			return size;
//		}
//		
//		@Override
//		public String toString() {
//			return String.format("name=%s fname=%s size=%d isform=%s", name, fileName, size, isFormField());
//		}
//	}
	
	/**
	 * Parse the request received within a multipart/form-data POST request. 
	 * @deprecated This method cannot handle multi values from HTML lists (select) , etc
	 * @param request HTTPP POST request.
	 * @param saveFiles If true will try to save the files in the given folder.
	 * @param saveFolder Folder where to save the files.
	 * @return A {@link List} of {@link FileItem}.
	 * @throws IOException on I/O errors.
	 * @throws ServletException on HTTP errors.
	 */
	public static List<FileItem> parseRequest(HttpServletRequest request, final boolean saveFiles, final String saveFolder) 
			throws IOException, ServletException 
	{
		final List<FileItem> items = new ArrayList<FileItem>();
		
		for (Part part : request.getParts()) {
			final String fileName 	= extractFileName(part);
			final FileItem item 	= new FileItem(part.getName(), fileName, part.getSize(), part.getInputStream());
			items.add(item);
			
			if ( saveFiles && (fileName != null) && !fileName.isEmpty() ) {
				part.write(saveFolder + File.separator + fileName);
			}
		} 
		return items;
	}

	/**
	 * Parse the request received within a multipart/form-data POST request. This method can handle multi-valued lists (HTTP select) <pre>
	 * -----------------------------117631447428527
	 * Content-Disposition: form-data; name="multivals_CALL_CENTER01_03_agentStateReasonCodes"
	 * 
	 * ----------------------------117631447428527
	 * Content-Disposition: form-data; name="CALL_CENTER01_03_agentStateReasonCodes"
	 * 
	 * foo1:bar1
	 * -----------------------------117631447428527
	 * Content-Disposition: form-data; name="CALL_CENTER01_03_agentStateReasonCodes"
	 * 
	 * foo2:bar2
	 * -----------------------------117631447428527</pre>
	 * @param request HTTPP POST request.
	 * @param saveFiles If true will try to save the files in the given folder.
	 * @param saveFolder Folder where to save the files.
	 * @return A {@link List} of {@link FileItem}.
	 * @throws IOException on I/O errors.
	 * @throws ServletException on HTTP errors.
	 */
	public static Map<String, List<FileItem>> parseMultiValuedRequest(HttpServletRequest request, final boolean saveFiles, final String saveFolder) 
			throws IOException, ServletException 
	{
		final Map<String, List<FileItem>> items = new HashMap<String, List<FileItem>>();
		
		for (Part part : request.getParts()) {
			final String fileName 	= extractFileName(part);
			final FileItem item 	= new FileItem(part.getName(), fileName, part.getSize(), part.getInputStream());
			
			// add
			if ( !items.containsKey(part.getName())) {
				final List<FileItem> list = new ArrayList<FileItem>();
				list.add(item);
				items.put(part.getName(), list);
			}
			else {
				// append
				items.get(part.getName()).add(item);
			}
			
			if ( saveFiles && (fileName != null) && !fileName.isEmpty() ) {
				part.write(saveFolder + File.separator + fileName);
			}
		} 
		return items;
	}
	
	/**
     * Extracts file name from HTTP header content-disposition
    */
    public static String extractFileName(Part part) {
        final String contentDisp = part.getHeader("content-disposition");
        final String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length()-1);
            }
        }
        return null;
    }
}
