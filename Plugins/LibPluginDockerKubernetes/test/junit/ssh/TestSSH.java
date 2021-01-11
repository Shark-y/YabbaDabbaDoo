package junit.ssh;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.cloud.core.io.IOTools;
import com.cloud.ssh.SSHExec;
import com.cloud.ssh.Scp;
import com.cloud.ssh.SSHExec.SSHExecResponse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSSH {

//	static String host = "192.168.99.100";
//	static String user = "docker";
//	static String pwd = "tcuser";
	static String host = "192.168.40.84";
	static String user = "labadmin";
	static String pwd = "Thenewcti1!";

	static void LOGD(String text) {
		System.out.println("[SSH] "  + text);
	}

	@Ignore
	@Test
	public void test01CommandExec() {
		try {
			String command = "ls -l /";
			LOGD("Test command exec " + command);

			LOGD(SSHExec.execute(host, user, pwd, command).toString());
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test02Scp() {
		try {
			LOGD("Test command exec");

			final String yaml 		= "HTTPServerTools.getRequestBody(request)";
			final File tempFile		= File.createTempFile("test", ".yaml");
			String yamlPath			= tempFile.getAbsolutePath();
			String remote 			= "dummy.yaml";
			
			LOGD("SCP file:" + tempFile + " Remote: " + remote + " YAML " + yaml);
			
			IOTools.saveText(tempFile.getAbsolutePath(), yaml);

			Scp.copy(host, user, pwd, yamlPath, remote);

			// cleanup?
			if ( yamlPath != null) {
				System.out.println("CLEANUP " + yamlPath);
				new File(yamlPath).delete();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void test03CommandExec() {
		try {
			/*
top - 22:10:29 up 37 days, 10:19,  0 users,  load average: 0.22, 0.33, 0.23
Tasks: 191 total,   1 running, 190 sleeping,   0 stopped,   0 zombie
%Cpu(s):  3.1 us,  1.6 sy,  0.0 ni, 95.3 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
KiB Mem :  3880484 total,   339940 free,  1003576 used,  2536968 buff/cache
KiB Swap:        0 total,        0 free,        0 used.  2341336 avail Mem 

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
  839 root      20   0 1655640  72112  21672 S   6.7  1.9   1464:24 kubelet
19506 root      20   0  219060  87812  31652 S   6.7  2.3  66:38.16 kube-contr+
    1 root      20   0  191516   4480   2508 S   0.0  0.1  24:59.02 systemd
    2 root      20   0       0      0      0 S   0.0  0.0   0:00.62 kthreadd
    3 root      20   0       0      0      0 S   0.0  0.0   1:23.64 ksoftirqd/0
    5 root       0 -20       0      0      0 S   0.0  0.0   0:00.00 kworker/0:+
			 */
			String[] hosts = {host, "elmerfudd.cloudlab.com", "granny.cloudlab.com", "marvinthemartian.cloudlab.com", "porkypig.cloudlab.com" , "speedygonzales.cloudlab.com"};
			long t0 = System.currentTimeMillis();
			for (int i = 0; i < hosts.length; i++) {
				LOGD(SSHExec.top(hosts[i], user, pwd).toString());
			}
			long t1 = System.currentTimeMillis();
			LOGD("Collection took " + (t1-t0) + " ms.");
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test04CommandExecGCP() {
		try {
			String host = "34.75.200.19";
			String user = "VSilva";
			String pk = "C:\\cygwin\\home\\VSilva\\.ssh\\id_rsa1.ppk";
			
			//System.out.println("FOO=" + "PKEY:C:\\Users\\vsilva/.cloud/CloudAdapter\\certs\\KubeCluster5-key.ppk".replaceAll("\\\\", "/"));
			
			LOGD("Testing GCP via private key " + pk + " at " + user + "@" + host);
			
			SSHExecResponse r =  SSHExec.execute(host, user, pk, null, "ls -la");
			
			LOGD(r.toString());
			
			// option 2 sshpwd = PKEY:C:\\Users\\vsilva/.cloud/CloudAdapter\\certs\\KubeCluster4-key.ppk
			String pwd = SSHExec.KEY_PREFIX + "C:\\Users\\vsilva/.cloud/CloudAdapter\\certs\\KubeCluster4-key.ppk";
			LOGD("test commad with pwd: " + pwd);
			LOGD(SSHExec.execute(host, user, pwd, "ls -la").toString());
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void test05KeySecurityValidation() {
		try {
			String host = "34.75.200.19";
			String user = "VSilva";
			String pk = "C:\\cygwin\\home\\VSilva\\.ssh\\id_rsa1.ppk";
			//LOGD("Testing GCP via private key " + pk + " at " + user + "@" + host);
			String key = IOTools.readFileFromFileSystem(pk);
			String re = "^.*[^&()#@!.]{0,2048}$"; //"^.*{1,4096}$";
			boolean valid = key.matches(re);
			
			LOGD("Key valid:" + valid + "\n" + key);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
}
