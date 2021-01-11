package com.cloud.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * SSH command execution tool. Usage:
 * <pre>
 *  SSHExecResponse resp = SSHExec.execute("192.168.99.100", "docker", "tcuser", "ls -l /");
 *  System.out.println(resp);</pre>
 *  
 * @author VSilva
 *
 */
public class SSHExec {

	/** Prefix for  provate key path */
	public static final String KEY_PREFIX = "PKEY:";
	
	public static class SSHExecResponse {
		public int exitStatus;
		public String response;
		
		@Override
		public String toString() {
			return response;
		}

		public int getExitStatus() {
			return exitStatus;
		}

		public String getResponse() {
			return response;
		}
	}

	static public SSHExecResponse execute (final String host, final String user, final String identity, final String command) throws JSchException, IOException {
		String pwd 		= null;
		String kPath 	= null;
		
		if ( identity.startsWith(KEY_PREFIX)) {
			kPath = identity.replace(KEY_PREFIX, "");
		}
		else {
			pwd = identity;
		}
		//System.out.println("SSH h=" + host + " u:" + user + " p:" + pwd + " k:" + kPath + " cmd=" + command);
		return execute(host, user, kPath, pwd /*identity*/, command);
	}
	
	/**
	 * Execute a command via SSH.
	 * @param host Host name or ip.
	 * @param user ssh user.
	 * @param privKeyPath For private key authentication, full path to the key (if null will use a pwd).
	 * @param password ssh password (set to null if using a private key).
	 * @param command remote command to execute
	 * @return See {@link SSHExecResponse}
	 * @throws JSchException
	 * @throws IOException
	 */
	static public SSHExecResponse execute (final String host, final String user, final String privKeyPath, final String password, final String command) throws JSchException, IOException {
		SSHExecResponse resp 	= new SSHExecResponse();
		JSch jsch 				= new JSch();
		
		if ( privKeyPath != null ) {
			jsch.addIdentity(privKeyPath);
		}
		Session session			= jsch.getSession(user, host, 22);
		
		session.setPassword(password);
		session.setUserInfo(new SSHDefaultUserInfo());
		session.connect(5000);
	
	    Channel channel	= session.openChannel("exec");
	    
	    ((ChannelExec)channel).setCommand(command);
	    channel.setInputStream(null);
	    
	    InputStream in 	= channel.getInputStream();

	    channel.connect(5000);

	    // read response
	    byte[] tmp			= new byte[1024];
	    StringBuffer buf	= new StringBuffer();
	    
	    while (true) {
	    	while (in.available() > 0) {
	    		int i = in.read(tmp, 0, 1024);
	    		if (i < 0 ) {
	    			break;
	    		}
	    		buf.append(new String(tmp, 0, i));
	        }
	        if (channel.isClosed()){
	        	if (in.available() > 0) {
	        		continue;
	        	}
	        	resp.exitStatus = channel.getExitStatus();
	        	break;
	        }
	        try {
	        	Thread.sleep(100);
	        }
	        catch(InterruptedException ee) { 
	        	 break;
	        }
	    }
	    channel.disconnect();
	    session.disconnect();
	    resp.response = buf.toString();
	    return resp;
	}
	
	/**
	 * Convert UNIX top output to a {@link JSONObject} via {@link SSHExec}.
	 * @param host Remote host.
	 * @param user Remote user.
	 * @param identity Remote password.
	 * @return {"mem":{"total":"3880484","used":"1008380","free":"331256"},"cpu":{"sy":"1.6","id":"96.8","us":"1.6"},"tasks":{"running":"1","total":"189","sleeping":"188"}}
	 * @throws JSchException On SSH errors.
	 * @throws IOException On I/O errors.
	 * @throws JSONException On JSON errors.
	 */
	public static JSONObject top(final String host, final String user, final String identity) throws JSchException, IOException, JSONException {
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
		final String command 	= "top -n 1 -b 2>&1";
		
		SSHExecResponse resp  	= SSHExec.execute(host, user, identity, command);
		String out 				= resp.response;
		String[] temp			= out.split("\n");
		
		// Tasks: 189 total,   1 running, 188 sleeping,   0 stopped,   0 zombie
		String tasks 			= temp[1].replaceAll("[A-z%(): ]*", "");
		//%Cpu(s):  1.5 us,  1.5 sy,  0.0 ni, 95.4 id,  0.0 wa,  0.0 hi,  1.5 si,  0.0 st
		// 6.2,3.1,0.0,90.8,0.0,0.0,0.0,0.0
		String cpu 				= temp[2].replaceAll("[A-z%(): ]*", "");
		// KiB Mem :  3880484 total,   338220 free,  1003356 used,  2538908 buff/cache
		String mem				= temp[3].replaceAll("[A-z%():/ ]*", "");

		JSONObject root 		= new JSONObject();
		
		temp					= tasks.split(",");
		root.put("tasks", new JSONObject().put("total", temp[0] ).put("running", temp[1]).put("sleeping", temp[2]));
		
		temp					= cpu.split(",");
		root.put("cpu", new JSONObject().put("us", temp[0] ).put("sy", temp[1]).put("id", temp[3]));

		temp					= mem.split(",");
		root.put("mem", new JSONObject().put("total", temp[0] ).put("free", temp[1]).put("used", temp[2]));
		
		return root;
	}
	
}
