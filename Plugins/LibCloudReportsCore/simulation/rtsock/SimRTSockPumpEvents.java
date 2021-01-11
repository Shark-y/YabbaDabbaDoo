package rtsock;

import java.io.IOException;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class simulates a client that pumps events into the RTS server
 * Usage: RTSockPump HOST PORT SPLIT/VDN BUF-SIZE
 * @author VSilva
 *
 */
public class SimRTSockPumpEvents {

	Socket sock;
	
	public SimRTSockPumpEvents(String host, int port) throws UnknownHostException, IOException {
		sock = new Socket(host, port);
		sock.setSendBufferSize(1048576);	// def send buf size = 1MB
		System.out.println("Connected " + sock + " Send BufSize: " + sock.getSendBufferSize());
	}
	
	public void write ( byte[] data ) throws IOException {
		sock.getOutputStream().write(data);
	}
	
	public void close () throws IOException {
		sock.close();
	}
	
	static int randInt ( int max) {
		return (int)(Math.random() * max);
	}
	
	/**
	 * Sample batch
F1|  1|   0|  41|  :00|  0|1|  :00| 13| 2:18|     |  0|  4|  1| 34|  0| 80|     |     |     |  0|100|  20|  13
F1|  2|   0|  47|  :00|  0|1|  :00|  8| 3:39|     |  0|  5|  0| 27|  1| 80|     |     |     |  0|100|   0|   8
F1|  3|   0|  46|  :00|  0|1|  :00| 26| 3:05|     |  0| 12|  0| 21|  1| 80|     |     |     |  0|100|   0|  26
F1|  4|   0|  31|  :00|  0|1|  :00| 30| 3:25|     |  0| 22|  0| 26|  1| 80|     |     |     |  0|100|   0|  30
F1|  5|   0|  39|     |  0|1|  :00|  0|     |     |  0|  0|  0| 40|  1| 80|     |     |     |  0|   |   0|   0
F3|END_OF_RECORDS

DATA ITEM	Field size	Type	Description
F1				2	string	Static “F1” string
SPLIT			4	number	Split/Skill number
INQUEUE+INRING	4	number	Calls waiting
AVAILABLE		3	number	Agents Available
ANSTIME/ACDCALLS	5	mm:ss	Avg. speed of answer
ABNCALLS		3	number	Abandon calls
ACD				1	number	ACD number
OLDESTCALL		5	mm:ss	Oldest call waiting
ACDCALLS		3	number	ACD calls
ACDTIME/ACDCALLS	5	mm:ss	Avg. ACD talk time
ABNTIME/ABNCALLS	5	mm:ss	Avg. time to abandon
AGINRING		3	number	Agents: Ringing
ONACD			3	number	Agents: ACD calls
INACW			3	number	Agents: After call work
OTHER			3	number	Agents: Other
INAUX			3	number	Agents: Aux work
STAFFED			3	number	Agents: Staffed
EWTHIGH			5	mm:ss	Expected wait time (high)
EWTMEDIUM		5	mm:ss	Expected wait time (medium)
EWTLOW			5	mm:ss	Expected wait time (low)
DA_INQUEUE+DA_INRING	3	number	Direct agent calls waiting
100*(ACCEPTABLE/CALLSOFFERED)	3	number	% Answered within service level
SERVICELEVEL	4	number	Service level
CALLSOFFERED	4	number	Calls that queued to the split/skill

	 * @return
	 */
	public static String getRandSplitBuffer(int seed) {
		int split = seed; //randInt(5);
		String splitName = "SPLIT-" + seed;
		
		if (split == 1) splitName = "IT";
		if (split == 2) splitName = "Sales";
		if (split == 3) splitName = "Marketing";
		if (split == 4) splitName = "Tech Support";
		if (split == 5) splitName = "Support";

		int callsw = randInt(100);
		int agents = randInt(300);
		int abandoned = randInt(100);
		int acdcalls = randInt(100);
		int aring = randInt(100);
		int aacd = randInt(100);
		int aacw = randInt(100);
		int aoth = randInt(100);
		int aaux = randInt(100);
		int astaf = randInt(100);
		String buf = String.format("F1|%4d|%s|%4d|%3d|%2d:%2d|%3d|%1d|%d|%3d"
				+ "|%2d:%2d|%2d:%2d"
				+ "|%3d|%3d|%3d|%3d|%3d|%3d"
				+ "|     |     |     "
				+ "|%3d|%3d|%4d|%4d"
				+ "\n"
				, split, splitName, callsw, agents, randInt(60), randInt(60), abandoned,1, randInt(100), acdcalls
				, randInt(60), randInt(60), randInt(60), randInt(60)
				, aring, aacd, aacw, aoth, aaux, astaf
				, randInt(20), randInt(100), randInt(10), randInt(100)
				);
		return buf;
	}

