package junit.cluster;

import com.cloud.cluster.multicast.ZeroConfDiscovery;

public class TestZeroReceiver {

	public static void main(String[] args) {
		try {
			ZeroConfDiscovery ds = ZeroConfDiscovery.getInstance();
			ds.joinGroup();
			ds.receive(new ZeroConfDiscovery.MessageListener() {
				@Override
				public void onTextMessage(String message) {
					System.out.println("Receiver: Got " + message);
				}
			});
			
			Thread.sleep(120000);
			
			System.out.println("Receiver: Shutdown.");
			ds.shutdown(); 

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
