package com.cloud.core.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;


/**
 * File Utilities
 * 
 * <h2>Change log </h2>
 * <ul>
 * <li> 18/05/2018 New method listFiles to read files from the the file system.
 * <li> 10/18/2018 New methods sizeOFFolder(), sizeFfFile()
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.1
 *
 */
public class FileTool {

	public static final String FILE_EXT_XML = "xml";
	public static final String FILE_EXT_INI = "ini";
	
	/**
	 * Delete FS Tree. (Very dangerous!)
	 * @param folder Folder to delete (be careful!).
	 * @param recursive If true will delete the entire tree! If false will delete only files
	 * in the under folder.
	 * @return true if folder was deleted.
	 * @throws IOException
	 */
	static public boolean deleteTree(File folder, boolean recursive) throws IOException {
		File[] files = folder.listFiles();
		
		// delete files
		if ( files != null ) {	// Findbugs
			for (int i = 0; i < files.length; i++) {
				if (  files[i].isDirectory() && recursive) {
					deleteTree(files[i], recursive);
				}
				else {
					//System.out.println("del " + files[i]);
					if ( !files[i].delete() ) {
						throw new IOException("Unable to delete " + files[i]);
					}
				}
			}
		}
		// del folder
		return folder.delete();
	}

	/**
	 * Copy a directory tree.
	 * @param source Source folder.
	 * @param target Destination folder.
	 * @param opts See {@link StandardCopyOption}.
	 * @throws IOException
	 */
	public static void copyTree(final Path source, final Path target, final StandardCopyOption[] opts ) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>(){
			@Override
			public FileVisitResult preVisitDirectory(Path dir,	BasicFileAttributes arg1) throws IOException {
				Path dst = target.resolve(source.relativize(dir));
				Files.createDirectories(dst);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes arg1)	throws IOException {
				Path dst = target.resolve(source.relativize(file));
				
				Files.copy(file, dst, opts);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	/**
	 * Delete a single folder. Note this sub will not recurse into subfolders.
	 * It will fail if the main directory has sub-directories.
	 * @param folder {@link File} to delete. Subfolders will not be deleted.
	 * @return true if deleted.
	 * @throws IOException If an error occurred.
	 */
	static public boolean deleteFolder(File folder) throws IOException {
		if ( folder == null || !folder.exists())
			return false;
		
		File[] files = folder.listFiles();
		
		// delete files
		if ( files != null ) {	// Findbugs
			for (int i = 0; i < files.length; i++) {
				if ( !files[i].delete() ) {
					throw new IOException("Unable to delete " + files[i]);
				}
			}
		}
		// del folder
		return folder.delete();
	}

	/**
	 * Get the base of a file system path
	 * @param path File system path.
	 * @return For example c:/foo/bar/moo.ini => c:/foo/bar. NULL if there is no base path.
	 */
	public static String getBasePath(String path) {
		if ( path == null ) 
			return null;
		int i = path.lastIndexOf(File.separator); 
		int j = path.lastIndexOf("/");
		if ( i == -1 && j == -1) {
			return null;
		}
		int k = i > j ? i : j;
		return path.substring(0, k); 
	}

	/**
	 * Get the file name from a full path. Returns empty if the path has no file name (c:\path\)
	 * @param path file system path
	 * @return For example: c:/foo/bar/moo.ini => moo.ini OR empty for c:\path\
	 */
	public static String getFileName(String path) {
		if ( path == null)
			return null;
		int i = path.lastIndexOf(File.separator); 
		int j = path.lastIndexOf("/");
		if ( i == -1 && j == -1) {
			return path;
		}
		int k = i > j ? i : j;
		return path.substring(k + 1, path.length());
	}

	/**
	 * Get the file name from a full path (no extension).
	 * @param path file system path
	 * @return For example: c:/foo/bar/moo.ini => moo
	 */
	public static String getFileNameWithoutExtension(String path) {
		String name = getFileName(path);
		return name != null && name.indexOf(".") != -1 ? name.substring(0, name.lastIndexOf(".")) : name;
	}

	/**
	 * Get the file extension from a path.
	 * @param path file name or full path.
	 * @return Extension, OR null if path is null OR empty string if missing.
	 */
	public static String getFileExtension(String path) {
		if ( path == null) return null;
		String name = getFileName(path);
		return name != null && name.indexOf(".") != -1 ? name.substring(name.lastIndexOf(".") + 1, name.length() ) : "";
	}
	
	/**
	 * Get the default temp directory.
	 * @return Java system property java.io.tmpdir
	 */
	public static String getTempDir() {
		return System.getProperty("java.io.tmpdir");
	}

	/**
	 * File exists?
	 * @param path Full file system path.
	 * @return True if the file exists
	 */
	public static boolean fileExists(String path) {
		File f = new File (path);
		return f.exists();
	}
	
	/**
	 * Rename a file
	 * @param path Original path.
	 * @param newPath new path.
	 * @return True if rename ok.
	 * @throws IOException
	 */
	public static boolean fileRename(String path, String newPath) throws IOException {
		File f 		= new File (path);
		File dest	= new File(newPath);
		boolean ret = f.renameTo(dest);
		if ( ! ret ) throw new IOException("Failed to rename " + path + " to " + newPath);
		return true;
	}

	/**
	 * Backup a file with a default name ORIGINAL-PATH.BK-{YYYY-MM-DD}
	 * @param path Full path to backup. <b>Note: The original file will be removed.</b>
	 * @return The name of the new file.
	 * @throws IOException On I/O errors.
	 */
	public static boolean fileBackUp(String path) throws IOException {
		String newName = fileBackUpGetFileName(path);
		String newPath = FileTool.getBasePath(path) + File.separator + newName;
		return fileRename(path, newPath);
	}

	/**
	 * Copy a file from a source location to a destination without removing the original.
	 * @param path Full path of the source file.
	 * @param newPath Full path of the destination file.
	 * @return True when the copy completes. If the destination exists it will be replaced.
	 * @throws IOException Any error as described by {@link Files} copy(Path, Path, Options).
	 */
	public static boolean fileCopyTo(String path, String newPath) throws IOException {
		File f 		= new File (path);
		File dest	= new File(newPath);
		Files.copy(f.toPath(), dest.toPath(), new CopyOption[] {StandardCopyOption.REPLACE_EXISTING});
		return true;
	}
	
	/**
	 * Get the default name used for file back ups
	 * @param path Full file path to back up.
	 * @return Default backup name ORIGINAL-PATH.BK-{YYYY-MM-DD}
	 */
	public static String fileBackUpGetFileName(String path) {
		return getFileName(path) + ".BK-" + ( new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis())));
	}
	
