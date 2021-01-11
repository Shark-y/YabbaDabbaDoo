package com.cloud.docker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.types.CoreTypes;
import com.cloud.docker.AnsiEscapeCodes.EraseMode;

/**
 * https://github.com/moby/moby/blob/master/pkg/jsonmessage/jsonmessage.go#L281
	<pre>--- STDOUT Format LEN1(HEX)\n<JSON1>\nLEN2(HEX)\n<JSON2>...\n0s
	47
	{"status":"Pulling fs layer","progressDetail":{},"id":"f7e2b70d04ae"}
	
	47
	{"status":"Pulling fs layer","progressDetail":{},"id":"08dd01e3f3ac"}
	
	47
	{"status":"Pulling fs layer","progressDetail":{},"id":"d9ef3a1eb792"}
	
	b1
	{"status":"Downloading","progressDetail":{"current":203,"total":203},"progress":"[==================================================\u003e]     203B/203B","id":"d9ef3a1eb792"}
	
	49
	{"status":"Verifying Checksum","progressDetail":{},"id":"d9ef3a1eb792"}
	
	48
	{"status":"Download complete","progressDetail":{},"id":"d9ef3a1eb792"}
	
	bb
	{"status":"Downloading","progressDetail":{"current":228503,"total":22496034},"progress":"[\u003e                                                  ]  228.5kB/22.5MB","id":"f7e2b70d04ae"}
	
	bc
	{"status":"Downloading","progressDetail":{"current":228503,"total":22262142},"progress":"[\u003e                                                  ]  228.5kB/22.26MB","id":"08dd01e3f3ac"}
	
	bb
	{"status":"Downloading","progressDetail":{"current":687368,"total":22496034},"progress":"[=\u003e                                                 ]  687.4kB/22.5MB","id":"f7e2b70d04ae"}
	
	bc
	{"status":"Downloading","progressDetail":{"current":1144814,"total":22496034},"progress":"[==\u003e                                                ]  1.145MB/22.5MB","id":"f7e2b70d04ae"}
	
	bc
	{"status":"Downloading","progressDetail":{"current":457992,"total":22262142},"progress":"[=\u003e                                                 ]    458kB/22.26MB","id":"08dd01e3f3ac"}
	
	bc
	{"status":"Downloading","progressDetail":{"current":1603566,"total":22496034},"progress":"[===\u003e                                               ]  1.604MB/22.5MB","id":"f7e2b70d04ae"}
	
	bc
	{"status":"Downloading","progressDetail":{"current":687368,"total":22262142},"progress":"[=\u003e                                                 ]  687.4kB/22.26MB","id":"08dd01e3f3ac"}
	
	5e
	{"status":"Digest: sha256:98efe605f61725fd817ea69521b0eeb32bef007af0e3d0aeb6258c6e6fe7fc1a"}
	
	3e
	{"status":"Status: Downloaded newer image for nginx:latest"}
	
	0
	-- END STDOUT</pre>

 * @author VSilva
 *
 */
public class DisplayJSONMessagesStream extends PrintStream {
	private static Map<String, Integer> ids = new HashMap<String, Integer>();
	private boolean isTerminal;
	
	public DisplayJSONMessagesStream(OutputStream os, boolean isTerminal) {
		super(os);
		this.isTerminal = isTerminal;
	}

	@Override
	public void write(byte[] in) throws IOException {
		try {
			display(in);
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}
	
	// XTerm.js DOWN = 40 , UP = 38
	void cursorUp (int n) throws IOException {
		String up = AnsiEscapeCodes.up(n);
		out.write(up.getBytes());
	}
	
	// XTerm.js DOWN = 40 , UP = 38
	void cursorDown (int n) throws IOException {
		String down = AnsiEscapeCodes.down(n);
		out.write(down.getBytes());
	}
	
	void clearLine () throws IOException {
		out.write(AnsiEscapeCodes.eraseLine(EraseMode.All).getBytes());
	}
	
	/*
	 * https://github.com/moby/moby/blob/master/pkg/jsonmessage/jsonmessage.go#L281
	 */
	void display (byte[] in ) throws JSONException, IOException {
		// extract JSON from 	47\n	{"status":"Pulling fs layer","progressDetail":{},"id":"f7e2b70d04ae"}
		String buf 	= new String(in, CoreTypes.CHARSET_UTF8);
		String raw 	= buf.contains("{") ? buf.substring(buf.indexOf("{"), buf.lastIndexOf("}") + 1 ) : "";
		
		if ( raw.isEmpty()) {
			return;
		}
		
		// {"status":"Downloading","progressDetail":{"current":687368,"total":22262142},"progress":"[=\u003e                                                 ]  687.4kB/22.26MB","id":"08dd01e3f3ac"}
		JSONObject root = new JSONObject(raw);
		int diff		= 0;
		
		if ( root.has("id") && root.has("progressDetail") && (root.getJSONObject("progressDetail").length() > 0 ) ) {
			String id		= root.getString("id");
			boolean found 	= ids.containsKey(id);
			int line 		= found ? ids.get(id) : 0;
			
			if ( ! found ) {
				// NOTE: This approach of using len(id) to
				// figure out the number of lines of history
				// only works as long as we clear the history
				// when we output something that's not
				// accounted for in the map, such as a line
				// with no ID.
				line = ids.size();
				ids.put(id, line);
				
				if ( isTerminal ) {
					out.write('\n');
				}
			}
			diff = ids.size() - line;
			
			if ( isTerminal) {
				cursorUp(diff);
			}
		}
		else {
			// When outputting something that isn't progress
			// output, clear the history of previous lines. We
			// don't want progress entries from some previous
			// operation to be updated (for example, pull -a
			// with multiple tags).
			ids.clear();
		}

		displayJSON(root);
		
		if ( root.has("id") && isTerminal ) {
			if ( diff > 0) {
				cursorDown(diff);
			}
		}
	}
	
	// Display displays the JSONMessage to `out`. If `isTerminal` is true, it will erase the
	// entire current line when displaying the progressbar.
	// {"status":"Downloading","progressDetail":{"current":687368,"total":22262142},"progress":"[=\u003e                                                 ]  687.4kB/22.26MB","id":"08dd01e3f3ac"}
	void displayJSON (JSONObject root) throws IOException, JSONException {
		String endl = "";
		
		if ( isTerminal && root.has("progressDetail") ) {
			clearLine();
			endl = "\r";
			out.write(endl.getBytes());
		}
		if ( root.has("id")) {
			out.write(String.format("%s: ", root.getString("id")).getBytes());
		}
		if ( root.has("progress") && isTerminal) {
			String status 	= root.getString("status");
			String progress = root.getString("progress");
			out.write(String.format("%s %s%s", status, progress, endl).getBytes());
		}
		else {
			String status 	= root.getString("status");
			endl 			= "\r\n"; 
			out.write(String.format("%s%s", status, endl).getBytes());
		}
	}
	
	public static void clear () {
		ids.clear();
	}
}