	/**
	 * queueName,
agentsOn,
agentsAvail,
agentTalking,
agentNotReady,
callsOffered,
callsAnswered,
callsAbandoned,
callsAbandonedBeforeThreshold,
callsWaiting,
oldestCallAge,
avgAnswerTime,
avgHandleTime,
avgAbandonTime,
serviceLevel

	 * @param seed
	 * @return
	 */
	public static String getRandSplitBuffer1(int seed) {
		String[] NAMES = {"SALES", "TECH SUPPORT", "EASTERN SALES"};
		int idx = (int) (Math.random() * NAMES.length);
		
		String buf = String.format(NAMES[idx] + "|%4d|%4d|%4d|%4d" + "|%4d|%4d|%4d|%4d|%4d" + "|%2d:%2d|%2d:%2d|%2d:%2d|%2d:%2d" + "|%4d\n"
				, randInt(60), randInt(100), randInt(100), randInt(100), 
				randInt(100), randInt(50), randInt(50), randInt(100), randInt(100), 
				randInt(60), randInt(60), randInt(60), randInt(60), randInt(60), randInt(60), randInt(60), randInt(60), 
				randInt(100));
		return buf;
	}
	
	/**
	 * DATA ITEM	Field size	Type	Description
F1					2	string	Static “F1” string
VDN					7	number	VDN number
VDN					20	synonym	VDN synonym
INPROGRESS-ATAGENT	4	number	Calls waiting
OLDESTCALL			5	mm:ss	Oldest call
AVG_ANSWER_SPEED	5	mm:ss	Average answer speed
ABNCALLS			4	number	Abandoned calls
AVG_ABANDON_TIME	5	mm:ss	Average Abandon Time
ACDCALLS			4	number	ACD calls
AVG_ACD_TALK_TIME	5	mm:ss	Avg. ACD talk time
ACTIVECALLS			4	Number	Active calls

Sample data stream:

F1|11057|Western Sales|0|:00||0||0|||
F1|11058|Eastern Sales|0|:00||0||0|||
F3|END_OF_RECORDS

	 * @return
	 */
	static final int SEED_VDN = 1000; //randInt(100000); 
	
	public static String getRandVDNBuffer(int salt) {
		int vdn 		= SEED_VDN + salt;
		int callsw 		= randInt(100);
		int abandoned 	= randInt(100);
		int acdcalls 	= randInt(100);
		
		String buf = String.format("F1|%7d|Eastern Sales|%4d|:00||%4d|%2d:%2d|%4d|%2d:%2d|%4d" + "\n"
				, vdn, callsw
				, abandoned, randInt(60), randInt(60),  acdcalls, randInt(60), randInt(60)
				, randInt(100)
				);
		return buf;
	}