	/**
	 * List files from a file system path. <b>This method excludes directories</b>
	 * @param path Full path in the file system.
	 * @param extensions Array of extensions to look for.
	 * @param nameFilters Array of regular expressions to match the file names against.
	 * @return An array of matching files.
	 * @since 1.0.1
	 */
	public static File[] listFiles(String path, final String[] extensions, final String[] nameFilters) {
		return listFiles(path, extensions, nameFilters, false);
	}

	/**
	 * List files from a file system path.
	 * @param path Full path in the file system.
	 * @param extensions Array of extensions to look for.
	 * @param nameFilters Array of regular expressions to match the file names against.
	 * @param includeDirs If true include directories in the result.
	 * @return An array of matching files.
	 * @since 1.0.1
	 */
	public static File[] listFiles(String path, final String[] extensions, final String[] nameFilters, final boolean includeDirs) {
		File dir = new File(path);
		return dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				boolean foundExt 	= extensions == null; // 6/14/2019false;
				boolean foundName	= nameFilters != null ? false : true;
				
				if ( extensions != null ) {
					for ( String ext : extensions) {
						final String extension = getFileExtension(pathname.getPath());
						if (  extension != null && extension.equals(ext) /* 6/14/2019 pathname.getName().contains("." + ext) */ ) { 
							foundExt = true;
							break;
						}
					}
				}
				if ( nameFilters != null ) {
					for ( String name : nameFilters) {
						boolean bool = pathname.getName().matches(name);
						if ( bool ) { 
							foundName = true;
							break;
						}
					}
				}
				return foundExt && foundName || ( includeDirs && pathname.isDirectory());
			}
		});
	}

	/**
	 * Get the size of an entire directory using {@link Files}.
	 * @param path Folder full path.
	 * @return folder size in bytes.
	 * @throws IOException on I/O errors.
	 */
	public static long sizeOfDirectory (final String path) throws IOException {
		final AtomicLong size = new AtomicLong();
		
		Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
			public java.nio.file.FileVisitResult visitFile(Path path, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
				size.addAndGet(attrs.size());
				return FileVisitResult.CONTINUE;
				
			};
		});
		return size.longValue();
	}

	/**
	 * Get the size of a file.
	 * @param path Full path of the file.
	 * @return File size in bytes.
	 */
	public static long sizeOfFile(String path) {
		return new File(path).length();
	}
	
}
