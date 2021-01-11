package junit.reports.core;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import rtsock.SimRTSockPumpEvents;

import com.cloud.core.io.FileTool;
import com.cloud.core.types.CoreTypes;
import com.rts.core.*;
import com.rts.datasource.DataFormat;
import com.rts.datasource.DataSourceManager;
import com.rts.datasource.IDataMatcher;
import com.rts.datasource.sock.TCPSockDataMatcher;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNetService {
	
	static DataSourceManager dsm = DataSourceManager.getInstance(); //new DataSourceManager();
	static IDataMatcher matcher = new TCPSockDataMatcher();
	static final String name = "VDN table";

	static void LOGD(String text) {
		System.out.println("[TES-NET-SERV] " + text);
	}

	static void LOGE(String text) {
		System.err.println("[TES-NET-SERV] " + text);
	}

	static final IBatchEventListener listener = new IBatchEventListener() {
		@Override
		public void onBatchReceived(JSONObject batch) {
			try {
				LOGD(batch.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	/* 
	@Test
	public void test() {
		try {
			LOGD("Test net service start.");
			
			String VDNFIELDS = "F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS";
			String footer = "F3|END_OF_RECORDS";
			
			//nsm.addListener("test1", 7000, "test1", new DataFormat(null, footer, "\\|", VDNFIELDS), listener);
			nsm.addListener("test2", 7001, "test2", new DataFormat("", footer, "\\|", VDNFIELDS), listener);
			//nsm.addListener("test2", 7000, "test1", new DataFormat("", "", "|"), null);
			
			System.out.println(nsm.toXML());
			
			nsm.startAll();
			
			LOGD("Sleeping...");
			sleep(30000);
			
			LOGD("Shutting down.");
			nsm.shutdownAll();
		} catch (Exception e) {
			e.printStackTrace();
			fail("TestnetServ failed: " + e.toString());
		}
	} */

	@Test
	public void test01LoadListenersFromFS() {
		try {
			String path = CoreTypes.getClassPathResourceAbsolutePath(TestNetService.class, "/resources/avaya-rts.xml");
			String base = FileTool.getBasePath(path);
			String name = FileTool.getFileName(path);
			
			DataSourceManager nsm = DataSourceManager.getInstance(base, name); // new DataSourceManager(base, name);
			LOGD(nsm.toXML());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test Load from FS failed: " + e.toString());
		}
	}
	
	@Test
	public void test03MatchDataAndFields () {
		String footer = "F3|END_OF_DATA";
		String record = "F1|   2|   7| 15|57:51|  8|1|  :00| 85|22:57| 5: 7| 96| 81| 78| 22| 81| 73|     |     |     |  2| 39|   2|  36";
		String VDNFIELDS = "F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS";
		DataFormat format = new DataFormat(null, footer, "\\|", "\n", VDNFIELDS, null);
		
		String  data = SimRTSockPumpEvents.getRandVDNBuffer(0);
		
		try {
			matcher.matchFormat("VDN table", data, format, listener);
		} catch (JSONException e) {
			e.printStackTrace();
			fail("Test Match data failed: " + e.toString());
		}
	}

	void pushBatches (int size) throws JSONException {
		String footer = "F3|END_OF_DATA";
		String VDNFIELDS = "F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS";
		DataFormat format = new DataFormat(null, footer, "\\|", "\n",  VDNFIELDS, null);
		
		// Add some batches
		for (int i = 0; i < size; i++) {
			String  data = SimRTSockPumpEvents.getRandVDNBuffer(i);

			matcher.matchFormat(name, data, format, new IBatchEventListener() {
				@Override
				public void onBatchReceived(JSONObject batch) {
					try {
						EventQueue.push(name , batch);
					} catch (IOException e) {
						LOGE("onBatchReceived :" + e.toString());
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	@Test
	public void test04EventQueuePushData () {
		// Event queue tests
		try {
			pushBatches(5);
			
			// dump queue, check the size
			JSONArray array = EventQueue.dump();
			
			LOGD("-------Event Queue");
			LOGD(array.toString(1));
			LOGD("-------End Event Queue");
			
			assertTrue("Event queue batchData size must be 5 but got " + array.length(), array.length() == 5);

			sleep(500);
			assertTrue("Event queue size must be 5 but got " + EventQueue.size(name), EventQueue.size(name) == 5);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Test Match data failed: " + e.toString());
		}
	}
	
	@Test
	public void test05EventQueuePopData () {
		// Event queue tests
		try {
			// pop last, check size
			JSONObject batch = EventQueue.pop(name); 
				
			assertNotNull("Batch popped from " + name + " cannot be null.", batch);
				
			LOGD("POP-ONE: " + batch);
			
			assertTrue("Event queue size must be 4 (after pop) but got " + EventQueue.size(name), EventQueue.size(name) == 4);
			
			// pop 4
			for (int i = 0; i < 4; i++) {
				LOGD ("POP [" + i + "]:" + EventQueue.pop(name));
			}
			assertTrue("Event queue size must be 0 (after pop 4) but got " + EventQueue.size(name), EventQueue.size(name) == 0);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Test Match data failed: " + e.toString());
		}
	}

	@Test
	public void test06EventQueuePopEmptyAndClean () {
		// Event queue tests
		try {
			// POP empty must be null
			JSONObject empty = EventQueue.pop(name);
			LOGD("POP from empty " + name + ": " + empty);
			assertNull("Pop from empty queue " + name + " must be null", empty);
			
			// clear all, check size == 0
			EventQueue.clear();
			LOGD ("Cleared all events: size is " + EventQueue.size(name) + " for " + name);
			assertTrue("Event queue size must be 0 (after clear) but got " + EventQueue.size(name), EventQueue.size(name) == 0);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Test Match data failed: " + e.toString());
		}
	}

	@Test
	public void test06EventQueuePrintWriter () {
		// Event queue tests
		try {
			pushBatches(5);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Writer pw = new PrintWriter(bos);
			
			// dump queue, check the size
			EventQueue.dump(pw);
			pw.close();
			
			LOGD("-------Event Queue");
			LOGD(bos.toString());
			LOGD("-------End Event Queue");
			
//			assertTrue("Event queue batchData size must be 5 but got " + array.length(), array.length() == 5);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Test Match data failed: " + e.toString());
		}
	}

	void sleep (long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
}
