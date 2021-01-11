package junit.reports.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.rts.datasource.fs.FileSystemDataSource;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFileSysDS {

	static void LOGD(String text) {
		System.out.println("[FS] " + text);
	}

	static void LOGE(String text) {
		System.err.println("[FS] " + text);
	}
	
	/**
	 * List files from a file system path.
	 * @param path Full path in the file system.
	 * @param extensions Array of extensions to look for.
	 * @param nameFilters Array of regular expressions to match the file names against.
	 * @return An array of matching files.
	 * @since 1.0.1
	 */
//	public static File[] listFiles(String path, final String[] extensions, final String[] nameFilters, final boolean includeDirs) {
//		File dir = new File(path);
//		return dir.listFiles(new FileFilter() {
//			public boolean accept(File pathname) {
//				boolean foundExt 	= extensions == null; // 6/14/2019false;
//				boolean foundName	= nameFilters != null ? false : true;
//				
//				if ( extensions != null ) {
//					for ( String ext : extensions) {
//						final String extension = FileTool.getFileExtension(pathname.getPath());
//						if (  extension != null && extension.equals(ext) /* 6/14/2019 pathname.getName().contains("." + ext) */ ) { 
//							foundExt = true;
//							break;
//						}
//					}
//				}
//				if ( nameFilters != null ) {
//					for ( String name : nameFilters) {
//						boolean bool = pathname.getName().matches(name);
//						if ( bool ) { 
//							foundName = true;
//							break;
//						}
//					}
//				}
//				return foundExt && foundName ||  ( includeDirs && pathname.isDirectory());
//			}
//		});
//	}
	
	@Test
	public void test01FS() {
		try {
			FileSystemDataSource fs = new FileSystemDataSource("nfs", "nfs", "c:\\temp", FileSystemDataSource.DEFAULT_EXTENSIONS);
			JSONArray files = fs.listFiles();
			
			LOGD("FS:" + fs.getPath() + " " + files.toString());
			LOGD("FS:" + fs.getPath() + " 1: " + fs.listFiles("c:\\temp\\junk"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test02FS() {
		try {
			File[] files = FileTool.listFiles("c:\\temp", new String[] {"txt"}, null, true);
			JSONArray list 	= new JSONArray();

			// list using default extensions for now...
			for (File file : files) {
				JSONObject f = new JSONObject(file);
				
				// Add some useful extra info
				f.put("length", file.length());
				f.put("lastModified", file.lastModified());
				list.put(f);
			}
			
			LOGD("FS:" + list.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