	/**
	 * DATA ITEM	Field size	Type	Description
F1					2	string	Static “F1” string
AGENTID				7	number	VDN number
AGENT-NAME			20	synonym	VDN synonym
SPLIT-ID			2	SPLIT Id
SPLIT-NAME			20	synonym	SPLIT name
INPROGRESS-ATAGENT	4	number	Calls waiting
OLDESTCALL			5	mm:ss	Oldest call
AVG_ANSWER_SPEED	5	mm:ss	Average answer speed
ABNCALLS			4	number	Abandoned calls
AVG_ABANDON_TIME	5	mm:ss	Average Abandon Time
ACDCALLS			4	number	ACD calls
AVG_ACD_TALK_TIME	5	mm:ss	Avg. ACD talk time
ACTIVECALLS			4	Number	Active calls

Sample data stream:

F1|11057|Western Sales|0|:00||0||0|||
F1|11058|Eastern Sales|0|:00||0||0|||
F3|END_OF_RECORDS

	 * @return
	 */
	public static String getRandAgentBuffer(int salt) {
		// Agent
		//String[] NAMES = {"Alice", "Bob", "Charlie Brown", "Monty Burns", "Gooffy", "Bugs Bunny", "Farnz Lizt"};
		//int idx = (int) (Math.random() * NAMES.length);

		// agent states
		String[] STATES = {"Hold", "Ringing", "Other", "AUX", "ACD"};
		int idx = (int) (Math.random() * STATES.length);

		// SPLIT
		String[] SPLIT_NAMES = {"NAR", "EMEA", "CALA", "APAC"};
		//int splitId 		 = (int) (Math.random() * SPLIT_NAMES.length);
		int splitId 		 = (salt % SPLIT_NAMES.length);
		
		int id	 		= SEED_VDN + salt;
		int callsw 		= randInt(100);
		int abandoned 	= randInt(100);
		int acdcalls 	= randInt(100);
		
		String buf = String.format("F1|%7d|%s|%d|%s|%4d|:00|%d"
				+ "|%4d|%2d:%2d|%4d|%2d:%2d"
				+ "|%4d|%s|%d"
				+ "\n"
				, id, /*NAMES[idx]*/ "Name" + salt, (splitId + 1), SPLIT_NAMES[splitId], callsw, randInt(100)
				, abandoned, randInt(60), randInt(60),  acdcalls, randInt(60), randInt(60)
				, randInt(100), STATES[idx], randInt(500)
				);
		return buf;
	}
	
	static String getStartOfBatch() {
		return "F0|" + System.currentTimeMillis() + "\n";
	}

	static String getEndOfBatch() {
		return "F3|END_OF_DATA\n";
	}
	
	static void sleep (long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}
	
	/**
	 * Usage: RTSockClient HOST PORT SPLIT/VDN BUF-SIZE
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if ( args.length < 2) {
				System.err.println("RTSockPump HOST PORT SPLIT/VDN BUF-SIZE");
				return;
			}
			String host 	= args[0];
			int port		= Integer.valueOf(args[1]);
			String type 	= args.length >= 3 ? args[2] : "SPLIT";
			int batchSize 	= args.length >= 4 ? Integer.valueOf(args[3]) : randInt(50);
			
			SimRTSockPumpEvents cli = new SimRTSockPumpEvents(host, port);

			// Max  loops = 100 (every 5s)
			int maxLoops 	= 0;
			while ( maxLoops++ < 100) {
				// start
				//cli.write(getStartOfBatch().getBytes());

				// data buffer
				StringWriter buf		= new StringWriter();

				// batch start
				buf.append(getStartOfBatch());
				
				for (int i = 0; i < batchSize; i++) {
					if ( type.equals("SPLIT")) {
						//cli.write(getRandSplitBuffer().getBytes());
						buf.append(getRandSplitBuffer(i + 1));
					}
					else  if ( type.equals("ACME")) {
						buf.append(getRandSplitBuffer1(i + 1));
					}
					else if ( type.equals("AGENT") ) {
						//cli.write(getRandVDNBuffer(i).getBytes());
						buf.append(getRandAgentBuffer(i));
					}
					else {
						//cli.write(getRandVDNBuffer(i).getBytes());
						buf.append(getRandVDNBuffer(i));
					}
				}
				
				// end
				buf.append(getEndOfBatch());
				
				//System.out.println(buf.toString());
				
				// whole batch at once
				byte[] data = buf.toString().getBytes(); 
				cli.write(data);
				
				// end
				//cli.write(getEndOfBatch().getBytes());
				
				System.out.println("Loop [" + maxLoops + "] Batch size:" + batchSize + " Data bytes: " + data.length);
				sleep(20000);
			}
			
			System.out.println("Closing.");
			cli.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
