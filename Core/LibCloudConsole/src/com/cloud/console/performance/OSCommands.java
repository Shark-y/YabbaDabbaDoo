package com.cloud.console.performance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;

import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;

/**
 * An OS command line utility using the {@link Runtime} class.
 * @author VSilva
 * @version 1.0.0
 */
public class OSCommands {

	private static final String LINE_SEP = System.getProperty("line.separator");
	
	/**
	 * Execute an OS command using the {@link Runtime} class and return is STDOUT.
	 * @param command OS command to exeute: Win32 or Linux.
	 * @return Command stdout.
	 * @since 1.0.0
	 * @throws IOException
	 */
	public static String execute (String command ) throws IOException {
		Process p 					= Runtime.getRuntime().exec(command);
		ByteArrayOutputStream out 	= new ByteArrayOutputStream();
		IOTools.pipeStream(p.getInputStream(), out);
		
		return out.toString(CoreTypes.DEFAULT_ENCODING);
	}

	/**
	 * 	Parse a Netstat command on Windows or linux.
	 * <ul>
	 * <li>Windows: netstat -P TCP
	 * <li>Linux  : netstat -t -a -n 
	 * </ul>
	 * <pre>Active Connections

  Proto  Local Address          Foreign Address        State
  TCP    127.0.0.1:4573         VLADS5014:49163        ESTABLISHED
  TCP    127.0.0.1:49163        VLADS5014:4573         ESTABLISHED
  TCP    127.0.0.1:50461        VLADS5014:50462        ESTABLISHED
  TCP    127.0.0.1:50462        VLADS5014:50461        ESTABLISHED </pre>
  <h2>Linux</h2>
  <pre>Active Internet Connections (servers and established)
  Proto  Recv-Q	Send-Q	Local Address          Foreign Address        State
  TCP    	X1			Y1		127.0.0.1:4573         VLADS5014:49163        ESTABLISHED
  TCP    	X2			Y2		127.0.0.1:49163        VLADS5014:4573         ESTABLISHED </pre>

	 * @return A data tables JSON array of the form [ row1, row2,...] where row = [PROTO, LOCAL ADDRESS, FOREIGN ADDRESS, STATE]
	 * @since 1.0.0
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static JSONArray netstat() throws IOException, JSONException {
		// Netstat is slow... ~6s in win32
		if ( IOTools.OS_IS_WINDOWS ) {
			return netstat_parse("netstat -n");
		}
		else {
			return netstat_parse("netstat -t -a -n");
		}
	}
	
	private static JSONArray netstat_parse(String command) throws IOException, JSONException {
		String stdout		= execute(command);
		
		JSONArray matrix 	= new JSONArray();
		//System.out.println(stdout);
		
		String[] lines = stdout.split(LINE_SEP);
		
		for (String line : lines) {
			// clean LFs
			line 			= line.replaceAll("[\\n\\r\\t]", "").trim();
			if ( line.length() == 0) continue;
			
			// clean spaces
			line 			= line.replaceAll("\\s+", ",");
			String[] fields = line.split(",");
			
			// ignore junk: win/linux
			int junkSize = IOTools.OS_IS_WINDOWS ? 4 : 6;
			
			if ( fields.length != junkSize) continue;

			// Linux must skip junk line[fields len=6]: Active Internet Connections (servers and established)
			if ( !IOTools.OS_IS_WINDOWS && !fields[0].toLowerCase().contains("tcp")) {
				continue;
			}
			
			// Win32: PROTO, LOCAL ADDRESS, FOREIGN ADDRESS, STATE
			// Linux: PROTO, RECV-Q, SEND_Q, LOCAL ADDRESS, FOREIGN ADDRESS, STATE
			//System.out.println("l=" + line + " " + line.length() + " fields=" + fields.length);
			int offset = IOTools.OS_IS_WINDOWS ? 0 : 2;
			JSONArray row = new JSONArray();
			row.put(fields[0]);				// PROTO
			row.put(fields[1 + offset]);	// LOCAL ADDRESS
			row.put(fields[2 + offset]);	// FOREIGN ADDRESS
			row.put(fields[3 + offset]);	// STATE
			matrix.put(row);
		}
		return matrix; 
	}
	
}
