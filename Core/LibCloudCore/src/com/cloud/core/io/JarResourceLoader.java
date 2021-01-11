package com.cloud.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.cloud.core.io.FileTool;
import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;

/**
 * Load resources from web-app JAr files
 * 
 * @author VSilva
 *
 */
public class JarResourceLoader {

	private static final Logger  log 	= Logger.getLogger(JarResourceLoader.class);
	
	static void LOGD(final String text) {
		//System.out.println("[JAR] " + text);
		log.debug(text);
	}
	static void LOGE(final String text) {
		//System.err.println("[PLUGIN] " + text);
		log.error(text);
	}

	/**
	 * A text resource within a lib
	 * 
	 * @author VSilva
	 *
	 */
	public static class TextResource {
		final Path path;
		final String text;
		
		/**
		 * Construct 
		 * @param path Resource (file) name
		 * @param text File contents.
		 */
		public TextResource(Path path, String text) {
			super();
			this.path = path;
			this.text = text;
		}
		
		/**
		 * @return file name
		 */
		public Path getPath() {
			return path;
		}
		
		/**
		 * @return file contents
		 */
		public String getText() {
			return text;
		}
		@Override
		public String toString() {
			return path + " " + text;
		}
	}
	
	/*
	 * Guess the lib folder using class.getProtectionDomain().getCodeSource()
	 */
	private static String getLibFolder () throws URISyntaxException {
		// file:/C:/Temp/Workspaces/CloudServices/Cloud-UnifiedContactCenter/.metadata/.plugins/org.eclipse.wst.server.core/tmp7/wtpwebapps/Test/WEB-INF/classes/
		File cls 	= new File(JarResourceLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		File lib 	=  cls.isFile() 
				? new File(FileTool.getBasePath(cls.getPath()))
				: new File(cls.getPath() + File.separator + ".." + File.separator + "lib");
				
		LOGD("CLS folder:" + cls);
		LOGD("LIB folder:" + lib);
		return lib.getPath();
	}
	
	/*
	 * Fetch all JArs from lib folder
	 */
	private static File[] getJars () throws URISyntaxException {
		final String path 	= getLibFolder();
		File[] jars 		= FileTool.listFiles(path, new String[] {"jar"}, null);
		return jars;
	}

	/**
	 * Get a single resource from the class path
	 * @param fileName
	 * @return The first {@link TextResource} or NULL if not found
	 * @throws FileNotFoundException 
	 */
	public static final TextResource getTextResource (final String fileName, final int depth) throws FileNotFoundException {
		List<TextResource> list = getTextResources(fileName, depth);
		if ( list.size() == 0) {
			throw new FileNotFoundException(fileName);
		}
		return list.get(0);
	}
	
	/**
	 * Fetch file contents from all Jars in the app-lib folder at depth 1
	 * @param fileName File to scan for (withinb libs)
	 * @return A list of {@link TextResource}.
	 */
	public static final List<TextResource> getTextResources (final String fileName) {
		return getTextResources(fileName, 1);
	}
	
	/**
	 * Fetch file contents from all Jars in the app-lib folder 
	 * @param fileName File to scan for (withinb libs)
	 * @param depth Scan depth.
	 * @return A list of {@link TextResource}.
	 */
	public static final List<TextResource> getTextResources (final String fileName, final int depth) {
		List<TextResource> list = new ArrayList<TextResource>();
		try {
			File[] jars = getJars();
			
			if ( jars == null ) {
				LOGE("Got zero libs! Abort resource load");
				return list;
			}
	
			// load - loop thru jars
			for (File jar : jars) {
				//System.out.println(jar);

				try {
					// walk thru / within jar looking for fileName
					FileSystem fs 		= FileSystems.newFileSystem(Paths.get(jar.getPath()), null);
					Stream<Path> walk 	= Files.walk(fs.getPath("/"), depth); 
					
					for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
						Path path 					= it.next();
						Path file 					= path.getFileName();
						final String desiredFile 	= FileTool.getFileName(fileName);
			            //System.out.println("\t" + path + " f:" + file + " Desired file:" + desiredFile );
			            
			            // read plugin.json from JAR
			            if ( file != null && file.endsWith(desiredFile/* fileName*/)) {
			            	List<String> lines 	= Files.readAllLines(path);
			            	String text 		= IOTools.join(lines.toArray(), CoreTypes.LINE_SEPARATOR);
			            	TextResource tr		= new TextResource(file, text);
			            	
			            	//LOGD("ADDED TEXT " + tr.getPath() + " from " + jar);
			            	list.add(tr);
			            }

					}
					fs.close();
				} 
				catch (Exception e) {
					log.error("Load resource " + fileName + " from " + jar, e);
				}
			}

		} catch (Exception e) {
			log.error ( "Get resources", e);
		}
		return list;
	}
}
