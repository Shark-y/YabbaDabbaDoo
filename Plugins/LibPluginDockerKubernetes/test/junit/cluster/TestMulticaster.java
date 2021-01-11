package junit.cluster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.cloud.core.io.IOTools;
import com.cloud.core.net.NetMulticaster;

public class TestMulticaster {

	static String loadMsg (String name) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		IOTools.pipeStream(TestMulticaster.class.getResourceAsStream(name), bos);
		return bos.toString();
	}
	
	public static void main(String[] args) {
		try {
			
			NetMulticaster m = new NetMulticaster();
			m.open(128);
			m.joinGroup();
			System.out.println("Sender TTL: " + m.getTimeToLive());
			
			String s = loadMsg("sample-1.json"); 
			System.out.println("Sender: " + s);
			m.send(s);

			s = loadMsg("sample-2.json"); 
			System.out.println("Sender: " + s);
			m.send(s);

			Thread.sleep(10000);
			System.out.println("Sender: Shutdown.");
			m.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
