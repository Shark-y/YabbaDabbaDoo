/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate the file transfer from local to remote.
 *   $ CLASSPATH=.:../build javac ScpTo.java
 *   $ CLASSPATH=.:../build java ScpTo file1 user@remotehost:file2
 * You will be asked passwd. 
 * If everything works fine, a local file 'file1' will copied to
 * 'file2' on 'remotehost'.
 *
 */
package com.cloud.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.cloud.ssh.SSHDefaultUserInfo;
import com.jcraft.jsch.*;


public class Scp {
	
	/**
	 * SSH remote copy from a local file to a remote host.
	 * @param host Remote host (receiver)
	 * @param user Remote user.
	 * @param identity Remote password.
	 * @param localPath Full path to the local file to be copied.
	 * @param remotePath Remote file path.
	 * @throws JSchException
	 * @throws IOException
	 */
	public static void copy(final String host, final String user, final String identity, final String localPath, final String remotePath) throws JSchException, IOException {

		FileInputStream fis = null;
		try {

			String lfile = localPath; // arg[0];
			String rfile = remotePath; // arg[1].substring(arg[1].indexOf(':') + 1);

			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);

			// username and password will be given via UserInfo interface.
			session.setPassword(identity);
			session.setUserInfo(new SSHDefaultUserInfo());
			
			session.connect();

			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			rfile = rfile.replace("'", "'\"'\"'");
			rfile = "'" + rfile + "'";
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				throw new IOException("SCP: Failed to ACK connection.");
			}

			File _lfile = new File(lfile);

			if (ptimestamp) {
				command = "T" + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					throw new IOException("SCP: Failed to ACK time stamp.");
				}
			}

			// send "C0644 filesize filename", where filename should not include
			// '/'
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " ";
			
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			
			if (checkAck(in) != 0) {
				throw new IOException("SCP: Failed to ACK file size.");
			}

			// send a content of lfile
			fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}
			out.close();

			channel.disconnect();
			session.disconnect();

		} 
		finally {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		}
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

}
