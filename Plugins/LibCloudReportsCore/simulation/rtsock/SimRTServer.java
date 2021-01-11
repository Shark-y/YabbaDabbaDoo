package rtsock;

import com.rts.datasource.DataFormat;
import com.rts.datasource.DataSourceManager;
import com.rts.datasource.IDataMatcher;
import com.rts.datasource.IDataSource.DataSourceType;
import com.rts.datasource.sock.TCPSockDataMatcher;

/**
 * Simulates a server listener.
 * Use this to dump data from a CMS RT sock client.
 * 
 * @author VSilva
 *
 */
public class SimRTServer {
	static DataSourceManager nsm = DataSourceManager.getInstance(); // new DataSourceManager();
	static IDataMatcher matcher = new TCPSockDataMatcher();

	public static void main(String[] args) {
		try {
			if ( args.length == 0) {
				System.err.println("USAGE: RTServer PORT");
				return;
			}
			int port 			= Integer.valueOf(args[0]);

			// default VDN values
			String VDNFIELDS 	= "F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS";
			String footer 		= "F3|END_OF_DATA";

			nsm.addSocketListener(DataSourceType.SOCKET, "test", port, "test", new DataFormat("", footer, "\\|", "\n", VDNFIELDS, null), null);

			System.out.println("Listening in port " + port);
			nsm.startAll();

			while ( true) {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
