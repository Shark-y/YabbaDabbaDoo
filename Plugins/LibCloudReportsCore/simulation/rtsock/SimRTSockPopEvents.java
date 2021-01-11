package rtsock;

import com.cloud.core.w3.WebClient;

/**
 * This class simulates a client that pop real time stats from the server.
 * USAGE: RTSockPop URL [MAXLOOPS]
 * @author VSilva
 *
 */
public class SimRTSockPopEvents {

	static WebClient wc;
	
	public static void main(String[] args) {
		try {
			if ( args.length == 0) {
				System.err.println("USAGE: RTSockPop <URL> <MAXLOOPS>");
				return;
			}
			String url = args[0];
			int maxLoops = args.length > 1 ? Integer.valueOf(args[1]) : 100;
			
			System.out.println(url + " Loops: " + maxLoops);
			
			wc 			= new WebClient(url);
			int count 	= 0;
			
			while ( count++ < maxLoops) {
				try {
					System.out.println(wc.doGet());
				} catch (Exception e) {
					System.err.println(url + " " + e.toString());
				}
				Thread.sleep(2000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
