package com.cloud.core.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.cloud.core.io.Base64;
import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;

/**
 * Compression/decompression utilities. For example:<pre>
 *  String srcDir = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\Profiles\\AESDev1";
 *  String zipFile = "C:\\Users\\vsilva\\.cloud\\CloudAdapter\\AESDev1.zip";
 *  String zipDest = "C:\\Users\\vsilva\\.cloud\\CloudAdapter";
 *  
 *  // Zip a folder into a file
 *  zipFolder(srcDir, zipFile);
 *  
 *  // Zip folder into a B64 string
 *  String b64 = zipFolderToB64(srcDir); 
 *  
 *  // Store a B64 (zip) into a file.
 *  b64ToZipFolder(b64, zipFile);
 *  
 *  // unzip a file into a dest folder.
 *  unzip(zipFile, zipDest);</pre>
 * 
 * @author VSilva
 * @version 1.0.1 10/22/2017 Unzip bug fixes when zip entry is foo/bar.txt
 * @version 1.0.2 10/23/2017 Fixed http://acme208.acme.com:6091/issue/CLOUD_CORE-83
 * @version 1.0.3 08/06/2020 Check for malicious ZIPs
 */
public class ZipTool {
	
	/**
	 * Store a b64 zipped folder to a zip file (without decompressing).
	 * @param zipb64 Compressed folder in B64 format.
	 * @param zipFile Name of the zip file.
	 * @throws IOException
	 */
	static public void b64ToZipFile(final String zipb64, final String zipFile) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(zipFile);
			fos.write(Base64.decode(zipb64));
		}
		finally {
			IOTools.closeStream(fos);
		}
	}
	
	/**
	 * Zip a folder in base64 format.
	 * @param srcFolder Folder to compress.
	 * @return Base64 string of the zipped folder.
	 * @throws IOException
	 */
	static public String zipFolderToB64 ( final String srcFolder) throws IOException {
		String b64 					= null;
		ByteArrayOutputStream bos 	= new ByteArrayOutputStream();
		
		zipFolder(srcFolder, bos);
		
		b64 = Base64.encode(bos.toByteArray());
		bos.close();
		return b64;
	}
	
	/**
	 * Zip a folder.
	 * @param srcFolder Source folder
	 * @param destZipFile Destination (zip file).
	 * @throws IOException
	 */
	static public void zipFolder ( final String srcFolder, final String destZipFile) throws IOException {
		FileOutputStream fos	= new FileOutputStream(destZipFile);
		zipFolder(srcFolder, fos);
	}
	
	/**
	 * Zip a folder into an output stream.
	 * @param srcFolder The source folder to compress.
	 * @param os The target outpit stream.
	 * @throws IOException on any compression error.
	 */
	static public void zipFolder(final String srcFolder, final OutputStream os ) throws IOException {
		ZipOutputStream zip 	= null;
		zip 					= new ZipOutputStream(os);

		addFolderToZip("", srcFolder, zip);
		
		zip.flush();
		zip.close();
	}

	/**
	 * Add a file to a {@link ZipOutputStream}.
	 * @param path Base path of the file.
	 * @param srcFile A file or directory to compress. If folder then all the inner files will be added.
	 * @param zip The {@link ZipOutputStream}.
	 * @throws IOException on compression errors.
	 */
	static private void addFileToZip ( final String path, final String srcFile, final ZipOutputStream zip) throws IOException {
		File folder = new File(srcFile);
		
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} 
		else {
			byte[] buf 			= new byte[1024];
			FileInputStream in 	= null;
			int len;
			
			try {
				in = new FileInputStream(srcFile);
				
				//zip.putNextEntry(new ZipEntry(path + File.separator + folder.getName()));
				// 10/23/2017 Fix for http://acme208.acme.com:6091/issue/CLOUD_CORE-83
				// should work when zip,unzip from win,linux or vice-versa
				zip.putNextEntry(new ZipEntry(path + "/" + folder.getName())); 
				
				while ((len = in.read(buf)) > 0) {
					zip.write(buf, 0, len);
				}
			}
			finally {
				IOTools.closeStream(in);
			}
		}
	}

	static private void addFolderToZip (final String path, final String srcFolder, final ZipOutputStream zip) throws IOException {
		File folder 	= new File(srcFolder);
		String[] files 	= folder.list();
		
		if ( files != null ) {	// Findbugs
			for (String fileName : files) {
				if (path.equals("")) {
					// 10/23/2017 addFileToZip(folder.getName(), srcFolder + File.separator + fileName, zip);
					addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
				} else {
					//10/23/2017 addFileToZip(path + File.separator + folder.getName(), srcFolder + File.separator	+ fileName, zip);
					addFileToZip(path + "/" + folder.getName(), srcFolder + "/"	+ fileName, zip);
				}
			}
		}
	}
	
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath Full path to the zip file.
     * @param destDirectory Full path to the destination directory.
     * @throws IOException
     */
    public static void unzip ( final String zipFilePath, final String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        
        if (!destDir.exists()) {
            if ( ! destDir.mkdir() ) {
            	LOGE("Failed to create dir " + destDir);
            }
        }
        
        ZipInputStream zipIn 	= new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry 			= zipIn.getNextEntry();
        
        String childDir;
        
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            
            if (!entry.isDirectory()) {
            	/* this part is necessary because file entry can come before
                 * directory entry where is file located
                 * i.e.:
                 *   /foo/foo.txt
                 *   /foo/
                 */
                childDir = FileTool.getBasePath(entry.getName()); // dirpart(entry.getName());
                
                if( childDir != null ) {
                	mkdirs(destDirectory, childDir);
                }
                
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                if ( ! dir.mkdir() )  {
                	LOGE("Failed to create dir " + dir);
                }
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile ( final ZipInputStream zipIn, final String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private static void mkdirs ( final String parent ,final String child)
    {
      File d = new File(parent, child);
      if( !d.exists() ) {
    	  if ( !d.mkdirs()) {
    		  LOGE("Failed to create dir " + d);
    	  }
      }
    }

    /**
     * Unzip a Base64 string into a destination folder
     * @param b64Zip Zip data as base 64.
     * @param destDirectory Destination path.
     * @throws IOException on I/O errors.
     */
    public static void unzipB64 ( final String b64Zip, final String destDirectory) throws IOException {
    	long time 		= System.currentTimeMillis();
    	String zipFile 	= destDirectory + File.separator + "b64-" + time + ".zip";
    	
    	b64ToZipFile(b64Zip, zipFile);
    	unzip(zipFile, destDirectory);
    	
    	// delete temp zip
    	if ( ! (new File(zipFile)).delete() ) {
    		LOGE("Failed to delete " + zipFile);
    	}
    }
    
    private static void LOGE (final String text) {
    	System.out.println("[ZIPTOOL-ERROR] " + text);
    }
    
    /**
     * Check for malicious ZIPs
     * @param zipIn See {@link ZipInputStream}.
     * @throws IOException If the Zip is invalid.
     * @since 1.0.3
     */
	public static void checkForEvilZip (InputStream in) throws IOException {
        ZipInputStream zipIn 	= new ZipInputStream(in);

		ZipEntry entry 			= zipIn.getNextEntry();
        
        // iterates over entries in the zip file
        while (entry != null) {
        	final String name = entry.getName();
        	// evil?
        	if ( name.contains("..") ) {
        		throw new IOException("Invalid entry " + name);
        	}
        	entry = zipIn.getNextEntry();
        }
	}

}
