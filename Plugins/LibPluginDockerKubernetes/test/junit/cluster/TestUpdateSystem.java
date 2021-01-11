package junit.cluster;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.ZipTool;
import com.cloud.core.logging.Container;
import com.cloud.core.net.DropboxClient;
import com.cloud.core.net.DropboxClient.DropboxFile;
import com.cloud.core.types.CoreTypes;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUpdateSystem {

	// YAHOO SMTP w/ TLS converge_one@yahoo.com
	String proto = "smtps";
	String host =  "smtp.mail.yahoo.com";
	String port = "465";
	boolean startTls = false;
	boolean debug = true;
	String from = "converge_one@yahoo.com";
	String to = "vsilva@convergeone.com";
	String user = "converge_one@yahoo.com";
	String password = "Thenewcti1";
	
	static String ZIP_LOGS_PATH = CoreTypes.TMP_DIR + File.separator + "SAMPLE-CONTAINER_LOGS.zip";
	
	// DROPBOX - https://www.dropbox.com/home
	// User: converge_one@yahoo.com/Thenewcti1

	 /* DROPBOX - https://www.dropbox.com/developers/apps/info/qxkl5xyh5wlcbob
		 * User converge_one@yahoo.com/Thenewcti1
		 * 
		 * App: C1AS_AutoUpdate
		 * App key: qxkl5xyh5wlcbob
		 * App Secret: b127uczw1pld4t0
		 * Access Token: F1S6eC-N7sAAAAAAAAAADApR55PokUZoqQ909QXQR861ZX_qnZx-FfZvfFs1oEw-
		 * 
		 * Docs - https://www.dropbox.com/developers/documentation/http/documentation#files-list_folder
	*/
	
	static Map<String, String> conf = new HashMap<String, String>();
	
	static void zipLogs() throws IOException {
		ZipTool.zipFolder(Container.getDefautContainerLogFolder(), ZIP_LOGS_PATH);
	}

	@BeforeClass
	public static void init () {
		conf.put(DropboxClient.KEY_TOKEN, "F1S6eC-N7sAAAAAAAAAADApR55PokUZoqQ909QXQR861ZX_qnZx-FfZvfFs1oEw-");
	}
	
	@Test
	public void test01DropboxListFile() {
		try {
			
			DropboxClient db = new DropboxClient(conf);
			// list root path
			JSONObject files = db.listAsJSON("");
			
			System.out.println("Root files: " + files.toString(1));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void test02DropboxListFile1() {
		try {
			
			DropboxClient db = new DropboxClient(conf);
			// list root path
			List<DropboxFile> files = db.listFolder("");
			System.out.println("Root files: " + files);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test03DropboxDownloadTextFile() {
		try {
			
			DropboxClient db = new DropboxClient(conf);
			// list root path
			String dest = CoreTypes.TMP_DIR + File.separator + "test.ini";
			db.download("/test.ini", dest);
			
			System.out.println("Downloaded: test.ini to " + dest);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test04DropboxDownloadBinaryFile() {
		try {
			String name = "CloudConnectorAES01##release-1.1-20180525.war";
			
			DropboxClient db = new DropboxClient(conf);
			
			// list root path
			String dest = CoreTypes.TMP_DIR + File.separator + name;
			db.download("/" + name, dest);
			
			System.out.println("Downloaded: " + name + " to " + dest);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test05CopyBinaryFileToTomactWebApps() {
		try {
			String name = "CloudConnectorAES01##release-1.1-20180525.war";
			String dest = "C:/Program Files (x86)/Apache Software Foundation/Tomcat 7.0/webapps/" + name;
			
			String src = CoreTypes.TMP_DIR + File.separator + name;
			
			File f1 = new File(src);
			File f2 = new File(dest);
			
			Files.copy(f1.toPath(), f2.toPath());
			
			System.out.println("Copied: " + f1 + " to " + f2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
